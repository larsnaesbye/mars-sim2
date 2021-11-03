/*
 * Mars Simulation Project
 * RescueSalvageVehicle.java
 * @date 2021-10-20
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.mission;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.malfunction.Malfunction;
import org.mars_sim.msp.core.person.EventType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.JobType;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.StatusType;
import org.mars_sim.msp.core.vehicle.Vehicle;

//   Current Definition :
//1. 'Rescue' a vehicle -- if crew members are inside the vehicle and the 
//   towing vehicle will supply and transfer the needed life support
//   resources or broken parts to the vehicle. It may involve towing the 
//   vehicle back to the settlement.
//2. 'Salvage' a vehicle -- if crew members are absent from the vehicle.
//   It always involves towing the vehicle back to the settlement. 
//   For now, it can NOT break down the broken vehicle to salvage 
//   its parts. On the other hand, it will wait for the broken parts to 
//   be manufactured and be replaced so that the vehicle will function 
//   again.
   
/**
 * The RescueSalvageVehicle class serves the purpose of (1) rescuing the crew of
 * a vehicle that has an emergency beacon on due to medical issues or lack of
 * resources, (2) supplying life support resources into the vehicle, (3) towing
 * the vehicle back if the crew is disable or dead and (4) salvaging the vehicle
 * if it's beyond repair.
 */
