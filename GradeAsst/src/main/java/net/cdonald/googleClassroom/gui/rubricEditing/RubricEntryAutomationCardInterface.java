package net.cdonald.googleClassroom.gui.rubricEditing;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JPanel;

import net.cdonald.googleClassroom.model.RubricEntry;

public abstract class RubricEntryAutomationCardInterface  extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 153883969297856883L;
	public abstract void saving();
	public abstract void testSourceChanged(Map<String, List<Method>> possibleMethodMap);	
	public abstract RubricEntry.AutomationTypes getAutomationType();
	public abstract void setEnableEditing(boolean enable);
	public void referenceSourceChanged(Map<String, Set<String>> methodMap) {}
	public RubricEntryAutomationCardInterface() {
		super();
	}
	public String getDescription() {
		return "Automated.  After you run rubrics, if no value is filled in for a student, check this tab for more information";		
	}

}
