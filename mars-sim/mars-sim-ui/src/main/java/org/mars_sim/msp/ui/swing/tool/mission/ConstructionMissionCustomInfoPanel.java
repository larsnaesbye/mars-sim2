/**
 * Mars Simulation Project
 * ConstructionMissionCustomInfoPanel.java
 * @version 3.08 2015-03-19

 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.mission;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.BoundedRangeModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.person.ai.mission.BuildingConstructionMission;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.MissionEvent;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.Part;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.construction.ConstructionEvent;
import org.mars_sim.msp.core.structure.construction.ConstructionListener;
import org.mars_sim.msp.core.structure.construction.ConstructionSite;
import org.mars_sim.msp.core.structure.construction.ConstructionStage;
import org.mars_sim.msp.core.structure.construction.ConstructionStageInfo;
import org.mars_sim.msp.core.structure.construction.ConstructionVehicleType;
import org.mars_sim.msp.core.structure.goods.Good;
import org.mars_sim.msp.core.structure.goods.GoodsUtil;
import org.mars_sim.msp.ui.swing.MainDesktopPane;

/**
 * A panel for displaying construction custom mission information.
 */
public class ConstructionMissionCustomInfoPanel
extends MissionCustomInfoPanel 
implements ConstructionListener {

    /** default serial id. */
    private static final long serialVersionUID = 1L;

    // Data members.
    private MainDesktopPane desktop;
    private BuildingConstructionMission mission;
    private ConstructionSite site;
    private JLabel stageLabel;
    private BoundedRangeModel progressBarModel;
    private JButton settlementButton;
    private RemainingMaterialsTableModel remainingMaterialsTableModel;

    /**
     * Constructor.
     * @param desktop the main desktop panel.
     */
    public ConstructionMissionCustomInfoPanel(MainDesktopPane desktop) {
        // Use MissionCustomInfoPanel constructor.
        super();

        // Initialize data members.
        this.desktop = desktop;

        // Set layout.
        setLayout(new BorderLayout());

        JPanel contentsPanel = new JPanel(new GridLayout(4, 1));
        add(contentsPanel, BorderLayout.NORTH);

        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        contentsPanel.add(titlePanel);

        String titleLabelString = Msg.getString("ConstructionMissionCustomInfoPanel.titleLabel"); //$NON-NLS-1$
        JLabel titleLabel = new JLabel(titleLabelString);
        titlePanel.add(titleLabel);

        JPanel settlementPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contentsPanel.add(settlementPanel);

        String settlementLabelString = Msg.getString("ConstructionMissionCustomInfoPanel.settlementLabel"); //$NON-NLS-1$
        JLabel settlementLabel = new JLabel(settlementLabelString);
        settlementPanel.add(settlementLabel);

        settlementButton = new JButton("   ");
        settlementPanel.add(settlementButton);
        settlementButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (mission != null) {
                    Settlement settlement = mission.getAssociatedSettlement();
                    if (settlement != null) getDesktop().openUnitWindow(settlement, false);
                }
            }
        });

        JPanel stagePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contentsPanel.add(stagePanel);

        String stageLabelString = Msg.getString("ConstructionMissionCustomInfoPanel.stageLabel"); //$NON-NLS-1$
        stageLabel = new JLabel(stageLabelString);
        stagePanel.add(stageLabel);

        JPanel progressBarPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        contentsPanel.add(progressBarPanel);

        JProgressBar progressBar = new JProgressBar();
        progressBarModel = progressBar.getModel();
        progressBar.setStringPainted(true);
        progressBarPanel.add(progressBar);

        JPanel lowerContentsPanel = new JPanel(new BorderLayout(0, 0));
        add(lowerContentsPanel, BorderLayout.CENTER);
        
        // Create remaining construction materials label panel.
        JPanel remainingMaterialsLabelPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lowerContentsPanel.add(remainingMaterialsLabelPane, BorderLayout.NORTH);

        // Create remaining construction materials label.
        String remainingMaterialsLabelString = Msg.getString("ConstructionMissionCustomInfoPanel.remainingMaterialsLabel"); //$NON-NLS-1$
        JLabel remainingMaterialsLabel = new JLabel(remainingMaterialsLabelString);
        remainingMaterialsLabelPane.add(remainingMaterialsLabel);

        // Create a scroll pane for the remaining construction materials table.
        JScrollPane remainingMaterialsScrollPane = new JScrollPane();
        remainingMaterialsScrollPane.setPreferredSize(new Dimension(-1, -1));
        lowerContentsPanel.add(remainingMaterialsScrollPane, BorderLayout.CENTER);

        // Create the remaining construction materials table and model.
        remainingMaterialsTableModel = new RemainingMaterialsTableModel();
        JTable remainingMaterialsTable = new JTable(remainingMaterialsTableModel);
        remainingMaterialsScrollPane.setViewportView(remainingMaterialsTable);

        // Add tooltip.
        setToolTipText(getToolTipString());
    }

    @Override
    public void updateMission(Mission mission) {
        // Remove as construction listener if necessary.
        if (site != null) {
            site.removeConstructionListener(this);
        }

        if (mission instanceof BuildingConstructionMission) {
            this.mission = (BuildingConstructionMission) mission;
            site = this.mission.getConstructionSite();
            if (site != null) {
                site.addConstructionListener(this);
            }

            settlementButton.setText(mission.getAssociatedSettlement().getName());
            stageLabel.setText(getStageString());
            updateProgressBar();

            // Update remaining construction materials table.
            remainingMaterialsTableModel.updateTable();

            // Update the tool tip string.
            setToolTipText(getToolTipString());
        }
    }

    @Override
    public void updateMissionEvent(MissionEvent e) {
        stageLabel.setText(getStageString());

        // Update remaining construction materials table.
        remainingMaterialsTableModel.updateTable();
    }

    /**
     * Catch construction update event.
     * @param event the mission event.
     */
    public void constructionUpdate(ConstructionEvent event) {
        if (ConstructionStage.ADD_CONSTRUCTION_WORK_EVENT.equals(event.getType())) {
            updateProgressBar();

            // Update the tool tip string.
            setToolTipText(getToolTipString());
        }
        else if (ConstructionStage.ADD_CONSTRUCTION_MATERIALS_EVENT.equals(event.getType())) {

            // Update remaining construction materials table.
            remainingMaterialsTableModel.updateTable();
        }
    }

    /**
     * Gets the stage label string.
     * @return stage string.
     */
    private String getStageString() {
        StringBuilder stageString = new StringBuilder("Stage: ");
        if (mission != null) {
            ConstructionStage stage = mission.getConstructionStage();
            if (stage != null) {
                stageString.append(stage.getInfo().getName());
            }
        }

        return stageString.toString();
    }

    /**
     * Updates the progress bar.
     */
    private void updateProgressBar() {
        int workProgress = 0;
        if (mission != null) {
            ConstructionStage stage = mission.getConstructionStage();
            if (stage != null) {
                double completedWork = stage.getCompletedWorkTime();
                double requiredWork = stage.getRequiredWorkTime();
                if (requiredWork > 0D) {
                    workProgress = (int) (100D * completedWork / requiredWork);
                }
            }
        }
        progressBarModel.setValue(workProgress);
    }

    /**
     * Gets the main desktop.
     * @return desktop.
     */
    private MainDesktopPane getDesktop() {
        return desktop;
    }

    /**
     * Gets a tool tip string for the panel.
     */
    private String getToolTipString() {
        StringBuilder result = new StringBuilder(Msg.HTML_START);

        ConstructionStage stage = null;
        if (site != null) {
            stage = site.getCurrentConstructionStage();
        }

        if (stage != null) {
            ConstructionStageInfo info = stage.getInfo();
            result.append("Status: building ").append(info.getName()).append(Msg.BR);
            result.append("Stage Type: ").append(info.getType()).append(Msg.BR);
            result.append("Work Type: Construction").append(Msg.BR);
            DecimalFormat formatter = new DecimalFormat("0.0");
            String requiredWorkTime = formatter.format(stage.getRequiredWorkTime() / 1000D);
            result.append("Work Time Required: ").append(requiredWorkTime).append(" Sols").append(Msg.BR);
            String completedWorkTime = formatter.format(stage.getCompletedWorkTime() / 1000D);
            result.append("Work Time Completed: ").append(completedWorkTime).append(" Sols").append(Msg.BR);
            result.append("Architect Construction Skill Required: ").append(info.getArchitectConstructionSkill()).append(Msg.BR);

            // Add remaining construction resources.
            if (stage.getRemainingResources().size() > 0) {
                result.append(Msg.BR).append("Remaining Construction Resources:").append(Msg.BR);
                Iterator<AmountResource> i = stage.getRemainingResources().keySet().iterator();
                while (i.hasNext()) {
                    AmountResource resource = i.next();
                    double amount = stage.getRemainingResources().get(resource);
                    result.append(Msg.NBSP).append(Msg.NBSP).append(resource.getName()).append(": ").append(amount).append(" kg").append(Msg.BR);
                }
            }

            // Add remaining construction parts.
            if (stage.getRemainingParts().size() > 0) {
                result.append(Msg.BR).append("Remaining Construction Parts:").append(Msg.BR);
                Iterator<Part> j = stage.getRemainingParts().keySet().iterator();
                while (j.hasNext()) {
                    Part part = j.next();
                    int number = stage.getRemainingParts().get(part);
                    result.append(Msg.NBSP).append(Msg.NBSP).append(part.getName()).append(": ").append(number).append(Msg.BR);
                }
            }

            // Add construction vehicles.
            if (info.getVehicles().size() > 0) {
                result.append(Msg.BR).append("Construction Vehicles:").append(Msg.BR);
                Iterator<ConstructionVehicleType> k = info.getVehicles().iterator();
                while (k.hasNext()) {
                    ConstructionVehicleType vehicle = k.next();
                    result.append(Msg.NBSP).append(Msg.NBSP).append("Vehicle Type: ").append(vehicle.getVehicleType()).append(Msg.BR);
                    result.append(Msg.NBSP).append(Msg.NBSP).append("Attachment Parts:").append(Msg.BR);
                    Iterator<Part> l = vehicle.getAttachmentParts().iterator();
                    while (l.hasNext()) {
                        result.append(Msg.NBSP).append(Msg.NBSP).append(Msg.NBSP).append(Msg.NBSP).append(l.next().getName()).append(Msg.BR);
                    }
                }
            }
        }

        result.append(Msg.HTML_STOP);

        return result.toString();
    }

    /**
     * Model for the remaining construction materials table.
     */
    private class RemainingMaterialsTableModel extends AbstractTableModel {

        /** default serial id. */
        private static final long serialVersionUID = 1L;

        // Data members.
        protected Map<Good, Integer> goodsMap;
        protected List<Good> goodsList;

        /**
         * Constructor.
         */
        private RemainingMaterialsTableModel() {
            // Use AbstractTableModel constructor.
            super();

            // Initialize goods map and list.
            goodsList = new ArrayList<Good>();
            goodsMap = new HashMap<Good, Integer>();
        }

        /**
         * Returns the number of rows in the model.
         * @return number of rows.
         */
        public int getRowCount() {
            return goodsList.size();
        }

        /**
         * Returns the number of columns in the model.
         * @return number of columns.
         */
        public int getColumnCount() {
            return 2;
        }

        /**
         * Returns the name of the column at columnIndex.
         * @param columnIndex the column index.
         * @return column name.
         */
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return Msg.getString("ConstructionMissionCustomInfoPanel.column.material"); //$NON-NLS-1$
            }
            else {
                return Msg.getString("ConstructionMissionCustomInfoPanel.column.amount"); //$NON-NLS-1$
            }
        }

        /**
         * Returns the value for the cell at columnIndex and rowIndex.
         * @param row the row whose value is to be queried.
         * @param column the column whose value is to be queried.
         * @return the value Object at the specified cell.
         */
        public Object getValueAt(int row, int column) {
            Object result = Msg.getString("unknown"); //$NON-NLS-1$

            if (row < goodsList.size()) {
                Good good = goodsList.get(row); 
                if (column == 0) {
                    result = good.getName();
                }
                else {
                    result = goodsMap.get(good);
                }
            }

            return result;
        }

        /**
         * Updates the table data.
         */
        protected void updateTable() {

            goodsMap = new HashMap<Good, Integer>();

            // Populate goodsMap.
            ConstructionStage stage = mission.getConstructionStage();
            if (stage != null) {

                // Add remaining resources.
                Iterator<AmountResource> i = stage.getRemainingResources().keySet().iterator();
                while (i.hasNext()) {
                    AmountResource resource = i.next();
                    double amount = stage.getRemainingResources().get(resource);
                    Good good = GoodsUtil.getResourceGood(resource);
                    goodsMap.put(good, (int) amount);
                }

                // Add remaining parts.
                Iterator<Part> j = stage.getRemainingParts().keySet().iterator();
                while (j.hasNext()) {
                    Part part = j.next();
                    int num = stage.getRemainingParts().get(part);
                    Good good = GoodsUtil.getResourceGood(part);
                    goodsMap.put(good, num);
                }
            }

            goodsList = new ArrayList<Good>(goodsMap.keySet());
            Collections.sort(goodsList);

            fireTableDataChanged();
        }
    }
}