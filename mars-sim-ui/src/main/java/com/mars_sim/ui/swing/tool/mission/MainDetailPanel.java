/*
 * Mars Simulation Project
 * MainDetailPanel.java
 * @date 2024-07-12
 * @author Scott Davis
 */

package com.mars_sim.ui.swing.tool.mission;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import com.mars_sim.core.Entity;
import com.mars_sim.core.Unit;
import com.mars_sim.core.UnitEvent;
import com.mars_sim.core.UnitEventType;
import com.mars_sim.core.UnitListener;
import com.mars_sim.core.UnitType;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.mission.AreologyFieldStudy;
import com.mars_sim.core.person.ai.mission.BiologyFieldStudy;
import com.mars_sim.core.person.ai.mission.ConstructionMission;
import com.mars_sim.core.person.ai.mission.SalvageMission;
import com.mars_sim.core.person.ai.mission.CollectIce;
import com.mars_sim.core.person.ai.mission.CollectRegolith;
import com.mars_sim.core.person.ai.mission.Delivery;
import com.mars_sim.core.person.ai.mission.EmergencySupply;
import com.mars_sim.core.person.ai.mission.Exploration;
import com.mars_sim.core.person.ai.mission.MeteorologyFieldStudy;
import com.mars_sim.core.person.ai.mission.Mining;
import com.mars_sim.core.person.ai.mission.Mission;
import com.mars_sim.core.person.ai.mission.MissionEvent;
import com.mars_sim.core.person.ai.mission.MissionEventType;
import com.mars_sim.core.person.ai.mission.MissionListener;
import com.mars_sim.core.person.ai.mission.MissionLog;
import com.mars_sim.core.person.ai.mission.MissionStatus;
import com.mars_sim.core.person.ai.mission.RescueSalvageVehicle;
import com.mars_sim.core.person.ai.mission.Trade;
import com.mars_sim.core.person.ai.mission.VehicleMission;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.tool.Conversion;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.core.vehicle.GroundVehicle;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.core.vehicle.VehicleType;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.MarsPanelBorder;
import com.mars_sim.ui.swing.StyleManager;
import com.mars_sim.ui.swing.utils.AttributePanel;
import com.mars_sim.ui.swing.utils.EntityModel;
import com.mars_sim.ui.swing.utils.EntityLauncher;

/**
 * The tab panel for showing mission details.
 */
@SuppressWarnings("serial")
public class MainDetailPanel extends JPanel implements MissionListener, UnitListener {

	// Custom mission panel IDs.
	private static final String EMPTY = Msg.getString("MainDetailPanel.empty"); //$NON-NLS-1$

	private static final int MAX_LENGTH = 48;
	private static final int WIDTH_1 = 300;
	private static final int WIDTH_2 = 250;
	private static final int HEIGHT_1 = 125;
	private static final int HEIGHT_2 = 250;
	
	// Private members
	private JLabel vehicleStatusLabel;
	private JLabel speedLabel;
	private JLabel distanceNextNavLabel;
	private JLabel traveledLabel;
	
	private JLabel typeTextField;
	private JLabel designationTextField;
	private JLabel settlementTextField;
	private JLabel leadTextField;
	private JLabel phaseTextField;
	private JLabel statusTextField;
	
	private JLabel memberLabel = new JLabel("", SwingConstants.LEFT);
	
	private MemberTableModel memberTableModel;
	private JTable memberTable;

	private JButton centerMapButton;
	private JButton vehicleButton;

	private CardLayout customPanelLayout;

	private JPanel missionCustomPane;
	private JPanel memberPane;
	private JPanel memberOuterPane;
	
	private Mission missionCache;
	private Vehicle currentVehicle;
	private MissionWindow missionWindow;
	private MainDesktopPane desktop;

	private Map<String, MissionCustomInfoPanel> customInfoPanels;

	private LogTableModel logTableModel;


	/**
	 * Constructor.
	 *
	 * @param desktop the main desktop panel.
	 */
	public MainDetailPanel(MainDesktopPane desktop, MissionWindow missionWindow) {
		// User JPanel constructor.
		super();
		// Initialize data members.
		this.desktop = desktop;
		this.missionWindow = missionWindow;
		
		// Set the layout.
		setLayout(new BorderLayout());
        setMaximumSize(new Dimension(MissionWindow.WIDTH - MissionWindow.LEFT_PANEL_WIDTH, MissionWindow.HEIGHT));
        
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getVerticalScrollBar().setUnitIncrement(10);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		add(scrollPane, BorderLayout.CENTER);

		// Create the main panel.
		JPanel mainBox = new JPanel(new BorderLayout(1, 1));
		scrollPane.setViewportView(mainBox);

		// Create the top box.
		JPanel topBox = new JPanel(new BorderLayout(1, 1));
		topBox.setBorder(new MarsPanelBorder());
		mainBox.add(topBox, BorderLayout.NORTH);

		// Create the center box.
		JPanel centerBox = new JPanel(new BorderLayout(1, 1));
		centerBox.setBorder(new MarsPanelBorder());
		mainBox.add(centerBox, BorderLayout.CENTER);

		// Create the member panel.
		JPanel bottomBox = new JPanel(new BorderLayout(1, 1));
		mainBox.add(bottomBox, BorderLayout.SOUTH);

		topBox.add(initMissionPane(), BorderLayout.CENTER);
		topBox.add(initLogPane(), BorderLayout.SOUTH);

		// Create the member panel.
		JPanel leftRightBox = new JPanel(new GridLayout(1, 2));
		leftRightBox.add(initVehiclePane());
		leftRightBox.add(initLocationPane());
		centerBox.add(leftRightBox, BorderLayout.NORTH);

		centerBox.add(initTravelPane(), BorderLayout.CENTER);

		memberOuterPane = new JPanel(new BorderLayout(1, 1));
		Border blackline = StyleManager.createLabelBorder("Team Members");
		memberOuterPane.setBorder(blackline);
			
		memberPane = initMemberPane();
		memberOuterPane.add(memberPane, BorderLayout.CENTER);
				
		bottomBox.add(memberOuterPane, BorderLayout.NORTH);
		bottomBox.add(initCustomMissionPane(), BorderLayout.SOUTH);
		
		// Update the log table model
		logTableModel.update();
	}

