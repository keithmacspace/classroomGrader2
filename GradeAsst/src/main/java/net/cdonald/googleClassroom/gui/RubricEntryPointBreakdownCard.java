package net.cdonald.googleClassroom.gui;

import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


import net.cdonald.googleClassroom.model.RubricEntry;

public class RubricEntryPointBreakdownCard implements RubricEntryDialogCardInterface {
	private RubricEntryPointBreakdownTable pointBreakdownTable;
	private boolean isActive;
	private RubricElementDialog dialogOwner;
	
	public RubricEntryPointBreakdownCard(RubricElementDialog dialogOwner) {
		this.dialogOwner = dialogOwner;
		pointBreakdownTable = new RubricEntryPointBreakdownTable(true);
		JPanel wrapperPanel = new JPanel();
		wrapperPanel.setLayout(new BorderLayout());
		wrapperPanel.setBorder(BorderFactory.createTitledBorder("Partial Credit Points"));
		wrapperPanel.add(new JScrollPane(pointBreakdownTable), BorderLayout.CENTER);
		dialogOwner.addAutomationCard(wrapperPanel, RubricEntry.AutomationTypes.NONE);
	}
	
	@Override
	public void addItems() {
		isActive = true;
	}

	@Override
	public boolean isActive() {		
		return isActive;
	}

	@Override
	public void removeItems() {
		pointBreakdownTable.stopEditing();
		isActive = false;
	}

	@Override
	public void referenceSourceEnabled(boolean enable) {
	}
	
	@Override
	public void saving() {
		pointBreakdownTable.stopEditing();
	}
	
	@Override
	public void rubricSet() {
		pointBreakdownTable.setAssociatedEntry(dialogOwner.getRubricToModify());
	}

}
