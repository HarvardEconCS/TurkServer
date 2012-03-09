package edu.harvard.econcs.turkserver.mturk;

import java.util.Date;

/**
 * A bean containing information about a HIT.
 * @author mao
 *
 */
public class HIT {
	
	private String hitId;
	private String hitTypeId;
	private Date creationTime;
	private String title;
	private String description;
	private String keywords;
	private String hitStatus;	
	private int maxAssignments;
	
	private String rewardInUSD;
	private int autoApprovalDelayInSeconds;
	private Date expiration;
	private int assignmentDurationInSeconds;
	private int numAssignmentsPending;
	private int numAssignmentsAvailable;
	private int numAssignmentsCompleted;
	
	public String toString() { return "HIT " + hitId; }
		
	public String getHitId() {
		return hitId;
	}
	
	public void setHitId(String hitId) {
		this.hitId = hitId;
	}
	
	public String getHitTypeId() {
		return hitTypeId;
	}
	
	public void setHitTypeId(String hitTypeId) {
		this.hitTypeId = hitTypeId;
	}
	
	public Date getCreationTime() {
		return creationTime;
	}
	
	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getDescription() {
		return description;
	}
	
	public void setDescription(String description) {
		this.description = description;
	}
	
	public String getKeywords() {
		return keywords;
	}
	
	public void setKeywords(String keywords) {
		this.keywords = keywords;
	}
	
	public String getHitStatus() {
		return hitStatus;
	}
	
	public void setHitStatus(String hitStatus) {
		this.hitStatus = hitStatus;
	}
	
	public int getMaxAssignments() {
		return maxAssignments;
	}
	
	public void setMaxAssignments(int maxAssignments) {
		this.maxAssignments = maxAssignments;
	}

	public String getRewardInUSD() {
		return rewardInUSD;
	}

	public void setRewardInUSD(String rewardInUSD) {
		this.rewardInUSD = rewardInUSD;
	}
	
	public int getAutoApprovalDelayInSeconds() {
		return autoApprovalDelayInSeconds;
	}
	
	public void setAutoApprovalDelayInSeconds(int autoApprovalDelayInSeconds) {
		this.autoApprovalDelayInSeconds = autoApprovalDelayInSeconds;
	}
	
	public Date getExpiration() {
		return expiration;
	}
	
	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}
	
	public int getAssignmentDurationInSeconds() {
		return assignmentDurationInSeconds;
	}
	
	public void setAssignmentDurationInSeconds(int assignmentDurationInSeconds) {
		this.assignmentDurationInSeconds = assignmentDurationInSeconds;
	}
	
	public int getNumAssignmentsPending() {
		return numAssignmentsPending;
	}
	
	public void setNumAssignmentsPending(int numAssignmentsPending) {
		this.numAssignmentsPending = numAssignmentsPending;
	}
	
	public int getNumAssignmentsAvailable() {
		return numAssignmentsAvailable;
	}
	
	public void setNumAssignmentsAvailable(int numAssignmentsAvailable) {
		this.numAssignmentsAvailable = numAssignmentsAvailable;
	}
	
	public int getNumAssignmentsCompleted() {
		return numAssignmentsCompleted;
	}
	
	public void setNumAssignmentsCompleted(int numAssignmentsCompleted) {
		this.numAssignmentsCompleted = numAssignmentsCompleted;
	}	
	
}
