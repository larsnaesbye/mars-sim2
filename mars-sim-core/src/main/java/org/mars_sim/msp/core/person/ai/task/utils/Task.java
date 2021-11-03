/**
 * Mars Simulation Project
 * Task.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.utils;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.LocalBoundedObject;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.environment.OrbitInfo;
import org.mars_sim.msp.core.environment.SurfaceFeatures;
import org.mars_sim.msp.core.environment.TerrainElevation;
import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.events.HistoricalEventManager;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.EventType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.JobType;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionManager;
import org.mars_sim.msp.core.person.ai.social.RelationshipManager;
import org.mars_sim.msp.core.person.ai.task.Walk;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.RobotType;
import org.mars_sim.msp.core.science.ScientificStudyManager;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.Function;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.LifeSupport;
import org.mars_sim.msp.core.structure.building.function.farming.CropConfig;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * The Task class is an abstract parent class for tasks that allow people to do
 * various things. A person's TaskManager keeps track of one current task for
 * the person, but a task may use other tasks internally to accomplish things.
 */
public abstract class Task implements Serializable, Comparable<Task> {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(Task.class.getName());

	private static final double JOB_STRESS_MODIFIER = .5D;
	// if that task is an a.i. task within a person's job, then the stress effect is
	// 1/2
	private static final double SKILL_STRESS_MODIFIER = .1D;

	protected static final int FIRST_ITEM_RESOURCE_ID = ResourceUtil.FIRST_ITEM_RESOURCE_ID;

	protected static final int FIRST_EQUIPMENT_RESOURCE_ID = ResourceUtil.FIRST_EQUIPMENT_RESOURCE_ID;

	/** Level of top level Task */
	private static final int TOP_LEVEL = 1;

	protected static final int OXYGEN_ID = ResourceUtil.oxygenID;
	protected static final int FOOD_ID = ResourceUtil.foodID;
	protected static final int WATER_ID = ResourceUtil.waterID;
	protected static final int ICE_ID = ResourceUtil.iceID;
	protected static final int REGOLITH_ID = ResourceUtil.regolithID;
	protected static final int ROCK_SAMPLES_ID = ResourceUtil.rockSamplesID;
	
	// Data members
	/** True if task is finished. */
	private boolean done;
	/** True if task has a time duration. */
	protected boolean hasDuration;
	/** Is this task effort driven. */
	protected boolean effortDriven;
	/** Task should create Historical events. */
	private boolean createEvents;
	/** Amount of time required to complete current phase (in millisols) */
	protected double phaseTimeRequired;
	/** Amount of time completed on the current phase (in millisols) */
	protected double phaseTimeCompleted;
	/** Stress modified by person performing task per millisol. */
	protected double stressModifier;
	/** The time duration (in millisols) of the task. */
	private double duration;
	/** The current amount of time spent on the task (in millisols). */
	private double timeCompleted;
	/** The id of the person/robot. */
	protected Integer id;
	/** The id of the teacher. */
	protected Integer teacherID;
	/** The name of the task. */
	private String name = "";
	/** Description of the task. */
	private String description = "";
	/** The level of this Task, one is top level Task **/
	private int level = TOP_LEVEL;

	/** The person teaching this task if any. */
	private transient Person teacher;
	/** The person performing the task. */
	protected transient Person person;
	/** The robot performing the task. */
	protected transient Robot robot;

	/** The worker performing the task */
	protected transient Worker worker;
	/** Unit for events distribution */
	private transient Unit eventTarget;

	/** The sub-task of this task. */
	protected Task subTask;
	/** The phase of this task. */
	private TaskPhase phase;

	/** A collection of the task's phases. */
	private Collection<TaskPhase> phases;

	private List<SkillType> neededSkills = null;

	/** Ratio of work time to experience */
	private double experienceRatio;

	/** What natural attribute influences experience points */
	private NaturalAttributeType experienceAttribute = NaturalAttributeType.EXPERIENCE_APTITUDE;

	/** The static instance of the mars clock */
	protected static MarsClock marsClock;
	/** The static instance of the event manager */
	public static HistoricalEventManager eventManager;
	/** The static instance of the relationship manager */
	public static RelationshipManager relationshipManager;
	/** The static instance of the UnitManager */
	protected static UnitManager unitManager;
	/** The static instance of the ScientificStudyManager */
	protected static ScientificStudyManager scientificStudyManager;
	/** The static instance of the SurfaceFeatures */
	protected static SurfaceFeatures surfaceFeatures;
	/** The static instance of the OrbitInfo */
	protected static OrbitInfo orbitInfo;
	/** The static instance of the MissionManager */
	protected static MissionManager missionManager;
	/** The static instance of the personConfig */
	protected static PersonConfig personConfig = SimulationConfig.instance().getPersonConfig();
	/** The static instance of the personConfig */
	protected static CropConfig cropConfig = SimulationConfig.instance().getCropConfiguration();
	/** The static instance of the TerrainElevation */
	protected static TerrainElevation terrainElevation;

	
	/**
	 * Constructs a Task object that has a fixed duration.
	 * 
	 * @param name            the name of the task
	 * @param worker          the worker performing the task
	 * @param effort          Does this task require physical effort
	 * @param createEvents    Does this task create events?
	 * @param stressModifier  stress modified by person performing task per
	 *                        millisol.
	 * @param primarySkill    The main skill needed for this Task
	 * @param experienceRatio What is the ratio of time per experience points
	 * @param duration        the time duration (in millisols) of the task (or 0 if
	 *                        none)
	 */
	protected Task(String name, Worker worker, boolean effort, boolean createEvents, double stressModifier,
			SkillType primarySkill, double experienceRatio, double duration) {

		this(name, worker, effort, createEvents, stressModifier, primarySkill, experienceRatio);

		if ((duration <= 0D) || !Double.isFinite(duration)) {
			throw new IllegalArgumentException("Task duration must be positive :" + duration);
		}

		this.duration = duration;
		this.hasDuration = true;
	}

