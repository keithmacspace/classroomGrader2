package net.cdonald.googleClassroom.gui.findUngraded;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.api.services.classroom.model.AssignmentSubmission;
import com.google.api.services.classroom.model.StudentSubmission;

import net.cdonald.googleClassroom.googleClassroomInterface.AssignmentFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.GoogleClassroomCommunicator;
import net.cdonald.googleClassroom.googleClassroomInterface.LoadSheetData;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.StudentFetcher;
import net.cdonald.googleClassroom.gui.MainGoogleClassroomFrame;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentClassQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryListener;
import net.cdonald.googleClassroom.listenerCoordinator.SheetFetcherListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.model.StudentData;

public class FindUngraded {
	private class UngradedInfo {
		private List<String> students = new ArrayList<String>();
		private String gradedBy;
		protected String getGradedBy() {
			return gradedBy;
		}
		protected void setGradedBy(String gradedBy) {
			this.gradedBy = gradedBy;
		}
		public UngradedInfo(String gradedBy) {
			setGradedBy(gradedBy);
		}
		public void addStudent(String studentID) {
			students.add(studentID);
		}
		public List<String> getStudents() {
			return students;
		}
	}
	private Map<String, UngradedInfo > ungradedAssignments; 
	private Map<String, UngradedInfo > submittedUpdate; 
	private GoogleClassroomCommunicator communicator;
	private String url;
	
	public FindUngraded(GoogleClassroomCommunicator communicator, String url) throws IOException {
		this.communicator = communicator;
		this.url = url;
		ungradedAssignments = new HashMap<String, UngradedInfo>();
		submittedUpdate = new HashMap<String, UngradedInfo>();
		System.out.println("Reading Grades");		
		
	}

	public void createUngradedList(List<ClassroomData> assignments, List<StudentData> students) throws IOException {
		Map<String, LoadSheetData> loadSheetData = communicator.readWholeSheet(url);						
		List<BasicSheetInfo> assignmentSheets = new ArrayList<BasicSheetInfo>();
		for (LoadSheetData sheetData : loadSheetData.values()) {
			if (sheetData != null) {
				assignmentSheets.add(new BasicSheetInfo(communicator, null, sheetData));
			}
		}
		populateMap(communicator, assignments, students, assignmentSheets);
		saveUngraded(communicator, assignments, students, assignmentSheets);
		System.out.println("Done!");
	}
	
