package com.wyona.katie.models.squad;
  
import java.io.Serializable;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class QnAs implements Serializable {

    private String question;
    private String id;
    private ArrayList<Answer> answers;
    private ArrayList<Answer> plausible_answers;
    boolean is_impossible;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public QnAs() {
        answers = new ArrayList<Answer>();
        plausible_answers = new ArrayList<Answer>();
        is_impossible = false;
    }

    /**
     * @param question Question, e.g. "What was Frédéric's nationalities?"
     * @param id Id, e.g. "56cbd2356d243a140015ed66"
     */
    public QnAs(String question, String id, boolean is_impossible) {
        this.question = question;
        this.id = id;
        answers = new ArrayList<Answer>();
        plausible_answers = new ArrayList<Answer>();
        this.is_impossible = is_impossible;
    }

    /**
     *
     */
    public void setQuestion(String question) {
        this.question = question;
    }

    /**
     *
     */
    public String getQuestion() {
        return question;
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
     * @param is_impossible When true, then it means there is no answer to the question
     */
    public void setIs_impossible(boolean is_impossible) {
        this.is_impossible = is_impossible;
    }

    /**
     * @return true when there is no answer to the question
     */
    public boolean getIs_imposssible() {
        return is_impossible;
    }

    /**
     *
     */
    public Answer[] getAnswers() {
        return answers.toArray(new Answer[0]);
    }

    /**
     *
     */
    public Answer[] getPlausible_answers() {
        return plausible_answers.toArray(new Answer[0]);
    }
}
