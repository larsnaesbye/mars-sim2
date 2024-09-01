/*
 * Mars Simulation Project
 * TabPanelCooking.java
 * @date 2023-04-18
 * @author Manny Kung
 */
package com.mars_sim.ui.swing.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import com.mars_sim.core.map.location.Coordinates;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.cooking.Cooking;
import com.mars_sim.core.structure.building.function.cooking.PreparingDessert;
import com.mars_sim.core.structure.building.function.task.CookMeal;
import com.mars_sim.core.time.MarsTime;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.NumberCellRenderer;
import com.mars_sim.ui.swing.StyleManager;
import com.mars_sim.ui.swing.unit_window.TabPanel;
import com.mars_sim.ui.swing.utils.AttributePanel;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multiset;

/**
 * This is a tab panel for displaying a settlement's Food Menu.
 */
@SuppressWarnings("serial")
public class TabPanelCooking extends TabPanel {

	/** default logger. */
	// private static final Logger logger = Logger.getLogger(TabPanelCooking.class.getName());

	private static final String COOKING_ICON = "cooking";
	private static final FunctionType COOKING = FunctionType.COOKING;
	private static final FunctionType PREPARING_DESSERT = FunctionType.PREPARING_DESSERT;

	private int numRow = 0;
	private int dayCache = 1;

	private Set<String> nameSet;
	private List<String> nameList;

	private JTable table;
	private CookingTableModel cookingTableModel;

	/** The number of available meals. */
	private JLabel availableMealsLabel;
	private int availableMealsCache = -1;
	/** The number of meals cooked today. */
	private JLabel mealsTodayLabel;
	private int mealsTodayCache = -1;

	/** The number of available Desserts. */
	private JLabel availableDessertsLabel;
	private int availableDessertsCache = -1;
	/** The number of Desserts cooked today. */
	private JLabel dessertsTodayLabel;
	private int dessertsTodayCache = -1;

	private JLabel mealsReplenishmentLabel;
	private double mealsReplenishmentCache = -1;
	private JLabel dessertsReplenishmentLabel;
	private double dessertsReplenishmentCache = -1;
	
	/** The number of cooks label. */
	private JLabel numCooksLabel;
	private int numCooksCache = -1;

	/** The cook capacity label. */
	private JLabel cookCapacityLabel;
	private int cookCapacityCache = -1;

	private Settlement settlement;

