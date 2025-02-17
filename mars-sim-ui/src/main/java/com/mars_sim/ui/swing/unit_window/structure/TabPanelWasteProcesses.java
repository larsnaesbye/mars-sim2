/*
 * Mars Simulation Project
 * TabPanelWasteProcesses.java
 * @date 2022-07-09
 * @author Manny Kung
 */
package com.mars_sim.ui.swing.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.mars_sim.core.Unit;
import com.mars_sim.core.resourceprocess.ResourceProcess;
import com.mars_sim.core.structure.OverrideType;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.WasteProcessing;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.unit_window.TabPanel;
import com.mars_sim.ui.swing.unit_window.structure.building.ResourceProcessPanel;

/**
 * A tab panel for displaying all of the waste processes in a settlement.
 */
@SuppressWarnings("serial")
public class TabPanelWasteProcesses extends TabPanel {
	
	private static final String RECYCLE_ICON = "recycle";

	/** The Settlement instance. */
	private Settlement settlement;
	private ResourceProcessPanel processPanel;

	/**
	 * Constructor.
	 * 
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelWasteProcesses(Unit unit, MainDesktopPane desktop) {

		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelWasteProcesses.title"), //$NON-NLS-1$
			ImageLoader.getIconByName(RECYCLE_ICON),
			Msg.getString("TabPanelWasteProcesses.title"), //$NON-NLS-1$
			unit, desktop
		);

		settlement = (Settlement) unit;
	}
	
	@Override
	protected void buildUI(JPanel content) {
		BuildingManager mgr = settlement.getBuildingManager();
		Map<Building, List<ResourceProcess>> processes = new HashMap<>();
		for (Building building : mgr.getBuildings(FunctionType.WASTE_PROCESSING)) {
			WasteProcessing processing = building.getWasteProcessing();
			processes.put(building, processing.getProcesses());
		}

		// Prepare process list panel.n
		processPanel = new ResourceProcessPanel(processes, getDesktop());
		processPanel.setPreferredSize(new Dimension(160, 120));
		content.add(processPanel, BorderLayout.CENTER);

		// Create override check box panel.
		JPanel overrideCheckboxPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		content.add(overrideCheckboxPane, BorderLayout.NORTH);

		// Create override check box.
		JCheckBox overrideCheckbox = new JCheckBox(Msg.getString("TabPanelWasteProcesses.checkbox.overrideWasteProcessToggling")); //$NON-NLS-1$
		overrideCheckbox.setToolTipText(Msg.getString("TabPanelWasteProcesses.tooltip.overrideWasteProcessToggling")); //$NON-NLS-1$
		overrideCheckbox.addActionListener(e ->
			setWasteProcessesOverride(overrideCheckbox.isSelected())
		);
		overrideCheckbox.setSelected(settlement.getProcessOverride(OverrideType.WASTE_PROCESSING));
		overrideCheckboxPane.add(overrideCheckbox);
	}


	@Override
	public void update() {
		processPanel.update();
	}

	/**
	 * Sets the settlement waste process override flag.
	 * @param override the waste process override flag.
	 */
	private void setWasteProcessesOverride(boolean override) {
		settlement.setProcessOverride(OverrideType.WASTE_PROCESSING, override);
	}
}
