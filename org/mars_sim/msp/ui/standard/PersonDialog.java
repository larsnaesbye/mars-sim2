/**
 * Mars Simulation Project
 * PersonDialog.java
 * @version 2.72 2001-07-08
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.standard;  
 
import org.mars_sim.msp.simulation.*; 
import org.mars_sim.msp.simulation.task.*; 
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

/** The PersonDialog class is a detail window for a person.
 *  It displays information about the person and the person's current status.
 */
public class PersonDialog extends UnitDialog {

	// Data members
	private Person person;          // Person detail window is about
	private JButton locationButton; // Location button
	private JLabel latitudeLabel;   // Latitude label
	private JLabel longitudeLabel;  // Longitude label
	private JPanel skillListPane;   // Panel containing list of person's skills and their levels.
	private JLabel taskDescription; // Current task description label
	private JLabel taskPhase;       // Current task phase label

	// Cached person data
	private Coordinates unitCoords;
	private String settlementName;
	private String vehicleName;
	private Hashtable skillList;

	/** Constructs a PersonDialog object 
     	 *  @param parentDesktop the desktop pane
     	 *  @param personUIProxy the person's UI proxy
     	 */
	public PersonDialog(MainDesktopPane parentDesktop, PersonUIProxy personUIProxy) {

		// Use UnitDialog constructor
		super(parentDesktop, personUIProxy);
	}

	/** Initialize cached data members */
	protected void initCachedData() {
		
		unitCoords = new Coordinates(0D, 0D);
		settlementName = "";
		vehicleName = "";
		skillList = new Hashtable();
	}
	
	/** Complete update (overridden) */
	protected void generalUpdate() {
		updatePosition();
		updateSettlement();
		updateVehicle();
		updateSkills();
		updateTask();
	}

	/** Update position */
	private void updatePosition() {
		if (!unitCoords.equals(person.getCoordinates())) {
			unitCoords = new Coordinates(person.getCoordinates());
			latitudeLabel.setText("Latitude: " + unitCoords.getFormattedLatitudeString());
			longitudeLabel.setText("Longitude: " + unitCoords.getFormattedLongitudeString());
		}
	}

	/** Update settlement */
	private void updateSettlement() { 
		Settlement tempSettlement = person.getSettlement();
		if (tempSettlement != null) {
			if (!settlementName.equals(tempSettlement.getName())) {
				settlementName = tempSettlement.getName();
				locationButton.setText(settlementName); 
			}
		}
	}

	/** Update vehicle */
	private void updateVehicle() { 
		Vehicle tempVehicle = person.getVehicle();
		if (tempVehicle != null) {
			if (!vehicleName.equals(tempVehicle.getName())) {
				vehicleName = tempVehicle.getName();
				locationButton.setText(vehicleName); 
			}
		}
	}
	
	/** Update skill list */
	private void updateSkills() {
		
		boolean change = false;
		SkillManager skillManager = person.getSkillManager();

		String[] keyNames = skillManager.getKeys();
		for (int x=0; x < keyNames.length; x++) {
			int skillLevel = skillManager.getSkillLevel(keyNames[x]);
			if (skillLevel > 0) {
				if (skillList.containsKey(keyNames[x])) {
					int cacheSkillLevel = ((Integer) skillList.get(keyNames[x])).intValue();
					if (skillLevel != cacheSkillLevel) {
						skillList.put(keyNames[x], new Integer(skillLevel));
						change = true; 
					}
				}
				else {
					skillList.put(keyNames[x], new Integer(skillLevel));
					change = true;
				}
			}
		}
			
		if (change) {
			skillListPane.removeAll();
			skillListPane.setLayout(new GridLayout(skillList.size(), 2));

			for (int x=0; x < keyNames.length; x++) {
				int skillLevel = skillManager.getSkillLevel(keyNames[x]);
				if (skillLevel > 0) {
				
					// Display skill name
					JLabel skillName = new JLabel(keyNames[x] + ":", JLabel.LEFT);
					skillName.setForeground(Color.black);
					skillName.setVerticalAlignment(JLabel.TOP);
					skillListPane.add(skillName);
				
					// Display skill value
					JLabel skillValue = new JLabel("" + skillLevel, JLabel.RIGHT);
					skillValue.setForeground(Color.black);
					skillValue.setVerticalAlignment(JLabel.TOP);
					skillListPane.add(skillValue);
				}
			}
			validate();
		}
	}
	
