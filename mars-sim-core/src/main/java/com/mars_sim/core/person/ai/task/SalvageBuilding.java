/*
 * Mars Simulation Project
 * SalvageBuilding.java
 * @date 2021-10-21
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.task;


import java.util.List;

import com.mars_sim.core.LocalAreaUtil;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.map.location.LocalPosition;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.NaturalAttributeType;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.person.ai.mission.SalvageMission;
import com.mars_sim.core.person.ai.task.util.TaskPhase;
import com.mars_sim.core.structure.Airlock;
import com.mars_sim.core.structure.construction.ConstructionSite;
import com.mars_sim.core.structure.construction.ConstructionStage;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.core.tool.RandomUtil;
import com.mars_sim.core.vehicle.Crewable;
import com.mars_sim.core.vehicle.GroundVehicle;
import com.mars_sim.core.vehicle.LightUtilityVehicle;

/**
 * Task for salvaging a building construction site stage.
 */
public class SalvageBuilding extends EVAOperation {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(SalvageBuilding.class.getName());

	/** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.salvageBuilding"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase SALVAGE = new TaskPhase(Msg.getString(
            "Task.phase.salvage"), createPhaseImpact(SkillType.CONSTRUCTION));

	/** The base chance of an accident while operating LUV per millisol. */
	public static final double BASE_LUV_ACCIDENT_CHANCE = .001;

	// Data members.
	private boolean operatingLUV;

	private ConstructionStage stage;
	private ConstructionSite site;
	private LightUtilityVehicle luv;

	private List<GroundVehicle> vehicles;

	/**
     * Constructor.
     * @param person the person performing the task.
     */
    public SalvageBuilding(Person person, SalvageMission mission) {
        // Use EVAOperation parent constructor.
        super(NAME, person, RandomUtil.getRandomDouble(50D) + 10D, SALVAGE);

		if (person.isSuperUnfit()) {
			checkLocation("Super Unfit.");
			return;
		}

        if (mission.isDone() || !canSalvage(person)) {
            endTask();
            return;
        }

        // Initialize data members.
        this.stage = mission.getConstructionStage();
        this.site = mission.getConstructionSite();
        this.vehicles = mission.getConstructionVehicles();

        init();
    }

	/**
	 * Constructor.
	 * @param person the person performing the task.
	 * @param stage the construction site salvage stage.
	 * @param vehicles the construction vehicles.
	 */
	public SalvageBuilding(Person person, ConstructionStage stage,
			ConstructionSite site, List<GroundVehicle> vehicles) {
		// Use EVAOperation parent constructor.
        super(NAME, person, RandomUtil.getRandomDouble(50D) + 10D, SALVAGE);

        // Initialize data members.
        this.stage = stage;
        this.site = site;
        this.vehicles = vehicles;

        init();
    }

	private void init() {
        setMinimumSunlight(LightLevel.HIGH);

        // Determine location for salvage site.
        LocalPosition salvageSiteLoc = determineSalvageLocation();
        setOutsideSiteLocation(salvageSiteLoc);
	}

    /**
     * Checks if a given person can work on salvaging a building at this time.
     * @param person the person.
     * @return true if person can salvage.
     */
    public  static boolean canSalvage(Person person) {

        // Check if person can exit the settlement airlock.
        Airlock airlock = getWalkableAvailableEgressAirlock(person);
        if (airlock != null && !ExitAirlock.canExitAirlock(person, airlock)) {
            return false;
        }

		// Check if it is night time.
		if (EVAOperation.isGettingDark(person)) {
            logger.fine(person.getName() + " end salvaging building : night time");
            return false;
		}

        // Check if person's medical condition will not allow task.
        return (person.getPerformanceRating() >= .5D);
    }
   

    /**
     * Determine location to go to at salvage site.
     * @return location.
     */
    private LocalPosition determineSalvageLocation() {
    	return LocalAreaUtil.getRandomLocalPos(site);
    }

