/**
 * Mars Simulation Project
 * BuildingManager.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */

package org.mars_sim.msp.core.structure.building;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.environment.Meteorite;
import org.mars_sim.msp.core.environment.MeteoriteModule;
import org.mars_sim.msp.core.equipment.Equipment;
import org.mars_sim.msp.core.events.HistoricalEventManager;
import org.mars_sim.msp.core.interplanetary.transport.resupply.Resupply;
import org.mars_sim.msp.core.location.LocationStateType;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.social.RelationshipManager;
import org.mars_sim.msp.core.person.ai.task.HaveConversation;
import org.mars_sim.msp.core.person.ai.task.utils.Worker;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.RobotType;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.structure.BuildingTemplate;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.SettlementConfig;
import org.mars_sim.msp.core.structure.building.connection.BuildingConnectorManager;
import org.mars_sim.msp.core.structure.building.connection.InsideBuildingPath;
import org.mars_sim.msp.core.structure.building.function.Administration;
import org.mars_sim.msp.core.structure.building.function.AstronomicalObservation;
import org.mars_sim.msp.core.structure.building.function.BuildingConnection;
import org.mars_sim.msp.core.structure.building.function.Communication;
import org.mars_sim.msp.core.structure.building.function.Computation;
import org.mars_sim.msp.core.structure.building.function.EVA;
import org.mars_sim.msp.core.structure.building.function.EarthReturn;
import org.mars_sim.msp.core.structure.building.function.Exercise;
import org.mars_sim.msp.core.structure.building.function.FoodProduction;
import org.mars_sim.msp.core.structure.building.function.Function;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.GroundVehicleMaintenance;
import org.mars_sim.msp.core.structure.building.function.LifeSupport;
import org.mars_sim.msp.core.structure.building.function.LivingAccommodations;
import org.mars_sim.msp.core.structure.building.function.Management;
import org.mars_sim.msp.core.structure.building.function.Manufacture;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;
import org.mars_sim.msp.core.structure.building.function.PowerGeneration;
import org.mars_sim.msp.core.structure.building.function.PowerStorage;
import org.mars_sim.msp.core.structure.building.function.Recreation;
import org.mars_sim.msp.core.structure.building.function.Research;
import org.mars_sim.msp.core.structure.building.function.ResourceProcessing;
import org.mars_sim.msp.core.structure.building.function.RoboticStation;
import org.mars_sim.msp.core.structure.building.function.Storage;
import org.mars_sim.msp.core.structure.building.function.ThermalGeneration;
import org.mars_sim.msp.core.structure.building.function.VehicleMaintenance;
import org.mars_sim.msp.core.structure.building.function.cooking.Cooking;
import org.mars_sim.msp.core.structure.building.function.cooking.Dining;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;
import org.mars_sim.msp.core.structure.building.function.farming.Farming;
import org.mars_sim.msp.core.structure.building.function.farming.Fishery;
import org.mars_sim.msp.core.structure.construction.ConstructionManager;
import org.mars_sim.msp.core.structure.construction.ConstructionSite;
import org.mars_sim.msp.core.structure.construction.ConstructionStageInfo;
import org.mars_sim.msp.core.structure.construction.ConstructionUtil;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.MasterClock;
import org.mars_sim.msp.core.tool.AlphanumComparator;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.StatusType;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.core.vehicle.VehicleType;

import com.google.inject.Guice;

/**
 * The BuildingManager manages the settlement's buildings.
 */
public class BuildingManager implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(BuildingManager.class.getName());

	private transient MarsClock lastVPUpdateTime;

	private transient List<Building> farmsNeedingWorkCache = new CopyOnWriteArrayList<>();
	private transient List<Building> buildings;
	private transient List<Building> garages; 
	
	private transient Map<String, Double> VPNewCache = new ConcurrentHashMap<String, Double>();
	private transient Map<String, Double> VPOldCache = new ConcurrentHashMap<String, Double>();
	private transient Map<FunctionType, List<Building>> buildingFunctionsMap  = new HashMap<FunctionType, List<Building>>();
	private transient Map<String, Integer> buildingTypeIDMap  = new HashMap<>();

	// Data members
	private double farmTimeCache = -5D;
	private transient Settlement settlement;
	private Integer settlementID;
	private int nextInhabitableID = 0;
	
	private double debrisMass;
	private double probabilityOfImpactPerSQMPerSol;
	private double wallPenetrationThicknessAL;

	private List<Integer> buildingInts = new ArrayList<>();
	private List<Integer> garageInts = new ArrayList<>();
	
	private static Simulation sim = Simulation.instance();
	private static SimulationConfig simulationConfig = SimulationConfig.instance();

	private static Meteorite meteorite;

	private static HistoricalEventManager eventManager;
	private static MarsClock marsClock;
	private static MasterClock masterClock;
	private static RelationshipManager relationshipManager;
	private static UnitManager unitManager = sim.getUnitManager();
	private static SettlementConfig settlementConfig = simulationConfig.getSettlementConfiguration();
	

	/**
	 * Constructor 1 : construct buildings from settlement config template. Called
	 * by Settlement
	 * 
	 * @param settlement the manager's settlement.
	 * @throws Exception if buildings cannot be constructed.
	 */
	public BuildingManager(Settlement settlement) {
		this(settlement, settlementConfig
				.getSettlementTemplate(settlement.getTemplate()).getBuildingTemplates());
	}

	/**
	 * Constructor 2 : construct buildings from name list. Called by constructor 1
	 * and by constructor 1
	 * 
	 * @param settlement        the manager's settlement
	 * @param buildingTemplates the settlement's building templates.
	 * @throws Exception if buildings cannot be constructed.
	 */
	public BuildingManager(Settlement settlement, List<BuildingTemplate> buildingTemplates) {
		this.settlement = settlement;
		this.settlementID = (Integer) settlement.getIdentifier();

		masterClock = sim.getMasterClock();
		marsClock = masterClock.getMarsClock();

		eventManager = sim.getEventManager();
		relationshipManager = sim.getRelationshipManager();
		unitManager = sim.getUnitManager();
		
		// Construct all buildings in the settlement.
		buildings = new CopyOnWriteArrayList<>();
		if (buildingTemplates != null) {
			Iterator<BuildingTemplate> i = buildingTemplates.iterator();
			while (i.hasNext()) {
				addBuilding(i.next(), false);
			}
		}

		buildings.stream().sorted(new AlphanumComparator()).collect(Collectors.toList());

		garages = getBuildings(FunctionType.GROUND_VEHICLE_MAINTENANCE);
		for (Building b: garages) {
			garageInts.add(b.getIdentifier());
		}
		
		if (buildingTypeIDMap.isEmpty())
			createBuildingTypeIDMap();

		if (buildingFunctionsMap.isEmpty())
			setupBuildingFunctionsMap();

		// Make use of Guice for Meteorite
		meteorite = Guice.createInjector(new MeteoriteModule()).getInstance(Meteorite.class);
	}

	/**
	 * Constructor 3 : Called by MockSettlement for maven test
	 * 
	 * @param settlement        the manager's settlement
	 * @param buildingTemplates the settlement's building templates.
	 * @throws Exception if buildings cannot be constructed.
	 */
	public BuildingManager(Settlement settlement, String name) {
		this.settlement = settlement;	
		this.settlementID = (Integer) settlement.getIdentifier();

		relationshipManager = sim.getRelationshipManager();
		unitManager = sim.getUnitManager();
		
		// Construct all buildings in the settlement.
		buildings = new CopyOnWriteArrayList<Building>();

		garages = getBuildings(FunctionType.GROUND_VEHICLE_MAINTENANCE);
		for (Building b: garages) {
			garageInts.add(b.getIdentifier());
		}
		
//		setupBuildingFunctionsMap();
	}

	
	/**
	 * Sets up the map for the building functions
	 */
	public void setupBuildingFunctionsMap() {
		for (FunctionType f : FunctionType.values()) {
			List<Building> list = new CopyOnWriteArrayList<Building>();
			for (Building b : buildings) {
				if (b.hasFunction(f)) {
					list.add(b);
					// Add this new building to the garage list if it has a garage
					if (f == FunctionType.GROUND_VEHICLE_MAINTENANCE)
						if (!garages.contains(b))
							garages.add(b);
				}
			}
			buildingFunctionsMap.put(f, list);
		}
	}

	/**
	 * Removes a building from the settlement.
	 * 
	 * @param oldBuilding the building to remove.
	 */
	public void removeBuilding(Building oldBuilding) {

		if (buildings.contains(oldBuilding)) {
			// Remove building connections (hatches) to old building.
			unitManager.getSettlementByID(settlementID).getBuildingConnectorManager().removeAllConnectionsToBuilding(oldBuilding);
			// Remove the building's functions from the settlement.
			oldBuilding.removeFunctionsFromSettlement();

			buildings.remove(oldBuilding);

			// Call to remove all references of this building in all functions
			removeAllFunctionsfromBFMap(oldBuilding);

			unitManager.getSettlementByID(settlementID).fireUnitUpdate(UnitEventType.REMOVE_BUILDING_EVENT, oldBuilding);
		}
	}

	/**
	 * Removes all references of this building in all functions in
	 * buildingFunctionsMap
	 * 
	 * @param oldBuilding
	 */
	public void removeAllFunctionsfromBFMap(Building oldBuilding) {
		if (buildingFunctionsMap != null) {
			// use this only after buildingFunctionsMap has been created
			for (FunctionType ft : FunctionType.values()) {
				// if this building has this function
				if (oldBuilding.hasFunction(ft)) {
					List<Building> list = buildingFunctionsMap.get(ft);
					if (list.contains(oldBuilding)) {
						list.remove(oldBuilding);
						buildingFunctionsMap.put(ft, list);
					}			
					// Remove this old building from the garage list if it has a garage
					if (ft == FunctionType.GROUND_VEHICLE_MAINTENANCE) {
						if (garages.contains(oldBuilding)) {
							garages.remove(oldBuilding);
							garageInts.remove(oldBuilding.getIdentifier());
						}
					}
				}
			}
		}
	}

	/**
	 * Removes the reference of this building for a functions in
	 * buildingFunctionsMap
	 * 
	 * @param a building
	 * @param a function
	 */
	public void removeOneFunctionfromBFMap(Building b, Function f) {
		if (buildingFunctionsMap != null) {
			FunctionType ft = f.getFunctionType();
			List<Building> list = buildingFunctionsMap.get(ft);
			if (list.contains(b)) {
				list.remove(b);
				buildingFunctionsMap.put(ft, list);	
			}
			// Remove this old building from the garage list if it has a garage
			if (ft == FunctionType.GROUND_VEHICLE_MAINTENANCE) {
				if (garages.contains(b)) {
					garages.remove(b);
					garageInts.remove(b.getIdentifier());
				}
			}
		}
	}

	/**
	 * Add references of this building in all functions in buildingFunctionsMap
	 * 
	 * @param oldBuilding
	 */
	public void addNewBuildingtoBFMap(Building newBuilding) {
		if (buildingFunctionsMap.isEmpty())
			setupBuildingFunctionsMap();
		if (buildingFunctionsMap != null) {
			// use this only after buildingFunctionsMap has been created
			for (FunctionType ft : FunctionType.values()) {
				// if this building has this function
				if (newBuilding.hasFunction(ft)) {
					List<Building> list = null;
					if (buildingFunctionsMap.containsKey(ft)) {
						list = buildingFunctionsMap.get(ft);
						// if this building is not on the list yet
						if (!list.contains(newBuilding))
							list.add(newBuilding);
					} else {
						// Starts a new list of building
						list = new CopyOnWriteArrayList<>();
						list.add(newBuilding);
					}
					buildingFunctionsMap.put(ft, list);
					
					if (ft == FunctionType.GROUND_VEHICLE_MAINTENANCE) {
						if (garages != null && !garages.contains(newBuilding)) {
							garages.add(newBuilding);
							garageInts.add(newBuilding.getIdentifier());
						}
					}
				}
			}
		}
	}

	/**
	 * Adds a new building to the settlement.
	 * 
	 * @param newBuilding               the building to add.
	 * @param createBuildingConnections true if automatically create building
	 *                                  connections.
	 */
	public void addBuilding(Building newBuilding, boolean createBuildingConnections) {
		if (!buildings.contains(newBuilding)) {	
			unitManager.addUnit(newBuilding);
			
			buildings.add(newBuilding);
//			System.out.println(newBuilding.getIdentifier());
			buildingInts.add(newBuilding.getIdentifier());
			
			int id = newBuilding.getInhabitableID();
			
			// Add tracking air composition
			if (settlement.getCompositionOfAir() != null && id != -1)
				settlement.getCompositionOfAir().addAirNew(newBuilding);
			// Insert this new building into buildingFunctionsMap
			addNewBuildingtoBFMap(newBuilding);
			
			settlement.fireUnitUpdate(UnitEventType.ADD_BUILDING_EVENT, newBuilding);
			// Create new building connections if needed.
			if (createBuildingConnections) {
				settlement.getBuildingConnectorManager().createBuildingConnections(newBuilding);
			}
		}
	}

	/**
	 * Adds a new mock building to the settlement.
	 * 
	 * @param newBuilding               the building to add.
	 */
	public void addMockBuilding(Building newBuilding) {
		if (!buildings.contains(newBuilding)) {
			buildings.add(newBuilding);
//			buildingInts.add(newBuilding.getIdentifier());
//			addAllFunctionstoBFMap(newBuilding);
		}
	}
	
	/**
	 * Removes all mock buildings and building functions in the settlement.
	 */
	public void removeAllMockBuildings() {
		buildings.clear();
		buildingFunctionsMap.clear();
	}
	
	/**
	 * Adds a building with a template to the settlement.
	 * 
	 * @param template                  the building template.
	 * @param createBuildingConnections true if automatically create building
	 *                                  connections.
	 */
	public void addBuilding(BuildingTemplate template, boolean createBuildingConnections) {
		addBuilding(new Building(template, this), createBuildingConnections);
	}

	/**
	 * Adds a building with a template to the settlement.
	 * 
	 * @param template                  the building template.
	 * @param createBuildingConnections true if automatically create building
	 *                                  connections.
	 * @return newBuilding
	 */
	public Building prepareToAddBuilding(BuildingTemplate template, Resupply resupply,
			boolean createBuildingConnections) {
		// Add prepareToAddBuilding-- called by confirmBuildingLocation() in
		// Resupply.java
		Building newBuilding = new Building(template, this);
		addBuilding(newBuilding, createBuildingConnections);
		return newBuilding;
	}

