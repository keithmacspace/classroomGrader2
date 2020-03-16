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
		ListenerCoordinator.fire(AppendOutputTextListener.class, id, ownerName, text + "\n", false);
	}
	
	protected void addOutput(String id, String text, boolean clearData) {
		ListenerCoordinator.fire(AppendOutputTextListener.class, id, ownerName, text + "\n", clearData);
	}

	public abstract RubricAutomation newCopy();	
	protected abstract Double runAutomation(RubricEntry owner, String studentName, String studentId, CompilerMessage message, StudentWorkCompiler compiler, List<FileData> referenceSource, List<FileData> testCodeSource, List<FileData> supportCodeSource, ConsoleData consoleData);
	protected abstract void saveAutomationColumns(String entryName, List<List<Object>> columnData);
	protected void loadAutomationColumns(String entryName, Map<String, List<List<Object>>> columnData, Map<String, FileData> fileDataMap) {
		// TODO Auto-generated method stub
		
	}

}
