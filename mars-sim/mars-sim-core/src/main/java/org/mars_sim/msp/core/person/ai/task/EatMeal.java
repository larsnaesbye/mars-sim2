/**
 * Mars Simulation Project
 * EatMeal.java
 * @version 3.08 2015-06-15
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.RandomUtil;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.PhysicalCondition;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.Storage;
import org.mars_sim.msp.core.structure.building.function.cooking.CookedMeal;
import org.mars_sim.msp.core.structure.building.function.cooking.Cooking;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparedDessert;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;

/**
 * The EatMeal class is a task for eating a meal.
 * The duration of the task is 40 millisols.
 * Note: Eating a meal reduces hunger to 0.
 */
public class EatMeal extends Task implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(EatMeal.class.getName());

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.eatMeal"); //$NON-NLS-1$

    /** Task phases. */
    private static final TaskPhase PICK_UP_MEAL = new TaskPhase(Msg.getString(
            "Task.phase.pickUpMeal")); //$NON-NLS-1$
    private static final TaskPhase PICK_UP_DESSERT = new TaskPhase(Msg.getString(
            "Task.phase.pickUpDessert")); //$NON-NLS-1$
    private static final TaskPhase EATING_MEAL = new TaskPhase(Msg.getString(
            "Task.phase.eatingMeal")); //$NON-NLS-1$
    private static final TaskPhase EATING_DESSERT = new TaskPhase(Msg.getString(
            "Task.phase.eatingDessert")); //$NON-NLS-1$

    // Static members
    /** The stress modified per millisol. */
    private static final double STRESS_MODIFIER = -.2D;
    private static final double DESSERT_STRESS_MODIFIER = -.5D;
    private static final int NUMBER_OF_MEAL_PER_SOL = 4;
    private static final int NUMBER_OF_DESSERT_PER_SOL = 4;

    /** The proportion of the task for eating a meal. */
    private static final double MEAL_EATING_PROPORTION = .75D;

    /** The proportion of the task for eating dessert. */
    private static final double DESSERT_EATING_PROPORTION = .25D;

    /** Percentage chance that preserved food has gone bad. */
    private static final double PRESERVED_FOOD_BAD_CHANCE = 5D;

    /** Percentage chance that unprepared dessert has gone bad. */
    private static final double UNPREPARED_DESSERT_BAD_CHANCE = 10D;

    /** Mass (kg) of single napkin for meal. */
    private static final double NAPKIN_MASS = .0025D;

    // Data members
    private CookedMeal meal;
    private PreparedDessert dessert;
    private String unpreparedDessertType;
    private boolean hasNapkin;
    private Cooking kitchen;
    private PreparingDessert dessertKitchen;
    private double totalMealEatingTime = 0D;
    private double mealEatingDuration = 0D;
    private double totalDessertEatingTime = 0D;
    private double dessertEatingDuration = 0D;
    private double startingHunger;
    private double currentHunger;

    /**
     * Constructor.
     * @param person the person to perform the task
     */
    public EatMeal(Person person) {
        super(NAME, person, false, false, STRESS_MODIFIER, true, 20D +
                RandomUtil.getRandomDouble(20D));

        // Check if person is not in a settlement or vehicle.
        LocationSituation location = person.getLocationSituation();
        if ((location != LocationSituation.IN_SETTLEMENT) && (location != LocationSituation.IN_VEHICLE)) {
            logger.severe(person + " is trying to eat a meal, but is not in a settlement or a vehicle.");
            endTask();
        }

        // Initialize data members.
        mealEatingDuration = getDuration() * MEAL_EATING_PROPORTION;
        dessertEatingDuration = getDuration() * DESSERT_EATING_PROPORTION;
        startingHunger = person.getPhysicalCondition().getHunger();
        currentHunger = startingHunger;
        
        // Take napkin from inventory if available.
        Unit container = person.getTopContainerUnit();
        if (container != null) {
            Inventory inv = container.getInventory();
            hasNapkin = Storage.retrieveAnResource(NAPKIN_MASS, "napkin", inv, true);
        }

        // Initialize task phase.
        addPhase(PICK_UP_MEAL);
        addPhase(PICK_UP_DESSERT);
        addPhase(EATING_MEAL);
        addPhase(EATING_DESSERT);

        setPhase(PICK_UP_MEAL);
    }

    @Override
    protected BuildingFunction getRelatedBuildingFunction() {
        return BuildingFunction.DINING;
    }

    /**
     * Performs the method mapped to the task's current phase.
     * @param time the amount of time (millisol) the phase is to be performed.
     * @return the remaining time (millisol) after the phase has been performed.
     */
    protected double performMappedPhase(double time) {
        if (getPhase() == null) {
            throw new IllegalArgumentException("Task phase is null");
        }
        else if (PICK_UP_MEAL.equals(getPhase())) {
            return pickUpMealPhase(time);
        }
        else if (PICK_UP_DESSERT.equals(getPhase())) {
            return pickUpDessertPhase(time);
        }
        else if (EATING_MEAL.equals(getPhase())) {
            return eatingMealPhase(time);
        }
        else if (EATING_DESSERT.equals(getPhase())) {
            return eatingDessertPhase(time);
        }
        else {
            return time;
        }
    }

    /**
     * Perform the pick up meal phase.
     * @param time the amount of time (millisol) to perform the phase.
     * @return the remaining time (millisol) after the phase has been performed.
     */
    private double pickUpMealPhase(double time) {

      // Determine preferred kitchen to get meal.
      if (kitchen == null) {
          kitchen = getKitchenWithMeal(person);

          if (kitchen != null) {
              // Walk to kitchen.
              walkToActivitySpotInBuilding(kitchen.getBuilding(), BuildingFunction.COOKING, true);
              return time;
          }
          else {
              // If no kitchen found, look for dessert.
              setPhase(PICK_UP_DESSERT);
              return time;
          }
      }

      // Pick up a meal at kitchen if one is available.
      meal = kitchen.chooseAMeal(person);
      if (meal != null) {
          logger.fine(person + " picking up a cooked meal to eat: " + meal.getName());
      }

      setPhase(PICK_UP_DESSERT);
      return time;
    }

    /**
     * Perform the pick up dessert phase.
     * @param time the amount of time (millisol) to perform the phase.
     * @return the remaining time (millisol) after the phase has been performed.
     */
    private double pickUpDessertPhase(double time) {

        // Determine preferred kitchen to get dessert.
        if (dessertKitchen == null) {
            dessertKitchen = getKitchenWithDessert(person);

            if (dessertKitchen != null) {
                // Walk to dessert kitchen.
                walkToActivitySpotInBuilding(dessertKitchen.getBuilding(), BuildingFunction.PREPARING_DESSERT, true);
                return time;
            }
            else {
                // If no dessert kitchen found, go eat meal.
                setPhase(EATING_MEAL);
                return time;
            }
        }

        // Pick up a dessert at kitchen if one is available.
        dessert = dessertKitchen.chooseADessert(person);
        if (dessert != null) {
            logger.fine(person + " picking up a prepared dessert to eat: " + dessert.getName());
        }

        setPhase(EATING_MEAL);
        return time;
    }

    /**
     * Performs the eating meal phase of the task.
     * @param time the amount of time (millisol) to perform the eating meal phase.
     * @return the amount of time (millisol) left after performing the eating meal phase.
     */
    private double eatingMealPhase(double time) {

        double remainingTime = 0D;

        double eatingTime = time;
        if ((totalMealEatingTime + eatingTime) >= mealEatingDuration) {
            eatingTime = mealEatingDuration - totalMealEatingTime;
        }

        if (eatingTime > 0D) {

            if (meal != null) {
                // Eat cooked meal.
                eatCookedMeal(eatingTime);
            }
            else {
                // Eat preserved food.
                boolean enoughFood = eatPreservedFood(eatingTime);

                // If not enough preserved food available, change to dessert phase.
                if (!enoughFood) {
                    setPhase(EATING_DESSERT);
                    remainingTime = time;
                }
            }
        }

        totalMealEatingTime += eatingTime;

        // If finished eating, change to dessert phase.
        if (eatingTime < time) {
            setPhase(EATING_DESSERT);
            remainingTime = time - eatingTime;
        }

        return remainingTime;
    }

    /**
     * Eat a cooked meal.
     * @param eatingTime the amount of time (millisols) to eat.
     */
    private void eatCookedMeal(double eatingTime) {

        setDescription(Msg.getString("Task.description.eatMeal.cooked")); //$NON-NLS-1$

        // Proportion of meal being eaten over this time period.
        double mealProportion = eatingTime / mealEatingDuration;

        PhysicalCondition condition = person.getPhysicalCondition();

        // Reduce person's hunger by proportion of meal eaten.
        // Entire meal will reduce person's hunger to 0.
        currentHunger -= (startingHunger * mealProportion);
        if (currentHunger < 0D) {
            currentHunger = 0D;
        }
        condition.setHunger(currentHunger);

        // Reduce person's stress over time from eating a cooked meal.
        // This is in addition to normal stress reduction from eating task.
        double mealStressModifier = STRESS_MODIFIER * (meal.getQuality() + 1D);
        double newStress = condition.getStress() - (mealStressModifier * eatingTime);
        condition.setStress(newStress);

        // Add caloric energy from meal.
        double caloricEnergyFoodAmount = meal.getDryMass() * mealProportion;
        condition.addEnergy(caloricEnergyFoodAmount);
    }

    /**
     * Eat a meal of preserved food.
     * @param eatingTime the amount of time (millisols) to eat.
     * @return true if enough preserved food available to eat.
     */
    private boolean eatPreservedFood(double eatingTime) {

        setDescription(Msg.getString("Task.description.eatMeal.preserved")); //$NON-NLS-1$

        boolean result = true;

        PhysicalCondition condition = person.getPhysicalCondition();

        // Determine total preserved food amount eaten during this meal.
        PersonConfig config = SimulationConfig.instance().getPersonConfiguration();
        double totalFoodAmount = config.getFoodConsumptionRate() / NUMBER_OF_MEAL_PER_SOL;

        // Proportion of meal being eaten over this time period.
        double mealProportion = eatingTime / mealEatingDuration;

        // Food amount eaten over this period of time.
        double foodAmount = totalFoodAmount * mealProportion;

        Unit containerUnit = person.getTopContainerUnit();
        if (containerUnit != null) {
            Inventory inv = containerUnit.getInventory();

            // Take preserved food from inventory if it is available.
            if (Storage.retrieveAnResource(foodAmount, "food", inv, true)) {

                // Check if preserved food has gone bad.
                if (RandomUtil.lessThanRandPercent(PRESERVED_FOOD_BAD_CHANCE)) {

                    // Throw food out.
                    Storage.storeAnResource(foodAmount, "food waste", inv);
                }
                else {
                    // Consume preserved food.

                    // Reduce person's hunger by proportion of meal eaten.
                    // Entire meal will reduce person's hunger to 0.
                    currentHunger -= (startingHunger * mealProportion);
                    if (currentHunger < 0D) {
                        currentHunger = 0D;
                    }
                    condition.setHunger(currentHunger);

                    // Add caloric energy from meal.
                    condition.addEnergy(foodAmount);
                }
            }
            else {
                // Not enough food available to eat.
                result = false;
            }
        }
        else {
            // Person is not inside a container unit, so end task.
            result = false;
            endTask();
        }

        return result;
    }

    /**
     * Performs the eating dessert phase of the task.
     * @param time the amount of time (millisol) to perform the eating dessert phase.
     * @return the amount of time (millisol) left after performing the eating dessert phase.
     */
    private double eatingDessertPhase(double time) {

        double remainingTime = 0D;

        double eatingTime = time;
        if ((totalDessertEatingTime + eatingTime) >= dessertEatingDuration) {
            eatingTime = dessertEatingDuration - totalDessertEatingTime;
        }

        if (eatingTime > 0D) {

            if (dessert != null) {
                // Eat prepared dessert.
                eatPreparedDessert(eatingTime);
            }
            else {
                // Eat unprepared dessert (fruit, soy milk, etc).
                boolean enoughDessert = eatUnpreparedDessert(eatingTime);

                // If not enough unprepared dessert available, end task.
                if (!enoughDessert) {
                    remainingTime = time;
                    endTask();
                }
            }
        }

        totalMealEatingTime += eatingTime;

        // If finished eating, end task.
        if (eatingTime < time) {
            remainingTime = time - eatingTime;
            endTask();
        }

        return remainingTime;
    }

    /**
     * Eat a prepared dessert.
     * @param eatingTime the amount of time (millisols) to eat.
     */
    private void eatPreparedDessert(double eatingTime) {

        setDescription(Msg.getString("Task.description.eatMeal.preparedDessert")); //$NON-NLS-1$

        // Proportion of dessert being eaten over this time period.
        double dessertProportion = eatingTime / dessertEatingDuration;

        PhysicalCondition condition = person.getPhysicalCondition();

        // Reduce person's stress over time from eating a prepared.
        // This is in addition to normal stress reduction from eating task.
        double mealStressModifier = DESSERT_STRESS_MODIFIER * (dessert.getQuality() + 1D);
        double newStress = condition.getStress() - (mealStressModifier * eatingTime);
        condition.setStress(newStress);

        // Add caloric energy from dessert.
        double caloricEnergyFoodAmount = dessert.getDryMass() * dessertProportion;
        condition.addEnergy(caloricEnergyFoodAmount);
    }

    /**
     * Eat an unprepared dessert.
     * @param eatingTime the amount of time (millisols) to eat.
     * @return true if enough unprepared dessert was available to eat.
     */
    private boolean eatUnpreparedDessert(double eatingTime) {

        setDescription(Msg.getString("Task.description.eatMeal.unpreparedDessert")); //$NON-NLS-1$

        boolean result = true;

        PhysicalCondition condition = person.getPhysicalCondition();

        // Determine total unprepared dessert amount eaten during this meal.
        PersonConfig config = SimulationConfig.instance().getPersonConfiguration();
        double totalDessertAmount = config.getDessertConsumptionRate() / NUMBER_OF_DESSERT_PER_SOL;

        // Determine dessert resource type if not known.
        if (unpreparedDessertType == null) {

            // Determine list of available dessert resources.
            List<String> availableDessertResources = getAvailableDessertResources(totalDessertAmount);
            if (availableDessertResources.size() > 0) {

                // Randomly choose available dessert resource.
                int index = RandomUtil.getRandomInt(availableDessertResources.size() - 1);
                unpreparedDessertType = availableDessertResources.get(index);
            }
            else {
                result = false;
            }
        }

        // Consume portion of unprepared dessert resource.
        if (unpreparedDessertType != null) {
            // Proportion of dessert being eaten over this time period.
            double dessertProportion = eatingTime / dessertEatingDuration;

            // Dessert amount eaten over this period of time.
            double dessertAmount = totalDessertAmount * dessertProportion;

            Unit containerUnit = person.getTopContainerUnit();
            if (containerUnit != null) {
                Inventory inv = containerUnit.getInventory();

                // Take dessert resource from inventory if it is available.
                if (Storage.retrieveAnResource(dessertAmount, unpreparedDessertType, inv, true)) {

                    // Check if dessert resource has gone bad.
                    if (RandomUtil.lessThanRandPercent(UNPREPARED_DESSERT_BAD_CHANCE)) {

                        // Throw dessert resource out.
                        Storage.storeAnResource(dessertAmount, "food waste", inv);
                    }
                    else {
                        // Consume unpreserved dessert.

                        // Add caloric energy from dessert.
                        condition.addEnergy(dessertAmount);
                    }
                }
                else {
                    // Not enough dessert resource available to eat.
                    result = false;
                }
            }
            else {
                // Person is not inside a container unit, so end task.
                result = false;
                endTask();
            }
        }

        return result;
    }

    /**
     * Gets a list of available unprepared dessert resources.
     * @param amountNeeded the amount (kg) of unprepared dessert needed for eating.
     * @return list of resource strings.
     */
    private List<String> getAvailableDessertResources(double amountNeeded) {

        List<String> result = new ArrayList<String>();

        Unit containerUnit = person.getTopContainerUnit();
        if (containerUnit != null) {
            Inventory inv = containerUnit.getInventory();

            String[] possibleDesserts = PreparingDessert.getArrayOfDesserts();
            for (int x = 0; x < possibleDesserts.length; x++) {
                String dessert = possibleDesserts[x];
                boolean available = Storage.retrieveAnResource(amountNeeded, dessert, inv, false);
                if (available) {
                    result.add(dessert);
                }
            }
        }

        return result;
    }

    /**
     * Adds experience to the person's skills used in this task.
     * @param time the amount of time (ms) the person performed this task.
     */
    protected void addExperience(double time) {
        // This task adds no experience.
    }

    /**
     * Gets an available dining building that the person can use.
     * Returns null if no dining building is currently available.
     *
     * @param person the person
     * @return available dining building
     * @throws BuildingException if error finding dining building.
     */
    public static Building getAvailableDiningBuilding(Person person) {

        Building result = null;

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Settlement settlement = person.getSettlement();
            BuildingManager manager = settlement.getBuildingManager();
            List<Building> diningBuildings = manager.getBuildings(BuildingFunction.DINING);
            diningBuildings = BuildingManager.getWalkableBuildings(person, diningBuildings);
            diningBuildings = BuildingManager.getNonMalfunctioningBuildings(diningBuildings);
            diningBuildings = BuildingManager.getLeastCrowdedBuildings(diningBuildings);

            if (diningBuildings.size() > 0) {
                Map<Building, Double> diningBuildingProbs = BuildingManager.getBestRelationshipBuildings(
                        person, diningBuildings);
                result = RandomUtil.getWeightedRandomObject(diningBuildingProbs);
            }
        }

        return result;
    }

    /**
     * Gets a kitchen in the person's settlement that currently has cooked meals.
     * @param person the person to check for
     * @return the kitchen or null if none.
     */
    public static Cooking getKitchenWithMeal(Person person) {
        Cooking result = null;

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Settlement settlement = person.getSettlement();
            BuildingManager manager = settlement.getBuildingManager();
            List<Building> cookingBuildings = manager.getBuildings(BuildingFunction.COOKING);
            Iterator<Building> i = cookingBuildings.iterator();
            while (i.hasNext() && (result == null)) {
                Building building = i.next();
                Cooking kitchen = (Cooking) building.getFunction(BuildingFunction.COOKING);
                if (kitchen.hasCookedMeal()) {
                    result = kitchen;
                }
            }
        }

        return result;
    }

    /**
     * Gets a kitchen in the person's settlement that currently has prepared desserts.
     * @param person the person to check for
     * @return the kitchen or null if none.
     */
    public static PreparingDessert getKitchenWithDessert(Person person) {
        PreparingDessert result = null;

        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Settlement settlement = person.getSettlement();
            BuildingManager manager = settlement.getBuildingManager();
            List<Building> dessertBuildings = manager.getBuildings(BuildingFunction.PREPARING_DESSERT);
            Iterator<Building> i = dessertBuildings.iterator();
            while (i.hasNext() && (result == null)) {
                Building building = i.next();
                PreparingDessert kitchen = (PreparingDessert) building.getFunction(BuildingFunction.PREPARING_DESSERT);
                if (kitchen.hasFreshDessert()) {
                    result = kitchen;
                }
            }
        }

        return result;
    }

    /**
     * Checks if there is preserved food available for the person.
     * @param person the person to check.
     * @return true if preserved food is available.
     */
    public static boolean isPreservedFoodAvailable(Person person) {
        boolean result = false;
        Unit containerUnit = person.getTopContainerUnit();
        if (containerUnit != null) {
            try {
                Inventory inv = containerUnit.getInventory();
                PersonConfig config = SimulationConfig.instance().getPersonConfiguration();
                double foodAmount = config.getFoodConsumptionRate() / NUMBER_OF_MEAL_PER_SOL;
                result = Storage.retrieveAnResource(foodAmount, "food", inv, false);
            }
            catch (Exception e) {
                e.printStackTrace(System.err);
            }
        }
        return result;
    }

    @Override
    public int getEffectiveSkillLevel() {
        return 0;
    }

    @Override
    public List<SkillType> getAssociatedSkills() {
        List<SkillType> results = new ArrayList<SkillType>(0);
        return results;
    }

    @Override
    public void endTask() {
        super.endTask();

        // Throw away napkin waste if one was used.
        if (hasNapkin) {
            Unit container = person.getTopContainerUnit();
            if (container != null) {
                Inventory inv = container.getInventory();
                Storage.storeAnResource(NAPKIN_MASS, "solid waste", inv);
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        kitchen = null;
        meal = null;
        dessertKitchen = null;
        dessert = null;
    }
}