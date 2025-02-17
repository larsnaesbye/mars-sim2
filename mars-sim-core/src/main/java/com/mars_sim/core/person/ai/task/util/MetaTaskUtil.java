/*
 * Mars Simulation Project
 * MetaTaskUtil.java
 * @date 2023-06-16
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.task.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.mars_sim.core.Simulation;
import com.mars_sim.core.activities.GroupActivityMetaTask;
import com.mars_sim.core.manufacture.task.ManufacturingMetaTask;
import com.mars_sim.core.person.ai.task.meta.AnalyzeMapDataMeta;
import com.mars_sim.core.person.ai.task.meta.ConnectOnlineMeta;
import com.mars_sim.core.person.ai.task.meta.ConsolidateContainersMeta;
import com.mars_sim.core.person.ai.task.meta.ConstructBuildingMeta;
import com.mars_sim.core.person.ai.task.meta.ConverseMeta;
import com.mars_sim.core.person.ai.task.meta.DelegateWorkMeta;
import com.mars_sim.core.person.ai.task.meta.DoInventoryMeta;
import com.mars_sim.core.person.ai.task.meta.EatDrinkMeta;
import com.mars_sim.core.person.ai.task.meta.ListenToMusicMeta;
import com.mars_sim.core.person.ai.task.meta.MeetTogetherMeta;
import com.mars_sim.core.person.ai.task.meta.PlanMissionMeta;
import com.mars_sim.core.person.ai.task.meta.PlayHoloGameMeta;
import com.mars_sim.core.person.ai.task.meta.ReadMeta;
import com.mars_sim.core.person.ai.task.meta.RecordActivityMeta;
import com.mars_sim.core.person.ai.task.meta.RelaxMeta;
import com.mars_sim.core.person.ai.task.meta.RepairMalfunctionMeta;
import com.mars_sim.core.person.ai.task.meta.ReportMissionControlMeta;
import com.mars_sim.core.person.ai.task.meta.ReviewJobReassignmentMeta;
import com.mars_sim.core.person.ai.task.meta.ReviewMissionPlanMeta;
import com.mars_sim.core.person.ai.task.meta.SalvageBuildingMeta;
import com.mars_sim.core.person.ai.task.meta.SleepMeta;
import com.mars_sim.core.person.ai.task.meta.TeachMeta;
import com.mars_sim.core.person.ai.task.meta.WorkoutMeta;
import com.mars_sim.core.person.ai.task.meta.WriteReportMeta;
import com.mars_sim.core.person.ai.task.meta.YogaMeta;
import com.mars_sim.core.person.ai.task.util.MetaTask.TaskScope;
import com.mars_sim.core.person.ai.task.util.MetaTask.WorkerType;
import com.mars_sim.core.person.health.task.ExamineBodyMeta;
import com.mars_sim.core.person.health.task.PrescribeMedicationMeta;
import com.mars_sim.core.person.health.task.RequestMedicalTreatmentMeta;
import com.mars_sim.core.person.health.task.RestingMedicalRecoveryMeta;
import com.mars_sim.core.person.health.task.SelfTreatHealthProblemMeta;
import com.mars_sim.core.person.health.task.TreatMedicalPatientMeta;
import com.mars_sim.core.resourceprocess.task.ToggleResourceProcessMeta;
import com.mars_sim.core.robot.ai.task.ChargeMeta;
import com.mars_sim.core.robot.ai.task.SavePowerMeta;
import com.mars_sim.core.science.task.AssistScientificStudyResearcherMeta;
import com.mars_sim.core.science.task.CompileScientificStudyResultsMeta;
import com.mars_sim.core.science.task.InviteStudyCollaboratorMeta;
import com.mars_sim.core.science.task.PeerReviewStudyPaperMeta;
import com.mars_sim.core.science.task.PerformLaboratoryResearchMeta;
import com.mars_sim.core.science.task.PerformMathematicalModelingMeta;
import com.mars_sim.core.science.task.ProposeScientificStudyMeta;
import com.mars_sim.core.science.task.RespondToStudyInvitationMeta;
import com.mars_sim.core.science.task.StudyFieldSamplesMeta;
import com.mars_sim.core.structure.building.function.task.CookMealMeta;
import com.mars_sim.core.structure.building.function.task.ObserveAstronomicalObjectsMeta;
import com.mars_sim.core.structure.building.function.task.OptimizeSystemMeta;
import com.mars_sim.core.structure.building.function.task.PrepareDessertMeta;
import com.mars_sim.core.structure.building.function.task.ProduceFoodMeta;
import com.mars_sim.core.structure.building.function.task.TendAlgaePondMeta;
import com.mars_sim.core.structure.building.function.task.TendFishTankMeta;
import com.mars_sim.core.structure.building.function.task.TendGreenhouseMeta;
import com.mars_sim.core.structure.building.function.task.ToggleFuelPowerSourceMeta;
import com.mars_sim.core.structure.building.task.MaintainBuildingMeta;
import com.mars_sim.core.structure.task.BudgetResourcesMeta;
import com.mars_sim.core.structure.task.DigLocalIceMeta;
import com.mars_sim.core.structure.task.DigLocalRegolithMeta;
import com.mars_sim.core.vehicle.task.LoadVehicleMeta;
import com.mars_sim.core.vehicle.task.MaintainVehicleMeta;
import com.mars_sim.core.vehicle.task.ReturnLightUtilityVehicleMeta;
import com.mars_sim.core.vehicle.task.UnloadVehicleMeta;

/**
 * A utility task for getting the list of meta tasks.
 */
