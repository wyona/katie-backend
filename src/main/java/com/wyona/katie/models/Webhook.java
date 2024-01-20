package com.wyona.katie.models;

/**
 * Webhook
 */
public class Webhook {

    private String id;
    private String payloadURL;
    private String contentType;
    private boolean enabled;

    private String apiKey;

    /**
     * @param payloadURL Payload URL, e.g. https://example.com/postreceive or https://postman-echo.com/post or https://discord.com/api/webhooks/996407023648391258/NR4J81L873ZzUSD75p1
     * @param enabled True when webhook is active
     */
    public Webhook(String id, String payloadURL, boolean enabled) {
        this.id = id;
        this.payloadURL = payloadURL;
        this.contentType = "application/json";
        this.enabled = enabled;
    }

    /**
     * Set Id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     */
    public String getId() {
        return id;
    }

    /**
     * @param payloadURL Payload URL, e.g. https://example.com/postreceive
     */
    public void setPayloadURL(String payloadURL) {
        this.payloadURL = payloadURL;
    }

    /**
     *
     */
    public String getPayloadURL() {
        return payloadURL;
    }

    /**
     * @param contentType Content type, e.g. "application/json" or "application/x-www-form-urlencoded"
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     *
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @param enabled True when webhook is active, false when webhook is not active
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @return true when webhook is active and false when webhook is not active
     */
    public boolean getEnabled() {
        return enabled;
    }

    /**
     *
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     *
     */
    public String getApiKey() {
        return apiKey;
    }
}
