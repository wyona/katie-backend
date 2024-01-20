package com.wyona.katie.models;

/**
 *
 */
public enum ContentType {

    // INFO: enum constants calling the enum constructor
    TEXT_PLAIN("text/plain"),
    TEXT_HTML("text/html"),
    TEXT_MARKDOWN("text/markdown"),
    TEXT_TOPDESK_HTML("text/x.topdesk-html"), // TODO: Consider text/x-topdesk-html (see https://en.wikipedia.org/wiki/Media_type)
    TEXT_SLACK_MRKDWN("text/x-slack-mrkdwn"),
    APPLICATION_JSON("application/json"),
    APPLICATION_PDF("application/pdf");

    private String contentType;

    /**
     * @param contentType Content type, e.g. "text/plain"
     */
    private ContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * @return content type, e.g. "text/plain"
     */
    @Override
    public String toString() {
        return contentType;
    }

    /**
     * @param text Content type as string, e.g. "text/plain"
     * @return content type as enum, e.g. ContentType.TEXT_PLAIN
     */
    public static ContentType fromString(String text) {
        for (ContentType ct : ContentType.values()) {
            if (ct.toString().equals(text)) {
                return ct;
            }
        }
        return null;
    }
}
