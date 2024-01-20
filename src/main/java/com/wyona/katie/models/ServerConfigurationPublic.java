package com.wyona.katie.models;

import com.wyona.katie.mail.EmailSenderConfig;
import lombok.extern.slf4j.Slf4j;

import com.wyona.katie.models.Context;

/**
 *
 */
@Slf4j
public class ServerConfigurationPublic {

    private String environment;
    private String version;
    private String defaultHostnameMailBody;
    private String slackRedirectUri;
    private String microsoftRedirectUri;
    private String emailSystemAdmin;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public ServerConfigurationPublic() {
    }

    /**
     * @param version Katie webapp version
     * @param environment Environment where Katie is running, e.g. "cloud" or "on-premises"
     * @param emailSystemAdmin Email of system administrator
     */
    public ServerConfigurationPublic(String environment, String version, String defaultHostnameMailBody, String slackRedirectUri, String microsoftRedirectUri, String emailSystemAdmin) {
        this.environment = environment;
        this.version = version;
        this.defaultHostnameMailBody = defaultHostnameMailBody;
        this.slackRedirectUri = slackRedirectUri;
        this.microsoftRedirectUri = microsoftRedirectUri;
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
    public String getVersion() {
        return version;
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
     * @return email of system administrator
     */
    public String getEmailSystemAdmin() {
        return emailSystemAdmin;
    }
}
