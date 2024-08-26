/*
 * Mars Simulation Project
 * AirComposition.java
 * @date 2024-07-10
 * @author Barry Evans
 */
package com.mars_sim.core.air;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.mars_sim.core.equipment.ResourceHolder;
import com.mars_sim.core.person.PersonConfig;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.time.ClockPulse;

/**
 * Models the composition of air within a containment. Holds the specific gas composition and pressure.
 */
public class AirComposition implements Serializable {
	// Note : Gas volumes are additive. If you mix some volumes of oxygen and
	// nitrogen, final volume will equal sum of
	// volumes, also final mass will equal sum of masses.

	private static final long serialVersionUID = 1L;

	/**
     * Properties of a specific gas.
     */
    static final public class GasDetails implements Serializable {
    	private double percent;
    	private double partialPressure;
    	private double numMoles;
    	private double mass;
    	private double standardMoles;
    
    	private static final long serialVersionUID = 1L;

    	public double getPercent() {
    		return percent;
    	}
    
        public double getPartialPressure() {
            return partialPressure;
        }
    
        public double getMass() {
            return mass;
        }
    }
	private static final int MILLISOLS_PER_UPDATE = 2;
	
	// See https://en.wikipedia.org/wiki/Gas_constant
	// R = 8.314 J/(mol C); 
	public static final double R_GAS_CONSTANT = 0.082057338D; // [ in L atm K^−1 mol^−1 ]
	public static final double MB_PER_ATM = 1013.2501D;
	public static final double KPA_PER_ATM = 101.32501D;
	public static final double PSI_PER_ATM = 14.696D;
	public static final double KPA_PER_PSI = KPA_PER_ATM / PSI_PER_ATM; // ~ 6.8947
	
	// Future: for EVA suit, may opt for 8.2 psi (56.54 kPa) suit with 34% O2 (19.222 kPa)
	
	/**
     * The % of air composition used by US Skylab Hab Modules. 5 psi or 340 mb is
     * the overall pressure rating.
     */
    // See http://www.collectspace.com/ubb/Forum29/HTML/001309.html
    // The partial pressures of each gas are in atm
    private static final double CO2_PARTIAL_PRESSURE = 0.5D / MB_PER_ATM;
    private static final double ARGON_PARTIAL_PRESSURE = 0.1D / MB_PER_ATM;
    private static final double N2_PARTIAL_PRESSURE = 120D / MB_PER_ATM;
    // For O2, it's 200 / 1013.2501 * 14.696 =  2.9008 psi or ~ 20 kPa
    private static final double O2_PARTIAL_PRESSURE = 200D / MB_PER_ATM;
    private static final double H2O_PARTIAL_PRESSURE = 19.4D / MB_PER_ATM;
    
    private static final double H2O_MOLAR_MASS = 18.02D / 1000D;
    private static final double O2_MOLAR_MASS = 32D / 1000D;
    private static final double N2_MOLAR_MASS = 28.02D / 1000D;
    private static final double ARGON_MOLAR_MASS = 39.948D / 1000D;
    private static final double CO2_MOLAR_MASS = 44.0095D / 1000D;
    
	private static final double CALCULATE_FREQUENCY = 2D;
	private static final double GAS_CAPTURE_EFFICIENCY = .95D;

	public static final double C_TO_K = 273.15;

    private static double o2Consumed;
	private static double cO2Expelled;
	private static double moistureExpelled;

	private double fixedVolume; // [in liter]; Note: 1 Cubic Meter = 1,000 Liters
	private double totalPressure; // in atm
	private double totalMass; // in kg
	private double totalNumMoles; // in mol

	private double accumulatedTime;
	
	private Map<Integer, GasDetails> gases = new HashMap<>();

	/**
	 * Constructor.
	 * 
	 * @param t
	 * @param vol
	 */
	public AirComposition(double t, double vol) {

		// Part 1 : set up initial conditions at the start of sim
		initialiseGas(ResourceUtil.co2ID);
		initialiseGas(ResourceUtil.argonID);
		initialiseGas(ResourceUtil.nitrogenID);
		initialiseGas(ResourceUtil.oxygenID);
		initialiseGas(ResourceUtil.waterID);
		
		// Part 2 : calculate total # of moles, total mass and total pressure
		fixedVolume = vol;
		for(Entry<Integer, GasDetails> g : gases.entrySet()) {
			int gasId = g.getKey();
			double molecularMass = getMolecularMass(gasId);
			GasDetails gas = g.getValue();

			double p = gas.partialPressure;
			double nm = p * vol / R_GAS_CONSTANT / t;
			double m = molecularMass * nm;

			gas.numMoles = nm;
			gas.standardMoles = nm;
			gas.mass = m;

			totalMass += m;
			totalPressure += p;
		}
		
		// Part 3 : calculate for each building the percent composition
		updateGasPercentage();
	}

	/**
	 * Initialises a new gas to the composition.
	 */
	private void initialiseGas(int gasId) {
		GasDetails gas = new GasDetails();
		gas.partialPressure = getIdealPressure(gasId);

		gases.put(gasId, gas);
	}

