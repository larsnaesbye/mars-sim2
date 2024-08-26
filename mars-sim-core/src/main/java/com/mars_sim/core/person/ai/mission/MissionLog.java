/*
 * Mars Simulation Project
 * MissionLog.java
 * @date 2024-07-14
 * @author Barry Evans
 */
package com.mars_sim.core.person.ai.mission;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.mars_sim.core.time.MarsTime;
import com.mars_sim.core.time.MasterClock;

/**
 * The class that holds all log details about a Mission.
 */
public class MissionLog implements Serializable  {

	private static final long serialVersionUID = 1L;

	/**
     * POJO class for a log entry.
     */
    public static class MissionLogEntry implements Serializable {
    	
        private static final long serialVersionUID = 1L;
        
        private MarsTime time;
        private String entry;
        
        private MissionLogEntry(MarsTime time, String entry) {
            super();
            this.time = time;
            this.entry = entry;
        }
    
        public MarsTime getTime() {
            return time;
        }
    
        public String getEntry() {
            return entry;
        }
    
        @Override
        public String toString() {
            return "MissionLogEntry [time=" + time + ", entry=" + entry + "]";
        }		
    }

    private List<MissionLogEntry> log = new ArrayList<>();
    private MarsTime dateEmbarked;
    private boolean done = false;
    protected static MasterClock clock;

    public void addEntry(String entry) {
		log.add(new MissionLogEntry(clock.getMarsTime(), entry));
    }

    /**
	 * Gets the date filed timestamp of the mission.
	 *
	 * @return
	 */
	public MarsTime getDateCreated() {
		if (!log.isEmpty()) {
			return log.get(0).getTime();
		}
		return null;
	}

    
	/**
	 * Gets the date embarked. This is after any preparation steps.
	 *
	 * @return
	 */
	public MarsTime getDateEmbarked() {
		return dateEmbarked;
	}

	/**
	 * Gets the date missions was finished.
	 *
	 * @return
	 */
	public MarsTime getDateFinished() {
		if (done && !log.isEmpty()) {
            // TODO SHould this be when teh mission returned to the Settlement? 
			return log.get(log.size()-1).getTime();
		}

		return null;
	}

    void setDone() {
        done = true;
    }

    /**
     * Generates the embarked date.
     */
	public void generatedDateEmbarked() {
		if (dateEmbarked == null) {
			dateEmbarked = clock.getMarsTime();
		}
	}

    public List<MissionLogEntry> getEntries() {
        return log;
    }

    public static void initialise(MasterClock mc) {
        clock = mc;
    }

    public MissionLogEntry getLastEntry() {
        if (log.isEmpty()) {
            return null;
        }
        return log.get(log.size()-1);
    }
}
