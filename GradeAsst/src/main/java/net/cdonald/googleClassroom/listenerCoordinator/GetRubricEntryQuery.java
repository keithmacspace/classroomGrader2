package net.cdonald.googleClassroom.listenerCoordinator;

import net.cdonald.googleClassroom.model.RubricEntry;

public interface GetRubricEntryQuery {
	RubricEntry fired(int columnNumber);
}
