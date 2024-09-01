package com.mars_sim.core.structure.building.task;

import com.mars_sim.core.AbstractMarsSimUnitTest;
import com.mars_sim.core.malfunction.MalfunctionManager;
import com.mars_sim.core.map.location.LocalPosition;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.science.task.MarsSimContext;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.function.FunctionType;

public class MaintainBuildingTest extends AbstractMarsSimUnitTest {
    static void buildingNeedMaintenance(Building b, MarsSimContext context) {
        MalfunctionManager manager = b.getMalfunctionManager();
        var mTime = context.getSim().getMasterClock().getMarsTime().addTime(manager.getMaintenancePeriod() * MaintainBuildingMeta.INSPECTION_PERCENTAGE * 2D);
        manager.activeTimePassing(context.createPulse(mTime, false, false));
    }
    
    public void testCreate() {

        var s = buildSettlement("Maintain");
        var b = buildResearch(s.getBuildingManager(), LocalPosition.DEFAULT_POSITION, 0D, 1);
        var p = buildPerson("Engineer", s, JobType.ENGINEER, b, FunctionType.RESEARCH);
        p.getSkillManager().addNewSkill(SkillType.MECHANICS, 10); // Skilled

        // Set building needs maintenance by moving time forward twice minimum
        buildingNeedMaintenance(b, this);
        var manager = b.getMalfunctionManager();
        assertGreaterThan("Maintenance due", 0D, manager.getEffectiveTimeSinceLastMaintenance());

        var task = new MaintainBuilding(p, b);
        assertFalse("Task created", task.isDone());

        // Do the initial walk
        executeTaskUntilSubTask(p, task, 100);
        assertTrue("Walk completed", task.getSubTask().isDone());

        // Do maintenance for a few calls to ensure maintenance is happening
        executeTaskUntilPhase(p, task, 2);
        assertFalse("Task still active", task.isDone());
        assertEquals("Engineer location", b, p.getBuildingLocation());
        assertGreaterThan("Maintenance completed", 0D, manager.getInspectionWorkTimeCompleted());

        // Complete until the end
        executeTaskForDuration(p, task, task.getTimeLeft() * 1.1);
        assertTrue("Task created", task.isDone());
        assertEquals("Maintenance period reset", 0D, manager.getTimeSinceLastMaintenance());
    }

    public void testMetaTask() {
        var s = buildSettlement("Maintenance");
        var b1 = buildResearch(s.getBuildingManager(), LocalPosition.DEFAULT_POSITION, 0D, 1);
        // 2nd building check logic
        buildResearch(s.getBuildingManager(), new LocalPosition(10, 10), 0D, 2);

        var mt = new MaintainBuildingMeta();
        var tasks = mt.getSettlementTasks(s);
        assertTrue("No tasks found", tasks.isEmpty());

        // One building needs maintenance
        buildingNeedMaintenance(b1, this);
        tasks = mt.getSettlementTasks(s);
        assertEquals("Tasks found", 1, tasks.size());

        var found = tasks.get(0);
        assertFalse("Not EVA task", found.isEVA());
        assertEquals("Found building with maintenance", b1, found.getFocus());
    }
}
