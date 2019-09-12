package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;


public class RubricEntry {
	public static enum HeadingNames {
		ID, NAME, VALUE, DESCRIPTION, AUTOMATION_TYPE
	}

	public static enum AutomationTypes {
		NONE, TURN_IN_SOMETHING, POINT_LOSS_FOR_LATE, COMPILES, RUN_CODE, CODE_CONTAINS_METHOD
	}
	
	public class StudentScore {
		public Double getScore() {
			return score;
		}
		public boolean isModifiedByUser() {
			return modifiedByUser;
		}
		Double score;
		boolean modifiedByUser;
	}

	String id;
	String name;
	String description;
	int rubricValue;
	AutomationTypes automationType;
	Map<String, StudentScore> studentScores;
	RubricAutomation automation;
	


	public RubricEntry(List<String> headings, List<Object> entries) {
		rubricValue = 0;
		automationType = AutomationTypes.NONE;
		for (int i = 0; i < headings.size(); i++) {
			if (i < entries.size()) {
				String headingName = headings.get(i);

				for (HeadingNames heading : HeadingNames.values()) {

					if (heading.name().compareToIgnoreCase(headingName) == 0) {
						setValue(heading, entries.get(i).toString());
					}
				}
			}
		}
		studentScores = new HashMap<String, StudentScore>();

	}
	
	
	// This is the form used when we create it via the dialog box in addRubricEntry
	public RubricEntry() {
		automationType = AutomationTypes.NONE;
		studentScores = new HashMap<String, StudentScore>();

	}
	
	public RubricEntry(RubricEntry other) {
		id = other.id;
		name = other.name;
		description = other.description;
		rubricValue = other.rubricValue;
		automationType = other.automationType;
		studentScores = new HashMap<String, StudentScore>();
		for (String key : other.studentScores.keySet()) {
			studentScores.put(key, other.studentScores.get(key));
		}
		if (other.automation != null) {
			setAutomation(other.automation.newCopy());
		}
	}

	public void setStudentValue(String studentID, String stringValue) {
		if (studentScores.containsKey(studentID) == false) {
			studentScores.put(studentID, new StudentScore());
		}
		StudentScore score = studentScores.get(studentID);
		if (Rubric.getModifiableState() == Rubric.ModifiableState.TRACK_MODIFICATIONS) {
			score.modifiedByUser = true;
		}

		try {
			if (stringValue == null) {
				score.score = null;
			}
			else if ( stringValue.length() > 0) {
				Double test = Double.parseDouble(stringValue);
				if (test <= rubricValue || rubricValue == 0.0) {
					score.score = test;
				}
			}
		} catch (NumberFormatException e) {

		}
	}

	
	public StudentScore getStudentScore(String studentID) {
		
		if (studentScores.containsKey(studentID)) {			
			return studentScores.get(studentID);
		}
		return null;
	}
	
	public String getStudentValue(String studentID) {
		String displayValue = "";
		
		if (studentScores.containsKey(studentID)) {			
			Double doubleValue = studentScores.get(studentID).score;
			if (doubleValue != null) {				
				double test = doubleValue;
				if ((int) test == test) {
					displayValue = "" + ((int) test);
				} else {
					displayValue = "" + test;
				}
			}
		}
		return displayValue;
	}
	
	public void setTableValue(HeadingNames headingName, Object param) {
		switch (headingName) {
		case NAME:
			name = (String)param;
			break;
		case VALUE:
			rubricValue = (Integer)param;
			break;
		case DESCRIPTION:
			description = (String)param;
			break;
		case AUTOMATION_TYPE:
			automationType = (AutomationTypes)param;
			newAutomationType();
			break;
		default:
			break;
		}		
	}
	
	public Object getTableValue(HeadingNames headingName) {
		switch (headingName) {
		case NAME:
			return name;
		case VALUE:
			if (rubricValue != 0.0) {
				return (Integer)rubricValue;
			}
			else {
				return null;
			}
		case DESCRIPTION:
			return description;
		case AUTOMATION_TYPE:
			return automationType;
		default:
			break;
		}
		return null;
		
	}
	private void newAutomationType() {
		switch(automationType) {
		case RUN_CODE:
			setAutomation(new RubricEntryRunCode());
			break;
		case CODE_CONTAINS_METHOD:
			setAutomation(new RubricEntryMethodContains());
			break;
		case POINT_LOSS_FOR_LATE:
			setAutomation(new RubricEntryPointLossForLate());
			break;
		default:
			break;
		}
		
	}

