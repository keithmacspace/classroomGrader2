package net.cdonald.googleClassroom.control;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Action;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.text.DefaultEditorKit;

import net.cdonald.googleClassroom.googleClassroomInterface.AssignmentFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.CourseFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.FileFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.GoogleClassroomCommunicator;
import net.cdonald.googleClassroom.googleClassroomInterface.LoadSheetGrades;
import net.cdonald.googleClassroom.googleClassroomInterface.SaveSheetGrades;
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
import net.cdonald.googleClassroom.listenerCoordinator.AssignmentSelected;
import net.cdonald.googleClassroom.listenerCoordinator.ClassSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.EnableRunRubricQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCompilerMessageQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentAssignmentQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentClassQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentRubricURL;
import net.cdonald.googleClassroom.listenerCoordinator.GetDBNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetFileDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetWorkingDirQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GradeFileSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LoadGradesListener;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryListener;
import net.cdonald.googleClassroom.listenerCoordinator.PublishGradesListener;
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
import net.cdonald.googleClassroom.model.StudentData;



public class DataController implements StudentListInfo {
	private StudentWorkCompiler studentWorkCompiler;
	private ConsoleData consoleData;
	private ClassroomData currentCourse;
	private List<StudentData> studentData;
	private Map<String, StudentData> studentMap;
	private Rubric currentRubric;
	private Rubric primaryRubric;

	private DataUpdateListener updateListener;
	private GoogleClassroomCommunicator googleClassroom;
	private MyPreferences prefs;
	private ClassroomData rubricURL;
	private ClassroomData gradeURL;
	private Map<String, Map<String, String> > notesCommentsMap;
	private Rubric rubricBeingEdited;
	private StudentData rubricBeingEditedStudent;
	private boolean gradesModified;

	
	

	public DataController(MainGoogleClassroomFrame mainFrame) {
		prefs = new MyPreferences();
		studentWorkCompiler = new StudentWorkCompiler(mainFrame);
		studentData = new ArrayList<StudentData>();
		studentMap = new HashMap<String, StudentData>();
		consoleData = new ConsoleData();		
		currentCourse = null;
		updateListener = mainFrame;
		notesCommentsMap = new HashMap<String, Map<String, String>>();
		initGoogle();		
		notesCommentsMap.put(prefs.getUserName(), new HashMap<String, String>());
		registerListeners();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void testCredentials() throws IOException {
		googleClassroom.getCredentials();
	}
	
	public void performFirstInit() {

		if (prefs.getClassroom() != null) {
			currentCourse = prefs.getClassroom();
			ListenerCoordinator.fire(ClassSelectedListener.class, currentCourse);
		}
		else {
			
		}
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

				studentWorkCompiler.clearData();
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
						studentWorkCompiler.compileAll();
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
							e.printStackTrace();					
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
				loadGrades();
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

	/**
	 * @return the gradesModified
	 */
	public boolean isGradesModified() {
		return gradesModified;
	}



	public StudentWorkCompiler getStudentWorkCompiler() {
		return studentWorkCompiler;
	}



	public Rubric getRubric() {
		return currentRubric;
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
			ListenerCoordinator.fire(AddRubricTabsListener.class, rubric);


			if ((rubricType != RubricType.RUBRIC_BEING_EDITED) && (isAlreadyLoaded == false) && studentData.size() > 1) {
				loadGrades();
			}

			if (rubric.isInModifiedState() && rubricType == RubricType.PRIMARY) {
				saveRubric(rubric);
			}
		}
	}

	private void setRubricBeingEdited(Rubric rubricBeingEdited) {
		this.rubricBeingEdited = rubricBeingEdited;
		rubricBeingEditedStudent = null;
		if (rubricBeingEdited != null) {
			currentRubric = rubricBeingEdited;
			rubricBeingEditedStudent = new StudentData("Source", "Golden", FileData.GOLDEN_SOURCE_ID, null);
			studentWorkCompiler.clearStudentFiles(FileData.GOLDEN_SOURCE_ID);			
			for (FileData fileData : rubricBeingEdited.getGoldenSource()) {
				fileData.setId(FileData.GOLDEN_SOURCE_ID);
				studentWorkCompiler.addFile(fileData);
			}
			studentWorkCompiler.compile(FileData.GOLDEN_SOURCE_ID);
			ListenerCoordinator.fire(AddRubricTabsListener.class, rubricBeingEdited);					
		}
		else {
			currentRubric = primaryRubric;
		}
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
		clearAllData();
		initStudents();
	}


	public void closing() {
		consoleData.setDone();
	}

	public List<FileData> getSourceCode(String id) {
		return studentWorkCompiler.getSourceCode(id);
	}
	
	
	
	public void run(String id) {

		if (studentWorkCompiler.isRunnable(id)) {
			consoleData.runStarted(id, "");
			StudentData student = studentMap.get(id);
			if (student != null) {
				ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "Running: " + student.getFirstName() + " " + student.getName());
			}
			studentWorkCompiler.run(id);
			ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
		}
	}
	
