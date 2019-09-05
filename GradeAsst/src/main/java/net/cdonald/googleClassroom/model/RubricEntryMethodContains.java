package net.cdonald.googleClassroom.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SetInfoLabelListener;
import net.cdonald.googleClassroom.utils.SimpleUtils;

public class RubricEntryMethodContains extends RubricAutomation {
	String fullMethodSignature;
	String methodNameToSearch;
	String methodReturnTypeToSearch;
	List<String> stringsToFind;
	private enum ColumnNames {FULL_METHOD_SIGNATURE, METHOD_NAME_TO_SEARCH, METHOD_RETURN_TYPE_TO_SEARCH, STRINGS_TO_FIND};
	
	public RubricEntryMethodContains() {
		fullMethodSignature = null;
		methodNameToSearch = null;
		methodReturnTypeToSearch = null;
		stringsToFind = new ArrayList<String>();
	}
	
	public RubricEntryMethodContains(RubricEntryMethodContains other) {
		fullMethodSignature = other.fullMethodSignature;
		methodNameToSearch = other.methodNameToSearch;
		methodReturnTypeToSearch = other.methodReturnTypeToSearch;
		stringsToFind = new ArrayList<String>();
		setStringsToFind(other.stringsToFind);
	}
	

	public String getFullMethodSignature() {
		return fullMethodSignature;
	}

	public void setMethodToSearch(Method method) {
		fullMethodSignature = method.toString();
		methodNameToSearch = method.getName();
		methodReturnTypeToSearch = method.getReturnType().toString();
	}
	
	
	/**
	 * @return the methodNameToSearch
	 */
	public String getMethodNameToSearch() {
		return methodNameToSearch;
	}

	/**
	 * @return the methodReturnTypeToSearch
	 */
	public String getMethodReturnTypeToSearch() {
		return methodReturnTypeToSearch;
	}

	public void setStringsToFind(List<String> stringsToFind) {
		this.stringsToFind.clear();
		for (String str : stringsToFind) {
			this.stringsToFind.add(str);
		}
	}
	

	/**
	 * @return the stringsToFind
	 */
	public List<String> getStringsToFind() {
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
		String messageStr = "Checking " + studentName + "'s source: " + methodNameToSearch + " for ";
		for (String str : stringsToFind) {
			messageStr += str;
		}
		ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, messageStr);
				
		consoleData.runStarted(studentId, getOwnerName());

