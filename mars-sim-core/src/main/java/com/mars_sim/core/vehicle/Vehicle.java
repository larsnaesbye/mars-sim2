/*
 * Mars Simulation Project
 * Vehicle.java
 * @date 2024-06-09
 * @author Scott Davis
 */
package com.mars_sim.core.vehicle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.mars_sim.core.LocalAreaUtil;
import com.mars_sim.core.Unit;
import com.mars_sim.core.UnitEventType;
import com.mars_sim.core.UnitType;
import com.mars_sim.core.data.History;
import com.mars_sim.core.data.MSolDataItem;
import com.mars_sim.core.data.MSolDataLogger;
import com.mars_sim.core.data.UnitSet;
import com.mars_sim.core.environment.MarsSurface;
import com.mars_sim.core.environment.TerrainElevation;
import com.mars_sim.core.equipment.Container;
import com.mars_sim.core.equipment.Equipment;
import com.mars_sim.core.equipment.EquipmentInventory;
import com.mars_sim.core.equipment.EquipmentOwner;
import com.mars_sim.core.equipment.EquipmentType;
import com.mars_sim.core.equipment.ItemHolder;
import com.mars_sim.core.location.LocationStateType;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.malfunction.MalfunctionManager;
import com.mars_sim.core.malfunction.Malfunctionable;
import com.mars_sim.core.manufacture.Salvagable;
import com.mars_sim.core.manufacture.SalvageInfo;
import com.mars_sim.core.manufacture.SalvageProcessInfo;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.mission.Mission;
import com.mars_sim.core.person.ai.task.Converse;
import com.mars_sim.core.person.ai.task.Repair;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.person.health.RadiationExposure;
import com.mars_sim.core.project.Stage;
import com.mars_sim.core.resource.SuppliesManifest;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.structure.RadiationStatus;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingCategory;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.Indoor;
import com.mars_sim.core.structure.building.function.SystemType;
import com.mars_sim.core.structure.building.function.VehicleMaintenance;
import com.mars_sim.core.structure.building.task.MaintainBuilding;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.MarsTime;
import com.mars_sim.core.time.Temporal;
import com.mars_sim.core.vehicle.task.LoadingController;
import com.mars_sim.mapdata.location.Coordinates;
import com.mars_sim.mapdata.location.Direction;
import com.mars_sim.mapdata.location.LocalBoundedObject;
import com.mars_sim.mapdata.location.LocalPosition;
import com.mars_sim.tools.util.RandomUtil;

/**
 * The Vehicle class represents a generic vehicle. It keeps track of generic
 * information about the vehicle. This class needs to be subclassed to represent
 * a specific type of vehicle.
 */
