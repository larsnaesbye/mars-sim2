/**
 * Mars Simulation Project
 * SettlementConfig.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mars_sim.msp.core.interplanetary.transport.resupply.ResupplyMissionTemplate;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResourceUtil;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.resource.PartPackageConfig;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.BuildingTemplate.BuildingConnectionTemplate;

/**
 * Provides configuration information about settlements templates. Uses a DOM document to
 * get the information.
 */
public class SettlementConfig implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 2L;

	private static final Logger logger = Logger.getLogger(SettlementConfig.class.getName());

	private static int templateID = 0;
	
	// Element names
	private static final String ROVER_LIFE_SUPPORT_RANGE_ERROR_MARGIN = "rover-life-support-range-error-margin";
	private static final String ROVER_FUEL_RANGE_ERROR_MARGIN = "rover-fuel-range-error-margin";
	private static final String MISSION_CONTROL = "mission-control";
	private static final String LIFE_SUPPORT_REQUIREMENTS = "life-support-requirements";
	private static final String TOTAL_PRESSURE = "total-pressure";// low="99.9" high="102.7" />
	private static final String PARTIAL_PRESSURE_OF_O2 = "partial-pressure-of-oxygen"; // low="19.5" high="23.1" />
	private static final String PARTIAL_PRESSURE_OF_N2 = "partial-pressure-of-nitrogen";// low="79" high="79"/>
	private static final String PARTIAL_PRESSURE_OF_CO2 = "partial-pressure-of-carbon-dioxide"; // low=".4" high=".4" />
	private static final String TEMPERATURE = "temperature";// low="18.3" high="23.9"/>
	private static final String RELATIVE_HUMIDITY = "relative-humidity"; // low="30" high="70"/>
	private static final String VENTILATION = "ventilation";//
	private static final String LOW = "low";
	private static final String HIGH = "high";
	private static final String SETTLEMENT_TEMPLATE_LIST = "settlement-template-list";
	private static final String TEMPLATE = "template";
	private static final String NAME = "name";
	private static final String DEFAULT_POPULATION = "default-population";
	private static final String DEFAULT_NUM_ROBOTS = "number-of-robots";
	private static final String BUILDING = "building";
	private static final String ID = "id";
	private static final String TYPE = "type";
	private static final String WIDTH = "width";
	private static final String LENGTH = "length";
	private static final String X_LOCATION = "x-location";
	private static final String Y_LOCATION = "y-location";
	private static final String FACING = "facing";
	private static final String CONNECTION_LIST = "connection-list";
	private static final String CONNECTION = "connection";
	private static final String NUMBER = "number";
	private static final String VEHICLE = "vehicle";
	private static final String EQUIPMENT = "equipment";
	private static final String VALUE = "value";
	private static final String SPONSOR = "sponsor";
	private static final String RESUPPLY = "resupply";
	private static final String RESUPPLY_MISSION = "resupply-mission";
	private static final String ARRIVAL_TIME = "arrival-time";
	private static final String RESOURCE = "resource";
	private static final String AMOUNT = "amount";
	private static final String PART = "part";
	private static final String PART_PACKAGE = "part-package";

	// Random value indicator.
	public static final String RANDOM = "random";


	private double[] rover_values = new double[] { 0, 0 };
	private double[][] life_support_values = new double[2][7];

	// Data members
	private List<SettlementTemplate> settlementTemplates;
	private Map<Integer, String> templateMap = new HashMap<>();

	/**
	 * Constructor.
	 * 
	 * @param settlementDoc     DOM document with settlement configuration.
	 * @param partPackageConfig the part package configuration.
	 * @throws Exception if error reading XML document.
	 */
	public SettlementConfig(Document settlementDoc, PartPackageConfig partPackageConfig) {
		settlementTemplates = new ArrayList<SettlementTemplate>();
		loadMissionControl(settlementDoc);
		loadLifeSupportRequirements(settlementDoc);
		loadSettlementTemplates(settlementDoc, partPackageConfig);
	}

	/**
	 * Maps a number to an alphabet
	 * 
	 * @param a number
	 * @return a String
	 */
	private String getCharForNumber(int i) {
		// NOTE: i must be > 1, if i = 0, return null
		return i > 0 && i < 27 ? String.valueOf((char) (i + 'A' - 1)) : null;
	}

	public double[] getRoverValues() {
		return rover_values;
	}
	
	/**
	 * Load the rover range margin error from the mission control parameters of a
	 * settlement from the XML document.
	 * 
	 * @return range margin.
	 * @throws Exception if error reading XML document.
	 */
	private void loadMissionControl(Document settlementDoc) {
		if (rover_values[0] != 0 || rover_values[1] != 0) {
			// System.out.println("using saved rover_values");
			return;
		}
		
		Element root = settlementDoc.getRootElement();
		Element missionControlElement = root.getChild(MISSION_CONTROL);
		Element lifeSupportRange = (Element) missionControlElement.getChild(ROVER_LIFE_SUPPORT_RANGE_ERROR_MARGIN);
		Element fuelRange = (Element) missionControlElement.getChild(ROVER_FUEL_RANGE_ERROR_MARGIN);

		rover_values[0] = Double.parseDouble(lifeSupportRange.getAttributeValue(VALUE));
		if (rover_values[0] < 1.0 || rover_values[0] > 3.0)
			throw new IllegalStateException(
					"Error in SettlementConfig.xml: rover life support range error margin is beyond acceptable range.");

		rover_values[1] = Double.parseDouble(fuelRange.getAttributeValue(VALUE));
		if (rover_values[1] < 1.0 || rover_values[1] > 3.0)
			throw new IllegalStateException(
					"Error in SettlementConfig.xml: rover fuel range error margin is beyond acceptable range.");
	}

	/**
	 * Load the life support requirements from the XML document.
	 * 
	 * @return an array of double.
	 * @throws Exception if error reading XML document.
	 */
	public double[][] getLifeSupportRequirements() {
		return life_support_values;
	}
	
	/**
	 * Load the life support requirements from the XML document.
	 * 
	 * @return an array of double.
	 * @throws Exception if error reading XML document.
	 */
	private void loadLifeSupportRequirements(Document settlementDoc) {
		if (life_support_values[0][0] != 0) {
			// testing only the value at [0][0]
			return;
		}

		Element root = settlementDoc.getRootElement();
		Element req = (Element) root.getChild(LIFE_SUPPORT_REQUIREMENTS);

		String[] types = new String[] {
				TOTAL_PRESSURE, 
				PARTIAL_PRESSURE_OF_O2, 
				PARTIAL_PRESSURE_OF_N2,
				PARTIAL_PRESSURE_OF_CO2, 
				TEMPERATURE, 
				RELATIVE_HUMIDITY, 
				VENTILATION};

		for (int j = 0; j < 2; j++) {
			for (int i = 0; i < 7; i++) {
				double t[] = getValues(req, types[i]);
				life_support_values[j][i] = t[j];
			}
		}
	}

	private double[] getValues(Element element, String name) {
		Element el = (Element) element.getChild(name);

		double a = Double.parseDouble(el.getAttributeValue(LOW));
		// if (result[0] < 1.0 || result[0] > 15.0 )
		// result[0] = 101.0;
		// System.out.println(a);

		double b = Double.parseDouble(el.getAttributeValue(HIGH));
		// if (result[0] < 1.0 || result[0] > 15.0 )
		// result[0] = 99.0;
		// System.out.println(b);

		return new double[] { a, b };

//		TOTAL_PRESSURE; // low="99.9" high="102.7" />
//		PARTIAL_PRESSURE_OF_O2 ; //low="19.5" high="23.1" />
//		PARTIAL_PRESSURE_OF_N2 ;// low="79" high="79"/>
//		PARTIAL_PRESSURE_OF_CO2 ; //low=".4" high=".4" />
//		TEMPERATURE ;// low="18.3" high="23.9"/>
//		RELATIVE_HUMIDITY ; //low="30" high="70"/>
//		VENTILATION ;//
	}

	/**
	 * Load the settlement templates from the XML document.
	 * 
	 * @param settlementDoc     DOM document with settlement configuration.
	 * @param partPackageConfig the part package configuration.
	 * @throws Exception if error reading XML document.
	 */
	private void loadSettlementTemplates(Document settlementDoc, PartPackageConfig partPackageConfig) {

		Element root = settlementDoc.getRootElement();
		Element templateList = root.getChild(SETTLEMENT_TEMPLATE_LIST);
		
		List<Element> templateNodes = templateList.getChildren(TEMPLATE);
		
		for (Element templateElement : templateNodes) {
			String settlementTemplateName = templateElement.getAttributeValue(NAME);
			String sponsor = templateElement.getAttributeValue(SPONSOR);
			if (templateMap.containsKey(templateID)) {
				throw new IllegalStateException("Error in SettlementConfig.xml: template ID in settlement template "
						+ settlementTemplateName + " is not unique.");
			} else
				templateMap.put(templateID, settlementTemplateName);
			
			// Obtains the default population
			int defaultPopulation = Integer.parseInt(templateElement.getAttributeValue(DEFAULT_POPULATION));
			// Obtains the default numbers of robots
			int defaultNumOfRobots = Integer.parseInt(templateElement.getAttributeValue(DEFAULT_NUM_ROBOTS));

			// Add templateID
			SettlementTemplate template = new SettlementTemplate(
					templateID, 
					settlementTemplateName, 
					sponsor,
					defaultPopulation,
					defaultNumOfRobots);
			
			settlementTemplates.add(template);

			Set<Integer> existingIDs = new HashSet<>();//HashMap.newKeySet();
			// Add buildingTypeIDMap
			Map<String, Integer> buildingTypeIDMap = new HashMap<>();

			List<Element> buildingNodes = templateElement.getChildren(BUILDING);
			for (Element buildingElement : buildingNodes) {

				double width = -1D;
				if (buildingElement.getAttribute(WIDTH) != null) {
					width = Double.parseDouble(buildingElement.getAttributeValue(WIDTH));
				}

				// Determine optional length attribute value. "-1" if it doesn't exist.
				double length = -1D;
				if (buildingElement.getAttribute(LENGTH) != null) {
					length = Double.parseDouble(buildingElement.getAttributeValue(LENGTH));
				}

				double xLoc = Double.parseDouble(buildingElement.getAttributeValue(X_LOCATION));
				double yLoc = Double.parseDouble(buildingElement.getAttributeValue(Y_LOCATION));
				double facing = Double.parseDouble(buildingElement.getAttributeValue(FACING));
		
				int bid = Integer.parseInt(buildingElement.getAttributeValue(ID));
				if (existingIDs.contains(bid)) {
					throw new IllegalStateException(
							"Error in SettlementConfig : building ID " + bid + " in settlement template "
									+ settlementTemplateName + " is not unique.");
				} else
					existingIDs.add(bid);

				String buildingType = buildingElement.getAttributeValue(TYPE);

				if (buildingTypeIDMap.containsKey(buildingType)) {
					int last = buildingTypeIDMap.get(buildingType);
					buildingTypeIDMap.put(buildingType, last + 1);
				} else
					buildingTypeIDMap.put(buildingType, 1);

				// Create a building nickname for every building
				// by appending the settlement id and building id to that building's type.
				String templateString = getCharForNumber(templateID + 1);
				// NOTE: i = sid + 1 since i must be > 1, if i = 0, s = null
	
				int buildingTypeID = buildingTypeIDMap.get(buildingType);

				String buildingNickName = buildingType + " " + buildingTypeID;

				BuildingTemplate buildingTemplate = new BuildingTemplate(settlementTemplateName, bid, templateString,
						buildingType, buildingNickName, width, length, xLoc, yLoc, facing);

				template.addBuildingTemplate(buildingTemplate);

				// Create building connection templates.
				Element connectionListElement = buildingElement.getChild(CONNECTION_LIST);
				if (connectionListElement != null) {
					List<Element> connectionNodes = connectionListElement.getChildren(CONNECTION);
					for (Element connectionElement : connectionNodes) {
						int connectionID = Integer.parseInt(connectionElement.getAttributeValue(ID));

						// Check that connection ID is not the same as the building ID.
						if (connectionID == bid) {
							throw new IllegalStateException(
									"Connection ID cannot be the same as building ID for building: " + buildingType
											+ " in settlement template: " + settlementTemplateName);
						}

						double connectionXLoc = Double.parseDouble(connectionElement.getAttributeValue(X_LOCATION));
						double connectionYLoc = Double.parseDouble(connectionElement.getAttributeValue(Y_LOCATION));

						buildingTemplate.addBuildingConnection(connectionID, connectionXLoc, connectionYLoc);
					}
				}
			}

			// Check that building connections point to valid building ID's.
			List<BuildingTemplate> buildingTemplates = template.getBuildingTemplates();
			for (BuildingTemplate buildingTemplate : buildingTemplates) {
				List<BuildingConnectionTemplate> connectionTemplates = buildingTemplate
						.getBuildingConnectionTemplates();
				for (BuildingConnectionTemplate connectionTemplate : connectionTemplates) {
					if (!existingIDs.contains(connectionTemplate.getID())) {
						throw new IllegalStateException("Connection ID: " + connectionTemplate.getID()
								+ " invalid for building: " + buildingTemplate.getNickName()
								+ " in settlement template: " + settlementTemplateName);
					}
				}
			}

			// Load vehicles
			List<Element> vehicleNodes = templateElement.getChildren(VEHICLE);
			for (Element vehicleElement : vehicleNodes) {
				String vehicleType = vehicleElement.getAttributeValue(TYPE);
				int vehicleNumber = Integer.parseInt(vehicleElement.getAttributeValue(NUMBER));
				template.addVehicles(vehicleType, vehicleNumber);
			}

			// Load equipment
			List<Element> equipmentNodes = templateElement.getChildren(EQUIPMENT);
			for (Element equipmentElement : equipmentNodes) {
				String equipmentType = equipmentElement.getAttributeValue(TYPE);
				int equipmentNumber = Integer.parseInt(equipmentElement.getAttributeValue(NUMBER));
				template.addEquipment(equipmentType, equipmentNumber);
			}

			// Load resources
			List<Element> resourceNodes = templateElement.getChildren(RESOURCE);
			for (Element resourceElement : resourceNodes) {
				String resourceType = resourceElement.getAttributeValue(TYPE);
				AmountResource resource = ResourceUtil.findAmountResource(resourceType);
				if (resource == null)
					logger.severe(resourceType + " shows up in settlements.xml but doesn't exist in resources.xml.");
				else {
					double resourceAmount = Double.parseDouble(resourceElement.getAttributeValue(AMOUNT));
					template.addAmountResource(resource, resourceAmount);
				}

			}

			// Load parts
			List<Element> partNodes = templateElement.getChildren(PART);
			for (Element partElement : partNodes) {
				String partType = partElement.getAttributeValue(TYPE);
				Part part = (Part) ItemResourceUtil.findItemResource(partType);
				if (part == null)
					logger.severe(partType + " shows up in settlements.xml but doesn't exist in parts.xml.");
				else {
					int partNumber = Integer.parseInt(partElement.getAttributeValue(NUMBER));
					template.addPart(part, partNumber);
				}
			}

			// Load part packages
			List<Element> partPackageNodes = templateElement.getChildren(PART_PACKAGE);
			for (Element partPackageElement : partPackageNodes) {
				String packageName = partPackageElement.getAttributeValue(NAME);
				int packageNumber = Integer.parseInt(partPackageElement.getAttributeValue(NUMBER));
				if (packageNumber > 0) {
					for (int z = 0; z < packageNumber; z++) {
						Map<Part, Integer> partPackage = partPackageConfig.getPartsInPackage(packageName);
						Iterator<Part> i = partPackage.keySet().iterator();
						while (i.hasNext()) {
							Part part = i.next();
							int partNumber = partPackage.get(part);
							template.addPart(part, partNumber);
						}
					}
				}
			}

			// Load resupplies
			Element resupplyList = templateElement.getChild(RESUPPLY);
			if (resupplyList != null) {
				List<Element> resupplyNodes = resupplyList.getChildren(RESUPPLY_MISSION);
				for (Element resupplyMissionElement : resupplyNodes) {
					String resupplyName = resupplyMissionElement.getAttributeValue(NAME);
					double arrivalTime = Double.parseDouble(resupplyMissionElement.getAttributeValue(ARRIVAL_TIME));
					ResupplyMissionTemplate resupplyMissionTemplate = new ResupplyMissionTemplate(resupplyName,
							arrivalTime);
					template.addResupplyMissionTemplate(resupplyMissionTemplate);
				}
			}
			// Increments the templateID to be used for the next template
			templateID++;
		}
	}

	public int getTemplateID() {
		return templateID;
	}

	/**
	 * Gets the settlement template that matches a template name.
	 * 
	 * @param templateName the template name.
	 * @return settlement template
	 */
	public SettlementTemplate getSettlementTemplate(String templateName) {
		SettlementTemplate result = null;

		Iterator<SettlementTemplate> i = settlementTemplates.iterator();
		while (i.hasNext()) {
			SettlementTemplate template = i.next();
			if (template.getTemplateName().equals(templateName))
				result = template;
		}

		if (result == null) {
			throw new IllegalArgumentException("Template named '" + templateName + "' not found.");
		}

		return result;
	}

	/**
	 * Gets a list of settlement templates.
	 * 
	 * @return list of settlement templates.
	 */
	public List<SettlementTemplate> getSettlementTemplates() {
		return settlementTemplates;
	}

	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		Iterator<SettlementTemplate> i = settlementTemplates.iterator();
		while (i.hasNext()) {
			i.next().destroy();
		}
		settlementTemplates.clear();
		settlementTemplates = null;
		templateMap.clear();
		templateMap = null;
	}
}
