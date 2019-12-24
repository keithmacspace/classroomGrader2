package net.cdonald.googleClassroom.gui;

import java.util.List;

import javax.swing.JPanel;

import net.cdonald.googleClassroom.model.FileData;

public interface RubricFileListener {
	public List<FileData> getReferenceSource();
	public List<FileData> getTestSource();	
	public JPanel getTestSourceButtons();

}
