package net.cdonald.googleClassroom.googleClassroomInterface;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.classroom.Classroom;
import com.google.api.services.classroom.Classroom.Builder;
import com.google.api.services.classroom.Classroom.Courses;
import com.google.api.services.classroom.Classroom.Courses.CourseWork.StudentSubmissions;
import com.google.api.services.classroom.Classroom.Courses.CourseWork.StudentSubmissions.Patch;
import com.google.api.services.classroom.ClassroomScopes;
import com.google.api.services.classroom.model.AssignmentSubmission;
import com.google.api.services.classroom.model.Attachment;
import com.google.api.services.classroom.model.Course;
import com.google.api.services.classroom.model.CourseWork;
import com.google.api.services.classroom.model.Date;
import com.google.api.services.classroom.model.DriveFile;
import com.google.api.services.classroom.model.ListCourseWorkResponse;
import com.google.api.services.classroom.model.ListCoursesResponse;
import com.google.api.services.classroom.model.ListStudentSubmissionsResponse;
import com.google.api.services.classroom.model.ListStudentsResponse;
import com.google.api.services.classroom.model.Name;
import com.google.api.services.classroom.model.Student;
import com.google.api.services.classroom.model.StudentSubmission;
import com.google.api.services.classroom.model.TimeOfDay;
import com.google.api.services.classroom.model.UserProfile;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values;
import com.google.api.services.sheets.v4.Sheets.Spreadsheets.Values.BatchGet;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.AddSheetRequest;
import com.google.api.services.sheets.v4.model.AppendDimensionRequest;
import com.google.api.services.sheets.v4.model.AutoResizeDimensionsRequest;
import com.google.api.services.sheets.v4.model.BatchGetValuesResponse;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateValuesResponse;
import com.google.api.services.sheets.v4.model.Border;
import com.google.api.services.sheets.v4.model.CellData;
import com.google.api.services.sheets.v4.model.CellFormat;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.InsertDimensionRequest;
import com.google.api.services.sheets.v4.model.MoveDimensionRequest;
import com.google.api.services.sheets.v4.model.RepeatCellRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.TextFormat;
import com.google.api.services.sheets.v4.model.UpdateBordersRequest;
import com.google.api.services.sheets.v4.model.UpdateCellsRequest;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.common.collect.ImmutableList;

import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.StudentData;
import net.cdonald.googleClassroom.utils.SimpleUtils;

public class GoogleClassroomCommunicator {
	private final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/**
	 * Global instance of the scopes required by this quickstart. If modifying these
	 * scopes, delete your previously saved tokens/ folder.
	 */
	private static final List<String> SCOPES = new ImmutableList.Builder<String>()
			.add(ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS)
			.add(ClassroomScopes.CLASSROOM_COURSES_READONLY).add(ClassroomScopes.CLASSROOM_ROSTERS_READONLY)
			.add(ClassroomScopes.CLASSROOM_COURSEWORK_STUDENTS_READONLY).add(DriveScopes.DRIVE)
			.add(DriveScopes.DRIVE_FILE).add(DriveScopes.DRIVE_APPDATA).add(DriveScopes.DRIVE_METADATA)
			.add(SheetsScopes.SPREADSHEETS, SheetsScopes.DRIVE, SheetsScopes.DRIVE_FILE).build();

	NetHttpTransport httpTransport;
	private String applicationName;
	private String tokensDirectoryPath;
	private String credentialsFilePath;
	private Classroom classroomService;
	private Drive driveService;
	private Sheets sheetsService;
	private static Semaphore readAssignmentsSemaphore = new Semaphore(1);
	private static Semaphore getCredentialsSemaphore = new Semaphore(1);
	private static Semaphore readStudentsSemaphore = new Semaphore(1);
	private static Semaphore readStudentsWorkSemaphore = new Semaphore(1);
	private static boolean cancelCurrentAssignmentRead = false;
	private static boolean cancelCurrentStudentRead = false;
	private static boolean cancelCurrentStudentWorkRead = false;
	
	public static String getColumnName(int column) {
		int temp = 0;
		String name = "";
		final String[] columnNames = { "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O",
				"P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z" };
		
		while (column > 25)
		{
			temp = (char)((column) % 26);
			name = columnNames[temp] + name;
			column /= 26;
			column--;
		}
		name = columnNames[column] + name;
		return name;
	}	



	public GoogleClassroomCommunicator(String applicationName, String tokensDirectoryPath, String credentialsFilePath)
			throws IOException, GeneralSecurityException {
		this.applicationName = applicationName;
		this.tokensDirectoryPath = tokensDirectoryPath.replace('\\', '/');
		this.credentialsFilePath = credentialsFilePath;// .replace('\\', '/');
		httpTransport = GoogleNetHttpTransport.newTrustedTransport();
	}
	


	public void initServices() throws IOException {
		DebugLogDialog.startMethod();
		try {
			DebugLogDialog.aquireSemaphore(getCredentialsSemaphore, 1);
		} catch (InterruptedException e) {
			getCredentialsSemaphore.release();
			DebugLogDialog.appendException(e);
		}
		// Build a new authorized API client service.


		if (classroomService == null || driveService == null || sheetsService == null) {
			Credential credentials = getCredentials();
			DebugLogDialog.appendCheckPoint("Pre drive", 1);
			if (driveService == null) {
				
				driveService = new Drive.Builder(httpTransport, JSON_FACTORY, credentials)
						.setApplicationName(applicationName).build();
			}

			DebugLogDialog.appendCheckPoint("Pre sheet", 1);
			if (sheetsService == null) {
				sheetsService = new Sheets.Builder(httpTransport, JSON_FACTORY, credentials)
						.setApplicationName(applicationName).build();
			}
			
			DebugLogDialog.appendCheckPoint(httpTransport.toString() + " " + JSON_FACTORY.toString() + " " + this.applicationName + " " + this.credentialsFilePath + " " + this.tokensDirectoryPath, 1);
			if (classroomService == null) {
				DebugLogDialog.appendCheckPoint("Pre builder", 1);
				Builder classRoom = new Classroom.Builder(httpTransport, JSON_FACTORY, credentials);
				DebugLogDialog.appendCheckPoint("Post builder", 1);
				classRoom.setApplicationName(applicationName);
				DebugLogDialog.appendCheckPoint("post application Name", 1);
				classroomService = classRoom.build();
				DebugLogDialog.appendCheckPoint("post build", 1);
			}


		}
		getCredentialsSemaphore.release();
		DebugLogDialog.endMethod();
	}

