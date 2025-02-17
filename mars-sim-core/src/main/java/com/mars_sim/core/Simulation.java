/*
 * Mars Simulation Project
 * Simulation.java
 * @date 2024-09-01
 * @author Scott Davis
 */
package com.mars_sim.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import com.mars_sim.core.air.AirComposition;
import com.mars_sim.core.authority.AuthorityFactory;
import com.mars_sim.core.data.DataLogger;
import com.mars_sim.core.data.History;
import com.mars_sim.core.data.UnitSet;
import com.mars_sim.core.environment.MarsSurface;
import com.mars_sim.core.environment.OrbitInfo;
import com.mars_sim.core.environment.OuterSpace;
import com.mars_sim.core.environment.SurfaceFeatures;
import com.mars_sim.core.environment.Weather;
import com.mars_sim.core.equipment.EquipmentFactory;
import com.mars_sim.core.events.HistoricalEventManager;
import com.mars_sim.core.goods.CreditManager;
import com.mars_sim.core.goods.GoodsManager;
import com.mars_sim.core.interplanetary.transport.TransportManager;
import com.mars_sim.core.logging.SimuLoggingFormatter;
import com.mars_sim.core.malfunction.MalfunctionFactory;
import com.mars_sim.core.malfunction.MalfunctionManager;
import com.mars_sim.core.mission.MissionStep;
import com.mars_sim.core.moon.LunarColonyManager;
import com.mars_sim.core.moon.LunarWorld;
import com.mars_sim.core.moon.Moon;
import com.mars_sim.core.person.PersonConfig;
import com.mars_sim.core.person.PhysicalCondition;
import com.mars_sim.core.person.ai.Mind;
import com.mars_sim.core.person.ai.job.util.Job;
import com.mars_sim.core.person.ai.mission.AbstractMission;
import com.mars_sim.core.person.ai.mission.MissionManager;
import com.mars_sim.core.person.ai.role.RoleUtil;
import com.mars_sim.core.person.ai.social.Relation;
import com.mars_sim.core.person.ai.task.util.MetaTaskUtil;
import com.mars_sim.core.person.ai.task.util.TaskManager;
import com.mars_sim.core.person.health.MedicalConfig;
import com.mars_sim.core.person.health.MedicalManager;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.science.ScientificStudy;
import com.mars_sim.core.science.ScientificStudyManager;
import com.mars_sim.core.science.ScientificStudyUtil;
import com.mars_sim.core.structure.Airlock;
import com.mars_sim.core.structure.ExplorationManager;
import com.mars_sim.core.structure.building.BuildingConfig;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.function.Function;
import com.mars_sim.core.structure.building.function.farming.AlgaeFarming;
import com.mars_sim.core.structure.building.function.farming.Crop;
import com.mars_sim.core.structure.building.function.farming.CropConfig;
import com.mars_sim.core.structure.building.utility.heating.Heating;
import com.mars_sim.core.structure.building.utility.heating.SolarHeatingSource;
import com.mars_sim.core.structure.building.utility.power.PowerSource;
import com.mars_sim.core.structure.construction.SalvageValues;
import com.mars_sim.core.time.ClockListener;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.MasterClock;
import com.mars_sim.core.time.SystemDateTime;
import com.mars_sim.core.tool.CheckSerializedSize;
import com.mars_sim.core.tool.Msg;

/**
 * The Simulation class is the primary singleton class in the MSP simulation.
 * It's capable of creating a new simulation or loading/saving an existing one.
 */
public class Simulation implements ClockListener, Serializable {

	private static class AutoSaveTrigger implements ClockListener {
		private Simulation sim;
		private SaveType type;
		
		public AutoSaveTrigger(Simulation sim, SaveType type) {
			super();
			this.sim = sim;
			this.type = type;
		}

		@Override
		public void pauseChange(boolean isPaused, boolean showPane) {
			// placeholder
		}
		
		@Override
		public void clockPulse(ClockPulse currentPulse) {
			// Set the pending save flag for an auto save
			sim.savePending = type;
		}
	}
	
	/** default serial id. */
	private static final long serialVersionUID = -631308653510974249L;

	private static final Logger logger = Logger.getLogger(Simulation.class.getName());

	enum SaveType {
		/** Save as default.sim. */
		SAVE_DEFAULT,
		/** Save as other name. */
		SAVE_AS,
		/** Autosave as default.sim. */
		AUTOSAVE_AS_DEFAULT,
		/** Autosave with build info and timestamp. */
		AUTOSAVE;
	}

	/** The dashes. */
	private static final String DASHES = " ---------------------------------------------------------";

	/** Default save filename. */
	public static final  String SAVE_FILE = Msg.getString("Simulation.saveFile"); //$NON-NLS-1$
	/** Default save filename extension. */
	public static final String SAVE_FILE_EXTENSION = Msg.getString("Simulation.saveFile.extension"); //$NON-NLS-1$


	/** true if displaying graphic user interface. */
	private transient boolean useGUI = true;
	/** Flag to indicate that a new simulation is being created or loaded. */
	private transient boolean isUpdating = false;
	/** Flag to keep track of whether the initial state of simulation has been initialized. */
	private transient boolean doneInitializing = false;

	private transient boolean justSaved = true;

	private transient boolean clockOnPause = false;

	private boolean initialSimulationCreated = false;

	/** The time stamp of the last saved sim. */
	private Date lastSaveTimeStamp = null;
	
	/** Clock listener that triggers autosaving **/
	private transient ClockListener autoSaveHandler;

