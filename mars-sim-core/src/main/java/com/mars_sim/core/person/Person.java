/*
 * Mars Simulation Project
 * Person.java
 * @date 2023-06-25
 * @author Scott Davis
 */

package com.mars_sim.core.person;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.mars_sim.core.Entity;
import com.mars_sim.core.LifeSupportInterface;
import com.mars_sim.core.Simulation;
import com.mars_sim.core.SimulationConfig;
import com.mars_sim.core.Unit;
import com.mars_sim.core.UnitEventType;
import com.mars_sim.core.UnitType;
import com.mars_sim.core.activities.GroupActivity;
import com.mars_sim.core.authority.Authority;
import com.mars_sim.core.data.SolMetricDataLogger;
import com.mars_sim.core.environment.MarsSurface;
import com.mars_sim.core.equipment.Container;
import com.mars_sim.core.equipment.EVASuit;
import com.mars_sim.core.equipment.Equipment;
import com.mars_sim.core.equipment.EquipmentInventory;
import com.mars_sim.core.equipment.EquipmentOwner;
import com.mars_sim.core.equipment.EquipmentType;
import com.mars_sim.core.location.LocationStateType;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.map.location.LocalPosition;
import com.mars_sim.core.person.ai.Mind;
import com.mars_sim.core.person.ai.NaturalAttributeManager;
import com.mars_sim.core.person.ai.NaturalAttributeType;
import com.mars_sim.core.person.ai.SkillManager;
import com.mars_sim.core.person.ai.fav.Favorite;
import com.mars_sim.core.person.ai.fav.FavoriteType;
import com.mars_sim.core.person.ai.fav.Preference;
import com.mars_sim.core.person.ai.job.util.AssignmentHistory;
import com.mars_sim.core.person.ai.job.util.AssignmentType;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.person.ai.job.util.JobUtil;
import com.mars_sim.core.person.ai.mission.Mission;
import com.mars_sim.core.person.ai.mission.MissionType;
import com.mars_sim.core.person.ai.role.Role;
import com.mars_sim.core.person.ai.role.RoleType;
import com.mars_sim.core.person.ai.shift.ShiftSlot;
import com.mars_sim.core.person.ai.shift.ShiftSlot.WorkStatus;
import com.mars_sim.core.person.ai.social.Appraiser;
import com.mars_sim.core.person.ai.social.Relation;
import com.mars_sim.core.person.ai.task.EVAOperation;
import com.mars_sim.core.person.ai.task.meta.WorkoutMeta;
import com.mars_sim.core.person.ai.task.util.TaskManager;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.person.ai.training.TrainingType;
import com.mars_sim.core.person.health.HealthProblem;
import com.mars_sim.core.person.health.MedicalEvent;
import com.mars_sim.core.resource.ItemResourceUtil;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.science.ResearchStudy;
import com.mars_sim.core.structure.GroupActivityType;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.function.ActivitySpot.AllocatedSpot;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.MarsTime;
import com.mars_sim.core.time.Temporal;
import com.mars_sim.core.tool.RandomUtil;
import com.mars_sim.core.vehicle.Crewable;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.core.vehicle.Vehicle;

/**
 * The Person class represents a person on Mars. It keeps track of everything
 * related to that person and provides information about him/her.
 */
public class Person extends Unit implements Worker, Temporal, Appraiser {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(Person.class.getName());
	
	/** The maximum number of sols for storing stats. */
	public static final int MAX_NUM_SOLS = 7;
	/** The standard hand carrying capacity for food in a person. */
	public static final int CARRYING_CAPACITY_FOOD = 1;
	
	/** A small amount. */
	private static final double SMALL_AMOUNT = 0.01;

	private static final String EARTHLING = "Earthling";

	private static double averageWeight;
	private static double averageHeight;

	static {
		var pc = SimulationConfig.instance().getPersonConfig();

		averageHeight = pc.getDefaultPhysicalChars().getAverageHeight();
		averageWeight = pc.getDefaultPhysicalChars().getAverageWeight();
	}

	// Transient data members
	/** The extrovert score of a person. */
	private transient int extrovertScore = -1;

	// Data members
	/** True if the person is buried. */
	private boolean isBuried;
	/** True if the person is declared dead. */
	private boolean declaredDead;

	/** The birth of a person */
	private LocalDate birthDate;
	/** The age of a person */
	private int age = -1;
	/** The current building location of the person. */
	private int currentBuildingInt;
	/** The carrying capacity of the person. */
	private int carryingCapacity;
	/** The id of the person who invite this person for a meeting. */
	private int initiatorId = -1;
	/** The id of the person being invited by this person for a meeting. */
	private int inviteeId = -1;
	
	/** The settlement the person is currently associated with. */
	private Integer associatedSettlementID = Integer.valueOf(-1);
	/** The buried settlement if the person has been deceased. */
	private Integer buriedSettlement = Integer.valueOf(-1);

	/** The eating speed of the person [kg/millisol]. */
	private double eatingSpeed = .5 + RandomUtil.getRandomDouble(-.05, .05);
	/** The height of the person (in cm). */
	private double height;
	/** The height of the person (in kg). */
	private double weight;

//	private long tLast;
	
	/** The person's country of origin. */
	private String country;
	/** The person's blood type. */
	private String bloodType;

	/** Settlement position (meters) from settlement center. */
	private LocalPosition position;
	/** The spot owned by this Person */
	private AllocatedSpot spot;
	/** The gender of the person (male or female). */
	private GenderType gender = GenderType.MALE;

	/** The person's skill manager. */
	private SkillManager skillManager;
	/** Manager for Person's natural attributes. */
	private NaturalAttributeManager attributes;
	/** Person's mind. */
	private Mind mind;
	/** Person's physical condition. */
	private PhysicalCondition condition;
	/** Person's circadian clock. */
	private CircadianClock circadian;
	/** Person's Favorite instance. */
	private Favorite favorite;
	/** Person's JobHistory instance. */
	private AssignmentHistory jobHistory;
	/** Person's Role instance. */
	private Role role;
	/** Person's Preference instance. */
	private Preference preference;
	/** Person's LifeSupportInterface instance. */
	private LifeSupportInterface support;
	/** Person's ReportingAuthority instance. */
	private Authority ra;
	/** The bed location of the person */
	private AllocatedSpot bed;

	/** The person's EquipmentInventory instance. */
	private EquipmentInventory eqmInventory;
	/** The person's research instance. */
	private ResearchStudy research;
	
	/** The person's mission experiences. */
	private Map<MissionType, Integer> missionExperiences;
	/** The person's list of prior trainings */
	private List<TrainingType> trainings;

	/** The person's EVA times. */
	private SolMetricDataLogger<String> eVATaskTime;
	
	private ShiftSlot shiftSlot;

