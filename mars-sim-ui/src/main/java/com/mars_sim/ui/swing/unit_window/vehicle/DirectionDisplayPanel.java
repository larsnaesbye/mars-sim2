/**
 * Mars Simulation Project
 * DirectionDisplayPanel.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */

package com.mars_sim.ui.swing.unit_window.vehicle;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import com.mars_sim.core.map.location.Direction;
import com.mars_sim.core.vehicle.StatusType;
import com.mars_sim.core.vehicle.Vehicle;

/**
 * The DirectionDisplayPanel class displays the compass direction a vehicle is
 * currently traveling.
 */
@SuppressWarnings("serial")
public class DirectionDisplayPanel extends JPanel {

	// Static members
	private final static int CIRCLE_DIAMETER = 44;
	private final static int CIRCLE_RADIUS = CIRCLE_DIAMETER / 2;

	// Data members
	private Vehicle vehicle;

	/**
	 * Constructor
	 *
	 * @param vehicle the vehicle to track
	 */
	public DirectionDisplayPanel(Vehicle vehicle) {
		// Use WebPanel constructor
		super();

		// Initialize data members
		this.vehicle = vehicle;

		// Set preferred component size.
		setPreferredSize(new Dimension(52, 52));

		// Add border
		setBorder(new LineBorder(Color.green));

		// Set panel to be opaque.
		setOpaque(true);

		// Set background to black
		setBackground(Color.black);
	}

	/**
	 * Update this panel.
	 */
	public void update() {
		repaint();
	}

	/**
	 * Override paintComponent method
	 * 
	 * @param g graphics context
	 */
	public void paintComponent(Graphics g) {

		super.paintComponent(g);

		// Get component height and width
		int height = getHeight();
		int width = getWidth();
		int centerX = width / 2;
		int centerY = height / 2;

		// Draw dark green background circle
		g.setColor(new Color(0, 62, 0));
		g.fillOval(centerX - CIRCLE_RADIUS, centerY - CIRCLE_RADIUS, CIRCLE_DIAMETER, CIRCLE_DIAMETER);

		// Draw bright green out circle
		g.setColor(Color.green);
		g.drawOval(centerX - CIRCLE_RADIUS, centerY - CIRCLE_RADIUS, CIRCLE_DIAMETER, CIRCLE_DIAMETER);

		// Draw center dot
		g.drawRect(centerX, centerY, 1, 1);

		// Prepare letter font
		Font tempFont = new Font("SansSerif", Font.PLAIN, 9);
		g.setFont(tempFont);
		FontMetrics tempMetrics = getFontMetrics(tempFont);
		int fontHeight = tempMetrics.getAscent();
		int letterRadius = CIRCLE_RADIUS - 7;

		// Draw 'N'
		int nWidth = tempMetrics.charWidth('N');
		g.drawString("N", centerX - (nWidth / 2), centerY - letterRadius + (fontHeight / 2));

		// Draw 'S'
		int sWidth = tempMetrics.charWidth('S');
		g.drawString("S", centerX - (sWidth / 2), centerY + letterRadius + (fontHeight / 2));

		// Draw 'W'
		int wWidth = tempMetrics.charWidth('W');
		g.drawString("W", centerX - letterRadius - (wWidth / 2), centerY + (fontHeight / 2));

		// Draw 'E'
		int eWidth = tempMetrics.charWidth('E');
		g.drawString("E", centerX + letterRadius - (eWidth / 2), centerY + (fontHeight / 2));

		// Draw direction line if necessary
		if (vehicle.getPrimaryStatus() == StatusType.MOVING) {
			Direction direction = vehicle.getDirection();
			double hyp = (double) (CIRCLE_RADIUS);
			int newX = (int) Math.round(hyp * direction.getSinDirection());
			int newY = -1 * (int) Math.round(hyp * direction.getCosDirection());
			g.drawLine(centerX, centerY, centerX + newX, centerY + newY);
		}
	}

	public void destroy() {
		vehicle = null;
	}
}
