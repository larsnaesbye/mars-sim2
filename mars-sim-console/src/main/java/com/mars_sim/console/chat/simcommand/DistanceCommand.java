/*
 * Mars Simulation Project
 * DistanceCommand.java
 * @date 2022-07-15
 * @author Barry Evans
 */

package com.mars_sim.console.chat.simcommand;

import com.mars_sim.console.chat.ChatCommand;
import com.mars_sim.console.chat.Conversation;
import com.mars_sim.core.Unit;
import com.mars_sim.core.map.location.Coordinates;

/**
 * Get the distance to a specific location. The starting point is the location of the calling
 * Unit.
 */
public class DistanceCommand extends ChatCommand {

	public static final ChatCommand DISTANCE = new DistanceCommand();

	private DistanceCommand() {
		super(TopLevel.SIMULATION_GROUP, "di", "distance", "Distance to a destination");
		setInteractive(true);
	}

	@Override
	public boolean execute(Conversation context, String input) {
		Coordinates start = null;
		
		// If a Unit then that is the start location
		ChatCommand parent = context.getCurrentCommand();
		if (parent instanceof ConnectedUnitCommand) {
			Unit source = ((ConnectedUnitCommand)context.getCurrentCommand()).getUnit();
			start = source.getCoordinates();
			context.println("Start location is " + start);
		}
		else {
			// Ask user
			start = CommandHelper.getCoordinates("Start", context);			
		}
		
		boolean result = false;
		// If a start then continue
		if (start != null) {
			Coordinates end = CommandHelper.getCoordinates("Destination", context);
			if (end != null) {
				double distance = start.getDistance(end);
				result  = true;
				
				context.println("The distance between (" + start.getFormattedString() + ") and ("
						+ end.getFormattedString() + ") is " + Math.round(distance *1_000.0)/1_000.0 + " km");
			}
		}
		
		return result;
	}
}
