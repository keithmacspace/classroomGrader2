package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.UndoManager;

import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabsListener;
import net.cdonald.googleClassroom.listenerCoordinator.AllStudentFilesLoadedListener;
import net.cdonald.googleClassroom.listenerCoordinator.AssignmentSelected;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentRubricQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentIDListQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.PreRunBlockingListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentSelectedListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;

public class ConsoleAndSourcePanel extends JPanel {
	private static final long serialVersionUID = 1084761781946423738L;	
	private JTabbedPane overallTabbedPane;		
	private JPopupMenu popupSource;
	private JPopupMenu popupInput;
	private JPopupMenu popupDisplays;
	private static Semaphore pauseSemaphore = new Semaphore(1);	
	private String currentID;		
	private UndoManager undoManager;
	private StudentSourceCards studentSourceCards;
	private enum OverallTabNames {Source, Rubric}
	private StudentOutputTabs studentOutputTabs;
	private RubricTabPanel rubricTabPanel;
	


	public ConsoleAndSourcePanel(UndoManager undoManager) {
		this.undoManager = undoManager;
		
		setMinimumSize(new Dimension(400, 400));
		createPopupMenu();
		createLayout();
		registerListeners();
		setVisible(true);
		
				

	}

	public void assignmentSelected() {		
		if (studentOutputTabs != null) {
			studentOutputTabs.clearText();
		}
	}	
	
	public void syncSource() {
		studentSourceCards.syncSource(currentID);
	}


	public void setWindowData(String idToDisplay) {
		
		SwingUtilities.invokeLater(new Runnable() {			
			@Override
			public void run() {

				if (currentID == null || !currentID.equals(idToDisplay)) {
					syncSource();
					studentSourceCards.switchSource(idToDisplay);
					studentOutputTabs.switchStudent(idToDisplay);
				}
				//bindStudentAreas(idToDisplay);
				currentID = idToDisplay;
			}

		});
		
	}
	
	public void refreshInfo() {
		if (currentID != null) {
			setWindowData(currentID);
		}
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
		

		recompile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				studentSourceCards.recompile(currentID);
			}
		});
		
		JMenuItem removeSource = new JMenuItem("Remove Source");
		popupSource.add(removeSource);		
		removeSource.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				studentSourceCards.removeSource(currentID);
			}
		});
	}

	private void createLayout() {
		setSize(800, 500);
		setLayout(new BorderLayout());

		setVisible(true);
		
		rubricTabPanel = new RubricTabPanel(undoManager, popupSource, popupDisplays);
		studentSourceCards = new StudentSourceCards();		
		overallTabbedPane = new JTabbedPane();
		new TabbedUndoListener(undoManager, overallTabbedPane);
		overallTabbedPane.addTab(OverallTabNames.Source.toString(), studentSourceCards);
		overallTabbedPane.addTab(OverallTabNames.Rubric.toString(), rubricTabPanel);
		

		studentOutputTabs = new StudentOutputTabs(popupInput, popupDisplays);
		
		JSplitPane sourceOutputSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, overallTabbedPane, studentOutputTabs);
		sourceOutputSplit.setResizeWeight(0.7);
		add(sourceOutputSplit, BorderLayout.CENTER);
		
		
		
		


	}

	private void registerListeners() {
		ListenerCoordinator.addListener(AddRubricTabsListener.class, new AddRubricTabsListener() {
			@Override
			public void fired(Rubric rubric) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						rubricTabPanel.changeRubricTabs(rubric, undoManager);

						@SuppressWarnings("unchecked")
						List<String> studentIDs = (ArrayList<String>)ListenerCoordinator.runQuery(GetStudentIDListQuery.class);
						studentOutputTabs.bindStudentAreas(studentIDs, rubric);
					}

				});
			}
		});

		ListenerCoordinator.addBlockingListener(PreRunBlockingListener.class, new PreRunBlockingListener() {
			public void fired(String studentID, String rubricName) {
				if (rubricName == null || rubricName.length() == 0) {
					studentOutputTabs.selectOutputTab(StudentOutputTabs.CONSOLE_TAB_NAME);
					String studentName = (String)ListenerCoordinator.runQuery(GetStudentNameQuery.class, studentID);
					studentOutputTabs.setRunningStudent(studentName);
				}
				else {
					if (overallTabbedPane.getTabCount() > OverallTabNames.Source.ordinal()) {
						overallTabbedPane.setSelectedIndex(OverallTabNames.Source.ordinal());
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
					DebugLogDialog.appendException(e);
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
		
		ListenerCoordinator.addListener(AllStudentFilesLoadedListener.class, new AllStudentFilesLoadedListener() {
			@Override
			public void fired() {
				@SuppressWarnings("unchecked")
				List<String> allIDs = (ArrayList<String>)ListenerCoordinator.runQuery(GetStudentIDListQuery.class);
				studentSourceCards.initSource(allIDs, undoManager, overallTabbedPane, OverallTabNames.Source.ordinal(), popupSource);
				Rubric rubric = (Rubric)ListenerCoordinator.runQuery(GetCurrentRubricQuery.class);
				// Only bind if we don't have a rubric, if we do we already have all the tabs we need.
				if (rubric == null) {
					studentOutputTabs.bindStudentAreas(allIDs, rubric);
				}
			}			
		});

	}
	
	public JTextArea getCurrentSource() {
		return studentSourceCards.getCurrentSource(currentID);
	}
	
	public void selectOutputTab(String tabName) {
		studentOutputTabs.selectOutputTab(tabName);
	}
		
	
	public void updateRubricCode(Rubric rubric) {
		rubricTabPanel.updateRubricCode();
	}	

    



}
