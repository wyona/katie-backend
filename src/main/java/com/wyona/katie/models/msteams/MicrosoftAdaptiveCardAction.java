package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class MicrosoftAdaptiveCardAction {

    private String title;
    private String type;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     * @param title Button title, e.g. "More answers ..."
     * @param type Action type, e.g. "Action.Submit" or "Action.OpenUrl"
     */
    public MicrosoftAdaptiveCardAction(String title, String type) {
        this.title = title;
        this.type = type;
    }

    /**
     *
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     */
    public String getType() {
        return type;
    }
}
