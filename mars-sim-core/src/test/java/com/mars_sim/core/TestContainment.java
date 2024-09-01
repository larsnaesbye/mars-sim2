/**
 * Mars Simulation Project
 * LoadVehicleTest.java
 * @version 3.1.0 2017-01-21
 * @author Scott Davis
 */

package com.mars_sim.core;

import com.mars_sim.core.environment.MarsSurface;
import com.mars_sim.core.equipment.Equipment;
import com.mars_sim.core.equipment.EquipmentFactory;
import com.mars_sim.core.equipment.EquipmentType;
import com.mars_sim.core.location.LocationStateType;
import com.mars_sim.core.map.location.LocalPosition;
import com.mars_sim.core.person.GenderType;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.function.VehicleGarage;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.core.vehicle.Vehicle;


public class TestContainment extends AbstractMarsSimUnitTest {

	private VehicleGarage garage;
	private Settlement settlement;
	private MarsSurface surface;

	@Override
    public void setUp() {
		super.setUp();
              
		settlement = buildSettlement();
        
		garage = buildGarage(settlement.getBuildingManager(), new LocalPosition(0, 0), 0D, 0);
        surface = unitManager.getMarsSurface();
    }

	private void testContainment(Unit source, Unit container, Unit topContainer, LocationStateType lon) {
		assertEquals("Location state type", lon, source.getLocationStateType());
		assertEquals("Parent container", container, source.getContainerUnit());
		assertEquals("Top container", topContainer, source.getTopContainerUnit());
	}

	private static void assertInsideSettlement(String msg, Unit source, Settlement base) {
		assertEquals(msg + ": Location state type", LocationStateType.INSIDE_SETTLEMENT, source.getLocationStateType());
		assertEquals(msg + ": Settlement", base, source.getSettlement());
		
		assertTrue(msg + ": InSettlement", source.isInSettlement());
		assertTrue(msg + ": IsInside", source.isInside());
		assertFalse(msg + ": IsOutside", source.isOutside());

		assertFalse(msg + ": isInVehicle", source.isInVehicle());
		assertNull(msg + ": Vehicle", source.getVehicle());
		
		assertEquals(msg + ": Container", base, source.getContainerUnit());
		assertEquals(msg + ": Top container", base, source.getTopContainerUnit());
	}

	/**
	 * Test condition of a vehicle parked in the vicinity of a settlement
	 * @param msg
	 * @param source
	 * @param base
	 */
	private void assertVehicleParked(String msg, Vehicle source, Settlement base) {
		assertEquals(msg + ": Location state type", LocationStateType.SETTLEMENT_VICINITY, source.getLocationStateType());
		assertEquals(msg + ": Settlement", base, source.getSettlement());
		
		assertTrue(msg + ": InSettlement", source.isInSettlement());
		assertFalse(msg + ": IsInside", source.isInside());
		assertTrue(msg + ": IsOutside", source.isOutside());

		assertFalse(msg + ": isInVehicle", source.isInVehicle());
		assertNull(msg + ": Vehicle", source.getVehicle());
		
		assertEquals(msg + ": Container", base, source.getContainerUnit());
		assertEquals(msg + ": Top container", base, source.getTopContainerUnit());
	}

	/**
	 * Test condition of a vehicle garaged inside a settlement
	 * @param msg
	 * @param source
	 * @param base
	 */
	private void assertVehicleGaraged(String msg, Vehicle source, Settlement base) {
		assertEquals(msg + ": Location state type", LocationStateType.INSIDE_SETTLEMENT, source.getLocationStateType());
		assertEquals(msg + ": Settlement", base, source.getSettlement());
		
		assertTrue(msg + ": InSettlement", source.isInSettlement());
		assertTrue(msg + ": IsInside", source.isInside());
		assertFalse(msg + ": IsOutside", source.isOutside());

		assertFalse(msg + ": isInVehicle", source.isInVehicle());
		assertNull(msg + ": Vehicle", source.getVehicle());
		
		assertEquals(msg + ": Container", base, source.getContainerUnit());
		assertEquals(msg + ": Top container", base, source.getTopContainerUnit());
	}
	
	private static void assertInBuilding(String msg, Person source, Building base, Settlement home) {
		assertInsideSettlement(msg, source, home);
		assertEquals(msg + ": Building", base, source.getBuildingLocation());
	}

	
	
