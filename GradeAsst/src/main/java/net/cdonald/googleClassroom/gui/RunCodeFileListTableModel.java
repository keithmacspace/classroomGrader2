package net.cdonald.googleClassroom.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;


public class RunCodeFileListTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 8156237947165577539L;	
	private List<FileData> files;
	RunCodeFileListTableModelListener listener;

	
	public RunCodeFileListTableModel(Rubric rubric, RunCodeFileListTableModelListener listener) {
		super();
		this.listener = listener;
		files = new ArrayList<FileData>();
	}
	
	public void init() {
		files.clear();			
	}


	
	@Override
	public int getRowCount() {
		return files.size();		
	}

	@Override
	public int getColumnCount() {
		// TODO Auto-generated method stub
		return 2;
	}
	
	@Override
	public Class<?> getColumnClass(int col) {
		switch(col) {
		case 0:
			return Boolean.class;
		case 1:
			return FileData.class;
		}
		return null;
	}


	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		FileData current = files.get(rowIndex);
		if (current == null) {
			return null;
		}
		if (columnIndex == 0) {
			return listener.containsSource(current);
		}
		return current;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		FileData current = files.get(rowIndex);		
		if (current != null) {
			if (columnIndex == 0) {
				Boolean value = (Boolean)aValue;
				if (value == false) {
					listener.removeRunCodeFile(current);
				}
				else {
					listener.addRunCodeFile(current);
				}				
			}
		}
	}

	@Override
	public String getColumnName(int column) {
		switch(column) {
		case 0:
			return "Use";
		case 1:
			return "File";
		}
		return null;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return (columnIndex == 0);
	}
	
	public void addFile(FileData fileData) {
		boolean found = false;
		for (int i = 0; i < files.size(); i++) {
			if (files.get(i) == null) {
				files.set(i, fileData);
				found = true;
				break;
			}
		}
		if (found == false) {
			files.add(fileData);			
		}		
	}
	
	public void removeFile(String fileName) {
		for (int i = 0; i < files.size(); i++) {
			if (files.get(i).getName().equals(fileName)) {
				files.remove(i);
				break;
			}
		}		
	}

}
