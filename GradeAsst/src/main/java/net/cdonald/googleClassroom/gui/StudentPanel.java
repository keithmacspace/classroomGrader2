package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import net.cdonald.googleClassroom.googleClassroomInterface.SaveSheetGrades;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.listenerCoordinator.AssignmentSelected;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SetInfoLabelListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentInfoChangedListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentListInfo;
import net.cdonald.googleClassroom.listenerCoordinator.StudentSelectedListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.StudentData;
import net.cdonald.googleClassroom.utils.SimpleUtils;

public class StudentPanel extends JPanel implements ResizeAfterUpdateListener{
	private static final long serialVersionUID = 3480731067309159048L;
	private StudentListModel studentModel;
	private JTable studentTable;
	private StudentListRenderer studentListRenderer;	
	private VerticalTableHeaderCellRenderer verticalHeaderRenderer;
	private volatile boolean resizing;
	private Map<String, JTextArea> notesAndCommentsTextArea;
	private Map<String, String> notesAndCommentsMap;
	private Map<String, Map<String, String>> otherComments;
	private String currentStudent;
	private JTabbedPane commentTabs;
	private String currentGrader;
	private TitledBorder notesTitle;
	private static final String DEFAULT_NOTES_HEADER = "Notes/Comments";
	private JPanel commentPane;
	private JSplitPane splitPane;
	private int defaultValueWidth;

