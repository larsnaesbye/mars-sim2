package com.mars_sim.core.resourceprocess;

import java.io.IOException;

import org.jdom2.JDOMException;

import com.mars_sim.core.AbstractMarsSimUnitTest;
import com.mars_sim.core.SimulationConfig;
import com.mars_sim.core.resource.ResourceUtil;

public class ResourceProcessConfigTest extends AbstractMarsSimUnitTest {
    private ResourceProcessConfig getResourceProcessConfig() throws JDOMException, IOException {
        return new ResourceProcessConfig(getConfig().parseXMLFileAsJDOMDocument(
                    SimulationConfig.RESPROCESS_FILE, true));
    }
    
    public void testRGWSReactor() throws JDOMException, IOException {
        var rConfig = getResourceProcessConfig();

        var name = "Sabatier RWGS Reactor";
        var spec = rConfig.getProcessSpec(name);

        assertNotNull("Found " + name, spec);
        assertEquals("Name", name, spec.getName());

        // Check inputs
        var inputs = spec.getInputResources();
        assertEquals("Input resources", 2, inputs.size());
        assertTrue("Inputs contains hydrogen", inputs.contains(ResourceUtil.hydrogenID));
        assertTrue("Inputs contains CO2", inputs.contains(ResourceUtil.co2ID));

        // Check minumum
        var mins = spec.getMinimumInputs();
        assertEquals("Minumum resources", 1, mins.size());
        assertTrue("Minimums contains", mins.keySet().contains(ResourceUtil.hydrogenID));
    }
}
