/**
 * Mars Simulation Project
 * ProspectingSitePanel.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */

package com.mars_sim.ui.swing.tool.mission.create;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import com.mars_sim.core.map.Map;
import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.map.location.IntPoint;
import com.mars_sim.core.person.ai.mission.MissionType;
import com.mars_sim.ui.swing.MarsPanelBorder;
import com.mars_sim.ui.swing.tool.map.EllipseLayer;
import com.mars_sim.ui.swing.tool.map.MapPanel;
import com.mars_sim.ui.swing.tool.map.MapUtils;
import com.mars_sim.ui.swing.tool.map.NavpointEditLayer;
import com.mars_sim.ui.swing.tool.map.UnitIconMapLayer;
import com.mars_sim.ui.swing.tool.map.UnitLabelMapLayer;

/**
 * A wizard panel for the ice or regolith prospecting site.
 */
@SuppressWarnings("serial")
class ProspectingSitePanel extends WizardPanel {

	private static final Logger logger = Logger.getLogger(ProspectingSitePanel.class.getName());
	
	// Wizard panel name.
	private static final String NAME = "Prospecting Site";
	
	// Range modifier.
	private static final double RANGE_MODIFIER = .95D;
	
	// Data members.
	private MapPanel mapPane;
	private EllipseLayer ellipseLayer;
	private NavpointEditLayer navLayer;
	private boolean navSelected;
	private IntPoint navOffset;
	private JLabel locationLabel;
	private int pixelRange;
	
	/**
	 * Constructor.
	 * 
	 * @param wizard the create mission wizard.
	 */
	ProspectingSitePanel(CreateMissionWizard wizard) {
		// Use WizardPanel constructor.
		super(wizard);
		
		// Set the layout.
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		// Set the border.
		setBorder(new MarsPanelBorder());
		
		// Create a vertical strut to add some UI space.
		add(Box.createVerticalStrut(10));
		
		// Create the title label.
		String resource = "";
		MissionType type = getWizard().getMissionData().getMissionType();
		if (MissionType.COLLECT_ICE == type) resource = "Ice";
		else if (MissionType.COLLECT_REGOLITH == type) resource = "Regolith";
		JLabel titleLabel = createTitleLabel("Choose Your " + resource + " Collection Site : ");
		add(titleLabel);
		
		// Create a vertical strut to add some UI space.
		add(Box.createVerticalStrut(10));
		
		// Create the map panel.
		mapPane = new MapPanel(wizard.getDesktop(), 200L);
		
		mapPane.addMapLayer(new UnitIconMapLayer(mapPane), 0);
		mapPane.addMapLayer(new UnitLabelMapLayer(), 1);
		mapPane.addMapLayer(ellipseLayer = new EllipseLayer(Color.GREEN), 2);
		mapPane.addMapLayer(navLayer = new NavpointEditLayer(mapPane, false), 3);
		
		mapPane.addMouseListener(new NavpointMouseListener());
		mapPane.addMouseMotionListener(new NavpointMouseMotionListener());
		mapPane.setMaximumSize(mapPane.getPreferredSize());
		mapPane.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(mapPane);
		
		// Create the location label.
		locationLabel = new JLabel("Location: ", SwingConstants.CENTER);
		locationLabel.setFont(locationLabel.getFont().deriveFont(Font.BOLD));
		locationLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(locationLabel);
		
		// Create a vertical strut to add some UI space.
		add(Box.createVerticalStrut(10));
		
		// Create the instruction label.
		JLabel instructionLabel = new JLabel("Drag navpoint flag to desired " + resource + 
				" collection site.");
		instructionLabel.setFont(instructionLabel.getFont().deriveFont(Font.BOLD));
		instructionLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
		add(instructionLabel);
		
		// Create vertical glue.
		add(Box.createVerticalGlue());
	}
	
	/**
	 * Gets the wizard panel name.
	 * 
	 * @return panel name.
	 */
	String getPanelName() {
		return NAME;
	}

	/**
	 * Commits changes from this wizard panel.
	 * 
	 * @param isTesting true if it's only testing conditions
	 * @return true if changes can be committed.
	 */
	@Override
	boolean commitChanges(boolean isTesting) {
		IntPoint navpointPixel = navLayer.getNavpointPosition(0);
		Coordinates navpoint = getCenterCoords().convertRectToSpherical(navpointPixel.getiX() - Map.HALF_MAP_BOX, 
				navpointPixel.getiY() - Map.HALF_MAP_BOX, mapPane.getMap().getRho());
		MissionType type = getWizard().getMissionData().getMissionType();
		if (MissionType.COLLECT_ICE == type) 
			getWizard().getMissionData().setIceCollectionSite(navpoint);
		else if (MissionType.COLLECT_REGOLITH == type) 
			getWizard().getMissionData().setRegolithCollectionSite(navpoint);
		return true;
	}

	/**
	 * Clears information on the wizard panel.
	 */
	void clearInfo() {
		getWizard().setButtons(false);
		navLayer.clearNavpointPositions();
	}

