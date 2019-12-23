package net.cdonald.googleClassroom.gui;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SelectStudentListener;

public class UndoableStudentSelection extends AbstractUndoableEdit {
	private static final long serialVersionUID = 6604127429109660697L;
	private String newID = null;
	private String formerID = null;
	private static volatile boolean undoing = false;
	
	public static void studentSelected(UndoManager undoManager, String newID, String formerID) {
		if (undoing == false && newID != null && formerID != null) {
			undoManager.addEdit(new UndoableStudentSelection(newID, formerID));
		}
		undoing = false;
	}
	
	public UndoableStudentSelection(String newID, String formerID) {
		super();
		this.newID = newID;
		this.formerID = formerID;
	}
	@Override
	public boolean isSignificant() {
		return newID != null && !newID.equals(formerID);
	}

	@Override
	public void undo() throws CannotUndoException {
		if (!newID.equals(formerID)) {
			undoing = true;
			ListenerCoordinator.fire(SelectStudentListener.class, formerID);
		}
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
	public void redo() throws CannotRedoException {
		if (!newID.equals(formerID)) {
			undoing = true;
			ListenerCoordinator.fire(SelectStudentListener.class, newID);
		}
	}

	@Override
	public boolean addEdit(UndoableEdit anEdit) {
		if (anEdit instanceof UndoableStudentSelection) {
			this.newID = ((UndoableStudentSelection)anEdit).newID;
			return true;
		}
		return false;
		
	}

}
