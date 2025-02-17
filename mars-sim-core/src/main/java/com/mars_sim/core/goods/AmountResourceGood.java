/*
 * Mars Simulation Project
 * AmountResourceGood.java
 * @date 2024-06-29
 * @author Barry Evans
 */
package com.mars_sim.core.goods;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.mars_sim.core.SimulationConfig;
import com.mars_sim.core.food.FoodProductionProcess;
import com.mars_sim.core.food.FoodProductionProcessInfo;
import com.mars_sim.core.food.FoodProductionUtil;
import com.mars_sim.core.goods.GoodsManager.CommerceType;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.manufacture.ManufactureProcessInfo;
import com.mars_sim.core.manufacture.ManufactureUtil;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.process.ProcessItem;
import com.mars_sim.core.resource.AmountResource;
import com.mars_sim.core.resource.ItemType;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.resourceprocess.ResourceProcess;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.LivingAccommodation;
import com.mars_sim.core.structure.building.function.ResourceProcessing;
import com.mars_sim.core.structure.building.function.cooking.Cooking;
import com.mars_sim.core.structure.building.function.cooking.HotMeal;
import com.mars_sim.core.structure.building.function.cooking.Ingredient;
import com.mars_sim.core.structure.building.function.cooking.PreparingDessert;
import com.mars_sim.core.structure.building.function.farming.Crop;
import com.mars_sim.core.structure.building.function.farming.Farming;
import com.mars_sim.core.structure.construction.ConstructionSite;
import com.mars_sim.core.structure.construction.ConstructionStage;
import com.mars_sim.core.structure.construction.ConstructionStageInfo;
import com.mars_sim.core.structure.construction.ConstructionUtil;
import com.mars_sim.core.structure.construction.ConstructionValues;
import com.mars_sim.core.time.MarsTime;
import com.mars_sim.core.vehicle.Vehicle;

/**
 * This represents how a Amount Resource can be traded.
 */
class AmountResourceGood extends Good {
	
	private static final long serialVersionUID = 1L;
	
	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(AmountResourceGood.class.getName());

	private static final String NACO3 = "Sodium Carbonate";
	private static final String IRON_POWDER = "Iron Powder";
	
	private static final double INITIAL_AMOUNT_DEMAND = 0;
	private static final double INITIAL_AMOUNT_SUPPLY = 0;

	private static final double WASTE_WATER_VALUE_MODIFIER = 1.5;
	private static final double GREY_WATER_VALUE_MODIFIER = 1;
	private static final double BLACK_WATER_VALUE_MODIFIER = .5;
	private static final double USEFUL_WASTE_VALUE_MODIFIER = 1.05D;

	// Cost modifiers
	private static final double CH4_COST = 0.3;
	private static final double METHANOL_COST = 0.4;
	private static final double H2_COST = 1;
	private static final double CO_COST = 0.05;
    private static final double CO2_COST = 0.0000005;
	private static final double CL_COST = 0.25;
	private static final double ICE_COST = 0.5;
	private static final double FOOD_COST = 0.1;
	private static final double DERIVED_COST = .07;
	private static final double SOY_COST = 0.05;
	private static final double CROP_COST = 5;
	private static final double ANIMAL_COST = 10;
	private static final double ORGANISM_COST = 10;
	private static final double CHEMICAL_COST = 0.01;
	private static final double MEDICAL_COST = 0.01;
	private static final double WASTE_COST = 0.0001;
	private static final double OIL_COST = 0.01;
	private static final double ROCK_COST = 5;
	private static final double REGOLITH_COST = 0.02;
	private static final double ORE_COST = 0.3;
	private static final double MINERAL_COST = 0.3;
	private static final double ELEMENT_COST = 0.5;
	private static final double LIFE_SUPPORT_COST = 0.5;


	// modifiers
    private static final double ICE_VALUE_MODIFIER = 0.1;
	private static final double WATER_VALUE_MODIFIER = 0.2;
	private static final double BRINE_WATER_VALUE_MODIFIER  = 0.04;
	
	private static final double SOIL_VALUE_MODIFIER = 0.05;
	private static final double SAND_VALUE_MODIFIER = 0.03;
	private static final double ORES_VALUE_MODIFIER = 0.05;
	
	private static final double CONCRETE_VALUE_MODIFIER = 0.7;
	private static final double CEMENT_VALUE_MODIFIER = 2;
	private static final double MINERAL_VALUE_MODIFIER = 0.02;
	private static final double ROCK_VALUE_MODIFIER = 0.02;
	private static final double METEORITE_VALUE_MODIFIER = 100;
	
	private static final double ROCK_SALT_VALUE_MODIFIER = 1;
	private static final double EPSOM_SALT_VALUE_MODIFIER = 0.1;
	
	private static final double FOOD_VALUE_MODIFIER = 0.1;
	
	private static final double OXYGEN_VALUE_MODIFIER = 8;
	private static final double METHANE_VALUE_MODIFIER = 0.07;
	private static final double HYDROGEN_VALUE_MODIFIER = 0.0001;
	private static final double METHANOL_VALUE_MODIFIER = 0.05;
	
	private static final double CO2_VALUE_MODIFIER = 0.0075;

	private static final double LIFE_SUPPORT_FACTOR = .005;
	private static final double VEHICLE_FUEL_FACTOR = 1;
	private static final double FARMING_FACTOR = .1;

	private static final double LEAVES_VALUE_MODIFIER = .5;

	private static final double DESSERT_FACTOR = .1;

	private static final double CROP_FACTOR = .01;
	
	private static final double TISSUE_CULTURE_VALUE = 0.5;
	
	private static final double ORGANISM_FACTOR = 0.05;
	private static final double SOY_BASED_FACTOR = 0.075;
	private static final double DERIVED_FACTOR = 0.15;
	private static final double INSECT_FACTOR = 0.075;
	private static final double OIL_FACTOR = 0.025;
	
	private static final double REGOLITH_TYPE_VALUE_MODIFIER = 2;
	private static final double REGOLITH_VALUE_MODIFIER = 2.0;
	private static final double REGOLITH_VALUE_MODIFIER_1 = 10;
	private static final double REGOLITH_VALUE_MODIFIER_2 = 10;
		
	// flatten multipliers
	private static final double ORE_FLATTENING_FACTOR = 1.1;
	private static final double MINERAL_FLATTENING_FACTOR = 1.1;
	private static final double ROCK_FLATTENING_FACTOR = 1;
	private static final double REGOLITH_FLATTENING_FACTOR = 2;
	private static final double SAND_FLATTENING_FACTOR = 1;
	
	private static final double OLIVINE_FLATTENING_FACTOR = 0.5;
	private static final double KAMACITE_FLATTENING_FACTOR = 0.2;
	
	private static final double CHEMICAL_FLATTENING_FACTOR = 3;
	private static final double COMPOUND_FLATTENING_FACTOR = 2;
	private static final double ELEMENT_FLATTENING_FACTOR = 4;

	private static final double GEMSTONE_FLATTENING_FACTOR = 3;

	private static final double WASTE_FLATTENING_FACTOR = 0.15;
	
	private static final double UTILITY_FLATTENING_FACTOR = 10;
	private static final double INSTRUMENT_FLATTENING_FACTOR = 5;

