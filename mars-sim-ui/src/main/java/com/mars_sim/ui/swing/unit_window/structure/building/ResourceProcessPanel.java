/*
 * Mars Simulation Project
 * ResourceProcessPanel.java
 * @date 2024-06-09
 * @author Barry Evans
 */
package com.mars_sim.ui.swing.unit_window.structure.building;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.DefaultCellEditor;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.mars_sim.core.Entity;
import com.mars_sim.core.resource.ResourceUtil;
import com.mars_sim.core.resourceprocess.ResourceProcess;
import com.mars_sim.core.resourceprocess.ResourceProcess.ProcessState;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.StyleManager;
import com.mars_sim.ui.swing.utils.EntityLauncher;
import com.mars_sim.ui.swing.utils.EntityModel;
import com.mars_sim.ui.swing.utils.JProcessButton;
import com.mars_sim.ui.swing.utils.ToolTipTableModel;

/**
 * Creates a JPanel that will render a list of ResourceProcesses in a JTable.
 * This includes creating a dynamic tooltip.
 */
@SuppressWarnings("serial")
public class ResourceProcessPanel extends JPanel {
    private static final Icon INPUTS_DOT = ImageLoader.getIconByName("dot/yellow");

    private static final String KG_SOL = " kg/sol";
	private static final String BR = "<br>";
	private static final String INPUTS = "&emsp;&emsp;&nbsp;Inputs:&emsp;";
	private static final String OUTPUTS = "&emsp;&nbsp;&nbsp;Outputs:&emsp;";
	private static final String SPACES = "&nbsp;&emsp;&emsp;&emsp;&emsp;&emsp;&emsp;";
	private static final String PROCESS = "&emsp;&nbsp;Process:&emsp;";
	private static final String BUILDING_HEADER = "&emsp;&nbsp;Building:&emsp;";
	private static final String POWER_REQ = "Power Req:&emsp;";
	private static final String NOTE = "&emsp;<i>Note:  * denotes an ambient resource</i>";

    /**
     * Private table model to manage the Resource Processes. 
     * - Single building mode
     * - Multiple building mode
     */
	private static class ResourceProcessTableModel extends AbstractTableModel
                implements EntityModel, ToolTipTableModel {
		private static final int RUNNING_STATE = 0;
        private static final int BUILDING_NAME = 1;		
        private static final int PROCESS_NAME = 2;
        private static final int INPUT_SCORE = 3;
        private static final int OUTPUT_SCORE = 4;
        private static final int SCORE = 5;
        
        private Building mainBuilding;
        
        private List<ResourceProcess> processes = new ArrayList<>();

        private List<Building> buildings;

        /**
         * Constructor 1 : for one single building.
         * 
         * @param building
         * @param source
         */
		public ResourceProcessTableModel(Building building, List<ResourceProcess> source) {
			processes = new ArrayList<>(source);
            mainBuilding = building;
		}

		/**
		 * Constructor 2 : for the whole settlement.
		 * 
		 * @param buildingProcs
		 */
        public ResourceProcessTableModel(Map<Building, List<ResourceProcess>> buildingProcs) {
            // Unpack map into a single list
            buildings = new ArrayList<>();
            for (Entry<Building, List<ResourceProcess>> entry : buildingProcs.entrySet()) {
                for (ResourceProcess p : entry.getValue()) {
                    processes.add(p);
                    Building building = entry.getKey();
                    if (building != null)
                    	buildings.add(building);
                }
            }
        }
        
        @Override
		public int getRowCount() {
			return processes.size();
		}

        @Override
		public int getColumnCount() {
        	return 6;
		}

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return (columnIndex == RUNNING_STATE);
        }

        @Override
		public Class<?> getColumnClass(int columnIndex) {
            switch(columnIndex) {
                case RUNNING_STATE: return ResourceProcess.ProcessState.class;
                case BUILDING_NAME: return String.class;
                case PROCESS_NAME: return String.class;                
                case INPUT_SCORE: return Double.class;
                case OUTPUT_SCORE: return Double.class;
                case SCORE: return Double.class;
                default:
                    throw new IllegalArgumentException("Column unknown " + columnIndex);
            }
		}

