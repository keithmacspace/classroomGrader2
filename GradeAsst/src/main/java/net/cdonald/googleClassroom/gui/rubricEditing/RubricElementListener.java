package net.cdonald.googleClassroom.gui.rubricEditing;

import net.cdonald.googleClassroom.model.RubricEntry;

public interface RubricElementListener {
	public boolean typeSelected(RubricEntry.AutomationTypes typeSelected, int row, boolean isSelected);
}
