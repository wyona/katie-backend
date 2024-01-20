package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class MicrosoftAdaptiveCardTextBlock implements MicrosoftAdaptiveCardBodyItem {

    private String text;
    private String size;
    private String weight;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MicrosoftAdaptiveCardTextBlock(String text) {
        this.text = text;
        this.size = "default";
        this.weight = "default";
    }

    /**
     * https://adaptivecards.io/schemas/adaptive-card.json
     * @return font size, e.g. "default", "small", "medium", "large", "extraLarge"
     */
    public String getSize() {
        return size;
    }

    /**
     *
     */
    public void setSize(String size) {
        this.size = size;
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
    public String getType() {
        return "TextBlock";
    }

    /**
     * https://adaptivecards.io/schemas/adaptive-card.json
     * @return font weight, e.g. "default", "lighter", "bolder"
     */
    public String getWeight() {
        return weight;
    }

    /**
     *
     */
    public void setWeight(String weight) {
        this.weight = weight;
    }

    /**
     *
     */
    public boolean getWrap() {
        return true;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Item: TODO");
        return s.toString();
    }
}
