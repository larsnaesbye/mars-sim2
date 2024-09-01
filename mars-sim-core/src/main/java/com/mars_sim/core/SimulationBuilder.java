/*
 * Mars Simulation Project
 * SimulationBuilder.java
 * @date 2023-03-31
 * @author Barry Evans
 */
package com.mars_sim.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.Runtime.Version;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

import com.mars_sim.core.authority.Authority;
import com.mars_sim.core.configuration.Scenario;
import com.mars_sim.core.configuration.ScenarioConfig;
import com.mars_sim.core.configuration.UserConfigurableConfig;
import com.mars_sim.core.logging.DiagnosticsManager;
import com.mars_sim.core.map.common.FileLocator;
import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.person.Crew;
import com.mars_sim.core.person.CrewConfig;
import com.mars_sim.core.structure.InitialSettlement;
import com.mars_sim.core.structure.SettlementBuilder;
import com.mars_sim.core.structure.SettlementConfig;
import com.mars_sim.core.structure.SettlementTemplate;
import com.mars_sim.core.tool.RandomUtil;

/*
 * This class is a Factory to bootstrap a new simulation according to
 * he various options.
 */
public class SimulationBuilder {
	private static final String NEW_ARG = "new";
	private static final String TIMERATIO_ARG = "timeratio";
	private static final String TEMPLATE_ARG = "template";
	private static final String DATADIR_ARG = "datadir";  
	private static final String BASEURL_ARG = "baseurl";  
	private static final String SPONSOR_ARG = "sponsor";
	private static final String LATITUDE_ARG = "lat";
	private static final String LONGITUDE_ARG = "lon";
	private static final String CREW_ARG = "crew";
	private static final String DIAGNOSTICS_ARG = "diags";
	private static final String SCENARIO_ARG = "scenario";
	private static final String WHITESPACES = "---------------------------------------------------";
	
	private static final Logger logger = Logger.getLogger(SimulationBuilder.class.getName());
	
	private int userTimeRatio = 0; // zero means not defined
	private String template;
	private String authorityName = null;
	private boolean newAllowed = false;
	private File simFile;
	private String latitude = null;
	private String longitude = null;
	private boolean useCrews = true;
	private UserConfigurableConfig<Crew> crewConfig;
	private Scenario bootstrap;

	public SimulationBuilder() {
		super();
	}

	/**
	 * Sets the time ratio to a specific rate.
	 * 
	 * @param timeRatio
	 */
	private void setTimeRatio(int timeRatio) {
		this.userTimeRatio = timeRatio;
	}

	/**
	 * Sets the loading of the crews.
	 * 
	 * @param useCrew
	 */
	public void setUseCrews(boolean useCrew) {
		this.useCrews = useCrew;
	}
	
	private void setLatitude(String lat) {
		String error = Coordinates.checkLat(lat);
		if (error != null) {
			throw new IllegalArgumentException(error);
		}
		latitude = lat;
	}
	
	private void setLongitude(String lon) {
		String error = Coordinates.checkLon(lon);
		if (error != null) {
			throw new IllegalArgumentException(error);
		}
		longitude = lon;
	}
	
	/**
	 * Sets the name of the template for a single Settlement simulation.
	 * 
	 * @param optionValue
	 */
	public void setTemplate(String optionValue) {
		template = optionValue;
	}

	private void setSponsor(String optionValue) {
		authorityName = optionValue;
	}

	private void setDiagnostics(String modules) {
		try {
			for (String name : modules.split(",")) {
				if (!DiagnosticsManager.setDiagnostics(name.trim(), true)) {
					throw new IllegalArgumentException("Problem with diagnostics module " + name);
				}
			}
		}
		catch (FileNotFoundException e) {
			throw new IllegalArgumentException("Problem with diagnostics file: " + e.getMessage());			
		}
	}
	
	/**
	 * Reload a previous simulation
	 * @param filename
	 */
	public void setSimFile(String filename) {
		if (filename == null) {
			throw new IllegalArgumentException("No simultion file specified");
		}

		if (Paths.get(filename).isAbsolute()) {
			this.simFile = new File(filename);
		}
		else {
			this.simFile = new File(SimulationRuntime.getSaveDir(),
									filename);
		}
	}

	/**
	 * Defines a set of crews to be used.
	 * 
	 * @param crewConfig
	 */
	public void setCrewConfig(UserConfigurableConfig<Crew> crewConfig) {
		this.crewConfig  = crewConfig;
	}

	/**
	 * Sets the scenario for a new simulation.
	 * 
	 * @param scenario
	 */
	public void setScenario(Scenario scenario) {
		this.bootstrap = scenario;
	}
	
