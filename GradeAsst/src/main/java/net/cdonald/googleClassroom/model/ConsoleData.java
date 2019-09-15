package net.cdonald.googleClassroom.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import javax.swing.JTextArea;
import javax.swing.SwingWorker;

import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.gui.StudentConsoleAreas;
import net.cdonald.googleClassroom.listenerCoordinator.AppendOutputTextListener;
import net.cdonald.googleClassroom.listenerCoordinator.GetStudentTextAreasQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.PreRunBlockingListener;
import net.cdonald.googleClassroom.listenerCoordinator.SetAdditionalConsoleListener;
import net.cdonald.googleClassroom.listenerCoordinator.SystemInListener;
import net.cdonald.googleClassroom.listenerCoordinator.SystemOutListener;


public class ConsoleData {
	public static final boolean CAPTURE_STDERR = false;
	private PipedInputStream inPipe;
	private final PipedInputStream outPipe = new PipedInputStream();
	private final PipedInputStream errPipe = new PipedInputStream();
	private final PipedInputStream debugOutPipe = new PipedInputStream();
	private final PipedInputStream debugErrPipe = new PipedInputStream();
	private PrintWriter inWriter;
	private PrintStream outWriter;
	private PrintStream errWriter;
	private PrintStream debugOutStream;
	private PrintStream debugErrStream;
	private ConsoleWorker consoleWorker;

	private Map<String, StudentConsoleAreas> studentConsoleAreaMap;
	private String currentStudentID;
	private String currentRubricName;
	private JTextArea currentOutputTextArea;
	private JTextArea currentErrorTextArea;
	private JTextArea currentInputTextArea;
	private JTextArea additionalTextArea;
	private PrintStream oldOut;
	private PrintStream oldErr;
	private InputStream oldIn;
	private static Semaphore runSemaphore = new Semaphore(1);	



	public ConsoleData() {
		oldOut = System.out;
		oldIn = System.in;
		oldErr = System.err;
		registerListeners();
		redirectConsole();
		studentConsoleAreaMap = new HashMap<String, StudentConsoleAreas>();
		try {
			debugOutStream = new PrintStream(new PipedOutputStream(debugOutPipe), true);
			debugErrStream = new PrintStream(new PipedOutputStream(debugErrPipe), true);
		} catch (IOException e) {
			
		}
		swapStreams();
	}


	
	private void registerListeners() {
		ListenerCoordinator.addBlockingListener(SystemInListener.class, new SystemInListener() {
			@Override
			public void fired(String text) {
				// Only do this when we are running, don't accidentally absorb extra data.
				if (runSemaphore.availablePermits() == 0) {
					inWriter.println(text);
					//System.out.println();
					if (currentOutputTextArea != null) {
						currentOutputTextArea.append(text + "\n");
					}
					if (currentInputTextArea != null) {
						currentInputTextArea.append(text + "\n");
					}
				}		
			}
		});
		
		ListenerCoordinator.addBlockingListener(AppendOutputTextListener.class, new AppendOutputTextListener() {
			public void fired(String studentID, String rubricName, String text) {				
				getStudentConsoleAreaMap(studentID).appendToOutput(rubricName, text);
			}
		});
		
		ListenerCoordinator.addQueryResponder(GetStudentTextAreasQuery.class, new GetStudentTextAreasQuery() {
			@Override
			public StudentConsoleAreas fired(String studentID) {
				return getStudentConsoleAreaMap(studentID);
			}
		});
		
		ListenerCoordinator.addBlockingListener(SetAdditionalConsoleListener.class, new SetAdditionalConsoleListener() {
			@Override
			public void fired(JTextArea additionalArea) {
				additionalTextArea = additionalArea;
			}
		});
	}
	
	public StudentConsoleAreas getStudentConsoleAreaMap(String studentID) {
		if (studentConsoleAreaMap.containsKey(studentID) == false) {
			studentConsoleAreaMap.put(studentID, new StudentConsoleAreas());
		}
		return studentConsoleAreaMap.get(studentID);		
	}
	
	public void assignmentSelected() {
		studentConsoleAreaMap.clear();
	}

	public void studentSelected(String id) {
		currentStudentID = id;
	}

