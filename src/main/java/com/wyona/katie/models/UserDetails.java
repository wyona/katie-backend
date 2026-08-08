package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class UserDetails {

    private String username;
    private String[] roles;
    private String accessToken;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public UserDetails() {
    }

    /**
     *
     */
    public UserDetails(String username, String[] roles) {
        this.username = username;
        this.roles = roles;
        this.accessToken = null;
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
    public String[] getRoles() {
        return roles;
    }

    /**
     * Set access token
     */
    public void setToken(String token) {
        this.accessToken = token;
    }

    /**
     * Get access token
     */
    public String getToken() {
        return accessToken;
    }
}
