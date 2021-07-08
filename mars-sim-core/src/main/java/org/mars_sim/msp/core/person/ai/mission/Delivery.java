/**
 * Mars Simulation Project
 * Delivery.java
 * @version 3.2.0 2021-06-20
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.mission;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.task.EVAOperation;
import org.mars_sim.msp.core.person.ai.task.LoadVehicleEVA;
import org.mars_sim.msp.core.person.ai.task.LoadVehicleGarage;
import org.mars_sim.msp.core.person.ai.task.NegotiateDelivery;
import org.mars_sim.msp.core.person.ai.task.UnloadVehicleEVA;
import org.mars_sim.msp.core.person.ai.task.UnloadVehicleGarage;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.VehicleMaintenance;
import org.mars_sim.msp.core.structure.goods.Good;
import org.mars_sim.msp.core.structure.goods.GoodCategory;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Drone;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * A mission for delivery between two settlements. TODO externalize strings
 */
public class Delivery extends DroneMission implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(Delivery.class.getName());

	/** Default description. */
	public static final String DEFAULT_DESCRIPTION = Msg.getString("Mission.description.delivery"); //$NON-NLS-1$

	/** Mission Type enum. */
	public static final MissionType missionType = MissionType.TRADE;
	
	/** Mission phases. */
	public static final MissionPhase TRADE_DISEMBARKING = new MissionPhase(
			Msg.getString("Mission.phase.deliveryDisembarking")); //$NON-NLS-1$
	public static final MissionPhase TRADE_NEGOTIATING = new MissionPhase(
			Msg.getString("Mission.phase.deliveryNegotiating")); //$NON-NLS-1$
	public static final MissionPhase UNLOAD_GOODS = new MissionPhase(Msg.getString("Mission.phase.unloadGoods")); //$NON-NLS-1$
	public static final MissionPhase LOAD_GOODS = new MissionPhase(Msg.getString("Mission.phase.loadGoods")); //$NON-NLS-1$
	public static final MissionPhase TRADE_EMBARKING = new MissionPhase(Msg.getString("Mission.phase.deliveryEmbarking")); //$NON-NLS-1$

	// Static members
	public static final double MAX_STARTING_PROBABILITY = 100D;

	// Static cache for holding trade profit info.
	public static final Map<Settlement, DeliveryProfitInfo> TRADE_PROFIT_CACHE = new HashMap<Settlement, DeliveryProfitInfo>();
	public static final Map<Settlement, Settlement> TRADE_SETTLEMENT_CACHE = new HashMap<Settlement, Settlement>();

	static final int MAX_MEMBERS = 1;

	// Data members.
	private double profit;
	private double desiredProfit;

	private boolean outbound;
	private boolean doNegotiation;

	private Settlement tradingSettlement;
	private MarsClock startNegotiationTime;
	private NegotiateDelivery negotiationTask;

	private Map<Good, Integer> sellLoad;
	private Map<Good, Integer> buyLoad;
	private Map<Good, Integer> desiredBuyLoad;

	/**
	 * Constructor. Started by DeliveryMeta
	 * 
	 * @param startingMember the mission member starting the settlement.
	 */
	public Delivery(MissionMember startingMember) {
		// Use DroneMission constructor.
		super(DEFAULT_DESCRIPTION, missionType, startingMember);

		Settlement s = startingMember.getSettlement();
		// Set the mission capacity.
		setMissionCapacity(MAX_MEMBERS);
		int availableSuitNum = Mission.getNumberAvailableEVASuitsAtSettlement(s);
		if (availableSuitNum < getMissionCapacity()) {
			setMissionCapacity(availableSuitNum);
		}

		outbound = true;
		doNegotiation = true;

		if (!isDone() && s != null) {

			// Initialize data members
			setStartingSettlement(s);

			// Get trading settlement
			tradingSettlement = TRADE_SETTLEMENT_CACHE.get(s);
			if (tradingSettlement != null && !tradingSettlement.equals(s)) {
				addNavpoint(new NavPoint(tradingSettlement.getCoordinates(), tradingSettlement,
						tradingSettlement.getName()));
				setDescription(Msg.getString("Mission.description.delivery.detail", tradingSettlement.getName())); // $NON-NLS-1$
				TRADE_PROFIT_CACHE.remove(getStartingSettlement());
				TRADE_PROFIT_CACHE.remove(tradingSettlement);
				TRADE_SETTLEMENT_CACHE.remove(getStartingSettlement());
				TRADE_SETTLEMENT_CACHE.remove(tradingSettlement);
			} else {
				addMissionStatus(MissionStatus.NO_TRADING_SETTLEMENT);
				endMission();
			}

			if (!isDone()) {
				// Get the credit that the starting settlement has with the destination
				// settlement.
				double credit = creditManager.getCredit(s, tradingSettlement);

				if (credit > (DeliveryUtil.SELL_CREDIT_LIMIT * -1D)) {
					// Determine desired buy load,
					desiredBuyLoad = DeliveryUtil.getDesiredBuyLoad(s, getDrone(), tradingSettlement);
				} else {
					// Cannot buy from settlement due to credit limit.
					desiredBuyLoad = new HashMap<Good, Integer>(0);
				}

				if (credit < DeliveryUtil.SELL_CREDIT_LIMIT) {
					// Determine sell load.
					sellLoad = DeliveryUtil.determineBestSellLoad(s, getDrone(), tradingSettlement);
				} else {
					// Will not sell to settlement due to credit limit.
					sellLoad = new HashMap<Good, Integer>(0);
				}

				// Determine desired trade profit.
				desiredProfit = estimateDeliveryProfit(desiredBuyLoad);
			}

			// Recruit additional members to mission.
			if (!isDone()) {
				if (!recruitMembersForMission(startingMember))
					return;
			}
		}

		// Add trade mission phases.
		addPhase(TRADE_DISEMBARKING);
		addPhase(TRADE_NEGOTIATING);
		addPhase(UNLOAD_GOODS);
		addPhase(LOAD_GOODS);
		addPhase(TRADE_EMBARKING);

		// Set initial phase
		setPhase(VehicleMission.REVIEWING);
		setPhaseDescription(Msg.getString("Mission.phase.reviewing.description")); //$NON-NLS-1$
		if (logger.isLoggable(Level.INFO)) {
			if (startingMember != null && getDrone() != null) {
				logger.info(startingMember, "Starting Delivery mission on " + getDrone().getName());
			}
		}
	}

	/**
	 * Constructor 2. Started by MissionDataBean 
	 * 
	 * @param members
	 * @param startingSettlement
	 * @param tradingSettlement
	 * @param drone
	 * @param description
	 * @param sellGoods
	 * @param buyGoods
	 */
	public Delivery(Collection<MissionMember> members, Settlement startingSettlement, Settlement tradingSettlement,
			Drone drone, String description, Map<Good, Integer> sellGoods, Map<Good, Integer> buyGoods) {
		// Use DroneMission constructor.
		super(description, missionType, (MissionMember) members.toArray()[0], 2, drone);

		Person person = null;
//		Robot robot = null;

		outbound = true;
		doNegotiation = false;

		// Initialize data members
		setStartingSettlement(startingSettlement);

		// Sets the mission capacity.
		setMissionCapacity(MAX_MEMBERS);
		int availableSuitNum = Mission.getNumberAvailableEVASuitsAtSettlement(startingSettlement);
		if (availableSuitNum < getMissionCapacity()) {
			setMissionCapacity(availableSuitNum);
		}

		// Set mission destination.
		this.tradingSettlement = tradingSettlement;
		addNavpoint(new NavPoint(tradingSettlement.getCoordinates(), tradingSettlement, tradingSettlement.getName()));

		// Add mission members.
		Iterator<MissionMember> i = members.iterator();
		while (i.hasNext()) {
			MissionMember member = i.next();
			// TODO Refactor.
			if (member instanceof Person) {
				person = (Person) member;
				person.getMind().setMission(this);
			} else if (member instanceof Robot) {
//				robot = (Robot) member;
//				robot.getBotMind().setMission(this);
			}
		}

		// Set trade goods.
		sellLoad = sellGoods;
		buyLoad = buyGoods;
		desiredBuyLoad = new HashMap<Good, Integer>(buyGoods);
		profit = estimateDeliveryProfit(buyLoad);
		desiredProfit = profit;

		// Add trade mission phases.
		addPhase(TRADE_DISEMBARKING);
		addPhase(TRADE_NEGOTIATING);
		addPhase(UNLOAD_GOODS);
		addPhase(LOAD_GOODS);
		addPhase(TRADE_EMBARKING);

		// Set initial phase
		setPhase(VehicleMission.EMBARKING);
		setPhaseDescription(Msg.getString("Mission.phase.embarking.description"));//, getStartingSettlement().getName())); // $NON-NLS-1$
		if (logger.isLoggable(Level.INFO)) {
			MissionMember startingMember = (MissionMember) members.toArray()[0];
			if (startingMember != null && getDrone() != null) {
				logger.info(startingMember, "Starting Delivery mission on " + getDrone().getName());
			}
		}
	}

	/**
	 * Determines a new phase for the mission when the current phase has ended.
	 */
	protected void determineNewPhase() {
		if (REVIEWING.equals(getPhase())) {
			setPhase(VehicleMission.EMBARKING);
			setPhaseDescription(
					Msg.getString("Mission.phase.embarking.description", getCurrentNavpoint().getDescription()));//startingMember.getSettlement().toString())); // $NON-NLS-1$
		}
		
		else if (EMBARKING.equals(getPhase())) {
			startTravelToNextNode();
			setPhase(VehicleMission.TRAVELLING);
			setPhaseDescription(
					Msg.getString("Mission.phase.travelling.description", getNextNavpoint().getDescription())); // $NON-NLS-1$
		} 
		
		else if (TRAVELLING.equals(getPhase())) {
			if (getCurrentNavpoint().isSettlementAtNavpoint()) {
				if (outbound) {
					setPhase(TRADE_DISEMBARKING);
					setPhaseDescription(
							Msg.getString("Mission.phase.disembarking.description", tradingSettlement.getName())); // $NON-NLS-1$
				} else {
					setPhase(VehicleMission.DISEMBARKING);
					setPhaseDescription(Msg.getString("Mission.phase.disembarking.description",
							getCurrentNavpoint().getDescription())); // $NON-NLS-1$
				}
			}
		} 
		
		else if (TRADE_DISEMBARKING.equals(getPhase())) {
			setPhase(TRADE_NEGOTIATING);
			setPhaseDescription(
					Msg.getString("Mission.phase.deliveryNegotiating.description", tradingSettlement.getName())); // $NON-NLS-1$
		} 
		
		else if (TRADE_NEGOTIATING.equals(getPhase())) {
			setPhase(UNLOAD_GOODS);
			setPhaseDescription(Msg.getString("Mission.phase.unloadGoods.description", tradingSettlement.getName())); // $NON-NLS-1$
		} 
		
		else if (UNLOAD_GOODS.equals(getPhase())) {
			setPhase(LOAD_GOODS);
			setPhaseDescription(Msg.getString("Mission.phase.loadGoods.description", tradingSettlement.getName())); // $NON-NLS-1$
		} 
		
		else if (LOAD_GOODS.equals(getPhase())) {
			setPhase(TRADE_EMBARKING);
			setPhaseDescription(Msg.getString("Mission.phase.embarking.description", tradingSettlement.getName())); // $NON-NLS-1$
		} 
		
		else if (TRADE_EMBARKING.equals(getPhase())) {
			startTravelToNextNode();
			setPhase(VehicleMission.TRAVELLING);
			setPhaseDescription(
					Msg.getString("Mission.phase.travelling.description", getNextNavpoint().getDescription())); // $NON-NLS-1$
		} 
		
//		else if (DISEMBARKING.equals(getPhase())) {
//			endMission(ALL_DISEMBARKED);
//		}
		
		else if (DISEMBARKING.equals(getPhase())) {
			setPhase(VehicleMission.COMPLETED);
			setPhaseDescription(
					Msg.getString("Mission.phase.completed.description")); // $NON-NLS-1$
		}
		
		else if (COMPLETED.equals(getPhase())) {
			addMissionStatus(MissionStatus.MISSION_ACCOMPLISHED);
			endMission();
		}
	}

	@Override
	protected void performPhase(MissionMember member) {
		super.performPhase(member);
		if (TRADE_DISEMBARKING.equals(getPhase())) {
			performDeliveryDisembarkingPhase(member);
		} else if (TRADE_NEGOTIATING.equals(getPhase())) {
			performDeliveryNegotiatingPhase(member);
		} else if (UNLOAD_GOODS.equals(getPhase())) {
			performDestinationUnloadGoodsPhase(member);
		} else if (LOAD_GOODS.equals(getPhase())) {
			performDestinationLoadGoodsPhase(member);
		} else if (TRADE_EMBARKING.equals(getPhase())) {
			performDeliveryEmbarkingPhase(member);
		}
	}

	/**
	 * Performs the trade disembarking phase.
	 * 
	 * @param member the mission member performing the mission.
	 */
	private void performDeliveryDisembarkingPhase(MissionMember member) {
		Vehicle v = getVehicle();
		// If drone is not parked at settlement, park it.
		if ((v != null) && (v.getSettlement() == null)) {

			tradingSettlement.getInventory().storeUnit(v);
	
			// Add vehicle to a garage if available.
			if (!tradingSettlement.getBuildingManager().addToGarage(v)) {
				// or else re-orient it
				v.findNewParkingLoc();
			}
		}

		setPhaseEnded(true);
	}

	/**
	 * Perform the trade negotiating phase.
	 * 
	 * @param member the mission member performing the phase.
	 */
	private void performDeliveryNegotiatingPhase(MissionMember member) {
		if (doNegotiation) {
//			if (member == getMissionDelivery()) {
				if (negotiationTask != null) {
					if (negotiationTask.isDone()) {
						buyLoad = negotiationTask.getBuyLoad();
						profit = estimateDeliveryProfit(buyLoad);
						fireMissionUpdate(MissionEventType.BUY_LOAD_EVENT);
						setPhaseEnded(true);
					}
				} 
				
				else {
					MarsClock currentTime = (MarsClock) Simulation.instance().getMasterClock().getMarsClock().clone();
					
					if (startNegotiationTime == null) {
						startNegotiationTime = currentTime;
					}
					
					Person settlementTrader = getSettlementTrader();
					
					if (settlementTrader != null) {
						boolean assigned = false;
						
						for (MissionMember mm: getMembers()) {
							
							if (mm instanceof Person) {
								Person person = (Person) mm;
								negotiationTask = new NegotiateDelivery(tradingSettlement, getStartingSettlement(), getDrone(),
										sellLoad, person, settlementTrader);
								assigned = assignTask(person, negotiationTask);
							}
							
							if (assigned)
								break;	
						}
						
					} else {

						double timeDiff = MarsClock.getTimeDiff(currentTime, startNegotiationTime);
						
						if (timeDiff > 1000D) {
							buyLoad = new HashMap<Good, Integer>(0);
							profit = 0D;
							fireMissionUpdate(MissionEventType.BUY_LOAD_EVENT);
							setPhaseEnded(true);
						}
					}
				}
//			}
		} else {
			setPhaseEnded(true);
		}

		if (getPhaseEnded()) {
			outbound = false;
			equipmentNeededCache = null;
			addNavpoint(new NavPoint(getStartingSettlement().getCoordinates(), getStartingSettlement(),
					getStartingSettlement().getName()));
			TRADE_PROFIT_CACHE.remove(getStartingSettlement());
		}
	}

	/**
	 * Perform the unload goods phase.
	 * 
	 * @param member the mission member performing the phase.
	 */
	private void performDestinationUnloadGoodsPhase(MissionMember member) {

		// Unload drone if necessary.
		boolean unloaded = getDrone().getInventory().getTotalInventoryMass(false) == 0D;
		if (!unloaded) {
			// Alert the people in the disembarked settlement to unload cargo
			for (Person person: tradingSettlement.getIndoorPeople()) {
				if (person.isInSettlement()) {
					// Random chance of having person unload (this allows person to do other things
					// sometimes)
					if (RandomUtil.lessThanRandPercent(50)) {
						if (isInAGarage()) {
							assignTask(person, new UnloadVehicleGarage(person, getDrone()));
						} 
						
						else {
							// Check if it is day time.
							if (!EVAOperation.isGettingDark(person) && person.isFit()) {
								assignTask(person, new UnloadVehicleEVA(person, getDrone()));
							}
						}
	
						return;
					}
				}
			}
		} else {
			setPhaseEnded(true);
		}
	}

	/**
	 * Performs the load goods phase.
	 * 
	 * @param member the mission member performing the phase.
	 */
	private void performDestinationLoadGoodsPhase(MissionMember member) {

		if (!isDone() && !isVehicleLoaded()) {

			// Check if vehicle can hold enough supplies for mission.
			if (isVehicleLoadable()) {
				
				for (Person person: tradingSettlement.getIndoorPeople()) {
					if (person.isInSettlement()) {// .getLocationSituation() == LocationSituation.IN_SETTLEMENT) {
						// Random chance of having person load (this allows person to do other things
						// sometimes)
						if (RandomUtil.lessThanRandPercent(50)) {
							if (isInAGarage()) {
								assignTask(person,
									new LoadVehicleGarage(person, getVehicle(), getRequiredResourcesToLoad(),
													getOptionalResourcesToLoad(), getRequiredEquipmentToLoad(),
													getOptionalEquipmentToLoad()));

							} else {
								// Check if it is day time.
								if (!EVAOperation.isGettingDark(person)) {
										assignTask(person,
												new LoadVehicleEVA(person, getVehicle(), getRequiredResourcesToLoad(),
														getOptionalResourcesToLoad(), getRequiredEquipmentToLoad(),
														getOptionalEquipmentToLoad()));
								}
							}
							
							return;
						}
					}
				}
			} else {
				addMissionStatus(MissionStatus.VEHICLE_NOT_LOADABLE);
				endMission();
			}
		} else {
			setPhaseEnded(true);
		}
	}

	/**
	 * Performs the trade embarking phase.
	 * 
	 * @param member the mission member performing the phase.
	 */
	private void performDeliveryEmbarkingPhase(MissionMember member) {

		// If person is not aboard the drone, board drone.
		if (!isDone() && !member.isInVehicle()) {

			if (member instanceof Person) {
				Person trader = (Person) member;
				if (trader.isDeclaredDead()) {
					logger.info(trader, "The person is no longer alive.");
					int bestSkillLevel = 0;
					// Pick another member to head the delivery
					for (MissionMember mm: getMembers()) {
						if (member instanceof Person) {
							Person p = (Person) mm;
							if (!p.isDeclaredDead()) {
								int level = p.getSkillManager().getSkillExp(SkillType.TRADING);
								if (level > bestSkillLevel) {
									bestSkillLevel = level;
									trader = p;
									setStartingMember(p);
									break;
								}
							}
						}
						else {
							setStartingMember(mm);
							break;
						}
					}
				}
			}
		}

		// If drone is loaded and everyone is aboard, embark from settlement.
		if (!isDone()) {

			// Remove from garage if in garage.
			Building garageBuilding = BuildingManager.getBuilding(getVehicle());
			if (garageBuilding != null) {
				VehicleMaintenance garage = garageBuilding.getVehicleMaintenance();
				garage.removeVehicle(getVehicle());
			}

			// Embark from settlement
			tradingSettlement.getInventory().retrieveUnit(getVehicle());
			setPhaseEnded(true);
		}
	}

	@Override
	protected void performEmbarkFromSettlementPhase(MissionMember member) {
		super.performEmbarkFromSettlementPhase(member);
	}

	@Override
	protected void performDisembarkToSettlementPhase(MissionMember member, Settlement disembarkSettlement) {
		super.performDisembarkToSettlementPhase(member, disembarkSettlement);
	}

	@Override
	public void endMission() {
		super.endMission();

//		// Unreserve any towed vehicles.
//		if (getDrone() != null) {
//			if (getDrone().getTowedVehicle() != null) {
//				Vehicle towed = getDrone().getTowedVehicle();
//				towed.setReservedForMission(false);
//			}
//		}
	}

	/**
	 * Gets the type of vehicle in a load.
	 * 
	 * @param buy true if buy load, false if sell load.
	 * @return vehicle type or null if none.
	 */
	private String getLoadVehicleType(boolean buy) {
		String result = null;

		Map<Good, Integer> load = null;
		if (buy) {
			load = buyLoad;
		} else {
			load = sellLoad;
		}

		Iterator<Good> i = load.keySet().iterator();
		while (i.hasNext()) {
			Good good = i.next();
			if (good.getCategory().equals(GoodCategory.VEHICLE)) {
				result = good.getName();
			}
		}

		return result;
	}

	/**
	 * Gets the initial load vehicle.
	 * 
	 * @param vehicleType the vehicle type string.
	 * @param buy         true if buying load, false if selling load.
	 * @return load vehicle.
	 */
	private Vehicle getInitialLoadVehicle(String vehicleType, boolean buy) {
		Vehicle result = null;

		if (vehicleType != null) {
			Settlement settlement = null;
			if (buy) {
				settlement = tradingSettlement;
			} else {
				settlement = getStartingSettlement();
			}

			Iterator<Vehicle> j = settlement.getParkedVehicles().iterator();
			while (j.hasNext()) {
				Vehicle vehicle = j.next();
				boolean isEmpty = vehicle.getInventory().isEmpty(false);
				if (vehicleType.equalsIgnoreCase(vehicle.getDescription())) {
					if ((vehicle != getVehicle()) && !vehicle.isReserved() && isEmpty) {
						result = vehicle;
					}
				}
			}
		}

		return result;
	}

	@Override
	public Map<Integer, Integer> getOptionalEquipmentToLoad() {

		Map<Integer, Integer> result = super.getOptionalEquipmentToLoad();

		// Add buy/sell load.
		Map<Good, Integer> load = null;
		if (outbound) {
			load = sellLoad;
		} else {
			load = buyLoad;
		}

		Iterator<Good> i = load.keySet().iterator();
		while (i.hasNext()) {
			Good good = i.next();
			if (good.getCategory().equals(GoodCategory.EQUIPMENT)) {
//				Class<?> equipmentClass = good.getClassType();
				int num = load.get(good);
				int id = good.getID();//EquipmentType.getEquipmentID(equipmentClass);
				if (result.containsKey(id)) {
					num += (Integer) result.get(id);
				}
				result.put(id, num);
//                result.put(ResourceUtil.findIDbyAmountResourceName(equipmentClass.getName()), num);
			}
		}

		return result;
	}

	@Override
	public Map<Integer, Number> getOptionalResourcesToLoad() {

		Map<Integer, Number> result = new HashMap<>();//super.getOptionalResourcesToLoad();

		// Add buy/sell load.
		Map<Good, Integer> load = null;
		if (outbound) {
			load = sellLoad;
		} else {
			load = buyLoad;
		}

		Iterator<Good> i = load.keySet().iterator();
		while (i.hasNext()) {
			Good good = i.next();
			if (good.getCategory().equals(GoodCategory.AMOUNT_RESOURCE)) {
//				AmountResource resource = (AmountResource) good.getObject();
				int id = good.getID();//resource.getID();
				double amount = load.get(good).doubleValue();
				if (result.containsKey(id)) {
					amount += (Double) result.get(id);
				}
				result.put(id, amount);
			} else if (good.getCategory().equals(GoodCategory.ITEM_RESOURCE)) {
//				ItemResource resource = (ItemResource) good.getObject();
				int id = good.getID();//resource.getID();
				int num = load.get(good);
				if (result.containsKey(id)) {
					num += (Integer) result.get(id);
				}
				result.put(id, num);
			}
		}

		return result;
	}

	@Override
	public Settlement getAssociatedSettlement() {
		return getStartingSettlement();
	}

	@Override
	protected int compareVehicles(Vehicle firstVehicle, Vehicle secondVehicle) {
		int result = super.compareVehicles(firstVehicle, secondVehicle);

		if ((result == 0) && isUsableVehicle(firstVehicle) && isUsableVehicle(secondVehicle)) {
			// Check if one has more general cargo capacity than the other.
			double firstCapacity = firstVehicle.getInventory().getGeneralCapacity();
			double secondCapacity = secondVehicle.getInventory().getGeneralCapacity();
			if (firstCapacity > secondCapacity) {
				result = 1;
			} else if (secondCapacity > firstCapacity) {
				result = -1;
			}

			// Vehicle with superior range should be ranked higher.
			if (result == 0) {
				if (firstVehicle.getRange(missionType) > secondVehicle.getRange(missionType)) {
					result = 1;
				} else if (firstVehicle.getRange(missionType) < secondVehicle.getRange(missionType)) {
					result = -1;
				}
			}
		}

		return result;
	}

	/**
	 * Gets the trader for the mission.
	 * 
	 * @return the trader.
	 */
	private Person getMissionDelivery() {
		Person bestDeliveryr = null;
		int bestDeliverySkill = -1;

		Iterator<MissionMember> i = getMembers().iterator();
		while (i.hasNext()) {
			MissionMember member = i.next();
			if (member instanceof Person) {
				Person person = (Person) member;
				int tradeSkill = person.getSkillManager().getEffectiveSkillLevel(SkillType.TRADING);
				if (tradeSkill > bestDeliverySkill) {
					bestDeliverySkill = tradeSkill;
					bestDeliveryr = person;
				}
			}
		}

		return bestDeliveryr;
	}

	/**
	 * Gets the trader and the destination settlement for the mission.
	 * 
	 * @return the trader.
	 */
	private Person getSettlementTrader() {
		Person bestDeliveryr = null;
		int bestDeliverySkill = -1;

		Iterator<Person> i = tradingSettlement.getIndoorPeople().iterator();
		while (i.hasNext()) {
			Person person = i.next();
			if (!getMembers().contains(person)) {
				int tradeSkill = person.getSkillManager().getEffectiveSkillLevel(SkillType.TRADING);
				if (tradeSkill > bestDeliverySkill) {
					bestDeliverySkill = tradeSkill;
					bestDeliveryr = person;
				}
			}
		}

		return bestDeliveryr;
	}

	/**
	 * Gets the load that is being sold in the trade.
	 * 
	 * @return sell load.
	 */
	public Map<Good, Integer> getSellLoad() {
		if (sellLoad != null) {
			return new HashMap<Good, Integer>(sellLoad);
		} else {
			return null;
		}
	}

	/**
	 * Gets the load that is being bought in the trade.
	 * 
	 * @return buy load.
	 */
	public Map<Good, Integer> getBuyLoad() {
		if (buyLoad != null) {
			return new HashMap<Good, Integer>(buyLoad);
		} else {
			return null;
		}
	}

	/**
	 * Gets the profit for the settlement initiating the trade.
	 * 
	 * @return profit (VP).
	 */
	public double getProfit() {
		return profit;
	}

	/**
	 * Gets the load that the starting settlement initially desires to buy.
	 * 
	 * @return desired buy load.
	 */
	public Map<Good, Integer> getDesiredBuyLoad() {
		if (desiredBuyLoad != null) {
			return new HashMap<Good, Integer>(desiredBuyLoad);
		} else {
			return null;
		}
	}

	/**
	 * Gets the profit initially expected by the starting settlement.
	 * 
	 * @return desired profit (VP).
	 */
	public double getDesiredProfit() {
		return desiredProfit;
	}

	/**
	 * Gets the settlement that the starting settlement is trading with.
	 * 
	 * @return trading settlement.
	 */
	public Settlement getTradingSettlement() {
		return tradingSettlement;
	}

	/**
	 * Estimates the profit for the starting settlement for a given buy load.
	 * 
	 * @param buyingLoad the load to buy.
	 * @return profit (VP).
	 */
	private double estimateDeliveryProfit(Map<Good, Integer> buyingLoad) {
		double result = 0D;

		try {
			double sellingValueHome = DeliveryUtil.determineLoadValue(sellLoad, getStartingSettlement(), false);
			double sellingValueRemote = DeliveryUtil.determineLoadValue(sellLoad, tradingSettlement, true);
			double sellingProfit = sellingValueRemote - sellingValueHome;

			double buyingValueHome = DeliveryUtil.determineLoadValue(buyingLoad, getStartingSettlement(), true);
			double buyingValueRemote = DeliveryUtil.determineLoadValue(buyingLoad, tradingSettlement, false);
			double buyingProfit = buyingValueHome - buyingValueRemote;

			double totalProfit = sellingProfit + buyingProfit;

			double estimatedDistance = Coordinates.computeDistance(getStartingSettlement().getCoordinates(), 
					tradingSettlement.getCoordinates()) * 2D;
			double missionCost = DeliveryUtil.getEstimatedMissionCost(getStartingSettlement(), getDrone(),
					estimatedDistance);

			result = totalProfit - missionCost;
		} catch (Exception e) {
			e.printStackTrace(System.err);
			addMissionStatus(MissionStatus.COULD_NOT_ESTIMATE_TRADE_PROFIT);
			endMission();
		}

		return result;
	}

	@Override
	public void destroy() {
		super.destroy();

		tradingSettlement = null;
		if (sellLoad != null)
			sellLoad.clear();
		sellLoad = null;
		if (buyLoad != null)
			buyLoad.clear();
		buyLoad = null;
		if (desiredBuyLoad != null)
			desiredBuyLoad.clear();
		desiredBuyLoad = null;
		startNegotiationTime = null;
		negotiationTask = null;
	}

	/**
	 * Inner class for storing trade profit info.
	 */
	public static class DeliveryProfitInfo {
		
		public double profit;
		public MarsClock time;

		public DeliveryProfitInfo(double profit, MarsClock time) {
			this.profit = profit;
			this.time = time;
		}
	}

	@Override
	public Map<Integer, Integer> getEquipmentNeededForRemainingMission(boolean useBuffer) {
		if (equipmentNeededCache != null)
			return equipmentNeededCache;
		else {
			Map<Integer, Integer> result = new HashMap<>(0);
			equipmentNeededCache = result;
			return result;
		}
	}
}
