/**
 * Mars Simulation Project
 * BiologyStudyFieldWork.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;

import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.MissionMember;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * A task for the EVA operation of performing biology field work at a research site
 * for a scientific study.
 */
public class BiologyStudyFieldWork
extends EVAOperation
implements Serializable {

    /** default serial id.*/
    private static final long serialVersionUID = 1L;

    private static final SimLogger logger = SimLogger.getLogger(BiologyStudyFieldWork.class.getName());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.biologyFieldWork"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase FIELD_WORK = new TaskPhase(Msg.getString(
            "Task.phase.fieldWork.biology")); //$NON-NLS-1$

    // Data members
    private Person leadResearcher;
    private ScientificStudy study;
    private Rover rover;

    /**
     * Constructor.
     * @param person the person performing the task.
     * @param leadResearcher the researcher leading the field work.
     * @param study the scientific study the field work is for.
     * @param rover the rover
     */
    public BiologyStudyFieldWork(Person person, Person leadResearcher, ScientificStudy study,
            Rover rover) {

        // Use EVAOperation parent constructor.
        super(NAME, person, true, RandomUtil.getRandomDouble(50D) + 10D, SkillType.BIOLOGY);

        // Initialize data members.
        this.leadResearcher = leadResearcher;
        this.study = study;
        this.rover = rover;

        // Determine location for field work.
        Point2D fieldWorkLoc = determineFieldWorkLocation();
        setOutsideSiteLocation(fieldWorkLoc.getX(), fieldWorkLoc.getY());

        // Add task phases
        addPhase(FIELD_WORK);
    }

    /**
     * Determine location for field work.
     * @return field work X and Y location outside rover.
     */
    private Point2D determineFieldWorkLocation() {

        Point2D newLocation = null;
        boolean goodLocation = false;
        for (int x = 0; (x < 5) && !goodLocation; x++) {
            for (int y = 0; (y < 10) && !goodLocation; y++) {

                double distance = RandomUtil.getRandomDouble(100D) + (x * 100D) + 50D;
                double radianDirection = RandomUtil.getRandomDouble(Math.PI * 2D);
                double newXLoc = rover.getXLocation() - (distance * Math.sin(radianDirection));
                double newYLoc = rover.getYLocation() + (distance * Math.cos(radianDirection));
                Point2D boundedLocalPoint = new Point2D.Double(newXLoc, newYLoc);

                newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(),
                        boundedLocalPoint.getY(), rover);
                goodLocation = LocalAreaUtil.isLocationCollisionFree(newLocation.getX(), newLocation.getY(),
                        person.getCoordinates());
            }
        }

        return newLocation;
    }

    /**
     * Checks if a person can research a site.
     * @param member the member.
     * @param rover the rover
     * @return true if person can research a site.
     */
    public static boolean canResearchSite(MissionMember member, Rover rover) {

        if (member instanceof Person) {
            Person person = (Person) member;

            // Check if person can exit the rover.
            if(!ExitAirlock.canExitAirlock(person, rover.getAirlock()))
            	return false;
            
            if (isGettingDark(person)) {
    			logger.fine(person, "Ended "
    					+ person.getTaskDescription() + " due to getting too dark.");
    			return false;
    		}

            // Check if person's medical condition will not allow task.
            return !(person.getPerformanceRating() < .5D);
        }

        return true;
    }

    @Override
    protected TaskPhase getOutsideSitePhase() {
        return FIELD_WORK;
    }

    @Override
    protected double performMappedPhase(double time) {

        time = super.performMappedPhase(time);
		if (!isDone()) {
	        if (getPhase() == null) {
	            throw new IllegalArgumentException("Task phase is null");
	        }
	        else if (FIELD_WORK.equals(getPhase())) {
	            time = fieldWorkPhase(time);
	        }	
	    }
		return time;
    }

    /**
     * Perform the field work phase of the task.
     * @param time the time available (millisols).
     * @return remaining time after performing phase (millisols).
     * @throws Exception if error performing phase.
     */
    private double fieldWorkPhase(double time) {

		// Check for radiation exposure during the EVA operation.
		if (isDone() || isRadiationDetected(time)) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
			return time;
		}
		
		// Check if site duration has ended or there is reason to cut the field
		// work phase short and return to the rover.
		if (shouldEndEVAOperation() || addTimeOnSite(time)) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
			return time;
		}
        
        // Add research work to the scientific study for lead researcher.
        addResearchWorkTime(time);

        // Add experience points
        addExperience(time);

		// Check for an accident during the EVA operation.
		checkForAccident(time);

        return 0D;
    }

    /**
     * Adds research work time to the scientific study for the lead researcher.
     * @param time the time (millisols) performing field work.
     */
    private void addResearchWorkTime(double time) {
        // Determine effective field work time.
        double effectiveFieldWorkTime = time;
        int skill = getEffectiveSkillLevel();
        if (skill == 0) {
            effectiveFieldWorkTime /= 2D;
        }
        else if (skill > 1) {
            effectiveFieldWorkTime += effectiveFieldWorkTime * (.2D * skill);
        }

        // If person isn't lead researcher, divide field work time by two.
        if (!person.equals(leadResearcher)) {
            effectiveFieldWorkTime /= 2D;
        }

        // Add research to study for primary or collaborative researcher.
        if (study.getPrimaryResearcher().equals(leadResearcher)) {
            study.addPrimaryResearchWorkTime(effectiveFieldWorkTime);
        }
        else {
            study.addCollaborativeResearchWorkTime(leadResearcher, effectiveFieldWorkTime);
        }
    }
}
