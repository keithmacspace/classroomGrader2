package net.cdonald.googleClassroom.gui;

import net.cdonald.googleClassroom.model.RubricEntry;

public interface RubricElementListener {
	public boolean typeSelected(RubricEntry.AutomationTypes typeSelected, boolean isSelected);
}
