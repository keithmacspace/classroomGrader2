package net.cdonald.googleClassroom.inMemoryJavaCompiler;

import java.security.Permission;

public class StopExitSecurityManager extends SecurityManager {

	private SecurityManager prior;
	public StopExitSecurityManager() {
		super();
 
	}
	
	public void install() {
		if (prior == null) {
			prior = System.getSecurityManager();
			System.setSecurityManager(this);
		}
	}
	
	public void uninstall() {
		if (prior != null) {
			System.setSecurityManager(prior);
			prior = null;
		}
	}
	 
	@Override
	public void checkPermission(Permission perm) {
		
	}

	@Override
	public void checkExit(int status) {
		throw new ExitTrappedException();
	}

}
