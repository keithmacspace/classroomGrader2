package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.undo.UndoManager;

import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;

public class RubricSummaryPanel extends JPanel implements RubricElementListener {
	private static final long serialVersionUID = 1868203593825264825L;
	private UndoManager undoManager;
	private Rubric rubricToModify;
	private RubricEntriesTable entriesTable;
	private JPanel automationPanel;
	private JPanel defaultPanel;
	private JPanel descriptionPanel;
	private Map<Integer, JPanel> automationAssignmentMap;
 

	RubricSummaryPanel(UndoManager undoManager) {
		super();
		this.undoManager = undoManager;
		automationAssignmentMap = new HashMap<Integer, JPanel>();
		createLayout();
	}
	
	private void createLayout() {
		setLayout(new BorderLayout());
		entriesTable = new RubricEntriesTable(this);
		defaultPanel = new JPanel();
		// cards = new JPanel(new CardLayout());
		defaultPanel.setLayout(new BorderLayout());
		defaultPanel.add(new JScrollPane(entriesTable), BorderLayout.CENTER);	
		
		automationPanel = new JPanel();
		automationPanel.setLayout(new CardLayout());
		descriptionPanel = new JPanel();
		descriptionPanel.setLayout(new CardLayout());
		JSplitPane automationSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, automationPanel, descriptionPanel);
		JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, defaultPanel, automationSplit);
		add(mainSplit, BorderLayout.CENTER);
		
		ListSelectionModel selectionModel = entriesTable.getSelectionModel();
		
		selectionModel.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						int selectedIndex = selectionModel.getMinSelectionIndex();
						CardLayout cardLayout = (CardLayout) automationPanel.getLayout();
						cardLayout.show(automationPanel, "" + selectedIndex);
					}
				});
			}
		});
				
	}
	
	public void enableEditing(Rubric rubricBeingEdited) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				rubricToModify = rubricBeingEdited;
				entriesTable.setAssociatedRubric(rubricBeingEdited);
				addRubricCards();
			}
		});
	}
	
	private void addRubricCards() {
		for (JPanel panel : automationAssignmentMap.values()) {
			automationPanel.remove(panel);
		}
		if (rubricToModify != null) {
			for (int i = 0; i < rubricToModify.getEntryCount(); i++) {
				RubricEntry entry = rubricToModify.getEntry(i);
				if (entry != null) {
					assignAutomation(entry.getAutomationType(), i);
				}
			}
		}
		validate();
	}
	
	private void assignAutomation(RubricEntry.AutomationTypes automationType, int index) {
		JPanel old = automationAssignmentMap.get(index);
		if (old != null) {
			automationPanel.remove(old);
		}
		JPanel panel = null;
		switch(automationType) {
		case RUN_CODE:
			panel = new RubricEntryRunCodeCard(rubricToModify, index);;
			break;
		default:
			panel = new JPanel();
			break;					
		}
		automationAssignmentMap.put(index, panel);
		automationPanel.add(panel, "" + index);		
	}
	
	public RubricEntry getCurrentEntry() {
		int entryNum = entriesTable.getSelectedRow();
		return rubricToModify.getEntry(entryNum);
	}

	@Override
	public boolean typeSelected(RubricEntry.AutomationTypes typeSelected, boolean isSelected) {		
		boolean validSelection = true;
		if (isSelected) {
			CardLayout c1 = (CardLayout)automationPanel.getLayout();			
			RubricEntry entry = getCurrentEntry();
			if (entry.getAutomationType() != typeSelected) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						entry.setAutomationType(typeSelected);
						int index = entriesTable.getSelectedRow();
						assignAutomation(typeSelected, index);
						c1.show(automationPanel, "" + index);						
					}
				});
			}
		}
		return validSelection;
	}
	

}
