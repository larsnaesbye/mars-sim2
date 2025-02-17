/*
 * Mars Simulation Project
 * ToggleResourceProcess.java
 * @date 2022-09-12
 * @author Scott Davis
 */
package com.mars_sim.core.resourceprocess.task;

import java.util.Objects;
import java.util.Set;

import com.mars_sim.core.UnitType;
import com.mars_sim.core.data.UnitSet;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskPhase;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.resourceprocess.ResourceProcess;
import com.mars_sim.core.resourceprocess.ResourceProcessSpec;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.ResourceProcessor;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.core.tool.RandomUtil;

/**
 * The ToggleResourceProcess class is an EVA task for toggling a particular
 * automated resource process on or off.
 */
public class ToggleResourceProcess extends Task {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(ToggleResourceProcess.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.toggleResourceProcess"); //$NON-NLS-1$
	private static final String TOGGLE_ON = Msg.getString("Task.description.toggleResourceProcess.on"); //$NON-NLS-1$
	private static final String TOGGLE_OFF = Msg.getString("Task.description.toggleResourceProcess.off"); //$NON-NLS-1$

	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = .25D;

	/** Task phases. */
	private static final TaskPhase TOGGLING = new TaskPhase(Msg.getString("Task.phase.toggleResourceProcess.toggling")); //$NON-NLS-1$
	private static final TaskPhase FINISHED = new TaskPhase(Msg.getString("Task.phase.toggleResourceProcess.finished")); //$NON-NLS-1$

	private static final String OFF = "off";
	private static final String ON = "on";

	// Data members
	/** True if the finished phase of the process has been completed. */
	private boolean isFinished = false;

	/** The resource process to be toggled. */
	private ResourceProcess process;
	/** The building the resource process is in. */
	private Building resourceProcessBuilding;

	/**
	 * Turn a process off
	 *
	 * @param worker the worker performing the task.
	 */
	public ToggleResourceProcess(Worker worker, Building processBuilding, ResourceProcess process) {
		super(NAME, worker, true, false, STRESS_MODIFIER, SkillType.MECHANICS, 100D, 20D);
		if (!worker.isInSettlement()) {
			clearTask("Not in Settlement.");
			return;
		}

		this.resourceProcessBuilding = processBuilding;
		this.process = process;
		prepResourceProcess();
	}

	/**
	 * Turn a process On
	 *
	 * @param worker the worker performing the task.
	 * @param processSpec Process to turn on
	 */
	public ToggleResourceProcess(Worker worker, boolean useWaste, ResourceProcessSpec process) {
		super(NAME, worker, true, false, STRESS_MODIFIER, SkillType.MECHANICS, 100D, 20D);
		if (!worker.isInSettlement()) {
			clearTask("Not in Settlement.");
			return;
		}

		if (!selectResourceProcess(worker.getSettlement(), useWaste, process)) {
			clearTask("No process to toggle on.");
			return;
		}
		prepResourceProcess();
	}

	private static record PotentialProcess(Building building, ResourceProcess process) {}
	
	/**
	 * Create a potential record if the processor can start the selected processSpec
	 */
	private static PotentialProcess createPotential(ResourceProcessor processor,
			ResourceProcessSpec processSpec) {
		for(var p : processor.getProcesses()) {
			if (p.getSpec().equals(processSpec) && p.canToggle()
				&& !p.isWorkerAssigned() && !p.isProcessRunning()) {
				return new PotentialProcess(processor.getBuilding(), p);
			}
		}
		return null;
	}

	/**
	 * Select a suitable resource process to toggle on for a Settlement.
	 * 
	 * @param s the Settlement.
	 * @param useWaste true if the process is for waste.
	 * @param processSpec the process specification.
	 */
	private boolean selectResourceProcess(Settlement s, boolean useWaste, ResourceProcessSpec processSpec) {
		// Create a set of potential processes to toggle on
		var potentials = s.getBuildingManager().getBuildingSet((useWaste ? FunctionType.WASTE_PROCESSING
									: FunctionType.RESOURCE_PROCESSING)).stream()
					.map(b -> (useWaste ? b.getWasteProcessing() : b.getResourceProcessing()))
					.map(rp -> createPotential(rp, processSpec))
					.filter(Objects::nonNull)
					.toList();

		var selected = RandomUtil.getRandomElement(potentials);
		if (selected == null) {
			clearTask("No process to toggle on.");
			return false;
		}
		this.process = selected.process;
		this.resourceProcessBuilding = selected.building;

		return true;
	}

	private void prepResourceProcess() {
		if (process.isWorkerAssigned()) {
			clearTask("Process toggle already active with someone else.");
			return;
		}
		else if (!process.canToggle()) {
			clearTask("Process already completed toggled.");
			return;
		}

		if (process.isProcessRunning()) {
			setName(TOGGLE_OFF);
			setDescription(TOGGLE_OFF);
			logger.fine(resourceProcessBuilding, process + " : " + worker + " made an attempt to toggle it off.");
		} else {
			setDescription(TOGGLE_ON);
			logger.fine(resourceProcessBuilding, process + " : " + worker + " made an attempt to toggle it on.");
		}

		if (resourceProcessBuilding.hasFunction(FunctionType.LIFE_SUPPORT))
			walkToResourceBldg(resourceProcessBuilding);
		else
			// Looks for management function for toggling resource process.
			checkManagement();

		addPhase(TOGGLING);
		addPhase(FINISHED);

		setPhase(TOGGLING);
		process.setWorkerAssigned(true);
	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (TOGGLING.equals(getPhase())) {
			return togglingPhase(time);
		} else if (FINISHED.equals(getPhase())) {
			return finishedPhase(time);
		} else {
			return time;
		}
	}

	/**
	 * Performs the toggle process phase.
	 *
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	private double togglingPhase(double time) {
		double workTime = time;

		// Determine effective work time based on "Mechanic" skill.
		int mechanicSkill = getEffectiveSkillLevel();
		if (mechanicSkill == 0) {
			workTime /= 2;
		}
		if (mechanicSkill > 1) {
			workTime += workTime * (.2D * mechanicSkill);
		}

		if (worker.getUnitType() == UnitType.PERSON) {
			double perf = worker.getPerformanceRating();
			// If worker is incapacitated, enter airlock.
			if (perf == 0D) {
				// reset it to 10% so that he can walk inside
				person.getPhysicalCondition().setPerformanceFactor(.1);
				clearTask(": poor performance in " + process.getProcessName() + ".");
			}

		} else {
			workTime /= 2;
		}

		// Add experience points
		addExperience(time);

		// Add work to the toggle process.
		if (process.addToggleWorkTime(workTime)) {
			setPhase(FINISHED);
		} else {
			double remainingTime = process.getRemainingToggleWorkTime();
			if (getDuration() < remainingTime + time * 2) {
				// Add two more frames and the remaining time to the task duration
				setDuration(remainingTime + time * 2 + getDuration());
			}
		}

		// Check if an accident happens during the manual toggling.
		if (resourceProcessBuilding.hasFunction(FunctionType.LIFE_SUPPORT)) {
			checkForAccident(resourceProcessBuilding, time, 0.002);
		}

		return 0;
	}

	/**
	 * Performs the finished phase.
	 *
	 * @param time the amount of time (millisols) to perform the phase.
	 * @return the amount of time (millisols) left over after performing the phase.
	 */
	protected double finishedPhase(double time) {

		if (!isFinished) {			
			// COmplete toggling with stop the process automatically
			String toggle = (process.isProcessRunning() ? ON : OFF);
			if (resourceProcessBuilding.hasFunction(FunctionType.LIFE_SUPPORT))
				logger.fine(resourceProcessBuilding, process + " : " + worker
						+ " just toggled it " + toggle + " manually.");
			else
				logger.fine(resourceProcessBuilding, process + " : " + worker
						+ " just toggled it " + toggle + " remotely.");

			// Only need to run the finished phase once and for all
			isFinished = true;

			endTask();
		}

		return 0D;
	}

	/**
	 * Checks if any management function is available.
	 */
	private void checkManagement() {

		boolean done = false;
		// Pick an administrative building for remote access to the resource building
		Set<Building> mgtBuildings = worker.getSettlement().getBuildingManager()
				.getBuildingSet(FunctionType.MANAGEMENT);

		if (!mgtBuildings.isEmpty()) {

			Set<Building> notFull = new UnitSet<>();

			for (Building b : mgtBuildings) {
				if (b.hasFunction(FunctionType.ADMINISTRATION)) {
					walkToMgtBldg(b);
					done = true;
					break;
				} else if (b.getManagement() != null && !b.getManagement().isFull()) {
					notFull.add(b);
				}
			}

			if (!done) {
				if (!notFull.isEmpty()) {
					walkToMgtBldg(RandomUtil.getARandSet(mgtBuildings));
				} else {
					clearTask(process.getProcessName() + ": Management space unavailable.");
				}
			}
		} else {
			clearTask("Management space unavailable.");
		}
	}
	
	/**
	 * This method is part of the Task Life Cycle. It is called once
	 * and only once per Task when it is ended. Release the flag on the Resoruceprocess as the Task has ended.
	 * Subclasses should override to receive callback when the Task is ending.
	 */
	@Override
	protected void clearDown() {
		if (process != null) {
			process.setWorkerAssigned(false);
		}
		super.clearDown();
	}

	/**
	 * Walks to the building with resource processing function.
	 *
	 * @param building
	 */
	private void walkToResourceBldg(Building building) {
		walkToTaskSpecificActivitySpotInBuilding(building,
				FunctionType.RESOURCE_PROCESSING,
				false);
	}

	/**
	 * Walks to the building with management function.
	 *
	 * @param building
	 */
	private void walkToMgtBldg(Building building) {
		walkToTaskSpecificActivitySpotInBuilding(building,
				FunctionType.MANAGEMENT,
				false);
	}

	ResourceProcess getResourceProcess() {
		return process;
	}

	Building getBuilding() {
		return resourceProcessBuilding;
	}
}
