/*
 * Mars Simulation Project
 * BuildingPanelFoodProduction.java
 * @date 2022-07-26
 * @author Manny Kung
 */

package com.mars_sim.ui.swing.unit_window.structure.building.food;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.mars_sim.core.food.FoodProductionProcess;
import com.mars_sim.core.food.FoodProductionProcessInfo;
import com.mars_sim.core.food.FoodProductionUtil;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.SkillManager;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.function.FoodProduction;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.unit_window.structure.building.BuildingFunctionPanel;
import com.mars_sim.ui.swing.utils.AttributePanel;

/**
 * A building panel displaying the foodProduction building function.
 */
@SuppressWarnings("serial")
public class BuildingPanelFoodProduction extends BuildingFunctionPanel {

	/** default logger. */
	private static final Logger logger = Logger.getLogger(BuildingPanelFoodProduction.class.getName());

	private static final String FOOD_ICON = "food";
	
	private static int processStringWidth = 60;
	
	/** The foodProduction building. */
	private FoodProduction foodFactory;
	/** Panel for displaying process panels. */
	private JPanel processListPane;
	private JScrollPane scrollPanel;
	/** List of foodProduction processes in building. */
	private List<FoodProductionProcess> processCache;
	/** Process selector. */
	private JComboBox<FoodProductionProcessInfo> processComboBox;
	/** List of available processes. */
	private Vector<FoodProductionProcessInfo> processComboBoxCache;

	/** Process selection button. */
	private JButton newProcessButton;

	/**
	 * Constructor.
	 * 
	 * @param foodFactory the manufacturing building function.
	 * @param desktop     the main desktop.
	 */
	public BuildingPanelFoodProduction(FoodProduction foodFactory, MainDesktopPane desktop) {
		// Use BuildingFunctionPanel constructor.
		super(
			Msg.getString("BuildingPanelFoodProduction.title"), //$NON-NLS-1$
			ImageLoader.getIconByName(FOOD_ICON),
			foodFactory.getBuilding(),
			desktop
		);
		
		// Initialize data model.
		this.foodFactory = foodFactory;
	}
	
	/**
	 * Build the UI elements
	 */
	@Override
	protected void buildUI(JPanel center) {

		// Prepare label panel
		AttributePanel labelPanel = new AttributePanel(2);

		center.add(labelPanel, BorderLayout.NORTH);

		// Prepare tech level label
		labelPanel.addTextField("Tech Level", Integer.toString(foodFactory.getTechLevel()), null);

		// Prepare processCapacity label
		labelPanel.addTextField("Process Capacity", Integer.toString(foodFactory.getMaxProcesses()), null);


		// Create scroll pane for food production processes
		scrollPanel = new JScrollPane();
		scrollPanel.setPreferredSize(new Dimension(170, 90));
		center.add(scrollPanel, BorderLayout.CENTER);
		addBorder(scrollPanel, "Preparation");
		
		// Create process list main panel
		JPanel processListMainPane = new JPanel(new BorderLayout(0, 0));
		scrollPanel.setViewportView(processListMainPane);

		// Create process list panel
		processListPane = new JPanel(new FlowLayout(10, 10 ,10));
		processListPane.setLayout(new BoxLayout(processListPane, BoxLayout.Y_AXIS));
		processListMainPane.add(processListPane, BorderLayout.NORTH);

		List<FoodProductionProcess> list = foodFactory.getProcesses();

		// Create process panels
		processCache = new ArrayList<>(list);
		Iterator<FoodProductionProcess> i = processCache.iterator();
		while (i.hasNext())
			processListPane.add(new FoodProductionPanel(i.next(), false, processStringWidth));

		// Create interaction panel.
		JPanel interactionPanel = new JPanel(new GridLayout(2, 1, 10, 10));
		addBorder(interactionPanel, "Add Food");
		center.add(interactionPanel, BorderLayout.SOUTH);

		// Create new foodProduction process selection.
		processComboBoxCache = getAvailableProcesses();
		processComboBox = new JComboBox<>(processComboBoxCache);

		processComboBox.setRenderer(new FoodProductionSelectionListCellRenderer());
		processComboBox.setToolTipText("Select An Available Food Production Process");
		interactionPanel.add(processComboBox);

		if (processComboBoxCache.size() > 0) 
			processComboBox.setSelectedIndex(0);

		// Create new process button.
		JPanel btnPanel = new JPanel(new FlowLayout());
		newProcessButton = new JButton("Create New Process");
		btnPanel.add(newProcessButton);

		newProcessButton.setEnabled(processComboBox.getItemCount() > 0);
		newProcessButton.setToolTipText("Create a New Food Production Process or Salvage a Process");
		newProcessButton.addActionListener(e -> {
				try {
					Object selectedItem = processComboBox.getSelectedItem();
					if (selectedItem != null
						&& selectedItem instanceof FoodProductionProcessInfo) {
						FoodProductionProcessInfo selectedProcess = (FoodProductionProcessInfo) selectedItem;
						if (FoodProductionUtil.canProcessBeStarted(selectedProcess, getFoodFactory())) {
							getFoodFactory()
									.addProcess(new FoodProductionProcess(selectedProcess, getFoodFactory()));
							update();
						}
					}
				} catch (Exception ex) {
					logger.log(Level.SEVERE, "new process button", ex);
				}
		});
		interactionPanel.add(btnPanel);
	}

