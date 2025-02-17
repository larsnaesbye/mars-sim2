/*
 * Mars Simulation Project
 * BuildingConfig.java
 * @date 2023-05-31
 * @author Scott Davis
 */
package com.mars_sim.core.structure.building;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;

import com.mars_sim.core.configuration.ConfigHelper;
import com.mars_sim.core.map.location.LocalPosition;
import com.mars_sim.core.resourceprocess.ResourceProcessConfig;
import com.mars_sim.core.resourceprocess.ResourceProcessEngine;
import com.mars_sim.core.science.ScienceType;
import com.mars_sim.core.structure.building.function.FunctionType;

/**
 * Provides configuration information about settlement buildings. Uses a DOM
 * document to get the information.
 */
public class BuildingConfig {

	// Element and attribute names
	private static final String CATEGORY = "category";
	private static final String DESCRIPTION = "description";
	private static final String BUILDING = "building";
	private static final String NAME = "name";
	private static final String BUILDING_TYPE = "type";
	private static final String WIDTH = "width";
	private static final String LENGTH = "length";
	private static final String N_S_ALIGNMENT = "north-south-alignment";
	private static final String CONSTRUCTION = "construction";
	private static final String BASE_LEVEL = "base-level";
	private static final String BASE_MASS = "base-mass";

	private static final String WEAR_LIFETIME = "wear-lifetime";
	private static final String MAINTENANCE_TIME = "maintenance-time";
	private static final String ROOM_TEMPERATURE = "room-temperature";

	private static final String FUNCTIONS = "functions";
	private static final String CAPACITY = "capacity";

	private static final String RESEARCH = "research";
	private static final String RESEARCH_SPECIALTY = "research-specialty";

	private static final String RESOURCE_PROCESSING = "resource-processing";

	private static final String NUMBER_MODULES = "number-modules";

	// The common power required XML attribute
	public static final String POWER_REQUIRED = "power-required";

	private static final String BASE_POWER = "base-power";
	private static final String BASE_POWER_DOWN_POWER = "base-power-down-power";
	
	private static final String PROCESS_ENGINE = "process-engine";
	private static final String STORAGE = "storage";
	private static final String RESOURCE_CAPACITY = "resource-capacity";
	private static final String RESOURCE_INITIAL = "resource-initial";
	private static final String TYPE = "type";
	private static final String MODULES = "modules";
	private static final String CONVERSION = "thermal-conversion";
	private static final String PERCENT_LOADING = "percent-loading";

	private static final String MEDICAL_CARE = "medical-care";
	private static final String BEDS = "bed";
	private static final String VEHICLE_MAINTENANCE = "vehicle-maintenance";
	private static final String PARKING_LOCATION = "parking-location";
	private static final String FLYER_LOCATION = "flyer-location";
	
	private static final String WASTE_PROCESSING = "waste-processing";

	private static final String ACTIVITY = "activity";
	private static final String ACTIVITY_SPOT = "activity-spot";
	private static final String BED_LOCATION = "bed-location";

	private static final String HEAT_SOURCE = "heat-source";
	private static final String THERMAL_GENERATION = "thermal-generation";

	// Power source types
	private static final String POWER_GENERATION = "power-generation";
	private static final String POWER_SOURCE = "power-source";
	private static final String POWER = "power";
	private static final Set<String> DEFAULT_SOURCE_ATTR = Set.of(TYPE, MODULES, CONVERSION, PERCENT_LOADING);

	private static final String POSITION = "-position";

	private Map<String, BuildingSpec> buildSpecMap = new HashMap<>();

	private Set<FunctionType> activityFunctions  = new HashSet<>();
	
	/**
	 * Constructor.
	 *
	 * @param buildingDoc DOM document with building configuration
	 * @param resProcConfig 
	 */
	public BuildingConfig(Document buildingDoc, ResourceProcessConfig resProcConfig) {

		List<Element> buildingNodes = buildingDoc.getRootElement().getChildren(BUILDING);
		for (Element buildingElement : buildingNodes) {
			String buildingType = buildingElement.getAttributeValue(BUILDING_TYPE);
			String key = generateSpecKey(buildingType);
			buildSpecMap.put(key, parseBuilding(buildingType, buildingElement, resProcConfig));
		}
	}

