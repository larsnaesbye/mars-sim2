/*
 * Mars Simulation Project
 * BuildingTemplate.java
 * @date 2022-07-19
 * @author Scott Davis
 */
package com.mars_sim.core.structure.building;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mars_sim.core.map.location.BoundedObject;
import com.mars_sim.core.map.location.LocalPosition;
import com.mars_sim.core.tool.Msg;

/**
 * A building template information.
 */
public class BuildingTemplate implements Serializable, Comparable<BuildingTemplate> {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private int zone;
	
	private String eVAAttachedStreetNum;
	private String streetNum;
	private String buildingType;
	private String buildingName;

	private BoundedObject bounds;

	private List<BuildingConnectionTemplate> connectionList;

	/*
	 * * BuildingTemplate Constructor.
	 */
	public BuildingTemplate(String id, int zone, String buildingType, String buildingName,
			BoundedObject bounds) {

		this.streetNum = id;
		this.zone = zone;
		this.buildingType = buildingType;
		this.buildingName = buildingName;
		this.bounds = bounds;
		connectionList = new ArrayList<>(0);
	}

	/**
	 * Gets the building street num.
	 * 
	 * @return id.
	 */
	public String getID() {
		return streetNum;
	}


	/**
	 * Gets the building zone.
	 * 
	 * @return zone.
	 */
	public int getZone() {
		return zone;
	}
	
	/**
	 * Gets the building type.
	 * 
	 * @return building type.
	 */
	public String getBuildingType() {
		return buildingType;
	}

	/**
	 * Gets the building nickname.
	 * 
	 * @return building nickname.
	 */
	public String getBuildingName() {
		return buildingName;
	}

	/**
	 * Sets the building nickname.
	 * 
	 * @param building nickname.
	 */
	public void setBuildingName(String name) {
		buildingName = name;
	}

	/**
	 * Gets the bounds of the building. Note: value in the individual attributes is -1 if not set in template.
	 * 
	 * @return Physical bounds of the building
	 */
	public BoundedObject getBounds() {
		return bounds;
	}

	/**
	 * Adds a new building connection.
	 * 
	 * @param id        the unique id of the building being connected to.
	 * @param location the location (local to the building) (meters).
	 */
	public void addBuildingConnection(String id, LocalPosition location) {
		BuildingConnectionTemplate template = new BuildingConnectionTemplate(id, location);
		if (!connectionList.contains(template)) {
			connectionList.add(template);
		} else {
			throw new IllegalArgumentException(Msg.getString("BuildingTemplate.error.connectionAlreadyExists")); //$NON-NLS-1$
		}
	}

	/**
	 * Adds a new building connection.
	 * 
	 * @param id        the unique id of the building being connected to.
	 * @param hatchFace the facing of the hatch.
	 */
	public void addBuildingConnection(String id, String hatchFace) {
		BuildingConnectionTemplate template = new BuildingConnectionTemplate(id, hatchFace);
		if (!connectionList.contains(template)) {
			connectionList.add(template);
		} else {
			throw new IllegalArgumentException(Msg.getString("BuildingTemplate.error.connectionAlreadyExists")); //$NON-NLS-1$
		}
	}
	
	public void addEVAAttachedBuildingID(String id) {
		eVAAttachedStreetNum = id;
	}
	
	public String getEVAAttachedBuildingID() {
		return eVAAttachedStreetNum;
	}
	
	/**
	 * Gets a list of all building connection templates.
	 * 
	 * @return list of all building connection templates.
	 */
	public List<BuildingConnectionTemplate> getBuildingConnectionTemplates() {
		return Collections.unmodifiableList(connectionList);
	}

	/**
	 * Compares to another building template.
	 */
	public int compareTo(BuildingTemplate o) {
		String compareId = ((BuildingTemplate) o).streetNum;
		if (this.streetNum.equalsIgnoreCase(compareId)) 
			return 0;
		else
			return -1;
	}
	
	/**
	 * Inner class to represent a building connection template.
	 */
	public class BuildingConnectionTemplate implements Serializable {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		// Data members
		private String id;
		private String hatchFace;
		private LocalPosition location;

		/**
		 * Constructor 1.
		 * 
		 * @param id        the unique id of the building being connected to.
		 * @param xLocation the x axis location (local to the building) (meters).
		 * @param yLocation the y axis location (local to the building) (meters).
		 */
		private BuildingConnectionTemplate(String id, LocalPosition loc) {
			this.id = id;
			this.location = loc;
		}
		
		/**
		 * Constructor 2.
		 * 
		 * @param id        the unique id of the building being connected to.
		 * @param hatchFace the face of the hatch .
		 */
		private BuildingConnectionTemplate(String id, String hatchFace) {
			this.id = id;
			this.hatchFace = hatchFace;
		}


		public String getID() {
			return id;
		}

		public LocalPosition getPosition() {
			return location;
		}

		public String getHatchFace() {
			return hatchFace;
		}
		
		public void setPosition(double x, double y) {
			location = new LocalPosition(x, y);
		}
		
		
		@Override
		public boolean equals(Object otherObject) {
			boolean result = false;

			if (otherObject instanceof BuildingConnectionTemplate) {
				BuildingConnectionTemplate template = (BuildingConnectionTemplate) otherObject;
				if ((id == template.id) 
						&& location.equals(template.location)
						&& hatchFace.equals(template.hatchFace)) {
					result = true;
				}
			}
			return result;
		}
		
		/**
		 * Gets the hash code for this object.
		 *
		 * @return hash code.
		 */
		@Override
		public int hashCode() {
			return super.hashCode();
		}
	}
}
