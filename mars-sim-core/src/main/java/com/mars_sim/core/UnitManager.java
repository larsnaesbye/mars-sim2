/**
 * Mars Simulation Project
 * UnitManager.java
 * @date 2023-06-15
 * @author Scott Davis
 */
package com.mars_sim.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import com.mars_sim.core.authority.Authority;
import com.mars_sim.core.data.UnitSet;
import com.mars_sim.core.environment.MarsSurface;
import com.mars_sim.core.environment.OuterSpace;
import com.mars_sim.core.equipment.Equipment;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.malfunction.MalfunctionFactory;
import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.moon.Moon;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.construction.ConstructionSite;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.Temporal;
import com.mars_sim.core.vehicle.Vehicle;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * The UnitManager class contains and manages all units in virtual Mars. It has
 * methods for getting information about units. It is also responsible for
 * creating all units on its construction. There should be only one instance of
 * this class and it should be constructed and owned by Simulation.
 */
public class UnitManager implements Serializable, Temporal {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(UnitManager.class.getName());

	public static final int THREE_SHIFTS_MIN_POPULATION = 6;

	//  The number of bits in the identifier field to use for the type element
	private static final int TYPE_BITS = 4;
	private static final int TYPE_MASK = (1 << (TYPE_BITS)) - 1;
	private static final int MAX_BASE_ID = (1 << (32-TYPE_BITS)) - 1;

	// Data members
	/** Flag true if the class has just been loaded. */
	public boolean justLoaded = true;
	/** Counter of unit identifiers. */
	private int uniqueId = 0;
	/** The commander's unique id . */
	private int commanderID = -1;
	/** The core engine's original build. */
	private String originalBuild;

	/** List of unit manager listeners. */
	private transient Map<UnitType, Set<UnitManagerListener>> listeners;

	private transient ExecutorService executor;

	private transient Set<Authority> sponsorSet = new HashSet<>();
	
	private transient Set<SettlementTask> settlementTasks = new HashSet<>();
	/** Map of equipment types and their numbers. */
	private Map<String, Integer> unitCounts = new HashMap<>();
	/** A map of all map display units (settlements and vehicles). */
	private Set<Unit> displayUnits;
	/** A map of settlements with its unit identifier. */
	private Map<Integer, Settlement> lookupSettlement;
	/** A map of sites with its unit identifier. */
	private Map<Integer, ConstructionSite> lookupSite;
	/** A map of persons with its unit identifier. */
	private Map<Integer, Person> lookupPerson;
	/** A map of robots with its unit identifier. */
	private Map<Integer, Robot> lookupRobot;
	/** A map of vehicle with its unit identifier. */
	private Map<Integer, Vehicle> lookupVehicle;
	/** A map of equipment (excluding robots and vehicles) with its unit identifier. */
	private Map<Integer, Equipment> lookupEquipment;
	/** A map of building with its unit identifier. */
	private Map<Integer, Building> lookupBuilding;
	/** A map of settlements with its coordinates. */
	private Map<Coordinates, Integer> settlementCoordinateMap;
	
	private static SimulationConfig simulationConfig = SimulationConfig.instance();
	private static Simulation sim = Simulation.instance();

	private static MalfunctionFactory factory = sim.getMalfunctionFactory();

	private static ThreadLocal<Settlement> activeSettlement = new ThreadLocal<>();

	/** The instance of Mars Surface. */
	private MarsSurface marsSurface;
	
	/** The instance of Outer Space. */
	private OuterSpace outerSpace;
	
	/** The instance of Moon. */
	private Moon moon;
	
	/**
	 * Constructor.
	 */
	public UnitManager() {
		// Initialize unit collection
		lookupSite       = new ConcurrentHashMap<>();
		lookupSettlement = new ConcurrentHashMap<>();
		lookupPerson     = new ConcurrentHashMap<>();
		lookupRobot      = new ConcurrentHashMap<>();
		lookupEquipment  = new ConcurrentHashMap<>();
		lookupVehicle    = new ConcurrentHashMap<>();
		lookupBuilding   = new ConcurrentHashMap<>();
		
		settlementCoordinateMap = new HashMap<>();
	}

