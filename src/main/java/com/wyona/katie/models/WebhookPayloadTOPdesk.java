package com.wyona.katie.models;

/**
 * Webhook Payload sent by TOPdesk
 * 
 */
public class WebhookPayloadTOPdesk extends WebhookPayload {

    private Integer requestType;
    private String incidentId;

    /**
     *
     */
    public WebhookPayloadTOPdesk() {
        this.incidentId = null;
    }

    /**
     * Set request type
     * 0 - Import batch of incidents, e.g. 1000 incidents
     * 1 - Import one particular incident
     * 2 - Get visible replies of a particular incident
     * 3 - Sync categories / subcategories
     * 4 - Analytics of batch of incidents, e.g. Analytics of 1000 incidents
     * @param requestType Request type
     */
    public void setRequestType(Integer requestType) {
        this.requestType = requestType;
    }

    /**
     * @return request type
     */
    public Integer getRequestType() {
        return requestType;
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
