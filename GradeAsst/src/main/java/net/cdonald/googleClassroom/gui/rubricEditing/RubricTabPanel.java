package net.cdonald.googleClassroom.gui.rubricEditing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.undo.UndoManager;
import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.gui.RubricTabInterface;
import net.cdonald.googleClassroom.gui.SetRubricListener;
import net.cdonald.googleClassroom.gui.SetRubricListener.RubricType;
import net.cdonald.googleClassroom.gui.StudentOutputTabs;
import net.cdonald.googleClassroom.gui.TabbedUndoListener;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.GetCompilerQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentIDListQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RubricTestCodeChanged;
import net.cdonald.googleClassroom.listenerCoordinator.SaveRubricListener;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntryRunCode;

public class RubricTabPanel extends JPanel implements RubricFileListener{
	private static final long serialVersionUID = -938409512740386882L;
	private JCheckBox enableEditingBox;
	private JTabbedPane rubricTabbedPane;	
	private Map<RubricTabNames, RubricSourcePanel> sourcePanelMap;
	private JButton saveRubricButton;
	private JButton cancelChangesButton;
	private JPanel compilerOutput;
	private JTextArea compilerOutputArea;
	private RubricTabInterface rubricTabInterface;

	private volatile Rubric rubric;
	
	private RubricSummaryPanel rubricSummaryPanel;
	private volatile Rubric formerRubric;	
	

	public RubricTabPanel(UndoManager undoManager, RubricTabInterface rubricTabInterface) {
		super();
		this.rubricTabInterface = rubricTabInterface;
		sourcePanelMap = new HashMap<RubricTabNames, RubricSourcePanel>();
		createLayout(undoManager, rubricTabInterface);
		registerListeners();
	}




	public void updateRubricCode() {
		if (rubric != null) {
			for (RubricTabNames tabName : sourcePanelMap.keySet()) {
				RubricSourcePanel panel = sourcePanelMap.get(tabName);
				if (panel != null) {
					if (panel.isModified()) {
						List<FileData> files = panel.createFileData(tabName.toString());				
						if (tabName == RubricTabNames.Reference) {
							rubric.setReferenceSource(files);
						}
						else {
							rubric.setTestCode(files);
						}
						panel.setModified(false);
					}
				}
			}
		}
	}

