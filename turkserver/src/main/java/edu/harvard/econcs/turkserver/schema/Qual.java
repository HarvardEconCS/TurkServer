package edu.harvard.econcs.turkserver.schema;


/**
 * Qual is a Querydsl bean type
 */
public class Qual {

    private String qual;

    private Integer value;

    private String workerId;

    public String getQual() {
        return qual;
    }

    public void setQual(String qual) {
        this.qual = qual;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

}

