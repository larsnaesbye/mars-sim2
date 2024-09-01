/*
 * Mars Simulation Project
 * FutureEventCommand.java
 * @date 2024-08-10
 * @author Barry Evans
 */

package com.mars_sim.console.chat.simcommand.settlement;

import com.mars_sim.console.chat.ChatCommand;
import com.mars_sim.console.chat.Conversation;
import com.mars_sim.console.chat.simcommand.StructuredResponse;
import com.mars_sim.core.events.ScheduledEventManager.ScheduledEvent;
import com.mars_sim.core.structure.Settlement;

/**
 * Command to display settlement future scheduled events.
 * Notes: this is a singleton.
 */
public class FutureEventCommand extends AbstractSettlementCommand {

	public static final ChatCommand FUTURE = new FutureEventCommand();

	private FutureEventCommand() {
		super("fe", "future", "Future scheduled events");
	}

	/** 
	 * @return 
	 */
	@Override
	protected boolean execute(Conversation context, String input, Settlement settlement) {
		StructuredResponse response = new StructuredResponse();
		response.appendTableHeading("When", 24, "Event");
							
		// Display each farm separately
		for (ScheduledEvent event : settlement.getFutureManager().getEvents()) {			
			response.appendTableRow(event.getWhen().getTruncatedDateTimeStamp(), event.getDescription());
		}
		context.println(response.getOutput());
		return true;
	}
}