	/**
	 * Constructor with the mandatory properties defined. All these are needed to construct the minimum person.
	 *
	 * @param name       the person's name
	 * @param settlement {@link Settlement} the settlement the person is at
	 * @param gender     the person's gender
	 * @param age		 Uhe person's age, can be optional of -1
	 * @param ethnicity Optional parameter of the ethnicity influences physical characteristics
	 * @param initialAttrs 
	 * @param personAttrs Persons attributes.
	 */
	public Person(String name, Settlement settlement, GenderType gender,
					int age, PopulationCharacteristics ethnicity,
					Map<NaturalAttributeType, Integer> initialAttrs) {
		super(name, settlement.getCoordinates());
		super.setDescription(EARTHLING);

		// Initialize data members
		this.position = LocalPosition.DEFAULT_POSITION;
		this.associatedSettlementID = settlement.getIdentifier();

		// Create a prior training profile
		generatePriorTraining();
		// Construct the PersonAttributeManager instance
		attributes = new NaturalAttributeManager(initialAttrs);

		// Construct the SkillManager instance
		skillManager = new SkillManager(this);
		// Construct the Mind instance
		mind = new Mind(this);

		// Set the person's status of death
		isBuried = false;

		// Add this person as a citizen
		settlement.addACitizen(this);

		// Calculate next birthday and scheduled a party in terms of future Mars sols
		var currentEarthTime = masterClock.getEarthTime();
		calculateBirthDate(currentEarthTime, age);


		// Create favorites
		favorite = new Favorite(this);
		// Create preferences
		preference = new Preference(this);
		// Set up genetic make-up. Notes it requires attributes.
		setupChromosomeMap(ethnicity);

		// Create circadian clock
		circadian = new CircadianClock(this);
		// Create physical condition
		condition = new PhysicalCondition(this);
		// Initialize field data in circadian clock
		circadian.initialize();
		// Create job history
		jobHistory = new AssignmentHistory();
		// Create the role
		role = new Role(this);
		// Create shift schedule
		shiftSlot = getAssociatedSettlement().getShiftManager().allocationShift(this);
		
		// Set up life support type
		support = getLifeSupportType();
		// Create the mission experiences map
		missionExperiences = new EnumMap<>(MissionType.class);
		// Create the EVA hours map
		eVATaskTime = new SolMetricDataLogger<>(MAX_NUM_SOLS);


		// Construct the EquipmentInventory instance. Start with the default
		eqmInventory = new EquipmentInventory(this, 100D);
		eqmInventory.setResourceCapacity(ResourceUtil.foodID, CARRYING_CAPACITY_FOOD);
		
		// Construct the ResearchStudy instance
		research = new ResearchStudy();
	}

	/**
	 * Uses static factory method to create an instance of PersonBuilder.
	 *
	 * @param name
	 * @param settlement
	 * @return
	 */
	public static PersonBuilder create(String name, Settlement settlement, GenderType gender) {
		return new PersonBuilder(name, settlement, gender);
	}

	/**
	 * Computes a person's chromosome map based on the characteristics of their nation.
	 * 
	 * @param nationPeople The population characteristics.
	 */
	private void setupChromosomeMap(PopulationCharacteristics nationPeople) {
		PersonConfig personConfig = SimulationConfig.instance().getPersonConfig();

		// Set up physical details based on Nation
		if (nationPeople == null) {
			nationPeople = personConfig.getDefaultPhysicalChars();
		}
		height = nationPeople.getRandomHeight(gender);
		setBaseMass(nationPeople.getRandomWeight(gender, height));
		// Biochemistry: id 0 - 19
		setupBloodType();

		// Set up carrying capacity and personality traits: id 40 - 59
		setupCarryingCapAttributeTrait(personConfig);
	}

	/**
	 * Computes a person's carrying capacity and attributes and its chromosome.
	 * 
	 * @param personConfig
	 */
	private void setupCarryingCapAttributeTrait(PersonConfig personConfig) {
		// Note: set up a set of genes that was passed onto this person
		// from two hypothetical parents

		int strength = attributes.getAttribute(NaturalAttributeType.STRENGTH);
		int endurance = attributes.getAttribute(NaturalAttributeType.ENDURANCE);
		double gym = 2D * getPreference().getPreferenceScore(new WorkoutMeta());
		if (getFavorite().getFavoriteActivity() == FavoriteType.FIELD_WORK)
			gym += RandomUtil.getRandomRegressionInteger(20);
		else if (getFavorite().getFavoriteActivity() == FavoriteType.SPORT)
			gym += RandomUtil.getRandomRegressionInteger(10);

		if (age < 0) {
			throw new IllegalStateException("Age is not defined");
		}
		int baseCap = (int)personConfig.getBaseCapacity();
		int load = 0;
		if (age > 4 && age < 8)
			load = age;
		else if (age > 7 && age <= 12)
			load = age * 2;
		else if (age > 11 && age <= 14)
			load = (baseCap/3 + age * 2);
		else if (age > 14 && age <= 18)
			load = (int)(baseCap/2.5 + age * 1.5);
		else if (age > 18 && age <= 25)
			load = (int)(baseCap/2.0 + 35 - age / 7.5);
		else if (age > 25 && age <= 35)
			load = (int)(baseCap + 30 - age / 12.5);
		else if (age > 35 && age <= 45)
			load = (baseCap + 25 - age / 10);
		else if (age > 45 && age <= 55)
			load = (int)(baseCap + 20 - age / 7.5);
		else if (age > 55 && age <= 65)
			load = (int)(baseCap/1.25 + 15 - age / 6.0);
		else if (age > 65 && age <= 70)
			load = (int)(baseCap/1.5 + 10 - age / 5.0);
		else if (age > 70 && age <= 75)
			load = (int)(baseCap/1.75 - age / 4.0);
		else if (age > 75 && age <= 80)
			load = (int)(baseCap/2.0 - age / 4.0);
		else
			load = (int)(baseCap/2.5 - age / 4.0);

		// Set inventory total mass capacity based on the person's weight and strength.
		// Must be able to carry an EVA suit
		carryingCapacity = Math.max((int)(EVASuit.getEmptyMass() * 2),
						(int)(gym + load + Math.max(20, weight/6.0) + (strength - 50)/1.5 + (endurance - 50)/2.0
				+ RandomUtil.getRandomRegressionInteger(10)));
	}

	private static final String getRandomBloodtype() {
		int rand = RandomUtil.getRandomInt(2);
		return switch(rand) {
			case 0 -> "A";
			case 1 -> "B";
			case 2 -> "O";
			default -> throw new IllegalStateException("Cannot get bloodtype of value " + rand);
		};
	}

	/**
	 * Computes a person's blood type and its chromosome.
	 */
	private void setupBloodType() {

		String dadBloodType = getRandomBloodtype();
		String momBloodType = getRandomBloodtype();
		String combined = dadBloodType + "-" + momBloodType;

		bloodType = switch(combined) {
			case "A-A" -> "A";
			case "A-B" -> "AB";
			case "A-O" -> "A";
			case "B-A" -> "AB";
			case "B-B" -> "B";
			case "B-O" -> "B";
			case "O-A" -> "A";
			case "O-B" -> "B";
			case "O-O" -> "O";
			default -> throw new IllegalStateException("Cannot get bloodtype from parents of " + combined);
		};
	}

	/**
	 * Gets the blood type.
	 *
	 * @return
	 */
	public String getBloodType() {
		return bloodType;
	}

	/*
	 * Sets sponsoring agency for the person.
	 */
	public void setSponsor(Authority sponsor) {
		ra = sponsor;
	}

	/*
	 * Gets sponsoring agency for the person.
	 */
	public Authority getReportingAuthority() {
		return ra;
	}

	/*
	 * Gets task preference for the person.
	 */
	public Preference getPreference() {
		return preference;
	}

	/**
	 * Sets the role for a person.
	 *
	 * @param type {@link RoleType}
	 */
	public void setRole(RoleType type) {
		getRole().changeRoleType(type);

		// In case of the role of the Mayor, his job must be set to Politician instead.
		if (type == RoleType.MAYOR) {
			// Set the job as Politician
			mind.assignJob(JobType.POLITICIAN, true, JobUtil.SETTLEMENT, AssignmentType.APPROVED, JobUtil.SETTLEMENT);
		}
	}

