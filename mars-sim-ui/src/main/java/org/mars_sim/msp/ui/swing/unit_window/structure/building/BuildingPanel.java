/*
 * Mars Simulation Project
 * BuildingPanel.java
 * @date 2021-10-06
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure.building;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.AstronomicalObservation;
import org.mars_sim.msp.core.structure.building.function.Computation;
import org.mars_sim.msp.core.structure.building.function.EVA;
import org.mars_sim.msp.core.structure.building.function.FoodProduction;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.LifeSupport;
import org.mars_sim.msp.core.structure.building.function.LivingAccommodations;
import org.mars_sim.msp.core.structure.building.function.Manufacture;
import org.mars_sim.msp.core.structure.building.function.MedicalCare;
import org.mars_sim.msp.core.structure.building.function.PowerStorage;
import org.mars_sim.msp.core.structure.building.function.Research;
import org.mars_sim.msp.core.structure.building.function.ResourceProcessing;
import org.mars_sim.msp.core.structure.building.function.Storage;
import org.mars_sim.msp.core.structure.building.function.ThermalGeneration;
import org.mars_sim.msp.core.structure.building.function.VehicleMaintenance;
import org.mars_sim.msp.core.structure.building.function.cooking.Cooking;
import org.mars_sim.msp.core.structure.building.function.cooking.PreparingDessert;
import org.mars_sim.msp.core.structure.building.function.farming.Farming;
import org.mars_sim.msp.core.structure.building.function.farming.Fishery;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.tool.settlement.SettlementMapPanel;
import org.mars_sim.msp.ui.swing.unit_window.UnitWindow;
import org.mars_sim.msp.ui.swing.unit_window.structure.building.food.BuildingPanelCooking;
import org.mars_sim.msp.ui.swing.unit_window.structure.building.food.BuildingPanelFoodProduction;
import org.mars_sim.msp.ui.swing.unit_window.structure.building.food.BuildingPanelPreparingDessert;


/**
 * The BuildingPanel class is a panel representing a settlement building.
 */
@SuppressWarnings("serial")
public class BuildingPanel extends JPanel {

	private static final Logger logger = Logger.getLogger(BuildingPanel.class.getName());

	public static final int WIDTH = UnitWindow.WIDTH - 170;
	public static final int HEIGHT = UnitWindow.HEIGHT - 190;
	
	/** The name of the panel. */
	private String panelName;

	private String newName;

	/** The function panels. */
	private List<BuildingFunctionPanel> functionPanels;

	private JLabel buildingNameLabel;

	/** The building this panel is for. */
	private Building building;

	private MainDesktopPane desktop;

	/**
	 * Constructor 1
	 *
	 * @param panelName the name of the panel.
	 * @param building  the building this panel is for.
	 * @param desktop   the main desktop.
	 * @throws MalformedURLException
	 */
	public BuildingPanel(String panelName, Building building, MainDesktopPane desktop) {
		super();

		// Initialize data members
		this.panelName = panelName;
		this.building = building;
		this.desktop = desktop;

		init();
	}

	/**
	 * Constructor 2
	 *
	 * @param isTranslucent
	 * @param panelName     the name of the panel.
	 * @param building      the building this panel is for.
	 * @param desktop       the main desktop.
	 */
	public BuildingPanel(boolean isTranslucent, String panelName, Building building, MainDesktopPane desktop) {

		// Initialize data members
		this.panelName = panelName;
		this.building = building;
		this.desktop = desktop;

		init();
	}

	/**
	 * Initializes the BuildingPanel
	 */
	public void init() {

		this.functionPanels = new ArrayList<BuildingFunctionPanel>();

		setLayout(new BorderLayout(0, 0));

		setMaximumSize(new Dimension(WIDTH, HEIGHT));
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
	
		JPanel topPanel = new JPanel(new BorderLayout(0, 0));
//		topPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		add(topPanel, BorderLayout.NORTH);

		// Add renameBtn for renaming a building
		JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton renameBtn = new JButton(Msg.getString("BuildingPanel.renameBuilding.renameButton")); //$NON-NLS-1$
		renameBtn.setPreferredSize(new Dimension(70, 20));
		renameBtn.setFont(new Font("Serif", Font.PLAIN, 9));
		// renameBtn.setBackground(Color.GRAY);
		renameBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				// if rename is done successfully, then update the building name
				renameBuilding();
				buildingNameLabel.setText(newName);
			}
		});
		btnPanel.add(renameBtn);
		topPanel.add(btnPanel, BorderLayout.NORTH);
		
		// Prepare function list panel.
		JPanel functionListPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		functionListPanel.setLayout(new BoxLayout(functionListPanel, BoxLayout.Y_AXIS));
