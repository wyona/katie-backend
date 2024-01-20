package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class DomainName {

    private String name;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public DomainName() {
    }

    /**
     * @param name Domain name, e.g. "FAQ Wyona" or "Domain of Michael Wechner"
     */
    public DomainName(String name) {
        this.name = name;
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
}