	/**
	 * Sets the job of a person.
	 *
	 * @param job JobType
	 * @param authority
	 */
	public void setJob(JobType job, String authority) {
		mind.assignJob(job, true, JobUtil.SETTLEMENT, AssignmentType.APPROVED, authority);
	}

	/**
	 * Gets the instance of Role for a person.
	 */
	public Role getRole() {
		return role;
	}

	/**
	 * Gets the instance of JobHistory for a person.
	 */
	public AssignmentHistory getJobHistory() {
		return jobHistory;
	}

	/**
	 * Gets the instance of Favorite for a person.
	 */
	public Favorite getFavorite() {
		return favorite;
	}

	/**
	 * Get details of the 
	 */
	public ShiftSlot getShiftSlot() {
		return shiftSlot;
	}

	/**
	 * Is this Person OnDuty. This does not include On Call.
	 */
	public boolean isOnDuty() {
		return shiftSlot.getStatus() == WorkStatus.ON_DUTY;
	}

	/**
	 * Creates a string representing the birth time of the person.
	 * @param clock
	 *
	 */
	private void calculateBirthDate(LocalDateTime earthLocalTime, int initialAge) {
		int daysPast = (initialAge * 365) + RandomUtil.getRandomInt(0,364);
		birthDate = earthLocalTime.minusDays(daysPast).toLocalDate();

		// Calculate the year
		// Set the age
		age = updateAge(earthLocalTime);

		// Calculate next birthday
		int day = birthDate.getDayOfMonth();
		int month = birthDate.getMonthValue();
		if (birthDate.isLeapYear() && (day == 29) &&(month == 2)) {
			// Leap year and 29th of February
			day = 28;
		}
		var nextBirthday = LocalDate.of(earthLocalTime.getYear() + 1, month, day);

		// Create a future activity so many earth days in the future
		var earthDays = ChronoUnit.DAYS.between(earthLocalTime.toLocalDate(), nextBirthday) % 365;
		GroupActivity.createPersonActivity("Birthday Party", GroupActivityType.BIRTHDAY,
									getAssociatedSettlement(), this, 
									(int)((earthDays * MarsTime.MILLISOLS_PER_EARTHDAY)/1000D),
									masterClock.getMarsTime());
	}

	/**
	 * Is the person outside of a settlement but within its vicinity ?
	 *
	 * @return true if the person is just right outside of a settlement
	 */
	public boolean isRightOutsideSettlement() {
		// Q: How to tell the different between a person doing EVA right outside a settlement 
		//    and a person doing EVA right outside a vehicle that are on a mission far away from the settlement ?
		// Ans: Use coordinates to see if it matches 
		
		return LocationStateType.SETTLEMENT_VICINITY == currentStateType || isBuried;
	}

	/**
	 * Gets the person's position at a settlement.
	 *
	 * @return distance (meters) from the settlement's center.
	 */
	@Override
	public LocalPosition getPosition() {
		return position;
	}

	/**
	 * Sets the person's settlement-wide position.
	 *
	 * @param position
	 */
	@Override
	public void setPosition(LocalPosition position) {
		this.position = position;
	}

	/**
	 * Gets the settlement in vicinity. This is used assume the person is not at a settlement.
	 *
	 * @return the person's settlement
	 */
	public Settlement getNearbySettlement() {
		return unitManager.findSettlement(getCoordinates());
	}
	
	/**
	 * Gets the settlement the person is at.
	 * Returns null if person is not at a settlement.
	 *
	 * @return the person's settlement
	 */
	@Override
	public Settlement getSettlement() {

		if (getContainerID() <= Unit.MARS_SURFACE_UNIT_ID)
			return null;

		Unit c = getContainerUnit();

		if (c.getUnitType() == UnitType.SETTLEMENT) {
			return (Settlement) c;
		}

		if (c.getUnitType() == UnitType.VEHICLE) {
			// Will see if vehicle is inside a garage or not
			return ((Vehicle)c).getSettlement();
		}

		if (c.getUnitType() == UnitType.BUILDING || c.getUnitType() == UnitType.PERSON
				|| c.getUnitType() == UnitType.ROBOT) {
			return c.getSettlement();
		}

		return null;
	}

	/**
	 * Buries the Person at the current location. This happens only if the person can
	 * be retrieved from any containing Settlements or Vehicles found. The body is
	 * fixed at the last location of the containing unit.
	 */
	public void buryBody() {
		// Bury the body
		isBuried = true;
		
		// Q: When should a person be removed from being a citizen of a settlement ?
		// A: after being buried ? 
		
		// Back up the last container unit
		condition.getDeathDetails().backupContainerUnit(getContainerUnit());
		// Set his/her buried settlement
		buriedSettlement = associatedSettlementID;
		// Throw unit event.
		fireUnitUpdate(UnitEventType.BURIAL_EVENT);
	}

	/**
	 * Declares the person dead.
	 */
	void setDeclaredDead() {
		// Set declaredDead
		declaredDead = true;
		// Set description
		setDescription("Dead");

		// Deregister the person's quarters
		deregisterBed();
		// Set work shift to OFF
		shiftSlot.getShift().leaveShift();

		// Relinquish his role
		var roleType = role.getType();
		role.relinquishOldRoleType();
		var chain = getAssociatedSettlement().getChainOfCommand();
		if (chain != null) {
			chain.reelectLeadership(roleType);
		}

		// Remove the person from the airlock's record
		getAssociatedSettlement().removeAirlockRecord(this);
		// Set the mind of the person to inactive
		mind.setInactive();

		research.terminateStudy();

		// Throw unit event
		fireUnitUpdate(UnitEventType.DEATH_EVENT);
	}

	/**
	 * Revives the person.
	 * 
	 * @param problem
	 */
	void setRevived(HealthProblem problem) {
		// Reset declaredDead
		declaredDead = false;
		// Set description
		setDescription("Recovering");
		// Reset isBuried
		isBuried = false;
		
		// Set buried settlement
		buriedSettlement = -1;
		// Throw unit event
		fireUnitUpdate(UnitEventType.REVIVED_EVENT);
		// Generate medical event
		MedicalEvent event = new MedicalEvent(this, problem, EventType.MEDICAL_RESUSCITATED);
		// Register event
		Simulation.instance().getEventManager().registerNewEvent(event);
	}
	
	/**
	 * Deregisters the person's quarters.
	 */
	void deregisterBed() {
		if (bed != null) {
			// Release the bed
			bed.leave(this, true);
			bed = null;
		}
	}

