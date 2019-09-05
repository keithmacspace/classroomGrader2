package net.cdonald.googleClassroom.model;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class ClassroomData implements Comparable<ClassroomData> {
	private String name;
	private String id;
	private Date date;
	private boolean isEmpty;
	private boolean retrievedFromGoogle;
	public static enum fieldNames {ID, NAME, DATE}

	static SimpleDateFormat formatter = new SimpleDateFormat("yyy-MM-dd'T'HH:mm:ss.SSSZ");
	
	
	public ClassroomData() {
		isEmpty = true;
		name = "None";
		id = "0";
		date = null;
		retrievedFromGoogle = false;
	}
	
	public ClassroomData(ClassroomData other) {
		name = other.name;
		id = other.id;
		date = other.date;
		isEmpty = other.isEmpty;
		retrievedFromGoogle = other.retrievedFromGoogle;
	}

	public ClassroomData(String name, String id, String creationTime) {
		super();
		Date date = null;
		if (creationTime != null) {
			try {
				date = formatter.parse(creationTime.replaceAll("Z$",  "+0000"));
			} catch (ParseException e) {
			}
		}
		init(name, id, date);
	}
	
	public ClassroomData(String name, String id, Date creationTime) {
		init(name, id, creationTime);
	}
	
	public ClassroomData(String name, String id) {
		init(name, id, null);
	}
	
	public ClassroomData(Map<String, String> dbInfo) {
		for (String fieldName : dbInfo.keySet()) {
			for (fieldNames field : fieldNames.values()) {
				if (fieldName.compareToIgnoreCase(field.toString()) == 0) {
					setDBValue(field, dbInfo.get(fieldName));
				}
			}
		}
	}
	
	private void init(String name, String id, Date creationTime) {
		this.name = name;
		this.id = id;
		isEmpty = false;
		date = creationTime;

	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}
	
	public void setId(String id) {
		this.id = id;
	}

	public Date getDate() {
		return date;
	}

	public boolean isEmpty() {
		return isEmpty;
	}

	public void setEmpty(boolean isEmpty) {
		this.isEmpty = isEmpty;
	}

	public String toString() {
		return name;
	}

	@Override
	public int compareTo(ClassroomData o) {
		if (isEmpty != o.isEmpty()) {
			if (isEmpty) {
				return -1;
			}
			return 1;
		}
		if (o.date != null && date != null) {
			int creationCompare = date.compareTo(o.date);
			if (creationCompare != 0) {
				return creationCompare;
			}
		}

		return o.name.compareToIgnoreCase(name);
	}
	
	
	public String[] getDBValues() {
		String [] dbString = {getId(), getName(), (date == null) ? "" : "" + date.getTime()};
		return dbString;
		
	}
	

				
	public void setDBValue(fieldNames field, String value) { 
		switch(field) {
		case ID:
			id = value;
			break;
		case NAME:
			name = value;
			break;
		case DATE:
			date = new Date(Long.parseLong(value));
			break;
		default:
			throw new IllegalArgumentException("Missing enum value");
		}
	}

	public boolean isRetrievedFromGoogle() {
		return retrievedFromGoogle;
	}

	public void setRetrievedFromGoogle(boolean retrievedFromGoogle) {
		this.retrievedFromGoogle = retrievedFromGoogle;
	}
	

}
