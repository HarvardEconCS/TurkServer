package edu.harvard.econcs.turkserver.client;

import java.rmi.RemoteException;

import javax.swing.JOptionPane;

import edu.harvard.econcs.turkserver.Codec;
import edu.harvard.econcs.turkserver.LobbyUpdateResp;
import edu.harvard.econcs.turkserver.Messages;
import edu.harvard.econcs.turkserver.QuizFailException;
import edu.harvard.econcs.turkserver.QuizMaterials;
import edu.harvard.econcs.turkserver.SessionCompletedException;
import edu.harvard.econcs.turkserver.SessionExpiredException;
import edu.harvard.econcs.turkserver.SessionOverlapException;
import edu.harvard.econcs.turkserver.SessionUnknownException;
import edu.harvard.econcs.turkserver.SimultaneousSessionsException;
import edu.harvard.econcs.turkserver.TooManyFailsException;
import edu.harvard.econcs.turkserver.TooManySessionsException;
import edu.harvard.econcs.turkserver.api.ClientError;
import edu.harvard.econcs.turkserver.api.ClientLobbyController;
import edu.harvard.econcs.turkserver.api.ExperimentClient;
import edu.harvard.econcs.turkserver.api.FinishExperiment;
import edu.harvard.econcs.turkserver.api.JoinLobby;
import edu.harvard.econcs.turkserver.api.QuizAttempt;
import edu.harvard.econcs.turkserver.api.RequestUsername;
import edu.harvard.econcs.turkserver.api.StartExperiment;
import edu.harvard.econcs.turkserver.api.UpdateLobby;

/**
 * Base GUI client for old stuff such as graph coloring.
 * TODO needs to be rethought. 
 * 
 * @author mao
 *
 */
@ExperimentClient
public abstract class AbstractGraphicalClient<GC extends GUIController> {

	protected ClientLobbyController cont;
	protected GC gc;
	
	public AbstractGraphicalClient(ClientLobbyController cont) {
		this.cont = cont;
	}
		
	public void setGC(GC gc) {
		this.gc = gc;
	}
	
	@QuizAttempt
	public void takeQuiz(QuizMaterials qm) {
		QuizResults qr = gc.doQuiz(qm);
		
		cont.sendQuizResults(qr);
	}
	
	@RequestUsername
	public void requestUsername() {
		gc.setStatus("Waiting for nickname...");

		String username = null;
		do {
			username = gc.questionMsg("Enter a nickname for yourself:");
		} while ( "".equals(username) );

		gc.setStatus("Sending nickname to server...");
		
		cont.sendUsername(username);
	}
	
	@JoinLobby
	public void joinLobby() {
		gc.setStatus("Connected to lobby");
	}
	
	@UpdateLobby
	public void updateLobby(Object data) {
		// TODO update the format of this for new data format
		LobbyUpdateResp lup = (LobbyUpdateResp) data;
		gc.getLobby().updateModel(lup);
		gc.setStatus(lup.joinEnabled 
				? StatusBar.lobbyReadyMsg 
						: StatusBar.lobbyWaitingMsg );		
	}
	
	@StartExperiment
	public void startExperiment() {
		gc.setStatus("Connecting to game");
		
		gc.setStatus(StatusBar.enteringExpMsg);
		gc.experimentRedraw();
	}
	
	@FinishExperiment
	public void finishExperiment() {
		gc.setStatus(StatusBar.finishedExpMsg);						
	}
	
