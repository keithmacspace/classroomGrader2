package net.cdonald.googleClassroom.gui.rubricEditing;

import java.util.List;

import javax.swing.JButton;

import net.cdonald.googleClassroom.model.FileData;

public interface RubricFileListener {
	public enum RubricTabNames {Summary("Summary"), Reference("Reference Source"), TestCode("Test Code"), SupportCode("Support Code");
		private String name;
		RubricTabNames(String name) {
			this.name = name;
		};
		public String toString() {
			return name;
		}
	}
	public List<FileData> getReferenceSource();
	public List<FileData> getTestSource();
	public List<FileData> getSupportSource();
	public boolean isReferenceSourceSet();
	public JButton getAddSourceButtons();
	public void addCompilerMessage(String message);
	public void sourceIsChanged();
	public void compileSource(RubricTabNames sourceType);

}