	/**
	 * Gets a set of all building types.
	 *
	 * @return set of building types.
	 */
	public Collection<BuildingSpec> getBuildingTypes() {
		return buildSpecMap.values();
	}

	/**
	 * Parses a building spec node.
	 *
	 * @param buildingTypeName
	 * @param buildingElement
	 * @param resProcConfig 
	 * @return
	 */
	private BuildingSpec parseBuilding(String buildingTypeName, Element buildingElement, ResourceProcessConfig resProcConfig) {
		Element descElement = buildingElement.getChild(DESCRIPTION);
		String desc = descElement.getValue().trim();
		desc = desc.replaceAll("\\t+", "").replaceAll("\\s+", " ").replace("   ", " ").replace("  ", " ");

		double width = Double.parseDouble(buildingElement.getAttributeValue(WIDTH));
		double length = Double.parseDouble(buildingElement.getAttributeValue(LENGTH));
		String alignment = buildingElement.getAttributeValue(N_S_ALIGNMENT);
		int baseLevel = Integer.parseInt(buildingElement.getAttributeValue(BASE_LEVEL));
		double presetTemp = Double.parseDouble(buildingElement.getAttributeValue(ROOM_TEMPERATURE));
		int maintenanceTime = Integer.parseInt(buildingElement.getAttributeValue(MAINTENANCE_TIME));
		int wearLifeTime = Integer.parseInt(buildingElement.getAttributeValue(WEAR_LIFETIME));

		Element powerElement = buildingElement.getChild(POWER_REQUIRED);
		double basePowerRequirement = Double.parseDouble(powerElement.getAttributeValue(BASE_POWER));
		double basePowerDownPowerRequirement = Double.parseDouble(powerElement.getAttributeValue(BASE_POWER_DOWN_POWER));

		// Get functions
		Map<FunctionType, FunctionSpec> supportedFunctions = new EnumMap<>(FunctionType.class);
		Element funcElement = buildingElement.getChild(FUNCTIONS);
		for (Element element : funcElement.getChildren()) {
			String name = element.getName();
			FunctionType function = FunctionType.valueOf(ConfigHelper.convertToEnumName(name));

			// Get activity spots
			String spotName = getDefaultSpotName(function);
			Set<NamedPosition> spots = parseNamedPositions(element, ACTIVITY,
													spotName, width, length);

			// Record that this Function has activity spots
			if (!spots.isEmpty()) {
				activityFunctions.add(function);
			}

			// Get attributes as basic properties
			Map<String, Object> props = new HashMap<>();
			for (Attribute attr : element.getAttributes()) {
				props.put(attr.getName(), attr.getValue());
			}

			// Any complex properties
			for (Element complexProperty : element.getChildren()) {
				if (complexProperty.getName().endsWith(POSITION)) {
					LocalPosition pos = ConfigHelper.parseLocalPosition(complexProperty);
					props.put(complexProperty.getName(), pos);
				}
			}
			
			FunctionSpec fspec = new FunctionSpec(function, props, spots);

			supportedFunctions.put(function, fspec);
		}

		String categoryString = buildingElement.getAttributeValue(CATEGORY);
		BuildingCategory category = null;
		if (categoryString != null) {
			category = BuildingCategory.valueOf(ConfigHelper.convertToEnumName(categoryString));
		}
		else {
			// Derive category from Functions
			category = deriveCategory(supportedFunctions.keySet());
		}

		BuildingSpec newSpec = new BuildingSpec(buildingTypeName, desc, category, 
				width, length, alignment, baseLevel,
			 	presetTemp, maintenanceTime, wearLifeTime,
			 	basePowerRequirement, basePowerDownPowerRequirement,
			 	supportedFunctions);
		
		String construction = buildingElement.getAttributeValue(CONSTRUCTION);
		if (construction != null) {
			newSpec.setConstruction(ConstructionType.valueOf(ConfigHelper.convertToEnumName(construction)));
		}

		String baseMass = buildingElement.getAttributeValue(BASE_MASS);
		if (baseMass != null) {
			newSpec.setBaseMass(Double.parseDouble(baseMass));
		}

		// Get Storage
		Element functionsElement = buildingElement.getChild(FUNCTIONS);
		Element storageElement = functionsElement.getChild(STORAGE);
		if (storageElement != null) {
			parseStorage(newSpec, storageElement);
		}

		Element thermalGenerationElement = functionsElement.getChild(THERMAL_GENERATION);
		if (thermalGenerationElement != null) {
			List<SourceSpec> heatSourceList = parseSources(thermalGenerationElement.getChildren(HEAT_SOURCE),
														   CAPACITY);
			newSpec.setHeatSource(heatSourceList);
		}

		Element powerGenerationElement = functionsElement.getChild(POWER_GENERATION);
		if (powerGenerationElement != null) {
			List<SourceSpec> powerSourceList = parseSources(powerGenerationElement.getChildren(POWER_SOURCE),
															POWER);
			newSpec.setPowerSource(powerSourceList);
		}

		Element researchElement = functionsElement.getChild(RESEARCH);
		if (researchElement != null) {
			parseResearch(newSpec, researchElement);
		}

		Element resourceProcessingElement = functionsElement.getChild(RESOURCE_PROCESSING);
		if (resourceProcessingElement != null) {
			newSpec.setResourceProcess(parseResourceProcessing(resProcConfig, resourceProcessingElement));
		}

		Element wasteProcessingElement = functionsElement.getChild(WASTE_PROCESSING);
		if (wasteProcessingElement != null) {
			newSpec.setWasteProcess(parseResourceProcessing(resProcConfig, wasteProcessingElement));
		}

		Element vehicleElement = functionsElement.getChild(VEHICLE_MAINTENANCE);
		if (vehicleElement != null) {
			Set<LocalPosition> parking = parsePositions(vehicleElement, "parking", PARKING_LOCATION,
												   width, length);
			newSpec.setParking(parking);
		}

		if (vehicleElement != null) {
			Set<LocalPosition> flyerParking = parsePositions(vehicleElement, "flyerParking", FLYER_LOCATION,
												   width, length);
			newSpec.setFlyerParking(flyerParking);
		}
		
		Element medicalElement = functionsElement.getChild(MEDICAL_CARE);
		if (medicalElement != null) {
			Set<LocalPosition> beds = parsePositions(medicalElement, BEDS, BED_LOCATION,
												width, length);
			newSpec.setBeds(beds);
		}
		
		return newSpec;
	}
	
