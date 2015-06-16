/**
 * Mars Simulation Project
 * SurfaceFeatures.java
 * @version 3.08 2015-06-15
 * @author Scott Davis
 */
package org.mars_sim.msp.core.mars;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.person.ai.mission.Mining;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionManager;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.MarsClock;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SurfaceFeatures represents the surface terrain and landmarks of the virtual Mars.
 */
public class SurfaceFeatures implements Serializable {

	private static final long serialVersionUID = 1L;
	public static double MEAN_SOLAR_IRRADIANCE =  586D; // in flux or [W/m2]  = 1371 / (1.52*1.52)
	// This is the so-called "solar constant" of Mars (not really a constant per se), which is the flux of solar radiation at the top of the atmosphere (TOA) at the mean distance a between Mars and the sun.
	// Note: at the top of the Mars atmosphere
	// The solar irradiance at Mars' mean distance from the Sun (1.52 AU) is S0 = 590 Wm-2.
	// This is about 44% of the Earth's solar constant (1350 Wm-2).
	// At perihelion (1.382 AU), the maximum available irradiance is S = 717 Wm-2, while at apohelion (1.666 AU) the maximum is S = 493 Wm-2.
	// see http://ccar.colorado.edu/asen5050/projects/projects_2001/benoit/solar_irradiance_on_mars.htm


	// Data members
	private int dataset = 0;
	/** Current sol since the start of sim. */
	private int solCache = 1;

	private double opticalDepthStartingValue = 0.2342;

    private List<Landmark> landmarks;
    private List<ExploredLocation> exploredLocations;

    private transient Mars mars;
    private transient OrbitInfo orbitInfo;
    private transient TerrainElevation surfaceTerrain;
    private MineralMap mineralMap;
    private AreothermalMap areothermalMap;
    private MissionManager missionManager;
    private Coordinates sunDirection;
    private Weather weather;

	private Map<Coordinates, Double> opticalDepthMap = new ConcurrentHashMap<>();
	private Map<Coordinates, Double> totalSolarIrradianceMap = new ConcurrentHashMap<>();
	private Map<Coordinates, Double> solarIrradianceMapCache;
	private MarsClock solarIrradianceMapCacheTime;

	DecimalFormat fmt3 = new DecimalFormat("#0.000");

    /**
     * Constructor
     * @throws Exception when error in creating surface features.
     */
    public SurfaceFeatures() {

        surfaceTerrain = new TerrainElevation();
        mineralMap = new RandomMineralMap();
        exploredLocations = new ArrayList<ExploredLocation>();
        areothermalMap = new AreothermalMap();

        //weather = Simulation.instance().getMars().getWeather();

        mars = Simulation.instance().getMars();
        //orbitInfo = mars.getOrbitInfo();
        missionManager = Simulation.instance().getMissionManager();

        try {
            landmarks = SimulationConfig.instance().getLandmarkConfiguration().getLandmarkList();
        } catch (Exception e) {
            throw new IllegalStateException("Landmarks could not be loaded: " + e.getMessage(), e);
        }


    }

    /**
     * Initialize transient data in the simulation.
     * @throws Exception if transient data could not be constructed.
     */
    public void initializeTransientData() {

        // Initialize surface terrain.
        surfaceTerrain = new TerrainElevation();
    }

    /** Returns the surface terrain
     *  @return surface terrain
     */
    public TerrainElevation getSurfaceTerrain() {
        return surfaceTerrain;
    }

    /**
     * Returns a float value representing the current sunlight
     * conditions at a particular location.
     * @deprecated // use getSolarIrradiance() instead
     * @return value from 0.0 - 1.0
     * 0.0 represents night time darkness.
     * 1.0 represents daylight.
     * Values in between 0.0 and 1.0 represent twilight conditions.
     */
    public double getSurfaceSunlight(Coordinates location) {
        if (mars == null)
        	mars = Simulation.instance().getMars();
        Coordinates sunDirection = mars.getOrbitInfo().getSunDirection();
        double angleFromSun = sunDirection.getAngle(location);

        double result = 0;
        double twilightzone = .2D; // Angle width of twilight border (radians)
        if (angleFromSun < (Math.PI / 2D) - (twilightzone / 2D)) {
            result = 1D;
        } else if (angleFromSun > (Math.PI / 2D) + (twilightzone / 2D)) {
            result = 0D;
        } else {
            double twilightAngle = angleFromSun - ((Math.PI / 2D) - (twilightzone / 2D));
            result = 1D - (twilightAngle / twilightzone);
        }

        return result;
    }

