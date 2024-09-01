/*
 * Mars Simulation Project
 * TabPanelOrganization.java
 * @date 2023-11-15
 * @author Manny Kung
 */

package com.mars_sim.ui.swing.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import com.mars_sim.core.Unit;
import com.mars_sim.core.UnitEvent;
import com.mars_sim.core.UnitEventType;
import com.mars_sim.core.UnitListener;
import com.mars_sim.core.UnitManager;
import com.mars_sim.core.UnitManagerEvent;
import com.mars_sim.core.UnitManagerEventType;
import com.mars_sim.core.UnitManagerListener;
import com.mars_sim.core.UnitType;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.role.RoleType;
import com.mars_sim.core.structure.ChainOfCommand;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.StyleManager;
import com.mars_sim.ui.swing.unit_window.TabPanel;

/**
 * The TabPanelOrganization is a tab panel showing the organizational structure of
 * a settlement.
 * 
 * @See https://docs.oracle.com/javase/tutorial/uiswing/components/tree.html#display
 */
@SuppressWarnings("serial")
public class TabPanelOrganization extends TabPanel {

	/** Default logger. */
	private static SimLogger logger = SimLogger.getLogger(TabPanelOrganization.class.getName());

	private static final String ORG_ICON = "organisation";
	
	/** The Settlement instance. */
	private Settlement settlement;

	private JTree tree;
	
	private DefaultMutableTreeNode root;

	private DefaultTreeModel defaultTreeModel;

//	private TreeTableModel treeTableModel; 
//	private JTreeTable treeTable;

	private DefaultMutableTreeNode crewNode;
	private DefaultMutableTreeNode crewEngineerNode;
	private DefaultMutableTreeNode crewScientistNode;
	private DefaultMutableTreeNode crewOperationOfficerNode;
	private DefaultMutableTreeNode crewHealthNSafetyOfficerNode;
	
	private DefaultMutableTreeNode commanderStaffNode;
	private DefaultMutableTreeNode commanderNode;
	private DefaultMutableTreeNode subCommanderNode;
	private DefaultMutableTreeNode mayorNode;
	private DefaultMutableTreeNode presidentNode;
	
	private DefaultMutableTreeNode divisionNode;
	
	private DefaultMutableTreeNode agricultureNode;
	private DefaultMutableTreeNode agricultureSpecialistNode;
	private DefaultMutableTreeNode agricultureChiefNode;

	private DefaultMutableTreeNode computingNode;
	private DefaultMutableTreeNode computingSpecialistNode;
	private DefaultMutableTreeNode computingChiefNode;

	private DefaultMutableTreeNode engineeringNode;
	private DefaultMutableTreeNode engineeringSpecialistNode;
	private DefaultMutableTreeNode engineeringChiefNode;

	private DefaultMutableTreeNode logisticNode;
	private DefaultMutableTreeNode logisticSpecialistNode;
	private DefaultMutableTreeNode logisticChiefNode;

	private DefaultMutableTreeNode missionNode;
	private DefaultMutableTreeNode missionSpecialistNode;
	private DefaultMutableTreeNode missionChiefNode;

	private DefaultMutableTreeNode safetyNode;
	private DefaultMutableTreeNode safetySpecialistNode;
	private DefaultMutableTreeNode safetyChiefNode;

	private DefaultMutableTreeNode scienceNode;
	private DefaultMutableTreeNode scienceSpecialistNode;
	private DefaultMutableTreeNode scienceChiefNode;
	
	private DefaultMutableTreeNode supplyNode;
	private DefaultMutableTreeNode supplySpecialistNode;
	private DefaultMutableTreeNode supplyChiefNode;

	private Map<Person, RoleType> roles = new HashMap<>();

	private List<DefaultMutableTreeNode> nodes = new ArrayList<>();

	private Map<Person, PersonListener> listeners  = new HashMap<>();

	private LocalUnitManagerListener unitManagerListener;

