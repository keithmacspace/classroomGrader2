package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import net.cdonald.googleClassroom.model.ConsoleData;

public class DebugLogDialog extends JDialog {
	private static DebugLogDialog dbg = null;
	private JTextArea textArea;
	public DebugLogDialog(Frame parent) {
		super(parent, "Debug Logs", Dialog.ModalityType.MODELESS);
		setLayout(new BorderLayout());
		setSize(new Dimension(400, 400));
		textArea = new JTextArea();
		dbg = this;
		add(new JScrollPane(textArea), BorderLayout.CENTER);
	}
	
	public static void showDebugInfo() {
		dbg.setVisible(true);		
	}
		
	
	public static void append(String text) {
		if (dbg != null) {
			dbg.textArea.append(text);
		}

	}
	public static void appendln(String text) {
		if (dbg != null) {
			dbg.textArea.append(text + "\n");
		}
		// When we aren't capturing stderr, then we want to just print errors to the screen
		if (!ConsoleData.CAPTURE_STDERR) {
			System.err.println(text);
		}
	}	
}