    /**
     * Calculate the solar irradiance at a particular location on Mars
     * @param location the coordinate location on Mars.
     * @return solar irradiance (W/m2)
     */
    public double getSolarIrradiance(Coordinates location) {

        // Lazy instantiation of solarIrradianceMapCache.
        MarsClock currentTime = Simulation.instance().getMasterClock().getMarsClock();
        if ((solarIrradianceMapCacheTime == null) || !currentTime.equals(solarIrradianceMapCacheTime)) {
            if (solarIrradianceMapCache == null) {
                solarIrradianceMapCache = new ConcurrentHashMap<Coordinates, Double>();
            }
            solarIrradianceMapCache.clear();
            solarIrradianceMapCacheTime = (MarsClock) currentTime.clone();
        }

        // If location is not in cache, calculate solar irradiance.
        if (!solarIrradianceMapCache.containsKey(location)) {
            if (mars == null) {
                mars = Simulation.instance().getMars();
            }
            // The solar irradiance value below is the value on top of the atmosphere only
            //double lat = location.getPhi2Lat(location.getPhi());
            //System.out.println("lat is " + lat);
            /*
// Approach 1
		double s1 = 0;
        double L_s = mars.getOrbitInfo().getL_s();
        double e = OrbitInfo.ECCENTRICITY;
    	double z = mars.getOrbitInfo().getSolarZenithAngle(phi);
    	double num =  1 + e * Math.cos( (L_s - 248) /180D * Math.PI);
    	double den = 1 - e * e;
    	s1 = MEAN_SOLAR_IRRADIANCE * Math.cos(z) * num / den * num / den  ;
    	System.out.println("solar irradiance s1 is " + s1);
             */

// Approach 2 consists of 5 parts
            // Part 1: get cosine solar zenith angle
            double G_0 = 0;
            double G_h = 0;
            double G_bh = 0;
            double G_dh = 0;
            //G_0: solar irradiance at the top of the atmosphere
            //G_h: global irradiance on a horizontal surface
            //G_bh: direct beam irradiance on a horizontal surface
            //G_dh: diffuse irradiance on a horizontal surface

            if (orbitInfo == null)
                orbitInfo = mars.getOrbitInfo();

            double cos_z =  orbitInfo.getCosineSolarZenithAngle(location);
            if (cos_z <= 0) {
                //System.out.println("   cos_z : "+ fmt3.format(cos_z)
                //				+ "   G_0 : " + fmt3.format(G_0));
                // the sun is set behind the planet Mars, total darkness and no need of calculation.
                G_0 = 0;

            }

            else {

                // Part 2: get the new average solar irradiance as a result of the changing distance between Mars and Sun  with respect to the value of L_s.
                //double L_s = orbitInfo.getL_s();

                // Note a: Because of Mars's orbital eccentricity, L_s advances somewhat unevenly with time, but can be evaluated
                // as a trigonometric power series for the orbital eccentricity and the orbital mean anomaly measured with respect to the perihelion.
                // The areocentric longitude at perihelion, L_s = 251.000 + 0.00645 * (year - 2000),
                // indicates a near alignment of the planet's closest approach to the Sun in its orbit with its winter solstice season,

                // Note b: In 2043, there is 35% (max is 45.4%) on average more sunlight at perihelion (L_s = 251.2774 deg) than at aphelion (L_s = 71.2774 deg)
                // Equation: 135% * (.5 * sin (L_s - 251.2774 + 180 - 90) + .5 )
                double mean =  MEAN_SOLAR_IRRADIANCE; // * 0.675 * (1 + Math.sin((L_s - 161.2774)/180D * Math.PI));

                // Part 3: get the instantaneous radius and semi major axis
                double r =  orbitInfo.getDistanceToSun();
                double a = OrbitInfo.SEMI_MAJOR_AXIS;
                double radiusAndAxis =  a * a / r / r;

                G_0 = cos_z * mean * radiusAndAxis;

                //if (G_0 <= 0)
                //	G_0 = 0;

                // Part 4 : Absorption and Scattering of Solar Radiation and Optical Depth

                double tau;

                if (weather == null)
                    weather = Simulation.instance().getMars().getWeather();
                double newTau =  0.2237 * weather.getDailyVariationAirPressure(location);
                // System.out.println("DailyVariationAirPressure : " + weather.getDailyVariationAirPressure(location));
                // Initially, weather.getDailyVariationAirPressure() = 0.009773345677998181

                // Equation: tau = 0.2342 + 0.2247 * yestersolAirPressureVariation;
                // the starting value for opticalDepth is 0.2342. See Ref below
                if (opticalDepthMap.containsKey(location))
                    tau = (opticalDepthMap.get(location) + opticalDepthStartingValue + newTau) / 2D;
                else {
                    tau = opticalDepthStartingValue + newTau;
                }

                // Reference :
                // see Chapter 2.3.1 and equation (2.44,45) on page 63 from the book "Mars: Prospective Energy and Material Resources" by Badescu, Springer 2009.
                // Optical depth is well correlated to the daily variation of surface pressure and to the standard deviation of daily surface pressure
                // The lower the value of tau, the clearer the sky
                // Note: tau has an "inverse" relationship with the daily global solar irradiance in Fig 2.8.

                // Add randomness to optical depth
                tau = tau + RandomUtil.getRandomDouble(.05) - RandomUtil.getRandomDouble(.05);
                // Notes:
                // (1) during relatively periods of clear sky, typical values for optical depth were between 0.2 and 0.5
                // (2) typical observable range is between .32 and .52 (average is 42%).
                // (3) From Viking data, at no time did the optical depth fall below 0.18,

                // tau is usually between .1 and 6, Page 860, Table IV  of R. M. Haberlet Et al.
      			if (tau > 6)
      				tau = 6;
      			if (tau < .1)
      				tau = .1;

                // TODO: Part 4a : reducing opacity of the Martian atmosphere due to local dust storm

                // The extinction of radiation through the Martian atmosphere is caused mainly by suspended dust particles.
                // Although dust particles are effective at scattering direct solar irradiance, a substantial amount of diffuse light is able to penetrate to the surface of the planet.
                // The amount of PAR available on the Martian surface can then be calculated to be 42% of the total PAR to reach the surface.

                // Based on Viking observation, it's estimated approximately 100 local dust storms (each last a few days) can occur in a given Martian year

                // Duration of a global dust storm is 35 - 70 sols. Local dust storms last a few days.


                // Choice 1 : if using Beer's law : transmissivity = Math.exp(-tau/cos_z);
                //G_bh = G_0 * cos_z * Math.exp(-tau/cos_z);

    	    	// Choice 2 : The pure scattering transmissivity = (1 + tau / 2 / cos_z)^ -1

     	    	G_bh = G_0 * cos_z / (1 + tau / 2 / cos_z);

      			// assuming the reflection from the surface is negligible
    	    	// ref: http://www.uapress.arizona.edu/onlinebks/ResourcesNearEarthSpace/resources30.pdf


    	    	// Note: m(z), the air mass, is estimated as ~ 1/cos_z

                // save tau onto opticalDepthMap
                opticalDepthMap.put(location, tau);


                // Note:  one can estimate m(z), the air mass, as ~ 1/cos_z

                // Part 5 : Diffuse solar irradiance.
                // G_h = G_direct + G_diffuse
                // On earth, the direct solar irradiance plays the major role of bringing in sunlight
                // On Mars, the role of diffuse solar irradiance is more prominent than that on Earth.

                // TODO: Modeling the diffuse effect of solar irradiance with formula
                // Note: the value of G_dh to decrease more slowly when value cos_z is diminishing

    	    	if (cos_z > .9)
    	    		G_dh = G_bh / 6;
    	    	else if (cos_z > .8)
    	    		G_dh = G_bh / 4.8;
    	    	else if (cos_z > .7)
    	    		G_dh = G_bh / 3.7D;
    	    	else if (cos_z > .6)
    	    		G_dh = G_bh / 2.5;
    	    	else if (cos_z > .5)
    	    		G_dh = G_bh / 2.2D;
    	    	else if (cos_z > .4)
    	    		G_dh = G_bh / 1.8D;
    	    	else if (cos_z > .3)
    	    		G_dh = G_bh / 1.6D;
    	    	else if (cos_z > .2)
    	    		G_dh = G_bh / 1.4D;
    	    	else if (cos_z > .1)
    	    		G_dh = G_bh / 1.2D;
    	    	else if (cos_z > .05)
    	    		G_dh = G_bh;
    	    	// Finally,
    	    	G_h = G_bh + G_dh;

                //System.out.println(" radiusAndAxis : " + fmt3.format(radiusAndAxis)
                //				+ "   cos_z : "+ fmt3.format(cos_z)
                //				+ "   G_0 : " + fmt3.format(G_0)
                //				+ "   G_bh : " + fmt3.format(G_bh)
                //				+ "   G_dh : " + fmt3.format(G_dh)
                //				+ "   G_h : " + fmt3.format(G_h));

                // TODO: Part 6 : calculate other components on Mars such as scattering and reflective irradiance
            }

            solarIrradianceMapCache.put(location, G_h);
        }

    	return solarIrradianceMapCache.get(location);
    }

