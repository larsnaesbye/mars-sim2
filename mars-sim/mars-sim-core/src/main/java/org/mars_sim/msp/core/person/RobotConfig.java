/**
 * Mars Simulation Project
 * RobotConfig.java
 * @version 3.07 2015-01-21
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom.Document;
import org.jdom.Element;

/**
 * Provides configuration information about robot units.
 * Uses a JDOM document to get the information.
 */
public class RobotConfig
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private List<String> alphaCrewRobotName; // = new ArrayList<String>();
	private List<String> alphaCrewRobotType; // = new ArrayList<String>();
	private List<String> alphaCrewRobotJob; //  = new ArrayList<String>();
	
	// Element names
	private static final String ROBOT_NAME = "name";
	private static final String ROBOT_TYPE = "type";
	private static final String POWER_CONSUMPTION_RATE = "power-consumption-rate";
	private static final String POWER_DEPRIVATION_TIME = "power-deprivation-time";

	private static final String MIN_AIR_PRESSURE = "min-air-pressure";
	private static final String DECOMPRESSION_TIME = "decompression-time";
	private static final String MIN_TEMPERATURE = "min-temperature";
	private static final String MAX_TEMPERATURE = "max-temperature";
	private static final String FREEZING_TIME = "freezing-time";
	private static final String ROBOT_LIST = "robot-list";
	private static final String ROBOT = "robot";
	private static final String SETTLEMENT = "settlement";
	private static final String JOB = "job";
	private static final String NATURAL_ATTRIBUTE_LIST = "natural-attribute-list";
	private static final String NATURAL_ATTRIBUTE = "natural-attribute";
	private static final String NAME= "name";
	private static final String VALUE= "value";
	private static final String SKILL_LIST = "skill-list";
	private static final String SKILL = "skill";
	private static final String LEVEL = "level";
