package com.wyona.katie.models;

/**
 * Ask question privacy options
 */
public class AskQuestionPrivacyOptions {

    boolean logRequest;
    boolean logQuestion;

    /**
     *
     */
    public AskQuestionPrivacyOptions() {
        this.logRequest = true;
        this.logQuestion = true;
    }

    /**
     * @return true when request is being logged and false otherwise
     */
    public boolean getLogRequest() {
        return logRequest;
    }

    /**
     * @param logRequest True when request should be logged and false when whole request should not be logged
     */
    public void setLogRequest(boolean logRequest) {
        this.logRequest = logRequest;
    }

    /**
     * @return true when question is being logged and false otherwise
     */
    public boolean getLogQuestion() {
        return logQuestion;
    }

    /**
     * @param logQuestion True when question should be logged and false when question only should not be logged
     */
    public void setLogQuestion(boolean logQuestion) {
        this.logQuestion = logQuestion;
    }
}
