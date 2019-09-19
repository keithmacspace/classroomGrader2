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

public class GradeSyncer implements SheetAccessorInterface {
	private enum RowTypes {
		ASSIGNMENT("Due:"),
		RUBRIC_NAME("Rubric Name"),
		GRADED_BY("Graded By"),
		RUBRIC_VALUE("Rubric Value"),
		STUDENT_START("");
		RowTypes(String searchValue) {
			this.searchString = searchValue;			
		}

		public String getSearchString() {
			return searchString;
		}
		private String searchString;

		
	}
	private static final int SUBMIT_DATA_COLUMN = 2;
	private static final String SUBMIT_DATE_STRING = "Submit Date";
	private static final String TOTAL_STRING = "Total";
	private static final String NOTES_APPEND = ": Notes";
	private static final String LAST_NAME_COLUMN_KEY = "Last Name";
	private static final String FIRST_NAME_COLUMN_KEY = "First Name";
	private boolean updateBorders;
	private Rubric rubric;
	private List<StudentRow> studentRowList;	
	private GoogleSheetData targetFile;
	private String graderName;	
	private GoogleClassroomCommunicator communicator;
	private Map<RowTypes, Integer> rowLocations;
	private Map<String, Integer> columnLocations;
	private Set<String> notesGraderNames;
	private Map<String, String> gradedByColumns;
	private ClassroomData assignment;
	private Map<String, Map<String, String>> graderCommentsMap;
	private int maxRow;
	
	/**
	 * As soon as this constructor is called, we will load the current sheet & load the grades into the rubric.
	 * The load will never change current values in the rubric.
	 */
	public GradeSyncer(GoogleClassroomCommunicator communicator, Map<String, Map<String, String>> graderCommentsMap, GoogleSheetData targetFile, Rubric rubric, List<StudentData> students, String graderName) throws IOException {	
		this.communicator = communicator;
		this.targetFile = targetFile;
		this.rubric = rubric;
		this.graderName = graderName;
		this.graderCommentsMap = graderCommentsMap;
		rowLocations = new HashMap<RowTypes, Integer>();
		columnLocations = new HashMap<String, Integer>();
		notesGraderNames = new HashSet<String>();
		gradedByColumns = new HashMap<String, String>();			
		studentRowList  = new ArrayList<StudentRow>();
		LoadSheetData data = null;
		maxRow = 0;

		data = communicator.readSheet(this);
		if (data != null && data.isEmpty() == false) {
			// Leave whatever formatting is in place, we'll just load the data.
			updateBorders = false;
			processRows(data, students);
			processColumnNames(data);
			loadDataIntoDisplayStructures(data);
		}
		else {
			// There was no grade sheet out there, so when we save also change the formatting
			updateBorders = true;
			fillDefaultRows(students);
			fillDefaultColumns();
		}		
	}

	/**
	 * Add the date the student submitted their files
	 */
	public void setDate(String studentId,Date date) {
		for (StudentRow studentRow : studentRowList) {
			if (studentRow.getStudent() != null && studentRow.getStudent().getId().equals(studentId)) {
				studentRow.setDate(date);
			}
		}
	}
	
	/**
	 * If there is no sheet to load, create the default row layout
	 */
	private void fillDefaultRows(List<StudentData> students) {
		// By default, the rows are just in the order they appear in the enum
		for (RowTypes rowType : RowTypes.values()) {
			rowLocations.put(rowType, rowType.ordinal());
		}
		for (int i = 0; i < students.size(); i++) {
			StudentRow studentRow = new StudentRow(students.get(i), RowTypes.STUDENT_START.ordinal() + i);
			studentRowList.add(studentRow);			
		}
	}
	
	/**
	 * If we don't have a row called RUBRIC NAME (normally when we didn't load a sheet, but also
	 * if it is missing from a sheet) then fill in the columns with default locations.
	 */
	private void fillDefaultColumns() {
		int col = 0;
		addColumnLocation(LAST_NAME_COLUMN_KEY, col++);
		addColumnLocation(FIRST_NAME_COLUMN_KEY, col++);
		addColumnLocation(SUBMIT_DATE_STRING, col++);
		addColumnLocation(TOTAL_STRING, col++);	
		for (int i = 0; i < rubric.getEntryCount(); i++) {
			addColumnLocation(rubric.getEntry(i).getName(), col++);
		}
		String notesString = graderName + NOTES_APPEND;
		addColumnLocation(notesString, col++);
	}

	
	/**
	 * When we have an existing sheet, there is no guarantee that all the rows are where they
	 * should be.  So go through the sheet & find the rows.  If there isn't one of the required
	 * rows, then insert it.
	 */
	
