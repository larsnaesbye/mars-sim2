/*
 * Mars Simulation Project
 * FoodProduction.java
 * @date 2022-07-26
 * @author Manny Kung
 */
package com.mars_sim.core.structure.building.function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import com.mars_sim.core.equipment.Equipment;
import com.mars_sim.core.equipment.EquipmentFactory;
import com.mars_sim.core.equipment.EquipmentType;
import com.mars_sim.core.food.FoodProductionProcess;
import com.mars_sim.core.food.FoodProductionProcessInfo;
import com.mars_sim.core.food.FoodProductionUtil;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.process.ProcessItem;
import com.mars_sim.core.resource.ItemResourceUtil;
import com.mars_sim.core.resource.ItemType;
import com.mars_sim.core.resource.Part;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingException;
import com.mars_sim.core.structure.building.FunctionSpec;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.tool.MathUtils;

/**
 * A building function for food production.
 */
public class FoodProduction extends Function {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/* default logger. */
	private static final SimLogger logger = SimLogger.getLogger(FoodProduction.class.getName());

	private static final int SKILL_GAP = 1;

	// Why are we usign printer to do cooking ?
	private static final int PRINTER_ID = ItemResourceUtil.printerID;

	private static final double PROCESS_MAX_VALUE = 100D;

	private static final String CONCURRENT_PROCESSES = "concurrent-processes";

	// Data members.
	private int techLevel;
	private int numPrintersInUse;
	private int numMaxConcurrentProcesses;
	
	private List<FoodProductionProcess> processes;

	/**
	 * Constructor.
	 * 
	 * @param building the building the function is for.
	 * @param spec Spec of the function
	 * @throws BuildingException if error constructing function.
	 */
	public FoodProduction(Building building, FunctionSpec spec) {
		// Use Function constructor.
		super(FunctionType.FOOD_PRODUCTION, spec, building);

		techLevel = spec.getTechLevel();
		numMaxConcurrentProcesses = spec.getIntegerProperty(CONCURRENT_PROCESSES);
		numPrintersInUse = numMaxConcurrentProcesses;
		
		processes = new ArrayList<>();
	}

	/**
	 * Gets the value of the function for a named building type.
	 * 
	 * @param buildingType the building type.
	 * @param newBuilding  true if adding a new building.
	 * @param settlement   the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String buildingType, boolean newBuilding, Settlement settlement) {

		double result;

		FunctionSpec spec = buildingConfig.getFunctionSpec(buildingType, FunctionType.FOOD_PRODUCTION);

		int buildingTech = spec.getTechLevel();

		double demand = settlement.getAllAssociatedPeople().stream()
						.mapToInt(p -> p.getSkillManager().getSkillLevel(SkillType.COOKING))
						.sum();

		double supply = 0D;
		int highestExistingTechLevel = 0;
		boolean removedBuilding = false;
		for(Building building : settlement.getBuildingManager().getBuildingSet(FunctionType.FOOD_PRODUCTION)) {
			if (!newBuilding && building.getBuildingType().equalsIgnoreCase(buildingType) && !removedBuilding) {
				removedBuilding = true;
			} else {
				FoodProduction manFunction = building.getFoodProduction();
				int tech = manFunction.techLevel;
				double processes = manFunction.getNumPrintersInUse();
				double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
				supply += (tech * tech) * processes * wearModifier;

				if (tech > highestExistingTechLevel) {
					highestExistingTechLevel = tech;
				}
			}
		}

		double baseFoodProductionValue = demand / (supply + 1D);

		double processes = spec.getIntegerProperty(CONCURRENT_PROCESSES);
		double foodProductionValue = (buildingTech * buildingTech) * processes;

		result = foodProductionValue * baseFoodProductionValue;

		// If building has higher tech level than other buildings at settlement,
		// add difference between best food production processes.
		if (buildingTech > highestExistingTechLevel) {
			double bestExistingProcessValue = 0D;
			if (highestExistingTechLevel > 0D) {
				bestExistingProcessValue = getBestFoodProductionProcessValue(highestExistingTechLevel, settlement);
			}
			double bestBuildingProcessValue = getBestFoodProductionProcessValue(buildingTech, settlement);
			double processValueDiff = bestBuildingProcessValue - bestExistingProcessValue;
			processValueDiff = MathUtils.between(processValueDiff, 0, PROCESS_MAX_VALUE);

			result += processValueDiff;
		}

		return result;
	}

	/**
	 * Gets the best food production process value for a given food production tech
	 * level at a settlement.
	 * 
	 * @param techLevel  the food production tech level.
	 * @param settlement the settlement
	 * @return best food production process value.
	 */
	private static double getBestFoodProductionProcessValue(int techLevel, Settlement settlement) {

		double result = 0D;

		Iterator<FoodProductionProcessInfo> i = FoodProductionUtil.getAllFoodProductionProcesses().iterator();
		while (i.hasNext()) {
			FoodProductionProcessInfo process = i.next();
			if (process.getTechLevelRequired() <= techLevel) {
				double value = FoodProductionUtil.getFoodProductionProcessValue(process, settlement);
				if (value > result) {
					result = value;
				}
			}
		}

		return result;
	}

