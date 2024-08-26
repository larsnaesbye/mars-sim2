/*
 * Mars Simulation Project
 * Population.java
 * @date 2023-09-25
 * @author Manny Kung
 */

package com.mars_sim.core.moon;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mars_sim.core.authority.Nation;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.moon.project.ColonyResearcher;
import com.mars_sim.core.moon.project.ColonySpecialist;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.Temporal;
import com.mars_sim.tools.util.RandomUtil;

public class Population implements Serializable, Temporal {

	private static final long serialVersionUID = 1L;

	public static final SimLogger logger = SimLogger.getLogger(Population.class.getName());

	private static final int INITIAL = 60;

	private double numTourists;
	private double numResidents;
	private double numResearchers;
	private double numEngineers;
	
	private double numBeds;
	
	private double growthRateTourists;
	private double growthRateResidents;
	private double growthRateResearchers;
	private double growthRateEngineers;
	
	private double growthRateBeds;
	
	private Colony colony;
	
	private Set<Colonist> colonists = new HashSet<>();
	
	/**
	 * Constructor.
	 * 
	 * @param colony
	 */
	public Population(Colony colony) {
		this.colony = colony;
		
		growthRateEngineers = RandomUtil.getRandomDouble(-.2, .4);
		growthRateTourists = RandomUtil.getRandomDouble(-.3, .4);
		growthRateResidents = RandomUtil.getRandomDouble(-.3, .4);
		growthRateResearchers = RandomUtil.getRandomDouble(-.3, .4);
		growthRateBeds = RandomUtil.getRandomDouble(-.3, .4);
		
		numEngineers = RandomUtil.getRandomInt(5, 10);
		numTourists = RandomUtil.getRandomInt(0, 3);
		numResidents = RandomUtil.getRandomInt(10, 20);
		numResearchers = RandomUtil.getRandomInt(3, 6);
		numEngineers = RandomUtil.getRandomInt(3, 6);
		
		int totPop = getTotalPopulation();
		numBeds = RandomUtil.getRandomInt(totPop, INITIAL);
	}
	
	public void init() {
		for (int i = 0; i < numResearchers; i++) {
			colonists.add(new ColonyResearcher("Researcher" + i, colony));
		}
		for (int i = 0; i < numEngineers; i++) {
			colonists.add(new ColonySpecialist("Engineer" + i, colony));
		}
	}
	
	public Colony getColony() {
		return colony;
	}
	
	@Override
	public boolean timePassing(ClockPulse pulse) {
		
		double researchersCache = numResearchers;
		double engineersCache = numEngineers;
		
		int millisolInt = pulse.getMarsTime().getMillisolInt();
		if (pulse.isNewIntMillisol() && millisolInt > 5 && millisolInt % 60 == 1) {

			// Recalculate tourists growth rate
			growthRateTourists += RandomUtil.getRandomDouble(-.125, .2);
			// Recalculate tourists
			numTourists += growthRateTourists;
			if (numTourists < 0)
				numTourists = 0;
			
			// Recalculate residents growth rate
			growthRateResidents += RandomUtil.getRandomDouble(-.125, .2);
			// Recalculate residents
			numResidents += growthRateResidents;
			if (numResidents < 0)
				numResidents = 0;
			
			// Recalculate researchers growth rate
			growthRateResearchers += RandomUtil.getRandomDouble(-.125, .2) + colony.getResearchDemand();		
			// Recalculate researchers	
			numResearchers += growthRateResearchers;
			if (numResearchers < 0)
				numResearchers = 0;
			
			// Recalculate engineers growth rate
			growthRateEngineers += RandomUtil.getRandomDouble(-.125, .2) + colony.getDevelopmentDemand();		
			// Recalculate engineers	
			numEngineers += growthRateEngineers;
			if (numEngineers < 0)
				numEngineers = 0;

			// Recalculate beds growth rate
			growthRateBeds += RandomUtil.getRandomDouble(-.125, .2);
			// Recalculate beds
			numBeds += growthRateBeds;
			if (numBeds < 0)
				numBeds = 0;

			// Checks if there is enough beds. 
			// If not, slow the growth rate in one type of pop
			if (numTourists + numResidents + numResearchers + numEngineers > numBeds * .95) {
				
				int rand = RandomUtil.getRandomInt(0, 10);
				if (rand == 0) {
					growthRateResidents -= 0.1;
				}
				else if ((rand == 1 || rand == 2)) {
					growthRateResearchers -= 0.1;
				}
				else if ((rand >= 3 || rand <= 5)) {
					growthRateEngineers -= 0.1;
				}
				else {
					growthRateTourists -= 0.1;
				}
			}
					
			// Limit the growth rate
			if (growthRateTourists > 1)
				growthRateTourists = 1;
			else if (growthRateTourists < -.5)
				growthRateTourists = -.5;
			
			if (growthRateResidents > 1)
				growthRateResidents = 1;
			else if (growthRateResidents < -.5)
				growthRateResidents = -.5;
			
			if (growthRateEngineers > 1)
				growthRateEngineers = 1;
			else if (growthRateEngineers < -.5)
				growthRateEngineers = -.5;	
			
			if (growthRateResearchers > 1)
				growthRateResearchers = 1;
			else if (growthRateResearchers < -.5)
				growthRateResearchers = -.5;	
			
			if (growthRateBeds > 2)
				growthRateBeds = 2;
			else if (growthRateBeds < -0.15)
				growthRateBeds = -0.15;
			
			if (numResidents < 0)
				numResidents = 0;
			if (numResearchers < 0)
				numResearchers = 0;
			if (numEngineers < 0)
				numEngineers = 0;
			if (numTourists < 0)
				numTourists = 0;
			
			if ((int)researchersCache < (int)numResearchers 
					&& !colonists.isEmpty()) {
				removeOneResearcher();
			}
			else if ((int)researchersCache > (int)numResearchers) {
				addOneResearcher();
			}
			// else if they are equal, then no change
			
			if ((int)engineersCache < (int)numEngineers 
					&& !colonists.isEmpty()) {
				removeOneEngineer();
			}
			else if ((int)engineersCache > (int)numEngineers) {
				addOneEngineer();
			}
			// else if they are equal, then no change
			
		}
		
		for (Colonist c: colonists) {
			if (c instanceof ColonyResearcher r) {
				r.timePassing(pulse);
			} else if (c instanceof ColonySpecialist r) {
				r.timePassing(pulse);
			}
		}
		
		return false;
	}
	
	
	/**
	 * Adds a researcher.
	 */
	public void addOneResearcher() {
		Nation nation = colony.getNation();
		
		if (nation == null) {
			colonists.add(new ColonyResearcher("R" 
					+ (int)numResearchers, colony));
		}
		else {
			Colonist colonist = nation.getOneColonist();
			if (colonist != null) {
				colonists.add(colonist);
			}
			else {
				colonists.add(new ColonyResearcher("R" 
					+ (int)numResearchers, colony));
			}
		}
		
		// Pull back the growth rate as a researcher has just been added
		growthRateResearchers = growthRateResearchers * .9;
	}
	
