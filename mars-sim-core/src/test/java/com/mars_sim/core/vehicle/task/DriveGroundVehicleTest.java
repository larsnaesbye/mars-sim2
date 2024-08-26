/*
 * Mars Simulation Project
 * DriveGroundVehicleTest.java
 * @date 2024-07-16
 * @author Barry Evans
 */

package com.mars_sim.core.vehicle.task;


import com.mars_sim.core.AbstractMarsSimUnitTest;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.vehicle.StatusType;
import com.mars_sim.mapdata.location.Direction;
import com.mars_sim.mapdata.location.LocalPosition;

public class DriveGroundVehicleTest extends AbstractMarsSimUnitTest {
    private static final double DIST = OperateVehicle.ARRIVING_BUFFER * 2;  // Drive 2 km
    private static final double METHANOL_AMOUNT = 30D;
    private static final double OXYGEN_AMOUNT = METHANOL_AMOUNT * OperateVehicle.RATIO_OXIDIZER_FUEL;
    
    public void testDriveVehicle() {
        var s = buildSettlement("Test Settlement");
        var v = buildRover(s, "Test Rover", LocalPosition.DEFAULT_POSITION);
        v.storeAmountResource(v.getFuelTypeID(), METHANOL_AMOUNT);
        v.storeAmountResource(ResourceUtil.oxygenID, OXYGEN_AMOUNT);

        // move to plant
        v.transfer(getSim().getUnitManager().getMarsSurface());

        String name = "Test Driver";
        var p = buildPerson(name, s, JobType.PILOT);
        p.transfer(v);

        var targetDir = new Direction(0.1);
        var dest = v.getCoordinates().getNewLocation(targetDir, DIST);
        var task = new DriveGroundVehicle(p, v, dest, getSim().getMasterClock().getMarsTime(),
                                    0D);

        assertFalse("Task created", task.isDone());
        assertEquals(name, p, v.getOperator());

        // Execute few calls to get driver positioned and moving
        executeTask(p, task, 10);
        assertEquals("Vehicle is moving", OperateVehicle.MOBILIZE, task.getPhase());
        assertGreaterThan("Vehicle speed", 0D, v.getSpeed());
        assertEquals("Vehicle primary status", StatusType.MOVING, v.getPrimaryStatus());

        // Drive the rest
        executeTaskUntilPhase(p, task, 1000);
        
        assertEquals("Vehicle oddmeter", Math.round(DIST), Math.round(v.getOdometerMileage()));
        assertEquals("Vehicle at destination", dest, v.getCoordinates());
        assertEquals("Vehicle end primary status", StatusType.PARKED, v.getPrimaryStatus());

        assertTrue("Task complete", task.isDone());
    }

    public void testDriveVehicleNoFuel() {
        var s = buildSettlement("Test Settlement");
        var v = buildRover(s, "Test Rover", LocalPosition.DEFAULT_POSITION);
        v.storeAmountResource(v.getFuelTypeID(), METHANOL_AMOUNT);
        v.storeAmountResource(ResourceUtil.oxygenID, OXYGEN_AMOUNT);

        // move to plant
        v.transfer(getSim().getUnitManager().getMarsSurface());

        var p = buildPerson("Test Driver", s, JobType.PILOT);
        p.transfer(v);

        var targetDir = new Direction(0.1);
        var dest = v.getCoordinates().getNewLocation(targetDir, DIST);
        var task = new DriveGroundVehicle(p, v, dest, getSim().getMasterClock().getMarsTime(),
                                    0D);

        assertFalse("Task created", task.isDone());
 
        // Execute few calls to get driver positioned and moving then remove fuel
        executeTask(p, task, 10);
        
        // If Battery power is used, instead of fuel
        assertEqualLessThan("Oxygen stored", OXYGEN_AMOUNT, v.getAmountResourceStored(ResourceUtil.oxygenID));
        assertEqualLessThan("Fuel stored", METHANOL_AMOUNT, v.getAmountResourceStored(v.getFuelTypeID()));
        
        v.retrieveAmountResource(v.getFuelTypeID(), v.getAmountResourceStored(v.getFuelTypeID()));
        assertEquals("Fuel emptied", 0.0D, v.getAmountResourceStored(v.getFuelTypeID()));

        executeTask(p, task, 10);
        assertEquals("Vehicle end primary status", StatusType.PARKED, v.getPrimaryStatus());
        assertTrue("Marked out of fuel", v.haveStatusType(StatusType.OUT_OF_FUEL));
        assertTrue("Task complete", task.isDone());
    }
}
