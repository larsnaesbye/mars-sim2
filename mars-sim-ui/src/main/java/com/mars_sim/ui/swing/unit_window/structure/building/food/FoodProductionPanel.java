/*
 * Mars Simulation Project
 * FoodProductionPanel.java
 * @date 2022-07-11
 * @author Manny Kung
 */
package com.mars_sim.ui.swing.unit_window.structure.building.food;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Iterator;

import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;

import com.mars_sim.core.food.FoodProductionProcess;
import com.mars_sim.core.food.FoodProductionProcessInfo;
import com.mars_sim.core.process.ProcessItem;
import com.mars_sim.core.resource.ItemType;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MarsPanelBorder;

/**
 * A panel showing information about a foodProduction process.
 */
@SuppressWarnings("serial")
public class FoodProductionPanel extends JPanel {

	// Data members
	private FoodProductionProcess process;
	private BoundedRangeModel workBarModel;
	private BoundedRangeModel timeBarModel;

	/**
	 * Constructor
	 * 
	 * @param process            the foodProduction process.
	 * @param showBuilding       is the building name shown?
	 * @param processStringWidth the max string width to display for the process
	 *                           name.
	 */
	public FoodProductionPanel(FoodProductionProcess process, boolean showBuilding, int processStringWidth) {
		// Call JPanel constructor
		super();

		// Initialize data members.
		this.process = process;

		// Set layout
		if (showBuilding)
			setLayout(new GridLayout(4, 1, 0, 0));
		else
			setLayout(new GridLayout(3, 1, 0, 0));

		// Set border
		setBorder(new MarsPanelBorder());

		// Prepare name panel.
		JPanel namePane = new JPanel(new FlowLayout(FlowLayout.LEFT, 1, 0));
		add(namePane);

		// Prepare cancel button.
		JButton cancelButton = new JButton(ImageLoader.getIconByName("action/cancel"));
		cancelButton.setMargin(new Insets(0, 0, 0, 0));
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
//        		try {
				getFoodProductionProcess().getKitchen().endFoodProductionProcess(getFoodProductionProcess(), true);
//        		}
//        		catch (BuildingException e) {}
			}
		});
		cancelButton.setToolTipText("Cancel his Food Production Process");
		namePane.add(cancelButton);

		// Prepare name label.
		String name = process.getInfo().getName();
		if (name.length() > 0) {
			String firstLetter = name.substring(0, 1).toUpperCase();
			name = " " + firstLetter + name.substring(1);
		}
		if (name.length() > processStringWidth)
			name = name.substring(0, processStringWidth) + "...";
		// 2014-11-19 Capitalized process names
		JLabel nameLabel = new JLabel(name, SwingConstants.CENTER);
		namePane.add(nameLabel);

		if (showBuilding) {
			// Prepare building name label.
			// 2014-11-19 Changed from getName() to getName()
			String buildingName = process.getKitchen().getBuilding().getName();
			JLabel buildingNameLabel = new JLabel(buildingName, SwingConstants.CENTER);
			add(buildingNameLabel);
		}

		// Prepare work panel.
		JPanel workPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		add(workPane);

		// Prepare work label.
		JLabel workLabel = new JLabel("Work: ", SwingConstants.LEFT);
		workPane.add(workLabel);

		// Prepare work progress bar.
		JProgressBar workBar = new JProgressBar();
		workBarModel = workBar.getModel();
		workBar.setStringPainted(true);
		workPane.add(workBar);

		// Prepare time panel.
		JPanel timePane = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		add(timePane);

		// Prepare time label.
		JLabel timeLabel = new JLabel("Time: ", SwingConstants.LEFT);
		timePane.add(timeLabel);

		// Prepare time progress bar.
		JProgressBar timeBar = new JProgressBar();
		timeBarModel = timeBar.getModel();
		timeBar.setStringPainted(true);
		timePane.add(timeBar);

		// Update progress bars.
		update();

		// Add tooltip.
		setToolTipText(getToolTipString(process.getInfo(), process.getKitchen().getBuilding()));
	}

	/**
	 * Updates the panel's information.
	 */
	public void update() {
		// Update work progress bar.
		double workTimeRequired = process.getInfo().getWorkTimeRequired();
		double workTimeRemaining = process.getWorkTimeRemaining();
		int workProgress = 100;
		if (workTimeRequired > 0D)
			workProgress = (int) (100D * (workTimeRequired - workTimeRemaining) / workTimeRequired);
		workBarModel.setValue(workProgress);

		// Update time progress bar.
		double timeRequired = process.getInfo().getProcessTimeRequired();
		double timeRemaining = process.getProcessTimeRemaining();
		int timeProgress = 100;
		if (timeRequired > 0D)
			timeProgress = (int) (100D * (timeRequired - timeRemaining) / timeRequired);
		timeBarModel.setValue(timeProgress);
	}

	/**
	 * Gets the foodProduction process.
	 * 
	 * @return process
	 */
	public FoodProductionProcess getFoodProductionProcess() {
		return process;
	}

	/**
	 * Gets a tool tip string for a foodProduction process.
	 * 
	 * @param info     the foodProduction process info.
	 * @param building the foodProduction building (or null if none).
	 */
	// Update tooltip formatting
	public static String getToolTipString(FoodProductionProcessInfo info, Building building) {
		StringBuilder result = new StringBuilder("<html>");

		result.append("&emsp;&emsp;&emsp;&emsp;&nbsp;Process : ").append(info.getName())
				.append("<br>");
		// 2014-11-19 Changed from getName() to getName()

		result.append("&emsp;&emsp;&emsp;&nbsp;Labor Req : ").append(info.getWorkTimeRequired())
				.append(" millisols<br>");
		result.append("&emsp;&emsp;&emsp;&nbsp;&nbsp;Time Req : ").append(info.getProcessTimeRequired())
				.append(" millisols<br>");
		result.append("&emsp;&emsp;&emsp;Power Req : ").append(info.getPowerRequired()).append(" kW<br>");
		result.append("&emsp;&nbsp;Bldg Tech Req :&nbsp;Level ").append(info.getTechLevelRequired()).append("<br>");
		result.append("Cooking Skill Req :&nbsp;Level ").append(info.getSkillLevelRequired()).append("<br>");

		// Add process inputs.
		result.append("&emsp;&emsp;&emsp;&emsp;&emsp;&nbsp;Inputs : ");
		Iterator<ProcessItem> i = info.getInputList().iterator();
		int ii = 0;
		while (i.hasNext()) {
			ProcessItem item = i.next();
			// 2014-11-19 Capitalized process names
			if (ii == 0)
				result.append(getItemAmountString(item)).append(" ").append(item.getName())
						.append("<br>");
			else
				result.append("&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;")
						.append(getItemAmountString(item)).append(" ").append(item.getName())
						.append("<br>");
			ii++;
		}

		// Add process outputs.
		result.append("&emsp;&emsp;&emsp;&emsp;&nbsp;&nbsp;Outputs : ");
		Iterator<ProcessItem> j = info.getOutputList().iterator();
		int jj = 0;
		while (j.hasNext()) {
			ProcessItem item = j.next();
			// 2014-11-19 Capitalized process names
			if (jj == 0)
				result.append(getItemAmountString(item)).append(" ").append(item.getName())
						.append("<br>");
			else
				result.append("&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;")
						.append(getItemAmountString(item)).append(" ").append(item.getName())
						.append("<br>");
			jj++;
		}

		result.append("</html>");

		return result.toString();
	}

	/**
	 * Gets a string representing an foodProduction process item amount.
	 * 
	 * @param item the foodProduction process item.
	 * @return amount string.
	 */
	private static String getItemAmountString(ProcessItem item) {
		if (ItemType.AMOUNT_RESOURCE.equals(item.getType())) {
			return item.getAmount() + " kg";
		} else
			return Integer.toString((int) item.getAmount());
	}
}
