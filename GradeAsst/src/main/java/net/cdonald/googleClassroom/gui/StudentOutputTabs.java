package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.cdonald.googleClassroom.gui.StudentConsoleAreas.OutputAreas;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentTextAreasQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SystemInListener;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;

public class StudentOutputTabs extends JPanel {
	private static final long serialVersionUID = -935623321670090415L;
	private List<JPanel> cardPanels;		
	private JTextArea consoleInput;
	private JTextArea currentInputHistory;
	private String runningStudent;
	private JTabbedPane outputTabs;
	private JCheckBox followInput; 
	public static String CONSOLE_TAB_NAME = "Console";
	public static String INPUT_HISTORY_TAB_NAME = "In Hist.";


	public StudentOutputTabs(JPopupMenu popupInput, JPopupMenu popupDisplays) {
		super();
		cardPanels = new ArrayList<JPanel>();
		createConsoleTabs(popupInput, popupDisplays);
		createLayout();
	}

	private void createLayout() {
		setLayout(new BorderLayout());
		followInput = new JCheckBox("Auto Select", true);

		add(followInput, BorderLayout.NORTH);
		add(outputTabs, BorderLayout.CENTER);

	}

	public void clearText() {
		consoleInput.setText("");
		currentInputHistory.setText("");
	}



	public void selectOutputTab(String tabName) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (followInput.isSelected()) {
					for (int i = 0; i < outputTabs.getTabCount(); i++) {
						String title = outputTabs.getTitleAt(i); 
						if (title.equals(tabName)) {				
							outputTabs.setSelectedIndex(i);
							break;
						}
					}
				}				
			}
		});
	}


	private void createConsoleTabs(JPopupMenu popupInput, JPopupMenu popupDisplays) {
		outputTabs = new JTabbedPane();
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
		currentInputHistory.setEditable(false);
		inputHistorWrapperPanel.add(new JScrollPane(currentInputHistory), BorderLayout.CENTER);
		currentInputHistory.setComponentPopupMenu(popupDisplays);

		inputHistorWrapperPanel.setBorder(BorderFactory.createTitledBorder("Input History"));

		JPanel inputWrapper = new JPanel();
		inputWrapper.setLayout(new BorderLayout());
		inputWrapper.add(consoleInputPanel, BorderLayout.NORTH);

		ioPanel.add(getCardPanel(0), BorderLayout.CENTER);
		ioPanel.add(inputWrapper, BorderLayout.SOUTH);

		outputTabs.addTab(CONSOLE_TAB_NAME, ioPanel);
		outputTabs.addTab(INPUT_HISTORY_TAB_NAME, new JScrollPane(currentInputHistory));

		consoleInput.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				if (e.getType() == DocumentEvent.EventType.CHANGE || e.getType() == DocumentEvent.EventType.INSERT) {
					String text = consoleInput.getText();

					int returnIndex = text.indexOf("\n");
					if (returnIndex != -1) {
						String inputText = text.substring(0, returnIndex);

						ListenerCoordinator.fire(SystemInListener.class, inputText);
						if (runningStudent != null) {
							currentInputHistory.append(runningStudent + " Input:\n");
							runningStudent = null;
						}
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

	private JPanel createPanel() {
		JPanel out = new JPanel();
		out.setLayout(new BorderLayout());			
		out.setBorder(BorderFactory.createTitledBorder("System.out"));		
		return out;
	}


	private JPanel getCardPanel(int cardNumber) {		
		while (cardPanels.size() <= cardNumber) {
			JPanel temp = new JPanel(new CardLayout());
			cardPanels.add(temp);
		}
		return cardPanels.get(cardNumber);
	}

	private int addTabIfNeeded(String tabName) {
		for (int i = 0; i < outputTabs.getTabCount(); i++) {
			String title = outputTabs.getTitleAt(i); 
			if (title.equals(tabName)) {
				return i;
			}
		}		
		JPanel cardPanel = getCardPanel(outputTabs.getTabCount());
		outputTabs.addTab(tabName, cardPanel);
		return outputTabs.getTabCount() - 1;
	}

	private void addCard(String tabName, String studentID, OutputAreas outputAreas, String initialText) {
		int currentPanelCount = addTabIfNeeded(tabName);		
		JPanel cardPanel = cardPanels.get(currentPanelCount);
		JPanel out = createPanel();
		if (outputAreas != null) {
			JScrollPane scrollPane = new JScrollPane(outputAreas.getOutputArea());
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
			out.add(scrollPane, BorderLayout.CENTER);
			outputAreas.appendOutput(initialText, true);

		}
		cardPanel.add(out, studentID);

	}

	public void switchStudent(String studentID) {
		if (studentID == null) {
			studentID = "null";
		}
		for (int i = 0; i < cardPanels.size(); i++) {
			for (JPanel cardPanel : cardPanels) {
				CardLayout cardLayout = (CardLayout) cardPanel.getLayout();
				cardLayout.show(cardPanel, studentID);
			}
		}
	}


	public void bindStudentAreas(List<String> studentIDs, Rubric currentRubric) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				while(outputTabs.getTabCount() > 2) {
					outputTabs.removeTabAt(outputTabs.getTabCount() - 1);
				}
				while (cardPanels.size() > 1) {
					cardPanels.remove(cardPanels.size() - 1);
				}
				for (String studentID : studentIDs) {						
					StudentConsoleAreas currentAreas = (StudentConsoleAreas)ListenerCoordinator.runQuery(GetStudentTextAreasQuery.class, studentID);
					OutputAreas outputAreas = currentAreas.getOutputAreas();
					addCard(CONSOLE_TAB_NAME, studentID, outputAreas, "");
					if (currentRubric != null) {
						for (int i = 0; i < currentRubric.getEntryCount(); i++) {
							RubricEntry rubricEntry = currentRubric.getEntry(i); 
							String rubricName = rubricEntry.getName();		
							String tip = "";

							if (rubricEntry.getValue() > 0) {
								tip += "Max Val = " + rubricEntry.getValue() + ": ";
							}
							tip += rubricEntry.getDescription();					
							addCard(rubricName, studentID, currentAreas.getRubricArea(rubricName), tip);
						}
					}
				}
				addCard(CONSOLE_TAB_NAME, "null", null, "");
				if (currentRubric != null) {
					for (int i = 0; i < currentRubric.getEntryCount(); i++) {
						RubricEntry rubricEntry = currentRubric.getEntry(i); 
						String rubricName = rubricEntry.getName();		
						addCard(rubricName, "null", null, "");
					}
				}
			}
		});


	}


	public void setRunningStudent(String studentName) {
		runningStudent = studentName;		
	}

}
