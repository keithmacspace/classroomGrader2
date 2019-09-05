package net.cdonald.googleClassroom.gui;


import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.model.ClassroomData;


public class StudentListModel extends AbstractTableModel {
	private static final long serialVersionUID = -3240265069491780098L;
	private StudentListInfo studentListInfo;
	private ResizeAfterUpdateListener resizeAfterUpdateListener;

	
	public StudentListModel(StudentListInfo studentListInfo, ResizeAfterUpdateListener resizeAfterUpdateListener) {
		this.studentListInfo = studentListInfo;
		this.resizeAfterUpdateListener = resizeAfterUpdateListener;
		clearAll();
	}

	@Override
	public String getColumnName(int column) {		
		if (column >= StudentListInfo.NUM_DEFAULT_COLUMNS) {
			return studentListInfo.getColumnName(column);
		}
		else {
			return StudentListInfo.defaultColumnNames[column];
		}
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		if (columnIndex == StudentListInfo.LAST_NAME_COLUMN) {
			return ClassroomData.class;
		}
		else if (columnIndex == StudentListInfo.DATE_COLUMN) {
			return java.util.Date.class;
		}
		else if (columnIndex == StudentListInfo.COMPILER_COLUMN) {
			return CompilerMessage.class;
		}
		return String.class;
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		if (columnIndex < StudentListInfo.NUM_DEFAULT_COLUMNS) {
			return false;
		}
		return true;
	}


	@Override
	public int getColumnCount() {
		int value = StudentListInfo.NUM_DEFAULT_COLUMNS;
		if (studentListInfo != null) {
			value = studentListInfo.getColumnCount();
		}
		return value;		
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if (studentListInfo != null) {
			return studentListInfo.getValueAt(rowIndex, columnIndex);
		}
		return null;
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		if (studentListInfo != null) {
			studentListInfo.setValueAt(aValue, rowIndex, columnIndex);
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				resizeAfterUpdateListener.resizeColumn(columnIndex, getValueAt(rowIndex, columnIndex), false);
			}
		});
		

	}

	public void clearAll() {
		for (int i = StudentListInfo.NUM_DEFAULT_COLUMNS; i < getColumnCount(); i++) {
			String columnName = studentListInfo.getColumnName(i); 
			addColumn(columnName);
		}
	}


	public void addColumn(String name) {

	}


	@Override
	public int getRowCount() {
		if (studentListInfo != null) {
			return studentListInfo.getRowCount();
		}
		return 0;
	}
	
	public void structureChanged() {
		clearAll();
		fireTableStructureChanged();				
	}

}