	/**
	 * Updates all the individual Gas percentages based on the partial & total pressure.
	 */
	private void updateGasPercentage() {
		for (GasDetails gd : gases.values()) {
			// calculate for each gas the % composition
			gd.percent = gd.partialPressure / totalPressure * 100D;
		}
	}

	/**
	 * Calculates the impact of time passing.
	 * 
	 * @param building
	 * @param pulse
	 */
	public void timePassing(Building building, ClockPulse pulse) {
		double time = pulse.getElapsed();
		double tt = building.getCurrentTemperature();

		if (tt > -40 && tt < 40) {
			double t = AirComposition.C_TO_K + tt;

			accumulatedTime += time;

			double newCheckPeriod = CALCULATE_FREQUENCY * time;
			
			if (accumulatedTime >= newCheckPeriod) {
				// Compute the remaining accumulatedTime
				accumulatedTime -= newCheckPeriod;
				
				int numPeople = building.getNumPeople();

				calcPersonImpact(t, numPeople,  accumulatedTime);
			}

			if (pulse.isNewIntMillisol()
					&& pulse.getMarsTime().getMillisolInt() % MILLISOLS_PER_UPDATE == 0) {
				monitorGases(building, t);
			}
		}
	}

	/**
	 * Updates each gas for occupants.
	 * 
	 * @param t Current temperature
	 * @param numPeople Number of people in using air
	 * @param time The time span of the gas consumption
	 */
	private void calcPersonImpact(double t, int numPeople, double time) {
		
		totalPressure = 0;
		totalMass = 0;
		
		for (Entry<Integer, GasDetails> g : gases.entrySet()) {
			int gasId = g.getKey();
			GasDetails gas = g.getValue();

			// Part 1 : calculate for each gas the partial pressure and # of moles
			double m = gas.mass;
			if (gasId == ResourceUtil.co2ID) {
				m += numPeople * cO2Expelled * time;
			} else if (gasId == ResourceUtil.oxygenID) {
				m -= numPeople * o2Consumed * time;
			} else if (gasId == ResourceUtil.waterID) {
				m += numPeople * moistureExpelled * time;
			}

			// Divide by molecular mass to convert mass to # of moles
			// note the kg/mole are as indicated as each gas have different amu
			double mm = getMolecularMass(gasId);
			double nm = 0;
			if (mm > 0)
				nm = m / mm;
			
			double p = nm * AirComposition.R_GAS_CONSTANT * t / fixedVolume;

			if (p < 0)
				p = 0;
			if (nm < 0)
				nm = 0;
			if (m < 0)
				m = 0;

			gas.partialPressure = p;
			gas.mass = m;
			gas.numMoles = nm;

			// Part 2
			// calculate for each building the total pressure, total # of moles and
			// percentage of composition
			totalPressure += gas.partialPressure;
			totalMass += gas.mass;
		}

		// Part 3
		// calculate for each building the percent composition
		updateGasPercentage();
	}

	/**
	 * Monitors the gases exchanges to a Resource Holder.
	 * 
	 * @param rh Source or destination of excess gas.
	 * @param t Current temperature
	 */
	public void monitorGases(ResourceHolder rh, double t) {
		totalPressure = 0;
		totalMass = 0;
		totalNumMoles = 0;
		
		for (Entry<Integer, GasDetails> g : gases.entrySet()) {
			int gasId = g.getKey();
			GasDetails gas = g.getValue();

			double pp = getIdealPressure(gasId);
			double p = gas.partialPressure;
			double tolerance = 0;
			if (pp > 0)		
				tolerance =	p / pp;

			// if this gas has BELOW 95% or ABOVE 105% the standard percentage of air
			// composition
			// if this gas has BELOW 90% or ABOVE 110% the standard percentage of air
			// composition
			if (tolerance > 1.1 || tolerance < .9) {

				double dNewMoles = gas.standardMoles - gas.numMoles;
				double molecularMass = getMolecularMass(gasId);
				double dMass = dNewMoles * molecularMass; // d_mass can be -ve

				if (dMass > 0) {
					rh.retrieveAmountResource(gasId, dMass);
				}
				else { // too much gas, need to recapture it; d_mass is less than 0
					double recaptured = -dMass * GAS_CAPTURE_EFFICIENCY;
					if (recaptured > 0) {
						rh.storeAmountResource(gasId, recaptured);								
					}						
				}

				double newM = gas.mass + dMass;
				double newMoles = 0;

				if (newM < 0) {
					newM = 0;
				}
				else if (molecularMass > 0) {
					newMoles = newM / molecularMass;
				}

				gas.partialPressure = newMoles * R_GAS_CONSTANT * t / fixedVolume;
				gas.mass = newM;
				gas.numMoles = newMoles;
            }

            // Update total
            totalPressure += gas.partialPressure;
            totalMass += gas.mass;
            totalNumMoles += gas.numMoles;
		}

		updateGasPercentage();
	}

