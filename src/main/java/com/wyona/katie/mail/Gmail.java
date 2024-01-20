package com.wyona.katie.mail;

import com.wyona.katie.models.GmailConfiguration;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.GmailScopes;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.wyona.katie.models.GmailConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.mail.internet.MimeMessage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class Gmail {

    @Autowired
    private EmailSender smtpSender;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final List<String> GMAIL_SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);

    /**
     * https://developers.google.com/workspace/guides/create-credentials
     * https://myaccount.google.com/u/2/permissions
     * https://developers.google.com/identity/protocols/oauth2/service-account#delegatingauthority
     * @param jsonFactory
     * @param gmailCredentials Gmail credentials path, e.g. "google/katie-360121-d549df70ff6c.json"
     * @param username Gmail username, e.g. "michael.wechner@ukatie.com"
     * @return
     * @throws IOException
     */
    public Credentials getGmailCredentials(JsonFactory jsonFactory, String gmailCredentials, String username, List<String> scopes) throws IOException {
        log.info("Load Google Application Credentials '" + gmailCredentials + "' ...");

        InputStream credentialsJSON = new ClassPathResource(gmailCredentials).getInputStream();
        if (credentialsJSON == null) {
            throw new FileNotFoundException("Resource not found: " + gmailCredentials);
        }

        GoogleCredentials gc = GoogleCredentials.fromStream(credentialsJSON).createScoped(scopes).createDelegated(username);
        return gc;
    }

    /**
     * Send email using Gmail API
     * @param gmailConfiguration Gmail configuration, including username of email account
     */
    @Async
    public void sendEmailByGmailAPI(GmailConfiguration gmailConfiguration, String from, String to, String subject, String body) throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Credentials credentials1 = getGmailCredentials(JSON_FACTORY, gmailConfiguration.getCredentialsPath(), gmailConfiguration.getUsername(), GMAIL_SCOPES);
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials1);
        com.google.api.services.gmail.Gmail service = new com.google.api.services.gmail.Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer).setApplicationName("Katie Gmail API").build();

        com.google.api.services.gmail.model.Message message = generateMessage(from, to,subject, body);
        service.users().messages().send(gmailConfiguration.getUsername(), message).execute();

        log.info("Email to '" + to + "' sent :-)");
    }

    /**
     * Get gmail message
     */
    private com.google.api.services.gmail.model.Message generateMessage(String fromEmail, String toEmail, String subject, String text) throws Exception {

        // TODO: gmail api from address override https://stackoverflow.com/questions/57927771/change-from-address-while-sending-email-via-gmail-api

        MimeMessage message = smtpSender.generateMessage(toEmail, fromEmail, subject, text, true);

        // Encode and wrap the MIME message into a gmail message
        java.io.ByteArrayOutputStream buffer = new java.io.ByteArrayOutputStream();
        message.writeTo(buffer);
        byte[] rawMessageBytes = buffer.toByteArray();
        String encodedEmail = org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(rawMessageBytes);
        com.google.api.services.gmail.model.Message gMessage = new com.google.api.services.gmail.model.Message();
        gMessage.setRaw(encodedEmail);


        return gMessage;
    }
}
