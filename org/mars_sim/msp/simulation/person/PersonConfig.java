/**
 * Mars Simulation Project
 * PersonConfig.java
 * @version 2.75 2004-03-10
 * @author Scott Davis
 */
package org.mars_sim.msp.simulation.person;

import java.util.*;
import org.w3c.dom.*;

/**
 * Provides configuration information about people units.
 * Uses a DOM document to get the information. 
 */
public class PersonConfig {
	
	// Element names
	private static final String PERSON_NAME_LIST = "person-name-list";
	private static final String PERSON_NAME = "person-name";
	private static final String OXYGEN_CONSUMPTION_RATE = "oxygen-consumption-rate";
	private static final String WATER_CONSUMPTION_RATE = "water-consumption-rate";
	private static final String FOOD_CONSUMPTION_RATE = "food-consumption-rate";
	private static final String OXYGEN_DEPRIVATION_TIME = "oxygen-deprivation-time";
	private static final String WATER_DEPRIVATION_TIME = "water-deprivation-time";
	private static final String FOOD_DEPRIVATION_TIME = "food-deprivation-time";
	private static final String MIN_AIR_PRESSURE = "min-air-pressure";
	private static final String DECOMPRESSION_TIME = "decompression-time";
	private static final String MIN_TEMPERATURE = "min-temperature";
	private static final String MAX_TEMPERATURE = "max-temperature";
	private static final String FREEZING_TIME = "freezing-time";
	
	private Document personDoc;
	private List nameList;

	/**
	 * Constructor
	 * @param personDoc the person congif DOM document.
	 */
	public PersonConfig(Document personDoc) {
		this.personDoc = personDoc;
	}
	
	/**
	 * Gets a list of person names for settlers.
	 * @return List of person names.
	 * @throws Exception if person names could not be found.
	 */
	public List getPersonNameList() throws Exception {
		
		if (nameList == null) {
			nameList = new ArrayList();
			Element root = personDoc.getDocumentElement();
			Element personNameList = (Element) root.getElementsByTagName(PERSON_NAME_LIST).item(0);
			NodeList personNames = personNameList.getElementsByTagName(PERSON_NAME);
			for (int x=0; x < personNames.getLength(); x++) {
					Element nameElement = (Element) personNames.item(x);
					nameList.add(nameElement.getAttribute("value"));
			}
		}
		
		return nameList;
	}
	
	/**
	 * Gets the oxygen consumption rate.
	 * @return oxygen rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getOxygenConsumptionRate() throws Exception {
		Element root = personDoc.getDocumentElement();
		Element oxygenRateElement = (Element) root.getElementsByTagName(OXYGEN_CONSUMPTION_RATE).item(0);
		String oxygenRateStr = oxygenRateElement.getAttribute("value");
		double oxygenRate = Double.parseDouble(oxygenRateStr);
		return oxygenRate;
	}
	
	/**
	 * Gets the water consumption rate.
	 * @return water rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getWaterConsumptionRate() throws Exception {
		Element root = personDoc.getDocumentElement();
		Element waterRateElement = (Element) root.getElementsByTagName(WATER_CONSUMPTION_RATE).item(0);
		String waterRateStr = waterRateElement.getAttribute("value");
		double waterRate = Double.parseDouble(waterRateStr);
		return waterRate;
	}
	
	/**
	 * Gets the food consumption rate.
	 * @return food rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getFoodConsumptionRate() throws Exception {
		Element root = personDoc.getDocumentElement();
		Element foodRateElement = (Element) root.getElementsByTagName(FOOD_CONSUMPTION_RATE).item(0);
		String foodRateStr = foodRateElement.getAttribute("value");
		double foodRate = Double.parseDouble(foodRateStr);
		return foodRate;
	}
	
	/**
	 * Gets the oxygen deprivation time.
	 * @return oxygen time in millisols.
	 * @throws Exception if oxygen deprivation time could not be found.
	 */
	public double getOxygenDeprivationTime() throws Exception {
		Element root = personDoc.getDocumentElement();
		Element oxygenTimeElement = (Element) root.getElementsByTagName(OXYGEN_DEPRIVATION_TIME).item(0);
		String oxygenTimeStr = oxygenTimeElement.getAttribute("value");
		double oxygenTime = Double.parseDouble(oxygenTimeStr);
		return oxygenTime;
	}
	
