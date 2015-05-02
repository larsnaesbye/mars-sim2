/**
 * Mars Simulation Project
 * TabPanelStructure.java
 * @version 3.08 2015-04-28
 * @author Manny Kung
 */
package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;


import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ScrollPaneConstants;
import javax.swing.UIManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;


import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.RoleType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

/**
 * The TabPanelStructure is a tab panel showing the organizational structure of a settlement.
 */
public class TabPanelStructure
extends TabPanel {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	private Settlement settlement;

	private JPanel infoPanel;

	/**
	 * Constructor.
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelStructure(Unit unit, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(
			"Org", //$NON-NLS-1$
			null,
			Msg.getString("TabPanelStructure.tooltip"), //$NON-NLS-1$
			unit, desktop
		);

	    settlement = (Settlement) unit;

		// Create label panel.
		JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(titlePanel);

		// Prepare label
		JLabel tlabel = new JLabel(Msg.getString("TabPanelStructure.title"), JLabel.CENTER); //$NON-NLS-1$
		tlabel.setFont(new Font("Serif", Font.BOLD, 16));
		tlabel.setForeground(new Color(102, 51, 0)); // dark brown
		titlePanel.add(tlabel);

		// Prepare info panel.
		infoPanel = new JPanel(new GridLayout(1, 2, 0, 0));
		infoPanel.setBorder(new MarsPanelBorder());
		centerContentPanel.add(infoPanel, BorderLayout.NORTH);

		// Create label panel.
		JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		infoPanel.add(labelPanel);

		// Prepare label
		JLabel label = new JLabel(Msg.getString("TabPanelStructure.label"), JLabel.CENTER); //$NON-NLS-1$
		labelPanel.add(label);

		createTree();
	}

	public void createTree() {

		DefaultMutableTreeNode root = new DefaultMutableTreeNode(settlement.getName());

		DefaultMutableTreeNode commanderStaffNode = new DefaultMutableTreeNode("Command Staff");
		DefaultMutableTreeNode commanderNode = new DefaultMutableTreeNode("Commander");
		DefaultMutableTreeNode subCommanderNode = new DefaultMutableTreeNode("Sub-Commander");

		DefaultMutableTreeNode cabinetNode = new DefaultMutableTreeNode("Cabinet");
		DefaultMutableTreeNode mayorNode = new DefaultMutableTreeNode("Mayor");

		DefaultMutableTreeNode agricultureNode = new DefaultMutableTreeNode("Agriculture");
		DefaultMutableTreeNode agricultureSpecialistNode = new DefaultMutableTreeNode("Agriculture Specialist");
		DefaultMutableTreeNode agricultureChiefNode = new DefaultMutableTreeNode("Chief of Agriculture");

		DefaultMutableTreeNode engineeringNode = new DefaultMutableTreeNode("Engineering");
		DefaultMutableTreeNode engineeringSpecialistNode = new DefaultMutableTreeNode("Engineering Specialist");
		DefaultMutableTreeNode engineeringChiefNode = new DefaultMutableTreeNode("Chief of Engineering");

		DefaultMutableTreeNode logisticNode = new DefaultMutableTreeNode("Logistic");
		DefaultMutableTreeNode logisticSpecialistNode = new DefaultMutableTreeNode("Logistic Specialist");
		DefaultMutableTreeNode logisticChiefNode = new DefaultMutableTreeNode("Chief of Logistic and Transportation");

		DefaultMutableTreeNode missionNode = new DefaultMutableTreeNode("Mission");
		DefaultMutableTreeNode missionSpecialistNode = new DefaultMutableTreeNode("Mission Specialist");
		DefaultMutableTreeNode missionChiefNode = new DefaultMutableTreeNode("Chief of Mission");

		DefaultMutableTreeNode safetyNode = new DefaultMutableTreeNode("Safety");
		DefaultMutableTreeNode safetySpecialistNode = new DefaultMutableTreeNode("Safety Specialist");
		DefaultMutableTreeNode safetyChiefNode = new DefaultMutableTreeNode("Chief of Safety");

		DefaultMutableTreeNode scienceNode = new DefaultMutableTreeNode("Science");
		DefaultMutableTreeNode scienceSpecialistNode = new DefaultMutableTreeNode("Science Specialist");
		DefaultMutableTreeNode scienceChiefNode = new DefaultMutableTreeNode("Chief of Science");

		DefaultMutableTreeNode supplyNode = new DefaultMutableTreeNode("Supply");
		DefaultMutableTreeNode supplySpecialistNode = new DefaultMutableTreeNode("Resource Specialist");
		DefaultMutableTreeNode supplyChiefNode = new DefaultMutableTreeNode("Chief of Supply and Resource");

		int population = settlement.getAllAssociatedPeople().size();

        if (population >= UnitManager.POPULATION_WITH_MAYOR) {
        	root.add(mayorNode);
        	root.add(cabinetNode);

        	cabinetNode.add(agricultureNode);
        	agricultureNode.add(agricultureChiefNode);
           	agricultureNode.add(agricultureSpecialistNode);

        	cabinetNode.add(engineeringNode);
        	engineeringNode.add(engineeringChiefNode);
        	engineeringNode.add(engineeringSpecialistNode);

           	cabinetNode.add(logisticNode);
        	logisticNode.add(logisticChiefNode);
           	logisticNode.add(logisticSpecialistNode);

           	cabinetNode.add(missionNode);
        	missionNode.add(missionChiefNode);
           	missionNode.add(missionSpecialistNode);

         	cabinetNode.add(safetyNode);
        	safetyNode.add(safetyChiefNode);
        	safetyNode.add(safetySpecialistNode);

           	cabinetNode.add(scienceNode);
        	scienceNode.add(scienceChiefNode);
           	scienceNode.add(scienceSpecialistNode);

          	cabinetNode.add(supplyNode);
        	supplyNode.add(supplyChiefNode);
        	supplyNode.add(supplySpecialistNode);


        	// TODO: More to add
        }
        else if (population >= UnitManager.POPULATION_WITH_SUB_COMMANDER) {
        	root.add(commanderStaffNode);
    		commanderStaffNode.add(commanderNode);
    		commanderStaffNode.add(subCommanderNode);

        	commanderStaffNode.add(engineeringNode);
        	engineeringNode.add(engineeringChiefNode);
        	engineeringNode.add(engineeringSpecialistNode);

           	commanderStaffNode.add(safetyNode);
        	safetyNode.add(safetyChiefNode);
        	safetyNode.add(safetySpecialistNode);

          	commanderStaffNode.add(supplyNode);
        	supplyNode.add(supplyChiefNode);
        	supplyNode.add(supplySpecialistNode);
        }
        else { // if population < 12
        	root.add(commanderNode);
        	root.add(engineeringSpecialistNode);
           	root.add(safetySpecialistNode);
          	root.add(supplySpecialistNode);
        }

		JTree tree = new JTree(root);
        tree.setVisibleRowCount(8);
		String currentTheme = UIManager.getLookAndFeel().getClass().getName();
		System.out.println("CurrentTheme is " + currentTheme);
/*
		if (desktop.getMainWindow() != null) {
			if (!desktop.getMainWindow().getLookAndFeelTheme().equals("nimrod")) {
				editIcons(tree);
			}
		} else {
			(desktop.getMainScene() != null) {
				if (!desktop.getMainScene().getLookAndFeelTheme().equals("nimrod")) {
					editIcons(tree);
				}
		}
*/
		if (!UIManager.getLookAndFeel().getClass().getName().equals("javax.swing.plaf.nimbus.NimbusLookAndFeel") )
			editIcons(tree);


	   	Collection<Person> people = settlement.getAllAssociatedPeople(); //.getInhabitants();

    	for (Person p : people) {
    		if (p.getRole().getType() == RoleType.COMMANDER) {
    	    	commanderNode.add(new DefaultMutableTreeNode(p));
    		}
    		else if (p.getRole().getType() == RoleType.SUB_COMMANDER) {
    	    	subCommanderNode.add(new DefaultMutableTreeNode(p));
    		}
    		else if (p.getRole().getType() == RoleType.MAYOR) {
    	    	mayorNode.add(new DefaultMutableTreeNode(p));
    		}
       		else if (p.getRole().getType() == RoleType.CHIEF_OF_AGRICULTURE) {
    	    	agricultureChiefNode.add(new DefaultMutableTreeNode(p));
    		}
    		else if (p.getRole().getType() == RoleType.AGRICULTURE_SPECIALIST) {
    			agricultureSpecialistNode.add(new DefaultMutableTreeNode(p));
    		}
    		else if (p.getRole().getType() == RoleType.CHIEF_OF_ENGINEERING) {
    	    	engineeringChiefNode.add(new DefaultMutableTreeNode(p));
    		}
    		else if (p.getRole().getType() == RoleType.ENGINEERING_SPECIALIST) {
    	    	engineeringSpecialistNode.add(new DefaultMutableTreeNode(p));
    		}
       		else if (p.getRole().getType() == RoleType.CHIEF_OF_LOGISTICS_N_OPERATIONS) {
    	    	logisticChiefNode.add(new DefaultMutableTreeNode(p));
    		}
    		else if (p.getRole().getType() == RoleType.LOGISTIC_SPECIALIST) {
    			logisticSpecialistNode.add(new DefaultMutableTreeNode(p));
    		}
       		else if (p.getRole().getType() == RoleType.CHIEF_OF_MISSION_PLANNING) {
    	    	missionChiefNode.add(new DefaultMutableTreeNode(p));
    		}
    		else if (p.getRole().getType() == RoleType.MISSION_SPECIALIST) {
    			missionSpecialistNode.add(new DefaultMutableTreeNode(p));
    		}
       		else if (p.getRole().getType() == RoleType.CHIEF_OF_SAFETY_N_HEALTH) {
    	    	safetyChiefNode.add(new DefaultMutableTreeNode(p));
    		}
    		else if (p.getRole().getType() == RoleType.SAFETY_SPECIALIST) {
    			safetySpecialistNode.add(new DefaultMutableTreeNode(p));
    		}
       		else if (p.getRole().getType() == RoleType.CHIEF_OF_SCIENCE) {
    	    	scienceChiefNode.add(new DefaultMutableTreeNode(p));
    		}
    		else if (p.getRole().getType() == RoleType.SCIENCE_SPECIALIST) {
    			scienceSpecialistNode.add(new DefaultMutableTreeNode(p));
    		}
       		else if (p.getRole().getType() == RoleType.CHIEF_OF_SUPPLY) {
    	    	supplyChiefNode.add(new DefaultMutableTreeNode(p));
    		}
    		else if (p.getRole().getType() == RoleType.RESOURCE_SPECIALIST) {
    			supplySpecialistNode.add(new DefaultMutableTreeNode(p));
    		}
    		else {
    			// anyone who does not belong will be placed in the root node
    			DefaultMutableTreeNode node = new DefaultMutableTreeNode(p);
    			root.add(node);
    		}
    	}

    	DefaultTreeModel defaultTreeModel = new DefaultTreeModel(root);
    	tree.setModel(defaultTreeModel);

    	for (int i = 0; i < tree.getRowCount(); i++)
    		tree.expandRow(i);

    	centerContentPanel.add(new JScrollPane(tree, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
    			ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);

    	MouseListener ml = new MouseAdapter() {
    	    public void mousePressed(MouseEvent e) {
    	        int selRow = tree.getRowForLocation(e.getX(), e.getY());
    	        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
    	        if(selRow != -1) {
    	            if(e.getClickCount() == 2) {
    	    			DefaultMutableTreeNode node = (DefaultMutableTreeNode)path.getLastPathComponent();
    	    			Person person =  (Person) node.getUserObject();
    	    			if (person != null) {
    	    				desktop.openUnitWindow(person, false);
    	            	}
    	            }
    	        }
    	    }
    	};

    	tree.addMouseListener(ml);

	}

	public void editIcons(JTree tree) {

        tree.setCellRenderer(new DefaultTreeCellRenderer() {
            private Icon personIcon = UIManager.getIcon("RadioButton.icon"); //OptionPane.errorIcon");
            private Icon roleIcon = UIManager.getIcon("FileChooser.detailsViewIcon");//OptionPane.informationIcon");
            private Icon homeIcon = UIManager.getIcon("FileChooser.homeFolderIcon");
            @Override
            public Component getTreeCellRendererComponent(JTree tree,
                    Object value, boolean selected, boolean expanded,
                    boolean isLeaf, int row, boolean focused) {
                Component c = super.getTreeCellRendererComponent(tree, value,
                        selected, expanded, isLeaf, row, focused);
                //if (selected)
                if (isLeaf)
                	// this node is a person
                    setIcon(personIcon);
                else if (row == 0)
                	// this is the root node
                	setIcon(homeIcon);
                else
                	// this node is just a role
                    setIcon(roleIcon);
                // TODO: how to detect a brand node that is empty ?
                return c;
            }
        });
	}

	public Person findPerson(String name) {
		//Person person = null;
		Collection<Person> people = settlement.getInhabitants();
		//List<Person> peopleList = new ArrayList<Person>(people);
		Person person = (Person) people.stream()
                .filter(p -> p.getName() == name);

		return person;
	}

	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {
		// TODO: if a person dies, have settlement re-elect another chief
		// and run createTree() again here

	}
}