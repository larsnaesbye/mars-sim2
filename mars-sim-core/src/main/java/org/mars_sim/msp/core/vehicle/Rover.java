/*
 * Mars Simulation Project
 * Rover.java
 * @date 2021-10-16
 * @author Scott Davis
 */

package org.mars_sim.msp.core.vehicle;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.LifeSupportInterface;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.data.UnitSet;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionMember;
import org.mars_sim.msp.core.person.ai.mission.MissionType;
import org.mars_sim.msp.core.person.ai.mission.VehicleMission;
import org.mars_sim.msp.core.person.ai.task.LoadingController;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Airlock;
import org.mars_sim.msp.core.structure.CompositionOfAir;
import org.mars_sim.msp.core.structure.Lab;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.ClockPulse;

/**
 * The Rover class represents the rover type of ground vehicle. It contains
 * information about the rover.
 */
public class Rover extends GroundVehicle implements Crewable, LifeSupportInterface, Airlockable, Medical, Towing {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
	// default logger.
	private static final SimLogger logger = SimLogger.getLogger(Rover.class.getName());
	
	/** The fuel range modifier. */
	public static final double FUEL_RANGE_FACTOR = 0.95;
	/** The mission range modifier. */  
	public static final double MISSION_RANGE_FACTOR = 1.9;
	/** The reference small amount of resource. */
	public static final double SMALL_AMOUNT = 0.1;
	/** The amount of work time to perform maintenance (millisols) */
	public static final double MAINTENANCE_WORK_TIME = 100D;

	// Note: 34 kPa (5 psi) is chosen for the composition of oxygen inside a settlement at 58.8%.
	/** Rate of change of temperature in degree celsius. */
	private static final double RATE_OF_CHANGE_OF_C_PER_MILLISOL = 0.0005D; 
	/** Rate of change of air pressure (kPa). */
	private static final double RATE_OF_CHANGE_OF_kPa_PER_MILLISOL = 0.0005D; 
	/** The factitious temperature flow [deg C per millisols] when connected to a settlement */
	private static final double TEMPERATURE_FLOW_PER_MILLISOL = 0.01D; 
	/** The factitious air pressure flow [kPa per millisols] when connected to a settlement */
	private static final double AIR_PRESSURE_FLOW_PER_MILLISOL = 0.01D; 
	
	/** Normal air pressure (kPa). */
	private static final double NORMAL_AIR_PRESSURE = 17; //20.7; //34.7D; 
	/** Normal temperature (celsius). */
	private static final double NORMAL_TEMP = 22.5D;
	
	public static final int OXYGEN = ResourceUtil.oxygenID; 
	public static final int NITROGEN = ResourceUtil.nitrogenID;
	public static final int WATER = ResourceUtil.waterID;
	public static final int METHANE = ResourceUtil.methaneID; 
	public static final int FOOD = ResourceUtil.foodID;
	
	public static final int FOOD_WASTE = ResourceUtil.foodWasteID;
	public static final int SOLID_WASTE = ResourceUtil.solidWasteID;
	public static final int TOXIC_WASTE = ResourceUtil.toxicWasteID;
	public static final int GREY_WATER = ResourceUtil.greyWaterID;
	public static final int BLACK_WATER = ResourceUtil.blackWaterID;
	
	public static final int ROCK_SAMPLES = ResourceUtil.rockSamplesID;
	public static final int ICE = ResourceUtil.iceID;
	
	// Data members
	/** The rover's capacity for crew members. */
	private int crewCapacity = 0;
	/** The rover's capacity for robot crew members. */
	private int robotCrewCapacity = 0;
	
	/** The minimum required O2 partial pressure. At 11.94 kPa (1.732 psi)  */
	private double min_o2_pressure;
	/** The full O2 partial pressure if at full tank. */
	private double fullO2PartialPressure;
	/** The nominal mass of O2 required to maintain the nominal partial pressure of 20.7 kPa (3.003 psi)  */
	private double massO2NominalLimit;
	/** The minimum mass of O2 required to maintain right above the safety limit of 11.94 kPa (1.732 psi)  */
	private double massO2MinimumLimit;
	/** The capacity of O2 in this rover (kg)  */
	private double oxygenCapacity;
	/** The rover's internal air pressure. */
	private double airPressure = 0; //NORMAL_AIR_PRESSURE;
	/** The rover's internal temperature. */
	private double temperature = 0; //NORMAL_TEMP;
	/** The rover's cargo capacity */
	private double cargoCapacity = 0;
	/** The rover's total crew internal volume. */
	private double cabinAirVolume;

