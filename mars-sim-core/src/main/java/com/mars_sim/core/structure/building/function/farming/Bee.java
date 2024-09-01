/*
 * Mars Simulation Project
 * Bee.java
 * @date 2021-10-21
 * @author Manny Kung
 */

package com.mars_sim.core.structure.building.function.farming;

import java.io.Serializable;

import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.tool.RandomUtil;

public class Bee
implements Serializable {
	
	/** default serial id. */
    private static final long serialVersionUID = 1L;
    /** default logger. */
//	private static final Logger logger = Logger.getLogger(Bee.class.getName());

//  private static final FunctionType FUNCTION = FunctionType.FARMING;

    private Settlement settlement;
    private Building building;
    private Farming farm;
    private BeeGrowing beeGrowing;
    private BeeHive hive;
    
    private String beeSpecies;
//    
    private int numOfBees = 1;
    private int numOfUnhatchedEggs = 0;
    private int beeType;  
    private int pollenNeededRate = 100;
    private int health = 100; //  in %, 1 being most healthy
    
    private int fertilityRate = 100; // in %
    private int growthRate = 100; // in %  
    private int deathRate = 0; // in %    

    public Bee(BeeHive hive, int beeType, String beeSpecies) {
    	this.hive = hive;
    	this.beeType = beeType;
    	this.beeSpecies = beeSpecies;
    	
    	this.beeGrowing = hive.getBeeGrowing();
        this.farm = beeGrowing.getFarming();      
        this.building = farm.getBuilding();		
        this.settlement = building.getSettlement();
	}


	public void timePassing(double time) {
		
		if (beeType == 0) {
			fertilityRate += (pollenNeededRate-100) - (100-health);
			//fertilityRate = Math.round(fertilityRate *1000.0)/1000.00;
			//deathRate += 1-health;	
			growthRate = fertilityRate + deathRate;		
			// compute how many eggs will be hatched
			int rand = RandomUtil.getRandomInt(100);
			if (rand < fertilityRate) {
				numOfUnhatchedEggs++;
			}
			
			// compute how many hatched eggs will be male vs. female
			int rand2 = RandomUtil.getRandomInt(4);			
			if (rand2 < 4) {
				numOfUnhatchedEggs--;
				hive.getLarvae().addBee();			
			}		
		}
	}

	public void addBee() {
		numOfBees++;
	}

}
