package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class OAuthStateInfo {

    private String clientId;
    private String state;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public OAuthStateInfo() {
    }

    /**
     * @param clientId Client Id, e.g., "1045897086839-7dhg0h1rbc9kdeklfdghtfj9r85p08dj.apps.googleusercontent.com" or "71098c9b-6ec0-483d-8c68-c98c7bef085e"
     * @param state State, e.g., "4dfa51ab3da5ab6efcad70bb4a5037dc37512ad3705e1a6201d0727552dace0b"
     */
    public OAuthStateInfo(String clientId, String state) {
        this.clientId = clientId;
        this.state = state;
    }

    /**
     *
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     *
     */
    public String getClientId() {
        return clientId;
    }

    /**
     *
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     *
     */
    public String getState() {
        return state;
    }
}