	/**
	 * Constructor.
	 *
	 * @param unit    the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelOrganization(Settlement unit, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(
			null, 
			ImageLoader.getIconByName(ORG_ICON),
			Msg.getString("TabPanelStructure.title"), //$NON-NLS-1$
			desktop);

		settlement = unit;

	}

	@Override
	protected void buildUI(JPanel content) {
		UnitManager unitManager = getSimulation().getUnitManager();
		unitManagerListener = new LocalUnitManagerListener();
		unitManager.addUnitManagerListener(UnitType.PERSON, unitManagerListener);

		// Create label panel.
		JPanel labelPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		content.add(labelPanel, BorderLayout.NORTH);

		// Prepare label
		JLabel label = new JLabel(Msg.getString("TabPanelStructure.label"), SwingConstants.CENTER); //$NON-NLS-1$
		StyleManager.applySubHeading(label);
		labelPanel.add(label);

		root = new DefaultMutableTreeNode("  " + settlement.getName() + "  -  " + settlement.getUnitType().getName() + "  ");

		// Will figure out how to change font in ((DefaultMutableTreeNode) root.getParent()).getUserObject().setFont(labelFont);
		
		defaultTreeModel = new DefaultTreeModel(root);
		// Note : will allow changing role name in future : defaultTreeModel.addTreeModelListener(new MyTreeModelListener());
		
		tree = new JTree(defaultTreeModel);
		// Note : will allow changing role name in future : tree.setEditable(true);
		
		tree.getSelectionModel().setSelectionMode
		        (TreeSelectionModel.SINGLE_TREE_SELECTION);
		tree.setShowsRootHandles(true);
		tree.setVisibleRowCount(8);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setViewportView(tree);
		content.add(scrollPane, BorderLayout.CENTER);
		
		buildTreeNodes();

		initNodes();
		
//		treeTableModel = OrganizationTreeTableModel.createModel(settlement); 
//		
//		treeTable = new JTreeTable(treeTableModel);
//		
//		JScrollPane scrollTreePane = new JScrollPane();
//		scrollTreePane.setViewportView(treeTable);
//		content.add(scrollTreePane, BorderLayout.SOUTH);
	}

	/**
	 * Tracks tree changes.
	 *
	 * @param e TreeSelectionEvent
	 */
	public void valueChanged(TreeSelectionEvent e) {
		
		emptyNodes();
		
		buildTreeNodes();
		
		initNodes();
	}

	protected void initNodes() {

		constructNodes();

		considerRoles();

		setupMouseOnNodes();

		for (int i = 0; i < tree.getRowCount(); i++)
			tree.expandRow(i);
	}

