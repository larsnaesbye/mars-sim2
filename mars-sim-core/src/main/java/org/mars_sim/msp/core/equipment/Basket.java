/*
 * Mars Simulation Project
 * Basket.java
 * @date 2023-07-12
 * @author Manny Kung
 */

package org.mars_sim.msp.core.equipment;

import org.mars_sim.msp.core.structure.building.function.farming.UnitEntity;

public class Basket extends AmountResourceBin {
	
	/** default serial id. */
	private static final long serialVersionUID = 1L;

	public static double CAP = 100;
	
	public Basket(UnitEntity unitEntity, double cap) {
		super(unitEntity, cap);

		setBinType(BinType.BASKET);
	}
}
