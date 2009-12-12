/**
 * Mars Simulation Project
 * Architect.java
 * @version 2.86 2009-05-10
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.job;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.person.NaturalAttributeManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.Skill;
import org.mars_sim.msp.core.person.ai.mission.BuildingConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.RescueSalvageVehicle;
import org.mars_sim.msp.core.person.ai.mission.TravelToSettlement;
import org.mars_sim.msp.core.structure.Settlement;

/** 
 * The Architect class represents an architect job focusing on construction of buildings, settlement 
 * and other structures.
 */
public class Architect extends Job implements Serializable {

    private static String CLASS_NAME = "org.mars_sim.msp.simulation.person.ai.job.Architect";
    private static Logger logger = Logger.getLogger(CLASS_NAME);
    
    /**
     * Constructor
     */
    public Architect() {
        // Use Job constructor.
        super("Architect");
        
        // Add architect-related tasks.
        
        // Add architect-related missions.
        jobMissionStarts.add(BuildingConstructionMission.class);
        jobMissionJoins.add(BuildingConstructionMission.class);
        jobMissionStarts.add(TravelToSettlement.class);
        jobMissionJoins.add(TravelToSettlement.class);  
        jobMissionStarts.add(RescueSalvageVehicle.class);
        jobMissionJoins.add(RescueSalvageVehicle.class);
    }
    
    @Override
    public double getCapability(Person person) {
        
        double result = 0D;
        
        int constructionSkill = person.getMind().getSkillManager().getSkillLevel(Skill.CONSTRUCTION);
        result = constructionSkill;
        
        NaturalAttributeManager attributes = person.getNaturalAttributeManager();
        int academicAptitude = attributes.getAttribute(NaturalAttributeManager.ACADEMIC_APTITUDE);
        int experienceAptitude = attributes.getAttribute(NaturalAttributeManager.EXPERIENCE_APTITUDE);
        double averageAptitude = (academicAptitude + experienceAptitude) / 2D;
        result+= result * ((averageAptitude - 50D) / 100D);
        
        if (person.getPhysicalCondition().hasSeriousMedicalProblems()) result = 0D;
        
        return result;
    }

    @Override
    public double getSettlementNeed(Settlement settlement) {
        
        double result = 0D;
        
        try {
            // Based on settlement construction profit.
            double constructionProfit = settlement.getConstructionManager().getConstructionValues().
                    getSettlementConstructionProfit();
            result = constructionProfit / 1000D;
            if (result < 0D) result = 0D;
            if (result > 100D) result = 100D;
        }
        catch (Exception e) {
            e.printStackTrace(System.err);
            logger.log(Level.SEVERE,"Architect.getSettlementNeed()", e);
        }
        
        // Add number of buildings currently at settlement / 2.
        result += settlement.getBuildingManager().getBuildingNum() / 2D;
        
        return result;  
    }
}