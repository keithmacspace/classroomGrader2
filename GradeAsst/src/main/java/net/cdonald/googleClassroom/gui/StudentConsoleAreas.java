package net.cdonald.googleClassroom.gui;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

public class StudentConsoleAreas {
	public class OutputAreas {
		private JTextPane outputArea;
		private JTextArea errorArea;
		private Style black;
		private Style red;
		private Style blue;
		private String currentOutput;
		private String currentError;
		public OutputAreas() {
			outputArea = createTextPane();

			errorArea = new JTextArea();
			errorArea.setEditable(false);
			currentOutput = "";
			currentError = "";
		}
		private JTextPane createTextPane() {
			JTextPane pane = new JTextPane();
			StyledDocument doc = pane.getStyledDocument();
			JTextArea temp = new JTextArea();
			red = createStyle("Red", doc, temp, Color.RED);
			black = createStyle("Black", doc, temp, Color.BLACK);
			blue = createStyle("Blue", doc, temp, Color.BLUE);
			return pane;
		}
		private Style createStyle(String name, StyledDocument doc, JTextArea temp, Color color) {
			Style def = StyleContext.getDefaultStyleContext().getStyle(StyleContext.DEFAULT_STYLE);

			Font font = temp.getFont();
			Style style = doc.addStyle(name, def);
			StyleConstants.setFontSize(style, font.getSize());
			StyleConstants.setFontFamily(style, font.getFamily());
			StyleConstants.setForeground(style, color);
			return style;
			
		}
		
		
		private String appendText(String currentText, String text, JTextPane pane, Style style, boolean finalPush) {
			if (text.endsWith("\n") || finalPush) {
				try {
					StyledDocument doc = pane.getStyledDocument();
					doc.insertString(doc.getLength(), currentText +  text,  style);
				}
				catch(BadLocationException e) {
					
				}
				currentText = "";
			}
			else {
				String [] lines = text.split("\n");
				for (int i = 0; i < lines.length - 1; i++) {
					appendText(currentText, lines[i] + "\n", pane, style, finalPush);
					currentText = "";
				}
				currentText = lines[lines.length - 1];
			}
			return currentText;			
		}
		public void appendInputToOutput(String input) {
			currentOutput = appendText(currentOutput, input, outputArea, blue, true);
		}
		
		public void appendOutput(String output, boolean finalPush) {			
			currentOutput = appendText(currentOutput, output, outputArea, black, finalPush);
		}
		public void appendError(String error, boolean finalPush) {
			currentError = appendText(currentError, error, outputArea, red, finalPush);
//			errorArea.append(error);
		}
		public void clearText() {
			outputArea.setText("");
			errorArea.setText("");
		}
		
		public JTextPane getOutputArea() {
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
	private OutputAreas outputAreas;
	
	public StudentConsoleAreas() {
		rubricOutputMap = new HashMap<String, OutputAreas>();				
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

	
	public void appendToOutput(String rubricName, String text) {
		if (rubricName != null && rubricName != "") {			
			getRubricArea(rubricName).appendOutput(text, true);
		}
		else {
			outputAreas.appendOutput(text, true);	
		}
	}
	
	public void clear() {
		outputAreas.clear();
		rubricOutputMap.clear();
	}
	
}
