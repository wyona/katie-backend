package com.wyona.katie.models;

/**
 * Webhook Payload sent by Website, e.g. https://wyona.atlassian.net
 */
public class WebhookPayloadWebsite extends WebhookPayload {

    private String pageUrl;

    /**
     *
     */
    public WebhookPayloadWebsite() {
        this.pageUrl = null;
    }

    /**
     *
     */
    public WebhookPayloadWebsite(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    /**
     * @param pageUrl Page URL, e.g. "https://www.stadt-zuerich.ch/gud/de/index/beratung_bewilligung/baubewilligung/fachthemen/energetische_massnahmen/energiegesetz.html"
     */
    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    /**
     * @return page URL, e.g. "https://www.stadt-zuerich.ch/gud/de/index/beratung_bewilligung/baubewilligung/fachthemen/energetische_massnahmen/energiegesetz.html"
     */
    public String getPageUrl() {
        return pageUrl;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Page URL: " + pageUrl);
        return sb.toString();
    }
}