	/**
	 * Constructor.
	 * 
	 * @param unit    the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelCooking(Settlement unit, MainDesktopPane desktop) {

		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelCooking.title"), //$NON-NLS-1$
			ImageLoader.getIconByName(COOKING_ICON),
			Msg.getString("TabPanelCooking.title"), //$NON-NLS-1$
			desktop);

		settlement = unit;
	}

	@Override
	protected void buildUI(JPanel content) {
		
		JPanel northPanel = new JPanel(new BorderLayout());
		content.add(northPanel, BorderLayout.NORTH);
			
		AttributePanel topPanel = new AttributePanel(3, 2);
		northPanel.add(topPanel, BorderLayout.NORTH);

		Coordinates location = settlement.getCoordinates();
		topPanel.addTextField(Msg.getString("TabPanelCooking.breakfastTime"), //$NON-NLS-1$
				CookMeal.getMealTimeString(location, 0), null);
		
		topPanel.addTextField(Msg.getString("TabPanelCooking.lunchTime"), //$NON-NLS-1$
				CookMeal.getMealTimeString(location, 1), null);
		
		topPanel.addTextField(Msg.getString("TabPanelCooking.dinnerTime"), //$NON-NLS-1$
				CookMeal.getMealTimeString(location, 2), null);
		
		topPanel.addTextField(Msg.getString("TabPanelCooking.midnightTime"), //$NON-NLS-1$
				CookMeal.getMealTimeString(location, 3), null);
		
		// Prepare cook number label
		numCooksLabel = topPanel.addTextField(Msg.getString("TabPanelCooking.numberOfCooks"),
											  "", null); //$NON-NLS-1$
		cookCapacityLabel = topPanel.addTextField(Msg.getString("TabPanelCooking.cookCapacity"),
													"", null); //$NON-NLS-1$
		
		// Prepare cooking label panel.
		JPanel splitPanel = new JPanel(new GridLayout(1, 2, 0, 0));
		northPanel.add(splitPanel, BorderLayout.CENTER);

		// Add TitledBorder
		AttributePanel d = new AttributePanel(3);
		d.setBorder(StyleManager.createLabelBorder("Desserts"));
		availableDessertsLabel = d.addTextField(Msg.getString("TabPanelCooking.available"), //$NON-NLS-1$
												"", null);
		dessertsTodayLabel = d.addTextField(Msg.getString("TabPanelCooking.madeToday"), //$NON-NLS-1$
												"", null);
		dessertsReplenishmentLabel = d.addTextField(Msg.getString("TabPanelCooking.replenishment"), //$NON-NLS-1$
												"", null); 
		splitPanel.add(d);

		// Prepare available meals label
		AttributePanel m = new AttributePanel(3);
		m.setBorder(StyleManager.createLabelBorder("Meals"));

		availableMealsLabel = m.addTextField(Msg.getString("TabPanelCooking.available"), //$NON-NLS-1$
												"", null);
		mealsTodayLabel = m.addTextField(Msg.getString("TabPanelCooking.madeToday"), //$NON-NLS-1$
												"",null); 
		mealsReplenishmentLabel = m.addTextField(
				Msg.getString("TabPanelCooking.replenishment"), //$NON-NLS-1$
				"", null); 
		splitPanel.add(m);

		// Create scroll panel for the outer table panel.
		JScrollPane scrollPane = new JScrollPane();
		// increase vertical mousewheel scrolling speed for this one
		scrollPane.getVerticalScrollBar().setUnitIncrement(16);
		content.add(scrollPane, BorderLayout.CENTER);

		// Prepare cooking table model.
		cookingTableModel = new CookingTableModel(settlement);

		// Prepare cooking table.
		table = new JTable(cookingTableModel) {

			public String getToolTipText(java.awt.event.MouseEvent e) {
				String personName = null;
				MarsTime time = null;

				StringBuilder result = new StringBuilder("<html>");

				try {

					result.append("&emsp;&nbsp;Eaten by :&emsp;");
					result.append(personName);
					result.append("&emsp;&nbsp;Time :&emsp;");
					result.append(time);

				} catch (RuntimeException e1) {
					// catch null pointer exception if mouse is over an empty line
				}
				result.append("</html>");
				return result.toString();
			}

		};

		scrollPane.setViewportView(table);
		table.setRowSelectionAllowed(true);
		table.setDefaultRenderer(Double.class, new NumberCellRenderer());
		TableColumnModel columnModel = table.getColumnModel();
		columnModel.getColumn(0).setPreferredWidth(140);
		columnModel.getColumn(1).setPreferredWidth(47);
		columnModel.getColumn(2).setPreferredWidth(45);
		columnModel.getColumn(3).setPreferredWidth(45);
		// Add the two methods below to make all heatTable columns
		// resizable automatically when its Panel resizes
		table.setPreferredScrollableViewportSize(new Dimension(225, -1));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		// Align the preference score to the center of the cell
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setHorizontalAlignment(SwingConstants.CENTER);
		columnModel.getColumn(1).setCellRenderer(renderer);
		columnModel.getColumn(2).setCellRenderer(renderer);
		columnModel.getColumn(3).setCellRenderer(renderer);

		table.setAutoCreateRowSorter(true);

		update();
	}

	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {
		cookingTableModel.update();
		updateMeals();
		updateDesserts();
	}

	private void updateMeals() {
		int numCooks = 0;
		int cookCapacity = 0;
		int availableMeals = 0;
		int mealsToday = 0;
		for(Building b : settlement.getBuildingManager().getBuildingSet(COOKING)) {
			// for each building's kitchen in the settlement
			Cooking kitchen = b.getCooking();
			availableMeals += kitchen.getNumberOfAvailableCookedMeals();
			mealsToday += kitchen.getTotalNumberOfCookedMealsToday();
			cookCapacity += kitchen.getCookCapacity();
			numCooks += kitchen.getNumCooks();
		}

		double mealsReplenishment = Math.round(settlement.getMealsReplenishmentRate() * 100.0) / 100.0;

		// Update # of meals replenishment rate
		if (mealsReplenishmentCache != mealsReplenishment) {
			mealsReplenishmentCache = mealsReplenishment;
			mealsReplenishmentLabel.setText(
							StyleManager.DECIMAL_PLACES1.format(mealsReplenishmentCache)); 
		}

		// Update # of available meals
		if (availableMealsCache != availableMeals) {
			availableMealsCache = availableMeals;
			availableMealsLabel.setText(Integer.toString(availableMealsCache)); //$NON-NLS-1$
		}

		// Update # of meals cooked today
		if (mealsTodayCache != mealsToday) {
			mealsTodayCache = mealsToday;
			mealsTodayLabel.setText(Integer.toString(mealsTodayCache)); //$NON-NLS-1$
		}

		// Update cook number
		if (numCooksCache != numCooks) {
			numCooksCache = numCooks;
			numCooksLabel.setText(Integer.toString(numCooksCache)); //$NON-NLS-1$
		}

		// Update cook capacity
		if (cookCapacityCache != cookCapacity) {
			cookCapacityCache = cookCapacity;
			cookCapacityLabel.setText(Integer.toString(cookCapacityCache)); //$NON-NLS-1$
		}
	}

	private void updateDesserts() {

		int availableDesserts = 0;
		int dessertsToday = 0;
		for(Building b : settlement.getBuildingManager().getBuildingSet(PREPARING_DESSERT)) {
			PreparingDessert kitchen = b.getPreparingDessert();
			availableDesserts += kitchen.getAvailableServingsDesserts();
			dessertsToday += kitchen.getTotalServingsOfDessertsToday();
		}
		double dessertsReplenishment = settlement.getDessertsReplenishmentRate();

		// Update # of available Desserts
		if (availableDessertsCache != availableDesserts) {
			availableDessertsCache = availableDesserts;
			availableDessertsLabel.setText(Integer.toString(availableDesserts)); //$NON-NLS-1$
		}

		// Update # of Desserts cooked today
		if (dessertsTodayCache != dessertsToday) {
			dessertsTodayCache = dessertsToday;
			dessertsTodayLabel.setText(Integer.toString(dessertsToday)); //$NON-NLS-1$
		}

		dessertsReplenishment = Math.round(dessertsReplenishment * 100.0) / 100.0;

		// Update # of desserts replenishment rate
		if (dessertsReplenishmentCache != dessertsReplenishment) {
			dessertsReplenishmentCache = dessertsReplenishment;
			dessertsReplenishmentLabel.setText(
					StyleManager.DECIMAL_PLACES1.format(dessertsReplenishment));
		}
	}

	/**
	 * Internal class used as model for the cooking table.
	 */
	private class CookingTableModel extends AbstractTableModel {