public class MetaTaskUtil {

	private static final String EVA = "EVA";
	private static final String INSIDE = "Inside";
	private static final String GARAGE = "Garage";
	
	private static List<FactoryMetaTask> dutyHourTasks = null;
	private static List<FactoryMetaTask> nonDutyHourTasks = null;
	private static List<FactoryMetaTask> onCallTasks = null;

	private static List<FactoryMetaTask> robotMetaTasks = null;

	private static Map<String, MetaTask> idToMetaTask;
	private static List<SettlementMetaTask> settlementTasks;
	private static List<MetaTask> personMetaTasks = null;
	private static List<TaskFactory> personTaskFactorys;

	private static ConverseMeta converseMeta = new ConverseMeta();
	
	/**
	 * Private constructor for utility class.
	 */
	private MetaTaskUtil() {
	}

	/**
	 * Lazy initialisation of metaTasks list.
	 */
	public static synchronized void initializeMetaTasks() {

		if (idToMetaTask != null) {
			// Created by another thread during the wait
			return;
		}
		
		// Would be nice to dynamically load based on what is in the package
		List<MetaTask> allMetaTasks = new ArrayList<>();
		allMetaTasks.add(new AnalyzeMapDataMeta());
		allMetaTasks.add(new AssistScientificStudyResearcherMeta());
		allMetaTasks.add(new BudgetResourcesMeta());
		allMetaTasks.add(new ChargeMeta());
		allMetaTasks.add(new CompileScientificStudyResultsMeta());
		allMetaTasks.add(new ConnectOnlineMeta());
		
		allMetaTasks.add(new ConsolidateContainersMeta());
		allMetaTasks.add(new ConstructBuildingMeta());
		allMetaTasks.add(new CookMealMeta());
		allMetaTasks.add(new DelegateWorkMeta());		
		allMetaTasks.add(new DigLocalIceMeta());
		
		allMetaTasks.add(new DigLocalRegolithMeta());
		allMetaTasks.add(new DoInventoryMeta());
		allMetaTasks.add(new EatDrinkMeta());
		allMetaTasks.add(new ExamineBodyMeta());
		converseMeta = new ConverseMeta();
		allMetaTasks.add(converseMeta);
		allMetaTasks.add(new GroupActivityMetaTask());

		allMetaTasks.add(new InviteStudyCollaboratorMeta());
		
		allMetaTasks.add(new ListenToMusicMeta());
		allMetaTasks.add(new LoadVehicleMeta());
		allMetaTasks.add(new MaintainVehicleMeta());
		allMetaTasks.add(new MaintainBuildingMeta());
		
		allMetaTasks.add(new ManufacturingMetaTask());
		allMetaTasks.add(new MeetTogetherMeta());
		allMetaTasks.add(new ObserveAstronomicalObjectsMeta());
		allMetaTasks.add(new OptimizeSystemMeta());
		allMetaTasks.add(new PeerReviewStudyPaperMeta());
		
		allMetaTasks.add(new PerformLaboratoryResearchMeta());
		allMetaTasks.add(new PerformMathematicalModelingMeta());
		allMetaTasks.add(new PlanMissionMeta());
		allMetaTasks.add(new PlayHoloGameMeta());
		allMetaTasks.add(new SavePowerMeta());
		
		allMetaTasks.add(new PrepareDessertMeta());
		allMetaTasks.add(new PrescribeMedicationMeta());
		allMetaTasks.add(new ProduceFoodMeta());
		allMetaTasks.add(new ProposeScientificStudyMeta());
		allMetaTasks.add(new ReadMeta());
		
		allMetaTasks.add(new RecordActivityMeta());
		allMetaTasks.add(new RelaxMeta());
		allMetaTasks.add(new RepairMalfunctionMeta());
		allMetaTasks.add(new ReportMissionControlMeta());
		allMetaTasks.add(new RequestMedicalTreatmentMeta());
		
		allMetaTasks.add(new RestingMedicalRecoveryMeta());
		allMetaTasks.add(new RespondToStudyInvitationMeta());
		allMetaTasks.add(new ReturnLightUtilityVehicleMeta());
		allMetaTasks.add(new ReviewJobReassignmentMeta());
		allMetaTasks.add(new ReviewMissionPlanMeta());
		
		allMetaTasks.add(new SalvageBuildingMeta());
		allMetaTasks.add(new SelfTreatHealthProblemMeta());
		allMetaTasks.add(new SleepMeta()); 
		allMetaTasks.add(new StudyFieldSamplesMeta());
		
		allMetaTasks.add(new TeachMeta());
		allMetaTasks.add(new TendAlgaePondMeta());
		allMetaTasks.add(new TendFishTankMeta());
		allMetaTasks.add(new TendGreenhouseMeta());
		allMetaTasks.add(new ToggleFuelPowerSourceMeta());
		allMetaTasks.add(new ToggleResourceProcessMeta());
		
		allMetaTasks.add(new TreatMedicalPatientMeta());
		allMetaTasks.add(new UnloadVehicleMeta());
		allMetaTasks.add(new WorkoutMeta());
		allMetaTasks.add(new WriteReportMeta());
		allMetaTasks.add(new YogaMeta());
		
		// Build the name lookup for later
		idToMetaTask = allMetaTasks.stream()
				.collect(Collectors.toMap(MetaTask::getID, Function.identity()));

		// Pick put settlement tasks
		settlementTasks = allMetaTasks.stream()
				.filter(SettlementMetaTask.class::isInstance)
				.map(SettlementMetaTask.class::cast)
				.toList();

		// Filter out All Unit Tasks
		personMetaTasks = allMetaTasks.stream()
				.filter(m -> ((m.getSupported() == WorkerType.BOTH)
								|| (m.getSupported() == WorkerType.PERSON)))
				.toList();
		personTaskFactorys = personMetaTasks.stream()
				.filter(TaskFactory.class::isInstance)
				.map(TaskFactory.class::cast)
				.toList();

		// Filter out All Unit Tasks
		onCallTasks = allMetaTasks.stream()
				.filter(FactoryMetaTask.class::isInstance)
				.map(FactoryMetaTask.class::cast)
				.filter(m -> ((m.getSupported() == WorkerType.BOTH)
								|| (m.getSupported() == WorkerType.PERSON)))
				.toList();
		robotMetaTasks = allMetaTasks.stream()
				.filter(FactoryMetaTask.class::isInstance)
				.map(FactoryMetaTask.class::cast)
				.filter(m -> ((m.getSupported() == WorkerType.BOTH)
								|| (m.getSupported() == WorkerType.ROBOT)))
				.toList();
		
		// Build special Shift based lists
		// Should these be just Person task?
		Map<TaskScope, List<FactoryMetaTask>> metaPerScope = onCallTasks.stream()
  					.collect(Collectors.groupingBy(MetaTask::getScope));

		List<FactoryMetaTask> tasks = new ArrayList<>();
		tasks.addAll(metaPerScope.get(TaskScope.ANY_HOUR));
		tasks.addAll(metaPerScope.get(TaskScope.WORK_HOUR));
		dutyHourTasks = Collections.unmodifiableList(tasks);

		tasks = new ArrayList<>();
		tasks.addAll(metaPerScope.get(TaskScope.ANY_HOUR));
		tasks.addAll(metaPerScope.get(TaskScope.NONWORK_HOUR));
		nonDutyHourTasks = Collections.unmodifiableList(tasks);
	}

