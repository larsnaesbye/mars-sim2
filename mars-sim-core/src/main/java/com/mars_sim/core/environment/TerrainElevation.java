/*
 * Mars Simulation Project
 * TerrainElevation.java
 * @date 2023-05-09
 * @author Scott Davis
 */

package com.mars_sim.core.environment;

import java.awt.Color;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.mars_sim.core.Simulation;
import com.mars_sim.core.UnitManager;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.map.MapDataUtil;
import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.map.location.Direction;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.tool.RandomUtil;

// Note: the newly surveyed ice deposit spans latitudes from 39 to 49 deg
// within the Utopia Planitia plains, as estimated by SHARAD, an subsurface
// sounding radar ice that penetrate below the surface. SHARAD was mounted
// on the Mars Reconnaissance Orbiter.

// See https://www.jpl.nasa.gov/news/news.php?feature=6680

// See https://github.com/mars-sim/mars-sim/issues/225 on past effort in finding elevation via color shaded maps

/**
 * The TerrainElevation class represents the surface terrain of the virtual
 * Mars. It provides information about elevation and terrain ruggedness and
 * calculate ice collection rate at a location on its vast surface.
 */
public class TerrainElevation implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final SimLogger logger = SimLogger.getLogger(TerrainElevation.class.getName());
	
	private static final double OLYMPUS_MONS_CALDERA_PHI = 1.267990;
	private static final double OLYMPUS_MONS_CALDERA_THETA = 3.949854;
	
	private static final double ASCRAEUS_MONS_PHI = 1.363102D;
	private static final double ASCRAEUS_MONS_THETA = 4.459316D;
	
	private static final double ARSIA_MONS_PHI = 1.411494; 
	private static final double ARSIA_MONS_THETA = 4.158439;
//
	private static final double ELYSIUM_MONS_PHI = 1.138866; 
	private static final double ELYSIUM_MONS_THETA = 2.555808;
