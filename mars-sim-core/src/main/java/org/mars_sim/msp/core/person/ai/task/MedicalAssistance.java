/*
 * Mars Simulation Project
 * MedicalAssistance.java
 * @date 2021-10-21
 * @author Barry Evans
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.person.EventType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.JobType;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.person.ai.task.utils.TaskEvent;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.person.health.HealthProblem;
import org.mars_sim.msp.core.person.health.MedicalAid;
import org.mars_sim.msp.core.person.health.Treatment;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;
import org.mars_sim.msp.core.structure.building.function.Storage;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Medical;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.SickBay;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * This class represents a task that requires a person to provide medical help
 * to someone else.
 */
public class MedicalAssistance extends Task implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(MedicalAssistance.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.medicalAssistance"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase TREATMENT = new TaskPhase(Msg.getString("Task.phase.treatment")); //$NON-NLS-1$

	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = .2D;
	private static final double AVERAGE_MEDICAL_WASTE = .1;

	public final static String TOXIC_WASTE = "toxic waste";

	/** The medical station the person is at. */
	private MedicalAid medical;
	/** How long for treatment. */
	private double duration;
	/** Health problem to treat. */
	private HealthProblem problem;

	public static int toxicWasteID = ResourceUtil.toxicWasteID;

	/**
	 * Constructor.
	 * 
	 * @param person the person to perform the task
	 */
	public MedicalAssistance(Person person) {
		super(NAME, person, true, true, STRESS_MODIFIER, SkillType.MEDICINE, 25D);

		// Get a local medical aid that needs work.
		List<MedicalAid> localAids = getNeedyMedicalAids(person);
		if (localAids.size() > 0) {
			int rand = RandomUtil.getRandomInt(localAids.size() - 1);
			medical = localAids.get(rand);

			// Get a curable medical problem waiting for treatment at the medical aid.
			problem = (HealthProblem) medical.getProblemsAwaitingTreatment().get(0);

			// Get the person's medical skill.
			int skill = person.getSkillManager().getEffectiveSkillLevel(SkillType.MEDICINE);

			// Treat medical problem.
			Treatment treatment = problem.getIllness().getRecoveryTreatment();
			setDescription(Msg.getString("Task.description.medicalAssistance.detail", treatment.getName())); // $NON-NLS-1$
			setDuration(treatment.getAdjustedDuration(skill));
			setStressModifier(STRESS_MODIFIER * treatment.getSkill());

			// Start the treatment
			try {
				medical.startTreatment(problem, duration);
				
				logger.log(worker, Level.INFO, 0, "Was treating " + problem.getIllness().getType().getName());

				// Add person to medical care building if necessary.
				if (medical instanceof MedicalCare) {
					// MedicalCare medicalCare = (MedicalCare) medical;
					// Building building = medicalCare.getBuilding();
					// Walk to medical care building.
					// walkToActivitySpotInBuilding(medicalCare.getBuilding(), false);
					Building b = person.getBuildingLocation();
					if (b != null)
						walkToActivitySpotInBuilding(b, FunctionType.MEDICAL_CARE, false);
					else
						endTask();

					// Create starting task event if needed.
					if (getCreateEvents()) {
						TaskEvent startingEvent = new TaskEvent(person, this, problem.getSufferer(),
								EventType.TASK_START, person.getSettlement().getName(), "Provide Medical Assistance");
						registerNewEvent(startingEvent);
					}
					produceMedicalWaste();

				} else if (medical instanceof SickBay) {
					Vehicle vehicle = ((SickBay) medical).getVehicle();
					if (vehicle instanceof Rover) {

						// Walk to rover sick bay activity spot.
						walkToSickBayActivitySpotInRover((Rover) vehicle, false);

						// Create starting task event if needed.
						if (getCreateEvents()) {
							TaskEvent startingEvent = new TaskEvent(person, this, problem.getSufferer(),
									EventType.TASK_START, person.getVehicle().getName(), "Provide Medical Assistance");
							Simulation.instance().getEventManager().registerNewEvent(startingEvent);
						}

						produceMedicalWaste();
					}
				}

			} catch (Exception e) {
				logger.severe(worker, "MedicalAssistance: " + e.getMessage());
				endTask();
			}
		} else {
			endTask();
		}

		// Initialize phase.
		addPhase(TREATMENT);
		setPhase(TREATMENT);
	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (TREATMENT.equals(getPhase())) {
			return treatmentPhase(time);
		} else {
			return time;
		}
	}

	/**
	 * Performs the treatment phase of the task.
	 * 
	 * @param time the amount of time (millisol) to perform the phase.
	 * @return the amount of time (millisol) left over after performing the phase.
	 */
	private double treatmentPhase(double time) {

		// If sickbay owner has malfunction, end task.
		Malfunctionable entity = getMalfunctionable(medical);
		if (entity.getMalfunctionManager().hasMalfunction()) {
			endTask();
		}

		if (isDone()) {
			endTask();
			return time;
		}

		// Check for accident in infirmary.
		checkForAccident(entity, 0.005D, time);

		if (getDuration() <= (getTimeCompleted() + time)) {
			problem.startRecovery();
			endTask();
		}

		// Add experience.
		addExperience(time);

		return 0D;
	}

	/**
	 * Gets the local medical aids that have patients waiting.
	 * 
	 * @return List of medical aids
	 */
	public static List<MedicalAid> getNeedyMedicalAids(Person person) {
		List<MedicalAid> result = new ArrayList<MedicalAid>();

		if (person.isInSettlement()) {
			try {
				Building building = getMedicalAidBuilding(person);
				if (building != null) {
					result.add(building.getMedical());
				}
			} catch (Exception e) {
				logger.severe(person, "MedicalAssistance.getNeedyMedicalAids(): " + e.getMessage());
			}
		} else if (person.isInVehicle()) {
			Vehicle vehicle = person.getVehicle();
			if (vehicle instanceof Medical) {
				MedicalAid aid = ((Medical) vehicle).getSickBay();
				if ((aid != null) && isNeedyMedicalAid(aid)) {
					result.add(aid);
				}
			}
		}

		return result;
	}

	/**
	 * Checks if a medical aid needs work.
	 * 
	 * @return true if medical aid has patients waiting and is not malfunctioning.
	 */
	private static boolean isNeedyMedicalAid(MedicalAid aid) {
		if (aid == null) {
			throw new IllegalArgumentException("aid is null");
		}

		boolean waitingProblems = (aid.getProblemsAwaitingTreatment().size() > 0);
		boolean malfunction = getMalfunctionable(aid).getMalfunctionManager().hasMalfunction();
		return waitingProblems && !malfunction;
	}

	/**
	 * Gets the malfunctionable associated with the medical aid.
	 * 
	 * @param aid The medical aid
	 * @return the associated Malfunctionable
	 */
	private static Malfunctionable getMalfunctionable(MedicalAid aid) {
		Malfunctionable result = null;

		if (aid instanceof SickBay) {
			result = ((SickBay) aid).getVehicle();
		} else if (aid instanceof MedicalCare) {
			result = ((MedicalCare) aid).getBuilding();
		} else {
			result = (Malfunctionable) aid;
		}

		return result;
	}


	/**
	 * Stop medical treatment
	 */
	@Override
	protected void clearDown() {
		// Stop treatment.
		try {
			medical.stopTreatment(problem);
		} catch (Exception e) {
			logger.severe(worker, "MedicalAssistance.endTask(): " + e.getMessage());
		}
	}

	/**
	 * Gets the medical aid the person is using for this task.
	 * 
	 * @return medical aid or null.
	 */
	public MedicalAid getMedicalAid() {
		return medical;
	}

	/**
	 * Gets the least crowded medical care building with a patient that needs
	 * treatment.
	 * 
	 * @param person the person looking for a medical care building.
	 * @return medical care building or null if none found.
	 */
	public static Building getMedicalAidBuilding(Person person) {
		Building result = null;

		if (person.isInSettlement()) {
			Settlement settlement = person.getSettlement();
			BuildingManager manager = settlement.getBuildingManager();
			List<Building> medicalBuildings = manager.getBuildings(FunctionType.MEDICAL_CARE);

			List<Building> needyMedicalBuildings = new ArrayList<Building>();
			Iterator<Building> i = medicalBuildings.iterator();
			while (i.hasNext()) {
				Building building = i.next();
				MedicalCare medical = building.getMedical();
				if (isNeedyMedicalAid(medical)) {
					needyMedicalBuildings.add(building);
				}
			}

			List<Building> bestMedicalBuildings = BuildingManager.getNonMalfunctioningBuildings(needyMedicalBuildings);
			bestMedicalBuildings = BuildingManager.getLeastCrowdedBuildings(bestMedicalBuildings);

			if (bestMedicalBuildings.size() > 0) {
				Map<Building, Double> medBuildingProbs = BuildingManager.getBestRelationshipBuildings(person,
						bestMedicalBuildings);
				result = RandomUtil.getWeightedRandomObject(medBuildingProbs);
			}
		} else {
			throw new IllegalStateException("MedicalAssistance.getMedicalAidBuilding(): Person is not in settlement.");
		}

		return result;
	}

	/**
	 * Checks to see if there is a doctor in the settlement or vehicle the person is
	 * in.
	 * 
	 * @param person the person checking.
	 * @return true if a doctor nearby.
	 */
	public static boolean isThereADoctorInTheHouse(Person person) {
		boolean result = false;

		Iterator<Person> i = null;
		if (person.isInSettlement()) {
			i  = person.getSettlement().getIndoorPeople().iterator();
		} else if (person.isInVehicle()) {
			if (person.getVehicle() instanceof Rover) {
				Rover rover = (Rover) person.getVehicle();
				i = rover.getCrew().iterator();
			}
		}

		if (i != null) {
			while (i.hasNext()) {
				Person inhabitant = i.next();
				if ((inhabitant != person) && 
						(inhabitant.getMind().getJob() == JobType.DOCTOR
						|| inhabitant.getMind().getJob() == JobType.PSYCHOLOGIST)
						) {
					result = true;
				}
			}
		}
		return result;
	}

	private void produceMedicalWaste() {
		Unit containerUnit = person.getContainerUnit();
		if (containerUnit.getUnitType() != UnitType.PLANET) {
			Storage.storeAnResource(AVERAGE_MEDICAL_WASTE, toxicWasteID, containerUnit.getSettlement(),
									"MedicalAssistence::produceMedicalWaste");
		}
	}
}
