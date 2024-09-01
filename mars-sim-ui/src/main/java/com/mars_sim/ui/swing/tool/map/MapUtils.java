/*
 * Mars Simulation Project
 * MapUtils.java
 * @date 2023-04-28
 * @author Scott Davis
 */

package com.mars_sim.ui.swing.tool.map;

import com.mars_sim.core.map.Map;
import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.map.location.IntPoint;

/**
 * Static class for map utilities.
 */
public class MapUtils {

	/**
	 * Private constructor for utility class.
	 */
	private MapUtils() {
		// nothing
	}

	/**
	 * Gets a coordinate x, y position on the map image.
	 * 
	 * @param coords location of unit
	 * @param baseMap the type of map.
	 * @return display point on map
	 */
	public static IntPoint getRectPosition(Coordinates coords, Coordinates mapCenter, Map baseMap) {
		int halfMap = baseMap.getPixelHeight()/2;
		return Coordinates.findRectPosition(coords, mapCenter, baseMap.getRho(),
											halfMap, halfMap - (MapPanel.MAP_BOX_HEIGHT/2));
	}

	/**
	 * Gets the distance in terms of the number of pixels.
	 * 
	 * @param distance
	 * @param mapType
	 * @return
	 */
	public static int getPixelDistance(double distance, Map baseMap) {
		return (int) Math.round(distance / Coordinates.MARS_CIRCUMFERENCE * baseMap.getPixelWidth());
	}
}
