package net.cdonald.googleClassroom.gui;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.StringTokenizer;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.DefaultEditorKit;

/**
 * pulled from
 * https://www.javaworld.com/article/2077579/java-tip-77--enable-copy-and-paste-functionality-between-swing-s-jtables-and-excel.html
 * ExcelAdapter enables Copy-Paste Clipboard functionality on JTables. The
 * clipboard data format used by the adapter is compatible with the clipboard
 * format used by Excel. This provides for clipboard interoperability between
 * enabled JTables and Excel.
 */
public class ExcelAdapter implements ActionListener {
	private String rowstring, value;
	private Clipboard system;
	private StringSelection stsel;
	private JTable jTable1;

	/**
	 * The Excel Adapter is constructed with a JTable on which it enables Copy-Paste
	 * and acts as a Clipboard listener.
	 */
	public ExcelAdapter(JTable myJTable) {
		jTable1 = myJTable;
		KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false);
		// Identifying the copy KeyStroke user can modify this
		// to copy on some other Key combination.
		KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK, false);
		// Identifying the Paste KeyStroke user can modify this
		// to copy on some other Key combination.
		KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, false);
		jTable1.registerKeyboardAction(this, "Copy", copy, JComponent.WHEN_FOCUSED);
		jTable1.registerKeyboardAction(this, "Paste", paste, JComponent.WHEN_FOCUSED);
		jTable1.registerKeyboardAction(this, "Delete", delete, JComponent.WHEN_FOCUSED);
		JPopupMenu popupMenu = new JPopupMenu();
		Action copy1 = new DefaultEditorKit.CopyAction();
		copy1.putValue(Action.NAME, "Copy");
		copy1.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control C"));
		JMenuItem copyMenu = new JMenuItem(copy1);
		copyMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copyAction();				
			}						
		});
		popupMenu.add(copyMenu);
		

		Action paste1 = new DefaultEditorKit.PasteAction();
		paste1.putValue(Action.NAME, "Paste");
		paste1.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke("control V"));
		JMenuItem pasteMenu = new JMenuItem(paste1);
		pasteMenu.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pasteAction();				
			}			
		});
		popupMenu.add(pasteMenu);
		
		jTable1.setComponentPopupMenu(popupMenu);
		system = Toolkit.getDefaultToolkit().getSystemClipboard();
	}

	/**
	 * Public Accessor methods for the Table on which this adapter acts.
	 */
	public JTable getJTable() {
		return jTable1;
	}

	public void setJTable(JTable jTable1) {
		this.jTable1 = jTable1;
	}

	/**
	 * This method is activated on the Keystrokes we are listening to in this
	 * implementation. Here it listens for Copy and Paste ActionCommands. Selections
	 * comprising non-adjacent cells result in invalid selection and then copy
	 * action cannot be performed. Paste is done by aligning the upper left corner
	 * of the selection with the 1st element in the current selection of the JTable.
	 */
	public void actionPerformed(ActionEvent e) {
		if (e.getActionCommand().compareTo("Copy") == 0) {
			copyAction();
		}
		if (e.getActionCommand().compareTo("Paste") == 0) {
			pasteAction();
		}
		if (e.getActionCommand().compareTo("Delete") == 0) {
			int[] rows = jTable1.getSelectedRows();
			int[] cols = jTable1.getSelectedColumns();
			if (rows != null && cols != null && rows.length > 0 && cols.length > 0) {
				for (int row : rows) {
					for (int col : cols) {
						if (jTable1.isCellEditable(row, col)) {
							jTable1.setValueAt(null, row, col);
						}
					}
				}
			}
			AbstractTableModel model = (AbstractTableModel)jTable1.getModel();
			model.fireTableDataChanged();
		}
	}
	
	private void copyAction() {
		StringBuffer sbf = new StringBuffer();
		// Check to ensure we have selected only a contiguous block of
		// cells
		int numcols = jTable1.getSelectedColumnCount();
		int numrows = jTable1.getSelectedRowCount();
		int[] rowsselected = jTable1.getSelectedRows();
		int[] colsselected = jTable1.getSelectedColumns();
		if (!((numrows - 1 == rowsselected[rowsselected.length - 1] - rowsselected[0]
				&& numrows == rowsselected.length)
				&& (numcols - 1 == colsselected[colsselected.length - 1] - colsselected[0]
						&& numcols == colsselected.length))) {
			JOptionPane.showMessageDialog(null, "Invalid Copy Selection", "Invalid Copy Selection",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		for (int i = 0; i < numrows; i++) {
			for (int j = 0; j < numcols; j++) {
				sbf.append(jTable1.getValueAt(rowsselected[i], colsselected[j]));
				if (j < numcols - 1)
					sbf.append("\t");
			}
			sbf.append("\n");
		}
		stsel = new StringSelection(sbf.toString());
		system = Toolkit.getDefaultToolkit().getSystemClipboard();
		system.setContents(stsel, stsel);
	}
	
	private void pasteAction() {
		int[] rows = jTable1.getSelectedRows();
		int[] cols = jTable1.getSelectedColumns();
		if (rows != null && cols != null && rows.length > 0 && cols.length > 0) {
			String trstring = "";
			try {
				trstring = (String) (system.getContents(this).getTransferData(DataFlavor.stringFlavor));
			} catch (Exception ex) {
				ex.printStackTrace();
			}				

			int rowIndex = 0;
			int colIndex = 0;

			int totalToConsume = cols.length * rows.length;
			while (totalToConsume > 0) {
				StringTokenizer st1 = new StringTokenizer(trstring, "\n");
				while (st1.hasMoreTokens() && totalToConsume > 0) {
					rowstring = st1.nextToken();
					rowIndex %= rows.length;
					int row = rows[rowIndex];
					StringTokenizer st2 = new StringTokenizer(rowstring, "\t");
					int entryValue = totalToConsume;
					while (st2.hasMoreTokens() && totalToConsume > 0) {							
						colIndex %= cols.length;
						int col = cols[colIndex];
						value = (String) st2.nextToken();							
						if (jTable1.isCellEditable(row, col)) {
							jTable1.setValueAt(value, row, col);
						}
						totalToConsume--;							
						colIndex++;
					}
					// Hit a case with nothing to copy.
					if (entryValue == totalToConsume) {
						totalToConsume = 0;
					}
					rowIndex++;
				}
			}
		}			
		AbstractTableModel model = (AbstractTableModel)jTable1.getModel();
		model.fireTableDataChanged();
	}
}