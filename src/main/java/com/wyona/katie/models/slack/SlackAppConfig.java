package com.wyona.katie.models.slack;
  
import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class SlackAppConfig {

    private String clientId;
    private String clientSecret;
    private String signingSecret;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public SlackAppConfig() {
    }

    /**
     *
     */
    public void setClientId(String id) {
        this.clientId = id;
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
    public void setClientSecret(String secret) {
        this.clientSecret = secret;
    }

    /**
     *
     */
    public String getClientSecret() {
        return clientSecret;
    }

    /**
     *
     */
    public void setSigningSecret(String secret) {
        this.signingSecret = secret;
    }

    /**
     *
     */
    public String getSigningSecret() {
        return signingSecret;
    }

    /**
     *
     */
    @Override
    public String toString() {
        return "Client ID: " + clientId + ", Client Secret: " + clientSecret + ", Signing Secret: " + getSigningSecret();
    }
}
