package net.cdonald.googleClassroom.utils;

import java.awt.Color;
import java.lang.reflect.Method;

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import javax.swing.text.Highlighter.HighlightPainter;

import net.cdonald.googleClassroom.gui.DebugLogDialog;

public class HighlightText {
	public static void highlightMethod(JTextArea textArea, Method method) {
		if (method != null) {
			String returnValueName = method.getReturnType().toString();
			String methodName = method.getName();		
			String searchText = textArea.getText();		


			String [] words = searchText.split("\\s");
			int index = 0;
			for (int i = 0; i < words.length; i++) {
				String word = words[i];
				index = searchText.indexOf(word, index);
				if (word.equals(returnValueName)) {
					if (i + 1 < words.length) {
						String nextWord = words[i + 1];
						int paren = nextWord.indexOf('(');
						if (paren != -1) {
							nextWord = nextWord.substring(0, paren);
						}
						if (nextWord.equals(methodName)) {
							int finalIndex = searchText.indexOf(nextWord, index) + methodName.length();
							moveCursorAndHighlight( textArea, index, finalIndex);
						}
					}
				}
			}
		}
	}
	public static void moveCursorAndHighlight(JTextArea textArea, int index, int finalIndex) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				int position = 0;
				try {
					position = textArea.getLineOfOffset(index);
				} catch (BadLocationException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				Highlighter highlighter = textArea.getHighlighter();
				highlighter.removeAllHighlights();
				HighlightPainter painter = new DefaultHighlighter.DefaultHighlightPainter(Color.LIGHT_GRAY);
				try {
					highlighter.addHighlight(index, finalIndex, painter);
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				textArea.setCaretPosition(position * 2);
				double ratio = (double)position/textArea.getLineCount();			
			}
		});
	}
}
