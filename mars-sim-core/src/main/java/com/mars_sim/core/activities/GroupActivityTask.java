/*
 * Mars Simulation Project
 * GroupActivityInfo.java
 * @date 2023-03-23
 * @author Barry Evans
 */
package com.mars_sim.core.activities;

import com.mars_sim.core.activities.GroupActivity.ActivityState;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskPhase;

/**
 * This acts as a Task to participate in a Group Activity. It goes through 2 phases
 * to reflect the starting and active states of a meeting. The Task will be ended once the
 * activity completes.
 */
public class GroupActivityTask extends Task {

    private static final TaskPhase WAITING = new TaskPhase("Waiting"); 
    private static final TaskPhase ACTIVE = new TaskPhase("Active"); 

    private GroupActivity activity;
    private ActivityState currentState;

    public GroupActivityTask(GroupActivity activity, Person person) {
        super(activity.getName(), person, false, false, 0D, activity.getTotalDuration());
        this.activity = activity;

        // TODO
        // Check is in Settlement
        // Add 2 phases
        // Set the pahase according to the activity state
        this.currentState = activity.getState();

        // Walk to meeting place
        if (!person.getBuildingLocation().equals(activity.getMeetingPlace())) {
            walkToRandomLocInBuilding(activity.getMeetingPlace(), true);
        }

        
		// Initialize phase
		addPhase(WAITING);
        addPhase(ACTIVE);

		setPhase(toPhase(activity));
    }

    private static TaskPhase toPhase(GroupActivity a) {
        return switch(a.getState()) {
            case PENDING -> WAITING;
            case ACTIVE -> ACTIVE;
            default -> null;
        };
    }

    @Override
    protected double performMappedPhase(double time) {
        if (!activity.isActive()) {
            endTask();
            return time;
        }

        // Change phase
        if ((activity.getState() == ActivityState.ACTIVE)
                && (currentState == ActivityState.PENDING)) {
            currentState = ActivityState.ACTIVE;
            setPhase(toPhase(activity));
        }
        return 0D;
    }

}