	/** The rover's lab activity spots. */
	private List<Point2D> labActivitySpots;
	/** The rover's sick bay activity spots. */
	private List<Point2D> sickBayActivitySpots;
	
	/** The rover's occupants. */
	private Set<Person> occupants;
	/** The rover's robot occupants. */
	private Set<Robot> robotOccupants;
	
	/** The rover's airlock. */
	private Airlock airlock;
	/** The rover's lab. */
	private Lab lab;
	/** The rover's sick bay. */
	private SickBay sickbay;
	/** The vehicle the rover is currently towing. */
	private Vehicle towedVehicle;
	/** The light utility vehicle currently docked at the rover. */
	private LightUtilityVehicle luv;	
	
	/**
	 * Constructs a Rover object at a given settlement
	 * 
	 * @param name        the name of the rover
	 * @param type the configuration type of the vehicle.
	 * @param settlement  the settlement the rover is parked at
	 */
	public Rover(String name, String type, Settlement settlement) {
		// Use GroundVehicle constructor
		super(name, type, settlement, MAINTENANCE_WORK_TIME);

		occupants = new UnitSet<>();
		robotOccupants = new UnitSet<>();
		
		// Set crew capacity
		VehicleConfig vehicleConfig = simulationConfig.getVehicleConfiguration();
		crewCapacity = vehicleConfig.getCrewSize(type);
		robotCrewCapacity = crewCapacity;
	
		// Gets the estimated cabin compartment air volume.
		cabinAirVolume = vehicleConfig.getEstimatedAirVolume(type);

		Map<String, Double> capacities = vehicleConfig.getCargoCapacity(type);
		oxygenCapacity = capacities.get(LifeSupportInterface.OXYGEN);
		
		min_o2_pressure = SimulationConfig.instance().getPersonConfig().getMinSuitO2Pressure();
		fullO2PartialPressure = Math.round(CompositionOfAir.KPA_PER_ATM * oxygenCapacity / CompositionOfAir.O2_MOLAR_MASS 
				* CompositionOfAir.R_GAS_CONSTANT / cabinAirVolume*1_000.0)/1_000.0;
		massO2MinimumLimit = Math.round(min_o2_pressure / fullO2PartialPressure * oxygenCapacity*10_000.0)/10_000.0;
		massO2NominalLimit =Math.round( NORMAL_AIR_PRESSURE / min_o2_pressure * massO2MinimumLimit*10_000.0)/10_000.0;
		 
//		logger.config(type + " : full tank O2 partial pressure is " + fullO2PartialPressure + " kPa");
//		logger.config(type + " : minimum mass limit of O2 (above the safety limit) is " + massO2MinimumLimit  + " kg");
//		logger.config(type + " : nomimal mass limit of O2 is " + massO2NominalLimit  + " kg");
			
		// Construct sick bay.
		if (vehicleConfig.hasSickbay(type)) {
			sickbay = new SickBay(this, vehicleConfig.getSickbayTechLevel(type),
					vehicleConfig.getSickbayBeds(type));

			// Initialize sick bay activity spots.
			sickBayActivitySpots = new ArrayList<Point2D>(vehicleConfig.getSickBayActivitySpots(type));
		}

		// Construct lab.
		if (vehicleConfig.hasLab(type)) {
			lab = new MobileLaboratory(1, vehicleConfig.getLabTechLevel(type),
					vehicleConfig.getLabTechSpecialties(type));

			// Initialize lab activity spots.
			labActivitySpots = new ArrayList<Point2D>(vehicleConfig.getLabActivitySpots(type));
		}
		
		// Set rover terrain modifier
		if (type.equalsIgnoreCase(VehicleType.CARGO_ROVER.getName()))
			setTerrainHandlingCapability(2.5);
		else if (type.equalsIgnoreCase(VehicleType.EXPLORER_ROVER.getName()))
			setTerrainHandlingCapability(5D);
		else if (type.equalsIgnoreCase(VehicleType.TRANSPORT_ROVER.getName()))
			setTerrainHandlingCapability(2.5);
		
		// Create the rover's airlock.
		double airlockXLoc = vehicleConfig.getAirlockXLocation(type);
		double airlockYLoc = vehicleConfig.getAirlockYLocation(type);
		double airlockInteriorXLoc = vehicleConfig.getAirlockInteriorXLocation(type);
		double airlockInteriorYLoc = vehicleConfig.getAirlockInteriorYLocation(type);
		double airlockExteriorXLoc = vehicleConfig.getAirlockExteriorXLocation(type);
		double airlockExteriorYLoc = vehicleConfig.getAirlockExteriorYLocation(type);

		try {
			airlock = new VehicleAirlock(this, 2, airlockXLoc, airlockYLoc, airlockInteriorXLoc, airlockInteriorYLoc,
					airlockExteriorXLoc, airlockExteriorYLoc);
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Problem instantiating new vehicle air lock: ", e);	
		}
	}

