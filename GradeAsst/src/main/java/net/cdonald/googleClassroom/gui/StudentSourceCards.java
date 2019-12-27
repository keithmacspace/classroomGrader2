package net.cdonald.googleClassroom.gui;

import java.awt.CardLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.undo.UndoManager;

import net.cdonald.googleClassroom.listenerCoordinator.GetStudentFilesQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RecompileListener;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveSourceListener;
import net.cdonald.googleClassroom.model.FileData;

public class StudentSourceCards extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8382429905694434119L;
	private Map<String, StudentSourceCard> sourceCardMap;
	public StudentSourceCards() {
		super();
		setLayout(new CardLayout());		
		sourceCardMap = new HashMap<String, StudentSourceCard>();
	}
	
	public void switchSource(String currentID) {
		if (currentID == null) {
			currentID = "null";
		}
		CardLayout cardLayout = (CardLayout)getLayout();
		cardLayout.show(this, currentID);	
	}
	
	public void syncSource(String currentID) {
		if (currentID != null && sourceCardMap.get(currentID) != null) {
			StudentSourceCard studentCard = sourceCardMap.get(currentID);
			studentCard.syncSource();
		}
	}
	
	public void recompile(String currentID) {
		if (currentID != null && sourceCardMap.get(currentID) != null) {
			StudentSourceCard studentCard = sourceCardMap.get(currentID);
			studentCard.recompile();
		}		
	}
	
	public void removeSource(String currentID) {
		if (currentID != null && sourceCardMap.get(currentID) != null) {
			StudentSourceCard studentCard = sourceCardMap.get(currentID);
			studentCard.removeSource();
		}		
	}
	
	public JTextArea getCurrentSource(String currentID) {
		if (currentID != null && sourceCardMap.get(currentID) != null) {
			StudentSourceCard studentCard = sourceCardMap.get(currentID);
			studentCard.getCurrentSource();
		}
		return null;
	}
	
	public void initSource(List<String> allIDs, UndoManager undoManager, JTabbedPane overallTabbedPane, int overallTabbedIndex, JPopupMenu popupSource ) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				sourceCardMap.clear();						
				for (String id : allIDs) {
					StudentSourceCard studentCard = new StudentSourceCard(undoManager, id, overallTabbedPane, overallTabbedIndex, popupSource);
					sourceCardMap.put(id, studentCard);
					add(studentCard, id);					
				}
				add(new JPanel(), "null");				
			}
		});
	}
	
	
	private class StudentSourceCard extends JTabbedPane{
		private static final long serialVersionUID = -8611112574356174862L;
		private String studentID;	
		private List<LineNumberTextArea> sourceTextArea;
		public StudentSourceCard(UndoManager undoManager, String studentID, JTabbedPane overallTabPane, int overallTabIndex, JPopupMenu popupSource) {
			super();
			this.studentID = studentID;
			sourceTextArea = new ArrayList<LineNumberTextArea>();
			new TabbedUndoListener(undoManager, this);
			@SuppressWarnings("unchecked")
			List<FileData> fileDataList = (List<FileData>) ListenerCoordinator.runQuery(GetStudentFilesQuery.class, studentID);		
			if (fileDataList != null) {
				for (FileData fileData : fileDataList) {
					String name = fileData.getName();
					LineNumberTextArea sourceArea = new LineNumberTextArea();
					sourceArea.setText(fileData.getFileContents());
					sourceTextArea.add(sourceArea);
					addTab(name, sourceArea.getScrollPane());
					sourceArea.setComponentPopupMenu(popupSource);
					sourceArea.getDocument().addUndoableEditListener(undoManager);
				}
			}		
		}

		public void removeSource() {
			int currentTab = getSelectedIndex();
			String fileName = getTitleAt(currentTab);
			sourceTextArea.remove(currentTab);
			removeTabAt(currentTab);
			ListenerCoordinator.fire(RemoveSourceListener.class, studentID, fileName);		
		}

		public JTextArea getCurrentSource() {		
			int currentTab = getSelectedIndex();			
			if (currentTab < sourceTextArea.size()) {
				JTextArea currentSourceArea = sourceTextArea.get(currentTab);
				return currentSourceArea;
			}
			return null;
		}

		public void syncSource() {
			@SuppressWarnings("unchecked")
			List<FileData> fileDataList = (List<FileData>) ListenerCoordinator.runQuery(GetStudentFilesQuery.class, studentID);			
			if (fileDataList != null) {
				for (FileData fileData : fileDataList) {
					String name = fileData.getName();
					for (int i = 0; i < getTabCount(); i++) {
						if (getTitleAt(i).equals(name)) {
							LineNumberTextArea sourceArea = sourceTextArea.get(i);
							if (sourceArea.isModified()) {
								fileData.setFileContents(sourceArea.getText());
							}
						}
					}
				}
			}		
		}

		public void recompile() {
			syncSource();
			ListenerCoordinator.fire(RecompileListener.class, studentID);

		}
	}	

}
