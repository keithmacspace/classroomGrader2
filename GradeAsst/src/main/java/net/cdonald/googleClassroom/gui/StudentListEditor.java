package net.cdonald.googleClassroom.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.TableCellEditor;

public class StudentListEditor extends AbstractCellEditor implements TableCellEditor {
	private static final long serialVersionUID = 1501619200543325132L;
	private JTextField textField;

	public StudentListEditor() {
		textField = new JTextField();
	}

	@Override
	public Object getCellEditorValue() {
		// TODO Auto-generated method stub
		return textField.getText();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
		textField.setText((String) value);
		textField.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				fireEditingStopped();
			}

		});

		return textField;
	}

}
