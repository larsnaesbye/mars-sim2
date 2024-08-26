/*
 * Mars Simulation Project
 * MapPanel.java
 * @date 2023-06-19
 * @author Scott Davis
 */

package com.mars_sim.ui.swing.tool.map;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JPanel;

import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.mapdata.IntegerMapData;
import com.mars_sim.mapdata.MapDataFactory;
import com.mars_sim.mapdata.MapDataUtil;
import com.mars_sim.mapdata.MapMetaData;
import com.mars_sim.mapdata.location.Coordinates;
import com.mars_sim.mapdata.map.Map;
import com.mars_sim.mapdata.map.MapLayer;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.tool.mission.MissionWindow;
import com.mars_sim.ui.swing.tool.mission.NavpointPanel;
import com.mars_sim.ui.swing.tool.navigator.NavigatorWindow;

@SuppressWarnings("serial")
public class MapPanel extends JPanel implements MouseWheelListener {

	private static final Logger logger = Logger.getLogger(MapPanel.class.getName());
	
	private static final double HALF_PI = Math.PI / 2d;

	public static final int MAP_BOX_HEIGHT = Map.MAP_BOX_HEIGHT;
	public static final int MAP_BOX_WIDTH = Map.MAP_BOX_WIDTH;
	private static int dragx, dragy;

	private transient ExecutorService executor;

	// Data members
	private boolean mouseDragging;
//	private boolean zooming;
	private boolean mapError;
	private boolean wait;
//	private boolean update;
	private boolean recreateMap = false;
	
	public static double RHO_DEFAULT;
	public static double MAX_RHO;
	public static double MIN_RHO;
	
	private final double ZOOM_STEP = 16;
	private double multiplier;
	private double magnification;
	
	private String mapErrorMessage;
	
	private Coordinates centerCoords;

	private Image mapImage;

	private Map marsMap;
	
	private MainDesktopPane desktop;

	private NavigatorWindow navwin;
	
	private NavpointPanel navPanel;

	private Image starfield;
	
	private List<MapLayer> mapLayers;

	private final MapDataUtil mapUtil = MapDataUtil.instance();
	
	/**
	 * Constructor 1.
	 * 
	 * @param desktop
	 * @param navwin
	 */
	public MapPanel(MainDesktopPane desktop, NavigatorWindow navwin) {
		this(desktop);
		this.navwin = navwin;
	}
	
	/**
	 * Constructor 2.
	 * 
	 * @param desktop
	 * @param navPanel
	 */
	public MapPanel(MainDesktopPane desktop, NavpointPanel navPanel) {
		this(desktop);
		this.navPanel = navPanel;
	}
	
	/**
	 * Constructor 3.
	 * 
	 * @param desktop
	 * @param refreshRate
	 */
	public MapPanel(MainDesktopPane desktop, long refreshRate) {
		this(desktop);
		this.desktop = desktop;
	}
	
	/**
	 * Constructor 4.
	 * 
	 * @param desktop
	 */
	public MapPanel(MainDesktopPane desktop) {
		super();
		this.desktop = desktop;
		
		init();
	}
	
	public void init() {
	    
		starfield = ImageLoader.getImage("map/starfield");
		
		executor = Executors.newSingleThreadExecutor();
		
		// Initializes map
		loadNewMapType(MapDataFactory.DEFAULT_MAP_TYPE, NavigatorWindow.getMapResolution());
		
		mapError = false;
		wait = false;
		mapLayers = new CopyOnWriteArrayList<>();
		centerCoords = new Coordinates(HALF_PI, 0D);
	
		setPreferredSize(new Dimension(MAP_BOX_WIDTH, MAP_BOX_HEIGHT));
		setMaximumSize(getPreferredSize());
		setSize(getPreferredSize());
		
		RHO_DEFAULT = IntegerMapData.RHO_DEFAULT;
		MAX_RHO = IntegerMapData.MAX_RHO;
		MIN_RHO = IntegerMapData.MIN_RHO;
		multiplier = RHO_DEFAULT / ZOOM_STEP;
		magnification = 1;
		
//		logger.info("scale: " + Math.round(RHO_DEFAULT * 10.0)/10.0 + "  multiplier: " + Math.round(multiplier * 10.0)/10.0);
	}

