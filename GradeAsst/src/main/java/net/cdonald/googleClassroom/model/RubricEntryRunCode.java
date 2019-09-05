package net.cdonald.googleClassroom.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.Visitable;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.listenerCoordinator.SetInfoLabelListener;
import net.cdonald.googleClassroom.utils.SimpleUtils;


public class RubricEntryRunCode extends  RubricAutomation {
	private String methodToCall;
	private List<FileData> sourceFiles;
	private List<String> goldenSourceClassNames;
	boolean checkSystemOut;		
	private enum ColumnNames {METHOD_TO_CALL, CLASS_NAMES_TO_REPLACE, SOURCE_FILE};


	
	public RubricEntryRunCode() {		
		sourceFiles = new ArrayList<FileData>();
		goldenSourceClassNames = new ArrayList<String>();
	}
	
	public RubricEntryRunCode(RubricEntryRunCode other) {
		methodToCall = other.methodToCall;
		sourceFiles = new ArrayList<FileData>();
		goldenSourceClassNames = new ArrayList<String>();
		for (FileData fileData : other.sourceFiles) {
			sourceFiles.add(fileData);
		}
		for (String className : other.goldenSourceClassNames) {
			goldenSourceClassNames.add(className);
		}
		checkSystemOut = other.checkSystemOut;
	}
	
	public RubricAutomation newCopy() {
		return new RubricEntryRunCode(this);
	}


	public void addSourceContents(FileData file) {
		file.setRubricCode(true);
		if (containsSource(file) == false) {
			this.sourceFiles.add(file);
		}

	}
	
	
	@Override
	public void removeFileData(FileData fileData) {

		for (int i = 0; i < sourceFiles.size(); i++) {
			if (sourceFiles.get(i).getName().equals(fileData.getName())) {
				sourceFiles.remove(i);
				break;
			}
		}
	}
	
	public boolean containsSource(FileData file) {
		for (FileData current : sourceFiles) {
			if (current.getName().equals(file.getName())) {
				return true;
			}
		}
		return false;
		
	}
	
	public List<Method> getPossibleMethods(List<FileData> goldenSource, StudentWorkCompiler compiler) {

		if (goldenSource == null || goldenSource.size() == 0) {
			return null;
		}
		
		goldenSourceClassNames.clear();		
		for (FileData fileData : goldenSource) {
			goldenSourceClassNames.add(fileData.getClassName());
		}
		
		List<FileData> rubricFiles = new ArrayList<FileData>(goldenSource);
		

		for (FileData sourceFile : sourceFiles) {
			rubricFiles.add(sourceFile);			
		}
		
		Map<String, Class<?>> compiled = null;
		try {
			compiled = compiler.compile(rubricFiles);
		} catch (Exception e) {
			return null;
		}
		List<Method> methods = new ArrayList<Method>();
		for (FileData sourceFile : sourceFiles) {
			Class<?> aClass = compiled.get(sourceFile.getClassName());
			for (Method method : aClass.getMethods()) {

				boolean validReturn = (method.getReturnType() == double.class || method.getReturnType() == Double.class);
				if (method.getParameterCount() == 0 && validReturn) {
					methods.add(method);
				}
			}
		}
		return methods;
	}
	
