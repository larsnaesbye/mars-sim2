/*
 * Mars Simulation Project
 * MapDataFactory.java
 * @date 2023-07-26
 * @author Scott Davis
 */
 package com.mars_sim.core.map;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mars_sim.core.map.megdr.MEGDRMapArray;
import com.mars_sim.core.map.megdr.MEGDRMapDirect;
import com.mars_sim.core.map.megdr.MEGDRMapMemory;
import com.mars_sim.core.map.megdr.MEGDRMapReader;
import com.mars_sim.core.tool.RandomUtil;

/**	
  * A factory for map data.
  */
 public class MapDataFactory {

	/**
	 * These are package friendly for Unit Test
	 */
	static final String ARRAY_READER = "array";
	static final String DIRECT_READER = "direct";
	static final String MEMORY_READER = "memory";
	
	static final String MAPS_FOLDER = "/maps/";
	
	static final String ELEVATION_FOLDER = "/elevation/";
	
	private static final String SEPARATOR = ",";
	
	private static Logger logger = Logger.getLogger(MapDataFactory.class.getName());

	private static final double PI = Math.PI;
 	private static final double DEG_PER_RADIAN = 180/Math.PI;
 	
	// The map properties MUST contain at least this map
	public static final String DEFAULT_MAP_TYPE = "vikingMDIM";
	
	private static final String MAP_PROPERTIES = "/mapdata.properties";

	private static final String ELEVATION_PROP = "elevation";

	private Map<String, MapMetaData> metaDataMap = new HashMap<>();

	private MapData mapDataCache;
	
	private MapMetaData mapMetaDataCache;
			
	private MEGDRMapReader reader;

 	/**
 	 * Constructor.
 	 */
 	MapDataFactory() {
 		
		String megdrSpec = MEMORY_READER + SEPARATOR + MEGDRMapReader.DEFAULT_MEGDR_FILE;
		
 		Properties mapProps = new Properties();
		try (InputStream propsStream = MapDataFactory.class.getResourceAsStream(MAP_PROPERTIES)) {
			mapProps.load(propsStream);

			for(String mapString : mapProps.stringPropertyNames()) {
				if (ELEVATION_PROP.equals(mapString)) {
					megdrSpec = mapProps.getProperty(ELEVATION_PROP);
				}
				else {		
					// Split the details into the parts
					String[] array = mapProps.getProperty(mapString).split(SEPARATOR);
					String mapType = array[0];
					boolean isColour = Boolean.parseBoolean(array[1].replaceAll(" ", ""));
								
					int size = array.length;
					List<String> mapList = new ArrayList<>();
					for (int i = 0; i < size; i++) {
						// Remove the element at index 0 and 1
						if (i > 1)
							mapList.add(array[i].replaceAll(" ", ""));
					}

//					Kept for debugging: System.out.println(array[0] + " - size: " + array.length + "  " + mapList + " - size: " + mapList.size());
//					Kept for debugging: System.out.println("mapString: " + mapString + ".  mapType: " + mapType + ".  isColour: " + isColour);
					
					metaDataMap.put(mapString, new MapMetaData(mapString, mapType, isColour, mapList));
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot load " + MAP_PROPERTIES, e);
		}

		if (!metaDataMap.containsKey(DEFAULT_MAP_TYPE)) {
			throw new IllegalStateException("There is no map data for '" + DEFAULT_MAP_TYPE + "' defined.");
		}

		reader = createReader(megdrSpec);
 	}

	/**
	 * Creates a MEGDRReader based on a spec that contains the "reader type, filename".
	 * 
	 * @param spec
	 * @return
	 */
	static MEGDRMapReader createReader(String spec) {
		String [] parts = spec.split(SEPARATOR);
		
		String reader = parts[0].trim().toLowerCase();
		String imageName = ELEVATION_FOLDER + parts[1].trim();

		logger.config("imageName: " + imageName);
		
		try {
			return switch(reader) {
				case ARRAY_READER -> new MEGDRMapArray(imageName);
				case DIRECT_READER -> new MEGDRMapDirect(imageName);
				case MEMORY_READER -> new MEGDRMapMemory(imageName);
				default -> throw new IllegalArgumentException("Unknown MEGDR reader called " + reader);
			};
		}
		catch(IOException ioe) {
			logger.severe("Problem creating MEGDRReader " + ioe.getMessage());
			throw new IllegalArgumentException("Problem loading MEGDRReader:" + ioe.getMessage());
		}
	}

 	/**
 	 * Gets map data of the requested type.
 	 * 
 	 * @param mapType the map type
 	 * @param res
 	 * @return the map data
 	 */
 	void setMapData(String mapType, int res) {

		MapMetaData metaData = metaDataMap.get(mapType);
 		if (metaData == null) {
 			logger.log(Level.SEVERE, "Map type " + mapType + " unknown.");
			
			new MapDataFactory();
		}
 		else
 			// Change the map resolution
 			metaData.setResolution(res);
 	}
 	
 	/**
 	 * Loads the map data of the requested map type.
 	 * 
 	 * @param mapType the map type
 	 * @return the map data
 	 */
 	MapData loadMapData(String mapType) {
 		
 		MapData mapData = null;
 		
		MapMetaData mapMetaData = metaDataMap.get(mapType);
		
 		if (mapMetaData == null) {
 			logger.log(Level.SEVERE, "Map type " + mapType + " unknown.");
			return null;
		}

 		if (mapMetaDataCache != null && mapMetaDataCache.equals(mapMetaData)
 			&& !mapMetaData.getFile().equals("")
 			&& mapMetaDataCache.getFile().equals(mapMetaData.getFile())
 				) {
 			return mapDataCache;
 		}

		try {
			// Obtain a new MapData instance
			mapData = new IntegerMapData(mapMetaData);		
			// Patch the metadata to be locally available
			mapMetaData.setLocallyAvailable(true);
			
			mapDataCache = mapData;
			
			mapMetaDataCache = mapMetaData;
			
			logger.log(Level.CONFIG, "Loading map type '" + mapType 
					+ "'. Res level: " + mapMetaData.getResolution() 
					+ ". Map name: '" + mapMetaData.getMapType()
					+ "'. Filename: '" + mapMetaData.getFile()
					+ "'. Color: " + mapMetaData.isColourful()
					+ ". Local: " + mapMetaData.isLocallyAvailable() + ".");

		} catch (IOException e) {
			logger.log(Level.SEVERE, "Could not find the map file.", e);
		}
		
		return mapData;
 	}

	/**
	 * Gets the available map types.
	 * 
	 * @return
	 */
	public Collection<MapMetaData> getLoadedTypes() {
		return metaDataMap.values();
	}
	

	/**
	 * Transforms the pixel i and j into lat and lon coordinate.
	 * 
	 * @param i sample coordinate
	 * @param j line coordinate
	 * @param n the number of lines or samples per line in the image
      (the images are square)
	 * @param res the map resolution in pixels per degree
	 * @return
	 */
	public double[] convertToLatLon(int i, int j, int n, int res) {
		// The transformation from line and sample coordinates to planetocentric
		// latitude and longitude is given by these equations.
		
		// Convert to Cartesian coordinate system with (0,0) at center
		double x = (i - n/2.0 - 0.5)/res;
		double y = (j - n/2.0 - 0.5)/res;

		// The radius from center of map to pixel i,j
		double r = Math.sqrt(x*x + y*y);

		// The east longitude of pixel i,j in degrees
		double lon = Math.atan2(x,y) * DEG_PER_RADIAN;
		// The latitude of pixel i,j in degrees
		double lat = 0;
		
		// For northern hemisphere
		if (y > 0)
			lat = 90 - 2 * Math.atan(r * PI/360) * DEG_PER_RADIAN;
		else if (y < 0)
			// For southern hemisphere
			lat = -90 + 2 * Math.atan(r * PI/360) * DEG_PER_RADIAN;

		return new double[] {lat, lon};
	}
	
	public void destroy() {
		metaDataMap.clear();
		metaDataMap = null;
		if (reader instanceof Closeable cl) {
			try {
				cl.close();
			} catch (IOException e) {
			}
		}
		reader = null;
	}

   /**
	 * Gets the elevation as a short integer at a given location.
	 * 
	 * @param phi   the phi location.
	 * @param theta the theta location.
	 * @return the elevation as an integer.
	 */
    public short getElevation(double phi, double theta) {
        return reader.getElevation(phi, theta);
	}

	public static void main(String[] args) throws IOException {
		runPerfTest(DIRECT_READER + SEPARATOR + MEGDRMapReader.DEFAULT_MEGDR_FILE);
		runPerfTest(ARRAY_READER + SEPARATOR + MEGDRMapReader.DEFAULT_MEGDR_FILE);
		runPerfTest(MEMORY_READER + SEPARATOR + MEGDRMapReader.DEFAULT_MEGDR_FILE);
	}

	private static void runPerfTest(String spec) {
		DecimalFormat formatter = new DecimalFormat("###,###,###");

		long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		MEGDRMapReader reader = createReader(spec);
		int size = 10000;
		double pi2 = Math.PI * 2;
		Instant start = Instant.now();
		for(int i = 0; i < size; i++) {
			double phi = RandomUtil.getRandomDouble(Math.PI);
			double theta = RandomUtil.getRandomDouble(pi2);
			reader.getElevation(phi, theta);
		}
		Instant finish = Instant.now();
		long finishMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		System.out.println("Reader " + spec + " Memory increase " + formatter.format(finishMemory - startMemory));
		System.out.println(size + " lookups in " + Duration.between(start, finish).toMillis());
	}
 }
