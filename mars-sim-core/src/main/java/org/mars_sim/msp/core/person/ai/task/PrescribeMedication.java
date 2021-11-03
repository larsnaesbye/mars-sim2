/**
 * Mars Simulation Project
 * PrescribeMedication.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.person.health.AnxietyMedication;
import org.mars_sim.msp.core.person.health.Medication;
import org.mars_sim.msp.core.person.health.RadiationExposure;
import org.mars_sim.msp.core.person.health.RadioProtectiveAgent;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * A task in which a doctor prescribes (and provides) a medication to a patient.
 */
public class PrescribeMedication
extends Task
implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(PrescribeMedication.class.getName());
    
	/** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.prescribeMedication"); //$NON-NLS-1$

	private static final double AVERAGE_MEDICAL_WASTE = .1 * RandomUtil.getRandomDouble(2);

    /** Task phases. */
    private static final TaskPhase MEDICATING = new TaskPhase(Msg.getString(
            "Task.phase.medicating")); //$NON-NLS-1$

	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = .1D;

	// Data members.
	private Person patient = null;
	//private Medication medication = null;

	/**
	 * Constructor.
	 * @param person the person performing the task.
	 */
	public PrescribeMedication(Person person) {
        // Use task constructor.
        super(NAME, person, true, false, STRESS_MODIFIER, SkillType.MEDICINE, 100D, 10D);

        // Determine patient needing medication.
        //if (patient == null)
        patient = determinePatient(person);
        if (patient != null) {
            // Determine medication to prescribe.
            //medication = determineMedication(patient);

            if (person.isOutside())
            	endTask();
            // If in settlement, move doctor to building patient is in.
            else if (person.isInSettlement() && patient.getBuildingLocation() != null) {
                // Walk to patient's building.
//            	patient.getMind().getTaskManager().clearTask();
            	patient.getMind().getTaskManager().addTask(new RequestMedicalTreatment(patient));
            	//walkToActivitySpotInBuilding(patient.getBuildingLocation(), FunctionType.MEDICAL_CARE, false);
                //walkToRandomLocInBuilding(BuildingManager.getBuilding(patient), false);
            }
            else
            	endTask();

        }
        else {
            endTask();
        }

        // Initialize phase
        addPhase(MEDICATING);
        setPhase(MEDICATING);
    }

	public PrescribeMedication(Robot robot) {
        // Use task constructor.
        super(NAME, robot, true, false, STRESS_MODIFIER, SkillType.MEDICINE, 100D, 10D);

        // Determine patient needing medication.
        //if (patient == null)
        patient = determinePatient(robot);
        if (patient != null) {
            // Determine medication to prescribe.
            //medication = determineMedication(patient);

            // If in settlement, move doctor to building patient is in.
            if (robot.isInSettlement() && patient.getBuildingLocation() != null) {
                // Walk to patient's building.
//            	patient.getMind().getTaskManager().clearTask();
            	patient.getMind().getTaskManager().addTask(new RequestMedicalTreatment(patient));
            	//walkToActivitySpotInBuilding(BuildingManager.getBuilding(patient), FunctionType.MEDICAL_CARE, false);
                //walkToRandomLocInBuilding(BuildingManager.getBuilding(patient), false);
            }
            else
            	endTask();
            //logger.info(robot.getName() + " prescribing " + medication.getName() +
            //        " to " + patient.getName());
        }
        else {
            endTask();
        }

        // Initialize phase
        addPhase(MEDICATING);
        setPhase(MEDICATING);
    }


