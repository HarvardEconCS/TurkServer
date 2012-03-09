package edu.harvard.econcs.turkserver.server;

import java.math.BigInteger;

public class SessionRecord {

	public enum SessionStatus { UNASSIGNED, ASSIGNED, LOBBY, EXPERIMENT, COMPLETED };
	
	private SessionStatus status;
	
	private BigInteger id;
	private String setId;
	private String data;

	private String hitId;
	private String assignmentId;
	private String workerId;
	
	private String username;
	
	private String experimentId;
	private double inactivePercent;

	public void setStatus(SessionStatus status) {
		this.status = status;
	}
	public SessionStatus getStatus() {
		return status;
	}
	public BigInteger getId() {
		return id;
	}
	public void setId(BigInteger id) {
		this.id = id;
	}
	public String getSetId() {
		return setId;
	}
	public void setSetId(String setId) {
		this.setId = setId;
	}
	public String getData() {
		return data;
	}
	public void setData(String data) {
		this.data = data;		
	}
	public String getHitId() {
		return hitId;
	}
	public void setHitId(String hitId) {
		this.hitId = hitId;
	}
	public String getAssignmentId() {
		return assignmentId;
	}
	public void setAssignmentId(String assignmentId) {
		this.assignmentId = assignmentId;
	}
	public String getWorkerId() {
		return workerId;
	}
	public void setWorkerId(String workerId) {
		this.workerId = workerId;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getExperimentId() {
		return experimentId;
	}
	public void setExperimentId(String experimentId) {
		this.experimentId = experimentId;
	}
	public double getInactivePercent() {
		return inactivePercent;
	}
	public void setInactivePercent(double inactivePercent) {
		this.inactivePercent = inactivePercent;
	}	
	
}
