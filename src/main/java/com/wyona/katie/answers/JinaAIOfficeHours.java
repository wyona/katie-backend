package com.wyona.katie.answers;

/**
 *
 */
public class JinaAIOfficeHours {

    /**
     *
     */
    public JinaAIOfficeHours() {
    }

    /**
     * @return next office hours, e.g. "Tuesday May 17 at 4pm CEST"
     */
    public String getNextOfficeHours() {
        java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        //return dateFormat.format(new java.util.Date());
        // TODO: Parse and extract next office hours from https://www.meetup.com/jina-community-meetup/
        return "Tuesday May 17 at 4pm CEST";
    }
}
