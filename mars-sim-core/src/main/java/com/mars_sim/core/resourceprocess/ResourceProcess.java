/*
 * Mars Simulation Project
 * ResourceProcess.java
 * @date 2024-06-09
 * @author Scott Davis
 */
package com.mars_sim.core.resourceprocess;

import java.util.Set;

import com.mars_sim.core.events.ScheduledEventHandler;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.MarsTime;
import com.mars_sim.core.tool.RandomUtil;

/**
 * The ResourceProcess class represents a process of converting one set of
 * resources to another. This represent the actual process instant attached to a Building.
 */
public class ResourceProcess implements ScheduledEventHandler {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(ResourceProcess.class.getName());

	private static final double SMALL_AMOUNT = 0.000001;
	// How often should the process be checked? 
	private static final double PROCESS_CHECK_FREQUENCY = 5D; // 200 times per sol
	
	/**
	 * Represents the internal state of the process.
	 */
	public enum ProcessState {
			RUNNING, IDLE, INPUTS_UNAVAILABLE
	}

	private boolean workerAssigned = false;
	private boolean runningProcess;
	
	/** The time accumulated [in millisols]. */
	private double accumulatedTime;
	private double currentProductionLevel;
	private double toggleRunningWorkTime;

	private ResourceProcessAssessment assessment;

	private boolean canToggle = false;
	private MarsTime toggleDue = null; 

	private ResourceProcessEngine engine;
	private ResourceProcessSpec processSpec;
	private Settlement host;

	public static final ResourceProcessAssessment DEFAULT_ASSESSMENT = new ResourceProcessAssessment(0, 0, 0, false);
	
	/**
	 * Constructor.
	 *
	 * @param engine The processing engine that this Process manages
	 */
	public ResourceProcess(ResourceProcessEngine engine, Settlement host) {
		this.processSpec = engine.getProcessSpec();
		runningProcess = processSpec.getDefaultOn();
		currentProductionLevel = 1D;
		this.canToggle = false;
		this.engine = engine;
		this.host = host;
		this.assessment = DEFAULT_ASSESSMENT;

		// Add some randomness, today is sol 1
		resetToggleWait(100 + RandomUtil.getRandomInt(processSpec.getProcessTime()));
	}

	
	@Override
	public int execute(MarsTime currentTime) {
		canToggle = true;
		return 0;
	}


	@Override
	public String getEventDescription() {
		return "Toggle " + (runningProcess ? "Off" : "On") + " for " + processSpec.getName();
	}

	/**
	 * Gets the process name.
	 *
	 * @return process name as string.
	 */
	public String getProcessName() {
		return processSpec.getName();
	}

	/**
	 * Gets the current production level of the process.
	 *
	 * @return proportion of full production (0D - 1D)
	 */
	public double getCurrentProductionLevel() {
		return currentProductionLevel;
	}

	/**
	 * Checks if the process is running or not.
	 *
	 * @return true if process is running.
	 */
	public boolean isProcessRunning() {
		return runningProcess;
	}

	/**
	 * Checks if the process has required inputs.
	 * This is not a live instaneous check, but a check of the last time the process was run.
	 *
	 * @return true if process has inputs
	 */
	public ProcessState getState() {
		if (runningProcess) {
			return ProcessState.RUNNING;
		}
		else if (assessment.inputsAvailable()) {
			return ProcessState.IDLE;
		}
		else {
			return ProcessState.INPUTS_UNAVAILABLE;
		}
	}

	/**
	 * Sets if the process is running or not.
	 *
	 * @param runningProcess true if process is running.
	 */
	public void setProcessRunning(boolean runningProcess) {
		this.runningProcess = runningProcess;

		int delay = processSpec.getProcessTime();
		if (!runningProcess) {
			// Not runnign so half the time before it can be restarted
			delay /= 2;
		}
		resetToggleWait(delay);
	}

	private void resetToggleWait(int delay) {
		var event = host.getFutureManager().addEvent(delay, this);
		toggleDue = event.getWhen();
	}

