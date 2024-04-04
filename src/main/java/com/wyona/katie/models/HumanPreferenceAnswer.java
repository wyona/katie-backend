package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class HumanPreferenceAnswer {

    private String humanMessage;
    private String chosenAnswer;
    private String rejectedAnswer;

    private HumanPreferenceMeta meta;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public HumanPreferenceAnswer() {
        this.humanMessage = null;
        this.chosenAnswer = null;
        this.rejectedAnswer = null;
        this.meta = null;
    }

    /**
     *
     */
    public void setHumanMessage(String humanMessage) {
        this.humanMessage = humanMessage;
    }

    /**
     *
     */
    public String getHumanMessage() {
        return humanMessage;
    }

    /**
     *
     */
    public void setChosenAnswer(String chosenAnswer) {
        this.chosenAnswer = chosenAnswer;
    }

    /**
     *
     */
    public String getChosenAnswer() {
        return chosenAnswer;
    }

    /**
     *
     */
    public void setRejectedAnswer(String rejectedAnswer) {
        this.rejectedAnswer = rejectedAnswer;
    }

    /**
     *
     */
    public String getRejectedAnswer() {
        return rejectedAnswer;
    }

    /**
     *
     */
    public void setMeta(HumanPreferenceMeta meta) {
        this.meta = meta;
    }

    /**
     *
     */
    public HumanPreferenceMeta getMeta() {
        return meta;
    }
}