	// Intransient data members (stored in save file)
	/** The lunar world (both surface and underground). */
	private LunarWorld lunarWorld; 
	/** The lunar colony manager. */
	private LunarColonyManager lunarColonyManager;
	/** Orbital info. */
	private OrbitInfo orbitInfo;
	/** The weather info. */
	private Weather weather;
	/** The surface features. */
	private SurfaceFeatures surfaceFeatures;
	/** All historical info. */
	private HistoricalEventManager eventManager;
	/** The malfunction factory. */
	private MalfunctionFactory malfunctionFactory;
	/** Manager for all units in simulation. */
	private UnitManager unitManager;
	/** Mission controller. */
	private MissionManager missionManager;
	/** Medical complaints. */
	private MedicalManager medicalManager;
	/** Master clock for the simulation. */
	private MasterClock masterClock;
	/** Manages scientific studies. */
	private ScientificStudyManager scientificStudyManager;
	/** Manages transportation of settlements and resupplies from Earth. */
	private TransportManager transportManager;
	/** The SimulationConfig instance. */
	private transient SimulationConfig simulationConfig;

	private transient SaveType savePending = null;
	private transient File savePendingFile = null;
	private transient SimulationListener saveCallback = null;

	/**
	 * Private constructor for the Singleton Simulation. This prevents instantiation
	 * from other classes.
	 */
	private Simulation() {
		// INFO Simulation's constructor is on both JavaFX-Launcher Thread
	}

	/**
	 * Initializes an inner static helper class for Bill Pugh Singleton Pattern
	 * Note: as soon as the instance() method is called the first time, the class is
	 * loaded into memory and an instance gets created. Advantage: it supports
	 * multiple threads calling instance() simultaneously with no synchronized
	 * keyword needed (which slows down the VM)
	 */
	private static class SingletonHelper {
		private static final Simulation INSTANCE = new Simulation();
	}

	/**
	 * Gets a Bill Pugh Singleton instance of the simulation.
	 *
	 * @return Simulation instance
	 */
	public static Simulation instance() {
		// NOTE: Simulation.instance() is accessible on any threads or by any threads
		return SingletonHelper.INSTANCE;
	}

	/**
	 * Prevents the singleton pattern from being destroyed at the time of
	 * serialization
	 *
	 * @return Simulation instance
	 */
	protected Object readResolve() throws ObjectStreamException {
		return instance();
	}

	/**
	 * Checks if the simulation is in a state of creating a new simulation or
	 * loading a saved simulation.
	 *
	 * @return true is simulation is in updating state.
	 */
	public boolean isUpdating() {
		return isUpdating;
	}

	
	public void loadSim() {
		Simulation sim = instance();

		// Destroy old simulation.
		if (sim.initialSimulationCreated) {
			sim.destroyOldSimulation();
		}
	}
	
	
	/**
	 * Creates a new simulation instance.
	 */
	public void createNewSimulation(int timeRatio) {
		isUpdating = true;

		logger.config(Msg.getString("Simulation.log.createNewSim")); //$NON-NLS-1$

		Simulation sim = instance();

		// Destroy old simulation.
		if (sim.initialSimulationCreated) {
			sim.destroyOldSimulation();
		}

		sim.initialSimulationCreated = true;

		// Initialize intransient data members.
		sim.initializeIntransientData(timeRatio);

		// Preserve the build version tag for future build
		// comparison when loading a saved sim
		unitManager.setOriginalBuild(SimulationRuntime.VERSION.getDescription());
		
		// Set this flag to false
		isUpdating = false;
	}

	public void runSocietySim() {
		
		// Create marsClock instance
		masterClock = new MasterClock(simulationConfig, 256);
		
		// Create lunar world instance
		lunarWorld = new LunarWorld(); 
		// Create lunar colony manager instance
		lunarColonyManager = new LunarColonyManager(lunarWorld);
		
		// Create orbit info
		orbitInfo = new OrbitInfo(masterClock, simulationConfig);
		// Create weather
		weather = new Weather(masterClock, orbitInfo);
		
		// Create surface features
		surfaceFeatures = new SurfaceFeatures(masterClock, orbitInfo, weather);
	}
		
	/**
	 * Initializes instance for the maven test.
	 */
	public void testRun() {
		
		ResourceUtil.getInstance().initializeInstances();
		
		simulationConfig = SimulationConfig.instance();
		
		MedicalConfig mc = simulationConfig.getMedicalConfiguration();

		// Should this method call the initialiseTransient method ?

		// Create marsClock instance
		masterClock = new MasterClock(simulationConfig, 256);

		// Set instances for logging
		SimuLoggingFormatter.initializeInstances(masterClock);
		History.initializeInstances(masterClock);
		
		// Create lunar world instance
		lunarWorld = new LunarWorld(); 
		// Create lunar colony manager instance
		lunarColonyManager = new LunarColonyManager(lunarWorld);
		
		// Create orbit info
		orbitInfo = new OrbitInfo(masterClock, simulationConfig);
		// Create weather
		weather = new Weather(masterClock, orbitInfo);
		
		// Create surface features
		surfaceFeatures = new SurfaceFeatures(masterClock, orbitInfo, weather);
		
		unitManager = new UnitManager();
		EquipmentFactory.initialise(unitManager, simulationConfig.getManufactureConfiguration());

		// Initialize OuterSpace instance
		OuterSpace outerSpace = new OuterSpace();
		// Add it to unitManager
		unitManager.addUnit(outerSpace);
		
		// Initialize Moon instance
		Moon moon = new Moon();
		// Add it to unitManager
		unitManager.addUnit(moon);
		
		// Build planetary objects
		MarsSurface marsSurface = new MarsSurface();
		// Add it to unitManager
		unitManager.addUnit(marsSurface);
	
        RoleUtil.initialize();
		GoodsManager.initializeInstances(simulationConfig, missionManager, unitManager);
		
		missionManager = new MissionManager();
		
		medicalManager = new MedicalManager();
		MedicalManager.initializeInstances(mc);

		malfunctionFactory = new MalfunctionFactory();
		MalfunctionManager.initializeInstances(masterClock, malfunctionFactory,
												medicalManager, eventManager,
												simulationConfig.getPartConfiguration());

		// Initialize ScientificStudy
		scientificStudyManager = new ScientificStudyManager(masterClock);
		ScientificStudy.initializeInstances(masterClock, simulationConfig.getScienceConfig());
		// Initialize ScientificStudyUtil
		ScientificStudyUtil.initializeInstances(unitManager);


		Unit.initializeInstances(masterClock, unitManager, weather, missionManager);
		
		LocalAreaUtil.initializeInstances(unitManager, masterClock);
		SalvageValues.initializeInstances(unitManager, masterClock);

		// Initialize instances in Airlock
		Airlock.initializeInstances(unitManager, marsSurface, masterClock);

		eventManager = new HistoricalEventManager(masterClock);
		PhysicalCondition.initializeInstances(masterClock, medicalManager,
							simulationConfig.getPersonConfig(), eventManager);

		BuildingManager.initializeInstances(simulationConfig, masterClock, unitManager);
		ExplorationManager.initialise(surfaceFeatures);

		AbstractMission.initializeInstances(this, eventManager, unitManager,
			surfaceFeatures, missionManager, simulationConfig.getPersonConfig());
		MissionStep.initializeInstances(masterClock, unitManager);

		TaskManager.initializeInstances(this, simulationConfig);

		doneInitializing = true;
	}