    @Override
    protected void addExperience(double time) {
    	super.addExperience(time);

        // If person is driving the light utility vehicle, add experience to driving skill.
        // 1 base experience point per 10 millisols of mining time spent.
        // Experience points adjusted by person's "Experience Aptitude" attribute.
        if (SALVAGE.equals(getPhase()) && operatingLUV) {
            int experienceAptitude = worker.getNaturalAttributeManager().getAttribute(NaturalAttributeType.EXPERIENCE_APTITUDE);

            // Experience should be calculated from teh operating LUV method
            double experienceAptitudeModifier = ((experienceAptitude) - 50D) / 100D;
            double drivingExperience = time / 10D;
            drivingExperience += drivingExperience * experienceAptitudeModifier;
            worker.getSkillManager().addExperience(SkillType.PILOTING, drivingExperience, time);
        }
    }

    @Override
    protected double performMappedPhase(double time) {

        time = super.performMappedPhase(time);
        if (!isDone()) {
	        if (getPhase() == null) {
	            throw new IllegalArgumentException("Task phase is null");
	        }
	        else if (SALVAGE.equals(getPhase())) {
	            time = salvage(time);
	        }
        }
        return time;
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
        if (operatingLUV && luv.getMalfunctionManager().hasMalfunction())
            result = true;

        return result;
    }

    /**
     * Performs the salvage phase of the task.
     * 
     * @param time amount (millisols) of time to perform the phase.
     * @return time (millisols) remaining after performing the phase.
     */
    private double salvage(double time) {
    	double remainingTime = 0;

        if (stage.isComplete() || addTimeOnSite(time)) {
            // End operating light utility vehicle.
            if (person != null) {
            	if ((luv != null) && ((Crewable)luv).isCrewmember(person)) {
                    returnVehicle();
            	}
            }
			else if (robot != null) {
				if ((luv != null) && ((Crewable)luv).isRobotCrewmember(robot)) {
					returnVehicle();
				}
			}

			checkLocation("Stage completed.");
			return remainingTime;
        }
	
		// Note: need to call addTimeOnSite() ahead of checkReadiness() since
		// checkReadiness's addTimeOnSite() lacks the details of handling LUV
		if (checkReadiness(time) > 0)
			return time;
		
        // Operate light utility vehicle if no one else is operating it.
        if (!operatingLUV) {
            obtainVehicle();
        }

        // Determine effective work time based on "Construction" and "EVA Operations" skills.
        double workTime = time;
        int skill = getEffectiveSkillLevel();
        if (skill == 0) {
            workTime /= 2;
        }
        else if (skill > 1) {
            workTime += workTime * (.2D * skill);
        }

        // Work on salvage.
        stage.addWorkTime(workTime);

        // Add experience points
        addExperience(workTime);

        // Check if an accident happens during salvage.
        checkForAccident(workTime);

        return 0;
    }

    /**
     * Obtains a construction vehicle from the settlement if possible.
     */
    private void obtainVehicle() {
        for(GroundVehicle vehicle : vehicles) {
            if (!vehicle.getMalfunctionManager().hasMalfunction()
                && (vehicle instanceof LightUtilityVehicle tempLuv) 
                && (tempLuv.getOperator() == null)) {
                    tempLuv.addPerson(person);
                    tempLuv.setOperator(person);

                    luv = tempLuv;
                    operatingLUV = true;

                    // Place light utility vehicles at random location in construction site.
                    LocalPosition settlementLocSite = LocalAreaUtil.getRandomLocalPos(site);
                    luv.setParkedLocation(settlementLocSite, RandomUtil.getRandomDouble(360D));

                    break;
            }
        }
    }

    /**
     * Returns the construction vehicle used to the settlement.
     */
    private void returnVehicle() {
        luv.removePerson(person);
        luv.setOperator(null);
        operatingLUV = false;
    }

    /**
     * Gets the construction stage that is being worked on.
     * @return construction stage.
     */
    public ConstructionStage getConstructionStage() {
        return stage;
    }
}
