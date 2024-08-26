/*
 * Mars Simulation Project
 * MainDesktopPane.java
 * @date 2021-08-28
 * @author Scott Davis
 */
package com.mars_sim.ui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

import com.mars_sim.core.Entity;
import com.mars_sim.core.GameManager.GameMode;
import com.mars_sim.core.interplanetary.transport.Transportable;
import com.mars_sim.core.person.ai.mission.Mission;
import com.mars_sim.core.science.ScientificStudy;
import com.mars_sim.core.Simulation;
import com.mars_sim.core.Unit;
import com.mars_sim.core.UnitManager;
import com.mars_sim.core.UnitManagerEvent;
import com.mars_sim.core.UnitManagerListener;
import com.mars_sim.core.UnitType;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.time.ClockListener;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.mapdata.location.Coordinates;
import com.mars_sim.tools.util.RandomUtil;
import com.mars_sim.ui.swing.UIConfig.WindowSpec;
import com.mars_sim.ui.swing.astroarts.OrbitViewer;
import com.mars_sim.ui.swing.sound.AudioPlayer;
import com.mars_sim.ui.swing.tool.commander.CommanderWindow;
import com.mars_sim.ui.swing.tool.guide.GuideWindow;
import com.mars_sim.ui.swing.tool.mission.MissionWindow;
import com.mars_sim.ui.swing.tool.monitor.MonitorWindow;
import com.mars_sim.ui.swing.tool.monitor.UnitTableModel;
import com.mars_sim.ui.swing.tool.navigator.NavigatorWindow;
import com.mars_sim.ui.swing.tool.resupply.ResupplyWindow;
import com.mars_sim.ui.swing.tool.science.ScienceWindow;
import com.mars_sim.ui.swing.tool.search.SearchWindow;
import com.mars_sim.ui.swing.tool.settlement.SettlementWindow;
import com.mars_sim.ui.swing.tool.time.TimeWindow;
import com.mars_sim.ui.swing.tool_window.ToolWindow;
import com.mars_sim.ui.swing.unit_display_info.UnitDisplayInfoFactory;
import com.mars_sim.ui.swing.unit_window.UnitWindow;
import com.mars_sim.ui.swing.unit_window.UnitWindowFactory;
import com.mars_sim.ui.swing.unit_window.UnitWindowListener;

/**
 * The MainDesktopPane class is the desktop part of the project's UI. It
 * contains all tool and unit windows, and is itself contained, along with the
 * tool bars, by the main window.
 */
