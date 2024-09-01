/*
 * Mars Simulation Project
 * Weather.java
 * @date 2024-02-03
 * @author Scott Davis
 * @author Hartmut Prochaska
 */
package com.mars_sim.core.environment;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import com.mars_sim.core.Simulation;
import com.mars_sim.core.air.AirComposition;
import com.mars_sim.core.data.MSolDataItem;
import com.mars_sim.core.data.MSolDataLogger;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.MasterClock;
import com.mars_sim.core.time.Temporal;
import com.mars_sim.core.tool.RandomUtil;

/** This class represents the weather properties on Mars. */
public class Weather implements Serializable, Temporal {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(Weather.class.getName());

	// Non-static data
	private final int MAX_RECORDED_DAYS = 2;
	/** The maximum initial windspeed of a new location. */
	private final double MAX_INITIAL_WINDSPEED = 20;
	/** The maximum initial windspeed of a new location. */
	private final double AVERAGE_WINDSPEED = 15;
	/** The effect of sunlight on the surface temperatures on Mars. */
	private final double LIGHT_EFFECT = 1.2;
	/** Extreme cold surface temperatures on Mars at deg Kelvin [or at -153.17 C]. */
	private final double EXTREME_COLD = 120D; 
	/** Viking 1's latitude. */
	private final double VIKING_LATITUDE = 22.48D;
	private final double VIKING_DT = Math.round((28D - 15D *
			Math.sin(2 * Math.PI / 180D * VIKING_LATITUDE + Math.PI / 2D) - 13D) * 100.0) / 100.00;
	
	public final double PARTIAL_PRESSURE_CARBON_DIOXIDE_MARS = 0.57D; // in kPa
	public final double PARTIAL_PRESSURE_CARBON_DIOXIDE_EARTH = 0.035D; // in kPa
	
	/** Under Earth's atmosphere, at 25 C, 50% relative humidity, in kPa. */
	public final double PARTIAL_PRESSURE_WATER_VAPOR_ROOM_CONDITION = 1.6D; 

	// Weather metrics update frequency
	private final int PRESSURE_REFRESH = 4;
	private final int TEMPERATURE_REFRESH = 2;
	private final int WINDSPEED_REFRESH = 5;
	private final int DATA_SAMPLING = 4;
	private final int DUST_STORM_REFRESH = 20;

	private final double DX = 255D * Math.PI / 180D - Math.PI;
	
	// Opportunity Rover landed at coordinates 1.95 degrees south, 354.47 degrees
	// east.
	// From the chart, it has an average of 8 C temperature variation on the maximum
	// and minimum temperature curves

	// Spirit Rover landed at 14.57 degrees south latitude and 175.47 degrees east
	// longitude.
	// From the chart, it has an average of 25 C temperature variation on the
	// maximum and minimum temperature curves
	private final double TEMPERATURE_DELTA_PER_DEG_LAT = 17 / 12.62;
	
	private int newStormID = 1;

	private int checkStorm = 0;
	// Note: compute the true dailyVariationAirPressure by the end of the day	
	private double dailyVariationAirPressure = RandomUtil.getRandomDouble(.01); // tentatively only
	
	// Singleton only updated in one method
	private Map<Coordinates, MSolDataLogger<DailyWeather>> weatherDataMap;
	
	private List<Coordinates> coordinateList;

	private transient Map<Coordinates, Double> temperatureCacheMap;
	private transient Map<Coordinates, Double> airPressureCacheMap;
	private transient Map<Coordinates, Double> windSpeedCacheMap;
	private transient Map<Coordinates, Integer> windDirCacheMap;

	private List<DustStorm> dustStorms;
	
	private Map<Coordinates, SunData> sunDataMap;
	
	private final ReentrantLock tempLock = new ReentrantLock(true);
	private final ReentrantLock pressureLock = new ReentrantLock(true);
	
	private OrbitInfo orbitInfo;
	private MasterClock clock;
	private SurfaceFeatures surfaceFeatures;
	
