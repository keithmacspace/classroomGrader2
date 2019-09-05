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

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import net.cdonald.googleClassroom.googleClassroomInterface.CourseFetcher;
import net.cdonald.googleClassroom.listenerCoordinator.ChooseGradeFileListener;
import net.cdonald.googleClassroom.listenerCoordinator.ClassSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.ExitFiredListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchGuidedSetupListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchNewRubricDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricEditorDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricFileDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
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
	
	public MainMenu(JFrame owner) {
		this.owner = owner;
		classByYear = new HashMap<Integer, JMenu>();
		years = new ArrayList<Integer>();
		file = new JMenu("File");
		rubric = new JMenu("Rubrics");
		jplag = new JMenu("JPLAG");
		edit = new JMenu("Edit");
		run = new JMenu("Run");
		help = new JMenu("Help");
		
		fillFileMenu();
		fillRunMenu();
		fillRubricMenu();
		fillJPLAGMenu();
		fillEditMenu();		
		fillHelpMenu();		
	}
	
	private void fillFileMenu() {

		openClassroom = new JMenu("Open Classroom");
		JMenuItem chooseGradeFile = new JMenuItem("Choose Grade File...");		
		JMenuItem syncGrades = new JMenuItem("Sync Grades to Sheet");
		syncGrades.setToolTipText("Loads & Saves grades to the grade file - not seen by students");
		publishGrades = new JMenuItem("Publish Grades");
		publishGrades.setToolTipText("Pushes grades back up to google classroom for the students to see");
		publishSelectedGrades = new JMenuItem("Publish Selected Grades");
		publishSelectedGrades.setToolTipText("Pushes grades of the selected students back up to google classroom");
		
		syncGrades.setEnabled(false);
		publishGrades.setEnabled(false);
		publishSelectedGrades.setEnabled(false);
		JMenuItem exit = new JMenuItem("Exit");
		
		file.add(openClassroom);
		file.addSeparator();
		file.add(chooseGradeFile);
		file.add(syncGrades);
		file.addSeparator();
		file.add(publishGrades);
		file.add(publishSelectedGrades);
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
				ListenerCoordinator.fire(SaveGradesListener.class);
				
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
		exit.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(ExitFiredListener.class);
			}			
		});
		

		
		ListenerCoordinator.addListener(RubricSelected.class, new RubricSelected() {
			@Override
			public void fired(GoogleSheetData googleSheet) {
				syncGrades.setEnabled(!googleSheet.isEmpty());
				editRubric.setEnabled(!googleSheet.isEmpty());
				publishGrades.setEnabled(!googleSheet.isEmpty());
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
		JMenuItem saveRubric = new JMenuItem("Save Rubric");
		
		rubric.add(runAllRubrics);
		rubric.add(runSelectedRubrics);
		rubric.addSeparator();
		rubric.add(setRubricFile);
		rubric.addSeparator();
		rubric.add(newRubric);
		rubric.add(editRubric);
		rubric.add(saveRubric);

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
				editRubric.setEnabled(!googleSheet.isEmpty());
				runAllRubrics.setEnabled(!googleSheet.isEmpty());
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
	
	private void fillEditMenu() {
		
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
	
	private void fillHelpMenu() {
		JMenuItem runGuidedSetup = new JMenuItem("Run Guided Setup");
		JMenuItem showDebugLog = new JMenuItem("Show Debug Log");
		help.add(runGuidedSetup);
		help.addSeparator();
		help.add(showDebugLog);
		add(help);
		showDebugLog.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				DebugLogDialog.showDebugInfo();
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
						if (((OpenClassroomListener)listener).getClassroom().getId().equalsIgnoreCase(id)) {
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
