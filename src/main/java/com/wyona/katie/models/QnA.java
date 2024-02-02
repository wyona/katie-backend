package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.List;

/**
 * QnA
 * TODO: Improve naming and usage of QnA.java and Answer.java
 */
public class QnA {

    private String uuid;
    private String question;
    private List<String> alternativeQuestions;
    private String answer;
    private String answerClientSideEncryptionAlgorithm;
    private List<String> classifications;
    private String url;

    /**
     *
     */
    public QnA() {
        this.uuid = null;
        this.question = null;
        this.alternativeQuestions = new ArrayList<String>();
        this.answer = null;
        this.answerClientSideEncryptionAlgorithm = null;
        this.classifications = new ArrayList<String>();
        this.url = null;
    }

    /**
     *
     */
    public QnA(Answer answer) {
        this.uuid = answer.getUuid();
        this.question = answer.getOriginalquestion();

        this.alternativeQuestions = new ArrayList<String>();
        for (String q: answer.getAlternativequestions()) {
            this.alternativeQuestions.add(q);
        }

        this.answer = answer.getAnswer();

        this.classifications = answer.getClassifications();

        this.answerClientSideEncryptionAlgorithm = answer.getAnswerClientSideEncryptionAlgorithm();

        this.url = answer.getUrl();
    }

    /**
     *
     */
    public String getUuid() {
        return uuid;
    }

    /**
     *
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
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
    public void setQuestion(String question) {
        this.question = question;
    }

    /**
     *
     */
    public String[] getAlternativeQuestions() {
        return alternativeQuestions.toArray(new String[0]);
    }

    /**
     *
     */
    public String getAnswer() {
        return answer;
    }

    /**
     *
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    /**
     * Get classifications
     */
    public List<String> getClassifications() {
        return classifications;
    }

    /**
     * @return Client side encryption algorithm, e.g. "aes-256"
     */
    public String getAnswerClientSideEncryptionAlgorithm() {
        return answerClientSideEncryptionAlgorithm;
    }

    /**
     *
     */
    public String getUrl() {
        return url;
    }

    /**
     *
     */
    public void setUrl(String url) {
        this.url = url;
    }
}
