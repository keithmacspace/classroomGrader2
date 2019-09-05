package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.text.DefaultCaret;

import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntryRunCode;
import net.cdonald.googleClassroom.utils.HighlightText;
import net.cdonald.googleClassroom.utils.SimpleUtils;

/**
 * 
 * Broke this out purely to shrink the code in RubricElementDialog.  It creates the RunCode card
 *
 */
public class RubricEntryRunCodeCard implements RunCodeFileListTableModelListener,  RubricEntryDialogCardInterface {
	
	private JButton addFilesButton;
	private JButton removeFilesButton;
	private JTable fileToUseList;
	private JScrollPane fileScroll;
	private RunCodeFileListTableModel fileToUseModel;
	private JComboBox<String> methodToCallCombo;
	private JLabel methodToCallLabel;
	private Map<String, JTextArea> sourceCodeArea;
	private Map<String, Method> methodMap;
	private JPanel runPanel;
	private JTabbedPane sourceTabs;
	private JPanel sourceCodePanel;
	private RubricEntryRunCode associatedAutomation;
	private JLabel explanation;
	private boolean isActive;
	private RubricElementDialog dialogOwner;
	public RubricEntryRunCodeCard(RubricElementDialog dialogOwner){
		this.dialogOwner = dialogOwner;
		sourceCodeArea = new HashMap<String, JTextArea>();
		methodMap = new HashMap<String, Method>();
		isActive = false;
		addFilesButton = dialogOwner.newButton("Add Files", true);
		removeFilesButton = dialogOwner.newButton("Remove Files", true);
		explanation = new JLabel("<html>Load new or select an already loaded test file that contains the code to run."
				+ " Then select the method that will be run. "
				+ "The method should have the signature: <br/> <i>public static double methodName()</i> <br/>"
				+ "The return value should be between 0 and 1 inclusive (calculated by dividing numTestsPassing/numTestRun)."
				+  "<br/>Test by selecting the \"Test Run\" button."
				+ "<br/>View results on the main screen (including rubric run output).</html>");


		
		JPanel buttonsPanel = dialogOwner.createButtonPanel(2);
		buttonsPanel.add(addFilesButton);
		buttonsPanel.add(removeFilesButton);
		JPanel buttonHolder = new JPanel();
		buttonHolder.setLayout(new BorderLayout());
		buttonHolder.add(buttonsPanel, BorderLayout.NORTH);
		JPanel namePanel = createNamePanel();
		JPanel nameAndButtons = new JPanel();
		nameAndButtons.setLayout(new BorderLayout());
		//nameAndButtons.add(fileScroll, BorderLayout.CENTER);
		//nameAndButtons.add(explanation, BorderLayout.NORTH);
		nameAndButtons.add(namePanel, BorderLayout.CENTER);
		nameAndButtons.add(buttonHolder, BorderLayout.EAST);

		sourceCodePanel = new JPanel();
		sourceCodePanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		sourceCodePanel.setLayout(new BorderLayout());

		JPanel sourcePanel = new JPanel();
		sourcePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		sourcePanel.setLayout(new BorderLayout());
		JTextArea sourceArea = new JTextArea();
		for (int i = 0; i < 20; i++) {
			sourceArea.append("\n");
		}
		sourcePanel.add(new JScrollPane(sourceArea));
		
		
		sourceTabs = new JTabbedPane();
		sourceTabs.addTab("",  sourcePanel);
		JPanel sourcePanelX = new JPanel();
		sourcePanelX.setLayout(new BorderLayout());
		sourcePanelX.add(sourceTabs, BorderLayout.CENTER);
		sourcePanelX.setPreferredSize(new Dimension(0, 200));

		sourceCodePanel.add(sourcePanelX, BorderLayout.CENTER);

		
		
		
		runPanel = new JPanel();
		runPanel.setLayout(new BorderLayout());
		runPanel.add(nameAndButtons, BorderLayout.NORTH);
		runPanel.add(sourceCodePanel, BorderLayout.CENTER);
		
	
		addFilesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addFile();
			}
		});
		
		removeFilesButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				removeFile();
			}
		});
		
		methodToCallCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				methodSelected();
			}
		});
		dialogOwner.addAutomationCard(runPanel, RubricEntry.AutomationTypes.RUN_CODE);
	}
	

	public void goldenSourceEnabled(boolean enable) {

		
	}


	@Override
	public void addItems() {
		isActive = true;
		RubricEntry associatedEntry = dialogOwner.getCurrentEntry();
		if (associatedEntry.getAutomation() == null || !(associatedEntry.getAutomation() instanceof RubricEntryRunCode)) {
			associatedEntry.setAutomation(new RubricEntryRunCode());
		}
		associatedAutomation = (RubricEntryRunCode)associatedEntry.getAutomation();
		
		fillRunCode();
		fileToUseModel.init();

		Map<String, FileData> allFiles = dialogOwner.getRubricToModify().getFileDataMap();
		for (String key : allFiles.keySet()) {
			fileToUseModel.addFile(allFiles.get(key));
		}			
		fillMethodCombo();

	}

	@Override
	public boolean isActive() {
		return isActive;
	}
	
	@Override
	public void removeItems() {
		isActive = false;		
	}
	
	private void fillRunCode() {				
		while(sourceTabs.getTabCount() != 0) {
			sourceTabs.removeTabAt(0);				
		}
		List<FileData> sourceFiles = associatedAutomation.getSourceFiles();
		for (FileData sourceFile : sourceFiles) {
			addCodeTabs(sourceFile);
		}
		methodToCallCombo.setSelectedItem(associatedAutomation.getMethodToCall());
	}
	
	private JPanel createNamePanel() {
		JPanel namePanel = new JPanel();
		namePanel.setLayout(new GridBagLayout());
		final int SPACE = 5;
		
		JLabel fileToUseLabel = new JLabel("File To Use: ");
		fileToUseList = new JTable();
		fileToUseModel = new RunCodeFileListTableModel(dialogOwner.getRubricToModify(), this);
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
		fileScroll.setPreferredSize(new Dimension(0, fileToUseList.getRowHeight() * 5 + 1));
		JPanel filePanel = new JPanel();
		filePanel.setLayout(new BorderLayout());
		filePanel.add(fileScroll, BorderLayout.CENTER);
		filePanel.setPreferredSize(new Dimension(0, fileToUseList.getRowHeight() * 5 + 1));
		SimpleUtils.addLabelAndComponent(namePanel, new JLabel(), explanation, 0);
		SimpleUtils.addLabelAndComponent(namePanel, new JLabel(), new JLabel("                 "), 1);
		SimpleUtils.addLabelAndComponent(namePanel, fileToUseLabel, filePanel, 2);
		SimpleUtils.addLabelAndComponent(namePanel, methodToCallLabel, methodToCallCombo, 3);
		return namePanel;

	}
	
	private void addFile() {
		List<FileData> allFiles = dialogOwner.loadSource();
		if (allFiles != null) {
			for (FileData fileData : allFiles) {
				if (dialogOwner.getRubricToModify().getFileDataMap().containsKey(fileData.getName()) == false) {
					dialogOwner.getRubricToModify().addFileData(fileData);
					addRunCodeFile(fileData);
					fileToUseModel.addFile(fileData);
					fileToUseModel.fireTableDataChanged();
				}
			}
		}		
	}
	
	private void removeFile() {
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				List<FileData> removeList = new ArrayList<FileData>();
				for (int row : fileToUseList.getSelectedRows()) {								
					FileData fileToRemove = (FileData)fileToUseModel.getValueAt(row, 1);
					removeList.add(fileToRemove);
					dialogOwner.getRubricToModify().removeFileData(fileToRemove);
					removeRunCodeFile(fileToRemove);
				}
				for (FileData fileToRemove : removeList) {								
					fileToUseModel.removeFile(fileToRemove.getName());
				}
				fileToUseModel.fireTableDataChanged();
				
			}
		});
	}
	
	private void methodSelected() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				String methodName = (String)methodToCallCombo.getSelectedItem();
				if (methodName != null) {
					Method method = methodMap.get(methodName);
					associatedAutomation.setMethodToCall(methodName);						
					for (FileData fileData : associatedAutomation.getSourceFiles()) {
						int index = fileData.getFileContents().indexOf(methodName); 
						if (index != -1) {
							for (int i = 0; i < sourceTabs.getTabCount(); i++) {
								if (sourceTabs.getTitleAt(i).contentEquals(fileData.getName())) {
									sourceTabs.setSelectedIndex(i);
									JTextArea textArea = sourceCodeArea.get(fileData.getName());										
									HighlightText.highlightMethod(textArea, method);
									break;
								}
							}
						}
					}
				}
			}
		});
	}

	
	private boolean firstTabIsEmpty() {
		return (sourceTabs.getTabCount() == 1 && sourceTabs.getTitleAt(0).length() == 0);
	}
	
	private void addCodeTabs(FileData fileData) {
		JPanel sourcePanel = new JPanel();
		sourcePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		sourcePanel.setLayout(new BorderLayout());
		JTextArea sourceArea = new JTextArea();
		DefaultCaret caret = (DefaultCaret) sourceArea.getCaret();
		caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
		JScrollPane scrollPane = new JScrollPane(sourceArea);
		sourcePanel.add(scrollPane);
		sourceArea.setText(fileData.getFileContents());
		sourceArea.getDocument().addDocumentListener(new SourceDocumentListener(fileData, sourceArea));
		sourceCodeArea.put(fileData.getName(), sourceArea);						
		if (firstTabIsEmpty()) {
			sourceTabs.setTitleAt(0, fileData.getName());
			sourceTabs.setComponentAt(0, sourcePanel);
		}
		else {
			sourceTabs.addTab(fileData.getName(), sourcePanel);
		}

	}
	

	@Override
	public void addRunCodeFile(FileData fileData) {
		addCodeTabs(fileData);
		associatedAutomation.addSourceContents(fileData);
		fillMethodCombo();
	}
	@Override
	public void removeRunCodeFile(FileData fileData) {
		for (int i = 0; i < sourceTabs.getTabCount(); i++) {
			String fileName = sourceTabs.getTitleAt(i);
			if (fileName.equals(fileData.getName())) {
				sourceTabs.removeTabAt(i);
				break;
			}
		}
		sourceCodeArea.remove(fileData.getName());			
		associatedAutomation.removeFileData(fileData);
		fillMethodCombo();
	}
	@Override
	public boolean containsSource(FileData fileData) {
		return associatedAutomation.containsSource(fileData);
	}
	
	public void fillMethodCombo() {
		List<Method> methods = associatedAutomation.getPossibleMethods(dialogOwner.getRubricToModify().getGoldenSource(), dialogOwner.getCompiler());
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



}