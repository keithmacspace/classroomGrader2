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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabsListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LoadSourceQuery;
import net.cdonald.googleClassroom.listenerCoordinator.RunRubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SaveRubricListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentInfoChangedListener;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;


public class RubricElementDialog extends JDialog implements RubricElementListener {
	private static final long serialVersionUID = -5580080426150572162L;
	private RubricEntriesTable entriesTable;	
	private Rubric rubricToModify;	

	private JPanel defaultPanel;
	private int priorSelectedIndex;
	private List<JButton> buttons;
	private List<JButton> referenceSourceEnabledButtons;
	private JButton referenceSourceButton;
	private JButton cancelButton;
	private StudentWorkCompiler compiler;
	private JPanel automationPanel;
	private JSplitPane mainSplit;
	private Map<RubricEntry.AutomationTypes, RubricEntryDialogCardInterface> cardInterfaces;
	

	public void modifyRubric(Rubric rubric) {
		this.rubricToModify = new Rubric(rubric);
		ListenerCoordinator.fire(SetRubricListener.class, this.rubricToModify, SetRubricListener.RubricType.RUBRIC_BEING_EDITED);
		entriesTable.setAssociatedEntry(rubricToModify);
		rubricToModify.setInModifiedState(true);
		for (RubricEntryDialogCardInterface card : cardInterfaces.values()) {
			if (card.isActive()) {
				card.removeItems();
			}
			card.rubricSet();
		}

		possiblyLoadReferenceSource();
		setVisible(true);
		
	}
		
	
	public RubricElementDialog(Frame parent, MyPreferences prefs, StudentWorkCompiler compiler) {
		super(parent, "Edit Rubric", Dialog.ModalityType.MODELESS);
		this.compiler = compiler;
		this.setUndecorated(false);
		cardInterfaces = new HashMap<RubricEntry.AutomationTypes, RubricEntryDialogCardInterface>();
		buttons = new ArrayList<JButton>();		
		priorSelectedIndex = -1;		

		
		buttons = new ArrayList<JButton>();
		referenceSourceEnabledButtons = new ArrayList<JButton>();
		JButton okButton = newButton("OK", false);
		JButton saveButton = newButton("Save", false);
		JButton deleteButton = newButton("Delete Row", false);
		referenceSourceButton = newButton("Load Ref. Src", false);
		referenceSourceButton.setToolTipText("Load the source file(s) representing code that passes 100% of the rubrics");

		JButton testButton = newButton("Test Run", true);
		
		cancelButton = newButton("Cancel", false);
		
		cancelButton.setMnemonic(KeyEvent.VK_C);



		JPanel constantPanel = new JPanel();
		constantPanel.setLayout(new BorderLayout());
		
		JPanel buttonsPanel = createButtonPanel(6);
		buttonsPanel.add(okButton);		
		buttonsPanel.add(saveButton);
		buttonsPanel.add(deleteButton);
		buttonsPanel.add(referenceSourceButton);
		buttonsPanel.add(testButton);
		buttonsPanel.add(cancelButton);




		constantPanel.add(buttonsPanel, BorderLayout.NORTH);
		entriesTable = new RubricEntriesTable(this);

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
		
		mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, defaultPanel, automationPanel);
		if (prefs.getSplitLocation(MyPreferences.Dividers.RUBRIC_SPLIT) != 0) {
			mainSplit.setDividerLocation(prefs.getSplitLocation(MyPreferences.Dividers.RUBRIC_SPLIT));
			
		}
		RubricEntryPointBreakdownCard defaultCard = new RubricEntryPointBreakdownCard(this);	
		add(mainSplit, BorderLayout.CENTER);
		cardInterfaces.put(RubricEntry.AutomationTypes.NONE, defaultCard);
		cardInterfaces.put(RubricEntry.AutomationTypes.RUN_CODE, new RubricEntryRunCodeCard(this));
		cardInterfaces.put(RubricEntry.AutomationTypes.CODE_CONTAINS_METHOD, new RubricEntryCodeContainsStringCard(this));
		cardInterfaces.put(RubricEntry.AutomationTypes.POINT_LOSS_FOR_LATE, new RubricEntryPointLossForLateCard(this));
		
		
		
		
		
		ListSelectionModel selectionModel = entriesTable.getSelectionModel();
		
		referenceSourceButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				loadreferenceSource();
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
					entriesTable.fireTableDataChanged();
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
					for (int i = 0; i < entriesTable.getColumnCount(); i++) {
						if (entriesTable.getColumnClass(i) == RubricEntry.AutomationTypes.class) {
							typeSelected((RubricEntry.AutomationTypes)entriesTable.getValueAt(selectedIndex, i), true);
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

		pack();
	}
	
	public void fireTableDataChanged() {
		int entryNum = entriesTable.getSelectedRow();
		entriesTable.fireTableDataChanged();
		entriesTable.setRowSelectionInterval(entryNum, entryNum);
	}

	public void preOKSaveTest() {
		for (RubricEntryDialogCardInterface card : cardInterfaces.values()) {			
			card.saving();
		}
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
	
	public JButton newButton(String name, boolean requiresreferenceSource) {
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
		if (requiresreferenceSource) {
			referenceSourceEnabledButtons.add(button);
		}
		buttons.add(button);
		return button;
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
					(rubricToModify.getReferenceSource() == null || rubricToModify.getReferenceSource().size() == 0)) {
				entry.setAutomationType(RubricEntry.AutomationTypes.NONE);
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {					
						JOptionPane.showMessageDialog(null, "Before this automation type can be selected, reference source must be loaded.", "Reference Source Missing",
								JOptionPane.ERROR_MESSAGE);
					}
				});

				validSelection = false;
			}
			if (validSelection) {
				if (priorSelectedIndex != entriesTable.getSelectedRow()) {
					priorSelectedIndex = entriesTable.getSelectedRow();
					for (RubricEntryDialogCardInterface card : cardInterfaces.values()) {
						if (card.isActive()) {
							card.removeItems();
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
							cardInterfaces.get(automationType).addItems();
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
	
	public void appendDescription(String description) {
		RubricEntry entry = getCurrentEntry();
		if (entry != null) {
			String currentDescription = entry.getDescription();
			if (currentDescription.indexOf(description) == -1) {
				currentDescription += description;
				entry.setDescription(currentDescription);
				fireTableDataChanged();

			}
		}

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


	public void loadreferenceSource() {
		List<FileData> allFiles = (List<FileData>)ListenerCoordinator.runQuery(LoadSourceQuery.class);
		if (allFiles != null & allFiles.size() != 0) {
			rubricToModify.setReferenceSource(allFiles);
			possiblyLoadReferenceSource();
		}	
	}
	
	
	private void possiblyLoadReferenceSource() {
		boolean enable = (rubricToModify != null && rubricToModify.getReferenceSource() != null && rubricToModify.getReferenceSource().size() != 0);
		for (JButton button : referenceSourceEnabledButtons) {			
				button.setEnabled(enable);			
		}
		if (enable) {
			ListenerCoordinator.fire(SetRubricListener.class, rubricToModify, SetRubricListener.RubricType.RUBRIC_BEING_EDITED);
			for (RubricEntryDialogCardInterface card : cardInterfaces.values()) {
				card.referenceSourceEnabled(enable);
			}
		}
	}
	
	public void addAutomationCard(JPanel panel, RubricEntry.AutomationTypes automationType) {
		automationPanel.add(panel, automationType.toString());
	}

}
