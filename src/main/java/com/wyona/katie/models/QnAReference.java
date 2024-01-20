package com.wyona.katie.models;

public class QnAReference {

    private String domainId;
    private String uuid;

    /**
     *
     */
    public QnAReference(String domainId, String uuid) {
        this.domainId = domainId;
        this.uuid = uuid;
    }

    /**
     *
     */
    public String getDomainId() {
        return domainId;
    }

    /**
     *
     */
    public String getUuid() {
        return uuid;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return domainId + " / " + uuid;
    }
}
