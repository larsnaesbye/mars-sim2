/*
 * Mars Simulation Project
 * Manufacture.java
 * @date 2022-07-26
 * @author Scott Davis
 */
package com.mars_sim.core.structure.building.function;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import com.mars_sim.core.Unit;
import com.mars_sim.core.UnitType;
import com.mars_sim.core.equipment.BinFactory;
import com.mars_sim.core.equipment.Equipment;
import com.mars_sim.core.equipment.EquipmentFactory;
import com.mars_sim.core.equipment.EquipmentType;
import com.mars_sim.core.goods.Good;
import com.mars_sim.core.goods.GoodsUtil;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.malfunction.Malfunctionable;
import com.mars_sim.core.manufacture.ManufactureProcess;
import com.mars_sim.core.manufacture.ManufactureProcessInfo;
import com.mars_sim.core.manufacture.ManufactureUtil;
import com.mars_sim.core.manufacture.Salvagable;
import com.mars_sim.core.manufacture.SalvageProcess;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.resource.AmountResource;
import com.mars_sim.core.resource.ItemResourceUtil;
import com.mars_sim.core.resource.ItemType;
import com.mars_sim.core.resource.Part;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingException;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.FunctionSpec;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.tool.MathUtils;
import com.mars_sim.core.tool.RandomUtil;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.core.vehicle.VehicleFactory;
import com.mars_sim.core.vehicle.VehicleType;

/**
 * A building function for manufacturing.
 */
public class Manufacture extends Function {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(Manufacture.class.getName());
	
	private static final int SKILL_GAP = 1;

	private static final int PRINTER_ID = ItemResourceUtil.printerID;

	private static final double PROCESS_MAX_VALUE = 100D;

	private static final String CONCURRENT_PROCESSES = "concurrent-processes";

	// Data members.
	private int techLevel;
	private int numPrintersInUse;
	private final int numMaxConcurrentProcesses;

	private List<ManufactureProcess> processes;
	private List<SalvageProcess> salvages;

	// NOTE: create a map to show which process has a 3D printer in use and which doesn't

	/**
	 * Constructor.
	 *
	 * @param building the building the function is for.
	 * @param spec Details of teh Funciton at hand
	 * @throws BuildingException if error constructing function.
	 */
	public Manufacture(Building building, FunctionSpec spec) {
		// Use Function constructor.
		super(FunctionType.MANUFACTURE, spec, building);

		techLevel = spec.getTechLevel();
		numMaxConcurrentProcesses = spec.getIntegerProperty(CONCURRENT_PROCESSES);
		numPrintersInUse = numMaxConcurrentProcesses;

		processes = new CopyOnWriteArrayList<>();
		salvages = new CopyOnWriteArrayList<>();
	}

	/**
	 * Gets the value of the function for a named building type.
	 *
	 * @param type the building type.
	 * @param newBuilding  true if adding a new building.
	 * @param settlement   the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String type, boolean newBuilding, Settlement settlement) {

		double result;

		FunctionSpec spec = buildingConfig.getFunctionSpec(type, FunctionType.MANUFACTURE);
		int buildingTech = spec.getTechLevel();

		double demand = 0D;
		Iterator<Person> i = settlement.getAllAssociatedPeople().iterator();
		while (i.hasNext()) {
			demand += i.next().getSkillManager().getSkillLevel(SkillType.MATERIALS_SCIENCE);
		}

		double supply = 0D;
		int highestExistingTechLevel = 0;
		boolean removedBuilding = false;
		BuildingManager buildingManager = settlement.getBuildingManager();
		Iterator<Building> j = buildingManager.getBuildingSet(FunctionType.MANUFACTURE).iterator();
		while (j.hasNext()) {
			Building building = j.next();
			if (!newBuilding && building.getBuildingType().equalsIgnoreCase(type) && !removedBuilding) {
				removedBuilding = true;
			} else {
				Manufacture manFunction = building.getManufacture();
				int tech = manFunction.techLevel;
				double processes = manFunction.getNumPrintersInUse();
				double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
				supply += (tech * tech) * processes * wearModifier;

				if (tech > highestExistingTechLevel) {
					highestExistingTechLevel = tech;
				}
			}
		}

		double baseManufactureValue = demand / (supply + 1D);

		double processes = spec.getIntegerProperty(CONCURRENT_PROCESSES);
		double manufactureValue = (buildingTech * buildingTech) * processes;

		result = manufactureValue * baseManufactureValue;

		// If building has higher tech level than other buildings at settlement,
		// add difference between best manufacturing processes.
		if (buildingTech > highestExistingTechLevel) {
			double bestExistingProcessValue = 0D;
			if (highestExistingTechLevel > 0D) {
				bestExistingProcessValue = getBestManufacturingProcessValue(highestExistingTechLevel, settlement);
			}
			double bestBuildingProcessValue = getBestManufacturingProcessValue(buildingTech, settlement);
			double processValueDiff = bestBuildingProcessValue - bestExistingProcessValue;

			processValueDiff = MathUtils.between(processValueDiff, 0D, PROCESS_MAX_VALUE);

			result += processValueDiff;
		}

		return result;
	}

	/**
	 * Gets the best manufacturing process value for a given manufacturing tech
	 * level at a settlement.
	 *
	 * @param techLevel  the manufacturing tech level.
	 * @param settlement the settlement
	 * @return best manufacturing process value.
	 */
	private static double getBestManufacturingProcessValue(int techLevel, Settlement settlement) {

		double result = 0D;

		Iterator<ManufactureProcessInfo> i = ManufactureUtil.getAllManufactureProcesses().iterator();
		while (i.hasNext()) {
			ManufactureProcessInfo process = i.next();
			if (process.getTechLevelRequired() <= techLevel) {
				double value = ManufactureUtil.getManufactureProcessValue(process, settlement);
				if (value > result) {
					result = value;
				}
			}
		}

		return result;
	}

