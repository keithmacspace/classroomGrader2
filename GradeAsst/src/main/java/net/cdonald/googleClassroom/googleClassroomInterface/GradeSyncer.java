package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.StudentData;
import net.cdonald.googleClassroom.utils.SimpleUtils;

public class GradeSyncer extends GradeSheet {


	private boolean updateBorders;
	private Rubric rubric;
			
	private String graderName;	
	
	private Map<String, Integer> columnLocations;
	private Set<String> notesGraderNames; // This is for handling earlier forms where we had individual notes
	private Map<String, String> gradedByColumns;
	private ClassroomData assignment;
	private Map<String, String> graderCommentsMap;
	private Map<String, String> modifiedCommentsMap;
	private int maxRow;
	private RecenlyUpdated gradeSummary;	
	
	/**
	 * As soon as this constructor is called, we will load the current sheet & load the grades into the rubric.
	 * The load will never change current values in the rubric.
	 */
	public GradeSyncer(GoogleClassroomCommunicator communicator, Map<String, String> modifiedCommentsMap, GoogleSheetData targetFile, Rubric rubric, List<StudentData> students, String graderName) throws IOException {
		super(communicator, targetFile);
		gradeSummary = new RecenlyUpdated(this, communicator, targetFile, students);
		this.rubric = rubric;
		this.graderName = graderName;
		this.modifiedCommentsMap = modifiedCommentsMap;
		graderCommentsMap = new HashMap<String, String>();

		columnLocations = new HashMap<String, Integer>();
		notesGraderNames = new HashSet<String>();
		gradedByColumns = new HashMap<String, String>();			
		maxRow = 0;
		// Leave whatever formatting is in place, we'll just load the data.
		updateBorders = false;
		LoadSheetData data = readSheet(students);
		if (data != null && data.isEmpty() == false) {						
			processColumnNames(data);
			loadDataIntoDisplayStructures(data);
			rubric.setLoadedFromFile(true);
		}
		else if (rubric != null){
			// There was no grade sheet out there, so when we save also change the formatting
			updateBorders = true;
			fillDefaultRows(students);
			fillDefaultColumns();
		}		
	}
	
	public Integer getColumnLocation(String columnName) {
		return columnLocations.get(columnName.toUpperCase()); 
	}
	
	/**
	 * Reverse search the map for the location of a particular column 
	 */
	private String getColumnName(int location) {
		for (String name : columnLocations.keySet()) {
			if (location == columnLocations.get(name).intValue()) {
				return name;
			}
		}
		return null;
	}
	
	/**
	 *  Once we find the locations of all the rows, we need to find the locations of the columns.  The column locations are
	 *  based on the rubric names.  It is one of the reasons we need the rubric name and the sheet name to be the same. 
	 */	
	private int processColumnNames(LoadSheetData data) {
		int rowNum = getRowLocation(RowTypes.RUBRIC_NAME);
		if (data.getNumRows() > rowNum) {
			List<Object> rubricNameRow = data.readRow(rowNum);
			int columnIndex = 0;
			if (rubricNameRow != null) {
				for (Object columnObject : rubricNameRow) {
					if (columnObject instanceof String) {						
						String column = (String)columnObject;
						addColumnLocation(column, columnIndex);
						if (!column.equalsIgnoreCase(NOTES_COLUMN_NAME)) {
							int noteIndex = column.toUpperCase().indexOf(NOTES_APPEND.toUpperCase()); 
							if (noteIndex != -1) {
								String noteName = column.substring(0, noteIndex);					
								notesGraderNames.add(noteName);
							}
						}
					}				
					columnIndex++;
				}
				int col = SUBMIT_DATA_COLUMN;
				possiblyInsertColumn(data, SUBMIT_DATE_STRING, col++);
				possiblyInsertColumn(data, LATE_INFO_COLUMN_KEY, col++);
				possiblyInsertColumn(data, TOTAL_STRING, col++);
				for (int i = 0; i < rubric.getEntryCount(); i++) {
					RubricEntry entry = rubric.getEntry(i);
					if (entry != null) {
						possiblyInsertColumn(data, entry.getName(), col++);
					}
				}
				possiblyInsertColumn(data, NOTES_COLUMN_NAME, col++);

			}
		}
		return -1;		
	}