	private static final double INSECT_FLATTENING_FACTOR = 5;
	private static final double ORGANISM_FLATTENING_FACTOR = 2;
	private static final double SOYBASED_FLATTENING_FACTOR = 0.5;
	private static final double ANIMAL_FLATTENING_FACTOR = 2;
	private static final double CROP_FLATTENING_FACTOR = 2;
	private static final double DERIVED_FLATTENING_FACTOR = 2;
	private static final double TISSUE_FLATTENING_FACTOR = 4;
	
	private static final double METHANOL_FLATTENING_FACTOR = 0.9;
	private static final double METHANE_FLATTENING_FACTOR = 1.1;
	private static final double HYDROGEN_FLATTENING_FACTOR = .025;
	private static final double OXYGEN_FLATTENING_FACTOR = .5;	
	
	private static final double ACETYLENE_FLATTENING_FACTOR = 0.025;
	private static final double CO_FLATTENING_FACTOR = 0.09;
	private static final double CO2_FLATTENING_FACTOR = 0.06;
	

	private static final double ICE_FLATTENING_FACTOR = 0.05;

	private static final double NACO3_FLATTENING_FACTOR = 0.5;
	private static final double IRON_POWDER_FLATTENING_FACTOR = 0.005;
	
	private static final double WATER_FLATTENING_FACTOR = 0.5;
	
	private static final double COOKED_MEAL_INPUT_FACTOR = 0.05;
	
	private static final double MANUFACTURING_INPUT_FACTOR = 2D;
	private static final double FOOD_PRODUCTION_INPUT_FACTOR = 0.1;
	private static final double CONSTRUCTION_SITE_REQUIRED_RESOURCE_FACTOR = 1000D;
	private static final double CONSTRUCTING_INPUT_FACTOR = 2D;

	private static final double MAX_RESOURCE_PROCESSING_DEMAND = 3000; 
	private static final double MAX_MANUFACTURING_DEMAND = 3000;
	private static final double MAX_FOOD_PRODUCTION_DEMAND = 3000;

	private static final int METEORITE_ID = ResourceUtil.findIDbyAmountResourceName("meteorite");
	/** The fixed flatten demand for this resource. */
	private double flattenDemand;
	/** The projected demand of each refresh cycle. */
	private double projectedDemand;
	/** The trade demand of each refresh cycle. */
	private double tradeDemand;
	
	private double costModifier = -1;
	
	private GoodType goodType;
	
	private AmountResource resource;

    AmountResourceGood(AmountResource ar) {
        super(ar.getName(), ar.getID());

		// Calculate fixed values
		flattenDemand = calculateFlattenDemand(ar);
		costModifier = calculateCostModifier(ar);
    }

	/**
	 * Flattens the flatten demand based on selected resource.
	 * 
	 * @param resource
	 * @return
	 */
	private double calculateFlattenDemand(AmountResource ar) {

		// WARNING: do NOT miss adding the break keyword at the end of each case
		// or else the flatten amount would be invalid
		
		double mod = 1;
		switch(ar.getGoodType()) {
		
		case ANIMAL:
			mod = ANIMAL_FLATTENING_FACTOR;
			
			break;	

		case CHEMICAL:
			mod = CHEMICAL_FLATTENING_FACTOR;	
			
			break;
			
		case COMPOUND:
			mod = COMPOUND_FLATTENING_FACTOR;
	
			if (ar.getID() == ResourceUtil.acetyleneID)
				mod *= ACETYLENE_FLATTENING_FACTOR;
			else if (ar.getID() == ResourceUtil.sandID)
				mod *= SAND_FLATTENING_FACTOR;
			else if (ar.getID() == ResourceUtil.iceID)
				mod *= ICE_FLATTENING_FACTOR;
			else if (ar.getID() == ResourceUtil.coID)
				mod *= CO_FLATTENING_FACTOR;
			else if (ar.getID() == ResourceUtil.co2ID)
				mod *= CO2_FLATTENING_FACTOR;
			else if (ar.getID() == ResourceUtil.methaneID)
				mod *= METHANE_FLATTENING_FACTOR;
			else if (ar.getID() == ResourceUtil.methanolID)
				mod *= METHANOL_FLATTENING_FACTOR;
			else if (ar.getID() == ResourceUtil.waterID)
				mod *= WATER_FLATTENING_FACTOR;
			
			String name = ar.getName();
			
			if (name.equalsIgnoreCase(NACO3)) 
				mod *= NACO3_FLATTENING_FACTOR;
			
			break;
	
		case CROP:
			mod = CROP_FLATTENING_FACTOR;
			
			break;
					
		case DERIVED:
			mod = DERIVED_FLATTENING_FACTOR;
			
			break;
					
		case ELEMENT:
			mod = ELEMENT_FLATTENING_FACTOR;
			
			if (ar.getID() == ResourceUtil.hydrogenID)
				mod *= HYDROGEN_FLATTENING_FACTOR;
			
			else if (ar.getID() == ResourceUtil.oxygenID)
				mod *= OXYGEN_FLATTENING_FACTOR;
			
			name = ar.getName();
		
			if (name.equalsIgnoreCase(IRON_POWDER))
				mod *= IRON_POWDER_FLATTENING_FACTOR;
			
			break;
			
		case GEMSTONE:
			mod = GEMSTONE_FLATTENING_FACTOR;
			
			break;
					
		case INSECT:
			mod = INSECT_FLATTENING_FACTOR;
			
			break;
			
		case INSTRUMENT:
			mod = INSTRUMENT_FLATTENING_FACTOR;

			break;
			
		case MINERAL:
			mod = MINERAL_FLATTENING_FACTOR;
			
			if (ar.getID() == ResourceUtil.olivineID)
				mod *= OLIVINE_FLATTENING_FACTOR;
			else if (ar.getID() == ResourceUtil.kamaciteID)
				mod *= KAMACITE_FLATTENING_FACTOR;
			
			break;
			
		case ORE:
			mod = ORE_FLATTENING_FACTOR;
			
			break;

		case ORGANISM:
			mod = ORGANISM_FLATTENING_FACTOR;
			
			break;

		case REGOLITH:
			mod = REGOLITH_FLATTENING_FACTOR;
			
			break;
				
		case ROCK:
			mod = ROCK_FLATTENING_FACTOR;
			
			break;
			
		case SOY_BASED:
			mod = SOYBASED_FLATTENING_FACTOR;
			
			break;
			
		case TISSUE:
			mod = TISSUE_FLATTENING_FACTOR;
			
			break;
	
		case UTILITY:
			mod = UTILITY_FLATTENING_FACTOR;
			
			break;
	
		case WASTE:
			mod = WASTE_FLATTENING_FACTOR;
			
			break;

		default:
			break;
		}

		return mod;
	}

    /**
     * Gets the flattened demand of this amount resource.
     * 
     * @return
     */
	@Override
    public double getFlattenDemand() {
    	return flattenDemand;
    }
	
    /**
     * Gets the projected demand of this resource.
     * 
     * @return
     */
	@Override
    public double getProjectedDemand() {
    	return projectedDemand;
    }
	
    /**
     * Gets the trade demand of this resource.
     * 
     * @return
     */
	@Override
    public double getTradeDemand() {
    	return tradeDemand;
    }
    
    @Override
    public GoodCategory getCategory() {
        return GoodCategory.AMOUNT_RESOURCE;
    }

    @Override
    public double getMassPerItem() {
        return 1D;
    }

