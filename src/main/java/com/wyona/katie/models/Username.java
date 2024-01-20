package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Username {

    private String username;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Username() {
    }

    /**
     *
     */
    public Username(String username) {
        // INFO: Make case-insensitive
        // TODO: Validate username, e.g. length, etc.
        this.username = username.toLowerCase();
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
    @Override
    public String toString() {
        return username;
    }
}
