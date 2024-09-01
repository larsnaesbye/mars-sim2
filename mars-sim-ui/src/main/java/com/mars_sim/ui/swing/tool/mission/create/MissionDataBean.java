/*
 * Mars Simulation Project
 * MissionDataBean.java
 * @date 2021-08-28
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.tool.mission.create;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.mars_sim.core.Simulation;
import com.mars_sim.core.environment.ExploredLocation;
import com.mars_sim.core.goods.Good;
import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.mission.AreologyFieldStudy;
import com.mars_sim.core.person.ai.mission.BiologyFieldStudy;
import com.mars_sim.core.person.ai.mission.ConstructionMission;
import com.mars_sim.core.person.ai.mission.SalvageMission;
import com.mars_sim.core.person.ai.mission.CollectIce;
import com.mars_sim.core.person.ai.mission.CollectRegolith;
import com.mars_sim.core.person.ai.mission.Delivery;
import com.mars_sim.core.person.ai.mission.EmergencySupply;
import com.mars_sim.core.person.ai.mission.Exploration;
import com.mars_sim.core.person.ai.mission.MeteorologyFieldStudy;
import com.mars_sim.core.person.ai.mission.Mining;
import com.mars_sim.core.person.ai.mission.Mission;
import com.mars_sim.core.person.ai.mission.MissionManager;
import com.mars_sim.core.person.ai.mission.MissionType;
import com.mars_sim.core.person.ai.mission.RescueSalvageVehicle;
import com.mars_sim.core.person.ai.mission.Trade;
import com.mars_sim.core.person.ai.mission.TravelToSettlement;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.science.ScientificStudy;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.construction.ConstructionSite;
import com.mars_sim.core.structure.construction.ConstructionStageInfo;
import com.mars_sim.core.vehicle.Drone;
import com.mars_sim.core.vehicle.GroundVehicle;
import com.mars_sim.core.vehicle.LightUtilityVehicle;
import com.mars_sim.core.vehicle.Rover;

/**
 * Mission data holder bean.
 */
class MissionDataBean {

	// Data members.
    private double constructionSiteXLoc;
    private double constructionSiteYLoc;
    private double constructionSiteFacing;

    private MissionType missionType;
	private String type = "";
	private String description = "";
	private String designation = "";
	
	private Settlement startingSettlement;
	private Settlement destinationSettlement;
    private Settlement constructionSettlement;
    private Settlement salvageSettlement;
    
	private Drone drone;
	private Rover rover;
	private Rover rescueRover;
	private LightUtilityVehicle luv;
	
	private Coordinates fieldSite;
	private Coordinates iceCollectionSite;
	private Coordinates regolithCollectionSite;
	
	private Coordinates[] explorationSites;
	
	private ExploredLocation miningSite;
    
    private ConstructionSite constructionSite;
    private ConstructionStageInfo constructionStageInfo;
    private ConstructionSite salvageSite;
    
    private Building salvageBuilding;
  
    private Person leadResearcher;
    private ScientificStudy study;
    
	private Collection<Worker> mixedMembers = new HashSet<>();
	private Collection<Worker> botMembers = new HashSet<>();
    private List<GroundVehicle> constructionVehicles;
    private List<GroundVehicle> salvageVehicles;
    private Map<Good, Integer> emergencyGoods;
	private Map<Good, Integer> sellGoods;
	private Map<Good, Integer> buyGoods;
	
    private static MissionManager missionManager = Simulation.instance().getMissionManager();
    
