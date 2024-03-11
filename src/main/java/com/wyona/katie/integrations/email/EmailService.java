package com.wyona.katie.integrations.email;

import com.fasterxml.jackson.databind.JsonNode;

import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;
import com.google.api.services.gmail.model.ModifyMessageRequest;
import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.*;

import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.services.gmail.Gmail;
import com.google.auth.Credentials;
import com.google.auth.http.HttpCredentialsAdapter;

//import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import com.google.api.services.gmail.GmailScopes;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;

@Slf4j
@Component
public class EmailService {

    @Autowired
    private MailerService mailerService;

    @Autowired
    private com.wyona.katie.mail.Gmail gmail;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private QuestionAnalyzerService questionAnalyzerService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private DataRepositoryService dataRepoService;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    // INFO: https://developers.google.com/gmail/api/auth/scopes
    private static final List<String> GMAIL_SCOPES = Collections.singletonList(GmailScopes.MAIL_GOOGLE_COM);
    //private static final List<String> GMAIL_SCOPES = Collections.singletonList(GmailScopes.GMAIL_LABELS);
    private static final String LABEL_UNREAD = "UNREAD";

    /**
     *
     */
    @Async
    public void processEmailsUnread(Context domain, boolean trustAllSSLCertificates, boolean includeFeedbackLinks, User user, String processId) {
        backgroundProcessService.startProcess(processId, "Retrieve and process unread emails ...", user.getId());

        backgroundProcessService.updateProcessStatus(processId, "Retrieve unread emails ...");
        int numberOfUnreadMessages = 0;
        Message[] messages = null;
        try {
            Message[] unreadMessages = getEmailsUnread(domain, trustAllSSLCertificates);
            if (unreadMessages != null) {
                numberOfUnreadMessages = unreadMessages.length;
                backgroundProcessService.updateProcessStatus(processId, numberOfUnreadMessages + " unread emails retrieved.");
            }
            messages = getEmailsMatchingReplyTo(domain, unreadMessages);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
        }

        int numberOfQuestionsDetected =  0;
        int numberOfAnswers = 0;
        if (messages != null) {
            backgroundProcessService.updateProcessStatus(processId, "Process " + messages.length + " unread matching emails ...");
            int counter = 0;
            for (Message message : messages) {
                analyticsService.logMessageReceived(domain.getId(), ChannelType.EMAIL, null); // TODO: Set channelId
                String question = null;
                try {
                    String body = mailerService.getBody(message);
                    question = detectQuestion(body, domain);
                } catch (java.io.IOException e) {
                    log.error(e.getMessage());
                    backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
                    try {
                        log.error("Email message with subject '" + message.getSubject() + "' does not seem to contain a body!");
                    } catch (Exception ex) {
                        log.error(ex.getMessage(), ex);
                        backgroundProcessService.updateProcessStatus(processId, ex.getMessage(), BackgroundProcessStatusType.ERROR);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
                }
                if (question != null) {
                    numberOfQuestionsDetected++;
                    try {
                        log.info("Question detected inside email with subject '" + message.getSubject() + "': " + question);

                        // INFO: We could also use the uuidEmail as channelRequestId directly and do not really need a mapping table, because the email address is contained by the persistently saved email message
                        String channelRequestId = UUID.randomUUID().toString();
                        String uuidEmail = UUID.randomUUID().toString();
                        dataRepoService.addEmailConversationValues(channelRequestId, domain.getId(), uuidEmail);

                        Date dateSubmitted = message.getSentDate();
                        String remoteAddress = "TODO:E-Mail";

                        // TODO
                        List<String> classifications = new ArrayList<String>();
                        boolean checkAuthorization = false; // TODO
                        String messageId = null; // TODO

                        // TODO: Check content type of email message
                        boolean isHtmlMessage = false;
                        ContentType requestedAnswerContentType = ContentType.TEXT_PLAIN;

                        List<ResponseAnswer> answers = qaService.getAnswers(question, classifications, messageId, domain, dateSubmitted, remoteAddress, ChannelType.EMAIL, channelRequestId, -1, -1, checkAuthorization, requestedAnswerContentType, includeFeedbackLinks, false);
                        if (answers != null && answers.size() > 0) {
                            numberOfAnswers++;
                            log.info("Answers found for email with subject '" + message.getSubject() + "'.");

                            if (domain.getAnswersMustBeApprovedByModerator()) {
                                log.info("Moderation approval enabled for domain '" + domain.getName() + "' (Id: " + domain.getId() + "), therefore do not return an answer.");
                                contextService.saveEmail(domain, uuidEmail, message);
                            } else {
                                Message replyMessage = mailerService.generateReply(message, answers, domain);
                                log.info("Send answer to '" + replyMessage.getAllRecipients()[0] + "' ...");
                                // TODO: Finish implementation using message as argument to send email
                                if (false) {
                                    mailerService.send(replyMessage);
                                }
                                mailerService.send(replyMessage.getAllRecipients()[0].toString(), domain.getMailSenderEmail(), replyMessage.getSubject(), mailerService.getBody(replyMessage), isHtmlMessage);
                            }
                        } else {
                            log.info("No answer found to email '" + message.getSubject() + "'.");

                            //contextService.saveEmail(domain, uuidEmail, message);

                            // TODO: Get language from matrix or question
                            Language language = Language.en;
                            String email = message.getAllRecipients()[0].toString();
                            contextService.answerQuestionByNaturalIntelligence(question, null, ChannelType.EMAIL, channelRequestId, email, null, language, null, remoteAddress, domain);
                        }
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
                    }
                } else {
                    try {
                        log.info("No question detected inside Email with subject: " + message.getSubject());
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                        backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
                    }
                }
                counter++;
                backgroundProcessService.updateProcessStatus(processId, counter + " matching emails processed.");
            }
        } else {
            backgroundProcessService.updateProcessStatus(processId, "No messages retrieved", BackgroundProcessStatusType.WARN);
        }

        StringBuilder stats = new StringBuilder();
        stats.append("match-reply-to: " + domain.getMatchReplyTo());
        stats.append(", number-of-unread-emails: " + numberOfUnreadMessages);
        stats.append(", number-of-emails-matched: " + messages.length);
        stats.append(", number-of-questions-detected: " + numberOfQuestionsDetected);
        stats.append(", number-of-answers: " + numberOfAnswers);

        backgroundProcessService.updateProcessStatus(processId, "All unread emails processed: " + stats.toString());
        backgroundProcessService.stopProcess(processId);
    }

    /**
     * Analyze text whether it might contain questions
     * @param text Text which might contain a question
     * @return question when text contains a question and null otherwise
     */
    private String detectQuestion(String text, Context domain) { // TODO: Return all questions in case text might contain more than one question
        // TODO: Improve algorithm to detect questions, see for example https://www.quora.com/Natural-Language-Processing-Whats-the-best-way-to-detect-if-a-piece-of-text-is-interrogative, https://stackoverflow.com/questions/53183467/nlpnatural-language-processing-how-to-detect-question-with-any-method
        AnalyzedMessage analyzedMessage = questionAnalyzerService.analyze(text, domain);
        if (analyzedMessage.getContainsQuestions()) {
            if (analyzedMessage.getQuestionsAndContexts().size() > 1) {
                log.info("TODO: Handle multiple questions ...");
            }
            return analyzedMessage.getQuestionsAndContexts().get(0).getQuestion().getSentence();
        }
        //log.info("Text '" + text + "' does not contain a question.");
        log.info("Email body does not contain a question.");
        return null;
    }

    /**
     * Get emails using IMAP or Gmail API which are unread
     */
    public Message[] getEmailsUnread(Context domain, boolean trustAllCertificates) throws Exception {
        log.info("Try to get emails associated with domain '" + domain.getName() + "' ...");

        IMAPConfiguration imapConfig = domain.getImapConfiguration();
        GmailConfiguration gmailConfig = domain.getGmailConfiguration();

        Message[] allUnreadMessages = null;
        if (imapConfig != null) {
            log.info("Try to get emails associated with domain '" + domain.getName() + "' using IMAP from server '" + imapConfig.getHostname() + ":" + imapConfig.getPort() + "' ...");
            allUnreadMessages = mailerService.getMessages(imapConfig.getHostname(), imapConfig.getPort(), imapConfig.getUsername(), imapConfig.getPassword(), trustAllCertificates);
        } else if (gmailConfig != null) {
            log.info("Try to get emails associated with domain '" + domain.getName() + "' using Gmail API ...");
            allUnreadMessages = getMessagesGmailService(gmailConfig);
            //allUnreadMessages = getMessagesGmailAPI(gmailConfig);
        } else {
            log.warn("Domain '" + domain.getId() + "' has neither IMAP nor Gmail configuration!");
        }

        log.info(allUnreadMessages.length + " unread messages.");

        return allUnreadMessages;
    }

    /**
     * Get emails which match configured reply-to address
     */
    public Message[] getEmailsMatchingReplyTo(Context domain, Message[] unreadMessages) throws Exception {
        List<Message> messages = new ArrayList<Message>();
        String configuredReplyTo = domain.getMatchReplyTo();
        if (unreadMessages != null) {
            for (Message message : unreadMessages) {
                log.info("Check E-Mail: Send date: " + message.getSentDate() + ", Subject: " + message.getSubject() + " From: " + message.getFrom()[0].toString() + " Reply-To: " + message.getReplyTo()[0]);
                if (configuredReplyTo != null) {
                    if (matchesWithAddress(message.getReplyTo(), configuredReplyTo, "ReplyTo") || matchesWithAddress(message.getAllRecipients(), configuredReplyTo, "AllRecipients")) {
                        messages.add(message);
                    } else {
                        log.info("Message '" + message.getSubject() + "' does not match configured reply-to '" + configuredReplyTo + "', therefore ignore it.");
                    }
                } else {
                    log.info("No 'match reply-to address' configured for domain '" + domain.getId() + "', therefore ignore all messages.");
                }
            }
        } else {
            log.info("No unread email messages available.");
        }

        log.info(messages.size() + " emails match configured reply-to address '" + configuredReplyTo + "'.");

        return messages.toArray(new Message[0]);
    }

    /**
     * @return true when configured reply-to address is macthing with one of the addresses, false otherwise
     */
    private boolean matchesWithAddress(Address[] addresses, String configuredReplyTo, String emailType) throws Exception {
        if (addresses != null) {
            for (Address address : addresses) {
                log.info("Check whether configured reply-to address matches with mail '" + emailType + "' address '" + address + "' ....");
                if (extractEmail(address).equals(configuredReplyTo)) {
                    return true;
                }
            }
        } else {
            log.warn("No '" + emailType + "' addresses available!");
        }
        return false;
    }

    /**
     * Extract email from address
     * @param address For example "Michael Wechner <michael.wechner@wyona.com>"
     * @return only email address, e.g. "michael.wechner@wyona.com"
     */
    private String extractEmail(Address address) throws Exception {
        String emailAddress = address.toString();
        int indexStartEmail =  emailAddress.indexOf("<");
        if (indexStartEmail >= 0) {
            int indexEndEmail = emailAddress.indexOf(">");
            log.info("Extract email from address '" + emailAddress + "' ...");
            return emailAddress.substring(indexStartEmail + 1, indexEndEmail);
        } else {
            return emailAddress;
        }
    }

    /**
     * Get all unread messages using Gmail API
     * @param gmailConfiguration Gmail configuration, including username of email account
     * @return all unread messages
     */
    private Message[] getMessagesGmailService(GmailConfiguration gmailConfiguration) throws Exception {
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

        Credentials credentials1 = gmail.getGmailCredentials(JSON_FACTORY, gmailConfiguration.getCredentialsPath(), gmailConfiguration.getUsername(), GMAIL_SCOPES);
        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials1);
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, requestInitializer).setApplicationName("Katie Gmail API").build();