	/**
	 * Checks if the process has been flagged for change.
	 *
	 * @return true if the process has been flagged for change.
	 */
	public boolean isWorkerAssigned() {
		return workerAssigned;
	}

	/**
	 * Flags the process for change.
	 *
	 * @param value true if the flag is true.
	 */
	public void setWorkerAssigned(boolean value) {
		workerAssigned = value;
	}
	
	/**
	 * Adds work time to toggling the process on or off.
	 *
	 * @param time the amount (millisols) of time to add.
	 * @return true if done
	 */
	public boolean addToggleWorkTime(double time) {
		toggleRunningWorkTime += time;
		if (toggleRunningWorkTime >= processSpec.getWorkTime()) {
			toggleRunningWorkTime = 0D;
			canToggle = false;
			
			setProcessRunning(!runningProcess);
			
			return true;
		}
		
		return false;
	}

	public double getRemainingToggleWorkTime() {
		double time = processSpec.getWorkTime() - toggleRunningWorkTime;
		if (time > 0)
			return time;
		else
			return 0;
	}
		
	public double getOverallScore() {
		return assessment.overallScore();
	}
	
	public double getInputScore() {
		return assessment.inputScore();
	}
	
	public double getOutputScore() {
		return assessment.outputScore();
	}
	
	public void setAssessment(ResourceProcessAssessment assessment) {
		this.assessment = assessment;
	}

	/**
	 * Get the specification of this process
	 * @return
	 */
	public ResourceProcessSpec getSpec() {
		return processSpec;
	}

	/**
	 * What is the minumum processing time for this process
	 * @return Value in mSol
	 */
	public int getProcessTime() {
		return processSpec.getProcessTime();
	}
	
	/**
	 * Gets the set of input resources.
	 *
	 * @return set of resources.
	 */
	public Set<Integer> getInputResources() {
		return processSpec.getInputResources();
	}

	public int getNumModules() {
		return engine.getModules();
	}
	
	/**
	 * Gets the base single input resource rate for a given resource.
	 *
	 * @return rate in kg/millisol.
	 */
	public double getBaseSingleInputRate(Integer resource) {
		return processSpec.getBaseInputRate(resource);
	}

	/**
	 * Gets the base full input resource rate for a given resource.
	 *
	 * @return rate in kg/millisol.
	 */
	public double getBaseFullInputRate(Integer resource) {
		return engine.getBaseFullInputRate(resource);

	}
	
	/**
	 * Checks if resource is an ambient input.
	 *
	 * @param resource the resource to check.
	 * @return true if ambient resource.
	 */
	public boolean isAmbientInputResource(Integer resource) {
		return processSpec.isAmbientInputResource(resource);
	}	
	
	/**
	 * Gets the set of output resources.
	 *
	 * @return set of resources.
	 */
	public Set<Integer> getOutputResources() {
		return processSpec.getOutputResources();
	}

	/**
	 * Gets the base single output resource rate for a given resource.
	 *
	 * @return rate in kg/millisol.
	 */
	public double getBaseSingleOutputRate(Integer resource) {
		return processSpec.getBaseOutputRate(resource);
	}

	/**
	 * Gets the base full output resource rate for a given resource.
	 *
	 * @return rate in kg/millisol.
	 */
	public double getBaseFullOutputRate(Integer resource) {
		return engine.getBaseFullOutputRate(resource);
	}
	
	/**
	 * Checks if resource is a waste output.
	 *
	 * @param resource the resource to check.
	 * @return true if waste output.
	 */
	public boolean isWasteOutputResource(Integer resource) {
		return processSpec.isWasteOutputResource(resource);
	}

