package com.wyona.katie.models;

import java.util.List;

public class CustomBenchmarkDataset {
    private String datasetName;
    private String datasetVersion;
    private List<BenchmarkQna> qnas;
    private List<Question> trueNegatives;

    public CustomBenchmarkDataset() {}

    public CustomBenchmarkDataset(String datasetName, String datasetVersion, List<BenchmarkQna> qnas) {
        super();
        this.datasetName = datasetName;
        this.datasetVersion = datasetVersion;
        this.qnas = qnas;
    }

    public String getDatasetName() {
        return datasetName;
    }

    public String getDatasetVersion() {
        return datasetVersion;
    }

    public List<BenchmarkQna> getQnas() {
        return qnas;
    }

    /**
     * Get questions which are supposed to be true negative (TN)
     * @return questions which are supposed to be true negative (TN)
     */
    public List<Question> getTrueNegatives() {
        return trueNegatives;
    }

}
