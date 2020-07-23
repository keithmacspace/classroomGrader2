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

	public static class StudentScore {
		public StudentScore() {
			score = null;
			modifiedByUser = false;
		}
		public StudentScore(StudentScore orig) {
			score = null;
			if (orig.score != null) {
				double value = orig.score;
				score = value;
			}
			modifiedByUser = orig.modifiedByUser;
			
		}
		public Double getScore() {
			return score;
		}

		public boolean isModifiedByUser() {
			return modifiedByUser;
		}

		private Double score;
		private boolean modifiedByUser;
		public boolean scoreEquals(StudentScore other) {
			if (score == null || other == null) { 
				return false;
			}
			if (score.equals(other.score)) {
				return true;
			}
			return false;			
		}
	}
	public static class RubricUndoInfo {
		public RubricUndoInfo(int rubricEntryIndex, String studentID, StudentScore studentScorePreChange,
				StudentScore studentScorePostChange) {
			super();
			this.rubricEntryIndex = rubricEntryIndex;
			this.studentID = studentID;
			this.studentScorePostChange = null;
			this.studentScorePostChange = null;
			if (studentScorePreChange != null) {
				this.studentScorePreChange = new StudentScore(studentScorePreChange);
			}
			if (studentScorePostChange != null) {
				this.studentScorePostChange = new StudentScore(studentScorePostChange);
			}
		}
		public String getStudentID() {
			return studentID;
		}
		public void setStudentID(String studentID) {
			this.studentID = studentID;
		}
		public int getRubricEntryIndex() {
			return rubricEntryIndex;
		}
		public void setRubricEntryIndex(int rubricEntryIndex) {
			this.rubricEntryIndex = rubricEntryIndex;
		}
		public RubricEntry.StudentScore getStudentScorePreChange() {
			return studentScorePreChange;
		}
		public void setStudentScorePreChange(RubricEntry.StudentScore studentScorePreChange) {
			this.studentScorePreChange = studentScorePreChange;
		}
		public RubricEntry.StudentScore getStudentScorePostChange() {
			return studentScorePostChange;
		}
		public void setStudentScorePostChange(RubricEntry.StudentScore studentScorePostChange) {
			this.studentScorePostChange = studentScorePostChange;
		}
		private String studentID;
		private int rubricEntryIndex;
		private RubricEntry.StudentScore studentScorePreChange;
		private RubricEntry.StudentScore studentScorePostChange;
	}
	private static int creationCount = 0; 
	private int uniqueID;
	private String name;
	private String description;
	private int rubricValue;
	private AutomationTypes automationType;
	private Map<String, StudentScore> studentScores;
	private RubricAutomation automation;
	private boolean isRubricDefinitionModified = false;


	private void assignID() {
		creationCount++;
		uniqueID = creationCount;
	}
	
	public int getUniqueID() {
		return uniqueID;
	}	

	public RubricEntry(List<String> headings, List<Object> entries) {
		assignID();
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
		assignID();
		automationType = AutomationTypes.NONE;
		studentScores = new HashMap<String, StudentScore>();		
	}

	public RubricEntry(RubricEntry other) {
		assignID();
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
		boolean nullScore = (score == null || score.score == null);

		try {
			if (stringValue == null || stringValue.length() == 0) {
				score.score = null;
				if (nullScore == false && Rubric.getScoreModifiableState() == Rubric.ScoreModifiableState.TRACK_MODIFICATIONS) {
					score.modifiedByUser = true;
				}

			} else if (stringValue.length() > 0) {
				Double test = Double.parseDouble(stringValue);
				if (test <= rubricValue || rubricValue == 0.0) {										
					if ((score.score == null || !test.equals(score.score)) && Rubric.getScoreModifiableState() == Rubric.ScoreModifiableState.TRACK_MODIFICATIONS) {
						score.modifiedByUser = true;
					}
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
	
	public void setStudentScore(String studentID, StudentScore score) {
		studentScores.put(studentID, score);
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
		isRubricDefinitionModified = true;
		switch (headingName) {
		case NAME:
			name = (String) param;
			break;
		case VALUE:
			if (param instanceof Integer) {
				rubricValue = (Integer)param;
			}
			else if (param != null){
				try {
					rubricValue = Integer.parseInt(param.toString());
				}
				catch (NumberFormatException e) {
					
				}
				
			}
			break;
		case DESCRIPTION:
			description = (String) param;
			break;
		case AUTOMATION_TYPE:
			automationType = (AutomationTypes) param;
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
				return (Integer) rubricValue;
			} else {
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
		switch (automationType) {
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
		isRubricDefinitionModified = true;
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
		return "RubricEntry [name=" + name + ", description=" + description + ", rubricValue=" + rubricValue
				+ ", automationType=" + automationType + "]";
	}

	boolean runAutomation(List<RubricUndoInfo> undoInfo, int elementIndex, String studentName, String studentId, CompilerMessage message, StudentWorkCompiler compiler,
			ConsoleData consoleData, List<FileData> referenceSource, List<FileData> testCodeSource, List<FileData> supportCodeSource) {
		// Don't change the value of already graded rubrics
		StudentScore studentScore = studentScores.get(studentId);
		if (studentScore != null && studentScore.score != null) {
			return false;
		}
		// If they haven't turned anything in, don't run any automation, Even for "Turn something in"
		// it is better to wait since we won't reassign the grade later
		List<FileData> files = compiler.getSourceCode(studentId);
		if (files == null) {
			return true;
		}
		if (studentScore == null) {
			studentScore = new StudentScore();
			studentScores.put(studentId, studentScore);
		}
		StudentScore oldScore = new StudentScore(studentScores.get(studentId));

		// No need to check the Rubric.ModifiableState, if we're running automation,
		// we're changing the value
		if (automation != null) {
			Double result = automation.runAutomation(this, studentName, studentId, message, compiler, referenceSource, testCodeSource, supportCodeSource, consoleData);
			// Leave the old score if the result is null.
			if (result != null) {
				studentScore.modifiedByUser = true;
				double score = result;
				score *= rubricValue;

				// Just truncate below one digits of precision
				score *= 10.0;
				score = Math.round(score);
				score = (int) score;
				score /= 10.0;
				studentScore.score = score;
			}
		}

		else {
			switch (automationType) {
			case COMPILES:
				if (message != null) {
					studentScore.modifiedByUser = true;
					if (message.isSuccessful()) {
						studentScore.score = (double) rubricValue;
					} else {
						studentScore.score = 0.0;
					}
				}
				break;
			case TURN_IN_SOMETHING:
				// We checked above that they turned something in, so they just get these points
				studentScore.modifiedByUser = true;
				studentScore.score = (double) rubricValue;
				break;

			default:
				break;
			}
		}
		undoInfo.add(new RubricUndoInfo(elementIndex, studentId, oldScore, studentScore));
		return true;
	}
	public void clearStudentData() {
		studentScores.clear();
	}

	public void setName(String name) {
		isRubricDefinitionModified = true;
		this.name = name;
	}

	public void setDescription(String description) {
		isRubricDefinitionModified = true;
		this.description = description;
	}

	public void setValue(int rubricValue) {
		isRubricDefinitionModified = true;
		this.rubricValue = rubricValue;
	}

	public RubricAutomation getAutomation() {
		return automation;
	}

	public void setAutomationType(AutomationTypes automationType) {
		isRubricDefinitionModified = true;
		this.automationType = automationType;
	}

	public void setAutomation(RubricAutomation automation) {
		isRubricDefinitionModified = true;
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

	public void loadAutomationColumns(Map<String, List<List<Object>>> columnData, Map<String, FileData> testCodeSource) {
		if (automation != null) {
			automation.loadAutomationColumns(name, columnData, testCodeSource);
		}

	}
	

	public void saveAutomationData(List<List<Object>> columnData) {
		if (automation != null) {
			automation.saveAutomationColumns(name, columnData);
		}
	}


	public void addRubricTab(List<String> rubricTabs) {
		if (automation != null) {
			rubricTabs.add(name);
		}
	}

	public boolean requiresReferenceFile() {
		if (automationType != null && automationType.ordinal() > AutomationTypes.COMPILES.ordinal()) {
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

	public void clearStudentScoreModifiedFlag() {
		for (StudentScore score : studentScores.values()) {
			if (score != null) {
				score.modifiedByUser = false;
			}
		}

	}


	public String getColumnName() {
		// TODO Auto-generated method stub
		return getValue() + "-" + getName();
	}
	
	public boolean isRubricDefinitionModified() {
		return isRubricDefinitionModified;
	}
	
	public void clearIsRubricDefinitionModified() {
		isRubricDefinitionModified = false;
	}

	public String getTipMessage() {
		String tip = "";
		if (getValue() > 0) {
			tip += "Max Val = " + getValue() + ": ";
		}
		if (getDescription() != null) {
			tip += getDescription();
		}
		return tip;		
	}


}