	/**
	 * Gets the appropriate Unit Map for a Unit type.
	 * 
	 * @param type
	 * @return
	 */
	private Map<Integer, ? extends Unit> getUnitMap(UnitType type ) {
		Map<Integer,? extends Unit> map = null;

		switch (type) {
		case PERSON:
			map = lookupPerson;
			break;
		case VEHICLE:
			map = lookupVehicle;
			break;
		case SETTLEMENT:
			map = lookupSettlement;
			break;
		case BUILDING:
			map = lookupBuilding;
			break;
		case EVA_SUIT:
		case CONTAINER:
			map = lookupEquipment;
			break;
		case ROBOT:
			map = lookupRobot;
			break;
		case CONSTRUCTION:
			map = lookupSite;
			break;
		default:
			throw new IllegalArgumentException("No Unit map for type " + type);
		}

		return map;
	}

	/**
	 * Gets the Unit of a certain type matching the name.
	 * 
	 * @param type The UnitType to search for
	 * @param name Name of the unit
	 */
	public Unit getUnitByName(UnitType type, String name) {
		Map<Integer,? extends Unit> map = getUnitMap(type);
		for(Unit u : map.values()) {
			if (u.getName().equalsIgnoreCase(name)) {
				return u;
			}
		}
		return null;
	}
	
	/**
	 * Gets the unit with a particular identifier (unit id).
	 *
	 * @param id identifier
	 * @return
	 */
	public Unit getUnitByID(Integer id) {
		if (id.intValue() == Unit.MARS_SURFACE_UNIT_ID)
			return marsSurface;
		else if (id.intValue() == Unit.OUTER_SPACE_UNIT_ID)
			return outerSpace;
		else if (id.intValue() == Unit.UNKNOWN_UNIT_ID) {
			return null;
		}

		UnitType type = getTypeFromIdentifier(id);
		Unit found = getUnitMap(type).get(id);
		if (found == null) {
			logger.warning("Unit not found. id: " + id + ". Type of unit: " + type
			               + " (Base ID: " + (id >>> TYPE_BITS) + ").");
		}
		return found;
	}

	public Settlement getSettlementByID(Integer id) {
		return lookupSettlement.get(id);
	}
	
	/**
	 * Finds a nearby settlement based on its coordinates.
	 *
	 * @param c {@link Coordinates}
	 * @return
	 */
	public Settlement findSettlement(Coordinates c) {

		if (!settlementCoordinateMap.containsKey(c)) {
			Collection<Settlement> ss = getSettlements();
			
			Settlement settlement = null;
			for (Settlement s : ss) {
				Coordinates coord = s.getCoordinates();
				
				// Put the coord and id into the map
				if (!settlementCoordinateMap.containsKey(coord))
					settlementCoordinateMap.put(coord, s.getIdentifier());
				if (coord.equals(c)) {
					settlement = s;
				}
			}

			return settlement;
		}
		
		Integer i = settlementCoordinateMap.get(c);
		if (i != null)
			return lookupSettlement.get(i);
		
		return null;
	}
	
	/**
	 * Is this a settlement's coordinates ?
	 *
	 * @param c {@link Coordinates}
	 * @return
	 */
	public boolean isSettlement(Coordinates c) {
		if (settlementCoordinateMap == null) {
			settlementCoordinateMap = new HashMap<>();

			Collection<Settlement> ss = getSettlements();
			
			for (Settlement s : ss) {
				settlementCoordinateMap.put(s.getCoordinates(), s.getIdentifier());
			}
		}
		
		Integer i = settlementCoordinateMap.get(c);
		if (i != null)
			return true;
		
		return false;
	}
	
	/**
	 * Gets commander settlement.
	 * 
	 * @return
	 */
	public Settlement getCommanderSettlement() {
		return getPersonByID(commanderID).getAssociatedSettlement();
	}