//	private static final String OPINION = "opinion";
//	private static final String PERCENTAGE = "percentage";

	private Document robotDoc;
	private List<String> nameList;

	/**
	 * Constructor
	 * @param robotDoc the robot config DOM document.
	 */
	public RobotConfig(Document robotDoc) {
		this.robotDoc = robotDoc;
	}

	/**
	 * Gets a list of Robot names for settlers.
	 * @return List of Robot names.
	 * @throws Exception if Robot names could not be found.
	 
    @SuppressWarnings("unchecked")
	public List<String> getRobotList() {

		if (nameList == null) {
			nameList = new ArrayList<String>();
			Element root = robotDoc.getRootElement();
			Element robotNameList = root.getChild(ROBOT_NAME);
			List<Element> robotNames = robotNameList.getChildren(ROBOT_TYPE);

			for (Element nameElement : robotNames) {
				nameList.add(nameElement.getAttributeValue(VALUE));
			}
		}

		return nameList;
	}
*/
	/**
	 * Gets the Power consumption rate.
	 * @return Power rate (kg/sol)
	 * @throws Exception if consumption rate could not be found.
	 */
	public double getPowerConsumptionRate() {
		return getValueAsDouble(POWER_CONSUMPTION_RATE);
	}

	/**
	 * Gets the Power deprivation time.
	 * @return Power time in sols.
	 * @throws Exception if Power deprivation time could not be found.
	 */
	public double getPowerDeprivationTime() {
		return getValueAsDouble(POWER_DEPRIVATION_TIME);
	}


	/**
	 * Gets the required air pressure.
	 * @return air pressure in Pa.
	 * @throws Exception if air pressure could not be found.
	 */
	public double getMinAirPressure() {
		return getValueAsDouble(MIN_AIR_PRESSURE);
	}

	/**
	 * Gets the max decompression time a robot can survive.
	 * @return decompression time in millisols.
	 * @throws Exception if decompression time could not be found.
	 */
	public double getDecompressionTime() {
		return getValueAsDouble(DECOMPRESSION_TIME);
	}

	/**
	 * Gets the minimum temperature a robot can tolerate.
	 * @return temperature in celsius
	 * @throws Exception if min temperature cannot be found.
	 */
	public double getMinTemperature() {
		return getValueAsDouble(MIN_TEMPERATURE);
	}

	/**
	 * Gets the maximum temperature a robot can tolerate.
	 * @return temperature in celsius
	 * @throws Exception if max temperature cannot be found.
	 */
	public double getMaxTemperature() {
		return getValueAsDouble(MAX_TEMPERATURE);
	}

	/**
	 * Gets the time a robot can survive below minimum temperature.
	 * @return freezing time in millisols.
	 * @throws Exception if freezing time could not be found.
	 */
	public double getFreezingTime() {
		return getValueAsDouble(FREEZING_TIME);
	}
	

	/**
	 * Gets the configured robot's name.
	 * @param index the robot's index.
	 * @return name or null if none.
	 * @throws Exception if error in XML parsing.
	 
	public String getConfiguredRobotType(int index) {
		//String s = getValueAsString(index,TYPE);
		//alphaCrewName.add(s);
		//return s;	
		if (alphaCrewRobotName != null)
			return alphaCrewRobotName.get(index) ;
		else 
			return getValueAsString(index,ROBOT_TYPE);
	}
*/
	/**
	 * Gets the configured robot's starting settlement.
	 * @param index the robot's index.
	 * @return the settlement name or null if none.
	 * @throws Exception if error in XML parsing.
	 */
	public String getConfiguredRobotSettlement(int index) {
		return getValueAsString(index,SETTLEMENT);
	}

	/**
	 * Gets the configured robot's job.
	 * @param index the robot's index.
	 * @return the job name or null if none.
	 * @throws Exception if error in XML parsing.
	 */
	public String getConfiguredRobotJob(int index) {
		if (alphaCrewRobotJob != null)
			return alphaCrewRobotJob.get(index) ;
		else
			return getValueAsString(index,JOB);
	}

	public void setRobotName(int index, String value) {
		if (alphaCrewRobotName == null) 
			alphaCrewRobotName = new ArrayList<String>(4);
		if (alphaCrewRobotName.size() == 4) {
			alphaCrewRobotName.set(index, value);
		} else
			alphaCrewRobotName.add(value);
	}
