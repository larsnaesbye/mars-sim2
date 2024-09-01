/*
 * Mars Simulation Project
 * MapMetaData.java
 * @date 2023-08-02
 * @author Barry Evans
 */

package com.mars_sim.core.map;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mars_sim.core.map.common.FileLocator;

public class MapMetaData {
	
    private boolean colourful = true;
    
    /** The selected resolution of the map file. */
	private int res = 0;
    /** The available number of resolution level for this map type. */
	private int numLevel = 1;
	
	private String mapString;
    private String mapType;
    
    private List<String> listOfMaps;
    
    private Map<String, Boolean> locallyAvailableMap = new HashMap<>();
    
    public MapMetaData(String mapString, String mapType, boolean colourful, List<String> array) {

        this.mapString = mapString;
        this.mapType = mapType;
        this.colourful = colourful;
        this.listOfMaps = array;
  
        numLevel = array.size();
      
        checkMapLocalAvailability();
    }

    /**
     * Checks if the maps are available locally.
     */
    public void checkMapLocalAvailability() {

		boolean[] loaded = new boolean[numLevel];
		
		for (int i = 0; i < numLevel; i++) {
			String map = listOfMaps.get(i);
			loaded[i] = FileLocator.isLocallyAvailable(map);
			locallyAvailableMap.put(map, loaded[i]);
		}
		
		// Initially set resolution to level 0, the lowest possible one
		setResolution(0);
    }
    
    public String getMapString() {
        return mapString;
    }

    public String getMapType() {
        return mapType;
    }

    /**
     * Sets if the file is locally available.
     * 
     * @param newValue
     */
    void setLocallyAvailable(boolean newValue) {
        locallyAvailableMap.put(getFile(), newValue);
    }

    /**
     * Sets the resolution of the map file.
     * 
     * @param selected
     */
    public void setResolution(int selected) {
    	res = selected;
    }
    
    /**
     * Gets the resolution of the map file.
     * 
     * @return
     */
    public int getResolution() {
    	return res;
    }
    
    /**
     * Gets the available number of level of resolution.
     * 
     * @return
     */
    public int getNumLevel() {
    	return numLevel;
    }
    
    /**
     * Is the map file locally available.
     * 
     * @param resolution
     * @return
     */
    public boolean isLocallyAvailable(int resolution) {
        return locallyAvailableMap.get(getFile(resolution));
    }

    /**
     * Is the map file available locally (or else remotely) ?
     * 
     * @return
     */
    public boolean isLocallyAvailable() {
        return locallyAvailableMap.get(getFile());
    }
    
    /**
     * Is this a color map ? 
     * 
     * @return
     */
    public boolean isColourful() {
        return colourful;
    }

    /**
     * Gets the filename.
     * 
     * @param res
     * @return
     */
    public String getFile(int res) {
    	numLevel = listOfMaps.size();	
    	if (numLevel > res) {
    		this.res = res;
    		return listOfMaps.get(res);
    	}
    	else {
    		res--;
    		return getFile(res);
    	}
    }
    
    /**
     * Gets the filename.
     * 
     * @return
     */
    public String getFile() {
    	return getFile(res);
    }

	/**
	 * Compares if an object is the same as this unit
	 *
	 * @param obj
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		return this.mapString == ((MapMetaData)obj).getMapString();
	}

	/**
	 * Gets the hash code for this object.
	 *
	 * @return hash code.
	 */
	public int hashCode() {
		int hashCode = mapType.hashCode() * mapString.hashCode();
		return hashCode;
	}
	
	public void destroy() {
		locallyAvailableMap.clear();
		locallyAvailableMap = null;
	}
}
