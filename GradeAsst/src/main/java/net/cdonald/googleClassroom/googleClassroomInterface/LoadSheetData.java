package net.cdonald.googleClassroom.googleClassroomInterface;

import java.util.ArrayList;
import java.util.List;

public class LoadSheetData {
	List<List<Object> > sheetInfo;
	
	
	public LoadSheetData(List<List<Object>> sheetInfo) {
		this.sheetInfo = sheetInfo;
	}
	
	public boolean isEmpty()  {
		return (sheetInfo == null || sheetInfo.size() == 0);
	}
	
	
	public int getNumRows() {
		if (sheetInfo != null) {
			return sheetInfo.size();
		}
		return 0;
	}
	
	public void rowInserted(int rowIndex) {
		sheetInfo.add(rowIndex, null);
	}
	
	public List<Object> readRow(int rowIndex) {
		if (rowIndex >= 0 && rowIndex <= sheetInfo.size()) {
			return sheetInfo.get(rowIndex);
		}
		return null;
	}
	

	
	public List<Object> readColumn(int columnIndex) {
		List<Object> column = new ArrayList<Object>();
		for (int i = 0; i < sheetInfo.size(); i++) {
			List<Object> row = sheetInfo.get(i);
			if (columnIndex < row.size()) {
				column.add(row.get(columnIndex));
			}
			else {
				column.add(null);
			}
		}
		return column;
	}

}