        @Override
		public String getColumnName(int columnIndex) {
            switch(columnIndex) {
                case RUNNING_STATE: return "On";
                case BUILDING_NAME: return "Building";
                case PROCESS_NAME: return "Process";
                case INPUT_SCORE: return "In";
                case OUTPUT_SCORE: return "Out";
                case SCORE: return "Score";
                default:
                    throw new IllegalArgumentException("Column unknown " + columnIndex);
            }
		}

        @Override
        public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
            ResourceProcess p = processes.get(rowIndex);
            ProcessState s = (ProcessState) aValue;
            p.setProcessRunning(s == ProcessState.RUNNING);
        }

        @Override
		public Object getValueAt(int row, int column) {
            ResourceProcess p = processes.get(row);
            switch(column) {
                case RUNNING_STATE: return p.getState();
                case BUILDING_NAME: return getBuilding(row);                
                case PROCESS_NAME: return p.getProcessName();
                case INPUT_SCORE: return Math.round(p.getInputScore() * 100.0)/100.0;
                case OUTPUT_SCORE: return Math.round(p.getOutputScore() * 100.0)/100.0;
                case SCORE: return Math.round(p.getOverallScore() * 100.0)/100.0;
                default:
                    throw new IllegalArgumentException("Column unknown " + column);
            }
		}

        /**
         * Gets the associated process object.
         */
        ResourceProcess getProcess(int rowIndex) {
            return processes.get(rowIndex);
        }

        /**
         * Gets the building hosting a process.
         * 
         * @param rowIndex
         * @return
         */
        Building getBuilding(int rowIndex) {
            if (buildings == null) 
                return mainBuilding;
            
            return buildings.get(rowIndex);
        }

        @Override
        public Entity getAssociatedEntity(int row) {
            return getBuilding(row);
        }

        @Override
        public String getToolTipAt(int row, int col) {
            if (col == RUNNING_STATE) {
                return switch(getProcess(row).getState()) {
                    case RUNNING -> "Running";
                    case IDLE -> "Idle";
                    case INPUTS_UNAVAILABLE -> "No inputs";
                };
            }

            // Only display tooltip if hovering over the 3rd column named "Process"
            if (col == PROCESS_NAME) {
                return generateProcessTooltip(getProcess(row), getBuilding(row));
            }
            
            return null;
        }

        /**
         * Generates the tooltip.
         * 
         * @param process
         * @param building
         * @return
         */
        private String generateProcessTooltip(ResourceProcess process, Building building) {

            // NOTE: internationalize the resource processes' dynamic tooltip.
            StringBuilder result = new StringBuilder("<html>");
            // Future: Use another tool tip manager to align text to improve tooltip readability			
            result.append(PROCESS).append(process.getProcessName()).append(BR);
            result.append(BUILDING_HEADER).append(building.getName()).append(BR);
            result.append(POWER_REQ).append(StyleManager.DECIMAL_KW.format(process.getPowerRequired()))
            .append(BR);

            result.append(INPUTS);
            boolean firstItem = true;
            boolean hasAmbient = false;
            for (Integer resource: process.getInputResources()) {
                if (!firstItem) 
                    result.append(SPACES);
                double fullRate = process.getBaseFullInputRate(resource) * 1000D;
                String rateString = StyleManager.DECIMAL_PLACES2.format(fullRate);

                result.append(ResourceUtil.findAmountResource(resource).getName());
                if (process.isAmbientInputResource(resource)) {
                    result.append("*");
                    hasAmbient = true;
                }
                result.append(" @ ").append(rateString).append(KG_SOL).append(BR);
                firstItem = false;    
            }

            result.append(OUTPUTS);
            firstItem = true;
            for (Integer resource : process.getOutputResources()) {
                if (!firstItem)
                    result.append(SPACES);
                double fullRate = process.getBaseFullOutputRate(resource) * 1000D;
                String rateString = StyleManager.DECIMAL_PLACES2.format(fullRate);
                result.append(ResourceUtil.findAmountResource(resource).getName())
                    .append(" @ ").append(rateString).append(KG_SOL).append(BR);
                firstItem = false;    
            }
            // Add a note to denote an ambient input resource
            if (hasAmbient)
                result.append(NOTE);
            result.append("</html>");   
            
            return result.toString();
        }
	}

    /**
     * Renders a boolean value displaying Green/Red dots.
     */
    private static class RunningCellRenderer extends DefaultTableCellRenderer {

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            setHorizontalAlignment( SwingConstants.CENTER );

            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(null);
            if (value instanceof ResourceProcess.ProcessState state) {
                var icon = switch(state) {
                    case RUNNING -> JProcessButton.RUNNING_DOT;
                    case IDLE -> JProcessButton.STOPPED_DOT;
                    case INPUTS_UNAVAILABLE -> INPUTS_DOT;
                };
                setIcon(icon);
            }
            return this;
        }        
    }

    /**
     * Allows a cell to be edited.
     */
    private static class RunningCellEditor extends DefaultCellEditor {

        public RunningCellEditor() {
            super(new JCheckBox());
        }

        private JProcessButton button;
        private ProcessState selected;
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                boolean isSelected,
                                int row, int column) {
            selected = (ResourceProcess.ProcessState) value;

            button = new JProcessButton();
            button.setRunning(selected == ProcessState.RUNNING);
            button.addActionListener(e -> {
                selected = (selected == ProcessState.RUNNING) ? ProcessState.IDLE : ProcessState.RUNNING;
                button.setRunning(selected == ProcessState.RUNNING);

                // Stop after one click
                stopCellEditing();
            });

            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return selected;
        }

    }

    private ResourceProcessTableModel resourceProcessTableModel;

    /**
     * Creates a resource panel for a single Build.
     */
    public ResourceProcessPanel(Building building, List<ResourceProcess> source) {
        
        resourceProcessTableModel = new ResourceProcessTableModel(building, source);

        buildUI();
    }

    /**
     * Creates a resource panel that encompasses multiple Buildings each with dedicated Resource Processes.
     * 
     * @param processes Map.
     */
    public ResourceProcessPanel(Map<Building, List<ResourceProcess>> processes, MainDesktopPane desktop) {
       
    	resourceProcessTableModel = new ResourceProcessTableModel(processes);

        JTable table = buildUI();

        // In the multi-building mode add a mouse listener to open Details window
        EntityLauncher.attach(table, desktop);
    }

    private JTable buildUI() {
        // Create scroll panel for storage table
		JScrollPane scrollPanel = new JScrollPane();
	    scrollPanel.getViewport().setOpaque(false);
	    scrollPanel.setOpaque(false);

		JTable pTable = new JTable(resourceProcessTableModel) {
            // Implement table cell tool tips. 
            @Override          
            public String getToolTipText(MouseEvent e) {
                return ToolTipTableModel.extractToolTip(e, this);
            }
        };

		pTable.setCellSelectionEnabled(false);
		pTable.setAutoCreateRowSorter(true);
		scrollPanel.setViewportView(pTable);

        TableColumnModel columnModel = pTable.getColumnModel();
        columnModel.getColumn(0).setCellRenderer(new RunningCellRenderer());
        columnModel.getColumn(0).setCellEditor(new RunningCellEditor());
        columnModel.getColumn(0).setPreferredWidth(40);
        columnModel.getColumn(1).setPreferredWidth(120);
        columnModel.getColumn(2).setPreferredWidth(200);
        columnModel.getColumn(3).setPreferredWidth(50);
        columnModel.getColumn(4).setPreferredWidth(50);
        columnModel.getColumn(5).setPreferredWidth(50);
        
        setLayout(new BorderLayout());
        add(scrollPanel, BorderLayout.CENTER);

        return pTable;
    }

    /**
     * Updates the status of any resource processes.
     */
    public void update() {
    	int numRow = resourceProcessTableModel.getRowCount();
    	for (int i=0; i< numRow; i++) {	
    		resourceProcessTableModel.fireTableCellUpdated(i, 0);
    		resourceProcessTableModel.fireTableCellUpdated(i, 3);
    		resourceProcessTableModel.fireTableCellUpdated(i, 4);
    		resourceProcessTableModel.fireTableCellUpdated(i, 5);
    	}
    }
}
