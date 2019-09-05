package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntryMethodContains;
import net.cdonald.googleClassroom.utils.SimpleUtils;

/**
 * 
 * Broke this out purely to reduce the code in RubricElementDialog, this represents
 * the CodeContainsString Card
 *
 */
public class RubricEntryCodeContainsStringCard implements RubricEntryDialogCardInterface{
	private JPanel codeContainsPanel;
	private JComboBox<Method> methodToSearchCombo;
	private JTable valuesToSearchForTable;
	private DefaultTableModel valuesToSearchForModel;
	private JLabel explanation;
	private boolean isActive;		
	private boolean enableTableListener;
	private RubricEntryMethodContains associatedAutomation;
	private RubricElementDialog dialogOwner;
	public RubricEntryCodeContainsStringCard(RubricElementDialog dialogOwner) {
		this.dialogOwner = dialogOwner;
		isActive = false;
		explanation = new JLabel("<html>Method to search is the one that should contain the string(s) of interest.<br/>"
				+ "  The method must be part of your golden source. "
				+ "By default, all of the method names in the golden source are possible strings to find.<br/>"
				+ "This automaton will only search for method calls and field use i.e. Math.pow or Math.PI.<br/>"
				+ "It will not search for things like variable declarations, local variable use, comments etc.<br/>"
				+ "<br/>Test by selecting the \"Test Run\" button."
				+ "<br/>View results on the main screen (including rubric run output).<br/></html>");

		JPanel namePanel = createNamePanel();
		codeContainsPanel = new JPanel();
		codeContainsPanel.setLayout(new BorderLayout());


		//codeContainsPanel.add(explanation, BorderLayout.NORTH);
		codeContainsPanel.add(namePanel, BorderLayout.CENTER);

		methodToSearchCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				//methodSelected();
			}
		});
		dialogOwner.addAutomationCard(codeContainsPanel, RubricEntry.AutomationTypes.CODE_CONTAINS_METHOD);
	}

	private JPanel createNamePanel() {
		JPanel namePanel = new JPanel();
		namePanel.setLayout(new GridBagLayout());
		final int SPACE = 5;
		JLabel methodToSearchLabel = new JLabel("Method To Search: ");

		JLabel stringToSearchFor = new JLabel("String(s) to search for: ");




		valuesToSearchForModel = new DefaultTableModel(null, new String[] {"", "String(s) To Search For"}) {
			private static final long serialVersionUID = 1L;

			@Override
			public Class<?> getColumnClass(int columnIndex) {
				if (columnIndex == 0) {
					return Boolean.class;
				}
				else {
					return String.class;
				}
			}
			@Override
			public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
				super.setValueAt(aValue, rowIndex, columnIndex);

				if (associatedAutomation != null) {

					if (columnIndex == 0) {
						List<String> valuesToFind = new ArrayList<String>();
						for (int row = 0; row < valuesToSearchForModel.getRowCount(); row++) {
							Object valueAtCol = valuesToSearchForModel.getValueAt(row, 0);
							if (valueAtCol instanceof Boolean) {
								Boolean booleanVal = (Boolean)valueAtCol;

								if (booleanVal != null && booleanVal) {
									String name = (String)valuesToSearchForModel.getValueAt(row,  1);
									if (name != null && name.length() > 1) {
										valuesToFind.add(name);
									}
								}
							}
						}

						associatedAutomation.setStringsToFind(valuesToFind);
					}
					else if (columnIndex == 1) {
						setValueAt(Boolean.TRUE, rowIndex, 0);
					}
				}

			}
			@Override
			public boolean isCellEditable(int row, int column) {
				if (column == 0) {
					String col1Value = (String)getValueAt(row, column + 1);
					if (col1Value == null || col1Value.length() == 0) {
						return false;
					}
				}
				return true;
			}

		};
		valuesToSearchForTable = new JTable(valuesToSearchForModel);


		valuesToSearchForModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (enableTableListener == true && e.getType() == TableModelEvent.UPDATE) {
					if ((e.getLastRow() + 1) == valuesToSearchForModel.getRowCount()) {
						valuesToSearchForModel.addRow(new Object[] {Boolean.FALSE, ""});
					}
				}
			}
		});
		valuesToSearchForTable.getColumnModel().getColumn(0).setMaxWidth(10);
		valuesToSearchForTable.setModel(valuesToSearchForModel);
		valuesToSearchForTable.setEnabled(false);
		namePanel.setBorder(BorderFactory.createEmptyBorder(SPACE, 0, SPACE, 0));
		methodToSearchCombo = new JComboBox<Method>();
		methodToSearchCombo.setEditable(false);	
		methodToSearchCombo.setRenderer(new MethodComboRenderer());
		methodToSearchCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Method methodToSearch = (Method)methodToSearchCombo.getSelectedItem();
				valuesToSearchForTable.setEnabled(methodToSearch != null);
				if (associatedAutomation != null) {
					associatedAutomation.setMethodToSearch(methodToSearch);
				}
			}
		});


		SimpleUtils.addLabelAndComponent(namePanel, new JLabel(), explanation, 0);
		SimpleUtils.addLabelAndComponent(namePanel, new JLabel(), new JLabel("                 "), 1);
		SimpleUtils.addLabelAndComponent(namePanel, methodToSearchLabel, methodToSearchCombo, 2);
		SimpleUtils.addLabelAndComponent(namePanel, new JLabel(), new JLabel("                 "), 3);
		SimpleUtils.addLabelAndComponent(namePanel, stringToSearchFor, valuesToSearchForTable, 4);
		return namePanel;

	}


	public void goldenSourceEnabled(boolean enable) {
		fillMethodCombo();

	}


	@Override
	public void addItems() {
		isActive = true;
		enableTableListener = false;
		// Do it it this way to prevent infinite recursion in when we call set value at
		associatedAutomation = null;
		for (int row = 0; row < valuesToSearchForModel.getRowCount(); row++) {
			valuesToSearchForModel.setValueAt(Boolean.FALSE, row, 0);
		}
		methodToSearchCombo.setSelectedIndex(0);
		RubricEntryMethodContains automation = (RubricEntryMethodContains)dialogOwner.getCurrentEntry().getAutomation();
		if (automation != null) {				
			for (String strToFind : automation.getStringsToFind()) {
				boolean found = false;
				for (int row = 0; row < valuesToSearchForModel.getRowCount(); row++) {
					String name = (String)valuesToSearchForModel.getValueAt(row,  1);
					if (name != null && name.equals(strToFind)) {
						valuesToSearchForModel.setValueAt(Boolean.TRUE, row, 0);
						found = true;
						break;
					}
				}
				if (found == false) {
					valuesToSearchForModel.insertRow(valuesToSearchForModel.getRowCount() - 1, new Object[]{Boolean.TRUE, strToFind});
				}
			}
			valuesToSearchForTable.setEnabled(false);
			for (int i = 1; i < methodToSearchCombo.getItemCount(); i++) {
				Method method = methodToSearchCombo.getItemAt(i);
				if (method.toString().equals(automation.getFullMethodSignature())) {
					methodToSearchCombo.setSelectedIndex(i);
					valuesToSearchForTable.setEnabled(true);
					break;
				}
			}
		}
		associatedAutomation = automation;
		enableTableListener = true;

	}


	@Override
	public boolean isActive() {
		return isActive;
	}


	@Override
	public void removeItems() {
		isActive = false;		
	}


	private void fillMethodCombo() {
		enableTableListener = false;
		Map<String, Class<?>> classMap = null;
		List<FileData> goldenSource = dialogOwner.getRubricToModify().getGoldenSource();
		try {
			classMap = dialogOwner.getCompiler().compile(goldenSource);
		} catch (Exception e) {

		}
		methodToSearchCombo.removeAllItems();
		methodToSearchCombo.addItem(null);
		while (valuesToSearchForModel.getRowCount() > 0) {
			valuesToSearchForModel.removeRow(0);
		}
		List<String> sortedNames = new ArrayList<String>();
		List<Method> sortedMethods = new ArrayList<Method>();
		if (classMap != null) {
			for (String className : classMap.keySet()) {
				Class<?> classContainer = classMap.get(className);
				for (Method method : classContainer.getMethods()) {
					String methodName = method.getName();
					for (FileData file : goldenSource) {
						if (file.getFileContents().indexOf(methodName) != -1) {						
							boolean inserted = false;
							for (int i = 0; i < sortedNames.size(); i++) {
								if (methodName.compareTo(sortedNames.get(i)) < 0) {
									inserted = true;
									sortedNames.add(i, methodName);
									sortedMethods.add(i, method);
									break;
								}
							}
							if (inserted == false) {
								sortedNames.add(methodName);
								sortedMethods.add(method);
							}
						}
					}
				}
				for (Method method : sortedMethods) {					
					String methodName = method.getName();
					methodToSearchCombo.addItem(method);
					if (methodName != "main") {
						valuesToSearchForModel.addRow(new Object[] {Boolean.FALSE, methodName});
					}
				}
			}
			valuesToSearchForModel.addRow(new Object[] {null, ""});
		}
		else {
			JOptionPane.showMessageDialog(null, "Golden source does not compile.  You can view the compiler message in the source window of the main screen.", "Golden Source Does Not Compile",
					JOptionPane.ERROR_MESSAGE);
		}
		enableTableListener = true;
	}


	private class MethodComboRenderer extends JLabel implements ListCellRenderer<Method>{

		@Override
		public Component getListCellRendererComponent(JList list, Method value, int index, boolean isSelected,
				boolean cellHasFocus) {
			if (value != null && value instanceof Method) {
				Method method = (Method)value;
				String comboDisplay = method.toString();
				int methodIndex = comboDisplay.indexOf(method.getName());
				String finalDisplay = comboDisplay;
				if (methodIndex != -1) {
					finalDisplay = comboDisplay.substring(methodIndex);
				}
				setText(finalDisplay);
			}
			else {
				setText("");
			}


			return this;
		}

	}
}
