package net.cdonald.googleClassroom.control;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.text.DefaultEditorKit;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import net.cdonald.googleClassroom.googleClassroomInterface.AssignmentFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.CourseFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.FileFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.GoogleClassroomCommunicator;
import net.cdonald.googleClassroom.googleClassroomInterface.GradeSyncer;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.StudentFetcher;
import net.cdonald.googleClassroom.gui.DataUpdateListener;
import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.gui.MainGoogleClassroomFrame;
import net.cdonald.googleClassroom.gui.SetRubricListener;
import net.cdonald.googleClassroom.gui.SetRubricListener.RubricType;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabsListener;
import net.cdonald.googleClassroom.listenerCoordinator.AllStudentNamesLoadedListener;
import net.cdonald.googleClassroom.listenerCoordinator.AssignmentSelected;
import net.cdonald.googleClassroom.listenerCoordinator.ClassSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.EnableRunRubricQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetAndClearModifiedNotes;
import net.cdonald.googleClassroom.listenerCoordinator.GetCompilerMessageQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCompilerQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentAssignmentQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentClassQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentRubricQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentRubricURL;
import net.cdonald.googleClassroom.listenerCoordinator.GetDBNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetFileDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentIDListQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetWorkingDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GradeFileSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.GradesSyncedListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LoadGradesListener;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryListener;
import net.cdonald.googleClassroom.listenerCoordinator.OptionsDialogUpdated;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricFileSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.RunJPLAGListener;
import net.cdonald.googleClassroom.listenerCoordinator.SaveRubricListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetFileDirListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetInfoLabelListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetRunRubricEnableStateListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetWorkingDirListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentInfoChangedListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.CompileErrorListener;
import net.cdonald.googleClassroom.model.ConsoleData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.JPLAGInvoker;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntry.StudentScore;
import net.cdonald.googleClassroom.model.StudentData;
import net.cdonald.googleClassroom.utils.SimpleUtils;



public class DataController implements StudentListInfo {
	private StudentWorkCompiler studentWorkCompiler;
	private ConsoleData consoleData;
	private ClassroomData currentCourse;
	private boolean isAnonymous;
	private class StudentDataClass {
		private StudentData studentData;
		private StudentData anonymousStudent;
		public StudentDataClass(StudentData studentData, int studentNumber) {
			this.studentData = studentData;
			anonymousStudent = new StudentData(studentData);
			anonymousStudent.setName("LastName" + studentNumber);
			anonymousStudent.setFirstName("FirstName" + studentNumber);
		}
		public StudentData getStudent(boolean isAnonymous) {
			if (isAnonymous) {
				return anonymousStudent;
			}
			return studentData;
		}
		public StudentData getStudent() {
			return getStudent(isAnonymous);
		}
	}
	private List<StudentDataClass> studentData;
	private Map<String, StudentDataClass> studentMap;
	private StudentData testFile;
	private Rubric currentRubric;
	private Rubric primaryRubric;

	private DataUpdateListener updateListener;
	private GoogleClassroomCommunicator googleClassroom;
	private MyPreferences prefs;
	private ClassroomData rubricURL;
	private ClassroomData gradeURL;	
	private Rubric rubricBeingEdited;
	
	private Map<String, Set<String>> showRedMap;
	private Date dueDate;
	private Boolean addEdits;
	private UndoManager undoManager;
	private boolean markLateRed;
	private int lateTime;
	private MyPreferences.LateType lateType;



	public DataController(MainGoogleClassroomFrame mainFrame, UndoManager undoManager) {
		this.undoManager = undoManager;
		prefs = new MyPreferences();
		this.isAnonymous = prefs.getAnonymousNames();
		markLateRed = prefs.getLateDatesInRed();
		lateTime = prefs.getLateDateTime();
		lateType = prefs.getLateType();
		studentWorkCompiler = new StudentWorkCompiler(mainFrame);
		studentData = new ArrayList<StudentDataClass>();
		studentMap = new HashMap<String, StudentDataClass>();
		consoleData = new ConsoleData();		
		currentCourse = null;
		updateListener = mainFrame;
		
		showRedMap = null;
		addEdits = false;
		initGoogle();				
		clearNotesAndComments();
		registerListeners();
	}
	
	private void clearNotesAndComments() {
		
	}
	

	
	public void initGoogle() {
		if (googleClassroom != null)  {
			return;
		}
		if (prefs.getJsonPath() == null) {
			return;
		}
		try {
			googleClassroom = new GoogleClassroomCommunicator(MainGoogleClassroomFrame.APP_NAME, prefs.getTokenDir(), prefs.getJsonPath());
		} catch (IOException e) {
			DebugLogDialog.appendException(e);
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			DebugLogDialog.appendException(e);
			e.printStackTrace();
		}
	}
	
	public void testCredentials() throws IOException {
		googleClassroom.initServices();
	}
	
