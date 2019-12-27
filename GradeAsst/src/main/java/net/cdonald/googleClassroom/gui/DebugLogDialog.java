package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

import net.cdonald.googleClassroom.model.ConsoleData;

public class DebugLogDialog extends JDialog {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8504450430102266002L;
	private static DebugLogDialog dbg = null;
	private static boolean enable = true;
	private Map<Long, JTextArea> checkPointAreaMap; 
	private JTabbedPane checkPointPane;
	private JTextArea debugInfoArea;

	public DebugLogDialog(Frame parent) {
		super(parent, "Debug Logs", Dialog.ModalityType.MODELESS);
		setLayout(new BorderLayout());
		setSize(new Dimension(400, 400));
		checkPointAreaMap = new HashMap<Long, JTextArea>();
		checkPointPane = new JTabbedPane();
		debugInfoArea = new JTextArea();
		JPanel checkpointPanel = new JPanel();
		JPanel debugInfoPanel = new JPanel();
		checkpointPanel.setLayout(new BorderLayout());
		debugInfoPanel.setLayout(new BorderLayout());
		checkpointPanel.setBorder(BorderFactory.createTitledBorder("Checkpoints"));
		debugInfoPanel.setBorder(BorderFactory.createTitledBorder("DebugInfo"));
		checkpointPanel.add(new JScrollPane(checkPointPane), BorderLayout.CENTER);
		debugInfoPanel.add(new JScrollPane(debugInfoArea), BorderLayout.CENTER);
		JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, checkpointPanel, debugInfoPanel);
		add(splitPane, BorderLayout.CENTER);
		dbg = this;
	}
	
	public static void showDebugInfo() {
		if (enable) {
			dbg.setVisible(true);
		}
	}
		
	
	public static void append(String text) {
		if (dbg != null && enable) {
			dbg.debugInfoArea.append(text);
		}

	}
	public static void appendln(String text) {
		if (dbg != null && enable) {
			dbg.debugInfoArea.append(text + "\n");
		}
		// When we aren't capturing stderr, then we want to just print errors to the screen
		if (!ConsoleData.CAPTURE_STDERR) {
			System.err.println(text);
		}
	}
	
	public static void aquireSemaphore(Semaphore sem, int nestingDepth) throws InterruptedException {
		appendCheckPoint("Acquire Semaphore", 3 + nestingDepth);
		sem.acquire();
		appendCheckPoint("Got Semaphore", 3 + nestingDepth);
	}
	
	public static void startMethod() {
		appendCheckPoint("Enter ", 3);
	}
	public static void endMethod() {
		appendCheckPoint("Exit ", 3);
	}
	
	public static void appendCheckPoint(String text, int depthBack) {
		if (dbg != null && enable) {
			Long id = Thread.currentThread().getId();
			if (dbg.checkPointAreaMap.containsKey(id) == false) {
				dbg.addThread(id);
			}
			JTextArea checkPointArea = dbg.checkPointAreaMap.get(id);
			StackTraceElement [] stackTrace = Thread.currentThread().getStackTrace();
			String className = stackTrace[depthBack].getClassName();
			int dotPart = className.lastIndexOf('.');
			className = className.substring(dotPart + 1);
			
			text += " " + className;
			text += "::" + stackTrace[depthBack].getMethodName(); 
			text += " " + stackTrace[depthBack].getFileName();
			text += ":" + stackTrace[depthBack].getLineNumber();
			checkPointArea.append(text + "\n");
		}
	}
	
	private void addThread(Long id) {
		if (enable) {
			JTextArea checkPointArea = new JTextArea();
			JPanel checkPointPanel = new JPanel();
			checkPointPanel.setLayout(new BorderLayout());
			checkPointPanel.add(new JScrollPane(checkPointArea), BorderLayout.CENTER);
			checkPointPane.addTab("" + id, checkPointPanel);
			checkPointAreaMap.put(id, checkPointArea);
		}
		
	}

	public static void setEnableDBG(boolean val) {
		enable = val;
		
	}
	
	public static boolean getEnableDBG() {
		return enable;
	}
	
	public static void appendException(Throwable e) {
		appendln(e.getMessage());
		for (StackTraceElement elem : e.getStackTrace()) {
			appendln(elem.toString());
		}
	}
	
}
