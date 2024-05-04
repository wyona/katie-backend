package com.wyona.katie.services;

import com.wyona.katie.mail.EmailSender;
import com.wyona.katie.mail.Gmail;
import com.wyona.katie.models.*;
import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.Message;
import javax.mail.internet.MimeMultipart;
import javax.mail.search.FlagTerm;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;


@Slf4j
@Component
public class MailerService {

    @Autowired
    private EmailSender smtpSender;

    @Autowired
    private Gmail gmail;

    @Autowired
    private Configuration configuration;

    @Value("${mail.subject.tag}")
    private String mailSubjectTag;

    @Value("${mail.default.sender.email.address}")
    private String mailDefaultSenderAddress;

    @Value("${mail.default.admin.email.address}")
    private String mailDefaultAdminAddress;

    @Value("${outgoing.impl}")
    private String outgoingImpl;

    @Value("${google.service.account.credentials}")
    private String credentialsPath;
    @Value("${gmail.username}")
    private String username;

    //private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    //private static final List<String> GMAIL_SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);

    /**
     * Notify technical system administrator of Katie
     * @param subject Subject of notification message
     * @param body Optional plain text body
     * @param fromEmail Optional sender email address
     * @param isHTMLMessage True when format of message is HTML, false otherwise
     */
    public void notifyAdministrator(String subject, String body, String fromEmail, boolean isHTMLMessage) {
        if (body == null) {
            body = subject;
        }
        try {
            if (fromEmail == null) {
                fromEmail = mailDefaultSenderAddress;
            }
            send(mailDefaultAdminAddress, fromEmail, mailSubjectTag + " " + subject, body, isHTMLMessage);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     *
     */
    public void notifyUsers(User[] users, String subject, String message) {
        for (User user: users) {
            try {
                send(user.getEmail(), mailDefaultSenderAddress, mailSubjectTag + " " + subject, message, true);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Send email
     * @param email Email address to which email will be sent, e.g. michael.wechner@wyona.com
     * @param fromEmail Optional sender email, e.g. "no-reply@wyona.com" or "Katie <no-reply@katie.qa>"
     * @param subject Email subject
     * @param text Email body text
     * @param isHTMLMessage If true, then text is HTML
     */
    public void send(String email, String fromEmail, String subject, String text, boolean isHTMLMessage) throws MessagingException {
        if (fromEmail == null) {
            fromEmail = mailDefaultSenderAddress;
        }

        if (false) { // TODO: Add global switch (or per domain) to disable notifications
            log.info("Don't send email to '" + email + "', because notifications have been disabled.");
        } else {
            // TODO: Validate email address

            // TODO: Add entry to database in order to verify that email actually got sent, because emailSender is sending email by separate thread ...
            log.info("Send email to {} ...", email);

            if (outgoingImpl.equals("gmail")) {
                try {
                    GmailConfiguration gmailConfig = new GmailConfiguration(credentialsPath, username);
                    gmail.sendEmailByGmailAPI(gmailConfig, fromEmail, email, subject, text);
                    return;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else if (outgoingImpl.equals("smtp")) {
                smtpSender.send(email, fromEmail, subject, text, isHTMLMessage);
                return;
            } else {
                log.error("No such outgoing mail implementation: " + outgoingImpl);
                return;
            }
        }
    }

    /**
     * Send email using message as argument
     */
    public void send(Message message) throws MessagingException {
        log.info("TODO: Finish this implementation");

        if (outgoingImpl.equals("gmail")) {
            log.warn("TODO: Use Gmail implementation!");
        } else if (outgoingImpl.equals("smtp")) {
            smtpSender.send(message);
        } else {
            log.error("No such outgoing mail implementation: " + outgoingImpl);
        }
    }

    /**
     * Get unseen messages using IMAP
     * @param host Hostname of mail server, e.g. "imap.gmail.com" or "mx2.wyona.com"
     * @param port Port of mail server, e.g. 993
     * @param username Username of mail account, e.g. "michaelwechner@gmail.com" or "asamalafleur"
     * @param password Password of mail account
     * @param trustAllCertificates When set to true, then IMAP connector will trust all SSL certificates, alsp self-signed certificates
     * @return unseen messages
     */
    public Message[] getMessages(String host, int port, String username, String password, boolean trustAllCertificates) throws Exception {
        Properties props = new Properties();
        if (trustAllCertificates) {
            props.put("mail.imaps.ssl.trust", "*");
        }
        Session session = Session.getDefaultInstance(props);
        Store store = session.getStore("imaps"); // INFO: "imaps" is correct :-)
        // INFO: Please make sure that Java has TLS versions enabled, which are supported by the IMAP server (See https://superuser.com/questions/1649382/upgrade-from-openjdk-11-0-7-to-11-0-11-causes-sslhandshakeexception-no-appropri)
        // On MacOS enable "TLSv1, TLSv1.1" by removing these versions from "jdk.tls.disabledAlgorithms" inside the config file /Library/Java/JavaVirtualMachines/jdk-11.0.11.jdk/Contents/Home/conf/security/java.security
        store.connect(host, port, username, password);
        Folder inbox = store.getFolder( "INBOX" );
        inbox.open(Folder.READ_ONLY);

        // Fetch unseen messages from inbox folder
        Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

        // Sort messages from recent to oldest
        Arrays.sort( messages, ( m1, m2 ) -> {
            try {
                return m2.getSentDate().compareTo( m1.getSentDate() );
            } catch ( MessagingException e ) {
                throw new RuntimeException( e );
            }
        });

        return messages;
    }

    /**
     * Generate reply to email containing question(s)
     * @param message Original email message
     * @param answers Answers to questions of original email message
     * @return message to be sent as reply to original email message
     */
    public Message generateReply(Message message, List<ResponseAnswer> answers, Context domain) throws Exception {
        boolean replyToAll = false;
        Message reply = message.reply(replyToAll);

        String br = System.getProperty("line.separator");

        StringBuilder content = new StringBuilder();
        content.append("Hi" + br + br + "Katie (" + domain.getHost() + ") has found the following answer(s) to the question(s) in your email:" + br + br);
        for (int k = 0; k < answers.size(); k++) {
            content.append(((ResponseAnswer)answers.get(k)).getAnswer());
            content.append(br + br);
        }
        //content.append("On Fri, May 14, 2021 at 1:11 AM " + message.getFrom()[0] + " wrote:");
        content.append("On " + message.getSentDate() + " " + message.getFrom()[0] + " wrote:");
        content.append(br + br);
        content.append(addQuotedLinePrefix(getBody(message)));

        //log.info("Reply body/content (type: " + message.getContentType() + "): " + content);

        reply.setText(content.toString());
        return reply;
    }

    /**
     *
     */
    private String addQuotedLinePrefix(String text) {
        log.info("Add quoted line prefix (https://en.wikipedia.org/wiki/Posting_style)");
        return "> " + text.replaceAll(System.getProperty("line.separator"),"> ");
    }

    /**
     * Get body of email message
     * @return TODO
     */
    public String getBody(Message message) throws Exception {
        String body = "";
        Object content = message.getContent();
        if (content instanceof String) {
            body = (String)content;
        } else if(content instanceof MimeMultipart) {
            MimeMultipart mp = (MimeMultipart)content;
            body = getTextFromMimeMultipart(mp);
        } else {
            log.error("Content type '" + message.getContentType() + "' not supported!");
        }
        return body;
    }

    /**
     * Get content as string from MimeMultipart
     * @return TODO
     */
    private String getTextFromMimeMultipart(MimeMultipart mimeMultipart)  throws Exception {
        String result = "";
        int count = mimeMultipart.getCount();
        for (int i = 0; i < count; i++) {
            BodyPart bodyPart = mimeMultipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result = result + "\n" + bodyPart.getContent();
                break; // without break same text appears twice in my tests
            } else if (bodyPart.isMimeType("text/html")) {
                String html = (String) bodyPart.getContent();
                result = result + "\n" + Utils.convertHtmlToPlainText(html);
            } else if (bodyPart.getContent() instanceof MimeMultipart){
                result = result + getTextFromMimeMultipart((MimeMultipart)bodyPart.getContent());
            }
        }
        return result;
    }

    /**
     * Get email template
     * @param name Template name, e.g. "qna-needs-approval_email_"
     * @param language Two-letter language code of template content
     * @param domain Domain which might contain custom templates
     * @return template object
     */
    public Template getTemplate(String name, Language language, Context domain) throws Exception {
        log.info("Reset to loading global email template ...");
        // TODO: Is setTemplateLoader thread safe?!
        configuration.setTemplateLoader(new ClassTemplateLoader(getClass(), "/templates"));

        if (language == null) {
            log.warn("No language provided!");
        }

        String DEFAULT_LANG = "en";
        try {
            if (domain != null) {
                File customTemplatesDir = domain.getEmailTemplatesDataPath();
                File customTemplate = new File(customTemplatesDir, name + language + ".ftl");
                File customTemplateDefault = new File(customTemplatesDir, name + DEFAULT_LANG + ".ftl");
                if (customTemplate.isFile() || customTemplateDefault.isFile()) {
                    FileTemplateLoader domainTemplateLoader = new FileTemplateLoader(customTemplatesDir);
                    configuration.setTemplateLoader(domainTemplateLoader); // See https://freemarker.apache.org/docs/pgui_config_templateloading.html
                    if (customTemplate.isFile()) {
                        log.info("Use custom email template: " + customTemplate.getAbsolutePath());
                        return configuration.getTemplate(name + language + ".ftl");
                    } else {
                        log.info("Use custom email default template: " + customTemplateDefault.getAbsolutePath());
                        return configuration.getTemplate(name + DEFAULT_LANG + ".ftl");
                    }
                } else {
                    log.info("No custom email template '" + customTemplate.getAbsolutePath() + "' for domain '" + domain.getId() + "' exists, therefore use default email template ...");
                    return configuration.getTemplate(name + language + ".ftl");
                }
            } else {
                log.info("No domain provided, therefore fallback to default email template ...");
                return configuration.getTemplate(name + language + ".ftl");
            }
        } catch(Exception e) {
            //log.error(e.getMessage(), e);
            log.info("No email template '" + name + "' for language '" + language + "', therefore fallback to default language '" + DEFAULT_LANG + "' ...");
            return configuration.getTemplate(name + DEFAULT_LANG + ".ftl");
        }
    }
}