		private Settlement settlement;

		private Multiset<String> allServingsSet;

		private Multimap<String, Double> qualityMap;
		private Multimap<String, Double> allQualityMap;

		private Multimap<? extends String, ? extends MarsTime> timeMap;
		private Multimap<String, MarsTime> allTimeMap;

		private Collection<Map.Entry<String, Double>> allQualityMapE;
		private Collection<Entry<String, MarsTime>> allTimeMapE;


		private String[] columnNames = { "Meal", "# Servings",
				"Best", "Worst" };
		
		private CookingTableModel(Settlement settlement) {
			this.settlement = settlement;

			allServingsSet = HashMultiset.create();
			allQualityMap = ArrayListMultimap.create();
			allTimeMap = ArrayListMultimap.create();
		}

		public int getRowCount() {
			return numRow;

		}

		public int getColumnCount() {
			return 4;
		}

		public Class<?> getColumnClass(int columnIndex) {
			Class<?> dataType = super.getColumnClass(columnIndex);
			if (columnIndex == 0)
				dataType = String.class;
			else if (columnIndex == 1)
				dataType = Double.class;
			else if (columnIndex == 2)
				dataType = Double.class;
			else if (columnIndex == 3)
				dataType = Double.class;
			return dataType;
		}

		public String getColumnName(int columnIndex) {
			if (columnIndex == 0)
				return columnNames[0];
			if (columnIndex == 1)
				return columnNames[1];
			if (columnIndex == 2)
				return columnNames[2];
			if (columnIndex == 3)
				return columnNames[3];

			return null;
		}

