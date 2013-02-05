package edu.harvard.econcs.turkserver.server.gui;

import java.awt.GridLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

public class TSTabbedPanel extends JPanel {

	private static final long serialVersionUID = -3363135146104711554L;

	private JTabbedPane tabbedPane;	
	
	public TSTabbedPanel() {
		super(new GridLayout(1, 1));
		
		// Enable scrolling tabs.
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		
		tabbedPane = new JTabbedPane();
		add(tabbedPane);
		
		ControlPanel controlPanel = new ControlPanel();
		tabbedPane.addTab("Control", null, controlPanel, null);		
		
		WorkerPanel workerPanel = new WorkerPanel();
		tabbedPane.addTab("Workers", null, workerPanel, null);
		
		ExperimentsPanel experimentsPanel = new ExperimentsPanel();
		tabbedPane.addTab("Experiments", null, experimentsPanel, null);		
		
		MaintenancePanel maintenancePanel = new MaintenancePanel();
		tabbedPane.addTab("Maintenance", null, maintenancePanel, null);				
	}
	
	public void addPanel(String title, JPanel panel) {
		tabbedPane.add(title, panel);
		// Select this panel
		tabbedPane.setSelectedComponent(panel);
	}
}