//	   public static int determineNumPatients(Unit doctor) {
//	        int result = 0;
//	        Person p = null;
//	        Robot r = null;
//	        if (doctor instanceof Person)
//	        	p = (Person) doctor;
//	        else
//	        	r = (Robot) doctor;
//	        
//	        // Get possible patient list.
//	        // Note: Doctor can also prescribe medication for himself.
//	        Collection<Person> patientList = null;
//	        
//	        if (p != null) {
//		        if (LocationSituation.IN_SETTLEMENT == p.getLocationSituation()) {
//		            patientList = p.getSettlement().getInhabitants();
//		        }
//		        else if (LocationSituation.IN_VEHICLE == p.getLocationSituation()) {
//		            Vehicle vehicle = p.getVehicle();
//		            if (vehicle instanceof Crewable) {
//		                Crewable crewVehicle = (Crewable) vehicle;
//		                patientList = crewVehicle.getCrew();
//		            }
//		        }
//	        }
//	        
//	        else if (r != null) {
//		        if (LocationSituation.IN_SETTLEMENT == r.getLocationSituation()) {
//		            patientList = r.getSettlement().getInhabitants();
//		        }
//		        else if (LocationSituation.IN_VEHICLE == r.getLocationSituation()) {
//		            Vehicle vehicle = r.getVehicle();
//		            if (vehicle instanceof Crewable) {
//		                Crewable crewVehicle = (Crewable) vehicle;
//		                patientList = crewVehicle.getCrew();
//		            }
//		        }
//	        }
//
//	        // Determine patient.
//	        if (patientList != null) {
//	            Iterator<Person> i = patientList.iterator();
//	            while (i.hasNext()) {
//	                Person person = i.next();
//	                PhysicalCondition condition = person.getPhysicalCondition();
//	                RadiationExposure exposure = condition.getRadiationExposure();
//	                if (!condition.isDead()) {
//	                	if (condition.isStressedOut()) {
//	                        // Only prescribing anti-stress medication at the moment.
//	                        if (!condition.hasMedication(AnxietyMedication.NAME)) {
//	                            result++;
//	                        }
//	                	}
//	                	else if (exposure.isSick()) {
//	                        if (!condition.hasMedication(RadioProtectiveAgent.NAME)) {
//	                            result++;
//	                        }
//	                	}
//	                }
//	            }
//	        }
//
//	        return result;
//	    }

	
    /**
     * Determines if there is a patient nearby needing medication.
     * @param doctor the doctor prescribing the medication.
     * @return patient if one found, null otherwise.
     */
    public Person determinePatient(Person doctor) {
        Person result = null;

        // Get possible patient list.
        // Note: Doctor can also prescribe medication for himself.
        Collection<Person> patientList = null;
        if (doctor.isInSettlement()) {
            patientList = doctor.getSettlement().getIndoorPeople();
        }
        else if (doctor.isInVehicle()) {
            Vehicle vehicle = doctor.getVehicle();
            if (vehicle instanceof Crewable) {
                Crewable crewVehicle = (Crewable) vehicle;
                patientList = crewVehicle.getCrew();
            }
        }

        // Determine patient.
        if (patientList != null) {
            Iterator<Person> i = patientList.iterator();
            while (i.hasNext() && (result == null)) {
                Person person = i.next();
                PhysicalCondition condition = person.getPhysicalCondition();
                RadiationExposure exposure = condition.getRadiationExposure();
                if (!condition.isDead()) {
                	if (condition.isStressedOut()) {
                        // Only prescribing anti-stress medication at the moment.
                        if (!condition.hasMedication(AnxietyMedication.NAME)) {
                            result = person;
                        }
                	}
                	else if (exposure.isSick()) {
                        if (!condition.hasMedication(RadioProtectiveAgent.NAME)) {
                            result = person;
                        }
                	}
                }
            }
        }

        return result;
    }

    public Person determinePatient(Robot doctor) {
        Person result = null;

        // Get possible patient list.
        // Note: Doctor can also prescribe medication for himself.
        Collection<Person> patientList = null;
        if (doctor.isInSettlement()) {
            patientList = doctor.getSettlement().getIndoorPeople();
        }
       
//        else if (loc == LocationSituation.IN_VEHICLE) {
//            Vehicle vehicle = doctor.getVehicle();
//            if (vehicle instanceof Crewable) {
//                Crewable crewVehicle = (Crewable) vehicle;
//                patientList = crewVehicle.getCrew();
//            }
//        }

        // Determine patient.
        if (patientList != null) {
            Iterator<Person> i = patientList.iterator();
            while (i.hasNext() && (result == null)) {
                Person person = i.next();
                PhysicalCondition condition = person.getPhysicalCondition();
                RadiationExposure exposure = condition.getRadiationExposure();
                if (!condition.isDead()) {
                	if (condition.isStressedOut()) {
                        // Only prescribing anti-stress medication at the moment.
                        if (!condition.hasMedication(AnxietyMedication.NAME)) {
                            result = person;
                        }
                	}
                	else if (exposure.isSick()) {
                        if (!condition.hasMedication(RadioProtectiveAgent.NAME)) {
                            result = person;
                        }
                	}
                }
            }
        }

        return result;
    }


