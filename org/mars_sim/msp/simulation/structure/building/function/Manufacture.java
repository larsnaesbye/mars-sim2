/**
 * Mars Simulation Project
 * Manufacture.java
 * @version 2.85 2008-11-28
 * @author Scott Davis
 */

package org.mars_sim.msp.simulation.structure.building.function;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.simulation.Inventory;
import org.mars_sim.msp.simulation.Simulation;
import org.mars_sim.msp.simulation.SimulationConfig;
import org.mars_sim.msp.simulation.UnitManager;
import org.mars_sim.msp.simulation.equipment.Equipment;
import org.mars_sim.msp.simulation.equipment.EquipmentFactory;
import org.mars_sim.msp.simulation.manufacture.ManufactureProcess;
import org.mars_sim.msp.simulation.manufacture.ManufactureProcessInfo;
import org.mars_sim.msp.simulation.manufacture.ManufactureProcessItem;
import org.mars_sim.msp.simulation.manufacture.ManufactureUtil;
import org.mars_sim.msp.simulation.resource.AmountResource;
import org.mars_sim.msp.simulation.resource.ItemResource;
import org.mars_sim.msp.simulation.resource.Part;
import org.mars_sim.msp.simulation.structure.Settlement;
import org.mars_sim.msp.simulation.structure.building.Building;
import org.mars_sim.msp.simulation.structure.building.BuildingConfig;
import org.mars_sim.msp.simulation.structure.building.BuildingException;
import org.mars_sim.msp.simulation.structure.goods.GoodsManager;
import org.mars_sim.msp.simulation.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.simulation.vehicle.Rover;

/**
 * A building function for manufacturing.
 */
public class Manufacture extends Function implements Serializable {

	private static String CLASS_NAME = 
	    "org.mars_sim.msp.simulation.structure.building.function.Manufacture";
	private static Logger logger = Logger.getLogger(CLASS_NAME);
	public static final String NAME = "Manufacture";
	
	// Data members.
	private int techLevel;
	private int concurrentProcesses;
	private List<ManufactureProcess> processes;
	
	/**
	 * Constructor
	 * @param building the building the function is for.
	 * @throws BuildingException if error constructing function.
	 */
	public Manufacture(Building building) throws BuildingException {
		// Use Function constructor.
		super(NAME, building);
		
		BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();
		
		try {
			techLevel = config.getManufactureTechLevel(building.getName());
			concurrentProcesses = config.getManufactureConcurrentProcesses(building.getName());
		}
		catch (Exception e) {
			throw new BuildingException("Manufacture.constructor: " + e.getMessage());
		}
		
		processes = new ArrayList<ManufactureProcess>();
	}
    
    /**
     * Gets the value of the function for a named building.
     * @param buildingName the building name.
     * @param newBuilding true if adding a new building.
     * @param settlement the settlement.
     * @return value (VP) of building function.
     * @throws Exception if error getting function value.
     */
    public static final double getFunctionValue(String buildingName, boolean newBuilding, 
            Settlement settlement) throws Exception {
        
        BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();
        double buildingTech = config.getManufactureTechLevel(buildingName);
        
        // Determine demand as highest manufacturing process value for settlement.
        double demand = 0D;
        Iterator<ManufactureProcessInfo> i = ManufactureUtil.getAllManufactureProcesses().iterator();
        while (i.hasNext()) {
            ManufactureProcessInfo process = i.next();
            if (process.getTechLevelRequired() <= buildingTech) {
                double value = ManufactureUtil.getManufactureProcessValue(process, settlement);
                if (value > demand) demand = value;
            }
        }
        if (demand > 1000D) demand = 1000D;
        
        double supply = 0D;
        boolean removedBuilding = false;
        Iterator<Building> j = settlement.getBuildingManager().getBuildings(NAME).iterator();
        while (j.hasNext()) {
            Building building = j.next();
            if (!newBuilding && building.getName().equalsIgnoreCase(buildingName) && !removedBuilding) {
                removedBuilding = true;
            }
            else {
                Manufacture manFunction = (Manufacture) building.getFunction(NAME);
                double tech = manFunction.getTechLevel();
                double processes = manFunction.getConcurrentProcesses();
                supply += (tech * tech) * processes;
            }
        }
        
        double baseManufactureValue = demand / (supply + 1D);
        
        double processes = config.getManufactureConcurrentProcesses(buildingName);
        double manufactureValue = (buildingTech * buildingTech) * processes;
        
        return manufactureValue * baseManufactureValue / 1000D;
    }
	
