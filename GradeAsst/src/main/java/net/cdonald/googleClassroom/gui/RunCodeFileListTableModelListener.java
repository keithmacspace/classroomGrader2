package net.cdonald.googleClassroom.gui;

import net.cdonald.googleClassroom.model.FileData;

public interface RunCodeFileListTableModelListener {
	public void addRunCodeFile(FileData fileData);
	public void removeRunCodeFile(FileData fileData);
	public boolean containsSource(FileData fileData);
}
