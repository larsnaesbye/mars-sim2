/*
 * Mars Simulation Project
 * FoodProductionUtil.java
 * @date 2021-10-20
 * @author Manny Kung
 */

package org.mars_sim.msp.core.foodProduction;

import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.equipment.EquipmentFactory;
import org.mars_sim.msp.core.equipment.EquipmentType;
import org.mars_sim.msp.core.resource.ItemResourceUtil;
import org.mars_sim.msp.core.resource.ItemType;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.function.FoodProduction;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.goods.Good;
import org.mars_sim.msp.core.structure.goods.GoodsManager;
import org.mars_sim.msp.core.structure.goods.GoodsUtil;
import org.mars_sim.msp.core.time.MarsClock;

/**
 * Utility class for getting foodProduction processes.
 */
public final class FoodProductionUtil {

	private static FoodProductionConfig config = SimulationConfig.instance().getFoodProductionConfiguration();
	
	/** Private constructor. */
	private FoodProductionUtil() {
	}

	/**
	 * Gets all foodProduction processes.
	 * 
	 * @return list of processes.
	 * @throws Exception if error getting processes.
	 */
	public static List<FoodProductionProcessInfo> getAllFoodProductionProcesses() {
		return config.getFoodProductionProcessList();
	}

	/**
	 * gives back an alphabetically ordered map of all foodProduction processes.
	 * 
	 * @return {@link TreeMap}<{@link String},{@link FoodProductionProcessInfo}>
	 */
	public static TreeMap<String, FoodProductionProcessInfo> getAllFoodProductionProcessesMap() {
		TreeMap<String, FoodProductionProcessInfo> map = new TreeMap<String, FoodProductionProcessInfo>();
		for (FoodProductionProcessInfo item : getAllFoodProductionProcesses()) {
			map.put(item.getName(), item);
		}
		return map;
	}

	/**
	 * Gets foodProduction processes within the capability of a tech level.
	 * 
	 * @param techLevel the tech level.
	 * @return list of processes.
	 * @throws Exception if error getting processes.
	 */
	public static List<FoodProductionProcessInfo> getFoodProductionProcessesForTechLevel(int techLevel) {
		List<FoodProductionProcessInfo> result = new CopyOnWriteArrayList<FoodProductionProcessInfo>();
		Iterator<FoodProductionProcessInfo> i = config.getFoodProductionProcessList().iterator();
		while (i.hasNext()) {
			FoodProductionProcessInfo process = i.next();
			if (process.getTechLevelRequired() <= techLevel)
				result.add(process);
		}

		return result;
	}

	/**
	 * gets foodProduction processes with given output.
	 * 
	 * @param item {@link String} name of desired output
	 * @return {@link List}<{@link FoodProductionProcessItem}> list of processes
	 */
	public static List<FoodProductionProcessInfo> getFoodProductionProcessesWithGivenOutput(String name) {
		List<FoodProductionProcessInfo> result = new CopyOnWriteArrayList<FoodProductionProcessInfo>();
		Iterator<FoodProductionProcessInfo> i = config.getFoodProductionProcessList().iterator();
		while (i.hasNext()) {
			FoodProductionProcessInfo process = i.next();
			for (String n : process.getOutputNames()) {
				if (name.equalsIgnoreCase(n))
					result.add(process);
			}
		}
		return result;
	}

	/**
	 * gets foodProduction processes with given input.
	 * 
	 * @param name {@link String} desired input
	 * @return {@link List}<{@link FoodProductionProcessItem}> list of processes
	 */
	public static List<FoodProductionProcessInfo> getFoodProductionProcessesWithGivenInput(String name) {
		List<FoodProductionProcessInfo> result = new CopyOnWriteArrayList<FoodProductionProcessInfo>();
		Iterator<FoodProductionProcessInfo> i = config.getFoodProductionProcessList().iterator();
		while (i.hasNext()) {
			FoodProductionProcessInfo process = i.next();
			for (String n : process.getInputNames()) {
				if (name.equalsIgnoreCase(n))
					result.add(process);
			}
		}
		return result;
	}

	/**
	 * Gets foodProduction processes within the capability of a tech level and a
	 * skill level.
	 * 
	 * @param techLevel  the tech level.
	 * @param skillLevel the skill level.
	 * @return list of processes.
	 * @throws Exception if error getting processes.
	 */
	public static List<FoodProductionProcessInfo> getFoodProductionProcessesForTechSkillLevel(int techLevel,
			int skillLevel) {
		List<FoodProductionProcessInfo> result = new CopyOnWriteArrayList<FoodProductionProcessInfo>();
		Iterator<FoodProductionProcessInfo> i = config.getFoodProductionProcessList().iterator();
		while (i.hasNext()) {
			FoodProductionProcessInfo process = i.next();
			if ((process.getTechLevelRequired() <= techLevel) && (process.getSkillLevelRequired() <= skillLevel))
				result.add(process);
		}

		return result;
	}

