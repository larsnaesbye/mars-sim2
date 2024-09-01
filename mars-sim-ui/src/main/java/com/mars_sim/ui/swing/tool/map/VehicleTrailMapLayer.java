/*
 * Mars Simulation Project
 * VehicleTrailMapLayer.java
 * @date 2022-07-31
 * @author Scott Davis
 */

package com.mars_sim.ui.swing.tool.map;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Iterator;

import com.mars_sim.core.map.Map;
import com.mars_sim.core.map.MapLayer;
import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.map.location.IntPoint;
import com.mars_sim.core.tool.SimulationConstants;
import com.mars_sim.core.vehicle.Vehicle;

/**
 * The VehicleTrailMapLayer is a graphics layer to display vehicle trails.
 */
public class VehicleTrailMapLayer implements MapLayer, SimulationConstants {

	// Data members
	private Vehicle singleVehicle;

	/**
	 * Sets the single vehicle trail to display. Set to null if display all vehicle
	 * trails.
	 * 
	 * @param singleVehicle the vehicle to display trail.
	 */
	public void setSingleVehicle(Vehicle singleVehicle) {
		this.singleVehicle = singleVehicle;
	}

	/**
	 * Displays the layer on the map image.
	 * 
	 * @param mapCenter the location of the center of the map.
	 * @param baseMap   the type of map.
	 * @param g         graphics context of the map display.
	 */
	@Override
	public void displayLayer(Coordinates mapCenter, Map baseMap, Graphics g) {

		// Set trail color
		g.setColor((baseMap.getMapMetaData().isColourful() ? Color.BLACK : new Color(0, 96, 0)));

		// Draw trail
		if (singleVehicle != null)
			displayTrail(singleVehicle, mapCenter, baseMap, g);
		else {
			Iterator<Vehicle> i = unitManager.getVehicles().iterator();
			while (i.hasNext())
				displayTrail(i.next(), mapCenter, baseMap, g);
		}
	}

	/**
	 * Displays the trail behind a vehicle.
	 * 
	 * @param vehicle   the vehicle to display.
	 * @param mapCenter the location of the center of the map.
	 * @param baseMap   the type of map.
	 * @param g         the graphics context.
	 */
	private void displayTrail(Vehicle vehicle, Coordinates mapCenter, Map baseMap, Graphics g) {

		// Get map angle.
		double angle = baseMap.getHalfAngle();

		// Draw trail.
		IntPoint oldpt = null;
		Iterator<Coordinates> j = (new ArrayList<>(vehicle.getTrail())).iterator();
		while (j.hasNext()) {
			Coordinates c = j.next();
			if (c != null
				&& mapCenter.getAngle(c) < angle) {
					IntPoint pt = MapUtils.getRectPosition(c, mapCenter, baseMap);
					if (oldpt == null)
						g.drawRect(pt.getiX(), pt.getiY(), 1, 1);
					else if (!pt.equals(oldpt))
						g.drawLine(oldpt.getiX(), oldpt.getiY(), pt.getiX(), pt.getiY());
					oldpt = pt;
			}
		}
	}
}