	/**
	 * Gets the spot name that is best associated with a certain FunctionType.
	 * 
	 * @param function
	 * @return
	 */
	private static String getDefaultSpotName(FunctionType function) {
		return switch(function) {
			case ADMINISTRATION -> "Desk";
			case DINING -> "Seat";
			case LIVING_ACCOMMODATION -> "Bed";
			case MANUFACTURE -> "Workbench";
			case MEDICAL_CARE -> "Sickbay";
			case RECREATION -> "Chair";
			case RESEARCH -> "Bench";
			case ROBOTIC_STATION -> "Charge Pt";
			default -> "S";
		};
	}

	/**
	 * Derives the category from the types of Functions.
	 * 
	 * @param functions
	 * @return
	 */
	private static BuildingCategory deriveCategory(Set<FunctionType> functions) {

		// Get a set of categories
		Set<BuildingCategory> cats = functions.stream()
						.map(f -> f.getCategory())
						.filter(c -> c != null)
						.collect(Collectors.toSet());

		BuildingCategory category = BuildingCategory.CONNECTION;
		if (!cats.isEmpty()) {
			// Find the category with the lowest ordinal as that is the best to represent
			// this set of Functions
			int lowestOrdinal = 999;
			for (BuildingCategory c : cats) {
				if (c.ordinal() < lowestOrdinal) {
					lowestOrdinal = c.ordinal();
					category = c;
				}
			}
		}
		return category;
	}

