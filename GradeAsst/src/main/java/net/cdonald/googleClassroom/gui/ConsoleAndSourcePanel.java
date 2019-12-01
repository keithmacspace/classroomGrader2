package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
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
import javax.swing.JTextPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.text.Element;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabsListener;
import net.cdonald.googleClassroom.listenerCoordinator.AssignmentSelected;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentTextAreasQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.PreRunBlockingListener;
import net.cdonald.googleClassroom.listenerCoordinator.RecompileListener;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveSourceListener;
import net.cdonald.googleClassroom.listenerCoordinator.SelectStudentListener;
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
	private JTabbedPane outputTabbedPane;
	//private JTextField consoleInput;
	private JTextArea consoleInput;
	private JPopupMenu popupSource;
	private JPopupMenu popupInput;
	private JPopupMenu popupDisplays;
	private JPopupMenu popupRubricSource;
	private SplitOutErrPanel outputWrapperPanel;
	
	private Map<String, SplitOutErrPanel> rubricPanels;
	private static Semaphore pauseSemaphore = new Semaphore(1);
	private JTextArea currentInputHistory;
	private String currentID;
	private List<JTextArea> currentSourceTextAreas;
	private Map<String, JTextArea> modifiedRubricTestCodeMap;
	private UndoManager undoManager;
	private Map<String, Map<String, JPanel>> sourceContentPanelMap;
	private Map<String, Map<String, JTextArea>> sourceContentTextMap;
	private enum TabNames {Source, Rubric}
	private JTabbedPane rubricTestPane = new JTabbedPane();


	public ConsoleAndSourcePanel(UndoManager undoManager) {
		this.undoManager = undoManager;
		
		setMinimumSize(new Dimension(400, 400));
		createPopupMenu();
		createLayout();
		registerListeners();
		setVisible(true);
		sourceContentTextMap = new HashMap<String, Map<String, JTextArea>>();
		sourceContentPanelMap = new HashMap<String, Map<String, JPanel>>();
		currentSourceTextAreas = new ArrayList<JTextArea>();
		rubricPanels = new HashMap<String, SplitOutErrPanel>();
		modifiedRubricTestCodeMap = new HashMap<String, JTextArea>();

	}

	public void assignmentSelected() {
		sourceTabbedPane.removeAll();
		consoleInput.setText("");
		sourceContentTextMap.clear();
		sourceContentPanelMap.clear();
		currentSourceTextAreas.clear();
	}
	
	public void syncSource() {
		if (currentID != null) {
			List<FileData> fileDataList = (List<FileData>) ListenerCoordinator.runQuery(GetStudentFilesQuery.class, currentID);			
			if (fileDataList != null) {
				for (FileData fileData : fileDataList) {
					String name = fileData.getName();
					for (int i = 0; i < sourceTabbedPane.getTabCount(); i++) {
						if (sourceTabbedPane.getTitleAt(i).equals(name)) {						
							fileData.setFileContents(currentSourceTextAreas.get(i).getText());
						}
					}
				}
			}
		}
	}


	public void setWindowData(String idToDisplay) {
		
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {

				if (currentID == null || !currentID.equals(idToDisplay)) {
					syncSource();
					currentSourceTextAreas.clear();
					sourceTabbedPane.removeAll();
					currentID = idToDisplay;
					if (idToDisplay != null) {
						@SuppressWarnings("unchecked")
						List<FileData> fileDataList = (List<FileData>) ListenerCoordinator.runQuery(GetStudentFilesQuery.class, idToDisplay);										
						if (fileDataList != null) {
							for (FileData fileData : fileDataList) {
								setSourceContents(currentID, fileData.getName(), fileData.getFileContents(), true);
							}
						}
					}
				}
				bindStudentAreas(idToDisplay);
			}

		});
		
	}
	
	public void refreshInfo() {
		if (currentID != null) {
			setWindowData(currentID);
		}
	}
	
	private static String getLineNumber(int number, int lastNumber) {
		String lineNum = "" + number;
		int stopPoint = (int)Math.log10(number);
		for (int i = (int)Math.log10(10000); i > stopPoint; i--) {		
			lineNum += " ";
		}
		if (number < lastNumber - 1) {
			lineNum += System.getProperty("line.separator");
		}
		return lineNum;
	}
	
	private void addLineNumbers(JScrollPane jsp, JTextArea jta) {		
		JTextArea lines = new JTextArea(getLineNumber(1, 1));
		Font font = new Font("monospaced", Font.PLAIN, jta.getFont().getSize());
		jta.setFont(font);
		lines.setFont(jta.getFont());
		
 
		lines.setEditable(false);
 
		jta.getDocument().addDocumentListener(new DocumentListener(){
			private int lastLineNumber = 0;
			private void changeLineNumbers() {
				int caretPosition = jta.getDocument().getLength();
				Element root = jta.getDocument().getDefaultRootElement();
				int lastNumber = root.getElementIndex( caretPosition ) + 2;
				if (lastNumber != lastLineNumber) {
					caretPosition = lines.getCaretPosition();
					lines.setText(getText(lastNumber));				
					lines.setCaretPosition(caretPosition);
					lastLineNumber = lastNumber;
				}
			}
			public String getText(int lastNumber){
				String text = getLineNumber(1, lastNumber);

				for(int i = 2; i < lastNumber; i++){
					text += getLineNumber(i, lastNumber);
				}
				return text;
			}
			@Override
			public void changedUpdate(DocumentEvent de) {
				changeLineNumbers();
			}
 
			@Override
			public void insertUpdate(DocumentEvent de) {
				changeLineNumbers();
			}
 
			@Override
			public void removeUpdate(DocumentEvent de) {
				changeLineNumbers();
			}
 
		});
		
		jsp.getViewport().add(jta);
		jsp.setRowHeaderView(lines);
		jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	}
		
	private void setSourceContents(String studentId, String title, String text, boolean editable) {
		if (sourceContentPanelMap.containsKey(studentId) == false) {
			sourceContentPanelMap.put(studentId, new HashMap<String, JPanel>());
			sourceContentTextMap.put(studentId, new HashMap<String, JTextArea>());
		}
		Map<String, JPanel> panelMap = sourceContentPanelMap.get(studentId);
		Map<String, JTextArea> textMap = sourceContentTextMap.get(studentId);
		if (panelMap.containsKey(title) == false) {
			JPanel sourcePanel = new JPanel();
			sourcePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			sourcePanel.setLayout(new BorderLayout());
			JTextArea sourceArea = new JTextArea();
			JScrollPane jsp = new JScrollPane();
			if (editable) {
				addLineNumbers(jsp, sourceArea);
			}
			sourceArea.setEditable(editable);
			sourceArea.setText(text);
			if (editable) {
				sourceArea.setComponentPopupMenu(popupSource);
				sourceArea.getDocument().addUndoableEditListener(new UndoableSourceEdit(title, true));
			}
			else {
				jsp.getViewport().add(sourceArea);
			}
			sourcePanel.add(jsp);
			sourceArea.setCaretPosition(0);
			if (editable ) {
				panelMap.put(title, sourcePanel);
				textMap.put(title, sourceArea);
			}
			else {
				currentSourceTextAreas.add(sourceArea);
				sourceTabbedPane.addTab(title, sourcePanel);
				return;
			}
		}
		
		JPanel sourcePanel = panelMap.get(title);
		JTextArea sourceArea = textMap.get(title);				
		
		currentSourceTextAreas.add(sourceArea);
		sourceTabbedPane.addTab(title, sourcePanel);
				
	}

	private void createPopupMenu() {
		popupSource = new JPopupMenu();
		popupDisplays = new JPopupMenu();
		popupInput = new JPopupMenu();
		popupRubricSource = new JPopupMenu();

		Action cut = new DefaultEditorKit.CutAction();
		cut.putValue(Action.NAME, "Cut");
		cut.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control X"));
		popupSource.add(cut);
		popupInput.add(cut);
		popupRubricSource.add(cut);

		Action copy = new DefaultEditorKit.CopyAction();
		copy.putValue(Action.NAME, "Copy");
		copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
		popupSource.add(copy);
		popupInput.add(cut);
		popupDisplays.add(copy);
		popupRubricSource.add(copy);

		Action paste = new DefaultEditorKit.PasteAction();
		paste.putValue(Action.NAME, "Paste");
		paste.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
		popupSource.add(paste);
		popupInput.add(paste);
		popupRubricSource.add(paste);
		
		JMenuItem recompile = new JMenuItem("Recompile");
		popupSource.add(recompile);
		

		recompile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (currentID != null) {
					int currentTab = sourceTabbedPane.getSelectedIndex();
					String fileName = sourceTabbedPane.getTitleAt(currentTab);
					if (currentTab < currentSourceTextAreas.size()) {
						JTextArea currentSourceArea = currentSourceTextAreas.get(currentTab);
						String sourceText = currentSourceArea.getText();
						String formerID = currentID;
						currentID = null; // Force a refresh of the windows
						ListenerCoordinator.fire(RecompileListener.class, formerID, fileName, sourceText);
					}
				}

			}
		});
		
		JMenuItem removeSource = new JMenuItem("Remove Source");
		popupSource.add(removeSource);		
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
		public SplitOutErrPanel() {
			out = new JPanel();
			out.setLayout(new BorderLayout());			
			out.setBorder(BorderFactory.createTitledBorder("System.out"));
		}
		
		public JPanel getOutputPane() {
			return out;
		}

		public void clearPanels() {
			clearPanel(out);
		}
		public void clearAndAdd(StudentConsoleAreas.OutputAreas outputAreas) {
			JTextPane output = outputAreas.getOutputArea();
			clearAndAddPanel(out, output);
			output.setComponentPopupMenu(popupDisplays);
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

		JPanel consoleInputPanel = new JPanel();
		consoleInputPanel.setLayout(new BorderLayout());
		
		consoleInputPanel.setBorder(BorderFactory.createTitledBorder("Console Input"));		
		consoleInputPanel.add(consoleInput, BorderLayout.CENTER);
		JPanel inputHistorWrapperPanel = new JPanel();
		inputHistorWrapperPanel.setLayout(new BorderLayout());
		currentInputHistory = new JTextArea();
		inputHistorWrapperPanel.add(new JScrollPane(currentInputHistory), BorderLayout.CENTER);
		currentInputHistory.setComponentPopupMenu(popupDisplays);
		
		inputHistorWrapperPanel.setBorder(BorderFactory.createTitledBorder("Input History"));
		
		JPanel inputWrapper = new JPanel();
		inputWrapper.setLayout(new BorderLayout());
		inputWrapper.add(consoleInputPanel, BorderLayout.NORTH);
//		inputWrapper.add(inputHistorWrapperPanel, BorderLayout.CENTER);


		outputWrapperPanel = new SplitOutErrPanel();


		


		//JSplitPane ioSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, outputWrapperPanel.getOutputPane(), inputWrapper);
		//ioSplit.setResizeWeight(0.5);

		ioPanel.add(outputWrapperPanel.getOutputPane(), BorderLayout.CENTER);
		ioPanel.add(inputWrapper, BorderLayout.SOUTH);
		setVisible(true);
		rubricTabbedPane = new JTabbedPane();

		sourceTabbedPane = new JTabbedPane();
		sourceTabbedPane.setComponentPopupMenu(popupSource);
		
		outputTabbedPane = new JTabbedPane();
		JSplitPane sourceOutputSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, sourceTabbedPane, outputTabbedPane);
		sourceOutputSplit.setResizeWeight(0.7);
		outputTabbedPane.addTab("Console", ioPanel);
		outputTabbedPane.addTab("In Hist", inputHistorWrapperPanel);

		overallTabbedPane = new JTabbedPane();
		overallTabbedPane.addTab(TabNames.Source.toString(), sourceOutputSplit);
		overallTabbedPane.addTab(TabNames.Rubric.toString(), rubricTabbedPane);
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
						currentInputHistory.append(inputText + "\n");
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
							List<FileData> referenceSourceArea = rubric.getReferenceSource();
							if (referenceSourceArea != null && referenceSourceArea.size() > 0) {
								JTabbedPane referenceSourceTabs = new JTabbedPane();
								rubricTabbedPane.addTab("Reference Source", referenceSourceTabs);
								for (FileData file : referenceSourceArea) {
									JTextArea goldSource = new JTextArea();
									goldSource.setComponentPopupMenu(popupDisplays);
									goldSource.setEditable(false);
									goldSource.setText(file.getFileContents());
									referenceSourceTabs.addTab(file.getName(), new JScrollPane(goldSource));
								}
							}
							List<FileData> rubricTestCode = rubric.getTestCode();
							rubricTestPane.removeAll();
							if (rubricTestCode != null && rubricTestCode.size() > 0) {								
								rubricTabbedPane.addTab("Test Code Source", rubricTestPane);
								for (FileData file : rubricTestCode) {
									JTextArea testSource = new JTextArea();
									JScrollPane jsp = new JScrollPane();
									addLineNumbers(jsp, testSource);
									//jsp.getViewport().add(testSource);
									testSource.setComponentPopupMenu(popupSource);
									testSource.setEditable(true);
									testSource.setText(file.getFileContents());
									testSource.getDocument().addUndoableEditListener(new UndoableSourceEdit(file.getName(), false));
									testSource.getDocument().addDocumentListener(new DocumentListener() {

										@Override
										public void insertUpdate(DocumentEvent e) {
											modifiedRubricTestCodeMap.put(file.getName(), testSource);
										}

										@Override
										public void removeUpdate(DocumentEvent e) {
											modifiedRubricTestCodeMap.put(file.getName(), testSource);
										}

										@Override
										public void changedUpdate(DocumentEvent e) {
										}										
									});
									rubricTestPane.addTab(file.getName(), jsp);
								}
							}
							while (outputTabbedPane.getTabCount() > 2) {
								outputTabbedPane.removeTabAt(2);
							}

							List<String> tabNames = rubric.getRubricTabs();					
							for (String rubricName : tabNames) {
								SplitOutErrPanel rubricPanel = new SplitOutErrPanel();
								rubricPanels.put(rubricName, rubricPanel);
								outputTabbedPane.addTab(rubricName, rubricPanel.getOutputPane());
							}
						}
					}
				});
			}
		});

		ListenerCoordinator.addBlockingListener(PreRunBlockingListener.class, new PreRunBlockingListener() {
			public void fired(String studentID, String rubricName) {
				if (rubricName == null || rubricName.length() == 0) {
					outputTabbedPane.setSelectedIndex(0);
					String studentName = (String)ListenerCoordinator.runQuery(GetStudentNameQuery.class, studentID);
					currentInputHistory.append(studentName + " Run Started\n");
				}
				else {
					if (overallTabbedPane.getTabCount() > TabNames.Source.ordinal()) {
						overallTabbedPane.setSelectedIndex(TabNames.Source.ordinal());
						selectOutputTab(rubricName);
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
	
	public JTextArea getCurrentSource() {
		if (currentID != null) {
			overallTabbedPane.setSelectedIndex(TabNames.Source.ordinal());
			int currentTab = sourceTabbedPane.getSelectedIndex();			
			if (currentTab < currentSourceTextAreas.size()) {
				JTextArea currentSourceArea = currentSourceTextAreas.get(currentTab);
				return currentSourceArea;
			}
		}
		return null;
	}
	
	public void selectOutputTab(String tabName) {		
		for (int i = 0; i < outputTabbedPane.getTabCount(); i++) {
			String title = outputTabbedPane.getTitleAt(i); 
			if (title.equals(tabName)) {				
				outputTabbedPane.setSelectedIndex(i);
				break;
			}
		}		
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
			//currentInputHistory = currentAreas.getInputArea();
			//clearAndAddPanel(inputHistorWrapperPanel, currentInputHistory);			
			Set<String> rubricKeys = rubricPanels.keySet();
			for (String rubricName : rubricKeys) {
				rubricPanels.get(rubricName).clearAndAdd(currentAreas.getRubricArea(rubricName));
			}
		}
		else {
			outputWrapperPanel.clearPanels();
			//currentInputHistory = null;
			//currentInputHistory.setText("");
			//clearPanel(inputHistorWrapperPanel);			
			for (SplitOutErrPanel rubricPanel : rubricPanels.values()) {
				rubricPanel.clearPanels();
			}
		}
	}
	
	public void updateRubricTestCode(Rubric rubric) {		
		for (String file : modifiedRubricTestCodeMap.keySet()) {
			rubric.modifyTestCode(file, modifiedRubricTestCodeMap.get(file).getText());
		}
		modifiedRubricTestCodeMap.clear();
	}
    //This one listens for edits that can be undone.
    protected class UndoableSourceEdit  implements UndoableEditListener {
    	private String tabTitle;
    	private boolean isStudentSource;
        public UndoableSourceEdit(String string, boolean isStudentSource) {
        	tabTitle = string;
        	this.isStudentSource = isStudentSource;
		}

		public void undoableEditHappened(UndoableEditEvent e) {
            //Remember the edit and update the menus.
            undoManager.addEdit(new UndoableTabEvent(e.getEdit(), tabTitle));

        }
		protected class UndoableTabEvent  implements UndoableEdit {
			UndoableEdit actualEdit;
			String tabTitle;
			String currentStudent;

			public UndoableTabEvent(UndoableEdit actualEdit, String tabTitle) {
				super();
				this.currentStudent = currentID;
				this.actualEdit = actualEdit;
				this.tabTitle = tabTitle;
			}
			private void selectSpecificTab(JTabbedPane tabbedPane, String tabTitle) {
				if (!tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()).equals(tabTitle)) {
					for (int i = 0; i < tabbedPane.getTabCount(); i++) {
						if (tabbedPane.getTitleAt(i).equals(tabTitle)) {
							tabbedPane.setSelectedIndex(i);
							break;
						}
					}
				}				
			}
			
			protected void selectTab() {
				if (isStudentSource) {
					if (!currentID.equals(currentStudent)) {
						setWindowData(currentStudent);
						ListenerCoordinator.fire(SelectStudentListener.class, currentStudent);
					}
					overallTabbedPane.setSelectedIndex(TabNames.Source.ordinal());
					selectSpecificTab(sourceTabbedPane, tabTitle);
				}
				else {
					if (overallTabbedPane.getTabCount() > TabNames.Rubric.ordinal()) {
						overallTabbedPane.setSelectedIndex(TabNames.Rubric.ordinal());
						
						selectSpecificTab(rubricTabbedPane, "Test Code Source");
						selectSpecificTab(rubricTestPane, tabTitle);
					}
				}
			}

			@Override
			public void undo() throws CannotUndoException {
				selectTab();
				actualEdit.undo();
				
			}

			@Override
			public boolean canUndo() {
				return actualEdit.canUndo();
			}

			@Override
			public void redo() throws CannotRedoException {
				selectTab();
				actualEdit.redo();
				
			}

			@Override
			public boolean canRedo() {
				return actualEdit.canRedo();
			}

			@Override
			public void die() {
				actualEdit.die();				
			}

			@Override
			public boolean addEdit(UndoableEdit anEdit) {
				return actualEdit.addEdit(anEdit);
			}

			@Override
			public boolean replaceEdit(UndoableEdit anEdit) {
				return actualEdit.replaceEdit(anEdit);
			}

			@Override
			public boolean isSignificant() {
				return actualEdit.isSignificant();
			}

			@Override
			public String getPresentationName() {
				return actualEdit.getPresentationName();
			}

			@Override
			public String getUndoPresentationName() {
				return actualEdit.getUndoPresentationName();
			}

			@Override
			public String getRedoPresentationName() {
				return actualEdit.getRedoPresentationName();
			}
			
		}
    }
    



}
