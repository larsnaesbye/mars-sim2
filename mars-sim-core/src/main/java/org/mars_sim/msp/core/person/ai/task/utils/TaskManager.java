/**
 * Mars Simulation Project
 * TaskManager.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.task.utils;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.mars_sim.msp.core.SimulationFiles;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitEventType;
import org.mars_sim.msp.core.data.SolListDataLogger;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.ai.task.Walk;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.MarsClockFormat;
import org.mars_sim.msp.core.time.Temporal;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Vehicle;

/*
 * The TaskManager class keeps track of a Worker's current task and can randomly
 * assign a new task based on a list of possible tasks and the current situation.
 */
public abstract class TaskManager implements Serializable, Temporal {
	/*
	 * This class represents a record of a given activity (task or mission)
	 * undertaken by a person
	 */
	public final class OneActivity implements Serializable {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		// Data members
		private String taskName;
		private String missionName;
		private String description;
		private String phase;
		private int startTime;


		public OneActivity(int startTime, String taskName, String description, String phase, String missionName) {
			this.taskName = taskName;
			this.missionName = missionName;
			this.description = description;
			this.startTime = startTime;
			this.phase = phase;
		}
		
		/**
		 * Are these 2 activities equivalent? i.e. are the taskName & phase the same
		 * @param lastActivity
		 * @return
		 */
		boolean isEquivalent(OneActivity lastActivity) {
			// Cheat here to save some time.
			// Do not bother checking the task name since if the description
			// and phase are the same then it will be the same Task.
			return (description.equals(lastActivity.description)
					&& phase.equals(lastActivity.phase));
		}

		/**
		 * Gets the start time of the task.
		 * 
		 * @return start time
		 */
		public int getStartTime() {
			return startTime;
		}

		/**
		 * Gets the task name.
		 * 
		 * @return task name
		 */
		public String getTaskName() {
			return taskName;
		}

		/**
		 * Gets the description what the actor is doing.
		 * 
		 * @return description
		 */
		public String getDescription() {
			return description;
		}

		/**
		 * Gets the task phase.
		 * 
		 * @return task phase
		 */
		public String getPhase() {
			return phase;
		}

		public String getMission() {
			return missionName;
		}
	}

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
	// Number of days to record Tack Activities
	private static final int NUM_SOLS = 5;
	
	protected static MarsClock marsClock;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(TaskManager.class.getName());

	protected static PrintWriter diagnosticFile = null;

	/**
	 * Enable the detailed diagnostics
	 * @throws FileNotFoundException 
	 */
	public static void setDiagnostics(boolean diagnostics) throws FileNotFoundException {
		if (diagnostics) {
			if (diagnosticFile == null) {
				String filename = SimulationFiles.getLogDir() + "/task-cache.txt";
				diagnosticFile  = new PrintWriter(filename);
				logger.config("Diagnostics enabled to " + filename);
			}
		}
		else if (diagnosticFile != null){
			diagnosticFile.close();
			diagnosticFile = null;
		}
	}

	/**The worker **/
	protected transient Unit worker;
	/** The current task the worker is doing. */
	protected transient Task currentTask;
	/** The last task the person was doing. */
	private transient Task lastTask;
	
	/** The cache for msol. */
	private double msolCache = -1.0;
	/** The cache for total probability. */
	protected double totalProbCache;
	protected transient Map<MetaTask, Double> taskProbCache;

	// Activity tracking. Keep a handy reference to the last one recorded
	private SolListDataLogger<OneActivity> allActivities;
	private OneActivity lastActivity = null;
	private int now = -1;

	protected TaskManager(Unit worker) {
		this.worker = worker;
		
		this.allActivities = new SolListDataLogger<>(NUM_SOLS);
	}

	/**
	 * Returns true if person has a task (may be inactive).
	 * 
	 * @return true if person has a task
	 */
	public boolean hasTask() {
		return currentTask != null;
	}

	/**
	 * Returns the name of the current task for UI purposes. Returns a blank string
	 * if there is no current task.
	 * 
	 * @return name of the current task
	 */
	public String getTaskName() {
		if (currentTask != null) {
			return currentTask.getName();
		} else {
			return "";
		}
	}

	public String getSubTaskName() {
		if (currentTask != null && currentTask.getSubTask() != null) {
			return currentTask.getSubTask().getName();
		} else {
			return "";
		}
	}

	public String getSubTask2Name() {
		Task task = getRealTask();
		if (task != null) {
			return task.getName();
		} else {
			return "";
		}
	}