	/**
	 * Sets the vehicle this rover is currently towing.
	 * 
	 * @param towedVehicle the vehicle being towed by this rover.
	 */
	public void setTowedVehicle(Vehicle towedVehicle) {
		if (this == towedVehicle)
			throw new IllegalArgumentException("Rover cannot tow itself.");
		
		if (towedVehicle != null) {
			// if towedVehicle is not null, it means this rover has just hooked up for towing the towedVehicle
			addStatus(StatusType.TOWING);
		}
		else {
			removeStatus(StatusType.TOWING);
		}
		
		this.towedVehicle = towedVehicle;
	}

	/**
	 * Gets the vehicle this rover is currently towing.
	 * 
	 * @return towed vehicle.
	 */
	public Vehicle getTowedVehicle() {
		return towedVehicle;
	}

	/**
	 * Is this rover towing another vehicle.
	 * 
	 * @return true or false
	 */
	public boolean isTowingAVehicle() {
        return towedVehicle != null;
	}

	/**
	 * Gets the number of crewmembers the vehicle can carry.
	 * 
	 * @return capacity
	 */
	public int getCrewCapacity() {
		return crewCapacity;
	}

	/**
	 * Gets the number of robot crewmembers the vehicle can carry.
	 * 
	 * @return capacity
	 */
	public int getRobotCrewCapacity() {
		return robotCrewCapacity;
	}

	/**
	 * Gets the current number of crewmembers.
	 * 
	 * @return number of crewmembers
	 */
	public int getCrewNum() {
		if (!getCrew().isEmpty())
			return occupants.size();
		return 0;
	}

	/**
	 * Gets the current number of crewmembers.
	 * 
	 * @return number of crewmembers
	 */
	public int getRobotCrewNum() {
		if (!getRobotCrew().isEmpty())
			return robotOccupants.size();
		return 0;
	}

	/**
	 * Gets a set of the robot crewmembers.
	 * 
	 * @return robot crewmembers
	 */
	public Set<Person> getCrew() {
		if (occupants == null || occupants.isEmpty())
			return new UnitSet<>();
		return occupants;
	}

	/**
	 * Gets a set of the robot crewmembers.
	 * 
	 * @return robot crewmembers
	 */
	public Set<Robot> getRobotCrew() {
		if (robotOccupants == null || robotOccupants.isEmpty())
			return new UnitSet<>();
		return robotOccupants;
	}

	/**
	 * Checks if person is a crewmember.
	 * 
	 * @param person the person to check
	 * @return true if person is a crewmember
	 */
	public boolean isCrewmember(Person person) {
		return occupants.contains(person);
	}

	/**
	 * Checks if robot is a crewmember.
	 * 
	 * @param robot the robot to check
	 * @return true if robot is a crewmember
	 */
	public boolean isRobotCrewmember(Robot robot) {
		return robotOccupants.contains(robot);
	}

	/**
	 * Adds a person as crewmember
	 * 
	 * @param person
	 * @param true if the person can be added
	 */
	public boolean addPerson(Person person) {
		if (!isCrewmember(person) && occupants.add(person)) {
			person.setContainerUnit(this);
			return true;
		}
		return false;
	}
	
	/**
	 * Removes a person as crewmember
	 * 
	 * @param person
	 * @param true if the person can be removed
	 */
	public boolean removePerson(Person person) {
		if (isCrewmember(person))
			return occupants.remove(person);
		return false;
	}
	
	/**
	 * Adds a robot as crewmember
	 * 
	 * @param robot
	 * @param true if the robot can be added
	 */
	public boolean addRobot(Robot robot) {
		if (!isRobotCrewmember(robot) && robotOccupants.add(robot)) {
			robot.setContainerUnit(this);
			return true;
		}
		return false;
	}
	
	/**
	 * Removes a robot as crewmember
	 * 
	 * @param robot
	 * @param true if the robot can be removed
	 */
	public boolean removeRobot(Robot robot) {
		if (isRobotCrewmember(robot))
			return robotOccupants.remove(robot);
		return false;
	}
	
