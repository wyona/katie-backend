package com.wyona.katie.models;

import java.time.LocalDateTime;

public class BenchmarkResult {

    private DetectDuplicatedQuestionImpl systemName;
    private String systemVersion;
    private String systemMeta;

    private double accuracy;
    private String[] failedQuestions;
    private double accuracyDeviation;
    private int totalNumberOfQuestions;
    private double precision;
    private double precisionDeviation;
    private double recall;
    private double recallDeviation;

    private double indexingTimeInSeconds;
    private double indexingTimeDeviation;

    private double timeToRunBenchmarkInSeconds;
    private double inferenceTimeDeviation;

    private LocalDateTime benchmarkStart;

    public BenchmarkResult() {}

    /**
     * @param systemName
     * @param systemVersion Search implementation version
     * @param systemMeta Search implementation meta information
     * @param accuracy
     * @param precision
     * @param recall
     * @param indexingTimeInSeconds Number of seconds it takes to index the whole dataset
     * @param timeToRunBenchmarkInSeconds Number of seconds it takes to run benchmark for the whole dataset
     * @param benchmarkStart Date / time when benchmark was started
     */
    public BenchmarkResult(DetectDuplicatedQuestionImpl systemName, String systemVersion, String systemMeta, double accuracy, int totalNumQuestions, String[] failedQuestions, double precision, double recall, double indexingTimeInSeconds, double timeToRunBenchmarkInSeconds, LocalDateTime benchmarkStart) {
        super();
        this.systemName = systemName;
        this.systemVersion = systemVersion;
        this.systemMeta = systemMeta;

        this.accuracy = accuracy;
        this.accuracyDeviation = 0.0;
        this.totalNumberOfQuestions = totalNumQuestions;
        this.failedQuestions = failedQuestions;
        this.precision = precision;
        this.precisionDeviation = 0.0;
        this.recall = recall;
        this.recallDeviation = 0.0;
        this.indexingTimeInSeconds = indexingTimeInSeconds;
        this.indexingTimeDeviation = 0.0;
        this.timeToRunBenchmarkInSeconds = timeToRunBenchmarkInSeconds;
        this.inferenceTimeDeviation = 0.0;

        this.benchmarkStart = benchmarkStart;
    }

    /**
     * @return search implementation
     */
    public DetectDuplicatedQuestionImpl getSystemName() {
        return systemName;
    }

    /**
     * @param systemName Search implementation
     */
    public void setSystemName(DetectDuplicatedQuestionImpl systemName) {
        this.systemName = systemName;
    }

    /**
     * @return search implementation version, e.g. "9.4.2" for Apache Lucene
     */
    public String getSystemVersion() {
        return systemVersion;
    }

    /**
     * @return system meta information, e.g. embedding model used
     */
    public String getSystemMeta() {
        return systemMeta;
    }

    /**
     * @return accuracy
     */
    public double getAccuracy() {
        return accuracy;
    }

    /**
     *
     * @param accuracy
     */
    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    /**
     *
     */
    public String[] getFailedQuestions() {
        return failedQuestions;
    }

    /**
     * @return deviation of accuracy re reference benchmark
     */
    public double getAccuracyDeviationInPercentage() {
        return accuracyDeviation;
    }

    /**
     *
     * @param deviation
     */
    public void setAccuracyDeviationInPercentage(double deviation) {
        this.accuracyDeviation = deviation;
    }

    /**
     * @return total number of questions asked to determine accuracy, precision and recall
     */
    public int getTotalNumberOfQuestions() {
        return totalNumberOfQuestions;
    }

    public double getPrecision() {
        return precision;
    }
    public void setPrecision(double precision) {
        this.precision = precision;
    }

    public void setPrecisionDeviationInPercentage(double deviation) {
        this.precisionDeviation = deviation;
    }

    public double getPrecisionDeviationInPercentage() {
        return precisionDeviation;
    }

    public double getRecall() {
        return recall;
    }
    public void setRecall(double recall) {
        this.recall = recall;
    }

    public void setRecallDeviationInPercentage(double deviation) {
        this.recallDeviation = deviation;
    }

    public double getRecallDeviationInPercentage() {
        return recallDeviation;
    }

    /**
     * @return number of seconds it takes to index the whole dateset
     */
    public double getIndexingTimeInSeconds() {
        return indexingTimeInSeconds;
    }

    /**
     * @param indexingTimeInSeconds Number of seconds it takes to index the whole dataset
     */
    public void setIndexingTimeInSeconds(double indexingTimeInSeconds) {
        this.indexingTimeInSeconds = indexingTimeInSeconds;
    }

    /**
     *
     */
    public void setIndexingTimeDeviationInPercentage(double deviation) {
        this.indexingTimeDeviation = deviation;
    }

    /**
     *
     */
    public double getIndexingTimeDeviationInPercentage() {
        return indexingTimeDeviation;
    }

    /**
     * @return number of seconds it takes to run benchmark for the whole dataset
     */
    public double getTimeToRunBenchmarkInSeconds() {
        return timeToRunBenchmarkInSeconds;
    }

    /**
     * @param timeToRunBenchmarkInSeconds Number of seconds it takes to run the benchmark with the whole dataset
     */
    public void setTimeToRunBenchmarkInSeconds(double timeToRunBenchmarkInSeconds) {
        this.timeToRunBenchmarkInSeconds = timeToRunBenchmarkInSeconds;
    }

    public void setTimeToRunBenchmarkDeviationInPercentage(double deviation) {
        this.inferenceTimeDeviation = deviation;
    }

    public double getTimeToRunBenchmarkDeviationInPercentage() {
        return this.inferenceTimeDeviation;
    }

    /**
     * @return date /time when benchmark was started
     */
    public LocalDateTime getDateTime() {
        return benchmarkStart;
    }

    /**
     * Set date / time when benchmark was started
     * @param benchmarkStart Date / time when benchmark was started
     */
    public void setDateTime(LocalDateTime benchmarkStart) {
        this.benchmarkStart = benchmarkStart;
    }
}
