/*
 * Mars Simulation Project
 * MarsTerminal.java
 * @date 2024-09-01
 * @author Manny Kung
 */
package com.mars_sim.console;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTextPane;
import javax.swing.KeyStroke;

import org.beryx.textio.TextTerminal;
import org.beryx.textio.jline.JLineTextTerminal;
import org.beryx.textio.swing.SwingTextTerminal;

import com.mars_sim.core.GameManager;
import com.mars_sim.core.Simulation;
import com.mars_sim.core.SimulationRuntime;
import com.mars_sim.core.time.ClockListener;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.MasterClock;

@SuppressWarnings("serial")
public class MarsTerminal extends SwingTextTerminal implements ClockListener {
	
    private static final Logger logger = Logger.getLogger(MarsTerminal.class.getName());

	/** Icon image filename for frame */
    private static final String ICON_IMAGE = "/icons/lander_hab64.png";
    public static final String MARS_SIM = "Mars Simulation Project";
    public static final String ABOUT_MSG =
			"     " + "     Version " + SimulationRuntime.VERSION.getVersionTag() + "\n"
			+ "     " + SimulationRuntime.VERSION.getBuildString();
		
	private static final int DEFAULT_WIDTH = 1024;
	private static final int DEFAULT_HEIGHT = 600;

	private int width;
	private int height;

	private JFrame frame = getFrame();
	
    private final JPopupMenu popup = new JPopupMenu();

	private MasterClock masterClock;

    private static class PopupListener extends MouseAdapter {
    	
        private final JPopupMenu popup;

        public PopupListener(JPopupMenu popup) {
            this.popup = popup;
        }
        @Override
        public void mousePressed(MouseEvent e) {
            maybeShowPopup(e);
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            maybeShowPopup(e);
        }

        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger()) {
                popup.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    }

    public MarsTerminal() {

        configureMainMenu();

        JTextPane textPane = getTextPane();
        addAction("ctrl C", "Copy", textPane::copy);
        addAction("ctrl V", "Paste", textPane::paste);
        MouseListener popupListener = new PopupListener(popup);
        textPane.addMouseListener(popupListener);
    	frame.toBack();
    	
    	frame.setAlwaysOnTop(false);
    }

    public static void clearScreen(TextTerminal<?> terminal) {
        if (terminal instanceof JLineTextTerminal) {
            terminal.print("\033[H\033[2J");
        } else if (terminal instanceof SwingTextTerminal) {
            ((SwingTextTerminal) terminal).resetToOffset(0);
        }
    }

	/**
	 * Converts from icon to image.
	 *
	 * @param icon
	 * @return
	 */
	public static Image iconToImage(Icon icon) {
		// Note: Use frame.setIconImage(image) afterward to set the image of a frame
		if (icon instanceof ImageIcon) {
			return ((ImageIcon)icon).getImage();
		}

		else {
			int w = icon.getIconWidth();
			int h = icon.getIconHeight();
			GraphicsEnvironment ge =
					GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice gd = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gd.getDefaultConfiguration();
			BufferedImage image = gc.createCompatibleImage(w, h);
			Graphics2D g = image.createGraphics();
			icon.paintIcon(null, g, 0, 0);
			g.dispose();
			return image;
		}
	}

	private void setSize(int w, int h) {
		width = w;
		height = h;
		frame.setPreferredSize(new Dimension(w, h));
		frame.pack();
	}

	private void setHeight(int h) {
		height = h;
		frame.setPreferredSize(new Dimension(width, h));
		frame.pack();
	}

	private void setWidth(int w) {
		width = w;
		frame.setPreferredSize(new Dimension(w, height));
		frame.pack();
	}