/*
	public void setPersonPersonality(int index, String value) {
		if (alphaCrewPersonality == null)  
			alphaCrewPersonality = new ArrayList<String>(4);
		if (alphaCrewPersonality.size() == 4) {
			alphaCrewPersonality.set(index, value);
		} else
			alphaCrewPersonality.add(value);
	}
*/
	public void setRobotJob(int index,String value) {
		if (alphaCrewRobotJob == null)  
			alphaCrewRobotJob = new ArrayList<String>(4);
		if (alphaCrewRobotJob.size() == 4) {
			alphaCrewRobotJob.set(index, value);
		} else
			alphaCrewRobotJob.add(value);
	}
	

	/**
	 * Gets the number of robots configured for the simulation.
	 * @return number of robots.
	 * @throws Exception if error in XML parsing.
	 */
	public int getNumberOfConfiguredRobots() {
		Element root = robotDoc.getRootElement();
		Element robotList = root.getChild(ROBOT_LIST);
		List robotNodes = robotList.getChildren(ROBOT);
		if (robotNodes != null) return robotNodes.size();
		else return 0;
	}
	

	/**
	 * Gets the configured Robot's name.
	 * @param index the Robot's index.
	 * @return name or null if none.
	 * @throws Exception if error in XML parsing.
	 */
	public String getConfiguredRobotName(int index) {
		String s = getValueAsString(index,ROBOT_NAME);
		//alphaCrewName.add(s);
		return s;	
		//if (alphaCrewName != null)
		//	return alphaCrewName.get(index) ;
		//else 
		//	return getValueAsString(index,ROBOT_NAME);
	}

	/**
	 * Gets the configured RobotType.
	 * @param index the Robot's index.
	 * @return {@link RobotType} or null if not found.
	 * @throws Exception if error in XML parsing.
	 */
	public RobotType getConfiguredRobotType(int index) {
		if (alphaCrewRobotType != null)
			return RobotType.valueOfIgnoreCase(alphaCrewRobotType.get(index)) ;
		else 
			return RobotType.valueOfIgnoreCase(getValueAsString(index, ROBOT_TYPE));
	}

	
	/**
	 * Gets a map of the configured robot's natural attributes.
	 * @param index the robot's index.
	 * @return map of natural attributes (empty map if not found).
	 * @throws Exception if error in XML parsing.
	 */
    @SuppressWarnings("unchecked")
	public Map<String, Integer> getNaturalAttributeMap(int index) {
		Map<String, Integer> result = new HashMap<String, Integer>();
		Element root = robotDoc.getRootElement();
		Element robotList = root.getChild(ROBOT_LIST);
		Element robotElement = (Element) robotList.getChildren(ROBOT).get(index);
		List<Element> naturalAttributeListNodes = robotElement.getChildren(NATURAL_ATTRIBUTE_LIST);

		if ((naturalAttributeListNodes != null) && (naturalAttributeListNodes.size() > 0)) {
			Element naturalAttributeList = naturalAttributeListNodes.get(0);
			int attributeNum = naturalAttributeList.getChildren(NATURAL_ATTRIBUTE).size();

			for (int x=0; x < attributeNum; x++) {
				Element naturalAttributeElement = (Element) naturalAttributeList.getChildren(NATURAL_ATTRIBUTE).get(x);
				String name = naturalAttributeElement.getAttributeValue(NAME);
				Integer value = new Integer(naturalAttributeElement.getAttributeValue(VALUE));
				result.put(name, value);
			}
		}
		return result;
	}

	private String getValueAsString(int index, String param){
		Element root = robotDoc.getRootElement();
		Element robotList = root.getChild(ROBOT_LIST);
		Element robotElement = (Element) robotList.getChildren(ROBOT).get(index);
		return robotElement.getAttributeValue(param);
	}


	private double getValueAsDouble(String child) {
		Element root = robotDoc.getRootElement();
		Element element = root.getChild(child);
		String str = element.getAttributeValue(VALUE);
		return Double.parseDouble(str);
	}
	/**
	 * Gets a map of the configured robot's skills.
	 * @param index the robot's index.
	 * @return map of skills (empty map if not found).
	 * @throws Exception if error in XML parsing.
	 */
    @SuppressWarnings("unchecked")
	public Map<String, Integer> getSkillMap(int index) {
		Map<String, Integer> result = new HashMap<String, Integer>();
		Element root = robotDoc.getRootElement();
		// 2014-10-07 mkung: changed the people.xml element from "robot-list" to "alpha-team"
		Element robotList = root.getChild(ROBOT_LIST);
		Element robotElement = (Element) robotList.getChildren(ROBOT).get(index);
		List<Element> skillListNodes = robotElement.getChildren(SKILL_LIST);
		if ((skillListNodes != null) && (skillListNodes.size() > 0)) {
			Element skillList = skillListNodes.get(0);
			int skillNum = skillList.getChildren(SKILL).size();
			for (int x=0; x < skillNum; x++) {
				Element skillElement = (Element) skillList.getChildren(SKILL).get(x);
				String name = skillElement.getAttributeValue(NAME);
				Integer level = new Integer(skillElement.getAttributeValue(LEVEL));
				result.put(name, level);
			}
		}
		return result;
	}

    /**
     * Prepare object for garbage collection.
     */
    public void destroy() {
        robotDoc = null;
        if(nameList != null){
            nameList.clear();
            nameList = null;
        }
        alphaCrewRobotName.clear();
        alphaCrewRobotName = null;
        alphaCrewRobotJob.clear();
        alphaCrewRobotJob = null;
    }
}
