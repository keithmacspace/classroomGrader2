package net.cdonald.googleClassroom.inMemoryJavaCompiler;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.mdkt.compiler.CompilationException;
import org.mdkt.compiler.InMemoryJavaCompiler;

import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.DoStmt;
import com.github.javaparser.ast.stmt.ForEachStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.Statement;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;

import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.listenerCoordinator.AddProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.RemoveProgressBarListener;
import net.cdonald.googleClassroom.listenerCoordinator.StopRunListener;
import net.cdonald.googleClassroom.model.FileData;

public class StudentWorkCompiler {

	private Map<String, StudentBuildInfo> studentBuildInfoMap;
	private SwingWorker<Void, CompilerMessage> compilerWorker;
	private CompileListener listener;
	private RunCore runCore;
	private static Semaphore stopSemaphore = new Semaphore(1);
	
	
	

	public StudentWorkCompiler(CompileListener listener) {
		this.listener = listener;
		studentBuildInfoMap = new HashMap<String, StudentBuildInfo>();
		ListenerCoordinator.addListener(StopRunListener.class, new StopRunListener() {
			@Override
			public void fired() {
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {						
						if (runCore != null) {							
							runCore.setStop();							
						}
					}
				});
			}
		});
	}
	
	
	public static boolean stopInstrumentation() {		
		if (stopSemaphore.availablePermits() == 0) {
			DebugLogDialog.appendln("throwing stop");
			throw new InfiniteLoopException();
		}
		return false;
	}

	public void clearData() {
		studentBuildInfoMap.clear();
	}
	


	public void addFile(FileData fileData) {
		String key = fileData.getId();		
		if (studentBuildInfoMap.containsKey(key) == false) {
			studentBuildInfoMap.put(key, new StudentBuildInfo());
		}
		fileData.modifySourceFile(new LoopInstrumenter());
		studentBuildInfoMap.get(key).addFileData(fileData);
		if (listener != null) {
			listener.dataUpdated();
		}
	}
	
	public void removeFiles(Set<String> ids) {
		for (String id : ids) {
			studentBuildInfoMap.remove(id);
		}
	}

	public CompileListener getListener() {
		return listener;
	}

	public void setListener(CompileListener listener) {
		this.listener = listener;
	}
	
	public Object runSpecificMethod(boolean expectingReturn, String methodName, CompilerMessage message, Class<?> []params, Object[] args) {
		String id = message.getStudentId();
		if (studentBuildInfoMap.containsKey(id)) {
			StudentBuildInfo studentBuildInfo = studentBuildInfoMap.get(id);
			if (studentBuildInfo.getStudentCompilerMap() != null) {				
				Map<String, Class<?>> compiled = studentBuildInfo.getStudentCompilerMap();
				List<FileData> files = studentBuildInfo.getStudentFileData();
				return runSpecificMethod(expectingReturn, methodName, files, compiled, params, args);
			}
		}
		throw new IllegalArgumentException();
	}

	private Object runSpecificMethod(boolean expectingReturn, String methodName, List<FileData> files, Map<String, Class<?>> compiled, Class<?>[] params, Object[] args) {
		for (FileData fileData : files) {
			Class<?> aClass = compiled.get(fileData.getClassName());
			Method method = getMethod(aClass, methodName, params);
			if (method != null) {
				return runCore(expectingReturn, method, args);
			}		
		}
		return null;		
	}
	
	public Map<String, Class<?>> compile(List<FileData> fileDataList) throws Exception {
		InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();
		Map<String, Class<?>> compiled = null;

		for (FileData fileData : fileDataList) {				
			compiler.addSource(fileData.getClassName(), fileData.getFileContents());
		}
		compiled = compiler.compileAll();
		return compiled;
		
	}
	
	public Object compileAndRun(boolean expectingReturn, List<FileData> fileDataList, String methodName, Class<?> []params, Object[] args) throws Exception {
		Map<String, Class<?>> compiled = compile(fileDataList);
		return runSpecificMethod(expectingReturn, methodName, fileDataList, compiled, params, args);
	}
	
	public void compileAll() {
		compilerWorker = new SwingWorker<Void, CompilerMessage>() {

			@Override
			protected void process(List<CompilerMessage> chunks) {
				for (CompilerMessage message : chunks) {					
					if (listener != null) {
						listener.dataUpdated();
					}
				}
			}

			@Override
			protected Void doInBackground() {
				final String PROGRESS_BAR_NAME = "Compiling";
				ListenerCoordinator.fire(AddProgressBarListener.class, PROGRESS_BAR_NAME);
				Set<String> keys = studentBuildInfoMap.keySet();
				for (String studentID : keys) {
					CompilerMessage compilerMessage = compile(studentID);
					publish(compilerMessage);
					
				}
				ListenerCoordinator.fire(RemoveProgressBarListener.class, PROGRESS_BAR_NAME);
				return null;
			}

			@Override
			protected void done() {
				if (listener != null) {
					listener.compileDone();
				}
			}

		};
		compilerWorker.execute();
	}
	
	public CompilerMessage compile(String studentID) {
		CompilerMessage message = null;
		StudentBuildInfo studentBuildInfo = studentBuildInfoMap.get(studentID);
		if (studentBuildInfo != null) {
	 		studentBuildInfo.setStudentCompilerMap(null);
			List<FileData> studentFiles = studentBuildInfo.getStudentFileData();
			InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();
			try {
				for (FileData file : studentFiles) {
					compiler.addSource(file.getClassName(), file.getFileContents());
				}
				Map<String, Class<?>> compiled = compiler.compileAll();
				studentBuildInfo.setStudentCompilerMap(compiled);			
				message = new CompilerMessage(studentID, true);
			} catch (CompilationException e) {
				message = new CompilerMessage(studentID, false, e.getLocalizedMessage());
			} catch (Exception e2) {
				message = new CompilerMessage(studentID, false, e2.getMessage());
			} 
			studentBuildInfo.setCompilerMessage(message);			
		}
		return message;
	}
	

	public List<FileData> getSourceCode(String id) {
		if (studentBuildInfoMap.containsKey(id) == false) {
			return null;
		}
		return studentBuildInfoMap.get(id).getStudentFileData();
	}

	public void run(String id) {

		if (studentBuildInfoMap.containsKey(id)) {
			StudentBuildInfo studentBuildInfo = studentBuildInfoMap.get(id);
			if (studentBuildInfo.getStudentCompilerMap() != null) {				
				Map<String, Class<?>> compiled = studentBuildInfo.getStudentCompilerMap();
				List<FileData> files = studentBuildInfo.getStudentFileData();
				for (FileData fileData : files) {
					Class<?> aClass = compiled.get(fileData.getClassName());
					Method method = getMethod(aClass);
					if (method != null) {
						runCore(method);
					}
				}
			}
		}
	}


	public String getCompleteMethodName(String studentId, String methodName) {
		StudentBuildInfo buildInfo = studentBuildInfoMap.get(studentId);
		if (buildInfo != null) {
			return buildInfo.getCompleteMethodName(methodName);
		}
		return null;
	}
	
	Method getMethod(Class<?> aClass) {
		Class<?> []params = { String[].class };
		return getMethod(aClass, "main", params);
	}
	
	Method getMethod(Class<?> aClass, String methodName, Class<?> []params) {

		Method method = null;
		try {
			method = aClass.getDeclaredMethod(methodName, params);
		} catch (NoSuchMethodException | SecurityException e1) {
			method = null;
		}
		return method;
	}
	private class RunCore extends Thread {
		private Semaphore runSemaphore = new Semaphore(0);
		private Object result;
		private Method method;
		private Object [] args;
		private StopExitSecurityManager preventExit;		
		public RunCore(Method method, Object[] args) {
			result = null;
			this.method = method;
			this.args = args;
			if (stopSemaphore.availablePermits() == 0) {
				stopSemaphore.release();
			}
		
			preventExit = new StopExitSecurityManager();
		}
			
		public boolean isStop() {
			return (stopSemaphore.availablePermits() == 0);
		}

		public void setStop() {
			try {
				if (isStop() == false) {
					stopSemaphore.acquire();
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			this.interrupt();
		}
		
		public void waitForFinish() {
			try {
				runSemaphore.acquire();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		public Object getResult() {
			return result;
		}
		@Override
		public void run() {
				try {
					result = null;
					preventExit.install();
					result = method.invoke(null, args);					
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				catch (Exception e) {
					DebugLogDialog.appendln("Caught Exception " + e.toString());
					preventExit.uninstall();
					runSemaphore.release();
					throw e;
				}
				preventExit.uninstall();
				runSemaphore.release();

		}
		
	}
	private Object runCore(boolean expectReturn, Method method, Object[] args) {
		runCore = new RunCore(method, args);
		try {
			runCore.start();
			runCore.waitForFinish();
			if (runCore.isStop()) {
				System.out.println("Terminated\n");
				System.out.println("\0");				
			}
			else {
				System.out.println("Ran Successfully");
				// This is how I send the message that execution has completed
				// Hopefully none of the students will print a zero.
				// I hate doing this, but I needed some sort of semaphore
				System.out.println("\0");
			}
		}
		catch (InfiniteLoopException ie) {
			System.out.println("Terminated\n");
			System.out.println("\0");							
		}
		catch (ExitTrappedException et) {
			System.out.println("exit(0) called");
			System.out.println("\0");
		}
		catch (Exception e) {
			System.out.println("Exception Caught\n" + e.getClass() + e.getMessage() + "\n");
			System.out.println("\0");
		}
		Object result = runCore.getResult();
		runCore = null;
		return result;
	}
	
	
		

	private void runCore(Method method) {
		Object[] args = {null};
		runCore(false, method, args);
	}
	
	public boolean isRunnable(String id) {
		if (studentBuildInfoMap.containsKey(id) == true) {
			StudentBuildInfo studentBuildInfo = studentBuildInfoMap.get(id);
			if (studentBuildInfo.getStudentCompilerMap() != null) {
				return true;
			}			
		}
		return false;		
	}
	
	public CompilerMessage getCompilerMessage(String id) {
		if (studentBuildInfoMap.containsKey(id) == true) {
			return studentBuildInfoMap.get(id).getCompilerMessage();
		}
		return null;
	}


	public void recompile(String studentID, String fileName, String fileText) {
		if (studentBuildInfoMap.containsKey(studentID)) {
			studentBuildInfoMap.get(studentID).changeFileData(fileName, fileText);
			compile(studentID); 			
		}		
	}


	public void removeSource(String studentID, String fileName) {
		if (studentBuildInfoMap.containsKey(studentID)) {
			studentBuildInfoMap.get(studentID).removeSource(fileName);
		}		
	}


	public void clearStudentFiles(String studentId) {
		if (studentBuildInfoMap.containsKey(studentId)) {
			studentBuildInfoMap.clear();
		}	
	}
	
	public String getInstrumentationCall() {
		return this.getClass().getCanonicalName() + ".stopInstrumentation";
	}

	
	private class LoopInstrumenter extends ModifierVisitor<Void> {

		private MethodCallExpr createLoopCheckExpression() {
			MethodCallExpr infiniteCheck = new MethodCallExpr(getInstrumentationCall());
			infiniteCheck.setLineComment("Auto added to detect infinite loops, remove if it creates compile errors");
			return infiniteCheck;
		}

		
		private void modifyLoop(Statement body) {			
			if (body.isBlockStmt()) 
			{
				BlockStmt bl = body.asBlockStmt();
				bl.addStatement(0, createLoopCheckExpression());
			}			
		}
		
		@Override
		public Visitable visit(DoStmt n, Void arg) {			
			super.visit(n, arg);
			modifyLoop(n.getBody());
			return n;
		}

		@Override
		public Visitable visit(ForEachStmt n, Void arg) {
			super.visit(n, arg);
			modifyLoop(n.getBody());
			return n;
		}

		@Override
		public Visitable visit(ForStmt n, Void arg) {
			super.visit(n, arg);
			modifyLoop(n.getBody());
			return n;
		}
		

		@Override
		public Visitable visit(WhileStmt n, Void arg) {
			super.visit(n, arg);
			modifyLoop(n.getBody());
			return n;
		}		
	}
}
