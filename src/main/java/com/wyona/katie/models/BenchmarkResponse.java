package com.wyona.katie.models;

import java.time.LocalDateTime;
import java.util.List;

/**
 *
 */
public class BenchmarkResponse {

    private BenchmarkInfo info;
    private BenchmarkResult[] implementationResults;

    /**
     * @param info TODO
     * @param implementationResults TODO
     */
    public BenchmarkResponse(BenchmarkInfo info, BenchmarkResult[] implementationResults) {
        super();

        this.info = info;
        this.implementationResults = implementationResults;
    }

    /**
     *
     */
    public BenchmarkInfo getInfo() {
        return info;
    }

    /**
     * @return list of tested implementations
     */
    public BenchmarkResult[] getImplementationResults() {
        return implementationResults;
    }
}