	/**
	 * Creates a mission from the mission data.
	 */
    protected void createMission() {
    	// Note: how to resolve the situation when rover is no longer available ?
    	
	    Mission mission = null;
	    if (MissionType.AREOLOGY == missionType) {
	        mission = new AreologyFieldStudy(mixedMembers, leadResearcher, study,
	                rover, fieldSite);
	    }
	    
	    else if (MissionType.BIOLOGY == missionType) {
	        mission = new BiologyFieldStudy(mixedMembers, leadResearcher, study,
	                rover, fieldSite);
	    }
	    
	    else if (MissionType.METEOROLOGY == missionType) {
	        mission = new MeteorologyFieldStudy(mixedMembers, leadResearcher, study,
	                rover, fieldSite);
	    }
	    
	    else if (MissionType.CONSTRUCTION == missionType) {
	        mission = new ConstructionMission(mixedMembers, constructionSettlement, constructionSite,
	                constructionStageInfo, constructionSiteXLoc, constructionSiteYLoc, constructionSiteFacing,
	                constructionVehicles);
	    }

	    else if (MissionType.COLLECT_ICE == missionType) {
	        List<Coordinates> collectionSites = new ArrayList<>(1);
	        collectionSites.add(iceCollectionSite);
	        mission = new CollectIce(mixedMembers, collectionSites, rover);
	    }
	    
	    else if (MissionType.COLLECT_REGOLITH == missionType) {
	        List<Coordinates> collectionSites = new ArrayList<>(1);
	        collectionSites.add(regolithCollectionSite);
	        mission = new CollectRegolith(mixedMembers, collectionSites, rover);
	    }
	    
	    else if (MissionType.DELIVERY == missionType) {
	    	Person startingMember = null;
	    	for (Worker mm: mixedMembers) {
	    		if (mm instanceof Person p)
	    			startingMember = p;
	    	}
		
	        mission = new Delivery(startingMember, mixedMembers, destinationSettlement, drone,
	                sellGoods, buyGoods);
	    }	 
	        
	    else if (MissionType.EMERGENCY_SUPPLY == missionType) {
	        mission = new EmergencySupply(mixedMembers, destinationSettlement,
	                emergencyGoods, rover);
	    }
	    
	    else if (MissionType.EXPLORATION == missionType) {
	        List<Coordinates> collectionSites = new ArrayList<>(explorationSites.length);
	        collectionSites.addAll(Arrays.asList(explorationSites));
	        mission = new Exploration(mixedMembers, collectionSites, rover);
	    }
	    
	    else if (MissionType.MINING == missionType) {
	        mission = new Mining(mixedMembers, miningSite, rover, luv);
	    }
	    
	    else if (MissionType.RESCUE_SALVAGE_VEHICLE == missionType) {
	        mission = new RescueSalvageVehicle(mixedMembers, rescueRover, rover);
	    }
	    
	    else if (MissionType.SALVAGE == missionType) {
	        mission = new SalvageMission(mixedMembers, salvageSettlement, salvageBuilding, salvageSite,
	                salvageVehicles);
	    }
	    
	    else if (MissionType.TRADE == missionType) {
	        mission = new Trade(mixedMembers, destinationSettlement, rover,
	                sellGoods, buyGoods);
	    }  
	    
	    else if (MissionType.TRAVEL_TO_SETTLEMENT == missionType) {
	        mission = new TravelToSettlement(mixedMembers, destinationSettlement, rover);
	    }
	    
	    else throw new IllegalStateException("Mission type: " + type + " unknown");

//		mission.setName(description);
	    missionManager.addMission(mission);
	}
 
	/**
	 * Gets mission types.
	 * @return array of mission type strings.
	 */
    protected static MissionType[] getMissionTypes() {
    	MissionType[] result = { 
    			MissionType.AREOLOGY,
    			MissionType.BIOLOGY,
    			MissionType.CONSTRUCTION,
    			MissionType.SALVAGE,
    			MissionType.COLLECT_ICE,
    			
    			MissionType.COLLECT_REGOLITH,
    			MissionType.DELIVERY,
    			MissionType.EMERGENCY_SUPPLY,
    			MissionType.EXPLORATION,
    			MissionType.METEOROLOGY,
    			
    			MissionType.MINING,
    			MissionType.RESCUE_SALVAGE_VEHICLE,
    			MissionType.TRADE,
    			MissionType.TRAVEL_TO_SETTLEMENT
    			};
		return result;
	}
    

