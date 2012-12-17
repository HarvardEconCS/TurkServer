package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.client.JTextFieldLimit;
import edu.harvard.econcs.turkserver.client.Lobby;
import edu.harvard.econcs.turkserver.client.SortedListModel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import net.andrewmao.misc.Utils;

public class ServerFrame extends JFrame implements ActionListener {

	private static final long serialVersionUID = -5350221106754787412L;
	
	private static final String updateStatusCmd = "UpdateStatus";
	
	private static final String runningExpsText = "Running Experiments: ";
	private static final String doneExpsText = "Completed Experiments: ";
	
	private final GroupServer server;
	
	private JTextField statusMsg;
	
	private SortedListModel<HITWorkerImpl> userListModel;
	
	private DefaultListModel runningExpModel;
	private JList runningExpList;
	private DefaultListModel doneExpModel;
	private JList doneExpList;
	
	private JLabel currentUsers;		
	
	private JLabel runningExpsLabel;
	private JLabel doneExpsLabel;
	
	private Timer timeTicker;
	
	public ServerFrame(GroupServer host) {
		super("Experiment Monitor");
		this.server = host;
		
		setMinimumSize(new Dimension(800, 600));
		
		// Put lobby on left and experiments on right
		getContentPane().setLayout(new GridLayout(1, 2));
		
		// Lobby
		JPanel lobbyPanel = new JPanel();
		lobbyPanel.setLayout(new BoxLayout(lobbyPanel, BoxLayout.PAGE_AXIS));
		lobbyPanel.setBorder(BorderFactory.createTitledBorder("Lobby"));
		
		JPanel statusPanel = new JPanel();
		// Limit the height of this panel
		statusPanel.setMaximumSize(new Dimension(800, 100));
		statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.LINE_AXIS));
		
		JLabel statusLabel = new JLabel("Message:");
		statusMsg = new JTextField(40);
		statusMsg.setDocument(new JTextFieldLimit(80));
		JButton updateStatusButton = new JButton("Update Status");
		updateStatusButton.setActionCommand(updateStatusCmd);
		updateStatusButton.addActionListener(this);								
		
		statusPanel.add(statusLabel);
		statusPanel.add(statusMsg);
		statusPanel.add(updateStatusButton);
		
		lobbyPanel.add(statusPanel);
		
		currentUsers = new JLabel();
		lobbyPanel.add(currentUsers);
		userListModel = new SortedListModel<HITWorkerImpl>(new UsernameComparator());
		JList userList = new JList(userListModel);
		userList.setCellRenderer(new ServerLobbyCellRenderer());
		lobbyPanel.add(new JScrollPane(userList));
		
		// Experiments
		JPanel expPanel = new JPanel();						
		expPanel.setBorder(BorderFactory.createTitledBorder("Experiments"));
		expPanel.setLayout(new GridLayout(2, 1));
		
		// Running experiments
		JPanel runningExpPanel = new JPanel();
		runningExpPanel.setLayout(new BoxLayout(runningExpPanel, BoxLayout.PAGE_AXIS));
		runningExpPanel.setBorder(BorderFactory.createEtchedBorder());
		
		runningExpsLabel = new JLabel(runningExpsText + 0);
		runningExpPanel.add(runningExpsLabel);
		
		runningExpModel = new DefaultListModel();		
		runningExpList = new JList(runningExpModel);
		runningExpList.setCellRenderer(new RunningExpCellRenderer());
		runningExpPanel.add(new JScrollPane(runningExpList));
		
		// Done experiments
		JPanel doneExpPanel = new JPanel();
		doneExpPanel.setLayout(new BoxLayout(doneExpPanel, BoxLayout.PAGE_AXIS));
		doneExpPanel.setBorder(BorderFactory.createEtchedBorder());
		
		doneExpsLabel = new JLabel(doneExpsText + 0);
		doneExpPanel.add(doneExpsLabel);
		
		doneExpModel = new DefaultListModel();
		doneExpList = new JList(doneExpModel);
		doneExpPanel.add(new JScrollPane(doneExpList));
		
		expPanel.add(runningExpPanel);
		expPanel.add(doneExpPanel);
				
		add(lobbyPanel);
		add(expPanel);
		
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		pack();
		setLocationRelativeTo(null);
		setVisible(true);
		
		timeTicker = new Timer(1000, this);
		timeTicker.start();
	}	
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if( e.getActionCommand() != null && e.getActionCommand().equals(updateStatusCmd) ) {
			server.serverMessage.set(statusMsg.getText());
			
			/* publish the message to lobby
			 * It's fine to broadcast here as it is a rare occurrence
			 */
			server.sendLobbyStatus();			
		}
		else if( e.getSource() == timeTicker && runningExpModel.size() > 0 ) {			
			// Only bother with this if there are running experiments
			runningExpList.repaint();
		}
	}

	public void updateLobbyModel() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Update user count
				currentUsers.setText("Users: " + server.lobbyStatus.size());															
				// Update users list				
				userListModel.updateModel(server.lobbyStatus.keySet());
			}			
		});		
	}
	
	private class ServerLobbyCellRenderer extends JLabel implements ListCellRenderer {							
		private static final long serialVersionUID = -9092662058995935206L;

		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
						
			Boolean b = server.lobbyStatus.get(value); 
			if( b != null) setIcon( b == true ? Lobby.ready : Lobby.notReady );
			
			HITWorkerImpl id = (HITWorkerImpl) value;
			
			setText( id.getUsername() );
			// TODO render textual messages
			
			setEnabled(true); // not list.isEnabled()); otherwise the icon won't draw
			setFont(list.getFont());
			setOpaque(true);
			
			return (JLabel) this;
		}		
	}

	public void newExperiment(final ExperimentControllerImpl exp) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {				
				runningExpsLabel.setText(runningExpsText + server.guiListener.inProgress.get());
				runningExpModel.addElement(exp);				
			}			
		});
	}
	
	private class RunningExpCellRenderer extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 5708685323712954603L;

		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			ExperimentControllerImpl exp = (ExperimentControllerImpl) value;
			
			setText( String.format("%s %s (%d)",
					Utils.paddedClockString(System.currentTimeMillis() - exp.expStartTime), 
					exp.toString(), exp.group.groupSize()
					));			
			
			setEnabled(true); // not list.isEnabled()); otherwise the icon won't draw
			setFont(list.getFont());
			setOpaque(true);
			
			return (JLabel) this;
		}
	
	}

	public void finishedExperiment(final ExperimentControllerImpl exp) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				runningExpsLabel.setText(runningExpsText + server.guiListener.inProgress.get());
				runningExpModel.removeElement(exp);
				
				doneExpsLabel.setText(doneExpsText + server.guiListener.completed.get());
				doneExpModel.addElement(exp);
			}			
		});	
	}
	
	public class UsernameComparator implements Comparator<HITWorkerImpl> {	
		@Override
		public int compare(HITWorkerImpl o1, HITWorkerImpl o2) {
			String u1 = o1.getUsername();
			String u2 = o1.getUsername();

			if( u1 != null ) {
				int comp = u1.compareTo(u2);
				if( comp != 0 ) return comp;				
			}
			
			return o1.getHitId().compareTo(o2.getHitId());
		}	
	}
}
