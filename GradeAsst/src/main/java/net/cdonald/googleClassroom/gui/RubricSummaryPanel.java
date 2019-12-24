package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.undo.UndoManager;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.GetCompilerQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntry.AutomationTypes;
import net.cdonald.googleClassroom.model.RubricEntryRunCode;

public class RubricSummaryPanel extends JPanel implements RubricElementListener {
	private static final long serialVersionUID = 1868203593825264825L;
	private UndoManager undoManager;
	private Rubric rubricToModify;
	private RubricEntriesTable entriesTable;
	private JPanel automationPanel;
	private JPanel defaultPanel;
	private JPanel descriptionPanel;
	private Map<Integer, RubricEntryAutomationCardInterface> automationAssignmentMap;
	private Map<Integer, JTextArea> descriptionAssignmentMap;
	private Map<String, List<Method>> possibleMethodMap;
	private RubricFileListener rubricFileListener;
	private boolean enableEditing;
 

	RubricSummaryPanel(UndoManager undoManager, RubricFileListener rubricFileListener) {
		super();
		this.undoManager = undoManager;
		this.rubricFileListener = rubricFileListener;		
		this.enableEditing = false;
		possibleMethodMap = new HashMap<String, List<Method>>();
		automationAssignmentMap = new HashMap<Integer, RubricEntryAutomationCardInterface>();
		descriptionAssignmentMap = new HashMap<Integer, JTextArea>();
		createLayout();
		automationPanel.add(new EmptyCard(), "Empty Card");
		enableEditing(false);

	}
	
