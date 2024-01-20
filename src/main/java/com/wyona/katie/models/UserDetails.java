package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class UserDetails {

    private String username;
    private String[] roles;

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
}