	public Weather(MasterClock clock, OrbitInfo orbitInfo) {
		weatherDataMap = new HashMap<>();
		sunDataMap = new HashMap<>();
		
		coordinateList = new ArrayList<>();
		dustStorms = new ArrayList<>();
		
		temperatureCacheMap = new HashMap<>();
		airPressureCacheMap = new HashMap<>();
		windSpeedCacheMap = new HashMap<>();
		windDirCacheMap = new HashMap<>();

		this.orbitInfo = orbitInfo;
		this.clock = clock;
	}

	void setSurfaceFeatures(SurfaceFeatures sf) {
		surfaceFeatures = sf;
	}

	/**
	 * Adds a location to the coordinate list.
	 * 
	 * @param location
	 */
	public void addLocation(Coordinates location) {
		if (!coordinateList.contains(location))
			coordinateList.add(location);
	}

	/**
	 * Gets the air density at a given location.
	 * 
	 * @return air density in g/m3.
	 */
	public double computeAirDensity(Coordinates location) {
		// The air density is derived from the equation of state : 
		// d = p / .1921 / (t + 273.1)
		// Multiply by 1000 to convert from kg/m3 to g/m3
		return 1000D * getAirPressure(location)
				/ (.1921 * (getTemperature(location) + AirComposition.C_TO_K));
	}

	/**
	 * Gets the air density at a given location.
	 * 
	 * @return air density in g/m3.
	 */
	public double getAirDensity(Coordinates location) {
		return computeAirDensity(location);
	}