	private static void assertInVehicle(String msg, Person source, Vehicle vehicle) {
		assertEquals(msg + ": person's location state type is INSIDE_VEHICLE", LocationStateType.INSIDE_VEHICLE, source.getLocationStateType());
	
		assertFalse(msg + ": isInVehicleInGarage", source.isInVehicleInGarage());
		assertFalse(msg + ": InSettlement", source.isInSettlement());
		
		assertTrue(msg + ": IsInside", source.isInside());
		assertFalse(msg + ": IsOutside", source.isOutside());

		assertTrue(msg + ": isInVehicle", source.isInVehicle());
		assertEquals(msg + ": Vehicle", vehicle, source.getVehicle());
		
		assertEquals(msg + ": Container", vehicle, source.getContainerUnit());
		assertEquals(msg + ": Top container", vehicle.getSettlement(), source.getTopContainerUnit());
	}

	private static void assertInVehicle(String msg, Equipment source, Vehicle vehicle) {
		assertEquals(msg + ": bag's location state type is INSIDE_VEHICLE", LocationStateType.INSIDE_VEHICLE, source.getLocationStateType());
		assertNull(msg + ": bag is still in settlement as vehicle is in settlement", source.getSettlement());
		
		assertFalse(msg + ": isInVehicleInGarage", source.isInVehicleInGarage());
		assertFalse(msg + ": InSettlement", source.isInSettlement());
		
		assertTrue(msg + ": IsInside", source.isInside());
		assertFalse(msg + ": IsOutside", source.isOutside());

		assertTrue(msg + ": isInVehicle", source.isInVehicle());
		assertEquals(msg + ": Vehicle", vehicle, source.getVehicle());
		
		assertEquals(msg + ": Container", vehicle, source.getContainerUnit());
		assertEquals(msg + ": Top container", vehicle.getSettlement(), source.getTopContainerUnit());
	}
	
