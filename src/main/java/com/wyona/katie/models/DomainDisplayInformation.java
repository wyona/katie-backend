package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class DomainDisplayInformation {

    private String uuid;
    private String name;
    private String tagName;

    // TODO: Add additional information, e.g. short description, contact email, icon (also see Slack App Basic/Display information)

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public DomainDisplayInformation() {
    }

    /**
     * @param uuid Domain UUID, e.g. "e4ff3246-372b-4042-a9e2-d30f612d1244"
     * @param name Domain name, e.g. "Apache Lenya"
     * @param tagName Domain tag name, e.g. "apache-lenya"
     */
    public DomainDisplayInformation(String uuid, String name, String tagName) {
        this.uuid = uuid;
        this.name = name;
        this.tagName = tagName;
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
    public String getName() {
        return name;
    }

    /**
     *
     */
    public String getTagName() {
        return tagName;
    }
}
