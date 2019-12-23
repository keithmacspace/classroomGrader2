package net.cdonald.googleClassroom.gui;

import net.cdonald.googleClassroom.model.Rubric;

public interface RubricEntryAutomationCardInterface {
	public void addItems(Rubric rubricToModify, int entryToModify);	
	public void saving();
	public String getDescription();
}
