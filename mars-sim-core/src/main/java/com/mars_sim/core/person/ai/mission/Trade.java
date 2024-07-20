/*
 * Mars Simulation Project
 * Trade.java
 * @date 2021-10-20
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.mission;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.mars_sim.core.LocalAreaUtil;
import com.mars_sim.core.equipment.EVASuitUtil;
import com.mars_sim.core.goods.CommerceMission;
import com.mars_sim.core.goods.CommerceUtil;
import com.mars_sim.core.goods.Deal;
import com.mars_sim.core.goods.Good;
import com.mars_sim.core.goods.GoodCategory;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.person.ai.task.NegotiateTrade;
import com.mars_sim.core.person.ai.task.Walk;
import com.mars_sim.core.person.ai.task.WalkingSteps;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.structure.ObjectiveType;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.core.vehicle.StatusType;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.mapdata.location.LocalPosition;

/**
 * A mission for trading between two settlements.
 */
public class Trade extends RoverMission implements CommerceMission {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(Trade.class.getName());

	/** Mission phases. */
	private static final MissionPhase TRADE_DISEMBARKING = new MissionPhase("Mission.phase.tradeDisembarking");
	private static final MissionPhase TRADE_NEGOTIATING = new MissionPhase("Mission.phase.tradeNegotiating");
	public static final MissionPhase UNLOAD_GOODS = new MissionPhase("Mission.phase.unloadGoods");
	public static final MissionPhase LOAD_GOODS = new MissionPhase("Mission.phase.loadGoods");
	private static final MissionPhase TRADE_EMBARKING = new MissionPhase("Mission.phase.tradeEmbarking");
	private static final MissionStatus SELLING_VEHICLE_NOT_AVAILABLE_FOR_TRADE = new MissionStatus("Mission.status.noSellingVehicle");

	// Static members
	public static final double MAX_STARTING_PROBABILITY = 100D;

	public static final int MAX_MEMBERS = 2;

	private static final Set<ObjectiveType> OBJECTIVES = Set.of(ObjectiveType.TRADE_CENTER);



	// Data members.
	private double profit;
	private double desiredProfit;

	private boolean outbound;
	private boolean doNegotiation;

	private Settlement tradingSettlement;
	private NegotiateTrade negotiationTask;

	private Map<Good, Integer> sellLoad;
	private Map<Good, Integer> buyLoad;
	private Map<Good, Integer> desiredBuyLoad;

	/**
	 * Constructor. Started by TradeMeta
	 *
	 * @param startingMember the mission member starting the settlement.
	 */
	public Trade(Worker startingMember, boolean needsReview) {
		// Use RoverMission constructor.
		super(MissionType.TRADE, startingMember, null);

		// Problem setting up the mission
		if (isDone()) {
			return;
		}

		Settlement s = startingMember.getSettlement();
		// Set the mission capacity.
		if (getMissionCapacity() > MAX_MEMBERS) {
			setMissionCapacity(MAX_MEMBERS);
		}

		outbound = true;
		doNegotiation = true;

		if (!isDone() && s != null) {
			// Get trading settlement
			Deal deal = s.getGoodsManager().getBestDeal(MissionType.TRADE, getVehicle());
			if (deal == null) {
				endMission(NO_TRADING_SETTLEMENT);
				return;
			}
			tradingSettlement = deal.getBuyer();
			addNavpoint(tradingSettlement);

			desiredBuyLoad = deal.getBuyingLoad();
			sellLoad = deal.getSellingLoad();
			desiredProfit = deal.getProfit();

			// Recruit additional members to mission.
			if (!isDone() && !recruitMembersForMission(startingMember, MAX_MEMBERS)) {
				return;
			}
		}

		// Set initial phase
		setInitialPhase(needsReview);
	}