	/**
	 * Constructs a Task object. It has no fixed duration.
	 * 
	 * @param name            the name of the task
	 * @param worker          the worker performing the task
	 * @param effort          Does this task require physical effort
	 * @param createEvents    Does this task create events?
	 * @param stressModifier  stress modified by person performing task per
	 *                        millisol.
	 * @param primarySkill    Main skill needed for task
	 * @param experienceRatio Ratio of work time per experience point
	 */
	protected Task(String name, Worker worker, boolean effort, boolean createEvents, double stressModifier,
			SkillType primarySkill, double experienceRatio) {
		this.name = name;
		this.description = name;
		this.effortDriven = effort;
		this.createEvents = createEvents;
		this.stressModifier = stressModifier;
		this.experienceRatio = experienceRatio;
		this.hasDuration = false;

		if (primarySkill != null) {
			addAdditionSkill(primarySkill);
		}

		if (worker == null) {
			throw new IllegalArgumentException("Worker can not be null");
		}
		this.worker = worker;

		if (worker instanceof Person) {
			this.person = (Person) worker;
			this.id = this.person.getIdentifier();
			this.eventTarget = this.person;
		}

		else if (worker instanceof Robot) {
			this.robot = (Robot) worker;
			this.id = this.robot.getIdentifier();
			this.eventTarget = this.robot;
		}

		done = false;

		timeCompleted = 0D;

		phase = null;
		phases = new ArrayList<>();

		// For sub task
		if (subTask != null) {
			subTask.setPhase(null);
			subTask = null;
		}
	}

	/**
	 * Constructs a basic Task object. It requires no skill and has a fixed
	 * duration.
	 * 
	 * @param name           the name of the task
	 * @param worker         the worker performing the task
	 * @param effort         Does this task require physical effort
	 * @param createEvents   Does this task create events?
	 * @param stressModifier stress modified by person performing task per millisol.
	 * @param duration       the time duration (in millisols) of the task (or 0 if
	 *                       none)
	 */
	protected Task(String name, Worker worker, boolean effort, boolean createEvents, double stressModifier,
			double duration) {
		this(name, worker, effort, createEvents, stressModifier, null, 0D, duration);
	}

	/**
	 * Set the Natural attribute that influences experience points. By default this
	 * is EXPERIENCE_APTITUDE
	 * 
	 * @param experienceAttribute the NaturalAttributeType enum
	 */
	protected void setExperienceAttribute(NaturalAttributeType experienceAttribute) {
		this.experienceAttribute = experienceAttribute;
	}

	protected void addAdditionSkill(SkillType newSkill) {
		if (neededSkills == null) {
			neededSkills = new ArrayList<>();
		}
		neededSkills.add(newSkill);
	}

	private void endSubTask() {
		// For sub task
		if (subTask != null) {
			subTask.endTask();
		}
	}

	public void endSubTask2() {
		// For sub task 2
		if (subTask != null) {
			subTask.endSubTask();
		}
	}

	/**
	 * Ends the task and performs any final actions.
	 * This has a lifecycle of invoking the callback clearDown method.
	 * This method cannot be overridden but the lifecycle callback
	 * method should be used to receive notification.
	 * 
	 * @see #clearDown()
	 */
	public final void endTask() {
		// Check we haven't been here already ??
		if (!done) {
			// Set done to true first to catch any re-calls
			done = true;

			// Cleardown the subclass task
			clearDown();
			
			// Set done to true
			setPhase(null);
			setDescription("");
	
			// End subtask
			endSubTask();
			
			// Fires task end event
			eventTarget.fireUnitUpdate(UnitEventType.TASK_ENDED_EVENT, this);
	
			// Create ending task historical event if needed.
			if (createEvents) {
	
				registerNewEvent(new TaskEvent(eventTarget, this, eventTarget, EventType.TASK_FINISH,
						eventTarget.getLocationTag().getExtendedLocation(),
						""));
			}
		}
//		else {
//			logger.warning(worker, "EndTask repeated " + name);
//		}
	}

	/**
	 * This method is part of the Task Life Cycle. It is called once
	 * and only once per Task when it is ended.
	 * Subclasses should override to receive callback when the Task is ending.
	 */
	protected void clearDown() {
	}

	/**
	 * Helper method for Event subclasses to register historical events.
	 * @param newEvent the new event
	 */
	protected static void registerNewEvent(HistoricalEvent newEvent) {
		eventManager.registerNewEvent(newEvent);
	}
	
	/**
	 * Return the value of the effort driven flag.
	 * 
	 * @return Effort driven.
	 */
	public boolean isEffortDriven() {
		return effortDriven;
	}

	/**
	 * Returns the name of the task.
	 * 
	 * @return the task's name
	 */
	public String getName() {
		return getName(true);
	}

	/**
	 * Gets the name of the task.
	 * 
	 * @param allowSubtask true if subtask name should be used.
	 * @return the task's name.
	 */
	public String getName(boolean allowSubtask) {
		if (allowSubtask && (subTask != null) && !subTask.done) {
			return subTask.getName();
		} else {
			return name;
		}
	}

	/**
	 * Gets the task name
	 * 
	 * @return the task's name in String.
	 */
	public String getTaskName() {
		return this.getClass().getSimpleName();

	}

