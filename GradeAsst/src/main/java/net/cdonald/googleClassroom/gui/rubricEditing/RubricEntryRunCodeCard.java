package net.cdonald.googleClassroom.gui.rubricEditing;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mdkt.compiler.CompilationException;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntry.AutomationTypes;
import net.cdonald.googleClassroom.model.RubricEntryRunCode;
import net.cdonald.googleClassroom.utils.SimpleUtils;

/**
 * 
 * Broke this out purely to shrink the code in RubricElementDialog.  It creates the RunCode card
 *
 */
public class RubricEntryRunCodeCard extends RubricEntryAutomationCardInterface implements RunCodeFileListTableModelListener {
	private static final long serialVersionUID = -1929262696727464061L;	
	private JTable fileToUseList;	
	private RunCodeFileListTableModel fileToUseModel;
	private JComboBox<String> methodToCallCombo;
	private JLabel methodToCallLabel;	
	private RubricEntryRunCode associatedAutomation;
	private JLabel explanation;	
	private Map<String, Method> methodMap;
	private RubricFileListener rubricFileListener;
	private JCheckBox updateOnlyOnPassBox;
	private Map<String, List<Method>> possibleMethodMap;
	public RubricEntryRunCodeCard(boolean enableEditing, Map<String, List<Method>> possibleMethodMap, RubricFileListener rubricFileListener, Rubric rubric, int elementID){
		this.possibleMethodMap = possibleMethodMap;
		this.rubricFileListener = rubricFileListener;		
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder("Run Code Automation Options"));
		methodMap = new HashMap<String, Method>();
		


