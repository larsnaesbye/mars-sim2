/*
 * Mars Simulation Project
 * UnitLocationCommand.java
 * @date 2024-08-10
 * @author Barry Evans
 */

package com.mars_sim.console.chat.simcommand.unit;

import com.mars_sim.console.chat.Conversation;
import com.mars_sim.core.Unit;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.vehicle.Vehicle;

/**
 * Command to stop speaking with an entity.
 * This is a singleton.
 */
public class UnitLocationCommand extends AbstractUnitCommand {


	public UnitLocationCommand(String groupName) {
		super(groupName, "l", "location", "What is the location.");
	}

	/** 
	 * Outputs the current immediate location of the Unit.
	 */
	@Override
	protected boolean execute(Conversation context, String input, Unit source) {
		var message = new StringBuilder();
		if (source.isOutside()) {
			message.append("On the surface @ ")
				   .append(source.getCoordinates().getFormattedString());
		}
		else if (source.isInVehicle()) {
			Vehicle v = source.getVehicle();
			message.append("In ").append(v.getName());
			message.append(" @ ").append(v.getCoordinates().getFormattedString());
			if (source.isInSettlement()) {
				message.append(", warning also isIsSettlement=true");
			}
		}
		else {
			Settlement base = source.getSettlement();
			Building building = source.getBuildingLocation();
			message.append("In ");
			if (building != null) {
				message.append(building.getName()).append(" @ ");
			}

			message.append(base.getName());				
		}

		context.println(message.toString());
		return true;
	}

}