	/** Update task info */
	private void updateTask() {
		
		TaskManager taskManager = person.getTaskManager();
	
		// Update task description
		String cacheDescription = "None";
		if (taskManager.hasCurrentTask()) cacheDescription = taskManager.getCurrentTaskDescription();
		if (!cacheDescription.equals(taskDescription.getText())) taskDescription.setText(cacheDescription);
		
		// Update task phase
		String cachePhase = "";
		if (taskManager.hasCurrentTask()) {
			String phase = taskManager.getCurrentPhase();
			if ((phase != null) && !phase.equals("")) cachePhase = "Phase: " + phase;
		}
		if (!cachePhase.equals(taskPhase.getText())) taskPhase.setText(cachePhase);
	}
	
	/** ActionListener method overriden */
	public void actionPerformed(ActionEvent event) {
		super.actionPerformed(event);
		
		Object button = event.getSource();
			
		// If location button, open window for selected unit
		if (button == locationButton) {
			if (person.getSettlement() != null) 
                parentDesktop.openUnitWindow(proxyManager.getUnitUIProxy(person.getSettlement()));
			else if (person.getVehicle() != null) 
                parentDesktop.openUnitWindow(proxyManager.getUnitUIProxy(person.getVehicle()));
		}
	}
	
	/** Set window size 
     *  @return the window's size
     */
	protected Dimension setWindowSize() { return new Dimension(300, 345); }
	
	/** Prepare components */
	protected void setupComponents() {
		
		super.setupComponents();
		
		// Initialize person
		person = (Person) parentUnit;

		// Prepare tab pane
		JTabbedPane tabPane = new JTabbedPane();
		tabPane.addTab("Task", setupTaskPane());
		tabPane.addTab("Location", setupLocationPane());
		tabPane.addTab("Attributes", setupAttributePane());
		tabPane.addTab("Skills", setupSkillPane());
		mainPane.add(tabPane, "Center");
	}
	
	/** Set up task panel 
     *  @return the task pane
     */
	protected JPanel setupTaskPane() {
		
		// Prepare Task pane
		JPanel taskPane = new JPanel(new BorderLayout());
		taskPane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
		
		// Prepare task label pane
		JPanel taskLabelPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		taskPane.add(taskLabelPane, "North");
		
		// Prepare task label
		JLabel taskLabel = new JLabel("Current Task", JLabel.CENTER);
		taskLabel.setForeground(Color.black);
		taskLabelPane.add(taskLabel);
		
		// Use person's task manager.
		TaskManager taskManager = person.getTaskManager();
		
		// Prepare task description pane
		JPanel taskDescriptionPane = new JPanel(new GridLayout(3, 1));
		JPanel taskDescriptionTopPane = new JPanel(new BorderLayout());
		taskDescriptionTopPane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
		taskDescriptionTopPane.add(taskDescriptionPane, "North");
		taskPane.add(new JScrollPane(taskDescriptionTopPane), "Center");
		
		// Display description of current task.
		// Display 'None' if person is currently doing nothing.
		taskDescription = new JLabel("None", JLabel.LEFT);
		if (taskManager.hasCurrentTask()) taskDescription.setText(taskManager.getCurrentTaskDescription());
		taskDescription.setForeground(Color.black);
		taskDescriptionPane.add(taskDescription);
		
		// Display name of current phase.
		// Display nothing if current task has no current phase.
		// Display nothing if person is currently doing nothing.
		taskPhase = new JLabel("", JLabel.LEFT);
		if (taskManager.hasCurrentTask()) {
			String phase = taskManager.getCurrentPhase();
			if ((phase != null) && !phase.equals("")) taskPhase.setText("Phase: " + phase);
		}
		taskPhase.setForeground(Color.black);
		taskDescriptionPane.add(taskPhase);
		
		// Return skill panel
		return taskPane;
	}
	
