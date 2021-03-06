package net.cdonald.googleClassroom.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.UndoManager;

import net.cdonald.googleClassroom.googleClassroomInterface.CourseFetcher;
import net.cdonald.googleClassroom.listenerCoordinator.ChooseGradeFileListener;
import net.cdonald.googleClassroom.listenerCoordinator.ClassSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.ExitFiredListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchFindReplaceDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchGuidedSetupListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchNewRubricDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchOptionsDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricEditorDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricFileDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LoadTestFileListener;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryListener;
import net.cdonald.googleClassroom.listenerCoordinator.PublishGradesListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricFileValidListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.RunJPLAGListener;
import net.cdonald.googleClassroom.listenerCoordinator.RunRubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.RunSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SaveGradesListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentSelectedListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.GoogleSheetData;

public class MainMenu extends JMenuBar {
	private static final long serialVersionUID = 5459790818047149118L;
	private JFrame owner;
	private JMenu file;
	private JMenu rubric;
	private JMenu help;
	private JMenu edit;
	private JMenu run;
	private JMenu jplag;
	private JMenu openClassroom;
	private JMenuItem runJPLAG;
	private JMenuItem newRubric;
	private JMenuItem editRubric;
	private JMenuItem runAllRubrics;
	private JMenuItem runSelectedRubrics;
	private JMenuItem runAll;
	private JMenuItem runSelected;
	private JMenuItem publishGrades;
	private JMenuItem publishSelectedGrades;
	private Map<Integer, JMenu> classByYear;
	private List<Integer> years;
	private UndoManager undoManager;

	public MainMenu(JFrame owner, UndoManager undoManager) {
		this.owner = owner;
		this.undoManager = undoManager;
		classByYear = new HashMap<Integer, JMenu>();
		years = new ArrayList<Integer>();
		file = new JMenu("File");
		edit = new JMenu("Edit");
		rubric = new JMenu("Rubrics");
		jplag = new JMenu("JPLAG");
		run = new JMenu("Run");
		help = new JMenu("Help");

		fillFileMenu();
		fillEditMenu();
		fillRunMenu();
		fillRubricMenu();
		fillJPLAGMenu();
		fillHelpMenu();
	}

