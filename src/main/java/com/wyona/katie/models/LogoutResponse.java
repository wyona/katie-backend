package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class LogoutResponse {

    private String message;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public LogoutResponse() {
    }

    /**
     *
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     *
     */
    public String getMessage() {
        return message;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return "Logout response message: " + message;
    }
}
