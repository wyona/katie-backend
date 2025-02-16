package com.wyona.katie.models;

/**
 *
 */
public class CompletionConfig {

    CompletionImpl completionImpl;
    String model;
    String apiKey;

    /**
     *
     */
    public CompletionConfig() {
    }

    /**
     * @param model Completion model, e.g. "deepseek-r1"
     */
    public CompletionConfig(CompletionImpl completionImpl, String model, String apiKey) {
        this.completionImpl = completionImpl;
        this.model = model;
        this.apiKey = apiKey;
    }

    /**
     *
     */
    public CompletionImpl getCompletionImpl() {
        return completionImpl;
    }

    /**
     *
     */
    public void setCompletionImpl(CompletionImpl completionImpl) {
        this.completionImpl = completionImpl;
    }

    /**
     * @return Completion model, e.g. "deepseek-r1"
     */
    public String getModel() {
        return model;
    }

    /**
     *
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     *
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     *
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