	private void populateMap(GoogleClassroomCommunicator communicator, List<ClassroomData> assignments, List<StudentData> students, List<BasicSheetInfo> assignmentSheets) {
		ClassroomData course = (ClassroomData)ListenerCoordinator.runQuery(GetCurrentClassQuery.class);
		for (BasicSheetInfo assignmentSheet : assignmentSheets) {
			boolean retry = false;
			int retryCount = 0;
			do {
				try {					
					assignmentSheet.readInfo(students);
					if (assignmentSheet.hasInfo()) {
						if (assignmentSheet.getAssignmentName() != null) {
							for (ClassroomData assignment : assignments) {
								if (assignment.getName().equals(assignmentSheet.getAssignmentName())) {
									populateSingleAssignmentEntry(course, assignment, assignmentSheet);
								}
							}
						}
					}
				} catch (IOException e) {
					// Sometimes, because of the vast data we are reading, we get a socket reset.
					retryCount++;
					if (retryCount < 3) {
						retry = true;
					}
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}while(retry == true);
		}
		
	}
	
	private void populateSingleAssignmentEntry(ClassroomData course, ClassroomData assignment, BasicSheetInfo assignmentSheet) {
		
		boolean retry = false;
		int retryCount = 0;
		do {
			retry = false;
			try {
				List<StudentSubmission> submissions = communicator.getStudentSubmissions(course, assignment);
				for (BasicSheetInfo.AssignmentData studentInfo : assignmentSheet.getAssignmentInfo()) {
					if (studentInfo.isUnparsableDate() == false && (studentInfo.getSubmitDate() == null)) {
						for (StudentSubmission submission : submissions) {
							String studentID = submission.getUserId();
							if (studentID.equals(studentInfo.getStudent().getId())) {
								AssignmentSubmission assignmentSubmission = submission.getAssignmentSubmission();
								if (assignmentSubmission != null && assignmentSubmission.getAttachments() != null) {
									updateMap(assignment.getId(), assignmentSheet.getGraderName(), ungradedAssignments, studentInfo.getStudent());
								}
							}
						}
					}
				}
			}
			catch(IOException e) {
				retryCount++;
				if (retryCount < 3) {
					retryCount++;
				}
			}
		}while(retry == true);
	}
	
	private void updateMap(String assignmentID, String graderName, Map<String, UngradedInfo > mapToMod, StudentData studentInfo) {
		UngradedInfo ungradedInfo = mapToMod.get(assignmentID);
		if (ungradedInfo == null) {
			ungradedInfo = new UngradedInfo(graderName);
			mapToMod.put(assignmentID, ungradedInfo);
		}
		ungradedInfo.addStudent(studentInfo.getFirstName() + " " + studentInfo.getName());		
	}
	
	private void saveUngraded( GoogleClassroomCommunicator communicator, List<ClassroomData> assignments, List<StudentData> students, List<BasicSheetInfo> assignmentSheets) {
		System.out.println("Ungraded:");
		printMap(assignments, ungradedAssignments);
		System.out.println("Updated:");
		printMap(assignments, submittedUpdate);
	}
	
		
	private void printMap( List<ClassroomData> assignments, Map<String, UngradedInfo > mapToPrint) {
		for (ClassroomData assignment : assignments) {
			UngradedInfo info = mapToPrint.get(assignment.getId());
			if (info != null) {
				System.out.print(assignment.getName() + " graded by: " + info.getGradedBy() + " : ");
				for (String name : info.getStudents()) {
					System.out.print(name + ", ");
				}
				System.out.println();				
				
			}
		}		
	}
	


	/**
	 * 
	 * All of this is only for the standalone version.
	 */

	volatile boolean fetchesComplete = false;

	public void fetchStudents(List<ClassroomData> assignments) {
		List<StudentData> students = new ArrayList<StudentData>();
		StudentFetcher fetcher = new StudentFetcher(communicator);
		LongQueryListener<ClassroomData> studentListener = new LongQueryListener<ClassroomData>() {
			@Override
			public void process(List<ClassroomData> list) {
				for (ClassroomData data : list) {
					students.add((StudentData) data);					
				}					
			}
			@Override
			public void done() {
				System.out.println("Creating ungraded list");
				try {
					createUngradedList(assignments,  students);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				fetchesComplete = true;
			}
			@Override
			public void remove(Set<String> removeList) {
				
			}					
		};	
		
		fetcher.setListener(studentListener);
		fetcher.execute();


	}
	
	public void startFetching(String url) {
		
		List<ClassroomData> assignments = new ArrayList<ClassroomData>();
		AssignmentFetcher fetcher = new AssignmentFetcher(communicator);
		LongQueryListener<ClassroomData> assignmentListener = new LongQueryListener<ClassroomData>() {

			@Override
			public void process(List<ClassroomData> list) {
				assignments.addAll(list);
			}

			@Override
			public void remove(Set<String> removeList) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void done() {
				System.out.println("Fetching Students");
				fetchStudents(assignments);
			}
		};
		fetcher.setListener(assignmentListener);
		fetcher.execute();
			while(fetchesComplete == false);
			System.out.println(new Date());

		
	}
	public static void runStandalone() {
		MyPreferences prefs = new MyPreferences();
		try {
			// All of the ClassroomData fetchers use this to get which classroom we are using
			ListenerCoordinator.addQueryResponder(GetCurrentClassQuery.class, new GetCurrentClassQuery(){
				@Override
				public ClassroomData fired() {
					return prefs.getClassroom();
				}			
			});

			GoogleClassroomCommunicator commun = new GoogleClassroomCommunicator(MainGoogleClassroomFrame.APP_NAME, prefs.getTokenDir(), prefs.getJsonPath());
			ListenerCoordinator.addLongQueryReponder(SheetFetcher.class, new SheetFetcher(commun));
			String url = prefs.getGradeURL();		
			FindUngraded ungraded = new FindUngraded(commun, url);			
			ungraded.startFetching(url);
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String []args) {
		runStandalone();

	}
}
