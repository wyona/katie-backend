package com.wyona.katie.models;

public class URLMeta {

    private String url;
    private long importDate;
    private ContentType contentType;
    private String etag;
    private String previousEtag;
    private String checksum;
    private String previousChecksum;

    /**
     *
     */
    public URLMeta(String url, long importDate, ContentType contentType, String etag, String previousEtag, String checksum, String previousChecksum) {
        this.url = url;
        this.importDate = importDate;
        this.contentType = contentType;
        this.etag = etag;
        this.previousEtag = previousEtag;
        this.checksum = checksum;
        this.previousChecksum = previousChecksum;
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
    public String getPreviousEtag() {
        return previousEtag;
    }

    /**
     * Get checksum of content
     */
    public String getChecksum() {
        return checksum;
    }

    /**
     *
     */
    public String getPreviousChecksum() {
        return previousChecksum;
    }

    /**
     * Check whether content associated with URL was modified
     * @return true when content associated with URL was modified, otherwise return false
     */
    public boolean wasModified() {
        if (previousChecksum == null) {
            return true;
        } else {
            if (checksum != null && previousChecksum.equals(checksum)) {
                return false;
            } else {
                return true;
            }
        }
    }
}
