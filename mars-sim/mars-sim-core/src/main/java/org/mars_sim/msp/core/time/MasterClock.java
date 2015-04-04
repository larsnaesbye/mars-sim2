/**
 * Mars Simulation Project
 * MasterClock.java
 * @version 3.08 2015-04-02
 * @author Scott Davis
 */

package org.mars_sim.msp.core.time;

import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationConfig;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The MasterClock represents the simulated time clock on virtual
 * Mars. Virtual Mars has only one master clock. The master clock
 * delivers a clock pulse the virtual Mars every second or so, which
 * represents a pulse of simulated time.  All actions taken with
 * virtual Mars and its units are synchronized with this clock pulse.
 * <p/>
 * Update: The pulse is now tied to the system clock. This means that each time
 * a timePulse is generated, it is the following length:
 * <p/>
 * (realworldseconds since last call ) * timeRatio
 * <p/>
 * update: with regard to pauses..
 * <p/>
 * they work. the sim will completely pause when setPause(true) is called, and will
 * resume with setPause(false);
 * However ! Do not make any calls to System.currenttimemillis(), instead use
 * uptimer.getuptimemillis(), as this is "shielded" from showing any passed time
 * while the game is paused. Thank you.
 */
public class MasterClock implements Serializable { // Runnable,

	/** default serial id. */
	static final long serialVersionUID = -1688463735489226493L;

	/** Initialized logger. */
	private static Logger logger = Logger.getLogger(MasterClock.class.getName());

	/** Clock thread sleep time (milliseconds) --> 25Hz should be sufficient. */
	private static long SLEEP_TIME = 40L;

	// Data members
	/** Runnable flag. */
	private transient volatile boolean keepRunning;
	/** Pausing clock. */
	private transient volatile boolean isPaused = false;
	/** Simulation/real-time ratio. */
	private volatile double timeRatio = 0D;
	/** Flag for loading a new simulation. */
	private transient volatile boolean loadSimulation;
	/** Flag for saving a simulation. */
	private transient volatile boolean saveSimulation;
	/** Flag for auto-saving a simulation. */
	private transient volatile boolean autosaveSimulation;
	/** Flag for ending the simulation program. */
	private transient volatile boolean exitProgram;

	private long totalPulses = 1;
	private transient long elapsedlast;
	private transient long elapsedMilliseconds;

	/** Clock listeners. */
	private transient List<ClockListener> listeners;
	private transient List<ClockListenerTask> clockListenerTaskList =  new ArrayList<ClockListenerTask>();

	/** Martian Clock. */
	private MarsClock marsTime;
	/** Initial Martian time. */
	private MarsClock initialMarsTime;
	/** Earth Clock. */
	private EarthClock earthTime;
	/** Uptime Timer. */
	private UpTimer uptimer;
	/** The file to save or load the simulation. */
	private transient volatile File file;

	private ClockThreadTask clockThreadTask;

	private transient ThreadPoolExecutor clockListenerExecutor;

    /**
     * Constructor
     *
     * @throws Exception if clock could not be constructed.
     */
    public MasterClock() {
        // Initialize data members
        SimulationConfig config = SimulationConfig.instance();

        // Create a Martian clock
        marsTime = new MarsClock(config.getMarsStartDateTime());
        initialMarsTime = (MarsClock) marsTime.clone();

        // Create an Earth clock
        earthTime = new EarthClock(config.getEarthStartDateTime());

        // Create an Uptime Timer
        uptimer = new UpTimer();

        // Create listener list.
        listeners = Collections.synchronizedList(new ArrayList<ClockListener>());
        elapsedlast = uptimer.getUptimeMillis();
        elapsedMilliseconds = 0L;

        //setupClockListenerTask();

        clockThreadTask = new ClockThreadTask();
    }

    /**
     * Returns the Martian clock
     *
     * @return Martian clock instance
     */
    public MarsClock getMarsClock() {
        return marsTime;
    }

    /**
     * Gets the initial Mars time at the start of the simulation.
     *
     * @return initial Mars time.
     */
    public MarsClock getInitialMarsTime() {
        return initialMarsTime;
    }

    /**
     * Returns the Earth clock
     *
     * @return Earth clock instance
     */
    public EarthClock getEarthClock() {
        return earthTime;
    }

    /**
     * Returns uptime timer
     *
     * @return uptimer instance
     */
    public UpTimer getUpTimer() {
        return uptimer;
    }

