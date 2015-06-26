/**
 * Mars Simulation Project
 * GlobeDisplay.java
 * @version 3.07 2014-12-06

 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.time;

import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.ui.javafx.MainScene;
import org.mars_sim.msp.ui.swing.MainDesktopPane;

import javax.swing.*;
import java.awt.*;

/**
 * The Mars Calendar Display class shows the current month
 * in a panel for the {@link TimeWindow} class.
 */
class MarsCalendarDisplay
extends JComponent {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	/** The Martian clock instance. */
	private MarsClock marsTime;

	private MainDesktopPane desktop;

	private MainScene mainScene;
	/** The Sol of month cache. */
	private int solOfMonthCache;

	private int theme;

	private Color baseColor, midColor, darkColor;

	/**
	 * Constructs a MarsCalendarDisplay object.
	 * @param marsTime Martian clock instance
	 */
	public MarsCalendarDisplay(MarsClock marsTime, MainDesktopPane desktop) {

		// Initialize data members
		this.marsTime = marsTime;
		this.desktop = desktop;
		mainScene = desktop.getMainScene();
		solOfMonthCache = marsTime.getSolOfMonth();

		// Set component size
		setPreferredSize(new Dimension(140, 90));
		setMaximumSize(getPreferredSize());
		setMinimumSize(getPreferredSize());

		baseColor = Color.orange;
		midColor = new Color(210, 117, 101);
		darkColor = new Color(140, 94, 74);
	}

	/**
	 * Updates the calendar display
	 */
	public void update() {

		if (mainScene != null) {
			theme = MainScene.getTheme();

			if (theme == 1) {
				baseColor = Color.orange;
				midColor = new Color(210, 117, 101);
				darkColor = new Color(140, 94, 74);
			}
			else if (theme == 2) {
				baseColor = Color.green;
				midColor = new Color(104, 114, 77); // 74, 140, 94
				darkColor = new Color(73, 97, 0);
			}
			else if (theme == 3) {
				baseColor = Color.red;
				midColor = new Color(255, 102, 102);
				darkColor = new Color(51, 25, 0);
			}
			else if (theme == 4) {
				baseColor = Color.yellow;
				midColor = new Color(152, 149, 92);
				darkColor = new Color(138, 141, 74);
			}
			else if (theme == 5) {
				baseColor = Color.gray;
				midColor = Color.lightGray;
				darkColor = Color.DARK_GRAY;
			}
			else if (theme == 6) {
				baseColor = Color.magenta;
				midColor = new Color(112, 76, 103);
				darkColor = new Color(51, 0, 51);
			}
			else if (theme == 7) {
				baseColor = Color.cyan;
				midColor = new Color(99, 125, 150);
				darkColor = new Color(101, 139, 210);
			}
		}

		if (solOfMonthCache != marsTime.getSolOfMonth()) {
			solOfMonthCache = marsTime.getSolOfMonth();
			repaint();
		}
	}

	/**
	 * Overrides paintComponent method.
	 * @param g graphics context
	 */
	@Override
	public void paintComponent(Graphics g) {

		// Paint dark green background
		g.setColor(darkColor);
		g.fillRect(0, 0, 140, 90);

		// Paint mid green week day name boxes
		g.setColor(midColor);
		g.fillRect(0, 0, 140, 10);

		int solsInMonth = MarsClock.getSolsInMonth(marsTime.getMonth(), marsTime.getOrbit());

		// If sols in month are 27, black out lower left square
		if (solsInMonth == 27) {
			g.setColor(Color.black);
			g.fillRect(121, 71, 138, 88);
		}

		// Paint green rectangle
		g.setColor(baseColor);
		g.drawRect(0, 0, 139, 89);

		// Paint vertical day lines
		for (int x=1; x < 7; x++) {
			g.drawLine(20 * x, 0, 20 * x, 89);
		}

		// Paint horizontal lines
		for (int x=0; x < 4; x++) {
			g.drawLine(0, (20 * x) + 10, 139, (20 * x) + 10);
		}

		// Set up week letter font
		Font weekFont = new Font("SansSerif", Font.PLAIN, 8);
		FontMetrics weekMetrics = getFontMetrics(weekFont);
		int weekHeight = weekMetrics.getAscent();

		// Draw week letters
		g.setFont(weekFont);
		char[] weekLetters = {'S', 'P', 'D', 'T', 'H', 'V', 'J'};
		for (int x=0; x < 7; x++) {
			int letterWidth = weekMetrics.charWidth(weekLetters[x]);
			g.drawString("" + weekLetters[x], (20 * x) + 11 - (letterWidth / 2), weekHeight - 1);
		}

		// Set up Sol number font
		Font solFont = new Font("SansSerif", Font.BOLD, 10);
		FontMetrics solMetrics = getFontMetrics(solFont);
		int solHeight = solMetrics.getAscent();

		// Draw sol letters
		g.setFont(solFont);
		for (int y=0; y < 4; y++) {
			for (int x=0; x < 7; x++) {
				int solNumber = (y * 7) + x + 1;
				int solNumberWidth = solMetrics.stringWidth("" + solNumber);
				int xPos = (20 * x) + 11 - (solNumberWidth / 2);
				int yPos = (20 * y) + 30 - (solHeight / 2);
				if (solNumber <= solsInMonth)
					g.drawString(Integer.toString(solNumber), xPos, yPos);
				if (solNumber == marsTime.getSolOfMonth()) {
					g.fillRect((20 * x) + 2, (20 * y) + 12, 17, 17);
					g.setColor(Color.black);
					g.drawString(Integer.toString(solNumber), xPos, yPos);
					g.setColor(baseColor);
				}
			}
		}
	}
}