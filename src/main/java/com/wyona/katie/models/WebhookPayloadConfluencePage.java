package com.wyona.katie.models;

/**
 *
 */
public class WebhookPayloadConfluencePage {

    private String id;
    private String url;

    /**
     *
     */
    public WebhookPayloadConfluencePage() {
        this.id = null;
        this.url = null;
    }

    /**
     *
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
     * @param url Page URL, e.g. "https://wyona.atlassian.net/wiki/spaces/BDEV/overview"
     */
    public void setSelf(String url) {
        this.url = url;
    }

    /**
     * @return page URL, e.g. "https://wyona.atlassian.net/wiki/spaces/BDEV/overview"
     */
    public String getSelf() {
        return url;
    }
}
