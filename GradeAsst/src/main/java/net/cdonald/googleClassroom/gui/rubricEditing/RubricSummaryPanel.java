package net.cdonald.googleClassroom.gui.rubricEditing;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
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

import org.mdkt.compiler.CompilationException;

import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.gui.RubricTabInterface;
import net.cdonald.googleClassroom.gui.StudentOutputTabs;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.GetCompilerQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntry.AutomationTypes;
import net.cdonald.googleClassroom.model.RubricEntryMethodContains;
import net.cdonald.googleClassroom.model.RubricEntryRunCode;

public class RubricSummaryPanel extends JPanel implements RubricElementListener {
	private static final long serialVersionUID = 1868203593825264825L;
	private UndoManager undoManager;
	private Rubric rubricToModify;
	private RubricEntriesTable entriesTable;
	private JPanel automationPanel;
	private JPanel defaultPanel;
	private JPanel descriptionPanel;
	//private JSplitPane automationSplit;
	private JSplitPane mainSplit;
	private Map<Integer, RubricEntryAutomationCardInterface> automationAssignmentMap;
	private Map<Integer, JTextArea> descriptionAssignmentMap;
	private RubricFileListener rubricFileListener;
	private RubricTabInterface rubricTabInterface;
	private boolean enableEditing;
	private boolean sourceModified;
	private volatile boolean hasFocus;
	private volatile static int lastErrorId = -1;
	private volatile Map<String, List<Method>> possibleMethodMap;
	private volatile Map<String, Set<String> > referenceMethodMap;
	private volatile static RubricEntry.AutomationTypes lastErrorType = RubricEntry.AutomationTypes.NONE;
	
 