	/**
	 * Gets the manufacturing tech level of the building.
	 * @return tech level.
	 */
	public int getTechLevel() {
		return techLevel;
	}
	
	/**
	 * Gets the maximum concurrent manufacturing processes supported by the building.
	 * @return maximum concurrent processes.
	 */
	public int getConcurrentProcesses() {
		return concurrentProcesses;
	}
	
	/**
	 * Gets a list of the current manufacturing processes.
	 * @return unmodifiable list of processes.
	 */
	public List<ManufactureProcess> getProcesses() {
		return Collections.unmodifiableList(processes);
	}
	
	/**
	 * Adds a new manufacturing process to the building.
	 * @param process the new manufacturing process.
	 * @throws BuildingException if error adding process.
	 */
	public void addProcess(ManufactureProcess process) throws BuildingException {
		if (process == null) throw new IllegalArgumentException("process is null");
		processes.add(process);
		
		// Consume inputs.
		try {
			Inventory inv = getBuilding().getInventory();
			Iterator<ManufactureProcessItem> i = process.getInfo().getInputList().iterator();
			while (i.hasNext()) {
				ManufactureProcessItem item = i.next();
				if (ManufactureProcessItem.AMOUNT_RESOURCE.equalsIgnoreCase(item.getType())) {
					AmountResource resource = AmountResource.findAmountResource(item.getName());
					inv.retrieveAmountResource(resource, item.getAmount());
				}
				else if (ManufactureProcessItem.PART.equalsIgnoreCase(item.getType())) {
					Part part = (Part) ItemResource.findItemResource(item.getName());
					inv.retrieveItemResources(part, (int) item.getAmount());
				}
				else throw new BuildingException("Manufacture process input: " + 
						item.getType() + " not a valid type.");
				
				// Recalculate settlement good value for input item.
				GoodsManager goodsManager = getBuilding().getBuildingManager().getSettlement().getGoodsManager();
				goodsManager.updateGoodValue(ManufactureUtil.getGood(item), false);
			}
		}
		catch (Exception e) {
			throw new BuildingException("Problem adding manufacturing process.", e);
		}
		
		// Log manufacturing process starting.
		if (logger.isLoggable(Level.FINEST)) {
			Settlement settlement = getBuilding().getBuildingManager().getSettlement();
			logger.finest(getBuilding() + " at " 
						    + settlement
						    + " starting manufacturing process: " 
						    + process.getInfo().getName());
		}
	}
	
