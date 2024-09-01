/*
 * Mars Simulation Project
 * ToolToolBar.java
 * @date 2023-04-02
 * @author Scott Davis
 */
package com.mars_sim.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.format.DateTimeFormatter;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import org.httprpc.sierra.DatePicker;

import com.mars_sim.core.time.MarsTime;
import com.mars_sim.core.time.MarsTimeFormat;
import com.mars_sim.core.time.MasterClock;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.ui.swing.astroarts.OrbitViewer;
import com.mars_sim.ui.swing.tool.commander.CommanderWindow;
import com.mars_sim.ui.swing.tool.guide.GuideWindow;
import com.mars_sim.ui.swing.tool.mission.MissionWindow;
import com.mars_sim.ui.swing.tool.monitor.MonitorWindow;
import com.mars_sim.ui.swing.tool.navigator.NavigatorWindow;
import com.mars_sim.ui.swing.tool.resupply.ResupplyWindow;
import com.mars_sim.ui.swing.tool.science.ScienceWindow;
import com.mars_sim.ui.swing.tool.search.SearchWindow;
import com.mars_sim.ui.swing.tool.settlement.SettlementWindow;
import com.mars_sim.ui.swing.tool.time.MarsCalendarDisplay;
import com.mars_sim.ui.swing.tool.time.TimeWindow;
import com.mars_sim.ui.swing.utils.SwingHelper;

/**
 * The ToolToolBar class is a UI toolbar for holding tool buttons. There should
 * only be one instance and it is contained in the {@link MainWindow} instance.
 */
public class ToolToolBar extends JToolBar implements ActionListener {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private static final String SAVE = "SAVE";
	private static final String SAVEAS = "SAVEAS";
	private static final String EXIT = "EXIT";
	private static final String MARSCAL = "MARS-CAL";

	private static final DateTimeFormatter SHORT_TIMESTAMP_FORMATTER = 
			DateTimeFormatter.ofPattern("yyyy-MMM-dd HH:mm:ss");

	private static final String DISPLAY_HELP = "display-help";
	private static final String MAIN_WIKI = "main-wiki";
	private static final String WIKI_URL = Msg.getString("ToolToolBar.wiki.url"); //$NON-NLS-1$
	
	/** Main window that contains this toolbar. */
	private MainWindow parentMainWindow;

	private MarsCalendarDisplay calendarDisplay; 
	
	private JLabel monthLabel;
	private JLabel weeksolLabel;

	private JLabel earthDate;
	private JLabel missionSol;
	private JLabel marsTime;

	private JPanel calendarPane;
	private DatePicker datePicker;
	private MasterClock masterClock;

	/**
	 * Constructs a ToolToolBar object.
	 * 
	 * @param parentMainWindow the main window pane
	 */
	public ToolToolBar(MainWindow parentMainWindow) {

		// Use JToolBar constructor
		super();

		// WebLaf is breaking the default layout, remove this once totally gone.
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

		// Initialize data members
		this.parentMainWindow = parentMainWindow;
		masterClock = parentMainWindow.getDesktop().getSimulation().getMasterClock();

		// Set name
		setName(Msg.getString("ToolToolBar.toolbar")); //$NON-NLS-1$

		setFloatable(true);

		setRollover(true);
		
		// Prepare tool buttons
		prepareToolButtons();
		setMaximumSize(new Dimension(0, 32));
		incrementClocks(masterClock);
	}

