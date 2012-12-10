package edu.harvard.econcs.turkserver.server;

import edu.harvard.econcs.turkserver.schema.Session;

public class SessionRecord {

	public enum SessionStatus { UNASSIGNED, ASSIGNED, LOBBY, EXPERIMENT, COMPLETED };
	
	public static SessionStatus status(Session session) {
		
		// What's the status of this session?
		if( session.getInactivePercent() != null ) {
			// Check that inactivePercent has been set
			// TODO in the future maybe link this up with experiment end time
			return SessionStatus.COMPLETED ;
		} 
		else if( session.getExperimentId() != null ) {
			return SessionStatus.EXPERIMENT;
		}
		else if( session.getLobbyTime() != null ) {
			return SessionStatus.LOBBY;
		}
		else if( session.getWorkerId() != null ) {
			return SessionStatus.ASSIGNED;
		}
		else {
			return SessionStatus.UNASSIGNED;
		}	
	}
	
}