        /*
        // INFO: CHAT, SENT, INBOX, IMPORTANT, TRASH, DRAFT, SPAM, CATEGORY_FORUMS, CATEGORY_UPDATES, CATEGORY_PERSONAL, CATEGORY_PROMOTIONS, CATEGORY_SOCIAL, STARRED, UNREAD
        // https://developers.google.com/gmail/api/guides/labels
        log.info("Try to get labels of user '" + gmailConfiguration.getUsername() + "' ...");
        ListLabelsResponse listResponse = service.users().labels().list(gmailConfiguration.getUsername()).execute();
        List<Label> labels = listResponse.getLabels();
        if (labels.isEmpty()) {
            log.info("No labels found.");
        } else {
            log.info("Labels:");
            for (Label label : labels) {
                log.info("Label name: " + label.getName());
            }
        }

         */

        List<Message> messages = new ArrayList<Message>();

        // INFO: https://developers.google.com/gmail/api/reference/rest/v1/users.messages/list
        List<com.google.api.services.gmail.model.Message> messageStubs = service.users().messages().list(gmailConfiguration.getUsername()).setQ("is:" + LABEL_UNREAD).execute().getMessages();
        if (messageStubs != null) {
            for (com.google.api.services.gmail.model.Message message : messageStubs) {
                log.info("\n\n");
                log.info("Process unread email Id: " + message.getId());

                Properties props = new Properties();
                Session session = Session.getDefaultInstance(props, null);
                Message emailMsg = new MimeMessage(session);

                // INFO: https://developers.google.com/gmail/api/reference/rest/v1/Format
                com.google.api.services.gmail.model.Message msg = service.users().messages().get(gmailConfiguration.getUsername(), message.getId()).setFormat("full").execute();

                MessagePart payload = msg.getPayload();

                for (MessagePartHeader header : payload.getHeaders()) {
                    log.debug(header.getName() + ":" + header.getValue());
                    if (header.getName().contains("Subject")) {
                        emailMsg.setSubject(header.getValue());
                    }
                    if (header.getName().contains("From")) {
                        emailMsg.setFrom(new InternetAddress(header.getValue()));
                    }
                    if (header.getName().contains("Date")) {
                        log.info("Date: " + header.getValue());
                        // TODO: emailMsg.setSentDate(header.getValue());
                        emailMsg.setSentDate(new Date());
                    }
                    if (header.getName().contains("Reply-To")) {
                        log.info("Set Reply-To address: " + header.getValue());
                        Address address = new InternetAddress(header.getValue());
                        Address[] addresses = new Address[1];
                        addresses[0] = address;
                        emailMsg.setReplyTo(addresses);
                        //emailMsg.setRecipient(Message.RecipientType.TO, address);
                        emailMsg.setRecipients(Message.RecipientType.TO, addresses);
                    }
                }

                log.info("SUBJECT: " + emailMsg.getSubject());

                log.info("Number of reply-to addresses: " + emailMsg.getReplyTo().length);
                if (emailMsg.getReplyTo().length == 0) {
                    log.warn("No Reply-To set, therefore set TODO ...");
                    Address address = new InternetAddress("michael.wechner@wyona.com"); // TODO: Replace hard coded email
                    Address[] addresses = new Address[1];
                    addresses[0] = address;
                    emailMsg.setReplyTo(addresses);
                } else {
                    log.info("Message has reply-to addresses set.");
                }

                // INFO: Get Message Body

                if (payload.getParts() != null) {
                    for (MessagePart msgPart : payload.getParts()) {
                        log.info("Message part mime type of email '" + message.getId() + "': " + msgPart.getMimeType());
                        if (msgPart.getMimeType().contains("text/plain")) {
                            String body = new String(org.apache.commons.codec.binary.Base64.decodeBase64(msgPart.getBody().getData()));
                            log.debug("BODY:\n" + body);
                            emailMsg.setText(body);
                        } else if (msgPart.getMimeType().contains("text/html")) {
                            //String body = new String(org.apache.commons.codec.binary.Base64.decodeBase64(msgPart.getBody().getData()));
                            //log.info("BODY as HTML:\n" + body);
                        } else {
                            log.info("PAYLOAD PART:\n" + msgPart.getBody().getData() + ", " + msgPart.getBody().getSize());
                        }
                    }
                } else {
                    log.warn("Payload does not contain any parts!");

                    if (payload.getBody().getSize() > 0) {
                        String pbody = new String(org.apache.commons.codec.binary.Base64.decodeBase64(payload.getBody().getData()));
                        log.info("PAYLOAD BODY:\n" + pbody);
                        emailMsg.setText(pbody);
                    } else {
                        log.warn("Payload does not contain a body!");
                    }
                }

                messages.add(emailMsg);

                log.info("Remove label '" + LABEL_UNREAD + "' from email '" + message.getId() + "' ...");
                ModifyMessageRequest modifyMessageRequest = new ModifyMessageRequest();
                List<String> removeLabels = new ArrayList<String>();
                removeLabels.add(LABEL_UNREAD);
                modifyMessageRequest.setRemoveLabelIds(removeLabels);
                service.users().messages().modify(gmailConfiguration.getUsername(), message.getId(), modifyMessageRequest).execute();
            }
        } else {
            log.info("No messages with label '" + LABEL_UNREAD + "'.");
        }

