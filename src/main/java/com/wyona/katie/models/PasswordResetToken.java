package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class PasswordResetToken {

    private String password;
    private String resetToken;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public PasswordResetToken() {
    }

    /**
     * @param password New password
     */
    public PasswordResetToken(String password, String resetToken) {
        this.password = password;
        this.resetToken = resetToken;
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
    public void setResetToken(String resetToken) {
        this.resetToken = resetToken;
    }

    /**
     *
     */
    public String getResetToken() {
        return resetToken;
    }
}
