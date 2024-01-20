package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Hostname {

    private String hostname;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Hostname() {
    }

    /**
     * @param hostname Hostname, e.g. "http://127.0.0.1:4200" or "https//katie.qa"
     */
    public Hostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     *
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     *
     */
    public String getHostname() {
        return hostname;
    }
}