	private void processRows(LoadSheetData data, List<StudentData> students) {
		// First find the student row because we will not search past that ever
		// Even if someone say, moves the assignment name into the middle of the students
		// we'll just ignore it and create a new assignment name at the top of the file.
		// But, if they reshuffle the first four rows, we'll stick with the reshuffling.
		int studentRow = findStudentStartRow(data, students);
		rowLocations.put(RowTypes.STUDENT_START, studentRow);
		findRow(data, RowTypes.ASSIGNMENT, studentRow);
		findRow(data, RowTypes.RUBRIC_NAME, studentRow);
		findRow(data, RowTypes.GRADED_BY, studentRow);
		findRow(data, RowTypes.RUBRIC_VALUE, studentRow);
		// Now, if we didn't find a row, insert it.
		possiblyInsert(data, RowTypes.ASSIGNMENT);
		if (possiblyInsert(data, RowTypes.RUBRIC_NAME)) {
			fillDefaultColumns();			
		}
		possiblyInsert(data, RowTypes.GRADED_BY);
		possiblyInsert(data, RowTypes.RUBRIC_VALUE);
	}

	/**
	 * Search each row, then search every column in that row for the keyword we are 
	 * looking for.  If we find it, then that is the row we are looking for.
	 */
	private void findRow(LoadSheetData data, RowTypes rowType, int stopSearch) {		
		for (int i = 0; i < stopSearch; i++) {
			// Never re-use a row.  This way, if the assignment name has one of the 
			// keyword strings, we still won't double book it.
			for (Integer intRow : rowLocations.values()) {				
				if (intRow != null && intRow.intValue() == i) {
					continue;
				}
			}
			List<Object> column = data.readRow(i);
			// Ignore empty rows
			if (column != null && column.size() > 0) {
				for (Object object : column) {
					if (object instanceof String) {
						// If we find an exact match (ignoring case), then assign that row to the 
						// type we are looking for
						String check = (String)object;
						if (check.equalsIgnoreCase(rowType.getSearchString())) {
							rowLocations.put(rowType, i);
							break;
						}
					}
				}
			}
		}		
	}
	
	/**
	 * If one or more of the header rows are missing, then we want to insert it.
	 * We'll always insert it into the location where we would have put it by default.
	 * This may mean we leave some useless blank rows, but so be it.
	 */
	public boolean possiblyInsert(LoadSheetData data, RowTypes rowType) {
		// If the row doesn't exist, add it. 
		if (rowLocations.containsKey(rowType) == false) {
			// First, check to see if we have a blank row at the default
			// position for this row, if we do, just consume that row
			int rowNum = rowType.ordinal();
			if (data.getNumRows() > rowNum) {
				List<Object> column = data.readRow(rowNum);
				// If we find an empty row at our desired spot, then just take
				// that over for our location
				if (column == null || column.size() == 0) {
					rowLocations.put(rowType, rowNum);
					return true;					
				}
			}
			insertRow(data, rowNum);
			rowLocations.put(rowType, rowNum);
			return true;
		}
		return false;
	}
	
	
	/**
	 * We are going to add a row to both the actual sheet and to our LoadSheetData
	 * When we do, move the positions stored in the StudentRow objects
	 * and the rowLocations map.
	 */
	private void insertRow(LoadSheetData data, int rowNum) {
		// OK, we don't have a blank row there, let's insert one.
		communicator.insertRow(targetFile, rowNum);
		// Insert a null row into our image of the data so later reads of the
		// rows get the correct data.
		data.rowInserted(rowNum);
		// Make sure we move the location of all other rows in the map
		for (RowTypes key : rowLocations.keySet()) {
			Integer currentRow = rowLocations.get(key);
			if (currentRow != null && currentRow > rowNum) {
				currentRow++;
				rowLocations.put(key, currentRow);
			}
		}
		for (StudentRow studentRow : studentRowList) {
			studentRow.incRowIfGreater(rowNum);
		}
	}

	
	/**
	 * We don't know what student will be at the top of the list, so search each row for
	 * any student.  The moment we find a student name, stop searching, that is our start
	 * student row.
	 */
	private int findStudentStartRow(LoadSheetData data, List<StudentData> students) {
		for (int i = 0; i < data.getNumRows(); i++) {
			List<Object> column = data.readRow(i);
			// Ignore empty rows or any rows that don't have enough space for a first and last name
			if (column != null && column.size() > 1) {
				// Now see if the row has a column entry that matches the a student's last name.
				for (int lastNameCol = 0; lastNameCol < column.size(); lastNameCol++) {
					Object possibleLastNameObject = column.get(lastNameCol);
					if (possibleLastNameObject != null && possibleLastNameObject instanceof String) {
						String possibleLastName = (String)possibleLastNameObject;					
						for (StudentData student : students) {
							if (processStudentsIfNameMatch(data, student, column, students, possibleLastName, i, lastNameCol)) {
								return i;
							}
						}						
					}
				}
			}
		}		
		// If we didn't find any students yet, then the student start row
		// is the end of the sheet.
		return data.getNumRows();
	}
	