	private int lastKeyboardCol;
	private boolean keyPressed;
	private static final int OTHER_COL_BASE_SIZE = 15;
	public StudentPanel(StudentListInfo studentListInfo, int dividerLocation) {
		this.otherComments = studentListInfo.getNotesCommentsMap();
		this.currentGrader = studentListInfo.getUserName();
		this.notesAndCommentsMap = studentListInfo.getNotesCommentsMap().get(currentGrader);
		lastKeyboardCol = -1;
		keyPressed = false;
		defaultValueWidth = OTHER_COL_BASE_SIZE;
		studentModel = new StudentListModel(studentListInfo, this);
		studentTable = new JTable(studentModel) {
			private static final long serialVersionUID = 1L;
			//Implement table header tool tips. 
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
					private static final long serialVersionUID = 1L;
					public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        return studentListInfo.getColumnTip(index);
                    }
                };
            }            
		};		
		studentTable.setAutoCreateRowSorter(false);
		studentTable.setCellSelectionEnabled(true);
		studentTable.getTableHeader().setReorderingAllowed(false);				
		studentListRenderer = new StudentListRenderer();
		new ExcelAdapter(studentTable);

		
		verticalHeaderRenderer = new VerticalTableHeaderCellRenderer();
		studentTable.setDefaultRenderer(FileData.class, studentListRenderer);
		studentTable.setDefaultRenderer(CompilerMessage.class, studentListRenderer);
		studentTable.setDefaultRenderer(java.util.Date.class, studentListRenderer);		

		notesAndCommentsTextArea = new HashMap<String, JTextArea>();


		commentPane = new JPanel();	
		commentPane.setLayout(new BorderLayout());
		notesTitle = BorderFactory.createTitledBorder(DEFAULT_NOTES_HEADER);
		commentPane.setBorder(notesTitle);
		commentTabs = new JTabbedPane();		
		commentPane.add(commentTabs, BorderLayout.CENTER);
		JPanel studentPanel = new JPanel();
		studentPanel.setLayout(new BorderLayout());		
		studentPanel.add(new JScrollPane(studentTable, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED), BorderLayout.CENTER);
		studentTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);


		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, studentPanel, commentPane);
		splitPane.setResizeWeight(0.90);
		if (dividerLocation != 0) {
			splitPane.setDividerLocation(dividerLocation);
		}


		setLayout(new BorderLayout());
		add(splitPane, BorderLayout.CENTER);

		


		//studentList.setRowHeight(25);
		notesAndCommentsTextArea.put(currentGrader, new JTextArea());
		JTextArea userComments = notesAndCommentsTextArea.get(currentGrader);
		

		userComments.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				if (currentStudent != null) {
					notesAndCommentsMap.put(currentStudent, userComments.getText());					
				}
			}

			@Override
			public void removeUpdate(DocumentEvent e) {				
				if (currentStudent != null) {
					notesAndCommentsMap.put(currentStudent, userComments.getText());
				}
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
	
			}
			
		});

		addComponentListener( new ComponentListener() {
	        @Override
	        public void componentResized(ComponentEvent e) {
	        	SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {					   
						resizeColumns();						
					}
	        		
	        	});	            
	        }
			@Override
			public void componentMoved(ComponentEvent e) {				
			}

			@Override 
			public void componentShown(ComponentEvent e) {				
			}

			@Override
			public void componentHidden(ComponentEvent e) {
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
						ListSelectionModel lsm = (ListSelectionModel)e.getSource();
						if (lsm.isSelectionEmpty() == false) {
							int selectedRow = lsm.getMinSelectionIndex();

							Object student = studentModel.getValueAt(selectedRow, StudentListInfo.LAST_NAME_COLUMN);
							String studentId = null;
							currentStudent = null;					
							if (student != null) {
								StudentData studentInfo = (StudentData)student;
								studentId = studentInfo.getId();
								notesTitle.setTitle(DEFAULT_NOTES_HEADER + ": " + studentInfo.getFirstName() + " " + studentInfo.getName());
							}
							addCommentAreas(studentId);
							for (int tab = 0; tab < commentTabs.getTabCount(); tab++) {
								String graderName = commentTabs.getTitleAt(tab);						
								Map<String, String> commentMap = otherComments.get(graderName);
								JTextArea commentArea = notesAndCommentsTextArea.get(graderName);
								if (studentId != null && commentMap.containsKey(studentId)) {
									commentArea.setText(commentMap.get(studentId));
								}
								else {
									commentArea.setText("");
								}
							}

							currentStudent = studentId;
						}
						else {
							notesTitle.setTitle(DEFAULT_NOTES_HEADER);
							currentStudent = null;
							addCommentAreas(null);
						}
						commentPane.repaint();
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
				
				ListSelectionModel lsm = (ListSelectionModel)e.getSource();
				if (lsm.isSelectionEmpty() == false) {
					int maxCol = lsm.getMaxSelectionIndex();
					int minCol = lsm.getMinSelectionIndex();				

					if (minCol == maxCol) {
						if (keyPressed) {
							minCol = lastKeyboardCol;
							maxCol = lastKeyboardCol;
						}
						String tip = studentListInfo.getColumnTip(minCol);
						if (tip == null) {
							tip = "";
						}
					
						ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUBRIC_INFO, tip);						
						if (minCol < StudentListInfo.COMPILER_COLUMN) {
							studentTable.setColumnSelectionInterval(0, studentTable.getColumnCount() - 1);
						}
						else {
							studentTable.setColumnSelectionInterval(minCol, maxCol);
						}
					}
					else if (minCol < StudentListInfo.COMPILER_COLUMN && maxCol < studentTable.getColumnCount() - 1) {
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
		
		ListenerCoordinator.addListener(AssignmentSelected.class, new AssignmentSelected() {
			@Override
			public void fired(ClassroomData data) {
				if (data != null) {
					studentListRenderer.setDueDate(data.getDate());
				}
				else {
					studentListRenderer.setDueDate(null);
				}
				
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
					}
					doSelect = true;
				}
				else if (keyCode == KeyEvent.VK_RIGHT) {
					if (lastKeyboardCol != -1) {
						lastKeyboardCol++;
					}
					doSelect = true;
				}
				else if (keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN) {
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
	
	private void addCommentAreas(String studentId) {
		commentTabs.removeAll();
		if (studentId != null) {
			addCommentArea(currentGrader, true);
			for (String key : otherComments.keySet()) {
				if (key.equalsIgnoreCase(currentGrader) == false) {
						addCommentArea(key, false);
					
				}
			}
		}
	}
	
	private void addCommentArea(String title, boolean editable) {
		JTextArea commentArea = notesAndCommentsTextArea.get(title);
		if (commentArea == null) {
			commentArea = new JTextArea();
			notesAndCommentsTextArea.put(title, commentArea);
		}
		
		commentArea.setEditable(editable);
		commentTabs.addTab(title, new JScrollPane(commentArea));
		
	}
	



	private void resizeColumns() {
		if (resizing) {
			return;
		}

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				resizing = true;
				for (int i = 0; i < StudentListInfo.COMPILER_COLUMN; i++) {
					resizeColumn(i);
				}
				resizeValueColumns();				
				setHeaderRenderer();
				resizing = false;
			}
		});
	}
	
	private void resizeValueColumns() {
		TableColumnModel jTableColumnModel = studentTable.getColumnModel();
		int numCols = jTableColumnModel.getColumnCount();
		for (int i = StudentListInfo.COMPILER_COLUMN; i < numCols; i++) {
			resizeColumn(i);
		}
	}
	
	public void resizeColumn(int columnNum) {
		int numRows = studentModel.getRowCount();
		for (int row = 0; row < numRows; row++) {
			Object value = studentModel.getValueAt(row, columnNum);
			resizeColumn(columnNum, value, row == 0);
		}
	}
	
	@Override
	public void resizeColumn(int columnNum, Object value, boolean reduceSize) {
		TableColumnModel jTableColumnModel = studentTable.getColumnModel();
		TableColumn column = jTableColumnModel.getColumn(columnNum);
		if (column != null) {
			column.setMinWidth(0);
			if (columnNum >= StudentListInfo.COMPILER_COLUMN || columnNum == StudentListInfo.DATE_COLUMN) {			
				final int TOTAL_COL_BASE_SIZE = 35;
				final int PER_LETTER_MULTIPLIER = 6;

				int baseSize = (reduceSize) ? 0 : column.getPreferredWidth();			
				if (columnNum == StudentListInfo.TOTAL_COLUMN) {
					baseSize = Math.max(TOTAL_COL_BASE_SIZE, baseSize);
				}
				else {
					baseSize = Math.max(defaultValueWidth, baseSize);
				}
				if (value != null) {
					String valueString = value.toString();
					if (columnNum ==StudentListInfo.DATE_COLUMN && value instanceof java.util.Date) {
						valueString = SimpleUtils.formatDate((Date) value);
					}
					if (columnNum == StudentListInfo.COMPILER_COLUMN) {
						valueString = "Y";
					}

					baseSize = Math.max(PER_LETTER_MULTIPLIER * valueString.length(), baseSize);
				}
				if (columnNum > StudentListInfo.TOTAL_COLUMN) {
					if (baseSize > defaultValueWidth) {
						defaultValueWidth = baseSize;
						resizeValueColumns();
						return;
					}
				}
				column.setPreferredWidth(baseSize);			
			}
			else {
				final int FIXED_PREF_SIZE = 90;
				column.setMinWidth(0);
				column.setPreferredWidth(FIXED_PREF_SIZE);
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
		int [] selectedRows = studentTable.getSelectedRows();
		List<String> ids = new ArrayList<String>();		
		for (int i = 0; i < selectedRows.length; i++) {
			Object student = studentModel.getValueAt(selectedRows[i], StudentListInfo.LAST_NAME_COLUMN);
			if (student != null) {
				ids.add(((StudentData)student).getId()); 
			}
		}
		return ids;
	}
	
	
	public void setSelectedStudent(String studentID) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				for (int i = 0; i < studentModel.getRowCount(); i++) {
					Object studentObj = studentModel.getValueAt(i, StudentListInfo.LAST_NAME_COLUMN);
					if (studentObj != null) {
						StudentData studentData = (StudentData)studentObj;
						
						if (studentID.equals(studentData.getId())){
							studentTable.setRowSelectionInterval(i, i);							
							studentTable.setColumnSelectionInterval(0, 0);
							lastKeyboardCol = 0;
						}
					}
				}
			}
		});
	}
	
	public void selectStudent(int row) {
		studentTable.setRowSelectionInterval(row, row);
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
				addCommentAreas(currentStudent);
			}			
		});
		resizeColumns();
		
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
	
	public void addStudentGrades(SaveSheetGrades saveGrades, Rubric rubric) {

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {

				for (int i = 0; i < studentModel.getRowCount(); i++) {
					if (studentTable.getCellEditor() != null) {
						studentTable.getCellEditor().stopCellEditing();
					}
					StudentData studentInfo = (StudentData)studentModel.getValueAt(i, StudentListInfo.LAST_NAME_COLUMN);
					saveGrades.addStudentColumn(studentInfo, StudentListInfo.defaultColumnNames[StudentListInfo.LAST_NAME_COLUMN], studentInfo.getName());
					saveGrades.addStudentColumn(studentInfo, StudentListInfo.defaultColumnNames[StudentListInfo.FIRST_NAME_COLUMN], studentInfo.getFirstName());
					Date date = (Date)studentModel.getValueAt(i, StudentListInfo.DATE_COLUMN);
					String dateString = SimpleUtils.formatDate(date);
					saveGrades.addStudentColumn(studentInfo, StudentListInfo.defaultColumnNames[StudentListInfo.DATE_COLUMN], dateString);
					for (int entryNum = 0; entryNum < rubric.getEntryCount(); entryNum++) {
						RubricEntry entry = rubric.getEntry(entryNum);
						Double grade = entry.getStudentDoubleValue(studentInfo.getId());
						if (grade != null) {
							saveGrades.addStudentColumn(studentInfo, entry.getName(), grade);
						}
					}
					if (notesAndCommentsMap.containsKey(studentInfo.getId())) {
						saveGrades.addStudentNotes(studentInfo, notesAndCommentsMap.get(studentInfo.getId()));
					}
				}
			}
		});

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
