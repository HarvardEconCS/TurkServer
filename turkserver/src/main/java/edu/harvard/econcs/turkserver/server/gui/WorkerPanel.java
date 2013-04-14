package edu.harvard.econcs.turkserver.server.gui;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.SwingWorker;

import edu.harvard.econcs.turkserver.mturk.RequesterServiceExt;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import javax.swing.JTextField;

public class WorkerPanel extends JPanel {
	
	private static final long serialVersionUID = 1L;

	RequesterServiceExt req;
	
	private JTextArea txaWorkerIds;
	private JLabel lblCommaseparatedWorkerIds;
	private JLabel lblMessage;
	private JTextArea txaMessage;
	private JButton btnSendMessage;
	private JLabel lblSubject;
	private JTextField txtSubject;

	/**
	 * Create the panel.
	 */
	public WorkerPanel(RequesterServiceExt req) {
		this.req = req;
		
		initGUI();
	}

	private void initGUI() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		lblCommaseparatedWorkerIds = new JLabel("Comma-separated Worker IDs:");
		add(lblCommaseparatedWorkerIds);
		
		txaWorkerIds = new JTextArea();
		txaWorkerIds.setWrapStyleWord(true);
		txaWorkerIds.setLineWrap(true);
		txaWorkerIds.setRows(10);
		add(txaWorkerIds);
		
		lblSubject = new JLabel("Subject:");
		add(lblSubject);
		
		txtSubject = new JTextField();
		add(txtSubject);
		txtSubject.setColumns(10);
		
		lblMessage = new JLabel("Message:");
		add(lblMessage);
		
		txaMessage = new JTextArea();
		txaMessage.setWrapStyleWord(true);
		txaMessage.setRows(10);
		txaMessage.setLineWrap(true);
		add(txaMessage);
		
		btnSendMessage = new JButton("Send Message");
		btnSendMessage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new SendMessageWorker(txaWorkerIds.getText(), txtSubject.getText(), txaMessage.getText()).execute();
			}
		});
		add(btnSendMessage);
	}

	public class SendMessageWorker extends SwingWorker<String, Object> {	
		String workerIds, subject, message;
		
		public SendMessageWorker(String workerIds, String subject, String message) {
			this.workerIds = workerIds;
			this.subject = subject;
			this.message = message;
		}
	
		@Override
		protected String doInBackground() throws Exception {			
			String[] ids = workerIds.split("[, ]+");
			
			req.notifyAllWorkers(subject, message, Arrays.asList(ids), 0);
			
			return "Workers Notified!";
		}
		
		@Override
		protected void done() {			
			try {
				JOptionPane.showMessageDialog(WorkerPanel.this, get());
			} catch (Exception e) {			
				e.printStackTrace();
			}
		}
		
	}

}
