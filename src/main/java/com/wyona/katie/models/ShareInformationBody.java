package com.wyona.katie.models;

/**
 * Share information body used by share controller
 */
public class ShareInformationBody {

    private String information;
    private String url;
    private String keywords;

    private String hint;
    private String encryptedCredentials;
    private String clientSideEncryptionAlgorithm;

    /**
     *
     */
    public ShareInformationBody() {
    }

    /**
     *
     */
    public void setInformation(String information) {
        this.information = information;
    }

    /**
     *
     */
    public String getInformation() {
        return information;
    }

    /**
     *
     */
    public void setURL(String url) {
        this.url = url;
    }

    /**
     *
     */
    public String getURL() {
        return url;
    }

    /**
     *
     */
    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    /**
     *
     */
    public String getKeywords() {
        return keywords;
    }

    /**
     * @param hint Hint re credentials, e.g. "My Netflix credentials"
     */
    public void setCredentialsHint(String hint) {
        this.hint = hint;
    }

    /**
     * @return hint re credentials, e.g. "My Netflix credentials"
     */
    public String getCredentialsHint() {
        return hint;
    }

    /**
     *
     */
    public void setEncryptedCredentials(String encryptedCredentials) {
        this.encryptedCredentials = encryptedCredentials;
    }

    /**
     *
     */
    public String getEncryptedCredentials() {
        return encryptedCredentials;
    }

    /**
     * @param clientSideEncryptionAlgorithm Client side encryption algorithm, e.g. "aes-256"
     */
    public void setClientSideEncryptionAlgorithm(String clientSideEncryptionAlgorithm) {
        this.clientSideEncryptionAlgorithm = clientSideEncryptionAlgorithm;
    }

    /**
     * @return client side encryption algorithm, e.g. "aes-256"
     */
    public String getClientSideEncryptionAlgorithm() {
        return clientSideEncryptionAlgorithm;
    }
}
