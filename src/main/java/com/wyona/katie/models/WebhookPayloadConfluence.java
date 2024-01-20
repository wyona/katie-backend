package com.wyona.katie.models;

/**
 * Webhook Payload sent by Confluence, e.g. https://wyona.atlassian.net
 */
public class WebhookPayloadConfluence extends WebhookPayload {

    // TODO: Confluence Payload examples

    /*
{
   "page":{
      "idAsString":"167182562",
      "creatorAccountId":"60dae1078a72bd006c5e3cbf",
      "spaceKey":"BDEV",
      "spaceId":167182346,
      "modificationDate":1681829304631,
      "lastModifierAccountId":"60e02bfe70941d00692dd5e1",
      "self":"https://wyona.atlassian.net/wiki/spaces/BDEV/overview",
      "id":167182562,
      "title":"Backend Development Home",
      "creationDate":1672916665028,
      "contentType":"page",
      "version":8,
      "creatorDisplayName":"Vanya Brucker",
      "spaceName":"Backend Development",
      "lastModifierDisplayName":"Wyona Admin"
   },
   "timestamp":1681829304645,
   "accountType":"customer",
   "userAccountId":"60e02bfe70941d00692dd5e1",
   "updateTrigger":"edit_page",
   "eventType":"page_updated",
   "transformedAt":"2023-04-18T14:48:25.081",
   "userDisplayName":"Wyona Admin"
}

{
   "page":{
      "idAsString":"194347009",
      "creatorAccountId":"60e02bfe70941d00692dd5e1",
      "spaceKey":"WTR",
      "spaceId":161251330,
      "modificationDate":1681976602080,
      "lastModifierAccountId":"60e02bfe70941d00692dd5e1",
      "self":"https://wyona.atlassian.net/wiki/spaces/WTR/pages/194347009/Missing+Resources",
      "id":194347009,
      "title":"Missing Resources",
      "creationDate":1681976523226,
      "contentType":"page",
      "version":1,
      "creatorDisplayName":"Wyona Admin",
      "spaceName":"Team Reports / Meeting Minutes",
      "lastModifierDisplayName":"Wyona Admin"
   },
   "accountType":"customer",
   "timestamp":1681976602128,
   "userAccountId":"60e02bfe70941d00692dd5e1",
   "eventType":"page_created",
   "transformedAt":"2023-04-20T07:43:22.659",
   "userDisplayName":"Wyona Admin"
}
     */

    private String eventType;
    private WebhookPayloadConfluencePage page;

    /**
     *
     */
    public WebhookPayloadConfluence() {
        this.eventType = null;
        this.page = null;
    }

    /**
     *
     */
    public WebhookPayloadConfluence(String eventType, WebhookPayloadConfluencePage page) {
        this.eventType = eventType;
        this.page = page;
    }

    /**
     * Set event type
     * @param eventType Event, e.g. "page_updated" or "page_created"
     */
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    /**
     *
     */
    public String getEventType() {
        return eventType;
    }

    /**
     *
     */
    public void setPage(WebhookPayloadConfluencePage page) {
        this.page = page;
    }

    /**
     *
     */
    public WebhookPayloadConfluencePage getPage() {
        return page;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Event type: " + eventType);
        return sb.toString();
    }
}
