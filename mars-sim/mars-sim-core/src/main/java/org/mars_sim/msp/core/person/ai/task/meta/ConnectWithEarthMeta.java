/**
 * Mars Simulation Project
 * ConnectWithEarthMeta.java
 * @version 3.08 2015-06-28
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.io.Serializable;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.RoleType;
import org.mars_sim.msp.core.person.ai.task.ConnectWithEarth;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.person.ai.task.WriteReport;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.building.Building;

/**
 * Meta task for the ConnectWithEarth task.
 */
public class ConnectWithEarthMeta implements MetaTask, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.connectWithEarth"); //$NON-NLS-1$

    public RoleType roleType;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new ConnectWithEarth(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT
        		&& !person.getPreference().getTaskStatus(this)) {

            // Probability affected by the person's stress and fatigue.
            PhysicalCondition condition = person.getPhysicalCondition();
            if (condition.getFatigue() < 1200D && condition.getStress() < 75D) {

	            // Get an available office space.
	            Building building = ConnectWithEarth.getAvailableBuilding(person);

	            if (building != null) {
	            	result = 10D;
	            	// A comm facility has terminal equipment that provides communication access with Earth
	            	// It is necessary
	                result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(person, building);
	                result *= TaskProbabilityUtil.getRelationshipModifier(person, building);

		            // Effort-driven task modifier.
		            result *= person.getPerformanceRating();

			        // 2015-06-07 Added Preference modifier
			        if (result > 0)
			        	result += person.getPreference().getPreferenceScore(this);
			        if (result < 0) result = 0;

	            }

            }
        }
        //System.out.println("result : " + result);
        return result;
    }

	@Override
	public Task constructInstance(Robot robot) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double getProbability(Robot robot) {
		// TODO Auto-generated method stub
		return 0;
	}
}