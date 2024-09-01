/*
 * Mars Simulation Project
 * MineralMapLayer.java
 * @date 2023-06-20
 * @author Scott Davis
 */

package com.mars_sim.ui.swing.tool.map;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.mars_sim.core.Simulation;
import com.mars_sim.core.environment.MineralMap;
import com.mars_sim.core.map.Map;
import com.mars_sim.core.map.MapLayer;
import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.tool.SimulationConstants;

/**
 * A map layer showing mineral concentrations.
 */
public class MineralMapLayer implements MapLayer, SimulationConstants {

	// Domain members
	private boolean updateLayer;

	private int numMineralsCache;

	private double rhoCache;
	
	private double centerX = Map.HALF_MAP_BOX;
	private double centerY = Map.HALF_MAP_BOX;
	
	private String mapTypeCache;
	
	private int[] mineralArrayCache;

	private Component displayComponent;
	
	private BufferedImage mineralImage;

	private Coordinates mapCenterCache;
	
	private MineralMap mineralMap;
	
	private SortedMap<String, Color> mineralColorMap;

	private Set<String> mineralsDisplaySet = new HashSet<>();
	
	/**
	 * Constructor
	 * 
	 * @param displayComponent the display component.
	 */
	public MineralMapLayer(Component displayComponent) {
		mineralMap = Simulation.instance().getSurfaceFeatures().getMineralMap();
		this.displayComponent = displayComponent;
	
		mineralColorMap = getMineralColors();
	}
	
