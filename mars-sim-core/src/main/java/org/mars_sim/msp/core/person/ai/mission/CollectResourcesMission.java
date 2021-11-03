/*
 * Mars Simulation Project
 * CollectResourcesMission.java
 * @date 2021-08-20
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.mission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Direction;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.data.ResourceHolder;
import org.mars_sim.msp.core.environment.TerrainElevation;
import org.mars_sim.msp.core.equipment.EquipmentType;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.CollectResources;
import org.mars_sim.msp.core.person.ai.task.EVAOperation;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.Worker;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * The CollectResourcesMission class is a mission to travel in a rover to
 * several random locations around a settlement and collect resources of a given
 * type.
 */
public abstract class CollectResourcesMission extends RoverMission
	implements Serializable, SiteMission {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(CollectResourcesMission.class.getName());
	
	/** Mission phase. */
	public static final MissionPhase COLLECT_RESOURCES = new MissionPhase(
			Msg.getString("Mission.phase.collectResources")); //$NON-NLS-1$

	/** Estimated collection time multiplier for EVA. */
	public static final double EVA_COLLECTION_OVERHEAD = 20D;

	/** THe maximum number of sites under consideration. */
	public static final int MAX_NUM_PRIMARY_SITES = 30;
	/** THe maximum number of sites under consideration. */
	public static final int MAX_NUM_SECONDARY_SITES = 5;
	
	// Data members
	/** The total site score of this prospective resource collection mission. */
	protected double totalSiteScore;
	/** The amount of resources (kg) collected at a collection site. */
	private double siteCollectedResources;
	/** The starting amount of resources in a rover at a collection site. */
	private double collectingStart;
	/** The goal amount of resources to collect at a site (kg). */
	private double siteResourceGoal;
	/** The resource collection rate for a person (kg/millisol). */
	protected double resourceCollectionRate;
	/** The number of containers needed for the mission. */
	private int containerNum;
	/** External flag for ending collection at the current site. */
	private boolean endCollectingSite;
	/** The total amount (kg) of resource collected. */
	private double totalResourceCollected;
	
	/** The type of container needed for the mission or null if none. */
	private EquipmentType containerID;
	/** The start time at the current collection site. */
	private MarsClock collectionSiteStartTime;
	/** The type of resource to collect. */
	private Integer resourceID;

	/**
	 * Constructor
	 * 
	 * @param missionName            The name of the mission.
	 * @param startingPerson         The person starting the mission.
	 * @param resourceID           The type of resource.
	 * @param siteResourceGoal       The goal amount of resources to collect at a
	 *                               site (kg) (or 0 if none).
	 * @param resourceCollectionRate The resource collection rate for a person
	 *                               (kg/millisol).
	 * @param containerID          The type of container needed for the mission or
	 *                               null if none.
	 * @param containerNum           The number of containers needed for the
	 *                               mission.
	 * @param numSites               The number of collection sites.
	 * @param minPeople              The mimimum number of people for the mission.
	 * @throws MissionException if problem constructing mission.
	 */
	CollectResourcesMission(String missionName, MissionType missionType, Person startingPerson, int resourceID, double siteResourceGoal,
			double resourceCollectionRate, EquipmentType containerID, int containerNum, int numSites, int minPeople) {

		// Use RoverMission constructor
		super(missionName, missionType, startingPerson, minPeople);
		
		// Problem starting mission
		if (isDone()) {
			return;
		}
		
		Settlement s = startingPerson.getSettlement();
		
		// Set mission capacity.
		if (hasVehicle())
			setMissionCapacity(getRover().getCrewCapacity());
		int availableSuitNum = Mission.getNumberAvailableEVASuitsAtSettlement(startingPerson.getSettlement());
		if (availableSuitNum < getMissionCapacity())
			setMissionCapacity(availableSuitNum);

		// Initialize data members.
		s = startingPerson.getSettlement();

		if (s != null) {
			setStartingSettlement(s);

			setResourceID(resourceID);
			this.siteResourceGoal = siteResourceGoal;
			this.resourceCollectionRate = resourceCollectionRate;
			this.containerID = containerID;
			this.containerNum = containerNum;

			// Recruit additional members to mission.
			if (!recruitMembersForMission(startingPerson))
				return;

			// Determine collection sites
			if (hasVehicle()) {
				// Get the current location.
				Coordinates startingLocation = s.getCoordinates();
				double range = getVehicle().getRange(missionType);
				double timeLimit = getTotalTripTimeLimit(getRover(), getPeopleNumber(), true);
				
				// Determining the actual traveling range.
				double timeRange = getTripTimeRange(timeLimit, numSites, true);
				if (timeRange < range)
					range = timeRange;
				if (range <= 0D) {
					logger.warning(getVehicle(), "Has zero range for mission " + missionName);
					addMissionStatus(MissionStatus.NO_AVAILABLE_VEHICLES);
					endMission();
					return;
				}
				
				if (terrainElevation == null)
					terrainElevation = surfaceFeatures.getTerrainElevation();
				
				// Find some sites
				List<Coordinates> unorderedSites = null;
				while (unorderedSites == null) {
					unorderedSites= determineCollectionSites(startingLocation,
							range,
							numSites);
				
					if (!isValidScore(totalSiteScore)) {
						totalSiteScore = 0;
						unorderedSites = null;
						logger.warning(startingPerson, missionName + " attempt another collection site find");
					}
					
					// Mission might be aborted at determine site step
					if (isDone()) {
						logger.warning(startingPerson, missionName + " site searched & mission aborted");
						return;
					}
				}
				
				// Reorder sites for shortest distance and load
				List<Coordinates> orderSites = Exploration.getMinimalPath(startingLocation, unorderedSites);
				int collectionSiteNum = 1;	
				for(Coordinates next : orderSites) {
					addNavpoint(new NavPoint(next, getCollectionSiteDescription(collectionSiteNum++)));
				}
			}

			// Add home settlement for return
			addNavpoint(new NavPoint(s.getCoordinates(), s, s.getName()));

			// Check if vehicle can carry enough supplies for the mission.
			if (hasVehicle() && !isVehicleLoadable()) {
				addMissionStatus(MissionStatus.CANNOT_LOAD_RESOURCES);
				endMission();
			}
		}

		if (!isDone()) {
			// Add collecting phase.
			addPhase(COLLECT_RESOURCES);

			// Set initial mission phase.
			setPhase(VehicleMission.REVIEWING);
			setPhaseDescription(Msg.getString("Mission.phase.reviewing.description")); //$NON-NLS-1$
		}
	}


	/**
	 * Constructor with explicit data
	 * 
	 * @param missionName            The name of the mission.
	 * @param members                collection of mission members.
	 * @param startingSettlement     the starting settlement.
	 * @param resourceID           The type of resource.
	 * @param siteResourceGoal       The goal amount of resources to collect at a
	 *                               site (kg) (or 0 if none).
	 * @param resourceCollectionRate The resource collection rate for a person
	 *                               (kg/millisol).
	 * @param containerID          The type of container needed for the mission or
	 *                               null if none.
	 * @param containerNum           The number of containers needed for the
	 *                               mission.
	 * @param numSites               The number of collection sites.
	 * @param minPeople              The mimimum number of people for the mission.
	 * @param rover                  the rover to use.
	 * @param iceCollectionSites     the sites to collect ice.
	 * @throws MissionException if problem constructing mission.
	 */
	CollectResourcesMission(String missionName, MissionType missionType, Collection<MissionMember> members, Settlement startingSettlement,
			Integer resourceID, double siteResourceGoal, double resourceCollectionRate, EquipmentType containerID,
			int containerNum, int numSites, int minPeople, Rover rover, List<Coordinates> collectionSites) {

		// Use RoverMission constructor
		super(missionName, missionType, (MissionMember) members.toArray()[0], minPeople, rover);

		setStartingSettlement(startingSettlement);

		// Set mission capacity.
		setMissionCapacity(getRover().getCrewCapacity());
		int availableSuitNum = Mission.getNumberAvailableEVASuitsAtSettlement(startingSettlement);
		if (availableSuitNum < getMissionCapacity())
			setMissionCapacity(availableSuitNum);

		this.resourceID = resourceID;
		this.siteResourceGoal = siteResourceGoal;
		this.resourceCollectionRate = resourceCollectionRate;
		this.containerID = containerID;
		this.containerNum = containerNum;

		// Set collection navpoints.
		for (int x = 0; x < collectionSites.size(); x++)
			addNavpoint(new NavPoint(collectionSites.get(x), getCollectionSiteDescription(x + 1)));

		// Add home navpoint.
		addNavpoint(
				new NavPoint(startingSettlement.getCoordinates(), startingSettlement, startingSettlement.getName()));

		Person person = null;

		// Add mission members.
		Iterator<MissionMember> i = members.iterator();
		while (i.hasNext()) {
			MissionMember member = i.next();			
			if (member instanceof Person) {
				person = (Person) member;
				person.getMind().setMission(this);
			}
		}

		// Add collecting phase.
		addPhase(COLLECT_RESOURCES);

		// Set initial mission phase.
		setPhase(VehicleMission.EMBARKING);
		setPhaseDescription(Msg.getString("Mission.phase.embarking.description", getStartingSettlement().getName())); // $NON-NLS-1$

		// Check if vehicle can carry enough supplies for the mission.
		if (hasVehicle() && !isVehicleLoadable()) {
			addMissionStatus(MissionStatus.CANNOT_LOAD_RESOURCES);
			endMission();
		}
	}

	/**
	 * By default score is always accepted
	 * @param score
	 * @return
	 */
	protected boolean isValidScore(double score) {
		return true;
	}


	protected void setResourceID(int newResource) {
		this.resourceID = newResource;
	}
	
	/**
	 * Gets the total amount of resources collected so far in the mission.
	 * 
	 * @return resource amount (kg).
	 */
	public double getTotalCollectedResources() {
		return totalResourceCollected;
	}

	/**
	 * Determines a new phase for the mission when the current phase has ended.
	 * 
	 * @throws MissionException if problem setting a new phase.
	 */
	protected void determineNewPhase() {

		if (REVIEWING.equals(getPhase())) {
			setPhase(VehicleMission.EMBARKING);
			setPhaseDescription(
					Msg.getString("Mission.phase.embarking.description", getStartingSettlement().getName()));//getCurrentNavpoint().getDescription()));//startingMember.getSettlement().toString())); // $NON-NLS-1$
		}
		
		else if (EMBARKING.equals(getPhase())) {
			startTravelToNextNode();
			setPhase(VehicleMission.TRAVELLING);
			setPhaseDescription(
					Msg.getString("Mission.phase.travelling.description", getNextNavpoint().getDescription())); // $NON-NLS-1$
		}

		else if (TRAVELLING.equals(getPhase())) {

			if (getCurrentNavpoint() == null)
				// go back home
				returnHome();
			else if (getCurrentNavpoint().isSettlementAtNavpoint()) {
				setPhase(VehicleMission.DISEMBARKING);
				setPhaseDescription(Msg.getString("Mission.phase.disembarking.description",
						getCurrentNavpoint().getSettlement().getName())); // $NON-NLS-1$
			}

			else {
				setPhase(COLLECT_RESOURCES);
				setPhaseDescription(Msg.getString("Mission.phase.collectResources.description",
						getCurrentNavpoint().getDescription())); // $NON-NLS-1$
				collectionSiteStartTime = (MarsClock) Simulation.instance().getMasterClock().getMarsClock().clone();
			}

		}

		else if (COLLECT_RESOURCES.equals(getPhase())) {
			startTravelToNextNode();
			setPhase(VehicleMission.TRAVELLING);
			setPhaseDescription(
					Msg.getString("Mission.phase.travelling.description", getNextNavpoint().getDescription())); // $NON-NLS-1$
		}

		else if (DISEMBARKING.equals(getPhase())) {
			setPhase(VehicleMission.COMPLETED);
			setPhaseDescription(
					Msg.getString("Mission.phase.completed.description")); // $NON-NLS-1$
		}
		
		else if (COMPLETED.equals(getPhase())) {
			addMissionStatus(MissionStatus.MISSION_ACCOMPLISHED);
			endMission();
		}
	}

	@Override
	protected void performPhase(MissionMember member) {
		super.performPhase(member);
		if (COLLECT_RESOURCES.equals(getPhase())) {
			collectingPhase(member);
		}
	}

	public void endCollectingAtSite() {

		endCollectingSite = true;

		// End each member's collection task.
		Iterator<MissionMember> i = getMembers().iterator();
		while (i.hasNext()) {
			MissionMember member = i.next();
			if (member instanceof Person) {
				Person person = (Person) member;
				Task task = person.getMind().getTaskManager().getTask();
				if (task instanceof CollectResources) {
					((EVAOperation) task).endEVA();
				}
			}
		}
	}

	/**
	 * Updates the resources collected
	 * 
	 * @param inv
	 * @return
	 */
	private double updateResources(ResourceHolder inv) {

		double resourcesCollected = 0;
		double resourcesCapacity = 0;
		
		// Get capacity for all collectible resources. The collectible
		// resource at a site may be more than the single one specified.
		for (Integer type : getCollectibleResources()) {
			resourcesCollected += inv.getAmountResourceStored(type);
			resourcesCapacity += inv.getAmountResourceCapacity(type);
		}
		
		// Set total collected resources.
		totalResourceCollected = resourcesCollected;

		// Calculate resources collected at the site so far.
		siteCollectedResources = resourcesCollected - collectingStart;
		
		// Check if rover capacity for resources is met, then end this phase.
		if (resourcesCollected >= resourcesCapacity) {
			setPhaseEnded(true);
		}
		
		return resourcesCapacity;
	}
	
	/**
	 * what resources can be collected once on site. By default this is just
	 * the main resource but could be others.
	 * @return
	 */
	protected int [] getCollectibleResources() {
		return new int[] {resourceID};
	}

	/**
	 * Performs the collecting phase of the mission.
	 * 
	 * @param member the mission member currently performing the mission
	 */
	private void collectingPhase(MissionMember member) {
		
		Rover rover = getRover();
		double roverRemainingCap = rover.getTotalCapacity() - rover.getStoredMass();
		
		double weight = ((Person)member).getMass();

		if (roverRemainingCap < weight + 5) {
			endCollectingSite = false;
			setPhaseEnded(true);
		}
		
		double resourcesCapacity = updateResources(rover);

		// Check if end collecting flag is set.
		if (endCollectingSite) {
			endCollectingSite = false;
			setPhaseEnded(true);
		}

		// If collected resources are sufficient for this site, end the collecting
		// phase.
		if (siteCollectedResources >= siteResourceGoal) {
			setPhaseEnded(true);
		}

		if (isEveryoneInRover()) {

			// Determine if no one can start the collect resources task.
			boolean nobodyCollect = true;
			Iterator<MissionMember> j = getMembers().iterator();
			while (j.hasNext() && nobodyCollect) {				
				MissionMember m = j.next();
				for (Integer type : getCollectibleResources()) {
					if (CollectResources.canCollectResources(m, getRover(), containerID, type)) {
						nobodyCollect = false;
					}
				}
			}

			if (nobodyCollect) {
				setPhaseEnded(true);
			}
			
			// If it gets too dark (except in polar region), end the collecting phase.
			boolean inDarkPolarRegion = surfaceFeatures.inDarkPolarRegion(getCurrentMissionLocation());
			double sunlight = surfaceFeatures.getSolarIrradiance(getCurrentMissionLocation());
			if (sunlight < 20D || inDarkPolarRegion) {
				logger.info(member, "Too dark for " + getPhaseDescription() + " of " + getTypeID()
									+ ", moving to next site");
				setPhaseEnded(true);
			}

			// Anyone in the crew or a single person at the home settlement has a dangerous
			// illness, end phase.
			if (hasEmergency()) {
				setPhaseEnded(true);
			}

			// Check if enough resources for remaining trip. false = not using margin.
			if (!hasEnoughResourcesForRemainingMission(false)) {
				// If not, determine an emergency destination.
				determineEmergencyDestination(member);
				setPhaseEnded(true);
			}
		}

		if (!getPhaseEnded()) {
			if ((siteCollectedResources < siteResourceGoal) && !endCollectingSite) {
				
				if (member instanceof Person) {
					Person person = (Person) member;

					double rate = calculateRate(person); 
					
					// Randomize the rate of collection upon arrival
					rate = rate 
							* (1 + RandomUtil.getRandomDouble(.3) - RandomUtil.getRandomDouble(.3));
					
					// Note: Add how areologists and some scientific study may come up with better technique 
					// to obtain better estimation of the collection rate. Go to a prospective site, rather 
					// than going to a site coordinate in the blind.
					
					if (rate > 0)
						resourceCollectionRate = rate;
					
					// If person can collect resources, start him/her on that task.
					if (CollectResources.canCollectResources(person, getRover(), containerID, resourceID)) {
						EVAOperation collectResources = new CollectResources("Collecting Resources", person,
								getRover(), resourceID, resourceCollectionRate,
								siteResourceGoal - siteCollectedResources, rover.getAmountResourceStored(resourceID),
								containerID);
						assignTask(person, collectResources);
					}
				}
			}
		} else {
			// If the rover is full of resources, head home.
			if (siteCollectedResources >= resourcesCapacity) {
				setNextNavpointIndex(getNumberOfNavpoints() - 2);
				updateTravelDestination();
				siteCollectedResources = 0D;
			}
		}
		
		// This will update the siteCollectedResources and totalResourceCollected after the last on-site collection activity
		updateResources(rover);
	}

	/**
	 * Calculate the collection for for a worker.
	 * @param worker
	 * @return
	 */
	protected abstract double calculateRate(Worker worker);

	/**
	 * Determine the locations of the sample collection sites.
	 * @param startingLocation 
	 * 
	 * @parma startingLocation Where to start from
	 * @param roverRange the rover's driving range.
	 * @param tripTimeLimit the time limit of trip (millisols).
	 * @param numSites   the number of collection sites.
	 * @return List of fund sites to visit
	 * @throws MissionException of collection sites can not be determined.
	 */
	private List<Coordinates> determineCollectionSites(Coordinates startingLocation,
		double range, int numSites) {
		int confidence = 3 + (int)RandomUtil.getRandomDouble(marsClock.getMissionSol());
		
		List<Coordinates> unorderedSites = new ArrayList<Coordinates>();

		double limit = 0;
		Direction direction = null;
		Coordinates newLocation = null;
		Coordinates currentLocation = null;
		int siteDistance = 0;
		
		/////////////////////////////////////////
		
		// Determine the first collection site.
		double bestScore = 0;
		Coordinates bestLocation = null;
		int count = 0;
		while (count++ <= MAX_NUM_PRIMARY_SITES || bestScore == 0) {
			direction = new Direction(RandomUtil.getRandomDouble(2 * Math.PI));
			limit = range / 4D;
			siteDistance = RandomUtil.getRandomRegressionInteger(confidence, (int)limit);
			newLocation = startingLocation.getNewLocation(direction, siteDistance);
				
			double score = scoreLocation(newLocation);
			
			if (score > bestScore) {
				bestScore = score;
				bestLocation = newLocation;
			}
		}
		if (bestLocation.equals(startingLocation)) {
			throw new IllegalStateException("First site is at starting location");
		}
		totalSiteScore += bestScore;

		unorderedSites.add(bestLocation);
		currentLocation = bestLocation;
		
		// Determine remaining collection sites.
		double remainingRange = RandomUtil.getRandomDouble(range/2 - siteDistance);
		
		/////////////////////////////////////////
		
		for (int x = 1; x < numSites; x++) {
			double currentDistanceToSettlement = Coordinates.computeDistance(currentLocation, startingLocation);
			if (remainingRange > currentDistanceToSettlement) {
				bestScore = 0;
				bestLocation = null;
				count = 0;
				while (count++ <= MAX_NUM_SECONDARY_SITES || bestScore == 0) {
					direction = new Direction(RandomUtil.getRandomDouble(2D * Math.PI));
					
					double tempLimit1 = Math.pow(remainingRange, 2D) - Math.pow(currentDistanceToSettlement, 2D);
					double tempLimit2 = (2D * remainingRange)
							- (2D * currentDistanceToSettlement * direction.getCosDirection());
					limit = tempLimit1 / tempLimit2;
					
					siteDistance = RandomUtil.getRandomRegressionInteger(confidence, (int)limit);
					newLocation = currentLocation.getNewLocation(direction, siteDistance);
					
					double score = scoreLocation(newLocation);

					if (score > bestScore) {
						bestScore = score;
						bestLocation = newLocation;
					}
				}
				
				totalSiteScore += bestScore;
				logger.log(Level.INFO, getMissionType().getName() + " totalSiteScore: " + Math.round(totalSiteScore*1000.0)/1000.0 
						+ "   bestScore: " + Math.round(bestScore*1000.0)/1000.0);
				unorderedSites.add(bestLocation);
				currentLocation = bestLocation;
			
				remainingRange -= siteDistance;
			}
		}
		return unorderedSites;
	}

	protected abstract double scoreLocation(Coordinates newLocation);

	/**
	 * Gets the range of a trip based on its time limit and collection sites.
	 * 
	 * @param tripTimeLimit time (millisols) limit of trip.
	 * @param numSites      the number of collection sites.
	 * @param useBuffer     Use time buffer in estimations if true.
	 * @return range (km) limit.
	 */
	protected double getTripTimeRange(double tripTimeLimit, int numSites, boolean useBuffer) {
		double timeAtSites = getEstimatedTimeAtCollectionSite(useBuffer) * numSites;
		double tripTimeTravellingLimit = tripTimeLimit - timeAtSites;
		double averageSpeed = getAverageVehicleSpeedForOperators();
		double averageSpeedMillisol = averageSpeed / MarsClock.MILLISOLS_PER_HOUR;
		return tripTimeTravellingLimit * averageSpeedMillisol;
	}

	/**
	 * Gets the settlement associated with the mission.
	 * 
	 * @return settlement or null if none.
	 */
	public Settlement getAssociatedSettlement() {
		return getStartingSettlement();
	}

	/**
	 * Gets the estimated time remaining for the mission.
	 * 
	 * @param useBuffer use time buffer in estimations if true.
	 * @return time (millisols)
	 * @throws MissionException
	 */
	public double getEstimatedRemainingMissionTime(boolean useBuffer) {
		double result = super.getEstimatedRemainingMissionTime(useBuffer);

		result += getEstimatedRemainingCollectionSiteTime(useBuffer);

		return result;
	}

	/**
	 * Gets the estimated time remaining for collection sites in the mission.
	 * 
	 * @param useBuffer use time buffer in estimations if true.
	 * @return time (millisols)
	 * @throws MissionException if error estimating time.
	 */
	private double getEstimatedRemainingCollectionSiteTime(boolean useBuffer) {
		double result = 0D;

		// Add estimated remaining collection time at current site if still there.
		if (COLLECT_RESOURCES.equals(getPhase())) {
			MarsClock currentTime = Simulation.instance().getMasterClock().getMarsClock();
			double timeSpentAtCollectionSite = MarsClock.getTimeDiff(currentTime, collectionSiteStartTime);
			double remainingTime = getEstimatedTimeAtCollectionSite(useBuffer) - timeSpentAtCollectionSite;
			if (remainingTime > 0D)
				result += remainingTime;
		}

		// Add estimated collection time at sites that haven't been visited yet.
		int remainingCollectionSites = getNumCollectionSites() - getNumCollectionSitesVisited();
		result += getEstimatedTimeAtCollectionSite(useBuffer) * remainingCollectionSites;

		return result;
	}

	@Override
	public Map<Integer, Number> getResourcesNeededForRemainingMission(boolean useBuffer) {
		// Note: currently, it has methane resource only
		Map<Integer, Number> result = super.getResourcesNeededForRemainingMission(useBuffer);

		double collectionSitesTime = getEstimatedRemainingCollectionSiteTime(useBuffer);
		double timeSols = collectionSitesTime / 1000D;

		int crewNum = getPeopleNumber();

		// Determine life support supplies needed for collection.
		addLifeSupportResources(result, crewNum, timeSols, useBuffer);

		return result;
	}

	@Override
	protected Map<Integer, Number> getSparePartsForTrip(double distance) {
		// Load the standard parts from VehicleMission.
		Map<Integer, Number> result = super.getSparePartsForTrip(distance); // new HashMap<>();

		// Determine repair parts for EVA Suits.
		double evaTime = getEstimatedRemainingCollectionSiteTime(false);
		double numberAccidents = evaTime * getPeopleNumber() * EVAOperation.BASE_ACCIDENT_CHANCE;

		// Assume the average number malfunctions per accident is 1.5.
		double numberMalfunctions = numberAccidents * VehicleMission.AVERAGE_EVA_MALFUNCTION;

		result.putAll(super.getEVASparePartsForTrip(numberMalfunctions));

		return result;
	}

	/**
	 * Gets the total number of collection sites for this mission.
	 * 
	 * @return number of sites.
	 */
	public final int getNumCollectionSites() {
		return getNumberOfNavpoints() - 2;
	}

	/**
	 * Gets the number of collection sites that have been currently visited by the
	 * mission.
	 * 
	 * @return number of sites.
	 */
	public final int getNumCollectionSitesVisited() {
		int result = getCurrentNavpointIndex();
		if (result == (getNumberOfNavpoints() - 1))
			result -= 1;
		return result;
	}

	/**
	 * Gets the estimated time spent at a collection site.
	 * 
	 * @param useBuffer Use time buffer in estimation if true.
	 * @return time (millisols)
	 */
	protected double getEstimatedTimeAtCollectionSite(boolean useBuffer) {
		double timePerPerson = siteResourceGoal / resourceCollectionRate;
		if (useBuffer)
			timePerPerson *= EVA_COLLECTION_OVERHEAD;
		return timePerPerson / getPeopleNumber();
	}

	@Override
	public Map<Integer, Integer> getEquipmentNeededForRemainingMission(boolean useBuffer) {
		if (equipmentNeededCache != null) {
			return equipmentNeededCache;
		} else {
			Map<Integer, Integer> result = new ConcurrentHashMap<>();

			// Include required number of containers.
			result.put(EquipmentType.getResourceID(containerID), containerNum);

			equipmentNeededCache = result;
			return result;
		}
	}

	/**
	 * Gets the computed site score of this prospective resource collection mission.
	 * @return
	 */
	@Override
	public double getTotalSiteScore(Settlement reviewSettlement) {
		return totalSiteScore;
	}
	
	/**
	 * Gets the description of a collection site.
	 * 
	 * @param siteNum the number of the site.
	 * @return description
	 */
	protected abstract String getCollectionSiteDescription(int siteNum);

	@Override
	public void destroy() {
		super.destroy();

		personConfig = null;
		resourceID = null;
		containerID = null;
		collectionSiteStartTime = null;
	}
}
