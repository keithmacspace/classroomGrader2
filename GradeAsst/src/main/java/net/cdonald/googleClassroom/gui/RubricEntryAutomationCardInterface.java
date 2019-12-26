package net.cdonald.googleClassroom.gui;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;

import net.cdonald.googleClassroom.model.RubricEntry;

public abstract class RubricEntryAutomationCardInterface  extends JPanel {
	public abstract void saving();
	public abstract void testSourceChanged(Map<String, List<Method>> possibleMethodMap);
	public abstract String getDescription();
	public abstract RubricEntry.AutomationTypes getAutomationType();
	public abstract void setEnableEditing(boolean enable);
	public RubricEntryAutomationCardInterface() {
		super();
	}
}
