package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 *
 */
@Slf4j
public class CompletionAssistant {

    private String id;
    private String name;
    private String instructions;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public CompletionAssistant() {
    }

    /**
     * @param id Assistant Id, e.g. "asst_79S9rWytfx7oNqyIr2rrJGBB"
     * @param name Assistant name, e.g. "Legal Insurance Assistant"
     * @param instructions Assistant instructions, e.g. "You are a legal insurance expert. Use your knowledge base to select the relevant documents to answer questions about legal topics."
     */
    public CompletionAssistant(String id, String name, String instructions) {
        this.id = id;
        this.name = name;
        this.instructions = instructions;
    }

    /**
     *
     */
    public String getId() {
        return id;
    }

    /**
     * Set assistant Id, e.g. "asst_79S9rWytfx7oNqyIr2rrJGBB"
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     */
    public String getName() {
        return name;
    }

    /**
     *
     */
    public String getInstructions() {
        return instructions;
    }
}
