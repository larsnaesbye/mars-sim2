/*
 * Mars Simulation Project
 * Building.java
 * @date 2024-07-03
 * @author Scott Davis
 */
package com.mars_sim.core.structure.building;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.mars_sim.core.Simulation;
import com.mars_sim.core.SimulationConfig;
import com.mars_sim.core.Unit;
import com.mars_sim.core.UnitEventType;
import com.mars_sim.core.UnitType;
import com.mars_sim.core.air.AirComposition;
import com.mars_sim.core.data.UnitSet;
import com.mars_sim.core.equipment.ItemHolder;
import com.mars_sim.core.equipment.ResourceHolder;
import com.mars_sim.core.events.HistoricalEvent;
import com.mars_sim.core.events.HistoricalEventManager;
import com.mars_sim.core.hazard.HazardEvent;
import com.mars_sim.core.location.LocationStateType;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.malfunction.Malfunction;
import com.mars_sim.core.malfunction.MalfunctionFactory;
import com.mars_sim.core.malfunction.MalfunctionManager;
import com.mars_sim.core.malfunction.Malfunctionable;
import com.mars_sim.core.person.EventType;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.PhysicalCondition;
import com.mars_sim.core.person.ai.NaturalAttributeType;
import com.mars_sim.core.person.ai.task.Repair;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.science.ScienceType;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.Structure;
import com.mars_sim.core.structure.building.connection.InsidePathLocation;
import com.mars_sim.core.structure.building.function.Administration;
import com.mars_sim.core.structure.building.function.AstronomicalObservation;
import com.mars_sim.core.structure.building.function.BuildingConnection;
import com.mars_sim.core.structure.building.function.Communication;
import com.mars_sim.core.structure.building.function.Computation;
import com.mars_sim.core.structure.building.function.EVA;
import com.mars_sim.core.structure.building.function.EarthReturn;
import com.mars_sim.core.structure.building.function.Exercise;
import com.mars_sim.core.structure.building.function.FoodProduction;
import com.mars_sim.core.structure.building.function.Function;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.LifeSupport;
import com.mars_sim.core.structure.building.function.LivingAccommodation;
import com.mars_sim.core.structure.building.function.Management;
import com.mars_sim.core.structure.building.function.Manufacture;
import com.mars_sim.core.structure.building.function.MedicalCare;
import com.mars_sim.core.structure.building.function.Recreation;
import com.mars_sim.core.structure.building.function.Research;
import com.mars_sim.core.structure.building.function.ResourceProcessing;
import com.mars_sim.core.structure.building.function.RoboticStation;
import com.mars_sim.core.structure.building.function.Storage;
import com.mars_sim.core.structure.building.function.SystemType;
import com.mars_sim.core.structure.building.function.VehicleGarage;
import com.mars_sim.core.structure.building.function.VehicleMaintenance;
import com.mars_sim.core.structure.building.function.WasteProcessing;
import com.mars_sim.core.structure.building.function.cooking.Cooking;
import com.mars_sim.core.structure.building.function.cooking.Dining;
import com.mars_sim.core.structure.building.function.cooking.PreparingDessert;
import com.mars_sim.core.structure.building.function.farming.AlgaeFarming;
import com.mars_sim.core.structure.building.function.farming.Farming;
import com.mars_sim.core.structure.building.function.farming.Fishery;
import com.mars_sim.core.structure.building.task.MaintainBuilding;
import com.mars_sim.core.structure.building.utility.heating.Heating;
import com.mars_sim.core.structure.building.utility.heating.ThermalGeneration;
import com.mars_sim.core.structure.building.utility.power.PowerGeneration;
import com.mars_sim.core.structure.building.utility.power.PowerMode;
import com.mars_sim.core.structure.building.utility.power.PowerStorage;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.Temporal;
import com.mars_sim.mapdata.location.BoundedObject;
import com.mars_sim.mapdata.location.LocalBoundedObject;
import com.mars_sim.mapdata.location.LocalPosition;
import com.mars_sim.tools.util.RandomUtil;

/**
 * The Building class is a settlement's building.
 */
