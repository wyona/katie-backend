package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class GmailConfiguration {

    private String credentialsPath;
    private String username;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public GmailConfiguration() {
    }

    /**
     * @param credentialsPath Path to credentials of service acccount, e.g. "google/katie-360121-d549df70ff6c.json"
     * @param username Impersonated username, e.g. "michael.wechner@ukatie.com" or "me"
     */
    public GmailConfiguration(String credentialsPath, String username) {
        this.credentialsPath = credentialsPath;
        this.username = username;
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
    public String getCredentialsPath() {
        return credentialsPath;
    }
}