//		functionListPanel.setPreferredSize(new Dimension(PopUpUnitMenu.WIDTH - 80, PopUpUnitMenu.HEIGHT)); // This width is very important
//		add(functionListPanel, BorderLayout.CENTER);
		
		// Prepare function scroll panel.
		JScrollPane scrollPanel = new JScrollPane();
		scrollPanel.setViewportView(functionListPanel);
		// CustomScroll scrollPanel = new CustomScroll(functionListPanel);
		scrollPanel.setPreferredSize(new Dimension(WIDTH, HEIGHT));//UnitWindow.HEIGHT - 300));
		scrollPanel.getVerticalScrollBar().setUnitIncrement(20);
		add(scrollPanel, BorderLayout.SOUTH);

		// Add SVG Image loading for the building
		Dimension dim = new Dimension(110, 110);
		// GraphicsNode node = SVGMapUtil.getSVGGraphicsNode("building", buildingType);
		Settlement settlement = building.getSettlement();
		// Conclusion: this panel is called only once per opening the unit window
		// session.
		SettlementMapPanel mapPanel = new SettlementMapPanel(settlement, building);
//		mapPanel.setPreferredSize(dim);
//		mapPanel.setMaximumSize(dim);
//		mapPanel.setMinimumSize(dim);

		JPanel svgPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
//		svgPanel.setMaximumSize(dim);
		svgPanel.setPreferredSize(dim);
		svgPanel.setBorder(new EmptyBorder(2, 2, 2, 2));
		svgPanel.add(mapPanel);

		Box box = new Box(BoxLayout.Y_AXIS);
		box.add(Box.createVerticalGlue());
		box.setAlignmentX(JComponent.CENTER_ALIGNMENT);
//		box.setAlignmentY(JComponent.CENTER_ALIGNMENT);
//		box.setBorder(BorderFactory.createLineBorder(java.awt.Color.GRAY, 1, true));
		box.add(svgPanel);
		box.add(Box.createVerticalGlue());
		functionListPanel.add(box);

		// Prepare the general info panel 
		BuildingPanelGeneral buildingPanelGeneral = new BuildingPanelGeneral(building, desktop);
		functionPanels.add(buildingPanelGeneral);
		functionListPanel.add(buildingPanelGeneral);
		
		// Prepare cooking panel if building has cooking.
		if (building.hasFunction(FunctionType.COOKING)) {
//			try {
			Cooking kitchen = building.getCooking();
			BuildingFunctionPanel cookingPanel = new BuildingPanelCooking(kitchen, desktop);
			functionPanels.add(cookingPanel);
			functionListPanel.add(cookingPanel);
			// if (isTranslucent)setPanelStyle(powerPanel);
//			}
//			catch (BuildingException e) {}
		}
		
		// Prepare cooking panel if building has cooking.
		if (building.hasFunction(FunctionType.COMPUTATION)) {
//			try {
			Computation computation = building.getComputation();
			BuildingFunctionPanel computationPanel = new BuildingPanelComputation(computation, desktop);
			functionPanels.add(computationPanel );
			functionListPanel.add(computationPanel );
			// if (isTranslucent)setPanelStyle(powerPanel);
//			}
//			catch (BuildingException e) {}
		}
		
		// Prepare cooking panel if building has cooking.
		if (building.hasFunction(FunctionType.EVA)) {
//			try {
			EVA eva = building.getEVA();
			BuildingFunctionPanel evaPanel = new BuildingPanelEVA(eva, desktop);
			functionPanels.add(evaPanel);
			functionListPanel.add(evaPanel);
			// if (isTranslucent)setPanelStyle(powerPanel);
//			}
//			catch (BuildingException e) {}
		}
		