	/**
 	 * Releases or recaptures numbers of moles of a certain gas to a given building.
 	 *
 	 * @param volume   volume change in the building
 	 * @param isReleasing positive if releasing, negative if recapturing
 	 * @param rh        local store of gases
 	*/
	public void releaseOrRecaptureAir(double volume, boolean isReleasing, ResourceHolder rh) {
		for (Entry<Integer,GasDetails> g : gases.entrySet()) {
			int gasId = g.getKey();
			GasDetails gas = g.getValue();

			double dMoles = gas.numMoles * volume / fixedVolume;

			double molecularMass = getMolecularMass(gasId);
			double dMass = molecularMass * dMoles;

			if (isReleasing) {
				gas.numMoles = gas.numMoles + dMoles;
				gas.mass  = gas.mass + dMass;
				if (dMass > 0) {
					rh.retrieveAmountResource(gasId, dMass);
				}
			}
			else { // recapture
				gas.numMoles = gas.numMoles - dMoles;
				gas.mass  = gas.mass - dMass;
				if (dMass > 0) {
					rh.storeAmountResource(gasId, dMass * AirComposition.GAS_CAPTURE_EFFICIENCY);	
				}
			}

			if (gas.numMoles < 0)
				gas.numMoles = 0;
			if (gas.mass< 0)
				gas.mass = 0;
		}
	}

	/**
	 * Gets the combined air pressure of all gas in the air [in atm].
	 * 
	 * @return
	 */
    public double getTotalPressure() {
        return totalPressure;
    }

    /**
     * Gets the total mass of the air [in kg].
     * 
     * @return
     */
    public double getTotalMass() {
        return totalMass;
    }

   /**
    * Gets the total number of moles of the air.
    * 
    * @return
    */
    public double getTotalNumMoles() {
    	return totalNumMoles;
    }
    
    public GasDetails getGas(int gasId) {
        return gases.get(gasId);
    }

	
    /**
     * Initializes constants.
     * 
     * @param personConfig
     */
	public static void initializeInstances(PersonConfig personConfig) {
		o2Consumed = personConfig.getHighO2ConsumptionRate() / 1000D; // divide by 1000 to convert to [kg/millisol]

		cO2Expelled = personConfig.getCO2ExpelledRate() / 1000D; // [in kg/millisol] 1.0433 kg or 2.3 pounds CO2 per day
																	// for high metabolic activity.

		// If we are breathing regular air, at about ~20-21% 02, we use about 5% of that
		// O2 and exhale the by product of
		// glucose utilization CO2 and the balance of the O2, so exhaled breath is about
		// 16% oxygen, and about 4.75 % CO2.
		moistureExpelled = .8 / 1000D; // ~800 ml through breathing, sweat and skin per sol, divide by 1000 to convert								// to [kg/millisol]
	}

	/**
	 * Gets the molecular mass of a given gas.
	 * 
	 * @param gasId
	 * @return
	 */
	private static final double getMolecularMass(int gasId) {
		// Can't use a switch because ResourceUtil ids are not constant, e.g. not final static.
		double result;
		if (gasId == ResourceUtil.co2ID)
			result = CO2_MOLAR_MASS;
		else if (gasId == ResourceUtil.argonID)
			result = ARGON_MOLAR_MASS;
		else if (gasId == ResourceUtil.nitrogenID)
			result = N2_MOLAR_MASS;
		else if (gasId == ResourceUtil.oxygenID)
			result = O2_MOLAR_MASS;
		else if (gasId == ResourceUtil.waterID)
			result = H2O_MOLAR_MASS;
		else {
			String g = ResourceUtil.findAmountResourceName(gasId);
			throw new IllegalArgumentException("Unknown gas '" + g + "' id=" + gasId);
		}
		return result;
	}

	/**
	 * Gets the ideal pressure for a particular gas.
	 * 
	 * @param gasId
	 * @return
	 */
	private static final double getIdealPressure(int gasId) {
		// Can't use a switch because ResourceUtil ids are not constant, e.g. not final static.
		double result;
		if (gasId == ResourceUtil.co2ID)
			result = CO2_PARTIAL_PRESSURE;
		else if (gasId == ResourceUtil.argonID)
			result = ARGON_PARTIAL_PRESSURE;
		else if (gasId == ResourceUtil.nitrogenID)
			result = N2_PARTIAL_PRESSURE;
		else if (gasId == ResourceUtil.oxygenID)
			result = O2_PARTIAL_PRESSURE;
		else if (gasId == ResourceUtil.waterID)
			result = H2O_PARTIAL_PRESSURE;
		else {
			String g = ResourceUtil.findAmountResourceName(gasId);
			throw new IllegalArgumentException("Unknown gas '" + g + "' id=" + gasId);
		}
		return result;
	}

    /**
     * Calculates the O2 pressure for a quantity in a fixed volume.
     * 
     * @param gasVol Amount of O2 present
     * @param totalVol Total volume of the container 
     */
    public static final double getOxygenPressure(double gasVol, double totalVol) {
    	return KPA_PER_ATM * gasVol / O2_MOLAR_MASS * R_GAS_CONSTANT / totalVol;
    }
}