	/**
	 * Person can take action with time passing.
	 *
	 * @param pulse amount of time passing (in millisols).
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		if (!isValid(pulse)) {
			return false;
		}

		// DEBUG: Calculate the real time elapsed [in milliseconds]
//		long tnow = System.currentTimeMillis();
		
		// Check to see if the person has deceased
		if (condition.getDeathDetails() != null
				&& condition.getDeathDetails().getBodyRetrieved()) {
			setDeceased();
		}
		
		// Check to see if the person is dead
		if (condition.isDead()) {
			return false;
		}

		// Primary researcher; my responsibility to update Study
		research.timePassing(pulse);

		EVASuit suit = getSuit();
		// Record the use of it
		if (suit != null) {
			suit.recordUsageTime(pulse);
		}

		// Mental changes with time passing.
		mind.timePassing(pulse);

		// If Person is dead, then skip
		if (getLifeSupportType() != null) {
			// Get the life support type
			support = getLifeSupportType();
		}

		circadian.timePassing(pulse, support);
		// Pass the time in the physical condition first as this may result in death.
		condition.timePassing(pulse, support);

		if (pulse.isNewSol()) {
			// Update the solCache
			int currentSol = pulse.getMarsTime().getMissionSol();

			if (currentSol == 1) {
				// On the first mission sol,
				// adjust the sleep habit according to the current work shift
				for (int i=0; i< 15; i++) {
					int shiftEnd = shiftSlot.getShift().getEnd();
					int m = shiftEnd - 20 * (i+1);
					if (m < 0)
						m = m + 1000;
					// suppress sleep during the work shift
					circadian.updateSleepCycle(m, false);

					m = shiftEnd + 10 * (i+1);
					if (m > 1000)
						m = m - 1000;
					// encourage sleep after the work shift
					circadian.updateSleepCycle(m, true);
				}

				condition.increaseFatigue(RandomUtil.getRandomInt(333));
			}
			else {
				// Adjust the sleep habit according to the current work shift
				for (int i=0; i< 5; i++) {
					int m = shiftSlot.getShift().getEnd() + 10 * (i+1);
					if (m > 1000)
						m = m - 1000;
					circadian.updateSleepCycle(m, true);
				}

				// Check if a person's age should be updated
				age = updateAge(pulse.getMasterClock().getEarthTime());

				// Checks if a person has a role
				if (role.getType() == null)
					role.obtainNewRole();
			}
		}
		
//		// DEBUG: Calculate the real time elapsed [in milliseconds]
//		tLast = System.currentTimeMillis();
//		long elapsedMS = tLast - tnow;
//		if (elapsedMS > 100)
//			logger.severe(this, "elapsedMS: " + elapsedMS);
				
		return true;
	}

	/**
	 * Checks if the person has deceased.
	 * 
	 * @return
	 */
	public void setDeceased() {
		if (!isBuried && !declaredDead) {
			// Declares the person dead
			setDeclaredDead();
			
			// Q: When should a person be removed from being a citizen of a settlement ?
			// A: after being buried ? 
			
			// Deregisters the person's quarters
			deregisterBed();
			// Deactivates the person's mind
			mind.setInactive();
		}
	}

	/**
	 * Returns a reference to the Person's natural attribute manager.
	 *
	 * @return the person's natural attribute manager
	 */
	@Override
	public NaturalAttributeManager getNaturalAttributeManager() {
		return attributes;
	}

	/**
	 * Gets the performance factor that effect Person with health complaint.
	 *
	 * @return The value is between 0 -> 1.
	 */
	public double getPerformanceRating() {
		return condition.getPerformanceFactor();
	}

	/**
	 * Returns a reference to the Person's physical condition.
	 *
	 * @return the person's physical condition
	 */
	public PhysicalCondition getPhysicalCondition() {
		return condition;
	}

	/**
	 * Returns the person's mind.
	 *
	 * @return the person's mind
	 */
	public Mind getMind() {
		return mind;
	}

	@Override
	public TaskManager getTaskManager() {
		return mind.getTaskManager();
	}

	/**
	 * Updates and returns the person's age.
	 *
	 * @return the person's age
	 */
	private int updateAge(LocalDateTime localDateTime) {
		age = (int)ChronoUnit.YEARS.between(birthDate, localDateTime);
		return age;
	}

	/**
	 * Sets a person's age and update one's year of birth.
	 *
	 * @param newAge
	 */
	public void changeAge(int newAge) {
		// Back calculate a person's year
		int offset = age - newAge;
		// Set year to newYear
		birthDate = LocalDate.of(birthDate.getYear() + offset, birthDate.getMonth(), birthDate.getDayOfMonth());
		age = newAge;
	}

	/**
	 * Returns the person's birth date in the format of "2055-05-06".
	 *
	 * @return the person's birth date
	 */
	public LocalDate getBirthDate() {
		return birthDate;
	}

	/**
	 * Gets the LifeSupport system supporting this Person. This may be from the
	 * Settlement, Vehicle or Equipment.
	 *
	 * @return Life support system.
	 */
	private LifeSupportInterface getLifeSupportType() {

		Settlement settlement = getSettlement();
		if (settlement != null) {
			// if the person is inside
			return settlement;
		}

		Vehicle vehicle = getVehicle();
		if ((vehicle != null) && (vehicle instanceof LifeSupportInterface v)) {

			if (vehicle.isInVehicleInGarage()) {
				// Note: if the vehicle is inside a garage
				// continue to use settlement's life support
				return vehicle.getSettlement();
			}

			else {
				return v;
			}
		}

		EVASuit suit = getSuit();
		if (suit != null) {
			return suit;
		}

		// Note: in future a person may rely on a portable gas mask
		// for breathing

		return null;
	}

	/**
	 * Gets the gender of the person.
	 *
	 * @return the gender
	 */
	public GenderType getGender() {
		return gender;
	}

	/**
	 * Sets the gender of the person.
	 *
	 * @param gender the GenderType
	 */
	public void setGender(GenderType gender) {
		this.gender = gender;
	}

	/**
	 * Gets the person's local group of people (in building or rover).
	 *
	 * @return collection of people in person's location. The collectino incldues the Person
	 */
	public Collection<Person> getLocalGroup() {
		Collection<Person> localGroup = null;

		if (isInSettlement()) {
			Building building = BuildingManager.getBuilding(this);
			if (building != null) {
				if (building.hasFunction(FunctionType.LIFE_SUPPORT)) {
					localGroup = building.getLifeSupport().getOccupants();
				}
			}
		} else if (isInVehicle()) {
			localGroup = ((Crewable) getVehicle()).getCrew();
		}

		if (localGroup == null) {
			localGroup = Collections.emptyList();
		}
		return localGroup;
	}

	/**
	 * Checks if a person is super unfit.
	 *
	 * @return true if a person is super fit
	 */
	public boolean isSuperUnfit() {
		return condition.isSuperUnfit();
    }

	/**
	 * Checks if a person is EVA fit.
	 *
	 * @return true if a person is EVA fit
	 */
	public boolean isEVAFit() {
        return condition.isEVAFit();
    }
	
	/**
	 * Checks if a person is nominally fit.
	 *
	 * @return true if a person is nominally fit
	 */
	public boolean isNominallyFit() {
        return !condition.isNominallyUnfit();
    }

	/**
	 * Gets the settlement the person is currently associated with.
	 *
	 * @return associated settlement or null if none.
	 */
	@Override
	public Settlement getAssociatedSettlement() {
		return unitManager.getSettlementByID(associatedSettlementID);
	}

	/**
	 * Sets the associated settlement for a person.
	 *
	 * @param newSettlement the new associated settlement or null if none.
	 */
	public void setAssociatedSettlement(int newSettlement) {

		if (associatedSettlementID != newSettlement) {

			int oldSettlement = associatedSettlementID;
			associatedSettlementID = newSettlement;

			if (oldSettlement != -1) {
				unitManager.getSettlementByID(oldSettlement).removeACitizen(this);
			}

			if (newSettlement != -1) {
				unitManager.getSettlementByID(newSettlement).addACitizen(this);
			}
		}
	}

	public Settlement getBuriedSettlement() {
		return unitManager.getSettlementByID(buriedSettlement);
	}



	/**
	 * Computes the building the person is currently located at.
	 * Returns null if outside of a settlement.
	 *
	 * @return building
	 */
	@Override
	public Building getBuildingLocation() {
		if (currentBuildingInt == -1)
			return null;
		return unitManager.getBuildingByID(currentBuildingInt);
	}

	/**
	 * Computes the building the person is currently located at.
	 * Returns null if outside of a settlement.
	 *
	 * @return building
	 */
	public void setCurrentBuilding(Building building) {
		if (building == null) {
			currentBuildingInt = -1;
		}
		else {
			currentBuildingInt = building.getIdentifier();
		}
	}