	public void performFirstInit() {
		DebugLogDialog.startMethod();
		if (prefs.getClassroom() != null) {
			currentCourse = prefs.getClassroom();
			ListenerCoordinator.fire(ClassSelectedListener.class, currentCourse);
		}
		else {
			
		}
		DebugLogDialog.endMethod();
	}
	
	private void registerListeners() {
		
		ListenerCoordinator.addLongQueryReponder(StudentFetcher.class, new StudentFetcher(googleClassroom));
		ListenerCoordinator.addLongQueryReponder(AssignmentFetcher.class, new AssignmentFetcher(googleClassroom));
		ListenerCoordinator.addLongQueryReponder(CourseFetcher.class, new CourseFetcher(googleClassroom));
		ListenerCoordinator.addLongQueryReponder(FileFetcher.class, new FileFetcher(googleClassroom));
		ListenerCoordinator.addLongQueryReponder(SheetFetcher.class, new SheetFetcher(googleClassroom));
		
		ListenerCoordinator.addListener(ClassSelectedListener.class, new ClassSelectedListener() {

			@Override
			public void fired(ClassroomData course) {
				setCurrentCourse(course);				
			}
			
		});
		
		ListenerCoordinator.addListener(AssignmentSelected.class, new AssignmentSelected() {
			@Override
			public void fired(ClassroomData data) {
				addEdits = false;
				studentWorkCompiler.clearData();
				undoManager.discardAllEdits();
				ListenerCoordinator.runLongQuery(FileFetcher.class, new LongQueryListener<ClassroomData>() {
					@Override
					public void process(List<ClassroomData> list) {
						for (ClassroomData data : list) {
							FileData fileData = (FileData) data;
							studentWorkCompiler.addFile(fileData);
							updateListener.dataUpdated();
						}
					}
					@Override
					public void done() {

						List<String> duplicates = studentWorkCompiler.checkForDuplicates();
						if (duplicates != null) {
							String message = "";
							for (int i = 0; i < duplicates.size(); i++) {
								StudentData student = DataController.this.studentMap.get(duplicates.get(i)).getStudent(false);
								message += student.getFirstName() + " " + student.getName();
								if (i != duplicates.size() - 1) {
									message += ", ";
								}
							}
							String have;
							String those;
							String students;
							if (duplicates.size() > 1) {
								have = " have ";
								those = "those ";
								students = "students' ";
							}
							else {
								have =  " has ";
								those = "that ";
								students = "student's ";
							}
							message += have + "two files with the same name.\nSelect " + those + students + " source, determine which is the right source, and delete the other source.";
							JOptionPane.showMessageDialog(null, message,  "Duplicate Source",
									JOptionPane.ERROR_MESSAGE);
							
						}
						updateListener.enableRuns();
					}
					@Override
					public void remove(Set<String> removeList) {
					}					
				});
			}			
		});
		

		ListenerCoordinator.addListener(CompileErrorListener.class, new CompileErrorListener() {
			@Override
			public void fired(String text) {
				System.err.println("Compile error caught " + text);				
			}			
		});
		
		ListenerCoordinator.addQueryResponder(GetCurrentClassQuery.class, new GetCurrentClassQuery() {
			@Override
			public ClassroomData fired() {
				return currentCourse;
			}
		});	
		
		ListenerCoordinator.addQueryResponder(GetFileDirQuery.class, new GetFileDirQuery() {
			@Override
			public String fired() {
				return prefs.getFileDir();
			}
		});
		
		ListenerCoordinator.addQueryResponder(GetStudentIDListQuery.class, new GetStudentIDListQuery() {
			@Override
			public List<String> fired() {
				List<String> allIDs = new ArrayList<String>();
				for (StudentDataClass studentData : studentData) {
					allIDs.add(studentData.getStudent().getId());
				}
				// TODO Auto-generated method stub
				return allIDs;
			}			
		});
		

		ListenerCoordinator.addQueryResponder(GetDBNameQuery.class, new GetDBNameQuery() {
			@Override
			public String fired(DBType type) {				
				switch (type) {
				case ASSIGNMENT_FILES_DB:
					return prefs.getClassroomDir() + File.separator +  "files.db";
				case CLASS_DB:
					return prefs.getClassroomDir() + File.separator +  "class.db";
				case RUBRIC_DB:
					return prefs.getWorkingDir() + File.separator + "rubric.db";
				case STUDENT_DB:
					return prefs.getClassroomDir() + File.separator  + "student.db";
				default:				
				}
				// TODO Auto-generated method stub
				return null;
			}
		});
		
		ListenerCoordinator.addQueryResponder(GetCompilerQuery.class, new GetCompilerQuery() {
			@Override
			public StudentWorkCompiler fired() {
				// TODO Auto-generated method stub
				return studentWorkCompiler;
			}
			
		}) ;
		
		ListenerCoordinator.addQueryResponder(GetCompilerMessageQuery.class, new GetCompilerMessageQuery() {
			@Override
			public CompilerMessage fired(String studentId) {
				return studentWorkCompiler.getCompilerMessage(studentId);
			}
		});

		ListenerCoordinator.addQueryResponder(GetWorkingDirQuery.class, new GetWorkingDirQuery() {

			@Override
			public String fired() {
				return prefs.getWorkingDir();
			}
			
		});
		
		ListenerCoordinator.addQueryResponder(EnableRunRubricQuery.class, new EnableRunRubricQuery() {
			@Override
			public Boolean fired() {
				return (Boolean)(primaryRubric != null);
			}
			
		});
		
		ListenerCoordinator.addQueryResponder(GetStudentFilesQuery.class, new GetStudentFilesQuery() {
			@Override
			public List<FileData> fired(String studentID) {
				// TODO Auto-generated method stub
				return studentWorkCompiler.getSourceCode(studentID);
			}			
		});
		
		ListenerCoordinator.addQueryResponder(GetCurrentRubricURL.class, new GetCurrentRubricURL() {
			@Override
			public String fired() {
				return getRubricURL();
			}
		});
		
		ListenerCoordinator.addQueryResponder(GetCurrentRubricQuery.class, new GetCurrentRubricQuery() {
			@Override
			public Rubric fired() {
				return currentRubric;
			}			
		});
		
		ListenerCoordinator.addQueryResponder(GetStudentNameQuery.class, new GetStudentNameQuery() {

			@Override
			public String fired(String studentID) {
				// TODO Auto-generated method stub
				return getStudentName(studentID);
			}
		});
		

		
		ListenerCoordinator.addListener(SetWorkingDirListener.class,  new SetWorkingDirListener() {

			@Override
			public void fired(String workingDir) {
				prefs.setWorkingDir(workingDir);				
			}			
		});
		
		ListenerCoordinator.addListener(SetFileDirListener.class, new SetFileDirListener() {
			@Override
			public void fired(String workingDir) {
				prefs.setFileDir(workingDir);				
			}			

		});
			
		ListenerCoordinator.addListener(RubricFileSelectedListener.class, new RubricFileSelectedListener() {
			@Override
			public void fired(String url, String fileName) {
				if (rubricURL == null || !rubricURL.getName().equals(url)) {
					if (checkValidURL(url, prefs.getGradeURL()) == false) {
						return;
					}
					String urlID = googleClassroom.googleSheetID(url);
					rubricURL = new ClassroomData(url, urlID);
					prefs.setRubricInfo(fileName, url);
				}
			}
		});
		
		ListenerCoordinator.addListener(RubricSelected.class, new RubricSelected() {
			@Override
			public void fired(GoogleSheetData data) {
				undoManager.discardAllEdits();
				clearNotesAndComments();
				if (data != null && data.isEmpty() == false) {
					if (primaryRubric == null || primaryRubric.getName().equals(data.getName()) == false) {
						Rubric rubric = new Rubric(data);
						try {
							ListenerCoordinator.fire(AddProgressBarListener.class, "Loading Rubric");
							googleClassroom.fillRubric(rubric);
							setRubric(rubric, RubricType.PRIMARY);
							ListenerCoordinator.fire(RemoveProgressBarListener.class, "Loading Rubric");
						} catch (IOException e) {
							JOptionPane.showMessageDialog(null, e.getMessage(), "Error accessing rubric db sheet",
									JOptionPane.ERROR_MESSAGE);
							DebugLogDialog.appendException(e);					
						}
						
					}				
				}
				else {
					setRubric(null, RubricType.PRIMARY);
				}

				ListenerCoordinator.fire(SetRunRubricEnableStateListener.class, (Boolean)(primaryRubric != null));
				updateListener.dataUpdated();				
			}
		});
		
		ListenerCoordinator.addListener(AddRubricTabsListener.class, new AddRubricTabsListener() {
			@Override
			public void fired(Rubric rubric, boolean structureChanged) {
				if (structureChanged) {
					updateListener.structureChanged();
				}
				else {
					updateListener.dataUpdated();
				}
			}			
		});

		ListenerCoordinator.addListener(SetRubricListener.class, new SetRubricListener() {
			@Override
			public void fired(Rubric rubric, RubricType rubricType) {
				setRubric(rubric, rubricType);
				
			}
		});
		
		ListenerCoordinator.addListener(SaveRubricListener.class, new SaveRubricListener() {
			@Override
			public void fired() {
				saveRubric();
			}			
		});
				
		ListenerCoordinator.addListener(LoadGradesListener.class, new LoadGradesListener() {
			@Override
			public void fired() {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						loadGrades();						
					}
				});

			}			
		});		
		ListenerCoordinator.addListener(GradeFileSelectedListener.class, new GradeFileSelectedListener() {
			@Override
			public void fired(String url, String fileName) {
				if (checkValidURL(prefs.getRubricURL(), url) == false) {
					return;
				}
				String urlID = googleClassroom.googleSheetID(url);
				gradeURL = new ClassroomData(url, urlID);
				prefs.setGradeInfo(fileName, url);
				ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.GRADE_FILE, fileName);
			}
		});


		
		ListenerCoordinator.addListener(RunJPLAGListener.class, new RunJPLAGListener() {
			@Override
			public void fired() {
				runJPLAG();				
			}			
		});
		
		ListenerCoordinator.addListener(AssignmentSelected.class, new AssignmentSelected() {
			@Override
			public void fired(ClassroomData data) {
				dueDate = null;
				if (data != null) {
					dueDate = data.getDate();
				} 

			}
		});
		
		ListenerCoordinator.addListener(OptionsDialogUpdated.class, new OptionsDialogUpdated() {
			@Override
			public void fired(Boolean anonymous) {
				isAnonymous = prefs.getAnonymousNames();
				markLateRed = prefs.getLateDatesInRed();
				lateTime = prefs.getLateDateTime();
				lateType = prefs.getLateType();
				updateListener.dataUpdated();
				
			}
			
		});
				
	}
	
	public String getStudentName(String id) {
		StudentData student = studentMap.get(id).getStudent();
		if (student != null) {
			return "" + student.getFirstName() + " " + student.getName();
		}
		return "";
	}

	private boolean checkValidURL(String rubricURL, String gradeFileURL) {
		if (gradeFileURL == null || rubricURL == null) {
			return true;
		}
		String rubricID = googleClassroom.googleSheetID(rubricURL);
		String gradeID =  googleClassroom.googleSheetID(gradeFileURL);
		if (rubricID.equals(gradeID)) {
			JOptionPane.showMessageDialog(null, "Rubric file and grade file cannot be the same because all grades are saved on a sheet with the rubric name",  "Bad URL",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	
	public String getGradeFileURL() {
		return prefs.getGradeURL();
	}


	public MyPreferences getPrefs() {
		return prefs;
	}


	public StudentWorkCompiler getStudentWorkCompiler() {
		return studentWorkCompiler;
	}



	public Rubric getRubric() {
		return currentRubric;
	}
	
	public void setTestFile(List<FileData> files) {
		testFile = new StudentData("Source", "Reference", FileData.REFERENCE_SOURCE_ID, null);
		for (FileData fileData : files) {
			fileData.setId(FileData.REFERENCE_SOURCE_ID);
			studentWorkCompiler.addFile(fileData);
		}
		studentWorkCompiler.compile(FileData.REFERENCE_SOURCE_ID, getSupportCode());		
	}

	private void setRubric(Rubric rubric, RubricType rubricType) {
		boolean isAlreadyLoaded = (rubric != null && primaryRubric != null && primaryRubric.getName().contentEquals(rubric.getName()));		
		if (rubricType == RubricType.RUBRIC_BEING_EDITED) {
			setRubricBeingEdited(rubric);
		}
		else if (rubricType == RubricType.PRIMARY) {
			setRubricBeingEdited(null);
			primaryRubric = rubric;
			currentRubric = rubric;
		}
		ListenerCoordinator.fire(StudentInfoChangedListener.class);
		
		if (rubric != null) {
			ListenerCoordinator.fire(AddRubricTabsListener.class, rubric, false);
			if ((rubricType != RubricType.RUBRIC_BEING_EDITED) && (isAlreadyLoaded == false) && studentData.size() > 1) {
				loadGrades();
			}
		}
		else {
			updateListener.dataUpdated();
		}
	}

	private void setRubricBeingEdited(Rubric rubricBeingEdited) {
		this.rubricBeingEdited = rubricBeingEdited;
		testFile = null;
		if (rubricBeingEdited != null) {
			currentRubric = rubricBeingEdited;
			//setTestFile(rubricBeingEdited.getReferenceSource());
			//ListenerCoordinator.fire(AddRubricTabsListener.class, rubricBeingEdited);					
		}
		else {
			currentRubric = primaryRubric;
		}
		updateListener.structureChanged();
	}


	public Rubric getRubricBeingEdited() {
		return rubricBeingEdited;
	}


	public ClassroomData getCurrentCourse() {
		return currentCourse;
	}
	
	private void clearAllData() {
		studentData.clear();
		studentMap.clear();
		studentWorkCompiler.clearData();		
	}


	private void setCurrentCourse(ClassroomData currentCourse) {
		prefs.setClassroom(currentCourse);
		this.currentCourse = currentCourse;
		String rubricURLName = prefs.getRubricURL();
		String rubricFileName = prefs.getRubricFile();
		if (rubricURLName != null) {
			ListenerCoordinator.fire(RubricFileSelectedListener.class, rubricURLName, rubricFileName);
		}
		String gradeURL = prefs.getGradeURL();
		String gradeFileName = prefs.getGradeFile();
		if (gradeURL != null) {
			ListenerCoordinator.fire(GradeFileSelectedListener.class, gradeURL, gradeFileName);			
		}
		clearAllData();
		initStudents();
	}


	public void closing() {
		consoleData.setDone();
	}

	public List<FileData> getSourceCode(String id) {
		return studentWorkCompiler.getSourceCode(id);
	}
	
	public boolean areGradesModified() {
		if (currentRubric != null && currentRubric.areGradesModified()) {
			return true;
		}
		return false;
	}
	
	public List<FileData> getSupportCode() {
		if (currentRubric == null) {
			return null;
		}
		return currentRubric.getSupportCodeSource();
	}

	
	public void run(String id) {		
		studentWorkCompiler.compile(id, getSupportCode());
		if (studentWorkCompiler.isRunnable(id)) {
			consoleData.runStarted(id, "");
			StudentData student = studentMap.get(id).getStudent();
			if (student != null) {
				ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "Running: " + student.getFirstName() + " " + student.getName());
			}
			studentWorkCompiler.run(id, getSupportCode());
			ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
		}
	}
	
	public  Set<String>  runRubric(String studentId, Set<String> rubricElementNames) {
		//studentWorkCompiler.compile(studentId);
		StudentData student = null;
		Set<String>  skipped = null;
		if (testFile != null && testFile.getId().equals(studentId)) {
			student = testFile;
		}
		else {
			student = studentMap.get(studentId).getStudent();
		}
		String studentName = "";
		if (student != null) {
			studentName = student.getFirstName() + " " + student.getName();
		}
		if (currentRubric != null) {
			CompilerMessage message = null; //studentWorkCompiler.getCompilerMessage(studentId);
			List<RubricEntry.RubricUndoInfo> undoInfo = new ArrayList<RubricEntry.RubricUndoInfo>();
			skipped = currentRubric.runAutomation(undoInfo, updateListener, rubricElementNames, studentName, studentId, message, studentWorkCompiler, consoleData);
			if (undoInfo.size() != 0) {
				undoManager.addEdit(new RubricUndoableEdit(undoInfo));
			}
		}
		ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
		return skipped;
	}
	

	public void initStudents()  {
		if (currentCourse != null) {
			studentData.clear();
			studentMap.clear();
			ListenerCoordinator.runLongQuery(StudentFetcher.class,  new LongQueryListener<ClassroomData>() {

				@Override
				public void process(List<ClassroomData> list) {
					for (ClassroomData data : list) {
						addStudent((StudentData) data);
						
					}					
				}
				@Override
				public void done() {					
					updateListener.structureChanged();
					ListenerCoordinator.fire(AllStudentNamesLoadedListener.class);

				}
				@Override
				public void remove(Set<String> removeList) {					
					for (String id : removeList) {
						if (studentMap.containsKey(id)) {
							studentMap.remove(id);
							for (StudentDataClass student : studentData) {
								if (student.getStudent(false).getId().equals(id)) {
									studentData.remove(student);								
									break;
								}
							}
						}
					}
				}					
			});
		}
	}
	
	private void addStudent(StudentData student) {
		boolean inserted = false;
		// Student already in the list
		if (studentMap.containsKey(student.getId())) {
			return;
		}
		StudentDataClass studentDataClass = null;
		for (int i = 0; i < studentData.size(); i++) {
			StudentData other = studentData.get(i).getStudent(false);
			if (other.compareTo(student) < 0) {
				studentDataClass = new StudentDataClass(student, studentData.size());
				studentData.add(i, studentDataClass);
				inserted = true;
				break;
			}
		}
		if (inserted == false) {
			studentDataClass = new StudentDataClass(student, studentData.size());
			studentData.add(studentDataClass);
		}
		studentMap.put(student.getId(), studentDataClass);
		updateListener.dataUpdated();
	}
	
	@Override
	public int getRowCount() {
		if (testFile != null) {
			return 1;
		}
		return studentData.size();
		
	}
	
	@Override
	public int getColumnCount() {
		
		int num = NUM_DEFAULT_COLUMNS;
		if (currentRubric != null) {
			num += currentRubric.getEntryCount();
		}
		return num;
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		Object retVal = null;
		switch(columnIndex) {
		case LAST_NAME_COLUMN:
			if (testFile != null) {
				retVal = testFile;
			}
			else {
				retVal = studentData.get(rowIndex).getStudent();
			}
			break;
		case FIRST_NAME_COLUMN:
			if (testFile != null) {
				retVal = "Source";
			}
			else {
				retVal = studentData.get(rowIndex).getStudent().getFirstName();
			}
			break;
		case DATE_COLUMN:
			List<FileData> fileData = studentWorkCompiler.getSourceCode(getStudentId(rowIndex));
			if (fileData != null && fileData.size() > 0) {
				retVal = fileData.get(0).getDate();
			}
			break;
		case COMPILER_COLUMN:

			CompilerMessage data = studentWorkCompiler.getCompilerMessage(getStudentId(rowIndex));
			retVal = data;
			break;
		case TOTAL_COLUMN:
			if (currentRubric != null) {
				return currentRubric.getTotalString(getStudentId(rowIndex));				
			}
			break;			
		default:
			if (currentRubric != null) {
				RubricEntry entry = currentRubric.getEntry(getRubricIndex(columnIndex));
				if (entry != null) {
					retVal = entry.getStudentValue(getStudentId(rowIndex));
				}
			}
			break;
		}
		return retVal;
	}
	
	
	@Override
	public void setValueAt(Object value, int rowIndex, int columnIndex) {
		// If we are in the middle of save/load do not allow modifications
		if (Rubric.getScoreModifiableState() == Rubric.ScoreModifiableState.LOCK_USER_MODIFICATIONS) {
			return;
		}
		int index = getRubricIndex(columnIndex);
		
		if (index >= 0 && currentRubric != null) {
			RubricEntry entry = currentRubric.getEntry(index);
			String studentID = getStudentId(rowIndex);
			StudentScore preChange = entry.getStudentScore(studentID);
			if (preChange != null) {
				preChange = new StudentScore(preChange);
			}
			if (!(value == null && (preChange == null || preChange.getScore() == null))) {
				entry.setStudentValue(studentID, (String)value);
				addUndo(index, studentID, preChange);
			}
		}		
	}
	
	private void addUndo(int index, String studentID, StudentScore preChange) {
		RubricEntry entry = currentRubric.getEntry(index);
		if (entry != null) {
			StudentScore postChange = entry.getStudentScore(studentID);
			boolean bothNull = ((preChange == null || preChange.getScore() == null) && (postChange == null || postChange.getScore() == null));
			if (!bothNull) {
				boolean sameValue = preChange != null && preChange.scoreEquals(postChange); 
				if (!sameValue) {
					undoManager.addEdit(new RubricUndoableEdit(index, studentID, preChange, postChange));
				}
			}
		}
	}
	
	@Override
	public String getColumnTip(int columnIndex) {
		if (columnIndex >= NUM_DEFAULT_COLUMNS && currentRubric != null) {
			int rubricIndex = getRubricIndex(columnIndex);
			if (rubricIndex != -1) {
				RubricEntry entry = currentRubric.getEntry(rubricIndex);
				if (entry != null) {
					return entry.getTipMessage();
				}				
			}
		}
		return "";
	}
	@Override
	public boolean showRed(int row, int col, Object value) {
		if (col == DATE_COLUMN) {				
			lateTime = prefs.getLateDateTime();
			lateType = prefs.getLateType();
			if (value instanceof Date) {
				if (markLateRed) {
					Date submitDate = (Date)value;				
					if (SimpleUtils.calculateDifference(submitDate, dueDate, lateType) > lateTime) {
						return true;
					}
				}
			}
		}
		else if (showRedMap != null) {
			String studentId = getStudentId(row);
			if (showRedMap.containsKey(studentId)) {
				if (currentRubric != null) {
					RubricEntry entry = currentRubric.getEntry(getRubricIndex(col));
					if (entry != null) {
						
						if (showRedMap.get(studentId).contains(entry.getColumnName())) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}
	
	@Override
	public void addEdits(boolean addEdits) {
		this.addEdits = addEdits;
	}
		
	@Override
	public String getColumnName(int columnIndex) {
		RubricEntry entry = getColumnEntry(columnIndex);
		if (entry != null) {
			return entry.getColumnName();
		}
		return null;
	}
	
	@Override
	public String getUserName() {
		return prefs.getUserName();
	}
	
	private int getRubricIndex(int columnIndex) {
		return columnIndex - NUM_DEFAULT_COLUMNS;
	}


	public void setShowRedMap(Map<String, Set<String>> showRedMap) {
		this.showRedMap = showRedMap;		
	}
	
	public RubricEntry getColumnEntry(int columnIndex) {
		if (currentRubric != null) {
			RubricEntry entry = currentRubric.getEntry(getRubricIndex(columnIndex));
			if (entry != null) {
				return entry;
			}
		}
		return null;		
	}



	public String getStudentId(int row) {
		if (testFile != null) {
				return FileData.REFERENCE_SOURCE_ID;
		}
		return studentData.get(row).getStudent(false).getId();		
	}
	
	public List<List<Object>> getColumnValuesForSheet() {
		List<List<Object>> fullData = new ArrayList<List<Object>>();
		
		for (int col = 0; col < getColumnCount(); col++) {
			List<Object> innerData = new ArrayList<Object>();
			if (col < NUM_DEFAULT_COLUMNS) {
				innerData.add(defaultColumnNames[col]);
			}
			else {
				RubricEntry entry = currentRubric.getEntry(getRubricIndex(col));
				String temp = entry.getName() + "\nValue = " + entry.getValue();
				innerData.add(temp);
			}
			for (int row = 0; row < getRowCount(); row++) {
			
				if (col == LAST_NAME_COLUMN) {
					innerData.add(studentData.get(row).getStudent(false).getName());
				}
				else {
					Object value = getValueAt(row, col);
					innerData.add(value);
				}
			}
			fullData.add(innerData);			
		}
		return fullData;
	}


	public String getRubricURL() {
		if (rubricURL != null) {
			return rubricURL.getName();
		}
		return "";
	}
	
	public List<String> getAllIDs() {
		List<String> ids = new ArrayList<String>();
		for (StudentDataClass student : studentData) {
			ids.add(student.getStudent(false).getId());
		}		
		return ids;
	}
	
	private void saveRubric(Rubric rubricToSave) {
		if (rubricToSave != null) {
//			SwingUtilities.invokeLater(new Runnable() {
				//public void run() {

					try {
						ListenerCoordinator.fire(AddProgressBarListener.class, "Saving Rubric");
						googleClassroom.writeSheet(rubricToSave);
						rubricToSave.clearRubricDefinitionModified();
					}
					catch(IOException e) {
						JOptionPane.showMessageDialog(null, e.getMessage(), "Error saving to rubric db sheet",
								JOptionPane.ERROR_MESSAGE);
						DebugLogDialog.appendException(e);

					}
					ListenerCoordinator.fire(RemoveProgressBarListener.class, "Saving Rubric");
				//}
			//});
		}
		
	}
	
	private void saveRubric() {
		saveRubric(currentRubric);
	}
	
	public Rubric newRubric(String name) {
		if (rubricURL != null) {
			GoogleSheetData sheetData = new GoogleSheetData(name, rubricURL.getId(), "NewRubric");
			Rubric rubric = new Rubric(sheetData);
			return rubric;
		}
		return null;
	}
	private List<StudentData> createStudentDataList(boolean anonymous) {
		List<StudentData> studentDataList = new ArrayList<StudentData>();
		for (StudentDataClass student : studentData) {
			studentDataList.add(student.getStudent(anonymous));
		}
		return studentDataList;
	}
	
	public boolean syncGrades() {
		boolean worked = true;
		
		if (currentRubric != null && currentRubric != rubricBeingEdited && gradeURL != null && testFile == null) {
			try {
				Rubric.setScoreModifiableState(Rubric.ScoreModifiableState.LOCK_USER_MODIFICATIONS);
				ClassroomData assignment = (ClassroomData) ListenerCoordinator.runQuery(GetCurrentAssignmentQuery.class);
				@SuppressWarnings("unchecked")
				Map<String, String> modifiedNotes = (Map<String, String>)ListenerCoordinator.runQuery(GetAndClearModifiedNotes.class);
				GoogleSheetData targetFile = new GoogleSheetData(currentRubric.getName(), gradeURL.getId(),  currentRubric.getName());
				GradeSyncer grades = new GradeSyncer(googleClassroom, modifiedNotes, targetFile, currentRubric, createStudentDataList(false), prefs.getUserName());
				ListenerCoordinator.fire(GradesSyncedListener.class, grades.getComments(), false);
				if (currentRubric.areGradesModified() || modifiedNotes.size() > 0 || currentRubric.isLoadedFromFile() == false) {
					for (StudentDataClass student : studentData) {
						String studentID = student.getStudent(false).getId();
						List<FileData> fileData = studentWorkCompiler.getSourceCode(studentID);
						Date date = null;
						if (fileData != null && fileData.size() > 0) {
							date = fileData.get(0).getDate();
						}					
						grades.setDate(studentID, date);
					}
					grades.saveData(assignment);
				}
				
				updateListener.dataUpdated();				
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Error saving grades to google sheet " + e.getMessage(),  "Save problem",
						JOptionPane.ERROR_MESSAGE);
				worked = false;
				DebugLogDialog.appendException(e);
			}			
			Rubric.setScoreModifiableState(Rubric.ScoreModifiableState.TRACK_MODIFICATIONS);
			
		}		
		return worked;
	}
	
	public void loadGrades() {
		if (currentRubric != null && gradeURL != null && currentRubric != rubricBeingEdited) {
			ListenerCoordinator.fire(AddProgressBarListener.class, "Loading Grades");
			Rubric.setScoreModifiableState(Rubric.ScoreModifiableState.LOCK_USER_MODIFICATIONS);
			try {				
				GoogleSheetData targetFile = new GoogleSheetData(currentRubric.getName(), gradeURL.getId(),  currentRubric.getName());
				GradeSyncer grades = new GradeSyncer(googleClassroom, null, targetFile, currentRubric, createStudentDataList(false), prefs.getUserName());
				ListenerCoordinator.fire(GradesSyncedListener.class, grades.getComments(), true);
				updateListener.structureChanged();
			} catch (Exception e) {
				JOptionPane.showMessageDialog(null, "Error loading grades from google sheet " + e.getMessage(),  "Save problem",
						JOptionPane.ERROR_MESSAGE);
				DebugLogDialog.appendException(e);
			}
			Rubric.setScoreModifiableState(Rubric.ScoreModifiableState.TRACK_MODIFICATIONS);

			ListenerCoordinator.fire(RemoveProgressBarListener.class, "Loading Grades");
			
		}
		
	}
	
	public void runJPLAG() {
		ListenerCoordinator.fire(AddProgressBarListener.class, "Running JPLAG");
		Map<String, List<FileData>> fileMap = new HashMap<String, List<FileData>>();
		for (StudentDataClass student : studentData) {
			String id = student.getStudent().getId();
			List<FileData> files = studentWorkCompiler.getSourceCode(id);
			if (files != null) {
				fileMap.put(id, files);
			}
		}		
		String outputFile = JPLAGInvoker.invokeJPLAG(fileMap, createStudentDataList(isAnonymous), prefs.getClassroomDir());
		if (outputFile == null) {
			return;
		}
		JTextArea message = new JTextArea(3,100);
		message.setText("Attempting to open browser with results.\nIf it does not open, manually copy this path into your browser:\n" + outputFile);
		message.setWrapStyleWord(true);
		message.setLineWrap(true);
		message.setCaretPosition(0);
		message.setEditable(false);
		JLabel background = new JLabel();
		message.setBackground(background.getBackground());
		JPopupMenu popupSource = new JPopupMenu();
		Action copy = new DefaultEditorKit.CopyAction();
		copy.putValue(Action.NAME, "Copy");
		copy.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
		popupSource.add(copy);
		message.setComponentPopupMenu(popupSource);
		JOptionPane.showMessageDialog(null, new JScrollPane(message),  "Open Results",
				JOptionPane.INFORMATION_MESSAGE);
		ListenerCoordinator.fire(RemoveProgressBarListener.class, "Running JPLAG");
				
	}

	public void recompile(String studentID) {
		studentWorkCompiler.compile(studentID, getSupportCode());		
	}
	
	public void removeSource(String studentID, String fileName) {
		studentWorkCompiler.removeSource(studentID, fileName);
	}

	public void publishGrades(List<String> ids) {
		if (currentRubric == null) {
			JOptionPane.showMessageDialog(null, "Cannot publish grades until you have seleted a rubric and rubric entries have been assigned a value",  "Rubric not selected",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		ClassroomData assignment = (ClassroomData)ListenerCoordinator.runQuery(GetCurrentAssignmentQuery.class);
		if (assignment == null || assignment.isEmpty()) {
			JOptionPane.showMessageDialog(null, "Cannot publish grades until you have seleted an assignment",  "Assignment not selected",
					JOptionPane.ERROR_MESSAGE);
			return;
			
		}
		ListenerCoordinator.fire(AddProgressBarListener.class, "Publishing Grades");
		boolean publishReady = true;
		Map<String, Double> scores = new HashMap<String, Double>();
		for (String id : ids) {
			if (currentRubric.isGradingComplete(id) == true) {
				scores.put(id, currentRubric.getTotalValue(id));				
			}			
		}
		if (publishReady == true) {
			try {
				googleClassroom.publishStudentGrades(currentCourse, assignment, scores);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Only the person who created this assignment can modify it\n" + e.getMessage(),  "Error publishing grades",
						JOptionPane.ERROR_MESSAGE);
				DebugLogDialog.appendException(e);
			}
		}
		ListenerCoordinator.fire(RemoveProgressBarListener.class, "Publishing Grades");
	}
	public void removeInstrumentation(String studentID, String fileName) {
		if (studentID == null) {
			studentWorkCompiler.removeAllInstrumentation();
		}
		else {
			studentWorkCompiler.removeInstrumentation(studentID, fileName);
		}
	}


	
	private class RubricUndoableEdit extends AbstractUndoableEdit {
		
		private static final long serialVersionUID = 1L;

		List<RubricEntry.RubricUndoInfo> undoInfo;
		public RubricUndoableEdit(int rubricEntry, String studentID, StudentScore preChange, StudentScore postChange) {
			undoInfo = new ArrayList<RubricEntry.RubricUndoInfo>();
			undoInfo.add(new RubricEntry.RubricUndoInfo(rubricEntry, studentID, preChange, postChange));
		}
		public RubricUndoableEdit(List<RubricEntry.RubricUndoInfo> undoInfo) {
			this.undoInfo = undoInfo;
		}

		@Override
		public void undo() throws CannotUndoException {
			for (RubricEntry.RubricUndoInfo entryInfo : undoInfo) {
				currentRubric.getEntry(entryInfo.getRubricEntryIndex()).setStudentScore(entryInfo.getStudentID(), entryInfo.getStudentScorePreChange());	
			}
			updateListener.dataUpdated();
		}

		@Override
		public boolean canUndo() {
			return true;
		}

		@Override
		public void redo() throws CannotRedoException {
			for (RubricEntry.RubricUndoInfo entryInfo : undoInfo) {
				currentRubric.getEntry(entryInfo.getRubricEntryIndex()).setStudentScore(entryInfo.getStudentID(), entryInfo.getStudentScorePostChange());	
			}
			updateListener.dataUpdated();
		}

		@Override
		public boolean canRedo() {
			return true;
		}

		@Override
		public boolean addEdit(UndoableEdit anEdit) {
			if (addEdits && anEdit instanceof RubricUndoableEdit) {
				RubricUndoableEdit other = (RubricUndoableEdit)anEdit;
				for (RubricEntry.RubricUndoInfo entryInfo : other.undoInfo) {
					undoInfo.add(entryInfo);	
				}
				return true;
			}
			//addEdits = true;
			return false;
		}

		@Override
		public boolean replaceEdit(UndoableEdit anEdit) {
			return false;
		}

		@Override
		public boolean isSignificant() {
			return true;
		}
		
	}
}


