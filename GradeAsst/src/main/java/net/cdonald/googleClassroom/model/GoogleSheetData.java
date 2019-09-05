package net.cdonald.googleClassroom.model;

import java.util.Map;


public class GoogleSheetData extends ClassroomData {
	private String sheetId; 
	public enum fieldNames{URL_ID, NAME, SHEET_ID}
	public static final String DB_NAME = "Rubric_Names";
	public GoogleSheetData(String name, String urlID, String sheetID) {
		super(name, urlID + sheetID);
		this.sheetId = sheetID;
	}
	
	public GoogleSheetData() {
		super();
		sheetId = "";
	}
	
	public GoogleSheetData(GoogleSheetData other) {
		this(other.getName(), other.getSpreadsheetId(), other.getSheetId());
	}
	
	public GoogleSheetData(Map<String, String> dbInfo) {
		for (String fieldName : dbInfo.keySet()) {
			for (fieldNames field : fieldNames.values()) {
				if (fieldName.compareToIgnoreCase(field.toString()) == 0) {
					setDBValue(field, dbInfo.get(fieldName));
				}
			}
		}
	}
	
	public void setDBValue(fieldNames field, String value) {
		switch (field) {
		case URL_ID:
			super.setDBValue(ClassroomData.fieldNames.ID, value);
			break;
		case NAME:
			super.setDBValue(ClassroomData.fieldNames.NAME, value);
			break;
		case SHEET_ID:
			sheetId = value;
			break;
		default:
			throw new IllegalArgumentException();
		}
	}
	
	public String getSpreadsheetId() {		
		// The url id of the spreadsheet is our id after removing the sheetID
		String spreadsheetId = getId();
		spreadsheetId = spreadsheetId.substring(0, spreadsheetId.length() - sheetId.length());
		return spreadsheetId;
	}

	public String getSheetId() {
		return sheetId;
	}
	
	public String[] getDBValues() {
		String [] superString = super.getDBValues();
		String [] dbString = {superString[0], superString[1], sheetId};
		return dbString;
	}
	
}
