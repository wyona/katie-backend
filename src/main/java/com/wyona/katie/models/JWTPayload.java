package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * See https://jwt.io/
 */
@Slf4j
public class JWTPayload {

    // INFO: Registered claims (http://self-issued.info/docs/draft-ietf-oauth-json-web-token.html#RegisteredClaimName)
    private String subject;
    private String issuer;
    private long expirationTime;
    private long issuedAt;

    // INFO: Hash map of private claims (http://self-issued.info/docs/draft-ietf-oauth-json-web-token.html#PrivateClaimName)
    private HashMap<String, String> privateClaims;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public JWTPayload() {
        subject = null;
        issuer = null;
        expirationTime = -1;
        issuedAt = -1;
    }

    /**
     * Set subject of token, e.g. UUID "71aa8dc6-0f19-4787-bd91-08fe1e863473" or email "louise@wyona.com"
     */
    public void setSub(String subject) {
        this.subject = subject;
    }

    /**
     * Get subject of token, e.g. UUID "71aa8dc6-0f19-4787-bd91-08fe1e863473" or email "louise@wyona.com"
     */
    public String getSub() {
        return subject;
    }

    /**
     * Set issuer of token, e.g. "Katie"
     */
    public void setIss(String issuer) {
        this.issuer = issuer;
    }

    /**
     * Get issuer of token, e.g. "Katie"
     */
    public String getIss() {
        return issuer;
    }

    /**
     * Get issued at (seconds since Unix epoch)
     */
    public long getIat() {
        return issuedAt;
    }

    /**
     * @param issuedAt Issued at (seconds since Unix epoch)
     */
    public void setIat(long issuedAt) {
        this.issuedAt = issuedAt;
    }

    /**
     * Get expiration time (seconds since Unix epoch)
     */
    public long getExp() {
        return expirationTime;
    }

    /**
     * @param expirationTime Expiration time (seconds since Unix epoch)
     */
    public void setExp(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    /**
     *
     */
    public void setPrivateClaims(HashMap<String, String> claims) {
        this.privateClaims = claims;
    }

    /**
     * Get private claims, e.g. "firstname", "lastname", "email"
     */
    public HashMap<String, String> getPrivateClaims() {
        return privateClaims;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"iss\":\"" + getIss() + "\",");
        sb.append("\"sub\":\"" + getSub() + "\",");
        sb.append("\"exp\":" + getExp() + ",");
        sb.append("\"iat\":" + getIat() + "");

        if (getPrivateClaims() != null) {
            for (Map.Entry claim : getPrivateClaims().entrySet()) {
                sb.append(",");
                sb.append("\"" + claim.getKey() + "\":\"" + claim.getValue() + "\"");
            }
        } else {
            log.info("No private claims set.");
        }
        sb.append("}");

        return sb.toString();
    }
}
