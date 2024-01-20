package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class MicrosoftAdaptiveCardActionOpenUrl extends MicrosoftAdaptiveCardAction {

    private String url;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     * @param title Button title, e.g. "More answers ..."
     */
    public MicrosoftAdaptiveCardActionOpenUrl(String title) {
        super(title, "Action.OpenUrl");
    }

    /**
     *
     */
    public String getUrl() {
        return url;
    }

    /**
     *
     */
    public void setUrl(String url) {
        this.url = url;
    }
}
