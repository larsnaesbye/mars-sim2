/*
 * Mars Simulation Project
 * GoodCategory.java
 * @date 2021-06-20
 * @author stpa
 */
package com.mars_sim.core.goods;

import com.mars_sim.core.tool.Msg;

public enum GoodCategory {

	AMOUNT_RESOURCE 	(Msg.getString("GoodCategory.amountResource")), //$NON-NLS-1$
	ITEM_RESOURCE 		(Msg.getString("GoodCategory.itemResource")), //$NON-NLS-1$
	EQUIPMENT 			(Msg.getString("GoodCategory.equipment")), //$NON-NLS-1$
	BIN 				(Msg.getString("GoodCategory.bin")), //$NON-NLS-1$	
	CONTAINER 			(Msg.getString("GoodCategory.container")), //$NON-NLS-1$
	VEHICLE 			(Msg.getString("GoodCategory.vehicle")), //$NON-NLS-1$
	ROBOT 				(Msg.getString("GoodCategory.robot")); //$NON-NLS-1$
	
	private String msgKey;

	private GoodCategory(String msgKey) {
		this.msgKey = msgKey;
	}

	public String getMsgKey() {
		return this.msgKey;
	}
}
