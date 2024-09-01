/*
 * Mars Simulation Project
 * UnitType.java
 * @date 2023-06-05
 * @author stpa
 */

package com.mars_sim.core;

import com.mars_sim.core.tool.Msg;

public enum UnitType {

	OUTER_SPACE		("UnitType.outerSpace"),
	MARS			("UnitType.mars"),
	MOON			("UnitType.moon"),
	SETTLEMENT 		("UnitType.settlement"),
	PERSON 			("UnitType.person"),
	VEHICLE 		("UnitType.vehicle"),
//	EQUIPMENT 		("UnitType.equipment"),
	CONTAINER		("UnitType.container"),
	EVA_SUIT 		("UnitType.evasuit"),
	ROBOT 			("UnitType.robot"),
	BUILDING 		("UnitType.building"),
	CONSTRUCTION 	("UnitType.construction");

	private String name;

	/** hidden constructor. */
	private UnitType(String msgKey) {
		this.name = Msg.getString(msgKey);
	}

	public String getName() {
		return this.name;
	}
}
