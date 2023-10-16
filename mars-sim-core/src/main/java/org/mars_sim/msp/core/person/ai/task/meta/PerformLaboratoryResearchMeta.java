/*
 * Mars Simulation Project
 * PerformLaboratoryResearchMeta.java
 * @Date 2021-10-05
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mars_sim.msp.core.data.RatingScore;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.fav.FavoriteType;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.person.ai.role.RoleType;
import org.mars_sim.msp.core.person.ai.task.PerformLaboratoryResearch;
import org.mars_sim.msp.core.person.ai.task.util.FactoryMetaTask;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.ai.task.util.TaskJob;
import org.mars_sim.msp.core.person.ai.task.util.TaskTrait;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.science.ScientificStudy;
import org.mars_sim.msp.core.structure.Lab;
import org.mars_sim.tools.Msg;

/**
 * Meta task for the PerformLaboratoryResearch task.
 */
public class PerformLaboratoryResearchMeta extends FactoryMetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.performLaboratoryResearch"); //$NON-NLS-1$

    public PerformLaboratoryResearchMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.WORK_HOUR);

		setFavorite(FavoriteType.LAB_EXPERIMENTATION, FavoriteType.RESEARCH);
		setTrait(TaskTrait.ACADEMIC);

		// Jobs are the lab technicians and some scientists
		Set<JobType> jobs = new HashSet<>(JobType.SCIENTISTS);
		jobs.add(JobType.MATHEMATICIAN);
		jobs.add(JobType.COMPUTER_SCIENTIST);
		jobs.add(JobType.METEOROLOGIST);
		setPreferredJob(jobs);
		setPreferredRole(RoleType.CHIEF_OF_SCIENCE, RoleType.SCIENCE_SPECIALIST);
	}

    @Override
    public Task constructInstance(Person person) {
        return new PerformLaboratoryResearch(person);
    }

    /**
	 * Assess if this Person can perform any laboratory based research. Find all research
     * perosn is collaborating in
	 * 
	 * @param person the Person to perform the task.
	 * @return List of TasksJob specifications.
	 */
    @Override
	public List<TaskJob> getTaskJobs(Person person) {

        ScientificStudy primaryStudy = person.getStudy();
        if ((primaryStudy == null) || !person.isInSettlement()
            || !person.getPhysicalCondition().isFitByLevel(1000, 70, 1000)) {
        	return EMPTY_TASKLIST;
        }

        // Add probability for researcher's primary study (if any).
        double base = 0D;
        if (ScientificStudy.RESEARCH_PHASE.equals(primaryStudy.getPhase())
                && !primaryStudy.isPrimaryResearchCompleted()) {
            base += getStudyScore(person, 50D, primaryStudy.getScience());
        }

	    // Add probability for each study researcher is collaborating on.
	    for(ScientificStudy collabStudy : person.getCollabStudies()) {
            if (ScientificStudy.RESEARCH_PHASE.equals(collabStudy.getPhase())
                    && !collabStudy.isCollaborativeResearchCompleted(person)) {
                ScienceType collabScience = collabStudy.getContribution(person);

                base += getStudyScore(person, 25D, collabScience);
	        }
        }

        if (base <= 0)
            return EMPTY_TASKLIST;

        var result = new RatingScore(base);
        result.addModifier(GOODS_MODIFIER,
                person.getAssociatedSettlement().getGoodsManager().getResearchFactor());
        result = assessPersonSuitability(result, person);
        return createTaskJobs(result);
    }

    /**
     * Assess the score of doing a study by a person. This involves finding a suitable lab.
     * @param person Being assessed
     * @param base Initial base score
     * @param science Science of the study.
     */
    private double getStudyScore(Person person, double base, ScienceType science) {
        double score = 0D;
        Lab lab = PerformLaboratoryResearch.getLocalLab(person, science);
        if (lab != null) {
            score = base;

            // Get lab building crowding modifier.
            score *= PerformLaboratoryResearch.getLabCrowdingModifier(person, lab);

            // If researcher's current job isn't related to study science, divide by two.
            JobType job = person.getMind().getJob();
            if (job != null) {
                ScienceType jobScience = ScienceType.getJobScience(job);
                if (science != jobScience) {
                    score /= 2D;
                }
            }
        }
        return score;
    }
}
