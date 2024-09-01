/*
 * Mars Simulation Project
 * Preference.java
 * @date 2022-07-13
 * @author Manny Kung
 */

package com.mars_sim.core.person.ai.fav;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.mars_sim.core.person.Connection;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.NaturalAttributeManager;
import com.mars_sim.core.person.ai.NaturalAttributeType;
import com.mars_sim.core.person.ai.task.util.MetaTask;
import com.mars_sim.core.person.ai.task.util.MetaTaskUtil;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskTrait;
import com.mars_sim.core.tool.RandomUtil;

/**
 * The Preference class determines the task preferences of a person.
 */
public class Preference implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static final String CONNECT_ONLINE = "Connect Online";
	
	private final int WEIGHT = 2;
	
	/** A map of MetaTasks that can only be done once a day. */
	private Map<MetaTask, Boolean> onceADayMap;
	/** A map of MetaTasks that has been accomplished once a day. */
	private Map<MetaTask, Boolean> taskAccomplishedMap;
	/**  A string map of tasks and preference scores. */
	private Map<String, Integer> scoreStringMap;
	/**  A connection preference map. */
	private Map<Connection, Integer> connectionMap;

	/** The Person instance. */
	private Person person;
	
	
	/**
	 * Constructor.
	 * 
	 * @param person
	 */
	public Preference(Person person) {

		this.person = person;

		// These lookups are all static in terms of the Person so they do not
		// need to use the concurrent list/maps
		scoreStringMap = new HashMap<>();
		taskAccomplishedMap = new HashMap<>();
		onceADayMap = new HashMap<>();
		connectionMap = new HashMap<>();
	}

	/*
	 * Initializes the preference score on each particular task. 
	 * TODO Ideally would be good to move this into the Person.initialise method
	 * but the Favorite.activity has to be defined. Maybe loading of the Favorite
	 * could be move to Person.initialise.
	 */
	public void initializePreference() {

		NaturalAttributeManager naturalAttributeManager = person.getNaturalAttributeManager();

		// Computes the adjustment from a person's natural attributes
		double aa = naturalAttributeManager.getAttribute(NaturalAttributeType.ACADEMIC_APTITUDE) / 50D * 1.5;
		
		double discipline = naturalAttributeManager.getAttribute(NaturalAttributeType.DISCIPLINE) / 50D * 1.5;
		double org = naturalAttributeManager.getAttribute(NaturalAttributeType.ORGANIZATION) / 50D * 1.5;
		
		double t = naturalAttributeManager.getAttribute(NaturalAttributeType.TEACHING) / 50D * 1.5;
		double l = naturalAttributeManager.getAttribute(NaturalAttributeType.LEADERSHIP) / 50D * 1.5;
		double es = (naturalAttributeManager.getAttribute(NaturalAttributeType.ENDURANCE)
				+ naturalAttributeManager.getAttribute(NaturalAttributeType.STRENGTH)) / 100D * 1.5;
		double ag = naturalAttributeManager.getAttribute(NaturalAttributeType.AGILITY) / 500D * 1.5;

		double ss = (naturalAttributeManager.getAttribute(NaturalAttributeType.STRESS_RESILIENCE)
				+ naturalAttributeManager.getAttribute(NaturalAttributeType.SPIRITUALITY)) / 100D * 1.5;
		double se = (naturalAttributeManager.getAttribute(NaturalAttributeType.STRESS_RESILIENCE)
				+ naturalAttributeManager.getAttribute(NaturalAttributeType.EMOTIONAL_STABILITY)) / 100D * 1.5;

		double ca = (naturalAttributeManager.getAttribute(NaturalAttributeType.CONVERSATION) / 50D
				+ naturalAttributeManager.getAttribute(NaturalAttributeType.ATTRACTIVENESS) / 200D) * 1.5;

		double art = naturalAttributeManager.getAttribute(NaturalAttributeType.ARTISTRY) / 50D * 1.5;

		double cou = naturalAttributeManager.getAttribute(NaturalAttributeType.COURAGE) / 50D * 1.5;

		// TODO: how to incorporate EXPERIENCE_APTITUDE ?
		int result = 0;
		FavoriteType hobby = person.getFavorite().getFavoriteActivity();
		for (MetaTask metaTask : MetaTaskUtil.getPersonMetaTasks()) {
			// Set them up in random
			double rand = RandomUtil.getRandomDouble(-5, 5);
			
			// Note: the preference score on a metaTask is modified by a person's natural
			// attributes

			// PART 1 : Influenced by FavoriteType 
			Set<FavoriteType> hobbies = metaTask.getFavourites();
			if (hobbies.contains(hobby)) {
				switch (hobby) {
				case ASTRONOMY:
				case LAB_EXPERIMENTATION:
				case RESEARCH:
					rand += 2 * RandomUtil.getRandomDouble(3);
					break;
					
				case COOKING:
				case FIELD_WORK:
				case GAMING:
				case OPERATION:
				case SPORT:
				case TENDING_FARM:
				case TINKERING:
					rand += 1 * RandomUtil.getRandomDouble(3);
					break;

				default:
					
				}
			}
			
			// PART 2 : influenced by natural attribute
			for(TaskTrait trait : metaTask.getTraits()) {
				switch (trait) {
				case ACADEMIC:
					rand += aa + .5;
					break;
				case AGILITY:
					rand += ag;
					break;
				case ARTISTIC:
					rand += art;
					break;
				case DISCIPLINE:
					rand += discipline;
					break;					
				case LEADERSHIP:
					rand += l;
					break;
				case MEDICAL:
					// need patience and stability to administer healing
					rand += (se + ss) / 2D;
					break;
				case ORGANIZATION:
					rand += org;
					break;
				case PEOPLE:
					rand += ca;
					break;
				case RELAXATION:
					// if a person has high spirituality score and has alternative ways to deal with
					// stress,
					// he will less likely require extra time to relax/sleep/workout/do yoga.
					rand -= ss;
					break;
				case STRENGTH:
					rand += .7 * es + .3 * cou;
					break;
				case TEACHING:
					rand += .7 * t + .3 * aa;
					break;						
				case TREATMENT:
					// if a person is stress-resilient and relatively emotional stable,
					// he will more likely endure pain and less likely ask to be medicated.
					rand -= se;
					break;					
				default:
					break;
				}
			}

			result = (int) Math.round(rand);
			
			if (result > 8)
				result = 8;
			else if (result < -8)
				result = -8;

			String s = getStringName(metaTask);
			if (!scoreStringMap.containsKey(s)) {
				scoreStringMap.put(s, result);
			}
		}
		
		int connectionScore = scoreStringMap.get(CONNECT_ONLINE);
		initializeConnections(connectionScore);
	}

	/**
	 * Initializes probability for each connection.
	 * 
	 * @param scoreStringMap
	 */
	private void initializeConnections(int scoreStringMap) {
		
		Connection[] connections = Connection.values();
		int size = connections.length;
		
		for (int i = 0; i < size; i++) {
			int p = RandomUtil.getRandomInt(0, 100) + scoreStringMap * WEIGHT;
			if (p < 5)
				p = 5;
			if (p > 100)
				p = 100;
			connectionMap.put(connections[i], p);
		}
	}

	/**
	 * Obtains the preference score modified by its priority for a meta task.
	 * 
	 * @param metaTask
	 * @return the score
	 */
	public int getPreferenceScore(MetaTask metaTask) {
		int result = 0;

		// DO NOT use MetaTask instance as the key because they are not serialized and
		// hence on a reload will not find a match since the instance will be different.
		String s = getStringName(metaTask);
		if (scoreStringMap.containsKey(s)) {
			result = scoreStringMap.get(s);
		}

		return result;
	}

	/**
	 * Obtains the proper string name of a meta task.
	 * 
	 * @param metaTask {@link MetaTask}
	 * @return string name of a meta task
	 */
	private static String getStringName(MetaTask metaTask) {
		String s = metaTask.getClass().getSimpleName();
		String ss = s.replaceAll("(?!^)([A-Z])", " $1")
				.replace("Meta", "")
				.replace("E V A ", "EVA ")
				.replace("With ", "with ")
				.replace("To ", "to ");
		return ss.trim();
	}

	/**
	 * Checks if this task is due.
	 * 
	 * @param MetaTask
	 * @return true if it does
	 */
	public boolean isTaskDue(MetaTask mt) {
		if (taskAccomplishedMap.isEmpty()) {
			// if it does not exist (either it is not scheduled or it have been
			// accomplished),
			// the status is true
			return true;
		} else if (taskAccomplishedMap.get(mt) == null)
			return true;
		else
			return taskAccomplishedMap.get(mt);
	}

	/**
	 * Flags this task as being due or not due.
	 * 
	 * @param MetaTask
	 * @param          true if it is due
	 */
	public void setTaskDue(Task task, boolean value) {
		MetaTask mt = MetaTaskUtil.getMetaTypeFromTask(task);

		// if this accomplished meta task is once-a-day task, remove it.
		if (value && onceADayMap.get(mt) != null && !onceADayMap.isEmpty())
			if (onceADayMap.get(mt) != null && onceADayMap.get(mt)) {
				onceADayMap.remove(mt);
				taskAccomplishedMap.remove(mt);
			} else
				taskAccomplishedMap.put(mt, value);

	}

	public Map<String, Integer> getScoreStringMap() {
		return scoreStringMap;
	}

	public Connection getRandomConnection() {
		return RandomUtil.getWeightedIntegerRandomObject(connectionMap);
	}
	
	public int getConnectionScore(Connection connection) {
		return connectionMap.get(connection);
	}
	
	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		person = null;
		onceADayMap = null;
		taskAccomplishedMap = null;
		scoreStringMap = null;
	}
}
