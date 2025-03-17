package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversation history
 */
public class ChatHistory {

    String userId;
    List<PromptMessageWithRoleLowerCase> messages = new ArrayList<>();

    /**
     *
     */
    public ChatHistory() {
    }

    /**
     * @param userId Id of user having conversation with LLM
     */
    public ChatHistory(String userId) {
        this.userId = userId;
    }

    /**
     * @param userId Id of user having conversation with LLM
     */
    public void setUserId(String userId) {
        this.userId = userId;
    }

    /**
     * @return Id of user having conversation with LLM
     */
    public String getUserId() {
        return userId;
    }

    /**
     *
     */
    public void appendMessage(PromptMessageWithRoleLowerCase message) {
        messages.add(message);
    }

    /**
     *
     */
    public PromptMessageWithRoleLowerCase[] getMessages() {
        return messages.toArray(new PromptMessageWithRoleLowerCase[0]);
    }
}
