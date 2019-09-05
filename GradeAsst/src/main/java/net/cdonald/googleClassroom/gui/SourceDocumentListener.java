package net.cdonald.googleClassroom.gui;

import javax.swing.JTextArea;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import net.cdonald.googleClassroom.model.FileData;

public class SourceDocumentListener implements DocumentListener {

	private FileData fileData;
	private String fileName;
	public SourceDocumentListener(FileData fileData, JTextArea textArea) {
		super();
		this.fileData = fileData;
	}
	
	private void updateSource(DocumentEvent e) {
		Document d = e.getDocument();
		int length = d.getLength();
		try {
			String text = d.getText(0, length);
			fileData.setFileContents(text);
		} catch (BadLocationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	

	@Override
	public void insertUpdate(DocumentEvent e) {
		updateSource(e);
	}

	@Override
	public void removeUpdate(DocumentEvent e) {
		updateSource(e);
	}

	@Override
	public void changedUpdate(DocumentEvent e) {
		// TODO Auto-generated method stub

	}

}
