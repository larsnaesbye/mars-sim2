/*
 * Mars Simulation Project
 * ConstructionSettlementPanel.java
 * @date 2023-07-24
 * @author Scott Davis
 */

package com.mars_sim.ui.swing.tool.mission.create;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.mars_sim.core.CollectionUtils;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.core.vehicle.VehicleType;
import com.mars_sim.ui.swing.MarsPanelBorder;

/**
 * A wizard panel for selecting the mission's construction settlement.
 */
@SuppressWarnings("serial")
class ConstructionSettlementPanel extends WizardPanel {

	/** The wizard panel name. */
    private final static String NAME = "Construction Settlement";
    
    // Data members.
    private SettlementTableModel settlementTableModel;
    private JTable settlementTable;
    private JLabel errorMessageLabel;
    
    /**
     * Constructor.
     * 
     * @param wizard the create mission wizard.
     */
    public ConstructionSettlementPanel(final CreateMissionWizard wizard) {
        // Use WizardPanel constructor.
        super(wizard);
        
        // Set the layout.
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        // Set the border.
        setBorder(new MarsPanelBorder());
        
        // Create the select settlement label.
        JLabel selectSettlementLabel = createTitleLabel(Msg.getString("ConstructionSettlementPanel.title")); //$NON-NLS-1$
        add(selectSettlementLabel);
        
        // Create the settlement panel.
        JPanel settlementPane = new JPanel(new BorderLayout(0, 0));
        settlementPane.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));
        settlementPane.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(settlementPane);
        
        // Create scroll panel for settlement list.
        JScrollPane settlementScrollPane = new JScrollPane();
        settlementPane.add(settlementScrollPane, BorderLayout.CENTER);
        
        // Create the settlement table model.
        settlementTableModel = new SettlementTableModel();
        
        // Create the settlement table
        settlementTable = new JTable(settlementTableModel);
		settlementTable.setAutoCreateRowSorter(true);        
        settlementTable.setDefaultRenderer(Object.class, new UnitTableCellRenderer(settlementTableModel));
        settlementTable.setRowSelectionAllowed(true);
        settlementTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        settlementTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) {
                        int index = settlementTable.getSelectedRow();
                        if (index > -1) {
                            if (settlementTableModel.isFailureRow(index)) {
                                errorMessageLabel.setText(Msg.getString("ConstructionSettlementPanel.error")); //$NON-NLS-1$
                                getWizard().setButtons(false);
                            }
                            else {
                                errorMessageLabel.setText(" ");
                                getWizard().setButtons(true);
                            }
                        }
                    }
                }
            }
        );
        
		// call it a click to next button when user double clicks the table
		settlementTable.addMouseListener(
			new MouseListener() {
				public void mouseReleased(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mouseEntered(MouseEvent e) {}
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2 && !e.isConsumed()) {
						wizard.buttonClickedNext();
					}
				}
			}
		);
		
        settlementTable.setPreferredScrollableViewportSize(settlementTable.getPreferredSize());
        settlementScrollPane.setViewportView(settlementTable);
        
        // Create the error message label.
        errorMessageLabel = createErrorLabel();
        add(errorMessageLabel);
        
        // Add a vertical glue.
        add(Box.createVerticalGlue());
    }
    
    @Override
    void clearInfo() {
        settlementTable.clearSelection();
        errorMessageLabel.setText(" ");
    }

	/**
	 * Commits changes from this wizard panel.
	 * 
	 * @param isTesting true if it's only testing conditions
	 * @return true if changes can be committed.
	 */
    @Override
    boolean commitChanges(boolean isTesting) {
        int selectedIndex = settlementTable.getSelectedRow();
        if (selectedIndex < 0)
        	return false;
        Settlement selectedSettlement = (Settlement) settlementTableModel.getUnit(selectedIndex);
        if (selectedSettlement == null)
        	return false;
        
//		if (!isTesting) {
			getWizard().getMissionData().setConstructionSettlement(selectedSettlement);
//			return true;
//		}	
	       return true;
    }

    @Override
    String getPanelName() {
        return NAME;
    }

    @Override
    void updatePanel() {
        settlementTableModel.updateTable();
        settlementTable.setPreferredScrollableViewportSize(settlementTable.getPreferredSize());
    }

    /**
     * A table model for settlements.
     */
    private static class SettlementTableModel extends UnitTableModel {
        
        /** default serial id. */
		private static final long serialVersionUID = 1L;

		/**
         * hidden constructor.
         */
        private SettlementTableModel() {
            // Use UnitTableModel constructor.
            super();
            
            // Add all settlements to table sorted by name.
            Collection<Settlement> settlements = CollectionUtils.sortByName(unitManager.getSettlements());
            Iterator<Settlement> i = settlements.iterator();
            while (i.hasNext()) units.add(i.next());
            
            // Add columns.
            columns.add("Name");
            columns.add("Population");
            columns.add("Construction Sites");
            columns.add("Light Utility Vehicles");
            columns.add("EVA Suits");
        }
        
        /**
         * Returns the value for the cell at columnIndex and rowIndex.
         * 
         * @param row the row whose value is to be queried
         * @param column the column whose value is to be queried
         * @return the value Object at the specified cell
         */
        public Object getValueAt(int row, int column) {
            Object result = null;
            
            if (row < units.size()) {
                try {
                    Settlement settlement = (Settlement) getUnit(row);
                    if (column == 0) 
                        result = settlement.getName();
                    else if (column == 1) 
                        result = settlement.getIndoorPeopleCount();
                    else if (column == 2) {
                        int numSites = settlement.getConstructionManager().getConstructionSites().size();
                        result = numSites;
                    }
                    else if (column == 3) 
                        result = settlement.findNumVehiclesOfType(VehicleType.LUV);
                    else if (column == 4) 
                        result = settlement.getNumEVASuit();
                }
                catch (Exception e) {}
            }
            
            return result;
        }
        
        /**
         * Updates the table data.
         */
        void updateTable() {
            fireTableStructureChanged();
        }
        
        /**
         * Checks if a table cell is a failure cell.
         * 
         * @param row the table row.
         * @param column the table column.
         * @return true if cell is a failure cell.
         */
        boolean isFailureCell(int row, int column) {
            boolean result = false;
            Settlement settlement = (Settlement) getUnit(row);
            
            try {
                if (column == 1) {
                    if (settlement.getIndoorPeopleCount() == 0) 
                    	result = true;
                }
            }
            catch (Exception e) {}
            
            return result;
        }
    }
}