	/**
	 * Initializes intransient data in the simulation.
	 */
	private void initializeIntransientData(int timeRatio) {

		// Initialize resources
		ResourceUtil.getInstance().initializeInstances();

		// Gets config file instances
		simulationConfig = SimulationConfig.instance();
		BuildingConfig bc = simulationConfig.getBuildingConfiguration();
		PersonConfig pc = simulationConfig.getPersonConfig();
		CropConfig cc = simulationConfig.getCropConfiguration();
		MedicalConfig mc = simulationConfig.getMedicalConfiguration();
		
		// Clock is always first
		masterClock = new MasterClock(simulationConfig, timeRatio);

		// Set log data
		DataLogger.changeTime(masterClock.getMarsTime());

		// Set instances for logging
		SimuLoggingFormatter.initializeInstances(masterClock);

		// Initialize serializable objects
		malfunctionFactory = new MalfunctionFactory();
	
		// Create lunar world instance
		lunarWorld = new LunarWorld(); 
		// Create lunar colony manager instance
		lunarColonyManager = new LunarColonyManager(lunarWorld);
	
		// Create orbit info
		orbitInfo = new OrbitInfo(masterClock, simulationConfig);
		// Create weather
		weather = new Weather(masterClock, orbitInfo);

		// Create surface features
		surfaceFeatures = new SurfaceFeatures(masterClock, orbitInfo, weather);

		// Initialize MissionManager instance
		missionManager = new MissionManager();

		medicalManager = new MedicalManager();
		MedicalManager.initializeInstances(mc);
		
		eventManager = new HistoricalEventManager(masterClock);
		
		transportManager = new TransportManager(this);
		
		// Initialize UnitManager instance
		unitManager = new UnitManager();
	
		// Initialize OuterSpace instance
		OuterSpace outerSpace = new OuterSpace();
		// Add it to unitManager
		unitManager.addUnit(outerSpace);
		
		// Initialize Moon instance
		Moon moon = new Moon();
		// Add it to unitManager
		unitManager.addUnit(moon);
		
		// Initialize MarsSurface instance
		MarsSurface marsSurface = new MarsSurface();
		// Add it to unitManager
		unitManager.addUnit(marsSurface);
		
		// Add colonies to lunarColonyManager
		lunarColonyManager.addInitColonies();
		
		// Initialize Unit
		Unit.initializeInstances(masterClock, unitManager, weather, missionManager);
	
		PhysicalCondition.initializeInstances(masterClock, medicalManager,
										simulationConfig.getPersonConfig(), eventManager);


		scientificStudyManager = new ScientificStudyManager(masterClock);
		// Re-initialize ScientificStudy
		ScientificStudy.initializeInstances(masterClock, simulationConfig.getScienceConfig());
		// Re-initialize ScientificStudyUtil
		ScientificStudyUtil.initializeInstances(unitManager);
		
        // Initialize RoleUtil
        new RoleUtil();
        // Initialize RoleUtil
        RoleUtil.initialize();
        // Initialize RoleU
		History.initializeInstances(masterClock);
		// Re-initialize Person/Robot related class
		Mind.initializeInstances(missionManager);
		
		EquipmentFactory.initialise(unitManager, simulationConfig.getManufactureConfiguration());

		// Initialize instances in Airlock
		Airlock.initializeInstances(unitManager, marsSurface, masterClock);
		
		AirComposition.initializeInstances(pc);
		
		PowerSource.initializeInstances(surfaceFeatures, orbitInfo, weather);
		
		SolarHeatingSource.initializeInstances(surfaceFeatures);
		// Re-initialize Building function related class
		Function.initializeInstances(bc, masterClock, pc, cc, surfaceFeatures,
								     weather, unitManager);

		Heating.initializeInstances(surfaceFeatures, weather);
		
		AlgaeFarming.initializeInstances(cc);
		
		Crop.initializeInstances(cc);
		
		// Initialize meta tasks
		MetaTaskUtil.initializeMetaTasks();
		
		TaskManager.initializeInstances(this, simulationConfig);
		
		Job.initializeInstances(unitManager, missionManager);
		
		// Initialize instances prior to UnitManager initiation
		MalfunctionManager.initializeInstances(masterClock, malfunctionFactory,
											medicalManager, eventManager,
											simulationConfig.getPartConfiguration());

		Relation.initializeInstances(unitManager);
		
		CreditManager.initializeInstances(unitManager);	
		
		GoodsManager.initializeInstances(simulationConfig, missionManager, unitManager);
		
		//  Re-initialize the GameManager
		GameManager.initializeInstances(unitManager);

		// Set instances for classes that extend Unit and Task and Mission
		AbstractMission.initializeInstances(this, eventManager, unitManager,
				surfaceFeatures, missionManager, pc);	
		MissionStep.initializeInstances(masterClock, unitManager);

		LocalAreaUtil.initializeInstances(unitManager, masterClock);
		
		// Initialize Unit related class
		SalvageValues.initializeInstances(unitManager, masterClock);
		
		BuildingManager.initializeInstances(simulationConfig, masterClock, unitManager);
		ExplorationManager.initialise(surfaceFeatures);
		
		doneInitializing = true;
		
		// Set this flag to false
		isUpdating = false;
	}