    @Override
    public GoodType getGoodType() {
    	if (goodType == null) {
    		goodType = getAmountResource().getGoodType();
    	}
        return goodType;
    }

    private AmountResource getAmountResource() {
    	if (resource == null) {
    		resource = ResourceUtil.findAmountResource(getID());
    	}
        return resource;
    }

    @Override
    protected double computeCostModifier() {
		return costModifier;
	}

	/**
	 * Calculates the cost modifier from a resource.
	 * 
     * @param ar
     * @return
     */
	private static double calculateCostModifier(AmountResource ar) {
        boolean edible = ar.isEdible();
        boolean lifeSupport = ar.isLifeSupport();
        GoodType type = ar.getGoodType();
        double result = 0D;

        if (lifeSupport)
            result += LIFE_SUPPORT_COST;
        
        else if (edible) {
            if (type == GoodType.DERIVED)
                result += DERIVED_COST;
            else if (type == GoodType.SOY_BASED)
                result += SOY_COST;
            else if (type == GoodType.ANIMAL)
                result += ANIMAL_COST;        
            else if (type == GoodType.ORGANISM)
                result += ORGANISM_COST;
            else
                result += FOOD_COST;
        }
        
        else if (type == GoodType.WASTE)
            result += WASTE_COST;

		// TODO Should be a Map GoodType -> double VALUE
        else if (type == GoodType.MEDICAL)
            result += MEDICAL_COST;
        else if (type == GoodType.OIL)
            result += OIL_COST;
        else if (type == GoodType.CROP)
            result += CROP_COST;
        else if (type == GoodType.ROCK)
            result += ROCK_COST;
        else if (type == GoodType.REGOLITH)
            result += REGOLITH_COST;
        else if (type == GoodType.ORE)
            result += ORE_COST;
        else if (type == GoodType.MINERAL)
            result += MINERAL_COST;
        else if (type == GoodType.ELEMENT)
            result += ELEMENT_COST;
        else if (type == GoodType.CHEMICAL)
            result += CHEMICAL_COST;
        else if (ar.getID() == ResourceUtil.methaneID)
            result += CH4_COST;
        else if (ar.getID() == ResourceUtil.methanolID)
            result += METHANOL_COST;
        else if (ar.getID() == ResourceUtil.hydrogenID)
            result += H2_COST;
        else if (ar.getID() == ResourceUtil.chlorineID)
            result += CL_COST;
        else if (ar.getID() == ResourceUtil.co2ID)
            result += CO2_COST;
        else if (ar.getID() == ResourceUtil.coID)
            result += CO_COST;
        else if (ar.getID() == ResourceUtil.iceID)
            result += ICE_COST;

        return result;
    }

    /**
	 * Gets the amount of the good being produced at the settlement by ongoing food
	 * production.
	 *
	 * @param settlement the good.
	 * @return amount (kg for amount resources, number for parts, equipment, and
	 *         vehicles).
	 */
	private double getFoodProductionOutput(Settlement settlement) {
		double result = 0D;

		// Get the amount of the resource that will be produced by ongoing food
		// production processes.
		for (Building b : settlement.getBuildingManager().getBuildingSet(FunctionType.FOOD_PRODUCTION)) {
			// Go through each ongoing food production process.
			for(FoodProductionProcess process : b.getFoodProduction().getProcesses()) {
				for(ProcessItem item : process.getInfo().getOutputList()) {
					if (item.getName().equalsIgnoreCase(getName())) {
						result += item.getAmount();
					}
				}
			}
		}

		return result;
	}


    @Override
    public double getNumberForSettlement(Settlement settlement) {
        double amount = 0D;

		// Get amount of resource in settlement storage.
		amount += settlement.getAmountResourceStored(getID());
        
        // Get amount of resource out on mission vehicles.
        amount += getVehiclesOnMissions(settlement)
                        .map(v -> v.getAmountResourceStored(getID()))
                        .collect(Collectors.summingDouble(f -> f));

		// Get amount of resource carried by people on EVA.
		amount += getPersonOnEVA(settlement)
                    .map(p -> p.getAmountResourceStored(getID()))
                    .collect(Collectors.summingDouble(f -> f));

		// Get the amount of the resource that will be produced by ongoing manufacturing
		// processes.
		amount += getManufacturingProcessOutput(settlement);

		// Get the amount of the resource that will be produced by ongoing food
		// production processes.
		amount += getFoodProductionOutput(settlement);

		return amount;
    }

    @Override
    double calculatePrice(Settlement settlement, double value) {
		double totalMass = Math.round(settlement.getAmountResourceStored(getID()) * 100.0)/100.0;
		double factor = 1.5 / (.5 + Math.log(totalMass + 1));
	    double price = getCostOutput() * (1 + 2 * factor * Math.log(value + 1));
	    setPrice(price);
	    return price;
    }

    @Override
    double getDefaultDemandValue() {
        return INITIAL_AMOUNT_DEMAND;
    }

    @Override
    double getDefaultSupplyValue() {
        return INITIAL_AMOUNT_SUPPLY;
    }

    @Override
    void refreshSupplyDemandValue(GoodsManager owner) {
        int id = getID();
        
		double previousDemand = owner.getDemandValue(this);

        Settlement settlement = owner.getSettlement();

		double totalDemand = 0;
		double totalSupply = 0;	

		// Calculate projected demand
		double projectedDemand = 
			// Tune ice demand.
			computeIceProjectedDemand(owner, settlement)
			// Tune regolith projected demand.
			+ computeRegolithProjectedDemand(owner, settlement)
			// Tune life support demand if applicable.
			+ getLifeSupportDemand(owner, settlement)
			// Tune potable water usage demand if applicable.
			+ getPotableWaterUsageDemand(owner, settlement)
			// Tune toiletry usage demand if applicable.
			+ getToiletryUsageDemand(settlement)
			// Tune vehicle demand if applicable.
			+ getVehicleFuelDemand(owner, settlement)
			// Tune farming demand.
			+ getFarmingDemand(owner, settlement)
			// Tune the tissue demand due to its crop
			+ computeTissueDemandDueToCrop(owner)
			// Tune resource processing demand.
			+ getResourceProcessingDemand(owner, settlement)
			// Tune manufacturing demand.
			+ getResourceManufacturingDemand(owner, settlement)
			// Tune food production related demand.
			+ getResourceFoodProductionDemand(owner, settlement)
			// Tune demand for the ingredients in all meals.
			+ getAvailableMealDemand(settlement)
			// Tune the favorite meal demand
			+ getFavoriteMealDemand(owner, settlement)
			// Tune dessert demand.
			+ getResourceDessertDemand(settlement)
			// Tune construction demand.
			+ getResourceConstructionDemand(settlement)
			// Tune construction site demand.
			+ getResourceConstructionSiteDemand(settlement)
			// Adjust the demand on minerals and ores.
			+ getMineralDemand(owner, settlement);
		
//		if (getID() == ResourceUtil.hydrogenID) {
//			System.out.println("old projected: " + Math.round(projected * 100.0)/100.0
//					+ "  flattenDemand: " + Math.round(flattenDemand * 100.0)/100.0
//					+ "  previousDemand: " + Math.round(previousDemand * 100.0)/100.0);
//		}
		
		projectedDemand = Math.min(HIGHEST_PROJECTED_VALUE, projectedDemand);
	
		this.projectedDemand = projectedDemand;
		
		double projected = projectedDemand
			// Flatten certain types of demand.
			* flattenDemand
			// Adjust the demand on various waste products with the disposal cost.
			* modifyWasteResource();
		
		// Add trade value. Cache is always false if this method is called
		this.tradeDemand = owner.determineTradeDemand(this);
		
		if (previousDemand == 0) {
			// At the start of the sim
			totalDemand = (
					.5 * projected 
					+ .5 * tradeDemand);

		}
		else {
			// Intentionally loses a tiny percentage (e.g. 0.0008) of its value
			// in order to counter the tendency for all goods to increase 
			// in value over time. 
			
			// Warning: a lot of Goods could easily will hit 10,000 demand
			// if not careful.
			
			// Allows only very small fluctuations of demand as possible

			totalDemand = (
					  .9986 * previousDemand 
					+ .00012 * projected 
					+ .00005 * tradeDemand); 
		}

//		if (getID() == ResourceUtil.hydrogenID) {
//			System.out.println("totalDemand: " + Math.round(totalDemand * 100.0)/100.0
//					+ "  previousDemand: " + Math.round(previousDemand * 100.0)/100.0
//					+ "  projected: " + Math.round(projected * 100.0)/100.0
//					+ "  trade: " + Math.round(trade * 100.0)/100.0);
//		}
		
		// Save the goods demand
		owner.setDemandValue(this, totalDemand);
		
		// Calculate total supply
		totalSupply = getAverageAmountSupply(settlement.getAmountResourceStored(id));

		// Store the average supply
		owner.setSupplyValue(this, totalSupply);
    }

    
    /**
	 * Gets the total supply for the amount resource.
	 *
	 * @param resource`
	 * @param supplyStored
	 * @param solElapsed
	 * @return
	 */
	private static double getAverageAmountSupply(double supplyStored) {
		return Math.sqrt(1 + supplyStored);
	}


