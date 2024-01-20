package com.wyona.katie.models.insights;

import java.util.Date;

/**
 * Insight event
 */
public class Event {

    private String type;
    private Date timestamp;

    /**
     *
     */
    public Event(String type, Date timestamp) {
        this.type = type;
        this.timestamp = timestamp;
    }

    /**
     *
     */
    public String getType() {
        return type;
    }

    /**
     *
     */
    public Date getTimestamp() {
        return timestamp;
    }
}
