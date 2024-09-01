/**
 * Mars Simulation Project
 * ConstructionVehiclePanel.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.tool.mission.create;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
import com.mars_sim.core.person.ai.mission.Mission;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.construction.ConstructionStageInfo;
import com.mars_sim.core.vehicle.GroundVehicle;
import com.mars_sim.core.vehicle.LightUtilityVehicle;
import com.mars_sim.core.vehicle.StatusType;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.ui.swing.MarsPanelBorder;

/**
 * A wizard panel for selecting the construction vehicles for a mission.
 */
@SuppressWarnings("serial")
class ConstructionVehiclePanel extends WizardPanel {

	/** The wizard panel name. */
    private final static String NAME = "Construction Vehicles";
    
    // Data members.
    private VehicleTableModel vehicleTableModel;
    private JTable vehicleTable;
    private JLabel requiredLabel;
    private JLabel selectedLabel;
    private JLabel errorMessageLabel;
    	
    /**
     * Constructor.
     * 
     * @param wizard the create mission wizard.
     */
    ConstructionVehiclePanel(final CreateMissionWizard wizard) {
        // User WizardPanel constructor.
        super(wizard);
        		
        // Set the layout.
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        // Set the border.
        setBorder(new MarsPanelBorder());
        
        // Create the select vehicle label.
        JLabel selectVehicleLabel = createTitleLabel("Select the light utility vehicles for the mission.");
        add(selectVehicleLabel);
        
        requiredLabel = new JLabel("Required vehicles: ");
        requiredLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(requiredLabel);
        
        selectedLabel = new JLabel("Number selected: ");
        selectedLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(selectedLabel);
        
        // Create the vehicle panel.
        JPanel vehiclePane = new JPanel(new BorderLayout(0, 0));
        vehiclePane.setMaximumSize(new Dimension(Short.MAX_VALUE, 100));
        vehiclePane.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(vehiclePane);
         
        // Create scroll panel for vehicle list.
        JScrollPane vehicleScrollPane = new JScrollPane();
        vehicleScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        vehiclePane.add(vehicleScrollPane, BorderLayout.CENTER);
        
        // Create the vehicle table model.
        vehicleTableModel = new VehicleTableModel();
        
        // Create the vehicle table.
        vehicleTable = new JTable(vehicleTableModel);
		vehicleTable.setAutoCreateRowSorter(true);		
        vehicleTable.setDefaultRenderer(Object.class, new UnitTableCellRenderer(vehicleTableModel));
        vehicleTable.setRowSelectionAllowed(true);
        vehicleTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        vehicleTable.getSelectionModel().addListSelectionListener(
            new ListSelectionListener() {
                public void valueChanged(ListSelectionEvent e) {
                    if (e.getValueIsAdjusting()) {
                        boolean goodVehicles = false;
                        errorMessageLabel.setText(" ");
                        
                        int requiredVehicles = getWizard().getMissionData().
                                getConstructionStageInfo().getVehicles().size();
                        
                        // Get the selected vehicle index.
                        int[] indexes = vehicleTable.getSelectedRows();
                        selectedLabel.setText("Number selected: " + indexes.length);
                        if (indexes.length >= requiredVehicles) {
                            goodVehicles = true;
                            for (int indexe : indexes) {
                                if (vehicleTableModel.isFailureRow(indexe)) {
                                    // Set the error message and disable the next button.
                                    errorMessageLabel.setText("Light Utility Vehicle cannot be " +
                                            "used on the mission (see red cells).");
                                    goodVehicles = false;
                                }
                            }
                        }
                        getWizard().setButtons(goodVehicles);
                    }
                }
            }
        );
		// call it a click to final button when user double clicks the table
		vehicleTable.addMouseListener(
			new MouseListener() {
				public void mouseReleased(MouseEvent e) {}
				public void mousePressed(MouseEvent e) {}
				public void mouseExited(MouseEvent e) {}
				public void mouseEntered(MouseEvent e) {}
				public void mouseClicked(MouseEvent e) {
					if (e.getClickCount() == 2 && !e.isConsumed()) {
						wizard.buttonClickedFinal();
					}
				}
			}
		);
        vehicleTable.setPreferredScrollableViewportSize(vehicleTable.getPreferredSize());
        vehicleScrollPane.setViewportView(vehicleTable);
        
        // Create the error message label.
        errorMessageLabel = createErrorLabel();
        add(errorMessageLabel);
        
        // Add a vertical glue.
        add(Box.createVerticalGlue());
    }
    