	/**
	 * Gets the wind speed at a given location.
	 * 
	 * @return wind speed in m/s.
	 */
	private double computeWindSpeed(Coordinates location) {
		double newSpeed = 0;
		
		if (windSpeedCacheMap == null)
			windSpeedCacheMap = new HashMap<>();

		// On sol 214 in this list of Viking wind speeds, 
		// 25.9 m/sec (93.24 km/hr) was recorded.

		// Viking spacecraft from the surface, "during a global dust storm the diurnal
		// temperature range narrowed sharply,
		// ...the wind speeds picked up considerably—indeed, within only an hour
		// of the storm's arrival they  had increased to 17 m/s (61 km/h), 
		// with gusts up to 26 m/s (94 km/h)
		// https://en.wikipedia.org/wiki/Climate_of_Mars
		double optical = surfaceFeatures.getOpticalDepth(location);
		
		if (windSpeedCacheMap.containsKey(location)) {
			// Load the previous wind speed
			double currentSpeed = windSpeedCacheMap.get(location);
			
			// Check if the location is covered by a Dust Storm
			DustStorm ds = null;
			for (DustStorm s : dustStorms) {
				// Maybe it should include the width of the DustStorm also
				if (s.getCoordinates().equals(location)) {
					ds = s;
					break;
				}
			}

			// Storm governs the wind speed
			if (ds != null) {
				double stormSpeed = 0;
				double dustSpeed = ds.getSpeed();
				switch (ds.getType()) {
					case DUST_DEVIL:
						// arbitrary speed determination
						stormSpeed = .8 * currentSpeed + .2 * dustSpeed;
						break;

					case LOCAL:
						// arbitrary speed determination
						stormSpeed = .985 * currentSpeed + .015 * dustSpeed;
						break;
					
					case REGIONAL:
						// arbitrary speed determination
						stormSpeed = .99 * currentSpeed + .01 * dustSpeed;
						break;

					case PLANET_ENCIRCLING:
						// arbitrary speed determination
						stormSpeed = .995 * currentSpeed + .005 * dustSpeed;
						break;
						
					default :
						stormSpeed = .99 * currentSpeed;
				}
				
				// Assume the max surface wind speed of up to 800 m/s
				if (stormSpeed > 800) {
					stormSpeed = 800;
				}
				
				newSpeed = stormSpeed;
			}
			
			else { 
				int msol = clock.getMarsTime().getMillisolInt();
				
				// the value of optical depth doesn't need to be refreshed too often
				if (clock.getClockPulse() != null && clock.getClockPulse().isNewIntMillisol()
						&& msol % WINDSPEED_REFRESH == 0) {
					
					double rand = RandomUtil.getRandomDouble(-0.02, 0.02);
					
					double[] terrain = surfaceFeatures.getTerrainElevation().getTerrainProfile(location);
					
					double boundary = Math.round(AVERAGE_WINDSPEED * optical 
									* Math.log(1.1 + Math.abs((1 + terrain[0]) * (5 - terrain[1])))* 1000.0)/1000.0;
					
					// Swing the wind speed back to AVERAGE_WINDSPEED
					if (currentSpeed > boundary) {
						newSpeed = currentSpeed * (1 + rand) - (currentSpeed - boundary) * Math.abs(rand) / 20;
					}
					else if (currentSpeed > boundary / 2) {
						newSpeed = currentSpeed * (1 + rand) - (currentSpeed - boundary / 2) * Math.abs(rand) / 20;
					}
					else {
						newSpeed = currentSpeed * (1 + rand) + (boundary / 2 - currentSpeed) * Math.abs(rand) / 40;
					}
								
					newSpeed = Math.round(newSpeed *1000.0)/1000.0;
					
					if (newSpeed < 0) {
						newSpeed = 0;
					}
					
					// Assume the max surface wind speed of up to 100 m/s
					if (newSpeed > 100) {
						newSpeed = 100;
					}
				}
				
				else {
					// Make no change to the previous wind speed
					newSpeed = currentSpeed;
				}
			}
		}
		
		else {
			// If wind cache doesn't exist at this location 
			newSpeed = RandomUtil.getRandomDouble(MAX_INITIAL_WINDSPEED) ;
			
			newSpeed = Math.round(newSpeed * 1000.0)/1000.0;
		}

		// Despite secondhand estimates of higher velocities, official observed gust
		// velocities on Mars are
		// in the range of 80-120 mph (120-160 km/hr).
		// At higher altitudes, the movement of dust was measured at 250-300 mph
		// (400-480 km/hr).

		// Note : 1 mile per hour (mph) = 0.44704 meter per sec (m/s)
		
		windSpeedCacheMap.put(location, newSpeed);
		
		return newSpeed;
	}

	/**
	 * Gets the wind speed at a given location.
	 * 
	 * @return wind speed in m/s.
	 */
	public double getWindSpeed(Coordinates location) {
		return computeWindSpeed(location);
	}

	/**
	 * Gets the wind direction at a given location.
	 * 
	 * @return wind direction in degree.
	 */
	public int getWindDirection(Coordinates location) {
		return computeWindDirection(location);
	}

	/**
	 * Computes the wind direction at a given location.
	 * 
	 * @return wind direction in degree.
	 */
	public int computeWindDirection(Coordinates location) {
		int result = 0;

		if (getWindSpeed(location) < 0.01)
			return 0;

		int newDir = RandomUtil.getRandomInt(359);

		if (windDirCacheMap == null)
			windDirCacheMap = new HashMap<>();

		if (windDirCacheMap.containsKey(location))
			// TODO: should the ratio of the weight of the past direction and present
			// direction of the wind be 9 to 1 ?
			result = (windDirCacheMap.get(location) * 9 + newDir) / 10;
		else {
			result = newDir;
		}

		if (result > 360)
			result = result - 360;

		windDirCacheMap.put(location, result);

		return result;
	}

	/**
	 * Computes the air pressure at a given location.
	 * 
	 * @return air pressure in Pa.
	 */
	public double getAirPressure(Coordinates location) {
		return getCachedAirPressure(location);
	}

	// The air pressure varies from 690 to 780 Pa in daily cycles from Sol 9.5 to 13
	// See chart at
	// https://mars.jpl.nasa.gov/msl/mission/instruments/environsensors/rems/

