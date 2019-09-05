package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultEditorKit;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabsListener;
import net.cdonald.googleClassroom.listenerCoordinator.AssignmentSelected;
import net.cdonald.googleClassroom.listenerCoordinator.GetCompilerMessageQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentTextAreasQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.PreRunBlockingListener;
import net.cdonald.googleClassroom.listenerCoordinator.RecompileListener;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveSourceListener;

import net.cdonald.googleClassroom.listenerCoordinator.StudentSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.SystemInListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;

public class ConsoleAndSourcePanel extends JPanel {
	private static final long serialVersionUID = 1084761781946423738L;
	private JTabbedPane overallTabbedPane;
	private JTabbedPane sourceTabbedPane;
	private JTabbedPane rubricTabbedPane;
	//private JTextField consoleInput;
	private JTextArea consoleInput;
	private JPopupMenu popupSource;
	private JPopupMenu popupInput;
	private JPopupMenu popupDisplays;
	private SplitOutErrPanel outputWrapperPanel;
	private JPanel inputHistorWrapperPanel;
	private Map<String, SplitOutErrPanel> rubricPanels;
	private static Semaphore pauseSemaphore = new Semaphore(1);
	private JTextArea currentInputHistory;
	private String currentID;
	private List<JTextArea> currentSourceTextAreas;


	public ConsoleAndSourcePanel() {
		setMinimumSize(new Dimension(400, 400));
		createPopupMenu();
		createLayout();
		registerListeners();
		setVisible(true);
		currentSourceTextAreas = new ArrayList<JTextArea>();
		rubricPanels = new HashMap<String, SplitOutErrPanel>();

	}

	public void assignmentSelected() {
		sourceTabbedPane.removeAll();
		consoleInput.setText("");		
	}