    /**
     * Adds a clock listener
     * @param newListener the listener to add.
     */
    // 2015-04-02 Modified addClockListener()
    public final void addClockListener(ClockListener newListener) {
        // if listeners list does not exist, create one
    	if (listeners == null) listeners = Collections.synchronizedList(new ArrayList<ClockListener>());
        // if the listeners list does not contain newListener, add it to the list
    	if (!listeners.contains(newListener)) listeners.add(newListener);
    	// will check if clockListenerTaskList already contain the newListener's task, if it doesn't, create one
    	addClockListenerTask(newListener);
      }


    /**
     * Removes a clock listener
     * @param oldListener the listener to remove.
     */
    // 2015-04-02 Modified removeClockListener()
    public final void removeClockListener(ClockListener oldListener) {
        if (listeners == null) listeners = Collections.synchronizedList(new ArrayList<ClockListener>());
        if (listeners.contains(oldListener)) listeners.remove(oldListener);
       	// Check if clockListenerTaskList contain the newListener's task, if it does, delete it
        ClockListenerTask task = retrieveClockListenerTask(oldListener);
        if (task != null) clockListenerTaskList.remove(task);
    }

    /**
     * Adds a clock listener task
     *
     * @param newListener the clock listener task to add.
     */
    // 2015-04-02 addClockListenerTask()
    public void addClockListenerTask(ClockListener listener) {
    	boolean hasIt = false;
    	//startClockListenerExecutor();
    	if (clockListenerTaskList == null)
    		clockListenerTaskList =  new ArrayList<ClockListenerTask>();
    	Iterator<ClockListenerTask> i = clockListenerTaskList.iterator();
    	while (i.hasNext()) {
    		ClockListenerTask c = i.next();
    		if (c.getClockListener().equals(listener))
    			hasIt = true;
    	}
    	if (!hasIt) {
	    	clockListenerTaskList.add(new ClockListenerTask(listener));
    	}
    }

    /**
     * Retrieve a clock listener task
     * @param oldListener the clock listener task to remove.
     */
    // 2015-04-02 retrieveClockListenerTask()
    public ClockListenerTask retrieveClockListenerTask(ClockListener oldListener) {
/*     	ClockListenerTask c = null;
    	clockListenerTaskList.forEach(t -> {
    		ClockListenerTask l = c;
    		if (t.getClockListener().equals(oldListener))
    			l = t;
    	});
*/
    	ClockListenerTask t = null;
    	Iterator<ClockListenerTask> i = clockListenerTaskList.iterator();
    	while (i.hasNext()) {
    		ClockListenerTask c = i.next();
    		if (c.getClockListener().equals(oldListener))
    		 t = c;
    	}
		return t;
    }

    /**
     * Sets the load simulation flag and the file to load from.
     *
     * @param file the file to load from.
     */
    public void loadSimulation(File file) {
        this.setPaused(false);
        loadSimulation = true;
        this.file = file;
    }

    /**
     * Checks if in the process of loading a simulation.
     *
     * @return true if loading simulation.
     */
    public boolean isLoadingSimulation() {
        return loadSimulation;
    }

    /**
     * Sets the save simulation flag and the file to save to.
     *
     * @param file save to file or null if default file.
     */
    public void saveSimulation(File file) {
        saveSimulation = true;
        this.file = file;
    }

    /**
     * Sets the autosave simulation flag and the file to save to.
     *
     * @param file autosave to file or null if default file.
     */
    // 2015-01-08 Added autosaveSimulation
    public void autosaveSimulation(File file) {
        autosaveSimulation = true;
        this.file = file;
    }

    /**
     * Checks if in the process of saving a simulation.
     *
     * @return true if saving simulation.
     */
    public boolean isSavingSimulation() {
        return saveSimulation;
    }

    /**
     * Checks if in the process of autosaving a simulation.
     *
     * @return true if autosaving simulation.
     */
    // 2015-01-08 Added isAutosavingSimulation
    public boolean isAutosavingSimulation() {
        return autosaveSimulation;
    }

    /**
     * Sets the exit program flag.
     */
    public void exitProgram() {
        this.setPaused(true);
        exitProgram = true;
    }

    /**
     * Gets the time pulse length
     * in other words, the number of realworld seconds that have elapsed since it was last called
     *
     * @return time pulse length in millisols
     * @throws Exception if time pulse length could not be determined.
     */
    public double getTimePulse() {

        // Get time ratio from simulation configuration.
        if (timeRatio == 0) setTimeRatio(SimulationConfig.instance().getSimulationTimeRatio());

        double timePulse;
        if (timeRatio > 0D) {
            double timePulseSeconds = ((double) getElapsedmillis() * (timeRatio / 1000D));
            timePulse = MarsClock.convertSecondsToMillisols(timePulseSeconds);
        }
        else timePulse = 1D;

        return timePulse;
    }