	/**
	 * Called in the loop above, we hit a last name match on a student, see now if we'll hit a first name match
	 * on that same student.  If we do, then we will say that is the start of our student rows.  We will then
	 * go through and find the locations of all the other student rows. 
	 */
	boolean processStudentsIfNameMatch(LoadSheetData data, StudentData student, List<Object> column, List<StudentData> students, String possibleLastName, int startRow, int lastNameCol) {
		// We found a match for this student's last name
		if (possibleLastName.equalsIgnoreCase(student.getName())) {
			// Now see if we can find a match for first name also
			for (int firstNameCol = 0; firstNameCol < column.size(); firstNameCol++) {
				if (firstNameCol != lastNameCol) {
					Object possibleFirstNameObject = column.get(firstNameCol);	
					// We want to use != in this case, we don't care if the strings are the same, only that the objects are
					if (possibleFirstNameObject != null &&  possibleFirstNameObject instanceof String) {
						String possibleFirstName = (String)possibleFirstNameObject;
						if (possibleFirstName.equalsIgnoreCase(student.getFirstName())) {
							// OK, we've found where our names start, now read all the names
							processStudentRows(data, students, startRow, lastNameCol, firstNameCol);
							return true;
						}
					}
				}
			}								
		}
		return false;

	}
	



	/**
	 * When we find the student start row in the list above, process the students
	 * (store the row they are on).
	 * We will skip rows that don't match any student (like if a student
	 * drops, but we load a grade file from earlier in the year).
	 * If we are missing a student, we will insert them.
	 */	
	private void processStudentRows(LoadSheetData data, List<StudentData> students, int startRow, int lastNameCol, int firstNameCol) {
		Map<String, Integer> studentRows = createStudentLocationMap(data, students, startRow, lastNameCol, firstNameCol);
		List<StudentData> studentsToInsert = new ArrayList<StudentData>();
		for (StudentData student : students) {
			String key = student.getName() + student.getFirstName();
			key = key.toUpperCase();
			if (studentRows.containsKey(key)) {
				StudentRow studentRow = new StudentRow(student, studentRows.get(key));
				studentRowList.add(studentRow);				
			}
			else {
				studentsToInsert.add(student);
			}
			
		}
		int lastRow = data.getNumRows();
		
		// These students will not be in alphabetical order, but this is the super rare case (someone has
		// to join the class between the first time we grade the assignment and the next time we grade
		// that same assignment).
		// and the additional sorting was a pain.
		for (StudentData student : studentsToInsert) {
			StudentRow studentRow = new StudentRow(student, lastRow);
			studentRowList.add(studentRow);
			lastRow++;
		}		
	}
	
