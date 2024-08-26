/*
 * Mars Simulation Project
 * LoadingController.java
 * @date 2021-10-21
 * @author Barry Evans
 */
package com.mars_sim.core.vehicle.task;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.mars_sim.core.equipment.Equipment;
import com.mars_sim.core.equipment.EquipmentType;
import com.mars_sim.core.goods.Good;
import com.mars_sim.core.goods.GoodsUtil;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.ai.NaturalAttributeType;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.resource.ItemResourceUtil;
import com.mars_sim.core.resource.Part;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.resource.SuppliesManifest;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.vehicle.StatusType;
import com.mars_sim.core.vehicle.Vehicle;

/**
 * This class control the loading of a Vehicle of resources.
 * It creates a manifest that is depleted as resources are loaded.
 */
public class LoadingController implements Serializable {

	private static final long serialVersionUID = 1L;

	/**
	 * The amount of resources (kg) one person of average strength can load per
	 * millisol.
	 */
	private static final double LOAD_RATE = 20D;

	// Resources that can be loaded in the background
	private static final int[] BACKGROUND_RESOURCES = {
			ResourceUtil.oxygenID,
			ResourceUtil.waterID,
			ResourceUtil.methanolID
	};

	// Avoid transferring micro-small amount
	private static final double SMALLEST_RESOURCE_LOAD = 0.01;

	// Have to limit the precision of the amount loading to avoid
	// problem with the double precision
	private static final double AMOUNT_BASE = 1000000D;

	// The number of times to attempt to get mandatory resources from a settlement
	public static final int MAX_SETTLEMENT_ATTEMPTS = 5;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(LoadingController.class.getName());

	private Map<Integer, Number> resourcesManifest;
	private Map<Integer, Number> optionalResourcesManifest;
	private Map<Integer, Integer> equipmentManifest;
	private Map<Integer, Integer> optionalEquipmentManifest;
	private Settlement settlement;
	private Vehicle vehicle;

	// The number of times a settlement can not have the resources
	private int retryAttempts = MAX_SETTLEMENT_ATTEMPTS;

	private boolean vehicleFull = false;

	/**
	 * Load a vehicle with a manifest from a Settlement
	 * @param settlement Source of resources for the load
	 * @param vehicle Vehicle to load
	 * @param manifest Manifest of supplies required
	 */
	public LoadingController(Settlement settlement, Vehicle vehicle,
							 SuppliesManifest manifest) {
		this.settlement = settlement;
		this.vehicle = vehicle;

		// Take copies to form the manifest as the quantities will be reduced
		this.resourcesManifest = new HashMap<>(manifest.getResources(true));
		this.optionalResourcesManifest = new HashMap<>(manifest.getResources(false));
		this.equipmentManifest = new HashMap<>(manifest.getEquipment(true));
		this.optionalEquipmentManifest = new HashMap<>(manifest.getEquipment(false));

		// Reduce what is already in the Vehicle
		removeVehicleResources(resourcesManifest);
		removeVehicleResources(optionalResourcesManifest);
		removeVehicleEquipment(equipmentManifest);
		removeVehicleEquipment(optionalEquipmentManifest);
	}

	/*
	 * Remove any equipment the Vehicle has from the manifest. Equipment completely loaded
	 * in the vehicle will be removed from the manifest.
	 */
	private void removeVehicleEquipment(Map<Integer, Integer> equipment) {
		Set<Integer> ids = new HashSet<>(equipment.keySet());
		for (Integer eqmId : ids) {
			EquipmentType eType = EquipmentType.convertID2Type(eqmId);
			int amountLoaded = vehicle.findNumEmptyContainersOfType(eType, false);
			if (amountLoaded > 0) {
				int newAmount = equipment.get(eqmId).intValue() - amountLoaded;
				if (newAmount <= 0D) {
					equipment.remove(eqmId);
				}
				else {
					equipment.put(eqmId, newAmount);
				}
			}
		}
	}

