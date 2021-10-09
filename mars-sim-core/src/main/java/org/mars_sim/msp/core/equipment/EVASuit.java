/*
 * Mars Simulation Project
 * EVASuit.java
 * @date 2021-10-03
 * @author Scott Davis
 */
package org.mars_sim.msp.core.equipment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;

import org.mars_sim.msp.core.LifeSupportInterface;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResourceUtil;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.CompositionOfAir;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.SystemType;
import org.mars_sim.msp.core.time.ClockPulse;

/**
 * The EVASuit class represents an EVA suit which provides life support for a
 * person during a EVA operation.
 * 
 * According to https://en.wikipedia.org/wiki/Space_suit, 
 * 
 * Generally speaking, in order to supply enough oxygen for respiration, a space suit 
 * using pure oxygen should have a pressure of about 
 * 
 * (A) 32.4 kPa (240 Torr; 4.7 psi), 
 * 
 *     which is equal to the 20.7 kPa (160 Torr; 3.0 psi) partial pressure of oxygen 
 *     in the Earth's atmosphere at sea level, 
 * 
 * (B) plus 5.31 kPa (40 Torr; 0.77 psi) CO2 and 6.28 kPa (47 Torr; 0.91 psi) water vapor 
 *     pressure, 
 *     
 * both of which must be subtracted from the alveolar pressure to get alveolar oxygen 
 * partial pressure in 100% oxygen atmospheres, by the alveolar gas equation.
 *  
 * According to https://en.wikipedia.org/wiki/Mars_suit#Breathing for a Mars suit, the
 * absolute minimum safe O2 requirement is a partial pressure of 11.94 kPa (1.732 psi) 
 * 	 
 * In contrast, The Russian Orlan spacesuit system operates at 40.0 kPa (5.8 psia). 
 * On the other hand, the U.S. EMU system operates at 29.6 kPa (4.3 psia) of oxygen, 
 * with traces of CO2 and water vapor.
 * 
 * The Russian EVA preparation protocol includes a 30-minute oxygen pre-breathe in 
 * the Orlan spacesuit at a pressure of 73 kPa (10.6 psia) to partially wash out 
 * nitrogen from crew members’ blood and tissues (Barer and Filipenkov, 1994)
 * 
 * See https://msis.jsc.nasa.gov/sections/section14.htm for more design and 
 * operational considerations.
 * 
 */
