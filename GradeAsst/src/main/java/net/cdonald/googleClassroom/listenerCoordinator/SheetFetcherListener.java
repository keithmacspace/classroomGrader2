package net.cdonald.googleClassroom.listenerCoordinator;

import net.cdonald.googleClassroom.model.ClassroomData;

public abstract class SheetFetcherListener extends LongQueryListener<ClassroomData> {
	private String sheetURL;

	
	public SheetFetcherListener(String sheetURL) {
		this.sheetURL = sheetURL;
	}
	

	public String getSheetURL() {
		return sheetURL;
	}
	

}
