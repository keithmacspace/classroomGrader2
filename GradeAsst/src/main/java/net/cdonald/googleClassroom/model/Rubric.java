package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RubricTestCodeChanged;

public class Rubric implements SheetAccessorInterface {
	public enum ScoreModifiableState {
		TRACK_MODIFICATIONS, LOCK_USER_MODIFICATIONS
	}
	public enum RubricModifiableState {
		
	}
	private static ScoreModifiableState scoreModifiableState = ScoreModifiableState.TRACK_MODIFICATIONS;
	private GoogleSheetData sheetData;
	
	private boolean isRubricDefinitionModified = false;
	private List<FileData> referenceSource;
	private List<FileData> testCodeSource;
	private List<RubricEntry> entries = Collections.synchronizedList(new ArrayList<RubricEntry>());
	private static final String REFERENCE_SOURCE_LABEL = "Reference Source Files";		
	private boolean loadedFromFile = false;
	private boolean allowEntryAddition = false;

	public static ScoreModifiableState getScoreModifiableState() {
		return scoreModifiableState;
	}

	public static void setScoreModifiableState(ScoreModifiableState modifiableState) {
		Rubric.scoreModifiableState = modifiableState;
	}

	public Rubric(GoogleSheetData sheetData) {
		super();
		this.sheetData = sheetData;
		loadedFromFile = false;
		Collections.synchronizedCollection(new ArrayList<RubricEntry>());		
		entries = Collections.synchronizedList(new ArrayList<RubricEntry>());
		referenceSource = new ArrayList<FileData>();
		testCodeSource = new ArrayList<FileData>();

	}

	public Rubric(Rubric other) {
		if (other.sheetData != null) {
			sheetData = new GoogleSheetData(other.sheetData);
		}
		else {
			sheetData = null;
		}		
		loadedFromFile = other.loadedFromFile;
		synchronized(other.entries) {
			other.entries.forEach((e)->
			{
				entries.add(e);
			});
		}
		referenceSource = new ArrayList<FileData>();
		testCodeSource = new ArrayList<FileData>();
		setSource(testCodeSource, other.testCodeSource);
		setSource(referenceSource, other.referenceSource);
		isRubricDefinitionModified = other.isRubricDefinitionModified;
		
	}

	// This form is used when we are creating a new rubric from scratch
	public Rubric() {
		isRubricDefinitionModified = false;
		loadedFromFile = false;		
		referenceSource = new ArrayList<FileData>();
		testCodeSource = new ArrayList<FileData>();
	}


	public List<FileData> getReferenceSource() {
		return referenceSource;
	}
	
	public boolean isAllowEntryAddition() {
		return allowEntryAddition;
	}

	public void setAllowEntryAddition(boolean allowEntryAddition) {
		this.allowEntryAddition = allowEntryAddition;
	}

	private void setSource(List<FileData> fileDataToChange, List<FileData> source) {
		isRubricDefinitionModified = true;
		fileDataToChange.clear();
		if (source != null) {
			for (FileData fileData : source) {
				fileDataToChange.add(fileData);
			}
		}		
		ListenerCoordinator.fire(RubricTestCodeChanged.class);
	}

	public void setReferenceSource(List<FileData> fileDataList) {
		setSource(referenceSource, fileDataList);
	}
	
	public void setTestCode(List<FileData> fileDataList) {
		setSource(testCodeSource, fileDataList);
	}
	public void addTestCode(List<FileData> newFiles) {
		if (newFiles != null && newFiles.size() > 0) {
			for (FileData fileData : newFiles) {
				boolean exists = false;
				for (FileData testFile : newFiles) {
					if (testFile.getName().contentEquals(fileData.getName())) {
						exists = true;
						testFile.setFileContents(fileData.getFileContents());
					}
				}
				if (exists == false) {
					testCodeSource.add(fileData);
				}
			}
			ListenerCoordinator.fire(RubricTestCodeChanged.class);
		}
	}


