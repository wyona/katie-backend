package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Message {

    private String message;
    private String messageId;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Message() {
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
     * @param id Foreign message id
     */
    public void setMessageId(String id) {
        this.messageId = id;
    }

    /**
     * @return foreign message id
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return "Message: " + message;
    }
}
