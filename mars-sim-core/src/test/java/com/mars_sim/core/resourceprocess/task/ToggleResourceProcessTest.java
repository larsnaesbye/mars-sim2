package com.mars_sim.core.resourceprocess.task;

import com.mars_sim.core.AbstractMarsSimUnitTest;
import com.mars_sim.core.map.location.LocalPosition;

public class ToggleResourceProcessTest extends AbstractMarsSimUnitTest {


    public void testStartToggleOn() {
        var s = buildSettlement("Resource", true);
        var b = ToggleResourceProcessMetaTest.buildProcessing(this, s.getBuildingManager(), LocalPosition.DEFAULT_POSITION, 0D);
        var r = b.getResourceProcessing();

        // Pick a prcoess and add resoruces
        var p = r.getProcesses().get(0);

        // Reset toggle for now
        ToggleResourceProcessMetaTest.moveToToggle(this, p);

        var w = buildPerson("worker", s);
        var t = new ToggleResourceProcess(w, false, p.getSpec());

        assertFalse("Started task", t.isDone());
        assertTrue("Worker assigned", p.isWorkerAssigned());
        assertEquals("Select process", p, t.getResourceProcess());
        assertEquals("Selected Building", b, t.getBuilding());
        assertFalse("Process not running", p.isProcessRunning());

        var completed = p.addToggleWorkTime(p.getRemainingToggleWorkTime() + 1);
        assertTrue("Toggle completed", completed);
        assertTrue("Process running", p.isProcessRunning());

        t.endTask();
        assertFalse("Worker relased", p.isWorkerAssigned());
    }

    public void testStartToggleOnDuplicate() {
        var s = buildSettlement("Resource", true);
        var b = ToggleResourceProcessMetaTest.buildProcessing(this, s.getBuildingManager(), LocalPosition.DEFAULT_POSITION, 0D);
        var r = b.getResourceProcessing();

        // Pick a prcoess and add resoruces
        var p = r.getProcesses().get(0);

        // Reset toggle for now
        ToggleResourceProcessMetaTest.moveToToggle(this, p);

        var w = buildPerson("worker", s);
        var t = new ToggleResourceProcess(w, false, p.getSpec());
        assertFalse("Started task", t.isDone());

        var w1 = buildPerson("worker 2", s);
        var t1 = new ToggleResourceProcess(w1, false, p.getSpec());
        assertTrue("Failed to start", t1.isDone());
    }

    public void testStartNoToggle() {
        var s = buildSettlement("Resource", true);
        var b = ToggleResourceProcessMetaTest.buildProcessing(this, s.getBuildingManager(), LocalPosition.DEFAULT_POSITION, 0D);
        var r = b.getResourceProcessing();

        // Pick a prcoess and add resoruces
        var p = r.getProcesses().get(0);

        var w = buildPerson("worker", s);
        var t = new ToggleResourceProcess(w, false, p.getSpec());

        assertTrue("Task not started", t.isDone());
    }
}
