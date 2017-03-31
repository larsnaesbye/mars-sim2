/**
 * Mars Simulation Project
 * CropType.java
 * @version 3.1.0 2017-03-31
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function.farming;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.mars_sim.msp.core.tool.Conversion;

/**
 * The CropType class is a type of crop.
 */
//2014-10-05 Added cropCategory
//2014-10-14 Added edibleBiomass. commented out ppf and photoperiod
public class CropType implements Serializable, Comparable<CropType> {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	/** TODO The name of the type of crop should be internationalizable. */
	private String name;
	/** The length of the crop type's growing phase. */
	private double growingTime;
	private double edibleBiomass; // the fresh basis Edible Biomass Productivity [ in gram per sq m per day ]
	private double edibleWaterContent;
	private double inedibleBiomass;
	/** Note: the Photosynthetic Photon Flux (PPF) is the amount of light needed [in micro mol per sq meter per second] */
	//private double ppf;
	/** Note: The Photoperiod is the number of hours of light needed [in hours per day] */
	//private double photoperiod;
	private double dailyPAR;
	/** The average harvest index (from 0 to 1) -- the ratio of the edible over the inedible part of the harvested crop [dimenionsion-less] */
	//private double harvestIndex;
	private Map<Integer, Phase> phases = new HashMap<>();
	/** The type of crop */
	private CropCategoryType cropCategoryType;


	/**
	 * Constructor.
	 * @param name - Name of the crop.
	 * @param growingTime Length of growing phase for crop in millisols.
	 * @param cropCategory The type of crop.
	 * @param edibleBiomass
	 * @param edibleWaterContent
	 * @param inedibleBiomass
	 * @param dailyPAR
	 * @param a map of phases
	 */
	public CropType(String name, double growingTime,
			CropCategoryType cropCategoryType, double edibleBiomass,
			double edibleWaterContent, double inedibleBiomass,
			double dailyPAR, Map<Integer, Phase> phases) {

		this.name = name;
		this.growingTime = growingTime;
		this.cropCategoryType = cropCategoryType;
		this.edibleBiomass = edibleBiomass;
		this.edibleWaterContent = edibleWaterContent;
		this.inedibleBiomass = inedibleBiomass;
		this.dailyPAR = dailyPAR;
		this.phases = phases;

	}


	/**
	 * Gets the crop type's name.
	 * @return name
	 */
	public String getName() {
		return name;//Conversion.capitalize(name);
	}

	/**
	 * Gets the length of the crop type's growing phase.
	 * @return crop type's growing time in millisols.
	 */
	public double getGrowingTime() {
		return growingTime;
	}

	/**
	* Gets the crop's category type.
	* @return cropCategoryType
	*/
	public CropCategoryType getCropCategoryType() {
		return cropCategoryType;
	}

	/**
	* Gets the edible biomass
	* @return crop's edible biomass (grams per m^2 per day)
	*/
	public double getEdibleBiomass() {
		return edibleBiomass;
	}

	/**
	* Gets the edible water content
	* @return crop's edible water content (grams per m^2 per day)
	*/
	public double getEdibleWaterContent() {
		//System.out.println(name + "'s water content in getEdibleWaterContent() is " + edibleWaterContent);
		return edibleWaterContent;
	}

	/**
	* Gets the inedible biomass
	* @return crop's inedible biomass (grams per m^2 per day)
	*/
	public double getInedibleBiomass() {
		return inedibleBiomass;
	}

	/**
	* Gets the daily PAR, the average amount of light needed per day in terms of daily Photosynthetically active radiation (PAR)
	* @return crop's daily PAR
	*/
	public double getDailyPAR() {
		return dailyPAR;
	}

	/**
	 * String representation of this cropType.
	 * @return The settlement and cropType's name.
	 */
	// 2014-12-09 Added toString()
	public String toString() {
		return Conversion.capitalize(name);
	}

	public Map<Integer, Phase> getPhases() {
		return phases;
	}

	/**
	 * Compares this object with the specified object for order.
	 * @param o the Object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object is less than,
	 * equal to, or greater than the specified object.
	 */
	// 2014-12-09 Added compareTo()
	public int compareTo(CropType c) {
		return name.compareToIgnoreCase(c.name);
	}


	public void destroy() {
		// TODO Auto-generated method stub

	}

}
