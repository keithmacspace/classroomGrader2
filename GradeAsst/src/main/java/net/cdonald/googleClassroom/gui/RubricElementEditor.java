package net.cdonald.googleClassroom.gui;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import net.cdonald.googleClassroom.model.RubricEntry;



public class RubricElementEditor extends AbstractCellEditor implements TableCellEditor {
	
	private JComboBox<RubricEntry.AutomationTypes> combo;
	
	public RubricElementEditor() {
		combo = new JComboBox<RubricEntry.AutomationTypes>(RubricEntry.AutomationTypes.values());
	}

	@Override
	public Object getCellEditorValue() {
		// TODO Auto-generated method stub
		return combo.getSelectedItem();
	}

	@Override
	public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int colum) {
		combo.setSelectedItem(value);
		
		combo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				fireEditingStopped();
			}
			
		});
		// TODO Auto-generated method stub
		return combo;
	}

	@Override
	public boolean isCellEditable(EventObject arg0) {
		return true;
	}
	

}
