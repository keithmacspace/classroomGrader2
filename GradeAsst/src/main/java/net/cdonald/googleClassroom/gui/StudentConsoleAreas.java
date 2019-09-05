package net.cdonald.googleClassroom.gui;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JTextArea;

public class StudentConsoleAreas {
	public class OutputAreas {
		private JTextArea outputArea;
		private JTextArea errorArea;
		public OutputAreas() {
			outputArea = new JTextArea();
			errorArea = new JTextArea();
		}
		
		public JTextArea getOutputArea() {
			return outputArea;
		}

		public JTextArea getErrorArea() {
			return errorArea;
		}

		public void clear() {
			outputArea.setText("");
			errorArea.setText("");
		}
		
	};
	private Map<String, OutputAreas>  rubricOutputMap;
	private JTextArea inputArea;
	private OutputAreas outputAreas;
	
	public StudentConsoleAreas() {
		rubricOutputMap = new HashMap<String, OutputAreas>();		
		inputArea = new JTextArea();
		outputAreas = new OutputAreas();
	}
	
	public OutputAreas getRubricArea(String rubricName) {
		if (rubricOutputMap.containsKey(rubricName) == false) {			
			rubricOutputMap.put(rubricName, new OutputAreas());
		}
		return rubricOutputMap.get(rubricName);
	}

	public OutputAreas getOutputAreas() {
		return outputAreas;
	}

	public JTextArea getInputArea() {
		return inputArea;
	}
	
	public void appendToOutput(String rubricName, String text) {
		if (rubricName != "") {			
			getRubricArea(rubricName).outputArea.append(text);
		}
		else {
			outputAreas.outputArea.append(text);	
		}
	}
	
	public void clear() {
		outputAreas.clear();
		inputArea.setText("");
		rubricOutputMap.clear();
	}
	
}
