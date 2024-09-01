/*
 * Mars Simulation Project
 * FoodType.java
 * @date 2022-06-25
 * @author Manny Kung
 */

package com.mars_sim.core.food;

import com.mars_sim.core.tool.Msg;

public enum FoodType {

	ANIMAL				(Msg.getString("FoodType.animal")), //$NON-NLS-1$ 
	CHEMICAL			(Msg.getString("FoodType.chemical")), //$NON-NLS-1$ 
	CROP				(Msg.getString("FoodType.crop")), //$NON-NLS-1$ 
	DERIVED				(Msg.getString("FoodType.derived")), //$NON-NLS-1$
	INSECT				(Msg.getString("FoodType.insect")), //$NON-NLS-1$ 
	OIL					(Msg.getString("FoodType.oil")), //$NON-NLS-1$ 
	ORGANISM			(Msg.getString("FoodType.organism")), //$NON-NLS-1$ 
	SOY_BASED			(Msg.getString("FoodType.soyBased")), //$NON-NLS-1$ 
	TISSUE				(Msg.getString("FoodType.tissue")); //$NON-NLS-1$ 
	
	private String name;

	private FoodType(String name) {
		this.name = name;
	}

	public String getName() {
		return this.name;
	}
	
	public String toString() {
		return this.name;
	}
}
