/*
 * Mars Simulation Project
 * Computation.java
 * @date 2023-11-30
 * @author Manny Kung
 */
package com.mars_sim.core.structure.building.function;

import java.util.HashMap;
import java.util.Map;

import com.mars_sim.core.UnitEventType;
import com.mars_sim.core.computing.ComputingTask;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingException;
import com.mars_sim.core.structure.building.FunctionSpec;
import com.mars_sim.core.structure.building.SourceSpec;
import com.mars_sim.core.time.ClockPulse;

/**
 * The Computation class is a building function for generating computational power.
 */
public class Computation extends Function {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(Computation.class.getName());
	
	// Configuration properties
	private static final double ENTROPY_FACTOR = .001;
	private static final double WASTE_HEAT_PERCENT = .1;
	
	private static final String COMPUTING_UNIT = "computing-unit";
	private static final String POWER_DEMAND = "power-demand";
	private static final String COOLING_DEMAND = "cooling-demand";

	/** The amount of entropy in the system. */
	private final double maxEntropy;
	/** The highest possible available amount of computing resources [in CUs]. */
	private final double peakCU;
	
	/** The amount of entropy in the system. */
	private double entropy;
	/** The amount of computing resources capacity currently available [in CUs]. */
	private double currentCU;
	/** The power load in kW for each running CU [in kW/CU]. */
	private double powerDemand;
	/** The power load in kW needed for cooling each running CU [in kW/CU]. */
	private double coolingDemand;
	/** The combined power demand for each running CU [in kW/CU]. */
	private double combinedkW;
	/** The power demand for each non-load CU [in kW/CU] - Assume 10% of full load. */
	private double nonLoadkW;
	/** The schedule demand [in CUs] for the current mission sol. */
	private Map<Integer, Double> todayDemand;

	/**
	 * Constructor.
	 * 
	 * @param building the building this function is for.
	 * @param spec Specification of the Computing Function
	 * @throws BuildingException if error in constructing function.
	 */
	public Computation(Building bldg, FunctionSpec spec) {
		// Call Function constructor.
		super(FunctionType.COMPUTATION, spec, bldg);
		
		peakCU = spec.getDoubleProperty(COMPUTING_UNIT);
		maxEntropy = peakCU;
		
		currentCU = peakCU; 
		powerDemand = spec.getDoubleProperty(POWER_DEMAND);
		coolingDemand = spec.getDoubleProperty(COOLING_DEMAND);	
		
		combinedkW = coolingDemand + powerDemand;
		// Assume 10% of full load
		nonLoadkW = 0.1 * combinedkW;
		
		todayDemand = new HashMap<>();
	}

	/**
	 * Gets the value of the function for a named building.
	 * 
	 * @param type the building type.
	 * @param newBuilding  true if adding a new building.
	 * @param settlement   the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String type, boolean newBuilding, Settlement settlement) {

		double demand = settlement.getPowerGrid().getRequiredPower();

		double supply = 0D;
		boolean removedBuilding = false;
		for (Building building : settlement.getBuildingManager().getBuildingSet(FunctionType.COMPUTATION)) {
			if (!newBuilding && building.getBuildingType().equalsIgnoreCase(type) && !removedBuilding) {
				removedBuilding = true;
			} else {
				Computation com = building.getComputation();
				double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
				supply += com.getCurrentCU() * wearModifier;
			}
		}

		double existingPowerValue = demand / (supply + 1D);

		double powerSupply = buildingConfig.getHeatSources(type).stream()
								.mapToDouble(SourceSpec::getCapacity).sum();

		return powerSupply * existingPowerValue;
	}

	/**
	 * Gets the computing unit capacity [in CU].
	 * 
	 * @return
	 */
	public double getCurrentCU() {
		return currentCU;
	}

	/**
	 * Gets the peak available computing units [in CU].
	 * 
	 * @return
	 */
	public double getPeakCU() {
		return peakCU;
	}
	
	/**
	 * Dumps the excess heat from server equipment.
	 * 
	 * @param heatGenerated
	 */
	public void dumpExcessHeat(double heatGenerated) {
		building.dumpExcessHeat(heatGenerated);
	}
	
