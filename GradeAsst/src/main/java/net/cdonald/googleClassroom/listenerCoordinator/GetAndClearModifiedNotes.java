package net.cdonald.googleClassroom.listenerCoordinator;

import java.util.Map;

public interface GetAndClearModifiedNotes {
	public Map<String, String> fired(boolean getAllNotes);
}
