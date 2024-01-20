package com.wyona.katie.mail;

import com.sun.mail.smtp.SMTPMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

import org.springframework.scheduling.annotation.Async;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class EmailSender {

    private final Session session;
    private final String defaultFromEmail;

    @Autowired
    public EmailSender(EmailSenderConfig config){

        Properties props = new Properties();

        props.put("mail.transport.protocol", "smtp");

        if (config.isStarttls()) {
            // WARN: Seems to cause trouble, when the mail server uses a self-signed certificate
            log.info("Set STARTTLS ...");
            props.put("mail.smtp.starttls.enable", true);
        } else {
            log.info("STARTTLS not set.");
        }

        // INFO: See java.security re TLSv1 and TLSv1.1
        log.warn("Use TLSv1.2 in order to prevent the error \"Could not convert socket to TLS\" with more recent JDK");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");

        log.info("Outgoing SMTP server: " + config.getHost() + ":" + config.getPort());
        props.put("mail.smtp.host", config.getHost());
        props.put("mail.smtp.port", config.getPort());

        defaultFromEmail = config.getDefaultFromEmailAddress();

        if (!config.getUsername().isEmpty() && !config.getPassword().isEmpty()) {
            props.put("mail.smtp.auth", true);
            log.info("SMTP authentication enabled.");

            session = Session.getInstance(
                    props,
                    new Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(config.getUsername(), config.getPassword());
                        }
                    }
            );
        } else {
            log.info("No credentials configured in order to connect with outgoing SMTP server '" + config.getHost() + ":" + config.getPort() + "'.");
            props.put("mail.smtp.auth", false);
            log.info("SMTP authentication disabled.");

            session = Session.getInstance(props);
        }
    }

    /**
     * Send email
     * @param message Message
     */
    @Async
    public void send(Message message) throws MessagingException {

        // TEST: Uncomment lines below to test thread
/*
        try {
            for (int i = 0; i < 5; i++) {
                log.info("Sleep for 2 seconds ...");
                Thread.sleep(2000);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
*/
        log.info("TODO: Implement send email to '" + message.getAllRecipients()[0] + "' using Message object ...");
        //message = new SMTPMessage(session);
        //message.setRecipients(Message.RecipientType.TO, InternetAddress.parse("michael.wechner@wyona.com"));
        //Transport.send(message);
    }

    /**
     * Send email
     * @param toEmail For example michael.wechner@wyona.com
     * @param fromEmail Sender email, e.g. "no-reply@wyona.com" or "Katie <no-reply@katie.qa>"
     * @param isHTMLMessage True when format of text message is HTML and false otherwise
     */
    @Async
    public void send(String toEmail, String fromEmail, String subject, String text, boolean isHTMLMessage) throws MessagingException {

        // TEST: Uncomment lines below to test thread
/*
        try {
            for (int i = 0; i < 5; i++) {
                log.info("Sleep for 2 seconds ...");
                Thread.sleep(2000);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
*/

        if (fromEmail == null) {
            fromEmail = defaultFromEmail;
        }

        log.info("Send email to '" + toEmail + "' (Outgoing mail server: " + session.getProperty("mail.smtp.host") + ") ...");

        Message message = generateMessage(toEmail, fromEmail, subject, text, isHTMLMessage);
        Transport.send(message);

        log.info("Email sent to '" + toEmail + "'.");
    }

    /**
     *
     */
    public MimeMessage generateMessage(String toEmail, String fromEmail, String subject, String text, boolean isHTMLMessage) throws MessagingException {
        MimeMessage message = null;

        if (isHTMLMessage) {
            message = new MimeMessage(session);
            log.info("From address: " + fromEmail);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setContent(text, "text/html;charset=UTF-8");
        } else {
            message = new SMTPMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject);
            message.setText(text);
        }

        return message;
    }
}
