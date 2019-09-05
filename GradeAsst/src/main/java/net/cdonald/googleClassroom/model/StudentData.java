package net.cdonald.googleClassroom.model;

import java.util.Date;
import java.util.Map;


public class StudentData extends ClassroomData {
	private String firstName;	
	public enum fieldNames {
		ID, LAST, FIRST
	};
	public static final String DB_TABLE_NAME = "Students";

	public StudentData(String firstName, String lastName, String id, Date creationTime) {
		super(lastName, id, creationTime);
		this.firstName = firstName;
	}
	
	public StudentData(Map<String, String> dbInfo) {
		for (String fieldName : dbInfo.keySet()) {
			for (fieldNames field : fieldNames.values()) {
				if (fieldName.compareToIgnoreCase(field.toString()) == 0) {
					setDBValue(field, dbInfo.get(fieldName));
				}
			}
		}
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	@Override
	public String[] getDBValues() {
		String[] superNames = super.getDBValues();
		String[] dbNames = { superNames[ClassroomData.fieldNames.ID.ordinal()],
				superNames[ClassroomData.fieldNames.NAME.ordinal()], firstName };
		return dbNames;
	}

	public void setDBValue(fieldNames field, String value) {
		switch (field) {
		case ID:
			super.setDBValue(ClassroomData.fieldNames.ID, value);
			break;
		case LAST:
			super.setDBValue(ClassroomData.fieldNames.NAME, value);
			break;
		case FIRST:
			firstName = value;
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

}