	/**
	 * This is used when we are processing the rubric names.  If we find one is missing then we will insert
	 * a column to add the missing one. 
	 */
	private void possiblyInsertColumn(LoadSheetData data, String columnName, int desiredSpot) {
		if (getColumnLocation(columnName) == null) {
			int rowNum = getRowLocation(RowTypes.RUBRIC_NAME);
			getCommunicator().insertColumn(getTargetFile(), desiredSpot, columnName, rowNum);
			for (String key : columnLocations.keySet()) {
				Integer location = columnLocations.get(key);
				if (location >= desiredSpot) {
					columnLocations.put(key, location + 1);
				}
			}
			data.colInserted(desiredSpot);
			addColumnLocation(columnName, desiredSpot);			
		}
	}
	
	

	/**
	 * The insert & search of column names (based on rubric names) is always case insensitive. 
	 */
	protected void addColumnLocation(String columnName, int location) {
		columnLocations.put(columnName.toUpperCase(), location);
	}


	public void setDate(String studentId,Date date) {
		gradeSummary.setDate(studentId, date);
		super.setDate(studentId, date);
	
	}
	/**
	 * If there is no sheet to load, create the default row layout
	 */
	private void fillDefaultRows(List<StudentData> students) {
		// By default, the rows are just in the order they appear in the enum
		for (RowTypes rowType : RowTypes.values()) {
			addRowLocation(rowType, rowType.ordinal());
		}
		fillDefaultStudentRows(RowTypes.STUDENT_START.ordinal(), students);
	}
	
	/**
	 * If we don't have a row called RUBRIC NAME (normally when we didn't load a sheet, but also
	 * if it is missing from a sheet) then fill in the columns with default locations.
	 */
	public void fillDefaultColumns() {
		int col = 0;
		addColumnLocation(LAST_NAME_COLUMN_KEY, col++);
		addColumnLocation(FIRST_NAME_COLUMN_KEY, col++);
		addColumnLocation(SUBMIT_DATE_STRING, col++);
		addColumnLocation(LATE_INFO_COLUMN_KEY, col++);
		addColumnLocation(TOTAL_STRING, col++);	
		for (int i = 0; i < rubric.getEntryCount(); i++) {
			addColumnLocation(rubric.getEntry(i).getName(), col++);
		}
		
		addColumnLocation(NOTES_COLUMN_NAME, col++);
	}




	

	



	
	
	/**
	 * Everything above mapped out where things are.  Now that we know where they are
	 * do the actual loading.
	 */	
	private void loadDataIntoDisplayStructures(LoadSheetData data) {
		loadStudentScoresAndNotes(data);
		loadGradedByColumns(data);
	}
	
	/**
	 * Go row by row for each student, and read their scores into the rubric and
	 * notes associated with that student into the graderCommentsMap.  Both the rubric
	 * and graderCommentsMap were passed in, so we are directly filling in the 
	 * data that will be display & modified by the user.
	 */
	private void loadStudentScoresAndNotes(LoadSheetData data) {
		for (StudentRow studentRow : getStudentRowList()) {
			int rowNum = studentRow.getRowNumber();
			if (studentRow.getStudent() != null && rowNum < data.getNumRows()) {
				String studentID = studentRow.getStudent().getId();
				List<Object> pointsOrNotes = data.readRow(rowNum);
				for (int i = 0; i < rubric.getEntryCount(); i++) {
					RubricEntry entry = rubric.getEntry(i);
					// Don't overwrite any values already in the rubric
					RubricEntry.StudentScore studentScore = entry.getStudentScore(studentID);
					if (studentScore == null || studentScore.isModifiedByUser() == false) {
						int index = getColumnLocation(entry.getName());
						if (pointsOrNotes.size() > index) {
							Object object = pointsOrNotes.get(index);
							if (object != null) {								
									entry.setStudentValue(studentID, object.toString());								
							}
						}						
					}
				}
				boolean addedDefaultNotes = mergeComments(NOTES_COLUMN_NAME, pointsOrNotes, studentID);
				if (addedDefaultNotes == false) {								
					// Handle the earlier form of notes
					for (String grader : notesGraderNames) {
						String columnName = grader + NOTES_APPEND;
						mergeComments(columnName, pointsOrNotes, studentID);						
					}					
				}
			}
		}
	}
	
