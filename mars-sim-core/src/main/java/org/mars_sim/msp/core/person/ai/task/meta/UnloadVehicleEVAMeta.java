/*
 * Mars Simulation Project
 * UnloadVehicleEVA.java
 * @date 2021-08-28
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.CollectionUtils;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.FavoriteType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.JobType;
import org.mars_sim.msp.core.person.ai.task.EVAOperation;
import org.mars_sim.msp.core.person.ai.task.UnloadVehicleEVA;
import org.mars_sim.msp.core.person.ai.task.utils.MetaTask;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.TaskTrait;
import org.mars_sim.msp.core.structure.Settlement;

/**
 * Meta task for the UnloadVehicleEVA task.
 */
public class UnloadVehicleEVAMeta extends MetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.unloadVehicleEVA"); //$NON-NLS-1$

    /** default logger. */
    private static final Logger logger = Logger.getLogger(UnloadVehicleEVAMeta.class.getName());
    
    public UnloadVehicleEVAMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.WORK_HOUR);
		setFavorite(FavoriteType.OPERATION);
		setTrait(TaskTrait.STRENGTH);
		setPreferredJob(JobType.LOADERS);
	}

    @Override
    public Task constructInstance(Person person) {
        return new UnloadVehicleEVA(person);
    }

    @Override
    public double getProbability(Person person) {
        double result = 0D;

        Settlement settlement = person.getSettlement();
        
        if (settlement != null) {
       
            // Probability affected by the person's stress and fatigue.
            if (!person.getPhysicalCondition().isFitByLevel(500, 50, 500))
            	return 0;
                    
	    	// Check for radiation events
	    	boolean[] exposed = settlement.getExposed();
	
			if (exposed[2]) // SEP can give lethal dose of radiation
	            return 0;
		
	        // Check if an airlock is available
	        if (EVAOperation.getWalkableAvailableAirlock(person) == null)
	    		return 0;
	
	        // Check if it is night time.
			if (EVAOperation.isGettingDark(person))
				return 0;
	        		
            // Checks if the person's settlement is at meal time and is hungry
            if (EVAOperation.isHungryAtMealTime(person))
            	return 0;
            
            // Checks if the person is physically drained
			if (EVAOperation.isExhausted(person))
				return 0;
			
	        // Check all vehicle missions occurring at the settlement.
	        try {
	            int numVehicles = 0;
	            numVehicles += UnloadVehicleEVA.getAllMissionsNeedingUnloading(settlement).size();
	            numVehicles += UnloadVehicleEVA.getNonMissionVehiclesNeedingUnloading(settlement).size();
	            result = 100D * numVehicles;
	        }
	        catch (Exception e) {
	            logger.log(Level.SEVERE,"Error finding unloading missions. " + e.getMessage());
	        }
	
	        if (result <= 0) result = 0;
	        
	        // Crowded settlement modifier
	        if (settlement.getIndoorPeopleCount() > settlement.getPopulationCapacity()) {
	            result *= 2D;
	        }
	
	        // Settlement factor
	        result *= settlement.getGoodsManager().getTransportationFactor();
	        
	        result = applyPersonModifier(result, person);
	
	    	if (exposed[0]) {
				result = result/3D;// Baseline can give a fair amount dose of radiation
			}
	
	    	if (exposed[1]) {// GCR can give nearly lethal dose of radiation
				result = result/6D;
			}
	
	        if (result < 0) result = 0;

        }
        
        return result;
    }
}
