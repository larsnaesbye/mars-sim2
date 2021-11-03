/*
 * Mars Simulation Project
 * TravelMission.java
 * @date 2021-08-15
 * @author Scott Davis
 */

package org.mars_sim.msp.core.person.ai.mission;

import java.util.ArrayList;
import java.util.List;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.time.MarsClock;

/**
 * A mission that involves traveling along a series of navpoints. TODO
 * externalize strings
 */
public abstract class TravelMission extends Mission {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(TravelMission.class.getName());

	// Travel Mission status
	public final static String AT_NAVPOINT = "At a navpoint";
	public final static String TRAVEL_TO_NAVPOINT = "Traveling to navpoint";

	// Data members
	/** List of navpoints for the mission. */
	private List<NavPoint> navPoints = new ArrayList<NavPoint>();
	/** The current navpoint index. */
	private int navIndex = 0;
	
	/** The estimated total distance for this mission. */
	private double estimatedTotalDistance = 0;
	/** The current leg remaining distance at this moment. */
	private double currentLegRemainingDistance;
	/** The estimated total remaining distance at this moment. */
	private double estimatedTotalRemainingDistance;
	
	/** The current traveling status of the mission. */
	private String travelStatus;
	
	/** The last navpoint the mission stopped at. */
	private NavPoint lastStopNavpoint;
	/** The time the last leg of the mission started at. */
	private MarsClock legStartingTime;

	
	/**
	 * Constructor 1
	 * 
	 * @param missionName
	 * @param startingMember
	 * @param minPeople
	 */
	protected TravelMission(String missionName, MissionType missionType, MissionMember startingMember, int minPeople) {
		// Use Mission constructor.
		super(missionName, missionType, startingMember, minPeople);

		NavPoint startingNavPoint = null;
		Coordinates c = getCurrentMissionLocation();

		if (c != null) {
			if (startingMember.getSettlement() != null) {
				startingNavPoint = new NavPoint(c, startingMember.getSettlement(),
						startingMember.getSettlement().getName());
			} else {
				startingNavPoint = new NavPoint(c, "starting location");
			}
		}

		if (startingNavPoint != null) {
			addNavpoint(startingNavPoint);
			lastStopNavpoint = startingNavPoint;

			setTravelStatus(AT_NAVPOINT);
		}
		
		logger.fine(getStartingPerson(), 
				"Set navpoints for " + missionName + ".");
	}

	/**
	 * sReset the trip statistics to return home
	 * 
	 * @param currentNavPoint
	 * @param destNavPoint
	 */
	public void resetToReturnTrip(NavPoint currentNavPoint, NavPoint destNavPoint) {
		
		setEstimatedTotalDistance(0);
		
		navPoints.clear();
		
		navIndex = 0;
		 
		addNavpoint(currentNavPoint);
		
		addNavpoint(destNavPoint);
		
		lastStopNavpoint = currentNavPoint;

		setTravelStatus(AT_NAVPOINT);

		// Need to recalculate what is left to travel to get resoruces loaded
		// for return
		computeEstimatedTotalDistance();

	}
	
	/**
	 * Adds a navpoint to the mission.
	 * 
	 * @param navPoint the new nav point location to be added.
	 * @throws IllegalArgumentException if location is null.
	 */
	public final void addNavpoint(NavPoint navPoint) {
		if (navPoint != null) {
			navPoints.add(navPoint);
			fireMissionUpdate(MissionEventType.NAVPOINTS_EVENT);
		} else {
			logger.severe(getTypeID() + " navPoint is null");
		}
	}

	/**
	 * Sets a nav point for the mission.
	 * 
	 * @param index    the index in the list of nav points.
	 * @param navPoint the new navpoint
	 * @throws IllegalArgumentException if location is null or index < 0.
	 */
	protected final void setNavpoint(int index, NavPoint navPoint) {
		if ((navPoint != null) && (index >= 0)) {
			navPoints.set(index, navPoint);
			fireMissionUpdate(MissionEventType.NAVPOINTS_EVENT);
		} else {
			logger.severe(getTypeID() + " navPoint is null");
		}
	}

