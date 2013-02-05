package edu.harvard.econcs.turkserver.server.gui;

import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

public class ControlPanel extends JPanel {

	private static final long serialVersionUID = 898019683117861450L;

	public ControlPanel() {
		setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));
		
		add(new JLabel("Control Panel"));
		
	}
	
}
