/*
 * Mars Simulation Project
 * Mission.java
 * @date 2021-10-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.environment.SurfaceFeatures;
import org.mars_sim.msp.core.environment.TerrainElevation;
import org.mars_sim.msp.core.equipment.EquipmentType;
import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.events.HistoricalEventManager;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.EventType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.ShiftType;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.job.JobType;
import org.mars_sim.msp.core.person.ai.social.RelationshipManager;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.ai.job.RobotJob;
import org.mars_sim.msp.core.science.ScientificStudyManager;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.goods.CreditManager;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.Temporal;
import org.mars_sim.msp.core.tool.Conversion;
import org.mars_sim.msp.core.tool.RandomUtil;


/**
 * The Mission class represents a large multi-person task There is at most one
 * instance of a mission per person. A Mission may have one or more people
 * associated with it.
 */
public abstract class Mission implements Serializable, Temporal {

	// Plain POJO to help score potential mission members
	private final static class MemberScore {
		Person candidate;
		double score;
		
		private MemberScore(Person candidate, double personValue) {
			super();
			this.candidate = candidate;
			this.score = personValue;
		}
		
		public double getScore() {
			return score;
		}

		@Override
		public String toString() {
			return "MemberScore [candidate=" + candidate + ", score=" + score + "]";
		}
	}
	
	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(Mission.class.getName());

	public static final String MISSION_SUFFIX = " mission";
	public static final String[] EXPRESSIONS = new String[] {
			"Where is everybody when I need someone for ",
			"So no one is available for ",
			"How come no one is available for "
	};	
	public static final String OUTSIDE = "Outside";

	
	public static final int MAX_CAP = 8;

	// Global mission identifier
	private static int missionIdentifer = 1;

	/**
	 * The marginal factor for the amount of water to be brought during a mission.
	 */
	public final static double WATER_MARGIN = 1.5;
	/**
	 * The marginal factor for the amount of oxygen to be brought during a mission.
	 */
	public final static double OXYGEN_MARGIN = 1.5;
	/**
	 * The marginal factor for the amount of food to be brought during a mission.
	 */
	public final static double FOOD_MARGIN = 1.5;
	/**
	 * The marginal factor for the amount of dessert to be brought during a mission.
	 */
	public final static double DESSERT_MARGIN = 1.5;

	
	// Data members
	/** Unique identifier */
	private int identifier;
	/** The minimum number of members for mission. */
	private int minMembers;
	/** The number of people that can be in the mission. */
	private int missionCapacity;
	/** The recorded number of people participated in this mission. */
	private int membersNum;
	/** The mission priority (between 1 and 5, with 1 the lowest, 5 the highest) */
	private int priority = 2;
	
	/** Has the current phase ended? */
	private boolean phaseEnded;
	/** True if mission is completed. */
	private boolean done = false;
	/** True if the mission has been approved. */
	private boolean approved = false;
	/** True if the mission has been requested. */
	protected boolean requested = false;
	
	/** The name of the settlement. */
	private String settlementName;
	/** The name of the vehicle reserved. */
	private String vehicleReserved;
	/** The date the mission was filed. */
	private String dateFiled = "";
	/** The date the mission embarked. */
	private String dateEmbarked = "";
	/** The date the mission was completed. */
	private String dateCompleted = "";
	/** The Name of this mission. */
	private String missionName;
	/** The description of this mission. */
	private String description;
	/** The description of the current phase of operation. */
	private String phaseDescription;
	/** The full mission designation. */
	private String fullMissionDesignation = "";
	
	/** The mission type enum. */
	private MissionType missionType;

	/** A list of mission members' names. */
	private Map<String, String> membersMap;
	/** A list of mission status. */
	private List<MissionStatus> missionStatus;
	/** The current phase of the mission. */
	private MissionPhase phase;
	/** The name of the starting member */
	private MissionMember startingMember;
	/** The mission plan. */
	private MissionPlanning plan;

	/** Mission members. */
	private Collection<MissionMember> members;
	/** A collection of the mission's phases. */
	private Collection<MissionPhase> phases;

	// transient members
	/** Mission listeners. */
	private transient List<MissionListener> listeners;

	// Static members
	protected static Simulation sim = Simulation.instance();
	
	protected static UnitManager unitManager;
	protected static HistoricalEventManager eventManager;
	protected static MissionManager missionManager;
	protected static ScientificStudyManager scientificManager;
	protected static SurfaceFeatures surfaceFeatures;
	protected static PersonConfig personConfig;
	protected static MarsClock marsClock;
	protected static RelationshipManager relationshipManager;
	protected static CreditManager creditManager;
	protected static TerrainElevation terrainElevation;

	/**
	 * Must be synchronised to prevent duplicate ids being assigned via different
	 * threads.
	 * 
	 * @return
	 */
	private static synchronized int getNextIdentifier() {
		return missionIdentifer++;
	}

