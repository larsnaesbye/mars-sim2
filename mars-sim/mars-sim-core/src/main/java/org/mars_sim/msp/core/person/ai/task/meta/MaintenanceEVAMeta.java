/**
 * Mars Simulation Project
 * MaintenanceEVAMeta.java
 * @version 3.08 2015-06-17
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task.meta;

import java.io.Serializable;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.malfunction.MalfunctionFactory;
import org.mars_sim.msp.core.malfunction.MalfunctionManager;
import org.mars_sim.msp.core.malfunction.Malfunctionable;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.core.person.LocationSituation;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.Job;
import org.mars_sim.msp.core.person.ai.task.EVAOperation;
import org.mars_sim.msp.core.person.ai.task.Maintenance;
import org.mars_sim.msp.core.person.ai.task.MaintenanceEVA;
import org.mars_sim.msp.core.person.ai.task.Task;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.ai.job.Repairbot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.Structure;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.BuildingFunction;

/**
 * Meta task for the MaintenanceEVA task.
 */
public class MaintenanceEVAMeta implements MetaTask, Serializable {

    /** default serial id. */
    private static final long serialVersionUID = 1L;
    
    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.maintenanceEVA"); //$NON-NLS-1$

    /** default logger. */
    private static Logger logger = Logger.getLogger(MaintenanceEVAMeta.class.getName());

    private SurfaceFeatures surface;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Task constructInstance(Person person) {
        return new MaintenanceEVA(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;
        
        // Crowded settlement modifier
        if (person.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
            Settlement settlement = person.getSettlement();
            if (settlement.getCurrentPopulationNum() > settlement.getPopulationCapacity())
                result *= 2D;

            try {
                // Total probabilities for all malfunctionable entities in person's local.
                Iterator<Malfunctionable> i = MalfunctionFactory.getMalfunctionables(person).iterator();
                while (i.hasNext()) {
                    Malfunctionable entity = i.next();
                    boolean isStructure = (entity instanceof Structure);
                    boolean uninhabitableBuilding = false;
                    if (entity instanceof Building) {
                        uninhabitableBuilding = !((Building) entity).hasFunction(BuildingFunction.LIFE_SUPPORT);
                    }
                    MalfunctionManager manager = entity.getMalfunctionManager();
                    boolean hasMalfunction = manager.hasMalfunction();
                    boolean hasParts = Maintenance.hasMaintenanceParts(person, entity);
                    double effectiveTime = manager.getEffectiveTimeSinceLastMaintenance();
                    boolean minTime = (effectiveTime >= 1000D);
                    if ((isStructure || uninhabitableBuilding) && !hasMalfunction && minTime && hasParts) {
                        double entityProb = manager.getEffectiveTimeSinceLastMaintenance() / 1000D;
                        if (entityProb > 100D) {
                            entityProb = 100D;
                        }
                        result += entityProb;
                    }
                }
            }
            catch (Exception e) {
                logger.log(Level.SEVERE,"getProbability()",e);
            }

            // Check if an airlock is available
            if (EVAOperation.getWalkableAvailableAirlock(person) == null) {
                result = 0D;
            }

            // Check if it is night time.
            if (surface == null) {
                surface = Simulation.instance().getMars().getSurfaceFeatures();
            }
            if (surface.getSolarIrradiance(person.getCoordinates()) == 0D) {
                if (!surface.inDarkPolarRegion(person.getCoordinates())) {
                    result = 0D;
                }
            }

            // Effort-driven task modifier.
            result *= person.getPerformanceRating();

            // Job modifier.
            Job job = person.getMind().getJob();
            if (job != null) {
                result *= job.getStartTaskProbabilityModifier(MaintenanceEVA.class);
            }

            // Modify if tinkering is the person's favorite activity.
            if (person.getFavorite().getFavoriteActivity().equalsIgnoreCase("Tinkering")) {
                result *= 2D;
            }

            // 2015-06-07 Added Preference modifier
            if (result > 0)
                result += person.getPreference().getPreferenceScore(this);
            if (result < 0) result = 0;
        }

        return result;
    }

	@Override
	public Task constructInstance(Robot robot) {
        return new MaintenanceEVA(robot);
	}

	@Override
	public double getProbability(Robot robot) {

        double result = 0D;

        if (robot.getBotMind().getRobotJob() instanceof Repairbot) {

	        if (robot.getLocationSituation() == LocationSituation.IN_SETTLEMENT) {

		        try {
		            // Total probabilities for all malfunctionable entities in robot's local.
		            Iterator<Malfunctionable> i = MalfunctionFactory.getMalfunctionables(robot).iterator();
		            while (i.hasNext()) {
		                Malfunctionable entity = i.next();
		                boolean isStructure = (entity instanceof Structure);
		                boolean uninhabitableBuilding = false;
		                if (entity instanceof Building) {
		                    uninhabitableBuilding = !((Building) entity).hasFunction(BuildingFunction.LIFE_SUPPORT);
		                }
		                MalfunctionManager manager = entity.getMalfunctionManager();
		                boolean hasMalfunction = manager.hasMalfunction();
		                boolean hasParts = Maintenance.hasMaintenanceParts(robot, entity);
		                double effectiveTime = manager.getEffectiveTimeSinceLastMaintenance();
		                boolean minTime = (effectiveTime >= 1000D);
		                if ((isStructure || uninhabitableBuilding) && !hasMalfunction && minTime && hasParts) {
		                    double entityProb = manager.getEffectiveTimeSinceLastMaintenance() / 1000D;
		                    if (entityProb > 100D) {
		                        entityProb = 100D;
		                    }
		                    result += entityProb;
		                }
		            }

		            // Effort-driven task modifier.
		            result *= robot.getPerformanceRating();

		            // Check if it is night time.
		            SurfaceFeatures surface = Simulation.instance().getMars().getSurfaceFeatures();
		            if (surface.getSolarIrradiance(robot.getCoordinates()) == 0D) {
		                if (!surface.inDarkPolarRegion(robot.getCoordinates())) {
		                    result = 0D;
		                }
		            }

		            // Check if an airlock is available
	                if (EVAOperation.getWalkableAvailableAirlock(robot) == null) {
	                    result = 0D;
	                }
		        }
		        catch (Exception e) {
		            logger.log(Level.SEVERE,"getProbability()",e);
		        }
	        }
        }

        return result;
	}
}