/**
 * Mars Simulation Project
 * NumberCellRenderer.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package com.mars_sim.ui.swing;

import java.awt.Component;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * The NumberCellRenderer class renders table cells containing Number objects.
 */
public class NumberCellRenderer extends DefaultTableCellRenderer {

	/** default serial id. */
	private static final long serialVersionUID = 1L;


	private static final Map<Integer,DecimalFormat> FORMATS = new HashMap<>();
	
	private String base = "#,###,##0";
	
	private DecimalFormat formatter;
	
	/**
	 * Constructor.
	 */
	public NumberCellRenderer() {
		this(1, null);
	}

	/**
	 * Constructor.
	 */
	public NumberCellRenderer(int decimal, boolean showDecimalPlace) {
		this(decimal, null);
	}

	/**
	 * Constructor.
	 */
	public NumberCellRenderer(int decimal) {
		this(decimal, null);
	}
	
	/**
	 * Constructor.
	 */
	public NumberCellRenderer(int digits, String preSymbol) {
		// Use DefaultTableCellRenderer constructor
		super();
		formatter = FORMATS.get(digits);
		if (formatter == null) {
			
			if (preSymbol != null) {
				base = preSymbol + base;
			}
			// Build the format string by adding zero's to the BASE
			StringBuilder format = new StringBuilder(base);
			if (digits > 0) {
				format.append('.');
				for(int i = 0; i < digits; i++) {
					format.append('0');
				}
			}
			formatter = new DecimalFormat(format.toString());
			FORMATS.put(digits, formatter);
		}

		// Set the horizontal alignment to right.
		setHorizontalAlignment(SwingConstants.RIGHT);
	}


    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
        JLabel cell = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row,
                column);

        String text = "";
		if (value != null) {
			try {
				text = formatter.format(value);
			}
			catch (IllegalArgumentException e) {
				System.err.println("Cell is not a number table =" + table.getModel().getClass().getName()
									+ " row=" + row + ", col=" + column);
			}
		}
		cell.setText(text);
        return cell;
    }
}
