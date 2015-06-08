/**
 * Mars Simulation Project
 * ProduceFoodMeta.java
 * @version 3.08 2015-05-29
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.task.ProduceFood;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.ai.job.Chefbot;
import org.mars_sim.msp.core.robot.ai.job.Makerbot;
import org.mars_sim.msp.core.structure.building.Building;

/**
 * Meta task for the ProduceFood task.
 */
public class ProduceFoodMeta implements MetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.produceFood"); //$NON-NLS-1$

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new ProduceFood(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        // Cancel any foodProduction processes that's beyond the skill of any people
        // associated with the settlement.
        ProduceFood.cancelDifficultFoodProductionProcesses(person);

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {

	        // If settlement has foodProduction override, no new foodProduction processes can be created.
	        if (!person.getSettlement().getFoodProductionOverride()) {


	            // See if there is an available foodProduction building.
	            Building foodProductionBuilding = ProduceFood.getAvailableFoodProductionBuilding(person);
	            if (foodProductionBuilding != null) {
	            	result += 1D;

	                // Crowding modifier.
	                result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(person, foodProductionBuilding);
	                result *= TaskProbabilityUtil.getRelationshipModifier(person, foodProductionBuilding);

	                // FoodProduction good value modifier.
	                result *= ProduceFood.getHighestFoodProductionProcessValue(person, foodProductionBuilding);

	                // Capping the probability at 100 as food production process values can be very large numbers.
	                if (result > 100D) {
                        result = 100D;
                    }

	                // If foodProduction building has process requiring work, add
	                // modifier.
	                SkillManager skillManager = person.getMind().getSkillManager();
	                int skill = skillManager.getEffectiveSkillLevel(SkillType.COOKING) * 5;
	                skill += skillManager.getEffectiveSkillLevel(SkillType.MATERIALS_SCIENCE) * 2;
	                skill = (int) Math.round(skill / 7D);
	                if (ProduceFood.hasProcessRequiringWork(foodProductionBuilding, skill)) {
	                    result += 10D;
	                }

	    	        // Effort-driven task modifier.
	    	        result *= person.getPerformanceRating();

	    	        // Job modifier.
	    	        Job job = person.getMind().getJob();
	    	        if (job != null) {
	    	            result *= job.getStartTaskProbabilityModifier(ProduceFood.class);
	    	        }

	                // Modify if cooking is the person's favorite activity.
	                if (person.getFavorite().getFavoriteActivity().equalsIgnoreCase("Cooking")) {
	                    result *= 2D;
	                }

	    	        // 2015-06-07 Added Preference modifier
	                if (result > 0)
	                	result += person.getPreference().getPreferenceScore(this);
	    	        if (result < 0) result = 0;
	            }
	        }
        }
        return result;
    }

	@Override
	public Task constructInstance(Robot robot) {
        return new ProduceFood(robot);
	}

	@Override
	public double getProbability(Robot robot) {

        double result = 0D;

        if (robot.getBotMind().getRobotJob() instanceof Chefbot || robot.getBotMind().getRobotJob() instanceof Makerbot)

			if (robot.getLocationSituation() == LocationSituation.IN_SETTLEMENT)

		        // If settlement has foodProduction override, no new
		        // foodProduction processes can be created.
		        if (! robot.getSettlement().getFoodProductionOverride()) {

		            // See if there is an available foodProduction building.
		            Building foodProductionBuilding = ProduceFood.getAvailableFoodProductionBuilding(robot);
		            if (foodProductionBuilding != null) {
		                result += 100D;

		                // FoodProduction good value modifier.
		                result *= ProduceFood.getHighestFoodProductionProcessValue(robot, foodProductionBuilding);

		                // If foodProduction building has process requiring work, add modifier.
		                SkillManager skillManager = robot.getBotMind().getSkillManager();
		                int skill = skillManager.getEffectiveSkillLevel(SkillType.COOKING) * 5;
		                skill += skillManager.getEffectiveSkillLevel(SkillType.MATERIALS_SCIENCE) * 2;
		                skill = (int) Math.round(skill / 7D);

		                if (ProduceFood.hasProcessRequiringWork(foodProductionBuilding, skill)) {
		                    result += 100D;
		                }

			            // Effort-driven task modifier.
			            result *= robot.getPerformanceRating();

		            }

		        }

        return result;
	}
}