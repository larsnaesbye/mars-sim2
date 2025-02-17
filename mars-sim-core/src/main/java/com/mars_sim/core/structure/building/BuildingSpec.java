/*
 * Mars Simulation Project
 * BuildingSpec.java
 * @date 2024-07-12
 * @author Barry Evans
 */
package com.mars_sim.core.structure.building;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.mars_sim.core.map.location.BoundedObject;
import com.mars_sim.core.map.location.LocalPosition;
import com.mars_sim.core.resourceprocess.ResourceProcessEngine;
import com.mars_sim.core.science.ScienceType;
import com.mars_sim.core.structure.building.function.FunctionType;

/**
 * The specification of a certain Building Type.
 *
 */
public class BuildingSpec {
	
	// Empty list constants
	private static final List<SourceSpec> EMPTY_SOURCE = new ArrayList<>();
	private static final List<ScienceType> EMPTY_SCIENCE = new ArrayList<>();
	private static final List<ResourceProcessEngine> EMPTY_RESOURCE = new ArrayList<>();
	
	/** is the building non-habitable. */
	private boolean isInhabitable = true;
	
	private int baseLevel;
	private int maintenanceTime;
	private int wearLifeTime;
	
	private double basePowerRequirement;
	private double basePowerDownPowerRequirement;
	private double presetTemperature;

	private double length;
	private double width;
	
	private double baseMass;
	
	private String alignment;
	private String buildingType;
	private String description;
	
	/**
	 * The type of material use for the construction of the wall of a building.
	 * Solid by default 
	 */
	private ConstructionType constructionType = ConstructionType.PRE_FABRICATED;

	private Map<FunctionType, FunctionSpec> supportedFunctions;
	
	// Optional Function details
	private Map<Integer, Double> storageMap = null;
	private Map<Integer, Double> initialMap = null;

	private List<SourceSpec> heatSourceList = EMPTY_SOURCE;
	private List<SourceSpec> powerSource = EMPTY_SOURCE;
	
	private List<ScienceType> scienceType = EMPTY_SCIENCE;
	
	private List<ResourceProcessEngine> resourceProcess = EMPTY_RESOURCE;
	private List<ResourceProcessEngine> wasteProcess = EMPTY_RESOURCE;

	private Set<LocalPosition> beds;
	private Set<LocalPosition> parking;
	private Set<LocalPosition> flyerParking;

	private BuildingCategory category;
	
	/**
	 * The constructor for BuildingSpecs.
	 * 
	 * @param buildingType
	 * @param description
	 * @param category
	 * @param width
	 * @param length
	 * @param alignment
	 * @param baseLevel
	 * @param presetTemperature
	 * @param maintenanceTime
	 * @param wearLifeTime
	 * @param basePowerRequirement
	 * @param basePowerDownPowerRequirement
	 * @param supportedFunctions
	 */
	BuildingSpec(String buildingType, String description, BuildingCategory category, 
			double width, double length, String alignment, int baseLevel,
			double presetTemperature, int maintenanceTime,
			int wearLifeTime, double basePowerRequirement, double basePowerDownPowerRequirement,
			Map<FunctionType, FunctionSpec> supportedFunctions) {
		
		super();
		
		this.buildingType = buildingType;
		this.description = description;
		this.category = category;
		this.width = width;
		this.length = length;
		this.alignment = alignment;
		this.baseLevel = baseLevel;
		this.presetTemperature = presetTemperature;
		this.maintenanceTime = maintenanceTime;
		this.wearLifeTime = wearLifeTime;
		this.basePowerRequirement = basePowerRequirement;
		this.basePowerDownPowerRequirement = basePowerDownPowerRequirement;
		this.supportedFunctions = supportedFunctions;
		
		if (supportedFunctions.containsKey(FunctionType.LIFE_SUPPORT))
			isInhabitable = false;
	}

	/**
	 * Checks if this building is isInhabitable.
	 * 
	 * @return
	 */
	public boolean isInhabitable() {
		return isInhabitable;
	}
	