	/**
	 * Returns true if life support is working properly and is not out of oxygen or
	 * water.
	 * 
	 * @return true if life support is OK
	 * @throws Exception if error checking life support.
	 */
	public boolean lifeSupportCheck() {
		// Note: need to draw the the hose connecting between the vehicle and the settlement to supply resources		
		if (isPluggedIn()) {
			if (haveStatusType(StatusType.TOWED) && !isInSettlement()) {
	
				double o2 = getTowingVehicle().getAmountResourceStored(OXYGEN);
				if (o2 < SMALL_AMOUNT) {
					logger.log(this, Level.WARNING, 60_000, 
						"No more oxygen.");
					return false;
				}
				
				else if (o2 <= massO2MinimumLimit) {
					logger.log(this, Level.WARNING, 60_000, 
							"Remaining oxygen was below the safety threshold (" 
									+ massO2MinimumLimit + " kg) ");
					return false;
				}
				
				if (getTowingVehicle().getAmountResourceStored(WATER) <= 0D) {
					logger.log(this, Level.WARNING, 60_000, 
							"Ran out of water.");
					return false;
				}
			}

			else if (getSettlement() != null)  {

				double o2 = getSettlement().getAmountResourceStored(OXYGEN);
				if (o2 < SMALL_AMOUNT) {
					logger.log(this, Level.WARNING, 60_000, 
						"No more oxygen.");
					return false;
				}
				
				else if (o2 <= massO2MinimumLimit) {
					logger.log(this, Level.WARNING, 60_000, 
							"Remaining oxygen was below the safety threshold (" 
									+ massO2MinimumLimit + " kg) ");
					return false;
				}
				
				if (getSettlement().getAmountResourceStored(WATER) <= 0D) {
					logger.log(this, Level.WARNING, 60_000, 
							"Ran out of water.");
					return false;
				}
			}

		}
		else {

			double o2 = getAmountResourceStored(OXYGEN);
			if (o2 < SMALL_AMOUNT) {
				logger.log(this, Level.WARNING, 60_000, 
					"No more oxygen.");
				return false;
			}
			
			else if (o2 <= massO2MinimumLimit) {
				logger.log(this, Level.WARNING, 60_000, 
						"Remaining oxygen was below the safety threshold (" 
								+ massO2MinimumLimit + " kg) ");
				return false;
			}
			
			if (getAmountResourceStored(WATER) <= 0D) {
				logger.log(this, Level.WARNING, 60_000, 
						"Ran out of water.");
				return false;
			}
		}

//		if (malfunctionManager.getOxygenFlowModifier() < 100D)
//			result = false;
//		if (malfunctionManager.getWaterFlowModifier() < 100D)
//			result = false;

		double p = getAirPressure();
		if (p > PhysicalCondition.MAXIMUM_AIR_PRESSURE || p <= min_o2_pressure) {
			logger.log(this, Level.WARNING, 60_000, 
					"Out-of-range O2 pressure at " + Math.round(p * 100.0D) / 100.0D 
					+ " kPa detected.");
			return false;
		}
		
		double t = getTemperature();
		if (t < Settlement.life_support_value[0][4] - Settlement.SAFE_TEMPERATURE_RANGE
				|| t > Settlement.life_support_value[1][4] + Settlement.SAFE_TEMPERATURE_RANGE) {
			logger.log(this, Level.WARNING, 10_000, 
					"Out-of-range overall temperature at " + Math.round(t * 100.0D) / 100.0D 
						+ " " + Msg.getString("temperature.sign.degreeCelsius") + " detected.");		
			return false;
		}

		return true;
	}

	/**
	 * Gets the number of people the life support can provide for.
	 * 
	 * @return the capacity of the life support system
	 */
	public int getLifeSupportCapacity() {
		return crewCapacity;
	}

	/**
	 * Is the rover connected to the settlement through hoses
	 * 
	 * @return true if yes
	 */
	public boolean isPluggedIn() {
		if (isInSettlement())
			return true;
		
		if (haveStatusType(StatusType.GARAGED))
			return true;

        return haveStatusType(StatusType.TOWED);
    }
	