	public void setWindowData(String idToDisplay) {
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {
				currentSourceTextAreas.clear();
				sourceTabbedPane.removeAll();
				currentID = idToDisplay;
				if (idToDisplay != null) {
					List<FileData> fileDataList = (List<FileData>) ListenerCoordinator.runQuery(GetStudentFilesQuery.class, idToDisplay);
					CompilerMessage compilerMessage = (CompilerMessage)ListenerCoordinator.runQuery(GetCompilerMessageQuery.class, idToDisplay);					
					if (fileDataList != null) {
						for (FileData fileData : fileDataList) {
							setSourceContents(fileData.getName(), fileData.getFileContents());
						}
						if (compilerMessage != null && compilerMessage.getCompilerMessage() != null && compilerMessage.getCompilerMessage().length() > 2) {
								setSourceContents("Compiler Message", compilerMessage.getCompilerMessage());
						}

					}
				}
				bindStudentAreas(idToDisplay);
			}

		});

	}
	
	private void setSourceContents(String title, String text) {
		JPanel sourcePanel = new JPanel();
		sourcePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		sourcePanel.setLayout(new BorderLayout());
		JTextArea sourceArea = new JTextArea();
		sourceArea.setText(text);
		sourceArea.setComponentPopupMenu(popupSource);
		sourcePanel.add(new JScrollPane(sourceArea));
		currentSourceTextAreas.add(sourceArea);
		sourceTabbedPane.addTab(title, sourcePanel);
		sourceArea.setCaretPosition(0);		
	}

	private void createPopupMenu() {
		popupSource = new JPopupMenu();
		popupDisplays = new JPopupMenu();
		popupInput = new JPopupMenu();

		Action cut = new DefaultEditorKit.CutAction();
		cut.putValue(Action.NAME, "Cut");
		cut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
		popupSource.add(cut);
		popupInput.add(cut);

		Action copy = new DefaultEditorKit.CopyAction();
		copy.putValue(Action.NAME, "Copy");
		copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
		popupSource.add(copy);
		popupInput.add(cut);
		popupDisplays.add(copy);

		Action paste = new DefaultEditorKit.PasteAction();
		paste.putValue(Action.NAME, "Paste");
		paste.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
		popupSource.add(paste);
		popupInput.add(paste);

		JMenuItem recompile = new JMenuItem("Recompile");
		popupSource.add(recompile);
		
		JMenuItem removeSource = new JMenuItem("Remove Source");
		popupSource.add(removeSource);
		recompile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentID != null) {
					int currentTab = sourceTabbedPane.getSelectedIndex();
					String fileName = sourceTabbedPane.getTitleAt(currentTab);
					if (currentTab < currentSourceTextAreas.size()) {
						JTextArea currentSourceArea = currentSourceTextAreas.get(currentTab);
						String sourceText = currentSourceArea.getText();
						ListenerCoordinator.fire(RecompileListener.class, currentID, fileName, sourceText);
					}
				}

			}
		});
		
		removeSource.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentID != null) {
					int currentTab = sourceTabbedPane.getSelectedIndex();
					String fileName = sourceTabbedPane.getTitleAt(currentTab);
					if (currentTab < currentSourceTextAreas.size()) {
						ListenerCoordinator.fire(RemoveSourceListener.class, currentID, fileName);
					}
				}

			}
		});
	}
	private class SplitOutErrPanel {
		private JPanel out;
		private JPanel err;
		private JSplitPane splitPane;
		public SplitOutErrPanel() {
			out = new JPanel();
			err = new JPanel();
			out.setLayout(new BorderLayout());			
			out.setBorder(BorderFactory.createTitledBorder("System.out"));
			err.setLayout(new BorderLayout());			
			err.setBorder(BorderFactory.createTitledBorder("System.err"));
			splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, out, err);
			splitPane.setResizeWeight(0.9);
		}
		
		public JSplitPane getSplitPane() {
			return splitPane;
		}

		public void clearPanels() {
			clearPanel(out);
			clearPanel(err);
		}
		public void clearAndAdd(StudentConsoleAreas.OutputAreas outputAreas) {
			JTextArea output = outputAreas.getOutputArea();
			clearAndAddPanel(out, output);
			JTextArea error = outputAreas.getErrorArea();
			clearAndAddPanel(err, error);
			output.setComponentPopupMenu(popupDisplays);
			error.setComponentPopupMenu(popupDisplays);
		}
	}

	private void createLayout() {
		setSize(800, 500);
		setLayout(new BorderLayout());

		consoleInput = new JTextArea();//new JTextField();
		consoleInput.setText("");
		consoleInput.setMinimumSize(new Dimension(20, 25));
		consoleInput.setPreferredSize(new Dimension(20, 25));
		consoleInput.setComponentPopupMenu(popupInput);


		JPanel ioPanel = new JPanel();
		ioPanel.setLayout(new BorderLayout());
		JPanel inputWrapper = new JPanel();
		inputWrapper.setLayout(new BorderLayout());
		;
		inputWrapper.setBorder(BorderFactory.createTitledBorder("Console Input"));		
		inputWrapper.add(consoleInput);

		outputWrapperPanel = new SplitOutErrPanel();


		inputHistorWrapperPanel = new JPanel();
		inputHistorWrapperPanel.setLayout(new BorderLayout());
		;
		inputHistorWrapperPanel.setBorder(BorderFactory.createTitledBorder("Input History"));



		JSplitPane ioSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outputWrapperPanel.getSplitPane(), inputHistorWrapperPanel);
		ioSplit.setResizeWeight(0.9);

		ioPanel.add(ioSplit, BorderLayout.CENTER);
		ioPanel.add(inputWrapper, BorderLayout.SOUTH);
		setVisible(true);
		rubricTabbedPane = new JTabbedPane();

		sourceTabbedPane = new JTabbedPane();
		sourceTabbedPane.setComponentPopupMenu(popupSource);

		overallTabbedPane = new JTabbedPane();
		overallTabbedPane.addTab("Source", sourceTabbedPane);
		overallTabbedPane.addTab("Console", ioPanel);
		overallTabbedPane.addTab("Rubric", rubricTabbedPane);
		add(overallTabbedPane, BorderLayout.CENTER);

		consoleInput.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				if (e.getType() == DocumentEvent.EventType.CHANGE || e.getType() == DocumentEvent.EventType.INSERT) {
					String text = consoleInput.getText();

					int returnIndex = text.indexOf("\n");
					if (returnIndex != -1) {
						String inputText = text.substring(0, returnIndex);

						ListenerCoordinator.fire(SystemInListener.class, inputText);
						SwingUtilities.invokeLater(new Runnable() {
							public void run() {								
								if (text.length() > returnIndex + 1) {
									consoleInput.setText(text.substring(returnIndex + 1));
								}
								else {
									consoleInput.setText("");
								}
							}
						});
					}					
				}				
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				
				
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				// TODO Auto-generated method stub
				
			}
		});

	}

	private void registerListeners() {
		ListenerCoordinator.addListener(AddRubricTabsListener.class, new AddRubricTabsListener() {
			@Override
			public void fired(Rubric rubric) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						rubricTabbedPane.removeAll();
						rubricPanels.clear();
						if (rubric != null) {
							List<String> tabNames = rubric.getRubricTabs();					
							for (String rubricName : tabNames) {
								SplitOutErrPanel rubricPanel = new SplitOutErrPanel();
								rubricPanels.put(rubricName, rubricPanel);
								rubricTabbedPane.addTab(rubricName, rubricPanel.getSplitPane());
							}
						}
					}
				});
			}
		});

		ListenerCoordinator.addBlockingListener(PreRunBlockingListener.class, new PreRunBlockingListener() {
			public void fired(String studentID, String rubricName) {
				if (rubricName == null || rubricName.length() == 0) {
					overallTabbedPane.setSelectedIndex(1);
				}
				else {
					if (overallTabbedPane.getTabCount() > 2) {
						overallTabbedPane.setSelectedIndex(2);						
						for (int i = 0; i < rubricTabbedPane.getTabCount(); i++) {
							String title = rubricTabbedPane.getTitleAt(i); 
							if (title.equals(rubricName)) {								
								rubricTabbedPane.setSelectedIndex(i);
								break;
							}
						}
					}
				}
				try {
					// Doing this prevents forward progress until the panes are ready
					pauseSemaphore.release();
					pauseSemaphore.acquire();
					setWindowData(studentID);
					// We will now hang here until the release in setWindowData
					pauseSemaphore.acquire();
					pauseSemaphore.release();
				} catch (InterruptedException e) {
				}
			}

		});

		ListenerCoordinator.addListener(AssignmentSelected.class, new AssignmentSelected() {
			@Override
			public void fired(ClassroomData data) {
				if (data == null || data.isEmpty()) {
					return;
				}
				assignmentSelected();
			}
		});
		
		ListenerCoordinator.addListener(StudentSelectedListener.class, new StudentSelectedListener() {

			@Override
			public void fired(String idToDisplay) {
				setWindowData(idToDisplay);
			}
		});
		


	}
	
	private void clearPanel(JPanel panel) {
		while (panel.getComponentCount() != 0) {
			panel.remove(0);
		}
		
	}
	
	private void clearAndAddPanel(JPanel panel, Component componentToAdd) {
		clearPanel(panel);
		if (componentToAdd != null) {
			panel.add(new JScrollPane(componentToAdd));
		}
	}
	private void bindStudentAreas(String studentID) {
		if (studentID != null) {
			StudentConsoleAreas currentAreas = (StudentConsoleAreas)ListenerCoordinator.runQuery(GetStudentTextAreasQuery.class, studentID);
			outputWrapperPanel.clearAndAdd(currentAreas.getOutputAreas());
			currentInputHistory = currentAreas.getInputArea();
			clearAndAddPanel(inputHistorWrapperPanel, currentInputHistory);
			currentInputHistory.setComponentPopupMenu(popupDisplays);
			Set<String> rubricKeys = rubricPanels.keySet();
			for (String rubricName : rubricKeys) {
				rubricPanels.get(rubricName).clearAndAdd(currentAreas.getRubricArea(rubricName));
			}
		}
		else {
			outputWrapperPanel.clearPanels();
			currentInputHistory = null;
			clearPanel(inputHistorWrapperPanel);
			Set<String> rubricKeys = rubricPanels.keySet();
			for (String rubricName : rubricKeys) {
				rubricPanels.get(rubricName).clearPanels();
			}
		}
	}


}
