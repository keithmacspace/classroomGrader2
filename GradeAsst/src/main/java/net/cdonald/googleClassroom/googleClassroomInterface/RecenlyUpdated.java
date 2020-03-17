package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.cdonald.googleClassroom.gui.StudentSheetColumns;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.StudentData;
import net.cdonald.googleClassroom.utils.SimpleUtils;

public class RecenlyUpdated extends StudentSheetColumns {
	public enum RowTypes {
		MESSAGE(""),
		ASSIGNMENT("Assignment:"),		
		GRADED_BY("Graded By:"),
		UPDATE_TIME("Update Time:"),
		RUBRIC_NAME("Rubric Tab:"),
		RUBRIC_HEADINGS(""),
		STUDENT_ROW("");
		
		RowTypes(String searchValue) {
			this.searchString = searchValue;			
		}

		public String getSearchString() {
			return searchString;
		}
		private String searchString;
	}
	private SaveSheetData saveSheetData;
	private List<StudentData> students;
	private GradeSyncer gradeSyncer;
	private String sheetLink;
	
	RecenlyUpdated(GradeSyncer gradeSyncer, GoogleClassroomCommunicator communicator, GoogleSheetData targetFile, List<StudentData> students) {
		super(communicator, new GoogleSheetData("Recently Updated", targetFile.getSpreadsheetId(), "Recently Updated"));		
		this.students = students;
		this.gradeSyncer = gradeSyncer;
		sheetLink = "=" + gradeSyncer.getSheetInfo().getName() + "!";
	}

	
	public void syncData() throws IOException {		
		saveSheetData = new SaveSheetData(SaveSheetData.ValueType.USER_ENTERED, "Recently Updated", false);
		LoadSheetData data = getCommunicator().readSheet(this, "FORMATTED_VALUE");
		boolean changeColors = false;
		boolean addData = false;
		if (data == null || data.isEmpty()) {
			fillDefaultStudentRows(RowTypes.STUDENT_ROW.ordinal(), students);			
			data = new LoadSheetData(createDefaultList(students));
			addFirstTwoColumns(saveSheetData, students);
			changeColors = true;
			addData = true;
		}
		else {
			int studentRow = findStudentStartRow(data, students);
			int diff = studentRow - RowTypes.STUDENT_ROW.ordinal();
			if (diff != 0) {
				adjustStudentRows(diff);				
			}
			
			List<Object> assignmentRow = data.readRow(RowTypes.RUBRIC_NAME.ordinal());
			if (assignmentRow != null && assignmentRow.size() > 2) {
				String check = assignmentRow.get(2).toString();
				if (!check.equals(gradeSyncer.getSheetInfo().getName())) {
					addData = true;
					getCommunicator().insertColumn(getTargetFile(), 2, "", 2);
					getCommunicator().insertColumn(getTargetFile(), 2, "", 2);
				}
			}
		}
		if (addData == true) {
			addNewDataColumns(saveSheetData, gradeSyncer, students);
		}
		getCommunicator().writeSheet(this);
		if (changeColors) {
			changeBorderColors(RowTypes.STUDENT_ROW.ordinal(), RowTypes.STUDENT_ROW.ordinal() + students.size());
		}
	}
	
	private List<List<Object> > createDefaultList(List<StudentData> students) {	
		int size = students.size() + RowTypes.STUDENT_ROW.ordinal();
		List<List<Object>> defaultList = new ArrayList<List<Object>>();
		for (int i = 0; i < size; i++) {
			defaultList.add(new ArrayList<Object>());
		}
		return defaultList;		
	}
	
	private void addFirstTwoColumns(SaveSheetData sheetData, List<StudentData> students) {
		List<Object> columnZero = new ArrayList<Object>();
		List<Object> columnOne = new ArrayList<Object>();
		for (int i = 0; i < RowTypes.STUDENT_ROW.ordinal(); i++) {
			RowTypes rowType = RowTypes.values()[i];
			columnZero.add(rowType.getSearchString());
			columnOne.add("");
		}
		for (StudentRow studentRow : getStudentRowList()) {
			StudentData student = studentRow.getStudent();
			columnZero.add(student.getName());
			columnOne.add(student.getFirstName());
		}

		sheetData.writeFullColumn(columnZero, 0);
		sheetData.writeFullColumn(columnOne, 1);
	}
	