	private boolean mergeComments(String columnName, List<Object> pointsOrNotes, String studentID) {
		Integer index = getColumnLocation(columnName);
		boolean addedNotes = false;
		if (index != null) {
			if (index < pointsOrNotes.size()) {
				if (pointsOrNotes.get(index) != null) {
					// Do not overwrite modified comments
					if (modifiedCommentsMap == null || modifiedCommentsMap.containsKey(studentID) == false) {
						String comment = pointsOrNotes.get(index).toString();
						if (comment.length() != 0) {
							addedNotes = true;
						}
						if (graderCommentsMap.containsKey(studentID)) {
							comment += graderCommentsMap.get(studentID);
						}
						graderCommentsMap.put(studentID, comment);
					}
				}							
			}
		}
		return addedNotes;
	}
	
	/**
	 * This is the data in the header.  We load this in, not for display but so that we don't overwrite
	 * it later.  The first name in the graded by column stays the final name.
	 */
	private void loadGradedByColumns(LoadSheetData data) {		
		List<Object> gradedBy = data.readRow(getRowLocation(RowTypes.GRADED_BY));
		int col = 0;
		if (gradedBy != null) {
			for (Object object : gradedBy) {
				if (object != null) {
					String name = object.toString();
					if (name.length() > 0) {
						String colName = getColumnName(col);
						if (colName != null)
						gradedByColumns.put(colName, name);
					}
				}
				col++;
			}
		}
	}
	


	
	/**
	 * This is where the save flow starts
	 * @param modifiedNotes 
	 */
	public void saveData(ClassroomData assignment) throws IOException {		
		this.assignment = assignment;
		gradeSummary.syncData();
		getCommunicator().writeSheet(this);
		if (updateBorders) {
			changeBorderColors(getRowLocation(RowTypes.STUDENT_START), maxRow);
		}
		VerifySave verify = new VerifySave();
		verify.verifyWrite();
		rubric.clearStudentScoreModifiedFlag();
	}
	

	/**
	 * Compute the max column based on the entries in our columnLocations map
	 */
	private int computeMaxColumn() {
		int maxColumn = 0;
		for (Integer colNum : columnLocations.values()) {
			maxColumn = Math.max(colNum, maxColumn);
		}
		maxColumn++;
		return maxColumn;
	}

