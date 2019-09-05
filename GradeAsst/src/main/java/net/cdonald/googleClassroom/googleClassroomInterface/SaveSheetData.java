package net.cdonald.googleClassroom.googleClassroomInterface;

import java.util.ArrayList;
import java.util.List;

import com.google.api.services.sheets.v4.model.ValueRange;

public class SaveSheetData {
	public enum ValueType {USER_ENTERED, RAW}
	private List<ValueRange> saveState;
	private List<ColumnEntries> columns;
	private String sheetName;
	private int maxColumn;
	private int maxRow;
	private ValueType valueType;
	private class ColumnEntries {
		private List<Object> columnValues;
		private int columnNumber;
		private int startRow;
		// Pass in -1 if we should write the entire column
		public ColumnEntries(List<Object> columnValues, int columnNumber, int startRow) {
			super();
			this.columnValues = new ArrayList<Object>(columnValues);
			this.columnNumber = columnNumber;
			this.startRow = startRow;
		}
		public ValueRange prepColumn(int maxRow) {
			List<List<Object> > data = new ArrayList<List<Object>>();
			for (Object object : columnValues) {
				List<Object> row = new ArrayList<Object>();
				row.add(object);
				data.add(row);
			}
			while (data.size() < maxRow) {
				List<Object> row = new ArrayList<Object>();
				row.add("");
				data.add(row);
			}
			String columnName = GoogleClassroomCommunicator.getColumnName(columnNumber);
			ValueRange range = new ValueRange();
			// I am using -1 as a flag to say write the entire column
			if (startRow != -1) {
				range.setRange(sheetName + "!" + columnName + startRow + ":" + columnName + (startRow + data.size()));
			}
			else {
				range.setRange(sheetName + "!" + columnName + ":" + columnName);
			}
			range.setValues(data);
			return range;
		}

	}


	public SaveSheetData(ValueType valueType, String sheetName) {
		this.valueType = valueType;
		columns = new ArrayList<ColumnEntries>();
		maxColumn = 0;
		this.sheetName = sheetName;
		saveState = new ArrayList<ValueRange>();
	}
	
	public String getValueType() {
		return valueType.toString();
	}
	
	public int getMaxColumn() {
		return maxColumn;
	}
	
	public int getMaxRow() {
		return maxRow;
	}
	

	
	// This method will overwrite the data in a single column, it doesn't matter if only one
	// value is written, everything below that value will be overwritten with empty data
	public void writeFullColumn(List<Object> column, int currentColumn) {
		maxRow = Math.max(column.size(), maxRow);
		maxColumn = Math.max(maxColumn, currentColumn);
		columns.add(new ColumnEntries(column, currentColumn, -1));
	}
	
	// This value will write the data in the entries starting at row X and going down
	// to row y
	public void writeColumnEntries(List<Object> column, int currentColumn, int startRow) {
		maxRow = Math.max(column.size() + startRow, maxRow);
		maxColumn = Math.max(maxColumn, currentColumn);
		columns.add(new ColumnEntries(column, currentColumn, startRow));
		
	}
	 
	private void addOneRow(List<Object> row, int currentRow, int currentColumn) {
		maxRow = Math.max(currentRow, maxRow);
		List<List<Object> > tempSave = new ArrayList<List<Object>>();
		List<Object> rowData = new ArrayList<Object>();
		for (int i = currentColumn; i < row.size(); i++) {
			if (row.get(i) != null) {
				rowData.add(row.get(i));
			}
			else {
				addOneRow(row, currentRow, i + 1);
				break;
			}
		}
		if (rowData.size() > 0) {
			tempSave.add(rowData);
			String startColumn = GoogleClassroomCommunicator.getColumnName(currentColumn);
			String endColumn = GoogleClassroomCommunicator.getColumnName(rowData.size() + currentColumn);
			maxColumn = Math.max(maxColumn, rowData.size() + currentColumn);
			String range =  sheetName + "!" + startColumn + currentRow + ":" + endColumn + currentRow;
			saveState.add(new ValueRange().setRange(range).setValues(tempSave));
		}
	}
	
	// For rows, we only write the values that are in the list passed in - null will be skipped
	// and we will leave all additional data in that row
	public void addOneRow(List<Object> row, int currentRow) {
		addOneRow(row, currentRow, 0);
	}
	
	public List<ValueRange> getSaveState() {
		for (ColumnEntries column : columns) {
			saveState.add(column.prepColumn(maxRow));
		}
		return saveState;
	}
}
