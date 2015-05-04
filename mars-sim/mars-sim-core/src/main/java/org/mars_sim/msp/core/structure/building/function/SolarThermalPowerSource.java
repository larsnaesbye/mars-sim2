/**
 * Mars Simulation Project
 * SolarThermalPowerSource.java
 * @version 3.07 2014-12-06
 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;

/**
 * A solar thermal power source.
 */
public class SolarThermalPowerSource
extends PowerSource
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static double efficiency_solar_thermal = .70;

	private Coordinates location ;
	private SurfaceFeatures surface ;
	/**
	 * Constructor.
	 * @param maxPower the maximum generated power.
	 */
	public SolarThermalPowerSource(double maxPower) {
		// Call PowerSource constructor.
		super(PowerSourceType.SOLAR_THERMAL, maxPower);
	}

	public static double getEfficiency() {
		return efficiency_solar_thermal;
	}

	@Override
	public double getCurrentPower(Building building) {
		BuildingManager manager = building.getBuildingManager();
		if (location == null)
			location = manager.getSettlement().getCoordinates();
		if (surface == null)
			surface = Simulation.instance().getMars().getSurfaceFeatures();
		//double sunlight = surface.getSurfaceSunlight(location);
		double sunlight = surface.getSolarIrradiance(location) * efficiency_solar_thermal / 600D ; // tentatively normalized to 600 W
		//TODO: need to account for the system specs of the panel such as area, efficiency, and wattage, etc.		// Solar thermal mirror only works in direct sunlight.
		//if (sunlight == 1D) return getMaxPower();
		//else return 0D;
		return sunlight * getMaxPower();
	}

	@Override
	public double getAveragePower(Settlement settlement) {
		return getMaxPower() / 2.5D;
	}

	@Override
	public double getMaintenanceTime() {
	    return getMaxPower() * 2D;
	}
}