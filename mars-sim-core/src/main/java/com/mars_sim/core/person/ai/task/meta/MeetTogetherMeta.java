/*
 * Mars Simulation Project
 * MeetTogetherMeta.java
 * @date 2022-09-02
 * @author Manny Kung
 */
package com.mars_sim.core.person.ai.task.meta;

import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.person.ai.role.RoleType;
import com.mars_sim.core.person.ai.task.MeetTogether;
import com.mars_sim.core.person.ai.task.util.FactoryMetaTask;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.tool.Msg;


/**
 * Meta task for MeetTogether task.
 */
public class MeetTogetherMeta extends FactoryMetaTask {

    /** default logger. */
//    private static final SimLogger logger = SimLogger.getLogger(MeetTogetherMeta.class.getName());
	
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.meetTogether"); //$NON-NLS-1$
    
	private static final int CAP = 1_000;
	
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
        
        if (person.isInSettlement() && roleType != null) {
	
	        if (roleType.isCouncil())
	        	result += 50D;
	
	        else if (roleType.isChief())
	        	result += 30D;
	 
	        // TODO: Probability affected by the person's stress and fatigue.	
	        boolean isOnShiftNow = person.isOnDuty();
	        
	        int size = person.getAssociatedSettlement().getIndoorPeopleCount();
	        result /= 2 * Math.sqrt(size/8.0);
	        
	        if (isOnShiftNow)
	        	result = result * 10;
	        
	        if (result > 0)
	        	result = result + result * person.getPreference().getPreferenceScore(this)/5D;
	
	        // Probability affected by the person's stress and fatigue.
	        double fatigue = person.getPhysicalCondition().getFatigue();
	        
	        result -= fatigue/50;
	         
	        if (result < 0) 
	        	result = 0;
	        
	        // Effort-driven task modifier.
	        result *= person.getPerformanceRating();
	       
        }
        
        if (result > CAP)
        	result = CAP;
        
        return result;
    }
}