	/**
	 * Gets mission description based on a mission type.
	 * 
	 * @param missionType the mission type.
	 * @return the mission description.
	 */
    protected static String getMissionDescription(MissionType missionType) {
    	return missionType.getName();
	}

	/**
	 * Gets the mission type.
	 * @return type.
	 */
    protected String getType() {
		return type;
	}

	/**
	 * Sets the mission type.
	 * @param type the mission type.
	 */
    protected void setType(String type) {
		this.type = type;
	}

	/**
	 * Gets the mission type enum.
	 * @return missionType enum.
	 */
    protected MissionType getMissionType() {
		return missionType;
	}

	/**
	 * Sets the mission type enum.
	 * 
	 * @param missionType the mission type enum.
	 */
    public void setMissionType(MissionType missionType) {
    	this.missionType = missionType;
    }
    
	/**
	 * Gets the mission description.
	 * 
	 * @return description.
	 */
    protected String getDescription() {
		return description;
	}

	/**
	 * Gets the mission designation.
	 * 
	 * @return designation.
	 */
    protected String getDesignation() {
		return designation;
	}

	/**
	 * Sets the mission description.
	 * 
	 * @param description the mission description.
	 */
    protected void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets the starting settlement.
	 * 
	 * @return settlement.
	 */
    protected Settlement getStartingSettlement() {
		return startingSettlement;
	}

	/**
	 * Sets the starting settlement.
	 * 
	 * @param startingSettlement starting settlement.
	 */
    protected void setStartingSettlement(Settlement startingSettlement) {
		this.startingSettlement = startingSettlement;
	}

	/**
	 * Gets the rover.
	 * 
	 * @return rover.
	 */
    protected Rover getRover() {
		return rover;
	}

	/**
	 * Sets the rover.
	 * 
	 * @param rover the rover.
	 */
    protected void setRover(Rover rover) {
		this.rover = rover;
	}

	/**
	 * Gets the drone.
	 * 
	 * @return drone.
	 */
    protected Drone getDrone() {
		return drone;
	}

	/**
	 * Sets the drone.
	 * 
	 * @param drone the drone.
	 */
    protected void setDrone(Drone drone) {
		this.drone = drone;
	}
    
	/**
	 * Gets the mission members.
	 * 
	 * @return the members.
	 */
    protected Collection<Worker> getMixedMembers() {
		return mixedMembers;
	}
    
	/**
	 * Gets both person and robot mission members.
	 * 
	 * @return the members.
	 */
    protected Collection<Worker> getAllMembers() {
    	Collection<Worker> all = mixedMembers;
    	all.addAll(botMembers);
		return all;
	}

    
	/**
	 * Sets the mission members.
	 * 
	 * @param members the members.
	 */
    protected void setBotMembers(Collection<Worker> mm) {
    	this.botMembers = mm;
	}
    
	/**
	 * Sets the mission members.
	 * 
	 * @param members the members.
	 */
    protected void setMixedMembers(Collection<Worker> mm) {
    	this.mixedMembers = mm;
	}
    
//	/**
//	 * Adds the mission members.
//	 * 
//	 * @param members the members.
//	 */
//    protected void addMixedMembers(Collection<Worker> mm) {
//    	this.mixedMembers.addAll(mm);
//	}
    
	/**
	 * Gets the destination settlement.
	 * 
	 * @return destination settlement.
	 */
    protected Settlement getDestinationSettlement() {
		return destinationSettlement;
	}

	/**
	 * Sets the destination settlement.
	 * 
	 * @param destinationSettlement the destination settlement.
	 */
    protected void setDestinationSettlement(Settlement destinationSettlement) {
		this.destinationSettlement = destinationSettlement;
	}

