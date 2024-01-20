package com.wyona.katie.models;

/**
 *
 */
public class TestResult {

    private String question;
    private boolean success;
    private String uuid;
    private String retrievedUuid;
    private double score;

    /**
     * @param question Question tested
     * @param success True when the test was successful and false when the test failed
     * @param uuid UUID of associated QnA
     * @param retrievedUuid UUID which was retrieved by search
     * @param score Confidence score
     */
    public TestResult(String question, boolean success, String uuid, String retrievedUuid, double score) {
        this.question = question;
        this.success = success;
        this.uuid = uuid;
        this.retrievedUuid = retrievedUuid;
        this.score = score;
    }

    /**
     * @return question which was tested
     */
    public String getQuestion() {
        return question;
    }

    /**
     * @return true when test successful and false otherwise
     */
    public boolean getSuccess() {
        return success;
    }

    /**
     * @return UUID of associated QnA
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @return UUID which was retrieved by search
     */
    public String getRetrievedUuid() {
        return retrievedUuid;
    }

    /**
     * @return confidence score
     */
    public double getScore() {
        return score;
    }
}
