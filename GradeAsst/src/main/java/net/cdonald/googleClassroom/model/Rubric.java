package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import net.cdonald.googleClassroom.googleClassroomInterface.LoadSheetData;
import net.cdonald.googleClassroom.googleClassroomInterface.SaveSheetData;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetAccessorInterface;
import net.cdonald.googleClassroom.gui.DataUpdateListener;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;

public class Rubric implements SheetAccessorInterface {
	private GoogleSheetData sheetData;
	private List<RubricEntry> entries;
	private boolean inModifiedState;
	private Map<String, FileData> fileDataMap;
	private List<FileData> goldenSource;
	private static final String GOLDEN_SOURCE_LABEL = "Golden Source Files";
	

	public Rubric(GoogleSheetData sheetData) {
		super();
		this.sheetData = sheetData;
		entries = new ArrayList<RubricEntry>();
		fileDataMap = new HashMap<String, FileData>();
		goldenSource = new ArrayList<FileData>();
	}
	
	public Rubric(Rubric other) {		
		sheetData = new GoogleSheetData(other.sheetData);
		entries = new ArrayList<RubricEntry>();
		for (RubricEntry otherEntry : other.entries) {
			entries.add(new RubricEntry(otherEntry));
		}
		inModifiedState = other.inModifiedState;
		fileDataMap = new HashMap<String, FileData>();
		for (String key : other.fileDataMap.keySet()) {
			fileDataMap.put(key, new FileData(other.fileDataMap.get(key)));
		}
		goldenSource = new ArrayList<FileData>();
		setGoldenSource(other.goldenSource);

	}
		
	
	// This form is used when we are creating a new rubric from scratch
	public Rubric() {
		inModifiedState = true;
		entries = new ArrayList<RubricEntry>();
		fileDataMap = new HashMap<String, FileData>();
		goldenSource = new ArrayList<FileData>();
	}


	public Map<String, FileData> getFileDataMap() {
		return fileDataMap;
	}
	
	public List<FileData> getGoldenSource() {
		return goldenSource;
	}
	
	
	public void setGoldenSource(List<FileData> fileDataList) {
		goldenSource.clear();
		if (fileDataList != null) {
			for (FileData fileData : fileDataList) {
				goldenSource.add(fileData);
			}
		}
	}
	
	public void addFileData(FileData fileData) {
		fileDataMap.put(fileData.getName(), fileData);
		for (RubricEntry entry : entries) {
			entry.removeFileData(fileData);
		}
	}
	
	public void removeFileData(FileData fileData) {
		fileDataMap.remove(fileData.getName());
	}
	
	public double getTotalValue(String id) {
		double value = 0.0;
		for (RubricEntry entry : entries) {
			Double studentValue = entry.getStudentDoubleValue(id);
			if (studentValue != null) {
				value += studentValue;
			}
		}
		value *= 100;
		value = (int)value;
		value /= 100;
		return value;
		
	}

	public String getTotalString(String id) {
		return "" + getTotalValue(id);
	}
	
	
	
	public List<String> getRubricTabs() {
		List<String> rubricTabs = new ArrayList<String>();
		for (RubricEntry entry : entries) {
			entry.addRubricTab(rubricTabs);
		}
		return rubricTabs;
	}
	
	
	public String getName() {
		return sheetData.getName();
	}
	
	public boolean isEmpty() {
		return entries.isEmpty();
	}
	
	public void addNewEntry() {
		entries.add(new RubricEntry());
	}
	
	public void addNewEntry(int index) {
		entries.add(index, new RubricEntry());
	}
	
	public void modifyEntry(RubricEntry entry) {
		boolean modified = false;
		for (int i = 0; (i < entries.size() && modified == false); i++) {
			if (entries.get(i).getName().equalsIgnoreCase(entry.getName())) {
				entries.set(i, entry);
				modified = true;
			}
		}
		if (modified == false) {
			entries.add(entry);
		}
	}
	
	public void removeEntry(String name) {
		for (int i = 0; i < entries.size(); i++) {
			RubricEntry entry = entries.get(i);
			if (entry.getName().compareToIgnoreCase(name) == 0){
				entries.remove(i);
				break;
			}
		}
	}