	/**
	 * Gets the demand for a resource from all automated resource processes at a
	 * settlement.
	 *
	 * @param resource the amount resource.
	 * @return demand (kg)
	 */
	private double getResourceProcessingDemand(GoodsManager owner, Settlement settlement) {
		double demand = 0D;

		// Get all resource processes at settlement.
		for(ResourceProcess i : getResourceProcesses(settlement)) {
			double processDemand = getResourceProcessDemand(owner, settlement, i);
			demand += processDemand;
		}

		return Math.min(MAX_RESOURCE_PROCESSING_DEMAND, demand / 3);
	}

	/**
	 * Gets the demand for a resource from an automated resource process.
	 *
	 * @param process  the resource process.
	 * @param resource the amount resource.
	 * @return demand (kg)
	 */
	private double getResourceProcessDemand(GoodsManager owner, Settlement settlement, ResourceProcess process) {

		int resource = getID();

		Set<Integer> inputResources = process.getInputResources();
		Set<Integer> outputResources = process.getOutputResources();

		/* For instance,		
		<process name="Selective Partial Oxidation of Methane to Methanol" power-required="0.05" >
		<input resource="methane" rate="3.2"  />
		<input resource="oxygen" rate="3.2"  />
		
		<output resource="methanol" rate="5.888"  />
		<output resource="methane" rate="0.512"  />
		*/
		
		if (inputResources.contains(resource) && !process.isAmbientInputResource(resource)) {
			double outputValue = 0D;
			for (Integer output : outputResources) {
				double singleOutputRate = process.getBaseSingleOutputRate(output);
				if (!process.isWasteOutputResource(resource)) {
					outputValue += (owner.getDemandValue(GoodsUtil.getGood(output)) 
							* singleOutputRate);
				}
			}

			double singleInputRate = process.getBaseSingleInputRate(resource);

			return outputValue / singleInputRate / 3; 
		}

		return 0;
	}

	/**
	 * Gets all resource processes at settlement.
	 *
	 * @return list of resource processes.
	 */
	private static List<ResourceProcess> getResourceProcesses(Settlement settlement) {
		List<ResourceProcess> processes = new ArrayList<>();
		for(Building building : settlement.getBuildingManager().getBuildingSet()) {
			if (building.hasFunction(FunctionType.RESOURCE_PROCESSING)) {
				ResourceProcessing processing = building.getResourceProcessing();
				processes.addAll(processing.getProcesses());
			}
		}
		return processes;
	}

	/**
	 * Gets the demand for an amount resource as an input in the settlement's
	 * manufacturing processes.
	 *
	 * @return demand (kg)
	 */
	private double getResourceManufacturingDemand(GoodsManager owner, Settlement settlement) {
		double demand = 0D;

		// Get highest manufacturing tech level in settlement.
		int techLevel = ManufactureUtil.getHighestManufacturingTechLevel(settlement);
		if (techLevel >= 0) {
			for (ManufactureProcessInfo i : ManufactureUtil.getManufactureProcessesForTechLevel(techLevel)) {
				double manufacturingDemand = getResourceManufacturingProcessDemand(owner, settlement, i);
				demand += manufacturingDemand / 1000D;
			}
		}

		return Math.min(MAX_MANUFACTURING_DEMAND, demand / 100);
	}

	/**
	 * Gets the demand for an amount resource as an input in the settlement's Food
	 * Production processes.
	 *
	 * @return demand (kg)
	 */
	private double getResourceFoodProductionDemand(GoodsManager owner, Settlement settlement) {
		double demand = 0D;

		// Get highest Food Production tech level in settlement.
		if (FoodProductionUtil.doesSettlementHaveFoodProduction(settlement)) {
			int techLevel = FoodProductionUtil.getHighestFoodProductionTechLevel(settlement);
			for(FoodProductionProcessInfo i : FoodProductionUtil.getProcessesForTechSkillLevel(techLevel)) {
				double foodProductionDemand = getResourceFoodProductionProcessDemand(owner, settlement, i);
				demand += foodProductionDemand;
			}
		}

		return Math.min(MAX_FOOD_PRODUCTION_DEMAND, demand);
	}