	/**
	 *  Recreates a few instances after loading from a saved sim.
	 */
	public void recreateSomeInstances(int userTimeRatio) {
		// Initialize resources
		ResourceUtil.getInstance().initializeInstances();
		// Gets config file instances
		simulationConfig = SimulationConfig.instance();
		// Clock is always first
		masterClock = new MasterClock(simulationConfig, userTimeRatio);
		// Initialize UnitManager instance
		unitManager = new UnitManager();
		// Initialize MissionManager instance
		missionManager = new MissionManager();
	}
	
	/**
	 *  Re-initializes instances after loading from a saved sim.
	 */
	private void reinitializeInstances() {	
		Simulation sim = instance();
		
		simulationConfig = SimulationConfig.instance();
	
		// Re-initialize the resources for the saved sim
		ResourceUtil.getInstance().initializeInstances();

		// Gets config file instances
		BuildingConfig bc = simulationConfig.getBuildingConfiguration();
		PersonConfig pc = simulationConfig.getPersonConfig();
		CropConfig cc = simulationConfig.getCropConfiguration();
		MedicalConfig mc = simulationConfig.getMedicalConfiguration();
		
		// Re-initialize the data logger
		DataLogger.changeTime(masterClock.getMarsTime());
		
		// Set instances for logging
		SimuLoggingFormatter.initializeInstances(masterClock);
		
		// Re-initialize medical manager
		MedicalManager.initializeInstances(mc);
		
		transportManager.reinitalizeInstances(sim);
	
		// Re-initialize the MarsSurface instance
		MarsSurface marsSurface = unitManager.getMarsSurface();
		
		
		// Re-initialize units prior to starting the unit manager
		Unit.initializeInstances(masterClock, unitManager, weather, missionManager);

		PhysicalCondition.initializeInstances(masterClock, medicalManager,
								simulationConfig.getPersonConfig(), eventManager);
		
		// Re-nitialize ScientificStudy
		ScientificStudy.initializeInstances(masterClock, simulationConfig.getScienceConfig());
		// Re-nitialize ScientificStudyUtil
		ScientificStudyUtil.initializeInstances(unitManager);
	
        // Initialize RoleUtil
        new RoleUtil();
        // Initialize RoleUtil
        RoleUtil.initialize();
        // Initialize RoleU
		History.initializeInstances(masterClock);
		// Re-initialize Person/Robot related class
		Mind.initializeInstances(missionManager);
		
		EquipmentFactory.initialise(unitManager, simulationConfig.getManufactureConfiguration());
		// Initialize instances in Airlock
		Airlock.initializeInstances(unitManager, marsSurface, masterClock);
		
		AirComposition.initializeInstances(pc);
		
		PowerSource.initializeInstances(surfaceFeatures, orbitInfo, weather);
		
		SolarHeatingSource.initializeInstances(surfaceFeatures);
		// Re-initialize Building function related class
		Function.initializeInstances(bc, masterClock, pc, cc, surfaceFeatures, weather, unitManager);
		
		Heating.initializeInstances(surfaceFeatures, weather);
		
		AlgaeFarming.initializeInstances(cc);
		
		Crop.initializeInstances(cc);
		
		// Re-initialize the utility class for getting lists of meta tasks.
		MetaTaskUtil.initializeMetaTasks();
		
		TaskManager.initializeInstances(sim, simulationConfig);
		
		Job.initializeInstances(unitManager, missionManager);
		
		MalfunctionManager.initializeInstances(masterClock, malfunctionFactory,
				medicalManager, eventManager,
				simulationConfig.getPartConfiguration());
	
		Relation.initializeInstances(unitManager);
		
		CreditManager.initializeInstances(unitManager);
		
		GoodsManager.initializeInstances(simulationConfig, missionManager, unitManager);
				
		//  Re-initialize the GameManager
		GameManager.initializeInstances(unitManager);
		
		// Re-initialize Mission related class
		AbstractMission.initializeInstances(sim, eventManager, unitManager,
				surfaceFeatures, missionManager, pc);

		LocalAreaUtil.initializeInstances(unitManager, masterClock);
		
		// Re-initialize Unit related class
		SalvageValues.initializeInstances(unitManager, masterClock);
		
		///////////////////////////////////////////////////////////////
		
		// Rediscover the MissionControls
		AuthorityFactory rf  = simulationConfig.getReportingAuthorityFactory();
		rf.discoverReportingAuthorities(unitManager);

		// Re-initialize Structure related class
		BuildingManager.initializeInstances(simulationConfig, masterClock, unitManager);
		ExplorationManager.initialise(surfaceFeatures);
	
		// Start a chain of calls to set instances
		// Warning: must call this at the end of this method
		// after all instances are set
		unitManager.reinit();
		
		doneInitializing = true;

		// Set this flag to false
		isUpdating = false;
	}
		
