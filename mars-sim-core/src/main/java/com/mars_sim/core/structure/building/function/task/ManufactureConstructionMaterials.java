/*
 * Mars Simulation Project
 * ManufactureConstructionMaterials.java
 * @date 2024-06-09
 * @author Scott Davis
 */
package com.mars_sim.core.structure.building.function.task;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mars_sim.core.UnitType;
import com.mars_sim.core.data.UnitSet;
import com.mars_sim.core.malfunction.Malfunctionable;
import com.mars_sim.core.manufacture.ManufactureProcess;
import com.mars_sim.core.manufacture.ManufactureProcessInfo;
import com.mars_sim.core.manufacture.ManufactureUtil;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.NaturalAttributeType;
import com.mars_sim.core.person.ai.SkillManager;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.person.ai.task.util.ExperienceImpact;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskPhase;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.process.ProcessItem;
import com.mars_sim.core.resource.ItemResourceUtil;
import com.mars_sim.core.resource.ItemType;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.structure.OverrideType;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.Manufacture;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.core.tool.RandomUtil;

/**
 * A task for working on a manufacturing process to produce construction
 * materials.
 */
public class ManufactureConstructionMaterials extends Task {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.manufactureConstructionMaterials"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase MANUFACTURE = new TaskPhase(Msg.getString("Task.phase.manufacture")); //$NON-NLS-1$

	/** Impact doing this Task */
	private static final ExperienceImpact IMPACT = new ExperienceImpact(50D,
										NaturalAttributeType.EXPERIENCE_APTITUDE, false, .1D,
										SkillType.CONSTRUCTION, SkillType.MATERIALS_SCIENCE);

	// Data members
	/** The manufacturing workshop the person is using. */
	private Manufacture workshop;

	/**
	 * Constructor.
	 * 
	 * @param person the person to perform the task
	 */
	public ManufactureConstructionMaterials(Person person) {
		super(NAME, person, true, IMPACT, 25);

		// Initialize data members
		if (person.getSettlement() != null) {
			setDescription(NAME);
		} else {
			endTask();
		}

		// Get available manufacturing workshop if any.
		Building manufactureBuilding = getAvailableManufacturingBuilding(person);
		if (manufactureBuilding != null) {
			workshop = manufactureBuilding.getManufacture();

			// Walk to manufacturing building.
			walkToTaskSpecificActivitySpotInBuilding(manufactureBuilding, FunctionType.MANUFACTURE, false);
		} else {
			endTask();
		}

		// Initialize phase
		setPhase(MANUFACTURE);
	}

	/**
	 * Constructor.
	 * 
	 * @param person the person to perform the task
	 */
	public ManufactureConstructionMaterials(Robot robot) {
		super(NAME, robot, true, IMPACT, 10D + RandomUtil.getRandomDouble(50D));
		
		// Initialize data members
		if (robot.getSettlement() != null) {
			setDescription(NAME);
		} else {
			endTask();
		}

		// Get available manufacturing workshop if any.
		Building manufactureBuilding = getAvailableManufacturingBuilding(robot);
		if (manufactureBuilding != null) {
			workshop = manufactureBuilding.getManufacture();
			
			// Walk to manufacturing building.
			walkToTaskSpecificActivitySpotInBuilding(manufactureBuilding, FunctionType.MANUFACTURE, false);
		} else {
			endTask();
		}

		// Initialize phase
		setPhase(MANUFACTURE);
	}

