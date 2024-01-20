package com.wyona.katie.models;

/**
 * Ask question contact information
 */
public class AskQuestionContactInfo {

    private String email;

    private String fcmToken;
    private String answerLinkType;

    private String webhookEchoData;

    /**
     *
     */
    public AskQuestionContactInfo() {
    }

    /**
     * @param email E-Mail of user asking question
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return e-mail of user asking question
     */
    public String getEmail() {
        return email;
    }

    /**
     *
     */
    public void setFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     *
     */
    public String getFcmToken() {
        return fcmToken;
    }

    /**
     *
     */
    public void setAnswerLinkType(String answerLinkType) {
        this.answerLinkType = answerLinkType;
    }

    /**
     *
     */
    public String getAnswerLinkType() {
        return answerLinkType;
    }

    /**
     * @param webhookEchoData Data to be sent to configured webhook, see for example https://github.com/wyona/katie-4-faq/blob/main/clients/katie4faq-nodejs-proxy/src/proxy.ts used by Veeting
     */
    public void setWebhookEchoData(String webhookEchoData) {
        this.webhookEchoData= webhookEchoData;
    }

    /**
     * @return data o be sent to configured webhook, see for example https://github.com/wyona/katie-4-faq/blob/main/clients/katie4faq-nodejs-proxy/src/proxy.ts used by Veeting
     */
    public String getWebhookEchoData() {
        return webhookEchoData;
    }
}
