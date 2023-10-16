/*
 * Mars Simulation Project
 * Favorite.java
 * @date 2022-08-01
 * @author Manny Kung
 */

package org.mars_sim.msp.core.person.ai.fav;

import java.io.Serializable;
import java.util.logging.Logger;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.structure.building.function.cooking.HotMeal;
import org.mars_sim.msp.core.structure.building.function.cooking.MealConfig;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;
import org.mars_sim.tools.util.RandomUtil;

public class Favorite implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;
    /** default logger. */
	private static final Logger logger = Logger.getLogger(Favorite.class.getName());

	/** The creativity points that are readily available for projects. */
	private double creativityPool = RandomUtil.getRandomDouble(5, 10);
	/** The cumulative creativity points generated so far . */	
	private double cumulativePool = creativityPool;
	
	private String favoriteMainDish;
	private String favoriteSideDish;
	private String favoriteDessert;
	private FavoriteType favoriteType;

	private static String[] availableDesserts;

	private Person person;
	
	public Favorite(Person person) {
		this.person = person;
		
        availableDesserts = PreparingDessert.getArrayOfDesserts();
        
    	favoriteType = determineRandomFavoriteType();
        favoriteMainDish = determineRandomMainDish();
    	favoriteSideDish = determineRandomSideDish();
    	favoriteDessert = determineRandomDessert();
	}

	
	/** 
	 * Returns the creativity points that are readily available for projects. 
	 */
	public double getCreativityPool() {
		return creativityPool;
	}
	
	/**
	 * Generate creativity points.
	 * 
	 * @param time
	 */
	public void generateCreativity(double time) {
		creativityPool += time * person.getNaturalAttributeManager()
				.getAttribute(NaturalAttributeType.CREATIVITY) / 100
				* RandomUtil.getRandomRegressionInteger(1, 3);
	}
	
	/** 
	 * Returns the cumulative creativity points generated by this person. 
	 */	
	private double getCumulativePool() {
		return cumulativePool;
	}
	
	public String determineRandomMainDish() {
		String result = "";
    	int num = RandomUtil.getRandomInt(MealConfig.getMainDishList().size()-1);
		result = MealConfig.getMainDishList().get(num).getMealName();
		return result;
	}
	
	public String determineRandomSideDish() {
		String result = "";
    	int num = RandomUtil.getRandomInt(MealConfig.getSideDishList().size()-1);
		result = MealConfig.getSideDishList().get(num).getMealName();
		return result;
	}


	public String determineRandomDessert() {
		String result = "";
    	int rand = RandomUtil.getRandomInt(availableDesserts.length - 1);
    	result = availableDesserts[rand];
		return result;
	}

	public FavoriteType determineRandomFavoriteType() {
    	int num = RandomUtil.getRandomInt(FavoriteType.availableFavoriteTypes.length - 1);
		return FavoriteType.availableFavoriteTypes[num];
	}

	public boolean isMainDish(String name) {
		if (name != null) {
	    	for (HotMeal hm : MealConfig.getMainDishList()) {
	    		if (name.equalsIgnoreCase(hm.getMealName())) {
	    			return true;
	    		}
	    	}
		}
		
		return false;
	}
	
	public boolean isSideDish(String name) {
		if (name != null) {
	    	for (HotMeal hm : MealConfig.getSideDishList()) {
	    		if (name.equalsIgnoreCase(hm.getMealName())) {
	    			return true;
	    		}
	    	}
		}
		
		return false;
	}
	
	public boolean isDessert(String name) {
		if (name != null) {
	    	for (String s : availableDesserts) {
	    		if (name.equalsIgnoreCase(s)) {
	    			return true;
	    		}
	    	}
		}
		
		return false;
	}
	
	public boolean isActivity(String name) {
		if (name != null) {
	    	for (FavoriteType f : FavoriteType.values()) {
	    		if (name.equalsIgnoreCase(f.getName())) {
	    			return true;
	    		}
	    	}
		}
		
		return false;
	}
	
	public String getFavoriteMainDish() {
		return favoriteMainDish;
	}

	public String getFavoriteSideDish() {
		return favoriteSideDish;
	}

	public String getFavoriteDessert() {
		return favoriteDessert;
	}

	public FavoriteType getFavoriteActivity() {
		return favoriteType;
	}

	public void setFavoriteMainDish(String name) {
		if (isMainDish(name))
			favoriteMainDish = name;
		else
			logger.severe("The main dish '" + name + "' does not exist in mars-sim !"); 
	}

	public void setFavoriteSideDish(String name) {
		if (isSideDish(name))
			favoriteSideDish = name;
		else
			logger.severe("The side dish '" + name + "' does not exist in mars-sim !"); 
	}

	public void setFavoriteDessert(String name) {
		if (isDessert(name))
			favoriteDessert = name;
		else
			logger.severe("The dessert '" + name + "' does not exist in mars-sim !"); 
	}

	public void setFavoriteActivity(String type) {
		favoriteType = FavoriteType.fromString(type);
		if (favoriteType == null)
			logger.severe("The activity '" + type + "' does not exist in mars-sim !"); 
	}
	
	public void setFavoriteActivityType(FavoriteType type) {
		favoriteType = type;
	}
	
	public void destroy() {
		favoriteType = null;
		availableDesserts = null;
	}
}
