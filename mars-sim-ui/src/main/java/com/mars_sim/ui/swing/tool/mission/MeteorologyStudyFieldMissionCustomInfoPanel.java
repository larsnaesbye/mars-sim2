/*
 * Mars Simulation Project
 * MeteorologyStudyFieldMissionCustomInfoPanel.java
 * @date 2021-09-20
 * @author Manny Kung
 */
package com.mars_sim.ui.swing.tool.mission;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.mission.MeteorologyFieldStudy;
import com.mars_sim.core.person.ai.mission.Mission;
import com.mars_sim.core.person.ai.mission.MissionEvent;
import com.mars_sim.core.science.ScientificStudy;
import com.mars_sim.core.science.ScientificStudyEvent;
import com.mars_sim.core.science.ScientificStudyListener;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.tool.science.ScienceWindow;


/**
 * A panel for displaying meteorology study field mission information.
 */
@SuppressWarnings("serial")
public class MeteorologyStudyFieldMissionCustomInfoPanel extends MissionCustomInfoPanel
		implements ScientificStudyListener {

	// Data members.
	private MainDesktopPane desktop;
	private ScientificStudy study;
	private MeteorologyFieldStudy meteorologyMission;
	private JLabel studyNameLabel;
	private JLabel researcherNameLabel;
	private JProgressBar studyResearchBar;

	/**
	 * Constructor.
	 * 
	 * @param desktop the main desktop pane.
	 */
	public MeteorologyStudyFieldMissionCustomInfoPanel(MainDesktopPane desktop) {
		// Use MissionCustomInfoPanel constructor.
		super();

		// Initialize data members.
		this.desktop = desktop;

		// Set layout.
		setLayout(new BorderLayout());

		// Create content panel.
		JPanel contentPanel = new JPanel(new GridLayout(3, 1));
		add(contentPanel, BorderLayout.NORTH);

		// Create study panel.
		JPanel studyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		contentPanel.add(studyPanel);

		// Create science tool button.
		JButton scienceToolButton = new JButton(ImageLoader.getIconByName(ScienceWindow.ICON)); //$NON-NLS-1$
		scienceToolButton.setMargin(new Insets(1, 1, 1, 1));
		scienceToolButton
				.setToolTipText(Msg.getString("MeteorologyStudyFieldMissionCustomInfoPanel.tooltip.openInScienceTool")); //$NON-NLS-1$
		scienceToolButton.addActionListener(e -> displayStudyInScienceTool());
		studyPanel.add(scienceToolButton);

		// Create study title label.
		JLabel studyTitleLabel = new JLabel(
				Msg.getString("MeteorologyStudyFieldMissionCustomInfoPanel.meteorologyFieldStudy")); //$NON-NLS-1$
		studyPanel.add(studyTitleLabel);

		// Create study name label.
		studyNameLabel = new JLabel(""); //$NON-NLS-1$
		studyPanel.add(studyNameLabel);

		// Create researcher panel.
		JPanel researcherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		contentPanel.add(researcherPanel);

		// Create researcher title label.
		JLabel researcherTitleLabel = new JLabel(
				Msg.getString("MeteorologyStudyFieldMissionCustomInfoPanel.leadResearcher")); //$NON-NLS-1$
		researcherPanel.add(researcherTitleLabel);

		// Create researcher name label.
		researcherNameLabel = new JLabel(""); //$NON-NLS-1$
		researcherPanel.add(researcherNameLabel);

		// Create study research panel.
		JPanel studyResearchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		contentPanel.add(studyResearchPanel);

		// Create study research title label.
		JLabel studyResearchTitleLabel = new JLabel(
				Msg.getString("MeteorologyStudyFieldMissionCustomInfoPanel.researchCompletion")); //$NON-NLS-1$
		studyResearchPanel.add(studyResearchTitleLabel);

		// Create study research progress bar.
		studyResearchBar = new JProgressBar(0, 100);
		studyResearchBar.setStringPainted(true);
		studyResearchPanel.add(studyResearchBar);
	}

	@Override
	public void updateMission(Mission mission) {
		if (mission instanceof MeteorologyFieldStudy) {
			meteorologyMission = (MeteorologyFieldStudy) mission;

			// Remove as scientific study listener.
			if (study != null) {
				study.removeScientificStudyListener(this);
			}

			// Add as scientific study listener to new study.
			study = meteorologyMission.getScientificStudy();
			if (study != null) {
				study.addScientificStudyListener(this);

				// Update study name.
				studyNameLabel.setText(study.toString());

				// Update lead researcher for mission.
				researcherNameLabel.setText(meteorologyMission.getLeadResearcher().getName());

				// Update study research bar.
				updateStudyResearchBar(study, meteorologyMission.getLeadResearcher());
			}
		}
	}

	@Override
	public void updateMissionEvent(MissionEvent e) {
		// Do nothing
	}

	@Override
	public void scientificStudyUpdate(ScientificStudyEvent event) {
		ScientificStudy study = event.getStudy();
		Person leadResearcher = meteorologyMission.getLeadResearcher();

		if (ScientificStudyEvent.PRIMARY_RESEARCH_WORK_EVENT.equals(event.getType())
				|| ScientificStudyEvent.COLLABORATION_RESEARCH_WORK_EVENT.equals(event.getType())) {
			if (leadResearcher.equals(event.getResearcher())) {
				updateStudyResearchBar(study, leadResearcher);
			}
		}
	}

	/**
	 * Checks if a researcher is the primary researcher on a scientific study.
	 * 
	 * @param researcher the researcher.
	 * @param study      the scientific study.
	 * @return true if primary researcher.
	 */
	private boolean isStudyPrimaryResearcher(Person researcher, ScientificStudy study) {
		boolean result = researcher.equals(study.getPrimaryResearcher());

        return result;
	}

	/**
	 * Checks if a researcher is a collaborative researcher on a scientific study.
	 * 
	 * @param researcher the researcher.
	 * @param study      the scientific study.
	 * @return true if collaborative researcher.
	 */
	private boolean isStudyCollaborativeResearcher(Person researcher, ScientificStudy study) {
		boolean result = study.getCollaborativeResearchers().contains(researcher);

        return result;
	}

	/**
	 * Updates the research completion progress bar.
	 * 
	 * @param study          the
	 * @param leadResearcher
	 */
	private void updateStudyResearchBar(ScientificStudy study, Person leadResearcher) {
		if (study != null) {
			double requiredResearchWork = 0D;
			double completedResearchWork = 0D;

			if (isStudyPrimaryResearcher(leadResearcher, study)) {
				requiredResearchWork = study.getTotalPrimaryResearchWorkTimeRequired();
				completedResearchWork = study.getPrimaryResearchWorkTimeCompleted();
			} else if (isStudyCollaborativeResearcher(leadResearcher, study)) {
				requiredResearchWork = study.getTotalCollaborativeResearchWorkTimeRequired();
				completedResearchWork = study.getCollaborativeResearchWorkTimeCompleted(leadResearcher);
			} else {
				return;
			}

			int percentResearchCompleted = (int) (completedResearchWork / requiredResearchWork * 100D);
			studyResearchBar.setValue(percentResearchCompleted);
		}
	}

	/**
	 * Display the scientific study in the science tool window.
	 */
	private void displayStudyInScienceTool() {
		if (study != null) {
			ScienceWindow scienceToolWindow = (ScienceWindow) desktop.openToolWindow(ScienceWindow.NAME);
			scienceToolWindow.setScientificStudy(study);
		}
	}
}
