package com.wyona.katie.models;

public class Classification {

    private String label;
    private String id;
    private Integer frequency = null;

    /**
     *
     */
    public Classification() {
    }

    /**
     * @param label Class name / Label, e.g. "Managed Device Services, MacOS Clients"
     * @param id Class Id, e.g. "64e3bb24-1522-4c49-8f82-f99b34a82062"
     */
    public Classification(String label, String id) {
        this.label = label;
        this.id = id;
    }

    /**
     * @param label Class name / Label
     */
    public void setTerm(String label) {
        this.label = label;
    }

    /**
     * @return class name / label
     */
    public String getTerm() {
        return label;
    }

    /**
     * @param id Class Id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return class Id
     */
    public String getId() {
        return id;
    }

    /**
     *
     */
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }

    /**
     * Get number of samples in this class, resp. number of samples that are labeled with this class
     */
    public Integer getFrequency() {
        return frequency;
    }
}
