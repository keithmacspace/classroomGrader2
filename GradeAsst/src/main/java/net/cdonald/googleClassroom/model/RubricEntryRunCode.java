package net.cdonald.googleClassroom.model;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.swing.JOptionPane;

import org.mdkt.compiler.CompilationException;

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
	private Set<String> testCodeSourceToUse;	
	boolean updateOnlyOnPass;		
	private enum ColumnNames {METHOD_TO_CALL, UPDATE_ONLY_ON_PASS, CLASS_NAMES_TO_REPLACE, SOURCE_FILE};
	


	
	public RubricEntryRunCode() {						
		testCodeSourceToUse = new HashSet<String>();
	}
	
	public void addTestCodeSourceName(String name) {
		testCodeSourceToUse.add(name);
	}
	
	public void removeTestCodeSource(String name) {
		testCodeSourceToUse.remove(name);
	}
	
	public boolean containsSource(String name) {
		return testCodeSourceToUse.contains(name);
	}
	
	public Set<String> getTestCodeSourceToUse() {
		return testCodeSourceToUse;
	}

	public boolean isUpdateOnlyOnPass() {
		return updateOnlyOnPass;
	}

	public void setUpdateOnlyOnPass(boolean updateOnlyOnPass) {
		this.updateOnlyOnPass = updateOnlyOnPass;
	}
	 

	public RubricEntryRunCode(RubricEntryRunCode other) {
		methodToCall = other.methodToCall;				
		testCodeSourceToUse = new HashSet<String>();
		for (String name : other.testCodeSourceToUse) {
			testCodeSourceToUse.add(name);
		}

		updateOnlyOnPass = other.updateOnlyOnPass;
	}
	
	public RubricAutomation newCopy() {
		return new RubricEntryRunCode(this);
	}


	
	
	public static Map<String, List<Method> > getPossibleMethods(List<FileData> referenceSource, StudentWorkCompiler compiler, List<FileData> testCodeFiles) throws CompilationException, Exception {

		if (referenceSource == null || referenceSource.size() == 0) {
			return null;
		}
		if (testCodeFiles == null || testCodeFiles.size() == 0) {
			return null;
		}
		

		List<FileData> rubricFiles = new ArrayList<FileData>(referenceSource);
		
		for (FileData sourceFile : testCodeFiles) {							
				rubricFiles.add(sourceFile);			
		}
		return getPossibleMethods(rubricFiles, compiler, createFilesToAddSet(testCodeFiles), true);
		
	}
		
	public static Map<String, List<Method> > getPossibleMethods(List<FileData> source, StudentWorkCompiler compiler, Set<String> filesToAdd, boolean filterTestCode) throws CompilationException, Exception {
		Map<String, Class<?>> compiled = null;
		compiled = compiler.compile(source);

		Map<String, List<Method> > methods = new HashMap<String, List<Method> >();
		for (FileData sourceFile : source) {
			if (filesToAdd.contains(sourceFile.getName())) {
				List<Method> fileMethods = new ArrayList<Method>();
				methods.put(sourceFile.getName(), fileMethods);
				Class<?> aClass = compiled.get(sourceFile.getClassName());
				if (aClass != null ) {
					for (Method method : aClass.getMethods()) {					
						boolean validReturn = (filterTestCode == false || method.getReturnType() == double.class || method.getReturnType() == Double.class);
						if ((method.getParameterCount() == 0 || filterTestCode == false) && validReturn) {
							fileMethods.add(method);
						}
					}
				}			
			}
		}
		return methods;
	}
	
	protected Double runAutomation(RubricEntry entry, String studentName, String studentId, CompilerMessage message, StudentWorkCompiler compiler, List<FileData> referenceSource, List<FileData> testCodeSource, ConsoleData consoleData) {
		//if (message == null) {
			//return null;
		//}
		if (true || message.isSuccessful()) {

			ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "Running " + this.getOwnerName() + " for " + studentName);
		

			List<FileData> studentFiles = compiler.getSourceCode(studentId);
			return runAutomation_(entry, studentFiles, studentName, studentId, compiler, referenceSource, testCodeSource, consoleData);
		}
		return null;
	}
	protected Double runAutomation_(RubricEntry entry, List<FileData> studentFiles, String studentName, String studentId, StudentWorkCompiler compiler, List<FileData> referenceSource, List<FileData> testCodeSource, ConsoleData consoleData) {
		if (studentFiles != null && studentFiles.size() != 0)
		{
			if (methodToCall == null || testCodeSourceToUse.size() == 0) {
				System.err.println(getOwnerName() + " is not fully defined ");
				return null;
			}

			List<FileData> rubricFiles = new ArrayList<FileData>(studentFiles);
			consoleData.runStarted(studentId, getOwnerName());				
			prepareForNextTest();
			addOutput(studentId, entry.getTipMessage());
			List<String> namesReplaced = new ArrayList<String>();

			String error = replaceClassNames(namesReplaced, studentFiles, rubricFiles, studentId, compiler, referenceSource, testCodeSource);
			if (error != null) {
				ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
				addOutput(studentId, error);					
				System.out.println("\0");					
				return null;					
			}
			Class<?> []params = {};
			Object []args = {};
			Object returnValue = null;
			for (FileData rubricFile : rubricFiles) {
				DebugLogDialog.appendln(rubricFile.getName());
				DebugLogDialog.appendln(rubricFile.getFileContents());
			}

			try {					
				returnValue = compiler.compileAndRun(true,  rubricFiles, methodToCall, params, args, true);
				if (compiler.getCompilerMessage(studentId) == null) {
					compiler.setCompilerMessage(studentId, true);
				}
			}
			catch (Exception e) {
				try {
					returnValue = compiler.compileAndRun(true,  rubricFiles, methodToCall, params, args, false);
				}
				catch (Exception e2) {
					ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");					
					String errorMessage = createMethodErrorMessage(referenceSource, studentFiles, namesReplaced, studentName, compiler);
					if (errorMessage.length() != 0) {
						addOutput(studentId, errorMessage);
					}
					else {
						addOutput(studentId, e2.getMessage());
						addOutput(studentId, "The parameters may not match in the automation.  Try modifying the test code's parameter order & re-running the automation.");
					}
					System.out.println("\0");					
					return null;
				}
			}
			

			if (returnValue == null) {
				System.out.println("\0");
				ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
				return null;
			}
			double value = 0.0;
			if (returnValue != null) {
				value = Double.parseDouble(returnValue.toString());
			}
			if (value > 1.0) {
				addOutput(studentId, "The test code returned a value greater than 1.0. Test code should return values only between 0.0 and 1.0 (numTestPassed/numTestsRun)");
			}
			else if (value != 1.0 && updateOnlyOnPass) {
				addOutput(studentId, "This test only updates scores that fully pass. The tests cannot accurately say what % of the possible points should be assigned, so you will have to look at the source + pass/fail info to assign partial credit.");
				System.out.println("\0");
				ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
				return null;
			}

			waitForTestFinish();

			ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");			
			return value;
		}
		return null;
	}
	private static Set<String> createFilesToAddSet(List<FileData> files) {
		Set<String> filesToAdd = new HashSet<String>();
		for (FileData file : files) {
			filesToAdd.add(file.getName());
		}
		return filesToAdd;		
	}
	private static List<Method> createCompleteMethodList(Map<String, List<Method>> methodMap) {
		List<Method> allMethods = new ArrayList<Method>();
		for (List<Method> methods : methodMap.values()) {
			allMethods.addAll(methods);			
		}
		return allMethods;
		
	}
	private static Method getMatchingMethod( String methodToFind, List<Method> allMethods) {		
		for (Method check : allMethods) {
			if (check.getName().equals(methodToFind)) {
				return check;
			}
		}
		return null;
	}
	

	
	private static String formatMethodCall(Method method) {
		String formatted = "";
		formatted += "toString" + method.toString() + "\n";
		formatted += "toGeneric" + method.toGenericString() + "\n";
		formatted += "return " + method.getReturnType().descriptorString() + "\n";
		formatted += "getname " + method.getName() + "\n";
		formatted += "params (";
		boolean first = true;
		for (Parameter param : method.getParameters() ) {
			if (first == false) {
				formatted += ", ";
			}
			formatted += param.toString();
		}
		formatted += ")";

		return formatted;
	}
	
	private static Map<String, MethodAndFieldList> getMethodMap(List<FileData> sourceFiles) {
		Map<String, MethodAndFieldList> sourceMap = new HashMap<String, MethodAndFieldList>();
		
		for (FileData sourceFile : sourceFiles) {
			CompilationUnit compilationUnit = StaticJavaParser.parse(sourceFile.getFileContents());
			MethodAndFieldList methodAndFieldList = new MethodAndFieldList();
			methodAndFieldList.visit(compilationUnit, null);
			sourceMap.put(sourceFile.getClassName(), methodAndFieldList);
		}
		return sourceMap;

	}
	
	private static Map<String, MethodDeclaration> completeMethodList(List<FileData> sourceFiles) {
		Map<String, MethodAndFieldList> sourceMap = getMethodMap(sourceFiles);
		Map<String, MethodDeclaration> methods = new HashMap<String, MethodDeclaration>();
		for (MethodAndFieldList methodAndFieldList : sourceMap.values()) {
			for (MethodDeclaration method : methodAndFieldList.getMethods()) {
				methods.put(method.getNameAsString(), method);
			}
		}
		return methods;
	}
	
	private static String createMethodErrorMessage(List<FileData> referenceSource, List<FileData> studentFiles, List<String> namesReplaced, String studentName, StudentWorkCompiler compiler) {
		String mismatchMethods = "";
		Set<String> check = new HashSet<String>();
		try {
			Map<String, MethodDeclaration> studentDeclarations = completeMethodList(studentFiles);
			Map<String, MethodDeclaration> referenceDeclarations = completeMethodList(referenceSource);
			for (String name : namesReplaced) {
				if (check.contains(name) == false) {
					check.add(name);

					MethodDeclaration student = studentDeclarations.get(name);
					MethodDeclaration reference = referenceDeclarations.get(name);
					if (student != null && reference != null) {
						String studentMethodName = student.getDeclarationAsString();
						String referenceMethodName = reference.getDeclarationAsString();

						if (!studentMethodName.equals(referenceMethodName)) {
							mismatchMethods += "Expected: " + referenceMethodName + "\n";
							mismatchMethods += "Actual:   " + studentMethodName + "\n";
						}
					}
				}
			}
		}
		catch (Exception e) {
			return "";
		}
		if (mismatchMethods.length() != 0) {
			mismatchMethods = "Student method signatures do not match expected\n" + mismatchMethods;
		}
		return mismatchMethods;		
	}
	
	private String replaceClassNames(List<String> namesReplaced, List<FileData> studentFiles, List<FileData> rubricFiles, String studentId, StudentWorkCompiler compiler, List<FileData> referenceSource, List<FileData> testCodeSource) {
		String errorPre = "Not all the methods/class constants found that are needed by this test code.\n";
		errorPre += "If the student just mis-spelled a name, fix it in the student source tab.\n";
		errorPre += "If they are completely missing a method, in the student source tab,\n";
		errorPre += "create an empty method that returns the correct type, but a bad value, like -10000000 in their file.\n\n";
		errorPre += "After you make the changes right-click and recompile, then re-run the rubric.\n";
		errorPre += "All code changes are local, and will not be saved back to google classroom\n\n";
		String error = "";
		try {
			
			Map<String, MethodAndFieldList> studentSourceMap = getMethodMap(studentFiles);			
			
			List<String> referenceSourceClassNames = new ArrayList<String>();
			for (FileData refSource : referenceSource) {
				boolean isTestCode = false;
				for (FileData testCode : testCodeSource) {
					if (testCode.getClassName().equals(refSource.getClassName())) {
						isTestCode = true;
						break;
					}
				}
				if (isTestCode == false) {
					referenceSourceClassNames.add(refSource.getClassName());
				}
			}
			for (FileData testFile : testCodeSource) {
				if (testCodeSourceToUse.contains(testFile.getName())) {
					CompilationUnit testCode = StaticJavaParser.parse(testFile.getFileContents());
					ClassNameModifier nameChange = new ClassNameModifier(namesReplaced, referenceSourceClassNames, studentSourceMap);
					nameChange.visit(testCode, null);
					if (nameChange.isError()) {
						error += nameChange.getErrorString();					
					}
					else {
						FileData temp = new FileData(testFile.getName(), testCode.toString(), studentId, null);
						rubricFiles.add(temp);
						namesReplaced.addAll(nameChange.getMethodModificationsMade());
					}
				}
			}
			
		}
		// Student source might not be parsable, in which case that is fine
		catch(Exception e) {
			error = "Could not parse student file";
			// Check to see if the test code compiles with the reference, if it doesn't print the compile message
			List<FileData> compileFiles = new ArrayList<FileData>(referenceSource);
			

			for (FileData sourceFile : testCodeSource) {
				if (testCodeSourceToUse.contains(sourceFile.getName())) {
					compileFiles.add(sourceFile);
				}
			}
			
			
			try {
				compiler.compile(compileFiles);
			} catch (CompilationException compEx ) {
				return compEx.getMessage();
			} catch (Exception compileMessage) {
				String compilerMessage = e.getLocalizedMessage();
				return compilerMessage;
			}

			
		}
		if (error.length() == 0) {
			return null;
		}
		return errorPre + error;
	}
	
	private static class ClassNameModifier extends ModifierVisitor<Void>  {
		private List<String> referenceSourceScopes;
		private Map<String, MethodAndFieldList> studentSources;
		private List<String> methodModificationsMade;
		boolean error;
		String errorString;
		public ClassNameModifier(List<String> methodModificationsMade, List<String> referenceSources, Map<String, MethodAndFieldList> studentSources) {
			super();
			this.referenceSourceScopes = referenceSources;
			this.studentSources = studentSources;
			this.methodModificationsMade = methodModificationsMade;
			error = false;
			errorString = "";
		}
		/**
		 * @return the error
		 */
		public boolean isError() {
			return error;
		}
		public List<String> getMethodModificationsMade() {
			return methodModificationsMade;
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
		
		private boolean isReferenceScope(String scopeString) {
			for (String scope : referenceSourceScopes) {
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
			if (isReferenceScope(scopeString)) {
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
				if (isReferenceScope(scopeString)) {
					for (String studentKey : studentSources.keySet()) {
						List<MethodDeclaration> methods = studentSources.get(studentKey).getMethods();
						for (MethodDeclaration methodDec : methods) {
							if (methodDec.getNameAsString().equals(methodName)) {
								methodModificationsMade.add(n.getNameAsString());
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



	
	@Override
	protected void saveAutomationColumns(String entryName, List<List<Object>> columnData) {
		List<Object> labels = new ArrayList<Object>();
		List<Object> content = new ArrayList<Object>();
		labels.add(entryName);
		content.add(entryName);
		labels.add(ColumnNames.METHOD_TO_CALL.toString());
		content.add(methodToCall);
		labels.add(ColumnNames.UPDATE_ONLY_ON_PASS.toString());
		content.add((Boolean)updateOnlyOnPass);
		columnData.add(labels);
		columnData.add(content);
		String sourceFileNames = "";
		int count = testCodeSourceToUse.size();
		for (String sourceName : testCodeSourceToUse) {
			count--;
			sourceFileNames += sourceName;
			if (count != 0) {
				sourceFileNames += ",";
			}
		}
		labels.add(ColumnNames.SOURCE_FILE.toString());
		content.add(sourceFileNames);
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
					
					else if (label.equalsIgnoreCase(ColumnNames.UPDATE_ONLY_ON_PASS.toString())) {
						Object update = columns.get(1).get(row);
						if (update != null) {
							updateOnlyOnPass = Boolean.parseBoolean("" + update);
						}
					}
				}
			}
			if (files == null || files.size() == 0 ||  methodToCall == null) {
				showErrorMessage(entryName);							
			}
			if (files == null) {
				return;
			}
			testCodeSourceToUse = new HashSet<String>();
			for (Object fileO : files) {
				if (fileO instanceof String) {
					String file = (String)fileO;
					if (file.indexOf(".java") != -1) {
						testCodeSourceToUse.add(file);
						// We want them all sharing the same source so that when we edit the
						// source in the rubric, all of them see the edit
						if (fileDataMap.containsKey(file) == false) {
							FileData fileData = FileData.newFromSheet(file, columnData.get(file.toUpperCase()));
							if (fileData == null) {
								showErrorMessage(entryName);
							}
							else {
								fileDataMap.put(file, fileData);
							}
						}
					}
				}
			}
		}
	}

}
