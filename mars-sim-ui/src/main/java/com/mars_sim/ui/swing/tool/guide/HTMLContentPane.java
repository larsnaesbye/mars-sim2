/*
 * Mars Simulation Project
 * HTMLContentPane.java
 * @date 2021-08-20
 * @author Lars Naesbye Christensen
 */
package com.mars_sim.ui.swing.tool.guide;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JEditorPane;
import javax.swing.SwingUtilities;

import com.mars_sim.core.tool.Msg;

/**
 * The HTMLContentPane class provides an HTML pane for displaying the help files
 * in the Guide window.
 */
@SuppressWarnings("serial")
public class HTMLContentPane extends JEditorPane {

	/** default logger. */
	private static final Logger logger = Logger.getLogger(HTMLContentPane.class.getName());

	private List<URL> history = new ArrayList<URL>();

	private int historyIndex;

	public HTMLContentPane() {

		setEditable(false);
		
//		HTMLEditorKit kit = new HTMLEditorKit();
//		StyleSheet styleSheet = kit.getStyleSheet();
////		styleSheet.addRule("A:active {orange}");
//		this.setEditorKit(kit);
		
//		setContentType("text/html");
//		setEditorKit(JEditorPane.createEditorKitForContentType("text/html"));
	}

	public void goToURL(URL url) {

		displayPage(url);
		if (historyIndex < history.size() - 1) {
			historyIndex++;
			history.set(historyIndex, url);
			while (historyIndex < history.size() - 1) {
				history.remove(historyIndex + 1);
			}
		} else {
			history.add(url);
			historyIndex = history.size() - 1;
		}

	}

	public URL forward() {
		historyIndex++;
		if (historyIndex >= history.size()) {
			historyIndex = history.size() - 1;
		}
		URL url = history.get(historyIndex);
		displayPage(url);

		return url;
	}

	public URL back() {
		historyIndex--;
		if (historyIndex < 0) {
			historyIndex = 0;
		}
		URL url = history.get(historyIndex);
		displayPage(url);

		return url;
	}

	public boolean isFirst() {
		return (historyIndex == 0);
	}

	public boolean isLast() {
		return (historyIndex == history.size() - 1);
	}

	private void displayPage(URL pageURL) {

		SwingUtilities.invokeLater(() -> {

			try {
				setPage(pageURL);
			} catch (IOException ioException) {
				logger.log(Level.SEVERE, Msg.getString("HTMLContentPane.log.badUrl", pageURL.toString()), //$NON-NLS-1$
						ioException);
			}

		});

	}
}
