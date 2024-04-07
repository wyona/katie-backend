package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 * Rating of predicted labels
 */
@Slf4j
public class RatingPredictedLabels {

    private String domainId;
    private String requestUuid;
    private Integer rank;
    private String feedback;
    private String email;
    private String bestFittingLabelId;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public RatingPredictedLabels() {
        this.domainId = null;
        this.requestUuid = null;
        this.rank = -1;
        this.feedback = null;
        this.email = null;
        this.bestFittingLabelId = null;
    }

    /**
     *
     */
    public void setDomainid(String domainId) {
        this.domainId = domainId;
    }

    /**
     *
     */
    public String getDomainid() {
        return domainId;
    }

    /**
     * @param email E-Mail address of user rating answer
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return E-Mail address of user rating answer
     */
    public String getEmail() {
        return this.email;
    }

    /**
     * Set UUID of request to get predicted labels
     */
    public void setRequestuuid(String uuid) {
        this.requestUuid = uuid;
    }

    /**
     * @return UUID of request to get predicted labels
     */
    public String getRequestuuid() {
        return requestUuid;
    }

    /**
     * Set result rank, e.g. 0 means top result (positive) and -1 means not part of result set (negative)
     */
    public void setRank(Integer rank) {
        this.rank = rank;
    }

    /**
     * @return rank of predicted label, e.g. 0 means top result (positive) and -1 means not part of result set (negative)
     */
    public Integer getRank() {
        return rank;
    }

    /**
     * Set user feedback, e.g. "Levi Brucker is not Levi Strauss"
     */
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    /**
     * Get user feedback, e.g. "Label gravel-bike would be correct"
     */
    public String getFeedback() {
        return feedback;
    }

    /**
     *
     */
    public void setBestFittingLabelId(String bestFittingLabelId) {
        this.bestFittingLabelId = bestFittingLabelId;
    }

    /**
     *
     */
    public String getBestFittingLabelId() {
        return bestFittingLabelId;
    }
}