	/**
	 * Sets the task's name.
	 * 
	 * @param name the task name.
	 */
	protected void setName(String name) {
		this.name = name;
		this.eventTarget.fireUnitUpdate(UnitEventType.TASK_NAME_EVENT, name);
	}

	/**
	 * Returns a string that is a description of what the task is currently doing.
	 * This is mainly for user interface purposes. Derived tasks should extend this
	 * if necessary. Defaults to just the name of the task.
	 * 
	 * @return the description of what the task is currently doing
	 */
	public String getDescription() {
		if (description == null || description.equals("") ) {
			return "";
		}
		return description;
	}

	/**
	 * Gets the description of the task.
	 * 
	 * @param allowSubtask true if subtask description should be used.
	 * @return the task description.
	 */
	public String getDescription(boolean allowSubtask) {
		if (allowSubtask && (subTask != null) && !subTask.done) {
			return subTask.getDescription();
		} else {
			return description;
		}
	}

	/**
	 * Sets the task's description.
	 * 
	 * @param des the task description.
	 */
	protected void setDescription(String des) {
		description = des;
		eventTarget.fireUnitUpdate(UnitEventType.TASK_DESCRIPTION_EVENT, des);
		
		if (phase != null) {
			// Record the activity
			if (canRecord()) {
				Mission ms = worker.getMission();
				worker.getTaskManager().recordTask(this,
						(ms != null ? ms.getTypeID() : null));
			}
		}
	}

	/**
	 * Returns a boolean whether this task should generate events
	 * 
	 * @return boolean flag.
	 */
	protected boolean getCreateEvents() {
		return createEvents;
	}

	/**
	 * Gets a string of the current phase of the task.
	 * 
	 * @return the current phase of the task
	 */
	public TaskPhase getPhase() {
		return phase;
	}

	/**
	 * Sets the task's current phase.
	 * 
	 * @param newPhase the phase to set the task at.
	 */
	protected void setPhase(TaskPhase newPhase) {
		phase = newPhase;
	
		// Method is called via endTask with a null phase
		if (newPhase != null) {
			if (phases != null && !phases.isEmpty() && phases.contains(newPhase)) {
	
				// Note: need to avoid java.lang.StackOverflowError when calling
				// PersonTableModel.unitUpdate()
				eventTarget.fireUnitUpdate(UnitEventType.TASK_PHASE_EVENT, newPhase);
			}
			
			// Record the activity
			if (canRecord()) {
				Mission ms = worker.getMission();
				worker.getTaskManager().recordTask(this,
						(ms != null ? ms.getTypeID() : null));
			}
		}
	}

	/**
	 * Does a change of Phase for this Task generate an entry in the Task Schedule 
	 * @return true by default
	 */
	protected boolean canRecord() {
		return true;
	}
	
	/**
	 * Adds a phase to the task's collection of phases.
	 * 
	 * @param newPhase the new phase to add.
	 */
	protected void addPhase(TaskPhase newPhase) {
		if (newPhase == null) {
			throw new IllegalArgumentException("newPhase is null");
		} else if ((phases != null && (phases.isEmpty() || !phases.contains(newPhase)))) {
			phases.add(newPhase);
		}
	}

	/**
	 * Determines if task is still active.
	 * 
	 * @return true if task is completed
	 */
	public boolean isDone() {
		return done;
	}

	/**
	 * Adds a new sub-task.
	 * 
	 * @param newSubTask the new sub-task to be added
	 */
	public void addSubTask(Task newSubTask) {
		if (subTask != null) {
			if (subTask.done) {
				createSubTask(newSubTask);
			}

			else if (!subTask.getDescription().equalsIgnoreCase(newSubTask.getDescription())) {
				subTask.addSubTask(newSubTask);
			}
		}

		else {
			createSubTask(newSubTask);
		}
	}

	/**
	 * Create a new sub-task.
	 * 
	 * @param newSubTask the new sub-task to be added
	 */
	private void createSubTask(Task newSubTask) {
		subTask = newSubTask;
		subTask.level = this.level + 1;
		eventTarget.fireUnitUpdate(UnitEventType.TASK_SUBTASK_EVENT, newSubTask);
	}

	/**
	 * Gets the task's subtask. Returns null if none
	 * 
	 * @return subtask
	 */
	public Task getSubTask() {
		return subTask;
	}

	/**
	 * Perform the task for the given number of seconds. Children should override
	 * and implement this.
	 * 
	 * @param time amount of time (millisol) given to perform the task (in
	 *             millisols)
	 * @return amount of time (millisol) remaining after performing the task (in
	 *         millisols)
	 */
	public double performTask(double time) {
		double timeLeft = time;
		if (subTask != null) {
			if (subTask.isDone()) {
				subTask.setPhase(null);
				subTask.setDescription("");
			} else {
				timeLeft = subTask.performTask(timeLeft);
			}
		}

		// If no subtask, and still active perform this task (could be ended by the subTask).
		if (!done && ((subTask == null) || subTask.isDone())) {

			if (person != null) {
				// If task is effort-driven and person is incapacitated, end task.
				if (effortDriven && (person.getPerformanceRating() == 0D)) {
					// "Resurrect" him a little to give him a chance to make amend
					person.getPhysicalCondition().setPerformanceFactor(.1);
					endTask();
				} else {
					timeLeft = executeMappedPhase(timeLeft, time);
				}

				if (time - timeLeft > time / 8D) // SMALL_AMOUNT_OF_TIME)
					// Modify stress performing task.
					modifyStress(time - timeLeft);

			}

			else if (robot != null) {
				// If task is effort-driven and person is incapacitated, end task.
				if (effortDriven && (robot.getPerformanceRating() == 0D)) {
					endTask();
				} else {
					timeLeft = executeMappedPhase(timeLeft, time);
				}
			}
		}

		return timeLeft;
	}

