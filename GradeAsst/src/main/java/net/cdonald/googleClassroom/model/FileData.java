package net.cdonald.googleClassroom.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.visitor.ModifierVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class FileData extends ClassroomData {
	public static final String REFERENCE_SOURCE_ID = "ReferenceSource";
	private String fileContents;
	private String packageName;
	private String className;
	private static final String DB_NAME = "JavaFiles";
	private boolean isRubricCode;
	private CompilationUnit compilationUnit;

	public static String dbTableName(ClassroomData assignment) {
		return DB_NAME + assignment.getId();
	}

	public static enum fieldNames {
		STUDENT_ID, NAME, CREATION_TIME, FILE_CONTENTS
	}
	
	public FileData() {
		super();
	}

	public FileData(String name, String fileContents, String id, String creationTime) {
		super(name, id, creationTime);
		setFileContents(fileContents);
		className = getName().replace(".java", "");
	}

	public FileData(Map<String, String> dbInfo) {
		for (String fieldName : dbInfo.keySet()) {
			for (fieldNames field : fieldNames.values()) {
				if (fieldName.compareToIgnoreCase(field.toString()) == 0) {
					setDBValue(field, dbInfo.get(fieldName));
				}
			}
		}
	}
	
	public FileData(FileData other) {
		super(other);
		setFileContents(other.fileContents);
		packageName = other.packageName;
		className = other.className;
		isRubricCode = other.isRubricCode;
	}
	

	
	public static FileData newFromSheet(String fileName, List<List<Object>> sourceInfo) {
		FileData fileData = null;
		if (sourceInfo != null) {
			String text = "";
			boolean firstLine = true;
			List<Object> lines = sourceInfo.get(0);
			for (Object lineO : lines) {
				if (lineO != null) {
					if (lineO instanceof String) {
						String line = (String)lineO;
						if (line.length() > 0) {
							if (firstLine == false) {
								text += line + "\n";
							}							
							firstLine = false;
						}
					}
				}					
			}
			fileData = new FileData(fileName, text, REFERENCE_SOURCE_ID, null); 
		}
		return fileData;
	}
	
	public CompilationUnit getCompilationUnit() {
		return compilationUnit;
	}

	public String getFileContents() {
		return fileContents;
	}
	
	public static String stripPackage(String fileContents) {
		CompilationUnit compilationUnit = StaticJavaParser.parse(fileContents);		
		compilationUnit.setPackageDeclaration((PackageDeclaration)null);
		return compilationUnit.toString();
	}

	public void setFileContents(String fileContents) {
		this.fileContents = fileContents;
		compilationUnit = null;
		packageName = null;
		try {
			compilationUnit = StaticJavaParser.parse(fileContents);
			Optional<PackageDeclaration> pd = compilationUnit.getPackageDeclaration();
			if (pd.isPresent()) {
				packageName = pd.get().getNameAsString();
			}
			else {
				packageName = null;
			}
		}
		// Student source might not be parsable, in which case that is fine
		catch(Exception e) {

		}

	}

	public String getClassName() {
		if (packageName != null) {
			return packageName + "." + className;
		}
		return className;
	}

	@Override
	public String[] getDBValues() {
		String[] superNames = super.getDBValues();
		String[] dbNames = { superNames[ClassroomData.fieldNames.ID.ordinal()],
				superNames[ClassroomData.fieldNames.NAME.ordinal()],
				superNames[ClassroomData.fieldNames.DATE.ordinal()], fileContents };
		return dbNames;
	}

	public void setDBValue(fieldNames field, String value) {
		switch (field) {
		case STUDENT_ID:
			super.setDBValue(ClassroomData.fieldNames.ID, value);
			break;
		case NAME:
			super.setDBValue(ClassroomData.fieldNames.NAME, value);
			break;
		case CREATION_TIME:
			super.setDBValue(ClassroomData.fieldNames.DATE, value);
			break;
		case FILE_CONTENTS:
			fileContents = value;
			break;
		default:
			throw new IllegalArgumentException();
		}
	}

	public boolean isRubricCode() {
		return isRubricCode;
	}

	public void setRubricCode(boolean isRubricCode) {
		this.isRubricCode = isRubricCode;
	}

	public List<Object> fillSaveData() {
		List<Object> fileLineList = new ArrayList<Object>();
		fileLineList.add(getName()); // Column header
		String[] fileLineArray = getFileContents().split("\n");
		int rowCount = 0;
		int lineIndex = 0;
		while(lineIndex < fileLineArray.length) {
			// Start putting multiple lines on a single row.
			if (rowCount > 200) {
				String nextLine = "";
				while(nextLine.length() < 200 && lineIndex < fileLineArray.length) {
					nextLine += fileLineArray[lineIndex];
					if (nextLine.length() < 199) {
						nextLine += "\n";
					}
					lineIndex++;
				}
				fileLineList.add(nextLine);
			}
			else {
				fileLineList.add(fileLineArray[lineIndex]);
				lineIndex++;	
			}
			rowCount++;					
		}
		return fileLineList;
	}
	
	public boolean modifySourceFile(ModifierVisitor<Void> modifier) {
		if (compilationUnit != null) {
			modifier.visit(compilationUnit, null);
			fileContents = compilationUnit.toString();
			return true;
		}
		return false;
	}
	
	public static FileData initFromDisk(String path) {		
		File file = new File(path);
		String fileName = file.getName();
		String content = null;
		try {			 
			 content = new String(Files.readAllBytes(Paths.get(file.toURI())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (content != null) {
			return new FileData(fileName, content, "RefSource", null);
		}
		return null;
	}
	
	

	public static void main(String[] args) {
		String testPackageRemove = "package x;"
				+ "\n"
				+ "public class empty {"
				+ "}";
		System.out.println(FileData.stripPackage(testPackageRemove));
//		FileData fileData = new FileData("file.java", "", "0", null);
//		String contents = "// this is a test\n" + "while(a != b && (c != d))\n /*{\n" + "here is a comment\n" + "*/{\n";
//		fileData.setFileContents(contents);
//		//fileData.instrumentFile("words.words.call()");
//		System.out.println(fileData.getFileContents());
	} 
	// This is used when the student uploads the file as a google document. In those cases
	// the name of the file is not className.java, and so we have to create it
	public static String createFileName(String initialFileName, String fileContents) {
		String fileName = initialFileName;
		// Ideal case is when we can parse & find the className
		try {
			CompilationUnit studentSourceCode = StaticJavaParser.parse(fileContents);		
			ClassNameFinder classNameFinder = new ClassNameFinder();
			classNameFinder.visit(studentSourceCode, null);
			fileName = classNameFinder.getClassName();
		}
		catch(Exception e) {
			// If the source does not compile, then we have to search it for the word class
			int publicIndex = fileContents.indexOf("public");
			boolean found = false;
			if (publicIndex != -1) {
				int classIndex = fileContents.indexOf("class", publicIndex);
				if (classIndex != -1) {
					classIndex += "class".length();
					while (Character.isWhitespace(fileContents.charAt(classIndex))) {
						classIndex++;
					}
					int endIndex = classIndex;
					while (!Character.isWhitespace(fileContents.charAt(endIndex))) {
						endIndex++;
					}
					fileName = fileContents.substring(classIndex, endIndex);
					found = true;
				}
			}
			if (found == false) {
				boolean replaced = true;
				while (replaced == true) {
					replaced = false;
					for (int i = 0; i < fileName.length(); i++) {
						if (Character.isAlphabetic(fileName.charAt(i)) == false) {
							fileName = fileName.replace("" + fileName.charAt(i), "");
							replaced = true;
						}
					}
				}
			}			
		}	
		fileName += ".java";
		return fileName;

	}
	private static class ClassNameFinder extends VoidVisitorAdapter<Void> {
		private String className = null;
		@Override
        public void visit(ClassOrInterfaceDeclaration n, Void arg) {
			// TODO Auto-generated method stub
			super.visit(n, arg);
			setClassName(n.getName().toString());			
		}
		public String getClassName() {
			return className;
		}
		public void setClassName(String className) {
			this.className = className;
		}
	}

}