	/**
	 * Parses the specific Resource processing process-engine nodes and create a list of ResourceProcessingEngine
	 * 
	 * @param resourceProcessingElement
	 * @return 
	 */
	private List<ResourceProcessEngine> parseResourceProcessing(ResourceProcessConfig resProcConfig,
										 Element resourceProcessingElement) {
		List<ResourceProcessEngine> resourceProcesses = new ArrayList<>();

		List<Element> resourceProcessNodes = resourceProcessingElement.getChildren(PROCESS_ENGINE);

		for (Element processElement : resourceProcessNodes) {
			String name = processElement.getAttributeValue(NAME);
			int modules = ConfigHelper.getOptionalAttributeInt(processElement, NUMBER_MODULES, 1);
			resourceProcesses.add(new ResourceProcessEngine(resProcConfig.getProcessSpec(name), modules));
		}

		return resourceProcesses;
	}
	
	/**
	 * Parses a specific research details.
	 * 
	 * @param newSpec
	 * @param researchElement
	 */
	private void parseResearch(BuildingSpec newSpec, Element researchElement) {
		List<ScienceType> result = new ArrayList<>();
		List<Element> researchSpecialities = researchElement.getChildren(RESEARCH_SPECIALTY);
		for (Element researchSpecialityElement : researchSpecialities) {
			String value = researchSpecialityElement.getAttributeValue(NAME);
			result.add(ScienceType.valueOf(ConfigHelper.convertToEnumName(value)));
		}
		newSpec.setScienceType(result);
	}

	/**
	 * Parses a sources element.
	 * 
	 * @param list
	 * @param unitName
	 * @return
	 */
	private List<SourceSpec> parseSources(List<Element> list, String unitName) {
		List<SourceSpec> sourceList = new ArrayList<>();
		for (Element sourceElement : list) {
			Properties attrs = new Properties();
			String type = sourceElement.getAttributeValue(TYPE);
			double unitCapacity = ConfigHelper.getOptionalAttributeDouble(sourceElement, unitName, 0);
			int numModules = ConfigHelper.getOptionalAttributeInt(sourceElement, MODULES, 1);
			double stirlingConversion =  ConfigHelper.getOptionalAttributeDouble(sourceElement, CONVERSION, 100D);
			double percentLoadCapacity = ConfigHelper.getOptionalAttributeDouble(sourceElement, PERCENT_LOADING, 100D);
			
			// Add optional attributes.
			for(Attribute attr : sourceElement.getAttributes()) {
				String attrName = attr.getName();
				if (!DEFAULT_SOURCE_ATTR.contains(attrName) && !attrName.equals(unitName)) {
					attrs.put(attr.getName(), attr.getValue());
				}
			}
			sourceList.add(new SourceSpec(type, attrs, numModules, 
					unitCapacity, stirlingConversion, percentLoadCapacity));
		}
		return sourceList;
	}

	/**
	 * Parses the specific Storage properties.
	 * 
	 * @param newSpec
	 * @param storageElement
	 */
	private void parseStorage(BuildingSpec newSpec, Element storageElement) {
		List<Element> resourceStorageNodes = storageElement.getChildren(RESOURCE_CAPACITY);
		var storageMap = ConfigHelper.parseResourceListById("Storage capacity in building " + newSpec.getName(),
										resourceStorageNodes);
		
		List<Element> resourceInitialNodes = storageElement.getChildren(RESOURCE_INITIAL);
		var initialMap = ConfigHelper.parseResourceListById("Initial storage in building " + newSpec.getName(),
										resourceInitialNodes);

		newSpec.setStorage(storageMap, initialMap);
	}