	/**
	 * Gets the manufacturing tech level of the building.
	 *
	 * @return tech level.
	 */
	public int getTechLevel() {
		return techLevel;
	}

	/**
	 * Gets the maximum concurrent manufacturing processes supported by the
	 * building.
	 *
	 * @return maximum concurrent processes.
	 */
	public int getMaxProcesses() {
		return numMaxConcurrentProcesses;
	}

	/**
	 * Gets the current total number of manufacturing and salvage processes happening in this
	 * building.
	 *
	 * @return current total.
	 */
	public int getCurrentTotalProcesses() {
		return processes.size() + salvages.size();
	}

	/**
	 * Gets a list of the current manufacturing processes.
	 *
	 * @return unmodifiable list of processes.
	 */
	public List<ManufactureProcess> getProcesses() {
		return Collections.unmodifiableList(processes);
	}

	/**
	 * Adds a new manufacturing process to the building.
	 *
	 * @param process the new manufacturing process.
	 * @throws BuildingException if error adding process.
	 */
	public void addProcess(ManufactureProcess process) {
		if (process == null) {
			throw new IllegalArgumentException("process is null");
		}

		if (getCurrentTotalProcesses() >= numPrintersInUse) {
			logger.info(getBuilding().getSettlement(), 20_000,
					getBuilding()
					+ ": " + getCurrentTotalProcesses() + " concurrent processes.");
			logger.info(getBuilding().getSettlement(), 20_000,
					getBuilding()
					+ ": " + numPrintersInUse + " 3D-printer(s) installed for use."
					+ "");
			logger.info(getBuilding().getSettlement(), 20_000,
					getBuilding()
					+ ": " + (numMaxConcurrentProcesses-numPrintersInUse)
					+ " 3D-printer slot(s) available."
					+ "");
			return;
		}

		processes.add(process);

		// Consume inputs.
		for (var item : process.getInfo().getInputList()) {
			if (ItemType.AMOUNT_RESOURCE.equals(item.getType())) {
				int id = ResourceUtil.findIDbyAmountResourceName(item.getName());
				building.getSettlement().retrieveAmountResource(id, item.getAmount());
			} else if (ItemType.PART.equals(item.getType())) {
				int id = ItemResourceUtil.findIDbyItemResourceName(item.getName());
				building.getSettlement().retrieveItemResource(id, (int) item.getAmount());
			} else 
				logger.log(getBuilding(), Level.SEVERE, 20_000,
						 "Manufacture process input: " + item.getType() + " not a valid type.");
		}

		// Log manufacturing process starting.
		logger.log(getBuilding(), Level.FINEST, 20_000,
						"Starting manufacturing process: " + process.getInfo().getName());
	}

	/**
	 * Gets a list of the current salvage processes.
	 *
	 * @return unmodifiable list of salvage processes.
	 */
	public List<SalvageProcess> getSalvageProcesses() {
		return Collections.unmodifiableList(salvages);
	}

