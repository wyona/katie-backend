package com.wyona.katie.models;

/**
 *
 */
public class BenchmarkPrecisionResult {

    private String question;
    private String uuidQnA;

    private boolean accuracy;
    private double precision;
    private double recall;

    private String[] relevantUuids;
    private String[] retrievedUuids;

    /**
     * @param question Question tested
     * @param uuidQnA UUID of QnA in case question is associated with a QnA
     * @param accuracy True when answer matched and false when answer did not match
     * @param precision Number of retrieved correct resp. relevant answers divided by total number of retrieved answers
     * @param recall Number of retrieved correct resp. relevant answers divided by total number of relevant answers
     * @param relevantUuids UUIDs of relevant answers
     * @param retrievedUuids UUIDs of retrieved answers
     */
    public BenchmarkPrecisionResult(String question, String uuidQnA, boolean accuracy, double precision, double recall, String[] relevantUuids, String[] retrievedUuids) {
        this.question = question;
        this.uuidQnA = uuidQnA;

        this.accuracy = accuracy;
        this.precision = precision;
        this.recall = recall;

        this.relevantUuids = relevantUuids;
        this.retrievedUuids = retrievedUuids;
    }

    /**
     * @return question which was tested
     */
    public String getQuestion() {
        return question;
    }

    /**
     *
     */
    public String getUuidQnA() {
        return uuidQnA;
    }

    /**
     * @return true whe top answer matched and false otherwise
     */
    public boolean getAccuracy() {
        return accuracy;
    }

    /**
     * @return Number of retrieved correct resp. relevant answers divided by total number of retrieved answers
     */
    public double getPrecision() {
        return precision;
    }

    /**
     * @return Number of retrieved correct resp. relevant answers divided by total number of relevant answers
     */
    public double getRecall() {
        return recall;
    }

    /**
     * @return UUIDs of relevant answers
     */
    public String[] getRelevantUuids() {
        return relevantUuids;
    }

    /**
     * @return UUIDs of retrieved answers
     */
    public String[] getRetrievedUuids() {
        return retrievedUuids;
    }
}
