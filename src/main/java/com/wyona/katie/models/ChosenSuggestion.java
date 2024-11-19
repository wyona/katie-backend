package com.wyona.katie.models;

/**
 *
 */
public class ChosenSuggestion {

    private String index;
    private String type;

    /**
     * @param index Index / Id of chosen suggestion, e.g. 0
     */
    public void setIndex(String index) {
        this.index = index;
    }

    /**
     *
     */
    public String getIndex() {
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