    private void configureMainMenu() {

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				// Save simulation and UI configuration when window is closed.
				int reply = JOptionPane.showConfirmDialog(null, "Are you sure you want to quit?", "Exit", JOptionPane.YES_NO_OPTION);
		        if (reply == JOptionPane.YES_OPTION) {
		            printf("Exiting the Simulation..." + System.lineSeparator());
		            Simulation sim = Simulation.instance();
		        	sim.endSimulation();
		    		if (sim.getMasterClock() != null)
		    			sim.getMasterClock().exitProgram();

					frame.setVisible(false);
			    	dispose(null);
					System.exit(0);
		        }
			}
		});


        frame.setResizable(true);
        
		setPaneTitle(SimulationRuntime.SHORT_TITLE);

        setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

		ImageIcon icon = new ImageIcon(getClass().getResource(ICON_IMAGE));
		frame.setIconImage(iconToImage(icon));

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Help");
        menu.setMnemonic(KeyEvent.VK_H);

        JMenu sizeMenu = new JMenu("Term Size");
        menu.add(sizeMenu);

        JMenu heightMenu = new JMenu("Height");
        sizeMenu.add(heightMenu);

        JMenuItem h0 = new JMenuItem("600");
        h0.addActionListener(e -> setHeight(600));
        heightMenu.add(h0);

        JMenuItem h1 = new JMenuItem("768");
        h1.addActionListener(e -> setHeight(768));
        heightMenu.add(h1);

        JMenuItem h2 = new JMenuItem("900");
        h2.addActionListener(e -> setHeight(900));
        heightMenu.add(h2);

        JMenuItem h3 = new JMenuItem("1050");
        h3.addActionListener(e -> setHeight(1050));
        heightMenu.add(h3);

        JMenuItem h4 = new JMenuItem("1200");
        h4.addActionListener(e -> setHeight(1200));
        heightMenu.add(h4);

        JMenu widthMenu = new JMenu("Width");
        sizeMenu.add(widthMenu);

        JMenuItem w0 = new JMenuItem("800");
        w0.addActionListener(e -> setWidth(800));
        widthMenu.add(w0);

        JMenuItem w1 = new JMenuItem("1024");
        w1.addActionListener(e -> setWidth(1024));
        widthMenu.add(w1);

        JMenuItem w2 = new JMenuItem("1280");
        w2.addActionListener(e -> setWidth(1280));
        widthMenu.add(w2);

        JMenuItem w3 = new JMenuItem("1366");
        w3.addActionListener(e -> setWidth(1366));
        widthMenu.add(w3);

        JMenuItem w4 = new JMenuItem("1440");
        w4.addActionListener(e -> setWidth(1440));
        widthMenu.add(w4);

        JMenuItem w5 = new JMenuItem("1600");
        w5.addActionListener(e -> setWidth(1600));
        widthMenu.add(w5);

        JMenuItem w6 = new JMenuItem("1920");
        w6.addActionListener(e -> setWidth(1920));
        widthMenu.add(w6);

        JMenuItem pauseItem = new JMenuItem("Pause/Unpause", KeyEvent.VK_P);

        pauseItem.addActionListener(e -> {

           	if (masterClock == null) {
        		masterClock = Simulation.instance().getMasterClock();
        	}

        	if (masterClock != null) {

				if (masterClock.isPaused()) {
					masterClock.setPaused(false, false);
					printf(System.lineSeparator() + System.lineSeparator());
					printf("                      [ Simulation Unpaused ]");
					printf(System.lineSeparator() + System.lineSeparator());
					logger.config("                            [ Simulation Paused ]");
				}
				else {
					masterClock.setPaused(true, false);
					printf(System.lineSeparator() + System.lineSeparator());
					printf("                       [ Simulation Paused ]");
					printf(System.lineSeparator() + System.lineSeparator());
					logger.config("                            [ Simulation Paused ]");
				}
        	}
        });
        menu.add(pauseItem);

        JMenuItem clearItem = new JMenuItem("Clear Screen", KeyEvent.VK_C);
        clearItem.addActionListener(e -> clearScreen(this));
        menu.add(clearItem);

        JMenuItem menuItem = new JMenuItem("About", KeyEvent.VK_A);
        menuItem.addActionListener(e -> openAboutDialog());
        menu.add(menuItem);

        menuBar.add(menu);
        		
        frame.setJMenuBar(menuBar);

        frame.setVisible(true);
    }
    
    /**
     * Opens the about dialog box.
     */
    public void openAboutDialog() {
    	JOptionPane.showMessageDialog(frame, ABOUT_MSG, MARS_SIM, JOptionPane.INFORMATION_MESSAGE);
    }

    private boolean addAction(String keyStroke, String menuText, Runnable action) {
        KeyStroke ks = KeyStroke.getKeyStroke(keyStroke);
        if(ks == null) {
            return false;
        }
        JMenuItem menuItem = new JMenuItem(menuText);
        menuItem.addActionListener(e -> action.run());
        popup.add(menuItem);

        JTextPane textPane = getTextPane();
        String actionKey = "MarsTerminal." + keyStroke.replaceAll("\\s", "-");
        textPane.getInputMap().put(ks, actionKey);
        textPane.getActionMap().put(actionKey, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
        return true;
    }

	public void changeTitle(boolean isPaused) {
		String mode = switch (GameManager.getGameMode()) {
			case COMMAND -> "Command Mode";
			case SANDBOX -> "Sandbox Mode";
			case SPONSOR -> "Sponsor Mode";
			case SOCIETY -> "Society Mode";
		};

		setPaneTitle(SimulationRuntime.SHORT_TITLE + "  -  " + mode + (isPaused ? "  -  [ P A U S E ]" : ""));
	}

	@Override
	public void clockPulse(ClockPulse pulse) {
		// not needed
	}

	@Override
	public void pauseChange(boolean isPaused, boolean showPane) {
		changeTitle(isPaused);
	}
}