	/**
	 * What functions are supported by this building type.
	 * 
	 * @return
	 */
	public Set<FunctionType> getFunctionSupported() {
		return supportedFunctions.keySet();	
	}
	
	/**
	 * Gets the function details for this building type.
	 * 
	 * @param function
	 * @return
	 */
	public FunctionSpec getFunctionSpec(FunctionType function) {
		return supportedFunctions.get(function);
	}
	
	public int getBaseLevel() {
		return baseLevel;
	}
	
	public String getName() {
		return buildingType;
	}

	public String getDescription() {
		return description;
	}

	public BuildingCategory getCategory() {
		return category;
	}

	public double getBasePowerRequirement() {
		return basePowerRequirement;
	}

	public double getBasePowerDownPowerRequirement() {
		return basePowerDownPowerRequirement;
	}
	
	public int getWearLifeTime() {
		return wearLifeTime;
	}

	public int getMaintenanceTime() {
		return maintenanceTime;
	}

	public double getPresetTemperature() {
		return presetTemperature;
	}

	public double getLength() {
		return length;
	}

	public double getWidth() {
		return width;
	}
	
    /**
     * Gets the north-south alignment info.
     * 
     * @return
     */
	public String getAlignment() {
		return alignment;
	}
	
	public double getBaseMass() {
		return baseMass;
	}
	
	public void setBaseMass(double value) {
		baseMass = value;
	}
	
	public ConstructionType getConstruction() {
		return constructionType;
	}

	public void setConstruction(ConstructionType type) {
		this.constructionType = type;
	}

	public Map<Integer, Double> getStorage() {
		return storageMap;
	}
	
	public Map<Integer, Double> getInitialResources() {
		return initialMap;
	}

	void setStorage(Map<Integer, Double> storageMap, Map<Integer, Double> initialMap) {
		this.storageMap = storageMap;
		this.initialMap = initialMap;
	}

	public void setHeatSource(List<SourceSpec> heatSourceList) {
		this.heatSourceList = heatSourceList;
	}
	
	public List<SourceSpec> getHeatSource() {
		return heatSourceList;
	}
	
	public void setPowerSource(List<SourceSpec> powerSource) {
		this.powerSource = powerSource;
	}
	
	public List<SourceSpec> getPowerSource() {
		return powerSource;
	}

	public void setScienceType(List<ScienceType> scienceType) {
		this.scienceType = scienceType;
	}
	
	public List<ScienceType> getScienceType() {
		return scienceType;
	}

	public void setResourceProcess(List<ResourceProcessEngine> resourceProcesses) {
		this.resourceProcess = resourceProcesses;
	}

	public List<ResourceProcessEngine> getResourceProcess() {
		return resourceProcess;
	}
	
	public void setWasteProcess(List<ResourceProcessEngine> list) {
		this.wasteProcess = list;
	}

	public List<ResourceProcessEngine> getWasteProcess() {
		return wasteProcess;
	}
	
	public String toString() {
		return buildingType;
	}
	
	public Set<LocalPosition> getBeds() {
		return beds;
	}

	void setBeds(Set<LocalPosition> beds) {
		this.beds = beds;
	}
	
	public Set<LocalPosition> getParking() {
		return parking;
	}

	void setParking(Set<LocalPosition> parking) {
		this.parking = Collections.unmodifiableSet(parking);
	}
	
	public Set<LocalPosition> getFlyerParking() {
		return flyerParking;
	}

	void setFlyerParking(Set<LocalPosition> droneParking) {
		this.flyerParking = Collections.unmodifiableSet(droneParking);
	}

	/**
	 * Combines a custom bounds definition with what is validate for this building.
	 * 
	 * @param bounds Request bounds
	 * @return Combined valid bounds
	 */
    public BoundedObject getValidBounds(BoundedObject bounds) {
		var newWidth = width;
		if (newWidth < 0) {
			newWidth = bounds.getWidth();
		}
		var newLength = length;
		if (newLength < 0) {
			newLength = bounds.getLength();
		}
        return new BoundedObject(bounds.getPosition(), newWidth, newLength, bounds.getFacing());
    }
}