public class Building extends Structure implements Malfunctionable, Indoor, 
		LocalBoundedObject, InsidePathLocation, Temporal, ResourceHolder, ItemHolder {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	// default logger.
	private static final SimLogger logger = SimLogger.getLogger(Building.class.getName());

	public static final int TISSUE_CAPACITY = 20;

	/** The height of an airlock in meters */
	// Assume an uniform height of 2.5 meters in all buildings
	public static final double HEIGHT = 2.5;
	/** 500 W heater for use during EVA ingress */
	public static final double EVA_HEATER_KW = .5D;
	// Assuming 20% chance for each person to witness or be conscious of the
	// meteorite impact in an affected building
	public static final double METEORITE_IMPACT_PROBABILITY_AFFECTED = 20;

	// Data members
	boolean isImpactImminent = false;
	/** Checked by getAllImmovableBoundedObjectsAtLocation() in LocalAreaUtil */
	boolean inTransportMode = true;
	
	/** The designated zone where this building is located at. */
	private int zone;
	/** Unique template id assigned for the settlement template of this building belong. */
	private String templateID;
	/** The base level for this building. -1 for in-ground, 0 for above-ground. */
	private int baseLevel;

	/** Default : 22.5 deg celsius. */
	private double presetTemperature = 0;//22.5D;
	private double width;
	// Q: how to handle the indefinite length of hallway/tunnel ?
	// "-1" if it doesn't exist.
	private double length;
	private double floorArea;
	private double facing;
	private double baseFullPowerRequirement;
	private double baseLowPowerRequirement;
	private double powerNeededForEVAHeater;
	
	/** Type of building. */
	private String buildingType;
	/** Description for this building. */
	private String description;

	/** Unique identifier for the settlement of this building. */
	private Integer settlementID;
	
	/** The settlement of this building. */
	private transient Settlement settlement;

	/** The MalfunctionManager instance. */
	protected MalfunctionManager malfunctionManager;

	private Administration admin;
	private AlgaeFarming algae;
	private AstronomicalObservation astro;
	private Communication comm;
	private Computation computation;
	private Cooking cooking;
	private Dining dine;
	private EarthReturn earthReturn;
	private EVA eva;
	private Exercise gym;
	private Farming farm;
	private Fishery fish;
	private FoodProduction foodFactory;
	private VehicleGarage maint;
	private Heating heating;
	private LivingAccommodation livingAccommodation;
	private LifeSupport lifeSupport;
	private Management management;
	private Manufacture manufacture;
	private MedicalCare medicalCare;
	private ThermalGeneration furnace;
	private PowerGeneration powerGen;
	private PowerStorage powerStorage;
	private PreparingDessert preparingDessert;
	private Recreation rec;
	private Research lab;
	private ResourceProcessing resourceProcessing;
	private RoboticStation roboticStation;
	private Storage storage;
	private VehicleMaintenance vehicleMaintenance;
	private WasteProcessing wasteProcessing;

	private PowerMode powerModeCache;
	private BuildingCategory category;
	private ConstructionType constructionType;
	
	private LocalPosition loc;
	
	/** A list of functions of this building. */
	private List<Function> functions = new ArrayList<>();
	
	private static HistoricalEventManager eventManager;

	/**
	 * Factory method to create from a building template
	 *
	 * @param template the building template.
	 * @param owner The owning Settlement
	 */
	public static Building createBuilding(BuildingTemplate template, Settlement owner) {
		var spec = BuildingManager.getBuildingConfig().getBuildingSpec(template.getBuildingType());

		return new Building(owner, template.getID(), template.getZone(), template.getBuildingName(),
							template.getBounds(), spec);
	}

	/**
	 * Constructor 1: the mandatory properties. 
	 *
	 * @param id           the building's unique ID number.
	 * @param buildingType the building Type.
	 * @param name         the building's name.
	 * @param bounds       the physical position of this Building
	 */
	public Building(Settlement owner, String id, int zone, String name,
					BoundedObject bounds, String buildingType, BuildingCategory category) {
		super(name, owner.getCoordinates());

		this.templateID = id;
		this.zone = zone;
		this.buildingType = buildingType;
		this.category = category;

		this.settlement = owner;
		this.settlementID = settlement.getIdentifier();
		setContainerID(settlementID);

		this.loc = bounds.getPosition();
		this.facing = bounds.getFacing();
		this.width = bounds.getWidth();
		this.length = bounds.getLength();
		
		if (length < 0) {
			logger.info(this, "length: " + length);
		}

		this.powerModeCache = PowerMode.FULL_POWER;

		if (length == width) {
			// For Habs and Hubs that have a circular footprint
			this.floorArea = Math.PI * .25 * 
					length * length;
		}
		else
			this.floorArea = length * width;
		
		if (floorArea <= 0) {
			throw new IllegalArgumentException("Floor area cannot be -ve w=" + width + ", l=" + length);
		}
	}

	/**
	 * Constructor 2 : Constructs a Building object.
	 * Called by Constructor 1 and ConstructionSite's createBuilding().
	 *
	 * @param id           the building's unique ID number.
	 * @param buildingType the building Type.
	 * @param name         the building's name.
	 * @param bounds       the physical position of this Building
	 * @param manager      the building's building manager.
	 * @throws BuildingException if building can not be created.
	 */
	public Building(Settlement owner, String id, int zone, String name, BoundedObject bounds,
						BuildingSpec buildingSpec) {
		this(owner, id, zone, name, buildingSpec.getValidBounds(bounds), buildingSpec.getName(), buildingSpec.getCategory());

		constructionType = buildingSpec.getConstruction();

		// Sets the base mass
		setBaseMass(buildingSpec.getBaseMass());

		baseLevel = buildingSpec.getBaseLevel();
		description = buildingSpec.getDescription();

		// Get base power requirements.
		baseFullPowerRequirement = buildingSpec.getBasePowerRequirement();
		baseLowPowerRequirement = buildingSpec.getBasePowerDownPowerRequirement();

		// Set room temperature
		presetTemperature = buildingSpec.getPresetTemperature();
		
		// Determine total maintenance time.
		double totalMaintenanceTime = buildingSpec.getMaintenanceTime();
		for (FunctionType supported : buildingSpec.getFunctionSupported()) {
			FunctionSpec fSpec = buildingSpec.getFunctionSpec(supported);

			var mFunction = addFunction(fSpec);
			totalMaintenanceTime += mFunction.getMaintenanceTime();
		}

		// Set up malfunction manager.
		malfunctionManager = new MalfunctionManager(this, buildingSpec.getWearLifeTime(), totalMaintenanceTime);
		// Add 'Building' to malfunction manager.
		malfunctionManager.addScopeString(SystemType.BUILDING.getName());
		
		// Add building type to the standard scope
		SimulationConfig.instance().getPartConfiguration().addScopes(buildingSpec.getName());
		
		// Add building type to malfunction manager.
		malfunctionManager.addScopeString(buildingSpec.getName());
			
		// Add each function to the malfunction scope.
		for (Function sfunction : functions) {
			Set<String> scopes = sfunction.getMalfunctionScopeStrings();
			for (String scope : scopes) {
				malfunctionManager.addScopeString(scope);
			}
		}

		// If no life support then no internal repairs
		malfunctionManager.setSupportInsideRepair(hasFunction(FunctionType.LIFE_SUPPORT));
	}


	/**
	 * Gets the description of a building.
	 *
	 * @return String description
	 */
	@Override
	public String getDescription() {
		return description;
	}

	/**
	 * Gets the category of this building.
	 */
	public BuildingCategory getCategory() {
		return category;
	}

	/**
	 * Gets the preset temperature of a building.
	 *
	 * @return temperature (deg C)
	 */
	public double getPresetTemperature() {
		return presetTemperature;
	}


	public Administration getAdministration() {
		if (admin == null)
			admin = (Administration) getFunction(FunctionType.ADMINISTRATION);
		return admin;
	}

	public AlgaeFarming getAlgae() {
		if (algae == null)
			algae = (AlgaeFarming) getFunction(FunctionType.ALGAE_FARMING);
		return algae;
	}
	
	public AstronomicalObservation getAstronomicalObservation() {
		if (astro == null)
			astro = (AstronomicalObservation) getFunction(FunctionType.ASTRONOMICAL_OBSERVATION);
		return astro;
	}

	public Dining getDining() {
		if (dine == null)
			dine = (Dining) getFunction(FunctionType.DINING);
		return dine;
	}

	public LifeSupport getLifeSupport() {
		if (lifeSupport == null)
			lifeSupport = (LifeSupport) getFunction(FunctionType.LIFE_SUPPORT);
		return lifeSupport;
	}

	public Farming getFarming() {
		if (farm == null)
			farm = (Farming) getFunction(FunctionType.FARMING);
		return farm;
	}

	public Fishery getFishery() {
		if (fish == null)
			fish = (Fishery) getFunction(FunctionType.FISHERY);
		return fish;
	}

	public Communication getComm() {
		if (comm == null)
			comm = (Communication) getFunction(FunctionType.COMMUNICATION);
		return comm;
	}


	public Cooking getCooking() {
		if (cooking == null)
			cooking = (Cooking) getFunction(FunctionType.COOKING);
		return cooking;
	}


	public Exercise getExercise() {
		if (gym == null)
			gym = (Exercise) getFunction(FunctionType.EXERCISE);
		return gym;
	}

	public EVA getEVA() {
		if (eva == null)
			eva = (EVA) getFunction(FunctionType.EVA);
		return eva;
	}

	public EarthReturn getEarthReturn() {
		if (earthReturn == null)
			earthReturn = (EarthReturn) getFunction(FunctionType.EARTH_RETURN);
		return earthReturn;
	}

	public FoodProduction getFoodProduction() {
		if (foodFactory == null)
			foodFactory = (FoodProduction) getFunction(FunctionType.FOOD_PRODUCTION);
		return foodFactory;
	}

	public VehicleGarage getVehicleParking() {
		if (maint == null)
			maint = (VehicleGarage) getFunction(FunctionType.VEHICLE_MAINTENANCE);
		return maint;
	}

	public LivingAccommodation getLivingAccommodation() {
		if (livingAccommodation == null)
			livingAccommodation = (LivingAccommodation) getFunction(FunctionType.LIVING_ACCOMMODATION);
		return livingAccommodation;
	}

	public Management getManagement() {
		if (management == null)
			management = (Management) getFunction(FunctionType.MANAGEMENT);
		return management;
	}

	public Manufacture getManufacture() {
		if (manufacture == null)
			manufacture = (Manufacture) getFunction(FunctionType.MANUFACTURE);
		return manufacture;
	}

	public MedicalCare getMedical() {
		if (medicalCare == null)
			medicalCare = (MedicalCare) getFunction(FunctionType.MEDICAL_CARE);
		return medicalCare;
	}

	public PowerGeneration getPowerGeneration() {
		if (powerGen == null)
			powerGen = (PowerGeneration) getFunction(FunctionType.POWER_GENERATION);
		return powerGen;
	}

	public Computation getComputation() {
		if (computation == null)
			computation = (Computation) getFunction(FunctionType.COMPUTATION);
		return computation;
	}

	public PowerStorage getPowerStorage() {
		if (powerStorage == null)
			powerStorage = (PowerStorage) getFunction(FunctionType.POWER_STORAGE);
		return powerStorage;
	}

	public PreparingDessert getPreparingDessert() {
		if (preparingDessert == null)
			preparingDessert = (PreparingDessert) getFunction(FunctionType.PREPARING_DESSERT);
		return preparingDessert;
	}

	public Recreation getRecreation() {
		if (rec == null)
			rec = (Recreation) getFunction(FunctionType.RECREATION);
		return rec;
	}

	public Research getResearch() {
		if (lab == null)
			lab = (Research) getFunction(FunctionType.RESEARCH);
		return lab;
	}

	public ResourceProcessing getResourceProcessing() {
		if (resourceProcessing == null)
			resourceProcessing = (ResourceProcessing) getFunction(FunctionType.RESOURCE_PROCESSING);
		return resourceProcessing;
	}
	
	public RoboticStation getRoboticStation() {
		if (roboticStation == null)
			roboticStation = (RoboticStation) getFunction(FunctionType.ROBOTIC_STATION);

		return roboticStation;
	}

	public ThermalGeneration getThermalGeneration() {
		if (furnace == null)
			furnace = (ThermalGeneration) getFunction(FunctionType.THERMAL_GENERATION);
		return furnace;
	}

	public Storage getStorage() {
		if (storage == null)
			storage = (Storage) getFunction(FunctionType.STORAGE);
		return storage;
	}

	public VehicleMaintenance getVehicleMaintenance() {
		if (vehicleMaintenance == null)
			vehicleMaintenance = (VehicleMaintenance) getFunction(FunctionType.VEHICLE_MAINTENANCE);
		return vehicleMaintenance;
	}

	public WasteProcessing getWasteProcessing() {
		if (wasteProcessing == null)
			wasteProcessing = (WasteProcessing) getFunction(FunctionType.WASTE_PROCESSING);
		return wasteProcessing;
	}
	
	/**
	 * Gets the temperature of a building.
	 *
	 * @return temperature (deg C)
	 */
	public double getCurrentTemperature() {
		if (heating != null)
			return heating.getCurrentTemperature();
		else {
			return presetTemperature;
		}
	}

	/**
	 * Gets a function type that has with openly available (empty) activity spot.
	 *
	 * @return FunctionType
	 */
	public FunctionType getEmptyActivitySpotFunctionType() {
		Function f = getEmptyActivitySpotFunction();
		if (f != null)
			return f.getFunctionType();
		else
			return null;
	}

	/**
	 * Gets a function that has with openly available (empty) activity spot.
	 *
	 * @return FunctionType
	 */
	public Function getEmptyActivitySpotFunction() {
		List<Function> goodFunctions = new ArrayList<>();

		// First, use recreation function's empty activity spot if available
		Function rec = getFunction(FunctionType.RECREATION);
		if (rec != null && rec.hasEmptyActivitySpot()) {
			return rec;
		}
								
		for (Function f : functions) {
			if (f.getFunctionType() != FunctionType.EVA 
				&& f.hasEmptyActivitySpot())
				goodFunctions.add(f);
		}

		if (goodFunctions.isEmpty())
			return null;

		// Choose a random function
		int index = RandomUtil.getRandomInt(goodFunctions.size() - 1);

		return goodFunctions.get(index);
	}

	/**
	 * Gets a local activity spot that is available (empty).
	 *
	 * @return FunctionType
	 */
	public LocalPosition getRandomEmptyActivitySpot() {
		
		// First, use the recreation function activity spot if available
		Function rec = getFunction(FunctionType.RECREATION);
		if (rec != null) {
			LocalPosition loc = rec.getAvailableActivitySpot();
			if (loc != null) {
				return loc;
			}
		}
				
		Collections.shuffle(functions);

		for (Function f : functions) {
			if (f.getFunctionType() != FunctionType.EVA) {
				loc = f.getAvailableActivitySpot();
				if (loc != null)
					return loc;
			}
		}

		return null;
	}
		
	/**
	 * Determines the building functions.
	 *
	 * @return list of building .
	 * @throws Exception if error in functions.
	 */
	public Function addFunction(FunctionSpec fSpec) {
		var f =  switch (fSpec.getType()) {
			case ADMINISTRATION -> new Administration(this, fSpec);
			case ALGAE_FARMING -> new AlgaeFarming(this, fSpec);				
			case ASTRONOMICAL_OBSERVATION -> new AstronomicalObservation(this, fSpec);
			case CONNECTION -> new BuildingConnection(this, fSpec);
			case COMMUNICATION -> new Communication(this, fSpec);
			case COMPUTATION -> new Computation(this, fSpec);
			case COOKING -> new Cooking(this, fSpec);
			case DINING -> new Dining(this, fSpec);
			case EARTH_RETURN -> new EarthReturn(this, fSpec);
			case EVA -> new EVA(this, fSpec);
			case EXERCISE -> new Exercise(this, fSpec);
			case FARMING -> new Farming(this, fSpec);
			case FISHERY -> new Fishery(this, fSpec);
			case FOOD_PRODUCTION -> new FoodProduction(this, fSpec);
			case VEHICLE_MAINTENANCE -> new VehicleGarage(this, fSpec);
			case LIFE_SUPPORT -> new LifeSupport(this, fSpec);
			case LIVING_ACCOMMODATION -> new LivingAccommodation(this, fSpec);
			case MANAGEMENT -> new Management(this, fSpec);
			case MANUFACTURE -> new Manufacture(this, fSpec);
			case MEDICAL_CARE -> new MedicalCare(this, fSpec);
			case POWER_GENERATION -> new PowerGeneration(this, fSpec);
			case POWER_STORAGE -> new PowerStorage(this, fSpec);
			case PREPARING_DESSERT -> new PreparingDessert(this, fSpec);				
			case RECREATION -> new Recreation(this, fSpec);
			case RESEARCH -> new Research(this, fSpec);
			case RESOURCE_PROCESSING -> new ResourceProcessing(this, fSpec);
			case ROBOTIC_STATION -> new RoboticStation(this, fSpec);
			case STORAGE -> new Storage(this, fSpec);
			case THERMAL_GENERATION -> new ThermalGeneration(this, fSpec);
			case WASTE_PROCESSING -> new WasteProcessing(this, fSpec);
			default ->
				throw new IllegalArgumentException("Do not know how to build Function " + fSpec.getType());
			};

		functions.add(f);
		return f;
	}

	/**
	 * Checks if this building has a particular function.
	 *
	 * @param function the enum name of the function.
	 * @return true if it does.
	 */
	public boolean hasFunction(FunctionType functionType) {
		for (Function f : functions) {
			if (f.getFunctionType() == functionType) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Gets a function if the building has it.
	 *
	 * @param functionType {@link FunctionType} the function of the building.
	 * @return function.
	 * @throws BuildingException if building doesn't have the function.
	 */
	public Function getFunction(FunctionType functionType) {
		for (Function f : functions) {
			if (f.getFunctionType() == functionType) {
				return f;
			}
		}
		return null;
	}

	/**
	 * Removes the building's functions from the settlement.
	 */
	public void removeFunctionsFromSettlement() {

		Iterator<Function> i = functions.iterator();
		while (i.hasNext()) {
			i.next().removeFromSettlement();
		}
	}

	/**
	 * Removes a building function.
	 *
	 * @param function
	 */
	public void removeFunction(Function function) {
		if (functions.contains(function)) {
			functions.remove(function);
			// Need to remove the function from the building function map
			getBuildingManager().removeOneFunctionfromBFMap(this, function);
		}
	}

	/**
	 * Gets the building's building manager.
	 *
	 * @return building manager
	 */
	public BuildingManager getBuildingManager() {
		return settlement.getBuildingManager();
	}

	/**
	 * Gets the building type.
	 *
	 * @return building type as a String. TODO internationalize building names for
	 *         display in user interface.
	 */
	public String getBuildingType() {
		return buildingType;
	}

	public double getWidth() {
		return width;
	}

	public double getLength() {
		return length;
	}

	public double getFloorArea() {
		return floorArea;
	}

	/**
	 * Returns the volume of the building in liter.
	 *
	 * @return volume in liter
	 */
	public double getVolumeInLiter() {
		return floorArea * HEIGHT * 1000; // 1 Cubic Meter = 1,000 Liters
	}

	@Override
	public LocalPosition getPosition() {
		return loc;
	}

	@Override
	public double getFacing() {
		return facing;
	}

	public boolean getInTransport() {
		return inTransportMode;
	}

	public void setInTransport(boolean value) {
		inTransportMode = value;
	}

	/**
	 * Gets the base level of the building.
	 *
	 * @return -1 for in-ground, 0 for above-ground.
	 */
	public int getBaseLevel() {
		return baseLevel;
	}

	/**
	 * Gets the power requirement for full-power mode.
	 *
	 * @return power in kW.
	 */
	public double getFullPowerRequired() {
		double result = baseFullPowerRequirement;

		// Determine power required for each function.
		for (Function function : functions) {
			double power = function.getPowerRequired();
			if (power > 0) {
//			Test for System.out.println(nickName + " : "
//					+ function.getFunctionType().toString() + " : "
//					+ Math.round(power * 10.0)/10.0 + " kW")
				result += power;
			}
		}

		return result + powerNeededForEVAHeater;
	}

	/**
	 * Gets the power requirement for low-power mode.
	 *
	 * @return power in kW.
	 */
	public double getLowPowerRequired() {
		double result = baseLowPowerRequirement;

		// Determine power required for each function.
		for (Function function : functions) {
			result += function.getPoweredDownPowerRequired();
		}

		return result;
	}

	/**
	 * Gets the building's power mode.
	 */
	public PowerMode getPowerMode() {
		return powerModeCache;
	}

	/**
	 * Sets the building's power mode.
	 */
	public void setPowerMode(PowerMode powerMode) {
		this.powerModeCache = powerMode;
	}

	/**
	 * Gets the heat this building currently required.
	 *
	 * @return heat in kW.
	 */
	public double getHeatRequired() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getHeating().getHeatRequired();

		return result;
	}

	/**
	 * Gets the initial net heat gain of this building.
	 *
	 * @return heat in kW.
	 */
	public double getPreNetHeat() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getHeating().getPreNetHeat();

		return result;
	}
	
	/**
	 * Gets the post net heat gain of this building.
	 *
	 * @return heat in kW.
	 */
	public double getPostNetHeat() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getHeating().getPostNetHeat();

		return result;
	}
	
	/**
	 * Gets the heat generated for this building currently.
	 *
	 * @return heat in kW.
	 */
	public double getHeatGenerated() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getHeating().getHeatGenerated();

		return result;
	}
	
	/**
	 * Gets the excess heat dumped on this building.
	 *
	 * @return heat in kW.
	 */
	public double getExcessHeat() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getHeating().getExcessHeat();

		return result;
	}
	
	/**
	 * Gets the incoming heat due to ventilation.
	 *
	 * @return heat in kW.
	 */
	public double getVentInHeat() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getHeating().getVentInHeat();

		return result;
	}
	
	/**
	 * Gets the outgoing heat due to ventilation.
	 *
	 * @return heat in kW.
	 */
	public double getVentOutHeat() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getHeating().getVentOutHeat();

		return result;
	}
	
	
	/**
	 * Gets the heat deviation of this building.
	 *
	 * @return heat in kW.
	 */
	public double getHeatDev() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getHeatSurplus();

		return result;
	}
	
	
	/**
	 * Gets the delta temperature match of this building.
	 *
	 * @return heat in kW.
	 */
	public double getDeltaTemp() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getHeating().getDeltaTemp();

		return result;
	}

	/**
	 * Gets the deviation temperature match of this building.
	 *
	 * @return heat in kW.
	 */
	public double getDevTemp() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getHeating().getDevTemp();

		return result;
	}
	
	/**
	 * Dumps the excess heat to the building.
	 *
	 * @param heatGenerated
	 */
	public void dumpExcessHeat(double heatGenerated) {
		if (heating == null)
			heating = furnace.getHeating();
		heating.insertExcessHeatComputation(heatGenerated);
	}

	/**
	 * Gets the nuclear heat of this building currently.
	 *
	 * @return heat in kW.
	 */
	public double getNuclearPowerGen() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getNuclearPowerGen();

		return result;
	}
	
	/**
	 * Gets the solar heat of this building currently.
	 *
	 * @return heat in kW.
	 */
	public double getSolarPowerGen() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getSolarPowerGen();

		return result;
	}
	
	/**
	 * Gets the electric heat of this building currently.
	 *
	 * @return heat in kW.
	 */
	public double getElectricPowerGen() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getElectricPowerGen();

		return result;
	}
	
	/**
	 * Gets the fuel heat of this building currently.
	 *
	 * @return heat in kW.
	 */
	public double getFuelPowerGen() {
		double result = 0;

		if (furnace != null && heating != null)
			result = furnace.getFuelPowerGen();

		return result;
	}
	
	
	/**
	 * Gets the entity's malfunction manager.
	 *
	 * @return malfunction manager
	 */
	public MalfunctionManager getMalfunctionManager() {
		return malfunctionManager;
	}

	/**
	 * Gets the total amount of lighting power in this greenhouse.
	 *
	 * @return power (kW)
	 */
	public double getTotalPowerForEVA() {
		return powerNeededForEVAHeater;
	}

	/**
	 * Calculates the number of people in the airlock.
	 *
	 * @return number of people
	 */
	public int numOfPeopleInAirLock() {
		int num = 0;
		if (eva == null)
			eva = (EVA) getFunction(FunctionType.EVA);
		if (eva != null) {
			num = eva.getAirlock().getOccupants().size();
			// Note: Assuming (.5) half of people are doing EVA ingress statistically
			powerNeededForEVAHeater = num * EVA_HEATER_KW * .5D; 
		}
		return num;
	}


	/**
	 * Gets the number of people.
	 *
	 * @return
	 */
	public int getNumPeople() {

		int people = 0;

		if (lifeSupport != null) {
			people = lifeSupport.getOccupantNumber();
		}

		return people;
	}


	/**
	 * Gets a collection of inhabitants.
	 *
	 * @return
	 */
	public Collection<Person> getInhabitants() {
		if (lifeSupport != null) {
			return lifeSupport.getOccupants();
		}
		
		return Collections.emptySet();
	}

	/**
	 * Gets a collection of robots.
	 *
	 * @return
	 */
	public Collection<Robot> getRobots() {
		if (roboticStation != null) {
			return roboticStation.getRobotOccupants();
		}
		
		return Collections.emptySet();
	}

	/**
	 * Gets a collection of people affected by this entity. Children buildings
	 * should add additional people as necessary.
	 *
	 * @return person collection
	 */
	public Collection<Person> getAffectedPeople() {

		Collection<Person> people = new UnitSet<>();
		// Check all people in settlement.
		Iterator<Person> i = settlement.getIndoorPeople().iterator();
		while (i.hasNext()) {
			Person person = i.next();

			if (person.getBuildingLocation() == this) {
				people.add(person);
			}

			Task task = person.getMind().getTaskManager().getTask();

			// Add all people maintaining this building.
			if (task instanceof MaintainBuilding maintainBuilding) {
				if (maintainBuilding.getEntity() == this) {
					if (!people.contains(person))
						people.add(person);
				}
			}

			// Add all people repairing this facility.
			if (task instanceof Repair repair) {
				if (repair.getEntity() == this) {
					if (!people.contains(person))
						people.add(person);
				}
			}
		}

		return people;
	}

	/**
	 * Time passing for building.
	 *
	 * @param time amount of time passing (in millisols)
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		if (!isValid(pulse)) {
			return false;
		}
		
		// DEBUG: Calculate the real time elapsed [in milliseconds]
//		long tnow = System.currentTimeMillis();

		// Send time to each building function.
		for (Function f : functions)
			f.timePassing(pulse);

		// If powered up, active time passing.
		if (powerModeCache == PowerMode.FULL_POWER)
			malfunctionManager.activeTimePassing(pulse);

		// Update malfunction manager.
		malfunctionManager.timePassing(pulse);

		if (pulse.isNewSol()) {
			// Determine if a meteorite impact will occur within the new sol
			checkForMeteoriteImpact(pulse);
		}

		inTransportMode = false;
		
		// DEBUG: Calculate the real time elapsed [in milliseconds]
//		tLast = System.currentTimeMillis();
//		long elapsedMS = tLast - tnow;
//		if (elapsedMS > 10)
//			logger.severe(this, "elapsedMS: " + elapsedMS);
			
		return true;
	}

	public List<Function> getFunctions() {
		return functions;
	}

	/*
	 * Checks for possible meteorite impact for this building.
	 */
	private void checkForMeteoriteImpact(ClockPulse pulse) {
		// Reset the impact time
		int momentOfImpact = 0;
		var meteorite = getBuildingManager().getMeteorite();

		// if assuming a gauissan profile, p = mean + RandomUtil.getGaussianDouble() * standardDeviation
		// Note: Will have 70% of values will fall between mean +/- standardDeviation, i.e., within one std deviation
		double probability = floorArea * meteorite.getProbabilityOfImpactPerSQMPerSol();
		// Probability is in percentage unit between 0% and 100%
		if (probability > 0 && RandomUtil.getRandomDouble(100D) <= probability) {
			isImpactImminent = true;
			// Set a time for the impact to happen any time between 0 and 1000 milisols
			momentOfImpact = RandomUtil.getRandomInt(999);
		}

		if (!isImpactImminent) {
			// The impact is not or no longer imminent.
			// Note: if the impact has already once. isImpactImminent will be set back to false.
			return;
		}
		
		int now = pulse.getMarsTime().getMillisolInt();
		// Note: at the fastest sim speed, up to ~5 millisols may be skipped.
		// need to set up detection of the impactTimeInMillisol with a +/- 3 range.
		int delta = (int) Math.sqrt(Math.sqrt(pulse.getMasterClock().getActualTR()));
		
		if (pulse.isNewIntMillisol()
				&& now > momentOfImpact - 2 * delta && now < momentOfImpact + 2 * delta) {
			// Yes the impact event occurs in the vicinity
			
			logger.log(this, Level.INFO, 20_000, "A meteorite impact event was imminent.");

			// Reset the boolean immediately for keeping track of whether 
			// the impact has occurred
			isImpactImminent = false;
			
			// Find the length this meteorite can penetrate
			double penetratedLength = meteorite.getWallPenetration();

			double wallThick = getWallThickness();
			
			double reductionFraction = penetratedLength / wallThick;
			if (reductionFraction > 1)
				reductionFraction = 1;
			
			// The impact reduces the structural health of the building 
			// Future: it should also generates a repair task to at least assess the damage
			malfunctionManager.reduceWearLifeTime(reductionFraction);
			
			if (penetratedLength < wallThick) {
				// Case A: No. it's not breached
				logger.warning(this, 20_000, "Meteorite Impact event observed. Building wall not breached but damaged. "
						+ "Penetration fraction: " + Math.round(reductionFraction * 10.0)/10.0 + ".");
				
				return ;
			}
	
			logger.warning(this, 20_000, "Meteorite Impact event observed. Building wall penetrated.");
			
			// Case B: Yes it's breached !	
			
			// Simulate the meteorite impact as a malfunction event for now
			Malfunction mal = malfunctionManager.triggerMalfunction(MalfunctionFactory
					.getMalfunctionByName(MalfunctionFactory.METEORITE_IMPACT_DAMAGE),
					true, this);

			logger.log(this, Level.INFO, 20_000, mal.getName() + " registered.");
			
			String victimNames = null;

			// check if someone under this roof may have seen/affected by the impact
			for (Person person : getInhabitants()) {
				
				if (person.getBuildingLocation() == this
					&& RandomUtil.lessThanRandPercent(METEORITE_IMPACT_PROBABILITY_AFFECTED)) {

					// Note 1: someone got hurt, declare medical emergency
					// Note 2: delineate the accidents from those listed in malfunction.xml
					// currently, malfunction whether a person gets hurt is handled by Malfunction
					// above
					int resilience = person.getNaturalAttributeManager()
							.getAttribute(NaturalAttributeType.STRESS_RESILIENCE);
					int courage = person.getNaturalAttributeManager()
							.getAttribute(NaturalAttributeType.COURAGE);
					double factor = 1 + RandomUtil.getRandomDouble(1) - resilience / 100D - courage / 100D;
					PhysicalCondition pc = person.getPhysicalCondition();
					if (factor > 1)
						pc.setStress(pc.getStress() * factor);

					if (victimNames != null)
						victimNames += ", " + person.getName();
					else
						victimNames = person.getName();
					
					mal.setTraumatized(victimNames);

					// Store the meteorite fragment in the settlement
					settlement.storeAmountResource(ResourceUtil.meteoriteID, floorArea * meteorite.getDebrisMass());

					logger.info(this, 20_000, "Found " + Math.round(meteorite.getDebrisMass() * 100.0)/100.0
							+ " kg of meteorite fragments");

					if (pc.getStress() > 50)
						logger.warning(this, 20_000, victimNames + " was traumatized by the meteorite impact");

				} // check if this person happens to be inside the affected building
				
			} // loop for persons


			if (victimNames == null)
				victimNames = "";
				
			HistoricalEvent hEvent = new HazardEvent(
					EventType.HAZARD_ACTS_OF_GOD,
					this,
					mal.getMalfunctionMeta().getName(),
					"",
					victimNames,
					this);

			if (eventManager == null)
				eventManager = Simulation.instance().getEventManager();
			
			eventManager.registerNewEvent(hEvent);

			fireUnitUpdate(UnitEventType.METEORITE_EVENT);
		}
		
		else {
			// No the impact does not occur in the vicinity
			logger.log(this, Level.INFO, 30_000, "Meteorite Impact event observed but occurred in settlement vicinity.");
		}
	}

	/**
	 * Get the wall thickness based on the constructionType type.
	 */
	private double getWallThickness() {
		switch(constructionType) {
			case PRE_FABRICATED:
				return 0.0000254;
			case INFLATABLE:
				return 0.0000018;
			case SEMI_ENGINEERED:
				return 0.0000100;
			default:
				return 0;
		}
	}

	public ConstructionType getConstruction() {
		return constructionType;
	}

	/**
	 * Gets the building's settlement template ID number.
	 *
	 * @return id.
	 */
	public String getTemplateID() {
		return templateID;
	}

	/**
	 * Gets the zone # where this building is at. It's zero by default. 
	 * Astronomy Observatory is located at zone 1.
	 *
	 * @return zone.
	 */
	public int getZone() {
		return zone;
	}

	/**
	 * Sets the building's settlement template ID number.
	 *
	 * @param id.
	 */
	public void setTemplateID(String id) {
		templateID = id;
	}

	/**
	 * Gets the settlement it is currently associated with.
	 *
	 * @return settlement or null if none.
	 */
	@Override
	public Settlement getSettlement() {
		return settlement;
	}
	
	/**
	 * Adds incoming heat arriving at this building due to ventilation.
	 * Note: heat gain if positive; heat loss if negative.
	 * 
	 * @param heat removed or added
	 */
	public void addVentInHeat(double heat) {
		// Set the instance of thermal generation function.
		if (furnace == null)
			furnace = (ThermalGeneration) getFunction(FunctionType.THERMAL_GENERATION);
		if (heating == null)
			heating = furnace.getHeating();

		heating.addVentInHeat(heat);
	}

	public double getCurrentAirPressure() {
		double p = 0D;

		if (hasFunction(FunctionType.LIFE_SUPPORT)) {
			p = getLifeSupport().getAir().getTotalPressure();
		}
		
		// convert from atm to kPascal
		return p * AirComposition.KPA_PER_ATM;
	}

	@Override
	public Building getBuildingLocation() {
		return this;
	}

	/**
	 * Gets the settlement the person is currently associated with.
	 *
	 * @return associated settlement or null if none.
	 */
	@Override
	public Settlement getAssociatedSettlement() {
		return getSettlement();
	}

	// TODO this is wrong as names can change. This is just used to identify if there are multiple floors.
	public boolean isAHabOrHub() {
        return buildingType.contains(" Hab")
                || buildingType.contains(" Hub");
    }

	/**
	 * Checks if the building has a lab with a particular science type
	 *
	 * @param type
	 * @return
	 */
	public boolean hasSpecialty(ScienceType type) {
		if (getResearch() == null)
			return false;

		return lab.hasSpecialty(type);
	}

	@Override
	public UnitType getUnitType() {
		return UnitType.BUILDING;
	}

	/**
	 * Is this unit inside a settlement
	 *
	 * @return true if the unit is inside a settlement
	 */
	@Override
	public boolean isInSettlement() {
		return true;
	}

	/**
	 * Gets the amount resource stored
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public double getAmountResourceStored(int resource) {
		return getSettlement().getAmountResourceStored(resource);
	}

	/**
	 * Gets all the amount resource resource stored, including inside equipment.
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public double getAllAmountResourceStored(int resource) {
		return getSettlement().getAllAmountResourceStored(resource);
	}
	
	/**
	 * Stores the amount resource
	 *
	 * @param resource the amount resource
	 * @param quantity
	 * @return excess quantity that cannot be stored
	 */
	@Override
	public double storeAmountResource(int resource, double quantity) {
		return getSettlement().storeAmountResource(resource, quantity);
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
		return getSettlement().retrieveAmountResource(resource, quantity);
	}

	/**
	 * Gets the capacity of a particular amount resource.
	 *
	 * @param resource
	 * @return capacity
	 */
	@Override
	public double getAmountResourceCapacity(int resource) {
		return getSettlement().getAmountResourceCapacity(resource);
	}

	/**
	 * Obtains the remaining storage space of a particular amount resource.
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public double getAmountResourceRemainingCapacity(int resource) {
		return getSettlement().getAmountResourceRemainingCapacity(resource);
	}

	/**
	 * Does it have unused space or capacity for a particular resource ?
	 * 
	 * @param resource
	 * @return
	 */
	@Override
	public boolean hasAmountResourceRemainingCapacity(int resource) {
		return getSettlement().hasAmountResourceRemainingCapacity(resource);
	}
	
	/**
	 * Gets all stored amount resources.
	 *
	 * @return all stored amount resources.
	 */
	@Override
	public Set<Integer> getAmountResourceIDs() {
		return getSettlement().getAmountResourceIDs();
	}
	
	/**
	 * Gets all stored amount resources in eqmInventory, including inside equipment.
	 *
	 * @return all stored amount resources.
	 */
	@Override
	public Set<Integer> getAllAmountResourceIDs() {
		return getSettlement().getAllAmountResourceIDs();
	}
	
	/**
	 * Sets the unit's container unit.
	 *
	 * @param newContainer the unit to contain this unit.
	 */
	public boolean setContainerUnit(Unit newContainer) {
		if (newContainer != null) {
			if (newContainer.equals(getContainerUnit())) {
				return false;
			}
			// 1. Set Coordinates
			setCoordinates(newContainer.getCoordinates());
			// 2. Set LocationStateType
			currentStateType = LocationStateType.INSIDE_SETTLEMENT;
			// 3. Set containerID
			// Q: what to set for a deceased person ?
			setContainerID(newContainer.getIdentifier());
			// 4. Fire the container unit event
			fireUnitUpdate(UnitEventType.CONTAINER_UNIT_EVENT, newContainer);
		}

		return true;
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

	@Override
	public int storeItemResource(int resource, int quantity) {
		return getSettlement().storeItemResource(resource, quantity);
	}

	@Override
	public int retrieveItemResource(int resource, int quantity) {
		return getSettlement().retrieveItemResource(resource, quantity);
	}


	@Override
	public int getItemResourceStored(int resource) {
		return getSettlement().getItemResourceStored(resource);
	}

	/**
	 * Gets the remaining quantity of an item resource.
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public int getItemResourceRemainingQuantity(int resource) {
		return getSettlement().getItemResourceRemainingQuantity(resource);
	}

	@Override
	public Set<Integer> getItemResourceIDs() {
		return getSettlement().getItemResourceIDs();
	}
	
	@Override
	public double getCargoCapacity() {
		return 0;
	}
	
	/**
	 * Checks if this building is isInhabitable.
	 * 
	 * @return
	 */
	public boolean isInhabitable() {
		return getLifeSupport() == null;
	}
	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

	public void reinit() {
		settlement = unitManager.getSettlementByID(settlementID);
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
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		functions = null;
		furnace = null;
		lifeSupport = null;
		roboticStation = null;
		powerGen = null;
		buildingType = null;
		powerModeCache = null;
		malfunctionManager = null;
	}
}
