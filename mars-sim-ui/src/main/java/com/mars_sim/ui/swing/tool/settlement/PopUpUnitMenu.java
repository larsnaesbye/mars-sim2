/*
 * Mars Simulation Project
 * PopUpUnitMenu.java
 * @date 2021-11-28
 * @author Manny Kung
 */

package com.mars_sim.ui.swing.tool.settlement;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.JDialog;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import com.mars_sim.core.Unit;
import com.mars_sim.core.UnitType;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.construction.ConstructionManager;
import com.mars_sim.core.structure.construction.ConstructionSite;
import com.mars_sim.core.tool.Conversion;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.core.vehicle.GroundVehicle;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.MainWindow;
import com.mars_sim.ui.swing.MarsPanelBorder;
import com.mars_sim.ui.swing.unit_window.UnitWindow;
import com.mars_sim.ui.swing.unit_window.structure.ConstructionSitesPanel;
import com.mars_sim.ui.swing.utils.SwingHelper;


public class PopUpUnitMenu extends JPopupMenu {

	private static final long serialVersionUID = 1L;
	
	// default logger.
	private static final SimLogger logger = SimLogger.getLogger(PopUpUnitMenu.class.getName());
	
	public static final int WIDTH_0 = 350;

	public static final int WIDTH_1 = WIDTH_0;
	public static final int HEIGHT_1 = 300;

	public static final int WIDTH_2 = UnitWindow.WIDTH - 130;
	public static final int HEIGHT_2 = UnitWindow.HEIGHT - 70;

	private static Map<Integer, JInternalFrame> panels = new ConcurrentHashMap<>();

    public PopUpUnitMenu(final SettlementWindow swindow, final Unit unit){
		add(unit.getUnitType().getName() + " : " + unit.getName());
		addSeparator();
    	MainDesktopPane desktop = swindow.getDesktop();
    	
    	switch (unit.getUnitType()) {
			case PERSON:
        		add(buildDetailsItem(unit, desktop));
				break;
        	
			case VEHICLE: 
				add(buildDescriptionitem(unit, desktop));
				add(buildDetailsItem(unit, desktop));
				add(buildVehicleRelocate(unit));
				add(buildVehicleToMaintain(unit));
				break;

        	case BUILDING:
				add(buildDescriptionitem(unit, desktop));
				add(buildDetailsItem(unit, desktop));
				break;

        	// Note: for construction sites
			case CONSTRUCTION:
				add(buildDescriptionitem(unit, desktop));
				add(buildDetailsItem(unit, desktop));
				add(relocateSite((ConstructionSite)unit));
				add(rotateSite((ConstructionSite)unit));
				add(confirmSite((ConstructionSite)unit));
				break;

			default:
				add(buildDetailsItem(unit, desktop));
				break;
        }
    }


    /**
     * Builds item one.
     *
     * @param unit
     */
    private JMenuItem buildDescriptionitem(final Unit unit, final MainDesktopPane desktop) {
        
		JMenuItem descriptionItem = new JMenuItem(Msg.getString("PopUpUnitMenu.description"));

        descriptionItem.addActionListener(e -> {

                String description = null;
                String type = null;
                String name = null;

                if (unit.getUnitType() == UnitType.VEHICLE) {
                	Vehicle vehicle = (Vehicle) unit;
                	description = vehicle.getDescription();
                	type = vehicle.getVehicleType().getName();
                	name = vehicle.getName();
                }
                else if (unit.getUnitType() == UnitType.BUILDING) {
                	Building building = (Building) unit;
                	description = building.getDescription();
                	type = building.getBuildingType();
                	name = building.getName();
                }
                else if (unit.getUnitType() == UnitType.CONSTRUCTION) {
                	ConstructionSite site = (ConstructionSite) unit;
                	description = site.getStageInfo().getName();
                	type = site.getStageInfo().getType();
                	name = site.getName();
                }
                else
                	return;

				UnitInfoPanel b = new UnitInfoPanel(desktop);

			    b.init(name, type, description);
	           	b.setOpaque(false);
		        b.setBackground(new Color(0,0,0,128));

				final JDialog d = SwingHelper.createPopupWindow(b, WIDTH_1, HEIGHT_1, 0, 0);

				d.setForeground(Color.WHITE); // orange font
				d.setFont(new Font("Arial", Font.BOLD, 14));

            	d.setOpacity(0.75f);
		        d.setBackground(new Color(0,0,0,128));
                d.setVisible(true);
             }
        );

		return descriptionItem;
    }

	
    /**
     * Builds item two.
     *
     * @param unit
     * @param mainDesktopPane
     */
    private JMenuItem buildDetailsItem(final Unit unit, final MainDesktopPane desktop) {
		JMenuItem detailsItem = new JMenuItem(Msg.getString("PopUpUnitMenu.details"));

        detailsItem.addActionListener(e -> {
	            if (unit.getUnitType() == UnitType.VEHICLE
	            		|| unit.getUnitType() == UnitType.PERSON
		            	|| unit.getUnitType() == UnitType.BUILDING	
	            		|| unit.getUnitType() == UnitType.ROBOT) {
	            	desktop.showDetails(unit);
	            }
	            
	            else if (unit.getUnitType() == UnitType.CONSTRUCTION) {
	            	buildConstructionWindow(unit, desktop);
	            }
	    });

		return detailsItem;
    }