	/*
	 * Remove any resources the Vehicle has from the manifest. Resource completely loaded
	 * in the vehicle will be removed from the manifest.
	 */
	private void removeVehicleResources(Map<Integer, Number> resources) {
		Set<Integer> ids = new HashSet<>(resources.keySet());
		for (Integer resourceId : ids) {
			double amountLoaded;
			if (resourceId < ResourceUtil.FIRST_ITEM_RESOURCE_ID) {
				// Load amount resources
				amountLoaded = vehicle.getAmountResourceStored(resourceId);
				double capacity = vehicle.getAmountResourceCapacity(resourceId);
				double amountRequired = resources.get(resourceId).doubleValue();
				if (capacity < amountRequired) {
					// So the vehicle can not handle the Manifest volume
					// Adjust the manifest down
					resources.put(resourceId, (capacity - amountLoaded));
					logger.warning(vehicle, "Could not hold "
							+ amountRequired + " kg "
							+ ResourceUtil.findAmountResourceName(resourceId)
							+ " from the manifest "
							+ "(Capacity: " + capacity + " kg).");
					amountLoaded = 0;
				}
			}
			else {
				// Load item resources
				amountLoaded = vehicle.getItemResourceStored(resourceId);
			}

			if (amountLoaded > 0) {
				double newAmount = resources.get(resourceId).doubleValue() - amountLoaded;
				if (newAmount <= 0D) {
					resources.remove(resourceId);
				}
				else {
					resources.put(resourceId, newAmount);
				}
			}
		}
	}

	/**
	 * Load resources by a worker
	 * @param worker
	 * @param time How much time does the Worker have
	 * @return Load completed
	 */
	public boolean load(Worker worker, double time) {
		int strength = worker.getNaturalAttributeManager().getAttribute(NaturalAttributeType.STRENGTH);
		double strengthModifier = .1D + (strength * .018D);
		double amountLoading = Math.round((LOAD_RATE * strengthModifier * time / 12D) * AMOUNT_BASE) / AMOUNT_BASE;

		// Temporarily remove rover from settlement so that inventory doesn't get mixed
		// in.
		boolean vehicleInSettlement = false;
		if (settlement.containsVicinityParkedVehicle(vehicle)) {
			vehicleInSettlement = true;
			settlement.removeVicinityParkedVehicle(vehicle);
		}

		// Load equipment
		if ((amountLoading > 0D) && !equipmentManifest.isEmpty()) {
			amountLoading = loadEquipment(amountLoading, equipmentManifest, true);
		}

		// Load resources
		if ((amountLoading > 0D) && !resourcesManifest.isEmpty()) {
			amountLoading = loadResources(worker, amountLoading, resourcesManifest, true);
		}

		// Load optionals last
		if ((amountLoading > 0D) && !optionalResourcesManifest.isEmpty()) {
			amountLoading = loadResources(worker, amountLoading, optionalResourcesManifest, false);
		}

		// Load optional equipment
		if ((amountLoading > 0D) && !optionalEquipmentManifest.isEmpty()) {
			amountLoading = loadEquipment(amountLoading, optionalEquipmentManifest, false);
		}

		// Put rover back into settlement.
		if (vehicleInSettlement) {
			settlement.addVicinityVehicle(vehicle);
		}

		// Should the load stop for this worker? Either fully loaded or did not
		// use load amount (that means load couldn't complete it
		boolean completed = isCompleted();
		
		// If not completed then check that the vehcile still has a capacity
		if (!completed) {
			vehicleFull = vehicle.getRemainingCargoCapacity() < 10D;
			if (vehicleFull) {
				completed = true;
				logger.warning(vehicle, "Vehicle full so loading completed");
			}
		}

		if (completed) {
			// Can remove assume fuel is reloaded
			vehicle.removeSecondaryStatus(StatusType.OUT_OF_FUEL);
			vehicle.setLoading(null);
			logger.fine(vehicle, "Loading completed by " + worker.getName());
		}
		return (amountLoading > 0D) || completed;
	}

