package com.wyona.katie.models;

/**
 * Register body used by OAuth controller
 */
public class OAuthRegisterBody {

    private String clientName;
    private String[] redirectUris;

    /**
     *
     */
    public void setClient_name(String clientName) {
        this.clientName = clientName;
    }

    /**
     *
     */
    public String getClient_name() {
        return clientName;
    }

    /**
     *
     */
    public void setRedirect_uris(String[] redirectUris) {
        this.redirectUris = redirectUris;
    }

    /**
     *
     */
    public String[] getRedirect_uris() {
        return redirectUris;
    }
}