	/**
	 * Gets the food production tech level of the building.
	 * 
	 * @return tech level.
	 */
	public int getTechLevel() {
		return techLevel;
	}

	/**
	 * Gets the maximum concurrent food production processes supported by the
	 * building.
	 *
	 * @return maximum concurrent processes.
	 */
	public int getMaxProcesses() {
		return numMaxConcurrentProcesses;
	}

	/**
	 * Gets the total food production processes currently in this building.
	 * 
	 * @return total process number.
	 */
	public int getCurrentTotalProcesses() {
		return processes.size();
	}

	/**
	 * Gets a list of the current food production processes.
	 * 
	 * @return unmodifiable list of processes.
	 */
	public List<FoodProductionProcess> getProcesses() {
		return Collections.unmodifiableList(processes);
	}

	/**
	 * Adds a new food production process to the building.
	 * 
	 * @param process the new food production process.
	 * @throws BuildingException if error adding process.
	 */
	public void addProcess(FoodProductionProcess process) {

		if (process == null) {
			throw new IllegalArgumentException("process is null");
		}

		if (getCurrentTotalProcesses() > numPrintersInUse) {
			logger.info(getBuilding(), 20_000,
					getBuilding()
					+ ": " + getCurrentTotalProcesses() + " concurrent processes.");
			logger.info(getBuilding(), 20_000,
					getBuilding()
					+ ": " + numPrintersInUse + " 3D-printer(s) installed for use."
					+ "");
			logger.info(getBuilding(), 20_000,
					getBuilding()
					+ ": " + (numMaxConcurrentProcesses-numPrintersInUse)
					+ " 3D-printer slot(s) available."
					+ "");
			return;
		}
		
		processes.add(process);

		// Consume inputs.
		for (ProcessItem item : process.getInfo().getInputList()) {
			if (ItemType.AMOUNT_RESOURCE.equals(item.getType())) {
				int id = ResourceUtil.findIDbyAmountResourceName(item.getName());
				getBuilding().retrieveAmountResource(id, item.getAmount());
			} else if (ItemType.PART.equals(item.getType())) {
				int id = ItemResourceUtil.findIDbyItemResourceName(item.getName());
				getBuilding().retrieveItemResource(id, (int) item.getAmount());
			} else
				logger.log(getBuilding(), Level.SEVERE, 20_000,
					getBuilding()
					+ " food production process input: " + item.getType() + " not a valid type.");
			
		}

		// Log food production process starting.
		logger.log(getBuilding(), Level.FINEST, 20_000,
				getBuilding()
				+ " starting food production process: " + process.getInfo().getName());	
	}

	@Override
	public double getCombinedPowerLoad() {
		double result = 0D;
		Iterator<FoodProductionProcess> i = processes.iterator();
		while (i.hasNext()) {
			FoodProductionProcess process = i.next();
			if (process.getProcessTimeRemaining() > 0D)
				result += process.getInfo().getPowerRequired();
		}
		return result;
	}

	@Override
	public double getPoweredDownPowerRequired() {
		return getCombinedPowerLoad();
	}