	/**
	 * Gets the rescue rover.
	 * 
	 * @return the rescue rover.
	 */
    protected Rover getRescueRover() {
		return rescueRover;
	}

	/**
	 * Sets the rescue rover.
	 * 
	 * @param rescueRover the rescue rover.
	 */
    protected void setRescueRover(Rover rescueRover) {
		this.rescueRover = rescueRover;
	}

	/**
	 * Gets the ice collection site.
	 * 
	 * @return ice collection site.
	 */
    protected Coordinates getIceCollectionSite() {
		return iceCollectionSite;
	}

	/**
	 * Sets the ice collection site.
	 * 
	 * @param iceCollectionSite the ice collection site.
	 */
    protected void setIceCollectionSite(Coordinates iceCollectionSite) {
		this.iceCollectionSite = iceCollectionSite;
	}

	/**
	 * Gets the regolith collection site.
	 * 
	 * @return regolith collection site.
	 */
    protected Coordinates getRegolithCollectionSite() {
		return regolithCollectionSite;
	}

	/**
	 * Sets the regolith collection site.
	 * 
	 * @param regolithCollectionSite the regolith collection site.
	 */
    protected void setRegolithCollectionSite(Coordinates regolithCollectionSite) {
		this.regolithCollectionSite = regolithCollectionSite;
	}

	/**
	 * Gets the exploration sites.
	 * 
	 * @return exploration sites.
	 */
    protected Coordinates[] getExplorationSites() {
		return explorationSites;
	}

	/**
	 * Sets the exploration sites.
	 * 
	 * @param explorationSites the exploration sites.
	 */
    protected void setExplorationSites(Coordinates[] explorationSites) {
		this.explorationSites = explorationSites;
	}

	/**
	 * Gets the sell goods.
	 * 
	 * @return map of goods and integer amounts.
	 */
    protected Map<Good, Integer> getSellGoods() {
		return sellGoods;
	}

	/**
	 * Sets the sell goods.
	 * 
	 * @param sellGoods map of goods and integer amounts.
	 */
    protected void setSellGoods(Map<Good, Integer> sellGoods) {
		this.sellGoods = sellGoods;
	}

	/**
	 * Gets the buy goods.
	 * 
	 * @return map of goods and integer amounts.
	 */
    protected Map<Good, Integer> getBuyGoods() {
		return buyGoods;
	}

	/**
	 * Sets the buy goods.
	 * 
	 * @param buyGoods map of goods and integer amounts.
	 */
	protected void setBuyGoods(Map<Good, Integer> buyGoods) {
		this.buyGoods = buyGoods;
	}

	/**
	 * Gets the light utility vehicle.
	 * 
	 * @return light utility vehicle
	 */
	protected LightUtilityVehicle getLUV() {
		return luv;
	}

	/**
	 * Sets the light utility vehicle.
	 * 
	 * @param luv the light utility vehicle
	 */
	protected void setLUV(LightUtilityVehicle luv) {
		this.luv = luv;
	}

	/**
	 * Gets the mining site.
	 * 
	 * @return mining site.
	 */
	protected ExploredLocation getMiningSite() {
		return miningSite;
	}

	/**
	 * Sets the mining site.
	 * 
	 * @param miningSite the mining site.
	 */
	protected void setMiningSite(ExploredLocation miningSite) {
		this.miningSite = miningSite;
	}

    /**
     * Gets the salvage settlement.
     * 
     * @return settlement.
     */
	protected Settlement getSalvageSettlement() {
        return salvageSettlement;
    }

    /**
     * Sets the salvage settlement.
     * 
     * @param salvageSettlement the salvage settlement.
     */
	protected void setSalvageSettlement(Settlement salvageSettlement) {
        this.salvageSettlement = salvageSettlement;
    }

    /**
     * Gets the salvage site.
     * 
     * @return salvage site.
     */
	protected ConstructionSite getSalvageSite() {
        return salvageSite;
    }

