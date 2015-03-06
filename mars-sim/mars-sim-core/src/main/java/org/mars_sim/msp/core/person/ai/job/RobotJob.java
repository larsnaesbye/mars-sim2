/**
 * Mars Simulation Project
 * RobotJob.java
 * @version 3.07 2015-03-05
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.job;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.mars_sim.msp.core.Msg;

import org.mars_sim.msp.core.person.Robot;
import org.mars_sim.msp.core.person.RobotType;
import org.mars_sim.msp.core.structure.Settlement;

/** 
 * The Job class represents a person's job.
 */
public abstract class RobotJob
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** Probability penalty for starting a non-job-related task. */
	private static final double NON_JOB_TASK_PENALTY = 0D;
	/** Probability penalty for starting a non-job-related mission. */
	private static final double NON_JOB_MISSION_START_PENALTY = 0D;
	/** Probability penalty for joining a non-job-related mission. */
	private static final double NON_JOB_MISSION_JOIN_PENALTY = 0D;

	// Domain members
	protected Class<? extends RobotJob> jobClass;
	/** List of tasks related to the job. */
	protected List<Class<?>> jobTasks;
	/** List of missions to be started by a person with this job. */
	protected List<Class<?>> jobMissionStarts;
	/** List of missions to be joined by a person with this job. */
	protected List<Class<?>> jobMissionJoins;

	/**
	 * Constructor.
	 * @param name the name of the job.
	 */
	public RobotJob(Class<? extends RobotJob> jobClass) {
		this.jobClass = jobClass;
		jobTasks = new ArrayList<Class<?>>();
		jobMissionStarts = new ArrayList<Class<?>>();
		jobMissionJoins = new ArrayList<Class<?>>();
	}

	/**
	 * Gets the job's internationalized name for display in user interface.
	 * @param robotType {@link RobotType}
	 * @return name
	 */
	public static String getName(RobotType robotType) {
		StringBuffer key = new StringBuffer().append("job."); //$NON-NLS-1$
		switch (robotType) {
			case CHEFBOT : key.append("chefBot"); break; //$NON-NLS-1$
			case CONSTRUCTIONBOT : key.append("constructionBot"); break; //$NON-NLS-1$
			case DELIVERYBOT : key.append("deliveryBot"); break; //$NON-NLS-1$			
			case GARDENBOT : key.append("gardenBot"); break; //$NON-NLS-1$
			case MAKERBOT : key.append("makerBot"); break; //$NON-NLS-1$			
			case MEDICBOT : key.append("medicBot"); break; //$NON-NLS-1$			
			case REPAIRBOT : key.append("repairBot"); break; //$NON-NLS-1$
			default : key.append("bot"); break; //$NON-NLS-1$
		}
		//key.append(jobClass.getSimpleName());
		return Msg.getString(key.toString()); //$NON-NLS-1$
	};
	
	public Class<? extends RobotJob> getJobClass() {
		return this.jobClass;
	}

	/**
	 * Gets a robot's capability to perform this job.
	 * @param robot the robot to check.
	 * @return capability (min 0.0).
	 */
	public abstract double getCapability(Robot robot);

	
	/**
	 * Gets the probability modifier for starting a non-job-related task.
	 * @param taskClass the task class
	 * @return modifier >= 0.0
	 */
	public double getStartTaskProbabilityModifier(Class<?> taskClass) {
		double result = 1D;
		if (!jobTasks.contains(taskClass)) result = NON_JOB_TASK_PENALTY;
		return result;
	}

	/**
	 * Gets the probability modifier for starting a non-job-related mission.
	 * @param missionClass the mission class
	 * @return modifier >= 0.0
	 */
	public double getStartMissionProbabilityModifier(Class<?> missionClass) {
		double result = 1D;
		if (!jobMissionStarts.contains(missionClass)) result = NON_JOB_MISSION_START_PENALTY;
		return result;
	}

	/**
	 * Gets the probability modifier for joining a non-job-related mission.
	 * @param missionClass the mission class
	 * @return modifier >= 0.0
	 */
	public double getJoinMissionProbabilityModifier(Class<?> missionClass) {
		double result = 1D;
		if (!jobMissionJoins.contains(missionClass)) result = NON_JOB_MISSION_JOIN_PENALTY;
		return result;
	}

	/**
	 * Gets the base settlement need for this job.
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public abstract double getSettlementNeed(Settlement settlement);

	/**
	 * Checks if a task is related to this job.
	 * @param taskClass the task class
	 * @return true if job related task.
	 */
	public boolean isJobRelatedTask(Class<?> taskClass) {
		return jobTasks.contains(taskClass);
	}
}