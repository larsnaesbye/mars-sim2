/**
 * Mars Simulation Project
 * UnitType.java
 * @version 3.07 2015-01-21
 * @author stpa
 */

package org.mars_sim.msp.core;

public enum UnitType {

	SETTLEMENT ("UnitType.settlement"),
	PERSON ("UnitType.person"),
	VEHICLE ("UnitType.vehicle"),
	EQUIPMENT ("UnitType.equipment"),
	ROBOT ("UnitType.robot");

	private String msgKey;

	/** hidden constructor. */
	private UnitType(String msgKey) {
		this.msgKey = msgKey;
	}

	public String getMsgKey() {
		return this.msgKey;
	}
}