public abstract class Vehicle extends Unit
		implements Malfunctionable, Salvagable, Temporal, Indoor,
		LocalBoundedObject, EquipmentOwner, ItemHolder, Towed {

	private static final long serialVersionUID = 1L;

	// default logger.
	private static final SimLogger logger = SimLogger.getLogger(Vehicle.class.getName());
	
	private static final double RANGE_FACTOR = 1.2;
	private static final double MAXIMUM_RANGE = 10_000;
	
    public static final double VEHICLE_CLEARANCE_0 = 1.4;
    public static final double VEHICLE_CLEARANCE_1 = 2.8;
    
	private static final int MAX_NUM_SOLS = 14;
	
	/** The error margin for determining vehicle range. (Actual distance / Safe distance). */
	private static double fuelRangeErrorMargin;
	private static double lifeSupportRangeErrorMargin;

	private static final String IMMINENT = " be imminent.";
	private static final String DETECTOR = "The radiation detector just forecasted a ";

	/** The types of status types that make a vehicle unavailable for us. */
	private static final List<StatusType> UNAVAILABLE_STATUS = Arrays.asList(
			StatusType.MAINTENANCE,
			StatusType.TOWED,
			StatusType.MOVING,
			StatusType.STUCK,
			StatusType.MALFUNCTION);

	/** True if vehicle is currently reserved for a mission. */
	protected boolean isReservedMission;
	/** True if vehicle is due for maintenance. */
	private boolean distanceMark;
	/** True if vehicle is currently reserved for periodic maintenance. */
	private boolean reservedForMaintenance;
	/** The emergency beacon for the vehicle. True if beacon is turned on. */
	private boolean emergencyBeacon;
	/** True if vehicle is salvaged. */
	private boolean isSalvaged;
	/** True if vehicle is charging. */
	private boolean isCharging;
	/** True if vehicle is ready to be drawn on the map. */
	private boolean isReady = false;
	
	/** Vehicle's associated Settlement. */
	private int associatedSettlementID;
	
	/** The average road load power of the vehicle [kph]. */
	private double averageRoadLoadSpeed;

	/** The average road load power of the vehicle [kW]. */
	private double averageRoadLoadPower;
			
	/** Parked facing (degrees clockwise from North). */
	private double facingParked;
	/** The Base Lifetime Wear in msols **/
	private double baseWearLifetime;
	/** Current accel of vehicle in m/s2. */
	private double accel = 0;
	/** Current speed of vehicle in kph. */
	private double speed = 0; //
	/** Total cumulative distance traveled by vehicle (km). */
	private double odometerMileage; //
	/** The last distance travelled by vehicle (km). */
	private double lastDistance;
	/** Distance traveled by vehicle since last maintenance (km) . */
	private double distanceMaint; //
	/** The cumulative energy usage of the vehicle [kWh] */
	private double cumEnergyUsedKWH;
	/** The instantaneous fuel economy of the vehicle [km/kg]. */
	private double iFuelEconomy;
	/** The instantaneous fuel consumption of the vehicle [Wh/km]. */
	private double iFuelConsumption;
	/** The actual start mass of the vehicle (base mass + crew weight + full cargo weight) for a trip [km/kg]. */
	private double startMass = 0;
	
	/** The vehicle specification */
	private String specName;
	
	/** Parked position (meters) from center of settlement. */
	private LocalPosition posParked;	
	/** The radiation status instance that capture if the settlement has been exposed to a radiation event. */
	private RadiationStatus exposed = RadiationStatus.calculateChance(0D);

	/** The vehicle type. */
	protected VehicleType vehicleType;
	/** The primary status type. */
	private StatusType primaryStatus;

	/** The malfunction manager for the vehicle. */
	protected MalfunctionManager malfunctionManager;
	/** Direction vehicle is traveling */
	private Direction direction;
	/** The operator of the vehicle. */
	private Worker vehicleOperator;
	/** The one currently towing this vehicle. */
	private Vehicle towingVehicle;
	/** The vehicle's salvage info. */
	private SalvageInfo salvageInfo;
	/** The EquipmentInventory instance. */
	private EquipmentInventory eqmInventory;
	/** The VehicleController instance. */
	private VehicleController vehicleController;
	/** The VehicleSpec instance. */
	private VehicleSpec spec;
	/** The mission instance. */
	private Mission mission;

	/** A collection of locations that make up the vehicle's trail. */
	private List<Coordinates> trail;
	/** List of operator activity spots. */
	private List<LocalPosition> operatorActivitySpots;
	/** List of passenger activity spots. */
	private List<LocalPosition> passengerActivitySpots;
	/** List of status types. */
	private Set<StatusType> statusTypes = new HashSet<>();
	
	/** The vehicle's status log. */
	private History<Set<StatusType>> vehicleLog = new History<>(40);
	/** The vehicle's road speed history. */
	private MSolDataLogger<Integer> roadSpeedHistory = new MSolDataLogger<>(MAX_NUM_SOLS);
	/** The vehicle's road power history. */	
	private MSolDataLogger<Integer> roadPowerHistory = new MSolDataLogger<>(MAX_NUM_SOLS);

	private LoadingController loadingController;
	
	static {
		lifeSupportRangeErrorMargin = simulationConfig.getSettlementConfiguration()
				.getRoverValues()[0];
		fuelRangeErrorMargin = simulationConfig.getSettlementConfiguration().getRoverValues()[1];
	}

	/**
	 * Constructor 1 : prepares a Vehicle object with a given settlement.
	 *
	 * @param name                the vehicle's name
	 * @param vehicleType         the configuration description of the vehicle.
	 * @param settlement          the settlement the vehicle is parked at.
	 * @param maintenanceWorkTime the work time required for maintenance (millisols)
	 */
	Vehicle(String name, VehicleSpec spec, Settlement settlement, double maintenanceWorkTime) {
		// Use Unit constructor
		super(name, settlement.getCoordinates());
		
		this.spec = spec;
		this.specName = spec.getName();
		this.vehicleType = spec.getType();
		setBaseMass(spec.getEmptyMass());
		
		// Get the description
		String description = spec.getDescription();
		// Set the description
		setDescription(description);
		// Get cargo capacity
		double cargoCapacity = spec.getTotalCapacity();
		// Create microInventory instance
		eqmInventory = new EquipmentInventory(this, cargoCapacity);

		// Set the capacities for each supported resource
		Map<Integer, Double> capacities = spec.getCargoCapacityMap();
		if (capacities != null) {
			eqmInventory.setResourceCapacityMap(capacities);
		}

		// Set total distance traveled by vehicle (km)
		odometerMileage = 0;
		// Set distance traveled by vehicle since last maintenance (km)
		distanceMaint = 0;
		// Obtain the associated settlement ID
		associatedSettlementID = settlement.getIdentifier();

		direction = new Direction(0);
		trail = new ArrayList<>();
		statusTypes = new HashSet<>();

		isReservedMission = false;
		distanceMark = false;
		reservedForMaintenance = false;
		emergencyBeacon = false;
		isSalvaged = false;
		
		// Make this vehicle to be owned by the settlement
		settlement.addOwnedVehicle(this);
		
		baseWearLifetime = spec.getWearLifetime();

		// Initialize malfunction manager.
		malfunctionManager = new MalfunctionManager(this, baseWearLifetime, maintenanceWorkTime);

		setupScopeString();

		primaryStatus = StatusType.PARKED;
		
		writeLog();

		// Instantiate the motor controller
		vehicleController = new VehicleController(this);
		
		// Set initial parked location and facing at settlement.
		// Note: already done in addOwnedVehicle()
//		findNewParkingLoc();

		// Initialize operator activity spots.
		operatorActivitySpots = spec.getOperatorActivitySpots();

		// Initialize passenger activity spots.
		passengerActivitySpots = spec.getPassengerActivitySpots();
		
		isReady = true;
	}

	/**
	 * Is the vehicle ready to be drawn on the settlement map ?
	 */
	public boolean isReady() {
		return isReady;
	}
	
	/**
	 * Sets the scope string.
	 */
	protected void setupScopeString() {
		// Add "vehicle" as scope
		malfunctionManager.addScopeString(SystemType.VEHICLE.getName());
	
		// Add its vehicle type as scope
		malfunctionManager.addScopeString(vehicleType.name());
	}
	
	/**
	 * Gets the base image for this Vehicle.
	 * 
	 * @todo This needs refactoring to avoid copying out VehicleSpec properties
	 * @return Name of base image for this vehicle
	 */
	public String getBaseImage() {
		return spec.getBaseImage();
	}
	/**
	 * Gets the name of the vehicle specification.
	 * 
	 * @see VehicleConfig#getVehicleSpec(String)
	 * @return Name of the VehicleSpec
	 */
	public String getSpecName() {
		return specName;
	}

	public VehicleType getVehicleType() {
		return vehicleType;
	}

	@Override
	public double getWidth() {
		return spec.getWidth();
	}

	@Override
	public double getLength() {
		return spec.getLength();
	}

	@Override
	public LocalPosition getPosition() {
		return posParked;
	}

	@Override
	public double getFacing() {
		return facingParked;
	}

	/**
	 * Gets a list of operator activity spots.
	 *
	 * @return list of activity spots as Point2D objects.
	 */
	public List<LocalPosition> getOperatorActivitySpots() {
		return operatorActivitySpots;
	}

	/**
	 * Gets a list of passenger activity spots.
	 *
	 * @return list of activity spots as Point2D objects.
	 */
	public List<LocalPosition> getPassengerActivitySpots() {
		return passengerActivitySpots;
	}

	/**
	 * Sets the location and facing of the vehicle when parked at a settlement.
	 *
	 * @param position  Position of the parking relative to the Settlement
	 * @param facing    (degrees from North clockwise).
	 */
	public void setParkedLocation(LocalPosition position, double facing) {
		// Set new parked location for the vehicle.
		this.posParked = position;
		this.facingParked = facing;
		
		// Get current human crew positions relative to the vehicle.
		Map<Person, LocalPosition> currentCrewPositions = getCurrentCrewPositions();
		// Set the human crew locations to the vehicle's new parked location.
		if (currentCrewPositions != null)
			setCrewPositions(currentCrewPositions);
		
		// Get current robot crew positions relative to the vehicle.
		Map<Robot, LocalPosition> currentRobotCrewPositions = getCurrentRobotCrewPositions();

		// Set the robot crew locations to the vehicle's new parked location.
		if (currentRobotCrewPositions != null)
			setRobotCrewPositions(currentRobotCrewPositions);
	}

	/**
	 * Sets the location and facing of the drone when parked at a settlement.
	 *
	 * @param position  Position of the parking relative to the Settlement
	 * @param facing    (degrees from North clockwise).
	 */
	public void setFlyerLocation(LocalPosition position, double facing) {
		// Set new parked location for the flyer.
		this.posParked = position;
		this.facingParked = facing;
	}
	
	/**
	 * Gets all human crew member positions relative to within the vehicle.
	 *
	 * @return map of crew members and their relative vehicle positions.
	 */
	private Map<Person, LocalPosition> getCurrentCrewPositions() {

		Map<Person, LocalPosition> result = null;

		// Record current object-relative crew positions if vehicle is crewable.
		if (this instanceof Crewable crewable) {
			result = new HashMap<>(crewable.getCrewNum());
			Iterator<Person> i = crewable.getCrew().iterator();
			while (i.hasNext()) {
				Person crewmember = i.next();
				LocalPosition crewPos = LocalAreaUtil.convert2LocalPos(crewmember.getPosition(), this);
				result.put(crewmember, crewPos);
			}
		}

		return result;
	}

	/**
	 * Gets all robot crew member positions relative to within the vehicle.
	 *
	 * @return map of crew members and their relative vehicle positions.
	 */
	private Map<Robot, LocalPosition> getCurrentRobotCrewPositions() {

		Map<Robot, LocalPosition> result = null;

		// Record current object-relative crew positions if vehicle is crewable.
		if (this instanceof Crewable crewable) {
			result = new HashMap<>(crewable.getRobotCrewNum());
			Iterator<Robot> i = crewable.getRobotCrew().iterator();
			while (i.hasNext()) {
				Robot robotCrewmember = i.next();
				LocalPosition crewPos = LocalAreaUtil.convert2LocalPos(robotCrewmember.getPosition(), this);
				result.put(robotCrewmember, crewPos);
			}
		}

		return result;
	}

	/**
	 * Sets the positions of all human crew members (if any) to the vehicle's
	 * location.
	 * 
	 * @param currentCrewPositions
	 */
	private void setCrewPositions(Map<Person, LocalPosition> currentCrewPositions) {

		// Only move crew if vehicle is Crewable.
		if (this instanceof Crewable crewable) {
			Iterator<Person> i = crewable.getCrew().iterator();
			while (i.hasNext()) {
				Person crewmember = i.next();

				LocalPosition currentCrewPos = currentCrewPositions.get(crewmember);
				LocalPosition settlementLoc = LocalAreaUtil.convert2SettlementPos(currentCrewPos,
																		this);
				crewmember.setPosition(settlementLoc);
			}
		}
	}

	/**
	 * Sets the positions of all robot crew members (if any) to the vehicle's
	 * location.
	 * 
	 * @param currentRobotCrewPositions
	 */
	private void setRobotCrewPositions(Map<Robot, LocalPosition> currentRobotCrewPositions) {

		// Only move crew if vehicle is Crewable.
		if (this instanceof Crewable crewable) {
			Iterator<Robot> i = crewable.getRobotCrew().iterator();
			while (i.hasNext()) {
				Robot robotCrewmember = i.next();

				LocalPosition currentCrewPos = currentRobotCrewPositions.get(robotCrewmember);
				LocalPosition settlementLoc = LocalAreaUtil.convert2SettlementPos(currentCrewPos,
														this);
				robotCrewmember.setPosition(settlementLoc);
			}
		}
	}

	/**
	 * Prints a string list of status types.
	 *
	 * @return
	 */
	public String printStatusTypes() {
		StringBuilder builder = new StringBuilder();
		builder.append(primaryStatus.getName());

		for (StatusType st : statusTypes) {
			builder.append(", ").append(st.getName());
		}
		return builder.toString();
	}

	/**
	 * Checks if this vehicle has already been tagged with a status type.
	 *
	 * @param status the status type of interest
	 * @return yes if it has it
	 */
	public boolean haveStatusType(StatusType status) {
        return (primaryStatus == status) || statusTypes.contains(status);
    }

	/**
	 * Checks if this vehicle has no issues and is ready for mission.
	 *
	 * @return yes if it has anyone of the bad status types
	 */
	public boolean isVehicleReady() {
	    for (StatusType st : UNAVAILABLE_STATUS) {
			if (statusTypes.contains(st))
				return false;
	    }

	    return true;
	}

	public StatusType getPrimaryStatus() {
		return primaryStatus;
	}

	/**
	 * Sets the Primary status of a Vehicle that represents it's situation.
	 * 
	 * @param newStatus Must be a primary status value
	 */
	public void setPrimaryStatus(StatusType newStatus) {
		setPrimaryStatus(newStatus, null);
	}

	/**
	 * Sets the Primary status of a Vehicle that represents it's situation. Also there is 
	 * a Secondary status on why the primary has changed.
	 * 
	 * @param newStatus Must be a primary status value
	 * @param secondary Reason for the change; can be null be none given
	 */
	public void setPrimaryStatus(StatusType newStatus, StatusType secondary) {
		if (!newStatus.isPrimary()) {
			throw new IllegalArgumentException("Status is not Primary " + newStatus.getName());
		}

		boolean doEvent = false;
		if (primaryStatus != newStatus) {
			primaryStatus = newStatus;
			doEvent = true;
		}

		// Secondary is optional
		if ((secondary != null) && !statusTypes.contains(secondary)) {
			statusTypes.add(secondary);
			doEvent = true;
		}

		if (doEvent) {
			writeLog();
			fireUnitUpdate(UnitEventType.STATUS_EVENT, newStatus);
		}
	}

	/**
	 * Adds a Secondary status type for this vehicle.
	 *
	 * @param newStatus the status to be added
	 */
	public void addSecondaryStatus(StatusType newStatus) {
		if (newStatus.isPrimary()) {
			throw new IllegalArgumentException("Status is not Secondary " + newStatus.getName());
		}

		// Update status based on current situation.
		if (!statusTypes.contains(newStatus)) {
			statusTypes.add(newStatus);
			writeLog();
			fireUnitUpdate(UnitEventType.STATUS_EVENT, newStatus);
		}
	}

	/**
	 * Removes a Secondary status type for this vehicle.
	 *
	 * @param oldStatus the status to be removed
	 */
	public void removeSecondaryStatus(StatusType oldStatus) {
		// Update status based on current situation.
		if (statusTypes.contains(oldStatus)) {
			statusTypes.remove(oldStatus);
			writeLog();
			fireUnitUpdate(UnitEventType.STATUS_EVENT, oldStatus);
		}
	}

	/**
	 * Get the loading plan associated with this Vehicle
	 */
	public LoadingController getLoadingPlan() {
		return loadingController;
	}

	/**
	 * Change the loading status of this loading
	 * @param manifest Supplies to load; if this is null then stop the loading
	 */
    public LoadingController setLoading(SuppliesManifest manifest) {
		if (manifest == null) {
			removeSecondaryStatus(StatusType.LOADING);
			loadingController = null;
		}
        else if (statusTypes.contains(StatusType.LOADING)) {
			logger.warning(this, "Is already in the loading status");
		}
		else {
			// Automatically remve UNLOADING as LOADING takes precedence
			removeSecondaryStatus(StatusType.UNLOADING);
			loadingController = new LoadingController(getSettlement(), this, manifest);
			addSecondaryStatus(StatusType.LOADING);
		}
		return loadingController;
    }

	/**
	 * Checks if the vehicle is currently in a garage or not.
	 *
	 * @return true if vehicle is in a garage.
	 */
	public boolean isInGarage() {

		Settlement settlement = getSettlement();
		if (settlement != null) {
			return getSettlement().getBuildingManager().isInGarage(this);
		}
		return false;
	}

	/**
	 * Adds the vehicle to a garage.
	 *
	 * @return true if successful.
	 */
	public boolean addToAGarage() {
		return getSettlement().getBuildingManager().addToGarageBuilding(this) != null;
	}
	
	/**
	 * Records the status in the vehicle log.
	 */
	private void writeLog() {
		Set<StatusType> entry = new HashSet<>(statusTypes);
		entry.add(primaryStatus);
		vehicleLog.add(entry);
	}

	/**
	 * Gets the vehicle log.
	 *
	 * @return List of changes ot the status
	 */
	public History<Set<StatusType>> getVehicleLog() {
		return vehicleLog;
	}

	/**
	 * Checks if the vehicle is reserved for any reason.
	 *
	 * @return true if vehicle is currently reserved
	 */
	public boolean isReserved() {
		return isReservedMission || reservedForMaintenance;
	}

	/**
	 * Checks if the vehicle is reserved for a mission.
	 *
	 * @return true if vehicle is reserved for a mission.
	 */
	public boolean isReservedForMission() {
		return isReservedMission;
	}

	/**
	 * Sets if the vehicle is reserved for a mission or not.
	 *
	 * @param reserved the vehicle's reserved for mission status
	 */
	public void setReservedForMission(boolean reserved) {
		if (isReservedMission != reserved) {
			isReservedMission = reserved;
			fireUnitUpdate(UnitEventType.RESERVED_EVENT);
		}
	}

	/**
	 * Checks if the vehicle is reserved for maintenance.
	 *
	 * @return true if reserved for maintenance.
	 */
	public boolean isReservedForMaintenance() {
		return reservedForMaintenance;
	}

	/**
	 * Sets if the vehicle is reserved for maintenance or not.
	 *
	 * @param reserved true if reserved for maintenance
	 */
	public void setReservedForMaintenance(boolean reserved) {
		if (reservedForMaintenance != reserved) {
			reservedForMaintenance = reserved;
			fireUnitUpdate(UnitEventType.RESERVED_EVENT);
		}
	}

	/**
	 * Sets the vehicle that is currently towing this vehicle.
	 *
	 * @param towingVehicle the vehicle
	 */
	public void setTowingVehicle(Vehicle towingVehicle) {
		if (this == towingVehicle)
			throw new IllegalArgumentException("Vehicle cannot tow itself.");

		if (towingVehicle != null) {
			// if towedVehicle is not null, it means this rover has just hooked up for towing the towedVehicle
			addSecondaryStatus(StatusType.TOWED);
		}
		else {
			removeSecondaryStatus(StatusType.TOWED);
		}

		this.towingVehicle = towingVehicle;
	}

	/**
	 * Gets the vehicle that is currently towing this vehicle.
	 *
	 * @return towing vehicle
	 */
	public Vehicle getTowingVehicle() {
		return towingVehicle;
	}

	/**
	 * Checks if this vehicle is being towed (by another vehicle).
	 *
	 * @return true if it is being towed
	 */
	public boolean isBeingTowed() {
        return towingVehicle != null;
    }

	/**
	 * Gets the average power of the vehicle when operating [kW].
	 * 
	 * @return
	 */
	public double getAveragePower() {
		return spec.getBasePower();
	}
	
	/**
	 * Gets the speed of vehicle.
	 *
	 * @return the vehicle's speed (in km/hr)
	 */
	public double getSpeed() {
		return speed;
	}

	/**
	 * Sets the vehicle's current speed.
	 *
	 * @param speed the vehicle's speed (in km/hr)
	 */
	public void setSpeed(double speed) {
		if (speed < 0D)
			throw new IllegalArgumentException("Vehicle speed cannot be less than 0 km/hr: " + speed);
		if (Double.isNaN(speed))
			throw new IllegalArgumentException("Vehicle speed is a NaN");

		if (speed != this.speed) {
			if (speed == 0D) {
				setPrimaryStatus(StatusType.PARKED);
			} 
			else if (this.speed == 0D) {
				// Was zero so now must be moving
				setPrimaryStatus(StatusType.MOVING);
			}
			this.speed = speed;
			fireUnitUpdate(UnitEventType.SPEED_EVENT);
		}
	}

	/**
	 * Gets the base speed of vehicle
	 *
	 * @return the vehicle's base speed (in kph or km/hr)
	 */
	public double getBaseSpeed() {
		return spec.getBaseSpeed();
	}


	/**
	 * Gets the current fuel range of the vehicle.
	 *
	 * @return the current fuel range of the vehicle (in km)
	 */
	public double getRange() {

		double range = 0;
		Mission mission = getMission();

        if ((mission == null) || (mission.getStage() == Stage.PREPARATION)) {
        	// Before the mission is created, the range would be based on vehicle's capacity
        	range = Math.min(getBaseRange() * RANGE_FACTOR, getEstimatedFuelEconomy() * getFuelCapacity()) * getMass() / getBeginningMass();// * fuel_range_error_margin
        }
        else {
        	
    		int fuelTypeID = getFuelTypeID();
    		if (fuelTypeID < 0) {
    			range = MAXIMUM_RANGE;
    		}
    		else {
                double amountOfFuel = getAmountResourceStored(fuelTypeID);
            	// During the journey, the range would be based on the amount of fuel in the vehicle
        		range = Math.min(getBaseRange() * RANGE_FACTOR, getEstimatedFuelEconomy() * amountOfFuel) * getMass() / getBeginningMass();
    		}
        }

        return (int)range;
	}

	/**
	 * Gets the base range of the vehicle.
	 *
	 * @return the base range of the vehicle [km]
	 * @throws Exception if error getting range.
	 */
	public double getBaseRange() {
		return spec.getBaseRange();
	}

	/**
	 * Gets the fuel capacity of the vehicle [kg].
	 *
	 * @return
	 */
	public double getFuelCapacity() {
		return spec.getFuelCapacity();
	}

	/**
	 * Sets the average road load speed of the vehicle [kph].
	 * 
	 * @return
	 */
	public void setAverageRoadLoadSpeed(int value) {
		roadSpeedHistory.addDataPoint(value);
	}
	
	/**
	 * Sets the average road load power of the vehicle [kW].
	 * 
	 * @return
	 */
	public void setAverageRoadLoadPower(int value) {
		roadPowerHistory.addDataPoint(value);
	}
	
	/**
	 * Gets the average road load power of the vehicle [kph].
	 * 
	 * @return
	 */
	public double getAverageRoadLoadSpeed() {
		return averageRoadLoadSpeed;
	}
	
	/**
	 * Gets the average road load power of the vehicle [kW].
	 * 
	 * @return
	 */
	public double getAverageRoadLoadPower() {
		return averageRoadLoadPower;
	}
	
	
	/**
	 * Gets the cumulative energy usage of the vehicle [kWh].
	 * 
	 * @return
	 */
	public double getCumEnergyUsage() {
		return cumEnergyUsedKWH;
	}
	
	/**
	 * Gets the energy available at the full tank [kWh].
	 *
	 * @return
	 */
	public double getEnergyCapacity() {
		return spec.getEnergyCapacity();
	}

	/**
	 * Gets the estimated energy available for the drivetrain [kWh].
	 *
	 * @return
	 */
	public double getDrivetrainEnergy() {
		return spec.getDrivetrainEnergy();
	}
	
	/**
	 * Gets the fuel to energy conversion factor [Wh/kg].
	 * 
	 * @return
	 */
	public double getFuelConv() {
		return spec.getFuel2DriveEnergy();
	}
	
	/**
	 * Gets the cumulative fuel economy [km/kg].
	 * 
	 * @return
	 */
	public double getCumFuelEconomy() {
		if (odometerMileage == 0 || cumEnergyUsedKWH == 0)
			return 0;
		return odometerMileage / cumEnergyUsedKWH / 1000 * getFuelConv();
	}
	
	/**
	 * Gets the cumulative fuel consumption [Wh/km].
	 * 
	 * @return
	 */
	public double getCumFuelConsumption() {
		if (odometerMileage == 0 || cumEnergyUsedKWH == 0)
			return 0;
		return 1000 * cumEnergyUsedKWH / odometerMileage;
	}
	
	/**
	 * Gets the coefficient for converting cumulative FC to cumulative FE.
	 * 
	 * @return
	 */
	public double getCoeffCumFC2FE() {
		double cumFE = getCumFuelEconomy();
		double cumFC = getCumFuelConsumption();
		
		if (cumFE > 0 && cumFC > 0 && averageRoadLoadPower > 0 && averageRoadLoadSpeed >0)
			return cumFE / cumFC * averageRoadLoadPower / averageRoadLoadSpeed ;
		
		return 0;
	}
	
	
	/**
	 * Gets the base fuel economy of the vehicle [km/kg].
	 * 
	 * @return
	 */
	public double getBaseFuelEconomy() {
		return spec.getBaseFuelEconomy();
	}

	/**
	 * Gets the base fuel consumption of the vehicle [Wh/km].
	 * 
	 * @return
	 */
	public double getBaseFuelConsumption() {
		return spec.getBaseFuelConsumption();
	}
	
	/**
	 * Gets the instantaneous fuel consumption of the vehicle [Wh/km].
	 * 
	 * @return
	 */
	public double getIFuelConsumption() {
		return iFuelConsumption;
	}
	
	/**
	 * Sets the instantaneous fuel consumption of the vehicle [kWh/km].
	 * 
	 * @param iFuelC
	 */
	public void setIFuelConsumption(double iFuelC) {
		this.iFuelConsumption = iFuelC;
	}
	
	/**
	 * Sets the instantaneous fuel economy of the vehicle [km/kg].
	 * 
	 * @param iFuelEconomy
	 */
	public void setIFuelEconomy(double iFuelEconomy) {
		this.iFuelEconomy = iFuelEconomy;
	}
	
	/**
	 * Gets the instantaneous fuel economy of the vehicle [km/kg].
	 * 
	 * @return
	 */
	public double getIFuelEconomy() {
		return iFuelEconomy;
	}
	
	/**
	 * Mass of Equipment is the stored mass plus the base mass.
	 */
	@Override
	public double getMass() {
		return eqmInventory.getStoredMass() + getBaseMass();
	}
	
	/**
	 * Gets the estimated beginning mass [kg].
	 */
	public double getBeginningMass() {
		return spec.getBeginningMass();
	}
	
	/**
	 * Records the beginning weight of the vehicle and its payload [kg].
	 */
	public void recordStartMass() {
		startMass = getMass();
	}

	/**
	 * Records the beginning weight of the vehicle and its payload [kg].
	 */
	public double getStartMass() {
		return startMass;
	}
	
	/**
	 * Gets the initial fuel economy of the vehicle [km/kg] for a trip.
	 *
	 * @return
	 */
	public double getInitialFuelEconomy() {
		return spec.getInitialFuelEconomy();
	}

	/**
	 * Gets the estimated fuel economy of the vehicle [km/kg] for a trip.
	 *
	 * @return
	 */
	public double getEstimatedFuelEconomy() {
		double base = getBaseFuelEconomy();
		double cum = getCumFuelEconomy();
		double init = getInitialFuelEconomy();
		// Note: init < base always
		// Note: if cum < base, then trip is less economical more than expected
		// Note: if cum > base, then trip is more economical than expected
		if (cum == 0)
			return (.5 * base + .5 * init) * VehicleController.FUEL_ECONOMY_FACTOR;
		else {
			return (.3 * base + .3 * init + .4 * cum);
		}
	}

	/**
	 * Gets the initial fuel consumption of the vehicle [Wh/km] for a trip.
	 *
	 * @return
	 */
	public double getInitialFuelConsumption() {
		return spec.getInitialFuelConsumption();
	}
	
	/**
	 * Gets the estimated fuel consumption of the vehicle [Wh/km] for a trip.
	 *
	 * @return
	 */
	public double getEstimatedFuelConsumption() {
		double base = getBaseFuelConsumption();
		double cum = getCumFuelConsumption();
		double init = getInitialFuelConsumption();
		// Note: init > base always
		// Note: if cum > base, then vehicle consumes more than expected
		// Note: if cum < base, then vehicle consumes less than expected		
		if (cum == 0)
			return (.5 * base + .5 * init) / VehicleController.FUEL_ECONOMY_FACTOR;
		else {
			return (.3 * base + .3 * init + .4 * cum);
		}
	}
	
	/**
	 * Gets the number of battery modules of the vehicle.
	 *
	 * @return
	 */
	public int getBatteryModule() {
		return spec.getBatteryModule();
	}
	
	/**
	 * Gets the total battery capacity of the vehicle.
	 *
	 * @return
	 */
	public double getBatteryCapacity() {
		return spec.getBatteryCapacity();
	}
	
	/**
	 * Gets the percent of remaining battery energy of the vehicle.
	 *
	 * @return
	 */
	public double getBatteryPercent() {
		return getController().getBattery().getBatteryState();
	}
	
	
	/**
	 * Gets the number of fuel cell stacks of the vehicle.
	 *
	 * @return
	 */
	public int getFuellCellStack() {
		return spec.getFuelCellStack();
	}
			
	/**
	 * Gets the drivetrain efficiency of the vehicle.
	 *
	 * @return drivetrain efficiency
	 */
	public double getDrivetrainEfficiency() {
		return spec.getDrivetrainEfficiency();
	}

	/**
	 * Returns total distance traveled by vehicle [km].
	 *
	 * @return the total distanced traveled by the vehicle [km]
	 */
	public double getOdometerMileage() {
		return odometerMileage;
	}

	/**
	 * Adds the distance traveled to vehicle's odometer (total distance traveled)
	 * and record the fuel used.
	 *
	 * @param distance the distance traveled traveled [km]
	 * @param cumEnergyUsed the energy used [Wh]
	 */
	public void addOdometerMileage(double distance, double cumEnergyUsed) {
		this.odometerMileage += distance;
		this.lastDistance = distance;
		this.cumEnergyUsedKWH += cumEnergyUsed/1000;
	}

	public double getLastDistanceTravelled() {
		return lastDistance;
	}
	
	/**
	 * Returns distance traveled by vehicle since last maintenance [km].
	 *
	 * @return distance traveled by vehicle since last maintenance [km]
	 */
	public double getDistanceLastMaintenance() {
		return distanceMaint;
	}

	/**
	 * Adds a distance to the vehicle's distance since last maintenance.
	 * Sets distanceMark to true if vehicle is due for maintenance.
	 *
	 * @param distance distance to add ([km]
	 */
	public void addDistanceLastMaintenance(double distance) {
		distanceMaint += distance;
		if ((distanceMaint > 5000D) && !distanceMark)
			distanceMark = true;
	}

	/** 
	 * Sets vehicle's distance since last maintenance to zero. 
	 */
	public void clearDistanceLastMaintenance() {
		distanceMaint = 0;
	}

	/**
	 * Returns direction of vehicle (0 = north, clockwise in radians).
	 *
	 * @return the direction the vehicle is traveling (in radians)
	 */
	public Direction getDirection() {
		return direction;
	}

	/**
	 * Sets the vehicle's facing direction (0 = north, clockwise in radians).
	 *
	 * @param direction the direction the vehicle is traveling (in radians)
	 */
	public void setDirection(Direction direction) {
		this.direction = direction;
	}

	/**
	 * Gets the instantaneous acceleration of the vehicle [m/s2].
	 * 
	 * @return
	 */
	public double getAccel() {
		return accel;
	}

	/**
	 * Sets the acceleration in [m/s2].
	 * 
	 * @param accel
	 */
	public void setAccel(double accel) {
		this.accel = accel;
	}
	
	/**
	 * Gets the allowable acceleration of the vehicle [m/s2].
	 * 
	 * @return
	 */
	public double getAllowedAccel() {
		if (speed <= 1)
			return getBaseAccel();
		return getBaseAccel() * getBeginningMass() / getMass();
//		return (baseAccel + Math.min(baseAccel, averagePower / getMass() / speed * 3600)) / 2.0;
	}
	
	/**
	 * Gets the base acceleration of the vehicle [m/s2].
	 * 
	 * @return
	 */
	public double getBaseAccel() {
		return spec.getBaseAccel();
	}
	
	public abstract double getTerrainGrade();

	public abstract double getElevation();

	/**
	 * Gets the operator of the vehicle (person or AI).
	 *
	 * @return the vehicle operator
	 */
	public Worker getOperator() {
		return vehicleOperator;
	}

	/**
	 * Sets the operator of the vehicle.
	 *
	 * @param vehicleOperator the vehicle operator
	 */
	public void setOperator(Worker vehicleOperator) {
		this.vehicleOperator = vehicleOperator;
		fireUnitUpdate(UnitEventType.OPERATOR_EVENT, vehicleOperator);
	}

	/**
	 * Returns the current settlement vehicle is parked at. Returns null if vehicle
	 * is not currently parked at a settlement.
	 *
	 * @return the settlement the vehicle is parked at
	 */
	@Override
	public Settlement getSettlement() {

		if (getContainerID() <= Unit.MARS_SURFACE_UNIT_ID)
			return null;

		Unit c = getContainerUnit();

		if (c.getUnitType() == UnitType.SETTLEMENT)
			return (Settlement) c;

		// If this unit is an LUV and it is within a rover
		if (c.getUnitType() == UnitType.VEHICLE)
			return ((Vehicle)c).getSettlement();

		return null;
	}

	/**
	 * Gets the garage building that the vehicle is at.
	 *
	 * @return {@link Building}
	 */
	public Building getGarage() {
		Settlement settlement = getSettlement();
		if (settlement == null)
			return null;
		
		for (Building garageBuilding : settlement.getBuildingManager().getGarages()) {
			VehicleMaintenance garage = garageBuilding.getVehicleMaintenance();
			if (garage != null) {
				if (this instanceof Flyer flyer && garage.containsFlyer(flyer)) {
					return garageBuilding;
				}
				else if (garage.containsVehicle(this)) {
					return garageBuilding;
				}
			}
		}
		return null;
	}

	/**
	 * Gets the unit's malfunction manager.
	 *
	 * @return malfunction manager
	 */
	public MalfunctionManager getMalfunctionManager() {
		return malfunctionManager;
	}

	/**
	 * Time passing for vehicle.
	 *
	 * @param pulse the amount of clock pulse passing (in millisols)
	 * @throws Exception if error during time.
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		if (!isValid(pulse)) {
			return false;
		}

		if (isCharging && getContainerUnit() instanceof Settlement settlement) {
			chargeVehicle(pulse, settlement);
		}
		
		// If it's outside and moving
		else if (primaryStatus == StatusType.MOVING) {
			// Assume the wear and tear factor is at 100% by being used in a mission
			malfunctionManager.activeTimePassing(pulse);
			// Add the location to the trail if outside on a mission
			addToTrail(getCoordinates());
		}

		// Regardless being outside or inside settlement,
		// if it's malfunction (outside or inside settlement) 
		// whether it's in a garage or not
		else if (haveStatusType(StatusType.MALFUNCTION)
				&& malfunctionManager.getMalfunctions().size() == 0) {
			// Remove the malfunction status
			removeSecondaryStatus(StatusType.MALFUNCTION);
		}
		
		// Regardless being outside or inside settlement,
		// if it's still reportedly under maintenance
		// but maintenance just got done	
		else if (haveStatusType(StatusType.MAINTENANCE) 
			&& malfunctionManager.getEffectiveTimeSinceLastMaintenance() <= 0D) {
			// Make sure reservedForMaintenance is false since vehicle now needs no maintenance.
			setReservedForMaintenance(false);
			// Remove the malfunction status
			removeSecondaryStatus(StatusType.MAINTENANCE);
			// If the vehicle is in a garage, remove from garage
			BuildingManager.removeFromGarage(this);
		}
		
		// Regardless being outside or inside settlement,
		// NOT under maintenance
		else {
			
			if (!haveStatusType(StatusType.GARAGED)) {
				int rand = RandomUtil.getRandomInt(3);
				// Assume the wear and tear factor is 75% less when not operating 
				if (rand == 3)
					malfunctionManager.activeTimePassing(pulse);
			}
			
			else {
				// Note: during maintenance, it doesn't need to be checking for malfunction.
				malfunctionManager.timePassing(pulse);
			}
			
		}

		// Check once per msol (millisol integer)
		if (pulse.isNewMSol()) {
			
//			if (primaryStatus == StatusType.PARKED && isReserved()){
//				// If the vehicle is reserved and is not in a garage, add to  garage
//				addToAGarage();
//			}
			
//			if (primaryStatus == StatusType.GARAGED && !isReserved()) {
//				// If the vehicle is not reserved and is in a garage, remove from garage
//				BuildingManager.removeFromGarage(this);
//			}
			
			// Sample a data point every SAMPLE_FREQ (in msols)
			int msol = pulse.getMarsTime().getMillisolInt();

			// Check every RADIATION_CHECK_FREQ (in millisols)
			// Compute whether a baseline, GCR, or SEP event has occurred
			int remainder = msol % RadiationExposure.RADIATION_CHECK_FREQ;
			if (remainder == RadiationExposure.RADIATION_CHECK_FREQ - 1) {
				RadiationStatus newExposed = RadiationStatus.calculateChance(pulse.getElapsed());
				setExposed(newExposed);
			}

			// TODO: Should the following be executed when a vehicle is on the road ? 
			
			int count = 0;
			int sum = 0;

			for (int sol: roadSpeedHistory.getHistory().keySet()) {
				List<MSolDataItem<Integer>> speeds = roadSpeedHistory.getHistory().get(sol);
				for (MSolDataItem<Integer> s: speeds) {
					count++;
					sum += s.getData();
				}
			}
			
			if (count > 0 && sum > 0)
				averageRoadLoadSpeed = sum / count;
			
			count = 0;
			sum = 0;
			for (int sol: roadPowerHistory.getHistory().keySet()) {
				List<MSolDataItem<Integer>> speeds = roadPowerHistory.getHistory().get(sol);
				for (MSolDataItem<Integer> s: speeds) {
					count++;
					sum += s.getData();
				}
			}
			
			if (count > 0 && sum > 0)
				averageRoadLoadPower = sum / count;
		}
		
		return true;
	}

	/*
	 * Updates the status of Radiation exposure.
	 * 
	 * @param newExposed
	 */
	public void setExposed(RadiationStatus newExposed) {
		exposed = newExposed;
		
		if (exposed.isBaselineEvent()) {
			logger.log(this, Level.INFO, 1_000, DETECTOR + UnitEventType.BASELINE_EVENT.toString() + IMMINENT);
			this.fireUnitUpdate(UnitEventType.BASELINE_EVENT);
		}

		if (exposed.isGCREvent()) {
			logger.log(this, Level.INFO, 1_000, DETECTOR + UnitEventType.GCR_EVENT.toString() + IMMINENT);
			this.fireUnitUpdate(UnitEventType.GCR_EVENT);
		}

		if (exposed.isSEPEvent()) {
			logger.log(this, Level.INFO, 1_000, DETECTOR + UnitEventType.SEP_EVENT.toString() + IMMINENT);
			this.fireUnitUpdate(UnitEventType.SEP_EVENT);
		}
	}
	
	/**
	 * Gets the radiation status.
	 * 
	 * @return
	 */
	public RadiationStatus getExposed() {
		return exposed;
	}
	
	/**
	 * Resets the vehicle reservation status.
	 */
	// public void correctVehicleReservation() {
	// 	if (isReservedMission
	// 		// Set reserved for mission to false if the vehicle is not associated with a
	// 		// mission.
	// 		&& missionManager.getMissionForVehicle(this) == null) {
	// 			logger.log(this, Level.FINE, 5000,
	// 					"Found reserved for an non-existing mission. Untagging it.");
	// 			setReservedForMission(false);
	// 	} else if (missionManager.getMissionForVehicle(this) != null) {
	// 			logger.log(this, Level.FINE, 5000,
	// 					"On a mission but not registered as mission reserved. Correcting it.");
	// 			setReservedForMission(true);
	// 	}
	// }

	/**
	 * Gets a collection of people affected by this entity.
	 *
	 * @return person collection
	 */
	public Collection<Person> getAffectedPeople() {
		Collection<Person> people = new UnitSet<>();

		// Check all people.
		Iterator<Person> i = unitManager.getPeople().iterator();
		while (i.hasNext()) {
			Person person = i.next();
			Task task = person.getMind().getTaskManager().getTask();

			// Add all people maintaining this vehicle.
			if (task instanceof MaintainBuilding mb
				&& this.equals(mb.getEntity())) {
				people.add(person);
			}

			// Add all people repairing this vehicle.
			if (task instanceof Repair r
				&& this.equals(r.getEntity())) {
				people.add(person);
			}
		}

		return people;
	}

	/**
	 * Gets a collection of people who are available for social conversation in this
	 * vehicle.
	 *
	 * @return person collection
	 */
	public Collection<Person> getTalkingPeople() {
		Collection<Person> people = new UnitSet<>();

		// Check all people.
		Iterator<Person> i = unitManager.getPeople().iterator();
		while (i.hasNext()) {
			Person person = i.next();
			Task task = person.getMind().getTaskManager().getTask();

			// Add all people having conversation from all places as the task
			if (task instanceof Converse)
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
		Collection<Robot> robots = new UnitSet<>();

		// Check all robots.
		Iterator<Robot> i = unitManager.getRobots().iterator();
		while (i.hasNext()) {
			Robot robot = i.next();
			Task task = robot.getBotMind().getBotTaskManager().getTask();

			// Add all robots maintaining this vehicle.
			if (task instanceof MaintainBuilding mb) {
				if (mb.getEntity() == this) {
					robots.add(robot);
				}
			}

			// Add all robots repairing this vehicle.
			if (task instanceof Repair r) {
				if (r.getEntity() == this) {
					robots.add(robot);
				}
			}
		}

		return robots;
	}

	/**
	 * Gets the vehicle's trail as a collection of coordinate locations.
	 *
	 * @return trail collection
	 */
	public Collection<Coordinates> getTrail() {
		return trail;
	}

	/**
	 * Adds a location to the vehicle's trail if appropriate.
	 *
	 * @param location location to be added to trail
	 */
	public void addToTrail(Coordinates location) {

		if (!trail.isEmpty()) {
			Coordinates lastLocation = trail.get(trail.size() - 1);
			if (!lastLocation.equals(location) 
					&& (lastLocation.getDistance(location) >= TerrainElevation.STEP_KM
					&& !trail.contains(location)))
				trail.add(location);
		} else if (!trail.contains(location)) {
			trail.add(location);
		}
	}

	/**
	 * Gets the resource type id that this vehicle uses for fuel.
	 *
	 * @return resource type id
	 */
	public int getFuelTypeID() {
		return spec.getFuelType();
	}
	
	/**
	 * Gets the fuel type of this vehicle.
	 *
	 * @return fuel type string
	 */
	public String getFuelTypeStr() {
		return spec.getFuelTypeStr();
	}
	
	/**
	 * Gets the estimated distance traveled in one sol.
	 *
	 * @return distance traveled (km)
	 */
	public double getEstimatedTravelDistancePerSol() {
		// Return estimated average speed in km / sol.
		return getBaseSpeed() * VehicleSpec.ESTIMATED_TRAVEL_HOURS_PER_SOL;
	}

	/**
	 * Checks if the vehicle's emergency beacon is turned on.
	 *
	 * @return true if beacon is on.
	 */
	public boolean isBeaconOn() {
		return emergencyBeacon;
	}

	/**
	 * Sets the vehicle's emergency beacon on or off.
	 *
	 * @param state true if beacon is to be on.
	 */
	public void setEmergencyBeacon(boolean state) {
		if (emergencyBeacon != state) {
			emergencyBeacon = state;
			fireUnitUpdate(UnitEventType.EMERGENCY_BEACON_EVENT);
		}
	}

	/**
	 * Checks if the item is salvaged.
	 *
	 * @return true if salvaged.
	 */
	public boolean isSalvaged() {
		return isSalvaged;
	}

	/**
	 * Indicates the start of a salvage process on the item.
	 *
	 * @param info       the salvage process info.
	 * @param settlement the settlement where the salvage is taking place.
	 */
	public void startSalvage(SalvageProcessInfo info, int settlement) {
		salvageInfo = new SalvageInfo(this, info, settlement, masterClock.getMarsTime());
		isSalvaged = true;
	}

	/**
	 * Gets the salvage info.
	 *
	 * @return salvage info or null if item not salvaged.
	 */
	public SalvageInfo getSalvageInfo() {
		return salvageInfo;
	}

	/**
	 * Finds a new parking location and facing.
	 */
	public void findNewParkingLoc() {

		Settlement settlement = getSettlement();
		if (settlement == null) {
			logger.severe(this, "Not found to be parked in a settlement.");
			return;
		}

		LocalPosition centerLoc = LocalPosition.DEFAULT_POSITION;

		int weight = 2;

		List<Building> evas = settlement.getBuildingManager()
				.getBuildingsOfSameCategoryNZone0(BuildingCategory.EVA);
		int numGarages = settlement.getBuildingManager().getGarages().size();
		int total = (int)(evas.size() + numGarages * weight - 1);
		if (total < 0)
			total = 0;
		int rand = RandomUtil.getRandomInt(total);

		if (rand != 0) {
			// Try parking near the eva for short walk
			if (rand < evas.size()) {
				Building eva = evas.get(rand);
				centerLoc = eva.getPosition();
			}

			else {
				// Try parking near a garage					
				Building garage = BuildingManager.getAGarage(getSettlement());
				if (garage != null) {
					centerLoc = garage.getPosition();
				}
			}
		}
		else {
			// Try parking near a garage
			// Get a nearby garage (but doesn't go in)
			Building garage = BuildingManager.getAGarage(getSettlement());
			if (garage != null) {
				centerLoc = garage.getPosition();
			}
		}

		// Place the vehicle starting from the settlement center (0,0).

		int oX = 10;
		int oY = 10;
		double newFacing = 0D;
		LocalPosition newLoc = null;
		int step = 2;
		boolean foundGoodLocation = false;

		boolean isSmallVehicle = getVehicleType() == VehicleType.DELIVERY_DRONE
				|| getVehicleType() == VehicleType.LUV;

		double d = VEHICLE_CLEARANCE_0;
		if (isSmallVehicle)
			d = VEHICLE_CLEARANCE_1;
		
		// Note: enlarge (times 1.25) the dimension of a vehicle to avoid getting 
		// trapped within those enclosed space surrounded by buildings or hallways.
		
		double w = getWidth() * d * 1.25;
		double l = getLength() * d * 1.25;
		
		// Note: May need a more permanent solution by figuring out how to detect those enclosed space
		
		int count = 0;
		
		// Try iteratively outward from 10m to 500m distance range.
		for (int x = oX; (x < 2000) && !foundGoodLocation; x+=step) {
			// Try random locations at each distance range.
			for (int y = oY; (y < 2000) && !foundGoodLocation; y++) {
				double distance = Math.max(y, RandomUtil.getRandomDouble(-.5*x, x) + y);
				double radianDirection = RandomUtil.getRandomDouble(Math.PI * 2D);
				
				newLoc = centerLoc.getPosition(distance, radianDirection);
				newFacing = RandomUtil.getRandomDouble(360D);

				// Check if new vehicle location collides with anything.
				
				// Note: excessive calling increase CPU Util
				foundGoodLocation = LocalAreaUtil.isObjectCollisionFree(this, w, l,
								newLoc.getX(), newLoc.getY(), 
								newFacing, getCoordinates());
				
				count++;
			}
		}

		if (foundGoodLocation) {
			setParkedLocation(newLoc, newFacing);
			logger.info(this, "Moved to new parking loc on iteration " + count + ".");
		}
		else {
			logger.info(this, "Parking loc not found.");
		}
	}
	
	/**
	 * Relocates a vehicle. 
	 */
	public void relocateVehicle() {
		// Remove the vehicle from a garage if it's inside
		BuildingManager.removeFromGarage(this);
		// Note: No need to call below separately to find a new outside location for parking
		// Calling removeFromGarage above will take care of finding new parking loc
		// findNewParkingLoc();
	}

	public static double getFuelRangeErrorMargin() {
		return fuelRangeErrorMargin;
	}

	public static double getLifeSupportRangeErrorMargin() {
		return lifeSupportRangeErrorMargin;
	}

	public int getAssociatedSettlementID() {
		return associatedSettlementID;
	}

	/**
	 * Gets the settlement the person is currently associated with.
	 *
	 * @return associated settlement or null if none.
	 */
	@Override
	public Settlement getAssociatedSettlement() {
		return unitManager.getSettlementByID(associatedSettlementID);
	}

	/**
	 * Is the vehicle outside of a settlement but within its vicinity ?
	 *
	 * @return
	 */
	public boolean isRightOutsideSettlement() {
        return getLocationStateType() == LocationStateType.SETTLEMENT_VICINITY;
	}


	@Override
	public Building getBuildingLocation() {
		return getGarage();
	}

	/**
	 * Checks if this vehicle is involved in a mission.
	 */
	public Mission getMission() {
		return mission;
	}

	public void setMission(Mission newMission) {
		this.mission = newMission;
	}

	/**
	 * Checks if this vehicle not in a settlement and is outside on a mission on the surface of Mars.
	 *
	 * @return true if yes
	 */
	public boolean isOutsideOnMarsMission() {
		return LocationStateType.MARS_SURFACE == currentStateType;
	}

	/**
	 * Checks if the person is in a moving vehicle.
	 *
	 * @param person the person.
	 * @return true if person is in a moving vehicle.
	 */
	public static boolean inMovingRover(Person person) {

		boolean result = false;

		if (person.isInVehicle()) {
			result = person.getVehicle().getPrimaryStatus() == StatusType.MOVING;
		}

		return result;
	}

	/**
	 * Gets the specific base wear life time of this vehicle (in msols).
	 *
	 * @return
	 */
	public double getBaseWearLifetime() {
		return baseWearLifetime;
	}

	@Override
	public UnitType getUnitType() {
		return UnitType.VEHICLE;
	}

	/**
	 * Gets the holder's unit instance.
	 *
	 * @return the holder's unit instance
	 */
	@Override
	public Unit getHolder() {
		return this;
	}

	/**
	 * Is this unit empty ?
	 *
	 * @return true if this unit doesn't carry any resources or equipment
	 */
	public boolean isEmpty() {
		return eqmInventory.isEmpty();
	}

	/**
	 * Gets the total mass on this vehicle (not including vehicle's weight).
	 *
	 * @return
	 */
	@Override
	public double getStoredMass() {
		return eqmInventory.getStoredMass();
	}

	/**
	 * Gets the equipment list.
	 *
	 * @return
	 */
	@Override
	public Set<Equipment> getEquipmentSet() {
		return eqmInventory.getEquipmentSet();
	}

	/**
	 * Gets the container set.
	 *
	 * @return
	 */
	@Override
	public Set<Equipment> getContainerSet() {
		return eqmInventory.getContainerSet();
	}

	/**
	 * Gets the EVA suit set.
	 * 
	 * @return
	 */
	@Override
	public Set<Equipment> getSuitSet() {
		return eqmInventory.getSuitSet();
	}
	
	/**
	 * Finds all of the containers of a particular type (excluding EVA suit).
	 *
	 * @return collection of containers or empty collection if none.
	 */
	@Override
	public Collection<Container> findContainersOfType(EquipmentType type){
		return eqmInventory.findContainersOfType(type);
	}

	/**
	 * Does this unit possess an equipment of this type ?
	 *
	 * @param typeID
	 * @return
	 */
	@Override
	public boolean containsEquipment(EquipmentType type) {
		return eqmInventory.containsEquipment(type);
	}

	/**
	 * Adds an equipment to this unit.
	 *
	 * @param equipment
	 * @return true if it can be carried
	 */
	@Override
	public boolean addEquipment(Equipment e) {
		if (eqmInventory.addEquipment(e)) {
			fireUnitUpdate(UnitEventType.ADD_ASSOCIATED_EQUIPMENT_EVENT, this);
			return true;
		}
		return false;
	}

	/**
	 * Removes an equipment.
	 *
	 * @param equipment
	 */
	@Override
	public boolean removeEquipment(Equipment equipment) {
		return eqmInventory.removeEquipment(equipment);
	}

	/**
	 * Stores the item resource.
	 *
	 * @param resource the item resource
	 * @param quantity
	 * @return excess quantity that cannot be stored
	 */
	@Override
	public int storeItemResource(int resource, int quantity) {
		return eqmInventory.storeItemResource(resource, quantity);
	}

	/**
	 * Retrieves the item resource.
	 *
	 * @param resource
	 * @param quantity
	 * @return quantity that cannot be retrieved
	 */
	@Override
	public int retrieveItemResource(int resource, int quantity) {
		return eqmInventory.retrieveItemResource(resource, quantity);
	}

	/**
	 * Retrieves the resource.
	 *
	 * @param resource
	 * @param quantity
	 * @return quantity that cannot be retrieved
	 */
	@Override
	public double retrieveAmountResource(int resource, double quantity) {
		return eqmInventory.retrieveAmountResource(resource, quantity);
	}

	/**
	 * Stores the resource.
	 *
	 * @param resource
	 * @param quantity
	 * @return excess quantity that cannot be stored
	 */
	@Override
	public double storeAmountResource(int resource, double quantity) {
		return eqmInventory.storeAmountResource(resource, quantity);
	}

	/**
	 * Gets the item resource stored.
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public int getItemResourceStored(int resource) {
		return eqmInventory.getItemResourceStored(resource);
	}

	/**
	 * Gets the capacity of a particular amount resource.
	 *
	 * @param resource
	 * @return capacity
	 */
	@Override
	public double getAmountResourceCapacity(int resource) {
		return eqmInventory.getAmountResourceCapacity(resource);
	}

	/**
	 * Obtains the remaining storage space of a particular amount resource.
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public double getAmountResourceRemainingCapacity(int resource) {
		return eqmInventory.getAmountResourceRemainingCapacity(resource);
	}

	/**
	 * Does it have unused space or capacity for a particular resource ?
	 * 
	 * @param resource
	 * @return
	 */
	@Override
	public boolean hasAmountResourceRemainingCapacity(int resource) {
		return eqmInventory.hasAmountResourceRemainingCapacity(resource);
	}
	
	/**
     * Gets the total capacity that it can hold.
     *
     * @return total capacity (kg).
     */
	@Override
	public double getCargoCapacity() {
		return eqmInventory.getCargoCapacity();
	}

	/**
	 * Gets the amount resource stored.
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public double getAmountResourceStored(int resource) {
		return eqmInventory.getAmountResourceStored(resource);
	}
	
	/**
	 * Gets all the amount resource resource stored, including inside equipment.
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public double getAllAmountResourceStored(int resource) {
		return eqmInventory.getAllAmountResourceStored(resource);
	}
	
	/**
	 * Finds the number of empty containers of a class that are contained in storage and have
	 * an empty inventory.
	 *
	 * @param containerClass  the unit class
	 * @param brandNew  does it include brand new bag only
	 * @return number of empty containers
	 */
	@Override
	public int findNumEmptyContainersOfType(EquipmentType containerType, boolean brandNew) {
		return eqmInventory.findNumEmptyContainersOfType(containerType, brandNew);
	}

	/**
	 * Finds the number of containers of a particular type.
	 * 
	 * Note: will not count EVA suits.
	 *
	 * @param containerType the equipment type
	 * @return number of empty containers
	 */
	@Override
	public int findNumContainersOfType(EquipmentType containerType) {
		return eqmInventory.findNumContainersOfType(containerType);
	}

	/**
	 * Finds a container in storage.
	 *
	 * Note: will not count EVA suits.
	 * 
	 * @param containerType
	 * @param empty does it need to be empty ?
	 * @param resource If -1 then resource doesn't matter
	 * @return instance of container or null if none
	 */
	@Override
	public Container findContainer(EquipmentType containerType, boolean empty, int resource) {
		return eqmInventory.findContainer(containerType, empty, resource);
	}


	/**
	 * Finds the number of EVA suits (may or may not have resources inside) that are contained in storage.
	 *
	 * @return number of EVA suits
	 */
	public int findNumEVASuits() {
		return getSuitSet().size();
	}

	/**
	 * Gets a set of item resources in storage.
	 * 
	 * @return  a set of resources
	 */
	@Override
	public Set<Integer> getItemResourceIDs() {
		return eqmInventory.getItemResourceIDs();
	}

	/**
	 * Gets a set of resources in storage.
	 * 
	 * @return  a set of resources
	 */
	@Override
	public Set<Integer> getAmountResourceIDs() {
		return eqmInventory.getAmountResourceIDs();
	}
	
	/**
	 * Gets all stored amount resources in eqmInventory, including inside equipment.
	 *
	 * @return all stored amount resources.
	 */
	@Override
	public Set<Integer> getAllAmountResourceIDs() {
		return eqmInventory.getAllAmountResourceIDs();
	}
	
	/**
	 * Obtains the remaining general storage space.
	 *
	 * @return quantity
	 */
	@Override
	public double getRemainingCargoCapacity() {
		return eqmInventory.getRemainingCargoCapacity();
	}

	/**
	 * Does it have this item resource ?
	 *
	 * @param resource
	 * @return
	 */
	@Override
	public boolean hasItemResource(int resource) {
		return eqmInventory.hasItemResource(resource);
	}

	/**
	 * Gets the remaining quantity of an item resource.
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public int getItemResourceRemainingQuantity(int resource) {
		return eqmInventory.getItemResourceRemainingQuantity(resource);
	}

	/**
	 * Sets the unit's container unit.
	 *
	 * @param newContainer the unit to contain this unit.
	 */
	public boolean setContainerUnit(Unit newContainer) {
		if (newContainer != null) {
			Unit cu = getContainerUnit();
			
			if (newContainer.equals(cu)) {
				return false;
			}

			// 1. Set Coordinates
			if (newContainer.getUnitType() == UnitType.MARS) {
				// Since it's on the surface of Mars,
				// First set its initial location to its old parent's location as it's leaving its parent.
				// Later it may move around and updates its coordinates by itself
//				setCoordinates(getContainerUnit().getCoordinates());
			}
//			else {
//				setCoordinates(newContainer.getCoordinates());
//			}
			
			// 2. Set new LocationStateType
			// 2a. If the old cu is a settlement
			//     and the new cu is mars surface,
			//     then location state is within settlement vicinity
			if (cu != null 
				&& (cu.getUnitType() == UnitType.SETTLEMENT
					|| cu.getUnitType() == UnitType.BUILDING)
				&& newContainer.getUnitType() == UnitType.MARS) {
					setLocationStateType(LocationStateType.SETTLEMENT_VICINITY);
			}	
			else {
				// 2b. If old cu is null (parking within settlement vicinity
				//     and the new cu is mars surface,
				//     then new location state is mars surface
				updateVehicleState(newContainer);
			}
			
			// 3. Set containerID
			setContainerID(newContainer.getIdentifier());
			
			// 4. Fire the container unit event
			fireUnitUpdate(UnitEventType.CONTAINER_UNIT_EVENT, newContainer);
		}
		return true;
	}

	/**
	 * Updates the location state type of a vehicle.
	 *
	 * @apiNote (1) : WITHIN_SETTLEMENT_VICINITY is the intermediate state between being INSIDE_SETTLEMENT (in a garage) and being OUTSIDE_ON_MARS.
	 *
	 * @apiNote (2) : WITHIN_SETTLEMENT_VICINITY can be used by a person or a vehicle.
	 *
	 * @apiNote (3) : If a vehicle may be in a garage inside a building, this vehicle is INSIDE_SETTLEMENT.
	 *                If a vehicle is parked right outside a settlement, this vehicle is WITHIN_SETTLEMENT_VICINITY.
	 *
	 * @param newContainer
	 */
	public void updateVehicleState(Unit newContainer) {
		if (newContainer == null) {
			setLocationStateType(LocationStateType.UNKNOWN);
			return;
		}

		setLocationStateType(getNewLocationState(newContainer));
	}

	/**
	 * Updates the location state type directly.
	 *
	 * @param type
	 */
	public void updateLocationStateType(LocationStateType type) {
		currentStateType = type;
	}

	/**
	 * Gets the location state type based on the type of the new container unit.
	 *
	 * @param newContainer
	 * @return {@link LocationStateType}
	 */
	private LocationStateType getNewLocationState(Unit newContainer) {

		if (newContainer.getUnitType() == UnitType.SETTLEMENT) {
			if (isInGarage()) {
				return LocationStateType.INSIDE_SETTLEMENT;
			}
			else
				return LocationStateType.SETTLEMENT_VICINITY;
		}

//		if (newContainer.getUnitType() == UnitType.BUILDING)
//			return LocationStateType.INSIDE_SETTLEMENT;

		if (newContainer.getUnitType() == UnitType.VEHICLE)
			return LocationStateType.INSIDE_VEHICLE;

		if (newContainer.getUnitType() == UnitType.CONSTRUCTION)
			return LocationStateType.MARS_SURFACE;

		if (newContainer.getUnitType() == UnitType.PERSON)
			return LocationStateType.ON_PERSON_OR_ROBOT;

		if (newContainer.getUnitType() == UnitType.MARS)
			return LocationStateType.MARS_SURFACE;

		return null;
	}

	/**
	 * Is this unit inside a settlement ?
	 *
	 * @return true if the unit is inside a settlement
	 */
	@Override
	public boolean isInSettlement() {

		if (containerID <= MARS_SURFACE_UNIT_ID)
			return false;

		// if the vehicle is parked in a garage
		if (LocationStateType.INSIDE_SETTLEMENT == currentStateType)
			return true;

		// if the vehicle is parked in the vicinity of a settlement and not in a garage
		if (LocationStateType.SETTLEMENT_VICINITY == currentStateType)
			return true;

		if (getContainerUnit().getUnitType() == UnitType.SETTLEMENT
				&& ((Settlement)(getContainerUnit())).containsVicinityParkedVehicle((Vehicle)this)) {
			return true;
		}

		return false;
	}

	/**
	 * Transfers the unit from one owner to another owner.
	 *
	 * @param origin {@link Unit} the original container unit
	 * @param destination {@link Unit} the destination container unit
	 */
	public boolean transfer(Unit destination) {
		boolean leaving = false;
		boolean arriving = false;
		boolean transferred = false;
		// Set the old container unit
		Unit cu = getContainerUnit();

		if (cu.getUnitType() == UnitType.MARS) {
			transferred = ((MarsSurface)cu).removeVehicle(this);
			arriving = true;
		}
		else if (cu.getUnitType() == UnitType.SETTLEMENT) {
			Settlement currentBase = (Settlement)cu;
			transferred = currentBase.removeVicinityParkedVehicle(this);
			leaving = true;
			// Q: do we need to set the coordinate to the settlement one last time prior to leaving
//			setCoordinates(currentBase.getCoordinates());
		}

		if (transferred) {
			if (destination.getUnitType() == UnitType.MARS) {
				transferred = ((MarsSurface)destination).addVehicle(this);
				leaving = leaving && true;
			}
			else if (cu.getUnitType() == UnitType.SETTLEMENT) {
				transferred = ((Settlement)destination).addVicinityVehicle(this);
			}

			if (!transferred) {
				logger.warning(this, 60_000L, "Cannot be stored into " + destination + ".");
				// NOTE: need to revert back the storage action
			}
			else {
				if (leaving && isInGarage()) {
					BuildingManager.removeFromGarage(this);
				}
				
				// Set the new container unit (which will internally set the container unit id)
				setContainerUnit(destination);
				// Fire the unit event type
				getContainerUnit().fireUnitUpdate(UnitEventType.INVENTORY_STORING_UNIT_EVENT, this);
				// Fire the unit event type
				getContainerUnit().fireUnitUpdate(UnitEventType.INVENTORY_RETRIEVING_UNIT_EVENT, this);
			}
		}
		else {
			logger.warning(this, 60_000L, "Cannot be retrieved from " + cu + ".");
			// NOTE: need to revert back the retrieval action
		}

		return transferred;
	}

    /**
	 * Gets the amount of fuel (kg) needed for a trip of a given distance (km).
	 *
	 * @param tripDistance   the distance (km) of the trip.
	 * @param useMargin      Apply safety margin when loading resources before embarking if true.
	 * @return amount of fuel needed for trip (kg)
	 */
	public double getFuelNeededForTrip(double tripDistance, boolean useMargin) {
		return vehicleController.getFuelNeededForTrip(this, tripDistance, 
				getEstimatedFuelEconomy(), useMargin);
	}
	
	/**
	 * Checks if battery charging is needed and charge the vehicle.
	 * 
	 * @param settlement
	 */ 
	public void chargeVehicle(ClockPulse pulse, Settlement settlement) {
		// Gets the time elapse in this frame
		double time = pulse.getElapsed();
		// Convert time to hours
		double hrs = time * MarsTime.HOURS_PER_MILLISOL;
		// Calculate max charing power that battery supports
		double allowedPower = getController().getBattery().getMaxPowerCharging(hrs);
		// Check if charging is needed
		if (allowedPower > 0) {
			// Retrieve energy from the settlement's power grid
			double retrieved = settlement.getPowerGrid().retrieveStoredEnergy(allowedPower * hrs, time);
			// Charge the vehicle
			getController().getBattery().provideEnergy(retrieved, hrs); 
			
			logger.info(this, 20_000L, "Supplying " + Math.round(retrieved * 10.0)/10.0 + " kWh of energy during charging.");
		}
		else {
			setCharging(false);
		}
	}
	
	public boolean isCharging() {
		return isCharging;
	}
	
	public void setCharging(boolean value) {
		isCharging = value;
	}
	
	public EquipmentInventory getEquipmentInventory() {
		return eqmInventory;
	}

	public VehicleController getController() {
		return vehicleController;
	}
	
	/** 
	 * Gets the VehicleSpec instance. 
	 */
	public VehicleSpec getVehicleSpec() {
		return spec;
	}
	
	/**
	 * Compares if an object is the same as this unit.
	 *
	 * @param obj
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		Vehicle v = (Vehicle) obj;
		return this.getIdentifier() == v.getIdentifier();
	}

	/**
	 * Gets the hash code value.
	 *
	 * @return hash code
	 */
	@Override
	public int hashCode() {
		return getIdentifier() % 32;
	}

	@Override
	public void destroy() {
		super.destroy();

		malfunctionManager.destroy();
		malfunctionManager = null;
		direction = null;
		vehicleOperator = null;
		trail.clear();
		trail = null;
		towingVehicle = null;
		statusTypes.clear();
		statusTypes = null;
		if (salvageInfo != null)
			salvageInfo.destroy();
		salvageInfo = null;
	}

    public String getChildContext() {
        return getContext() + ENTITY_SEPERATOR + getName();
    }
}