/*
 * Mars Simulation Project
 * DashboardCommand.java
 * @date 2022-07-15
 * @author Barry Evans
 */

package com.mars_sim.console.chat.simcommand.settlement;

import com.mars_sim.console.chat.Conversation;
import com.mars_sim.console.chat.simcommand.CommandHelper;
import com.mars_sim.console.chat.simcommand.StructuredResponse;
import com.mars_sim.core.environment.TerrainElevation;
import com.mars_sim.core.goods.GoodsManager;
import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.structure.Settlement;

/**
 * Command to display dashboard for this settlement
 * This is a singleton.
 */
public class DashboardCommand extends AbstractSettlementCommand {

	public static final DashboardCommand DASHBOARD = new DashboardCommand();
	
	private DashboardCommand() {
		super("d", "dashboard", "Dashboard of the settlement");
	}

	/** 
	 * Outputs the current immediate location of the Unit.
	 */
	@Override
	protected boolean execute(Conversation context, String input, Settlement settlement) {

		StructuredResponse response = new StructuredResponse();
		generatedDashboard(settlement, response);
		
		context.println(response.getOutput());
		
		return true;
	}

	/**
	 * Generates a dashboard for a Settlement.
	 * 
	 * @param settlement
	 * @return
	 */
	void generatedDashboard(Settlement settlement, StructuredResponse response) {
		Coordinates location = settlement.getCoordinates();
		double elevationMEDGR = TerrainElevation.getMEGDRElevation(location);
		double elevationColor = TerrainElevation.getColorElevation(location);
		
		response.appendLabeledString("Sponsor", settlement.getReportingAuthority().getDescription());
		response.appendLabeledString("Objective", settlement.getObjective().getName());
		response.appendLabeledString("Location", location.getFormattedString());
		response.appendLabeledString("MEDGR Elevation", String.format(CommandHelper.KM_FORMAT, elevationMEDGR));
		response.appendLabeledString("Color Elevation", String.format(CommandHelper.KM_FORMAT, elevationColor));
		response.appendLabelledDigit("Population", settlement.getNumCitizens());	

		String[] cats = new String[] { "Repair", "Maintenance", "EVA Suit Production" };

		GoodsManager goodsManager = settlement.getGoodsManager();

		int[] levels = new int[] { goodsManager.getRepairLevel(), goodsManager.getMaintenanceLevel(),
				goodsManager.getEVASuitLevel() };
		
		response.appendBlankLine();
		response.appendTableHeading("Area", 22, "Level");

		for (int i=0; i<3; i++) {
			response.appendTableRow(cats[i], levels[i]);
		}
	}
}
