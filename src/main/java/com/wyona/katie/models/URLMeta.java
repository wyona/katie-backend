package com.wyona.katie.models;

public class URLMeta {

    private String url;
    private long importDate;
    private ContentType contentType;

    /**
     *
     */
    public URLMeta(String url, long importDate, ContentType contentType) {
        this.url = url;
        this.importDate = importDate;
        this.contentType = contentType;
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
    public long getImportDate() {
        return importDate;
    }

    /**
     *
     */
    public ContentType getContentType() {
        return contentType;
    }
}
