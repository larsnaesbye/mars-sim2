/*
 * Mars Simulation Project
 * ColumnSelector.java
 * @date 2021-09-20
 * @author Barry Evans
 */
package com.mars_sim.ui.swing.tool.monitor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.MarsPanelBorder;
import com.mars_sim.ui.swing.ModalInternalFrame;

/**
 * This window displays a list of columns from the specified model.
 * The columns displayed depends upon the final chart being rendered.
 */
@SuppressWarnings("serial")
public class ColumnSelector
extends ModalInternalFrame {

	private final static String PIE_MESSAGE = Msg.getString("ColumnSelector.singleColumn"); //$NON-NLS-1$
	private final static String BAR_MESSAGE = Msg.getString("ColumnSelector.multipleColumns"); //$NON-NLS-1$

	// Data members
	/** Check boxes. */
	private JList<?> columnList = null;
	private int columnMappings[] = null;
	private boolean okPressed = false;

	/**
	 * Constructs the dialog with a title and columns extracted from the
	 * specified model.
	 * @param desktop MainDesktopPane the owner of the dialog.
	 * @param model Model driving the columns.
	 * @param bar Display selection for a Bar chart.
	 */
	public ColumnSelector(MainDesktopPane desktop, MonitorModel model, boolean bar) {
		// Use ModalInternalFrame constructor
        super(model.getName());

		// Create main panel
        JPanel mainPane = new JPanel(new BorderLayout());
        setContentPane(mainPane);

		// Set the border.
		((JComponent) getContentPane()).setBorder(new MarsPanelBorder());

		// Add all valid columns into the list
		Vector<String> items = new Vector<String>();
		columnMappings = new int[model.getColumnCount()-1];
		for(int i = 1; i < model.getColumnCount(); i++) {
			// If a valid column then add to model.
			Class<?> columnType = model.getColumnClass(i);

			// If a bar, look for Number classes
			if (bar) {
				if (isNumber(columnType)) {
					columnMappings[items.size()] = i;
					items.add(model.getColumnName(i));
				}
			}
			else if (isCategory(columnType)) {
				columnMappings[items.size()] = i;
				items.add(model.getColumnName(i));
			}
		}

		// Center pane
		JPanel centerPane = new JPanel(new BorderLayout());
		centerPane.setBorder(BorderFactory.createRaisedBevelBorder());
		columnList = new JList<Object>(items);
		if (bar) {
			columnList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
			centerPane.add(new JLabel(BAR_MESSAGE), BorderLayout.NORTH);
		}
		else {
			columnList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			centerPane.add(new JLabel(PIE_MESSAGE), BorderLayout.NORTH);
		}
		centerPane.add(new JScrollPane(columnList), BorderLayout.CENTER);

		// Buttons
		JPanel buttonPanel = new JPanel(new FlowLayout());
		JButton okButton = new JButton(Msg.getString("ColumnSelector.button.ok")); //$NON-NLS-1$
		okButton.addActionListener(e -> {
				okPressed = true;
				setVisible(false);
				//setModal(false);
		});
		buttonPanel.add(okButton);
		JButton cancelButton = new JButton(Msg.getString("ColumnSelector.button.cancel")); //$NON-NLS-1$
		cancelButton.addActionListener(e -> {
				setVisible(false);
				//setModal(false);
		});
		buttonPanel.add(cancelButton);

		// Package dialog
		JPanel pane = new JPanel(new BorderLayout());
		pane.add(centerPane, BorderLayout.CENTER);
		pane.add(buttonPanel, BorderLayout.SOUTH);
		getContentPane().add(pane);

		setSize(300, 300);
		setPreferredSize(new Dimension (300, 300));

//		if (desktop.getMainScene() != null) {
//			Dimension desktopSize = desktop.getSize();
//		    Dimension size = this.getSize();
//		    int width = (desktopSize.width - size.width) / 2;
//		    int height = (desktopSize.height - size.height) / 4;
//		    setLocation(width, height);
//		}

        // Add to its own tab pane
        //if (desktop.getMainScene() != null)
        //	desktop.add(this);
        	//desktop.getMainScene().getDesktops().get(0).add(this);
        //else
        	desktop.add(this);

        setModal(true);
        moveToFront();
	    //setVisible(true);
		//pack();

	}

	/**
	 * Create a column selector popup for use with a Bar chart.
	 * @param window Parent frame.
	 * @param model Model containing columns.
	 * @return Array of column indexes to display.
	 */
	public static int[] createBarSelector(MainDesktopPane desktop,
			MonitorModel model) {
		ColumnSelector select = new ColumnSelector(desktop, model, true);
		select.setVisible(true);
		//select.setModal(true);
		//return select.getSelectedColumns();
		int columns[] = select.getSelectedColumns();
		//if (columns.length > 0) {
			return columns;
		//}
		//else {
		//	return columns;
		//}
	}

	/**
	 * Create a column selector popup for a Pie chart.
	 * @param window Parent frame.
	 * @param model Model containing columns.
	 * @return Column index to use as category.
	 */
	public static int createPieSelector(MainDesktopPane desktop,
			MonitorModel model) {
        //System.out.println("ColumnSelector.java : start calling createPieSelector ");
		ColumnSelector select = new ColumnSelector(desktop, model, false);
		//System.out.println("createPieSelector() : just done calling new ColumnSelector");
		select.setVisible(true);
		//System.out.println("createPieSelector() : just done calling setVisible(true)");
		//select.setModal(true);
		//System.out.println("createPieSelector() : just done calling setModal(true)");
		int column = select.getSelectedColumn();
		if (column >= 0) {
			//System.out.println("createPieSelector() : columns >= 0");
			return column;
		}
		else {
			//System.out.println("createPieSelector() : columns < 0");
			return -1;
		}
	}

	/**
	 * Return the list of selected columns. The return is the index value
	 * as described by the associated table model. The return array maybe
	 * zero or more depending on selection and mode.
	 * @return Array of TableColumn indexes of selection.
	 */
	public int[] getSelectedColumns() {
		int index[] = null;
		if (okPressed) {
			int selection[] = columnList.getSelectedIndices();
			index = new int[selection.length];

			for(int i = 0; i < selection.length; i++) {
				index[i] = columnMappings[selection[i]];
			}
		}
		else {
			index = new int[0];
		}

		return index;
	}

	/**
	 * Return the selected columns. The return is the index value
	 * as described by the associated table model. The return array maybe
	 * zero or more depending on selection and mode.
	 * @return TableColumn index of selection.
	 */
	public int getSelectedColumn() {
		int index = -1;
		if (okPressed) {
			int selected = columnList.getSelectedIndex();
			if (selected > -1)
				index = columnMappings[selected];
		}

		return index;
	}

	/**
	 * Is the class specified suitable to be displayed as a category dataset.
	 * This is basically any non-numerical class
	 * @param columnClass The Class of the data in the column.
	 * @return Is the class a category
	 */
	private boolean isCategory(Class<?> columnClass) {
		return (String.class.equals(columnClass)
				&& !Number.class.isAssignableFrom(columnClass)
				&& !Coordinates.class.equals(columnClass));
	}

	private boolean isNumber(Class<?> columnClass) {
		return (Number.class.isAssignableFrom(columnClass)
				&& !String.class.equals(columnClass));
	}
}