	public double getTotalValue(String id) {
		double value = 0.0;
		synchronized(entries) {
			Iterator<RubricEntry> it = entries.iterator();
			while (it.hasNext()) {
				RubricEntry entry = it.next();
				RubricEntry.StudentScore studentValue = entry.getStudentScore(id);
				if (studentValue != null && studentValue.getScore() != null) {
					value += studentValue.getScore();
				}				
			}
		}
		// Keep 1 unit of precision
		value *= 10;
		value = Math.round(value);
		value = (int) value;
		value /= 10;
		return value;

	}

	public String getTotalString(String id) {
		return "" + getTotalValue(id);
	}

	public List<String> getRubricTabs() {
		List<String> rubricTabs = new ArrayList<String>();
		synchronized(entries) {
			Iterator<RubricEntry> it = entries.iterator();
			while (it.hasNext()) {
				RubricEntry entry = it.next();
				entry.addRubricTab(rubricTabs);
			}
		}
		return rubricTabs;
	}

	public String getName() {
		return sheetData.getName();
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public boolean isLoadedFromFile() {
		return loadedFromFile;
	}

	public void setLoadedFromFile(boolean loadedFromFile) {
		this.loadedFromFile = loadedFromFile;
	}

	public void addNewEntry() {
		entries.add(new RubricEntry());
		
	}

	public boolean areGradesModified() {
		synchronized(entries) {
			Iterator<RubricEntry> it = entries.iterator();
			while (it.hasNext()) {
				RubricEntry entry = it.next();
				if (entry.anyGradesModified()) {
					return true;
				}
			}
		}		
		return false;
	}

	public void addNewEntry(int index) {
		isRubricDefinitionModified = true;
		entries.add(index, new RubricEntry());
	}

	public void modifyEntry(RubricEntry entry) {
		isRubricDefinitionModified = true;
		synchronized(entries) {
			Iterator<RubricEntry> it = entries.iterator();
			int index = -1;
			boolean found = false;
			while (it.hasNext() && found == false) {
				RubricEntry currentEntry = it.next();
				if (currentEntry.getName().equalsIgnoreCase(entry.getName())) {
					found = true;
				}
				else {
					index++;
				}
			}
			if (found) {
				entries.set(index, entry);
			}
			else {
				entries.add(entry);
			}
		}
	}

	public void removeEntry(String name) {
		isRubricDefinitionModified = true;
		synchronized(entries) {
			for (int i = 0; i < entries.size(); i++) {
				RubricEntry entry = entries.get(i);
				if (entry.getName().compareToIgnoreCase(name) == 0) {
					entries.remove(i);
					break;
				}
			}
		}
	}

	public void removeEntry(int index) {
		isRubricDefinitionModified = true;
		synchronized(entries) {
			if (index >= 0 && index <= entries.size()) {
				entries.remove(index);
			}
		}
	}

	public void swapEntries(int index, int otherIndex) {
		isRubricDefinitionModified = true;
		synchronized(entries) {
			if (index >= 0 && index < entries.size() && otherIndex >= 0 && otherIndex < entries.size()) {
				RubricEntry temp = entries.set(index, entries.get(otherIndex));
				entries.set(otherIndex, temp);
			}
		}
	}

	public boolean isInRubric(String name) {
		synchronized(entries) {
			Iterator<RubricEntry> it = entries.iterator();
			while (it.hasNext()) {
				RubricEntry entry = it.next();

				if (entry.getName().equals(name)) {
					return true;
				}
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
			if (allowEntryAddition) {
				synchronized(entries) {					
					for (int i = entries.size() - 1; i >= 0; i--) {
						RubricEntry entry = entries.get(i);
						if (entry.getName() != null && entry.getName().length() != 0) {
							return i + 1;
						}
					}
				}			
			} else {
				return entries.size();
			}

		}
		return 0;
	}

	public RubricEntry getEntry(int index) {
		if (index < 0) {
			return null;
		}
		synchronized(entries) {
			if (index >= 0 && index < entries.size()) {
				return entries.get(index);
			}
			// When we are modifying the rubric, then we can just add elements
			else if (allowEntryAddition) {
				while (entries.size() <= index) {
					entries.add(new RubricEntry());
				}
				return entries.get(index);

			}
		}
		return null;
	}
	
	public RubricEntry getEntryByID(int elementID) {
		synchronized(entries) {
			Iterator<RubricEntry> it = entries.iterator();
			while (it.hasNext()) {
				RubricEntry entry = it.next();

				if (elementID == entry.getUniqueID()) {
					return entry;
				}			
			}
		}
		return null;
	}

	public boolean isGradingComplete(String id) {
		synchronized(entries) {
			Iterator<RubricEntry> it = entries.iterator();
			while (it.hasNext()) {
				RubricEntry entry = it.next();
				if (entry.getStudentScore(id) == null) {
					return false;
				}
			}
		}
		return true;
	}

	public void clearStudentScoreModifiedFlag() {
		synchronized(entries) {
			Iterator<RubricEntry> it = entries.iterator();
			while (it.hasNext()) {
				RubricEntry entry = it.next();
				entry.clearStudentScoreModifiedFlag();
			}
		}
	}
	

	public List<FileData> getTestCode() {		
		return testCodeSource;
	}



	public Set<String> runAutomation(List<RubricEntry.RubricUndoInfo> undoInfo, DataUpdateListener updateListener, Set<String> rubricElementNames, String studentName,
			String studentId, CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData) {

		Set<String> entriesSkipped = null;
		
		for (int index = 0; index < entries.size(); index++) {
			RubricEntry entry = entries.get(index);
			if (rubricElementNames == null || rubricElementNames.contains(entry.getColumnName())) {
				if (entry.runAutomation(undoInfo, index, studentName, studentId, message, compiler, consoleData, referenceSource, testCodeSource) == false) {
					if (entriesSkipped == null) {
						entriesSkipped = new HashSet<String>();						
					}
					entriesSkipped.add(entry.getColumnName());
				}
				updateListener.dataUpdated();
			}
		}
		return entriesSkipped;

	}

	public void clearStudentData() {
		for (RubricEntry entry : entries) {
			entry.clearStudentData();
		}
	}

	public boolean isRubricDefinitionModified() {
		if (isRubricDefinitionModified) {
			return true;
		}
		synchronized(entries) {
			Iterator<RubricEntry> it = entries.iterator();
			while (it.hasNext()) {
				RubricEntry entry = it.next();
				if (entry.isRubricDefinitionModified()) {
					return true;
				}
			}
		}
		return false;
	}

	public void clearRubricDefinitionModified() {
		this.isRubricDefinitionModified = false;
		synchronized(entries) {
			Iterator<RubricEntry> it = entries.iterator();
			while (it.hasNext()) {
				RubricEntry entry = it.next();
				entry.clearIsRubricDefinitionModified();
			}
		}
	}

	public void deleteEntry(String elementName) {
		synchronized(entries) {
			Iterator<RubricEntry> it = entries.iterator();
			while (it.hasNext()) {
				RubricEntry entry = it.next();
				if (entry.getName().equals(elementName) || entry.getColumnName().equals(elementName)) {
					it.remove();
					break;
				}
			}
		}
	}

	public void loadFromSheet(LoadSheetData loadSheetData) {
		showedErrorMessage = false;
		entries.clear();
		referenceSource.clear();
		testCodeSource.clear();
		Map<String, FileData> fileDataMap = new HashMap<String, FileData>();

		if (loadSheetData == null || loadSheetData.isEmpty() == true) {
			return;
		}

		int currentAutomationHeader = 0;
		Map<String, List<List<Object>>> entryColumns = new HashMap<String, List<List<Object>>>();
		List<String> columnHeaders = new ArrayList<String>();
		// First read the label names & map them to columns
		List<Object> row = loadSheetData.readRow(0);

		for (Object entry : row) {
			String entryName = (String) entry;
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
			} else {
				break;
			}
		}
		
		// Now read the automation columns
		if (currentAutomationHeader != 0) {
			while (columnHeaders.size() > currentAutomationHeader
					&& columnHeaders.get(currentAutomationHeader) != null) {
				String entryNameKey = columnHeaders.get(currentAutomationHeader).toUpperCase();
				List<Object> column = loadSheetData.readColumn(currentAutomationHeader);
				if (entryNameKey != null && entryNameKey.length() != 0) {
					List<List<Object>> entryColumn = entryColumns.get(entryNameKey);
					if (entryColumn != null) {
						entryColumn.add(column);
					}
				}
				currentAutomationHeader++;
			}
			for (RubricEntry entry : entries) {
				entry.loadAutomationColumns(entryColumns, fileDataMap);
			}
		}
		
		for (FileData testSource : fileDataMap.values()) {
			testCodeSource.add(testSource);
		}

		// Load the source files so that we can verify all the correct files exist.
		loadSource(entryColumns, referenceSource, REFERENCE_SOURCE_LABEL);		
		clearRubricDefinitionModified();
		//loadPointsBreakdown(entryColumns);
		
	}

	private void loadSource(Map<String, List<List<Object>>> entryColumns,  List<FileData> fileDataList, String columnName) {
		List<List<Object>> columns = entryColumns.get(columnName.toUpperCase());

		fileDataList.clear();
		if (columns == null || columns.size() == 0) {

			return;
		}

		List<Object> column = columns.get(0);
		List<String> fileNames = new ArrayList<String>();
		for (int i = 1; i < column.size(); i++) {
			String fileName = (String) column.get(i);
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
					if (entry.requiresReferenceFile() == true) {
						showError = true;
					}
				}
				if (showError) {
					showLoadError("Source file " + fileName + " is missing from save data");
				}
			} else {
				fileDataList.add(fileData);
			}
		}
	}

	@Override
	public SaveSheetData getSheetSaveState() {
		SaveSheetData saveState = new SaveSheetData(SaveSheetData.ValueType.RAW, sheetData.getName(), true);
		
		List<List<Object>> columnData = new ArrayList<List<Object>>();		

		saveState.addOneRow(RubricEntry.getRubricHeader(), 1);
		int currentRow = 2;
		for (RubricEntry entry : entries) {
			saveState.addOneRow(entry.getRubricEntryInfo(), currentRow);
			entry.saveAutomationData(columnData);
			currentRow++;
		}

		int currentColumn = RubricEntry.HeadingNames.values().length + 1;
		saveState.setAutoSizeColumnStart(currentColumn);
		for (List<Object> column : columnData) {
			saveState.writeFullColumn(column, currentColumn);
			currentColumn++;
		}

		List<Object> referenceSourceNameRows = new ArrayList<Object>();
		referenceSourceNameRows.add(REFERENCE_SOURCE_LABEL);
		for (FileData file : referenceSource) {
			referenceSourceNameRows.add(file.getName());
		}

		saveState.writeFullColumn(referenceSourceNameRows, currentColumn);
		currentColumn++;
		
		currentColumn = saveSource(saveState, referenceSource, currentColumn);
		currentColumn = saveSource(saveState, testCodeSource, currentColumn);
		return saveState;
	}

	private static int saveSource(SaveSheetData saveState, List<FileData> fileData, int currentColumn) {
		currentColumn++;
		for (FileData file : fileData) {
			saveState.writeFullColumn(file.fillSaveData(), currentColumn);
			currentColumn++;
		}
		return currentColumn;
	}

	@Override
	public GoogleSheetData getSheetInfo() {
		// TODO Auto-generated method stub
		return sheetData;
	}

	private static boolean showedErrorMessage = false;

	// For any given load, we only want to show one message to tell them to finish
	// editing
	public static void showLoadError(String message) {
		if (showedErrorMessage == false) {
			JOptionPane.showMessageDialog(null, message + "  Edit rubric before running",
					"Rubric Incomplete - Finish Editing", JOptionPane.ERROR_MESSAGE);
			showedErrorMessage = true;
		}
	}

	/**
	 * Called after we edit if we hit OK, Save, or Test. This goes through and
	 * remove all entries that do not have a name
	 */
	public void cleanup() {
		for (int i = entries.size() - 1; i >= 0; i--) {
			if (entries.get(i).getName() == null || entries.get(i).getName().length() == 0) {
				entries.remove(i);
			}
		}

	}




}
