package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

public class BenchmarkPrecision {

    private List<BenchmarkPrecisionResult> results;

    private int successful;
    private int failed;

    private double precisionSum;
    private double recallSum;

    private Date date;
    private String domainId;

    private DetectDuplicatedQuestionImpl detectDuplicatedQuestionImpl;
    private EmbeddingsImpl embeddingsImpl;

    private List<String> failedQuestions;

    /**
     *
     */
    public BenchmarkPrecision(String domainId, DetectDuplicatedQuestionImpl detectDuplicatedQuestionImpl) {
        this.domainId = domainId;
        this.detectDuplicatedQuestionImpl = detectDuplicatedQuestionImpl;
        this.embeddingsImpl = EmbeddingsImpl.UNSET;
        results = new ArrayList<BenchmarkPrecisionResult>();

        successful = 0;
        failed = 0;
        failedQuestions = new ArrayList<String>();

        precisionSum = 0;
        recallSum = 0;

        date = new Date();
    }

    /**
     * Add evaluation result to benchmark report
     */
    public void addResult(BenchmarkPrecisionResult result) {
        results.add(result);
        if (result.getAccuracy()) {
            successful++;
        } else {
            failed++;
            String foundAnswer = null;
            if (result.getRetrievedUuids() != null && result.getRetrievedUuids().length > 0) {
                foundAnswer = result.getRetrievedUuids()[0];
            }
            // TODO: Instead a string, add an object containing question, UUID of retrieved QnA and UUID of expected QnA
            failedQuestions.add(result.getQuestion() + " (Retrieved QnA: " + foundAnswer + ", Expected QnA: " + result.getUuidQnA() + ")");
        }
        precisionSum = precisionSum + result.getPrecision();
        recallSum = recallSum + result.getRecall();
    }

    /**
     * Get all test results
     */
    public BenchmarkPrecisionResult[] getResults() {
        return results.toArray(new BenchmarkPrecisionResult[0]);
    }

    /**
     * Get date of report / test run
     */
    public Date getDate() {
        return date;
    }

    /**
     * Get total number of questions tested
     */
    public int getTotalNumQuestions() {
        return results.size();
    }

    /**
     * Get accuracy
     */
    public double getAccuracy() {
        double accuracy = (double)this.successful / (double)results.size();
        return accuracy;
    }

    /**
     *
     */
    public String[] getFailedQuestions() {
        return failedQuestions.toArray(new String[0]);
    }

    /**
     * Get precision
     * https://en.wikipedia.org/wiki/Evaluation_measures_(information_retrieval)#Precision
     */
    public double getPrecision() {
        double precision = this.precisionSum / (double)results.size();
        return precision;
    }

    /**
     * Get recall
     * https://en.wikipedia.org/wiki/Evaluation_measures_(information_retrieval)#Recall
     */
    public double getRecall() {
        double recall = this.recallSum / (double)results.size();
        return recall;
    }

    /**
     * https://en.wikipedia.org/wiki/Evaluation_measures_(information_retrieval)#F-score_/_F-measure
     */
    public double getF1() {
        return 2*getPrecision()*getRecall()/(getPrecision()+getRecall());
    }

    /**
     *
     */
    public String getDomainId() {
        return domainId;
    }

    /**
     *
     */
    public DetectDuplicatedQuestionImpl getDetectDuplicatedQuestionImpl() {
        return detectDuplicatedQuestionImpl;
    }

    /**
     *
     */
    public void setEmbeddingsImpl(EmbeddingsImpl embeddingsImpl) {
        this.embeddingsImpl = embeddingsImpl;
    }

    /**
     *
     */
    public EmbeddingsImpl getEmbeddingsImpl() {
        return embeddingsImpl;
    }
}
