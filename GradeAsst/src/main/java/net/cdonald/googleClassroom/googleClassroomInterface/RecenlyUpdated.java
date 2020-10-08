package net.cdonald.googleClassroom.googleClassroomInterface;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.cdonald.googleClassroom.gui.StudentSheetColumns;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.StudentData;

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
	
	RecenlyUpdated(GradeSyncer gradeSyncer, GoogleClassroomCommunicator communicator, GoogleSheetData targetFile, List<StudentData> students) throws IOException {
		super(communicator, new GoogleSheetData("Recently Updated", targetFile.getSpreadsheetId(), "Recently Updated"));		
		this.students = students;
		this.gradeSyncer = gradeSyncer;
		sheetLink = "='" + gradeSyncer.getSheetInfo().getName() + "'!";
		getCommunicator().createIfNeeded(getTargetFile(), false);
	}

	
	public void syncData(Rubric rubric) throws IOException {		
		saveSheetData = new SaveSheetData(SaveSheetData.ValueType.USER_ENTERED, "Recently Updated", false);
		LoadSheetData data = getCommunicator().readSheet(this, "FORMATTED_VALUE");
		boolean changeColors = false;
		boolean addData = false;
		
		if (data == null || data.isEmpty() || 
				data.getNumRows() < RowTypes.STUDENT_ROW.ordinal() + students.size() || 
				data.readRow(RowTypes.STUDENT_ROW.ordinal()).size() < 2) {
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
			// Students may have changed their names, so add the student columns no matter what
			addFirstTwoColumns(saveSheetData, students);
			List<Object> assignmentRow = data.readRow(RowTypes.RUBRIC_NAME.ordinal());
		
			if (assignmentRow.size() <= 2) {
				addData = true;
			}
			else {
				// data is not valid past this point
				assignmentRow = deleteGaps(data, assignmentRow);
				boolean moved = moveExisting(assignmentRow);
				if (moved == false && assignmentRow != null && assignmentRow.size() > 2) {
					String check = assignmentRow.get(2).toString();
					if (!check.equals(gradeSyncer.getSheetInfo().getName())) {
						addData = true;
						getCommunicator().insertColumns(getTargetFile(), 2, 2);
					}
				}
			}
		}
		if (addData == true) {
			addNewDataColumns(saveSheetData, gradeSyncer, students);
		}
		else {
			List<Object> oneEntry = new ArrayList<Object>();
			oneEntry.add(new Date().toString());
			saveSheetData.writeColumnEntries(oneEntry, 2, RowTypes.UPDATE_TIME.ordinal());
		}
		getCommunicator().writeSheet(this);
		if (changeColors) {
			changeBorderColors(RowTypes.STUDENT_ROW.ordinal(), RowTypes.STUDENT_ROW.ordinal() + students.size());
		}
		changeTextColors(rubric, addData);
	}
	
	private boolean moveExisting(List<Object> assignmentRow) throws IOException {
		for (int i = 4; i < assignmentRow.size(); i+= 2) {
			String check = assignmentRow.get(i).toString();
			if (check.equals(gradeSyncer.getSheetInfo().getName())) {
				getCommunicator().moveColumns(getTargetFile(), i, 2, 2);
				return true;
			}
		}
		return false;		
	}
	
	private List<Object> deleteGaps(LoadSheetData data, List<Object> assignmentRow) throws IOException {
		List<Integer> deleteList = new ArrayList<Integer>();
		boolean foundValid = false;
		List<Object> headingRow = data.readRow(RowTypes.RUBRIC_HEADINGS.ordinal());
		for (int i = headingRow.size() - 1; i >= 2; i--) {
			Object check1 = headingRow.get(i);
			String check = headingRow.get(i).toString();
			if (check1 == null || check.length() < 1) {
				if (foundValid) {
					deleteList.add(i);
				}
			}
			else {
				foundValid = true;
			}					
		}
		if (deleteList.size() != 0) {
			int current = deleteList.size() - 1;
			while (current >= 0) {
				int startIndex = current;
				current--;
				while (current >= 0 && deleteList.get(startIndex) == (deleteList.get(current) - 1)) {
					current--;
				}
				int endIndex = current + 1;
				int deleteStart = deleteList.get(startIndex);
				int size = deleteList.get(endIndex) - deleteStart + 1;
				getCommunicator().deleteColumns(getSheetInfo(), deleteStart, size);
			}		
			data = getCommunicator().readSheet(this, "FORMATTED_VALUE");
			assignmentRow = data.readRow(RowTypes.RUBRIC_NAME.ordinal());
		}
		return assignmentRow;
	}
	
	private void changeTextColors(Rubric rubric, boolean forceBlack) { 
		List<GoogleClassroomCommunicator.RowCol> rowCols = new ArrayList<GoogleClassroomCommunicator.RowCol>(); 
		for (int i = 0; i < rubric.getEntryCount(); i++) {
			RubricEntry entry = rubric.getEntry(i);
			if (entry.anyGradesModified()) {
				for (StudentData student : students) {
					RubricEntry.StudentScore score = entry.getStudentScore(student.getId());
					Color c = Color.BLACK;
					boolean bold = false;
					int studentRow = getStudentRow(student.getId(), 0);
					if (studentRow >= 0) {
						if (score != null && score.isModifiedByUser()) {
							c = Color.MAGENTA;
							bold = true;
						}
						if (forceBlack == true || bold == true) {
							rowCols.add(new GoogleClassroomCommunicator.RowCol(studentRow, 2, c, bold));
							rowCols.add(new GoogleClassroomCommunicator.RowCol(studentRow, 3, c, bold));
						}
					}

				}
			}
		}
		if (rowCols.size() != 0) {
			getCommunicator().changeTextColor(getTargetFile(), rowCols);
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
		List<StudentRow> studentRows = getStudentRowList(); 
		for (StudentRow studentRow : studentRows) {
			StudentData student = studentRow.getStudent();
			while (columnZero.size() <= studentRow.getRowNumber()) {
				columnZero.add("");
				columnOne.add("");
			}

			columnZero.set(studentRow.getRowNumber(), student.getName());
			columnOne.set(studentRow.getRowNumber(), student.getFirstName());
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
		List<StudentRow> studentRows = getStudentRowList(); 
		for (StudentRow studentRow : studentRows) {
			StudentData student = studentRow.getStudent();
			while (totalColumn.size() <= studentRow.getRowNumber()) {
				totalColumn.add("");
				onTimeColumn.add("");
			}
				
			int linkRow = gradeSyncer.getStudentRow(student.getId(), possibleStart);
			if (linkRow != -1) {
				linkRow++;
				totalColumn.set(studentRow.getRowNumber(), totalColumnLink + "$" + linkRow);
				onTimeColumn.set(studentRow.getRowNumber(), lateByLink + "$" + linkRow);
			}
			else {
				totalColumn.set(studentRow.getRowNumber(), "");
				onTimeColumn.set(studentRow.getRowNumber(), "name change or drop?");
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
