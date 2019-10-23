package net.cdonald.googleClassroom.gui;

import java.awt.Color;
import java.awt.Component;
import java.util.Date;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.utils.SimpleUtils;

public class StudentListRenderer extends DefaultTableCellRenderer {
	private static final long serialVersionUID = -7082168845923165249L;
	private StudentListInfo studentListInfo;
	
	public StudentListRenderer(StudentListInfo studentListInfo) {
		this.studentListInfo = studentListInfo;
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {

		Component c = null;
		boolean makeRed = false;

		if (value != null) {
			makeRed = studentListInfo.showRed(row, column, value);
			String valueString = null;

			switch (column) {
			case StudentListInfo.DATE_COLUMN:				
				Date date = (Date)value;
				valueString = SimpleUtils.formatDate(date);
				break;
			case StudentListInfo.COMPILER_COLUMN:
				if (value != null) {
					CompilerMessage message = (CompilerMessage)value;
					if (message.isSuccessful()) {
						valueString = "Y";					}
					else {
						valueString = "N";
						makeRed = true;
					}
				}
				break;
			default:
				valueString = value.toString();
				break;				
			}
			c = super.getTableCellRendererComponent(table, valueString, isSelected, hasFocus, row, column);
		} else {
			c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
		}
		if (makeRed) {
			c.setForeground(Color.RED);
		} else {
			c.setForeground(Color.BLACK);
		}
		return c;
	}

}