	/**
	 * Gets the goods value of a foodProduction process at a settlement.
	 * 
	 * @param process    the foodProduction process.
	 * @param settlement the settlement.
	 * @return goods value of output goods minus input goods.
	 * @throws Exception if error determining good values.
	 */
	public static double getFoodProductionProcessValue(FoodProductionProcessInfo process, Settlement settlement) {

		double inputsValue = 0D;
		Iterator<FoodProductionProcessItem> i = process.getInputList().iterator();
		while (i.hasNext())
			inputsValue += getFoodProductionProcessItemValue(i.next(), settlement, false);

		double outputsValue = 0D;
		Iterator<FoodProductionProcessItem> j = process.getOutputList().iterator();
		while (j.hasNext())
			outputsValue += getFoodProductionProcessItemValue(j.next(), settlement, true);

		// Subtract power value.
//		double hoursInMillisol = MarsClock.convertMillisolsToSeconds(1D) / 60D / 60D;
		double powerHrsRequiredPerMillisol = process.getPowerRequired() * MarsClock.HOURS_PER_MILLISOL;
		double powerValue = powerHrsRequiredPerMillisol * settlement.getPowerGrid().getPowerValue();

		return outputsValue - inputsValue - powerValue;
	}

	/**
	 * Gets the good value of a foodProduction process item for a settlement.
	 * 
	 * @param item       the foodProduction process item.
	 * @param settlement the settlement.
	 * @param isOutput   is item an output of process?
	 * @return good value.
	 * @throws Exception if error getting good value.
	 */
	public static double getFoodProductionProcessItemValue(FoodProductionProcessItem item, Settlement settlement,
			boolean isOutput) {
		double result = 0D;

		GoodsManager manager = settlement.getGoodsManager();

		if (item.getType() == ItemType.AMOUNT_RESOURCE) {
//			AmountResource resource = ResourceUtil.findAmountResource(item.getName());
	        int id = ResourceUtil.findIDbyAmountResourceName(item.getName());
			double amount = item.getAmount();
			if (isOutput) {
				double remainingCapacity = settlement.getAmountResourceRemainingCapacity(id);
				if (amount > remainingCapacity) {
					amount = remainingCapacity;
				}
			}
			result = manager.getGoodValuePerItem(id) * amount;
		} else if (item.getType() == ItemType.PART) {
			int id = ItemResourceUtil.findIDbyItemResourceName(item.getName());
			result = manager.getGoodValuePerItem(id) * item.getAmount();
		} else if (item.getType() == ItemType.EQUIPMENT) {
			int id = EquipmentType.convertName2ID(item.getName());
			result = manager.getGoodValuePerItem(id) * item.getAmount();
//		} else if (item.getType().equals(ItemType.VEHICLE)) {
//			Good good = GoodsUtil.getVehicleGood(item.getName());
//			result = manager.getGoodValuePerItem(good) * item.getAmount();
		} else
			throw new IllegalStateException("Item type: " + item.getType() + " not valid.");

		return result;
	}

	/**
	 * Checks to see if a foodProduction process can be started at a given
	 * foodProduction building.
	 * 
	 * @param process the foodProduction process to start.
	 * @param kitchen the foodProduction building.
	 * @return true if process can be started.
	 * @throws Exception if error determining if process can be started.
	 */
	public static boolean canProcessBeStarted(FoodProductionProcessInfo process, FoodProduction kitchen) {
		boolean result = kitchen.getTotalProcessNumber() < kitchen.getConcurrentProcesses();

		// Check to see if kitchen is full of processes.

        // Check to see if process tech level is above kitchen tech level.
		if (kitchen.getTechLevel() < process.getTechLevelRequired())
			result = false;

		Settlement settlement = kitchen.getBuilding().getSettlement();

		// Check to see if process input items are available at settlement.
		if (!areProcessInputsAvailable(process, settlement))
			result = false;

		// Check to see if room for process output items at settlement.
		// if (!canProcessOutputsBeStored(process, inv)) result = false;

		return result;
	}