        return messages.toArray(new Message[0]);
    }

    /**
     *
     */
    private Message[] getMessagesGmailAPI(GmailConfiguration gmailConfiguration) throws Exception {
        // INFO: https://developers.google.com/identity/protocols/oauth2/service-account
        // TODO: https://developers.google.com/gmail/api/reference/rest/v1/users.messages/list
        // TODO: https://developers.google.com/gmail/api/reference/rest/v1/users.messages/get

        // https://gmail.googleapis.com/gmail/v1/users/me/messages

        String gmailApiHost = "https://gmail.googleapis.com";

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders(gmailConfiguration.getCredentialsPath(), gmailConfiguration.getUsername());
            HttpEntity<String> request = new HttpEntity<String>(headers);

            String requestUrl = gmailApiHost + "/gmail/v1" + "/users/me/messages";
            log.info("Get messages: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders(String gmailCredentials, String username) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Bearer " + getAccessToken(gmailCredentials, username));
        return headers;
    }

    /**
     * https://developers.google.com/identity/protocols/oauth2/service-account#delegatingauthority
     */
    private String getAccessToken(String gmailCredentials, String username) throws Exception {
        log.info("Load Gmail Credentials '" + gmailCredentials + "' ...");
        ServiceAccountCredentials sac = ServiceAccountCredentials.fromStream(new ClassPathResource(gmailCredentials).getInputStream());
        java.security.PrivateKey privateKey = sac.getPrivateKey();

        JWTPayload jwtPayload = new JWTPayload();
        jwtPayload.setIss(sac.getClientEmail());
        jwtPayload.setSub(username);
        //jwtPayload.setSub("michaelhanneswechner@gmail.com");
        HashMap<String, String> claims = new HashMap<String, String>();
        claims.put("aud", sac.getTokenServerUri().toString());
        claims.put("scope", "https://www.googleapis.com/auth/gmail.readonly");
        //claims.put("scope", "https://mail.google.com/"); // INFO: https://developers.google.com/gmail/api/auth/scopes
        jwtPayload.setPrivateClaims(claims);

        log.info("JWT payload: " + jwtPayload);

        String jwt = jwtService.generateJWT(jwtPayload, 3600, privateKey);
        log.info("JWT: " + jwt); // TODO: Comment or remove

        return getAccessTokenFromGoogleOauthServer(jwt);
    }

    /**
     * https://developers.google.com/identity/protocols/oauth2/service-account#httprest
     */
    private String getAccessTokenFromGoogleOauthServer(String jwt) {
        // TODO: Get access token from https://oauth2.googleapis.com/token
        String accessToken = "ya29.A0AVA9y1svJGQvBT74vTt7GgCCba7eM0At-vzfwd2sVeh1I1flhW9VlpPkLyttyjW9Y3a0CRCfTBt3pK0kCfsc_cQt-BDZ0TZl98nPH1UT-JjA8o-AeGx28p-S4X1u7NnjUgK18wssseFQZlY-VbvQMQUJlO1DaCgYKATASATASFQE65dr8yL3R3HiQoBGms0C5VbFA7w0163";
        return accessToken;
    }
}
