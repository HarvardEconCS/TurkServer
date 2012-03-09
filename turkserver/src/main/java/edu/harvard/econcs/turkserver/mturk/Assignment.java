package edu.harvard.econcs.turkserver.mturk;

import java.util.Date;

import org.w3c.dom.Document;

public class Assignment {
	
	private String assignmentId;
	private String workerId;
	private String hitId;
	private String assignmentStatus;
	
	private Date autoApprovalTime;
	private Date acceptTime;
	private Date submitTime;
	
	private Document answer;
	
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

	public String getHitId() {
		return hitId;
	}

	public void setHitId(String hitId) {
		this.hitId = hitId;
	}

	public String getAssignmentStatus() {
		return assignmentStatus;
	}

	public void setAssignmentStatus(String assignmentStatus) {
		this.assignmentStatus = assignmentStatus;
	}

	public Date getAutoApprovalTime() {
		return autoApprovalTime;
	}

	public void setAutoApprovalTime(Date autoApprovalTime) {
		this.autoApprovalTime = autoApprovalTime;
	}

	public Date getAcceptTime() {
		return acceptTime;
	}

	public void setAcceptTime(Date acceptTime) {
		this.acceptTime = acceptTime;
	}

	public Date getSubmitTime() {
		return submitTime;
	}

	public void setSubmitTime(Date submitTime) {
		this.submitTime = submitTime;
	}

	public Document getAnswer() {
		return answer;
	}

	public void setAnswer(Document answer) {
		this.answer = answer;
	}	
}
