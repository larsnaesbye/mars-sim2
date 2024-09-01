/*
 * Mars Simulation Project
 * TabPanelPersonality.java
 * @date 2022-07-13
 * @author Manny Kung
 */
package com.mars_sim.ui.swing.unit_window.person;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

import javax.swing.JPanel;
import javax.swing.SpringLayout;

import com.mars_sim.core.Unit;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.MBTIPersonality;
import com.mars_sim.core.person.ai.PersonalityTraitManager;
import com.mars_sim.core.person.ai.PersonalityTraitType;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.unit_window.TabPanel;
import com.mars_sim.ui.swing.utils.AttributePanel;

/**
 * The TabPanelPersonality is a tab panel for personality information about a person.
 */
@SuppressWarnings("serial")
public class TabPanelPersonality extends TabPanel {

	private static final String PER_ICON = "personality"; //$NON-NLS-1$

	/** The Person instance. */
	private Person person;

	/**
	 * Constructor.
	 * 
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelPersonality(Unit unit, MainDesktopPane desktop) {
		
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelPersonality.title"), //$NON-NLS-1$
			ImageLoader.getIconByName(PER_ICON),		
			Msg.getString("TabPanelPersonality.title"), //$NON-NLS-1$
			unit, desktop
		);

		person = (Person) unit;
	}
	
	@Override
	protected void buildUI(JPanel content) {

		// Prepare spring layout info panel.
		JPanel infoPanel = new JPanel(new SpringLayout());
		content.add(infoPanel, BorderLayout.NORTH);

		JPanel scorePanel = new JPanel(new BorderLayout(1, 1));
		content.add(scorePanel, BorderLayout.CENTER);
		
		// Create the text area for displaying the Big Five scores
		scorePanel.add(createBigFive(), BorderLayout.NORTH);
		// Create the text area for displaying the MBTI scores
		scorePanel.add(createMBTI(person.getMind().getMBTI()), BorderLayout.CENTER);
	}
	
	
	/**
	 * Creates the MBTI text area.
	 * 
	 * @param p an instance of MBTIPersonality
	 * @param content
	 * @return
	 */
	private JPanel createMBTI(MBTIPersonality p) {
		
		int ie = p.getIntrovertExtrovertScore();
		int ns = p.getScores().get(1);
		int ft = p.getScores().get(2);
		int jp = p.getScores().get(3);
		
		String[] types = new String[4];
		int[] scores = new int[4];

		// Prepare attribute panel.
		AttributePanel attributePanel = new AttributePanel(5);
		
		JPanel listPanel = new JPanel(new FlowLayout(FlowLayout.CENTER)); // BorderLayout(1, 1));
		listPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		listPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
		listPanel.add(attributePanel, BorderLayout.CENTER);
		
		addBorder(listPanel, Msg.getString("TabPanelPersonality.mbti.title"));
		
		if (ie < 0) {
			types[0] = "Introvert (I)";
			scores[0] = ie;
		}
		else {
			types[0] = "Extrovert (E)";
			scores[0] = ie;
		}
		
		if (ns < 0) {
			types[1] = "Intuitive (N)";
			scores[1] = ns;
		}
		else {
			types[1] = "Sensing (S)";
			scores[1] = ns;
		}
		
		if (ft < 0) {
			types[2] = "Feeling (F)";
			scores[2] = ft;
		}
		else {
			types[2] = "Thinking (T)";
			scores[2] = ft;
		}
		
		if (jp < 0) {
			types[3] = "Judging (J)";
			scores[3] = jp;
		}
		else {
			types[3] = "Perceiving (P)";
			scores[3] = jp;
		}
		
		String tip =  "<html>Introvert (I) / Extrovert  (E) : -50 to 0 / 0 to 50" 
				  + "<br>Intuitive (N) / Sensing    (S) : -50 to 0 / 0 to 50"
				  + "<br>  Feeling (F) / Thinking   (T) : -50 to 0 / 0 to 50"
				  + "<br>  Judging (J) / Perceiving (P) : -50 to 0 / 0 to 50</html>";
		
		for (int i = 0; i < 4; i++) {
			attributePanel.addTextField(types[i], Math.round(scores[i] * 10.0)/10.0 + " %", tip);
		}
		
		
		String type = p.getTypeString();
		String descriptor = p.getDescriptor();
		
		attributePanel.addTextField(type, descriptor, Msg.getString("TabPanelPersonality.mbti.descriptor.tip"));
		
		return listPanel;
	}

	/**
	 * Creates the text area for the Big Five.
	 * 
	 * @param content
	 * @return
	 */
	private JPanel createBigFive() {
		PersonalityTraitManager p = person.getMind().getTraitManager();
		
		String[] types = new String[5];
		int[] scores = new int[5];
		
    	for (PersonalityTraitType t : PersonalityTraitType.values()) {
    		types[t.ordinal()] = t.getName();
    		scores[t.ordinal()] = p.getPersonalityTrait(t);
    	}

		// Prepare attribute panel.
		AttributePanel attributePanel = new AttributePanel(5);
    	
		JPanel listPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		listPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
		listPanel.setToolTipText(Msg.getString("TabPanelPersonality.bigFive.tip"));//$NON-NLS-1$
		listPanel.add(attributePanel, BorderLayout.CENTER);
		
		addBorder(listPanel, Msg.getString("TabPanelPersonality.bigFive.title"));
		
		String tip =  "<html>          Openness : 0 to 100" 
			  	  + "<br> Conscientiousness : 0 to 100"
			  	  + "<br>      Extraversion : 0 to 100"
			  	  + "<br>       Neuroticism : 0 to 100</html>";

		for (int i = 0; i < 5; i++) {
			attributePanel.addTextField(types[i], Math.round(scores[i] * 10.0)/10.0 + " %", tip);
		}
		
		return listPanel;
	}
	
	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {
	}
}
