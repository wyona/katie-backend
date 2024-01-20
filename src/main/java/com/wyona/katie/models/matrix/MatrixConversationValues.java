package com.wyona.katie.models.matrix;

import java.util.Date;

public class MatrixConversationValues {

    private String uuid;
    private String domainId;

    private String userId;
    private String roomId;
    private String eventId;

    /**
     * @param userId Matrix user Id, e.g. @michaelhanneswechner:matrix.org
     * @param roomId Matrix room Id, e.g. !kwQFREBhkbipmDDUio:matrix.org
     * @param eventId Matrix event Id, e.g. $72wbRvFnXb6QYQYUdwjGnDUxVjBG_eOgHlzbT1fcVZ0
     */
    public MatrixConversationValues(String userId, String roomId, String eventId) {
        this.userId = userId;
        this.roomId = roomId;
        this.eventId = eventId;
    }

    /**
     * @param uuid UUID of resubmitted question
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
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
    public String getDomainId() {
        return domainId;
    }

    /**
     * @param domainId Domain Id resubmitted question is associated with
     */
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    /**
     * Get Matrix user ID (https://matrix.org/docs/spec/appendices#user-identifiers, e.g. @michaelhanneswechner:matrix.org)
     */
    public String getUserId() {
        return userId;
    }

    /**
     *
     */
    public String getRoomId() {
        return roomId;
    }

    /**
     *
     */
    public String getEventId() {
        return eventId;
    }
}
