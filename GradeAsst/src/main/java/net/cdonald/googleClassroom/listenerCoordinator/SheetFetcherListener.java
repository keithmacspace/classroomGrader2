package net.cdonald.googleClassroom.listenerCoordinator;

import java.util.ArrayList;
import java.util.List;

import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.GoogleSheetData;

public abstract class SheetFetcherListener extends LongQueryListener<ClassroomData> {
	private String sheetURL;

	
	public SheetFetcherListener(String sheetURL) {
		this.sheetURL = sheetURL;
	}
	

	public String getSheetURL() {
		return sheetURL;
	}
	

}