	protected Double runAutomation_(RubricEntry entry, String studentName, String studentId, CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData) {
		if (message == null) {
			return null;
		}
		if (message.isSuccessful()) {

			ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "Running " + this.getOwnerName() + " for " + studentName);
		

			List<FileData> studentFiles = compiler.getSourceCode(studentId);
			return runAutomation_(studentFiles, studentId, compiler, consoleData);
		}
		return null;
	}
	protected Double runAutomation_(List<FileData> studentFiles, String studentId, StudentWorkCompiler compiler, ConsoleData consoleData) {
		if (studentFiles != null && studentFiles.size() != 0)
		{
			List<FileData> rubricFiles = new ArrayList<FileData>(studentFiles);
			consoleData.runStarted(studentId, getOwnerName());				
			prepareForNextTest();

			String error = replaceClassNames(studentFiles, rubricFiles, studentId);
			if (error != null) {
				ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
				addOutput(studentId, error);					
				System.out.println("\0");					
				return null;					
			}
			Class<?> []params = {};
			Object []args = {};
			Object returnValue = null;

			try {					
				returnValue = compiler.compileAndRun(true,  rubricFiles, methodToCall, params, args);				
			}
			catch (Exception e) {
				ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
				addOutput(studentId, e.getMessage());					
				System.out.println("\0");					
				return null;
			}

			if (returnValue == null) {
				return null;
			}
			double value = 0.0;
			if (returnValue != null) {
				value = Double.parseDouble(returnValue.toString());
			}

			waitForTestFinish();


			ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
			return value;
		}
		return null;
	}
	
	String replaceClassNames(List<FileData> studentFiles, List<FileData> rubricFiles, String studentId) {
		String error = "";
		try {
			
			Map<String, MethodAndFieldList> studentSourceMap = new HashMap<String, MethodAndFieldList>();
			
			for (FileData studentFile : studentFiles) {
				CompilationUnit studentSourceCode = StaticJavaParser.parse(studentFile.getFileContents());
				MethodAndFieldList studentSourceLists = new MethodAndFieldList();
				studentSourceLists.visit(studentSourceCode, null);
				studentSourceMap.put(studentFile.getClassName(), studentSourceLists);
			}
			for (FileData testFile : sourceFiles) {
				CompilationUnit testCode = StaticJavaParser.parse(testFile.getFileContents());
				ClassNameModifier nameChange = new ClassNameModifier(goldenSourceClassNames, studentSourceMap);
				nameChange.visit(testCode, null);
				if (nameChange.isError()) {
					error += nameChange.getErrorString();					
				}
				else {
					FileData temp = new FileData(testFile.getName(), testCode.toString(), studentId, null);
					rubricFiles.add(temp);
				}			
			}			
		}
		// Student source might not be parsable, in which case that is fine
		catch(Exception e) {
			error = "Could not parse student file";
		}
		if (error.length() == 0) {
			return null;
		}
		return error;
	}
	
	private static class ClassNameModifier extends ModifierVisitor<Void>  {
		List<String> goldenSourceScopes;
		Map<String, MethodAndFieldList> studentSources;
		boolean error;
		String errorString;
		public ClassNameModifier(List<String> goldenSources, Map<String, MethodAndFieldList> studentSources) {
			super();
			this.goldenSourceScopes = goldenSources;
			this.studentSources = studentSources;
			error = false;
			errorString = "";
		}
		/**
		 * @return the error
		 */
		public boolean isError() {
			return error;
		}
		/**
		 * @return the errorString
		 */
		public String getErrorString() {
			return errorString;
		}
		
		private void appendError(String message) {
			if (errorString.indexOf(message) == -1) {
				errorString += message;
			}
		}
		
		private boolean isGoldenScope(String scopeString) {
			for (String scope : goldenSourceScopes) {
				if (scopeString.equals(scope)) {					
					return true;
				}
			}
			return false;
		}
		

		@Override
		public Visitable visit(FieldAccessExpr n, Void arg) {
			// TODO Auto-generated method stub
			super.visit(n, arg);
			Expression scope = n.getScope();
			String fieldName = n.getNameAsString();
			String scopeString = scope.toString();
			if (isGoldenScope(scopeString)) {
				for (String studentKey : studentSources.keySet()) {
					List<FieldDeclaration> fields = studentSources.get(studentKey).getFields();
					for (FieldDeclaration field : fields) {
						for (VariableDeclarator vars : field.getVariables()) {						

							if (vars.getNameAsString().equals(fieldName)) {								
								NameExpr newScope = new NameExpr(studentKey);
								FieldAccessExpr newExpr = new FieldAccessExpr(newScope, fieldName);
								return newExpr;
							}
						}
					}
				}
				error = true;
				appendError("Could not find required field: " + fieldName + " in student source\n");
			}
			return n;
		}
		@Override
		public Visitable visit(MethodCallExpr n, Void arg) {
			// TODO Auto-generated method stub
			super.visit(n, arg);
			Optional<Expression> scope = n.getScope();
			String methodName = n.getNameAsString();
			if (scope.isPresent()) {
				String scopeString = scope.get().toString();
				if (isGoldenScope(scopeString)) {
					for (String studentKey : studentSources.keySet()) {
						List<MethodDeclaration> methods = studentSources.get(studentKey).getMethods();
						for (MethodDeclaration methodDec : methods) {
							if (methodDec.getNameAsString().equals(methodName)) {
								String newCall = studentKey + "." + n.getNameAsString();					
								MethodCallExpr newMethodCall = new MethodCallExpr(newCall);
								newMethodCall.setArguments(n.getArguments());
								return newMethodCall;
								
							}
						}
					}
					error = true;
					appendError("Could not find required method: " + methodName + " in student source\n");
				}
			}
			return n;
		}

	}
	
	private static class MethodAndFieldList extends VoidVisitorAdapter<Void> {
		List<FieldDeclaration> fields;
		List<MethodDeclaration> methods;
		public MethodAndFieldList() {
			fields = new ArrayList<FieldDeclaration>();
			methods = new ArrayList<MethodDeclaration>();
		}
		
		
		/**
		 * @return the fields
		 */
		public List<FieldDeclaration> getFields() {
			return fields;
		}
		/**
		 * @return the methods
		 */
		public List<MethodDeclaration> getMethods() {
			return methods;
		}


		@Override
		public void visit(ClassExpr n, Void arg) {			
			super.visit(n, arg);
			System.out.println(n.toString());
		}


		@Override
		public void visit(FieldDeclaration n, Void arg) {
			// TODO Auto-generated method stub
			super.visit(n, arg);
			fields.add(n);
		}

		@Override
		public void visit(MethodDeclaration n, Void arg) {
			// TODO Auto-generated method stub
			super.visit(n, arg);
			methods.add(n);
		}
		
	}
	
	public String getMethodToCall() {
		return methodToCall;
	}


	public void setMethodToCall(String methodToCall) {
		this.methodToCall = methodToCall;
	}




	public List<FileData> getSourceFiles() {
		return sourceFiles;
	}



	
	@Override
	protected void saveAutomationColumns(String entryName, List<List<Object>> columnData, Map<String, List<Object>> fileData) {
		List<Object> labels = new ArrayList<Object>();
		List<Object> content = new ArrayList<Object>();
		labels.add(entryName);
		content.add(entryName);
		labels.add(ColumnNames.CLASS_NAMES_TO_REPLACE.toString());
		String classes = "";
		for (int i = 0; i < goldenSourceClassNames.size() - 1; i++) {
			classes += goldenSourceClassNames.get(i) + ",";
		}
		if (goldenSourceClassNames.size() != 0) {
			classes += goldenSourceClassNames.get(goldenSourceClassNames.size() - 1);
		}
		content.add(classes);
		labels.add(ColumnNames.METHOD_TO_CALL.toString());
		content.add(methodToCall);
		columnData.add(labels);
		columnData.add(content);
		String sourceFileNames = "";
		
		for (int i = 0; i < sourceFiles.size() - 1; i++) {
			sourceFileNames += sourceFiles.get(i).getName() + ",";
		}
		if (sourceFiles.size() != 0) {
			sourceFileNames += sourceFiles.get(sourceFiles.size() - 1).getName();
		}
		labels.add(ColumnNames.SOURCE_FILE.toString());
		content.add(sourceFileNames);
		for (FileData file : sourceFiles) {
			if (fileData.containsKey(file.getName()) == false) {
				List<Object> fileLineList = file.fillSaveData();
				fileData.put(file.getName(), fileLineList);
			}
		}
	}

	
	private void showErrorMessage(String entryName) {
		Rubric.showLoadError("The rubric component \"" + entryName + "\" is missing run data.");
	}


	@Override
	protected void loadAutomationColumns(String entryName, Map<String, List<List<Object>>> columnData, Map<String, FileData> fileDataMap) {
		List<List<Object> > columns = columnData.get(entryName.toUpperCase());
		if (columns == null || columns.size() != 2) {
			showErrorMessage(entryName);
			return;
		}
		else {
			List<String> files = null;
			goldenSourceClassNames.clear();

			List<Object> labelRow = columns.get(0);
			methodToCall = null;
			for (int row = 0; row < labelRow.size(); row++) {
				String label = (String)labelRow.get(row);
				if (label != null) { 
					if (label.equalsIgnoreCase(ColumnNames.METHOD_TO_CALL.toString())) {
						methodToCall = (String)columns.get(1).get(row);
					}
					else if (label.equalsIgnoreCase(ColumnNames.SOURCE_FILE.toString())) {
						files = SimpleUtils.breakUpCommaList(columns.get(1).get(row));
					}
					else if (label.equalsIgnoreCase(ColumnNames.CLASS_NAMES_TO_REPLACE.toString())) {
						goldenSourceClassNames = SimpleUtils.breakUpCommaList(columns.get(1).get(row));
					}
				}
			}
			if (files == null || files.size() == 0 ||  goldenSourceClassNames.size() == 0 || methodToCall == null) {
				showErrorMessage(entryName);							
			}
			if (files == null) {
				return;
			}
			sourceFiles = new ArrayList<FileData>();
			for (Object fileO : files) {
				if (fileO instanceof String) {
					String file = (String)fileO;
					// We want them all sharing the same source so that when we edit the
					// source in the rubric, all of them see the edit
					if (fileDataMap.containsKey(file)) {
						sourceFiles.add(fileDataMap.get(file));
					}
					else {
						FileData fileData = FileData.newFromSheet(file, columnData.get(file.toUpperCase()));
						if (fileData == null) {
							showErrorMessage(entryName);
						}
						else {
							sourceFiles.add(fileData);
							fileDataMap.put(file, fileData);
						}
					}
				}
			}
		}
	}

}