	@Override
	public String getTaskDescription() {
		return getMind().getTaskManager().getTaskDescription(false);
	}

	/**
	 * Does the current task require no physical effort ?
	 * 
	 * @return
	 */
	public boolean isRestingTask() {
		if (getMind().getTaskManager().getTask() != null)
			return !getMind().getTaskManager().getTask().isEffortDriven();
		return true;
	}
    		
	public String getTaskPhase() {
		if (getMind().getTaskManager().getPhase() != null)
			return getMind().getTaskManager().getPhase().getName();
		return "";
	}

	@Override
	public Mission getMission() {
		return getMind().getMission();
	}

	@Override
	public void setMission(Mission newMission) {
		getMind().setMission(newMission);
	}

	public int[] getPreferredSleepHours() {
		return circadian.getPreferredSleepHours();
	}

	/**
	 * Returns the weight/desire for sleep at a msol.
	 * 
	 * @param index
	 * @return
	 */
	public int getSleepWeight(int msol) {
		return circadian.getSleepWeight(msol);
	}
	
	public void updateSleepCycle(int millisols, boolean updateType) {
		circadian.updateSleepCycle(millisols, updateType);
	}

	/**
	 * Gets the settlement location of this bed.
	 * 
	 * @return
	 */
	public AllocatedSpot getBed() {
		return bed;
	}

	/**
	 * Assign a bed to this person.
	 * 
	 * @param bed2 The assignment
	 */
	public void setBed(AllocatedSpot bed2) {
		this.bed = bed2;	
	}

	/**
	 * Does this person have an assigned bed ?
	 * 
	 * @return
	 */
	public boolean hasBed() {
		return bed != null;
	}
	
    public void setCountry(String name) {
		this.country = name;
    }

	public String getCountry() {
		return country;
	}

	public boolean isDeclaredDead() {
		return declaredDead;
	}

	public boolean isBuried() {
		return isBuried;
	}

	/**
	 * Gets vehicle person is in, null if person is not in vehicle.
	 *
	 * @return the person's vehicle
	 */
	@Override
	public Vehicle getVehicle() {
		if (getLocationStateType() == LocationStateType.INSIDE_VEHICLE) {
			return (Vehicle) getContainerUnit();
		}

		return null;
	}

	public CircadianClock getCircadianClock() {
		return circadian;
	}

	/**
	 * Adds the mission experience score.
	 *
	 * @param missionType
	 * @param score
	 */
	public void addMissionExperience(MissionType missionType, int score) {
		Integer total = missionExperiences.getOrDefault(missionType, 0);
		total += score;
		missionExperiences.put(missionType, total);
	}

	/**
	 * Gets the mission experiences map.
	 *
	 * @return a map of mission experiences
	 */
	public int getMissionExperience(MissionType missionType) {
		Integer previous = missionExperiences.get(missionType);
		if (previous != null) {
			return previous;
		}
		return 0;
	}

	/**
	 * Adds the EVA time.
	 *
	 * @param taskName
	 * @param time
	 */
	public void addEVATime(String taskName, double time) {
		eVATaskTime.increaseDataPoint(taskName, time);
	}

	/**
	 * Gets the map of EVA task time.
	 *
	 * @return a map of EVA time by sol
	 */
	public Map<Integer, Double> getTotalEVATaskTimeBySol() {
		Map<Integer, Double> map = new ConcurrentHashMap<>();
		Map<Integer, Map<String, Double>> history = eVATaskTime.getHistory();
		for (Entry<Integer, Map<String, Double>> day : history.entrySet()) {
			double sum = 0;
			int sol = day.getKey();
			for (Double t : day.getValue().values()) {
				sum += t;
			}

			map.put(sol, sum);
		}

		return map;
	}

	public double getEatingSpeed() {
		return eatingSpeed;
	}

	/**
	 * Returns the person's height in cm
	 *
	 * @return the person's height
	 */
	public double getHeight() {
		return height;
	}

	/**
	 * Gets the age of this person.
	 *
	 * @return
	 */
	public int getAge() {
		return age;
	}

	/**
	 * Returns a reference to the Person's skill manager
	 *
	 * @return the person's skill manager
	 */
	@Override
	public SkillManager getSkillManager() {
		return skillManager;
	}

	/**
	 * Randomly generates a list of training the person may have attended.
	 */
	private void generatePriorTraining() {
		if (trainings == null) {
			trainings = new ArrayList<>();
			List<TrainingType> lists = new ArrayList<>(Arrays.asList(TrainingType.values()));
			int num = RandomUtil.getRandomRegressionInteger(4);
			// Guarantee at least one training
			if (num == 0) num = 1;
			for (int i= 0 ; i < num; i++) {
				int size = lists.size();
				int rand = RandomUtil.getRandomInt(size-1);
				TrainingType t = lists.get(rand);
				trainings.add(t);
				lists.remove(t);
			}
		}
	}

	/**
	 * Gets a list of prior training.
	 *
	 * @return {@link List<TrainingType>}
	 */
	public List<TrainingType> getTrainings() {
		return trainings;
	}

	/**
	 * Gets the EVA suit the person has donned on.
	 *
	 * @return
	 */
	public EVASuit getSuit() {
		return getInventorySuit();
	}

	/**
	 * Gets the EVA suit instance the person has in inventory.
	 *
	 * @return
	 */
	public EVASuit getInventorySuit() {
		return (EVASuit) getSuitSet().stream().findAny().orElse(null);
	}

	public int getExtrovertmodifier() {
		if (extrovertScore == -1) {
			int score = mind.getTraitManager().getIntrovertExtrovertScore();
			extrovertScore = score;

			// if introvert, score  0 to  50 --> -2 to 0
			// if extrovert, score 50 to 100 -->  0 to 2
		}

		return (int)((extrovertScore - 50) / 25D);
	}

	/**
	 * Calculates the modifier for walking speed based on how much the person is carrying.
	 */
	public double getWalkSpeedMod() {
		// Get the modified stored mass and base mass 
		double mass = getMass();
		// The modifier is a ratio of the mass the person carry and the carrying capacity 
		// Make sure it doesn't go to zero or -ve as there is always some movement
		return Math.max(carryingCapacity/mass/1.2, SMALL_AMOUNT);
	}

	/**
	 * Gets the remaining carrying capacity available.
	 *
	 * @return capacity (kg).
	 */
	public double getRemainingCarryingCapacity() {
		double result = carryingCapacity - eqmInventory.getStoredMass();
		if (result < 0)
			return 0;
		return result;
	}

	/**
	 * Gets the capacity a person can carry.
	 *
	 * @return capacity (kg)
	 */
	public double getCarryingCapacity() {
		return carryingCapacity;
	}

	/**
	 * Returns the mass as the base mass plus whatever being stored in him.
	 */
	@Override
	public double getMass() {
		// TODO because the Person is not fully initialised in the constructor this
		// can be null. The initialise method is the culprit.
		return (eqmInventory != null ? eqmInventory.getModifiedMass(EquipmentType.WHEELBARROW, 20) : 0) + getBaseMass();
	}
	
	/**
	 * Gets the equipment list.
	 *
	 * @return the equipment list
	 */
	@Override
	public Set<Equipment> getEquipmentSet() {
		return eqmInventory.getEquipmentSet();
	}

	/**
	 * Gets the container set.
	 *
	 * @return
	 */
	@Override
	public Set<Equipment> getContainerSet() {
		return eqmInventory.getContainerSet();
	}
	
	/**
	 * Gets the EVA suit set.
	 * 
	 * @return
	 */
	@Override
	public Set<Equipment> getSuitSet() {
		return eqmInventory.getSuitSet();
	}
	
