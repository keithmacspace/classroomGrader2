package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;

import net.cdonald.googleClassroom.model.RubricEntry;
import net.cdonald.googleClassroom.model.RubricEntryPointLossForLate;

public class RubricEntryPointLossForLateCard implements  RubricEntryDialogCardInterface {
	private JComboBox<RubricEntryPointLossForLate.TimeUnit> timeUnitCombo;
	private JTextField pointsLost;
	private RubricEntryPointLossForLate associatedAutomation;
	private RubricElementDialog dialogOwner;
	private boolean isActive;
	public RubricEntryPointLossForLateCard(RubricElementDialog dialogOwner) {
		this.dialogOwner = dialogOwner;
		JPanel valuePanel = new JPanel();
		valuePanel.setLayout(new FlowLayout());
		pointsLost = new JTextField();
		pointsLost.setColumns(5);
		timeUnitCombo = new  JComboBox<RubricEntryPointLossForLate.TimeUnit>(RubricEntryPointLossForLate.TimeUnit.values());
		valuePanel.add(pointsLost);
		JLabel perLabel = new JLabel(" points lost per ");
		valuePanel.add(perLabel);
		valuePanel.add(timeUnitCombo);
		JPanel wrapperPanel = new JPanel();
		wrapperPanel.setLayout(new BorderLayout());
		wrapperPanel.add(valuePanel, BorderLayout.CENTER);
		isActive = false;
		dialogOwner.addAutomationCard(wrapperPanel, RubricEntry.AutomationTypes.POINT_LOSS_FOR_LATE);
		
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
					associatedAutomation.setTimeUnit((RubricEntryPointLossForLate.TimeUnit)timeUnitCombo.getSelectedItem());
				}				
			}
			
		});
		
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
	public void addItems() {
		isActive = true;
		RubricEntry associatedEntry = dialogOwner.getCurrentEntry();
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
	public void removeItems() {		
		isActive = false;
		associatedAutomation = null;
	}

	@Override
	public boolean isActive() {
		return isActive;
	}
	
	@Override
	public void goldenSourceEnabled(boolean enable) {
		
	}
}
