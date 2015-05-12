/**
 * Mars Simulation Project
 * ChainOfCommand.java
 * @version 3.08 2015-05-12
 * @author Manny Kung
 */

package org.mars_sim.msp.core.structure;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.Role;
import org.mars_sim.msp.core.person.RoleType;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.job.JobManager;

public class ChainOfCommand implements Serializable {
    /** default serial id. */
    private static final long serialVersionUID = 1L;
/*
    int safetySlot = 0;
    int engrSlot = 0;
    int resourceSlot = 0;
	int missionSlot = 0;
	int agriSlot = 0;
	int scienceSlot = 0;
	int logSlot = 0;
*/
    private boolean has7Divisions = false;
    private boolean has3Divisions = false;

    private Map<RoleType, Integer> jobRole;

    private Settlement settlement;

	public ChainOfCommand(Settlement settlement) {
		this.settlement = settlement;
		jobRole = new ConcurrentHashMap<>();
		initializeJobRole();

	}

	public void initializeJobRole() {

	}


    public void assignRole(Job job, Person person, int num) {
    	int safety = getNumFilled(RoleType.SAFETY_SPECIALIST);
    	int resource = getNumFilled(RoleType.RESOURCE_SPECIALIST);
    	int engr = getNumFilled(RoleType.ENGINEERING_SPECIALIST);
    	//System.out.println(person.getName());

    	// fill up a particular role in sequence without considering one's job type
    	if (safety == num - 1) {
        	person.getRole().setNewRoleType(RoleType.SAFETY_SPECIALIST);
        	//System.out.println(person.getRole().toString());
    	}
    	else if (engr == num - 1) {
          	person.getRole().setNewRoleType(RoleType.ENGINEERING_SPECIALIST);
        	//System.out.println(person.getRole().toString());
    	}
    	else if (resource == num - 1) {
        	person.getRole().setNewRoleType(RoleType.RESOURCE_SPECIALIST);
        	//System.out.println(person.getRole().toString());
    	}

 /*
        if (job.equals(JobManager.getJob("Architect")))
        	person.getRole().setRoleType(RoleType.SAFETY_SPECIALIST);
        else if (job.equals(JobManager.getJob("Areologist")))
        	person.getRole().setRoleType(RoleType.ENGINEERING_SPECIALIST);
        else if (job.equals(JobManager.getJob("Astronomer")))
        	person.getRole().setRoleType(RoleType.ENGINEERING_SPECIALIST);
        else if (job.equals(JobManager.getJob("Biologist")))
       		person.getRole().setRoleType(RoleType.RESOURCE_SPECIALIST);
        else if (job.equals(JobManager.getJob("Botanist")))
        	person.getRole().setRoleType(RoleType.RESOURCE_SPECIALIST);
        else if (job.equals(JobManager.getJob("Chef")))
        	person.getRole().setRoleType(RoleType.RESOURCE_SPECIALIST);
        else if (job.equals(JobManager.getJob("Chemist")))
        	person.getRole().setRoleType(RoleType.RESOURCE_SPECIALIST);
        else if (job.equals(JobManager.getJob("Doctor")))
        	person.getRole().setRoleType(RoleType.SAFETY_SPECIALIST);
        else if (job.equals(JobManager.getJob("Driver")))
            person.getRole().setRoleType(RoleType.SAFETY_SPECIALIST);
        else if (job.equals(JobManager.getJob("Engineer")))
        	person.getRole().setRoleType(RoleType.ENGINEERING_SPECIALIST);
        else if (job.equals(JobManager.getJob("Mathematician")))
           	person.getRole().setRoleType(RoleType.ENGINEERING_SPECIALIST);
        else if (job.equals(JobManager.getJob("Meteorologist")))
        	person.getRole().setRoleType(RoleType.SAFETY_SPECIALIST);
        else if (job.equals(JobManager.getJob("Physicist")))
            person.getRole().setRoleType(RoleType.ENGINEERING_SPECIALIST);
        else if (job.equals(JobManager.getJob("Technician")))
           	person.getRole().setRoleType(RoleType.ENGINEERING_SPECIALIST);
        else if (job.equals(JobManager.getJob("Trader")))
           	person.getRole().setRoleType(RoleType.SAFETY_SPECIALIST);
*/
    }

