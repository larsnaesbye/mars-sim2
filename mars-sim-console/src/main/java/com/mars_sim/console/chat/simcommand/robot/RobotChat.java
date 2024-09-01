/*
 * Mars Simulation Project
 * RobotChat.java
 * @date 2024-08-10
 * @author Barry Evans
 */

package com.mars_sim.console.chat.simcommand.robot;

import java.util.Arrays;
import java.util.List;

import com.mars_sim.console.chat.ChatCommand;
import com.mars_sim.console.chat.command.InteractiveChatCommand;
import com.mars_sim.console.chat.simcommand.ConnectedUnitCommand;
import com.mars_sim.console.chat.simcommand.unit.EquipmentCommand;
import com.mars_sim.console.chat.simcommand.unit.MalfunctionCreateCommand;
import com.mars_sim.console.chat.simcommand.unit.MissionCommand;
import com.mars_sim.console.chat.simcommand.unit.UnitLocationCommand;
import com.mars_sim.console.chat.simcommand.unit.UnitMalfunctionCommand;
import com.mars_sim.console.chat.simcommand.unit.WorkerActivityCommand;
import com.mars_sim.console.chat.simcommand.unit.WorkerAttributeCommand;
import com.mars_sim.console.chat.simcommand.unit.WorkerSkillsCommand;
import com.mars_sim.console.chat.simcommand.unit.WorkerTaskCommand;
import com.mars_sim.console.chat.simcommand.unit.WorkerWorkCommand;
import com.mars_sim.core.robot.Robot;

/**
 * Represents a connection to a Robot.
 */
public class RobotChat extends ConnectedUnitCommand {
	public static final String ROBOT_GROUP = "Robot";

	private static final List<ChatCommand> COMMANDS = Arrays.asList(new WorkerAttributeCommand(ROBOT_GROUP),
																	SystemCommand.SYSTEM,
																	new EquipmentCommand(ROBOT_GROUP),
																	new MissionCommand(ROBOT_GROUP),
																	new WorkerSkillsCommand(ROBOT_GROUP),
																	new UnitMalfunctionCommand(ROBOT_GROUP),
																	new MalfunctionCreateCommand(ROBOT_GROUP),
																	new UnitLocationCommand(ROBOT_GROUP),
																    new WorkerActivityCommand(ROBOT_GROUP),
																    new WorkerTaskCommand(ROBOT_GROUP),
																    new WorkerWorkCommand(ROBOT_GROUP)
																		);

	private Robot robot;

	public RobotChat(Robot robot, InteractiveChatCommand parent) {
		super(robot, COMMANDS, parent);
		this.robot = robot;
	}

	@Override
	public String getIntroduction() {
		StringBuilder sb = new StringBuilder();
		sb.append("Hi, I am a ");
		sb.append(robot.getRobotType());
		sb.append(" called ");
		sb.append(robot.getName());
		sb.append(" based in ");
		sb.append(robot.getSettlement().getName());
		
		return sb.toString();
	}
}
