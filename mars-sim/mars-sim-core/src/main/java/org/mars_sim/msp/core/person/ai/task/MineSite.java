/**
 * Mars Simulation Project
 * MineSite.java
 * @version 3.07 2014-09-22
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.Mining;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * Task for mining minerals at a site.
 */
public class MineSite
extends EVAOperation
implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static Logger logger = Logger.getLogger(MineSite.class.getName());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.mineSite"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase MINING = new TaskPhase(Msg.getString(
            "Task.phase.mining")); //$NON-NLS-1$

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
     * @param person the person performing the task.
     * @param site the explored site to mine.
     * @param rover the rover used for the EVA operation.
     * @param luv the light utility vehicle used for mining.
     */
    public MineSite(Person person, Coordinates site, Rover rover,
            LightUtilityVehicle luv) {

        // Use EVAOperation parent constructor.
        super(NAME, person, true, RandomUtil.getRandomDouble(50D) + 10D);

        // Initialize data members.
        this.site = site;
        this.rover = rover;
        this.luv = luv;
        operatingLUV = false;

        // Determine location for mining site.
        Point2D miningSiteLoc = determineMiningSiteLocation();
        setOutsideSiteLocation(miningSiteLoc.getX(), miningSiteLoc.getY());

        // Add task phase
        addPhase(MINING);
    }
    public MineSite(Robot robot, Coordinates site, Rover rover,
            LightUtilityVehicle luv) {

        // Use EVAOperation parent constructor.
        super(NAME, robot, true, RandomUtil.getRandomDouble(50D) + 10D);

        // Initialize data members.
        this.site = site;
        this.rover = rover;
        this.luv = luv;
        operatingLUV = false;

        // Determine location for mining site.
        Point2D miningSiteLoc = determineMiningSiteLocation();
        setOutsideSiteLocation(miningSiteLoc.getX(), miningSiteLoc.getY());

        // Add task phase
        addPhase(MINING);
    }
    /**
     * Determine location for the mining site.
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

                newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(),
                        boundedLocalPoint.getY(), rover);
                if (person != null)
                    goodLocation = LocalAreaUtil.checkLocationCollision(newLocation.getX(), newLocation.getY(),
                            person.getCoordinates());
                else if (robot != null)
	                goodLocation = LocalAreaUtil.checkLocationCollision(newLocation.getX(), newLocation.getY(),
	                        robot.getCoordinates());
	          }
        }

        return newLocation;
    }

    /**
     * Checks if a person can mine a site.
     * @param person the person
     * @param rover the rover
     * @return true if person can mine a site.
     */
    public static boolean canMineSite(Person person, Rover rover) {
        // Check if person can exit the rover.
        boolean exitable = ExitAirlock.canExitAirlock(person, rover.getAirlock());

        SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();

        // Check if it is night time outside.
        boolean sunlight = surface.getSurfaceSunlight(rover.getCoordinates()) > 0;

        // Check if in dark polar region.
        boolean darkRegion = surface.inDarkPolarRegion(rover.getCoordinates());

        // Check if person's medical condition will not allow task.
        boolean medical = person.getPerformanceRating() < .5D;

        return (exitable && (sunlight || darkRegion) && !medical);
    }
    public static boolean canMineSite(Robot robot, Rover rover) {
        // Check if robot can exit the rover.
        boolean exitable = ExitAirlock.canExitAirlock(robot, rover.getAirlock());

        SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();

        // Check if it is night time outside.
        boolean sunlight = surface.getSurfaceSunlight(rover.getCoordinates()) > 0;

        // Check if in dark polar region.
        boolean darkRegion = surface.inDarkPolarRegion(rover.getCoordinates());

        // Check if robot's medical condition will not allow task.
        boolean medical = robot.getPerformanceRating() < .5D;

        return (exitable && (sunlight || darkRegion) && !medical);
    }

    @Override
    protected TaskPhase getOutsideSitePhase() {
        return MINING;
    }

    @Override
    protected double performMappedPhase(double time) {

        time = super.performMappedPhase(time);

        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (MINING.equals(getPhase())) {
            return miningPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Perform the mining phase of the task.
     * @param time the time available (millisols).
     * @return remaining time after performing phase (millisols).
     * @throws Exception if error performing phase.
     */
    private double miningPhase(double time) {

        // Check for an accident during the EVA operation.
        checkForAccident(time);

        // 2015-05-29 Check for radiation exposure during the EVA operation.
        checkForRadiation(time);

        // Check if there is reason to cut the mining phase short and return
        // to the rover.
        if (shouldEndEVAOperation() || addTimeOnSite(time)) {
            // End operating light utility vehicle.
        	if (person != null) {
        		if (luv.getInventory().containsUnit(person)) {
	                luv.getInventory().retrieveUnit(person);
	                luv.setOperator(null);
	                operatingLUV = false;
	            }
        	}
            else if (robot != null) {
	        	if (luv.getInventory().containsUnit(robot)) {
	                luv.getInventory().retrieveUnit(robot);
	                luv.setOperator(null);
	                operatingLUV = false;
	            }
            }

            setPhase(WALK_BACK_INSIDE);
            return time;
        }

        // Operate light utility vehicle if no one else is operating it.
        if (!luv.getMalfunctionManager().hasMalfunction() && (luv.getCrewNum() == 0) && (luv.getRobotCrewNum() == 0))
        	if (person != null) {

            	if (luv.getInventory().canStoreUnit(person, false)) {
                    luv.getInventory().storeUnit(person);

                    Point2D.Double vehicleLoc = LocalAreaUtil.getRandomInteriorLocation(luv);
                    Point2D.Double settlementLoc = LocalAreaUtil.getLocalRelativeLocation(vehicleLoc.getX(),
                            vehicleLoc.getY(), luv);

    	                person.setXLocation(settlementLoc.getX());
    	                person.setYLocation(settlementLoc.getY());
    	                luv.setOperator(person);

                    operatingLUV = true;
                    setDescription(Msg.getString("Task.description.mineSite.detail",
                            luv.getName())); //$NON-NLS-1$
                }
                else {
                    logger.info(person.getName() + " could not operate " + luv.getName());
                }

        	}
            else if (robot != null) {

	        	if (luv.getInventory().canStoreUnit(robot, false)) {
	                luv.getInventory().storeUnit(robot);

	                Point2D.Double vehicleLoc = LocalAreaUtil.getRandomInteriorLocation(luv);
	                Point2D.Double settlementLoc = LocalAreaUtil.getLocalRelativeLocation(vehicleLoc.getX(),
	                        vehicleLoc.getY(), luv);

		                robot.setXLocation(settlementLoc.getX());
		                robot.setYLocation(settlementLoc.getY());
		                luv.setOperator(robot);

	                operatingLUV = true;
	                setDescription(Msg.getString("Task.description.mineSite.detail",
	                        luv.getName())); //$NON-NLS-1$
	            }
	            else {
	                logger.info(person.getName() + " could not operate " + luv.getName());
	            }
        }

        // Excavate minerals.
        excavateMinerals(time);

        // Add experience points
        addExperience(time);

        return 0D;
    }

    /**
     * Excavating minerals from the mining site.
     * @param time the time to excavate minerals.
     * @throws Exception if error excavating minerals.
     */
    private void excavateMinerals(double time) {

        Map<String, Double> minerals = Simulation.instance().getMars().getSurfaceFeatures()
                .getMineralMap().getAllMineralConcentrations(site);
        Iterator<String> i = minerals.keySet().iterator();
        while (i.hasNext()) {
            String mineralName = i.next();
            double amountExcavated = 0D;
            if (operatingLUV) {
                amountExcavated = LUV_EXCAVATION_RATE * time;
            }
            else {
                amountExcavated = HAND_EXCAVATION_RATE * time;
            }
            double mineralConcentration = minerals.get(mineralName);
            amountExcavated *= mineralConcentration / 100D;
            amountExcavated *= getEffectiveSkillLevel();

            AmountResource mineralResource = AmountResource.findAmountResource(mineralName);
            Mining mission = null;
            if (person != null)
                mission = (Mining) person.getMind().getMission();
            else if (robot != null)
            	mission = (Mining) robot.getBotMind().getMission();

            mission.excavateMineral(mineralResource, amountExcavated);
        }
    }

    @Override
    protected void addExperience(double time) {
        SkillManager manager = null;
        if (person != null)
            manager = person.getMind().getSkillManager();
        else if (robot != null)
        	manager = robot.getBotMind().getSkillManager();

        // Add experience to "EVA Operations" skill.
        // (1 base experience point per 100 millisols of time spent)
        double evaExperience = time / 100D;

        // Experience points adjusted by person's "Experience Aptitude" attribute.
        NaturalAttributeManager nManager = null;
        if (person != null)
        	nManager = person.getNaturalAttributeManager();
        else if (robot != null)
        	nManager = robot.getNaturalAttributeManager();
        int experienceAptitude = nManager.getAttribute(NaturalAttribute.EXPERIENCE_APTITUDE);
        double experienceAptitudeModifier = (((double) experienceAptitude) - 50D) / 100D;
        evaExperience += evaExperience * experienceAptitudeModifier;
        evaExperience *= getTeachingExperienceModifier();
        manager.addExperience(SkillType.EVA_OPERATIONS, evaExperience);

        // If phase is mining, add experience to areology skill.
        if (MINING.equals(getPhase())) {
            // 1 base experience point per 10 millisols of mining time spent.
            // Experience points adjusted by person's "Experience Aptitude" attribute.
            double areologyExperience = time / 10D;
            areologyExperience += areologyExperience * experienceAptitudeModifier;
            manager.addExperience(SkillType.AREOLOGY, areologyExperience);

            // If person is driving the light utility vehicle, add experience to driving skill.
            // 1 base experience point per 10 millisols of mining time spent.
            // Experience points adjusted by person's "Experience Aptitude" attribute.
            if (operatingLUV) {
                double drivingExperience = time / 10D;
                drivingExperience += drivingExperience * experienceAptitudeModifier;
                manager.addExperience(SkillType.DRIVING, drivingExperience);
            }
        }
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(3);
        results.add(SkillType.EVA_OPERATIONS);
        results.add(SkillType.AREOLOGY);
        if (operatingLUV) {
            results.add(SkillType.DRIVING);
        }
        return results;
    }

    @Override
    public int getEffectiveSkillLevel() {
        int result = 0;

        SkillManager manager = null;
        if (person != null)
        	manager = person.getMind().getSkillManager();
        else if (robot != null)
        	manager = robot.getBotMind().getSkillManager();
        int EVAOperationsSkill = manager.getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
        int areologySkill = manager.getEffectiveSkillLevel(SkillType.AREOLOGY);
        if (operatingLUV) {
            int drivingSkill = manager.getEffectiveSkillLevel(SkillType.DRIVING);
            result = (int) Math.round((double)(EVAOperationsSkill + areologySkill + drivingSkill) / 3D);
        }
        else {
            result = (int) Math.round((double)(EVAOperationsSkill + areologySkill) / 2D);
        }

        return result;
    }

    @Override
    protected void checkForAccident(double time) {
        super.checkForAccident(time);

        // Check for light utility vehicle accident if operating one.
        if (operatingLUV) {
            double chance = BASE_LUV_ACCIDENT_CHANCE;

            // Driving skill modification.
            int skill = 0;
            if (person != null)
                skill = person.getMind().getSkillManager().getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
            else if (robot != null)
            	skill = robot.getBotMind().getSkillManager().getEffectiveSkillLevel(SkillType.EVA_OPERATIONS);
            if (skill <= 3) {
                chance *= (4 - skill);
            }
            else {
                chance /= (skill - 2);
            }

            // Modify based on the LUV's wear condition.
            chance *= luv.getMalfunctionManager().getWearConditionAccidentModifier();

            if (RandomUtil.lessThanRandPercent(chance * time)) {
                luv.getMalfunctionManager().accident();
            }
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

    @Override
    public void destroy() {
        super.destroy();

        site = null;
        rover = null;
        luv = null;
    }
}