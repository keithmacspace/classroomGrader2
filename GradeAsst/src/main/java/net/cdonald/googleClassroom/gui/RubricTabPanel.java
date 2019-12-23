package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.UndoManager;

import net.cdonald.googleClassroom.gui.SetRubricListener.RubricType;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentIDListQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LoadSourceQuery;
import net.cdonald.googleClassroom.listenerCoordinator.RubricTestCodeChanged;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;

public class RubricTabPanel extends JPanel {
	private static final long serialVersionUID = -938409512740386882L;
	private JCheckBox enableEditingBox;
	private JTabbedPane rubricTabbedPane;	
	private Map<RubricTabNames, Map<String, JTextArea> > sourceCodeMap;
	private Map<RubricTabNames, JTabbedPane> sourceTabsMap;
	private Map<RubricTabNames, RubricSourcePanel> sourcePanelMap;
	private JButton saveRubricButton;
	private JButton cancelChangesButton;
	private JPopupMenu editablePopup;
	private JPopupMenu displayPopup;	
	private Rubric referenceRubric;
	private Rubric rubric;
	private UndoManager undoManager;
	private RubricSummaryPanel rubricSummaryPanel;
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
		sourceCodeMap = new HashMap<RubricTabNames, Map<String, JTextArea>>();
		sourceTabsMap = new HashMap<RubricTabNames, JTabbedPane>();
		sourcePanelMap = new HashMap<RubricTabNames, RubricSourcePanel>();
		createLayout(undoManager);
		registerListeners();
	}
	private List<FileData> createFileData(RubricTabNames rubricTab) {
		Map<String, JTextArea> sourceMap = sourceCodeMap.get(rubricTab);
		List<FileData> fileData = new ArrayList<FileData>();
		if (sourceMap != null) {			
			for (String name : sourceMap.keySet()) {
				fileData.add(new FileData(name, sourceMap.get(name).getText(), rubricTab.toString(), null));
			}
		}
		return fileData;
	}

	public void updateRubricCode() {
		if (rubric != null) {
			rubric.setReferenceSource(createFileData(RubricTabNames.Reference));
			rubric.setTestCode(createFileData(RubricTabNames.TestCode));
		}
	}

	private void createLayout(UndoManager undoManager) {
		setLayout(new BorderLayout());
		createPopupMenu();
		addButtonBar();	
		rubricSummaryPanel = new RubricSummaryPanel(undoManager);
		sourcePanelMap.put(RubricTabNames.Reference, new RubricSourcePanel(RubricTabNames.Reference));
		sourcePanelMap.put(RubricTabNames.TestCode, new RubricSourcePanel(RubricTabNames.TestCode));
		rubricTabbedPane = new JTabbedPane();		
		rubricTabbedPane.addTab(RubricTabNames.Reference.toString(), sourcePanelMap.get(RubricTabNames.Reference));
		rubricTabbedPane.addTab(RubricTabNames.TestCode.toString(), sourcePanelMap.get(RubricTabNames.TestCode));

		
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
		ListenerCoordinator.fire(SetRubricListener.class, rubric, RubricType.PRIMARY);
	}
	private void addButtonBar() {
		JPanel buttonBar = new JPanel();
		buttonBar.setLayout(new FlowLayout(FlowLayout.LEFT));		
		enableEditingBox = new JCheckBox("Enable Editing", false);
		saveRubricButton = new JButton("Save");
		cancelChangesButton = new JButton("Cancel");
		saveRubricButton.setPreferredSize(cancelChangesButton.getPreferredSize());		

		enableEditingBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				enableEditing(enableEditingBox.isSelected());
			}
		});
		
		saveRubricButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAllChanges();				
			}
		});
		
		cancelChangesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(SetRubricListener.class, referenceRubric, RubricType.PRIMARY);				
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
				saveRubricButton.setVisible(enableEditing);
				cancelChangesButton.setVisible(enableEditing);
				enableSourceEditing(enableEditing);
				for (RubricSourcePanel sourcePanel : sourcePanelMap.values()) {
					sourcePanel.enableEditing(enableEditing);
				}
				enableEditingBox.setVisible(!enableEditing);
				if (enableEditing == true && rubric != null) {
					referenceRubric = rubric;
					rubric = new Rubric(rubric);
					ListenerCoordinator.fire(SetRubricListener.class, rubric, RubricType.RUBRIC_BEING_EDITED);
					rubricTabbedPane.addTab(RubricTabNames.Summary.toString(), rubricSummaryPanel);
					rubricSummaryPanel.enableEditing(rubric);
				}
				if (enableEditing == false) {			
					rubricTabbedPane.remove(rubricSummaryPanel);
				}
			}
		});
		
	}

	private void enableSourceEditing(boolean enableEditing) {
		for (Map<String, JTextArea> sourceMap : sourceCodeMap.values()) {
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
					rubric = localRubric;
					addSources();
					enableEditingBox.setSelected(false);
					enableEditing(false);
				}
			}
		});
	}
	
	private void addSources() {
		if (rubric != null) {
			List<FileData> referenceSoure = rubric.getReferenceSource();
			addSource(referenceSoure, RubricTabNames.Reference);						
			List<FileData> rubricTestCode = rubric.getTestCode();
			addSource(rubricTestCode, RubricTabNames.TestCode);						
		}

	}

	private JTabbedPane addSource(List<FileData> files, RubricTabNames tabName) {
		Map<String, JTextArea> sourceMap = sourceCodeMap.get(tabName);
		if (sourceMap == null) {
			sourceCodeMap.put(tabName, new HashMap<String, JTextArea>());
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
				}
			}
		}
		enableSourceEditing(enableEditingBox.isSelected());
		sourceTabsMap.put(tabName, sourceTabs);
		sourcePanelMap.get(tabName).setSourceTabs(sourceTabs);
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

		RubricSourcePanel(RubricTabNames tabName) {
			super();
			this.tabName = tabName;
			createLayout();		
		}

		public void enableEditing(boolean enableEditing) {
			buttonSource.setVisible(enableEditing);
			if (enableEditing && tabName == RubricTabNames.Reference && studentTable == null) {
				@SuppressWarnings("unchecked")
				List<String> allIDs = (ArrayList<String>)ListenerCoordinator.runQuery(GetStudentIDListQuery.class);
				if (allIDs != null && allIDs.size() > 0) {
					Object[][] names = new Object[allIDs.size()][1];
					
					
					for (int i = 0; i < allIDs.size(); i++) {
						names[i][0] = ListenerCoordinator.runQuery(GetStudentNameQuery.class, allIDs.get(i));
					}
					studentTable = new JTable(names, new String[] {"Source To Use"});
					studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					studentTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
						
	
						@Override
						public void valueChanged(ListSelectionEvent e) {
							if (e.getValueIsAdjusting()) {
								return;
							}
							SwingUtilities.invokeLater(new Runnable() {

								@Override
								public void run() {
									ListSelectionModel lsm = (ListSelectionModel) e.getSource();
									if (lsm.isSelectionEmpty() == false) {
										int selectedRow = lsm.getMinSelectionIndex();								
										String id = allIDs.get(selectedRow);
										@SuppressWarnings( "unchecked" )
										List<FileData> studentSource = (List<FileData>)ListenerCoordinator.runQuery(GetStudentFilesQuery.class, id);
										addSource(studentSource, tabName);							
									}										
								}
								
							});
					
						}					
					});
					buttonSource.add(new JScrollPane(studentTable), BorderLayout.CENTER);
				}
			}

			revalidate();
		}

		public void setSourceTabs(JTabbedPane sourceTabs) {
			for (int i = 0; i < this.getComponentCount(); i++) {
				if (this.getComponent(i) != buttonSource) {
					this.remove(i);
				}
			}
			add(sourceTabs, BorderLayout.CENTER);
			revalidate();
		}

		private void createLayout() {
			setLayout(new BorderLayout());
			buttonSource = new JPanel();		
			buttonSource.setLayout(new BorderLayout());
			JButton loadSource = new JButton("Load Source");
			loadSource.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							@SuppressWarnings("unchecked")
							List<FileData> allFiles = (List<FileData>)ListenerCoordinator.runQuery(LoadSourceQuery.class);
							addSource(allFiles, tabName);							
						}						
					});					
				}				
			});
			
			buttonSource.add(loadSource, BorderLayout.NORTH);
			add(buttonSource, BorderLayout.EAST);
		}
	}


}
