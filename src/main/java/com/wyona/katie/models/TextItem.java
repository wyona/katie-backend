package com.wyona.katie.models;

public class TextItem {

    private String text;
    private int label;

    /**
     *
     */
    public TextItem(String text, int label) {
        this.text = text;
        this.label = label;
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
    public int getLabel() {
        return label;
    }
}
