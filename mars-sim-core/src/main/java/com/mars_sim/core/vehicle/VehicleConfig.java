/*
 * Mars Simulation Project
 * VehicleConfig.java
 * @date 2023-06-05
 * @author Barry Evans
 */
package com.mars_sim.core.vehicle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.jdom2.Document;
import org.jdom2.Element;

import com.mars_sim.core.configuration.ConfigHelper;
import com.mars_sim.core.manufacture.ManufactureConfig;
import com.mars_sim.core.resource.AmountResource;
import com.mars_sim.core.resource.ItemResourceUtil;
import com.mars_sim.core.resource.Part;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.science.ScienceType;
import com.mars_sim.mapdata.location.LocalPosition;

/**
 * Provides configuration information about vehicle units. Uses a DOM document
 * to get the information.
 */
public class VehicleConfig {


	private static final Logger logger = Logger.getLogger(VehicleConfig.class.getName());
	
	// Element names
	private static final String VEHICLE = "vehicle";
	private static final String NAME = "name";
	private static final String MODEL = "model";
	private static final String TYPE = "type";
	private static final String FUEL = "fuel";
	private static final String BASE_IMAGE = "base-image";
	private static final String WIDTH = "width";
	private static final String LENGTH = "length";
	private static final String DESCRIPTION = "description";
	private static final String POWER_SOURCE = "power-source";	
	private static final String BATTERY_MODULE = "battery-module";
	private static final String ENERGY_PER_MODULE = "energy-per-module";
	private static final String FUEL_CELL_STACK = "fuel-cell-stack";
	private static final String DRIVETRAIN_EFFICIENCY = "drivetrain-efficiency";
	private static final String BASE_SPEED = "base-speed";
	private static final String BASE_POWER = "base-power";
	private static final String EMPTY_MASS = "empty-mass";
	private static final String CREW_SIZE = "crew-size";
	private static final String CARGO = "cargo";
	private static final String TOTAL_CAPACITY = "total-capacity";
	private static final String CAPACITY = "capacity";
	private static final String RESOURCE = "resource";
	private static final String SICKBAY = "sickbay";
	private static final String LAB = "lab";
	private static final String TECH_LEVEL = "tech-level";
	private static final String CAP_LEVEL = "capacity";
	private static final String BEDS = "beds";
	private static final String TECH_SPECIALTY = "tech-specialty";
	private static final String PART_ATTACHMENT = "part-attachment";
	private static final String NUMBER_SLOTS = "number-slots";
	private static final String PART = "part";
	private static final String AIRLOCK = "airlock";
	private static final String INTERIOR_LOCATION = "interior";
	private static final String EXTERIOR_LOCATION = "exterior";
	private static final String ACTIVITY = "activity";
	private static final String ACTIVITY_SPOT = "activity-spot";
	private static final String OPERATOR_TYPE = "operator";
	private static final String PASSENGER_TYPE = "passenger";
	private static final String SICKBAY_TYPE = "sickbay";
	private static final String LAB_TYPE = "lab";
	private static final String TERRAIN_HANDLING = "terrain-handling";
	
	private static final String VALUE = "value";
	private static final String NUMBER = "number";

	private transient Map<String, VehicleSpec> vehicleSpecMap;
	
	/**
	 * Constructor.
	 * 
	 * @param vehicleDoc {@link Document} DOM document with vehicle configuration.
	 * @param manuCon Use to calculate vehcile construction details
	 */
	public VehicleConfig(Document vehicleDoc, ManufactureConfig manuConfig) {
		loadVehicleSpecs(vehicleDoc, manuConfig);
	}

