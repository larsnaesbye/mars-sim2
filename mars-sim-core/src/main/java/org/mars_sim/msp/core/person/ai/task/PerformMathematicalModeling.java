/*
 * Mars Simulation Project
 * PerformMathematicalModeling.java
 * @Date 2021-10-05
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.science.ScientificStudyManager;
import org.mars_sim.msp.core.structure.Lab;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.Research;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * A task for performing mathematical modeling in a laboratory for a scientific study.
 */
public class PerformMathematicalModeling extends Task
implements ResearchScientificStudy, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    /** default logger. */
    private static SimLogger logger = SimLogger.getLogger(PerformMathematicalModeling.class.getName());
  
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.performMathematicalModeling"); //$NON-NLS-1$

    private static final String NO_LAB_SLOT = " can't find a lab slot.";
    
    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = .2D;

    /** Task phases. */
    private static final TaskPhase MODELING = new TaskPhase(Msg.getString(
            "Task.phase.modeling")); //$NON-NLS-1$

    // Data members.
    /** The scientific study the person is modeling for. */
    private ScientificStudy study;
    /** The laboratory the person is working in. */
    private Lab lab;
    /** The lab's associated malfunction manager. */
    private Malfunctionable malfunctions;
    /** The research assistant. */
    private Person researchAssistant;

    private static ScientificStudyManager manager;

    
    /**
     * Constructor.
     * @param person the person performing the task.
     */
    public PerformMathematicalModeling(Person person) {
        // Use task constructor.
        super(NAME, person, true, false, STRESS_MODIFIER,
        		SkillType.MATHEMATICS, 20D, 10D + RandomUtil.getRandomDouble(10D));
        setExperienceAttribute(NaturalAttributeType.ACADEMIC_APTITUDE);
        
//		if (person.getPhysicalCondition().computeFitnessLevel() < 2) {
//			logger.log(person, Level.FINE, 10_000, "Ended performing math modeling. Not feeling well.");
//			endTask();
//		}
		
        // Determine study.
        study = determineStudy();
        if (study != null) {
            lab = getLocalLab(person);
            if (lab != null) {
                addPersonToLab(person);
            }
            
            else {
            	logger.log(person, Level.INFO, 5000, NO_LAB_SLOT);
            	endTask();
            }
            
            // Initialize phase
            addPhase(MODELING);
            setPhase(MODELING);       
        }
        
        else {
        	endTask();
        }

        // Check if person is in a moving rover.
        //if (inMovingRover(person)) {
        //    endTask();
        //}

    }

    /**
     * Gets the crowding modifier for a researcher to use a given laboratory building.
     * @param researcher the researcher.
     * @param lab the laboratory.
     * @return crowding modifier.
     */
    public static double getLabCrowdingModifier(Person researcher, Lab lab)
    {
        double result = 1D;
        if (researcher.isInSettlement()) {
            Building labBuilding = ((Research) lab).getBuilding();
            if (labBuilding != null) {
                result *= Task.getCrowdingProbabilityModifier(researcher, labBuilding);
                result *= Task.getRelationshipModifier(researcher, labBuilding);
            }
        }
        return result;
    }

    /**
     * Determines the scientific study that will be researched.
     * @return study or null if none available.
     */
    private ScientificStudy determineStudy() {
        ScientificStudy result = null;

        List<ScientificStudy> possibleStudies = new ArrayList<ScientificStudy>();

        // Add primary study if mathematics and in research phase.
        if (manager == null)
        	manager = Simulation.instance().getScientificStudyManager();
        ScientificStudy primaryStudy = person.getStudy();
        if (primaryStudy != null) {
            if (ScientificStudy.RESEARCH_PHASE.equals(primaryStudy.getPhase()) &&
                    !primaryStudy.isPrimaryResearchCompleted() &&
                    (ScienceType.MATHEMATICS == primaryStudy.getScience())) {
                // Primary study added twice to double chance of random selection.
                possibleStudies.add(primaryStudy);
                possibleStudies.add(primaryStudy);
            }
        }

        // Add all collaborative studies with mathematics and in research phase.
        for (ScientificStudy collabStudy : person.getCollabStudies()) {
            if (ScientificStudy.RESEARCH_PHASE.equals(collabStudy.getPhase()) &&
                    !collabStudy.isCollaborativeResearchCompleted(person)) {
                ScienceType collabScience = collabStudy.getContribution(person);
                if (ScienceType.MATHEMATICS == collabScience) {
                    possibleStudies.add(collabStudy);
                }
            }
        }

        // Randomly select study.
        if (possibleStudies.size() > 0) {
            int selected = RandomUtil.getRandomInt(possibleStudies.size() - 1);
            result = possibleStudies.get(selected);
        }

        return result;
    }

    /**
     * Gets a local lab for mathematical modeling.
     * @param person the person checking for the lab.
     * @return laboratory found or null if none.
     */
    public static Lab getLocalLab(Person person) {
        Lab result = null;

        if (person.isInSettlement()) {
            result = getSettlementLab(person);
        }
        else if (person.isInVehicle()) {
            result = getVehicleLab(person.getVehicle());
        }

        return result;
    }

    /**
     * Gets a settlement lab for mathematical modeling.
     * @param person the person looking for a lab.
     * @return a valid modeling lab.
     */
    private static Lab getSettlementLab(Person person) {
        Lab result = null;

        //BuildingManager manager = person.getSettlement().getBuildingManager();
        List<Building> labBuildings = person.getSettlement().getBuildingManager().getBuildings(FunctionType.RESEARCH);
        labBuildings = getSettlementLabsWithMathematicsSpeciality(labBuildings);
        labBuildings = BuildingManager.getNonMalfunctioningBuildings(labBuildings);
        labBuildings = getSettlementLabsWithAvailableSpace(labBuildings);
        labBuildings = BuildingManager.getLeastCrowdedBuildings(labBuildings);

        if (labBuildings.size() > 0) {
            Map<Building, Double> labBuildingProbs = BuildingManager.getBestRelationshipBuildings(
                    person, labBuildings);
            //return Building building = RandomUtil.getWeightedRandomObject(labBuildingProbs);
            //result = building.getResearch();
            return RandomUtil.getWeightedRandomObject(labBuildingProbs).getResearch();
        }

        return result;
    }

    /**
     * Gets a list of research buildings with available research space from a list of buildings
     * with the research function.
     * @param buildingList list of buildings with research function.
     * @return research buildings with available lab space.
     */
    private static List<Building> getSettlementLabsWithAvailableSpace(
            List<Building> buildingList) {
        List<Building> result = new ArrayList<Building>();
        for (Building building : buildingList) {
            Research lab = building.getResearch();
            if (lab.getResearcherNum() < lab.getLaboratorySize()) {
                result.add(building);
            }
        }

        return result;
    }

    /**
     * Gets a list of research buildings with mathematics specialty from a list of
     * buildings with the research function.
     * @param buildingList list of buildings with research function.
     * @return research buildings with mathematics specialty.
     */
    private static List<Building> getSettlementLabsWithMathematicsSpeciality(
            List<Building> buildingList) {
        List<Building> result = new ArrayList<Building>();

        for (Building building : buildingList) {
            Research lab = building.getResearch();
            if (lab.hasSpecialty(ScienceType.MATHEMATICS)) {
                result.add(building);
            }
        }

        return result;
    }

    /**
     * Gets an available lab in a vehicle.
     * Returns null if no lab is currently available.
     * @param vehicle the vehicle
     * @return available lab
     */
    private static Lab getVehicleLab(Vehicle vehicle) {

        Lab result = null;

        if (vehicle instanceof Rover) {
            Rover rover = (Rover) vehicle;
            if (rover.hasLab()) {
                Lab lab = rover.getLab();
                boolean availableSpace = (lab.getResearcherNum() < lab.getLaboratorySize());
                boolean specialty = lab.hasSpecialty(ScienceType.MATHEMATICS);
                boolean malfunction = (rover.getMalfunctionManager().hasMalfunction());
                if (availableSpace && specialty && !malfunction) {
                    result = lab;
                }
            }
        }

        return result;
    }

    /**
     * Adds a person to a lab.
     */
    private void addPersonToLab(Person researcher) {

        try {
            
            if (researcher.isInSettlement()) {
                Building labBuilding = ((Research) lab).getBuilding();

                // Walk to lab building.
                walkToResearchSpotInBuilding(labBuilding, false);

                lab.addResearcher();
                malfunctions = labBuilding;
            }
            
            else if (researcher.isInVehicle()) {

                // Walk to lab internal location in rover.
                walkToLabActivitySpotInRover((Rover) researcher.getVehicle(), false);

                lab.addResearcher();
                malfunctions = researcher.getVehicle();
            }
        }
        catch (Exception e) {
        	logger.log(person, Level.SEVERE, 10_000, "Couldn't be added to a lab", e);
        }
    }

    /**
     * Gets the effective mathematical modeling time based on the person's mathematics skill.
     * @param time the real amount of time (millisol) for modeling.
     * @return the effective amount of time (millisol) for modeling.
     */
    private double getEffectiveModelingTime(double time) {
        // Determine effective research time based on the mathematics skill.
        double modelingTime = time;
        int mathematicsSkill = getEffectiveSkillLevel();
        if (mathematicsSkill == 0){
            modelingTime /= 2D;
        }
        else if (mathematicsSkill > 1) {
            modelingTime += modelingTime * (.2D * mathematicsSkill);
        }

        // Modify by tech level of laboratory.
        int techLevel = lab.getTechnologyLevel();
        if (techLevel > 0) {
            modelingTime *= techLevel;
        }

        // If research assistant, modify by assistant's effective skill.
        if (hasResearchAssistant()) {
            //SkillManager manager = researchAssistant.getSkillManager();
            int assistantSkill = researchAssistant.getSkillManager().getEffectiveSkillLevel(SkillType.MATHEMATICS);
            if (mathematicsSkill > 0) {
                modelingTime *= 1D + ((double) assistantSkill / (double) mathematicsSkill);
            }
        }

        return modelingTime;
    }

    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (MODELING.equals(getPhase())) {
            return modelingPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Performs the mathematical modeling phase.
     * @param time the amount of time (millisols) to perform the phase.
     * @return the amount of time (millisols) left over after performing the phase.
     */
    private double modelingPhase(double time) {
        // If person is incapacitated, end task.
        if (person.getPerformanceRating() <= .2) {
            endTask();
        }

		if (person.getPhysicalCondition().computeFitnessLevel() < 2) {
			logger.log(person, Level.FINE, 10_000, "Ended performing math modeling. Not feeling well.");
			endTask();
		}
		
        // Check for laboratory malfunction.
        if (malfunctions.getMalfunctionManager().hasMalfunction()) {
            endTask();
        }

        // Check if research in study is completed.
        boolean isPrimary = study.getPrimaryResearcher().equals(person);

        if (isDone()) {
            return time;
        }

        // Add modeling work time to study.
        double modelingTime = getEffectiveModelingTime(time);
        if (isPrimary) {
            study.addPrimaryResearchWorkTime(modelingTime);
        }
        else {
            study.addCollaborativeResearchWorkTime(person, modelingTime);
        }

        if (isPrimary) {
            if (study.isPrimaryResearchCompleted()) {
    			logger.log(person, Level.INFO, 0, "Just spent " 
    					+ Math.round(study.getPrimaryResearchWorkTimeCompleted() *10.0)/10.0
    					+ " millisols in completing mathematical modeling" 
    					+ " in primary research on " + study.getName() + ".");	
                endTask();
            }
        }
        else {
            if (study.isCollaborativeResearchCompleted(person)) {
    			logger.log(person, Level.INFO, 0, "Just spent " 
    					+ Math.round(study.getCollaborativeResearchWorkTimeCompleted(person) *10.0)/10.0
    					+ " millisols in performing collaborative study on mathematical modeling on " 
    					+ " in " + study.getName() + ".");	
                endTask();
            }
        }
        
        // Add experience
        addExperience(modelingTime);

        // Check for lab accident.
        checkForAccident(malfunctions, 0.001D, time);

        return 0D;
    }

    @Override
    protected void clearDown() {
        // Remove person from lab so others can use it.
        try {
            if (lab != null) {
                lab.removeResearcher();
                lab = null;
            }
        }
        catch(Exception e) {}
    }

    @Override
    public ScienceType getResearchScience() {
        return ScienceType.MATHEMATICS;
    }

    @Override
    public Person getResearcher() {
        return person;
    }

    @Override
    public boolean hasResearchAssistant() {
        return (researchAssistant != null);
    }

    @Override
    public Person getResearchAssistant() {
        return researchAssistant;
    }

    @Override
    public void setResearchAssistant(Person researchAssistant) {
        this.researchAssistant = researchAssistant;
    }
}
