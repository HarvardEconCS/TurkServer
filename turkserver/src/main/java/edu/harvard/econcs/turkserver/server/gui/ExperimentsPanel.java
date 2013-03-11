package edu.harvard.econcs.turkserver.server.gui;

import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import javax.swing.border.BevelBorder;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.border.EtchedBorder;
import javax.swing.BoxLayout;

import com.amazonaws.mturk.service.axis.RequesterService;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

public class ExperimentsPanel extends JSplitPane {

	private static final long serialVersionUID = 6441325813688236984L;
	
	MysqlConnectionPoolDataSource cpds;
	RequesterService req;
	
	private JTable table;	
	private JPanel panel;
	private JButton btnPayWorkers;
	private JButton btnDisableUnusedHits;
	private JButton btnCheckAndDispose;
	private JLabel lblAssignedHits;
	private JLabel lblCompletedHits;
	private JLabel lblTotalHits;
	private JLabel label;
	private JLabel lblSelectASet;

	/**
	 * Create the panel.
	 */
	public ExperimentsPanel(MysqlConnectionPoolDataSource cpds, RequesterService req) {
		this.cpds = cpds;
		this.req = req;
		
		initGUI();
		
		new RefreshDatabaseWorker().execute();
	}
	
	private void initGUI() {
		setResizeWeight(0.5);		
		
		table = new JTable();
		table.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		table.setModel(new DefaultTableModel(
			new Object[][] {
			},
			new String[] {
				"Set", "New column", "New column", "New column"
			}
		) {
			Class[] columnTypes = new Class[] {
				String.class, Object.class, Object.class, Object.class
			};
			public Class getColumnClass(int columnIndex) {
				return columnTypes[columnIndex];
			}
		});				
		
		setLeftComponent(table);		
				
		panel = new JPanel();
		panel.setBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null));
		setRightComponent(panel);
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		
		lblSelectASet = new JLabel("Select a set of experiments to manage your stuff.");
		panel.add(lblSelectASet);
		
		lblTotalHits = new JLabel("Total HITs");
		panel.add(lblTotalHits);
		
		lblAssignedHits = new JLabel("Assigned HITs");
		panel.add(lblAssignedHits);
		
		lblCompletedHits = new JLabel("Completed HITs");
		panel.add(lblCompletedHits);
		
		label = new JLabel("");
		panel.add(label);
		
		btnDisableUnusedHits = new JButton("Disable Unused HITs");
		btnDisableUnusedHits.addActionListener(new BtnDisableUnusedHitsActionListener());
		panel.add(btnDisableUnusedHits);
		
		btnPayWorkers = new JButton("Review and Pay Workers");
		btnPayWorkers.addActionListener(new BtnPayWorkersActionListener());
		panel.add(btnPayWorkers);
		
		btnCheckAndDispose = new JButton("Check and Dispose HITs");
		btnCheckAndDispose.addActionListener(new BtnCheckAndDisposeActionListener());
		panel.add(btnCheckAndDispose);

	}

	private class BtnDisableUnusedHitsActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
		}
	}
	
	private class BtnPayWorkersActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
		}
	}

	private class BtnCheckAndDisposeActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
		}
	}
	
	
	public class RefreshDatabaseWorker extends SwingWorker<Object, Object> {
		@Override
		protected Object doInBackground() throws Exception {
			// TODO Auto-generated method stub
			return null;
		}
	}

}