@SuppressWarnings("serial")
public class MainDesktopPane extends JDesktopPane
		implements ClockListener, ComponentListener, UnitManagerListener {

	// Properties for UIConfig settings
	private static final String DESKTOP_PROPS = "desktop";
	private static final String PRELOAD_TOOLS = "preload_tools";

	/** default logger. */
	private static Logger logger = Logger.getLogger(MainDesktopPane.class.getName());

	/** The sound player. */
	private static AudioPlayer soundPlayer;

	/** The game mode of this simulation session. */
	public GameMode mode;
	/** List of open or buttoned unit windows. */
	private Collection<UnitWindow> unitWindows;
	/** List of tool windows. */
	private Collection<ToolWindow> toolWindows;
	/** ImageIcon that contains the tiled background. */
	private ImageIcon backgroundImageIcon;
	/** Label that contains the tiled background. */
	private JLabel backgroundLabel;
	/** The image icon of the tiled background. */
	private Image baseImageIcon = ImageLoader.getImage("background");

	/** The desktop popup announcement window. */
	private AnnouncementWindow announcementWindow;

	private MainWindow mainWindow;
	
	// Preload the Tool windows
	private boolean preloadTools = true;

	// Simulation reference used by the UI windows
	private Simulation sim;

	/**
	 * Constructor 1.
	 *
	 * @param mainWindow the main outer window
	 */
	public MainDesktopPane(MainWindow mainWindow, Simulation sim) {
		super();

		this.mainWindow = mainWindow;
		this.sim = sim;

		// Initialize data members
		if (!AudioPlayer.isAudioDisabled()) {
			soundPlayer = new AudioPlayer(this);
			soundPlayer.playRandomMusicTrack();
		}
		// Prepare unit windows.
		unitWindows = new ArrayList<>();

		// Prepare tool windows. Needs to be thread safe as windows are used by clock pulse
		toolWindows = new CopyOnWriteArrayList<>();

		prepareListeners();

		init();
	}

	private void init() {
		// Set background color to black
		setBackground(Color.black);
		// set desktop manager
		setDesktopManager(new MainDesktopManager());
		// Set component listener
		addComponentListener(this);
		// Create background label and set it to the back layer
		backgroundImageIcon = new ImageIcon();
		// Set up background
		backgroundLabel = new JLabel(backgroundImageIcon);
		// Add background
		add(backgroundLabel, Integer.MIN_VALUE);
		// Set location of background
		backgroundLabel.setLocation(0, 0);
		// Push the background to the back
		moveToBack(backgroundLabel);

		Properties props = mainWindow.getConfig().getPropSet(DESKTOP_PROPS);
		preloadTools = UIConfig.extractBoolean(props, PRELOAD_TOOLS, false);
		if (preloadTools) {
			// Prep tool windows
			prepareToolWindows();
		}
		
		// Prep listeners
		prepareListeners();
		// Setup announcement window
		prepareAnnouncementWindow();

		// Set background paper size
		Dimension selectedSize = mainWindow.getSelectedSize();
		if (selectedSize != null) {
			int w = selectedSize.width;
			int h = selectedSize.height;
			setSize(new Dimension(w, h));
			setPreferredSize(new Dimension(w, h));
			logger.config("Main Window initially set to "
					+ w
					+ " x "
					+ h
					+ ".");
		}
	}

	/**
	 * Creates background tile when MainDesktopPane is first displayed. Center
	 * logoLabel on MainWindow and set backgroundLabel to the size of
	 * MainDesktopPane.
	 *
	 * @param e the component event
	 */
	@Override
	public void componentResized(ComponentEvent e) {

		Dimension screenSize = getSize();

		if (screenSize == null || screenSize.getWidth() == 0 || screenSize.getHeight() == 0) {
			screenSize = Toolkit.getDefaultToolkit().getScreenSize();
			logger.config("Toolkit default screen size is " + screenSize.getWidth() + " x " + screenSize.getHeight());
		}

		Image backgroundImage = createImage((int) screenSize.getWidth(), (int) screenSize.getHeight());
		if (backgroundImage != null) {
			// Not loaded when the window is first building
			Graphics backgroundGraphics = backgroundImage.getGraphics();

			int sourceWidth = baseImageIcon.getWidth(this);
			int sourceHeight = baseImageIcon.getHeight(this);
			int targetWidth = backgroundImage.getWidth(this);
			int targetHeight = backgroundImage.getHeight(this);
			for (int x = 0; x < targetWidth; x += sourceWidth) {
				for (int y = 0; y < targetHeight; y += sourceHeight) {
					backgroundGraphics.drawImage(baseImageIcon, x, y, this);
				}
			}

			backgroundImageIcon.setImage(backgroundImage);
			// Set the backgroundLabel size to the size of the desktop
			backgroundLabel.setSize(getSize());
		}
	}

	// Additional Component Listener methods implemented but not used.
	@Override
	public void componentMoved(ComponentEvent e) {
		updateToolWindow();
	}

	@Override
	public void componentShown(ComponentEvent e) {
	}

	@Override
	public void componentHidden(ComponentEvent e) {
	}

	public void updateToolWindow() {
		JInternalFrame[] frames = (JInternalFrame[]) this.getAllFrames();
		for (JInternalFrame f : frames) {
			f.updateUI();
		}
	}

	@Override
	public Component add(Component comp) {
		super.add(comp);
		centerJIF(comp);
		return comp;
	}

	private void centerJIF(Component comp) {
		Dimension desktopSize = getSize();
		Dimension jInternalFrameSize = comp.getSize();
		int width = (desktopSize.width - jInternalFrameSize.width) / 2;
		int height = (desktopSize.height - jInternalFrameSize.height) / 2;
		comp.setLocation(width, height);
		comp.setVisible(true);
	}

	@Override
	public void unitManagerUpdate(UnitManagerEvent event) {
		Object unit = event.getUnit();
		if (unit instanceof Settlement) {

			SwingUtilities.invokeLater(() -> updateToolWindow());
		}
	}

	/**
	 * Sets up this class with two listeners.
	 */
	private void prepareListeners() {
		// Attach UnitManagerListener to desktop
		UnitManager unitManager = sim.getUnitManager();
		unitManager.addUnitManagerListener(UnitType.SETTLEMENT, this);
	}

	/**
	 * Returns the MainWindow instance.
	 *
	 * @return MainWindow instance
	 */
	public MainWindow getMainWindow() {
		return mainWindow;
	}

	/**
	 * Creates tool windows.
	 */
	private void prepareToolWindows() {
		getToolWindow(CommanderWindow.NAME, true);
		getToolWindow(NavigatorWindow.NAME, true);
		getToolWindow(SearchWindow.NAME, true);
		getToolWindow(TimeWindow.NAME, true);
		getToolWindow(SettlementWindow.NAME, true);
		getToolWindow(ScienceWindow.NAME, true);
		getToolWindow(MonitorWindow.NAME, true);
		getToolWindow(MissionWindow.NAME, true);
		getToolWindow(ResupplyWindow.NAME, true);
	}

	/**
	 * Creates announcement windows & transportWizard
	 */
	private void prepareAnnouncementWindow() {
		// Prepare announcementWindow.
		announcementWindow = new AnnouncementWindow(this);
		try {
			announcementWindow.setClosed(true);
		} catch (java.beans.PropertyVetoException e) {
			logger.severe("Announcement Window not ready: " + e.getMessage());
		}
	}

	/**
	 * Returns a tool window for a given tool name.
	 *
	 * @param toolName      the name of the tool window
	 * @param createWinddow Create a window if it doesn't exist
	 * @return the tool window
	 */
	private ToolWindow getToolWindow(String toolName, boolean createWindow) {
		for (ToolWindow w : toolWindows) {
			if (toolName.equals(w.getToolName()))
				return w;
		}

		if (createWindow) {
			ToolWindow w = null;
			if (toolName.equals(CommanderWindow.NAME)) {
				w = new CommanderWindow(this);
			} else if (toolName.equals(NavigatorWindow.NAME)) {
				w = new NavigatorWindow(this);
			} else if (toolName.equals(SearchWindow.NAME)) {
				w = new SearchWindow(this);
			} else if (toolName.equals(TimeWindow.NAME)) {
				w = new TimeWindow(this);
			} else if (toolName.equals(SettlementWindow.NAME)) {
				w = new SettlementWindow(this);
			} else if (toolName.equals(ScienceWindow.NAME)) {
				w = new ScienceWindow(this);
			} else if (toolName.equals(GuideWindow.NAME)) {
				w = new GuideWindow(this);
			} else if (toolName.equals(MonitorWindow.NAME)) {
				w = new MonitorWindow(this);
			} else if (toolName.equals(MissionWindow.NAME)) {
				w = new MissionWindow(this);
			} else if (toolName.equals(ResupplyWindow.NAME)) {
				w = new ResupplyWindow(this);
			} else if (toolName.equals(OrbitViewer.NAME)) {
				w = new OrbitViewer(this);
			} else {
				return null;
			}

			// Close it from the start
			try {
				w.setClosed(true);
			} catch (PropertyVetoException e) {
				logger.warning("Problem loading tool window " + e.getMessage());
			}
			toolWindows.add(w);
			return w;
		}
		return null;
	}

	/**
	 * Displays a new Unit model in the monitor window.
	 *
	 * @param model the new model to display
	 */
	public void addModel(UnitTableModel<?> model) {
		((MonitorWindow) openToolWindow(MonitorWindow.NAME)).displayModel(model);
	}

	/**
	 * Centers the map and the globe on given coordinates. Also opens the map tool
	 * if it's closed.
	 *
	 * @param targetLocation the new center location
	 */
	public void centerMapGlobe(Coordinates targetLocation) {
		((NavigatorWindow) openToolWindow(NavigatorWindow.NAME)).updateCoordsMaps(targetLocation);
	}

	/**
	 * Returns true if tool window is open.
	 *
	 * @param toolName the name of the tool window
	 * @return true true if tool window is open
	 */
	public boolean isToolWindowOpen(String toolName) {
		ToolWindow w = getToolWindow(toolName, false);
		if (w != null)
			return !w.isClosed();
		return false;
	}

	/**
	 * Opens a tool window if necessary.
	 *
	 * @param toolName the name of the tool window
	 */
	public ToolWindow openToolWindow(String toolName) {
		ToolWindow window = getToolWindow(toolName, true);

		if (window == null) {
			return null;
		}

		if (window.isClosed() && !window.wasOpened()) {
			UIConfig config = mainWindow.getConfig();
			Point location = null;
			WindowSpec previousDetails = config.getInternalWindowDetails(toolName);
			if (previousDetails != null) {
				location = previousDetails.position();
				if (window.isResizable()) {
					window.setSize(previousDetails.size());
				}
			} else if (toolName.equals(TimeWindow.NAME))
				location = computeLocation(window, 0, 2);
			else if (toolName.equals(MonitorWindow.NAME))
				location = computeLocation(window, 1, 0);
			else
				location = getCenterLocation(window);

			// Check is visible
			Dimension currentSize = getSize();
			location = new Point(Math.max(1, Math.min(location.x, (int)currentSize.getWidth() - 20)),
								 Math.max(1, Math.min(location.y, (int)currentSize.getHeight() - 20)));
			window.setLocation(location);
			window.setWasOpened(true);
		}

		// in case of classic swing mode for MainWindow
		add(window, 0);

		try {
			window.setClosed(false);
		} catch (Exception e) {
			logger.log(Level.SEVERE, e.toString());
		}

		window.show();

		// bring to front if it overlaps with other windows
		try {
			window.setSelected(true);
		} catch (PropertyVetoException e) {
			// ignore if setSelected is vetoed
		}

		window.getContentPane().validate();
		window.getContentPane().repaint();

		validate();
		repaint();

		return window;
	}

	/**
	 * Closes a tool window if it is open.
	 *
	 * @param toolName the name of the tool window
	 */
	public void closeToolWindow(String toolName) {
		SwingUtilities.invokeLater(() -> {
			ToolWindow window = getToolWindow(toolName, false);
			if ((window != null) && !window.isClosed()) {
				try {
					window.setClosed(true);
				} catch (java.beans.PropertyVetoException e) {
					// ignore
				}
			}
		});
	}

	/**
	 * Creates and opens a window for an Entity if it isn't already in existence and
	 * open. This selects the most appropriate tool window.
	 *
	 * @param entity Entity to display
	 */
    public void showDetails(Entity entity) {
		if (entity instanceof Unit u) {
			openUnitWindow(u, null);
		}
		else if (entity instanceof Mission m) {
			((MissionWindow)openToolWindow(MissionWindow.NAME)).openMission(m);
		}
		else if (entity instanceof Transportable t) {
			((ResupplyWindow)openToolWindow(ResupplyWindow.NAME)).openTransportable(t);
		}
		else if (entity instanceof ScientificStudy s) {
			((ScienceWindow)openToolWindow(ScienceWindow.NAME)).setScientificStudy(s);
		}
    }

	/**
	 * Opens a Unit Window for a specific Unit with a optional set of user properties.
	 * 
	 * @param unit Unit to display
	 * @param initProps Initial properties
	 */
	private void openUnitWindow(Unit unit, WindowSpec initProps) {

		UnitWindow tempWindow = null;

		// See if the window of this unit has already been opened
		for (UnitWindow window : unitWindows) {
			if (window.getUnit() == unit) {
				tempWindow = window;
			}
		}

		if (tempWindow != null) {
			if (tempWindow.isClosed()) {
				add(tempWindow, 0);
			}
		}

		else {
			// Create new window for unit.
			tempWindow = UnitWindowFactory.getUnitWindow(unit, this);
			
			add(tempWindow, 0);
			tempWindow.pack();

			// Set internal frame listener
			tempWindow.addInternalFrameListener(new UnitWindowListener(this));

			Point newPosition = null;
			if (initProps != null) {
				tempWindow.setUIProps(initProps.props());
				newPosition = initProps.position();
			}
			if (newPosition == null) {
				newPosition = getRandomLocation(tempWindow);
			}
			// Put window in random position on desktop.
			tempWindow.setLocation(newPosition);

			// Add unit window to unit windows
			unitWindows.add(tempWindow);

			// Create new unit button in tool bar if necessary
			if (mainWindow != null)
				mainWindow.createUnitButton(unit);
		}

		tempWindow.setVisible(true);

		// Correct window becomes selected
		try {
			tempWindow.setSelected(true);
			tempWindow.moveToFront();
		} catch (java.beans.PropertyVetoException e) {
		}

		// Play sound
		String soundFilePath = UnitDisplayInfoFactory.getUnitDisplayInfo(unit).getSound(unit);
		if (soundFilePath != null && soundFilePath.length() != 0 && soundPlayer != null) {
			soundPlayer.playSound(soundFilePath);
		}
	}

	/**
	 * Disposes a unit window and button.
	 *
	 * @param window the unit window to dispose.
	 */
	public void disposeUnitWindow(UnitWindow window) {

		if (window != null) {
			window.setVisible(false);

			unitWindows.remove(window);
			window.dispose();

			// Have main window dispose of unit button
			if (mainWindow != null)
				mainWindow.disposeUnitButton(window.getUnit());

			// Lastly destory the window
			window.destroy();
		}
	}

	/**
	 * Update the desktop and all of its windows.
	 */
	private void updateWindows(ClockPulse pulse) {
		// Update all unit windows.
		for (UnitWindow u : unitWindows) {
			if (u.isVisible() || u.isShowing())
				u.update();
		}

		// Update all tool windows.
		for (ToolWindow w : toolWindows) {
			if (w.isVisible() || w.isShowing())
				w.update(pulse);
		}
	}

	private Point getCenterLocation(JInternalFrame tempWindow) {

		Dimension desktop_size = getSize();
		Dimension window_size = tempWindow.getSize();

		int rX = (int) Math.round((desktop_size.width - window_size.width) / 2D);
		int rY = (int) Math.round((desktop_size.height - window_size.height - 100) / 2D);

		// Added rX checking
		if (rX < 0) {
			rX = 0;
		}

		// Make sure y position isn't < 0.
		if (rY < 0) {
			rY = 0;
		}

		return new Point(rX, rY);
	}

	/**
	 * Gets a random location on the desktop for a given {@link JInternalFrame}.
	 *
	 * @param tempWindow an internal window
	 * @return random point on the desktop
	 */
	private Point getRandomLocation(JInternalFrame tempWindow) {

		Dimension desktop_size = getSize();
		Dimension window_size = tempWindow.getSize();

		// Populate windows in grid=like starting position
		int w = desktop_size.width - window_size.width;
		int rX = RandomUtil.getRandomInt(w / 20) * 20;

		int rY = RandomUtil.getRandomInt(5) * 20;

		return new Point(rX, rY);
	}

	/**
	 * Gets a particular location on the desktop for a given {@link JInternalFrame}.
	 *
	 * @param f
	 * @param position
	 * @return a specific point on the desktop
	 */
	private Point computeLocation(JInternalFrame f, int positionX, int positionY) {
		Dimension desktop_size = getSize();
		Dimension window_size = f.getSize();

		// Populate windows in grid=like starting position
		int w = desktop_size.width - window_size.width;
		int h = desktop_size.height - window_size.height;
		int rX = 0;
		int rY = 0;

		if (positionX == 0)
			rX = 0;
		else if (positionX == 1)
			rX = w / 2;
		else if (positionX == 2)
			rX = w;

		if (positionY == 0)
			rY = 0;
		else if (positionY == 1)
			rY = h / 2;
		else if (positionY == 2)
			rY = h;

		return new Point(rX, rY);
	}

	/**
	 * Gets the sound player used by the desktop.
	 *
	 * @return sound player.
	 */
	public AudioPlayer getSoundPlayer() {
		return soundPlayer;
	}

	/**
	 * Opens a popup announcement window on the desktop.
	 *
	 * @param announcement the announcement text to display.
	 */
	public void openAnnouncementWindow(String announcement) {
		announcementWindow.setAnnouncement(announcement);
		announcementWindow.setSize(new Dimension(200, 100));
		announcementWindow.pack();
		add(announcementWindow, 0);
		int Xloc = (int) ((getWidth() - announcementWindow.getWidth()) * .5D);
		int Yloc = (int) ((getHeight() - announcementWindow.getHeight()) * .15D);
		announcementWindow.setLocation(Xloc, Yloc);
		// Note: second window packing seems necessary to get window
		// to display components correctly.
		announcementWindow.pack();
		announcementWindow.setVisible(true);
		validate();
		repaint();
	}

	/**
	 * Removes the popup announcement window from the desktop.
	 */
	public void disposeAnnouncementWindow() {
		announcementWindow.dispose();
	}

	/**
	 * Opens all initial windows based on UI configuration.
	 */
	public void openInitialWindows() {
		List<WindowSpec> startingWindows = mainWindow.getConfig().getConfiguredWindows();

		if (!startingWindows.isEmpty()) {
			UnitManager uMgr = mainWindow.getDesktop().getSimulation().getUnitManager();
			for(WindowSpec w : startingWindows) {
				switch(w.type()) {
					case UIConfig.TOOL:
						openToolWindow(w.name());
					break;

					case UIConfig.UNIT:
						Unit u = UnitWindow.getUnit(uMgr, w.props());
						if (u != null) {
							openUnitWindow(u, w);
						}
					break;
				}
 			}
		}
		else {
			if (mode == GameMode.COMMAND) {
				// Open the time window for the Commander Mode
				openToolWindow(TimeWindow.NAME);
				openToolWindow(CommanderWindow.NAME);
			}

			else {
				openToolWindow(MonitorWindow.NAME);
			}
		}
	}

	/**
	 * Creates a standardized empty border.
	 */
	public static EmptyBorder newEmptyBorder() {
		return new EmptyBorder(1, 1, 1, 1);
	}

	public AnnouncementWindow getAnnouncementWindow() {
		return announcementWindow;
	}

	/**
	 * Gets a reference to the Simulation being displayed.
	 * 
	 * @return
	 */
	public Simulation getSimulation() {
		return sim;
	}

	@Override
	public void clockPulse(ClockPulse pulse) {
		if (!mainWindow.isIconified()) {
			// Why not pass the pulse??
			updateWindows(pulse);
		}
	}

	@Override
	public void pauseChange(boolean isPaused, boolean showPane) {
	}

	/**
	 * Gets UI properties of the Desktop.
	 */
	public Map<String, Properties> getUIProps() {
		Map<String, Properties> result = new HashMap<>();

		if (soundPlayer != null) {
			result.put(AudioPlayer.PROPS_NAME, soundPlayer.getUIProps());
		}

		// Add desktop properties
		Properties desktopProps = new Properties();
		desktopProps.setProperty(PRELOAD_TOOLS, Boolean.toString(preloadTools));
		result.put(DESKTOP_PROPS, desktopProps);
		return result;
    }

	/**
	 * Prepares the panel for deletion.
	 */
	public void destroy() {

		mode = null;
		if (unitWindows != null) {
			for (UnitWindow u : unitWindows) {
				u.destroy();
				u = null;
			}
			unitWindows = null;
		}
		if (toolWindows != null) {
			for (ToolWindow w : toolWindows) {
				w.destroy();
				w = null;
			}
			toolWindows = null;
		}
		backgroundImageIcon = null;
		backgroundLabel = null;
		soundPlayer = null;
		announcementWindow = null;
		mainWindow = null;
	}

}
