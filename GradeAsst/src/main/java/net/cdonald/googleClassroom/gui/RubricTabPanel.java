package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.UndoManager;

import net.cdonald.googleClassroom.gui.SetRubricListener.RubricType;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.GetCompilerQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentIDListQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LoadSourceQuery;
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
	private Map<RubricTabNames, Map<String, LineNumberTextArea> > sourceCodeMap;
	private Map<RubricTabNames, JTabbedPane> sourceTabsMap;
	private Map<RubricTabNames, RubricSourcePanel> sourcePanelMap;
	private JButton saveRubricButton;
	private JButton cancelChangesButton;
	private JPopupMenu editablePopup;
	private JPopupMenu displayPopup;
	private Rubric rubric;
	private UndoManager undoManager;
	private RubricSummaryPanel rubricSummaryPanel;
	private volatile Rubric formerRubric;	
	private enum RubricTabNames {Summary("Summary"), Reference("Reference Source"), TestCode("Test Code");
		private String name;
		RubricTabNames(String name) {
			this.name = name;
		};
		public String toString() {
			return name;
		}

	}
	public RubricTabPanel(UndoManager undoManager, JPopupMenu popupSource, JPopupMenu popupDisplay) {
		super();
		this.displayPopup = popupDisplay;
		this.editablePopup = popupSource;
		this.undoManager = undoManager;		
		sourceCodeMap = new HashMap<RubricTabNames, Map<String, LineNumberTextArea>>();
		sourceTabsMap = new HashMap<RubricTabNames, JTabbedPane>();
		sourcePanelMap = new HashMap<RubricTabNames, RubricSourcePanel>();
		createLayout(undoManager);
		registerListeners();
	}
	private List<FileData> createFileData(RubricTabNames rubricTab) {
		Map<String, LineNumberTextArea> sourceMap = sourceCodeMap.get(rubricTab);
		List<FileData> fileData = new ArrayList<FileData>();
		if (sourceMap != null) {			
			for (String name : sourceMap.keySet()) {
				fileData.add(new FileData(name, sourceMap.get(name).getText(), rubricTab.toString(), null));
			}
		}
		return fileData;
	}

	private boolean isSourceModified(RubricTabNames rubricTab) {
		Map<String, LineNumberTextArea> sourceMap = sourceCodeMap.get(rubricTab);
		if (sourceMap != null) {
			for (LineNumberTextArea textArea : sourceMap.values()) {
				if (textArea.isModified()) {
					return true;
				}
			}
		}
		return false;		
	}

	public void updateRubricCode() {
		if (rubric != null) {
			if (isSourceModified(RubricTabNames.Reference)) {
				rubric.setReferenceSource(createFileData(RubricTabNames.Reference));
				
			}
			if (isSourceModified(RubricTabNames.TestCode)) {
				rubric.setTestCode(createFileData(RubricTabNames.TestCode));	
			}
		}
	}

	private void createLayout(UndoManager undoManager) {
		setLayout(new BorderLayout());
		createPopupMenu();
		addButtonBar();	
		rubricSummaryPanel = new RubricSummaryPanel(undoManager, this);
		sourcePanelMap.put(RubricTabNames.Reference, new RubricSourcePanel(RubricTabNames.Reference));
		sourcePanelMap.put(RubricTabNames.TestCode, new RubricSourcePanel(RubricTabNames.TestCode));
		rubricTabbedPane = new JTabbedPane();		
		rubricTabbedPane.addTab(RubricTabNames.Reference.toString(), sourcePanelMap.get(RubricTabNames.Reference));
		rubricTabbedPane.addTab(RubricTabNames.TestCode.toString(), sourcePanelMap.get(RubricTabNames.TestCode));
		rubricTabbedPane.addTab(RubricTabNames.Summary.toString(), rubricSummaryPanel);

		new TabbedUndoListener(undoManager, rubricTabbedPane);

		add(rubricTabbedPane, BorderLayout.CENTER);
		enableEditing(false);


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
	
	private void setFormerRubric(boolean set) {
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				synchronized(RubricTabPanel.this) 
				{
					if (set) {
						formerRubric = new Rubric(rubric);
					}
					else {
						rubric = formerRubric;
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
				setFormerRubric(true);
				ListenerCoordinator.fire(SaveRubricListener.class);
			}
		});

		cancelChangesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(SetRubricListener.class, formerRubric, RubricType.PRIMARY);
				setFormerRubric(false);
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
				enableSourceEditing(enableEditing);
				for (RubricSourcePanel sourcePanel : sourcePanelMap.values()) {
					sourcePanel.enableEditing(enableEditing);
				}
				if (enableEditing == true && rubric != null && formerRubric == null) {
					setFormerRubric(true);
				}				
				rubricSummaryPanel.enableEditing(enableEditing);
				revalidate();

			}
		});

	}

	private void enableSourceEditing(boolean enableEditing) {
		for (Map<String, LineNumberTextArea> sourceMap : sourceCodeMap.values()) {
			if (sourceMap != null) {
				for (JTextArea textArea : sourceMap.values()) {
					textArea.setEditable(enableEditing);
					textArea.setComponentPopupMenu(enableEditing ? editablePopup : displayPopup);			
				}
			}
		}
	}

	public void changeRubricTabs(Rubric localRubric, UndoManager undoManager) {		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (rubric != localRubric) {					
					setFormerRubric(false);
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

	private JTabbedPane addSource(List<FileData> files, RubricTabNames tabName, boolean setModified) {
		Map<String, LineNumberTextArea> sourceMap = sourceCodeMap.get(tabName);
		if (sourceMap == null) {
			sourceCodeMap.put(tabName, new HashMap<String, LineNumberTextArea>());
			sourceMap = sourceCodeMap.get(tabName);
		}
		sourceMap.clear();
		JTabbedPane sourceTabs = new JTabbedPane();
		new TabbedUndoListener(undoManager, sourceTabs);
		if (files != null) {						
			if (files != null && files.size() > 0) {								
				for (FileData file : files) {
					LineNumberTextArea testSource = new LineNumberTextArea();
					testSource.setText(file.getFileContents());
					sourceTabs.addTab(file.getName(), testSource.getScrollPane());									
					testSource.getDocument().addUndoableEditListener(undoManager);
					sourceMap.put(file.getName(), testSource);
					testSource.setModified(setModified);
				}
			}
		}
		enableSourceEditing(enableEditingBox.isSelected());
		sourceTabsMap.put(tabName, sourceTabs);
		sourcePanelMap.get(tabName).setSourceTabs(sourceTabs);
		if (tabName == RubricTabNames.Reference) {
			rubricSummaryPanel.referenceSourceChanged();
		}
		else {
			rubricSummaryPanel.testSourceChanged();
		}
		return sourceTabs;
	}

	private void createPopupMenu() {
		editablePopup = new JPopupMenu();
		displayPopup = new JPopupMenu();

		Action cut = new DefaultEditorKit.CutAction();
		cut.putValue(Action.NAME, "Cut");
		cut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
		editablePopup.add(cut);


		Action copy = new DefaultEditorKit.CopyAction();
		copy.putValue(Action.NAME, "Copy");
		copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
		editablePopup.add(copy);		
		displayPopup.add(copy);


		Action paste = new DefaultEditorKit.PasteAction();
		paste.putValue(Action.NAME, "Paste");
		paste.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
		editablePopup.add(paste);


		JMenuItem recompile = new JMenuItem("Recompile");
		editablePopup.add(recompile);

		JMenuItem removeSource = new JMenuItem("Remove Source");
		editablePopup.add(removeSource);		
		removeSource.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int currentTab = rubricTabbedPane.getSelectedIndex();
				String title = rubricTabbedPane.getTitleAt(currentTab);
				RubricTabNames rubricTabSelected = null;
				for (RubricTabNames rubricTab : RubricTabNames.values()) {
					if (rubricTab.toString().equals(title)) {
						rubricTabSelected = rubricTab;
						break;
					}
				}
				if (rubricTabSelected != null) {
					JTabbedPane sourceTabs = sourceTabsMap.get(rubricTabSelected);
					if (sourceTabs != null) {
						int selectedIndex = sourceTabs.getSelectedIndex();
						sourceTabs.removeTabAt(selectedIndex);
					}
				}
			}
		});
	}

	private class RubricSourcePanel extends JPanel {
		private static final long serialVersionUID = 4659401187763797680L;
		private JPanel buttonSource;
		private RubricTabNames tabName;
		private JTable studentTable; 	
		private List<String> allIDs;
		private JTabbedPane sourceTabPane;
		private JPanel studentPanel;

		RubricSourcePanel(RubricTabNames tabName) {
			super();
			this.tabName = tabName;
			setVisible(true);
			createLayout();		
		}

		public void enableEditing(boolean enableEditing) {
			buttonSource.setVisible(enableEditing);
			if (studentPanel != null) {
				studentPanel.setVisible(enableEditing);
			}

			
			//revalidate();
		}
	

		public void setSourceTabs(JTabbedPane sourceTabs) {
			if (sourceTabPane != null) {
				this.remove(sourceTabPane);
			}
			sourceTabPane = sourceTabs;
			GridBagConstraints c = new GridBagConstraints();
			//c.insets = new Insets(3, 3, 3, 0);
			c.weightx = 10.0;
			c.weighty = 1.0;
			c.fill = GridBagConstraints.BOTH;
			c.anchor = GridBagConstraints.LINE_START;
			//c.gridwidth = GridBagConstraints.REMAINDER;
			c.gridheight = 2;
			c.gridx = 0;
			c.gridy = 0;

			add(sourceTabs, c);
			
			revalidate();
		}

		private void createLayout() {
			
			createStudentTable(tabName);
//			if (studentPanel != null) {
//				add(studentPanel, BorderLayout.CENTER);				
//			}
			buttonSource = getSourceButtons(tabName);
			//add(buttonSource, BorderLayout.NORTH);

			setLayout(new GridBagLayout());
 

			
			GridBagConstraints c = new GridBagConstraints();
			//			c.insets = new Insets(3, 3, 3, 0);
			c.weightx = tabName == RubricTabNames.TestCode ? 0.1 : 1.0;
			c.weighty = 1.0;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.LINE_END;
			//c.gridwidth = GridBagConstraints.;
			//c.gridheight = 1;
			c.gridx = 1;
			c.gridy = 0;
			add(buttonSource, c);
			
			if (studentPanel != null) {
				c = new GridBagConstraints();
				c.insets = new Insets(3, 3, 3, 0);
				c.weightx = 0.8;
				c.weighty = 15.0;
				c.fill = GridBagConstraints.BOTH;
				c.anchor = GridBagConstraints.LINE_START;
				//c.gridwidth = GridBagConstraints.REMAINDER;
				//c.gridheight = 10;
				//c.gridwidth = ;
				c.gridx = 1;
				c.gridy = 1;
				add(studentPanel, c);
			}
			

		
		}
		
		public void addTableListener() {
			studentTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
				@Override
				public void valueChanged(ListSelectionEvent e) {
					if (e.getValueIsAdjusting()) {
						return;
					}
					SwingUtilities.invokeLater(new Runnable() {

						@Override
						public void run() {
							if (allIDs != null && allIDs.size() > 0) {

								ListSelectionModel lsm = (ListSelectionModel) e.getSource();
								if (lsm.isSelectionEmpty() == false) {
									int selectedRow = lsm.getMinSelectionIndex();
									if (allIDs != null && allIDs.size() > selectedRow) {
										String id = allIDs.get(selectedRow);
										@SuppressWarnings( "unchecked" )
										List<FileData> studentSource = (List<FileData>)ListenerCoordinator.runQuery(GetStudentFilesQuery.class, id);
										if (studentSource != null && studentSource.size() != 0) {
											addSource(studentSource, tabName, true);
										}
									}
								}
							}
						}

					});				
				}					
			});
		}
		public void createStudentTable(RubricTabNames tabName) {
			studentPanel = null;
			if (tabName == RubricTabNames.Reference) {
				studentPanel = new JPanel();
				studentPanel.setLayout(new GridBagLayout());

				studentTable = new JTable();				
				DefaultTableModel dtm = new StudentTableModel();
				studentTable.setModel(dtm);
				studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				studentTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
				studentTable.setTableHeader(null);
				
				GridBagConstraints c = new GridBagConstraints();
				c.insets = new Insets(3, 3, 3, 0);
				c.weightx = 1.0;
				c.weighty = 1.0;
				c.fill = GridBagConstraints.NONE;
				c.anchor = GridBagConstraints.LINE_START;
				//c.gridwidth = GridBagConstraints.;
				//c.gridheight = 1;
				c.gridx = 0;
				c.gridy = 0;
				studentPanel.add(new JLabel("Or Select Student Source"), c);
				c = new GridBagConstraints();
				c.insets = new Insets(3, 3, 3, 0);
				c.weightx = 1.0;
				c.weighty = 5.0;
				c.fill = GridBagConstraints.BOTH;
				c.anchor = GridBagConstraints.LINE_START;
				//c.gridwidth = GridBagConstraints.;
				//c.gridheight = 1;
				c.gridx = 0;
				c.gridy = 1;
				//Dimension viewPortSize = studentTable.getPreferredScrollableViewportSize();				
			
				JScrollPane jsp = new JScrollPane(studentTable);
				studentPanel.add(jsp, c);
				addTableListener();
			}			
		}
		private class StudentTableModel extends DefaultTableModel {
			private static final long serialVersionUID = -6492076653694653813L;
			
			
			@Override
			public int getRowCount() {
				setStudentNames(0);
				if (allIDs == null) {
					return 20;
				}
				return allIDs.size();
			}

			@Override
			public int getColumnCount() {
				return 1;
			}

			@Override
			public boolean isCellEditable(int row, int column) {	
				return false;
			}

			@Override
			public Object getValueAt(int row, int column) {
				setStudentNames(row);
				if (allIDs == null || row > allIDs.size()) {
					return "                           ";
				}
				else {					
					return ListenerCoordinator.runQuery(GetStudentNameQuery.class, allIDs.get(row));
				}
			}

			private void setStudentNames(int row) {
				if (allIDs == null  || row >= allIDs.size()) {
					allIDs = (ArrayList<String>)ListenerCoordinator.runQuery(GetStudentIDListQuery.class);
				}
			}
		}
	}

	private JPanel getSourceButtons(RubricTabNames tabName) {
		JPanel buttonSource;
		buttonSource = new JPanel();		
		JButton loadSource = new JButton("Load Source");
		loadSource.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (enableEditingBox.isSelected()) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							@SuppressWarnings("unchecked")
							List<FileData> allFiles = (List<FileData>)ListenerCoordinator.runQuery(LoadSourceQuery.class);
							addSource(allFiles, tabName, true);							
						}						
					});					
				}
			}				
		});
		

		buttonSource.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 3, 3, 0);
		c.weightx = 0.3;
		c.weighty = 0.3;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		//c.gridwidth = GridBagConstraints.REMAINDER;
		//c.gridheight = 1;
		c.gridx = 0;
		c.gridy = 0;
		buttonSource.add(loadSource, c);		
		return buttonSource;

	}

	@Override
	public List<FileData> getReferenceSource() {		
		return createFileData(RubricTabNames.Reference);
	}
	@Override
	public List<FileData> getTestSource() {
		return createFileData(RubricTabNames.TestCode);	}

	@Override
	public JPanel getTestSourceButtons() {
		return getSourceButtons(RubricTabNames.TestCode);
	}
	
	@Override
	public boolean isReferenceSourceSet() {
		Map<String, LineNumberTextArea> sourceMap = sourceCodeMap.get(RubricTabNames.Reference);
		return (sourceMap != null && sourceMap.size() != 0); 
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
	private static class TestFrame extends JFrame implements RubricFileListener{

	
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
			RubricTabPanel testPanel = new RubricTabPanel(undoManager, null, null);
			testPanel.changeRubricTabs(rubric, undoManager);
			add(testPanel);
			setVisible(true);
		}


		@Override
		public List<FileData> getReferenceSource() {
			return refSource;
		}

		@Override
		public List<FileData> getTestSource() {
			// TODO Auto-generated method stub
			return testSource;
		}

		@Override
		public JPanel getTestSourceButtons() {
			// TODO Auto-generated method stub
			JPanel temp = new JPanel();
			temp.setLayout(new BorderLayout());
			temp.add(new JButton("Load Source"), BorderLayout.CENTER);
			return temp;
		}


		@Override
		public boolean isReferenceSourceSet() {
			return true;
		}
	}



}