	/**
	 * Used in processStudentRows.  The easiest way to assign the rows to the StudentData is to first go through and
	 * create a map based on lastName + firstName.  Then we can use that map to find which students are still
	 * enrolled, and only track their rows. 
	 */
	private Map<String, Integer> createStudentLocationMap(LoadSheetData data, List<StudentData> students, int startRow, int lastNameCol, int firstNameCol) {
		// Start from where we found our first student match
		int rowNum = startRow;
		int maxCol = Math.max(lastNameCol,  firstNameCol); 
		Map<String, Integer> studentRows = new HashMap<String, Integer>();
		Map<String, Integer> flipNameRows = new HashMap<String, Integer>();
		// Go through every row starting from the first one where we found a student name and assign all the
		// create a map of of lastName + firstName = row.  We'll also create a firstName + lastName
		// in case of a stupidly rare condition (see below).
		for (int row = rowNum; row < data.getNumRows(); row++) {
			List<Object> rowValues = data.readRow(row); 
			if (rowValues != null && rowValues.size() > maxCol) {
				Object firstNameObject = rowValues.get(firstNameCol);
				Object lastNameObject = rowValues.get(lastNameCol);
				if (lastNameObject instanceof String && firstNameObject instanceof String) {
					String lastName = (String)lastNameObject;
					String firstName = (String)firstNameObject;
					String key = lastName + firstName;
					studentRows.put(key.toUpperCase(), row);
					// This is to take care of the unlikely case were we have two students, one named
					// DOE, JANE  and JANE, DOE.
					// When we searched in findStudentStartRow we found the line for DOE, JANE
					// but assigned it to JANE, DOE.  That means our firstNameCol and lastNameCol
					// should be flipped.
					// Just for that stupidly rare case, we'll also store things in reverse order.
					// if we find that we have more matches with the flip map, then we'll assign
					// based on the flip.
					String flipKey = firstName + lastName;
					flipNameRows.put(flipKey.toUpperCase(), row);
				}
			}
		}
		// OK, we now have a list of all the names and where they are.  Now determine
		// which map we should use, the normal one or the reverse one.		
		int normalMatchCount = 0;
		int flipMatchCount = 0; 
		for (StudentData student : students) {
			String key = student.getName() + student.getFirstName();
			key = key.toUpperCase();
			if (studentRows.containsKey(key)) {
				normalMatchCount++;			
			}
			// Note, we want to use the same key for both.  We are checking to see if 
			// what we thought were first names in the code above were actually last names.
			if (flipNameRows.containsKey(key)) {
				flipMatchCount++;
			}
		}
		if (flipMatchCount > normalMatchCount) {
			addColumnLocation(LAST_NAME_COLUMN_KEY, firstNameCol);
			addColumnLocation(FIRST_NAME_COLUMN_KEY, lastNameCol);
			return flipNameRows;
		}
		addColumnLocation(FIRST_NAME_COLUMN_KEY, firstNameCol);
		addColumnLocation(LAST_NAME_COLUMN_KEY, lastNameCol);
		return studentRows;
	}
	

	/**
	 * The insert & search of column names (based on rubric names) is always case insensitive. 
	 */
	private void addColumnLocation(String columnName, int location) {
		columnLocations.put(columnName.toUpperCase(), location);
	}
	
	private Integer getColumnLocation(String columnName) {
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
		int rowNum = rowLocations.get(RowTypes.RUBRIC_NAME);
		if (data.getNumRows() > rowNum) {
			List<Object> rubricNameRow = data.readRow(rowNum);
			int columnIndex = 0;
			if (rubricNameRow != null) {
				for (Object columnObject : rubricNameRow) {
					if (columnObject instanceof String) {						
						String column = (String)columnObject;
						addColumnLocation(column, columnIndex);
						int noteIndex = column.toUpperCase().indexOf(NOTES_APPEND.toUpperCase()); 
						if (noteIndex != -1) {
							String noteName = column.substring(0, noteIndex);					
							notesGraderNames.add(noteName);
						}
					}				
					columnIndex++;
				}
				int col = SUBMIT_DATA_COLUMN;
				possiblyInsertColumn(SUBMIT_DATE_STRING, col++);
				possiblyInsertColumn(TOTAL_STRING, col++);
				for (int i = 0; i < rubric.getEntryCount(); i++) {
					possiblyInsertColumn(rubric.getEntry(i).getName(), col++);
				}
				possiblyInsertColumn(graderName + NOTES_APPEND, computeMaxColumn());

			}
		}
		return -1;		
	}

	/**
	 * This is used when we are processing the rubric names.  If we find one is missing then we will insert
	 * a column to add the missing one. 
	 */
	private void possiblyInsertColumn(String columnName, int desiredSpot) {
		if (getColumnLocation(columnName) == null) {
			communicator.insertColumn(targetFile, desiredSpot);
			for (String key : columnLocations.keySet()) {
				Integer location = columnLocations.get(key);
				if (location >= desiredSpot) {
					columnLocations.put(key, location + 1);
				}
			}
			addColumnLocation(columnName, desiredSpot);			
		}
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
		for (StudentRow studentRow : studentRowList) {
			int rowNum = studentRow.getRowNumber();
			if (studentRow.getStudent() != null && rowNum < data.getNumRows()) {
				String studentID = studentRow.getStudent().getId();
				List<Object> points = data.readRow(rowNum);
				for (int i = 0; i < rubric.getEntryCount(); i++) {
					RubricEntry entry = rubric.getEntry(i);
					// Don't overwrite any values already in the rubric
					RubricEntry.StudentScore studentScore = entry.getStudentScore(studentID);
					if (studentScore == null || studentScore.isModifiedByUser() == false) {
						int index = getColumnLocation(entry.getName());
						if (points.size() > index) {
							Object object = points.get(index);
							if (object != null) {								
									entry.setStudentValue(studentID, object.toString());								
							}
						}						
					}
				}
				for (String grader : notesGraderNames) {
					String columnName = grader + NOTES_APPEND;
					Integer index = getColumnLocation(columnName); 
					if (index != null) {
						if (index < points.size()) {
							if (points.get(index) != null) {
								if (graderCommentsMap.containsKey(grader) == false) {
									graderCommentsMap.put(grader, new HashMap<String, String>());
								}
								Map<String, String> current = graderCommentsMap.get(grader);								
								// Don't overwrite comments added by the current user
								if (grader.equalsIgnoreCase(graderName) == false || current == null || current.containsKey(studentID) == false) { 					
									current.put(studentID, points.get(index).toString());
								}
							}							
						}
					}
				}					
			}
		}
	}
	
