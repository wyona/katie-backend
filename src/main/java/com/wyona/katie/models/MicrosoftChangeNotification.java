package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/*
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
 * https://learn.microsoft.com/en-us/graph/change-notifications-delivery-webhooks?tabs=http#change-notification-example
 */
@Slf4j
public class MicrosoftChangeNotification {

    private String subscriptionId;
    private String clientState;
    private String expirationDateTime;
    private String resource;
    private String tenantId;
    private String siteUrl;
    private String webId;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MicrosoftChangeNotification() {
    }

    /**
     * @param subscriptionId Subscription Id, e.g. "213e9e3c-59bc-4443-ad60-02c5041d228d"
     */
    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    /**
     *
     */
    public String getSubscriptionId() {
        return subscriptionId;
    }

    /**
     *
     */
    public void setClientState(String clientState) {
        this.clientState = clientState;
    }

    /**
     *
     */
    public String getClientState() {
        return clientState;
    }

    /**
     *
     */
    public void setExpirationDateTime(String expirationDateTime) {
        this.expirationDateTime = expirationDateTime;
    }

    /**
     *
     */
    public String getExpirationDateTime() {
        return expirationDateTime;
    }

    /**
     * @param resource Resource Id, e.g. ID of modified sharepoint list "0b0db340-f8b0-4ad6-8ebd-3e165f78a2cd"
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     *
     */
    public String getResource() {
        return resource;
    }

    /**
     * @param tenantId Tenant Id, e.g. "c5dce9b8-8095-444d-9730-5ccb69b43413"
     */
    public void setTenantId(String tenantId) {this.tenantId = tenantId;}

    /**
     *
     */
    public String getTenantId() {
        return tenantId;
    }

    /**
     * @param siteUrl, e.g. ""/sites/KatieTest""
     */
    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    /**
     *
     */
    public String getSiteUrl() {
        return siteUrl;
    }

    /**
     *
     */
    public void setWebId(String webId) {
        this.webId = webId;
    }

    /**
     *
     */
    public String getWebId() {
        return webId;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Subscription Id: " + subscriptionId + ", Resource Id: " + resource + ", Tenant Id: " + tenantId + ", Site URL: " + siteUrl);
        return s.toString();
    }
}
