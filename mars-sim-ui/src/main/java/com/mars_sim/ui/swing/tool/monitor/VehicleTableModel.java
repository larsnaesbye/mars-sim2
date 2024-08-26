/*
 * Mars Simulation Project
 * VehicleTableModel.java
 * @date 2021-10-23
 * @author Barry Evans
 */
package com.mars_sim.ui.swing.tool.monitor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.mars_sim.core.Simulation;
import com.mars_sim.core.Unit;
import com.mars_sim.core.UnitEvent;
import com.mars_sim.core.UnitEventType;
import com.mars_sim.core.UnitType;
import com.mars_sim.core.malfunction.Malfunction;
import com.mars_sim.core.person.ai.mission.Mission;
import com.mars_sim.core.person.ai.mission.MissionEvent;
import com.mars_sim.core.person.ai.mission.MissionEventType;
import com.mars_sim.core.person.ai.mission.MissionListener;
import com.mars_sim.core.person.ai.mission.MissionManager;
import com.mars_sim.core.person.ai.mission.MissionManagerListener;
import com.mars_sim.core.person.ai.mission.NavPoint;
import com.mars_sim.core.person.ai.mission.VehicleMission;
import com.mars_sim.core.resource.AmountResource;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.vehicle.Crewable;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.mapdata.location.Coordinates;
import com.mars_sim.tools.Msg;
import com.mars_sim.ui.swing.utils.ColumnSpec;

/**
 * The VehicleTableModel that maintains a list of Vehicle objects.
 * It maps key attributes of the Vehicle into Columns.
 */
@SuppressWarnings("serial")
public class VehicleTableModel extends UnitTableModel<Vehicle> {

	private static final String ON = "On";
	private static final String OFF = "Off";
	private static final String TRUE = "True";
	private static final String FALSE = "False";

	// Column indexes
	private static final int NAME = 0;
	private static final int TYPE = NAME+1;
	private static final int MODEL = TYPE+1;
	private static final int SETTLEMENT = MODEL+1;
	private static final int LOCATION = SETTLEMENT+1;
	private static final int DESTINATION = LOCATION+1;
	private static final int DESTDIST = DESTINATION+1;
	private static final int MISSION = DESTDIST+1;
	private static final int CREW = MISSION+1;
	private static final int DRIVER = CREW+1;
	private static final int STATUS = DRIVER+1;
	private static final int BEACON = STATUS+1;
	private static final int RESERVED = BEACON+1;
	private static final int SPEED = RESERVED+1;
	private static final int MALFUNCTION = SPEED+1;
	private static final int OXYGEN = MALFUNCTION+1;
	private static final int METHANOL = OXYGEN+1;
	private static final int WATER = METHANOL+1;
	private static final int FOOD = WATER+1;
	private static final int ROCK_SAMPLES = FOOD+1;
	private static final int ICE = ROCK_SAMPLES+1;
	/** The number of Columns. */
	private static final int COLUMNCOUNT = ICE+1;
	/** Names of Columns. */
	private static final ColumnSpec[] COLUMNS;
	private static final Map<Integer,Integer> RESOURCE_TO_COL;

	/**
	 * Class initialiser creates the static names and classes.
	 */
	static {
		COLUMNS = new ColumnSpec[COLUMNCOUNT];
		COLUMNS[NAME] = new ColumnSpec("Name", String.class);
		COLUMNS[TYPE] = new ColumnSpec("Type", String.class);
		COLUMNS[MODEL] = new ColumnSpec("Model", String.class);
		COLUMNS[SETTLEMENT] = new ColumnSpec("Settlement", String.class);
		COLUMNS[LOCATION] = new ColumnSpec("Location", String.class);
		COLUMNS[DESTINATION] = new ColumnSpec("Next Waypoint", Coordinates.class);
		COLUMNS[DESTDIST] = new ColumnSpec("Dist. to next [km]", Double.class);
		COLUMNS[MISSION] = new ColumnSpec("Mission", String.class);
		COLUMNS[CREW] = new ColumnSpec("Crew", Integer.class);
		COLUMNS[DRIVER] = new ColumnSpec("Driver", String.class);
		COLUMNS[STATUS] = new ColumnSpec("Status", String.class);
		COLUMNS[BEACON] = new ColumnSpec("Beacon", String.class);
		COLUMNS[RESERVED] = new ColumnSpec("Reserved", String.class);
		COLUMNS[SPEED] = new ColumnSpec("Speed", Double.class);
		COLUMNS[MALFUNCTION] = new ColumnSpec("Malfunction", String.class);
		COLUMNS[OXYGEN] = new ColumnSpec("Oxygen", Double.class);
		COLUMNS[METHANOL] = new ColumnSpec("Methanol", Double.class);
		COLUMNS[WATER] = new ColumnSpec("Water", Double.class);
		COLUMNS[FOOD] = new ColumnSpec("Food", Double.class);
		COLUMNS[ROCK_SAMPLES] = new ColumnSpec("Rock Samples", Double.class);
		COLUMNS[ICE] = new ColumnSpec("Ice", Double.class);

		RESOURCE_TO_COL = new HashMap<>();
		RESOURCE_TO_COL.put(ResourceUtil.oxygenID, OXYGEN);
		RESOURCE_TO_COL.put(ResourceUtil.methanolID, METHANOL);
		RESOURCE_TO_COL.put(ResourceUtil.foodID, FOOD);
		RESOURCE_TO_COL.put(ResourceUtil.waterID, WATER);
		RESOURCE_TO_COL.put(ResourceUtil.rockSamplesID, ROCK_SAMPLES);
		RESOURCE_TO_COL.put(ResourceUtil.iceID, ICE);
	}