	/**
	 * Clears out any unreached nav points.
	 */
	public final void clearRemainingNavpoints() {
		int index = getNextNavpointIndex();
		int numNavpoints = getNumberOfNavpoints();
		for (int x = index; x < numNavpoints; x++) {
			navPoints.remove(index);
			fireMissionUpdate(MissionEventType.NAVPOINTS_EVENT);
		}
	}

	/**
	 * Gets the last navpoint reached.
	 * 
	 * @return navpoint
	 */
	public final NavPoint getPreviousNavpoint() {
		return lastStopNavpoint;
	}

	/**
	 * Gets the mission's next navpoint.
	 * 
	 * @return navpoint or null if no more navpoints.
	 */
	public final NavPoint getNextNavpoint() {
		if (navIndex < navPoints.size())
			return navPoints.get(navIndex);
		else
			return null;
	}

	/**
	 * Gets the mission's next navpoint index.
	 * 
	 * @return navpoint index or -1 if none.
	 */
	public final int getNextNavpointIndex() {
		if (navIndex < navPoints.size())
			return navIndex;
		else
			return -1;
	}

	/**
	 * Set the next navpoint index.
	 * 
	 * @param newNavIndex the next navpoint index.
	 * @throws MissionException if the new navpoint is out of range.
	 */
	public final void setNextNavpointIndex(int newNavIndex) {
		if (newNavIndex < getNumberOfNavpoints()) {
			navIndex = newNavIndex;
		} else
			logger.severe(getPhase() + "'s newNavIndex " + newNavIndex + " is out of bounds.");
	}

	/**
	 * Gets the navpoint at an index value.
	 * 
	 * @param index the index value
	 * @return navpoint
	 * @throws IllegaArgumentException if no navpoint at that index.
	 */
	public final NavPoint getNavpoint(int index) {
		if ((index >= 0) && (index < getNumberOfNavpoints()))
			return navPoints.get(index);
		else {
			logger.severe(getTypeID() + " navpoint " + index + " is null.");
			return null;
		}
	}

	/**
	 * Gets the index of a navpoint.
	 * 
	 * @param navpoint the navpoint
	 * @return index or -1 if navpoint isn't in the trip.
	 */
	public final int getNavpointIndex(NavPoint navpoint) {
		if (navpoint == null)
			logger.severe(getTypeID() + " navpoint is null.");
		if (navPoints.contains(navpoint))
			return navPoints.indexOf(navpoint);
		else
			return -1;
	}

	/**
	 * Gets the number of navpoints on the trip.
	 * 
	 * @return number of navpoints
	 */
	public final int getNumberOfNavpoints() {
		return navPoints.size();
	}

	/**
	 * Gets a list of navpoint coordinates
	 * 
	 * @return
	 */
	public List<Coordinates> getNavCoordinates() {
		List<Coordinates> list = new ArrayList<>();
		int size = getNumberOfNavpoints();
		for (int i=0; i< size; i++) {
			list.add(navPoints.get(i).getLocation());
		}
		return list;
	}
	
	/**
	 * Gets the current navpoint the mission is stopped at.
	 * 
	 * @return navpoint or null if mission is not stopped at a navpoint.
	 */
	public final NavPoint getCurrentNavpoint() {
		if (travelStatus != null && AT_NAVPOINT.equals(travelStatus)) {
			if (navIndex < navPoints.size())
				return navPoints.get(navIndex);
			else
				return null;
		} else
			return null;
	}

	/**
	 * Gets the index of the current navpoint the mission is stopped at.
	 * 
	 * @return index of current navpoint or -1 if mission is not stopped at a
	 *         navpoint.
	 */
	public final int getCurrentNavpointIndex() {
		if (travelStatus != null && AT_NAVPOINT.equals(travelStatus))
			return navIndex;
		else
			return -1;
	}

