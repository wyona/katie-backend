package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class HumanPreferenceMeta {

    public final static String HUMAN_FEEDBACK = "humanFeedback";
    public final static String CLIENT_MESSAGE_ID = "clientMessageId";
    public final static String REQUEST_UUID = "requestUuid";

    private String id;
    private Integer rating;
    private Long epochTime;
    private String questionUuid;
    private String requestUuid;
    private String qnaUuid;
    private String humanFeedback;
    private String userEmail;
    private String clientMessageId;
    private Boolean approved;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public HumanPreferenceMeta() {
        this.rating = null;
        this.epochTime = null;
        this.approved = false;
    }

    /**
     * @param id Id of rating / human feedback
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return id of rating / human feedback
     */
    public String getId() {
        return id;
    }

    /**
     *
     */
    public void setRating(Integer rating) {
        this.rating = rating;
    }

    /**
     *
     */
    public Integer getRating() {
        return rating;
    }

    /**
     *
     */
    public void setEpochTime(Long epochTime) {
        this.epochTime = epochTime;
    }

    /**
     *
     */
    public Long getEpochTime() {
        return epochTime;
    }

    /**
     *
     */
    public void setRequestUuid(String requestUuid) {
        this.requestUuid = requestUuid;
    }

    /**
     *
     */
    public String getRequestUuid() {
        return requestUuid;
    }

    /**
     *
     */
    public void setQuestionUuid(String questionUuid) {
        this.questionUuid = questionUuid;
    }

    /**
     *
     */
    public String getQuestionUuid() {
        return questionUuid;
    }

    /**
     *
     */
    public void setQnaUuid(String qnaUuid) {
        this.qnaUuid = qnaUuid;
    }

    /**
     *
     */
    public String getQnaUuid() {
        return qnaUuid;
    }

    /**
     *
     */
    public void setHumanFeedback(String humanFeedback) {
        this.humanFeedback = humanFeedback;
    }

    /**
     *
     */
    public String getHumanFeedback() {
        return humanFeedback;
    }

    /**
     *
     */
    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    /**
     *
     */
    public String getUserEmail() {
        return userEmail;
    }

    /**
     *
     */
    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }

    /**
     *
     */
    public String getClientMessageId() {
        return clientMessageId;
    }

    /**
     * @return true when label rating was approved by another expert, otherwise return false
     */
    public Boolean getApproved() {
        return approved;
    }

    /**
     * @param approved True when label rating was approved by another expert, otherwise set to false
     */
    public void setApproved(Boolean approved) {
        this.approved = approved;
    }
}
