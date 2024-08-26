/*
 * Mars Simulation Project
 * TabPanelScience.java
 * @date 2022-07-09
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.mars_sim.core.science.ScienceType;
import com.mars_sim.core.science.ScientificStudy;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.tools.Msg;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.NumberCellRenderer;
import com.mars_sim.ui.swing.StyleManager;
import com.mars_sim.ui.swing.tool.science.ScienceWindow;
import com.mars_sim.ui.swing.unit_window.TabPanel;

/**
 * A tab panel displaying a settlement's scientific studies and achievements.
 */
@SuppressWarnings("serial")
public class TabPanelScience
extends TabPanel {

	private static final String SCIENCE_ICON = "science";

	// Data members
	/** The Settlement instance. */
	private Settlement settlement;
	
	private JButton scienceToolButton;
	private JLabel totalAchievementLabel;

	private JTable achievementTable;
	private JTable studyTable;

	private StudyTableModel studyTableModel;
	private AchievementTableModel achievementTableModel;
	
	/**
	 * Constructor.
	 * @param settlement the settlement.
	 * @param desktop the main desktop.
	 */
	public TabPanelScience(Settlement settlement, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelScience.title"), //$NON-NLS-1$
			ImageLoader.getIconByName(SCIENCE_ICON),
			Msg.getString("TabPanelScience.tooltip"), //$NON-NLS-1$
			settlement, desktop
		);

		this.settlement = settlement;
	}
	
	@Override
	protected void buildUI(JPanel content){

		// Create the main panel.
		JPanel mainPane = new JPanel(new GridLayout(2, 1, 0, 0));
		content.add(mainPane);

		// Create the studies panel.
		JPanel studiesPane = new JPanel(new BorderLayout(5, 5));
		mainPane.add(studiesPane);

		// Create the studies label.
		JLabel studiesLabel = new JLabel(Msg.getString("TabPanelScience.scientificStudies"), SwingConstants.CENTER); //$NON-NLS-1$
		studiesPane.add(studiesLabel, BorderLayout.NORTH);

		// Create the study scroll panel.
		JScrollPane studyScrollPane = new JScrollPane();
		studyScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		studiesPane.add(studyScrollPane, BorderLayout.CENTER);

		// Create the study table.
		studyTableModel = new StudyTableModel(settlement);
		studyTable = new JTable(studyTableModel);
		
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setHorizontalAlignment(SwingConstants.CENTER);
		TableColumnModel studyColumns = studyTable.getColumnModel();
		studyColumns.getColumn(0).setCellRenderer(renderer);
		studyColumns.getColumn(1).setCellRenderer(renderer);
		studyColumns.getColumn(2).setCellRenderer(renderer);
		studyColumns.getColumn(3).setCellRenderer(renderer);
		studyColumns.getColumn(4).setCellRenderer(renderer);
		
		studyColumns.getColumn(0).setPreferredWidth(5);
		studyColumns.getColumn(1).setPreferredWidth(40);
		studyColumns.getColumn(2).setPreferredWidth(5);
		studyColumns.getColumn(3).setPreferredWidth(80);
		studyColumns.getColumn(4).setPreferredWidth(80);
		
		studyTable.setPreferredScrollableViewportSize(new Dimension(225, -1));
		studyTable.setRowSelectionAllowed(true);
		studyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		studyTable.getSelectionModel().addListSelectionListener(event -> {
			if (event.getValueIsAdjusting() && (studyTable.getSelectedRow() >= 0)) {
					setEnabledScienceToolButton(true);
			}
		});
		studyScrollPane.setViewportView(studyTable);

		// Added sorting
		studyTable.setAutoCreateRowSorter(true);

		// Create the button panel.
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		studiesPane.add(buttonPane, BorderLayout.SOUTH);

		// Create the science tool button.
		scienceToolButton = new JButton(ImageLoader.getIconByName(ScienceWindow.ICON)); //$NON-NLS-1$
		scienceToolButton.setEnabled(false);
		scienceToolButton.setMargin(new Insets(1, 1, 1, 1));
		scienceToolButton.setToolTipText(Msg.getString("TabPanelScience.button.science")); //$NON-NLS-1$
		scienceToolButton.addActionListener(arg0 -> displayStudyInScienceTool());
		buttonPane.add(scienceToolButton);

		// Create the achievement panel.
		JPanel achievementPane = new JPanel(new BorderLayout());
		mainPane.add(achievementPane);

		// Create achievement label panel.
		JPanel achievementLabelPane = new JPanel(new GridLayout(2, 1, 0, 0));
		achievementPane.add(achievementLabelPane, BorderLayout.NORTH);

		// Create the achievement label.
		JLabel achievementLabel = new JLabel(Msg.getString("TabPanelScience.scientificAchievement"), SwingConstants.CENTER); //$NON-NLS-1$
		achievementLabelPane.add(achievementLabel);

		String totalAchievementString = StyleManager.DECIMAL_PLACES1.format(settlement.getTotalScientificAchievement());
		totalAchievementLabel = new JLabel(
			Msg.getString(
				"TabPanelScience.totalAchievementCredit", //$NON-NLS-1$
				totalAchievementString
			), SwingConstants.CENTER
		);
		achievementLabelPane.add(totalAchievementLabel);

		// Create the achievement scroll panel.
		JScrollPane achievementScrollPane = new JScrollPane();
		achievementScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		achievementPane.add(achievementScrollPane, BorderLayout.CENTER);

		// Create the achievement table.
		achievementTableModel = new AchievementTableModel(settlement);
		achievementTable = new JTable(achievementTableModel);
		achievementTable.setPreferredScrollableViewportSize(new Dimension(225, -1));
		achievementTable.setRowSelectionAllowed(true);
		achievementTable.setDefaultRenderer(Double.class, new NumberCellRenderer(1));
		achievementScrollPane.setViewportView(achievementTable);

		// Added sorting
		achievementTable.setAutoCreateRowSorter(true);

		// Align the preference score to the center of the cell
		DefaultTableCellRenderer renderer1 = new DefaultTableCellRenderer();
		renderer1.setHorizontalAlignment(SwingConstants.CENTER);
		achievementTable.getColumnModel().getColumn(0).setCellRenderer(renderer1);
		achievementTable.getColumnModel().getColumn(1).setCellRenderer(renderer1);
	}

	@Override
	public void update() {		

		// Get selected study in table if any.
		int selectedStudyIndex = studyTable.getSelectedRow();
		ScientificStudy selectedStudy = null;
		if (selectedStudyIndex >= 0) selectedStudy = studyTableModel.getStudy(selectedStudyIndex);

		// Update study table model.
		studyTableModel.update();

		// Reselect study in table.
		if (selectedStudy != null) {
			int newStudyIndex = studyTableModel.getStudyIndex(selectedStudy);
			if (newStudyIndex >= 0)
				studyTable.getSelectionModel().setSelectionInterval(newStudyIndex, newStudyIndex);
		}

		// Update achievement table model.
		achievementTableModel.update();

		// Update total achievement label.
		String totalAchievementString = StyleManager.DECIMAL_PLACES1.format(settlement.getTotalScientificAchievement());
		totalAchievementLabel.setText(Msg.getString("TabPanelScience.totalAchievementCredit", totalAchievementString)); //$NON-NLS-1$
	}

	/**
	 * Sets if the science tool button is enabled or not.
	 * @param enabled true if button enabled.
	 */
	private void setEnabledScienceToolButton(boolean enabled) {
		scienceToolButton.setEnabled(enabled);
	}

	/**
	 * Displays the scientific study selected in the table in the science tool.
	 */
	private void displayStudyInScienceTool() {
		int selectedStudyIndex = studyTable.getSelectedRow();
		if (selectedStudyIndex >= 0) {
			ScientificStudy selectedStudy = studyTableModel.getStudy(selectedStudyIndex);
			((ScienceWindow) getDesktop().openToolWindow(ScienceWindow.NAME)).setScientificStudy(selectedStudy);
		}
	}

	/**
	 * Inner class for study table model.
	 */
	private class StudyTableModel extends AbstractTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		// Data members.
		private Settlement settlement;
		private List<ScientificStudy> studies;

		/**
		 * Constructor.
		 * 
		 * @param settlement the settlement.
		 */
		private StudyTableModel(Settlement settlement) {
			// Use AbstractTableModel constructor.
			super();

			this.settlement = settlement;

			// Get all studies the settlement is primary for.
			studies = getSimulation().getScientificStudyManager().getAllStudies(settlement);
		}

		/**
		 * Returns the number of columns in the model.
		 * 
		 * @return the number of columns in the model.
		 */
		public int getColumnCount() {
			return 5;
		}

		@Override
		public String getColumnName(int column) {
			if (column == 0) 
				return Msg.getString("TabPanelScience.column.id"); //$NON-NLS-1$
			else if (column == 1) 
				return Msg.getString("TabPanelScience.column.study"); //$NON-NLS-1$
			else if (column == 2) 
				return Msg.getString("TabPanelScience.column.level"); //$NON-NLS-1$
			else if (column == 3) 
				return Msg.getString("TabPanelScience.column.phase"); //$NON-NLS-1$
			else if (column == 4)
				return Msg.getString("TabPanelScience.column.researcher"); //$NON-NLS-1$
			
			return null;
		}

		/**
		 * Returns the number of rows in the model.
		 * 
		 * @return the number of rows in the model.
		 */
		public int getRowCount() {
			return studies.size();
		}

		/**
		 * Returns the value for the cell at columnIndex and rowIndex.
		 * 
		 * @param rowIndex the row whose value is to be queried.
		 * @param columnIndex the column whose value is to be queried.
		 * @return the value Object at the specified cell.
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			String result = null;
			if ((rowIndex >= 0) && (rowIndex < studies.size())) {
				ScientificStudy study = studies.get(rowIndex);

				if (columnIndex == 0) 
					result = study.getID() + "";
				else if (columnIndex == 1) 
					result = study.getScience().getName();
				else if (columnIndex == 2) 
					result = study.getDifficultyLevel() + "";
				else if (columnIndex == 3) {
					result = study.getPhase().getName();
				}
				else {
					String researcherN = "";	
					if (study.getPrimaryResearcher() != null) {
						researcherN = study.getPrimaryResearcher().getName();
						result = researcherN;
					}
				}

			}
			return result;
		}

		/**
		 * Updates the table model.
		 */
		private void update() {
			List<ScientificStudy> newStudies = getSimulation().getScientificStudyManager().getAllStudies(settlement);
			if (!newStudies.equals(studies)) studies = newStudies;
			fireTableDataChanged();
		}

		/**
		 * Gets the scientific study in the table at a given row index.
		 * @param rowIndex the row index in the table.
		 * @return scientific study or null if invalid index.
		 */
		private ScientificStudy getStudy(int rowIndex) {
			ScientificStudy result = null;
			if ((rowIndex >= 0) && (rowIndex < studies.size()))
				result = studies.get(rowIndex);
			return result;
		}

		/**
		 * Gets the row index of a given scientific study.
		 * @param study the scientific study.
		 * @return the table row index or -1 if not in table.
		 */
		private int getStudyIndex(ScientificStudy study) {
			int result = -1;
			if ((study != null) && studies.contains(study)) result = studies.indexOf(study);
			return result;
		}
	}

	/**
	 * Inner class for achievement table model.
	 */
	private class AchievementTableModel
	extends AbstractTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		// Data members.
		private Settlement settlement;
		private ScienceType[] sciences;

		/** hidden constructor. */
		private AchievementTableModel(Settlement settlement) {
			// Use AbstractTableModel constructor.
			super();
			this.settlement = settlement;
			sciences = ScienceType.values();
		}

		/**
		 * Returns the number of columns in the model.
		 * @return the number of columns in the model.
		 */
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) return Msg.getString("TabPanelScience.column.science"); //$NON-NLS-1$
			else if (columnIndex == 1) return Msg.getString("TabPanelScience.column.achievementCredit"); //$NON-NLS-1$
			else return null;
		}

		/**
		 * Returns the most specific superclass for all the cell values in the column.
		 * @param columnIndex the index of the column.
		 * @return the common ancestor class of the object values in the model.
		 */
		@Override
		public Class<?> getColumnClass(int columnIndex) {
			Class<?> dataType = super.getColumnClass(columnIndex);
			if (columnIndex == 0) dataType = String.class;
			else if (columnIndex == 1) dataType = Double.class;
			return dataType;
		}

		/**
		 * Returns the number of rows in the model.
		 * @return the number of rows in the model.
		 */
		public int getRowCount() {
			return sciences.length;
		}

		/**
		 * Returns the value for the cell at columnIndex and rowIndex.
		 * @param rowIndex the row whose value is to be queried.
		 * @param columnIndex the column whose value is to be queried.
		 * @return the value Object at the specified cell.
		 */
		public Object getValueAt(int rowIndex, int columnIndex) {
			Object result = null;
			if ((rowIndex >= 0) && (rowIndex < sciences.length)) {
				ScienceType science = sciences[rowIndex];
				if (columnIndex == 0) result = science.getName();
				else if (columnIndex == 1) {
					result = settlement.getScientificAchievement(science);
				}
			}
			return result;
		}

		/**
		 * Updates the table model.
		 */
		private void update() {
			fireTableDataChanged();
		}
	}
	
	/**
     * Prepare object for garbage collection.
     */
	@Override
    public void destroy() {
    	scienceToolButton = null;
    	totalAchievementLabel = null;

    	achievementTable = null;
    	studyTable = null;

    	studyTableModel = null;
    	achievementTableModel = null;

		super.destroy();

    }
}
