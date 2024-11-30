package com.mars_sim.core.structure.building;


import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.mars_sim.core.AbstractMarsSimUnitTest;
import com.mars_sim.core.SimulationConfig;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.science.ScienceType;
import com.mars_sim.core.structure.building.function.FunctionType;


public class BuildingConfigTest extends AbstractMarsSimUnitTest {

    private static final String LANDER_HAB = "Lander Hab";

    public void testLanderHabFunctions() {
        var bc = simConfig.getBuildingConfiguration();


        BuildingSpec spec = bc.getBuildingSpec(LANDER_HAB);
        assertNotNull("Building spec " + LANDER_HAB, spec);

        // Lander Hab has many functions
        assertEquals("Number of Functions in " + LANDER_HAB, 22, spec.getFunctionSupported().size());
    }

    public void testLanderHabActivitySpots() {
        var bc = simConfig.getBuildingConfiguration();


        BuildingSpec spec = bc.getBuildingSpec(LANDER_HAB);

        // Check names of beds but could check any Function
        FunctionSpec beds = spec.getFunctionSpec(FunctionType.LIVING_ACCOMMODATION);
        assertTrue("Has of beds in " + LANDER_HAB, !beds.getActivitySpots().isEmpty());

        for(var b : beds.getActivitySpots()) {
            assertTrue("Bed name", b.name().startsWith("Bed #"));
        }
        Set<String> names = beds.getActivitySpots().stream()
                                .map(NamedPosition::name)
                                .collect(Collectors.toSet());
        assertEquals("Number of unique beds names", beds.getActivitySpots().size(), names.size());
    }

    /**
     * This tets is very tied to the building spec of LANDER_HAB
     */
    public void testLanderHabNamedSpots() {
        var bc = simConfig.getBuildingConfiguration();


        BuildingSpec spec = bc.getBuildingSpec(LANDER_HAB);

        // Check names of exercise stations but could check any Function
        FunctionSpec exercise = spec.getFunctionSpec(FunctionType.EXERCISE);
        assertTrue("Has of exercise in " + LANDER_HAB, !exercise.getActivitySpots().isEmpty());

        Set<String> names = exercise.getActivitySpots().stream()
                                .map(NamedPosition::name)
                                .collect(Collectors.toSet());
        assertTrue("Exercise spot called 'Bike'", names.contains("Bike"));
        assertTrue("Exercise spot called 'Running Machine'", names.contains("Running Machine"));
    }

        /**
     * This tets is very tied to the building spec of LANDER_HAB
     */
    public void testInflatableGreenhouse() {
	    var simConfig = SimulationConfig.instance();
        simConfig.loadConfig();
        var bc = simConfig.getBuildingConfiguration();

        var found = bc.getBuildingSpec("Inflatable Greenhouse");
        
        assertNotNull("Found", found);

        assertEquals("width", 6D, found.getWidth());
        assertEquals("length", 9D, found.getLength());
        assertEquals("width", 6D, found.getWidth());
        assertEquals("width", 3000D, found.getBaseMass());

        assertEquals("Construction", ConstructionType.INFLATABLE, found.getConstruction());

        assertEquals("Functions", Set.of(FunctionType.FARMING, FunctionType.LIFE_SUPPORT,
                                         FunctionType.POWER_GENERATION, FunctionType.POWER_STORAGE,
                                         FunctionType.RECREATION, FunctionType.RESEARCH,
                                         FunctionType.RESOURCE_PROCESSING, FunctionType.ROBOTIC_STATION,
                                         FunctionType.STORAGE, FunctionType.THERMAL_GENERATION),
                                        found.getFunctionSupported());

        
        var storage = found.getStorage();
        assertEquals("Oxygen capacity", 5000D, storage.get(ResourceUtil.oxygenID));
        assertEquals("Nitrogen capacity", 2500D, storage.get(ResourceUtil.nitrogenID));

        var initial = found.getInitialResources();
        assertEquals("Carbon stored", 100D, initial.get(ResourceUtil.co2ID));

        assertEquals("Science research", Set.of(ScienceType.BIOLOGY, ScienceType.BOTANY,
                                                ScienceType.CHEMISTRY), new HashSet<>(found.getScienceType()));
    }
}
