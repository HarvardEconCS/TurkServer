/**
 * 
 */
package edu.harvard.econcs.turkserver.server.mysql;

import edu.harvard.econcs.turkserver.server.SessionRecord;
import edu.harvard.econcs.turkserver.server.SessionRecord.SessionStatus;

import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.commons.dbutils.handlers.AbstractListHandler;

/**
 * @author mao
 *
 */
public class SessionRecordListHandler extends AbstractListHandler<SessionRecord> {

	@Override
	protected SessionRecord handleRow(ResultSet rs) throws SQLException {
		SessionRecord sr = new SessionRecord();		
				
		BigInteger id = null;
		try { id = new BigInteger(rs.getString("id"), 16); }
		catch( SQLException e ) {}
		
		String setId = rs.getString("setId");
		
		String data = null;
		try { data = rs.getString("data"); }
		catch (SQLException e) {}
		
		String hitId = rs.getString("hitId");
		String assignmentId = rs.getString("assignmentId");
		String workerId = rs.getString("workerId");
		
		String username = null;
		try { username = rs.getString("username"); }
		catch (SQLException e) {}
		
		String experimentId = null;
		try{ experimentId = rs.getString("experimentId"); }
		catch (SQLException e) {}
		
		double inactivePercent = rs.getDouble("inactivePercent");		
		boolean inactiveWasNull = rs.wasNull();
		
		sr.setId(id);
		sr.setSetId(setId);
		sr.setData(data);
		sr.setHitId(hitId);
		sr.setAssignmentId(assignmentId);
		sr.setWorkerId(workerId);		
		
		sr.setUsername(username);
		
		sr.setExperimentId(experimentId);
		
		sr.setInactivePercent(inactivePercent);
		
		// What's the status of this session?
		// TODO deal with EXPIRED sessions
		if( !inactiveWasNull ) {
			// Check that inactivePercent has been set
			// TODO in the future maybe link this up with experiment end time
			sr.setStatus(SessionStatus.COMPLETED);
		} 
		else if( experimentId != null ) {
			sr.setStatus(SessionStatus.EXPERIMENT);
		}
		else if( workerId != null ) {
			sr.setStatus(SessionStatus.LOBBY);
		}
		else {
			sr.setStatus(SessionStatus.UNASSIGNED);
		}		
		
		return sr;
	}
	

}
