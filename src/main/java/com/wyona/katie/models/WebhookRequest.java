package com.wyona.katie.models;

/**
 * Webhook request
 */
public class WebhookRequest {

    private String id;
    private long sentAt;
    private int statusCode;

    /**
     * @param id Webhook Id
     * @param sentAt Epoch time when request was sent
     * @param statusCode Status code of response, e.g. 200 or 404
     */
    public WebhookRequest(String id, long sentAt, int statusCode) {
        this.id = id;
        this.sentAt = sentAt;
        this.statusCode = statusCode;
    }

    /**
     * Get webhook Id
     */
    public String getId() {
        return id;
    }

    /**
     * @return epoch time when webhook request was sent
     */
    public long getSentAt() {
        return sentAt;
    }

    /**
     * @return response status code, e.g. 200 or 404
     */
    public int getStatusCode() {
        return statusCode;
    }
}
