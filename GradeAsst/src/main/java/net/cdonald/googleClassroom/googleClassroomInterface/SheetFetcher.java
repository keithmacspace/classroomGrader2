package net.cdonald.googleClassroom.googleClassroomInterface;

import java.io.IOException;
import java.util.Map;

import net.cdonald.googleClassroom.listenerCoordinator.LongQueryResponder;
import net.cdonald.googleClassroom.listenerCoordinator.SheetFetcherListener;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.GoogleSheetData;

public class SheetFetcher extends ClassroomDataFetcher {



	
	public SheetFetcher( GoogleClassroomCommunicator authorize) {
		super(authorize);
	}
	


	@Override
	protected Void doInBackground() throws Exception {
		SheetFetcherListener listener = (SheetFetcherListener)getListener();
		if (listener != null) {
			String url = listener.getSheetURL();
			//readDataBase();
			try {
				authorize.getSheetNames(url, this);
			} catch (IOException e) {
				communicationException = e;
			}
		}
		return null;
	}

	@Override
	protected ClassroomData newData(Map<String, String> initData) {
		return new GoogleSheetData(initData);
	}
	
	@Override
	public LongQueryResponder<ClassroomData> newInstance() {
		return new SheetFetcher(authorize);
	}

}