	/**
	 * Gets oxygen from system.
	 * 
	 * @param amountRequested the amount of oxygen requested from system (kg)
	 * @return the amount of oxgyen actually received from system (kg)
	 * @throws Exception if error providing oxygen.
	 */
	public double provideOxygen(double amountRequested) {
		double oxygenTaken = amountRequested;
	
		Vehicle v = null;
		
		// NOTE: need to draw the the hose connecting between the vehicle and the settlement to supply resources
		if (isPluggedIn()) {
			if (haveStatusType(StatusType.TOWED) && !isInSettlement()) {
				v = getTowingVehicle();

				double oxygenLeft = v.getAmountResourceStored(OXYGEN);

				if (oxygenTaken > oxygenLeft)
					oxygenTaken = oxygenLeft;
				
				v.retrieveAmountResource(OXYGEN, oxygenTaken);
			}

			else {

				double oxygenLeft = getSettlement().getAmountResourceStored(OXYGEN);

				if (oxygenTaken > oxygenLeft)
					oxygenTaken = oxygenLeft;
				
				getSettlement().retrieveAmountResource(OXYGEN, oxygenTaken);
			}
		}
		
		else {

			double oxygenLeft = getAmountResourceStored(OXYGEN);

			if (oxygenTaken > oxygenLeft)
				oxygenTaken = oxygenLeft;
			
			retrieveAmountResource(OXYGEN, oxygenTaken);
		}

		return oxygenTaken; // * (malfunctionManager.getOxygenFlowModifier() / 100D);
	}

	/**
	 * Gets water from system.
	 * 
	 * @param amountRequested the amount of water requested from system (kg)
	 * @return the amount of water actually received from system (kg)
	 * @throws Exception if error providing water.
	 */
	public double provideWater(double amountRequested) {
		double waterTaken = amountRequested;
		
		Vehicle v = null;
		
		// Note: need to draw the the hose connecting between the vehicle and the settlement to supply resources
		
		if (isPluggedIn()) {
			if (haveStatusType(StatusType.TOWED) && !isInSettlement()) {
				v = getTowingVehicle();
				double waterLeft = v.getAmountResourceStored(WATER);

				if (waterTaken > waterLeft)
					waterTaken = waterLeft;

				v.retrieveAmountResource(WATER, waterTaken);
			}

			else {
		
				double waterLeft = getSettlement().getAmountResourceStored(WATER);

				if (waterTaken > waterLeft)
					waterTaken = waterLeft;

				getSettlement().retrieveAmountResource(WATER, waterTaken);
			}
		}
		else {
			double waterLeft = getAmountResourceStored(WATER);

			if (waterTaken > waterLeft)
				waterTaken = waterLeft;

			retrieveAmountResource(WATER, waterTaken);
		}

		return waterTaken; // * (malfunctionManager.getWaterFlowModifier() / 100D);
	}

	/**
	 * Gets the air pressure of the life support system.
	 * 
	 * @return air pressure (Pa)
	 */
	public double getAirPressure() {
		// Based on some pre-calculation, 
		// To supply a partial oxygen pressure of 20.7 kPa, one needs at least 0.3107 kg O2

		// With the minimum required O2 partial pressure of 11.94 kPa (1.732 psi), the minimum mass of O2 is 0.1792 kg 
		
		// Note : our target o2 partial pressure is now 17 kPa (not 20.7 kPa)
		// To supply 17 kPa O2, need 0.2552 kg O2

		double oxygenLeft = 0;
		
		if (!isInSettlement()) {	
			if (getTowingVehicle() != null) {
				oxygenLeft = getTowingVehicle().getAmountResourceStored(OXYGEN);
			}
			else
				oxygenLeft = getAmountResourceStored(OXYGEN);
		}
		else {
			oxygenLeft = getSettlement().getAmountResourceStored(OXYGEN);
		}
 
		if (oxygenLeft < SMALL_AMOUNT) {
			return 0;
		}
		
		else if (oxygenLeft < massO2NominalLimit) {
			// Assuming that we can maintain a constant oxygen partial pressure unless it falls below massO2NominalLimit
			
			double remainingMass = oxygenLeft;
			double pp = CompositionOfAir.KPA_PER_ATM * remainingMass / CompositionOfAir.O2_MOLAR_MASS * CompositionOfAir.R_GAS_CONSTANT / cabinAirVolume;
			logger.log(this, Level.WARNING, 60_000, 
					Math.round(oxygenLeft*100.0)/100.0
						+ " kg O2 left at partial pressure of " + Math.round(pp*100.0)/100.0 + " kPa.");
			return pp;
		}

//		Note: the outside ambient air pressure is weather.getAirPressure(getCoordinates());

		return NORMAL_AIR_PRESSURE;// * (malfunctionManager.getAirPressureModifier() / 100D);	
	}