	public Credential getCredentials() throws IOException {
		DebugLogDialog.startMethod();
		InputStream in = new FileInputStream(credentialsFilePath);
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY,
				clientSecrets, SCOPES)
						.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectoryPath)))
						.setAccessType("offline").build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		Credential cred = new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
		DebugLogDialog.appendCheckPoint("" + cred.toString(), 2);
		DebugLogDialog.endMethod();
		return cred;
	}

	private boolean acquireReadAssignmentSemaphore() {
		boolean worked = true;
		try {
			DebugLogDialog.aquireSemaphore(readAssignmentsSemaphore, 1);
		} catch (InterruptedException e) {
			worked = false;
			readAssignmentsSemaphore.release();
			DebugLogDialog.appendException(e);
		}
		cancelCurrentAssignmentRead = false;
		return worked;
	}

	private boolean acquireReadStudentsSemaphore() {
		boolean worked = true;
		try {
			DebugLogDialog.aquireSemaphore(readStudentsSemaphore, 1);
		} catch (InterruptedException e) {
			readStudentsSemaphore.release();
			DebugLogDialog.appendException(e);
			worked = false;
		}
		cancelCurrentStudentRead = false;		
		return worked;
	}

	private boolean acquireReadStudentsWorkSemaphore() {
		boolean worked = true;
		try {
			DebugLogDialog.aquireSemaphore(readStudentsWorkSemaphore, 1);
		} catch (InterruptedException e) {
			DebugLogDialog.appendException(e);
			worked = false;
			readStudentsWorkSemaphore.release();
		}
		worked = true;
		cancelCurrentStudentWorkRead = false;
		return worked;
	}

	public void getClasses(DataFetchListener fetchListener) throws IOException {
		DebugLogDialog.startMethod();
		initServices();
		try {
			ListCoursesResponse response = classroomService.courses().list().execute();
			List<Course> courses = response.getCourses();
			for (Course course : courses) {				
				ClassroomData data = new ClassroomData(course.getName(), course.getId(), course.getCreationTime());
				data.setRetrievedFromGoogle(true);
				fetchListener.retrievedInfo(data);
			}
		} catch (IOException e) {
			DebugLogDialog.appendException(e);
			throw e;
		}
		DebugLogDialog.endMethod();
	}

	public void getStudents(ClassroomData course, DataFetchListener fetchListener) throws IOException {
		if (course.isEmpty()) {
			return;
		}
		try {
			DebugLogDialog.startMethod();
			cancelCurrentStudentWorkRead = true;
			acquireReadStudentsSemaphore();
			initServices();
			Set<String> studentNames = new HashSet<String>();
			ListStudentsResponse studentListResponse = classroomService.courses().students().list(course.getId())
					.execute();
			for (Student student : studentListResponse.getStudents()) {
				UserProfile studentProfile = student.getProfile();
				if (cancelCurrentStudentRead) {
					break;
				}
				

				Name name = studentProfile.getName();
				String firstName = name.getGivenName();
				String lastName = name.getFamilyName();
				String searchName = lastName + firstName;
				int matchCount = 1;
				while (studentNames.contains(searchName)) {
					firstName =  name.getGivenName() + "(" + matchCount + ")";
					searchName = lastName + firstName;
					matchCount++;
				}
				studentNames.add(searchName);
				StudentData data = new StudentData(firstName, lastName, studentProfile.getId(),
						course.getDate());

				data.setRetrievedFromGoogle(true);
				fetchListener.retrievedInfo(data);
			}
		} catch (IOException e) {
			readStudentsSemaphore.release();
			DebugLogDialog.appendException(e);
			throw e;
		}		
		readStudentsSemaphore.release();
		DebugLogDialog.endMethod();
	}

	public void getAssignments(ClassroomData course, DataFetchListener fetchListener) throws IOException {
		if (course.isEmpty()) {
			return;
		}
		DebugLogDialog.startMethod();
		cancelCurrentAssignmentRead = true;
		acquireReadAssignmentSemaphore();
		acquireReadStudentsWorkSemaphore();

		try {
			initServices();

			ListCourseWorkResponse courseListResponse = classroomService.courses().courseWork().list(course.getId())
					.execute();

			for (CourseWork courseWork : courseListResponse.getCourseWork()) {
				if (cancelCurrentAssignmentRead) {
					break;
				}

				Date date = courseWork.getDueDate();
				java.util.Date convertedDueDate = null;
				

				TimeOfDay dueTime = courseWork.getDueTime();
				
				if (date != null && dueTime != null) {					
					Integer hours = dueTime.getHours();
					Integer minutes = dueTime.getMinutes();
					
					Integer month = date.getMonth();
					Integer year = date.getYear();
					Integer day = date.getDay();
					
					
					Calendar temp = new GregorianCalendar(year, month - 1, day,
							(hours == null) ? 0 : hours, (minutes == null) ? 0 : minutes);
					long timeInMS = temp.getTimeInMillis();
					long offset = TimeZone.getDefault().getOffset(timeInMS);
					timeInMS += offset;
					temp.setTimeInMillis(timeInMS);
					convertedDueDate = temp.getTime();
				}
				else {
					convertedDueDate = SimpleUtils.createDate(courseWork.getCreationTime());
					if (convertedDueDate == null) {
						convertedDueDate = new java.util.Date();
					}
					if (convertedDueDate != null) {
						// If there is no due date, just add 4 days.
						convertedDueDate.setTime(convertedDueDate.getTime() + (4 * 24 * 3600 * 1000));
					}
				}
				
					ClassroomData data = new ClassroomData(courseWork.getTitle(), courseWork.getId(), convertedDueDate);
					data.setRetrievedFromGoogle(true);
					fetchListener.retrievedInfo(data);
				
			}
		} catch (IOException e) {
			readStudentsWorkSemaphore.release();
			readAssignmentsSemaphore.release();
			DebugLogDialog.appendException(e);
			throw e;
		}

		readStudentsWorkSemaphore.release();
		readAssignmentsSemaphore.release();
		DebugLogDialog.endMethod();
	}
	
	public List<StudentSubmission> getStudentSubmissions(ClassroomData course, ClassroomData assignment) throws IOException {
		initServices();
		ListStudentSubmissionsResponse studentSubmissionResponse = classroomService.courses().courseWork()
				.studentSubmissions().list(course.getId(), assignment.getId()).execute();
		return studentSubmissionResponse.getStudentSubmissions();
	}

	public void getStudentWork(ClassroomData course, ClassroomData assignment, DataFetchListener fetchListener)
			throws IOException {
		if (course.isEmpty() || assignment.isEmpty()) {
			return;
		}
		DebugLogDialog.startMethod();
		try {

			acquireReadStudentsWorkSemaphore();
			List<StudentSubmission> submissions = getStudentSubmissions(course, assignment);

			for (StudentSubmission submission : submissions) {
				if (cancelCurrentStudentWorkRead) {
					break;
				}
				AssignmentSubmission assignmentSubmission = submission.getAssignmentSubmission();
				
				if (assignmentSubmission != null && assignmentSubmission.getAttachments() != null) {
					String studentNameKey = submission.getUserId();
					String studentName = (String)ListenerCoordinator.runQuery(GetStudentNameQuery.class, studentNameKey);

					for (Attachment attachment : assignmentSubmission.getAttachments()) {
						if (cancelCurrentStudentWorkRead) {
							break;
						}
						DriveFile driveFile = attachment.getDriveFile();						
						String fileContents = "";
						if (/*title.contains(".java") == */ true) {

							ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
							File file = driveService.files().get(driveFile.getId()).execute();
							String type = file.getMimeType();

							String fileName = driveFile.getTitle();

							try {
								if (type.contains("text")) {
									driveService.files().get(driveFile.getId()).executeMediaAndDownloadTo(outputStream);
									fileContents = outputStream.toString("US-ASCII");
								}
								else if (type.contains("google-apps.document")) {
									driveService.files().export(driveFile.getId(), "text/plain").executeMediaAndDownloadTo(outputStream);

									fileContents = decodeGoogleDoc(outputStream.toByteArray());
									if (fileContents == null) {
										fileContents = outputStream.toString("US-ASCII");
									}
									fileContents = "// Student uploaded this as a google document, delete any weird characters and\n" +
											"//rename the class to have the same name as the file (without the .java).\n" +
											fileContents;
									//outputStream.toString("US-ASCII");								

								} 
								else {
									fileContents = "Student uploaded file in unsupported format, nothing downloaded";								
								}
							}
							catch(IOException e1)
							{
								fileContents = "Error loading file, check the submission for corruption/an empty file.";
							}
							fileName = FileData.createFileName(fileName, fileContents);
							fileContents = FileData.stripPackage(fileContents);
							ClassroomData data = new FileData(fileName, fileContents, studentNameKey,
									submission.getUpdateTime());
							data.setRetrievedFromGoogle(true);
							fetchListener.retrievedInfo(data);
							outputStream.close();
						}
					}
				}
			}
		} catch (IOException e) {
			readStudentsWorkSemaphore.release();
			DebugLogDialog.appendException(e);
			throw e;
		}
		readStudentsWorkSemaphore.release();
		DebugLogDialog.endMethod();
	}
	private static String decodeGoogleDoc(byte[] utf8) { 
				  
		CharsetDecoder decoder = Charset.forName("US-ASCII").newDecoder();

		decoder.onMalformedInput(
				java.nio.charset.CodingErrorAction.IGNORE);
		decoder.onUnmappableCharacter(CodingErrorAction.IGNORE);

		String str = null;
		try {			
			str = decoder.decode(ByteBuffer.wrap(utf8)).toString();
		} catch (CharacterCodingException e) {
			DebugLogDialog.appendException(e);
		}
		// set decoder back to its default value: REPORT

		decoder.onMalformedInput(CodingErrorAction.REPORT);
		decoder.onUnmappableCharacter(CodingErrorAction.REPORT);

		return str;
	}

	public void publishStudentGrades(ClassroomData course, ClassroomData assignment, Map<String, Double> grades) throws IOException {
		if (course.isEmpty() || assignment.isEmpty()) {
			return;
		}
		DebugLogDialog.startMethod();
		try {
			acquireReadStudentsWorkSemaphore();
			initServices();
			Courses courses = classroomService.courses();
			Courses.CourseWork courseWork = courses.courseWork();
			StudentSubmissions submissions = courseWork.studentSubmissions();
			ListStudentSubmissionsResponse studentSubmissionResponse = submissions.list(course.getId(), assignment.getId()).execute();
			for (StudentSubmission submission : studentSubmissionResponse.getStudentSubmissions()) {
				String studentNameKey = submission.getUserId();
				Double value = grades.get(studentNameKey);
				
				if (value != null) {
					Double assignedGrade = submission.getAssignedGrade();
					if (assignedGrade == null || !value.equals(assignedGrade)) {
						submission.setDraftGrade(value);
						String courseId = submission.getCourseId();
						String courseWorkId = submission.getCourseWorkId();
						String submissionId = submission.getId();
						Patch patchGrade = submissions.patch(courseId, courseWorkId, submissionId, submission);
						patchGrade.setUpdateMask("draftGrade");
						patchGrade.execute();
					}
				}
			}
			
		} catch (IOException e) {
			readStudentsWorkSemaphore.release();
			DebugLogDialog.appendException(e);
			throw e;
		}
		readStudentsWorkSemaphore.release();
		DebugLogDialog.endMethod();
	}

	public String googleSheetID(String sheetURL) {

		final Pattern p = Pattern.compile("/spreadsheets/d/([a-zA-Z0-9-_]+)");
		Matcher m = p.matcher(sheetURL);
		String id = "";
		if (m.find()) {
			id = m.group(1);
		} else {
			final String ID_QUERY = "?id=";
			int idIndex = sheetURL.indexOf(ID_QUERY);
			if (idIndex != -1) {
				id = sheetURL.substring(idIndex + ID_QUERY.length());
			}
			int slashIndex = id.lastIndexOf('/');
			if (slashIndex != -1) {
				id = id.substring(slashIndex + 1);
			}
		}		
		return id;
	}
	
	public Spreadsheet getValidSheet(String url) {
		DebugLogDialog.startMethod();
		try {
			initServices();
		} catch (IOException e) {
			DebugLogDialog.appendException(e);
		}
		String id = googleSheetID(url);
		if (id.length() < 25) {
			DebugLogDialog.endMethod();
			return null;
		}		
		Spreadsheet spreadSheet = null;
		try {
			spreadSheet = sheetsService.spreadsheets().get(id).execute();
		} catch (IOException e) {
			DebugLogDialog.endMethod();
			DebugLogDialog.appendException(e);
			return null;
		}
		if (spreadSheet == null) {
			DebugLogDialog.endMethod();
			return null;
		}
		DebugLogDialog.endMethod();
		return spreadSheet;
	}

	public List<Sheet> getSheetNames(String sheetURL, DataFetchListener fetchListener) throws IOException {
		DebugLogDialog.startMethod();
		initServices();
		Spreadsheet spreadSheet = getValidSheet(sheetURL);
		if (spreadSheet == null) {
			DebugLogDialog.endMethod();
			return null;
		}
		String id = googleSheetID(sheetURL);
		GoogleSheetData fileName = new GoogleSheetData(spreadSheet.getProperties().getTitle(), id, sheetURL);
		fileName.setEmpty(true);
		if (fetchListener != null) {
			fetchListener.retrievedInfo(fileName);
		}
		DebugLogDialog.endMethod();
		return getSheetNames(fileName, fetchListener);
	}
	
	public List<Sheet> getSheetNames(GoogleSheetData sheetData, DataFetchListener fetchListener) throws IOException {
		DebugLogDialog.startMethod();
		Spreadsheet spreadSheet = sheetsService.spreadsheets().get(sheetData.getSpreadsheetId()).execute();
		List<Sheet> innerSheets = spreadSheet.getSheets();
		for (Sheet sheet : innerSheets) {
			if (fetchListener != null) {
				ClassroomData data = new GoogleSheetData(sheet.getProperties().getTitle(),
						sheetData.getSpreadsheetId(),
						sheet.getProperties().getSheetId().toString());
				data.setRetrievedFromGoogle(true);
				fetchListener.retrievedInfo(data);
			}
		}
		DebugLogDialog.endMethod();
		return innerSheets;
		
	}
	public void fillRubric(Rubric rubric) throws IOException {
		DebugLogDialog.startMethod();
		initServices();
		rubric.loadFromSheet(readSheet(rubric));
	}
	
	public LoadSheetData readSheet(SheetAccessorInterface sheetReader) throws IOException {
		
		
		return readSheet(sheetReader, "FORMATTED_VALUE");
	}
	
	public LoadSheetData readSpecificRange(GoogleSheetData targetSheet, String rangeParam, String valueRenderOption) throws IOException {
		DebugLogDialog.startMethod();
		initServices();
		String id = targetSheet.getSpreadsheetId();
		String sheetName = targetSheet.getName();		
		Sheet current = getSheet(targetSheet);
		if (current != null) {
			String range = sheetName + "!" + rangeParam;
			Values.Get valueGet = sheetsService.spreadsheets().values().get(id, range);
			valueGet.setValueRenderOption(valueRenderOption);
			
			ValueRange response = valueGet.execute();
			
			DebugLogDialog.endMethod();
			return new LoadSheetData(response.getValues());
		}
		DebugLogDialog.endMethod();
		return null;
		
	}

	public LoadSheetData readSheet(SheetAccessorInterface sheetReader, String valueRenderOption) throws IOException {
		DebugLogDialog.startMethod();
		
		initServices();					
		Sheet current = getSheet(sheetReader.getSheetInfo());
		LoadSheetData returnData = null;
		if (current != null) {
			int numCols = current.getProperties().getGridProperties().getColumnCount();
			int numRows = current.getProperties().getGridProperties().getRowCount();
			String range = "A1:" + getColumnName(numCols) + numRows;			
			returnData = readSpecificRange(sheetReader.getSheetInfo(), range, valueRenderOption);
		}
		DebugLogDialog.endMethod();
		return returnData;
	}
	
	Sheet getSheet(GoogleSheetData sheetData) throws IOException  {
		DebugLogDialog.startMethod();
		Sheet current = null;
		String sheetName = sheetData.getName();
		List<Sheet> existing = getSheetNames(sheetData, null);
		for (Sheet sheet : existing) {
			String name = sheet.getProperties().getTitle();
			if (name.compareToIgnoreCase(sheetName) == 0) {
				current = sheet;
				break;

			}
		}
		DebugLogDialog.endMethod();
		return current;

	}
	
	public Sheet createIfNeeded(GoogleSheetData sheetInfo, boolean eraseAllData) throws IOException {
		DebugLogDialog.startMethod();
		String id = sheetInfo.getSpreadsheetId();
		
		
		// Create requestList and set it on the batchUpdateSpreadsheetRequest
		List<Request> requestsList = new ArrayList<Request>();

		Sheet current = getSheet(sheetInfo);
		if (current != null && eraseAllData) {
			UpdateCellsRequest updateRequest = new UpdateCellsRequest();
			GridRange allGrids = new GridRange();
			allGrids.setSheetId(current.getProperties().getSheetId());			
			updateRequest.setRange(allGrids);			
			updateRequest.setFields("userEnteredValue");
			Request request = new Request();
			request.setUpdateCells(updateRequest);
			requestsList.add(request);
		}
		
		if (current == null) {
			AddSheetRequest addRequest = new AddSheetRequest();
			SheetProperties sheetProperties = new SheetProperties();
			addRequest.setProperties(sheetProperties);
			addRequest.setProperties(sheetProperties.setTitle(sheetInfo.getName()));
			// Create a new request with containing the addSheetRequest and add it to the
			// requestList
			Request request = new Request();
			request.setAddSheet(addRequest);
			requestsList.add(request);
		}
		if (requestsList.size() != 0) {
			BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
			// Add the requestList to the batchUpdateSpreadsheetRequest
			batchUpdateSpreadsheetRequest.setRequests(requestsList);
			sheetsService.spreadsheets().batchUpdate(id, batchUpdateSpreadsheetRequest).execute();
			current = getSheet(sheetInfo);
		}
		
		DebugLogDialog.endMethod();
		return current;		
	}

	public void insertColumns(GoogleSheetData targetFile, int start, int numToInsert) throws IOException {
		insertRowsOrColumns(targetFile, start, numToInsert, "COLUMNS");
	}
	
	public void insertRows(GoogleSheetData targetFile, int start, int numToInsert) throws IOException {
		insertRowsOrColumns(targetFile, start, numToInsert, "ROWS");
	}
	
	public void insertRowPlusHeader(GoogleSheetData targetFile, int row, String rowName, int colLocationForName) {
		insertRowOrColumnPlusHeader(targetFile, row, "ROWS", rowName, colLocationForName);
	}
	public void insertColumnPlusHeader(GoogleSheetData targetFile, int column, String colName, int rowLocationForName) {
		insertRowOrColumnPlusHeader(targetFile, column, "COLUMNS", colName, rowLocationForName );
	}
	
	public void moveColumns(GoogleSheetData targetFile, int start, int numToMove, int destinationStart) throws IOException {
		moveRowsOrColumns(targetFile, start, numToMove, destinationStart, "COLUMNS");
	}
	
	public void moveRows(GoogleSheetData targetFile, int start, int numToMove, int destinationStart) throws IOException {
		moveRowsOrColumns(targetFile, start, numToMove, destinationStart, "ROWS");
	}
	
	public void deleteColumns(GoogleSheetData targetFile, int start, int numToDelete) throws IOException {
		deleteRowsOrColumns(targetFile, start, numToDelete, "COLUMNS");
	}
	
	public void deleteRows(GoogleSheetData targetFile, int start, int numToDelete) throws IOException {
		deleteRowsOrColumns(targetFile, start, numToDelete, "ROWS");
	}
	
	
	public void deleteRowsOrColumns(GoogleSheetData targetFile, int start, int numToDelete, String dimensionType) throws IOException	{
		initServices();
		String id = targetFile.getSpreadsheetId();
		Sheet current = getSheet(targetFile);
		if (current != null) {
			List<Request> requestsList = new ArrayList<Request>();
			DeleteDimensionRequest deleteDimension = new DeleteDimensionRequest();
			DimensionRange range = new DimensionRange();
			range.setSheetId(current.getProperties().getSheetId());
			range.setDimension(dimensionType);
			range.setStartIndex(start);
			range.setEndIndex(start + numToDelete);
			deleteDimension.setRange(range);
			Request request = new Request();
			request.setDeleteDimension(deleteDimension);
			requestsList.add(request);
			BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
			batchUpdateSpreadsheetRequest.setRequests(requestsList);
			sheetsService.spreadsheets().batchUpdate(id, batchUpdateSpreadsheetRequest).execute();
		}		
	}
	
	
	public void moveRowsOrColumns(GoogleSheetData targetFile, int start, int numToMove, int destinationStart, String dimensionType) throws IOException	{
		initServices();
		String id = targetFile.getSpreadsheetId();				
		Sheet current = getSheet(targetFile);
		if (current != null) {
			List<Request> requestsList = new ArrayList<Request>();
			MoveDimensionRequest moveRequest = new MoveDimensionRequest();
			DimensionRange range = new DimensionRange();
			range.setSheetId(current.getProperties().getSheetId());
			range.setDimension(dimensionType);
			range.setStartIndex(start);
			range.setEndIndex(start + numToMove);
			moveRequest.setSource(range);
			moveRequest.setDestinationIndex(destinationStart);
			Request request = new Request();
			request.setMoveDimension(moveRequest);
			requestsList.add(request);
			BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
			batchUpdateSpreadsheetRequest.setRequests(requestsList);
			sheetsService.spreadsheets().batchUpdate(id, batchUpdateSpreadsheetRequest).execute();
		}		
	}
	
	public void insertRowsOrColumns(GoogleSheetData targetFile, int rowOrCol, int numToInsert, String dimensionType) throws IOException {
		initServices();
		String id = targetFile.getSpreadsheetId();				
		Sheet current = getSheet(targetFile);
		if (current != null) {
			List<Request> requestsList = new ArrayList<Request>();
			InsertDimensionRequest insertRequest = new InsertDimensionRequest();
			DimensionRange range = new DimensionRange();
			range.setSheetId(current.getProperties().getSheetId());
			range.setDimension(dimensionType);
			range.setStartIndex(rowOrCol);
			range.setEndIndex(rowOrCol + numToInsert);
			insertRequest.setRange(range);
			insertRequest.setInheritFromBefore(false);
			Request request = new Request();
			request.setInsertDimension(insertRequest);
			requestsList.add(request);
			BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
			batchUpdateSpreadsheetRequest.setRequests(requestsList);
			sheetsService.spreadsheets().batchUpdate(id, batchUpdateSpreadsheetRequest).execute();
		}
		
	}
	

	
	private void insertRowOrColumnPlusHeader(GoogleSheetData targetFile, int rowOrCol, String dimensionType, String headerName, int headerLocation) {
		DebugLogDialog.startMethod();
		Sheet current = null;
		String id = null;
		
		try {
			initServices();
			id = targetFile.getSpreadsheetId();					
			current = getSheet(targetFile);
			if (current != null) {
				insertRowsOrColumns(targetFile, rowOrCol, 1, dimensionType);
				List<List<Object>> nameList2d = new ArrayList<List<Object>>();
				List<Object> nameList = new ArrayList<Object>();
				nameList.add(headerName);
				nameList2d.add(nameList);
				int columnNum = (dimensionType.equals("ROWS"))? headerLocation : rowOrCol;
				int rowNum = (dimensionType.equals("ROWS"))? rowOrCol : headerLocation;
				rowNum++;
				ValueRange labelRange = new ValueRange();
				String columnName = getColumnName(columnNum);
				columnName += "" + rowNum;
				String sheetName = targetFile.getName();
				String stringRange = sheetName + "!" + columnName + ":" + columnName;
				labelRange.setRange(stringRange);
				labelRange.setValues(nameList2d);
				sheetsService.spreadsheets().values().update(id,  stringRange,  labelRange).setValueInputOption("RAW").execute();
			}
		} catch (IOException e) {
			DebugLogDialog.appendException(e);
		}
		DebugLogDialog.endMethod();
	}
	
	public static class RowCol {
		public int row;
		public int col;
		public Color color;
		boolean bold;
		public RowCol(int r, int c, Color colo, boolean b) {
			row = r;
			col = c;
			color = colo;
			bold = b;
		}
	}
	public void changeTextColor(GoogleSheetData targetFile, List<RowCol> regionsToChange) {
		//String sheetName = null;
		try {
			initServices();
			String id = targetFile.getSpreadsheetId();
			//sheetName = targetFile.getName();		

			Sheet current = getSheet(targetFile);

			if (current != null) {
				List<Request> requestsList = new ArrayList<Request>();
				for (RowCol rowCol : regionsToChange) {
					// Change the color of the rows in the header
					RepeatCellRequest repeatCellRequest = new RepeatCellRequest();
					GridRange rangeToModify = new GridRange();
					rangeToModify.setSheetId(current.getProperties().getSheetId());
					rangeToModify.setStartRowIndex(rowCol.row);
					rangeToModify.setEndRowIndex(rowCol.row + 1);
					rangeToModify.setStartColumnIndex(rowCol.col);
					rangeToModify.setEndColumnIndex(rowCol.col + 1);

					com.google.api.services.sheets.v4.model.Color modifiedColor = new com.google.api.services.sheets.v4.model.Color();
					modifiedColor.setRed((float) rowCol.color.getRed() / 255);
					modifiedColor.setGreen((float) rowCol.color.getGreen()/ 255);
					modifiedColor.setBlue((float) rowCol.color.getBlue() / 255);

					TextFormat textFormat = new TextFormat();
					textFormat.setForegroundColor(modifiedColor);
					textFormat.setBold(rowCol.bold);
					CellFormat cellFormat = new CellFormat();
					cellFormat.setTextFormat(textFormat);
					CellData cellData = new CellData();
					cellData.setUserEnteredFormat(cellFormat);				

					repeatCellRequest.setRange(rangeToModify);
					repeatCellRequest.setCell(cellData);
					repeatCellRequest.setFields("userEnteredFormat(textFormat)");
					Request request = new Request();
					request.setRepeatCell(repeatCellRequest);
					requestsList.add(request);
				}				
				BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
				batchUpdateSpreadsheetRequest.setRequests(requestsList);
				sheetsService.spreadsheets().batchUpdate(id, batchUpdateSpreadsheetRequest).execute();

			}
		} catch (IOException e) {
			DebugLogDialog.appendException(e);
		}
		DebugLogDialog.endMethod();
		
	}
	
	public void setHeaderRows(GoogleSheetData targetFile, int lastHeaderRow, int lastHeaderCol, int lastToggleRow) {		
		DebugLogDialog.startMethod();
		Sheet current = null;
		String id = null;
		//String sheetName = null;
		try {
			initServices();
			id = targetFile.getSpreadsheetId();
			//sheetName = targetFile.getName();		

			current = getSheet(targetFile);

			if (current != null) {
				List<Request> requestsList = new ArrayList<Request>();
				// Change the color of the rows in the header
				RepeatCellRequest repeatCellRequest = new RepeatCellRequest();
				GridRange rangeToModify = new GridRange();
				rangeToModify.setSheetId(current.getProperties().getSheetId());
				rangeToModify.setStartRowIndex(0);
				rangeToModify.setEndRowIndex(lastHeaderRow);
				com.google.api.services.sheets.v4.model.Color backgroundColor = new com.google.api.services.sheets.v4.model.Color();
				Float grayLevel = (float)Color.LIGHT_GRAY.getRed() / (float)255.0;
				backgroundColor.setRed(grayLevel);
				backgroundColor.setGreen(grayLevel);
				backgroundColor.setBlue(grayLevel);
				
				TextFormat textFormat = new TextFormat();
				textFormat.setBold(true);
				CellFormat cellFormat = new CellFormat();
				cellFormat.setBackgroundColor(backgroundColor);
				cellFormat.setTextFormat(textFormat);
				CellData cellData = new CellData();
				cellData.setUserEnteredFormat(cellFormat);				
				
				repeatCellRequest.setRange(rangeToModify);
				repeatCellRequest.setCell(cellData);
				repeatCellRequest.setFields("userEnteredFormat(backgroundColor,textFormat)");
				Request request = new Request();
				request.setRepeatCell(repeatCellRequest);
				requestsList.add(request);
				
				UpdateBordersRequest updateBorders = new UpdateBordersRequest();
				updateBorders.setRange(rangeToModify);
				
				// Change the border on the bottom row
				Border border = new Border();
				com.google.api.services.sheets.v4.model.Color borderColor = new com.google.api.services.sheets.v4.model.Color();
				borderColor.setRed((float)0.0);
				borderColor.setGreen((float)0.0);
				borderColor.setBlue((float)0.0);
				border.setColor(borderColor);
				border.setStyle("SOLID_MEDIUM");
				updateBorders.setBottom(border);
				Request borderRequest = new Request();
				borderRequest.setUpdateBorders(updateBorders);
				requestsList.add(borderRequest);

				// Change the border on the column
				GridRange colRangeToModify = new GridRange();
				colRangeToModify.setSheetId(current.getProperties().getSheetId());
				colRangeToModify.setStartColumnIndex(0);
				colRangeToModify.setEndColumnIndex(lastHeaderCol);
				colRangeToModify.setStartRowIndex(lastHeaderRow);
				//colRangeToModify.setEndRowIndex(endRowIndex);
				UpdateBordersRequest updateColBorders = new UpdateBordersRequest();
				updateColBorders.setRange(colRangeToModify);
				Border colBorder = new Border();
				
				colBorder.setColor(borderColor);
				colBorder.setStyle("SOLID");
				updateColBorders.setRight(colBorder);
				Request colBorderRequest = new Request();
				colBorderRequest.setUpdateBorders(updateColBorders);
				requestsList.add(colBorderRequest);


				addToggleColors(current, requestsList, lastHeaderRow + 1, lastToggleRow);
				BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
				batchUpdateSpreadsheetRequest.setRequests(requestsList);
				sheetsService.spreadsheets().batchUpdate(id, batchUpdateSpreadsheetRequest).execute();

			}
		} catch (IOException e) {
			DebugLogDialog.appendException(e);
		}
		DebugLogDialog.endMethod();
	}
	
	private void addToggleColors(Sheet current, List<Request> requestsList, int start, int end) {
		for (int i = start; i <= end; i+=2) {
			// Change the color of the rows in the header
			RepeatCellRequest repeatCellRequest = new RepeatCellRequest();
			GridRange rangeToModify = new GridRange();
			rangeToModify.setSheetId(current.getProperties().getSheetId());
			rangeToModify.setStartRowIndex(i);
			rangeToModify.setEndRowIndex(i+1);
			com.google.api.services.sheets.v4.model.Color backgroundColor = new com.google.api.services.sheets.v4.model.Color();
			
			backgroundColor.setRed((float)0.99);
			backgroundColor.setGreen((float)0.99);
			backgroundColor.setBlue((float)0.99);
			
			CellFormat cellFormat = new CellFormat();
			cellFormat.setBackgroundColor(backgroundColor);
			CellData cellData = new CellData();
			cellData.setUserEnteredFormat(cellFormat);							
			repeatCellRequest.setRange(rangeToModify);
			repeatCellRequest.setCell(cellData);
			repeatCellRequest.setFields("userEnteredFormat(backgroundColor)");
			Request request = new Request();
			request.setRepeatCell(repeatCellRequest);
			requestsList.add(request);			
		}		
	}
	
	private void expandIfNeeded(String id, Sheet current, int maxColumn, int maxRow) throws IOException {
		DebugLogDialog.startMethod();
		int currentColumns = current.getProperties().getGridProperties().getColumnCount();
		int currentRows = current.getProperties().getGridProperties().getRowCount();
		List<Request> requestsList = new ArrayList<Request>();



		if (currentColumns <= maxColumn) {
			AppendDimensionRequest appendDimension = new AppendDimensionRequest();
			appendDimension.setDimension("COLUMNS");
			int numToAdd = maxColumn - currentColumns + 1;
			appendDimension.setLength(numToAdd);
			appendDimension.setSheetId(current.getProperties().getSheetId());
			Request request = new Request();
			request.setAppendDimension(appendDimension);
			requestsList.add(request);
		}
		if (currentRows <= maxRow) {
			AppendDimensionRequest appendDimension = new AppendDimensionRequest();
			appendDimension.setDimension("ROWS");
			int numToAdd = maxRow - currentRows + 1;
			appendDimension.setLength(numToAdd);
			appendDimension.setSheetId(current.getProperties().getSheetId());
			Request request = new Request();
			request.setAppendDimension(appendDimension);
			requestsList.add(request);
		}


		if (requestsList.size() != 0) {
			BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
			batchUpdateSpreadsheetRequest.setRequests(requestsList);
			sheetsService.spreadsheets().batchUpdate(id, batchUpdateSpreadsheetRequest).execute();
		}
		DebugLogDialog.endMethod();
	}

	public String writeSheet(SheetAccessorInterface sheetWriter) throws IOException {
		DebugLogDialog.startMethod();
		initServices();
		String id = sheetWriter.getSheetInfo().getSpreadsheetId();

		SaveSheetData saveData = sheetWriter.getSheetSaveState();
		Sheet current = createIfNeeded(sheetWriter.getSheetInfo(), saveData.getResetDataOnSave());
		

		
		
		expandIfNeeded(id, current, saveData.getMaxColumn(), saveData.getMaxRow());
		
		
		BatchUpdateValuesRequest body = new BatchUpdateValuesRequest().setValueInputOption(saveData.getValueType()).setData(saveData.getSaveState());
		BatchUpdateValuesResponse result = sheetsService.spreadsheets().values().batchUpdate(id, body).execute();
		
		if (saveData.getAutoSizeColumnStart() != -1) {
			List<Request> requestsList = new ArrayList<Request>();
			AutoResizeDimensionsRequest autoResize = new AutoResizeDimensionsRequest();
			DimensionRange dimensions = new DimensionRange();
			dimensions.setSheetId(current.getProperties().getSheetId());
			dimensions.setDimension("COLUMNS");
			dimensions.setStartIndex(saveData.getAutoSizeColumnStart());
			dimensions.setEndIndex(saveData.getMaxColumn() + 1);
			autoResize.setDimensions(dimensions);
			Request request = new Request();
			request.setAutoResizeDimensions(autoResize);
			requestsList.add(request);		
			if (requestsList.size() != 0) {
				BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
				batchUpdateSpreadsheetRequest.setRequests(requestsList);
				sheetsService.spreadsheets().batchUpdate(id, batchUpdateSpreadsheetRequest).execute();
			}
		}
		DebugLogDialog.endMethod();
		return result.toString();
	}
	
	public Map<String, LoadSheetData> readWholeSpreadsheet(String sheetURL) throws IOException {
		initServices();
		String id = this.googleSheetID(sheetURL);
		BatchGet get = sheetsService.spreadsheets().values().batchGet(id);
		List<Sheet> sheets = getSheetNames(sheetURL, null);
		List<String> ranges = new ArrayList<String>();

		for (Sheet sheet : sheets) {
			ranges.add(sheet.getProperties().getTitle() + "!A:ZZ");
		}
		get.setRanges(ranges);
		BatchGetValuesResponse ss = get.execute();
		List<ValueRange> values = ss.getValueRanges();
		Map<String, LoadSheetData> wholeSheet = new HashMap<String, LoadSheetData>();
		for (int i = 0; i < values.size(); i++) {
			String sheetName = sheets.get(i).getProperties().getTitle(); 
			ValueRange valueRange = values.get(i);
			wholeSheet.put(sheetName, new LoadSheetData(valueRange.getValues()));
		}
		return wholeSheet;
		
	}

	public void listFoldersInRoot() throws IOException {
		DebugLogDialog.startMethod();
		initServices();
		FileList result = driveService.files().list()
				.setQ("'root' in parents and mimeType = 'application/vnd.google-apps.folder' and trashed = false")
				.setSpaces("drive").setFields("nextPageToken, files(id, name, parents)").execute();
		List<com.google.api.services.drive.model.File> folders = result.getFiles();
		for (com.google.api.services.drive.model.File folder : folders) {
			System.out.println(folder);
		}
		DebugLogDialog.endMethod();
	}

	public Map<File, List<File>> listChildItemsOfFolder(String searchParentFolderName) throws IOException {
		Map<File, List<File>> results = new HashMap<File, List<File>>();
		DebugLogDialog.startMethod();
		initServices();
		FileList result = driveService.files().list()
				.setQ(String.format(
						"name = '%s' and mimeType = 'application/vnd.google-apps.folder' and trashed = false",
						searchParentFolderName))
				.setSpaces("drive").setFields("nextPageToken, files(id, name, parents)").execute();

		List<File> foldersMatchingSearchName = result.getFiles();

		if (foldersMatchingSearchName != null && !foldersMatchingSearchName.isEmpty()) {
			for (File folder : foldersMatchingSearchName) {
				FileList childResult = driveService.files().list()
						.setQ(String.format("'%s' in parents and trashed = false", folder.getId())).setSpaces("drive")
						.setFields("nextPageToken, files(id, name, parents)").execute();

				List<File> childItems = childResult.getFiles();

				if (childItems != null && !childItems.isEmpty()) {
					results.put(folder, childItems);
				}
			}
		}
		DebugLogDialog.endMethod();
		return results;
	}
	

	

	
	public static class TestAccessor implements SheetAccessorInterface{

		GoogleSheetData sheetData;
		public TestAccessor() {
			sheetData = new GoogleSheetData("TestStuff2", "1o69WgpVf5LnDKvXBRwx5Rwik4xDavJHuHYuSdoG82cY", "PageTest");
			
		}
		

		@Override
		public GoogleSheetData getSheetInfo() {
			// TODO Auto-generated method stub
			return sheetData;
		}


		@Override
		public SaveSheetData getSheetSaveState() {
			SaveSheetData saveState = new SaveSheetData(SaveSheetData.ValueType.USER_ENTERED, "TestStuff2", true);
			saveState.setAutoSizeColumnStart(3);
			
			
			List<Object> sums = new ArrayList<Object>();
			for (int i = 1; i < 20; i++) {
				sums.add("=ROUND(SUM(B"  + i + ":D" + i + "))");
			}
			saveState.writeFullColumn(sums, 0);
			for (int i= 1; i < 3; i++) {
				List<Object> values = new ArrayList<Object>();
				for (int j = 0; j < 20; j++ ) {
					values.add((Double)(Math.random()));
				}
				saveState.writeFullColumn(values, i);
			}
			for (int i = 3; i < 6; i++) {
				List<Object> strings = new ArrayList<Object>();
				for (int j = 0; j < 20; j++ ) {
					int numToRepeat = (int)(Math.random() * 10);
					String test = "Test ";
					for (int x = 0; x < numToRepeat; x++) {
						test += "More ";
					}
					strings.add(test);
				}
				saveState.writeFullColumn(strings, i);
				
			}
			return saveState;
			
		}
	}

	public static void main(String[] args) throws IOException, GeneralSecurityException {

		GoogleClassroomCommunicator communicator = new GoogleClassroomCommunicator("Google Classroom Grader", "C:\\Users\\kdmacdon\\Documents\\Teals\\GoogleClassroomData\\tokens", "C:\\Users\\kdmacdon\\Documents\\Teals\\GoogleClassroomData\\credentials.json");
		//TestAccessor test = new TestAccessor();

		//communicator.writeSheet(test);
		//System.out.println(communicator.readWholeSpreadsheet("https://drive.google.com/open?id=1o69WgpVf5LnDKvXBRwx5Rwik4xDavJHuHYuSdoG82cY"));
		//public void changeTextColor(GoogleSheetData targetFile, List<RowCol> regionsToChange, Color color) {
		GoogleSheetData targetFile = new GoogleSheetData("TestStuff", "1o69WgpVf5LnDKvXBRwx5Rwik4xDavJHuHYuSdoG82cY", "TestStuff");
//		List<RowCol> rowCols = new ArrayList<RowCol>();
//		rowCols.add(new RowCol(0, 0, Color.MAGENTA, true));
//		rowCols.add(new RowCol(1, 0, Color.BLUE, true));
//		rowCols.add(new RowCol(12, 4, Color.RED, true));
//		rowCols.add(new RowCol(12, 5, Color.GREEN, true));
//		communicator.changeTextColor(targetFile, rowCols);
		communicator.deleteColumns(targetFile, 0, 1);
		System.err.println("done");

	}

}