public class EVASuit extends Equipment implements LifeSupportInterface, Serializable, Malfunctionable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/* default logger. */
	private static final SimLogger logger = SimLogger.getLogger(EVASuit.class.getName());

	// Static members
	public static String TYPE = SystemType.EVA_SUIT.getName();

	public static String GOODTYPE = "EVA Gear";
	
	public static final int o2 = ResourceUtil.oxygenID; 
	public static final int h2o = ResourceUtil.waterID;
	public static final int co2 = ResourceUtil.co2ID;
	
	/** capacity (kg). */
	public static final double CAPACITY = 250;
	/** Total gas tank volume of EVA suit (Liter). */
	public static final double TOTAL_VOLUME = 3.9D;
	/** Oxygen capacity (kg.). */
	private static final double OXYGEN_CAPACITY = 1D;
	/** CO2 capacity (kg.). */
	private static final double CO2_CAPACITY = 1D;
	/** Water capacity (kg.). */
	private static final double WATER_CAPACITY = 4D;
	/** Typical O2 air pressure (Pa) inside EVA suit is set to be 20.7 kPa. */
	private static final double NORMAL_AIR_PRESSURE = 17;// 20.7; // CompositionOfAir.SKYLAB_TOTAL_AIR_PRESSURE_kPA; // 101325D;
	/** Normal temperature (celsius). */
	private static final double NORMAL_TEMP = 25D;
	/** The wear lifetime value of 334 Sols (1/2 orbit). */
	private static final double WEAR_LIFETIME = 334_000;
	/** The maintenance time of 20 millisols. */
	private static final double MAINTENANCE_TIME = 20D;
	
	private static double min_o2_pressure;
	/** The full O2 partial pressure if at full tank. */
	private static double fullO2PartialPressure;
	/** The nominal mass of O2 required to maintain the nominal partial pressure of 20.7 kPa (3.003 psi)  */
	private static double massO2NominalLimit;
	/** The minimum mass of O2 required to maintain right above the safety limit of 11.94 kPa (1.732 psi)  */
	private static double massO2MinimumLimit;
	/** Unloaded mass of EVA suit (kg.). The combined total of the mass of all parts. */
	public static double emptyMass;
	
	// Data members
	/** A list of resource id's it can hold. */
	private List<Integer> resourceIDs = new ArrayList<>();
	/** A list of Amount Resources it can hold. */
	private List<AmountResource> resourceARs = new ArrayList<>();
	/** A list of the amount of the resources it can hold. */	
	private List<Double> quantities = new ArrayList<>();
	/** A list of the capacity of the resources it has. */	
	private List<Double> capacities = new ArrayList<>();
	
	/** The equipment's malfunction manager. */
	private MalfunctionManager malfunctionManager;

	static {
		 
		 for (String p: ItemResourceUtil.EVASUIT_PARTS) {
			 emptyMass += ItemResourceUtil.findItemResource(p).getMassPerItem();
		 }
		 
		 PersonConfig personConfig = SimulationConfig.instance().getPersonConfig();
		 min_o2_pressure = personConfig.getMinSuitO2Pressure();
		 
		 fullO2PartialPressure = CompositionOfAir.KPA_PER_ATM * OXYGEN_CAPACITY 
				 / CompositionOfAir.O2_MOLAR_MASS * CompositionOfAir.R_GAS_CONSTANT / TOTAL_VOLUME;

		 massO2MinimumLimit = min_o2_pressure / fullO2PartialPressure * OXYGEN_CAPACITY;
		 
		 massO2NominalLimit = NORMAL_AIR_PRESSURE / min_o2_pressure * massO2MinimumLimit;
		 
		 logger.config(" EVA suit's unloaded weight : " + Math.round(emptyMass*1_000.0)/1_000.0 + " kg");
		 logger.config("      Total gas tank volume : " + Math.round(TOTAL_VOLUME*100.0)/100.0 + "L"); 
		 logger.config("               Full Tank O2 : " + Math.round(fullO2PartialPressure*100.0)/100.0 + " kPa -> "
				 		+ OXYGEN_CAPACITY + "    kg - Maximum tank pressure");
		 logger.config("                 Nomimal O2 : " + NORMAL_AIR_PRESSURE + "  kPa -> "
				 		+ Math.round(massO2NominalLimit*10_000.0)/10_000.0  + " kg - Suit target pressure");
		 logger.config("                 Minimum O2 : " + Math.round(min_o2_pressure*100.0)/100.0 + " kPa -> "
				 		+ Math.round(massO2MinimumLimit*10_000.0)/10_000.0  + " kg - Safety limit");
		 
			// 66.61 kPa -> 1      kg (full tank O2 pressure)
			// 20.7  kPa -> 0.3107 kg 
			// 17    kPa -> 0.2552 kg (target O2 pressure)
			// 11.94 kPa -> 0.1792 kg (min O2 pressure)
	}
	
	/**
	 * Constructor.
	 * @param name 
	 * 
	 * @param settlement the location of the EVA suit.
	 * @throws Exception if error creating EVASuit.
	 */
	public EVASuit(String name, Settlement settlement) {

		// Use Equipment constructor.
		super(name, TYPE, settlement);
	
		// Add scope to malfunction manager.
		malfunctionManager = new MalfunctionManager(this, WEAR_LIFETIME, MAINTENANCE_TIME);
		malfunctionManager.addScopeString(TYPE);
		malfunctionManager.addScopeString(FunctionType.LIFE_SUPPORT.getName());

		// Set the empty mass of the EVA suit in kg.
		setBaseMass(emptyMass);

//		resourceIDs = new ArrayList<>();
		resourceIDs.add(0, o2); 
		resourceIDs.add(1, h2o);
		resourceIDs.add(2, co2);
		 
//		resourceARs = new ArrayList<>();
		resourceARs.add(0, ResourceUtil.oxygenAR); 
		resourceARs.add(1, ResourceUtil.waterAR);
		resourceARs.add(2, ResourceUtil.carbonDioxideAR);
		
//		quantities = new ArrayList<>();
		quantities.add(0.0); 
		quantities.add(0.0);
		quantities.add(0.0);
		
//		capacities = new ArrayList<>();
		capacities.add(0, OXYGEN_CAPACITY); 
		capacities.add(1, WATER_CAPACITY);
		capacities.add(2, CO2_CAPACITY);
	}
	
	/**
	 * Gets a list of supported resources
	 * 
	 * @param index
	 * @return a list of resources
	 */
	public List<Integer> getResourceIDs() {
		return resourceIDs;
	}
	
	
	/**
	 * Gets the id of a particular resource
	 * 
	 * @param index
	 * @return Amount Resource id
	 */
	public int getResourceID(int index) {
		return resourceIDs.get(index);
	}
	
	/**
	 * Gets the AmountResource of a particular resource
	 * 
	 * @param index
	 * @return Amount Resource
	 */
	public AmountResource getResourceAR(int index) {
		return resourceARs.get(index);
	}
	
	/**
	 * Gets the amount in quantity of a particular resource
	 * 
	 * @param index
	 * @return quantity
	 */
	public double getQuanity(int index) {
		return quantities.get(index);
	}
	
	/**
	 * Sets the amount in quantity of a particular resource
	 * 
	 * @param index
	 * @param quantity
	 */
	public void setQuanity(int index, double quantity) {
		quantities.set(index, quantity);
	}
	
	/**
	 * Is this resource supported ?
	 * 
	 * @param resource
	 * @return true if this resource is supported
	 */
	public boolean isResourceSupported(int resource) {
		for (int r: resourceIDs) {
			if (r == resource)
				return true;
		}
		
		return false;
	}
	
	/**
	 * Gets the index of a particular resource
	 * 
	 * @param resource
	 * @return index
	 */
	public int getIndex(int resource) {
		if (resourceIDs != null && !resourceIDs.isEmpty()) {
			for (int r: resourceIDs) {
				if (r == resource)
					return resourceIDs.indexOf(r);
			}
		}
		
		return -1;
	}
	
	/**
	 * Stores the resource
	 * 
	 * @param resource
	 * @param quantity
	 * @return excess quantity that cannot be stored
	 */
	@Override
	public double storeAmountResource(int resource, double quantity) {
		int index = getIndex(resource);
		
		if (index != -1) {
			
			double newQ = getQuanity(index) + quantity;
			String name = ResourceUtil.findAmountResourceName(resource);
			
			if (newQ > getTotalCapacity()) {
				double excess = newQ - getTotalCapacity();
				logger.warning(this, "Storage is full. Excess " + Math.round(excess * 10.0)/10.0 + " kg " + name + ".");
				setQuanity(index, getTotalCapacity());
				return excess;
			}
			else {
				setQuanity(index, newQ);
//				logger.config(this, "Just added " + Math.round(quantity * 10.0)/10.0 + " kg " + name + ".");
				return 0;
			}
		}
		else {
			String name = ResourceUtil.findAmountResourceName(resource);
			logger.warning(this, "Invalid request for storing " + name + ".");
			return quantity;
		}
	}
	
	/**
	 * Retrieves the resource 
	 * 
	 * @param resource
	 * @param quantity
	 * @return quantity that cannot be retrieved
	 */
	@Override
	public double retrieveAmountResource(int resource, double quantity) {
		int index = getIndex(resource);
		
		if (index != -1) {
			double q = getQuanity(index);
			double diff = q - quantity;
			if (diff < 0) {
				String name = ResourceUtil.findAmountResourceName(resource);
				logger.warning(this, 10_000L, "Just retrieved all " + q + " kg of " 
						+ name + ". Lacking " + Math.round(-diff * 10.0)/10.0 + " kg.");
				setQuanity(index, 0);
				return diff;
			}
			else {
				setQuanity(index, diff);
				return 0;
			}
		}
		else {
			String requestedResource = ResourceUtil.findAmountResourceName(resource);
			logger.warning(this, "No such resource. Cannot retrieve " 
					+ Math.round(quantity* 10.0)/10.0 + " kg "+ requestedResource + ".");
			return quantity;
		}
	}
	
	/**
	 * Gets the capacity of a particular amount resource
	 * 
	 * @param resource
	 * @return capacity
	 */
	@Override
	public double getAmountResourceCapacity(int resource) {
		int index = getIndex(resource);
		
		if (index != -1) {
			return getCapacity(index);
		}
		else {
			return 0;
		}
	}
	
	/**
	 * Obtains the remaining storage space of a particular amount resource
	 * 
	 * @param resource
	 * @return quantity
	 */
	@Override
	public double getAmountResourceRemainingCapacity(int resource) {
		int index = getIndex(resource);
		
		if (index != -1) {
			return getCapacity(index) - this.getQuanity(index);
		}
		else {
			return 0;
		}
	}
	
	/**
	 * Gets the amount resource stored
	 * 
	 * @param resource
	 * @return quantity
	 */
	@Override
	public double getAmountResourceStored(int resource) {
		int index = getIndex(resource);
		
		if (index != -1) {
			return getQuanity(index);
		}
		else {
			return 0;
		}
	}
	
	/**
     * Gets the total capacity of resource that this container can hold.
     * @return total capacity (kg).
     */
	@Override
    public double getTotalCapacity() {
        return CAPACITY;
    }
	
	/**
     * Gets the capacity of this resource that this container can hold.
     * @return capacity (kg).
     */
    public double getCapacity(int resource) {
        return capacities.get(resource);
    }
    
	/**
	 * Gets the total weight of the stored resources
	 * 
	 * @return
	 */
    @Override
	public double getStoredMass() {
		double total = 0;
		if (quantities != null && !quantities.isEmpty()) {
			for (double m: quantities) {
				total += m;
			}
		}
		return total;
	}
	
	/**
	 * Is this suit empty ?
	 * 
	 * @param resource
	 * @return
	 */
	@Override
	public boolean isEmpty(int resource) {
		if (quantities != null && !quantities.isEmpty()) {
			for (double m: quantities) {
				if (m != 0)
					return false;
			}
		}
		return true;
	}

	/**
	 * Is this suit empty ? 
	 * 
	 * @param brandNew true if it needs to be brand new
	 * @return
	 */
	@Override
	public boolean isEmpty(boolean brandNew) {
		if (brandNew) {
			if (getLastOwnerID() == -1) {				
				return true;
			}
		}

		if (quantities != null && !quantities.isEmpty()) {
			for (double m: quantities) {
				if (m > 0)
					return false;
			}
		}
		
		return true;
	}	

	@Override
	public boolean isBrandNew() {
		if (getLastOwnerID() == -1)
			return true;
		return false;
	}

	@Override
	public boolean isUsed() {
		return !isBrandNew();
	}

	@Override
	public boolean hasContent() {
		if (quantities != null && !quantities.isEmpty()) {
			for (double m: quantities) {
				if (m > 0)
					return true;
			}
		}
		return false;
	}
	
	/**
	 * Gets the unit's malfunction manager.
	 * 
	 * @return malfunction manager
	 */
	public MalfunctionManager getMalfunctionManager() {
		return malfunctionManager;
	}

	/**
	 * Returns true if life support is working properly and is not out of oxygen or
	 * water.
	 * 
	 * @return true if life support is OK
	 * @throws Exception if error checking life support.
	 */
	public boolean lifeSupportCheck() {
		// boolean result = true;
		try {
			// With the minimum required O2 partial pressure of 11.94 kPa (1.732 psi), the minimum mass of O2 is 0.1792 kg 
//			String name = getOwner().getName();
			if (getAmountResourceStored(o2) <= massO2MinimumLimit) {				
				logger.log(getOwner(), Level.WARNING, 30_000,
						"Less than 0.1792 kg oxygen (below the safety limit).");
				return false;
			}
			
			if (getAmountResourceStored(h2o) <= 0D) {				
				logger.log(getOwner(), Level.WARNING, 30_000, 
						"Ran out of water.");
//				return false;
			}
			
			if (malfunctionManager.getOxygenFlowModifier() < 100D) {
				logger.log(getOwner(), Level.WARNING, 30_000,
						"Oxygen flow sensor malfunction.", null);
				return false;
			}
//			if (malfunctionManager.getWaterFlowModifier() < 100D) {
//				LogConsolidated.log(Level.INFO, 5000, sourceName,
//						"[" + this.getLocationTag().getLocale() + "] " 
//								+ person.getName() + "'s " + this.getName() + "'s water flow sensor detected malfunction.", null);
//				return false;
//			}

			double p = getAirPressure();
			if (p > PhysicalCondition.MAXIMUM_AIR_PRESSURE || p <= min_o2_pressure) {
				logger.log(getOwner(), Level.WARNING, 30_000,
						"Detected improper o2 pressure at " + Math.round(p * 100.0D) / 100.0D + " kPa.");
				return false;
			}
			double t = getTemperature();
			if (t > NORMAL_TEMP + 15 || t < NORMAL_TEMP - 20) {
				logger.log(getOwner(), Level.WARNING, 30_000,
						"Detected improper temperature at " + Math.round(t * 100.0D) / 100.0D + " deg C");
				return false;
			}
		} catch (Exception e) {
          	logger.log(Level.SEVERE, "Cannot finish life support check: "+ e.getMessage());
		}

		return true;
	}

	/**
	 * Gets the number of people the life support can provide for.
	 * 
	 * @return the capacity of the life support system.
	 */
	public int getLifeSupportCapacity() {
		return 1;
	}

	/**
	 * Gets oxygen from system.
	 * 
	 * @param amountRequested the amount of oxygen requested from system (kg)
	 * @return the amount of oxygen actually received from system (kg)
	 * @throws Exception if error providing oxygen.
	 */
	public double provideOxygen(double amountRequested) {
		double oxygenTaken = amountRequested;
		// NOTE: Should we assume breathing in pure oxygen or trimix and heliox
		// http://www.proscubadiver.net/padi-course-director-joey-ridge/helium-and-diving/
		// May pressurize the suit to 1/3 of atmospheric pressure, per NASA aboard on
		// the ISS
		if (amountRequested > 0) {
			try {
				double oxygenLeft = getAmountResourceStored(o2);
	//			if (oxygenTaken * 100 > oxygenLeft) {
	//				// O2 is running out soon
	//				// Walk back to the building or vehicle
	//				person.getMind().getTaskManager().clearTask();//
	//				person.getMind().getTaskManager().addTask(new Relax(person));
	//			}
				
				if (oxygenTaken > oxygenLeft)
					oxygenTaken = oxygenLeft;
				if (oxygenTaken > 0)
					retrieveAmountResource(o2, oxygenTaken);
							
				// NOTE: Assume the EVA Suit has pump system to vent out all CO2 to prevent the
				// built-up. Since the breath rate is 12 to 25 per minute. Size of breath is 500 mL.
				// Percent CO2 exhaled is 4% so CO2 per breath is approx 0.04g ( 2g/L x .04 x
				// .5l).
	
				double carbonDioxideProvided = .04 * .04 * oxygenTaken;
				double carbonDioxideCapacity = getAmountResourceRemainingCapacity(co2);
				if (carbonDioxideProvided > carbonDioxideCapacity)
					carbonDioxideProvided = carbonDioxideCapacity;
	
				storeAmountResource(co2, carbonDioxideProvided);
	
			} catch (Exception e) {
				logger.log(Level.SEVERE, this.getName() + " - Error in providing O2 needs: " + e.getMessage());
			}
		}
		
		return oxygenTaken;// * (malfunctionManager.getOxygenFlowModifier() / 100D);
	}

	/**
	 * Gets water from the system.
	 * 
	 * @param waterTaken the amount of water requested from system (kg)
	 * @return the amount of water actually received from system (kg)
	 * @throws Exception if error providing water.
	 */
	public double provideWater(double waterTaken) {
		double waterLeft = getAmountResourceStored(h2o);

		if (waterTaken > waterLeft) {
			waterTaken = waterLeft;
		}

		retrieveAmountResource(h2o, waterTaken);

		return waterTaken;// * (malfunctionManager.getWaterFlowModifier() / 100D);
	}

	/**
	 * Gets the air pressure of the life support system.
	 * 
	 * @return air pressure (Pa)
	 */
	public double getAirPressure() {
		// Based on some pre-calculation, 
		// In a 3.9 liter system, 1 kg of O2 can create 66.61118 kPa partial pressure 	
		// e.g. To supply a partial oxygen pressure of 20.7 kPa, one needs at least 0.3107 kg O2	
		// With the minimum required O2 partial pressure of 11.94 kPa (1.732 psi), the minimum mass of O2 is 0.1792 kg 	
		// Note : our target o2 partial pressure is 17 kPa (not 20.7 kPa), the targeted mass of O2 is 0.2552 kg
		
		// 66.61 kPa -> 1      kg (full tank O2 pressure)
		// 20.7  kPa -> 0.3107 kg 
		// 17    kPa -> 0.2552 kg (target O2 pressure)
		// 11.94 kPa -> 0.1792 kg (min O2 pressure)
		
		double oxygenLeft = getAmountResourceStored(h2o);
		// Assuming that we can maintain a constant oxygen partial pressure unless it falls below massO2NominalLimit 
		if (oxygenLeft < massO2NominalLimit) {
			double remainingMass = oxygenLeft;
			double pp = CompositionOfAir.KPA_PER_ATM * remainingMass / CompositionOfAir.O2_MOLAR_MASS 
					* CompositionOfAir.R_GAS_CONSTANT / TOTAL_VOLUME;
			logger.log(this, getOwner(), Level.WARNING, 30_000, 
					" got " + Math.round(oxygenLeft*100.0)/100.0
						+ " kg O2 left at partial pressure of " + Math.round(pp*100.0)/100.0 + " kPa.");
			return pp;
		}
//		Note: the outside ambient air pressure is weather.getAirPressure(getCoordinates());
		return NORMAL_AIR_PRESSURE;// * (malfunctionManager.getAirPressureModifier() / 100D);	
	}

	/**
	 * Gets the temperature of the life support system.
	 * 
	 * @return temperature (degrees C)
	 */
	public double getTemperature() {
		double result = NORMAL_TEMP;// * (malfunctionManager.getTemperatureModifier() / 100D);
//		double ambient = weather.getTemperature(getCoordinates());

//		if (result < ambient) {
			// if outside temperature is higher than the EVA normally allowed temp
			// Note: Add codes to simulate the use of cooling coil to turn on cooler to
			// reduce the temperature inside the EVA suit.
			// if cooling coil malfunction, then return ambient only
//			return result;
//		}

		return result;
	}