	/**
	 * Gets the real-time task 
	 * 
	 * @return
	 */
	public Task getRealTask() {
		if (currentTask == null) {
			return null;
		}
		
		Task subtask1 = currentTask.getSubTask();
		if (subtask1 == null) {
			return currentTask;
		}
		
		if (subtask1.getSubTask() == null) {
			return subtask1;
		}
		
		Task subtask2 = subtask1.getSubTask();
		if (subtask2 == null) {
			return subtask1;
		}
		
		if (subtask2.getSubTask() == null) {
			return subtask2;
		}
		
		return subtask2.getSubTask();
	}

	/**
	 * Returns the name of the current task for UI purposes. Returns a blank string
	 * if there is no current task.
	 * 
	 * @return name of the current task
	 */
	public String getTaskClassName() {
		if (currentTask != null) {
			return currentTask.getTaskName();
		} else {
			return "";
		}
	}

	/**
	 * Returns a description of current task for UI purposes. Returns a blank string
	 * if there is no current task.
	 * 
	 * @return a description of the current task
	 */
	public String getTaskDescription(boolean subTask) {
		if (currentTask != null) {
			String t = currentTask.getDescription(subTask);
			if (t != null) // || !t.equals(""))
				return t;
			else
				return "";
		} else
			return "";
	}

	public String getSubTaskDescription() {
		if (currentTask != null && currentTask.getSubTask() != null) {
			String t = currentTask.getSubTask().getDescription();
			if (t != null) // || !t.equals(""))
				return t;
			else
				return "";
		} else
			return "";
	}

	public String getSubTask2Description() {
		if (currentTask != null && currentTask.getSubTask() != null
				&& currentTask.getSubTask().getSubTask() != null) {
			String t = currentTask.getSubTask().getSubTask().getDescription();
			if (t != null) // || !t.equals(""))
				return t;
			else
				return "";
		} else
			return "";
	}

	/**
	 * Returns the current task phase if there is one. Returns null if current task
	 * has no phase. Returns null if there is no current task.
	 * 
	 * @return the current task phase
	 */
	public TaskPhase getPhase() {
		if (currentTask != null) {
			return currentTask.getPhase();
		} else {
			return null;
		}
	}

	public TaskPhase getSubTaskPhase() {
		if (currentTask != null && currentTask.getSubTask() != null) {
			return currentTask.getSubTask().getPhase();
		} else {
			return null;
		}
	}

	public TaskPhase getSubTask2Phase() {
		if (currentTask != null && currentTask.getSubTask() != null
				&& currentTask.getSubTask().getSubTask() != null) {
			return currentTask.getSubTask().getSubTask().getPhase();
		} else {
			return null;
		}
	}

	/**
	 * Returns the current task. Return null if there is no current task.
	 * 
	 * @return the current task
	 */
	public Task getTask() {
		return currentTask;
	}

	public String getLastTaskName() {
		return (lastTask != null ? lastTask.getTaskName() : "");
	}

	public String getLastTaskDescription() {
		return (lastTask != null ? lastTask.getDescription() : "");
	}

	/**
	 * Returns true if person has an active task.
	 * 
	 * @return true if person has an active task
	 */
	public boolean hasActiveTask() {
		return (currentTask != null && !currentTask.isDone());
	}

	/**
	 * Ends all sub tasks
	 */
	public void endSubTask() {
		if (currentTask != null && currentTask.getSubTask() != null) {
			currentTask.getSubTask().endTask();
		}
	}

	/**
	 * Start a new task
	 * @param newTask
	 */
	public void startTask(Task newTask) {
		if (newTask != null) {
			// Save the current task as last task
			lastTask = currentTask;
			
			// End the current task properly
			if ((currentTask != null) && !currentTask.isDone()) {
				String des = currentTask.getDescription();
	
				logger.info(worker, 20_000, "Quit '" + des + "' to start new Task '"
							+ newTask.getDescription() + "'.");
				currentTask.endTask();
			}
			
			// Make the new task as the current task
			currentTask = newTask;
			
			// Send out the task event
			worker.fireUnitUpdate(UnitEventType.TASK_EVENT, newTask);
		}
	}
	
