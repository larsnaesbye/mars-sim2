/*
 * Mars Simulation Project
 * JProcessButton.java
 * @date 2023-02-18
 * @author Barry Evans
 */
package com.mars_sim.ui.swing.utils;

import javax.swing.Icon;
import javax.swing.JButton;

import com.mars_sim.ui.swing.ImageLoader;

/**
 * A JButton implementation that can be used to show running processes.
 * It is represented as a Red or Green dot.
 */
@SuppressWarnings("serial")
public class JProcessButton extends JButton {
    public static final Icon STOPPED_DOT = ImageLoader.getIconByName("dot/red");
    public static final Icon RUNNING_DOT = ImageLoader.getIconByName("dot/green");

    public JProcessButton() {
        super();
    }

    /**
     * Set the button in a running state
     * @param running Current running state
     */
    public void setRunning(boolean running) {
		if (running) {
            setIcon(RUNNING_DOT);
        }
        else {
			setIcon(STOPPED_DOT);
        }
    }
}