	// also the air pressure varies 730 to 920 throughout the year (L_s 0 to 360)
	// See chart at
	// http://cab.inta-csic.es/rems/en/weather-report-mars-year-33-month-11/

	/**
	 * Gets the cached air pressure at a given location.
	 * 
	 * @return air pressure in kPa.
	 */
	public double getCachedAirPressure(Coordinates location) {
		double newP = 0;
		
		// Lazy instantiation of airPressureCacheMap.
		if (airPressureCacheMap == null) {
			airPressureCacheMap = new HashMap<>();
		}

		pressureLock.lock();
		
		if (clock.getClockPulse() != null && clock.getClockPulse().isNewIntMillisol()
				&& clock.getMarsTime().getMillisolInt() % PRESSURE_REFRESH == 1) {
			newP = calculateAirPressure(location, 0);
			airPressureCacheMap.put(location, newP);
		}
		else {
			newP = airPressureCacheMap.computeIfAbsent(location, l -> calculateAirPressure(l, 0));
		}
		
		double prevP = 0;
		if (airPressureCacheMap.containsKey(location)) {
			prevP = airPressureCacheMap.get(location);
			newP = Math.round((newP + prevP) / 2.0 * 100.0) / 100.0;
			airPressureCacheMap.put(location, newP);
		}
		
		pressureLock.unlock();
		
		return newP;
	}

	/**
	 * Calculates the air pressure at a given location and/or height.
	 * 
	 * @param location
	 * @param height   [in km]
	 * @return air pressure [in kPa]
	 */
	public double calculateAirPressure(Coordinates location, double height) {
		// Get local elevation in meters.
		double elevation = 0;

		if (height == 0)
			elevation = TerrainElevation.getAverageElevation(location); // in km since getElevation() return the value in km
		else
			elevation = height;

		// p = pressure0 * e(-((density0 * gravitation) / pressure0) * h)
		// Q: What are these enclosed values ==> P = 0.009 * e(-(0.0155 * 3.0 / 0.009) *
		// elevation

		// Use curve-fitting equations at
		// http://www.grc.nasa.gov/WWW/k-12/airplane/atmosmrm.html for modeling Mars
		// p = .699 * exp(-0.00009 * h); p in kPa, h in m

		double pressure = 0.699 * Math.exp(-0.00009 * elevation * 1000);
		// why * 1000 ? The input value of height was in km, but h is in meters

		// Added randomness
		double up = RandomUtil.getRandomDouble(.01);
		double down = RandomUtil.getRandomDouble(.01);

		pressure = pressure + up - down;

		return pressure;
	}

	/**
	 * Gets the temperature at a given location.
	 * 
	 * @return temperature in deg Celsius.
	 */
	public double getTemperature(Coordinates location) {
		double newT = 0;
		
		// Lazy instantiation of temperatureCacheMap.
		if (temperatureCacheMap == null) {
			temperatureCacheMap = new HashMap<>();
		}
	
		tempLock.lock();
		
		if (clock.getClockPulse() != null && clock.getClockPulse().isNewIntMillisol()
				&& clock.getMarsTime().getMillisolInt() % TEMPERATURE_REFRESH == 0) {
			newT = calculateTemperature(location);
		} 
		else {
			newT = temperatureCacheMap.computeIfAbsent(location, l -> calculateTemperature(l));
		}
		
		double prevT = 0;
		if (temperatureCacheMap.containsKey(location)) {
			prevT = temperatureCacheMap.get(location);
			newT = Math.round((newT + prevT) / 2.0 * 100.0) / 100.0;
			temperatureCacheMap.put(location, newT);
		}
		
		tempLock.unlock();

		return newT;
	}