	/**
	 * Gets the temperature of the life support system.
	 * 
	 * @return temperature (degrees C)
	 */
	public double getTemperature() {
		return temperature;	
	}

	/**
	 * Plugs in the rover and adjust the temperature 
	 * 
	 * @param time
	 */
	public void plugInTemperature(double time) {
		// TODO: need to draw the the hose connecting between the vehicle and the settlement to supply resources
		if (isPluggedIn()) {
			if (temperature > NORMAL_TEMP * 1.15 || temperature < NORMAL_TEMP * 0.85) {
				// Internal air pumps of a rover maintains the air pressure
				// TODO: need to model the power usage
				
				double p = 0;
				if (haveStatusType(StatusType.GARAGED))
					p = getGarage().getCurrentTemperature();
				else 
					p = getSettlement().getTemperature();// * (malfunctionManager.getTemperatureModifier() / 100D);
				double delta = temperature - p;
				if (delta > 5)
					delta = 5;
				else if (delta < -5)
					delta = -5;

				double result = temperature - delta * TEMPERATURE_FLOW_PER_MILLISOL * time;
					
				temperature = result;
			}
		}
	}
	
	
	/**
	 * Unplugs the rover and adjust the temperature 
	 * 
	 * @param time
	 */
	public void plugOffTemperature(double time) {
		if (temperature >= 0) {
			// if no one is occupying the rover, can power it off 
		
			// TODO : will need to the internal air composition/pressure of a vehicle 
			temperature -= RATE_OF_CHANGE_OF_C_PER_MILLISOL * time;
			if (temperature < 0)
				// but will use power to maintain the temperature at the minimum of zero deg C
				temperature = 0;	
		}
	}
	
	/**
	 * Adjust the air pressure of the rover
	 * 
	 * @param time
	 */
	public void plugInAirPressure(double time) {
		// TODO: need to draw the the hose connecting between the vehicle and the settlement to supply resources
		if (isPluggedIn()) {
			if (airPressure > NORMAL_AIR_PRESSURE * 1.15 || airPressure < NORMAL_AIR_PRESSURE * 0.85) {
				// Internal heat pump of a rover maintains the air pressure
				// TODO: need to model the power usage
				
				double p = 0;
				if (haveStatusType(StatusType.GARAGED))
					p = getGarage().getCurrentAirPressure();
				else 
					p = getSettlement().getAirPressure();// * (malfunctionManager.getAirPressureModifier() / 100D);
				
				double delta = airPressure - p;
				if (delta > 5)
					delta = 5;
				else if (delta < -5)
					delta = -5;
				
				double result = airPressure - delta * AIR_PRESSURE_FLOW_PER_MILLISOL * time;
				airPressure = result;
			}
		}
	}
		
	public void plugOffAirPressure(double time) {
		if (airPressure >= 0) {
			// if no one is occupying the rover, can power it off 
	
//			double nitrogenLeft = getInventory().getAmountResourceStored(ResourceUtil.nitrogenID, false);
//			double oxygenLeft = getInventory().getAmountResourceStored(OXYGEN, false);
//			double co2Left = getInventory().getAmountResourceStored(CO2, false);
//			double waterLeft = getInventory().getAmountResourceStored(WATER, false);
//			double sum = nitrogenLeft + oxygenLeft + co2Left + waterLeft;
			
			double rate = RATE_OF_CHANGE_OF_kPa_PER_MILLISOL * time;
			
			// TODO : will need to the internal air composition/pressure of a vehicle 
			airPressure -= rate;	
			
			if (airPressure < 0)
				airPressure = 0;	
		}
	}
	
	/**
	 * Gets the rover's airlock.
	 * 
	 * @return rover's airlock
	 */
	public Airlock getAirlock() {
		return airlock;
	}
	