	/**
	 * Constructor 1
	 * 
	 * @param missionName
	 * @param startingMember
	 * @param minMembers
	 */
	public Mission(String missionName, MissionType missionType, MissionMember startingMember, int minMembers) {
		// Initialize data members
		this.identifier = getNextIdentifier();
		this.missionName = missionName;
		this.missionType = missionType;
		this.startingMember = startingMember;
		this.description = missionName;
		
		// Create the date filed timestamp
		createDateFiled();

		membersMap = new HashMap<>();
		missionStatus = new CopyOnWriteArrayList<>();
		members = new ConcurrentLinkedQueue<MissionMember>();
		done = false;
		phase = null;
		phaseDescription = "";
		phases = new CopyOnWriteArrayList<MissionPhase>();
		phaseEnded = false;
		this.minMembers = minMembers;
		missionCapacity = MAX_CAP;
				
		listeners = new CopyOnWriteArrayList<>();
		
		Person person = (Person) startingMember;
		String loc0 = null;
		String loc1 = null;

		if (person.isInSettlement()) {
			loc0 = person.getBuildingLocation().getNickName();
			loc1 = person.getSettlement().getName();

			// Created mission starting event.
			HistoricalEvent newEvent = new MissionHistoricalEvent(EventType.MISSION_START, this,
					"Mission Starting", missionName, person.getName(), loc0, loc1, person.getAssociatedSettlement().getName());
			
			eventManager.registerNewEvent(newEvent);

			// Log mission starting.
			int n = members.size();
			String appendStr = "";
			if (n == 0)
				appendStr = ".";
			else if (n == 1)
				appendStr = "' with 1 other.";
			else
				appendStr = "' with " + n + " others.";
			
			String article = "a ";
					
			String missionStr = missionName;
			
			if (!missionStr.toLowerCase().contains("mission"))
				missionStr = missionName + " mission";
				
			if(Conversion.isVowel(missionName))
				article = "an ";

			logger.log(startingMember, Level.INFO, 0, 
					"Began organizing " + article + missionStr + appendStr);

			// Add starting member to mission.
			startingMember.setMission(this);
			// Save the settlement name for posterity
			settlementName = startingMember.getSettlement().getName();
			
			// Note: do NOT set his shift to ON_CALL yet.
			// let the mission lead have more sleep before departing

		}

	}

	/**
	 * Gets the date filed timestamp of the mission.
	 * 
	 * @return
	 */
	public String getDateFiled() {
		return dateFiled;
	}

	/**
	 * Gets the date embarked timestamp of the mission.
	 * 
	 * @return
	 */
	public String getDateEmbarked() {
		return dateEmbarked;
	}

	/**
	 * Gets the date returned timestamp of the mission.
	 * 
	 * @return
	 */
	public String getDateReturned() {
		return dateCompleted;
	}

	/**
	 * Creates the date filed timestamp of the mission.
	 */
	private void createDateFiled() {
		if (dateFiled.equals("")) {
			dateFiled = marsClock.getTrucatedDateTimeStamp();
			fireMissionUpdate(MissionEventType.DATE_EVENT, dateFiled);
		}
	}
	
	/**
	 * Creates the date completed timestamp of the mission.
	 */
	public void createDateCompleted() {
		if (dateCompleted.equals("")) {
			dateCompleted = marsClock.getTrucatedDateTimeStamp();
			fireMissionUpdate(MissionEventType.DATE_EVENT, dateCompleted);
		}
	}

	/**
	 * Creates the date embarked timestamp of the mission.
	 */
	public void createDateEmbarked() {
		if (dateEmbarked.equals("")) {
			dateEmbarked = marsClock.getTrucatedDateTimeStamp();
			fireMissionUpdate(MissionEventType.DATE_EVENT, dateEmbarked);
		}
	}
	
	/**
	 * Return the unique identifier of the mission.
	 * 
	 * @return
	 */
	public int getIdentifier() {
		return identifier;
	}

	/**
	 * Adds a listener.
	 * 
	 * @param newListener the listener to add.
	 */
	public final void addMissionListener(MissionListener newListener) {
		if (listeners == null) {
			listeners = new CopyOnWriteArrayList<>();//Collections.synchronizedList(new ArrayList<MissionListener>());
		}
		if (!listeners.contains(newListener)) {
			listeners.add(newListener);
		}
	}

	/**
	 * Removes a listener.
	 * 
	 * @param oldListener the listener to remove.
	 */
	public final void removeMissionListener(MissionListener oldListener) {
		if (listeners == null) {
			listeners = new CopyOnWriteArrayList<>();//Collections.synchronizedList(new ArrayList<MissionListener>());
		}
		if (listeners.contains(oldListener)) {
			listeners.remove(oldListener);
		}
	}

	/**
	 * Fire a mission update event.
	 * 
	 * @param updateType the update type.
	 */
	protected final void fireMissionUpdate(MissionEventType updateType) {
		fireMissionUpdate(updateType, this);
	}

	/**
	 * Fire a mission update event.
	 * 
	 * @param addMemberEvent the update type.
	 * @param target         the event target or null if none.
	 */
	protected final void fireMissionUpdate(MissionEventType addMemberEvent, Object target) {
		if (listeners == null)
			listeners = new CopyOnWriteArrayList<>();
			Iterator<MissionListener> i = listeners.iterator();
			while (i.hasNext())
				i.next().missionUpdate(new MissionEvent(this, addMemberEvent, target));
	}

	/**
	 * Gets the string representation of this mission.
	 */
	public String toString() {
		return this.getTypeID();
	}