	private void assertWithinSettlementVicinity(String msg, Unit source) {
		assertEquals(msg + ": Location state type", LocationStateType.SETTLEMENT_VICINITY, source.getLocationStateType());
		
		assertFalse(msg + ": InSettlement", source.isInSettlement());
		assertNull(msg + ": Settlement", source.getSettlement());
		assertFalse(msg + ": IsInside", source.isInside());
		assertTrue(msg + ": IsOutside", source.isOutside());
		assertFalse(msg + ": isInVehicle", source.isInVehicle());
		assertNull(msg + ": Vehicle", source.getVehicle());
		
		assertEquals(msg + ": Container", surface, source.getContainerUnit());
	}

	
	/*
	 * Test method for 'com.mars_sim.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testPersonInGarage() throws Exception {
		Person person = Person.create("Worker One", settlement, GenderType.MALE).build();
		unitManager.addUnit(person);

		assertInsideSettlement("Initial person", person, settlement);
		
		person.setCurrentBuilding(garage.getBuilding());
		
		assertInBuilding("Person in garage", person, garage.getBuilding(), settlement);
	}
	
	/*
	 * Test method for 'com.mars_sim.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testVehicleInGarage() throws Exception {
		Vehicle vehicle = buildRover(settlement, "Garage Rover", new LocalPosition(1,1));
        
		assertVehicleParked("Initial Vehicle", vehicle, settlement);
		
		boolean addedTogarage = settlement.getBuildingManager().addToGarage(vehicle);
		assertTrue("Parking vehicle in garage", addedTogarage);
		
		assertVehicleGaraged("Vehicle in garage", vehicle, settlement);
	}

	/*
	 * Test method for 'com.mars_sim.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testVehicleNearSettlement() throws Exception {
		Vehicle vehicle = buildRover(settlement, "Near Rover", new LocalPosition(1,1));

		vehicle.setContainerUnit(settlement);

		testContainment(vehicle, settlement, settlement, LocationStateType.SETTLEMENT_VICINITY);
	}
	
	/*
	 * Test method for 'com.mars_sim.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testVehicleOnSurface() throws Exception {
		Vehicle vehicle = buildRover(settlement, "Garage Rover", new LocalPosition(1,1));

		assertTrue("Transfer to Mars surface but still within settlement vicinity", vehicle.transfer(surface));

		assertWithinSettlementVicinity("After transfer from Settlement", vehicle);
	}

	/*
	 * Test method for 'com.mars_sim.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testBagOnSurface() throws Exception {

		Equipment bag = EquipmentFactory.createEquipment(EquipmentType.BAG, settlement);
		
		assertInsideSettlement("Initial equipment", bag, settlement);
		
		assertTrue("Transfer to Mars surface but still within settlement vicinity", bag.transfer(surface));
		assertWithinSettlementVicinity("in a settlement vicinity", bag);
		
		assertTrue("Transfer to settlement", bag.transfer(settlement));
		assertInsideSettlement("After return", bag, settlement);

	}
	
	/*
	 * Test method for 'com.mars_sim.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testBagOnVehicle() throws Exception {

		Equipment bag = EquipmentFactory.createEquipment(EquipmentType.BAG, settlement);
		
		assertInsideSettlement("Initial equipment", bag, settlement);
		
		Vehicle vehicle = buildRover(settlement, "Garage Rover", new LocalPosition(1,1));
		
        // Vehicle leaves garage
        BuildingManager.removeFromGarage(vehicle);

		assertTrue("Vehicle leaving garage. Transfer bag from settlement to vehicle", bag.transfer(vehicle));
		assertInVehicle("In vehicle", bag, vehicle);
		
		assertTrue("Transfer bag from vehicle back to settlement", bag.transfer(settlement));
		assertInsideSettlement("After return", bag, settlement);

	}

	
	/*
	 * Test method for 'com.mars_sim.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
	public void testPersonOnVehicle() throws Exception {

		Person person = buildPerson("Test Person", settlement);
		
		assertInsideSettlement("Initial Person", person, settlement);
		
		Rover vehicle = buildRover(settlement, getName(), new LocalPosition(1,1));
        
		assertTrue("Transfer person from settlement to vehicle", person.transfer(vehicle));
		assertInVehicle("In vehicle", person, vehicle);
		assertEquals("Person's location state type is INSIDE_VEHICLE", LocationStateType.INSIDE_VEHICLE, person.getLocationStateType());

		assertTrue("Person in crew", vehicle.getCrew().contains(person));
		assertFalse("Person in a vehicle. Person is not considered to be in a settlement", person.isInSettlement());
		assertTrue("Vehicle still in a settlement", vehicle.isInSettlement());
		
		// Vehicle going into a garage
		settlement.getBuildingManager().addToGarageBuilding(vehicle);
		assertTrue("Vehicle has entered a garage", vehicle.isInGarage());
		assertInsideSettlement("Vehicle still in a settlement", vehicle, settlement);

		assertEquals("vehicle location state type is INSIDE_SETTLEMENT", LocationStateType.INSIDE_SETTLEMENT, vehicle.getLocationStateType());
		assertEquals("Person's location state type is INSIDE_VEHICLE", LocationStateType.INSIDE_VEHICLE, person.getLocationStateType());
		
		
        // Vehicle leaves garage
        BuildingManager.removeFromGarage(vehicle);
		assertEquals("Person's location state type is INSIDE_VEHICLE", LocationStateType.INSIDE_VEHICLE, person.getLocationStateType());
		
		assertFalse("Vehicle has left garage", vehicle.isInGarage());

//		assertNull("Person in a vehicle. Vehicle is not in settlement", person.getSettlement());
		
		assertInVehicle("In vehicle", person, vehicle);
		assertTrue("Person in crew", vehicle.getCrew().contains(person));
		
		assertTrue("Transfer person from vehicle back to settlement", person.transfer(settlement));
		assertInsideSettlement("After return", person, settlement);

	}

	
	/*
	 * Test method for 'com.mars_sim.simulation.person.ai.task.LoadVehicle.isFullyLoaded()'
	 */
//	public void testEVAOnPerson() throws Exception {
//		Person person = new Person("Worker Two", settlement);
//		person.initialize(); // TODO This is bad. Why do we have to call a 2nd method after the constructor ???
//        unitManager.addUnit(person);
//
//        person.transfer(settlement,  surface);
//
//		EVASuit suit = new EVASuit("EVA Suit", settlement);
//		unitManager.addUnit(suit);
//		assertTrue("transfer suit to Person", suit.transfer(settlement, person));
//
//		assertEquals("Location state type", LocationStateType.ON_PERSON_OR_ROBOT, suit.getLocationStateType());
//		assertNull("Settlement", suit.getSettlement());
//		
//		assertFalse("InSettlement", suit.isInSettlement());
//		assertFalse("IsInside", suit.isInside());
//		assertFalse("IsOutside", suit.isOutside());
//
//		assertFalse("isInVehicle", suit.isInVehicle());
//		assertNull("Vehicle", suit.getVehicle());
//		
//		assertEquals("Container", person, suit.getContainerUnit());
//		assertEquals("Top container", person, suit.getTopContainerUnit());
//	}
}