	/**
	 * Gets the settlement list including the commander's associated settlement
	 * and the settlement that he's at or in the vicinity.
	 *
	 * @return {@link List<Settlement>}
	 */
	public List<Settlement> getCommanderSettlements() {
		List<Settlement> settlements = new ArrayList<>();

		Person cc = getPersonByID(commanderID);
		// Add the commander's associated settlement
		Settlement as = cc.getAssociatedSettlement();
		settlements.add(as);

		// Find the settlement the commander is at
		Settlement s = cc.getSettlement();
		// If the commander is in the vicinity of a settlement
		if (s == null)
			s = findSettlement(cc.getCoordinates());
		if (s != null && as != s)
			settlements.add(s);

		return settlements;
	}

	public Person getPersonByID(Integer id) {
		return lookupPerson.get(id);
	}

	public Robot getRobotByID(Integer id) {
		return lookupRobot.get(id);
	}

	public Equipment getEquipmentByID(Integer id) {
		return lookupEquipment.get(id);
	}

	public Building getBuildingByID(Integer id) {
		return lookupBuilding.get(id);
	}

	public Vehicle getVehicleByID(Integer id) {
		return lookupVehicle.get(id);
	}

	/**
	 * Adds a unit to the unit manager if it doesn't already have it.
	 *
	 * @param unit new unit to add.
	 */
	public synchronized void addUnit(Unit unit) {

		if (unit != null) {
			switch(unit.getUnitType()) {
			case SETTLEMENT:
				lookupSettlement.put(unit.getIdentifier(),
			   			(Settlement) unit);
				addDisplayUnit(unit);
				break;
			case PERSON:
				lookupPerson.put(unit.getIdentifier(),
			   			(Person) unit);
				break;
			case ROBOT:
				lookupRobot.put(unit.getIdentifier(),
			   			(Robot) unit);
				break;
			case VEHICLE:
				lookupVehicle.put(unit.getIdentifier(),
			   			(Vehicle) unit);
				addDisplayUnit(unit);
				break;
			case CONTAINER:
			case EVA_SUIT:
				lookupEquipment.put(unit.getIdentifier(),
			   			(Equipment) unit);
				break;
			case BUILDING:
				lookupBuilding.put(unit.getIdentifier(),
						   			(Building) unit);
				break;
			case CONSTRUCTION:
				lookupSite.put(unit.getIdentifier(),
							   (ConstructionSite) unit);
				break;
			case MARS:
				// Bit of a hack at the moment.
				// Need to revisit once extra Planets added.
				marsSurface = (MarsSurface) unit;
				break;
				
			case OUTER_SPACE:
				outerSpace = (OuterSpace) unit;
				break;
				
			case MOON:
				moon = (Moon) unit;
				break;
				
			default:
				throw new IllegalArgumentException("Cannot store unit type:" + unit.getUnitType());
			}

			// Fire unit manager event.
			fireUnitManagerUpdate(UnitManagerEventType.ADD_UNIT, unit);
		}
	}

	/**
	 * Removes a unit from the unit manager.
	 *
	 * @param unit the unit to remove.
	 */
	public synchronized void removeUnit(Unit unit) {
		UnitType type = getTypeFromIdentifier(unit.getIdentifier());
		Map<Integer,? extends Unit> map = getUnitMap(type);

		map.remove(unit.getIdentifier());

		// Fire unit manager event.
		fireUnitManagerUpdate(UnitManagerEventType.REMOVE_UNIT, unit);
	}

	/**
	 * Increments the count of the number of new unit requested.
	 * This count is independent of the actual Units held in the manager.
	 * 
	 * @param name
	 * @return
	 */
	public int incrementTypeCount(String name) {
		synchronized (unitCounts) {
			return unitCounts.merge(name, 1, (a, b) -> a + b);
		}
	}

	public void setCommanderId(int commanderID) {
		this.commanderID = commanderID;
	}

	public int getCommanderID() {
		return commanderID;
	}


