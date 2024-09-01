/*

 * Mars Simulation Project
 * TimeWindow.java
 * @date 2023-09-08
 * @author Scott Davis
 */

package com.mars_sim.ui.swing.tool.time;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

import com.mars_sim.core.Simulation;
import com.mars_sim.core.environment.OrbitInfo;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.ClockUtils;
import com.mars_sim.core.time.MarsTime;
import com.mars_sim.core.time.MarsTimeFormat;
import com.mars_sim.core.time.MasterClock;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.MarsPanelBorder;
import com.mars_sim.ui.swing.StyleManager;
import com.mars_sim.ui.swing.tool.guide.GuideWindow;
import com.mars_sim.ui.swing.tool_window.ToolWindow;
import com.mars_sim.ui.swing.utils.AttributePanel;
import com.mars_sim.ui.swing.utils.SwingHelper;

/**
 * The TimeWindow is a tool window that displays the current Martian and Earth
 * time.
 */
public class TimeWindow extends ToolWindow {

	// Milliseconds between updates to date fields
	private static final long DATE_UPDATE_PERIOD = 300L;

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	public static final String NAME = "time";
	public static final String ICON = "time";
	public static final String TITLE = Msg.getString("TimeWindow.title");

	public static final int WIDTH = 300;
	public static final int HEIGHT = 590;
	
	/** Tool name. */
	public final String DESIRED = "x (Desired : ";
	public final String X_CLOSE_P = "x)";
	public final String AVERAGE = " (Average : ";
	public final String CLOSE_P = ")";

	public final String WIKI_URL = Msg.getString("TimeWindow.calendar.url"); //$NON-NLS-1$
	public final String WIKI_TEXT = Msg.getString("TimeWindow.calendar.title"); //$NON-NLS-1$	
	
//    private final DateTimeFormatter DATE_TIME_FORMATTER = DateCommand.DATE_TIME_FORMATTER;
	
	/** the execution time label string */
	private static final String EXEC = "Execution";
	/** the sleep time label string */
	private static final String SLEEP_TIME = "Sleep";
	/** the time pulse width label string */
	private static final String NEXT_PULSE_TIME = "Next Pulse Width";
	/** the pulse deviation label string */
	private static final String PULSE_DEVIATION = "Pulse Deviation";
	/** the optimal pulse label string */
	private static final String OPTIMAL = " (Optimal : ";
	/** the reference pulse label string */
	private static final String REFERENCE = " (Ref : ";
	
	/** the execution time unit */
	private static final String MS = " ms";
	/** the Universal Mean Time abbreviation */
	private static final String UMT = " (UMT) ";


	// Data members
	private String northernSeasonTip ="";
	private String northernSeasonCache = "";
	private String southernSeasonTip = "";
	private String southernSeasonCache = "";

	/** Martian calendar panel. */
	private MarsCalendarDisplay calendarDisplay;

	/** The tick spinner */
	private JSpinner cpuSpinner;
	/** label for Martian time. */
	private JLabel martianTimeLabel;
	/** label for Earth time. */
	private JLabel earthTimeLabel;
	/** label for areocentric longitude. */
	private JLabel lonLabel;
	/** label for Northern hemisphere season. */
	private JLabel northernSeasonLabel;
	/** label for Southern hemisphere season. */
	private JLabel southernSeasonLabel;
	/** label for uptimer. */
	private JLabel uptimeLabel;
	/** label for pulses per second label. */
	private JLabel ticksPerSecLabel;
	/** label for actual time ratio. */
	private JLabel actualTRLabel;
	/** label for pulse deviation percent. */
	private JLabel pulseDeviationLabel;
	/** label for execution time. */
	private JLabel execTimeLabel;
	/** label for sleep time. */
	private JLabel sleepTimeLabel;
	/** label for mars simulation time. */
	private JLabel marsPulseLabel;
	/** label for time compression. */
	private JLabel realTimeClockLabel;

	private JLabel monthLabel;

	private JLabel weeksolLabel;
	
	private OrbitInfo orbitInfo;
	
	/** Arial font. */
	private final Font arialFont = new Font("Arial", Font.PLAIN, 14);

	private long lastDateUpdate = 0;
	
