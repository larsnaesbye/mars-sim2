/**
 * Mars Simulation Project
 * MembersPanel.java
 * @version 3.08 2015-03-24
 * @author Manny Kung
 */

package org.mars_sim.msp.ui.swing.tool.mission.create;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.mars_sim.msp.core.CollectionUtils;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.Robot;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;

/**
 * A wizard panel to select mission bot members.
 */
class BotMembersPanel
extends WizardPanel
implements ActionListener {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** The wizard panel name. */
	private final static String NAME = "Members";

	// Data members.
	private BotsTableModel botsTableModel;
	private JTable botsTable;
	private BotMembersTableModel botMembersTableModel;

	private JTable botMembersTable;
	private JLabel errorMessageLabel;
	private JButton addButton;
	private JButton removeButton;
	private JLabel roverCapacityLabel;

	/**
	 * Constructor
	 * @param wizard the create mission wizard.
	 */
	BotMembersPanel(CreateMissionWizard wizard) {
		// Use WizardPanel constructor
		super(wizard);

		// Set the layout.
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

		// Set the border.
		setBorder(new MarsPanelBorder());

		// Create the select members label.
		JLabel selectMembersLabel = new JLabel("Select Bots for the Mission", JLabel.CENTER);
		selectMembersLabel.setFont(selectMembersLabel.getFont().deriveFont(Font.BOLD));
		selectMembersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(selectMembersLabel);

		// Create the available bots label.
		JLabel availableBotsLabel = new JLabel("Available Bots", JLabel.CENTER);
		availableBotsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(availableBotsLabel);

		// Create the bots panel.
		JPanel botsPane = new JPanel(new BorderLayout(0, 0));
		botsPane.setPreferredSize(new Dimension(300, 150));
		botsPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(botsPane);

		// Create scroll panel for available bots list.
		JScrollPane botsScrollPane = new JScrollPane();
		botsPane.add(botsScrollPane, BorderLayout.CENTER);

		// Create the bots table model.
		botsTableModel = new BotsTableModel();

		// Create the bots table.
		botsTable = new JTable(botsTableModel);
		botsTable.setDefaultRenderer(Object.class, new UnitTableCellRenderer(botsTableModel));
		botsTable.setRowSelectionAllowed(true);
		botsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		botsTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						// Get the selected rows.
						int[] selectedRows = botsTable.getSelectedRows();
						if (selectedRows.length > 0) {
							if (e.getValueIsAdjusting()) {
								botMembersTable.clearSelection();

								// Check if any of the rows failed.
								boolean failedRow = false;
								for (int selectedRow : selectedRows)
									if (botsTableModel.isFailureRow(selectedRow)) failedRow = true;

								if (failedRow) {
									// Display failed row message and disable add button.
									errorMessageLabel.setText("One or more selected bots cannot be used on the mission " +
											"(see red cells).");
									addButton.setEnabled(false);
								}
								else {
									// Check if number of rows exceed rover remaining capacity.
									if (selectedRows.length > getRemainingRoverCapacity()) {
										// Display over capacity message and disable add button.
										errorMessageLabel.setText("Not enough rover capacity to hold selected bots.");
										addButton.setEnabled(false);
									}
									else {
										// Enable add button.
										errorMessageLabel.setText(" ");
										addButton.setEnabled(true);
									}
								}
							}
						}
						else {
							// Disable add button when no rows are selected.
							addButton.setEnabled(false);
							errorMessageLabel.setText(" ");
						}
					}
				}
				);
		// call it a click to add button when user double clicks the table
		botsTable.addMouseListener(
				new MouseListener() {
					public void mouseReleased(MouseEvent e) {}
					public void mousePressed(MouseEvent e) {}
					public void mouseExited(MouseEvent e) {}
					public void mouseEntered(MouseEvent e) {}
					public void mouseClicked(MouseEvent e) {
						if (e.getClickCount() == 2 && !e.isConsumed()) {
							addButtonClicked();
						}
					}
				}
				);
		botsScrollPane.setViewportView(botsTable);

		// Create the message label.
		errorMessageLabel = new JLabel(" ", JLabel.CENTER);
		errorMessageLabel.setForeground(Color.RED);
		errorMessageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(errorMessageLabel);

		// Add vertical strut to make some UI space.
		add(Box.createVerticalStrut(10));

		// Create the button panel.
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
		buttonPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(buttonPane);

		// Create the add button.
		addButton = new JButton("Add Bots");
		addButton.setEnabled(false);
		addButton.addActionListener(this);
		buttonPane.add(addButton);

		// Create the remove button.
		removeButton = new JButton("Remove Bots");
		removeButton.setEnabled(false);
		removeButton.addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						// Remove the selected rows in the members table to the bots table.
						int[] selectedRows = botMembersTable.getSelectedRows();
						Collection<Robot> bots = new ConcurrentLinkedQueue<Robot>();
						for (int selectedRow : selectedRows)
							bots.add((Robot) botMembersTableModel.getUnit(selectedRow));
						botsTableModel.addRobots(bots);
						botMembersTableModel.removeRobots(bots);
						updateRoverCapacityLabel();
					}
				});
		buttonPane.add(removeButton);

		// Add a vertical strut to make UI space.
		add(Box.createVerticalStrut(10));

		// Create the rover capacity label.
		roverCapacityLabel = new JLabel("Remaining Rover Capacity: ");
		roverCapacityLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(roverCapacityLabel);

		// Add a vertical strut to make UI space.
		add(Box.createVerticalStrut(10));

		// Create the members label.
		JLabel membersLabel = new JLabel("Mission Bots");
		membersLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(membersLabel);

		// Create the members panel.
		JPanel membersPane = new JPanel(new BorderLayout(0, 0));
		membersPane.setPreferredSize(new Dimension(300, 150));
		membersPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(membersPane);

		// Create scroll panel for members list.
		JScrollPane membersScrollPane = new JScrollPane();
		membersPane.add(membersScrollPane, BorderLayout.CENTER);

		// Create the members table model.
		botMembersTableModel = new BotMembersTableModel();

		// Create the members table.
		botMembersTable = new JTable(botMembersTableModel);
		botMembersTable.setRowSelectionAllowed(true);
		botMembersTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		botMembersTable.getSelectionModel().addListSelectionListener(
				new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent e) {
						int[] selectedRows = botMembersTable.getSelectedRows();
						if (selectedRows.length > 0) {
							if (e.getValueIsAdjusting()) {
								// Enable the remove button.
								botsTable.clearSelection();
								removeButton.setEnabled(true);
							}
						}
						else removeButton.setEnabled(false);
					}
				});
		membersScrollPane.setViewportView(botMembersTable);
	}

	/**
	 * Gets the wizard panel name.
	 * @return panel name.
	 */
	String getPanelName() {
		return NAME;
	}

	/**
	 * Commits changes from this wizard panel.
	 * @retun true if changes can be committed.
	 */
	boolean commitChanges() {
		Collection<Unit> units = new ConcurrentLinkedQueue<Unit>();
		for (int x = 0; x < botMembersTableModel.getRowCount(); x++) 
			units.add((Unit) botMembersTableModel.getUnit(x));
		getWizard().getMissionData().setMixedMembers(units);
		return true;
	}

	/**
	 * Clear information on the wizard panel.
	 */
	void clearInfo() {
		botsTable.clearSelection();
		botMembersTable.clearSelection();
		errorMessageLabel.setText(" ");
	}

	/**
	 * Updates the wizard panel information.
	 */
	void updatePanel() {
		botsTableModel.updateTable();
		botMembersTableModel.updateTable();
		updateRoverCapacityLabel();
	}

	/**
	 * Updates the rover capacity label.
	 */
	void updateRoverCapacityLabel() {
		String type = getWizard().getMissionData().getType();
		if (MissionDataBean.CONSTRUCTION_MISSION.equals(type)) {
			roverCapacityLabel.setText(" ");
		}
		else if (MissionDataBean.SALVAGE_MISSION.equals(type)) { 
			roverCapacityLabel.setText(" ");
		}
		else {
			roverCapacityLabel.setText("Remaining Rover Capacity: " + getRemainingRoverCapacity());
		}
	}

	/**
	 * Gets the remaining rover capacity.
	 * @return rover capacity.
	 */
	int getRemainingRoverCapacity() {
		String type = getWizard().getMissionData().getType();
		if (MissionDataBean.CONSTRUCTION_MISSION.equals(type)) return Integer.MAX_VALUE;
		else if (MissionDataBean.SALVAGE_MISSION.equals(type)) return Integer.MAX_VALUE;
		else {
			int roverCapacity = getWizard().getMissionData().getRover().getCrewCapacity();
			int memberNum = botMembersTableModel.getRowCount();
			return roverCapacity - memberNum;
		}
	}

	/**
	 * Table model for bots.
	 */
	private class BotsTableModel
	extends UnitTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		/** Constructor. */
		private BotsTableModel() {
			// Use UnitTableModel constructor.
			super();

			// Add table columns.
			columns.add("Name");
			columns.add("Job");
			columns.add("Current Mission");
			columns.add("Performance");
			columns.add("Health");
		}

		/**
		 * Returns the value for the cell at columnIndex and rowIndex.
		 * @param row the row whose value is to be queried.
		 * @param column the column whose value is to be queried.
		 * @return the value Object at the specified cell.
		 */
		public Object getValueAt(int row, int column) {
			Object result = null;

			if (row < units.size()) {
				Robot robot = (Robot) getUnit(row);

				try {
					if (column == 0) 
						result = robot.getName();
					else if (column == 1) 
						result = robot.getBotMind().getRobotJob().toString();
					else if (column == 2) {
						Mission mission = robot.getBotMind().getMission();
						if (mission != null) result = mission.getName();
						else result = "none";
					}
					else if (column == 3) 
						result = (int) (robot.getPerformanceRating() * 100D) + "%";
					else if (column == 4)
						result = robot.getPhysicalCondition().getHealthSituation();
				}
				catch (Exception e) {}
			}

			return result;
		}

		/**
		 * Updates the table data.
		 */
		void updateTable() {
			units.clear();
			MissionDataBean missionData = getWizard().getMissionData();
			Settlement settlement = missionData.getStartingSettlement();
			if (MissionDataBean.CONSTRUCTION_MISSION.equals(missionData.getType()))
				settlement = missionData.getConstructionSettlement();
			else if (MissionDataBean.SALVAGE_MISSION.equals(missionData.getType()))
				settlement = missionData.getSalvageSettlement();
			Collection<Robot> robots = CollectionUtils.sortByName(settlement.getRobots());
			Iterator<Robot> i = robots.iterator();
			while (i.hasNext()) units.add(i.next());
			fireTableDataChanged();
		}

		/**
		 * Checks if a table cell is a failure cell.
		 * @param row the table row.
		 * @param column the table column.
		 * @return true if cell is a failure cell.
		 */
		boolean isFailureCell(int row, int column) {
			boolean result = false;

			if (row < units.size()) {
				Robot robot = (Robot) getUnit(row);

				if (column == 2) {
					if (robot.getBotMind().getMission() != null) return true;
				}
			}

			return result;
		}

		/**
		 * Adds robots to the table.
		 * @param robots the collection of robots to add.
		 */
		void addRobots(Collection<Robot> robots) {
			Iterator<Robot> i = robots.iterator();
			while (i.hasNext()) {
				Robot robot = i.next();
				if (!units.contains(robot)) units.add(robot);
			}
			units = CollectionUtils.sortByName(units);
			fireTableDataChanged();
		}

		/**
		 * Removes robots from the table.
		 * @param robots the collection of robots to remove.
		 */
		void removeRobots(Collection<Robot> robots) {
			Iterator<Robot> i = robots.iterator();
			while (i.hasNext()) {
				Robot robot = i.next();
				if (units.contains(robot)) units.remove(robot);
			}
			fireTableDataChanged();
		}
	}

	/**
	 * A table model for mission members.
	 */
	private class BotMembersTableModel
	extends UnitTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		/** * Constructor. */
		private BotMembersTableModel() {
			// Use UnitTableModel constructor.
			super();

			// Add columns.
			columns.add("Name");
			columns.add("Job");
			columns.add("Current Mission");
			columns.add("Performance");
			columns.add("Health");
		}

		/**
		 * Returns the value for the cell at columnIndex and rowIndex.
		 * @param row the row whose value is to be queried
		 * @param column the column whose value is to be queried
		 * @return the value Object at the specified cell
		 */
		public Object getValueAt(int row, int column) {
			Object result = null;

			if (row < units.size()) {
				Robot robot = (Robot) getUnit(row);

				try {
					if (column == 0) 
						result = robot.getName();
					else if (column == 1) 
						result = robot.getBotMind().getRobotJob().toString();
					else if (column == 2) {
						Mission mission = robot.getBotMind().getMission();
						if (mission != null) result = mission.getName();
						else result = "none";
					}
					else if (column == 3) 
						result = (int) (robot.getPerformanceRating() * 100D) + "%";
					else if (column == 4)
						result = robot.getPhysicalCondition().getHealthSituation();
				}
				catch (Exception e) {}
			}

			return result;
		}

		/**
		 * Updates the table data.
		 */
		void updateTable() {
			units.clear();
			fireTableDataChanged();
		}

		/**
		 * Checks if a table cell is a failure cell.
		 * @param row the table row.
		 * @param column the table column.
		 * @return true if cell is a failure cell.
		 */
		boolean isFailureCell(int row, int column) {
			return false;
		}

		/**
		 * Adds robots to the table.
		 * @param robots the collection of robots to add.
		 */
		void addRobots(Collection<Robot> robots) {
			Iterator<Robot> i = robots.iterator();
			while (i.hasNext()) {
				Robot robot = i.next();
				if (!units.contains(robot)) units.add(robot);
			}
			units = CollectionUtils.sortByName(units);
			fireTableDataChanged();

			getWizard().setButtons(units.size() > 0);
		}

		/**
		 * Removes robots from the list.
		 * @param robots the collection of robots to remove.
		 */
		void removeRobots(Collection<Robot> robots) {
			Iterator<Robot> i = robots.iterator();
			while (i.hasNext()) {
				Robot robot = i.next();
				if (units.contains(robot)) units.remove(robot);
			}
			fireTableDataChanged();

			getWizard().setButtons(units.size() > 0);
		}
	}


	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == addButton) {
			addButtonClicked();
		}
	}

	private final void addButtonClicked() {
		// Add the selected rows in the robots table to the members table.
		int[] selectedRows = botsTable.getSelectedRows();
		Collection<Robot> robots = new ConcurrentLinkedQueue<Robot>();
		for (int selectedRow : selectedRows) robots.add((Robot) botsTableModel.getUnit(selectedRow));
		botsTableModel.removeRobots(robots);
		botMembersTableModel.addRobots(robots);
		updateRoverCapacityLabel();
	}

}