	/**
	 * Constructor 2. Started by MissionDataBean
	 *
	 * @param members
	 * @param startingSettlement
	 * @param tradingSettlement
	 * @param rover
	 * @param description
	 * @param sellGoods
	 * @param buyGoods
	 */
	public Trade(Collection<Worker> members, Settlement tradingSettlement,
			Rover rover, Map<Good, Integer> sellGoods, Map<Good, Integer> buyGoods) {
		// Use RoverMission constructor.
		super(MissionType.TRADE, (Worker) members.toArray()[0], rover);

		outbound = true;
		doNegotiation = false;

		// Sets the mission capacity.
		if (getMissionCapacity() > MAX_MEMBERS) {
			setMissionCapacity(MAX_MEMBERS);
		}

		// Set mission destination.
		this.tradingSettlement = tradingSettlement;
		addNavpoint(tradingSettlement);

		addMembers(members, false);

		// Set trade goods.
		sellLoad = sellGoods;
		buyLoad = buyGoods;
		desiredBuyLoad = new HashMap<>(buyGoods);
		profit = CommerceUtil.getEstimatedProfit(getStartingSettlement(), getRover(), tradingSettlement, buyLoad, sellLoad);

		desiredProfit = profit;

		// Set initial phase
		setInitialPhase(false);
	}

	/**
	 * Determines a new phase for the mission when the current phase has ended.
	 */
	@Override
	protected boolean determineNewPhase() {
		boolean handled = true;
		if (!super.determineNewPhase()) {
			if (TRAVELLING.equals(getPhase())) {
				if (isCurrentNavpointSettlement()) {
					startDisembarkingPhase(outbound ? TRADE_DISEMBARKING : DISEMBARKING);
				}
			}

			else if (TRADE_DISEMBARKING.equals(getPhase())) {
				setPhase(TRADE_NEGOTIATING, tradingSettlement.getName());
			}

			else if (TRADE_NEGOTIATING.equals(getPhase())) {
				setPhase(UNLOAD_GOODS, tradingSettlement.getName());
			}

			else if (UNLOAD_GOODS.equals(getPhase())) {
				// Check if vehicle can hold enough supplies for mission.
				if (!isVehicleLoadable()) {
					endMission(CANNOT_LOAD_RESOURCES);
				}
				else {
					// Start the loading
					prepareLoadingPlan(tradingSettlement);
					setPhase(LOAD_GOODS, tradingSettlement.getName());
				}
			}

			else if (LOAD_GOODS.equals(getPhase())) {
				setPhase(TRADE_EMBARKING, tradingSettlement.getName());
			}

			else if (TRADE_EMBARKING.equals(getPhase())) {
				startTravellingPhase();
			}
			else {
				handled = false;
			}
		}

		return handled;
	}

	@Override
	protected void performPhase(Worker member) {
		super.performPhase(member);
		if (TRADE_DISEMBARKING.equals(getPhase())) {
			performTradeDisembarkingPhase(member);
		} else if (TRADE_NEGOTIATING.equals(getPhase())) {
			performTradeNegotiatingPhase(member);
		} else if (UNLOAD_GOODS.equals(getPhase())) {
			performUnloadGoodsPhase(member);
		} else if (LOAD_GOODS.equals(getPhase())) {
			performLoadGoodsPhase(member);
		} else if (TRADE_EMBARKING.equals(getPhase())) {
			computeTotalDistanceProposed();
			performTradeEmbarkingPhase(member);
		}
	}

