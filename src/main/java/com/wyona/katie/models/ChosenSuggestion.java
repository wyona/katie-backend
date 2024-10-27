package com.wyona.katie.models;

/**
 *
 */
public class ChosenSuggestion {

    private int index;
    private String type;

    /**
     * @param index Index / Id of chosen suggestion, e.g. 0
     */
    public void setIndex(int index) {
        this.index = index;
    }

    /**
     *
     */
    public int getIndex() {
        return index;
    }

    /**
     * @param type Type of chosen suggestion, e.g. "starter"
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     *
     */
    public String getType() {
        return type;
    }
}
