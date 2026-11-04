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
public class NotificationService {

    @Autowired
    private EmailSender smtpSender;

    @Autowired
    private Gmail gmail;

    @Autowired
    private Configuration configuration;

    @Autowired
    private XMLService xmlService;
    @Autowired
    private UsersXMLFileService usersXMLFileService;

    @Autowired
    private MailerService mailerService;

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
            mailerService.send(mailDefaultAdminAddress, fromEmail, mailSubjectTag + " " + subject, body, isHTMLMessage);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Notify domain members
     * @param roles List of roles
     */
    public boolean notifyDomainMembers(String domainId, List<RoleDomain> roles, String subject, String body, boolean isHTMLMessage) {
        boolean notificationSent = false;
        try {
            User[] users = xmlService.getMembers(domainId, false, false);
            for (User user : users) {
                RoleDomain role = user.getDomainRole();
                if (hasOneOfTheseRoles(user, roles)) {
                    // xmlService.getMembers() does not return email, etc. of user, therefore overwrite
                    user = usersXMLFileService.getIAMUserById(user.getId(), false, false);
                    user.setDomainRole(role);

                    log.info("Notify domain member '" + user.getFirstname() + "' (" + user.getEmail()+ ", " + user.getDomainRole() + ") of domain '" + domainId + "' about occured error(s) ...");
                    mailerService.send(user.getEmail(), null, subject, body.toString(), false);
                    notificationSent = true;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return notificationSent;
    }

    /**
     *
     */
    public void notifyUsers(User[] users, String subject, String message) {
        for (User user: users) {
            try {
                mailerService.send(user.getEmail(), mailDefaultSenderAddress, mailSubjectTag + " " + subject, message, true);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * @return true when user has one of the roles provided by the list and false otherwise
     */
    private boolean hasOneOfTheseRoles(User user, List<RoleDomain> roles) {
        for (RoleDomain role : roles) {
            if (user.getDomainRole() != null && user.getDomainRole().equals(role)) {
                return true;
            }
        }
        return false;
    }
}