	private void fillFileMenu() {

		openClassroom = new JMenu("Open Classroom");
		JMenuItem chooseGradeFile = new JMenuItem("Choose Grade File...");
		JMenuItem syncGrades = new JMenuItem("Sync Modified Grades to Sheet");
		JMenuItem syncAllGrades = new JMenuItem("Sync All Grades to Sheet");
		syncAllGrades.setToolTipText("A bit slower, saves all grades, even those that have not changed.");
		syncGrades.setToolTipText("Loads & Saves all changes made since last save to the grade file.");
		publishGrades = new JMenuItem("Publish Grades");
		publishGrades.setToolTipText("Pushes grades back up to google classroom for the students to see");
		publishSelectedGrades = new JMenuItem("Publish Selected Grades");
		publishSelectedGrades.setToolTipText("Pushes grades of the selected students back up to google classroom");
		JMenuItem loadTestFile = new JMenuItem("Load Test File...");
		loadTestFile.setToolTipText("Useful for testing a new rubric, load a file off of disk to run against");

		syncGrades.setEnabled(false);
		publishGrades.setEnabled(false);
		publishSelectedGrades.setEnabled(false);
		JMenuItem exit = new JMenuItem("Exit");

		file.add(openClassroom);
		file.addSeparator();
		file.add(chooseGradeFile);
		file.add(syncGrades);
		file.add(syncAllGrades);
		file.addSeparator();
		file.add(publishGrades);
		file.add(publishSelectedGrades);
		file.addSeparator();
		file.add(loadTestFile);
		file.addSeparator();
		file.add(exit);
		file.setMnemonic(KeyEvent.VK_F);
		exit.setMnemonic(KeyEvent.VK_X);
		syncGrades.setMnemonic(KeyEvent.VK_S);
		syncGrades.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
		exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
		add(file);

		chooseGradeFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(ChooseGradeFileListener.class);

			}

		});

		syncGrades.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(SaveGradesListener.class, false);

			}
		});
		
		syncAllGrades.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(SaveGradesListener.class, true);

			}
		});

		ListenerCoordinator.runLongQuery(CourseFetcher.class, new LongQueryListener<ClassroomData>() {
			@Override
			public void process(List<ClassroomData> list) {
				for (ClassroomData classroom : list) {
					addClass(classroom);
				}
			}

			@Override
			public void remove(Set<String> removeList) {
			}
		});

		publishGrades.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(PublishGradesListener.class, true);

			}
		});

		publishSelectedGrades.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(PublishGradesListener.class, false);

			}
		});
		
		loadTestFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(LoadTestFileListener.class);				
			}			
		});
		exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(ExitFiredListener.class);
			}
		});

		ListenerCoordinator.addListener(RubricSelected.class, new RubricSelected() {
			@Override
			public void fired(GoogleSheetData googleSheet) {
				boolean enable = googleSheet != null && !googleSheet.isEmpty();
				syncGrades.setEnabled(enable);
				editRubric.setEnabled(enable);
				publishGrades.setEnabled(enable);
			}
		});
	}

	private void fillEditMenu() {
		JMenuItem findReplace = new JMenuItem("Find/Replace");
		findReplace.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
		edit.add(findReplace);
		edit.addSeparator();
		findReplace.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(LaunchFindReplaceDialogListener.class);
			}

		});

		JMenuItem undo = new JMenuItem("Undo");
		undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, ActionEvent.CTRL_MASK));


		edit.add(undo);
		undo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (undoManager.canUndo()) {
					undoManager.undo();
				}
			}
		});
		JMenuItem redo = new JMenuItem("Redo");
		redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Y, ActionEvent.CTRL_MASK));
		edit.add(redo);
		redo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (undoManager.canRedo()) {
					undoManager.redo();
				}
			}
		});
		edit.addSeparator();
		JMenuItem options = new JMenuItem("Options...");
		options.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(LaunchOptionsDialogListener.class);
			}
		});
		edit.add(options);

	
		add(edit);
	
	}

	private void fillRunMenu() {
		runAll = new JMenuItem("Run All");
		runSelected = new JMenuItem("Run Selected");
		runAll.setEnabled(false);
		runSelected.setEnabled(false);
		run.add(runAll);
		run.add(runSelected);

		add(run);
		runAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(RunSelected.class, true);
			}
		});

		runSelected.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(RunSelected.class, false);
			}
		});

	}

	private void fillRubricMenu() {

		runAllRubrics = new JMenuItem("Run All Rubrics");
		runSelectedRubrics = new JMenuItem("Run Selected Rubrics");
		newRubric = new JMenuItem("New Rubric...");
		editRubric = new JMenuItem("Edit Rubric...");
		newRubric.setEnabled(true);
		editRubric.setEnabled(false);
		runAllRubrics.setEnabled(false);
		runSelectedRubrics.setEnabled(false);
		JMenuItem setRubricFile = new JMenuItem("Rubric File...");


		rubric.add(runAllRubrics);
		rubric.add(runSelectedRubrics);
		rubric.addSeparator();
		rubric.add(setRubricFile);
		rubric.addSeparator();
		rubric.add(newRubric);
		rubric.add(editRubric);


		add(rubric);
		runAllRubrics.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(RunRubricSelected.class, true);
			}
		});

		runSelectedRubrics.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(RunRubricSelected.class, false);
			}
		});

		setRubricFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(LaunchRubricFileDialogListener.class);
			}
		});

		newRubric.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(LaunchNewRubricDialogListener.class);
			}
		});

		editRubric.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(LaunchRubricEditorDialogListener.class);
			}
		});

		ListenerCoordinator.addListener(RubricFileValidListener.class, new RubricFileValidListener() {
			@Override
			public void fired() {
				newRubric.setEnabled(true);
				runSelectedRubrics.setEnabled(runSelected.isEnabled());
			}
		});

		ListenerCoordinator.addListener(RubricSelected.class, new RubricSelected() {
			@Override
			public void fired(GoogleSheetData googleSheet) {
				boolean enable = googleSheet != null && !googleSheet.isEmpty();
				editRubric.setEnabled(enable);
				runAllRubrics.setEnabled(enable);
			}
		});
		ListenerCoordinator.addListener(StudentSelectedListener.class, new StudentSelectedListener() {
			@Override
			public void fired(String idToDisplay) {
				runSelectedRubrics.setEnabled(newRubric.isEnabled() && (idToDisplay != null));
				runSelected.setEnabled(runAll.isEnabled() && idToDisplay != null);
				publishSelectedGrades.setEnabled(publishGrades.isEnabled() && (idToDisplay != null));
			}
		});
		
		

	}

	public void fillJPLAGMenu() {
		runJPLAG = new JMenuItem("Run JPLAG...");
		runJPLAG.setEnabled(false);
		jplag.add(runJPLAG);
		add(jplag);
		runJPLAG.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(RunJPLAGListener.class);
			}
		});
	}

	private void fillHelpMenu() {
		JMenuItem runGuidedSetup = new JMenuItem("Run Guided Setup");
		JMenuItem showDebugLog = new JMenuItem("Show Debug Log");
		JMenuItem showAbout = new JMenuItem("About...");

		help.add(showDebugLog);
		help.addSeparator();
		help.add(showAbout);
		help.addSeparator();
		help.add(runGuidedSetup);
		add(help);
		showDebugLog.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DebugLogDialog.showDebugInfo();
			}
		});

		showAbout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JTextArea message = new JTextArea(3, 100);
				message.setText(
						"Grading Assistant Version 1.0\n\nSend bugs/enhancement requests to keithm@cdonald.net\n\nGit Repro: https://github.com/keithmacspace/classroomGrader2.git");
				message.setWrapStyleWord(true);
				message.setLineWrap(true);
				message.setCaretPosition(0);
				message.setEditable(false);
				JLabel label = new JLabel();
				message.setBackground(label.getBackground());
				JPopupMenu popupSource = new JPopupMenu();
				Action copy = new DefaultEditorKit.CopyAction();
				copy.putValue(Action.NAME, "Copy");
				copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
				popupSource.add(copy);
				message.setComponentPopupMenu(popupSource);
				JOptionPane.showMessageDialog(owner, message, "About Grading Assistant",
						JOptionPane.INFORMATION_MESSAGE);
			}
		});

		runGuidedSetup.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(LaunchGuidedSetupListener.class);
			}

		});
	}

	public void addClass(ClassroomData classroom) {
		Date creationDate = classroom.getDate();
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(creationDate);
		int year = calendar.get(Calendar.YEAR);
		if (classByYear.get(year) == null) {
			String yearVal = year + "/" + (year + 1);
			boolean inserted = false;
			JMenu menu = new JMenu(yearVal);
			for (int i = 0; i < years.size(); i++) {
				if (year > years.get(i)) {
					openClassroom.insert(menu, i);
					years.add(year);
					inserted = true;
					break;
				}
			}
			if (inserted == false) {
				years.add(year);
				openClassroom.add(menu);
			}
			classByYear.put(year, menu);
		}
		JMenu yearMenu = classByYear.get(year);
		JMenuItem classroomOption = new JMenuItem(classroom.getName());

		classroomOption.addActionListener(new OpenClassroomListener(classroom));
		yearMenu.add(classroomOption);
	}

	public void removeClasses(Set<String> ids) {
		for (String id : ids) {
			boolean removed = false;
			for (int i = 0; removed == false && i < openClassroom.getItemCount(); i++) {
				ActionListener[] listeners = openClassroom.getActionListeners();
				for (ActionListener listener : listeners) {
					if (listener instanceof OpenClassroomListener) {
						if (((OpenClassroomListener) listener).getClassroom().getId().equalsIgnoreCase(id)) {
							openClassroom.remove(i);
							removed = true;
							break;
						}
					}
				}
			}
		}
	}

	private class OpenClassroomListener implements ActionListener {
		ClassroomData classroom;

		public ClassroomData getClassroom() {
			return classroom;
		}

		public OpenClassroomListener(ClassroomData classroom) {
			super();
			this.classroom = classroom;

		}

		@Override
		public void actionPerformed(ActionEvent e) {
			ListenerCoordinator.fire(ClassSelectedListener.class, classroom);
		}

	}

	public void disableConditionalMenus() {
		runJPLAG.setEnabled(false);
		runAllRubrics.setEnabled(false);
		runSelectedRubrics.setEnabled(false);
		runAll.setEnabled(false);
		runSelected.setEnabled(false);
		publishGrades.setEnabled(false);
		publishSelectedGrades.setEnabled(false);

	}

	public void enableConditionalMenuItems(boolean enableSelected, boolean rubricLoaded) {
		runJPLAG.setEnabled(true);
		runAllRubrics.setEnabled(rubricLoaded);
		runSelectedRubrics.setEnabled(enableSelected);
		runAll.setEnabled(true);
		runSelected.setEnabled(enableSelected && rubricLoaded);
		publishGrades.setEnabled(rubricLoaded);
		publishSelectedGrades.setEnabled(enableSelected && rubricLoaded);
	}

}