	/**
	 * Constructs a TimeWindow object.
	 *
	 * @param desktop the desktop pane
	 */
	public TimeWindow(final MainDesktopPane desktop) {
		// Use TimeWindow constructor
		super(NAME, TITLE, desktop);
	
		// Set window resizable to false.
		setResizable(true);
		
		// Initialize data members
		Simulation sim = desktop.getSimulation();
		MasterClock masterClock = sim.getMasterClock();
		MarsTime marsTime = masterClock.getMarsTime();
		orbitInfo = sim.getOrbitInfo();
		
		// Get content pane
		JPanel mainPane = new JPanel(new BorderLayout());
		mainPane.setBorder(new MarsPanelBorder());
		setContentPane(mainPane);

//		// Create Earth time panel
//		JPanel earthTimePane = new JPanel(new BorderLayout());
//		mainPane.add(earthTimePane, BorderLayout.NORTH);
//		
//		// Create Earth time label
//		earthTimeLabel = new JLabel();
//		earthTimeLabel.setHorizontalAlignment(JLabel.CENTER);
//		earthTimeLabel.setVerticalAlignment(JLabel.CENTER);
////		earthTimeLabel.setFont(arialFont);
////		earthTimeLabel.setForeground(new Color(0, 69, 165));
//		earthTimeLabel.setText("");
//		earthTimeLabel.setToolTipText("Earth Timestamp in Greenwich Mean Time (GMT)");
//		earthTimePane.add(earthTimeLabel, BorderLayout.SOUTH);
//		earthTimePane.setBorder(StyleManager.createLabelBorder(Msg.getString("TimeWindow.earthTime")));

		// Create Martian time panel
		JPanel martianTimePane = new JPanel(new BorderLayout());
		mainPane.add(martianTimePane, BorderLayout.NORTH);
	
		// Create Martian time header label
		martianTimeLabel = new JLabel();
		martianTimeLabel.setHorizontalAlignment(JLabel.CENTER);
		martianTimeLabel.setVerticalAlignment(JLabel.CENTER);
//		martianTimeLabel.setFont(arialFont);
//		martianTimeLabel.setForeground(new Color(135, 100, 39));
		martianTimeLabel.setText("");
		martianTimeLabel.setToolTipText("Mars Timestamp in Universal Mean Time (UMT)");
		martianTimePane.add(martianTimeLabel, BorderLayout.CENTER);
		martianTimePane.setBorder(StyleManager.createLabelBorder(Msg.getString("TimeWindow.martianTime")));

		JButton wikiButton = new JButton(GuideWindow.wikiIcon);
		wikiButton.setAlignmentX(.5f);
		wikiButton.setAlignmentY(.5f);
		wikiButton.setToolTipText("Open Timekeeping Wiki in GitHub");
		wikiButton.addActionListener(e -> SwingHelper.openBrowser(WIKI_URL));

		JPanel linkPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 2));
		linkPane.add(wikiButton, SwingConstants.CENTER);
		martianTimePane.add(linkPane, BorderLayout.EAST);
		
		// Create Martian month panel
		JPanel martianMonthPane = new JPanel(new BorderLayout());//new FlowLayout(FlowLayout.CENTER));
		martianMonthPane.setBorder(StyleManager.createLabelBorder(Msg.getString("TimeWindow.martianMonth")));
		mainPane.add(martianMonthPane, BorderLayout.CENTER);
		
		// Create Martian calendar label panel
		AttributePanel labelPane = new AttributePanel(1, 2);
		labelPane.setAlignmentX(SwingConstants.CENTER);
		labelPane.setAlignmentY(SwingConstants.CENTER);
		martianMonthPane.add(labelPane, BorderLayout.NORTH);
		
		String mn = marsTime.getMonthName();
		monthLabel = labelPane.addTextField("Month", mn, null);
		
		String wd = MarsTimeFormat.getSolOfWeekName(marsTime);
		weeksolLabel = labelPane.addTextField("Weeksol", wd, null);

		// Create Martian calendar month panel
		JPanel calendarMonthPane = new JPanel(new BorderLayout());
		calendarMonthPane.setAlignmentX(SwingConstants.CENTER);
		calendarMonthPane.setAlignmentY(SwingConstants.CENTER);
