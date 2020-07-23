package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.gui.StudentSheetColumns;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.StudentData;

public abstract class GradeSheet extends StudentSheetColumns {
	public enum RowTypes {
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
	public static final int SUBMIT_DATA_COLUMN = 2;
	public static final String SUBMIT_DATE_STRING = "Submit Date";
	public static final String LATE_INFO_COLUMN_KEY = "Late By";
	public static final String TOTAL_STRING = "Total";
	public static final String NOTES_APPEND = ": Notes";
	public static final String NOTES_COLUMN_NAME = "Grader" + NOTES_APPEND; // Ealier form broke up notes by grader, but that didn't work as cleanly.  
	public static final String LAST_NAME_COLUMN_KEY = "Last Name";
	public static final String FIRST_NAME_COLUMN_KEY = "First Name";
	
	private Map<RowTypes, Integer> rowLocations;
	
	
	
	public GradeSheet(GoogleClassroomCommunicator communicator, GoogleSheetData targetFile) {
		super(communicator, targetFile);
		rowLocations = new HashMap<RowTypes, Integer>();

		
	}
	
	public LoadSheetData readSheet(List<StudentData> students) throws IOException {
		LoadSheetData data = getCommunicator().readSheet(this);
		if (data != null) {
			processRows(data, students);
		}
		return data;
	}

	
	abstract public SaveSheetData getSheetSaveState();
	
	abstract public void fillDefaultColumns();
	abstract protected void addColumnLocation(String columnName, int location);

	protected int getRowLocation(RowTypes rowType) {
		return rowLocations.get(rowType);
	}
	
	protected void addRowLocation(RowTypes rowType, int rowNum) {
		rowLocations.put(rowType, rowNum);
	}
	
	protected Map<RowTypes, Integer> getRowLocations() {
		return rowLocations;
	}


	/**
	 * When we have an existing sheet, there is no guarantee that all the rows are where they
	 * should be.  So go through the sheet & find the rows.  If there isn't one of the required
	 * rows, then insert it.
	 */	
	protected void processRows(LoadSheetData data, List<StudentData> students) {
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
			insertRow(data, rowNum, rowType);
			rowLocations.put(rowType, rowNum);
			return true;
		}
		return false;
	}
	
	protected void setFirstLastNameColumns(int firstNameCol, int lastNameCol) {
		addColumnLocation(FIRST_NAME_COLUMN_KEY, firstNameCol);
		addColumnLocation(LAST_NAME_COLUMN_KEY, lastNameCol);
	}
	
	
	/**
	 * We are going to add a row to both the actual sheet and to our LoadSheetData
	 * When we do, move the positions stored in the StudentRow objects
	 * and the rowLocations map.
	 */
	protected void insertRow(LoadSheetData data, int rowNum, RowTypes rowType) {
		// OK, we don't have a blank row there, let's insert one.
		getCommunicator().insertRowPlusHeader(getTargetFile(), rowNum, rowType.getSearchString(), 0);
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
		for (StudentRow studentRow : getStudentRowList()) {
			studentRow.incRowIfGreaterOrEqual(rowNum);
		}
	}

	

	




	
	

	

	

	
	

}
