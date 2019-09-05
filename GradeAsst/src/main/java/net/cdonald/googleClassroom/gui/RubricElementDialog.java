package net.cdonald.googleClassroom.gui;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabsListener;
import net.cdonald.googleClassroom.listenerCoordinator.GetFileDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RunRubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SaveRubricListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetFileDirListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentInfoChangedListener;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;


public class RubricElementDialog extends JDialog implements RubricElementListener {
	private static final long serialVersionUID = -5580080426150572162L;
	private JTable entriesTable;
	private RubricElementTableModel entriesModel;	
	private Rubric rubricToModify;	

	private JPanel defaultPanel;
	private int priorSelectedIndex;
	private List<JButton> buttons;
	private List<JButton> goldSourceEnabledButtons;
	private JButton goldenSourceButton;
	private JButton cancelButton;
	private StudentWorkCompiler compiler;
	private JPanel automationPanel;
	private JSplitPane mainSplit;
	private Map<RubricEntry.AutomationTypes, RubricEntryDialogCardInterface> cardInterfaces;
	

	public void modifyRubric(Rubric rubric) {
		this.rubricToModify = new Rubric(rubric);
		ListenerCoordinator.fire(SetRubricListener.class, this.rubricToModify, SetRubricListener.RubricType.RUBRIC_BEING_EDITED);
		entriesModel.setRubricToModify(rubricToModify);
		rubricToModify.setInModifiedState(true);
		for (RubricEntry.AutomationTypes key : cardInterfaces.keySet()) {
			if(cardInterfaces.get(key).isActive()) {
				cardInterfaces.get(key).removeItems();
			}
		}
		possiblyLoadGoldenSource();
		setVisible(true);
		
	}
		
	
	public RubricElementDialog(Frame parent, MyPreferences prefs, StudentWorkCompiler compiler) {
		super(parent, "Edit Rubric", Dialog.ModalityType.MODELESS);
		this.compiler = compiler;
		this.setUndecorated(false);
		cardInterfaces = new HashMap<RubricEntry.AutomationTypes, RubricEntryDialogCardInterface>();
		buttons = new ArrayList<JButton>();		
		priorSelectedIndex = -1;		
		entriesModel = new RubricElementTableModel();
		entriesTable = new JTable(entriesModel);		
		entriesTable.setDefaultRenderer(RubricEntry.AutomationTypes.class, new RubricElementRenderer(this));
		entriesTable.setDefaultEditor(RubricEntry.AutomationTypes.class, new RubricElementEditor());
		entriesTable.setRowHeight(20);
		entriesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		entriesTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		
		buttons = new ArrayList<JButton>();
		goldSourceEnabledButtons = new ArrayList<JButton>();
		JButton okButton = newButton("OK", false);
		JButton saveButton = newButton("Save", false);
		JButton deleteButton = newButton("Delete Row", false);
		goldenSourceButton = newButton("Load Gold Source", false);
		goldenSourceButton.setToolTipText("Load the source file(s) representing code that passes 100% of the rubrics");

		JButton testButton = newButton("Test Run", true);
		
		cancelButton = newButton("Cancel", false);
		
		cancelButton.setMnemonic(KeyEvent.VK_C);



		JPanel constantPanel = new JPanel();
		constantPanel.setLayout(new BorderLayout());
		
		JPanel buttonsPanel = createButtonPanel(6);
		buttonsPanel.add(okButton);		
		buttonsPanel.add(saveButton);
		buttonsPanel.add(deleteButton);
		buttonsPanel.add(goldenSourceButton);
		buttonsPanel.add(testButton);
		buttonsPanel.add(cancelButton);




		constantPanel.add(buttonsPanel, BorderLayout.NORTH);

		defaultPanel = new JPanel();
		// cards = new JPanel(new CardLayout());
		defaultPanel.setLayout(new BorderLayout());
		defaultPanel.add(new JScrollPane(entriesTable), BorderLayout.CENTER);
		defaultPanel.add(constantPanel, BorderLayout.EAST);		
		if (prefs.dimensionExists(MyPreferences.Dimensions.RUBRIC_EDIT)) {
			setPreferredSize(prefs.getDimension(MyPreferences.Dimensions.RUBRIC_EDIT, 0, 0));
		}
		
		automationPanel = new JPanel();
		automationPanel.setLayout(new CardLayout());
		
		JPanel emptyPanel = new JPanel();
		automationPanel.add(emptyPanel, RubricEntry.AutomationTypes.NONE.toString());
		
		mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, defaultPanel, automationPanel);
		if (prefs.getSplitLocation(MyPreferences.Dividers.RUBRIC_SPLIT) != 0) {
			mainSplit.setDividerLocation(prefs.getSplitLocation(MyPreferences.Dividers.RUBRIC_SPLIT));
			
		}
			
		add(mainSplit, BorderLayout.CENTER);
		
