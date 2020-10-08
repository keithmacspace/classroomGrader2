package net.cdonald.googleClassroom.gui;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.googleClassroomInterface.GoogleClassroomCommunicator;
import net.cdonald.googleClassroom.googleClassroomInterface.LoadSheetData;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetAccessorInterface;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.StudentData;

public abstract class StudentSheetColumns implements SheetAccessorInterface {
	private List<StudentRow> studentRowList;
	private GoogleSheetData targetFile;
	private GoogleClassroomCommunicator communicator;


	public StudentSheetColumns(GoogleClassroomCommunicator communicator, GoogleSheetData targetFile) {
		this.communicator = communicator;
		this.targetFile = targetFile;
		studentRowList  = new ArrayList<StudentRow>();

	}
	

	/**
	 * Used in both the save & load flows
	 */
	@Override
	public GoogleSheetData getSheetInfo() {
		return targetFile;
	}
	
	protected void loadStudentInfo(int startIndex) {
		
	}
	
	protected GoogleSheetData getTargetFile() {
		return targetFile;
	}

	protected GoogleClassroomCommunicator getCommunicator() {
		return communicator;
	}
	
	protected List<StudentRow> getStudentRowList() {
		return studentRowList;
	}
	
	public int getStudentRow(String studentID, int possibleStart) {

		if (studentRowList.size() > possibleStart) {
			if (studentRowList.get(possibleStart).student.getId().equals(studentID)) {
				return studentRowList.get(possibleStart).getRowNumber();
			}
		}
		for (StudentRow studentRow : studentRowList) {
			if (studentRow.student.getId().equals(studentID)) {
				return studentRow.getRowNumber();
			}
		}
		return -1;
	}
	
	protected void fillDefaultStudentRows(int startRow, List<StudentData> students) {
		for (int i = 0; i < students.size(); i++) {
			StudentRow studentRow = new StudentRow(students.get(i), startRow + i);
			studentRowList.add(studentRow);			
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
	
	protected void adjustStudentRows(int diff) {
		for (StudentRow studentRow : studentRowList) {
			studentRow.rowNumber += diff;
		}
	}
	
	/**
	 * This will change the colors of the rows & columns and add the bold lines
	 */
	protected void changeBorderColors(int startOfStudents, int maxRow) {
		getCommunicator().setHeaderRows(getTargetFile(), startOfStudents, 2, maxRow);
	}
	
	/**
	 * We don't know what student will be at the top of the list, so search each row for
	 * any student.  The moment we find a student name, stop searching, that is our start
	 * student row.
	 */
	public int findStudentStartRow(LoadSheetData data, List<StudentData> students) {
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
	public  boolean processStudentsIfNameMatch(LoadSheetData data, StudentData student, List<Object> column, List<StudentData> students, String possibleLastName, int startRow, int lastNameCol) {
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
				addStudentToList(studentRow);
				studentRows.remove(key);
			}
			else {
				studentsToInsert.add(student);
			}			
		}
		int maxCol = Math.max(lastNameCol,  firstNameCol);
		// We may have had a name change, or a drop, but we don't want to delete the row right now
		// Just have duplicates. In those cases, we just want to add the blank names as student rows
		for (Integer missingRow : studentRows.values()) {
			List<Object> rowValues = data.readRow(missingRow); 
			if (rowValues != null && rowValues.size() > maxCol) {
				Object firstNameObject = rowValues.get(firstNameCol);
				Object lastNameObject = rowValues.get(lastNameCol);
				if (lastNameObject instanceof String && firstNameObject instanceof String) {
					String lastName = (String)lastNameObject;
					String firstName = (String)firstNameObject;
					StudentData empty = new StudentData(firstName, lastName, "FAKE" + missingRow, null);
					StudentRow studentRow = new StudentRow(empty, missingRow);
					addStudentToList(studentRow);
				}
			}
		}
		int lastRow = data.getNumRows();
		
		// These students will not be in alphabetical order, but this is the super rare case (someone has
		// to join the class between the first time we grade the assignment and the next time we grade
		// that same assignment).
		// and the additional sorting was a pain.
		for (StudentData student : studentsToInsert) {
			StudentRow studentRow = new StudentRow(student, lastRow);
			addStudentToList(studentRow);
			lastRow++;
		}		
	}
	
	private void addStudentToList(StudentRow student) {
		boolean found = false;
		String id = student.getStudent().getId();
		for (StudentRow studentRow : studentRowList) {
			if (studentRow.getStudent().getId().equals(id)) {
				studentRow.rowNumber = student.rowNumber;
				found = true;
				break;
			}
		}
		if (!found) {
			studentRowList.add(student);
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
			setFirstLastNameColumns(lastNameCol, firstNameCol);
			return flipNameRows;
		}
		setFirstLastNameColumns(firstNameCol, lastNameCol);
		return studentRows;
	}
	
	abstract protected void setFirstLastNameColumns(int firstNameCol, int lastNameCol);
	
	protected class StudentRow {
		StudentData student;
		int rowNumber;	
		Date dateTurnedIn;
		public StudentRow(StudentData student, int rowNumber) {
			this.student = student;	
			this.rowNumber = rowNumber;
			dateTurnedIn = null;
		}
		
		public int getRowNumber() {
			return rowNumber;
		}
		
		public void incRowIfGreaterOrEqual(int rowNum) {
			if (rowNumber >= rowNum) {
				rowNumber++;
			}
		}
		
		public StudentData getStudent() {
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