//		Simulation.instance().getMasterClock().addClockListener(buildingPanelEVA);
		
		// Prepare farming panel if building has farming.
		if (building.hasFunction(FunctionType.FARMING)) {
//        	try {
			Farming farm = building.getFarming();
			BuildingFunctionPanel farmingPanel = new BuildingPanelFarming(farm, desktop);
			functionPanels.add(farmingPanel);
			functionListPanel.add(farmingPanel);
//        	}
//        	catch (BuildingException e) {}
		}

		// Prepare fishery panel
		if (building.hasFunction(FunctionType.FISHERY)) {
//			try {
			Fishery tank = (Fishery) building.getFunction(FunctionType.FISHERY);
			BuildingPanelFishery fishPanel = new BuildingPanelFishery(tank, desktop);
			functionPanels.add(fishPanel);
			functionListPanel.add(fishPanel);
			// if (isTranslucent)setPanelStyle(powerPanel);
//			}
//			catch (BuildingException e) {}
		}
		
		if (building.hasFunction(FunctionType.FOOD_PRODUCTION)) {
//        	try {
			FoodProduction foodFactory = building.getFoodProduction();
			BuildingFunctionPanel foodProductionPanel = new BuildingPanelFoodProduction(foodFactory, desktop);
			functionPanels.add(foodProductionPanel);
			functionListPanel.add(foodProductionPanel);
//        	}
//        	catch (BuildingException e) {}
		}


		
		// Prepare inhabitable panel if building has lifeSupport.
		if (building.hasFunction(FunctionType.LIFE_SUPPORT)) {
//        	try {
			LifeSupport lifeSupport = building.getLifeSupport();
			BuildingFunctionPanel inhabitablePanel = new BuildingPanelInhabitable(lifeSupport, desktop);
			functionPanels.add(inhabitablePanel);
			functionListPanel.add(inhabitablePanel);
//        	}
//        	catch (BuildingException e) {}
		}

		// Prepare living panel if building has living accommodations.
		if (building.hasFunction(FunctionType.LIVING_ACCOMMODATIONS)) {
//        	try {
			LivingAccommodations living = building.getLivingAccommodations();
			BuildingFunctionPanel livingPanel = new BuildingPanelLiving(living, desktop);
			functionPanels.add(livingPanel);
			functionListPanel.add(livingPanel);
//        	}
//        	catch (BuildingException e) {}
		}
		
		// Prepare manufacture panel if building has manufacturing.
		if (building.hasFunction(FunctionType.MANUFACTURE)) {
//        	try {
			Manufacture workshop = building.getManufacture();
			BuildingFunctionPanel manufacturePanel = new BuildingPanelManufacture(workshop, desktop);
			// manufacturePanel.setOpaque(false);
			// manufacturePanel.setBackground(new Color(0,0,0,128));
			functionPanels.add(manufacturePanel);
			functionListPanel.add(manufacturePanel);
//        	}
//        	catch (BuildingException e) {}
		}


		// Add preparing dessert function
		// Prepare dessert panel if building has preparing dessert function.
		if (building.hasFunction(FunctionType.PREPARING_DESSERT)) {
//			try {
			PreparingDessert kitchen = building.getPreparingDessert();
			BuildingFunctionPanel preparingDessertPanel = new BuildingPanelPreparingDessert(kitchen, desktop);
			functionPanels.add(preparingDessertPanel);
			functionListPanel.add(preparingDessertPanel);
			// if (isTranslucent) setPanelStyle(powerPanel);
//			}
//			catch (BuildingException e) {}
		}

		// Prepare medical care panel if building has medical care.
		if (building.hasFunction(FunctionType.MEDICAL_CARE)) {
//        	try {
			MedicalCare med = building.getMedical();
			BuildingFunctionPanel medicalCarePanel = new BuildingPanelMedicalCare(med, desktop);
			functionPanels.add(medicalCarePanel);
			functionListPanel.add(medicalCarePanel);
			// if (isTranslucent) setPanelStyle(powerPanel);
//        	}
//        	catch (BuildingException e) {}
		}

		// Prepare vehicle maintenance panel if building has vehicle maintenance.
		if (building.hasFunction(FunctionType.GROUND_VEHICLE_MAINTENANCE)) {
//			try {
			VehicleMaintenance garage = building.getVehicleMaintenance();
			BuildingFunctionPanel vehicleMaintenancePanel = new BuildingPanelVehicleMaintenance(garage, desktop);
			functionPanels.add(vehicleMaintenancePanel);
			functionListPanel.add(vehicleMaintenancePanel);
			// if (isTranslucent) setPanelStyle(powerPanel);
//			}
//			catch (BuildingException e) {}
		}

		// Prepare research panel if building has research.
		if (building.hasFunction(FunctionType.RESEARCH)) {
//        	try {
			Research lab = building.getResearch();
			BuildingFunctionPanel researchPanel = new BuildingPanelResearch(lab, desktop);
			functionPanels.add(researchPanel);
			functionListPanel.add(researchPanel);
			// if (isTranslucent) setPanelStyle(powerPanel);
//        	}
//        	catch (BuildingException e) {}
		}

		// Prepare Observation panel if building has Observatory.
		if (building.hasFunction(FunctionType.ASTRONOMICAL_OBSERVATION)) {
//        	try {
			AstronomicalObservation observation = building.getAstronomicalObservation();
			BuildingFunctionPanel observationPanel = new BuildingPanelAstronomicalObservation(observation, desktop);
			functionPanels.add(observationPanel);
			functionListPanel.add(observationPanel);
			// if (isTranslucent) setPanelStyle(observationPanel);
//        	}
//        	catch (BuildingException e) {}
		}

		// Prepare power panel.
		BuildingFunctionPanel powerPanel = new BuildingPanelPower(building, desktop);
		functionPanels.add(powerPanel);
		functionListPanel.add(powerPanel);
		// setPanelStyle(powerPanel);

		// Prepare power storage panel if building has power storage.
		if (building.hasFunction(FunctionType.POWER_STORAGE)) {
//            try {
			PowerStorage storage = building.getPowerStorage();
			BuildingFunctionPanel powerStoragePanel = new BuildingPanelPowerStorage(storage, desktop);
			functionPanels.add(powerStoragePanel);
			functionListPanel.add(powerStoragePanel);
			// if (isTranslucent) setPanelStyle(powerStoragePanel);
//            }
//            catch (BuildingException e) {}
		}

		//  Modify Heating Panel
		if (building.hasFunction(FunctionType.THERMAL_GENERATION)) {
//          try {
			ThermalGeneration heat = building.getThermalGeneration();
			BuildingFunctionPanel heatPanel = new BuildingPanelThermal(heat, desktop);
			functionPanels.add(heatPanel);
			functionListPanel.add(heatPanel);
			// if (isTranslucent) setPanelStyle(heatPanel);
//        }
//      catch (BuildingException e) {}
		}

		// Prepare resource processing panel if building has resource processes.
		if (building.hasFunction(FunctionType.RESOURCE_PROCESSING)) {
//        	try {
			ResourceProcessing processor = building.getResourceProcessing();
			BuildingFunctionPanel resourceProcessingPanel = new BuildingPanelResourceProcessing(processor, desktop);
			functionPanels.add(resourceProcessingPanel);
			functionListPanel.add(resourceProcessingPanel);
			// if (isTranslucent) setPanelStyle(resourceProcessingPanel);
//        	}
//        	catch (BuildingException e) {}
		}

		// Prepare storage process panel if building has storage function.
		if (building.hasFunction(FunctionType.STORAGE)) {
//            try {
			Storage storage = (Storage) building.getFunction(FunctionType.STORAGE);
			BuildingFunctionPanel storagePanel = new BuildingPanelStorage(storage, desktop);
			functionPanels.add(storagePanel);
			functionListPanel.add(storagePanel);
			// if (isTranslucent) setPanelStyle(storagePanel);
//            }
//            catch (BuildingException e) {}
		}

		// Prepare malfunctionable panel.
		BuildingFunctionPanel malfunctionPanel = new BuildingPanelMalfunctionable(building, desktop);
		functionPanels.add(malfunctionPanel);
		functionListPanel.add(malfunctionPanel);
		// setPanelStyle(malfunctionPanel);

		// Prepare maintenance panel.
		BuildingFunctionPanel maintenancePanel = new BuildingPanelMaintenance(building, desktop);
		functionPanels.add(maintenancePanel);
		functionListPanel.add(maintenancePanel);
		// setPanelStyle(maintenancePanel);

