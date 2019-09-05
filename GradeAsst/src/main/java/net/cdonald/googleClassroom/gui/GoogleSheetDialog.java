package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import net.cdonald.googleClassroom.googleClassroomInterface.SheetFetcher;
import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.SheetFetcherListener;
import net.cdonald.googleClassroom.model.ClassroomData;

public class GoogleSheetDialog extends JDialog {

	private static final long serialVersionUID = 3861011773987299144L;
	private JTextField url;
	private JButton okButton;
	private JButton cancelButton;	
	private Class<?> listenerClass;
	private String fileName;



	public GoogleSheetDialog(Frame parent) {
		super(parent, "", false);
		this.listenerClass = null;
		initLayout();
	}
	
	public void setVisible(String title, Class<?> listener, String currentURL) {
		this.listenerClass = listener;
		setTitle(title);
		url.setText(currentURL);
		setVisible(true);
	}

	private void initLayout() {		

		JPanel buttonsPanel = setupButtonLayout();
		JPanel controlPanel = setupControlLayout();		

		add(controlPanel, BorderLayout.CENTER);
		add(buttonsPanel, BorderLayout.EAST);
		
	
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String urlName = url.getText();	
				validate(urlName);
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
	
	private void validate(String urlName) {

		fileName = null;
		
		ListenerCoordinator.fire(AddProgressBarListener.class, "Validating URL");
		
		ListenerCoordinator.runLongQuery(SheetFetcher.class, new SheetFetcherListener(urlName) {
			@Override
			public void process(List<ClassroomData> list) {
				for (ClassroomData data : list) {
					if (data.isEmpty()) {
						fileName =  data.getName();
					}
				}
			}					
			@Override
			public void done() {
				ListenerCoordinator.fire(RemoveProgressBarListener.class, "Validating URL");
				if (fileName != null) {
					ListenerCoordinator.fire(listenerClass, urlName, fileName);
					setVisible(false);
				}
				else {
					JOptionPane.showMessageDialog(null, "Make sure you have the name correct\nand permissions to access the file", "Cannot Accesss URL",
							JOptionPane.ERROR_MESSAGE);
				}
			}
			@Override
			public void remove(Set<String> removeList) {
				
				
			}
		});	
	}


	
	private JPanel setupButtonLayout() {
		int SPACE = 6;
		JPanel buttonsPanel = new JPanel();
		Border spaceBorder = BorderFactory.createEmptyBorder(SPACE, SPACE, SPACE, SPACE);
		buttonsPanel.setBorder(spaceBorder);
		GridLayout buttonLayout = new GridLayout(3, 0);
		final int GAP_SIZE = 6;
		buttonLayout.setVgap(GAP_SIZE);			
		buttonsPanel.setLayout(buttonLayout);
		
		okButton = new JButton("OK");
		cancelButton = new JButton("Cancel");
		buttonsPanel.add(okButton);
		buttonsPanel.add(cancelButton);
		setLayout(new BorderLayout());
		return buttonsPanel;
	}
	private JPanel setupControlLayout() {
		Border titleBorder = BorderFactory.createTitledBorder("Copy in the shareable link of the Google sheet you wish to use");
		JPanel controlPanel = new JPanel();				
		url = new JTextField(40);
		controlPanel.setBorder(titleBorder);
		controlPanel.setLayout(new GridBagLayout());
		GridBagConstraints urlConstraints = new GridBagConstraints();
		urlConstraints.fill = GridBagConstraints.HORIZONTAL;
		urlConstraints.gridwidth = GridBagConstraints.REMAINDER;
		addLabelAndComponent(controlPanel, "url:", url, 0, urlConstraints);
		
		fileName = null;
		
		return controlPanel;
	}
	
	
	
	private void addLabelAndComponent(JPanel parent, String label, Component component, int y, GridBagConstraints c) {

		GridBagConstraints l = new GridBagConstraints();
		l.weightx = 0;
		l.weighty = 0;
		l.gridx = 0;
		l.gridy = y;
		l.gridheight = 1;	
		l.anchor = GridBagConstraints.LINE_END;
		parent.add(new JLabel(label), l);		
		c.weightx = 1.0;
		c.weighty = 0.0;		
		c.anchor = GridBagConstraints.LINE_START;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = y;

		parent.add(component, c);
	}

}
