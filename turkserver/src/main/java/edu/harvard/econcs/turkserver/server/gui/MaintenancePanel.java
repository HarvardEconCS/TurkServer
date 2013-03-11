package edu.harvard.econcs.turkserver.server.gui;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import edu.harvard.econcs.turkserver.mturk.RequesterServiceExt;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class MaintenancePanel extends JPanel {
	
	private static final long serialVersionUID = 1L;

	RequesterServiceExt req;
	
	/**
	 * Create the panel.
	 */	
	public MaintenancePanel(RequesterServiceExt req) {
		initGUI();
		
		this.req = req;
	}
	
	private void initGUI() {
		
		JButton btnDisableUnassigned = new JButton("Disable Unassigned HITs");
		btnDisableUnassigned.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (GUIUtils.checkRequesterNotNull(req, MaintenancePanel.this))
					new DisableUnassignedWorker().execute();
			}
		});
		
		JButton btnDisableAllHits = new JButton("Disable All HITs on MTurk");
		btnDisableAllHits.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (GUIUtils.checkRequesterNotNull(req, MaintenancePanel.this))
					new DisableAllHITsWorker().execute();
			}
		});
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(btnDisableUnassigned);
		add(btnDisableAllHits);
	}

	class DisableAllHITsWorker extends SwingWorker<Integer, Object> {
		@Override
		protected Integer doInBackground() throws Exception {			
			return req.disableAllHITs();						
		}		
		
		@Override
		protected void done() {
			try {
				JOptionPane.showMessageDialog(MaintenancePanel.this, "Deleted HITs: " + get());
			} catch (Exception e) {			
				e.printStackTrace();
			}
		}
	}
	
	class DisableUnassignedWorker extends SwingWorker<Integer, Object> {
		@Override
		protected Integer doInBackground() throws Exception {			
			return req.disableUnassignedHITs();						
		}
		
		@Override
		protected void done() {
			try {
				JOptionPane.showMessageDialog(MaintenancePanel.this, "Deleted Unassigned HITs: " + get());
			} catch (Exception e) {			
				e.printStackTrace();
			}
		}
	}
}