	/**
	 * Get the travel mission's current status.
	 * 
	 * @return travel status as a String.
	 */
	public final String getTravelStatus() {
		return travelStatus;
	}

	/**
	 * Set the travel mission's current status.
	 * 
	 * @param newTravelStatus the mission travel status.
	 */
	private void setTravelStatus(String newTravelStatus) {
		travelStatus = newTravelStatus;
		fireMissionUpdate(MissionEventType.TRAVEL_STATUS_EVENT);
	}

	/**
	 * Starts travel to the next navpoint in the mission.
	 * 
	 * @throws MissionException if no more navpoints.
	 */
	protected final void startTravelToNextNode() {
		setNextNavpointIndex(navIndex + 1);
		setTravelStatus(TRAVEL_TO_NAVPOINT);
		legStartingTime = (MarsClock) Simulation.instance().getMasterClock().getMarsClock().clone();
	}

	/**
	 * The mission has reached the next navpoint.
	 * 
	 * @throws MisisonException if error determining mission location.
	 */
	protected final void reachedNextNode() {
		setTravelStatus(AT_NAVPOINT);
		lastStopNavpoint = getCurrentNavpoint();
	}

	/**
	 * Performs the travel phase of the mission.
	 * 
	 * @param member the mission member currently performing the mission.
	 */
	protected abstract void performTravelPhase(MissionMember member);
//    protected abstract void performTravelPhase(Robot robot);

	/**
	 * Gets the starting time of the current leg of the mission.
	 * 
	 * @return starting time
	 */
	protected final MarsClock getCurrentLegStartingTime() {
		if (legStartingTime != null) {
			return (MarsClock) legStartingTime.clone();
		} else {
			logger.severe(getTypeID() + " legStartingTime is null.");
			return null;
		}
	}

	/**
	 * Gets the distance of the current leg of the mission, or 0 if not in the
	 * travelling phase.
	 * 
	 * @return distance (km)
	 */
	public final double getCurrentLegDistance() {
		if (travelStatus != null && TRAVEL_TO_NAVPOINT.equals(travelStatus) && lastStopNavpoint != null) {
			NavPoint next = getNextNavpoint();
			if (next != null) {
				return lastStopNavpoint.getLocation().getDistance(next.getLocation());
			}
		}
		return 0D;
	}

	/**
	 * Gets the remaining distance for the current leg of the mission.
	 * 
	 * @return distance (km) or 0 if not in the travelling phase.
	 * @throws MissionException if error determining distance.
	 */
	public final double getCurrentLegRemainingDistance() {
		
		if (travelStatus != null && TRAVEL_TO_NAVPOINT.equals(travelStatus)) {

			if (getNextNavpoint() == null) {
				int offset = 2;
				if (getPhase().equals(VehicleMission.TRAVELLING))
					offset = 1;
				setNextNavpointIndex(getNumberOfNavpoints() - offset);
				updateTravelDestination();
			}
			
			Coordinates c1 = null;
			
			NavPoint next = getNextNavpoint();
			if (next != null) {
				c1 = next.getLocation();
			}
			else if (this instanceof TravelToSettlement) {
				c1 = ((TravelToSettlement)this).getDestinationSettlement().getCoordinates();	
			}
			
			double dist = 0;
			
			if (c1 != null) {
				dist = Coordinates.computeDistance(getCurrentMissionLocation(), c1);
			
				if (Double.isNaN(dist)) {
					logger.severe(getTypeID() + 
							": current leg's remaining distance is NaN.");
					dist = 0;
				}
			}
			
			if (currentLegRemainingDistance != dist) {
				currentLegRemainingDistance = dist;
				fireMissionUpdate(MissionEventType.DISTANCE_EVENT);
			}
					
//			System.out.println("   c0 : " + c0 + "   c1 : " + c1 + "   dist : " + dist);
			return dist;
		}

		else
			return 0D;
	}