	/**
	 * Detects the mouse wheel movement.
	 */
	public void mouseWheelMoved(MouseWheelEvent e) {
		// Gets the latest scale
		double oldRho = getRho();

		// May use this if (e.isControlDown()) {} to add ctrl key
        // May use if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {} to combine with other keys 
		
		double delta = e.getWheelRotation();
    	double rhoDelta = - multiplier * delta;
    	double newRho = oldRho + rhoDelta;
    	
//      Note 1:: Scroll up to zoom in or magnify map
//    	Note 2:Scroll down to zoom out or shrink the map

		if (newRho > MAX_RHO) {
			newRho = MAX_RHO;
		}
		else if (newRho < MIN_RHO) {
			newRho = MIN_RHO;
		}
 
		if (newRho != oldRho) {

			magnification = newRho/RHO_DEFAULT;
			
//			zooming = true;
			
//			logger.info("mag: " + Math.round(magnification * 1000.0)/1000.0 
//					+ "  rhoDelta: " + Math.round(rhoDelta* 1000.0)/1000.0
//					+ "  newRho: " + Math.round(newRho* 1000.0)/1000.0
//					+ "  RHO_DEFAULT: " + Math.round(RHO_DEFAULT* 1000.0)/1000.0
//					);
			
	    	// Update the map scale
//	    	setMapScale(newRho);

			// Call showMap
//			showMap(centerCoords, newRho);
	    	// which in turns calls updateDisplay()
	    	// which in turns calls MapTask thread
	    	// which in turns calls marsMap.drawMap(centerCoords, getScale());

			marsMap.drawMap(centerCoords, newRho);
			
			repaint();
		}
    }
	
	/*
	 * Sets up the mouse dragging capability.
	 */
	public void setMouseDragger(boolean isNavigator) {

		// Detect the mouse scroll
		addMouseWheelListener(this);
		
		// Note: need navWin prior to calling addMouseMotionListener()
		addMouseMotionListener(new MouseAdapter() {
			@Override
			public void mouseDragged(MouseEvent e) {
				int x = e.getX();
				int y = e.getY();
				int dx = dragx - x;
				int dy = dragy - y;

				if ((dx != 0 || dy != 0) 
					 && x > 0 && x < MAP_BOX_WIDTH 
					 && y > 0 && y < MAP_BOX_HEIGHT) {
					
					mouseDragging = true;
					
					// Update the centerCoords while dragging
					centerCoords = centerCoords.convertRectToSpherical(dx, dy, marsMap.getRho());
					// Do we really want to update the map while dragging ? 
					// Yes. It's needed to provide smooth viewing of the surface map
					marsMap.drawMap(centerCoords, getRho());
				
					repaint();
				}

				dragx = x;
				dragy = y;

			}
		});

		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				dragx = e.getX();
				dragy = e.getY();

				mouseDragging = true;
				
				setCursor(new Cursor(Cursor.MOVE_CURSOR));
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				dragx = 0;
				dragy = 0;
				
				mouseDragging = false;
//				zooming = false;

				setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}
		});
	}

	public boolean isMouseDragging() {
		return mouseDragging;
	}

