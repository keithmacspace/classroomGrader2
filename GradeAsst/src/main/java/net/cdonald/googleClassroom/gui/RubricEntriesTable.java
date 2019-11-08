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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;


import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntry.HeadingNames;


@SuppressWarnings("serial")
public class RubricEntriesTable extends JTable {
	private RubricEntryTableModel rubricEntryTableModel;
	private Rubric associatedRubric;
	private int defaultHeight;
	private ExcelAdapter excelAdapter;


	public RubricEntriesTable(RubricElementListener listener) {
		super();

		setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		rubricEntryTableModel = new RubricEntryTableModel();
		setModel(rubricEntryTableModel);

		for (int i = 0; i < rubricEntryTableModel.getColumnCount(); i++) {
			TableColumn column = getColumnModel().getColumn(i);
			switch (rubricEntryTableModel.getColumnHeading(i)) {
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
		setSelectionBackground(getSelectionBackground());
		setCellSelectionEnabled(true);
		addListeners();
		defaultHeight = -1;
		excelAdapter = new ExcelAdapter(this, true, false);
		setComponentPopupMenu(createPopupMenu());
	}
	
	private void addListeners() {
		getSelectionModel().addListSelectionListener(new ListSelectionListener() {

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (e.getValueIsAdjusting()) {
					return;
				}
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						ListSelectionModel lsm = (ListSelectionModel) e.getSource();		
						if (lsm.isSelectionEmpty() == false) {
							int selectedRow = lsm.getMinSelectionIndex();
							updateRowHeights(selectedRow);
						}

					}
				});
			}
		});
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
		associatedRubric = associatedEntry;
		fireTableDataChanged();
		updateRowHeights(-1);
	}

	public void stopEditing() {

		if (getCellEditor() != null) {
			getCellEditor().stopCellEditing();
		}

	}

	public void updateRowHeights(int selectedRow) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int automationCol = 0;
				int descriptionCol = 0;
				for (int column = 0; column < getColumnCount(); column++) {
					if (rubricEntryTableModel.getColumnHeading(column) == RubricEntry.HeadingNames.DESCRIPTION )  {
						descriptionCol = column;
					}
					if (rubricEntryTableModel.getColumnHeading(column) == RubricEntry.HeadingNames.AUTOMATION_TYPE) {
						automationCol = column;
					}
				}

				if (defaultHeight == -1) {
					Component comp = prepareRenderer(getCellRenderer(0, automationCol), 0, automationCol);				            
					defaultHeight = comp.getPreferredSize().height;
				}
				for (int row = 0; row < getRowCount(); row++) {
					int rowHeight = defaultHeight;
					if (row == selectedRow) {
						Component comp = prepareRenderer(getCellRenderer(row, descriptionCol), row, descriptionCol);
						rowHeight = Math.max(rowHeight, comp.getPreferredSize().height);
					}

					setRowHeight(row, rowHeight);
				}						
				
			}

		});
	}
	
	public void updateRowHeight() {
		updateRowHeights(this.getSelectedRow());
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
				valid = rubricElementListener.typeSelected((RubricEntry.AutomationTypes) value, isSelected);
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

			//textarea.setComponentPopupMenu(createPopupMenu());
			textarea.getDocument().addDocumentListener(new DocumentListener() {

				@Override
				public void insertUpdate(DocumentEvent e) {
					updateRowHeight();

				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					updateRowHeight();

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

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {
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
			int min = 30;
			if (associatedRubric != null) {
				min = Math.max(min, associatedRubric.getEntryCount());
				min++;
				System.err.println(min);
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
			switch (getColumnHeading(column)) {
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
				entry.setTableValue(getColumnHeading(column), aValue);
				if (row == getRowCount() - 1) {
					Object [] rowData = {null, null, null, null};
					this.addRow(rowData);
					//associatedRubric.addNewEntry();
				}
			}
		}

		@Override
		public void fireTableDataChanged() {
			super.fireTableDataChanged();
			updateRowHeights(-1);

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
				t.setAssociatedEntry(rubric);
				x.add(new JScrollPane(t));
				a.add(x);
				a.pack();
				a.setVisible(true);
			}
		});
	}
}
