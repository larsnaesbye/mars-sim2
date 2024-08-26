/*
 * Mars Simulation Project
 * AttributePanel.java
 * @date 2023-08-27
 * @author Barry Evans
 */
package com.mars_sim.ui.swing.utils;

import java.awt.FlowLayout;
import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import com.mars_sim.ui.swing.StyleManager;

/**
 * A simple panel implementation that supports adding JComponents accompanied by a JLabel.
 * These are added in a row layout.
 */
@SuppressWarnings("serial")
public class AttributePanel extends JPanel {

    public AttributePanel(int rows) {
        this(rows, 1);
    }

    public AttributePanel(int rows, int cols) {
        super(new GridLayout(rows, 2*cols, 3, 3));
    }

    public AttributePanel(int rows, int cols, int hgap, int vgap) {
        super(new GridLayout(rows, 2*cols, hgap, vgap));
    }
    
	/**
	 * Adds a field with labels to a panel. The layout should be a spring layout.
	 * 
	 * @param titleLabel 	The fixed label
	 * @param content 		The first content of the field
	 * @param content1 		The second content of the field
	 * @return The JLabel that can be updated
	 */
	public JLabel addLabels(String titleLabel, String content, String unicode) {
		JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEADING, 5, 5));
		JLabel contentLabel = new JLabel();
		if (unicode.equals("")) {
			contentLabel.setText(content);
			wrapper.add(contentLabel);
		}
		else {
			contentLabel.setText("<html>" + content + " (" + String.valueOf(unicode) + ") </html>");
//			contentLabel.setText(content + " (" + String.valueOf(unicode) + ")");
			wrapper.add(contentLabel);
		}
		addLabelledItem(titleLabel, contentLabel);
		return contentLabel;
	}
	
	/**
	 * Adds a text field and label to a Panel. The layout should be Spring layout.
	 * 
	 * @param titleLabel The fixed label
	 * @param content Initial content of the text field
	 * @param tooltip Optional tooltip
	 * @return The JLabel that can be updated.
	 */
	public JLabel addTextField(String titleLabel, String content, String tooltip) {
		JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		JLabel contentLabel = new JLabel(content);
		if (tooltip != null) {
			contentLabel.setToolTipText(tooltip);
		}
		wrapper.add(contentLabel);
		addLabelledItem(titleLabel, contentLabel);
		return contentLabel;
	}
	
	/**
	 * Adds a field and label to a Panel. The layout should be Spring layout.
	 * 
	 * @param titleLabel The fixed label
	 * @param content Initial content of the text field
	 * @tooltip
	 * @return The JLabel that can be updated.
	 */
	public JLabel addRow(String titleLabel, String content, String tooltip) {
		return addTextField(titleLabel, content, tooltip);
	}
	
	/**
	 * Adds a field and label to a Panel. The layout should be Spring layout.
	 * 
	 * @param titleLabel The fixed label
	 * @param content Initial content of the text field
	 * @return The JLabel that can be updated.
	 */
	public JLabel addRow(String titleLabel, String content) {
		return addTextField(titleLabel, content, null);
	}
	
	/**
	 * Adds a labelled content to the TabPanel. This ensures the styling is common.
	 * 
	 * @param titleLabel Label to add
	 * @param content Content showing the value
	 */
	public void addLabelledItem(String titleLabel, JComponent content) {
        if ((titleLabel != null) && !titleLabel.endsWith(": ")) {
            titleLabel = titleLabel + " :";
        }
        JLabel title = new JLabel(titleLabel, SwingConstants.RIGHT);
        title.setFont(StyleManager.getLabelFont());
		add(title);
		add(content);
	}
}
