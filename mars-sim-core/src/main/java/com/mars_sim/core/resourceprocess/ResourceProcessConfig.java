/*
 * Mars Simulation Project
 * ResourceProcessConfig.java
 * @date 2022-10-23
 * @author Barry Evans
 */
package com.mars_sim.core.resourceprocess;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import com.mars_sim.core.configuration.ConfigHelper;
import com.mars_sim.core.resource.ResourceUtil;

/**
 * Parses a definition of Resource processes from XML and creates a lookup library.
 */
public class ResourceProcessConfig {
	
    private static final String DEFAULT = "defaultOn";
	private static final String NAME = "name";
	private static final String POWER_REQUIRED = "power-required";
	private static final String PROCESS = "process";
	private static final String INPUT = "input";
	private static final String OUTPUT = "output";
	private static final String WORK_TIME = "work-time";
	private static final String PROCESS_TIME = "process-time";
	private static final String RATE = "rate";
	private static final String AMBIENT = "ambient";
	private static final String WASTE = "waste";
	private static final String RESOURCE = "resource";

	private Map<String, ResourceProcessSpec> processSpecMap = new HashMap<>();

	/**
	 * Constructor.
	 *
	 * @param processDoc DOM document with ProcessSpec configuration
	 */
	public ResourceProcessConfig(Document processDoc) {

		List<Element> processNodes = processDoc.getRootElement().getChildren(PROCESS);
		for (Element processElement : processNodes) {
			ResourceProcessSpec newProcess = parseProcessSpec(processElement);

			String key = newProcess.getName().toLowerCase();
			if (processSpecMap.containsKey(key)) {
				throw new IllegalStateException("There are duplicate defintions in " + getClass().getSimpleName()
							+ " with the name '" + newProcess.getName() + "'");
			}
			processSpecMap.put(key, newProcess);
		}
	}

	/**
	 * Parses the specific Processing 
	 * 
	 * @param resourceProcessingElement
	 */
	private ResourceProcessSpec parseProcessSpec(Element processElement) {

		String name = processElement.getAttributeValue(NAME);
        double powerRequired = Double.parseDouble(processElement.getAttributeValue(POWER_REQUIRED));
		int processTime = ConfigHelper.getOptionalAttributeInt(processElement, PROCESS_TIME, 200);
		int workTime = ConfigHelper.getOptionalAttributeInt(processElement, WORK_TIME, 10);
        boolean defaultOn = ConfigHelper.getOptionalAttributeBool(processElement, DEFAULT, false);
    
        ResourceProcessSpec process =  new ResourceProcessSpec(name, powerRequired, processTime, workTime, defaultOn);

		// Get input resources.
		List<Element> inputNodes = processElement.getChildren(INPUT);
		for (Element inputElement : inputNodes) {
			String resourceName = inputElement.getAttributeValue(RESOURCE);
			Integer id = ResourceUtil.findIDbyAmountResourceName(resourceName);
			// Convert RATE in kg/sol to rate in kg/millisol
			double rate = Double.parseDouble(inputElement.getAttributeValue(RATE)) / 1000.0;
			boolean ambient = ConfigHelper.getOptionalAttributeBool(inputElement, AMBIENT, false);
			
			process.addBaseInputResourceRate(id, rate, ambient);
		}

		// Get output resources.
		List<Element> outputNodes = processElement.getChildren(OUTPUT);
		for (Element outputElement : outputNodes) {
			String resourceName = outputElement.getAttributeValue(RESOURCE);
			Integer id = ResourceUtil.findIDbyAmountResourceName(resourceName);
			// Convert RATE in kg/sol to rate in kg/millisol
			double rate = Double.parseDouble(outputElement.getAttributeValue(RATE)) / 1000.0;
			boolean waste = ConfigHelper.getOptionalAttributeBool(outputElement, WASTE, false);

			process.addBaseOutputResourceRate(id, rate, waste);
		}

		return process;
	}

	/**
	 * Finds a Building spec according to the name.
	 * 
	 * @param buildingType
	 * @return
	 */
	public ResourceProcessSpec getProcessSpec(String processName) {
		ResourceProcessSpec result = processSpecMap.get(processName.toLowerCase());
		if (result == null) {
			throw new IllegalArgumentException("Process Spec not known :" + processName);
		}
		return result;
	}
}