    public long getTotalPulses() {
        return totalPulses;
    }

    /**
     * setTimeRatio is for setting the Masterclock's time ratio directly. It is a double
     * indicating the simetime:realtime ratio. 1000 means 1000 sim time minutes elapse for
     * each real-world minute.
     */
    public void setTimeRatio(double ratio) {
        if (ratio >= 0.0001D && ratio <= 500000D) {
            timeRatio = ratio;
        }
        else throw new IllegalArgumentException("Time ratio out of bounds ");
    }

    /**
     * Gets the real-time/simulation ratio.
     *
     * @return ratio
     */
    public double getTimeRatio() {
        return timeRatio;
    }

    /**
     * Returns the instance of ClockThreadTask
     * @return ClockThreadTask
     */
    public ClockThreadTask getClockThreadTask() {
    	return clockThreadTask;
    }

    /**
     * Runs master clock's thread using ThreadPoolExecutor
     */
    class ClockThreadTask implements Runnable, Serializable {

		private static final long serialVersionUID = 1L;

		private ClockThreadTask() {
		}

		@Override
		public void run() {

	        keepRunning = true;
	        long lastTimeDiff;
	        elapsedlast = uptimer.getUptimeMillis();

	        // Keep running until told not to
	        while (keepRunning) {

	            // Pause simulation to allow other threads to complete.
	            try {
	                Thread.yield();
	                Thread.sleep(SLEEP_TIME);
	            }
	            catch (Exception e) {
	                logger.log(Level.WARNING, "Problem with Thread.yield() in MasterClock.run() ", e);
	            }

	            if (!isPaused) {

	                // Update elapsed milliseconds.
	                updateElapsedMilliseconds();

	                // Get the time pulse length in millisols.
	                double timePulse = getTimePulse();

	                // Incrementing total time pulse number.
	                totalPulses++;

	                long startTime = System.nanoTime();

	                // Add time pulse length to Earth and Mars clocks.
	                double earthTimeDiff = getElapsedmillis() * timeRatio / 1000D;

	                // TODO : if null
	                //if (earthTime == null)
	                //	earthTime = Simulation.instance().getMasterClock().getEarthClock();
	                if (keepRunning)
	                	earthTime.addTime(earthTimeDiff);

	                marsTime.addTime(timePulse);

	  		  		if (!clockListenerExecutor.isTerminating() || !clockListenerExecutor.isTerminated() || !clockListenerExecutor.isShutdown() )
	  		  			fireClockPulse(timePulse);

	                long endTime = System.nanoTime();
	                lastTimeDiff = (long) ((endTime - startTime) / 1000000D);

	                logger.finest("Pulse #" + totalPulses + " time: " + lastTimeDiff + " ms");
	            }

	            if (saveSimulation) {
	                // Save the simulation to a file.
	                try {
	                    Simulation.instance().saveSimulation(file, false);
	                } catch (IOException e) {

	                    logger.log(Level.SEVERE, "Could not save the simulation with file = "
	                            + (file == null ? "null" : file.getPath()), e);
	                    e.printStackTrace();
	                }
	                saveSimulation = false;
	            }

	            else if (autosaveSimulation) {
	                // Autosave the simulation to a file.
	                try {
	                    Simulation.instance().saveSimulation(file, true);
	                } catch (IOException e) {

	                    logger.log(Level.SEVERE, "Could not autosave the simulation with file = "
	                            + (file == null ? "null" : file.getPath()), e);
	                    e.printStackTrace();
	                }
	                autosaveSimulation = false;
	            }

	            else if (loadSimulation) {
	                // Load the simulation from a file.
	                if (file.exists() && file.canRead()) {
	                    Simulation.instance().loadSimulation(file);
	                    Simulation.instance().start();
	                }
	                else {
	                    logger.warning("Cannot access file " + file.getPath() + ", not reading");
	                }
	                loadSimulation = false;
	            }

	            // Exit program if exitProgram flag is true.
	            if (exitProgram) {
	                exitProgram = false;
	                System.exit(0);
	            }
	        }
	    } // end of run
    }


