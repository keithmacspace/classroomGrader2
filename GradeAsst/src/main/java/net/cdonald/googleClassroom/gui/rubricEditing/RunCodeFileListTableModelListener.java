package net.cdonald.googleClassroom.gui.rubricEditing;

import java.util.List;
import net.cdonald.googleClassroom.model.FileData;

public interface RunCodeFileListTableModelListener {
	public List<FileData> getFiles();
	public void addRunCodeFile(FileData fileData);
	public void removeRunCodeFile(FileData fileData);
	public boolean containsSource(FileData fileData);
}
