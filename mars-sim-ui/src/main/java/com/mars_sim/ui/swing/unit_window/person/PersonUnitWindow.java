/*
 * Mars Simulation Project
 * PersonWindow.java
 * @date 2023-06-04
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.unit_window.person;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.shift.ShiftSlot;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.StyleManager;
import com.mars_sim.ui.swing.unit_display_info.UnitDisplayInfo;
import com.mars_sim.ui.swing.unit_display_info.UnitDisplayInfoFactory;
import com.mars_sim.ui.swing.unit_window.InventoryTabPanel;
import com.mars_sim.ui.swing.unit_window.LocationTabPanel;
import com.mars_sim.ui.swing.unit_window.NotesTabPanel;
import com.mars_sim.ui.swing.unit_window.SponsorTabPanel;
import com.mars_sim.ui.swing.unit_window.UnitWindow;


/**
 * The PersonWindow is the window for displaying a person.
 */
@SuppressWarnings("serial")
public class PersonUnitWindow extends UnitWindow {

	private static final String TOWN = "settlement";
	private static final String JOB = "career";
	private static final String ROLE = "role";
	private static final String SHIFT = "shift";
	
	private static final String TWO_SPACES = "  ";
	private static final String SIX_SPACES = "      ";
	
	/** Is person dead? */
	private boolean deadCache = false;
	
	private String oldRole = "";
	private String oldJob = "";
	private String oldTown = "";
	private String oldShift = "";
	
	private JLabel townLabel;
	private JLabel jobLabel;
	private JLabel roleLabel;
	private JLabel shiftLabel;

	private JPanel statusPanel;
	
	private Person person;