	/**
	 * Adds an engineer.
	 */
	public void addOneEngineer() {
		Nation nation = colony.getNation();
		
		if (nation == null) {
			colonists.add(new ColonySpecialist("E" 
					+ (int)numEngineers, colony));
		}
		else {
			Colonist colonist = nation.getOneColonist();
			if (colonist != null) {
				colonists.add(colonist);
			}
			else {
				colonists.add(new ColonySpecialist("E" 
					+ (int)numEngineers, colony));
			}
		}
		
		// Pull back the growth rate as an engineer has just been added
		growthRateEngineers = growthRateEngineers * .9;
	}
	
	/**
	 * Removes a researcher.
	 */
	public void removeOneResearcher() {
	
		int rand = RandomUtil.getRandomInt(colonists.size() - 1);
	
		List<Colonist> list = new ArrayList<>(colonists);
		
		Colonist c = list.get(rand);
		
		colonists.remove(c);

		Nation nation = colony.getNation();
		
		if (nation == null) {
//			String countryName = colony.getAuthority().getOneCountry();
//			logger.warning("Colony: " + colony.getName() 
//							+ "  Sponsor: " + colony.getAuthority().getName()
//							+ "  Country: " + countryName);
		}
		else {
			// Go back to one's nation pool
			nation.addColonist(c);
			c.setColony(null);	
		}
		
		// Speed up the growth rate as a researcher has just been removed
		growthRateResearchers = growthRateResearchers * .9;		
	}
	
	/**
	 * Removes an engineer.
	 */
	public void removeOneEngineer() {
	
		int rand = RandomUtil.getRandomInt(colonists.size() - 1);
	
		List<Colonist> list = new ArrayList<>(colonists);
		
		Colonist c = list.get(rand);
		
		colonists.remove(c);

		Nation nation = colony.getNation();
		
		if (nation == null) {
//			String countryName = colony.getAuthority().getOneCountry();
//			logger.warning("Colony: " + colony.getName() 
//							+ "  Sponsor: " + colony.getAuthority().getName()
//							+ "  Country: " + countryName);
		}
		else {
			// Go back to one's nation pool
			nation.addColonist(c);
			((ColonySpecialist)c).setColony(null);	
		}
		
		// Speed up the growth rate as an engineer has just been removed
		growthRateEngineers = growthRateEngineers * .9;		
	}
	
	/**
	 * Gets a set of researchers.
	 * 
	 * @return
	 */
	public Set<ColonyResearcher> getResearchers() {
		Set<ColonyResearcher> set = new HashSet<>();
		for (Colonist c: colonists) {
			if (c instanceof ColonyResearcher r) {
				set.add(r);
			}
		}
		return set;
	}
	
	public int getNumBed() {
		return (int)numBeds;
	}
	
	public int getNumTourists() {
		return (int)numTourists;
	}
	
	public int getNumResidents() {
		return (int)numResidents;
	}
	
	public int getNumResearchers() {
		return (int)numResearchers;
	}
	
	public int getNumEngineers() {
		return (int)numEngineers;
	}
	
	public int getTotalPopulation() {
		return (int)numTourists + (int)numResidents + (int)numResearchers + (int)numEngineers;
	}
	
	public double getGrowthNumBed() {
		return growthRateBeds;
	}
	
	public double getGrowthTourists() {
		return growthRateTourists;
	}
	
	public double getGrowthResidents() {
		return growthRateResidents;
	}
	
	public double getGrowthResearchers() {
		return growthRateResearchers;
	}
	
	public double getGrowthEngineers() {
		return growthRateEngineers;
	}
	
	public double getGrowthTotalPopulation() {
		return growthRateTourists * growthRateResidents * growthRateResearchers + growthRateEngineers;
	}
	
	/**
	 * Prepares for deletion.
	 */
	public void destroy() {
		colony = null;
		colonists.clear();
		colonists = null;
	}
}