	/**
	 * Parses only once. Stores resulting data for later use.
	 * 
	 * @param vehicleDoc
	 * @param manuConfig 
	 */
	private synchronized void loadVehicleSpecs(Document vehicleDoc, ManufactureConfig manuConfig) {
		if (vehicleSpecMap != null) {
			// just in case if another thread is being created
			return;
		}
		
		// Build the global list in a temp to avoid access before it is built
		Map<String, VehicleSpec> newMap = new HashMap<>();
		
		Element root = vehicleDoc.getRootElement();
		List<Element> vehicleNodes = root.getChildren(VEHICLE);
		for (Element vehicleElement : vehicleNodes) {
			String name = vehicleElement.getAttributeValue(NAME);
			String model = vehicleElement.getAttributeValue(MODEL);
			VehicleType type = VehicleType.valueOf(ConfigHelper.convertToEnumName(vehicleElement.getAttributeValue(TYPE)));

			String baseImage = vehicleElement.getAttributeValue(BASE_IMAGE);
			if (baseImage == null) {
				baseImage = type.name().toLowerCase().replace(" ", "_");
			}

			// vehicle description
			double width = Double.parseDouble(vehicleElement.getAttributeValue(WIDTH));
			double length = Double.parseDouble(vehicleElement.getAttributeValue(LENGTH));
			String description = "No Description is Available.";
			if (vehicleElement.getChildren(DESCRIPTION).size() > 0) {
				description = vehicleElement.getChildText(DESCRIPTION);
			}
			
			String powerSourceType = "None";
			String fuelTypeStr = "None";
			double powerValue = 0;
			
			Element powerSourceElement = vehicleElement.getChild(POWER_SOURCE);
			powerSourceType = powerSourceElement.getAttributeValue(TYPE);
			fuelTypeStr = powerSourceElement.getAttributeValue(FUEL);
			powerValue = Double.parseDouble(powerSourceElement.getAttributeValue(VALUE));
			
			int battery = Integer.parseInt(vehicleElement.getChild(BATTERY_MODULE).getAttributeValue(NUMBER));
			double energyPerModule = Double.parseDouble(vehicleElement.getChild(ENERGY_PER_MODULE).getAttributeValue(VALUE));
			int fuelCell = Integer.parseInt(vehicleElement.getChild(FUEL_CELL_STACK).getAttributeValue(NUMBER));
			
			double drivetrainEff = Double
					.parseDouble(vehicleElement.getChild(DRIVETRAIN_EFFICIENCY).getAttributeValue(VALUE));
			double baseSpeed = Double.parseDouble(vehicleElement.getChild(BASE_SPEED).getAttributeValue(VALUE));
			double basePower = Double.parseDouble(vehicleElement.getChild(BASE_POWER).getAttributeValue(VALUE));
			double emptyMass = Double.parseDouble(vehicleElement.getChild(EMPTY_MASS).getAttributeValue(VALUE));
			
			int crewSize = Integer.parseInt(vehicleElement.getChild(CREW_SIZE).getAttributeValue(VALUE));

			VehicleSpec v = new VehicleSpec(name, type, model, description, baseImage, 
					powerSourceType, fuelTypeStr, powerValue,
					battery, energyPerModule, fuelCell, 
					drivetrainEff, baseSpeed, basePower, emptyMass, 
					crewSize);
			
			v.setWidth(width);
			v.setLength(length);
			
			// Ground vehicle terrain handling ability
			if (vehicleElement.getChild(TERRAIN_HANDLING) != null) {
				v.setTerrainHandling(Double.parseDouble(vehicleElement.getChild(TERRAIN_HANDLING).getAttributeValue(VALUE)));
			}

			// cargo capacities
			Element cargoElement = vehicleElement.getChild(CARGO);
			if (cargoElement != null) {
				Map<Integer, Double> cargoCapacityMap = new HashMap<>();
				double resourceCapacity = 0D;
				List<Element> capacityList = cargoElement.getChildren(CAPACITY);
				for (Element capacityElement : capacityList) {
					resourceCapacity = Double.parseDouble(capacityElement.getAttributeValue(VALUE));

					// toLowerCase() is crucial in matching resource name
					String resource = capacityElement.getAttributeValue(RESOURCE).toLowerCase();

					AmountResource ar = ResourceUtil.findAmountResource(resource);
					if (ar == null)
						logger.severe(
								resource + " shows up in vehicles.xml but doesn't exist in resources.xml.");
					else
						cargoCapacityMap.put(ar.getID(), resourceCapacity);		
					
				}
				
				double totalCapacity = Double.parseDouble(cargoElement.getAttributeValue(TOTAL_CAPACITY));
				v.setCargoCapacity(totalCapacity, cargoCapacityMap);
			}

			// Use the cargo capacity for performance analysis
			v.calculateDetails(manuConfig);
			
			// sickbay
			if (!vehicleElement.getChildren(SICKBAY).isEmpty()) {
				Element sickbayElement = vehicleElement.getChild(SICKBAY);
				if (sickbayElement != null) {
					int sickbayTechLevel = Integer.parseInt(sickbayElement.getAttributeValue(TECH_LEVEL));
					int sickbayBeds = Integer.parseInt(sickbayElement.getAttributeValue(BEDS));
					v.setSickBay(sickbayTechLevel, sickbayBeds);
				}
			}

			// labs
			if (!vehicleElement.getChildren(LAB).isEmpty()) {
				Element labElement = vehicleElement.getChild(LAB);
				if (labElement != null) {
					List<ScienceType> labTechSpecialties = new ArrayList<>();
					int labTechLevel = Integer.parseInt(labElement.getAttributeValue(TECH_LEVEL));
					int labCapacity = Integer.parseInt(labElement.getAttributeValue(CAP_LEVEL));
					for (Element tech : labElement.getChildren(TECH_SPECIALTY)) {
						String scienceName = tech.getAttributeValue(VALUE);
						labTechSpecialties
								.add(ScienceType.valueOf(ConfigHelper.convertToEnumName(scienceName)));
					}
					
					v.setLabSpec(labTechLevel, labCapacity, labTechSpecialties);
				}
			}

			// attachments
			if (!vehicleElement.getChildren(PART_ATTACHMENT).isEmpty()) {
				List<Part> attachableParts = new ArrayList<>();
				Element attachmentElement = vehicleElement.getChild(PART_ATTACHMENT);
				int attachmentSlots = Integer.parseInt(attachmentElement.getAttributeValue(NUMBER_SLOTS));
				for (Element part : attachmentElement.getChildren(PART)) {
					attachableParts.add((Part) ItemResourceUtil
							.findItemResource(((part.getAttributeValue(NAME)).toLowerCase())));
				}
				v.setAttachments(attachmentSlots, attachableParts);
			}

			// airlock locations (optional).
			Element airlockElement = vehicleElement.getChild(AIRLOCK);
			if (airlockElement != null) {
				LocalPosition airlockLoc = ConfigHelper.parseLocalPosition(airlockElement);
				LocalPosition airlockInteriorLoc = ConfigHelper.parseLocalPosition(airlockElement.getChild(INTERIOR_LOCATION));
				LocalPosition airlockExteriorLoc = ConfigHelper.parseLocalPosition(airlockElement.getChild(EXTERIOR_LOCATION));
				
				v.setAirlock(airlockLoc, airlockInteriorLoc, airlockExteriorLoc);
			}

			// Activity spots.
			Element activityElement = vehicleElement.getChild(ACTIVITY);
			if (activityElement != null) {

				// Initialize activity spot lists.
				List<LocalPosition> operatorActivitySpots = new ArrayList<>();
				List<LocalPosition> passengerActivitySpots = new ArrayList<>();
				List<LocalPosition> sickBayActivitySpots = new ArrayList<>();
				List<LocalPosition> labActivitySpots = new ArrayList<>();

				for (Object activitySpot : activityElement.getChildren(ACTIVITY_SPOT)) {
					Element activitySpotElement = (Element) activitySpot;
					LocalPosition spot = ConfigHelper.parseLocalPosition(activitySpotElement);
					String activitySpotType = activitySpotElement.getAttributeValue(TYPE);
					if (OPERATOR_TYPE.equals(activitySpotType)) {
						operatorActivitySpots.add(spot);
					} else if (PASSENGER_TYPE.equals(activitySpotType)) {
						passengerActivitySpots.add(spot);
					} else if (SICKBAY_TYPE.equals(activitySpotType)) {
						sickBayActivitySpots.add(spot);
					} else if (LAB_TYPE.equals(activitySpotType)) {
						labActivitySpots.add(spot);
					}
				}
				
				v.setActivitySpots(operatorActivitySpots, passengerActivitySpots, sickBayActivitySpots, labActivitySpots);
			}

			// Keep results for later use
			newMap.put(name.toLowerCase(), v);
		}
		
		vehicleSpecMap = Collections.unmodifiableMap(newMap);
	}

	/**
	 * Returns a set of all vehicle types.
	 * 
	 * @return set of vehicle types as strings.
	 * @throws Exception if error retrieving vehicle types.
	 */
	public Collection<VehicleSpec> getVehicleSpecs() {
		return Collections.unmodifiableCollection(vehicleSpecMap.values());
	}

	/**
	 * Gets the vehicle description class.
	 * 
	 * @param vehicleType
	 * @return {@link VehicleSpec}
	 */
	public VehicleSpec getVehicleSpec(String vehicleType) {
		return vehicleSpecMap.get(vehicleType.toLowerCase());
	}
	
	/**
	 * Prepares object for garbage collection. or simulation reboot.
	 */
	public void destroy() {
		if (vehicleSpecMap != null) {
			vehicleSpecMap = null;
		}
	}
}
