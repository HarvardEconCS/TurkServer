/**
 * 
 */
package edu.harvard.econcs.turkserver.client;

import edu.harvard.econcs.turkserver.ColorIcon;
import edu.harvard.econcs.turkserver.LobbyUpdateResp;
import edu.harvard.econcs.turkserver.UserStatus;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.TreeSet;

import javax.swing.*;

/**
 * @author mao
 *
 */
public class Lobby extends JPanel {
	private static final long serialVersionUID = 948003342518575634L;
	
	public static final String updateStatusCmd = "UpdateStatus";
	
	public static final Icon notReady = new ColorIcon(Color.RED, false);
	public static final Icon ready = new ColorIcon(Color.GREEN, false);

	private static final String serverMsgText = "Server Message: ";
	
	private static final String LobbyUsersText = "Users In Lobby: ";
	private static final String requiredUsersText = "Required to Start: ";
	private static final String readyUsersText = "Ready Users: ";
	
	private static final String totalUsersText = "Total Users: ";
	private static final String totalGamesText = "Games In Progress: ";
	
	private final GUIController gc;
	
	private JLabel serverMessage;
	
	private JPanel listPanel;
	private JList userList;
	private SortedListModel<UserStatus> listModel;
	private JScrollPane listScrollPane;
	
	private JPanel statusPanel;
	private JLabel statusLabel;
	private JTextField statusMsg;
	JButton updateStatusButton;
	
	private JPanel infoPanel;
	private JLabel currentUsers;
	private JLabel requiredUsers;
	private JLabel readyUsers;
	
	private JLabel totalUsers;
	private JLabel totalGames;
	
	JCheckBox joinCheckBox;
	
	public Lobby(GUIController gc) {
		this.gc = gc;
		
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		
		// Set up user list
		{
			listPanel = new JPanel();
			listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.PAGE_AXIS));			
			listPanel.setMinimumSize(new Dimension(600, 600));
			listPanel.setBorder(BorderFactory.createTitledBorder( "Connected Users" ));
			
			serverMessage = new JLabel();
			serverMessage.setAlignmentX(CENTER_ALIGNMENT);
			
			listModel = new SortedListModel<UserStatus>(new UserStatus.UsernameComparator());
			userList = new JList(listModel);
			userList.setBorder(BorderFactory.createLoweredBevelBorder());
			
			// Give enough room to draw icons
			userList.setFixedCellHeight(ColorIcon.HEIGHT + 4);
			
			// disable selecting (for now, selection indicates self)		
			userList.setEnabled(false);		
			userList.setCellRenderer(new LobbyCellRenderer());
						
			listScrollPane = new JScrollPane(userList);
			
			listPanel.add(serverMessage);
			listPanel.add(listScrollPane);
			
			listPanel.add(Box.createHorizontalGlue());
			