	public void setRubricToModify(Rubric rubric) {
		this.rubricToModify = rubric;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {			

				for (Integer id : automationAssignmentMap.keySet()) {
					RubricEntryAutomationCardInterface card = automationAssignmentMap.get(id);
					if (card != null) {
						automationPanel.remove(card);
						card.setEnableEditing(enableEditing);
					}
				}
				descriptionPanel.removeAll();
				automationAssignmentMap.clear();
				possibleMethodMap.clear();
				descriptionAssignmentMap.clear();
				entriesTable.setAssociatedRubric(rubric);				
				initPossibleMethodMap();
			}
		});
	}

	private void addRubricEntryCards(int index) {
		RubricEntry entry = rubricToModify.getEntry(index);
		addDescriptionCard(entry);
		assignAutomation(entry.getAutomationType(), entry.getUniqueID());
	}
	
	private void initPossibleMethodMap() {
		StudentWorkCompiler compiler = (StudentWorkCompiler)ListenerCoordinator.runQuery(GetCompilerQuery.class);
		possibleMethodMap = RubricEntryRunCode.getPossibleMethods(rubricFileListener.getReferenceSource(), compiler, rubricFileListener.getTestSource());
		

	}
	@Override
	public void referenceSourceChanged() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				initPossibleMethodMap();
			}
		});
		
	}

	@Override
	public void testSourceChanged() {		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				initPossibleMethodMap();
				for (RubricEntryAutomationCardInterface automation : automationAssignmentMap.values()) {
					automation.testSourceChanged();
				}				
			}
		});
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
		JSplitPane automationSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, descriptionPanel, automationPanel);
		JSplitPane mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, defaultPanel, automationSplit);
		mainSplit.setResizeWeight(0.5);
		automationSplit.setResizeWeight(0.3);
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
						RubricEntry entry = rubricToModify.getEntry(selectedIndex);						
						if (entry != null) {
							addRubricEntryCards(selectedIndex);
							CardLayout cardLayout = (CardLayout) automationPanel.getLayout();
							cardLayout.show(automationPanel, "" + entry.getUniqueID());
							cardLayout = (CardLayout)descriptionPanel.getLayout();
							cardLayout.show(descriptionPanel, "" + entry.getUniqueID());
						}
						
					}
				});
			}
		});				
	}
	
	public void enableEditing(boolean enableEditing) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				RubricSummaryPanel.this.enableEditing = enableEditing;
				if (rubricToModify != null) {
					rubricToModify.setAllowEntryAddition(enableEditing);
				}
				for (Integer id : automationAssignmentMap.keySet()) {
					RubricEntryAutomationCardInterface card = automationAssignmentMap.get(id);
					if (card != null) {
						card.setEnableEditing(enableEditing);
					}
					JTextArea textArea = descriptionAssignmentMap.get(id);
					if (textArea != null) {
						textArea.setEditable(enableEditing);
					}
				}
				entriesTable.setEditingEnabled(enableEditing);						
			}
		});
	}
	
	public void saving() {
		entriesTable.stopEditing();
		RubricEntry entry = getCurrentEntry();
		if (entry != null) {
			Integer id = entry.getUniqueID();
			RubricEntryAutomationCardInterface card = automationAssignmentMap.get(id);
			if (card != null) {
				card.saving();
			}
		}

		for (Integer id : descriptionAssignmentMap.keySet()) {
			JTextArea area = descriptionAssignmentMap.get(id);
			if (area != null) {
				RubricEntry localEntry = rubricToModify.getEntryByID(id);
				if (!localEntry.getDescription().equals(area.getText())) {
					localEntry.setDescription(area.getText());
				}
			}
		}
	}
	
	private void addDescriptionCard(RubricEntry entry) {
		if (descriptionAssignmentMap.containsKey(entry.getUniqueID())) {
			return;
		}
		JPanel panel = new JPanel();
		panel.setLayout(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder("Description"));
		String hint = "Add more information about how to grade this entry here";
		panel.setToolTipText(hint);
		JTextArea textArea = new JTextArea();
		textArea.setToolTipText(hint);
		textArea.getDocument().addUndoableEditListener(undoManager);
		textArea.setText(entry.getDescription());
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		textArea.setEditable(enableEditing);
		panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
		descriptionAssignmentMap.put(entry.getUniqueID(), textArea);
		descriptionPanel.add(panel, "" + entry.getUniqueID());
	}
	
	
	private void assignAutomation(RubricEntry.AutomationTypes automationType, int uniqueID) {
		RubricEntryAutomationCardInterface old = automationAssignmentMap.get(uniqueID);
		if (old != null)  {
			if ( old.getAutomationType() == automationType) {
				return;
			}
			automationPanel.remove(old);
		}
		RubricEntryAutomationCardInterface panel = null;
		switch(automationType) {
		case RUN_CODE:
			panel = new RubricEntryRunCodeCard(enableEditing, possibleMethodMap, rubricFileListener, rubricToModify, uniqueID);;
			break;
		case CODE_CONTAINS_METHOD:
			panel = new RubricEntryCodeContainsStringCard(enableEditing, rubricFileListener, rubricToModify, uniqueID);
			break;
		case POINT_LOSS_FOR_LATE:
			panel = new RubricEntryPointLossForLateCard(enableEditing, rubricToModify, uniqueID);
			break;
		default:
			panel = new EmptyCard();
			break;					
		}
		automationAssignmentMap.put(uniqueID, panel);
		automationPanel.add(panel, "" + uniqueID);		
	}
	
	public RubricEntry getCurrentEntry() {
		int entryNum = entriesTable.getSelectedRow();
		if (entryNum > 0) {
			return rubricToModify.getEntry(entryNum);
		}
		return null;
	}

	@Override
	public boolean typeSelected(RubricEntry.AutomationTypes typeSelected, boolean isSelected) {		
		boolean validSelection = true;
		if (isSelected) {
						
			RubricEntry entry = getCurrentEntry();
			if (entry != null) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						entry.setAutomationType(typeSelected);
						int index = entriesTable.getSelectedRow();
						assignAutomation(typeSelected, entry.getUniqueID());
						CardLayout c1 = (CardLayout)automationPanel.getLayout();
						c1.show(automationPanel, "" + entry.getUniqueID());						
					}
				});
			}
		}
		return validSelection;
	}
	
	private class EmptyCard extends RubricEntryAutomationCardInterface {



		@Override
		public void saving() {			
		}

		@Override
		public void testSourceChanged() {						
		}

		@Override
		public String getDescription() {
			return "";
		}

		@Override
		public AutomationTypes getAutomationType() {			
			return RubricEntry.AutomationTypes.NONE;
		}

		@Override
		public void setEnableEditing(boolean enable) {
		}
		
	}
	public static void main(String args[]) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new TestFrame().setVisible(true);
			}
		});
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
			Map<String, List<Method>> possibleMethodMap = RubricEntryRunCode.getPossibleMethods(refSource, compiler, testSource);
			ListenerCoordinator.addQueryResponder(GetCompilerQuery.class, new GetCompilerQuery() {

				@Override
				public StudentWorkCompiler fired() {
					// TODO Auto-generated method stub
					return compiler;
				}
				
			});
			Rubric rubric = new Rubric();
			rubric.addNewEntry(0);
			RubricEntry e = rubric.getEntry(0);
			e.setName("Test");
			e.setValue(5);
			e.setAutomation(new RubricEntryRunCode());
			RubricSummaryPanel testPanel = new RubricSummaryPanel(new UndoManager(), this);
			testPanel.setRubricToModify(rubric);
			testPanel.enableEditing(true);
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
	}

	

}