	/**
	 * Execute the mapped phase repeatedly
	 * 
	 * @param timeLeft the time left previously
	 * @param time the time pulse
	 * @return the time left currently
	 */
	private double executeMappedPhase(double timeLeft, double time) {
		// Perform phases of task until time is up or task is done.
		while ((timeLeft > 0D) && !done && getPhase() != null && ((subTask == null) || subTask.done)) {

			if (hasDuration) {
				// Keep track of the duration of the task.
				double timeRequired = duration - timeCompleted;
				if (timeLeft > timeRequired) {
					timeLeft = timeLeft - timeRequired;
					// No need to record consumed time as already know the duration
					performMappedPhase(timeRequired);
					timeCompleted = duration;
					// NOTE: does endTask() cause Sleep task to unncessarily end and restart ?
					endTask();
				} else {
					double remainingTime = timeLeft;
					timeLeft = performMappedPhase(timeLeft);
					timeCompleted += remainingTime;
				}
			} else {
				timeLeft = performMappedPhase(timeLeft);
			}
		}

		return timeLeft;
	}

	/**
	 * Performs the method mapped to the task's current phase.
	 * 
	 * @param time the amount of time (millisol) the phase is to be performed.
	 * @return the remaining time (millisol) after the phase has been performed.
	 */
	protected abstract double performMappedPhase(double time);

	/**
	 * Should the start of this task create an historical event.
	 * 
	 * @param create New flag value.
	 */
	protected void setCreateEvents(boolean create) {
		createEvents = create;
	}

	/**
	 * Get a string representation of this Task. It's content will consist of the
	 * description.
	 *
	 * @return Description of the task.
	 */
	public String toString() {
		return description;
	}

	/**
	 * Compare this object to another for an ordering. THe ordering is based on the
	 * alphabetic ordering of the Name attribute.
	 *
	 * @param other Object to compare against.
	 * @return integer comparison of the two objects.
	 * @throws ClassCastException if the object in not of a Task.
	 */
	public int compareTo(Task other) {
		return name.compareTo(other.name);
	}

	/**
	 * Modify stress from performing task for given time.
	 * 
	 * @param time the time performing the task.
	 */
	private void modifyStress(double time) {

		if (person != null) {
			double effectiveStressModifier = stressModifier;

			if (stressModifier > 0D) {

				JobType job = person.getMind().getJob();

				//	TODO What to do about this ?
//				if ((job != null) && JobUtil.getJobSpec(job).isJobRelatedTask(getClass())) {
//					effectiveStressModifier *= JOB_STRESS_MODIFIER;
//				}

				// Reduce stress modifier for person's skill related to the task.
				int skill = getEffectiveSkillLevel();
				effectiveStressModifier -= (effectiveStressModifier * (double) skill * SKILL_STRESS_MODIFIER);

				// If effective stress modifier < 0, set it to 0.
				if (effectiveStressModifier < 0D) {
					effectiveStressModifier = 0D;
				}
			}

			person.getPhysicalCondition()
					.setStress(person.getPhysicalCondition().getStress() + (effectiveStressModifier * time));
		}
	}

	/**
	 * Set the task's stress modifier. Stress modifier can be positive (increase in
	 * stress) or negative (decrease in stress).
	 * 
	 * @param newStressModifier stress modification per millisol.
	 */
	protected void setStressModifier(double newStressModifier) {
		this.stressModifier = newStressModifier;
	}

	/**
	 * Gets the probability modifier for a task if person needs to go to a new
	 * building.
	 * 
	 * @param person      the person to perform the task.
	 * @param newBuilding the building the person is to go to.
	 * @return probability modifier
	 * @throws BuildingException if current or new building doesn't have life
	 *                           support function.
	 */
	protected static double getCrowdingProbabilityModifier(Person person, Building newBuilding) {
		double modifier = 1D;

		Building currentBuilding = BuildingManager.getBuilding(person);

		if ((currentBuilding != null) && (newBuilding != null) && (currentBuilding != newBuilding)) {

			// Increase probability if current building is overcrowded.
			LifeSupport currentLS = currentBuilding.getLifeSupport();
			int currentOverCrowding = currentLS.getOccupantNumber() - currentLS.getOccupantCapacity();
			if (currentOverCrowding > 0) {
				modifier *= ((double) currentOverCrowding + 2);
			}

			// Decrease probability if new building is overcrowded.
			LifeSupport newLS = newBuilding.getLifeSupport();
			int newOverCrowding = newLS.getOccupantNumber() - newLS.getOccupantCapacity();
			if (newOverCrowding > 0) {
				modifier /= ((double) newOverCrowding + 2);
			}
		}

		return modifier;
	}

	protected static double getCrowdingProbabilityModifier(Robot robot, Building newBuilding) {
		double modifier = 1D;

		return modifier;
	}

	/**
	 * Gets the effective skill level a worker has at this task.
	 * 
	 * @return effective skill level
	 */
	public int getEffectiveSkillLevel() {
		int result = 0;
		if (neededSkills != null && worker != null) {
			SkillManager manager = worker.getSkillManager();
			for (SkillType skillType : neededSkills) {
				result += manager.getEffectiveSkillLevel(skillType);
			}

			// Take the average
			result = result / neededSkills.size();
		}

		return result;
	}

