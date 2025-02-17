/*
 * Mars Simulation Project
 * ResourceCommand.java
 * @date 2022-07-15
 * @author Barry Evans
 */

package com.mars_sim.console.chat.simcommand.settlement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.mars_sim.console.chat.ChatCommand;
import com.mars_sim.console.chat.Conversation;
import com.mars_sim.console.chat.simcommand.CommandHelper;
import com.mars_sim.console.chat.simcommand.StructuredResponse;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.resourceprocess.ResourceProcess;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.WaterUseType;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.ResourceProcessing;
import com.mars_sim.core.structure.building.function.farming.Farming;

public class ResourceCommand extends AbstractSettlementCommand {
	private static final String PROJECTED_DAILY_CONSUMED = "Projected daily consumed";

	private static final String TOTAL_GROWING_AREA = "Total growing area";

	private static final String PROCESSES = "Processes";
	
	private static final String WASTES = "Wastes";

	private static final String CONSUMED_DAILY_PER_M2 = "Consumed daily per m2";

	private static final String TOTAL_AMOUNT_CONSUMED_DAILY = "Total consumed daily";

	private static final String CURRENT_RESERVE = "Current reserve";

	private static final String O2_FARMING	= "         Oxygen Generation from Farming";
	private static final String H2O_FARMING	= "         Water Consumption from Farming";
	private static final String CO2_FARMING	= "     Carbon Dioxide Consumption from Farming";

	
	private static final String KG_M2_SOL_FORMAT = "%8.2f kg/m^2/sol";

	private static final String KG_SOL_FORMAT = "%8.2f kg/sol";

	private static final String M2_FORMAT = "%8.2f m^2";
	
	public static final ChatCommand RESOURCE = new ResourceCommand();
	
	private static final String OXYGEN = "o2";
	private static final String CO2 = "co2";
	private static final String WATER = "water";
	private static final String GREY_WATER = "grey water";


	private ResourceCommand() {
		super("rs", "resource", "Settlement resources: either oxygen, co2, water, or grey water");
		
		// Setup the fixed arguments
		setArguments(Arrays.asList(OXYGEN, CO2, WATER, GREY_WATER));
	}
	
	@Override
	protected boolean execute(Conversation context, String input, Settlement settlement) {
		boolean result = false;
		if (input == null || input.isEmpty()) {
			context.println("Must enter a resource type " + getArguments(context));
		}
		else {
			StructuredResponse response = new StructuredResponse();
			String subCommand = input.trim().toLowerCase();

			switch (subCommand) {
			case OXYGEN:
				displayOxygen(settlement, response);
				result = true;
				break;
			
			case WATER:
				displayWater(settlement, response, ResourceUtil.waterID);
				result = true;
				break;
				
			case GREY_WATER:
				displayWater(settlement, response, ResourceUtil.greyWaterID);
				result = true;
				break;
				
			case CO2:
				displayCO2(settlement, response);
				result = true;
				break;

			default:
				response.append("Sorry don't know about resource " + subCommand);
				break;
			}
			
			context.println(response.getOutput());
		}
		return result;
	}

	private void displayCO2(Settlement settlement, StructuredResponse response) {
		double usage = 0;
		double totalArea = 0;
		double reserve = settlement.getAmountResourceStored(ResourceUtil.co2ID);

		response.appendHeading(CO2_FARMING);
		response.appendLabeledString(CURRENT_RESERVE, String.format(CommandHelper.KG_FORMAT, reserve));

		// Prints greenhouse usage
		Set<Building> farms = settlement.getBuildingManager().getBuildingSet(FunctionType.FARMING);
		for (Building b : farms) {
			Farming f = b.getFarming();
			usage += f.computeUsage(ResourceUtil.co2ID);
			totalArea += f.getGrowingArea();
		}
		totalArea = (totalArea != 0 ? totalArea: 0.1D); // Guard against divide by zero

		response.appendLabeledString(TOTAL_GROWING_AREA, String.format(M2_FORMAT, totalArea));
		response.appendLabeledString("Generated daily per m2", String.format(KG_M2_SOL_FORMAT,
																			(usage / totalArea)));
		response.appendLabeledString("Total generated daily", String.format(KG_SOL_FORMAT, usage));		
	}

