/*
 * Mars Simulation Project
 * ConstructBuilding.java
 * @date 2021-10-21
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.BuildingConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.structure.Airlock;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.construction.ConstructionSite;
import org.mars_sim.msp.core.structure.construction.ConstructionStage;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.GroundVehicle;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;

/**
 * Task for constructing a building construction site stage.
 */
public class ConstructBuilding extends EVAOperation implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final Logger logger = Logger.getLogger(ConstructBuilding.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.constructBuilding"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase CONSTRUCTION = new TaskPhase(Msg.getString("Task.phase.construction")); //$NON-NLS-1$

	// The base chance of an accident while operating LUV per millisol.
	public static final double BASE_LUV_ACCIDENT_CHANCE = .001;

	// Data members.
	private boolean operatingLUV;

	private ConstructionStage stage;
	private ConstructionSite site;
	private LightUtilityVehicle luv;

	private List<GroundVehicle> vehicles;

	/**
	 * Constructor.
	 * 
	 * @param person the person performing the task.
	 */
	public ConstructBuilding(Person person) {
		// Use EVAOperation parent constructor.
		super(NAME, person, true, RandomUtil.getRandomDouble(5D) + 100D, SkillType.CONSTRUCTION);

		if (!person.isFit()) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
        	return;
		}
		
		BuildingConstructionMission mission = getMissionNeedingAssistance(person);
		
		if ((mission != null) && canConstruct(person, mission.getConstructionSite())) {

			// Initialize data members.
			this.stage = mission.getConstructionStage();
			this.site = mission.getConstructionSite();
			this.vehicles = mission.getConstructionVehicles();

			// Determine location for construction site.
			Point2D constructionSiteLoc = determineConstructionLocation();
			setOutsideSiteLocation(constructionSiteLoc.getX(), constructionSiteLoc.getY());

			// Add task phase
			addPhase(CONSTRUCTION);
		} 
		
		else {
			endTask();
		}
	}

	/**
	 * Constructor.
	 * 
	 * @param person   the person performing the task.
	 * @param stage    the construction site stage.
	 * @param vehicles the construction vehicles.
	 * @throws Exception if error constructing task.
	 */
	public ConstructBuilding(Person person, ConstructionStage stage, ConstructionSite site,
			List<GroundVehicle> vehicles) {
		// Use EVAOperation parent constructor.
		super(NAME, person, true, RandomUtil.getRandomDouble(5D) + 100D, SkillType.CONSTRUCTION);

		// Initialize data members.
		this.stage = stage;
		this.site = site;
		this.vehicles = vehicles;

		if (!person.isFit()) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
        	return;
		}
		
		// Determine location for construction site.
		Point2D constructionSiteLoc = determineConstructionLocation();
		setOutsideSiteLocation(constructionSiteLoc.getX(), constructionSiteLoc.getY());

		// Add task phase
		addPhase(CONSTRUCTION);
	}

	/**
	 * Checks if a given person can work on construction at this time.
	 * 
	 * @param person the person.
	 * @return true if person can construct.
	 */
	public static boolean canConstruct(Person person, ConstructionSite site) {

		// Check if person can exit the settlement airlock.
		Airlock airlock = getClosestWalkableAvailableAirlock(person, site.getXLocation(), site.getYLocation());
		if (airlock != null) {
			if (!ExitAirlock.canExitAirlock(person, airlock))
				return false;
		}

		if (EVAOperation.isGettingDark(person))
			return false;
		
//		if (surface.getSolarIrradiance(person.getCoordinates()) == 0D) {
//			logger.fine(person.getName() + " end constructing building : night time");
//			if (!surface.inDarkPolarRegion(person.getCoordinates()))
//				return false;
//		}

		// Check if person's medical condition will not allow task.
		if (person.getPerformanceRating() < .5D)
			return false;

		// Check if there is work that can be done on the construction stage.
		ConstructionStage stage = site.getCurrentConstructionStage();

		boolean workAvailable = false;

		// Checking stage for NullPointerException
		if (stage != null)
			workAvailable = stage.getCompletableWorkTime() > stage.getCompletedWorkTime();

		// System.out.println("stage is " + stage); // test if stage is null

		return (workAvailable);
	}

