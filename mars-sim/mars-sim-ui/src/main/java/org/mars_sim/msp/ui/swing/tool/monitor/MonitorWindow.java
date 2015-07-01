/**
 * Mars Simulation Project
 * MonitorWindow.java
 * @version 3.07 2015-01-21
 * @author Barry Evans
 */
package org.mars_sim.msp.ui.swing.tool.monitor;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.notification.NotificationWindow;
import org.mars_sim.msp.ui.swing.tool.ToolWindow;

import com.jidesoft.plaf.LookAndFeelFactory;
import com.jidesoft.plaf.UIDefaultsLookup;
import com.jidesoft.swing.JideTabbedPane;
import com.jidesoft.swing.Searchable;
import com.jidesoft.swing.SearchableBar;
//import com.jidesoft.swing.SearchableBar;
import com.jidesoft.swing.SearchableUtils;
import com.jidesoft.swing.TableSearchable;

/**
 * The MonitorWindow is a tool window that displays a selection of tables
 * each of which monitor a set of Units.
 */
public class MonitorWindow
extends ToolWindow
implements TableModelListener, ActionListener {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	final private static int STATUSHEIGHT = 25;

	public static final String NAME = Msg.getString("MonitorWindow.title"); //$NON-NLS-1$

	// 2015-06-20 Added an custom icon for each tab
	public static final String PEOPLE_ICON = "people_32";
	public static final String BOT_ICON = "bot_32";
	public static final String BUS_ICON = "bus_32";
	public static final String BASE_ICON = "base_32";
	public static final String CARROT_ICON = "carrot_32";
	public static final String TRASH_ICON = "trash_32";
	public static final String CENTERMAP_ICON = "centermap_32";
	public static final String FIND_ICON = "find_32";
	public static final String BRIEFCASE_ICON = "briefcase_32";
	public static final String COLUMN_ICON = "column_32";
	public static final String FILTER_ICON = "filter_32";

	// Data members
	//private JTabbedPane tabsSection;
	private JideTabbedPane tabsSection;
	private JLabel rowCount;
	private ArrayList<MonitorTab> tabs = new ArrayList<MonitorTab>();
	/** Tab showing historical events. */
	private EventTab eventsTab;
	private MonitorTab oldTab = null;
	private JButton buttonPie;
	private JButton buttonBar;
	private JButton buttonRemoveTab;
	private JButton buttonMap;
	private JButton buttonDetails;
	private JButton buttonMissions;
	private JButton buttonFilter;
	private JButton buttonProps;

	private MainDesktopPane desktop;

	private JPanel statusPanel;
	private JTable table ;
	private Searchable searchable ;
	private SearchableBar _tableSearchableBar;

	/**
	 * Constructor.
	 * @param desktop the desktop pane
	 */
	public MonitorWindow(MainDesktopPane desktop) {

		// Use TableWindow constructor
		super(NAME, desktop);
		this.desktop = desktop;
		this.setOpaque(true);
		//this.setBackground(new Color(205, 133, 63, 50));//Color.ORANGE);
		//this.setBackground(new Color(0, 0, 0, 0));

		setMaximizable(true);

		// Get content pane
		JPanel mainPane = new JPanel(new BorderLayout());
		mainPane.setBorder(new MarsPanelBorder());
		setContentPane(mainPane);

		// Create toolbar
		JToolBar toolbar = new JToolBar();
		toolbar.setFloatable(false);
		mainPane.add(toolbar, BorderLayout.NORTH);

		// Create graph button
		buttonPie = new JButton(PieChartTab.PIEICON);
		buttonPie.setToolTipText(Msg.getString("MonitorWindow.tooltip.singleColumnPieChart")); //$NON-NLS-1$
		buttonPie.addActionListener(this);
		toolbar.add(buttonPie);

		buttonBar = new JButton(BarChartTab.BARICON);
		buttonBar.setToolTipText(Msg.getString("MonitorWindow.tooltip.multipleColumnBarChart")); //$NON-NLS-1$
		buttonBar.addActionListener(this);
		toolbar.add(buttonBar);

		//buttonRemoveTab = new JButton(ImageLoader.getIcon(Msg.getString("img.tabRemove"))); //$NON-NLS-1$
		buttonRemoveTab = new JButton(ImageLoader.getNewIcon(TRASH_ICON)); //$NON-NLS-1$
		buttonRemoveTab.setToolTipText(Msg.getString("MonitorWindow.tooltip.tabRemove")); //$NON-NLS-1$
		buttonRemoveTab.addActionListener(this);
		toolbar.add(buttonRemoveTab);
		toolbar.addSeparator();

		// Create buttons based on selection
		//buttonMap = new JButton(ImageLoader.getIcon(Msg.getString("img.centerMap"))); //$NON-NLS-1$
		buttonMap = new JButton(ImageLoader.getNewIcon(CENTERMAP_ICON)); //$NON-NLS-1$
		buttonMap.setMargin(new Insets(3, 4, 4, 4));
		buttonMap.setToolTipText(Msg.getString("MonitorWindow.tooltip.centerMap")); //$NON-NLS-1$
		buttonMap.addActionListener(this);
		toolbar.add(buttonMap);

		//buttonDetails = new JButton(ImageLoader.getIcon(Msg.getString("img.showDetails"))); //$NON-NLS-1$
		buttonDetails = new JButton(ImageLoader.getNewIcon(FIND_ICON)); //$NON-NLS-1$
		buttonDetails.setToolTipText(Msg.getString("MonitorWindow.tooltip.showDetails")); //$NON-NLS-1$
		buttonDetails.addActionListener(this);
		toolbar.add(buttonDetails);

		//buttonMissions = new JButton(ImageLoader.getIcon(Msg.getString("img.mission"))); //$NON-NLS-1$
		buttonMissions = new JButton(ImageLoader.getNewIcon(BRIEFCASE_ICON)); //$NON-NLS-1$
		buttonMissions.setToolTipText(Msg.getString("MonitorWindow.tooltip.mission")); //$NON-NLS-1$
		buttonMissions.addActionListener(this);
		toolbar.add(buttonMissions);
		toolbar.addSeparator();

		//buttonProps = new JButton(ImageLoader.getIcon(Msg.getString("img.preferences"))); //$NON-NLS-1$
		buttonProps = new JButton(ImageLoader.getNewIcon(COLUMN_ICON)); //$NON-NLS-1$
		buttonProps.setToolTipText(Msg.getString("MonitorWindow.tooltip.preferences")); //$NON-NLS-1$
		buttonProps.addActionListener(this);
		toolbar.add(buttonProps);
		toolbar.addSeparator();

		//buttonFilter = new JButton(ImageLoader.getIcon(Msg.getString("img.categoryFilter"))); //$NON-NLS-1$
		buttonFilter = new JButton(ImageLoader.getNewIcon(FILTER_ICON)); //$NON-NLS-1$
		buttonFilter.setToolTipText(Msg.getString("MonitorWindow.tooltip.categoryFilter")); //$NON-NLS-1$
		buttonFilter.addActionListener(this);
		toolbar.add(buttonFilter);

		// Create tabbed pane for the table
		tabsSection = new JideTabbedPane();
        LookAndFeelFactory.installJideExtension(LookAndFeelFactory.OFFICE2003_STYLE);
		tabsSection.setBoldActiveTab(true);
		tabsSection.setScrollSelectedTabOnWheel(true);
		//tabsSection.setTabColorProvider(JideTabbedPane.ONENOTE_COLOR_PROVIDER);
		tabsSection.setTabShape(JideTabbedPane.SHAPE_WINDOWS_SELECTED);
		tabsSection.setColorTheme(JideTabbedPane.COLOR_THEME_OFFICE2003); //COLOR_THEME_VSNET);
		//tabsSection.setBackground(UIDefaultsLookup.getColor("control"));
		tabsSection.setTabPlacement(JideTabbedPane.BOTTOM);
		tabsSection.setForeground(Color.DARK_GRAY);
		mainPane.add(tabsSection, BorderLayout.CENTER);

		// Create a status panel
		statusPanel = new JPanel();
		statusPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 1));
		mainPane.add(statusPanel, BorderLayout.SOUTH);

		// Status item for row
		rowCount = new JLabel("  "); //$NON-NLS-1$
		rowCount.setHorizontalAlignment(SwingConstants.LEFT);
		rowCount.setBorder(BorderFactory.createLoweredBevelBorder());
		statusPanel.add(rowCount);
		Dimension dims = new Dimension(120, STATUSHEIGHT);
		rowCount.setPreferredSize(dims);

		// Add the default table tabs
		UnitManager unitManager = Simulation.instance().getUnitManager();

		// 2014-11-29 Added notifyBox
		NotificationWindow notifyBox = new NotificationWindow(desktop);

		// 2015-01-21 Added RobotTableModel
		addTab(new UnitTab(this, new RobotTableModel(unitManager, desktop), true, BOT_ICON));
		// 2014-10-14 mkung: added FoodTableModel
		addTab(new UnitTab(this, new CropTableModel(unitManager), true, CARROT_ICON));
		// 2014-11-29 Added notifyBox 2015-01-15 Added desktop
		eventsTab = new EventTab(this, notifyBox, desktop);

		addTab(eventsTab);
		// 2014-11-25 mkung: added FoodInventoryTab()
		addTab(new FoodInventoryTab(this));

		addTab(new TradeTab(this));

		addTab(new MissionTab(this));

		addTab(new UnitTab(this, new PersonTableModel(unitManager, desktop), true, PEOPLE_ICON));

		addTab(new UnitTab(this, new SettlementTableModel(unitManager), true, BASE_ICON));

		addTab(new UnitTab(this, new VehicleTableModel(unitManager), true, BUS_ICON));

		// Open the people tab
		tabsSection.setSelectedIndex(6);
		tabChanged();

		// Add a listener for the tab changes
		tabsSection.addChangeListener(
			new ChangeListener() {
				public void stateChanged(ChangeEvent e) {
					tabChanged();
				}
			}
		);

		// 2015-06-17 Added createSearchBar();
		//setTable();
		//createSearchableBar();
		statusPanel.add(_tableSearchableBar); // , BorderLayout.AFTER_LAST_LINE);
		statusPanel.invalidate();
		statusPanel.revalidate();


		// Have to define a starting size
		// 2014-10-10 mkung: changed the horizontal resolution from 600 to 800 to accommodate the addition
		// of 3 columns (Grains, Fruits, Vegetables)
		setPreferredSize(new Dimension(1024, 512));
		setMinimumSize(new Dimension(1024, 512));
		setSize(new Dimension(1024, 512));

		pack();
	}


    public void setTable() {
        //System.out.println("setTable()");
		MonitorTab monitorTab = getSelected();
    	if (searchable != null)
    		SearchableUtils.uninstallSearchable(table);
		TableTab tableTab = (TableTab) monitorTab;
    	table = tableTab.getTable();
		//System.out.println("tab : " + tableTab + "\n    table : " + table);
    }

    //public void switchTable() {
    //	_tableSearchableBar.getSearchable();
    //}

    public void createSearchableBar() {
        //System.out.println("createSearchableBar()");
    	if (searchable != null)
    		SearchableUtils.uninstallSearchable(searchable);

       	searchable = SearchableUtils.installSearchable(table);
        searchable.setRepeats(true);
        //searchable.setPopupTimeout(5000);
     	searchable.setCaseSensitive(false);

		if (_tableSearchableBar != null) {
			_tableSearchableBar.setSearchingText("");
			_tableSearchableBar = null;
			//statusPanel.remove(_tableSearchableBar);
		}

        _tableSearchableBar = new SearchableBar(searchable);
        _tableSearchableBar.setCompact(false);
		_tableSearchableBar.setToolTipText("Type in your search terms");
		((TableSearchable) searchable).setMainIndex(-1); // -1 = search for all columns
		//_tableSearchableBar.setVisibleButtons(_tableSearchableBar.getVisibleButtons());
	    //_tableSearchableBar.setName("TableSearchableBar");
	    _tableSearchableBar.setShowMatchCount(true);
	    _tableSearchableBar.setVisible(true);

		//statusPanel.add(_tableSearchableBar); // , BorderLayout.AFTER_LAST_LINE);
		//statusPanel.invalidate();
		//statusPanel.revalidate();
    }


	/**
	 * This method add the specified Unit table as a new tab in the Monitor. The
	 * model is displayed as a table by default.
	 * The name of the tab is that of the Model.
	 *
	 * @param model The new model to display.
	 */
	public void displayModel(UnitTableModel model) {
		if (containsModel(model)) tabsSection.setSelectedIndex(getModelIndex(model));
		else addTab(new UnitTab(this,model, false, "user_32"));
	}

	/**
	 * Checks if a monitor tab contains this model.
	 * @param model the model to check for.
	 * @return true if a tab contains the model.
	 */
	public boolean containsModel(UnitTableModel model) {
		boolean result = false;
		Iterator<MonitorTab> i = tabs.iterator();
		while (i.hasNext()) {
			if (i.next().getModel().equals(model)) result = true;
		}
		return result;
	}

	/**
	 * Gets the index of the monitor tab with the model.
	 * @param model the model to check for.
	 * @return tab index or -1 if none.
	 */
	public int getModelIndex(UnitTableModel model) {
		int result = -1;
		Iterator<MonitorTab> i = tabs.iterator();
		while (i.hasNext()) {
			MonitorTab tab = i.next();
			if (tab.getModel().equals(model)) result = tabs.indexOf(tab);
		}
		return result;
	}

	/**
	 * This method creates a new chart window based on the model of the
	 * currently selected window. The chart is added as a separate tab to the
	 * window.
	 */
	private void createBarChart() {
		MonitorModel model = getSelected().getModel();

		// Show modal column selector
		int columns[] = ColumnSelector.createBarSelector(desktop.getMainWindow().getFrame(), model);
		if (columns.length > 0) {
			addTab(new BarChartTab(model, columns));
		}
	}

	private void createPieChart() {
		MonitorModel model = getSelected().getModel();

		// Show modal column selector
		int column = ColumnSelector.createPieSelector(desktop.getMainWindow().getFrame(), model);
		if (column >= 0) {
			addTab(new PieChartTab(model, column));
		}
	}
	/**
	 * Return the currently selected tab.
	 *
	 * @return Monitor tab being displayed.
	 */
	private MonitorTab getSelected() {
		MonitorTab selected = null;
		int selectedIdx = tabsSection.getSelectedIndex();
		if ((selectedIdx != -1) && (selectedIdx < tabs.size()))
			selected = tabs.get(selectedIdx);
		return selected;
	}

	private void tabChanged() {
		//System.out.println("tabChanged()");
		MonitorTab selected = getSelected();
		if (selected != null) {
			String status = selected.getCountString();
			rowCount.setText(status);
			if (oldTab != null) {
				MonitorModel model = oldTab.getModel();
				if (model != null) oldTab.getModel().removeTableModelListener(this);
			}
			selected.getModel().addTableModelListener(this);
			oldTab = selected;

			// 2015-06-17 Added setTable();
			setTable();
			//statusPanel.remove(_tableSearchableBar);
			createSearchableBar();

			// Enable/disable buttons based on selected tab.
			buttonMap.setEnabled(false);
			buttonDetails.setEnabled(false);
			buttonMissions.setEnabled(false);
			buttonFilter.setEnabled(false);

			if (selected instanceof UnitTab) {
				buttonMap.setEnabled(true);
				buttonDetails.setEnabled(true);
			}
			else if (selected instanceof MissionTab) {
				buttonMap.setEnabled(true);
				buttonMissions.setEnabled(true);
			}
			else if (selected instanceof EventTab) {
				buttonMap.setEnabled(true);
				buttonDetails.setEnabled(true);
				buttonFilter.setEnabled(true);
			}
		}

		SwingUtilities.updateComponentTreeUI(this);
	}

	public void tableChanged(TableModelEvent e) {
		if (e.getType() != TableModelEvent.UPDATE) {
			MonitorTab selected = getSelected();
			if (selected != null) {
				String status = selected.getCountString();
				rowCount.setText(status);
			}
		}
		//System.out.println("tableChanged()");
	}

	private void addTab(MonitorTab newTab) {
		tabs.add(newTab);
		tabsSection.addTab(newTab.getName(), newTab.getIcon(), newTab);
		tabsSection.setSelectedIndex(tabs.size()-1);
		tabChanged();
	}

	private void removeTab(MonitorTab oldTab) {
		tabs.remove(oldTab);
		tabsSection.remove(oldTab);

		oldTab.removeTab();
		if (getSelected() == oldTab) {
			tabsSection.setSelectedIndex(0);
		}
		tabChanged();
	}

	private void centerMap() {
		MonitorTab selected = getSelected();
		if (selected != null) {
			selected.centerMap(desktop);
		}
	}

	public void displayDetails() {
		MonitorTab selected = getSelected();
		if (selected != null) {
			selected.displayDetails(desktop);
		}
	}

	private void displayMission() {
		MonitorTab selected = getSelected();
		if ((selected instanceof MissionTab) && (selected != null)) {
			((MissionTab) selected).displayMission(desktop);
		}
	}

	private void displayProps() {
		MonitorTab selected = getSelected();
		if (selected != null) {
			selected.displayProps(desktop);
		}
	}

	private void filterCategories() {
		EventTab events = eventsTab;
		if (events != null) {
			events.filterCategories(desktop);
		}
	}

	/**
	 * Prepare tool window for deletion.
	 */
	 public void destroy() {
		 Iterator<MonitorTab> i = tabs.iterator();
		 while (i.hasNext()) i.next().removeTab();
		 tabs.clear();
	 }

	@Override
	public void actionPerformed(ActionEvent e) {
		Object source = e.getSource();
		if (source == this.buttonPie) {
			createPieChart();
		} else if (source == this.buttonBar) {
			createBarChart();
		} else if (source == this.buttonRemoveTab) {
			MonitorTab selected = getSelected();
			if (!selected.getMandatory()) {
				removeTab(getSelected());
			}
		} else if (source == this.buttonDetails) {
			displayDetails();
		} else if (source == this.buttonMap) {
			centerMap();
		} else if (source == this.buttonMissions) {
			displayMission();
		} else if (source == this.buttonProps) {
			displayProps();
		} else if (source == this.buttonFilter) {
			filterCategories();
		}
	}

}