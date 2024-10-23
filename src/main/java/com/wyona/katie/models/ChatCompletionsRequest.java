package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ChatCompletionsRequest {

    private List<PromptMessageWithRoleLowerCase> messages = new ArrayList<>();

    /**
     *
     */
    public PromptMessageWithRoleLowerCase[] getMessages() {
        return messages.toArray(new PromptMessageWithRoleLowerCase[0]);
    }

    /**
     *
     */
    public void setMessages(PromptMessageWithRoleLowerCase[] messages) {
        for (PromptMessageWithRoleLowerCase msg : messages) {
            this.messages.add(msg);
        }
    }
}
