/*
 * Mars Simulation Project
 * TabPanelVehicles.java
 * @date 2022-07-09
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.unit_window.structure;


import java.util.Collection;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.unit_window.TabPanel;
import com.mars_sim.ui.swing.unit_window.UnitListPanel;

/** 
 * The TabPanelVehicles is a tab panel for parked vehicles and vehicles on mission.
 */
@SuppressWarnings("serial")
public class TabPanelVehicles extends TabPanel {
	
	private static final String SUV_ICON ="vehicle";
	
	/** The Settlement instance. */
	private Settlement settlement;
	
	private UnitListPanel<Vehicle> parkedVehicles;
	private UnitListPanel<Vehicle> missionVehicles;
	
	/**
	 * Constructor.
	 * @param unit the unit to display
	 * @param desktop the main desktop.
	 */
	public TabPanelVehicles(Settlement unit, MainDesktopPane desktop) { 
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelVehicles.title"), //$NON-NLS-1$
			ImageLoader.getIconByName(SUV_ICON),
			Msg.getString("TabPanelVehicles.title"), //$NON-NLS-1$
			desktop
		);

		settlement = unit;
	}
	
	@Override
	protected void buildUI(JPanel content) {
		JPanel vehiclePanel = new JPanel();
		vehiclePanel.setLayout(new BoxLayout(vehiclePanel, BoxLayout.Y_AXIS));
		content.add(vehiclePanel);

		// Parked Vehicles
		MainDesktopPane desktop = getDesktop();
		parkedVehicles = new UnitListPanel<>(desktop) {
			@Override
			protected Collection<Vehicle> getData() {
				return settlement.getParkedGaragedVehicles();
			}
		};
		addBorder(parkedVehicles, Msg.getString("TabPanelVehicles.parked.vehicles"));
		vehiclePanel.add(parkedVehicles);

		// Mission vehicles
		missionVehicles = new UnitListPanel<>(desktop) {
			@Override
			protected Collection<Vehicle> getData() {
				return settlement.getMissionVehicles();
			}
		};
		addBorder(missionVehicles, Msg.getString("TabPanelVehicles.mission.vehicles"));
		vehiclePanel.add(missionVehicles);
	}

	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {
		// Update vehicle list
		parkedVehicles.update();
		missionVehicles.update();
	}
	
	/**
     * Prepare object for garbage collection.
     */
	@Override
    public void destroy() {
		super.destroy();
    	parkedVehicles = null;
    	missionVehicles = null;
    }
}