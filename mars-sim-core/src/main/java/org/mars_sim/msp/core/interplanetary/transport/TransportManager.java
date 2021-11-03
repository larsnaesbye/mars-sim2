/*
 * Mars Simulation Project
 * TransportManager.java
 * @date 2021-09-04
 * @author Scott Davis
 */
package org.mars_sim.msp.core.interplanetary.transport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.configuration.Scenario;
import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.events.HistoricalEventManager;
import org.mars_sim.msp.core.interplanetary.transport.resupply.ResupplyUtil;
import org.mars_sim.msp.core.interplanetary.transport.settlement.ArrivingSettlement;
import org.mars_sim.msp.core.person.EventType;
import org.mars_sim.msp.core.reportingAuthority.ReportingAuthorityFactory;
import org.mars_sim.msp.core.structure.SettlementConfig;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.Temporal;

/**
 * A manager for interplanetary transportation.
 */
public class TransportManager implements Serializable, Temporal {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	
	/** Average transit time for arriving settlements from Earth to Mars (sols). */
	private static int AVG_TRANSIT_TIME = 250;

	/** default logger. */
	private static final Logger logger = Logger.getLogger(TransportManager.class.getName());

	private Collection<Transportable> transportItems;

	private HistoricalEventManager eventManager;

	/**
	 * Constructor.
	 * @param scenario 
	 * @param raFactory 
	 */
	public TransportManager(HistoricalEventManager eventManager) {
		this.eventManager = eventManager;
		
		// initialize ResupplyUtil.
		new ResupplyUtil();
		// Initialize data
		transportItems = new ArrayList<>();

		// Create initial resupply missions.
		// TODO Need to revisit
		transportItems.addAll(ResupplyUtil.loadInitialResupplyMissions());
	}
	
	/**
	 * Add any arriving Settlements that are defined in a Scenario
	 * @param scenario
	 * @param settlementConfig 
	 * @param raFactory 
	 */
	public void loadArrivingSettments(Scenario scenario, SettlementConfig settlementConfig,
									  ReportingAuthorityFactory raFactory) {
		MarsClock now = Simulation.instance().getMasterClock().getMarsClock();
		// Create initial arriving settlements.
		for (ArrivingSettlement a : scenario.getArrivals()) {
			// Check the defines values are correct; these throw exception
			settlementConfig.getSettlementTemplate(a.getTemplate());
			
			if (raFactory.getItem(a.getSponsorCode()) == null) {
				throw new IllegalArgumentException("Arriving settlement has a incorrect RAcode " + a.getSponsorCode());
			}
			
			a.scheduleLaunch(now, AVG_TRANSIT_TIME);
			logger.info("New settlement called " + a.getName() + " arriving in sols "
						+ a.getArrivalDate().getTrucatedDateTimeStamp());
			transportItems.add(a);
		}
	}
	
	/**
	 * Adds a new transport item.
	 * 
	 * @param transportItem the new transport item.
	 */
	public void addNewTransportItem(Transportable transportItem) {
		transportItems.add(transportItem);
		HistoricalEvent newEvent = new TransportEvent(transportItem, EventType.TRANSPORT_ITEM_CREATED,
				"Mission Control", transportItem.getSettlementName());
		eventManager.registerNewEvent(newEvent);
		logger.info("A new transport item was created ");
	}

	/**
	 * Gets the transport items that are planned or in transit.
	 * 
	 * @return transportables.
	 */
	public List<Transportable> getIncomingTransportItems() {
		List<Transportable> incoming = new ArrayList<>();

		Iterator<Transportable> i = transportItems.iterator();
		while (i.hasNext()) {
			Transportable transportItem = i.next();
			TransitState state = transportItem.getTransitState();
			if (TransitState.PLANNED == state || TransitState.IN_TRANSIT == state) {
				incoming.add(transportItem);
			}
		}

		return incoming;
	}

	/**
	 * Gets the transport items that have already arrived.
	 * 
	 * @return transportables.
	 */
	public List<Transportable> getArrivedTransportItems() {
		List<Transportable> arrived = new ArrayList<>();

		Iterator<Transportable> i = transportItems.iterator();
		while (i.hasNext()) {
			Transportable transportItem = i.next();
			TransitState state = transportItem.getTransitState();
			if (TransitState.ARRIVED == state) {
				arrived.add(transportItem);
			}
		}

		return arrived;
	}

	/**
	 * Cancels a transport item.
	 * 
	 * @param transportItem the transport item.
	 */
	public void cancelTransportItem(Transportable transportItem) {
		transportItem.setTransitState(TransitState.CANCELED);
		HistoricalEvent cancelEvent = new TransportEvent(transportItem, EventType.TRANSPORT_ITEM_CANCELLED, "Reserved",
				transportItem.getSettlementName());
		eventManager.registerNewEvent(cancelEvent);
		logger.info("A transport item was cancelled: ");// + transportItem.toString());
	}

	/**
	 * Time passing.
	 *
	 * @param pulse Pulse of the simulation
	 * @throws Exception if error.
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {
		Iterator<Transportable> i = transportItems.iterator();
		while (i.hasNext()) {
			Transportable transportItem = i.next();
			if (TransitState.PLANNED == transportItem.getTransitState()) {
				if (MarsClock.getTimeDiff(pulse.getMarsTime(), transportItem.getLaunchDate()) >= 0D) {
					// Transport item is launched.
					transportItem.setTransitState(TransitState.IN_TRANSIT);
					HistoricalEvent deliverEvent = new TransportEvent(transportItem, EventType.TRANSPORT_ITEM_LAUNCHED,
							"Transport item launched", transportItem.getSettlementName());
					eventManager.registerNewEvent(deliverEvent);
					logger.info("Transport item launched: " + transportItem.toString());
					continue;
				}
			} else if (TransitState.IN_TRANSIT == transportItem.getTransitState()) {
				if (MarsClock.getTimeDiff(pulse.getMarsTime(), transportItem.getArrivalDate()) >= 0D) {
					// Transport item has arrived on Mars.
					transportItem.setTransitState(TransitState.ARRIVED);
					transportItem.performArrival();
					HistoricalEvent arrivalEvent = new TransportEvent(transportItem, EventType.TRANSPORT_ITEM_ARRIVED,
							transportItem.getSettlementName(), "Transport item arrived on Mars");
					eventManager.registerNewEvent(arrivalEvent);
					logger.info("Transport item arrived at " + transportItem.toString());
				}
			}
		}
		
		return true;
	}
}
