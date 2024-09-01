/*
 * Mars Simulation Project
 * SSHConversation.java
 * @date 2024-08-10
 * @author Barry Evans
 */

package com.mars_sim.console.chat.service;

import java.util.Set;

import com.mars_sim.console.chat.Conversation;
import com.mars_sim.console.chat.ConversationRole;
import com.mars_sim.core.Simulation;

public class SSHConversation extends Conversation {

	private String username;
	private RemoteChatService parent;

	public SSHConversation(RemoteChatService parent, SSHChannel sshChannel, String username,
						   Set<ConversationRole> roles, Simulation sim) {
		super(sshChannel, new RemoteTopLevel(username), roles, sim);
		this.username = username;
		this.parent = parent;
	}

	public RemoteChatService getService() {
		return parent;
	}
	
	public String getUsername() {
		return username;
	}

}