	@Override
	public void update() {

		// Update processes and salvage processes if necessary.
		List<FoodProductionProcess> processes = foodFactory.getProcesses();
		if (!processCache.equals(processes)) {

			// Add process panels for new processes.
			Iterator<FoodProductionProcess> i = processes.iterator();
			while (i.hasNext()) {
				FoodProductionProcess process = i.next();
				if (!processCache.contains(process))
					processListPane.add(new FoodProductionPanel(process, false, processStringWidth));
			}

			// Remove process panels for old processes.
			Iterator<FoodProductionProcess> j = processCache.iterator();
			while (j.hasNext()) {
				FoodProductionProcess process = j.next();
				if (!processes.contains(process)) {
					FoodProductionPanel panel = getFoodProductionPanel(process);
					if (panel != null)
						processListPane.remove(panel);
				}
			}

			// Update processCache
			processCache.clear();
			processCache.addAll(processes);

			scrollPanel.validate();
		}

		// Update all process panels.
		Iterator<FoodProductionProcess> i = processes.iterator();
		while (i.hasNext()) {
			FoodProductionPanel panel = getFoodProductionPanel(i.next());
			if (panel != null)
				panel.update();

		}
		// Update process selection list.
		Vector<FoodProductionProcessInfo> newProcesses = getAvailableProcesses();

		if (!newProcesses.equals(processComboBoxCache)) {

			processComboBoxCache = newProcesses;

			Object currentSelection = processComboBox.getSelectedItem();

			processComboBox.removeAllItems();

			Iterator<FoodProductionProcessInfo> k = processComboBoxCache.iterator();

			while (k.hasNext()) processComboBox.addItem(k.next());

			if (currentSelection != null) {
				if (processComboBoxCache.contains(currentSelection))
					processComboBox.setSelectedItem(currentSelection);
			} else if (processComboBoxCache.size() > 0)
				processComboBox.setSelectedIndex(0);
		}

		// Update new process button.
		newProcessButton.setEnabled(processComboBox.getItemCount() > 0);
	}

	/**
	 * Gets the panel for a foodProduction process.
	 * 
	 * @param process the foodProduction process.
	 * @return foodProduction panel or null if none.
	 */
	private FoodProductionPanel getFoodProductionPanel(FoodProductionProcess process) {
		FoodProductionPanel result = null;

		for (int x = 0; x < processListPane.getComponentCount(); x++) {
			Component component = processListPane.getComponent(x);
			if (component instanceof FoodProductionPanel) {
				FoodProductionPanel panel = (FoodProductionPanel) component;
				if (panel.getFoodProductionProcess().equals(process))
					result = panel;

			}
		}

		return result;
	}

	/**
	 * Gets all manufacturing processes available at the foodFactory.
	 * 
	 * @return vector of processes.
	 */
	private Vector<FoodProductionProcessInfo> getAvailableProcesses() {
		Vector<FoodProductionProcessInfo> result = new Vector<>();

		if (foodFactory.getProcesses().size() < foodFactory.getNumPrintersInUse()) {

			// Determine highest materials science skill level at settlement.
			Settlement settlement = foodFactory.getBuilding().getSettlement();
			int highestSkillLevel = 0;
			Iterator<Person> i = settlement.getAllAssociatedPeople().iterator();
			while (i.hasNext()) {
				Person tempPerson = i.next();
				SkillManager skillManager = tempPerson.getSkillManager();
				int skill = skillManager.getSkillLevel(SkillType.COOKING);
				if (skill > highestSkillLevel) {
					highestSkillLevel = skill;
				}
			}

			Iterator<Robot> k = settlement.getAllAssociatedRobots().iterator();
			while (k.hasNext()) {
				Robot r = k.next();
				SkillManager skillManager = r.getSkillManager();
				int skill = skillManager.getSkillLevel(SkillType.COOKING);
				if (skill > highestSkillLevel) {
					highestSkillLevel = skill;
				}
			}
			
			try {
				Iterator<FoodProductionProcessInfo> j = Collections.unmodifiableList(FoodProductionUtil
						.getFoodProductionProcessesForTechSkillLevel(foodFactory.getTechLevel(), highestSkillLevel))
						.iterator();
				while (j.hasNext()) {
					FoodProductionProcessInfo process = j.next();
					if (FoodProductionUtil.canProcessBeStarted(process, foodFactory))
						result.add(process);
				}
			} catch (Exception e) {
			}
		}

		// Enable Collections.sorts by implementing Comparable<>
		Collections.sort(result);
		return result;
	}

	/**
	 * Gets the foodFactory for this panel.
	 * 
	 * @return foodFactory
	 */
	private FoodProduction getFoodFactory() {
		return foodFactory;
	}

	/**
	 * Inner class for the foodProduction selection list cell renderer.
	 */
	private static class FoodProductionSelectionListCellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof FoodProductionProcessInfo) {
				FoodProductionProcessInfo info = (FoodProductionProcessInfo) value;
				if (info != null) {
					// Capitalize processName
					String processName = info.getName();
					if (processName.length() > processStringWidth)
						processName = processName.substring(0, processStringWidth) + "...";
					((JLabel) result).setText(processName);
					((JComponent) result).setToolTipText(FoodProductionPanel.getToolTipString(info, null));
				}
			}

			return result;
		}
	}
}
