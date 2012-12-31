package edu.harvard.econcs.turkserver.server.gui;

import java.awt.Dimension;

import javax.swing.JFrame;

import com.google.inject.Inject;

public class ServerFrame extends JFrame {

	private static final long serialVersionUID = 993316450266296548L;	
	
	TSTabbedPanel tabbedPanel;
	
	@Inject
	public ServerFrame(TSTabbedPanel tabbedPanel) {
		super("TurkServer");
		
		setMinimumSize(new Dimension(800, 600));				
		
		add(this.tabbedPanel = tabbedPanel);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
	}	

}
