/*
 * Mars Simulation Project
 * ConsolidateContainersMeta.java
 * @date 2023-05-01
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.task.meta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mars_sim.core.data.RatingScore;
import com.mars_sim.core.equipment.Container;
import com.mars_sim.core.equipment.Equipment;
import com.mars_sim.core.equipment.EquipmentOwner;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.fav.FavoriteType;
import com.mars_sim.core.person.ai.task.ConsolidateContainers;
import com.mars_sim.core.person.ai.task.util.FactoryMetaTask;
import com.mars_sim.core.person.ai.task.util.SettlementMetaTask;
import com.mars_sim.core.person.ai.task.util.SettlementTask;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskJob;
import com.mars_sim.core.person.ai.task.util.TaskUtil;
import com.mars_sim.core.person.ai.task.util.TaskTrait;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.robot.RobotType;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.tool.Msg;

/**
 * Meta task for the ConsolidateContainers task to create shared SettlementTask
 * and individual Person tasks when they are in a Vehicle.
 */
public class ConsolidateContainersMeta extends FactoryMetaTask implements SettlementMetaTask {
    private static class ConsolidateTaskJob extends SettlementTask {
		
		private static final long serialVersionUID = 1L;

		public ConsolidateTaskJob(SettlementMetaTask owner, RatingScore score) {
			super(owner, "Consolidate Containers", null, score);
		}

		@Override
		public Task createTask(Person person) {
			return new ConsolidateContainers(person);
		}

		@Override
		public Task createTask(Robot robot) {
			return new ConsolidateContainers(robot);
		}
	}

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.consolidateContainers"); //$NON-NLS-1$

    private static final double DEFAULT_SCORE = 100;
    
    public ConsolidateContainersMeta() {
		super(NAME, WorkerType.BOTH, TaskScope.WORK_HOUR);
		
		setFavorite(FavoriteType.OPERATION, FavoriteType.TINKERING);
		setTrait(TaskTrait.STRENGTH, TaskTrait.ORGANIZATION, TaskTrait.DISCIPLINE);
		addPreferredRobot(RobotType.MEDICBOT);
        addPreferredRobot(RobotType.DELIVERYBOT);
	}

		
	/**
	 * Gets a list of Task Jobs.
	 * 
	 * Note: Person do not get any individual consolidate assigned as they never go in a Vehicle
	 */
    @Override
    public List<TaskJob> getTaskJobs(Person person) {
        List<TaskJob> result = Collections.emptyList();
        if (person.isInVehicle() && person.isInside() &&
                        needsConsolidation(person.getVehicle(), false)) {
            // Create a real list
            result = new ArrayList<>();
            RatingScore score = new RatingScore(DEFAULT_SCORE);
            assessPersonSuitability(score, person);
            result.add(new ConsolidateTaskJob(this, score));
        }
        return result;
	}
	

	/**
	 *  Gets a list of Task Jobs.
	 *  
	 *  Note: Robots do not get any individual consolidate container assigned as they never go in a Vehicle
	 */
    @Override
    public List<TaskJob> getTaskJobs(Robot robot) {
		return Collections.emptyList();
	}
	
    /**
     * Creates a task if any containers can be consolidated in the Settlement.
     * 
     * @param settlement Source of Containers
     */
    @Override
    public List<SettlementTask> getSettlementTasks(Settlement settlement) {
        List<SettlementTask> result = Collections.emptyList();
        if (needsConsolidation(settlement, true)) {
            // Create a real list
            result = new ArrayList<>();
            result.add(new ConsolidateTaskJob(this, new RatingScore(DEFAULT_SCORE)));
        }
        return result;
    }

    /**
     * Score modifier for a Robot is based on it's performance rating
     */
    @Override
    public RatingScore assessRobotSuitability(SettlementTask t, Robot r) {
        return TaskUtil.assessRobot(t, r);
    }

    /**
     * Consolidates the container's resources.
     * 
     * @param topContainer
     * @param useTopInventory
     * @return
     */
    private static boolean needsConsolidation(EquipmentOwner topContainer, boolean useTopInventory) {   	        
        int partialContainers = 0;
                
        // Note: if in a vehicle, do not use main store. keep resources in containers
        for (Equipment e: topContainer.getContainerSet()) {
        	Container c = (Container)e;
            if (!e.isEmpty(false)) {
                // Only check one type of amount resource for container.
                int resource = c.getResource();
                // Check if this resource from this container could be loaded into the settlement/vehicle's inventory.
                if (useTopInventory && (resource > 0) 
                		&& topContainer.hasAmountResourceRemainingCapacity(resource)) {
                	return true;
                }

                // Check if container is only partially full of resource.
                if (c.hasAmountResourceRemainingCapacity(resource)) {
                    // If another container is also partially full of resource, they can be consolidated.
                	partialContainers++;
                    if (partialContainers > 2) {
                    	// Need at least 3 containers
                        return true;
                    }
                }
            }
        }
    	
    	return false;
    }
}