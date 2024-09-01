/*
 * Mars Simulation Project
 * PowerMode.java
 * @date 2023-05-25
 * @author stpa
 */

package com.mars_sim.core.structure.building.utility.power;

import com.mars_sim.core.tool.Msg;

public enum PowerMode {

	FULL_POWER (Msg.getString("PowerMode.fullPower")), //$NON-NLS-1$
	LOW_POWER (Msg.getString("PowerMode.lowPower")), //$NON-NLS-1$
	NO_POWER (Msg.getString("PowerMode.noPower")); //$NON-NLS-1$

	private String name;

	/** hidden constructor. */
	private PowerMode(String name) {
		this.name = name;
	}

	/** gives back an internationalized {@link String} for display in user interface. */
	public String getName() {
		return this.name;
	}
}