//    public static boolean canConstruct(Robot robot, ConstructionSite site) {
//
//        // Check if robot can exit the settlement airlock.
//        Airlock airlock = getClosestWalkableAvailableAirlock(robot, site.getXLocation(), site.getYLocation());
//        if (airlock != null) {
//            if(!ExitAirlock.canExitAirlock(robot, airlock))
//            	return false;
//        }
//
//
//        Mars mars = Simulation.instance().getMars();
//        if (mars.getSurfaceFeatures().getSolarIrradiance(robot.getCoordinates()) == 0D) {
//            logger.fine(robot.getName() + " should end EVA: night time.");
//            if (!mars.getSurfaceFeatures().inDarkPolarRegion(robot.getCoordinates()))
//                return false;
//        }
//
//        // Check if robot's medical condition will not allow task.
//        if (robot.getPerformanceRating() < .5D)
//        	return false;
//
//        // Check if there is work that can be done on the construction stage.
//        ConstructionStage stage = site.getCurrentConstructionStage();
//        //boolean workAvailable = stage.getCompletableWorkTime() > stage.getCompletedWorkTime();
//
//        boolean workAvailable = false;
//
//        // 2016-06-08 Checking stage for NullPointerException
//        if (stage != null)
//        	workAvailable = stage.getCompletableWorkTime() > stage.getCompletedWorkTime();
//
//        return (workAvailable);
//    }

	/**
	 * Gets a random building construction mission that needs assistance.
	 * 
	 * @return construction mission or null if none found.
	 */
	public static BuildingConstructionMission getMissionNeedingAssistance(Person person) {

		BuildingConstructionMission result = null;

		List<BuildingConstructionMission> constructionMissions = null;

//        if (person != null) {
		constructionMissions = getAllMissionsNeedingAssistance(person.getAssociatedSettlement());
//        }
//        else if (robot != null) {
//        	constructionMissions = getAllMissionsNeedingAssistance(
//                robot.getAssociatedSettlement());
//        }

		if (constructionMissions.size() > 0) {
			int index = RandomUtil.getRandomInt(constructionMissions.size() - 1);
			result = (BuildingConstructionMission) constructionMissions.get(index);
		}

		return result;
	}

	/**
	 * Gets a list of all building construction missions that need assistance at a
	 * settlement.
	 * 
	 * @param settlement the settlement.
	 * @return list of building construction missions.
	 */
	public static List<BuildingConstructionMission> getAllMissionsNeedingAssistance(Settlement settlement) {

		List<BuildingConstructionMission> result = new CopyOnWriteArrayList<BuildingConstructionMission>();

		Iterator<Mission> i = missionManager.getMissionsForSettlement(settlement).iterator();
		while (i.hasNext()) {
			Mission mission = (Mission) i.next();
			if (mission instanceof BuildingConstructionMission) {
				result.add((BuildingConstructionMission) mission);
			}
		}

		return result;
	}

	/**
	 * Determine location to go to at construction site.
	 * 
	 * @return location.
	 */
	private Point2D determineConstructionLocation() {

		Point2D.Double relativeLocSite = LocalAreaUtil.getRandomInteriorLocation(site, false);
		Point2D.Double settlementLocSite = LocalAreaUtil.getLocalRelativeLocation(relativeLocSite.getX(),
				relativeLocSite.getY(), site);

		return settlementLocSite;
	}

	@Override
	protected TaskPhase getOutsideSitePhase() {
		return CONSTRUCTION;
	}

	@Override
	protected double performMappedPhase(double time) {

		time = super.performMappedPhase(time);
		if (!isDone()) {
			if (getPhase() == null) {
				throw new IllegalArgumentException("Task phase is null");
			} else if (CONSTRUCTION.equals(getPhase())) {
				time = constructionPhase(time);
			}
		}
		return time;
	}

	/**
	 * Perform the construction phase of the task.
	 * 
	 * @param time amount (millisols) of time to perform the phase.
	 * @return time (millisols) remaining after performing the phase.
	 * @throws Exception
	 */
	private double constructionPhase(double time) {
		
		// Check for radiation exposure during the EVA operation.
		if (isRadiationDetected(time)) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
			return time;
		}

		if (shouldEndEVAOperation() || addTimeOnSite(time)) {
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
		
		// Operate light utility vehicle if no one else is operating it.
		if (!operatingLUV) {
			obtainVehicle();
		}

		// Determine effective work time based on "Construction" and "EVA Operations"
		// skills.
		double workTime = time;
		int skill = getEffectiveSkillLevel();
		if (skill == 0) {
			workTime /= 2;
		} else if (skill > 1) {
			workTime += workTime * (.2D * skill);
		}

		// Work on construction.
		stage.addWorkTime(workTime);

		// Add experience points
		addExperience(time);

		boolean availableWork = stage.getCompletableWorkTime() > stage.getCompletedWorkTime();

		// Check if site duration has ended or there is reason to cut the construction
		// phase short and return to the rover.
		if (stage.isComplete() || !availableWork) {

			// End operating light utility vehicle.
			if (luv != null) {
				if ((person != null) && ((Crewable)luv).isCrewmember(person)) {
					returnVehicle();
				}
//                if ((robot != null) && luv.getInventory().containsUnit(robot)) {
//                    returnVehicle();
//                }
			}

			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
			return time;
		}
		
		// Check if an accident happens during construction.
		checkForAccident(time);

		return 0D;
	}

	/**
	 * Obtains a construction vehicle from the settlement if possible.
	 * 
	 * @throws Exception if error obtaining construction vehicle.
	 */
	private void obtainVehicle() {
		Iterator<GroundVehicle> i = vehicles.iterator();
		while (i.hasNext() && (luv == null)) {
			GroundVehicle vehicle = i.next();
			if (!vehicle.getMalfunctionManager().hasMalfunction()) {
				if (vehicle instanceof LightUtilityVehicle) {
					LightUtilityVehicle tempLuv = (LightUtilityVehicle) vehicle;
					if (tempLuv.getOperator() == null) {

//                    	 if (person != null) {
						tempLuv.addPerson(person);
						tempLuv.setOperator(person);
//                    	 }
//
//                         else if (robot != null) {
//	                        //tempLuv.getInventory().storeUnit(robot);
//	                        //tempLuv.setOperator(robot);
//	                     }

						luv = tempLuv;
						operatingLUV = true;

						// Place light utility vehicles at random location in construction site.
						Point2D.Double relativeLocSite = LocalAreaUtil.getRandomInteriorLocation(site);
						Point2D.Double settlementLocSite = LocalAreaUtil
								.getLocalRelativeLocation(relativeLocSite.getX(), relativeLocSite.getY(), site);
						luv.setParkedLocation(settlementLocSite.getX(), settlementLocSite.getY(),
								RandomUtil.getRandomDouble(360D));

						break;
					}
				}
			}
		}
	}

	/**
	 * Returns the construction vehicle used to the settlement.
	 * 
	 * @throws Exception if error returning construction vehicle.
	 */
	private void returnVehicle() {
//    	if (person != null)
		luv.removePerson(person);
//		else if (robot != null)
//	        luv.getInventory().retrieveUnit(robot);

		luv.setOperator(null);
		operatingLUV = false;
	}

	@Override
	protected void addExperience(double time) {
		super.addExperience(time);

		// If person is driving the light utility vehicle, add experience to driving
		// skill.
		// 1 base experience point per 10 millisols of mining time spent.
		// Experience points adjusted by person's "Experience Aptitude" attribute.
		if ((CONSTRUCTION.equals(getPhase())) && operatingLUV) {
			int experienceAptitude = worker.getNaturalAttributeManager().getAttribute(NaturalAttributeType.EXPERIENCE_APTITUDE);
			
			double experienceAptitudeModifier = (((double) experienceAptitude) - 50D) / 100D;
			double drivingExperience = time / 10D;
			drivingExperience += drivingExperience * experienceAptitudeModifier;
			worker.getSkillManager().addExperience(SkillType.PILOTING, drivingExperience, time);
		}
	}

	protected void checkForAccident(double time) {
		super.checkForAccident(time);

		// Check for light utility vehicle accident if operating one.
		if (operatingLUV) {
			// Driving skill modification.
			int skill = worker.getSkillManager().getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
			checkForAccident(luv, time, BASE_LUV_ACCIDENT_CHANCE, skill, site.getName());
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

	/**
	 * Gets the construction stage that is being worked on.
	 * 
	 * @return construction stage.
	 */
	public ConstructionStage getConstructionStage() {
		return stage;
	}
}
