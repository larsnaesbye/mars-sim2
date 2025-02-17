/*
 * Mars Simulation Project
 * ResourceProcessSpec.java
 * @date 2021-08-20
 * @author Barry Evans
 */
package com.mars_sim.core.resourceprocess;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The ResourceProcessSpec class represents the specification of a process of converting one set of
 * resources to another. This object is shared amongst ResourceProcess of the same type.
 */
public class ResourceProcessSpec implements Serializable{

	private static final long serialVersionUID = 1L;

	private static final double MIN_PERC = 0.25;

	private boolean defaultOn;
	private String name;
	private double powerRequired;

	private Map<Integer, Double> baseInputRates;
	private Map<Integer, Double> baseOutputRates;
	private Map<Integer, Double> minimumInputs;

	// Cache some aggregate values
	private Set<Integer> ambientResources;
	private Set<Integer> wasteResources;

	/** How long does it take to complete the process*/
	private int processTime = 100;

	/** The work time required to toggle this process on or off. */
	private int workTime = 10;

	/**
	 * Constructor.
	 * 
	 * @param name
	 * @param powerRequired
	 * @param processTime
	 * @param workTime
	 * @param defaultOn
	 */
	ResourceProcessSpec(String name, double powerRequired, int processTime, int workTime, boolean defaultOn) {
		this.defaultOn = defaultOn;
		this.name = name;
		this.baseInputRates = new HashMap<>();
		this.baseOutputRates = new HashMap<>();
		this.minimumInputs = new HashMap<>();

		this.wasteResources = new HashSet<>();
		this.ambientResources = new HashSet<>();
		this.powerRequired = powerRequired;
		this.processTime = processTime;
		this.workTime = workTime;
	}

	/**
	 * Adds a base input resource rate.
	 *
	 * @param resource the amount resource.
	 * @param rate     base input resource rate (kg/millisol)
	 * @param ambient  is resource from available from surroundings? (air)
	 */
	void addBaseInputResourceRate(Integer resource, double rate, boolean ambient) {
		if (ambient) {
			ambientResources.add(resource);
		}
		else {
			double minAmount = rate * (processTime/1000D) * MIN_PERC;
			minimumInputs.put(resource, minAmount);
		}

		baseInputRates.put(resource, rate);
	}

	/**
	 * Adds a base output resource rate.
	 *
	 * @param resource the amount resource.
	 * @param rate     base output resource rate (kg/millisol)
	 * @param waste    is resource waste material not to be stored?
	 */
	void addBaseOutputResourceRate(Integer resource, double rate, boolean waste) {
		if (waste) {
			wasteResources.add(resource);
		}

		baseOutputRates.put(resource, rate);
	}

	public String getName() {
		return name;
	}

	/**
	 * Gets a set of input resources for this process.
	 * 
	 * @return
	 */
	public Set<Integer> getInputResources() {
		return baseInputRates.keySet();
	}

	/**
	 * Gets the base input resource rate for a given resource.
	 *
	 * @return rate in kg/millisol.
	 */
	public double getBaseInputRate(Integer resource) {
		return baseInputRates.get(resource);
	}

	/**
	 * Gets the minimum input resource rate for a given resource.
	 *
	 * @return Amount required
	 */
	public Map<Integer, Double> getMinimumInputs() {
		return minimumInputs;
	}

	/**
	 * Checks if resource is an ambient input.
	 *
	 * @param resource the resource to check.
	 * @return true if ambient resource.
	 */
	public boolean isAmbientInputResource(Integer resource) {
		return ambientResources.contains(resource);
	}

	/**
	 * Gets a set of output resources from this process.
	 * 
	 * @return
	 */
	public Set<Integer> getOutputResources() {
		return baseOutputRates.keySet();
	}
	
	/**
	 * Gets the base output resource rate for a given resource.
	 *
	 * @return rate in kg/millisol.
	 */
	public double getBaseOutputRate(Integer resource) {
		var value = baseOutputRates.get(resource);
		if (value != null) {
			return value.doubleValue();
		}
		return 0D;
	}

	/**
	 * Checks if resource is a waste output.
	 *
	 * @param resource the resource to check.
	 * @return true if waste output.
	 */
	public boolean isWasteOutputResource(Integer resource) {
		return wasteResources.contains(resource);
	}

	/**
	 * Gets the power required for this process.
	 * 
	 * @return
	 */
	public double getPowerRequired() {
		return powerRequired;
	}

	/**
	 * Gets the time to complete this process.
	 * 
	 * @return Number of msol
	 */
	public int getProcessTime() {
		return processTime ;
	}

	/**
	 * Gets the work time needed on this process.
	 * 
	 * @return
	 */
	public int getWorkTime() {
		return workTime;
	}

	public boolean getDefaultOn() {
		return defaultOn;
	}
}