	/**
	 * Gets a list of the skills associated with this task. May be empty list if no
	 * associated skills.
	 * 
	 * @return list of skills
	 */
	public List<SkillType> getAssociatedSkills() {
		return neededSkills;
	}

	/**
	 * Checks if someone is teaching this task to the person performing it.
	 * 
	 * @return true if teacher.
	 */
	public boolean hasTeacher() {
		return (teacher != null);
	}

	/**
	 * Gets the person teaching this task.
	 * 
	 * @return teacher or null if none.
	 */
	public Person getTeacher() {
		return teacher;
	}

	/**
	 * Sets the person teaching this task.
	 * 
	 * @param newTeacher the new teacher.
	 */
	public void setTeacher(Person newTeacher) {
		this.teacher = newTeacher;
	}

	/**
	 * Who is working on this Task.
	 * @return
	 */
	public Worker getWorker() {
		return worker;
	}
	
	/**
	 * Gets the experience modifier when being taught by a teacher.
	 * 
	 * @return modifier;
	 */
	protected double getTeachingExperienceModifier() {
		double result = 1D;

		if (hasTeacher()) {
			int teachingModifier = teacher.getNaturalAttributeManager().getAttribute(NaturalAttributeType.TEACHING);
			int learningModifier = worker.getNaturalAttributeManager()
					.getAttribute(NaturalAttributeType.ACADEMIC_APTITUDE);

			result += (double) (teachingModifier + learningModifier) / 100D;
		}

		return result;
	}

	/**
	 * Gets the probability modifier for a person performing a task based on his/her
	 * relationships with the people in the room the task is to be performed in.
	 * 
	 * @param person   the person to check for.
	 * @param building the building the person will need to be in for the task.
	 * @return probability modifier
	 */
	protected static double getRelationshipModifier(Person person, Building building) {
		double result = 1D;

		if ((person == null) || (building == null)) {
			throw new IllegalArgumentException("Task.getRelationshipModifier(): null parameter.");
		} else {
			if (building.hasFunction(FunctionType.LIFE_SUPPORT)) {
				LifeSupport lifeSupport = building.getLifeSupport();
				double totalOpinion = 0D;
				Iterator<Person> i = lifeSupport.getOccupants().iterator();
				while (i.hasNext()) {
					Person occupant = i.next();
					if (person != occupant) {
						totalOpinion += ((relationshipManager.getOpinionOfPerson(person, occupant) - 50D) / 50D);
					}
				}

				if (totalOpinion >= 0D) {
					result *= (1D + totalOpinion);
				} else {
					result /= (1D - totalOpinion);
				}
			}
		}

		return result;
	}

	/**
	 * Adds experience to the worker;s skills used in this task.
	 * 
	 * @param time the amount of time (ms) the person performed this task.
	 */
	protected void addExperience(double time) {
		if (neededSkills != null) {
			// Add experience to "Primary skill" skill
			// (1 base experience point per X millisols of work)
			// Experience points adjusted by worker natural attribute.
			double newPoints = time / experienceRatio;
			int experienceAptitude = worker.getNaturalAttributeManager().getAttribute(experienceAttribute);

			newPoints += newPoints * ((double) experienceAptitude - 50D) / 100D;
			newPoints *= getTeachingExperienceModifier();

			SkillManager sm = worker.getSkillManager();
			for (SkillType skillType : neededSkills) {
				sm.addExperience(skillType, newPoints, time);
			}
		}
	}

	/**
	 * Check for a simple accident in entity. This will use the
	 * {@link #getEffectiveSkillLevel()} method in the risk calculation.
	 * 
	 * @param entity Entity that can malfunction
	 * @param time   the amount of time working (in millisols)
	 * @param chance Chance of an accident
	 */
	protected void checkForAccident(Malfunctionable entity, double time, double chance) {
		int skill = getEffectiveSkillLevel();
		checkForAccident(entity, time, chance, skill, null);
	}

	/**
	 * Check for a complex accident in an Entity. This can cover inside & outside
	 * accidents
	 * 
	 * @param entity   Entity that can malfunction
	 * @param time     the amount of time working (in millisols)
	 * @param chance   Chance of an accident
	 * @param skill    Skill of the worker
	 * @param location Location of the accident; maybe null
	 */
	protected void checkForAccident(Malfunctionable entity, double time, double chance, int skill, String location) {

		if (skill <= 3) {
			chance *= (4 - skill);
		} else {
			chance /= (skill - 2);
		}

		// Modify based on the entity building's wear condition.
		chance *= entity.getMalfunctionManager().getWearConditionAccidentModifier();

		if (RandomUtil.lessThanRandPercent(chance * time)) {
			entity.getMalfunctionManager().createASeriesOfMalfunctions(location, worker);
		}
	}

	/**
	 * Gets the duration of the task or 0 if none.
	 * 
	 * @return duration (millisol)
	 */
	protected double getDuration() {
		return duration;
	}

	/**
	 * Sets the duration of the task
	 * 
	 * @param newDuration the new duration (millisol)
	 */
	protected void setDuration(double newDuration) {
		if (newDuration < 0D) {
			throw new IllegalArgumentException("newDuration less than 0");
		}
		this.duration = newDuration;
	}

	/**
	 * Gets the amount of time the task has completed.
	 * 
	 * @return time (in millisols)
	 */
	protected double getTimeCompleted() {
		return timeCompleted;
	}

	/**
	 * Walk to an available activity spot in a building.
	 * 
	 * @param building  the destination building.
	 * @param function  Particular area within the building
	 * @param allowFail true if walking is allowed to fail.
	 */
	protected void walkToTaskSpecificActivitySpotInBuilding(Building building, FunctionType function, boolean allowFail) {

		if ((function != null) && (building.hasFunction(function))) {
			walkToActivitySpotInBuilding(building, function, allowFail);
		} else {
			// If no available activity spot, go to random location in building.
			walkToRandomLocInBuilding(building, allowFail);
		}
	}

