package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.undo.UndoManager;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RubricColumnChanged;
import net.cdonald.googleClassroom.listenerCoordinator.SelectStudentListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentInfoChangedListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.listenerCoordinator.StudentSelectedListener;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.StudentData;
import net.cdonald.googleClassroom.utils.SimpleUtils;

public class StudentPanel extends JPanel{
	private static final long serialVersionUID = 3480731067309159048L;
	private StudentListModel studentModel;
	private JTable studentTable;
	private StudentListRenderer studentListRenderer;
	private VerticalTableHeaderCellRenderer verticalHeaderRenderer;
	private volatile boolean resizing;
	private String currentStudent;	
	private GraderCommentCards commentPane;
	private JSplitPane splitPane;
	private int lastKeyboardCol;
	private boolean keyPressed;
	private UndoManager undoManager;


	public StudentPanel(UndoManager undoManager, StudentListInfo studentListInfo, int dividerLocation) {
		this.undoManager = undoManager;
		lastKeyboardCol = -1;
		keyPressed = false;
		studentModel = new StudentListModel(studentListInfo);
		studentTable = new JTable(studentModel);

		studentTable.setAutoCreateRowSorter(false);
		studentTable.setCellSelectionEnabled(true);
		studentTable.getTableHeader().setReorderingAllowed(false);
		studentListRenderer = new StudentListRenderer(studentListInfo);
		new ExcelAdapter(studentTable, false, true);

		verticalHeaderRenderer = new VerticalTableHeaderCellRenderer();
		studentTable.setDefaultRenderer(FileData.class, studentListRenderer);
		studentTable.setDefaultRenderer(CompilerMessage.class, studentListRenderer);
		studentTable.setDefaultRenderer(java.util.Date.class, studentListRenderer);
		studentTable.setDefaultRenderer(String.class, studentListRenderer);



		commentPane = new GraderCommentCards(undoManager);
		JPanel studentPanel = new JPanel();
		studentPanel.setLayout(new BorderLayout());
		studentPanel.add(new JScrollPane(studentTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
		studentTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, studentPanel, commentPane);
		splitPane.setResizeWeight(0.90);
		if (dividerLocation != 0) {
			splitPane.setDividerLocation(dividerLocation);
		}

		setLayout(new BorderLayout());
		add(splitPane, BorderLayout.CENTER);

		JTextArea notesArea = new JTextArea();
		notesArea.setWrapStyleWord(true);
		notesArea.setLineWrap(true);
		addListeners(studentListInfo);
		resizeColumns();

	}
	
	private void addListeners(StudentListInfo studentListInfo) {

		
		ListenerCoordinator.addListener(SelectStudentListener.class, new SelectStudentListener() {

			@Override
			public void fired(String studentID) {
				setSelectedStudent(studentID);				
			}			
		});

		studentTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

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
							Object student = studentModel.getValueAt(selectedRow, StudentListInfo.LAST_NAME_COLUMN);
							String studentId = null;							
							if (student != null) {
								StudentData studentInfo = (StudentData) student;
								studentId = studentInfo.getId();								
							}
							if (currentStudent != null) {
								UndoableStudentSelection.studentSelected(undoManager, studentId, currentStudent);
							}
							currentStudent = studentId;
						} else {
							currentStudent = null;
						}
						commentPane.switchComment(currentStudent);
						ListenerCoordinator.fire(StudentSelectedListener.class, currentStudent);
					}
				});
			}
		});
		
		
		
		studentTable.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					lastKeyboardCol = studentTable.columnAtPoint(e.getPoint());

				}

			}

			@Override
			public void mousePressed(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseReleased(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			@Override
			public void mouseExited(MouseEvent e) {
				// TODO Auto-generated method stub

			}

		});

		studentTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
			@Override
			public void columnAdded(TableColumnModelEvent e) {
			}

			@Override
			public void columnRemoved(TableColumnModelEvent e) {
			}

			@Override
			public void columnMoved(TableColumnModelEvent e) {
			}

			@Override
			public void columnMarginChanged(ChangeEvent e) {
			}

			@Override
			public void columnSelectionChanged(ListSelectionEvent e) {

				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				if (lsm.isSelectionEmpty() == false) {
					int maxCol = lsm.getMaxSelectionIndex();
					int minCol = lsm.getMinSelectionIndex();

					if (minCol == maxCol) {
						if (keyPressed) {
							minCol = lastKeyboardCol;
							maxCol = lastKeyboardCol;							
						}
						ListenerCoordinator.fire(RubricColumnChanged.class, minCol);
						String description = studentListInfo.getColumnTip(minCol);
						if (description == null) {
							description = "";
						}

						if (minCol < StudentListInfo.COMPILER_COLUMN) {
							studentTable.setColumnSelectionInterval(0, studentTable.getColumnCount() - 1);
						} else {
							studentTable.setColumnSelectionInterval(minCol, maxCol);
						}
					} else if (minCol < StudentListInfo.COMPILER_COLUMN && maxCol < studentTable.getColumnCount() - 1) {
						studentTable.setColumnSelectionInterval(lastKeyboardCol, lastKeyboardCol);
					}
				}
			}
		});

		ListenerCoordinator.addBlockingListener(StudentInfoChangedListener.class, new StudentInfoChangedListener() {
			public void fired() {
				structureChanged();
			}
		});
		

		
		ListenerCoordinator.addListener(StudentSelectedListener.class, new StudentSelectedListener() {

			@Override
			public void fired(String idToDisplay) {
				
				// TODO Auto-generated method stub
				
			}
			
		});

		studentTable.addKeyListener(new KeyListener() {
			@Override
			public void keyTyped(KeyEvent e) {
			}

			@Override
			public void keyPressed(KeyEvent e) {
				int keyCode = e.getKeyCode();
				keyPressed = true;
				boolean doSelect = false;
				if (keyCode == KeyEvent.VK_LEFT) {
					if (lastKeyboardCol != -1) {
						lastKeyboardCol--;
						if (lastKeyboardCol < 0) {
							lastKeyboardCol = 0;
						}
						else {
							doSelect = true;
						}
					}

				} else if (keyCode == KeyEvent.VK_RIGHT) {
					if (lastKeyboardCol != -1) {
						lastKeyboardCol++;
						doSelect = true;
					}
					if (lastKeyboardCol >= studentTable.getColumnCount()) {
						lastKeyboardCol = studentTable.getColumnCount() - 1;
						doSelect = false;
					}
					
				} else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
					doSelect = true;
				}
				if (doSelect) {
					studentTable.setColumnSelectionInterval(lastKeyboardCol, lastKeyboardCol);
				}

			}

			@Override
			public void keyReleased(KeyEvent e) {
				keyPressed = false;

			}
		});
	}

	public int getDividerLocation() {
		return splitPane.getDividerLocation();
	}


	private void resizeColumns() {
		if (resizing) {
			return;
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				resizing = true;
				for (int i = 0; i < studentModel.getColumnCount(); i++) {
					resizeColumn(i, null);
				}
				setHeaderRenderer();
				resizing = false;
			}
		});
	}

	private int getColumnWidth(int columnNum, Component component) {		
		int perLetterWidth = 6;
		Graphics g = component.getGraphics();
		FontMetrics fontMetrics = null;
		if (g != null) {
			fontMetrics = g.getFontMetrics();
			if (fontMetrics != null) {
				perLetterWidth = fontMetrics.charWidth('0');
			}
		}
		if (columnNum <= StudentListInfo.FIRST_NAME_COLUMN) {
			int longestString = 0;
			for (int i = 0; i < studentModel.getRowCount(); i++) {
				Object value = studentModel.getValueAt(i, columnNum);
				if (value != null && value instanceof StudentData) {
					StudentData student = (StudentData)value;
					String name = null;
					if (columnNum == 0) {
						name = student.getName();
					}
					else {
						name = student.getFirstName();
					}
					int nameDisplayLength = name.length() * perLetterWidth;
					if (fontMetrics != null) {
						nameDisplayLength = fontMetrics.stringWidth(name);
						if (name.length() > 10) {
							nameDisplayLength += perLetterWidth * 2;
						}
					}
					 
					longestString = Math.max(nameDisplayLength, longestString);
				}
			}
			if (longestString == 0) {
				longestString = 10 * perLetterWidth;
			}			
			return longestString;
		}
		else if (columnNum == StudentListInfo.COMPILER_COLUMN) {
			return perLetterWidth;						
		}
		else if (columnNum == StudentListInfo.DATE_COLUMN) {
			return SimpleUtils.DATE_PATTERN.length() * perLetterWidth;
		}
		else if (columnNum == StudentListInfo.TOTAL_COLUMN) {
			// Use 5 so that we have enough space for 999.9
			return 5 * perLetterWidth;
		}
		else {
			// Enough space for 99.9
			return 4 * perLetterWidth;
		}
	}

	private void resizeColumn(int columnNum, Object value) {
		TableColumnModel jTableColumnModel = studentTable.getColumnModel();
		if (jTableColumnModel != null) {
			if (columnNum < jTableColumnModel.getColumnCount()) {
				TableColumn column = jTableColumnModel.getColumn(columnNum);
				if (column != null) {
					int preferredWidth = getColumnWidth(columnNum, studentTable);
					preferredWidth += studentTable.getIntercellSpacing().width;
					column.setPreferredWidth(preferredWidth);
				}
			}
		}
	}
	
	

	private void setHeaderRenderer() {
		TableColumnModel columnModel = studentTable.getColumnModel();
		for (int i = 0; i < columnModel.getColumnCount(); i++) {
			columnModel.getColumn(i).setHeaderRenderer(verticalHeaderRenderer);
		}

	}

	public void clearStudents() {
		studentModel.clearAll();
	}

	public List<String> getSelectedIds() {
		int[] selectedRows = studentTable.getSelectedRows();
		List<String> ids = new ArrayList<String>();
		for (int i = 0; i < selectedRows.length; i++) {
			Object student = studentModel.getValueAt(selectedRows[i], StudentListInfo.LAST_NAME_COLUMN);
			if (student != null) {
				ids.add(((StudentData) student).getId());
			}
		}
		return ids;
	}

	public void setSelectedStudent(String studentID) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				if (!studentID.equals(currentStudent)) {
					for (int i = 0; i < studentModel.getRowCount(); i++) {
						Object studentObj = studentModel.getValueAt(i, StudentListInfo.LAST_NAME_COLUMN);
						if (studentObj != null) {
							StudentData studentData = (StudentData) studentObj;

							if (studentID.equals(studentData.getId())) {
								studentTable.setRowSelectionInterval(i, i);
								studentTable.setColumnSelectionInterval(0, 0);
								lastKeyboardCol = 0;
							}
						}
					}
				}
			}
		});
	}

	public void setRubric(Rubric rubric) {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				setHeaderRenderer();
				revalidate();
				resizeColumns();
			}

		});
	}

	public void dataChanged() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				studentModel.fireTableDataChanged();				
			}
		});
		//resizeColumns();

	}

	public void structureChanged() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				studentModel.structureChanged();
				setHeaderRenderer();
				revalidate();
				resizeColumns();
			}
		});
	}

	public void stopEditing() {
		if (studentTable.getCellEditor() != null) {
			studentTable.getCellEditor().stopCellEditing();
		}
	}

	public boolean isAnyStudentSelected() {
		return currentStudent != null;
	}

	public Set<String> getSelectedColumns() {
		Set<String> selectedCols = new HashSet<String>();
		for (int col : studentTable.getSelectedColumns()) {
			selectedCols.add(studentModel.getColumnName(col));
		}
		return selectedCols;
	}

}
