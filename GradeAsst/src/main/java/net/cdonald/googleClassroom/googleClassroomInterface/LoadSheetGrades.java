package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.StudentData;

public class LoadSheetGrades extends GradeSheetAccessor{
	private Map<String, Map<String, String>> graderCommentsMap;
	private Map<String, String> gradedByMap;
	public LoadSheetGrades(GoogleSheetData targetFile, Rubric rubricParam, List<StudentData> students, String graderName, Map<String, Map<String, String>> graderCommentsMap)  {
		super(targetFile, rubricParam, students, graderName);
		this.graderCommentsMap = graderCommentsMap;
		gradedByMap = new HashMap<String, String>();
	}
	
	public void loadData(GoogleClassroomCommunicator communicator, boolean keepCurrentRubricValues) throws IOException {
		setLoading(true);
		LoadSheetData data = communicator.readSheet(this);
		if (data != null) {
			int nameRow = processColumnNames(data);
			// If namerow comes back -1, then we are starting right from the starting line
			nameRow++;		
			processNameData(nameRow, data, keepCurrentRubricValues);
		}
		setLoading(false);
	}
	
	public String getGradedBy(String columnName) {
		return gradedByMap.get(columnName.toUpperCase());
	}

	
	private int processColumnNames(LoadSheetData data) {
		
		List<Object> gradedRow = null;
		for (int i = 0; i < data.getNumRows(); i++) {
			List<Object> row = data.readRow(i);
			int matchCount = 0;
			for (Object columnObject : row) {
				if (columnObject instanceof String) {
					String column = (String)columnObject;					
					if (column.equalsIgnoreCase(StudentListInfo.defaultColumnNames[StudentListInfo.LAST_NAME_COLUMN])) {
						matchCount++;
					}
					if (column.equalsIgnoreCase(StudentListInfo.defaultColumnNames[StudentListInfo.FIRST_NAME_COLUMN])) {
						matchCount++;
					}
					if (column.equalsIgnoreCase("Graded By")) {
						gradedRow = row;
					}
					if (matchCount == 2) {
						break;
					}
				}
			}
			if (matchCount == 2) { 
				int columnIndex = 0;
				for (Object columnObject : row) {
					boolean insertBlank = true;
					if (columnObject instanceof String) {						
						String column = (String)columnObject;
						Object gradedBy = null;
						if (gradedRow.size() > columnIndex) {
							gradedBy = gradedRow.get(columnIndex);
						}
						if (gradedBy != null) {
							gradedByMap.put(column.toUpperCase(), gradedBy.toString());
						}
						else {
							gradedByMap.put(column.toUpperCase(), null);
						}

						if (column.length() > 1) {
							insertBlank = false;
							int currentIndex = getColumnLocation(column);
							if (currentIndex == -1) {
								insertColumn(columnIndex, column);
							}
							else if (currentIndex != columnIndex) {
								moveColumn(currentIndex, columnIndex, column);
							}
						}
					}
					if (insertBlank) {
						insertColumn(columnIndex, "no header" + columnIndex);
					}
					columnIndex++;
				}
				return i;				
			}
		}
		return -1;		
	}
	
	
	private void processNameData(int nameRow, LoadSheetData data, boolean keepCurrentRubricValues) {
		int lastNameColumn = getColumnLocation(StudentListInfo.defaultColumnNames[StudentListInfo.LAST_NAME_COLUMN]);
		int firstNameColumn = getColumnLocation(StudentListInfo.defaultColumnNames[StudentListInfo.FIRST_NAME_COLUMN]);
		Map<String, Integer> foundCountMap = new HashMap<String, Integer>();
		Map<String, RubricEntry> rubricEntryMap = new HashMap<String, RubricEntry>();
		for (int i = nameRow; i < data.getNumRows(); i++) {
			List<Object> row = data.readRow(i);
			String lastName = (String)row.get(lastNameColumn);
			String firstName = (String)row.get(firstNameColumn);
			String key = getNameKey(lastName, firstName);
			if (foundCountMap.containsKey(key) == false) {
				foundCountMap.put(key, 0);
			}
			int foundCount = foundCountMap.get(key);
			foundCountMap.put(key, foundCount+1);
			StudentRow studentRow = getStudentRow(lastName, firstName, foundCount);
			String studentID = studentRow.getStudent().getId();
			for (int col = 0; col < row.size(); col++) {
				if (col != firstNameColumn && col != lastNameColumn) {
					String columnName = getColumnName(col);
					studentRow.addColumn(columnName, row.get(col));
					addDataToStudent(studentID, columnName, row.get(col), rubricEntryMap, keepCurrentRubricValues);
				}				
			}
		}
	}
	
	private void addDataToStudent(String studentID, String columnName, Object info, Map<String, RubricEntry> rubricEntryMap, boolean keepCurrentRubricValues) {
		int commentIndex = columnName.toUpperCase().indexOf(getNoNameNotesHeader().toUpperCase());		
		if (commentIndex != -1) {
			if (info != null) {		
				String grader = columnName.substring(0, commentIndex);				
				grader.replaceAll("\\s", "");		
				if (graderCommentsMap.containsKey(grader) == false) {
					graderCommentsMap.put(grader, new HashMap<String, String>());
				}
				Map<String, String> current = graderCommentsMap.get(grader);

				// Don't overwrite comments added by the current user
				if (grader.equalsIgnoreCase(getGraderName()) == false || current == null || current.containsKey(studentID) == false) { 					
					current.put(studentID, info.toString());
				}
			}
			return;
		}
		Rubric rubric = getRubric();
		if (rubricEntryMap.containsKey(columnName) == false) {
			boolean found = false;
			for (int i = 0; i < rubric.getEntryCount(); i++) {
				RubricEntry entry = rubric.getEntry(i);
				if (entry.getName().equalsIgnoreCase(columnName)) {
					rubricEntryMap.put(columnName, entry);
					found = true;
					break;
				}
			}
			if (found == false) {
				rubricEntryMap.put(columnName, null);
			}
		}
		RubricEntry entry = rubricEntryMap.get(columnName);
		if (entry != null && info != null && info.toString().length() > 0) {
			if (keepCurrentRubricValues == false || entry.getStudentDoubleValue(studentID) == null) {
				entry.setStudentValue(studentID, info.toString());
			}
		}							
	}
	
	@Override
	public SaveSheetData getSheetSaveState() {
		// TODO Auto-generated method stub
		return null;
	}

}
