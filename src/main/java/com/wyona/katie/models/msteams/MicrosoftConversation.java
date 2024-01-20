package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class MicrosoftConversation {

    private String id;
    private String name;
    private String conversationType;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MicrosoftConversation() {
    }

    /**
     *
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     *
     */
    public String getName() {
        return name;
    }

    /**
     * @param conversationType Conversation type, e.g. "personal"
     */
    public void setConversationType(String conversationType) {
        this.conversationType = conversationType;
    }

    /**
     * @return conversation type, e.g. "personal"
     */
    public String getConversationType() {
        return conversationType;
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
     *
     */
    @Override
    public String toString() {
        StringBuilder s = new StringBuilder("Name: " + name + ", Conversation type: " + conversationType + ", Id: " + id);
        return s.toString();
    }
}
