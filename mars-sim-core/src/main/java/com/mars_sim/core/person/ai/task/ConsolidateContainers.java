/*
 * Mars Simulation Project
 * ConsolidateContainers.java
 * @date 2023-05-17
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.task;

import java.util.Iterator;

import com.mars_sim.core.equipment.Container;
import com.mars_sim.core.equipment.Equipment;
import com.mars_sim.core.equipment.EquipmentOwner;
import com.mars_sim.core.equipment.ResourceHolder;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.NaturalAttributeType;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskPhase;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.core.vehicle.Rover;

/** 
 * A task for consolidating the resources stored in local containers.
 */
public class ConsolidateContainers 
extends Task {

    /** default serial id. */
    private static final long serialVersionUID = 1L;
    
    /** default logger. */
    private static SimLogger logger = SimLogger.getLogger(ConsolidateContainers.class.getName());
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.consolidateContainers"); //$NON-NLS-1$
    
    /** Task phases. */
    private static final TaskPhase CONSOLIDATING = new TaskPhase(Msg.getString(
            "Task.phase.consolidating")); //$NON-NLS-1$
    
    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = -.1D;
    
    /** The amount of resources (kg) a worker of average strength can load per millisol. */
    private static final double LOAD_RATE = 20D;
    
    /** Time (millisols) duration. */
    private static final double DURATION = 30D;
    
    /**
     * Constructor 1.
     * 
     * @param person the person performing the task.
     * @throws Exception if error constructing task.
     */
    public ConsolidateContainers(Person person) {
        // Use Task constructor
        super(NAME, person, true, false, STRESS_MODIFIER, DURATION);
                
        if (person.isOutside()) {
        	endTask();
        	return;
        }
        
        else if (person.isInVehicle()) {
            // If person is in rover, walk to passenger activity spot.
            if (person.getVehicle() instanceof Rover) {
                walkToPassengerActivitySpotInRover((Rover) person.getVehicle(), true);
            }
        }
        
        else if (person.isInSettlement()) {
        	Building storage = person.getSettlement().getBuildingManager().getABuilding(FunctionType.STORAGE);
        	walkToActivitySpotInBuilding(storage, FunctionType.STORAGE, true);
        }
        
        else {
            logger.severe(person, "Not in a proper location for consolidating containers.");
            endTask();
        }
        
        // Add task phase
        addPhase(CONSOLIDATING);
        setPhase(CONSOLIDATING);
    }
    
    /**
     * Constructor 2.
     * 
     * @param robot the robot performing the task.
     * @throws Exception if error constructing task.
     */
    public ConsolidateContainers(Robot robot) {
        // Use Task constructor
        super(NAME, robot, true, false, STRESS_MODIFIER, DURATION);
        
        if (robot.isOutside()) {
        	endTask();
        	return;
        }
        
        if (robot.isInVehicle()) {
            // If robot is in rover, walk to passenger activity spot.
            if (robot.getVehicle() instanceof Rover) {
                walkToPassengerActivitySpotInRover((Rover) robot.getVehicle(), true);
            }
        }
        else if (robot.isInSettlement()) {
        	Building storage = robot.getSettlement().getBuildingManager().getABuilding(FunctionType.STORAGE);
        	walkToActivitySpotInBuilding(storage, FunctionType.STORAGE, true);
        }
        
        else {
            logger.severe(robot, "Not in a proper location for consolidating containers");
            endTask();
        }
        
        // Add task phase
        addPhase(CONSOLIDATING);
        setPhase(CONSOLIDATING);
    }    
    
    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (CONSOLIDATING.equals(getPhase())) {
            return consolidatingPhase(time);
        }
        else {
            return time;
        }
    }
    
    /**
     * Performs the consolidating phase.
     * 
     * @param time the amount of time (millisol) to perform the consolidating phase.
     * @return the amount of time (millisol) left after performing the consolidating phase.
     */
    private double consolidatingPhase(double time) {
    	EquipmentOwner parent = (EquipmentOwner) worker.getTopContainerUnit();
    	boolean useTopInventory = worker.isInSettlement();
    	
        // Determine consolidation load rate.
    	int strength = worker.getNaturalAttributeManager().getAttribute(NaturalAttributeType.STRENGTH);	
    	
        double strengthModifier = .1D + (strength * .018D);
        double totalAmountLoading = LOAD_RATE * strengthModifier * time;
        double remainingAmountLoading = totalAmountLoading;
        
        // Go through each container in top inventory.   
        for (Equipment source: parent.getContainerSet()) {
            Container c = (Container)source; 	
        	
        	int resourceID = c.getResource();
            if (resourceID != -1) {
            	// resourceID = -1 means the container has not been initialized
                double sourceAmount = source.getAmountResourceStored(resourceID);
                if (sourceAmount > 0D) {
	                // Move resource in container to top inventory if possible.
	                double topRemainingCapacity = parent.getAmountResourceRemainingCapacity(resourceID);
	                if (useTopInventory && (topRemainingCapacity >= 0D)) {
                        double loadAmount = transferResource(c, sourceAmount, resourceID,
                                                             topRemainingCapacity,
                                                             parent, topRemainingCapacity);
	                   
	                    remainingAmountLoading -= loadAmount;
	                    sourceAmount -= loadAmount;
	                    if (remainingAmountLoading <= 0D) {
	                    	return 0D;
	                    }
	                }
	                
	                // Check if container is empty.
	                if (sourceAmount > 0D) {
	                    // Go through each other container in top inventory and try to consolidate resource.
	                    Iterator<Equipment> k = parent.getContainerSet().iterator();
	                    while (k.hasNext() && (remainingAmountLoading > 0D) && (sourceAmount > 0D)) {
	                    	Container otherUnit = (Container)k.next();
	                        if (otherUnit != source) { // && otherUnit instanceof Container) {
	                            double otherAmount = otherUnit.getAmountResourceStored(resourceID);
	                            if (otherAmount > 0D) {
	                                double otherRemainingCapacity = otherUnit.getAmountResourceRemainingCapacity(resourceID);
	                                if (otherRemainingCapacity >= 0D) {
                                        double loadAmount = transferResource(c, sourceAmount, resourceID,
                                                                             remainingAmountLoading,
                                                                             otherUnit, otherRemainingCapacity);

	                                    remainingAmountLoading -= loadAmount;
	                                    sourceAmount -= loadAmount;
	            	                    if (remainingAmountLoading <= 0D) {
                                            return 0D;
	            	                    }
	                                }
	                            }
	                        }
	                    }
	                }
                }
            }
        }
        
        double remainingTime = (remainingAmountLoading / totalAmountLoading) * time;
        
        // If nothing has been loaded, end task.
        if (remainingAmountLoading == totalAmountLoading) {
            endTask();
        }
        
        return remainingTime;
    }

    /**
     * Transfer resource from a Container into a parent holder. 
     * @param source Source container
     * @param sourceAmount Amount availble in the source
     * @param sourceResource Resource being transferred
     * @param transferAmount Maximum amount to be transferred
     * @param target Target resource holder
     * @param targetCapacity Capacity in the target
     * @return
     */
    private static double transferResource(Container source, double sourceAmount, int sourceResource,
                                           double transferAmount,
                                           ResourceHolder target, double targetCapacity) {
       double loadAmount = targetCapacity;
        if (loadAmount > sourceAmount) {
            loadAmount = sourceAmount;
        }
        
        if (loadAmount > transferAmount) {
            loadAmount = transferAmount;
        }
        
        source.retrieveAmountResource(sourceResource, loadAmount);
        if (source.getStoredMass() == 0) {
            source.clean();
        }
        target.storeAmountResource(sourceResource, loadAmount);
        return loadAmount;
    }
}