	/**
	 * Calculates the mid-air temperature.
	 * 
	 * @param h is elevation in km
	 * @return temperature at elevation h
	 */
	public double calculateMidAirTemperature(double h) {
		double t = 0;

		// Assume a temperature model with two zones with separate curve fits for the
		// lower atmosphere
		// and the upper atmosphere.

		// In the both lower and upper atmosphere, the temperature decreases linearly
		// and the pressure decreases exponentially.
		// The rate of temperature decrease is called the lapse rate. For the
		// temperature T and the pressure p,
		// the metric units curve fits for the lower atmosphere are:

		if (h <= 7)
			t = -31 - 0.000998 * h;
		else
			t = -23.4 - 0.00222 * h;

		return t;
	}

	/**
	 * Calculates the surface temperature at a given location.
	 * 
	 * @return temperature in Celsius.
	 */
	public double calculateTemperature(Coordinates location) {

		double t = 0;

		if (surfaceFeatures.inDarkPolarRegion(location)) {

			// vs. just in inPolarRegion()
			// see http://www.alpo-astronomy.org/jbeish/Observing_Mars_3.html
			// Note that the polar region may be exposed to more sunlight

			// see https://www.atmos.umd.edu/~ekalnay/pubs/2008JE003120.pdf
			// The swing can be plus and minus 10K deg

			t = EXTREME_COLD + RandomUtil.getRandomDouble(10)
					- AirComposition.C_TO_K;
		}

		else if (surfaceFeatures.inPolarRegion(location)) {

			// Based on Surface brightness temperatures at 32 µm retrieved from the MCS data
			// for
			// over five Mars Years (MY), at the “Tleilax” site.

			double lS = orbitInfo.getSunAreoLongitude();

			// split into 6 zones for linear curve fitting for each martian year
			// See chart at https://www.hou.usra.edu/meetings/marspolar2016/pdf/6012.pdf

			if (lS < 90)
				t = 0.8333 * lS + 145;
			else if (lS <= 180)
				t = -0.8333 * lS + 295;
			else if (lS <= 225)
				t = -.3333 * lS + 205;
			else if (lS <= 280)
				t = .1818 * lS + 89.091;
			else if (lS <= 320)
				t = -.125 * lS + 175;
			else if (lS <= 360)
				t = .25 * lS + 55;

			t = t + RandomUtil.getRandomDouble(-1.0, 1.0) - AirComposition.C_TO_K;

		} else {
			// We arrived at this temperature model based on Viking 1 & Opportunity Rover
			// by assuming the temperature is the linear combination of the following
			// factors:
			// 1. Time of day, longitude and solar irradiance,
			// 2. Terrain elevation,
			// 3. Latitude,
			// 4. Seasonal variation (dependent upon latitude)
			// 5. Randomness
			// 6. Wind speed
			
			double lightFactor = surfaceFeatures.getSunlightRatio(location) * LIGHT_EFFECT;

			// Equation below is modeled after Viking's data.
			double equatorialTemperature = 27.5D * lightFactor - 58.5D;

			// (2). Terrain Elevation
			// use http://www.grc.nasa.gov/WWW/k-12/airplane/atmosmrm.html for modeling Mars
			// with precalculated values
			// The lower atmosphere runs from the surface of Mars to 7,000 meters.
			// T = -31 - 0.000998 * h
			// The upper stratosphere model is used for altitudes above 7,000 meters.
			// T = -23.4 - 0.00222 * h

			double elevation = TerrainElevation.getAverageElevation(location); // in km from getElevation(location)
			double terrainDT;

			// Assume a typical temperature of -31 deg celsius
			if (elevation < 7)
				terrainDT = -0.000998 * elevation * 1000;
			else // delta = -31 + 23.4 = 7.6
				terrainDT = 7.6 - 0.00222 * elevation * 1000;

			// (3). Latitude
			double latDegree = location.getPhi2Lat();

			double latDt = -15D * (1 + Math.sin(2D * latDegree * Math.PI / 180D + Math.PI / 2D));

			// (4). Seasonal variation
			double latAdjustment = TEMPERATURE_DELTA_PER_DEG_LAT * latDegree; // an educated guess
			int solElapsed = clock.getMarsTime().getMissionSol();
			double seasonalDt = latAdjustment * Math.sin(2 * Math.PI / 1000D * (solElapsed - 142));

			// (5). Add windspeed
			double windDt = 0;
			if (windSpeedCacheMap == null)
				windSpeedCacheMap = new HashMap<>();

			if (windSpeedCacheMap.containsKey(location))
				windDt = 10.0 / (1 + Math.exp(-.15 * windSpeedCacheMap.get(location)));

			// Subtotal		
			t = equatorialTemperature + VIKING_DT - latDt - terrainDT + seasonalDt;

			if (t > 0)
				t = t + windDt;
			else
				t = t - windDt;
			
			// (5). Limit the highest and lowest temperature
			if (t > 40)
				t = 40;
			
			else if (t < -160)
				t = -160;

			// (6). Add randomness
			double rand = RandomUtil.getRandomDouble(-1.0, 1.0);
			
			// (7). Total
			t += rand;  
					
			double previousTemperature = 0;
			if (temperatureCacheMap == null) {
				temperatureCacheMap = new HashMap<>();
			}

			if (temperatureCacheMap.containsKey(location)) {
				previousTemperature = temperatureCacheMap.get(location);
			}

			t = Math.round((t + previousTemperature) / 2.0 * 100.0) / 100.0;
		}

		return t;
	}

