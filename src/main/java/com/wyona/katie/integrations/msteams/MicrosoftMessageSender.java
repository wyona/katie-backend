package com.wyona.katie.integrations.msteams;

import com.wyona.katie.config.RestProxyTemplate;
import com.wyona.katie.integrations.CommonMessageSender;
import com.wyona.katie.integrations.msteams.services.MicrosoftAuthorizationService;
import com.wyona.katie.integrations.msteams.services.MicrosoftDomainService;
import com.wyona.katie.models.*;
import com.wyona.katie.models.msteams.*;
import com.wyona.katie.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Async;

import lombok.extern.slf4j.Slf4j;

import java.net.URLEncoder;
import java.util.*;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.wyona.katie.integrations.msteams.services.MicrosoftDomainService;

/**
 * Message sender (back to Microsoft)
 */
@Slf4j
@Component
public class MicrosoftMessageSender extends CommonMessageSender  {

    private static final String remoteAddress = "TODO:MS-Teams";

    private static final String ACTION_SEPARATOR = "::";

    @Value("${ms.oauth.url}")
    private String oauthUrl;

    @Value("${ms.grant_type}")
    private String grantType;

    @Value("${ms.client.id}")
    private String clientId;

    @Value("${ms.client.secret}")
    private String clientSecret;

    @Value("${ms.scope}")
    private String scope;

    @Value("${mail.default.sender.email.address}")
    private String defaultSenderEmailAddress;

    @Value("${new.context.mail.body.host}")
    private String defaultHostnameMailBody;

    @Value("${ms.katie.username}")
    private String usernameTechnicalUser;

    @Autowired
    private IAMService iamService;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private QuestionAnalyzerService questionAnalyzerService;

    @Autowired
    private WebhooksService webhooksService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private DataRepositoryService dataRepoService;

    @Autowired
    private MicrosoftDomainService domainService;

    @Autowired
    private MicrosoftAuthorizationService authorizationService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private MailerService mailerService;

    @Autowired
    private ResourceBundleMessageSource messageSource;

    @Autowired
    private RestProxyTemplate restProxyTemplate;

