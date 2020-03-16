package net.cdonald.googleClassroom.gui;
/**
 * To build the release version. Open a cmd prompt.  Go to the GradeAsst directory in the repro
 * run gradle fatJar this will put a new jar in the libs section
 * Then open NSIS and go to the ExportStuff directory & run the build script
 * That will create a new install.exe in the ExportStuff directory
 */
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.undo.UndoManager;

import net.cdonald.googleClassroom.control.DataController;
import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.ChooseGradeFileListener;
import net.cdonald.googleClassroom.listenerCoordinator.ClassSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.ExitFiredListener;
import net.cdonald.googleClassroom.listenerCoordinator.GetDebugDialogQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetFileDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GradeFileSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchFindReplaceDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchGuidedSetupListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchNewRubricDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchOptionsDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricEditorDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricFileDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LoadSourceQuery;
import net.cdonald.googleClassroom.listenerCoordinator.LoadTestFileListener;
import net.cdonald.googleClassroom.listenerCoordinator.PublishGradesListener;
import net.cdonald.googleClassroom.listenerCoordinator.RecompileListener;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveInstrumentationListener;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveSourceListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricColumnChanged;
import net.cdonald.googleClassroom.listenerCoordinator.RubricFileSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.RunRubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.RunSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SaveGradesListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetFileDirListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;

public class MainGoogleClassroomFrame extends JFrame implements DataUpdateListener {
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
	//private RubricElementDialog rubricElementDialog;
	private NewRubricDialog newRubricDialog;
	private InfoPanel infoPanel;
	private DebugLogDialog dbg;
	private GuidedSetupDialog guidedSetup;
	private FindReplaceDialog replaceDialog;
	private UndoManager undoManager;
	private OptionsDialog optionsDialog;


	public MainGoogleClassroomFrame() throws InterruptedException {
		super(APP_NAME);
		undoManager = new UndoManager();

		dbg = new DebugLogDialog(this);
		dataController = new DataController(this, undoManager);				
		newRubricDialog = new NewRubricDialog(this);
		guidedSetup = new GuidedSetupDialog(this, dataController);
		replaceDialog = new FindReplaceDialog(this);
		optionsDialog = new OptionsDialog(this,dataController.getPrefs());		
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
		else {
		
			dataController.performFirstInit();
		

	
			if (dataController.getPrefs().getClassroom() == null) {
				new GuidedSetupFinalInstructions(this).setVisible(true);
			}
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
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//			for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
//				// System.out.println(info.getName());
//				if ("Windows".equals(info.getName())) {
//					UIManager.setLookAndFeel(info.getClassName());
//				}
//			}
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
		consoleAndSourcePanel = new ConsoleAndSourcePanel(undoManager);
		mainToolBar = new MainToolBar();
		studentPanel = new StudentPanel(undoManager, dataController, prefs.getSplitLocation(MyPreferences.Dividers.STUDENT_NOTES));
		mainMenu = new MainMenu(this, undoManager);
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
				studentPanel.stopEditing();
				if (dataController.areGradesModified()) {
					int option = JOptionPane.showConfirmDialog(MainGoogleClassroomFrame.this,  "Save Grades Before Exiting?", "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION);
					if (option == JOptionPane.CANCEL_OPTION) {
						return;
					}
					if (option == JOptionPane.YES_OPTION) {
						SwingWorker<Void, Void> saveWorker = new SwingWorker<Void, Void>() {
							private boolean worked;
							@Override
							protected Void doInBackground() throws Exception {
								worked = saveGradesInCurrentThread();
								return null;
							}

							@Override
							protected void done() {
								if (worked) {
									finalizeExit();
								}
							}														
						};
						saveWorker.execute();
						return;
					}
					else {
						finalizeExit();
					}
				}
				else {
					finalizeExit();
				}
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
				Rubric rubric = dataController.getRubric(); 
				if (rubric == null) {					
					return;
				}
				consoleAndSourcePanel.updateRubricCode(rubric);
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
				consoleAndSourcePanel.editRubric();
			}
		});
		
		ListenerCoordinator.addListener(LaunchNewRubricDialogListener.class, new LaunchNewRubricDialogListener() {
			@Override
			public void fired() {
				newRubricDialog.setVisible(true);
				String rubricName = newRubricDialog.getRubricName();
				if (rubricName != null) {
					Rubric temp = dataController.newRubric(rubricName);
					consoleAndSourcePanel.newRubric(temp);
					mainToolBar.addRubricInfo(temp.getSheetInfo(), true);					
				}
			}
		});
		
		ListenerCoordinator.addQueryResponder(GetDebugDialogQuery.class, new GetDebugDialogQuery() {
			public DebugLogDialog fired() {
				return dbg;
			}
		});
		
