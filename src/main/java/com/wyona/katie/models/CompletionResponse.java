package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
@Slf4j
public class CompletionResponse {

    private String text;
    private HashMap<String, String> functionArguments;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public CompletionResponse() {
        this.functionArguments = new HashMap<String, String>();
    }

    /**
     * @param text LLM response message
     */
    public CompletionResponse(String text) {
        this.functionArguments = new HashMap<String, String>();
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

    /**
     *
     */
    public void addFunctionArgument(String key, String value) {
        this.functionArguments.put(key, value);
    }

    /**
     *
     */
    public String getFunctionArgumentValue(String key) {
        if (functionArguments.containsKey(key)) {
            return functionArguments.get(key);
        } else {
            return null;
        }
    }
}