    /**
     * Send response back to Microsoft (https://docs.microsoft.com/en-us/azure/bot-service/rest-api/bot-framework-rest-connector-quickstart?view=azure-bot-service-4.0)
     */
    @Async
    public void sendResponse(MicrosoftBotMessage message) {
        String locale = message.getLocale();

        String channelRequestId = UUID.randomUUID().toString();
        String teamId = null;
        String channelId = null;
        if (message.getChannelData() != null) {
            teamId = message.getChannelData().getTeamsTeamId();
            if (message.getChannelData().getTeam() != null) {
                // https://teams.microsoft.com/l/channel/19%3a-FDvruvsgC5v3vHi4grkmIg0l79p7JQi4_58ek31w7s1%40thread.tacv2/Allgemein?groupId=d8ac9375-83fc-43b7-9297-feec5ba0fbd7&tenantId=2079ee81-b3cb-494e-8f00-4c29d74133ad
                teamId = message.getChannelData().getTeam().getId();
                //channelId = message.getChannelData().getTeamsChannelId();
                channelId = message.getChannelData().getChannel().getId();
                String tenantId = message.getChannelData().getTenant().getId();
                log.info("Tenant Id: " + tenantId);
            }
            log.info("Team Id: " + teamId + ", Channel Id: " + channelId);
        }
        // TODO: Add tenant Id to conversation values
        MSTeamsConversationValues convValues = new MSTeamsConversationValues(channelRequestId, teamId, channelId, message.getServiceUrl(), message.getConversation().getId(), message.getId(), message.getRecipient().getId(), message.getFrom().getId());
        MicrosoftResponse responseMsg = new MicrosoftResponse();

        MessageValue value = message.getValue();
        if (value != null) {
            String[] actionParts = value.getMessage().split(ACTION_SEPARATOR);
            ChannelAction action = ChannelAction.valueOf(actionParts[0]);
            if (action.equals(ChannelAction.REQUEST_INVITATION)) {
                log.info("Send invitation request for user registration to Katie administrators ...");
                String userName = actionParts[2];
                String userId = actionParts[1];
                String domainId = value.getDomainid(); // MessageValue.TEXT_INPUT_DOMAIN_ID
                responseMsg = getRequestInvitationInteractionResponse(responseMsg, userName, userId, domainId);
                send(convValues, message.getConversation().getName(), message.getRecipient(), message.getFrom(), responseMsg);
                return;
            }
        }

        try {
            Context domain = null;
            if (teamId != null) {
                log.info("Team Id: " + teamId);
                domain = domainService.getDomain(teamId);
            } else {
                log.info("No team Id available, either because MS Teams user sends direct message or because message was sent from Azure Portal Web Chat.");
            }

            AnalyzedMessage analyzedMessage = getAnalyzedMessage(message.getText(), getMentionEntity(message), domain);
            String question = null;
            if (analyzedMessage != null) {
                question = analyzedMessage.getMessage();
            } else {
                log.info("No question detected!");
            }

            String conversationType = message.getConversation().getConversationType();
            log.info("Conversation type: " + conversationType); // INFO: For example "channel" or "personal"
            if (conversationType != null) {
                if (conversationType.equals("channel")) {
                    responseMsg = getAnswerForChannelConversation(message, question, responseMsg, locale, convValues, domain, teamId, channelId);
                } else if (conversationType.equals("personal")) {
                    log.info("No team Id available, conversation type is 'personal', therefore, MS Teams user '" + message.getFrom().getName() + " / " + message.getFrom().getId() + "' chats directly with Katie.");
                    responseMsg = getAnswerForPersonalConversation(message, question, responseMsg, convValues);
                } else {
                    String msg = "No such conversation type '" + conversationType + "' implemented!";
                    log.warn(msg);
                    responseMsg.setText(msg);
                }
            } else {
                // INFO: Message probably from Azure Portal Web Chat
                if (message.getChannelId() == ChannelId.webchat) {
                    log.info("Message has been sent from Azure Portal Web Chat.");
                }
                String msg = "Thanks for sending a message ('" + message.getText() + "') to Katie! Unfortunately your message does not contain a team Id and the conversation type is not 'personal', but '" + conversationType + "', therefore Katie does not know how to handle your message.";
                log.info(msg);
                responseMsg.setText(msg);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            responseMsg.setText(e.getMessage());
        }

        send(convValues, message.getConversation().getName(), message.getRecipient(), message.getFrom(), responseMsg);
    }

    /**
     * Check whether message contains @mention '<at>katie</at>'
     */
    private MicrosoftEntity getMentionEntity(MicrosoftBotMessage message) {
        if (message.getEntities() != null && message.getEntities().length > 0) {
            for (MicrosoftEntity entity: message.getEntities()) {
                log.info("Entity type: " + entity.getType());
                if (entity.getType().equals("mention")) {
                    return entity;
                }
            }
        }
        return null;
    }

    /**
     * Get answer for when MS Teams user sends message to Katie from within a channel
     * @param teamId Team Id, e.g. "19:Jq32E...E781@thread.tacv2"
     */
    private MicrosoftResponse getAnswerForChannelConversation(MicrosoftBotMessage message, String question, MicrosoftResponse responseMsg, String lang, MSTeamsConversationValues convValues, Context domain, String teamId, String channelId) throws Exception {
        String msTeamsUserId = message.getFrom().getId();
        try {
            log.info("Login as technical user ...");
            authService.login(usernameTechnicalUser, null);
            log.info("Signed in as technical Katie user '" + authService.getUserId() + "', whereas MS Teams user Id is '" + msTeamsUserId + "'.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        if (question != null) {
            if (domain == null) {
                String userId = message.getFrom().getId();
                log.info("MS Teams User Id: " + userId);

                String userName = message.getFrom().getName();
                log.info("MS Teams User Name: " + userName);

                log.warn("No Katie domain connected yet with MS Teams '" + teamId + "', therefore ask user (Locale: " + message.getLocale() + ") whether domain should be created automatically or connected with an existing domain ...");
                responseMsg.setText(messageSource.getMessage("hi.my.name.is.katie.ask.create.connect.domain", null, Utils.getLocale(lang)));

                // INFO: Buttons to connect with new or existing knowledge base
                MicrosoftAdaptiveCard notConnectedWithKBYet = new MicrosoftAdaptiveCard(messageSource.getMessage("not.connected.yet.with.a.domain", null, Utils.getLocale(lang)));

                MicrosoftAdaptiveCardActionSubmit createAndConnectWithNewDomain = new MicrosoftAdaptiveCardActionSubmit(messageSource.getMessage("create.new.domain", null, Utils.getLocale(lang)));
                createAndConnectWithNewDomain.setData(new MessageValue(ChannelAction.CREATE_DOMAIN + ACTION_SEPARATOR + teamId + ACTION_SEPARATOR + userName));
                notConnectedWithKBYet.addAction(createAndConnectWithNewDomain);

                MicrosoftAdaptiveCardActionSubmit connectWithExistingDomain = new MicrosoftAdaptiveCardActionSubmit(messageSource.getMessage("connect.with.existing.domain", null, Utils.getLocale(lang)));
                connectWithExistingDomain.setData(new MessageValue(ChannelAction.CONNECT_DOMAIN + ACTION_SEPARATOR + teamId + ACTION_SEPARATOR + userName + ACTION_SEPARATOR + userId));
                notConnectedWithKBYet.addAction(connectWithExistingDomain);

                responseMsg.addAttachment(new MicrosoftAttachment(notConnectedWithKBYet));
            } else {
                log.info("Try to get answer from Katie domain '" + domain.getId() + "' for channel chat with MS Teams Id '" + teamId + "' ...");
                responseMsg = getAnswerMessage(message, domain, question, responseMsg, teamId, channelId);
            }
        } else {
            MessageValue value = message.getValue();
            if (value != null) {
                String[] actionParts = value.getMessage().split(ACTION_SEPARATOR);
                ChannelAction action = ChannelAction.valueOf(actionParts[0]);
                log.info("Channel action: " + action);

                String userName = null;
                if (actionParts.length >= 3) {
                    userName = actionParts[2];
                } else {
                    log.error("No MS Teams username available!");
                }

                if (action.equals(ChannelAction.CREATE_DOMAIN)) {
                    log.info("Execute action '" + action + "' ...");
                    // TODO: Generate Input Form asking for email address os user, whereas see SlackMessageSender.CHANNEL_VIEW_CREATE_DOMAIN
                    String _teamId = actionParts[1];
                    log.info("Team Id: " + _teamId);
                    responseMsg.setText(requestCreateDomain(_teamId, userName));
                } else if (action.equals(ChannelAction.CONNECT_DOMAIN)) {
                    log.info("Execute action '" + action + "' ...");
                    //TODO: Generate Input Form asking for domain id, whereas see SlackMessageSender.CHANNEL_VIEW_CONNECT_DOMAIN
                    String domainId = "TODO"; //message.getValue().getDomainid();
                    String userId = actionParts[3];
                    if (!iamService.usernameExists(new Username(userId))) {
                        getRequestInvitationInteractionResponse(responseMsg, userName, userId, domainId);
                    } else {
                        log.info("MS Teams user '" + userId + "' already has a Katie account.");
                    }
                    String _teamId = actionParts[1];
                    log.info("Team Id: " + _teamId);
                    responseMsg.setText(requestConnectExistingDomain(_teamId, userName));
                } else {
                    responseMsg = getAnswerForActionRequest(message, responseMsg, convValues, domain.getId());
                }
            } else {
                log.warn("Neither question nor action!");
                responseMsg.setText(messageSource.getMessage("neither.question.nor.action.detected", null, Utils.getLocale(lang)));
                responseMsg.setTextFormat("plain");
            }
        }

        return responseMsg;
    }

    /**
     * Get answer for direct (1:1) chat (User sending message directly to Katie)
     * @param message TODO
     * @param question Question asked by MS Teams user
     * @param responseMsg TODO
     * @param convValues TODO
     */
    private MicrosoftResponse getAnswerForPersonalConversation(MicrosoftBotMessage message, String question, MicrosoftResponse responseMsg, MSTeamsConversationValues convValues) throws Exception {
        log.info("Generate answer for personal conversation ...");

        String userId = message.getFrom().getId();
        String locale  = message.getLocale();
        User user = iamService.getUserByUsername(new Username(userId), false, false);
        if (user != null) {
            try {
                log.info("Login with ID of MS Teams user ...");
                authService.login(user.getUsername(), null);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            String[] domainIds = contextService.getDomainIDsUserIsMemberOf(user);
            if (domainIds.length > 0) {
                for (String domainId: domainIds) {
                    analyticsService.logMessageReceived(domainId, ChannelType.MS_TEAMS, null); // TODO: Set channelId
                }
            }
        } else {
            String tenantId = message.getChannelData().getTenant().getId();
            log.warn("MS Teams user Id '" + userId + "' of MS Teams tenant '" + tenantId + "' is not registered yet with Katie!");
            String[] args = new String[2];
            args[0] = "<strong>" + userId + "</strong>";
            args[1] = defaultHostnameMailBody;
            responseMsg.setText(messageSource.getMessage("user.id.not.registered", args, Utils.getLocale(locale)));
            responseMsg.setTextFormat("xml");

            // WARNING: For security reasons do not auto-register users, but register users only by invitation!
            MicrosoftAdaptiveCard requestInvitationCard = getRequestInvitationCard(Utils.getLocale(message.getLocale()), userId, message.getFrom().getName());
            responseMsg.addAttachment(new MicrosoftAttachment(requestInvitationCard));

            return responseMsg;
        }

        if (question != null) {
            String[] domainIds = contextService.getDomainIDsUserIsMemberOf(user);
            if (domainIds.length == 0) {
                String[] args = new String[1];
                args[0] = "<strong>" + user.getUsername() + "</strong>";
                responseMsg.setText(messageSource.getMessage("cannot.answer.because.user.id.not.linked.with.domain", args, Utils.getLocale(locale)));
                responseMsg.setTextFormat("xml");

                String lang = message.getLocale();
                MicrosoftAdaptiveCard notDomainMemberYet = getRequestBecomeDomainMemberCard(Utils.getLocale(lang), user.getUsername(), message.getFrom().getName());
                responseMsg.addAttachment(new MicrosoftAttachment(notDomainMemberYet));
            } else if (domainIds.length == 1) {
                Context domain = contextService.getContext(domainIds[0]);
                log.info("MS Teams user '" + user.getId() + "' is member only of Katie domain '" + domain.getId() + "', therefore try to get answer from this domain ...");
                responseMsg = getAnswerMessage(message, domain, question, responseMsg, null, null);
            } else {
                // TODO / TBD: Should we query all domains where user is member of?!
                String domains = domainIds[0];
                for (int i = 1; i < domainIds.length; i++) {
                    domains = domains + ", " + domainIds[i];
                }
                responseMsg.setText("You (" + user.getUsername() + ") are member of more than one domain (" + domains + "), therefore Katie does not know which domain should be used to ask your question.");
            }
        } else {
            MessageValue value = message.getValue();
            if (value != null) {
                log.info("Action detected: " + value.getMessage()); // INFO: For example: REQUEST_BECOME_MEMBER::29:1lgs5cd7zlt-i8q09aahzyrcri4sje_ydrn-qoeflaffxptf3lsxy-m6xslkb08fy69tjor-ueduxjc5sahsyzq::Michael Wechner

                String[] domainIds = contextService.getDomainIDsUserIsMemberOf(user);
                String domainId = null;
                if (domainIds.length > 0) {
                    domainId = domainIds[0];
                } else {
                    log.warn("User '" + user.getId() + "' is not member of any domains.");
                    // User should have submitted a domain Id
                    domainId = value.getDomainid(); // MessageValue.TEXT_INPUT_DOMAIN_ID
                }
                responseMsg = getAnswerForActionRequest(message, responseMsg, convValues, domainId);
            } else {
                responseMsg.setText(messageSource.getMessage("neither.question.nor.action.detected", null, Utils.getLocale(locale)));
                responseMsg.setTextFormat("plain");
            }
        }

        return responseMsg;
    }

    /**
     * Get card to request to become member (MS Teams user already registered) of a particular domain to use direct messaging with Katie
     * @param userId MS Teams user Id, e.g. "29:1lgs3cd7zlt-i8q09aahzyrcri4sje_gdrn-qoeflaffxptf2lsxy-m6zslkb08fy69tjor-ueduxjc2sahsyzq"
     * @param name Name of user, e.g. "Michael Wechner"
     */
    private MicrosoftAdaptiveCard getRequestBecomeDomainMemberCard(Locale locale, String userId, String name) {
        MicrosoftAdaptiveCard card = new MicrosoftAdaptiveCard(messageSource.getMessage("not.member.yet", null, locale));

        // TODO: Get available Katie domain IDs / Names by MS Teams tenant Id (assuming that tenant Id is linked somehow with domain IDs)
        MicrosoftAdaptiveCardBody domainIdInput = new MicrosoftAdaptiveCardBody("Katie Domain Id:");
        domainIdInput.addItem(new MicrosoftAdaptiveCardInputText(MessageValue.TEXT_INPUT_DOMAIN_ID, "abc3rdb3-34a9-4a84-b12a-13d5dfd2152w"));
        card.addBody(domainIdInput);

        MicrosoftAdaptiveCardActionSubmit requestBecomeMember = new MicrosoftAdaptiveCardActionSubmit(messageSource.getMessage("request.become.member", null, locale));
        requestBecomeMember.setData(new MessageValue(ChannelAction.REQUEST_BECOME_MEMBER + ACTION_SEPARATOR + userId + ACTION_SEPARATOR + name));
        card.addAction(requestBecomeMember);

        return card;
    }

    /**
     * Get card to request invitation (MS Teams user not registered yet) to use direct messaging with Katie
     */
    private MicrosoftAdaptiveCard getRequestInvitationCard(Locale locale, String userId, String name) {
        MicrosoftAdaptiveCard card = new MicrosoftAdaptiveCard(messageSource.getMessage("not.registered.yet", null, locale));

        // TODO: Get available Katie domain IDs / Names by MS Teams tenant Id (assuming that tenant Id is linked somehow with domain IDs)
        // TODO: Consider asking user also for email address
        MicrosoftAdaptiveCardBody domainIdInput = new MicrosoftAdaptiveCardBody("Katie Domain Id:");
        domainIdInput.addItem(new MicrosoftAdaptiveCardInputText(MessageValue.TEXT_INPUT_DOMAIN_ID, "abc3rdb3-34a9-4a84-b12a-13d5dfd2152w"));
        card.addBody(domainIdInput);

        MicrosoftAdaptiveCardActionSubmit requestInvitation = new MicrosoftAdaptiveCardActionSubmit(messageSource.getMessage("request.invitation", null, locale));
        requestInvitation.setData(new MessageValue(ChannelAction.REQUEST_INVITATION + ACTION_SEPARATOR + userId + ACTION_SEPARATOR + name));
        card.addAction(requestInvitation);

        return card;
    }

    /**
     * Send message back to MS Teams
     * @param convValues TODO
     * @param convName Conversation name, e.g. TODO
     * @param recipientAsFrom From, e.g. Katie2, 28:a888d256-6f0e-4358-b23e-9b644fe0fd64
     * @param fromAsRecipient Recipient, e.g. Michael Wechner, 29:1EJDwgL0u2JE_2RUJIb7BciubbOTOmD-IxXHEim0k_fqkcijFbagVLa1a8ymq5FbdJp2JaznK_bHrZ7gpyGg5Ug
     * @param responseMsg TODO
     */
    private void send(MSTeamsConversationValues convValues, String convName, MicrosoftRecipient recipientAsFrom, MicrosoftFrom fromAsRecipient, MicrosoftResponse responseMsg) {
        if (responseMsg == null) {
            log.info("No response message generated, therefore do not send a response.");
            return;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            log.info("Response message: " + mapper.writerWithDefaultPrettyPrinter().writeValueAsString(responseMsg));
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        // INFO: https://docs.microsoft.com/en-us/azure/bot-service/rest-api/bot-framework-rest-connector-authentication?view=azure-bot-service-4.0#bot-to-connector (https://docs.microsoft.com/en-us/azure/bot-service/bot-service-manage-overview?view=azure-bot-service-4.0)
        log.info("Get access token in order to send message back to MS Teams ...");
        String token = getAccessToken();
        //log.info("DEBUG: " + token);
        if (token == null) {
            log.error("No access token could be retrieved, therefore do not send message to MS Teams!");
            return;
        }


        // INFO: https://learn.microsoft.com/en-us/azure/bot-service/rest-api/bot-framework-rest-connector-send-and-receive-messages?view=azure-bot-service-4.0
        log.info("Send parameters: Service URL: " + convValues.getServiceUrl() + ", Conversation Id: " + convValues.getConversationId() + ", Conversation Name: " + convName + ", Message Id: " + convValues.getMessageId() + ", From Id: " + recipientAsFrom.getId() + ", From Name: " + recipientAsFrom.getName() + ", Recipient Id: " + fromAsRecipient.getId() + ", Recipient Name: " + fromAsRecipient.getName());

        responseMsg.setReplyToId(convValues.getMessageId());

        MicrosoftConversation conversation = new MicrosoftConversation();
        conversation.setId(convValues.getConversationId());
        responseMsg.setConversation(conversation);
        if (convName != null) {
            conversation.setName(convName);
        }

        MicrosoftFrom from = new MicrosoftFrom();
        from.setName(recipientAsFrom.getName());
        from.setId(recipientAsFrom.getId());
        responseMsg.setFrom(from);

        MicrosoftRecipient recipient = new MicrosoftRecipient();
        recipient.setName(fromAsRecipient.getName());
        recipient.setId(fromAsRecipient.getId());
        responseMsg.setRecipient(recipient);

        String requestUrl = convValues.getServiceUrl() + "/v3/conversations/" + convValues.getConversationId() + "/activities/" + convValues.getMessageId();
        log.info("Send response to '" + requestUrl + "'");

        RestTemplate restTemplate = restProxyTemplate.getRestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Accept-Charset", "UTF-8");
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<MicrosoftResponse> request = new HttpEntity<MicrosoftResponse>(responseMsg, headers);
        try {
            log.info("Try to send response {" + request.getBody() + "} to Microsoft Bot Connector API: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(requestUrl, request, JsonNode.class);
            log.info("Response status code: " + response.getStatusCodeValue());
            if (response.getStatusCodeValue() == 200) {
                log.info("Message was sent to MS Teams successfully :-)");
            } else {
                log.warn("Sending message to MS Teams did not work as expected.");
            }
            JsonNode bodyNode = response.getBody();
            log.info("Response body as JSON: " + bodyNode);
        } catch(Exception e) {
            // TODO: 401 Unauthorized: [no body]
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Send invitation request for registration to Katie administrators
     *
     * @param responseMsg Response message
     * @param userName Name of user to be registered, e.g. "Michael Wechner"
     * @param  userId Id of user to be registered, e.g. "29:1EJDogL0u2Jb_2RUJIb7BciubbOTOmD-IxXHEim0k_fqkcijFbagVLa1a2ymq5FbdJp2JaznK_bHrZ7gpyGg5Ug"
     *
     * @return response to received message
     */
    private MicrosoftResponse getRequestInvitationInteractionResponse(MicrosoftResponse responseMsg, String userName, String userId, String domainId) {
        try {
            log.info("Send email to Katie administrators that MS Teams user '" + userName + "' (" + userId + ") requests Katie registration and to get invited to domain '" + domainId + "' ...");
            String subject = "MS Teams user requests invitation to Katie domain '" + domainId + "'";
            String body = getMailBodyForRegisteringMSTeamsUser(userName, userId, domainId);
            notifyAdministrators(subject, body);
            if (domainId != null) {
                contextService.notifyDomainOwnersAndAdmins(subject, body, domainId);
            }

            responseMsg.setText("Katie System Administrator has been notified re Katie registration request of user '" + userName + "' (" + userId + ").");
            responseMsg.setTextFormat("markdown");
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            responseMsg.setText(e.getMessage());
        }

        return responseMsg;
    }

    /**
     * Request that a new Katie domain gets created and connected with MS Teams
     * @return message that Katie system administrator has been notified re creating a new domain and connecting with MS Teams
     */
    private String requestCreateDomain(String teamId, String userName) {
        try {
            StringBuilder body = new StringBuilder();
            body.append("<html><body>");
            body.append("<p>The MS Teams user <strong>" + userName + "</strong>  requests to create a new domain and connect with the team Id '" + teamId + "'.</p>");
            body.append("<p>" + defaultHostnameMailBody + "/swagger-ui/#/microsoft-bot-controller/connectTeamWithDomainUsingGET</p>");
            body.append("</body></html>");

            notifyAdministrators("MS Teams user requests creating a new domain", body.toString());

            return "Katie System Administrator has been notified re creating new domain and connect with team Id '" + teamId + "'.";
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

    /**
     * Request that MS Teams gets connected with an existing domain
     * @return  message that Katie system administrator has been notified re connecting MS Teams with an existing domain
     */
    private String requestConnectExistingDomain(String teamId, String userName) {
        try {
            StringBuilder body = new StringBuilder();
            body.append("<html><body>");
            body.append("<p>The MS Teams user <strong>" + userName + "</strong>  requests to connect the team Id '" + teamId + "' with an existing domain.</p>");
            body.append("<p>" + defaultHostnameMailBody + "/swagger-ui/#/microsoft-bot-controller/connectTeamWithDomainUsingGET</p>");
            body.append("</body></html>");

            notifyAdministrators("MS Teams user requests connecting team channel with existing domain", body.toString());

            return "Katie System Administrator has been notified re connecting existing domain with team Id '" + teamId + "'.";
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

    /**
     * TODO: Move this method to Katie core
     * Notify administrators of Katie instance
     */
    private void notifyAdministrators(String subject, String message) throws Exception {
        User[] admins = iamService.getAdministrators();
        mailerService.notifyUsers(admins, subject, message);
    }

    /**
     * @param userName User name, e.g. "Michael Wechner"
     */
    private String getMailBodyForRegisteringMSTeamsUser(String userName, String userId, String domainId) {
        // TODO: Use mailerService.getTemplate()
        StringBuilder body = new StringBuilder();
        body.append("<html><body>");
        body.append("<p>The MS Teams user <strong>" + userName + "</strong> with the user Id <strong>" + userId + "</strong> requests to be registered at <a href=\"" + defaultHostnameMailBody + "\">Katie</a> and would like to be invited to the domain <a href=\"" + defaultHostnameMailBody + "/#/domain/" + domainId + "\">" + domainId + "</a></p>");
        body.append("</body></html>");
        return body.toString();
    }

    /**
     * Send become member request to Katie administrators
     *
     * @param responseMsg Response message
     * @param userName Name of user to be registered, e.g. "Michael Wechner"
     * @param  userId Id of user to be registered, e.g. "29:1EJDogL0u2Jb_2RUJIb7BciubbOTOmD-IxXHEim0k_fqkcijFbagVLa1a2ymq5FbdJp2JaznK_bHrZ7gpyGg5Ug"
     *
     * @return response to received message
     */
    private MicrosoftResponse getRequestBecomeMemberInteractionResponse(MicrosoftResponse responseMsg, String userName, String userId, Context domain) {
        try {
            notifyAdministrators("MS Teams user requests becoming member of a Katie domain", getMailBodyForBecomingMember(userName, userId, domain));

            responseMsg.setText("Administrator has been notified re user '" + userName + "' (" + userId + ") becoming member of a domain '" + domain.getId() + "'.");
            responseMsg.setTextFormat("markdown");
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            responseMsg.setText(e.getMessage());
        }

        return responseMsg;
    }

    /**
     *
     */
    private String getMailBodyForBecomingMember(String userName, String userId, Context domain) {
        // TODO: Use mailerService.getTemplate()
        StringBuilder body = new StringBuilder();
        body.append("<html><body>");
        body.append("<p>The MS Teams / Katie user <strong>" + userName + "</strong> with the user Id <a href=\"" + defaultHostnameMailBody + "/#/iam\">" + userId + "</a> requests to become member of the Katie domain '" + domain.getName() + "' (" + domain.getId() + ").</p>");
        body.append("</body></html>");
        return body.toString();
    }

    /**
     * Get answer for action request
     * @param message Received message
     * @param responseMsg Response message
     * @return response to received message
     */
    private MicrosoftResponse getAnswerForActionRequest(MicrosoftBotMessage message, MicrosoftResponse responseMsg, MSTeamsConversationValues convValues, String domainId) {
        String[] actionParts = message.getValue().getMessage().split(ACTION_SEPARATOR);
        ChannelAction action = ChannelAction.valueOf(actionParts[0]);
        log.info("Execute action '" + action + "' ...");

        /*
        String teamId = message.getChannelData().getTeam().getId();
        log.info("Team Id: " + teamId);
        String channelId = message.getChannelData().getChannel().getId();
        log.info("Channel Id: " + channelId);
        String tenantId = message.getChannelData().getTenant().getId();
        log.info("Tenant Id: " + tenantId);
         */

        /*
        String domainId = null;
        if (teamId == null) {
            log.info("No team Id available, because probably direct message from user.");
            User user = authService.getUser(false, false);
            String[] domainIds = contextService.getDomainIDsUserIsMemberOf(user);
            if (domainIds.length > 0) {
                domainId = domainIds[0];
            } else {
                log.warn("User '" + user.getUsername() + "' is not member of any Katie domain!");
                String[] args = new String[1];
                args[0] = "<strong>" + user.getUsername() + "</strong>";
                responseMsg.setText(messageSource.getMessage("cannot.answer.because.user.id.not.linked.with.domain", args, Utils.getLocale(message.getLocale())));
                return responseMsg;
            }
        } else {
            domainId = dataRepoService.getDomainIdForMSTeam(teamId);
        }

         */

        Context domain = null;
        try {
            if (domainId != null) {
                domain = contextService.getContext(domainId);
            } else {
                log.warn("No domain Id available!");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            responseMsg.setText(e.getMessage());
            return responseMsg;
        }

        String lang = message.getLocale();
        log.info("Locale of received message: " + lang);
        Locale locale = Utils.getLocale(lang);

        //User user = iamService.getUserByUsername(new Username(message.getFrom().getId()), false, false);
        User user = authService.getUser(false, false);
        if (action.equals(ChannelAction.IMPROVE_CORRECT_ANSWER)) {
            /* TODO: Consider question UUID instead of answer UUID and question
            String questionUuid = "TODO";
            AskedQuestion askedQuestion = dataRepoService.getQuestionByUUID(questionUuid);
            askedQuestion.getQuestion();
            askedQuestion.getAnswerUUID();
             */
            String uuidAnswer = actionParts[1];
            String question = actionParts[2];
            String text = improveAnswerAction(uuidAnswer, question, domain, user, Utils.getLocale(message.getLocale()), message.getConversation().getConversationType());
            responseMsg.setText(text);
            responseMsg.setTextFormat("markdown");
        } else if (action.equals(ChannelAction.ENTER_BETTER_ANSWER)) {
            String questionUuid = actionParts[1];
            //responseMsg.setText("Thanks for entering a better answer! Question UUID: " + questionUuid);
            MicrosoftAdaptiveCard feedbackCard = getBetterAnswerFeedbackCard(locale, questionUuid);
            responseMsg.addAttachment(new MicrosoftAttachment(feedbackCard));
        } else if (action.equals(ChannelAction.SEND_BETTER_ANSWER)) {
            String questionUuid = actionParts[1];
            try {
                AskedQuestion askedQuestion = contextService.getAskedQuestionByUUID(questionUuid);
                String[] args = new String[1];
                args[0] = askedQuestion.getQuestion();
                responseMsg.setText(messageSource.getMessage("thanks.for.better.answer", args, locale));

                String betterAnswer = message.getValue().getBetteranswer(); // MessageValue.TEXT_INPUT_BETTER_ANSWER
                String relevantUrl = message.getValue().getRelevanturl(); // MessageValue.TEXT_INPUT_RELEVANT_URL
                log.info("Save better answer: " + betterAnswer + ", " + relevantUrl);
                ContentType contentType = null;
                List<String> classifications  = new ArrayList<String>();
                Date dateAnswered = new Date();
                Date dateAnswerModified = dateAnswered;
                Date dateOriginalQuestionSubmitted = dateAnswered;
                boolean isPublic = false;
                Answer newQnA = new Answer(null, betterAnswer, contentType, relevantUrl, classifications, QnAType.DEFAULT, null, dateAnswered, dateAnswerModified, null, domain.getId(), null, askedQuestion.getQuestion(), dateOriginalQuestionSubmitted, isPublic, new Permissions(isPublic), false, user.getId());
                contextService.addQuestionAnswer(newQnA, domain);
                contextService.train(new QnA(newQnA), domain, true);

                contextService.notifyExpertsToApproveProvidedAnswer(newQnA.getUuid(), domain, askedQuestion.getQuestion());

                String channelRequestId = null;
                webhooksService.deliver(WebhookTriggerEvent.BETTER_ANSWER_PROVIDED, domain.getId(), newQnA.getUuid(), newQnA.getOriginalquestion(), newQnA.getAnswer(), newQnA.getAnswerContentType(), null, channelRequestId);
            } catch (Exception e) {
                responseMsg.setText("Exception: " + e.getMessage());
                log.error(e.getMessage(), e);
            }
        } else if (action.equals(ChannelAction.SEND_QUESTION_TO_EXPERT)) {
            String question = actionParts[1];
            String text = sendQuestionToExpertAction(question, domainId, user, locale, message.getConversation().getConversationType(), convValues);
            responseMsg.setText(text);
            responseMsg.setTextFormat("markdown");
        } else if (action.equals(ChannelAction.SEE_MORE_ANSWERS)) {
            List<String> classifications = new ArrayList<String>();
            String channelRequestId = java.util.UUID.randomUUID().toString();
            int limit = 4;
            String question = actionParts[2];
            try {
                List<ResponseAnswer> answers = qaService.getAnswers(question, null, false, classifications, message.getId(), domain, new Date(), remoteAddress, ChannelType.MS_TEAMS, channelRequestId, limit, 1, true, null,false, false, false);

                StringBuilder response = new StringBuilder();
                response.append("<div>");
                for (int i = 0; i < answers.size(); i++) {
                    ResponseAnswer answer = answers.get(i);
                    // TODO: Reconsider spacings between source and meta info
                    String _answer = getAnswerInclMetaInformation(answer.getAnswer(), answer.getOriginalQuestion(), answer.getDateOriginalQuestion(), Utils.getLocale(message.getLocale()), answer.getUrl(), "<br/><br/>", "");
                    response.append("<div>" + _answer + "</div>");
                    if (i < answers.size() - 1) {
                        response.append("<span>---------------------------</span>");
                    }
                }
                response.append("</div>");

                responseMsg.setText(response.toString());
                responseMsg.setTextFormat("markdown");
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                responseMsg.setText(e.getMessage());
                return responseMsg;
            }
        } else if (action.equals(ChannelAction.REQUEST_BECOME_MEMBER)) {
            log.info("Send become domain member request to Katie administrators ...");
            String userId = actionParts[1];
            String userName = actionParts[2];
            if (domain != null) {
                return getRequestBecomeMemberInteractionResponse(responseMsg, userName, userId, domain);
            } else {
                responseMsg.setText("Please make sure to provide a Katie domain Id!");
                return responseMsg;
            }
        } else if (action.equals(ChannelAction.THUMB_UP) || action.equals(ChannelAction.THUMB_DOWN)) {
            String questionUuid = actionParts[2];
            try {
                AskedQuestion askedQuestion = contextService.getAskedQuestionByUUID(questionUuid);

                if (action.equals(ChannelAction.THUMB_UP)) {
                    contextService.thumbUpDown(askedQuestion, true, domain);
                    responseMsg.setText(messageSource.getMessage("thanks.for.positive.feedback", null, locale));
                } else {
                    contextService.thumbUpDown(askedQuestion, false, domain);
                    responseMsg.setText(messageSource.getMessage("thanks.for.negative.feedback", null, locale));

                    MicrosoftAdaptiveCard feedbackCard = getBetterAnswerFeedbackCard(locale, questionUuid);
                    responseMsg.addAttachment(new MicrosoftAttachment(feedbackCard));
                }
            } catch (Exception e) {
                responseMsg.setText(e.getMessage());
            }
        } else {
            responseMsg.setText("Error: No such action type '" + action + "' implemented!");
        }

        return responseMsg;
    }

    /**
     * Get feedback card to suggest better answer
     */
    private MicrosoftAdaptiveCard getBetterAnswerFeedbackCard(Locale locale, String askedQuestionUuid) {
        MicrosoftAdaptiveCard feedbackCard = new MicrosoftAdaptiveCard(messageSource.getMessage("provide.better.answer", null, locale));

        MicrosoftAdaptiveCardBody betterResponse = new MicrosoftAdaptiveCardBody(messageSource.getMessage("better.answer", null, locale) + ":");
        betterResponse.addItem(new MicrosoftAdaptiveCardInputText(MessageValue.TEXT_INPUT_BETTER_ANSWER, messageSource.getMessage( "what.would.be.helpful.answer", null, locale), true));
        feedbackCard.addBody(betterResponse);

        MicrosoftAdaptiveCardBody betterResponseSourceUrl = new MicrosoftAdaptiveCardBody("URL:");
        betterResponseSourceUrl.addItem(new MicrosoftAdaptiveCardInputText(MessageValue.TEXT_INPUT_RELEVANT_URL, messageSource.getMessage("url.relevant.source", null, locale)));
        feedbackCard.addBody(betterResponseSourceUrl);

        MicrosoftAdaptiveCardActionSubmit improveAnswer = new MicrosoftAdaptiveCardActionSubmit(messageSource.getMessage("submit.better.answer", null, locale));
        improveAnswer.setData(new MessageValue(ChannelAction.SEND_BETTER_ANSWER + ACTION_SEPARATOR + askedQuestionUuid));
        feedbackCard.addAction(improveAnswer);

        return feedbackCard;
    }

    /**
     * Generate answer containing link which will allow user to improve or correct answer
     * @param question Question which was asked by MS Teams user
     * @param user User which wants to improve answer
     * @param conversationType Conversation type, e.g. "channel" or "personal"
     * @return answer containing link to improve/correct trained question-answer
     */
    private String improveAnswerAction(String uuid, String question, Context domain, User user, Locale locale, String conversationType) {
        log.info("MS Teams user with Id '" + user.getId() + "' would like to edit trained question answer '" + uuid + "' inside Katie domain '" + domain.getId() + "' ...");

        if (user != null && contextService.isUserMemberOfDomain(user.getId(), domain.getId())) {
            try {
                String token = jwtService.generateJWT(user.getUsername(), domain.getId(), 600, null); // TODO: Make token validity configurable
                iamService.addJWT(new Username(user.getUsername()), token); // INFO: See AuthenticationController, where iamService.hasValidJWT(...) is being checked
                String[] args = new String[3];
                args[0] = user.getFirstname();
                args[1] = getImproveAnswerLink(messageSource.getMessage("here", args, locale), domain, uuid, URLEncoder.encode(question, "UTF-8"), token);
                args[2] = uuid;
                return messageSource.getMessage("improve.answer.link", args, locale);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
                return e.getMessage();
            }
        } else {
            String[] args = new String[2];
            args[0] = user.getUsername();
            args[1] = domain.getId();
            return messageSource.getMessage("improve.answer.not.member", args, locale);
        }
    }

    /**
     * @param uuid UUID of trained QnA
     * @param submittedQuestion Question which was asked by user and for which answer should be improved or corrected
     */
    private String getImproveAnswerLink(String linkText, Context domain, String uuid, String submittedQuestion, String jwtToken) {
        return getLink(domain.getHost() + "/#/trained-questions-answers/" + domain.getId() + "/" + uuid + "/improve?token=" + jwtToken + "&q=" + submittedQuestion, linkText);
    }

    /**
     * Send question to expert
     * @param question Question which was asked by MS Teams user
     * @param user User which wants to send question to expert
     * @param conversationType Conversation type, e.g. "channel" or "personal"
     */
    private String sendQuestionToExpertAction(String question, String domainId, User user, Locale locale, String conversationType, MSTeamsConversationValues convValues) {
        log.info("MS Teams user with Katie user '" + user.getId() + "' sends question to expert ...");
        String remoteAddress = null;
        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
            convValues.setDomainId(domain.getId());
            log.info("Add conversation values to database ...");
            dataRepoService.addMSTeamsConversationValues(convValues);
            String uuid = contextService.answerQuestionByNaturalIntelligence(question, user, ChannelType.MS_TEAMS, convValues.getUuid(), null, null, mapLanguage(locale.getLanguage()), null, remoteAddress, domain);

            String[] emailsTo = contextService.getMailNotificationAddresses(domain.getId());
            if (emailsTo.length > 0) {
                String[] args = new String[2];
                args[0] = "*" + Utils.escapeDoubleQuotes(question) + "*";
                args[1] = getKatieDomainLink(messageSource.getMessage("send.question.to.expert.expert", null, locale), domain, null);
                StringBuilder sb = new StringBuilder();
                sb.append(messageSource.getMessage("send.question.to.expert.forwarded", args, locale));
                /*
                sb.append("I have forwarded your question *'" + escapeDoubleQuotes(question) + "'* to an " + getKatieDomainLink("expert", domain, null));
                if (conversationType.equals("channel")) {
                    sb.append(" affiliated with this MS Teams TODOchannel");
                } else {
                    log.info("Conversation type: " + conversationType);
                }
                sb.append(" and you should receive an answer as soon as possible!");
                 */
                return sb.toString();
            } else {
                return "I have submitted your question *'" + Utils.escapeDoubleQuotes(question) + "'*, but the " + getKatieDomainLink("knowledge base", domain, null) + " affiliated with this MS Teams TODOchannel has no experts configured yet!";
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

    /**
     *
     */
    private Language mapLanguage(String language) {
        if (language != null) {
            return Language.valueOf(language);
        } else {
            log.warn("No language provided, therefore we use 'en'.");
            return Language.en;
        }
    }

    /**
     *
     */
    private String getKatieDomainLink(String linkText, Context domain, String jwtToken) {
        if (jwtToken != null) {
            return getLink(domain.getHost() + "/#/domain/" + domain.getId() + "?token=" + jwtToken, linkText);
        } else {
            return getLink(domain.getHost() + "/#/domain/" + domain.getId(), linkText);
        }
    }

    /**
     *
     */
    private String getLink(String url, String text) {
        return "[" + text + "](" + url + ")";
    }

    /**
     * Get answer to question and add this answer to response message
     *
     * @param message Received message
     * @param domain Katie domain which user or team is associated with
     * @param question Question which was asked by user
     * @param responseMsg Response message
     * @param teamId Team Id when message was sent from a channel and null otherwise (e.g. direct message / personal)
     *
     * @return response to received message
     */
    private MicrosoftResponse getAnswerMessage(MicrosoftBotMessage message, Context domain, String question, MicrosoftResponse responseMsg, String teamId, String channelId) throws Exception {
        String lang = message.getLocale();
        log.info("Locale of received message: " + lang);
        Locale locale = Utils.getLocale(lang);

        String userId = message.getFrom().getId();

        String channelRequestId = java.util.UUID.randomUUID().toString();
        MSTeamsConversationValues convValues = new MSTeamsConversationValues(channelRequestId, teamId, channelId, message.getServiceUrl(), message.getConversation().getId(), message.getId(), message.getRecipient().getId(), message.getFrom().getId());
        convValues.setDomainId(domain.getId());
        dataRepoService.addMSTeamsConversationValues(convValues);

        Date dateSubmitted = new Date();
        // TODO
        List<String> classifications = new ArrayList<String>();
        boolean checkAuthorization = false; // TODO: Set to true
        List<ResponseAnswer> answers = qaService.getAnswers(question,  null, false, classifications, message.getId(), domain, dateSubmitted, remoteAddress, ChannelType.MS_TEAMS, channelRequestId, 2, 0, checkAuthorization, null, false, false, false);

        MicrosoftAdaptiveCard answerNotHelpfulCard = new MicrosoftAdaptiveCard(messageSource.getMessage("answer.helpful", null, locale));

        if (answers != null && answers.size() > 0) {
            ResponseAnswer topAnswer = answers.get(0);
            if (domain.getAnswersMustBeApprovedByModerator()) {
                // INFO: When moderation is enabled, then Katie does not send any messages back to the channel
                log.info("Moderation approval enabled for domain '" + domain.getName() + "' (Id: " + domain.getId() + "), therefore do not return an answer.");
                responseMsg.setText("Thanks for submitting your question, whereas Katie's answer must be approved by a moderator. Thank you for your patience!");
                if (domain.getInformUserReModeration()) {
                    return responseMsg;
                } else {
                    return null;
                }
            } else if (topAnswer.getPermissionStatus() == PermissionStatus.PERMISSION_DENIED) {
                // TODO: Detect language of question, whereas see https://pm.wyona.com/issues/2739
                responseMsg.setText(messageSource.getMessage("sorry.not.authorized", null, locale));
            } else if (topAnswer.getPermissionStatus() == PermissionStatus.NOT_SUFFICIENT_PERMISSIONS_TO_READ_ANSWER) {
                if (teamId != null) {
                    String[] args = new String[3];
                    args[0] = domain.getName();
                    args[1] = domain.getHost();
                    args[2] = teamId;
                    responseMsg.setText(messageSource.getMessage("sorry.no.permission", args, locale));

                    // TODO: Instead becoming a domain member, administrator can also add a techincal user 'msteams' as domain member (see property 'ms.katie.username')
                    MicrosoftAdaptiveCard notConnectedYet = new MicrosoftAdaptiveCard(messageSource.getMessage("not.member.yet", null, Utils.getLocale(lang)));

                    MicrosoftAdaptiveCardActionSubmit requestBecomeMember = new MicrosoftAdaptiveCardActionSubmit(messageSource.getMessage("request.become.member", null, locale));
                    requestBecomeMember.setData(new MessageValue(ChannelAction.REQUEST_BECOME_MEMBER + ACTION_SEPARATOR + userId + ACTION_SEPARATOR + message.getFrom().getName()));
                    notConnectedYet.addAction(requestBecomeMember);

                    responseMsg.addAttachment(new MicrosoftAttachment(notConnectedYet));
                } else {
                    responseMsg.setText(messageSource.getMessage("sorry.no.permission.personal", null, locale));
                }

                return responseMsg;
            } else {
                // TODO: Make truncation resp. max answer length configurable
                String seeMore = messageSource.getMessage("see.more", null, locale);;
                responseMsg.setText(getAnswerInclMetaInformation(topAnswer.getAnswer(300, seeMore), topAnswer.getOriginalQuestion(), topAnswer.getDateOriginalQuestion(), locale, topAnswer.getUrl(), "<br/><br/>", "<hr/>"));
                responseMsg.setTextFormat("xml");
            }

            MicrosoftAdaptiveCardActionSubmit seeMoreAnswers = new MicrosoftAdaptiveCardActionSubmit(messageSource.getMessage("see.more.answers", null, locale));
            seeMoreAnswers.setData(new MessageValue(ChannelAction.SEE_MORE_ANSWERS + ACTION_SEPARATOR + domain.getId() + ACTION_SEPARATOR + question));
            answerNotHelpfulCard.addAction(seeMoreAnswers);

            String correct = messageSource.getMessage("correct.answer", null, locale);
            String wrong = messageSource.getMessage("wrong.answer", null, locale);

            MicrosoftAdaptiveCardActionSubmit thumbUp = new MicrosoftAdaptiveCardActionSubmit(Emoji.THUMB_UP + " " + correct);
            thumbUp.setData(new MessageValue(ChannelAction.THUMB_UP + ACTION_SEPARATOR + domain.getId() + ACTION_SEPARATOR + topAnswer.getQuestionUUID()));
            answerNotHelpfulCard.addAction(thumbUp);

            MicrosoftAdaptiveCardActionSubmit thumbDown = new MicrosoftAdaptiveCardActionSubmit(Emoji.THUMB_DOWN + " " + wrong);
            thumbDown.setData(new MessageValue(ChannelAction.THUMB_DOWN + ACTION_SEPARATOR + domain.getId() + ACTION_SEPARATOR + topAnswer.getQuestionUUID()));
            answerNotHelpfulCard.addAction(thumbDown);

            MicrosoftAdaptiveCardActionSubmit sendToExpert = new MicrosoftAdaptiveCardActionSubmit(messageSource.getMessage("send.question.to.expert", null, locale));
            sendToExpert.setData(new MessageValue(ChannelAction.SEND_QUESTION_TO_EXPERT + ACTION_SEPARATOR + question));
            answerNotHelpfulCard.addAction(sendToExpert);

            MicrosoftAdaptiveCardActionSubmit enterBetterAnswer = new MicrosoftAdaptiveCardActionSubmit(messageSource.getMessage("provide.better.answer", null, locale));
            enterBetterAnswer.setData(new MessageValue(ChannelAction.ENTER_BETTER_ANSWER + ACTION_SEPARATOR + topAnswer.getQuestionUUID()));
            answerNotHelpfulCard.addAction(enterBetterAnswer);

            if (false) { // TODO: Make configurable
                if (topAnswer != null) {
                    MicrosoftAdaptiveCardActionSubmit improveAnswer = new MicrosoftAdaptiveCardActionSubmit(messageSource.getMessage("improve.answer", null, locale));
                /* TODO: Consider sending question UUID instead of answer UUID and question
                topAnswer.getQuestionUUID();
                domain.getId();
                 */
                    improveAnswer.setData(new MessageValue(ChannelAction.IMPROVE_CORRECT_ANSWER + ACTION_SEPARATOR + topAnswer.getUuid() + ACTION_SEPARATOR + question));
                    answerNotHelpfulCard.addAction(improveAnswer);
                } else {
                    log.info("No answer available for question '" + question + "'.");
                }
            }
        } else {
            if(domain.getInformUserNoAnswerAvailable()) {
                // TODO
            }
            responseMsg.setText(messageSource.getMessage("sorry.do.not.know.an.answer", null, locale));
            // TODO: Add action, e.g. "send.question.to.expert"
        }

        if (false) { // TODO: Make configurable
            MicrosoftAdaptiveCardActionOpenUrl logIntoKatie = new MicrosoftAdaptiveCardActionOpenUrl("Log into Katie ...");
            logIntoKatie.setUrl(domain.getHost());
            answerNotHelpfulCard.addAction(logIntoKatie);
        }

        if (answerNotHelpfulCard.getActions().length > 0) {
            responseMsg.addAttachment(new MicrosoftAttachment(answerNotHelpfulCard));
        }

        return responseMsg;
    }

    /**
     * Send expert's answer to resubmitted question back to MS Teams channel where question was asked
     * @param answerOnly When true, then replied message contains the answer only and no additional text
     */
    public void sendAnswer(ResubmittedQuestion question, boolean answerOnly) {
        log.info("Send expert's answer (to resubmitted question '" + question.getUuid() + "') back to MS Teams channel ...");

        MicrosoftResponse responseMsg = new MicrosoftResponse();
        if (answerOnly) {
            responseMsg.setText(question.getAnswer());
            responseMsg.setTextFormat("xml");
        } else {
            String[] args = new String[1];
            args[0] = "<strong>" + question.getQuestion() + "</strong>";
            // TODO: Replace hard coded language
            responseMsg.setText("<div>" + messageSource.getMessage("send.question.to.expert.answer", args, new Locale("de")) + "<br/><br/>" + question.getAnswer() + "</div>");
            responseMsg.setTextFormat("xml");
        }

        log.info("Get conversation values from database ...");
        MSTeamsConversationValues convValues = dataRepoService.getMSTeamsConversationValues(question.getChannelRequestId());

        MicrosoftRecipient from = new MicrosoftRecipient();
        from.setId(convValues.getKatieBotId());

        MicrosoftFrom recipient = new MicrosoftFrom();
        recipient.setId(convValues.getMsTeamsUserId());

        send(convValues, null, from, recipient, responseMsg);
    }

    /**
     * Get access token, cache it and refresh token before it expires
     * https://docs.microsoft.com/en-us/azure/bot-service/rest-api/bot-framework-rest-connector-authentication?view=azure-bot-service-4.0
     */
    private String getAccessToken() {
        return authorizationService.getAccessToken(oauthUrl, grantType, clientId, clientSecret, scope);
    }

    /**
     * Analyze message
     * @param message Message, e.g. "<at>Katie</at> How is the weather today?"
     * @param mentionEntity Mention text, e.g. "<at>Katie</at>"
     * @param domain Katie domain associated with message / question
     * @return analyzed message when message contains question (e.g. "How is the weather today?" or "What is the wifi password") or null otherwise
     */
    private AnalyzedMessage getAnalyzedMessage(String message, MicrosoftEntity mentionEntity, Context domain) {
        if (message == null || (message != null && message.trim().length() == 0)) {
            log.warn("No message, therefore no question.");
            return null;
        }

        if (mentionEntity != null) {
            log.info("Remove @mention '" + mentionEntity.getText() + "' from message '" + message + "' ...");
        }

        String messageWithoutKatieMention = message.trim();

        if (messageWithoutKatieMention.startsWith("<at>")) {
            int endOfKatieMention = messageWithoutKatieMention.indexOf("</at>") + 5;
            String mentionPart = messageWithoutKatieMention.substring(0, endOfKatieMention);
            log.info("Mention part: " + mentionPart); // INFO: Should be the same like mentionEntity.geText()
            messageWithoutKatieMention = messageWithoutKatieMention.substring(endOfKatieMention).trim();
            log.info("Message without '" + mentionPart + "': " + messageWithoutKatieMention);
        } else {
            log.info("Message does not contain @mention: '" + messageWithoutKatieMention + "'");
        }

        AnalyzedMessage analyzedMessage = questionAnalyzerService.analyze(messageWithoutKatieMention, domain);
        if (analyzedMessage.getContainsQuestions()) {
            log.info("Question detected: '" + analyzedMessage.getMessage() + "'");
            if (analyzedMessage.getQuestionsAndContexts().size() > 1) {
                log.info("TODO: Handle multiple questions ...");
            }
            return analyzedMessage;
        } else {
            log.info("No question detected '" + analyzedMessage.getMessage() + "', but let us consider message as question anyway.");
            // INFO: Consider every message as question, because in the case of MS Teams users either talk directly to Katie or use @mention
            return analyzedMessage;
        }
    }
}