	private void displayWater(Settlement settlement, StructuredResponse response, int id) {
		double reserve = settlement.getAmountResourceStored(id);
		response.appendLabeledString(CURRENT_RESERVE, String.format(CommandHelper.KG_FORMAT, reserve));
		response.appendBlankLine();
		// For consumption, use the '+ve' sign	
		double usage = 0;
		double totalArea = 0;
		// For production, use the '-ve' sign
		double sign = -1.0;
		
		// Prints greenhouse usage
		Set<Building> farms = settlement.getBuildingManager().getBuildingSet(FunctionType.FARMING);
		for (Building b : farms) {
			Farming f = b.getFarming();
			usage += f.computeUsage(id);
			totalArea += f.getGrowingArea();
		}

		response.appendHeading(H2O_FARMING); 
		response.appendLabeledString(TOTAL_GROWING_AREA, String.format(M2_FORMAT, totalArea));
		if (totalArea > 0) {
			response.appendLabeledString(CONSUMED_DAILY_PER_M2,	String.format(KG_M2_SOL_FORMAT, (usage / totalArea)));
		}
		response.appendLabeledString(PROJECTED_DAILY_CONSUMED, String.format(KG_SOL_FORMAT, usage));

		response.appendBlankLine();

		response.appendTableHeading("Category", 16, "[kg/sol] '+ve':Consumed '-ve':Produced ");
		
		
		double net = 0;
		double greenhouseUsage = farms.stream()
							.mapToDouble(b -> b.getFarming().getDailyAverageWaterUsage())
							.sum();
		response.appendTableRow("Greenhouse", Math.round(greenhouseUsage * 100.0) / 100.0);
		net = net + greenhouseUsage;

		// Prints consumption
		List<Person> ppl = new ArrayList<>(settlement.getAllAssociatedPeople());
		double consumption = ppl.stream()
								.mapToDouble(p -> p.getPhysicalCondition().getDailyFoodUsage(3))
								.sum();
		response.appendTableRow("People", Math.round(consumption * 100.0) / 100.0);
		net = net + consumption;

		// Add water usage from making meal and dessert
		double cooking = settlement.getDailyWaterUsage(WaterUseType.PREP_MEAL)
					+ settlement.getDailyWaterUsage(WaterUseType.PREP_DESSERT);
		response.appendTableRow("Cooking", Math.round(cooking * 100.0) / 100.0);
		net = net + cooking;

		// Prints living usage
		List<Building> quarters = settlement.getBuildingManager()
				.getBuildings(FunctionType.LIVING_ACCOMMODATION);
		double livingUsage = quarters.stream()
					.mapToDouble(b -> b.getLivingAccommodation().getDailyAverageWaterUsage())
					.sum();		
		response.appendTableRow("Accommodation", Math.round(livingUsage * 100.0) / 100.0);
		net = net + livingUsage;

		// Prints cleaning usage
		double cleaning = settlement.getDailyWaterUsage(WaterUseType.CLEAN_MEAL)
					+ settlement.getDailyWaterUsage(WaterUseType.CLEAN_DESSERT);
		response.appendTableRow("Cleaning", Math.round(cleaning * 100.0) / 100.0);
		net = net + cleaning;

		// Prints output from resource processing
		double output = 0;
		List<Building> bldgs = settlement.getBuildingManager().getBuildings(FunctionType.RESOURCE_PROCESSING);
		for (Building b : bldgs) {
			ResourceProcessing rp = b.getResourceProcessing();
			List<ResourceProcess> processes = rp.getProcesses();
			output += processes.stream()
							.filter(ResourceProcess::isProcessRunning)
							.mapToDouble(p -> p.getBaseFullOutputRate(id))
							.sum();
		}
		// convert from 'per millisol' to 'per sol'
		output = output * 1_000;
		
		response.appendTableRow(PROCESSES, Math.round(- sign * output * 100.0) / 100.0);
		net = net + - sign * output;
		
		// Prints output from waste processing
		double output2 = 0;
		for (Building b : settlement.getBuildingManager().getBuildings(FunctionType.WASTE_PROCESSING)) {
				output2 += b.getWasteProcessing().getProcesses().stream()
									.filter(ResourceProcess::isProcessRunning)
									.mapToDouble(p -> p.getBaseFullOutputRate(id))
									.sum();
		}
		// convert from 'per millisol' to 'per sol'
		output2 = output2 * 1_000;
		
		response.appendTableRow(WASTES, Math.round(- sign * output2 * 100.0) / 100.0);
		net = net + - sign * output2;

		response.appendTableRow("NET", Math.round(net * 100.0) / 100.0);		
	}

	private void displayOxygen(Settlement settlement, StructuredResponse response) {
		double usage = 0;
		double totalArea = 0;
		double reserve = settlement.getAmountResourceStored(ResourceUtil.oxygenID);
		
		response.appendHeading(O2_FARMING);
		response.appendLabeledString(CURRENT_RESERVE, String.format(CommandHelper.KG_FORMAT, reserve));

		// Prints greenhouse usage
		Set<Building> farms = settlement.getBuildingManager().getBuildingSet(FunctionType.FARMING);
		for (Building b : farms) {
			Farming f = b.getFarming();
			usage += f.computeUsage(ResourceUtil.oxygenID);
			totalArea += f.getGrowingArea();
		}

		response.appendLabeledString(TOTAL_GROWING_AREA, String.format(M2_FORMAT, totalArea));
		if (totalArea > 0) {
			response.appendLabeledString(CONSUMED_DAILY_PER_M2,
										String.format(KG_M2_SOL_FORMAT, (usage / totalArea)));
		}
		response.appendLabeledString(TOTAL_AMOUNT_CONSUMED_DAILY, String.format(KG_SOL_FORMAT, usage));
	}
}
