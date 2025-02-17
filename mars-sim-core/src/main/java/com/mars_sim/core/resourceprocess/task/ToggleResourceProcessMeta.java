/*
 * Mars Simulation Project
 * ToggleResourceProcessMeta.java
 * @date 2024-06-08
 * @author Scott Davis
 */
package com.mars_sim.core.resourceprocess.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mars_sim.core.data.RatingScore;
import com.mars_sim.core.goods.GoodsManager;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.fav.FavoriteType;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.person.ai.task.util.MetaTask;
import com.mars_sim.core.person.ai.task.util.SettlementMetaTask;
import com.mars_sim.core.person.ai.task.util.SettlementTask;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskUtil;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.resourceprocess.ResourceProcess;
import com.mars_sim.core.resourceprocess.ResourceProcessAssessment;
import com.mars_sim.core.resourceprocess.ResourceProcessSpec;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.robot.RobotType;
import com.mars_sim.core.structure.OverrideType;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.tool.Msg;

/**
 * Meta task for the ToggleResourceProcess task.
 */
public class ToggleResourceProcessMeta extends MetaTask implements SettlementMetaTask {
	
	/**
	 * Represents a job to toggle a Resource process in a building.
	 */
    private static class ToggleOffJob extends SettlementTask {
		
		private static final long serialVersionUID = 1L;
		
		private ResourceProcess process;
		
        public ToggleOffJob(SettlementMetaTask mt, Building processBuilding,
						ResourceProcess process,
						RatingScore score) {
			super(mt, "Toggle Off "
								+ process.getProcessName(), processBuilding, score);
			this.process = process;
        }

        @Override
        public Task createTask(Person person) {
            return new ToggleResourceProcess(person, (Building) getFocus(), process);
        }

        @Override
        public Task createTask(Robot robot) {
            return new ToggleResourceProcess(robot, (Building) getFocus(), process);
        }
		
 		@Override
		public int hashCode() {
			return process.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (super.equals(obj)) {
				// Same building & meta task so compare on Process
				ToggleOffJob other = (ToggleOffJob) obj;
				return process.equals(other.process);
			}
			return false;
		}
    }

    private static class ToggleOnJob extends SettlementTask {
		
		private static final long serialVersionUID = 1L;
		
		private ResourceProcessSpec process;
		private boolean useWaste;

        public ToggleOnJob(SettlementMetaTask mt, boolean useWaste,
							ResourceProcessSpec process, RatingScore score) {
			super(mt, "Toggle On " + process.getName(), null, score);
			this.process = process;
			this.useWaste = useWaste;
        }

        @Override
        public Task createTask(Person person) {
            return new ToggleResourceProcess(person, useWaste, process);
        }

        @Override
        public Task createTask(Robot robot) {
            return new ToggleResourceProcess(robot, useWaste, process);
        }
		
 		@Override
		public int hashCode() {
			return super.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (super.equals(obj)) {
				// Same building & meta task so compare on Process
				ToggleOnJob other = (ToggleOnJob) obj;
				return process.equals(other.process);
			}
			return false;
		}
    }

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.toggleResourceProcess"); //$NON-NLS-1$
	
	private static final double MAX_SCORE = 500;
	private static final double WASTE_THRESHOLD = 0.3; // % waste need to be available to toggle
	
	private static final double RATE_FACTOR = 10;
	private static final double INPUT_BIAS = 0.9;
	private static final double MATERIAL_BIAS = 3;
	

    public ToggleResourceProcessMeta() {
		super(NAME, WorkerType.BOTH, TaskScope.ANY_HOUR);
		setFavorite(FavoriteType.TINKERING);
		setPreferredJob(JobType.TECHNICIAN, JobType.ENGINEER);

		addPreferredRobot(RobotType.REPAIRBOT, RobotType.CONSTRUCTIONBOT, 
				RobotType.MAKERBOT, RobotType.DELIVERYBOT);
	}

	/**
	 * Robots can toggle resource processes.
	 * 
	 * @param t Task 
	 * @param r Robot making the request
	 */
	@Override
	public RatingScore assessRobotSuitability(SettlementTask t, Robot r)  {
        return TaskUtil.assessRobot(t, r);
    }