	/**
	 * Adds a task to the stack of tasks.
	 * 
	 * @param newTask the task to be added
	 */
	public boolean addTask(Task newTask) {
		
		if (newTask == null) {
			// if newTask is null, it comes from TaskManager's startNewTask()
			
			lastTask = currentTask;
			
			currentTask = null;
			
			return false;
		}
		
		if (hasActiveTask()) {
			String currentDes = currentTask.getDescription();

			// Note: make sure robot's 'Sleep Mode' won't return false;
			if (currentDes.contains("Sleeping"))
				return false;
			
			if (currentDes.contains("EVA"))
				return false;
			
			if (newTask.getDescription().equalsIgnoreCase(currentDes))
				return false;	
		}
		
		startTask(newTask);
		return true;
	}

	/**
	 * Sets the current task to null.
	 */
	public void clearAllTasks(String reason) {
//			String lastTask = (currentTask != null ? currentTask.getDescription() : "Unknown Task");
//			String subtask1 = getSubTaskDescription();
//			String subtask2 = getSubTask2Description();
//			StringBuilder s = new StringBuilder(reason);
//			s.append(". Clearing task(s): ").append(lastTask);
//			if (!subtask1.equalsIgnoreCase("")) {
//				s.append(", ").append(subtask1);
//				if (!subtask2.equalsIgnoreCase(""))
//					s.append(", ").append(subtask2);
//			}
//			s.append(".");
//			logger.info(worker, 1_000, s.toString());
			endCurrentTask();
		}

	/**
	 * Ends the current task
	 */
	public void endCurrentTask() {
		if (currentTask != null) {
			currentTask.endTask();
			currentTask = null;
			worker.fireUnitUpdate(UnitEventType.TASK_EVENT);
		}
	}

	/**
	 * Clears a specific task
	 * 
	 * @param taskString
	 */
	public void clearSpecificTask(String taskString) {
		
		if (currentTask != null) {
			
			if (currentTask.getSubTask() != null) {
				
				if (currentTask.getSubTask().getSubTask() != null) {
					String taskName2 = currentTask.getSubTask().getSubTask().getClass().getSimpleName();
					if (taskName2.equalsIgnoreCase(taskString)) {
						currentTask.getSubTask().getSubTask().endTask();
					}
				}
				
				else {				
					String taskName1 = currentTask.getSubTask().getClass().getSimpleName();
					if (taskName1.equalsIgnoreCase(taskString)) {
						currentTask.getSubTask().endTask();
					}
				}
			}
			
			else {
				String taskName0 = currentTask.getClass().getSimpleName();
				if (taskName0.equalsIgnoreCase(taskString)) {
					endCurrentTask();
				}
			}
		}
	}

	/**
	 * Re-initializes instances when loading from a saved sim
	 */
	public void reinit() {
		if (currentTask != null)		
			currentTask.reinit();
		if (lastTask != null)
			lastTask.reinit();
	}

	/**
	 * Checks if task probability cache should be used.
	 * 
	 * @return true if cache should be used.
	 */
	protected boolean useCache() {
		double msol = marsClock.getMillisol();
		double diff = msol - msolCache;
		if (diff > 0.1D) {
			msolCache = msol;
			return false;
		}
		return true;
	}

	/**
	 * Calculates and caches the probabilities.
	 * This will NOT use the cache but assumes the callers know when a cahce can be used or not used. 
	 */
	protected abstract void rebuildTaskCache();

	/**
	 * Start a new task for the worker based on tasks available at their location.
	 * Uses the task probability cache. If a task is found; then it is assigned
	 * to the manager to start working.
	 */
	public void startNewTask() {
		Task result = null;
		MetaTask selectedMetaTask = null;

		// If cache is not current, calculate the probabilities.
		if (!useCache()) {
			rebuildTaskCache();
		}		

		if (totalProbCache == 0D) {
			logger.warning(worker, "No normal Tasks available");

			// Switch to loading non-work hour meta tasks since
			// leisure tasks are NOT based on needs
			List<MetaTask> list = MetaTaskUtil.getNonWorkHourMetaTasks();
			selectedMetaTask = list.get(RandomUtil.getRandomInt(list.size() - 1));
		} else if (taskProbCache != null && !taskProbCache.isEmpty()) {

			double r = RandomUtil.getRandomDouble(totalProbCache);

			// Determine which task is selected.
			Iterator<MetaTask> it = taskProbCache.keySet().iterator();
			while ((selectedMetaTask == null) && it.hasNext()) {
				MetaTask mt = it.next();
				double probWeight = taskProbCache.get(mt);
				if (r <= probWeight) {
					// Select this task
					selectedMetaTask = mt;
				} else {
					r -= probWeight;
				}
			}
		}

		if (selectedMetaTask == null) {
			logger.severe(worker, "Could not determine a new task.");
		} else {
			// Call constructInstance of the selected Meta Task to commence the ai task
			result = createTask(selectedMetaTask);
			
			startTask(result);
		}

		// Clear time cache.
		msolCache = -1;	
	}

