package net.cdonald.googleClassroom.gui;

import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetInfoLabelListener;


public class InfoPanel extends JPanel {
	private Map<String, JProgressBar> progressBars;
	private JLabel [] infoLabels;
	private String [] defaultStrings;
	private final String INFO_STRING = "                                                                      ";
	private final String RUNNING_STRING = "                           ";
	private final String GRADE_FILE_STRING = "                          ";
	public InfoPanel() {
		super();
		infoLabels = new JLabel[SetInfoLabelListener.LabelTypes.values().length];
		defaultStrings = new String[infoLabels.length];		
		progressBars = new HashMap<String, JProgressBar>();
		defaultStrings[SetInfoLabelListener.LabelTypes.GRADE_FILE.ordinal()] = GRADE_FILE_STRING;
		defaultStrings[SetInfoLabelListener.LabelTypes.RUNNING.ordinal()] = RUNNING_STRING;
		defaultStrings[SetInfoLabelListener.LabelTypes.RUBRIC_INFO.ordinal()] = INFO_STRING;
		setLayout(new FlowLayout(FlowLayout.LEFT));
		for (int i = 0; i < defaultStrings.length; i++) {
			infoLabels[i] = new JLabel(defaultStrings[i]);
			add(infoLabels[i]);
		}
		ListenerCoordinator.addBlockingListener(AddProgressBarListener.class, new AddProgressBarListener() {
			@Override
			public void fired(String progressBarName) {
				JProgressBar progress = new JProgressBar();
				progress.setString(progressBarName);	
				progress.setIndeterminate(true);
				progress.setStringPainted(true);
				progressBars.put(progressBarName, progress);
				add(progress);
				revalidate();
				repaint();
			}
		});
		ListenerCoordinator.addBlockingListener(RemoveProgressBarListener.class, new RemoveProgressBarListener() {
			@Override
			public void fired(String progressBarName) {
				JProgressBar progress = progressBars.remove(progressBarName);
				if (progress != null) {
					progress.setVisible(false);
					remove(progress);
					revalidate();
					repaint();
				}
			}
		});
		

		ListenerCoordinator.addBlockingListener(SetInfoLabelListener.class, new SetInfoLabelListener() {
			@Override
			public void fired(SetInfoLabelListener.LabelTypes labelType, String text) {
				JLabel label = infoLabels[labelType.ordinal()];
				String mergeText = defaultStrings[labelType.ordinal()];
				if (labelType == SetInfoLabelListener.LabelTypes.GRADE_FILE) {
					text = "Grade File: " + text;
				}
				if (text.length() < mergeText.length()) {
					text += mergeText.substring(text.length());
				}
				label.setText(text);
			}			
		});

		
	}

}