	private static MissionManager missionManager = Simulation.instance().getMissionManager();

	private transient LocalMissionManagerListener missionManagerListener;
	
	public VehicleTableModel() {
		super(UnitType.VEHICLE,
			Msg.getString("VehicleTableModel.tabName"),
			"VehicleTableModel.countingVehicles", //$NON-NLS-1$
			COLUMNS
		);

		setCachedColumns(OXYGEN, ICE);
		setSettlementColumn(SETTLEMENT);
		missionManagerListener = new LocalMissionManagerListener();
	}

	/**
	 * Filter the vehicles to a settlement
	 */
	@Override
	public boolean setSettlementFilter(Set<Settlement> filter) {
		resetEntities(filter.stream().flatMap(s -> s.getAllAssociatedVehicles().stream()).toList());

		return true;
	}

	/**
	 * Returns the value of a Cell.
	 * 
	 * @param rowIndex Row index of the cell.
	 * @param columnIndex Column index of the cell.
	 */
	@Override
	protected Object getEntityValue(Vehicle vehicle, int columnIndex) {
		Object result = null;

		switch (columnIndex) {
			case NAME : 
				result = vehicle.getName();
				break;

			case SETTLEMENT : 
				result = vehicle.getAssociatedSettlement().getName();
				break;

			case TYPE :
				result = vehicle.getVehicleType().getName();
				break;

			case MODEL :
				result = vehicle.getModelName();
				break;
				
			case LOCATION : {
				Settlement settle = vehicle.getSettlement();
				if (settle != null) {
					result = settle.getName();
				}
				else {
					result = vehicle.getCoordinates().getFormattedString();
				}
			} break;

			case DESTINATION : {
				Mission mission = vehicle.getMission();
				if (mission instanceof VehicleMission vm) {
					NavPoint destination = vm.getCurrentDestination();
					if (destination.isSettlementAtNavpoint())
						result = destination.getSettlement().getName();
					else
						result = destination.getDescription()
							+ " - " + destination.getLocation().getFormattedString();
				}
			} break;

			case DESTDIST : {
				Mission mission = vehicle.getMission();
				if (mission instanceof VehicleMission vm) {
					result = vm.getDistanceCurrentLegRemaining();
				}
			} break;

			case MISSION : {
				Mission mission = vehicle.getMission();
				if (mission != null) {
					result = mission.getFullMissionDesignation();
				}
			} break;

			case CREW : {
				if (vehicle instanceof Crewable c)
					result = c.getCrewNum();
			} break;

			case DRIVER :
				result = (vehicle.getOperator() != null ? vehicle.getOperator().getName() : null);
				break;

			case SPEED :
				result = vehicle.getSpeed();
				break;

			// Status is a combination of Mechanical failure and maintenance
			case STATUS :
				result = vehicle.printStatusTypes();
				break;

			case BEACON :
				result = (vehicle.isBeaconOn() ? ON : OFF);
				break;

			case RESERVED :
				result = (vehicle.isReserved() ? TRUE : FALSE);
				break;

			case MALFUNCTION: {
				Malfunction failure = vehicle.getMalfunctionManager().getMostSeriousMalfunction();
				if (failure != null) result = failure.getName();
			} break;

			case WATER : 
				result = vehicle.getAmountResourceStored(ResourceUtil.waterID);
				break;

			case FOOD : 
				result = vehicle.getAmountResourceStored(ResourceUtil.foodID);
				break;

			case OXYGEN : 
				result = vehicle.getAmountResourceStored(ResourceUtil.oxygenID);
				break;

			case METHANOL : 
				result = vehicle.getAmountResourceStored(ResourceUtil.methanolID);
				break;

			case ROCK_SAMPLES : 
				result = vehicle.getAmountResourceStored(ResourceUtil.rockSamplesID);
				break;

			case ICE : 
				result = vehicle.getAmountResourceStored(ResourceUtil.iceID);
				break;
			
			default:
				throw new IllegalArgumentException("Unknown column");
		}

		return result;
	}