	/**
	 * Walk to an available research activity spot in a building.
	 * 
	 * @param building  the destination building.
	 * @param allowFail true if walking is allowed to fail.
	 */
	protected void walkToResearchSpotInBuilding(Building building, boolean allowFail) {
		
		if (building.hasFunction(FunctionType.RESEARCH)) {
			walkToActivitySpotInBuilding(building, FunctionType.RESEARCH, allowFail);
		} 
		else if (building.hasFunction(FunctionType.ADMINISTRATION)) {
			walkToActivitySpotInBuilding(building, FunctionType.ADMINISTRATION, allowFail);
		} 
		else if (building.hasFunction(FunctionType.DINING)) {
			walkToActivitySpotInBuilding(building, FunctionType.DINING, allowFail);
		} 
		else if (building.hasFunction(FunctionType.LIVING_ACCOMMODATIONS)) {
			walkToActivitySpotInBuilding(building, FunctionType.LIVING_ACCOMMODATIONS, allowFail);			
		} 
		else {
			// If no available activity spot, go to random location in building.
			walkToRandomLocInBuilding(building, allowFail);
		}
	}

	/**
	 * Walks to the bed assigned for this person
	 * 
	 * @param building the building the bed is at
	 * @param person the person who walks to the bed
	 * @param allowFail true if allowing the walk task to fail
	 */
	protected boolean walkToBed(Building building, Person person, boolean allowFail) {
		Point2D bed = person.getBed();
		if (bed != null) {
			Point2D spot = LocalAreaUtil.getLocalRelativeLocation(bed.getX() - building.getXLocation(),
					bed.getY() - building.getYLocation(), building);
			// Create subtask for walking to destination.
			createWalkingSubtask(building, spot, allowFail);
			return true;
		}
		else
			return false;
	}

	/**
	 * Walk to an available activity spot of a particular function type in a
	 * building.
	 * 
	 * @param building     the destination building.
	 * @param functionType the building function type for the activity.
	 * @param allowFail    true if walking is allowed to fail.
	 */
	protected void walkToActivitySpotInBuilding(Building building, FunctionType functionType, boolean allowFail) {

		Function f = building.getFunction(functionType);
		if (f == null) {
			// If the functionType does not exist in this building, go to random location in
			// building.
//			walkToRandomLocInBuilding(building, allowFail);
			return;
		}

		Point2D settlementLoc = null;
		if (person != null) {
			// Find available activity spot in building.
			settlementLoc = f.getAvailableActivitySpot(person);
		} else if (robot != null) {
			// Find available activity spot in building.
			settlementLoc = f.getAvailableActivitySpot(robot);
		}

		if (settlementLoc != null) {
			// Create subtask for walking to destination.
			createWalkingSubtask(building, settlementLoc, allowFail);
		}
//		else {
//			// If no available activity spot, go to random location in building.
//			walkToRandomLocInBuilding(building, allowFail);
//		}
	}

	protected Point2D walkToEVASpot(Building building) {

		Point2D loc = building.getFunction(FunctionType.EVA).getAvailableActivitySpot(person);

		if (loc != null) {
			// Create subtask for walking to destination.
			createWalkingSubtask(building, loc, false);
		}

		return loc;
	}

	/**
	 * Walk to an empty activity spot in a building.
	 * 
	 * @param building     the destination building.
	 * @param allowFail    true if walking is allowed to fail.
	 */
	private void walkToEmptyActivitySpotInBuilding(Building building, boolean allowFail) {

		Function f = building.getEmptyActivitySpotFunction();
		if (f == null) {
			// If the functionType does not exist in this building, go to random location in
			// building.
//			walkToRandomLocInBuilding(building, allowFail);
//			endTask();
			return;
		}

		Point2D settlementLoc = null;
		if (person != null) {
			// Find available activity spot in building.
			settlementLoc = f.getAvailableActivitySpot(person);
		} else if (robot != null) {
			// Find available activity spot in building.
			settlementLoc = f.getAvailableActivitySpot(robot);
		}

		if (settlementLoc != null) {
			// Create subtask for walking to destination.
			createWalkingSubtask(building, settlementLoc, allowFail);
		} else {
//			endTask();
			// If no available activity spot, go to random location in building.
//			walkToRandomLocInBuilding(building, allowFail);
			return;
		}
	}

	/**
	 * Walk to a random interior location in a building.
	 * 
	 * @param building  the destination building.
	 * @param allowFail true if walking is allowed to fail.
	 */
	protected void walkToRandomLocInBuilding(Building building, boolean allowFail) {

		Point2D interiorPos = LocalAreaUtil.getRandomInteriorLocation(building);
		Point2D adjustedInteriorPos = LocalAreaUtil.getLocalRelativeLocation(interiorPos.getX(), interiorPos.getY(),
				building);

		// Create subtask for walking to destination.
		createWalkingSubtask(building, adjustedInteriorPos, allowFail);
	}

	/**
	 * Walk to an available passenger activity spot in a rover.
	 * 
	 * @param rover     the rover.
	 * @param allowFail true if walking is allowed to fail.
	 */
	protected void walkToPassengerActivitySpotInRover(Rover rover, boolean allowFail) {
		walkToActivitySpotInRover(rover, rover.getPassengerActivitySpots(), allowFail);
	}

