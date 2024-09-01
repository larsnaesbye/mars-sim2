/*
 * Mars Simulation Project
 * Unit.java
 * @date 2023-05-09
 * @author Scott Davis
 */
package com.mars_sim.core;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import com.mars_sim.core.environment.Weather;
import com.mars_sim.core.location.LocationStateType;
import com.mars_sim.core.location.LocationTag;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.person.ai.mission.MissionManager;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.MasterClock;
import com.mars_sim.core.vehicle.Vehicle;

/**
 * The Unit class is the abstract parent class to all units in the simulation.
 * Units include people, vehicles and settlements. This class provides data
 * members and methods common to all units.
 */
public abstract class Unit implements UnitIdentifer, Comparable<Unit> {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(Unit.class.getName());

	public static final int MOON_UNIT_ID = -2;
	public static final int OUTER_SPACE_UNIT_ID = -1;
	public static final int MARS_SURFACE_UNIT_ID = 0;
	public static final Integer UNKNOWN_UNIT_ID = -3;

	// Data members
	/** The unit containing this unit. */
	protected Integer containerID = UNKNOWN_UNIT_ID;

	// Unique Unit identifier
	private int identifier;
	/** The mass of the unit without inventory. */
	private double baseMass;
	/** The last pulse applied. */
	private long lastPulse = 0;
	
	private String name;
	private String description = "No Description";
	/** Commander's notes on this unit. */
	private String notes = "";
	/** The unit's location tag. */
	private LocationTag tag;
	/** The unit's coordinates. */
	private Coordinates location;

	/** The unit's current location state. */
	protected LocationStateType currentStateType;
	/** Unit listeners. */
	private transient Set<UnitListener> listeners;

	protected static SimulationConfig simulationConfig = SimulationConfig.instance();

	protected static MasterClock masterClock;

	protected static UnitManager unitManager;
	protected static MissionManager missionManager;

	protected static Weather weather;

	// File for diagnostics output
	private static PrintWriter diagnosticFile = null;

	/**
	 * Enable the detailed diagnostics
	 *
	 * @throws FileNotFoundException
	 */
	public static void setDiagnostics(boolean diagnostics) throws FileNotFoundException {
		if (diagnostics) {
			if (diagnosticFile == null) {
				String filename = SimulationRuntime.getLogDir() + "/unit-create.txt";
				diagnosticFile = new PrintWriter(filename);
				logger.config("Diagnostics enabled to " + filename);
			}
		} else if (diagnosticFile != null) {
			diagnosticFile.close();
			diagnosticFile = null;
		}
	}

	/**
	 * Log the creation of a new Unit
	 *
	 * @param entry
	 */
	private static void logCreation(Unit entry) {
		StringBuilder output = new StringBuilder();
		output.append(masterClock.getMarsTime().getDateTimeStamp()).append(" Id:").append(entry.getIdentifier())
				.append(" Type:").append(entry.getUnitType()).append(" Name:").append(entry.getName());

		synchronized (diagnosticFile) {
			diagnosticFile.println(output.toString());
			diagnosticFile.flush();
		}
	}

	/**
	 * Gets the identifier of this unit.
	 */
	public final int getIdentifier() {
		return identifier;
	}

	/**
	 * Constructor 1: the name and identifier are defined.
	 *
	 * @param name     {@link String} the name of the unit
	 * @param id Unit identifier
	 * @param containerId Identifier of the container
	 */
	protected Unit(String name, int id, int containerId) {
		// Initialize data members from parameters
		this.name = name;
//		this.description = name;
		this.baseMass = 0;
		this.identifier = id;
		this.containerID = containerId;
		
		// For now, set currentStateType to MARS_SURFACE
		currentStateType = LocationStateType.MARS_SURFACE;
	}

	/**
	 * Constructor 2: where the name and location are defined.
	 *
	 * @param name     {@link String} the name of the unit
	 * @param location {@link Coordinates} the unit's location
	 */
	protected Unit(String name, Coordinates location) {
		// Initialize data members from parameters
		this.name = name;
//		this.description = name;
		this.baseMass = 0;

		if (masterClock != null) {
			// Needed for maven test
			this.lastPulse = masterClock.getNextPulse() - 1;

			// Creates a new location tag instance for each unit
			tag = new LocationTag(this);
	
			// Calculate the new Identifier for this type
			identifier = unitManager.generateNewId(getUnitType());
		}

		// Define the default LocationStateType of an unit at the start of the sim
		// Instantiate Inventory as needed. Still needs to be pushed to subclass
		// constructors
		switch (getUnitType()) {
		case BUILDING, CONTAINER, EVA_SUIT, PERSON, ROBOT:
			currentStateType = LocationStateType.INSIDE_SETTLEMENT;
			// Why no containerID ?
			break;
			
		case VEHICLE:
			currentStateType = LocationStateType.SETTLEMENT_VICINITY;
			containerID = MARS_SURFACE_UNIT_ID;
			break;

		case CONSTRUCTION, MARS, SETTLEMENT:
			currentStateType = LocationStateType.MARS_SURFACE;
			containerID = MARS_SURFACE_UNIT_ID;
			break;

		case MOON:
			currentStateType = LocationStateType.MOON;
			containerID = MOON_UNIT_ID;
			break;
			
		default:
			throw new IllegalStateException("Do not know Unittype " + getUnitType());
		}

		if (location != null) {
			// Set the unit's location coordinates
			setCoordinates(location);
		}
		else
			// Set to (0, 0) when still initializing Settlement instance
			this.location = new Coordinates(0D, 0D);

		if (diagnosticFile != null) {
			logCreation(this);
		}
	}