	@ClientError
	public void gotError(String msg) {
		if( Codec.status_expfinished.equals(msg) ) {
			// Connected to exp that is already finished. Enable the submit.
			// TODO fix this if want to allow view graph for finished exp
			gc.blankRedraw("Experiment already finished");
			gc.setStatus("Experiment already finished.");
			gc.popMsg("This experiment is already finished.\n" +
					"Please submit the HIT below.");
			enableSubmit();
		}
		else if( Codec.startExpError.equals(msg) ) {
			// Server unilaterally disabled due to error, disable join button
			gc.getLobby().setJoinEnabled(false);
			gc.popMsg("Unable to start experiment!" +
					"There might be a problem with the server." +
					"You may try again, or just return the HIT.",
					"Internal Server Error", JOptionPane.ERROR_MESSAGE);
		}
		else if( Codec.status_batchfinished.equals(msg) ) {
			gc.blankRedraw("All games finished");
			gc.setStatus(StatusBar.batchFinishedMsg);
			gc.popMsg("All games for this batch have been completed.\n" +
					"If you have signed up for notifications, we will let you know\n" +
					"when we post more games. Please return the HIT.",
					"Game Batch Completed", JOptionPane.WARNING_MESSAGE);
		}
		
		// TODO connect/handshake error
		gc.blankRedraw("Unable to connect");
		gc.setStatus("Unable to connect to server");
		gc.popMsg(Messages.CONNECT_ERROR, "Connection Error", JOptionPane.ERROR_MESSAGE);
		
		// TODO if the server disconnects
		gc.blankRedraw("Server disconnected");
		gc.setStatus("Server disconnected");
		gc.popMsg(Messages.SERVER_DISCONNECTED,	"Server Disconnected", JOptionPane.WARNING_MESSAGE);
		
		// TODO arbitrary errors
		gc.blankRedraw("Connection Error");
		gc.setStatus("Connection Error.");
		
		gc.popMsg("There was an error connecting to the server.\n" +
				"Please refresh the page to try again, or return the HIT.");
		
		// Random error messages for later refactoring

		// Get the cause of the server-side Remote Exception
		Throwable t = new RemoteException().getCause().getCause();		
		gc.blankRedraw("Authentication error");
		gc.setStatus("Session authentication error");

		if( t instanceof SessionUnknownException ) {
			gc.popMsg(Messages.UNRECOGNIZED_SESSION, "Unrecognized Session ID", JOptionPane.ERROR_MESSAGE);
		}
		else if (t instanceof SimultaneousSessionsException) {
			gc.popMsg(Messages.SIMULTANEOUS_SESSIONS, "Too Many Games Open", JOptionPane.WARNING_MESSAGE);
		}
		else if (t instanceof SessionExpiredException ) {
			// TODO this message is the same as above, except on connect
			gc.popMsg(Messages.EXPIRED_SESSION,	"Game Batch Completed", JOptionPane.INFORMATION_MESSAGE);
		}
		else if (t instanceof TooManySessionsException) {
			gc.popMsg(Messages.TOO_MANY_SESSIONS, "Game Limit Hit", JOptionPane.INFORMATION_MESSAGE);
		}
		else if (t instanceof SessionOverlapException ) {
			gc.popMsg(Messages.SESSION_OVERLAP, "Session Already Used", JOptionPane.WARNING_MESSAGE);
		}
		else if (t instanceof SessionCompletedException) {
			gc.popMsg(Messages.SESSION_COMPLETED, "Session Already Completed", JOptionPane.WARNING_MESSAGE);				
			gc.setStatus("Experiment already finished!");				
			enableSubmit();
		} 
		else if (t instanceof QuizFailException) {
			gc.popMsg(Messages.QUIZ_FAILED, "Quiz Failed!", JOptionPane.WARNING_MESSAGE);
			gc.blankRedraw("Quiz Failed!");
			gc.setStatus("Quiz Failed!");
		}
		else if (t instanceof TooManyFailsException) {
			gc.blankRedraw("Too Many Failed Quizzes!");
			gc.popMsg(Messages.TOO_MANY_FAILS, "Too Many Fails!", JOptionPane.WARNING_MESSAGE);
			gc.setStatus("Too many times failed, please come back later!");
		}
		else {
			t.printStackTrace();
			gc.popMsg("Unknown error, please report this and return the HIT, " +
					"or try a different HIT: \n" + t.toString(),
					"Unknown error", JOptionPane.ERROR_MESSAGE);
		}				

		// After connect to server, Draw the lobby
		gc.lobbyRedraw();
	}

	/**
	 * Enable the HIT to be submitted
	 */
	protected abstract void enableSubmit();
	
}