	/**
	 * Gets an available manufacturing building that the person can use. Returns
	 * null if no manufacturing building is currently available.
	 * 
	 * @param person the person
	 * @return available manufacturing building
	 */
	public static Building getAvailableManufacturingBuilding(Worker worker) {

		Building result = null;

		SkillManager skillManager = worker.getSkillManager();
		
		// Note: Allow a low material science skill person to have access to 
		// do the next 2 levels of skill process or else difficult 
		// tasks are not learned.		
		int skill = skillManager.getEffectiveSkillLevel(SkillType.MATERIALS_SCIENCE);

		if (worker.getUnitType() == UnitType.PERSON) {
			skill = skill + 2;
		}
		
		if (worker.isInSettlement()) {
			BuildingManager manager = worker.getSettlement().getBuildingManager();
			Set<Building> manufacturingBuildings = manager.getBuildingSet(FunctionType.MANUFACTURE);
			manufacturingBuildings = BuildingManager.getNonMalfunctioningBuildings(manufacturingBuildings);
			manufacturingBuildings = getManufacturingBuildingsNeedingWork(manufacturingBuildings, skill);
			manufacturingBuildings = getBuildingsWithProcessesRequiringWork(manufacturingBuildings, skill);
			manufacturingBuildings = getHighestManufacturingTechLevelBuildings(manufacturingBuildings);
			manufacturingBuildings = BuildingManager.getLeastCrowdedBuildings(manufacturingBuildings);

			Map<Building, Double> manufacturingBuildingProbs = null;
			
			if (worker.getUnitType() == UnitType.PERSON) {
				manufacturingBuildingProbs = BuildingManager.getBestRelationshipBuildings((Person)worker,
						manufacturingBuildings);
			}
			
			if (manufacturingBuildingProbs == null) {
				return null;
			}
			
			else if (!manufacturingBuildingProbs.isEmpty()) {
				result = RandomUtil.getWeightedRandomObject(manufacturingBuildingProbs);
			}
		}

		return result;
	}
	

	/**
	 * Gets a list of manufacturing buildings needing work from a list of buildings
	 * with the manufacture function.
	 * 
	 * @param buildingList list of buildings with the manufacture function.
	 * @param skill        the materials science skill level of the person.
	 * @return list of manufacture buildings needing work.
	 */
	private static Set<Building> getManufacturingBuildingsNeedingWork(
			Set<Building> buildingList, int skill) {

		Set<Building> result = new UnitSet<>();

		Iterator<Building> i = buildingList.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			Manufacture manufacturingFunction = building.getManufacture(); 
			if (manufacturingFunction.requiresWork(skill)) {
				result.add(building);
			}
		}

