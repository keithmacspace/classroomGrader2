package net.cdonald.googleClassroom.listenerCoordinator;

public class ListenerContainer {
	boolean enabled;
	boolean blocking;
	Object listener;
	public ListenerContainer(Object listener, boolean enabled, boolean blocking) {
		super();
		this.enabled = enabled;
		this.blocking = blocking;
		this.listener = listener;
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	public boolean isBlocking() {
		return blocking;
	}
	public Object getListener() {
		return listener;
	}		
}