//	public boolean isZooming() {
//		return zooming;
//	}
	
	public boolean isChanging() {
		return mouseDragging;
	}
	
	/**
	 * Adds a new map layer.
	 * 
	 * @param newLayer the new map layer.
	 * @param index    the index order of the map layer.
	 */
	public void addMapLayer(MapLayer newLayer, int index) {
		if (newLayer != null) {
			if (!mapLayers.contains(newLayer)) {
				if (index < mapLayers.size()) {
					mapLayers.add(index, newLayer);
				} else {
					mapLayers.add(newLayer);
				}
			}
		} else
			throw new IllegalArgumentException("newLayer is null");
	}

	/**
	 * Removes a map layer.
	 * 
	 * @param oldLayer the old map layer.
	 */
	public void removeMapLayer(MapLayer oldLayer) {
		if (oldLayer != null) {
			if (mapLayers.contains(oldLayer)) {
				mapLayers.remove(oldLayer);
			}
		} else
			throw new IllegalArgumentException("oldLayer is null");
	}

	/**
	 * Checks if map has a map layer.
	 * 
	 * @param layer the map layer.
	 * @return true if map has the map layer.
	 */
	public boolean hasMapLayer(MapLayer layer) {
		return mapLayers.contains(layer);
	}
	   
	/**
	 * Gets the map type.
	 * 
	 * @return map type.
	 */
	public MapMetaData getMapMetaData() {
		return marsMap.getMapMetaData();
	}

	public Map getMap() {
		return marsMap;
	}

	/**
	 * Loads the new map type.
	 * 
	 * @param mapStringType
	 * @param res
	 * @return true if map type set successfully
	 */
	public boolean loadNewMapType(String newMapString, int res) {
		boolean reload = false;
		
		if (marsMap == null) {	
//			logger.info("marsMap == null");
			reload = true;
		}
		else if (res != marsMap.getMapMetaData().getResolution()) {
//			logger.info("res != marsMap.getMapMetaData().getResolution()");
			reload = true;
		}
		else if (!newMapString.equals(marsMap.getMapMetaData().getMapString())) {
//			logger.info("!newMapString.equals(marsMap.getMapMetaData().getMapString())");
			reload = true;
		}
		
		if (reload) {
			
			mapUtil.setMapData(newMapString, res);
			marsMap = new CannedMarsMap(this, mapUtil.loadMapData(newMapString));

			// Redefine map param
			RHO_DEFAULT = IntegerMapData.RHO_DEFAULT;
			MAX_RHO = IntegerMapData.MAX_RHO;
			MIN_RHO = IntegerMapData.MIN_RHO;
			multiplier = RHO_DEFAULT / ZOOM_STEP;
			
			// Actually need to retain the previous magnification
//			magnification = getRho()/RHO_DEFAULT;
			
			recreateMap = true;
			
			showMap(centerCoords, getRho());
			
			return reload;
		}
	
		return reload;
	}

	public Coordinates getCenterLocation() {
		return centerCoords;
	}
	
	public void showMap(Coordinates newCenter) {
		showMap(newCenter, getRho());
	}
	
	/**
	 * Displays map at given center, regenerating if necessary.
	 *
	 * @param newCenter the center location for the globe
	 */
	public void showMap(Coordinates newCenter, double scale) {
		if (newCenter == null) 
			return;
		
		if (centerCoords == null
			|| !centerCoords.equals(newCenter)
			|| scale != getRho()) {
				recreateMap = true;
				centerCoords = newCenter;
		}
		else {
			recreateMap = false;
		}
			
		if (recreateMap) {
			wait = true;
			updateDisplay(scale);
			recreateMap = false;
		}
	}

	public void updateDisplay() {
		updateDisplay(getRho());
	}

	public void updateDisplay(double scale) {
		if ((desktop.isToolWindowOpen(NavigatorWindow.NAME) 
			|| desktop.isToolWindowOpen(MissionWindow.NAME))
			&& (!executor.isTerminated() || !executor.isShutdown())) {
				executor.execute(new MapTask(scale));
		}
	}

	class MapTask implements Runnable {

		private double scale;
		
		private MapTask(double scale) {
			this.scale = scale;
		}

		@Override
		public void run() {
			try {
				mapError = false;

				if (centerCoords == null) {
					logger.severe("centerCoords is null.");
					centerCoords = new Coordinates(HALF_PI, 0);
				}

				marsMap.drawMap(centerCoords, scale);
				
				wait = false;
				repaint();
				
			} catch (Exception e) {
				mapError = true;
				mapErrorMessage = e.getMessage();
				logger.severe("Can't draw surface map: " + e);
			}
		}
	}
	
	@Override	
	public void paintComponent(Graphics g) {
        super.paintComponent(g);

        if (desktop != null && isShowing() && 
        		(desktop.isToolWindowOpen(NavigatorWindow.NAME)
        		|| desktop.isToolWindowOpen(MissionWindow.NAME))) {
	        
        	Graphics2D g2d = (Graphics2D) g.create();
	        
        	g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			
	        if (wait) {
//	        	if (mapImage != null) {
//	        		g2d.drawImage(mapImage, 0, 0, this);	
//	        	}
	        	String message = "Generating Map";
	        	drawCenteredMessage(message, g2d);
	        }
	        else {
	        	if (mapError) {
	            	logger.log(Level.SEVERE,"mapError: " + mapErrorMessage);
	                // Display previous map image
//	                if (mapImage != null) g2d.drawImage(mapImage, 0, 0, this);
	
	                // Draw error message
	                if (mapErrorMessage == null) mapErrorMessage = "Null Map";
	                drawCenteredMessage(mapErrorMessage, g2d);
	            }
	        	else {
		
//	        		g2d.setBackground(Color.BLACK);
	        		
	        		// Clear the background with white
//	        		g2d.clearRect(0, 0, Map.DISPLAY_WIDTH, Map.DISPLAY_HEIGHT);

	        		// Paint black background
//	        		g2d.setPaint(Color.BLACK); 
	        		g2d.setColor(Color.BLACK);
	                
	        		g2d.fillRect(0, 0, Map.MAP_BOX_WIDTH, Map.MAP_BOX_HEIGHT);

	        		g2d.drawImage(starfield, 0, 0, Color.black, null);
	        		
//	        		g2d.drawImage(starfield, 0, 0, Color.BLACK, this);
	        		
//	        		g2d.setComposite(AlphaComposite.SrcOver); 
//	        		g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.0f)); 
	        		// or 0.0f)); // draw transparent background
	        		// or 1.0f)); // turn on opacity
	        		
//	        		g2d.fillRect(0, 0, Map.MAP_BOX_WIDTH, Map.MAP_BOX_HEIGHT);

//	        		Graphics2D gbi = null;
	        		
	                if (centerCoords != null) {
	                	if (marsMap != null && marsMap.isImageDone()) {
	                		mapImage = marsMap.getMapImage();
	                		if (mapImage != null) {
//		                		gbi = (Graphics2D) mapImage.getGraphics();       
//		                		gbi.fillRect(0, 0, Map.MAP_BOX_WIDTH, Map.MAP_BOX_HEIGHT);
//	                			g2d.clearRect(0, 0, Map.MAP_BOX_WIDTH, Map.MAP_BOX_HEIGHT);
//	                			gbi.drawImage(mapImage, 0, 0, this);      
	                			g2d.drawImage(mapImage, 0, 0, this);  
	                		}         		
	                	}
	                	else
	                		return;
	
	                	// Display map layers.
	                	
	                	// Put the map layers here.
	                	// Say if the location of a vehicle is updated
	                	// it doesn't have to redraw the marsMap.
	                	// It only have to redraw its map layer below
	                	Iterator<MapLayer> i = mapLayers.iterator();
	                	while (i.hasNext()) i.next().displayLayer(centerCoords, marsMap, g);
	                	
                		
//                		gbi.dispose();
	                }
	        	}
	        }
	        
	        g2d.dispose();
        }
    }

	/**
	 * Draws a message string in the center of the map panel.
	 * 
	 * @param message the message string
	 * @param g       the graphics context
	 */
	private void drawCenteredMessage(String message, Graphics g) {

		// Set message color
		g.setColor(Color.green);

		// Set up font
		Font messageFont = new Font("SansSerif", Font.BOLD, 25);
		g.setFont(messageFont);
		FontMetrics messageMetrics = getFontMetrics(messageFont);

		// Determine message dimensions
		int msgHeight = messageMetrics.getHeight();
		int msgWidth = messageMetrics.stringWidth(message);

		// Determine message draw position
		int x = (Map.MAP_BOX_WIDTH - msgWidth) / 2;
		int y = (Map.MAP_BOX_HEIGHT + msgHeight) / 2;

		// Draw message
		g.drawString(message, x, y);
	}

	public void update(ClockPulse pulse) {
		updateDisplay();
	}

	/**
	 * Gets the true surface lat and lon coordinates of the mouse pointer.
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
    public Coordinates getMouseCoordinates(int x, int y) {
		double xMap = x - Map.MAP_BOX_WIDTH / 2.0;
		double yMap = y - Map.MAP_BOX_HEIGHT / 2.0;
		
		return centerCoords.convertRectToSpherical(xMap, yMap, marsMap.getRho());
    }

    /**
     * Gets the rho of the Mars surface map.
     * 
     * @return
     */
    public double getRho() {
    	return marsMap.getRho();
    }

	/**
	 * Sets the map rho.
	 *
	 * @param rho
	 */
	public void setRho(double rho) {
//		marsMap.setRho(rho);
		marsMap.drawMap(centerCoords, rho);
		repaint();
	}
	
    
    /**
     * Gets the magnification of the Mars surface map.
     * 
     * @return
     */
    public double getMagnification() {
    	return magnification;
    }
    
	/**
	 * Prepares map panel for deletion.
	 */
	public void destroy() {
		// Remove clock listener.
		mapLayers = null;
		if (executor != null) {
			// Stop anything running
			executor.shutdownNow();
		}
		executor = null;
		marsMap = null;
//		update = false;
		mapImage = null;
	}

}