	/**
	 * Loads the vehicle with required resources from the settlement.
	 *
	 * @param amountLoading the amount (kg) the person can load in this time period.
	 * @param manifest Manfiest to load from
	 * @return the remaining amount (kg) the person can load in this time period.
	 * @throws Exception if problem loading resources.
	 */
	private double loadResources(Worker loader, double amountLoading, Map<Integer, Number> manifest, boolean mandatory) {

		String  loaderName = loader.getName();

		// Load required resources. Take a cope as load will change the
		// manifest
		Set<Integer> resources = new HashSet<>(manifest.keySet());
		for(Integer resource : resources) {
			if (resource < ResourceUtil.FIRST_ITEM_RESOURCE_ID) {
				// Load amount resources
				amountLoading = loadAmountResource(loaderName, amountLoading, resource, manifest, mandatory);
			}
			else {
				// Load item resources
				amountLoading = loadItemResource(loaderName, amountLoading, resource, manifest, mandatory);
			}

			// Exhausted the loading amount
			if (amountLoading <= 0D) {
				return 0;
			}
		}
		// Return remaining amount that can be loaded by person this time period.
		return amountLoading;
	}

	/**
	 * Loads the vehicle with an amount resource from the settlement.
	 *
	 * @param loader		Entity doing the loading
	 * @param amountLoading the amount (kg) the person can load in this time period.
	 * @param resource      the amount resource to be loaded.
	 * @param manifest		The Resources being loaded
	 * @param mandatory     true if the amount resource is required to load, false
	 *                      if optional.
	 * @return the remaining amount (kg) the person can load in this time period.
	 */
	private double loadAmountResource(String loader, double amountLoading, Integer resource,
									  Map<Integer, Number> manifest, boolean mandatory) {

		String resourceName = ResourceUtil.findAmountResourceName(resource);

		// Determine amount to load.
		boolean usedSupply = false;
		double amountNeeded = manifest.get(resource).doubleValue();
		double amountToLoad = Math.min(amountNeeded, amountLoading);

		if (amountToLoad > 0) {
			// Check if enough resource in settlement inventory.
			double settlementStored = settlement.getAmountResourceStored(resource);

			// Settlement has enough stored resource?
			if (settlementStored < amountToLoad) {
				if (mandatory) {
					retryAttempts--;
					logger.warning(vehicle, "Not enough available for loading "
							+ Math.round(amountToLoad * 100D) / 100D
							+ " kg " + resourceName
							+ ". Settlement has "
							+ Math.round(settlementStored * 100D) / 100D
							+ " kg. will try " + retryAttempts + " more times.");
					return amountLoading;
				}
				else {
					usedSupply = true;
					amountToLoad = settlementStored;
				}
			}

			// Check remaining capacity in vehicle inventory.
			double remainingCapacity = vehicle.getAmountResourceRemainingCapacity(resource);
			if (remainingCapacity < amountToLoad) {
				if (remainingCapacity < SMALLEST_RESOURCE_LOAD) {
					// Nothing left for this type so stop loading this resource
					amountNeeded = 0;
					logger.warning(vehicle, "Capacity exhausted in loading "
									+ resourceName);
				}
				else if (mandatory && ((amountToLoad - remainingCapacity) > SMALLEST_RESOURCE_LOAD)) {
					// Will load up as much required resource as possible
					logger.warning(vehicle, "Not enough capacity for loading "
							+ Math.round(amountToLoad * 100D) / 100D + " kg "
							+ resourceName
							+ ". Vehicle remaining cap: "
							+ Math.round(remainingCapacity * 100D) / 100D + " kg.");
				}
				usedSupply = true;
				amountToLoad = remainingCapacity;
			}

			// Load resource from settlement inventory to vehicle inventory.
			if (amountToLoad > 0) {
				// Take resource from the settlement
				settlement.retrieveAmountResource(resource, amountToLoad);
				// Store resource in the vehicle
				vehicle.storeAmountResource(resource, amountToLoad);
			}
		}

		// Check if this resource is complete
		amountNeeded -= amountToLoad;
		if (amountNeeded <= SMALLEST_RESOURCE_LOAD) {
			manifest.remove(resource);
		}
		else if (!mandatory && usedSupply) {
			logger.info(vehicle, loader + " could not load " + resourceName
						+ ", returning " + Math.round(amountNeeded * 100D) / 100D + " kg.");
			manifest.remove(resource);
		}
		else {
			manifest.put(resource, amountNeeded);
		}

		// Return remaining amount that can be loaded by person this time period.
		return amountLoading - amountToLoad;
	}


