package com.wyona.katie.models;

public class URLMeta {

    private String url;
    private long importDate;
    private ContentType contentType;
    private String etag;
    private String checksum;

    /**
     *
     */
    public URLMeta(String url, long importDate, ContentType contentType, String etag, String checksum) {
        this.url = url;
        this.importDate = importDate;
        this.contentType = contentType;
        this.etag = etag;
        this.checksum = checksum;
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

    /**
     *
     */
    public String getETag() {
        return etag;
    }

    /**
     *
     */
    public String getChecksum() {
        return checksum;
    }
}
