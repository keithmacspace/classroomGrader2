package net.cdonald.googleClassroom.googleClassroomInterface;

import net.cdonald.googleClassroom.model.GoogleSheetData;

public interface SheetAccessorInterface {
	public GoogleSheetData getSheetInfo();
	public SaveSheetData getSheetSaveState();

}
