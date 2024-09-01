/*
 * Mars Simulation Project
 * NotesTabPanel.java
 * @date 2022-07-09
 * @author Manny Kung
 */
package com.mars_sim.ui.swing.unit_window;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

import com.mars_sim.core.Unit;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.MarsPanelBorder;


/**
 * The NotesTabPanel is a tab panel for recording commander's notes regarding this unit
 */
@SuppressWarnings("serial")
public class NotesTabPanel extends TabPanel{

	private static final String NOTE_ICON = "note";
	
	private static final String ENTER_HERE = "Enter Here";

	/** The cache for notes. */
	private String notesCache = "";
		
	/** The text area for holding the notes. */
	private JTextArea textArea;
		
	/**
	 * Constructor.
	 * 
	 * @param unit    the unit to display.
	 * @param desktop the main desktop.
	 */
	public NotesTabPanel(Unit unit, MainDesktopPane desktop) {
		super(null, ImageLoader.getIconByName(NOTE_ICON),
				Msg.getString("NotesTabPanel.title"), unit, desktop);
	}

	@Override
	protected void buildUI(JPanel content) {

		// Create notes panel
		JPanel notesPanel = new JPanel(new BorderLayout(5, 5));
		notesPanel.setBorder(new MarsPanelBorder());
		notesPanel.setBorder(new EmptyBorder(1, 1, 1, 1));
		content.add(notesPanel);
		
		notesCache = getUnit().getNotes();
		
		textArea = new JTextArea();
		notesPanel.add(textArea);
		
		if (notesCache == null || notesCache.equals(""))
			textArea.append(ENTER_HERE);
		else {
			textArea.append(notesCache);
		}
	}
	
	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {
		String notes = textArea.getText();
		Unit unit = getUnit();
		if (notes == null || notes.equals("")) {
			notesCache = "";
			unit.setNotes(notes);
		}
		else if (!notesCache.equals(notes)) {
			notesCache = notes;
			unit.setNotes(notes);
		}
	}
}
