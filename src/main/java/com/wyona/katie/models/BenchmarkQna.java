package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

public class BenchmarkQna {
    private String uuid;
    private String question;
    //@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> alternativeQuestions;
    private List<BenchmarkQuestion> testQuestions;
    private String answer;

    //@JsonIgnore
    private String answerClientSideEncryptionAlgorithm;
    //@JsonIgnore
    //@JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> classifications;

    private String url;

    /**
     *
     */
    public BenchmarkQna() {
        this.alternativeQuestions = new ArrayList<String>();
        this.testQuestions = new ArrayList<BenchmarkQuestion>();
        this.classifications = new ArrayList<String>();
    }

    /**
     * Get UUID of QnA
     * @return
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Set UUID of QnA
     * @param uuid
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    /**
     * @return list of alternative questions
     */
    public List<String> getAlternativeQuestions() {
        return alternativeQuestions;
    }

    /**
     * Set alternative questions
     * @param alternativeQuestions List of alternative questions
     */
    public void setAlternativeQuestions(List<String> alternativeQuestions) {
        this.alternativeQuestions = alternativeQuestions;
    }

    /**
     * Set test questions
     */
    public void setTestQuestions(List<BenchmarkQuestion> testQuestions) {
        this.testQuestions = testQuestions;
    }

    /**
     *
     * @return list of test questions
     */
    public List<BenchmarkQuestion> getTestQuestions() {
        return testQuestions;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }


    public String getAnswerClientSideEncryptionAlgorithm() {
        return answerClientSideEncryptionAlgorithm;
    }

    public void setAnswerClientSideEncryptionAlgorithm(String answerClientSideEncryptionAlgorithm) {
        this.answerClientSideEncryptionAlgorithm = answerClientSideEncryptionAlgorithm;
    }

    public List<String> getClassifications() {
        return classifications;
    }

    public void setClassifications(List<String> classifications) {
        this.classifications = classifications;
    }

    /**
     * Set source URL
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get source URL
     */
    public String getUrl() {
        return url;
    }
}