	/**
	 * Gets all the known MetaTasks.
	 * 
	 * @return 
	 */
	public static Collection<MetaTask> getAllMetaTasks() {
		return idToMetaTask.values(); 
	}

	/**
	 * Gets a list of all Person meta tasks.
	 * 
	 * @return list of meta tasks.
	 */
	public static List<FactoryMetaTask> getOnCallMetaTasks() {
		return onCallTasks;
	}

	/**
	 * Gets a list of duty meta tasks.
	 * 
	 * @return list of duty meta tasks.
	 */
	public static List<FactoryMetaTask> getDutyHourTasks() {
		return dutyHourTasks;
	}
	
	/**
	 * Gets a list of non-duty meta tasks.
	 * 
	 * @return list of non-duty meta tasks.
	 */
	public static List<FactoryMetaTask> getNonDutyHourTasks() {
		return nonDutyHourTasks;
	}
	
	
	/**
	 * Get a lists of MetaTasks that are applicable for a Settlement.
	 * 
	 * @return List of SettlementMetaTasks
	 */
    public static List<SettlementMetaTask> getSettlementMetaTasks() {
        return settlementTasks;
    }

	/**
	 * Converts a task name in String to Metatask.
	 * 
	 * @return meta tasks.
	 */
	public static MetaTask getMetaTask(String name) {
		return idToMetaTask.get(name.toUpperCase());
	}