	/**
	 * Performs the trade disembarking phase.
	 *
	 * @param member the mission member performing the mission.
	 */
	private void performTradeDisembarkingPhase(Worker member) {
		Vehicle v = getVehicle();
		// If rover is not parked at settlement, park it.
		// if ((v != null) && (v.getSettlement() == null)) {

		// 	// This should have already been done as part of the vehicle.transfer
		// 	tradingSettlement.addVicinityVehicle(v);

		// 	// Add vehicle to a garage if available.
		// 	tradingSettlement.getBuildingManager().addToGarage(v);
		// 	v.addSecondaryStatus(StatusType.UNLOADING);
		// }

		// Have member exit rover if necessary.
		if (!member.isInSettlement()) {

			// Get random inhabitable building at trading settlement.
			Building destinationBuilding = tradingSettlement.getBuildingManager().getRandomAirlockBuilding();
			if (destinationBuilding != null) {
				LocalPosition adjustedLoc = LocalAreaUtil.getRandomLocalPos(destinationBuilding);
				if (member instanceof Person person) {
					
					WalkingSteps walkingSteps = new WalkingSteps(person, adjustedLoc, destinationBuilding);
					boolean canWalk = Walk.canWalkAllSteps(person, walkingSteps);
					
					if (canWalk) {
						boolean canDo = assignTask(person, new Walk(person, walkingSteps));
						if (!canDo) {
							logger.severe("Unable to start walking to building " + destinationBuilding);
						}
					}
					else {
						logger.severe("Unable to walk to building " + destinationBuilding);
					}
				}
				else if (member instanceof Robot robot) {
					
					WalkingSteps walkingSteps = new WalkingSteps(robot, adjustedLoc, destinationBuilding);
					boolean canWalk = Walk.canWalkAllSteps(robot, walkingSteps);
					
					if (canWalk) {
						boolean canDo = assignTask(robot, new Walk(robot, walkingSteps));
						if (!canDo) {
							logger.severe("Unable to start walking to building " + destinationBuilding);
						}
					}
					else {
						logger.severe("Unable to walk to building " + destinationBuilding);
					}
				}
			}
			else {
				endMissionProblem(tradingSettlement, "No inhabitable buildings");
			}
		}

		// End the phase when everyone is out of the rover.
		if (isNoOneInRover()) {
			setPhaseEnded(true);
		}
	}

	/**
	 * Perform the trade negotiating phase.
	 *
	 * @param member the mission member performing the phase.
	 */
	private void performTradeNegotiatingPhase(Worker member) {
		if (doNegotiation) {
			if (member == getMissionTrader()) {
				if (negotiationTask != null) {
					if (negotiationTask.isDone()) {
						buyLoad = negotiationTask.getBuyLoad();
						profit = CommerceUtil.getEstimatedProfit(getStartingSettlement(), getRover(), tradingSettlement, buyLoad, sellLoad);
						fireMissionUpdate(MissionEventType.BUY_LOAD_EVENT);
						setPhaseEnded(true);
					}
				}

				else {
					Person settlementTrader = getSettlementTrader();

					if (settlementTrader != null) {
						if (member instanceof Person person) {
							negotiationTask = new NegotiateTrade(tradingSettlement, getStartingSettlement(), getRover(),
									sellLoad, person, settlementTrader);
							assignTask(person, negotiationTask);
						}
					}
					else if (getPhaseDuration() > 1000D) {
						buyLoad = new HashMap<>();
						profit = 0D;
						fireMissionUpdate(MissionEventType.BUY_LOAD_EVENT);
						setPhaseEnded(true);
					}
				}
			}
		} else {
			setPhaseEnded(true);
		}

		if (getPhaseEnded()) {
			outbound = false;
			resetToReturnTrip(
					new NavPoint(tradingSettlement, null),
					new NavPoint(getStartingSettlement(), tradingSettlement.getCoordinates()));

			getStartingSettlement().getGoodsManager().clearDeal(MissionType.TRADE);
		}
	}

	/**
	 * Perform the unload goods phase.
	 *
	 * @param member the mission member performing the phase.
	 */
	private void performUnloadGoodsPhase(Worker member) {

		// Unload towed vehicle (if necessary).
		unloadTowedVehicle();

		// Unload rover if necessary.
		boolean roverUnloaded = !getRover().haveStatusType(StatusType.UNLOADING);
		if (roverUnloaded) {
			setPhaseEnded(true);
		}
	}

	/**
	 * Performs the load goods phase.
	 *
	 * @param member the mission member performing the phase.
	 */
	private void performLoadGoodsPhase(Worker member) {

		if (!isDone()) {
			// Load towed vehicle (if necessary).
			loadTowedVehicle();
		}

		if (isDone() || isVehicleLoaded()) {
			setPhaseEnded(true);
		}
	}

	/**
	 * Unloads any towed vehicles.
	 */
	private void unloadTowedVehicle() {
		Vehicle towed = getRover().getTowedVehicle();
		if (towed != null) {
			towed.setReservedForMission(false);
			getRover().setTowedVehicle(null);
			towed.setTowingVehicle(null);
			tradingSettlement.addVicinityVehicle(towed);
			towed.findNewParkingLoc();
		}
	}