//	public Resupply getResupply() {
//		return resupply;
//	}

//	public void addResupply(Resupply resupply) {
//		this.resupply = resupply;
//	}

	/**
	 * Gets a copy of settlement's collection of buildings.
	 * 
	 * @return collection of buildings
	 */
	public List<Building> getACopyOfBuildings() {
		return new CopyOnWriteArrayList<Building>(buildings);
	}

	/**
	 * Gets a collection of buildings.
	 * 
	 * @return collection of buildings
	 */
	public List<Building> getBuildings() {
		return buildings;
	}

	/**
	 * Gets a collection of buildings.
	 * 
	 * @return collection of buildings
	 */
	public List<Building> getSortedBuildings() {
		return buildings.stream().sorted(new AlphanumComparator()).collect(Collectors.toList());
	}

//	/**
//	 * Gets the settlement's collection of buildings (in their nicknames)
//	 * 
//	 * @return collection of buildings (in their nicknames)
//	 */
//	public List<Building> getBuildingsNickNames() {
//		return new CopyOnWriteArrayList<Building>(buildingsNickNames);
//	}

	/**
	 * Gets a list of settlement's buildings with Robotic Station function
	 * 
	 * @return list of buildings
	 */
	public List<Building> getBuildingsWithRoboticStation() {
		return getBuildings(FunctionType.ROBOTIC_STATION);
		// Using JavaFX/8 Stream
//		List<Building> buildings = getACopyOfBuildings();
//    	List<Building> buildingsWithRoboticStation =
//            	buildings.stream()
//        		//buildings.parallelStream() // parallelStream makes it 3x slower than sequential stream
//    	        .filter(s -> buildingConfig.hasRoboticStation(s.getBuildingType()))
//    	        .collect(Collectors.toList());
//
//    	return buildingsWithRoboticStation;
	}

	/**
	 * Gets a list of settlement's buildings with Life Supportfunction
	 * 
	 * @return list of buildings
	 */
	public List<Building> getBuildingsWithLifeSupport() {
		return getBuildings(FunctionType.LIFE_SUPPORT);
	}

	/**
	 * Gets a list of settlement's buildings with power generation function
	 * 
	 * @return list of buildings
	 */
	public List<Building> getBuildingsWithPowerGeneration() {
		return getBuildings(FunctionType.POWER_GENERATION);
	}

	/**
	 * Gets a list of settlement's buildings (not including  hallway or tunnel) having a particular function type
	 * 
	 * @param functionType
	 * @return list of buildings
	 */
	public List<Building> getBuildingsNoHallwayTunnel(FunctionType functionType) {
		// Filter off hallways and tunnels
		return getBuildings(functionType).stream().filter(b -> 
				!b.getBuildingType().equalsIgnoreCase("hallway")
				&& !b.getBuildingType().equalsIgnoreCase("tunnel")
				).collect(Collectors.toList());
	}
	
	/**
	 * Gets a list of settlement's buildings (not including hallway, tunnel or observatory) 
	 * having a particular function type
	 * 
	 * @param functionType
	 * @return list of buildings
	 */
	public List<Building> getBuildingsNoHallwayTunnelObs(FunctionType functionType) {
		// Filter off hallways and tunnels
		return getBuildings(functionType).stream().filter(b -> 
				!b.getBuildingType().equalsIgnoreCase(Building.HALLWAY)
				&& !b.getBuildingType().equalsIgnoreCase(Building.TUNNEL)
				&& !b.getBuildingType().equalsIgnoreCase(Building.ASTRONOMY_OBSERVATORY)
				).collect(Collectors.toList());
	}
	
	/**
	 * Gets a list of settlement's buildings with thermal function
	 * 
	 * @return list of buildings
	 */
	public List<Building> getBuildingsWithThermal() {
		return getBuildings(FunctionType.THERMAL_GENERATION);
	}

	/**
	 * Checks if the settlement contains a given building.
	 * 
	 * @param building the building.
	 * @return true if settlement contains building.
	 */
	public boolean containsBuilding(Building building) {
		return buildings.contains(building);
	}

	/**
	 * Gets the building with the given template ID.
	 * 
	 * @param id the template ID .
	 * @return building or null if none found.
	 */
	public Building getBuildingByTemplateID(int id) {
		// Use Java 8 stream
		// Note: stream won't pass junit test.
//		return buildings
//				.stream()
//				.filter(b-> b.getID() == id)
//				.findFirst().orElse(null);//.get();	// .findAny()

		// Note: the version below can pass junit test.
		Building result = null;

		Iterator<Building> i = buildings.iterator();
		while (i.hasNext()) {
			Building b = i.next();
			if (b.getTemplateID() == id) {
				result = b;
				// return b;
				// NOTE: do NOT use return b or else it fails maven test.
				// break;
				// NOTE: the word 'break' here will cause maven test to fail
			}
		}

		return result;
	}


	/**
	 * Gets the buildings in a settlement that has a given function.
	 * 
	 * @param building function {@link FunctionType} the function of the building.
	 * @return list of buildings.
	 */
	public List<Building> getBuildings(FunctionType bf) {
		if (buildingFunctionsMap == null) {
			buildingFunctionsMap = new ConcurrentHashMap<FunctionType, List<Building>>();
			setupBuildingFunctionsMap();
		}
		
		if (buildingFunctionsMap.containsKey(bf)) {
			return buildingFunctionsMap.get(bf);
		}

		else {
			List<Building> list = buildings.stream().filter(b -> b.hasFunction(bf)).collect(Collectors.toList());

			buildingFunctionsMap.put(bf, list);
			return list;
		}
	}

	/**
	 * Gets the buildings in a settlement that has a given function.
	 * 
	 * @param building function {@link FunctionType} the function of the building.
	 * @return list of buildings.
	 */
	public List<Building> getBuildingsWithoutFunctionType(FunctionType bf) {
		return buildings.stream().filter(b -> !b.hasFunction(bf)).collect(Collectors.toList());
	}
	
	public List<Building>getBuildingsWithScienceType(ScienceType type) {
		return buildings.stream().filter(b -> b.hasSpecialty(type)).collect(Collectors.toList());
	}
	
	/**
	 * Gets an available building that the person can use.
	 * 
	 * @param person the person
	 * @return available building or null if none.
	 */
	public static Building getAvailableBuilding(ScientificStudy study, Person person) {
		Building b = person.getBuildingLocation();
		
		// If this person is located in the observatory
		if ((b != null) && b.getBuildingType().equalsIgnoreCase(Building.ASTRONOMY_OBSERVATORY))
			return b;
		
		if (person.isInSettlement()) {
			List<Building> buildings = null;

			if (study != null) {
				ScienceType science = study.getScience();
				
				buildings = person.getSettlement().getBuildingManager().getBuildingsWithScienceType(science);				
			}
			
			if (buildings == null || buildings.size() == 0) {
				buildings = getBuildings(person, FunctionType.RESEARCH);
			}
			if (buildings == null || buildings.size() == 0) {
				buildings = getBuildings(person, FunctionType.ADMINISTRATION);
			}
			if (buildings == null || buildings.size() == 0) {
				buildings = getBuildings(person, FunctionType.DINING);
			}
			if (buildings == null || buildings.size() == 0) {
				buildings = getBuildings(person, FunctionType.LIVING_ACCOMMODATIONS);
			}
			
			if (buildings != null && buildings.size() > 0) {
				Map<Building, Double> possibleBuildings = BuildingManager.getBestRelationshipBuildings(person,
						buildings);
				b = RandomUtil.getWeightedRandomObject(possibleBuildings);
			}
		}

		return b;
	}

	public static List<Building> getBuildings(Person person, FunctionType functionType) {
		List<Building> buildings = person.getSettlement().getBuildingManager().getBuildings(functionType);
		buildings = BuildingManager.getNonMalfunctioningBuildings(buildings);
		return BuildingManager.getLeastCrowdedBuildings(buildings);
	}
	
	/**
	 * Gets the buildings in a settlement have have all of a given array of
	 * functions.
	 * 
	 * @param functions the array of required functions {@link BuildingFunctions}.
	 * @return list of buildings.
	 */
	public List<Building> getBuildings(FunctionType f1, FunctionType f2) {
		return buildings.stream().filter(b -> b.hasFunction(f1) && b.hasFunction(f2)).collect(Collectors.toList());
	}

	/**
	 * Gets the buildings in the settlement with a given building type.
	 * 
	 * @param buildingType the building type.
	 * @return list of buildings.
	 */
	public List<Building> getBuildingsOfSameType(String buildingType) {
		// Called by Resupply.java and BuildingConstructionMission.java
		// for putting new building next to the same building "type".
		return buildings.stream().filter(b -> b.getBuildingType().equalsIgnoreCase(buildingType))
				.collect(Collectors.toList());
	}

	/**
	 * Gets a random building in a settlement that has a given function.
	 * 
	 * @param bf {@link FunctionType} the function of the building.
	 * @return a building.
	 */
	public Building getABuilding(FunctionType bf) {
		if (buildingFunctionsMap == null) {
			buildingFunctionsMap = new ConcurrentHashMap<FunctionType, List<Building>>();
			setupBuildingFunctionsMap();
		}
		
		if (buildingFunctionsMap.containsKey(bf)) {
			List<Building> list = buildingFunctionsMap.get(bf);
			Building b = list.get(RandomUtil.getRandomInt(list.size()-1));
			return b;
		}

		else {
			return null;
//			List<Building> list = buildings.stream().filter(b -> b.hasFunction(bf)).collect(Collectors.toList());
//
//			buildingFunctionsMap.put(bf, list);
//			logger.config(bf + " was not found in buildingFunctionsMap yet. Just added.");
//
//			return list.get(RandomUtil.getRandomInt(list.size()-1));
		}
	}
	
	public Building getABuilding(FunctionType f1, FunctionType f2) {
		List<Building> list = buildings.stream().filter(b -> b.hasFunction(f1) && b.hasFunction(f2))
				.collect(Collectors.toList());
		return list.get(RandomUtil.getRandomInt(list.size()-1));
	}
	
	/**
	 * Gets the number of the same type of building.
	 * 
	 * @param buildingType
	 * @return a number
	 */
	public Long getNumBuildingsOfSameType(String buildingType) {
		return buildings.stream().filter(b -> b.getBuildingType().equalsIgnoreCase(buildingType))
				.collect(Collectors.counting());
	}
	
	/**
	 * Gets the number of buildings at the settlement.
	 * 
	 * @return number of buildings
	 */
	public int getNumBuildings() {
		return buildings.size();
	}


	/**
	 * Register beds for everyone in the settlement at the start of the sim
	 */
	public void registerBeds() {
		List<Point2D> beds = new CopyOnWriteArrayList<>();
		Map<Point2D, Building> map = new ConcurrentHashMap<>();
		// Discover a list of beds
		for (Building b : getBuildings(FunctionType.LIVING_ACCOMMODATIONS)) {
			Function l = b.getLivingAccommodations();
			List<Point2D> spots = l.getActivitySpotsList();
			for (Point2D s : spots) {		
				// Convert the activity spot (the bed location) to the settlement reference coordinate
				double x = s.getX() + b.getXLocation();
				double y = s.getY() + b.getYLocation();
				s.setLocation(x, y);
				beds.add(s);
				map.put(s, b);
			}
		}
		
		for (Person p : getSettlement().getAllAssociatedPeople()) {
			if (p.getBed() == null) {
				Iterator<Point2D> i = beds.iterator();
	            while (i.hasNext()) {
	            	Point2D s = i.next();
	            	Building b = map.get(s);
	            	b.getLivingAccommodations().assignABed(p, s);
//					beds.remove(s);
					i.remove();
					break;
				}
			}
		}
	}
	
	/**
	 * Time passing for all buildings.
	 *
	 * @param time amount of time passing (in millisols)
	 * @throws Exception if error.
	 */
	public boolean timePassing(ClockPulse pulse) {

		if (buildingTypeIDMap == null) {
			buildingTypeIDMap = new ConcurrentHashMap<>();
			createBuildingTypeIDMap();
		}
		
		if (buildingFunctionsMap == null) {
			buildingFunctionsMap = new ConcurrentHashMap<FunctionType, List<Building>>();
			setupBuildingFunctionsMap();
		}
		

		if (pulse.isNewSol()) {

//			if (solCache == 0) {
//				registerBeds();
//			}

			if (meteorite == null) {		
				meteorite = Guice.createInjector(new MeteoriteModule()).getInstance(Meteorite.class);
			}
			
			// Update the impact probability for each settlement based on the size and speed
			// of the new meteorite
			meteorite.startMeteoriteImpact(this);
		}
		
		for (Building b : buildings) {
			try {
				b.timePassing(pulse);
			}
			catch (RuntimeException rte) {
				logger.severe(b, "Problem applying pulse to Building", rte);
			}
		}
		return true;
	}

	/**
	 * Gets an empty bed in an airlock randomly
	 * 
	 * @return
	 */
	public Point2D getAirlockEmptyBed() {		
		List<Building> list = getBuildings(FunctionType.EVA, FunctionType.LIVING_ACCOMMODATIONS);
		Collections.shuffle(list);
		for (Building b : list) {
			Point2D bed = b.getLivingAccommodations().getEmptyBed();
			return bed;
		}
		return null;
	}
		
	/**
	 * Gets a random building with an airlock.
	 * 
	 * @return random building.
	 */
	public Building getRandomAirlockBuilding() {		
		return getABuilding(FunctionType.EVA);
		
//		if (buildingFunctionsMap == null) {
//			buildingFunctionsMap = new ConcurrentHashMap<FunctionType, List<Building>>();
//			setupBuildingFunctionsMap();
//		}
//		
//		if (buildingFunctionsMap.containsKey(FunctionType.EVA)) {
//			List<Building> list = buildingFunctionsMap.get(FunctionType.EVA);
//			int num = list.size();
//			if (num == 0)
//				return null;
//			else if (num == 1)
//				return list.get(0);
//				
//			int rand = RandomUtil.getRandomInt(num - 1);
//			return list.get(rand);
//		}
//
//		else {
//			List<Building> list = buildings.stream()
//					.filter(b -> b.hasFunction(FunctionType.EVA)).collect(Collectors.toList());
//
//			buildingFunctionsMap.put(FunctionType.EVA, list);
//			logger.config(FunctionType.EVA + " was not found in buildingFunctionsMap yet. Just added.");
//
//			return list.get(0);
//		}
	}
	
	/**
	 * Adds a person to a random medical building within a settlement.
	 *
	 * @param unit       the person/robot to add.
	 * @param settlementID the settlement to find a building.
	 * @throws BuildingException if person/robot cannot be added to any building.
	 */
	public static void addToMedicalBuilding(Person p, int settlementID) {
	
		Settlement s = unitManager.getSettlementByID(settlementID);
		Building building = s.getBuildingManager()
				.getABuilding(FunctionType.MEDICAL_CARE, FunctionType.LIFE_SUPPORT);
	
		if (building != null) {
			addPersonOrRobotToBuildingRandomLocation(p, building);
		} 
		
		else {
			logger.log(s, Level.WARNING, 2000,	"No medical facility available for "
							+ p.getName() + ". Go to a random building.");
			addToRandomBuilding(p, settlementID);
		}
	}

	/**
	 * Adds a person/robot to a random inhabitable building within a settlement.
	 *
	 * @param unit       the person/robot to add.
	 * @param settlementID the settlement to find a building.
	 * @throws BuildingException if person/robot cannot be added to any building.
	 */
	public static void addToRandomBuilding(Unit unit, int settlementID) {
		Person person = null;
		Robot robot = null;
		Settlement s = unitManager.getSettlementByID(settlementID);
		BuildingManager manager = s.getBuildingManager();
		if (unit instanceof Person) {
			person = (Person) unit;
	
			List<Building> list = getLeastCrowdedBuildings(manager.getBuildings(FunctionType.LIFE_SUPPORT)
					.stream().filter(b -> !b.getBuildingType().equalsIgnoreCase(Building.ASTRONOMY_OBSERVATORY))
					.collect(Collectors.toList()));
			
			if (list.size() == 0)
				return;
			
			Building building = list.get(RandomUtil.getRandomInt(list.size()-1));

			if (building != null) {
				// Add the person to a random building loc
				addPersonOrRobotToBuildingRandomLocation(person, building);
			} 
			
			else {
				logger.warning(person, "No inhabitable buildings available");
			}

		}

		else if (unit instanceof Robot) {
			robot = (Robot) unit;
			// find robot type
			RobotType robotType = robot.getRobotType();
			FunctionType function = null;

			if (robotType == RobotType.CHEFBOT) {
				function = FunctionType.COOKING;
			} else if (robotType == RobotType.CONSTRUCTIONBOT) {
				function = FunctionType.MANUFACTURE;
			} else if (robotType == RobotType.DELIVERYBOT) {
				function = FunctionType.GROUND_VEHICLE_MAINTENANCE;
			} else if (robotType == RobotType.GARDENBOT) {
				function = FunctionType.FARMING;
			} else if (robotType == RobotType.MAKERBOT) {
				function = FunctionType.MANUFACTURE;
			} else if (robotType == RobotType.MEDICBOT) {
				function = FunctionType.MEDICAL_CARE;
			} else if (robotType == RobotType.REPAIRBOT) {
				function = FunctionType.ROBOTIC_STATION;
				// TODO: create another building function to house repairbot ?
			}

			List<Building> functionBuildings = manager.getBuildings(function);

			Building destination = null;

			// Note: if the function is robotic-station, go through the list and remove
			// hallways
			// since we don't want robots to stay in a hallway
			List<Building> validBuildings = new CopyOnWriteArrayList<Building>();
			for (Building bldg : functionBuildings) {
				RoboticStation roboticStation = bldg.getRoboticStation();
				// remove hallway, tunnel, observatory
				if (roboticStation != null) {
					if (bldg.getBuildingType().equalsIgnoreCase("hallway")
							|| bldg.getBuildingType().equalsIgnoreCase("tunnel")
							|| bldg.getBuildingType().toLowerCase().contains("observatory")) {
						// functionBuildings.remove(bldg); // will cause ConcurrentModificationException
					} else if (function == FunctionType.FARMING) {
						if (bldg.getBuildingType().toLowerCase().contains("greenhouse")) {
							validBuildings.add(bldg);
						}
					} else if (function == FunctionType.MANUFACTURE) {
						if (bldg.getBuildingType().equalsIgnoreCase("workshop")
								|| bldg.getBuildingType().equalsIgnoreCase("manufacturing shed")
								|| bldg.getBuildingType().equalsIgnoreCase("machinery hab")) {
							validBuildings.add(bldg);
						}
					} else if (function == FunctionType.COOKING) {
						if (bldg.getBuildingType().equalsIgnoreCase("lounge")
							|| bldg.getBuildingType().equalsIgnoreCase("lander hab")
							|| bldg.getBuildingType().equalsIgnoreCase("outpost hub")) {
							validBuildings.add(bldg);
						}
					} else if (function == FunctionType.MEDICAL_CARE) {
						if (bldg.getBuildingType().equalsIgnoreCase("infirmary")
								|| bldg.getBuildingType().toLowerCase().contains("medical")) {
							validBuildings.add(bldg);
						}
					} else if (function == FunctionType.GROUND_VEHICLE_MAINTENANCE) {
						if (bldg.getBuildingType().toLowerCase().contains("garage")) {
							validBuildings.add(bldg);
						}
					} else { // if there is no specialized buildings,
								// then add general purpose buildings like "Lander Hab"
						validBuildings.add(bldg);
					}
				}
			}

			// Randomly pick one of the buildings
			if (validBuildings.size() >= 1) {
				int rand = RandomUtil.getRandomInt(validBuildings.size() - 1);
				destination = validBuildings.get(rand);
				addPersonOrRobotToBuildingRandomLocation(robot, destination);
			}

			else {
				List<Building> validBuildings1 = new CopyOnWriteArrayList<Building>();
				List<Building> stations = manager.getBuildings(FunctionType.ROBOTIC_STATION);
				for (Building bldg : stations) {
					// remove hallway, tunnel, observatory
					if (bldg.getBuildingType().equalsIgnoreCase("hallway")
							|| bldg.getBuildingType().equalsIgnoreCase("tunnel")
							|| bldg.getBuildingType().toLowerCase().contains("observatory")) {
						// stations.remove(bldg);// will cause ConcurrentModificationException
					} else {
						validBuildings1.add(bldg); // do nothing
					}
				}
				// Randomly pick one of the buildings
				if (validBuildings1.size() >= 1) {
					int rand = RandomUtil.getRandomInt(validBuildings1.size() - 1);
					destination = validBuildings1.get(rand);
				}
				addPersonOrRobotToBuildingRandomLocation(robot, destination);
				// throw new IllegalStateException("No inhabitable buildings available for " +
				// unit.getName());
			}
		}
	}
	
	/**
	 * Adds a vehicle to a random ground vehicle maintenance building within
	 * a settlement.
	 * 
	 * @param vehicle    the vehicle to add.
	 * @param settlement the settlement to find a building.
	 * @throws BuildingException if vehicle cannot be added to any building.
	 * 
	 * @return Building the garage it's in or has just been added to
	 */
	public Building addToGarageBuilding(Vehicle vehicle) {

		if (vehicle.isBeingTowed())
			return null;
		
		if (VehicleType.isRover(vehicle.getVehicleType())) {
			if (((Rover)vehicle).isTowingAVehicle())
				return null;
		}
		
		List<Building> garages = getGarages();
		
		if (garages.isEmpty()) {
			// This settlement has no garages at all
			vehicle.removeStatus(StatusType.GARAGED);
			return null;
		}
		
		for (Building garageBuilding : garages) {
			VehicleMaintenance garage = garageBuilding.getVehicleMaintenance();
			
			if (garage != null && garage.containsVehicle(vehicle)) {
				
				logger.log(settlement, vehicle, Level.INFO, 60_000, 
						   "Already inside " + garage.getBuilding().getNickName() + ".");
					
				return garageBuilding;
			}
			else {
				if (garage.getCurrentVehicleNumber() < garage.getVehicleCapacity()
					&& garage.addVehicle(vehicle)) {

					logger.log(settlement, vehicle, Level.INFO, 60_000, 
 							   "Just stowed inside " + garage.getBuilding().getNickName() + ".");
					return garageBuilding;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Removes a vehicle from garage
	 * 
	 * @param vehicle
	 * @return
	 */
	public static boolean removeFromGarage(Vehicle vehicle) {
		// If the vehicle is in a garage, put the vehicle outside.
		Building garage = getBuilding(vehicle);
		if (garage != null && garage.getVehicleMaintenance().removeVehicle(vehicle)) {
			return true;
		}
		
		return false;
	}

	/**
	 * Adds a vehicle to a random ground vehicle maintenance building within
	 * a settlement.
	 * 
	 * @param vehicle    the vehicle to add.
	 * @param settlement the settlement to find a building.
	 * @throws BuildingException if vehicle cannot be added to any building.
	 * 
	 * @return true if it's already in the garage or added to a garage 
	 */
	public boolean addToGarage(Vehicle vehicle) {
		if (vehicle.isBeingTowed())
			return false;
		
		if (VehicleType.isRover(vehicle.getVehicleType())) {
			if (((Rover)vehicle).isTowingAVehicle())
				return false;
		}
		
		List<Building> garages = getGarages();
		
		if (garages.isEmpty()) {
			// This settlement has no garages at all
			vehicle.removeStatus(StatusType.GARAGED);
			return false;
		}
		
		if (isInGarage(vehicle)) {
			vehicle.addStatus(StatusType.GARAGED);
			
			return true;
		}
		
		else {
			// Checks if this settlement have open garage space
			for (Building garageBuilding : garages) {
				VehicleMaintenance garage = garageBuilding.getVehicleMaintenance();
				if (garage.getCurrentVehicleNumber() < garage.getVehicleCapacity()
					&& garage.addVehicle(vehicle)) {
						
					// Update the vehicle's location state type
					vehicle.updateLocationStateType(LocationStateType.INSIDE_SETTLEMENT);
					
					logger.log(settlement, vehicle, Level.INFO, 60_000, 
 							   "Stowed inside " + garage.getBuilding().getNickName() + ".");
					return true;
				}
			}
		}
		
		return false;
	}


	/**
	 * Checks if the vehicle is currently in a garage or not.
	 * 
	 * @return true if vehicle is in a garage.
	 */
	public boolean isInGarage(Vehicle vehicle) {
		if (settlement != null) {
			for (Building garageBuilding : getGarages()) {
				VehicleMaintenance garage = garageBuilding.getVehicleMaintenance();
				if (garage != null && garage.containsVehicle(vehicle)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Checks if the vehicle is currently in a garage or not.
	 * 
	 * @return true if vehicle is in a garage.
	 */
	public static boolean isInAGarage(Vehicle vehicle) {
		if (vehicle == null)
			throw new IllegalArgumentException("vehicle is null");

		Settlement settlement = vehicle.getSettlement();
		if (settlement != null) {
			List<Building> list = settlement.getBuildingManager().getBuildings(FunctionType.GROUND_VEHICLE_MAINTENANCE);
			for (Building garageBuilding : list) {
				try {
					VehicleMaintenance garage = garageBuilding.getVehicleMaintenance();
					if (garage != null && garage.containsVehicle(vehicle)) {
						return true;
					}
				} catch (Exception e) {
					logger.log(null, vehicle, Level.SEVERE, 2000, "Not in a building.", e);
				}
			}
		}
		return false;
	}
	
	/**
	 * Gets the vehicle maintenance building a given vehicle is in.
	 * 
	 * @return building or null if none.
	 */
	public static Building getBuilding(Vehicle vehicle) {
		if (vehicle == null)
			throw new IllegalArgumentException("vehicle is null");
		Building result = null;
		Settlement settlement = vehicle.getSettlement();
		if (settlement != null) {
			List<Building> list = settlement.getBuildingManager().getBuildings(FunctionType.GROUND_VEHICLE_MAINTENANCE);
			for (Building garageBuilding : list) {
				try {
					VehicleMaintenance garage = garageBuilding.getVehicleMaintenance();
					if (garage != null && garage.containsVehicle(vehicle)) {
						return garageBuilding;
					}
				} catch (Exception e) {
					logger.log(settlement, vehicle, Level.SEVERE, 2000, 
							   "is not in a building.", e);
				}
			}
		}
		return result;
	}

	/**
	 * Gets the vehicle maintenance building a given vehicle is in.
	 * 
	 * @return building or null if none.
	 */
	public static Building getBuilding(Vehicle vehicle, Settlement settlement) {
		if (vehicle == null)
			throw new IllegalArgumentException("vehicle is null");
		if (settlement != null) {
			List<Building> list = settlement.getBuildingManager().getBuildings(FunctionType.GROUND_VEHICLE_MAINTENANCE);
			for (Building garageBuilding : list) {
				try {
					VehicleMaintenance garage = garageBuilding.getVehicleMaintenance();
					if (garage != null && garage.containsVehicle(vehicle)) {
						return garageBuilding;
					}
				} catch (Exception e) {
//					logger.log(Level.SEVERE, "Calling getBuilding(vehicle, settlement) : " + e.getMessage());
					logger.log(settlement, vehicle, Level.SEVERE, 2000, 
							   "is not in a building.", e);
				}
			}
		}
		return null;
	}

	/**
	 * Gets an available vehicle maintenance building for resource hookup.
	 * 
	 * @param settlement
	 * @return building or null if none.
	 */
	public static Building getAGarage(Settlement settlement) {
		if (settlement != null) {
			List<Building> list = settlement.getBuildingManager().getBuildings(FunctionType.GROUND_VEHICLE_MAINTENANCE);
			int size = list.size();
			int rand = RandomUtil.getRandomInt(size-1);
			return list.get(rand);
//			for (Building garageBuilding : list) {
//				try {
//					VehicleMaintenance garage = garageBuilding.getVehicleMaintenance();
//					if (garage != null) {
//						return garageBuilding;
//					}
//				} catch (Exception e) {
//					logger.log(Level.SEVERE, "Calling getAGarage(settlement) : " + e.getMessage());
//				}
//			}
		}
		return null;
	}
	
	/**
	 * Gets the building a person or robot is in.
	 * 
	 * @return building or null if none.
	 */
	public static Building getBuilding(Unit unit) {
		Building result = null;
		Person person = null;
		Robot robot = null;

		if (unit instanceof Person) {
			person = (Person) unit;
			if (person.isInSettlement()) {
				return person.getBuildingLocation();
			}
		} 
		
		else if (unit instanceof Robot) {
			robot = (Robot) unit;
			if (robot.isInSettlement()) {
				return robot.getBuildingLocation();
			}
		}
		return result;
	}

	/**
	 * Check what building a given local settlement position is within.
	 * 
	 * @param xLoc the local X position.
	 * @param yLoc the local Y position.
	 * @return building the position is within, or null if the position is not
	 *         within any building.
	 */
	public Building getBuildingAtPosition(double xLoc, double yLoc) {
        Iterator<Building> i = buildings.iterator();
        while (i.hasNext()) {
            Building building = i.next();
            if (LocalAreaUtil.isLocationWithinLocalBoundedObject(xLoc, yLoc, building)) {
                return building;
            }
        }

        return null;
	}

	/**
	 * Gets a list of uncrowded buildings from a given list of buildings with life
	 * support.
	 * 
	 * @param buildingList list of buildings with the life support function.
	 * @return list of buildings that are not at or above maximum occupant capacity.
	 * @throws BuildingException if building in list does not have the life support
	 *                           function.
	 */
	public static List<Building> getUncrowdedBuildings(List<Building> buildingList) {
		return buildingList.stream()
				.filter(b -> ((RoboticStation) b.getFunction(FunctionType.LIFE_SUPPORT))
				.getAvailableOccupancy() > 0)
				.collect(Collectors.toList());
	}

	/**
	 * Gets a list of the least crowded buildings from a given list of buildings
	 * with life support.
	 * 
	 * @param buildingList list of buildings with the life support function.
	 * @return list of least crowded buildings.
	 * @throws BuildingException if building in list does not have the life support
	 *                           function.
	 */
	public static List<Building> getLeastCrowdedBuildings(List<Building> buildingList) {

		List<Building> result = new ArrayList<>();

		// Find least crowded population.
		int leastCrowded = Integer.MAX_VALUE;
		for (Building b0 : buildingList) {
			if (!b0.getBuildingType().equalsIgnoreCase(Building.EVA_AIRLOCK)) {
				LifeSupport lifeSupport = b0.getLifeSupport();
				int crowded = lifeSupport.getOccupantNumber() - lifeSupport.getOccupantCapacity();
				if (crowded < -1)
					crowded = -1;
				if (crowded < leastCrowded) {
					// New leastCrowded so reset the list
					leastCrowded = crowded;
					result = new ArrayList<>();
					result.add(b0);
				}
				else if (crowded == leastCrowded) {
					result.add(b0);
				}
			}
		}

		return result;
	}

	public static List<Building> getLeastCrowded4BotBuildings(List<Building> buildingList) {

		List<Building> result = new ArrayList<Building>();

		// Find least crowded population.
		int leastCrowded = Integer.MAX_VALUE;
		for (Building building : buildingList) {
			RoboticStation roboticStation = building.getRoboticStation();
			int crowded = roboticStation.getRobotOccupantNumber() - roboticStation.getOccupantCapacity();
			if (crowded < -1)
				crowded = -1;
			if (crowded < leastCrowded) {
				leastCrowded = crowded;
				result = new ArrayList<>();
				result.add(building);
			}
			else if (crowded == leastCrowded) {
				result.add(building);
			}
		}

		return result;
	}

	/**
	 * Gets a map of buildings and their probabilities for being chosen based on the
	 * best relationships for a given person from a list of buildings.
	 * 
	 * @param person       the person to check for.
	 * @param buildingList the list of buildings to filter.
	 * @return map of buildings and their probabilities.
	 */
	public static Map<Building, Double> getBestRelationshipBuildings(Person person, List<Building> buildingList) {
		Map<Building, Double> result = new ConcurrentHashMap<Building, Double>(buildingList.size());
		// Determine probabilities based on relationships in buildings.
		for (Building building : buildingList) {
			if (!building.getBuildingType().equalsIgnoreCase(Building.EVA_AIRLOCK)) {
				LifeSupport lifeSupport = building.getLifeSupport();
				double buildingRelationships = 0D;
				int numPeople = 0;
				for (Person occupant : lifeSupport.getOccupants()) {
					if (person != occupant) {
						buildingRelationships += relationshipManager.getOpinionOfPerson(person, occupant);
						numPeople++;
					}
				}
				double prob = 50D;
				if (numPeople > 0) {
					prob = buildingRelationships / numPeople;
					if (prob < 0D) {
						prob = 0D;
					}
				}
				result.put(building, prob);
			}
		}
		return result;
	}

	/**
	 * Gets a map of buildings having on-going social conversations
	 * 
	 * @param buildingList the list of buildings to filter.
	 * @return map of buildings and their probabilities.
	 */
	public static List<Building> getChattyBuildings(List<Building> buildingList) {

		List<Building> result = new CopyOnWriteArrayList<Building>();
		for (Building building : buildingList) {
			if (!building.getBuildingType().equalsIgnoreCase(Building.EVA_AIRLOCK)) {
				LifeSupport lifeSupport = building.getLifeSupport();
				int numPeople = 0;
				for (Person occupant : lifeSupport.getOccupants()) {
					// Task task = occupant.getMind().getTaskManager().getTask();
					if (occupant.getMind().getTaskManager().getTask() instanceof HaveConversation) {
						numPeople++;
					}
				}
				if (numPeople > 0)
					result.add(building);
			}
		}
		return result;
	}

	/**
	 * Gets a list of buildings that don't have any malfunctions from a list of
	 * buildings.
	 * 
	 * @param buildingList the list of buildings.
	 * @return list of buildings without malfunctions.
	 */
	public static List<Building> getNonMalfunctioningBuildings(List<Building> buildingList) {
		return buildingList.stream().filter(b -> !b.getMalfunctionManager().hasMalfunction())
				.collect(Collectors.toList());
	}

	/**
	 * Gets a list of buildings that have a valid interior walking path from the
	 * person's current building.
	 * 
	 * @param person       the person.
	 * @param buildingList initial list of buildings.
	 * @return list of buildings with valid walking path.
	 */
	public static List<Building> getWalkableBuildings(Unit unit, List<Building> buildingList) {
		List<Building> result = new ArrayList<>();
		Person person = null;
		Robot robot = null;

		if (unit instanceof Person) {
			person = (Person) unit;
			Building currentBuilding = BuildingManager.getBuilding(person);
			if (currentBuilding != null) {
				BuildingConnectorManager connectorManager = person.getSettlement().getBuildingConnectorManager();

				for (Building building : buildingList) {
					InsideBuildingPath validPath = connectorManager.determineShortestPath(currentBuilding,
							currentBuilding.getXLocation(), currentBuilding.getYLocation(), building,
							building.getXLocation(), building.getYLocation());

					if (validPath != null) {
						result.add(building);
					}
				}
			}
		} else if (unit instanceof Robot) {
			robot = (Robot) unit;
			Building currentBuilding = BuildingManager.getBuilding(robot);
			if (currentBuilding != null) {
				BuildingConnectorManager connectorManager = robot.getSettlement().getBuildingConnectorManager();

				for (Building building : buildingList) {
					InsideBuildingPath validPath = connectorManager.determineShortestPath(currentBuilding,
							currentBuilding.getXLocation(), currentBuilding.getYLocation(), building,
							building.getXLocation(), building.getYLocation());

					if (validPath != null) {
						result.add(building);
					}
				}
			}
		}
		return result;
	}

	public static boolean isInBuildingAirlock(Person person) {
		Building b = person.getBuildingLocation();
        return b != null && b.getBuildingType().equalsIgnoreCase(Building.EVA_AIRLOCK);
    }
		
	/**
	 * Adds the person to the building if possible.
	 * 
	 * @param person   the person to add.
	 * @param building the building to add the person to.
	 */
	public static void addPersonOrRobotToBuilding(Worker unit, Building building) {
		if (building != null) {
			try {
				if (unit instanceof Person) {
					Person person = (Person) unit;
					LifeSupport lifeSupport = building.getLifeSupport();

					if (!lifeSupport.containsOccupant(person)) {
						lifeSupport.addPerson(person);
						
						person.setCurrentBuilding(building);
					}

				}

				else if (unit instanceof Robot) {
					Robot robot = (Robot) unit;
					RoboticStation roboticStation = building.getRoboticStation();

					if (!roboticStation.containsRobotOccupant(robot)) {
						roboticStation.addRobot(robot);
						
						robot.setCurrentBuilding(building);
					}
				}

			} catch (Exception e) {
				logger.log(building, unit, Level.SEVERE, SimLogger.DEFAULT_SEVERE_TIME, "Could not be added", e);
			}
		}
		
		else 
			logger.log(unit, Level.SEVERE, 2000, "The building is null.");
	}

	/**
	 * Adds the person to the building at a given location if possible.
	 * 
	 * @param person   the person to add.
	 * @param building the building to add the person to.
	 */
	public static void addPersonOrRobotToBuilding(Unit unit, Building building, double xLocation, double yLocation) {
		if (building != null) {

			if (!LocalAreaUtil.isLocationWithinLocalBoundedObject(xLocation, yLocation, building)) {
				throw new IllegalArgumentException(
						building.getNickName() + " does not contain location x: " + xLocation + ", y: " + yLocation);
			}

			try {
				if (unit instanceof Person) {
					Person person = (Person) unit;
					LifeSupport lifeSupport = building.getLifeSupport();

					if (!lifeSupport.containsOccupant(person)) {
						lifeSupport.addPerson(person);
						
						person.setXLocation(xLocation);
						person.setYLocation(yLocation);
						person.setCurrentBuilding(building);
					}
				}

				else if (unit instanceof Robot) {
					Robot robot = (Robot) unit;
					RoboticStation roboticStation = building.getRoboticStation();

					if (roboticStation.containsRobotOccupant(robot)) {
						roboticStation.addRobot(robot);
						
						robot.setXLocation(xLocation);
						robot.setYLocation(yLocation);
						robot.setCurrentBuilding(building);
					}
				}

			} catch (Exception e) {
				logger.log(building, unit, Level.SEVERE, 2000, "Could not be added", e);
			}
		} else {
			logger.severe(unit, "Building is null.");
		}
	}

	/**
	 * Adds the person to the building at a random location if possible.
	 * 
	 * @param person   the person to add.
	 * @param building the building to add the person to.
	 */
	public static void addPersonOrRobotToBuildingRandomLocation(Unit unit, Building building) {
		if (building != null) {
			try {
				// Add person to random location within building.
				// TODO: Modify this when implementing active locations in buildings.
				Point2D.Double buildingLoc = LocalAreaUtil.getRandomInteriorLocation(building);
				Point2D.Double settlementLoc = LocalAreaUtil.getLocalRelativeLocation(buildingLoc.getX(),
						buildingLoc.getY(), building);

				if (unit instanceof Person) {
					Person person = (Person) unit;
					LifeSupport lifeSupport = building.getLifeSupport();

					if (!lifeSupport.containsOccupant(person)) {
						lifeSupport.addPerson(person);
						
						person.setXLocation(settlementLoc.getX());
						person.setYLocation(settlementLoc.getY());
						person.setCurrentBuilding(building);
//						logger.config(person + " was being randomly added to " + building.getNickName());
					}
				}

				else if (unit instanceof Robot) {
					Robot robot = (Robot) unit;
					RoboticStation roboticStation = building.getRoboticStation();

					if (!roboticStation.containsRobotOccupant(robot)) {
						roboticStation.addRobot(robot);
						
						robot.setXLocation(settlementLoc.getX());
						robot.setYLocation(settlementLoc.getY());	
						robot.setCurrentBuilding(building);
					}
				}
			} catch (Exception e) {
				logger.log(building, unit, Level.SEVERE, 2000, "Could not be added", e);
			}
		} else {
			logger.log(unit, Level.SEVERE, 2000, "Building is null.");
		}
	}

	/**
	 * Removes the person from a building if possible.
	 * 
	 * @param person   the person to remove.
	 * @param building the building to remove the person from.
	 */
	public static void removePersonFromBuilding(Person person, Building building) {
		if (building != null) {
			try {

				LifeSupport lifeSupport = building.getLifeSupport();

				if (lifeSupport.containsOccupant(person)) {
					lifeSupport.removePerson(person);

					person.setCurrentBuilding(null);
				}


			} catch (Exception e) {
				logger.log(building, person, Level.SEVERE, 2000, "could not be removed", e);
			}
		} else {
			logger.log(person, Level.SEVERE, 2000, "Building is null.");
		}
	}

	/**
	 * Removes the robot from a building if possible.
	 * 
	 * @param robot   the robot to remove.
	 * @param building the building to remove the robot from.
	 */
	public static void removeRobotFromBuilding(Robot robot, Building building) {
		if (building != null) {
			try {

				RoboticStation roboticStation = building.getRoboticStation();

				if (roboticStation.containsRobotOccupant(robot)) {
					roboticStation.removeRobot(robot);
				
					robot.setCurrentBuilding(null);
				}

			} catch (Exception e) {
				logger.log(building, robot, Level.SEVERE, 2000,
						   " could not be removed", e);
			}
		} else {
//			throw new IllegalStateException("Building is null");
			logger.log(robot, Level.SEVERE, 2000, "building is null.");
		}
	}
	
	/**
	 * Gets the value of a building at the settlement.
	 * 
	 * @param buildingType the building type.
	 * @param newBuilding  true if adding a new building.
	 * @return building value (VP).
	 */
	public double getBuildingValue(String type, boolean newBuilding) {

		// Make sure building name is lower case.
		String buildingType = type.toLowerCase().trim();

		if (VPNewCache == null)
			VPNewCache = new ConcurrentHashMap<>();
		if (VPOldCache == null)
			VPOldCache = new ConcurrentHashMap<>();
		
		// Update building values cache once per Sol.
		// MarsClock currentTime =?
		if ((lastVPUpdateTime == null)
				|| (MarsClock.getTimeDiff(marsClock, lastVPUpdateTime) > 1000D)) {
			VPNewCache.clear();
			VPOldCache.clear();
			lastVPUpdateTime = (MarsClock) marsClock.clone();
		}
		
		if (newBuilding && VPNewCache.containsKey(buildingType)) {
			return VPNewCache.get(buildingType);
		} 
		
		else if (!newBuilding && VPOldCache.containsKey(buildingType)) {
			return VPOldCache.get(buildingType);
		} 
		
		else {
			double result = 0D;
			Settlement settlement = unitManager.getSettlementByID(settlementID);
			BuildingSpec spec = simulationConfig.getBuildingConfiguration().getBuildingSpec(buildingType);
			for(FunctionType supported : spec.getFunctionSupported()) {
				switch (supported) {
				
				case ADMINISTRATION:
					result += Administration.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case ASTRONOMICAL_OBSERVATION:
					result += AstronomicalObservation.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case BUILDING_CONNECTION:
					result += BuildingConnection.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case COMMUNICATION:
					result += Communication.getFunctionValue(buildingType, newBuilding, settlement);
					break;

				case COMPUTATION:
					result += Computation.getFunctionValue(buildingType, newBuilding, settlement);
					break;

				case COOKING:
					result += Cooking.getFunctionValue(buildingType, newBuilding, settlement);
					result += PreparingDessert.getFunctionValue(buildingType, newBuilding, settlement);
					break;

				case DINING:
					result += Dining.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case EARTH_RETURN:
					result += EarthReturn.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case EVA:
					result += EVA.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case EXERCISE:
					result += Exercise.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case FARMING:
					result += Farming.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case FISHERY:
					result += Fishery.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case FOOD_PRODUCTION:
					result += FoodProduction.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case GROUND_VEHICLE_MAINTENANCE:
					result += GroundVehicleMaintenance.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case LIFE_SUPPORT:
					result += LifeSupport.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case LIVING_ACCOMMODATIONS:
					result += LivingAccommodations.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case MANAGEMENT:
					result += Management.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case MANUFACTURE:
					result += Manufacture.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case MEDICAL_CARE:
					result += MedicalCare.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case POWER_GENERATION:
					result += PowerGeneration.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case POWER_STORAGE:
					result += PowerStorage.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case RECREATION:
					result += Recreation.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case RESEARCH:
					result += Research.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case RESOURCE_PROCESSING:
					result += ResourceProcessing.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case ROBOTIC_STATION:
					result += RoboticStation.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case STORAGE:
					result += Storage.getFunctionValue(buildingType, newBuilding, settlement);
					break;
					
				case THERMAL_GENERATION:
					result += ThermalGeneration.getFunctionValue(buildingType, newBuilding, settlement);
					break;

				case WASTE_DISPOSAL:
					// Nothing to count here
					break;
					
				default:
					throw new IllegalArgumentException("Do not know how to build Function " + supported);
				}
			}

			// Multiply value.
			result *= 1000D;

			// Subtract power costs per Sol.
			double power = spec.getBasePowerRequirement();
			double powerPerSol = power * MarsClock.HOURS_PER_MILLISOL;
			double powerValue = powerPerSol * settlement.getPowerGrid().getPowerValue();
			result -= powerValue;

			if (result < 0D)
				result = 0D;

			// Check if a new non-constructable building has a frame that already exists at
			// the settlement.
			if (newBuilding) {
				ConstructionStageInfo buildingConstInfo = ConstructionUtil.getConstructionStageInfo(buildingType);
				if (buildingConstInfo != null) {
					ConstructionStageInfo frameConstInfo = ConstructionUtil.getPrerequisiteStage(buildingConstInfo);
					if (frameConstInfo != null) {
						// Check if frame is not constructable.
						if (!frameConstInfo.isConstructable()) {
							// Check if the building's frame exists at the settlement.
							if (!hasBuildingFrame(frameConstInfo.getName())) {
								// If frame doesn't exist and isn't constructable, the building has zero value.
								result = 0D;
							}
						}
					}
				}
			}

			// System.out.println("Building " + buildingType + " value: " + (int) result);

			if (newBuilding)
				VPNewCache.put(buildingType, result);
			else
				VPOldCache.put(buildingType, result);

			return result;
		}
	}

	/**
	 * Gets the value of a building at the settlement.
	 * 
	 * @param building the building.
	 * @return building value (VP).
	 */
	public double getBuildingValue(Building building) {
		double result = 0D;

		result = getBuildingValue(building.getBuildingType(), false);

		// Modify building value by its wear condition.
		double wearCondition = building.getMalfunctionManager().getWearCondition();
		result *= (wearCondition / 100D) * .75D + .25D;

		return result;
	}

	/**
	 * Checks if a proposed building location is open or intersects with existing
	 * buildings or construction sites.
	 * 
	 * @param xLoc   the new building's X location.
	 * @param yLoc   the new building's Y location.
	 * @param width  the new building's width (meters).
	 * @param length the new building's length (meters).
	 * @param facing the new building's facing (degrees clockwise from North).
	 * @return true if new building location is open.
	 */
	public boolean isBuildingLocationOpen(double xLoc, double yLoc, double width, double length, double facing) {
		return isBuildingLocationOpen(xLoc, yLoc, width, length, facing, null);
	}

	/**
	 * Checks if a proposed building location is open or intersects with existing
	 * buildings or construction sites.
	 * 
	 * @param xLoc   the new building's X location.
	 * @param yLoc   the new building's Y location.
	 * @param width  the new building's width (meters).
	 * @param length the new building's length (meters).
	 * @param facing the new building's facing (degrees clockwise from North).
	 * @param site   the new construction site or null if none.
	 * @return true if new building location is open.
	 */
	public boolean isBuildingLocationOpen(double xLoc, double yLoc, double width, double length, double facing,
			ConstructionSite site) {
		boolean goodLocation = true;

		goodLocation = LocalAreaUtil.isObjectCollisionFree(site, width, length, xLoc, yLoc, facing,
				unitManager.getSettlementByID(settlementID).getCoordinates());

		return goodLocation;
	}

	/**
	 * Checks if a building frame exists at the settlement. Either with an existing
	 * building or at a construction site.
	 * 
	 * @param frameName the frame's name.
	 * @return true if frame exists.
	 */
	public boolean hasBuildingFrame(String frameName) {
		boolean result = false;

		// Check if any existing buildings have this frame.
		for (Building building : buildings) {
			// TODO: determine if getName() needed to be changed to getNickName()
			ConstructionStageInfo buildingStageInfo = ConstructionUtil
					.getConstructionStageInfo(building.getBuildingType());
			if (buildingStageInfo != null) {
				ConstructionStageInfo frameStageInfo = ConstructionUtil.getPrerequisiteStage(buildingStageInfo);
				if (frameStageInfo != null) {
					// TODO: determine if getName() needed to be changed to getNickName()
					if (frameStageInfo.getName().equals(frameName)) {
						result = true;
						break;
					}
				}
			}
		}

		// Check if any construction projects have this frame.
		if (!result) {
			ConstructionStageInfo frameStageInfo = ConstructionUtil.getConstructionStageInfo(frameName);
			if (frameStageInfo != null) {
				ConstructionManager constManager = unitManager.getSettlementByID(settlementID).getConstructionManager();
				Iterator<ConstructionSite> j = constManager.getConstructionSites().iterator();
				while (j.hasNext()) {
					ConstructionSite site = j.next();
					if (site.hasStage(frameStageInfo)) {
						result = true;
						break;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Obtains the inhabitable building having that particular id
	 * 
	 * @param id
	 * @return inhabitable building
	 */
	public Building getInhabitableBuilding(int id) {
		// Use Java 8 stream
//		return buildings.stream().filter(b -> b.getInhabitableID() == id).findFirst().orElse(null);// .get();
//    	Building result = null;
        Iterator<Building> i = buildings.iterator();
        while (i.hasNext()) {
            Building b = i.next();
            if (b.getInhabitableID() == id) {
            	return b;
            }
        }

        return null;
	}

	/**
	 * Gets the next template ID for a new building in a settlement (but not unique
	 * in a simulation)
	 * 
	 * @return template ID (starting from 0).
	 */
	public int getNextTemplateID() {

		int largestID = 0;
		for (Building building : buildings) {
			int id = building.getTemplateID();
			if (id > largestID) {
				largestID = id;
			}
		}

		return largestID + 1;
	}

	/**
	 * Gets a unique ID for a new inhabitable building in a settlement (but not
	 * unique in a simulation)
	 * 
	 * @return inhabitable ID (starting from 0).
	 */
	public int obtainNextInhabitableID() {
		return nextInhabitableID++;
		
//		int max = -1;
//		for (Building b : buildings) {
//			if (b.hasFunction(FunctionType.LIFE_SUPPORT)) {
//				int id = b.getInhabitableID();
//				max = Math.max(id, max);
//				// if (id > nextNum)
//				// nextNum++;
//			}
//		}
//		
////		System.out.println("getNextInhabitableID() " + buildings.size());
//		return max + 1;
	}

	/**
	 * Gets the total number of inhabitable buildings in a settlement
	 * 
	 * @return inhabitable ID (starting from 0).
	 */
	public int getNumInhabitables() {
		return nextInhabitableID;
//		int max = -1;
//		for (Building b : buildings) {
//			if (b.hasFunction(FunctionType.LIFE_SUPPORT)) {
//				int id = b.getInhabitableID();
//				max = Math.max(id, max);
//			}
//		}
//		return max;
	}

	/**
	 * Creates a map of building type id
	 * 
	 * @param b a given building
	 */
	public void createBuildingTypeIDMap() {
		for (Building b : buildings) {
			String buildingType = b.getBuildingType();
			String n = b.getNickName();
			int new_id = Integer.parseInt(b.getNickName().substring(n.lastIndexOf(" ") + 1, n.length()));
	
			if (buildingTypeIDMap.containsKey(buildingType)) {
				int old_id = buildingTypeIDMap.get(buildingType);
				if (old_id < new_id)
					buildingTypeIDMap.put(buildingType, new_id);
			} else
				buildingTypeIDMap.put(buildingType, new_id);
		}
	}

	/**
	 * Gets an available building type ID for a new building.
	 * 
	 * @param buildingType
	 * @return type ID (starting from 1).
	 */
	public int getNextBuildingTypeID(String buildingType) {
		int id = 1;
		if (buildingTypeIDMap.containsKey(buildingType)) {
			id = buildingTypeIDMap.get(buildingType);
			buildingTypeIDMap.put(buildingType, id + 1);
			return id;
		} else {
			buildingTypeIDMap.put(buildingType, id);
			return id;
		}

//        int largest = 0;
//        Iterator<Building> i = buildings.iterator();
//        while (i.hasNext()) {
//            Building b = i.next();
//            String type = b.getBuildingType();
//            if (buildingType.equals(type)) {
//            	int id = b.getTemplateID();
//            	//largest = Math.max(id, largest);
//            	if (id > largest)
//            	largest++;
//            }
//        }
//
//        return largest + 1;
	}

	/**
	 * Gets a unique nick name for a new building
	 * 
	 * @return a unique nick name
	 */
	public String getBuildingNickName(String buildingType) {
		return buildingType + " " + getNextBuildingTypeID(buildingType);
	}

//	private String getCharForNumber(int i) {
//		// NOTE: i must be > 1, if i = 0, return null
//	    return i > 0 && i < 27 ? String.valueOf((char)(i + 'A' - 1)) : null;
//	}

	/**
	 * Gets a list of farm buildings needing work from a list of buildings with the
	 * farming function.
	 * 
	 * @param buildingList list of buildings with the farming function.
	 * @return list of farming buildings needing work.
	 */
	public List<Building> getFarmsNeedingWork() {
		List<Building> result = null;

		if (farmsNeedingWorkCache == null)
			farmsNeedingWorkCache = new CopyOnWriteArrayList<>();
		
		// Must use the absolute time otherwise it stalls after one sol day
		double m = marsClock.getTotalMillisols();
		
		// Add caching and relocate from TendGreenhouse
		if ((farmTimeCache + 20) >= m && !farmsNeedingWorkCache.isEmpty()) {
			result = farmsNeedingWorkCache;
		}

		else {
			farmTimeCache = m;
			List<Building> farmBuildings = getLeastCrowdedBuildings(
					getNonMalfunctioningBuildings(getBuildings(FunctionType.FARMING)));
			result = new CopyOnWriteArrayList<Building>();
			for (Building b : farmBuildings) {
				Farming farm = b.getFarming();
				if (farm.requiresWork()) {
					result.add(b);
				}
			}

			farmsNeedingWorkCache = result;
		}
		return result;
	}

	/**
	 * Gets an available building with the recreational function.
	 * 
	 * @param person the person looking for the recreational facility.
	 * @return an available space or null if none found.
	 */
	public static Building getAvailableRecBuilding(Person person) {
		Building result = null;

		// If person is in a settlement, try to find a building with an office.
		if (person.isInSettlement()) {
			
			List<Building> bldgs = person.getSettlement().getBuildingManager().getBuildings(FunctionType.RECREATION);
			bldgs = getNonMalfunctioningBuildings(bldgs);
			bldgs = getLeastCrowdedBuildings(bldgs);

			if (bldgs.size() > 0) {
				Map<Building, Double> selectedBldgs = getBestRelationshipBuildings(person, bldgs);
				result = RandomUtil.getWeightedRandomObject(selectedBldgs);
			}
		}

		return result;
	}
	
	/**
	 * Gets an available building with the comm function.
	 * 
	 * @param person the person looking for the comm facility.
	 * @return an available space or null if none found.
	 */
	public static Building getAvailableCommBuilding(Person person) {
		Building result = null;

		// If person is in a settlement, try to find a building with an office.
		if (person.isInSettlement()) {
			
			List<Building> bldgs = person.getSettlement().getBuildingManager().getBuildings(FunctionType.COMMUNICATION);
			bldgs = getNonMalfunctioningBuildings(bldgs);
			bldgs = getLeastCrowdedBuildings(bldgs);

			if (bldgs.size() > 0) {
				Map<Building, Double> selectedBldgs = getBestRelationshipBuildings(person, bldgs);
				result = RandomUtil.getWeightedRandomObject(selectedBldgs);
			}
		}

		return result;
	}

	/**
	 * Gets an available building with the admin function.
	 * 
	 * @param person the person looking for the admin facility.
	 * @return an available space or null if none found.
	 */
	public static Building getAvailableAdminBuilding(Person person) {
		Building result = null;

		// If person is in a settlement, try to find a building with an office.
		if (person.isInSettlement()) {
			
			List<Building> bldgs = person.getSettlement().getBuildingManager().getBuildings(FunctionType.ADMINISTRATION);
			bldgs = getNonMalfunctioningBuildings(bldgs);
			bldgs = getLeastCrowdedBuildings(bldgs);

			if (bldgs.size() > 0) {
				Map<Building, Double> selectedBldgs = getBestRelationshipBuildings(person, bldgs);
				result = RandomUtil.getWeightedRandomObject(selectedBldgs);
			}
		}

		return result;
	}
	
	// This method is called by MeteoriteImpactImpl
	public void setProbabilityOfImpactPerSQMPerSol(double value) {
		probabilityOfImpactPerSQMPerSol = value;
	}

	// Called by each building once a sol to see if an impact is imminent
	public double getProbabilityOfImpactPerSQMPerSol() {
		return probabilityOfImpactPerSQMPerSol;
	}

	public void setDebrisMass(double value) {
		debrisMass = value;
	}
	
	public double getDebrisMass() {
		return debrisMass;
	}
	
	public void setWallPenetration(double value) {
		wallPenetrationThicknessAL = value;
	}

	public double getWallPenetration() {
		return wallPenetrationThicknessAL;
	}

	public Meteorite getMeteorite() {
		return meteorite;
	}

	/**
	 * Gets the building manager's settlement.
	 *
	 * @return settlement
	 */
	public Settlement getSettlement() {
		return settlement;
	}

	/**
	 * Gets an instance of the historical event manager
	 * 
	 * @return
	 */
	public HistoricalEventManager getEventManager() {
		return eventManager;
	}

	/**
	 * Gets a list of garages for the settlement
	 * 
	 * @return
	 */
	public List<Building> getGarages() {
		return garages; 
	}
	
	/**
	 * Reloads instances after loading from a saved sim
	 * 
	 * @param {@link MasterClock}
	 * @param {{@link MarsClock}
	 */
	public static void initializeInstances(Simulation s, MasterClock c0, MarsClock c1,
			HistoricalEventManager e, RelationshipManager r, UnitManager u) {
		sim = s;
		simulationConfig = SimulationConfig.instance();
		masterClock = c0;
		marsClock = c1;
		eventManager = e;
		relationshipManager = r;
		unitManager = u;
	}
			
	/**
	 * Reconstruct the building lists after loading from a saved sim
	 */
	public void reinit() {
		settlement = unitManager.getSettlementByID(settlementID);
		
		buildings = new CopyOnWriteArrayList<>();
		for (Integer i : buildingInts) {
			buildings.add(unitManager.getBuildingByID(i));
		}
		garages = new CopyOnWriteArrayList<>();
		for (Integer i : garageInts) {
			garages.add(unitManager.getBuildingByID(i));
		}
	}
	
	
	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		Iterator<Building> i = buildings.iterator();
		while (i.hasNext()) {
			i.next().destroy();
		}
		// buildings.clear();
		buildings = null;
//		settlement = null;
		// buildingValuesNewCache.clear();
		VPNewCache = null;
		// buildingValuesOldCache.clear();
		VPOldCache = null;
		lastVPUpdateTime = null;
//		resupply = null;
		meteorite = null;
		marsClock = null;
		masterClock = null;
	}

}
