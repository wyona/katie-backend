package com.wyona.katie.answers;

/**
 *
 */
public class Time {

    /**
     *
     */
    public Time() {
    }

    /**
     *
     */
    public String getTime() {
        java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        return dateFormat.format(new java.util.Date());
    }
}
