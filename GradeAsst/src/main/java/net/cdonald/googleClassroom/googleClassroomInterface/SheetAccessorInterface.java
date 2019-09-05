package net.cdonald.googleClassroom.googleClassroomInterface;

import java.util.List;

import com.google.api.services.sheets.v4.model.ValueRange;

import net.cdonald.googleClassroom.model.GoogleSheetData;

public interface SheetAccessorInterface {
	public GoogleSheetData getSheetInfo();
	public SaveSheetData getSheetSaveState();

}
