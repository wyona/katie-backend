package com.wyona.katie.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EmailSenderConfig {

    @Value("${app.mail.host}")
    private String host;

    @Value("${app.mail.port}")
    private int port;

    @Value("${app.mail.username}")
    private String username;

    @Value("${app.mail.password}")
    private String password;

    @Value("${app.mail.smtp.starttls.enabled}")
    private String startTlsEnabled; // TODO: Use boolean

    @Value("${mail.default.sender.email.address}")
    private String defaultSenderEmailAddress;

    /**
     * @return true when starttls enabled, otherwise false
     */
    public boolean isStarttls() {
        log.info("Starttls enabled: " + startTlsEnabled);
        if (startTlsEnabled.equals("true")) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get hostname of outgoing mail server, e.g. "mail.wyona.com"
     */
    public String getHost() {
        return host;
    }

    /**
     * Get port of outgoing mail server, e.g. "587"
     */
    public int getPort() {
        return port;
    }

    /**
     * Get username, which is used for the credentials to send emails
     */
    public String getUsername() {
        return username;
    }

    /**
     * Get password, which is used for the credentials to send emails
     */
    protected String getPassword() { // INFO: Do not set to public, because otherwise the password will be accessible via REST Interface, whereas see ConfigurationController
        return password;
    }

    /**
     * @return default email sender address, e.g. "Katie <no-reply@wyona.com>"
     */
    public String getDefaultFromEmailAddress() {
        return defaultSenderEmailAddress;
    }
}
