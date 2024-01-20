package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Slf4j
public class BackgroundProcess {

    private String id;
    private String userId;
    private String description;

    List<String> statusDescriptions = new ArrayList<String>();

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public BackgroundProcess() {
    }

    /**
     * @param id Process Id
     */
    public BackgroundProcess(String id) {
        this.id = id;
    }

    /**
     *
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     */
    public String getId() {
        return id;
    }

    /**
     *
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     *
     */
    public String getUserId() {
        return userId;
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
    public void addStatusDescription(String description) {
        statusDescriptions.add(description);
    }

    /**
     *
     */
    public String[] getStatusDescriptions() {
        return statusDescriptions.toArray(new String[0]);
    }
}