	/**
	 * Checks if process inputs are available in an inventory.
	 * 
	 * @param process the foodProduction process.
	 * @param inv     the inventory.
	 * @return true if process inputs are available.
	 * @throws Exception if error determining if process inputs are available.
	 */
	private static boolean areProcessInputsAvailable(FoodProductionProcessInfo process, Settlement settlement) {
		boolean result = true;

		Iterator<FoodProductionProcessItem> i = process.getInputList().iterator();
		while (result && i.hasNext()) {
			FoodProductionProcessItem item = i.next();
			if (ItemType.AMOUNT_RESOURCE == item.getType()) {
//                AmountResource resource = ResourceUtil.findAmountResource(item.getName());
				int id = ResourceUtil.findIDbyAmountResourceName(item.getName());
				result = (settlement.getAmountResourceStored(id) >= item.getAmount());
				// Add demand tracking
//				inv.addAmountDemandTotalRequest(id, item.getAmount());
			} else if (ItemType.PART == item.getType()) {
//				Part part = (Part) ItemResourceUtil.findItemResource(item.getName());
				int id = ItemResourceUtil.findIDbyItemResourceName(item.getName());
				result = (settlement.getItemResourceStored(id) >= (int) item.getAmount());
				// Add tracking demand
//				inv.addItemDemandTotalRequest(id, (int) item.getAmount());
			} else
				throw new IllegalStateException(
						"FoodProduction process input: " + item.getType() + " not a valid type.");
		}

		return result;
	}

	/**
	 * Checks if settlement has buildings with food production function.
	 * 
	 * @param settlement the settlement.
	 * @return true if buildings with food production function.
	 * @throws BuildingException if error checking for foodProduction buildings.
	 */
	public static boolean doesSettlementHaveFoodProduction(Settlement settlement) {
		return (settlement.getBuildingManager().getBuildings(FunctionType.FOOD_PRODUCTION).size() > 0);
	}

	/**
	 * Gets the highest foodProduction tech level in a settlement.
	 * 
	 * @param settlement the settlement.
	 * @return highest foodProduction tech level.
	 * @throws BuildingException if error determining highest tech level.
	 */
	public static int getHighestFoodProductionTechLevel(Settlement settlement) {
		int highestTechLevel = 0;
		Iterator<Building> i = settlement.getBuildingManager().getBuildings(FunctionType.FOOD_PRODUCTION).iterator();
		while (i.hasNext()) {
//			Building building = i.next();
			FoodProduction foodProductionFunction = i.next().getFoodProduction();
			if (foodProductionFunction.getTechLevel() > highestTechLevel)
				highestTechLevel = foodProductionFunction.getTechLevel();
		}

		return highestTechLevel;
	}

	/**
	 * Gets a good for a food production process item.
	 * 
	 * @param item the food production process item.
	 * @return good
	 * @throws Exception if error determining good.
	 */
	public static Good getGood(FoodProductionProcessItem item) {
		Good result = null;
		if (ItemType.AMOUNT_RESOURCE == item.getType()) {
			result = GoodsUtil.getResourceGood(ResourceUtil.findAmountResource(item.getName()));
		} else if (ItemType.PART == item.getType()) {
			result = GoodsUtil.getResourceGood(ItemResourceUtil.findItemResource(item.getName()));
		} else if (ItemType.EQUIPMENT == item.getType()) {
			result = GoodsUtil.getEquipmentGood(EquipmentType.convertName2Enum(item.getName()));
		}
//		 else if (Type.VEHICLE.equals(item.getType())) {
//		 result = GoodsUtil.getVehicleGood(item.getName());
//		 }

		return result;
	}

	/**
	 * Gets the mass for a foodProduction process item.
	 * 
	 * @param item the foodProduction process item.
	 * @return mass (kg).
	 * @throws Exception if error determining the mass.
	 */
	public static double getMass(FoodProductionProcessItem item) {
		double mass = 0D;

		if (ItemType.AMOUNT_RESOURCE == item.getType()) {
			mass = item.getAmount();
		} else if (ItemType.PART == item.getType()) {
			Part part = (Part) ItemResourceUtil.findItemResource(item.getName());
			mass = item.getAmount() * part.getMassPerItem();
		} else if (ItemType.EQUIPMENT == item.getType()) {
			double equipmentMass = EquipmentFactory.getEquipmentMass(EquipmentType.convertName2Enum(item.getName()));
			mass = item.getAmount() * equipmentMass;
		}
//		 else if (Type.VEHICLE.equals(item.getType())) {
//		 VehicleConfig config = SimulationConfig.instance().getVehicleConfiguration();
//		 mass = item.getAmount() * config.getEmptyMass(item.getName());
//		 }

		return mass;
	}

}