//	
	private static final double PAVONIS_MONS_PHI = 1.569704; 
	private static final double PAVONIS_MONS_THETA = 4.305273;
	private static final double HECATES_THOLUS_PHI = 1.015563; 
	private static final double HECATES_THOLUS_THETA = 2.615812;
	private static final double ALBOR_THOLUS_PHI = 1.245184; 
	private static final double ALBOR_THOLUS_THETA = 2.615812;
	
	private static final double NORTH_POLE_PHI = 0; 
	private static final double NORTH_POLE_THETA = 0;
	private static final double SOUTH_POLE_PHI = Math.PI; 
	private static final double SOUTH_POLE_THETA = 0;
	
	public static final double STEP_KM = 2;
	private static final double DEG_TO_RAD = Math.PI/180;
	private static final double RATE = 1;

	private static final String TOPO_MAP_TYPE = "molaColor";
	
	private Set<CollectionSite> sites;
	
	private Map<Coordinates, double[]> terrainProfileMap;
	
	private static MapDataUtil mapDataUtil = MapDataUtil.instance();

	private static UnitManager unitManager;

	
	/**
	 * Constructor.
	 */
	public TerrainElevation() {
		sites = new HashSet<>();
		terrainProfileMap = new HashMap<>();
	}

	/**
	 * Returns terrain steepness angle (in radians) from location by sampling a step distance in given
	 * direction.
	 *
	 * @param currentLocation  the coordinates of the current location
	 * @param currentDirection the current direction (in radians)
	 * @return terrain steepness angle (in radians)
	 */
	public static double determineTerrainSteepness(Coordinates currentLocation, Direction currentDirection) {
		return determineTerrainSteepness(currentLocation, getMEGDRElevation(currentLocation), currentDirection);
	}

	/**
	 * Determines the terrain steepness angle (in radians) from location by sampling a step distance in given
	 * direction and elevation.
	 *
	 * @param currentLocation
	 * @param elevation
	 * @param currentDirection
	 * @return
	 */
	public static double determineTerrainSteepness(Coordinates currentLocation, double elevation, Direction currentDirection) {
		double newY = - 1.5 * currentDirection.getCosDirection();
		double newX = 1.5 * currentDirection.getSinDirection();
		Coordinates sampleLocation = currentLocation.convertRectToSpherical(newX, newY);
		double elevationChange = getAverageElevation(sampleLocation) - elevation;
		// Compute steepness
		return Math.atan(elevationChange / STEP_KM);
	}

	/**
	 * Determines the terrain steepness angle (in radians) from location by sampling a random coordinate set and a step distance in given
	 * direction and elevation.
	 *
	 * @param currentLocation
	 * @param elevation
	 * @param currentDirection
	 * @return
	 */
	public static double determineTerrainSteepnessRandom(Coordinates currentLocation, double elevation, Direction currentDirection) {
		double newY = - RandomUtil.getRandomDouble(1.5) * currentDirection.getCosDirection();
		double newX = RandomUtil.getRandomDouble(1.5) * currentDirection.getSinDirection();
		Coordinates sampleLocation = currentLocation.convertRectToSpherical(newX, newY);
		double elevationChange = getAverageElevation(sampleLocation) - elevation;
		return Math.atan(elevationChange / STEP_KM);
	}



	/**
	 * Gets the terrain profile of a location.
	 *
	 * @param {@link Coordinates} currentLocation
	 * @return an array of two doubles, namely elevation and steepness
	 */
	public double[] getTerrainProfile(Coordinates currentLocation) {
		if (!terrainProfileMap.containsKey(currentLocation)) {

			double steepness = 0;
			double elevation = getAverageElevation(currentLocation);
			for (int i=0 ; i <= 360 ; i++) {
				double rad = i * DEG_TO_RAD;
				steepness += Math.abs(determineTerrainSteepness(currentLocation, elevation, new Direction(rad)));
			}
	
			double[] terrain = {elevation, steepness};
					
			terrainProfileMap.put(currentLocation, terrain);
			
			return terrain;
		}
		
		return terrainProfileMap.get(currentLocation);
	}


	/**
	 * Computes the regolith collection rate of a location.
	 *
	 * @param site
	 * @param currentLocation
	 * @return regolith collection rate
	 */
	public void computeRegolithCollectionRate(CollectionSite site, Coordinates currentLocation) {

		// Get the elevation and terrain gradient factor
		double[] terrainProfile = getTerrainProfile(currentLocation);

		double elevation = terrainProfile[0];
		double steepness = terrainProfile[1];
		double latitude = currentLocation.getLatitudeDouble();

		site.setElevation(elevation);
		site.setSteepness(steepness);

		double rate = RATE;

		// Note: Add seasonal variation for north and south hemisphere
		// Note: The collection rate may be increased by relevant scientific studies

		if (latitude < 60 && latitude > -60) {
			// The steeper the slope, the harder it is to retrieve the deposit
			rate *= RandomUtil.getRandomDouble(10) + (- 0.639 * elevation + 14.2492) / 5D  - Math.abs(steepness) / 10D;
		}

		else if ((latitude >= 60 && latitude < 75)
			|| (latitude <= -60 && latitude > -75)) {
			rate *= RandomUtil.getRandomDouble(5) + Math.abs(elevation) / 20.0  - Math.abs(latitude) / 100.0 - Math.abs(steepness) / 10D;
		}

		else if ((latitude >= 75 && latitude <= 90)
				|| (latitude <= -75 && latitude >= -90)) {
				rate *= Math.abs(elevation) / 50.0  - Math.abs(latitude) / 50.0;
		}

		if (rate > 200)
			rate = 200;

		if (rate < 1)
			rate = 1;

		site.setRegolithCollectionRate(rate);
	}

	/**
	 * Computes the ice collection rate of a location.
	 *
	 * @param site
	 * @param currentLocation
	 * @return ice collection rate
	 */
	public void computeIceCollectionRate(CollectionSite site, Coordinates currentLocation) {

		// Get the elevation and terrain gradient factor
		double[] terrainProfile = getTerrainProfile(currentLocation);

		double elevation = terrainProfile[0];
		double steepness = terrainProfile[1];
		double latitude = currentLocation.getLatitudeDouble();

		site.setElevation(elevation);
		site.setSteepness(steepness);

		double rate = RATE;

		// Note 1: Add seasonal variation for north and south hemisphere
		// Note 2: The collection rate may be increased by relevant scientific studies

		if (latitude < 60 && latitude > -60) {
			// The steeper the slope, the harder it is to retrieve the ice deposit
			rate *= (- 0.639 * elevation + 14.2492) / 20D + Math.abs(steepness) / 10D;
		}

		else if ((latitude >= 60 && latitude < 75)
			|| (latitude <= -60 && latitude > -75)) {
			rate *= RandomUtil.getRandomDouble(5) + Math.abs(elevation) / 2.0 + Math.abs(latitude) / 75.0 - Math.abs(steepness) / 10D;
		}

		else if ((latitude >= 75 && latitude <= 90)
				|| (latitude <= -75 && latitude >= -90)) {
				rate *= RandomUtil.getRandomDouble(10) + Math.abs(elevation) + Math.abs(latitude) / 75.0;
		}

		if (rate > 200)
			rate = 200;

		if (rate < 1)
			rate = 1;

		site.setIceCollectionRate(rate);
	}

	/**
	 * Obtains the ice collection rate of a location.
	 *
	 * @param loc
	 * @return the collection rate
	 */
	public double obtainIceCollectionRate(Coordinates loc) {
		CollectionSite site = getCollectionSite(loc);
		
		if (site.getIceCollectionRate() == -1)
			computeIceCollectionRate(site, loc);
		
		return site.getIceCollectionRate();
	}

	/**
	 * Obtains the regolith collection rate of a location.
	 *
	 * @param loc
	 * @return the collection rate
	 */
	public double obtainRegolithCollectionRate(Coordinates loc) {
		CollectionSite site = getCollectionSite(loc);
		
		if (site.getRegolithCollectionRate() == -1)
			computeRegolithCollectionRate(site, loc);
		
		return site.getRegolithCollectionRate();
	}

