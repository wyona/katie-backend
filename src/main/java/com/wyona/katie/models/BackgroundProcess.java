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
    private List<BackgroundProcessStatus> statusDescriptions = new ArrayList<>();
    private String status;

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
     * @param statusDescription Status description including status type
     */
    public void addStatusDescription(BackgroundProcessStatus statusDescription) {
        statusDescriptions.add(statusDescription);
    }

    /**
     *
     */
    public BackgroundProcessStatus[] getStatusDescriptions() {
        return statusDescriptions.toArray(new BackgroundProcessStatus[0]);
    }

    /**
     * @param status Current status of background process, either "IN_PROGRESS" or "COMPLETED"
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return current status of background process, either "IN_PROGRESS" or "COMPLETED"
     */
    public String getStatus() {
        return status;
    }
}