	public void removeEntry(int index) {
		if (index >= 0 && index <= entries.size()) {
			entries.remove(index);
		}
	}
	
	public void swapEntries(int index, int otherIndex) {
		if (index >= 0 && index < entries.size() && otherIndex >= 0 && otherIndex < entries.size()) {
			RubricEntry temp = entries.set(index, entries.get(otherIndex));
			entries.set(otherIndex, temp);
		}
	}
	
	public boolean isInRubric(String name) {
		for (RubricEntry entry : entries) {
			if (entry.getName().equals(name)) {
				return true;
			}
		}
		return false;
	}
	@Override
	public String toString() {
		return sheetData.getName();
	}

	
	public int getEntryCount() {
		if (entries != null) {
			if (inModifiedState) {
				for (int i = entries.size() - 1; i >= 0; i--) {
					RubricEntry entry = entries.get(i);
					if (entry.getName() != null && entry.getName().length() != 0) {
						return i + 1;
					}
				}
			}
			else {
				return entries.size();
			}
		}
		return 0;
	}
	
	public RubricEntry getEntry(int index) {
		if (index >= 0 && index < entries.size()) {
			return entries.get(index);
		}
		// When we are modifying the rubric, then we can just add elements
		else if (isInModifiedState()) {
			while (entries.size() <= index) {
				entries.add(new RubricEntry());
			}
			return entries.get(index);
		}
		return null;
	}
	
	public void runAutomation(DataUpdateListener updateListener, Set<String> rubricElementNames, String studentName, String studentId, CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData ) {				
		
		for (RubricEntry entry : entries) {
			if (rubricElementNames == null || rubricElementNames.contains(entry.getName())) {
				entry.runAutomation(studentName, studentId, message, compiler, consoleData);				
				updateListener.dataUpdated();
			}
		}

	}
	
	public void clearStudentData() {
		for (RubricEntry entry : entries) {
			entry.clearStudentData();
		}
	}

	public boolean isInModifiedState() {
		return inModifiedState;
	}

	public void setInModifiedState(boolean inModifiedState) {
		this.inModifiedState = inModifiedState;
	}
	
