package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SetInfoLabelListener;
import net.cdonald.googleClassroom.utils.SimpleUtils;

public class RubricEntryMethodContains extends RubricAutomation {
	Map<String, Set<String> > stringsToFind;

	
	
	public RubricEntryMethodContains() {
		stringsToFind = new HashMap<String, Set<String> >();

	}
	
	public RubricEntryMethodContains(RubricEntryMethodContains other) {
		stringsToFind = new HashMap<String, Set<String> >();
		setStringsToFind(other.stringsToFind);
	}
	


	public void setStringsToFind(Map<String, Set<String> > otherStringsToFind) {
		this.stringsToFind.clear();
		for (String method : otherStringsToFind.keySet()) {
			
			Set<String> otherListToFind = otherStringsToFind.get(method);
			Set<String> listToFind = new HashSet<String>();
			for (String findIt : otherListToFind) {
				listToFind.add(findIt);
			}
			this.stringsToFind.put(method, listToFind);
		}
	}
	
	public void changeStringsToFind(String methodName, String stringToCall, boolean add) {
		if (stringsToFind.containsKey(methodName) == false) {
			stringsToFind.put(methodName, new HashSet<String>());
		}
		Set<String> calls = stringsToFind.get(methodName);
		if (add == true) {
			calls.add(stringToCall);
		}
		else {
			calls.remove(stringToCall);
		}
		if (calls.size() == 0) {
			stringsToFind.remove(methodName);
		}
	}
	
	public Boolean requiresCall(String methodName, String call) {
		Set<String> calls = stringsToFind.get(methodName);
		if (calls == null) {
			return Boolean.FALSE;
		}
		return (Boolean)calls.contains(call);		
	}
	

	/**
	 * @return the stringsToFind
	 */
	public Map<String, Set<String> > getStringsToFind() {
		return stringsToFind;
	}

	@Override
	public RubricAutomation newCopy() {
		return new RubricEntryMethodContains(this);
	}

	@Override
	public void removeFileData(FileData fileData) {
	}

	@Override
	protected Double runAutomation_(RubricEntry owner, String studentName, String studentId, CompilerMessage message, StudentWorkCompiler compiler,
			ConsoleData consoleData) {

		if (message == null) {
			return null;
		}
		ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "Running: " + owner.getName());
				
		consoleData.runStarted(studentId, getOwnerName());

