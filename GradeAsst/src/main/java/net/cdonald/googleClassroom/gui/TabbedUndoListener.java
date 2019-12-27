package net.cdonald.googleClassroom.gui;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

public class TabbedUndoListener  implements UndoableEditListener {	
	private UndoManager undoManager;
	private static volatile boolean undoing = false;	
	private int priorSelection;


	public TabbedUndoListener(UndoManager undoManager, JTabbedPane tabbedPane) {		
		this.undoManager = undoManager;
		priorSelection = 0;

		tabbedPane.addChangeListener(new ChangeListener() {
			@Override
			public void stateChanged(ChangeEvent e) {
				if (undoing == false) {
					Object probablePane = e.getSource();
					if (probablePane == tabbedPane) {
						
							int currentSelection = tabbedPane.getSelectedIndex();					
							if (priorSelection != currentSelection) {													
								undoManager.addEdit(new UndoableTabSelection(tabbedPane, currentSelection, priorSelection));
							}
							priorSelection = currentSelection;
						}
					
				}
			}
		});
	}

	public void undoableEditHappened(UndoableEditEvent e) {
		undoManager.addEdit(e.getEdit());
	}

	private class UndoableTabSelection extends AbstractUndoableEdit {
		private static final long serialVersionUID = 5528788393235202282L;
		private JTabbedPane tabbedPane;
		private int priorSelection = -1;
		private int currentSelection = -1;
		public UndoableTabSelection(JTabbedPane tabbedPane, int currentSelection, int priorSelection) {
			super();
			this.priorSelection = priorSelection;
			this.currentSelection = currentSelection;
			this.tabbedPane = tabbedPane;
		}
		@Override
		public void undo() throws CannotUndoException {
			undoing = true;
			if (tabbedPane.getTabCount() > priorSelection) {
				tabbedPane.setSelectedIndex(priorSelection);
			}
			undoing = false;
		}
		@Override
		public void redo() throws CannotRedoException {
			undoing = true;
			if (tabbedPane.getTabCount() > currentSelection) {
				tabbedPane.setSelectedIndex(currentSelection);
			}
			undoing = false;
		}
		
		@Override
		public boolean canUndo() {
			return true;
		}
		
		@Override 
		public boolean canRedo() {
			return true;
		}
		
		@Override
		public boolean isSignificant() {
			return currentSelection != priorSelection;
		}
		
		@Override
		public boolean addEdit(UndoableEdit anEdit) {
			if (anEdit instanceof UndoableTabSelection) {
				 UndoableTabSelection other =  ((UndoableTabSelection)anEdit);
				 if (other.tabbedPane == tabbedPane) {
					 this.currentSelection = ((UndoableTabSelection)anEdit).currentSelection;
					 return true;
				 }				
			}
			return false;			
		}
	}
}