	/**
	 * Gets the demand for an input amount resource in a manufacturing process.
	 *
	 * @param process  the manufacturing process.
	 * @return demand (kg)
	 */
	private double getResourceManufacturingProcessDemand(GoodsManager owner, Settlement settlement, ManufactureProcessInfo process) {
		double demand = 0D;
		String r = getAmountResource().getName().toLowerCase();

		ProcessItem resourceInput = null;
		for(var item : process.getInputList()) {
			if ((ItemType.AMOUNT_RESOURCE == item.getType()) && r.equals(item.getName())) {
				resourceInput = item;
				break;
			}
		}

		if (resourceInput != null) {
			double outputsValue = 0D;
			for(var j : process.getOutputList()) {
				outputsValue += ManufactureUtil.getManufactureProcessItemValue(j, settlement, true);
			}

			double totalItems = 0D;
			for(var k : process.getInputList()) {
				totalItems += k.getAmount();
			}

			// Determine value of required process power.
			double powerHrsRequiredPerMillisol = process.getPowerRequired() * MarsTime.HOURS_PER_MILLISOL;
			double powerValue = powerHrsRequiredPerMillisol * settlement.getPowerGrid().getPowerValue();

			// Obtain the value of this resource
			double totalInputsValue = (outputsValue - powerValue);

			int resourceID = getID();
			CommerceType cType = null;
			GoodType type = getGoodType();
			switch(settlement.getObjective()) {
				case BUILDERS_HAVEN: { 
					if (GoodType.REGOLITH == type
					|| GoodType.MINERAL == type
					|| GoodType.ORE == type
					|| GoodType.UTILITY == type) {
						cType = CommerceType.BUILDING;
					}
				} break;

				case CROP_FARM: {
					if (GoodType.CROP == type
					|| GoodType.DERIVED == type
					|| GoodType.SOY_BASED == type) {
						cType = CommerceType.CROP;
					}
				} break;

				case MANUFACTURING_DEPOT: 
					cType = CommerceType.MANUFACTURING;
				break;

				case RESEARCH_CAMPUS: { 
					if (GoodType.MEDICAL == type
					|| GoodType.ORGANISM == type
					|| GoodType.CHEMICAL == type
					|| GoodType.ROCK == type) {
						cType = CommerceType.RESEARCH;
					}
				} break;

				case TRADE_CENTER:
					cType = CommerceType.TRADE;
				break;

				case TRANSPORTATION_HUB: {
					if (resourceID == ResourceUtil.methanolID) {
//					|| resourceID == ResourceUtil.methaneID
//					|| resourceID == ResourceUtil.hydrogenID) {
						cType = CommerceType.TRANSPORT;
					}
				} break;

				default:
					break;
			}
			if (cType != null) {
				totalInputsValue *= owner.getCommerceFactor(cType);
			}

			// Modify by other factors
			totalInputsValue *= MANUFACTURING_INPUT_FACTOR;

			if (totalItems > 0) {
				demand = (1D / totalItems) * totalInputsValue;
			}
		}

		return demand;
	}

	/**
	 * Gets the demand for an input amount resource in a Food Production process.
	 *
	 * @param process  the Food Production process.
	 * @return demand (kg)
	 */
	private double getResourceFoodProductionProcessDemand(GoodsManager owner, Settlement settlement, FoodProductionProcessInfo process) {
		double demand = 0D;
		String name = getAmountResource().getName();

		ProcessItem resourceInput = null;
		for(ProcessItem i : process.getInputList()) {
			if ((ItemType.AMOUNT_RESOURCE == i.getType())
					&& name.equalsIgnoreCase(i.getName())) {
				resourceInput = i;
				break;
			}
		}

		if (resourceInput != null) {
			double outputsValue = 0D;
			for(ProcessItem j : process.getOutputList()) {
				outputsValue += FoodProductionUtil.getProcessItemValue(j, settlement, true);
			}

			double totalItems = 0D;
			for(ProcessItem k : process.getInputList()) {
				totalItems += k.getAmount();
			}

			// Determine value of required process power.
			double powerHrsRequiredPerMillisol = process.getPowerRequired() * MarsTime.HOURS_PER_MILLISOL;
			double powerValue = powerHrsRequiredPerMillisol * settlement.getPowerGrid().getPowerValue();

			double totalInputsValue = (outputsValue - powerValue) * owner.getCommerceFactor(CommerceType.TRADE)
											* owner.getCommerceFactor(CommerceType.CROP)
											* FOOD_PRODUCTION_INPUT_FACTOR;

			if (totalItems > 0D) {
				demand = (1D / totalItems) * totalInputsValue;
			}
		}

		return demand;
	}

	/**
	 * Gets the resource demand for all available meals.
	 *
	 * @param resource the amount resource.
	 * @return demand (kg)
	 */
	private double getAvailableMealDemand(Settlement settlement) {

		if (!getAmountResource().isEdible())
			return 0;
		
		double demand = 0D;
		int id = getID();
		
		if (id == ResourceUtil.tableSaltID) {
			// Assuming a person takes 3 meals per sol
			return MarsTime.AVERAGE_SOLS_PER_ORBIT_NON_LEAPYEAR * 3 * Cooking.AMOUNT_OF_SALT_PER_MEAL; 
		}

		else {
			for (int oilID : Cooking.getOilMenu()) {
				if (id == oilID) {
					// Assuming a person takes 3 meals per sol
					return MarsTime.SOLS_PER_ORBIT_NON_LEAPYEAR * 3 * Cooking.AMOUNT_OF_OIL_PER_MEAL;
				}
			}

			// Determine total demand for cooked meal mass for the settlement.
			double cookedMealDemandSol = personConfig.getFoodConsumptionRate();
			double cookedMealDemandOrbit = cookedMealDemandSol * MarsTime.SOLS_PER_ORBIT_NON_LEAPYEAR;
			int numPeople = settlement.getNumCitizens();
			double cookedMealDemand = numPeople * cookedMealDemandOrbit;
			var meals = SimulationConfig.instance().getMealConfiguration().getDishList();
			int numMeals = meals.size();
			double factor = cookedMealDemand / numMeals * COOKED_MEAL_INPUT_FACTOR;
			
			// Determine demand for the resource as an ingredient for each cooked meal
			// recipe.

			
			for (HotMeal meal : meals) {
				for (Ingredient ingredient : meal.getIngredientList()) {
					if (id == ingredient.getAmountResourceID()) {
						demand += ingredient.getProportion() * factor;
					}
				}
			}
		}

		return demand;
	}

	/**
	 * Gets the demand for a food dessert item.
	 *
	 * @param resource the amount resource.
	 * @return demand (kg)
	 */
	private double getResourceDessertDemand(Settlement settlement) {
		double demand = 0D;

		AmountResource[] dessert = PreparingDessert.getArrayOfDessertsAR();
		boolean hasDessert = false;

		if (dessert[0] != null) {
			for (AmountResource ar : dessert) {
				if (ar.getID() == getID()) {
					hasDessert = true;
					break;
				}
			}

			if (hasDessert) {
				double amountNeededSol = personConfig.getDessertConsumptionRate() / dessert.length;
				int numPeople = settlement.getNumCitizens();
				demand = 5 * Math.log(1.0 + numPeople) * amountNeededSol * DESSERT_FACTOR;
			}
		}

		return demand;
	}

	/**
	 * Gets the demand for a resource from construction sites.
	 *
	 * @return demand (kg)
	 */
	private double getResourceConstructionSiteDemand(Settlement settlement) {
		double base = 0D;
		int resource = getID();

		// Note: Need to filter the construction resources first here
		
		// Add demand for resource required as remaining construction material on
		// construction sites.
		for (ConstructionSite site : settlement.getConstructionManager().getConstructionSites()) {
			if (site.hasUnfinishedStage() && !site.getCurrentConstructionStage().isSalvaging()) {
				ConstructionStage stage = site.getCurrentConstructionStage();
				if (stage.getMissingResources().containsKey(resource)) {
					double requiredAmount = stage.getMissingResources().get(resource);
					base += requiredAmount * CONSTRUCTION_SITE_REQUIRED_RESOURCE_FACTOR;
				}
				
//				logger.info(settlement, site, 20_000L, ResourceUtil.findAmountResourceName(resource) + " needed " 
//						+ Math.round(amount * 10.0)/10.0
//						+ " for constructions.  Raising demand to " + Math.round(demand * 10.0)/10.0);
			}
		}

		return Math.min(GoodsManager.MAX_DEMAND, base / 100);
	}