	/**
	 * This will be called by GoogleClassroom when we call writeSheet
	 */
	@Override
	public SaveSheetData getSheetSaveState() {
		SaveSheetData saveData = new SaveSheetData(SaveSheetData.ValueType.USER_ENTERED, rubric.getName(), false);
		int maxColumn = computeMaxColumn();
		fillStudentRowsToSave(saveData, maxColumn, null);
		// Fill these after the student rows so that the graded by is correct
		fillDefaultRowsToSave(saveData, maxColumn);
		maxRow = saveData.getMaxRow();
		return saveData;
	}
	
	
	/**
	 * 
	 * Fill the save state data with the scores & notes for individual students
	 */
	private boolean fillStudentRowsToSave(SaveSheetData saveData, int maxColumn, LoadSheetData loadData) {
		boolean changedData = false;		
		for (StudentRow studentRow : getStudentRowList()) {			
			StudentData studentData = studentRow.getStudent();
			if (studentData != null) {
				int row = studentRow.getRowNumber();
				String studentID = studentData.getId();
				List<Object> studentRowData = new ArrayList<Object>(maxColumn);
				List<Object> currentRow = null;
				if (loadData != null && loadData.getNumRows() > row) {
					currentRow = loadData.readRow(row);
				}
				
				for (int i = 0; i < maxColumn; i++) {
					studentRowData.add(null);
				}
				studentRowData.set(getColumnLocation(LAST_NAME_COLUMN_KEY), studentData.getName());
				studentRowData.set(getColumnLocation(FIRST_NAME_COLUMN_KEY), studentData.getFirstName());
				Date date = studentRow.getDate();
				if (date != null) {					
					studentRowData.set(getColumnLocation(SUBMIT_DATE_STRING), SimpleUtils.formatDate(date));
					if (assignment != null) {
						studentRowData.set(getColumnLocation(LATE_INFO_COLUMN_KEY), SimpleUtils.formatLate(date, assignment.getDate()));
					}
				}
				changedData |= fillStudentGradeColumns(studentID, studentRowData, row, currentRow);

				String note = null;
				if (modifiedCommentsMap != null) {
					note = modifiedCommentsMap.get(studentID);
				}
				if (note != null) {					 
					Integer notesIndex = getColumnLocation(NOTES_COLUMN_NAME);
					if(notesIndex == null) {
						studentRowData.add(note);
					}
					else {
						studentRowData.set(notesIndex, note);
					}
				}
				saveData.addOneRow(studentRowData, row + 1);
			}
		}
		return changedData;
	}
	
	
	/**
	 * Go through the rubric entries for a single student, and fill in their scores & notes
	 * returns true if we changed anything. 
	 */
	private boolean fillStudentGradeColumns(String studentID, List<Object> studentRowData, int row, List<Object> currentRow) {
		boolean changedData = false;
		int minRubricColumn = columnLocations.size() + getColumnLocation(TOTAL_STRING);
		int maxRubricColumn = 0;
		for (int i = 0; i < rubric.getEntryCount(); i++) {
			RubricEntry entry = rubric.getEntry(i);
			Integer location = getColumnLocation(entry.getName()); 
			if (location == null) {
				DebugLogDialog.appendln("Rubric somehow missing: " + entry.getName());
			}
			minRubricColumn = Math.min(location, minRubricColumn);
			maxRubricColumn = Math.max(location, maxRubricColumn);
			RubricEntry.StudentScore studentScore = entry.getStudentScore(studentID);
			if (studentScore != null && studentScore.isModifiedByUser()) {
				if (gradedByColumns.containsKey(entry.getName()) == false) {
					gradedByColumns.put(entry.getName(), graderName);
				}				
				changedData |= conditionallyAddToSave(studentRowData, location, studentScore, currentRow);
			}
		}
		String startColumnName = GoogleClassroomCommunicator.getColumnName(minRubricColumn);
		String endColumnName = GoogleClassroomCommunicator.getColumnName(maxRubricColumn);
		String sumString = "=ROUND(SUM(" + startColumnName + (row  + 1) + ":" + endColumnName + (row + 1) + "))";
		studentRowData.set(getColumnLocation(TOTAL_STRING), sumString);
		return changedData;
	}