	/**
	 * Walk to an available lab activity spot in a rover.
	 * 
	 * @param rover     the rover.
	 * @param allowFail true if walking is allowed to fail.
	 */
	protected void walkToLabActivitySpotInRover(Rover rover, boolean allowFail) {
		walkToActivitySpotInRover(rover, rover.getLabActivitySpots(), allowFail);
	}

	/**
	 * Walk to an available sick bay activity spot in a rover.
	 * 
	 * @param rover     the rover.
	 * @param allowFail true if walking is allowed to fail.
	 */
	protected void walkToSickBayActivitySpotInRover(Rover rover, boolean allowFail) {
		walkToActivitySpotInRover(rover, rover.getSickBayActivitySpots(), allowFail);
	}

	/**
	 * Walk to an available activity spot in a rover from a list of activity spots.
	 * 
	 * @param rover         the rover.
	 * @param activitySpots list of activity spots.
	 * @param allowFail     true if walking is allowed to fail.
	 */
	protected void walkToActivitySpotInRover(Rover rover, List<Point2D> activitySpots, boolean allowFail) {

		// Determine available operator activity spots.
		Point2D activitySpot = null;
		if ((activitySpots != null) && (activitySpots.size() > 0)) {

			List<Point2D> availableSpots = new CopyOnWriteArrayList<Point2D>();
			Iterator<Point2D> i = activitySpots.iterator();
			while (i.hasNext()) {
				Point2D spot = i.next();
				Point2D localSpot = LocalAreaUtil.getLocalRelativeLocation(spot.getX(), spot.getY(), rover);
				if (isActivitySpotAvailable(rover, localSpot)) {
					availableSpots.add(localSpot);
				}
			}

			// Randomly select an activity spot from available spots.
			if (availableSpots.size() > 0) {
				activitySpot = availableSpots.get(RandomUtil.getRandomInt(availableSpots.size() - 1));
			}
		}

		walkToActivitySpotInRover(rover, activitySpot, allowFail);
	}

