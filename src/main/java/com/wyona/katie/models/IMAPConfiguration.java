package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class IMAPConfiguration {

    private String hostname;
    private int port;
    private String username;
    private String password;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public IMAPConfiguration() {
    }

    /**
     *
     */
    public IMAPConfiguration(String hostname, int port, String username, String password) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.password = password;
    }

    /**
     *
     */
    public String getHostname() {
        return hostname;
    }

    /**
     *
     */
    public int getPort() {
        return port;
    }

    /**
     *
     */
    public String getUsername() {
        return username;
    }

    /**
     *
     */
    public String getPassword() {
        return password;
    }
}
