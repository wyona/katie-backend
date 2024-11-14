package com.wyona.katie.models.learningcoach;

import com.wyona.katie.models.PromptMessageWithRoleLowerCase;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ConversationStarter {

    private Suggestion suggestion;
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

    /**
     *
     */
    public Suggestion getSuggestion() {
        return suggestion;
    }

    /**
     *
     */
    public void setSuggestion(Suggestion suggestion) {
        this.suggestion = suggestion;
    }
}