	/**
	 * Displays the layer on the map image.
	 * 
	 * @param mapCenter the location of the center of the map.
	 * @param baseMap   the type of map.
	 * @param g         graphics context of the map display.
	 */
	public void displayLayer(Coordinates mapCenter, Map baseMap, Graphics g) {
		
		if (mineralsDisplaySet.isEmpty()) {
			return;
		}

		boolean isChanging = ((MapPanel)displayComponent).isChanging();
		
		double rho = baseMap.getRho();
		
		if (isChanging) {
			return;	
		}

		Graphics2D g2d = (Graphics2D) g;
		
		String mapType = baseMap.getMapMetaData().getMapString();
		
		int numMinerals = mineralsDisplaySet.size();

		if (mapCenterCache == null || !mapCenter.equals(mapCenterCache) || !mapType.equals(mapTypeCache) 
				|| updateLayer || rhoCache != rho || numMineralsCache != numMinerals) {
			
			Point2D point = null;
			int deltaX = 0;
			int deltaY = 0;
			
			// if rho has changed, it's not meaningful to calculate deltaX and deltaY 
			// since the scale is no longer the same
			if (mapCenterCache != null) {
				point = Coordinates.computeDeltaPixels(mapCenterCache, mapCenter, Map.MAP_BOX_HEIGHT / 2.0);
				deltaX = (int)point.getX();
				deltaY = (int)point.getY();
			}
			
			mapTypeCache = mapType;
			updateLayer = false;
			
			int[] newMineralArray = new int[Map.MAP_BOX_WIDTH * Map.MAP_BOX_HEIGHT];

			double mag = baseMap.getMagnification();
	
			boolean hasMinerals = false;
			
//			boolean redo = false;
			
			for (int x = 0; x < Map.MAP_BOX_WIDTH; x = x + 2) {
				
				for (int y = 0; y < Map.MAP_BOX_HEIGHT; y = y + 2) {
						
					// Do NOT delete the following line of codes
//					if (mapCenterCache == null || rho != rhoCache || numMineralsCache != numMinerals) {
					
						java.util.Map<String, Double> mineralConcentrations = 
							mineralMap.getSomeMineralConcentrations(
										mineralsDisplaySet, 
										mapCenter.convertRectToSpherical(x - centerX, y - centerY, rho), 
										mag);
						hasMinerals = true;
//					}
					
					// Do NOT delete the following blocks of codes						
					// Would save a lot of CPU util if one is able to put mineralArrayCache to work	
						
//					else {
//						
//						if (mapCenterCache != null && mineralArrayCache != null && rho == rhoCache && (deltaX > 0 || deltaY > 0)) {
//
//							if (deltaX <= 0 && deltaY <= 0 && x >= deltaX && y >= deltaY) {
//								logger.info(10_000L, "case 1");
//								int i = x - deltaX;
//								int j = y - deltaY;
////								int index = (x - deltaX) + (y * Map.MAP_BOX_WIDTH - deltaY);
//								// Shift the portion of the old x and y values from the old mineralConcentrationArray
//								newMineralArray[x] = mineralArrayCache[i + j];		
//								hasMinerals = true;
//							}
//							else if (deltaX >= 0 && deltaY >= 0 && x <= Map.MAP_BOX_WIDTH - deltaX && y <= Map.MAP_BOX_HEIGHT - deltaY) {
//								logger.info(10_000L, "case 2");
//								int i = x;
//								int j = y;
////								int index = x + (y * Map.MAP_BOX_WIDTH);
//								// Shift the portion of the old x and y values from the old mineralConcentrationArray
//								newMineralArray[x] = mineralArrayCache[i + j];		
//								hasMinerals = true;
//							}
//							else if (deltaX >= 0 && deltaY <= 0 && x <= Map.MAP_BOX_WIDTH - deltaX && y >= deltaY) {
//								logger.info(10_000L, "case 3");
//								int i = x;
//								int j = y - deltaY;
////								int index = x + (y * Map.MAP_BOX_WIDTH - deltaY);
//								// Shift the portion of the old x and y values from the old mineralConcentrationArray
//								newMineralArray[x] = mineralArrayCache[i + j];	
//								hasMinerals = true;
//							}
//							else if (deltaX <= 0 && deltaY >= 0 && x >= deltaX && y <= Map.MAP_BOX_HEIGHT - deltaY) {
//								logger.info(10_000L, "case 4");
//								int i = x - deltaX;
//								int j = y;
////								int index = (x - deltaX) + (y * Map.MAP_BOX_WIDTH);
//								// Shift the portion of the old x and y values from the old mineralConcentrationArray
//								newMineralArray[x] = mineralArrayCache[i + j];		
//								hasMinerals = true;
//							}
//							else {
//								mineralConcentrations = mineralMap
//									.getSomeMineralConcentrations(mineralsDisplaySet, mapCenter.convertRectToSpherical(x - centerX, y - centerY, rho), mag);
//							}
//						}
//						else {
//							mineralConcentrations = mineralMap
//								.getSomeMineralConcentrations(mineralsDisplaySet, mapCenter.convertRectToSpherical(x - centerX, y - centerY, rho), mag);
//						}
//					}
						
					if (mineralConcentrations != null && !mineralConcentrations.isEmpty()) {
		
						Iterator<String> i = mineralConcentrations.keySet().iterator();
						while (i.hasNext()) {
							String mineralType = i.next();
							if (isMineralDisplayed(mineralType)) {
	//							logger.info(mineralType + " is being drawn.");
								double concentration = mineralConcentrations.get(mineralType);
								if (concentration <= 0) {
									continue;
								}
								Color baseColor = mineralColorMap.get(mineralType);
								int index = x + (y * Map.MAP_BOX_WIDTH);
								addColorToMineralConcentrationArray(index, baseColor, concentration, newMineralArray);
								addColorToMineralConcentrationArray((index + 1), baseColor, concentration, newMineralArray);
								if (y < Map.MAP_BOX_HEIGHT - 1) {
									int indexNextLine = x + ((y + 1) * Map.MAP_BOX_WIDTH);
									addColorToMineralConcentrationArray(indexNextLine, baseColor, concentration, newMineralArray);
									addColorToMineralConcentrationArray((indexNextLine + 1), baseColor,
											concentration, newMineralArray);
								}
							}
						}
						
						hasMinerals = true;
					}
				}
			}
			
			mapCenterCache = mapCenter;
			
			if (hasMinerals)
				mineralArrayCache = newMineralArray;
			
			numMineralsCache = numMinerals;

			rhoCache = rho;
			
			// Create a new buffered image to draw the map on.
			mineralImage = new BufferedImage(Map.MAP_BOX_WIDTH, Map.MAP_BOX_HEIGHT, 
	 				BufferedImage.TYPE_INT_ARGB);
	 		
	 		// Create new map image.
			mineralImage.setRGB(0, 0, Map.MAP_BOX_WIDTH, Map.MAP_BOX_HEIGHT, newMineralArray, 0, Map.MAP_BOX_WIDTH);
	
//			// Create mineral concentration image
//			mineralImage = displayComponent.createImage(new MemoryImageSource(Map.MAP_BOX_WIDTH,
//					Map.MAP_BOX_HEIGHT, mineralConcentrationArray, 0, Map.MAP_BOX_WIDTH));
//
//			MediaTracker mt = new MediaTracker(displayComponent);
//			mt.addImage(mineralImage, 0);
//			try {
//				mt.waitForID(0);
//			} catch (InterruptedException e) {
//				logger.log(Level.SEVERE, "MineralMapLayer interrupted: " + e);
//				// Restore interrupted state
//			    Thread.currentThread().interrupt();
//			}

		}
		
		// Draw the mineral concentration image
		g2d.drawImage(mineralImage, 0, 0, displayComponent);
			
		// Do not call dispose
//		g2d.dispose();
	}

