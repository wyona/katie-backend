package com.wyona.katie.models;

//import com.wyona.katie.models.MicrosoftChangeNotification;

/**
 * Webhook Payload sent by Sharepoint, e.g. https://wyona.sharepoint.com/sites/KatieTest/Lists/Test%20List/AllItems.aspx
 * https://learn.microsoft.com/en-us/graph/change-notifications-delivery-webhooks?tabs=http#receive-notifications
 */
public class WebhookPayloadSharepoint extends WebhookPayload {

    private MicrosoftChangeNotification[] value;

    /**
     *
     */
    public WebhookPayloadSharepoint() {
        this.value = null;
    }

    /**
     * @param value Array of change notifications
     */
    public void setValue(MicrosoftChangeNotification[] value) {
        this.value = value;
    }

    /**
     * @return value
     */
    public MicrosoftChangeNotification[] getValue() {
        return value;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (MicrosoftChangeNotification changeNotification : value) {
            sb.append("Change notification: " + changeNotification);
        }
        return sb.toString();
    }
}