    /**
     * Sets the salvage site.
     * 
     * @param salvageSite the salvage site.
     */
    protected void setSalvageSite(ConstructionSite salvageSite) {
        this.salvageSite = salvageSite;
    }

    /**
     * Gets the salvage building.
     * 
     * @return salvage building.
     */
    protected Building getSalvageBuilding() {
        return salvageBuilding;
    }

    /**
     * Sets the salvage building.
     * 
     * @param salvageBuilding the salvage building.
     */
    protected void setSalvageBuilding(Building salvageBuilding) {
        this.salvageBuilding = salvageBuilding;
    }

    /**
     * Gets the salvage vehicles.
     * 
     * @return list of ground vehicles.
     */
    protected List<GroundVehicle> getSalvageVehicles() {
        return salvageVehicles;
    }

    /**
     * Sets the salvage vehicles.
     * 
     * @param salvageVehicles list of ground vehicles.
     */
    protected void setSalvageVehicles(List<GroundVehicle> salvageVehicles) {
        this.salvageVehicles = salvageVehicles;
    }

    /**
     * Gets the construction settlement.
     * 
     * @return settlement.
     */
    protected Settlement getConstructionSettlement() {
        return constructionSettlement;
    }

    /**
     * Sets the construction settlement.
     * 
     * @param constructionSettlement the construction settlement.
     */
    protected void setConstructionSettlement(Settlement constructionSettlement) {
        this.constructionSettlement = constructionSettlement;
    }

    /**
     * Gets the construction site.
     * 
     * @return construction site.
     */
    protected ConstructionSite getConstructionSite() {
        return constructionSite;
    }

    /**
     * Sets the construction site.
     * 
     * @param constructionSite the construction site.
     */
    protected void setConstructionSite(ConstructionSite constructionSite) {
        this.constructionSite = constructionSite;
    }

    /**
     * Gets the construction stage info.
     * 
     * @return construction stage info.
     */
    protected ConstructionStageInfo getConstructionStageInfo() {
        return constructionStageInfo;
    }

    /**
     * Sets the construction stage info.
     * 
     * @param constructionStageInfo the construction stage info.
     */
    protected void setConstructionStageInfo(ConstructionStageInfo constructionStageInfo) {
        this.constructionStageInfo = constructionStageInfo;
    }

    /**
     * Gets the construction site X location.
     * 
     * @return X location (meters).
     */
    protected double getConstructionSiteXLocation() {
        return constructionSiteXLoc;
    }

    /**
     * Sets the construction site X location.
     * 
     * @param constructionSiteXLoc X location (meters).
     */
    protected void setConstructionSiteXLocation(double constructionSiteXLoc) {
        this.constructionSiteXLoc = constructionSiteXLoc;
    }

    /**
     * Gets the construction site Y location.
     * 
     * @return Y location (meters).
     */
    protected double getConstructionSiteYLocation() {
        return constructionSiteYLoc;
    }

    /**
     * Sets the construction site Y location.
     * 
     * @param constructionSiteYLoc Y Location (meters).
     */
    protected void setConstructionSiteYLocation(double constructionSiteYLoc) {
        this.constructionSiteYLoc = constructionSiteYLoc;
    }

    /**
     * Gets the construction site facing.
     * 
     * @return the construction site facing (degrees clockwise from North).
     */
    protected double getConstructionSiteFacing() {
        return constructionSiteFacing;
    }

    /**
     * Sets the construction site facing.
     * 
     * @param constructionSiteFacing facing (degrees clockwise from North).
     */
    protected void setConstructionSiteFacing(double constructionSiteFacing) {
        this.constructionSiteFacing = constructionSiteFacing;
    }

    /**
     * Gets the construction vehicles.
     * 
     * @return list of ground vehicles.
     */
    protected List<GroundVehicle> getConstructionVehicles() {
        return constructionVehicles;
    }

