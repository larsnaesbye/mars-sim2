/**
 * Mars Simulation Project
 * MissionTravelStep.java
 * @date 2023-06-03
 * @author Barry Evans
 */
package com.mars_sim.core.mission;

import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.mission.NavPoint;
import com.mars_sim.core.person.ai.mission.VehicleMission;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.project.Stage;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.resource.SuppliesManifest;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.vehicle.GroundVehicle;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.core.vehicle.VehicleController;
import com.mars_sim.core.vehicle.task.DriveGroundVehicle;
import com.mars_sim.mapdata.location.Coordinates;

/**
 * This is a step in a Mission that travels from one location to another.
 */
public class MissionTravelStep extends MissionStep {

    private static final long serialVersionUID = 1L;

	private static final SimLogger logger = SimLogger.getLogger(MissionTravelStep.class.getName());

    private static final double SMALL_DISTANCE = 0.1D;
    private NavPoint destination;

    private Coordinates startingCoordinate;

    public MissionTravelStep(MissionVehicleProject project, NavPoint destination) {
        super(project, Stage.ACTIVE, "Travel to " + destination.getDescription());

        this.destination = destination;
    }

    @Override
    protected boolean execute(Worker worker) {

        Vehicle vehicle = ((VehicleMission) getMission()).getVehicle();
        Coordinates current = vehicle.getCoordinates();
        Coordinates target =  destination.getLocation();

		if (current.equals(target)
					|| Coordinates.computeDistance(current, target) < SMALL_DISTANCE) {
            Settlement base = destination.getSettlement();
            if (vehicle.getAssociatedSettlement().equals(base)) {
                logger.info(vehicle, "Arrived back home " + base.getName() + ".");
                vehicle.transfer(base);

                // Note: There is a problem with the Vehicle not being on the
                // surface vehicle list. The problem is a lack of transfer at the start of TRAVEL phase
                // This is temporary fix pending #474 which will revisit transfers
                if (!base.equals(vehicle.getContainerUnit())) {
                    vehicle.setContainerUnit(base);
                    logger.severe(vehicle, "Forced its container unit to become its home base.");
                }
            }

            // Arrived
            complete();
            return true;
        }

		// Choose a driver
        boolean workedOn = false;
        if (vehicle.getOperator() == null) {
            Task operateVehicleTask = createOperateVehicleTask(vehicle, worker);
            if (operateVehicleTask != null) {
                workedOn = assignTask(worker, operateVehicleTask);
            }
        }
        return workedOn;
    }

    /**
     * Start the step so record the Vehicles currnet locaiton
     */
    @Override
    protected void start() {
        super.start();
        startingCoordinate = getVehicle().getCoordinates();
    }

    /**
     * Get the assigned vehicle
     * @return
     */
    private Vehicle getVehicle() {
        return ((VehicleMission) getMission()).getVehicle();
    }

    /**
     * Create the most appropriate Task to operate the vehicle
     * @param vehicle
     * @param worker
     * @return
     */
    private Task createOperateVehicleTask(Vehicle vehicle, Worker worker) {
        if (vehicle instanceof GroundVehicle gv) {
            double coveredSoFar = getDistanceCovered();
            return new DriveGroundVehicle((Person) worker, gv, destination.getLocation(),
                                            getMission().getPhaseStartTime(), coveredSoFar);
        }
        else {
            throw new IllegalArgumentException("Can only operatate Ground Vehicle " + vehicle.getName());
        }
    }

    /**
     * What resources are needed for this travel. Should be vehicle fuel plus food and oxygen.
     */
    @Override
    void getRequiredResources(SuppliesManifest manifest, boolean addOptionals) {

        Vehicle vehicle = getVehicle();
        double distance = destination.getDistance() - getDistanceCovered();
        MissionVehicleProject mvp = (MissionVehicleProject) getMission();

        // Must use the same logic in all cases otherwise too few fuel will be loaded
        double amount = vehicle.getFuelNeededForTrip(distance, addOptionals);
        manifest.addResource(vehicle.getFuelTypeID(), amount, true);
        
        // if useMargin is true, include more oxygen
        manifest.addResource(ResourceUtil.oxygenID, VehicleController.RATIO_OXIDIZER_FUEL * amount,
                            true);

        if (vehicle instanceof Rover) {
            double travelDuration = mvp.getEstimateTravelTime(distance);

            addLifeSupportResource(mvp.getMembers().size(), travelDuration, addOptionals, manifest);
        }
    }

    /**
     * Distance covered so far.
     */
    public double getDistanceCovered() {
        if (startingCoordinate == null) {
            return 0D;
        }          
        return getVehicle().getCoordinates().getDistance(startingCoordinate);
    }

    /**
     * Where is the travel step taking the Misison
     * @return
     */
    public NavPoint getDestination() {
        return destination;
    }

    /**
     * Completing the travel step transfer the Vehicle to the desitnation Settlement
    */ 
    @Override
    protected void complete() {
        Settlement target = destination.getSettlement();
        Vehicle v = getVehicle();

        // If target is a settlement then park it
		if (target != null) {
            // Should already have been transferred to the Settlment but safety check
            if (!target.equals(v.getSettlement())) {
                if (v.transfer(target)) {
                    logger.warning(v, "Had to transfer as part of Travel step to " + target.getName() + ".");
                }
                else {
                    logger.warning(v, "Unable to transfer to " + target.getName() + ".");
                }
            }

            // garage it
            target.getBuildingManager().addToGarage(v);
		}
        super.complete();
    }
    
    @Override
    public String toString() {
        return "Mission " + getMission().getName() + " travel to " + destination.getDescription();
    }
}
