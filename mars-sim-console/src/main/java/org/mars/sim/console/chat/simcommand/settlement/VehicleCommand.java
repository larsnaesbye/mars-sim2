/**
 * Mars Simulation Project
 * VehicleCommand.java
 * @version 3.1.2 2020-12-30
 * @author Barry Evans
 */

package org.mars.sim.console.chat.simcommand.settlement;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.mars.sim.console.chat.ChatCommand;
import org.mars.sim.console.chat.Conversation;
import org.mars.sim.console.chat.simcommand.CommandHelper;
import org.mars.sim.console.chat.simcommand.StructuredResponse;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.core.vehicle.VehicleType;

/**
 * Command to display vehicles
 * This is a singleton.
 */
public class VehicleCommand extends AbstractSettlementCommand {

	public static final ChatCommand VEHICLE = new VehicleCommand();

	private VehicleCommand() {
		super("v", "vehicles", "Vehicle list");
	}

	/** 
	 * Output the answer
	 */
	@Override
	protected boolean execute(Conversation context, String input, Settlement settlement) {
		StructuredResponse response = new StructuredResponse();

		Collection<Vehicle> parked = settlement.getParkedVehicles();
		
		response.appendHeading("Vehicles");
		
		// Sort the vehicle list according to the type
		Collection<Vehicle> list = settlement.getAllAssociatedVehicles();
		List<Vehicle> vlist = list.stream().sorted((p1, p2) -> p1.getVehicleTypeString().compareTo(p2.getVehicleTypeString()))
				.collect(Collectors.toList());

		response.appendTableHeading("Name", CommandHelper.PERSON_WIDTH, "Type", 15, 
									"Parked", "Home", "Garage", "Reserved", "Mission", 25);

		BuildingManager bm = settlement.getBuildingManager();
		var missionMgr = context.getSim().getMissionManager();
		for (Vehicle v : vlist) {
			VehicleType vt = v.getVehicleType();
			String vTypeStr;
			if (vt == VehicleType.LUV)
				vTypeStr = "LUV";
			else {
				vTypeStr = vt.getName();
			}
			
			// Print mission name
			String missionName = "";
			Mission mission = missionMgr.getMissionForVehicle(v);
			if (mission != null) {
				missionName = mission.getTypeID();
			}
			
			// Dropped Parked once fix problem
			boolean isParked = parked.contains(v);
			boolean isHome = settlement.equals(v.getSettlement());
			boolean isGaraged = bm.isInGarage(v);
			response.appendTableRow(v.getName(), vTypeStr, isParked,
						isHome, isGaraged, v.isReserved(), missionName);
		}
		
		context.println(response.getOutput());
		return true;
	}
}
