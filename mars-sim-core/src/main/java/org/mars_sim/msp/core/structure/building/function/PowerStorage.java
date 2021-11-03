/**
 * Mars Simulation Project
 * PowerStorage.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;
import java.util.Iterator;

import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.structure.PowerGrid;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * The PowerStorage class is a building function depicting the interworking of a grid battery for energy storage.
 */
public class PowerStorage
extends Function
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(PowerStorage.class.getName());

	public static double HOURS_PER_MILLISOL = 0.0247 ; //MarsClock.SECONDS_IN_MILLISOL / 3600D;
	
	public static final double SECONDARY_LINE_VOLTAGE = 240D;
	
	public static final double BATTERY_MAX_VOLTAGE = 375D;
	
	public static final double PERCENT_BATTERY_RECONDITIONING_PER_CYCLE = .1; // [in %]

	/** The number of cells per modules of the battery. */
	private static int cellsPerModule = 104; // 3.6 V * 104 = 374.4 V 
	// Note : Tesla Model S has 104 cells per module
	
	// Data members.
	/** The number of modules of the battery. */
	private int numModules = 0;
	
	/** The number of times the battery has been fully discharged/depleted since last reconditioning. */
	private int timesFullyDepleted = 0;
	
	/** The degradation rate of the battery in % per sol. */
	public double percentBatteryDegradationPerSol = .05; // [in %]
	
	/** The maximum nameplate kWh of this battery. */	
	public double max_kWh_nameplate;
	
	/** The internal resistance [in ohms] in each cell. */	
	public double r_cell = 0.06; // [in ohms]

	/**  The total internal resistance of the battery. */
	private double r_total; // R_total = R of each cell * # of cells * # of modules 
	
	/**The maximum continuous discharge rate (within the safety limit) of this battery. */
	private double C_rating = 2D;
	
	/** The health of the battery. */
	private double health = 1D; 	
	
	/** The max energy storage capacity in kWh. */
	private double currentMaxCap; // [in kilo watt-hour, not Watt-hour]
	
	/** The energy last stored in the battery. */
	private double kWhCache;
	
	/** 
	 * The energy currently stored in the battery 
	 * The Watt-hour signifies that a battery can supply an amount of watts for an hour
	 * e.g. a 60 watt-hour battery can power a 60 watt light bulb for an hour
	 */
	private double kWhStored; // [in kilo Watt-hour] 

	/** 
	 * The rating of the battery in terms of its charging/discharging ability at 
	 * a particular C-rating. An amp is a measure of electrical current. The hour 
	 * indicates the length of time that the battery can supply this current.
	 * e.g. a 2.2Ah battery can supply 2.2 amps for an hour
	 */
	private double ampHours; // [in ampere-hour or Ah] 	
	
	/*
	 * The Terminal voltage is between the battery terminals with load applied. 
	 * It varies with SOC and discharge/charge current.
	 */
	private double terminalVoltage; 
	
	private double time;
	
	/**
	 * True if the battery reconditioning is prohibited
	 */
	private boolean locked;
		
	/**
	 * Constructor.
	 * @param building the building with the function.
	 * @throws BuildingException if error parsing configuration.
	 */
	public PowerStorage(Building building) {
		// Call Function constructor.
		super(FunctionType.POWER_STORAGE, building);
		
		max_kWh_nameplate = buildingConfig.getFunctionCapacityDouble(building.getBuildingType(), FunctionType.POWER_STORAGE);
		
		currentMaxCap = max_kWh_nameplate;

		numModules = (int)(currentMaxCap * .9);

		r_total = r_cell * numModules * cellsPerModule;
		
		ampHours = 1000D * currentMaxCap/SECONDARY_LINE_VOLTAGE; 

		if (building.getBuildingType().contains(Building.ARRAY))
			C_rating *= 1.75;
		else if (building.getBuildingType().contains(Building.TURBINE))
			C_rating *= 1.5;
		else if (building.getBuildingType().contains(Building.WELL))
			C_rating *= 1.25;
		else
			C_rating *= 1.25;
		
		// At the start of sim, set to a random value		
		kWhStored = .5 * max_kWh_nameplate + RandomUtil.getRandomDouble(.5 * max_kWh_nameplate);		
		//logger.info("initial kWattHoursStored is " + kWattHoursStored);
		
		// update batteryVoltage
		updateVoltage();
		
	}

	/**
	 * Gets the value of the function for a named building.
	 * @param buildingName the building name.
	 * @param newBuilding true if adding a new building.
	 * @param settlement the settlement.
	 * @return value (VP) of building function.
	 * @throws Exception if error getting function value.
	 */
	public static double getFunctionValue(String buildingName, boolean newBuilding, Settlement settlement) {

		PowerGrid grid = settlement.getPowerGrid();

		double hrInSol = 1000D * PowerGrid.HOURS_PER_MILLISOL;//MarsClock.convertMillisolsToSeconds(1000D) / 60D / 60D;
		double demand = grid.getRequiredPower() * hrInSol;

		double supply = 0D;
		Iterator<Building> iStore = settlement.getBuildingManager().getBuildings(FunctionType.POWER_STORAGE).iterator();
		while (iStore.hasNext()) {
			Building building = iStore.next();
			PowerStorage store = building.getPowerStorage();//(PowerStorage) building.getFunction(PowerStorage.FUNCTION);
			double wearModifier = (building.getMalfunctionManager().getWearCondition() / 100D) * .75D + .25D;
			supply += store.currentMaxCap * wearModifier;
		}

		double existingPowerStorageValue = demand / (supply + 1D);

		//BuildingConfig config = SimulationConfig.instance().getBuildingConfiguration();
		double powerStorage = buildingConfig.getFunctionCapacityDouble(buildingName, FunctionType.POWER_STORAGE);

		double value = powerStorage * existingPowerStorageValue / hrInSol;
		if (value > 10D) value = 10D;

		return value;
	}


	/**
	 * Sets the energy stored in the building.
	 * @param kWh the stored energy (kW hour).
	 */
	public void setEnergyStored(double kWh) {

		kWhCache = kWhStored;
		kWhStored = kWh;	
		
		boolean needRecondition = false;
		
		if (!locked) {
			
			if (kWh <= 0D) {
				kWh = 0D;
				needRecondition = true;
		        // recondition once and lock it for the rest of the sol
		        locked = true;
		        timesFullyDepleted++;
			}
			
			else if (kWh < currentMaxCap / 5D) {
				
				int rand = RandomUtil.getRandomInt((int)kWh);		
				if (rand == 0) {
					needRecondition = true;
					//System.out.println("Start reconditioning. kWh : " + kWh);	
			        // recondition once and lock it for the rest of the sol
			        locked = true;
				}
			}
	
			if (needRecondition && timesFullyDepleted > 20) {
				needRecondition = false;
				timesFullyDepleted = 0;
				reconditionBattery();
			}
		}
		
		if (kWh > currentMaxCap) {
			kWh = currentMaxCap;			
		}
		
	
		updateVoltage();		
		
	}

	/***
	 * Diagnoses health and update the status of the battery
	 */
	private void diagnoseBattery() {
		if (health > 1)
			health = 1;
    	currentMaxCap = currentMaxCap * health;
    	if (currentMaxCap > max_kWh_nameplate)
    		currentMaxCap = max_kWh_nameplate;
		ampHours = 1000D * currentMaxCap/SECONDARY_LINE_VOLTAGE; 
		if (kWhStored > currentMaxCap) {
			kWhStored = currentMaxCap;		
			kWhCache = kWhStored; 
		}
	}
	
	/**
	 * Updates the terminal voltage of the battery
	 */
	private void updateVoltage() {
		//r_total = r_cell * cellsPerModule * numModules;
    	terminalVoltage = kWhStored / ampHours * 1000D;
    	if (terminalVoltage > BATTERY_MAX_VOLTAGE)
    		terminalVoltage = BATTERY_MAX_VOLTAGE;
	}
	

	/**
	 * Updates the health of the battery
	 */
	private void updateHealth() {
    	health = health * (1 - percentBatteryDegradationPerSol/100D);		
	}

	/**
	 * Reconditions the battery
	 */
	private void reconditionBattery() {
		health = health * (1 + PERCENT_BATTERY_RECONDITIONING_PER_CYCLE/100D);
		
		logger.info(building, "The grid battery has just been reconditioned.");
	}
	
	
	@Override
	public boolean timePassing(ClockPulse pulse) {
		boolean valid = isValid(pulse);
		if (valid) {
			this.time = pulse.getElapsed();
	        
	        if (pulse.isNewSol()) {
	        	locked = false;
	        	updateHealth();
	    		diagnoseBattery();
	    		updateVoltage();
	        }
		}
        return valid;
	}

	@Override
	public double getMaintenanceTime() {
		return currentMaxCap / 5D;
	}

	
	@Override
	public double getFullPowerRequired() {
//		double delta = kWhStored - kWhCache;
//		if (delta > 0 && time > 0) {
//			kWhCache = kWhStored;
//			return delta/time/HOURS_PER_MILLISOL; 
//		}
//		else
			return 0;
	}

	/**
	 * Gets the building's current max storage capacity
	 * (Note : this accounts for the battery degradation over time)
	 * @return capacity (kWh).
	 */
	public double getCurrentMaxCapacity() {
		return currentMaxCap;
	}

	/**
	 * Gets the building's stored energy.
	 * @return energy (kW hr).
	 */
	public double getkWattHourStored() {
		return kWhStored;
	}
	
	public double getAmpHourRating() {
		return ampHours;
	}
	
	public double getTerminalVoltage() {
		return terminalVoltage;
	}
	
	public void setTerminalVoltage(double value) {
		terminalVoltage = value;
	}
	
	public double getBatteryHealth() {
		return health;
	}
	
	public double geCRating() {
		return C_rating;
	}

	public double getResistance() {
		return r_total;
	}
}