	/**
	 * Starts the simulation clock.
	 *
	 * @param autosaveDefault True if default is used for autosave
	 */
	public void startClock(boolean autosaveDefault) {
		masterClock.addClockListener(this, 0);
		
		// Add a listener to trigger the auto save
		autoSaveHandler = new AutoSaveTrigger(this, autosaveDefault ? SaveType.AUTOSAVE_AS_DEFAULT : SaveType.AUTOSAVE);
		long autoSaveDuration = simulationConfig.getAutosaveInterval() * 60000L;
		logger.config("Setting up autosave to be triggered every " + autoSaveDuration + " ms (" +
				autoSaveDuration/60.0/1000.0 + " mins).");
		masterClock.addClockListener(autoSaveHandler, autoSaveDuration);
		masterClock.start();
		
		printLastSavedSol();
	}

	/**
	 * Loads a simulation instance from a save file.
	 *
	 * @param file the file to be loaded from.
	 */
	public void loadSimulation(final File file) {
		isUpdating = true;

		File f = file;

		Simulation sim = instance();
		if (f == null) {
			// Try the default file path if file is null.
			f = new File(SimulationRuntime.getSaveDir(), SAVE_FILE + SAVE_FILE_EXTENSION);
		}

		logger.config("The file to be loaded is " + f);

		if (f.exists() && f.canRead()) {

			try {
				sim.readFromFile(f);
			}
			catch (Exception e) {
				logger.log(Level.SEVERE, "Problem loading file: ", e);
			}
		}

		else {
			logger.log(Level.SEVERE, "Quitting mars-sim. The saved sim cannot be read/found.");
			System.exit(1);
		}

					
		// Re-initialize instances
		reinitializeInstances();
	}

    /**
     * Deserializes to Object from given file.
     */
    private void deserialize(File file) throws IOException, ClassNotFoundException {

		FileInputStream in = null;
	    ObjectInputStream ois = null;

		try {
			in = new FileInputStream(file);

			// Stream the file directly into the Object stream to reduce memory
			ois = new ObjectInputStream(new GZIPInputStream(in));

			// Load remaining serialized objects
			lastSaveTimeStamp = (Date) ois.readObject();
			malfunctionFactory = (MalfunctionFactory) ois.readObject();
			lunarWorld = (LunarWorld) ois.readObject();
			lunarColonyManager = (LunarColonyManager) ois.readObject();
			orbitInfo = (OrbitInfo) ois.readObject();
			weather = (Weather) ois.readObject();
			surfaceFeatures = (SurfaceFeatures) ois.readObject();
			missionManager = (MissionManager) ois.readObject();
			medicalManager = (MedicalManager) ois.readObject();
			scientificStudyManager = (ScientificStudyManager) ois.readObject();
			eventManager = (HistoricalEventManager) ois.readObject();
			transportManager = (TransportManager) ois.readObject();
			unitManager = (UnitManager) ois.readObject();
			masterClock = (MasterClock) ois.readObject();
			
			UnitSet.reinit(unitManager);

		} catch (Exception e) {
			logger.log(Level.SEVERE, "Cannot deserialize : " + e.getMessage(), e);
			
		} finally {

			if (ois != null) {
				ois.close();
			}

			if (in != null) {
				in.close();
			}
		}
    }

    /**
     * Computes the size of the file.
     *
     * @param file
     * @return the file size with unit in a string
     */
    private String computeFileSize(File file) {
		// Convert from Bytes to KB
		double fileSize = file.length() / 1000D;
		String s = "";

		if (fileSize > 1000D) {
			fileSize = fileSize / 1_000.0;
			s = " MB";
		}
		else {
			s = " KB";
		}

		return Math.round(fileSize * 100.0) / 100.0 + s;
    }

	/**
	 * Reads a serialized simulation from a file.
	 *
	 * @param file the saved serialized simulation.
	 * @throws ClassNotFoundException if error reading serialized classes.
	 * @throws IOException            if error reading from file.
	 */
	private void readFromFile(File file) throws ClassNotFoundException, IOException {
		logger.config("Loading and processing the saved sim. Please wait...");

		String filename = file.getName();
		String path = file.getPath().replace(filename, "");

		// Deserialize the file
		deserialize(file);

		// Get the current build
		String currentBuild = SimulationRuntime.VERSION.getDescription();
		// Load the previous saved sim's build
		String loadBuild = unitManager.getOriginalBuild();
		
		if (loadBuild == null)
			loadBuild = "unknown";

		logger.config(" ");
		logger.config("                   Info on Saved Simulation                      ");
		logger.config(DASHES);
		logger.config("                   Filename : " + filename);
		logger.config("                       Path : " + path);
		logger.config("                       Size : " + computeFileSize(file));
		logger.config("              Made in Build : " + loadBuild);
		logger.config("  Current Core Engine Build : " + currentBuild);
		if (lastSaveTimeStamp != null)
		logger.config("          System Time Stamp : " + DateFormat.getDateTimeInstance().format(lastSaveTimeStamp));
		logger.config("           Earth Time Stamp : " + masterClock.getEarthTime());
		logger.config("         Martian Time Stamp : " + masterClock.getMarsTime().getDateTimeStamp());

		logger.config(DASHES);
		if (currentBuild.equals(loadBuild)) {
			logger.config(" Note : The core engine uses the same build as the saved sim.");
		} else {
			logger.config(" Note : The core engine does not use the same build as the saved sim.");
			logger.warning("Will attempt to load a simulation made in older build " + loadBuild
				+ " under a newer core engine build " + currentBuild + ".");
		}

		initialSimulationCreated = true;
	}