	/**
	 * Adds a color to the mineral concentration array.
	 * 
	 * @param index         the index of the pixel in the array.
	 * @param color         the mineral color.
	 * @param concentration the amount of concentration (0% - 100.0%).
	 */
	private void addColorToMineralConcentrationArray(int index, Color color, double concentration, int[] newMineralArray) {
		int concentrationInt = (int) (255 * (concentration / 100D));
		int concentrationColor = (concentrationInt << 24) | (color.getRGB() & 0x00FFFFFF);
		int currentColor = newMineralArray[index];
		newMineralArray[index] = currentColor | concentrationColor;
	}

	/**
	 * Gets a map of all mineral type names and their display colors.
	 * 
	 * @return map of names and colors.
	 */
	public SortedMap<String, Color> getMineralColors() {
		
		if (mineralColorMap == null || mineralColorMap.isEmpty()) {
			String[] mineralNames = mineralMap.getMineralTypeNames();
			int num = mineralNames.length;
			SortedMap<String, Color> map = new TreeMap<>();
			for (int x = 0; x < num ; x++) {
				String mineralTypeName = mineralMap.getMineralTypeNames()[x];
				// Determine color of a mineral
				// See https://stackoverflow.com/questions/44326765/color-mapping-for-specific-range
//				float value = 1f * x / num; //this is your value between 0 and 1
//				float minHue = 255f/255; // 300f corresponds to Magenta // 120f corresponds to Green
//				float maxHue = 0; //corresponds to red
//				float hue = value * maxHue + (1 - value) * minHue; // ((float) x + 1 / (float) mineralNames.length)
//				int mineralColor = Color.HSBtoRGB(hue, .9F, .9F);
				
				String rgbString = mineralMap.getColorString(mineralTypeName);
						
				Color rgbColor = Color.decode(rgbString);
				
				map.put(mineralTypeName, rgbColor);
			}
			
			mineralColorMap = map;
			return map;
		}
		return mineralColorMap;
	}

	
	/**
	 * Updates the set of minerals to be display.
	 * 
	 * @param availableMinerals
	 * @return
	 */
	private Set<String> updateMineralDisplay(Set<String> availableMinerals) {

		Set<String> intersection = new HashSet<>();
		availableMinerals.forEach((i) -> {
			if (mineralsDisplaySet.contains(i))
				intersection.add(i);
		});
		
		return intersection;
	}


	/**
	 * Checks if a mineral type is displayed on the map.
	 * 
	 * @param mineralType the mineral type to display.
	 * @return true if displayed.
	 */
	public boolean isMineralDisplayed(String mineralType) {
		if (mineralsDisplaySet.contains(mineralType))
			return true;
		else
			return false;
	}

	/**
	 * Sets a mineral type to be displayed on the map or not.
	 * 
	 * @param mineralType the mineral type to display.
	 * @param displayed   true if displayed, false if not.
	 */
	public void setMineralDisplayed(String mineralType, boolean displayed) {
		if (mineralsDisplaySet.isEmpty()) {
			if (displayed) {
				mineralsDisplaySet.add(mineralType);	
				updateLayer = true;
			}
		}
		else {
			if (displayed) {
				mineralsDisplaySet.add(mineralType);	
				updateLayer = true;
			}
			else {
				if (mineralsDisplaySet.contains(mineralType)) {
					mineralsDisplaySet.remove(mineralType);
					updateLayer = true;
				}
			}
		}
	}
	
	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {

		displayComponent = null;
		mineralImage = null;
		mapCenterCache = null;
		mineralMap = null;
		mineralsDisplaySet.clear();
		mineralsDisplaySet = null;
	}
}