	/**
	 * Adds a new salvage process to the building.
	 *
	 * @param process the new salvage process.
	 * @throws BuildingException if error adding process.
	 */
	public void addSalvageProcess(SalvageProcess process) {
		if (process == null)
			throw new IllegalArgumentException("process is null");

		if (getCurrentTotalProcesses() >= numPrintersInUse)
			throw new IllegalStateException("No more space left to add new salvage process.");

		salvages.add(process);

		// Retrieve salvaged unit and remove from unit manager.
		Unit salvagedUnit = process.getSalvagedUnit();
		if (salvagedUnit != null) {
			if (salvagedUnit.getUnitType() == UnitType.CONTAINER
					|| salvagedUnit.getUnitType() == UnitType.EVA_SUIT) {
				building.getSettlement().removeEquipment((Equipment)salvagedUnit);
			} else if (salvagedUnit.getUnitType() == UnitType.VEHICLE) {
				building.getSettlement().removeOwnedVehicle((Vehicle)salvagedUnit);
				building.getSettlement().removeVicinityParkedVehicle((Vehicle)salvagedUnit);
			} else if (salvagedUnit.getUnitType() == UnitType.ROBOT) {
				building.getSettlement().removeOwnedRobot((Robot)salvagedUnit);
			}
		} else
			throw new IllegalStateException("Salvaged unit is null");

		Settlement settlement = building.getSettlement();

		// Set the salvage process info for the salvaged unit.
		((Salvagable) salvagedUnit).startSalvage(process.getInfo(), settlement.getIdentifier());

		// Recalculate settlement good value for salvaged unit.
		Good salvagedGood = null;
		if (salvagedUnit instanceof Equipment e) {
			salvagedGood = GoodsUtil.getEquipmentGood(e.getEquipmentType());
		} else if (salvagedUnit instanceof Vehicle v) {
			salvagedGood = GoodsUtil.getVehicleGood(v.getDescription());
		}

		if (salvagedGood == null) {
			throw new IllegalStateException("Salvaged good is null");
		}

		// Log salvage process starting.
		logger.log(getBuilding(), Level.FINEST, 20_000,
						"Starting salvage process: " + process.getInfo().getName());
	}

	@Override
	public double getCombinedPowerLoad() {
		double result = 0D;
		Iterator<ManufactureProcess> i = processes.iterator();
		while (i.hasNext()) {
			ManufactureProcess process = i.next();
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
			// Check once a sol only
			checkPrinters(pulse);

			List<ManufactureProcess> finishedProcesses = new CopyOnWriteArrayList<>();

			Iterator<ManufactureProcess> i = processes.iterator();
			while (i.hasNext()) {
				ManufactureProcess process = i.next();
				process.addProcessTime(pulse.getElapsed());

				if ((process.getProcessTimeRemaining() == 0D) && (process.getWorkTimeRemaining() == 0D)) {
					finishedProcesses.add(process);
				}
			}

			// End all processes that are done.
			Iterator<ManufactureProcess> j = finishedProcesses.iterator();
			while (j.hasNext()) {
				endManufacturingProcess(j.next(), false);
			}
		}
		return valid;
	}

	/**
	 * Checks if manufacturing function currently requires manufacturing work.
	 *
	 * @param skill the person's materials science skill level.
	 * @return true if manufacturing work.
	 */
	public boolean requiresWork(int skill) {
		boolean result = false;

		if (numPrintersInUse > getCurrentTotalProcesses())
			result = true;
		else {
			Iterator<ManufactureProcess> i = processes.iterator();
			while (i.hasNext()) {
				ManufactureProcess process = i.next();
				boolean workRequired = (process.getWorkTimeRemaining() > 0D);
				// Allow a low material science skill person to have access to do the next level skill process
				boolean skillRequired = (process.getInfo().getSkillLevelRequired() <= skill + SKILL_GAP);
				if (workRequired && skillRequired)
					result = true;
			}
		}

		return result;
	}

	/**
	 * Checks if manufacturing function currently requires salvaging work.
	 *
	 * @param skill the person's materials science skill level.
	 * @return true if manufacturing work.
	 */
	public boolean requiresSalvagingWork(int skill) {
		boolean result = false;

		if (numPrintersInUse > getCurrentTotalProcesses())
			result = true;
		else {
			Iterator<SalvageProcess> i = salvages.iterator();
			while (i.hasNext()) {
				SalvageProcess process = i.next();
				boolean workRequired = (process.getWorkTimeRemaining() > 0D);
				// Allow a low material science skill person to have access to do the next level skill process
				boolean skillRequired = (process.getInfo().getSkillLevelRequired() <= skill + 1);
				if (workRequired && skillRequired)
					result = true;
			}
		}

		return result;
	}