	/**
	 * Gets the water deprivation time.
	 * @return water time in sols.
	 * @throws Exception if water deprivation time could not be found.
	 */
	public double getWaterDeprivationTime() throws Exception {
		Element root = personDoc.getDocumentElement();
		Element waterTimeElement = (Element) root.getElementsByTagName(WATER_DEPRIVATION_TIME).item(0);
		String waterTimeStr = waterTimeElement.getAttribute("value");
		double waterTime = Double.parseDouble(waterTimeStr);
		return waterTime;
	}
	
	/**
	 * Gets the food deprivation time.
	 * @return food time in sols.
	 * @throws Exception if food deprivation time could not be found.
	 */
	public double getFoodDeprivationTime() throws Exception {
		Element root = personDoc.getDocumentElement();
		Element foodTimeElement = (Element) root.getElementsByTagName(FOOD_DEPRIVATION_TIME).item(0);
		String foodTimeStr = foodTimeElement.getAttribute("value");
		double foodTime = Double.parseDouble(foodTimeStr);
		return foodTime;
	}
	
	/**
	 * Gets the required air pressure.
	 * @return air pressure in atm.
	 * @throws Exception if air pressure could not be found.
	 */
	public double getMinAirPressure() throws Exception {
		Element root = personDoc.getDocumentElement();
		Element airPressureElement = (Element) root.getElementsByTagName(MIN_AIR_PRESSURE).item(0);
		String airPressureStr = airPressureElement.getAttribute("value");
		double airPressure = Double.parseDouble(airPressureStr);
		return airPressure;
	}
	
	/**
	 * Gets the max decompression time a person can survive.
	 * @return decompression time in millisols.
	 * @throws Exception if decompression time could not be found.
	 */
	public double getDecompressionTime() throws Exception {
		Element root = personDoc.getDocumentElement();
		Element decompressionTimeElement = (Element) root.getElementsByTagName(DECOMPRESSION_TIME).item(0);
		String decompressionTimeStr = decompressionTimeElement.getAttribute("value");
		double decompressionTime = Double.parseDouble(decompressionTimeStr);
		return decompressionTime;
	}
	
	/**
	 * Gets the minimum temperature a person can tolerate.
	 * @return temperature in celsius
	 * @throws Exception if min temperature cannot be found.
	 */
	public double getMinTemperature() throws Exception {
		Element root = personDoc.getDocumentElement();
		Element minTemperatureElement = (Element) root.getElementsByTagName(MIN_TEMPERATURE).item(0);
		String minTemperatureStr = minTemperatureElement.getAttribute("value");
		double minTemperature = Double.parseDouble(minTemperatureStr);
		return minTemperature;
	}
	
	/**
	 * Gets the maximum temperature a person can tolerate.
	 * @return temperature in celsius
	 * @throws Exception if max temperature cannot be found.
	 */
	public double getMaxTemperature() throws Exception {
		Element root = personDoc.getDocumentElement();
		Element maxTemperatureElement = (Element) root.getElementsByTagName(MAX_TEMPERATURE).item(0);
		String maxTemperatureStr = maxTemperatureElement.getAttribute("value");
		double maxTemperature = Double.parseDouble(maxTemperatureStr);
		return maxTemperature;
	}
	
	/**
	 * Gets the time a person can survive below minimum temperature.
	 * @return freezing time in millisols.
	 * @throws Exception if freezing time could not be found.
	 */
	public double getFreezingTime() throws Exception {
		Element root = personDoc.getDocumentElement();
		Element freezingTimeElement = (Element) root.getElementsByTagName(FREEZING_TIME).item(0);
		String freezingTimeStr = freezingTimeElement.getAttribute("value");
		double freezingTime = Double.parseDouble(freezingTimeStr);
		return freezingTime;
	}
}