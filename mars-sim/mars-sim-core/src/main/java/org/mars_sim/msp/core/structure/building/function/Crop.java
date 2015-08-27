/**
 * Mars Simulation Project
 * Crop.java
 * @version 3.08 2015-04-08
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LifeSupportType;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.MasterClock;


/**
 * The Crop class is a food crop grown on a farm.
 */
public class Crop
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(Crop.class.getName());

	// TODO Static members of crops should be initialized from some xml instead of being hard coded.
	/** Amount of grey water needed per harvest mass. */
	public static final double WASTE_WATER_NEEDED = 5D;
	/** Amount of carbon dioxide needed per harvest mass. */
	public static final double CARBON_DIOXIDE_NEEDED = 2D;
	/** Amount of oxygen needed per harvest mass. */
	public static final double OXYGEN_NEEDED = 2D;

	public static final double SOIL_NEEDED_PER_SQM = 1D;

	//  Be sure that FERTILIZER_NEEDED is static, but NOT "static final"
	public static double FERTILIZER_NEEDED = 0.0001D;  // a very minute amount needed per unit time, called if grey water is not available

	public static final double FERTILIZER_NEEDED_PER_SQM = 1D; // amount needed when planting a new crop

	public static final double WATER_RECLAMATION_RATE = .8D;
	public static final double OXYGEN_GENERATION_RATE = .9D;
	public static final double CO2_GENERATION_RATE = .9D;

	// public static final double SOLAR_IRRADIANCE_TO_PAR_RATIO = .42; // only 42% are EM within 400 to 700 nm
	// see http://ccar.colorado.edu/asen5050/projects/projects_2001/benoit/solar_irradiance_on_mars.htm#_top

	public static final double WATT_TO_PHOTON_CONVERSION_RATIO = 4.609; // in u mol / m2 /s / W m-2 for Mars only

	public static final double kW_PER_HPS = .4;
	public static final double VISIBLE_RADIATION_HPS = .4; // high pressure sodium (HPS) lamps efficiency
	public static final double BALLAST_LOSS_HPS = .1; // for high pressure sodium (HPS)
	public static final double NON_VISIBLE_RADIATION_HPS = .37; // for high pressure sodium (HPS)
	public static final double CONDUCTION_CONVECTION_HPS = .13; // for high pressure sodium (HPS)
	public static final double LOSS_AS_HEAT_HPS = NON_VISIBLE_RADIATION_HPS*.75 + CONDUCTION_CONVECTION_HPS/2D;
	//public static final double MEAN_DAILY_PAR = 237.2217D ; // in [mol/m2/day]
	// SurfaceFeatures.MEAN_SOLAR_IRRADIANCE * 4.56 * (not 88775.244)/1e6 = 237.2217

	// TODO Crop phases should be an internationalizable enum.
	public static final String PLANTING = "Planting";
	public static final String GERMINATION = "Germination"; // include initial sprouting of a seedling
	public static final String GROWING = "Growing";
	// TODO: add FLOWERING phase
	public static final String HARVESTING = "Harvesting";
	public static final String FINISHED = "Finished";

    private static final double T_TOLERANCE = 3D;

    private static final double PERCENT_IN_GERMINATION_PHASE = 5; // Assuming the first 5% of a crop's life is in germination phase


	// Data members
    /** Current sol since the start of sim. */
	private int solCache = 1;
	/** Current sol of month. */
	private int currentSol = 1;
	/** Maximum possible food harvest for crop. (kg) */
	private double maxHarvest;
	/** Required work time for planting (millisols). */
	private double plantingWorkRequired;
	/** Required work time to tend crop daily (millisols). */
	private double dailyTendingWorkRequired;
	/** Required work time for harvesting (millisols). */
	private double harvestingWorkRequired;
	/** Completed work time in current phase (millisols). */
	private double currentPhaseWorkCompleted;
	/** Actual food harvest for crop. (kg) */
	private double actualHarvest;
	/** max possible daily harvest for crop. (kg) */
	private double maxHarvestinKgPerDay;
	/** Growing phase time completed thus far (millisols). */
	private double growingTimeCompleted; // this parameter is initially randomly generated at the beginning of a sim for a growing crop
	/** the area the crop occupies in square meters. */
	private double growingArea;
	/** Growing day in number of millisols. */
	private double cropGrowingTime;
	/** Growing day in number of sols. */
	private double cropGrowingDay;
	private double fractionalGrowthCompleted;
	private double t_initial;
	private double dailyPARRequired;
	private double dailyPARCache = 0;
	private double sunlightModifierCache = 1;
	//private double timeCache = 0;

	private double lightingPower = 0; // in kW

	private double healthCondition = 0;

	/** Current phase of crop. */
	private String phase;
	private String cropName;
	/** The type of crop. */
	private CropType cropType;
	private Inventory inv;
	/** Farm crop being grown in. */
	private Farming farm;
	/** The settlement the crop is located at. */
	private Settlement settlement;
	private SurfaceFeatures surface;
	private MarsClock marsClock;
	private MasterClock masterClock;

	DecimalFormat fmt = new DecimalFormat("0.000");

	/**
	 * Constructor.
	 * @param cropType the type of crop.
	 * @param maxHarvest - Maximum possible food harvest for crop. (kg)
	 * @param farm - Farm crop being grown in.
	 * @param settlement - the settlement the crop is located at.
	 * @param newCrop - true if this crop starts in it's planting phase.
	 * @param percentGrowth
	 */
	// Called by Farming.java constructor and timePassing()
	// 2015-08-26 Added new param percentGrowth
	public Crop(CropType cropType, double growingArea, double maxHarvestinKgPerDay, Farming farm,
			Settlement settlement, boolean newCrop, double percentGrowth) {
		this.cropType = cropType;

		this.farm = farm;
		this.settlement = settlement;
		this.growingArea = growingArea;
		this.maxHarvestinKgPerDay = maxHarvestinKgPerDay;

		init(newCrop, percentGrowth);//, maxHarvestinKgPerDay);
	}

	// 2015-08-26 Added init()
	public void init(boolean newCrop, double percentGrowth) {//, double maxHarvestinKgPerDay) {

		surface = Simulation.instance().getMars().getSurfaceFeatures();
        masterClock = Simulation.instance().getMasterClock();
		marsClock = masterClock.getMarsClock();

		inv = settlement.getInventory();
		t_initial = farm.getBuilding().getInitialTemperature();

		cropName = cropType.getName();
		// cropGrowingTime in millisols
		cropGrowingTime = cropType.getGrowingTime();
		//System.out.println(cropType.getName() + " cropGrowingTime : " + cropGrowingTime);
		// cropGrowingDay in sols
		cropGrowingDay = cropGrowingTime/1000D;
		//System.out.println(cropType.getName() + " cropGrowingDay : " + cropGrowingDay);

		// 2015-04-08  Added dailyPARRequired
		dailyPARRequired = cropType.getDailyPAR();

		maxHarvest = maxHarvestinKgPerDay * cropGrowingDay;

		// Determine work required.
		plantingWorkRequired = maxHarvest *1.5D;
		dailyTendingWorkRequired = maxHarvest;
		harvestingWorkRequired = maxHarvest * 3D; // old default is 5. why?

		if (newCrop) {
			phase = PLANTING;
			//actualHarvest = 0D;
			// 2015-08-26 Added percentGrowth to account for the percent growth of a seedling
			growingTimeCompleted = percentGrowth /100D * PERCENT_IN_GERMINATION_PHASE /100D * cropGrowingTime;

	}
		else { // set up a crop's "initial" percentage of growth randomly when the simulation gets started
			growingTimeCompleted = RandomUtil.getRandomDouble(cropGrowingTime);
		//}

		fractionalGrowthCompleted = growingTimeCompleted/cropGrowingTime;

		if ( fractionalGrowthCompleted * 100D <= PERCENT_IN_GERMINATION_PHASE) {	// assuming the first 10% growing day of each crop is germination
			phase = GERMINATION;
		}
		else if ( fractionalGrowthCompleted * 100D > PERCENT_IN_GERMINATION_PHASE) {
			phase = GROWING;
		}

		actualHarvest = maxHarvest * fractionalGrowthCompleted;
		//System.out.println(cropType.getName() + " growingTimeCompleted : " + growingTimeCompleted);
		//System.out.println(cropType.getName() + " maxHarvest : " + maxHarvest);
		//System.out.println(cropType.getName() + " fractionalGrowthCompleted : " + fractionalGrowthCompleted);
		//System.out.println(cropType.getName() + " actualHarvest : " + actualHarvest);
	}

	}

	public double getLightingPower() {
		return 	lightingPower;
	}

	public double getGrowingArea() {
		return growingArea;
	}

	/**
	 * Gets the type of crop.
	 *
	 * @return crop type
	 */
	public CropType getCropType() {
		return cropType;
	}

	/**
	 * Gets the phase of the crop.
	 * @return phase
	 */
	// Called by BuildingPanelFarming.java to retrieve the phase of the crop
	public String getPhase() {
		return phase;
	}


	/**
	 * Gets the crop category
	 * @return category
	 * 2014-10-10 by mkung: added this method for UI to show crop category
	 */
	// Called by BuildingPanelFarming.java to retrieve the crop category
	public String getCategory() {
		return cropType.getCropCategory();
	}

	/**
	 * Gets the maximum possible food harvest for crop.
	 * @return food harvest (kg.)
	 */
	public double getMaxHarvest() { return maxHarvest; }

	/**
	 * Gets the amount of growing time completed.
	 * @return growing time (millisols)
	 */
	public double getGrowingTimeCompleted() {
		return growingTimeCompleted;
	}

	/**
	 * Sets the amount of growing time completed.
	 * @param percent
	 */
	//public void setGrowingTimeCompleted(double percent) {
	//	this.growingTimeCompleted = percent * cropGrowingTime / 100D ;
	//}


	/**
	 * Checks if crop needs additional work on current sol.
	 * @return true if more work needed.
	 */
	public boolean requiresWork() {
		boolean result = false;
		if (phase.equals(PLANTING) || phase.equals(HARVESTING)) result = true;
		else if (phase.equals(GROWING) || phase.equals(GERMINATION) ) {
			if (dailyTendingWorkRequired > currentPhaseWorkCompleted) result = true;
		}

		return result;
	}

	/**
	 * Gets the overall health condition of the crop.
	 *
	 * @return condition as value from 0 (poor) to 1 (healthy)
	 */
	// Called by BuildingPanelFarming.java to retrieve the health condition status
	// 2015-08-26 Revised getCondition()
	public double getCondition() {
		// O:bad, 1:good
		double result = 0D;

		//System.out.println(cropType.getName() + " fractionalGrowthCompleted : " + fractionalGrowthCompleted);
		//System.out.println(cropType.getName() + " actualHarvest : " + actualHarvest);

		if (phase.equals(PLANTING)) {
			result = 1D;
			//actualHarvest = 0;
			//growingTimeCompleted = 0;
		}
		//else if (actualHarvest <= 0D) {
		//	actualHarvest = 0;
		//	growingTimeCompleted = 0;
		//	result = 1D;
		//}

		else if (phase.equals(GERMINATION)) {
			if ( (growingTimeCompleted / cropGrowingTime) <= .02 ) {
				// avoid initial spurious data
				result = 1; //actualHarvest / maxHarvest * cropGrowingTime / growingTimeCompleted ;
				//System.out.println("condition is "+ result + " for " + cropName);
			}
			else {
				result = actualHarvest / maxHarvest * cropGrowingTime / growingTimeCompleted ;
				//System.out.println("condition is "+ result + " for " + cropName);
			}
		}
		else if (phase.equals(GROWING) || phase.equals(HARVESTING)) {
			result = actualHarvest / maxHarvest * cropGrowingTime / growingTimeCompleted ;
			//System.out.println("condition is "+ result + " for " + cropName);
		}

/*
  		if (phase.equals(PLANTING)) result = 1D;

		else if (phase.equals(GERMINATION)) {
			if ((maxHarvest == 0D) || (growingTimeCompleted == 0D)) result = 1D;
			else result = actualHarvest / maxHarvest * cropGrowingTime / growingTimeCompleted;
			//System.out.println("condition is "+ result);
		}

		else if (phase.equals(GROWING) ) {
			if ((maxHarvest == 0D) || (growingTimeCompleted == 0D)) result = 1D;
			else result = actualHarvest / maxHarvest * cropGrowingTime / growingTimeCompleted;
			//System.out.println("condition is "+ result);
		}

		else if (phase.equals(HARVESTING) || phase.equals(FINISHED)) {
			result = actualHarvest / maxHarvest;
		}
*/
		if (result > 1D) result = 1D;
		else if (result < 0D) result = 0D;

		//System.out.println
		//logger.info("getCondition() : " + cropName + "'s condition is "+ result);

		healthCondition = result;

		return result;
	}

	/**
	 * Adds work time to the crops current phase.
	 * @param workTime - Work time to be added (millisols)
	 * @return workTime remaining after working on crop (millisols)
	 * @throws Exception if error adding work.
	 */
	// Called by Farming.java's addWork()
	public double addWork(double workTime) {
		double remainingWorkTime = workTime;

		if (actualHarvest <= 0D) {
			actualHarvest = 0;
			growingTimeCompleted = 0;
		}

		if (phase.equals(PLANTING)) {
			currentPhaseWorkCompleted += remainingWorkTime;
			if (currentPhaseWorkCompleted >= plantingWorkRequired) {

				remainingWorkTime = currentPhaseWorkCompleted - plantingWorkRequired;
				currentPhaseWorkCompleted = 0D;

				// 2015-08-26 Added checking the following two conditions
				fractionalGrowthCompleted = growingTimeCompleted/cropGrowingTime;
				if ( fractionalGrowthCompleted * 100D <= PERCENT_IN_GERMINATION_PHASE) {	// assuming the first 10% growing day of each crop is germination
					phase = GERMINATION;
				}
				else if ( fractionalGrowthCompleted * 100D > PERCENT_IN_GERMINATION_PHASE) {
					phase = GROWING;
				}

			}
			else {
				remainingWorkTime = 0D;
			}
		}

		// 2015-02-15 Added GERMINATION
		if (phase.equals(GERMINATION) || phase.equals(GROWING)) {
			currentPhaseWorkCompleted += remainingWorkTime;
	        //System.out.println("addWork() : currentPhaseWorkCompleted is " + currentPhaseWorkCompleted);
			if (currentPhaseWorkCompleted >= dailyTendingWorkRequired) {
				remainingWorkTime = currentPhaseWorkCompleted - dailyTendingWorkRequired;
				currentPhaseWorkCompleted = dailyTendingWorkRequired;
			}
			else {
				remainingWorkTime = 0D;
			}
		}

		else if (phase.equals(HARVESTING)) {
				//logger.info("addWork() : crop is in Harvesting phase");
			currentPhaseWorkCompleted += remainingWorkTime;
			if (currentPhaseWorkCompleted >= harvestingWorkRequired) {
				// Harvest is over. Close out this phase
				//logger.info("addWork() : done harvesting. remainingWorkTime is " + Math.round(remainingWorkTime));
				double overWorkTime = currentPhaseWorkCompleted - harvestingWorkRequired;
				// 2014-10-07 modified parameter list to include crop name
				double lastHarvest = actualHarvest * (remainingWorkTime - overWorkTime) / harvestingWorkRequired;
				// Store the crop harvest
				Storage.storeAnResource(lastHarvest, cropName, inv);
				logger.info("addWork() : harvesting " + cropName + " : " + Math.round(lastHarvest * 1000.0)/1000.0 + " kg. All Done.");
				remainingWorkTime = overWorkTime;
				phase = FINISHED;
				generateCropWaste(lastHarvest);

				//2015-08-26 Added harvestSeedlings();
				harvestSeedlings();
				//actualHarvest = 0;
				//growingTimeCompleted = 0;

			}
			else { 	// continue the harvesting process
				// 2014-10-07 modified parameter list to include crop name
				double modifiedHarvest = actualHarvest * workTime / harvestingWorkRequired;
				//logger.info("addWork() : " + cropName + " modifiedHarvest is " + Math.round(modifiedHarvest * 1000.0)/1000.0);
				// Store the crop harvest
				Storage.storeAnResource(modifiedHarvest, cropName, inv);
				logger.info("addWork() : harvesting " + cropName + " : " + Math.round(modifiedHarvest * 1000.0)/1000.0 + " kg.");
				remainingWorkTime = 0D;
				generateCropWaste(modifiedHarvest);
			}
		}

		return remainingWorkTime;
	}

	//2015-08-26 Added harvestSeedlings();
	public void harvestSeedlings() {
		// Added the contributing factor based on the health condition
		// TODO: re-tune the amount of seedlings based on not just based on the edible biomass (actualHarvest) but also the inedible biomass and the crop category
		double seedlings = healthCondition* actualHarvest/10D;

		// Added randomness
		double rand = RandomUtil.getRandomDouble(2);
		seedlings = seedlings * .7 + seedlings * rand;

		//int numSeedlings = (int) seedlings;

		String seedlingName = cropName + " seedling";
		Storage.storeAnResource(seedlings, seedlingName, inv);

		System.out.println(seedlings + " kg " + seedlingName + " extracted and preserved");
	}

	public void generateCropWaste(double harvestMass) {
		// 2015-02-06 Added Crop Waste
		double amountCropWaste = harvestMass * cropType.getInedibleBiomass() / (cropType.getInedibleBiomass() +cropType.getEdibleBiomass());
		Storage.storeAnResource(amountCropWaste, "Crop Waste", inv);
		//logger.info("addWork() : " + cropName + " amountCropWaste " + Math.round(amountCropWaste * 1000.0)/1000.0);
	}


    /**
     * Stores an resource
     * @param amount
     * @param name

	// 2015-02-06 Added storeAnResource()
    public void storeAnResource(double amount, String name) {
    	try {
            AmountResource ar = AmountResource.findAmountResource(name);
            double remainingCapacity = inv.getAmountResourceRemainingCapacity(ar, false, false);

            if (remainingCapacity < amount) {
                // if the remaining capacity is smaller than the harvested amount, set remaining capacity to full
            	amount = remainingCapacity;
                //logger.info(" storage is full!");
            }
            // TODO: consider the case when it is full
            inv.storeAmountResource(ar, amount, true);
            inv.addAmountSupplyAmount(ar, amount);
        }  catch (Exception e) {
    		logger.log(Level.SEVERE,e.getMessage());
        }
    }
     */

	/**
	 * Time passing for crop.
	 * @param time - amount of time passing (millisols)
	 */
	public void timePassing(double time) {

		if (phase.equals(FINISHED)) {
			actualHarvest = 0;
			growingTimeCompleted = 0;
			//System.out.println("timePassing() : FINISHED");

		}

		else if (phase.equals(GROWING)|| phase.equals(GERMINATION)) {
			//System.out.println("timePassing() : GROWING or GERMINATION");
			//System.out.println("timePassing() : time is " + time);
			if (time > 0D) {
				growingTimeCompleted += time;
				//System.out.println("timePassing() : growingTimeCompleted : " + growingTimeCompleted );
				//System.out.println("timePassing() : growingTimeCompleted / cropGrowingTime : " + growingTimeCompleted / cropGrowingTime);

				if (growingTimeCompleted <= cropGrowingTime * PERCENT_IN_GERMINATION_PHASE / 100D) {
					phase = GERMINATION;
					currentPhaseWorkCompleted = 0D;
				}

				else if (growingTimeCompleted < cropGrowingTime) {
					phase = GROWING;
					currentPhaseWorkCompleted = 0D;
				}

				if (growingTimeCompleted >= cropGrowingTime) {
					phase = HARVESTING;
					currentPhaseWorkCompleted = 0D;
				} else if (growingTimeCompleted == 0) {
					//
				}
				else { // still in phase.equals(GROWING)|| phase.equals(GERMINATION)
					//System.out.println("timePassing() : at else{}");

/*					// getSolOfMonth() is unreliable for some reason. use MarsClock.getSolOfYear(clock) instead
					if (marsClock == null)
						marsClock = Simulation.instance().getMasterClock().getMarsClock();
					int newSol = marsClock.getSolOfMonth();
*/
					// Modify actual harvest amount based on daily tending work.
					if (masterClock == null)
						masterClock = Simulation.instance().getMasterClock();
					// get the current time
					MarsClock clock = masterClock.getMarsClock();
					// check for the passing of each day
					int newSol = MarsClock.getSolOfYear(clock);
					if (newSol != currentSol) {
						// TODO: why doing this at the end of a sol?
						//double maxDailyHarvest = maxHarvest / cropGrowingDay;
						double dailyWorkCompleted = currentPhaseWorkCompleted / dailyTendingWorkRequired;
						// TODO: is it better off doing the actualHarvest computation once a day or every time
						actualHarvest += (maxHarvestinKgPerDay * (dailyWorkCompleted - .5D));
						currentSol = newSol;
						//System.out.println(" a new sol");
						// reset the daily work counter currentPhaseWorkCompleted back to zero
						currentPhaseWorkCompleted = 0D;
					}

					// max possible harvest within this period of time
					double maxPeriodHarvest = maxHarvest * (time / cropGrowingTime);
					// Compute each harvestModifiers and sum them up below
					double harvestModifier = calculateHarvestModifier(maxPeriodHarvest, time);
					//System.out.println("Crop.java : just done calling calculateHarvestModifier()");

					// Modify harvest amount.
					actualHarvest += maxPeriodHarvest * harvestModifier * 10D; // assuming the standard area of 10 sq m

					if (actualHarvest < 0)
						actualHarvest = 0;

					//System.out.println("timePassing() : maxPeriodHarvest is " + maxPeriodHarvest);
					//System.out.println("timePassing() : harvestModifier is " + harvestModifier);
					//System.out.println("timePassing() : actualHarvest is " + actualHarvest);
					//System.out.println("timePassing() : maxHarvest is " + maxHarvest);

					// Compute health condition
					getCondition();
					fractionalGrowthCompleted = growingTimeCompleted/cropGrowingTime;
					// Check on the health of a >25% grown crop
					if ( (fractionalGrowthCompleted > .25D) && (healthCondition < .1D) ) {
						phase = FINISHED;
						logger.info("Crop " + cropName + " at " + settlement.getName() + " died of poor health.");
						// 2015-02-06 Added Crop Waste
						double amountCropWaste = actualHarvest * cropType.getInedibleBiomass() / ( cropType.getInedibleBiomass() + cropType.getEdibleBiomass());
						Storage.storeAnResource(amountCropWaste, "Crop Waste", inv);
						logger.info(amountCropWaste + " kg Crop Waste generated from the dead "+ cropName);
						//actualHarvest = 0;
						//growingTimeCompleted = 0;
					}

					// Seedling (<10% grown crop) is less resilient and more prone to environmental factors
					if ( (fractionalGrowthCompleted > 0) && (fractionalGrowthCompleted < .1D) && (healthCondition < .15D) ) {
						phase = FINISHED;
						logger.info("The seedlings of " + cropName + " at " + settlement.getName() + " did not survive.");
						// 2015-02-06 Added Crop Waste
						double amountCropWaste = actualHarvest * cropType.getInedibleBiomass() / ( cropType.getInedibleBiomass() + cropType.getEdibleBiomass());
						Storage.storeAnResource(amountCropWaste, "Crop Waste", inv);
						logger.info(amountCropWaste + " kg Crop Waste generated from the dead "+ cropName);
						//actualHarvest = 0;
						//growingTimeCompleted = 0;
					}
				}
			}
		}
	}

	public void turnOnLighting(double kW) {
		lightingPower = kW;
	}

	public void turnOffLighting() {
		lightingPower = 0;
		//return lightingPower;
	}

	// 2015-02-16 Added calculateHarvestModifier()
	// TODO: use theoretical model for crop growth, instead of empirical model below.
	// TODO: the calculation should be uniquely tuned to each crop
	public double calculateHarvestModifier(double maxPeriodHarvest, double time) {
		double harvestModifier = 1D;
		//timeCache = timeCache + time;

		// TODO: Modify harvest modifier according to the moisture level
		// TODO: Modify harvest modifier according to the pollination by the  number of bees in the greenhouse


		// Determine harvest modifier according to amount of light.
		// TODO: Modify harvest modifier by amount of artificial light available to the whole greenhouse
		double sunlightModifier = 0;
		if (surface == null)
			surface = Simulation.instance().getMars().getSurfaceFeatures();

		if (masterClock == null)
			masterClock = Simulation.instance().getMasterClock();
		// get the current time

		if (marsClock == null)
			marsClock = masterClock.getMarsClock();

	    int currentMillisols = (int) marsClock.getMillisol();


		// 2015-04-09 Add instantaneous PAR from solar irradiance
		//double uPAR = SOLAR_IRRADIANCE_TO_PAR_RATIO * surface.getSolarIrradiance(settlement.getCoordinates());
		double uPAR = WATT_TO_PHOTON_CONVERSION_RATIO * surface.getSolarIrradiance(settlement.getCoordinates());

		double instantaneousPAR	= 0;
		if (uPAR > 10)
			instantaneousPAR = uPAR * time * MarsClock.SECONDS_IN_MILLISOL / 1_000_000D; // in mol / m2 within this period of time

	    // Gauge if there is enough sunlight
	    double progress = dailyPARCache / dailyPARRequired;
	    double ruler = currentMillisols / 1000D;
		//System.out.println("uPAR : "+ fmt.format(uPAR) + "\tinstantaneousPAR : " + fmt.format(instantaneousPAR)
		//		+ "\tprogress : "+ fmt.format(progress) + "\truler : " + fmt.format(ruler));

	    // When enough PAR have been administered to the crop, the HPS will turn off.
	    // TODO: what if the time zone of a settlement causes sunlight to shine at near the tail end of the currentMillisols time ?

	    // 2015-04-09 Compare dailyPARCache / dailyPARRequired  vs. current time / 1000D
	    if (progress < ruler) {
	    	// TODO: also compare also how much more sunlight will still be available
	    	// if sunlight is available
	    	if (uPAR > 10) {
	    		dailyPARCache = dailyPARCache + instantaneousPAR ;
	 		    //System.out.print("\tdailyPARCache : " + fmt.format(dailyPARCache));
	    	}
	    	else {
		    	//if no sunlight, turn on artificial lighting
	    		double d_PAR = dailyPARRequired - dailyPARCache; // in mol / m2 / d

	    		double d_PAR_area = d_PAR / (1000 - currentMillisols) / MarsClock.SECONDS_IN_MILLISOL * growingArea; // in mol / msol

		    	double d_kW_area = d_PAR_area * 1000 *  WATT_TO_PHOTON_CONVERSION_RATIO ;

		    	//double d_Joules_now = d_kWatt * time * MarsClock.SECONDS_IN_MILLISOL;

		    	// TODO: Typically, 5 lamps per square meter for a level of ~1000 mol/ m^2 /s

		    	int numLamp = (int) (d_kW_area / kW_PER_HPS / VISIBLE_RADIATION_HPS);
		    	// each HPS lamp supplies 400W with 40% visible radiation efficiency
		    	double supplykW = numLamp * kW_PER_HPS;

		    	// TODO: should also allow the use of LED for lighting

		    	//System.out.println("time : "+ time);
		    	double supplyIntantaneousPAR = supplykW * time * MarsClock.SECONDS_IN_MILLISOL /1000 / WATT_TO_PHOTON_CONVERSION_RATIO / growingArea ; // in mol / m2

		    	turnOnLighting(supplykW);

			    dailyPARCache = dailyPARCache + supplyIntantaneousPAR;
			    //System.out.println("\td_kW_area : "+ fmt.format(d_kW_area) + "\tsupplykW : "+ fmt.format(supplykW)
			    //+ "\tsPAR : "+ fmt.format(supplyIntantaneousPAR) + "\tdailyPARCache : " + fmt.format(dailyPARCache));
	    	}

	    }
	    else {
	    	turnOffLighting();
			dailyPARCache = dailyPARCache + instantaneousPAR;
			//System.out.println("\tdailyPARCache : " + fmt.format(dailyPARCache));

	    }
	        // check for the passing of each day
	    int newSol = MarsClock.getSolOfYear(marsClock);
		if (newSol != solCache) {
			//logger.info("Crop.java : calculateHarvestModifier() : instantaneousPAR is "+instantaneousPAR);
			// the crop has memory of the past lighting condition
			sunlightModifier = 0.5 * sunlightModifierCache  + 0.5 * dailyPARCache / dailyPARRequired;
			if (sunlightModifier > 1.5)
				sunlightModifier = 1.5;
			// TODO: If too much light, the crop's health may suffer unless a person comes to intervene
			solCache = newSol;
			//logger.info("dailyPARRequired is " + dailyPARRequired);
			//logger.info("dailyPARCache is " + dailyPARCache);
			//logger.info("sunlightModifier is " + sunlightModifier);
			dailyPARCache = 0;
			//logger.info("timeCache is "+ timeCache);
			//timeCache = 0;
		}
		else {
			//System.out.println(" currentSol : newSol   " + currentSol + " : " + newSol);
			sunlightModifier = sunlightModifierCache;
		}

		double T_NOW = farm.getBuilding().getCurrentTemperature();
		double temperatureModifier = 0 ;
		if (T_NOW > (t_initial + T_TOLERANCE))
			temperatureModifier = t_initial / T_NOW;
		else if (T_NOW < (t_initial - T_TOLERANCE))
			temperatureModifier = T_NOW / t_initial;
		else // if (T_NOW < (t + T_TOLERANCE ) && T_NOW > (t - T_TOLERANCE )) {
			// TODO: implement optimal growing temperature for each particular crop
			temperatureModifier = 1.1;

		//System.out.println("Farming.java: temperatureModifier is " + temperatureModifier);

		// Determine harvest modifier according to amount of grey water available.

		double factor = 0;
		// amount of wastewater/water needed is also based on % of growth
		if (phase.equals(GERMINATION))
			factor = .1;
		else if (fractionalGrowthCompleted < .1 )
			factor = .2;
		else if (fractionalGrowthCompleted < .2 )
			factor = .25;
		else if (fractionalGrowthCompleted < .3 )
			factor = .3;
		else if (phase.equals(GROWING))
			factor = fractionalGrowthCompleted;

		double waterUsed = 0;
		double wasteWaterRequired = factor * maxPeriodHarvest * WASTE_WATER_NEEDED;
		AmountResource wasteWater = AmountResource.findAmountResource("grey water");
		double wasteWaterAvailable = inv.getAmountResourceStored(wasteWater, false);
		double wasteWaterUsed = wasteWaterRequired;
		if (wasteWaterUsed > wasteWaterAvailable) {
			// 2015-01-25 Added diff, waterUsed and consumeWater() when grey water is not available
			double diff = wasteWaterUsed - wasteWaterAvailable;
			waterUsed = consumeWater(diff);
			Storage.retrieveAnResource(FERTILIZER_NEEDED, "fertilizer", inv, true);
			wasteWaterUsed = wasteWaterAvailable;
		}
		Storage.retrieveAnResource(wasteWaterUsed, "grey water", inv, true);
		//retrieveAnResource(wasteWater, wasteWaterUsed);

		// 2015-01-25 Added waterUsed and combinedWaterUsed
		double combinedWaterUsed = wasteWaterUsed + waterUsed;
		double fractionUsed = combinedWaterUsed / wasteWaterRequired;

		double waterModifier = fractionUsed * .5D + .5D;
		if (waterModifier > 1.1)
			waterModifier = 1.1;

		//System.out.println("Farming.java: waterModifier is " + waterModifier);

		// Amount of water generated through recycling
		double waterAmount = wasteWaterUsed * WATER_RECLAMATION_RATE;
		Storage.storeAnResource(waterAmount, LifeSupportType.WATER, inv);

		double o2Modifier = 0, co2Modifier = 0;

		if (sunlightModifier <= .5) {
			AmountResource o2ar = AmountResource.findAmountResource(LifeSupportType.OXYGEN);
			double o2Required = factor * maxPeriodHarvest * OXYGEN_NEEDED;
			double o2Available = inv.getAmountResourceStored(o2ar, false);
			double o2Used = o2Required;

			if (o2Used > o2Available)
				o2Used = o2Available;
			//retrieveAnResource(o2ar, o2Used);
			Storage.retrieveAnResource(o2Used, LifeSupportType.OXYGEN, inv, true);

			o2Modifier =  o2Used / o2Required * .5D + .5D;
			if (o2Modifier > 1.05)
				o2Modifier = 1.05;

			//System.out.println("Farming.java: o2Modifier is " + o2Modifier);


			// Determine the amount of co2 generated via gas exchange.
			double co2Amount = o2Used * CO2_GENERATION_RATE;
			Storage.storeAnResource(co2Amount, "carbon dioxide", inv);
		}

		else if (sunlightModifier > .5) {
			// TODO: gives a better modeling of how the amount of light available will trigger photosynthesis that converts co2 to o2
			// Determine harvest modifier by amount of carbon dioxide available.
			AmountResource carbonDioxide = AmountResource.findAmountResource("carbon dioxide");
			double carbonDioxideRequired = factor * maxPeriodHarvest * CARBON_DIOXIDE_NEEDED;
			double carbonDioxideAvailable = inv.getAmountResourceStored(carbonDioxide, false);
			double carbonDioxideUsed = carbonDioxideRequired;

			if (carbonDioxideUsed > carbonDioxideAvailable)
				carbonDioxideUsed = carbonDioxideAvailable;
			//retrieveAnResource(carbonDioxide, carbonDioxideUsed);
			Storage.retrieveAnResource(carbonDioxideUsed, "carbon dioxide", inv, true);

			// TODO: allow higher concentration of co2 to be pumped to increase the harvest modifier to the harvest.
			co2Modifier = carbonDioxideUsed / carbonDioxideRequired * .5D + .5D;
			// TODO: high amount of CO2 may facilitate the crop growth and reverse past bad health
			if (co2Modifier > 1.1)
				co2Modifier = 1.1;
			//System.out.println("Farming.java: co2Modifier is " + co2Modifier);

			// Determine the amount of oxygen generated via gas exchange.
			double oxygenAmount = carbonDioxideUsed * OXYGEN_GENERATION_RATE;
			Storage.storeAnResource(oxygenAmount, LifeSupportType.OXYGEN, inv);

		}


		// 2015-08-26 Tuned harvestModifier
		if (phase.equals(GROWING)) {
			// 2015-08-26 Tuned harvestModifier
			harvestModifier = .6 * harvestModifier + .4 * harvestModifier * sunlightModifier;
			//System.out.println("Farming.java: sunlight harvestModifier is " + harvestModifier);
		}
		else if (phase.equals(GERMINATION))
			harvestModifier = .8 * harvestModifier + .2 * harvestModifier * sunlightModifier;

		if (sunlightModifier > .5) {
			// 2015-08-26 Tuned harvestModifier
			harvestModifier = .7 * harvestModifier
					+ .1 * harvestModifier * temperatureModifier
					+ .1 * harvestModifier * waterModifier
					+ .1 * harvestModifier * co2Modifier;
		}

		else {
			harvestModifier = .7 * harvestModifier
					+ .1 * harvestModifier * temperatureModifier
					+ .1 * harvestModifier * waterModifier
					+ .1 * harvestModifier * o2Modifier;
		}

		// TODO: add airPressureModifier in future

		//System.out.println("harvestModifier is "+ harvestModifier);
		return harvestModifier;
	}

	/**
	 * Retrieves an amount from water.
	 * @param waterRequired
	 */
	// 2015-01-25 consumeWater()
	public double consumeWater(double waterRequired) {

		/*
		AmountResource water = AmountResource.findAmountResource("water");
		double waterAvailable = inv.getAmountResourceStored(water, false);
		double waterUsed = waterRequired;

		AmountResource fertilizer = AmountResource.findAmountResource("fertilizer");
		double fertilizerAvailable = inv.getAmountResourceStored(fertilizer, false);
		double fertilizerUsed = FERTILIZER_NEEDED;

		if (waterUsed > waterAvailable)
			waterUsed = waterAvailable;

		retrieveAnResource(water, waterUsed);

		if (fertilizerUsed >= fertilizerAvailable)
			fertilizerUsed = fertilizerAvailable;

		retrieveAnResource(fertilizer, fertilizerUsed);

		*/

		//double amountFertilizer = retrieveAnResource("fertilizer", FERTILIZER_NEEDED);
		//System.out.println("fertilizer used when grey water is not available: " + amountFertilizer);
		double amountWater = retrieveAnResource("water", waterRequired);

	    return amountWater;
	}

	/**
	 * Retrieves an amount from an Amount Resource.
	 * @param AmountResource resource
	 * @param double amount

	// 2015-01-25 Added retrieveAnResource()
	public void retrieveAnResource(AmountResource resource, double amount) {
		try {
			inv.retrieveAmountResource(resource, amount);
		    inv.addAmountDemandTotalRequest(resource);
		    inv.addAmountDemand(resource, amount);

	    } catch (Exception e) {
	        logger.log(Level.SEVERE,e.getMessage());
		}
	}
	*/
	   /**
     * Retrieves the resource
     * @param name
     * @parama requestedAmount
    */
    //2015-02-28 Added retrieveAnResource()
    public double retrieveAnResource(String name, double requestedAmount) {
    	try {
	    	AmountResource nameAR = AmountResource.findAmountResource(name);
	        double amountStored = inv.getAmountResourceStored(nameAR, false);
	    	inv.addAmountDemandTotalRequest(nameAR);
	        if (amountStored < requestedAmount) {
	     		requestedAmount = amountStored;
	    		logger.warning("Just used up all " + name);
	        }
	    	else if (amountStored < 0.00001)
	    		logger.warning("no more " + name + " in " + settlement.getName());
	    	else {
	    		inv.retrieveAmountResource(nameAR, requestedAmount);
	    		inv.addAmountDemand(nameAR, requestedAmount);
	    	}
	    }  catch (Exception e) {
    		logger.log(Level.SEVERE,e.getMessage());
	    }

    	return requestedAmount;
    }



	/**
	 * Gets the average growing time for a crop.
	 * @return average growing time (millisols)
	 * @throws Exception if error reading crop config.
	 */
	public static double getAverageCropGrowingTime() {
		CropConfig cropConfig = SimulationConfig.instance().getCropConfiguration();
		double totalGrowingTime = 0D;
		List<CropType> cropTypes = cropConfig.getCropList();
		Iterator<CropType> i = cropTypes.iterator();
		while (i.hasNext()) totalGrowingTime += i.next().getGrowingTime()*1000D;
		return totalGrowingTime / cropTypes.size();
	}

	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		cropType = null;
		farm = null;
		inv = null;
		settlement = null;
		phase = null;
	}
}
