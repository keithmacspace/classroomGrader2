package net.cdonald.googleClassroom.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
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
	private List<FileData> sourceFiles;
	private List<String> referenceSourceClassNames;
	boolean checkSystemOut;		
	private enum ColumnNames {METHOD_TO_CALL, CLASS_NAMES_TO_REPLACE, SOURCE_FILE};


	
	public RubricEntryRunCode() {		
		sourceFiles = new ArrayList<FileData>();
		referenceSourceClassNames = new ArrayList<String>();
	}
	
	public RubricEntryRunCode(RubricEntryRunCode other) {
		methodToCall = other.methodToCall;
		sourceFiles = new ArrayList<FileData>();
		referenceSourceClassNames = new ArrayList<String>();
		for (FileData fileData : other.sourceFiles) {
			sourceFiles.add(fileData);
		}
		for (String className : other.referenceSourceClassNames) {
			referenceSourceClassNames.add(className);
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
	public void getTestCode(List<FileData> files, Set<String> names) {
		for (FileData sourceFile : sourceFiles) {
			if (names.contains(sourceFile.getName()) == false){
				files.add(sourceFile);
				names.add(sourceFile.getName());
			}
		}
	}
	
	@Override
	public void modifyTestCode(String fileName, String contents) {
		for (FileData source : sourceFiles ) {
			if (source.getName().equals(fileName) ) {
				source.setFileContents(contents);
			}
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
	
	
	public List<Method> getPossibleMethods(List<FileData> referenceSource, StudentWorkCompiler compiler) {

		if (referenceSource == null || referenceSource.size() == 0) {
			return null;
		}
		
		referenceSourceClassNames.clear();		
		for (FileData fileData : referenceSource) {
			referenceSourceClassNames.add(fileData.getClassName());
		}
		
		List<FileData> rubricFiles = new ArrayList<FileData>(referenceSource);
		

		for (FileData sourceFile : sourceFiles) {
			rubricFiles.add(sourceFile);			
		}
		
		Map<String, Class<?>> compiled = null;
		try {
			compiled = compiler.compile(rubricFiles);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage(), "Error compiling the test code",
					JOptionPane.ERROR_MESSAGE);
			DebugLogDialog.appendException(e);
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
	
	protected Double runAutomation_(RubricEntry entry, String studentName, String studentId, CompilerMessage message, StudentWorkCompiler compiler, List<FileData> referenceSource, ConsoleData consoleData) {
		if (message == null) {
			return null;
		}
		if (message.isSuccessful()) {

			ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "Running " + this.getOwnerName() + " for " + studentName);
		

			List<FileData> studentFiles = compiler.getSourceCode(studentId);
			return runAutomation_(studentFiles, studentId, compiler, referenceSource, consoleData);
		}
		return null;
	}
	protected Double runAutomation_(List<FileData> studentFiles, String studentId, StudentWorkCompiler compiler, List<FileData> referenceSource, ConsoleData consoleData) {
		if (studentFiles != null && studentFiles.size() != 0)
		{
			if (methodToCall == null || sourceFiles.size() == 0 || referenceSourceClassNames == null || referenceSourceClassNames.size() == 0) {
				System.err.println(getOwnerName() + " is not fully defined ");
				return null;
			}

			List<FileData> rubricFiles = new ArrayList<FileData>(studentFiles);
			consoleData.runStarted(studentId, getOwnerName());				
			prepareForNextTest();

			String error = replaceClassNames(studentFiles, rubricFiles, studentId, compiler, referenceSource);
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
				returnValue = compiler.compileAndRun(true,  rubricFiles, methodToCall, params, args, true);				
			}
			catch (Exception e) {
				ListenerCoordinator.fire(SetInfoLabelListener.class, SetInfoLabelListener.LabelTypes.RUNNING, "");
				addOutput(studentId, e.getMessage());
				addOutput(studentId, "The parameters may not match in the automation.  Try modifying the test code's parameter order & re-running the automation.");
				System.out.println("\0");					
				return null;
			}

			if (returnValue == null) {
				System.out.println("\0");
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
	
	String replaceClassNames(List<FileData> studentFiles, List<FileData> rubricFiles, String studentId, StudentWorkCompiler compiler, List<FileData> referenceSource) {
		String errorPre = "Not all the methods/class constants found that are needed by this test code.\n";
		errorPre += "If the student just mis-spelled a name, fix it in the student source tab.\n";
		errorPre += "If they are completely missing a method, in the student source tab,\n";
		errorPre += "create an empty method that returns the correct type, but a bad value, like -10000000 in their file.\n\n";
		errorPre += "After you make the changes right-click and recompile, then re-run the rubric.\n";
		errorPre += "All code changes are local, and will not be saved back to google classroom\n\n";
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
				ClassNameModifier nameChange = new ClassNameModifier(referenceSourceClassNames, studentSourceMap);
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
			// Check to see if the test code compiles with the reference, if it doesn't print the compile message
			List<FileData> compileFiles = new ArrayList<FileData>(referenceSource);
			

			for (FileData sourceFile : sourceFiles) {
				compileFiles.add(sourceFile);			
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
		List<String> referenceSourceScopes;
		Map<String, MethodAndFieldList> studentSources;
		boolean error;
		String errorString;
		public ClassNameModifier(List<String> referenceSources, Map<String, MethodAndFieldList> studentSources) {
			super();
			this.referenceSourceScopes = referenceSources;
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
		for (int i = 0; i < referenceSourceClassNames.size() - 1; i++) {
			classes += referenceSourceClassNames.get(i) + ",";
		}
		if (referenceSourceClassNames.size() != 0) {
			classes += referenceSourceClassNames.get(referenceSourceClassNames.size() - 1);
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
			referenceSourceClassNames.clear();

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
						referenceSourceClassNames = SimpleUtils.breakUpCommaList(columns.get(1).get(row));
					}
				}
			}
			if (files == null || files.size() == 0 ||  referenceSourceClassNames.size() == 0 || methodToCall == null) {
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
