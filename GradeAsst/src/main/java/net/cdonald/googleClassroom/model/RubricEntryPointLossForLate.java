package net.cdonald.googleClassroom.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import net.cdonald.googleClassroom.inMemoryJavaCompiler.CompilerMessage;
import net.cdonald.googleClassroom.inMemoryJavaCompiler.StudentWorkCompiler;
import net.cdonald.googleClassroom.listenerCoordinator.GetCurrentAssignmentQuery;
import net.cdonald.googleClassroom.listenerCoordinator.ListenerCoordinator;
import net.cdonald.googleClassroom.utils.SimpleUtils;
import net.cdonald.googleClassroom.utils.SimpleUtils.TimeUnit;

public class RubricEntryPointLossForLate extends RubricAutomation {

	private double pointsLostPerTimeUnit;
	private SimpleUtils.TimeUnit timeUnit;
	private enum ColumnNames {TIME_UNIT, POINTS_LOST_PER_TIME_UNIT}
	

	public RubricEntryPointLossForLate() {
		timeUnit = TimeUnit.NONE;
		pointsLostPerTimeUnit = 0.0;
	}

	public RubricEntryPointLossForLate(RubricEntryPointLossForLate other) {
		timeUnit = other.timeUnit;
		pointsLostPerTimeUnit = other.pointsLostPerTimeUnit;
	}



	/**
	 * @return the pointsLostPerTimeUnit
	 */
	public double getPointsLostPerTimeUnit() {
		return pointsLostPerTimeUnit;
	}

	/**
	 * @param pointsLostPerTimeUnit the pointsLostPerTimeUnit to set
	 */
	public void setPointsLostPerTimeUnit(double pointsLostPerTimeUnit) {

		this.pointsLostPerTimeUnit = pointsLostPerTimeUnit;
	}

	/**
	 * @return the timeUnit
	 */
	public TimeUnit getTimeUnit() {
		return timeUnit;
	}

	/**
	 * @param timeUnit the timeUnit to set
	 */
	public void setTimeUnit(TimeUnit timeUnit) {		
		this.timeUnit = timeUnit;
	}

	@Override
	public RubricAutomation newCopy() {
		return new RubricEntryPointLossForLate(this);
	}

	@Override
	public void removeFileData(FileData fileData) {

	}


	@Override
	protected Double runAutomation_(RubricEntry owner, String studentName, String studentId, CompilerMessage message,
			StudentWorkCompiler compiler, ConsoleData consoleData) {
		ClassroomData assignment = (ClassroomData)ListenerCoordinator.runQuery(GetCurrentAssignmentQuery.class);
		if (assignment == null) {
			return null;
		}
		List<FileData> files = compiler.getSourceCode(studentId);
		if (files == null || files.size() == 0) {
			return null;
		}
		Date dueDate = assignment.getDate();
		if (dueDate == null) {
			return null;
		}
		Date submitDate = files.get(0).getDate();
		if (submitDate == null) {
			return null;
		}
		
		if (pointsLostPerTimeUnit == 0.0 || timeUnit == TimeUnit.NONE) {
			System.err.println(getOwnerName() + " is not fully defined ");
			return null;
		}
		
		

		// If we were totally on time, then we are done.
		if (submitDate.compareTo(dueDate) <= 0) {
			addOutput(studentId, "On time");
			return 1.0;
		}
		
		double difference = SimpleUtils.calculateDifference(submitDate, dueDate, timeUnit);
		double reportDifference = difference;
		reportDifference *= 10;
		reportDifference = (long)reportDifference;
		reportDifference /= 10;
		double numToSubtract = 0.0;
		String timeMessage;
		if (reportDifference == 0.0) {
			timeMessage = "Essentially on time. Only late by: " + difference;
		}
		else {
			timeMessage = "Late by " + reportDifference;
			numToSubtract = pointsLostPerTimeUnit * reportDifference;
		}
		timeMessage +=  " " + timeUnit.name().toLowerCase();
		if (reportDifference != 1.0) {
			timeMessage += "s";
		}
		addOutput(studentId, timeMessage);
		
		if (numToSubtract >= owner.getValue()) {
			addOutput(studentId, "Submitted too late to get any points");
		}
		return (double)(owner.getValue() - numToSubtract)/(double)owner.getValue();		
	}

	@Override
	protected void saveAutomationColumns(String entryName, List<List<Object>> columnData,
			Map<String, List<Object>> fileData) {

		List<Object> labels = new ArrayList<Object>();
		List<Object> content = new ArrayList<Object>();
		labels.add(entryName);
		content.add(entryName);
		labels.add(ColumnNames.TIME_UNIT.toString());
		labels.add(ColumnNames.POINTS_LOST_PER_TIME_UNIT.toString());
		content.add(timeUnit.toString());
		content.add(pointsLostPerTimeUnit);
		columnData.add(labels);
		columnData.add(content);


	}

	@Override
	protected void loadAutomationColumns(String entryName, Map<String, List<List<Object>>> columnData,
			Map<String, FileData> fileDataMap) {
		List<List<Object> > columns = columnData.get(entryName.toUpperCase());
		if (columns == null || columns.size() != 2) {
			Rubric.showLoadError("Missing data for entry: \"" + entryName + "\"");
			return;
		}
		else {

			timeUnit = null;
			pointsLostPerTimeUnit = -1.0;

			List<Object> labelRow = columns.get(0);
			for (int row = 0; row < labelRow.size(); row++) {
				String label = (String)labelRow.get(row);
				if (label != null) { 
					if (label.equalsIgnoreCase(ColumnNames.TIME_UNIT.toString())) {
						timeUnit = TimeUnit.valueOf((String)columns.get(1).get(row));
					}
					else if (label.equalsIgnoreCase(ColumnNames.POINTS_LOST_PER_TIME_UNIT.toString())) {
						try {
							pointsLostPerTimeUnit = Double.parseDouble( (String)columns.get(1).get(row));
						}
						catch (NumberFormatException e) {
							
						}
					}

				}
			}
			if (timeUnit == null || pointsLostPerTimeUnit <= 0.0 || timeUnit == TimeUnit.NONE ) {
				Rubric.showLoadError("Missing data for entry: \"" + entryName + "\"");
				timeUnit = TimeUnit.NONE;
			}

		}
	}

}
