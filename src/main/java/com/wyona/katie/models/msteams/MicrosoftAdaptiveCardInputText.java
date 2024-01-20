package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

/**
 * https://adaptivecards.io/schemas/adaptive-card.json
 */
@Slf4j
public class MicrosoftAdaptiveCardInputText implements MicrosoftAdaptiveCardBodyItem {


    private String id;
    private String value;
    private String placeholder;
    private boolean isMultiline = false;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MicrosoftAdaptiveCardInputText(String id, String placeholder) {
        this.id = id;
        this.value = null;
        this.placeholder = placeholder;
        this.isMultiline = false;
    }

    /**
     *
     */
    public MicrosoftAdaptiveCardInputText(String id, String placeholder, boolean isMultiline) {
        this.id = id;
        this.value = null;
        this.placeholder = placeholder;
        this.isMultiline = isMultiline;
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
    public String getType() {
        return "Input.Text";
    }

    /**
     *
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     *
     */
    public String getValue() {
        return value;
    }

    /**
     *
     */
    public String getPlaceholder() {
        return placeholder;
    }

    /**
     *
     */
    public boolean getIsMultiline() {
        return isMultiline;
    }
}