	/** 
	 * Prepares tool buttons 
	 */
	private void prepareToolButtons() {

		addToolButton(SAVE, Msg.getString("mainMenu.save"), "action/save"); //$NON-NLS-1$ //$NON-NLS-2$
		addToolButton(SAVEAS, Msg.getString("mainMenu.saveAs"), "action/saveAs"); //$NON-NLS-
		addToolButton(EXIT, Msg.getString("mainMenu.exit"), "action/exit"); //$NON-NLS-

		addSeparator(new Dimension(20, 20));

		// Add Tools buttons
		addToolButton(NavigatorWindow.NAME, null, NavigatorWindow.ICON); //$NON-NLS-
		addToolButton(SearchWindow.NAME, null, SearchWindow.ICON); //$NON-NLS-1$
		addToolButton(TimeWindow.NAME, null, TimeWindow.ICON); //$NON-NLS-1$
		addToolButton(MonitorWindow.NAME, null, MonitorWindow.ICON); //$NON-NLS-1$
		addToolButton(MissionWindow.NAME, null, MissionWindow.ICON); //$NON-NLS-1$
		addToolButton(SettlementWindow.NAME, null, SettlementWindow.ICON); //$NON-NLS-1$
		addToolButton(ScienceWindow.NAME, null, ScienceWindow.ICON); //$NON-NLS-1$
		addToolButton(ResupplyWindow.NAME, null, ResupplyWindow.ICON); //$NON-NLS-1$
		addToolButton(CommanderWindow.NAME, null, CommanderWindow.ICON); //$NON-NLS-1$

		addToolButton(OrbitViewer.NAME, "Open Orbit Viewer", OrbitViewer.ICON);
		
		// Everything after this is on the right hand side
		add(Box.createHorizontalGlue()); 

		earthDate = createTextLabel("Greenwich Mean Time (GMT) for Earth");
		add(earthDate);
			
//		datePicker = new DatePicker();		
//      var earthTime = masterClock.getEarthTime();
////    var now = LocalDate.now();
//      var now = LocalDate.of(earthTime.getYear(), earthTime.getMonth(), earthTime.getDayOfMonth());
//      datePicker.setDate(now);
//      datePicker.setMinimumDate(now.minusMonths(3));
//      datePicker.setMaximumDate(now.plusMonths(3));
//      datePicker.setPopupVerticalAlignment(VerticalAlignment.TOP);
//      datePicker.addActionListener(event -> showSelection(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL))); //datePicker.getDate()));	
		
		addSeparator();
		missionSol = createTextLabel("Simulation Sol Count");
		add(missionSol);
		addSeparator();
		marsTime = createTextLabel("Universal Mean Time (UMT) for Mars. Format: 'Orbit-Month-Sol:Millisols Weeksol'");
		add(marsTime);
		addSeparator();

		calendarPane = setupCalendarPanel(masterClock.getMarsTime());	
		addToolButton(MARSCAL, "Open Mars Calendar", "schedule");

		addSeparator(new Dimension(20, 20));

		// Add guide button
		addToolButton(DISPLAY_HELP, "View Help tool", GuideWindow.guideIcon);
		
		// Add wiki button
		addToolButton(MAIN_WIKI, "View mars-sim wiki", GuideWindow.wikiIcon);
	}

//    private void showSelection(DateTimeFormatter formatter) {//, TemporalAccessor value) {
////        var message = String.format("You selected %s.", formatter.format(value));
//        var earthTime = masterClock.getEarthTime();
////      var now = LocalDate.now();
//        var now = LocalDate.of(earthTime.getYear(), earthTime.getMonth(), earthTime.getDayOfMonth());
//        datePicker.setDate(now);
////        earthDate.setText(masterClock.getEarthTime().format(SHORT_TIMESTAMP_FORMATTER));
//        earthDate.setText(String.format(formatter.format(now)));
//    }
    
	/**
	 * Adds a tool bar button.
	 * 
	 * @param toolName
	 * @param tooltip
	 * @param iconKey
	 */
	private void addToolButton(String toolName, String tooltip, String iconKey) {
		JButton toolButton = new JButton(ImageLoader.getIconByName(iconKey));
		toolButton.setActionCommand(toolName);
		toolButton.setPreferredSize(new Dimension(30, 30));
		toolButton.setMaximumSize(new Dimension(30, 30));
		if (tooltip == null) {
			tooltip = toolName;
		}
		toolButton.setToolTipText(tooltip);
		toolButton.addActionListener(this);
		add(toolButton);
	}

