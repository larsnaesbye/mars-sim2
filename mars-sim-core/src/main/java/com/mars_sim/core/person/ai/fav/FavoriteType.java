/*
 * Mars Simulation Project
 * FavoriteType.java
 * @date 2022-08-01
 * @author Manny Kung
 */

package com.mars_sim.core.person.ai.fav;

import java.util.Arrays;
import java.util.List;

import com.mars_sim.core.tool.Msg;

public enum FavoriteType {

	ASTRONOMY	 				(Msg.getString("FavoriteType.astronomy")), //$NON-NLS-1$
	COOKING						(Msg.getString("FavoriteType.cooking")), //$NON-NLS-1$
	FIELD_WORK	 				(Msg.getString("FavoriteType.fieldWork")), //$NON-NLS-1$
	GAMING						(Msg.getString("FavoriteType.gaming")), //$NON-NLS-1$
	LAB_EXPERIMENTATION			(Msg.getString("FavoriteType.labExperimentation")), //$NON-NLS-1$
	OPERATION		 			(Msg.getString("FavoriteType.operation")), //$NON-NLS-1$
	RESEARCH 	 				(Msg.getString("FavoriteType.research")), //$NON-NLS-1$
	SPORT 		 				(Msg.getString("FavoriteType.sport")), //$NON-NLS-1$
	TENDING_FARM				(Msg.getString("FavoriteType.tendingFarm")), //$NON-NLS-1$
	TINKERING	 				(Msg.getString("FavoriteType.tinkering")), //$NON-NLS-1$
	;

	static FavoriteType[] availableFavoriteTypes = new FavoriteType[] { 	
			ASTRONOMY,
			COOKING,
			FIELD_WORK,
			GAMING,
			LAB_EXPERIMENTATION,
			OPERATION,
			RESEARCH,
			SPORT,
			TENDING_FARM,	
			TINKERING
			};
	
	private String name;

	/** hidden constructor. */
	private FavoriteType(String name) {
		this.name = name;
	}

	public final String getName() {
		return this.name;
	}

	@Override
	public final String toString() {
		return getName();
	}
	
	public static FavoriteType fromString(String name) {
		if (name != null) {
	    	for (FavoriteType f : FavoriteType.values()) {
	    		if (name.equalsIgnoreCase(f.name)) {
	    			return f;
	    		}
	    	}
		}
		
		return null;
	}

	/**
	 * gives back a list of all valid values for the FavoriteType enum.
	 */
	public static List<FavoriteType> valuesList() {
		return Arrays.asList(FavoriteType.values());
		// Arrays.asList() returns an ArrayList which is a private static class inside Arrays. 
		// It is not an java.util.ArrayList class.
		// Could possibly reconfigure this method as follows: 
		// public ArrayList<FavoriteType> valuesList() {
		// 	return new ArrayList<FavoriteType>(Arrays.asList(FavoriteType.values())); }
	}
}
