package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;

import net.cdonald.googleClassroom.googleClassroomInterface.LoadSheetData;
import net.cdonald.googleClassroom.googleClassroomInterface.SaveSheetData;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetAccessorInterface;
import net.cdonald.googleClassroom.gui.DataUpdateListener;
import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RubricTestCodeChanged;

public class Rubric implements SheetAccessorInterface {
	public enum ModifiableState {
		TRACK_MODIFICATIONS, LOCK_USER_MODIFICATIONS
	}
	public class PointBreakdown {
		private int value;
		private String description;
		public PointBreakdown() {
			super();
			this.value = 0;
			this.description = "";
		}
		public PointBreakdown(int value, String description) {
			super();
			this.value = value;
			this.description = description;
		}
		public int getValue() {
			return value;
		}
		public void setValue(int value) {
			this.value = value;
		}
		public void setValue(String value) {
			try {
				this.value = Integer.parseInt(value);
			}catch(NumberFormatException e) {
				
			}
		}
		public String getDescription() {
			return description;
		}
		public void setDescription(String description) {
			this.description = description;
		}		
	}
	private final String partialCreditString = "PARTIAL CREDIT";
	private static ModifiableState modifiableState = ModifiableState.TRACK_MODIFICATIONS;
	private GoogleSheetData sheetData;
	private List<RubricEntry> entries;
	private boolean inModifiedState;
	private List<FileData> referenceSource;
	private List<FileData> testCodeSource;
	private static final String REFERENCE_SOURCE_LABEL = "Reference Source Files";	
	private List<PointBreakdown> pointBreakdown;
	private boolean loadedFromFile = false;

	public static ModifiableState getModifiableState() {
		return modifiableState;
	}

	public static void setModifiableState(ModifiableState modifiableState) {
		Rubric.modifiableState = modifiableState;
	}

	public Rubric(GoogleSheetData sheetData) {
		super();
		this.sheetData = sheetData;
		loadedFromFile = false;
		entries = new ArrayList<RubricEntry>();
		referenceSource = new ArrayList<FileData>();
		testCodeSource = new ArrayList<FileData>();

	}

	public Rubric(Rubric other) {
		sheetData = new GoogleSheetData(other.sheetData);
		entries = new ArrayList<RubricEntry>();
		loadedFromFile = other.loadedFromFile;
		for (RubricEntry otherEntry : other.entries) {
			entries.add(new RubricEntry(otherEntry));
		}
		inModifiedState = other.inModifiedState;
		referenceSource = new ArrayList<FileData>();
		testCodeSource = new ArrayList<FileData>();
		setSource(testCodeSource, other.testCodeSource);
		setSource(referenceSource, other.referenceSource);
		if (other.pointBreakdown != null) {
			for (PointBreakdown points : other.pointBreakdown) {
				addPointBreakdown(points);
			}
		}

	}

	// This form is used when we are creating a new rubric from scratch
	public Rubric() {
		inModifiedState = true;
		loadedFromFile = false;
		entries = new ArrayList<RubricEntry>();		
		referenceSource = new ArrayList<FileData>();
		testCodeSource = new ArrayList<FileData>();
	}


	public List<FileData> getReferenceSource() {
		return referenceSource;
	}
	