	/**
	 * Notifies all the units that time has passed. Times they are a changing.
	 *
	 * @param pulse the amount time passing (in millisols)
	 * @throws Exception if error during time passing.
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		if (pulse.isNewSol() || justLoaded) {
			if (factory != null) {
				// Compute reliability daily for each part
				factory.computePartReliability(pulse.getMarsTime().getMissionSol());
			}
			justLoaded = false;
		}

		if (pulse.getElapsed() > 0) {
			runExecutor(pulse);
		}
		else {
			logger.warning("Zero elapsed pulse #" + pulse.getId());
		}

		return true;
	}

	/**
	 * Sets up executive service.
	 */
	private void setupExecutor() {
		if (executor == null) {
			int size = (int)(getSettlementNum()/2D);
			int num = Math.min(size, SimulationRuntime.NUM_CORES - simulationConfig.getUnusedCores());
			if (num <= 0) num = 1;
			logger.config("Setting up " + num + " thread(s) for running the settlement update.");
			executor = Executors.newFixedThreadPool(num,
					new ThreadFactoryBuilder().setNameFormat("unitmanager-thread-%d").build());
		}
	}

	/**
	 * Sets up settlement tasks for executive service.
	 */
	private void setupTasks() {
		if (settlementTasks == null || settlementTasks.isEmpty()) {
			settlementTasks = new HashSet<>();
			lookupSettlement.values().forEach(s -> activateSettlement(s));
		}
	}

	/**
	 * Adds a Settlement to the managed list and activate it for time pulses.
	 * 
	 * @param s
	 */
	public void activateSettlement(Settlement s) {
		if (!lookupSettlement.containsKey(s.getIdentifier())) {
			throw new IllegalStateException("Do not know new settlement "
						+ s.getName());
		}

		logger.config("Setting up a settlement task thread for " + s + ".");
		SettlementTask st = new SettlementTask(s);
		settlementTasks.add(st);
	}

	/**
	 * This method validates whether the current active Settlement in this thread matches
	 * the owner of an entity. This is a Thread specific method.
	 * 
	 * @param operation
	 * @param owner
	 */
	public static void validateActiveSettlement(String operation, Unit owner) {
		Settlement currentSettlement = activeSettlement.get();
		Settlement owningSettlement;
		if (owner instanceof Settlement s) {
			owningSettlement = s;
		}
		else {
			owningSettlement = owner.getAssociatedSettlement();
		}
		if ((currentSettlement != null) && !currentSettlement.equals(owningSettlement)) {
			// Log an error but don't throw an exception; use a temp exception to get a stack trace
			logger.severe(operation + " is executed by "
					+ currentSettlement.getName() + " but owner is " + owningSettlement.getName(),
					new IllegalStateException(operation));
		}
	}

	/**
	 * Fires the clock pulse to each clock listener.
	 *
	 * @param pulse
	 */
	private void runExecutor(ClockPulse pulse) {
		setupExecutor();
		setupTasks();
		// May use parallelStream() after it's proven to be safe
		settlementTasks.stream().forEach(s -> s.setCurrentPulse(pulse));

		// Execute all listener concurrently and wait for all to complete before advancing
		// Ensure that Settlements stay synch'ed and some don't get ahead of others as tasks queue
		try {
			List<Future<String>> results = executor.invokeAll(settlementTasks);
			for (Future<String> future : results) {
				future.get();
			}
		}
		catch (ExecutionException ee) {
			// Problem running the pulse
			logger.severe("Problem running the pulse : ", ee);
		}
		catch (InterruptedException ie) {
			// Program probably exiting
			if (executor.isShutdown()) {
				Thread.currentThread().interrupt();
			}
		}
	}

	/**
	 * Ends the current executor.
	 */
	public void endSimulation() {
		if (executor != null)
			executor.shutdownNow();
	}

	/**
	 * Gets number of settlements.
	 *
	 * @return the number of settlements
	 */
	public int getSettlementNum() {
		return lookupSettlement.size();
	}

	/**
	 * Gets a collection of sponsors.
	 *
	 * @return Collection of sponsors
	 */
	public Collection<Authority> getSponsorSet() {
		if (sponsorSet.isEmpty()) {
			Set<Authority> sponsors = new HashSet<>();
			
			for (Settlement s: getSettlements()) {
				Authority ra = s.getReportingAuthority();
				if (!sponsors.contains(ra))
					sponsors.add(ra);
			}
			
			sponsorSet = sponsors;
		}
		return sponsorSet;
	}
	
	
	/**
	 * Gets a collection of settlements.
	 *
	 * @return Collection of settlements
	 */
	public Collection<Settlement> getSettlements() {
		return Collections.unmodifiableCollection(lookupSettlement.values());
	}

