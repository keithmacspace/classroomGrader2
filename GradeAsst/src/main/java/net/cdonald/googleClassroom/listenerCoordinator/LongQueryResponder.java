package net.cdonald.googleClassroom.listenerCoordinator;

import java.util.List;
import java.util.Set;

import javax.swing.SwingWorker;

public abstract class LongQueryResponder<V> extends SwingWorker<Void, V> {
	private LongQueryListener<V> listener;

	public LongQueryResponder() {

	}
	
	public LongQueryListener<V> getListener() {
		return listener;
	}

	@SuppressWarnings("unchecked")
	public void setListener(LongQueryListener<?> listener) {
		this.listener = (LongQueryListener<V>) listener;		

	}

	@Override
	protected void process(List<V> chunks) {
		listener.process(chunks);
	}

	@Override
	protected void done() {
		listener.done();
	}
	
	protected void sendRemove(Set<String> removeList) {
		listener.remove(removeList);
	}
	
	
	public abstract LongQueryResponder<V> newInstance();
	


}