	private void createLayout(UndoManager undoManager, RubricTabInterface rubricTabInterface) {
		setLayout(new BorderLayout());
		compilerOutput = new JPanel();
		compilerOutput.setLayout(new BorderLayout());
		compilerOutputArea = new JTextArea();
		compilerOutput.add(new JScrollPane(compilerOutputArea), BorderLayout.CENTER);
		addButtonBar();	
		rubricSummaryPanel = new RubricSummaryPanel(undoManager, this, rubricTabInterface);
		sourcePanelMap.put(RubricTabNames.Reference, new RubricSourcePanel(undoManager, RubricTabNames.Reference, this));
		sourcePanelMap.put(RubricTabNames.TestCode, new RubricSourcePanel(undoManager, RubricTabNames.TestCode, this));
		rubricTabbedPane = new JTabbedPane();		
		rubricTabbedPane.addTab(RubricTabNames.Reference.toString(), sourcePanelMap.get(RubricTabNames.Reference));
		rubricTabbedPane.addTab(RubricTabNames.TestCode.toString(), sourcePanelMap.get(RubricTabNames.TestCode));
		rubricTabbedPane.addTab(RubricTabNames.Summary.toString(), rubricSummaryPanel);

		new TabbedUndoListener(undoManager, rubricTabbedPane);

		add(rubricTabbedPane, BorderLayout.CENTER);
		enableEditing(false);
		rubricTabbedPane.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				boolean summaryFocus =  rubricTabbedPane.getTitleAt(rubricTabbedPane.getSelectedIndex()).equals(RubricTabNames.Summary.toString());
				if (summaryFocus) {
					for (RubricSourcePanel sourcePanel : sourcePanelMap.values()) {
						if (sourcePanel.isModified()) {
							rubricSummaryPanel.sourceIsChanged();
							break;
						}
					}
				}
				rubricSummaryPanel.hasFocus(summaryFocus);
			}			
		});

	}

	private void registerListeners() {
		ListenerCoordinator.addListener(RubricTestCodeChanged.class, new RubricTestCodeChanged() {
			@Override
			public void fired() {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						addSources();
					}
				});

			}
		});
		
	}

	public void saveAllChanges() {
		updateRubricCode();
		rubricSummaryPanel.saving();

	}
	
	private void setFormerRubric(Rubric rubric) {		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				synchronized(RubricTabPanel.this) 
				{
					if (rubric != null) {
						formerRubric = new Rubric(rubric);
					}
					else {
						formerRubric = null;
					}
				}
			}
		});
	}

	private void addButtonBar() {
		JPanel buttonBar = new JPanel();
		buttonBar.setLayout(new FlowLayout(FlowLayout.LEFT));		
		enableEditingBox = new JCheckBox("Enable Editing", false);
		saveRubricButton = new JButton("Save");
		cancelChangesButton = new JButton("Cancel");
		cancelChangesButton.setToolTipText("Removes all changes since the last save");
		saveRubricButton.setPreferredSize(cancelChangesButton.getPreferredSize());		

		enableEditingBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (enableEditingBox.isSelected() == false) {
					saveAllChanges();					
				}
				enableEditing(enableEditingBox.isSelected());
			}
		});

		saveRubricButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAllChanges();
				setFormerRubric(rubric);
				ListenerCoordinator.fire(SaveRubricListener.class);
			}
		});

		cancelChangesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rubric = null;
				synchronized(formerRubric) {
					ListenerCoordinator.fire(SetRubricListener.class, formerRubric, RubricType.PRIMARY);
				}
				setFormerRubric(null);
				rubric.clearRubricDefinitionModified();
				enableEditingBox.setSelected(false);
				enableEditing(false);
				
			}
		});



		buttonBar.add(enableEditingBox);
		buttonBar.add(saveRubricButton);
		buttonBar.add(cancelChangesButton);
		add(buttonBar, BorderLayout.NORTH);
	}
	

	private void enableEditing(boolean enableEditing) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				saveRubricButton.setVisible(enableEditing || (rubric != null && rubric.isRubricDefinitionModified()));
				cancelChangesButton.setVisible(saveRubricButton.isVisible());
				for (RubricSourcePanel sourcePanel : sourcePanelMap.values()) {
					sourcePanel.enableEditing(enableEditing);
				}
				rubricSummaryPanel.enableEditing(enableEditing);
				if (enableEditing == true && rubric != null && formerRubric == null) {
					setFormerRubric(rubric);
				}				
				revalidate();
			}
		});

	}



	public void changeRubricTabs(Rubric localRubric, UndoManager undoManager) {		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (rubric != localRubric) {					
					setFormerRubric(null);
					rubric = localRubric;
					addSources();
					rubricSummaryPanel.setRubricToModify(localRubric);
					rubricSummaryPanel.enableEditing(false);
					enableEditingBox.setSelected(false);
					enableEditing(false);
				}
			}
		});
	}

	private void addSources() {
		if (rubric != null) {
			List<FileData> referenceSoure = rubric.getReferenceSource();
			addSource(referenceSoure, RubricTabNames.Reference, false);						
			List<FileData> rubricTestCode = rubric.getTestCode();
			addSource(rubricTestCode, RubricTabNames.TestCode, false);			
		}

	}

	private void addSource(List<FileData> files, RubricTabNames tabName, boolean setModified) {		
		sourcePanelMap.get(tabName).setSourceTabs(files, setModified);
		
	}
	
	public List<FileData> createFileData(RubricTabNames rubricTabs) {
		RubricSourcePanel panel = sourcePanelMap.get(rubricTabs);
		if (panel != null) {
			return panel.createFileData(rubricTabs.toString());
		}
		else {
			return new ArrayList<FileData>();
		}
	}
	

	@Override
	public List<FileData> getReferenceSource() {		
		return createFileData(RubricTabNames.Reference);
	}
	@Override
	public List<FileData> getTestSource() {
		return createFileData(RubricTabNames.TestCode);	}

	@Override
	public JButton getAddSourceButtons() {
		return sourcePanelMap.get(RubricTabNames.TestCode).getSourceButton(true);

	}
	
	@Override
	public boolean isReferenceSourceSet() {		
		return sourcePanelMap.get(RubricTabNames.Reference).containsFiles();
	} 
	
	@Override
	public void addCompilerMessage(String text) {
		rubricTabInterface.addAndSelectRubricTab("Compiler Message", compilerOutput);
		compilerOutputArea.setText(text);
	}
	@Override
	public void sourceIsChanged() {
		rubricSummaryPanel.sourceIsChanged();
	}
	@Override
	public void compileSource(RubricTabNames sourceType) {
		List<FileData> refFiles = null;
		StudentWorkCompiler compiler = (StudentWorkCompiler)ListenerCoordinator.runQuery(GetCompilerQuery.class);

		switch(sourceType) {
		case Reference:
			refFiles = createFileData(RubricTabNames.Reference);
			break;
		case Summary:
			refFiles = createFileData(RubricTabNames.Reference);
			refFiles.addAll(createFileData(sourceType));
			break;
		case TestCode:
			break;
		}

		if (refFiles != null) {
			try {
				compiler.compile(refFiles);
			} catch (org.mdkt.compiler.CompilationException e) {
				addCompilerMessage(e.getLocalizedMessage());
				return;
			} catch (Exception e) {
				DebugLogDialog.appendException(e);
			}
		}	
		addCompilerMessage("");		

	}

	public static void main(String args[]) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				Thread.setDefaultUncaughtExceptionHandler(new ExceptionLogger());
				new TestFrame().setVisible(true);
			}
		});
	}
	public static class ExceptionLogger implements Thread.UncaughtExceptionHandler {

		@Override
		public void uncaughtException(Thread t, Throwable e) {
			e.printStackTrace();			
		}
    	
    }

	
	private static class TestFrame extends JFrame {
		private static final long serialVersionUID = 6493675455296312016L;
		List<FileData> refSource;
		List<FileData> testSource;
		public TestFrame() {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedLookAndFeelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setSize(800, 800);
			refSource = new ArrayList<FileData>();
			testSource = new ArrayList<FileData>();
			refSource.add(FileData.initFromDisk("C:\\Users\\kdmacdon\\Documents\\Teals\\EclipseWork\\BigIntAddition\\src\\BigIntAddition.java"));
			testSource.add(FileData.initFromDisk("C:\\Users\\kdmacdon\\Documents\\Teals\\EclipseWork\\BigIntAddition\\src\\ConvertStringTest.java"));
			testSource.add(FileData.initFromDisk("C:\\Users\\kdmacdon\\Documents\\Teals\\EclipseWork\\BigIntAddition\\src\\AddBigIntTest.java"));
			StudentWorkCompiler compiler = new StudentWorkCompiler(null);			
			ListenerCoordinator.addQueryResponder(GetCompilerQuery.class, new GetCompilerQuery() {

				@Override
				public StudentWorkCompiler fired() {
					// TODO Auto-generated method stub
					return compiler;
				}				
			});
			Map<String, String> nameMap = new HashMap<String, String>();
			List<String> ids = new ArrayList<String>();
			for (int i = 0; i < 6; i++) {
				ids.add("" + i);
				nameMap.put("" + i, "First Name " + " Last Name " + i );
			}

			ListenerCoordinator.addQueryResponder(GetStudentIDListQuery.class, new GetStudentIDListQuery() {

				@Override
				public List<String> fired() {
					return ids;
				}				
			});
			ListenerCoordinator.addQueryResponder(GetStudentNameQuery.class, new GetStudentNameQuery() {

				@Override
				public String fired(String studentID) {
					return nameMap.get(studentID);
				}				
			});			

			ListenerCoordinator.addQueryResponder(GetStudentFilesQuery.class, new GetStudentFilesQuery() {
				@Override
				public List<FileData> fired(String studentID) {
					Integer id = Integer.parseInt(studentID);
					if (id == 2) {
						return testSource;
					}
					return null;
				}

			});
			Rubric rubric = new Rubric();
			rubric.setReferenceSource(refSource);
			rubric.setTestCode(testSource);
			rubric.addNewEntry(0);
			RubricEntry e = rubric.getEntry(0);
			e.setName("Test");
			e.setValue(5);
			e.setAutomation(new RubricEntryRunCode());
			rubric.clearRubricDefinitionModified();
			UndoManager undoManager = new UndoManager();
			StudentOutputTabs studentTabs = new StudentOutputTabs(null, null);
			RubricTabPanel testPanel = new RubricTabPanel(undoManager, studentTabs);
			testPanel.changeRubricTabs(rubric, undoManager);
			add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, testPanel, studentTabs));
			setVisible(true);
		}


	}



}
