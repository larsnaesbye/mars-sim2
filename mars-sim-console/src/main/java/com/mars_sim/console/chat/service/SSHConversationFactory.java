/*
 * Mars Simulation Project
 * SSHConversationFactory.java
 * @date 2024-08-10
 * @author Barry Evans
 */

package com.mars_sim.console.chat.service;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.sshd.core.CoreModuleProperties;
import org.apache.sshd.server.channel.ChannelSession;
import org.apache.sshd.server.command.Command;
import org.apache.sshd.server.shell.ShellFactory;
import com.mars_sim.core.Simulation;

class SSHConversationFactory implements ShellFactory {

	private ExecutorService executor;
	private RemoteChatService parent;
	
	public SSHConversationFactory(RemoteChatService remoteChatService) {
		this.executor = Executors.newCachedThreadPool();
		this.parent = remoteChatService;
	}
	
	@Override
	public Command createShell(ChannelSession channel) throws IOException {
		
		// Put a big idle timeout
		CoreModuleProperties.IDLE_TIMEOUT.set(channel.getServerSession(), Duration.ofMillis(60 * 60 * 1000L));
		
		return new SSHChannel(executor, parent, Simulation.instance());
	}

}