	/**
	 * Prints the last saved sol if reloading from a saved sim.
	 */
	private void printLastSavedSol() {
		int lastSol = masterClock.getMarsTime().getMissionSol();
		if (lastSol != 1)
			logger.config(" - - - - - - - - - - - - - - Sol " 
				+ lastSol
				+ " (Cont') - - - - - - - - - - - - - - ");
	}


	/**
	 * Saves a simulation instance to a save file.
	 *
	 * @param type
	 * @param file the file to be saved to.
	 * @param callback
	 */
	synchronized void saveSimulation(SaveType type, File file, SimulationListener callback) {

		// Checks to see if the simulation is on pause
		boolean isAlreadyPaused = masterClock.isPaused();

		// Stops the master clock and removes the Simulation clock listener
		masterClock.stop();
		
		if (!isAlreadyPaused) 
			masterClock.setPaused(true, false);

		// Call up garbage collector System.gc(). But it's still up to the gc what it will do.

		lastSaveTimeStamp = new Date();

		Path srcPath = null;
		Path destPath = null;

		// Use type to differentiate in what name/dir it is saved
		switch(type) {
			case AUTOSAVE_AS_DEFAULT, SAVE_DEFAULT:
				file = new File(SimulationRuntime.getSaveDir(), SAVE_FILE + SAVE_FILE_EXTENSION);
	
				if (file.exists() && !file.isDirectory()) {
					FileSystem fileSys = FileSystems.getDefault();
					
					// Create the backup file for storing the previous version of default.sim
					File backupFile = new File(SimulationRuntime.getSaveDir(), "previous" + SAVE_FILE_EXTENSION);

					destPath = fileSys.getPath(backupFile.getPath());
					srcPath = fileSys.getPath(file.getPath());
					
					try {
						// Backup the existing default.sim
						Files.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
					
					}
					catch (IOException ioe) {
						logger.severe("Problem saving simulation " + ioe.getMessage());
					}
				}
	
				logger.config("Saving the simulation as " + SAVE_FILE + SAVE_FILE_EXTENSION + ".");
				break;
			
			case SAVE_AS:
				String f = file.getName();
				String dir = file.getParentFile().getAbsolutePath();
				if (!f.contains(".sim"))
					file = new File(dir, f + SAVE_FILE_EXTENSION);
				logger.config("Saving the simulation as " + file + "...");
				break;
			
			case AUTOSAVE:
				int missionSol = masterClock.getMarsTime().getMissionSol();
				String saveTime = new SystemDateTime().getDateTimeStr();
				String autosaveFilename = saveTime + "_sol" + missionSol + "_r" + SimulationRuntime.VERSION.getShortVersion()
						+ SAVE_FILE_EXTENSION;
				file = new File(SimulationRuntime.getAutoSaveDir(), autosaveFilename);
				logger.config("Autosaving the simulation as " + autosaveFilename + ".");
				
				// NOTE: Should purge old auto saved files
				break;
				
			default:
				break;
		}
	
		// if the autosave/default save directory does not exist, create one now
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}

		boolean success = checkHeapSizeSerialize(type, file, srcPath, destPath);
			
		if (callback != null) {
			callback.eventPerformed(success ? SimulationListener.SAVE_COMPLETED : SimulationListener.SAVE_FAILED);
		}

		// Restarts the master clock and adds back the Simulation clock listener
		if (!isAlreadyPaused) 
			masterClock.setPaused(false, false);
		
