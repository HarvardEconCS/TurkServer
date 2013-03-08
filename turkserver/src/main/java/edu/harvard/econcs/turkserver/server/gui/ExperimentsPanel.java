package edu.harvard.econcs.turkserver.server.gui;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.BevelBorder;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

public class ExperimentsPanel extends JSplitPane {

	private static final long serialVersionUID = 6441325813688236984L;
	private JTable table;	
	private JPanel panel;
	private JButton btnPayWorkers;
	private JButton btnDisableUnusedHits;
	private JButton btnCheckAndDispose;
	private JLabel lblAssignedHits;
	private JLabel lblCompletedHits;
	private JLabel lblTotalHitsOutstanding;

	/**
	 * Create the panel.
	 */
	public ExperimentsPanel() {

		initGUI();
	}
	private void initGUI() {
		setResizeWeight(0.5);
		
		
		table = new JTable();
		table.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		table.setModel(new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"New column", "New column"
			}
		));				
		
		setLeftComponent(table);		
				
		panel = new JPanel();
		setRightComponent(panel);
		panel.setLayout(null);
		
		lblAssignedHits = new JLabel("Assigned HITs");
		lblAssignedHits.setBounds(51, 15, 87, 15);
		panel.add(lblAssignedHits);
		
		lblCompletedHits = new JLabel("Completed HITs");
		lblCompletedHits.setBounds(46, 65, 97, 15);
		panel.add(lblCompletedHits);
		
		lblTotalHitsOutstanding = new JLabel("Total HITs Outstanding");
		lblTotalHitsOutstanding.setBounds(23, 115, 142, 15);
		panel.add(lblTotalHitsOutstanding);
		
		btnDisableUnusedHits = new JButton("Disable Unused HITs");
		btnDisableUnusedHits.setBounds(0, 150, 219, 45);
		panel.add(btnDisableUnusedHits);
		
		btnPayWorkers = new JButton("Review and Pay Workers");
		btnPayWorkers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
			}
		});
		
		btnPayWorkers.setBounds(0, 200, 219, 45);
		panel.add(btnPayWorkers);
		
		btnCheckAndDispose = new JButton("Check and Dispose HITs");
		btnCheckAndDispose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
			}
		});
		btnCheckAndDispose.setBounds(0, 250, 219, 50);
		panel.add(btnCheckAndDispose);
	}


}