	/**
	 * Gets the demand for an amount resource as an input in building construction.
	 *
	 * @return demand (kg)
	 */
	private double getResourceConstructionDemand(Settlement settlement) {
		double base = 0D;

		ConstructionValues values = settlement.getConstructionManager().getConstructionValues();
		int bestConstructionSkill = ConstructionUtil.getBestConstructionSkillAtSettlement(settlement);
		for(Entry<ConstructionStageInfo, Double> stageDetail : values.getAllConstructionStageValues(bestConstructionSkill).entrySet()) {
			ConstructionStageInfo stage = stageDetail.getKey();
			double stageValue = stageDetail.getValue();
			if (stageValue > 0D && ConstructionStageInfo.Stage.BUILDING.equals(stage.getType())
					// TODO: reduce the utilization on this method using 5.7% of total cpu
					&& isLocallyConstructable(settlement, stage)) {
				double constructionDemand = getResourceConstructionStageDemand(stage, stageValue);
				if (constructionDemand > 0D) {
					base += constructionDemand;
				}
			}
		}

		return Math.min(GoodsManager.MAX_DEMAND, base / 100);
	}

	/**
	 * Gets the demand for an amount resources as an input for a particular building
	 * construction stage.
	 *
	 * @param stage      the building construction stage.
	 * @param stageValue the building construction stage value (VP).
	 * @return demand (kg)
	 */
	private double getResourceConstructionStageDemand(ConstructionStageInfo stage,
			double stageValue) {

		double demand = 0D;
		double resourceAmount = getPrerequisiteConstructionResourceAmount(stage);

		// TODO Not sure this makes sense
		if (resourceAmount > 0D) {
			Map<Integer, Double> resources = getAllPrerequisiteConstructionResources(stage);
			Map<Integer, Integer> parts = getAllPrerequisiteConstructionParts(stage);

			double totalItems = 0D;

			Iterator<Integer> i = resources.keySet().iterator();
			while (i.hasNext()) {
				totalItems += resources.get(i.next());
			}

			Iterator<Integer> j = parts.keySet().iterator();
			while (j.hasNext()) {
				totalItems += parts.get(j.next());
			}

			if (totalItems > 0 ) {
				double totalInputsValue = stageValue * CONSTRUCTING_INPUT_FACTOR;
				demand = (1D / totalItems) * totalInputsValue;
			}
		}

		return demand;
	}

	/**
	 * Gets the total amount of a given resource required to build a stage.
	 *
	 * @param resource the resource.
	 * @param stage    the stage.
	 * @return total amount (kg) of the resource.
	 */
	private double getPrerequisiteConstructionResourceAmount(ConstructionStageInfo stage) {

		int resource = getID();

		// Add resource amount needed for stage.
		double result = stage.getResources().getOrDefault(resource, 0D);

		// Add resource amount needed for first prestage, if any.
		ConstructionStageInfo preStage1 = stage.getPrerequisiteStage();
		if ((preStage1 != null) && preStage1.isConstructable()) {
			result += preStage1.getResources().getOrDefault(resource, 0D);

			// Add resource amount needed for second prestage, if any.
			ConstructionStageInfo preStage2 = preStage1.getPrerequisiteStage();
			if ((preStage2 != null) && preStage2.isConstructable()) {
				result += preStage2.getResources().getOrDefault(resource, 0D);
			}
		}

		return result;
	}


	/**
	 * Gets the farming demand for the resource.
	 *
	 * @return demand (kg) for the resource.
	 */
	private double getFarmingDemand(GoodsManager owner, Settlement settlement) {
		double demand = 0D;

		// Determine demand for resource at each farming building at settlement.
		for(Building b : settlement.getBuildingManager().getBuildingSet(FunctionType.FARMING)) {
			Farming farm = b.getFarming();
			demand += getFarmingResourceDemand(farm) * owner.getCommerceFactor(CommerceType.CROP) * FARMING_FACTOR;
		}

		return demand;
	}

	/**
	 * Gets the individual greenhouse resource demand
	 *
	 * @param farm
	 * @return
	 */
	private double getFarmingResourceDemand(Farming farm) {
		double base = 0;
		int resource = getID();
		
		double averageGrowingCyclesPerOrbit = farm.getAverageGrowingCyclesPerOrbit();
		double totalCropArea = farm.getGrowingArea();
		int solsInOrbit = MarsTime.SOLS_PER_ORBIT_NON_LEAPYEAR;
		double factor = totalCropArea * averageGrowingCyclesPerOrbit / solsInOrbit;

		if (resource == ResourceUtil.waterID) {
			// Average water consumption rate of crops per orbit using total growing area.
			base = cropConfig.getWaterConsumptionRate() * factor;
		} else if (resource == ResourceUtil.co2ID) {
			// Average co2 consumption rate of crops per orbit using total growing area.
			base = cropConfig.getCarbonDioxideConsumptionRate() * factor * CO2_VALUE_MODIFIER;
		} else if (resource == ResourceUtil.oxygenID) {
			// Average oxygen consumption rate of crops per orbit using total growing area.
			base = cropConfig.getOxygenConsumptionRate() * factor * OXYGEN_VALUE_MODIFIER;
		} else if (resource == ResourceUtil.soilID) {
			// Estimate soil needed for average number of crop plantings for total growing
			// area.
			base = Crop.NEW_SOIL_NEEDED_PER_SQM * factor;
		} else if (resource == ResourceUtil.fertilizerID) {
			// Estimate fertilizer needed for average number of crop plantings for total
			// growing area.
			// Estimate fertilizer needed when grey water not available.
			base = (Crop.FERTILIZER_NEEDED_IN_SOIL_PER_SQM * Crop.FERTILIZER_NEEDED_WATERING * solsInOrbit) * factor * 5;
		} else if (resource == ResourceUtil.greyWaterID) {
			// NOTE: how to properly get rid of grey water? it should NOT be considered an
			// economically vital resource
			// Average grey water consumption rate of crops per orbit using total growing
			// area.
			// demand = cropConfig.getWaterConsumptionRate() * totalCropArea * solsInOrbit;
			base = WASTE_WATER_VALUE_MODIFIER;
		}

		return base;
	}

	/**
	 * Gets the resource demand based on settlement favorite meals.
	 *
	 * @return demand (kg) for the resource.
	 */
	private double getFavoriteMealDemand(GoodsManager owner, Settlement settlement) {
		if (settlement.getNumCitizens() == 0)
			return 0;

		if (!getAmountResource().isEdible())
			return 0;
		
		if (getAmountResource().getGoodType() == GoodType.TISSUE)
			return 0;
		
		double demand = 0D;

		HotMeal mainMeal = null;
		HotMeal sideMeal = null;

		// Q: Why does it scan all People, instead of scanning each meal in MealConfig/meal.xml ? 
		
		// A: Each settlement will have unique demand (and the demand of each ingredient vary 
		// over time) over the food ingredients because it's based on what settlers like to eat.
		
		// Q: how to save cpu cycles from having to look at the favorite main dish 
		// and side dish of a person if they are unchanged ? 
		
		Iterator<Person> i = settlement.getAllAssociatedPeople().iterator();
		while (i.hasNext()) {
			Person p = i.next();
			mainMeal = p.getFavorite().getMainDishHotMeal();
			if (mainMeal != null)
				demand += computeIngredientDemand(owner, getID(), 
						mainMeal.getIngredientList());
			sideMeal = p.getFavorite().getSideDishHotMeal();
			if (sideMeal != null)
				demand += computeIngredientDemand(owner, getID(), 
						sideMeal.getIngredientList());
		}
	
		// Limit the demand between 0 and 100
		demand = Math.max(GoodsManager.MIN_DEMAND, Math.min(GoodsManager.MAX_DEMAND, demand));

		return demand;
	}

