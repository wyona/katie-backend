package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class MessageValue {

    private String message;
    private String betterAnswer;
    private String relevantUrl;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MessageValue() {
    }

    /**
     *
     */
    public MessageValue(String message) {
        this.message = message;
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
    public void setBetteranswer(String betterAnswer) {
        this.betterAnswer = betterAnswer;
    }

    /**
     *
     */
    public String getBetteranswer() {
        return betterAnswer;
    }

    /**
     *
     */
    public void setRelevanturl(String relevantUrl) {
        this.relevantUrl = relevantUrl;
    }

    /**
     *
     */
    public String getRelevanturl() {
        return relevantUrl;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return "Message: " + message;
    }
}