package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Date;

@Slf4j
public class TestReport {

    private List<TestResult> results;
    private int failed;
    private int success;
    private Date date;
    private String domainId;

    private DetectDuplicatedQuestionImpl detectDuplicatedQuestionImpl;
    private EmbeddingsImpl embeddingsImpl;
    private String embeddingsImplModel;
    private String luceneVectorSearchMetric;
    private float sentenceBERTDistanceThreshold;

    /**
     *
     */
    public TestReport(String domainId, DetectDuplicatedQuestionImpl detectDuplicatedQuestionImpl) {
        this.domainId = domainId;
        this.detectDuplicatedQuestionImpl = detectDuplicatedQuestionImpl;
        this.embeddingsImpl = EmbeddingsImpl.UNSET;
        results = new ArrayList<TestResult>();
        failed = 0;
        success = 0;
        date = new Date();

        sentenceBERTDistanceThreshold = -1;
    }

    /**
     * Add test result to report
     */
    public void addResult(TestResult result) {
        results.add(result);
        if (result.getSuccess()) {
            success++;
        } else {
            failed++;
        }
    }

    /**
     * Get all test results
     */
    public TestResult[] getResults() {
        return results.toArray(new TestResult[0]);
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
     * Get total number of failed tests
     */
    public int getFailed() {
        return failed;
    }

    /**
     * Get total number of successful tests
     */
    public int getSuccess() {
        return success;
    }

    /**
     * Get accuracy (correct retrieved answers divided by total number of questions)
     * https://en.wikipedia.org/wiki/Accuracy_and_precision#In_information_systems
     */
    public double getAccuracy() {
        double accuracy = (double)success / (double)results.size();
        return accuracy;
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

    /**
     * @return embeddings model, e.g. Cohere's embed-multilingual-v2.0
     */
    public String getEmbeddingsImplModel() {
        return embeddingsImplModel;
    }

    /**
     *
     */
    public void setEmbeddingsImplModel(String embeddingsImplModel) {
        this.embeddingsImplModel = embeddingsImplModel;
    }

    /**
     * @return Lucene vector search metric, e.g. COSINE, DOT_PRODUCT, EUCLIDEAN
     */
    public String getLuceneVectorSearchMetric() {
        return luceneVectorSearchMetric;
    }

    /**
     *
     */
    public void setLuceneVectorSearchMetric(String luceneVectorSearchMetric) {
        this.luceneVectorSearchMetric = luceneVectorSearchMetric;
    }

    /**
     *
     */
    public float getSentenceBERTDistanceThreshold() {
        return sentenceBERTDistanceThreshold;
    }

    /**
     *
     */
    public void setSentenceBERTDistanceThreshold(float sentenceBERTDistanceThreshold) {
        log.info("Set SentenceBERT distance threshold: " + sentenceBERTDistanceThreshold);
        this.sentenceBERTDistanceThreshold = sentenceBERTDistanceThreshold;
    }
}