		Double value = runAutomationWrapped(studentName, studentId, compiler, consoleData);
		System.out.println("\0");
		waitForTestFinish();
		ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
		return value;
	}
	
	private Double runAutomationWrapped(String studentName, String studentId, StudentWorkCompiler compiler,
			ConsoleData consoleData) {
	
		if (stringsToFind == null || stringsToFind.size() == 0) {
			System.err.println(getOwnerName() + " is not fully defined ");
			return null;
		}
		
		

		List<FileData> studentFiles = compiler.getSourceCode(studentId);
		Map<String, Set<String> > methodMap = createCallMap(studentFiles);
		int numFound = 0;
		int numExpected = 0;
		boolean allMethodsFound = true;
		
		for (String methodName : stringsToFind.keySet()) {
			if (methodMap.containsKey(methodName)) {
				Set<String> findCalls = stringsToFind.get(methodName);
				Set<String> actualCalls = methodMap.get(methodName);
				for (String call : findCalls) {
					numExpected++;
					if (actualCalls.contains(call)) {
						numFound++;
						addOutput(studentId, "Pass: " + methodName + " calls: " + call);
					}
					else {
						addOutput(studentId, "Fail: " + methodName + " does not call: " + call);
					}
				}				
			}
			else {
				addOutput(studentId, "Could not find method: " + methodName);
				allMethodsFound = false;
			}
		}
		if (allMethodsFound == false) {
			addOutput(studentId, "\nNot all methods found, cannot automatically assign a grade.");
			addOutput(studentId, "You can modify the student source to correct a mis-spelled method.");
			addOutput(studentId, "If the method is missing, you can also add the missing method, make it an empty method, and rerun to get the rest of the points calculated.");
			addOutput(studentId, "Once you change the source, Right-click and recompile, then re-run the rubric.");
			addOutput(studentId, "All code changes are local, and will not be saved back to google classroom\n");
			addOutput(studentId, "Here is the complete list of what this rubric looked for:");
			addOutput(studentId, createCompleteCallList());
			addOutput(studentId, "\nHere is a complete list of all the methods in the student file and what they call:");
			addOutput(studentId, createCompleteCallList(methodMap));
			return null;			
		}
		return (Double)((double)numFound/(double)numExpected);
	}
	
	public String createCompleteCallList() {
		return createCompleteCallList(stringsToFind);
	}
	private  String createCompleteCallList(Map<String, Set<String>> methodMap) {
		String output = "";
		for (String methodName : methodMap.keySet()) {
			output += methodName + " calls: ";
			output += createCommaSeparatedList(methodMap.get(methodName));
			output += "\n";
		}
		return output;
	}
	
	private String createCommaSeparatedList(Set<String> calls) {
		String output = "";	
		if (calls != null) {

			int printCount = 1;
			for (String call : calls) {
				output += " " + call;
				if (printCount < calls.size()) {
					output += ", ";
				}
				printCount++;
			}
		}
		return output;
	}
	
	public static Map<String, Set<String> > createCallMap(List<FileData> fileData) {
		Map<String, Set<String> > methodMap = new HashMap<String, Set<String> >();
		
		MethodVisitor methodVisitor = new MethodVisitor();
		for (FileData studentFile : fileData) {
			methodVisitor.visit(studentFile.getCompilationUnit(), methodMap);
		}
		return methodMap;

	}
	private static class CheckForUseVisitor extends VoidVisitorAdapter<Set<String>> {
		
		@Override
		public void visit(FieldAccessExpr n, Set<String> foundSet) {
			// TODO Auto-generated method stub
			super.visit(n, foundSet);
			Expression scope = n.getScope();
			foundSet.add(scope.toString() + "." + n.getNameAsString());
		}

		@Override
		public void visit(MethodCallExpr n, Set<String> foundSet) {
			// TODO Auto-generated method stub
			super.visit(n, foundSet);
			
			Optional<Expression> scope = n.getScope();
			String name = "";
			if (scope.isPresent()) {
				name = scope.get().toString() + ".";
			}
			name += n.getNameAsString();
			foundSet.add(name);
		}

	}
	private static class MethodVisitor extends VoidVisitorAdapter<Map<String, Set<String> >> {
		@Override
		public void visit(MethodDeclaration n, Map<String, Set<String> > methodContents) {
			// TODO Auto-generated method stub
			super.visit(n, methodContents);
			if (n.getBody().isPresent() && n.getBody().get().isBlockStmt()) {
				String methodName = n.getDeclarationAsString(false, false, false);
				CheckForUseVisitor mup = new CheckForUseVisitor();
				Set<String> callsList = new HashSet<String>();
				n.accept(mup, callsList);
				methodContents.put(methodName, callsList);
							
			}
		}

	}

	@Override
	protected void saveAutomationColumns(String entryName, List<List<Object>> columnData,
			Map<String, List<Object>> fileData) {
		List<Object> methodNames = new ArrayList<Object>();
		List<Object> callsToFind = new ArrayList<Object>();
		methodNames.add(entryName);
		callsToFind.add(entryName);
		for (String methodName : stringsToFind.keySet()) {
			methodNames.add(methodName);
			callsToFind.add(createCommaSeparatedList(stringsToFind.get(methodName)));
		}
		columnData.add(methodNames);
		columnData.add(callsToFind);

	}

	@Override
	protected void loadAutomationColumns(String entryName, Map<String, List<List<Object>>> columnData,
			Map<String, FileData> fileDataMap) {
		List<List<Object> > columns = columnData.get(entryName.toUpperCase());
		if (columns == null || columns.size() != 2) {
			Rubric.showLoadError("Missing data for entry: \"" + entryName + "\"");
			return;
		}
		else {			
			stringsToFind.clear();
			List<Object> methodColumn = columns.get(0);
			List<Object> callsColumn = columns.get(1);
			for (int  i = 1; i < methodColumn.size(); i++) {
				String methodName = methodColumn.get(i).toString();
				if (methodName != null && methodName.length() > 0) {
					if (callsColumn.size() > i) {
						Set<String> callsToFind = new HashSet<String>();
						List<String> callList = SimpleUtils.breakUpCommaList(callsColumn.get(i));
						for (String call : callList) {
							callsToFind.add(call);
						}
						stringsToFind.put(methodName, callsToFind);
					}
					else {
						Rubric.showLoadError("Missing list of calls to find for " + methodName);
					}
				}
			}
		}

	}

}
