package net.cdonald.googleClassroom.googleClassroomInterface;


import java.io.IOException;
import java.util.Map;

import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentClassQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetDBNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryResponder;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.StudentData;

public class StudentFetcher extends ClassroomDataFetcher {

	private final String PROGRESS_BAR_NAME = "Reading Students";
	
	public StudentFetcher( GoogleClassroomCommunicator authorize) {		
		super(authorize);

	}
	@Override
	protected void done() {
		ListenerCoordinator.fire(RemoveProgressBarListener.class, PROGRESS_BAR_NAME);
		super.done();
	}

	@Override
	protected Void doInBackground()  {		
		ClassroomData classSelected = (ClassroomData) ListenerCoordinator.runQuery(GetCurrentClassQuery.class);
		if (classSelected != null) {
			ListenerCoordinator.fire(AddProgressBarListener.class, PROGRESS_BAR_NAME);
			String classDB = (String)ListenerCoordinator.runQuery(GetDBNameQuery.class, GetDBNameQuery.DBType.STUDENT_DB);

			readDataBase(classDB, StudentData.DB_TABLE_NAME, StudentData.fieldNames.class);
			try {
				authorize.getStudents(classSelected, this);
			} catch (IOException e) {
				communicationException = e;
			}
			ListenerCoordinator.fire(RemoveProgressBarListener.class, PROGRESS_BAR_NAME);
		}
		
		return null;
	}


	@Override
	protected ClassroomData newData(Map<String, String> initData) {
		// TODO Auto-generated method stub
		return new StudentData(initData);
	}

	@Override
	public LongQueryResponder<ClassroomData> newInstance() {
		return new StudentFetcher(authorize);
	}
}
