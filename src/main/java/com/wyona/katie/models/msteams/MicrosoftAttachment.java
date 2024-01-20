package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class MicrosoftAttachment {

    private Object content;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MicrosoftAttachment(Object content) {
        this.content = content;
    }

    /**
     *
     */
    public String getContentType() {
        return "application/vnd.microsoft.card.adaptive";
    }

    /**
     *
     */
    public String getContentUrl() {
        return null;
    }

    /**
     *
     */
    public Object getContent() {
        return content;
    }
}