	public void runRubric(String studentId, Set<String> rubricElementNames) {
		StudentData student = null;
		if (rubricBeingEditedStudent != null && rubricBeingEditedStudent.getId().equals(studentId)) {
			student = rubricBeingEditedStudent;
		}
		else {
			student = studentMap.get(studentId);
		}
		String studentName = "";
		if (student != null) {
			studentName = student.getFirstName() + " " + student.getName();
		}
		if (currentRubric != null) {
			CompilerMessage message = studentWorkCompiler.getCompilerMessage(studentId);			
			currentRubric.runAutomation(updateListener, rubricElementNames, studentName, studentId, message, studentWorkCompiler, consoleData);
			updateListener.dataUpdated();
			
		}
		ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
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
				public void remove(Set<String> removeList) {
					DebugLogDialog.appendln("Remove" + removeList);
					for (String id : removeList) {
						if (studentMap.containsKey(id)) {
							studentMap.remove(id);
							for (StudentData student : studentData) {
								if (student.getId().equals(id)) {
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

		for (int i = 0; i < studentData.size(); i++) {
			StudentData other = studentData.get(i);
			if (other.compareTo(student) < 0) {
				studentData.add(i, student);
				inserted = true;
				break;
			}
		}
		if (inserted == false) {
			studentData.add(student);
		}
		studentMap.put(student.getId(), student);
		updateListener.dataUpdated();
	}
	
	@Override
	public int getRowCount() {
		if (rubricBeingEdited != null) {
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
			if (rubricBeingEditedStudent != null) {
				retVal = rubricBeingEditedStudent;
			}
			else {
				retVal = studentData.get(rowIndex);
			}
			break;
		case FIRST_NAME_COLUMN:
			if (rubricBeingEdited != null) {
				retVal = "Source";
			}
			else {
				retVal = studentData.get(rowIndex).getFirstName();
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
		gradesModified = true;
		int index = getRubricIndex(columnIndex);		
		if (index >= 0 && currentRubric != null) {
			currentRubric.getEntry(index).setStudentValue(getStudentId(rowIndex), (String)value);
		}
		
	}
	
	@Override
	public String getColumnTip(int columnIndex) {
		if (columnIndex >= NUM_DEFAULT_COLUMNS && currentRubric != null) {
			int rubricIndex = getRubricIndex(columnIndex);
			if (rubricIndex != -1) {
				String tip = "";
				RubricEntry entry = currentRubric.getEntry(rubricIndex);
				if (entry.getValue() > 0) {
					tip += "Max Val = " + entry.getValue() + ": ";
				}
				tip += entry.getDescription();
				return tip;
			}
		}
		return "";
	}
	
	private int getRubricIndex(int columnIndex) {
		return columnIndex - NUM_DEFAULT_COLUMNS;
	}
	
	@Override
	public String getColumnName(int columnIndex) {		
		if (columnIndex < NUM_DEFAULT_COLUMNS) {
			return defaultColumnNames[columnIndex];
		}
		if (currentRubric != null) {
			return currentRubric.getEntry(getRubricIndex(columnIndex)).getName();
		}
		return null;
	}
	
	@Override
	public Map<String, Map<String, String> >getNotesCommentsMap() {
		return notesCommentsMap;
	}
	
	@Override
	public String getUserName() {
		return prefs.getUserName();
	}

	public String getStudentId(int row) {
		if (rubricBeingEdited != null) {
				return FileData.GOLDEN_SOURCE_ID;
		}
		return studentData.get(row).getId();		
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
					innerData.add(studentData.get(row).getName());
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
		if (rubricBeingEdited != null) {
			ids.add(FileData.GOLDEN_SOURCE_ID);
		}
		else {
			for (StudentData student : studentData) {
				ids.add(student.getId());
			}
		}
		return ids;
	}
	
	private void saveRubric(Rubric rubricToSave) {
		if (rubricToSave != null) {
			try {
				ListenerCoordinator.fire(AddProgressBarListener.class, "Saving Rubric");
				googleClassroom.writeSheet(rubricToSave);				
			}
			catch(IOException e) {
				JOptionPane.showMessageDialog(null, e.getMessage(), "Error saving to rubric db sheet",
						JOptionPane.ERROR_MESSAGE);
				
			}
			ListenerCoordinator.fire(RemoveProgressBarListener.class, "Saving Rubric");
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
	
	public SaveSheetGrades newSaveGrades(String assignmentName) {
		if (currentRubric != null && currentRubric != rubricBeingEdited) {
			ClassroomData assignment = (ClassroomData) ListenerCoordinator.runQuery(GetCurrentAssignmentQuery.class);
			SaveSheetGrades grades = new SaveSheetGrades(googleClassroom, new GoogleSheetData(currentRubric.getName(), gradeURL.getId(),  currentRubric.getName()), assignment, currentRubric, studentData, prefs.getUserName(), notesCommentsMap);
			return grades;
		}
		return null;
	}
	
	public void loadGrades() {
		if (currentRubric != null && gradeURL != null && currentRubric != rubricBeingEdited) {
			LoadSheetGrades grades = new LoadSheetGrades(new GoogleSheetData(currentRubric.getName(), gradeURL.getId(), currentRubric.getName()), currentRubric, studentData, prefs.getUserName(), notesCommentsMap);

			try {
				ListenerCoordinator.fire(AddProgressBarListener.class, "Loading Grades");
				grades.loadData(googleClassroom, false);
				updateListener.dataUpdated();
			} catch (IOException e) {

			}
			ListenerCoordinator.fire(RemoveProgressBarListener.class, "Loading Grades");
		}
		
	}
	
	public void saveGrades(SaveSheetGrades grades) {
		try {
			gradesModified = false;
			googleClassroom.writeSheet(grades);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "Error saving grades to google sheet " + e.getMessage(),  "Save problem",
					JOptionPane.ERROR_MESSAGE);
		}
	}
	
	public void runJPLAG() {
		ListenerCoordinator.fire(AddProgressBarListener.class, "Running JPLAG");
		Map<String, List<FileData>> fileMap = new HashMap<String, List<FileData>>();
		for (StudentData student : studentData) {
			String id = student.getId();
			List<FileData> files = studentWorkCompiler.getSourceCode(id);
			if (files != null) {
				fileMap.put(id, files);
			}
		}		
		String outputFile = JPLAGInvoker.invokeJPLAG(fileMap, studentData, prefs.getClassroomDir());
		JTextArea message = new JTextArea(3,100);
		message.setText("Attempting to open browser with results.\nIf it does not open, manually copy this path into your browser:\n" + outputFile);
		message.setWrapStyleWord(true);
		message.setLineWrap(true);
		message.setCaretPosition(0);
		message.setEditable(false);
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

	public void recompile(String studentID, String fileName, String fileText) {
		studentWorkCompiler.recompile(studentID, fileName, fileText);		
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
			if (currentRubric.isGradingComplete(id) == false) {
				publishReady = false;			
				JOptionPane.showMessageDialog(null, "Cannot publish grades until all rubric entries have been assigned a value",  "Grades incomplete",
					JOptionPane.ERROR_MESSAGE);
				break;
			}
			else {
				scores.put(id, currentRubric.getTotalValue(id));				
			}			
		}
		if (publishReady == true) {
			try {
				googleClassroom.publishStudentGrades(currentCourse, assignment, scores);
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "Google classroom communication error: " + e.getMessage(),  "Error publishing grades",
						JOptionPane.ERROR_MESSAGE);
			}
		}
		ListenerCoordinator.fire(RemoveProgressBarListener.class, "Publishing Grades");
	}}
