package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.cdonald.googleClassroom.gui.StudentSheetColumns;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.StudentData;
import net.cdonald.googleClassroom.utils.SimpleUtils;

public class GradeSummaryUpdate extends StudentSheetColumns {
	public enum RowTypes {
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
	
	GradeSummaryUpdate(GoogleClassroomCommunicator communicator, GoogleSheetData targetFile, List<StudentData> students) {
		super(communicator, new GoogleSheetData("Recently Updated", targetFile.getSpreadsheetId(), "Recently Updated"));
		super.fillDefaultStudentRows(RowTypes.STUDENT_ROW.ordinal(), students);
		this.students = students;
	}
	
	public void syncData(String gradedBy, ClassroomData assignment, Rubric rubric) throws IOException {
		saveSheetData = new SaveSheetData(SaveSheetData.ValueType.USER_ENTERED, "Recently Updated", true);
		LoadSheetData data = getCommunicator().readSheet(this);
		boolean changeColors = false;
		if (data == null || data.isEmpty()) {
			fillDefaultStudentRows(RowTypes.STUDENT_ROW.ordinal(), students);			
			data = new LoadSheetData(createDefaultList(students));
			addFirstTwoColumns(data, students);
			changeColors = true;
		}
		else {
			int studentRow = findStudentStartRow(data, students);
			int diff = studentRow - RowTypes.STUDENT_ROW.ordinal();
			if (diff != 0) {
				adjustStudentRows(diff);				
			}
			List<Object> assignmentRow = data.readRow(RowTypes.ASSIGNMENT.ordinal());
			for (int i = assignmentRow.size() - 1; i > 1; i--) {
				String check = assignmentRow.get(i).toString();
				boolean delete = false;
				if (check.equals(assignment.getName())) {
					delete = true;
				}
				else if (check.length() == 0 && i % 2 ==  0) {
					delete = true;
				}
				if (delete) {
					data.deleteCols(i, i + 1);
				}
			}
		}
		addNewDataColumns(data, gradedBy, rubric, students, assignment);
		for (int rowIndex = 0; rowIndex < data.getNumRows(); rowIndex++) {
			saveSheetData.addOneRow(data.readRow(rowIndex), rowIndex + 1);
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
	
	private void addFirstTwoColumns(LoadSheetData data, List<StudentData> students) {
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
		data.insertCol(0, columnZero);
		data.insertCol(1, columnOne);
	}
	
	private void addNewDataColumns(LoadSheetData data, String gradedBy, Rubric rubric, List<StudentData> students, ClassroomData assignment) {
		List<Object> totalColumn = new ArrayList<Object>();
		List<Object> onTimeColumn = new ArrayList<Object>();
		for (int i = 0; i < RowTypes.STUDENT_ROW.ordinal(); i++) {
			totalColumn.add(null);
			onTimeColumn.add(null);
		}
		totalColumn.set(RowTypes.ASSIGNMENT.ordinal(), assignment.getName());
		totalColumn.set(RowTypes.GRADED_BY.ordinal(), gradedBy);
		totalColumn.set(RowTypes.RUBRIC_NAME.ordinal(), rubric.getName());
		totalColumn.set(RowTypes.UPDATE_TIME.ordinal(), new Date().toString());
		totalColumn.set(RowTypes.RUBRIC_HEADINGS.ordinal(), "Total");
		
		onTimeColumn.set(RowTypes.ASSIGNMENT.ordinal(), "");
		onTimeColumn.set(RowTypes.GRADED_BY.ordinal(), "");
		onTimeColumn.set(RowTypes.RUBRIC_NAME.ordinal(), "");
		onTimeColumn.set(RowTypes.UPDATE_TIME.ordinal(), "");
		onTimeColumn.set(RowTypes.RUBRIC_HEADINGS.ordinal(), "On Time");

		Date assignmentDate = assignment.getDate();
		for (StudentRow studentRow : getStudentRowList()) {
			StudentData student = studentRow.getStudent();
			Date studentDate = studentRow.getDate();
			if (studentDate != null) {
				String id = student.getId();
				totalColumn.add(rubric.getTotalValue(id));
				onTimeColumn.add(SimpleUtils.formatLate(studentDate, assignmentDate));
			}
			else {
				totalColumn.add("");
				onTimeColumn.add("missing");
			}
		}
		data.insertCol(2, totalColumn);
		data.insertCol(3, onTimeColumn);
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
