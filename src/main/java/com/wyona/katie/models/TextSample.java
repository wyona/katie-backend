package com.wyona.katie.models;

/**
 * Text sample of a particular classification
 */
public class TextSample {

    private String text;
    private Classification classification;

    /**
     *
     */
    public TextSample(String text, Classification classification) {
        this.text = text;
        this.classification = classification;
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
