package net.cdonald.googleClassroom.gui.rubricEditing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.cdonald.googleClassroom.model.Rubric;
import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntry.AutomationTypes;
import net.cdonald.googleClassroom.model.RubricEntryPointLossForLate;
import net.cdonald.googleClassroom.utils.SimpleUtils;

public class RubricEntryPointLossForLateCard extends  RubricEntryAutomationCardInterface {
	private JComboBox<SimpleUtils.TimeUnit> timeUnitCombo;
	private JTextField pointsLost;
	private RubricEntryPointLossForLate associatedAutomation;

	public RubricEntryPointLossForLateCard(boolean enableEditing, Rubric rubricToModify, int elementID) {		
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder("Automation Options"));
		JPanel valuePanel = new JPanel();
		valuePanel.setLayout(new FlowLayout());
		pointsLost = new JTextField();
		pointsLost.setColumns(5);
		timeUnitCombo = new  JComboBox<SimpleUtils.TimeUnit>(SimpleUtils.TimeUnit.values());
		valuePanel.add(pointsLost);
		JLabel perLabel = new JLabel(" points lost per ");
		valuePanel.add(perLabel);
		valuePanel.add(timeUnitCombo);
		

		add(valuePanel, BorderLayout.CENTER);
		
		
		
		pointsLost.getDocument().addDocumentListener(new DocumentListener() {

			@Override
			public void insertUpdate(DocumentEvent e) {
				setValue();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				setValue();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
			}
			
		});
		
		pointsLost.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				
				
			}

			@Override
			public void focusLost(FocusEvent e) {
				setValue();
				
			}
			
		});
		timeUnitCombo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (associatedAutomation != null) {
					associatedAutomation.setTimeUnit((SimpleUtils.TimeUnit)timeUnitCombo.getSelectedItem());
				}				
			}
			
		});
		addItems(rubricToModify, elementID);
		setEnableEditing(enableEditing);
		
	}
	
	private void setValue() {
		String value = pointsLost.getText();
		
		if (value != null && value.length() > 0) {
			try {
				
				double d = Double.parseDouble(value);
				if (associatedAutomation != null) {
					associatedAutomation.setPointsLostPerTimeUnit(d);
				}					
			}
			catch (NumberFormatException ex) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						pointsLost.setText("0.0");
					}
					
				});
				
			}

		}
	}
	
	@Override
	public void saving() {
		
	}

	public void addItems(Rubric rubricToModify, int elementID) {
		RubricEntry associatedEntry = rubricToModify.getEntryByID(elementID);
	
		if (associatedEntry.getAutomation() == null || !(associatedEntry.getAutomation() instanceof RubricEntryPointLossForLate)) {
			associatedEntry.setAutomation(new RubricEntryPointLossForLate());
		}
		RubricEntryPointLossForLate temp = (RubricEntryPointLossForLate)associatedEntry.getAutomation();
		associatedAutomation = null;
		pointsLost.setText("" + temp.getPointsLostPerTimeUnit());
		timeUnitCombo.setSelectedItem(temp.getTimeUnit());
		associatedAutomation = temp;
	}

	@Override
	public void testSourceChanged(Map<String, List<Method>> possibleMethodMap) {
		// TODO Auto-generated method stub
		
	}



	@Override
	public AutomationTypes getAutomationType() {
		// TODO Auto-generated method stub
		return RubricEntry.AutomationTypes.POINT_LOSS_FOR_LATE;
	}

	@Override
	public void setEnableEditing(boolean enable) {
		timeUnitCombo.setEnabled(enable);
		pointsLost.setEditable(enable);
		
	}
	

}
