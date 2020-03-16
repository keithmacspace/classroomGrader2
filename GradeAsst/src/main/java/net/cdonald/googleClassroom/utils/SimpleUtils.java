package net.cdonald.googleClassroom.utils;

import java.awt.GridBagConstraints;
import java.awt.GridLayout;
import java.awt.Insets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.cdonald.googleClassroom.model.FileData;
import net.cdonald.googleClassroom.model.MyPreferences;

public class SimpleUtils {
	public static enum TimeUnit{NONE, WEEK, DAY, HOUR, MINUTES};
	public static final String DATE_PATTERN = "MM/dd/yyyy HH:mm:ss";
	public static final String GOOGLE_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(DATE_PATTERN);
	private static final SimpleDateFormat googleDateFormat = new SimpleDateFormat(GOOGLE_PATTERN);
	private static final long MS_PER_MINUTE = 60000;
	private static final long MS_PER_HOUR = 60 * MS_PER_MINUTE;
	private static final long MS_PER_DAY = MS_PER_HOUR * 24;
	private static final long MS_PER_WEEK = MS_PER_DAY * 7;

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
		try {
			return googleDateFormat.parse(date);				
		} catch (ParseException e1) {

		}
		
		return null;
	}

	public static TimeUnit convertTimeUnit(MyPreferences.LateType lateType) {
		switch(lateType) {
		case Days:
			return TimeUnit.DAY;			
		case Hours:
			return TimeUnit.HOUR;			
		case Minutes:
			return TimeUnit.MINUTES;			
		default:
			return TimeUnit.MINUTES;		
		}
	}

	public static double calculateDifference(Date submitDate, Date dueDate, MyPreferences.LateType lateType) {
		return calculateDifference(submitDate, dueDate, convertTimeUnit(lateType));
	}

	public static double calculateDifference(Date submitDate, Date dueDate, TimeUnit timeUnit) {
		long dueDateTime = dueDate.getTime();
		long submitDateTime = submitDate.getTime();
		double difference = submitDateTime - dueDateTime;
		switch(timeUnit) {

		case DAY:
			difference /= MS_PER_DAY;
			break;
		case HOUR:
			difference /= MS_PER_HOUR;
			break;
		case MINUTES:
			difference /= MS_PER_MINUTE;
		case WEEK:
			difference /= MS_PER_WEEK;
			break;
		default:
			difference = 0;
			break;
		}
		return difference;		
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
		c.insets = new Insets(3, 3, 3, 0);
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

	public static String formatLate(Date submitDate, Date dueDate) {
		if (submitDate == null || dueDate == null) {
			return "";
		}
		if (dueDate.getTime() >= submitDate.getTime() ) {
			return "On Time";
		}
		long dueDateTime = dueDate.getTime();
		long submitDateTime = submitDate.getTime();
		long difference = submitDateTime - dueDateTime;
		if (difference / MS_PER_MINUTE == 0) {
			return "On Time";
		}
		String lateInfo = "";
		long week = difference / MS_PER_WEEK;
		if (week > 0) {
			lateInfo += week + "Wk ";			
		}
		difference -= week * MS_PER_WEEK;
		long day = difference / MS_PER_DAY;
		if (day > 0) {
			lateInfo += day + "D ";
		}
		if (week == 0) {
			difference -= day * MS_PER_DAY;		
			long hour = difference / MS_PER_HOUR;
			if (hour > 0) {
				lateInfo += hour + "H ";
			}
			if (day == 0) {
				difference -= hour * MS_PER_HOUR;		
				long minute = difference / MS_PER_MINUTE;
				if (minute > 0) {
					lateInfo += minute + "Min ";
				}
			}
		}
		lateInfo += "late";
		return lateInfo;
	}
	
	public static JPanel createButtonPanel(int numButtons) {
		final int SPACE = 6;
		final int BUTTON_TOP_SPACE = 5;
		JPanel buttonsPanel;
		GridLayout buttonLayout;
		buttonsPanel = new JPanel();
		// buttonsPanel.setLayout(new FlowLayout());
		buttonLayout = new GridLayout(numButtons, 0);
		final int GAP_SIZE = 6;
		buttonLayout.setVgap(GAP_SIZE);
		buttonsPanel.setBorder(BorderFactory.createEmptyBorder(BUTTON_TOP_SPACE, SPACE, SPACE, SPACE));
		buttonsPanel.setLayout(buttonLayout);
		return buttonsPanel;
	}
	
	public static List<FileData> mergeSourceLists(List<FileData> studentSource, List<FileData> supportSource) {
		if (supportSource == null || supportSource.size() == 0) {
			return studentSource;
		}
		List<FileData> merged = new ArrayList<FileData>(studentSource);
		for (FileData support : supportSource) {
			boolean found = false;
			for (FileData student : studentSource) {
				if (student.getName().equals(support.getName())) {
					found = true;
				}
			}
			if (found == false) {
				merged.add(support);
			}
		}
		return merged;
	}


}
