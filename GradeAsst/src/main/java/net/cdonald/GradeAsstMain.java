package net.cdonald;


import javax.swing.SwingUtilities;

import net.cdonald.googleClassroom.gui.MainGoogleClassroomFrame;

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
		


		SwingUtilities.invokeLater(new GradeAsstMain());

	}

}