    /**
     * Run clock

    public void run() {

        keepRunning = true;
        long lastTimeDiff;
        elapsedlast = uptimer.getUptimeMillis();

        // Keep running until told not to
        while (keepRunning) {

            // Pause simulation to allow other threads to complete.
            try {
                Thread.yield();
                Thread.sleep(SLEEP_TIME);
            }
            catch (Exception e) {
                logger.log(Level.WARNING, "Problem with Thread.yield() in MasterClock.run() ", e);
            }

            if (!isPaused) {

                // Update elapsed milliseconds.
                updateElapsedMilliseconds();

                // Get the time pulse length in millisols.
                double timePulse = getTimePulse();

                // Incrementing total time pulse number.
                totalPulses++;

                long startTime = System.nanoTime();

                // Add time pulse length to Earth and Mars clocks.
                double earthTimeDiff = getElapsedmillis() * timeRatio / 1000D;

                earthTime.addTime(earthTimeDiff);
                marsTime.addTime(timePulse);

                fireClockPulse(timePulse);

                long endTime = System.nanoTime();
                lastTimeDiff = (long) ((endTime - startTime) / 1000000D);

                logger.finest("Pulse #" + totalPulses + " time: " + lastTimeDiff + " ms");
            }

            if (saveSimulation) {
                // Save the simulation to a file.
                try {
                    Simulation.instance().saveSimulation(file, false);
                } catch (IOException e) {

                    logger.log(Level.SEVERE, "Could not save the simulation with file = "
                            + (file == null ? "null" : file.getPath()), e);
                    e.printStackTrace();
                }
                saveSimulation = false;
            }

            else if (autosaveSimulation) {
                // Autosave the simulation to a file.
                try {
                    Simulation.instance().saveSimulation(file, true);
                } catch (IOException e) {

                    logger.log(Level.SEVERE, "Could not autosave the simulation with file = "
                            + (file == null ? "null" : file.getPath()), e);
                    e.printStackTrace();
                }
                autosaveSimulation = false;
            }

            else if (loadSimulation) {
                // Load the simulation from a file.
                if (file.exists() && file.canRead()) {
                    Simulation.instance().loadSimulation(file);
                    Simulation.instance().start();
                }
                else {
                    logger.warning("Cannot access file " + file.getPath() + ", not reading");
                }
                loadSimulation = false;
            }

            // Exit program if exitProgram flag is true.
            if (exitProgram) {
                exitProgram = false;
                System.exit(0);
            }
        }
    }
*/
    /**
     * Looks at the clock listener list and checks if each listener has already had a corresponding task in the clock listener task list.
     */
    // 2015-04-02 setupClockListenerTask()
    public void setupClockListenerTask() {
		listeners.forEach(t -> {
			// Check if it has a corresponding task or not, if it doesn't, create a task for t
			addClockListenerTask(t);
		});
    }

    /**
     * Prepare clocklistener tasks for setting up threads.
     */
    // 2015-04-02 Added ClockListenerTask
	class ClockListenerTask implements Runnable {

		//long SLEEP_TIME = 1;
		double time;
		private ClockListener listener;

		protected ClockListener getClockListener() {
			return listener;
		}

		private ClockListenerTask(ClockListener listener) {
			//logger.info(Msg.getString("MainDesktopPane.toolWindow.thread.running")); //$NON-NLS-1$
			this.listener = listener;

		}

		public void addTime(double time) {
			this.time = time;
		}

		@Override
		public void run() {
			try {
				//while (!clockListenerExecutor.isTerminated()){
				listener.clockPulse(time);
				//	TimeUnit.SECONDS.sleep(SLEEP_TIME);
				//}
			} catch (ConcurrentModificationException e) {} //Exception e) {}
		}
	}


    /**
     * Fires the clock pulse to each clock listener
     */
    // 2015-04-02 Modified fireClockPulse() to make use of ThreadPoolExecutor
	public void fireClockPulse(double time) {
		// java 8 internal iterator style
		//listeners.forEach(cl -> cl.clockPulse(time));

/*	      synchronized (listeners) {
	            Iterator<ClockListener> i = listeners.iterator();
	            while (i.hasNext()) {
	                ClockListener cl = i.next();
	                try {
	                    cl.clockPulse(time);
	                    try {
	                        Thread.yield();
	                    }
	                    catch (Exception e) {
	                        logger.log(Level.WARNING, "Problem with Thread.yield() in MasterClock.run() ", e);
	                    }
	                } catch (Exception e) {
	            		throw new IllegalStateException("Error while firing clock pulse", e);
	                }
	            }
	       }

	  //if (clockListenerTaskList.isEmpty())
		setupClockListenerTask();
*/
		if (!clockListenerTaskList.isEmpty() || clockListenerTaskList != null)
			// run all clockListener Tasks
		  	clockListenerTaskList.forEach(t -> {
				// TODO: check if the thread for t is running
		  			try {
		  		  		if ( t != null || !clockListenerExecutor.isTerminating() || !clockListenerExecutor.isTerminated() || !clockListenerExecutor.isShutdown() ) {
			  		  		t.addTime(time);
		  		  			clockListenerExecutor.execute(t);
		  		  		}
		  		  		else
		  		  			return;
		  				//}
	                } catch (Exception e) {
	            		//throw new IllegalStateException("Error while firing clock pulse", e);
	                }

			});

	  	//endClockListenerExecutor();

    }

