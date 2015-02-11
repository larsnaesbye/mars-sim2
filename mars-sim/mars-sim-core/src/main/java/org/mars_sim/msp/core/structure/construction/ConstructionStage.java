/**
 * Mars Simulation Project
 * ConstructionStage.java
 * @version 3.08 2015-02-10

 * @author Scott Davis
 */
package org.mars_sim.msp.core.structure.construction;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.Part;

/**
 * A construction stage of a construction site.
 * TODO externalize strings
 */
public class ConstructionStage implements Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    // Construction site events.
    public static final String ADD_CONSTRUCTION_WORK_EVENT = "adding construction work";
    public static final String ADD_SALVAGE_WORK_EVENT = "adding salvage work";

    /** Work time modifier for salvaging a construction stage. */
    private static final double SALVAGE_WORK_TIME_MODIFIER = .25D;

    // Data members
    private ConstructionStageInfo info;
    private ConstructionSite site;
    private double completedWorkTime;
    private boolean isSalvaging;
    private double completableWorkTime;
    private Map<Part, Integer> remainingParts;
    private Map<AmountResource, Double> remainingResources;

    /**
     * Constructor.
     * @param info the stage information.
     */
    public ConstructionStage(ConstructionStageInfo info, ConstructionSite site) {
        this.info = info;
        this.site = site;
        completedWorkTime = 0D;
        isSalvaging = false;
        completableWorkTime = 0D;
        remainingParts = new HashMap<Part, Integer>(info.getParts());
        remainingResources = new HashMap<AmountResource, Double>(info.getResources());

        // Update the remaining completable work time.
        updateCompletableWorkTime();
    }

    /**
     * Get the construction stage information.
     * @return stage information.
     */
    public ConstructionStageInfo getInfo() {
        return info;
    }

    /**
     * Gets the completed work time on the stage.
     * @return work time (in millisols).
     */
    public double getCompletedWorkTime() {
        return completedWorkTime;
    }

    /**
     * Sets the completed work time on the stage.
     * @param completedWorkTime work time (in millisols).
     */
    public void setCompletedWorkTime(double completedWorkTime) {
        this.completedWorkTime = completedWorkTime;
    }

    /**
     * Gets the amount work time that can be completed for this stage.
     * @return completable work time (millisols).
     */
    public double getCompletableWorkTime() {
        return completableWorkTime;
    }

    /**
     * Gets the required work time for the stage.
     * @return work time (in millisols).
     */
    public double getRequiredWorkTime() {
        double requiredWorkTime = info.getWorkTime();
        if (isSalvaging) {
            requiredWorkTime *= SALVAGE_WORK_TIME_MODIFIER;
        }
        return requiredWorkTime;
    }

    /**
     * Adds work time to the construction stage.
     * @param workTime the work time (in millisols) to add.
     */
    public void addWorkTime(double workTime) {
        completedWorkTime += workTime;

        if (completedWorkTime > getRequiredWorkTime()) {
            completedWorkTime = getRequiredWorkTime();
        }

        // Fire construction event
        if (isSalvaging) {
            site.fireConstructionUpdate(ADD_SALVAGE_WORK_EVENT, this);
        }
        else {
            site.fireConstructionUpdate(ADD_CONSTRUCTION_WORK_EVENT, this);
        }
    }

    /**
     * Checks if the stage is complete.
     * @return true if stage is complete.
     */
    public boolean isComplete() {
        return (completedWorkTime >= getRequiredWorkTime());
    }

    /**
     * Checks if the stage is salvaging.
     * @return true if stage is salvaging.
     */
    public boolean isSalvaging() {
        return isSalvaging;
    }

    /**
     * Sets if the stage is salvaging.
     * @param isSalvaging true if staging is salvaging.
     */
    public void setSalvaging(boolean isSalvaging) {
        this.isSalvaging = isSalvaging;
    }

    /**
     * Gets the remaining parts needed for construction.
     * @return map of parts and their numbers.
     */
    public Map<Part, Integer> getRemainingParts() {
        return new HashMap<Part, Integer>(remainingParts);
    }

    /**
     * Gets the remaining resources needed for construction.
     * @return map of resources and their amounts (kg).
     */
    public Map<AmountResource, Double> getRemainingResources() {
        return new HashMap<AmountResource, Double>(remainingResources);
    }

    /**
     * Adds parts to the construction stage.
     * @param part the part to add.
     * @param number the number of parts to add.
     */
    public void addParts(Part part, int number) {

        if (remainingParts.containsKey(part)) {
            int remainingRequiredNum = remainingParts.get(part);
            if (number <= remainingRequiredNum) {
                remainingRequiredNum -= number;
                if (remainingRequiredNum > 0) {
                    remainingParts.put(part, remainingRequiredNum);
                }
                else {
                    remainingParts.remove(part);
                }

                // Update the remaining completable work time.
                updateCompletableWorkTime();
            }
            else {
                throw new IllegalStateException("Trying to add " + number + " " + part + 
                        " to " + info.getName() + " when only " + remainingRequiredNum + 
                        " are needed.");
            }
        }
        else {
            throw new IllegalStateException("Construction stage " + info.getName() + 
                    " does not require part " + part);
        }
    }

    /**
     * Add resource to the construction stage.
     * @param resource the resource to add.
     * @param amount the amount (kg) of resource to add.
     */
    public void addResource(AmountResource resource, double amount) {

        if (remainingResources.containsKey(resource)) {
            double remainingRequiredAmount = remainingResources.get(resource);
            if (amount <= remainingRequiredAmount) {
                remainingRequiredAmount -= amount;
                if (remainingRequiredAmount > 0D) {
                    remainingResources.put(resource, remainingRequiredAmount);
                }
                else {
                    remainingResources.remove(resource);
                }

                // Update the remaining completable work time.
                updateCompletableWorkTime();
            }
            else {
                throw new IllegalStateException("Trying to add " + amount + " " + resource + 
                        " to " + info.getName() + " when only " + remainingRequiredAmount + 
                        " are needed.");
            }
        }
        else {
            throw new IllegalStateException("Construction stage " + info.getName() + 
                    " does not require resource " + resource);
        }
    }

    /**
     * Updates the completable work time available.
     */
    private void updateCompletableWorkTime() {

        double totalRequiredConstructionMaterial = getConstructionMaterialMass(
                info.getResources(), info.getParts());

        double remainingConstructionMaterial = getConstructionMaterialMass(
                remainingResources, remainingParts);

        double proportion = 1D;
        if (totalRequiredConstructionMaterial > 0D) {
            proportion = (totalRequiredConstructionMaterial - remainingConstructionMaterial) / 
                    totalRequiredConstructionMaterial;
        }

        completableWorkTime = proportion * info.getWorkTime();
    }

    /**
     * Gets the total mass of construction materials.
     * @param resources map of resources and their amounts (kg).
     * @param parts map of parts and their numbers.
     * @return total mass.
     */
    private double getConstructionMaterialMass(Map<AmountResource, Double> resources, Map<Part, 
            Integer> parts) {

        double result = 0D;

        // Add total mass of resources.
        Iterator<AmountResource> i = resources.keySet().iterator();
        while (i.hasNext()) {
            AmountResource resource = i.next();
            double amount = resources.get(resource);
            result += amount;
        }

        // Add total mass of parts.
        Iterator<Part> j = parts.keySet().iterator();
        while (j.hasNext()) {
            Part part = j.next();
            int number = parts.get(part);
            double mass = part.getMassPerItem();
            result += number * mass;
        }

        return result;
    }

    @Override
    public String toString() {
        String result = "";
        if (isSalvaging) result = "salvaging " + info.getName();
        else result = "constructing " + info.getName();
        return result;
    }
}