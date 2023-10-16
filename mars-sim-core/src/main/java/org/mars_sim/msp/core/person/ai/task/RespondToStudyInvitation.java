/*
 * Mars Simulation Project
 * RespondToStudyInvitation.java
 * @date 2022-06-11
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.person.ai.social.RelationshipUtil;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.ai.task.util.TaskPhase;
import org.mars_sim.msp.core.science.ScienceConfig;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.tools.Msg;
import org.mars_sim.tools.util.RandomUtil;

/**
 * A task for responding to an invitation to collaborate on a scientific study.
 */
public class RespondToStudyInvitation extends Task {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(RespondToStudyInvitation.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.respondToStudyInvitation"); //$NON-NLS-1$

	/** The stress modified per millisol. */
	private static final double STRESS_MODIFIER = 0D;

	/** Duration (millisols) of task. */
	private static final double DURATION = 40D;

	/** Task phases. */
	private static final TaskPhase RESPONDING_INVITATION = new TaskPhase(
			Msg.getString("Task.phase.respondingInvitation")); //$NON-NLS-1$

	/** The scientific study. */
	private ScientificStudy study;

	/**
	 * Constructor
	 *
	 * @param person the person performing the task.
	 */
	public RespondToStudyInvitation(Person person) {
		// Skill determined based on person job type
		super(NAME, person, false, true, STRESS_MODIFIER, null, 25D, DURATION);
		setExperienceAttribute(NaturalAttributeType.ACADEMIC_APTITUDE);

//		if (person.getPhysicalCondition().computeFitnessLevel() < 2) {
//			logger.fine(person, "Ended responding to study invitation. Not feeling well.");
//			endTask();
//		}

		ScienceType scienceType = ScienceType.getJobScience(person.getMind().getJob());
		if (scienceType != null) {
			addAdditionSkill(scienceType.getSkill());
		}

		List<ScientificStudy> invitedStudies = scientificStudyManager.getOpenInvitationStudies(person);
		if (invitedStudies.size() > 0) {
			study = invitedStudies.get(0);

			// If person is in a settlement, try to find an administration building.
			boolean adminWalk = false;
			if (person.isInSettlement()) {
				Building b = BuildingManager.getAvailableBuilding(study, person);
				if (b != null) {
					// Walk to that building.
                	walkToResearchSpotInBuilding(b, false);
					adminWalk = true;
				}
			}

			if (!adminWalk) {

				if (person.isInVehicle()) {
					// If person is in rover, walk to passenger activity spot.
					if (person.getVehicle() instanceof Rover) {
						walkToPassengerActivitySpotInRover((Rover) person.getVehicle(), false);
					}
				} else {
					// Walk to random location.
					walkToRandomLocation(true);
				}
			}
		} else {
			logger.log(person, Level.SEVERE, 0, "Could not find any openly invited studies.");
			endTask();
		}

		// Initialize phase
		addPhase(RESPONDING_INVITATION);
		setPhase(RESPONDING_INVITATION);
	}

	/**
	 * Gets an available administration building that the person can use.
	 *
	 * @param person the person
	 * @return available administration building or null if none.
	 */
	public static Building getAvailableAdministrationBuilding(Person person) {

		Building result = null;

		if (person.isInSettlement()) {
			BuildingManager manager = person.getSettlement().getBuildingManager();
			Set<Building> administrationBuildings = manager.getBuildingSet(FunctionType.ADMINISTRATION);
			administrationBuildings = BuildingManager.getNonMalfunctioningBuildings(administrationBuildings);
			administrationBuildings = BuildingManager.getLeastCrowdedBuildings(administrationBuildings);

			if (!administrationBuildings.isEmpty()) {
				Map<Building, Double> administrationBuildingProbs = BuildingManager.getBestRelationshipBuildings(person,
						administrationBuildings);
				result = RandomUtil.getWeightedRandomObject(administrationBuildingProbs);
			}
		}

		return result;
	}

	/**
	 * Performs the responding to invitation phase.
	 *
	 * @param time the time (millisols) to perform the phase.
	 * @return the remaining time (millisols) after performing the phase.
	 */
	private double respondingToInvitationPhase(double time) {

		if (person.getPhysicalCondition().computeFitnessLevel() < 2) {
			logger.fine(person, "Ended responding to study invitation. Not feeling well.");
			endTask();
			return time;
		}

		if (isDone()) {
			endTask();
			return time;
		}

		// If duration, send respond to invitation.
		if (getDuration() <= (getTimeCompleted() + time)) {

			study.respondingInvitedResearcher(person);

			// LImit how many studies a person can do
			int studyCount = (person.getStudy() != null ? 1 : 0);
			studyCount += person.getCollabStudies().size();
			if (studyCount >= ScienceConfig.getMaxStudies()) {
				logger.warning(person, "Doing too many studies to accept " + study.getName());
				endTask();
				return time;
			}

			JobType job = person.getMind().getJob();

			// Get relationship between invitee and primary researcher.
			Person primaryResearcher = study.getPrimaryResearcher();

			// Decide response to invitation.
			if (decideResponse()) {
				ScienceType science = ScienceType.getJobScience(job);
				study.addCollaborativeResearcher(person, science);

				// Add 5 points to primary researcher's opinion of invitee for accepting
				// invitation.
		        RelationshipUtil.changeOpinion(primaryResearcher, person, RandomUtil.getRandomDouble(2));

				logger.log(person, Level.FINE, 0, "Accepted invitation from " + primaryResearcher.getName()
							+ " to collaborate on "	+ study.getName() + ".");
			} else {

				// Subtract 5 points from primary researcher's opinion of invitee for rejecting
				// invitation.
		        RelationshipUtil.changeOpinion(primaryResearcher, person, RandomUtil.getRandomDouble(-2));

				logger.log(person, Level.FINE, 0, "Rejected invitation from " + primaryResearcher.getName()
							+ " to collaborate on "	+ study.getName() + ".");
			}
		}

		return 0D;
	}

	/**
	 * Decides is the researcher accepts or rejects invitation.
	 *
	 * @return true if accepts, false if rejects.
	 */
	private boolean decideResponse() {
		boolean result = false;

		ScienceType studyScience = study.getScience();
		ScienceType jobScience = ScienceType.getJobScience(person.getMind().getJob());
		if (jobScience != null) {
			boolean isPrimaryScience = studyScience.equals(jobScience);
			boolean isCollaborativeScience = ScienceType.isCollaborativeScience(studyScience, jobScience);
			if (isPrimaryScience || isCollaborativeScience) {
				double acceptChance = 50D;

				// Modify based on study primary researcher's achievement.
				double primaryAchievement = study.getPrimaryResearcher().getScientificAchievement(studyScience);
				acceptChance += primaryAchievement;

//				logger.info("studyScience: " + studyScience.getName() + "    jobScience: " + jobScience
//						+ "    collaborators: " + study.getCollaborativeResearchers().keySet());

				// Modify based on study collaborative researchers' achievements.
				for (Entry<Person, ScienceType> partner : study.getPersonCollaborativePersons().entrySet()) {
						ScienceType collaborativeScience = partner.getValue();
						acceptChance += (partner.getKey().getScientificAchievement(collaborativeScience) / 2D);

				}

				// Modify if researcher's job science is collaborative.
				if (isCollaborativeScience) {
					acceptChance /= 2D;
				}

				// Modify by how many studies researcher is already collaborating on.
				acceptChance /= (person.getCollabStudies().size() + 1D);

				// Modify based on difficulty level of study vs researcher's skill.
				SkillType skill = jobScience.getSkill();
				int skillLevel = person.getSkillManager().getSkillLevel(skill);
				if (skillLevel == 0) {
					skillLevel = 1;
				}
				int difficultyLevel = study.getDifficultyLevel();
				if (difficultyLevel == 0) {
					difficultyLevel = 1;
				}
				acceptChance *= ((double) difficultyLevel / (double) skillLevel);

				// Modify based on researchers opinion of primary researcher.
				double researcherOpinion = RelationshipUtil.getOpinionOfPerson(person, study.getPrimaryResearcher());
				acceptChance *= (researcherOpinion / 50D);

				// Modify based on if researcher and primary researcher are at same settlement.
				Settlement researcherSettlement = person.getAssociatedSettlement();
				Settlement primarySettlement = study.getPrimaryResearcher().getAssociatedSettlement();
				if ((researcherSettlement != null) && researcherSettlement.equals(primarySettlement)) {
					acceptChance *= 2D;
				}

				result = (RandomUtil.getRandomDouble(100D) < acceptChance);
			}
		}

		return result;
	}

	@Override
	protected double performMappedPhase(double time) {
		if (getPhase() == null) {
			throw new IllegalArgumentException("Task phase is null");
		} else if (RESPONDING_INVITATION.equals(getPhase())) {
			return respondingToInvitationPhase(time);
		} else {
			return time;
		}
	}
}
