/*
 * Mars Simulation Project
 * NewTransportItemDialog.java
 * @date 2022-07-19
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.tool.resupply;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;

import com.mars_sim.ui.swing.JComboBoxMW;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.MarsPanelBorder;
import com.mars_sim.ui.swing.ModalInternalFrame;


/**
 * A dialog for creating a new transport item.
 */
@SuppressWarnings("serial")
public class NewTransportItemDialog extends ModalInternalFrame {

	/** default logger. */
//	private static SimLogger logger = SimLogger.getLogger(NewTransportItemDialog.class.getName());

	// Transport item types.
	private static final String DEFAULT_MESSAGE = "Select Transport Item Type";
	private static final String RESUPPLY_MISSION = "New Resupply Mission";
	private static final String ARRIVING_SETTLEMENT = "New Arriving Settlement";

	// Data members.
	private TransportItemEditingPanel editingPanel;
	private JPanel mainEditingPanel;
	private CardLayout mainEditingLayout;
	private JPanel emptyPanel;
	private TransportItemEditingPanel resupplyMissionPanel;
	private TransportItemEditingPanel arrivingSettlementPanel;
	private JButton createButton;

	private ResupplyWindow resupplyWindow;
	
	/**
	 * Constructor.
	 * @param MainDesktopPane desktop
	 * @param transportItem the transport item to modify.
	 */
	@SuppressWarnings("unchecked")
	public NewTransportItemDialog(MainDesktopPane desktop, ResupplyWindow resupplyWindow) {

		// Use ModalInternalFrame constructor
        super("New Transport Item");
        this.resupplyWindow = resupplyWindow;

		setSize(580, 600);

		 // Create main panel
		JPanel mainPane = new JPanel(new BorderLayout());
        setContentPane(mainPane);

		// Set the border.
		((JComponent) getContentPane()).setBorder(new MarsPanelBorder());

		// Create transport type panel.
		JPanel transportTypePanel = new JPanel(new FlowLayout(10, 10, FlowLayout.CENTER));
		getContentPane().add(transportTypePanel, BorderLayout.NORTH);

		// Create combo box for determining transport item type.
		JComboBoxMW<String> typeBox = new JComboBoxMW<>();
		typeBox.addItem(DEFAULT_MESSAGE);
		typeBox.addItem(RESUPPLY_MISSION);
		typeBox.addItem(ARRIVING_SETTLEMENT);
		typeBox.setSelectedItem(DEFAULT_MESSAGE);
		typeBox.addActionListener(e -> {
				JComboBox<?> cb = (JComboBox<?>) e.getSource();
				setEditingPanel((String) cb.getSelectedItem());
		});
		transportTypePanel.add(typeBox);

		// Create main editing panel.
		mainEditingLayout = new CardLayout();
		mainEditingPanel = new JPanel(mainEditingLayout);
		getContentPane().add(mainEditingPanel, BorderLayout.CENTER);

		// Create empty default panel.
		emptyPanel = new JPanel();
		emptyPanel.setBorder(new MarsPanelBorder());
		mainEditingPanel.add(emptyPanel, DEFAULT_MESSAGE);

		// Create resupply mission editing panel.
		resupplyMissionPanel = new ResupplyMissionEditingPanel(null, resupplyWindow, null, this); // resupply cannot be null !
		mainEditingPanel.add(resupplyMissionPanel, RESUPPLY_MISSION);

		// Create arriving settlement editing panel.
		arrivingSettlementPanel = new ArrivingSettlementEditingPanel(null, resupplyWindow, null, this); // resupply cannot be null !
		mainEditingPanel.add(arrivingSettlementPanel, ARRIVING_SETTLEMENT);

		// Create the button pane.
		JPanel buttonPane = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
		getContentPane().add(buttonPane, BorderLayout.SOUTH);

		// Create create button.
		createButton = new JButton("Create");
		createButton.addActionListener(e ->
				// Create transport item and close dialog.
				createTransportItem());
		createButton.setEnabled(false);
		buttonPane.add(createButton);

		// Create cancel button.
		JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(e -> 
				// Close dialog.
				dispose()
				//resupplyWindow.setRunning(false);
		);
		buttonPane.add(cancelButton);

        // Add to its own tab pane
       	desktop.add(this);

		Dimension desktopSize = desktop.getParent().getSize();
	    Dimension jInternalFrameSize = this.getSize();
	    int width = (desktopSize.width - jInternalFrameSize.width) / 2;
	    int height = (desktopSize.height - jInternalFrameSize.height) / 2;
	    setLocation(width, height);

	    setModal(true);
	    setVisible(true);
	}

	/**
	 * Sets the editing panel.
	 * 
	 * @param panelKey the panel key string.
	 */
	private void setEditingPanel(String panelKey) {

		if (panelKey != null) {
			mainEditingLayout.show(mainEditingPanel, panelKey);

			if (panelKey.equals(DEFAULT_MESSAGE)) {
				editingPanel = null;
				createButton.setEnabled(false);
			}
			else if (panelKey.equals(RESUPPLY_MISSION)) {
				editingPanel = resupplyMissionPanel;
				createButton.setEnabled(true);
			}
			else if (panelKey.equals(ARRIVING_SETTLEMENT)) {
				editingPanel = arrivingSettlementPanel;
				createButton.setEnabled(true);
			}
		}
	}

	public void setCreateButton(boolean value) {
		createButton.setEnabled(value);
	}

	/**
	 * Creates the new transport item and close the dialog.
	 */
	private void createTransportItem() {
		if ((editingPanel != null) && editingPanel.createTransportItem()) {
			dispose();
		}
	}

	/**
	 * Prepares this window for deletion.
	 */
	public void destroy() {

		editingPanel  = null;
		mainEditingPanel  = null;
		mainEditingLayout  = null;
		emptyPanel  = null;
		resupplyMissionPanel  = null;
		arrivingSettlementPanel  = null;
		createButton  = null;
		resupplyWindow  = null;
	}



}
