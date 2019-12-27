package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntry.AutomationTypes;
import net.cdonald.googleClassroom.model.RubricEntryMethodContains;
import net.cdonald.googleClassroom.utils.SimpleUtils;

/**
 * 
 * Broke this out purely to reduce the code in RubricElementDialog, this represents
 * the CodeContainsString Card
 *
 */
public class RubricEntryCodeContainsStringCard extends RubricEntryAutomationCardInterface{
	private static final long serialVersionUID = 1L;	
	private JComboBox<String> methodToSearchCombo;
	private JTable valuesToSearchForTable;
	private DefaultTableModel valuesToSearchForModel;
	private JLabel explanation;
	private RubricEntryMethodContains associatedAutomation;	
	private Map<String, Set<String> > methodMap;
	public RubricEntryCodeContainsStringCard(boolean enableEditing, Map<String, Set<String> > methodMap, Rubric rubricToModify, int elementID) {
		super();
		this.methodMap = methodMap;
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder("Automation Options"));
		
 
		explanation = new JLabel("<html>Method to search is the one that should contain the string(s) of interest.<br/>"
				+ "  The method must be part of your reference source. "
				+ "By default, all of the method names in the reference source are possible strings to find.<br/>"
				+ "This automaton will only search for method calls and field use i.e. Math.pow or Math.PI.<br/>"
				+ "It will not search for things like variable declarations, local variable use, comments etc.</html>");

		JPanel namePanel = createNamePanel();
		


		//codeContainsPanel.add(explanation, BorderLayout.NORTH);
		add(namePanel, BorderLayout.CENTER);
		addItems(rubricToModify, elementID);
		setEnableEditing(enableEditing);
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
						for (int row = 0; row < valuesToSearchForModel.getRowCount(); row++) {
							Object valueAtCol = valuesToSearchForModel.getValueAt(row, 0);
							if (valueAtCol instanceof Boolean) {
								Boolean booleanVal = (Boolean)valueAtCol;
								String name = (String)valuesToSearchForModel.getValueAt(row,  1);
								if (booleanVal != null && name != null && name.length() > 0) {
									String methodName = (String)methodToSearchCombo.getSelectedItem();
									if (methodName != null) {
										associatedAutomation.changeStringsToFind(methodName, name, booleanVal);
									}									
								}

							}
						}
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


		valuesToSearchForTable.getColumnModel().getColumn(0).setMaxWidth(10);
		valuesToSearchForTable.setModel(valuesToSearchForModel);
		valuesToSearchForTable.setEnabled(false);
		namePanel.setBorder(BorderFactory.createEmptyBorder(SPACE, 0, SPACE, 0));
		methodToSearchCombo = new JComboBox<String>();
		methodToSearchCombo.setEditable(false);	
		//methodToSearchCombo.setRenderer(new MethodComboRenderer());
		methodToSearchCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String methodToSearch = (String)methodToSearchCombo.getSelectedItem();
				valuesToSearchForTable.setEnabled(methodToSearch != null);
				fillMethodToFindTable(methodToSearch);
			}
		});


		SimpleUtils.addLabelAndComponent(namePanel, new JLabel(), explanation, 0);
//		SimpleUtils.addLabelAndComponent(namePanel, new JLabel(), new JLabel("                 "), 1);
		SimpleUtils.addLabelAndComponent(namePanel, methodToSearchLabel, methodToSearchCombo, 1);
		//SimpleUtils.addLabelAndComponent(namePanel, new JLabel(), new JLabel("                 "), 3);
		SimpleUtils.addLabelAndComponent(namePanel, stringToSearchFor, valuesToSearchForTable, 2);
		return namePanel;

	}
	@Override
	public String getDescription() {
		String description = "Automated. Checks the following:\n";
		description += associatedAutomation.createCompleteCallList();
		description += "If the automation does not set a grade, see the rubric tab for more info.";
		return description;					
	}
	
	@Override
	public void referenceSourceChanged(Map<String, Set<String>> methodMap) {
		this.methodMap = methodMap;
		fillMethodCombo();
	}



	public void referenceSourceEnabled(boolean enable) {
		fillMethodCombo();
	}


	public void addItems(Rubric rubricToModify, int elementID) {
		RubricEntry associatedEntry = rubricToModify.getEntryByID(elementID);
		if (associatedEntry.getAutomation() == null || !(associatedEntry.getAutomation() instanceof RubricEntryMethodContains)) {
			associatedEntry.setAutomation(new RubricEntryMethodContains());
		}

		// Do it it this way to prevent infinite recursion in when we call set value at
		associatedAutomation = null;
		for (int row = 0; row < valuesToSearchForModel.getRowCount(); row++) {
			valuesToSearchForModel.setValueAt(Boolean.FALSE, row, 0);
		}
		fillMethodCombo();
		RubricEntryMethodContains automation = (RubricEntryMethodContains)associatedEntry.getAutomation();
		associatedAutomation = automation;
	}

	@Override
	public void saving() {
		
	}
	




	private void fillMethodCombo() {
		methodToSearchCombo.removeAllItems();
		methodToSearchCombo.addItem(null);
		
		

		if (methodMap != null) {	
			List<String> sortedNames = new ArrayList<String>();		
			for (String methodName : methodMap.keySet()) {
				boolean inserted = false;
				for (int i = 0; i < sortedNames.size(); i++) {
					if (methodName.compareTo(sortedNames.get(i)) < 0) {
						inserted = true;
						sortedNames.add(i, methodName);
	
						break;
					}
				}
				if (inserted == false) {
					sortedNames.add(methodName);	
				}
			}
			for (String methodName : sortedNames) {					
				methodToSearchCombo.addItem(methodName);
			}
		}
	}
	
	private void fillMethodToFindTable(String methodSelected) {
		
		while (valuesToSearchForModel.getRowCount() > 0) {
			valuesToSearchForModel.removeRow(0);
		}
		if (methodSelected != null && methodMap != null ) {
			Set<String> calls = methodMap.get(methodSelected);
			for (String call : calls) {
				Boolean value = Boolean.FALSE;
				if (associatedAutomation != null) {
					value = associatedAutomation.requiresCall(methodSelected, call);
				}				
				valuesToSearchForModel.addRow(new Object[] {value, call});
			}
		}		

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



	@Override
	public void testSourceChanged(Map<String, List<Method>> possibleMethodMap) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public AutomationTypes getAutomationType() {
		// TODO Auto-generated method stub
		return RubricEntry.AutomationTypes.CODE_CONTAINS_METHOD;
	}

	@Override
	public void setEnableEditing(boolean enableEditing) {
		valuesToSearchForTable.setEnabled(enableEditing);
		methodToSearchCombo.setEnabled(enableEditing);
		
	}


	


}
