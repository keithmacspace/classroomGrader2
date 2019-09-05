package net.cdonald.googleClassroom.gui;

import java.awt.Color;
import java.awt.Component;

import javax.swing.JComboBox;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

import net.cdonald.googleClassroom.model.RubricEntry;

public class RubricElementRenderer implements TableCellRenderer {
	private RubricElementListener rubricElementListener;
	private JComboBox<RubricEntry.AutomationTypes> automationCombo;
	
	public RubricElementRenderer(RubricElementListener rubricElementListener) {
		automationCombo = new JComboBox<RubricEntry.AutomationTypes>(RubricEntry.AutomationTypes.values());
		automationCombo.setBackground(Color.WHITE);
		this.rubricElementListener = rubricElementListener;
	}
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
			int row, int column) {
		boolean valid = rubricElementListener.typeSelected((RubricEntry.AutomationTypes)value, isSelected);
		if (valid == true) {
			automationCombo.setSelectedItem(value);
		}
		return automationCombo;
	}

}
