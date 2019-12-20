package net.cdonald.googleClassroom.listenerCoordinator;

import java.util.Map;

public interface GradesSyncedListener {
	public void fired(Map<String, String> commentMap);
}