	/**
	 * What logical UnitType of this object in terms of the management. This is NOT
	 * a direct mapping to the concrete subclass of Unit since some logical
	 * UnitTypes can have multiple implementation, e.g. Equipment.
	 *
	 * @return
	 */
	public abstract UnitType getUnitType();

	/**
	 * Is this time pulse valid for the Unit. Has it been already applied? The logic
	 * on this method can be commented out later on
	 *
	 * @param pulse Pulse to apply
	 * @return Valid to accept
	 */
	protected boolean isValid(ClockPulse pulse) {
		long newPulse = pulse.getId();
		boolean result = (newPulse > lastPulse);
		if (result) {
			long expectedPulse = lastPulse + 1;
			if (expectedPulse != newPulse) {
				// Pulse out of sequence; maybe missed one
				logger.warning(getName() + " expected pulse #" + expectedPulse + " but received " + newPulse);
			}
			lastPulse = newPulse;
		} else {
			if (newPulse == lastPulse) {
				// This is a newly added unit such as person/vehicle/robot in a resupply transport.
				return true;
			}
			else
				logger.severe(getName() + " rejected pulse #" + newPulse + ", last pulse was " + lastPulse);
		}
		return result;
	}

	/**
	 * Changes the unit's name.
	 *
	 * @param newName new name
	 */
	public final void changeName(String newName) {
		// Create an event here ?
		setName(newName);
	}

	/**
	 * Gets the unit's name.
	 *
	 * @return the unit's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * This method assumes the Unit could be movable and change container. It identifies the
	 * appropriate container it can use. Ideally this method should be moved to a 
	 * new subclass called 'MovableUnit' that encapsulates some positioning methods 
	 * not applicable to Structures.
	 */
	public String getContext() {
		if (isInSettlement()) {
			var b = getBuildingLocation();
			if (b != null) {
				return b.getChildContext();
			}
			else {
				return getAssociatedSettlement().getName();
			}
		}
		else if (isInVehicle()) {
			return getVehicle().getChildContext();
		}
		else if (isOutside()) {
			return getCoordinates().getFormattedString();
		}
		else {
			return getContainerUnit().getName();
		}
	}
	/**
	 * Gets the unit's shortened name.
	 *
	 * @return the unit's shortened name
	 */
	public String getShortenedName() {
		name = name.trim();
		int num = name.length();

		boolean hasSpace = name.matches("^\\s*$");

		if (hasSpace) {
			int space = name.indexOf(" ");

			String oldFirst = name.substring(0, space);
			String oldLast = name.substring(space + 1, num);
			String newFirst = oldFirst;
			String newLast = oldLast;
			String newName = name;

			if (num > 20) {

				if (oldFirst.length() > 10) {
					newFirst = oldFirst.substring(0, 10);
				} else if (oldLast.length() > 10) {
					newLast = oldLast.substring(0, 10);
				}
				newName = newFirst + " " + newLast;

			}

			return newName;
		}

		else
			return name;
	}

	/**
	 * Sets the unit's name.
	 *
	 * @param name new name
	 */
	public void setName(String name) {
		this.name = name;
		fireUnitUpdate(UnitEventType.NAME_EVENT, name);
	}

	/**
	 * Gets the unit's description.
	 *
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the unit's description.
	 *
	 * @param description new description.
	 */
	protected void setDescription(String description) {
		this.description = description;
		fireUnitUpdate(UnitEventType.DESCRIPTION_EVENT, description);
	}

	/**
	 * Gets the commander's notes on this unit.
	 *
	 * @return notes
	 */
	public String getNotes() {
		return notes;
	}

	/**
	 * Sets the commander's notes on this unit.
	 *
	 * @param notes.
	 */
	public void setNotes(String notes) {
		this.notes = notes;
		fireUnitUpdate(UnitEventType.NOTES_EVENT, notes);
	}

