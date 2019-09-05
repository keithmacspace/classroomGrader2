package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;


import net.cdonald.googleClassroom.listenerCoordinator.GetRubricNamesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.LaunchRubricEditorDialogListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RubricFileValidListener;

public class NewRubricDialog extends JDialog {
	private JTextField rubricNameField;
	private String rubricName;

	
	NewRubricDialog(Frame parent) {
		super(parent, "Choose Rubric Name", true);
		rubricNameField = new JTextField("", 40);
		final int GAP_SIZE = 6;
		
		JPanel textPanel = new JPanel();
		textPanel.setLayout(new FlowLayout());
		textPanel.add(new JLabel("Rubric name: "));
		textPanel.add(rubricNameField);
		JPanel buttonsPanel = new JPanel();
		// buttonsPanel.setLayout(new FlowLayout());
		GridLayout buttonLayout = new GridLayout(3, 0);
		JButton okButton = new JButton("OK");
		JButton cancelButton = new JButton("Cancel");
		buttonLayout.setVgap(GAP_SIZE);
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
		buttonsPanel.setLayout(buttonLayout);
		buttonsPanel.add(okButton);
		buttonsPanel.add(cancelButton);
		setLayout(new BorderLayout());
		add(textPanel, BorderLayout.CENTER);
		add(buttonsPanel, BorderLayout.EAST);
		
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				List<String> rubricNames = (List<String>)ListenerCoordinator.runQuery(GetRubricNamesQuery.class);
				String newName = rubricNameField.getText();
				boolean valid = true;
				for (String name : rubricNames) {
					if (name.equalsIgnoreCase(newName)) {
						JOptionPane.showMessageDialog(null,  newName + " is already an existing rubric name.  Select the rubric, then selecte Edit rubric, to edit it.", "Name already exists", JOptionPane.ERROR_MESSAGE);
						valid = false;
					}
				}
				if (valid) {
					rubricName = newName;
					setVisible(false);
				}
			}			
		});
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {				
				setVisible(false);
			}
		});
		pack();
	}
	
	public void launch() {
		rubricName = null;
		setVisible(true);
	}
	
	public String getRubricName() {
		return rubricName;
	}
}
