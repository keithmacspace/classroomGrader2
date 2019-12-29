package net.cdonald.googleClassroom.gui;

import java.awt.Toolkit;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.DefaultEditorKit;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;  
/**
 * pulled from
 * https://www.javaworld.com/article/2077579/java-tip-77--enable-copy-and-paste-functionality-between-swing-s-jtables-and-excel.html
 * ExcelAdapter enables Copy-Paste Clipboard functionality on JTables. The
 * clipboard data format used by the adapter is compatible with the clipboard
 * format used by Excel. This provides for clipboard interoperability between
 * enabled JTables and Excel.
 */
public class ExcelAdapter  {	
	private Clipboard system;
	private StringSelection stsel;
	private JTable jTable1;
	private boolean expandRows;

	/**
	 * The Excel Adapter is constructed with a JTable on which it enables Copy-Paste
	 * and acts as a Clipboard listener.
	 */
	public ExcelAdapter(JTable myJTable, boolean expandRows, boolean addPopup) {
		jTable1 = myJTable;
		this.expandRows = expandRows;
		system = Toolkit.getDefaultToolkit().getSystemClipboard();
		if (addPopup) {
			JPopupMenu popupMenu = new JPopupMenu();
			addPopupOptions(popupMenu);
			myJTable.setComponentPopupMenu(popupMenu);
		}

	}
	private void registerKeyboardAction(String name, KeyStroke key, AbstractAction action) {
		jTable1.getInputMap(JComponent.WHEN_FOCUSED).put(key, name);
		jTable1.getActionMap().put(name, action);
	}
	
	@SuppressWarnings("serial")
	public void addPopupOptions(JPopupMenu popupMenu) {
		KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false);
		// Identifying the copy KeyStroke user can modify this
		// to copy on some other Key combination.
		KeyStroke paste = KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK, false);
		// Identifying the Paste KeyStroke user can modify this
		// to copy on some other Key combination.
		KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, false);
		KeyStroke backSpace = KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0, false);
		
		registerKeyboardAction("Copy", copy, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				copyAction();				
			}			
		});
		registerKeyboardAction("Paste", paste, new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				pasteAction();				
			}			
		});
		AbstractAction deleteAction = new AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent e) {
				deleteAction();				
			}			
		};
		registerKeyboardAction("Delete", delete, deleteAction);
		registerKeyboardAction("Backspace", backSpace, deleteAction);
		if (popupMenu == null) {
			popupMenu = new JPopupMenu();
		}
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
		if ((e.getActionCommand().compareTo("Delete") == 0) || (e.getActionCommand().compareTo("Backspace") == 0)){
		}
	}
	
	public void deleteAction() {
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
		jTable1.setRowSelectionInterval(rows[0], rows[rows.length - 1]);
		jTable1.setColumnSelectionInterval(cols[0], cols[cols.length - 1]);
		
	}
	
	public void copyAction() {
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
	
	public void pasteAction() {
		int[] rows = jTable1.getSelectedRows();
		int[] cols = jTable1.getSelectedColumns();

		if (rows != null && cols != null && rows.length > 0 && cols.length > 0) {
			String trstring = "";
			String htmlString = null;
			try {
				trstring = (String) (system.getContents(this).getTransferData(DataFlavor.stringFlavor));
				htmlString = (String)(system.getContents(this).getTransferData(DataFlavor.allHtmlFlavor));

			} catch (Exception ex) {
				DebugLogDialog.appendException(ex);

			}
			List<List<String> > entries = null;
			if (htmlString != null) {
				entries = parseClipHTMLContents(htmlString);
			}
			else {
				entries = parseClipTextContents(trstring);
			}
			
			int currentRow = rows[0];
			int numRows = rows.length;
			AbstractTableModel model = (AbstractTableModel)jTable1.getModel();

			for (List<String> col : entries) {
				if (numRows > 0 || expandRows ) {
					for (int colIndex = 0, entryColIndex = 0; colIndex < cols.length && entryColIndex < col.size(); colIndex++, entryColIndex++) {
						String colValue = col.get(entryColIndex);
						int colNum = cols[colIndex];
						if (expandRows == true && model.getRowCount() <= currentRow) {
							((DefaultTableModel)model).addRow((Object []) null); 
							
						}

						if (jTable1.isCellEditable(currentRow, colNum)) {
							jTable1.setValueAt(colValue, currentRow, colNum);
						}

					}
				}
				currentRow++;
				numRows--;
			}
			
			model.fireTableDataChanged();
			jTable1.setRowSelectionInterval(rows[0], rows[rows.length - 1]);
			jTable1.setColumnSelectionInterval(cols[0], cols[cols.length - 1]);

		}			
	}
	public static List<List<String> > parseClipTextContents(String contents) {
		List<List<String> > entries = new ArrayList<List<String>>();
		
		StringTokenizer st1 = new StringTokenizer(contents, "\n");
		while (st1.hasMoreTokens()) {
			String rowstring = st1.nextToken();
			StringTokenizer st2 = new StringTokenizer(rowstring, "\t");
			List<String> colList = new ArrayList<String>();
			while (st2.hasMoreTokens()) {							
				String colValue = (String) st2.nextToken();
				colList.add(colValue);
			}
			entries.add(colList);
		}
		return entries;
	}
	
	
	public static List<List<String> > parseClipHTMLContents(String contents) {
		List<List<String> > entries = new ArrayList<List<String>>();
		try {
		
			Document doc = Jsoup.parseBodyFragment(contents);
			doc.outputSettings().prettyPrint(false);
			
			Elements metas = doc.select("meta");
			String lineBreak = "span";
			for (Element meta : metas) {
				if (meta.hasAttr("name")) {					
					if (meta.attr("name").toLowerCase().contains("generator")) {
						if (meta.hasAttr("content")) {
							String gen = meta.attr("content").toLowerCase();
							if (gen.contains("sheets")) {
								lineBreak = null;
							}
						}
					}
				}
			}
			Elements table = doc.select("table");

			Elements rows = table.select("tr");
			for (Element row : rows) {
				List<String> colList = new ArrayList<String>();
				Elements cols = row.select("td");
				for (Element col : cols) {					
					String str = "";
					if (lineBreak != null) {
						Elements lines = col.select(lineBreak);
						if (lines.size() == 0) {
							str = col.text();
						}
						for (int i = 0; i < lines.size(); i++) {
							Element div = lines.get(i);
							String line = div.ownText();
							line = line.stripLeading();
							line = line.stripTrailing();
							if (line.length() > 0) {
								if (str.length() != 0) {
									str += "\n";
								}
								str += line;
							}
						}
					}
					else {						
						str = col.text();
					}
					colList.add(str);
				}
				entries.add(colList);
			}
		} catch (Exception e) {
			DebugLogDialog.appendException(e);
		}
		return entries;
	}
}