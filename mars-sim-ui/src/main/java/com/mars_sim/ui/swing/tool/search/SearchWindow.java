/*
 * Mars Simulation Project
 * SearchWindow.java
 * @date 2023-07-10
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.tool.search;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.icons.FlatSearchWithHistoryIcon;
import com.mars_sim.core.CollectionUtils;
import com.mars_sim.core.Unit;
import com.mars_sim.core.UnitManager;
import com.mars_sim.core.UnitType;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.MarsPanelBorder;
import com.mars_sim.ui.swing.tool_window.MapSelector;
import com.mars_sim.ui.swing.tool_window.ToolWindow;


/**
 * The SearchWindow is a tool window that allows the user to search
 * for individual units by name and category.
 */
@SuppressWarnings("serial")
public class SearchWindow
extends ToolWindow {

	/** Tool name. */
	public static final String NAME = "search";
	public static final String ICON = "action/find";
    public static final String TITLE = Msg.getString("SearchWindow.title");

	/** True if unitList selection events should be ignored. */
	private boolean lockUnitList;
	/** True if selectTextField events should be ignored. */
	private boolean lockSearchText;
	
	/** Category selector. */
	private JComboBox<UnitType> searchForSelect;
	/** List of selectable units. */
	private JList<Unit> unitList;
	/** Model for unit select list. */
	private UnitListModel unitListModel;
	/** Selection text field. */
	private JTextField selectTextField;
	/** Status label for displaying warnings. */
	private JLabel statusLabel;
	/** Checkbox to indicate if unit window is to be opened. */
	private JCheckBox openWindowCheck;
	/** Checkbox to indicate if  map is to be centered on unit. */
	private JCheckBox showMapCheck;
	/** Button to execute the search of the selected unit. */
	private JButton searchButton;

	private UnitManager unitManager;
	
	private List<String> history = new ArrayList<>();

	/**
	 * Constructor.
	 * @param desktop {@link MainDesktopPane} the desktop pane
	 */
	public SearchWindow(MainDesktopPane desktop) {

		// Use ToolWindow constructor
		super(NAME, TITLE, desktop);
		unitManager = desktop.getSimulation().getUnitManager();
	
		// Initialize locks
		lockUnitList = false;
		lockSearchText = false;
		
		// Get content pane
		JPanel mainPane = new JPanel(new BorderLayout());
		mainPane.setBorder(new MarsPanelBorder());
		setContentPane(mainPane);

		// Create search for panel
		JPanel searchForPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
		searchForPane.setPreferredSize(new Dimension(240, 26));
		mainPane.add(searchForPane, BorderLayout.NORTH);

		// Create search for label
		JLabel searchForLabel = new JLabel(Msg.getString("SearchWindow.searchFor")); //$NON-NLS-1$
		searchForPane.add(searchForLabel);

		// Create search for select
		UnitType[] categories = {
			UnitType.PERSON,
			UnitType.SETTLEMENT,
			UnitType.VEHICLE,
			UnitType.ROBOT,
			UnitType.EVA_SUIT
		};
		searchForSelect = new JComboBox<>(categories);
		searchForSelect.setRenderer(new UnitTypeRenderer());
		searchForSelect.setSelectedIndex(0);
		searchForSelect.addItemListener(event -> changeCategory((UnitType) searchForSelect.getSelectedItem()));
		searchForPane.add(searchForSelect);

		// Create select unit panel
		JPanel selectUnitPane = new JPanel(new BorderLayout());
		mainPane.add(selectUnitPane, BorderLayout.CENTER);
		
		// Create select text field
		selectTextField = new JTextField();
		selectTextField.getDocument().addDocumentListener(new DocumentListener() {
			public void changedUpdate(DocumentEvent event) {
				// Not needed
			}
			public void insertUpdate(DocumentEvent event) {
				searchTextChange();
				searchButton.setEnabled(true);
			}
			public void removeUpdate(DocumentEvent event) {
				searchTextChange();
				searchButton.setEnabled(false);
			}
		});
		selectUnitPane.add(selectTextField, BorderLayout.NORTH);
					
		// search history button
		JButton searchHistoryButton = new JButton(new FlatSearchWithHistoryIcon(true));
		searchHistoryButton.setToolTipText("Search History");
		searchHistoryButton.addActionListener( e -> {
			JPopupMenu popupMenu = new JPopupMenu();
			int size = history.size();
			if (history.isEmpty()) {
				popupMenu.add("(empty)");
			}
			else {
				for (int i = 0; i < size; i++) {
					popupMenu.add(history.get(i));
				}
			}
			popupMenu.show(searchHistoryButton, 0, searchHistoryButton.getHeight());
		} );
		
		// Add leading/trailing icons to text fields
		selectTextField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Search");
		selectTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_LEADING_COMPONENT, searchHistoryButton);
		selectTextField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true);
		
		// Create unit list
		unitListModel = new UnitListModel(UnitType.PERSON);
		unitList = new JList<>(unitListModel);
		unitList.setSelectedIndex(0);
		unitList.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent event) {
				if (event.getClickCount() == 2) search();
				else if (!lockUnitList) {
					// Change search text to selected name.
					String selectedUnitName = unitList.getSelectedValue().getName();
					lockSearchText = true;
					if (!selectTextField.getText().equals(selectedUnitName))
						selectTextField.setText(selectedUnitName);
					lockSearchText = false;
				}
			}
		});
		selectUnitPane.add(new JScrollPane(unitList), BorderLayout.CENTER);

		// Create bottom panel
		JPanel bottomPane = new JPanel(new BorderLayout());
		mainPane.add(bottomPane, BorderLayout.SOUTH);

		// Create select options panel
		JPanel selectOptionsPane = new JPanel(new GridLayout(2, 1));
		bottomPane.add(selectOptionsPane, BorderLayout.NORTH);

		// Create open the unit window
		openWindowCheck = new JCheckBox(Msg.getString("SearchWindow.openWindow")); //$NON-NLS-1$
		openWindowCheck.setSelected(true);
		selectOptionsPane.add(openWindowCheck);

		// Create open the map
		showMapCheck = new JCheckBox(Msg.getString("SearchWindow.openNav")); //$NON-NLS-1$
		selectOptionsPane.add(showMapCheck);

		// Create status label
		statusLabel = new JLabel(" ", SwingConstants.CENTER); //$NON-NLS-1$
		statusLabel.setBorder(new EtchedBorder());
		bottomPane.add(statusLabel, BorderLayout.CENTER);

		// Create search button panel
		JPanel searchButtonPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		bottomPane.add(searchButtonPane, BorderLayout.SOUTH);

		// Create search button
		searchButton = new JButton(Msg.getString("SearchWindow.button.search")); //$NON-NLS-1$
		searchButton.addActionListener(e -> search());
		searchButton.setEnabled(false);
		searchButton.setToolTipText(Msg.getString("SearchWindow.button.toolTip"));
		searchButtonPane.add(searchButton);

		// Pack window
		pack();
	}

	/**
	 * Searches for named unit when button is pushed.
	 * Retrieves info on all units of selected category.
	 */
	private void search() {
		UnitType category = (UnitType) searchForSelect.getSelectedItem();
		
		String selectedUnitName = selectTextField.getText();
		// If entered text equals the name of a unit in this category, take appropriate action.
		boolean foundUnit = false;
		Unit unit = unitManager.getUnitByName(category, selectedUnitName);
		if (unit != null) {
			foundUnit = true;
			if (openWindowCheck.isSelected()) 
				desktop.showDetails(unit);
			
			if (showMapCheck.isSelected()) {
				MapSelector.displayOnMap(desktop, unit);
			}
			
			if (!history.contains(selectedUnitName))
				history.add(selectedUnitName);
		}

		String tempName = category.getName();

		// If not found, display "'Category' Not Found" in statusLabel.
		if (!foundUnit) statusLabel.setText(Msg.getString("SearchWindow.unitNotFound",tempName)); //$NON-NLS-1$

		// If there is no text entered, display "Enter The Name of a 'Category'" in statusLabel.
		if (selectTextField.getText().length() == 0)
			statusLabel.setText(Msg.getString("SearchWindow.defaultSearch",tempName)); //$NON-NLS-1$
	}
	
	/**
	 * Changes the category of the unit list.
	 * 
	 * @param category
	 */
	private void changeCategory(UnitType category) {
		// Change unitList to the appropriate category list
		lockUnitList = true;
		unitListModel.updateCategory(category);
		unitList.setSelectedIndex(0);
		unitList.ensureIndexIsVisible(0);
		lockUnitList = false;

		// Clear statusLabel.
		statusLabel.setText(" "); //$NON-NLS-1$
	}

	/**
	 * Makes selection in list depending on what unit names.
	 * Begins with the changed text.
	 */
	private void searchTextChange() {
		if (!lockSearchText) {
			String searchText = selectTextField.getText().toLowerCase();
			int fitIndex = 0;
			boolean goodFit = false;
			for (int x = unitListModel.size() - 1; x > -1; x--) {
				Unit unit = unitListModel.elementAt(x);
				String unitString = unit.getName().toLowerCase();
				if (unitString.startsWith(searchText)) {
					fitIndex = x;
					goodFit = true;
				}
			}
			if (goodFit) {
				lockUnitList = true;
				unitList.setSelectedIndex(fitIndex);
				unitList.ensureIndexIsVisible(fitIndex);
				lockUnitList = false;
			}
		}

		// Clear statusLabel
		statusLabel.setText(" "); //$NON-NLS-1$
	}

	@Override
	public void destroy() {
		super.destroy();
		
		if (unitListModel != null) {
			unitListModel.clear();
			unitListModel = null;
		}
	
	}
	private static class UnitTypeRenderer extends BasicComboBoxRenderer {
        @SuppressWarnings("rawtypes")
		@Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof UnitType) {
                setText(((UnitType)value).getName());
            }
            return this;
        }
    }

	/**
	 * Inner class list model for categorized units.
	 */
	private class UnitListModel
	extends DefaultListModel<Unit> {

		private static final long serialVersionUID = 1L;
		// Data members.
		private UnitType category;

		/**
		 * Constructor
		 * @param initialCategory the initial category to display.
		 */
		public UnitListModel(UnitType initialCategory) {

			// Use DefaultListModel constructor.
			super();

			// Initialize data members.
			this.category = initialCategory;

			updateList();
		}

		/**
		 * Updates the category.
		 * @param category the list category
		 */
		private void updateCategory(UnitType category) {
			if (!this.category.equals(category)) {
				this.category = category;

				updateList();
			}
		}

		/**
		 * Updates the list items.
		 */
		private void updateList() {

			clear();

			Collection<? extends Unit> units = null;
			switch(category) {
				case PERSON:
					units = CollectionUtils.sortByName(unitManager.getPeople());
					break;
				case SETTLEMENT:
					units = CollectionUtils.sortByName(unitManager.getSettlements());
					break;
				case VEHICLE:
					units = CollectionUtils.sortByName(unitManager.getVehicles());
					break;
				case ROBOT:
					units = CollectionUtils.sortByName(unitManager.getRobots());
					break;
				case EVA_SUIT:
					units = CollectionUtils.sortByName(unitManager.getEVASuits());
					break;	
				default:
			}
			
			if (units != null && !units.isEmpty()) {
				Iterator<? extends Unit> unitI = units.iterator();
				while (unitI.hasNext()) {
					addElement(unitI.next());
				}
			}
		}
	}
}


