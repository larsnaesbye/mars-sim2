/**
 * Mars Simulation Project
 * SalvageBuildingMeta.java
 * @version 3.08 2015-06-08
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.mission.BuildingSalvageMission;
import org.mars_sim.msp.core.person.ai.task.EVAOperation;
import org.mars_sim.msp.core.person.ai.task.SalvageBuilding;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.ai.job.Constructionbot;
import org.mars_sim.msp.core.structure.Settlement;

/**
 * Meta task for the SalvageBuilding task.
 */
public class SalvageBuildingMeta implements MetaTask, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.salvageBuilding"); //$NON-NLS-1$

    /** default logger. */
    private static Logger logger = Logger.getLogger(SalvageBuildingMeta.class.getName());

    private SurfaceFeatures surface;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new SalvageBuilding(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        // Check if an airlock is available
        if (EVAOperation.getWalkableAvailableAirlock(person) == null) {
            result = 0D;
        }

        // Check if it is night time.
        if (surface == null)
        	surface = Simulation.instance().getMars().getSurfaceFeatures();

        if (surface.getPreviousSolarIrradiance(person.getCoordinates()) == 0D) {
            if (!surface.inDarkPolarRegion(person.getCoordinates()))
                result = 0D;
        }

        if (result != 0)

        	if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {

	            // Check all building salvage missions occurring at the settlement.
	            try {
	                List<BuildingSalvageMission> missions = SalvageBuilding.
	                        getAllMissionsNeedingAssistance(person.getSettlement());
	                result = 100D * missions.size();
	            }
	            catch (Exception e) {
	                logger.log(Level.SEVERE, "Error finding building salvage missions.", e);
	            }

	            // Crowded settlement modifier
	            Settlement settlement = person.getSettlement();
	            if (settlement.getCurrentPopulationNum() > settlement.getPopulationCapacity()) {
	                result *= 2D;
	            }

	            // Effort-driven task modifier.
	            result *= person.getPerformanceRating();

	            // Job modifier.
	            Job job = person.getMind().getJob();
	            if (job != null) {
	                result *= job.getStartTaskProbabilityModifier(SalvageBuilding.class);
	            }

	            // Modify if construction is the person's favorite activity.
	            if (person.getFavorite().getFavoriteActivity().equalsIgnoreCase("Construction")) {
	                result *= 2D;
	            }

		        // 2015-06-07 Added Preference modifier
		        if (result > 0)
		        	result += person.getPreference().getPreferenceScore(this);
		        if (result < 0) result = 0;
	        }


        return result;
    }

	@Override
	public Task constructInstance(Robot robot) {
        return new SalvageBuilding(robot);
	}

	@Override
	public double getProbability(Robot robot) {

        double result = 0D;

        if (robot.getBotMind().getRobotJob() instanceof Constructionbot) {

	        if (robot.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {

	            // Check all building salvage missions occurring at the settlement.
	            try {
	                List<BuildingSalvageMission> missions = SalvageBuilding.
	                        getAllMissionsNeedingAssistance(robot.getSettlement());
	                result = 100D * missions.size();
	            }
	            catch (Exception e) {
	                logger.log(Level.SEVERE, "Error finding building salvage missions.", e);
	            }
	        }

	        // Effort-driven task modifier.
            result *= robot.getPerformanceRating();

	        // Check if an airlock is available
            if (EVAOperation.getWalkableAvailableAirlock(robot) == null) {
                result = 0D;
            }

	        // Check if it is night time.
            SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();
            if (surface.getPreviousSolarIrradiance(robot.getCoordinates()) == 0D) {
                if (!surface.inDarkPolarRegion(robot.getCoordinates())) {
                    result = 0D;
                }
            }

        }
        return result;
    }
}