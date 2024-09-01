/*
 * Mars Simulation Project
 * DiagnosticsCommand.java
 * @date 2024-08-10
 * @author Barry Evans
 */

package com.mars_sim.console.chat.simcommand;

import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.mars_sim.console.chat.ChatCommand;
import com.mars_sim.console.chat.Conversation;
import com.mars_sim.console.chat.ConversationRole;
import com.mars_sim.core.logging.DiagnosticsManager;

/**
 * Controls which diagnostics are enabled.
 */
public class DiagnosticsCommand extends ChatCommand {
	public static final ChatCommand DIAGNOSTICS = new DiagnosticsCommand();
	

	private DiagnosticsCommand() {
		super(TopLevel.SIMULATION_GROUP, "dg", "diagnostics", "Change a module diagnostic logging");
		addRequiredRole(ConversationRole.ADMIN);
	}

	@Override
	public boolean execute(Conversation context, String input) {
		String module = null;
		boolean enabled = true;
		if (input != null) {
			String []parts = input.split(" ");
			if (parts.length >= 2) {
				// Stripp of the flag
				module = input.substring(0, input.length()-2).trim();

				String flag = parts[parts.length-1];
				if (flag.equalsIgnoreCase("Y")) {
					enabled = true;
				}
				else if (flag.equalsIgnoreCase("N")) {
					enabled = false;
				}
				else {
					module = null;
				}
			}
		}
				
		boolean result = false;
		if (module != null) {
			// Apply the change
			context.println((enabled ? "Enabling" : "Disabling")
					+ " diagnostics on module " + module);
			try {
				result = DiagnosticsManager.setDiagnostics(module.trim(), enabled);
			} catch (FileNotFoundException e) {
				context.println("Problem with diagnostics file "
								+ e.getMessage());	
			}
		}
		if (!result) {
			printHelp(context);
		}
		return result;
	}
	
	private void printHelp(Conversation context) {
		String modules = Arrays.stream(DiagnosticsManager.MODULE_NAMES).collect(
						Collectors.joining(",", "'", "'"));
		context.println("Sorry wrong format. Must have arguments of <module name> <Y|N>");
		context.println("Module names are " + modules);
	}
}