		ListenerCoordinator.addQueryResponder(LoadSourceQuery.class, new LoadSourceQuery() {
			@Override
			public List<FileData> fired() {
				// TODO Auto-generated method stub
				return loadSource();
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
			public void fired(String studentID) { 
				dataController.recompile(studentID);
				dataUpdated();					
				studentPanel.setSelectedStudent(studentID);
			}			
		});
		ListenerCoordinator.addListener(RemoveSourceListener.class, new RemoveSourceListener() {
			@Override
			public void fired(String studentID, String fileName) {
				dataController.removeSource(studentID, fileName);								
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
		
		ListenerCoordinator.addListener(RemoveInstrumentationListener.class, new RemoveInstrumentationListener() {
			public void fired(String studentID, String fileName) {
				
				dataController.removeInstrumentation(studentID, fileName);
				consoleAndSourcePanel.refreshInfo();
			}
		});
		
		ListenerCoordinator.addListener(LaunchFindReplaceDialogListener.class, new LaunchFindReplaceDialogListener() {

			@Override
			public void fired() {
				JTextArea source = consoleAndSourcePanel.getCurrentSource();
				if (source != null) {
					replaceDialog.showDialog(source);					
				}				
			}
			
		});
		
		ListenerCoordinator.addListener(LoadTestFileListener.class, new LoadTestFileListener() {
			@Override
			public void fired() {
				List<FileData> files = loadSource();
				if (files != null) {
					dataController.setTestFile(files);
					structureChanged();
					enableRuns();
				}
				
			}			
		});
		
		ListenerCoordinator.addListener(LaunchOptionsDialogListener.class, new LaunchOptionsDialogListener() {
			public void fired() {
				optionsDialog.setVisible(true);
			}
		});
		
		ListenerCoordinator.addListener(RubricColumnChanged.class, new RubricColumnChanged() {
			public void fired(int columnNumber) {
				RubricEntry entry = dataController.getColumnEntry(columnNumber);
				if (entry != null) {
					consoleAndSourcePanel.selectOutputTab(entry.getName());
				}
				else {
					consoleAndSourcePanel.selectOutputTab("Console");
				}
			}
		});
	
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
		if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
			for (File file : fileChooser.getSelectedFiles()) { 
				Path path = Paths.get(file.getAbsolutePath());
				ListenerCoordinator.fire(SetFileDirListener.class, path.getParent().toString());
				String fileName = path.getFileName().toString();
				
				try {
					String text = new String(Files.readAllBytes(path));
					FileData fileData = new FileData(fileName, text, FileData.REFERENCE_SOURCE_ID, null);
					allFiles.add(fileData);
				} catch (IOException e1) {
					DebugLogDialog.appendException(e1);
				}						
			}
		}
		return allFiles;
	}
	
	private void saveGrades() {
				saveGradesInCurrentThread();		
	}
	
	private boolean saveGradesInCurrentThread() {
		ListenerCoordinator.fire(AddProgressBarListener.class, "Syncing Grades");
		studentPanel.stopEditing();				
		boolean worked = dataController.syncGrades();
		ListenerCoordinator.fire(RemoveProgressBarListener.class, "Syncing Grades");
		return worked;
	}

	
	private void runRubricOrCode(boolean runSource, boolean runAll) {
		runWorker = new SwingWorker<Void, String>() {
			private String lastId = null;
			private Map<String, Set<String>> entriesSkipped = null;

			@Override
			protected void process(List<String> chunks) {
				if (chunks.size() != 0) {
					lastId = chunks.get(chunks.size() - 1);
				}
				dataUpdated();
			}
			@Override
			protected Void doInBackground() throws Exception {
				consoleAndSourcePanel.syncSource();
				
				
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
					dataController.addEdits(true);
					for (String id : ids) {						
						if (runSource == true) {
							dataController.run(id);												
						}
						else {

							Set<String> skipped = dataController.runRubric(id, selectedRubricNames);
							if (skipped != null && skipped.size() != 0) {
								if (entriesSkipped == null) {
									entriesSkipped = new HashMap<String, Set<String> >();
								}
								entriesSkipped.put(id, skipped);				
							}
						}
						publish(id);
						
					}
				} catch (Exception e) {
					JOptionPane.showMessageDialog(MainGoogleClassroomFrame.this, e.getMessage(), "Error while running",
							JOptionPane.ERROR_MESSAGE);
					e.printStackTrace();
					DebugLogDialog.appendException(e);
					System.out.println("\0");
				}
				dataController.addEdits(false);
				return null;
			}
			@Override
			protected void done() {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						studentPanel.setSelectedStudent(lastId);
						mainToolBar.setStopEnabled(false);
						if (entriesSkipped != null) {
							dataController.setShowRedMap(entriesSkipped);
							dataUpdated();
							JOptionPane.showMessageDialog(MainGoogleClassroomFrame.this, "Entries shown in red were not updated because they already had a score.\nDelete the score if you want automation to change the value.\nThe red will be removed when you close this dialog.", "Some Entries Not Updated",
									JOptionPane.OK_OPTION);
							dataController.setShowRedMap(null);
							dataUpdated();					
						}
						entriesSkipped = null;						

						enableRuns();
						
					}
				});

			}
		};
		runWorker.execute();
		
	}
	
	private void disableRuns() {
		mainToolBar.disableRunButtons();
		mainMenu.disableConditionalMenus();
	}
	
	@Override
	public void enableRuns() {
		boolean rubricSelected = (dataController.getRubric() != null);
		mainToolBar.enableRunButton(rubricSelected);
		mainMenu.enableConditionalMenuItems(studentPanel.isAnyStudentSelected(), rubricSelected);
	}

	@Override
	public void dataUpdated() {
		studentPanel.dataChanged();
	}
	
	@Override
	public void structureChanged() {
		studentPanel.structureChanged();
	}

}