	@Override
	public boolean timePassing(ClockPulse pulse) {
		boolean valid = isValid(pulse);
		if (valid) {
			if (pulse.isNewSol()) {
				// Check once a sol only
				checkPrinters();
			}

			List<FoodProductionProcess> finishedProcesses = new ArrayList<>();
	
			Iterator<FoodProductionProcess> i = processes.iterator();
			while (i.hasNext()) {
				FoodProductionProcess process = i.next();
				process.addProcessTime(pulse.getElapsed());
	
				if ((process.getProcessTimeRemaining() == 0D) && (process.getWorkTimeRemaining() == 0D)) {
					finishedProcesses.add(process);
				}
			}
	
			// End all processes that are done.
			Iterator<FoodProductionProcess> j = finishedProcesses.iterator();
			while (j.hasNext()) {
				endFoodProductionProcess(j.next(), false);
			}
		}
		return valid;
	}

	/**
	 * Checks if food production function currently requires food production work.
	 * 
	 * @param skill the person's materials science skill level.
	 * @return true if food production work.
	 */
	public boolean requiresWork(int skill) {
		boolean result = false;

		if (numPrintersInUse > getCurrentTotalProcesses())
			result = true;
		else {
			Iterator<FoodProductionProcess> i = processes.iterator();
			while (i.hasNext()) {
				FoodProductionProcess process = i.next();
				boolean workRequired = (process.getWorkTimeRemaining() > 0D);
				// Allow a low material science skill person to have access to do the next level skill process
				boolean skillRequired = (process.getInfo().getSkillLevelRequired() <= skill + SKILL_GAP);
				if (workRequired && skillRequired)
					result = true;
			}
		}

		return result;
	}

	private void despositOutputs(FoodProductionProcess process) {
		Settlement settlement = building.getSettlement();

		// Produce outputs.
		for(var item : process.getInfo().getOutputList()) {
			if (FoodProductionUtil.getProcessItemValue(item, settlement, true) > 0D) {
				int outputId = -1;
				double outputAmount = item.getAmount();
				switch(item.getType()) {
					case AMOUNT_RESOURCE: {
						// Produce amount resources.
						outputId = ResourceUtil.findIDbyAmountResourceName(item.getName());
						double capacity = settlement.getAmountResourceRemainingCapacity(outputId);
						if (outputAmount> capacity) {
							double overAmount = item.getAmount() - capacity;
							logger.severe(getBuilding(), "Not enough storage capacity to store " + overAmount + " of " + item.getName()
									+ " from " + process.getInfo().getName());
							outputAmount = capacity;
						}
						settlement.storeAmountResource(outputId, outputAmount);

						// Record the food produced
						settlement.addOutput(outputId, outputAmount, process.getTotalWorkTime());
					} break;

					case PART: {
						// Produce parts.
						Part part = (Part) ItemResourceUtil.findItemResource(item.getName());
						outputId = part.getID();
						int num = (int)outputAmount;
						double mass = num * part.getMassPerItem();
						double capacity = settlement.getCargoCapacity();
						if (mass <= capacity) {
							settlement.storeItemResource(outputId, num);
						}
					} break;

					case EQUIPMENT: {
						// Produce equipment.
						var equipmentType = EquipmentType.convertName2Enum(item.getName());
						int number = (int) outputAmount;
						for (int x = 0; x < number; x++) {
							EquipmentFactory.createEquipment(equipmentType, settlement);
						}
					} break;

					case BIN:
					case VEHICLE: {
						throw new IllegalArgumentException("Cannot create " + item.getType()
										+ " from Food Process");
					}
				}
			}
		}

		// Record process finish
		settlement.recordProcess(process.getInfo(), "Food", building);
	}

