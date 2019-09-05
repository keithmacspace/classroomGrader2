package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;

public class RubricEntryCallMethod extends RubricAutomation {
	public static Class<?>[] classesSupported = { int.class, boolean.class, double.class, String.class, Integer.class,
			Double.class, Boolean.class, int[].class, boolean[].class, double[].class, String[].class, List.class,
			ArrayList.class };
	private Class<?>[] paramTypes;
	private String methodToCall;
	boolean checkSystemOut;
	List<List<String>> inputs;
	List<String> outputs;
	

	public RubricEntryCallMethod(List<String> paramTypes, String methodToCall, String returnType) {
		super();
		this.paramTypes = new Class<?>[paramTypes.size()];
		for (int i = 0; i < paramTypes.size(); i++) {
			this.paramTypes[i] = convertStringToClass(paramTypes.get(i));
		}
		this.methodToCall = methodToCall;
		if (returnType == "System.out") {
			checkSystemOut = true;
		}
		inputs = new ArrayList<List<String>>();
		outputs = new ArrayList<String>();
		
	}
	
	public RubricEntryCallMethod(RubricEntryCallMethod other) {
		this.paramTypes = new Class<?>[other.paramTypes.length];
		
		for (int i = 0; i < other.paramTypes.length; i++) {
			this.paramTypes[i] = other.paramTypes[i];
		}
		this.methodToCall = other.methodToCall;
		this.checkSystemOut = other.checkSystemOut;				
		inputs = new ArrayList<List<String>>();
		outputs = new ArrayList<String>();
		for (List<String> inputList : other.inputs) {
			List<String> temp = new ArrayList<String>();

			for (String inputStr : inputList) {
				temp.add(inputStr);
			}
			inputs.add(temp);
		}
		for (String outputStr : other.outputs) {
			outputs.add(outputStr);
		}
		
	}
	public RubricAutomation newCopy() {
		return new RubricEntryCallMethod(this);
	}

	public void addParameterPair(List<String> inputs, String output) {
		this.inputs.add(inputs);
		this.outputs.add(output);
	}

	protected Double runAutomation_(RubricEntry owner, String studentName, String studentId, CompilerMessage message, StudentWorkCompiler compiler, ConsoleData consoleData) {
		if (message == null) {
			return null;
		}
		int runCount = 0;
		int passCount = 0;
		if (message.isSuccessful()) {
			consoleData.runStarted(message.getStudentId(), getOwnerName());
			Object[] args = new Object[paramTypes.length];
			for (int i = 0; i < inputs.size(); i++) {
				if (checkSystemOut) {
					prepareForNextTest();
				}
				List<String> inputList = inputs.get(i);
				int argCount = 0;
				String resultsMessage = "";
				for (int index = 0; index < inputList.size(); index++) {
					String input = inputList.get(index);
					Object value = convertStringToObject(input, paramTypes[argCount]);
					args[argCount] = value;
					resultsMessage += value;
					if (index < inputList.size() - 1) {
						resultsMessage += ", ";
						argCount++;
					}
				}
				runCount++;

				// here is where we translate the individual param types into that actual type
				Object results = compiler.runSpecificMethod(!checkSystemOut, methodToCall, message, paramTypes, args);
				if (results == null) {
					return null;
				}
				if (checkSystemOut) {
					results = getSysOutText(message.getStudentId());
				}
				String expected = outputs.get(i);
				resultsMessage += " = " + results;
				resultsMessage += ". Expected = " + expected + "\n";				

				if (results.equals(expected) == true) {
					passCount++;
					resultsMessage = "Pass: " + resultsMessage;
				}
				else {
					resultsMessage = "Fail: " + resultsMessage;
				}
				addOutput(message.getStudentId(), resultsMessage);
			}
		}		
		if (runCount > 0) {
			return (Double)((double) passCount / (double) runCount);
		}
		return null;
	}

	public Object convertStringToObject(String value, Class<?> typeToConvertTo) {

		if (typeToConvertTo == int.class) {
			return (int) Integer.decode(value);
		} else if (typeToConvertTo == boolean.class) {
			return (boolean) Boolean.parseBoolean(value);
		} else if (typeToConvertTo == double.class) {
			return (double) Double.parseDouble(value);
		} else if (typeToConvertTo == String.class) {
			return value;
		} else if (typeToConvertTo == Integer.class) {
			return Integer.decode(value);
		} else if (typeToConvertTo == Double.class) {
			return Double.parseDouble(value);
		} else if (typeToConvertTo == Boolean.class) {
			return Boolean.parseBoolean(value);
		}
		/*
		 * int[].class, boolean[].class, double[].class, String[].class, List.class,
		 * ArrayList.class
		 */
		return null;
	}

	public static Class<?> convertStringToClass(String value) {
		for (Class<?> check : classesSupported) {
			System.out.println("check = " + check + " " + "value = " + value);
			if (check.toString().compareTo(value) == 0) {
				return check;
			}
		}
		return null;
	}

	public static RubricEntry createTest() {
		List<String> paramTypes = new ArrayList<String>();
		paramTypes.add(RubricEntryCallMethod.classesSupported[0].toString());
		paramTypes.add(RubricEntryCallMethod.classesSupported[0].toString());
		RubricEntryCallMethod test = new RubricEntryCallMethod(paramTypes, "add", paramTypes.get(0));

		for (int j = 0; j < 20; j++) {
			List<String> inputs = new ArrayList<String>();
			int value = 0;

			inputs.add("" + j);
			inputs.add("" + j);
			value = j + j;

			test.addParameterPair(inputs, "" + value);
		}

		RubricEntry entry = new RubricEntry();
		entry.setName("Test");
		entry.setValue(5);
		//entry.setAutomationType(RubricEntry.AutomationTypes.CALL_METHOD);
		entry.setAutomation(test);
		return entry;

	}

	public static void main(String[] args) {
		try {
			Class<?> otherClassType = java.util.Scanner[].class;
			System.out.println(otherClassType);
			Class<?> classType = Class.forName("java.util.Scanner");
			System.out.println(classType);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}


	@Override
	protected void loadAutomationColumns(String entryName, Map<String, List<List<Object>>> columnData, Map<String, FileData> fileData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void saveAutomationColumns(String entryName, List<List<Object>> columnData,
			Map<String, List<Object>> fileData) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void removeFileData(FileData fileData) {
		
	}
}
