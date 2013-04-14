package edu.harvard.econcs.turkserver.server.gui;

import java.awt.GridLayout;

import javax.annotation.Nullable;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.google.inject.Inject;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import edu.harvard.econcs.turkserver.mturk.RequesterServiceExt;

public class TSTabbedPanel extends JPanel {

	private static final long serialVersionUID = -3363135146104711554L;

	private JTabbedPane tabbedPane;	
	
	@Inject
	public TSTabbedPanel(
			MysqlConnectionPoolDataSource cpds,
			@Nullable RequesterServiceExt req) {
		super(new GridLayout(1, 1));
				
		tabbedPane = new JTabbedPane();
		// Enable scrolling tabs.
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
				
		add(tabbedPane);
		
		ControlPanel controlPanel = new ControlPanel();
		tabbedPane.addTab("Control", null, controlPanel, null);		
		
		WorkerPanel workerPanel = new WorkerPanel(req);
		tabbedPane.addTab("Workers", null, workerPanel, null);
		
		ExperimentsPanel experimentsPanel = new ExperimentsPanel(cpds, req);
		tabbedPane.addTab("Experiments", null, experimentsPanel, null);		
		
		MaintenancePanel maintenancePanel = new MaintenancePanel(req);
		tabbedPane.addTab("Maintenance", null, maintenancePanel, null);				
	}
	
	public void addPanel(String title, JPanel panel) {
		tabbedPane.add(title, panel);
		// Select this panel
		tabbedPane.setSelectedComponent(panel);
	}
}
