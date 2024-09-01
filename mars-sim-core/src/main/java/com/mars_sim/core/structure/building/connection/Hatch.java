/**
 * Mars Simulation Project
 * Hatch.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package com.mars_sim.core.structure.building.connection;

import com.mars_sim.core.map.location.LocalBoundedObject;
import com.mars_sim.core.map.location.LocalPosition;
import com.mars_sim.core.structure.building.Building;

/**
 * A hatch on one side of a building connection.
 */
public class Hatch implements LocalBoundedObject, InsidePathLocation {

	private static final long serialVersionUID = 1L;
	
	// Static members.
	public static final double LENGTH = .6D;
	public static final double WIDTH = 2.76D;

	// Data members
	private Building building;
	private BuildingConnector connector;
	private LocalPosition pos;
	private double facing;

	/**
	 * Constructor.
	 * 
	 * @param building  the building the hatch is connected to.
	 * @param connector the building connector for the hatch.
	 * @param pos       The position of the center point of the hatch.
	 * @param facing    The facing of the hatch (degrees).
	 */
	public Hatch(Building building, BuildingConnector connector, LocalPosition pos, double facing) {
		this.building = building;
		this.connector = connector;
		this.pos = pos;
		this.facing = facing;
	}

	/**
	 * Gets the building the hatch is connected to.
	 * 
	 * @return building.
	 */
	public Building getBuilding() {
		return building;
	}

	/**
	 * Gets the building connector for the hatch.
	 * 
	 * @return connector.
	 */
	public BuildingConnector getBuildingConnector() {
		return connector;
	}

	@Override
	public LocalPosition getPosition() {
		return pos;
	}

	@Override
	public double getWidth() {
		return WIDTH;
	}

	@Override
	public double getLength() {
		return LENGTH;
	}

	@Override
	public double getFacing() {
		return facing;
	}

	@Override
	public boolean equals(Object other) {

		boolean result = false;

		if (other instanceof Hatch) {
			Hatch otherHatch = (Hatch) other;

			if (pos.equals(otherHatch.getPosition())
					&& (facing == otherHatch.getFacing())) {
				result = true;
			}
		}

		return result;
	}

	/**
	 * Gets the hash code for this object.
	 *
	 * @return hash code.
	 */
	@Override
	public int hashCode() {
		return super.hashCode();
	}
	
	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		building = null;
		connector = null;
	}
}