//    /**
//     * Determines a medication for the patient.
//     * @param patient the patient to medicate.
//     * @return medication.
//    */ 
//    private Medication determineMedication(Person patient) {
//        // Only allow anti-stress medication for now.
//        return new AnxietyMedication(patient); 
//    }

    
    /**
     * Performs the medicating phase.
     * @param time the amount of time (millisols) to perform the phase.
     * @return the amount of time (millisols) left over after performing the phase.
     */
    private double medicatingPhase(double time) {

        // If duration, provide medication.
        if (getDuration() <= (getTimeCompleted() + time)) {
            if (patient != null && patient.getSettlement() != null && patient.getBuildingLocation() != null) {
               // if (medication != null) {
                    PhysicalCondition condition = patient.getPhysicalCondition();

                    boolean needMeds = false;
                    Medication medication = null;
                    
                    if (condition.isRadiationPoisoned()) {
                    
                    	medication = new RadioProtectiveAgent(patient);                    
	                    // Check if patient already has taken medication.
	                    if (!condition.hasMedication(medication.getName())) {
	                        // Medicate patient.
	                        condition.addMedication(medication);
	                        needMeds = true;              
	                    }
                    }
                    
                    else if (condition.isStressedOut()) {
                    	
                    	medication = new AnxietyMedication(patient);                	
	                    // Check if patient already has taken medication.
	                    if (!condition.hasMedication(medication.getName())) {
	                        // Medicate patient.
	                        condition.addMedication(medication);
	                        needMeds = true;            		
	                    }
                    }
                    
                    if (needMeds) {
                    	StringBuilder phrase = new StringBuilder();
                        
                    	if (!worker.equals(patient)) {
                    		phrase = phrase.append("Prescribing ").append(medication.getName())
                    			.append(" to ").append(patient.getName()).append(" in ").append(patient.getBuildingLocation().getNickName())
                    			.append("."); 
                    	}
                    	else {
                    		phrase = phrase.append("Is self-prescribing ").append(medication.getName())
                        			.append(" to onself in ").append(person.getBuildingLocation().getNickName())
                        			.append("."); 
                    	}
                		logger.log(worker, Level.INFO, 5000,  phrase.toString());
                    }
                    
                    produceMedicalWaste();

                    Building b = patient.getBuildingLocation();
                    if (b != null && b.hasFunction(FunctionType.MEDICAL_CARE))
                    	walkToActivitySpotInBuilding(b, FunctionType.MEDICAL_CARE, false);
                //}
               // else throw new IllegalStateException("medication is null");
            }
            else 
            	logger.info(patient, "Is not in a proper place to receive medication.");
            	//throw new IllegalStateException ("patient is null");
        }

        // Add experience.
        addExperience(time);

        return 0D;
    }


	public void produceMedicalWaste() {
		if (!worker.isOutside()) {
            worker.storeAmountResource(ResourceUtil.toxicWasteID, AVERAGE_MEDICAL_WASTE);
        }
	}


    @Override
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (MEDICATING.equals(getPhase())) {
            return medicatingPhase(time);
        }
        else {
            return time;
        }
    }
}
