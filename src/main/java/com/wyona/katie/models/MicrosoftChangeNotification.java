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

/*
{
   "value":[
      {
         "subscriptionId":"3612f6ac-4bf6-4873-9b7a-2db238c3152b",
         "clientState":"secretClientValue",
         "resource":"/sites/5ad177f3-5385-4040-9545-370c658bb2d1/lists/e6db5fd8-0875-4211-8adf-efb98b3b6f9e",
         "tenantId":"c7e438db-e462-4c22-a90a-c358b16980b3",
         "resourceData":{
            "@odata.type":"#Microsoft.Graph.ListItem"
         },
         "subscriptionExpirationDateTime":"2026-01-13T19:37:20.909035+00:00",
         "changeType":"updated"
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
     * @param resource Resource URI, e.g., ID of modified sharepoint list "/sites/5ad177f3-5385-4040-9545-370c658bb2d1/lists/e6db5fd8-0875-4211-8adf-efb98b3b6f9e"
     */
    public void setResource(String resource) {
        this.resource = resource;
    }

    /**
     * @return resource URI, e.g., "/sites/5ad177f3-5385-4040-9545-370c658bb2d1/lists/e6db5fd8-0875-4211-8adf-efb98b3b6f9e"
     */
    public String getResource() {
        log.info("Resource URI: " + resource);
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
     * @param siteUrl, e.g. "/sites/KatieTest"
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
        StringBuilder s = new StringBuilder("Subscription Id: " + subscriptionId + ", Resource URI: " + resource + ", Tenant Id: " + tenantId + ", Site URL: " + siteUrl);
        return s.toString();
    }
}
