package edu.harvard.econcs.turkserver.server.gui;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class TSTabbedPanel extends JPanel {

	private static final long serialVersionUID = -3363135146104711554L;

	private JTabbedPane tabbedPane;
	
	public TSTabbedPanel() {
		super(new GridLayout(1, 1));
		
		tabbedPane = new JTabbedPane();
		
		tabbedPane.addTab("Control", new ControlPanel());
		
		add(tabbedPane);
		// Enable scrolling tabs.
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
	}
	
	public void addPanel(String title, JPanel panel) {
		tabbedPane.add(title, panel);
		// Select this panel
		tabbedPane.setSelectedComponent(panel);
	}
}