	/**
	 * Initializes the mission pane.
	 * 
	 * @return
	 */
	private JPanel initMissionPane() {

		// Create the vehicle pane.
		JPanel missionLayout = new JPanel(new BorderLayout());
		Border blackline = StyleManager.createLabelBorder("Profile");
		missionLayout.setBorder(blackline);
	
		// Prepare count spring layout panel.
		AttributePanel missionPanel = new AttributePanel(6);
		missionLayout.add(missionPanel, BorderLayout.NORTH);
		
		typeTextField = missionPanel.addTextField(Msg.getString("MainDetailPanel.type"), "", null); // $NON-NLS-1$
		designationTextField = missionPanel.addTextField(Msg.getString("MainDetailPanel.designation"), "",null); // $NON-NLS-1$
		settlementTextField = missionPanel.addTextField(Msg.getString("MainDetailPanel.settlement"), "", null); // $NON-NLS-1$
		leadTextField = missionPanel.addTextField(Msg.getString("MainDetailPanel.startingMember"), "", null); // $NON-NLS-1$
		phaseTextField = missionPanel.addTextField(Msg.getString("MainDetailPanel.phase"), "", null); // $NON-NLS-1$
		statusTextField = missionPanel.addTextField(Msg.getString("MainDetailPanel.missionStatus"), "", null); // $NON-NLS-1$

		
		return missionLayout;
	}
	