		cardInterfaces.put(RubricEntry.AutomationTypes.RUN_CODE, new RubricEntryRunCodeCard(this));
		cardInterfaces.put(RubricEntry.AutomationTypes.CODE_CONTAINS_METHOD, new RubricEntryCodeContainsStringCard(this));
		cardInterfaces.put(RubricEntry.AutomationTypes.POINT_LOSS_FOR_LATE, new RubricEntryPointLossForLateCard(this));
		
		
		
		
		
		ListSelectionModel selectionModel = entriesTable.getSelectionModel();
		
		goldenSourceButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadGoldSource();
			}
		});
		
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				preOKSaveTest();				
				ListenerCoordinator.fire(SetRubricListener.class, rubricToModify, SetRubricListener.RubricType.PRIMARY);
				prefs.setDimension(MyPreferences.Dimensions.RUBRIC_EDIT, getSize());
				prefs.setSplitLocation(MyPreferences.Dividers.RUBRIC_SPLIT, mainSplit.getDividerLocation());
				setVisible(false);
			}
		});
		
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				preOKSaveTest();
				ListenerCoordinator.fire(SaveRubricListener.class);								
			}
		});

		testButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				preOKSaveTest();
				ListenerCoordinator.fire(StudentInfoChangedListener.class);
				ListenerCoordinator.fire(AddRubricTabsListener.class, rubricToModify);
				ListenerCoordinator.fire(RunRubricSelected.class, true);				
			}
		});

		
		
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				if (selectedIndex != -1) {
					rubricToModify.removeEntry(selectedIndex);
					entriesModel.fireTableDataChanged();
				}
			}
		});
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(SetRubricListener.class, null, SetRubricListener.RubricType.RUBRIC_BEING_EDITED);
				prefs.setDimension(MyPreferences.Dimensions.RUBRIC_EDIT, getSize());
				prefs.setSplitLocation(MyPreferences.Dividers.RUBRIC_SPLIT, mainSplit.getDividerLocation());
				rubricToModify.setInModifiedState(false);
				setVisible(false);
			}
		});
		

		selectionModel.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				if (selectedIndex != -1 && priorSelectedIndex != selectedIndex) {
					for (int i = 0; i < entriesModel.getColumnCount(); i++) {
						if (entriesModel.getColumnClass(i) == RubricEntry.AutomationTypes.class) {
							typeSelected((RubricEntry.AutomationTypes)entriesModel.getValueAt(selectedIndex, i), true);
							break;
						}
					}
				}
			}
		});
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent ev) {				
				ListenerCoordinator.fire(SetRubricListener.class, null, SetRubricListener.RubricType.RUBRIC_BEING_EDITED);
			}
		});

		createPopupMenu(selectionModel);
		pack();
	}

	public void preOKSaveTest() {
		if (entriesTable.getCellEditor() != null) {
			entriesTable.getCellEditor().stopCellEditing();
		}
		rubricToModify.cleanup();
	}
	
	public JPanel createButtonPanel(int numButtons) {
		final int SPACE = 6;
		final int BUTTON_TOP_SPACE = 5;
		JPanel buttonsPanel;
		GridLayout buttonLayout;
		buttonsPanel = new JPanel();
		// buttonsPanel.setLayout(new FlowLayout());
		buttonLayout = new GridLayout(numButtons, 0);
		final int GAP_SIZE = 6;
		buttonLayout.setVgap(GAP_SIZE);
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(BUTTON_TOP_SPACE, SPACE, SPACE, SPACE));
		buttonsPanel.setLayout(buttonLayout);
		return buttonsPanel;
	}
	
	public JButton newButton(String name, boolean requiresGoldSource) {
		JButton button = new JButton(name);		
		Dimension former = null;
		if (buttons.size() > 0) {
			former = buttons.get(0).getPreferredSize();
		}		
		Dimension current = button.getPreferredSize();
		if (former != null && current.getWidth() > former.getWidth()) {
			for (JButton oldButton : buttons) {				
				oldButton.setPreferredSize(current);
			}
		}
		else if (former != null){			
			button.setPreferredSize(former);
		}
		if (requiresGoldSource) {
			goldSourceEnabledButtons.add(button);
		}
		buttons.add(button);
		return button;
	}
	
	
	private void createPopupMenu(ListSelectionModel selectionModel) {
		JPopupMenu rightClickPopup = new JPopupMenu();
		JMenuItem moveUpItem = new JMenuItem("Move Up");
		JMenuItem moveDownItem = new JMenuItem("Move Down");
		JMenuItem insertAbove = new JMenuItem("Add Entry Above");
		JMenuItem insertBelow = new JMenuItem("Add Entry Below");
		rightClickPopup.add(moveUpItem);		
		rightClickPopup.add(moveDownItem);
		rightClickPopup.add(insertAbove);
		rightClickPopup.add(insertBelow);
		
		moveUpItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				rubricToModify.swapEntries(selectedIndex, selectedIndex -1);
				entriesModel.fireTableDataChanged();
			}			
		});
		moveDownItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				rubricToModify.swapEntries(selectedIndex, selectedIndex + 1);
				entriesModel.fireTableDataChanged();
			}			
		});
		insertAbove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				rubricToModify.addNewEntry(selectedIndex);
				entriesModel.fireTableDataChanged();
			}			
		});
		insertBelow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				rubricToModify.addNewEntry(selectedIndex + 1);
				entriesModel.fireTableDataChanged();
			}			
		});

		entriesTable.setComponentPopupMenu(rightClickPopup);
		

	}
	


	@Override
	public boolean typeSelected(RubricEntry.AutomationTypes automationType, boolean isSelected) {
		boolean validSelection = true;
		if (isSelected) {
			CardLayout c1 = (CardLayout)automationPanel.getLayout();			
			RubricEntry entry = getCurrentEntry();


			if (entry.getAutomationType() != automationType) {
				entry.setAutomationType(automationType);
			}
			if ((entry.getAutomationType().ordinal() > RubricEntry.AutomationTypes.COMPILES.ordinal()) &&
					(rubricToModify.getGoldenSource() == null || rubricToModify.getGoldenSource().size() == 0)) {
				entry.setAutomationType(RubricEntry.AutomationTypes.NONE);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {					
						JOptionPane.showMessageDialog(null, "Before this automation type can be selected, golden source must be loaded.", "Golden Source Missing",
								JOptionPane.ERROR_MESSAGE);
					}
				});

				validSelection = false;
			}
			if (validSelection) {
				if (priorSelectedIndex != entriesTable.getSelectedRow()) {

					priorSelectedIndex = entriesTable.getSelectedRow();
					for (RubricEntry.AutomationTypes key : cardInterfaces.keySet()) {
						if(cardInterfaces.get(key).isActive()) {
							cardInterfaces.get(key).removeItems();
						}
					}
					if (cardInterfaces.get(automationType) != null) {

						cardInterfaces.get(automationType).addItems();
						c1.show(automationPanel, automationType.toString());
					}
					else {
						c1.show(automationPanel, RubricEntry.AutomationTypes.NONE.toString());
					}
				}
				else {

					if (cardInterfaces.get(automationType) != null) {
						if (cardInterfaces.get(automationType).isActive() == false) {
							c1.show(automationPanel,  entry.getAutomationType().toString());
						}
					}
				}
			}

		}
		return validSelection;
	}
	
	public Rubric getRubricToModify() {
		return rubricToModify;
	}
	
	
	public RubricEntry getCurrentEntry() {
		int entryNum = entriesTable.getSelectedRow();
		return rubricToModify.getEntry(entryNum);
	}
	
	public boolean validateTextField(String name, JTextField field) {
		String text = field.getText();
		return validateText(name, text);
	}
	public boolean validateComboField(String name, JComboBox<String> combo) {
		String text = (String)combo.getSelectedItem();
		return validateText(name, text);
	}
	public boolean validateText(String name, String text) {		
		text = text.replaceAll("\\s+", "");
		if (text.length() == 0) {			
			JOptionPane.showMessageDialog(null,  name + " field must have a value", "Field Is Empty", JOptionPane.ERROR_MESSAGE);
			return false;
		}		
		return true;
	}
	
	public StudentWorkCompiler getCompiler() {
		return compiler;
	}


	public void loadGoldSource() {
		List<FileData> allFiles = loadSource();
		if (allFiles != null & allFiles.size() != 0) {
			rubricToModify.setGoldenSource(allFiles);
			possiblyLoadGoldenSource();
		}	
	}
	
	public List<FileData> loadSource() {
		JFileChooser fileChooser = null;
		String currentWorkingDir = (String)ListenerCoordinator.runQuery(GetFileDirQuery.class);
		if (currentWorkingDir != null) {
			fileChooser = new JFileChooser(currentWorkingDir);
		} else {
			fileChooser = new JFileChooser();
		}
		fileChooser.setMultiSelectionEnabled(true);
		List<FileData> allFiles = new ArrayList<FileData>();
		if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
			for (File file : fileChooser.getSelectedFiles()) { 
				Path path = Paths.get(file.getAbsolutePath());
				ListenerCoordinator.fire(SetFileDirListener.class, path.getParent().toString());
				String fileName = path.getFileName().toString();
				
				try {
					String text = new String(Files.readAllBytes(path));
					FileData fileData = new FileData(fileName, text, FileData.GOLDEN_SOURCE_ID, null);
					allFiles.add(fileData);
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}						
			}
		}
		return allFiles;
	}
	
	private void possiblyLoadGoldenSource() {
		boolean enable = (rubricToModify != null && rubricToModify.getGoldenSource() != null && rubricToModify.getGoldenSource().size() != 0);
		for (JButton button : goldSourceEnabledButtons) {			
				button.setEnabled(enable);			
		}
		if (enable) {
			ListenerCoordinator.fire(SetRubricListener.class, rubricToModify, SetRubricListener.RubricType.RUBRIC_BEING_EDITED);
			for (RubricEntry.AutomationTypes key : cardInterfaces.keySet()) {
				cardInterfaces.get(key).goldenSourceEnabled(enable);				
			}
		}
	}
	
	public void addAutomationCard(JPanel panel, RubricEntry.AutomationTypes automationType) {
		automationPanel.add(panel, automationType.toString());
	}

}
