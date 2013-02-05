package edu.harvard.econcs.turkserver.server.gui;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

public class MaintenancePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;

	/**
	 * Create the panel.
	 */
	public MaintenancePanel() {
		initGUI();
	}
	private void initGUI() {
		
		JButton btnDisableUnassigned = new JButton("Disable Unassigned HITs");
		
		JButton btnDisableAllHits = new JButton("Disable All HITs on MTurk");
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(btnDisableUnassigned);
		add(btnDisableAllHits);
	}

}