	public EquipmentInventory getEquipmentInventory() {
		return eqmInventory;
	}

	/**
	 * Finds all of the containers of a particular type (excluding EVA suit).
	 *
	 * @return collection of containers or empty collection if none.
	 */
	@Override
	public Collection<Container> findContainersOfType(EquipmentType type){
		return eqmInventory.findContainersOfType(type);
	}

	/**
	 * Does this person possess an equipment of this equipment type ?
	 *
	 * @param typeID
	 * @return true if this person possess this equipment type
	 */
	@Override
	public boolean containsEquipment(EquipmentType type) {
		return eqmInventory.containsEquipment(type);
	}

	/**
	 * Adds an equipment to this person.
	 *
	 * @param equipment
	 * @return true if this person can carry it
	 */
	@Override
	public boolean addEquipment(Equipment e) {
		if (eqmInventory.addEquipment(e)) {
			fireUnitUpdate(UnitEventType.ADD_ASSOCIATED_EQUIPMENT_EVENT, this);
			return true;
		}
		return false;
	}

	/**
	 * Removes an equipment.
	 *
	 * @param equipment
	 */
	@Override
	public boolean removeEquipment(Equipment equipment) {
		return eqmInventory.removeEquipment(equipment);
	}
	
	/**
	 * Stores the item resource.
	 *
	 * @param resource the item resource
	 * @param quantity
	 * @return excess quantity that cannot be stored
	 */
	@Override
	public int storeItemResource(int resource, int quantity) {
		return eqmInventory.storeItemResource(resource, quantity);
	}

	/**
	 * Retrieves the item resource.
	 *
	 * @param resource
	 * @param quantity
	 * @return quantity that cannot be retrieved
	 */
	@Override
	public int retrieveItemResource(int resource, int quantity) {
		return eqmInventory.retrieveItemResource(resource, quantity);
	}

	/**
	 * Gets the item resource stored.
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public int getItemResourceStored(int resource) {
		return eqmInventory.getItemResourceStored(resource);
	}

	/**
	 * Stores the amount resource.
	 *
	 * @param resource the amount resource
	 * @param quantity
	 * @return excess quantity that cannot be stored
	 */
	@Override
	public double storeAmountResource(int resource, double quantity) {
		return eqmInventory.storeAmountResource(resource, quantity);
	}

	/**
	 * Retrieves the resource.
	 *
	 * @param resource
	 * @param quantity
	 * @return quantity that cannot be retrieved
	 */
	@Override
	public double retrieveAmountResource(int resource, double quantity) {
		return eqmInventory.retrieveAmountResource(resource, quantity);
	}

	/**
	 * Gets the capacity of a particular amount resource.
	 *
	 * @param resource
	 * @return capacity
	 */
	@Override
	public double getAmountResourceCapacity(int resource) {
		return eqmInventory.getAmountResourceCapacity(resource);
	}

	/**
	 * Obtains the remaining storage space of a particular amount resource.
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public double getAmountResourceRemainingCapacity(int resource) {
		return eqmInventory.getAmountResourceRemainingCapacity(resource);
	}

	/**
	 * Does it have unused space or capacity for a particular resource ?
	 * 
	 * @param resource
	 * @return
	 */
	@Override
	public boolean hasAmountResourceRemainingCapacity(int resource) {
		return eqmInventory.hasAmountResourceRemainingCapacity(resource);
	}
	
	/**
     * Gets the total capacity that this person can hold.
     *
     * @return total capacity (kg).
     */
	@Override
	public double getCargoCapacity() {
		return eqmInventory.getCargoCapacity();
	}

	/**
	 * Gets the amount resource stored.
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public double getAmountResourceStored(int resource) {
		return eqmInventory.getAmountResourceStored(resource);
	}

	/**
	 * Gets all the amount resource resource stored, including inside equipment.
	 *
	 * @param resource
	 * @return quantity
	 */
	@Override
	public double getAllAmountResourceStored(int resource) {
		return eqmInventory.getAllAmountResourceStored(resource);
	}
	
	/**
	 * Gets all stored amount resources in eqmInventory.
	 *
	 * @return all stored amount resources.
	 */
	@Override
	public Set<Integer> getAmountResourceIDs() {
		return eqmInventory.getAmountResourceIDs();
	}
	
	/**
	 * Gets all stored amount resources in eqmInventory, including inside equipment
	 *
	 * @return all stored amount resources.
	 */
	@Override
	public Set<Integer> getAllAmountResourceIDs() {
		return eqmInventory.getAllAmountResourceIDs();
	}

	/**
	 * Gets all stored item resources.
	 *
	 * @return all stored item resources.
	 */
	@Override
	public Set<Integer> getItemResourceIDs() {
		return eqmInventory.getItemResourceIDs();
	}


	/**
	 * Finds the number of empty containers of a class that are contained in storage and have
	 * an empty inventory. 
	 * 
	 * Note: NOT for EVA suits.
	 *
	 * @param containerClass  the unit class.
	 * @param brandNew  does it include brand new bag only
	 * @return number of empty containers.
	 */
	@Override
	public int findNumEmptyContainersOfType(EquipmentType containerType, boolean brandNew) {
		return eqmInventory.findNumEmptyContainersOfType(containerType, brandNew);
	}

	/**
	 * Finds the number of containers of a particular type.
	 *
	 * Note: will not count EVA suits.
	 * 
	 * @param containerType the equipment type.
	 * @return number of empty containers.
	 */
	@Override
	public int findNumContainersOfType(EquipmentType containerType) {
		return eqmInventory.findNumContainersOfType(containerType);
	}

	/**
	 * Finds a container in storage.
	 *
	 * Note: will not count EVA suits.
	 * 
	 * @param containerType
	 * @param empty does it need to be empty ?
	 * @param resource If -1 then resource doesn't matter
	 * @return instance of container or null if none.
	 */
	@Override
	public Container findContainer(EquipmentType containerType, boolean empty, int resource) {
		return eqmInventory.findContainer(containerType, empty, resource);
	}

	/**
	 * Is this unit empty ?
	 *
	 * @return true if this unit doesn't carry any resources or equipment
	 */
	public boolean isEmpty() {
		return eqmInventory.isEmpty();
	}


	/**
	 * Gets the stored mass.
	 */
	@Override
	public double getStoredMass() {
		return eqmInventory.getStoredMass();
	}

	/**
	 * Obtains the remaining general storage space.
	 *
	 * @return quantity
	 */
	@Override
	public double getRemainingCargoCapacity() {
		return eqmInventory.getRemainingCargoCapacity();
	}

	/**
	 * Does it have this item resource ?
	 *
	 * @param resource
	 * @return
	 */
	@Override
	public boolean hasItemResource(int resource) {
		return eqmInventory.hasItemResource(resource);
	}

