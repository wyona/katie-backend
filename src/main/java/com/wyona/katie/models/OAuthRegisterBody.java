package com.wyona.katie.models;

/**
 * Register body used by OAuth controller
 */
public class OAuthRegisterBody {

    private String clientName;
    private String[] redirectUris;
    private String[] grantTypes;
    private String[] responseTypes;
    private String tokenEndpointAuthMethod;

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

    /**
     *
     */
    public void setGrant_types(String[] grantTypes) {
        this.grantTypes = grantTypes;
    }

    /**
     *
     */
    public String[] getGrant_types() {
        return grantTypes;
    }

    /**
     *
     */
    public void setResponse_types(String[] responseTypes) {
        this.responseTypes = responseTypes;
    }

    /**
     *
     */
    public String[] getResponse_types() {
        return responseTypes;
    }

    /**
     * @param tokenEndpointAuthMethod Token endpoint auth method, e.g., "client_secret_basic"
     */
    public void setToken_endpoint_auth_method(String tokenEndpointAuthMethod) {
        this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
    }

    /**
     *
     */
    public String getToken_endpoint_auth_method() {
        return tokenEndpointAuthMethod;
    }
}
