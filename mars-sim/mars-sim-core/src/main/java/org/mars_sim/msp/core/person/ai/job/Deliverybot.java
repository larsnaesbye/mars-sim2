/**
 * Mars Simulation Project
 * Deliverybot.java
 * @version 3.07 2015-03-02
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.job;

import java.io.Serializable;
import java.util.Iterator;

import org.mars_sim.msp.core.person.NaturalAttribute;
import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Robot;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.Trade;
import org.mars_sim.msp.core.person.ai.mission.TravelToSettlement;
import org.mars_sim.msp.core.person.ai.task.ConsolidateContainers;
import org.mars_sim.msp.core.person.ai.task.LoadVehicleEVA;
import org.mars_sim.msp.core.person.ai.task.LoadVehicleGarage;
import org.mars_sim.msp.core.person.ai.task.UnloadVehicleEVA;
import org.mars_sim.msp.core.person.ai.task.UnloadVehicleGarage;
import org.mars_sim.msp.core.structure.Settlement;

public class Deliverybot
extends RobotJob
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static double TRADING_RANGE = 1500D;
	private static double SETTLEMENT_MULTIPLIER = 3D;

	/**
	 * Constructor.
	 */
	public Deliverybot() {
		// Use Job constructor.
		super(Deliverybot.class);
		
		// 2015-01-03 Added PrepareDessert
		//jobTasks.add(PrepareDessert.class);

		// Add trader-related tasks.
		jobTasks.add(LoadVehicleEVA.class);
        jobTasks.add(LoadVehicleGarage.class);
        jobTasks.add(UnloadVehicleEVA.class);
        jobTasks.add(UnloadVehicleGarage.class);
        //jobTasks.add(ConsolidateContainers.class);.
		jobMissionStarts.add(Trade.class);
		jobMissionJoins.add(Trade.class);
        jobMissionStarts.add(TravelToSettlement.class);
		jobMissionJoins.add(TravelToSettlement.class);

	}
	
	/**
	 * Gets a robot's capability to perform this job.
	 * @param robot the robot to check.
	 * @return capability (min 0.0).
	 */
	public double getCapability(Robot robot) {
		
		double result = 0D;
		
		int tradingSkill = robot.getBotMind().getSkillManager().getSkillLevel(SkillType.TRADING);
		result = tradingSkill;
		
		NaturalAttributeManager attributes = robot.getNaturalAttributeManager();
		
		// Add experience aptitude.
		int experienceAptitude = attributes.getAttribute(NaturalAttribute.EXPERIENCE_APTITUDE);
		result+= result * ((experienceAptitude - 50D) / 100D);
		
		// Add conversation.
		int conversation = attributes.getAttribute(NaturalAttribute.CONVERSATION);
		result+= result * ((conversation - 50D) / 100D);
		
		return result;
	}

	/**
	 * Gets the base settlement need for this job.
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public double getSettlementNeed(Settlement settlement) {
		
        double result = 0D;
        
        Iterator<Settlement> i = settlement.getUnitManager().getSettlements().iterator();
        while (i.hasNext()) {
            Settlement otherSettlement = i.next();
            if (otherSettlement != settlement) {
                double distance = settlement.getCoordinates().getDistance(otherSettlement.getCoordinates());
                if (distance <= TRADING_RANGE) result += SETTLEMENT_MULTIPLIER; 
            }
        }
        
		return result;
	}
}