/*
 * Mars Simulation Project
 * VehicleGarage.java
 * @date 2022-09-21
 * @author Scott Davis
 */
package com.mars_sim.core.structure.building.function;

import java.util.Iterator;

import com.mars_sim.core.map.location.LocalPosition;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.FunctionSpec;

/**
 * The VehicleGarage class is a building function for parking vehicles 
 * and performing maintenance of vehicles.
 */
public class VehicleGarage
extends VehicleMaintenance {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 * 
	 * @param building the building the function is for.
	 * @param spec Defines the Function details
	 */
	public VehicleGarage(Building building, FunctionSpec spec) {
		// Call VehicleMaintenance constructor.
		super(FunctionType.VEHICLE_MAINTENANCE, spec, building);

		for (LocalPosition loc : buildingConfig.getParkingLocations(building.getBuildingType())) {
			addParkingLocation(loc);
		}
		
		for (LocalPosition loc : buildingConfig.getDroneLocations(building.getBuildingType())) {
			addFlyerLocation(loc);
		}
	}

	/**
	 * Constructor for unit test.
	 * 
	 * @param building the building the function is for.
	 * @param parkingLocations the parking locations.
	 */
	public VehicleGarage(Building building, LocalPosition[] parkingLocations) {
		// Call VehicleMaintenance constructor.
		super(FunctionType.VEHICLE_MAINTENANCE, null, building);
		
		for (LocalPosition parkingLocation : parkingLocations) {
			addParkingLocation(parkingLocation);
		}
	}

	/**
	 * Gets the value of the function for a named building.
	 * 
	 * @param buildingName the building name.
	 * @param newBuilding true if adding a new building.
	 * @param settlement the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String buildingName, boolean newBuilding,
			Settlement settlement) {

		// Demand is one ground vehicle capacity for every ground vehicles.
		double demand = settlement.getOwnedVehicleNum();

		double supply = 0D;
		boolean removedBuilding = false;
		Iterator<Building> i = settlement.getBuildingManager().getBuildingSet(FunctionType.VEHICLE_MAINTENANCE).iterator();
		while (i.hasNext()) {
			Building building = i.next();
			if (!newBuilding && building.getBuildingType().equalsIgnoreCase(buildingName) && !removedBuilding) {
				removedBuilding = true;
			}
			else {
				VehicleGarage maintFunction = building.getVehicleParking();
				double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
				supply += maintFunction.getVehicleCapacity() * wearModifier;
			}
		}

		double vehicleCapacityValue = demand / (supply + 1D);

		double vehicleCapacity = buildingConfig.getParkingLocations(buildingName).size()
					+ buildingConfig.getDroneLocations(buildingName).size() + 1.0;

		return vehicleCapacity * vehicleCapacityValue;
	}
}
