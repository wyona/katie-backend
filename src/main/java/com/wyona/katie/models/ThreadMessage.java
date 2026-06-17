package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class ThreadMessage {

    private String messages;
    private String messageSeparator;

    private String category;
    private String subcategory;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public ThreadMessage() {
    }

    /**
     * Set thread message(s)
     */
    public void setMessage(String messages) {
        this.messages = messages;
    }

    /**
     * @return thread message(s)
     */
    public String getMessage() {
        return messages;
    }

    /**
     *
     */
    public void setMessageSeparator(String messageSeparator) {
        this.messageSeparator = messageSeparator;
    }

    /**
     *
     */
    public String getMessageSeparator() {
        return messageSeparator;
    }

    /**
     *
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     *
     */
    public String getCategory() {
        return category;
    }

    /**
     *
     */
    public void setSubcategory(String subcategory) {
        this.subcategory = subcategory;
    }

    /**
     *
     */
    public String getSubcategory() {
        return subcategory;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return "Message(s): " + messages;
    }
}