	private String createLink(GradeSyncer gradeSyncer, GradeSheet.RowTypes rowType, String columnKey, int adjustment) {
		return createLink(gradeSyncer, rowType, gradeSyncer.getColumnLocation(columnKey) + adjustment);
	}
	private String createLink(GradeSyncer gradeSyncer, GradeSheet.RowTypes rowType, int columnNum) {
		return createLink(gradeSyncer, gradeSyncer.getRowLocation(rowType), columnNum);
	}
	
	private String createLink(GradeSyncer gradeSyncer, int rowNumber, int columnNumber) {
		String linkString = sheetLink;
		linkString += GoogleClassroomCommunicator.getColumnName(columnNumber);
		linkString += (rowNumber + 1);
		return linkString;		
	}
	
	private void addNewDataColumns(SaveSheetData sheetData, GradeSyncer gradeSyncer, List<StudentData> students) {
		List<Object> totalColumn = new ArrayList<Object>();
		List<Object> onTimeColumn = new ArrayList<Object>();
		for (int i = 0; i < RowTypes.STUDENT_ROW.ordinal(); i++) {
			totalColumn.add(null);
			onTimeColumn.add(null);
		}
		
		totalColumn.set(RowTypes.ASSIGNMENT.ordinal(), createLink(gradeSyncer, GradeSheet.RowTypes.ASSIGNMENT, 4) );
		totalColumn.set(RowTypes.GRADED_BY.ordinal(), createLink(gradeSyncer, GradeSheet.RowTypes.GRADED_BY, GradeSheet.TOTAL_STRING, 1));
		totalColumn.set(RowTypes.RUBRIC_NAME.ordinal(), gradeSyncer.getSheetInfo().getName());
		totalColumn.set(RowTypes.UPDATE_TIME.ordinal(), new Date().toString());
		totalColumn.set(RowTypes.RUBRIC_HEADINGS.ordinal(), "Total");
		
		onTimeColumn.set(RowTypes.ASSIGNMENT.ordinal(), "");
		onTimeColumn.set(RowTypes.GRADED_BY.ordinal(), "");
		onTimeColumn.set(RowTypes.RUBRIC_NAME.ordinal(), "");
		onTimeColumn.set(RowTypes.UPDATE_TIME.ordinal(), "");
		onTimeColumn.set(RowTypes.RUBRIC_HEADINGS.ordinal(), "On Time");

		String totalColumnLink = sheetLink + "$" + GoogleClassroomCommunicator.getColumnName(gradeSyncer.getColumnLocation(GradeSyncer.TOTAL_STRING));
		String lateByLink = sheetLink + "$" + GoogleClassroomCommunicator.getColumnName(gradeSyncer.getColumnLocation(GradeSyncer.LATE_INFO_COLUMN_KEY));
		
		
		int possibleStart = 0;
		for (StudentRow studentRow : getStudentRowList()) {
			StudentData student = studentRow.getStudent();
			int linkRow = gradeSyncer.getStudentRow(student.getId(), possibleStart);
			if (linkRow != -1) {
				linkRow++;
				totalColumn.add(totalColumnLink + "$" + linkRow);
				onTimeColumn.add(lateByLink + "$" + linkRow);
			}
			else {
				totalColumn.add("");
				onTimeColumn.add("missing");
			}
			possibleStart++;
		}
		sheetData.writeFullColumn(totalColumn, 2);
		sheetData.writeFullColumn(onTimeColumn, 3);
	}



	@Override
	public SaveSheetData getSheetSaveState() {
		// TODO Auto-generated method stub
		return saveSheetData;
	}

	@Override
	protected void setFirstLastNameColumns(int firstNameCol, int lastNameCol) {
		// TODO Auto-generated method stub
		
	}

}
