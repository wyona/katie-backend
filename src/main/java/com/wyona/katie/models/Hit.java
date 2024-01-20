package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Hit {

    private Answer answer;
    private double score;
    private int rating;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Hit() {
    }

    /**
     * @param answer Answer
     * @param score Confidence score, that answer is right
     */
    public Hit(Answer answer, double score) {
        this.answer = answer;
        this.score = score;
        this.rating = -1;
    }

    /**
     *
     */
    public Answer getAnswer() {
        return answer;
    }

    /**
     *
     */
    public double getScore() {
        return score;
    }

    /**
     * Set human rating
     * @param rating -1: no rating available, 0: completely wrong, 10: completely correct
     */
    public void setRating(int rating) {
        this.rating = rating;
    }

    /**
     * Get human rating
     * @return  -1: no rating available, 0: completely wrong, 10: completely correct
     */
    public int getRating() {
        return rating;
    }
}