	/**
	 * Gets a MetaTask instance that is associated with a Task class.
	 * Note: this method logic is fragile and needs a better solution.
	 * 
	 * @param task
	 * @return
	 */
	public static MetaTask getMetaTypeFromTask(Task task) {
		String s = task.getClass().getSimpleName();
		String ss = s.replace(EVA, "")
				.replace(INSIDE, "")
				.replace(GARAGE, "");
		String metaTaskName = ss.trim();
	
		return getMetaTask(metaTaskName);
	}

	public static List<FactoryMetaTask> getRobotMetaTasks() {
		return robotMetaTasks;
	}

	public static List<MetaTask> getPersonMetaTasks() {
		return personMetaTasks;
	}

    public static List<TaskFactory> getPersonTaskFactorys() {
        return personTaskFactorys;
    }

	public static ConverseMeta getConverseMeta() {
		return converseMeta;
	}
	
	/**
	 * Loads any references that MetaTasks need.
	 */
    static void initialiseInstances(Simulation sim) {
		MetaTask.initialiseInstances(sim);
		ExamineBodyMeta.initialiseInstances(sim.getMedicalManager());
		ReviewMissionPlanMeta.initialiseInstances(sim.getMissionManager());
		ObserveAstronomicalObjectsMeta.initialiseInstances(sim.getScientificStudyManager());
    }

}
