package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventObject;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import net.cdonald.googleClassroom.listenerCoordinator.AddRubricTabsListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntry.HeadingNames;


@SuppressWarnings("serial")
public class RubricEntriesTable extends JTable {
	private RubricEntryTableModel rubricEntryTableModel;
	private Rubric associatedRubric;
	private ExcelAdapter excelAdapter;
	private static final RubricEntry.HeadingNames [] headingNames = {RubricEntry.HeadingNames.NAME, RubricEntry.HeadingNames.VALUE, RubricEntry.HeadingNames.AUTOMATION_TYPE};
	private boolean editingEnabled;
	private RubricElementListener rubricElementListener;

	public RubricEntriesTable(RubricElementListener listener) {
		super();
		this.rubricElementListener = listener;

		setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		rubricEntryTableModel = new RubricEntryTableModel();
		setModel(rubricEntryTableModel);

		for (int i = 0; i < rubricEntryTableModel.getColumnCount(); i++) {
			TableColumn column = getColumnModel().getColumn(i);
			switch (rubricEntryTableModel.getColumnHeading(i)) {
			case AUTOMATION_TYPE:
				column.setCellEditor(new RubricAutomationEditor());
				column.setCellRenderer(new AutomationCellRenderer());
				break;
			case NAME:
				break;
			case VALUE:
				break;
			default:
				break;
			}
		}		
		setSelectionBackground(getSelectionBackground());
		setCellSelectionEnabled(true);
		addListeners();
		excelAdapter = new ExcelAdapter(this, true, false);
		setComponentPopupMenu(createPopupMenu());
	}
	
	private void addListeners() {

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
		excelAdapter.addPopupOptions(rightClickPopup);
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
					ListenerCoordinator.fire(AddRubricTabsListener.class, associatedRubric, true);
				}

			}
		});

		moveUpItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopEditing();
				if (associatedRubric != null) {
					int selectedIndex = selectionModel.getMinSelectionIndex();
					associatedRubric.swapEntries(selectedIndex, selectedIndex - 1);
					rubricEntryTableModel.fireTableDataChanged();
					ListenerCoordinator.fire(AddRubricTabsListener.class, associatedRubric, true);
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
					ListenerCoordinator.fire(AddRubricTabsListener.class, associatedRubric, true);
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
					ListenerCoordinator.fire(AddRubricTabsListener.class, associatedRubric, true);
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
					ListenerCoordinator.fire(AddRubricTabsListener.class, associatedRubric, true);
				}
			}
		});
		return rightClickPopup;
	}

	public Rubric getAssociatedRubric() {
		return associatedRubric;
	}

	public void setAssociatedRubric(Rubric associatedEntry) {
		associatedRubric = associatedEntry;
		fireTableDataChanged();

	}

	public boolean isEditingEnabled() {
		return editingEnabled;
	}

	public void setEditingEnabled(boolean editingEnabled) {
		this.editingEnabled = editingEnabled;
	}

	public void stopEditing() {
		if (getCellEditor() != null) {
			getCellEditor().stopCellEditing();
		}
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
		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int colum) {
			boolean valid = true;
			if (rubricElementListener != null) {
				valid = rubricElementListener.typeSelected((RubricEntry.AutomationTypes) value, row, isSelected);
			}
			if (valid == true) {
				combo.setSelectedItem(value);
			}
			else {
			}

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

		private JComboBox<RubricEntry.AutomationTypes> automationCombo;

		public AutomationCellRenderer() {
			automationCombo = new JComboBox<RubricEntry.AutomationTypes>(RubricEntry.AutomationTypes.values());
			automationCombo.setBackground(Color.WHITE);			
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
			boolean valid = true;
			if (rubricElementListener != null) {
				valid = rubricElementListener.typeSelected((RubricEntry.AutomationTypes) value, row, hasFocus);
			}
			if (valid == true) {
				automationCombo.setSelectedItem(value);
			}
			else {
				automationCombo.setSelectedIndex(0);
			}
			return automationCombo;
		}

	}



	private class RubricEntryTableModel extends DefaultTableModel {
		private static final long serialVersionUID = 3651730049445837992L;		

		public RubricEntryTableModel() {

		}

		public RubricEntry.HeadingNames getColumnHeading(int column) {			
			return headingNames[column];
		}

		@Override
		public int getRowCount() {
			int min = 30;
			if (associatedRubric != null) {
				min = Math.max(min, associatedRubric.getEntryCount());
				min++;				
			}
			return min;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return editingEnabled;
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
			switch (getColumnHeading(column)) {
			case AUTOMATION_TYPE:
				return "Automation Type";
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
			return headingNames.length;
		}

		@Override
		public Object getValueAt(int row, int column) {
			if (associatedRubric == null) {
				return null;
			}
			RubricEntry entry = associatedRubric.getEntry(row);
			Object retVal = null;
			if (entry != null) {
				retVal = entry.getTableValue(getColumnHeading(column));
			}
			return retVal;
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			if (associatedRubric == null) {
				return;
			}			
			
			RubricEntry entry = associatedRubric.getEntry(row);			
			if (entry != null) {
				if ((entry.getName() == null || entry.getName().length() == 0) ||
					 entry.getValue() == 0){
					ListenerCoordinator.fire(AddRubricTabsListener.class, associatedRubric, true);		
				}
				else {
					ListenerCoordinator.fire(AddRubricTabsListener.class, associatedRubric, false);		
				}
				entry.setTableValue(getColumnHeading(column), aValue);
				if (row == getRowCount() - 1) {
					Object [] rowData = {null, null, null, null};
					this.addRow(rowData);
				}
			}
		}

		@Override
		public void fireTableDataChanged() {
			super.fireTableDataChanged();

		}

		@Override
		public void fireTableStructureChanged() {
			super.fireTableStructureChanged();

		}

	}

	public static void main(String args[]) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JFrame a = new JFrame();
				JPanel x = new JPanel();
				x.setLayout(new BorderLayout());
				Rubric rubric = new Rubric();
				rubric.addNewEntry(0);
				RubricEntry e = rubric.getEntry(0);
				e.setDescription("This is a longish description which should require that we wrap.  I have to figure out what is causing my components to get so messed up");
				
				
				RubricEntriesTable t = new RubricEntriesTable(null);
				t.setAssociatedRubric(rubric);
				x.add(new JScrollPane(t));
				a.add(x);
				a.pack();
				a.setVisible(true);
			}
		});
	}
}
