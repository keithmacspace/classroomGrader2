package net.cdonald.googleClassroom.gui.findUngraded;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.cdonald.googleClassroom.googleClassroomInterface.GoogleClassroomCommunicator;
import net.cdonald.googleClassroom.googleClassroomInterface.GradeSheet;
import net.cdonald.googleClassroom.googleClassroomInterface.LoadSheetData;
import net.cdonald.googleClassroom.googleClassroomInterface.SaveSheetData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.StudentData;
import net.cdonald.googleClassroom.utils.SimpleUtils;

public class BasicSheetInfo extends GradeSheet {
	public class AssignmentData {
		StudentData studentData;
		Date submitDate;
		boolean unparsableDate;

		public AssignmentData(StudentData student, String date) {
			studentData = student;
			submitDate = null;
			unparsableDate = false;
			if (date != null && date.length() > 3) {				
				submitDate = SimpleUtils.createDate(date);
				unparsableDate = (submitDate == null);
			}			
		}
		public StudentData getStudent() {
			return studentData;
		}
		public Date getSubmitDate() {
			return submitDate;
		}
		public String toString() {
			return studentData.getFirstName() + " " + studentData.getName() + " " + submitDate;
		}
		public boolean isUnparsableDate() {
			return unparsableDate;
		}		
	}
	private List<AssignmentData> assignmentInfo;
	private LoadSheetData data;
	private String graderName;
	private String assignmentName;
	private boolean gradeSheet;

	public BasicSheetInfo(GoogleClassroomCommunicator communicator, GoogleSheetData targetFile, LoadSheetData data)  {
		super(communicator, targetFile);
		this.data = data;
		assignmentInfo = new ArrayList<AssignmentData>();
	}
	public boolean isAGradeSheet() {
		return gradeSheet;
	}
	
	public void readInfo(List<StudentData> students) throws IOException {
		assignmentInfo.clear();
		try {
			gradeSheet = true;
			processRows(data, students);
		}
		catch (NotAGradeSheetException e){
			gradeSheet = false;
			return;
		}
		
		if (data != null) {
			loadAssignmentName(data);
			processMissingAssignments(data);
			loadGraderName(data);
		}
	}
	
	public boolean hasInfo() {
		return assignmentInfo.size() != 0;
	}
	
	public List<AssignmentData> getAssignmentInfo() {
		return assignmentInfo;
	}
	
	public String getAssignmentName() {
		return assignmentName;
	}
	
	public String getGraderName() {		
		return graderName;
	}
	
	private void loadGraderName(LoadSheetData data) {
		int rowNum = getRowLocation(RowTypes.GRADED_BY);
		if (rowNum >= 0 && data.getNumRows() > rowNum) {
			List<Object> graderNameRow = data.readRow(rowNum);
			if (graderNameRow != null) {
				for (int i = 1; i < graderNameRow.size(); i++) {
					if (graderNameRow.get(i) instanceof String) {
						String colString = (String)graderNameRow.get(i);
						if (colString.length() > 1 && colString.equals(RowTypes.GRADED_BY.getSearchString()) == false) {
							graderName = colString;
							return;
						}
					}
				}				
			}
		}		
		
	}

	
	private void loadAssignmentName(LoadSheetData data) {
		int rowNum = getRowLocation(RowTypes.ASSIGNMENT);
		if (rowNum >= 0 && data.getNumRows() > rowNum) {
			List<Object> assignmentNameRow = data.readRow(rowNum);
			if (assignmentNameRow != null) {
				for (int i = 0; i < assignmentNameRow.size(); i++) {
					if (assignmentNameRow.get(i) instanceof String) {
						String colString = (String)assignmentNameRow.get(i);
						if (colString.equalsIgnoreCase("Name:") && assignmentNameRow.size() > i + 1) {
							assignmentName = assignmentNameRow.get(i + 1).toString();							
							return;							
						}
					}
				}				
			}
		}		
	}
	
	private void processMissingAssignments(LoadSheetData data) {
		if (assignmentName != null){
			for (StudentRow student : getStudentRowList()) {
				int rowNum = student.getRowNumber();
				if (rowNum > 0 && data.getNumRows() > rowNum) {
					List<Object> studentRow = data.readRow(rowNum);
					if (studentRow.size() > SUBMIT_DATA_COLUMN) {
						String submit = studentRow.get(SUBMIT_DATA_COLUMN).toString();
						if (submit == null || submit.length() < 3) {
							assignmentInfo.add(new AssignmentData(student.getStudent(), submit));
						}
					}
				}
			}
		}
	}
	
	public void printDebugInfo() {
		System.out.println(assignmentName);
		for (AssignmentData student : assignmentInfo) {
			System.out.println(student);
		}
	}
	public class NotAGradeSheetException extends RuntimeException {

		public NotAGradeSheetException() {
			super();
			// TODO Auto-generated constructor stub
		}

		public NotAGradeSheetException(String message) {
			super(message);
			// TODO Auto-generated constructor stub
		}
		
	}

	@Override
	public SaveSheetData getSheetSaveState() {
		
		return null;
	}

	@Override
	public void fillDefaultColumns() {
		throw new NotAGradeSheetException();
		
	}

	@Override
	protected void addColumnLocation(String columnName, int location) {		
	}
	
	protected void insertRow(LoadSheetData data, int rowNum, RowTypes rowType) {
		throw new NotAGradeSheetException();
	}



}
