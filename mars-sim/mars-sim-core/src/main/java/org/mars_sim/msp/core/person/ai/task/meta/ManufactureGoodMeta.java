/**
 * Mars Simulation Project
 * ManufactureGoodMeta.java
 * @version 3.08 2015-04-13
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.task.ManufactureGood;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.ai.job.Makerbot;
import org.mars_sim.msp.core.structure.building.Building;

/**
 * Meta task for the ManufactureGood task.
 */
public class ManufactureGoodMeta implements MetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.manufactureGood"); //$NON-NLS-1$

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new ManufactureGood(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        // Cancel any manufacturing processes that's beyond the skill of any people
        // associated with the settlement.
        ManufactureGood.cancelDifficultManufacturingProcesses(person);

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {

            // the person has to be inside the settlement to check for manufacture override
            if (!person.getSettlement().getManufactureOverride())  {

                // See if there is an available manufacturing building.
                Building manufacturingBuilding = ManufactureGood.getAvailableManufacturingBuilding(person);
                if (manufacturingBuilding != null) {
                    result = 1D;

                    // Crowding modifier.
                    result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(person, manufacturingBuilding);
                    result *= TaskProbabilityUtil.getRelationshipModifier(person, manufacturingBuilding);

                    // Manufacturing good value modifier.
                    result *= ManufactureGood.getHighestManufacturingProcessValue(person, manufacturingBuilding);

                    // Capping the probability at 100 as manufacturing process values can be very large numbers.
                    if (result > 100D) {
                        result = 100D;
                    }

                    //if (person.getFavorite().getFavoriteActivity().equals("Tinkering"))
                    //    result += 50D;

                    // If manufacturing building has process requiring work, add
                    // modifier.
                    SkillManager skillManager = person.getMind().getSkillManager();
                    int skill = skillManager.getEffectiveSkillLevel(SkillType.MATERIALS_SCIENCE);
                    if (ManufactureGood.hasProcessRequiringWork(manufacturingBuilding, skill))
                        result += 10D;

                    // Effort-driven task modifier.
                    result *= person.getPerformanceRating();

                    // Job modifier.
                    Job job = person.getMind().getJob();
                    if (job != null)
                        result *= job.getStartTaskProbabilityModifier(ManufactureGood.class);

                }
            }
        }

        return result;
    }

	@Override
	public Task constructInstance(Robot robot) {
        return new ManufactureGood(robot);
	}

	@Override
	public double getProbability(Robot robot) {

        double result = 0D;

        // Cancel any manufacturing processes that's beyond the skill of any people
        // associated with the settlement.
        ManufactureGood.cancelDifficultManufacturingProcesses(robot);

	        if (robot.getBotMind().getRobotJob() instanceof Makerbot)

		        if (robot.getLocationSituation() == LocationSituation.IN_SETTLEMENT)
		            // If settlement has manufacturing override, no new
		            // manufacturing processes can be created.
		            if (!robot.getSettlement().getManufactureOverride()) {
		        	// the person has to be inside the settlement to check for manufacture override

			            // See if there is an available manufacturing building.
			            Building manufacturingBuilding = ManufactureGood.getAvailableManufacturingBuilding(robot);
			            if (manufacturingBuilding != null) {
			                result = 100D;

			                // Crowding modifier.
			                result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(robot, manufacturingBuilding);
			                //result *= TaskProbabilityUtil.getRelationshipModifier(robot, manufacturingBuilding);

			                // Manufacturing good value modifier.
			                result *= ManufactureGood.getHighestManufacturingProcessValue(robot, manufacturingBuilding);

			                if (result > 100D) {
			                    result = 100D;
			                }

			                // If manufacturing building has process requiring work, add
			                // modifier.
			                SkillManager skillManager = robot.getBotMind().getSkillManager();
			                int skill = skillManager.getEffectiveSkillLevel(SkillType.MATERIALS_SCIENCE);
			                if (ManufactureGood.hasProcessRequiringWork(manufacturingBuilding, skill)) {
			                    result += 10D;
			                }

				            // Effort-driven task modifier.
				            result *= robot.getPerformanceRating();
			            }

			        }

        return result;
    }
}