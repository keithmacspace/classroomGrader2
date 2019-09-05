package net.cdonald.googleClassroom.inMemoryJavaCompiler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.model.FileData;

/**
 * 
 * This class holds all the information about a single student's build 
 * It holds the source files, the compiler message, and the ClassLoader
 * that should be used to run the data
 *
 */
public class StudentBuildInfo {
	// mapped by class name, ex: net.cdonald.googleClassroom.inMemoryJavaCompiler would be the key
	private Map<String, Class<?>> studentCompilerMap;
	private List<FileData> studentFileData;
	private CompilerMessage compilerMessage;
	
	public StudentBuildInfo() {
		compilerMessage = null;
		studentCompilerMap = null;
		studentFileData = new ArrayList<FileData>();
	}

	
	public List<FileData> getStudentFileData() {
		return studentFileData;
	}
	
	public void addFileData(FileData data) {
		studentFileData.add(data);
	}

	public CompilerMessage getCompilerMessage() {
		return compilerMessage;
	}

	public void setCompilerMessage(CompilerMessage compilerMessage) {
		this.compilerMessage = compilerMessage;
	}
	
	public Map<String, Class<?>> getStudentCompilerMap() {
		return studentCompilerMap;
	}

	public void setStudentCompilerMap(Map<String, Class<?>> studentCompilerMap) {
		this.studentCompilerMap = studentCompilerMap;
	}
	
	public String getCompleteMethodName(String methodName) {
		for (String key : studentCompilerMap.keySet()) {
			Class<?> aClass = studentCompilerMap.get(key);
			for (Method method : aClass.getMethods()) {
				String currentMethodName = method.getName();

				if (methodName.compareTo(currentMethodName) == 0) {					
					return key + "." + currentMethodName;
				}
			}
		}
		return null;
	}


	public void changeFileData(String fileName, String fileText) {
		for (int i = 0; i < studentFileData.size(); i++) {
			if (studentFileData.get(i).getName().contentEquals(fileName)) {
				studentFileData.get(i).setFileContents(fileText);
			}
		}
		
	}


	public void removeSource(String fileName) {
		for (int i = 0; i < studentFileData.size(); i++) {
			if (studentFileData.get(i).getName().contentEquals(fileName)) {
				studentFileData.remove(i);
				break;
			}
		}
 
		
	}


}
