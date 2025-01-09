package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 *
 */
@Slf4j
public class CompletionResponse {

    private String text;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public CompletionResponse() {
    }

    /**
     * @param text LLM response message
     */
    public CompletionResponse(String text) {
        this.text = text;
    }

    /**
     * @param text LLM response message
     */
    public void setText(String text) {
        this.text = text;
    }

    /**
     * @return LLM response message
     */
    public String getText() {
        return text;
    }
}
