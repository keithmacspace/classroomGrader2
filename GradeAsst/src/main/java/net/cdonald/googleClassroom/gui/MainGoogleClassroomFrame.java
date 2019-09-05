package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

import net.cdonald.googleClassroom.control.DataController;
import net.cdonald.googleClassroom.googleClassroomInterface.SaveSheetGrades;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompileListener;
import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.ChooseGradeFileListener;
import net.cdonald.googleClassroom.listenerCoordinator.ClassSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.ExitFiredListener;
import net.cdonald.googleClassroom.listenerCoordinator.GetDebugDialogQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GradeFileSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchGuidedSetupListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchNewRubricDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricEditorDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricFileDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.PublishGradesListener;
import net.cdonald.googleClassroom.listenerCoordinator.RecompileListener;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveSourceListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricFileSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.RunRubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.RunSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SaveGradesListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.model.Rubric;

public class MainGoogleClassroomFrame extends JFrame implements CompileListener {
	private static final long serialVersionUID = 7452928818734325088L;
	public static final String APP_NAME = "Google Classroom Grader";
	private StudentPanel studentPanel;
	private MainToolBar mainToolBar;
	private JSplitPane splitPanePrimary;
	private ConsoleAndSourcePanel consoleAndSourcePanel;
	private SwingWorker<Void, String> runWorker;
	private MainMenu mainMenu;
	private GoogleSheetDialog importExportDialog;
	private DataController dataController;		
	private RubricElementDialog rubricElementDialog;
	private NewRubricDialog newRubricDialog;
	private InfoPanel infoPanel;
	private DebugLogDialog dbg;
	private GuidedSetupDialog guidedSetup;



	public MainGoogleClassroomFrame() throws InterruptedException {
		super(APP_NAME);
		dbg = new DebugLogDialog(this);
		dataController = new DataController(this);		
		rubricElementDialog = new RubricElementDialog(this, dataController.getPrefs(), dataController.getStudentWorkCompiler());
		newRubricDialog = new NewRubricDialog(this);
		guidedSetup = new GuidedSetupDialog(this, dataController);

		
		setLayout();		

		
		
		importExportDialog = new GoogleSheetDialog(this);

		registerListeners();
		
		boolean ranGuided = false;
		if (dataController.getPrefs().getJsonPath() == null) {
			runGuidedSetup(true);
			ranGuided = true;			
		}
		
		setVisible(true);
		if (ranGuided == true) {
			callExit();
		}
		
		dataController.performFirstInit();
		

	
		if (dataController.getPrefs().getClass() == null) {
			new GuidedSetupFinalInstructions(this).setVisible(true);
		}
	}
	

	private boolean runGuidedSetup(boolean forceRun) {
		if (forceRun == true) {
			guidedSetup.runGuidedSetup();
			return true;
		}
		return false;
	}


	private void setLayout() {
		try {
			// UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
				// System.out.println(info.getName());
				if ("Windows".equals(info.getName())) {
					UIManager.setLookAndFeel(info.getClassName());
				}
			}
			// UIManager.setLookAndFeel("javax.swing.plaf.motif.MotifLookAndFeel");
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
		MyPreferences prefs = dataController.getPrefs();
		this.setSize(prefs.getDimension(MyPreferences.Dimensions.MAIN, 800, 700));
		setLayout(new BorderLayout());
		consoleAndSourcePanel = new ConsoleAndSourcePanel();
		mainToolBar = new MainToolBar();
		studentPanel = new StudentPanel(dataController, prefs.getSplitLocation(MyPreferences.Dividers.STUDENT_NOTES));
		mainMenu = new MainMenu(this);
		infoPanel = new InfoPanel();
		setJMenuBar(mainMenu);

		add(mainToolBar, BorderLayout.PAGE_START);
		add(studentPanel, BorderLayout.WEST);
		splitPanePrimary = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, studentPanel, consoleAndSourcePanel);
		int splitLocation = dataController.getPrefs().getSplitLocation(MyPreferences.Dividers.STUDENT_SOURCE);
		if (splitLocation != 0) {
			splitPanePrimary.setDividerLocation(splitLocation);
		}
		add(splitPanePrimary, BorderLayout.CENTER);
		add(infoPanel, BorderLayout.SOUTH);


		setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent ev) {
				if (dataController.isGradesModified()) {
					int option = JOptionPane.showConfirmDialog(MainGoogleClassroomFrame.this,  "Save Grades Before Exiting?", "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
					if (option == JOptionPane.CANCEL_OPTION) {
						return;
					}
					if (option == JOptionPane.YES_OPTION) {
						SwingWorker<Void, Void> saveWorker = new SwingWorker<Void, Void>() {

							@Override
							protected Void doInBackground() throws Exception {
								saveGrades();
								return null;
							}

							@Override
							protected void done() {
								finalizeExit();
							}														
						};
						saveWorker.execute();
						return;
					}
				}
				finalizeExit();
			}
		});
	}
	
	// WARNING this should only be called from the window close event!
	private void finalizeExit() {
		MyPreferences prefs = dataController.getPrefs();
		prefs.setDimension(MyPreferences.Dimensions.MAIN, getSize());
		prefs.setSplitLocation(MyPreferences.Dividers.STUDENT_SOURCE, splitPanePrimary.getDividerLocation());
		prefs.setSplitLocation(MyPreferences.Dividers.STUDENT_NOTES, studentPanel.getDividerLocation());
		dataController.closing();
		dispose();
		System.gc();
		
	}
	
	
	
	private void callExit() {
		WindowListener[] listeners = getWindowListeners();
		for (WindowListener listener : listeners) {
			listener.windowClosing(new WindowEvent(MainGoogleClassroomFrame.this, 0));
		}				
		
	}


	private void registerListeners() {

		ListenerCoordinator.addListener(LaunchGuidedSetupListener.class, new LaunchGuidedSetupListener() {
			@Override
			public void fired() {
				runGuidedSetup(true);
				callExit();
			}
		});
		
		
		ListenerCoordinator.addListener(ClassSelectedListener.class, new ClassSelectedListener() {
			@Override
			public void fired(ClassroomData course) {
				if (course != null) {
					setTitle(APP_NAME + " - " + course.getName());
				}
				else {
					setTitle(APP_NAME);
				}
			}
		}		);

		
		ListenerCoordinator.addListener(ExitFiredListener.class, new ExitFiredListener() {
			public void fired() {
				callExit();
			}
		});
		
		ListenerCoordinator.addListener(RunRubricSelected.class, new RunRubricSelected() {

			@Override
			public void fired(boolean runAll) {
				if (dataController.getRubric() == null) {
					return;
				}
				runRubricOrCode(false, runAll);
			}
		});

		ListenerCoordinator.addListener(RunSelected.class, new RunSelected() {
			public void fired(boolean runAll) {
				runRubricOrCode(true, runAll);
			}
		});
		
 
		
		ListenerCoordinator.addListener(SaveGradesListener.class, new SaveGradesListener() {
			@Override
			public void fired() {
				saveGrades();				
			}			
		});
		
		ListenerCoordinator.addListener(LaunchRubricFileDialogListener.class, new LaunchRubricFileDialogListener() {
			@Override
			public void fired() {
				importExportDialog.setVisible("Select Rubric File", RubricFileSelectedListener.class, dataController.getRubricURL());			
			}			
		});
		
		ListenerCoordinator.addListener(LaunchRubricEditorDialogListener.class, new LaunchRubricEditorDialogListener() {
			@Override
			public void fired() {
				editRubric(dataController.getRubric());
			}
		});
		
		ListenerCoordinator.addListener(LaunchNewRubricDialogListener.class, new LaunchNewRubricDialogListener() {
			@Override
			public void fired() {
				newRubricDialog.setVisible(true);
				String rubricName = newRubricDialog.getRubricName();
				if (rubricName != null) {
					Rubric temp = dataController.newRubric(rubricName);
					mainToolBar.addRubricInfo(temp.getSheetInfo(), true);
					editRubric(temp);
					
				}
			}
		});
		
		ListenerCoordinator.addQueryResponder(GetDebugDialogQuery.class, new GetDebugDialogQuery() {
			public DebugLogDialog fired() {
				return dbg;
			}
		});
		
		ListenerCoordinator.addListener(ChooseGradeFileListener.class, new ChooseGradeFileListener() {
			@Override
			public void fired() {
				importExportDialog.setVisible("Select Grades File", GradeFileSelectedListener.class, dataController.getGradeFileURL());			
			}			
		});
		
		ListenerCoordinator.addListener(RecompileListener.class, new RecompileListener() {
			@Override
			public void fired(String studentID, String fileName, String fileText) { 
				dataController.recompile(studentID, fileName, fileText);
				dataUpdated();					
				studentPanel.setSelectedStudent(studentID);
			}			
		});
		ListenerCoordinator.addListener(RemoveSourceListener.class, new RemoveSourceListener() {
			@Override
			public void fired(String studentID, String fileName) {
				dataController.removeSource(studentID, fileName);
				dataUpdated();				
				studentPanel.setSelectedStudent(studentID);
			}			
		});
		
		ListenerCoordinator.addListener(PublishGradesListener.class, new PublishGradesListener() {
			@Override
			public void fired(boolean pushAll) {
				List<String> ids;
				if (pushAll == false) {
					ids = studentPanel.getSelectedIds();
				}
				else {
					ids = dataController.getAllIDs();
				}
				dataController.publishGrades(ids);
			}			
		});
	
	}
	
	private void saveGrades() {
		ListenerCoordinator.fire(AddProgressBarListener.class, "Saving Grades");				
		ClassroomData assignment = mainToolBar.getAssignmentSelected();
		SaveSheetGrades grades = dataController.newSaveGrades(assignment.getName());
		studentPanel.addStudentGrades(grades, dataController.getRubric());
		dataController.saveGrades(grades);
		dataUpdated();
		ListenerCoordinator.fire(RemoveProgressBarListener.class, "Saving Grades");

	}
	
	private void editRubric(Rubric rubricToModify) {
		 rubricElementDialog.modifyRubric(rubricToModify); 
	}
	
	private void runRubricOrCode(boolean runSource, boolean runAll) {
		runWorker = new SwingWorker<Void, String>() {
			private String lastId = null;
			 

			@Override
			protected void process(List<String> chunks) {
				if (chunks.size() != 0) {
					lastId = chunks.get(chunks.size() - 1);
				}
			}
			@Override
			protected Void doInBackground() throws Exception {
				disableRuns();				
				List<String> ids = null;
				if (runAll == false) {
					ids = studentPanel.getSelectedIds();
				}
				else {
					ids = dataController.getAllIDs();
				}
				Set<String> selectedRubricNames = null;
				if (runAll == false) {
					selectedRubricNames = studentPanel.getSelectedColumns();
				}
				try {
					mainToolBar.setStopEnabled(true);
					for (String id : ids) {
						if (runSource == true) {
							dataController.run(id);												
						}
						else {

							dataController.runRubric(id, selectedRubricNames);
						}
						publish(id);
					}
				} catch (Exception e) {
					JOptionPane.showMessageDialog(MainGoogleClassroomFrame.this, e.getMessage(), "Error while running",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();					
					System.out.println("\0");
				}
				return null;
			}
			@Override
			protected void done() {
				studentPanel.setSelectedStudent(lastId);
				enableRuns();
				mainToolBar.setStopEnabled(false);
			}
		};
		runWorker.execute();
		
	}
	
	private void disableRuns() {
		mainToolBar.disableRunButtons();
		mainMenu.disableConditionalMenus();
	}
	
	private void enableRuns() {
		boolean rubricSelected = (dataController.getRubric() != null);
		mainToolBar.enableRunButton(rubricSelected);
		mainMenu.enableConditionalMenuItems(studentPanel.isAnyStudentSelected(), rubricSelected);
	}

	@Override
	public void dataUpdated() {
		studentPanel.dataChanged();
	}


	@Override
	public void compileDone() {
		enableRuns();
	}	
}