		return result;
	}

	/**
	 * Gets a subset list of manufacturing buildings with processes requiring work.
	 * 
	 * @param buildingList the original building list.
	 * @param skill        the materials science skill level of the person.
	 * @return subset list of buildings with processes requiring work, or original
	 *         list if none found.
	 */
	private static Set<Building> getBuildingsWithProcessesRequiringWork(
			Set<Building> buildingList, int skill) {

		Set<Building> result = new UnitSet<>();

		// Add all buildings with processes requiring work.
		Iterator<Building> i = buildingList.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			if (hasProcessRequiringWork(building, skill)) {
				result.add(building);
			}
		}

		// If no building with processes requiring work, return original list.
		if (result.isEmpty()) {
			result = buildingList;
		}

		return result;
	}

	/**
	 * Checks if manufacturing building has any processes requiring work.
	 * 
	 * @param manufacturingBuilding the manufacturing building.
	 * @param skill                 the materials science skill level of the person.
	 * @return true if processes requiring work.
	 */
	public static boolean hasProcessRequiringWork(Building manufacturingBuilding, int skill) {

		boolean result = false;

		Manufacture manufacturingFunction = manufacturingBuilding.getManufacture();
		Iterator<ManufactureProcess> i = manufacturingFunction.getProcesses().iterator();
		while (i.hasNext()) {
			ManufactureProcess process = i.next();
			if (producesConstructionMaterials(process)) {
				boolean workRequired = (process.getWorkTimeRemaining() > 0D);
				boolean skillRequired = (process.getInfo().getSkillLevelRequired() <= skill);
				if (workRequired && skillRequired) {
					result = true;
				}
			}
		}

		return result;
	}

	/**
	 * Checks if a manufacture process produces construction materials.
	 * 
	 * @param process the manufacture process.
	 * @return true if produces construction materials.
	 */
	private static boolean producesConstructionMaterials(ManufactureProcess process) {
		return producesConstructionMaterials(process.getInfo());
	}

	/**
	 * Gets a subset list of manufacturing buildings with the highest tech level
	 * from a list of buildings with the manufacture function.
	 * 
	 * @param buildingList list of buildings with the manufacture function.
	 * @return subset list of highest tech level buildings.
	 */
	private static Set<Building> getHighestManufacturingTechLevelBuildings(Set<Building> buildingList) {

		Set<Building> result = new UnitSet<>();

		int highestTechLevel = 0;
		Iterator<Building> i = buildingList.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			Manufacture manufacturingFunction = building.getManufacture();
			if (manufacturingFunction.getTechLevel() > highestTechLevel) {
				highestTechLevel = manufacturingFunction.getTechLevel();
			}
		}

		Iterator<Building> j = buildingList.iterator();
		while (j.hasNext()) {
			Building building = j.next();
			Manufacture manufacturingFunction = building.getManufacture();
			if (manufacturingFunction.getTechLevel() == highestTechLevel) {
				result.add(building);
			}
		}

		return result;
	}

	/**
	 * Gets the highest manufacturing process goods value for the person and the
	 * manufacturing building.
	 * 
	 * @param person                the person to perform manufacturing.
	 * @param manufacturingBuilding the manufacturing building.
	 * @return highest process good value.
	 */
	public static double getHighestManufacturingProcessValue(Worker person, Building manufacturingBuilding) {

		double highestProcessValue = 0D;

		int skillLevel = person.getSkillManager().getEffectiveSkillLevel(SkillType.MATERIALS_SCIENCE);

		Manufacture manufacturingFunction = manufacturingBuilding.getManufacture();
		int techLevel = manufacturingFunction.getTechLevel();

		Iterator<ManufactureProcessInfo> i = ManufactureUtil
				.getManufactureProcessesForTechSkillLevel(techLevel, skillLevel).iterator();
		while (i.hasNext()) {
			ManufactureProcessInfo process = i.next();
			if ((ManufactureUtil.canProcessBeStarted(process, manufacturingFunction)
				|| isProcessRunning(process, manufacturingFunction)) 
				&& producesConstructionMaterials(process)) {
				Settlement settlement = manufacturingBuilding.getSettlement();
				double processValue = ManufactureUtil.getManufactureProcessValue(process, settlement);
				if (processValue > highestProcessValue) {
					highestProcessValue = processValue;
				}
			}
		}

		return highestProcessValue;
	}

	/**
	 * Checks if a manufacture process produces construction materials.
	 * 
	 * @param process the manufacture process.
	 * @return true if produces construction materials.
	 */
	private static boolean producesConstructionMaterials(ManufactureProcessInfo info) {
		boolean result = false;

		List<Integer> constructionResources = constructionConfig.determineConstructionResources();
		
		List<Integer> constructionParts = constructionConfig.determineConstructionParts();
		
		Iterator<ProcessItem> i = info.getOutputList().iterator();
		while (!result && i.hasNext()) {
			var item = i.next();
			if (ItemType.AMOUNT_RESOURCE.equals(item.getType())) {
				int resource = ResourceUtil.findIDbyAmountResourceName(item.getName());
				if (constructionResources.contains(resource)) {
					result = true;
				}
			} else if (ItemType.PART.equals(item.getType())) {
				int part = ItemResourceUtil.findIDbyItemResourceName(item.getName());
				if (constructionParts.contains(part)) {
					result = true;
				}
			}
		}

		return result;
	}


	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (MANUFACTURE.equals(getPhase())) {
			return manufacturePhase(time);
		} else {
			return time;
		}
	}

	/**
	 * Performs the manufacturing phase.
	 * 
	 * @param time the time to perform (millisols)
	 * @return remaining time after performing (millisols)
	 */
	private double manufacturePhase(double time) {

		if (worker.isOutside()) {
			endTask();
			return time;
		}
		
		// Check if workshop has malfunction.
		Malfunctionable entity = workshop.getBuilding();
		if (entity.getMalfunctionManager().hasMalfunction()) {
			endTask();
			return time;
		}

		// Determine amount of effective work time based on "Materials Science"
		// skill.
		double workTime = time;
		int skill = getEffectiveSkillLevel();
		if (skill == 0) {
			workTime /= 2;
		} else {
			workTime += workTime * (.2D * skill);
		}

		// Apply work time to manufacturing processes.
		while ((workTime > 0D) && !isDone()) {
			manufacture(workTime);
		}

		// Add experience
		addExperience(time);

		// Check for accident in workshop.
		checkForAccident(entity, time, 0.006);

		return 0D;
	}

	/**
	 * Executes the manufacture process.
	 * 
	 * @param workTime
	 */
	private void manufacture(double workTime) {
		ManufactureProcess process = getRunningManufactureProcess();
		if (process != null) {
			double remainingWorkTime = process.getWorkTimeRemaining();
			double providedWorkTime = workTime;
			if (providedWorkTime > remainingWorkTime) {
				providedWorkTime = remainingWorkTime;
			}
			process.addWorkTime(providedWorkTime);
			workTime -= providedWorkTime;

			if ((process.getWorkTimeRemaining() <= 0D) && (process.getProcessTimeRemaining() <= 0D)) {
				workshop.endManufacturingProcess(process, false);
			}
		} 
		else {
			if (!worker.getSettlement().getProcessOverride(OverrideType.MANUFACTURE))
				process = createNewManufactureProcess();
			
			if (process == null) {
				endTask();
			}
		}

		if (process == null) {
			// Prints description
			setDescription(Msg.getString("Task.description.manufactureConstructionMaterials.checking")); //$NON-NLS-1$
		}
	}
	
	
	
	/**
	 * Gets an available running manufacturing process.
	 * 
	 * @return process or null if none.
	 */
	private ManufactureProcess getRunningManufactureProcess() {
		ManufactureProcess result = null;

		int skillLevel = getEffectiveSkillLevel();

		Iterator<ManufactureProcess> i = workshop.getProcesses().iterator();
		while (i.hasNext() && (result == null)) {
			ManufactureProcess process = i.next();
			if ((process.getInfo().getSkillLevelRequired() <= skillLevel) && (process.getWorkTimeRemaining() > 0D)
					&& producesConstructionMaterials(process)) {
				result = process;
			}
		}

		return result;
	}

	/**
	 * Checks if a process type is currently running at a manufacturing building.
	 * 
	 * @param processInfo         the process type.
	 * @param manufactureBuilding the manufacturing building.
	 * @return true if process is running.
	 */
	private static boolean isProcessRunning(ManufactureProcessInfo processInfo, Manufacture manufactureBuilding) {
		boolean result = false;

		Iterator<ManufactureProcess> i = manufactureBuilding.getProcesses().iterator();
		while (i.hasNext()) {
			ManufactureProcess process = i.next();
			if (process.getInfo().getName().equals(processInfo.getName())) {
				result = true;
			}
		}

		return result;
	}

	/**
	 * Creates a new manufacturing process if possible.
	 * 
	 * @return the new manufacturing process or null if none.
	 */
	private ManufactureProcess createNewManufactureProcess() {
		ManufactureProcess result = null;

		if (workshop.getCurrentTotalProcesses() < workshop.getNumPrintersInUse()) {

			int skillLevel = getEffectiveSkillLevel();
			int techLevel = workshop.getTechLevel();

			// Determine all manufacturing processes that are possible and profitable.
			Map<ManufactureProcessInfo, Double> processProbMap = new HashMap<>();
			Iterator<ManufactureProcessInfo> i = ManufactureUtil
					.getManufactureProcessesForTechSkillLevel(techLevel, skillLevel).iterator();
			while (i.hasNext()) {
				ManufactureProcessInfo processInfo = i.next();

				if (ManufactureUtil.canProcessBeStarted(processInfo, workshop)
						&& producesConstructionMaterials(processInfo)) {
					double processValue = ManufactureUtil.getManufactureProcessValue(processInfo,
							worker.getSettlement());
					if (processValue > 0D) {
						processProbMap.put(processInfo, processValue);
					}
				}
			}

			// Randomly choose among possible manufacturing processes based on their
			// relative profitability.
			ManufactureProcessInfo chosenProcess = null;
			if (!processProbMap.isEmpty()) {
				chosenProcess = RandomUtil.getWeightedRandomObject(processProbMap);
			}

			// Create chosen manufacturing process.
			if (chosenProcess != null) {
				result = new ManufactureProcess(chosenProcess, workshop);
				workshop.addProcess(result);

			}
		}

		return result;
	}
}
