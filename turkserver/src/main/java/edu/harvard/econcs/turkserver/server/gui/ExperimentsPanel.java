package edu.harvard.econcs.turkserver.server.gui;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.SwingWorker;
import javax.swing.table.AbstractTableModel;
import javax.swing.border.BevelBorder;
import javax.swing.JButton;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.BoxLayout;

import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;

import edu.harvard.econcs.turkserver.mturk.RequesterServiceExt;
import edu.harvard.econcs.turkserver.mturk.TurkHITController;
import edu.harvard.econcs.turkserver.schema.Sets;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker.SessionSummary;
import edu.harvard.econcs.turkserver.server.mysql.MySQLDataTracker;
import javax.swing.ListSelectionModel;
import javax.swing.JTextArea;

public class ExperimentsPanel extends JSplitPane {

	private static final long serialVersionUID = 6441325813688236984L;
	
	MysqlConnectionPoolDataSource cpds;
	MySQLDataTracker dt;
	RequesterServiceExt req;
	TurkHITController hits;
	
	volatile String selectedSet;
	
	private JTable table;
	private ExperimentsTableModel expModel;
	private JPanel panel;
	private JButton btnPayWorkers;
	private JButton btnDisableUnusedHits;
	private JButton btnCheckAndDispose;
	private JLabel lblAssignedHits;
	private JLabel lblCompletedHits;
	private JLabel lblTotalHits;
	private JLabel label;
	private JLabel lblSelectASet;
	private JTextArea txtrFeedback;

	/**
	 * Create the panel.
	 */
	public ExperimentsPanel(MysqlConnectionPoolDataSource cpds, RequesterServiceExt req) {
		this.cpds = cpds;
		this.req = req;
		
		initGUI();
		
		dt = new MySQLDataTracker(cpds);
		hits = new TurkHITController(req, dt);
		
		new RefreshDatabaseWorker().execute();
	}
	
	private void initGUI() {
		setResizeWeight(0.5);		
		
		table = new JTable();
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		
		table.getSelectionModel().addListSelectionListener(new ExpSelectionListener());
		
		expModel = new ExperimentsTableModel();		
		table.setModel(expModel);				
		
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
		btnDisableUnusedHits.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (selectedSet != null && GUIUtils.checkRequesterNotNull(req, ExperimentsPanel.this))
					new DisableUnusedWorker().execute();
			}
		});
		
		panel.add(btnDisableUnusedHits);
		
		btnPayWorkers = new JButton("Review and Pay Workers");
		btnPayWorkers.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (selectedSet != null && GUIUtils.checkRequesterNotNull(req, ExperimentsPanel.this))
					new ReviewAndPayWorker(txtrFeedback.getText()).execute();
			}
		});
		
		txtrFeedback = new JTextArea();
		txtrFeedback.setText("Thanks for your work!");
		panel.add(txtrFeedback);
		
		panel.add(btnPayWorkers);
		
		btnCheckAndDispose = new JButton("Check and Dispose HITs");
		btnCheckAndDispose.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (selectedSet != null && GUIUtils.checkRequesterNotNull(req, ExperimentsPanel.this))
					new CheckAndDisposeWorker().execute();
			}
		});
		
		panel.add(btnCheckAndDispose);
	}

	public class ExperimentsTableModel extends AbstractTableModel {		
				
		private static final long serialVersionUID = 1L;
		
		private List<Sets> data;

		@Override
		public int getRowCount() {			
			return data == null ? 0 : data.size();
		}
	
		@Override
		public int getColumnCount() {			
			return 1;
		}
	
		@Override
		public Object getValueAt(int rowIndex, int columnIndex) {			
			return data.get(rowIndex).getName();
		}
	
	}

	public class ExpSelectionListener implements ListSelectionListener {
		@Override
		public void valueChanged(ListSelectionEvent e) {
			int selectedRow = table.getSelectedRow();
			selectedSet = selectedRow < 0 ? null : expModel.getValueAt(selectedRow, 0).toString();
			if( selectedSet != null ) new RefreshSetWorker().execute();
		}	
	}
	
	public class RefreshDatabaseWorker extends SwingWorker<List<Sets>, Object> {
		@Override protected List<Sets> doInBackground() throws Exception {
			return dt.getAllSets();
		}	
		@Override protected void done() {
			List<Sets> data;
			try {
				data = get();
			} catch (Exception e) {
				GUIUtils.showException(e, ExperimentsPanel.this);				
				e.printStackTrace();
				return;
			}
			
			System.out.println("Got sets");
			
			expModel.data = data;
			expModel.fireTableDataChanged();			
		}		
	}

	public class RefreshSetWorker extends SwingWorker<SessionSummary, Object> {				
		@Override protected SessionSummary doInBackground() throws Exception {
			dt.setSetId(selectedSet);
			return dt.getSetSessionSummary();
		}
		
		@Override protected void done() {
			SessionSummary data;
			try {
				data = get();
			} catch (Exception e) {
				GUIUtils.showException(e, ExperimentsPanel.this);
				e.printStackTrace();
				return;
			}
			
			lblTotalHits.setText("Total HITs: " + data.createdHITs);
			lblAssignedHits.setText("Assigned HITs: " + data.assignedHITs);
			lblCompletedHits.setText("Completed HITs: " + data.completedHITs);
						
		}
	}

	public class DisableUnusedWorker extends SwingWorker<Integer, Object> {	
		@Override protected Integer doInBackground() throws Exception {
			return hits.disableUnusedFromDB();			
		}
		
		@Override protected void done() {
			int deletedCount;
			try {
				deletedCount = get();
			} catch (Exception e) {
				GUIUtils.showException(e, ExperimentsPanel.this);
				e.printStackTrace();
				return;
			}
			new RefreshDatabaseWorker().execute();
			JOptionPane.showMessageDialog(ExperimentsPanel.this, deletedCount + " unused HITs disabled");			
		}
	}

	public class ReviewAndPayWorker extends SwingWorker<Integer, Object> {	
		final String approvalText;
		
		ReviewAndPayWorker(String approvalText) { 
			this.approvalText = approvalText; 
		}
		
		@Override protected Integer doInBackground() throws Exception {					
			return hits.reviewAndPayWorkers(approvalText);
		}
		
		// TODO inform about how many workers paid and $ spent
		@Override protected void done() {
			int approvedCount;
			try {
				approvedCount = get();
			} catch (Exception e) {
				GUIUtils.showException(e, ExperimentsPanel.this);
				e.printStackTrace();
				return;
			}
			JOptionPane.showMessageDialog(ExperimentsPanel.this, approvedCount + " HITs paid");
		}
	}

	public class CheckAndDisposeWorker extends SwingWorker<Integer, Object> {	
		@Override protected Integer doInBackground() throws Exception {			
			return hits.checkAndDispose();
		}
		
		@Override protected void done() {
			int disposedCount;
			try {
				disposedCount = get();
			} catch (Exception e) {
				GUIUtils.showException(e, ExperimentsPanel.this);
				e.printStackTrace();
				return;
			}
			JOptionPane.showMessageDialog(ExperimentsPanel.this, disposedCount + " HITs disposed");
		}
	}

}