	/**
	 * Adds a tool bar button.
	 * 
	 * @param toolName
	 * @param tooltip
	 * @param icon
	 */
	private void addToolButton(String toolName, String tooltip, Icon icon) {
		JButton toolButton = new JButton(icon);
		toolButton.setActionCommand(toolName);
		toolButton.setPreferredSize(new Dimension(30, 30));
		toolButton.setMaximumSize(new Dimension(30, 30));
		if (tooltip == null) {
			tooltip = toolName;
		}
		toolButton.setToolTipText(tooltip);
		toolButton.addActionListener(this);
		add(toolButton);
	}
	
	
	private JLabel createTextLabel(String tooltip) {
		JLabel label = new JLabel();
		Border margin = new EmptyBorder(2,5,2,5);
		label.setBorder(new CompoundBorder(BorderFactory.createLoweredBevelBorder(), margin));
		label.setToolTipText(tooltip);
		return label;
	}

	private JPanel setupCalendarPanel(MarsTime marsClock) {
		JPanel innerPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));

		calendarDisplay = new MarsCalendarDisplay(marsClock, parentMainWindow.getDesktop());
		innerPane.add(calendarDisplay);

		final JPanel midPane = new JPanel(new BorderLayout(2, 2));

		midPane.add(innerPane, BorderLayout.CENTER);
		midPane.setBorder(new BevelBorder(BevelBorder.LOWERED, Color.ORANGE, new Color(210, 105, 30)));

		final JPanel outerPane = new JPanel(new BorderLayout(2, 2));
		outerPane.add(midPane, BorderLayout.CENTER);

		// Create Martian month label
    	String mn = "Month : " + marsClock.getMonthName();
    	monthLabel = new JLabel(mn, SwingConstants.CENTER);
    	
    	// Create Martian Weeksol label
    	String wd = "Weeksol : " + MarsTimeFormat.getSolOfWeekName(marsClock);
    	weeksolLabel = new JLabel(wd, SwingConstants.CENTER);

		JPanel monthPane = new JPanel(new GridLayout(2, 1));
		monthPane.add(weeksolLabel, SwingConstants.CENTER);
		monthPane.add(monthLabel, SwingConstants.CENTER);
		midPane.add(monthPane, BorderLayout.NORTH);
		
    	return outerPane;
	}

	/**
	 * Increments the label of both the earth and mars clocks.
	 * 
	 * @param master
	 */
	public void incrementClocks(MasterClock master) {
		missionSol.setText("Sol : " + master.getMarsTime().getMissionSol());
		earthDate.setText(master.getEarthTime().format(SHORT_TIMESTAMP_FORMATTER));
		marsTime.setText(master.getMarsTime().getTruncatedDateTimeStamp());
	}

	/** 
	 * ActionListener method overridden.
	 */
	@Override
	public void actionPerformed(ActionEvent event) {

		switch(event.getActionCommand()) {
			case SAVE:
				parentMainWindow.saveSimulation(true);
				break;

			case SAVEAS:
				parentMainWindow.saveSimulation(false);
				break;

			case EXIT:
				parentMainWindow.exitSimulation();
				break;
			
			case MARSCAL:
				MarsTime mc = masterClock.getMarsTime();
				calendarDisplay.update(mc);
		
				String mn = "Month : " + mc.getMonthName();
				monthLabel.setText(mn);
				
				String wd = "Weeksol : " + MarsTimeFormat.getSolOfWeekName(mc);
				weeksolLabel.setText(wd);

				JDialog popOver = SwingHelper.createPopupWindow(calendarPane, -1, -1, -75, 20);
				popOver.setVisible(true);
				break;
			
			case DISPLAY_HELP:
				parentMainWindow.showHelp(null); // Default help page
				break;
				
			case MAIN_WIKI:
				SwingHelper.openBrowser(WIKI_URL);
				break;
				
			default:
				parentMainWindow.getDesktop().openToolWindow(event.getActionCommand());
				break;
		}
	}
}