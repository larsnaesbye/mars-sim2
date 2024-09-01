/*
 * Mars Simulation Project
 * Mining.java
 * @date 2023-06-30
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.mission;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.mars_sim.core.environment.ExploredLocation;
import com.mars_sim.core.equipment.Container;
import com.mars_sim.core.equipment.ContainerUtil;
import com.mars_sim.core.equipment.EVASuit;
import com.mars_sim.core.equipment.EVASuitUtil;
import com.mars_sim.core.equipment.EquipmentType;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.person.ai.task.CollectMinedMinerals;
import com.mars_sim.core.person.ai.task.ExitAirlock;
import com.mars_sim.core.person.ai.task.MineSite;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.resource.AmountResource;
import com.mars_sim.core.resource.ItemResourceUtil;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.structure.ObjectiveType;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.time.MarsTime;
import com.mars_sim.core.tool.RandomUtil;
import com.mars_sim.core.vehicle.Crewable;
import com.mars_sim.core.vehicle.LightUtilityVehicle;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.core.vehicle.StatusType;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.core.vehicle.VehicleType;

/**
 * Mission for mining mineral concentrations at an explored site.
 */
public class Mining extends EVAMission
	implements SiteMission {

	private static final Set<JobType> PREFERRED_JOBS = Set.of(JobType.AREOLOGIST, JobType.ASTRONOMER, JobType.PILOT);

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(Mining.class.getName());
	
	/** Mission phases */
	private static final MissionPhase MINING_SITE = new MissionPhase("Mission.phase.miningSite");
	private static final MissionStatus MINING_SITE_NOT_BE_DETERMINED = new MissionStatus("Mission.status.miningSite");
	private static final MissionStatus LUV_NOT_AVAILABLE = new MissionStatus("Mission.status.noLUV");
	private static final MissionStatus LUV_ATTACHMENT_PARTS_NOT_LOADABLE = new MissionStatus("Mission.status.noLUVAttachments");

	private static final int MAX = 3000;
	
	/** Number of large bags needed for mission. */
	public static final int NUMBER_OF_LARGE_BAGS = 4;

	/** The good value factor of a site. */
	static final double MINERAL_GOOD_VALUE_FACTOR = 500;
	
	/** The averge good value of a site. */
	static final double AVERAGE_RESERVE_GOOD_VALUE = 50_000;

	/** Amount of time(millisols) to spend at the mining site. */
	private static final double MINING_SITE_TIME = 4000D;

	/** Minimum amount (kg) of an excavated mineral that can be collected. */
	private static final double MINIMUM_COLLECT_AMOUNT = .01;


	/**
	 * The minimum number of mineral concentration estimation improvements for an
	 * exploration site for it to be considered mature enough to mine.
	 */
	public static final int MATURE_ESTIMATE_NUM = 75;

	private static final Set<ObjectiveType> OBJECTIVES = Set.of(ObjectiveType.BUILDERS_HAVEN, ObjectiveType.MANUFACTURING_DEPOT);

	
	private ExploredLocation miningSite;
	private LightUtilityVehicle luv;
	
	private Map<AmountResource, Double> detectedMinerals;
	private Map<AmountResource, Double> totalExcavatedMinerals;

	/**
	 * Constructor
	 * 
	 * @param startingPerson the person starting the mission.
	 * @throws MissionException if error creating mission.
	 */
	public Mining(Person startingPerson, boolean needsReview) {

		// Use RoverMission constructor.
		super(MissionType.MINING, startingPerson, null, MINING_SITE, MineSite.LIGHT_LEVEL);

		if (!isDone()) {
			// Initialize data members.
			detectedMinerals = new HashMap<>(1);
			totalExcavatedMinerals = new HashMap<>(1);
			
			// Recruit additional members to mission.
			if (!recruitMembersForMission(startingPerson, MIN_GOING_MEMBERS))
				return;

			int numMembers = (getMissionCapacity() + getMembers().size()) / 2;
			int buffer = (int)(numMembers * 1.5);
			int newContainerNum = Math.max(buffer, NUMBER_OF_LARGE_BAGS);
			
			setEVAEquipment(EquipmentType.LARGE_BAG, newContainerNum);
			
			Settlement s = getStartingSettlement();
			
			// Determine mining site.
			if (hasVehicle()) {
				miningSite = determineBestMiningSite(getRover(), s);
				if (miningSite == null) {
					logger.severe(startingPerson, "Mining site could not be determined.");
					endMission(MINING_SITE_NOT_BE_DETERMINED);
					return;
				}
				miningSite.setReserved(true);

				addNavpoint(miningSite.getLocation(), "a mining site");
				
				setupDetectedMinerals();
			}

			// Add home settlement
			addNavpoint(s);

			// Check if vehicle can carry enough supplies for the mission.
			if (hasVehicle() && !isVehicleLoadable()) {
				endMission(CANNOT_LOAD_RESOURCES);
			}

			if (!isDone()) {
				// Reserve light utility vehicle.
				luv = reserveLightUtilityVehicle();
				if (luv == null) {
					endMission(LUV_NOT_AVAILABLE);
					return;
				}
				setInitialPhase(needsReview);
			}
		}
	}

	/**
	 * Constructor with explicit data.
	 * 
	 * @param members            collection of mission members.
	 * @param miningSite         the site to mine.
	 * @param rover              the rover to use.
	 * @param description        the mission's description.
	 */
	public Mining(Collection<Worker> members, ExploredLocation miningSite,
			Rover rover, LightUtilityVehicle luv) {

		// Use RoverMission constructor.,  
		super(MissionType.MINING, (Worker) members.toArray()[0], rover, MINING_SITE, MineSite.LIGHT_LEVEL);

		// Initialize data members.
		this.miningSite = miningSite;
		miningSite.setReserved(true);
		detectedMinerals = new HashMap<>(1);
		totalExcavatedMinerals = new HashMap<>(1);
		
		int numMembers = (getMissionCapacity() + getMembers().size()) / 2;
		int buffer = (int)(numMembers * 1.5);
		int newContainerNum = Math.max(buffer, NUMBER_OF_LARGE_BAGS);
		
		setEVAEquipment(EquipmentType.LARGE_BAG, newContainerNum);

		addMembers(members, false);

		// Add mining site nav point.
		addNavpoint(miningSite.getLocation(), "a mining site");

		setupDetectedMinerals();
		
		// Add home settlement
		Settlement s = getStartingSettlement();
		addNavpoint(s);

		// Check if vehicle can carry enough supplies for the mission.
		if (hasVehicle() && !isVehicleLoadable()) {
			endMission(CANNOT_LOAD_RESOURCES);
		}

		// Reserve light utility vehicle.
		this.luv = luv;
		if (luv == null) {
			logger.warning("Light utility vehicle not available.");
			endMission(LUV_NOT_AVAILABLE);
		} else {
			claimVehicle(luv);
		}

		// Set initial mission phase.
		setInitialPhase(false);

	}

	/**
	 * Checks if a light utility vehicle (LUV) is available for the mission.
	 * 
	 * @param settlement the settlement to check.
	 * @return true if LUV available.
	 */
	public static boolean isLUVAvailable(Settlement settlement) {
		boolean result = false;

		Iterator<Vehicle> i = settlement.getParkedGaragedVehicles().iterator();
		while (i.hasNext()) {
			Vehicle vehicle = i.next();
			if (vehicle.getVehicleType() == VehicleType.LUV) {
				boolean usable = !vehicle.isReserved();
				
                usable = usable && vehicle.isVehicleReady() && !vehicle.isBeingTowed();

				if (((Crewable) vehicle).getCrewNum() > 0 || ((Crewable) vehicle).getRobotCrewNum() > 0)
					usable = false;

				if (usable)
					result = true;

			}
		}

		return result;
	}

	/**
	 * Checks if the required attachment parts are available.
	 * 
	 * @param settlement the settlement to check.
	 * @return true if available attachment parts.
	 */
	public static boolean areAvailableAttachmentParts(Settlement settlement) {
		boolean result = true;

		try {
			if (!settlement.getItemResourceIDs().contains(ItemResourceUtil.pneumaticDrillID)) {
				result = false;
			}
			if (!settlement.getItemResourceIDs().contains(ItemResourceUtil.backhoeID)) {
				result = false;
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error in getting parts.");
		}

		return result;
	}


	@Override
	protected void performDepartingFromSettlementPhase(Worker member) {
		super.performDepartingFromSettlementPhase(member);
		performEmbarkFrom();
	}

	private void performEmbarkFrom() {
		// Attach light utility vehicle for towing.
		if (!isDone() && (getRover().getTowedVehicle() == null)) {

			Settlement settlement = getStartingSettlement();

			getRover().setTowedVehicle(luv);
			luv.setTowingVehicle(getRover());
			settlement.removeVicinityParkedVehicle(luv);

			if (!settlement.hasItemResource(ItemResourceUtil.pneumaticDrillID)
					|| !settlement.hasItemResource(ItemResourceUtil.backhoeID)) {
				logger.warning(luv, 
						"Could not load LUV and/or its attachment parts for mission " + getName());
				endMission(LUV_ATTACHMENT_PARTS_NOT_LOADABLE);
				return;
			}
			
			// Load light utility vehicle with attachment parts.
			settlement.retrieveItemResource(ItemResourceUtil.pneumaticDrillID, 1);
			luv.storeItemResource(ItemResourceUtil.pneumaticDrillID, 1);

			settlement.retrieveItemResource(ItemResourceUtil.backhoeID, 1);
			luv.storeItemResource(ItemResourceUtil.backhoeID, 1);
		}
	}

	@Override
	protected void performDisembarkToSettlementPhase(Worker member, Settlement disembarkSettlement) {
		// Disconnect the LUV
		disengageLUV();
		
		super.performDisembarkToSettlementPhase(member, disembarkSettlement);
	}

	/**
	 * Disconnects the LUV and return the attachment parts prior to disembarking.
	 */
	protected void disengageLUV() {
		// Unload towed light utility vehicle.
		if (!isDone() && (getRover().getTowedVehicle() != null)) {
			Settlement settlement = getStartingSettlement();
			
			getRover().setTowedVehicle(null);
			luv.setTowingVehicle(null);
			settlement.removeVicinityParkedVehicle(luv);
			luv.findNewParkingLoc();

			// Unload attachment parts.
			luv.retrieveItemResource(ItemResourceUtil.pneumaticDrillID, 1);
			settlement.storeItemResource(ItemResourceUtil.pneumaticDrillID, 1);

			luv.retrieveItemResource(ItemResourceUtil.backhoeID, 1);
			settlement.storeItemResource(ItemResourceUtil.backhoeID, 1);
		}
	}

	/**
	 * Performs the EVA.
	 */
	@Override
	protected boolean performEVA(Person person) {

		Rover rover = getRover();
		double roverRemainingCap = rover.getCargoCapacity() - rover.getStoredMass();

		if (roverRemainingCap <= 0) {
			logger.info(getRover(), "No more room in " + rover.getName());
			addMissionLog("No remaining rover capacity");
			return false;
		}

		double weight = person.getMass();
		if (roverRemainingCap < weight) {
			logger.info(getRover(), "No enough capacity to fit " + person.getName() + "(" + weight + " kg).");
			addMissionLog("Rover capacity full");
			return false;
		}
		
		// Detach towed light utility vehicle if necessary.
		if (getRover().getTowedVehicle() != null) {
			getRover().setTowedVehicle(null);
			luv.setTowingVehicle(null);
		}

		// Determine if no one can start the mine site or collect resources tasks.	
		boolean canDo = false;
		boolean itsMe = false;
		for (Worker tempMember : getMembers()) {
			if (MineSite.canMineSite(tempMember, getRover())) {
				// If one person is unfit and can't get out of the airlock, 
				// it's okay. Do NOT stop others from doing EVA.
				canDo = canDo || true;
			}
			if (canCollectExcavatedMinerals(tempMember)) {
				
				if (person.equals(tempMember)) {
					itsMe = true;
				}
				
				// If one person is unfit and can't get out of the airlock, 
				// it's okay. Do NOT stop others from doing EVA.
				canDo = canDo || true;
			}
		}

		// Nobody can do anything so stop
		if (!canDo) {
			logger.warning(getRover(), "No one can mine sites in " + getName() + ".");
			return false;
		}

		if (itsMe) {
			AmountResource mineralToCollect = getMineralToCollect(person);
			assignTask(person, new CollectMinedMinerals(person, getRover(), mineralToCollect));
		}
		else {
			assignTask(person, new MineSite(person, miningSite.getLocation(), getRover(), luv));
		}

		return true;
	}


	/**
	 * Closes down the mining activities
	 */
	@Override
	protected void endEVATasks() {
		super.endEVATasks();
			
		double remainingMass = miningSite.getRemainingMass();
		if (remainingMass < 100)
			// Mark site as mined.
			miningSite.setMinable(false);

		// Attach light utility vehicle for towing.
		Rover rover = getRover();
		if (!luv.equals(rover.getTowedVehicle())) {
			rover.setTowedVehicle(luv);
			luv.setTowingVehicle(rover);
		}
	}

	private void setupDetectedMinerals() {
		Map<String, Double> concs = miningSite.getEstimatedMineralConcentrations();
		double remainingMass = miningSite.getRemainingMass();

		Iterator<String> i = concs.keySet().iterator();
		while (i.hasNext()) {
			String name = i.next();
			AmountResource resource = ResourceUtil.findAmountResource(name);
			double percent = concs.get(name);
			detectedMinerals.put(resource, remainingMass * percent / 100);
			
			logger.info(getName() + " detected " + Math.round(remainingMass * 100.0)/100.0 + " kg " + resource.getName());
		}
	}
	
	/**
	 * Checks if a person can collect minerals from the excavation pile.
	 * 
	 * @param member the member collecting.
	 * @return true if can collect minerals.
	 */
	private boolean canCollectExcavatedMinerals(Worker member) {
		boolean result = false;

		Iterator<AmountResource> i = detectedMinerals.keySet().iterator();
		while (i.hasNext()) {
			AmountResource resource = i.next();
			if ((detectedMinerals.get(resource) >= MINIMUM_COLLECT_AMOUNT)
					&& canCollectMinerals(member, getRover(), resource)) {
				result = true;
			}
		}

		return result;
	}

	/**
	 * Checks if a person can perform a CollectMinedMinerals task.
	 * 
	 * @param member      the member to perform the task
	 * @param rover       the rover the person will EVA from
	 * @param mineralType the resource to collect.
	 * @return true if person can perform the task.
	 */
	private  boolean canCollectMinerals(Worker member, Rover rover, AmountResource mineralType) {
		if (member instanceof Robot) {
			return false;
		}
		var person = (Person) member;

		// Check if person can exit the rover.
		if (!ExitAirlock.canExitAirlock(person, rover.getAirlock()))
			return false;

		if (!isEnoughSunlightForEVA()) {
			return false;
		}
		
		// Check if person's medical condition will not allow task.
		if (person.getPerformanceRating() < .2D)
			return false;

		if (person.isSuperUnfit())
			return false;
		
		// Checks if available bags with remaining capacity for resource.
		Container bag = ContainerUtil.findLeastFullContainer(rover,
															EquipmentType.LARGE_BAG,
															mineralType.getID());
		boolean bagAvailable = (bag != null);

		// Check if bag and full EVA suit can be carried by person or is too heavy.
		double carryMass = 0D;
		if (bag != null) {
			carryMass += bag.getBaseMass();
		}

		EVASuit suit = EVASuitUtil.findRegisteredOrGoodEVASuit(person);
		if (suit != null) {
			carryMass += suit.getMass();
			carryMass += suit.getAmountResourceRemainingCapacity(ResourceUtil.oxygenID);
			carryMass += suit.getAmountResourceRemainingCapacity(ResourceUtil.waterID);
		}
		double carryCapacity = person.getCarryingCapacity();
		boolean canCarryEquipment = (carryCapacity >= carryMass);

		return (bagAvailable && canCarryEquipment);
	}

	/**
	 * Gets the mineral resource to collect from the excavation pile.
	 * 
	 * @param person the person collecting.
	 * @return mineral
	 */
	private AmountResource getMineralToCollect(Person person) {
		AmountResource result = null;
		double largestAmount = 0D;

		Iterator<AmountResource> i = detectedMinerals.keySet().iterator();
		while (i.hasNext()) {
			AmountResource resource = i.next();
			if ((detectedMinerals.get(resource) >= MINIMUM_COLLECT_AMOUNT)
					&& canCollectMinerals(person, getRover(), resource)) {
				double amount = detectedMinerals.get(resource);
				if (amount > largestAmount) {
					result = resource;
					largestAmount = amount;
				}
			}
		}

		return result;
	}

	/**
	 * Determines the best available mining site.
	 * 
	 * @param rover          the mission rover.
	 * @param homeSettlement the mission home settlement.
	 * @return best explored location for mining, or null if none found.
	 */
	public static ExploredLocation determineBestMiningSite(Rover rover, Settlement homeSettlement) {

		ExploredLocation result = null;
		double bestValue = 0D;

		try {
			double roverRange = rover.getEstimatedRange();
			double tripTimeLimit = rover.getTotalTripTimeLimit(true);
			double tripRange = getTripTimeRange(tripTimeLimit, rover.getBaseSpeed() / 2D);
			double range = roverRange;
			if (tripRange < range) {
				range = tripRange;
			}

			for(ExploredLocation site : surfaceFeatures.getAllPossibleRegionOfInterestLocations()) {
				boolean isMature = (site.getNumEstimationImprovement() >= 
						RandomUtil.getRandomDouble(MATURE_ESTIMATE_NUM/2.0, 1.0 * MATURE_ESTIMATE_NUM));

				if (site.isMinable() && site.isClaimed() && !site.isReserved() && site.isExplored() && isMature
					// Only mine from sites explored from home settlement.
					&& (site.getSettlement() == null || homeSettlement.equals(site.getSettlement()))
					&& homeSettlement.getCoordinates().getDistance(site.getLocation()) <= range) {
						double value = getMiningSiteValue(site, homeSettlement);
						if (value > bestValue) {
							result = site;
							bestValue = value;
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error determining best mining site.");
		}

		return result;
	}

	/**
	 * Determines the total mature mining sites score.
	 * 
	 * @param rover          the mission rover.
	 * @param homeSettlement the mission home settlement.
	 * @return the total score
	 */
	public static double getMatureMiningSitesTotalScore(Rover rover, Settlement homeSettlement) {

		double total = 0;

		try {
			double roverRange = rover.getEstimatedRange();
			double tripTimeLimit = rover.getTotalTripTimeLimit(true);
			double tripRange = getTripTimeRange(tripTimeLimit, rover.getBaseSpeed() / 2D);
			double range = roverRange;
			if (tripRange < range) {
				range = tripRange;
			}

			for (ExploredLocation site : surfaceFeatures.getAllPossibleRegionOfInterestLocations()) {
				boolean isMature = (site.getNumEstimationImprovement() >= 
						RandomUtil.getRandomDouble(MATURE_ESTIMATE_NUM/2.0, 1.0 * MATURE_ESTIMATE_NUM));
				if (site.isMinable() && site.isClaimed() && !site.isReserved() && site.isExplored() && isMature
					// Only mine from sites explored from home settlement.
					&& (site.getSettlement() == null || homeSettlement.equals(site.getSettlement()))
					&& homeSettlement.getCoordinates().getDistance(site.getLocation()) <= range) {
						double value = getMiningSiteValue(site, homeSettlement);
						total += value;
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error determining best mining site.");
		}

		return total;
	}
	
	/**
	 * Gets the estimated mineral value of a mining site.
	 * 
	 * @param site       the mining site.
	 * @param settlement the settlement valuing the minerals.
	 * @return estimated value of the minerals at the site (VP).
	 * @throws MissionException if error determining the value.
	 */
	public static double getMiningSiteValue(ExploredLocation site, Settlement settlement) {

		double result = 0D;

		for (Map.Entry<String, Double> conc : site.getEstimatedMineralConcentrations().entrySet()) {
			int mineralResource = ResourceUtil.findIDbyAmountResourceName(conc.getKey());
			double mineralValue = settlement.getGoodsManager().getGoodValuePoint(mineralResource);
			double reserve = site.getRemainingMass();
			double mineralAmount = (conc.getValue() / 100) * reserve / AVERAGE_RESERVE_GOOD_VALUE * MINERAL_GOOD_VALUE_FACTOR;
			result += mineralValue * mineralAmount;
		}

		result = Math.min(MAX, result);
		
		logger.info(settlement, 30_000L, site.getLocation() 
			+ " has a Mining Value of " + Math.round(result * 100.0)/100.0 + ".");
		
		return result;
	}

	/**
	 * Gets the range of a trip based on its time limit and mining site.
	 * 
	 * @param tripTimeLimit time (millisols) limit of trip.
	 * @param averageSpeed  the average speed of the vehicle.
	 * @return range (km) limit.
	 */
	private static double getTripTimeRange(double tripTimeLimit, double averageSpeed) {
		double tripTimeTravellingLimit = tripTimeLimit - MINING_SITE_TIME;
		double averageSpeedMillisol = averageSpeed / MarsTime.MILLISOLS_PER_HOUR;
		return tripTimeTravellingLimit * averageSpeedMillisol;
	}

	/**
	 * Gets the mission mining site.
	 * 
	 * @return mining site.
	 */
	public ExploredLocation getMiningSite() {
		return miningSite;
	}

	@Override
	protected void endMission(MissionStatus endStatus) {
		super.endMission(endStatus);

		if (miningSite != null) {
			miningSite.setReserved(false);
		}
		if (luv != null) {
			releaseVehicle(luv);
		}
	}

	/**
	 * Reserves a light utility vehicle for the mission.
	 * 
	 * @return reserved light utility vehicle or null if none.
	 */
	private LightUtilityVehicle reserveLightUtilityVehicle() {
		for(Vehicle vehicle : getStartingSettlement().getParkedGaragedVehicles()) {
			if (vehicle.getVehicleType() == VehicleType.LUV) {
				LightUtilityVehicle luvTemp = (LightUtilityVehicle) vehicle;
				if (((luvTemp.getPrimaryStatus() == StatusType.PARKED) || (luvTemp.getPrimaryStatus() == StatusType.GARAGED))
						&& !luvTemp.isReserved() && (luvTemp.getCrewNum() == 0) && (luvTemp.getRobotCrewNum() == 0)) {
					claimVehicle(luvTemp);
					return luvTemp;
				}
			}
		}

		return null;
	}

	/**
	 * Gets the mission's light utility vehicle.
	 * 
	 * @return light utility vehicle.
	 */
	public LightUtilityVehicle getLightUtilityVehicle() {
		return luv;
	}

	/**
	 * Gets the amount of a mineral currently excavated.
	 * 
	 * @param mineral the mineral resource.
	 * @return amount (kg)
	 */
	public double getMineralExcavationAmount(AmountResource mineral) {
		return detectedMinerals.getOrDefault(mineral, 0D);
	}

	/**
	 * Gets the total amount of a mineral that has been excavated so far.
	 * 
	 * @param mineral the mineral resource.
	 * @return amount (kg)
	 */
	public double getTotalMineralExcavatedAmount(AmountResource mineral) {
		return totalExcavatedMinerals.getOrDefault(mineral, 0D);
	}

	/**
	 * Excavates an amount of a mineral.
	 * 
	 * @param mineral the mineral resource.
	 * @param amount  the amount (kg)
	 */
	public void excavateMineral(AmountResource mineral, double amount) {
		double currentExcavated = amount;
		if (detectedMinerals.containsKey(mineral)) {
			currentExcavated += detectedMinerals.get(mineral);
		}
		detectedMinerals.put(mineral, currentExcavated);

		double totalExcavated = amount;
		if (totalExcavatedMinerals.containsKey(mineral)) {
			totalExcavated += totalExcavatedMinerals.get(mineral);
		}
		totalExcavatedMinerals.put(mineral, totalExcavated);

		fireMissionUpdate(MissionEventType.EXCAVATE_MINERALS_EVENT);
	}

	/**
	 * Collects an amount of a mineral.
	 * 
	 * @param mineral the mineral resource.
	 * @param amount  the amount (kg)
	 * @throws Exception if error collecting mineral.
	 */
	public void collectMineral(AmountResource mineral, double amount) {
		double currentExcavated = 0D;
		if (detectedMinerals.containsKey(mineral)) {
			currentExcavated = detectedMinerals.get(mineral);
		}
		if (currentExcavated >= amount) {
			// Record the excavated amount
			detectedMinerals.put(mineral, (currentExcavated - amount));
			// Reduce the mass at the site
			getMiningSite().excavateMass((currentExcavated - amount));	
		
		} else {
			throw new IllegalStateException(
					mineral.getName() + " amount: " + amount + " more than currently excavated.");
		}
		fireMissionUpdate(MissionEventType.COLLECT_MINERALS_EVENT);
	}

	@Override
	protected Set<JobType> getPreferredPersonJobs() {
		return PREFERRED_JOBS;
	}

	@Override
	public Set<ObjectiveType> getObjectiveSatisified() {
		return OBJECTIVES;
	}
	
	@Override
	public double getTotalSiteScore(Settlement reviewerSettlement) {
		return getMiningSiteValue(miningSite, reviewerSettlement);
	}

	@Override
	protected double getEstimatedTimeAtEVASite(boolean buffer) {
		return MINING_SITE_TIME;
	}
}
