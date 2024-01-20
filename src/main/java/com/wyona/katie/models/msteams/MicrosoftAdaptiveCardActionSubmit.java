package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class MicrosoftAdaptiveCardActionSubmit extends MicrosoftAdaptiveCardAction {

    private Object data;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MicrosoftAdaptiveCardActionSubmit(String title) {
        super(title, "Action.Submit");
    }

    /**
     *
     */
    public void setData(Object data) {
        this.data = data;
    }

    /**
     *
     */
    public Object getData() {
        return data;
    }
}
