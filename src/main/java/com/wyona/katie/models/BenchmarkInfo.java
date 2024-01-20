package com.wyona.katie.models;

import java.time.LocalDateTime;

public class BenchmarkInfo {

    private LocalDateTime dateTime;
    private String id;
    private String datasetName;
    private int datasetSize;
    private int numberOfRuns;
    private String datasetVersion;

    private String environmentMeta;

    private boolean alternativeQuestionsIndexed;
    private boolean answersReRanked;
    private ReRankImpl reRankImpl;

    /**
     *
     */
    public BenchmarkInfo() {}

    /**
     *
     * @param dateTime TODO
     * @param id Benchmark Id
     * @param datasetName TODO
     * @param datasetSize TODO
     * @param numberOfRuns TODO
     * @param datasetVersion TODO
     * @param environmentMeta TODO
     * @param alternativeQuestionsIndexed True when alternative questions got indexed
     * @param answersReRanked True when answers were re-ranked
     * @param reRankImpl Re-rank implementation when answers were re-ranked
     */
    public BenchmarkInfo(LocalDateTime dateTime, String id, String datasetName, int datasetSize, int numberOfRuns, String datasetVersion, String environmentMeta, boolean alternativeQuestionsIndexed, boolean answersReRanked, ReRankImpl reRankImpl) {
        super();

        this.dateTime = dateTime;
        this.id = id;

        this.datasetName = datasetName;
        this.datasetSize = datasetSize;
        this.datasetVersion = datasetVersion;

        this.numberOfRuns = numberOfRuns;

        this.environmentMeta = environmentMeta;

        this.alternativeQuestionsIndexed = alternativeQuestionsIndexed;
        this.answersReRanked = answersReRanked;
        this.reRankImpl = reRankImpl;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }
    public String getId() {
        return id;
    }
    public String getDatasetName() {
        return datasetName;
    }
    public int getDatasetSize() {
        return datasetSize;
    }
    public int getNumberOfRuns() {
        return numberOfRuns;
    }
    public String getDatasetVersion() {
        return datasetVersion;
    }

    /**
     * @return meta information re environment where benchmark is being executed, e.g. operating system
     */
    public String getEnvironmentMeta() {
        return environmentMeta;
    }

    /**
     * @return true when alternative questions got indexed
     */
    public boolean getAlternativeQuestionsIndexed() {
        return alternativeQuestionsIndexed;
    }

    /**
     *
     */
    public boolean getAnswersReRanked() {
        return answersReRanked;
    }

    /**
     *
     */
    public ReRankImpl getReRankImpl() {
        return reRankImpl;
    }
}
