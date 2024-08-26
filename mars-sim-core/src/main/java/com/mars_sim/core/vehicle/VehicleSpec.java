/*
 * Mars Simulation Project
 * VehicleSpec.java
 * @date 2023-05-17
 * @author Barry Evans
 */
package com.mars_sim.core.vehicle;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mars_sim.core.manufacture.ManufactureConfig;
import com.mars_sim.core.manufacture.ManufactureProcessInfo;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.resource.ItemResourceUtil;
import com.mars_sim.core.resource.Part;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.science.ScienceType;
import com.mars_sim.core.structure.building.utility.power.PowerSourceType;
import com.mars_sim.core.tool.Conversion;
import com.mars_sim.mapdata.location.LocalPosition;

/** 
 * The Specification of a Vehicle loaded from the external configuration.
 */
public class VehicleSpec implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 12L;

	// default logger.
	// May add back private static final SimLogger logger = SimLogger.getLogger(VehicleSpec.class.getName())
	
	public static final String DASHES = " -----------------------------------------------------------------------";

	/** The wear lifetime value is 1 orbit. */
	private static final double WEAR_LIFETIME = 668_000D;
	
	// As comparison, 1 gallon (or 3.7854 L) of gasoline has 33.7 kWh of energy. 
	// Energy Density is 8.9 kWh/L or 44-46 MJ/kg
	
	// Future : may move fuel specs to a separate config file

	/**
	 * Methane's Specific Energy is 15.416 kWh/kg (or 55.5 MJ/kg).
	 * 
	 * @See https://hypertextbook.com/facts/2004/BillyWan.shtml
	 */
	public static final double METHANE_SPECIFIC_ENERGY = 15.4167D; // [in kWh/kg]
	/**
	 * Methane's volumetric energy density [in kWh/L]. or 38.3 MJ/L with unknown wt %. 
	 */
	public static final double METHANE_VOLUMETRIC_ENERGY_DENSITY = 7.1689D; 
	
	// 1 liter of methane weighs 0.465 kilogram. or 2.1505 L = 1 kg
	// 
	// 1 kg of methane is equal to 1496.558 liters, and 1 liter of methane is equal to 0.0006682 kg
	// The density of methane is 0.6682 kg/m3 for 25C
	
	// Note : 1 MJ = 0.277778 kWh; 1 kWh = 3.6 MJ
	
	/**
	 * Methanol's Specific Energy is 6.1206 kWh/kg (or 22.0342 MJ/kg)
	 * https://en.wikipedia.org/wiki/Fuel_efficiency uses 19.9 MJ/kg and 17.9 MJ/L
	 */
	public static final double METHANOL_SPECIFIC_ENERGY = 6.1206D; // [in kWh/kg]

	/**
	 * Methanol's volumetric energy density [in kWh/L].
	 */
	public static final double METHANOL_VOLUMETRIC_ENERGY_DENSITY = 4.33D;
	/**
	 * Methanol's gravimetric energy density [in kWh/L].
	 */
	public static final double METHANOL_GRAVIMETRIC_ENERGY_DENSITY = 5.53D;
	
	
	/**
	 * Uranium oxide's specific energy [in kWh/kg].
	 */
	public static final double URANIUM_OXIDE_SPECIFIC_ENERGY = 49_600D;
	
	/**
	 * Stirling conversion efficiency.
	 */
	public static final double STIRLING_CONVERSION_EFFICIENCY = .274D;
	
	/**
	 * The Solid Oxide Fuel Cell (SOFC) Conversion Efficiency for using 
	 * methane is dimension-less. Assume methane can be internally 
	 * reformed within the anode of the fuel cells.
	 */
	public static final double SOFC_CONVERSION_EFFICIENCY = .65D;
	
	/**
	 * Reformed (or indirect)-methanol fuel cells (RMFC) are a subcategory of proton-exchange 
	 * fuel cells in which methanol is used as the fuel.
	 */
	public static final double RMFC_CONVERSION_EFFICIENCY = .95;
	
	/** The kWh-to-Kg conversion factor for our fuel-cell vehicles using methane. */
	public static final double METHANE_KG_PER_KWH = 1.0D / SOFC_CONVERSION_EFFICIENCY / METHANE_SPECIFIC_ENERGY; 
	/** The kg-to-Wh conversion factor for our fuel-cell vehicles using methane. */	
	public static final double METHANE_WH_PER_KG = 1000.0D / METHANE_KG_PER_KWH;

	
	/** The kWh-to-Kg conversion factor for our fuel-cell vehicles using methanol. */
	public static final double METHANOL_KG_PER_KWH = 1.0D / RMFC_CONVERSION_EFFICIENCY / METHANOL_SPECIFIC_ENERGY; 
	/** The kg-to-Wh conversion factor for our fuel-cell vehicles using methanol. */	
	public static final double METHANOL_WH_PER_KG = 1000.0D / METHANOL_KG_PER_KWH; 

	
	/** The kWh-to-Kg conversion factor for our nuclear-powered vehicles using uranium oxide. */
	public static final double URANIUM_OXIDE_KG_PER_KWH = 1.0D / STIRLING_CONVERSION_EFFICIENCY / URANIUM_OXIDE_SPECIFIC_ENERGY; 
	/** The kg-to-Wh conversion factor for our nuclear-powered vehicles using uranium oxide. */	
	public static final double URANIUM_OXIDE_WH_PER_KG = 1000.0D / URANIUM_OXIDE_KG_PER_KWH; 
	
	
	/** Estimated Number of hours traveled each day. **/
	static final int ESTIMATED_TRAVEL_HOURS_PER_SOL = 16;
	
	// Data members
	private boolean hasLab = false;
	private boolean hasPartAttachments = false;
	private boolean hasSickbay = false;

	private int crewSize;
	private int sickbayTechLevel = -1;
	private int sickbayBeds = 0;
	private int labTechLevel = -1;
	private int labCapacity = 0;
	private int attachmentSlots;
	
	/** The # of battery modules of the vehicle.  */
	private int numBatteryModule;
	
	private int fuelTypeID;
	/** The # of fuel cell stacks of the vehicle.  */
	private int numFuelCellStack;

	private double value;
    /** The maximum energy capacity of a standard battery module in kWh. */
    private double energyPerModule;
	/** Base speed of vehicle in kph (can be set in child class). */
	private double baseSpeed = 1;
	/** The base range of the vehicle (with full tank of fuel and no cargo) (km). */
	private double baseRange = 1;
	/** The efficiency of the vehicle's drivetrain. [dimension-less] */
	private double drivetrainEfficiency;
	/** The conversion fuel-to-drive energy factor for a specific vehicle type [Wh/kg] */
	private double fuel2DriveEnergy;
	/** The base acceleration of the vehicle [m/s2]. */
	private double baseAccel = 1;
	
	// 1989 NASA Mars Manned Transportation Vehicle - Shuttle Fuel Cell Power Plant (FCP)  7.6 kg/kW
	// DOE 2010 Targe : Specific power = 650 W_e/L; Power Density = 650 W_e/kg
	// Toyota Mirai Fuel cell - 90 kW
	
	/** The base average power output of the vehicle. (kW). */
	private double basePower = 0;
	/** The peak rated power output of the vehicle. (kW). */
	private double peakPower = 0;
	
	// Define percent of other energy usage (other than for drivetrain)
	private double otherEnergyUsagePercent = 0;
	/** The estimated total number of hours the vehicle can run [hr], given the full tank of fuel. */
	private double baseTotalHours;
	/** The maximum fuel capacity of the vehicle [kg] */
	private double fuelCapacity;
	/** The maximum cargo capacity of the vehicle [kg] */	
	private double totalCapacity = 0D;
	/** The total energy of the vehicle in full tank of methanol [kWh]. */
	private double methanolEnergyCapacity;
	/** The estimated energy available for the drivetrain [kWh]. */
	private double drivetrainEnergy;
	/** The available max energy capacity from the battery [kWh]. */
	private double batteryCapacity;	
	
	// Definition : Fuel economy of a vehicle refers to the distance traveled 
	// by a vehicle and the amount of fuel consumed.
	
	// See https://en.wikipedia.org/wiki/Fuel_efficiency
	
	/** 
	 * The base fuel economy of the vehicle [km/kg].  
	 * Note: it's the distance traveled by a vehicle per kg of fuel consumed
	 */
	private double baseFuelEconomy;
	/** The initial average fuel economy of the vehicle for a trip [km/kg]. */
	private double initialFuelEconomy;

	
	// Definition : Fuel consumption of a vehicle refers to the energy used
	// by a vehicle to travel each km.
	
	// See https://ev-database.org/cheatsheet/energy-consumption-electric-car 
	
	/**
	 * The coefficient for converting FC to FE 
	 */
	private double coefficientBaseFC2FE;
	
	/** 
	 * The base fuel consumption of the vehicle [Wh/km]. 
	 * Note: it's the energy used by a vehicle to travel each km.
	 */
	private double baseFuelConsumption;
	/** The initial average fuel consumption of the vehicle [Wh/km]. */
	private double initialFuelConsumption;
	
	/** The estimated beginning mass of the vehicle (base mass + crew weight + full cargo weight) for a trip [km/kg]. */
	private double beginningMass;
	/** The calculated empty mass of the vehicle, based on its parts [km/kg]. */
	private double calculatedEmptyMass;
	
	/** Width of vehicle (meters). */
	private double width;
	/** Length of vehicle (meters). */
	private double length;
	/** Get estimated total crew weight. */
	private double estimatedTotalCrewWeight;
	/** The terrain handling of vehicle. */
	private double terrainHandling;

	private String baseImage;
	
	private String modelName;
	
	private String name;

	private String description;

	private String fuelTypeStr;

	private VehicleType type;
	
	private PowerSourceType powerSourceType;
	
	private Map<Integer, Double> cargoCapacityMap;
	private List<ScienceType> labTechSpecialties = null;
	private List<Part> attachableParts = null;
	private List<LocalPosition> operatorActivitySpots;
	private List<LocalPosition> passengerActivitySpots;
	private List<LocalPosition> sickBayActivitySpots;
	private List<LocalPosition> labActivitySpots;
	
	private LocalPosition airlockLoc;
	private LocalPosition airlockInteriorLoc;
	private LocalPosition airlockExteriorLoc;

	private Set<Integer> partIDs;

	public VehicleSpec(String name, VehicleType type, String model, String description, String baseImage,
			String powerSourceStr, String fuelTypeStr, double value,
			int batteryModule, double energyPerModule, int fuelCellStack,
			double drivetrainEff, 
			double baseSpeed, double basePower,
			double emptyMass, int crewSize) {
		this.name = name;
		this.modelName = model;
		this.type = type;
		this.description = description;
		this.baseImage = baseImage;
		
		this.powerSourceType = PowerSourceType.getType(powerSourceStr.replaceAll("_", " ").toLowerCase());
		
		this.fuelTypeStr = Conversion.capitalize(fuelTypeStr.toLowerCase());
		
		if (PowerSourceType.FUEL_POWER == powerSourceType) {
			
			if (fuelTypeStr.equalsIgnoreCase(ResourceUtil.METHANOL)) {
				fuelTypeID = ResourceUtil.findAmountResource(ResourceUtil.METHANOL).getID();
			}
			else if (fuelTypeStr.equalsIgnoreCase(ResourceUtil.METHANE)) {
				fuelTypeID = ResourceUtil.findAmountResource(ResourceUtil.METHANE).getID();
			}
		}
		else if (PowerSourceType.FISSION_POWER == powerSourceType) {
			fuelTypeID = -1;
		}
		else if (PowerSourceType.SOLAR_POWER == powerSourceType) {
			fuelTypeID = -1;
		}
		
		this.value = value;
		
		// Set the number of battery modules of the vehicle.
		this.numBatteryModule = batteryModule;	
		// Set the energy kWh per battery module of the vehicle.		
		this.energyPerModule = energyPerModule;
		
		// Set the # of fuel cell stacks of the vehicle.
		this.numFuelCellStack = fuelCellStack;
		// Set the drivetrain efficiency [dimension-less] of the vehicle.
		this.drivetrainEfficiency = drivetrainEff;
		
		// Set base speed.
		this.baseSpeed = baseSpeed;
		// Set base average power when operating the vehicle at average speed.
		this.basePower = basePower;
		// Get the crew capacity
		this.crewSize = crewSize;
	
		// Get estimated total crew weight
		this.estimatedTotalCrewWeight = crewSize * Person.getAverageWeight();
	}
	
	/**
	 * Initializes the specifications.
	 * Note: Called by VehicleConfig to update the cargo capacity.
	 * 
	 * @param manuConfig 
	 */
	void calculateDetails(ManufactureConfig manuConfig) {
		
		calculateEmptyMass(manuConfig);
		
		defineVehiclePerformance();
	}

	/**
	 * Calculates the base/empty mass of this type of vehicle.
	 */
	private void calculateEmptyMass(ManufactureConfig manuConfig) {
				
		// Find the name of the process to build this type of vehicle Spec
		ManufactureProcessInfo buildDetails = null;
		String buildName = "Assemble " + type.name().replace("_", " ");
		for (ManufactureProcessInfo info : manuConfig.getManufactureProcessList()) {
			if (info.getName().equalsIgnoreCase(buildName)) {
				buildDetails = info;
			}
		}
		if (buildDetails == null) {
			throw new IllegalStateException("Can not find Manufacturing process for vehicle called "
											+ buildName);
		}
			
		List<String> names = buildDetails.getInputNames();
		partIDs = ItemResourceUtil.convertNameListToResourceIDs(names);
						
		// Calculate total mass as the summation of the multiplication of the quantity and mass of each part  
		calculatedEmptyMass = buildDetails.calculateTotalInputMass();
	}
	
	/**
	 * Defines the vehicle performance specifications.
	 * 
	 * @param spec
	 */
	private void defineVehiclePerformance() {

    	batteryCapacity = energyPerModule * numBatteryModule;
    	
		if (fuelTypeID > 0) {
			// Gets the capacity [in kg] of vehicle's fuel tank
			fuelCapacity = getCargoCapacity(getFuelType());
			// Gets the energy capacity [kWh] based on a full tank of methanol
			methanolEnergyCapacity = fuelCapacity / METHANOL_KG_PER_KWH;
			// Gets the conversion factor for a specific vehicle [Wh/kg]
			fuel2DriveEnergy =  METHANOL_WH_PER_KG * drivetrainEfficiency + batteryCapacity;
		}
		else if (fuelTypeStr.equalsIgnoreCase("NUCLEAR")){
			// Gets the capacity [in kg] of vehicle's fuel tank
			fuelCapacity = .01;
			// Gets the energy capacity [kWh] based on a full tank of methanol
			methanolEnergyCapacity = fuelCapacity / URANIUM_OXIDE_KG_PER_KWH ;
			// Gets the conversion factor for a specific vehicle [Wh/kg]
			fuel2DriveEnergy = URANIUM_OXIDE_WH_PER_KG * drivetrainEfficiency + batteryCapacity;
		}
		
		// Assume the peak power is related to the average power, number of battery modules and numbers of fuel cell stack.
		peakPower = basePower * Math.log(3 + 1.5 * (1 + numBatteryModule) + 1.5 * (1 + numFuelCellStack));
		// Define the estimated additional beginning mass for each type of vehicle
		double additionalBeginningMass = 0;
		// Define the estimated additional end mass for each type of vehicle
		double additionalEndMass = 0;		

		switch (type) {
			// see https://droneii.com/drone-energy-sources
			case DELIVERY_DRONE: {
				// Hard-code percent energy usage for this vehicle.
				otherEnergyUsagePercent = 2.0;
				// Accounts for the fuel (methanol and oxygen) and the traded goods
				additionalBeginningMass = 500;
				// Accounts for water and the traded goods
				additionalEndMass = 400;		
			} break;

			case LUV: {
				// Hard-code percent energy usage for this vehicle.
				otherEnergyUsagePercent = 3.0;
				// Accounts for the occupant weight
				additionalBeginningMass = estimatedTotalCrewWeight;
				// Accounts for the occupant weight
				additionalEndMass = estimatedTotalCrewWeight;			
			} break;

			case EXPLORER_ROVER: {
				// Hard-code percent energy usage for this vehicle.
				otherEnergyUsagePercent = 7.5;
				// Accounts for the occupants and their consumables
				additionalBeginningMass = estimatedTotalCrewWeight + 4 * 50;
				// Accounts for the occupant and rock sample, ice or regolith collected
				additionalEndMass = estimatedTotalCrewWeight + 800;	
			} break;

			case CARGO_ROVER: {
				// Hard-code percent energy usage for this vehicle.
				otherEnergyUsagePercent = 5.0;
				// Accounts for the occupants and their consumables and traded goods 
				additionalBeginningMass = estimatedTotalCrewWeight + 2 * 50 + 1500;
				// Accounts for the occupants and traded goods
				additionalEndMass = estimatedTotalCrewWeight + 1500;				
			} break;

			case TRANSPORT_ROVER: {
				// Hard-code percent energy usage for this vehicle.
				otherEnergyUsagePercent = 10.0;
				// Accounts for the occupants and their consumables and personal possession
				additionalBeginningMass = estimatedTotalCrewWeight + 8 * (50 + 100);
				// Accounts for the occupants and their personal possession
				additionalEndMass = estimatedTotalCrewWeight + 8 * 100;				
			} break;
		}

		// Gets the estimated energy available for drivetrain [in kWh]
		drivetrainEnergy = methanolEnergyCapacity * (1.0 - otherEnergyUsagePercent / 100.0) * drivetrainEfficiency + batteryCapacity;
		
		// Gets the estimated energy available to be consumed for the trip [in kWh]
		double baseEnergyConsumed = methanolEnergyCapacity * drivetrainEfficiency + batteryCapacity;
		// Gets the estimated average road load power (including coasting)
		double baseRoadLoadPower = .25 * (.15 * peakPower + .85 * basePower);
		// Gets the maximum total # of hours the vehicle is capable of operating
		baseTotalHours = baseEnergyConsumed / baseRoadLoadPower;
		// kW / kph -> (kW / km / h) -> kW * h / km
		double baseForce = baseRoadLoadPower / baseSpeed; 
	
		// Gets the base range [in km] of the vehicle
		// km = kWh / (kW * h / km) -> km * h / h -> km
		baseRange = baseEnergyConsumed / baseForce;
		// Gets the base fuel economy [in km/kg] of this vehicle
		baseFuelEconomy = baseRange / (1 + fuelCapacity);
		// Gets the base fuel consumption [in Wh/km] of this vehicle. Convert estEnergyConsumed from kWh to Wh.
		baseFuelConsumption =  1000 * baseEnergyConsumed / (1 + baseRange);
		// Gets the base coeff for FC to FE
		coefficientBaseFC2FE = baseFuelEconomy / baseFuelConsumption * baseRoadLoadPower / baseSpeed;
		
		// Accounts for the estimated additional beginning mass
		beginningMass = calculatedEmptyMass + additionalBeginningMass;
		// Accounts for the estimated additional end mass
		double endMass = calculatedEmptyMass + additionalEndMass;
		// Accounts for the additional payload mass
		double massModifier = 1 + .2 * (additionalBeginningMass/calculatedEmptyMass 
				+ additionalEndMass/calculatedEmptyMass);
		
		// Gets the initial fuel economy for a trip [km/kg]
		initialFuelEconomy = baseFuelEconomy / massModifier; 
		// Gets the initial fuel consumption [in Wh/km] of this vehicle
		initialFuelConsumption = baseFuelConsumption * massModifier;
		
		// Gets the base acceleration [m/s2]
		baseAccel = peakPower / (1 + .5 * (endMass + beginningMass)) / (1 + baseSpeed) * 1000 * 3.6;
	}
	
	public final void setWidth(double width) {
		this.width = width;
	}
	
	public final void setLength(double length) {
		this.length = length;
	}
	
	/**
	 * Gets <code>0.0d</code> or capacity for given cargo.
	 * 
	 * @param cargo {@link String}
	 * @return {@link Double}
	 */
	public final double getCargoCapacity(int resourceId) {
		if (cargoCapacityMap != null) {
			return cargoCapacityMap.getOrDefault(resourceId, 0D);
		}
		
		return 0D;
	}

	/**
	 * Gets the name of the vehicle.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	
	/**
	 * Gets the model name of the vehicle.
	 * 
	 * @return
	 */
	public String getModelName() {
		return modelName;
	}

	/**
	 * Gets the type of the vehicle specification.
	 * 
	 * @return
	 */
	public VehicleType getType() {
		return type;
	}
	
	/**
	 * Gets the description of the vehicle.
	 * 
	 * @return the description 
	 */
	public final String getDescription() {
		return description;
	}

	/**
	 * Gets the width of the vehicle.
	 * 
	 * @return the width 
	 */
	public final double getWidth() {
		return width;
	}

	/**
	 * Gets the length of the vehicle.
	 * 
	 * @return the length 
	 */
	public final double getLength() {
		return length;
	}
	
	/**
	 * Gets the number of modules of the battery.
	 * 
	 * @return
	 */
	public int getBatteryModule() {
		return numBatteryModule;
	}
	
	/**
	 * Gets the energy per module of the battery module [kWh].
	 * 
	 * @return
	 */
	public double getEnergyPerModule() {
		return energyPerModule;
	}
	
	/**
	 * Gets the total battery capacity of the vehicle.
	 *
	 * @return
	 */
	public double getBatteryCapacity() {
		return batteryCapacity;
	}
	
	/**
	 * Gets the number of fuel cell stacks.
	 * 
	 * @return
	 */
	public int getFuelCellStack() {
		return numFuelCellStack;
	}
	
	/**
	 * Gets the drivetrain efficiency of the vehicle.
	 *
	 * @return drivetrain efficiency
	 */
	public double getDrivetrainEfficiency() {
		return drivetrainEfficiency;
	}
	

	/** 
	 * Returns the base speed output of the vehicle [in kph].
	 * 
	 * @return the baseSpeed 
	 */
	public final double getBaseSpeed() {
		return baseSpeed;
	}
	
	/** 
	 * Returns the base power output of the vehicle [in kW].
	 * 
	 * @return the basePower 
	 */
	public final double getBasePower() {
		return basePower;
	}
	
	/** 
	 * Returns the peak power output of the vehicle [in kW].
	 */
	public final double getPeakPower() {
		return peakPower;
	}
	

	/** @return the emptyMass */
	public final double getEmptyMass() {
		return calculatedEmptyMass;
	}

	/** @return the crewSize */
	public final int getCrewSize() {
		return crewSize;
	}

	/** @return the totalCapacity */
	public final double getTotalCapacity() {
		return totalCapacity;
	}

	/** @return the cargoCapacity */
	public final Map<Integer, Double> getCargoCapacityMap() {
		return cargoCapacityMap;
	}

	/** @return the hasSickbay */
	public final boolean hasSickbay() {
		return hasSickbay;
	}

	/** @return the hasLab */
	public final boolean hasLab() {
		return hasLab;
	}

	/** @return the hasPartAttachments */
	public final boolean hasPartAttachments() {
		return hasPartAttachments;
	}

	/** @return the sickbayTechLevel */
	public final int getSickbayTechLevel() {
		return sickbayTechLevel;
	}

	/** @return the sickbayBeds */
	public final int getSickbayBeds() {
		return sickbayBeds;
	}

	/**
	 * Gets the labTechLevel.
	 * 
	 * @return
	 */
	public final int getLabTechLevel() {
		return labTechLevel;
	}

	/**
	 * Gets the lab capacity.
	 * 
	 * @return
	 */
	public final int getLabCapacity() {
		return labCapacity;
	}
	
	/** @return the attachmentSlots */
	public final int getAttachmentSlots() {
		return attachmentSlots;
	}

	/** @return the labTechSpecialties */
	public final List<ScienceType> getLabTechSpecialties() {
		return labTechSpecialties;
	}

	/** @return the attachableParts */
	public final List<Part> getAttachableParts() {
		return attachableParts;
	}

	/** @return the airlockLoc */
	public final LocalPosition getAirlockLoc() {
		return airlockLoc;
	}

	/** @return the airlockInteriorLoc */
	public final LocalPosition getAirlockInteriorLoc() {
		return airlockInteriorLoc;
	}

	/** @return the airlockExteriorXLoc */
	public final LocalPosition getAirlockExteriorLoc() {
		return airlockExteriorLoc;
	}

	/** @return the operator activity spots. */
	public final List<LocalPosition> getOperatorActivitySpots() {
		return operatorActivitySpots;
	}

	/** @return the passenger activity spots. */
	public final List<LocalPosition> getPassengerActivitySpots() {
		return passengerActivitySpots;
	}

	/** @return the sick bay activity spots. */
	public final List<LocalPosition> getSickBayActivitySpots() {
		return sickBayActivitySpots;
	}

	/** @return the lab activity spots. */
	public final List<LocalPosition> getLabActivitySpots() {
		return labActivitySpots;
	}

	void setSickBay(int sickbayTechLevel2, int sickbayBeds2) {
		this.hasSickbay = true;
		this.sickbayBeds = sickbayBeds2;
		this.sickbayTechLevel = sickbayTechLevel2;
	}

	void setLabSpec(int labTechLevel, int labCapacity, List<ScienceType> labTechSpecialties2) {
		this.hasLab = true;
		this.labTechLevel = labTechLevel;
		this.labCapacity = labCapacity;
		this.labTechSpecialties = Collections.unmodifiableList(labTechSpecialties2);
	}

	void setAttachments(int attachmentSlots2, List<Part> attachableParts2) {
		this.hasPartAttachments = true;
		this.attachmentSlots = attachmentSlots2;
		this.attachableParts = Collections.unmodifiableList(attachableParts2);
	}

	void setActivitySpots(List<LocalPosition> operatorActivitySpots2, List<LocalPosition> passengerActivitySpots2,
			List<LocalPosition> sickBayActivitySpots2, List<LocalPosition> labActivitySpots2) {
		this.operatorActivitySpots = Collections.unmodifiableList(operatorActivitySpots2);
		this.passengerActivitySpots = Collections.unmodifiableList(passengerActivitySpots2);
		this.sickBayActivitySpots = Collections.unmodifiableList(sickBayActivitySpots2);
		this.labActivitySpots = Collections.unmodifiableList(labActivitySpots2);	
	}

	void setAirlock(LocalPosition airlockLoc, LocalPosition airlockInteriorLoc,
			LocalPosition airlockExteriorLoc) {
		this.airlockLoc = airlockLoc;
		this.airlockInteriorLoc = airlockInteriorLoc;
		this.airlockExteriorLoc = airlockExteriorLoc;
	}

	void setCargoCapacity(double totalCapacity2, Map<Integer, Double> cargoCapacityMap2) {
		this.totalCapacity = totalCapacity2;
		this.cargoCapacityMap = cargoCapacityMap2;
	}

	void setTerrainHandling(double terrainHandling) {
		this.terrainHandling = terrainHandling;
	}
	
	public double getTerrainHandling() {
		return terrainHandling;
	}

    public String getBaseImage() {
        return baseImage;
    }
    
	/**
	 * Gets the base range of the vehicle.
	 *
	 * @return the base range of the vehicle [km]
	 * @throws Exception if error getting range.
	 */
	public double getBaseRange() {
		return baseRange;
	}

	/**
	 * Gets the resource type id that this vehicle uses as fuel.
	 *
	 * @return resource type id
	 */
	public int getFuelType() {
		return fuelTypeID;
	}
	
	/**
	 * Gets the power source type that this vehicle uses as fuel.
	 *
	 * @return power source type 
	 */
	public PowerSourceType getPowerSourceType() {
		return powerSourceType;
	}
	
	/**
	 * Gets the fuel type of this vehicle.
	 *
	 * @return fuel type string
	 */
	public String getFuelTypeStr() {
		return fuelTypeStr;
	}
	
	/**
	 * Gets the fuel capacity of the vehicle [kg].
	 *
	 * @return
	 */
	public double getFuelCapacity() {
		return fuelCapacity;
	}

	/**
	 * Gets the energy available at the full tank [kWh].
	 *
	 * @return
	 */
	public double getEnergyCapacity() {
		return methanolEnergyCapacity;
	}

	/**
	 * Gets the estimated energy available for the drivetrain [kWh].
	 *
	 * @return
	 */
	public double getDrivetrainEnergy() {
		return drivetrainEnergy;
	}
	
	/**
	 * Gets the fuel-to-drivetrain energy conversion factor [Wh/kg].
	 * 
	 * @return
	 */
	public double getFuel2DriveEnergy() {
		return fuel2DriveEnergy;
	}
	
	/**
	 * Gets the base fuel economy of the vehicle [km/kg].
	 * 
	 * @return
	 */
	public double getBaseFuelEconomy() {
		return baseFuelEconomy;
	}

	/**
	 * Gets the initial fuel economy of the vehicle [km/kg] for a trip.
	 * Note: Assume that it is half of two fuel consumption values (between the beginning and the end of the trip)
	 *
	 * @return
	 */
	public double getInitialFuelEconomy() {
		return initialFuelEconomy;
	}
	
	/**
	 * Gets the coefficient for converting base FC to base FE.
	 * 
	 * @return
	 */
	public double getCoeffBaseFC2FE() {
		return coefficientBaseFC2FE;
	}
	
	/**
	 * Gets the base fuel consumption of the vehicle [Wh/km].
	 * 
	 * @return
	 */
	public double getBaseFuelConsumption() {
		return baseFuelConsumption;
	}
	
	/**
	 * Gets the initial fuel consumption of the vehicle [Wh/km] for a trip.
	 * Note: Assume that it is half of two fuel consumption values (between the beginning and the end of the trip)
	 *
	 * @return
	 */
	public double getInitialFuelConsumption() {
		return initialFuelConsumption;
	}
	
	
	/**
	 * Gets the estimated beginning mass [kg].
	 */
	public double getBeginningMass() {
		return beginningMass;
	}
	
	/**
	 * Gets the base acceleration of the vehicle [m/s2].
	 * 
	 * @return
	 */
	public double getBaseAccel() {
		return baseAccel;
	}
	
	/**
	 * Gets the base total hours the vehicle can operate.
	 * 
	 * @return
	 */
	public double getBaseTotalHours() {
		return baseTotalHours;
	}

	/**
	 * Gets the wear lifetime [in millisols].
	 * 
	 * @return
	 */
	public double getWearLifetime() {
		return WEAR_LIFETIME *  getWearModifier();
	}
	 
	/**
	 * Gets the wear modifier for this vehicle spec.
	 */
    public double getWearModifier() {
	
		return switch(type) {
			case DELIVERY_DRONE -> .85;
			case LUV -> 2D;
			case EXPLORER_ROVER -> 1D;
			case TRANSPORT_ROVER -> 1.25D;
			case CARGO_ROVER -> 1.25D;
		};
    }

	/**
	 * Gets the parts of the consistent this vehicle spec.
	 * 
	 * @return
	 */
	public Set<Integer> getParts() {
		return partIDs;
	}
	
	/**
	 * Returns the specified value of the power source of this vehicle spec.
	 * 
	 * @return
	 */
	public double getValue() {
		return value;
	}
	
	public double getOtherEnergyUsagePercent() {
		return otherEnergyUsagePercent;
	}
	
	public void destroy() {
		cargoCapacityMap = null;
		labTechSpecialties = null;
		attachableParts = null;
		operatorActivitySpots = null;
		passengerActivitySpots = null;
		sickBayActivitySpots = null;
		labActivitySpots = null;
		airlockLoc = null;
		airlockInteriorLoc = null;
		airlockExteriorLoc = null;
		partIDs = null;
	}
}