	/**
	 * Checks if an activity spot is available (unoccupied).
	 * 
	 * @param rover        the rover.
	 * @param activitySpot the activity spot (local-relative)
	 * @return true if activity spot is unoccupied.
	 */
	private boolean isActivitySpotAvailable(Rover rover, Point2D activitySpot) {

		boolean result = true;

		if (person != null) {
			// Check all crew members other than person doing task.
			Iterator<Person> i = rover.getCrew().iterator();
			while (i.hasNext()) {
				Person crewmember = i.next();
				if (!crewmember.equals(person)) {

					// Check if crew member's location is very close to activity spot.
					Point2D crewmemberLoc = new Point2D.Double(crewmember.getXLocation(), crewmember.getYLocation());
					if (LocalAreaUtil.areLocationsClose(activitySpot, crewmemberLoc)) {
						result = false;
					}
				}
			}
		} else if (robot != null) {
			// Check all crew members other than robot doing task.
			Iterator<Robot> i = rover.getRobotCrew().iterator();
			while (i.hasNext()) {
				Robot crewmember = i.next();
				if (!crewmember.equals(robot)) {

					// Check if crew member's location is very close to activity spot.
					Point2D crewmemberLoc = new Point2D.Double(crewmember.getXLocation(), crewmember.getYLocation());
					if (LocalAreaUtil.areLocationsClose(activitySpot, crewmemberLoc)) {
						result = false;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Walk to an available activity spot in a rover.
	 * 
	 * @param rover        the destination rover.
	 * @param activitySpot the activity spot as a Point2D object.
	 * @param allowFail    true if walking is allowed to fail.
	 */
	private void walkToActivitySpotInRover(Rover rover, Point2D activitySpot, boolean allowFail) {

		if (activitySpot != null) {

			// Create subtask for walking to destination.
			createWalkingSubtask(rover, activitySpot, allowFail);
		} else {

			// Walk to a random location in the rover.
			walkToRandomLocInRover(rover, allowFail);
		}
	}

	/**
	 * Walk to a random interior location in a rover.
	 * 
	 * @param rover     the destination rover.
	 * @param allowFail true if walking is allowed to fail.
	 */
	protected void walkToRandomLocInRover(Rover rover, boolean allowFail) {

		Point2D interiorPos = LocalAreaUtil.getRandomInteriorLocation(rover);
		Point2D adjustedInteriorPos = LocalAreaUtil.getLocalRelativeLocation(interiorPos.getX(), interiorPos.getY(),
				rover);

		// Create subtask for walking to destination.
		createWalkingSubtask(rover, adjustedInteriorPos, allowFail);
	}

	/**
	 * Walk to a random location.
	 * 
	 * @param allowFail true if walking is allowed to fail.
	 */
	public void walkToRandomLocation(boolean allowFail) {

		if (person != null) {
			// If person is in a settlement, walk to random building.
			Settlement s = person.getSettlement();
			if (s != null) {

//				Building currentBuilding = person.getBuildingLocation();
//
//				if (currentBuilding.getBuildingType().equalsIgnoreCase(Building.ASTRONOMY_OBSERVATORY)) {
//					walkToEmptyActivitySpotInBuilding(currentBuilding, allowFail);
//					return;
//				}
				
				List<Building> buildingList = s.getBuildingManager()
						.getBuildingsWithoutFunctionType(FunctionType.EVA);
				// Randomize its order
				Collections.shuffle(buildingList);	

				if (buildingList.size() > 0) {
					for (Building b : buildingList) {
//						if (!currentBuilding.equals(b)) {
							FunctionType ft = b.getEmptyActivitySpotFunctionType();
							if (ft != null) {
								walkToEmptyActivitySpotInBuilding(b, allowFail);
								break;
							}
//						}
					}
				}
			}
			// If person is in a vehicle, walk to random location within vehicle.
			else if (person.isInVehicle()) {

				// Walk to a random location within rover if possible.
				if (person.getVehicle() instanceof Rover) {
					walkToRandomLocInRover((Rover) person.getVehicle(), allowFail);
				}
			}
		} else if (robot != null) {
			// If robot is in a settlement, walk to random building.
			if (robot.isInSettlement()) {
				walkToAssignedDutyLocation(robot, false);
			}
			// If robot is in a vehicle, walk to random location within vehicle.
//	        else if (robot.isInVehicle()) {

			// Walk to a random location within rover if possible.
//	            if (robot.getVehicle() instanceof Rover) {
//	                walkToRandomLocInRover((Rover) robot.getVehicle(), allowFail);
//	            }
//	        }
		}
	}

	protected void walkToAssignedDutyLocation(Robot robot, boolean allowFail) {
		if (robot.isInSettlement()) {
			Building currentBuilding = BuildingManager.getBuilding(robot);

			if (currentBuilding != null) {
				RobotType type = robot.getRobotType();
				FunctionType fct = null;

				if (type == RobotType.CHEFBOT)
					fct = FunctionType.COOKING;
				else if (type == RobotType.CONSTRUCTIONBOT)
					fct = FunctionType.MANUFACTURE;
				else if (type == RobotType.DELIVERYBOT)
					fct = FunctionType.ROBOTIC_STATION;
				else if (type == RobotType.GARDENBOT)
					fct = FunctionType.FARMING;
				else if (type == RobotType.MAKERBOT)
					fct = FunctionType.MANUFACTURE;
				else if (type == RobotType.MEDICBOT)
					fct = FunctionType.MEDICAL_CARE;
				else if (type == RobotType.REPAIRBOT)
					fct = FunctionType.LIFE_SUPPORT;
				else
					fct = FunctionType.ROBOTIC_STATION;

				List<Building> buildingList = currentBuilding.getBuildingManager().getBuildingsNoHallwayTunnelObs(fct);

				if (buildingList.size() > 0) {
					int buildingIndex = RandomUtil.getRandomInt(buildingList.size() - 1);

					Building building = buildingList.get(buildingIndex);

					if (robot.getSettlement().getBuildingConnectors(building).size() > 0) {
						logger.log(robot, Level.FINER, 5000, "Is walking toward " + building.getNickName());
						walkToActivitySpotInBuilding(building, fct, allowFail);
					}
				}
			}
		}
	}

	/**
	 * Create a walk to an interior position in a building or vehicle.
	 * 
	 * @param interiorObject the destination interior object.
	 * @param settlementPos  the settlement local position destination.
	 * @param allowFail      true if walking is allowed to fail.
	 */
	private void createWalkingSubtask(LocalBoundedObject interiorObject, Point2D settlementPos, boolean allowFail) {

		if (person != null) {
			if (Walk.canWalkAllSteps(person, settlementPos.getX(), settlementPos.getY(), 0, interiorObject)) {

				// Add subtask for walking to destination.
				addSubTask(new Walk(person, settlementPos.getX(), settlementPos.getY(), 0, interiorObject));
			} else {

				if (!allowFail) {
					logger.log(person, Level.INFO, 4_000, "Ended the task of walking to " + interiorObject);
					endTask();
				} else {
					logger.log(person, Level.INFO, 4_000, "Unable to walk to " + interiorObject);
				}
			}
		} else if (robot != null) {
			if (Walk.canWalkAllSteps(robot, settlementPos.getX(), settlementPos.getY(), 0, interiorObject)) {
				// Add subtask for walking to destination.
				addSubTask(new Walk(robot, settlementPos.getX(), settlementPos.getY(), 0, interiorObject));
			} else {
				if (!allowFail) {
					logger.log(robot, Level.INFO, 4_000, "Ended the task of walking to " + interiorObject);
					endTask();
				} else {
					logger.log(robot, Level.INFO, 4_000, "Was unable to walk to " + interiorObject);
				}
			}
		}
	}
	


	public void reinit() {
		person = unitManager.getPersonByID(id);
		robot = unitManager.getRobotByID(id);

		if (person != null) {
			worker = person;
			eventTarget = person;
		} else {
			worker = robot;
			eventTarget = robot;
		}

		if (teacherID != null && (!teacherID.equals(Integer.valueOf(-1)) || !teacherID.equals(Integer.valueOf(0))))
			teacher = unitManager.getPersonByID(teacherID);

		if (subTask != null)
			subTask.reinit();
	}


	/**
	 * Reloads instances after loading from a saved sim
	 * 
	 * @param c  {@link MarsClock}
	 * @param e  {@link HistoricalEventManager}
	 * @param r  {@link RelationshipManager}
	 * @param u  {@link UnitManager}
	 * @param s  {@link ScientificStudyManager}
	 * @param sf {@link SurfaceFeatures}
	 * @param oi {@link OrbitInfo}
	 * @param m  {@link MissionManager}
	 * @param pc  {@link PersonConfig}
	 */
	public static void initializeInstances(MarsClock c, HistoricalEventManager e, RelationshipManager r, UnitManager u,
			ScientificStudyManager s, SurfaceFeatures sf, OrbitInfo oi, MissionManager m, PersonConfig pc) {
		marsClock = c;
		eventManager = e;
		relationshipManager = r;
		unitManager = u;
		scientificStudyManager = s;
		surfaceFeatures = sf;
		orbitInfo = oi;
		missionManager = m;
		personConfig = pc;
	}
}

