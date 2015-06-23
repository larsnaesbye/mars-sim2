/**
 * Mars Simulation Project
 * GlobeDisplay.java
 * @version 3.08 2015-06-17

 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.navigator;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.MemoryImageSource;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JComponent;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.IntPoint;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.mars.Mars;
import org.mars_sim.msp.core.mars.SurfaceFeatures;
import org.mars_sim.msp.ui.swing.ImageLoader;

import org.mars_sim.msp.ui.swing.unit_display_info.UnitDisplayInfo;
import org.mars_sim.msp.ui.swing.unit_display_info.UnitDisplayInfoFactory;

/**
 * The Globe Display class displays a graphical globe of Mars in the Navigator
 * tool.
 */
class GlobeDisplay
extends JComponent
implements Runnable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static Logger logger = Logger.getLogger(GlobeDisplay.class.getName());

	private static final double HALF_PI = Math.PI / 2d;
	private static int dragx, dragy;

	// Data members
	/** Real surface sphere object. */
	private MarsGlobe marsSphere;
	/** Topographical sphere object. */
	private MarsGlobe topoSphere;
	/** Spherical coordinates for globe center. */
	private Coordinates centerCoords;
	/** Refresh thread. */
	private Thread showThread;
	/** <code>true</code> if in topographical mode, false if in real surface mode. */
	private boolean topo;
	/** <code>true</code> if globe needs to be regenerated */
	private boolean recreate;
	/** width of the globe display component. */
	private int width;
	/** height of the globe display component. */
	private int height;
	/** <code>true</code> if USGS surface map is to be used. */
	private boolean useUSGSMap;
	/** Array used to generate day/night shading image. */
	private int[] shadingArray;
	/** <code>true</code> if day/night shading is to be used. */
	private boolean showDayNightShading;
	/** <code>true</code> if globe should be updated. */
	private boolean update;
	/** <code>true</code> if refresh thread should continue. */
	private boolean keepRunning;

	/** stores the internationalized string for reuse in {@link #drawCrossHair(Graphics)}. */
	private String longitude = Msg.getString("direction.longitude"); //$NON-NLS-1$
	/** stores the internationalized string for reuse in {@link #drawCrossHair(Graphics)}. */
	private String latitude = Msg.getString("direction.latitude"); //$NON-NLS-1$
	/** stores the font for drawing lon/lat strings in {@link #drawCrossHair(Graphics)}. */
	private Font positionFont = new Font("Helvetica", Font.PLAIN, 10);
	/** measures the pixels needed to display text. */
	private FontMetrics positionMetrics = getFontMetrics(positionFont);
	/** stores the position for drawing lon/lat strings in {@link #drawCrossHair(Graphics)}. */
	int leftWidth = positionMetrics.stringWidth(latitude);
	/** stores the position for drawing lon/lat strings in {@link #drawCrossHair(Graphics)}. */
	int rightWidth = positionMetrics.stringWidth(longitude);

	private Mars mars;

	private SurfaceFeatures surfaceFeatures;
	/**
	 * Constructor.
	 * @param navwin the navigator window.
	 * @param width the width of the globe display
	 * @param height the height of the globe display
	 */
	public GlobeDisplay(final NavigatorWindow navwin, int width, int height) {

		// Initialize data members
		this.width = width;
		this.height = height;

		// Set component size
		setPreferredSize(new Dimension(width, height));
		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());

		// Construct sphere objects for both real and topographical modes
		marsSphere = new MarsGlobe(MarsGlobeType.SURFACE, this);
		topoSphere = new MarsGlobe(MarsGlobeType.TOPO, this);

		// Initialize global variables
		centerCoords = new Coordinates(HALF_PI, 0D);
		update = true;
		topo = false;
		recreate = true;
		useUSGSMap = false;
		shadingArray = new int[width * height];
		showDayNightShading = false;

		addMouseMotionListener(new MouseAdapter() {
			int lastx, lasty;

			@Override
			public void mouseDragged(MouseEvent e) {
				int difx, dify, x = e.getX(), y = e.getY();

				if (y > dragy) {
					if (y < lasty) {
						dragy = y;
					}
				} else {
					if (y > lasty) {
						dragy = y;
					}
				}

				if (x > dragx) {
					if (x < lastx) {
						dragx = x;
					}
				} else {
					if (x > lastx) {
						dragx = x;
					}
				}
				dify = dragy - y;
				difx = dragx - x;
				lastx = x;
				lasty = y;
				/*
				 * System.out.println("GlobeDisplay mouseDragged difx = "+difx);
				 * System.out.println("GlobeDisplay mouseDragged dify = "+dify);
				 * System.out.println("GlobeDisplay mouseDragged dragx = "+dragx);
				 * System.out.println("GlobeDisplay mouseDragged dragy = "+dragy);
				 * System.out.println("GlobeDisplay mouseDragged X = "+e.getX());
				 * System.out.println("GlobeDisplay mouseDragged Y = "+e.getY());
				 */
				if (dragx != 0 && dragy != 0) {
					double newPhi = centerCoords.getPhi() + ((double) dify * .0025D % (Math.PI));
					if (newPhi > Math.PI) {
						newPhi = Math.PI;
					}
					if (newPhi < 0D) {
						newPhi = 0D;
					}

					double newTheta = centerCoords.getTheta() + ((double) difx * .0025D % (Math.PI / 2D));
					while (newTheta > (Math.PI * 2D)) {
						newTheta -= (Math.PI * 2D);
					}
					while (newTheta < 0D) {
						newTheta += (Math.PI * 2D);
					}

					centerCoords = new Coordinates(newPhi, newTheta);

					if (topo) {
						topoSphere.drawSphere(centerCoords);
					} else {
						marsSphere.drawSphere(centerCoords);
					}

					recreate = false;

					repaint();
				}

				super.mouseDragged(e);
			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				//System.out.println("mousepressed X = " + e.getX());
				//System.out.println("             Y = " + e.getY());
				dragx = e.getX();
				dragy = e.getY();

				super.mousePressed(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				dragx = 0;
				dragy = 0;
				navwin.updateCoords(centerCoords);
				super.mouseReleased(e);
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				navwin.setCursor(new Cursor(Cursor.MOVE_CURSOR));
				super.mouseReleased(e);
			}

			@Override
			public void mouseExited(MouseEvent e) {
				navwin.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				super.mouseReleased(e);
			}
		});

		// Initially show real surface globe
		showSurf();
	}

	/**
	 * Displays real surface globe, regenerating if necessary
	 */
	public void showSurf() {
		if (topo) {
			recreate = true;
		}
		topo = false;
		showGlobe(centerCoords);
	}

	/**
	 * Displays topographical globe, regenerating if necessary
	 */
	public void showTopo() {
		if (!topo) {
			recreate = true;
		}
		topo = true;
		showGlobe(centerCoords);
	}

	/**
	 * Displays globe at given center regardless of mode, regenerating if
	 * necessary
	 *
	 * @param newCenter
	 *            the center location for the globe
	 */
	public void showGlobe(Coordinates newCenter) {
		if (!centerCoords.equals(newCenter)) {
			recreate = true;
			centerCoords.setCoords(newCenter);
		}
		updateDisplay();
	}

	/**
	 * Starts display update thread (or creates a new one if necessary)
	 */
	private void updateDisplay() {
		if ((showThread == null) || (!showThread.isAlive())) {
			showThread = new Thread(this, Msg.getString("GlobeDisplay.thread.globe")); //$NON-NLS-1$
			showThread.start();
		} else {
			showThread.interrupt();
		}
	}

	/**
	 * the run method for the runnable interface
	 */
	public void run() {
		while (update)
			refreshLoop();
	}

	/**
	 * loop, refreshing the globe display when necessary
	 */
	public void refreshLoop() {
		keepRunning = true;
		while (keepRunning) { // Endless refresh loop
			if (recreate) {
				// Regenerate globe if recreate is true, then display
				if (topo) {
					topoSphere.drawSphere(centerCoords);
				} else {
					marsSphere.drawSphere(centerCoords);
				}
				recreate = false;
				repaint();
			} else {
				// Pause for 2 seconds between display refreshs
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {}

				repaint();
			}
		}
	}

	@Override
	public void paintComponent(Graphics g) {

		Image starfield = ImageLoader.getImage("starfield.gif"); //TODO: localize
		g.drawImage(starfield, 0, 0, Color.black, null);
		// Draw real or topo globe
		MarsGlobe globe = topo ? topoSphere : marsSphere;

		if (globe.isImageDone()) {
			g.drawImage(globe.getGlobeImage(), 0, 0, this);
		}

		if (showDayNightShading) {
			drawShading(g);
		}

		drawUnits(g);
		drawCrossHair(g);
	}

	/**
	 * Draws the day/night shading on the globe.
	 * @param g graphics context
	 */
	protected void drawShading(Graphics g) {
		int centerX = width / 2;
		int centerY = height / 2;

		//if (mars == null)
		//	mars = Simulation.instance().getMars();

		if (surfaceFeatures == null)
			surfaceFeatures = Simulation.instance().getMars().getSurfaceFeatures();


		// Coordinates sunDirection = mars.getOrbitInfo().getSunDirection();

		Coordinates location = new Coordinates(0D, 0D);
		for (int x = 0; x < 150; x++) {
			for (int y = 0; y < 150; y++) {
				int xDiff = x - centerX;
				int yDiff = y - centerY;
				if (Math.sqrt((xDiff * xDiff) + (yDiff * yDiff)) <= 47.74648293D) {
					centerCoords.convertRectToSpherical(xDiff, yDiff,
							47.74648293D, location);
					double sunlight = surfaceFeatures.getSurfaceSunlight(location);
					//double sunlight =surfaceFeatures.getSolarIrradiance(location) / SurfaceFeatures.MEAN_SOLAR_IRRADIANCE;

					if (sunlight > 1D) {
					    sunlight = 1D;
					}
					int sunlightInt = (int) (127 * sunlight);
					shadingArray[x + (y * width)] = ((127 - sunlightInt) << 24) & 0xFF000000;
				} else {
					shadingArray[x + (y * 150)] = 0xFF000000;
				}
			}
		}

		// Create shading image for map
		Image shadingMap = this.createImage(new MemoryImageSource(width,
				height, shadingArray, 0, width));

				MediaTracker mt = new MediaTracker(this);
				mt.addImage(shadingMap, 0);
				try {
					mt.waitForID(0);
				} catch (InterruptedException e) {
					logger.log(
						Level.SEVERE,
						Msg.getString("GlobeDisplay.log.shadingInterrupted",e.toString()) //$NON-NLS-1$
					);
				}

				// Draw the shading image
				g.drawImage(shadingMap, 0, 0, this);
	}

	/**
	 * draw the dots on the globe that identify units
	 * @param g graphics context
	 */
	protected void drawUnits(Graphics g) {
		Iterator<Unit> i = Simulation.instance().getUnitManager().getUnits()
				.iterator();
		while (i.hasNext()) {
			Unit unit = i.next();
			UnitDisplayInfo displayInfo = UnitDisplayInfoFactory
					.getUnitDisplayInfo(unit);
			if (displayInfo.isGlobeDisplayed(unit)) {
				Coordinates unitCoords = unit.getCoordinates();
				if (centerCoords.getAngle(unitCoords) < HALF_PI) {
					if (topo) {
						g.setColor(displayInfo.getTopoGlobeColor());
					}
					else {
						g.setColor(displayInfo.getSurfGlobeColor());
					}

					IntPoint tempLocation = getUnitDrawLocation(unitCoords);
					g.fillRect(tempLocation.getiX(), tempLocation
							.getiY(), 1, 1);
				}
			}
		}
	}

	/**
	 * Draw green rectanges and lines (cross-hair type thingy), and write the
	 * latitude and logitude of the center point of the current globe view.
	 * @param g graphics context
	 */
	protected void drawCrossHair(Graphics g) {
		g.setColor(Color.green);

		// If USGS map is used, use small crosshairs.
		if (useUSGSMap & !topo) {
			g.drawRect(72, 72, 6, 6);
			g.drawLine(0, 75, 71, 75);
			g.drawLine(79, 75, 149, 75);
			g.drawLine(75, 0, 75, 71);
			g.drawLine(75, 79, 75, 149);
		}
		// If not USGS map, use large crosshairs.
		else {
			g.drawRect(57, 57, 33, 33);
			g.drawLine(0, 74, 56, 74);
			g.drawLine(90, 74, 149, 74);
			g.drawLine(74, 0, 74, 57);
			g.drawLine(74, 90, 74, 149);
		}

		// use prepared font
		g.setFont(positionFont);

		// Draw longitude and latitude strings using prepared measurements
		g.drawString(latitude, 5, 130);
		g.drawString(longitude, 145 - rightWidth, 130);

		String latString = centerCoords.getFormattedLatitudeString();
		String longString = centerCoords.getFormattedLongitudeString();

		int latWidth = positionMetrics.stringWidth(latString);
		int longWidth = positionMetrics.stringWidth(longString);

		int latPosition = ((leftWidth - latWidth) / 2) + 5;
		int longPosition = 145 - rightWidth + ((rightWidth - longWidth) / 2);

		g.drawString(latString, latPosition, 142);
		g.drawString(longString, longPosition, 142);
	}

	/**
	 * Returns unit x, y position on globe panel
	 * @param unitCoords the unit's location
	 * @return x, y position on globe panel
	 */
	private IntPoint getUnitDrawLocation(Coordinates unitCoords) {
		double rho = width / Math.PI;
		int half_map = width / 2;
		int low_edge = 0;
		return Coordinates.findRectPosition(unitCoords, centerCoords, rho,
				half_map, low_edge);
	}

	/**
	 * Set USGS as surface map
	 * @param useUSGSMap true if using USGS map.
	 */
	public void setUSGSMap(boolean useUSGSMap) {
		this.useUSGSMap = useUSGSMap;
	}

	/**
	 * Sets day/night tracking to on or off.
	 * @param showDayNightShading true if globe is to use day/night tracking.
	 */
	public void setDayNightTracking(boolean showDayNightShading) {
		this.showDayNightShading = showDayNightShading;
	}

	/**
	 * Gets the center coordinates of the globe.
	 * @return coordinates.
	 */
	public Coordinates getCoordinates() {
		return centerCoords;
	}

	/**
	 * Sets the center coordinates of the globe.
	 * @param c the center coordinates.
	 */
	public void setCoordinates(Coordinates c) {
		if (c != null) {
			centerCoords = c;
		}
	}

	/**
	 * Prepare globe for deletion.
	 */
	public void destroy() {
		update = false;
		keepRunning = false;
		marsSphere = null;
		topoSphere = null;
		centerCoords = null;
	}
}