	public void buildTreeNodes() {
		int population = settlement.getNumCitizens();
		
		if (population <= ChainOfCommand.POPULATION_WITH_COMMANDER) {

			commanderNode = new DefaultMutableTreeNode(RoleType.COMMANDER.toString());
			crewNode = new DefaultMutableTreeNode("Crew");
			
			crewEngineerNode = new DefaultMutableTreeNode(
					RoleType.CREW_ENGINEER.toString());
			crewScientistNode = new DefaultMutableTreeNode(
					RoleType.CREW_SCIENTIST.toString());
			crewHealthNSafetyOfficerNode = new DefaultMutableTreeNode(
					RoleType.CREW_HEALTH_N_SAFETY_OFFICER.toString());
			crewOperationOfficerNode = new DefaultMutableTreeNode(
					RoleType.CREW_OPERATION_OFFICER.toString());
			
			nodes.add(commanderNode);	
			nodes.add(crewNode);	
				
			crewNode.add(crewEngineerNode);
			crewNode.add(crewScientistNode);
			crewNode.add(crewOperationOfficerNode);
			crewNode.add(crewHealthNSafetyOfficerNode);
		}
		
		else {
			
			commanderStaffNode = new DefaultMutableTreeNode("(A). Command Staff");
			commanderNode = new DefaultMutableTreeNode(RoleType.COMMANDER.toString());
			subCommanderNode = new DefaultMutableTreeNode(RoleType.SUB_COMMANDER.toString());
	
			nodes.add(commanderStaffNode);
			nodes.add(commanderNode);
			nodes.add(subCommanderNode);
	
			divisionNode = new DefaultMutableTreeNode("(B). Division");
			mayorNode = new DefaultMutableTreeNode(RoleType.MAYOR.toString());
			presidentNode = new DefaultMutableTreeNode(RoleType.PRESIDENT.toString());
			
			nodes.add(divisionNode);
			nodes.add(mayorNode);
			nodes.add(presidentNode);
			
			agricultureNode = new DefaultMutableTreeNode("(1). Agriculture");
			agricultureSpecialistNode = new DefaultMutableTreeNode(
					RoleType.AGRICULTURE_SPECIALIST.toString());
			agricultureChiefNode = new DefaultMutableTreeNode(
					RoleType.CHIEF_OF_AGRICULTURE.toString());
	
			nodes.add(agricultureNode);
			nodes.add(agricultureSpecialistNode);
			nodes.add(agricultureChiefNode);
	
			computingNode = new DefaultMutableTreeNode("(2). Computing");
			computingSpecialistNode = new DefaultMutableTreeNode(
					RoleType.COMPUTING_SPECIALIST.toString());
			computingChiefNode = new DefaultMutableTreeNode(
					RoleType.CHIEF_OF_COMPUTING.toString());
	
			nodes.add(computingNode);
			nodes.add(computingSpecialistNode);
			nodes.add(computingChiefNode);
	
			engineeringNode = new DefaultMutableTreeNode("(3). Engineering");
			engineeringSpecialistNode = new DefaultMutableTreeNode(
					RoleType.ENGINEERING_SPECIALIST.toString());
			engineeringChiefNode = new DefaultMutableTreeNode(
					RoleType.CHIEF_OF_ENGINEERING.toString());
	
			nodes.add(engineeringNode);
			nodes.add(engineeringSpecialistNode);
			nodes.add(engineeringChiefNode);
	
			logisticNode = new DefaultMutableTreeNode("(4). Logistic");
			logisticSpecialistNode = new DefaultMutableTreeNode(
					RoleType.LOGISTIC_SPECIALIST.toString());
			logisticChiefNode = new DefaultMutableTreeNode(
					RoleType.CHIEF_OF_LOGISTICS_N_OPERATIONS.toString());
	
			nodes.add(logisticNode);
			nodes.add(logisticSpecialistNode);
			nodes.add(logisticChiefNode);
	
			missionNode = new DefaultMutableTreeNode("(5). Mission");
			missionSpecialistNode = new DefaultMutableTreeNode(
					RoleType.MISSION_SPECIALIST.toString());
			missionChiefNode = new DefaultMutableTreeNode(
					RoleType.CHIEF_OF_MISSION_PLANNING.toString());
	
			nodes.add(missionNode);
			nodes.add(missionSpecialistNode);
			nodes.add(missionChiefNode);
	
			safetyNode = new DefaultMutableTreeNode("(6). Safety");
			safetySpecialistNode = new DefaultMutableTreeNode(RoleType.SAFETY_SPECIALIST.toString());
			safetyChiefNode = new DefaultMutableTreeNode(
					RoleType.CHIEF_OF_SAFETY_N_HEALTH.toString());
	
			nodes.add(safetyNode);
			nodes.add(safetySpecialistNode);
			nodes.add(safetyChiefNode);
	
			scienceNode = new DefaultMutableTreeNode("(7). Science");
			scienceSpecialistNode = new DefaultMutableTreeNode(
					RoleType.SCIENCE_SPECIALIST.toString());
			scienceChiefNode = new DefaultMutableTreeNode(RoleType.CHIEF_OF_SCIENCE);
	
			nodes.add(scienceNode);
			nodes.add(scienceSpecialistNode);
			nodes.add(scienceChiefNode);
	
			supplyNode = new DefaultMutableTreeNode("(8). Supply");
			supplySpecialistNode = new DefaultMutableTreeNode(
					RoleType.RESOURCE_SPECIALIST.toString());
			supplyChiefNode = new DefaultMutableTreeNode(RoleType.CHIEF_OF_SUPPLY_N_RESOURCES);
	
			nodes.add(supplyNode);
			nodes.add(supplySpecialistNode);
			nodes.add(supplyChiefNode);
		}
	}

	public void deleteAllNodes() {
		nodes.clear();
		root.removeAllChildren();
	}

