package com.wyona.katie.models;

import io.swagger.models.auth.In;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 *
 */
@Slf4j
public class HumanPreferenceMeta {

    private Integer rating;
    private Long epochTime;
    private String questionUuid;
    private String qnaUuid;
    private String humanFeedback;
    private String userEmail;
    private String clientMessageId;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public HumanPreferenceMeta() {
        this.rating = null;
        this.epochTime = null;
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
}