	/**
	 * Computes the food ingredient demand.
	 * 
	 * @param owner
	 * @param resource
	 * @param ingredients
	 * @return
	 */
	private double computeIngredientDemand(GoodsManager owner, int resource, List<Ingredient> ingredients) {
		if (ingredients.isEmpty()) {
			logger.info("ingredients is empty.");
			return 0;
		}
			
		double averageDemand = 0D;
		double cropFarmFactor = owner.getCommerceFactor(CommerceType.CROP);
		double factor = .01 * cropFarmFactor;
		double count = 0;
		
		for (Ingredient it : ingredients) {
			count++;
			if (it.getAmountResourceID() == resource) {
				
//				logger.info("resource: " + it.getName());
				GoodType goodType = getAmountResource().getGoodType();

				if (goodType == GoodType.CROP) {
					// Tune demand with various factors
					averageDemand += CROP_FACTOR * factor;
				}
				else if (goodType == GoodType.ORGANISM) {
					averageDemand += ORGANISM_FACTOR * factor;
				}
				else if (goodType == GoodType.SOY_BASED) {
					averageDemand += SOY_BASED_FACTOR * factor;
				}
				else if (goodType == GoodType.DERIVED) {
					averageDemand += DERIVED_FACTOR * factor;
				}
				else if (goodType == GoodType.INSECT) {
					averageDemand += INSECT_FACTOR * factor;
				}		
				else if (goodType == GoodType.OIL) {
					averageDemand += OIL_FACTOR * factor;
				}		
			}		
		}
		// Limit the demand
		averageDemand = averageDemand / count;
		averageDemand = Math.max(GoodsManager.MIN_DEMAND, Math.min(GoodsManager.MAX_DEMAND, averageDemand));

		return averageDemand;
	}

	


	/**
	 * Computes the tissue demand.
	 * 
	 * @param owner
	 * @return
	 */
	private double computeTissueDemandDueToCrop(GoodsManager owner) {

		AmountResource ar = getAmountResource();
		if (ar.getGoodType() == GoodType.TISSUE) {		
			String cropName = ar.getName().replace(" tissue", "");
			// Convert the tissue name to its crop's name
			return owner.getDemandValue(GoodsUtil.getGood(cropName)) * TISSUE_CULTURE_VALUE;
		}
		else
			return 0;
	}
	
	
	/**
	 * Gets the life support demand for an amount resource.
	 *
	 * @param resource the resource to check.
	 * @return demand (kg)
	 */
	private double getLifeSupportDemand(GoodsManager owner, Settlement settlement) {
		int resource = getID();
		
		AmountResource ar = getAmountResource();
		if (ar.isLifeSupport()) {
			double amountNeededSol = 0;
			int numPeople = settlement.getNumCitizens();
	
			if (resource == ResourceUtil.oxygenID) {
				amountNeededSol = personConfig.getNominalO2ConsumptionRate() * OXYGEN_VALUE_MODIFIER;
			} else if (resource == ResourceUtil.waterID) {
				amountNeededSol = personConfig.getWaterConsumptionRate() *  WATER_VALUE_MODIFIER;
			} else if (resource == ResourceUtil.foodID) {
				amountNeededSol = personConfig.getFoodConsumptionRate() * FOOD_VALUE_MODIFIER;
			} else if (resource == ResourceUtil.methaneID) {
				// Methane is fuel for heating and is an arguably life support resource
				amountNeededSol = personConfig.getWaterConsumptionRate() * METHANE_VALUE_MODIFIER;
			} else if (resource == ResourceUtil.co2ID) {
				amountNeededSol = CO2_VALUE_MODIFIER;
			} else if (resource == ResourceUtil.hydrogenID) {
				amountNeededSol = personConfig.getWaterConsumptionRate() * HYDROGEN_VALUE_MODIFIER;
			}
			
		
			return numPeople * amountNeededSol * owner.getCommerceFactor(CommerceType.TRADE)  
					* LIFE_SUPPORT_FACTOR;
		}
		else
			return 0;
	}

	/**
	 * Gets mineral demand.
	 *
	 * @return
	 */
	private double getMineralDemand(GoodsManager owner, Settlement settlement) {
		double base = .25;
		int resource = getID();

        if (resource == ResourceUtil.rockSaltID)
			return base * ROCK_SALT_VALUE_MODIFIER;
		else if (resource == ResourceUtil.epsomSaltID)
			return base * EPSOM_SALT_VALUE_MODIFIER;
		else if (resource == ResourceUtil.soilID)
			// TODO Should be based on growing area
			return base * settlement.getTotalCropArea() * SOIL_VALUE_MODIFIER;
		else if (resource == ResourceUtil.cementID) {
			double cementDemand = owner.getDemandValueWithID(ResourceUtil.cementID);
			double concreteDemand = owner.getDemandValueWithID(ResourceUtil.concreteID);
			double regolithDemand = owner.getDemandValueWithID(ResourceUtil.regolithID);
			double sandDemand = owner.getDemandValueWithID(ResourceUtil.sandID);
			return base * (.5 * cementDemand + .2 * regolithDemand + .2 * sandDemand + .1 * concreteDemand) 
					/ (1 + cementDemand) * CEMENT_VALUE_MODIFIER;
		}
		else if (resource == ResourceUtil.concreteID) {
			double concreteDemand = owner.getDemandValueWithID(ResourceUtil.concreteID);
			double regolithDemand = owner.getDemandValueWithID(ResourceUtil.regolithID);
			double sandDemand = owner.getDemandValueWithID(ResourceUtil.sandID);
			// the demand for sand is dragged up or down by that of regolith
			// loses 5% by default
			return base * (.5 * concreteDemand + .55 * regolithDemand + .25 * sandDemand) 
						/ (1 + concreteDemand) * CONCRETE_VALUE_MODIFIER;
		}
		else if (resource == ResourceUtil.sandID) {
			double regolithDemand = owner.getDemandValueWithID(ResourceUtil.regolithID);
			double sandDemand = owner.getDemandValueWithID(ResourceUtil.sandID);
			// the demand for sand is dragged up or down by that of regolith
			// loses 10% by default
			return base * (.2 * regolithDemand + .7 * sandDemand) 
						/ (1 + sandDemand) * SAND_VALUE_MODIFIER;
		}
        
        else {
			double regolithDemand = owner.getDemandValueWithID(ResourceUtil.regolithID);
			double sandDemand = owner.getDemandValueWithID(ResourceUtil.sandID);

			for (int id : ResourceUtil.rockIDs) {
				if (resource == id) {
					double rockDemand = owner.getDemandValueWithID(id);
					return base * (.2 * regolithDemand + .9 * rockDemand) 
							/ (1 + rockDemand) * ROCK_VALUE_MODIFIER;
				}
			}

			for (int id : ResourceUtil.mineralConcIDs) {
				if (resource == id) {
					double mineralDemand = owner.getDemandValueWithID(id);
					return base * (.2 * regolithDemand + .9 * mineralDemand) 
							/ (1 + mineralDemand) * MINERAL_VALUE_MODIFIER;
				}
			}

			for (int id : ResourceUtil.oreDepositIDs) {
				if (resource == id) {
					double oreDemand = owner.getDemandValueWithID(id);
					// loses 10% by default
					return base * (.3 * regolithDemand + .6 * oreDemand) 
							/ (1 + oreDemand) * ORES_VALUE_MODIFIER;
				}
			}

			if (resource == ResourceUtil.regolithID) {
				return base * regolithDemand * REGOLITH_VALUE_MODIFIER;
			}
			
			else if (resource == ResourceUtil.regolithBID 
					|| resource == ResourceUtil.regolithCID) {
				return base * regolithDemand * REGOLITH_VALUE_MODIFIER_1;
			}
			
			else if (resource == ResourceUtil.regolithDID) {
				return base * regolithDemand * REGOLITH_VALUE_MODIFIER_2;
			}
			
			// Checks if this resource is a ROCK type
			GoodType type = getGoodType();
			if (type != null && type == GoodType.ROCK) {
				double rockDemand = owner.getDemandValueWithID(resource);

				if (resource == METEORITE_ID)
					return base * (.4 * regolithDemand + .5 * rockDemand) 
							/ (1 + rockDemand) * METEORITE_VALUE_MODIFIER;
				else
					return base * (.2 * sandDemand + .7 * rockDemand) 
							/ (1 + rockDemand) * ROCK_VALUE_MODIFIER;
			}
		}

		return base;
	}

