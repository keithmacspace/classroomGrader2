package net.cdonald.googleClassroom.utils;

import java.awt.GridBagConstraints;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class SimpleUtils {
	private static final String PATTERN = "MM/dd/yyyy HH:mm:ss";
	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(PATTERN);

	public static List<String> breakUpCommaList(Object object) {
		List<String> partsList = new ArrayList<String>();
		if (object instanceof String) {
			String [] parts = ((String)object).split(",");
			for (String part : parts) {
				partsList.add(part.trim());
			}			
		}
		return partsList;		
	}
	
	public static String formatDate(Date date) {
		if (date != null) {
			return simpleDateFormat.format(date);
		}
		return "";
	}
	
	public static Date createDate(String date) {
		try {
			return simpleDateFormat.parse(date);
		} catch (ParseException e) {
			
		}
		return null;
	}
	
	public static void addLabel(JPanel parent, JLabel label, int y) {
		GridBagConstraints l = new GridBagConstraints();
		l.weightx = 0;
		l.weighty = 0;
		l.gridx = 0;
		l.gridy = y;
		l.gridheight = 1;
		l.anchor = GridBagConstraints.LINE_END;
		parent.add(label, l);		
	}
	public static void addLabelAndComponent(JPanel parent, JLabel label, JComponent component, int y) {
		addLabel(parent, label, y);
		GridBagConstraints c = new GridBagConstraints();
		c.weightx = 1.0;
		c.weighty = 0.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.LINE_START;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = y;
		parent.add(component, c);
	}
}