	public void setValue(HeadingNames headingName, String param) {

		switch (headingName) {
		case NAME:
			name = param;
			break;
		case VALUE:
			rubricValue = Integer.parseInt(param);
			break;
		case DESCRIPTION:
			description = param;
			break;
		case AUTOMATION_TYPE:
			for (AutomationTypes automationValue : AutomationTypes.values()) {
				if (automationValue.name().compareToIgnoreCase(param) == 0) {
					automationType = automationValue;
					newAutomationType();
					break;
				}
			}
			break;
		default:
			break;
		}
	}

	public String getName() {
		return name;
	}

	public int getValue() {
		return rubricValue;
	}

	public String getDescription() {
		return description;
	}


	public AutomationTypes getAutomationType() {
		return automationType;
	}
	

	@Override
	public String toString() {
		return "RubricEntry [name=" + name + ", description=" + description + 
			  ", rubricValue=" + rubricValue + ", automationType=" + automationType
				+ "]";
	}

	void runAutomation(String studentName, String studentId, CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData) {
		// Don't change the value of already graded rubrics
		StudentScore studentScore = studentScores.get(message.getStudentId());
		if (studentScore != null && studentScore.score != null) {
			return;
		}
		if (studentScore == null) {
			studentScores.put(studentId, new StudentScore());
		}
		
		// No need to check the Rubric.ModifiableState, if we're running automation, we're changing the value
		studentScore.modifiedByUser = true;
		if (automation != null) {			
			Double result = automation.runAutomation(this, studentName, studentId, message, compiler, consoleData);
			// Leave the old score if the result is null.
			if (result != null) {
				double score = result;
				score *= rubricValue;
				// Just truncate below two digits of precision
				score *= 100.0;
				score = (int)score;
				score /= 100.0;
				studentScore.score = score;
			}
		}
		
		else {
			switch (automationType) {
			case COMPILES:
				if (message.isSuccessful()) {
					studentScore.score = (double)rubricValue;
				}
				else {
					studentScore.score = 0.0;
				}
				break;
			case TURN_IN_SOMETHING:
				List<FileData> files = compiler.getSourceCode(studentId);
				if (files != null) {
					studentScore.score = (double)rubricValue;
					return;
				}
				else {
					studentScore.score = 0.0;				
				}
				break;
				
			default:
				break;
			}
		}
	}

	public void clearStudentData() {
		studentScores.clear();		
	}
	

	public void setName(String name) {
		this.name = name;
	}

	public void setDescription(String description) {
		this.description = description;
	}


	public void setValue(int rubricValue) {
		this.rubricValue = rubricValue;
	}


	public RubricAutomation getAutomation() {
		return automation;
	}
	
	public void setAutomationType(AutomationTypes automationType) {
		this.automationType = automationType;
	}


	public void setAutomation(RubricAutomation automation) {
		this.automation = automation;
		if (automation != null) {
			automation.setOwnerName(getName());
		}
	}

	
	public List<Object> getRubricEntryInfo() {
		List<Object> row = new ArrayList<Object>();
		row.add(name);
		row.add("" + rubricValue);
		row.add(description);		
		row.add(automationType.toString());		
		return row;
	}
	
	public static List<Object> getRubricHeader() {
		List<Object> row = new ArrayList<Object>();
		for (HeadingNames name : HeadingNames.values()) {
			if (name != HeadingNames.ID) {
				row.add(name.toString());
			}
		}
		return row;
	}


	
	public void loadAutomationColumns(Map<String, List<List<Object>>> columnData, Map<String, FileData> fileDataMap) {
	
		if (automation != null) {
			automation.loadAutomationColumns(name, columnData, fileDataMap);
		}
	}
	
	public void saveAutomationData(List<List<Object>> columnData, Map<String, List<Object>> fileData) {
		if (automation != null) {
			automation.saveAutomationColumns(name, columnData, fileData);
		}
	}


	public void addRubricTab(List<String> rubricTabs) {
		if (automation != null) {
			rubricTabs.add(name);
		}		
	}
	
	public void removeFileData(FileData fileData) {
		if (automation != null) {
			automation.removeFileData(fileData);
		}
	}


	public boolean requiresGoldenFile() {
		if (automationType != null && automationType.ordinal() > AutomationTypes.COMPILES.ordinal() ) {
			return true;
		}
		return false;
	}


	public boolean anyGradesModified() {
		for (StudentScore studentScore : studentScores.values()) {
			if (studentScore != null && studentScore.isModifiedByUser() == true) {
				return true;
			}
		}
		return false;
	}


	public void clearModifiedFlag() {
		for (StudentScore score : studentScores.values()) {
			if (score != null) {
				score.modifiedByUser = false;
			}
		}
		
	}
}
