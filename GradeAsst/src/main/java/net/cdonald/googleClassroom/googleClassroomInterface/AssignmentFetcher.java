package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.IOException;
import java.util.Map;

import javax.swing.JProgressBar;

import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentClassQuery;
import net.cdonald.googleClassroom.listenerCoordinator.GetDBNameQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryResponder;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.model.ClassroomData;



public class AssignmentFetcher extends ClassroomDataFetcher {
	private final String PROGRESS_BAR_NAME = "Reading Assignment Names";
	public AssignmentFetcher(GoogleClassroomCommunicator authorize) {
		super(authorize);
	}
	@Override
	protected void done() {
		ListenerCoordinator.fire(RemoveProgressBarListener.class, PROGRESS_BAR_NAME);
		super.done();
	}

	@Override
	protected Void doInBackground()  {
		ListenerCoordinator.fire(AddProgressBarListener.class, PROGRESS_BAR_NAME);
		ClassroomData classSelected = (ClassroomData) ListenerCoordinator.runQuery(GetCurrentClassQuery.class);
		if (classSelected != null) {

			String classDB = (String)ListenerCoordinator.runQuery(GetDBNameQuery.class, GetDBNameQuery.DBType.CLASS_DB);
			readDataBase(classDB,  "Assignments", ClassroomData.fieldNames.class);
			try {
				authorize.getAssignments(classSelected, this);
			} catch (IOException e) {
				communicationException = e;
			}
		}		
		return null;
	}

	@Override
	protected ClassroomData newData(Map<String, String> initData) {
		// TODO Auto-generated method stub
		return new ClassroomData(initData);
	}
	
	@Override
	public LongQueryResponder<ClassroomData> newInstance() {
		return new AssignmentFetcher(authorize);
	}

}
