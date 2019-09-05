package net.cdonald.googleClassroom.listenerCoordinator;

import java.util.List;

import net.cdonald.googleClassroom.model.FileData;

public interface GetStudentFilesQuery {
	public List<FileData> fired(String studentID);
}