    private void buildConstructionWindow(final Unit unit, final MainDesktopPane desktop) {
    	int newID = unit.getIdentifier();

    	if (!panels.isEmpty()) {
        	Iterator<Integer> i = panels.keySet().iterator();
			while (i.hasNext()) {
				int oldID = i.next();
				JInternalFrame f = panels.get(oldID);
        		if (newID == oldID && (f.isShowing() || f.isVisible())) {
        			f.dispose();
        			panels.remove(oldID);
        		}
        	}
    	}
    	
       	ConstructionSite site = (ConstructionSite) unit;

       	ConstructionManager manager = site.getAssociatedSettlement().getConstructionManager();
       	
		final ConstructionSitesPanel sitePanel = new ConstructionSitesPanel(manager);

        JInternalFrame d = new JInternalFrame(
        		unit.getSettlement().getName() + " - " + site,
        		true, 
                false, 
                true,
                false); 

        d.setIconifiable(false);
        d.setClosable(true);
		d.setFrameIcon(MainWindow.getLanderIcon());
		d.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

		JPanel panel = new JPanel(new BorderLayout(1, 1));
		panel.setBorder(new MarsPanelBorder());
		panel.setBorder(new EmptyBorder(1, 1, 1, 1));

		panel.add(sitePanel, BorderLayout.CENTER);

		String phase = site.getPhase().getName();
		JLabel label = new JLabel("Mission Phase : " + phase, SwingConstants.CENTER);
		
		panel.add(label, BorderLayout.SOUTH);
		
		d.add(panel);
		desktop.add(d);

		d.setMaximumSize(new Dimension(WIDTH_2, HEIGHT_2));
		d.setPreferredSize(new Dimension(WIDTH_2, HEIGHT_2));
		d.setSize(WIDTH_2, HEIGHT_2); // undecorated: 300, 335; decorated: 310, 370
		d.setLayout(new FlowLayout());

		// Create compound border
		Border border = new MarsPanelBorder();
		Border margin = new EmptyBorder(1,1,1,1);
		d.getRootPane().setBorder(new CompoundBorder(border, margin));

        // Save this panel into the map
        panels.put(site.getIdentifier(), d);

        d.setVisible(true);
    }
    
    /**
     * Builds item for vehicle relocation.
     *
     * @param unit
     */
	private JMenuItem buildVehicleRelocate(Unit unit) {
		JMenuItem relocateItem = new JMenuItem(Msg.getString("PopUpUnitMenu.relocate"));

        relocateItem.addActionListener(e -> {
	            ((Vehicle) unit).relocateVehicle();
	    		repaint();
        });

		return relocateItem;
	}
	
    /**
     * Builds item for maintenance tagging.
     *
     * @param unit
     */
	private JMenuItem buildVehicleToMaintain(Unit unit) {
		JMenuItem item = new JMenuItem(Msg.getString("PopUpUnitMenu.maintain"));

		item.addActionListener(e -> {
	            ((Vehicle) unit).maintainVehicle();
	    		repaint();
        });

		return item;
	}
	
    /**
     * Builds item for relocating a construction site.
     *
     * @param unit
     */
	private JMenuItem relocateSite(ConstructionSite site) {
		JMenuItem relocateItem = new JMenuItem(Msg.getString("PopUpUnitMenu.relocate"));

		List<GroundVehicle> vehicles = site.getVehicles();
		
        relocateItem.setForeground(new Color(139,69,19));
        relocateItem.addActionListener(e -> {
        		site.relocateSite();
        		
        		if (vehicles != null && !vehicles.isEmpty()) {
	        		for (Vehicle v: vehicles) {
	        			// Why is this needed ?
	        			v.setCoordinates(site.getCoordinates());
	        		}
        		}
	    		repaint();
        });

		return relocateItem;
	}
	
	/**
     * Builds item five.
     *
     * @param unit
     */
	private JMenuItem rotateSite(ConstructionSite site) {
		JMenuItem rotateItem = new JMenuItem(Msg.getString("PopUpUnitMenu.rotate"));

		rotateItem.setForeground(new Color(139,69,19));
		rotateItem.addActionListener(e -> {
			int siteAngle = (int) site.getFacing();
			siteAngle += 90;
			if (siteAngle >= 360)
				siteAngle = 0;
			site.setFacing(siteAngle);
			logger.info(site, "Just set facing to " + siteAngle + ".");
			repaint();
        });

		return rotateItem;
	}
	
	/**
     * Builds item six.
     *
     * @param unit
     */
	private JMenuItem confirmSite(ConstructionSite site) {
		boolean isConfirm = site.isSiteLocConfirmed();
		
		JMenuItem confirmItem = new JMenuItem(Msg.getString("PopUpUnitMenu.confirmSite") 
				+ " (" + Conversion.capitalize(isConfirm + "") + ")");

		confirmItem.setForeground(new Color(139, 69, 19));
		confirmItem.addActionListener(e -> {
	
			if (!isConfirm) {
				// If it's not being confirmed
				site.setSiteLocConfirmed(true);
				confirmItem.setText(Msg.getString("PopUpUnitMenu.confirmSite") 
						+ " (" + Conversion.capitalize(isConfirm + "") + ")");
				logger.info(site, "Just confirmed the site for construction. Ready to go to the next phase.");
				repaint();
			}
			else {
				logger.info(site, "The site has already been confirmed for construction.");
			}
			
        });

		return confirmItem;
	}
}
