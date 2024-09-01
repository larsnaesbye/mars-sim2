/**
 * Mars Simulation Project
 * TradeMissionCustomInfoPanel.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.tool.mission;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;

import com.mars_sim.core.goods.CommerceMission;
import com.mars_sim.core.goods.Good;
import com.mars_sim.core.person.ai.mission.Mission;
import com.mars_sim.core.person.ai.mission.MissionEvent;
import com.mars_sim.core.person.ai.mission.MissionEventType;
import com.mars_sim.core.person.ai.mission.Trade;


/**
 * A panel for displaying trade mission information.
 */
@SuppressWarnings("serial")
public class TradeMissionCustomInfoPanel extends MissionCustomInfoPanel {

	private final static int HEIGHT = 120;
	
	// Data members.
	private Trade mission;
	private GoodsTableModel sellingGoodsTableModel;
	private JLabel desiredGoodsProfitLabel;
	private GoodsTableModel desiredGoodsTableModel;
	private JLabel boughtGoodsProfitLabel;
	private GoodsTableModel boughtGoodsTableModel;

	/**
	 * Constructor.
	 */
	public TradeMissionCustomInfoPanel() {
		// Use JPanel constructor
		super();

		// Set the layout.
		setLayout(new GridLayout(3, 1));

		// Create the selling goods panel.
		JPanel sellingGoodsPane = new JPanel(new BorderLayout());
		add(sellingGoodsPane);

		// Create the selling goods label.
		JLabel sellingGoodsLabel = new JLabel("Goods to Sell:", SwingConstants.LEFT);
		sellingGoodsPane.add(sellingGoodsLabel, BorderLayout.NORTH);

		// Create a scroll pane for the selling goods table.
		JScrollPane sellingGoodsScrollPane = new JScrollPane();
		sellingGoodsScrollPane.setPreferredSize(new Dimension(-1, HEIGHT));
		sellingGoodsPane.add(sellingGoodsScrollPane, BorderLayout.CENTER);

		// Create the selling goods table and model.
		sellingGoodsTableModel = new GoodsTableModel() {
			@Override
			protected Map<Good, Integer> getLoad(CommerceMission commerce) {
				return commerce.getSellLoad();
			}
		};
		JTable sellingGoodsTable = new JTable(sellingGoodsTableModel);
		sellingGoodsScrollPane.setViewportView(sellingGoodsTable);

		// Create the desired goods panel.
		JPanel desiredGoodsPane = new JPanel(new BorderLayout());
		add(desiredGoodsPane);

		// Create the desired goods label panel.
		JPanel desiredGoodsLabelPane = new JPanel(new GridLayout(1, 2, 0, 0));
		desiredGoodsPane.add(desiredGoodsLabelPane, BorderLayout.NORTH);

		// Create the desired goods label.
		JLabel desiredGoodsLabel = new JLabel("Desired Goods to Buy:", SwingConstants.LEFT);
		desiredGoodsLabelPane.add(desiredGoodsLabel);

		// Create the desired goods profit label.
		desiredGoodsProfitLabel = new JLabel("Profit:", SwingConstants.LEFT);
		desiredGoodsLabelPane.add(desiredGoodsProfitLabel);

		// Create a scroll pane for the desired goods table.
		JScrollPane desiredGoodsScrollPane = new JScrollPane();
		desiredGoodsScrollPane.setPreferredSize(new Dimension(-1, HEIGHT));
		desiredGoodsPane.add(desiredGoodsScrollPane, BorderLayout.CENTER);

		// Create the desired goods table and model.
		desiredGoodsTableModel = new GoodsTableModel() {
			@Override
			protected Map<Good, Integer> getLoad(CommerceMission commerce) {
				return commerce.getDesiredBuyLoad();
			}
		};
		JTable desiredGoodsTable = new JTable(desiredGoodsTableModel);
		desiredGoodsScrollPane.setViewportView(desiredGoodsTable);

		// Create the bought goods panel.
		JPanel boughtGoodsPane = new JPanel(new BorderLayout());
		add(boughtGoodsPane);

		// Create the bought goods label panel.
		JPanel boughtGoodsLabelPane = new JPanel(new GridLayout(1, 2, 0, 0));
		boughtGoodsPane.add(boughtGoodsLabelPane, BorderLayout.NORTH);

		// Create the bought goods label.
		JLabel boughtGoodsLabel = new JLabel("Goods Bought:", SwingConstants.LEFT);
		boughtGoodsLabelPane.add(boughtGoodsLabel);

		// Create the bought goods profit label.
		boughtGoodsProfitLabel = new JLabel("Profit:", SwingConstants.LEFT);
		boughtGoodsLabelPane.add(boughtGoodsProfitLabel);

		// Create a scroll pane for the bought goods table.
		JScrollPane boughtGoodsScrollPane = new JScrollPane();
		boughtGoodsScrollPane.setPreferredSize(new Dimension(-1, HEIGHT));
		boughtGoodsPane.add(boughtGoodsScrollPane, BorderLayout.CENTER);

		// Create the bought goods table and model.
		boughtGoodsTableModel = new GoodsTableModel() {
			@Override
			protected Map<Good, Integer> getLoad(CommerceMission commerce) {
				return commerce.getBuyLoad();
			}
		};
		JTable boughtGoodsTable = new JTable(boughtGoodsTableModel);
		boughtGoodsScrollPane.setViewportView(boughtGoodsTable);
	}

	@Override
	public void updateMissionEvent(MissionEvent e) {
		if (e.getType() == MissionEventType.BUY_LOAD_EVENT) {
			boughtGoodsTableModel.updateTable(mission);
			updateBoughtGoodsProfit();
		}
	}

	@Override
	public void updateMission(Mission newMission) {
		if (newMission instanceof Trade) {
			this.mission = (Trade) newMission;
			sellingGoodsTableModel.updateTable(mission);
			desiredGoodsTableModel.updateTable(mission);
			boughtGoodsTableModel.updateTable(mission);
			updateDesiredGoodsProfit();
			updateBoughtGoodsProfit();
		}
	}

	/**
	 * Updates the desired goods profit label.
	 */
	private void updateDesiredGoodsProfit() {
		int profit = (int) mission.getDesiredProfit();
		desiredGoodsProfitLabel.setText("Profit: " + profit + " VP");
	}

	/**
	 * Updates the bought goods profit label.
	 */
	private void updateBoughtGoodsProfit() {
		int profit = (int) mission.getProfit();
		boughtGoodsProfitLabel.setText("Profit: " + profit + " VP");
	}
}
