package net.cdonald.googleClassroom.gui;

import java.awt.Font;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;

public class LineNumberTextArea extends JTextArea {
	private static final long serialVersionUID = 5392213771105928768L;
	private JScrollPane scrollPane = new JScrollPane();
	
	
	public JScrollPane getScrollPane() {
		return scrollPane;
	}
	
	public LineNumberTextArea() {
		super();
		addLineNumbers();
	}
	
	private static String getLineNumber(int number, int lastNumber) {
		String lineNum = "" + number;
		int stopPoint = (int)Math.log10(number);
		for (int i = (int)Math.log10(10000); i > stopPoint; i--) {		
			lineNum += " ";
		}
		if (number < lastNumber - 1) {
			lineNum += System.getProperty("line.separator");
		}
		return lineNum;
	}
	
	private void addLineNumbers() {		
		JTextArea lines = new JTextArea(getLineNumber(1, 1));
		Font font = new Font("monospaced", Font.PLAIN, getFont().getSize());
		setFont(font);
		lines.setFont(getFont());
		
 
		lines.setEditable(false);
 
		getDocument().addDocumentListener(new DocumentListener(){
			private int lastLineNumber = 0;
			private void changeLineNumbers() {
				int caretPosition = getDocument().getLength();
				Element root = getDocument().getDefaultRootElement();
				int lastNumber = root.getElementIndex( caretPosition ) + 2;
				if (lastNumber != lastLineNumber) {
					caretPosition = lines.getCaretPosition();
					lines.setText(getText(lastNumber));				
					lines.setCaretPosition(caretPosition);
					lastLineNumber = lastNumber;
				}
			}
			public String getText(int lastNumber){
				String text = getLineNumber(1, lastNumber);

				for(int i = 2; i < lastNumber; i++){
					text += getLineNumber(i, lastNumber);
				}
				return text;
			}
			@Override
			public void changedUpdate(DocumentEvent de) {
				changeLineNumbers();
			}
 
			@Override
			public void insertUpdate(DocumentEvent de) {
				changeLineNumbers();
			}
 
			@Override
			public void removeUpdate(DocumentEvent de) {
				changeLineNumbers();
			}
 
		});		
		scrollPane.getViewport().add(this);
		scrollPane.setRowHeaderView(lines);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	}


}
