package com.wyona.katie.models;

public class Classification {

    private String label;
    private String id;
    private String katieId;
    private Integer frequency = null;

    /**
     *
     */
    public Classification() {
    }

    /**
     * @param label Class name / Label, e.g. "Managed Device Services, MacOS Clients"
     * @param id Foreign class Id, e.g. "64e3bb24-1522-4c49-8f82-f99b34a82062" or "https://jena.apache.org/2f61f866-bcd8-4db3-833b-37e6f7877e52"
     * @param katieId Id assigned by Katie
     */
    public Classification(String label, String id, String katieId) {
        this.label = label;
        this.id = id;
        this.katieId = katieId;
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
     * @param id Foreign class Id, e.g. "64e3bb24-1522-4c49-8f82-f99b34a82062" or "https://jena.apache.org/2f61f866-bcd8-4db3-833b-37e6f7877e52"
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * @return foreign class Id, e.g. "64e3bb24-1522-4c49-8f82-f99b34a82062" or "https://jena.apache.org/2f61f866-bcd8-4db3-833b-37e6f7877e52"
     */
    public String getId() {
        return id;
    }

    /**
     *
     */
    public void setKatieId(String katieId) {
        this.katieId = katieId;
    }

    /**
     *
     */
    public String getKatieId() {
        return katieId;
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