	private void setSource(List<FileData> fileDataToChange, List<FileData> source) {
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
		for (RubricEntry entry : entries) {
			RubricEntry.StudentScore studentValue = entry.getStudentScore(id);
			if (studentValue != null && studentValue.getScore() != null) {
				value += studentValue.getScore();
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
		for (RubricEntry entry : entries) {
			if (entry.anyGradesModified()) {
				return true;
			}
		}
		return false;
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
			if (entry.getName().compareToIgnoreCase(name) == 0) {
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
			} else {
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

	public boolean isInModifiedState() {
		return inModifiedState;
	}

	public void setInModifiedState(boolean inModifiedState) {
		this.inModifiedState = inModifiedState;
	}

	public void deleteEntry(String elementName) {
		for (int i = 0; i < entries.size(); i++) {
			if (entries.get(i).getName().equals(elementName) || entries.get(i).getColumnName().equals(elementName)) {
				entries.remove(i);
				break;
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
				entryColumns.get(entryNameKey).add(column);
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

		loadPointsBreakdown(entryColumns);
		
	}

	private void loadSource(Map<String, List<List<Object>>> entryColumns,  List<FileData> fileDataList, String columnName) {
		List<List<Object>> columns = entryColumns.get(columnName.toUpperCase());
		for (String colName : entryColumns.keySet()) {
			DebugLogDialog.appendln(colName);
		}
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
		savePointBreakdown(columnData);
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
		for (FileData ref : referenceSource) {
			saveState.writeFullColumn(ref.fillSaveData(), currentColumn);
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

	public boolean isGradingComplete(String id) {
		for (RubricEntry entry : entries) {
			if (entry.getStudentScore(id) == null) {
				return false;
			}
		}
		return true;
	}

	public void clearModifiedFlag() {
		for (RubricEntry entry : entries) {
			entry.clearModifiedFlag();
		}
	}
	
	public void addPointBreakdown(int value, String description) {
		addPointBreakdown(new PointBreakdown(value, description));
	}
	
	public void addPointBreakdown(PointBreakdown points) {
		if (pointBreakdown == null) {
			pointBreakdown = new ArrayList<PointBreakdown>();
		}
		pointBreakdown.add(points);
	}
	
	public List<PointBreakdown> getPointBreakdown() {
		return pointBreakdown;
	}
	public String getPointBreakdownValue(int row) {
		if (pointBreakdown != null && pointBreakdown.size() > row) {
			return "" + pointBreakdown.get(row).getValue();
		}
		return null;		
	}
	public String getPointBreakdownDescription(int row) {
		if (pointBreakdown != null && pointBreakdown.size() > row) {
			return pointBreakdown.get(row).getDescription();
		}
		return null;		
	}
	
	public PointBreakdown getPointBreakdown(int row) {
		if (pointBreakdown == null) {
			pointBreakdown = new ArrayList<PointBreakdown>();
		}
		if (row < pointBreakdown.size()) {
			return pointBreakdown.get(row);
		}
		pointBreakdown.add(new PointBreakdown(0, ""));
		return pointBreakdown.get(pointBreakdown.size() - 1);
	}
	public void setPointBreakdownValue(int row, String value) {		
			getPointBreakdown(row).setValue(value);			
	}
	public void setPointBreakdownDescription(int row, String value) {		
			getPointBreakdown(row).setDescription(value);		
	}
	public void addNewPointBreakdownDescription(int row) {
		if (row <= pointBreakdown.size() && row >= 0) {
			pointBreakdown.add(row, new PointBreakdown());
		}
	}
	
	public void removePointBreakdownDescription(int row) {
		if (row < pointBreakdown.size() && row >= 0) {
			pointBreakdown.remove(row);
		}
	}
	public void swapPointBreakdownDescriptions(int row1, int row2) {
		if (row1 >= 0 && row2 >= 0 && row1 < pointBreakdown.size() && row2 < pointBreakdown.size()) {
			PointBreakdown a = pointBreakdown.get(row1);
			pointBreakdown.set(row1, pointBreakdown.get(row2));
			pointBreakdown.set(row2, a);
		}
	}

	
	private void loadPointsBreakdown(Map<String, List<List<Object>>> columnData) {
		List<List<Object> > columns = columnData.get(partialCreditString);
		if (columns == null || columns.size() == 0) {
			return;
		}
		else {
			List<Object> valueRows = null;
			int maxRows = 0;
			int descriptionIndex = columns.size() - 1;
			if (descriptionIndex != 0) {
				valueRows = columns.get(0);
				maxRows = valueRows.size();
			}
			List<Object> descriptionRows = columns.get(descriptionIndex);
			maxRows = Math.max(descriptionRows.size(), maxRows);
			for (int i = 1; i < maxRows; i++) {
				int value = 0;
				String description = "";
				if (valueRows != null && valueRows.size() > i) {
					String valueString = (String)valueRows.get(i);
					if (valueString != null) {
						try {
							value = Integer.parseInt(valueString);
						}
						catch(NumberFormatException e) {
							description = valueString;
						}
					}
				}
				if (descriptionRows.size() > i) {
					String descriptionString = (String)descriptionRows.get(i);
					if (descriptionString != null) {
						description += descriptionString;
					}
				}
				if (description.length() > 0) {
					addPointBreakdown(value, description);
				}
			}
		}
	}
	
	private void savePointBreakdown(List<List<Object>> columnData) {
		if (pointBreakdown == null) {
			return;
		}
		List<Object> values = new ArrayList<Object>();
		List<Object> descriptions = new ArrayList<Object>();
		values.add(partialCreditString);
		descriptions.add(partialCreditString);
		for (PointBreakdown points : pointBreakdown) {
			values.add((Integer)points.getValue());
			descriptions.add(points.description);
		}
		columnData.add(values);
		columnData.add(descriptions);
	}

	public List<FileData> getTestCode() {		
		return testCodeSource;
	}



}
