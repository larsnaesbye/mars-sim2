/*
 * Mars Simulation Project
 * MineSite.java
 * @date 2021-10-21
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.Mining;
import org.mars_sim.msp.core.person.ai.mission.MissionMember;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * Task for mining minerals at a site.
 */
public class MineSite extends EVAOperation implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(MineSite.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.mineSite"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase MINING = new TaskPhase(Msg.getString("Task.phase.mining")); //$NON-NLS-1$

	/** Excavation rates (kg/millisol). */
	private static final double HAND_EXCAVATION_RATE = .1D;
	/** Excavation rates (kg/millisol). */
	private static final double LUV_EXCAVATION_RATE = 1D;

	/** The base chance of an accident while operating LUV per millisol. */
	public static final double BASE_LUV_ACCIDENT_CHANCE = .001;

	// Data members
	private Coordinates site;
	private Rover rover;
	private LightUtilityVehicle luv;
	private boolean operatingLUV;

	/**
	 * Constructor
	 * 
	 * @param person the person performing the task.
	 * @param site   the explored site to mine.
	 * @param rover  the rover used for the EVA operation.
	 * @param luv    the light utility vehicle used for mining.
	 */
	public MineSite(Person person, Coordinates site, Rover rover, LightUtilityVehicle luv) {

		// Use EVAOperation parent constructor.
		super(NAME, person, true, RandomUtil.getRandomDouble(50D) + 10D, SkillType.AREOLOGY);

		// Initialize data members.
		this.site = site;
		this.rover = rover;
		this.luv = luv;
		operatingLUV = false;

		if (shouldEndEVAOperation()) {
        	if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
        	return;
        }
		
		if (!person.isFit()) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
        	return;
		}
		
		// Determine location for mining site.
		Point2D miningSiteLoc = determineMiningSiteLocation();
		setOutsideSiteLocation(miningSiteLoc.getX(), miningSiteLoc.getY());

		// Add task phase
		addPhase(MINING);
	}

	public MineSite(Robot robot, Coordinates site, Rover rover, LightUtilityVehicle luv) {

		// Use EVAOperation parent constructor.
		super(NAME, robot, true, RandomUtil.getRandomDouble(50D) + 10D, SkillType.AREOLOGY);

		// Initialize data members.
		this.site = site;
		this.rover = rover;
		this.luv = luv;
		operatingLUV = false;

		if (shouldEndEVAOperation()) {
        	if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
        	return;
        }
		
		if (!person.isFit()) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
        	return;
		}
		
		// Determine location for mining site.
		Point2D miningSiteLoc = determineMiningSiteLocation();
		setOutsideSiteLocation(miningSiteLoc.getX(), miningSiteLoc.getY());

		// Add task phase
		addPhase(MINING);
	}

	/**
	 * Determine location for the mining site.
	 * 
	 * @return site X and Y location outside rover.
	 */
	private Point2D determineMiningSiteLocation() {

		Point2D newLocation = null;
		boolean goodLocation = false;
		for (int x = 0; (x < 5) && !goodLocation; x++) {
			for (int y = 0; (y < 10) && !goodLocation; y++) {

				double distance = RandomUtil.getRandomDouble(50D) + (x * 100D) + 50D;
				double radianDirection = RandomUtil.getRandomDouble(Math.PI * 2D);
				double newXLoc = rover.getXLocation() - (distance * Math.sin(radianDirection));
				double newYLoc = rover.getYLocation() + (distance * Math.cos(radianDirection));
				Point2D boundedLocalPoint = new Point2D.Double(newXLoc, newYLoc);

				newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(), boundedLocalPoint.getY(),
						rover);

				goodLocation = LocalAreaUtil.isLocationCollisionFree(newLocation.getX(), newLocation.getY(),
							worker.getCoordinates());
			}
		}

		return newLocation;
	}

	/**
	 * Checks if a person can mine a site.
	 * 
	 * @param member the member
	 * @param rover  the rover
	 * @return true if person can mine a site.
	 */
	public static boolean canMineSite(MissionMember member, Rover rover) {

		if (member instanceof Person) {
			Person person = (Person) member;

			// Check if person can exit the rover.
			if (!ExitAirlock.canExitAirlock(person, rover.getAirlock()))
				return false;

			if (EVAOperation.isGettingDark(person))
				return false;

			// Check if person's medical condition will not allow task.
            return !(person.getPerformanceRating() < .2D);
		}

		return true;
	}

	@Override
	protected TaskPhase getOutsideSitePhase() {
		return MINING;
	}

	@Override
	protected double performMappedPhase(double time) {

		time = super.performMappedPhase(time);
		if (!isDone()) {
			if (getPhase() == null) {
				throw new IllegalArgumentException("Task phase is null");
			}
			else if (MINING.equals(getPhase())) {
				time = miningPhase(time);
			}
		}
		return time;
	}

	/**
	 * Perform the mining phase of the task.
	 * 
	 * @param time the time available (millisols).
	 * @return remaining time after performing phase (millisols).
	 * @throws Exception if error performing phase.
	 */
	private double miningPhase(double time) {
		// Check for radiation exposure during the EVA operation.
		if (isDone() || isRadiationDetected(time)) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
			return time;
		}
		
		if (!person.isFit()) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
		}
		
		// Check if there is reason to cut the mining phase short and return
		// to the rover.
		if (shouldEndEVAOperation() || addTimeOnSite(time)) {
			// End operating light utility vehicle.
			if (person != null) {
				if (((Crewable)luv).isCrewmember(person)) {
					luv.removePerson(person);
					luv.setOperator(null);
					operatingLUV = false;
				}
			} else if (robot != null) {
				if (((Crewable)luv).isRobotCrewmember(robot)) {
					luv.removeRobot(robot);
					luv.setOperator(null);
					operatingLUV = false;
				}
			}

        	if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
		}

		// Operate light utility vehicle if no one else is operating it.
		if (!luv.getMalfunctionManager().hasMalfunction() 
				&& (luv.getCrewNum() == 0) && (luv.getRobotCrewNum() == 0)) {

			if (luv.addPerson(person)) {
				
				Point2D.Double vehicleLoc = LocalAreaUtil.getRandomInteriorLocation(luv);
				Point2D.Double settlementLoc = LocalAreaUtil.getLocalRelativeLocation(vehicleLoc.getX(),
						vehicleLoc.getY(), luv);

				person.setXLocation(settlementLoc.getX());
				person.setYLocation(settlementLoc.getY());
				luv.setOperator(person);

				operatingLUV = true;
				setDescription(Msg.getString("Task.description.mineSite.detail", luv.getName())); // $NON-NLS-1$
			} else {
				logger.info(person, " could not operate " + luv.getName());
			}

		}

		// Excavate minerals.
		excavateMinerals(time);

		// Add experience points
		addExperience(time);

		// Check for an accident during the EVA operation.
		checkForAccident(time);
		
		return 0D;
	}

	/**
	 * Excavating minerals from the mining site.
	 * 
	 * @param time the time to excavate minerals.
	 * @throws Exception if error excavating minerals.
	 */
	private void excavateMinerals(double time) {

		Map<String, Double> minerals = surfaceFeatures.getMineralMap()
				.getAllMineralConcentrations(site);
		Iterator<String> i = minerals.keySet().iterator();
		while (i.hasNext()) {
			String mineralName = i.next();
			double amountExcavated = 0D;
			if (operatingLUV) {
				amountExcavated = LUV_EXCAVATION_RATE * time;
			} else {
				amountExcavated = HAND_EXCAVATION_RATE * time;
			}
			double mineralConcentration = minerals.get(mineralName);
			amountExcavated *= mineralConcentration / 100D;
			amountExcavated *= getEffectiveSkillLevel();

			((Mining) worker.getMission()).excavateMineral(
					ResourceUtil.findAmountResource(mineralName), amountExcavated);
		}
	}

	@Override
	protected void addExperience(double time) {
		super.addExperience(time);

		// If person is driving the light utility vehicle, add experience to driving
		// skill.
		// 1 base experience point per 10 millisols of mining time spent.
		// Experience points adjusted by person's "Experience Aptitude" attribute.
		if (getOutsideSitePhase().equals(getPhase()) && operatingLUV) {
			int experienceAptitude = worker.getNaturalAttributeManager().getAttribute(NaturalAttributeType.EXPERIENCE_APTITUDE);
			double experienceAptitudeModifier = (((double) experienceAptitude) - 50D) / 100D;
			double drivingExperience = time / 10D;
			drivingExperience += drivingExperience * experienceAptitudeModifier;
			worker.getSkillManager().addExperience(SkillType.PILOTING, drivingExperience, time);
		}
	}

	@Override
	protected void checkForAccident(double time) {
		super.checkForAccident(time);

		// Check for light utility vehicle accident if operating one.
		if (operatingLUV) {

			// Driving skill modification.
			int skill = worker.getSkillManager().getEffectiveSkillLevel(SkillType.PILOTING);
			checkForAccident(luv, time, BASE_LUV_ACCIDENT_CHANCE, skill, luv.getName());
		}
	}

	@Override
	protected boolean shouldEndEVAOperation() {
		boolean result = super.shouldEndEVAOperation();

		// If operating LUV, check if LUV has malfunction.
		if (operatingLUV && luv.getMalfunctionManager().hasMalfunction()) {
			result = true;
		}

		return result;
	}
}