	/**
	 * Catches unit update event.
	 * 
	 * @param event the unit event.
	 */
	@Override
	public void unitUpdate(UnitEvent event) {
		Vehicle vehicle = (Vehicle) event.getSource();
		Object target = event.getTarget();
		UnitEventType eventType = event.getType();

		int columnNum = -1;
		switch(eventType) {
			case NAME_EVENT: columnNum = NAME; break;
			case LOCATION_EVENT: columnNum = LOCATION; break;
			case INVENTORY_STORING_UNIT_EVENT:
			case INVENTORY_RETRIEVING_UNIT_EVENT:
				if (((Unit)target).getUnitType() == UnitType.PERSON)
					columnNum = CREW;
				break;
			case OPERATOR_EVENT: columnNum = DRIVER; break;
			case STATUS_EVENT: columnNum = STATUS; break;
			case EMERGENCY_BEACON_EVENT: columnNum = BEACON; break;
			case RESERVED_EVENT: columnNum = RESERVED; break;
			case SPEED_EVENT: columnNum = SPEED; break;
			case MALFUNCTION_EVENT: columnNum = MALFUNCTION; break;
			case INVENTORY_RESOURCE_EVENT: {
				int resourceId = -1;
				if (target instanceof AmountResource ar) {
					resourceId = ar.getID();
				}
				else if (target instanceof Integer item) {
					resourceId = item;
					if (resourceId >= ResourceUtil.FIRST_ITEM_RESOURCE_ID)
						// if it's an item resource, quit
						return;
				}

				if (RESOURCE_TO_COL.containsKey(resourceId)) 
					columnNum = RESOURCE_TO_COL.get(resourceId);
			} break;
			default:
		}

		if (columnNum > -1) {
			entityValueUpdated(vehicle, columnNum, columnNum);
		}
	}
	
	/**
	 * Prepares the model for deletion.
	 */
	@Override
	public void destroy() {
		super.destroy();

		if (missionManagerListener != null) {
			missionManagerListener.destroy();
		}
		missionManagerListener = null;
	}


	private class LocalMissionManagerListener implements MissionManagerListener {

		private List<Mission> missions;
		private MissionListener missionListener;

		LocalMissionManagerListener() {
			missionListener = new LocalMissionListener();

			missions = missionManager.getMissions();

			for (Mission m : missions)
				addMission(m);
		}

		/**
		 * Adds a new mission.
		 * 
		 * @param mission the new mission.
		 */
		public void addMission(Mission mission) {
			mission.addMissionListener(missionListener);
			fireTableDataChanged();
		}

		/**
		 * Removes an old mission.
		 * 
		 * @param mission the old mission.
		 */
		public void removeMission(Mission mission){
			mission.removeMissionListener(missionListener);
			fireTableDataChanged();
		}

		/**
		 * Prepares for deletion.
		 */
		public void destroy() {
			for (Mission m : missions) removeMission(m);
			missions = null;
			missionListener = null;
		}
	}

	/**
	 * MissionListener inner class.
	 */
	private class LocalMissionListener implements MissionListener {

		/**
		 * Catch mission update event.
		 * @param event the mission event.
		 */
		public void missionUpdate(MissionEvent event) {
			Mission mission = (Mission) event.getSource();
			MissionEventType eventType = event.getType();
			int columnNum = switch(eventType) {
				case TRAVEL_STATUS_EVENT, NAVPOINTS_EVENT -> DESTINATION;
				case DISTANCE_EVENT -> DESTDIST;
				case VEHICLE_EVENT -> MISSION;
				default -> -1;
			};

			if ((columnNum > -1) && (mission instanceof VehicleMission vm)) {
				Vehicle vehicle = vm.getVehicle();
				if (vehicle != null) {
					entityValueUpdated(vehicle, columnNum, columnNum);
				}
			}
		}
	}
}