	/**
	 * Sets the unit's container unit.
	 *
	 * @param newContainer the unit to contain this unit.
	 */
	private boolean setContainerUnit(Unit newContainer) {
		if (newContainer != null) {
			// Gets the old container unit
			Unit oldCU = getContainerUnit();
			
			if (newContainer.equals(oldCU)) {
				return true;
			}
			
			// 1. Set Coordinates
			if (newContainer.getUnitType() == UnitType.MARS) {
				// Since it's on the surface of Mars,
				// First set its initial location to its old parent's location as it's leaving its parent.
				// Later it may move around and updates its coordinates by itself
				setCoordinates(oldCU.getCoordinates());
			}
			else {
				setCoordinates(newContainer.getCoordinates());
			}
			
			// 2. Set new LocationStateType
			if (oldCU != null) {
				// 2a. If the previous cu is a settlement
				//     and this person's new cu is mars surface,
				//     then location state is within settlement vicinity
				if (oldCU.getUnitType() == UnitType.SETTLEMENT
					&& newContainer.getUnitType() == UnitType.MARS) {
						currentStateType = LocationStateType.SETTLEMENT_VICINITY;
				}	
				// 2b. If the previous cu is a vehicle
				//     and the previous cu is in settlement vicinity
				//     then the new location state is settlement vicinity
				else if (oldCU.getUnitType() == UnitType.VEHICLE
						&& oldCU.isInSettlementVicinity()
						&& newContainer.getUnitType() == UnitType.MARS) {
							currentStateType = LocationStateType.SETTLEMENT_VICINITY;
				}
				// 2c. If the previous cu is a vehicle
				//     and the previous cu vehicle is outside on mars surface
				//     then the new location state is vehicle vicinity
				else if ((oldCU.getUnitType() == UnitType.VEHICLE)
						&& oldCU.isOutside()
						&& newContainer.getUnitType() == UnitType.MARS) {
							currentStateType = LocationStateType.VEHICLE_VICINITY;
				}
				else {
					updatePersonState(newContainer);
				}
			}
			else {
				updatePersonState(newContainer);
			}
			
			// 3. Set containerID
			// Note: need to decide what to set for a deceased person
			setContainerID(newContainer.getIdentifier());
			
			// 4. Fire the container unit event
			fireUnitUpdate(UnitEventType.CONTAINER_UNIT_EVENT, newContainer);
		}
		return true;
	}

	/**
	 * Updates the location state type of a person.
	 *
	 * @param newContainer
	 */
	public void updatePersonState(Unit newContainer) {
		if (newContainer == null) {
			currentStateType = LocationStateType.UNKNOWN;
			return;
		}

		currentStateType = getNewLocationState(newContainer);
	}

	/**
	 * Gets the location state type based on the type of the new container unit.
	 *
	 * @param newContainer
	 * @return {@link LocationStateType}
	 */
	private LocationStateType getNewLocationState(Unit newContainer) {

		if (newContainer.getUnitType() == UnitType.SETTLEMENT)
			return LocationStateType.INSIDE_SETTLEMENT;

		if (newContainer.getUnitType() == UnitType.BUILDING)
			return LocationStateType.INSIDE_SETTLEMENT;

		if (newContainer.getUnitType() == UnitType.VEHICLE)
			return LocationStateType.INSIDE_VEHICLE;

		if (newContainer.getUnitType() == UnitType.CONSTRUCTION)
			return LocationStateType.MARS_SURFACE;

		if (newContainer.getUnitType() == UnitType.PERSON)
			return LocationStateType.ON_PERSON_OR_ROBOT;

		if (newContainer.getUnitType() == UnitType.MARS)
			return LocationStateType.MARS_SURFACE;

		return null;
	}

	/**
	 * Is this unit inside a settlement ?
	 *
	 * @return true if the unit is inside a settlement
	 */
	@Override
	public boolean isInSettlement() {

		if (containerID <= MARS_SURFACE_UNIT_ID)
			return false;

		if (LocationStateType.INSIDE_SETTLEMENT == currentStateType)
			return true;

		return false;
	}

	/**
	 * Transfer the unit from one owner to another owner.
	 *
	 * @param origin {@link Unit} the original container unit
	 * @param destination {@link Unit} the destination container unit
	 */
	public boolean transfer(Unit destination) {
		boolean transferred = false;
		Unit cu = getContainerUnit();
		if (cu == null) {
			// Fire the unit event type
			destination.fireUnitUpdate(UnitEventType.INVENTORY_STORING_UNIT_EVENT, this);
			// Set the new container unit (which will internally set the container unit id)
			return setContainerUnit(destination);
		}
		
		UnitType ut = cu.getUnitType();

		if (destination.equals(cu)) {
			return true;
		}

		// Check if the origin is a vehicle
		if (ut == UnitType.VEHICLE) {
			if (cu instanceof Crewable c) { //!(VehicleType.isDrone(((Vehicle)cu).getVehicleType()))) { 
				transferred = c.removePerson(this);
			}
			else {
				logger.warning(this, 20_000, "Not possible to be retrieved from " + cu + ".");
			}
		}
		else if (ut == UnitType.MARS) {
			transferred = ((MarsSurface)cu).removePerson(this);
		}
		else if (ut == UnitType.BUILDING 
				|| ut == UnitType.SETTLEMENT) {
			// Q1: should one remove this person from settlement's peopleWithin list,
			//     especially if he is still inside a vehicle in the garage of a settlement ?
			// Q2: should it be the vehicle's responsibility to remove the person from the settlement
			//     as the vehicle leaves the garage ?
			transferred = ((Settlement)cu).removePeopleWithin(this);
			BuildingManager.removePersonFromBuilding(this, getBuildingLocation());
		}

		if (!transferred) {
			logger.severe(this, 20_000, "Cannot be retrieved from " + cu + ".");
			// NOTE: need to revert back to the previous container unit cu
		}
		
		else {
			// Check if the destination is a vehicle
			if (destination.getUnitType() == UnitType.VEHICLE) {
				if (destination instanceof Crewable c) {
					transferred = c.addPerson(this);
				}
				else {
					logger.warning(this, 20_000, "Not possible to be stored into " + cu + ".");
				}
			}
			else if (destination.getUnitType() == UnitType.MARS) {
				transferred = ((MarsSurface)destination).addPerson(this);
			}
			
			else if (destination.getUnitType() == UnitType.SETTLEMENT) {
				transferred = ((Settlement)destination).addToIndoor(this);
				// WARNING: Transferring a person/robot/equipment from a vehicle into a settlement 
				// can be problematic if no building is assigned.
				// If exiting a vehicle in a garage, it's recommended using garageBuilding as a destination
			}
			else if (destination.getUnitType() == UnitType.BUILDING) {
				transferred = ((Building)destination).getSettlement().addToIndoor(this);
				// Turn a building destination to a settlement to avoid 
				// casting issue with making containerUnit a building instance
				BuildingManager.setToBuilding(this, (Building)destination);
				destination = (((Building)destination)).getSettlement();
			}

			if (!transferred) {
				logger.warning(this, 20_000, "Cannot be stored into " + destination + ".");
				// NOTE: need to revert back the storage action
			}
			else {
				// Set the new container unit (which will internally set the container unit id)
				setContainerUnit(destination);
				// Fire the unit event type
				destination.fireUnitUpdate(UnitEventType.INVENTORY_STORING_UNIT_EVENT, this);
				// Fire the unit event type for old container
				cu.fireUnitUpdate(UnitEventType.INVENTORY_RETRIEVING_UNIT_EVENT, this);
			}
		}

		return transferred;
	}

	@Override
	public UnitType getUnitType() {
		return UnitType.PERSON;
	}

	/**
	 * Gets the holder's unit instance.
	 *
	 * @return the holder's unit instance
	 */
	@Override
	public Unit getHolder() {
		return this;
	}

	/**
	 * Does this person have a set of clothing ?
	 */
	public boolean hasGarment() {
		return getItemResourceStored(ItemResourceUtil.garmentID) > 0;
	}

	/**
	 * Does this person have a pressure suit ?
	 */
	public boolean hasPressureSuit() {
		return getItemResourceStored(ItemResourceUtil.pressureSuitID) > 0;
	}

	/**
	 * Does this person have a thermal bottle ?
	 * 
	 * @return
	 */
	public boolean hasThermalBottle() {
		return findNumContainersOfType(EquipmentType.THERMAL_BOTTLE) > 0;
	}
	
