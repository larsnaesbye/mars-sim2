/**
 * Mars Simulation Project
 * CookMealMeta.java
 * @version 3.08 2015-05-13
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

//import java.util.logging.Logger;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.task.CookMeal;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.ai.job.Chefbot;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;
import org.mars_sim.msp.core.structure.building.function.cooking.Cooking;

/**
 * Meta task for the CookMeal task.
 */
public class CookMealMeta implements MetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.cookMeal"); //$NON-NLS-1$

    /** default logger. */
    //private static Logger logger = Logger.getLogger(CookMealMeta.class.getName());

    public CookMealMeta () {

    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new CookMeal(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;

        if (CookMeal.isMealTime(person.getCoordinates())) {

            // See if there is an available kitchen.
            Building kitchenBuilding = CookMeal.getAvailableKitchen(person);
            if (kitchenBuilding != null) {

                Cooking kitchen = (Cooking) kitchenBuilding.getFunction(BuildingFunction.COOKING);

                // Check if there are enough ingredients to cook a meal.
                int numGoodRecipes = kitchen.getMealRecipesWithAvailableIngredients().size();

                // Check if enough meals have been cooked at kitchen for this meal time.
                boolean enoughMeals = kitchen.getCookNoMore();

                if ((numGoodRecipes > 0) && !enoughMeals) {

                    result = 300D;

                    // Crowding modifier.
                    result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(person, kitchenBuilding);
                    result *= TaskProbabilityUtil.getRelationshipModifier(person, kitchenBuilding);

                    // Effort-driven task modifier.
                    result *= person.getPerformanceRating();

                    // Job modifier.
                    Job job = person.getMind().getJob();
                    if (job != null) {
                        result *= job.getStartTaskProbabilityModifier(CookMeal.class);
                    }

                    // Modify if cooking is the person's favorite activity.
                    if (person.getFavorite().getFavoriteActivity().equalsIgnoreCase("Cooking")) {
                        result *= 2D;
                    }

                    // 2015-06-07 Added Preference modifier
                    if (result > 0)
                    	result += person.getPreference().getPreferenceScore(this);
                    if (result < 0) result = 0;

                }
            }
        }

        return result;
    }

	@Override
	public Task constructInstance(Robot robot) {
        return new CookMeal(robot);
	}

	@Override
	public double getProbability(Robot robot) {

        double result = 0D;

        if (CookMeal.isMealTime(robot)) {
            if (robot.getBotMind().getRobotJob() instanceof Chefbot) {

                // See if there is an available kitchen.
                Building kitchenBuilding = CookMeal.getAvailableKitchen(robot);

                if (kitchenBuilding != null) {

                    Cooking kitchen = (Cooking) kitchenBuilding.getFunction(BuildingFunction.COOKING);

                    // Check if there are enough ingredients to cook a meal.
                    int numGoodRecipes = kitchen.getMealRecipesWithAvailableIngredients().size();

                    // Check if enough meals have been cooked at kitchen for this meal time.
                    boolean enoughMeals = kitchen.getCookNoMore();

                    if ((numGoodRecipes > 0) && !enoughMeals) {

                        result = 300D;

                        // Crowding modifier.
                        result *= TaskProbabilityUtil.getCrowdingProbabilityModifier(robot, kitchenBuilding);

                        // Effort-driven task modifier.
                        result *= robot.getPerformanceRating();
                    }
                }
            }
        }

        return result;
	}
}