/**
 * Mars Simulation Project
 * TabPanelFavorite.java
 * @version 3.08 2015-03-26
 * @author Manny Kung
 */
package org.mars_sim.msp.ui.swing.unit_window.person;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicComboBoxRenderer;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang3.text.WordUtils;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.TaskSchedule;
import org.mars_sim.msp.core.person.TaskSchedule.OneTask;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.ui.javafx.MainScene;
import org.mars_sim.msp.ui.swing.JComboBoxMW;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MainWindow;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.tool.MultisortTableHeaderCellRenderer;
import org.mars_sim.msp.ui.swing.tool.TableStyle;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;
import org.mars_sim.msp.ui.swing.unit_window.structure.StormTrackingWindow;


/**
 * The TabPanelSchedule is a tab panel showing the daily schedule a person.
 */
public class TabPanelSchedule
extends TabPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	//private int sol;
	private int todayCache;
	private int today ;

	private boolean hideRepeated, hideRepeatedCache, isRealTimeUpdate;

	private Integer selectedSol;
	private Integer todayInteger;

	private String shiftType, shiftCache = null;

	private JCheckBox hideRepeatedTasksCheckBox;
	private JCheckBox realTimeUpdateCheckBox;
	private JTextField shiftTF;


	private JComboBoxMW<Object> comboBox;
	private DefaultComboBoxModel<Object> comboBoxModel;
	private ScheduleTableModel scheduleTableModel;

	private List<OneTask> tasks;
	private List<Object> solList;
	private Map <Integer, List<OneTask>> schedules;

	private Person person;
	private Robot robot;
	private TaskSchedule taskSchedule;
	private PlannerWindow plannerWindow;


	/**
	 * Constructor.
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelSchedule(Unit unit, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelSchedule.title"), //$NON-NLS-1$
			null,
			Msg.getString("TabPanelSchedule.tooltip"), //$NON-NLS-1$
			unit, desktop
		);

		// Prepare combo box
        if (unit instanceof Person) {
         	person = (Person) unit;
         	taskSchedule = person.getTaskSchedule();
        }
        else if (unit instanceof Robot) {
        	robot = (Robot) unit;
        	taskSchedule = robot.getTaskSchedule();
        }

        schedules = taskSchedule.getSchedules();

		// Create label panel.
		JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(labelPanel);

		// Prepare label
		JLabel label = new JLabel(Msg.getString("TabPanelSchedule.label"), JLabel.CENTER); //$NON-NLS-1$
		labelPanel.add(label);

		// Prepare info panel.
//		JPanel infoPanel = new JPanel(new GridLayout(1, 3, 40, 0)); //new FlowLayout(FlowLayout.CENTER));
//		infoPanel.setBorder(new MarsPanelBorder());

       	// Create the button panel.
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(buttonPane);//, BorderLayout.NORTH);

		shiftType = person.getTaskSchedule().getShiftType();
		shiftCache = shiftType;
		JLabel shiftLabel = new JLabel(Msg.getString("TabPanelSchedule.shift"), JLabel.CENTER);
		buttonPane.add(shiftLabel);

		shiftTF = new JTextField(shiftCache);
		shiftTF.setEditable(false);
		shiftTF.setColumns(3);
		buttonPane.add(shiftTF);
		buttonPane.add(new JLabel("           "));

		// Create the Storm Tracking button.
		JButton button = new JButton("Open Planner");
		button.setToolTipText("Click to Open Personal Planner");
		button.addActionListener(
			new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					// Open storm tracking window.
					openPlannerWindow();
				}
			});
		buttonPane.add(button);

		Box box = Box.createHorizontalBox();
		box.setBorder(new MarsPanelBorder());

//		centerContentPanel.add(infoPanel, BorderLayout.NORTH);
		centerContentPanel.add(box, BorderLayout.NORTH);

		// Create hideRepeatedTaskBox.
		hideRepeatedTasksCheckBox = new JCheckBox(Msg.getString("TabPanelSchedule.checkbox.showRepeatedTask")); //$NON-NLS-1$
		//hideRepeatedTasksCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
		hideRepeatedTasksCheckBox.setFont(new Font("Serif", Font.PLAIN, 11));
		hideRepeatedTasksCheckBox.setToolTipText(Msg.getString("TabPanelSchedule.tooltip.showRepeatedTask")); //$NON-NLS-1$
		hideRepeatedTasksCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (hideRepeatedTasksCheckBox.isSelected()) {
					hideRepeated = true;
				}
				else {
					hideRepeated = false;
				}

			}
		});
		hideRepeatedTasksCheckBox.setSelected(hideRepeated);
//		infoPanel.add(hideRepeatedTasksCheckBox);
		box.add(hideRepeatedTasksCheckBox);
		box.add(Box.createHorizontalGlue());

    	today = taskSchedule.getSolCache();
    	todayInteger = (Integer) today ;
    	solList = new CopyOnWriteArrayList<Object>();

		int size = schedules.size();
		for (int i = 0 ; i < size + 1; i++ )
			// size + 1 is needed to add today into solList
			solList.add(i + 1);

    	// Create comboBoxModel
    	Collections.sort(solList, Collections.reverseOrder());
    	comboBoxModel = new DefaultComboBoxModel<Object>();
    	// Using internal iterator in lambda expression
		solList.forEach(s -> comboBoxModel.addElement(s));

		// Create comboBox
		comboBox = new JComboBoxMW<Object>(comboBoxModel);
		comboBox.setSelectedItem(todayInteger);
		//comboBox.setOpaque(false);
		comboBox.setRenderer(new PromptComboBoxRenderer());
		//comboBox.setRenderer(new PromptComboBoxRenderer(" List of Schedules "));
		//comboBox.setBackground(new Color(0,0,0,128));
		//comboBox.setBackground(new Color(255,229,204));
		//comboBox.setForeground(Color.orange);
		comboBox.setMaximumRowCount(7);
		//comboBox.setBorder(null);

		JPanel solPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		solPanel.add(comboBox);

//		infoPanel.add(solPanel);
		box.add(solPanel);
		box.add(Box.createHorizontalGlue());

    	selectedSol = (Integer) comboBox.getSelectedItem();
		if (selectedSol == null)
			comboBox.setSelectedItem(todayInteger);

		comboBox.setSelectedItem((Integer)1);
		comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	selectedSol = (Integer) comboBox.getSelectedItem();
            	if ( selectedSol != null ) // e.g. when first loading up
            		scheduleTableModel.update(hideRepeated, (int) selectedSol);
            	if (selectedSol == todayInteger)
            		// Binds comboBox with realTimeUpdateCheckBox
            		realTimeUpdateCheckBox.setSelected(true);
            }
		});

		// Create realTimeUpdateCheckBox.
		realTimeUpdateCheckBox = new JCheckBox(Msg.getString("TabPanelSchedule.checkbox.realTimeUpdate")); //$NON-NLS-1$
		realTimeUpdateCheckBox.setSelected(true);
		realTimeUpdateCheckBox.setHorizontalTextPosition(SwingConstants.RIGHT);
		realTimeUpdateCheckBox.setFont(new Font("Serif", Font.PLAIN, 11));
		realTimeUpdateCheckBox.setToolTipText(Msg.getString("TabPanelSchedule.tooltip.realTimeUpdate")); //$NON-NLS-1$
		realTimeUpdateCheckBox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if (realTimeUpdateCheckBox.isSelected()){
					isRealTimeUpdate = true;
					scheduleTableModel.update(hideRepeated, today);
					comboBox.setSelectedItem(todayInteger);
				}
				else
					isRealTimeUpdate = false;
			}
		});
		box.add(realTimeUpdateCheckBox);

		// Create schedule table model
		if (unit instanceof Person)
			scheduleTableModel = new ScheduleTableModel((Person) unit);
		else if (unit instanceof Robot)
			scheduleTableModel = new ScheduleTableModel((Robot) unit);

		// Create attribute scroll panel
		JScrollPane scrollPanel = new JScrollPane();
		scrollPanel.setBorder(new MarsPanelBorder());
		centerContentPanel.add(scrollPanel);

		// Create schedule table
		JTable table = new JTable(scheduleTableModel);
		table.setPreferredScrollableViewportSize(new Dimension(225, 100));
		table.getColumnModel().getColumn(0).setPreferredWidth(25);
		table.getColumnModel().getColumn(1).setPreferredWidth(150);
		table.setCellSelectionEnabled(false);
		// table.setDefaultRenderer(Integer.class, new NumberCellRenderer());
		scrollPanel.setViewportView(table);

		// 2015-06-08 Added sorting
		table.setAutoCreateRowSorter(true);
		table.getTableHeader().setDefaultRenderer(new MultisortTableHeaderCellRenderer());

		// 2015-06-08 Added setTableStyle()
		TableStyle.setTableStyle(table);

		update();
	}

	//private int GridLayout(int i, int j) {
		// TODO Auto-generated method stub
	//	return 0;
	//}

	/**
	 * Updates the info on this panel.
	 */
	public void update() {

		if (person != null) {
			shiftType = person.getTaskSchedule().getShiftType();

			//if (shiftCache != null)
			if (!shiftCache.equals(shiftType)) {
				shiftCache = shiftType;
				shiftTF.setText(shiftCache);
			}
		}

    	today = taskSchedule.getSolCache();
    	todayInteger = (Integer) today ;
       	selectedSol = (Integer) comboBox.getSelectedItem(); // necessary or else if (isRealTimeUpdate) below will have NullPointerException

       	// Update the sol box at the beginning of a new sol
    	if (today != todayCache) {
    		int size = schedules.size();
			solList.clear();
    		for (int i = 0 ; i < size + 1; i++ ) {
        		// size + 1 is needed for starting on sol 1
    			solList.add(i + 1);
    		}

	    	Collections.sort(solList, Collections.reverseOrder());
	    	DefaultComboBoxModel<Object> newComboBoxModel = new DefaultComboBoxModel<Object>();
			solList.forEach(s -> newComboBoxModel.addElement(s));

			// Update comboBox
			comboBox.setModel(newComboBoxModel);
			comboBox.setRenderer(new PromptComboBoxRenderer());

			// Note: Below is needed or else users will be constantly interrupted
			// as soon as the combobox got updated with the new day's schedule
			// and will be swapped out without warning.
			if (selectedSol != null)
				comboBox.setSelectedItem(selectedSol);
			else {
				comboBox.setSelectedItem(todayInteger);
				selectedSol = null;
			}

			todayCache = today;
    	}

       	// Turn off the Real Time Update if the user is still looking at a previous sol's schedule
       	if (selectedSol != todayInteger) {
       		isRealTimeUpdate = false;
       		realTimeUpdateCheckBox.setSelected(false);
       	}

		// Detects if the Hide Repeated box has changed. If yes, call for update
		if (hideRepeatedCache != hideRepeated) {
			hideRepeatedCache = hideRepeated;
			scheduleTableModel.update(hideRepeated, selectedSol);
		}

		if (isRealTimeUpdate)
			scheduleTableModel.update(hideRepeated, todayInteger);
	}

    public void setViewer(PlannerWindow w) {
    	this.plannerWindow = w;
    }

	/**
	 * Opens PlannerWindow
	 */
    // 2015-05-21 Added openPlannerWindow()
	private void openPlannerWindow() {

		MainWindow mw = desktop.getMainWindow();
		if (mw !=null )  {
			// Pause simulation
			mw.pauseSimulation();
			// Create PlannerWindow
			if (plannerWindow == null)
				plannerWindow = new PlannerWindow(unit, desktop, this);
			// Unpause simulation
			mw.unpauseSimulation();
		}

		MainScene ms = desktop.getMainScene();
		if (ms !=null )  {
			// Pause simulation
			ms.pauseSimulation();
			// Create PlannerWindow
			if (plannerWindow == null) {
				plannerWindow = new PlannerWindow(unit, desktop, this);
			}
			// Unpause simulation
			ms.unpauseSimulation();
		}

	}

	class PromptComboBoxRenderer extends BasicComboBoxRenderer {

		private static final long serialVersionUID = 1L;
		private String prompt;
		//public boolean isOptimizedDrawingEnabled();
		//private DefaultListCellRenderer defaultRenderer = new DefaultListCellRenderer();
		public PromptComboBoxRenderer(){
			//defaultRenderer.setHorizontalAlignment(DefaultListCellRenderer.CENTER);
		    //settlementListBox.setRenderer(defaultRenderer);
		    //setOpaque(false);
		    setHorizontalAlignment(CENTER);
		    setVerticalAlignment(CENTER);
		}

		public PromptComboBoxRenderer(String prompt){
				this.prompt = prompt;
			}

			@Override
		    public Component getListCellRendererComponent(JList list, Object value,
		            int index, boolean isSelected, boolean cellHasFocus) {
		        JComponent result = (JComponent)super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		        //Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
		        //component.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
				if (value == null) {
					setText( prompt );
					//this.setForeground(Color.orange);
			        //this.setBackground(new Color(184,134,11));
					return this;
				}

				setText(" Sol " + value);

				if (isSelected) {
					result.setForeground(new Color(184,134,11));
			        result.setBackground(Color.orange);

		          // unselected, and not the DnD drop location
		        } else {
		        	  result.setForeground(new Color(184,134,11));
		        	  result.setBackground(new Color(255,229,204)); //pale yellow (255,229,204)
				      //Color(184,134,11)) brown
		        }

		        //result.setOpaque(false);

		        return result;
		    }
	}

	/**
	 * Internal class used as model for the attribute table.
	 */
	private class ScheduleTableModel
	extends AbstractTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		//private TaskSchedule taskSchedule;
		//private List<OneTask> tasks;

		DecimalFormat fmt = new DecimalFormat("0000");

		/**
		 * hidden constructor.
		 * @param person {@link Person}
		 */
		private ScheduleTableModel(Unit unit) {
/*
	        Person person = null;
	        Robot robot = null;
	        if (unit instanceof Person) {
	         	person = (Person) unit;
	         	taskSchedule = person.getTaskSchedule();
	        }
	        else if (unit instanceof Robot) {
	        	robot = (Robot) unit;
	        	taskSchedule = robot.getTaskSchedule();
	        }

	        tasks = taskSchedule.getTodaySchedule();
*/

			//tasks = new ArrayList<>(taskSchedule.getTodaySchedule());
		}

		@Override
		public int getRowCount() {
			if (tasks != null)
				return tasks.size();
			else
				return 0;
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public Class<?> getColumnClass(int columnIndex) {
			Class<?> dataType = super.getColumnClass(columnIndex);
			if (columnIndex == 0) dataType = String.class;
			if (columnIndex == 1) dataType = String.class;
			//if (columnIndex == 2) dataType = String.class;
			return dataType;
		}

		@Override
		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) return Msg.getString("TabPanelSchedule.column.time"); //$NON-NLS-1$
			else if (columnIndex == 1) return Msg.getString("TabPanelSchedule.column.activity"); //$NON-NLS-1$
			//else if (columnIndex == 2) return Msg.getString("TabPanelSchedule.column.location"); //$NON-NLS-1$
			else return null;
		}

		@Override
		public Object getValueAt(int row, int column) {
			if (column == 0) return fmt.format(tasks.get(row).getStartTime());
			else if (column == 1) return tasks.get(row).getDoAction();
			//else if (column == 2) return tasks.get(row).getLocation();
			else return null;
		}

		/**
		 * Prepares a list of activities done on the selected day
		 * @param hideRepeatedTasks
		 * @param selectedSol
		 */
		public void update(boolean hideRepeatedTasks, int selectedSol) {
	        int todaySol = taskSchedule.getSolCache();

	        // Load previous day's schedule if selected
			if (todaySol == selectedSol) {
				// Load today's schedule
				tasks = new CopyOnWriteArrayList<OneTask>(taskSchedule.getTodaySchedule());

			}
			else {
				tasks = new CopyOnWriteArrayList<OneTask>(schedules.get(selectedSol));
			}

			// check if user selected hide repeated tasks checkbox
			if (tasks != null && hideRepeatedTasks) {
				// show only non-repeating consecutive tasks
				List<OneTask> thisSchedule = new CopyOnWriteArrayList<OneTask>(tasks);
		        int i = thisSchedule.size() - 1;
		        // TODO: do I need "thisSchedule" ? or just use "tasks"
		        while (i > 0 ) {

		        	OneTask currentTask = thisSchedule.get(i);
		        	OneTask lastTask = null;

		        	if ( i - 1 > -1 )
		        		lastTask = thisSchedule.get(i - 1);

		        	String lastActivity = lastTask.getDoAction();
		        	String currentActivity = currentTask.getDoAction();
		        	// check if the last task is the same as the current task
		        	if (lastActivity.equals(currentActivity)) {
		        		// remove the current task if it's the same as the last task
		        		thisSchedule.remove(i);
		        	}

		        	i--;
		        }

		        tasks = thisSchedule;
			}

        	fireTableDataChanged();

		}

	}

}