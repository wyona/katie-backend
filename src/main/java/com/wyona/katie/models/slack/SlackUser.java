package com.wyona.katie.models.slack;
  
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SlackUser {

    private String id;
    private String username;
    private String name;
    private String teamId;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public SlackUser() {
    }

    /**
     *
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get user Id, e.g. "U018F80DU1C"
     */
    public String getId() {
        return id;
    }

    /**
     *
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     *
     */
    public String getUsername() {
        return username;
    }

    /**
     *
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get name, e.g. "michi123"
     */
    public String getName() {
        return name;
    }

    /**
     *
     */
    public void setTeam_id(String teamId) {
        this.teamId = teamId;
    }

    /**
     * @return for example 'T01848J69AP'
     */
    public String getTeam_id() {
        return teamId;
    }
}