	/**
	 * Gets the unit's location.
	 *
	 * @return the unit's location
	 */
	public Coordinates getCoordinates() {
		if (getUnitType() == UnitType.SETTLEMENT) {	
			return location;
		}
	
		Unit cu = getContainerUnit();
		if (cu.getUnitType() == UnitType.MARS) {	
			// Since Mars surface has no coordinates, 
			// Get from its previously setting location
			return location;
		}
		
		// Unless it's on Mars surface, get its container unit's coordinates
		return cu.getCoordinates();
	}

	/**
	 * Sets unit's location coordinates.
	 *
	 * @param newLocation the new location of the unit
	 */
	public void setCoordinates(Coordinates newLocation) {
		if (location == null || !location.equals(newLocation)) {
			location = newLocation;
			fireUnitUpdate(UnitEventType.LOCATION_EVENT, newLocation);
		}
	}

	/**
	 * Gets the unit's container unit. Returns null if unit has no container unit.
	 *
	 * @return the unit's container unit
	 */
	public Unit getContainerUnit() {
		if (unitManager == null) // for maven test
			return null;
		return unitManager.getUnitByID(containerID);
	}

	public int getContainerID() {
		return containerID;
	}

	protected void setContainerID(Integer id) {
		containerID = id;
	}

	/**
	 * Gets the topmost container unit that owns this unit, either a settlement or a vehicle.
	 * If it's on the surface of Mars, then the topmost container is MarsSurface.
	 *
	 * @return the unit's topmost container unit
	 */
	public Unit getTopContainerUnit() {
		Unit topUnit = getContainerUnit();
		if (!(topUnit.getUnitType() == UnitType.MARS)) {
			while (topUnit != null && topUnit.getContainerUnit() != null
					&& !(topUnit.getContainerUnit().getUnitType() == UnitType.MARS)) {
				topUnit = topUnit.getContainerUnit();
			}
		}

		return topUnit;
	}

	/**
	 * Gets the topmost container unit that owns this unit, either a settlement or a vehicle.
	 * If it's on the surface of Mars, then the topmost container is MarsSurface.
	 *
	 * @return the unit's topmost container unit
	 */
	public int getTopContainerID() {

		int topID = getContainerUnit().getContainerID();
		if (topID != Unit.MARS_SURFACE_UNIT_ID) {
			while (topID != Unit.MARS_SURFACE_UNIT_ID) {
				topID = getContainerUnit().getContainerID();
			}
		}

		return topID;
	}

	/**
	 * Gets the unit's mass including inventory mass.
	 *
	 * @return mass of unit and inventory
	 * @throws Exception if error getting the mass.
	 */
	public double getMass() {
		// Note: this method will be overridden by those inheriting this Unit
		return baseMass;// + getStoredMass(); ?
	}

	/**
	 * Sets the unit's base mass.
	 *
	 * @param base mass (kg)
	 */
	public final void setBaseMass(double baseMass) {
		this.baseMass = baseMass;
		fireUnitUpdate(UnitEventType.MASS_EVENT);
	}

	/**
	 * Gets the base mass of the unit.
	 *
	 * @return base mass (kg)
	 */
	public double getBaseMass() {
		return baseMass;
	}
	
	/**
	 * Checks if it has a unit listener.
	 * 
	 * @param listener
	 * @return
	 */
	public synchronized boolean hasUnitListener(UnitListener listener) {
		if (listeners == null)
			return false;
		return listeners.contains(listener);
	}

	/**
	 * Adds a unit listener.
	 *
	 * @param newListener the listener to add.
	 */
	public synchronized final void addUnitListener(UnitListener newListener) {
		if (newListener == null)
			throw new IllegalArgumentException();
		if (listeners == null)
			listeners = new HashSet<>();

		synchronized(listeners) {	
			listeners.add(newListener);
		}
	}

	/**
	 * Removes a unit listener.
	 *
	 * @param oldListener the listener to remove.
	 */
	public synchronized final void removeUnitListener(UnitListener oldListener) {
		if (oldListener == null)
			throw new IllegalArgumentException();

		if (listeners != null) {
			synchronized(listeners) {
				listeners.remove(oldListener);
			}
		}
	}

	/**
	 * Fires a unit update event.
	 *
	 * @param updateType the update type.
	 */
	public final void fireUnitUpdate(UnitEventType updateType) {
		fireUnitUpdate(updateType, null);
	}

