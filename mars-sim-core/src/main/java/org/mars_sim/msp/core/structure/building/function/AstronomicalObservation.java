/**
 * Mars Simulation Project
 * AstronomicalObservation.java
 * @version 3.2.0 2021-06-20
 * @author Sebastien Venot
 */
package org.mars_sim.msp.core.structure.building.function;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingConfig;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.FunctionSpec;
import org.mars_sim.msp.core.time.MarsTime;

/**
 * A building function for observing astronomical objects.
 */
public class AstronomicalObservation extends Function {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final Logger logger = Logger.getLogger(AstronomicalObservation.class.getName());

	// Data members
	private double powerRequired;
	private int techLevel;
	private int observatoryCapacity;
	private int observerNum;

	/**
	 * Constructor.
	 * 
	 * @param building the building the function is for.
	 * @param spec Function details
	 * @throws BuildingException if error creating building function.
	 */
	public AstronomicalObservation(Building building, FunctionSpec spec) {
		// Use function constructor.
		super(FunctionType.ASTRONOMICAL_OBSERVATION, spec, building);

		powerRequired = spec.getDoubleProperty(BuildingConfig.POWER_REQUIRED);
		techLevel = spec.getTechLevel();
		observatoryCapacity = spec.getCapacity();
	}

	/**
	 * Gets the amount of power required when function is at full power.
	 * 
	 * @return power (kW)
	 */
	@Override
	public double getFullPowerRequired() {
		return powerRequired;
	}

	/**
	 * Adds a new observer to the observatory.
	 * 
	 * @throws Exception if observatory is already at capacity.
	 */
	public void addObserver() {
		observerNum++;
		if (observerNum > observatoryCapacity) {
			observerNum = observatoryCapacity;
			logger.log(Level.SEVERE, "addObserver(): " + "Observatory is already full of observers.");
			throw new IllegalStateException("Observatory is already full of observers.");
		}
	}

	/**
	 * Removes an observer from the observatory.
	 * 
	 * @throws Exception if no observers currently in observatory.
	 */
	public void removeObserver() {
		observerNum--;
		if (observerNum < 0) {
			observerNum = 0;
			logger.log(Level.SEVERE, "removeObserver(): " + "Observatory is already empty of observers.");
			throw new IllegalStateException("Observatory is already empty of observers.");
		}
	}

	/**
	 * Gets the current number of observers in the observatory.
	 * 
	 * @return number of observers.
	 */
	public int getObserverNum() {
		return observerNum;
	}

	/**
	 * Gets the capacity for observers in the observatory.
	 * 
	 * @return capacity.
	 */
	public int getObservatoryCapacity() {
		return observatoryCapacity;
	}

	/**
	 * Gets the technology level of the observatory.
	 * 
	 * @return technology level.
	 */
	public int getTechnologyLevel() {
		return techLevel;
	}

	/**
	 * Gets the value of the function for a named building type.
	 * 
	 * @param type  the building type.
	 * @param newBuilding  true if adding a new building.
	 * @param settlement   the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String type, boolean newBuilding, Settlement settlement) {

		double observatoryDemand = 0D;
		ScienceType astronomyScience = ScienceType.ASTRONOMY;

		// Determine settlement demand for astronomical observatories.
		SkillType astronomySkill = astronomyScience.getSkill();
		for(Person p : settlement.getAllAssociatedPeople()) {
			observatoryDemand += p.getSkillManager().getSkillLevel(astronomySkill);
		}

		// Determine existing settlement supply of astronomical observatories.
		double observatorySupply = 0D;
		boolean removedBuilding = false;
		for(Building building : settlement.getBuildingManager().getBuildingSet(FunctionType.ASTRONOMICAL_OBSERVATION)) {
			if (!newBuilding && building.getBuildingType().equalsIgnoreCase(type) && !removedBuilding) {
				removedBuilding = true;
			} else {
				AstronomicalObservation astroFunction = building.getAstronomicalObservation();
				int techLevel = astroFunction.techLevel;
				int observatorySize = astroFunction.observatoryCapacity;
				double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
				observatorySupply += techLevel * observatorySize * wearModifier;
			}
		}

		// Determine existing settlement value for astronomical observatories.
		double existingObservatoryValue = observatoryDemand / (observatorySupply + 1D);

		// Determine settlement value for this building's astronomical observatory
		// function.
		FunctionSpec fSpec = buildingConfig.getFunctionSpec(type, FunctionType.ASTRONOMICAL_OBSERVATION);
		int techLevel = fSpec.getTechLevel();
		int observatorySize = fSpec.getCapacity();
		int buildingObservatorySupply = techLevel * observatorySize;

		double result = buildingObservatorySupply * existingObservatoryValue;

		// Subtract power usage cost per sol.
		double power = fSpec.getDoubleProperty(BuildingConfig.POWER_REQUIRED);
		double powerPerSol = power * MarsTime.HOURS_PER_MILLISOL * 1000D;
		double powerValue = powerPerSol * settlement.getPowerGrid().getPowerValue();
		result -= powerValue;

		if (result < 0D)
			result = 0D;

		return result;
	}

	@Override
	public double getMaintenanceTime() {

		double result = 0D;

		// Add maintenance for tech level.
		result += techLevel * 10D;

		// Add maintenance for observer capacity.
		result += observatoryCapacity * 10D;

		return result;
	}
}
