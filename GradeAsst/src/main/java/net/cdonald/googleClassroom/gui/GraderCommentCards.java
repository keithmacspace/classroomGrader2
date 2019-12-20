package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.undo.UndoManager;

import net.cdonald.googleClassroom.listenerCoordinator.AllStudentNamesLoadedListener;
import net.cdonald.googleClassroom.listenerCoordinator.GetAndClearModifiedNotes;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentIDListQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GradesSyncedListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;

public class GraderCommentCards extends JPanel {
	private static final long serialVersionUID = -402426283675751733L;
	private Map<String, TrackModifiedTextArea> notesAreas;	
	public GraderCommentCards(UndoManager undoManager) {
		super();
		setLayout(new CardLayout());
		notesAreas = new HashMap<String, TrackModifiedTextArea>();
		ListenerCoordinator.addListener(AllStudentNamesLoadedListener.class, new AllStudentNamesLoadedListener() {
			public void fired() {
				@SuppressWarnings("unchecked")
				List<String> allIDs = (List<String>)ListenerCoordinator.runQuery(GetStudentIDListQuery.class);
				JPopupMenu popupSource = createPopup();
				for (String studentID : allIDs) {
					JPanel notePanel = new JPanel(new BorderLayout());
					TrackModifiedTextArea noteArea = new TrackModifiedTextArea();
					String studentName = (String)ListenerCoordinator.runQuery(GetStudentNameQuery.class, studentID);
					String title = "Notes for " + studentName;
					noteArea.setBorder(BorderFactory.createTitledBorder(title));
					noteArea.setComponentPopupMenu(popupSource);
					noteArea.getDocument().addUndoableEditListener(undoManager);
					noteArea.setToolTipText("You will need to manually copy the notes to Google classroom");
					noteArea.setWrapStyleWord(true);
					noteArea.setLineWrap(true);
					JScrollPane jsp = new JScrollPane(noteArea);
					jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
					notePanel.add(jsp);			
					notesAreas.put(studentID, noteArea);
					add(notePanel, studentID);
				}
				JPanel nullPanel = new JPanel();
				add(nullPanel, (String)"null");
				switchComment("null");
			}
		});
		ListenerCoordinator.addListener(GradesSyncedListener.class, new GradesSyncedListener() {
			public void fired(Map<String, String> commentMap) {
				initComments(commentMap);
			}			
		});

		ListenerCoordinator.addQueryResponder(GetAndClearModifiedNotes.class, new GetAndClearModifiedNotes() {
			@Override
			public Map<String, String> fired() {
				return getModifiedComments();
			}
		});
	}
	
	public void switchComment(String studentID) {
		CardLayout cardLayout = (CardLayout)getLayout();
		cardLayout.show(this, "" + studentID);
	}
	
	private JPopupMenu createPopup() {
		return null;
	}
	
	public void initComments(Map<String, String> comments) {		
		for (String studentID : comments.keySet()) {
			TrackModifiedTextArea textArea = notesAreas.get(studentID);
			if (textArea != null) {
				textArea.setText(comments.get(studentID));
				textArea.setModified(false);
			}
		}
	}
	
	public Map<String, String>  getModifiedComments() {
		Map<String, String> commentMap = new HashMap<String, String>();
		for (String studentID : notesAreas.keySet()) {
			TrackModifiedTextArea textArea = notesAreas.get(studentID);
			if (textArea.isModified()) {
				commentMap.put(studentID, textArea.getText());
				textArea.setModified(false);
			}
		}
		return commentMap;
	}
	
	
	private static class TrackModifiedTextArea  extends JTextArea {
		private static final long serialVersionUID = 3084045724297493096L;
		public TrackModifiedTextArea() {
			super();
			modified = false;
			getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					modified = true;					
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					modified = true;					
				}

				@Override
				public void changedUpdate(DocumentEvent e) {					
				}			
			});
		}
		private boolean modified;
		public boolean isModified() {
			return modified;
		}
		public void setModified(boolean modified) {
			this.modified = modified;
		}
	}
}