	/**
	 * Parses an set of named position for a building's function. These have a xloc & yloc structure.
	 *
	 * @param functionElement Element holding locations
	 * @param locations Name of the location elements
	 * @param namePrefix The default name prefix to assign to the spots
	 * @param buildingWidth
	 * @param buildingLength
	 * @return set of activity spots as Point2D objects.
	 */
	private Set<NamedPosition> parseNamedPositions(Element functionElement, String locations,
												   String namePrefix,
										 		   double buildingWidth, double buildingLength) {
		Set<NamedPosition> result = new HashSet<>();

		// Maximum coord is half the width or length
		double maxX = buildingWidth/2D;
		double maxY = buildingLength/2D;
		boolean hasMax = (maxX > 0 && maxY > 0);

		Element activityElement = functionElement.getChild(locations);
		if (activityElement != null) {
			int i = 1;
			for(Element activitySpot : activityElement.getChildren(ACTIVITY_SPOT)) {
				LocalPosition point = ConfigHelper.parseLocalPosition(activitySpot);

				// Check location is within the building. Check as long as the maximum
				// is defined
				if (hasMax && !point.isWithin(maxX, maxY)) {
					// Roughly walk back over the XPath
					StringBuilder name = new StringBuilder();
					do {
						name.append(functionElement.getName()).append(' ');
						functionElement = functionElement.getParentElement();
					} while (!functionElement.getName().equals(BUILDING));
					name.append("in building '").append(functionElement.getAttributeValue(TYPE)).append("'");

					throw new IllegalArgumentException("Locations '" + locations
							+ "' of " + name.toString()
							+ " are outside building");
				}

				// Identify name
				String name = activitySpot.getAttributeValue(NAME);
				if (name == null) {
					name = namePrefix + " #" + i;
					i++;
				}
				result.add(new NamedPosition(name, point));
			}
		}
		return result;
	}
	/**
	 * Parses an set of position for a building's function. These have a xloc & yloc structure.
	 *
	 * @param functionElement Element holding locations
	 * @param locations Name of the location elements
	 * @param pointName Name of the point item
	 * @return set of activity spots as Point2D objects.
	 */
	private Set<LocalPosition> parsePositions(Element functionElement, String locations, String pointName,
										 double buildingWidth, double buildingLength) {
		Set<LocalPosition> result = new HashSet<>();

		// Maximum coord is half the width or length
		double maxX = buildingWidth/2D;
		double maxY = buildingLength/2D;
		boolean hasMax = (maxX > 0 && maxY > 0);
		
		Element activityElement = functionElement.getChild(locations);
		if (activityElement != null) {
			for(Element activitySpot : activityElement.getChildren(pointName)) {
				LocalPosition point = ConfigHelper.parseLocalPosition(activitySpot);

				// Check location is within the building. Check as long as the maximum
				// is defined
				if (hasMax && !point.isWithin(maxX, maxY)) {
					// Roughly walk back over the XPath
					StringBuilder name = new StringBuilder();
					do {
						name.append(functionElement.getName()).append(' ');
						functionElement = functionElement.getParentElement();
					} while (!functionElement.getName().equals(BUILDING));
					name.append("in building '").append(functionElement.getAttributeValue(TYPE)).append("'");

					throw new IllegalArgumentException("Locations '" + locations
							+ "' of " + name.toString()
							+ " are outside building");
				}

				result.add(point);
			}
		}
		return result;
	}

	/**
	 * Finds a building spec according to the name.
	 * 
	 * @param buildingType
	 * @return
	 */
	public BuildingSpec getBuildingSpec(String buildingType) {
		BuildingSpec result = buildSpecMap.get(generateSpecKey(buildingType));
		if (result == null) {
			throw new IllegalArgumentException("Building Type not known: " + buildingType);
		}
		return result;
	}

	/**
	 * Checks if building has a certain function capability.
	 *
	 * @param buildingType the type of the building.
	 * @param function Type of service.
	 * @return true if function supported.
	 * @throws Exception if building type cannot be found.
	 */
	public boolean hasFunction(String buildingType, FunctionType function) {
		return getBuildingSpec(buildingType).getFunctionSupported().contains(function);
	}

