package com.wyona.katie.models;

public class Classification implements Comparable<Classification> {

    private String label;
    private String description;
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
     * @param id Foreign class Id, e.g. "9daa804d-95dc-4a60-b1b6-995977607a4e_99e33f5d-37a1-4333-b4a9-483535fa187a" or "64e3bb24-1522-4c49-8f82-f99b34a82062" or "https://jena.apache.org/2f61f866-bcd8-4db3-833b-37e6f7877e52"
     * @param katieId Id assigned by Katie, e.g. "eb818156-6c77-4673-b612-fc94b82bb1a8"
     */
    public Classification(String label, String id, String katieId) {
        this.label = label;
        this.id = id;
        this.katieId = katieId;
    }

    /**
     * @param label Class name / Label, e.g. "Managed Device Services (ZI), MacOS Clients"
     */
    public void setTerm(String label) {
        this.label = label;
    }

    /**
     * @return class name / label, e.g. "Managed Device Services (ZI), MacOS Clients"
     */
    public String getTerm() {
        return label;
    }

    /**
     * @param description Description of classification, e.g. "This subcategory contains incidents involving devices with MacOS that are managed by the University of Zurich."
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return description of classification, e.g. "This subcategory contains incidents involving devices with MacOS that are managed by the University of Zurich."
     */
    public String getDescription() {
        return description;
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

    /**
     * Compare two classifications by label term (alphabetically)
     */
    @Override
    public int compareTo(Classification classification) {
        //return classification.getTerm().compareToIgnoreCase(this.getTerm());
        return this.getTerm().compareToIgnoreCase(classification.getTerm());
    }
}