		masterClock.start();
	}

	private boolean checkHeapSizeSerialize(SaveType type, File file, Path  srcPath, Path destPath) {
		boolean sucessful = false;
		try {
			// Get maximum size of heap in bytes. The heap cannot grow beyond this size.// Any attempt will result in an OutOfMemoryException.
			long heapMaxSize = Runtime.getRuntime().maxMemory();
			 // Get amount of free memory within the heap in bytes. This size will increase // after garbage collection and decrease as new objects are created.
			long heapFreeSize = Runtime.getRuntime().freeMemory();
	
			logger.config("Heap Max Size: " + formatSize(heapMaxSize)
						+ ", Heap Free Size: " + formatSize(heapFreeSize));
	
				// Save local machine timestamp
			// Serialize the file
			lastSaveTimeStamp = new Date();
			sucessful = serialize(type, file, srcPath, destPath);

			if (sucessful && (type == SaveType.AUTOSAVE)) {
				// Purge old auto backups
				SimulationRuntime.purgeOldFiles( SimulationRuntime.getAutoSaveDir(),
											   simulationConfig.getNumberAutoSaves(), SAVE_FILE_EXTENSION);
			}
		}
		catch (IOException ioe) {
			logger.severe("Problem saving simulation " + ioe.getMessage());
		}

		return sucessful;
	}
	
	/**
	 * Delays for a period of time in millis
	 *
	 * @param millis
	 */
    private static void delay(long millis) {
        try {
			TimeUnit.MILLISECONDS.sleep(millis);
        } catch (InterruptedException e) {
          	logger.log(Level.SEVERE, "Cannot sleep : " + e.getMessage());
          	// Restore interrupted state
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Prints the format for the size of files.
     * 
     * @param v
     * @return
     */
    private static String formatSize(long v) {
        if (v < 1024) return v + " B";
        int z = (63 - Long.numberOfLeadingZeros(v)) / 10;
        return String.format("%.2f %sB", (double)v / (1L << (z*10)), " KMGTPE".charAt(z));
    }

    /**
     * Serializes the given object and save it to a given file.
     * 
     * @return 
     */
    private boolean serialize(SaveType type, File file, Path srcPath, Path destPath)
            throws IOException {
		boolean success = false;
	    ObjectOutputStream oos = new ObjectOutputStream(
	    			new GZIPOutputStream(new FileOutputStream(file)));
		try {

			// Set a delay for 500 millis to avoid java.util.ConcurrentModificationException
			delay(500L);

			// Store the in-transient objects.
			oos.writeObject(lastSaveTimeStamp);
			oos.writeObject(malfunctionFactory);
			oos.writeObject(lunarWorld);
			oos.writeObject(lunarColonyManager);
			oos.writeObject(orbitInfo);
			oos.writeObject(weather);
			oos.writeObject(surfaceFeatures);		
			oos.writeObject(missionManager);
			oos.writeObject(medicalManager);
			oos.writeObject(scientificStudyManager);
			oos.writeObject(eventManager);
			oos.writeObject(transportManager);
			oos.writeObject(unitManager);
			oos.writeObject(masterClock);

			oos.flush();
			oos.close();

			// Print the size of the saved sim
			logger.config("           File size: " + computeFileSize(file));
			logger.config("Done saving. The simulation resumes.");
			success = true;

		} catch (IOException e0) {
			logger.log(Level.SEVERE, "Problem saving simulation", e0); 

			if ((type == SaveType.AUTOSAVE_AS_DEFAULT || type == SaveType.SAVE_DEFAULT) 
				&& file.exists() && !file.isDirectory()) {
				// Backup the existing default.sim
				Files.move(destPath, srcPath, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally {
			if (oos != null)
				oos.close();
			justSaved = true;
		}

		return success;
    }

	/**
	 * Prints the object and its size.
	 * 
	 * @throws IOException
	 */
	public StringBuilder printObjectSize(int type) {
      	StringBuilder sb = new StringBuilder();

		List<Serializable> list = Arrays.asList(
				ResourceUtil.getInstance(),
				malfunctionFactory,
				orbitInfo,
				weather,
				surfaceFeatures,
				missionManager,
				medicalManager,
				scientificStudyManager,
				transportManager,
				eventManager,
				unitManager,
				masterClock
		);

		list.sort((Serializable d1, Serializable d2) -> d1.getClass().getSimpleName().compareTo(d2.getClass().getSimpleName()));

		sb.append("      Serializable object | Serialized Size");
		sb.append("  | Object Size");
		sb.append(System.lineSeparator());
		sb.append(DASHES + System.lineSeparator());
		int max0 = 25;
		int max1 = 10;

		String SPACE = " ";

		double sumFileSize = 0;

		String unit = "";

		masterClock.setPaused(true, false);

		for (Serializable o : list) {
			String name = o.getClass().getSimpleName();
			int size0 = max0 - name.length();
			for (int i=0; i<size0; i++) {
				sb.append(SPACE);
			}
			sb.append(name);
			sb.append(SPACE + ":" + SPACE);

			// Get size
			double fileSize = 0;

			if (type == 0) {
				// Method 1 - Using Outputstream as a Counter
				fileSize = CheckSerializedSize.getSerializedSize(o);

			}
			else if (type == 1) {
				// Method 2 - Using Byte Arrays
				fileSize = CheckSerializedSize.getSerializedSizeByteArray(o);
			}

			sumFileSize += fileSize;

			if (fileSize < 1_000) {
				unit = SPACE + "B" + SPACE;
			}
			else if (fileSize < 1_000_000) {
				fileSize = fileSize/1_000D;
				unit = SPACE + "KB";
			}
			else if (fileSize < 1_000_000_000) {
				fileSize = fileSize/1_000_000D;
				unit = SPACE + "MB";
			}

			String sizeStr = String.format("%.2f", fileSize) + unit;
			int size = max1 - sizeStr.length();
			for (int i=0; i<size; i++) {
				sb.append(SPACE);
			}

			sb.append(sizeStr);

			sb.append(System.lineSeparator());
		}

		// Get the total size
		if (sumFileSize < 1_000D) {
			unit = SPACE + "B" + SPACE;
		}
		else if (sumFileSize < 1_000_000D) {
			sumFileSize = sumFileSize/1_000D;
			unit = SPACE + "KB";
		}
		else if (sumFileSize < 1_000_000_000) {
			sumFileSize = sumFileSize/1_000_000D;
			unit = SPACE + "MB";
		}

		sb.append(DASHES + System.lineSeparator());

		String name = "Total";
		int size0 = max0 - name.length();
		for (int i=0; i<size0; i++) {
			sb.append(SPACE);
		}
		sb.append(name);
		sb.append(SPACE + ":" + SPACE);

		String sizeStr = String.format("%.2f", sumFileSize) + unit;
		int size2 = max1 - sizeStr.length();
		for (int i=0; i<size2; i++) {
			sb.append(SPACE);
		}

		sb.append(sizeStr + System.lineSeparator());

		masterClock.setPaused(false, false);

		return sb;
	}


	/**
	 * Ends the current simulation.
	 */
	public void endSimulation() {
		logger.log(Level.CONFIG, "Exiting the simulation. Good Bye !");

		instance().stop();
		// Ends the clock listener executor in master clock
		if (masterClock != null)
			masterClock.shutdown();

		// Ends the unitmanager's executor thread pools
		if (unitManager != null) {
			unitManager.endSimulation();
		}
	}

	/**
	 * Stops the simulation.
	 */
	public void stop() {
		if (masterClock != null) {
			masterClock.stop();
			masterClock.removeClockListener(this);
			masterClock.removeClockListener(autoSaveHandler);
		}
	}

	/**
	 * Gets the unit manager.
	 *
	 * @return unit manager
	 */
	public UnitManager getUnitManager() {
		return unitManager;
	}
	
	/**
	 * Gets the lunar world instance.
	 * 
	 * @return
	 */
	public LunarWorld getLunarWorld() {
		return lunarWorld;
	}
	
	/**
	 * Gets the lunar colony manager instance.
	 * 
	 * @return
	 */
	public LunarColonyManager getLunarColonyManager() {
		return lunarColonyManager;
	}

	public OrbitInfo getOrbitInfo() {
		return orbitInfo;
	}
	
	public Weather getWeather() {
		return weather;
	}
	
	public SurfaceFeatures getSurfaceFeatures() {
		return surfaceFeatures;
	}
	
	/**
	 * Gets the mission manager.
	 *
	 * @return mission manager
	 */
	public MissionManager getMissionManager() {
		return missionManager;
	}

	/**
	 * Gets the malfunction factory.
	 *
	 * @return malfunction factory
	 */
	public MalfunctionFactory getMalfunctionFactory() {
		return malfunctionFactory;
	}

	/**
	 * Get the historical event manager.
	 *
	 * @return historical event manager
	 */
	public HistoricalEventManager getEventManager() {
		return eventManager;
	}

	/**
	 * Gets the medical manager.
	 *
	 * @return medical manager
	 */
	public MedicalManager getMedicalManager() {
		return medicalManager;
	}

	/**
	 * Gets the scientific study manager.
	 *
	 * @return scientific study manager.
	 */
	public ScientificStudyManager getScientificStudyManager() {
		return scientificStudyManager;
	}

	/**
	 * Gets the transport manager.
	 *
	 * @return transport manager.
	 */
	public TransportManager getTransportManager() {
		return transportManager;
	}

	/**
	 * Gets the master clock.
	 *
	 * @return master clock
	 */
	public MasterClock getMasterClock() {
		return masterClock;
	}

	public SimulationConfig getConfig() {
		return simulationConfig;
	}
	
	/**
	 * Sets if simulation was loaded with GUI.
	 *
	 * @param value is true if GUI is in use.
	 */
	public void setUseGUI(boolean value) {
		useGUI = value;
	}

	/**
	 * Checks if simulation was loaded with GUI.
	 *
	 * @return true if GUI is in use.
	 */
	public boolean getUseGUI() {
		return useGUI;
	}

	public boolean getJustSaved() {
		return justSaved;
	}

	public void setJustSaved(boolean value) {
		justSaved = value;
	}

	/**
	 * Requests that the simulation is saved on the next idle period.
	 * 
	 * @param saveFile Optional file to save info, null means default file.
	 * @param callback Optional callback when save is completed
	 */
	public void requestSave(File saveFile, SimulationListener callback) {
		logger.log(Level.CONFIG, "Submitting the request for saving the simulation."); 
		savePending = (saveFile == null ? SaveType.SAVE_DEFAULT : SaveType.SAVE_AS);
		savePendingFile = saveFile;	
		saveCallback = callback;	
	}

	/**
	 * Is a save request still pending ?
	 * 
	 * @return
	 */
	public boolean isSavePending() {
		return (savePending != null);
	}
	
	/**
	 * Sends out the clock pulse instance.
	 *
	 * @param pulse the amount of clock pulse passing (in millisols)
	 */
	@Override
	public void clockPulse(ClockPulse pulse) {
		if (doneInitializing && !clockOnPause) {
			// Refresh all Data loggers; this can be refactored later to a Manager class
			DataLogger.changeTime(pulse.getMasterClock().getMarsTime());
			
			// Future: Will call each nation's timePassing(pulse) once per pulse
		
			lunarColonyManager.timePassing(pulse);
			
			orbitInfo.timePassing(pulse);
			
			weather.timePassing(pulse);

			surfaceFeatures.timePassing(pulse);

			unitManager.timePassing(pulse);

			transportManager.timePassing(pulse);
			
			// Pending save
			if (savePending != null) {
				saveSimulation(savePending, savePendingFile, saveCallback);
				saveCallback = null;
				savePending = null;
			}
		}
	}

	@Override
	public void pauseChange(boolean isPaused, boolean showPane) {
        clockOnPause = isPaused;
	}

	/**
	 * Destroys the current simulation to prepare for creating or loading a new
	 * simulation.
	 */
	private void destroyOldSimulation() {
		logger.config("Starting destroyOldSimulation()");

		// Remove old clock listeners ?
		if (masterClock != null) {
			masterClock.removeClockListener(this);
			if (autoSaveHandler != null) {
				masterClock.removeClockListener(autoSaveHandler);
			}
		}
		malfunctionFactory = null;

		if (lunarWorld != null) {
			lunarWorld = null;
		}
		
		if (lunarColonyManager != null) {
			lunarColonyManager = null;
		}
		
		if (orbitInfo != null) {
			orbitInfo = null;
		}
		
		if (weather != null) {
			weather.destroy();
			weather = null;
		}

		if (surfaceFeatures != null) {
			surfaceFeatures.destroy();
			surfaceFeatures = null;
		}

		if (missionManager != null) {
			missionManager.destroy();
			missionManager = null;
		}

		if (medicalManager != null) {
			medicalManager = null;
		}

		logger.config("Done with medicalManager");

		if (masterClock != null) {
			masterClock.destroy();
			masterClock = null;
		}

		if (unitManager != null) {
			unitManager.destroy();
			unitManager = null;
		}

		if (scientificStudyManager != null) {
			scientificStudyManager.destroy();
			scientificStudyManager = null;
		}

		eventManager = null;

		 logger.config("Done with Simulation's destroyOldSimulation()");
	}
}