	public void constructNodes() {
		int population = settlement.getNumCitizens();
		
		if (population <= ChainOfCommand.POPULATION_WITH_COMMANDER) {
			root.add(commanderNode);
			root.add(crewNode);
			return;
		}
		
		if (population >= ChainOfCommand.POPULATION_WITH_CHIEFS) {

			divisionNode.add(agricultureNode);
			agricultureNode.add(agricultureChiefNode);
			agricultureNode.add(agricultureSpecialistNode);

			divisionNode.add(computingNode);
			computingNode.add(computingChiefNode);
			computingNode.add(computingSpecialistNode);

			divisionNode.add(engineeringNode);
			engineeringNode.add(engineeringChiefNode);
			engineeringNode.add(engineeringSpecialistNode);

			divisionNode.add(logisticNode);
			logisticNode.add(logisticChiefNode);
			logisticNode.add(logisticSpecialistNode);

			divisionNode.add(missionNode);
			missionNode.add(missionChiefNode);
			missionNode.add(missionSpecialistNode);

			divisionNode.add(safetyNode);
			safetyNode.add(safetyChiefNode);
			safetyNode.add(safetySpecialistNode);

			divisionNode.add(scienceNode);
			scienceNode.add(scienceChiefNode);
			scienceNode.add(scienceSpecialistNode);

			divisionNode.add(supplyNode);
			supplyNode.add(supplyChiefNode);
			supplyNode.add(supplySpecialistNode);

		}

		else {

			divisionNode.add(agricultureNode);
			agricultureNode.add(agricultureSpecialistNode);

			divisionNode.add(computingNode);
			computingNode.add(computingSpecialistNode);

			divisionNode.add(engineeringNode);
			engineeringNode.add(engineeringSpecialistNode);

			divisionNode.add(logisticNode);
			logisticNode.add(logisticSpecialistNode);

			divisionNode.add(missionNode);
			missionNode.add(missionSpecialistNode);

			divisionNode.add(safetyNode);
			safetyNode.add(safetySpecialistNode);

			divisionNode.add(scienceNode);
			scienceNode.add(scienceSpecialistNode);

			divisionNode.add(supplyNode);
			supplyNode.add(supplySpecialistNode);
		}


		if (population >= ChainOfCommand.POPULATION_WITH_PRESIDENT) {
			root.add(commanderStaffNode);
			commanderStaffNode.add(presidentNode);
			commanderStaffNode.add(mayorNode);
		}
		
		else if (population >= ChainOfCommand.POPULATION_WITH_MAYOR) {
			root.add(commanderStaffNode);
			commanderStaffNode.add(mayorNode);
		}

		else if (population >= ChainOfCommand.POPULATION_WITH_SUB_COMMANDER) {
			root.add(commanderStaffNode);
			commanderStaffNode.add(commanderNode);
			commanderStaffNode.add(subCommanderNode);
		}

		else if (population > ChainOfCommand.POPULATION_WITH_COMMANDER) {
			root.add(commanderStaffNode);
			commanderStaffNode.add(commanderNode);
		}

		root.add(divisionNode);
	}


