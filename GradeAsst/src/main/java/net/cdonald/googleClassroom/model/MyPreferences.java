package net.cdonald.googleClassroom.model;

import java.awt.Dimension;
import java.io.File;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import net.cdonald.googleClassroom.gui.DebugLogDialog;

/**
 * 
 * Just a small class to keep the preferences - keeps the names stable.
 *
 */
public class MyPreferences {
	public enum Dimensions {MAIN, RUBRIC_EDIT}
	public enum Dividers {STUDENT_SOURCE, STUDENT_NOTES, RUBRIC_SPLIT}
	// We store a few global preferences in java's prefs, but the rest we store
	// for individual classes in a per-class db
	private Preferences preferences;
	
	private enum GlobalPrefs {WORKING_DIR, CLASS_ID, CLASS_NAME, FILE_DIR, GRADED_BY_NAME, RUBRIC_URL, RUBRIC_FILE}
	
	private enum ClassPrefs {CLASS_RUBRIC_URL, CLASS_RUBRIC_FILE, GRADE_URL, GRADE_FILE};	

	
	public MyPreferences() {
		preferences = Preferences.userNodeForPackage(net.cdonald.googleClassroom.gui.MainGoogleClassroomFrame.class);	
	}	
	
	public String getRubricFile() {
		return getRubricInfo(GlobalPrefs.RUBRIC_FILE, ClassPrefs.CLASS_RUBRIC_FILE);			
	}
	
	public String getRubricURL() {
		return getRubricInfo(GlobalPrefs.RUBRIC_URL, ClassPrefs.CLASS_RUBRIC_URL);
	}
	
	public int getSplitLocation(Dividers divider) {
		return preferences.getInt(divider.toString(), 0);
	}
	
	public void setSplitLocation(Dividers divider, int location) {
		preferences.putInt(divider.toString(), location);
	}
	
	public Dimension getDimension(Dimensions dimType, int defaultX, int defaultY) {
		int xVal = preferences.getInt(dimType.toString() + "_X", defaultX);
		int yVal = preferences.getInt(dimType.toString() + "_Y", defaultY);
		return new Dimension(xVal, yVal);
	}
	
	public boolean dimensionExists(Dimensions dimType) {
		String searchString = dimType.toString() + "_X";
		try {
			for (String key : preferences.keys()) {
				if (key.equals(searchString)) {
					return true;
				}
			}
		} catch (BackingStoreException e) {

		}
		return false;
	}

	
	public void setDimension(Dimensions dimType, Dimension dimension) {
		preferences.putInt(dimType.toString() + "_X", dimension.width);
		preferences.putInt(dimType.toString() + "_Y", dimension.height);
	
	}

	
	private String getRubricInfo(GlobalPrefs globalPref, ClassPrefs classPref) {
		String rubricInfo = getClassPref(classPref);
		if (rubricInfo == null) {
			rubricInfo = preferences.get(globalPref.toString(), null);
		}
		return rubricInfo;		
	}
	
	
	
	public void setRubricInfo(String rubricName, String rubricURL) {
		preferences.put(GlobalPrefs.RUBRIC_FILE.toString(),  rubricName);
		preferences.put(GlobalPrefs.RUBRIC_URL.toString(), rubricURL);
		setClassPrefs(ClassPrefs.CLASS_RUBRIC_FILE, rubricName);
		setClassPrefs(ClassPrefs.CLASS_RUBRIC_URL, rubricURL);

	}
	
	public String getGradeFile() {
		return getClassPref(ClassPrefs.GRADE_FILE);		
	}
	
	public String getGradeURL() {
		return getClassPref(ClassPrefs.GRADE_URL);
	}
	
	public void setGradeInfo(String gradeFile, String gradeURL) {
		setClassPrefs(ClassPrefs.GRADE_FILE, gradeFile);
		setClassPrefs(ClassPrefs.GRADE_URL, gradeURL);
	}

	
	
	public String getWorkingDir() {
		return preferences.get(GlobalPrefs.WORKING_DIR.toString(), null);		
	}

	public void setWorkingDir(String workingDir) {
		preferences.put(GlobalPrefs.WORKING_DIR.toString(), workingDir);
		makeDirs();		
	}
	
	public String getFileDir() {
		return preferences.get(GlobalPrefs.FILE_DIR.toString(), null);		
	}
	
	public void setFileDir(String fileDir) {
		preferences.put(GlobalPrefs.FILE_DIR.toString(), fileDir);
	}

	public String getUserName() {
		return preferences.get(GlobalPrefs.GRADED_BY_NAME.toString(), System.getProperty("user.name"));
	}
	
	public void setUserName(String name) {
		preferences.put(GlobalPrefs.GRADED_BY_NAME.toString(), name);
	}
	
	public ClassroomData getClassroom() {
		String id = preferences.get(GlobalPrefs.CLASS_ID.toString(), null);
		if (id == null) {
			return null;
		}
		String name = preferences.get(GlobalPrefs.CLASS_NAME.toString(), null);
		makeDirs(getClassroomDir(id));
		return new ClassroomData(name, id);
	}
	
	
	public void setClassroom(ClassroomData classroom) {
		if (classroom != null) {
			DebugLogDialog.appendln(classroom.getName() + " " + classroom.getId());
			preferences.put(GlobalPrefs.CLASS_NAME.toString(), classroom.getName());
			preferences.put(GlobalPrefs.CLASS_ID.toString(), classroom.getId());
			makeDirs(getClassroomDir(classroom.getId()));			
		}		
	}
	
	private String getClassPref(ClassPrefs name) {
		String prefName = name + preferences.get(GlobalPrefs.CLASS_ID.toString(), null);
		return preferences.get(prefName, null);

	}
	
	private void setClassPrefs(ClassPrefs name, String value) {
		String prefName = name + preferences.get(GlobalPrefs.CLASS_ID.toString(), null);
		preferences.put(prefName, value);
	}
	
	
	public String getTokenDir() {
		return getWorkingDir() + File.separator + "tokens";
		
	}
	
	public String getClassroomDir() {
		ClassroomData currentCourse = getClassroom();
		if (currentCourse != null) {
			return getClassroomDir(currentCourse.getId());			
		}
		return null;		
	}

	public String getClassroomDir(String classID) {
		String classDirName = classID.replaceAll("\\s", "_");
		String classDir = getWorkingDir() + File.separator + classDirName;
		return classDir;
	}
	
	public String getJsonPath() {
		return getWorkingDir() + File.separator + "credentials.json";
		
	}
	
	private void makeDirs() {		
		makeDirs(getClassroomDir());
	}
	
	private void makeDirs(String classroomDir) {
		String directoryPath = getWorkingDir();

		if (classroomDir != null) {			
			new File(classroomDir).mkdir();
		}
		String tokenDir = directoryPath + File.separator + "tokens";
		new File(tokenDir).mkdir();		
	}
	

}
