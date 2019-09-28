package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

import net.cdonald.googleClassroom.model.Rubric;

public class RubricEntryPointBreakdownTable extends JTable {
	private PointBreakdownTableModel pointBreakdownModel;
	private Rubric associatedRubric;
	private boolean isEditable;
	private static final String enterActionKey = "Enter_Action";
	public RubricEntryPointBreakdownTable(boolean isEditable) {
		super();
		this.isEditable = isEditable;
		setDefaultRenderer(String.class, new MultiLineCellRenderer());
		setDefaultEditor(String.class, new TextAreaEditor());
		setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		pointBreakdownModel = new PointBreakdownTableModel();
		setModel(pointBreakdownModel);
		JLabel val = new JLabel("Val");
		getColumnModel().getColumn(0).setMaxWidth((int)(val.getPreferredSize().getWidth() * 2));
		if (isEditable) {
			setComponentPopupMenu(createPopupMenu());
		}
		else {
			setBackground(val.getBackground());
		}
		setSelectionBackground(getSelectionBackground());
		setCellSelectionEnabled(true);
		new ExcelAdapter(this, true);
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
				int selectedIndex = selectionModel.getMinSelectionIndex();
				associatedRubric.removePointBreakdownDescription(selectedIndex);
				pointBreakdownModel.fireTableDataChanged();
				
			}			
		});

		moveUpItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopEditing();
				int selectedIndex = selectionModel.getMinSelectionIndex();
				associatedRubric.swapPointBreakdownDescriptions(selectedIndex, selectedIndex -1);
				pointBreakdownModel.fireTableDataChanged();
				
			}			
		});
		moveDownItem.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				stopEditing();
				int selectedIndex = selectionModel.getMinSelectionIndex();
				associatedRubric.swapPointBreakdownDescriptions(selectedIndex, selectedIndex + 1);
				pointBreakdownModel.fireTableDataChanged();
			}			
		});
		insertAbove.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopEditing();
				int selectedIndex = selectionModel.getMinSelectionIndex();
				associatedRubric.addNewPointBreakdownDescription(selectedIndex);
				pointBreakdownModel.fireTableDataChanged();
			}			
		});
		insertBelow.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopEditing();
				int selectedIndex = selectionModel.getMinSelectionIndex();
				associatedRubric.addNewPointBreakdownDescription(selectedIndex + 1);
				pointBreakdownModel.fireTableDataChanged();

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
				pointBreakdownModel.fireTableDataChanged();		
				updateRowHeights();				
			}
		});
		
	}
	
	public void stopEditing() {
		if (isEditable) {
			if (getCellEditor() != null) {
				getCellEditor().stopCellEditing();
			}
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

					int newRowHeight = area.getLineCount();

					Graphics g = area.getGraphics();
					if (g != null) {
						FontMetrics f = g.getFontMetrics(area.getFont());
						if (f != null) {
							newRowHeight *= f.getHeight();
						}
					}
					if (newRowHeight > getRowHeight()) {
						setRowHeight(selectedIndex, newRowHeight);
					}

				}
			}
		});
	}
	
	class MultiLineCellRenderer extends JTextArea implements TableCellRenderer {

		public MultiLineCellRenderer() {
			setLineWrap(true);
			setWrapStyleWord(true);
			setOpaque(true);
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

			setText((value == null) ? "" : value.toString());
			return this;
		}
	}


	class TextAreaEditor extends DefaultCellEditor {

		protected JTextArea textarea;

		public TextAreaEditor() {
			super(new JCheckBox());
			
			textarea = new JTextArea();
			textarea.setLineWrap(true);
			textarea.setWrapStyleWord(true);
			
			if (isEditable) {
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
		}

		public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
				int column) {
			textarea.setText((String) value);
			return textarea;
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
			if (associatedRubric != null && associatedRubric.getPointBreakdown() != null) {
				min = Math.max(min, associatedRubric.getPointBreakdown().size());
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
				return "Val";
			}
			return "Description";
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public Object getValueAt(int row, int column) {
			if (associatedRubric == null) {
				return null;
			}
			if (column == 0) {
				return associatedRubric.getPointBreakdownValue(row);
			}
			String description = associatedRubric.getPointBreakdownDescription(row);
			return description;
		}

		@Override
		public void setValueAt(Object aValue, int row, int column) {
			if (associatedRubric == null) {
				return;
			}
			if (column == 0) {
				associatedRubric.setPointBreakdownValue(row, aValue.toString());
			} else {
				String temp = (String) aValue;			
				associatedRubric.setPointBreakdownDescription(row, temp);
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
				RubricEntryPointBreakdownTable t = new RubricEntryPointBreakdownTable(true);
				t.setAssociatedEntry(new Rubric());
				x.add(new JScrollPane(t));
				a.add(x);
				a.pack();
				a.setVisible(true);
			}
		});
		
		
	}

}