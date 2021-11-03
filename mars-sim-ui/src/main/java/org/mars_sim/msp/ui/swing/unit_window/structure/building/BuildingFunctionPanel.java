/**
 * Mars Simulation Project
 * BuildingFunctionPanel.java
 * @date 2021-10-06
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure.building;

import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;

/**
 * The BuildingFunctionPanel class is a panel representing a function for a
 * settlement building.
 */
@SuppressWarnings("serial")
public abstract class BuildingFunctionPanel extends JPanel {

	/** The building this panel is for. */
	protected Building building;
	/** The main desktop. */
	protected MainDesktopPane desktop;

	/**
	 * Constructor.
	 * 
	 * @param building The building this panel is for.
	 * @param desktop  The main desktop.
	 */
	public BuildingFunctionPanel(Building building, MainDesktopPane desktop) {
		// User JPanel constructor
		super();

		// Initialize data members
		this.building = building;
		this.desktop = desktop;

		Border border = new MarsPanelBorder();
		Border margin = new EmptyBorder(2, 2, 2, 2);
		setBorder(new CompoundBorder(border, margin));
	}

	/**
	 * Update this panel.
	 */
	public abstract void update();
}