	RubricSummaryPanel(UndoManager undoManager, RubricFileListener rubricFileListener, RubricTabInterface rubricTabInterface) {
		super();	
		this.undoManager = undoManager;
		this.rubricFileListener = rubricFileListener;		
		this.enableEditing = false;	
		this.rubricTabInterface = rubricTabInterface;
		this.hasFocus = false;

		automationAssignmentMap = new HashMap<Integer, RubricEntryAutomationCardInterface>();
		descriptionAssignmentMap = new HashMap<Integer, JTextArea>();
		fillSourceMaps();
		createLayout();
		JPanel emptyText = new JPanel();
		emptyText.setPreferredSize(new Dimension(0, 40));
		descriptionPanel.add(emptyText, "Empty Card");
		automationPanel.add(new EmptyAutomationCard(), "Empty Card");
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
				descriptionAssignmentMap.clear();
				entriesTable.setAssociatedRubric(rubric);				
				initPossibleMethodMap();
				//automationSplit.setDividerLocation(0.3);
				mainSplit.setDividerLocation(0.5);
			}
		});
	}
	private void fillSourceMaps() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				initPossibleMethodMap();
				initReferenceSourceMap();
				for (RubricEntryAutomationCardInterface automation : automationAssignmentMap.values()) {
					automation.testSourceChanged(possibleMethodMap);
					automation.referenceSourceChanged(referenceMethodMap);
				}
				automationPanel.revalidate();
			}
		});
	}
	public void hasFocus(boolean hasFocus) {
		this.hasFocus = hasFocus;
		if (hasFocus) { 
			if (sourceModified) {
				fillSourceMaps();
			}
			sourceModified = false;
		}
	}
	public void sourceIsChanged() {
		if (hasFocus) {
			fillSourceMaps();
		}
		else {
			sourceModified = true;
		}
	}
	

	private void addRubricEntryCards(int index) {
		RubricEntry entry = rubricToModify.getEntry(index);
		addDescriptionCard(entry.getDescription(), entry.getUniqueID());
		assignAutomation(entry.getAutomationType(), entry);
	}
	
	private void  initPossibleMethodMap() {
		StudentWorkCompiler compiler = (StudentWorkCompiler)ListenerCoordinator.runQuery(GetCompilerQuery.class);
		try {
			possibleMethodMap = RubricEntryRunCode.getPossibleMethods(rubricFileListener.getReferenceSource(), compiler, rubricFileListener.getTestSource());
		} catch (CompilationException e) {
			rubricFileListener.addCompilerMessage(e.getLocalizedMessage());			
		} catch (Exception e) {
			DebugLogDialog.appendException(e);
		}
		if (possibleMethodMap != null) {
			rubricFileListener.addCompilerMessage("");
		}
	}
	
	private void initReferenceSourceMap() {
		List<FileData> referenceSource = rubricFileListener.getReferenceSource();
		referenceMethodMap = RubricEntryMethodContains.createCallMap(referenceSource);
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
		//automationSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, descriptionPanel, automationPanel);
		mainSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, defaultPanel, descriptionPanel);

		
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
							rubricTabInterface.addAndSelectRubricTab("Automation", automationPanel);
							CardLayout cardLayout = (CardLayout) automationPanel.getLayout();
							cardLayout.show(automationPanel, "" + entry.getUniqueID());
							cardLayout = (CardLayout)descriptionPanel.getLayout();
							cardLayout.show(descriptionPanel, "" + entry.getUniqueID());
							automationPanel.revalidate();
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
		for (RubricEntryAutomationCardInterface card : automationAssignmentMap.values()) {
			if (card != null) {
				card.saving();
			}
		}

		for (Integer id : descriptionAssignmentMap.keySet()) {
			JTextArea area = descriptionAssignmentMap.get(id);
			if (area != null) {
				RubricEntry localEntry = rubricToModify.getEntryByID(id);
				if (localEntry != null) {
					if (!localEntry.getDescription().equals(area.getText())) {
						localEntry.setDescription(area.getText());
					}
				}
			}
		}
	}
	
	private void addDescriptionCard(String description, int uniqueID) {
		if (descriptionAssignmentMap.containsKey(uniqueID)) {
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
		if (description != null) {
			textArea.setText(description);
		}
		else {
			textArea.setText("");
		}
		textArea.setWrapStyleWord(true);
		textArea.setLineWrap(true);
		textArea.setEditable(enableEditing);
		panel.add(new JScrollPane(textArea), BorderLayout.CENTER);
		descriptionAssignmentMap.put(uniqueID, textArea);
		descriptionPanel.add(panel, "" + uniqueID);
	}
	
	
	private void assignAutomation(RubricEntry.AutomationTypes automationType, RubricEntry entry) {
		if (entry == null) {
			return;
		}
		int uniqueID = entry.getUniqueID();
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
			panel = new RubricEntryCodeContainsStringCard(enableEditing, referenceMethodMap, rubricToModify, uniqueID);
			break;
		case POINT_LOSS_FOR_LATE:
			panel = new RubricEntryPointLossForLateCard(enableEditing, rubricToModify, uniqueID);
			break;
		case NONE:
			panel = new EmptyCard();
			break;
		default:
			panel = new EmptyAutomationCard();
			break;					
		}
		automationAssignmentMap.put(uniqueID, panel);
		automationPanel.add(panel, "" + uniqueID);
		modifyDescription(entry, panel.getDescription());
	}
	
	public void modifyDescription(RubricEntry entry, String newDescription) {
		final String start = "**";
		final String end = "**";
		if (entry != null) {
			int uniqueID = entry.getUniqueID();
			String description = "";
			if (newDescription != null && newDescription.length() != 0) {
				description = start + newDescription + end;
			}
			JTextArea currentDescription = descriptionAssignmentMap.get(uniqueID);		
			if (currentDescription == null) {
				addDescriptionCard(entry.getDescription(), uniqueID);
			}
			currentDescription = descriptionAssignmentMap.get(uniqueID);
			String oldDescription = currentDescription.getText();
			int htmlStart = oldDescription.indexOf(start);
			int htmlEnd = oldDescription.lastIndexOf(end);
			if (htmlStart != -1 && htmlEnd != -1) {
				String temp = oldDescription.substring(0, htmlStart);
				temp += oldDescription.substring(htmlEnd + end.length());
				oldDescription = temp;					
			}
			//description += "\n" + oldDescription;
			entry.setDescription(description);
			currentDescription.setText(description);
		}		
	}
	
	public RubricEntry getCurrentEntry() {
		int entryNum = entriesTable.getSelectedRow();
		if (entryNum > 0) {
			return rubricToModify.getEntry(entryNum);
		}
		return null;
	}

	@Override
	public boolean typeSelected(RubricEntry.AutomationTypes typeSelected, int row, boolean hasFocus) {		
		boolean validSelection = true;
		if (hasFocus) {

			if (typeSelected == RubricEntry.AutomationTypes.CODE_CONTAINS_METHOD || typeSelected == RubricEntry.AutomationTypes.RUN_CODE) {
				if (rubricFileListener.isReferenceSourceSet() == false) {
					RubricEntry entry = null;
					if (rubricToModify != null) {
						entry = rubricToModify.getEntry(row);
					}
					if (hasFocus && entry != null && (entry.getUniqueID() != lastErrorId || typeSelected != lastErrorType )) {
						lastErrorId = entry.getUniqueID();
						lastErrorType = typeSelected; 

						SwingUtilities.invokeLater(new Runnable() {	
							public void run() {
								System.out.println("would have created dialog box");
								JOptionPane.showMessageDialog(null, "Before selecting this automation type, you must load some reference source.\n" +
										"Go to the reference source tab and add source there", "Reference Source Required",
										JOptionPane.ERROR_MESSAGE);
							}
						});
					}
					System.out.println("returning false");
					return false;
				}			
			}


			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					RubricEntry entry = null;
					if (rubricToModify != null) {
						entry = rubricToModify.getEntry(row);
					}
					if (entry != null) {
						entry.setAutomationType(typeSelected);						
						assignAutomation(typeSelected, entry);
						CardLayout c1 = (CardLayout)automationPanel.getLayout();
						c1.show(automationPanel, "" + entry.getUniqueID());
					}
				}
			});

		}
		return validSelection;
	}
	
	
	
	private class EmptyAutomationCard extends RubricEntryAutomationCardInterface {
		private static final long serialVersionUID = -3013431743093417345L;

		public EmptyAutomationCard() {
			this.setPreferredSize(new Dimension(10, 40));			
		}


		@Override
		public void saving() {			
		}

		@Override
		public void testSourceChanged(Map<String, List<Method>> possibleMethodMap) {						
		}


		@Override
		public AutomationTypes getAutomationType() {			
			return RubricEntry.AutomationTypes.NONE;
		}

		@Override
		public void setEnableEditing(boolean enable) {
		}


		@Override
		public void referenceSourceChanged(Map<String, Set<String>> methodMap) {						
		}
	
	}
	
	private class EmptyCard extends EmptyAutomationCard {
		/**
		 * 
		 */
		private static final long serialVersionUID = -1392802686485082310L;

		@Override
		public String getDescription() {
			return "";		
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

		private static final long serialVersionUID = -228872435733451730L;
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
			Rubric rubric = new Rubric();
			rubric.addNewEntry(0);
			RubricEntry e = rubric.getEntry(0);
			e.setName("Test");
			e.setValue(5);
			e.setAutomation(new RubricEntryRunCode());
			UndoManager undoManager = new UndoManager();
			StudentOutputTabs studentTabs = new StudentOutputTabs(null, null);
			RubricSummaryPanel testPanel = new RubricSummaryPanel(undoManager, this, studentTabs);
			
			testPanel.setRubricToModify(rubric);
			testPanel.enableEditing(true);
			add(new JSplitPane(JSplitPane.VERTICAL_SPLIT, testPanel, studentTabs));
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
		public JButton getAddSourceButtons() {
			// TODO Auto-generated method stub
			return new JButton("Add Source");
		}



		@Override
		public boolean isReferenceSourceSet() {
			return true;
		}



		@Override
		public void addCompilerMessage(String message) {
			// TODO Auto-generated method stub
			
		}



		@Override
		public void sourceIsChanged() {
			// TODO Auto-generated method stub
			
		}



		@Override
		public void compileSource(RubricTabNames sourceType) {
			// TODO Auto-generated method stub
			
		}
	}


	

}
