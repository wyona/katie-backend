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
    public TextSample(String id, String text, Classification classification) {
        this.id = id;
        this.text = text;
        this.classification = classification;
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
    public String getText() {
        return text;
    }

    /**
     *
     */
    public Classification getClassification() {
        return classification;
    }
}
