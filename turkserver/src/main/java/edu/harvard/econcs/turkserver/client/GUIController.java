package edu.harvard.econcs.turkserver.client;

import edu.harvard.econcs.turkserver.QuizMaterials;
import edu.harvard.econcs.turkserver.QuizResults;
import edu.harvard.econcs.turkserver.api.ClientLobbyController;

import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public abstract class GUIController implements ItemListener, ActionListener {
	
	private enum Activity { ACTIVE, INACTIVE };
	
	protected final ClientLobbyController clientCont;
	private final Container content;
	private final Container host;
	
	protected Lobby lobby;
	
	// This should always be at the bottom of the screen
	protected StatusBar statusBar;
	
	// Blank screen messages
	protected JPanel blankPanel;
	protected JLabel blankLabel;
	
	private volatile Activity activeStatus;
	
	/**
	 * 
	 * @param client
	 * @param content
	 * @param host the top level container (JFrame or Applet)
	 */
	protected GUIController(ClientLobbyController client, Container content, Container host) {
		this.clientCont = client;
		this.content = content;
		this.host = host;
		
		content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));						
		
		// Create lobby
		lobby = new Lobby(this);
		
		// Create status bar
		statusBar = new StatusBar("Initializing...");
		
		// Set to active by default, timers will start on inactivity
		activeStatus = Activity.ACTIVE;
		
		// Create blank screen stuff
		blankPanel = new JPanel();
		blankLabel = new JLabel();		
		blankPanel.add(blankLabel);
	}	
	
	/**
	 * Dummy constructor for gc that does nothing
	 * @param client
	 */
	public GUIController(ClientLobbyController client) {
		this.clientCont = client;
		content = null;
		host = null;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		// From the checkbox
		if( e.getStateChange() == ItemEvent.SELECTED ) {
			clientCont.updateLobbyReadiness(true);
		} 
		else {
			clientCont.updateLobbyReadiness(false);
		}				
	}	

	@Override
	public void actionPerformed(ActionEvent e) {		
		if( Lobby.updateStatusCmd.equals(e.getActionCommand()) ) {
			clientCont.updateLobbyStatus(lobby.getStatusMsg());
		}
		else {
			System.out.println("Unrecognized command: " + e.toString());
		}
	}

	public Lobby getLobby() { 
		return lobby; 
	}
	
	public Container getContent() {
		return content;
	}
	
	/**
	 * Gets the top level container
	 * @return
	 */
	public Container getHost() {
		return host;
	}

	/**
	 * These two methods should be called from the same thread
	 * @param millis
	 */
	public void recordInactivity(long millis) {
		clientCont.recordInactivity(millis);
		
		if( activeStatus == Activity.ACTIVE ) {
			blankRedraw(
					"You seem to be inactive. Move the mouse to return to the game.");
			activeStatus = Activity.INACTIVE;
			setStatus("Inactive - move mouse or press any key");
		}		
		
	}

	/**
	 * These two methods should be called from the same thread
	 */
	public void clearInactivity() {		
		if( activeStatus == Activity.INACTIVE ) {
			experimentRedraw();
			activeStatus = Activity.ACTIVE;
			setStatus("Resumed");
		}									
	}

	/**
	 * Pops a modal dialog that waits for input.
	 * @param prompt
	 * @return
	 */
	public String questionMsg(final String prompt) {
		/* TODO fix this really lame semantics that 
		 * breaks when multiple windows pop up
		 */
		final BlockingQueue<String> sem = new LinkedBlockingQueue<String>(1);
		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {				
				String resp = JOptionPane.showInternalInputDialog(content, prompt);
				// Have to pass something
				sem.offer(resp == null ? "" : resp);
				System.out.println("Got " + resp);
			}			
		});	
		
		String ret = null;				
		while( ret == null ) {
			// Wait for response
			try {
				ret = sem.take();
			} catch (InterruptedException e) {			
				e.printStackTrace();
			}
		}
		
		return ret;
	}
	
	public void popMsg(final String str) {		
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {				
				JOptionPane.showInternalMessageDialog(content, str);
			}			
		});		
	}

	public void popMsg(final String str, final String title, final int messageType) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {				
				JOptionPane.showInternalMessageDialog(content, str, title, messageType);
			}			
		});		
	}
	
	public void setStatus(final String status) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				statusBar.setMessage(status);
			}			
		});
	}
	
	protected abstract void addExperimentComponents();
	
	/**
	 * Draws a blank screen with the status bar
	 */
	public void blankRedraw(final String message) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				content.removeAll();				
				content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
				
				blankLabel.setText(message);
				content.add(blankPanel);
				content.add(statusBar);
				
				// Redraw everything including the label				
				content.validate();				
			}
		});	
	}
	
	/**
	 * Draws the lobby
	 */
	public void lobbyRedraw() {
		lobby.clearModel();
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {				
				content.removeAll();
				content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
				
				content.add(lobby);
				content.add(statusBar);	
				
				content.validate();
			}
		});
	}

	/**
	 * Draws the experiment
	 */
	public void experimentRedraw() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				content.removeAll();
				content.setLayout(new BoxLayout(content, BoxLayout.PAGE_AXIS));
				
				addExperimentComponents();
				content.add(statusBar);
								
				content.repaint();
			}
		});	
	}

	/**
	 * Applies the quiz to the user and gets the results
	 * Blocks until quiz is finished
	 * @param qm
	 * @return
	 */
	public abstract QuizResults doQuiz(QuizMaterials qm);
}
