package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.List;

/**
 * Conversation history
 */
public class ChatHistory {

    List<PromptMessageWithRoleLowerCase> messages = new ArrayList<>();

    /**
     *
     */
    public ChatHistory() {
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