	/**
	 * Actually constructs a new Task of the specified type.
	 * @param selectedMetaTask Type of task to create.
	 * @return New Task.
	 */
	protected abstract Task createTask(MetaTask selectedMetaTask);

	/**
	 * This return the last calculated probability map.
	 * @return
	 */
	public Map<MetaTask, Double> getLatestTaskProbability() {
		return taskProbCache;
	}

	/**
	 * This method output the cache to a file for diagnostics.
	 * @param extras Extra details about Task
	 */
	protected void outputCache(String... extras) {	
		synchronized (diagnosticFile) {	
			diagnosticFile.println(MarsClockFormat.getDateTimeStamp(marsClock));
			diagnosticFile.println("Worker:" + worker.getName());
			for (String s : extras) {
				diagnosticFile.println(s);				
			}
			diagnosticFile.println("Total:" + totalProbCache);
			for (Entry<MetaTask, Double> task : taskProbCache.entrySet()) {
				diagnosticFile.println(task.getKey().getName() + ":" + task.getValue());
			}
			
			diagnosticFile.println();
			diagnosticFile.flush();
		}
	}

	/**
	 * Time has advanced on. This has to carry over the last Activity of yesterday into today.
	 */
	public boolean timePassing(ClockPulse pulse) {
		now = pulse.getMarsTime().getMillisolInt();
		
		// New day so the Activity at the end of yesterday has to be carried over to the 1st of today
		if (pulse.isNewSol() && (lastActivity != null)) {
			// New activity for the start of the day
			OneActivity firstActivity = new OneActivity(0,
											lastActivity.getTaskName(),
											lastActivity.getDescription(),
											lastActivity.getPhase(),
											lastActivity.getMission());
			allActivities.addData(firstActivity);
		}
		return true;
	}
	
	/**
	 * Records a task onto the schedule
	 * 
	 * @param changed The active task.
	 * @param mission Associated mission.
	 */
	void recordTask(Task changed, String mission) {
		OneActivity newActivity = new OneActivity(now, changed.getName(false),
												  changed.getDescription(),
												  changed.getPhase().getName(), mission);
		
		if ((lastActivity == null) || !newActivity.isEquivalent(lastActivity)) {
			// Identify the level.
			allActivities.addData(newActivity);
			lastActivity = newActivity;
		}
	}
	
	/**
	 * Gets the today's activities.
	 * 
	 * @return a list of today's activities
	 */
	public List<OneActivity> getTodayActivities() {
		return allActivities.getTodayData();
	}
	
	/**
	 * Gets all activities of all days a person.
	 * 
	 * @return all activity schedules
	 */
	public Map<Integer, List<OneActivity>> getAllActivities() {
		return allActivities.getHistory();
	}
	
	/**
	 * Checks if the person or robot is walking through a given vehicle.
	 * @param vehicle the vehicle.
	 * @return true if walking through vehicle.
	 */
	public boolean isWalkingThroughVehicle(Vehicle vehicle) {
	
	    boolean result = false;
	
	    Task task = currentTask;
	    while ((task != null) && !result) {
	        if (task instanceof Walk) {
	            Walk walkTask = (Walk) task;
	            if (walkTask.isWalkingThroughVehicle(vehicle)) {
	                result = true;
	            }
	        }
	        task = task.getSubTask();
	    }
	
	    return result;
	}

	/**
	 * Checks if the person or robot is walking through a given building.
	 * 
	 * @param building the building.
	 * @return true if walking through building.
	 */
	public boolean isWalkingThroughBuilding(Building building) {
	
		boolean result = false;
	
		Task task = currentTask;
		while ((task != null) && !result) {
			if (task instanceof Walk) {
				Walk walkTask = (Walk) task;
				if (walkTask.isWalkingThroughBuilding(building)) {
					result = true;
				}
			}
			task = task.getSubTask();
		}
	
		return result;
	}
	
	/**
	 * Reloads instances after loading from a saved sim
	 * 
	 * @param clock
	 */
	public static void initializeInstances(MarsClock clock) {
		marsClock = clock;
	}	
}
