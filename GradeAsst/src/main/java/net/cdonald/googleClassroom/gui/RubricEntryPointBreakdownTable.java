package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import net.cdonald.googleClassroom.model.RubricEntry;

public class RubricEntryPointBreakdownTable extends JTable {
	private PointBreakdownTableModel pointBreakdownModel;
	private RubricEntry associatedEntry;
	private boolean isEditable;

	public RubricEntryPointBreakdownTable(boolean isEditable) {
		super();
		this.isEditable = isEditable;
		setDefaultRenderer(String.class, new TextAreaRenderer());
		setDefaultEditor(String.class, new TextAreaEditor());
		setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		if (isEditable) {
			setRowHeight(80);
		}
		pointBreakdownModel = new PointBreakdownTableModel();
		setModel(pointBreakdownModel);
		//getColumnModel().getColumn(0).setMaxWidth(16);
	}

	public RubricEntry getAssociatedEntry() {
		return associatedEntry;
	}

	public void setAssociatedEntry(RubricEntry associatedEntry) {
		this.associatedEntry = associatedEntry;
		pointBreakdownModel.fireTableDataChanged();
		if (isEditable == false) {
			updateRowHeights();
		}
	}
	
	public void stopEditing() {
		if (getCellEditor() != null) {
			getCellEditor().stopCellEditing();
		}
	}
	
	private void updateRowHeights()
	{
	    for (int row = 0; row < getRowCount(); row++)
	    {
	        int rowHeight = getRowHeight();

	        for (int column = 0; column < getColumnCount(); column++)
	        {
	            Component comp = prepareRenderer(getCellRenderer(row, column), row, column);
	            rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
	        }
	        setRowHeight(row, rowHeight);
	    }
	}


	class TextAreaRenderer extends JScrollPane implements TableCellRenderer {
		JTextArea textarea;

		public TextAreaRenderer() {
			textarea = new JTextArea();
			textarea.setLineWrap(true);
			textarea.setWrapStyleWord(true);
			getViewport().add(textarea);
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			if (isSelected) {
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
				textarea.setForeground(table.getSelectionForeground());
				textarea.setBackground(table.getSelectionBackground());
			} else {
				setForeground(table.getForeground());
				setBackground(table.getBackground());
				textarea.setForeground(table.getForeground());
				textarea.setBackground(table.getBackground());
			}

			textarea.setText((String) value);
			textarea.setCaretPosition(0);
			return this;
		}
	}

	class TextAreaEditor extends DefaultCellEditor {
		protected JScrollPane scrollpane;
		protected JTextArea textarea;

		public TextAreaEditor() {
			super(new JCheckBox());
			scrollpane = new JScrollPane();
			textarea = new JTextArea();
			textarea.setLineWrap(true);
			textarea.setWrapStyleWord(true);
			scrollpane.getViewport().add(textarea);
		}

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			textarea.setText((String) value);
			return scrollpane;
		}

		public Object getCellEditorValue() {
			return textarea.getText();
		}
	}

	private class PointBreakdownTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 3651730049445837992L;

		@Override
		public int getRowCount() {
			int min = (isEditable) ? 3 : 0;
			if (associatedEntry != null && associatedEntry.getPointBreakdown() != null) {
				min = Math.max(min, associatedEntry.getPointBreakdown().size());
				if (isEditable) {
					min++;
				}
			}
			return min;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return isEditable;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			if (columnIndex == 0) {
				return Integer.class;
			}
			return String.class;
		}

		@Override
		public String getColumnName(int column) {
			if (column == 0) {
				return "Value";
			}
			return "Description";
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public Object getValueAt(int row, int column) {

			if (associatedEntry == null) {
				return null;
			}
			if (column == 0) {
				return associatedEntry.getPointBreakdownValue(row);
			}
			return associatedEntry.getPointBreakdownDescription(row);
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			if (associatedEntry == null) {
				return;
			}
			if (column == 0) {
				associatedEntry.setPointBreakdownValue(row, aValue.toString());
			} else {
				associatedEntry.setPointBreakdownDescription(row, (String) aValue);
			}
		}

	}

}
