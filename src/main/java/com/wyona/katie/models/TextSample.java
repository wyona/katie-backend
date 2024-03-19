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
     *
     */
    public TextSample(String id, String text, Classification classification) {
        this.id = id;
        this.text = text;
        this.classification = classification;
    }

    /**
     *
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
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