	/**
	 * Computes the estimated total distance of the trip.
	 * 
	 * @return distance (km)
	 */
	public final void computeEstimatedTotalDistance() {
//		if (estimatedTotalDistance == 0) {
			if (navPoints.size() > 1) {
				double result = 0D;
				
				for (int x = 1; x < navPoints.size(); x++) {
					NavPoint prevNav = navPoints.get(x - 1);
					NavPoint currNav = navPoints.get(x);
					double distance = Coordinates.computeDistance(currNav.getLocation(), prevNav.getLocation());
					result += distance;
				}
				
				if (estimatedTotalDistance != result) {
					// Record the distance
					estimatedTotalDistance = result;
					fireMissionUpdate(MissionEventType.DISTANCE_EVENT);	
				}
			}
//		}
	}

	/**
	 * Gets the estimated total distance of the trip.
	 * 
	 * @return distance (km)
	 */
	public final double getEstimatedTotalDistance() {
		return estimatedTotalDistance;
	}
	
	/**
	 * Sets the estimated total distance of the trip.
	 * 
	 * @param value (km)
	 */
	public void setEstimatedTotalDistance(double value) {
		estimatedTotalDistance = value;
	}
	
	/**
	 * Gets the estimated total remaining distance to travel in the mission.
	 * 
	 * @return distance (km).
	 * @throws MissionException if error determining distance.
	 */
	public final double getEstimatedTotalRemainingDistance() {
		
		double leg = getCurrentLegRemainingDistance();
		int index = 0;
		double navDist = 0;
		if (AT_NAVPOINT.equals(travelStatus))
			index = getCurrentNavpointIndex();
		else if (TRAVEL_TO_NAVPOINT.equals(travelStatus))
			index = getNextNavpointIndex();

		for (int x = index + 1; x < getNumberOfNavpoints(); x++) {
			NavPoint prev = getNavpoint(x - 1);
			NavPoint next = getNavpoint(x); 
			if ((prev != null) && (next != null)) {
				navDist += Coordinates.computeDistance(prev.getLocation(), next.getLocation());
			}
		}
		
		// Note: check for Double.isInfinite() and Double.isNaN()
		if (Double.isNaN(navDist)) {
			logger.severe(getTypeID() + " has navDist is NaN.");
			navDist = 0;
		}
		
		double total = leg + navDist;
		
		if (estimatedTotalRemainingDistance != total) {
			// Record the distance
			estimatedTotalRemainingDistance = total;
			fireMissionUpdate(MissionEventType.DISTANCE_EVENT);	
		}
			
		
		return total;
	}

	/**
	 * Gets the actual total distance travelled during the mission so far.
	 * 
	 * @return distance (km)
	 */
	public abstract double getActualTotalDistanceTravelled();

	/**
	 * Gets the estimated time of arrival (ETA) for the current leg of the mission.
	 * 
	 * @return time (MarsClock) or null if not applicable.
	 */
	public abstract MarsClock getLegETA();

	/**
	 * Gets the estimated time remaining for the mission.
	 * 
	 * @param useBuffer use a time buffer in estimation if true.
	 * @return time (millisols)
	 * @throws MissionException
	 */
	public abstract double getEstimatedRemainingMissionTime(boolean useBuffer);

	/**
	 * Gets the estimated time for a trip.
	 * 
	 * @param useBuffer use time buffers in estimation if true.
	 * @param distance  the distance of the trip.
	 * @return time (millisols)
	 * @throws MissionException
	 */
	public abstract double getEstimatedTripTime(boolean useBuffer, double distance);

	/**
	 * Update mission to the next navpoint destination.
	 */
	public abstract void updateTravelDestination();

	@Override
	public void endMission() {
		super.endMission();
	}

	@Override
	public void destroy() {
		super.destroy();

		if (navPoints != null)
			navPoints.clear();
		navPoints = null;
		travelStatus = null;
		lastStopNavpoint = null;
		legStartingTime = null;
	}
}