	public void considerRoles() {

		Collection<Person> people = settlement.getAllAssociatedPeople();

		int population = people.size();
		
		for (Person p : people) {

			addListener(p);

			roles.clear();

			RoleType rt = p.getRole().getType();

			roles.put(p, rt);

			if (population <= ChainOfCommand.POPULATION_WITH_COMMANDER) {
				if (rt == RoleType.COMMANDER) {
					commanderNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.CREW_ENGINEER) {
					crewEngineerNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.CREW_HEALTH_N_SAFETY_OFFICER) {
					crewHealthNSafetyOfficerNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.CREW_OPERATION_OFFICER) {
					crewOperationOfficerNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.CREW_SCIENTIST) {
					crewScientistNode.add(new DefaultMutableTreeNode(p));	
				}
			}
			
			else {
				if (rt == RoleType.COMMANDER) {
					commanderNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.SUB_COMMANDER) {
					subCommanderNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.MAYOR) {
					mayorNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.PRESIDENT) {
					presidentNode.add(new DefaultMutableTreeNode(p));
					
				} else if (rt == RoleType.CHIEF_OF_AGRICULTURE) {
					agricultureChiefNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.AGRICULTURE_SPECIALIST) {
					agricultureSpecialistNode.add(new DefaultMutableTreeNode(p));

				} else if (rt == RoleType.CHIEF_OF_COMPUTING) {
					computingChiefNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.COMPUTING_SPECIALIST) {
					computingSpecialistNode.add(new DefaultMutableTreeNode(p));

				} else if (rt == RoleType.CHIEF_OF_ENGINEERING) {
					engineeringChiefNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.ENGINEERING_SPECIALIST) {
					engineeringSpecialistNode.add(new DefaultMutableTreeNode(p));

				} else if (rt == RoleType.CHIEF_OF_LOGISTICS_N_OPERATIONS) {
					logisticChiefNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.LOGISTIC_SPECIALIST) {
					logisticSpecialistNode.add(new DefaultMutableTreeNode(p));

				} else if (rt == RoleType.CHIEF_OF_MISSION_PLANNING) {
					missionChiefNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.MISSION_SPECIALIST) {
					missionSpecialistNode.add(new DefaultMutableTreeNode(p));

				} else if (rt == RoleType.CHIEF_OF_SAFETY_N_HEALTH) {
					safetyChiefNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.SAFETY_SPECIALIST) {
					safetySpecialistNode.add(new DefaultMutableTreeNode(p));

				} else if (rt == RoleType.CHIEF_OF_SCIENCE) {
					scienceChiefNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.SCIENCE_SPECIALIST) {
					scienceSpecialistNode.add(new DefaultMutableTreeNode(p));

				} else if (rt == RoleType.CHIEF_OF_SUPPLY_N_RESOURCES) {
					supplyChiefNode.add(new DefaultMutableTreeNode(p));
				} else if (rt == RoleType.RESOURCE_SPECIALIST) {
					supplySpecialistNode.add(new DefaultMutableTreeNode(p));

				} else {
					// anyone who does not belong will be placed in the root node
					DefaultMutableTreeNode node = new DefaultMutableTreeNode(p);
					root.add(node);
				}
			}
		}
	}