	/**
	 * Gets the power demand [in kW].
	 * 
	 * @return
	 */
	public double getPowerDemand() {
		return powerDemand;
	}

	/**
	 * Gets the cooling demand [in kW].
	 * 
	 * @return
	 */
	public double getCoolingDemand() {
		return coolingDemand;
	}

	/**
	 * Schedules for a computing tasks.
	 * 
	 * @param computingTask
	 * @return
	 */
	public boolean scheduleTask(ComputingTask computingTask) {
		double demand = computingTask.getComputingPower();
		int beginningMSol = computingTask.getStartTime();
		int endMSol = beginningMSol + computingTask.getDuration();
		return scheduleTask(demand, beginningMSol, endMSol);
	}
	
	/**
	 * Schedules for a computing task.
	 * 
	 * @param needed the CUs needed per msol
	 * @param beginningMSol the start msol
	 * @param endMSol the end msol
	 * @return
	 */
	public boolean scheduleTask(double needed, int beginningMSol, int endMSol) {
		int duration = endMSol - beginningMSol;
		if (duration < 0)
			duration = endMSol + 1000 - beginningMSol;
		double existing = 0;
		// Test to see if the assigned duration has enough resources
		for (int i = 0; i < duration; i++) {
			int sol = i + beginningMSol;
			if (sol > 999) {
				sol = sol - 1000;
			}
			if (todayDemand.containsKey(sol)) {
				existing = todayDemand.get(sol);
			}
			double available = peakCU - existing - needed;
			if (available < 0) {
				logger.info(getBuilding(), 30_000L, "scheduleTask::available: " + available);
				// Need to make sure each msol has enough resources
				return false;
			}
		}

		// Now the actual scheduling
		for (int i = 0; i < duration; i++) {
			int sol = i + beginningMSol;
			if (sol > 999) {
				sol = sol - 1000;
			}
			if (todayDemand.containsKey(sol)) {
				existing = todayDemand.get(sol);
			}
			todayDemand.put(sol, existing + needed);
			
			// Increase the entropy
			increaseEntropy(needed * ENTROPY_FACTOR);
		}

		return true;
	}
	
