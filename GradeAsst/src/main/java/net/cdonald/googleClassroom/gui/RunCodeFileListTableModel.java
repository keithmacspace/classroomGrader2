package net.cdonald.googleClassroom.gui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;


public class RunCodeFileListTableModel extends AbstractTableModel {
	private static final long serialVersionUID = 8156237947165577539L;		
	RunCodeFileListTableModelListener listener;

	
	public RunCodeFileListTableModel(RunCodeFileListTableModelListener listener) {
		super();
		this.listener = listener;
	}
	
	public void init() {
			
	}


	
	@Override
	public int getRowCount() {
		List<FileData> files = listener.getFiles();
		if (files == null || files.size() == 0) {
			return 5;
		}
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
		List<FileData> files = listener.getFiles();
		FileData current = null;
		if (files != null && files.size() > rowIndex) {
			current = files.get(rowIndex);
			if (columnIndex == 0) {
				return listener.containsSource(current);
			}
			if (current == null) {
				return null;
			}
			
		}
		if (columnIndex == 0) {
			return Boolean.FALSE;
		}
		return current;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		List<FileData> files = listener.getFiles();
		FileData current = null;
		if (files != null && files.size() > rowIndex && columnIndex == 0) {
			current = files.get(rowIndex);		
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

}
