package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.GetCompilerQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LoadSourceQuery;
import net.cdonald.googleClassroom.listenerCoordinator.RubricTestCodeChanged;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntryRunCode;
import net.cdonald.googleClassroom.utils.SimpleUtils;

/**
 * 
 * Broke this out purely to shrink the code in RubricElementDialog.  It creates the RunCode card
 *
 */
public class RubricEntryRunCodeCard extends JPanel implements RunCodeFileListTableModelListener,  RubricEntryAutomationCardInterface {
	private static final long serialVersionUID = -1929262696727464061L;
	private JButton addFilesButton;
	private JTable fileToUseList;
	private JScrollPane fileScroll;
	private RunCodeFileListTableModel fileToUseModel;
	private JComboBox<String> methodToCallCombo;
	private JLabel methodToCallLabel;
	private Map<String, Method> methodMap;	
	private RubricEntryRunCode associatedAutomation;
	private JLabel explanation;
	private Rubric rubricToModify;
	public RubricEntryRunCodeCard(Rubric rubric, int elementIndex){		
		methodMap = new HashMap<String, Method>();
		
		addFilesButton = new JButton("Add Files");

		explanation = new JLabel("<html>Load new or select an already loaded test file that contains the code to run."
				+ " Then select the method that will be run. "
				+ "The method should have the signature: <br/> <i>public static double methodName()</i> <br/>"
				+ "The return value should be between 0 and 1 inclusive (calculated by dividing numTestsPassing/numTestRun)."
				+  "<br/>Test by selecting the \"Test Run\" button."
				+ "<br/>View results on the main screen (including rubric run output).</html>");


		
		JPanel buttonsPanel = SimpleUtils.createButtonPanel(2);
		buttonsPanel.add(addFilesButton);		
		JPanel buttonHolder = new JPanel();
		buttonHolder.setLayout(new BorderLayout());
		buttonHolder.add(buttonsPanel, BorderLayout.NORTH);
		JPanel namePanel = createNamePanel();
		JPanel nameAndButtons = new JPanel();
		nameAndButtons.setLayout(new BorderLayout());
		nameAndButtons.add(fileScroll, BorderLayout.CENTER);
		nameAndButtons.add(explanation, BorderLayout.NORTH);
		nameAndButtons.add(namePanel, BorderLayout.CENTER);
		nameAndButtons.add(buttonHolder, BorderLayout.EAST);

		
		
		setLayout(new BorderLayout());
		add(nameAndButtons, BorderLayout.CENTER);
		
		
	
		addFilesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addFile();
			}
		});
		

		methodToCallCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				methodSelected();
			}
		});
		addItems(rubric, elementIndex);
	}
	
	private JPanel createNamePanel() {
		JPanel namePanel = new JPanel();
		namePanel.setLayout(new GridBagLayout());
		final int SPACE = 5;
		
		JLabel fileToUseLabel = new JLabel("File To Use: ");
		fileToUseList = new JTable();
		fileToUseModel = new RunCodeFileListTableModel(this);
		fileToUseList.setModel(fileToUseModel);
		fileToUseList.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		fileToUseList.getColumnModel().getColumn(0).setMaxWidth(10);
		fileToUseList.setTableHeader(null);
		namePanel.setBorder(BorderFactory.createEmptyBorder(SPACE, 0, SPACE, 0));
		methodToCallCombo = new JComboBox<String>();
		methodToCallCombo.setEditable(false);

	 

		methodToCallLabel = new JLabel("Method to call: ");
		methodToCallLabel.setToolTipText("Method's signature should be public static double methodName()."
				+ " Enter only the name of the method, without parameters or return type.");
		fileScroll = new JScrollPane();
		fileScroll.setViewportView(fileToUseList);
		//fileScroll.setPreferredSize(new Dimension(0, fileToUseList.getRowHeight() * 5 + 1));
		JPanel filePanel = new JPanel();
		filePanel.setLayout(new BorderLayout());
		filePanel.add(fileScroll, BorderLayout.CENTER);
		filePanel.setPreferredSize(new Dimension(0, fileToUseList.getRowHeight() * 5 + 1));
		SimpleUtils.addLabelAndComponent(namePanel, new JLabel(), explanation, 0);
		//SimpleUtils.addLabelAndComponent(namePanel, new JLabel(), new JLabel("                 "), 1);
		SimpleUtils.addLabelAndComponent(namePanel, fileToUseLabel, filePanel, 1);
		SimpleUtils.addLabelAndComponent(namePanel, methodToCallLabel, methodToCallCombo, 2);
		return namePanel;

	}
	
	private void registerListeners() {
		ListenerCoordinator.addListener(RubricTestCodeChanged.class, new RubricTestCodeChanged() {
			@Override
			public void fired() {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						addSources();
					}
				});
				
			}
		});
	}
	

	public void referenceSourceEnabled(boolean enable) {

		
	}
	
	private void addSources() {
		fileToUseModel.fireTableDataChanged();
	}


	@Override
	public void addItems(Rubric rubricToModify, int index) {
		this.rubricToModify = rubricToModify;

		RubricEntry associatedEntry = rubricToModify.getEntry(index);
		if (associatedEntry.getAutomation() == null || !(associatedEntry.getAutomation() instanceof RubricEntryRunCode)) {
			associatedEntry.setAutomation(new RubricEntryRunCode());
		}
		associatedAutomation = (RubricEntryRunCode)associatedEntry.getAutomation();
		
		fillRunCode();		
		fillMethodCombo();
		fileToUseModel.fireTableDataChanged();
	}
	
	@Override
	public String getDescription() {
		return "Automated.  After you run rubrics, if no value is filled in for a student, check this tab for more information";		
	}

	
	@Override
	public void saving() {
		
	}
	
	private void fillRunCode() {				
		methodToCallCombo.setSelectedItem(associatedAutomation.getMethodToCall());
	}
	

	
	private void addFile() {
		if (rubricToModify != null) {


			List<FileData> allFiles = (List<FileData>)ListenerCoordinator.runQuery(LoadSourceQuery.class);
			if (allFiles != null) {
				rubricToModify.addTestCode(allFiles);
			}
		}
	}
	
	private void methodSelected() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				String methodName = (String)methodToCallCombo.getSelectedItem();
				if (methodName != null) {
					Method method = methodMap.get(methodName);
					associatedAutomation.setMethodToCall(methodName);
				}
			}
		});
	}

	

	
	public void fillMethodCombo() {
		StudentWorkCompiler compiler = (StudentWorkCompiler)ListenerCoordinator.runQuery(GetCompilerQuery.class);
		List<Method> methods = associatedAutomation.getPossibleMethods(rubricToModify.getReferenceSource(), compiler, rubricToModify.getTestCode());
		if (methods == null) {
			methodToCallCombo.removeAllItems();
			methodMap.clear();
		}
		else {				
			boolean changed = false;
			for (Method method : methods) {
				if (methodMap.containsKey(method.getName()) == false) {
					changed = true;
					break;
				}
			}
			if (changed) {
				methodToCallCombo.removeAllItems();
				methodMap.clear();				
				for (Method method : methods) {
					methodToCallCombo.addItem(method.getName());
					methodMap.put(method.getName(), method);
					
				}
			}				
		}			
	}

	@Override
	public void addRunCodeFile(FileData fileData) {
		if (associatedAutomation != null) {
			associatedAutomation.addTestCodeSourceName(fileData.getName());
			fillMethodCombo();
		}
		
	}

	@Override
	public void removeRunCodeFile(FileData fileData) {
		if (associatedAutomation != null) {
			associatedAutomation.removeTestCodeSource(fileData.getName());
			fillMethodCombo();
		}
		
	}

	@Override
	public boolean containsSource(FileData fileData) {
		if (associatedAutomation != null) {
			return associatedAutomation.containsSource(fileData.getName());
		}
		return false;
	}
	
	@Override
	public List<FileData> getFiles() {
		if (rubricToModify != null) {
			return rubricToModify.getTestCode();
		}
		return null;
	}



}