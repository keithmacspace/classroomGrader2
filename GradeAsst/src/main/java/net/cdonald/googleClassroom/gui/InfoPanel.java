package net.cdonald.googleClassroom.gui;

import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetInfoLabelListener;


public class InfoPanel extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3650819366942415507L;
	private List<JProgressBar> freeProgressBars;
	private Map<String, JProgressBar> progressBars;
	private JLabel [] infoLabels;
	private String [] defaultStrings;
	private final String INFO_STRING = "                                                                      ";
	private final String RUNNING_STRING = "                           ";
	private final String GRADE_FILE_STRING = "                          ";
	private Set<String> alreadyRemoved;
	private Set<String> running;
	public InfoPanel() {
		super();
		freeProgressBars = Collections.synchronizedList(new ArrayList<JProgressBar>());
		alreadyRemoved = Collections.synchronizedSet(new HashSet<String>());
		running = Collections.synchronizedSet(new HashSet<String>());
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
				synchronized(alreadyRemoved) {
					if (alreadyRemoved.contains(progressBarName)) {
						alreadyRemoved.remove(progressBarName);
						return;
					}
					if (running.contains(progressBarName)) {
						return;
					}
					running.add(progressBarName);
				}
				JProgressBar progress = null;
				synchronized(freeProgressBars) {
					if (freeProgressBars.size() == 0) {						
						progress = new JProgressBar();
						progress.setIndeterminate(true);
						progress.setStringPainted(true);
						add(progress);
					}
					else {
						progress = freeProgressBars.remove(0);					
					}
				}
				progress.setString(progressBarName);	
				progressBars.put(progressBarName, progress);
				progress.setVisible(true);
				revalidate();
				repaint();						
//					}
//				});
			}
		});
		
		
		ListenerCoordinator.addBlockingListener(RemoveProgressBarListener.class, new RemoveProgressBarListener() {
			@Override
			public void fired(String progressBarName) {
				removeProgressBar(progressBarName);
				revalidate();
				repaint();

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
	private void removeProgressBar(String progressBarName) {
		synchronized(alreadyRemoved) {
			if (running.contains(progressBarName)) {
				running.remove(progressBarName);
			}
			else {
				alreadyRemoved.add(progressBarName);
			}
		}
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JProgressBar progress = progressBars.remove(progressBarName);
				if (progress != null) {
					freeProgressBars.add(progress);
					progress.setVisible(false);
				}
			}
		});
		
	}

}
