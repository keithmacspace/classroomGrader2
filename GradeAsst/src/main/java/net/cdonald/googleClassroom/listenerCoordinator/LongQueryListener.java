package net.cdonald.googleClassroom.listenerCoordinator;

import java.util.List;
import java.util.Set;

public abstract class LongQueryListener<V> {

	public abstract void process(List<V> list);
	public abstract void remove(Set<String> removeList);
	public void done() {}
}
