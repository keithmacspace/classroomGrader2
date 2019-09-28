package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntry.HeadingNames;

public class RubricEntriesTable extends JTable {
	private RubricEntryTableModel rubricEntryTableModel;
	private Rubric associatedRubric;	
	private static final String enterActionKey = "Enter_Action";
	public RubricEntriesTable(RubricElementListener listener) {
		super();


		setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		rubricEntryTableModel = new RubricEntryTableModel();
		setModel(rubricEntryTableModel);
		JLabel val = new JLabel("Val");
		for (int i = 0; i < rubricEntryTableModel.getColumnCount(); i++) {
			TableColumn column = getColumnModel().getColumn(i);
			switch(rubricEntryTableModel.getColumnHeading(i)) {
			case AUTOMATION_TYPE:
				column.setCellEditor(new RubricAutomationEditor());
				column.setCellRenderer(new AutomationCellRenderer(listener));
				break;
			case DESCRIPTION:
				column.setCellEditor(new DescriptionAreaEditor());
				column.setCellRenderer(new DescriptionCellRenderer());
				break;

			case NAME:
				break;
			case VALUE:
				break;
			default:
				break;			
			}
		}
		setComponentPopupMenu(createPopupMenu());
		setSelectionBackground(getSelectionBackground());
		setCellSelectionEnabled(true);
		new ExcelAdapter(this, true);
	}
	
	public void fireTableDataChanged() {
		rubricEntryTableModel.fireTableDataChanged();
	}
	
	public int getColumnCount() {
		return rubricEntryTableModel.getColumnCount();
	}
	
	public Class<?> getColumnClass(int columnIndex) {
		return rubricEntryTableModel.getColumnClass(columnIndex);
	}
	
	public Object getValueAt(int row, int column) {
		return rubricEntryTableModel.getValueAt(row, column);
	}
	
	