	/**
	 * Loads the vehicle with an item resource from the settlement.
	 *
	 * @param loader        Entity doing the loading
	 * @param amountLoading the amount (kg) the person can load in this time period.
	 * @param manifest
	 * @param resource      the item resource to be loaded.
	 * @param mandatory      true if the item resource is required to load, false if
	 *                      optional.
	 * @return the remaining amount (kg) the person can load in this time period.
	 */
	private double loadItemResource(String loader, double amountLoading, Integer id, Map<Integer, Number> manifest, boolean mandatory) {

		Part p = ItemResourceUtil.findItemResource(id);
		boolean usedSupply = false;
		boolean noCapacity = false;
		
		// Determine number to load. Could load at least one
		// Part if needed
		int amountNeeded = (int)manifest.get(id).doubleValue();
		int amountCouldLoad = Math.max(1, (int)(amountLoading/p.getMassPerItem()));
		int amountToLoad = Math.min(amountNeeded, amountCouldLoad);
		if (amountToLoad > 0) {
			// Check if enough resource in settlement inventory.
			int settlementStored = settlement.getItemResourceStored(id);

			// Settlement has enough stored resource?
			if (settlementStored < amountToLoad) {
				if (mandatory) {
					retryAttempts--;
					logger.warning(vehicle, "Not enough available for loading "
							+ amountToLoad + "x "
							+ p.getName()
							+ ". Settlement has "
							+ settlementStored + " will retry for another " + retryAttempts);
				}
				amountToLoad = settlementStored;
				usedSupply = true;
			}

			// Check remaining capacity in vehicle inventory.
			double remainingMassCapacity = vehicle.getRemainingCargoCapacity();
			if (remainingMassCapacity < 0D) {
				remainingMassCapacity = 0D;
			}
			double loadingMass = amountToLoad * p.getMassPerItem();
			if (remainingMassCapacity < loadingMass) {
				if (mandatory) {
					// Will load up as much required resource as possible
					logger.warning(vehicle, "Not enough capacity for loading "
							+ Math.round(loadingMass * 100D) / 100D + " kg "
							+ p.getName()
							+ ". Vehicle remaining cap: "
							+ Math.round(remainingMassCapacity * 100D) / 100D + " kg.");
				}
				amountToLoad = (int) Math.floor(remainingMassCapacity/p.getMassPerItem());
				noCapacity = (amountToLoad == 0);
			}

			// Load item from settlement inventory to vehicle inventory.
			try {
				// Take resource from the settlement
				settlement.retrieveItemResource(id, amountToLoad);
				// Store resource in the vehicle
				vehicle.storeItemResource(id, amountToLoad);
			} catch (Exception e) {
				logger.severe(vehicle, "Cannot transfer Item from settlement to vehicle: ", e);
				return amountLoading;
			}
		}

		// Check if this resource is complete
		amountNeeded -= amountToLoad;
		if (amountNeeded == 0) {
			logger.fine(vehicle, loader + " completed loading item " + p.getName());
			manifest.remove(id);
		}
		// If it's optional and attempted to load something then remove it.
		else if ((!mandatory && usedSupply) || noCapacity) {
			logger.warning(vehicle, loader + " item " + p.getName()
						+ ", " + amountNeeded + " not loaded ");
			manifest.remove(id);
		}
		else {
			manifest.put(id, amountNeeded);
		}

		// Return remaining amount that can be loaded by person this time period.
		return amountLoading - (amountToLoad * p.getMassPerItem());
	}