    /**
     * Sets the construction vehicles.
     * 
     * @param constructionVehicles list of ground vehicles.
     */
    protected void setConstructionVehicles(List<GroundVehicle> constructionVehicles) {
        this.constructionVehicles = constructionVehicles;
    }

    /**
     * Gets the field site.
     * 
     * @return field site location.
     */
    protected Coordinates getFieldSite() {
        return fieldSite;
    }

    /**
     * Sets the field site.
     * 
     * @param fieldSite the field site location.
     */
    protected void setFieldSite(Coordinates fieldSite) {
        this.fieldSite = fieldSite;
    }

    /**
     * Gets the lead researcher for the mission.
     * 
     * @return lead researcher.
     */
    protected Person getLeadResearcher() {
        return leadResearcher;
    }

    /**
     * Sets the lead researcher for the mission.
     * 
     * @param leadResearcher the lead researcher.
     */
    protected void setLeadResearcher(Person leadResearcher) {
        this.leadResearcher = leadResearcher;
    }

    /**
     * Gets the scientific study.
     * 
     * @return the scientific study.
     */
    protected ScientificStudy getStudy() {
        return study;
    }

    /**
     * Sets the scientific study.
     * 
     * @param study the scientific study.
     */
    protected void setScientificStudy(ScientificStudy study) {
        this.study = study;
    }

    /**
     * Gets the emergency resources map.
     * 
     * @return map of resources and amounts (kg).
     */
    protected Map<Good, Integer> getEmergencyGoods() {
        return emergencyGoods;
    }

    /**
     * Sets the emergency resources.
     * 
     * @param emergencyGoods map of resources and amounts (kg).
     */
    protected void setEmergencyGoods(Map<Good, Integer> emergencyGoods) {
        this.emergencyGoods = emergencyGoods;
    }

	protected boolean isScientificMission() {
        return missionType == MissionType.AREOLOGY
                || missionType == MissionType.BIOLOGY
                || missionType == MissionType.METEOROLOGY;
	}

	protected boolean isMiningMission() {
		return missionType == MissionType.MINING;
	}

	protected boolean isExplorationMission() {
    	return missionType == MissionType.EXPLORATION;
	}

	protected boolean isTradeMission() {
    	return missionType == MissionType.TRADE;
	}

	protected boolean isDeliveryMission() {
    	return missionType == MissionType.DELIVERY;
	}

	protected boolean isEmergencySupplyMission() {
    	return missionType == MissionType.EMERGENCY_SUPPLY;
	}

	protected boolean isTravelMission() {
    	return missionType == MissionType.TRAVEL_TO_SETTLEMENT;
	}

	protected boolean isConstructionMission() {
    	return missionType == MissionType.CONSTRUCTION;
	}

	protected boolean isBuildingSalvageMission() {
    	return missionType == MissionType.SALVAGE;
	}

	protected boolean requiresFieldSite() {
		return isScientificMission();// || isMiningMission() || isExplorationMission() );
	}

	protected boolean requiresDestinationSettlement() {
		return  ( isDeliveryMission() || isTradeMission() || isEmergencySupplyMission() || isTravelMission() );
	}

	protected boolean isProspectingMission() {
		return  ( missionType == MissionType.COLLECT_REGOLITH
				|| missionType == MissionType.COLLECT_ICE);
	}

	/**
	 * Describes if the mission requires a vehicle for transportation or is executed
	 * on-site.
	 * 
	 * @return true, if a mission takes place outside the starting base
	 */
	protected boolean isRemoteMission() {
        return missionType != MissionType.CONSTRUCTION
                && missionType != MissionType.SALVAGE;
	}

	/**
	 * Missions that require meeting another rover.
	 * 
	 * @return true, when a on-ground rendezvous is needed
	 */
	protected boolean isRescueRendezvousMission() {
		return missionType == MissionType.RESCUE_SALVAGE_VEHICLE;
	}

}