	/**
	 * Constructor.
	 * 
	 * @param desktop the main desktop panel.
	 * @param person  the person for this window.
	 */
	public PersonUnitWindow(MainDesktopPane desktop, Person person) {
		// Use UnitWindow constructor
		super(desktop, person, person.getName() 
				+ " of " + 
				((person.getAssociatedSettlement() != null) ? person.getAssociatedSettlement() : person.getBuriedSettlement())
				+ " (" + (person.getLocationStateType().getName()) + ")"
				, false);
		this.person = person;

		// Create status panel
		statusPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 5));
		statusPanel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
		statusPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		statusPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
		
		getContentPane().add(statusPanel, BorderLayout.NORTH);	
		
		initTopPanel(person);
		
		initTabPanel(person);
		
		statusUpdate();
	}
	
	
	public void initTopPanel(Person person) {
		statusPanel.setPreferredSize(new Dimension(-1, UnitWindow.STATUS_HEIGHT + 5));

		// Create name label
		UnitDisplayInfo displayInfo = UnitDisplayInfoFactory.getUnitDisplayInfo(unit);
		String name = SIX_SPACES + unit.getShortenedName() + SIX_SPACES;

		JLabel nameLabel = new JLabel(name, displayInfo.getButtonIcon(unit), SwingConstants.CENTER);
		nameLabel.setMinimumSize(new Dimension(120, UnitWindow.STATUS_HEIGHT));
		
		JPanel namePane = new JPanel(new BorderLayout(0, 0));
		namePane.setAlignmentX(Component.CENTER_ALIGNMENT);
		namePane.setAlignmentY(Component.CENTER_ALIGNMENT);
		namePane.add(nameLabel, BorderLayout.CENTER);
	
		Font font = StyleManager.getSmallLabelFont();
		nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		nameLabel.setAlignmentY(Component.TOP_ALIGNMENT);
		nameLabel.setFont(font);
		nameLabel.setVerticalTextPosition(JLabel.BOTTOM);
		nameLabel.setHorizontalTextPosition(JLabel.CENTER);

		statusPanel.add(namePane, BorderLayout.WEST);

		JLabel townIconLabel = new JLabel();
		townIconLabel.setToolTipText("Hometown");
		setImage(TOWN, townIconLabel);

		JLabel jobIconLabel = new JLabel();
		jobIconLabel.setToolTipText("Job");
		setImage(JOB, jobIconLabel);

		JLabel roleIconLabel = new JLabel();
		roleIconLabel.setToolTipText("Role");
		setImage(ROLE, roleIconLabel);

		JLabel shiftIconLabel = new JLabel();
		shiftIconLabel.setToolTipText("Work Shift");
		setImage(SHIFT, shiftIconLabel);

		JPanel townPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JPanel jobPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		JPanel shiftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

		townLabel = new JLabel();
		townLabel.setFont(font);

		jobLabel = new JLabel();
		jobLabel.setFont(font);

		roleLabel = new JLabel();
		roleLabel.setFont(font);

		shiftLabel = new JLabel();
		shiftLabel.setFont(font);

		townPanel.add(townIconLabel);
		townPanel.add(townLabel);
		townPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

		jobPanel.add(jobIconLabel);
		jobPanel.add(jobLabel);
		jobPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

		rolePanel.add(roleIconLabel);
		rolePanel.add(roleLabel);
		rolePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

		shiftPanel.add(shiftIconLabel);
		shiftPanel.add(shiftLabel);
		shiftPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

		JPanel gridPanel = new JPanel(new GridLayout(2, 2, 5, 1));		
		gridPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		gridPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
		
		gridPanel.add(townPanel);
		gridPanel.add(rolePanel);
		gridPanel.add(shiftPanel);
		gridPanel.add(jobPanel);

		statusPanel.add(gridPanel, BorderLayout.CENTER);
	}
	
	public void initTabPanel(Person person) {
		// Add tab panels	
		addTabPanel(new TabPanelActivity(person, desktop));
		
		addTabPanel(new TabPanelAttribute(person, desktop));

		addTabPanel(new TabPanelCareer(person, desktop));

		// Add death tab panel if person is dead.
		if (person.isDeclaredDead()
				|| person.getPhysicalCondition().isDead()) {
			deadCache = true;
			addTabPanel(new TabPanelDeath(person, desktop));
		}

		addTabPanel(new TabPanelFavorite(person, desktop));

		addTabPanel(new TabPanelHealth(person, desktop));

		addTabPanel(new InventoryTabPanel(person, desktop));

		addTabPanel(new LocationTabPanel(person, desktop));

		addTabPanel(new NotesTabPanel(person, desktop));
		
		addTabPanel(new TabPanelPersonality(person, desktop));
		
		addTabPanel(new TabPanelSchedule(person, desktop));

		addTabPanel(new TabPanelScienceStudy(person, desktop));

		addTabPanel(new TabPanelSkill(person, desktop));

		addTabPanel(new TabPanelSocial(person, desktop));

		addTabPanel(new SponsorTabPanel(person.getReportingAuthority(), desktop));
		
		addFirstPanel(new TabPanelGeneral(person, desktop));
		
		// Add to tab panels. 
		addTabIconPanels();
	}

	/**
	 * Updates this window.
	 */
	@Override
	public void update() {
		super.update();
		
		String title = person.getName() 
				+ " of " + 
				((person.getAssociatedSettlement() != null) ? person.getAssociatedSettlement() : person.getBuriedSettlement())
				+ " (" + (person.getLocationStateType().getName()) + ")";
		super.setTitle(title);
		
		if (!deadCache 
			&& (person.isDeclaredDead()
			|| person.getPhysicalCondition().isDead())) {
			deadCache = true;
			addTabPanel(new TabPanelDeath(person, desktop));
		}
		
		statusUpdate();
	}

	/*
	 * Updates the status of the person.
	 */
	public void statusUpdate() {

		String townString = null;

		if (person.getPhysicalCondition().isDead()) {
			if (person.getAssociatedSettlement() != null)
				townString = person.getAssociatedSettlement().getName();
			else if (person.getBuriedSettlement() != null)
				townString = person.getBuriedSettlement().getName();
			else if (person.getPhysicalCondition().getDeathDetails().getPlaceOfDeath() != null)
				townString = person.getPhysicalCondition().getDeathDetails().getPlaceOfDeath();
		}

		else if (person.getAssociatedSettlement() != null)
			townString = person.getAssociatedSettlement().getName();

		if (townString != null && !oldTown.equals(townString)) {
			oldJob = townString;
			if (townString.length() > 40)
				townString = townString.substring(0, 40);
			townLabel.setText(TWO_SPACES + townString);
		}

		String jobString = person.getMind().getJob().getName();
		if (!oldJob.equals(jobString)) {
			oldJob = jobString;
			jobLabel.setText(TWO_SPACES + jobString);
		}

		String roleString = person.getRole().getType().getName();
		if (!oldRole.equals(roleString)) {
			oldRole = roleString;
			roleLabel.setText(TWO_SPACES + roleString);
		}

		ShiftSlot shiftSlot = person.getShiftSlot();
		String shiftDesc = TabPanelSchedule.getShiftNote(person.getShiftSlot());
		String newShift = shiftSlot.getStatusDescription();
		if (!oldShift.equalsIgnoreCase(newShift)) {
			oldShift = newShift;
			shiftLabel.setText(TWO_SPACES + newShift);
			shiftLabel.setToolTipText(shiftDesc);
		}
	}
	
	/**
	 * Prepares unit window for deletion.
	 */
	public void destroy() {		
		person = null;
		statusPanel = null;
		townLabel = null;
		jobLabel = null;
		roleLabel = null;
		shiftLabel = null;
	}

}