	/**
	 * Initializes the vehicle pane.
	 * 
	 * @return
	 */
	private JPanel initVehiclePane() {
		
		JPanel mainLayout = new JPanel(new BorderLayout(5, 5));
		Border blackline = StyleManager.createLabelBorder(Msg.getString("MainDetailPanel.vehicle")); //$NON-NLS-1$
		mainLayout.setBorder(blackline);

		// Create the vehicle panel.
		vehicleButton = new JButton(" ");
		vehicleButton.addActionListener(e -> {
			if (missionCache instanceof VehicleMission vehicleMission) {
				Vehicle vehicle = vehicleMission.getVehicle();
				if (vehicle != null) {
					getDesktop().showDetails(vehicle);
				}
			} else if (missionCache instanceof ConstructionMission constructionMission) {
				if (!constructionMission.getConstructionVehicles().isEmpty()) {
					Vehicle vehicle = constructionMission.getConstructionVehicles().get(0);
					getDesktop().showDetails(vehicle);
				}
			}
		});
		vehicleButton.setVisible(false);
		vehicleButton.setToolTipText(Msg.getString("MainDetailPanel.vehicleToolTip")); //$NON-NLS-1$

		JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));//BorderLayout(5, 5));
		buttonWrapper.add(vehicleButton);
		mainLayout.add(buttonWrapper, BorderLayout.CENTER);
		
		return mainLayout;
	}
	
	private JPanel initLocationPane() {

		JPanel mainLayout = new JPanel(new BorderLayout(5, 5));
		Border blackline = StyleManager.createLabelBorder(Msg.getString("MainDetailPanel.location")); //$NON-NLS-1$
		mainLayout.setBorder(blackline);
		
		// Create center map button
		centerMapButton = new JButton(ImageLoader.getIconByName("mars")); //$NON-NLS-1$
//		centerMapButton.setMargin(new Insets(2, 2, 2, 2));
		centerMapButton.addActionListener(e -> {
			if (missionCache != null)
				getDesktop().centerMapGlobe(missionCache.getCurrentMissionLocation());
		});
		centerMapButton.setEnabled(false);
		centerMapButton.setToolTipText(Msg.getString("MainDetailPanel.gotoMarsMap")); //$NON-NLS-1$
		
		JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER));//BorderLayout(5, 5));
		buttonWrapper.add(centerMapButton);
		mainLayout.add(buttonWrapper, BorderLayout.CENTER);
		
		return mainLayout;
	}

	/**
	 * Initializes the travel pane.
	 * 
	 * @return
	 */
	private JPanel initTravelPane() {
		
		JPanel mainLayout = new JPanel(new BorderLayout());
		mainLayout.setAlignmentX(CENTER_ALIGNMENT);
		mainLayout.setAlignmentY(CENTER_ALIGNMENT);
		Border blackline = StyleManager.createLabelBorder("Travel");
		mainLayout.setBorder(blackline);
		
		// Prepare travel grid layout.
		AttributePanel travelGridPane = new AttributePanel(4);
		mainLayout.add(travelGridPane, BorderLayout.CENTER);

		vehicleStatusLabel = travelGridPane.addTextField(Msg.getString("MainDetailPanel.vehicleStatus"), "", null);
		speedLabel = travelGridPane.addTextField(Msg.getString("MainDetailPanel.vehicleSpeed"), "", null);
		distanceNextNavLabel = travelGridPane.addTextField(Msg.getString("MainDetailPanel.distanceNextNavPoint"), "", null);
		traveledLabel = travelGridPane.addTextField(Msg.getString("MainDetailPanel.distanceTraveled"), "", null);

		return mainLayout;
	}

	/**
	 * Initializes the phase log pane.
	 * 
	 * @return
	 */
	private JPanel initLogPane() {

		// Create the member panel.
		JPanel logPane = new JPanel(new BorderLayout());
		Border blackline = StyleManager.createLabelBorder("Phase Log");
		logPane.setBorder(blackline);
		logPane.setAlignmentX(Component.RIGHT_ALIGNMENT);
		logPane.setPreferredSize(new Dimension(WIDTH_2, HEIGHT_2));

		// Create scroll panel for member list.
		JScrollPane logScrollPane = new JScrollPane();
		logPane.add(logScrollPane, BorderLayout.CENTER);

		// Create member table model.
		logTableModel = new LogTableModel();

		// Create member table.
		JTable logTable = new JTable(logTableModel);
		logTable.getColumnModel().getColumn(0).setPreferredWidth(80);
		logTable.getColumnModel().getColumn(1).setPreferredWidth(150);

		logScrollPane.setViewportView(logTable);

		return logPane;
	}

	/**
	 * Initializes the member pane.
	 * 
	 * @return
	 */
	private JPanel initMemberPane() {
		
		if (memberPane == null) {	
			// Create the member panel.
			memberPane = new JPanel(new BorderLayout(1, 1));
			memberPane.setAlignmentX(Component.RIGHT_ALIGNMENT);
	
			// Create member bottom panel.
			JPanel memberBottomPane = new JPanel(new BorderLayout(5, 5));
			memberBottomPane.setAlignmentX(Component.RIGHT_ALIGNMENT);
			memberPane.add(memberBottomPane);
	
			// Prepare member list panel
			JPanel memberListPane = new JPanel(new BorderLayout(5, 5));
			memberListPane.setPreferredSize(new Dimension(WIDTH_1, HEIGHT_1));
			memberBottomPane.add(memberListPane, BorderLayout.CENTER);
	
			// Create scroll panel for member list.
			JScrollPane memberScrollPane = new JScrollPane();
			memberListPane.add(memberScrollPane, BorderLayout.CENTER);
	
			// Create member table model.
			memberTableModel = new MemberTableModel();
	
			// Create member table.
			memberTable = new JTable(memberTableModel);
			memberTable.getColumnModel().getColumn(0).setPreferredWidth(80);
			memberTable.getColumnModel().getColumn(1).setPreferredWidth(150);
			memberTable.getColumnModel().getColumn(2).setPreferredWidth(40);
			memberTable.getColumnModel().getColumn(3).setPreferredWidth(40);
			memberTable.setRowSelectionAllowed(true);
			memberTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			EntityLauncher.attach(memberTable, desktop);
			memberScrollPane.setViewportView(memberTable);
		}
		
		return memberPane;
	}

	/**
	 * Initializes the custom mission pane.
	 * 
	 * @return
	 */
	private JPanel initCustomMissionPane() {

		// Create the mission custom panel.
		customPanelLayout = new CardLayout(10, 10);
		missionCustomPane = new JPanel(customPanelLayout);
		missionCustomPane.setAlignmentX(Component.RIGHT_ALIGNMENT);

		Border blackline = StyleManager.createLabelBorder("Mission Specific");
		missionCustomPane.setBorder(blackline);
		
		// Create custom empty panel.
		JPanel emptyCustomPanel = new JPanel();
		missionCustomPane.add(emptyCustomPanel, EMPTY);
		customInfoPanels = new HashMap<>();

		// Create custom areology field mission panel.
		MissionCustomInfoPanel areologyFieldPanel = new AreologyStudyFieldMissionCustomInfoPanel(desktop);
		String areologyMissionName = AreologyFieldStudy.class.getName();
		customInfoPanels.put(areologyMissionName, areologyFieldPanel);
		missionCustomPane.add(areologyFieldPanel, areologyMissionName);

		// Create custom biology field mission panel.
		MissionCustomInfoPanel biologyFieldPanel = new BiologyStudyFieldMissionCustomInfoPanel(desktop);
		String biologyMissionName = BiologyFieldStudy.class.getName();
		customInfoPanels.put(biologyMissionName, biologyFieldPanel);
		missionCustomPane.add(biologyFieldPanel, biologyMissionName);

		// Create custom meteorology field mission panel.
		MissionCustomInfoPanel meteorologyFieldPanel = new MeteorologyStudyFieldMissionCustomInfoPanel(desktop);
		String meteorologyMissionName = MeteorologyFieldStudy.class.getName();
		customInfoPanels.put(meteorologyMissionName, meteorologyFieldPanel);
		missionCustomPane.add(meteorologyFieldPanel, meteorologyMissionName);

		// Create custom delivery mission panel.
		MissionCustomInfoPanel deliveryPanel = new DeliveryMissionCustomInfoPanel();
		String deliveryMissionName = Delivery.class.getName();
		customInfoPanels.put(deliveryMissionName, deliveryPanel);
		missionCustomPane.add(deliveryPanel, deliveryMissionName);

		// Create custom trade mission panel.
		MissionCustomInfoPanel tradePanel = new TradeMissionCustomInfoPanel();
		String tradeMissionName = Trade.class.getName();
		customInfoPanels.put(tradeMissionName, tradePanel);
		missionCustomPane.add(tradePanel, tradeMissionName);

		// Create custom mining mission panel.
		MissionCustomInfoPanel miningPanel = new MiningMissionCustomInfoPanel(desktop);
		String miningMissionName = Mining.class.getName();
		customInfoPanels.put(miningMissionName, miningPanel);
		missionCustomPane.add(miningPanel, miningMissionName);

		// Create custom construction mission panel.
		MissionCustomInfoPanel constructionPanel = new ConstructionMissionCustomInfoPanel(desktop);
		String constructionMissionName = ConstructionMission.class.getName();
		customInfoPanels.put(constructionMissionName, constructionPanel);
		missionCustomPane.add(constructionPanel, constructionMissionName);

		// Create custom salvage mission panel.
		MissionCustomInfoPanel salvagePanel = new SalvageMissionCustomInfoPanel(desktop);
		String salvageMissionName = SalvageMission.class.getName();
		customInfoPanels.put(salvageMissionName, salvagePanel);
		missionCustomPane.add(salvagePanel, salvageMissionName);

		// Create custom exploration mission panel.
		MissionCustomInfoPanel explorationPanel = new ExplorationCustomInfoPanel(ResourceUtil.rockIDs);
		String explorationMissionName = Exploration.class.getName();
		customInfoPanels.put(explorationMissionName, explorationPanel);
		missionCustomPane.add(explorationPanel, explorationMissionName);

		// Create custom collect regolith mission panel.
		MissionCustomInfoPanel collectRegolithPanel = new CollectResourcesMissionCustomInfoPanel(ResourceUtil.REGOLITH_TYPES);
		String collectRegolithMissionName = CollectRegolith.class.getName();
		customInfoPanels.put(collectRegolithMissionName, collectRegolithPanel);
		missionCustomPane.add(collectRegolithPanel, collectRegolithMissionName);

		// Create custom collect ice mission panel.
		MissionCustomInfoPanel collectIcePanel = new CollectResourcesMissionCustomInfoPanel(new int[] {ResourceUtil.iceID});
		String collectIceMissionName = CollectIce.class.getName();
		customInfoPanels.put(collectIceMissionName, collectIcePanel);
		missionCustomPane.add(collectIcePanel, collectIceMissionName);

		// Create custom rescue/salvage vehicle mission panel.
		MissionCustomInfoPanel rescuePanel = new RescueMissionCustomInfoPanel(desktop);
		String rescueMissionName = RescueSalvageVehicle.class.getName();
		customInfoPanels.put(rescueMissionName, rescuePanel);
		missionCustomPane.add(rescuePanel, rescueMissionName);

		// Create custom emergency supply mission panel.
		MissionCustomInfoPanel emergencySupplyPanel = new EmergencySupplyMissionCustomInfoPanel();
		String emergencySupplyMissionName = EmergencySupply.class.getName();
		customInfoPanels.put(emergencySupplyMissionName, emergencySupplyPanel);
		missionCustomPane.add(emergencySupplyPanel, emergencySupplyMissionName);

		return missionCustomPane;
	}


	
	public void setCurrentMission(Mission mission) {
		if (missionCache != null) {
			if (!missionCache.equals(mission)) {
				missionCache = mission;
			}
		}
		else {
			missionCache = mission;
		}
	}
	
	public Mission getCurrentMission() {
		return missionCache;
	}

	/**
	 * Installs a listener to receive notification when the text of any
	 * {@code JTextComponent} is changed. Internally, it installs a
	 * {@link DocumentListener} on the text component's {@link Document},
	 * and a {@link PropertyChangeListener} on the text component to detect
	 * if the {@code Document} itself is replaced.
	 *
	 * @param text any text component, such as a {@link JTextField}
	 *        or {@link JTextArea}
	 * @param changeListener a listener to receive {@link ChangeEvent}
	 *        when the text is changed; the source object for the events
	 *        will be the text component
	 * @throws NullPointerException if either parameter is null
	 */
	public static void addChangeListener(JTextComponent text, ChangeListener changeListener) {
	    Objects.requireNonNull(text);
	    Objects.requireNonNull(changeListener);
	    DocumentListener dl = new DocumentListener() {
	        private int lastChange = 0, lastNotifiedChange = 0;

	        @Override
	        public void insertUpdate(DocumentEvent e) {
	            changedUpdate(e);
	        }

	        @Override
	        public void removeUpdate(DocumentEvent e) {
	            changedUpdate(e);
	        }

	        @Override
	        public void changedUpdate(DocumentEvent e) {
	            lastChange++;
	            SwingUtilities.invokeLater(() -> {
	                if (lastNotifiedChange != lastChange) {
	                    lastNotifiedChange = lastChange;
	                    changeListener.stateChanged(new ChangeEvent(text));
	                }
	            });
	        }
	    };
	    text.addPropertyChangeListener("document", (PropertyChangeEvent e) -> {
	        Document d1 = (Document)e.getOldValue();
	        Document d2 = (Document)e.getNewValue();
	        if (d1 != null) d1.removeDocumentListener(dl);
	        if (d2 != null) d2.addDocumentListener(dl);
	        dl.changedUpdate(null);
	    });
	    Document d = text.getDocument();
	    if (d != null) d.addDocumentListener(dl);
	}

	/**
	 * Sets to the given mission.
	 *
	 * @param newMission
	 */
	public void setMission(Mission newMission) {
		// Remove this as previous mission listener.
		if (missionCache != null)
			missionCache.removeMissionListener(this);

		if (newMission == null) {	
			clearInfo();
			return;
		}
				
		missionCache = newMission;
		
		// Add this as listener for new mission.
		newMission.addMissionListener(this);
		
		setCurrentMission(newMission);
		// Update info on Main tab
		updateMainTab(newMission);
		// Update custom mission panel.
		updateCustomPanel(newMission);
	}


	/**
	 * Updates the mission content on the Main tab.
	 *
	 * @param mission
	 */
	public void updateMainTab(Mission mission) {

		if (mission == null || missionCache == null) {	
			clearInfo();
			return;
		}

		if (mission.isDone()) {
			// Check if the mission is done and the members have been disbanded
			memberOuterPane.removeAll();
			memberOuterPane.add(memberLabel);
			memberLabel.setText("    [Disbanded] : " + printMembers(mission));
		}
		else {
			memberOuterPane.removeAll();
			memberOuterPane.add(initMemberPane());
			memberTableModel.setMission(mission);
		}
		
		String d = mission.getFullMissionDesignation();
		if (d == null || d.equals(""))
			d = "";
		designationTextField.setText(d);
		typeTextField.setText(mission.getName());
		
		leadTextField.setText(mission.getStartingPerson().getName());

		String phaseText = mission.getPhaseDescription();
		phaseTextField.setToolTipText(phaseText);
		if (phaseText.length() > MAX_LENGTH)
			phaseText = phaseText.substring(0, MAX_LENGTH) + "...";
		phaseTextField.setText(phaseText);

		var missionStatusText = new StringBuilder();
		missionStatusText.append(mission.getMissionStatus().stream().map(MissionStatus::getName).collect(Collectors.joining(", ")));
		statusTextField.setText(missionStatusText.toString());
		
		settlementTextField.setText(mission.getAssociatedSettlement().getName());

		logTableModel.setMission(mission);
		logTableModel.update();
		
		centerMapButton.setEnabled(true);
		
		// Update mission vehicle info in UI.
		if (mission instanceof VehicleMission vehicleMission) {
			Vehicle vehicle = vehicleMission.getVehicle();
			if (vehicle != null) {
				vehicleButton.setText(vehicle.getName());
				vehicleButton.setVisible(true);
			}

			if (vehicle != null && !mission.isDone()) {
				vehicleStatusLabel.setText(vehicle.printStatusTypes());
				speedLabel.setText(StyleManager.DECIMAL_KPH.format(vehicle.getSpeed())); //$NON-NLS-1$
				try {
					int currentLegRemainingDist = (int) vehicleMission.getDistanceCurrentLegRemaining();
					distanceNextNavLabel.setText(StyleManager.DECIMAL_KM.format(currentLegRemainingDist)); //$NON-NLS-1$
				} catch (Exception e2) {
				}

				double travelledDistance = Math.round(vehicleMission.getTotalDistanceTravelled()*10.0)/10.0;
				double estTotalDistance = Math.round(vehicleMission.getTotalDistanceProposed()*10.0)/10.0;

				traveledLabel.setText(Msg.getString("MainDetailPanel.kmTraveled", //$NON-NLS-1$
						travelledDistance,
						estTotalDistance
						));

				if (!vehicle.equals(currentVehicle)) {
					vehicle.addUnitListener(this);
					if (currentVehicle != null) {
						currentVehicle.removeUnitListener(this);
					}
					vehicle.addUnitListener(this);
					currentVehicle = vehicle;
				}
			}
			else {
				vehicleStatusLabel.setText(" ");
				speedLabel.setText(StyleManager.DECIMAL_KPH.format(0)); //$NON-NLS-1$ //$NON-NLS-2$
				distanceNextNavLabel.setText(StyleManager.DECIMAL_KM.format(0)); //$NON-NLS-1$ //$NON-NLS-2$
		
				double travelledDistance = Math.round(vehicleMission.getTotalDistanceTravelled()*10.0)/10.0;
				double estTotalDistance = Math.round(vehicleMission.getTotalDistanceProposed()*10.0)/10.0;

				traveledLabel.setText(Msg.getString("MainDetailPanel.kmTraveled", //$NON-NLS-1$
						travelledDistance,
						estTotalDistance
						));
				
				if (currentVehicle != null) {
					currentVehicle.removeUnitListener(this);
				}
				currentVehicle = null;
			}
		} else if (mission instanceof ConstructionMission constructionMission) {
			// Display first of mission's list of construction vehicles.
			List<GroundVehicle> constVehicles = constructionMission.getConstructionVehicles();
			if (!constVehicles.isEmpty()) {
				Vehicle vehicle = constVehicles.get(0);
				vehicleButton.setText(vehicle.getName());
				vehicleButton.setVisible(true);
				vehicleStatusLabel.setText(vehicle.printStatusTypes());
				speedLabel.setText(StyleManager.DECIMAL_KPH.format(vehicle.getSpeed())); //$NON-NLS-1$
				distanceNextNavLabel.setText(StyleManager.DECIMAL_KM.format(0)); //$NON-NLS-1$ //$NON-NLS-2$
				traveledLabel.setText(Msg.getString("MainDetailPanel.kmTraveled", "0", "0")); //$NON-NLS-1$ //$NON-NLS-2$
				vehicle.addUnitListener(this);
				currentVehicle = vehicle;
			}
		}

		// Add mission listener.
		mission.addMissionListener(this);
		missionCache = mission;
	}


	/**
	 * Clears the mission content on the Main tab.
	 */
	public void clearInfo() {
		// NOTE: do NOT clear the mission info. Leave the info there for future viewing
		// Clear mission info in UI.
		leadTextField.setText(" ");
		designationTextField.setText(" ");
		typeTextField.setText(" ");
		phaseTextField.setText(" ");
		phaseTextField.setToolTipText(" ");
		
		statusTextField.setText(" ");
		settlementTextField.setText(" ");

		memberTableModel.setMission(null);
		
		logTableModel.update();
		logTableModel.setMission(null);
		
		centerMapButton.setEnabled(false);
		
		vehicleStatusLabel.setText(" ");
		speedLabel.setText(StyleManager.DECIMAL_KPH.format(0)); //$NON-NLS-1$ //$NON-NLS-2$
		distanceNextNavLabel.setText(StyleManager.DECIMAL_KM.format(0)); //$NON-NLS-1$ //$NON-NLS-2$
		traveledLabel.setText(Msg.getString("MainDetailPanel.kmTraveled", "0", "0")); //$NON-NLS-1$ //$NON-NLS-2$
		
		if (missionCache != null) {
			missionCache.removeMissionListener(this);
		}
		missionCache = null;
		
		if (currentVehicle != null)
			currentVehicle.removeUnitListener(this);
		currentVehicle = null;
		
		customPanelLayout.show(missionCustomPane, EMPTY);
	}

	/**
	 * Prints the list of members.
	 * 
	 * @return
	 */
	private String printMembers(Mission mission) {
		Set<Worker> list = mission.getSignup();
		if (list.isEmpty()) {
			return "";
		}
		
		return list.stream().map(Worker::getName).collect(Collectors.joining(", "));
	}
	
	/**
	 * Updates the custom mission panel with a mission.
	 *
	 * @param mission the mission.
	 */
	private void updateCustomPanel(Mission mission) {
		boolean hasMissionPanel = false;
		if (mission != null) {		
			String missionClassName = mission.getClass().getName();
			if (customInfoPanels.containsKey(missionClassName)) {
				hasMissionPanel = true;
				MissionCustomInfoPanel panel = customInfoPanels.get(missionClassName);
				customPanelLayout.show(missionCustomPane, missionClassName);
				panel.updateMission(mission);
			}
		}

		if (!hasMissionPanel)
			customPanelLayout.show(missionCustomPane, EMPTY);
	}

	/**
	 * Mission event update.
	 */
	public void missionUpdate(MissionEvent e) {
		if (e.getSource().equals(missionCache)) {
			SwingUtilities.invokeLater(new MissionEventUpdater(e, this));
		}
	}

	/**
	 * Updates the custom mission panels with a mission event.
	 *
	 * @param e the mission event.
	 */
	private void updateCustomPanelMissionEvent(MissionEvent e) {
		Mission mission = (Mission) e.getSource();
		if (mission != null) {
			String missionClassName = mission.getClass().getName();
			if (customInfoPanels.containsKey(missionClassName)) {
				customInfoPanels.get(missionClassName).updateMissionEvent(e);
			}
		}
	}

	/**
	 * Catches unit update event.
	 *
	 * @param event the unit event.
	 */
	public void unitUpdate(UnitEvent event) {
		if ((((Unit)event.getSource()).getUnitType() == UnitType.VEHICLE)
			&& event.getSource().equals(currentVehicle)) {
				SwingUtilities.invokeLater(new VehicleInfoUpdater(event));
		}
	}

	public MissionWindow getMissionWindow() {
		return missionWindow;
	}

	public void destroy() {
		designationTextField = null;
		typeTextField = null;
		leadTextField = null;
		phaseTextField = null;
		statusTextField = null;
		settlementTextField = null;
		vehicleStatusLabel = null;
		speedLabel = null;
		distanceNextNavLabel = null;
		traveledLabel = null;
		centerMapButton = null;
		vehicleButton = null;
		customPanelLayout = null;
		missionCustomPane = null;
		missionCache = null;
		currentVehicle = null;
		missionWindow = null;
		desktop = null;
		memberTable = null;
		memberTableModel = null;
	}
	
	/**
	 * Gets the main desktop.
	 *
	 * @return desktop.
	 */
	private MainDesktopPane getDesktop() {
		return desktop;
	}

	private class MissionEventUpdater implements Runnable {

		private MissionEvent event;
		private MainDetailPanel panel;

		private MissionEventUpdater(MissionEvent event, MainDetailPanel panel) {
			this.event = event;
			this.panel = panel;
		}

		public void run() {
			Mission mission = (Mission) event.getSource();
			MissionEventType type = event.getType();

			// Update UI based on mission event type.
			if (type == MissionEventType.TYPE_EVENT || type == MissionEventType.TYPE_ID_EVENT)
				typeTextField.setText(mission.getName());
			else if (type == MissionEventType.DESCRIPTION_EVENT) {
				// Implement the missing descriptionLabel
			}
			else if (type == MissionEventType.DESIGNATION_EVENT) {
				// Implement the missing descriptionLabel
				if (missionWindow.getCreateMissionWizard() != null) {
					String s = mission.getFullMissionDesignation();
					if (s == null || s.equals("")) {
						s = "[TBA]";
					}

					designationTextField.setText(Conversion.capitalize(s));
				}
			} else if (type == MissionEventType.PHASE_DESCRIPTION_EVENT) {
				String phaseText = mission.getPhaseDescription();
				if (phaseText.length() > MAX_LENGTH)
					phaseText = phaseText.substring(0, MAX_LENGTH) + "...";
				phaseTextField.setText(phaseText);
				
				// Update the log table model
				logTableModel.update();
				
			} else if (type == MissionEventType.END_MISSION_EVENT) {
				var missionStatusText = new StringBuilder();
				missionStatusText.append( mission.getMissionStatus().stream().map(MissionStatus::getName).collect(Collectors.joining(", ")));
				statusTextField.setText(missionStatusText.toString());
			} else if (type == MissionEventType.ADD_MEMBER_EVENT || type == MissionEventType.REMOVE_MEMBER_EVENT
					|| type == MissionEventType.MIN_MEMBERS_EVENT || type == MissionEventType.CAPACITY_EVENT) {
				memberTableModel.updateMembers();
			} else if (type == MissionEventType.VEHICLE_EVENT) {
				Vehicle vehicle = ((VehicleMission) mission).getVehicle();
				if (vehicle != null) {
					vehicleButton.setText(vehicle.getName());
					vehicleButton.setVisible(true);
					vehicleStatusLabel.setText(vehicle.printStatusTypes());
					speedLabel.setText(StyleManager.DECIMAL_KPH.format(vehicle.getSpeed())); //$NON-NLS-1$
					vehicle.addUnitListener(panel);
					currentVehicle = vehicle;
				} else {
					vehicleButton.setVisible(false);
					vehicleStatusLabel.setText("Not Applicable");
					speedLabel.setText(StyleManager.DECIMAL_KPH.format(0)); //$NON-NLS-1$
					if (currentVehicle != null)
						currentVehicle.removeUnitListener(panel);
					currentVehicle = null;
				}
			} else if (type == MissionEventType.DISTANCE_EVENT) {
				VehicleMission vehicleMission = (VehicleMission) mission;

				
				double travelledDistance = Math.round(vehicleMission.getTotalDistanceTravelled()*10.0)/10.0;
				double estTotalDistance = Math.round(vehicleMission.getTotalDistanceProposed()*10.0)/10.0;
				traveledLabel.setText(Msg.getString("MainDetailPanel.kmTraveled", //$NON-NLS-1$
						travelledDistance,
						estTotalDistance
						));
				
				try {
					// Make sure to call getTotalDistanceTravelled first. 
					// It should be by default already been called in performPhase's TRAVELLING
					int distanceNextNav = (int) vehicleMission.getDistanceCurrentLegRemaining();
					distanceNextNavLabel.setText(StyleManager.DECIMAL_KM.format(distanceNextNav)); //$NON-NLS-1$
				} catch (Exception e2) {
				}
			}

			// Update custom mission panel.
			updateCustomPanelMissionEvent(event);
		}
	}

	/**
	 * Inner class for updating vehicle info.
	 */
	private class VehicleInfoUpdater implements Runnable {

		private UnitEvent event;

		private VehicleInfoUpdater(UnitEvent event) {
			this.event = event;
		}

		public void run() {
			// Update vehicle info in UI based on event type.
			UnitEventType type = event.getType();
			Vehicle vehicle = (Vehicle) event.getSource();
			if (type == UnitEventType.STATUS_EVENT) {
				vehicleStatusLabel.setText(vehicle.printStatusTypes());
			} else if (type == UnitEventType.SPEED_EVENT)
				speedLabel.setText(StyleManager.DECIMAL_KPH.format(vehicle.getSpeed())); //$NON-NLS-1$
		}
	}

	
	/**
	 * Adapter for the mission log
	 */
	private class LogTableModel extends AbstractTableModel {
		
		private Mission mission;
		
		private List<MissionLog.MissionLogEntry> entries;
	
		/**
		 * Constructor.
		 */
		private LogTableModel() {
			mission = null;
			entries = new ArrayList<>();
		}

		public void update() {
			if (mission != null)
				entries = mission.getLog().getEntries();
		}
		
		/**
		 * Gets the row count.
		 *
		 * @return row count.
		 */
		public int getRowCount() {
			return (mission != null ? entries.size() : 0);
		}

		/**
		 * Gets the column count.
		 *
		 * @return column count.
		 */
		public int getColumnCount() {
			return 2;
		}

		/**
		 * Gets the column name at a given index.
		 *
		 * @param columnIndex the column's index.
		 * @return the column name.
		 */
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0)
				return "Date"; //$NON-NLS-1$
			else if (columnIndex == 1)
				return "Entry";
			else
				return Msg.getString("unknown"); //$NON-NLS-1$
		}

		/**
		 * Gets the value at a given row and column.
		 *
		 * @param row    the table row.
		 * @param column the table column.
		 * @return the value.
		 */
		public Object getValueAt(int row, int column) {
			if (mission == null || entries == null)
				return null;
				
			if (row < entries.size()) {
				if (column == 0)
					return entries.get(row).getTime().getTruncatedDateTimeStamp();
				else
					return entries.get(row).getEntry();
			} else
				return Msg.getString("unknown"); //$NON-NLS-1$
		}

		/**
		 * Sets the mission for this table model.
		 *
		 * @param newMission the new mission.
		 */
		void setMission(Mission newMission) {
			this.mission = newMission;
			fireTableDataChanged();
		}
	}

	/**
	 * Table model for mission members.
	 */
	private class MemberTableModel extends AbstractTableModel implements UnitListener, EntityModel {

		// Private members.
		private Mission mission;
		private List<Worker> members;

		/**
		 * Constructor.
		 */
		private MemberTableModel() {
			mission = null;
			members = new ArrayList<>();
		}
		
		/**
		 * Gets the row count.
		 *
		 * @return row count.
		 */
		public int getRowCount() {
			return members.size();
		}

		/**
		 * Gets the column count.
		 *
		 * @return column count.
		 */
		public int getColumnCount() {
			return 4;
		}

		/**
		 * Gets the column name at a given index.
		 *
		 * @param columnIndex the column's index.
		 * @return the column name.
		 */
		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0)
				return Msg.getString("MainDetailPanel.column.name"); //$NON-NLS-1$
			else if (columnIndex == 1)
				return Msg.getString("MainDetailPanel.column.task"); //$NON-NLS-1$
			else if (columnIndex == 2)
				return Msg.getString("MainDetailPanel.column.onboard"); //$NON-NLS-1$
			else if (columnIndex == 3)
				return Msg.getString("MainDetailPanel.column.airlock"); //$NON-NLS-1$
			else
				return null;
		}

		/**
		 * Gets the value at a given row and column.
		 *
		 * @param row    the table row.
		 * @param column the table column.
		 * @return the value.
		 */
		@Override
		public Object getValueAt(int row, int column) {
			if (row < members.size()) {
				Worker member = members.get(row);
				if (column == 0)
					return member.getName();
				else if (column == 1)
					return member.getTaskDescription();
				else if (column == 2) {
					if (isOnboard(member))
						return "Y";
					return "N";
				}
				else if (column == 3) {
					if (isInAirlock(member))
						return "Y";
					return "N";
				}
				else 
					return null;
			} else
				return Msg.getString("unknown"); //$NON-NLS-1$
		}

		/**
		 * Is this member currently onboard a rover ?
		 *
		 * @param member
		 * @return
		 */
		boolean isOnboard(Worker member) {
			if (mission instanceof VehicleMission vm) {		
				if (member.getUnitType() == UnitType.PERSON) {
					Vehicle v = vm.getVehicle();
					if (VehicleType.isDrone(v.getVehicleType())) {
						return false;
					}
					else if (v instanceof Rover r) {
						if (r != null && r.isCrewmember((Person)member)) {
							return true;
						}
					}
				}
			}
			return false;
		}
		
		/**
		 * Is this member currently in vehicle's airlock ?
		 *
		 * @param member
		 * @return
		 */
		boolean isInAirlock(Worker member) {
			if (mission instanceof VehicleMission vm) {		
				if (member.getUnitType() == UnitType.PERSON) {
					Vehicle v = vm.getVehicle();
					if (VehicleType.isDrone(v.getVehicleType())) {
						return false;
					}
					else if (v instanceof Rover r) {
						if (r != null && r.isInAirlock((Person)member)) {
							return true;
						}
					}
				}
			}
			return false;
		}
		
		/**
		 * Sets the mission for this table model.
		 *
		 * @param newMission the new mission.
		 */
		void setMission(Mission newMission) {
			this.mission = newMission;
			updateMembers();
		}

		/**
		 * Catches unit update event.
		 *
		 * @param event the unit event.
		 */
		public void unitUpdate(UnitEvent event) {
			UnitEventType type = event.getType();
			Worker member = (Worker) event.getSource();
			int index = members.indexOf(member);
			if (type == UnitEventType.NAME_EVENT) {
				SwingUtilities.invokeLater(new MemberTableUpdater(index, 0));
			} else if ((type == UnitEventType.TASK_DESCRIPTION_EVENT) || (type == UnitEventType.TASK_EVENT)
					|| (type == UnitEventType.TASK_ENDED_EVENT) || (type == UnitEventType.TASK_SUBTASK_EVENT)
					|| (type == UnitEventType.TASK_NAME_EVENT)) {
				SwingUtilities.invokeLater(new MemberTableUpdater(index, 1));
			}
		}

		/**
		 * Updates mission members.
		 */
		void updateMembers() {
			
			if (mission != null) {
				
				clearMembers();
				members = new ArrayList<>(mission.getMembers());
				Iterator<Worker> i = members.iterator();
				while (i.hasNext()) {
					Worker member = i.next();
					member.addUnitListener(this);
				}
				SwingUtilities.invokeLater(new MemberTableUpdater());
			} else {
				if (!members.isEmpty()) {
					clearMembers();
					SwingUtilities.invokeLater(new MemberTableUpdater());
				}
			}
		}

		/**
		 * Clears all members from the table.
		 */
		private void clearMembers() {
			if (members != null) {
				Iterator<Worker> i = members.iterator();
				while (i.hasNext()) {
					Worker member = i.next();
					member.removeUnitListener(this);
				}
				members.clear();
			}
		}

		/**
		 * Inner class for updating member table.
		 */
		private class MemberTableUpdater implements Runnable {

			private int row;
			private int column;
			private boolean entireData;

			private MemberTableUpdater(int row, int column) {
				this.row = row;
				this.column = column;
				entireData = false;
			}

			private MemberTableUpdater() {
				entireData = true;
			}

			public void run() {
				if (entireData) {
					fireTableDataChanged();
				} else {
					fireTableCellUpdated(row, column);
				}
			}
		}

		@Override
		public Entity getAssociatedEntity(int row) {
			return members.get(row);

		}
	}
}