	/**
	 * This is the data in the header.  We load this in, not for display but so that we don't overwrite
	 * it later.  The first name in the graded by column stays the final name.
	 */
	private void loadGradedByColumns(LoadSheetData data) {		
		List<Object> gradedBy = data.readRow(rowLocations.get(RowTypes.GRADED_BY));
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
	 * Used in both the save & load flows
	 */
	@Override
	public GoogleSheetData getSheetInfo() {
		return targetFile;
	}

	
	/**
	 * This is where the save flow starts
	 */
	public void saveData(ClassroomData assignment) throws IOException {
		this.assignment = assignment;
		communicator.writeSheet(this);
		if (updateBorders) {
			changeBorderColors();
		}
		VerifySave verify = new VerifySave();
		verify.verifyWrite();
		rubric.clearModifiedFlag();
	}
	
	/**
	 * This will change the colors of the rows & columns and add the bold lines
	 */
	private void changeBorderColors() {
		communicator.setHeaderRows(targetFile, rowLocations.get(RowTypes.STUDENT_START), 2, maxRow);
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
		SaveSheetData saveData = new SaveSheetData(SaveSheetData.ValueType.USER_ENTERED, rubric.getName());
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
		Map<String, String> graderComments = graderCommentsMap.get(graderName);
		for (StudentRow studentRow : studentRowList) {			
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
				studentRowData.set(getColumnLocation(SUBMIT_DATE_STRING), SimpleUtils.formatDate(studentData.getDate()));
				Date date = studentRow.getDate();
				if (date != null) {
					studentRowData.set(getColumnLocation(SUBMIT_DATE_STRING), SimpleUtils.formatDate(date));
				}
				changedData |= fillStudentGradeColumns(studentID, studentRowData, row, currentRow);

				String note = graderComments.get(studentID);
				if (note != null) {					 
					Integer notesIndex = getColumnLocation(graderName + NOTES_APPEND);
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
		String sumString = "=SUM(" + startColumnName + (row  + 1) + ":" + endColumnName + (row + 1) + ")";
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

		String graderNotes = graderName + NOTES_APPEND;
		Integer noteLocation = getColumnLocation(graderNotes);
		if (noteLocation != null) {
			columnNameRow.set(noteLocation, graderNotes);
		}

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
		saveData.addOneRow(assignmentRow, rowLocations.get(RowTypes.ASSIGNMENT) + 1);
		saveData.addOneRow(gradedByRow, rowLocations.get(RowTypes.GRADED_BY) + 1);		
		saveData.addOneRow(rubricValueRow, rowLocations.get(RowTypes.RUBRIC_VALUE) + 1);
		saveData.addOneRow(columnNameRow, rowLocations.get(RowTypes.RUBRIC_NAME) + 1);
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
				LoadSheetData loadData = communicator.readSheet(this);
				saveData = new SaveSheetData(SaveSheetData.ValueType.USER_ENTERED, rubric.getName());			
				int maxColumn = computeMaxColumn();
				changedData = fillStudentRowsToSave(saveData, maxColumn, loadData); 
				if (changedData) {
					communicator.writeSheet(this);
				}
			}while (changedData == true);
		}


		@Override
		public GoogleSheetData getSheetInfo() {			
			return targetFile;
		}

		@Override
		public SaveSheetData getSheetSaveState() {
			// TODO Auto-generated method stub
			return saveData;
		}
		
	}
	
	
	protected class StudentRow {
		StudentData student;
		int rowNumber;	
		Date dateTurnedIn;
		public StudentRow(StudentData student, int rowNumber) {
			this.student = student;	
			this.rowNumber = rowNumber;
			dateTurnedIn = null;
		}
		
		int getRowNumber() {
			return rowNumber;
		}
		
		void incRowIfGreater(int rowNum) {
			if (rowNumber > rowNum) {
				rowNumber++;
			}
		}
		
		StudentData getStudent() {
			return student;
		}
		
		
		public void setDate( Date date) {
			if (date != null) {
				this.dateTurnedIn = date;
			}
		}
		
		public Date getDate() {
			return dateTurnedIn;
		}

	}

}