//	/**
//	 * Compute the RGB Topo map based Elevation.
//	 * 
//	 * @param location
//	 * @return
//	 */
//	public static int getRGBIntElevation(Coordinates location) {	
//		// Find hue and saturation color components at location.
//		int color = mapDataUtil.getMapData("topo").getRGBColorInt(location.getPhi(), location.getTheta());
//
//		// The peak of Olympus Mons is 21,229 meters (69,649 feet) above the Mars areoid (a reference datum similar to Earth's sea level). 
//		// The lowest point is within the Hellas Impact Crater (marked by a flag with the letter "L"). 
//	    // The lowest point in the Hellas Impact Crater is 8,200 meters (26,902 feet) below the Mars areoid. 
//		
//		double height = (color - 9000) / 10_000;
//
//		return (int)Math.round(height);
//	}
	
	/**
	 * Returns the elevation in km at the given location, based on MEGDR's dataset.
	 *
	 * @param location the location in question
	 * @return the elevation at the location (in km)
	 */
	public static double getColorElevation(Coordinates location) {	
		
//		if (unitManager == null)
//			unitManager = Simulation.instance().getUnitManager();
//			
//		// Check if this location is a settlement
//		Settlement s = unitManager.findSettlement(location);
				
		double height = 0;
//		if (s != null) {
//			height = s.getElevation();
//		}
//		else {
			height = getColorElevation(location.getPhi(), location.getTheta());	
//		}
			
		return height;
	}
	
	/**
	 * Compute the HSB color topo map based Elevation.
	 * 
	 * @param phi
	 * @param theta
	 * @return elevation in km
	 */
	public static double getColorElevation(double phi, double theta) {	
		// Find hue and saturation color components at location.
		Color color = new Color(mapDataUtil.loadMapData(TOPO_MAP_TYPE).getRGBColorInt(phi, theta));
		int red = color.getRed();
		int green = color.getGreen();
		int blue = color.getBlue();
		
		float[] hsb = getHSB(red, green, blue);
		
		float hue = hsb[0];
		float sat = hsb[1];
		float bri = hsb[2];
		
		// height in meters
		double height = 0;
		if (hue > .5 && hue < .51) {
			if (sat > .14 && sat < .3 && bri > .9 && bri <= 1.0) {
				// For height between 20 and 21 km 
				height = sat * 6980;
				// Note: the factor 6980 was derived from the lookup table
				// See https://github.com/mars-sim/mars-sim/issues/225#issuecomment-535167986
			}
			else {
				// For height between h = -4 km and - 3 km 
				height = 1000 * (-7.2993 * hue - 26.39062);
			}
		}
		else {
			// Not done yet in considering exceptions
			 if ((hue < .792) && (hue > .033)) 
				 height = (-13801.99 * hue) + 2500D;
		     else 
		    	 height = (-21527.78 * sat) + 19375 + 2500D;
		}
		
		height = height / 1000;
		
        // Patch elevation problems at certain locations.
//		height = patchElevation(height, phi, theta);
		
		return height;
	}
	
    /**
     * Patches elevation errors around mountain tops.
     * @param elevation the original elevation for the location.
     * @param location the coordinates
     * @return the patched elevation for the location
     */
    private static double patchElevation(double elevation, double phi, double theta) {
    	double result = elevation;
		// Patch errors at Olympus Mons caldera.
    	
		// Patch the smallest cauldera at the center
		if (Math.abs(theta - OLYMPUS_MONS_CALDERA_THETA) < .0176
			 && Math.abs(phi - OLYMPUS_MONS_CALDERA_PHI) < .0174) {
				result = 19; 
		}
		
		// Patch the larger white cauldera 
		else if (Math.abs(theta - OLYMPUS_MONS_CALDERA_THETA) < .0796
			&& Math.abs(phi - OLYMPUS_MONS_CALDERA_PHI) < .0796) {

			if (elevation > 19 && elevation < 21.2870)
				result = elevation;
			else
				result = 21.287D;
		}
		
		// Patch the red base cauldera 
		else if (Math.abs(theta - OLYMPUS_MONS_CALDERA_THETA) < .1731
			&& Math.abs(phi - OLYMPUS_MONS_CALDERA_PHI) < .1731) {

			if (elevation < 19 && elevation > 3)
				result = elevation;
			else
				result = 3;
		}
		
    	// Patch errors at Ascraeus Mons.
		if (Math.abs(theta - ASCRAEUS_MONS_THETA) < .02D) {
			if (Math.abs(phi - ASCRAEUS_MONS_PHI) < .02D) {
				if (elevation < 3D) result = 20D;
			}
		}
    	
		else if (Math.abs(theta - ARSIA_MONS_THETA) < .04D) {
			if (Math.abs(phi - ARSIA_MONS_PHI) < .04D) {
				if (elevation < 3D)
					result = 17.781;
			}
		}
		
		else if (Math.abs(theta - ELYSIUM_MONS_THETA) < .04D) {
			if (Math.abs(phi - ELYSIUM_MONS_PHI) < .04D) {
				if (elevation < 3D)
					result = 14.127;
			}
		}
		
		else if (Math.abs(theta - PAVONIS_MONS_THETA) < .04D) {
			if (Math.abs(phi - PAVONIS_MONS_PHI) < .04D) {
				if (elevation < 3D)
					result = 14.057;
			}
		}
		
		else if (Math.abs(theta - HECATES_THOLUS_THETA) < .04D) {
			if (Math.abs(phi - HECATES_THOLUS_PHI) < .04D) {
//				if (elevation < 2.5D)
					result = 4.853;
			}
		}
		
		// Patch errors at Ascraeus Mons.
		else if (Math.abs(theta - ALBOR_THOLUS_THETA) < .04D) {
			if (Math.abs(phi - ALBOR_THOLUS_PHI) < .04D) {
//				if (elevation < 2D)
					result = 3.925;
			}
		}
		
//		// Patch errors at the north pole.
//		else if (Math.abs(location.getTheta() - NORTH_POLE_THETA) < .2D) {
//			if (Math.abs(location.getPhi() - NORTH_POLE_PHI) < .04D) {
////				if (elevation < 2D)
//					result = 1.015;
//			}
//		}
//		
//		// Patch errors at the south pole.
//		else if (Math.abs(location.getTheta() - SOUTH_POLE_THETA) < .04D) {
//			if (Math.abs(location.getPhi() - SOUTH_POLE_PHI) < .04D) {
////				if (elevation < 2D)
//					result = .783;
//			}
//		}
          
    	return result;
    }
    
	public int getHue(int red, int green, int blue) {

	    float min = Math.min(Math.min(red, green), blue);
	    float max = Math.max(Math.max(red, green), blue);

	    if (min == max) {
	        return 0;
	    }

	    float hue = 0f;
	    if (max == red) {
	        hue = (green - blue) / (max - min);

	    } else if (max == green) {
	        hue = 2f + (blue - red) / (max - min);

	    } else {
	        hue = 4f + (red - green) / (max - min);
	    }

	    hue = hue * 60;
	    if (hue < 0) hue = hue + 360;

	    return Math.round(hue);
	}
	
    /**
     * Returns the HSB array.
     * 
     * @param rgb
     * @return
     */
    public static float[] getHSB(int red, int green, int blue) {
		float[] hsb = Color.RGBtoHSB(red, green, blue, null);

		// Reference: 
		// 1. https://kdoore.gitbook.io/cs1335-java-and-processing/getting-started/hsb_color_mode
		// 2. https://www.learnui.design/blog/the-hsb-color-system-practicioners-primer.html
		
//		String s1 = String.format("HSB: %5.3f, %5.3f, %5.3f   RGB: %3d, %3d, %3d",
//				Math.round(hsb[0] * 1000.0)/1000.0,
//				Math.round(hsb[1] * 1000.0)/1000.0,
//				Math.round(hsb[2] * 1000.0)/1000.0,
//				red, green, blue);
//		System.out.println(s1);

		return hsb;
	}
	
    /** 
     * Returns the average elevation using both the topo map and MOLA data set.
     * 
     *  @return elevation in km.
     */
    public static double getAverageElevation(Coordinates location) {
    	return getMEGDRElevation(location);
    }

	/**
	 * Returns the elevation in km at the given location, based on MEGDR's dataset.
	 *
	 * @param location the location in question
	 * @return the elevation at the location (in km)
	 */
	public static double getMEGDRElevation(Coordinates location) {	
		
		if (unitManager == null)
			unitManager = Simulation.instance().getUnitManager();
			
		// Check if this location is a settlement
		Settlement s = unitManager.findSettlement(location);
				
		double MOLAHeight = 0;
		if (s != null) {
			MOLAHeight = s.getElevation();
		}
		else {
			MOLAHeight = getMOLAElevation(location.getPhi(), location.getTheta());	
		}
			
		return MOLAHeight;
	}

	/**
	 * Returns the elevation in km at the given location, based on MOLA's MEDGR dataset.
	 *
	 * @param phi
	 * @param theta
	 * @return the elevation at the location (in km)
	 */
	public static double getMOLAElevation(double phi, double theta) {
		return mapDataUtil.getMapDataFactory().getElevation(phi, theta)/1000.0;
	}

	
	public Set<CollectionSite> getCollectionSites() {
		return sites;
	}

	public void addCollectionSite(CollectionSite site) {
		sites.add(site);
	}

	public synchronized CollectionSite getCollectionSite(Coordinates newLocation) {
		// Create a shallow copy of sites to avoid ConcurrentModificationException
		for (CollectionSite s:  sites) {
			if (s.getLocation().equals(newLocation)) {
				return s;
			}
		}
		CollectionSite site = new CollectionSite(newLocation);
		addCollectionSite(site);
		return site;
	}

	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		//nothing
	}
}
