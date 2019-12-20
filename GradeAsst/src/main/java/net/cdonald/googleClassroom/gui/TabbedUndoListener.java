package net.cdonald.googleClassroom.gui;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AbstractDocument.DefaultDocumentEvent;
import javax.swing.text.BadLocationException;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;

public class TabbedUndoListener  implements UndoableEditListener {
	public static class TabInfo {
		private JTabbedPane tabbedPane;
		private int paneNumber;
		public TabInfo(JTabbedPane tabbedPane, int paneNumber) {
			super();
			this.tabbedPane = tabbedPane;
			this.paneNumber = paneNumber;
		}
		public JTabbedPane getTabbedPane() {
			return tabbedPane;
		}
		public int getPaneNumber() {
			return paneNumber;
		}
		public void setPaneNumber(int paneNumber) {
			this.paneNumber = paneNumber;
		}
		public void selectTab() {
			tabbedPane.setSelectedIndex(paneNumber);
		}		
	}
	private TabInfo[] tabInfo;
	private UndoManager undoManager;
	private boolean addTabChangeUndo;
	private static boolean undoing = false;	

	public void undoableEditHappened(UndoableEditEvent e) {
		addTabChangeUndo = true;
		undoManager.addEdit(e.getEdit());
	}


	public TabbedUndoListener(UndoManager undoManager, TabInfo[] tabsToSelect) {
		this.tabInfo = tabsToSelect;
		this.undoManager = undoManager;
		addTabChangeUndo = false;
		for (TabInfo info : tabInfo) {
			info.getTabbedPane().addChangeListener(new ChangeListener() {
				@Override
				public void stateChanged(ChangeEvent e) {
					if (addTabChangeUndo == true && undoing == false) {
						undoManager.addEdit(new UndoableTabSelection(tabInfo));
						addTabChangeUndo = false;						
					}					
				}				
			});
		}
	}

	private class UndoableTabSelection extends AbstractUndoableEdit {
		private TabInfo[] tabInfo;
		private TabInfo[] redoInfo;
		public UndoableTabSelection(TabInfo[] tabInfo) {
			super();
			this.tabInfo = tabInfo;
			if (tabInfo.length != 0) {
				redoInfo = new TabInfo[tabInfo.length];
				for (int i = 0; i < tabInfo.length; i++) {
					redoInfo[i] = new TabInfo(tabInfo[i].getTabbedPane(), 0);
				}
			}
		}
		@Override
		public void undo() throws CannotUndoException {
			undoing = true;
			if (tabInfo != null) {
				for (int i = 0; i < tabInfo.length; i++) {
					redoInfo[i].setPaneNumber(redoInfo[i].getTabbedPane().getSelectedIndex());					
					tabInfo[i].selectTab();
				}
			}
			undoing = false;
		}
		@Override
		public boolean canUndo() {
			return tabInfo != null;
		}
		@Override
		public void redo() throws CannotRedoException {
			undoing = true;
			if (redoInfo != null) {
				for (int i = 0; i < redoInfo.length; i++) {
					redoInfo[i].selectTab();
				}
			}
			undoing = false;

		}
		@Override
		public boolean canRedo() {
			return 	redoInfo != null;
		}
		@Override
		public boolean isSignificant() {
			return true;
		}
	}
}