	/**
	 * Perform time-related processes
	 * 
	 * @param pulse the amount of clock pulse passing (in millisols)
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		if (!super.timePassing(pulse)) {
			return false;
		}

		airlock.timePassing(pulse.getElapsed());
		
		boolean onAMission = isOnAMission();
		if (onAMission || isReservedForMission()) {
			
			Mission mission = getMission();
			if (mission != null) {
				// This code feel wrong
				Collection<MissionMember> members = mission.getMembers();
				for (MissionMember m: members) {
					if (m.getMission() == null) {
						// Defensively set the mission in the case that the delivery bot is registered as a mission member 
						// but its mission is null 
						// Question: why would the mission be null for this member in the first place after loading from a saved sim
						logger.info(this, m.getName() + " reregistered for " + mission + ".");
						m.setMission(mission);
					}
				}
				
				if (isInSettlement()) {				
					if (mission instanceof VehicleMission) {
						LoadingController lp = ((VehicleMission)mission).getLoadingPlan();
						
						if ((lp != null) && !lp.isCompleted()) {
							double time = pulse.getElapsed();
							double transferSpeed = 10; // Assume 10 kg per msol
							double amountLoading = time * transferSpeed;

							lp.backgroundLoad(amountLoading);
						}
					}
					
					plugInTemperature(pulse.getElapsed());
					plugInAirPressure(pulse.getElapsed());
				}
			}
				
			if (getAmountResourceStored(getFuelType()) > GroundVehicle.LEAST_AMOUNT)
				if (super.haveStatusType(StatusType.OUT_OF_FUEL))
					super.removeStatus(StatusType.OUT_OF_FUEL);
			
//			String s = this + " is plugged in.  " +  + airPressure + " kPa  " + temperature + " C";
//			if (!sCache.equals(s)) {
//				sCache = s;
//				logger.info(sCache);
//			}
		}
		
		else if (crewCapacity <= 0) {
			plugOffTemperature(pulse.getElapsed());
			plugOffAirPressure(pulse.getElapsed());
		}
		
		return true;
	}

	/**
	 * Gets a collection of people affected by this entity.
	 * 
	 * @return people collection
	 */
	public Collection<Person> getAffectedPeople() {
		Collection<Person> people = super.getAffectedPeople();

		Collection<Person> crew = getCrew();
		Iterator<Person> i = crew.iterator();
		while (i.hasNext()) {
			Person person = i.next();
			if (!people.contains(person))
				people.add(person);
		}

		return people;
	}

	/**
	 * Gets a collection of robots affected by this entity.
	 * 
	 * @return robots collection
	 */
	public Collection<Robot> getAffectedRobots() {
		Collection<Robot> robots = super.getAffectedRobots();

		Collection<Robot> crew = getRobotCrew();
		Iterator<Robot> i = crew.iterator();
		while (i.hasNext()) {
			Robot robot = i.next();
			if (!robots.contains(robot))
				robots.add(robot);
		}

		return robots;
	}

	/**
	 * Checks if the rover has a laboratory.
	 * 
	 * @return true if lab.
	 */
	public boolean hasLab() {
		return lab != null;
	}

	/**
	 * Gets the rover's laboratory
	 * 
	 * @return lab
	 */
	public Lab getLab() {
		return lab;
	}

	/**
	 * Gets a list of lab activity spots.
	 * 
	 * @return list of activity spots as Point2D objects.
	 */
	public List<Point2D> getLabActivitySpots() {
		return labActivitySpots;
	}

	/**
	 * Checks if the rover has a sick bay.
	 * 
	 * @return true if sick bay
	 */
	public boolean hasSickBay() {
		return sickbay != null;
	}

	/**
	 * Gets the rover's sick bay.
	 * 
	 * @return sick bay
	 */
	public SickBay getSickBay() {
		return sickbay;
	}

	/**
	 * Gets a list of sick bay activity spots.
	 * 
	 * @return list of activity spots as Point2D objects.
	 */
	public List<Point2D> getSickBayActivitySpots() {
		return sickBayActivitySpots;
	}

	/**
	 * Gets the resource type id that this vehicle uses as fuel, namely, methane
	 * 
	 * @return resource type id
	 */
	public int getFuelType() {
		return METHANE;
	}

	public AmountResource getFuelTypeAR() {
		return ResourceUtil.methaneAR;
	}

	/**
	 * Sets unit's location coordinates
	 * 
	 * @param newLocation the new location of the unit
	 */
	public void setCoordinates(Coordinates newLocation) {
		super.setCoordinates(newLocation);

		// Set towed vehicle (if any) to new location.
		if (towedVehicle != null)
			towedVehicle.setCoordinates(newLocation);
	}

