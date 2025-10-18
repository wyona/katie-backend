package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * See for example https://platform.openai.com/docs/guides/function-calling#function-calling-with-structured-outputs
 */
@Slf4j
public class CompletionTool {

    private String type;
    private String functionArgument;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public CompletionTool() {
    }

    /**
     * @param type Tool type, e.g. "function"
     */
    public CompletionTool(String type) {
        this.type = type;
    }

    /**
     * @param type Tool type, e.g. "function"
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return tool type, e.g. "function"
     */
    public String getType() {
        return type;
    }

    /**
     * Tmp
     * @param functionArgument Function argument, e.g. "file_path"
     */
    public void setFunctionArgument(String functionArgument) {
        this.functionArgument = functionArgument;
    }

    /**
     * @return function argument, e.g. "file_path"
     */
    public String getFunctionArgument() {
        return functionArgument;
    }
}