			// Add the message box to send message to other users
			statusPanel = new JPanel();
			// Limit the height of this panel
			statusPanel.setMaximumSize(new Dimension(700, 100));
			statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.LINE_AXIS));
			
			statusLabel = new JLabel("Status Message:");
			statusMsg = new JTextField(40);
			statusMsg.setDocument(new JTextFieldLimit(60));
			updateStatusButton = new JButton("Update Status");
			updateStatusButton.setActionCommand(Lobby.updateStatusCmd);
			updateStatusButton.addActionListener(gc);								
			
			statusPanel.add(statusLabel);
			statusPanel.add(statusMsg);
			statusPanel.add(updateStatusButton);
			
			listPanel.add(statusPanel);			
		}
		
		// Set up info stuff 
		{						
			infoPanel = new JPanel();
			 
			infoPanel.setMinimumSize(new Dimension(160, 200));
			infoPanel.setMaximumSize(new Dimension(200, 1000));
			
			infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.PAGE_AXIS));
			infoPanel.setBorder(BorderFactory.createTitledBorder( "Server Info" ));

			currentUsers = new JLabel();
			requiredUsers = new JLabel();
			readyUsers = new JLabel();
			
			totalUsers = new JLabel();
			totalGames = new JLabel();
			
			JLabel readyInfo = new JLabel(
					"<html>" +
					"Turn on your speakers! A sound will be heard " +
					"and the ready button below will be enabled " +
					"when enough users join for a game to start." +
					"</html>"
					);
			readyInfo.setMaximumSize(new Dimension(160, 200));
			
			joinCheckBox = new JCheckBox("I'm Ready!");
			
			joinCheckBox.addItemListener(gc);
			
			infoPanel.add(currentUsers);
			infoPanel.add(requiredUsers);
			infoPanel.add(readyUsers);
			infoPanel.add(Box.createVerticalGlue());
			infoPanel.add(totalUsers);
			infoPanel.add(totalGames);
			infoPanel.add(Box.createVerticalGlue());
			infoPanel.add(readyInfo);
			infoPanel.add(Box.createVerticalGlue());
			infoPanel.add(joinCheckBox);
		}
		
		// Arrange high level
		add(listPanel);
		add(infoPanel);
		
		setInitialValues();
	}

	private void setInitialValues() {
		serverMessage.setText(serverMsgText);
		
		currentUsers.setText(LobbyUsersText);
		requiredUsers.setText(requiredUsersText);
		readyUsers.setText(readyUsersText);
		
		totalUsers.setText(totalUsersText);
		totalGames.setText(totalGamesText);
		
		joinCheckBox.setSelected(false);
		joinCheckBox.setEnabled(false);
	}

	public void setUserCount(int users) {
		currentUsers.setText(LobbyUsersText + users);
	}
	
	public String getStatusMsg() {		
		return statusMsg.getText();
	}
	
	public void setJoinEnabled(final boolean b) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				if( !b ) joinCheckBox.setSelected(false);
				joinCheckBox.setEnabled(b);
			}	
		});
	}

	public void clearModel() {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Set initial label values and clear what's in the model 
				setInitialValues();				
				listModel.updateModel(new TreeSet<UserStatus>());
			}			
		});		
	}

	/**
	 * Called from a non-swing thread
	 * @param lup
	 */
	public void updateModel(final LobbyUpdateResp lup) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				// Update server message
				serverMessage.setText(serverMsgText + lup.serverMessage); 
				
				// Update user count
				currentUsers.setText(LobbyUsersText + lup.userList.size());				
				// Set required users
				requiredUsers.setText(requiredUsersText + lup.usersNeeded);
								
				totalUsers.setText(totalUsersText + lup.totalConnections);
				totalGames.setText(totalGamesText + lup.gamesInProgress);
				
				// Set ready users					
				int ready = 0;
				for( UserStatus u : lup.userList ) if (u.isReadyLobby) ready++;
				readyUsers.setText(readyUsersText + ready); 
				
				// Update checkbox - disable if people left								
				joinCheckBox.setEnabled(lup.joinEnabled);
				if( lup.joinEnabled ) { 
					// Beep!					
					Toolkit.getDefaultToolkit().beep();
				}
				else {
					joinCheckBox.setSelected(false);									
				}
				
				// Update users list
				listModel.updateModel( lup.userList );
			}			
		});		
	}

	private class LobbyCellRenderer extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = -5549036239396720773L;	
			
		@Override
		public Component getListCellRendererComponent(JList list, Object value,
				int index, boolean isSelected, boolean cellHasFocus) {
			UserStatus u = (UserStatus) value;
			
			if ( gc.client.getHITId().equals( u.sessionID ) ) {
				// Myself is the one selected
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			} else {
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			
			Boolean b = u.isReadyLobby; 
			if( b != null) setIcon( b == true ? ready : notReady );
			setText( u.userName + " - " + u.message);			
			
			setEnabled(true); // not list.isEnabled()); otherwise the icon won't draw
			setFont(list.getFont());
			setOpaque(true);
			
			return (JLabel) this;
		}		
	}
}