	/**
	 * Clears weather-related parameter cache map to prevent excessive build-up of
	 * key-value sets.
	 */
	public synchronized void clearMap() {
		if (temperatureCacheMap != null) {
			temperatureCacheMap.clear();
		}

		if (airPressureCacheMap != null) {
			airPressureCacheMap.clear();
		}
		
		if (windSpeedCacheMap != null) {
			windSpeedCacheMap.clear();
		}
		
		if (windDirCacheMap != null) {
			windDirCacheMap.clear();
		}
	}

	/**
	 * Creates a weather record based on yestersol sun data.
	 */
	public void addWeatherDataPoint() {
		coordinateList.forEach(location ->  {			
			MSolDataLogger<DailyWeather> dailyRecordMap = 
					weatherDataMap.computeIfAbsent(location,
								k -> new MSolDataLogger<>(MAX_RECORDED_DAYS));		
			
			DailyWeather dailyWeather = new DailyWeather( 
					getTemperature(location), 
					getAirPressure(location),
					getAirDensity(location), 
					getWindSpeed(location), 
					surfaceFeatures.getSolarIrradiance(location),
					surfaceFeatures.getOpticalDepth(location));
			
			dailyRecordMap.addDataPoint(dailyWeather);			
		});
	}
	
	/**
	 * Time passing in the simulation.
	 * 
	 * @param time time in millisols
	 * @throws Exception if error during time.
	 */
	public boolean timePassing(ClockPulse pulse) {
		
		// Sample a data point every RECORDING_FREQUENCY (in millisols)
		int msol = pulse.getMarsTime().getMillisolInt();
		int remainder0 = msol % DATA_SAMPLING;
		int remainder1 = msol % DUST_STORM_REFRESH;
		
		if (pulse.isNewIntMillisol()) {
			
			if (remainder0 == 1) {		
				// Add a data point
				addWeatherDataPoint();
			}
			
			if (remainder1 == 1) {		
				// More often observed from mid-southern summer, between 241 deg and 270 deg Ls,
				// with a peak period at 255 deg Ls.

				// Note : The Mars dust storm season begins just after perihelion at around Ls =
				// 260°.			
				double aLs = (int) (Math.round(orbitInfo.getSunAreoLongitude()));
				int aLon = (int) (aLs);
						
				if (aLon == 230) {
					// reset the counter once a year
					checkStorm = 0;
				}

				// Arbitrarily assume
				// (1) 5% is the highest chance of forming a storm, if L_s is right at 255 deg
				// (2) 0.05% is the lowest chance of forming a storm, if L_s is right at 75 deg

				// By doing curve-fitting a cosine curve
				// (5% - .05%)/2 = 2.475

				double probability = -2.475 * Math.cos(aLs * Math.PI / 180D - DX) + (2.475 + .05);
				// probability is 5% at max
				double size = dustStorms.size();
				
				// Artificially limit the # of dust storm to 10
				if (aLon > 240 && aLon < 271 && size <= 10  && checkStorm < 200) {
					// When L_s = 250 (use 255 instead), Mars is at perihelion--when the sun is
					// closed to Mars.

					// All of the observed storms have begun within 50-60 degrees of Ls of
					// perihelion (Ls ~ 250);
					createDustDevils(probability, aLs);
				}

				else if (dustStorms.size() <= 20 && checkStorm < 200) {

					createDustDevils(probability, aLs);
				}

				checkOnDustStorms();
			}			
		}

		if (pulse.isNewHalfSol()) {

			dailyVariationAirPressure += RandomUtil.getRandomDouble(-.01, .01);
			if (dailyVariationAirPressure > .05)
				dailyVariationAirPressure = .05;
			else if (dailyVariationAirPressure < -.05)
				dailyVariationAirPressure = -.05;
		}
		
		if (pulse.isNewSol()) {
			// Calculate the new sun data for each location based on yestersol
			coordinateList.forEach(this::calculateSunRecord);
		}
		
		return true;
	}