	private void returnInputs(FoodProductionProcess process) {
		var settlement = getBuilding().getAssociatedSettlement();

		// Premature end of process. Return all input materials.
		Iterator<ProcessItem> j = process.getInfo().getInputList().iterator();
		while (j.hasNext()) {
			ProcessItem item = j.next();
			if (FoodProductionUtil.getProcessItemValue(item, settlement, false) > 0D) {
				if (ItemType.AMOUNT_RESOURCE.equals(item.getType())) {
					// Produce amount resources.
					int id = ResourceUtil.findIDbyAmountResourceName(item.getName());
					double amount = item.getAmount();
					double capacity = settlement.getAmountResourceRemainingCapacity(id);
					if (item.getAmount() > capacity) {
						double overAmount = item.getAmount() - capacity;
						logger.severe("Not enough storage capacity to store " + overAmount + " of " + item.getName()
								+ " from " + process.getInfo().getName() + " at " + settlement.getName());
						amount = capacity;
					}
					settlement.storeAmountResource(id, amount);
					
				} else if (ItemType.PART.equals(item.getType())) {
					// Produce parts.
					Part part = (Part) ItemResourceUtil.findItemResource(item.getName());
					int id = part.getID();
					double mass = item.getAmount() * part.getMassPerItem();
					double capacity = settlement.getCargoCapacity();
					if (mass <= capacity) {
						settlement.storeItemResource(id, (int) item.getAmount());
					}
				} else if (ItemType.EQUIPMENT.equals(item.getType())) {
					// Produce equipment.
					String equipmentType = item.getName();
					int number = (int) item.getAmount();
					for (int x = 0; x < number; x++) {
						Equipment equipment = EquipmentFactory.createEquipment(equipmentType,
								settlement);
						unitManager.addUnit(equipment);
					}
				}
				else
					throw new IllegalStateException(
							"FoodProduction.addProcess(): output: invalid type:" + item.getType());
			}
		}
	}

	/**
	 * Ends a food production process.
	 * 
	 * @param process   the process to end.
	 * @param premature true if the process has ended prematurely.
	 * @throws BuildingException if error ending process.
	 */
	public void endFoodProductionProcess(FoodProductionProcess process, boolean premature) {
	
		if (!premature) {
			// Produce outputs.
			despositOutputs(process);
		}
		else {
			returnInputs(process);
		}

		processes.remove(process);
		
		// Log process ending.
		logger.log(getBuilding(), Level.FINEST, 20_000,
				"Ending food production process: " + process.getInfo().getName());
	}

	@Override
	public double getMaintenanceTime() {
		double result = getCombinedPowerLoad() * .25;
		// Add maintenance for tech level.
		result *= techLevel * .5;
		// Add maintenance for num of printers in use.
		result *= numPrintersInUse * .5;

		return result;
	}

	/**
	 * Check if enough 3D printer(s) are supporting the manufacturing
	 * processes
	 * @param pulse
	 */
	private void checkPrinters() {
		// Gets the available number of printers in storage
		int numAvailable = building.getSettlement().getItemResourceStored(PRINTER_ID);

		// Malfunction of a 3D-printer should trigger this
		// NOTE: it's reasonable to create a settler's task to install 
		// a 3-D printer manually over a period of time
		if (numPrintersInUse < numMaxConcurrentProcesses) {
			int deficit = numMaxConcurrentProcesses - numPrintersInUse;
			logger.info(getBuilding(), 20_000,
					getBuilding() + " - "
					+ numAvailable
					+ " 3D-printer(s) in storage.");
			logger.info(getBuilding(), 20_000,
					getBuilding() + " - "
					+ numPrintersInUse
					+ " 3D-printer(s) in use.");

			if (deficit > 0 && numAvailable > 0) {
				int size = Math.min(numAvailable, deficit);
				for (int i=0; i<size; i++) {
					numPrintersInUse++;
					numAvailable--;
					int lacking = building.getSettlement().retrieveItemResource(PRINTER_ID, 1);
					if (lacking > 0) {
						logger.info(getBuilding(), 20_000,
								"No 3D-printer available for " + getBuilding() + ".");
					}
				}

				logger.info(getBuilding(), 20_000,
						getBuilding() + " - "
						+ size
						+ " 3D-printer(s) just installed.");
			}
		}

        // NOTE: if not having enough printers,
		// determine how to use GoodsManager to push for making new 3D printers
	}


	public int getNumPrintersInUse() {
		return numPrintersInUse;
	}
	
	@Override
	public void destroy() {
		super.destroy();

		Iterator<FoodProductionProcess> i = processes.iterator();
		while (i.hasNext()) {
			i.next().destroy();
		}

	}
}
