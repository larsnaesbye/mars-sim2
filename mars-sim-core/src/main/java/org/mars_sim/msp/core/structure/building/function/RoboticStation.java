/*
 * Mars Simulation Project
 * RoboticStation.java
 * @date 2021-10-21
 * @author Manny Kung
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.time.ClockPulse;

/**
 * The RoboticStation class is a building function for a Robotic Station.
 */
public class RoboticStation extends Function implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final Logger logger = Logger.getLogger(LifeSupport.class.getName());

	public final static double POWER_USAGE_PER_ROBOT = 1D; // in kW

	private int slots;
	private int sleepers;
	private int occupantCapacity;

//	private double powerRequired;

	private Collection<Robot> robotOccupants;

	/**
	 * Constructor
	 * 
	 * @param building the building this function is for.
	 * @throws BuildingException if error in constructing function.
	 */
	public RoboticStation(Building building) {
		// Call Function constructor.
		super(FunctionType.ROBOTIC_STATION, building);

		robotOccupants = new ConcurrentLinkedQueue<Robot>();
		// Set occupant capacity.
		occupantCapacity = buildingConfig.getFunctionCapacity(building.getBuildingType(), FunctionType.LIFE_SUPPORT);
//		powerRequired = buildingConfig.getLifeSupportPowerRequirement(building.getBuildingType());
		// this.occupantCapacity = occupantCapacity;
		// this.powerRequired = powerRequired;

		slots = buildingConfig.getFunctionCapacity(building.getBuildingType(), FunctionType.ROBOTIC_STATION);
	}

	/**
	 * Gets the value of the function for a named building.
	 * 
	 * @param buildingName the building name.
	 * @param newBuilding  true if adding a new building.
	 * @param settlement   the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String buildingName, boolean newBuilding, Settlement settlement) {

		// Demand is one stations for every robot
		double demand = settlement.getNumBots() * 1D;

		double supply = 0D;
		boolean removedBuilding = false;
		Iterator<Building> i = settlement.getBuildingManager().getBuildings(FunctionType.ROBOTIC_STATION).iterator();
		while (i.hasNext()) {
			Building building = i.next();
			if (!newBuilding && building.getBuildingType().equalsIgnoreCase(buildingName) && !removedBuilding) {
				removedBuilding = true;
			} else {
				RoboticStation station = building.getRoboticStation();
				double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
				supply += station.slots * wearModifier;
			}
		}

		double stationCapacityValue = demand / (supply + 1D);
		int stationCapacity = buildingConfig.getFunctionCapacity(buildingName, FunctionType.ROBOTIC_STATION);
		return stationCapacity * stationCapacityValue;
	}

	/**
	 * Gets the number of slots in the living accommodations.
	 * 
	 * @return number of slots.
	 */
	public int getSlots() {
		return slots;
	}

	/**
	 * Gets the number of robots sleeping in the stations.
	 * 
	 * @return number of robots
	 */
	public int getSleepers() {
		return sleepers;
	}

	/**
	 * Adds a sleeper to a station.
	 * 
	 * @throws BuildingException if stations are already in use.
	 */
	public void addSleeper() {
		sleepers++;
		if (sleepers > slots) {
			sleepers = slots;
			throw new IllegalStateException("All slots are full.");
		}
	}

	/**
	 * Removes a sleeper from a station.
	 * 
	 * @throws BuildingException if no sleepers to remove.
	 */
	public void removeSleeper() {
		sleepers--;
		if (sleepers < 0) {
			sleepers = 0;
			throw new IllegalStateException("Slots are empty.");
		}
	}

	/**
	 * Calculate the power for robots.
	 * 
	 * @param time amount of time passing (millisols)
	 * @throws Exception if error in power usage.
	 */
	private void powerUsage(double millisols) {
//        Settlement settlement = getBuilding().getBuildingManager()
//                .getSettlement();
//        double energyPerRobot = POWER_USAGE_PER_ROBOT * millisols * SECONDS_IN_MILLISOL;
//        double energyUsageSettlement = energyPerRobot * settlement.getNumBots();
//        double buildingProportionCap = (double) slots / (double) settlement.getRobotCapacity();
//        double energyUsageBuilding = energyUsageSettlement * buildingProportionCap;

//        Inventory inv = getBuilding().getSettlementInventory();
//        AmountResource water = AmountResource.findAmountResource(org.mars_sim.msp.core.LifeSupport.WATER);
//        double waterUsed = powerUsageBuilding;
//        double waterAvailable = inv.getAmountResourceStored(water, false);
//        inv.addDemandTotalRequest(water);
//        if (waterUsed > waterAvailable)
//            waterUsed = waterAvailable;
//        inv.retrieveAmountResource(water, waterUsed);
//       	inv.addDemandAmount(water, waterUsed);
//
//        AmountResource wasteWater = AmountResource
//                .findAmountResource("waste water");
//        double wasteWaterProduced = waterUsed;
//        double wasteWaterCapacity = inv.getAmountResourceRemainingCapacity(
//                wasteWater, false, false);
//        if (wasteWaterProduced > wasteWaterCapacity)
//            wasteWaterProduced = wasteWaterCapacity;
//        inv.storeAmountResource(wasteWater, wasteWaterProduced, false);
//        inv.addSupplyAmount(wasteWater, wasteWaterProduced);
	}

	/**
	 * Time passing for the building.
	 * 
	 * @param time amount of time passing (in millisols)
	 * @throws BuildingException if error occurs.
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		boolean valid = isValid(pulse);
		if (valid) {
//			// Make sure all occupants are actually in settlement inventory.
//			// If not, remove them as occupants.
//			if (robotOccupants != null && robotOccupants.size() > 0) {
//				Iterator<Robot> ii = robotOccupants.iterator();
//				while (ii.hasNext()) {
//					if (!building.getInventory().containsUnit(ii.next()))
//						ii.remove();
//				}
//			}
		}
		return valid;
	}

	@Override
	public double getMaintenanceTime() {
		return slots * 7D;
	}

	/**
	 * Gets the building's capacity for supporting occupants.
	 * 
	 * @return number of inhabitants.
	 */
	public int getOccupantCapacity() {
		return occupantCapacity;
	}

	/**
	 * Gets a collection of robotOccupants in the building.
	 * 
	 * @return collection of robotOccupants
	 */
	public Collection<Robot> getRobotOccupants() {
		return new ConcurrentLinkedQueue<Robot>(robotOccupants);
	}

	public int getRobotOccupantNumber() {
		return robotOccupants.size();
	}

	/**
	 * Gets the available occupancy room.
	 * 
	 * @return occupancy room
	 */
	public int getAvailableOccupancy() {
		int available = occupantCapacity - getRobotOccupantNumber();
        return Math.max(available, 0);
	}

	/**
	 * Checks if the building contains a particular unit.
	 * 
	 * @return true if unit is in building.
	 */
	public boolean containsRobotOccupant(Robot robot) {
		return robotOccupants.contains(robot);

	}

	/**
	 * Adds a robot to the building. Note: robot capacity can be exceeded
	 * 
	 * @param robot new robot to add to building.
	 * @throws BuildingException if robot is already building occupant.
	 */
	public void addRobot(Robot robot) {
		if (!robotOccupants.contains(robot)) {
			// Remove robot from any other inhabitable building in the settlement.
			Iterator<Building> i = getBuilding().getBuildingManager().getBuildings().iterator();
			while (i.hasNext()) {
				Building building = i.next();
				if (building.hasFunction(FunctionType.ROBOTIC_STATION)) {
					BuildingManager.removeRobotFromBuilding(robot, building);
//					building.getRoboticStation().removeRobot(robot);
				}
			}

			// Add robot to this building.
			logger.finest("Adding " + robot + " to " + getBuilding() + " robotic station.");
			robotOccupants.add(robot);
		} else {
			throw new IllegalStateException("This robot is already in this building.");
		}
	}

	/**
	 * Removes a robot from the building.
	 * 
	 * @param occupant the robot to remove from building.
	 * @throws BuildingException if robot is not building occupant.
	 */
	public void removeRobot(Robot robot) {
		if (robotOccupants.contains(robot)) {
			robotOccupants.remove(robot);
			logger.finest("Removing " + robot + " from " + getBuilding() + " robotic station.");
		} else {
			throw new IllegalStateException("The robot is not in this building.");
		}
	}

	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		super.destroy();
		robotOccupants.clear();
		robotOccupants = null;
	}
}
