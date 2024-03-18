package com.wyona.katie.ai.models;

/**
 *
 */
public class TextEmbedding {

    private String text;
    private float[] vector;

    /**
     *
     */
    public TextEmbedding(String text, float[] vector) {
        this.text = text;
        this.vector = vector;
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
    public float[] getVector() {
        return vector;
    }
}