	public final void addMember(MissionMember member) {
		if (!members.contains(member)) {
			members.add(member);

			Person person = (Person) member;
			String role = "";
			if (members.size() == 0)
				role = "Lead";
			if (person.getMind().getJob() == JobType.PILOT)
				role = JobType.PILOT.getName();			
					
			membersMap.put(person.getName(), role);
			
			String loc0 = null;
			String loc1 = null;
			if (person.isInSettlement()) {
				loc0 = person.getBuildingLocation().getNickName();
				loc1 = person.getSettlement().getName();
			} else if (person.isInVehicle()) {
				loc0 = person.getVehicle().getName();

				if (person.getVehicle().getBuildingLocation() != null)
					loc1 = person.getVehicle().getSettlement().getName();
				else
					loc1 = person.getCoordinates().toString();
			} else {
				loc0 = OUTSIDE;
				loc1 = person.getCoordinates().toString();
			}

			// Creating mission joining event.
			HistoricalEvent newEvent = new MissionHistoricalEvent(EventType.MISSION_JOINING, this,
					"Adding a member", missionName, person.getName(), loc0, loc1, person.getAssociatedSettlement().getName());

			eventManager.registerNewEvent(newEvent);

			fireMissionUpdate(MissionEventType.ADD_MEMBER_EVENT, member);

			logger.log(member, Level.FINER, 0, "Just got added to " + missionName + " #" + identifier + ".");
		}
	}

	/**
	 * Removes a member from the mission.
	 * 
	 * @param member to be removed
	 */
	public final void removeMember(MissionMember member) {
		if (members.contains(member)) {
			members.remove(member);

			// Added codes in reassigning a work shift
			if (member instanceof Person) {
				Person person = (Person) member;
				person.getMind().stopMission();
				member.setMission(null);

				ShiftType shift = null;
				if (person.getSettlement() != null) {
					shift = person.getSettlement().getAnEmptyWorkShift(-1);
					person.setShiftType(shift);
				} else if (person.getVehicle() != null)
					if (person.getVehicle().getSettlement() != null) {
						shift = person.getVehicle().getSettlement().getAnEmptyWorkShift(-1);
						person.setShiftType(shift);
					}

				String loc0 = null;
				String loc1 = null;
				if (person.isInSettlement()) {
					loc0 = person.getBuildingLocation().getNickName();
					loc1 = person.getSettlement().getName();
				} else if (person.isInVehicle()) {
					loc0 = person.getVehicle().getName();
					if (person.getVehicle().getBuildingLocation() != null)
						loc1 = person.getVehicle().getSettlement().getName();
					else
						loc1 = person.getCoordinates().toString();
				} else {
					loc0 = OUTSIDE;
					loc1 = person.getCoordinates().toString();
				}

				eventManager.registerNewEvent(new MissionHistoricalEvent(EventType.MISSION_FINISH, this,
								"Removing a member", missionName, person.getName(), loc0, loc1, person.getAssociatedSettlement().getName()));
				fireMissionUpdate(MissionEventType.REMOVE_MEMBER_EVENT, member);

				if (getPeopleNumber() == 0 && !done) {
					addMissionStatus(MissionStatus.NO_MEMBERS_AVAILABLE);
					endMission();
				}
			}
		}
	}

	/**
	 * Determines if a mission includes the given member.
	 * 
	 * @param member member to be checked
	 * @return true if member is a part of the mission.
	 */
	public final boolean hasMember(MissionMember member) {
		return members.contains(member);
	}

	/**
	 * Gets the number of members in the mission.
	 * 
	 * @return number of members.
	 */
	public final int getMembersNumber() {
		if (members.size() > 0)
			membersNum = members.size();
		return membersNum;
	}
	
	/**
	 * Gets the number of people in the mission.
	 * 
	 * @return number of people
	 */
	public final int getPeopleNumber() {
		return getMembersNumber();
	}

	/**
	 * Gets the minimum number of members required for mission.
	 * 
	 * @return minimum number of members
	 */
	public final int getMinMembers() {
		return minMembers;
	}

	/**
	 * Sets the minimum number of members required for a mission.
	 * 
	 * @param minMembers minimum number of members
	 */
	protected final void setMinMembers(int minMembers) {
		this.minMembers = minMembers;
		fireMissionUpdate(MissionEventType.MIN_MEMBERS_EVENT, minMembers);
	}

	/**
	 * Gets a collection of the members in the mission.
	 * 
	 * @return collection of members
	 */
	public final Collection<MissionMember> getMembers() {
		return new ConcurrentLinkedQueue<MissionMember>(members);
	}

	public void setMembers(MissionMember member) {
		members.add(member);
	}
	

	/**
	 * Determines if mission is completed.
	 * 
	 * @return true if mission is completed
	 */
	public final boolean isDone() {
		return done;
	}

	/**
	 * Set mission as done.
	 */
	public void setDone(boolean value) {
		done = value;
	}

	/**
	 * Gets the name of the mission.
	 * 
	 * @return name of mission
	 */
	public final String getName() {
		return missionName;
	}

	/**
	 * Gets the type and identifier of the mission.
	 * 
	 * @return type and identifier of the mission
	 */
	public final String getTypeID() {
		return missionName + " #" + identifier;
	}
	
	
	/**
	 * Sets the name of the mission.
	 * 
	 * @param name the new mission name
	 */
	protected final void setName(String name) {
		this.missionName = name;
		fireMissionUpdate(MissionEventType.NAME_EVENT, name);
	}

	/**
	 * Gets the mission type enum.
	 * 
	 * @return
	 */
	public MissionType getMissionType() {
		return missionType;
	}
	
	
	/**
	 * Gets the mission's description.
	 * 
	 * @return mission description
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Sets the mission's description.
	 * 
	 * @param description the new description.
	 */
	public final void setDescription(String description) {
		if (!this.description.equals(description)) {
			this.description = description;
			fireMissionUpdate(MissionEventType.DESIGNATION_EVENT, description);
		}
	}

