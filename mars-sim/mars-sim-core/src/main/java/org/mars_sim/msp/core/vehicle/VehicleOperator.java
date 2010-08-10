/*
 * Mars Simulation Project
 * VehicleOperator.java
 * @version 3.00 2010-08-10
 * @author Scott Davis
 */

package org.mars_sim.msp.core.vehicle;

/**
 * An entity capable of operating a vehicle.
 * Could be a person or an AI computer.
 */
public interface VehicleOperator {

	/**
	 * Checks if the vehicle operator is fit for operating the vehicle.
	 * @return true if vehicle operator is fit.
	 */
	public boolean isFitForOperatingVehicle();
	
	/**
	 * Gets the name of the vehicle operator
	 * @return vehicle operator name.
	 */
	public String getOperatorName();
}