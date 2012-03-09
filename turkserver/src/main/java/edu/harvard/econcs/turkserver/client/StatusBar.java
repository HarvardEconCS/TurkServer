/**
 * 
 */
package edu.harvard.econcs.turkserver.client;

import javax.swing.*;
import javax.swing.border.BevelBorder;


/**
 * @author mao
 *
 */
public class StatusBar extends JPanel {
	
	public static final String lobbyWaitingMsg = "Waiting for enough users...";
	public static final String lobbyReadyMsg = "Click ready to participate!";
	
	public static final String enteringExpMsg = "Entering game!";
	public static final String finishedExpMsg = "Game completed!";
	
	public static final String batchFinishedMsg = "No more games available on server";
	
	public static final String returningToLobbyMsg = "Returning to lobby.";
	
	private static final long serialVersionUID = -7113109294122393589L;
		
	
	private JLabel message;	
	
	public StatusBar(String initialText) {
		this.setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));		
		
		message = new JLabel(initialText);
		
		this.add(message);
		this.add(Box.createHorizontalGlue());
	}

	public void setMessage(String msg) {
		message.setText(msg);
	}		
		
}
