/**
 * Mars Simulation Project
 * EventCommand.java
 * @version 3.1.2 2020-12-30
 * @author Barry Evans
 */

package org.mars.sim.console.chat.simcommand;

import java.util.List;
import java.util.Objects;

import org.mars.sim.console.chat.ChatCommand;
import org.mars.sim.console.chat.Conversation;
import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.events.HistoricalEventManager;

/**
 * Command to stop speaking with an entity.
 * This is a singleton.
 */
public class EventCommand extends ChatCommand {

	public static final EventCommand EVENT = new EventCommand();

	private static final int EVENT_SIZE = 7;

	private EventCommand() {
		super(TopLevel.SIMULATION_GROUP, "ev", "events", "Display recent events");
	}

	@Override
	public boolean execute(Conversation context, String input) {
		HistoricalEventManager mgr = context.getSim().getEventManager();
		List<HistoricalEvent> events = mgr.getEvents();
		
		if (events.isEmpty()) {
			context.println("None to display");
		}
		else {
			StructuredResponse response = new StructuredResponse();
			
			int latest = events.size() - 1;
			int lastId = Math.max(latest - EVENT_SIZE, 0);
			for(int idx = latest; idx >= lastId; idx--) {
				HistoricalEvent e = events.get(idx);
				String source = Objects.requireNonNullElse(e.getSource(), "").toString();
				
				response.appendHeading(e.getCategory().getName() + " @ " + e.getTimestamp().getDateTimeStamp());
				response.appendLabeledString("Type", e.getType().getName());
				response.appendLabeledString("Source", source);
				response.appendLabeledString("Cause", e.getWhatCause());
				
				// Create location description
				StringBuilder location = new StringBuilder(e.getContainer());
				if (!e.getContainer().equals(e.getCoordinates())) {
					location.append(",").append(e.getCoordinates());
				}
				response.appendLabeledString("Location", location.toString());
				response.appendLabeledString("Settlement", e.getHomeTown());
				
				response.appendBlankLine();
			}
			
			context.println(response.getOutput());
		}
		return true;
	}

}
