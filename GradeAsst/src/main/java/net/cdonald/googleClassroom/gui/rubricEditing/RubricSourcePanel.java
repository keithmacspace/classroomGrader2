package net.cdonald.googleClassroom.gui.rubricEditing;

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
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.UndoManager;

import net.cdonald.googleClassroom.gui.LineNumberTextArea;
import net.cdonald.googleClassroom.gui.TabbedUndoListener;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentIDListQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LoadSourceQuery;
import net.cdonald.googleClassroom.model.FileData;

public class RubricSourcePanel extends JPanel {
	private static final long serialVersionUID = 4659401187763797680L;
	private JPanel buttonSource;
	private RubricTabPanel.RubricTabNames tabName;
	private JTable studentTable; 	
	private List<String> allIDs;
	private JTabbedPane sourceTabPane;
	private JPanel studentPanel;
	private boolean isModified;
	private  Map<String, LineNumberTextArea>  sourceCodeMap;
	private JPopupMenu editablePopup;
	private JPopupMenu displayPopup;
	private boolean enableEditing;
	private UndoManager undoManager;
	private RubricFileListener rubricFileListener;

	RubricSourcePanel(UndoManager undoManager, RubricTabPanel.RubricTabNames tabName, RubricFileListener rubricFileListener) {
		super();
		this.rubricFileListener = rubricFileListener;
		this.undoManager = undoManager;
		this.tabName = tabName;
		this.rubricFileListener = rubricFileListener;
		enableEditing = false;
		sourceCodeMap = new HashMap<String, LineNumberTextArea>();
		isModified = false;
		setVisible(true);
		createPopupMenu();
		createLayout();		
	}

	public void setSourceTabs(List<FileData> files, boolean setModified) {
		JTabbedPane sourceTabs = new JTabbedPane();
		sourceCodeMap.clear();		
		new TabbedUndoListener(undoManager, sourceTabs);
		if (files != null) {						
			if (files != null && files.size() > 0) {								
				for (FileData file : files) {
					file.setFileContents(FileData.stripPackage(file.getFileContents()));
					LineNumberTextArea testSource = new LineNumberTextArea();
					testSource.setText(file.getFileContents());
					sourceTabs.addTab(file.getName(), testSource.getScrollPane());									
					testSource.getDocument().addUndoableEditListener(undoManager);
					sourceCodeMap.put(file.getName(), testSource);
					testSource.setModified(setModified);
				}
			}
		}
		rubricFileListener.sourceIsChanged();
		setSourceTabs(sourceTabs);
		enableEditing(enableEditing);
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

	
	public void enableEditing(boolean enableEditing) {
		this.enableEditing = enableEditing;
		buttonSource.setVisible(enableEditing);
		if (studentPanel != null) {
			studentPanel.setVisible(enableEditing);
		}
		for (LineNumberTextArea textArea : sourceCodeMap.values()) {
			if (textArea != null) {
				textArea.setEditable(enableEditing);
				textArea.setComponentPopupMenu(enableEditing ? editablePopup : displayPopup);			

			}
		}
	}
	
	public JButton getSourceButton(boolean addSource) {
		JButton loadSource = new JButton(addSource ? "Add Source" : "Load Source");
		loadSource.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (enableEditing) {
					SwingUtilities.invokeLater(new Runnable() {
						@Override
						public void run() {
							@SuppressWarnings("unchecked")
							List<FileData> allFiles = (List<FileData>)ListenerCoordinator.runQuery(LoadSourceQuery.class);
							if (addSource) {
								List<FileData> currentFiles = createFileData(tabName.toString());
								allFiles.addAll(currentFiles);
							}
							setSourceTabs(allFiles, true);
							rubricFileListener.compileSource(tabName);
						}						
					});					
				}
			}				
		});
		return loadSource;
	}
	
	
	public JPanel getSourceButtons() {

		JPanel buttonSource;
		buttonSource = new JPanel();		
		buttonSource.setLayout(new GridBagLayout());
		for (int i = 0; i < 2; i++) {
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(3, 3, 3, 0);
			c.weightx = 0.3;
			c.weighty = 0.3;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.LINE_START;
			//c.gridwidth = GridBagConstraints.REMAINDER;
			//c.gridheight = 1;
			c.gridx = 0;
			c.gridy = i;
			buttonSource.add(getSourceButton(i != 0), c);
		}
		return buttonSource;
	}



	public void setModified(boolean isModified) {
		this.isModified = isModified;
	}
	
	public List<FileData> createFileData(String fileID) {		
		List<FileData> fileData = new ArrayList<FileData>();
		if (sourceTabPane != null) {
			for (String name : sourceCodeMap.keySet()) {
				fileData.add(new FileData(name, sourceCodeMap.get(name).getText(), fileID, null));
			}
		}
		return fileData;
	}
	public boolean isModified() {
		if (isModified == false) {
			if (sourceCodeMap != null) {
				for (LineNumberTextArea textArea : sourceCodeMap.values()) {
					if (textArea.isModified()) {
						return true;
					}
				}
			}
		}
		return isModified;		
	}
	
	public boolean containsFiles() {
		return sourceCodeMap != null && sourceCodeMap.size() > 0;
	}
	
	private void createLayout() {
		
		createStudentTable(tabName);
//		if (studentPanel != null) {
//			add(studentPanel, BorderLayout.CENTER);				
//		}
		buttonSource = getSourceButtons();
		//add(buttonSource, BorderLayout.NORTH);

		setLayout(new GridBagLayout());


		
		GridBagConstraints c = new GridBagConstraints();
		//			c.insets = new Insets(3, 3, 3, 0);
		c.weightx = tabName == RubricTabPanel.RubricTabNames.TestCode ? 0.1 : 1.0;
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
										setSourceTabs(studentSource, true);
										rubricFileListener.compileSource(tabName);
									}
								}
							}
						}
					}

				});				
			}					
		});
	}
	public void createStudentTable(RubricTabPanel.RubricTabNames tabName) {
		studentPanel = null;
		if (tabName == RubricTabPanel.RubricTabNames.Reference) {
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

		@SuppressWarnings("unchecked")
		private void setStudentNames(int row) {
			if (allIDs == null  || row >= allIDs.size()) {
				allIDs = (ArrayList<String>)ListenerCoordinator.runQuery(GetStudentIDListQuery.class);
			}
		}
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
		



		JMenuItem compileSource = new JMenuItem("Compile");
		editablePopup.add(compileSource);		
		compileSource.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				rubricFileListener.compileSource(tabName);
			}
		});


		JMenuItem removeSource = new JMenuItem("Remove Source");
		editablePopup.add(removeSource);		
		removeSource.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				isModified = true;
				int currentTab = sourceTabPane.getSelectedIndex();
				String title = sourceTabPane.getTitleAt(currentTab);
				sourceCodeMap.remove(title);												
				sourceTabPane.removeTabAt(currentTab);
			}
		});
	}
}