    public double getOpticalDepth(Coordinates location) {
    	double result;
    	if (opticalDepthMap.containsKey(location))
    		result = opticalDepthMap.get(location);
    	else
    		result = opticalDepthStartingValue;
    	return result;
    }

    /** Returns true if location is in a dark polar region.
     *  A dark polar region is where the sun doesn't rise in the current sol.
     *  @return true if location is in dark polar region
     */
    public boolean inDarkPolarRegion(Coordinates location) {

        boolean result = false;

        if (mars == null)
        	mars = Simulation.instance().getMars();
        if (sunDirection == null)
        	sunDirection = mars.getOrbitInfo().getSunDirection();

        double sunPhi = sunDirection.getPhi();
        double darkPhi = 0D;

        if (sunPhi < (Math.PI / 2D)) {
            darkPhi = Math.PI - ((Math.PI / 2D) - sunPhi);
            if (location.getPhi() >= darkPhi) {
                result = true;
            }
        } else {
            darkPhi = sunPhi - (Math.PI / 2D);
            if (location.getPhi() < darkPhi) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Checks if location is within a polar region of Mars.
     * @param location the location to check.
     * @return true if in polar region.
     */
    public boolean inPolarRegion(Coordinates location) {
        double polarPhi = .1D * Math.PI;

        return (location.getPhi() < polarPhi) || (location.getPhi() > Math.PI - polarPhi);
    }

    /**
     * Gets a list of landmarks on Mars.
     * @return list of landmarks.
     */
    public List<Landmark> getLandmarks() {
        return landmarks;
    }

    /**
     * Gets the mineral map.
     * @return mineral map.
     */
    public MineralMap getMineralMap() {
        return mineralMap;
    }

    /**
     * Adds an explored location.
     * @param location the location coordinates.
     * @param estimatedMineralConcentrations a map of all mineral types
     * and their estimated concentrations (0% -100%)
     * @param settlement the settlement the exploring mission is from.
     * @return the explored location
     */
    public ExploredLocation addExploredLocation(Coordinates location,
            Map<String, Double> estimatedMineralConcentrations, Settlement settlement) {
        ExploredLocation result = new ExploredLocation(location,
                estimatedMineralConcentrations, settlement);
        exploredLocations.add(result);
        return result;
    }

    /**
     * Gets a list of all explored locations on Mars.
     * @return list of explored locations.
     */
    public List<ExploredLocation> getExploredLocations() {
        return exploredLocations;
    }

    /**
     * Gets the areothermal heat potential for a given location.
     * @param location the coordinate location.
     * @return areothermal heat potential as percentage (0% - low, 100% - high).
     */
    public double getAreothermalPotential(Coordinates location) {
        return areothermalMap.getAreothermalPotential(location);
    }

    /**
     * Time passing in the simulation.
     * @param time time in millisols
     * @throws Exception if error during time.
     */
    public void timePassing(double time) {
/*
    	// TODO: clear the total solar irradiance map and save data in DailyWeather.
	    // check for the passing of each day
		if (masterClock == null)
			masterClock = Simulation.instance().getMasterClock();
		marsClock = masterClock.getMarsClock();

	    int newSol = MarsClock.getSolOfYear(marsClock);
		if (newSol != solCache) {


			totalSolarIrradianceMap.clear();
			solCache = newSol;
		}
*/
        // Update any reserved explored locations.
        Iterator<ExploredLocation> i = exploredLocations.iterator();
        while (i.hasNext()) {
            ExploredLocation site = i.next();
            if (site.isReserved()) {
                // Check if site is reserved by a current mining mission.
                // If not, mark as unreserved.
                boolean goodMission = false;
                if (missionManager == null)
                	missionManager = Simulation.instance().getMissionManager();
                Iterator<Mission> j = missionManager.getMissions().iterator();
                while (j.hasNext()) {
                    Mission mission = j.next();
                    if (mission instanceof Mining) {
                        if (site.equals(((Mining) mission).getMiningSite())) {
                            goodMission = true;
                        }
                    }
                }
                if (!goodMission) {
                    site.setReserved(false);
                }
            }
        }
    }

    /**
     * Prepare object for garbage collection.
     */
    public void destroy() {
        surfaceTerrain = null;
        orbitInfo = null;
        mars = null;
        missionManager = null;;
        sunDirection = null;
        landmarks.clear();
        landmarks = null;
        mineralMap.destroy();
        mineralMap = null;
        exploredLocations.clear();
        exploredLocations = null;
        areothermalMap.destroy();
        areothermalMap = null;
    }
}
