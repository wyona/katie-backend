package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 * https://cookbook.openai.com/examples/azure/chat_with_your_own_data
 * https://platform.openai.com/docs/guides/text-generation/chat-completions-api
 */
@Slf4j
public class PromptMessage {

    private PromptMessageRole role;
    private String content;
    private String[] attachments;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public PromptMessage() {
    }

    /**
     *
     */
    public PromptMessage(PromptMessageRole role, String content) {
        this.role = role;
        this.content = content;
    }

    /**
     * @param role Role, e.g. "system", "user", "assistant"
     */
    public void setRole(PromptMessageRole role) {
        this.role = role;
    }

    /**
     *
     */
    public PromptMessageRole getRole() {
        return role;
    }

    /**
     *
     */
    public void setContent(String content) {
        this.content = content;
    }

    /**
     *
     */
    public String getContent() {
        return content;
    }

    /**
     *
     */
    public void setAttachments(String[] attachments) {
        this.attachments = attachments;
    }

    /**
     *
     */
    public String[] getAttachments() {
        return attachments;
    }
}