	private void despositOutputs(ManufactureProcess process) {
		Settlement settlement = building.getSettlement();

		// Produce outputs.
		for(var item : process.getInfo().getOutputList()) {
			if (ManufactureUtil.getManufactureProcessItemValue(item, settlement, true) > 0D) {
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
						else {
							outputId = -1;
						}
					} break;

					case EQUIPMENT: {
						// Produce equipment.
						var equipmentType = EquipmentType.convertName2Enum(item.getName());
						outputId = EquipmentType.getResourceID(equipmentType);
						int number = (int) outputAmount;
						for (int x = 0; x < number; x++) {
							EquipmentFactory.createEquipment(equipmentType, settlement);
						}
					} break;

					case BIN: {
						// Produce bins.
						outputAmount = item.getAmount();
						int number = (int) outputAmount;
						for (int x = 0; x < number; x++) {
							BinFactory.createBins(item.getName(), settlement);
						}
					} break;
				
					case VEHICLE: {
						// Produce vehicles.
						int number = (int) outputAmount;
						for (int x = 0; x < number; x++) {
							Vehicle v = VehicleFactory.createVehicle(unitManager, settlement, item.getName());

							outputId = VehicleType.getVehicleID(v.getVehicleType());
						}
					} break;
				}

				// Output settlement
				if (outputId >= 0) {
					settlement.addOutput(outputId, outputAmount, process.getTotalWorkTime());
				}

				Good good = GoodsUtil.getGood(item.getName());
				if (good == null) {
					logger.severe(item.getName() + " is not a good.");
				}
				else
					// Recalculate settlement good value for the output item.
					settlement.getGoodsManager().determineGoodValue(good);
			}
		}

		// Record process finish
		settlement.recordProcess(process.getInfo(), "Manufacture", building);
	}

	private void returnInputs(ManufactureProcess process) {
		Settlement settlement = building.getAssociatedSettlement();
	
		// Premature end of process. Return all input materials.
		// Note: should some resources be consumed and irreversible ?
		for(var item : process.getInfo().getInputList()) {
			if (ManufactureUtil.getManufactureProcessItemValue(item, settlement, false) > 0D) {
				if (ItemType.AMOUNT_RESOURCE.equals(item.getType())) {
					// Produce amount resources.
					AmountResource resource = ResourceUtil.findAmountResource(item.getName());
					double amount = item.getAmount();
					double capacity = settlement.getAmountResourceRemainingCapacity(resource.getID());
					if (item.getAmount() > capacity) {
						double overAmount = item.getAmount() - capacity;
						logger.severe("Premature ending '" +  process.getInfo().getName() + "'. "
								+ "Not enough storage capacity to store " + overAmount + " of " + item.getName()
								+ " at " + settlement.getName());
						amount = capacity;
					}
					settlement.storeAmountResource(resource.getID(), amount);
				}

				else if (ItemType.PART.equals(item.getType())) {
					// Produce parts.
					Part part = (Part) ItemResourceUtil.findItemResource(item.getName());
					int num = (int) item.getAmount();
					int id = part.getID();
					double mass = num * part.getMassPerItem();
					double capacity = settlement.getCargoCapacity();
					if (mass <= capacity) {
						settlement.storeItemResource(id, num);
					}
				}

				else if (ItemType.EQUIPMENT.equals(item.getType())) {
					// Produce equipment.
					String equipmentType = item.getName();
					int number = (int) item.getAmount();
					for (int x = 0; x < number; x++) {
						Equipment equipment = EquipmentFactory.createEquipment(equipmentType,
								settlement);
						unitManager.addUnit(equipment);
					}
				}

				else if (ItemType.BIN.equals(item.getType())) {
					// Produce equipment.
					String type = item.getName();
					int number = (int) item.getAmount();
					for (int x = 0; x < number; x++) {
						BinFactory.createBins(type, settlement);
					}
				}
				
				else if (ItemType.VEHICLE.equals(item.getType())) {
					// Produce vehicles.
					String vehicleType = item.getName();
					int number = (int) item.getAmount();
					for (int x = 0; x < number; x++) {
						VehicleFactory.createVehicle(unitManager, settlement, vehicleType);
					}
				}

				else
					throw new IllegalStateException(
							"Manufacture.addProcess(): output: " + item.getType() + " not a valid type.");
			}
		}
	}

	/**
	 * Ends a manufacturing process.
	 *
	 * @param process   the process to end.
	 * @param premature true if the process has ended prematurely.
	 * @throws BuildingException if error ending process.
	 */
	public void endManufacturingProcess(ManufactureProcess process, boolean premature) {

		if (!premature) {
			despositOutputs(process);
		}
		else {
			returnInputs(process);
		}

		processes.remove(process);

		// Log process ending.
		logger.log(getBuilding(), Level.FINEST, 20_000,
				"Ending manufacturing process: " + process.getInfo().getName());
	}

	/**
	 * Ends a salvage process.
	 *
	 * @param process   the process to end.
	 * @param premature true if process is ended prematurely.
	 * @throws BuildingException if error ending process.
	 */
	public void endSalvageProcess(SalvageProcess process, boolean premature) {
		Settlement settlement = building.getSettlement();

		Map<Integer, Integer> partsSalvaged = new ConcurrentHashMap<>(0);

		if (!premature) {
			// Produce salvaged parts.

			// Determine the salvage chance based on the wear condition of the item.
			double salvageChance = 50D;
			Unit salvagedUnit = process.getSalvagedUnit();
			if (salvagedUnit instanceof Malfunctionable) {
				Malfunctionable malfunctionable = (Malfunctionable) salvagedUnit;
				double wearCondition = malfunctionable.getMalfunctionManager().getWearCondition();
				salvageChance = (wearCondition * .25D) + 25D;
			}

			// Add the average material science skill of the salvagers.
			salvageChance += process.getAverageSkillLevel() * 5D;

			// Salvage parts.
			for(var partSalvage : process.getInfo().getOutputList()) {
				Part part = (Part) ItemResourceUtil.findItemResource(partSalvage.getName());
				int id = part.getID();

				int totalNumber = 0;
				for (int x = 0; x < (int)partSalvage.getAmount(); x++) {
					if (RandomUtil.lessThanRandPercent(salvageChance))
						totalNumber++;
				}

				if (totalNumber > 0) {
					partsSalvaged.put(id, totalNumber);

					double mass = totalNumber * part.getMassPerItem();
					double capacity = settlement.getCargoCapacity();
					if (mass <= capacity)
						settlement.storeItemResource(id, totalNumber);

					Good good = GoodsUtil.getGood(part.getName());
					if (good == null) {
						logger.severe(getBuilding(), part.getName() + " is not a good.");
					}
					else
						// Recalculate settlement good value for salvaged part.
						settlement.getGoodsManager().determineGoodValue(good);
				}
			}

			settlement.recordProcess(process.getInfo(), "Salvage", building);
		}

		// Finish the salvage.
		((Salvagable) process.getSalvagedUnit()).getSalvageInfo().finishSalvage(partsSalvaged, masterClock.getMarsTime());

		salvages.remove(process);

		// Log salvage process ending.
		logger.log(getBuilding(), Level.FINEST, 20_000,
						"Ending salvage process: " + process.getInfo().getName());

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
	 * Checks if enough 3D printer(s) are supporting the manufacturing.
	 * 
	 * processes
	 * @param pulse
	 */
	public void checkPrinters(ClockPulse pulse) {
		// Check only once a day for # of processes that are needed.
		if (pulse.isNewSol()) {
			// Gets the available number of printers in storage
			int numAvailable = building.getSettlement().getItemResourceStored(PRINTER_ID);

			// NOTE: it's reasonable to create a settler's task to install a 3-D printer manually over a period of time
			if (numPrintersInUse < numMaxConcurrentProcesses) {
				int deficit = numMaxConcurrentProcesses - numPrintersInUse;
				logger.info(getBuilding(), 20_000,
						numPrintersInUse
						+ " 3D-printer(s) in use out of " + numAvailable);

				if (deficit > 0 && numAvailable > 0) {
					int size = Math.min(numAvailable, deficit);
					for (int i=0; i<size; i++) {
						numPrintersInUse++;
						numAvailable--;
						int lacking = building.getSettlement().retrieveItemResource(PRINTER_ID, 1);
						if (lacking > 0) {
							logger.info(getBuilding(), 20_000,
									"No 3D-printer available.");
						}
					}

					logger.info(getBuilding(), 20_000,
							size + " 3D-printer(s) just installed.");
				}
			}

            // NOTE: if not having enough printers,
			// determine how to use GoodsManager to push for making new 3D printers
		}
	}

	public int getNumPrintersInUse() {
		return numPrintersInUse;
	}


	@Override
	public void destroy() {
		super.destroy();

		Iterator<ManufactureProcess> i = processes.iterator();
		while (i.hasNext()) {
			i.next().destroy();
		}

		Iterator<SalvageProcess> j = salvages.iterator();
		while (j.hasNext()) {
			j.next().destroy();
		}
	}
}