	public void assignSpecialiststo3Divisions(Person person) {
		// if a person has not been assigned a role, he/she will be mission specialist
            Job job = person.getMind().getJob();
            Role role = person.getRole();
            int pop = person.getSettlement().getAllAssociatedPeople().size();
            //int slot = (int) ((pop - 2 - 3 )/ 3);

            boolean allSlotsFilledOnce = areAllFilled(1);

            boolean allSlotsFilledTwice = true;
            if (pop >= 4)
            	allSlotsFilledTwice = areAllFilled(2);

            boolean allSlotsFilledTriple = true;
            if (pop > 8)
            	allSlotsFilledTriple = areAllFilled(3);

            //boolean allSlotsFilledQuad = true;
            //if (pop > 12)
            //	allSlotsFilledQuad = areAllFilled(4);

            //boolean allSlotsFilledPenta = true;
           // if (pop > 24)
            //	allSlotsFilledPenta = areAllFilled(5);


            if (!allSlotsFilledOnce) {
            	//System.out.println("inside if (!allSlotsFilledOnce)");
            	assignRole(job, person, 1);
            }
            else if (!allSlotsFilledTwice) {
               	//System.out.println("inside if (!allSlotsFilledTwice)");
            	assignRole(job, person, 2);
            }
            else if (!allSlotsFilledTriple) {
            	assignRole(job, person, 3);
            }
            //else if (!allSlotsFilledQuad) {
            //	assignRole(job, person, 4);
            //}
            //else if (!allSlotsFilledPenta) {
            //	assignRole(job, person, 5);
            //}
            else {
            	//System.out.println("inside else");
	            //System.out.println("job is " + job.toString());
	            if (job.equals(JobManager.getJob("Architect"))) {
	            	if (RandomUtil.getRandomInt(1, 2) == 1) {
	            		role.setNewRoleType(RoleType.SAFETY_SPECIALIST);
	            	}
	            	else {
	                	role.setNewRoleType(RoleType.ENGINEERING_SPECIALIST);
	            	}
	            }
	            else if (job.equals(JobManager.getJob("Areologist")))
	            	role.setNewRoleType(RoleType.ENGINEERING_SPECIALIST);
	            else if (job.equals(JobManager.getJob("Astronomer")))
	            	role.setNewRoleType(RoleType.ENGINEERING_SPECIALIST);
	            else if (job.equals(JobManager.getJob("Biologist"))) {
	            	if (RandomUtil.getRandomInt(1, 2) == 1)
	            		role.setNewRoleType(RoleType.SAFETY_SPECIALIST);
	            	else
	            		role.setNewRoleType(RoleType.RESOURCE_SPECIALIST);
	            }
	            else if (job.equals(JobManager.getJob("Botanist")))
	            	role.setNewRoleType(RoleType.RESOURCE_SPECIALIST);
	            else if (job.equals(JobManager.getJob("Chef")))
	            	role.setNewRoleType(RoleType.RESOURCE_SPECIALIST);
	            else if (job.equals(JobManager.getJob("Chemist"))) {
	            	if (RandomUtil.getRandomInt(1, 2) == 1)
	                	role.setNewRoleType(RoleType.SAFETY_SPECIALIST);
	            	else
	            		role.setNewRoleType(RoleType.RESOURCE_SPECIALIST);
	            }
	            else if (job.equals(JobManager.getJob("Doctor")))
	            	role.setNewRoleType(RoleType.SAFETY_SPECIALIST);
	            else if (job.equals(JobManager.getJob("Driver")))
	                	role.setNewRoleType(RoleType.SAFETY_SPECIALIST);
	            else if (job.equals(JobManager.getJob("Engineer")))
	            	role.setNewRoleType(RoleType.ENGINEERING_SPECIALIST);
	            else if (job.equals(JobManager.getJob("Mathematician"))){
	            	if (RandomUtil.getRandomInt(1, 2) == 1)
	                	role.setNewRoleType(RoleType.ENGINEERING_SPECIALIST);
	            	else
	                	role.setNewRoleType(RoleType.RESOURCE_SPECIALIST);
	            }
	            else if (job.equals(JobManager.getJob("Meteorologist")))
	            	role.setNewRoleType(RoleType.SAFETY_SPECIALIST);
	            else if (job.equals(JobManager.getJob("Physicist"))) {
	            	if (RandomUtil.getRandomInt(1, 2) == 1)
	                	role.setNewRoleType(RoleType.ENGINEERING_SPECIALIST);
	            	else
	                	role.setNewRoleType(RoleType.SAFETY_SPECIALIST);
	            }
	            else if (job.equals(JobManager.getJob("Technician"))) {
	            	if (RandomUtil.getRandomInt(1, 2) == 1)
	                	role.setNewRoleType(RoleType.ENGINEERING_SPECIALIST);
	            	else
	                	role.setNewRoleType(RoleType.SAFETY_SPECIALIST);
	            }
	            else if (job.equals(JobManager.getJob("Trader"))) {
	            	if (RandomUtil.getRandomInt(1, 2) == 1)
	                	role.setNewRoleType(RoleType.SAFETY_SPECIALIST);
	            	else
	            		role.setNewRoleType(RoleType.RESOURCE_SPECIALIST);
	            }
            }
	}


