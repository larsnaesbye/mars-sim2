/*
 * Mars Simulation Project
 * Conversation.java
 * @date 2023-06-14
 * @author Barry Evans
 */

package com.mars_sim.console.chat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mars_sim.console.chat.command.InteractiveChatCommand;
import com.mars_sim.core.Simulation;

/**
 * Establishes a Conversation with a user.
 */
public class Conversation implements UserOutbound {
	
	public static final String AUTO_COMPLETE_KEY = "tab";
	public static final String HISTORY_BACK_KEY = "up";
	public static final String HISTORY_FORWARD_KEY = "down";
	public static final String CANCEL_KEY = "escape";
	
    private static final Logger LOGGER = Logger.getLogger(Conversation.class.getName());
	
	private List<String> inputHistory;
	private int historyIdx = 0;
	
	private Deque<InteractiveChatCommand> previous;
	private InteractiveChatCommand current;
	private CancellableCommand activeCommand;
	private UserChannel comms;

	private boolean active;
	private List<String> options;
	private int optionsIdx;
	private Object optionsPartial;

	private Simulation sim;
	private Set<ConversationRole> roles = null;
	
	/**
	 * Starts a conversation with the user using a Comms Channel starting with a certain command.
	 * 
	 * @param comms an instance of UserChannel
	 * @param initial an instance of InteractiveChatCommand
	 * @param roles a set of ConversationRole
	 * @param sim an instance of Simulation
	 */
	public Conversation(UserChannel comms, InteractiveChatCommand initial, Set<ConversationRole> roles,
						Simulation sim) {
		this.current = initial;
        this.active = true;
        this.comms = comms;
        this.roles = roles;
        this.previous = new ArrayDeque<>();
        this.inputHistory = new ArrayList<>();
        
        this.sim = sim;

        comms.registerHandler(AUTO_COMPLETE_KEY, this, false);
        comms.registerHandler(HISTORY_BACK_KEY, this, false);
        comms.registerHandler(HISTORY_FORWARD_KEY, this, false);
        comms.registerHandler(CANCEL_KEY, this, true);
	}
	
	/**
	 * Return the parent command of the current one; which is the last in the Deck
	 */
	public InteractiveChatCommand getParentCommand() {
		if (!previous.isEmpty()) {
			return previous.getLast();
		}
		return null;
	}
	
	public InteractiveChatCommand getCurrentCommand() {
		return current;
	}
	
	/**
	 * Updates the current chat command and potentially remember it for later.
	 * 
	 * @param newCommand New chat
	 * @param remember Push this in the stack of previous commands
	 */
	public void setCurrentCommand(InteractiveChatCommand newCommand, boolean remember) {
		if (remember) {
			previous.push(this.current);
		}
		this.current = newCommand;
	}
	
    public void setCompleted() {
    	active = false;
    }
    
	/**
	 * Interacts with the end user.
	 */
    public void interact() {
    	if (current == null) {
    		throw new IllegalStateException("There is no current command");
    	}
    	
    	InteractiveChatCommand lastCurrent = null;
		while (active) {
			// A new chat so let it welcome itself
			if (current != lastCurrent) {
	        	String preamble = current.getIntroduction();
	        	if (preamble != null) {
	        		println(preamble);
				}
	        	lastCurrent = current;
			}
			
	      	try {
	        	// Get the input
	      		getInput();
        	}
        	catch (RuntimeException rte) {
        		printProblem(rte);
        	}
        }
		
		comms.close();
    }

    /**
     * Gets the input.
     */
    private void getInput() {
    	String prompt = current.getPrompt(this) + " > ";
    	String input = getInput(prompt);
    	if (input.length() > 0) {
			options = null; // Remove any auto complete options once user executes
			
			// Update history
			boolean addToHistory = true; 
			if (!inputHistory.isEmpty()) {
				// Do not accept repeated commands
				String lastCommand = inputHistory.get(inputHistory.size() - 1);
				addToHistory = !input.equals(lastCommand);
			}
			if (addToHistory) {
				inputHistory.add(input);
			}
			
			// Always set the history pointer to the most recent command
			historyIdx = inputHistory.size();

			current.execute(this, input);
		}
    }
    