	private JPopupMenu createPopupMenu() {
		ListSelectionModel selectionModel = getSelectionModel();
		JPopupMenu rightClickPopup = new JPopupMenu();
		JMenuItem moveUpItem = new JMenuItem("Move Up");
		JMenuItem moveDownItem = new JMenuItem("Move Down");
		JMenuItem insertAbove = new JMenuItem("Add Entry Above");
		JMenuItem insertBelow = new JMenuItem("Add Entry Below");
		JMenuItem delete = new JMenuItem("Delete");
		rightClickPopup.add(moveUpItem);		
		rightClickPopup.add(moveDownItem);
		rightClickPopup.add(insertAbove);
		rightClickPopup.add(insertBelow);
		rightClickPopup.add(delete);

		delete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopEditing();
				if (associatedRubric != null) {
					int selectedIndex = selectionModel.getMinSelectionIndex();
					associatedRubric.removeEntry(selectedIndex);
					rubricEntryTableModel.fireTableDataChanged();
				}

			}			
		});

		moveUpItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopEditing();
				if (associatedRubric != null) {
					int selectedIndex = selectionModel.getMinSelectionIndex();
					associatedRubric.swapEntries(selectedIndex, selectedIndex -1);
					rubricEntryTableModel.fireTableDataChanged();
				}

			}			
		});
		moveDownItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				stopEditing();
				if (associatedRubric != null) {
					int selectedIndex = selectionModel.getMinSelectionIndex();
					associatedRubric.swapEntries(selectedIndex, selectedIndex + 1);
					rubricEntryTableModel.fireTableDataChanged();
				}
			}			
		});
		insertAbove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopEditing();
				if (associatedRubric != null) {
					int selectedIndex = selectionModel.getMinSelectionIndex();
					associatedRubric.addNewEntry(selectedIndex);
					rubricEntryTableModel.fireTableDataChanged();
				}
			}			
		});
		insertBelow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopEditing();
				int selectedIndex = selectionModel.getMinSelectionIndex();
				if (associatedRubric != null) {
					associatedRubric.addNewEntry(selectedIndex + 1);
					rubricEntryTableModel.fireTableDataChanged();
				}
			}			
		});
		return rightClickPopup;
	}


	public Rubric getAssociatedRubric() {
		return associatedRubric;
	}

	public void setAssociatedEntry(Rubric associatedEntry) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				associatedRubric = associatedEntry;
				rubricEntryTableModel.fireTableDataChanged();		
				updateRowHeights();				
			}
		});

	}

	public void stopEditing() {

		if (getCellEditor() != null) {
			getCellEditor().stopCellEditing();
		}

	}

	public void updateRowHeights()
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

	private void updateRowHeight(JTextArea area) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int selectedIndex = selectionModel.getMinSelectionIndex();
				if (selectedIndex >= 0 && selectedIndex <= getRowCount()) {
					
					int newRowHeight = area.getPreferredSize().height;
					Graphics g = area.getGraphics();
					if (g != null) {
						FontMetrics f = g.getFontMetrics(area.getFont());
						if (f != null) {
							int maxHeight = 5 * f.getHeight();
							if (newRowHeight > maxHeight) {
								newRowHeight = maxHeight;
							}
						}
					}

					if (newRowHeight > getRowHeight()) {
						setRowHeight(selectedIndex, newRowHeight);
					}

				}
			}
		});
	}

	private class RubricAutomationEditor extends AbstractCellEditor implements TableCellEditor {

		private JComboBox<RubricEntry.AutomationTypes> combo;

		public RubricAutomationEditor() {
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

	private class AutomationCellRenderer implements TableCellRenderer {
		private RubricElementListener rubricElementListener;
		private JComboBox<RubricEntry.AutomationTypes> automationCombo;

		public AutomationCellRenderer(RubricElementListener rubricElementListener) {
			automationCombo = new JComboBox<RubricEntry.AutomationTypes>(RubricEntry.AutomationTypes.values());
			automationCombo.setBackground(Color.WHITE);
			this.rubricElementListener = rubricElementListener;
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			boolean valid = true;
			if (rubricElementListener != null) {
				valid = rubricElementListener.typeSelected((RubricEntry.AutomationTypes)value, isSelected);
			}
			if (valid == true) {
				automationCombo.setSelectedItem(value);
			}
			return automationCombo;
		}

	}




	private class DescriptionAreaEditor extends DefaultCellEditor {

		private JTextArea textarea;
		private JScrollPane scrollPane;

		public DescriptionAreaEditor() {
			super(new JCheckBox());

			textarea = new JTextArea();
			textarea.setLineWrap(true);
			textarea.setWrapStyleWord(true);
			scrollPane = new JScrollPane(textarea);
			scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);


			textarea.setComponentPopupMenu(createPopupMenu());
			textarea.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void insertUpdate(DocumentEvent e) {
					updateRowHeight(textarea);

				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					updateRowHeight(textarea);

				}

				@Override
				public void changedUpdate(DocumentEvent e) {

				}

			});

		}

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			textarea.setText((String) value);
			return scrollPane;
		}

		public Object getCellEditorValue() {
			return textarea.getText();
		}
	}
	
	class DescriptionCellRenderer extends JScrollPane implements TableCellRenderer {
		JTextArea textArea;
		public DescriptionCellRenderer() {
			textArea = new JTextArea();
			textArea.setLineWrap(true);
			textArea.setWrapStyleWord(true);
			textArea.setOpaque(true);
			getViewport().add(textArea);
			setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		}

		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			if (isSelected) {
				setForeground(table.getSelectionForeground());
				setBackground(table.getSelectionBackground());
			} else {
				setForeground(table.getForeground());
				setBackground(table.getBackground());
			}
			setFont(table.getFont());

			textArea.setText((value == null) ? "" : value.toString());
			return this;
		}
	}

	private class RubricEntryTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 3651730049445837992L;
		public RubricEntryTableModel() {

		}

		public RubricEntry.HeadingNames getColumnHeading(int column) {
			column++;			
			return RubricEntry.HeadingNames.values()[column];	
		}

		@Override
		public int getRowCount() {
			int min = 3;
			if (associatedRubric != null && associatedRubric.getPointBreakdown() != null) {
				min = Math.max(min, associatedRubric.getPointBreakdown().size());
				min++;

			}
			return min;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return true;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {

			if (getColumnHeading(columnIndex) == HeadingNames.AUTOMATION_TYPE) {
				return RubricEntry.AutomationTypes.class;
			}	
			return String.class;

		}

		@Override
		public String getColumnName(int column) {
			switch(getColumnHeading(column)) {
			case AUTOMATION_TYPE:
				return "Automation Type";
			case DESCRIPTION:
				return "Description";		
			case NAME:
				return "Name";
			case VALUE:
				return "Val";
			default:
				break;				
			}
			return "";
		}

		@Override
		public int getColumnCount() {
			return HeadingNames.values().length - 1; // We don't want ID
		}
		@Override
		public Object getValueAt(int row, int column) {
			if (associatedRubric == null) {
				return null;
			}
			RubricEntry entry = associatedRubric.getEntry(row);
			Object retVal = entry.getTableValue(getColumnHeading(column));
			return retVal;
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			if (associatedRubric == null) {
				return;
			}
			RubricEntry entry = associatedRubric.getEntry(row);
			entry.setTableValue(getColumnHeading(column), aValue);
			if (row == getRowCount() - 1) {
				this.fireTableStructureChanged();
			}
		}

		@Override
		public void fireTableDataChanged() {			
			super.fireTableDataChanged();
			updateRowHeights();
		}

		@Override
		public void fireTableStructureChanged() {
			super.fireTableStructureChanged();
			updateRowHeights();
		}

	}

	public static void main(String args[]) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame a = new JFrame();
				JPanel x = new JPanel();
				x.setLayout(new BorderLayout());
				RubricEntriesTable t = new RubricEntriesTable(null);
				t.setAssociatedEntry(new Rubric());
				x.add(new JScrollPane(t));
				a.add(x);
				a.pack();
				a.setVisible(true);
			}
		});
	}
}
