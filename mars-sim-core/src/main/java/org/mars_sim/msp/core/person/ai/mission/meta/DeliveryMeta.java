/*
 * Mars Simulation Project
 * DeliveryMeta.java
 * @date 2021-08-28
 * @author Manny Kung
 */
package org.mars_sim.msp.core.person.ai.mission.meta;

import java.util.logging.Level;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.mission.Delivery;
import org.mars_sim.msp.core.person.ai.mission.Delivery.DeliveryProfitInfo;
import org.mars_sim.msp.core.person.ai.mission.DeliveryUtil;
import org.mars_sim.msp.core.person.ai.mission.DroneMission;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionType;
import org.mars_sim.msp.core.person.ai.role.RoleType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.vehicle.Drone;

/**
 * A meta mission for the delivery mission.
 */
public class DeliveryMeta extends AbstractMetaMission {

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(DeliveryMeta.class.getName());
	
    private static final int FREQUENCY = 100;
    
    private static final int VALUE = 1;
    
    private static final double DIVISOR = 10;


	DeliveryMeta() {
		// Everyone can start Delivery ??
		super(MissionType.DELIVERY, "delivery", null);
	}

	@Override
	public Mission constructInstance(Person person) {
		return new Delivery(person);
	}

	@Override
	public double getProbability(Person person) {

		double missionProbability = 0D;

		// Check if person is in a settlement.

		// Check if mission is possible for person based on their circumstance.
		Settlement settlement = person.getAssociatedSettlement();

		if (settlement.isFirstSol())
			return 0;
		
		RoleType roleType = person.getRole().getType();
		if (RoleType.CHIEF_OF_SUPPLY_N_RESOURCES == roleType
				|| RoleType.MISSION_SPECIALIST == roleType
				|| RoleType.CHIEF_OF_MISSION_PLANNING == roleType
				|| RoleType.RESOURCE_SPECIALIST == roleType
				|| RoleType.SUB_COMMANDER == roleType
				|| RoleType.COMMANDER == roleType
				) {
			
				// Note: checkMission() gives rise to a NULLPOINTEREXCEPTION that points to
				// Inventory
				// It happens only when this sim is a loaded saved sim.
				missionProbability = getSettlementProbability(settlement); 
			
		} else {
			missionProbability = 0;
		}
		
		if (missionProbability <= 0)
			return 0;
	
		// if introvert, score  0 to  50 --> -2 to 0
		// if extrovert, score 50 to 100 -->  0 to 2
		// Reduce probability if introvert
		int extrovert = person.getExtrovertmodifier();
		missionProbability += extrovert;
		
		if (missionProbability < 0)
			missionProbability = 0;
	
        if (missionProbability > 0)
        	logger.info(person, "DeliveryMeta's probability: " +
				 Math.round(missionProbability*100D)/100D);

		return missionProbability;
	}

	
	private double getSettlementProbability(Settlement settlement) {

		double missionProbability = 0;

		// Check for the best delivery settlement within range.
		double deliveryProfit = 0D;
		
		Drone drone = (Drone) DroneMission.getDroneWithGreatestRange(MissionType.DELIVERY, settlement, false);
		
		if (drone == null) {
			return 0;
		}
		
		logger.info(settlement, 10_000L, drone.getNickName() + " available for delivery mission.");
		
		try {
			// Only check every couple of Sols, else use cache.
			// Note: this method is very CPU intensive.
			boolean useCache = false;

			if (Delivery.TRADE_PROFIT_CACHE.containsKey(settlement)) {
				DeliveryProfitInfo profitInfo = Delivery.TRADE_PROFIT_CACHE.get(settlement);
				double timeDiff = MarsClock.getTimeDiff(marsClock, profitInfo.time);
				if (timeDiff < FREQUENCY) {
					deliveryProfit = profitInfo.profit;
					useCache = true;
				}
			} else {
				Delivery.TRADE_PROFIT_CACHE.put(settlement,
						new DeliveryProfitInfo(deliveryProfit, (MarsClock) marsClock.clone()));
			}

			if (!useCache) {

				deliveryProfit = DeliveryUtil.getBestDeliveryProfit(settlement, drone) * VALUE;

				Delivery.TRADE_PROFIT_CACHE.put(settlement,
						new DeliveryProfitInfo(deliveryProfit, (MarsClock) marsClock.clone()));
				Delivery.TRADE_SETTLEMENT_CACHE.put(settlement, DeliveryUtil.bestDeliverySettlementCache);
			}
			
	
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Issues with DeliveryUtil: ", e);
			return 0;
		}

		// Delivery value modifier.
		missionProbability = deliveryProfit / DIVISOR * settlement.getGoodsManager().getTradeFactor();
		if (missionProbability > Delivery.MAX_STARTING_PROBABILITY) {
			missionProbability = Delivery.MAX_STARTING_PROBABILITY;
		}

		int numThisMission = Simulation.instance().getMissionManager().numParticularMissions(MissionType.DELIVERY, settlement);

   		// Check for # of embarking missions.
		if (Math.max(1, settlement.getNumCitizens() / 2.0) < numThisMission) {
			return 0;
		}			
		
		else if (numThisMission > 1)
			return 0;	
		
		int f2 = 2*numThisMission + 1;
		
		missionProbability *= settlement.getNumCitizens() / f2 / 2D;
		
		// Crowding modifier.
		int crowding = settlement.getIndoorPeopleCount() - settlement.getPopulationCapacity();
		if (crowding > 0) {
			missionProbability *= (crowding + 1);
		}

		return missionProbability;
	}
}