//	/**
//	 * Checks to see if the inventory is at full capacity with oxygen and water.
//	 * 
//	 * @return true if oxygen and water stores at full capacity
//	 * @throws Exception if error checking inventory.
//	 */
//	public boolean isFullyLoaded() {
//		boolean result = true;
//
//		double oxygen = getInventory().getAmountResourceStored(ResourceUtil.oxygenID, false);
//		if (oxygen != OXYGEN_CAPACITY) {
//			result = false;
//		}
//
//		double water = getInventory().getAmountResourceStored(ResourceUtil.waterID, false);
//		if (water != WATER_CAPACITY) {
//			result = false;
//		}
//		
//		return result;
//	}

	/**
	 * Time passing for EVA suit.
	 *
	 * @param pulse the amount of clock pulse passing (in millisols)
	 * @throws Exception if error during time.
	 */
	public boolean timePassing(ClockPulse pulse) {
		if (!isValid(pulse)) {
			return false;
		}
		
		Unit container = getContainerUnit();
		if (container instanceof Person) {
			Person person = (Person) container;
			if (!person.getPhysicalCondition().isDead()) {
				malfunctionManager.activeTimePassing(pulse.getElapsed());	
			}
		}

		malfunctionManager.timePassing(pulse);
		return true;
	}

	/**
	 * Gets a list of people affected by this equipment
	 * @return Collection<Person>
	 */
	@Override
	public Collection<Person> getAffectedPeople() {
		return super.getAffectedPeople();
	}

	@Override
	public String getNickName() {
		return getName();
	}

	@Override
	public String getImmediateLocation() {
		return getLocationTag().getImmediateLocation();
	}

	@Override
	public String getLocale() {
		return getLocationTag().getLocale();
	}

	@Override
	public Building getBuildingLocation() {
		return getContainerUnit().getBuildingLocation();
	}

	@Override
	public Settlement getAssociatedSettlement() {
		Settlement s = getContainerUnit().getAssociatedSettlement();
		if (s == null) s = super.getAssociatedSettlement();
		return s;
	}

	
//	public boolean equals(Object obj) {
//		if (this == obj) return true;
//		if (obj == null) return false;
//		if (this.getClass() != obj.getClass()) return false;
//		EVASuit e = (EVASuit) obj;
//		return this.getNickName().equals(e.getNickName());
//	}
	
	private Person getOwner() {
		return (Person)getLastOwner();
	}
	
	public void destroy() {
		malfunctionManager = null;
	}
}

