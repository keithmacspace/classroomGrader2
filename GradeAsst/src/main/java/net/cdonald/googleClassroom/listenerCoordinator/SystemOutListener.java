package net.cdonald.googleClassroom.listenerCoordinator;

public interface SystemOutListener {
	public void fired(String studentId, String rubricName,String text, Boolean executionFinished);

}
