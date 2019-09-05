package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;


public class GuidedSetupFinalInstructions extends JDialog {
	private static final long serialVersionUID = -5091221520347243985L;

	
	GuidedSetupFinalInstructions(Frame parent) {
		super(parent, "Final Steps", false);
		JPanel finalInfo = new JPanel();
		final int SPACE = 5;		
		finalInfo.setBorder(BorderFactory.createEmptyBorder(SPACE, SPACE, SPACE, SPACE));
		finalInfo.setLayout(new BorderLayout());
		
		
		JTextArea finalInstructions = new JTextArea();
		finalInstructions.setEditable(false);

		finalInstructions.append("Last Three Steps:\n");
		finalInstructions.append("1. Select the class you will be grading from the \"Open Classroom\" option under the File menu\n");
		finalInstructions.append("2. Set the grade file to use from the \"Choose Grade File...\" option under the File menu\n");
		finalInstructions.append("3. Select the rubric file to use from the \"Rubric File...\" option under the Rubrics menu\n");		
		finalInfo.add(new JScrollPane(finalInstructions), BorderLayout.CENTER);
		JButton ok = new JButton("OK");
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout());
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, SPACE, SPACE, SPACE));
		buttonPanel.add(ok, BorderLayout.EAST);
		add(finalInfo, BorderLayout.CENTER);
		add(buttonPanel, BorderLayout.SOUTH);
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				setVisible(false);
				
			}			
		});
		
		pack();
	}
}