		/***
		 * Converts a numeral quality to letter grade for a meal
		 * 
		 * @param quality
		 * @return grade
		 */
		public String computeGrade(double quality) {
			String grade = "";

			if (quality < -4)
				grade = "C-";
			else if (quality < -3)
				grade = "C";
			else if (quality < -2)
				grade = "C+";
			else if (quality < -1)
				grade = "B-";
			else if (quality < 0)
				grade = "B";
			else if (quality < 1)
				grade = "B+";
			else if (quality < 2)
				grade = "A-";
			else if (quality < 3)
				grade = "A";
			else
				grade = "A+";

			return grade;
		}

		public Object getValueAt(int row, int column) {
			if (nameList.isEmpty())
				return null;
			
			Object result = null;
			
			String name = nameList.get(row);

			if (column == 0)
				result = name;

			else if (column == 1) {
				// Use Multimap.get(key) returns a view of the values 
				// associated with the specified key
				int numServings = allServingsSet.count(name);
				result = numServings;
				// allServingsSet.clear();
			} else if (column == 2) {
				double best = 0;
				double value = 0;
				for (Map.Entry<String, Double> entry : allQualityMapE) {
					String key = entry.getKey();
					if (name.equals(key)) {
						value = entry.getValue();
						if (value > best)
							best = value;
					}
					result = computeGrade(best);
					// allQualityMap.clear();
				}
			} else if (column == 3) {
				double worst = 10;
				double value = 0;
				for (Map.Entry<String, Double> entry : allQualityMapE) {
					String key = entry.getKey();
					if (name.equals(key)) {
						value = entry.getValue();

						if (value < worst)
							worst = value;
					}
					result = computeGrade(worst);
					// allTimeMap.clear();
				}
			} else
				result = null;
			return result;
		}

		// TODO: decide in what situation it needs update and at what time ?
		// update every second or after each meal or once a day ?
		public void update() {
			cleanUpTable();
			getMultimap();
			fireTableDataChanged();
		}

		public void getMultimap() {

			Iterator<Building> i = settlement.getBuildingManager().getBuildings(COOKING).iterator();

			while (i.hasNext()) { 
				// for each building's kitchen in the settlement
				Building building = i.next();
				Cooking kitchen = building.getCooking();

				qualityMap = kitchen.getQualityMap();
				timeMap = kitchen.getTimeMap();

				allQualityMap.putAll(qualityMap);
				allTimeMap.putAll(timeMap);
			}

			allQualityMapE = allQualityMap.entries();
			allTimeMapE = allTimeMap.entries();
			allServingsSet = allQualityMap.keys();

			numRow = allTimeMap.keySet().size();
			nameSet = allTimeMap.keySet();
			// nameSet = servingsSet.elementSet(); // or using servingsSet
			nameList = new ArrayList<>(nameSet);
		}

		/**
		 * Removes all entries on all maps at the beginning of each new sol.
		 */
		public void cleanUpTable() {
			// 1. find any expired meals
			// 2. remove any expired meals from all 3 maps
			// 3. call cookingTableModel.update()

			int currentDay = getDesktop().getSimulation().getMasterClock().getMarsTime().getSolOfMonth();

			if (dayCache != currentDay) {
				if (!allTimeMap.isEmpty())
					allTimeMap.clear();
				if (allTimeMapE != null)
					allTimeMapE.clear();

				if (!allQualityMap.isEmpty())
					allQualityMap.clear();
				if (allQualityMapE != null)
					allQualityMapE.clear();

				if (!allServingsSet.isEmpty())
					allServingsSet.clear();

				dayCache = currentDay;
			}
		}
	}
}