	/**
	 * Gets a collection of vehicles.
	 *
	 * @return Collection of vehicles
	 */
	public Collection<Vehicle> getVehicles() {
		return Collections.unmodifiableCollection(lookupVehicle.values());
	}

	/**
	 * Gets a collection of people.
	 *
	 * @return Collection of people
	 */
	public Collection<Person> getPeople() {
		return Collections.unmodifiableCollection(lookupPerson.values());
	}

	/**
	 * Get a collection of robots.
	 *
	 * @return Collection of Robots
	 */
	public Collection<Robot> getRobots() {
		return Collections.unmodifiableCollection(lookupRobot.values());
	}

	/**
	 * Get a collection of EVA suits.
	 *
	 * @return Collection of EVA suits
	 */
	public Collection<Equipment> getEVASuits() {
		return lookupEquipment.values().stream()
				.filter(e -> e.getUnitType() == UnitType.EVA_SUIT)
				.collect(Collectors.toSet());
	}
	
	/**
	 * Adds the unit for display.
	 *
	 * @param unit
	 */
	private void addDisplayUnit(Unit unit) {
		if (displayUnits == null)
			displayUnits = new UnitSet<>();

		displayUnits.add(unit);
	}

	/**
	 * Obtains the settlement and vehicle units for map display.
	 * 
	 * @return
	 */
	public Set<Unit> getDisplayUnits() {
		return displayUnits;
	}

	/**
	 * Adds a unit manager listener.
	 *
	 * @param source UnitType monitored
	 * @param newListener the listener to add.
	 */
	public final void addUnitManagerListener(UnitType source, UnitManagerListener newListener) {
		if (listeners == null) {
			listeners = new EnumMap<>(UnitType.class);
		}
		synchronized(listeners) {
			listeners.computeIfAbsent(source, k -> new HashSet<>()).add(newListener);

			// Over adding listeners?
			//logger.info("Added Listener " + source + " #" + listeners.get(source).size() + " : " + newListener.toString());
		}
	}

	/**
	 * Removes a unit manager listener.
	 *
	 * @param oldListener the listener to remove.
	 */
	public final void removeUnitManagerListener(UnitType source, UnitManagerListener oldListener) {
		if (listeners == null) {
			// Will never happen
			return;
		}

		synchronized(listeners) {
			Set<UnitManagerListener> l = listeners.get(source);
			if (l != null) {
				l.remove(oldListener);
			}
		}
	}

	/**
	 * Fires a unit update event.
	 *
	 * @param eventType the event type.
	 * @param unit      the unit causing the event.
	 */
	private final void fireUnitManagerUpdate(UnitManagerEventType eventType, Unit unit) {
		if (listeners == null) {
			return;
		}
		synchronized (listeners) {
			Set<UnitManagerListener> l = listeners.get(unit.getUnitType());
			if (l != null) {
				UnitManagerEvent e = new UnitManagerEvent(this, eventType, unit);

				for (UnitManagerListener listener : l) {
					listener.unitManagerUpdate(e);
				}
			}
		}
	}

	/**
	 * Returns the Mars surface instance.
	 *
	 * @return {@Link MarsSurface}
	 */
	public MarsSurface getMarsSurface() {
		return marsSurface;
	}

	/**
	 * Returns the outer space instance.
	 *
	 * @return {@Link OuterSpace}
	 */	
	public OuterSpace getOuterSpace() {
		return outerSpace;
	}
	
	/**
	 * Returns the Moon instance.
	 *
	 * @return {@Link Moon}
	 */	
	public Moon getMoon() {
		return moon;
	}
	
	/**
	 * Extracts the UnitType from an identifier.
	 * 
	 * @param id
	 * @return
	 */
	public static UnitType getTypeFromIdentifier(int id) {
		// Extract the bottom 4 bit
		int typeId = (id & TYPE_MASK);

		return UnitType.values()[typeId];
	}

