/*
 * Mars Simulation Project
 * FuelHeatSource.java
 * @date 2021-10-21
 * @author Manny Kung
 */
package org.mars_sim.msp.core.structure.building.function;

import java.io.Serializable;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;

public class FuelHeatSource extends HeatSource implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static final Logger logger = Logger.getLogger(FuelHeatSource.class.getName());

	/** The work time (millisol) required to toggle this heat source on or off. */
	public static final double TOGGLE_RUNNING_WORK_TIME_REQUIRED = 10D;

	public static final double THERMAL_EFFICIENCY = .9;

	public static final double ELECTRIC_EFFICIENCY = FuelPowerSource.ELECTRICAL_EFFICIENCY;

	private double rate;

	private double toggleRunningWorkTime;

	private double maxFuel;

	private double time;

	private boolean toggle = false;

	private static int oxygenID = ResourceUtil.oxygenID;
	private static int methaneID = ResourceUtil.methaneID;
	
	private Building building;

	/**
	 * Constructor.
	 * 
	 * @param _maxHeat          the maximum power/heat (kW) of the heat source.
	 * @param _toggle           if the heat source is toggled on or off.
	 * @param fuelType          the fuel type.
	 * @param _consumptionSpeed the rate of fuel consumption (kg/Sol).
	 */
	public FuelHeatSource(Building building, double maxHeat, boolean toggle, String fuelType, double consumptionSpeed) {
		super(HeatSourceType.FUEL_HEATING, maxHeat);
		this.rate = consumptionSpeed;
		this.toggle = toggle;
		this.building = building;
	}

//     Note : every mole of methane (16 g) releases 810 KJ of energy if burning with 2 moles of oxygen (64 g)
//	 CH4(g) + 2O2(g) --> CO2(g) + 2 H2O(g), deltaH = -890 kJ 
//	 
// 	 CnH2n+2 + (3n + 1)O2 -> nCO2 + (n + 1)H2O + (6n + 2)e- 
//
//	 Assume thermal efficiency at 41%,
//	 364.9 kW needs 16 g/s 
//	 1 kW_t <- 3.8926 g/millisol or 3.8926 kg/sol	 
//	 
//	 Assume thermal efficiency at 100%,
//	 1 kW_t <- 1.5960 g/millisol or kg/sol
//	 
//	 SOFC uses methane with 1100 W-hr/kg, 
//	 This translate to 71.25 % efficiency
//	
// 	 Use of heat will push it up to 85%
//	 see http://www.nfcrc.uci.edu/3/FUEL_CELL_INFORMATION/FCexplained/FC_benefits.aspx
//	 
//	 or 90% see https://phys.org/news/2017-07-hydrocarbon-fuel-cells-high-efficiency.html 

	private double consumeFuel(double time, Settlement settlement) {

		double rate_millisol = rate / 1000D;

		maxFuel = (getPercentagePower() * time * rate_millisol)/100D;
		// System.out.println("maxFuel : "+maxFuel);
		double consumed = 0;

		double fuelStored = settlement.getAmountResourceStored(methaneID);
		double o2Stored = settlement.getAmountResourceStored(oxygenID);

		// Note that 16 g of methane requires 64 g of oxygen, a 1 to 4 ratio
		consumed = Math.min(maxFuel, Math.min(fuelStored, o2Stored / 4D));

//		boolean a = (fuelStored >= maxFuel);
//		boolean b = (o2Stored >= 4D * maxFuel);
//		
//		if (a && b) {
//			consumed = maxFuel;
//		}
//		else if (a && !b) {
//			consumed = o2Stored/4D;
//		}
//		else if (!a && b) {
//			consumed = maxFuel;
//		}	
//		else if (!a && !b) {
//			consumed = Math.min(fuelStored, o2Stored/4D);
//		}				

		settlement.retrieveAmountResource(methaneID, consumed);
		settlement.retrieveAmountResource(oxygenID, 4D * consumed);

//		inv.addAmountDemandTotalRequest(methaneID, consumed);
//		inv.addAmountDemand(methaneID, consumed);
//
//		inv.addAmountDemandTotalRequest(oxygenID, consumed);
//		inv.addAmountDemand(oxygenID, 4D * consumed);

		return consumed;
	}

	/**
	 * Gets the amount resource used as fuel.
	 * 
	 * @return amount resource.
	 */
	public int getFuelResourceID() {
		return methaneID;
	}

//	/**
//	 * Gets the amount resource used as fuel.
//	 * @return amount resource.
//	 */
//	 public AmountResource getFuelResource() {
//		return methaneAR;
//	}

	/**
	 * Gets the rate the fuel is consumed.
	 * 
	 * @return rate (kg/Sol).
	 */
	public double getFuelConsumptionRate() {
		return rate;
	}

	@Override
	public double getCurrentHeat(Building building) {

		if (toggle) {
			double spentFuel = consumeFuel(time, building.getSettlement());
			return getMaxHeat() * spentFuel / maxFuel * THERMAL_EFFICIENCY;
		}

		return 0;
	}

	@Override
	public double getAverageHeat(Settlement settlement) {
		double fuelHeat = getMaxHeat();
		double fuelValue = settlement.getGoodsManager().getGoodValuePerItem(methaneID);
		fuelValue *= getFuelConsumptionRate();
		fuelHeat -= fuelValue;
		if (fuelHeat < 0D)
			fuelHeat = 0D;
		return fuelHeat;
	}

	@Override
	public double getMaintenanceTime() {
		return getMaxHeat() * 2D;
	}

	@Override
	public double getEfficiency() {
		return 1;
	}

	@Override
	public double getCurrentPower(Building building) {

		if (toggle) {
			double spentFuel = consumeFuel(time, building.getSettlement());
			return getMaxHeat() * spentFuel / maxFuel * ELECTRIC_EFFICIENCY;
		}

		return 0;
	}

	/**
	 * Adds work time to toggling the heat source on or off.
	 * 
	 * @param time the amount (millisols) of time to add.
	 */
	public void addToggleWorkTime(double time) {
		toggleRunningWorkTime += time;
		if (toggleRunningWorkTime >= TOGGLE_RUNNING_WORK_TIME_REQUIRED) {
			toggleRunningWorkTime = 0D;
			toggle = !toggle;
			if (toggle)
				logger.info(building.getNickName() + "- " + Msg.getString("FuelHeatSource.log.turnedOn", getType().getName())); //$NON-NLS-1$
			else
				logger.info(building.getNickName() + "- " + Msg.getString("FuelHeatSource.log.turnedOff", getType().getName())); //$NON-NLS-1$
		}
	}

	public void toggleON() {
		toggle = true;
	}

	public void toggleOFF() {
		toggle = false;
	}

	public boolean isToggleON() {
		return toggle;
	}

	@Override
	public void setTime(double time) {
		this.time = time; // default is 0.12255668934010477 or 0.12255668934010477
	}

	@Override
	public void setPercentagePower(int percentage) {
		super.setPercentagePower(percentage);
		toggle = (percentage != 75) && (percentage != 0); // 75% does not need toggle ???
	}
}
