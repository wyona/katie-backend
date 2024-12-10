package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class HitLabel {

    private Classification label;
    private double score;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public HitLabel() {
    }

    /**
     * @param label Label / classification
     * @param score Confidence score, that predicted label is accurate, e.g. 0.73694
     */
    public HitLabel(Classification label, double score) {
        this.label = label;
        this.score = score;
    }

    /**
     *
     */
    public Classification getLabel() {
        return label;
    }

    /**
     *
     */
    public double getScore() {
        return score;
    }
}