	/**
	 * Loads the vehicle with equipment out of a manifest from the settlement.
	 *
	 * @param amountLoading the amount (kg) the person can load in this time period.
	 * @param mandatory Are these items needed
	 * @param manifest
	 * @return the remaining amount (kg) the person can load in this time period.
	 */
	private double loadEquipment(double amountLoading, Map<Integer, Integer> manifest, boolean mandatory) {

		Set<Integer> eqmIds = new HashSet<>(manifest.keySet());
		for(Integer equipmentType : eqmIds) {
			int amountNeeded = manifest.get(equipmentType);
			if (amountNeeded > 0) {
				// How many available ?
				EquipmentType eType = EquipmentType.convertID2Type(equipmentType);
				List<Equipment> list = new ArrayList<>(settlement.getContainerSet(eType));
				for(Equipment eq : list) {
					if (eq.isEmpty(true)) {
						// Put this equipment into a vehicle
						boolean done = eq.transfer(vehicle);

						if (!done) {
                			logger.warning(vehicle, "Cannot store Equipment " + eq.getName());
						}
						else {
							amountLoading -= eq.getMass();
							amountNeeded--;

							// Check can still keep going
							if ((amountNeeded == 0) || (amountLoading <= 0D)) {
								break;
							}
						}
					}
				}
			}

			// Update the manifest
			if (amountNeeded == 0) {
				logger.fine(vehicle, "Completed loading equipment " + equipmentType);
				manifest.remove(equipmentType);
			}
			else if (!mandatory && (amountLoading > 0D)) {
				// For optional and still have capacity to load so abort
				logger.fine(vehicle, "Optional equipment " + equipmentType + " not loaded " + amountNeeded);
				manifest.remove(equipmentType);
			}
			else {
				manifest.put(equipmentType, amountNeeded);
			}

			// No more load effort left
			if (amountLoading <= 0D) {
				return 0D;
			}
		}

		// Return remaining amount that can be loaded by person this time period.
		return amountLoading;
	}

	/**
	 * This is called in the background from the Drone/Rover time pulse method
	 * to load resources. But why is it different to the loadResources above,
	 * it uses a different definition of resources needed
	 * @param amountLoading
	 */
	public void backgroundLoad(double amountLoading) {

		for (Integer id: BACKGROUND_RESOURCES) {
			if (resourcesManifest.containsKey(id)) {
				// Load this resource
				loadAmountResource("Background", amountLoading, id, resourcesManifest, true);
			}
		}
	}

	/**
	 * Is the loading plan completed for the mandatory details.
	 * @return
	 */
	public boolean isCompleted() {
		// Manifest is empty so complete
		return (resourcesManifest.isEmpty() && equipmentManifest.isEmpty()
				&& optionalResourcesManifest.isEmpty() && optionalEquipmentManifest.isEmpty())
				|| vehicleFull;
	}

	/**
	 * Has the load failed on the mandatory items?
	 * @return
	 */
	public boolean isFailure() {
		return retryAttempts <= 0;
	}

	public Map<Integer, Number> getResourcesManifest() {
		return Collections.unmodifiableMap(this.resourcesManifest);
	}

	public Map<Integer, Number> getOptionalResourcesManifest() {
		return Collections.unmodifiableMap(this.optionalResourcesManifest);
	}

	public Map<Integer, Integer> getEquipmentManifest() {
		return Collections.unmodifiableMap(this.equipmentManifest);
	}

	public Map<Integer, Integer> getOptionalEquipmentManifest() {
		return Collections.unmodifiableMap(this.optionalEquipmentManifest);
	}

	/**
	 * Returns the settlement providing the resources for the load.
	 * 
	 * @return
	 */
	public Settlement getSettlement() {
		return settlement;
	}

	/**
	 * The vehicle that is the target of this loading plan
	 * @return
	 */
    public Vehicle getVehicle() {
		return vehicle;
    }

	/**
	 * Dumps a description of the contents into a String representation.
	 * 
	 * @return String description of contents.
	 */
	public String dumpContents() {
		StringBuilder buffer = new StringBuilder();
		buffer.append("Resources:");
		outputResourceManifest(resourcesManifest, buffer);		
		buffer.append("Optional Resources:");
		outputResourceManifest(optionalResourcesManifest, buffer);	

		return buffer.toString();
	}

	private static void outputResourceManifest(Map<Integer, Number> manifest, StringBuilder buffer) {
		for (Entry<Integer, Number> i : manifest.entrySet()) {
			Good g = GoodsUtil.getGood(i.getKey());
			buffer.append(g.getName()).append("=").append(i.getValue().toString()).append(' ');
		}
		buffer.append("\n");
	}
}
