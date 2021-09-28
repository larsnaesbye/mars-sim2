/*
 * Mars Simulation Project
 * MeetTogetherMeta.java
 * @date 2021-09-27
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.job.JobType;
import org.mars_sim.msp.core.person.ai.role.RoleType;
import org.mars_sim.msp.core.person.ai.task.MeetTogether;
import org.mars_sim.msp.core.person.ai.task.utils.MetaTask;
import org.mars_sim.msp.core.person.ai.task.utils.Task;


/**
 * Meta task for MeetTogether task.
 */
public class MeetTogetherMeta extends MetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.meetTogether"); //$NON-NLS-1$
    
    public MeetTogetherMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.WORK_HOUR);
		
		setPreferredJob(JobType.POLITICIAN, JobType.REPORTER);
	}

    @Override
    public Task constructInstance(Person person) {
        return new MeetTogether(person);
    }

    @Override
    public double getProbability(Person person) {
    	
        double result = 0D;
        
        RoleType roleType = person.getRole().getType();
        
        // Probability affected by the person's stress and fatigue.
        PhysicalCondition condition = person.getPhysicalCondition();
        double fatigue = condition.getFatigue();
        
        if (fatigue > 1000)
        	return 0;
        
        if (person.isInSettlement() && roleType != null) {
	
	        if (roleType == RoleType.PRESIDENT
	                	|| roleType == RoleType.MAYOR
	            		|| roleType == RoleType.COMMANDER)
	        	result += 50D;
	
	        else if (RoleType.isChief(roleType))
	        	result += 30D;
	 
	
	        // TODO: Probability affected by the person's stress and fatigue.
	
	        // Effort-driven task modifier.
	        result *= person.getPerformanceRating();
	
	    	int now = Simulation.instance().getMasterClock().getMarsClock().getMillisolInt();
	        boolean isOnShiftNow = person.getTaskSchedule().isShiftHour(now);
	        
	        if (isOnShiftNow)
	        	result = result*1.5D;
	        
	        if (result > 0)
	        	result = result + result * person.getPreference().getPreferenceScore(this)/5D;
	
	        if (result < 0) 
	        	result = 0;
        }
        
        return result;
    }
}
