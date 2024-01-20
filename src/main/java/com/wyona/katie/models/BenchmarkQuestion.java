package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Benchmark Question
 */
public class BenchmarkQuestion {

    private String question;
    private List<String> classifications;
    private List<String> relevantUuids;

    /**
     *
     */
    public BenchmarkQuestion() {
        classifications = new ArrayList<String>();
        relevantUuids = new ArrayList<String>();
    }

    /**
     * @param question Question, e.g. "Wie hoch ist die Restwertentschädigung für eine alte Ölheizung?"
     */
    public void setQuestion(String question) {
        this.question = question;
    }

    /**
     *
     */
    public String getQuestion() {
        return question;
    }

    /**
     *
     */
    public void addClassification(String classification) {
        classifications.add(classification);
    }

    /**
     * @param classifications List of classifications, e.g. "oilheating", "compensation"
     */
    public void setClassifications(List<String> classifications) {
        this.classifications = classifications;
    }

    /**
     *
     */
    public List<String> getClassifications() {
        return classifications;
    }

    /**
     *
     */
    public void addRelevantUuid(String uuid) {
        relevantUuids.add(uuid);
    }

    /**
     *
     */
    public String[] getRelevantUuids() {
        return relevantUuids.toArray(new String[0]);
    }
}
