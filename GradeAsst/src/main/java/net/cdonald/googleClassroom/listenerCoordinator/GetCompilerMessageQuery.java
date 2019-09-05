package net.cdonald.googleClassroom.listenerCoordinator;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;

public interface GetCompilerMessageQuery {
	public CompilerMessage fired(String studentID);
}
