/*
 * Mars Simulation Project
 * UnitIconMapLayer.java
 * @date 2023-04-28
 * @author Scott Davis
 */

package com.mars_sim.ui.swing.tool.map;

import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

import com.mars_sim.core.Unit;
import com.mars_sim.core.map.Map;
import com.mars_sim.core.map.MapMetaData;
import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.map.location.IntPoint;
import com.mars_sim.ui.swing.unit_display_info.UnitDisplayInfo;
import com.mars_sim.ui.swing.unit_display_info.UnitDisplayInfoFactory;

/**
 * The UnitMapLayer is a graphics layer to display unit icons.
 */
public class UnitIconMapLayer extends UnitMapLayer {

	private Component displayComponent;

	public UnitIconMapLayer(Component displayComponent) {
		this.displayComponent = displayComponent;
	}

	/**
	 * Displays a unit on the map.
	 * 
	 * @param unit      the unit to display.
	 * @param mapCenter the location center of the map.
	 * @param baseMap   the type of map.
	 * @param g         the graphics context.
	 */
	protected void displayUnit(Unit unit, Coordinates mapCenter, Map baseMap, Graphics g) {

		IntPoint location = MapUtils.getRectPosition(unit.getCoordinates(), mapCenter, baseMap);
		UnitDisplayInfo displayInfo = UnitDisplayInfoFactory.getUnitDisplayInfo(unit);

		IntPoint imageLocation = getUnitDrawLocation(location, displayInfo.getMapIcon(unit, baseMap.getMapMetaData()));
		int locX = imageLocation.getiX();
		int locY = imageLocation.getiY();

		if (!(displayInfo.isMapBlink(unit) && getBlinkFlag())) {
			MapMetaData mapType = baseMap.getMapMetaData();
			Icon displayIcon = displayInfo.getMapIcon(unit, mapType);	
			if (g != null)
				displayIcon.paintIcon(displayComponent, g, locX, locY);
		}
	}

	/**
	 * Gets the unit image draw position on the map image.
	 *
	 * @param unitPosition absolute unit position
	 * @param unitIcon     unit's map image icon
	 * @return draw position for unit image
	 */
	private IntPoint getUnitDrawLocation(IntPoint unitPosition, Icon unitIcon) {

		int unitX = unitPosition.getiX();
		int unitY = unitPosition.getiY();
		int iconHeight = unitIcon.getIconHeight();
		int iconWidth = unitIcon.getIconWidth();

		return new IntPoint(unitX - (iconWidth / 2), unitY - (iconHeight / 2));
	}
}