	/**
	 * Generates a new unique UnitId for a certain type. This will be used later
	 * for lookups.
	 * The lowest 4 bits contain the ordinal of the UnitType. Top remaining bits
	 * are a unique increasing number.
	 * This guarantees uniqueness PLUS a quick means to identify the UnitType 
	 * from only the identifier.
	 * 
	 * @param unitType
	 * @return
	 */
	public synchronized int generateNewId(UnitType unitType) {
		int baseId = uniqueId++;
		if (baseId >= MAX_BASE_ID) {
			throw new IllegalStateException("Too many Unit created " + MAX_BASE_ID);
		}
		int typeId = unitType.ordinal();

		return (baseId << TYPE_BITS) + typeId;
	}

	public void setOriginalBuild(String build) {
		originalBuild = build;
	}

	public String getOriginalBuild() {
		return originalBuild;
	}

	/**
	 * Reloads instances after loading from a saved sim.
	 *
	 * @param clock
	 */
	public void reinit() {

		for (Person p: lookupPerson.values()) {
			p.reinit();
		}
		for (Robot r: lookupRobot.values()) {
			r.reinit();
		}
		for (Building b: lookupBuilding.values()) {
			b.reinit();
		}
		for (Settlement s: lookupSettlement.values()) {
			s.reinit();
		}

		// Sets up the executor
		setupExecutor();
		// Sets up the concurrent tasks
		setupTasks();
	}
	
//	/**
//	 * Reinitializes instances after deserialization.
//	 * 
//	 * @param in
//	 * @throws IOException
//	 * @throws ClassNotFoundException
//	 */
//	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
//		in.defaultReadObject();
//	}
	
	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		activeSettlement.remove();

		Iterator<Settlement> i1 = lookupSettlement.values().iterator();
		while (i1.hasNext()) {
			i1.next().destroy();
		}
		Iterator<ConstructionSite> i0 = lookupSite.values().iterator();
		while (i0.hasNext()) {
			i0.next().destroy();
		}
		Iterator<Vehicle> i2 = lookupVehicle.values().iterator();
		while (i2.hasNext()) {
			i2.next().destroy();
		}
		Iterator<Building> i3 = lookupBuilding.values().iterator();
		while (i3.hasNext()) {
			i3.next().destroy();
		}
		Iterator<Person> i4 = lookupPerson.values().iterator();
		while (i4.hasNext()) {
			i4.next().destroy();
		}
		Iterator<Robot> i5 = lookupRobot.values().iterator();
		while (i5.hasNext()) {
			i5.next().destroy();
		}
		Iterator<Equipment> i6 = lookupEquipment.values().iterator();
		while (i6.hasNext()) {
			i6.next().destroy();
		}

		lookupSite.clear();
		lookupSettlement.clear();
		lookupVehicle.clear();
		lookupBuilding.clear();
		lookupPerson.clear();
		lookupRobot.clear();
		lookupEquipment.clear();

		lookupSite = null;
		lookupSettlement = null;
		lookupVehicle = null;
		lookupBuilding = null;
		lookupPerson = null;
		lookupRobot = null;
		lookupEquipment = null;

		marsSurface = null;

//		listeners.clear();
		listeners = null;
	}

	/**
	 * Prepares the Settlement task for setting up its own thread.
	 */
	class SettlementTask implements Callable<String> {
		private Settlement settlement;
		private ClockPulse currentPulse;

		protected Settlement getSettlement() {
			return settlement;
		}

		public void setCurrentPulse(ClockPulse pulse) {
			this.currentPulse = pulse;
		}

		private SettlementTask(Settlement settlement) {
			this.settlement = settlement;
		}

		@Override
		public String call() throws Exception {
			try {
				activeSettlement.set(settlement);
				settlement.timePassing(currentPulse);
				activeSettlement.remove();
			}
			catch (RuntimeException rte) {
				String msg = "Problem with pulse on " + settlement.getName()
        					  + ": " + rte.getMessage();
	            logger.severe(msg, rte);
	            return msg;
			}
			return settlement.getName() + " completed pulse #" + currentPulse.getId();
		}
	}
}
