package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Credentials {

    private String email;
    private String password;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Credentials() {
    }

    /**
     * @param email Email as username
     */
    public Credentials(String email, String password) {
        this.email = email;
        this.password = password;
    }

    /**
     *
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     *
     */
    public String getPassword() {
        return password;
    }

    /**
     *
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     *
     */
    public String getEmail() {
        return email;
    }
}
