/**
 * Mars Simulation Project
 * ReturnLightUtilityVehicleMeta.java
 * @version 3.08 2015-06-08
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.io.Serializable;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.task.ReturnLightUtilityVehicle;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;

/**
 * Meta task for the ReturnLightUtilityVehicle task.
 */
public class ReturnLightUtilityVehicleMeta implements MetaTask, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.returnLightUtilityVehicle"); //$NON-NLS-1$

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new ReturnLightUtilityVehicle(person);
    }

    @Override
    public double getProbability(Person person) {
        double result = 0D;

        if (person.getLocationSituation() == LocationSituation.IN_VEHICLE) {

            if (person.getVehicle() instanceof LightUtilityVehicle) {
                result = 500D;

    	        // 2015-06-07 Added Preference modifier
    	        if (result > 0)
    	        	result += person.getPreference().getPreferenceScore(this);
    	        if (result < 0) result = 0;
            }
        }

        return result;
    }

	@Override
	public Task constructInstance(Robot robot) {
		return new ReturnLightUtilityVehicle(robot);
	}

	@Override
	public double getProbability(Robot robot) {
        double result = 0D;

        if (robot.getLocationSituation() == LocationSituation.IN_VEHICLE) {

            if (robot.getVehicle() instanceof LightUtilityVehicle) {
                result = 500D;
            }
        }

        return result;
    }
}