package com.wyona.katie.models;

public class BenchmarkConfiguration {

    private RetrievalConfiguration[] retrievalConfigs;

    /**
     *
     */
    public void setRetrievalConfigs(RetrievalConfiguration[] retrievalConfigs) {
        this.retrievalConfigs = retrievalConfigs;
    }

    /**
     *
     */
    public RetrievalConfiguration[] getRetrievalConfigs() {
        return retrievalConfigs;
    }
}