		Double value = runAutomationWrapped(studentName, studentId, compiler, consoleData);
		System.out.println("\0");
		waitForTestFinish();
		ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
		return value;
	}
	
	private Double runAutomationWrapped(String studentName, String studentId, StudentWorkCompiler compiler,
			ConsoleData consoleData) {
	
		if (stringsToFind.size() == 0 || methodNameToSearch == null) {
			System.err.println(getOwnerName() + " is not fully defined ");
			return null;
		}


		Set<String> foundSet = new HashSet<String>();
		List<FileData> studentFiles = compiler.getSourceCode(studentId);
		MethodVisitor methodVisitor = new MethodVisitor();
		for (FileData studentFile : studentFiles) {
			methodVisitor.visit(studentFile.getCompilationUnit(), foundSet);
		}
		if (methodVisitor.getMethodContents() == null) {
			System.err.println("Could not find method: " + methodNameToSearch);
			return null;
		}
		else {

			for (String strToFind : stringsToFind) {
				if (foundSet.contains(strToFind) == false) {
					addOutput(studentId, "Could not find string: " + strToFind);
				}
				else {
					addOutput(studentId, "Found: " + strToFind);
				}
			}
			addOutput(studentId, "\nMethod contents searched:");
			addOutput(studentId, methodVisitor.getMethodContents());
		}		
		return (Double)((double)foundSet.size()/(double)stringsToFind.size());
	}
	private class CheckForUseVisitor extends VoidVisitorAdapter<Set<String>> {
		//stringsToFind = new ArrayList<String>();
		public void searchExpression(String scope, String name, Set<String> foundSet) {			
			for (String str : stringsToFind) {
				int scopeIndex = str.lastIndexOf('.');
				String findScope = null;
				String searchName = str;
				
				if (scopeIndex != -1) {
					findScope = str.substring(0, scopeIndex);
					searchName = str.substring(scopeIndex + 1);
				}				
				if (searchName.equals(name)) {
					if ((findScope == null) || (scope.indexOf(findScope) != -1)) {
						foundSet.add(str);
					}
					
				}
			}
		}
		
		@Override
		public void visit(FieldAccessExpr n, Set<String> foundSet) {
			// TODO Auto-generated method stub
			super.visit(n, foundSet);
			searchExpression(n.getScope().toString(), n.getNameAsString(), foundSet);
		}

		@Override
		public void visit(MethodCallExpr n, Set<String> foundSet) {
			// TODO Auto-generated method stub
			super.visit(n, foundSet);
			String scope = "";
			if (n.getScope().isPresent()) {
				scope = n.getScope().get().toString();
			}
			searchExpression(scope, n.getNameAsString(), foundSet);
		}

	}
	private class MethodVisitor extends VoidVisitorAdapter<Set<String>> {
		private String methodContents;
		@Override
		public void visit(MethodDeclaration n, Set<String> foundSet) {
			// TODO Auto-generated method stub
			super.visit(n, foundSet);
			if (n.getBody().isPresent() && n.getBody().get().isBlockStmt()) {
				if (n.getNameAsString().equals(methodNameToSearch) && n.getTypeAsString().equals(methodReturnTypeToSearch)) {
					methodContents = n.getDeclarationAsString() + n.getBody().get().toString();
					CheckForUseVisitor mup = new CheckForUseVisitor();
					n.accept(mup, foundSet);
				}			
			}
		}
		
		public String getMethodContents() {
			return methodContents;
		}
		
	}

	@Override
	protected void saveAutomationColumns(String entryName, List<List<Object>> columnData,
			Map<String, List<Object>> fileData) {
		List<Object> labels = new ArrayList<Object>();
		List<Object> content = new ArrayList<Object>();
		labels.add(entryName);
		content.add(entryName);
		labels.add(ColumnNames.FULL_METHOD_SIGNATURE.toString());
		labels.add(ColumnNames.METHOD_NAME_TO_SEARCH.toString());
		labels.add(ColumnNames.METHOD_RETURN_TYPE_TO_SEARCH.toString());
		labels.add(ColumnNames.STRINGS_TO_FIND.toString());
		content.add(fullMethodSignature);
		content.add(methodNameToSearch);
		content.add(methodReturnTypeToSearch);

		String searchStrings = "";
		for (int i = 0; i < stringsToFind.size() - 1; i++) {
			searchStrings += stringsToFind.get(i) + ",";
		}
		if (stringsToFind.size() != 0) {
			searchStrings += stringsToFind.get(stringsToFind.size() - 1);
		}
		content.add(searchStrings);
		columnData.add(labels);
		columnData.add(content);

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
			List<String> files = null;
			stringsToFind.clear();

			List<Object> labelRow = columns.get(0);
			methodNameToSearch = null;
			methodReturnTypeToSearch = null;
			fullMethodSignature = null;
			for (int row = 0; row < labelRow.size(); row++) {
				String label = (String)labelRow.get(row);
				if (label != null) { 
					if (label.equalsIgnoreCase(ColumnNames.METHOD_NAME_TO_SEARCH.toString())) {
						methodNameToSearch = (String)columns.get(1).get(row);
					}
					else if (label.equalsIgnoreCase(ColumnNames.METHOD_RETURN_TYPE_TO_SEARCH.toString())) {
						methodReturnTypeToSearch = (String)columns.get(1).get(row);
					}
					else if (label.equalsIgnoreCase(ColumnNames.STRINGS_TO_FIND.toString())) {
						stringsToFind = SimpleUtils.breakUpCommaList(columns.get(1).get(row));
					}
					else if (label.equalsIgnoreCase(ColumnNames.FULL_METHOD_SIGNATURE.toString())) {
						fullMethodSignature = (String)columns.get(1).get(row);
					}
				}
			}
			if (methodNameToSearch == null || fullMethodSignature == null || methodReturnTypeToSearch == null || stringsToFind == null || stringsToFind.size() == 0) {
				Rubric.showLoadError("Missing data for entry: \"" + entryName + "\"");
			}
		}


	}

}
