package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class BackgroundProcessStatus {

    private String description;
    private BackgroundProcessStatusType type;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public BackgroundProcessStatus() {
    }

    /**
     * @param description Status description, e.g. "Text classified"
     */
    public BackgroundProcessStatus(String description, BackgroundProcessStatusType type) {
        this.description = description;
        this.type = type;
    }

    /**
     *
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     *
     */
    public String getDescription() {
        return description;
    }

    /**
     *
     */
    public void setType(BackgroundProcessStatusType type) {
        this.type = type;
    }

    /**
     *
     */
    public BackgroundProcessStatusType getType() {
        return type;
    }
}
