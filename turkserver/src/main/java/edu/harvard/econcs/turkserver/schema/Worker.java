package edu.harvard.econcs.turkserver.schema;


/**
 * Worker is a Querydsl bean type
 */
public class Worker {

    private String id;

    private String notify;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNotify() {
        return notify;
    }

    public void setNotify(String notify) {
        this.notify = notify;
    }

}