	/**
	 * Fires a unit update event.
	 *
	 * @param updateType the update type.
	 * @param target     the event target object or null if none.
	 */
	public final void fireUnitUpdate(UnitEventType updateType, Object target) {
		if (listeners == null || listeners.isEmpty()) {
			return;
		}
		final UnitEvent ue = new UnitEvent(this, updateType, target);
		synchronized (listeners) {
			for(UnitListener i : listeners) {
				try {
					// Stop listeners breaking the update thread
					i.unitUpdate(ue);
				}
				catch(RuntimeException rte) {
					logger.severe(this, "Problem executing listener " + i + " for event " + ue, rte);
				}
			}
		}
	}

	public LocationStateType getLocationStateType() {
		return currentStateType;
	}

	public void setLocationStateType(LocationStateType locationStateType) {
		currentStateType = locationStateType;
	}
	
	public LocationTag getLocationTag() {
		return tag;
	}

	public Settlement getSettlement() {
		return null;
	}
	
	/**
	 * Gets the building this unit is at.
	 *
	 * @return the building
	 */
	public Building getBuildingLocation() {
		return null;
	}

	/**
	 * Gets the associated settlement this unit is with.
	 *
	 * @return the associated settlement
	 */
	public Settlement getAssociatedSettlement() {
		return null;
	}

	/**
	 * Gets the vehicle this unit is in, null if not in vehicle.
	 *
	 * @return the vehicle
	 */
	public Vehicle getVehicle() {
		return null;
	}

	/**
	 * Is this unit inside an environmentally enclosed breathable living space such
	 * as inside a settlement or a vehicle (NOT including in an EVA Suit) ?
	 *
	 * @return true if the unit is inside a breathable environment
	 */
	public boolean isInside() {
		if (LocationStateType.INSIDE_SETTLEMENT == currentStateType
				|| LocationStateType.INSIDE_VEHICLE == currentStateType)
			return true;

		if (LocationStateType.ON_PERSON_OR_ROBOT == currentStateType)
			return getContainerUnit().isInside();

		return false;
	}

	/**
	 * Is this unit outside on the surface of Mars, including wearing an EVA Suit
	 * and being just right outside in a settlement/building/vehicle vicinity
	 * Note: being inside a vehicle (that's on a mission outside) doesn't count being outside
	 *
	 * @return true if the unit is outside
	 */
	public boolean isOutside() {
		if (LocationStateType.MARS_SURFACE == currentStateType
				|| LocationStateType.SETTLEMENT_VICINITY == currentStateType
				|| LocationStateType.VEHICLE_VICINITY == currentStateType)
			return true;

		if (LocationStateType.ON_PERSON_OR_ROBOT == currentStateType)
			return getContainerUnit().isOutside();

		return false;
	}

	/**
	 * Is this unit inside a vehicle ?
	 *
	 * @return true if the unit is inside a vehicle
	 */
	public boolean isInVehicle() {
		if (LocationStateType.INSIDE_VEHICLE == currentStateType)
			return true;

		if (LocationStateType.ON_PERSON_OR_ROBOT == currentStateType)
			return getContainerUnit().isInVehicle();

		return false;
	}

	/**
	 * Is this unit inside a settlement ?
	 *
	 * @return true if the unit is inside a settlement
	 */
	public abstract boolean isInSettlement();

	/**
	 * Is this unit in the vicinity of a settlement ?
	 *
	 * @return true if the unit is inside a settlement
	 */
	public boolean isInSettlementVicinity() {
		return tag.isInSettlementVicinity();
	}

	/**
	 * Is this unit inside a vehicle in a garage ?
	 *
	 * @return true if the unit is in a vehicle inside a garage
	 */
	public boolean isInVehicleInGarage() {
		Unit cu = getContainerUnit();
		if (cu.getUnitType() == UnitType.VEHICLE) {
			// still inside the garage
			return ((Vehicle)cu).isInGarage();
		}
		return false;
	}

	/**
	 * Loads instances.
	 *
	 */
	public static void initializeInstances(MasterClock c0, UnitManager um,
			Weather w, MissionManager mm) {
		masterClock = c0;
		weather = w;
		unitManager = um;
		missionManager = mm;
	}

	/**
	 * Compares this object with the specified object for order.
	 *
	 * @param o the Object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 */
	@Override
	public int compareTo(Unit o) {
		return name.compareToIgnoreCase(o.name);
	}

	/**
	 * String representation of this Unit.
	 *
	 * @return The units name.
	 */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * Compares if an object is the same as this unit
	 *
	 * @param obj
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		return this.getIdentifier() == ((Unit) obj).getIdentifier();
	}

	/**
	 * Gets the hash code for this object.
	 *
	 * @return hash code.
	 */
	public int hashCode() {
		int hashCode = getIdentifier() % 32;
		return hashCode;
	}

	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		location = null;
		name = null;
		description = null;
		listeners = null;
	}

}