	/**
	 * Processes resources for a given amount of time.
	 *
	 * @param pulse
	 * @param productionLevel proportion of max process rate (0.0D - 1.0D)
	 * @throws Exception if error processing resources.
	 */
	public void processResources(ClockPulse pulse, double productionLevel) {
		double time = pulse.getElapsed();

		if ((productionLevel < 0D) || (productionLevel > 1D) || (time < SMALL_AMOUNT))
			return;

		if (runningProcess) {
			double newProdLevel = productionLevel;

			accumulatedTime += time;

			double newCheckPeriod = PROCESS_CHECK_FREQUENCY * time;
			
			if (accumulatedTime >= newCheckPeriod) {
				// Compute the remaining accumulatedTime
				accumulatedTime -= newCheckPeriod;
	
				double bottleneck = 1D;

				// Input resources from inventory.
				for (Integer resource : processSpec.getInputResources()) {
					if (!processSpec.isAmbientInputResource(resource)) {
						double fullRate = getBaseFullInputRate(resource);
						double resourceRate = fullRate * newProdLevel;
						double required = resourceRate * accumulatedTime;
						double stored = host.getAmountResourceStored(resource);
						
						// Get resource bottleneck
						double desiredResourceAmount = fullRate * time;
						double proportionAvailable = 1D;
						if (desiredResourceAmount > 0D)
							proportionAvailable = stored / desiredResourceAmount;
						if (bottleneck > proportionAvailable)
							bottleneck = proportionAvailable;
						
						// Retrieve the right amount
						if (stored > SMALL_AMOUNT) {
							if (required > stored) {
								resourceProblem(resource, false, required, stored);

								required = stored;
								host.retrieveAmountResource(resource, required);
								break;
							}
							else
								host.retrieveAmountResource(resource, required);
						}
						else {
							resourceProblem(resource, false, required, stored);
							break;
						}
					}
				}

				// Set level
				newProdLevel = Math.min(newProdLevel, bottleneck);
				
				// Output resources to inventory.
				for (Integer resource : processSpec.getOutputResources()) {
						double maxRate = getBaseFullOutputRate(resource);
						double resourceRate = maxRate * newProdLevel;
						double required = resourceRate * accumulatedTime;
						double remainingCap = host.getAmountResourceRemainingCapacity(resource);
						
						// Store the right amount
						if (remainingCap > SMALL_AMOUNT) {
							if (required > remainingCap) {
								resourceProblem(resource, true, required, remainingCap);

								required = remainingCap;
								host.storeAmountResource(resource, required);

								break;
							}
							else
								host.storeAmountResource(resource, required);
							
						}
						else {
							resourceProblem(resource, true, required, remainingCap);
							break;
						}
				}
			}

			// Set the current production level.
			currentProductionLevel = newProdLevel;
		}
	}

	private void resourceProblem(int resource, boolean capacity, double required, double available) {
		logger.fine(host, 30_000,
					(capacity ? "No capacity '" : "Not enough '")
					+ ResourceUtil.findAmountResourceName(resource)
					+ "' for '" + processSpec.getName() + "'. Required: "
					+ Math.round(required * 1000.0)/1000.0 + " kg. Available: "
					+ Math.round(available * 1000.0)/1000.0 + " kg.");
		setProcessRunning(false);
	}

	/**
	 * Gets the string value for this object.
	 *
	 * @return string
	 */
	public String toString() {
		return getProcessName();
	}

	/**
	 * Gets the amount of power required to run the process.
	 *
	 * @return power (kW).
	 */
	public double getPowerRequired() {
		return processSpec.getPowerRequired();
	}

	/**
	 * Checks if the process has exceeded the time limit.
	 *
	 * @return
	 */
	public boolean canToggle() {
		return canToggle;
	}

	/**
	 * Gets the time permissions for the next toggle.
	 * 
	 * @return Maybe null if no toggle scheduled
	 */
	public MarsTime getToggleDue() {
		return toggleDue;
	}

	/**
	 * Times of the toggle operation. First item is the toggle work executed, 2nd is the target.
	 * 
	 * @return
	 */
	public double[] getToggleSwitchDuration() {
		return new double[] {toggleRunningWorkTime, processSpec.getWorkTime()};
	}
}