	/**
	 * Gets a list of research specialties for the building's lab.
	 *
	 * @param buildingType the type of the building
	 * @return list of research specialties as {@link ScienceType}.
	 * @throws Exception if building type cannot be found or XML parsing error.
	 */
	public List<ScienceType> getResearchSpecialties(String buildingType) {
		return getBuildingSpec(buildingType).getScienceType();

	}

	/**
	 * Gets the building's resource processes.
	 *
	 * @param buildingType the type of the building.
	 * @return a list of resource processes.
	 * @throws Exception if building type cannot be found or XML parsing error.
	 */
	public List<ResourceProcessEngine> getResourceProcesses(String buildingType) {
		return getBuildingSpec(buildingType).getResourceProcess();
	}

	/**
	 * Gets the building's waste processes.
	 *
	 * @param buildingType the type of the building.
	 * @return a list of waste processes.
	 * @throws Exception if building type cannot be found or XML parsing error.
	 */
	public List<ResourceProcessEngine> getWasteProcesses(String buildingType) {
		return getBuildingSpec(buildingType).getWasteProcess();
	}

	/**
	 * Gets a list of the building's resource capacities.
	 *
	 * @param buildingType the type of the building.
	 * @return list of storage capacities
	 * @thrList<ResourceProcessEngine>ing type cannot be found or XML parsing error.
	 */
	public Map<Integer, Double> getStorageCapacities(String buildingType) {
		return getBuildingSpec(buildingType).getStorage();
	}

	/**
	 * Gets a map of the initial resources stored in this building.
	 *
	 * @param buildingType the type of the building
	 * @return map of initial resources
	 * @throws Exception if building type cannot be found or XML parsing error.
	 */
	public Map<Integer, Double> getInitialResources(String buildingType) {
		return getBuildingSpec(buildingType).getInitialResources();
	}

	/**
	 * Gets a list of the building's heat sources.
	 *
	 * @param buildingType the type of the building.
	 * @return list of heat sources
	 * @throws Exception if building type cannot be found or XML parsing error.
	 */
	public List<SourceSpec> getHeatSources(String buildingType) {
		return getBuildingSpec(buildingType).getHeatSource();
	}

	/**
	 * Gets a list of the building's power sources.
	 *
	 * @param buildingType the type of the building.
	 * @return list of power sources
	 * @throws Exception if building type cannot be found or XML parsing error.
	 */
	public List<SourceSpec> getPowerSources(String buildingType) {
		return getBuildingSpec(buildingType).getPowerSource();

	}

	/**
	 * Gets the relative location in the building of a parking location.
	 *
	 * @param buildingType the type of the building.
	 * @return Positions containing the relative X & Y position from the building
	 *         center.
	 */
	public Set<LocalPosition> getParkingLocations(String buildingType) {
		return getBuildingSpec(buildingType).getParking();
	}

	/**
	 * Gets the relative location in the building of a drone location.
	 *
	 * @param buildingType the type of the building.
	 * @return Positions containing the relative X & Y position from the building
	 *         center.
	 */
	public Set<LocalPosition> getDroneLocations(String buildingType) {
		return getBuildingSpec(buildingType).getFlyerParking();
	}

	
	private static final String generateSpecKey(String buildingType) {
		return buildingType.toLowerCase().replace(" ", "-");
	}

	/**
	 * Get the Function spec from a Building Type.
	 * 
	 * @param type Building type
	 * @param functionType Type of function
	 * @return
	 */
	public FunctionSpec getFunctionSpec(String type, FunctionType functionType) {
		return getBuildingSpec(type).getFunctionSpec(functionType);
	}

	/**
	 * Get the FunctionTypes that have ActivitySpots assigned
	 * @return
	 */
    public Set<FunctionType> getActivitySpotFunctions() {
        return activityFunctions;
    }
}
