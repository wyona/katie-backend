package com.wyona.katie.models;

/**
 * Webhook Payload sent by OneNote
 */
public class WebhookPayloadOneNote extends WebhookPayload {

    private String location;

    /**
     *
     */
    public WebhookPayloadOneNote() {
        this.location = null;
    }

    /**
     * @param location For example "me"
     */
    public WebhookPayloadOneNote(String location) {
        this.location = location;
    }

    /**
     *
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * @return location, e.g. "me"
     */
    public String getLocation() {
        return location;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Location: " + location);
        return sb.toString();
    }
}