	@Override
	public double getFullPowerRequired() {
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
	public double getPowerDownPowerRequired() {
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
	public void timePassing(double time) throws BuildingException {
		
		List<ManufactureProcess> finishedProcesses = new ArrayList<ManufactureProcess>();
		
		Iterator<ManufactureProcess> i = processes.iterator();
		while (i.hasNext()) {
			ManufactureProcess process = i.next();
			process.addProcessTime(time);
		
			if ((process.getProcessTimeRemaining() == 0D) && 
					(process.getWorkTimeRemaining() == 0D)) {
				finishedProcesses.add(process);
			}
		}
		
		// End all processes that are done.
		Iterator<ManufactureProcess> j = finishedProcesses.iterator();
		while (j.hasNext()) {
			endManufacturingProcess(j.next());
		}
	}
	
    /**
     * Checks if manufacturing function currently requires work.
     * @param skill the person's materials science skill level.
     * @return true if manufacturing work.
     */
    public boolean requiresWork(int skill) {
		boolean result = false;
		
		if (concurrentProcesses > processes.size()) result = true;
		else {
			Iterator<ManufactureProcess> i = processes.iterator();
			while (i.hasNext()) {
				ManufactureProcess process = i.next();
				boolean workRequired = (process.getWorkTimeRemaining() > 0D);
				boolean skillRequired = (process.getInfo().getSkillLevelRequired() <= skill);
				if (workRequired && skillRequired) result = true;
			}
		}
		
		return result;
    }
    
    /**
     * Ends a manufacturing process.
     * @param process the process to end.
     * @throws BuildingException if error ending process.
     */
    public void endManufacturingProcess(ManufactureProcess process) throws BuildingException {
    	
		// Produce outputs.
		try {
			Settlement settlement = getBuilding().getBuildingManager().getSettlement();
			UnitManager manager = Simulation.instance().getUnitManager();
			Inventory inv = getBuilding().getInventory();
			
			Iterator<ManufactureProcessItem> j = process.getInfo().getOutputList().iterator();
			while (j.hasNext()) {
				ManufactureProcessItem item = j.next();
				if (ManufactureUtil.getManufactureProcessItemValue(item, settlement) > 0D) {
					if (ManufactureProcessItem.AMOUNT_RESOURCE.equalsIgnoreCase(item.getType())) {
						// Produce amount resources.
						AmountResource resource = AmountResource.findAmountResource(item.getName());
						double amount = item.getAmount();
						double capacity = inv.getAmountResourceRemainingCapacity(resource, true);
						if (item.getAmount() > capacity) amount = capacity;  
						inv.storeAmountResource(resource, amount, true);
					}
					else if (ManufactureProcessItem.PART.equalsIgnoreCase(item.getType())) {
						// Produce parts.
						Part part = (Part) ItemResource.findItemResource(item.getName());
						double mass = item.getAmount() * part.getMassPerItem();
						double capacity = inv.getGeneralCapacity();
						if (mass <= capacity)
							inv.storeItemResources(part, (int) item.getAmount());
					}
					else if (ManufactureProcessItem.EQUIPMENT.equalsIgnoreCase(item.getType())) {
						// Produce equipment.
						String equipmentType = item.getName();
						int number = (int) item.getAmount();
						for (int x = 0; x < number; x++) {
							Equipment equipment = EquipmentFactory.getEquipment(equipmentType, settlement.getCoordinates(), false);
							equipment.setName(manager.getNewName(UnitManager.EQUIPMENT, equipmentType, null));
							inv.storeUnit(equipment);
						}
					}
					else if (ManufactureProcessItem.VEHICLE.equalsIgnoreCase(item.getType())) {
						// Produce vehicles.
						String vehicleType = item.getName();
						int number = (int) item.getAmount();
						for (int x = 0; x < number; x++) {
							if (LightUtilityVehicle.NAME.equals(vehicleType)) {
		    					String name = manager.getNewName(UnitManager.VEHICLE, "LUV", null);
		    					manager.addUnit(new LightUtilityVehicle(name, vehicleType, settlement));
		    				}
		    				else {
		    					String name = manager.getNewName(UnitManager.VEHICLE, null, null);
		    					manager.addUnit(new Rover(name, vehicleType, settlement));
		    				}
						}
					}
					else throw new BuildingException("Manufacture.addProcess(): output: " + 
							item.getType() + " not a valid type.");
					
					// Recalculate settlement good value for output item.
					GoodsManager goodsManager = getBuilding().getBuildingManager().getSettlement().getGoodsManager();
					goodsManager.updateGoodValue(ManufactureUtil.getGood(item), false);
				}
			}
		}
		catch (Exception e) {
			throw new BuildingException("Problem completing manufacturing process.", e);
		}
		
		processes.remove(process);
		
		// Log process ending.
		if (logger.isLoggable(Level.FINEST)) { 
			Settlement settlement = getBuilding().getBuildingManager().getSettlement();
			logger.finest(getBuilding() + " at " + settlement + " ending manufacturing process: " + 
					process.getInfo().getName());
		}
    }
}