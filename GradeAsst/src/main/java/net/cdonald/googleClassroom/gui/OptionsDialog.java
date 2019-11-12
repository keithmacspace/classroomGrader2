package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.OptionsDialogUpdated;
import net.cdonald.googleClassroom.model.MyPreferences;
import net.cdonald.googleClassroom.utils.SimpleUtils;

@SuppressWarnings("serial")
public class OptionsDialog extends JDialog{
	private JTextField userNameField;
	private JComboBox<Integer> lateTimeCombo;
	private JComboBox<MyPreferences.LateType> lateTypeCombo;
	private JCheckBox redDatesCheckBox;
	private JCheckBox anonymousCheckBox;
	private JCheckBox collectDebugInfoCheckBox;
	
	public OptionsDialog(Frame parent, MyPreferences prefs) {
		super(parent, "Options", true);
		setLayout(new BorderLayout());		
		
		JPanel namePanel = createNamePanel(prefs);
		JPanel lateDatesPanel = createLateDatesPanel(prefs);
		JPanel miscPanel = createMiscOptions(prefs);
		
		JPanel optionsPanel = new JPanel(new BorderLayout());
		optionsPanel.add(lateDatesPanel, BorderLayout.CENTER);
		optionsPanel.add(miscPanel, BorderLayout.SOUTH);
		JPanel buttonsPanel = createButtonsPanel(prefs);

		add(namePanel, BorderLayout.NORTH);
		add(optionsPanel, BorderLayout.CENTER);
		add(buttonsPanel, BorderLayout.SOUTH);
		pack();
	}
	
	private JPanel createNamePanel(MyPreferences prefs) {
		JPanel namePanel = new JPanel();
		namePanel.setLayout(new GridBagLayout());
		namePanel.setBorder(BorderFactory.createTitledBorder("Grader name"));		
		userNameField = new JTextField(20);
		String name = prefs.getUserName();
		userNameField.setText(name);
		JLabel nameLabel = new JLabel("Name");
		nameLabel.setToolTipText("Name that appears in the google sheet under grader");
		SimpleUtils.addLabelAndComponent(namePanel,nameLabel, userNameField, 0);
		return namePanel;
		
	}
	
	private JPanel createLateDatesPanel(MyPreferences prefs) {
		JPanel lateDatesPanel = new JPanel(new GridBagLayout());
		lateDatesPanel.setBorder(BorderFactory.createTitledBorder("Late Date Handling"));
		JLabel redDatesLabel = new JLabel("Late Dates In Red");
		JLabel redTimesLabel = new JLabel("No red if late by less than:");
		redDatesCheckBox = new JCheckBox();
		redDatesCheckBox.setSelected(prefs.getLateDatesInRed());
		SimpleUtils.addLabelAndComponent(lateDatesPanel, redDatesLabel, redDatesCheckBox, 0);
		redDatesCheckBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						enableDisableLateTypes(redTimesLabel, redDatesCheckBox.isSelected());
					}
				});
			}			
		});
		
		lateTimeCombo = new JComboBox<Integer>();
		lateTimeCombo.setEditable(false);
		for (int i = 0; i < 100; i++) {
			lateTimeCombo.addItem((Integer)i);
		}
		lateTimeCombo.setSelectedItem(prefs.getLateDateTime());
		lateTypeCombo = new JComboBox<MyPreferences.LateType>();
		lateTypeCombo.setEditable(false);
		lateTypeCombo.addItem(MyPreferences.LateType.Minutes);
		lateTypeCombo.addItem(MyPreferences.LateType.Hours);
		lateTypeCombo.addItem(MyPreferences.LateType.Days);
		lateTypeCombo.setSelectedItem(prefs.getLateType());
		
		enableDisableLateTypes(redTimesLabel, redDatesCheckBox.isSelected());
		
		JPanel lateOptions = new JPanel(new FlowLayout(FlowLayout.LEADING));
		lateOptions.add(lateTimeCombo);
		lateOptions.add(lateTypeCombo);
		SimpleUtils.addLabelAndComponent(lateDatesPanel, redTimesLabel, lateOptions, 1);
		return lateDatesPanel;
	}
	
	private void enableDisableLateTypes(JLabel redTimesLabel, boolean enabled) {
		redTimesLabel.setEnabled(enabled);
		lateTimeCombo.setEnabled(enabled);
		lateTypeCombo.setEnabled(enabled);
		
	}
	
	
	private JPanel createMiscOptions(MyPreferences prefs) {
		JPanel miscOptions = new JPanel(new GridBagLayout());
		miscOptions.setBorder(BorderFactory.createTitledBorder("Misc Options"));
		// Anonymous names
		JLabel anonymousCheckBoxLabel = new JLabel("Anonymous Names");
		anonymousCheckBoxLabel.setToolTipText("The actual names will still appear in the google sheet");
		anonymousCheckBox = new JCheckBox();
		anonymousCheckBox.setSelected(prefs.getAnonymousNames());
		addCheckBox(miscOptions, anonymousCheckBoxLabel, anonymousCheckBox, 0, 0);
		JLabel collectDebugInfo = new JLabel("Enable Debug Dialog");
		collectDebugInfo.setToolTipText("If enabled, You can view the debug dialog under the help menu.");		
		collectDebugInfoCheckBox = new JCheckBox();
		collectDebugInfoCheckBox.setSelected(prefs.getCollectDebugInfo());
		addCheckBox(miscOptions, collectDebugInfo, collectDebugInfoCheckBox, 2, 0);
		

		return miscOptions;
	}
	
	private void addCheckBox(JPanel parent, JLabel label, JCheckBox checkBox, int x, int y) {
		GridBagConstraints l = new GridBagConstraints();
		l.insets = new Insets(3, 3, 3, 0);
		l.weightx = 0;
		l.weighty = 0;
		l.gridx = x;
		l.gridy = y;
		l.gridheight = 1;
		l.anchor = GridBagConstraints.LINE_END;
		parent.add(label, l);		
	

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 0, 3, 3);
		c.weightx = 1.0;
		c.weighty = 0.0;
		//c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		//c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		c.gridx = x + 1;
		c.gridy = y;
		parent.add(checkBox, c);
	
	}
	private JPanel createButtonsPanel(MyPreferences prefs) {
		// Buttons
		JPanel buttonsPanel = new JPanel();
		buttonsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");
		buttonsPanel.add(cancelButton);
		okButton.setPreferredSize(cancelButton.getPreferredSize());
		buttonsPanel.add(okButton);
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
					prefs.setLateDatesInRed(redDatesCheckBox.isSelected());
					prefs.setLateDateTypeAndTime((MyPreferences.LateType)lateTypeCombo.getSelectedItem(), (Integer)lateTimeCombo.getSelectedItem());
					prefs.setUserName(userNameField.getText());					
					DebugLogDialog.setEnableDBG(collectDebugInfoCheckBox.isSelected());
					prefs.setCollectDebugInfo(collectDebugInfoCheckBox.isSelected());
					prefs.setAnonymousNames(anonymousCheckBox.isSelected());
					ListenerCoordinator.fire(OptionsDialogUpdated.class, anonymousCheckBox.isSelected());
					setVisible(false);
								
			}			
		});
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {				
					setVisible(false);							
			}			
			
		});
		return buttonsPanel;
		
	}

}
