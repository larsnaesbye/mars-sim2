/*
 * Mars Simulation Project
 * SalvagaInfo.java
 * @date 2024-08-10
 * @author Scott Davis
 */

package com.mars_sim.core.manufacture;

import com.mars_sim.core.Simulation;
import com.mars_sim.core.UnitManager;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.time.MarsTime;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Information about the salvage of a particular salvagable item.
 */
public class SalvageInfo implements Serializable {

	private static final long serialVersionUID = 1L;
    // Data members
    private int settlementID;

	private Salvagable item;
    private SalvageProcessInfo processInfo;
    private MarsTime startTime;
    private MarsTime finishTime;

    private Map<Integer, Integer> partsSalvaged;
    
	private static UnitManager unitManager = Simulation.instance().getUnitManager();

    /**
     * Constructor.
     * 
     * @param item the salvaged item.
     * @param processInfo the salvage process info.
     */
    public SalvageInfo(Salvagable item,  SalvageProcessInfo processInfo, int settlementID, 
                        MarsTime startTime) {
        this.item = item;
        this.processInfo = processInfo;
        this.settlementID = settlementID;
        this.startTime = startTime;
        finishTime = null;
        partsSalvaged = new HashMap<>();
    }
    
    /**
     * Finishes the salvage.
     * 
     * @param partsSalvaged a map of the parts salvaged and their number or an empty map if none.
     */
    public void finishSalvage(Map<Integer, Integer> partsSalvaged, MarsTime finishTime) {
        this.partsSalvaged = partsSalvaged;
        this.finishTime = finishTime;
    }
    
    /**
     * Gets the salvagable item.
     * 
     * @return item.
     */
    public Salvagable getItem() {
        return item;
    }
    
    /**
     * Gets the salvage process info.
     * 
     * @return process info.
     */
    public SalvageProcessInfo getProcessInfo() {
        return processInfo;
    }
    
    /**
     * Gets the time when the salvage process is started.
     * 
     * @return start time.
     */
    public MarsTime getStartTime() {
        return startTime;
    }
    
    /**
     * Gets the time when the salvage process is finished.
     * 
     * @return finish time or null if not finished yet.
     */
    public MarsTime getFinishTime() {
        return finishTime;
    }
    
    /**
     * Gets a map of the parts salvaged and their number from this item.
     * 
     * @return map of parts and their number or empty map if salvage not finished.
     */
    public Map<Integer, Integer> getPartsSalvaged() {
        return new HashMap<>(partsSalvaged);
    }
    
    /**
     * Gets the settlement where the salvage took or is taking place.
     * 
     * @return settlement
     */
    public Settlement getSettlement() {
        return unitManager.getSettlementByID(settlementID);
    }
	
    /**
     * Prepares object for garbage collection.
     */
    public void destroy() {
        item = null;
        processInfo = null;
        startTime = null;
        finishTime = null;
        partsSalvaged = null;
    }
}