	/**
	 * Does this computing center have the resources to schedule for a computing task ?
	 * 
	 * @param needed
	 * @param beginningMSol
	 * @param endMSol
	 * @return
	 */
	public boolean canScheduleTask(double needed, int beginningMSol, int endMSol) {
		int duration = endMSol - beginningMSol;
		if (duration < 0)
			duration = endMSol + 1000 - beginningMSol;
		double existing = 0;
		double available = 0;
		// Test to see if the assigned duration has enough resources
		for (int i = 0; i < duration; i++) {
			int sol = i + beginningMSol;
			if (sol > 999) {
				sol = sol - 1000;
			}
			if (todayDemand.containsKey(sol)) {
				existing = todayDemand.get(sol);
			}
			available = peakCU - existing - needed;
		}
		if (available < 0) {
			logger.info(getBuilding(), 30_000L, "canScheduleTask::available: " + available);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Returns the evaluation score if scheduling for a computing task for a prescribed period of time. 
	 * 
	 * @param needed CU(s) per millisol
	 * @param beginningMSol
	 * @param endMSol
	 * @return
	 */
	public double evaluateScheduleTask(double needed, int beginningMSol, int endMSol) {
		int duration = endMSol - beginningMSol;
		if (duration < 0)
			duration = endMSol + 1000 - beginningMSol;
		double score = 0;
		double existing = 0;
		// Test to see if the assigned duration has enough resources
		for (int i = 0; i < duration; i++) {
			int sol = i + beginningMSol;
			if (sol > 999) {
				sol = sol - 1000;
			}
			if (todayDemand.containsKey(sol)) {
				existing = todayDemand.get(sol);
			}
			double available = peakCU - existing - needed;
			if (available < 0) {
				logger.info(getBuilding(), 30_000L, "evaluateScheduleTask::available: " + available);
				return 0;
			}
			
			score += available;
		}
		
		score = score * getEntropyPenalty();
		
		return score;
	}
	
	/**
	 * Sets the computing units or resources to a new value and fires the unit event type alert.
	 * 
	 * @param value
	 */
	public void setCU(double value) {
		double cu = Math.round(value * 100_000.0) / 100_000.0;
		if (currentCU != cu) {
			currentCU = cu;
			building.getSettlement().fireUnitUpdate(UnitEventType.CONSUMING_COMPUTING_EVENT);
		}
	}
	
	/**
	 * Time passing for the building.
	 * 
	 * @param deltaTime amount of time passing (in millisols)
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		boolean valid = isValid(pulse);
		if (valid) {
	
			int msol = pulse.getMarsTime().getMillisolInt();
			
			if (pulse.isNewMSol()) {
				
				increaseEntropy(pulse.getElapsed() * ENTROPY_FACTOR * currentCU / 10);
	
				double newDemand = 0;
				
				// Delete past demand on previous sol
				if (msol - 1 > 0 && todayDemand.containsKey(msol - 1)) {
					todayDemand.remove(msol - 1);
				}
				// Delete past demand on the sol before yestersol 
				if (msol - 2 > 0 && todayDemand.containsKey(msol - 2)) {
					todayDemand.remove(msol - 2);
				}
				
				if (todayDemand.containsKey(msol)) {
					newDemand = todayDemand.get(msol);
				}
				if (newDemand > 0) {
					// Updates the CUs
					setCU(peakCU - newDemand); 
				}
				else {
					setCU(peakCU);
				}

				// Notes: 
				// if it falls below 10%, flash yellow
				// if it falls below 0%, flash red
				
				double fullPower = getFullPowerRequired();
				double heat = fullPower * WASTE_HEAT_PERCENT;
				// Dump the generated heat into the building to raise the room temperature
				dumpExcessHeat(heat);
			}
		}
		return valid;
	}
	
	/**
	 * Returns the percent of usage of computing resources.
	 * 
	 * @return
	 */
	public double getUsagePercent() {
		return (peakCU - currentCU)/peakCU * 100.0;
	}	
	
	/**
	 * Gets the minimum entropy (a negative number).
	 * 
	 * @return
	 */
	public double getMinEntropy() {
		return -0.5 * maxEntropy;
	}
	
	/**
	 * Reduces the entropy.
	 * 
	 * @param the suggested value of entropy to be reduced
	 * @return the final value of entropy being reduced
	 */
	public double reduceEntropy(double value) {
		double oldEntropy = entropy;
		double diff = entropy - value;
		
		if (diff < getMinEntropy()) {
			// Note that entropy can become negative
			// This means that the system has been tuned up
			// to perform very well
			diff = getMinEntropy();
			entropy = diff + value;

		}
		else
			entropy -= value;
		
		return oldEntropy - entropy;
	}
	
	/**
	 * Increases the entropy.
	 * 
	 * @param value
	 */
	public void increaseEntropy(double value) {
		entropy += value;
	}
	
	/**
	 * Gets the penalty factor due to entropy.
	 * Note: it's bad if negative
	 * 
	 * @return
	 */
	public double getEntropyPenalty() {
		return 1 - entropy / maxEntropy;
	}
	
	/**
	 * Gets the current entropy.
	 * 
	 * @return
	 */
	public double getEntropy() {
		return entropy;
	}
	
	/**
	 * Gets the entropy per CU in this node.
	 * 
	 * @return
	 */
	public double getEntropyPerCU() {
		return entropy/currentCU;
	}
	
	/**
	 * Gets the amount of power required, based on the current load.
	 *
	 * @return power (kW) default zero
	 */
	@Override
	public double getFullPowerRequired() {
		double load = peakCU - currentCU;
		double nonLoad = currentCU;
		// Note: Should entropy also increase the power required to run the node ?
		// When entropy is negative, it should reduce or save power
		return (load + nonLoadkW * nonLoad) * combinedkW;
	}
	
	@Override
	public void destroy() {
		todayDemand.clear();
		todayDemand = null;
		super.destroy();
	}

}