public class RescueSalvageVehicle extends RoverMission implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(RescueSalvageVehicle.class.getName());

	/** Default description. */
	private static final String DEFAULT_DESCRIPTION = Msg.getString("Mission.description.rescueSalvageVehicle"); //$NON-NLS-1$

	/** Mission Type enum. */
	public static final MissionType MISSION_TYPE = MissionType.RESCUE_SALVAGE_VEHICLE;
	
	// Static members
	public static final int MIN_STAYING_MEMBERS = 1;
	public static final int MIN_GOING_MEMBERS = 2;
	private static final int MAX_GOING_MEMBERS = 3;

	public static final double BASE_RESCUE_MISSION_WEIGHT = 100D;
	public static final double BASE_SALVAGE_MISSION_WEIGHT = 20D;
	private static final double RESCUE_RESOURCE_BUFFER = 1D;

	// Mission phases
	public static final MissionPhase RENDEZVOUS = new MissionPhase(Msg.getString("Mission.phase.rendezvous")); //$NON-NLS-1$

	private static final int MIN_MEMBER = 1;
	
	// Data members
	private boolean rescue = false;

	private Vehicle vehicleTarget;

	/**
	 * Constructor
	 * 
	 * @param startingPerson the person starting the mission.
	 * @throws MissionException if error constructing mission.
	 */
	public RescueSalvageVehicle(Person startingPerson) {
		// Use RoverMission constructor
		super(DEFAULT_DESCRIPTION, MISSION_TYPE, startingPerson, MIN_GOING_MEMBERS);

		if (!isDone()) {
			setStartingSettlement(startingPerson.getSettlement());
			setMissionCapacity(MAX_GOING_MEMBERS);

			// Obtain a rescuing vehicle and ensure that vehicleTarget is not included.
			if (!reserveVehicle())
				return;
			
			if (hasVehicle()) {
				
				if (vehicleTarget == null)
					vehicleTarget = findBeaconVehicle(getStartingSettlement(), getVehicle().getRange(MISSION_TYPE));
	
				if (vehicleTarget != null) {
					
					int capacity = getRover().getCrewCapacity();
					if (capacity < MAX_GOING_MEMBERS) {
						setMissionCapacity(capacity);
					}

					int availableSuitNum = Mission.getNumberAvailableEVASuitsAtSettlement(startingPerson.getSettlement());
					if (availableSuitNum < getMissionCapacity()) {
						setMissionCapacity(availableSuitNum);
					}
					
//					if (getRescuePeopleNum(vehicleTarget) > 0) {
						rescue = true;
						setMinMembers(MIN_MEMBER);
						setDescription(
								Msg.getString("Mission.description.rescueSalvageVehicle.rescue", vehicleTarget.getName())); // $NON-NLS-1$)
//					} 
							
					// Add navpoints for target vehicle and back home again.
					addNavpoint(new NavPoint(vehicleTarget.getCoordinates(), vehicleTarget.getName()));
					addNavpoint(new NavPoint(getStartingSettlement().getCoordinates(), getStartingSettlement(),
							getStartingSettlement().getName()));

					// Recruit additional members to mission.
					if (!isDone()) {
						if (!recruitMembersForMission(startingPerson))
							return;
					}

					// Check if vehicle can carry enough supplies for the mission.
					if (hasVehicle() && !isVehicleLoadable()) {			
						addMissionStatus(MissionStatus.CANNOT_LOAD_RESOURCES);
						endMission();
					}

					// Add rendezvous phase.
					addPhase(RENDEZVOUS);

					// Set initial phase
					setPhase(VehicleMission.REVIEWING);
					setPhaseDescription(
							Msg.getString("Mission.phase.reviewing.description")); // $NON-NLS-1$
					
					logger.info(startingPerson, "Started a Rescue Vehicle Mission.");
					
				} else {
					addMissionStatus(MissionStatus.TARGET_VEHICLE_NOT_FOUND);
					endMission();
				}
			}
		}
	}

	/**
	 * Constructor with explicit data.
	 * 
	 * @param members            collection of mission members.
	 * @param startingSettlement the starting settlement.
	 * @param vehicleTarget      the vehicle to rescue/salvage.
	 * @param rover              the rover to use.
	 * @param description        the mission's description.
	 * @throws MissionException if error constructing mission.
	 */
	public RescueSalvageVehicle(Collection<MissionMember> members, Settlement startingSettlement, Vehicle vehicleTarget,
			Rover rover, String description) {

		// Use RoverMission constructor.
		super(description, MISSION_TYPE, (MissionMember) members.toArray()[0], RoverMission.MIN_GOING_MEMBERS, rover);

		setStartingSettlement(startingSettlement);
		this.vehicleTarget = vehicleTarget;
		setMissionCapacity(getRover().getCrewCapacity());

		if (getRescuePeopleNum(vehicleTarget) > 0) {
			rescue = true;
		}

		// Add navpoints for target vehicle and back home again.
		addNavpoint(new NavPoint(vehicleTarget.getCoordinates(), vehicleTarget.getName()));
		addNavpoint(
				new NavPoint(startingSettlement.getCoordinates(), startingSettlement, startingSettlement.getName()));

		Person person = null;
//		Robot robot = null;

		// Add mission members.
		Iterator<MissionMember> i = members.iterator();
		while (i.hasNext()) {
			MissionMember member = i.next();
			if (member instanceof Person) {
				person = (Person) member;
				person.getMind().setMission(this);
			} else if (member instanceof Robot) {
			}
		}

		// Add rendezvous phase.
		addPhase(RENDEZVOUS);

		// Set initial phase
		setPhase(VehicleMission.EMBARKING);
		setPhaseDescription(Msg.getString("Mission.phase.embarking.description")); // $NON-NLS-1$

		// Check if vehicle can carry enough supplies for the mission.
		if (hasVehicle() && !isVehicleLoadable()) {
			addMissionStatus(MissionStatus.CANNOT_LOAD_RESOURCES);
			endMission();
		}
		
		logger.info(startingSettlement, "Had started RescueSalvageVehicle");
	}

	@Override
	protected boolean isUsableVehicle(Vehicle vehicle) {
		if (vehicle != null) {
			boolean usable = true;

			// Filter off the vehicleTarget as the candidate vehicle to be used for rescue
			if (vehicleTarget != null && vehicleTarget.equals(vehicle))
				return false;

			if (!(vehicle instanceof Rover))
				usable = false;

			if (vehicle.isReservedForMission())
				usable = false;

			usable = vehicle.isVehicleReady();

			if (vehicle.getStoredMass() > 0D)
				usable = false;

			return usable;
		} else {
			throw new IllegalArgumentException("isUsableVehicle: newVehicle is null.");
		}
	}

	@Override
	protected void setVehicle(Vehicle newVehicle) {
		super.setVehicle(newVehicle);
		if (newVehicle.isReservedForMaintenance()) {
			newVehicle.setReservedForMaintenance(false);
			newVehicle.removeStatus(StatusType.MAINTENANCE);
		}
	}

	/**
	 * Check if mission is a rescue mission or a salvage mission.
	 * 
	 * @return true if rescue mission
	 */
	public boolean isRescueMission() {
		return rescue;
	}

	/**
	 * Gets the vehicle being rescued/salvaged by this mission.
	 * 
	 * @return vehicle
	 */
	public Vehicle getVehicleTarget() {
		return vehicleTarget;
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
					Msg.getString("Mission.phase.embarking.description", getStartingSettlement().getDescription())); // $NON-NLS-1$
		}

		else if (EMBARKING.equals(getPhase())) {
			startTravelToNextNode();
			setPhase(VehicleMission.TRAVELLING);
			setPhaseDescription(
					Msg.getString("Mission.phase.travelling.description", getNextNavpoint().getDescription())); // $NON-NLS-1$
			logger.log(getVehicle(), Level.INFO, 3000, "Has been embarked to rescue/salvage " + vehicleTarget.getName());
		}

		else if (TRAVELLING.equals(getPhase())) {
			if (null != getCurrentNavpoint() && getCurrentNavpoint().isSettlementAtNavpoint()) {
				setPhase(VehicleMission.DISEMBARKING);
				setPhaseDescription(Msg.getString("Mission.phase.disembarking.description",
						getCurrentNavpoint().getSettlement().getName())); // $NON-NLS-1$
			} else {
				setPhase(RENDEZVOUS);
				if (rescue) {
					setPhaseDescription(
							Msg.getString("Mission.phase.rendezvous.descriptionRescue", vehicleTarget.getName())); // $NON-NLS-1$
				} else {
					setPhaseDescription(
							Msg.getString("Mission.phase.rendezvous.descriptionSalvage", vehicleTarget.getName())); // $NON-NLS-1$
				}
			}
		}

		else if (RENDEZVOUS.equals(getPhase())) {
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
		if (RENDEZVOUS.equals(getPhase())) {
			rendezvousPhase(member);
		}
	}

	/**
	 * Performs the rendezvous phase of the mission.
	 * 
	 * @param member the mission member currently performing the mission.
	 */
	private void rendezvousPhase(MissionMember member) {

		logger.log(getVehicle(), member, Level.INFO, 5000, "Has arrived to rendezvous with " + vehicleTarget.getName() + ".", null);

		// If rescuing vehicle crew, load rescue life support resources into vehicle (if
		// possible).
		if (rescue) {
			Map<Integer, Number> rescueResources = determineRescueResourcesNeeded(true);

			for (Integer resource : rescueResources.keySet()) {
				double amount = (Double) rescueResources.get(resource);
				double amountNeeded = amount - vehicleTarget.getAmountResourceStored(resource);

				if ((amountNeeded > 0) && (getRover().getAmountResourceStored(resource) > amountNeeded)) {
					getRover().retrieveAmountResource(resource, amountNeeded);
					vehicleTarget.storeAmountResource(resource, amountNeeded);
				}
			}
		}

		// Hook vehicle up for towing.
		getRover().setTowedVehicle(vehicleTarget);
		vehicleTarget.setTowingVehicle(getRover());

		setPhaseEnded(true);

		// The member of the rescuing vehicle will turn off the target vehicle's
		// emergency beacon.
		if (vehicleTarget.isBeaconOn())
			setEmergencyBeacon(member, vehicleTarget, false, "Towing vehicles arrived");

		// Set mission event.
		HistoricalEvent newEvent = null;
		if (rescue) {
			newEvent = new MissionHistoricalEvent(EventType.MISSION_RENDEZVOUS, this,
					"Stranded Vehicle", // cause
					this.getTypeID(), // during
					member.getName(), // member
					getVehicle().getName(), // loc0
					vehicleTarget.getLocationTag().getLocale(), // loc1
					vehicleTarget.getAssociatedSettlement().getName()
			);
		} else {
			newEvent = new MissionHistoricalEvent(EventType.MISSION_RENDEZVOUS, this,
					"Salvaged Vehicle", 
					this.getTypeID(), 
					member.getName(), 
					getVehicle().getName(),
					vehicleTarget.getLocationTag().getLocale(),
					vehicleTarget.getAssociatedSettlement().getName()
			);
		}
		eventManager.registerNewEvent(newEvent);
	}

	/**
	 * Performs the disembark to settlement phase of the mission.
	 * 
	 * @param person              the person currently performing the mission.
	 * @param disembarkSettlement the settlement to be disembarked to.
	 * @throws MissionException if error performing phase.
	 */
	protected void performDisembarkToSettlementPhase(Person person, Settlement disembarkSettlement) {

		// Put towed vehicle and crew in settlement if necessary.
		if (hasVehicle()) {
			disembarkTowedVehicles(person, getRover(), disembarkSettlement);
			super.disembark(person, getRover().getTowedVehicle(), disembarkSettlement);
		}

		super.performDisembarkToSettlementPhase(person, disembarkSettlement);
	}

	/**
	 * Stores the towed vehicle and any crew at settlement.
	 * 
	 * @param rover               the towing rover.
	 * @param disembarkSettlement the settlement to store the towed vehicle in.
	 * @throws MissionException if error disembarking towed vehicle.
	 */
	private void disembarkTowedVehicles(Person person, Rover rover, Settlement disembarkSettlement) {

		if (rover.getTowedVehicle() != null) {
			Vehicle towedVehicle = rover.getTowedVehicle();

	    	logger.log(towedVehicle, Level.INFO, 0, "Has been towed to " + disembarkSettlement.getName());

	    	Malfunction serious = vehicleTarget.getMalfunctionManager().getMostSeriousMalfunction();
			if (serious != null) {
				HistoricalEvent salvageEvent = new MissionHistoricalEvent(
						EventType.MISSION_SALVAGE_VEHICLE, 
						this, 
						serious.getName(),
					this.getTypeID(), 
					towedVehicle.getName(),
					person.getLocationTag().getImmediateLocation(), 
					person.getLocationTag().getLocale(),
					person.getAssociatedSettlement().getName()
					);
				eventManager.registerNewEvent(salvageEvent);
			}
			
			// Unload crew from the towed vehicle at settlement.
			if (towedVehicle instanceof Crewable) {
				Crewable crewVehicle = (Crewable) towedVehicle;

				for (Person p : crewVehicle.getCrew()) {
	
					if (p.isDeclaredDead() || p.getPerformanceRating() < 0.1) {
						
						if (p.isDeclaredDead())
							logger.log(p, Level.INFO, 0, "Body had been retrieved from the towed rover "
										+ towedVehicle.getName() + " during an Rescue Operation.");
						else
							logger.log(p, Level.INFO, 0, "Was rescued from the towed rover "
											+ towedVehicle.getName() + " during an Rescue Operation.");
						
						// Retrieve the dead person
						p.transfer(disembarkSettlement);
						
						int id = disembarkSettlement.getIdentifier();
						BuildingManager.addToMedicalBuilding(p, id);
						p.setAssociatedSettlement(id);

						HistoricalEvent rescueEvent = new MissionHistoricalEvent(EventType.MISSION_RESCUE_PERSON, 
								this,
								p.getPhysicalCondition().getHealthSituation(), 
								p.getTaskDescription(), 
								p.getName(),
								p.getVehicle().getName(), 
								p.getLocationTag().getLocale(),
								p.getAssociatedSettlement().getName()
								);
						eventManager.registerNewEvent(rescueEvent);												
					}
					
					else {					
						logger.log(p, Level.INFO, 0, "Successfully towed the rover "+ towedVehicle.getName() + " back home.");
					}
				}
			}
			// Unhook towed vehicle.
			rover.setTowedVehicle(null);
			towedVehicle.setTowingVehicle(null);
			logger.log(rover, Level.INFO, 0,"Was being unhooked from " + towedVehicle + " at " + disembarkSettlement);
		}
	}

	/**
	 * Gets the resources needed for the crew to be rescued.
	 * 
	 * @param useBuffer use time buffers in estimation if true.
	 * @return map of amount resources and their amounts.
	 * @throws MissionException if error determining resources.
	 */
	private Map<Integer, Number> determineRescueResourcesNeeded(boolean useBuffer) {
		Map<Integer, Number> result = new HashMap<Integer, Number>(3);

		// Determine estimate time for trip.
		double distance = vehicleTarget.getCoordinates().getDistance(getStartingSettlement().getCoordinates());
		double time = getEstimatedTripTime(true, distance);
		double timeSols = time / 1000D;

		int peopleNum = getRescuePeopleNum(vehicleTarget);

		// Determine life support supplies needed for to support rescued people
		addLifeSupportResources(result, peopleNum, timeSols, useBuffer);

		// Add extra EVA Suits based on how many people to be rescued
		return result;
	}

	/**
	 * Finds the closest available rescue or salvage vehicles within range.
	 * 
	 * @param settlement the starting settlement.
	 * @param range      the available range (km).
	 * @return vehicle or null if none available.
	 */
	public static Vehicle findBeaconVehicle(Settlement settlement, double range) {
		Vehicle result = null;
		double halfRange = range / 2D;

		Collection<Vehicle> emergencyBeaconVehicles = new ConcurrentLinkedQueue<Vehicle>();
		Collection<Vehicle> vehiclesNeedingRescue = new ConcurrentLinkedQueue<Vehicle>();

		// Find all available vehicles.
		for (Vehicle vehicle : unitManager.getVehicles()) {
			if (vehicle.isBeaconOn() && !isVehicleAlreadyMissionTarget(vehicle)) {
				emergencyBeaconVehicles.add(vehicle);

				if (vehicle instanceof Crewable) {
					if (((Crewable) vehicle).getCrewNum() > 0 || ((Crewable) vehicle).getRobotCrewNum() > 0) {
						vehiclesNeedingRescue.add(vehicle);
					}
				}
			}
		}

		// Check for vehicles with crew needing rescue first.
		if (vehiclesNeedingRescue.size() > 0) {
			Vehicle vehicle = findClosestVehicle(settlement.getCoordinates(), vehiclesNeedingRescue);
			if (vehicle != null) {
				double vehicleRange = Coordinates.computeDistance(settlement.getCoordinates(), vehicle.getCoordinates());
				if (vehicleRange <= halfRange) {
					result = vehicle;
				}
			}
		}

		// Check for vehicles needing salvage next.
		if ((result == null) && (emergencyBeaconVehicles.size() > 0)) {
			Vehicle vehicle = findClosestVehicle(settlement.getCoordinates(), emergencyBeaconVehicles);
			if (vehicle != null) {
				double vehicleRange = Coordinates.computeDistance(settlement.getCoordinates(), vehicle.getCoordinates());
				if (vehicleRange <= halfRange) {
					result = vehicle;
				}
			}
		}

		return result;
	}

	/**
	 * Checks if vehicle is already the target of a rescue/salvage vehicle mission.
	 * 
	 * @param vehicle the vehicle to check.
	 * @return true if already mission target.
	 */
	private static boolean isVehicleAlreadyMissionTarget(Vehicle vehicle) {
		boolean result = false;

		if (missionManager == null)
			missionManager = Simulation.instance().getMissionManager();

		Iterator<Mission> i = missionManager.getMissions().iterator();
		while (i.hasNext() && !result) {
			Mission mission = i.next();
			if (mission instanceof RescueSalvageVehicle) {
				Vehicle vehicleTarget = ((RescueSalvageVehicle) mission).vehicleTarget;
				if (vehicle == vehicleTarget) {
					result = true;
				}
			}
		}

		return result;
	}

	/**
	 * Gets the closest vehicle in a vehicle collection
	 * 
	 * @param location the location to measure from.
	 * @param vehicles the vehicle collection.
	 * @return closest vehicle.
	 */
	private static Vehicle findClosestVehicle(Coordinates location, Collection<Vehicle> vehicles) {
		Vehicle closest = null;
		double closestDistance = Double.MAX_VALUE;
		Iterator<Vehicle> i = vehicles.iterator();
		while (i.hasNext()) {
			Vehicle vehicle = i.next();
			double vehicleDistance = location.getDistance(vehicle.getCoordinates());
			if (vehicleDistance < closestDistance) {
				closest = vehicle;
				closestDistance = vehicleDistance;
			}
		}
		return closest;
	}

	/**
	 * Gets the number of people in the vehicle who needs to be rescued.
	 * 
	 * @param vehicle the vehicle.
	 * @return number of people.
	 */
	public static int getRescuePeopleNum(Vehicle vehicle) {
		int result = 0;

		if (vehicle instanceof Crewable)
			result = ((Crewable) vehicle).getCrewNum();
		return result;
	}

	/**
	 * Gets the number of robots in the vehicle that needs to be rescued.
	 * 
	 * @param vehicle
	 * @return
	 */
	public static int getRescueRobotsNum(Vehicle vehicle) {
		int result = 0;

		if (vehicle instanceof Crewable)
			result = ((Crewable) vehicle).getRobotCrewNum();
		return result;
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
	 * Gets a map of resources needed for the remaining mission.
	 * 
	 * @param useBuffer
	 * @return a map of resources
	 */
	@Override
	public Map<Integer, Number> getResourcesNeededForRemainingMission(boolean useBuffer) {

		Map<Integer, Number> result = super.getResourcesNeededForRemainingMission(useBuffer);

		// Include rescue resources if needed.
		if (rescue && (getRover().getTowedVehicle() == null)) {
			Map<Integer, Number> rescueResources = determineRescueResourcesNeeded(useBuffer);

			for (Integer id : rescueResources.keySet()) {
				if (id < ResourceUtil.FIRST_ITEM_RESOURCE_ID) {
					double amount = (Double) rescueResources.get(id);
					if (result.containsKey(id)) {
						amount += (Double) result.get(id);
					}
					if (useBuffer) {
						amount *= RESCUE_RESOURCE_BUFFER;
					}
					result.put(id, amount);
					
				}  // Check if these resources are Parts
				else if (id >= ResourceUtil.FIRST_ITEM_RESOURCE_ID && id < ResourceUtil.FIRST_VEHICLE_RESOURCE_ID) {
					int num = (Integer) rescueResources.get(id);
					if (result.containsKey(id)) {
						num += (Integer) result.get(id);
					}
					result.put(id, num);
				}
			}
		}

		return result;
	}

	/**
	 * Gets a map of equipment needed for the remaining mission.
	 * 
	 * @param useBuffer
	 * @return a map of equipment
	 */
	@Override
	public Map<Integer, Integer> getEquipmentNeededForRemainingMission(boolean useBuffer) {
		if (equipmentNeededCache != null) {
			return equipmentNeededCache;
		} else {
			Map<Integer, Integer> result = new HashMap<>();
			equipmentNeededCache = result;
			return result;
		}
	}

	/**
	 * Gets the mission qualification score
	 * 
	 * @param member
	 * @return the score
	 */
	@Override
	public double getMissionQualification(MissionMember member) {
		double result = 0D;

		result = super.getMissionQualification(member);

		if (member instanceof Person) {
			Person person = (Person) member;

			// If person has the "Driver" job, add 1 to their qualification.
			if (person.getMind().getJob() == JobType.PILOT) {
				result += 1D;
			}
		}

		return result;
	}


	/**
	 * Checks if this is the closest settlement to a beacon vehicle that could
	 * rescue/salvage it.
	 * 
	 * @param thisSettlement this settlement.
	 * @param thisVehicle    the beacon vehicle.
	 * @return true if this is the closest settlement.
	 * @throws MissionException if error in checking settlements.
	 */
	public static boolean isClosestCapableSettlement(Settlement thisSettlement, Vehicle thisVehicle) {
		boolean result = true;

		double distance = thisSettlement.getCoordinates().getDistance(thisVehicle.getCoordinates());

		Iterator<Settlement> iS = unitManager.getSettlements().iterator();
		while (iS.hasNext() && result) {
			Settlement settlement = iS.next();
			if (settlement != thisSettlement) {
				double settlementDistance = settlement.getCoordinates().getDistance(thisVehicle.getCoordinates());
				if (settlementDistance < distance) {
					if (settlement.getIndoorPeopleCount() >= MIN_GOING_MEMBERS) {
						Iterator<Vehicle> iV = settlement.getParkedVehicles().iterator();
						while (iV.hasNext() && result) {
							Vehicle vehicle = iV.next();
							if (vehicle instanceof Rover) {
								if (vehicle.getRange(MISSION_TYPE) >= (settlementDistance * 2D)) {
									result = false;
								}
							}
						}
					}
				}
			}
		}

		return result;
	}

	/**
	 * Gets the resources needed for loading the vehicle.
	 * 
	 * @return resources and their number.
	 * @throws MissionException if error determining resources.
	 */
	public Map<Integer, Number> getResourcesToLoad() {
		// Override and full rover with fuel and life support resources.
		Map<Integer, Number> result = new HashMap<Integer, Number>(4);

		result.put(getVehicle().getFuelType(), getVehicle().getAmountResourceCapacity(getVehicle().getFuelType()));
		result.put(OXYGEN_ID, getVehicle().getAmountResourceCapacity(OXYGEN_ID));
		result.put(WATER_ID, getVehicle().getAmountResourceCapacity(WATER_ID));
		result.put(FOOD_ID, getVehicle().getAmountResourceCapacity(FOOD_ID));

		// Get parts too.
		result.putAll(getSparePartsForTrip(getEstimatedTotalRemainingDistance()));

		return result;
	}

	
	@Override
	public void destroy() {
		super.destroy();

		vehicleTarget = null;
		missionManager = null;
	}
}
