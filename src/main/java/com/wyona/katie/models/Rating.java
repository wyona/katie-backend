package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.util.Date;

/**
 *
 */
@Slf4j
public class Rating {

    private int rating;
    private Date date;
    private String feedback;
    private String userQuestion;
    private String questionUuid;

    private String qnaUuid;
    private String email;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Rating() {
        this.rating = -1;
        this.date = null;
        this.feedback = null;
        this.userQuestion = null;
        this.questionUuid = null;

        this.qnaUuid = null;
        this.email = null;
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
     * Set question asked by user, e.g. "How old is Levi Brucker?"
     */
    public void setUserquestion(String userQuestion) {
        this.userQuestion = userQuestion;
    }

    /**
     * Get question asked by user, e.g. "How old is Levi Brucker?"
     */
    public String getUserquestion() {
        return userQuestion;
    }

    /**
     * Set UUID of asked question
     */
    public void setQuestionuuid(String uuid) {
        this.questionUuid = uuid;
    }

    /**
     * @return UUID of asked question
     */
    public String getQuestionuuid() {
        return questionUuid;
    }

    /**
     * Set UUID of answer / QnA provided to question
     */
    public void setQnauuid(String uuid) {
        this.qnaUuid = uuid;
    }

    /**
     * Get UUID of answer / QnA provided to question
     */
    public String getQnauuid() {
        return this.qnaUuid;
    }

    /**
     * Set user feedback, e.g. "Levi Brucker is not Levi Strauss"
     */
    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    /**
     * Get user feedback, e.g. "Levi Brucker is not Levi Strauss"
     */
    public String getFeedback() {
        return feedback;
    }

    /**
     * Set rating
     * @param rating -1: no rating available, 0: completely wrong, 10: completely correct
     */
    public void setRating(int rating) {
        this.rating = rating;
    }

    /**
     * Get rating
     * @return -1: no rating available, 0: completely wrong, 10: completely correct
     */
    public int getRating() {
        return rating;
    }

    /**
     * @param date Date of rating
     */
    public void setDate(Date date) {
        this.date = date;
    }

    /**
     * @return date of rating
     */
    public Date getDate() {
        return date;
    }
}
