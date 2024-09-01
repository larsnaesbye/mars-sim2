/*
 * Mars Simulation Project
 * FoodInventoryTab.java
 * @date 2022-05-27
 * @author Manny Kung
 */
package com.mars_sim.ui.swing.tool.monitor;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.mars_sim.ui.swing.NumberCellRenderer;

/**
 * This class represents an inventory of food at settlements displayed within
 * the Monitor Window.
 */
@SuppressWarnings("serial")
public class FoodInventoryTab extends TableTab {

	private static final String FOOD_ICON = "food";
	
	private NumberCellRenderer currency = new NumberCellRenderer(2, "$ ");
	
	/**
	 * constructor.
	 *
	 * @param window {@link MonitorWindow} the containing window.
	 */
	public FoodInventoryTab(final MonitorWindow window) {
		// Use TableTab constructor
		super(window, new FoodInventoryTableModel(), true, false, FOOD_ICON);
	
		TableColumnModel m = table.getColumnModel();
		for (int i= FoodInventoryTableModel.NUM_INITIAL_COLUMNS; i < m.getColumnCount(); i++) {
			TableCellRenderer r;
			switch(i) {
				case FoodInventoryTableModel.COST_COL:
				case FoodInventoryTableModel.PRICE_COL:
					r = currency; //NumberRenderer.getCurrencyRenderer();
					break;

				case FoodInventoryTableModel.MASS_COL:
					r = DIGIT2_RENDERER;
					break;

				default:
					r = DIGIT3_RENDERER;
			}

			m.getColumn(i).setCellRenderer(r);
		}
		
		adjustColumnWidth(table);
	}
}