	/**
	 * Gets the current phase of the mission.
	 * 
	 * @return phase
	 */
	public final MissionPhase getPhase() {
		return phase;
	}

	/**
	 * Sets the mission phase.
	 * 
	 * @param newPhase the new mission phase.
	 * @throws MissionException if newPhase is not in the mission's collection of
	 *                          phases.
	 */
	public final void setPhase(MissionPhase newPhase) {
		if (newPhase == null) {
			throw new IllegalArgumentException("newPhase is null");
		} else if (phases.contains(newPhase)) {
			phase = newPhase;
			setPhaseEnded(false);
			phaseDescription = "";
			fireMissionUpdate(MissionEventType.PHASE_EVENT, newPhase);
		} else {
			throw new IllegalStateException(
					phase + " : newPhase: " + newPhase + " is not a valid phase for this mission.");
		}
	}

	/**
	 * Adds a phase to the mission's collection of phases.
	 * 
	 * @param newPhase the new phase to add.
	 */
	public final void addPhase(MissionPhase newPhase) {
		if (newPhase == null) {
			throw new IllegalArgumentException("newPhase is null");
		} else if (phases.isEmpty() || !phases.contains(newPhase)) {
			phases.add(newPhase);
		}
	}

	/**
	 * Gets the description of the current phase.
	 * 
	 * @return phase description.
	 */
	public final String getPhaseDescription() {
		if (phaseDescription != null && !phaseDescription.equals("")) {
			return phaseDescription;
		} else if (phase != null) {
			return phase.toString();
		} else
			return "";
	}

	/**
	 * Sets the description of the current phase.
	 * 
	 * @param description the phase description.
	 */
	protected final void setPhaseDescription(String description) {
		phaseDescription = description;
		fireMissionUpdate(MissionEventType.PHASE_DESCRIPTION_EVENT, description);
	}

	/**
	 * Performs the mission.
	 * 
	 * @param member the member performing the mission.
	 */
	public void performMission(MissionMember member) {

		// If current phase is over, decide what to do next.
		if (phaseEnded) {
			determineNewPhase();
		}

		// Perform phase.
		if (!done) {
			performPhase(member);
		}
	}

	/**
	 * Determines a new phase for the mission when the current phase has ended.
	 * 
	 * @throws MissionException if problem setting a new phase.
	 */
	protected abstract void determineNewPhase();

	/**
	 * The member performs the current phase of the mission.
	 * 
	 * @param member the member performing the phase.
	 */
	protected void performPhase(MissionMember member) {
		if (phase == null) {
			addMissionStatus(MissionStatus.CURRENT_MISSION_PHASE_IS_NULL);
			endMission();
		}
	}

	/**
	 * Gets the mission capacity for participating people.
	 * 
	 * @return mission capacity
	 */
	public final int getMissionCapacity() {
		return missionCapacity;
	}

	/**
	 * Sets the mission capacity to a given value.
	 * 
	 * @param newCapacity the new mission capacity
	 */
	protected final void setMissionCapacity(int newCapacity) {
		missionCapacity = newCapacity;
		fireMissionUpdate(MissionEventType.CAPACITY_EVENT, newCapacity);
	}

	/**
	 * End the mission collection phase at the current site.
	 */
	protected void endCollectionPhase() {
		if (this instanceof CollectResourcesMission)
			((CollectResourcesMission) this).endCollectingAtSite();
	}

	/**
	 * Have the mission return home and end collection phase if necessary.
	 */
	protected void returnHome() {
		if (this instanceof TravelMission) {
			TravelMission travelMission = (TravelMission) this;
			int offset = 2;
			if (travelMission.getPhase().equals(VehicleMission.TRAVELLING))
				offset = 1;
			travelMission.setNextNavpointIndex(travelMission.getNumberOfNavpoints() - offset);
			travelMission.updateTravelDestination();
			endCollectionPhase();
		}
	}

	/**
	 * Go to the nearest settlement and end collection phase if necessary.
	 */
	// Note : connect to determineEmergencyDestination() in VehicleMission
	public void goToNearestSettlement() {
		if (this instanceof VehicleMission) {
			VehicleMission vehicleMission = (VehicleMission) this;
			try {
				Settlement nearestSettlement = vehicleMission.findClosestSettlement();
				if (nearestSettlement != null) {
					vehicleMission.clearRemainingNavpoints();
					vehicleMission.addNavpoint(new NavPoint(nearestSettlement.getCoordinates(), nearestSettlement,
							nearestSettlement.getName()));
					// Note: Not sure if they should become citizens of another settlement
					vehicleMission.updateTravelDestination();
					endCollectionPhase();
				}
			} catch (Exception e) {
			}
		}
	}

	/**
	 * Computes the mission experience score
	 * 
	 * @param reason
	 */
	private void addMissionScore() {
		if (haveMissionStatus(MissionStatus.MISSION_ACCOMPLISHED)) {
			for (MissionMember member : members) {
				if (member instanceof Person) {
					Person person = (Person) member;
					
					if (!person.isDeclaredDead()) {
						if (person.getPhysicalCondition().hasSeriousMedicalProblems()) {
							// Note : there is a minor penalty for those who are sick 
							// and thus unable to fully function during the mission
							person.addMissionExperience(missionType, 2);
						}
						else if (person.equals(startingMember)) {
							// The mission lead receive extra bonus
							person.addMissionExperience(missionType, 6);
							
							// Add a leadership point to the mission lead
							person.getNaturalAttributeManager().adjustAttribute(NaturalAttributeType.LEADERSHIP, 1);	
						}
						else
							person.addMissionExperience(missionType, 3);
					}
				}
			}
		}
	}
	
