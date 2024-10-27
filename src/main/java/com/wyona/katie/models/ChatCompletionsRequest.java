package com.wyona.katie.models;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ChatCompletionsRequest {

    private Double temperature;
    private List<PromptMessageWithRoleLowerCase> messages = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    private ChosenSuggestion chosenSuggestion = null;

    /**
     *
     */
    public Double getTemperature() {
        return temperature;
    }

    /**
     *
     */
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

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
    public String[] getSuggestions() {
        return suggestions.toArray(new String[0]);
    }

    /**
     * Set conversation starters, e.g. "Summarize a text" or "Help me improving a text"
     */
    public void setSuggestions(String[] suggestions) {
        for (String suggestion : suggestions) {
            this.suggestions.add(suggestion);
        }
    }

    /**
     * Set chosen conversation starter
     */
    public void setchosen_suggestion(ChosenSuggestion chosenSuggestion) {
        this.chosenSuggestion = chosenSuggestion;
    }

    /**
     *
     */
    public ChosenSuggestion getchosen_suggestion() {
        return chosenSuggestion;
    }
}
