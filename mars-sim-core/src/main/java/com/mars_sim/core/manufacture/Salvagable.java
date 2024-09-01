/**
 * Mars Simulation Project
 * Salvagable.java
 * @date 2024-08-10
 * @author Scott Davis
 */

package com.mars_sim.core.manufacture;

/**
 * An interface for salvagable items.
 */
public interface Salvagable {

    /**
     * Checks if the item is salvaged.
     * @return true if salvaged.
     */
    public boolean isSalvaged();
    
    /**
     * Indicates the start of a salvage process on the item.
     * 
     * @param info the salvage process info.
     * @param settlement the settlement where the salvage is taking place.
     */
    public void startSalvage(SalvageProcessInfo info, int settlement);
    
    /**
     * Gets the salvage info.
     * 
     * @return salvage info or null if item not salvaged.
     */
    public SalvageInfo getSalvageInfo();
}
