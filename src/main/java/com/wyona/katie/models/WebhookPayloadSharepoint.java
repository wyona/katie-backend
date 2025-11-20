package com.wyona.katie.models;

/*
https://learn.microsoft.com/en-us/graph/change-notifications-delivery-webhooks?tabs=http#receive-notifications

{
"value":[
                {
                "subscriptionId":"213e9e3c-59bc-4443-ad60-02c5041d228d",
                "clientState":"secretClientValue",
                "expirationDateTime":"2025-12-12T23:59:00.0000000Z",
                "resource":"0b0db340-f8b0-4ad6-8ebd-3e165f78a2cd",
                "tenantId":"c5dce9b8-8095-444d-9730-5ccb69b43413",
                "siteUrl":"/sites/KatieTest",
                "webId":"8bf0d69c-dace-4f0a-9f56-f249450bf2ac"
                }
]
}
 */

/**
 * Webhook Payload sent by Sharepoint, e.g. https://wyona.sharepoint.com/sites/KatieTest/Lists/Test%20List/AllItems.aspx
 * https://learn.microsoft.com/en-us/graph/change-notifications-delivery-webhooks?tabs=http#receive-notifications
 */
public class WebhookPayloadSharepoint extends WebhookPayload {

    private String value;

    /**
     *
     */
    public WebhookPayloadSharepoint() {
        this.value = null;
    }

    /**
     * @param value TODO
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * @return value
     */
    public String getValue() {
        return value;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Value: " + value);
        return sb.toString();
    }
}
