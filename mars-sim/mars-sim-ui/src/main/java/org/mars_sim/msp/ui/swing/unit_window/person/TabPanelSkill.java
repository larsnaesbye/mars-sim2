/**
 * Mars Simulation Project
 * SkillTabPanel.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.person;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillManager;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.NumberCellRenderer;
import org.mars_sim.msp.ui.swing.tool.MultisortTableHeaderCellRenderer;
import org.mars_sim.msp.ui.swing.tool.TableStyle;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

/**
 * The SkillTabPanel is a tab panel for the skills of a person.
 */
public class TabPanelSkill
extends TabPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	private SkillTableModel skillTableModel;
	//private Person person;
	//private Robot robot;
	/**
	 * Constructor 1.
	 * @param person the person.
	 * @param desktop the main desktop.
	 */
	public TabPanelSkill(Person person, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelSkill.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelSkill.tooltip"), //$NON-NLS-1$
			person, desktop
		);

		//this.person = person;

		// Create skill table model
		skillTableModel = new SkillTableModel(person);

		init();
	}

	/**
	 * Constructor 2.
	 * @param person the person.
	 * @param desktop the main desktop.
	 */
	public TabPanelSkill(Robot robot, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelSkill.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelSkill.tooltip"), //$NON-NLS-1$
			robot, desktop
		);

		//this.robot = robot;

		// Create skill table model
		skillTableModel = new SkillTableModel(robot);

		init();
	}

	public void init() {

		// Create skill label panel.
		JPanel skillLabelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(skillLabelPanel);

		// Create skill label
		JLabel skillLabel = new JLabel(Msg.getString("TabPanelSkill.label"), JLabel.CENTER); //$NON-NLS-1$
		skillLabelPanel.add(skillLabel);

		// Create skill scroll panel
		JScrollPane skillScrollPanel = new JScrollPane();
		skillScrollPanel.setBorder(new MarsPanelBorder());
		centerContentPanel.add(skillScrollPanel);

		// Create skill table
		JTable skillTable = new JTable(skillTableModel);
		skillTable.setPreferredScrollableViewportSize(new Dimension(225, 100));
		skillTable.getColumnModel().getColumn(0).setPreferredWidth(50);
		skillTable.getColumnModel().getColumn(1).setPreferredWidth(120);
		skillTable.setCellSelectionEnabled(false);
		skillTable.setDefaultRenderer(Integer.class, new NumberCellRenderer());
		skillScrollPanel.setViewportView(skillTable);

		// 2015-06-08 Added sorting
		skillTable.setAutoCreateRowSorter(true);
		skillTable.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());

		// 2015-06-08 Added setTableStyle()
		TableStyle.setTableStyle(skillTable);
	}

	/**
	 * Updates the info on this panel.
	 */
	public void update() {
		skillTableModel.update();
	}

	/**
	 * Internal class used as model for the skill table.
	 */
	private static class SkillTableModel
	extends AbstractTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		private SkillManager manager;
		private Map<String, Integer> skills;
		private List<String> skillNames;

		private SkillTableModel(Unit unit) {
			Person person = null;
	        Robot robot = null;

	        if (unit instanceof Person) {
	         	person = (Person) unit;
				manager = person.getMind().getSkillManager();
	        }
	        else if (unit instanceof Robot) {
	        	robot = (Robot) unit;
				manager = robot.getBotMind().getSkillManager();
	        }


			SkillType[] keys = manager.getKeys();
			skills = new HashMap<String, Integer>();
			skillNames = new ArrayList<String>();
			for (SkillType skill : keys) {
				int level = manager.getSkillLevel(skill);
				if (level > 0) {
					skillNames.add(skill.getName());
					skills.put(skill.getName(), level);
				}
			}
		}

		public int getRowCount() {
			return skillNames.size();
		}

		public int getColumnCount() {
			return 2;
		}

		public Class<?> getColumnClass(int columnIndex) {
			Class<?> dataType = super.getColumnClass(columnIndex);
			if (columnIndex == 1) dataType = String.class;
			if (columnIndex == 0) dataType = Integer.class;
			return dataType;
		}

		public String getColumnName(int columnIndex) {
			if (columnIndex == 1) return Msg.getString("TabPanelSkill.column.skill"); //$NON-NLS-1$
			else if (columnIndex == 0) return Msg.getString("TabPanelSkill.column.level"); //$NON-NLS-1$
			else return null;
		}

		public Object getValueAt(int row, int column) {
			if (column == 1) return skillNames.get(row);
			else if (column == 0) return skills.get(skillNames.get(row));
			else return null;
		}

		public void update() {
			SkillType[] keys = manager.getKeys();
			List<String> newSkillNames = new ArrayList<String>();
			Map<String, Integer> newSkills = new HashMap<String, Integer>();
			for (SkillType skill : keys) {
				int level = manager.getSkillLevel(skill);
				if (level > 0) {
					newSkillNames.add(skill.getName());
					newSkills.put(skill.getName(), level);
				}
			}

			if (!skills.equals(newSkills)) {
				skillNames = newSkillNames;
				skills = newSkills;
				fireTableDataChanged();
			}
		}
	}
}