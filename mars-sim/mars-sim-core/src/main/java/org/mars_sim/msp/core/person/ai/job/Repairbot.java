/**
 * Mars Simulation Project
 * Repairbot.java
 * @version 3.07 2015-02-02
 * @author Manny Kung
 */

package org.mars_sim.msp.core.person.ai.job;

import java.io.Serializable;

import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Robot;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.task.Maintenance;
import org.mars_sim.msp.core.person.ai.task.MaintenanceEVA;
import org.mars_sim.msp.core.person.ai.task.RepairEVAMalfunction;
import org.mars_sim.msp.core.person.ai.task.RepairMalfunction;
import org.mars_sim.msp.core.structure.Settlement;

public class Repairbot
extends RobotJob
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/**
	 * Constructor.
	 */
	public Repairbot() {
		// Use Job constructor
		super(Repairbot.class);

		// Add technician-related tasks.
		jobTasks.add(Maintenance.class);
		jobTasks.add(MaintenanceEVA.class);
		jobTasks.add(RepairEVAMalfunction.class);
		jobTasks.add(RepairMalfunction.class);

	}

	/**
	 * Gets the base settlement need for this job.
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public double getSettlementNeed(Settlement settlement) {

		double result = 10D;

		// Add number of buildings in settlement.
		result+= settlement.getBuildingManager().getBuildingNum() / 3D;

		// Add number of vehicles parked at settlement.
		result+= settlement.getParkedVehicleNum() / 3D;

		return result;	
	}

	/**
	 * Gets a robot's capability to perform this job.
	 * @param robot the person to check.
	 * @return capability.
	 */
	public double getCapability(Robot robot) {

		double result = 10D;

		int mechanicSkill = robot.getBotMind().getSkillManager().getSkillLevel(SkillType.MECHANICS);
		result += mechanicSkill;

		NaturalAttributeManager attributes = robot.getNaturalAttributeManager();
		int experienceAptitude = attributes.getAttribute(NaturalAttribute.EXPERIENCE_APTITUDE);
		result+= result * ((experienceAptitude - 50D) / 100D);

		//if (robot.getPhysicalCondition().hasSeriousMedicalProblems()) result = 0D;

		return result;
	}
}