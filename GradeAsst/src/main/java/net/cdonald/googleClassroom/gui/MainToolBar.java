package net.cdonald.googleClassroom.gui;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import net.cdonald.googleClassroom.googleClassroomInterface.AssignmentFetcher;
import net.cdonald.googleClassroom.googleClassroomInterface.SheetFetcher;
import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.AssignmentSelected;
import net.cdonald.googleClassroom.listenerCoordinator.ClassSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.EnableRunRubricQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentAssignmentQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentRubricQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetRubricNamesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryListener;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricFileSelectedListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricFileValidListener;
import net.cdonald.googleClassroom.listenerCoordinator.RubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.RunRubricSelected;
import net.cdonald.googleClassroom.listenerCoordinator.RunSelected;
import net.cdonald.googleClassroom.listenerCoordinator.SetRunEnableStateListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetRunRubricEnableStateListener;
import net.cdonald.googleClassroom.listenerCoordinator.SheetFetcherListener;
import net.cdonald.googleClassroom.listenerCoordinator.StopRunListener;
import net.cdonald.googleClassroom.listenerCoordinator.StudentSelectedListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.GoogleSheetData;
import net.cdonald.googleClassroom.utils.SimpleUtils;



public class MainToolBar extends JToolBar {
	private static final long serialVersionUID = 5112657453014257288L;

	private JComboBox<ClassroomData> assignmentCombo;
	private JComboBox<GoogleSheetData> rubricCombo; 
	private DefaultComboBoxModel<ClassroomData> assignmentModel;
	private DefaultComboBoxModel<GoogleSheetData> rubricModel;
	private GoogleSheetData emptySheet;
	private ClassroomData empty;
	private JButton runButton;
	private JButton runRubricButton;
	private JButton stopButton;
	private JLabel dueDate;
	private List<String> rubricNames;
	private enum RunType {ALL, SELECTED}	
	private final String[] RUN_TITLES = {"Run All", "Run Selected"};
	private final String[] RUBRIC_TITLES = {"Run All Rubric", "Run Selected Rubrics"};


	public MainToolBar() {
		setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		rubricNames = new ArrayList<String>();
		dueDate = new JLabel("Due: ");
		Border border = BorderFactory.createLineBorder(Color.BLACK, 1);
		dueDate.setBorder(border);
		assignmentCombo = new JComboBox<ClassroomData>();
		assignmentModel = new DefaultComboBoxModel<ClassroomData>();		
		assignmentCombo.setModel(assignmentModel);
		ClassroomData sizeRestriction = new ClassroomData("This is a reasonable length but sometimes longer", "");
		assignmentCombo.setPrototypeDisplayValue(sizeRestriction);
		rubricCombo = new JComboBox<GoogleSheetData>();
		rubricModel = new DefaultComboBoxModel<GoogleSheetData>();
		rubricCombo.setModel(rubricModel);
		
		
		runButton = new JButton(RUN_TITLES[RunType.SELECTED.ordinal()]);
		runRubricButton = new JButton(RUBRIC_TITLES[RunType.SELECTED.ordinal()]);
		stopButton = new JButton("Stop");
		setRunTitles(RunType.ALL);
		setLayout(new FlowLayout(FlowLayout.LEFT));
		empty = new ClassroomData();	
		
		
		assignmentModel.addElement(empty);
		emptySheet = new GoogleSheetData();
		rubricModel.addElement(emptySheet);
		add(new JLabel("Assignment: "));
		add(new JScrollPane(assignmentCombo));
		add(dueDate);
		add(new JLabel("Rubric: "));
		add(rubricCombo);
		add(runButton);
		add(runRubricButton);
		add(stopButton);
		registerListeners();
		addSelectionListeners();
		stopButton.setEnabled(false);
		disableRunButtons();
	}
	
	private void setRunTitles(RunType runType) {
		runButton.setText(RUN_TITLES[runType.ordinal()]);
		runRubricButton.setText(RUBRIC_TITLES[runType.ordinal()]);
		
	}
	

	
	public void enableRunButton(boolean rubricLoaded) {
		runButton.setEnabled(true);
		runRubricButton.setEnabled(rubricLoaded);
	}
	
	public void disableRunButtons() {
		runButton.setEnabled(false);
		runRubricButton.setEnabled(false);		
	}
	
	public void setStopEnabled(boolean enabled) {
		stopButton.setEnabled(enabled);
	}

