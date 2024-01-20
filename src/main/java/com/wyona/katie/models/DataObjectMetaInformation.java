package com.wyona.katie.models;

import java.util.Date;

/**
 *
 */
public class DataObjectMetaInformation {

    private ContentType contentType;
    private Date created;
    private Date modified;

    /**
     *
     */
    public DataObjectMetaInformation() {
    }

    /**
     *
     */
    public DataObjectMetaInformation(ContentType contentType) {
        this.contentType = contentType;
    }

    /**
     * @param contentType Content type of data object, e.g. "application/json"
     */
    public void setContentType(String contentType) {
        this.contentType = ContentType.fromString(contentType);
    }

    /**
     * @return content type of data object, e.g. "application/json"
     */
    public String getContentType() {
        return contentType.toString();
    }
}