	/**
	 * The first time we call this, it will be from getSheetSaveState->fillStudentRowsToSave
	 * in that flow, currentRow will be null.  That is the flow where we are saving the data out
	 * to the file.
	 * 
	 * Then in the verify flow, we will check if the data is correct, and only write data that does
	 * not match (this is basically verifying the write).
	 * 
	 */
	private boolean conditionallyAddToSave(List<Object> studentRowData, int col, RubricEntry.StudentScore valueToSave, List<Object> currentRow) {
		if (valueToSave == null || valueToSave.getScore() == null) {
			return false;
		}
		if (valueToSave.isModifiedByUser() == false) {
			return false;
		}
		boolean writeValue = false;
		if (currentRow == null || currentRow.size() <= col) {
			writeValue = true;
		}
		else {
			Object currentObject = currentRow.get(col);
			if (currentObject == null) {
				writeValue = true;			
			}
			else {		
				Double test = null;
				if (currentObject instanceof Double) {
					test = (Double)currentObject;
				}
				else {
					try {
						test = Double.parseDouble(currentObject.toString());
					}
					catch(NumberFormatException e) {						
					}
				}
				writeValue = (test == null || !test.equals(valueToSave.getScore()));
			}
		}
		if (writeValue) {
			studentRowData.set(col,  valueToSave.getScore());
		}
		return writeValue;
	}


	
	/**
	 * 
	 * Add the default rows - assignment name, rubric name etc. to the save state
	 */
	private void fillDefaultRowsToSave(SaveSheetData saveData, int maxColumns) {
		List<Object> assignmentRow = new ArrayList<Object>();
		List<Object> gradedByRow = new ArrayList<Object>(maxColumns);
		List<Object> columnNameRow = new ArrayList<Object>(maxColumns);
		List<Object> rubricValueRow = new ArrayList<Object>(maxColumns);
		for (int col = 0; col < maxColumns; col++) {
			gradedByRow.add(null);
			columnNameRow.add(null);
			rubricValueRow.add(null);
		}
		if (assignment != null && assignment.isEmpty() == false) {
			assignmentRow.add("Assignment");
			assignmentRow.add(RowTypes.ASSIGNMENT.getSearchString());
			assignmentRow.add(SimpleUtils.formatDate(assignment.getDate()));
			assignmentRow.add("Name:");
			assignmentRow.add(assignment.getName());
		}

		gradedByRow.set(0, RowTypes.GRADED_BY.getSearchString());
		rubricValueRow.set(0, RowTypes.RUBRIC_VALUE.getSearchString());		
		columnNameRow.set(0, RowTypes.RUBRIC_NAME.getSearchString());
		columnNameRow.set(getColumnLocation(TOTAL_STRING), TOTAL_STRING);
		columnNameRow.set(getColumnLocation(SUBMIT_DATE_STRING), SUBMIT_DATE_STRING);
		columnNameRow.set(getColumnLocation(LATE_INFO_COLUMN_KEY), LATE_INFO_COLUMN_KEY);
		columnNameRow.set(getColumnLocation(NOTES_COLUMN_NAME), NOTES_COLUMN_NAME);

		for (int i = 0; i < rubric.getEntryCount(); i++) {
			RubricEntry entry = rubric.getEntry(i);
			int col = getColumnLocation(entry.getName());
			String gradedBy = gradedByColumns.get(entry.getName());
			if (gradedBy != null) {
				gradedByRow.set(col, gradedBy);
			}
			columnNameRow.set(col, entry.getName());
			rubricValueRow.set(col, (Integer)entry.getValue());			
		}
		saveData.addOneRow(assignmentRow, getRowLocation(RowTypes.ASSIGNMENT) + 1);
		saveData.addOneRow(gradedByRow, getRowLocation(RowTypes.GRADED_BY) + 1);		
		saveData.addOneRow(rubricValueRow, getRowLocation(RowTypes.RUBRIC_VALUE) + 1);
		saveData.addOneRow(columnNameRow, getRowLocation(RowTypes.RUBRIC_NAME) + 1);
	}
	
	
	/**
	 * 
	 * Every so often the sheet writer glitches, so verify the write until all the data matches
	 *
	 */
	protected class VerifySave implements SheetAccessorInterface {
		SaveSheetData saveData;
		private void verifyWrite() throws IOException {
			boolean changedData = false;
			
			do {
				changedData = false;			
				LoadSheetData loadData = getCommunicator().readSheet(this);
				saveData = new SaveSheetData(SaveSheetData.ValueType.USER_ENTERED, rubric.getName(), false);			
				int maxColumn = computeMaxColumn();
				changedData = fillStudentRowsToSave(saveData, maxColumn, loadData); 
				if (changedData) {
					getCommunicator().writeSheet(this);
				}
			}while (changedData == true);
		}


		@Override
		public GoogleSheetData getSheetInfo() {			
			return getTargetFile();
		}

		@Override
		public SaveSheetData getSheetSaveState() {
			// TODO Auto-generated method stub
			return saveData;
		}
		
	}
	


	public Map<String, String> getComments() {
		return graderCommentsMap;
	}

}