    @Override
    void clearInfo() {
        vehicleTable.clearSelection();
        errorMessageLabel.setText(" ");
        requiredLabel.setText("Required vehicles: ");
        selectedLabel.setText("Number selected: ");
    }

	/**
	 * Commits changes from this wizard panel.
	 * 
	 * @param isTesting true if it's only testing conditions
	 * @return true if changes can be committed.
	 */
    @Override
    boolean commitChanges(boolean isTesting) {
        List<GroundVehicle> constructionVehicles = new ArrayList<>();
        int[] selectedIndexs = vehicleTable.getSelectedRows();
        if (selectedIndexs.length == 0)
        	return false;
        
//        int requiredVehicles = getWizard().getMissionData().
//                getConstructionStageInfo().getVehicles().size();
        
        for (int x = 0; x < selectedIndexs.length; x++) {
            LightUtilityVehicle selectedVehicle = 
                (LightUtilityVehicle) vehicleTableModel.getUnit(selectedIndexs[x]);
            constructionVehicles.add(selectedVehicle);
        }
        
//        if (!isTesting) {
			getWizard().getMissionData().setConstructionVehicles(constructionVehicles);
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
        vehicleTableModel.updateTable();
        vehicleTable.setPreferredScrollableViewportSize(vehicleTable.getPreferredSize());
        ConstructionStageInfo cInfo = getWizard().getMissionData().getConstructionStageInfo();
        if (cInfo != null) {
	        int requiredVehicles = cInfo.getVehicles().size();
	        requiredLabel.setText("Required vehicles: " + requiredVehicles);  
	        if (requiredVehicles == 0) 
	        	getWizard().setButtons(true);
        }
    }
    
    /**
     * A table model for vehicles.
     */
    private class VehicleTableModel extends UnitTableModel {
        
        /**
         * Constructor
         */
        private VehicleTableModel() {
            // Use UnitTableModel constructor.
            super();
            
            // Add columns.
            columns.add("Name");
            columns.add("Status");
            columns.add("Mission");
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
                LightUtilityVehicle vehicle = (LightUtilityVehicle) getUnit(row);
                
                try {
                    if (column == 0) 
                        result = vehicle.getName();
                    else if (column == 1) 
                        result = vehicle.printStatusTypes();
                    else if (column == 2) {
                        Mission mission = vehicle.getMission();
                        if (mission != null) result = mission.getName();
                        else result = "None";
                    }
                }
                catch (Exception e) {}
            }
            
            return result;
        }
        
        /**
         * Updates the table data.
         */
        void updateTable() {
            units.clear();
            Settlement settlement = getWizard().getMissionData().getConstructionSettlement();
            Collection<Vehicle> vehicles = CollectionUtils.sortByName(
                    settlement.getParkedGaragedVehicles());
            Iterator<Vehicle> i = vehicles.iterator();
            while (i.hasNext()) {
                Vehicle vehicle = i.next();
                if (vehicle instanceof LightUtilityVehicle) units.add(vehicle);
            }
            fireTableDataChanged();
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
            LightUtilityVehicle vehicle = (LightUtilityVehicle) getUnit(row);
            
            if (column == 1) {
				if ((vehicle.getPrimaryStatus() != StatusType.PARKED) && (vehicle.getPrimaryStatus() != StatusType.GARAGED))
                	result = true;
            }
            else if (column == 2) {
                Mission mission = vehicle.getMission();
                if (mission != null) result = true;
            }
            
            return result;
        }
    }
}