	/** Set up location panel 
     *  @return location pane
     */
	protected JPanel setupLocationPane() {
		
		// Prepare location pane
		JPanel locationPane = new JPanel(new BorderLayout());
		locationPane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));

		// Prepare location sub pane
		JPanel locationSubPane = new JPanel(new GridLayout(2, 1));
		locationPane.add(locationSubPane, "North");

		// Prepare location label pane
		JPanel locationLabelPane = new JPanel(new FlowLayout(FlowLayout.LEFT));
		locationSubPane.add(locationLabelPane);

		// Prepare center map button
		centerMapButton = new JButton(new ImageIcon("images/CenterMap.gif"));
		centerMapButton.setMargin(new Insets(1, 1, 1, 1));
		centerMapButton.addActionListener(this);
		locationLabelPane.add(centerMapButton);

		// Prepare location label
		JLabel locationLabel = new JLabel("Location: ", JLabel.LEFT);
		locationLabel.setForeground(Color.black);
		locationLabelPane.add(locationLabel);

		// Prepare location button
		locationButton = new JButton();
		locationButton.setMargin(new Insets(1, 1, 1, 1));
		if (person.getSettlement() != null) locationButton.setText(person.getSettlement().getName());
		else if (person.getVehicle() != null) locationButton.setText(person.getVehicle().getName());
		locationButton.addActionListener(this);
		locationLabelPane.add(locationButton);

		// Prepare location coordinates pane
		JPanel locationCoordsPane = new JPanel(new GridLayout(1, 2,  0, 0));
		locationSubPane.add(locationCoordsPane);

		// Prepare latitude label
		latitudeLabel = new JLabel("Latitude: ", JLabel.LEFT);
		latitudeLabel.setForeground(Color.black);
		locationCoordsPane.add(latitudeLabel);

		// Prepare longitude label
		longitudeLabel = new JLabel("Longitude: ", JLabel.LEFT);
		longitudeLabel.setForeground(Color.black);
		locationCoordsPane.add(longitudeLabel);
		
		// Return location panel
		return locationPane;
	}
	
	/** Set up attribute panel 
     *  @return attribute pane
     */
	protected JPanel setupAttributePane() {
	
		// Prepare attribute pane
		JPanel attributePane = new JPanel(new BorderLayout());
		attributePane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
		
		// Prepare attribute label pane
		JPanel attributeLabelPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		attributePane.add(attributeLabelPane, "North");
		
		// Prepare attribute label
		JLabel attributeLabel = new JLabel("Natural Attributes", JLabel.CENTER);
		attributeLabel.setForeground(Color.black);
		attributeLabelPane.add(attributeLabel);
		
		// Use person's natural attribute manager.
		NaturalAttributeManager attributeManager = person.getNaturalAttributeManager();
		
		// Prepare attribute list pane
		JPanel attributeListPane = new JPanel(new GridLayout(attributeManager.getAttributeNum(), 2));
		attributeListPane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
		attributePane.add(new JScrollPane(attributeListPane), "Center");
		
		// For each natural attribute, display the name and its value.
		String[] keyNames = attributeManager.getKeys();
		for (int x=0; x < keyNames.length; x++) {
			
			// Display attribute name
			JLabel attributeName = new JLabel(keyNames[x] + ":", JLabel.LEFT);
			attributeName.setForeground(Color.black);
			attributeListPane.add(attributeName);
	
			// Display attribute value
			JLabel attributeValue = new JLabel("" + attributeManager.getAttribute(keyNames[x]), JLabel.RIGHT);
			attributeValue.setForeground(Color.black);
			attributeListPane.add(attributeValue);
		}
	
		// Return attribute panel
		return attributePane;
	}
	
	/** Set up skill panel 
     *  @return the skill pane
     */
	protected JPanel setupSkillPane() {
	
		// Prepare skill pane
		JPanel skillPane = new JPanel(new BorderLayout());
		skillPane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
		
		// Prepare skill label pane
		JPanel skillLabelPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		skillPane.add(skillLabelPane, "North");
		
		// Prepare skill label
		JLabel skillLabel = new JLabel("Skills", JLabel.CENTER);
		skillLabel.setForeground(Color.black);
		skillLabelPane.add(skillLabel);
		
		// Populate skill list
		SkillManager skillManager = person.getSkillManager();
		String[] keyNames = skillManager.getKeys();
		skillList = new Hashtable();
		for (int x=0; x < keyNames.length; x++) {
			int skillLevel = skillManager.getSkillLevel(keyNames[x]);
			if (skillLevel > 0) skillList.put(keyNames[x], new Integer(skillLevel));
		}
		
		// Prepare skill list pane
		JPanel skillListTopPane = new JPanel(new BorderLayout());
		skillListTopPane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
		skillPane.add(new JScrollPane(skillListTopPane), "Center");
		skillListPane = new JPanel(new GridLayout(skillList.size(), 2));
		skillListTopPane.add(skillListPane, "North");
		
		// For each skill, display the name and its value.
		for (int x=0; x < keyNames.length; x++) {
			if (skillList.containsKey(keyNames[x])) {

				// Display skill name
				JLabel skillName = new JLabel(keyNames[x] + ":", JLabel.LEFT);
				skillName.setForeground(Color.black);
				skillName.setVerticalAlignment(JLabel.TOP);
				skillListPane.add(skillName);

				// Display skill value
				JLabel skillValue = new JLabel("" + skillList.get(keyNames[x]), JLabel.RIGHT);
				skillValue.setForeground(Color.black);
				skillValue.setVerticalAlignment(JLabel.TOP);
				skillListPane.add(skillValue);
			}
		}
	
		// Return skill panel
		return skillPane;
	}
}
