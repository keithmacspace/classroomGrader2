package net.cdonald.googleClassroom.gui;

import net.cdonald.googleClassroom.model.Rubric;

public interface SetRubricListener {
	public enum RubricType {PRIMARY, RUBRIC_BEING_EDITED} 
	public void fired(Rubric rubric, RubricType rubricType);

}