	/**
	 * Fills up a thermal bottle with water.
	 * 
	 * @param amount
	 */
	public void fillUpThermalBottle(double amount) {
		Container bottle = lookForThermalBottle();
		bottle.storeAmountResource(ResourceUtil.waterID, amount);
	}
	
	/**
	 * Looks for one's thermal bottle.
	 * 
	 * @return
	 */
	public Container lookForThermalBottle() {
		Container c = eqmInventory.findOwnedContainer(EquipmentType.THERMAL_BOTTLE, getIdentifier(), ResourceUtil.waterID);
		if (c == null)
			return findContainer(EquipmentType.THERMAL_BOTTLE, false, ResourceUtil.waterID);
		else
			return c;
	}
	
	/**
	 * Assigns a thermal bottle as a standard living necessity.
	 */
	public void assignThermalBottle() {

		if (!hasThermalBottle() && isInside()) {
			Equipment aBottle = null;
			for (Equipment e : ((EquipmentOwner)getContainerUnit()).getContainerSet()) {
				if (e.getEquipmentType() == EquipmentType.THERMAL_BOTTLE) {
					Person originalOwner = e.getRegisteredOwner();
					if (originalOwner != null && originalOwner.equals(this)) {
						// Remove it from the container unit
						e.transfer(this);
						// Register the person as the owner of this bottle
						e.setRegisteredOwner(this);
						
						return;
					}
					
					// Tag this bottle first
					if (aBottle == null) {
						aBottle = e;
					}
				}
			}
			
			// After done with iterating over all the bottle,
			// if it still can't find a bottle that was last assigned to this person
			// get the first saved one 
			if (aBottle != null) {
				// Remove it from the container unit
				aBottle.transfer(this);
				// Register the person as the owner of this bottle
				aBottle.setRegisteredOwner(this);
			}
		}
	}
	
	/**
	 * Drops off the thermal bottle such as when going out for an EVA.
	 */
	public void dropOffThermalBottle() {

		if (isInside()) {
			for(Equipment e : getContainerSet()) {
				if (e.getEquipmentType() == EquipmentType.THERMAL_BOTTLE) {
					// Transfer to this person's container unit 
					e.transfer(getContainerUnit());
					
					break;
				}
			}
		}
	}
	
	/**
	 * Puts on a garment.
	 *
	 * @param holder the current equipment holder of the clothing
	 */
	public void wearGarment(EquipmentOwner holder) {
		if (!hasGarment() && holder.retrieveItemResource(ItemResourceUtil.garmentID, 1) == 0) {
			storeItemResource(ItemResourceUtil.garmentID, 1);
		}
	}

	/**
	 * Puts on a pressure suit set.
	 *
	 * @param holder the current equipment holder of the clothing
	 */
	public void wearPressureSuit(EquipmentOwner holder) {
		if (!hasPressureSuit() && holder.retrieveItemResource(ItemResourceUtil.pressureSuitID, 1) == 0) {
			storeItemResource(ItemResourceUtil.pressureSuitID, 1);
		}
	}


	/**
	 * Puts off the garment.
	 *
	 * @param holder the new holder of the clothing
	 * @return true if successful
	 */
	public boolean unwearGarment(EquipmentOwner holder) {
		if (hasGarment() && retrieveItemResource(ItemResourceUtil.garmentID, 1) == 0) {
			if (holder.storeItemResource(ItemResourceUtil.garmentID, 1) == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Puts off the pressure suit set.
	 *
	 * @param suit
	 * @return true if successful
	 */
	public boolean unwearPressureSuit(EquipmentOwner holder) {
		if (hasPressureSuit() && retrieveItemResource(ItemResourceUtil.pressureSuitID, 1) == 0) {
			if (holder.storeItemResource(ItemResourceUtil.pressureSuitID, 1) == 0) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Puts off the garment and the thermal bottle. (UNUSED)
	 * 
	 * DO NOT DELETE THIS YET
	 * 
	 * @param person
	 * @param entity
	 */
	public void putOff(Person person, Entity entity) {
		EquipmentOwner housing = null;

		boolean inS = person.isInSettlement();
		
		if (inS)
			housing = ((Building)entity).getSettlement();
		else
			housing = (Vehicle)entity;
		
		// Remove pressure suit and put on garment
		if (person.unwearPressureSuit(housing)) {
			person.wearGarment(housing);
		}

		// Assign thermal bottle
		person.assignThermalBottle();
	}
	
	/**
	 * Rescues the person from the rover in settlement vicinity.
	 * 
	 * Note: this is more like a hack, rather than a legitimate 
	 * way of transferring a person through the airlock into the settlement.			
	 *
	 * @param r the rover
	 * @param p the person
	 * @param s the settlement
	 */
	public boolean rescueOperation(Rover r, Settlement s) {
		boolean result = false;
		
		if (isDeclaredDead()) {
			// WARNING: Transferring a person/robot/equipment from a vehicle into a settlement 
			// can be problematic if no building is assigned.
			// If exiting a vehicle in a garage, it's recommended using garageBuilding as a destination
			result = transfer(s);
		}
		else if (r != null || isOutside()) {
			result = transfer(s);
		}

		// Add the person to a medical facility	
		EVAOperation.send2Medical(this, s);
		
		return result;
	}
	
	/**
	 * Gets the relation instance.
	 * 
	 * @return
	 */
	public Relation getRelation( ) {
		return mind.getRelation();
	}
	
	public void setMeetingInitiator(int initiatorId) {
		this.initiatorId = initiatorId;
	}
	
	public void setMeetingInvitee(int inviteeId) {
		this.inviteeId = inviteeId;
	}
	
	public int getMeetingInitiator() {
		return initiatorId;
	}
	
	public int getMeetingInvitee() {
		return inviteeId;
	}
	
	/**
	 * Gets the allocated activity spot.
	 */
	@Override
	public AllocatedSpot getActivitySpot() {
		return spot;
	}
	
	/**
	 * Sets the activity spot allocated.
	 * 
	 * @param newSpot Can be null if no spot assigned
	 */
	@Override
	public void setActivitySpot(AllocatedSpot newSpot) {
		// Check the spot is really changing and not just a reallocation
		if ((spot != null) && !spot.equals(newSpot)) {
			spot.leave(this, false);
		}
		spot = newSpot;
	}
	
	/**
	 * Gets the research study instance.
	 * 
	 * @return
	 */
	public ResearchStudy getResearchStudy() {
		return research;
	}
	
	/**
	 * Reinitialize references after loading from a saved sim.
	 */
	public void reinit() {
		mind.reinit();
		condition.reinit();
	}

	/**
	 * Compares if an object is the same as this person.
	 *
	 * @param obj
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		Person p = (Person) obj;
		return this.getIdentifier() == p.getIdentifier();
	}

	/**
	 * Gets the hash code for this object.
	 *
	 * @return hash code.
	 */
	@Override
	public int hashCode() {
		// Hash must be constant and not depend upon changing attributes
		return getIdentifier() % 64;
	}

	@Override
	public void destroy() {
		super.destroy();
		circadian = null;
		condition = null;
		favorite = null;
		jobHistory = null;
		role = null;
		preference = null;
		support = null;
		ra = null;
		bed = null;
		attributes.destroy();
		attributes = null;
		mind.destroy();
		mind = null;

		skillManager.destroy();
		skillManager = null;
	}

	public static double getAverageWeight() {
		return averageWeight;
	}

    public static double getAverageHeight() {
        return averageHeight;
    }

}