	/**
	 * Loads the towed vehicle is not already loaded.
	 */
	private void loadTowedVehicle() {
		if (!isDone() && (getRover().getTowedVehicle() == null)) {
			String vehicleType = getLoadVehicleType(true);
			if (vehicleType != null) {
				Vehicle buyVehicle = getInitialLoadVehicle(vehicleType, true);
				if (buyVehicle != null) {
					buyVehicle.setReservedForMission(true);
					getRover().setTowedVehicle(buyVehicle);
					buyVehicle.setTowingVehicle(getRover());
					tradingSettlement.removeVicinityParkedVehicle(buyVehicle);
				} else {
					logger.warning(getRover(), "Selling vehicle (" + vehicleType + ") is not available (Trade).");
					endMission(SELLING_VEHICLE_NOT_AVAILABLE_FOR_TRADE);
				}
			}
		}
	}

	/**
	 * Performs the trade embarking phase.
	 *
	 * @param member the mission member performing the phase.
	 */
	private void performTradeEmbarkingPhase(Worker member) {
		Vehicle v = getVehicle();
		
		// If person is not aboard the rover, board rover.
		if (!isDone() && !member.isInVehicle()) {

			// Move person to random location within rover.
			LocalPosition adjustedLoc = LocalAreaUtil.getRandomLocalPos(v);

			// Elect a new mission lead if the previous one was dead
			if (member instanceof Person lead) {
				if (lead.isDeclaredDead()) {
					logger.info(lead, "No longer alive.");
					int bestSkillLevel = 0;
					for (Worker mm: getMembers()) {
						if (mm instanceof Person p) {
							int level = lead.getSkillManager().getSkillExp(SkillType.TRADING);
							if (level > bestSkillLevel) {
								bestSkillLevel = level;
								lead = p;
								setStartingMember(p);
								break;
							}
						}
					}
				}
			}

			// Question: is the trading settlement responsible
			// for providing an EVA suit for each person
			for (Worker mm: getMembers()) {
				if (mm instanceof Person person) {
					if (person.isDeclaredDead()) {
						continue;
					}

					if (v == null)
						v = person.getVehicle();
					
					// Check if an EVA suit is available
					EVASuitUtil.fetchEVASuitFromAny(person, v, tradingSettlement);

					WalkingSteps walkingSteps = new WalkingSteps(person, adjustedLoc, v);
					boolean canWalk = Walk.canWalkAllSteps(person, walkingSteps);
					// Walk back to the vehicle and be ready to embark and go home
					if (canWalk) {
						boolean canDo = assignTask(person, new Walk(person, walkingSteps));
						if (!canDo) {
							logger.warning(person, "Unable to start walking to " + v + ".");
						}
					}

					else {
						endMissionProblem(person, "Unable to enter rover " + v.getName());
					}
				}
			}
		}

		// If rover is loaded and everyone is aboard, embark from settlement.
		if (!isDone() && isEveryoneInRover()) {

			// Embark from settlement
			if (v.transfer(unitManager.getMarsSurface())) {
				setPhaseEnded(true);
			}
			else {
				endMissionProblem(v, "Could not transfer to Surface");
			}			
		}
	}

	@Override
	protected void performDepartingFromSettlementPhase(Worker member) {
		super.performDepartingFromSettlementPhase(member);

		if (!isDone() && (getRover().getTowedVehicle() == null)) {
			String vehicleType = getLoadVehicleType(false);
			if (vehicleType != null) {
				Vehicle sellVehicle = getInitialLoadVehicle(vehicleType, false);
				if (sellVehicle != null) {
					sellVehicle.setReservedForMission(true);
					getRover().setTowedVehicle(sellVehicle);
					sellVehicle.setTowingVehicle(getRover());
					getStartingSettlement().removeVicinityParkedVehicle(sellVehicle);
				} else {
					logger.warning(getRover(), "Selling vehicle (" + vehicleType + ") is not available (Trade).");
					endMission(SELLING_VEHICLE_NOT_AVAILABLE_FOR_TRADE);
				}
			}
		}
	}

