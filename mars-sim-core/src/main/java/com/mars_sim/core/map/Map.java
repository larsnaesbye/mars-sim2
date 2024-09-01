/*
 * Mars Simulation Project
 * Map.java
 * @date 2022-08-02
 * @author Greg Whelan
 */

package com.mars_sim.core.map;

import java.awt.Image;

import com.mars_sim.core.map.location.Coordinates;

/**
 * The Map interface represents a map usable by the CannedMarsMap.
 */
public interface Map {

	/** The display box map height (for scrolling) */
	public static final int MAP_BOX_HEIGHT = 512;
	/** The display box map width (for scrolling) */
	public static final int MAP_BOX_WIDTH = MAP_BOX_HEIGHT;
	/** Map display width in pixels. */
//	public static final int MAP_VIS_WIDTH = MAP_BOX_WIDTH;
	/** Map display height in pixels. */
//	public static final int MAP_VIS_HEIGHT = MAP_BOX_HEIGHT;
	/** Half of the display box map height. */
	public static final int HALF_MAP_BOX = (int) (0.5 * MAP_BOX_HEIGHT);

	public static final double TWO_PI = Math.PI * 2D;
	
	/**
	 * Creates a 2D map at a given center point.
	 * 
	 * @param newCenter 	The new center location
	 * @param rho 		The new map rho
	 * @throws Exception if error in drawing map.
	 */
	public void drawMap(Coordinates newCenter, double rho);

	/**
	 * Checks if a requested map is complete.
	 * 
	 * @return true if requested map is complete
	 */
	public boolean isImageDone();

	/**
	 * Gets the constructed map image.
	 * 
	 * @return constructed map image
	 */
	public Image getMapImage();

	/**
	 * Gets the rho of the Mars surface map (height pixels divided by pi).
	 * 
	 * @return
	 */
	public double getRho();
	
	/**
	 * sets the rho of the Mars surface map (height pixels divided by pi).
	 * 
	 * @return
	 */
	public void setRho(double rho);

	/**
     * Gets the magnification of the Mars surface map.
     * 
     * @return
     */
    public double getMagnification();
    
	/**
     * Gets the half angle of the Mars surface map.
     * 
     * @return
     */
    public double getHalfAngle();
    
	/**
	 * Gets the name type of this map.
	 * 
	 * @return
	 */
	public MapMetaData getMapMetaData();

	/**
	 * Gets the height of this map in pixels.
	 * 
	 * @return
	 */
    public int getPixelHeight();

	/**
	 * Gets the width of this map in pixels.
	 * 
	 * @return
	 */
    public int getPixelWidth();
}