	/**
	 * Builds a list of TaskJob covering the most suitable Resource Processes to toggle.
	 * 
	 * @param settlement Settlement to check
	 */
	@Override
	public List<SettlementTask> getSettlementTasks(Settlement settlement) {
		List<SettlementTask> tasks = new ArrayList<>();

		Map<ResourceProcessSpec,ResourceProcessAssessment> assessed = new HashMap<>();

		if (!settlement.getProcessOverride(OverrideType.RESOURCE_PROCESS)) {			
			// Get the most suitable process per Building; not each process as too many will be created
			for (Building building : settlement.getBuildingManager().getBuildingSet(FunctionType.RESOURCE_PROCESSING)) {
				// In this building, select the best resource to compete
				selectToggableProcesses(building, 
								building.getResourceProcessing().getProcesses(), 
								false, tasks, assessed);
			}
		}

		if (!settlement.getProcessOverride(OverrideType.WASTE_PROCESSING)) {
			// Get the most suitable process per Building; not each process as too many will be created
			for (Building building : settlement.getBuildingManager().getBuildingSet(FunctionType.WASTE_PROCESSING)) {
				// In this building, select the best resource to compete
				selectToggableProcesses(building, 
								building.getWasteProcessing().getProcesses(), 
								true, tasks, assessed);
			}
		}
		
		return tasks;
	}

	/**
	 * Register any resource/waste process (from a building) based on its resource score.
	 *
	 * @param building
	 * @param processes
	 * @param isWaste
	 * @param rate0
	 * @param rate1
	 * @param results Holds the list of Task created
	 * @param assessed 
	 */
	private void selectToggableProcesses(Building building, 
			List<ResourceProcess> processes, boolean isWaste, List<SettlementTask> results,
			Map<ResourceProcessSpec,ResourceProcessAssessment> assessed) {

		Settlement settlement = building.getSettlement();		
		for (ResourceProcess process : processes) {
			// Avoid process that can't be toggled or no point toggling
			if (process.canToggle() && !process.isWorkerAssigned()) {

				// Is either running or not with with input available
				if (process.isProcessRunning()) {
					var score = new RatingScore(MAX_SCORE/2);

					var elapsed = getMarsTime().getTimeDiff(process.getToggleDue());

					// If overdue by more that 250 msols then score gets increased
					score.addModifier("toggleTime", 0.75 + (elapsed/500D));
				
					// Has a score so queue it; avoid very small benefits
					if (score.getScore() > 1) { 
						results.add(new ToggleOffJob(this, building, process, score));
					}
				}
				else {
					var spec = process.getSpec();
					var a = assessed.computeIfAbsent(spec,
								s -> calculateAssessment(settlement, s, isWaste, results));
					process.setAssessment(a);
				}
			}
		}
	}
			