	/**
	 * Finalizes the mission. String reason Reason for ending mission. Mission can
	 * override this to perform necessary finalizing operations.
	 * 
	 * @param reason
	 */
	public void endMission() {
		
		logger.log(startingMember, Level.INFO, 0, "Ended " + getTypeID() + ".");

		// Add mission experience score
		addMissionScore();
		
		// Note: !done is very important to keep !
		if (!done) {
			// Note: there can be custom reason such as "Equipment EVA Suit 12 cannot be
			// loaded in rover Rahu" with mission name 'Trade With Camp Bradbury'
	
			done = true; // Note: done = true is very important to keep !
		}
		
		else {
			StringBuilder status = new StringBuilder();
			status.append("Ended the ").append(missionName).append(" with the following status flag(s) :");
		
			for (int i=0; i< missionStatus.size(); i++) {
				status.append(" (").append(i+1).append(") ").append(missionStatus.get(i).getName());
			}
			logger.log(startingMember, Level.INFO, 500, status.toString());
		}
		
		// The members are leaving the mission
		if (members != null && !members.isEmpty()) { 
			logger.log(startingMember, Level.INFO, 500, "Disbanded mission member(s) : " + members);
			Iterator<MissionMember> i = members.iterator();
			while (i.hasNext()) {
				removeMember(i.next());
			}
		}
		
		if (haveMissionStatus(MissionStatus.MISSION_ACCOMPLISHED)) {
			if (this instanceof VehicleMission) {
				setPhase(VehicleMission.COMPLETED);
			}
			setPhaseDescription(
					Msg.getString("Mission.phase.completed.description")); // $NON-NLS-1$
		}
		
		else {
			String s = "";
			for (MissionStatus ms : missionStatus) {
				s += ms.getName();
			}
			
			setPhaseDescription(
					Msg.getString("Mission.phase.aborted.description", s)); // $NON-NLS-1$
		}
		
		// Proactively call removeMission to update the list in MissionManager right away
		// Note: Calling missionManager.removeMission(this) will crash the mission tab in monitor tool
	}

	/**
	 * Checks if a person has any issues in starting a new task 
	 * 
	 * @param person the person to assign to the task
	 * @param task   the new task to be assigned
	 * @return true if task can be performed.
	 */
	protected boolean assignTask(Person person, Task task) {
		boolean canPerformTask = !task.isEffortDriven() || (person.getPerformanceRating() != 0D);

		// If task is effort-driven and person too ill, do not assign task.

        if (canPerformTask) {
			canPerformTask = person.getMind().getTaskManager().addTask(task);
		}

		return canPerformTask;
	}

	/**
	 * Adds a new task for a robot in the mission. Task may be not assigned if the
	 * robot has a malfunction.
	 * 
	 * @param robot the robot to assign to the task
	 * @param task  the new task to be assigned
	 * @return true if task can be performed.
	 */
	protected boolean assignTask(Robot robot, Task task) {
		boolean canPerformTask = true;

		// If robot is malfunctioning, it cannot perform task.
		boolean hasMalfunction = robot.getMalfunctionManager().hasMalfunction();
		if (hasMalfunction) {
			canPerformTask = false;
		}

		if (canPerformTask) {
			canPerformTask = robot.getBotMind().getBotTaskManager().addTask(task);
		}

		return canPerformTask;
	}

