package net.cdonald.googleClassroom.model;
import java.util.List;
import java.util.Map;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.AppendOutputTextListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;



public abstract class RubricAutomation {

	private String ownerName;
	private RubricEntrySystemListeners sysListeners;
	

	public RubricAutomation() {		
		
	}

	
	public Double runAutomation(RubricEntry owner, String studentName, String studentId, CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData) {		
		return runAutomation_(owner, studentName, studentId, message, compiler, consoleData);		
	}
	
	public void setOwnerName(String name) {
		ownerName = name;
		sysListeners = new RubricEntrySystemListeners(ownerName);		
	}
		
	public String getOwnerName() {
		return ownerName;
	}
	
	protected void prepareForNextTest() {
		sysListeners.prepareForNextTest();
	}
	
	protected void waitForTestFinish() {
		sysListeners.waitForTestFinish();
	}
	
	protected String getSysOutText(String studentID) {
		return sysListeners.getSysOutText(studentID);
	}

	
	protected void addOutput(String id, String text) {
		ListenerCoordinator.fire(AppendOutputTextListener.class, id, ownerName, text + "\n");
	}

	public abstract RubricAutomation newCopy();
	public abstract void removeFileData(FileData fileData);
	protected abstract Double runAutomation_(RubricEntry owner, String studentName, String studentId, CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData);
	protected abstract void saveAutomationColumns(String entryName, List<List<Object>> columnData, Map<String, List<Object>> fileData);
	protected abstract void loadAutomationColumns(String entryName, Map<String, List<List<Object>>> columnData,	Map<String, FileData> fileDataMap);



}
