package edu.harvard.econcs.turkserver.schema;


/**
 * Quiz is a Querydsl bean type
 */
public class Quiz {

    private String answers;

    private Integer id;

    private Integer numCorrect;

    private Integer numTotal;

    private Double score;

    private String sessionId;

    private String setId;

    private String workerId;

    public String getAnswers() {
        return answers;
    }

    public void setAnswers(String answers) {
        this.answers = answers;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getNumCorrect() {
        return numCorrect;
    }

    public void setNumCorrect(Integer numCorrect) {
        this.numCorrect = numCorrect;
    }

    public Integer getNumTotal() {
        return numTotal;
    }

    public void setNumTotal(Integer numTotal) {
        this.numTotal = numTotal;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSetId() {
        return setId;
    }

    public void setSetId(String setId) {
        this.setId = setId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

}

