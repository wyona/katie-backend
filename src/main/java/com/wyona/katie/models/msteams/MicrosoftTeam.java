package com.wyona.katie.models.msteams;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class MicrosoftTeam {

    private String id;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public MicrosoftTeam() {
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
}
