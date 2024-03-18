package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.List;

public class ClassificationDataset {

    private String name;
    private List<Classification> labels = new ArrayList<>();
    private List<TextSample> samples = new ArrayList<>();

    /**
     *
     */
    public ClassificationDataset() {
    }

    /**
     *
     */
    public ClassificationDataset(String name) {
        this.name = name;
    }

    /**
     * Get name of dataset, e.g. "20 newsgroups"
     */
    public String getName() {
        return name;
    }

    /**
     *
     */
    public void addLabel(Classification label) {
        labels.add(label);
    }

    /**
     *
     */
    public void addSample(TextSample sample) {
        samples.add(sample);
    }
}
