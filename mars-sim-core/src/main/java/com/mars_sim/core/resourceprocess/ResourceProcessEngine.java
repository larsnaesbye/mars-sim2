/*
 * Mars Simulation Project
 * ResourceProcessEngine.java
 * @date 2022-10-23
 * @author Barry Evans
 */
package com.mars_sim.core.resourceprocess;

import java.io.Serializable;

/**
 * This class represents an instance of a ResourceProcessing engine that hosts a Resource process and has
 * a number of modules. The input, output and power of the processSpec is multiplied by the number of modules.
 * It is a shared configuration entity.
 */
public class ResourceProcessEngine implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

    private int modules;
    private ResourceProcessSpec processSpec;

    public ResourceProcessEngine(ResourceProcessSpec processSpec, int modules) {
        this.modules = modules;
        this.processSpec = processSpec;
    }

    /**
     * What process does this engine support
     * @return
     */
    public ResourceProcessSpec getProcessSpec() {
        return processSpec;
    }

    public double getBaseFullInputRate(Integer resource) {
        return processSpec.getBaseInputRate(resource) * modules;
    }

    public double getBaseFullOutputRate(Integer resource) {
        return processSpec.getBaseOutputRate(resource) * modules;
    }
    
    /**
     * How many modules does this resource process engine have ?
     */
    public int getModules() {
        return modules;
    }
}
