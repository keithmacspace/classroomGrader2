package net.cdonald;


import javax.swing.SwingUtilities;

import net.cdonald.googleClassroom.gui.MainGoogleClassroomFrame;
import net.cdonald.googleClassroom.model.MyPreferences;

public class GradeAsstMain implements Runnable {
	

	@Override
	public void run() {
		try {
			new MainGoogleClassroomFrame();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//new GradingWindow();
	}


	public static void main(String[] args) {
		if (args.length != 0) {
			if (args[0].equals("-uninstall")) {
				MyPreferences prefs = new MyPreferences();
				prefs.uninstall();
				return;
			}			
		}
		


		SwingUtilities.invokeLater(new GradeAsstMain());

	}

}
