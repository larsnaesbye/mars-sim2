/**
 * Mars Simulation Project
 * Recreation.java
 * @version 2.75 2004-03-31
 * @author Scott Davis
 */
package org.mars_sim.msp.simulation.structure.building.function;

import java.io.Serializable;
import org.mars_sim.msp.simulation.structure.building.*;

/**
 * The Recreation class is a building function for recreation.
 */
public class Recreation extends Function implements Serializable {
        
	public static final String NAME = "Recreation";
	
	/**
	 * Constructor
	 * @param building the building this function is for.
	 */
	public Recreation(Building building) {
		// Use Function constructor.
		super(NAME, building);
	}
	
	/**
	 * Time passing for the building.
	 * @param time amount of time passing (in millisols)
	 * @throws BuildingException if error occurs.
	 */
	public void timePassing(double time) throws BuildingException {}
	
	/**
	 * Gets the amount of power required when function is at full power.
	 * @return power (kW)
	 */
	public double getFullPowerRequired() {
		return 0D;
	}
	
	/**
	 * Gets the amount of power required when function is at power down level.
	 * @return power (kW)
	 */
	public double getPowerDownPowerRequired() {
		return 0D;
	}
}