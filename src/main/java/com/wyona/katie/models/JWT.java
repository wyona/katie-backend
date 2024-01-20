package com.wyona.katie.models;

/**
 *
 */
public class JWT {

    private String encodedToken;
    private JWTPayload payload;
    Boolean isExpired;

    /**
     *
     */
    public JWT(String encodedToken) {
        this.encodedToken = encodedToken;
        this.payload = new JWTPayload();
        this.isExpired = null;
    }

    /**
     * @param encodedToken Encoded token
     */
    public void setToken(String encodedToken) {
        this.encodedToken = encodedToken;
    }

    /**
     * Get encoded token
     */
    public String getToken() {
        return encodedToken;
    }

    /**
     *
     */
    public void setPayload(JWTPayload payload) {
        this.payload = payload;
    }

    /**
     *
     */
    public JWTPayload getPayload() {
        return payload;
    }

    /**
     *
     */
    public void setIsExpired(Boolean isExpired) {
        this.isExpired = isExpired;
    }

    /**
     *
     */
    public Boolean getIsExpired() {
        return this.isExpired;
    }
}
