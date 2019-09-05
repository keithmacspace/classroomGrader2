package net.cdonald.googleClassroom.inMemoryJavaCompiler;

import net.cdonald.googleClassroom.gui.DataUpdateListener;

public interface CompileListener extends DataUpdateListener {
	public void compileDone();

}