	/**
	 * Gets the range of the vehicle
	 * 
	 * @return the range of the vehicle (in km)
	 * @throws Exception if error getting range.
	 */
	public double getRange(MissionType missionType) {
		// Note: multiply by 0.9 would account for the extra distance travelled in between sites 
		double fuelRange = super.getRange(missionType) * FUEL_RANGE_FACTOR;
		// Obtains the max mission range [in km] based on the type of mission
		// Note: total route ~= mission radius * 2   
		double missionRange = super.getMissionRange(missionType) * MISSION_RANGE_FACTOR;
		
		// Estimate the distance traveled per sol
		double distancePerSol = getEstimatedTravelDistancePerSol();
		
		// Gets the life support resource margin
		double margin = Vehicle.getLifeSupportRangeErrorMargin();
		
		// Check food capacity as range limit.
		PersonConfig personConfig = SimulationConfig.instance().getPersonConfig();
		double foodConsumptionRate = personConfig.getFoodConsumptionRate();
		double foodCapacity = getAmountResourceCapacity(FOOD);
		double foodSols = foodCapacity / (foodConsumptionRate * crewCapacity);
		double foodRange = distancePerSol * foodSols / margin;

		// Check water capacity as range limit.
		double waterConsumptionRate = personConfig.getWaterConsumptionRate();
		double waterCapacity = getAmountResourceCapacity(WATER);
		double waterSols = waterCapacity / (waterConsumptionRate * crewCapacity);
		double waterRange = distancePerSol * waterSols / margin;
//    	if (waterRange < fuelRange) fuelRange = waterRange;

		// Check oxygen capacity as range limit.
		double oxygenConsumptionRate = personConfig.getNominalO2ConsumptionRate();
		double oxygenCapacity = getAmountResourceCapacity(OXYGEN);
		double oxygenSols = oxygenCapacity / (oxygenConsumptionRate * crewCapacity);
		double oxygenRange = distancePerSol * oxygenSols / margin;
//    	if (oxygenRange < fuelRange) fuelRange = oxygenRange;

		double max = Math.min(oxygenRange, Math.min(foodRange, Math.min(waterRange, Math.min(missionRange, fuelRange))));

//		String s0 = this + " - " + missionName + " \n";
//		String s1 = String.format(" Radius : %5.0f km   Fuel : %5.0f km   Dist/sol : %5.0f km   Max : %5.0f km", 
//				missionRange, fuelRange, distancePerSol, max);
//		System.out.print(s0);
//		System.out.println(s1);
		
		return max;
	}

	@Override
	public void setParkedLocation(double xLocation, double yLocation, double facing) {
		super.setParkedLocation(xLocation, yLocation, facing);

		// Update towed vehicle locations.
		updatedTowedVehicleSettlementLocation();
	}

	/**
	 * Updates the settlement location of any towed vehicles.
	 */
	private void updatedTowedVehicleSettlementLocation() {

		Vehicle towedVehicle = getTowedVehicle();
		if (towedVehicle != null) {
			if (towedVehicle instanceof Rover) {
				// Towed rovers should be located behind this rover with same facing.
				double distance = (getLength() + towedVehicle.getLength()) / 2D;
				double towedX = 0D;
				double towedY = 0D - distance;
				Point2D.Double towedLoc = LocalAreaUtil.getLocalRelativeLocation(towedX, towedY, this);
				towedVehicle.setParkedLocation(towedLoc.getX(), towedLoc.getY(), getFacing());
			} else if (towedVehicle instanceof LightUtilityVehicle) {
				// Towed light utility vehicles should be attached to back of the rover
				// sideways and facing to the right.
				double distance = (getLength() + towedVehicle.getWidth()) / 2D;
				double towedX = 0D;
				double towedY = 0D - distance;
				Point2D.Double towedLoc = LocalAreaUtil.getLocalRelativeLocation(towedX, towedY, this);
				towedVehicle.setParkedLocation(towedLoc.getX(), towedLoc.getY(), getFacing() + 90D);
			}
		}
	}

	@Override
	public String getNickName() {
		return getName();
	}
	
	
	public double getCargoCapacity() {
		return cargoCapacity;
	}

	public boolean setLUV(LightUtilityVehicle luv) {
		if (!hasLUV()) {
			this.luv = luv;
			return true;
		}
		return false;
	}
	
	public LightUtilityVehicle getLUV() {
		return luv;
	}
	
	public boolean hasLUV() {
		return luv != null;
	}
	
	@Override
	public void destroy() {
		super.destroy();

		towedVehicle = null;

		labActivitySpots = null;
		sickBayActivitySpots = null;

		if (airlock != null)
			airlock.destroy();
		airlock = null;

		if (lab != null)
			lab.destroy();
		lab = null;

		if (sickbay != null)
			sickbay.destroy();
		sickbay = null;
	}

}
