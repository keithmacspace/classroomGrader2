package net.cdonald.googleClassroom.listenerCoordinator;

public interface GetDBNameQuery {
	public enum DBType{
		CLASS_DB,
		RUBRIC_DB,
		ASSIGNMENT_FILES_DB,
		STUDENT_DB,
	}
	public String fired(DBType type);
	

}
