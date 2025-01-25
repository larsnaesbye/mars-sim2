/*
 * Mars Simulation Project
 * Phase.java
 * @date 2023-05-06
 * @author Barry Evans
 */

package com.mars_sim.core.structure.building.function.farming;

import java.io.Serializable;

/**
 * Describes a phase in a Crop development
 */
public class Phase implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

	private PhaseType phaseType;
	 /** The work needed [in sols] at this phase. */
	private double workRequired;
	private double percentGrowth;
	private double totalGrowth;


	public Phase(PhaseType phaseType, double workRequired, double percentGrowth, double totalGrowth) {
		this.phaseType = phaseType;
		this.workRequired = workRequired;
		this.percentGrowth = percentGrowth;
		this.totalGrowth = totalGrowth;
	}

	public double getWorkRequired() {
		return workRequired;
	}

	public double getPercentGrowth() {
		return percentGrowth;
	}

	public double getCumulativePercentGrowth() {
		return totalGrowth;
	}	

	public PhaseType getPhaseType() {
		return phaseType;
	}

	@Override
	public String toString() {
		return phaseType.name();
	}
}
