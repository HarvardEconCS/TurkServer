package edu.harvard.econcs.turkserver.schema;


/**
 * Session is a Querydsl bean type
 */
public class Session {

    private String assignmentId;

    private java.math.BigDecimal bonus;

    private Boolean bonusPaid;

    private String comment;

    private String experimentId;

    private String hitId;

    private String hitStatus;

    private String inactiveData;

    private Double inactivePercent;

    private String ipAddr;

    private java.sql.Timestamp lobbyTime;

    private Integer numDisconnects;

    private Boolean paid;

    private java.math.BigDecimal payment;

    private String setId;

    private String username;

    private String workerId;

    public String getAssignmentId() {
        return assignmentId;
    }

    public void setAssignmentId(String assignmentId) {
        this.assignmentId = assignmentId;
    }

    public java.math.BigDecimal getBonus() {
        return bonus;
    }

    public void setBonus(java.math.BigDecimal bonus) {
        this.bonus = bonus;
    }

    public Boolean getBonusPaid() {
        return bonusPaid;
    }

    public void setBonusPaid(Boolean bonusPaid) {
        this.bonusPaid = bonusPaid;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getHitId() {
        return hitId;
    }

    public void setHitId(String hitId) {
        this.hitId = hitId;
    }

    public String getHitStatus() {
        return hitStatus;
    }

    public void setHitStatus(String hitStatus) {
        this.hitStatus = hitStatus;
    }

    public String getInactiveData() {
        return inactiveData;
    }

    public void setInactiveData(String inactiveData) {
        this.inactiveData = inactiveData;
    }

    public Double getInactivePercent() {
        return inactivePercent;
    }

    public void setInactivePercent(Double inactivePercent) {
        this.inactivePercent = inactivePercent;
    }

    public String getIpAddr() {
        return ipAddr;
    }

    public void setIpAddr(String ipAddr) {
        this.ipAddr = ipAddr;
    }

    public java.sql.Timestamp getLobbyTime() {
        return lobbyTime;
    }

    public void setLobbyTime(java.sql.Timestamp lobbyTime) {
        this.lobbyTime = lobbyTime;
    }

    public Integer getNumDisconnects() {
        return numDisconnects;
    }

    public void setNumDisconnects(Integer numDisconnects) {
        this.numDisconnects = numDisconnects;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public java.math.BigDecimal getPayment() {
        return payment;
    }

    public void setPayment(java.math.BigDecimal payment) {
        this.payment = payment;
    }

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

}