	public void assignSpecialiststo7Divisions(Person person) {
		// if a person has not been assigned a role, he/she will be mission specialist
/*
            int missionSlot = 0;
            int safetySlot = 0;
            int agriSlot = 0;
            int engrSlot = 0;
            int resourceSlot = 0;
            int scienceSlot = 0;
            int logSlot = 0;
*/
            Job job = person.getMind().getJob();
            Role role = person.getRole();
            int pop = person.getSettlement().getAllAssociatedPeople().size();
            int slot = (int) ((pop - 2 - 7 )/ 7);

            boolean allSlotsFilledOnce = areAllFilled(1);

            boolean allSlotsFilledTwice = true;
            if (pop >= 4)
            	allSlotsFilledTwice = areAllFilled(2);

            boolean allSlotsFilledTriple = true;
            if (pop > 8)
            	allSlotsFilledTriple = areAllFilled(3);

            boolean allSlotsFilledQuad = true;
            if (pop > 12)
            	allSlotsFilledQuad = areAllFilled(4);

            boolean allSlotsFilledPenta = true;
            if (pop > 24)
            	allSlotsFilledPenta = areAllFilled(5);

            if (!allSlotsFilledOnce) {
            	//System.out.println("inside if (!allSlotsFilledOnce)");
            	assignRole(job, person, 1);
            }
            else if (!allSlotsFilledTwice) {
               	//System.out.println("inside if (!allSlotsFilledTwice)");
            	assignRole(job, person, 2);
            }
            else if (!allSlotsFilledTriple) {
            	assignRole(job, person, 3);
            }
            else if (!allSlotsFilledQuad) {
            	assignRole(job, person, 4);
            }
            else if (!allSlotsFilledPenta) {
            	assignRole(job, person, 5);
            }
            else {

	            if (job.equals(JobManager.getJob("Architect"))) {
	            	role.setNewRoleType(RoleType.SAFETY_SPECIALIST);
	            }
	            else if (job.equals(JobManager.getJob("Areologist"))) {
	            	role.setNewRoleType(RoleType.MISSION_SPECIALIST);
	            }
	            else if (job.equals(JobManager.getJob("Astronomer"))) {
	            	role.setNewRoleType(RoleType.SCIENCE_SPECIALIST);
	            }
	            else if (job.equals(JobManager.getJob("Biologist"))) {
	            	if (RandomUtil.getRandomInt(1, 2) == 1) {
	            		role.setNewRoleType(RoleType.AGRICULTURE_SPECIALIST);
	            	}
	            	else {
	            		role.setNewRoleType(RoleType.RESOURCE_SPECIALIST);
	            	}
	            }
	            else if (job.equals(JobManager.getJob("Botanist"))) {
	            	role.setNewRoleType(RoleType.AGRICULTURE_SPECIALIST);
	            }
	            else if (job.equals(JobManager.getJob("Chef"))) {
	            	role.setNewRoleType(RoleType.RESOURCE_SPECIALIST);
	            }
	            else if (job.equals(JobManager.getJob("Chemist"))) {
	            	if (RandomUtil.getRandomInt(1, 2) == 1) {
	            		role.setNewRoleType(RoleType.RESOURCE_SPECIALIST);
	            	}
	            	else {
	                	role.setNewRoleType(RoleType.SCIENCE_SPECIALIST);
	            	}
	            }
	            else if (job.equals(JobManager.getJob("Doctor"))) {
	            	role.setNewRoleType(RoleType.SAFETY_SPECIALIST);
	            }
	            else if (job.equals(JobManager.getJob("Driver"))) {
	            	if (RandomUtil.getRandomInt(1, 2) == 1) {
	                	role.setNewRoleType(RoleType.MISSION_SPECIALIST);
	            	} else {
	                	role.setNewRoleType(RoleType.LOGISTIC_SPECIALIST);
	            	}
	            }
	            else if (job.equals(JobManager.getJob("Engineer"))) {
	            	role.setNewRoleType(RoleType.ENGINEERING_SPECIALIST);
	            }
	            else if (job.equals(JobManager.getJob("Mathematician"))){
	            	if (RandomUtil.getRandomInt(1, 2) == 1) {
	                	role.setNewRoleType(RoleType.MISSION_SPECIALIST);
	            	}
	            	else {
	                	role.setNewRoleType(RoleType.ENGINEERING_SPECIALIST);
	            	}
	            }
	            else if (job.equals(JobManager.getJob("Meteorologist"))) {
	            	if (RandomUtil.getRandomInt(1, 2) == 1) {
	                	role.setNewRoleType(RoleType.SAFETY_SPECIALIST);
	            	}
	            	else {
		            	role.setNewRoleType(RoleType.MISSION_SPECIALIST);
	            	}
	            }
	            else if (job.equals(JobManager.getJob("Physicist"))) {
	            	int num = RandomUtil.getRandomInt(1, 3);
	            	if (num == 1) {
	                	role.setNewRoleType(RoleType.ENGINEERING_SPECIALIST);
	            	}
	            	else if (num == 2)  {
	                	role.setNewRoleType(RoleType.SCIENCE_SPECIALIST);
	            	}
	            	else {
	                	role.setNewRoleType(RoleType.LOGISTIC_SPECIALIST);
	            	}
	            }
	            else if (job.equals(JobManager.getJob("Technician"))) {
	            	if (RandomUtil.getRandomInt(1, 2) == 1) {
	                	role.setNewRoleType(RoleType.ENGINEERING_SPECIALIST);
	            	}
	            	else {
	                	role.setNewRoleType(RoleType.LOGISTIC_SPECIALIST);
	            	}
	            }
	            else if (job.equals(JobManager.getJob("Trader"))) {
	            	if (RandomUtil.getRandomInt(1, 2) == 1) {
	            		role.setNewRoleType(RoleType.RESOURCE_SPECIALIST);
	            	}
	            	else {
		            	role.setNewRoleType(RoleType.LOGISTIC_SPECIALIST);
	            	}
	            }
            }
	}

/*
    public int getSafetySlot() {
    	return safetySlot;
    }

    public int getEngrSlot() {
    	return engrSlot;
    }

    public int getResourceSlot() {
    	return resourceSlot;
    }

    public void addSafety() {
    	safetySlot++;
       	//System.out.println("safetySlot : "+ safetySlot);
    }
*/
    public void addJobRoleMap(RoleType key) {
    	int value = getNumFilled(key);
    	jobRole.put(key, value + 1);
    }

