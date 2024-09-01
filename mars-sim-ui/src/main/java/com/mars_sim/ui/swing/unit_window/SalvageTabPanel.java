/*
 * Mars Simulation Project
 * SalvageTabPanel.java
 * @date 2022-07-09
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.unit_window;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;

import com.mars_sim.core.Unit;
import com.mars_sim.core.manufacture.Salvagable;
import com.mars_sim.core.manufacture.SalvageInfo;
import com.mars_sim.core.time.MarsTime;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.MarsPanelBorder;
import com.mars_sim.ui.swing.NumberCellRenderer;

/**
 * A tab panel with info about an item's salvage.
 */
@SuppressWarnings("serial")
public class SalvageTabPanel extends TabPanel {

	private static final String WARN_ICON = "warn";
	
    private String finishTimeString;
    private JLabel finishTimeLabel;
    private PartTableModel partTableModel;
    
    /**
     * Constructor
     * @param unit the unit to display.
     * @param desktop the main desktop.
     */
    public SalvageTabPanel(Unit unit, MainDesktopPane desktop) { 
        // Use the TabPanel constructor
        super(null, ImageLoader.getIconByName(WARN_ICON), "Salvage Info", unit, desktop);
	}

    @Override
    protected void buildUI(JPanel content) {
    	
		Unit unit = getUnit();
        Salvagable salvageItem = (Salvagable) unit;
        SalvageInfo salvageInfo = salvageItem.getSalvageInfo();

        // Create the salvage header panel.
        JPanel salvageHeaderPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 10));
        content.add(salvageHeaderPanel);
        
        // Create the salvage header label.
        JLabel salvageHeaderLabel = new JLabel(unit.getName() + " has been salvaged.", SwingConstants.CENTER);
        salvageHeaderPanel.add(salvageHeaderLabel);
        
        // Create the salvage info panel.
        JPanel salvageInfoPanel = new JPanel(new BorderLayout(0, 0));
        content.add(salvageInfoPanel);
        
        // Create the time panel.
        JPanel timePanel = new JPanel(new GridLayout(2, 1, 0, 0));
        salvageInfoPanel.add(timePanel, BorderLayout.NORTH);
        
        // Create the start time label.
        String startTimeString = salvageInfo.getStartTime().getDateTimeStamp();
        JLabel startTimeLabel = new JLabel("Start Time: " + startTimeString, SwingConstants.LEFT);
        timePanel.add(startTimeLabel);
        
        // Create the finish time label.
        MarsTime finishTime = salvageInfo.getFinishTime();
        finishTimeString = "";
        if (finishTime != null) finishTimeString = finishTime.getDateTimeStamp();
        finishTimeLabel = new JLabel("Finish Time: " + finishTimeString, SwingConstants.LEFT);
        timePanel.add(finishTimeLabel);

        // Create the settlement panel.
        JPanel settlementPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        salvageInfoPanel.add(settlementPanel);
        
        // Create the settlement label.
        JLabel settlementLabel = new JLabel("Settlement: ");
        settlementPanel.add(settlementLabel);
        
        // Create the settlement button.
        JButton settlementButton = new JButton(salvageInfo.getSettlement().getName());
        settlementButton.addActionListener(e -> {
                SalvageInfo info = ((Salvagable) getUnit()).getSalvageInfo();
                getDesktop().showDetails(info.getSettlement());
        });
        settlementPanel.add(settlementButton);
        
        // Create the parts panel.
        JPanel partsPanel = new JPanel(new BorderLayout(0, 10));
        partsPanel.setBorder(new MarsPanelBorder());
        content.add(partsPanel);
        
        // Create the parts label.
        JLabel partsLabel = new JLabel("Salvaged Parts", SwingConstants.CENTER);
        partsPanel.add(partsLabel, BorderLayout.NORTH);
        
        // Create the parts table panel.
        JScrollPane partsTablePanel = new JScrollPane();
        partsPanel.add(partsTablePanel, BorderLayout.CENTER);
        
        // Create the parts table.
        partTableModel = new PartTableModel(salvageInfo);
        JTable partsTable = new JTable(partTableModel);
        partsTable.setPreferredScrollableViewportSize(new Dimension(150, 75));
        partsTable.getColumnModel().getColumn(0).setPreferredWidth(100);
        partsTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        partsTable.setCellSelectionEnabled(false);
        partsTable.setDefaultRenderer(Double.class, new NumberCellRenderer(1));
        partsTablePanel.setViewportView(partsTable);
    }
    
    @Override
    public void update() {
        // Update finish time.
        SalvageInfo salvageInfo = ((Salvagable) getUnit()).getSalvageInfo();
        MarsTime finishTime = salvageInfo.getFinishTime();
        String newFinishTimeString = "";
        if (finishTime != null) newFinishTimeString = finishTime.getDateTimeStamp();
        if (!finishTimeString.equals(newFinishTimeString)) {
            finishTimeString = newFinishTimeString;
            finishTimeLabel.setText("Finish Time: " + finishTimeString);
        }
        
        // Update part table model.
        partTableModel.update();
    }
    
    /** 
     * Internal class used as model for the parts table.
     */
    private static class PartTableModel extends AbstractTableModel {
        
        // Data members
        private SalvageInfo salvageInfo;
        private Map<Integer, Integer> parts;
        private List<Integer> keys;
        
        /**
         * Constructor
         * @param salvageInfo the salvage info.
         */
        private PartTableModel(SalvageInfo salvageInfo) {
            this.salvageInfo = salvageInfo;
            parts = new HashMap<Integer, Integer>(salvageInfo.getPartsSalvaged());
            keys = new ArrayList<Integer>(parts.keySet());
            
            // Sort parts alphabetically by name.
            Collections.sort(keys);
        }
        
        @Override
        public int getRowCount() {
            return keys.size();
        }
        
        @Override
        public int getColumnCount() {
            return 2;
        }
        
        @Override
        public Class<?> getColumnClass(int columnIndex) {
            Class<?> dataType = super.getColumnClass(columnIndex);
            if (columnIndex == 1) dataType = Integer.class;
            return dataType;
        }
        
        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) return "Part";
            else if (columnIndex == 1) return "Number";
            else return "unknown";
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            if (column == 0) return keys.get(row);
            else if (column == 1) return parts.get(keys.get(row));
            else return "unknown";
        }
  
        /**
         * Updates the table model.
         */
        public void update() {
            if (!parts.equals(salvageInfo.getPartsSalvaged())) {
                parts.putAll(salvageInfo.getPartsSalvaged());
                keys.addAll(parts.keySet());
                Collections.sort(keys);
                fireTableDataChanged();
            }
        }
    }
}
