package com.wyona.katie.models;

/**
 * Text sample of a particular classification
 */
public class TextSample {

    private String id;
    private String text;
    private Classification classification;

    /**
     *
     */
    public TextSample() {
    }

    /**
     * @param id Sample Id, e.g. "I-240605-0858"
     * @param text Sample text, e.g. "My TS Teams does not connect anymore"
     * @param classification Sample classification, e.g. "Communication and Collaboration, M365-Teams"
     */
    public TextSample(String id, String text, Classification classification) {
        this.id = id;
        this.text = text;
        this.classification = classification;
    }

    /**
     * Set sample Id, e.g. "I-240605-0858"
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get sample Id, e.g. "I-240605-0858"
     */
    public String getId() {
        return id;
    }

    /**
     *
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     *
     */
    public String getText() {
        return text;
    }

    /**
     *
     */
    public void setClassification(Classification classification) {
        this.classification = classification;
    }

    /**
     *
     */
    public Classification getClassification() {
        return classification;
    }
}