    public void releaseJobRoleMap(RoleType key) {
    	int value = getNumFilled(key);
    	if (value != 0)
    		jobRole.put(key, value - 1);
    	// Check if the job Role released is manager/commander/chief
    	reelect(key);
    }

    public void reelect(RoleType key) {
       	// if the job Role released is manager/commander/chief,
    	// need to elect someone else to fill his place.
    	if (key == RoleType.CHIEF_OF_SUPPLY
    		|| key == RoleType.CHIEF_OF_ENGINEERING
    		|| key == RoleType.CHIEF_OF_SAFETY_N_HEALTH) {
    		Simulation.instance().getUnitManager().electChief(settlement, key);
    	}
    	else if (key == RoleType.COMMANDER
    			|| key == RoleType.SUB_COMMANDER) {
    		int pop = settlement.getAllAssociatedPeople().size();
    		Simulation.instance().getUnitManager().electCommanders(settlement, key, pop);
    	}
    	else if ( key == RoleType.MAYOR) {
    		Simulation.instance().getUnitManager().electMayor(settlement, key);
    	}
    }


    public int getNumFilled(RoleType key) {
    	int value = 0;
    	if (jobRole.containsKey(key))
    		value = jobRole.get(key);
    	 return value;
    }
/*
    public void decrementSafety() {
    	safetySlot--;
    }

    public void setSafetySlot(int value) {
    	safetySlot = value;
    }

    public void addEngr() {
    	engrSlot++;
    }

    public void decrementEngr() {
    	engrSlot--;
       	//System.out.println("engrSlot : "+ engrSlot);
    }

    public void setEngrSlot(int value) {
    	engrSlot = value;
    }

    public void addResource() {
    	resourceSlot++;
       	//System.out.println("resourceSlot : "+ resourceSlot);
    }

    public void decrementResource() {
    	resourceSlot--;
       	//System.out.println("resourceSlot : "+ resourceSlot);
    }

    public void setResourceSlot(int value) {
    	resourceSlot = value;
    }

    public void addScience() {
    	scienceSlot++;
    }

    public void addLogistic() {
    	logSlot++;
    }

    public void addAgri() {
    	agriSlot++;
    }

    public void addMission() {
    	missionSlot++;
    }
*/
    public void set7Divisions(boolean value) {
    	has7Divisions = value;
       	//System.out.println("has7Divisions = " + has7Divisions);
    }