	/**
	 * Checks to see if any of the people in the mission have any dangerous medical
	 * problems that require treatment at a settlement. Also any environmental
	 * problems, such as suffocation.
	 * 
	 * @return true if dangerous medical problems
	 */
	private final boolean hasDangerousMedicalProblems() {
		for (MissionMember member : members) {
			if (member instanceof Person) {
				Person person = (Person) member;
				if (person.getPhysicalCondition().hasSeriousMedicalProblems()) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Checks to see if any of the people in the mission have any potential medical
	 * problems due to low fitness level that will soon degrade into illness.
	 * 
	 * @return true if potential medical problems exist
	 */
	protected final boolean hasAnyPotentialMedicalProblems() {
		for (MissionMember member : members) {
			if (member instanceof Person) {
				Person person = (Person) member;
				if (person.getPhysicalCondition().computeFitnessLevel() < 2) {
					return true;
				}
			}
		}

		return false;
	}
	
	/**
	 * Checks to see if all of the people in the mission have any dangerous medical
	 * problems that require treatment at a settlement. Also any environmental
	 * problems, such as suffocation.
	 * 
	 * @return true if all have dangerous medical problems
	 */
	public final boolean hasDangerousMedicalProblemsAllCrew() {
		boolean result = true;
		for (MissionMember member : members) {
			if (member instanceof Person) {
				Person person = (Person) member;
				if (!person.getPhysicalCondition().hasSeriousMedicalProblems()) {
					result = false;
				}
			}
		}
		return result;
	}

	/**
	 * Checks if the mission has an emergency situation.
	 * 
	 * @return true if emergency.
	 */
	protected boolean hasEmergency() {
		return hasDangerousMedicalProblems();
	}

	/**
	 * Checks if the mission has an emergency situation affecting all the crew.
	 * 
	 * @return true if emergency affecting all.
	 */
	public boolean hasEmergencyAllCrew() {
		return hasDangerousMedicalProblemsAllCrew();
	}

	/**
	 * Recruits new members into the mission.
	 * 
	 * @param startingMember the mission member starting the mission.
	 * @param sameSettlement do members have to be at the same Settlement as the starting Member
	 */
	protected boolean recruitMembersForMission(MissionMember startingMember, boolean sameSettlement) {
		
		// Get all people qualified for the mission.
		Collection<Person> possibles;
		if (sameSettlement) {
			possibles = startingMember.getAssociatedSettlement().getAllAssociatedPeople();
		}
		else {
			possibles = unitManager.getPeople();
		}
		
		List<MemberScore> qualifiedPeople = new ArrayList<>();
		for(Person person : possibles) {
			if (isCapableOfMission(person)) {
				// Determine the person's mission qualification.
				double qualification = getMissionQualification(person) * 100D;

				// Determine how much the recruiter likes the person.
				double likability = 50D;
				if (startingMember instanceof Person) {
					likability = relationshipManager.getOpinionOfPerson((Person) startingMember, person);
				}

				// Check if person is the best recruit.
				double personValue = (qualification + likability) / 2D;
				qualifiedPeople.add(new MemberScore(person, personValue));
			}
		}

		int pop = startingMember.getAssociatedSettlement().getNumCitizens();
		int max = 0;
		
		if (pop < 4)
			max = 1;
		else if (pop >= 4 && pop < 7)
			max = 2;
		else if (pop >= 7 && pop < 10)
			max = 3;
		else if (pop >= 10 && pop < 14)
			max = 4;
		else if (pop >= 14 && pop < 18)
			max = 5;
		else if (pop >= 18 && pop < 23)
			max = 6;
		else if (pop >= 23 && pop < 29)
			max = 7;
		else if (pop >= 29)
			max = 8;
		
		// 50% tendency to have 1 less person
		int rand = RandomUtil.getRandomInt(1);
		if (rand == 1) {
			if (max >= 5)
			max--;
		}
		
		// Max can not bigger than mission capacity
		max = Math.min(max, missionCapacity);
		
		// Recruit the most qualified and most liked people first.
		qualifiedPeople.sort(Comparator.comparing(MemberScore::getScore, Comparator.reverseOrder()));
		while (!qualifiedPeople.isEmpty() && (members.size() < max)) {

			// Try to recruit best person available to the mission.
			MemberScore next = qualifiedPeople.remove(0);
			recruitPerson(startingMember, next.candidate);
		}

		if (getMembersNumber() < minMembers) {
			addMissionStatus(MissionStatus.NOT_ENOUGH_MEMBERS);
			endMission();
			return false;
		}
		
		return true;
	}

	/**
	 * Attempt to recruit a new person into the mission.
	 * 
	 * @param recruiter the mission member doing the recruiting.
	 * @param recruitee the person being recruited.
	 */
	private void recruitPerson(MissionMember recruiter, Person recruitee) {
		if (isCapableOfMission(recruitee)) {
			// Get mission qualification modifier.
			double qualification = getMissionQualification(recruitee) * 100D;
			// Get the recruitee's social opinion of the recruiter.
			double recruiterLikability = 50D;
			if (recruiter instanceof Person) {
				recruiterLikability = relationshipManager.getOpinionOfPerson(recruitee, (Person) recruiter);
			}

			// Get the recruitee's average opinion of all the current mission members.
			List<Person> people = new CopyOnWriteArrayList<Person>();
			Iterator<MissionMember> i = members.iterator();
			while (i.hasNext()) {
				MissionMember member = i.next();
				if (member instanceof Person) {
					people.add((Person) member);
				}
			}
			double groupLikability = relationshipManager.getAverageOpinionOfPeople(recruitee, people);

			double recruitmentChance = (qualification + recruiterLikability + groupLikability) / 3D;
			if (recruitmentChance > 100D) {
				recruitmentChance = 100D;
			} else if (recruitmentChance < 0D) {
				recruitmentChance = 0D;
			}

			if (RandomUtil.lessThanRandPercent(recruitmentChance)) {
				recruitee.setMission(this);

				// NOTE: do not set his shift to ON_CALL until after the mission plan has been approved
			}
		}
	}

	/**
	 * Checks to see if a member is capable of joining a mission.
	 * 
	 * @param member the member to check.
	 * @return true if member could join mission.
	 */
	protected boolean isCapableOfMission(MissionMember member) {
		boolean result = false;

		if (member == null) {
			throw new IllegalArgumentException("member is null");
		}

		if (member instanceof Person) {
			Person person = (Person) member;

			// Make sure person isn't already on a mission.
			boolean onMission = (person.getMind().getMission() != null);

			// Make sure person doesn't have any serious health problems.
			boolean healthProblem = person.getPhysicalCondition().hasSeriousMedicalProblems();

			// Check if person is qualified to join the mission.
			boolean isQualified = (getMissionQualification(person) > 0D);

			if (!onMission && !healthProblem && isQualified) {
				result = true;
			}
		}

		return result;
	}
	
	/**
	 * Gets the mission qualification value for the member. Member is qualified in
	 * joining the mission if the value is larger than 0. The larger the
	 * qualification value, the more likely the member will be picked for the
	 * mission.
	 * 
	 * @param member the member to check.
	 * @return mission qualification value.
	 */
	public double getMissionQualification(MissionMember member) {

		double result = 0D;

		if (member instanceof Person) {
			Person person = (Person) member;
			result = Math.max(5,  person.getMissionExperience(missionType));
			
			// Get base result for job modifier.
			Set<JobType> prefered = getPreferredPersonJobs();
			JobType job = person.getMind().getJob();
			double jobModifier;
			if ((prefered != null) && prefered.contains(job)) {
				jobModifier = 1D;
			}
			else {
				jobModifier = 0.5D;
			}
				
			result = result + 2 * result * jobModifier;			
		} else if (member instanceof Robot) {
			Robot robot = (Robot) member;

			// Get base result for job modifier.
			RobotJob job = robot.getBotMind().getRobotJob();
			if (job != null) {
				result = job.getJoinMissionProbabilityModifier(this.getClass());
			}
		}

		return result;
	}

	/**
	 * Get the preferred Job types.
	 * @return
	 */
	protected Set<JobType> getPreferredPersonJobs() {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Checks if the current phase has ended or not.
	 * 
	 * @return true if phase has ended
	 */
	public final boolean getPhaseEnded() {
		return phaseEnded;
	}

	/**
	 * Sets if the current phase has ended or not.
	 * 
	 * @param phaseEnded true if phase has ended
	 */
	protected final void setPhaseEnded(boolean phaseEnded) {
		this.phaseEnded = phaseEnded;
	}

	/**
	 * Gets the settlement associated with the mission.
	 * 
	 * @return settlement or null if none.
	 */
	public abstract Settlement getAssociatedSettlement();

	/**
	 * Gets the number and amounts of resources needed for the mission.
	 * 
	 * @param useBuffer use time buffers in estimation if true.
	 * @return map of amount and item resources and their Double amount or Integer
	 *         number.
	 */
	public abstract Map<Integer, Number> getResourcesNeededForRemainingMission(boolean useBuffer);

	/**
	 * Gets the number and types of equipment needed for the mission.
	 * 
	 * @param useBuffer use time buffers in estimation if true.
	 * @return map of equipment types and number.
	 */
	public abstract Map<Integer, Integer> getEquipmentNeededForRemainingMission(boolean useBuffer);

	/**
	 * Time passing for mission.
	 * 
	 * @param time the amount of time passing (in millisols)
	 * @throws Exception if error during time passing.
	 */
	public boolean timePassing(ClockPulse pulse) {
		return true;
	}


	/**
	 * Gets the current location of the mission.
	 * 
	 * @return coordinate location.
	 * @throws MissionException if error determining location.
	 */
	public Coordinates getCurrentMissionLocation() {

		Coordinates result = null;
			
		if (startingMember != null)	{
			Person p = (Person)startingMember;
			Settlement s = p.getSettlement();
			if (s != null)
				return s.getCoordinates();	
			if (p.isInVehicle())
				return p.getVehicle().getCoordinates();
			else
				return p.getCoordinates();
			
		} else {
			StringBuilder s = new StringBuilder();
			String missionName = this.toString();
			if (missionName.toLowerCase().contains("mission")) {
				if (phase != null) {
					s.append("Error : no crew members on the ").append(phase).append(" phase in ")
					.append(missionName).append(" (").append(fullMissionDesignation).append(").");
				}
				else {
					s.append("Error : no crew members for ");

					if (Conversion.isVowel(missionName))
						s.append("an ");
					else
						s.append("a ");
						
					s.append(missionName).append(" (").append(fullMissionDesignation).append(").");
				}
			}
			
			else {
				if (phase != null) {
					s.append("Error : no crew members on the ").append(phase).append(" phase in ")
						.append(missionName).append(MISSION_SUFFIX).append(" (").append(fullMissionDesignation).append(").");
				}
				else {
					s.append("Error : no crew members for ");
					
					if (Conversion.isVowel(missionName))
						s.append("an ");
					else
						s.append("a ");
				
					s.append(missionName).append(MISSION_SUFFIX).append(" (").append(fullMissionDesignation).append(").");
				}
			}
			
			logger.log(startingMember, Level.INFO, 0, s.toString());
		}

		return result;
	}

	/**
	 * Gets the number of available EVA suits for a mission at a settlement.
	 * 
	 * @param settlement the settlement to check.
	 * @return number of available suits.
	 */
	public static int getNumberAvailableEVASuitsAtSettlement(Settlement settlement) {

		if (settlement == null)
			throw new IllegalArgumentException("Settlement is null");
		
		int result = settlement.findNumContainersOfType(EquipmentType.EVA_SUIT);

		// Leave one suit for settlement use.
		if (result > 0) {
			result--;
		}
		return result;
	}

	/**
	 * Request review for the mission.
	 * 
	 * @param member the mission lead.
	 */	
	protected void requestReviewPhase(MissionMember member) {	
		Person p = (Person)member;
		
		if (plan == null) {			
			plan = new MissionPlanning(this, p.getName(), p.getRole().getType());
			logger.log(member, Level.INFO, 0, "Serving as the mission lead on " + this + ".");

			 missionManager.requestMissionApproving(plan);
		}
		
		else if (plan != null) {
			if (plan.getStatus() == PlanType.NOT_APPROVED) {
				addMissionStatus(MissionStatus.MISSION_NOT_APPROVED);
				endMission();
			}
			
			else if (plan.getStatus() == PlanType.APPROVED) {
				
				fullMissionDesignation = createFullDesignation(p);
				
				logger.log(p, Level.INFO, 0, "Getting ready to embark on " + getDescription() + ".");

				if (!(this instanceof TravelMission)) {
					// Set the members' work shift to on-call to get ready
					for (MissionMember m : members) {
						 ((Person) m).setShiftType(ShiftType.ON_CALL);
					}
				}
				setPhaseEnded(true);
			}
		}
	}

	/**
	 * Sets the mission plan status
	 * 
	 * @param value
	 */
	public void setApproval(boolean value) {
		approved = value;
	}

	/**
	 * Returns the mission plan
	 * 
	 * @return {@link MissionPlanning}
	 */
	public MissionPlanning getPlan() {
		return plan;
	}
	
	/**
	 * Returns the starting person
	 * 
	 * @return {@link Person}
	 */
	public Person getStartingPerson() {
		if (startingMember instanceof Person)
			return (Person)startingMember;
		else
			return null;
	}
	
	/**
	 * Sets the starting member.
	 * 
	 * @param member the new starting member
	 */
	protected final void setStartingMember(MissionMember member) {
		this.startingMember = member;
		fireMissionUpdate(MissionEventType.STARTING_SETTLEMENT_EVENT);
	}
	
	public String getFullMissionDesignation() {
		return fullMissionDesignation;
	}

	/**
	 * Creates the mission designation string for this mission
	 * 
	 * @param person the mission lead 
	 * @return
	 */
	public String createFullDesignation(Person person) {	
		return Conversion.getInitials(getDescription().replaceAll("with", "").trim()) + " " 
				+ missionManager.getMissionDesignationString(person.getAssociatedSettlement().getName());
	}
	
	public void setReservedVehicle(String name) {
		vehicleReserved = name;
	}
	
	public String getReservedVehicle() {
		return vehicleReserved;
	}
	
	public List<MissionStatus> getMissionStatus() {
		return missionStatus;
	}
	
	/**
	 * Checks if this mission has already been tagged with this mission status
	 * 
	 * @param status
	 * @return
	 */
	public boolean haveMissionStatus(MissionStatus status) {
        return missionStatus.contains(status);
	}
	
	/**
	 * Check if the rover has mission status that prompts to turn on the beacon to ask for help 
	 * 
	 * @return
	 */
	public boolean needHelp() {
        return missionStatus.contains(MissionStatus.NOT_ENOUGH_RESOURCES)
                || missionStatus.contains(MissionStatus.NO_METHANE)
                || missionStatus.contains(MissionStatus.UNREPAIRABLE_MALFUNCTION)
                || missionStatus.contains(MissionStatus.NO_EMERGENCY_SETTLEMENT_DESTINATION_FOUND)
                || missionStatus.contains(MissionStatus.MEDICAL_EMERGENCY)
                || missionStatus.contains(MissionStatus.REQUEST_RESCUE);
	}
	
	/**
	 * Add a new mission status
	 * 
	 * @param status
	 */
	public void addMissionStatus(MissionStatus status) {
		if (!missionStatus.contains(status)) {
			missionStatus.add(status);
			logger.log(startingMember, Level.INFO, 3_000, getTypeID() 
					+ " tagged with '" + status.getName() + "'.");
		}
		else
			logger.log(startingMember, Level.WARNING, 3_000, getTypeID()
					+ " already been tagged with '" + status.getName() + "'.");
	}
	
	public int getPriority() {
		return priority;
	}
	
	public String getSettlmentName() {
		return settlementName;
	}
	
	/**
	 * Be default a worker can always participate
	 * @param worker
	 * @return
	 */
	public boolean canParticipate(MissionMember worker) {
		return true;
	}

	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		Mission m = (Mission) obj;
		return this.missionType == m.getMissionType()
				&& this.identifier == m.getIdentifier();
	}

	/**
	 * Gets the hash code for this object.
	 * 
	 * @return hash code.
	 */
	public int hashCode() {
		int hashCode = (int)(1 + identifier);
		hashCode *= missionType.hashCode();
		return hashCode;
	}
	
	/**
	 * Reloads instances after loading from a saved sim
	 * 
	 * @param si {@link Simulation}
	 * @param c {@link MarsClock}
	 * @param e {@link HistoricalEventManager}
	 * @param u {@link UnitManager}
	 * @param s {@link ScientificStudyManager}
	 * @param sf {@link SurfaceFeatures}
	 * @param m {@link MissionManager}
	 */
	public static void initializeInstances(Simulation si, MarsClock c, HistoricalEventManager e, 
			UnitManager u, ScientificStudyManager s, SurfaceFeatures sf, TerrainElevation te,
			MissionManager m, RelationshipManager r, PersonConfig pc, CreditManager cm) {
		sim = si;
		marsClock = c;		
		eventManager = e;
		unitManager = u;
		scientificManager = s;
		surfaceFeatures = sf;
		terrainElevation = te;
		missionManager = m;
		personConfig = pc;
		relationshipManager = r;
		creditManager = cm;
	}
	
	/**
	 * Prepare object for garbage collection.
	 */
	public void destroy() {
		if (members != null) {
			members.clear();
		}
		members = null;
		missionName = null;
		if (phases != null) {
			phases.clear();
		}
		phases = null;
		phase = null;
		phaseDescription = null;
		if (listeners != null) {
			listeners.clear();
		}
		listeners = null;
	}

}