//		calendarMonthPane.setPreferredSize(new Dimension(MarsCalendarDisplay.BOX_WIDTH, MarsCalendarDisplay.BOX_LENGTH + 5));
//		calendarMonthPane.setMaximumSize(new Dimension(MarsCalendarDisplay.BOX_WIDTH, MarsCalendarDisplay.BOX_LENGTH + 5));
//		calendarMonthPane.setMinimumSize(new Dimension(MarsCalendarDisplay.BOX_WIDTH, MarsCalendarDisplay.BOX_LENGTH + 5));
		martianMonthPane.add(calendarMonthPane, BorderLayout.CENTER);
		
		JPanel innerCalendarPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		innerCalendarPane.setAlignmentX(SwingConstants.CENTER);
		innerCalendarPane.setAlignmentY(SwingConstants.CENTER);

		// Create Martian calendar display
		calendarDisplay = new MarsCalendarDisplay(marsTime, desktop);
		innerCalendarPane.add(calendarDisplay);
		calendarMonthPane.add(innerCalendarPane, BorderLayout.CENTER);
		
		JPanel seasonPane = new JPanel(new BorderLayout());
		mainPane.add(seasonPane, BorderLayout.SOUTH);

		// Create Martian hemisphere panel
		AttributePanel hemiPane = new AttributePanel(3);
		seasonPane.add(hemiPane, BorderLayout.NORTH);		
		hemiPane.setBorder(StyleManager.createLabelBorder(Msg.getString("TimeWindow.martianSeasons")));

		String str =
				"<html>&#8201;Earth (days) vs Mars (sols)" +
				"<br>&#8201;Spring : 93 days vs 199 sols" +
				"<br>&#8201;Summer : 94 days vs 184 sols" +
				"<br>&#8201;Fall : 89 days vs 146 sols" +
				"<br>&#8201;Winter : 89 days vs 158 sols</html>";

		hemiPane.setToolTipText(str);

