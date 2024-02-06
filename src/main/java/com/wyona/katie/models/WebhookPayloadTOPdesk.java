package com.wyona.katie.models;

/**
 * Webhook Payload sent by TOPdesk
 * 
 */
public class WebhookPayloadTOPdesk extends WebhookPayload {

    private String incidentId;

    /**
     *
     */
    public WebhookPayloadTOPdesk() {
        this.incidentId = null;
    }

    /**
     * Set incident Id
     * @param incidentId Incident Id, e.g. "I-240205-0956"
     */
    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    /**
     * @return incident Id, e.g. "I-240205-0956"
     */
    public String getIncidentId() {
        return incidentId;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Incident Id: " + incidentId);
        return sb.toString();
    }
}
