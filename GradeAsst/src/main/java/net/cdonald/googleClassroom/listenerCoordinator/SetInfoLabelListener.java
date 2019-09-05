package net.cdonald.googleClassroom.listenerCoordinator;

public interface SetInfoLabelListener {
	public enum LabelTypes{GRADE_FILE, RUBRIC_INFO, RUNNING}
	public void fired(LabelTypes labelType, String label);
}