	/**
	 * Calculates the sunlight data of a settlement location.
	 * 
	 * @param c
	 * @return
	 */
	public void calculateSunRecord(Coordinates c) {			
		List<MSolDataItem<DailyWeather>> dailyWeatherList = null;

		if (weatherDataMap.containsKey(c)) {
			MSolDataLogger<DailyWeather> w = weatherDataMap.get(c);
	
			if (!w.isYestersolDataValid()) {
				logger.warning(30_000, "Weather data from yestersol at " + c + " not available.");
				return;
			}
			else
				dailyWeatherList = w.getYestersolData();
		}
		else {
			logger.warning(30_000, "Weather data at " + c + " not available.");
			return;
		}
		
		if (dailyWeatherList == null || dailyWeatherList.isEmpty())
			return;

		int sunrise = 0;
		int sunset = 0;
		int maxIndex0 = 0;
		int maxIndex1 = 0;		
		int maxSun = 0;
		int previous = 0;
		int daylight = 0;
				
		for (MSolDataItem<DailyWeather> dataPoint : dailyWeatherList) {
			// Gets the solar irradiance at this instant of time
			int current = (int)(Math.round(dataPoint.getData().getSolarIrradiance()*10.0)/10.0);
			// Gets this instant of time
			int t = dataPoint.getMsol();
			if (current > 0) {
				// Sun up
				if (current > previous && previous <= 0) {
					sunrise = t;
				}
			}
			else {
				// Sun down
				if (current < previous && previous > 0) {
					sunset = t;
				}
			}

			// Gets maxSun as the max solar irradiance
			// Gets maxIndex0 at this instant of time
			if (current > maxSun && current > previous) {
				maxSun = current;
				maxIndex0 = t;
			}
			
			if (current < maxSun && previous == maxSun) {
				maxIndex1 = t;
			}	
			
			previous = current;
		}
		
		if (sunrise > sunset)
			daylight = sunset + 1000 - sunrise;
		else
			daylight = sunset - sunrise ;
		
		if (sunrise > 1000)
			sunrise = sunrise - 1000;
		if (sunrise < 0)
			sunrise = sunrise + 1000;
		
		if (sunset > 1000)
			sunset = sunset - 1000;
		if (sunset < 0)
			sunset = sunset + 1000;
		
		if (maxIndex1 < maxIndex0)
			maxIndex1 += 1000;
			
		int duration = maxIndex1 - maxIndex0;
		
		int zenith = maxIndex0 + duration/2;
		
		if (zenith > 1000)
			zenith = zenith - 1000;
		if (zenith < 0)
			zenith = zenith + 1000;
		
		SunData sunData = new SunData(sunrise, sunset, daylight, zenith, maxSun);
		// Overwrite the previous data
		sunDataMap.put(c, sunData);
	}
	

	
	/**
	 * Gets the sun data record.
	 * 
	 * @param c
	 * @return
	 */
	public SunData getSunRecord(Coordinates c) {
		return sunDataMap.get(c);
	}
	

