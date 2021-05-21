/**
 * Mars Simulation Project
 * LoadVehicleGarageMeta.java
 * @version 3.1.2 2020-09-02
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.util.List;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.location.LocationStateType;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.FavoriteType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.job.JobType;
import org.mars_sim.msp.core.person.ai.job.JobUtil;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.task.LoadVehicleGarage;
import org.mars_sim.msp.core.person.ai.task.utils.MetaTask;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.ai.job.Deliverybot;
import org.mars_sim.msp.core.tool.RandomUtil;

/**
 * Meta task for the LoadVehicleGarage task.
 */
public class LoadVehicleGarageMeta extends MetaTask {
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.loadVehicleGarage"); //$NON-NLS-1$

    /** default logger. */
    private static SimLogger logger = SimLogger.getLogger(LoadVehicleGarageMeta.class.getName());

    public LoadVehicleGarageMeta() {
		super(NAME, WorkerType.BOTH, TaskScope.WORK_HOUR);
	}

    @Override
    public Task constructInstance(Person person) {
        return new LoadVehicleGarage(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        if (person.isInSettlement()) {

            // Probability affected by the person's stress and fatigue.
            PhysicalCondition condition = person.getPhysicalCondition();
            double fatigue = condition.getFatigue();
            double stress = condition.getStress();
            double hunger = condition.getHunger();
            
            if (fatigue > 1000 || stress > 50 || hunger > 500)
            	return 0;
            
            // Check all vehicle missions occurring at the settlement.
            try {
                List<Mission> missions = LoadVehicleGarage.getAllMissionsNeedingLoading(person.getSettlement());
                int num = missions.size();
                if (num == 0)
                	return 0;
                else
                	result = 100D * num;
                
            }
            catch (Exception e) {
                logger.severe(person, "Error finding loading missions.", e);
            }

            // Effort-driven task modifier.
            result *= person.getPerformanceRating();

            // Job modifier.
            JobType job = person.getMind().getJob();
            if (job != null) {
                result *= JobUtil.getStartTaskProbabilityModifier(job, LoadVehicleGarage.class)
                		* person.getSettlement().getGoodsManager().getTransportationFactor();
            }

            // Modify if operations is the person's favorite activity.
            if (person.getFavorite().getFavoriteActivity() == FavoriteType.OPERATION) {
                result += RandomUtil.getRandomInt(1, 20);
            }

            // 2015-06-07 Added Preference modifier
            if (result > 0D) {
                result = result + result * person.getPreference().getPreferenceScore(this)/5D;
            }

            if (result < 0) result = 0;

        }

        return result;
    }

	@Override
	public Task constructInstance(Robot robot) {
		return new LoadVehicleGarage(robot);
	}

	@Override
	public double getProbability(Robot robot) {

        double result = 0D;

        if (robot.getBotMind().getRobotJob() instanceof Deliverybot)

    		if (robot.getLocationStateType() == LocationStateType.INSIDE_SETTLEMENT) {

	            // Check all vehicle missions occurring at the settlement.
	            try {
	                List<Mission> missions = LoadVehicleGarage.getAllMissionsNeedingLoading(robot.getSettlement());
	                result = 100D * missions.size();
	            }
	            catch (Exception e) {
	                logger.severe(robot, "Error finding loading missions.", e);
	            }


	            // Effort-driven task modifier.
	            result *= robot.getPerformanceRating();

	        }

        return result;
    }
}