//        setPanelTranslucent();
		
		JScrollBar verticalScrollBar = scrollPanel.getVerticalScrollBar();
		verticalScrollBar.setValue(verticalScrollBar.getMinimum());
	}


	/**
	 * Ask for a new building name using JOptionPane
	 * 
	 * @return new name
	 */
	// Move askNameDialog() from TabPanelBuilding.java to here
	public String askNameDialog() {
		return JOptionPane.showInputDialog(desktop, Msg.getString("BuildingPanel.renameBuilding.dialogInput"),
				Msg.getString("BuildingPanel.renameBuilding.dialogTitle"), JOptionPane.QUESTION_MESSAGE);
	}

//	/**
//	 * Ask for a new building name using TextInputDialog in JavaFX/8
//	 * 
//	 * @return new name
//	 */
//	public String askNameFX(String oldName) {
//		String newName = null;
//		TextInputDialog dialog = new TextInputDialog(oldName);
//		dialog.setTitle(Msg.getString("BuildingPanel.renameBuilding.dialogTitle"));
//		dialog.setHeaderText(Msg.getString("BuildingPanel.renameBuilding.dialog.header"));
//		dialog.setContentText(Msg.getString("BuildingPanel.renameBuilding.dialog.content"));
//
//		Optional<String> result = dialog.showAndWait();
//		// result.ifPresent(name -> {});
//
//		if (result.isPresent()) {
//			logger.info("The old building name has been changed to: " + result.get());
//			newName = result.get();
//		}
//
//		return newName;
//	}

	/**
	 * Change and validate the new name of a Building
	 * 
	 * @return call Dialog popup
	 */
	private void renameBuilding() {

		String oldName = building.getNickName();
		newName = oldName;
		logger.info("Old name was " + oldName);

//		if (desktop.getMainScene() != null) {
//
//			Platform.runLater(() -> {
//
//				String newName = askNameFX(oldName);
//				if (!isBlank(newName)) { // newName != null && !newName.isEmpty() && newName with only whitespace(s)
//					building.setNickName(newName);
//					logger.info("New name is now " + newName);
//					buildingNameLabel.setText(building.getNickName());
//				} else {
//					Alert alert = new Alert(AlertType.ERROR, "Please use a valid name.");
//					alert.initOwner(desktop.getMainScene().getStage());
//					alert.showAndWait();
//				}
//			});
//
//		}
//		else {

			JDialog.setDefaultLookAndFeelDecorated(true);
			newName = askNameDialog();
			// Note: do not use if (newName.trim().equals(null), will throw
			// java.lang.NullPointerException
			if (newName == null || newName.trim() == "" || (newName.trim().length() == 0)) {
				newName = askNameDialog();
			} else {
				building.setNickName(newName);
				buildingNameLabel.setText(building.getNickName());
				logger.info("New name is now " + newName);
				// isRenamed = true;
			}
//		}

		// return isRenamed;
	}

	/**
	 * <p>
	 * Checks if a String is whitespace, empty ("") or null.
	 * </p>
	 *
	 * <pre>
	 * StringUtils.isBlank(null)      = true
	 * StringUtils.isBlank("")        = true
	 * StringUtils.isBlank(" ")       = true
	 * StringUtils.isBlank("bob")     = false
	 * StringUtils.isBlank("  bob  ") = false
	 * </pre>
	 *
	 * @param str the String to check, may be null
	 * @return <code>true</code> if the String is null, empty or whitespace
	 * @since 2.0
	 * @author commons.apache.org
	 */
	public static boolean isBlank(String str) {
		int strLen;
		if (str == null || (strLen = str.length()) == 0) {
			return true;
		}
		for (int i = 0; i < strLen; i++) {
			if ((Character.isWhitespace(str.charAt(i)) == false)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Gets the panel's name.
	 * 
	 * @return panel name
	 */
	public String getPanelName() {
		return panelName;
	}

	/**
	 * Gets the panel's building.
	 * 
	 * @return building
	 */
	public Building getBuilding() {
		return building;
	}

	/**
	 * Update this panel.
	 */
	public void update() {
		// Update each building function panel.
		for (BuildingFunctionPanel p : functionPanels)
//			if (p.isVisible())//&& p.isShowing())
				p.update();
	}

}