	/**
	 * Checks if a dust devil is formed for each settlement.
	 * 
	 * @param probability
	 * @param L_s_int
	 */
	private void createDustDevils(double probability, double ls) {
		// TODO this needs fixing
		Collection<Settlement> settlements = Simulation.instance().getUnitManager().getSettlements();
		for (Settlement s : settlements) {
			
			if (s.getDustStorm() == null) {
				// if settlement doesn't have a dust storm formed near it yet
		
				double chance = RandomUtil.getRandomDouble(100);
				if (chance <= probability) {

					// arbitrarily set to the highest 3% chance (if L_s is 241 or 270) of generating
					// a dust devil
					// on each sol since it is usually created in Martian spring or summer day,
					checkStorm++;
					createStorm(s, DustStormType.DUST_DEVIL); 
				}
			}
		}
	}

	/**
	 * Creates a dust storm at a certain Settlement.
	 * 
	 * @param s Settlement where the storm is focused
	 * @param stormType Type of storm to create
	 * @return
	 */
	public DustStorm createStorm(Settlement s, DustStormType stormType) {
		// Assuming all storms start out as a dust devil
		DustStorm ds = new DustStorm(stormType, newStormID, this, s);
		dustStorms.add(ds);
		s.setDustStorm(ds);
		newStormID++;
		logger.info(s, 30_000, ds.getName() + " (type " + stormType.getName() + ") was visible on radar.");
		return ds;
	}


	/**
	 * Checks to DustStorms.
	 */
	private void checkOnDustStorms() {
		boolean allowPlantStorms = (dustStorms.stream()
				.filter(d -> d.getType() == DustStormType.PLANET_ENCIRCLING)
				.count() < 2);
		
		if (!dustStorms.isEmpty()) {
			List<DustStorm> storms = new ArrayList<>(dustStorms);
			for (DustStorm ds : storms) {
				if (ds.computeNewSize(allowPlantStorms) == 0) {
					dustStorms.remove(ds);
				} 
		
				if (ds.getSize() != 0) {
					Settlement s = ds.getSettlement();
					String msg = ds.getName()
						+ " (size " + ds.getSize() + " with wind speed "
						+ Math.round(ds.getSpeed() * 10.0) / 10.0 + " m/s) was sighted.";
					s.setDustStormMsg(msg);
					logger.info(s, 30_000, msg);
				}
			}
		}
	}

	/**
	 * Gets daily variation.
	 * 
	 * @param location Not used
	 * @return
	 */
	public double getDailyVariationAirPressure(Coordinates location) {
		return dailyVariationAirPressure;
	}

	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		weatherDataMap.clear();
		weatherDataMap = null;
		coordinateList.clear();
		coordinateList = null;
		
		if (temperatureCacheMap != null) {
			temperatureCacheMap.clear();
			temperatureCacheMap = null;
		}
		if (airPressureCacheMap != null) {
			airPressureCacheMap.clear();
			airPressureCacheMap = null;
		}
		if (windSpeedCacheMap != null) {
			windSpeedCacheMap.clear();
			windSpeedCacheMap = null;
		}
		if (windDirCacheMap != null) {
			windDirCacheMap.clear();
			windDirCacheMap = null;
		}
		if (dustStorms != null) {
			dustStorms.clear();
			dustStorms = null;
		}

		sunDataMap.clear();
		sunDataMap = null;
		orbitInfo = null;
	}
}
