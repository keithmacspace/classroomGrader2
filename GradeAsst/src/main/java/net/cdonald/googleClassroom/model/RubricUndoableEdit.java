package net.cdonald.googleClassroom.model;

import java.util.ArrayList;

import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;

import net.cdonald.googleClassroom.model.RubricEntry.StudentScore;

public class RubricUndoableEdit extends AbstractUndoableEdit {
	private static final long serialVersionUID = 1L;
	private static class RubricEntryInfo {
		public RubricEntryInfo(int rubricEntryIndex, StudentScore studentScorePreChange,
				StudentScore studentScorePostChange) {
			super();
			this.rubricEntryIndex = rubricEntryIndex;
			this.studentScorePreChange = studentScorePreChange;
			this.studentScorePostChange = studentScorePostChange;
		}
		private int rubricEntryIndex;
		private RubricEntry.StudentScore studentScorePreChange;
		private RubricEntry.StudentScore studentScorePostChange;
	}
	ArrayList<RubricEntryInfo> undoInfo;
	public RubricUndoableEdit(int rubricEntry, StudentScore preChange, StudentScore postChange) {
		undoInfo = new ArrayList<RubricEntryInfo>();
		undoInfo.add(new RubricEntryInfo(rubricEntry, preChange, postChange));
	}

	@Override
	public void undo() throws CannotUndoException {
		// TODO Auto-generated method stub
		super.undo();
	}

	@Override
	public boolean canUndo() {
		// TODO Auto-generated method stub
		return super.canUndo();
	}

	@Override
	public void redo() throws CannotRedoException {
		// TODO Auto-generated method stub
		super.redo();
	}

	@Override
	public boolean canRedo() {
		// TODO Auto-generated method stub
		return super.canRedo();
	}

	@Override
	public boolean addEdit(UndoableEdit anEdit) {
		// TODO Auto-generated method stub
		return super.addEdit(anEdit);
	}

	@Override
	public boolean replaceEdit(UndoableEdit anEdit) {
		// TODO Auto-generated method stub
		return super.replaceEdit(anEdit);
	}

	@Override
	public boolean isSignificant() {
		// TODO Auto-generated method stub
		return super.isSignificant();
	}
	

}