	@Override
	protected void performDisembarkToSettlementPhase(Worker member, Settlement disembarkSettlement) {

		// Unload towed vehicle if any.
//		if (!isDone() && (getRover().getTowedVehicle() != null)) {
//			Vehicle towed = getRover().getTowedVehicle();
//			towed.setReservedForMission(false);
//			getRover().setTowedVehicle(null);
//			towed.setTowingVehicle(null);
//			disembarkSettlement.addParkedVehicle(towed);
//			towed.findNewParkingLoc();
//		}

		super.performDisembarkToSettlementPhase(member, disembarkSettlement);
	}

	@Override
	protected void endMission(MissionStatus endStatus) {
		super.endMission(endStatus);

		// Unreserve any towed vehicles.
		if (getRover() != null) {
			if (getRover().getTowedVehicle() != null) {
				Vehicle towed = getRover().getTowedVehicle();
				towed.setReservedForMission(false);
			}
		}
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

			Iterator<Vehicle> j = settlement.getParkedGaragedVehicles().iterator();
			while (j.hasNext()) {
				Vehicle vehicle = j.next();
				boolean isEmpty = vehicle.isEmpty();
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
	protected Map<Integer, Integer> getOptionalEquipmentToLoad() {

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
			if (good.getCategory().equals(GoodCategory.EQUIPMENT)
					|| good.getCategory() == GoodCategory.CONTAINER) {
				int num = load.get(good);
				int id = good.getID();
				if (result.containsKey(id)) {
					num += (Integer) result.get(id);
				}
				result.put(id, num);
			}
		}

		return result;
	}

	@Override
	protected Map<Integer, Number> getOptionalResourcesToLoad() {

		Map<Integer, Number> result = super.getOptionalResourcesToLoad();

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
				int id = good.getID();
				double amount = load.get(good).doubleValue();
				if (result.containsKey(id)) {
					amount += (Double) result.get(id);
				}
				result.put(id, amount);
			} else if (good.getCategory().equals(GoodCategory.ITEM_RESOURCE)) {
				int id = good.getID();
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

		if (result == 0) {
			// Check if one has more general cargo capacity than the other.
			double firstCapacity = firstVehicle.getCargoCapacity();
			double secondCapacity = secondVehicle.getCargoCapacity();
			if (firstCapacity > secondCapacity) {
				result = 1;
			} else if (secondCapacity > firstCapacity) {
				result = -1;
			}

			// Vehicle with superior range should be ranked higher.
			if (result == 0) {
				double firstRange = firstVehicle.getRange();
				double secondRange = secondVehicle.getRange();
				if (firstRange > secondRange) {
					result = 1;
				} else if (firstRange <secondRange) {
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
	private Person getMissionTrader() {
		Person bestTrader = null;
		int bestTradeSkill = -1;

		Iterator<Worker> i = getMembers().iterator();
		while (i.hasNext()) {
			Worker member = i.next();
			if (member instanceof Person person) {
				int tradeSkill = person.getSkillManager().getEffectiveSkillLevel(SkillType.TRADING);
				if (tradeSkill > bestTradeSkill) {
					bestTradeSkill = tradeSkill;
					bestTrader = person;
				}
			}
		}

		return bestTrader;
	}

	/**
	 * Gets the trader and the destination settlement for the mission.
	 *
	 * @return the trader.
	 */
	private Person getSettlementTrader() {
		Person bestTrader = null;
		int bestTradeSkill = -1;

		Iterator<Person> i = tradingSettlement.getIndoorPeople().iterator();
		while (i.hasNext()) {
			Person person = i.next();
			if (!getMembers().contains(person)) {
				int tradeSkill = person.getSkillManager().getEffectiveSkillLevel(SkillType.TRADING);
				if (tradeSkill > bestTradeSkill) {
					bestTradeSkill = tradeSkill;
					bestTrader = person;
				}
			}
		}

		return bestTrader;
	}

	/**
	 * Gets the load that is being sold in the trade.
	 *
	 * @return sell load.
	 */
	public Map<Good, Integer> getSellLoad() {
		if (sellLoad != null) {
			return Collections.unmodifiableMap(sellLoad);
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
			return Collections.unmodifiableMap(buyLoad);
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
			return Collections.unmodifiableMap(desiredBuyLoad);
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

	@Override
	public Set<ObjectiveType> getObjectiveSatisified() {
		return OBJECTIVES;
	}
}
