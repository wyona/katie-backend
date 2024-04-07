package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 *
 */
@Slf4j
public class HumanPreferenceLabel {

    public final static String TEXT_FIELD = "text";
    public final static String LABEL_FIELD = "term";
    public final static String CHOSEN_LABEL_FIELD = "chosenLabel";
    public final static String REJECTED_LABEL_FIELD = "rejectedLabel";

    private String text;
    private Classification chosenLabel;
    private Classification rejectedLabel;

    private HumanPreferenceMeta meta;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public HumanPreferenceLabel() {
        this.text = null;
        this.chosenLabel = null;
        this.rejectedLabel = null;
        this.meta = null;
    }

    /**
     *
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     *
     */
    public String getText() {
        return text;
    }

    /**
     *
     */
    public void setChosenLabel(Classification chosenLabel) {
        this.chosenLabel = chosenLabel;
    }

    /**
     *
     */
    public Classification getChosenLabel() {
        return chosenLabel;
    }

    /**
     *
     */
    public void setRejectedLabel(Classification rejectedLabel) {
        this.rejectedLabel = rejectedLabel;
    }

    /**
     *
     */
    public Classification getRejectedLabel() {
        return rejectedLabel;
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