		explanation = new JLabel("<html>Load new or select an already loaded test file that contains the code to run."
				+ " Then select the method that will be run. "
				+ "The method should have the signature: <br/> <i>public static double methodName()</i> OR <i>public static Double methodName()</i><br/>"
				+ "The return value should be between 0 and 1 inclusive (calculated by dividing numTestsPassing/numTestRun).<br/></html>");


		

			
		JPanel buttonHolder = new JPanel();
		buttonHolder.setLayout(new BorderLayout());
		buttonHolder.add(rubricFileListener.getAddSourceButtons(), BorderLayout.NORTH);
		JPanel namePanel = createNamePanel();
		JPanel nameAndButtons = new JPanel();
		nameAndButtons.setLayout(new BorderLayout());		
		nameAndButtons.add(explanation, BorderLayout.NORTH);
		nameAndButtons.add(namePanel, BorderLayout.CENTER);
		nameAndButtons.add(buttonHolder, BorderLayout.EAST);
		add(nameAndButtons, BorderLayout.CENTER);
		methodToCallCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				methodSelected();
			}
		});
		addItems(rubric, elementID, possibleMethodMap);
		setEnableEditing(enableEditing);
	}
	
	private JPanel createNamePanel() {
		JPanel namePanel = new JPanel();
		namePanel.setLayout(new GridBagLayout());
		final int SPACE = 5;
		
		updateOnlyOnPassBox = new JCheckBox("Update grades only on 100% pass.");
		addUpdateOnFail(namePanel, updateOnlyOnPassBox);
		
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
		JScrollPane fileScroll = new JScrollPane(fileToUseList);
		JPanel filePanel = new JPanel();
		filePanel.setLayout(new BorderLayout());
		//filePanel.add(fileScroll, BorderLayout.CENTER);
		SimpleUtils.addLabelAndComponent(namePanel, new JLabel(), explanation, 0);
		SimpleUtils.addLabel(namePanel, fileToUseLabel, 2);
		//SimpleUtils.addLabelAndComponent(namePanel, new JLabel(), new JLabel("                 "), 1);
		addFileTable(namePanel, fileScroll);
		SimpleUtils.addLabel(namePanel, methodToCallLabel, 3);
		addMethodCombo(namePanel, methodToCallCombo);
		return namePanel;
	}
	
	private void addUpdateOnFail(JPanel namePanel, JCheckBox updateOnFailBox) {
		GridBagConstraints c = new GridBagConstraints();
		//c.insets = new Insets(3, 3, 3, 0);
		c.weightx = 1.0;
		c.weighty = 0.1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.LINE_START;
		//c.gridwidth = GridBagConstraints.REMAINDER;
		//c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 0;
		namePanel.add(updateOnFailBox, c);
		
	}
	private void addFileTable(JPanel namePanel, JScrollPane fileToUseTable) {
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 3, 3, 0);
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.LINE_START;
		//c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 2;
		namePanel.add(fileToUseTable, c);
	}
	private void addMethodCombo(JPanel namePanel, JComboBox<String> methoToCallCombo) {
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 3, 3, 0);
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		//c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 3;
		namePanel.add(methoToCallCombo, c);
	}
	

	public void referenceSourceEnabled(boolean enable) {

		
	}


	public void addItems(Rubric rubricToModify, int elementID, Map<String, List<Method>> possibleMethodMap) {
		RubricEntry associatedEntry = rubricToModify.getEntryByID(elementID);
		if (associatedEntry.getAutomation() == null || !(associatedEntry.getAutomation() instanceof RubricEntryRunCode)) {
			associatedEntry.setAutomation(new RubricEntryRunCode());
		}
		associatedAutomation = (RubricEntryRunCode)associatedEntry.getAutomation();
		if (associatedAutomation != null) {
			updateOnlyOnPassBox.setSelected(associatedAutomation.isUpdateOnlyOnPass());
			fillRunCode();		
			fillMethodCombo(possibleMethodMap);
			fileToUseModel.fireTableDataChanged();
		}
	}
	
	
	@Override
	public void saving() {
		if (associatedAutomation != null) {
			String methodName = (String)methodToCallCombo.getSelectedItem();
			if (methodName != null) {
				for (int i = 0; i < fileToUseList.getRowCount(); i++) {
					Boolean isChecked = (Boolean)fileToUseModel.getValueAt(i, 0);
					if (isChecked == Boolean.TRUE) {
						FileData fileData = (FileData) fileToUseList.getValueAt(i, 1);
						associatedAutomation.addTestCodeSourceName(fileData.getName());
					}
				}
				associatedAutomation.setMethodToCall(methodName);
				associatedAutomation.setUpdateOnlyOnPass(updateOnlyOnPassBox.isSelected());
			}			
		}
		
	}
	
	@Override
	public void testSourceChanged(Map<String, List<Method>> possibleMethodMap) {
		this.possibleMethodMap = possibleMethodMap;
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				fileToUseModel.fireTableDataChanged();
				fillMethodCombo(possibleMethodMap);				
			}
		});

		
	}
	
	private void fillRunCode() {				
		methodToCallCombo.setSelectedItem(associatedAutomation.getMethodToCall());
	}
	
	private void methodSelected() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				String methodName = (String)methodToCallCombo.getSelectedItem();
				if (methodName != null) {					
					associatedAutomation.setMethodToCall(methodName);
				}
			}
		});
	}

	
	public void fillMethodCombo(Map<String, List<Method>> possibleMethodMap) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				methodToCallCombo.removeAllItems();
				methodMap.clear();
				if (possibleMethodMap != null) {
					synchronized(possibleMethodMap) {
						for (String file : associatedAutomation.getTestCodeSourceToUse()) {
							List<Method> methods = null;
							if (possibleMethodMap != null) {
								methods = possibleMethodMap.get(file);
							}
							if (methods != null) {
								for (Method method : methods) {
									methodToCallCombo.addItem(method.getName());
									methodMap.put(method.getName(), method);
								}
							}
						}
					}
				}
				revalidate();
			}
		});
	}

	@Override
	public void addRunCodeFile(FileData fileData) {
		if (associatedAutomation != null) {
			associatedAutomation.addTestCodeSourceName(fileData.getName());
			fillMethodCombo(possibleMethodMap);
		}
		
	}

	@Override
	public void removeRunCodeFile(FileData fileData) {
		if (associatedAutomation != null) {
			associatedAutomation.removeTestCodeSource(fileData.getName());
			fillMethodCombo(possibleMethodMap);
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
		return rubricFileListener.getTestSource();
	}
	
	@Override
	public void setEnableEditing(boolean enable) {
		methodToCallCombo.setEnabled(enable);
		fileToUseModel.setEditable(enable);
		updateOnlyOnPassBox.setEnabled(enable);
		
	}
	
	@Override
	public AutomationTypes getAutomationType() {
		// TODO Auto-generated method stub
		return RubricEntry.AutomationTypes.RUN_CODE;
	}

	
	public static void main(String args[]) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new TestFrame().setVisible(true);
			}
		});
	}
	private static class TestFrame extends JFrame implements RubricFileListener{
		/**
		 * 
		 */
		private static final long serialVersionUID = -8389731518262159090L;
		List<FileData> refSource;
		List<FileData> testSource;
		public TestFrame() {
			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedLookAndFeelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			setSize(800, 500);
			refSource = new ArrayList<FileData>();
			testSource = new ArrayList<FileData>();
			refSource.add(FileData.initFromDisk("C:\\Users\\kdmacdon\\Documents\\Teals\\EclipseWork\\BigIntAddition\\src\\BigIntAddition.java"));
			testSource.add(FileData.initFromDisk("C:\\Users\\kdmacdon\\Documents\\Teals\\EclipseWork\\BigIntAddition\\src\\ConvertStringTest.java"));
			testSource.add(FileData.initFromDisk("C:\\Users\\kdmacdon\\Documents\\Teals\\EclipseWork\\BigIntAddition\\src\\AddBigIntTest.java"));
			StudentWorkCompiler compiler = new StudentWorkCompiler(null);
			Map<String, List<Method>> possibleMethodMap = null;
			try {
				possibleMethodMap = RubricEntryRunCode.getPossibleMethods(refSource, compiler, testSource);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			Rubric rubric = new Rubric();
			rubric.addNewEntry(0);
			RubricEntry e = rubric.getEntry(0);
			e.setName("Test");
			e.setValue(5);
			e.setAutomation(new RubricEntryRunCode());
			JPanel testPanel = new RubricEntryRunCodeCard(true, possibleMethodMap, this, rubric, e.getUniqueID());
			add(testPanel);
			setVisible(true);
		}
		
		

		@Override
		public List<FileData> getReferenceSource() {
			return refSource;
		}

		@Override
		public List<FileData> getTestSource() {
			// TODO Auto-generated method stub
			return testSource;
		}

		@Override
		public JButton getAddSourceButtons() {
			return new JButton("Add Source");

		}



		@Override
		public boolean isReferenceSourceSet() {
			return true;
		}



		@Override
		public void addCompilerMessage(String message) {
			// TODO Auto-generated method stub
			
		}



		@Override
		public void sourceIsChanged() {
			// TODO Auto-generated method stub
			
		}



		@Override
		public void compileSource(RubricTabNames sourceType) {
			// TODO Auto-generated method stub
			
		}
	}




}