	/**
	 * Adjusts the demand for waste resources.
	 *
	 * @return demand (kg)
	 */
	private double modifyWasteResource() {
		int resource = getID();

		if (resource == ResourceUtil.brineWaterID) {
			return BRINE_WATER_VALUE_MODIFIER;
		}
		
		if (resource == ResourceUtil.greyWaterID) {
			return GREY_WATER_VALUE_MODIFIER;
		}

		if (resource == ResourceUtil.blackWaterID) {
			return BLACK_WATER_VALUE_MODIFIER;
		}
		
		if (resource == ResourceUtil.leavesID) {
			return LEAVES_VALUE_MODIFIER;
		}

		if (resource == ResourceUtil.soilID) {
			return SOIL_VALUE_MODIFIER;
		}

		if (resource == ResourceUtil.foodWasteID) {
			return 4 * USEFUL_WASTE_VALUE_MODIFIER;
		}

		if (resource == ResourceUtil.solidWasteID) {
			return .2;
		}
		
		if (resource == ResourceUtil.toxicWasteID) {
			return .05;
		}
		
		if (resource == ResourceUtil.cropWasteID) {
			return 4 * USEFUL_WASTE_VALUE_MODIFIER;
		}

		if (resource == ResourceUtil.compostID) {
			return 2 * USEFUL_WASTE_VALUE_MODIFIER;
		}

//		if (getGoodType() == GoodType.WASTE) {
//			return WASTE_COST;
//		}
		
		return 1;
	}

	/**
	 * Gets the potable water usage demand for an amount resource.
	 *
	 * @param resource the resource to check.
	 * @return demand (kg)
	 */
	private double getPotableWaterUsageDemand(GoodsManager owner, Settlement settlement) {
		double demand = 0;
		if (getID() == ResourceUtil.waterID) {
			// Add the awareness of the water ration level in adjusting the water demand
			double waterRationLevel = settlement.getWaterRationLevel();
			double amountNeededSol = personConfig.getWaterUsageRate();
			int numPeople = settlement.getNumCitizens();
			demand = numPeople * amountNeededSol *  WATER_VALUE_MODIFIER 
					* owner.getCommerceFactor(CommerceType.TRADE)  * (1 + waterRationLevel);
		}

		return demand;
	}

	/**
	 * Computes ice projected demand.
	 *
	 * @param owner
	 * @param settlement
	 * @return demand
	 */
	private double computeIceProjectedDemand(GoodsManager owner, Settlement settlement) {
        int resource = getID();
		if (resource == ResourceUtil.iceID) {
			double ice = 1 + owner.getDemandValueWithID(resource);
			double water = 1 + owner.getDemandValueWithID(ResourceUtil.waterID);
			// Use the water's VP and existing iceSupply to compute the ice demand
			return  (.5 * water + .5 * ice) / ice
					* ICE_VALUE_MODIFIER;
		}

		return 0;
	}

	/**
	 * Computes regolith projected demand.
	 *
	 * @param owner
	 * @param settlement
	 * @return demand
	 */
	private double computeRegolithProjectedDemand(GoodsManager owner, Settlement settlement) {
		double demand = 0;
		int resource = getID();
		// This averaging method make all regolith types to be placed at similar demand
		if (resource == ResourceUtil.regolithBID
			|| resource ==  ResourceUtil.regolithCID
			|| resource ==  ResourceUtil.regolithDID) {

			double sand = owner.getDemandValueWithID(ResourceUtil.sandID);
			double concrete = owner.getDemandValueWithID(ResourceUtil.concreteID);
			double cement = owner.getDemandValueWithID(ResourceUtil.cementID);

			double targetRegolith = owner.getDemandValueWithID(resource);
			double regolith = owner.getDemandValueWithID(ResourceUtil.regolithID);
			double regolithB = owner.getDemandValueWithID(ResourceUtil.regolithBID);
			double regolithC = owner.getDemandValueWithID(ResourceUtil.regolithCID);
			double regolithD = owner.getDemandValueWithID(ResourceUtil.regolithDID);
			
			double averageRegolith = (regolith + regolithB + regolithC + regolithD) / 4.0;
			
			// Limit the minimum value of regolith projected demand
			demand = (cement + concrete + 4 * targetRegolith + 2 * sand + 4 * averageRegolith) 
					* REGOLITH_TYPE_VALUE_MODIFIER;
		}

		return demand;
	}

	/**
	 * Gets the toilet tissue usage demand.
	 *
	 * @param resource the resource to check.
	 * @return demand (kg)
	 */
	private double getToiletryUsageDemand(Settlement settlement) {
		if (getID() == ResourceUtil.toiletTissueID) {
			double amountNeededSol = LivingAccommodation.TOILET_WASTE_PERSON_SOL;
			int numPeople = settlement.getIndoorPeopleCount();
			return numPeople * amountNeededSol;
		}

		return 0;
	}

	/**
	 * Gets vehicle fuel demand for an amount resource.
	 *
	 * @return demand (kg) for the resource.
	 */
	private double getVehicleFuelDemand(GoodsManager owner, Settlement settlement) {
		double demand = 0D;
		double transFactor = owner.getCommerceFactor(CommerceType.TRANSPORT) * VEHICLE_FUEL_FACTOR; 
		if (getID() == ResourceUtil.methanolID) {
			for(Vehicle v: settlement.getAllAssociatedVehicles()) {
				double fuelDemand = v.getAmountResourceCapacity(getID());
				demand += fuelDemand * transFactor * METHANOL_VALUE_MODIFIER;
			}
		}
		
		else if (getID() == ResourceUtil.methaneID) {
			for(Vehicle v: settlement.getAllAssociatedVehicles()) {
				double fuelDemand = v.getAmountResourceCapacity(getID());
				demand += fuelDemand * transFactor * METHANE_VALUE_MODIFIER / 5;
			}
		}

		else if (getID() == ResourceUtil.hydrogenID) {
			demand +=  transFactor * HYDROGEN_VALUE_MODIFIER / 10;
		}

		return demand / 5;
	}
}