	public void setupMouseOnNodes() {
		MouseListener ml = new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				int selRow = tree.getRowForLocation(e.getX(), e.getY());
				TreePath path = tree.getPathForLocation(e.getX(), e.getY());
				if (selRow != -1) {
					if (e.getClickCount() == 2) {
						DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();

						// Check for node to avoid java.lang.ClassCastException:
						// java.lang.String cannot be cast to com.mars_sim.core.person.Person
						if (node.getUserObject() instanceof Person) {
							Person person = (Person) node.getUserObject();
							if (person != null) {
								getDesktop().showDetails(person);
							}
						}
					}
				}
			}
		};

		tree.addMouseListener(ml);

	}

	public void editIcons(JTree tree) {

		tree.setCellRenderer(new DefaultTreeCellRenderer() {
			private Icon personIcon = UIManager.getIcon("RadioButton.icon"); // OptionPane.errorIcon");
			private Icon roleIcon = UIManager.getIcon("FileChooser.detailsViewIcon");// OptionPane.informationIcon");
			private Icon homeIcon = UIManager.getIcon("FileChooser.homeFolderIcon");

			@Override
			public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
					boolean isLeaf, int row, boolean focused) {
				Component c = super.getTreeCellRendererComponent(tree, value, selected, expanded, isLeaf, row, focused);
				// if (selected)
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


	/**
	 * Reloads the root.
	 */
	public void reloadTree() {
		defaultTreeModel.reload(root); // notify changes to model
		tree.expandPath(tree.getSelectionPath());
		for (int i = 0; i < tree.getRowCount(); i++)
			tree.expandRow(i);
	}

	/**
	 * Empties the nodes.
	 */
	public void emptyNodes() {

		for (int i = 0; i < tree.getRowCount(); i++)
			tree.collapseRow(i);

		deleteAllNodes();
	}

	/**
	 * Removes the listener for a person.
	 */
	public void removeListener(Person p) {
//		for (Person p : listeners.keySet()) {
			p.removeUnitListener(listeners.get(p));
//		}
		listeners.remove(p);
	}

	/**
	 * Removes the listener for a person.
	 */
	public void addListener(Person p) {
		PersonListener pl = new PersonListener();
		p.addUnitListener(pl);
		listeners.put(p, pl);
	}
	/**
	 * PersonListener class listens to the change of each settler in a settlement.
	 */
	private class PersonListener implements UnitListener {

		/**
		 * Catch unit update event.
		 *
		 * @param event the unit event.
		 */
		public void unitUpdate(UnitEvent event) {
			if (event.getType() == UnitEventType.ROLE_EVENT) {
				Unit unit = (Unit)event.getSource();
				if (unit.getUnitType() == UnitType.PERSON) {
					Person p = (Person) unit;
					if (p.getAssociatedSettlement() == settlement) {
						emptyNodes();
						buildTreeNodes();
						initNodes();
						reloadTree();
					}
				}
			}
		}
	}

	/**
	 * UnitManagerListener inner class.
	 */
	private class LocalUnitManagerListener implements UnitManagerListener {

		/**
		 * Catches unit manager update event.
		 *
		 * @param event the unit event.
		 */
		public void unitManagerUpdate(UnitManagerEvent event) {
			Unit unit = event.getUnit();
			UnitManagerEventType eventType = event.getEventType();
			if (unit.getUnitType() == UnitType.PERSON) {
				if (eventType == UnitManagerEventType.ADD_UNIT) {
					// TODO: should only add/remove the affected person's listener and node
					addListener((Person) unit);
					emptyNodes();
					buildTreeNodes();
					initNodes();
					reloadTree();
				}

				else if (eventType == UnitManagerEventType.REMOVE_UNIT) {
					// TODO: should only add/remove the affected person's listener and node
					removeListener((Person) unit);
					emptyNodes();
					buildTreeNodes();
					initNodes();
					reloadTree();
				}
			}
		}
	}

	class MyTreeModelListener implements TreeModelListener {
	    public void treeNodesChanged(TreeModelEvent e) {
	        DefaultMutableTreeNode node;
	        node = (DefaultMutableTreeNode)
	                 (e.getTreePath().getLastPathComponent());
	        /*
	         * If the event lists children, then the changed
	         * node is the child of the node we have already
	         * gotten.  Otherwise, the changed node and the
	         * specified node are the same.
	         */
	        
	        try {
	            int index = e.getChildIndices()[0];
	            node = (DefaultMutableTreeNode)
	                   (node.getChildAt(index));
	        } catch (NullPointerException exc) {}

	        logger.info(settlement, "The user has finished editing the node.");
	        logger.info(settlement, "New value: " + node.getUserObject());
	    }
	    public void treeNodesInserted(TreeModelEvent e) {
	    }
	    public void treeNodesRemoved(TreeModelEvent e) {
	    }
	    public void treeStructureChanged(TreeModelEvent e) {
	    }
	}
	
	/**
	 * Prepares objects for garbage collection.
	 */
	@Override
	public void destroy() {
		super.destroy();
		
		UnitManager unitManager = getSimulation().getUnitManager();
		unitManager.removeUnitManagerListener(UnitType.PERSON, unitManagerListener);
		
		// take care to avoid null exceptions
		settlement = null;
		tree = null;
		root = null;
		roles = null;
		nodes = null;
		listeners = null;
		unitManagerListener = null;
		
		defaultTreeModel = null;
		commanderStaffNode = null;
		commanderNode = null;
		subCommanderNode = null;
		divisionNode = null;
		mayorNode = null;
		agricultureNode = null;
		agricultureSpecialistNode = null;
		agricultureChiefNode = null;
		computingNode = null;
		computingSpecialistNode = null;
		computingChiefNode = null;
		engineeringNode = null;
		engineeringSpecialistNode = null;
		engineeringChiefNode = null;
		logisticNode = null;
		logisticSpecialistNode = null;
		logisticChiefNode = null;
		missionNode = null;
		missionSpecialistNode = null;
		missionChiefNode = null;
		safetyNode = null;
		safetySpecialistNode = null;
		safetyChiefNode = null;
		scienceNode = null;
		scienceSpecialistNode = null;
		scienceChiefNode = null;
		supplyNode = null;
		supplySpecialistNode = null;
		supplyChiefNode = null;
	}
}