	/**
	 * Gets the list of core command line options that are supported by this builder.
	 * 
	 * @return the list
	 */
	public List<Option> getCmdLineOptions() {
		List<Option> options = new ArrayList<>();

		options.add(Option.builder(TIMERATIO_ARG).argName("Ratio (power of 2)").hasArg()
								.desc("Define the time ratio of the simulation").build());
		options.add(Option.builder(DATADIR_ARG).argName("path to data directory").hasArg()
				.desc("Path to the data directory for simulation files (defaults to user.home)").build());
		options.add(Option.builder(BASEURL_ARG).argName("URL to remote content").hasArg()
				.desc("URL to the remote content repository (defaults to master in GitHub)").build());
		
		options.add(Option.builder(NEW_ARG)
						.desc("Create a new simulation if one is not present").build());
		options.add(Option.builder(SCENARIO_ARG).argName("scenario name").hasArg()
				.desc("New simulation from a scenario").build());
		options.add(Option.builder(TEMPLATE_ARG).argName("template name").hasArg()
						.desc("New simulation from a template").build());
		options.add(Option.builder(SPONSOR_ARG).argName(SPONSOR_ARG).hasArg()
						.desc("Set the sponsor for the settlement template").build());		
		options.add(Option.builder(LATITUDE_ARG).argName("latitude").hasArg()
				.desc("Set the latitude of the new template Settlement").build());	
		options.add(Option.builder(LONGITUDE_ARG).argName("longitude").hasArg()
				.desc("Set the longitude of the new template Settlement").build());	
		options.add(Option.builder(CREW_ARG).argName("true|false").hasArg()
				.desc("Enable or disable use of the crews").build());	
		options.add(Option.builder(DIAGNOSTICS_ARG).argName("<module>,<module>.....").hasArg()
				.desc("Enable diagnositics modules").build());	
		return options;
	}

	/**
	 * Parses the command line and process the core Simulation arguments.
	 * 
	 * @param line
	 */
	public void parseCommandLine(CommandLine line) {	

		if (line.hasOption(TIMERATIO_ARG)) {
			setTimeRatio(Integer.parseInt(line.getOptionValue(TIMERATIO_ARG)));
		}
		if (line.hasOption(NEW_ARG)) {
			newAllowed = true;
		}
		if (line.hasOption(TEMPLATE_ARG)) {
			setTemplate(line.getOptionValue(TEMPLATE_ARG));
		}
		if (line.hasOption(SPONSOR_ARG)) {
			setSponsor(line.getOptionValue(SPONSOR_ARG));
		}
		if (line.hasOption(SCENARIO_ARG)) {
			setScenarioName(line.getOptionValue(SCENARIO_ARG));
		}
		if (line.hasOption(LATITUDE_ARG)) {
			setLatitude(line.getOptionValue(LATITUDE_ARG));
		}
		if (line.hasOption(LONGITUDE_ARG)) {
			setLongitude(line.getOptionValue(LONGITUDE_ARG));
		}
		if (line.hasOption(DATADIR_ARG)) {
			SimulationRuntime.setDataDir(line.getOptionValue(DATADIR_ARG));
		}
		if (line.hasOption(BASEURL_ARG)) {
			FileLocator.setBaseURL(line.getOptionValue(BASEURL_ARG));
		}
		if (line.hasOption(CREW_ARG)) {
			setUseCrews(Boolean.parseBoolean(line.getOptionValue(CREW_ARG)));
		}
		if (line.hasOption(DIAGNOSTICS_ARG)) {
			setDiagnostics(line.getOptionValue(DIAGNOSTICS_ARG));
		}		
	}

	/**
	 * Sets the bootstrap Scenario based on the name.
	 * 
	 * @param name
	 */
	private void setScenarioName(String name) {
		ScenarioConfig config = new ScenarioConfig();
		Scenario found = config.getItem(name);
		if (found == null) {
			throw new IllegalArgumentException("No scenario named '" + name + "'");
		}
		setScenario(found);
	}

	public void printJavaVersion() {
		Version version = java.lang.Runtime.version();
		logger.config(WHITESPACES);
		logger.config("    Java Version Full String = " + version);
		logger.config("Java Version Feature Element = " + version.feature());
		logger.config("Java Version Interim Element = " + version.interim());
		logger.config("  Java Patch Element Version = " + version.patch());
		logger.config(" Java Update Element Version = " + version.update());
		logger.config("          Java Version Build = " + version.build().orElse(0));
		logger.config("  Java additional build Info = " + version.optional().orElse("None"));
		logger.config("       Java Pre-Release Info = " + version.pre().orElse("NA"));
	}
	