	public void runStarted(String id, String rubricName) {
		// This will force us to wait until the last run stops
		try {
			runSemaphore.acquire();
		} catch (InterruptedException e) {

		}
		currentStudentID = id;
		currentRubricName = rubricName;
		StudentConsoleAreas currentArea = getStudentConsoleAreaMap(id);
		if (rubricName != "") {
			currentOutputTextArea = currentArea.getRubricArea(rubricName).getOutputArea();
			currentErrorTextArea = currentArea.getRubricArea(rubricName).getErrorArea();
			currentInputTextArea = null;
		}
		else {
			currentOutputTextArea = currentArea.getOutputAreas().getOutputArea();
			currentErrorTextArea = currentArea.getOutputAreas().getErrorArea();
			currentInputTextArea = currentArea.getInputArea();
		}
		currentOutputTextArea.setText("");
		currentErrorTextArea.setText("");
		if (currentInputTextArea != null) {
			currentInputTextArea.setText("");
		}
		redirectStreams();
		ListenerCoordinator.fire(PreRunBlockingListener.class, id, rubricName);
	}
	private void runStopped() {		
		swapStreams();
		inPipe = new PipedInputStream();
		try {
			inWriter = new PrintWriter(new PipedOutputStream(inPipe), true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.setIn(inPipe);	
		currentOutputTextArea = null;
		currentInputTextArea = null;
		currentStudentID = null;
		currentRubricName = null;
		runSemaphore.release();

	}
	
	private void redirectStreams() {		
		inPipe = new PipedInputStream();
		try {
			if (outWriter == null) {
				outWriter = new PrintStream(new PipedOutputStream(outPipe), true);
			}
			if (errWriter == null) {
				errWriter = new PrintStream(new PipedOutputStream(errPipe), true);
			}
			inWriter = new PrintWriter(new PipedOutputStream(inPipe), true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.setIn(inPipe);
		System.setOut(outWriter);
		if (CAPTURE_STDERR) {
			System.setErr(errWriter);
		}
		else {
			System.setErr(oldErr);
		}
	}

	// Swap in our debug streams
	private void swapStreams() {
		System.out.flush();
		System.err.flush();
		//System.setOut(oldOut);
		System.setOut(debugOutStream);
		System.setIn(oldIn); 
		if (CAPTURE_STDERR) {
			System.setErr(debugErrStream);	
		}
		else {
			System.setErr(oldErr);	
		}
		
		
		
	}

	private void restoreStreams() {
		// Put things back
		System.out.flush();
		System.err.flush();
		System.setOut(oldOut);
		System.setIn(oldIn);
		System.setErr(oldErr);		
	}
	
	public void setDone() {
		consoleWorker.setStop(true);
		restoreStreams();
		consoleWorker.cancel(true);

	}
	
	private class PipeInfo{
		char character;
		PipedInputStream pipe;
		public PipeInfo(char character, PipedInputStream pipe) {
			this.character = character;
			this.pipe = pipe;
		}
	}

	private class ConsoleWorker extends SwingWorker<Void, PipeInfo> {
		public volatile boolean stop;

		public void setStop(boolean stop) {
			this.stop = stop;
		}

		@Override
		protected void done() {
			super.done();
		}

		@Override
		protected void process(List<PipeInfo> chunks) {
			boolean done = false;
			String outTemp = "";
			String errTemp = "";
			String debugTemp = "";

			for (PipeInfo info : chunks) {
				char ch = info.character;

				if (info.pipe == outPipe) {
					// Special flag sent by StudentWorkCompiler to tell us when we are done running
					// a program. I know it is a little ugly, I just can't figure out how to wait
					// until all the output finishes flowing down					
					if (ch == 0) {
						done = true;
					} else {
						outTemp += ch;
					}					
				}
				else if (info.pipe == errPipe) {
					errTemp += ch;
				}
				else {
					debugTemp += ch;
				}
			}
			if (outTemp.length() != 0) {
				if (currentOutputTextArea != null) {
					currentOutputTextArea.append(outTemp);
				}
			}
			if (errTemp.length() != 0) {
				if (currentErrorTextArea != null) {
					currentErrorTextArea.append(errTemp);
				}				
			}
			
			if (debugTemp.length() != 0) {				
				if (additionalTextArea != null) {
					additionalTextArea.append(debugTemp);
					additionalTextArea.revalidate();
				}
				DebugLogDialog.append(debugTemp);
			}
			if (done == true) {				
				if (done == true) {
					runStopped();
				}
				ListenerCoordinator.fire(SystemOutListener.class, currentStudentID, currentRubricName, outTemp, (Boolean)done);				
			}
		}
		
		private void readPipe(PipedInputStream pipe) {
			try {	
				while (pipe != null && pipe.available() != 0) {					
					char temp = (char)pipe.read();
					publish(new PipeInfo(temp, pipe));
					if (temp == '\n' && pipe != errPipe && pipe != debugErrPipe) {
						if (errPipe.available() != 0 || debugErrPipe.available() != 0) {
							break;
						}
					}
				}
			} catch (IOException e) {

			}
			
		}
		@Override
		protected Void doInBackground() {
			try {
				while (stop == false) {					
					readPipe(errPipe);
					readPipe(outPipe);
					readPipe(debugErrPipe);
					readPipe(debugOutPipe);					
				}
			} catch (Exception e) {

			}
			// Show what happened
			return null;
		}
		
	}

	
	
	private void redirectConsole() {

		consoleWorker = new ConsoleWorker();
		consoleWorker.execute();


	}


}