    /**
     * Stop the clock
     */
	// called by stop() in Simulation.java
    public void stop() {
        keepRunning = false;
    }

    public void restart() {
        keepRunning = true;
    }

    /**
     * Set if the simulation is paused or not.
     *
     * @param isPaused true if simulation is paused.
     */
    public void setPaused(boolean isPaused) {
        uptimer.setPaused(isPaused);
    	//if (isPaused) System.out.println("MasterClock.java : setPaused() : isPause is true");
        this.isPaused = isPaused;
        // Fire pause change to all clock listeners.
        firePauseChange();
    }

    /**
     * Checks if the simulation is paused or not.
     *
     * @return true if paused.
     */
    public boolean isPaused() {
        return isPaused;
    }

    /**
     * Send a pulse change event to all clock listeners.
     */
    public void firePauseChange() {

        listeners.forEach(cl -> cl.pauseChange(isPaused));

/*
        synchronized (listeners) {
            Iterator<ClockListener> i = listeners.iterator();
            while (i.hasNext()) {
                ClockListener cl = i.next();
                try {
                    cl.pauseChange(isPaused);
                } catch (Exception e) {
                    throw new IllegalStateException("Error while firing pase change", e);
                }
            }
        }
*/
    }

    public double getPulsesPerSecond() {
        //System.out.println("pulsespersecond: "+((double) totalPulses / (uptimer.getUptimeMillis()/1000 ) ));
        return ((double) totalPulses / (uptimer.getUptimeMillis() / 1000D));
    }

    /**
     * Update the milliseconds elapsed since last time pulse.
     */
    private void updateElapsedMilliseconds() {
    	if ( uptimer == null) {
    		//System.out.println("MasterClock : uptimer == null");
    		uptimer = new UpTimer();
    	}
        long tnow = uptimer.getUptimeMillis();
        elapsedMilliseconds = tnow - elapsedlast;
        elapsedlast = tnow;
        //System.out.println("getElapsedmilliseconds " + elapsedMilliseconds);
    }

    private long getElapsedmillis() {
        return elapsedMilliseconds;
    }

    public static final int secspmin = 60, secsphour = 3600, secspday = 86400, secsperyear = 31536000;


    /**
     * the following is a utility. It may be slow. It returns a string in YY:DDD:HH:MM:SS.SSS format
     * note: it is set up currently to only return hh:mm:ss.s
     */
    public String getTimeString(double seconds) {

        long years = (int) Math.floor(seconds / secsperyear);
        long days = (int) ((seconds % secsperyear) / secspday);
        long hours = (int) ((seconds % secspday) / secsphour);
        long minutes = (int) ((seconds % secsphour) / secspmin);
        double secs = (seconds % secspmin);

        StringBuilder b = new StringBuilder();

        b.append(years);
        if(years>0){
            b.append(":");
        }

        if (days > 0) {
            b.append(String.format("%03d", days)).append(":");
        } else {
            b.append("0:");
        }

        if (hours > 0) {
            b.append(String.format("%02d", hours)).append(":");
        } else {
            b.append("00:");
        }

        if (minutes > 0) {
            b.append(String.format("%02d", minutes)).append(":");
        } else {
            b.append("00:");
        }

        b.append(String.format("%5.3f", secs));

        return b.toString();
    }

    /**
     * Starts clock listener thread pool executor
     */
    public void startClockListenerExecutor() {
    	//if ( clockListenerExecutor.isTerminated() || clockListenerExecutor.isShutdown() )
    	if (clockListenerExecutor == null)
    		clockListenerExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
//Executors.newSingleThreadExecutor();// Executors.newFixedThreadPool(1); // newCachedThreadPool(); //
    }

    /**
     * Shuts down clock listener thread pool executor
     */
    public void endClockListenerExecutor() {
    	clockListenerExecutor.shutdown();
/*
    	if ( clockListenerExecutor.isTerminating() )
			try {
				TimeUnit.MILLISECONDS.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

    	if ( clockListenerExecutor.isTerminated() || !clockListenerExecutor.isShutdown() )
    		///

*/
    }
    /**
     * Prepare object for garbage collection.
     */
    public void destroy() {
       marsTime = null;
       initialMarsTime = null;
       earthTime = null;
       uptimer = null;
       listeners.clear();
       listeners = null;
       file = null;
       clockListenerExecutor = null;
    }
}