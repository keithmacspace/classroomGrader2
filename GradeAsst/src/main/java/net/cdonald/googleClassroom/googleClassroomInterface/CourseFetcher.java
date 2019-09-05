package net.cdonald.googleClassroom.googleClassroomInterface;

import java.util.Map;

import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryResponder;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.model.ClassroomData;

public class CourseFetcher extends ClassroomDataFetcher {
	private final String PROGRESS_BAR_NAME = "Downloading Classes";
	public CourseFetcher(GoogleClassroomCommunicator authorize) {
		super(authorize);
	}
	
	@Override
	protected void done() {
		ListenerCoordinator.fire(RemoveProgressBarListener.class, PROGRESS_BAR_NAME);
		super.done();
	}
	
	
	
	@Override
	protected Void doInBackground() throws Exception {
		ListenerCoordinator.fire(AddProgressBarListener.class, PROGRESS_BAR_NAME);
		authorize.getClasses(this);
		ListenerCoordinator.fire(RemoveProgressBarListener.class, PROGRESS_BAR_NAME);
		return null;
	}


	@Override
	protected ClassroomData newData(Map<String, String> initData) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public LongQueryResponder<ClassroomData> newInstance() {
		return new CourseFetcher(authorize);
	}



}
