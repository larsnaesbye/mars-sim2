/**
 * Mars Simulation Project
 * ManufactureTabPanel.java
 * @version 2.85 2008-07-12
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.manufacture.ManufactureProcess;
import org.mars_sim.msp.core.manufacture.ManufactureProcessInfo;
import org.mars_sim.msp.core.manufacture.ManufactureUtil;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingException;
import org.mars_sim.msp.core.structure.building.function.Manufacture;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

public class ManufactureTabPanel extends TabPanel {

	private static String CLASS_NAME = 
	    "org.mars_sim.msp.ui.standard.unit_window.structure.ManufactureTabPanel";
	private static Logger logger = Logger.getLogger(CLASS_NAME);
	
	// Data members
	private Settlement settlement;
	private JPanel manufactureListPane;
	private JScrollPane manufactureScrollPane;
	private List<ManufactureProcess> processCache;
	private JComboBox buildingSelection; // Building selector.
	private Vector<Building> buildingSelectionCache; // List of available manufacture buildings.
	private JComboBox processSelection; // Process selector.
	private Vector<ManufactureProcessInfo> processSelectionCache; // List of available processes.
	private JButton newProcessButton; // Process selection button.
	private JCheckBox overrideCheckbox;
	
    /**
     * Constructor
     * @param unit the unit to display.
     * @param desktop the main desktop.
     */
	public ManufactureTabPanel(Unit unit, MainDesktopPane desktop) {
        // Use the TabPanel constructor
        super("Manu", null, "Manufacturing", unit, desktop);
        
        settlement = (Settlement) unit;
        setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        // Create topPanel.
        JPanel topPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
        topContentPanel.add(topPane);
        
        // Create manufacture label.
        JLabel manufactureLabel = new JLabel("Manufacturing", JLabel.CENTER);
        topPane.add(manufactureLabel);
        
		// Create scroll panel for manufacture list pane.
        manufactureScrollPane = new JScrollPane();
        manufactureScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        manufactureScrollPane.setPreferredSize(new Dimension(220, 215));
        topContentPanel.add(manufactureScrollPane);  
		
        // Prepare manufacture outer list pane.
        JPanel manufactureOuterListPane = new JPanel(new BorderLayout(0, 0));
        manufactureOuterListPane.setBorder(new MarsPanelBorder());
        manufactureScrollPane.setViewportView(manufactureOuterListPane);
        
        // Prepare malfunctions list pane.
        manufactureListPane = new JPanel();
        manufactureListPane.setLayout(new BoxLayout(manufactureListPane, BoxLayout.Y_AXIS));
        manufactureOuterListPane.add(manufactureListPane, BorderLayout.NORTH);
        
        // Create the process panels.
        processCache = getManufactureProcesses();
        Iterator<ManufactureProcess> i = processCache.iterator();
        while (i.hasNext()) manufactureListPane.add(new ManufacturePanel(i.next(), true, 30));
        
        // Create interaction panel.
        JPanel interactionPanel = new JPanel(new GridLayout(4, 1, 0, 0));
        topContentPanel.add(interactionPanel);
        
        // Create new building selection.
        buildingSelectionCache = getManufacturingBuildings();
        buildingSelection = new JComboBox(buildingSelectionCache);
        buildingSelection.setToolTipText("Select a manufacturing building");
        buildingSelection.addItemListener(new ItemListener() {
        	public void itemStateChanged(ItemEvent event) {
            	update();
            }
        });
        interactionPanel.add(buildingSelection);
        
        // Create new manufacture process selection.
        Building workshopBuilding = (Building) buildingSelection.getSelectedItem();
        processSelectionCache = getAvailableProcesses(workshopBuilding);
        processSelection = new JComboBox(processSelectionCache);
        processSelection.setRenderer(new ManufactureSelectionListCellRenderer());
        processSelection.setToolTipText("Select an available manufacturing process");
        interactionPanel.add(processSelection);
        
        // Create new process button.
        newProcessButton = new JButton("Create New Process");
        newProcessButton.setEnabled(processSelection.getItemCount() > 0);
        newProcessButton.setToolTipText("Create a new manufacturing process");
        newProcessButton.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent event) {
        		try {
        			Building workshopBuilding = (Building) buildingSelection.getSelectedItem();
        			if (workshopBuilding != null) {
        				Manufacture workshop = (Manufacture) workshopBuilding.getFunction(Manufacture.NAME);
        				ManufactureProcessInfo selectedProcess = (ManufactureProcessInfo) 
        						processSelection.getSelectedItem();
        				if (selectedProcess != null) {
        					if (ManufactureUtil.canProcessBeStarted(selectedProcess, workshop)) {
        						workshop.addProcess(new ManufactureProcess(selectedProcess, workshop));
        						update();
        					}
        				}
        			}
        		}
        		catch (Exception e) {
        			logger.log(Level.SEVERE, "new process button", e);
        		}
        	}
        });
        interactionPanel.add(newProcessButton);
        
        // Create override check box.
        overrideCheckbox = new JCheckBox("Override manufacturing");
        overrideCheckbox.setToolTipText("Prevents settlement inhabitants from " +
        		"starting new manufacturing processes.");
        overrideCheckbox.addActionListener(new ActionListener() {
        	public void actionPerformed(ActionEvent arg0) {
        		setManufactureOverride(overrideCheckbox.isSelected());
        	}
        });
        overrideCheckbox.setSelected(settlement.getManufactureOverride());
        interactionPanel.add(overrideCheckbox);
	}
	
	@Override
	public void update() {
		
		// Update processes if necessary.
		List<ManufactureProcess> processes = getManufactureProcesses();
		if (!processCache.equals(processes)) {
			
			// Add manufacture panels for new processes.
			Iterator<ManufactureProcess> i = processes.iterator();
			while (i.hasNext()) {
				ManufactureProcess process = i.next();
				if (!processCache.contains(process)) 
					manufactureListPane.add(new ManufacturePanel(process, true, 30));
			}
			
			// Remove manufacture panels for old processes.
			Iterator<ManufactureProcess> j = processCache.iterator();
			while (j.hasNext()) {
				ManufactureProcess process = j.next();
				if (!processes.contains(process)) {
					ManufacturePanel panel = getManufacturePanel(process);
					if (panel != null) manufactureListPane.remove(panel);
				}
			}
			
			manufactureScrollPane.validate();
			
			// Update processCache
			processCache.clear();
			processCache.addAll(processes);
		}
		
		// Update all process panels.
		Iterator<ManufactureProcess> i = processes.iterator();
		while (i.hasNext()) {
			ManufacturePanel panel = getManufacturePanel(i.next());
			if (panel != null) panel.update();
		}
		
		// Update building selection list.
		Vector<Building> newBuildings = getManufacturingBuildings();
		if (!newBuildings.equals(buildingSelectionCache)) {
			buildingSelectionCache = newBuildings;
			Building currentSelection = (Building) buildingSelection.getSelectedItem();
			buildingSelection.removeAllItems();
			Iterator<Building> j = buildingSelectionCache.iterator();
			while (j.hasNext()) buildingSelection.addItem(j.next());
			
			if (currentSelection != null) {
				if (buildingSelectionCache.contains(currentSelection)) 
					buildingSelection.setSelectedItem(currentSelection);
			}
		}
		
		// Update process selection list.
		Building selectedBuilding = (Building) buildingSelection.getSelectedItem();
		Vector<ManufactureProcessInfo> newProcesses = getAvailableProcesses(selectedBuilding);
		if (!newProcesses.equals(processSelectionCache)) {
			processSelectionCache = newProcesses;
			ManufactureProcessInfo currentSelection = (ManufactureProcessInfo) 
					processSelection.getSelectedItem();
			processSelection.removeAllItems();
			Iterator<ManufactureProcessInfo> j = processSelectionCache.iterator();
			while (j.hasNext()) processSelection.addItem(j.next());
			
			if (currentSelection != null) {
				if (processSelectionCache.contains(currentSelection)) 
					processSelection.setSelectedItem(currentSelection);
			}
		}
		
		// Update new process button.
		newProcessButton.setEnabled(processSelection.getItemCount() > 0);
		
		// Update ooverride check box.
		if (settlement.getManufactureOverride() != overrideCheckbox.isSelected()) 
			overrideCheckbox.setSelected(settlement.getManufactureOverride());
	}
	
	/**
	 * Gets all the manufacture processes at the settlement.
	 * @return list of manufacture processes.
	 */
	private List<ManufactureProcess> getManufactureProcesses() {
		List<ManufactureProcess> result = new ArrayList<ManufactureProcess>();
		
		Iterator<Building> i = settlement.getBuildingManager().getBuildings(Manufacture.NAME).iterator();
		while (i.hasNext()) {
			try {
				Manufacture workshop = (Manufacture) i.next().getFunction(Manufacture.NAME);
				result.addAll(workshop.getProcesses());
			}
			catch (BuildingException e) {}
		}
		
		return result;
	}
	
	/**
	 * Gets the panel for a manufacture process.
	 * @param process the manufacture process.
	 * @return manufacture panel or null if none.
	 */
	private ManufacturePanel getManufacturePanel(ManufactureProcess process) {
		ManufacturePanel result = null;
		
		for (int x = 0; x < manufactureListPane.getComponentCount(); x++) {
			Component component = manufactureListPane.getComponent(x);
			if (component instanceof ManufacturePanel) {
				ManufacturePanel panel = (ManufacturePanel) component;
				if (panel.getManufactureProcess().equals(process)) result = panel;
			}
		}
		
		return result;
	}
	
	/**
	 * Gets all manufacturing buildings at a settlement.
	 * @return vector of buildings.
	 */
	private Vector<Building> getManufacturingBuildings() {
		return new Vector<Building>(settlement.getBuildingManager().getBuildings(Manufacture.NAME));
	}
	
	/**
	 * Gets all manufacturing processes available at the workshop.
	 * @param manufactureBuilding the manufacturing building.
	 * @return vector of processes.
	 */
	private Vector<ManufactureProcessInfo> getAvailableProcesses(Building manufactureBuilding) {
		Vector<ManufactureProcessInfo> result = new Vector<ManufactureProcessInfo>();
		
		try {
			if (manufactureBuilding != null) {
				Manufacture workshop = (Manufacture) manufactureBuilding.getFunction(Manufacture.NAME);
				if (workshop.getProcesses().size() < workshop.getConcurrentProcesses()) {
					Iterator<ManufactureProcessInfo> i = 
						ManufactureUtil.getManufactureProcessesForTechLevel(
								workshop.getTechLevel()).iterator();
					while (i.hasNext()) {
						ManufactureProcessInfo process = i.next();
						if (ManufactureUtil.canProcessBeStarted(process, workshop)) 
							result.add(process);
					}
				}
			}
		}
		catch (Exception e) {
			// Note: Exceptions here are due to concurrency errors between
			// this UI thread querying an Inventory object and the simulation
			// thread changing it at the same time.
			// logger.log(Level.SEVERE, "get available processes", e);
		}
		
		return result;
	}
	
	/**
	 * Sets the settlement manufacture override flag.
	 * @param override the manufacture override flag.
	 */
	private void setManufactureOverride(boolean override) {
		settlement.setManufactureOverride(override);
	}
	
	/**
	 * Inner class for the manufacture selection list cell renderer.
	 */
	private class ManufactureSelectionListCellRenderer extends DefaultListCellRenderer {
		
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, 
				boolean isSelected, boolean cellHasFocus) {
			Component result = super.getListCellRendererComponent(list, value, index, isSelected, 
					cellHasFocus);
			ManufactureProcessInfo info = (ManufactureProcessInfo) value;
			if (info != null) {
				String processName = info.getName();
				if (processName.length() > 35) processName = processName.substring(0, 35) + "...";
				((JLabel) result).setText(processName);
				((JComponent) result).setToolTipText(ManufacturePanel.getToolTipString(info, null));
			}
			return result;
		}
	}
}