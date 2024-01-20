package com.wyona.katie.models;

import com.wyona.katie.mail.EmailSenderConfig;
import lombok.extern.slf4j.Slf4j;

import com.wyona.katie.models.Context;

/**
 *
 */
@Slf4j
public class ServerConfiguration {

    private String environment;
    private String version;
    private NerImpl nerImplementation;
    private QuestionClassificationImpl questionClassificationImpl;
    private EmbeddingsImpl embeddingsImpl;
    private ReRankImpl reRankImpl;
    private String[] domainIDs;
    private String defaultHostnameMailBody;
    private String slackRedirectUri;
    private String microsoftRedirectUri;
    private EmailSenderConfig emailSenderConfig;
    private String emailSystemAdmin;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public ServerConfiguration() {
    }

    /**
     * @param version AskKatie webapp version
     * @param environment Environment where Katie is running, e.g. "cloud" or "on-premises"
     * @param emailSystemAdmin Email of system administrator
     */
    public ServerConfiguration(String environment, String version, String[] domainIDs, NerImpl nerImplementation, QuestionClassificationImpl questionClassificationImpl, EmbeddingsImpl embeddingsImpl, ReRankImpl reRankImpl, String defaultHostnameMailBody, String slackRedirectUri, String microsoftRedirectUri, EmailSenderConfig emailSenderConfig, String emailSystemAdmin) {
        this.environment = environment;
        this.version = version;
        this.domainIDs = domainIDs;
        this.nerImplementation = nerImplementation;
        this.questionClassificationImpl = questionClassificationImpl;
        this.embeddingsImpl = embeddingsImpl;
        this.reRankImpl = reRankImpl;
        this.defaultHostnameMailBody = defaultHostnameMailBody;
        this.slackRedirectUri = slackRedirectUri;
        this.microsoftRedirectUri = microsoftRedirectUri;
        this.emailSenderConfig = emailSenderConfig;
        this.emailSystemAdmin = emailSystemAdmin;
    }

    /**
     *
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     *
     */
    public void setVersion(String version) {
        this.version = version;
    }

    /**
     *
     */
    public String getVersion() {
        return version;
    }

    /**
     * Get IDs of all confgured domains
     */
    public String[] getDomainIDs() {
        return domainIDs;
    }

    /**
     *
     */
    public NerImpl getNERImplementation() {
        return nerImplementation;
    }

    /**
     *
     */
    public QuestionClassificationImpl getQuestionClassificationImpl() {
        return questionClassificationImpl;
    }

    /**
     *
     */
    public EmbeddingsImpl getEmbeddingsImpl() {
        return embeddingsImpl;
    }

    /**
     *
     */
    public ReRankImpl getReRankImpl() {
        return reRankImpl;
    }

    /**
     *
     */
    public String getDefaultHostnameMailBody() {
        return defaultHostnameMailBody;
    }

    /**
     *
     */
    public String getSlackRedirectUri() {
        return slackRedirectUri;
    }

    /**
     *
     */
    public String getMicrosoftRedirectUri() {
        return microsoftRedirectUri;
    }

    /**
     *
     */
    public EmailSenderConfig getEmailSenderConfig() {
        return emailSenderConfig;
    }

    /**
     * @return email of system administrator
     */
    public String getEmailSystemAdmin() {
        return emailSystemAdmin;
    }
}
