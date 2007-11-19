/**
 * Mars Simulation Project
 * MainWindow.java
 * @version 2.82 2007-11-18
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.standard;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.mars_sim.msp.simulation.Simulation;
import org.mars_sim.msp.simulation.Unit;
import org.mars_sim.msp.simulation.time.MasterClock;
import org.mars_sim.msp.ui.standard.tool.navigator.NavigatorWindow;

/** 
 * The MainWindow class is the primary UI frame for the project. It
 * contains the tool bars and main desktop pane.
 */
public class MainWindow extends JFrame {

    // Data members
    private UnitToolBar unitToolbar; // The unit tool bar
    private MainDesktopPane desktop; // The main desktop

    private Thread newSimThread;
    private Thread loadSimThread;
    private Thread saveSimThread;
    
    /** 
     * Constructor
     */
    public MainWindow() {

        // use JFrame constructor
        super("Mars Simulation Project (version " + Simulation.VERSION + ")");

		// Set look and feel of UI.
		setLookAndFeel(false);
        
        // Prepare frame
        setVisible(false);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent event) {
                exitSimulation();
            }
        });

        // Prepare menu
        setJMenuBar(new MainWindowMenu(this));

        // Prepare content frame
        JPanel mainPane = new JPanel(new BorderLayout());
        setContentPane(mainPane);

        // Prepare tool toolbar
        ToolToolBar toolToolbar = new ToolToolBar(this);
        mainPane.add(toolToolbar, "North");

        // Prepare unit toolbar
        unitToolbar = new UnitToolBar(this);
        mainPane.add(unitToolbar, "South");

        // Prepare desktop
        desktop = new MainDesktopPane(this);
        mainPane.add(desktop, "Center");

        // Set frame size
        Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frame_size = new Dimension(screen_size);
        if (screen_size.width > 800) {
            frame_size = new Dimension(
                    (int) Math.round(screen_size.getWidth() * .8D),
                    (int) Math.round(screen_size.getHeight() * .8D));
        }
        setSize(frame_size);

        // Center frame on screen
        setLocation(((screen_size.width - frame_size.width) / 2),
                ((screen_size.height - frame_size.height) / 2));

        // Show frame
        setVisible(true);
        
        // Open up navigator tool initially.
        desktop.openToolWindow(NavigatorWindow.NAME);
    }
    
    /**
     * Gets the main desktop panel.
     *
     * @return desktop
     */
    public MainDesktopPane getDesktop() {
        return desktop;
    }

    /**
     * Load a previously saved simulation.
     */
    public void loadSimulation() {
        if ((loadSimThread == null) || !loadSimThread.isAlive()) {
        	loadSimThread = new Thread() {
        		public void run() {
        			loadSimulationProcess();
        		}
        	};
        	loadSimThread.start();
        }
        else {
        	loadSimThread.interrupt();
        }
    }
    
    /**
     * Performs the process of loading a simulation.
     */
    private void loadSimulationProcess() {
    	try {
			JFileChooser chooser = new JFileChooser(Simulation.DEFAULT_DIR);
            chooser.setDialogTitle("Select stored simulation");
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            	desktop.openAnnouncementWindow("Loading simulation...");
            	MasterClock clock = Simulation.instance().getMasterClock();
            	clock.loadSimulation(chooser.getSelectedFile());
            	while (clock.isLoadingSimulation()) Thread.sleep(100L);
            	desktop.resetDesktop();
            	desktop.disposeAnnouncementWindow();
            	
            	// Open navigator tool after loading.
            	desktop.openToolWindow(NavigatorWindow.NAME);
            }
		}
		catch(Exception e) {
	        JOptionPane.showMessageDialog(null, "Problem loading simulation",
				e.toString(), JOptionPane.ERROR_MESSAGE);
	        System.err.println("Problem loading simulation: " + e);
	        e.printStackTrace();
	    }
    }

    /**
     * Create a new simulation.
     */
    public void newSimulation() {
	    if ((newSimThread == null) || !newSimThread.isAlive()) {
	    	newSimThread = new Thread() {
	        	public void run() {
	        		newSimulationProcess();
	        	}
	        };
	        newSimThread.start();
	    }
	    else {
	    	newSimThread.interrupt();
	    }
    }
    
    /**
     * Performs the process of creating a new simulation.
     *
     */
    private void newSimulationProcess() {
    	try {
			desktop.openAnnouncementWindow("Creating new simulation...");
			Simulation.createNewSimulation();
			desktop.resetDesktop();
			desktop.disposeAnnouncementWindow();
			
			// Open navigator tool after creating new simulation.
			desktop.openToolWindow(NavigatorWindow.NAME);
		}
		catch(Exception e) {
			System.err.println("Problem creating new simulation: " + e);
			e.printStackTrace(System.err);
		}
    }

    /**
     * Save the current simulation. This display a FileChooser to select the
     * location to save the simulation if the default is not to be used.
     *
     * @param useDefault Should the user be allowed to override location.
     */
    public void saveSimulation(final boolean useDefault) {
	    if ((saveSimThread == null) || !saveSimThread.isAlive()) {
	    	saveSimThread = new Thread() {
	        	public void run() {
	        		saveSimulationProcess(useDefault);
	        	}
	        };
	        saveSimThread.start();
	    }
	    else {
	    	saveSimThread.interrupt();
	    }
    }
    
    /**
     * Performs the process of saving a simulation.
     */
    private void saveSimulationProcess(boolean useDefault) {
    	try {
    		File fileLocn = null;
    		
    		if (!useDefault) {
    			JFileChooser chooser = new JFileChooser(Simulation.DEFAULT_DIR);
    			chooser.setDialogTitle("Select save location");
    			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) 
    				fileLocn = chooser.getSelectedFile();
    			else return;
    		}
            
    		desktop.openAnnouncementWindow("Saving simulation...");
            MasterClock clock = Simulation.instance().getMasterClock();
            clock.saveSimulation(fileLocn);
            while (clock.isSavingSimulation()) Thread.sleep(100L);
            desktop.disposeAnnouncementWindow();
		}
		catch(Exception e) {
	        JOptionPane.showMessageDialog(null, "Problem saving simulation",
				e.toString(), JOptionPane.ERROR_MESSAGE);
	        System.err.println("Problem saving simulation: " + e);
	        e.printStackTrace();
	    }
    }
    
    /**
     * Pauses the simulation and opens an announcement window.
     */
    public void pauseSimulation() {
    	desktop.openAnnouncementWindow("Pausing simulation");
    	Simulation.instance().getMasterClock().setPaused(true);
    }
    
    /**
     * Closes the announcement window and unpauses the simulation.
     */
    public void unpauseSimulation() {
    	Simulation.instance().getMasterClock().setPaused(false);
    	desktop.disposeAnnouncementWindow();
    }
    
    /** 
     * Create a new unit button in toolbar.
     *
     * @param unit the unit the button is for.
     */
    public void createUnitButton(Unit unit) {
        unitToolbar.createUnitButton(unit);
    }
    
    /** 
     * Disposes a unit button in toolbar.
     *
     * @param unit the unit to dispose.
     */
    public void disposeUnitButton(Unit unit) {
        unitToolbar.disposeUnitButton(unit);
    }

    /**
     * Exit the simulation for running and exit.
     */
    public void exitSimulation() {
    	// System.out.println("Exiting simulation");
    	Simulation sim = Simulation.instance();
        try {
        	sim.getMasterClock().saveSimulation(null);
        }
        catch(Exception e) {
            System.err.println("Problem saving simulation " + e);
            e.printStackTrace(System.err);
        }

        sim.getMasterClock().exitProgram();
    }
    
    /**
     * Sets the look and feel of the UI
     * @param nativeLookAndFeel true if native look and feel should be used.
     */
    public void setLookAndFeel(boolean nativeLookAndFeel) {
    	try {
    		if (nativeLookAndFeel) {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    		}
    		else {
				MetalLookAndFeel.setCurrentTheme(new MarsTheme());
				UIManager.setLookAndFeel(new MetalLookAndFeel());
    		}
			SwingUtilities.updateComponentTreeUI(this);
			if (desktop != null) desktop.updateToolWindowLF();
    	}
    	catch (Exception e) {
			e.printStackTrace(System.err);
    	}
    }
}