    public boolean areAllFilled(int value) {
    	boolean result = false;
	    if (has3Divisions) {
	    	if (getNumFilled(RoleType.SAFETY_SPECIALIST) >= value
	    		&& getNumFilled(RoleType.ENGINEERING_SPECIALIST) >= value
	    		&& getNumFilled(RoleType.RESOURCE_SPECIALIST) >= value)
	    		result = true;
	       	//System.out.println("result of 3 : "+ result);
	    }
	    else if (has7Divisions) {
	    	if (getNumFilled(RoleType.SAFETY_SPECIALIST) >= value
		    	&& getNumFilled(RoleType.ENGINEERING_SPECIALIST) >= value
		    	&& getNumFilled(RoleType.RESOURCE_SPECIALIST) >= value
		    	&& getNumFilled(RoleType.MISSION_SPECIALIST) >= value
				&& getNumFilled(RoleType.AGRICULTURE_SPECIALIST) >= value
				&& getNumFilled(RoleType.SCIENCE_SPECIALIST) >= value
				&& getNumFilled(RoleType.LOGISTIC_SPECIALIST) >= value)
	    		result = true;
	       	//System.out.println("result of 7 : : "+ result);
	    }
       	//System.out.println("areAllFilled : "+ result);
		return result;
    }

    public void set3Divisions(boolean value) {
    	has3Divisions = value;
       	//System.out.println("has3Divisions = " + has3Divisions);
    }


}
