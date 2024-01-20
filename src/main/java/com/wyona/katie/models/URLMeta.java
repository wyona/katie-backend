package com.wyona.katie.models;

public class URLMeta {

    private String url;
    private long importDate;

    /**
     *
     */
    public URLMeta(String url, long importDate) {
        this.url = url;
        this.importDate = importDate;
    }

    /**
     * Get URL of webpage containing QnAs
     */
    public String getUrl() {
        return url;
    }

    /**
     * Get date when QnAs were imported from URL
     */
    public long getIportDate() {
        return importDate;
    }
}
