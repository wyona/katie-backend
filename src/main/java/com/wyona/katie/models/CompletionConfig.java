package com.wyona.katie.models;

/**
 * Completion configuration
 */
public class CompletionConfig {

    CompletionImpl completionImpl;
    String model;
    String apiKey;
    String host;

    /**
     *
     */
    public CompletionConfig() {
    }

    /**
     * @param model Completion model, e.g. "deepseek-r1" or OpenAI's "gpt-3.5-turbo"
     * @param host Custom host, e.g. "http://localhost:11434/" or "https://ollama.katie.qa/"
     */
    public CompletionConfig(CompletionImpl completionImpl, String model, String apiKey, String host) {
        this.completionImpl = completionImpl;
        this.model = model;
        this.apiKey = apiKey;
        this.host = host;
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

    /**
     * @return host of completion endpoint, e.g. "http://localhost:11434/" or "https://ollama.katie.qa/"
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host Host of completion endpoint, e.g. "http://localhost:11434/" or "https://ollama.katie.qa/"
     */
    public void setHost(String host) {
        this.host = host;
    }
}
