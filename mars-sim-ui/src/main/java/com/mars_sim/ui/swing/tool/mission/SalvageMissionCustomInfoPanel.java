/*
 * Mars Simulation Project
 * SalvageMissionCustomInfoPanel.java
 * @date 2021-09-20
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.tool.mission;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Iterator;

import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.mars_sim.core.person.ai.mission.Mission;
import com.mars_sim.core.person.ai.mission.MissionEvent;
import com.mars_sim.core.person.ai.mission.SalvageMission;
import com.mars_sim.core.resource.ItemResourceUtil;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.construction.ConstructionEvent;
import com.mars_sim.core.structure.construction.ConstructionListener;
import com.mars_sim.core.structure.construction.ConstructionSite;
import com.mars_sim.core.structure.construction.ConstructionStage;
import com.mars_sim.core.structure.construction.ConstructionStageInfo;
import com.mars_sim.core.structure.construction.ConstructionVehicleType;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.StyleManager;


/**
 * A panel for displaying salvage custom mission information.
 */
@SuppressWarnings("serial")
public class SalvageMissionCustomInfoPanel
extends MissionCustomInfoPanel 
implements ConstructionListener {

	// Data members.
	private MainDesktopPane desktop;
	private SalvageMission mission;
	private ConstructionSite site;
	private JLabel stageLabel;
	private BoundedRangeModel progressBarModel;
	private JButton settlementButton;

	/**
	 * Constructor.
	 * @param desktop the main desktop panel.
	 */
	public SalvageMissionCustomInfoPanel(MainDesktopPane desktop) {
		// Use MissionCustomInfoPanel constructor.
		super();

		// Initialize data members.
		this.desktop = desktop;

		// Set layout.
		setLayout(new BorderLayout());

		JPanel contentsPanel = new JPanel(new GridLayout(4, 1));
		add(contentsPanel, BorderLayout.NORTH);

		JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		contentsPanel.add(titlePanel);

		JLabel titleLabel = new JLabel("Salvage Construction Site");
		titlePanel.add(titleLabel);

		JPanel settlementPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		contentsPanel.add(settlementPanel);

		JLabel settlementLabel = new JLabel("Settlement: ");
		settlementPanel.add(settlementLabel);

		settlementButton = new JButton("   ");
		settlementPanel.add(settlementButton);
		settlementButton.addActionListener(e -> {
				if (mission != null) {
					Settlement settlement = mission.getAssociatedSettlement();
					if (settlement != null) getDesktop().showDetails(settlement);
				}
		});

		JPanel stagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		contentsPanel.add(stagePanel);

		stageLabel = new JLabel("Stage:");
		stagePanel.add(stageLabel);

		JPanel progressBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		contentsPanel.add(progressBarPanel);

		JProgressBar progressBar = new JProgressBar();
		progressBarModel = progressBar.getModel();
		progressBar.setStringPainted(true);
		progressBarPanel.add(progressBar);

		// Add tooltip.
		setToolTipText(getToolTipString());
	}

	@Override
	public void updateMission(Mission mission) {
		// Remove as construction listener if necessary.
		if (site != null) site.removeConstructionListener(this);

		if (mission instanceof SalvageMission) {
			this.mission = (SalvageMission) mission;
			site = this.mission.getConstructionSite();
			if (site != null) site.addConstructionListener(this);

			settlementButton.setText(mission.getAssociatedSettlement().getName());
			stageLabel.setText(getStageString());
			updateProgressBar();

			// Update the tool tip string.
			setToolTipText(getToolTipString());
		}
	}

	@Override
	public void updateMissionEvent(MissionEvent e) {
		stageLabel.setText(getStageString());
	}

	@Override
	public void constructionUpdate(ConstructionEvent event) {
		if (ConstructionStage.ADD_SALVAGE_WORK_EVENT.equals(event.getType())) {
			updateProgressBar();

			// Update the tool tip string.
			setToolTipText(getToolTipString());
		}
	}

	/**
	 * Gets the stage label string.
	 * @return stage string.
	 */
	private String getStageString() {
		StringBuilder stageString = new StringBuilder("Stage: ");
		if (mission != null) {
			ConstructionStage stage = mission.getConstructionStage();
			if (stage != null) stageString.append(stage.getInfo().getName());
		}

		return stageString.toString();
	}

	/**
	 * Updates the progress bar.
	 */
	private void updateProgressBar() {
		int workProgress = 0;
		if (mission != null) {
			ConstructionStage stage = mission.getConstructionStage();
			if (stage != null) {
				double completedWork = stage.getCompletedWorkTime();
				double requiredWork = stage.getRequiredWorkTime();
				if (requiredWork > 0D) workProgress = (int) (100D * completedWork / requiredWork);
			}
		}
		progressBarModel.setValue(workProgress);
	}

	/**
	 * Gets the main desktop.
	 * @return desktop.
	 */
	private MainDesktopPane getDesktop() {
		return desktop;
	}

	/**
	 * Gets a tool tip string for the panel.
	 */
	private String getToolTipString() {
		StringBuilder result = new StringBuilder("<html>");

		ConstructionStage stage = null;
		if (site != null) stage = site.getCurrentConstructionStage();
		if (stage != null) {
			ConstructionStageInfo info = stage.getInfo();
			result.append("Status: salvaging ").append(info.getName()).append("<br>");
			result.append("Stage Type: ").append(info.getType()).append("<br>");
			if (stage.isSalvaging()) result.append("Work Type: salvage<br>");
			else result.append("Work Type: Construction<br>");
			String requiredWorkTime = StyleManager.DECIMAL_PLACES1.format(stage.getRequiredWorkTime() / 1000D);
			result.append("Work Time Required: ").append(requiredWorkTime).append(" Sols<br>");
			String completedWorkTime = StyleManager.DECIMAL_PLACES1.format(stage.getCompletedWorkTime() / 1000D);
			result.append("Work Time Completed: ").append(completedWorkTime).append(" Sols<br>");
			result.append("Architect Construction Skill Required: ").append(info.getArchitectConstructionSkill()).append("<br>");

			// Add construction parts.
			if (info.getParts().size() > 0) {
				result.append("<br>Salvagable Parts:<br>");
				Iterator<Integer> j = info.getParts().keySet().iterator();
				while (j.hasNext()) {
					Integer id = j.next();
					int number = info.getParts().get(id);
					result.append("&nbsp;&nbsp;").append(ItemResourceUtil.findItemResource(id).getName()).append(": ").append(number).append("<br>");
				}
			}

			// Add construction vehicles.
			if (info.getVehicles().size() > 0) {
				result.append("<br>Salvage Vehicles:<br>");
				Iterator<ConstructionVehicleType> k = info.getVehicles().iterator();
				while (k.hasNext()) {
					ConstructionVehicleType vehicle = k.next();
					result.append("&nbsp;&nbsp;Vehicle Type: ").append(vehicle.getVehicleType()).append("<br>");
					result.append("&nbsp;&nbsp;Attachment Parts:<br>");
					Iterator<Integer> l = vehicle.getAttachmentParts().iterator();
					while (l.hasNext()) {
						result.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(ItemResourceUtil.findItemResource(l.next()).getName()).append("<br>");
					}
				}
			}
		}

		result.append("</html>");

		return result.toString();
	}
}