//		Note :
//		&#8201; Thin tab space
//		&#8194; En tab space
//		&#8195; Em tab space
		
		northernSeasonLabel = hemiPane.addTextField(Msg.getString("TimeWindow.northernHemisphere"),
													"", null);
		southernSeasonLabel = hemiPane.addTextField(Msg.getString("TimeWindow.southernHemisphere"),
													"", null);
		// Create areocentric longitude header label
		lonLabel = hemiPane.addTextField(Msg.getString("TimeWindow.areocentricLon"), "", null);
		lonLabel.setToolTipText("The Areocentric Longitude (L_s) of Mars with respect to the Sun");
		
		
		JPanel southPane = new JPanel(new BorderLayout());
		seasonPane.add(southPane, BorderLayout.SOUTH);

		// Create the tick spinner
		createCPUSpinner(masterClock);
		JPanel tickPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		tickPane.add(new JLabel("CPU Pulse : "));
		tickPane.add(cpuSpinner);
		southPane.add(tickPane, BorderLayout.NORTH);
		
		// Create param panel
		AttributePanel paramPane = new AttributePanel(9);
		paramPane.setBorder(StyleManager.createLabelBorder(Msg.getString("TimeWindow.simParam")));

		southPane.add(paramPane, BorderLayout.CENTER);

		ticksPerSecLabel = paramPane.addTextField(Msg.getString("TimeWindow.ticksPerSecond"), "", null);
		execTimeLabel = paramPane.addTextField(EXEC, "", null);
		sleepTimeLabel = paramPane.addTextField(SLEEP_TIME, "", null);
		marsPulseLabel = paramPane.addTextField(NEXT_PULSE_TIME, "", null);
		pulseDeviationLabel = paramPane.addTextField(PULSE_DEVIATION, "", null);
		actualTRLabel = paramPane.addTextField(Msg.getString("TimeWindow.actualTRHeader"), "",
									"Master clock's actual time ratio");
		realTimeClockLabel = paramPane.addTextField(Msg.getString("TimeWindow.rtc"), "", null);
		uptimeLabel = paramPane.addTextField(Msg.getString("TimeWindow.simUptime"), "", null);
	
		// Pack window
		pack();

		setPreferredSize(new Dimension(WIDTH, HEIGHT));

		// Update the two time labels
		updateFastLabels(masterClock);
		updateDateLabels(masterClock);
		updateTimeLabels(masterClock);
		
		// Update season labels
		updateSeason();
	}

	/**
	 * Creates the CPU spinner.
	 * 
	 * @param masterClock
	 */
	private void createCPUSpinner(MasterClock masterClock) {
		// Initializes one more time after  
		// clockExecutor.getActiveCount() is stabilized
		masterClock.initReferencePulse();
		
		double cpuValue = masterClock.getCpuFactor();
	
		double min = Math.round(cpuValue / 5 * 10.0)/10.0;
		double max = Math.round(5 * cpuValue * 10.0)/10.0;
		
		SpinnerNumberModel spinnerModel = new SpinnerNumberModel(cpuValue, min, max, cpuValue/20);

		spinnerModel.setValue(cpuValue);
		
		cpuSpinner = new JSpinner(spinnerModel);	
		// 1. Get the editor component of your spinner:
		Component spinnerEditor = cpuSpinner.getEditor();
		// 2. Get the text field of your spinner's editor:
		JFormattedTextField jftf = ((JSpinner.DefaultEditor) spinnerEditor).getTextField();
		// 3. Set a default size to the text field:
		jftf.setColumns(2);
	
		jftf.setHorizontalAlignment(JTextField.LEFT);
		
		cpuSpinner.setToolTipText("Manually adjust the CPU Utilization associating with # of threads used");
		
		cpuSpinner.addChangeListener(e -> {
			double cpuTicks = ((Number)spinnerModel.getValue()).doubleValue();
			masterClock.setCPUFactor(cpuTicks);
		});
	}
	
	
	/**
	 * Updates various time labels.
	 * 
	 * @param masterClock
	 */
	private void updateTimeLabels(MasterClock masterClock) {
		double cpuCache = masterClock.getCpuFactor();
		SpinnerNumberModel spinnerModel = (SpinnerNumberModel)(cpuSpinner.getModel());
		
		double cpuValue = ((Number)spinnerModel.getValue()).doubleValue();
		if (cpuValue != cpuCache) {
		
			double cpu = masterClock.getCpuFactor();
			double min = Math.round(cpu / 5 * 10.0)/10.0;
			double max = Math.round(5 * cpu * 10.0)/10.0;
			
			spinnerModel.setValue(cpu);
			spinnerModel.setMinimum(min);
			spinnerModel.setMaximum(max);
			spinnerModel.setStepSize(cpu/20);
		}
		
		// Update execution time label
		long execTime = masterClock.getExecutionTime();
		execTimeLabel.setText(execTime + MS);

		// Update sleep time label
		long sleepTime = masterClock.getSleepTime();
		sleepTimeLabel.setText(sleepTime + MS);

		// Update pulse width label
		double nextPulse = masterClock.getNextPulseTime();
		double optPulse = masterClock.getOptPulseTime();
		double refPulse = masterClock.getReferencePulse();
		
		StringBuilder pulseText = new StringBuilder();
		pulseText.append(StyleManager.DECIMAL_PLACES4.format(nextPulse))
			  .append(REFERENCE)
			  .append(StyleManager.DECIMAL_PLACES4.format(refPulse))
			  .append(CLOSE_P);
		marsPulseLabel.setText(pulseText.toString());

		// Update pulse deviation label
		double percent = masterClock.getNextPulseDeviation() * 100;
		StringBuilder pulseDevText = new StringBuilder();
		pulseDevText.append(StyleManager.DECIMAL1_PERC.format(percent))
			  .append(OPTIMAL)
			  .append(StyleManager.DECIMAL_PLACES4.format(optPulse))
			  .append(CLOSE_P);
		pulseDeviationLabel.setText(pulseDevText.toString());
		
		// Update actual TR label
		StringBuilder trText = new StringBuilder();
		trText.append(StyleManager.DECIMAL_PLACES1.format(masterClock.getActualTR()))
			  .append(DESIRED)
			  .append(masterClock.getDesiredTR())
			  .append(X_CLOSE_P);
		actualTRLabel.setText(trText.toString());

		// Update real time clock (RTC) or time compression label
		realTimeClockLabel.setText(ClockUtils.getRTCString(masterClock.getActualTR()));
	}

	/**
	 * Sets and updates the season labels.
	 */
	private void updateSeason() {
		String northernSeason = orbitInfo.getSeason(OrbitInfo.NORTHERN_HEMISPHERE);
		String southernSeason = orbitInfo.getSeason(OrbitInfo.SOUTHERN_HEMISPHERE);
	
		if (!northernSeasonCache.equals(northernSeason)) {
			northernSeasonCache = northernSeason;

			if (orbitInfo.getSeason(OrbitInfo.NORTHERN_HEMISPHERE) != null) {
				northernSeasonLabel.setText(northernSeason);
			}

			northernSeasonTip = getSeasonTip(northernSeason);
			northernSeasonLabel.setToolTipText(northernSeasonTip);
		}

		if (!southernSeasonCache.equals(southernSeason)) {
			southernSeasonCache = southernSeason;

			if (orbitInfo.getSeason(OrbitInfo.SOUTHERN_HEMISPHERE) != null) {
				southernSeasonLabel.setText(southernSeason);
			}

			southernSeasonTip = getSeasonTip(southernSeason);
			southernSeasonLabel.setToolTipText(southernSeasonTip);
		}
	}

	/**
	 * Gets the text for the season label tooltip.
	 *
	 * @param hemi the northern or southern hemisphere
	 */
	private static String getSeasonTip(String hemi) {
		if (hemi.contains("Spring"))
			return Msg.getString("TimeWindow.season.spring");
		else if (hemi.contains("Summer"))
			return Msg.getString("TimeWindow.season.summer");
		else if (hemi.contains("Autumn"))
			return Msg.getString("TimeWindow.season.autumn");
		else if (hemi.contains("Winter"))
			return Msg.getString("TimeWindow.season.winter");
		else
			return null;
	}

	/**
	 * Updates the calendar, the areocentric longitude and the time labels via ui pulse.
	 * 
	 * @param mc
	 */
	private void updateDateLabels(MasterClock mc) {
		
		String mn = mc.getMarsTime().getMonthName();
		monthLabel.setText(mn);
		
		String wd = MarsTimeFormat.getSolOfWeekName(mc.getMarsTime());
		weeksolLabel.setText(wd);
		
		// Update the calender
		calendarDisplay.update(mc.getMarsTime());
		// Update areocentric longitude
		lonLabel.setText(Math.round(orbitInfo.getSunAreoLongitude() * 10_000.0)/10_000.0 + "");	
		
		// Update season
		if (mc.getClockPulse().isNewSol()) {
			updateSeason();
		}
	}

	/**
	 * Updates date and time in Time Tool via clock pulse.
	 * 
	 * @param mc
	 */
	private void updateFastLabels(MasterClock mc) {
		MarsTime mTime = mc.getMarsTime();
		String ts = mTime.getDateTimeStamp() + " " + MarsTimeFormat.getSolOfWeekName(mTime) + UMT;
		martianTimeLabel.setText(ts);

//		ts = mc.getEarthTime().format(DATE_TIME_FORMATTER);
//		earthTimeLabel.setText(ts);

		// Update average TPS label
		double ave = mc.getAveragePulsesPerSecond();
		StringBuilder tpsText = new StringBuilder();
		tpsText.append(StyleManager.DECIMAL_PLACES2.format(mc.getCurrentPulsesPerSecond()))
			  .append(AVERAGE)
			  .append(StyleManager.DECIMAL_PLACES2.format(ave))
			  .append(CLOSE_P);
		
		ticksPerSecLabel.setText(tpsText.toString());

		uptimeLabel.setText(mc.getUpTimer().getUptime());
	}

	@Override
	public void update(ClockPulse pulse) {
		if (desktop.isToolWindowOpen(TimeWindow.NAME)) {
			MasterClock masterClock = pulse.getMasterClock();

			// update the fast labels
			updateFastLabels(masterClock);

			long currentTime = System.currentTimeMillis();
			if ((currentTime - lastDateUpdate) > DATE_UPDATE_PERIOD) {
				// update the slow labels
				updateDateLabels(masterClock);
				updateTimeLabels(masterClock);
				lastDateUpdate = currentTime;
			}
		}
	}
}