    private void printProblem(Exception rte) {
		LOGGER.log(Level.SEVERE, "Problem executing command ", rte);
		
		StringWriter writer = new StringWriter();
		PrintWriter out = new PrintWriter(writer);
		rte.printStackTrace(out);
		println("Sorry I had a problem doing that " + rte.getMessage());
		println(writer.toString());
    }
    
	/**
	 * Resets the current command to the previous one.
	 */
	public void resetCommand() {
		current = previous.pop();
	}

	public String getInput(String prompt) {
		return comms.getInput(prompt);
	}


	public void println(String text) {
		comms.println(text);
	}


	public void print(String text) {
		comms.print(text);
	}

	public CancellableCommand getActiveCommand() {
		return activeCommand;
	}

	public void setActiveCommand(CancellableCommand activeCommand) {
		this.activeCommand = activeCommand;
	}

	/**
	 * User has pressed a special key that is listened for.
	 * 
	 * @param keyStroke Key pressed
	 */
	@Override
	public void keyStrokeApplied(String key) { 		
		switch(key) {
		case AUTO_COMPLETE_KEY:
			autoComplete();
			break;
			
		case HISTORY_BACK_KEY:
			replayHistory(-1);
			break;
			
		case HISTORY_FORWARD_KEY:
			replayHistory(1);
			break;
			
		case CANCEL_KEY:
			cancelCommand();
			break;
		
		default:
			LOGGER.warning("Unexpected keystroke " + key);
			break;
		}
	}

	/**
	 * Cancels the current command that is registered.
	 */
	private void cancelCommand() {
		if (activeCommand != null) {
			println("Cancelling command. Please wait......");
			activeCommand.cancel();
			activeCommand = null;
		}
	}

	private void replayHistory(int offset) {
		historyIdx += offset;
		historyIdx = Math.max(historyIdx, 0);
		historyIdx = Math.min(historyIdx, inputHistory.size()-1);
		
		// Check within range and replace
		if ((historyIdx >= 0) && (historyIdx < inputHistory.size())) {
			comms.replaceUserInput(inputHistory.get(historyIdx));	
		}
	}

	/**
	 * User wants to do auto complete on partial input.
	 */
	private void autoComplete() {
		String partialInString = comms.getPartialInput();
		
		// So no complete option or user has changed partial input
		if ((options == null) || !partialInString.equals(optionsPartial)) {
			options = current.getAutoComplete(this, partialInString);
			Collections.sort(options);
			
			// Reset pointer
			optionsIdx = 0;
			optionsPartial = partialInString;
		}
		
		// If there are options
		if (!options.isEmpty()) {
			if (optionsIdx >= options.size()) {
				optionsIdx = 0;
			}
			
			// Replace the user input
			comms.replaceUserInput(options.get(optionsIdx));
			
			optionsIdx++; // ready for next one
		}
	}

	public Set<ConversationRole> getRoles() {
		return roles;
	}
	
	public void setRoles(Set<ConversationRole> newRoles) {
		this.roles = newRoles;
		
		// Clear the stacked interact commands
		current.resetCache(this);
		
		for (InteractiveChatCommand i : previous) {
			i.resetCache(this);
		}
	}
	
	public Simulation getSim() {
		return sim;
	}

	/**
	 * Gets a integer input.
	 * 
	 * @param prompt
	 * @return
	 */
	public int getIntInput(String prompt) {
		String response = getInput(prompt);

		int newLevel = -1;
		if (!response.isBlank()) {
			try {
				newLevel = Integer.parseInt(response);
			}
			catch (NumberFormatException e) {
				println("Sorry, not a valid entry.");
			}
		}
		return newLevel;
	}
}
