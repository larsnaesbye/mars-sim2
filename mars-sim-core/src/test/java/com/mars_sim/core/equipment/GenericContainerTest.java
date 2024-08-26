/*
 * Mars Simulation Project
 * GenericContainerTest.java
 * @date 2021-10-14
 * @author Barry Evans
 */

package com.mars_sim.core.equipment;

import static org.junit.Assert.assertThrows;

import com.mars_sim.core.Simulation;
import com.mars_sim.core.SimulationConfig;
import com.mars_sim.core.UnitManager;
import com.mars_sim.core.resource.PhaseType;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.structure.MockSettlement;
import com.mars_sim.core.structure.Settlement;

import junit.framework.TestCase;

/**
 * Tests the logic of a Generic container
 */
public class GenericContainerTest
extends TestCase {

	// Extra amount to add to resource to handle double arithmetic mismatch
	private Settlement settlement;
	
	@Override
    public void setUp() throws Exception {
		// Create new simulation instance.
        SimulationConfig simConfig = SimulationConfig.instance();
        simConfig.loadConfig();
        
        Simulation sim = Simulation.instance();
        sim.testRun();

        UnitManager unitManager = sim.getUnitManager();
        
		// Create test settlement.
		settlement = new MockSettlement();	
		unitManager.addUnit(settlement);
    }

	/*
	 * Tests container associated settlement assignment.
	 */
	public void testAssociatedSettlement() throws Exception {
		GenericContainer c = new GenericContainer("Bag", EquipmentType.BAG, false, settlement);
	
		Settlement as = c.getAssociatedSettlement();
		assertEquals("Associated Settlement", as, settlement);
	}
	
	/*
	 * Tests container with a single resource.
	 */
	public void testSingleResource() throws Exception {
		GenericContainer c = new GenericContainer("Bag", EquipmentType.BAG, false, settlement);
		
		int rockID = ResourceUtil.rockSamplesID;
		double bagCap = ContainerUtil.getContainerCapacity(EquipmentType.BAG);
		assertEquals("EMpty capacity", bagCap,
						c.getAmountResourceCapacity(rockID));
		assertEquals("Remaining capacity", bagCap,
				c.getAmountResourceRemainingCapacity(rockID));
		
		// Load oxygen
		double quantity = bagCap/2D;
		assertEquals("Full store", 0D, c.storeAmountResource(rockID, quantity));
		assertEquals("Stored", quantity, c.getAmountResourceStored(rockID));
		assertEquals("Remaining capacity after load", quantity, c.getAmountResourceRemainingCapacity(rockID));

		// Fully overload
		assertEquals("Overload stored excess", quantity, c.storeAmountResource(rockID, bagCap));
		assertEquals("Stored after overload", bagCap, c.getAmountResourceStored(rockID));
		assertEquals("Remaining capacity after overload", 0D, c.getAmountResourceRemainingCapacity(rockID));
	}
	
	/*
	 * Tests container with 2 resources.
	 */
	public void testTwoResource() throws Exception {
		GenericContainer c = new GenericContainer("Bag", EquipmentType.BAG, false, settlement);
		
		int rockID = ResourceUtil.rockSamplesID;
		double bagCap = ContainerUtil.getContainerCapacity(EquipmentType.BAG);
		
		// Load rock
		double quantity = bagCap/2D;
		c.storeAmountResource(rockID, quantity);
		assertEquals("Stored", quantity, c.getAmountResourceStored(rockID));
		assertEquals("Stored resource", rockID, c.getResource());
		
		// Attempt to load 2nd resource
		int secondResource = ResourceUtil.iceID;
		assertEquals("Stored 2nd resource", quantity, c.storeAmountResource(secondResource, quantity));
		assertEquals("2nd resource capacity", 0D, c.getAmountResourceStored(secondResource));
		assertEquals("2nd resource remaining capacity", 0D, c.getAmountResourceRemainingCapacity(secondResource));
	}
	
	/*
	 * Tests container with 2 resources.
	 */
	public void testEmptying() throws Exception {
		GenericContainer c = new GenericContainer("Bag", EquipmentType.BAG, false, settlement);
		
		int rockID = ResourceUtil.rockSamplesID;
		double bagCap = ContainerUtil.getContainerCapacity(EquipmentType.BAG);
		
		// Load rock
		double quantity = bagCap/2D;
		c.storeAmountResource(rockID, bagCap);
		assertEquals("Stored", bagCap, c.getAmountResourceStored(rockID));
		assertEquals("Partial Unload", 0D, c.retrieveAmountResource(rockID, quantity));
		assertEquals("Stored after partial unload", quantity, c.getAmountResourceStored(rockID));

		assertEquals("Full Unload", 0D, c.retrieveAmountResource(rockID, quantity));
		assertEquals("Stored after full unload", 0D, c.getAmountResourceStored(rockID));

		assertEquals("Excessive Unload", quantity, c.retrieveAmountResource(rockID, quantity));

		
		// Still fixed to original resource
		int secondResource = ResourceUtil.iceID;
		assertEquals("1st resource after unload", bagCap, c.getAmountResourceRemainingCapacity(rockID));
		assertEquals("2nd resource after unload", 0D, c.getAmountResourceRemainingCapacity(secondResource));
	}
	
	/*
	 * Tests container with 2 resources.
	 */
	public void testNoneReusable() throws Exception {
		GenericContainer c = new GenericContainer("Bag", EquipmentType.BAG, false, settlement);
		
		int secondResource = ResourceUtil.iceID;
		int rockID = ResourceUtil.rockSamplesID;
		double bagCap = ContainerUtil.getContainerCapacity(EquipmentType.BAG);
		
		// Load rock
		c.storeAmountResource(rockID, bagCap);
		c.retrieveAmountResource(rockID, bagCap);
		assertEquals("Capacity of prime resource after full unload", bagCap, c.getAmountResourceRemainingCapacity(rockID));
		assertEquals("Capacity of secondary resource after full unload", 0D, c.getAmountResourceRemainingCapacity(secondResource));
	}
	
	
	/*
	 * Tests container with 2 resources.
	 */
	public void testReusable() throws Exception {
		GenericContainer c = new GenericContainer("Specimen", EquipmentType.SPECIMEN_BOX, true, settlement);
		
		int secondResource = ResourceUtil.iceID;
		int rockID = ResourceUtil.rockSamplesID;
		double bagCap = ContainerUtil.getContainerCapacity(EquipmentType.SPECIMEN_BOX);
		
		// Load rock
		c.storeAmountResource(rockID, bagCap);
		c.retrieveAmountResource(rockID, bagCap);
		
		// Both should be back to full capacity
		assertEquals("Capacity of prime resource after full unload", bagCap, c.getAmountResourceRemainingCapacity(rockID));
		assertEquals("Capacity of secondary resource after full unload", bagCap, c.getAmountResourceRemainingCapacity(secondResource));
	}

	/*
	 * Tests container with Liquids & Solids.
	 */
	public void testBarrelLiquid() throws Exception {
		EquipmentType cType = EquipmentType.BARREL;
		GenericContainer c = new GenericContainer(cType.getName(), cType, true, settlement);

		double cap = ContainerUtil.getContainerCapacity(cType);
		int	allowedId1 = ResourceUtil.waterID;
		int failedId1 = ResourceUtil.oxygenID;
		int allowedId2 = ResourceUtil.regolithBID;
		
		// Test negatives first
		assertPhaseNotSupported(c, failedId1);
		
		assertEquals("Container capacity 1", cap, c.getAmountResourceRemainingCapacity(allowedId1));
		assertEquals("Container capacity 2", cap, c.getAmountResourceRemainingCapacity(allowedId2));
		
		// Check the correct resource can be stored
		c.storeAmountResource(allowedId1, cap/2);
		
		// Both should be back to full capacity
		assertEquals("Container " + c.getName() + " resource stored", allowedId1, c.getResource());
		assertEquals("Container " + c.getName() + " stored", cap/2, c.getStoredMass());
	}
	
	/*
	 * Test container with Gas.
	 */
	public void testCanisterGas() throws Exception {
		assertPhaseSupported(EquipmentType.GAS_CANISTER, PhaseType.GAS);
	}
	
	/*
	 * Test container with Solids.
	 */
	public void testLargeBagSolid() throws Exception {
		assertPhaseSupported(EquipmentType.LARGE_BAG, PhaseType.SOLID);
	}
	
	public void testBoxSolid() throws Exception {
		assertPhaseSupported(EquipmentType.SPECIMEN_BOX, PhaseType.SOLID);
	}
	
	public void testBagSolid() throws Exception {
		assertPhaseSupported(EquipmentType.BAG, PhaseType.SOLID);
	}
	
	/**
	 * Test that a specific container type can only support the specific PhaseType.
	 * 
	 * @param cType
	 * @param required
	 */
	private void assertPhaseSupported(EquipmentType cType, PhaseType required) {
		GenericContainer c = new GenericContainer(cType.getName(), cType, true, settlement);

		double cap = ContainerUtil.getContainerCapacity(cType);
		int allowedId = 0;
		int failedId1 = 0;
		int failedId2 = 0;
		switch(required) {
		case LIQUID:
			allowedId = ResourceUtil.waterID;
			failedId1 = ResourceUtil.oxygenID;
			failedId2 = ResourceUtil.regolithBID;
			break;
		
		case GAS:
			allowedId = ResourceUtil.oxygenID;
			failedId1 = ResourceUtil.waterID;
			failedId2 = ResourceUtil.regolithBID;
			break;
			
		case SOLID:
			allowedId = ResourceUtil.regolithBID;
			failedId1 = ResourceUtil.oxygenID;
			failedId2 = ResourceUtil.waterID;
			break;
		}
		
		// Test negatives first
		assertPhaseNotSupported(c, failedId1);
		assertPhaseNotSupported(c, failedId2);

		assertEquals("Container capacity", cap, c.getAmountResourceRemainingCapacity(allowedId));
		
		// Check the correct resource can be stored
		c.storeAmountResource(allowedId, cap/2);
		
		// Both should be back to full capacity
		assertEquals("Container " + c.getName() + " resource stored", allowedId, c.getResource());
		assertEquals("Container " + c.getName() + " stored", cap/2, c.getStoredMass());
	}
	
	/**
	 * Tests phase support.
	 * 
	 * @param c
	 * @param resourceId
	 */
	private void assertPhaseNotSupported(GenericContainer c, int resourceId) {
		double cap = ContainerUtil.getContainerCapacity(c.getEquipmentType());

		assertEquals("Container no capacity", 0D, c.getAmountResourceRemainingCapacity(resourceId));
		
		// Load resource
		assertThrows(IllegalArgumentException.class, () -> {
				c.storeAmountResource(resourceId, cap/2);
		    });
		  
		// Both should be back to full capacity
		assertEquals("Container " + c.getName() + " has no resource", -1, c.getResource());
		assertEquals("Container " + c.getName() + " nothing stored", 0D, c.getStoredMass());
	}
}