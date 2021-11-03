/*
 * Mars Simulation Project
 * Maintenance.java
 * @date 2021-10-21
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.environment.MarsSurface;
import org.mars_sim.msp.core.malfunction.MalfunctionFactory;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.person.ai.task.utils.Worker;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.StatusType;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * The Maintenance class is a task for performing preventive maintenance on
 * vehicles, settlements and equipment.
 */
public class Maintenance extends Task implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final Logger logger = Logger.getLogger(Maintenance.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.maintenance"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase MAINTAIN = new TaskPhase(Msg.getString("Task.phase.maintain")); //$NON-NLS-1$

	// Static members
	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = .1D;

	// Data members
	/** Entity to be maintained. */
	private Malfunctionable entity;

	/**
	 * Constructor.
	 * 
	 * @param person the person to perform the task
	 */
	public Maintenance(Person person) {
		super(NAME, person, true, false, STRESS_MODIFIER, SkillType.MECHANICS, 100D,
				10D + RandomUtil.getRandomDouble(40D));

		if (person.isOutside()) {
			endTask();
			return;
		}
		
		try {
			entity = getMaintenanceMalfunctionable();
			
			if (entity != null) {
				
				if (isInhabitableBuilding(entity)) {
					// Walk to random location in building.
					walkToRandomLocInBuilding((Building) entity, false);
					
				} else if (isVehicleMalfunction(entity)) {

					if (person.isInVehicle()) {
						// If person is in rover, walk to passenger activity spot.
						if (person.getVehicle() instanceof Rover) {
							walkToPassengerActivitySpotInRover((Rover) person.getVehicle(), false);
						}
					} else {
						// Walk to random location.
						walkToRandomLocation(true);
					}
				}
				
				// Initialize phase.
				addPhase(MAINTAIN);
				setPhase(MAINTAIN);			
			}
			
			else {
				endTask();
				return;
			}
			
		} catch (Exception e) {
			logger.log(Level.SEVERE, person + " was unable to perform maintenance.", e);
			endTask();
			return;
		}
	}

	public Maintenance(Robot robot) {
		super(NAME, robot, true, false, STRESS_MODIFIER, SkillType.MECHANICS, 100D,
				10D + RandomUtil.getRandomDouble(40D));

		if (robot.isOutside()) {
			endTask();
			return;
		}
		
		try {
			entity = getMaintenanceMalfunctionable();
			if (entity != null) {
				
				if (isInhabitableBuilding(entity)) {
					// Walk to random location in building.
					walkToRandomLocInBuilding((Building) entity, false);
					
				} else if (isVehicleMalfunction(entity)) {
					
					if (!robot.isInVehicle()) {
						// Walk to random location.
						walkToRandomLocation(false);
					}
				}
				
				// Initialize phase.
				addPhase(MAINTAIN);
				setPhase(MAINTAIN);
				
			} else {
				endTask();
				return;
			}
		} catch (Exception e) {
//			logger.log(Level.SEVERE, robot + " is unable to perform maintenance.", e);
			endTask();
			return;
		}
	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (MAINTAIN.equals(getPhase())) {
			return maintainPhase(time);
		} else {
			return time;
		}
	}

	/**
	 * Performs the maintain phase.
	 * 
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double maintainPhase(double time) {
		MalfunctionManager manager = entity.getMalfunctionManager();

		// If worker is incapacitated, end task.
		if (worker.getPerformanceRating() == 0D) {
			endTask();
		}

		// Check if maintenance has already been completed.
		if (manager.getEffectiveTimeSinceLastMaintenance() < 1000D) {
			endTask();
		}

		// If equipment has malfunction, end task.
		if (manager.hasMalfunction()) {
			endTask();
		}

		if (isDone()) {
			endTask();
			return time;
		}

		// Determine effective work time based on "Mechanic" skill.
		double workTime = time;
		int mechanicSkill = getEffectiveSkillLevel();
		if (mechanicSkill == 0) {
			workTime /= 2;
		}
		if (mechanicSkill > 1) {
			workTime += workTime * (.4D * mechanicSkill);
		}

		// Add repair parts if necessary.
		boolean repairParts = false;
		Unit containerUnit = worker.getTopContainerUnit();

		if (!(containerUnit instanceof MarsSurface)) {
//			Inventory inv = containerUnit.getInventory();
			if (Maintenance.hasMaintenanceParts(containerUnit, entity)) {
				repairParts = true;
				Map<Integer, Integer> parts = new HashMap<>(manager.getMaintenanceParts());
				Iterator<Integer> j = parts.keySet().iterator();
				
				if (containerUnit.getUnitType() == UnitType.SETTLEMENT) {
					
					while (j.hasNext()) {
						Integer part = j.next();
						int number = parts.get(part);
						((Settlement)containerUnit).retrieveItemResource(part, number);
						manager.maintainWithParts(part, number);
						
						// Add tracking item demand
//						containerUnit.getInventory().addItemDemandTotalRequest(part, number);
//						containerUnit.getInventory().addItemDemand(part, number);
					}
				}
				else {
					while (j.hasNext()) {
						Integer part = j.next();
						int number = parts.get(part);
						((Vehicle)containerUnit).retrieveItemResource(part, number);
						manager.maintainWithParts(part, number);
						
						// Add tracking item demand
//						inv.addItemDemandTotalRequest(part, number);
//						inv.addItemDemand(part, number);
					}
				}
				
			}
		}
		
		if (!repairParts) {
			endTask();
			return time;
		}

		// Add work to the maintenance
		manager.addMaintenanceWorkTime(workTime);

		// Add experience points
		addExperience(time);

		// If maintenance is complete, task is done.
		if (manager.getEffectiveTimeSinceLastMaintenance() == 0D) {
			endTask();
		}

		// Check if an accident happens during maintenance.
		checkForAccident(entity, time, 0.005D); 

		return 0D;
	}

	/**
	 * Gets the entity the person is maintaining. Returns null if none.
	 * 
	 * @return entity
	 */
	public Malfunctionable getEntity() {
		return entity;
	}

	/**
	 * Gets a random malfunctionable to perform maintenance on.
	 * 
	 * @return malfunctionable or null.
	 */
	private Malfunctionable getMaintenanceMalfunctionable() {
		Malfunctionable result = null;

		// Determine all malfunctionables local to the worker.
		Map<Malfunctionable, Double> malfunctionables = new HashMap<Malfunctionable, Double>();
		Iterator<Malfunctionable> i = MalfunctionFactory.getLocalMalfunctionables(worker).iterator();
		while (i.hasNext()) {
			Malfunctionable entity = i.next();
			double probability = getProbabilityWeight(entity);
			if (probability > 0D) {
				malfunctionables.put(entity, probability);
			}
		}

		if (!malfunctionables.isEmpty()) {
			result = RandomUtil.getWeightedRandomObject(malfunctionables);
		}

		if (result != null) {
			setDescription(Msg.getString("Task.description.maintenance.detail", result.getNickName())); // $NON-NLS-1$
		}

		return result;
	}

	/**
	 * Checks if a malfunctionable is an inhabitable building.
	 * 
	 * @param malfunctionable the malfunctionable.
	 * @return true if inhabitable building.
	 */
	private boolean isInhabitableBuilding(Malfunctionable malfunctionable) {
		boolean result = false;
		if (malfunctionable instanceof Building) {
			Building building = (Building) malfunctionable;
			if (building.hasFunction(FunctionType.LIFE_SUPPORT)) {
				result = true;
			}
		}
		return result;
	}

	/**
	 * Checks if a malfunctionable is a vehicle.
	 * 
	 * @param malfunctionable the malfunctionable.
	 * @return true if it's a vehicle.
	 */
	private boolean isVehicleMalfunction(Malfunctionable malfunctionable) {
		boolean result = false;
		if (malfunctionable instanceof Vehicle) {
			Vehicle v = (Vehicle) malfunctionable;
			if (v.haveStatusType(StatusType.MALFUNCTION)) {
				result = true;
			}
		}
		return result;
	}
	
	/**
	 * Gets the probability weight for a malfunctionable.
	 * 
	 * @param malfunctionable the malfunctionable
	 * @return the probability weight.
	 * @throws Exception if error determining probability weight.
	 */
	private double getProbabilityWeight(Malfunctionable malfunctionable) {
		double result = 0D;
		
		boolean isVehicle = (entity instanceof Vehicle);
		if (isVehicle)
			return 0;
		
		boolean uninhabitableBuilding = false;
		if (entity instanceof Building) {
			uninhabitableBuilding = !((Building) entity).hasFunction(FunctionType.LIFE_SUPPORT);
		}
		if (uninhabitableBuilding)
			return 0;
		
		MalfunctionManager manager = entity.getMalfunctionManager();
		boolean hasMalfunction = manager.hasMalfunction();
		if (hasMalfunction) {
			return 0;
		}

		boolean hasParts = hasMaintenanceParts(worker, malfunctionable);
		if (!hasParts) {
			return 0;
		}
		
		double effectiveTime = manager.getEffectiveTimeSinceLastMaintenance();
		boolean minTime = (effectiveTime >= 1000D);
		
		if (minTime) {
			result = effectiveTime;
		}
		
		if (malfunctionable instanceof Building) {
			Building building = (Building) malfunctionable;
			if (isInhabitableBuilding(malfunctionable)) {
				if (person != null) {
					result *= Task.getCrowdingProbabilityModifier(person, building);
					result *= Task.getRelationshipModifier(person, building);
				}
				else {
					result = 2 * result;
				}
			}
		}

		return result;
	}

	/**
	 * Checks if there are enough local parts to perform maintenance.
	 * 
	 * @param worker          the person performing the maintenance.
	 * @param malfunctionable the entity needing maintenance.
	 * @return true if enough parts.
	 * @throws Exception if error checking parts availability.
	 */
	public static boolean hasMaintenanceParts(Worker worker, Malfunctionable malfunctionable) {
		Unit unit = null;
		
		if (worker.isInSettlement()) 
			// This is also the case when the person is in a garage
			unit = worker.getSettlement();
		
		else if (worker.isRightOutsideSettlement())
			unit = worker.getNearbySettlement();
		else if (worker.isInVehicle())
			unit = worker.getVehicle();
			
		return hasMaintenanceParts(unit, malfunctionable);
	}

	/**
	 * Checks if there are enough local parts to perform maintenance.
	 * 
	 * @param settlement the settlement holding the needed parts.
	 * @param malfunctionable the entity needing maintenance.
	 * @return true if enough parts.
	 */
	public static boolean hasMaintenanceParts(Settlement settlement, Malfunctionable malfunctionable) {
		boolean result = true;
		
		Map<Integer, Integer> parts = malfunctionable.getMalfunctionManager().getMaintenanceParts();
		Iterator<Integer> i = parts.keySet().iterator();
		while (i.hasNext()) {
			Integer part = i.next();
			int number = parts.get(part);
			if (settlement.getItemResourceStored(part) < number) {
				result = false;
				// Boosts the item demand
//				unit.getInventory().addItemDemand(part, number);
			}
		}
		
		return result;
	}
	
	/**
	 * Checks if there are enough local parts to perform maintenance.
	 * 
	 * @param unit the unit holding the needed parts.
	 * @param malfunctionable the entity needing maintenance.
	 * @return true if enough parts.
	 * @throws Exception if error checking parts availability.
	 */
	static boolean hasMaintenanceParts(Unit unit, Malfunctionable malfunctionable) {
		boolean result = true;

		if (unit.getUnitType() == UnitType.SETTLEMENT) {
			return hasMaintenanceParts((Settlement)unit, malfunctionable);
		}
		else {
			Map<Integer, Integer> parts = malfunctionable.getMalfunctionManager().getMaintenanceParts();
			Iterator<Integer> i = parts.keySet().iterator();
			while (i.hasNext()) {
				Integer part = i.next();
				int number = parts.get(part);
				if (((Vehicle)unit).getItemResourceStored(part) < number) {
					result = false;
					// Boosts the item demand
//					unit.addItemDemand(part, number);
				}
			}
		}
		

		return result;
	}
}