	/**
	 * Uses the previously defines options and start the required Simulation.
	 * 
	 * @return The new simulation started
	 */
	public Simulation start() {
		
		// Load xml files but not until arguments parsed since it may change 
		// the data directory
		SimulationConfig simConfig = SimulationConfig.instance();
		simConfig.loadConfig();
		
		// Initialize storage manager
//		simConfig.createStorageManager();
//		simConfig.createStorageManager().start();
		
		Simulation sim = Simulation.instance();
			
		boolean loaded = false;
		if (simFile != null) {
			loaded = loadSimulation();
		}
		
		InitialSettlement spec = null;
		if (template != null) {
			spec = loadSettlementTemplate(simConfig);
		}
		
		if (!loaded) {
			// Create a new simulation
			sim.createNewSimulation(userTimeRatio); 
			
			SettlementBuilder builder = new SettlementBuilder(sim,
					simConfig);
			if (useCrews && crewConfig == null) {
				crewConfig = new CrewConfig();
			}
			builder.setCrew(crewConfig);
			
			// Is the a specific template requested?
			if (spec !=  null) {
				builder.createFullSettlement(spec);
			}
			else {
				if (bootstrap == null) {
					String defaultName = ScenarioConfig.PREDEFINED_SCENARIOS[0];
					ScenarioConfig config = new ScenarioConfig();
					bootstrap = config.getItem(defaultName);
				}
				builder.createInitialSettlements(bootstrap);
				sim.getTransportManager().loadArrivingSettments(bootstrap,
																simConfig.getSettlementConfiguration(),
																simConfig.getReportingAuthorityFactory());
			}
		}

		if (!loaded) {
			// initialize getTransportManager	
			sim.getTransportManager().init(sim);
		}

		while (true) {
	        try {
				TimeUnit.MILLISECONDS.sleep(1000);
				if (!sim.isUpdating()) {
					logger.config("Starting the Master Clock...");		
					sim.startClock(false);
					break;
				}
	        } catch (InterruptedException e) {
				logger.log(Level.WARNING, "Trouble starting Main Window. ", e); 
				// Restore interrupted state...
			    Thread.currentThread().interrupt();
	        }
		}

		
		return sim;
	}

	/**
	 * Starts the society Simulation.
	 * 
	 */
	public void startSocietySim() {
		printJavaVersion();
		
		// Clock is always first
		Simulation sim = Simulation.instance();
		sim.runSocietySim();
	}
	
	/**
	 * Loads a previously saved simulation.
	 * 
	 * @return true if loaded
	 */
	private boolean loadSimulation() {
		
		boolean result = false;
	
		if (simFile.exists()) {
			if (!simFile.canRead()) {
				throw new IllegalArgumentException("Problem: simulation file can not be opened: " + simFile.getAbsolutePath());
			}
			logger.config("Loading from " + simFile.getAbsolutePath());

			Simulation sim = Simulation.instance();
			
			// Question : Why does it have to create some of the class instances in recreateSomeInstances(), 
			// only later be rewritten in loadSimulation() ?
			sim.recreateSomeInstances(userTimeRatio);
			// Note: if skipping createNewSimulation(), it would not be deserialized correctly
			sim.loadSimulation(simFile);		
			
			result  = true;			

		}
		else if (!newAllowed) {
			// Not allowed to create a new simulation so throw error
			throw new IllegalArgumentException("Problem: simulation file does not exist: " + simFile.getAbsolutePath());			
		}
		return result;
	}

	/**
	 * Loads the prescribed settlement template.
	 * 
	 * @param simulationConfig 
	 * @return InitialSettlement
	 */
	private InitialSettlement loadSettlementTemplate(SimulationConfig simulationConfig) {
		SettlementConfig settlementConfig = simulationConfig.getSettlementConfiguration();
			
		// Find the template
		SettlementTemplate settlementTemplate = settlementConfig.getItem(template);	
		Authority authority;
		if (authorityName == null) {
			// Use the default on the template
			String sponsorCode = settlementTemplate.getSponsor();
			authority = simulationConfig.getReportingAuthorityFactory().getItem(sponsorCode);
		}
		else {
			authority = simulationConfig.getReportingAuthorityFactory().getItem(authorityName);
		}
		
		// Create a random name
		String settlementName = "New Settlement";
		List<String> settlementNames = authority.getSettlementNames();
		if (!settlementNames.isEmpty()) {
			int size = settlementNames.size();
			int rand = RandomUtil.getRandomInt(size-1);
			settlementName = settlementNames.get(rand);
		}
		
		logger.info("Starting a single settlement sim using template '" + template
				+ "' with settlement name '" + settlementName + "'.");
		return new InitialSettlement(settlementName, authority.getName(), template, 
									 settlementTemplate.getDefaultPopulation(),
									 settlementTemplate.getDefaultNumOfRobots(),
									 new Coordinates(latitude, longitude), null);
	}

	/**
	 * Are all the pre-condition defined to start a simulation?
	 * 
	 * @return
	 */
	public boolean isFullyDefined() {
		return (template != null) || (simFile != null)
				|| (bootstrap != null);
	}
}
