/**
 * Mars Simulation Project
 * TreatMedicalPatientMeta.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.util.Iterator;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.location.LocationStateType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.JobType;
import org.mars_sim.msp.core.person.ai.task.TreatMedicalPatient;
import org.mars_sim.msp.core.person.ai.task.utils.MetaTask;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.TaskTrait;
import org.mars_sim.msp.core.person.health.HealthProblem;
import org.mars_sim.msp.core.person.health.MedicalAid;
import org.mars_sim.msp.core.person.health.Treatment;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.SickBay;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * Meta task for the TreatMedicalPatient task.
 */
public class TreatMedicalPatientMeta extends MetaTask {
    
	private static final int VALUE = 1000;
	
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.treatMedicalPatient"); //$NON-NLS-1$

    public TreatMedicalPatientMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.ANY_HOUR);
		
		setTrait(TaskTrait.MEDICAL);
		setPreferredJob(JobType.MEDICS);
	}
   

    @Override
    public Task constructInstance(Person person) {
        return new TreatMedicalPatient(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;
      
        // Probability affected by the person's stress and fatigue.
//        if (!person.getPhysicalCondition().isFitByLevel(1000, 50, 500))
//        	return 0;
          
        if (person.isInside()) {
	        // Get the local medical aids to use.
	        if (hasNeedyMedicalAids(person)) {
	            result = VALUE;	
	            
	            if (person.isInVehicle()) {	
	    	        // Check if person is in a moving rover.
	    	        if (Vehicle.inMovingRover(person)) {
	    	        	result += -50;
	    	        } 	       
	    	        else
	    	        	result += 50;
	            }
	            
	        }
	
	        result = applyPersonModifier(result, person);
        }
        
        return result;
    }

    /**
     * Checks if there are local medical aids that have people waiting for treatment.
     * @param person the person.
     * @return true if needy medical aids.
     */
    private boolean hasNeedyMedicalAids(Person person) {

        boolean result = false;

		if (person.getLocationStateType() == LocationStateType.INSIDE_SETTLEMENT) {
            result = hasNeedyMedicalAidsAtSettlement(person, person.getSettlement());
        }
        else if (person.getLocationStateType() == LocationStateType.INSIDE_VEHICLE) {
            result = hasNeedyMedicalAidsInVehicle(person, person.getVehicle());
        }

        return result;
    }

    /**
     * Checks if there are medical aids at a settlement that have people waiting for treatment.
     * @param person the person.
     * @param settlement the settlement.
     * @return true if needy medical aids.
     */
    private boolean hasNeedyMedicalAidsAtSettlement(Person person, Settlement settlement) {

        boolean result = false;

        // Check all medical care buildings.
        Iterator<Building> i = settlement.getBuildingManager().getBuildings(
                FunctionType.MEDICAL_CARE).iterator();
        while (i.hasNext() && !result) {
            Building building = i.next();

            // Check if building currently has a malfunction.
            boolean malfunction = building.getMalfunctionManager().hasMalfunction();

            if (!malfunction) {

                // Check if there are any treatable medical problems at building.
                MedicalCare medicalCare = building.getMedical();
                if (hasTreatableHealthProblems(person, medicalCare)) {
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Checks if there are medical aids in a vehicle that have people waiting for treatment.
     * @param person the person.
     * @param vehicle the vehicle.
     * @return true if needy medical aids.
     */
    private boolean hasNeedyMedicalAidsInVehicle(Person person, Vehicle vehicle) {

        boolean result = false;

        if (person.getVehicle() instanceof Rover) {
            Rover rover = (Rover) person.getVehicle();
            if (rover.hasSickBay()) {
                SickBay sickBay = rover.getSickBay();
                if (hasTreatableHealthProblems(person, sickBay)) {
                    result = true;
                }
            }
        }

        return result;
    }

    /**
     * Checks if a medical aid has waiting people with health problems that the person can treat.
     * @param person the person.
     * @param aid the medical aid.
     * @return true if treatable health problems.
     */
    private boolean hasTreatableHealthProblems(Person person, MedicalAid aid) {

        boolean result = false;

        // Get the person's medical skill.
        int skill = person.getSkillManager().getEffectiveSkillLevel(SkillType.MEDICINE);

        // Check if there are any treatable health problems awaiting treatment.
        Iterator<HealthProblem> j = aid.getProblemsAwaitingTreatment().iterator();
        while (j.hasNext() && !result) {
            HealthProblem problem = j.next();
            Treatment treatment = problem.getIllness().getRecoveryTreatment();
            if (treatment != null) {
                int requiredSkill = treatment.getSkill();
                if (skill >= requiredSkill) {
                    result = true;
                }
            }
        }

        return result;
    }
}