	/**
	 * Updates the wizard panel information.
	 */
	void updatePanel() {
		try {
			
			pixelRange = convertRadiusToMapPixels(getRoverRange());
			
			ellipseLayer.setEllipseDetails(new IntPoint(Map.HALF_MAP_BOX, Map.HALF_MAP_BOX), new IntPoint(Map.HALF_MAP_BOX, Map.HALF_MAP_BOX), (pixelRange * 2));
			IntPoint initialNavpointPos = new IntPoint(Map.HALF_MAP_BOX, Map.HALF_MAP_BOX - (pixelRange / 2));
			navLayer.addNavpointPosition(initialNavpointPos);
			Coordinates initialNavpoint = getCenterCoords().convertRectToSpherical(0, (-1 * (pixelRange / 2)), 
										mapPane.getMap().getRho());
			locationLabel.setText("Location: " + initialNavpoint.getFormattedString());
			mapPane.showMap(initialNavpoint);
		}
		catch (Exception e) {
			logger.severe("updatePanel encounters an exception in ProspectingSitePanel.");
		}
		
		getWizard().setButtons(true);
	}
	
	/**
	 * Gets the center coordinates.
	 * 
	 * @return center coordinates.
	 */
	private Coordinates getCenterCoords() {
		return getWizard().getMissionData().getStartingSettlement().getCoordinates();
	}
	
	/**
	 * Converts radius (km) into pixel range on map.
	 * 
	 * @param radius the radius (km).
	 * @return pixel radius.
	 */
	private int convertRadiusToMapPixels(double radius) {
		return MapUtils.getPixelDistance(radius, mapPane.getMap());
	}
	
	/**
	 * Gets the mission rover range.
	 * 
	 * @return range (km)
	 * @throws Exception if error getting mission rover.
	 */
	private double getRoverRange() {
		double range = getWizard().getMissionData().getRover().getEstimatedRange() * RANGE_MODIFIER;
		return range / 2D;
	}
	
	/**
	 * Inner class for listening to mouse events on the navpoint display.
	 */
	private class NavpointMouseListener extends MouseAdapter {
	
		/**
		 * Invoked when a mouse button has been pressed on a component.
		 * @param event the mouse event.
		 */
		public void mousePressed(MouseEvent event) {
			if (navLayer.overNavIcon(event.getX(), event.getY()) == 0) {
				// Select navpoint flag.
				navSelected = true;
				navLayer.selectNavpoint(0);
				navOffset = determineOffset(event.getX(), event.getY());
				ellipseLayer.setDisplayEllipse(true);
				mapPane.repaint();
			}
		}
		
		/**
		 * Gets the pixel offset from the currently selected navpoint.
		 * @param x the x coordinate selected.
		 * @param y the y coordinate selected.
		 * @return the pixel offset.
		 */
		private IntPoint determineOffset(int x, int y) {
			int xOffset = navLayer.getNavpointPosition(0).getiX() - x;
			int yOffset = navLayer.getNavpointPosition(0).getiY() - y;
			return new IntPoint(xOffset, yOffset);
		}
	
		/**
		 * Invoked when a mouse button has been released on a component.
		 * @param event the mouse event.
		 */
		public void mouseReleased(MouseEvent event) {
			navSelected = false;
			navLayer.clearSelectedNavpoint();
			ellipseLayer.setDisplayEllipse(false);
			mapPane.repaint();
		}
	}
	
	/**
	 * Inner class for listening to mouse movement on the navpoint display.
	 */
	private class NavpointMouseMotionListener extends MouseMotionAdapter {
		
		/**
		 * Invoked when a mouse button is pressed on a component and then dragged.
		 * @param event the mouse event.
		 */
		public void mouseDragged(MouseEvent event) {
			if (navSelected) {
				// Drag navpoint flag.
				int displayX = event.getPoint().x + navOffset.getiX();
				int displayY = event.getPoint().y + navOffset.getiY();
				IntPoint displayPos = new IntPoint(displayX, displayY);
				if (withinBounds(displayPos)) {
					navLayer.setNavpointPosition(0, displayPos);
					Coordinates center = getWizard().getMissionData().getStartingSettlement().getCoordinates();
					Coordinates navpoint = center.convertRectToSpherical(displayPos.getiX() - Map.HALF_MAP_BOX, 
					        displayPos.getiY() - Map.HALF_MAP_BOX, mapPane.getMap().getRho());
					locationLabel.setText("Location: " + navpoint.getFormattedString());
				
					mapPane.repaint();
				}
			}
		}
		
		/**
		 * Checks if mouse location is within range boundaries and edge of map display.
		 * 
		 * @param position the mouse location.
		 * @return true if within boundaries.
		 */
		private boolean withinBounds(IntPoint position) {
			
			if (!navLayer.withinDisplayEdges(position)) 
				return false;
			
			pixelRange = convertRadiusToMapPixels(getRoverRange());
			
            int radius = (int) Math.round(Math.sqrt(Math.pow(Map.HALF_MAP_BOX - position.getX(), 2D) +
			        Math.pow(Map.HALF_MAP_BOX - position.getY(), 2D)));
            
			if (radius > pixelRange) 
				return false;
			else
				return true;
		}
	}
}
