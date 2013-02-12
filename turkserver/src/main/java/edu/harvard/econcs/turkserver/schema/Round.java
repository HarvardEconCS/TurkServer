package edu.harvard.econcs.turkserver.schema;


/**
 * Round is a Querydsl bean type
 */
public class Round {

    private java.sql.Timestamp endTime;

    private String experimentId;

    private String inputdata;

    private String results;

    private Integer roundnum;

    private java.sql.Timestamp startTime;

    public java.sql.Timestamp getEndTime() {
        return endTime;
    }

    public void setEndTime(java.sql.Timestamp endTime) {
        this.endTime = endTime;
    }

    public String getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(String experimentId) {
        this.experimentId = experimentId;
    }

    public String getInputdata() {
        return inputdata;
    }

    public void setInputdata(String inputdata) {
        this.inputdata = inputdata;
    }

    public String getResults() {
        return results;
    }

    public void setResults(String results) {
        this.results = results;
    }

    public Integer getRoundnum() {
        return roundnum;
    }

    public void setRoundnum(Integer roundnum) {
        this.roundnum = roundnum;
    }

    public java.sql.Timestamp getStartTime() {
        return startTime;
    }

    public void setStartTime(java.sql.Timestamp startTime) {
        this.startTime = startTime;
    }

}