	public void deleteEntry(String elementName) {
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).getName().equals(elementName)) {
				entries.remove(i);
				break;
			}
		}
	}
	
	public void loadFromSheet(LoadSheetData loadSheetData) {
		showedErrorMessage = false;
		entries.clear();
		fileDataMap.clear();
		if (loadSheetData == null || loadSheetData.isEmpty() == true) {
			return;
		}

		int currentAutomationHeader = 0;
		Map<String, List<List<Object>>> entryColumns = new HashMap<String, List<List<Object>>>();
		List<String> columnHeaders = new ArrayList<String>();
		// First read the label names & map them to columns
		List<Object> row = loadSheetData.readRow(0);
		
		for (Object entry : row) {
			String entryName = (String)entry;
			columnHeaders.add(entryName);
			if (entry != null && entry instanceof String) {							
				if (entryName.length() > 1 && columnHeaders.size() > 4) {
					if (entryColumns.containsKey(entryName) == false) {
						entryColumns.put(entryName.toUpperCase(), new ArrayList<List<Object>>());
					}
					if (currentAutomationHeader == 0) {
						currentAutomationHeader = columnHeaders.size() - 1;
					}
				}
			}							
		}
		
		// Now read all the standard components of the entries		
		for (int i = 1; i < loadSheetData.getNumRows(); i++) {
			List<Object> entries = loadSheetData.readRow(i);				
			if (entries != null && entries.get(0) != null && entries.get(0).toString().length() > 0) {
				modifyEntry(new RubricEntry(columnHeaders, entries));
			}
			else {
				break;
			}
		}
		
		// Now read the automation columns
		if (currentAutomationHeader != 0) {
			while(columnHeaders.size() > currentAutomationHeader && columnHeaders.get(currentAutomationHeader) != null) {
				String entryNameKey = columnHeaders.get(currentAutomationHeader).toUpperCase();			
				List<Object> column = loadSheetData.readColumn(currentAutomationHeader);
				entryColumns.get(entryNameKey).add(column);
				currentAutomationHeader++;
			}
			for (RubricEntry entry : entries) {
				entry.loadAutomationColumns(entryColumns, fileDataMap);
			}			
		}
		
		// Finally read the golden source
		loadGoldenSource(entryColumns);
	}
	
	private void loadGoldenSource(Map<String, List<List<Object>>> entryColumns) {
		List<List<Object> > columns = entryColumns.get(GOLDEN_SOURCE_LABEL.toUpperCase());
		if (columns == null || columns.size() == 0) {
			
			return;
		}
		
		List<Object> column = columns.get(0);
		List<String> fileNames = new ArrayList<String>();
		for (int i = 1; i < column.size(); i++) {
			String fileName = (String)column.get(i);
			if (fileName != null && fileName.length() > 0) {
				fileNames.add(fileName);
			}
		}
		if (fileNames.size() == 0) {

		}
		for (String fileName : fileNames) {
			FileData fileData = FileData.newFromSheet(fileName, entryColumns.get(fileName.toUpperCase()));
			if (fileData == null) {
				boolean showError = false;
				for (RubricEntry entry : entries) {
					if (entry.requiresGoldenFile() == true) {
						showError = true;
					}
				}
				if (showError) {
					showLoadError("Golden source file " + fileName + " is missing from save data");
				}
			}
			else {
				goldenSource.add(fileData);
			}
		}		
	}
	
	@Override
	public SaveSheetData getSheetSaveState() {
		SaveSheetData saveState = new SaveSheetData(SaveSheetData.ValueType.RAW, sheetData.getName());
		List<List<Object>> columnData = new ArrayList<List<Object>>();
		Map<String, List<Object>> fileData = new HashMap<String, List<Object>>();
		
		
		saveState.addOneRow(RubricEntry.getRubricHeader(), 1);
		int currentRow = 2;
		for (RubricEntry entry : entries) {
			saveState.addOneRow(entry.getRubricEntryInfo(), currentRow);
			entry.saveAutomationData(columnData, fileData);	
			currentRow++;
		}
		

		int currentColumn = RubricEntry.HeadingNames.values().length + 1;
		for (List<Object> column : columnData) {
			saveState.writeFullColumn(column, currentColumn);
			currentColumn++;
		}
		for (String key : fileData.keySet()) {
			List<Object> fileColumn = fileData.get(key);
			saveState.writeFullColumn(fileColumn, currentColumn);
			currentColumn++;			
		}
		saveGoldenSource(saveState, currentColumn);
		return saveState;		
	}
	
	private void saveGoldenSource(SaveSheetData saveState, int currentColumn) {
		List<Object> goldenSourceNameRows = new ArrayList<Object>();
		goldenSourceNameRows.add(GOLDEN_SOURCE_LABEL);
		for (FileData file : goldenSource) {
			goldenSourceNameRows.add(file.getName());
		}
		
		saveState.writeFullColumn(goldenSourceNameRows, currentColumn);
		currentColumn++;
		for (FileData file : goldenSource) {
			saveState.writeFullColumn(file.fillSaveData(), currentColumn);
			currentColumn++;
		}
	}
	

	@Override
	public GoogleSheetData getSheetInfo() {
		// TODO Auto-generated method stub
		return sheetData;
	}
	private static boolean showedErrorMessage = false;
	// For any given load, we only want to show one message to tell them to finish editing
	public static void showLoadError(String message) {
		if (showedErrorMessage == false) {
			JOptionPane.showMessageDialog(null, message + "  Edit rubric before running", "Rubric Incomplete - Finish Editing",
					JOptionPane.ERROR_MESSAGE);
				showedErrorMessage = true;			
		}
	}
	
	/**
	 * Called after we edit if we hit OK, Save, or Test.  This goes through and remove all entries that do not have a name
	 */
	public void cleanup() {
		for (int i = entries.size() - 1; i >= 0; i--) {
			if (entries.get(i).getName() == null || entries.get(i).getName().length() == 0) {
				entries.remove(i);
			}
		}
		
	}

	public boolean isGradingComplete(String id) {
		for (RubricEntry entry : entries) {
			if (entry.getStudentDoubleValue(id) == null) {
				return false;
			}
		}
		return true;
	}

}
