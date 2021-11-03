/*
 * Mars Simulation Project
 * Scenario.java
 * @date 2021-08-20
 * @author Barry Evans
 */
package org.mars_sim.msp.core.configuration;

import java.util.List;

import org.mars_sim.msp.core.interplanetary.transport.settlement.ArrivingSettlement;
import org.mars_sim.msp.core.structure.InitialSettlement;

/**
 * This represents a certain scenario that is used to bootstrap a new Simulation.
 */
public class Scenario implements UserConfigurable {

	private String description;
	private boolean bundled;
	private String name;
	private List<InitialSettlement> settlements;
	private List<ArrivingSettlement> arrivals;

	public Scenario(String name, String description, List<InitialSettlement> settlements,
			List<ArrivingSettlement> arrivals, boolean bundled) {
		super();
		this.name = name;
		this.description = description;
		this.bundled = bundled;
		this.settlements = settlements;
		this.arrivals = arrivals;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isBundled() {
		return bundled;
	}

	/**
	 * The initial settlements associated with this scenario.
	 * @return
	 */
	public List<ArrivingSettlement> getArrivals() {
		return arrivals;
	}
	
	/**
	 * The initial settlements associated with this scenario.
	 * @return
	 */
	public List<InitialSettlement> getSettlements() {
		return settlements;
	}
}