	private void registerListeners() {
		
		ListenerCoordinator.addQueryResponder(GetCurrentAssignmentQuery.class, new GetCurrentAssignmentQuery() {
			@Override
			public ClassroomData fired() {
				ClassroomData data = (ClassroomData) assignmentCombo.getSelectedItem();
				if (data != null && data.isEmpty() == false) {
					return data;
				}
				return null;
			}			
		});
		ListenerCoordinator.addQueryResponder(GetCurrentRubricQuery.class, new GetCurrentRubricQuery() {
			@Override
			public GoogleSheetData fired() {
				Object item = rubricCombo.getSelectedItem();
				if (item != null) {
					GoogleSheetData rubric = (GoogleSheetData)item;
					if (rubric.isEmpty() == false) {
						return rubric;
					}
				}
				return null;
			}			
		});
		
		ListenerCoordinator.addQueryResponder(GetRubricNamesQuery.class, new GetRubricNamesQuery() {
			@Override
			public List<String> fired() {
				return rubricNames;
			}			
		});
		
		// When we change the overall class, we have to change the possible assignments
		ListenerCoordinator.addListener(ClassSelectedListener.class, new ClassSelectedListener() {

			@Override
			public void fired(ClassroomData course) {
				assignmentModel.removeAllElements();
				assignmentModel.addElement(empty);
				ListenerCoordinator.runLongQuery(AssignmentFetcher.class, new LongQueryListener<ClassroomData>() {
					@Override
					public void process(List<ClassroomData> list) {
						for (ClassroomData assignment : list) {
							addAssignment(assignment);
						}						
					}

					@Override
					public void remove(Set<String> removeList) {
						removeAssignments(removeList);
						
					}
				});
			}			
		});
		
		ListenerCoordinator.addListener(RubricFileSelectedListener.class, new RubricFileSelectedListener() {
			@Override
			public void fired(String url, String fileName) {
				final String progressName = "Reading " + fileName;
				rubricNames.clear();
				for (int i = 1; i < rubricCombo.getComponentCount(); i++) {
					rubricCombo.remove(i);
				}
				ListenerCoordinator.fire(AddProgressBarListener.class, progressName);
				ListenerCoordinator.runLongQuery(SheetFetcher.class, new SheetFetcherListener(url) {
					@Override
					public void process(List<ClassroomData> list) {
						for (ClassroomData data : list) {
							if (data.isEmpty() == false) {
								addRubricInfo((GoogleSheetData) data, false);
							}
						}
					}
					@Override
					public void done() {
						ListenerCoordinator.fire(RemoveProgressBarListener.class, progressName);
						if (rubricNames.size() != 0) {
							ListenerCoordinator.fire(RubricFileValidListener.class);
						}
					}
					@Override
					public void remove(Set<String> removeList) {
					}
				});

			}			
		});
		
		
		ListenerCoordinator.addListener(SetRunEnableStateListener.class, new SetRunEnableStateListener() {
			public void fired(Boolean setRunEnabled) {
				runButton.setEnabled(setRunEnabled);
				if ((Boolean)ListenerCoordinator.runQuery(EnableRunRubricQuery.class) == true) {
					runRubricButton.setEnabled(true);
				}
			}
		});
		
		ListenerCoordinator.addListener(SetRunRubricEnableStateListener.class, new SetRunRubricEnableStateListener() {
			public void fired(Boolean enable) {				 
				if (runButton.isEnabled() == false) {
					enable = false;
				}
				runRubricButton.setEnabled(enable);
				
			}
		});
		
		ListenerCoordinator.addListener(StudentSelectedListener.class, new StudentSelectedListener() {
			@Override
			public void fired(String idToDisplay) {
				setRunTitles((idToDisplay == null) ? RunType.ALL : RunType.SELECTED);
			}			
		});
	}

	private void addSelectionListeners() {

		assignmentCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				ClassroomData data = (ClassroomData) assignmentCombo.getSelectedItem();
				if (data != null && data.isEmpty() == false) {
					ListenerCoordinator.fire(AssignmentSelected.class, data);
					dueDate.setText("Due: " + SimpleUtils.formatDate(data.getDate()));
				}
				else {
					dueDate.setText("Due: ");
				}
			}
		});
		
		rubricCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				Object item = rubricCombo.getSelectedItem();
				GoogleSheetData rubric = (GoogleSheetData)item;
				ListenerCoordinator.fire(RubricSelected.class, rubric);
			}
			
		});
		
		runButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(RunSelected.class, runButton.getText().equals(RUN_TITLES[RunType.ALL.ordinal()]));
			}			
		});	
		
		runRubricButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(RunRubricSelected.class, runRubricButton.getText().equals(RUBRIC_TITLES[RunType.ALL.ordinal()]));
			}			
		});
		
		stopButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				ListenerCoordinator.fire(StopRunListener.class);
			}
		});

	}


	public void clearAssignment() {
		assignmentModel.removeAllElements();
		assignmentModel.addElement(empty);
	}


	public ClassroomData getAssignmentSelected() {
		return assignmentModel.getElementAt(assignmentCombo.getSelectedIndex());
	}


	public void addAssignment(ClassroomData data) {
		if (data.isEmpty() == false) {
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					boolean inserted = false;
					Date assignmentDate = data.getDate();
					if (assignmentDate != null) {
						for (int i = 1; i < assignmentModel.getSize(); i++) {
							ClassroomData assignment = assignmentModel.getElementAt(i);
							Date compareDate = assignment.getDate();
							if (compareDate != null) {
								if (assignmentDate.compareTo(compareDate) > 0) {
									assignmentModel.insertElementAt(data, i);
									inserted = true;
									break;
								}
							}
						
						}
					}
					if (inserted == false) {
						assignmentModel.addElement(data);
					}
				}
			});
		}
	}
	
	public void removeAssignments(Set<String> removeIDs) {
		if (removeIDs.size() != 0) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					for (String id : removeIDs) {
						for (int i = 1; i < assignmentModel.getSize(); i++) {
							ClassroomData assignment = assignmentModel.getElementAt(i);
							if (assignment.getId().equals(id)) {
								assignmentModel.removeElementAt(i);
								break;								
							}						
						}
					}
				}
			});
			
		}
	}
	
	public void addRubricInfo(GoogleSheetData data, boolean select) {
		
		for (String name : rubricNames) {
			if (name.contentEquals(data.getName())) {
				return;
			}
		}
		String newName = data.getName();
		boolean inserted = false;
		for (int i = 0; i < rubricNames.size(); i++) {
			if (rubricNames.get(i).compareTo(newName) > 0) {
				rubricCombo.insertItemAt(data, i + 1);
				rubricNames.add(i, newName);
				inserted = true;
				break;
			}
		}
		if (inserted == false) {
			rubricCombo.addItem(data);
			rubricNames.add(newName);
		}

		if (select) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					rubricCombo.setSelectedItem(data);
				}
			});			
		}
	}
}