	private ResourceProcessAssessment calculateAssessment(Settlement settlement,
					ResourceProcessSpec process, boolean isWaste,
					List<SettlementTask> results) {
		ResourceProcessAssessment a = ResourceProcess.DEFAULT_ASSESSMENT;

		var inputsAvaiable = isInputsPresent(settlement, process);
		if (inputsAvaiable) {
			// Score each process
			RatingScore score;
			if (isWaste)  {
				a = computeInputScore(settlement, process);
				score = new RatingScore("waste", a.overallScore());
			}
			else {
				// Compute the input score
				double inputValue = computeResourcesValue(settlement, process, true) * 10;

				// Compute the output score		
				double outputValue = computeResourcesValue(settlement, process, false) * 10;

				a = new ResourceProcessAssessment(inputValue, outputValue,
									outputValue - inputValue, true);
				score = new RatingScore("outputs", outputValue);
				score.addBase("inputs", -inputValue);
			}

			if (score.getScore() > 0) {
				score.applyRange(0, MAX_SCORE);

				results.add(new ToggleOnJob(this, isWaste, process, score));
			}
		}

		return a;
	}
	
	
	/**
	 * Checks if a resource process spec has all input resources.
	 *
	 * @param settlement the settlement the resource is at.
	 * @param processSpec the resource process spec.
	 * @return false if any input resources are empty.
	 */
	private static boolean isInputsPresent(Settlement settlement, ResourceProcessSpec processSpec) {
		for (var amount : processSpec.getMinimumInputs().entrySet()) {
			if (amount.getValue() > settlement.getAmountResourceStored(amount.getKey())) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Gets the composite resource score based on the availability of inputs
	 *
	 * @param settlement the settlement the resource process is at.
	 * @param process    the resource process.
	 * @return the resource assessment
	 */
	private static ResourceProcessAssessment computeInputScore(Settlement settlement,
							ResourceProcessSpec process) {
		double lowestPercentage = -1;

		// Is tere enugh for a sol'sworth of processing
		for(int id : process.getInputResources()) {
			double percAvailable;
			if (process.isAmbientInputResource(id)) {
				// Ambient is always avaialble
				percAvailable = 1D;
			}
			else {
				double available = settlement.getAmountResourceStored(id);
				double rate = process.getBaseInputRate(id); // per sol
				double perSol = process.getProcessTime() / 1000D;
				percAvailable = Math.min(1D, ((rate * perSol)/available));
			}

			// Update the lowest
			if (lowestPercentage < 0) {
				lowestPercentage = percAvailable;
			}
			else {
				lowestPercentage = Math.min(lowestPercentage, percAvailable);
			}
		}

		// Put a min threshold on the available waste
		double rawScore = 0;
		if (lowestPercentage > WASTE_THRESHOLD) {
			rawScore = lowestPercentage * MAX_SCORE;
		}

		return new ResourceProcessAssessment(rawScore, 0, rawScore, true);
	}

	/**
	 * Gets the total value of a resource process's input or output.
	 *
	 * @param settlement the settlement for the resource process.
	 * @param input      is the resource value for the input?
	 * @return the total value for the input or output.
	 */
	private static double computeResourcesValue(Settlement settlement,
												ResourceProcessSpec processSpec,
												boolean input) {
		double score = 0;

		Set<Integer> set = null;
		if (input)
			set = processSpec.getInputResources();
		else
			set = processSpec.getOutputResources();

		GoodsManager gm = settlement.getGoodsManager();
		for (int resource : set) {
			// Gets the vp for this resource
			// Add 1 to avoid being less than 1
			double vp = gm.getGoodValuePoint(resource);

			// Gets the supply of this resource
			// Note: use supply instead of stored amount.
			// Stored amount is slower and more time consuming
			double supply = gm.getSupplyValue(resource);

			if (input) {
				// For inputs: 
				// Note: mass rate is kg/sol
				double rate = processSpec.getBaseInputRate(resource) * RATE_FACTOR;
				
				// Multiply by bias so as to favor/discourage the production of output resources

				// Calculate the modified mass rate
				double mrate = rate * vp * INPUT_BIAS;
				
				// Note: mass rate * VP -> demand
				
				// if this resource is ambient
				// that the settlement doesn't need to supply (e.g. carbon dioxide),
				// then it won't need to check how much it has in stock
				// and it will not be affected by its vp and supply
				if (processSpec.isAmbientInputResource(resource)) {
					// e.g. For CO2, limit the score
					score += mrate;
				} else if (isInSitu(resource) || isRawMaterial(resource)) {
					// If in-situ, reduce the input score 
					score += mrate / MATERIAL_BIAS * Math.max(60, supply);
				} else {
					score += mrate * supply;
				}
			}

			else {
				// For outputs: 
				// Gets the remaining amount of this resource
				double remain = settlement.getAmountResourceRemainingCapacity(resource);

				if (remain == 0.0)
					return 0;

				double rate = processSpec.getBaseOutputRate(resource) * RATE_FACTOR;

				// For output value
				if (rate > remain) {
					// This limits the rate to match the remaining space 
					// that can accommodate this output resource
					rate = remain;
				}

				// Calculate the modified mass rate
				double mrate = rate * vp;
				
				// if this resource is ambient or a waste product
				// that the settlement won't keep (e.g. carbon dioxide),
				// then it won't need to check how much it has in stock
				// and it will not be affected by its vp and supply
				if (processSpec.isWasteOutputResource(resource)) {
					score += mrate;
				} else if (isInSitu(resource) || isRawMaterial(resource)) {
					// If in-situ, increase the output score 
					score += mrate * supply * MATERIAL_BIAS;
				} else
					score += mrate * supply;
			}
		}

		return score;
	}

	/**
	 * Is this an in-situ resource ?
	 * 
	 * @param resource
	 * @return
	 */
	private static boolean isInSitu(int resource) {
		return ResourceUtil.isInSitu(resource);
	}
	
	/**
	 * Is this a raw material resource ?
	 * 
	 * @param resource
	 * @return
	 */
	private static boolean isRawMaterial(int resource) {
		return ResourceUtil.isRawMaterial(resource);
	}
}
