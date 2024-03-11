package com.wyona.katie.integrations.slack;

import com.wyona.katie.integrations.CommonMessageSender;
import com.wyona.katie.integrations.slack.services.SlackClientService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.integrations.slack.models.SlackActionElement;
import com.wyona.katie.integrations.slack.models.SlackAnswer;
import com.wyona.katie.integrations.slack.services.DomainService;
import com.wyona.katie.models.*;
import com.wyona.katie.models.slack.*;
import com.wyona.katie.services.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.context.support.ResourceBundleMessageSource;

import com.wyona.katie.integrations.slack.services.DomainService;

import org.springframework.scheduling.annotation.Async;

import java.io.StringWriter;
import java.util.*;

import freemarker.template.Template;

/**
 * Send a message (answer to question) to Slack
 *
 * Trigger sending a message to Slack by using the following  REST interface of Katie
 *
 * POST /api/v1/slack/events
 *
 * {
 *  "challenge": "string",
 *  "event": {
 *   "channel": "C018TT68E72",
 *    "text": "What time is it?",
 *    "type": "message",
 *    "user": "string"
 *  },
 *  "event_id": "string",
 *  "team_id": "string",
 *  "token": "hoMub0zJ8kMXIxgqFgrDIc3x",
 *  "type": "event_callback"
 * }
 */
@Slf4j
@Component
public class SlackMessageSender extends CommonMessageSender  {

    private static final String CHANNEL_ID = "channel_id";
    private static final String QUESTION_UUID = "question_uuid";

    private static final String FORMAT_MARKDOWN = "mrkdwn";
    private static final String FORMAT_PLAIN_TEXT = "plain_text";

    private static final String JWT_CLAIM_UUID = "uuid";
    private static final String JWT_CLAIM_QUESTION_ASKED = "question-asked";

    private static final String ACTION_IMPROVE_CORRECT_ANSWER_SEPARATOR = "::";
    private static final String ACTION_CREATE_DOMAIN_SEPARATOR = "::";
    private static final String ACTION_CONNECT_DOMAIN_SEPARATOR = "::";

    private static final String CHANNEL_VIEW_CONNECT_DOMAIN = "connect_domain";
    private static final String CHANNEL_VIEW_CREATE_DOMAIN = "create_domain";

    private static final String USERNAME_KATIE = "Katie";

    @Value("${slack.number.of.questions.limit}")
    private int numberOfQuestionsLimit;

    @Value("${slack.post.message.url}")
    private String postMessageURL;

    @Value("${slack.katie.username}")
    private String usernameTechnicalUser;

    @Value("${new.context.mail.body.host}")
    private String katieHost;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private IAMService iamService;

    @Autowired
    private QuestionAnsweringService qaService;

    @Autowired
    private QuestionAnalyzerService questionAnalyzerService;

    @Autowired
    private NamedEntityRecognitionService nerService;

    @Autowired
    private WebhooksService webhooksService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private SlackClientService slackClientService;

    @Autowired
    private DataRepositoryService dataRepoService;

    @Autowired
    private MailerService mailerService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private ResourceBundleMessageSource messageSource;

    /**
     * Add technical user as member to a particular domain
     * @param domainId Katie domain Id
     */
    private User addTechnicalUserAsMember(String domainId) throws Exception {
        /*
        // INFO: Check whether user is already registered. TODO: Register user with Katie using OpenID Connect https://api.slack.com/legacy/oauth
        User user = getUserById(slackUserId); // INFO: For example "U0LP4BRLG"
        if (user == null) {
            try {
                user = registerUser(slackUserId);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
                throw e;
            }
        }
        try {
            authService.login(user.getUsername(), null);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
         */

        try {
            authService.login(usernameTechnicalUser, null);
            log.info("Signed in as technical Katie user '" + authService.getUserId() + "'.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        User user = authService.getUser(false, false);
        log.info("Signed in as Katie user '" + user.getId() + "'.");

        if (!contextService.isUserMemberOfDomain(user.getId(), domainId)) {
            log.warn("User '" + user.getId() + "' is not member of domain '" + domainId + "' yet!");
            try {
                contextService.addMember(user.getId(), false, false, null, domainId);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
                throw e;
            }
        }

        return user;
    }

    /**
     * Send expert's answer to resubmitted question back to Slack channel where question was asked
     */
    public void sendAnswerToResubmittedQuestion(ResubmittedQuestion question) {
        SlackEvent event = dataRepoService.getSlackConversationValues(question.getChannelRequestId());

        String slackChannelId = event.getChannel();
        log.info("Send expert's answer to resubmitted question back to Slack channel '" + slackChannelId + "' ...");

        // https://api.slack.com/apps/A0184KMLJJE/incoming-webhooks?
        // https://api.slack.com/messaging/sending

        String answer = Utils.convertToSlackMrkdwn(question.getAnswer());
        String[] args = new String[1];
        args[0] = "*" + question.getQuestion() + "*";
        String text = messageSource.getMessage("send.question.to.expert.answer", args, new Locale("en")) + "\n\n" + answer;

        // TODO: When question is resubmitted, them save "Timestamp of parent message", such that it can be used here
        String body = slackClientService.getResponseJSON(slackChannelId, null, text);

        slackClientService.send(body.toString(), postMessageURL, dataRepoService.getSlackBearerTokenOfDomain(question.getContextId(), slackChannelId));
    }

    /**
     * Send moderator's approved/moderated answer back to Slack channel where question was asked
     * @param channelId Slack channel Id
     * @param msgTs Timestamp of parent message
     */
    public void sendApprovedAnswer(ResubmittedQuestion question, String channelId, String msgTs) {
        log.info("Send moderator's approved answer back to Slack channel '" + channelId + "' ...");

        // https://api.slack.com/apps/A0184KMLJJE/incoming-webhooks?
        // https://api.slack.com/messaging/sending

        String a = getAnswerInclMetaInformation(question.getAnswer(), question.getQuestion(), question.getTimestampResubmitted().getTime(), new Locale("en"), null,"<br/>", "<hr/>");

        String answer = Utils.convertToSlackMrkdwn(a);

        String bodyAsJSON = slackClientService.getResponseJSON(channelId, msgTs, answer);

        slackClientService.send(bodyAsJSON, postMessageURL, dataRepoService.getSlackBearerTokenOfDomain(question.getContextId(), channelId));
    }

    /**
     * Send interaction response back to Slack (https://api.slack.com/interactivity/handling#message_responses)
     * @param interaction Parsed Slack interaction payload
     */
    @Async
    public void sendInteractionResponse(SlackInteraction interaction) {
        if (interaction.getResponse_url() != null) {
            // TODO: When is this ever the case?!
            log.info("Send interaction response back to Slack: " + interaction.getResponse_url());
        } else {
            log.info("Send interaction response back to Slack: " + postMessageURL);
        }

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

        String slackUserId = interaction.getUser().getId();
        try {
            authService.login(usernameTechnicalUser, null);
            log.info("Signed in as technical Katie user '" + authService.getUserId() + "', whereas Slack user Id is '" + slackUserId + "'.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        String type = interaction.getType();
        log.info("Interaction type: " + type);

        if (interaction.getActions() != null) {
            handleAction(interaction);
        } else if (type.equals("view_submission")) {
            SlackView view = interaction.getView();

            String privateMetadata = view.getPrivate_metadata();
            log.info("Private metadata: " + privateMetadata);
            String channelId = getValueFromPrivateMetadata(privateMetadata, CHANNEL_ID);
            log.info("Channel Id: " + channelId);

            String teamId = interaction.getUser().getTeam_id();
            if (view.getCallback_id().equals(CHANNEL_VIEW_CONNECT_DOMAIN)) {
                connectTeamChannelWithDomain(teamId, channelId, slackUserId, interaction);
                // TODO: Move sending of message here
            } else if (view.getCallback_id().equals(ChannelAction.SEND_BETTER_ANSWER.toString())) {
                SlackNodeAskedquestion askedQuestionNode = view.getState().getValues().getAskedquestion();
                String askedQuestion = askedQuestionNode.getSingle_line_input().getValue();
                SlackNodeBetteranswer betterAnswerNode = view.getState().getValues().getBetteranswwer();
                String betterAnswer = betterAnswerNode.getSingle_line_input().getValue();

                SlackNodeRelevanturl relevantUrlNode = view.getState().getValues().getRelevanturl();
                String relevantUrl = relevantUrlNode.getSingle_line_input().getValue();

                String questionUuid = getValueFromPrivateMetadata(privateMetadata, QUESTION_UUID);
                log.info("Question UUID: " + questionUuid);

                try {
                    saveBetterAnswer(questionUuid, teamId, channelId, askedQuestion, betterAnswer, relevantUrl);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    // TODO: Return exception message back to Slack
                }

                String[] args = new String[1];
                args[0] = askedQuestion;
                String _answerBackSlack = messageSource.getMessage("thanks.for.better.answer", args, new Locale("en"));
                SlackAnswer answerBackToSlack = new SlackAnswer(_answerBackSlack, FORMAT_MARKDOWN);
                slackClientService.send(embedAnswerIntoJSON(answerBackToSlack, channelId), postMessageURL, dataRepoService.getSlackBearerTokenOfTeam(teamId));
            } else if (view.getCallback_id().equals(CHANNEL_VIEW_CREATE_DOMAIN)) {
                log.info("Create new Katie domain and connect with Slack team / channel '" + teamId + " / " + channelId + "'.");
                SlackNodeEmail emailNode = view.getState().getValues().getEmail();
                String email = emailNode.getSingle_line_input().getValue();
                SlackAnswer answer = createDomainAction(teamId, channelId, interaction.getUser().getId(), email);
                slackClientService.send(embedAnswerIntoJSON(answer, channelId), postMessageURL, dataRepoService.getSlackBearerTokenOfTeam(teamId));
            } else {
                log.warn("No such view '" + view.getCallback_id() + "' implemented!");
            }
        } else {
            log.warn("No such interaction type '" + type + "' supported!");
        }
    }

    /**
     * @param privateMetadata Prviate metadata string, e.g. "channel_id::C056L19282H,question_uuid::TODO"
     * @param key Key, e.g. "channel_id"
     */
    private String getValueFromPrivateMetadata(String privateMetadata, String key) {
        String[] keyValues = privateMetadata.split(",");
        for (String keyValue : keyValues) {
            if (keyValue.startsWith(key)) {
                return keyValue.substring(key.length() + 2); // INFO: The 2 is the length of the separator "::"
            }
        }
        log.error("Private metadata '" + privateMetadata + "' does not contain key '" + key +"'!");
        return null;
    }

    /**
     * @param askedQuestion Asked question (TOOD: Use questionUUID instead)
     * @param answer Better answer
     */
    private void saveBetterAnswer(String questionUUID, String teamId, String channelId, String askedQuestion, String answer, String relevantUrl) throws Exception {
        log.info("Save better answer: " + answer);
        Context domain = domainService.getDomain(teamId, channelId);

        List<String> classifications  = new ArrayList<String>();
        Date dateAnswered = new Date();
        Date dateAnswerModified = dateAnswered;
        Date dateOriginalQuestionSubmitted = dateAnswered;
        boolean isPublic = false;
        User user = authService.getUser(false, false);
        Answer newQnA = new Answer(null, answer, ContentType.TEXT_PLAIN, relevantUrl, classifications, QnAType.DEFAULT, null, dateAnswered, dateAnswerModified, null, domain.getId(), null, askedQuestion, dateOriginalQuestionSubmitted, isPublic, new Permissions(isPublic), false, user.getId());
        contextService.addQuestionAnswer(newQnA, domain);
        contextService.train(new QnA(newQnA), domain, true);

        contextService.notifyExpertsToApproveProvidedAnswer(newQnA.getUuid(), domain, askedQuestion);

        String channelRequestId = null;
        webhooksService.deliver(WebhookTriggerEvent.BETTER_ANSWER_PROVIDED, domain.getId(), newQnA.getUuid(), newQnA.getOriginalquestion(), newQnA.getAnswer(), newQnA.getAnswerContentType(), null, channelRequestId);
    }

    /**
     * TODO
     */
    private void handleAction(SlackInteraction interaction) {

        ChannelAction actionId = null;
        try {
            actionId = ChannelAction.valueOf(interaction.getActions().get(0).getAction_id());
        } catch(IllegalArgumentException e) {
            log.error(e.getMessage() + " - abort handle action!");
            return;
        }

        String teamId = interaction.getUser().getTeam_id();
        String channelId = interaction.getChannel().getId();

        SlackAnswer answer = null;

        if (actionId.equals(ChannelAction.SEND_QUESTION_TO_EXPERT)) {
            String askedQuestion = interaction.getActions().get(0).getValue();
            answer = new SlackAnswer(sendQuestionToExpertAction(askedQuestion, interaction.getChannel(), interaction.getUser()), FORMAT_MARKDOWN);
        } else if (actionId.equals(ChannelAction.THUMB_UP) || actionId.equals(ChannelAction.THUMB_DOWN)) {
            answer = thumbUpDown(actionId, interaction);
        } else if (actionId.equals(ChannelAction.LOGIN)) {
            answer = loginAction(interaction);
        } else if (actionId.equals(ChannelAction.GET_PROTECTED_ANSWER)) {
            //String askedQuestion = interaction.getActions().get(0).getValue();
            answer = new SlackAnswer(getProtectedAnswerAction(interaction), FORMAT_MARKDOWN);
        } else if (actionId.equals(ChannelAction.IMPROVE_CORRECT_ANSWER)) {
            answer = editTrainedQuestionAnswerAction(interaction);
        } else if (actionId.equals(ChannelAction.ANSWER_QUESTION)) {
            answer = new SlackAnswer(addQnAAction(interaction), FORMAT_MARKDOWN);
        } else if (actionId.equals(ChannelAction.REQUEST_INVITATION)) {
            answer = requestInvitation(interaction);
        } else if (actionId.equals(ChannelAction.ENTER_BETTER_ANSWER)) {
            String askedQuestion = interaction.getActions().get(0).getValue();
            String questionUUID = "TODO"; // TODO: Get question UUID from interaction
            slackClientService.send(getView(interaction.getTrigger_id(), getBetterAnswerModal(questionUUID, askedQuestion, channelId)), "https://slack.com/api/views.open", dataRepoService.getSlackBearerTokenOfTeam(teamId));
            return;
        } else if (actionId.equals(ChannelAction.CREATE_DOMAIN)) {
            /*
            String[] parts = interaction.getActions().get(0).getValue().split(ACTION_CREATE_DOMAIN_SEPARATOR);
            String _teamId = parts[0];
            String _channelId = parts[1];
            String _inviterUserId = parts[2];
            answer = createDomainAction(_teamId, _channelId, _inviterUserId, "TODO:email");
             */
            slackClientService.send(getView(interaction.getTrigger_id(), getCreateConnectDomainModal(CHANNEL_VIEW_CREATE_DOMAIN, "Create Domain", "Connect", SlackViewStateValues.BLOCK_ID_EMAIL, "Your email address", "Valid email address, e.g. katie@wyona.com", "Please make sure that you use an email which can be verified by yourself.", channelId)), "https://slack.com/api/views.open", dataRepoService.getSlackBearerTokenOfTeam(teamId));
            return;
        } else if (actionId.equals(ChannelAction.CONNECT_DOMAIN)) {
            log.info("Channel Id: " + channelId);
            SlackDomainMapping mapping = domainService.getDomainMappingForSlackTeamChannel(teamId, channelId);
            if (mapping != null) {
                answer = new SlackAnswer("Team / channel '" + teamId + " / " + channelId + "' already connected with domain '" + mapping.getDomainId() + "', whereas connecting status is '" + mapping.getStatus() + "'.", FORMAT_MARKDOWN);
            } else {
                slackClientService.send(getView(interaction.getTrigger_id(), getCreateConnectDomainModal(CHANNEL_VIEW_CONNECT_DOMAIN, "Connect Domain", "Connect", SlackViewStateValues.BLOCK_ID_DOMAIN_ID, "Katie Domain Id", "Domain Id, e.g. f319131a-837a-42a6-8108-3b7z0bc1b8e4", "Copy domain Id from settings of your existing Katie domain.", channelId)), "https://slack.com/api/views.open", dataRepoService.getSlackBearerTokenOfTeam(teamId));
                return;
            }
        } else {
            log.warn("No such action id type '" + actionId + "' implemented yet.");
            String actionText = interaction.getActions().get(0).getText().getText();
            answer = new SlackAnswer("Action button '" + actionText + "' not implemented yet!", FORMAT_MARKDOWN);
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyX = mapper.createObjectNode();

        StringBuilder body = new StringBuilder("{");
        // DEPRECATED: body.append("\"as_user\":\"false\",");
        body.append("\"username\":\"" + USERNAME_KATIE + "\"");
        body.append(",");
        // TODO: escapeForJSON not necessary when using ObjectMapper
        body.append("\"text\":\"" + Utils.escapeForJSON(answer.getAnswer()) + "\"");
        // TODO: In the case of "getProtectedAnswerAction" there should be the question "Answer not helpful?", etc.
        if (actionId.equals(ChannelAction.LOGIN) || actionId.equals(ChannelAction.GET_PROTECTED_ANSWER)) {
            body.append(",");
            body.append("\"response_type\":\"ephemeral\"");  // https://api.slack.com/legacy/interactive-messages https://api.slack.com/messaging/managing#ephemeral
            body.append(",");
            body.append("\"replace_original\":false");
        }
        SlackActionElement[] elements = answer.getElements();
        if (elements.length > 0) {
            body.append(",");
            body.append(getBlocks(mapper, bodyX, answer));
        }
        body.append("}");

        slackClientService.send(body.toString(), interaction.getResponse_url(), dataRepoService.getSlackBearerTokenOfTeam(teamId));
    }

    /**
     * Request connection of Slack team/channel with Katie domain
     * @param teamId Slack team Id
     * @param channelId Slack channel Id
     * @param userId Slack user Id
     * @param interaction TODO
     */
    private void connectTeamChannelWithDomain(String teamId, String channelId, String userId, SlackInteraction interaction) {
        SlackView view = interaction.getView();

        SlackNodeDomainId domainIdNode = view.getState().getValues().getDomain_id(); // BLOCK_ID_DOMAIN_ID
        String domainId = domainIdNode.getSingle_line_input().getValue();

        //SlackNodeChannelId channelIdNode = view.getState().getValues().getChannel_id(); // BLOCK_ID_CHANNEL_ID
        //String channelId = channelIdNode.getSelect_id().getSelected_channel();

        log.info("Connect domain '" + domainId + "' with team / channel '" + teamId + " / " + channelId + "'  ...");

        SlackAnswer answer;
        if (!contextService.existsContext(domainId)) {
            String errorMsg = "No such Katie domain '" + domainId + "' available!";
            log.error(errorMsg);
            answer = new SlackAnswer(errorMsg, FORMAT_MARKDOWN);
        } else {
            SlackDomainMapping mapping = domainService.getDomainMappingForSlackTeamChannel(teamId, channelId);
            if (mapping != null) {
            //if (domainService.getDomain(teamId, channelId) != null) {
                log.error("There already exists a mapping between Team / Channel '" + teamId + " / " + channelId + "' and Domain '" + domainId + "'!");
                return;
            }

            log.info("Notify domain members (experts) for approval of the request by the Slack user '" + userId + "' ...");
            String token = generateConnectWithDomainToken(teamId, channelId, domainId, userId);
            int numberOfNotificationsSent = 0;
            try {
                Context domain = contextService.getContext(domainId);

                domainService.connectDomainIdWithSlackTeamId(domainId, teamId, channelId, ConnectStatus.NEEDS_APPROVAL, token);

                String link = domain.getHost() + "/api/v1/slack/connect-team-channel-domain?token=" + token;
                User[] members = contextService.getMembers(domainId, false);
                for (User user: members) {
                    if (user.getEmail() != null && user.getIsExpert()) {
                        mailerService.send(user.getEmail(), domain.getMailSenderEmail(), "[" + domain.getMailSubjectTag() + "] " + "Approve connecting domain with Slack team / channel", getMailBodyForApproval(userId, teamId, channelId, domainId, link), true);
                        numberOfNotificationsSent++;
                    }
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return;
            }

            if (numberOfNotificationsSent == 0) {
                log.warn("Thank you for connecting this channel with Katie, but unfortunately nobody could be notified for approval! Make sure that at least one Katie domain member is configured as expert.");
            }

            answer = new SlackAnswer("Thank you for connecting this channel with Katie. " + numberOfNotificationsSent + " domain members (experts) have been notified for approval. As soon as the connection will be approved, Katie will detect and answer questions.", FORMAT_MARKDOWN);
        }

        slackClientService.send(embedAnswerIntoJSON(answer, channelId), postMessageURL, dataRepoService.getSlackBearerTokenOfTeam(teamId));
        //send(embedAnswerIntoJSON(answer, channelId), interaction.getResponse_url(), dataRepoService.getSlackBearerTokenOfTeam(teamId));
    }

    /**
     *
     */
    private String embedAnswerIntoJSON(SlackAnswer answer, String channelId) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyX = mapper.createObjectNode();

        StringBuilder body = new StringBuilder("{");
        body.append("\"channel\":\"" + channelId + "\",");
        body.append("\"username\":\"" + USERNAME_KATIE + "\"");
        body.append(",");
        body.append("\"text\":\"" + answer.getAnswer() + "\"");
        SlackActionElement[] elements = answer.getElements();
        if (elements.length > 0) {
            body.append(",");
            body.append(getBlocks(mapper, bodyX, answer));
        }
        body.append("}");

        return body.toString();
    }

    /**
     *
     */
    private String getMailBodyForInvitingSlackUser(String userId, String teamId, String channelId, Context domain, String link) throws Exception {
        Map<String, Object> input = new HashMap<>();
        input.put("user_id", userId);
        input.put("team_id", teamId);
        input.put("channel_id", channelId);
        input.put("domain_id", domain.getId());
        input.put("link", link);

        StringWriter writer = new StringWriter();
        Template template = mailerService.getTemplate("notify-members-re-invitation-request_", Language.valueOf("en"), domain);
        template.process(input, writer);
        return writer.toString();
    }

    /**
     * Get message body that Slack Katie connection needs to be approved
     */
    private String getMailBodyForApproval(String userId, String teamId, String channelId, String domainId, String link) {
        // TODO: Use mailerService.getTemplate(...)

        StringBuilder body = new StringBuilder();
        body.append("<html><body>");
        body.append("<p>The Slack user '" + userId + "' requests to connect the Slack team / channel '" + teamId + " / " + channelId + "' with the Katie domain '" + domainId + "'.</p>");
        body.append("<p>Please click on the following link to approve this request:</p>");
        body.append("<p><a href=\"" + link + "\">" + link + "</a></p>");
        body.append("</body></html>");
        return body.toString();
    }

    /**
     * Generate JWT token in order to approve connection between Katie domain and Slack team/channel
     * @return JWT token
     */
    private String generateConnectWithDomainToken(String teamId, String channelId, String domainId, String userId) {
        JWTPayload jwtPayload = new JWTPayload();
        jwtPayload.setIss("Katie");
        HashMap<String, String> claims = new HashMap<String, String>();
        claims.put(JWTClaims.TEAM_ID, teamId);
        claims.put(JWTClaims.CHANNEL_ID, channelId);

        // TODO: Not really used
        claims.put(JWTClaims.DOMAIN_ID, domainId);
        claims.put(JWTClaims.USER_ID, userId);

        jwtPayload.setPrivateClaims(claims);
        try {
            return jwtService.generateJWT(jwtPayload, 3600, null);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return null;
        }
    }

    /**
     * @param value View value as JSON
     */
    private String getView(String triggerId, String value) {
        StringBuilder view = new StringBuilder();

        view.append("{");
        // INFO: trigger_id is necessary, because otherwise Slack will throw error "missing required field" and not display view. Also see https://api.slack.com/interactivity/handling#modal_responses
        view.append("\"trigger_id\":\"" + triggerId + "\",");
        view.append("\"view\":" + value);
        view.append("}");

        return view.toString();
    }

    /**
     * https://api.slack.com/surfaces/modals
     */
    private String getBetterAnswerModal(String questionUUID, String askedQuestion, String channelId) {
        Locale locale = new Locale("en");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("type", "modal");
        rootNode.put("callback_id", ChannelAction.SEND_BETTER_ANSWER.toString());
        // INFO: See https://api.slack.com/surfaces/modals#view-object-fields
        rootNode.put("private_metadata",CHANNEL_ID + "::" + channelId + "," + QUESTION_UUID + "::" + questionUUID);

        ObjectNode titleNode = mapper.createObjectNode();
        rootNode.put("title", titleNode);
        titleNode.put("type", FORMAT_PLAIN_TEXT);
        titleNode.put("text", messageSource.getMessage("provide.better.answer", null, locale));

        ObjectNode submitNode = mapper.createObjectNode();
        rootNode.put("submit", submitNode);
        submitNode.put("type", FORMAT_PLAIN_TEXT);
        submitNode.put("text", "Send");

        ArrayNode blocksNode = mapper.createArrayNode();
        rootNode.put("blocks", blocksNode);

        // INFO: Asked question block
        ObjectNode inputAskedQuestionBlockNode = mapper.createObjectNode();
        blocksNode.add(inputAskedQuestionBlockNode);
        inputAskedQuestionBlockNode.put("type", "input");
        inputAskedQuestionBlockNode.put("block_id", SlackViewStateValues.BLOCK_ID_ASKED_QUESTION);

        ObjectNode elementAskedQuestionNode = mapper.createObjectNode();
        inputAskedQuestionBlockNode.put("element", elementAskedQuestionNode);
        elementAskedQuestionNode.put("type", "plain_text_input");
        elementAskedQuestionNode.put("action_id", "single_line_input");
        elementAskedQuestionNode.put("initial_value", askedQuestion);

        ObjectNode labelAskedQuestionNode = mapper.createObjectNode();
        inputAskedQuestionBlockNode.put("label", labelAskedQuestionNode);
        labelAskedQuestionNode.put("type", FORMAT_PLAIN_TEXT);
        labelAskedQuestionNode.put("text", messageSource.getMessage("asked.question", null, locale));

        ObjectNode hintNode = mapper.createObjectNode();
        inputAskedQuestionBlockNode.put("hint", hintNode);
        hintNode.put("type", FORMAT_PLAIN_TEXT);
        hintNode.put("text", "Please feel free to enter a corrected / modified version of the asked question");

        // INFO: Better answer block
        ObjectNode inputBetterAnswerBlockNode = mapper.createObjectNode();
        blocksNode.add(inputBetterAnswerBlockNode);
        inputBetterAnswerBlockNode.put("type", "input");
        inputBetterAnswerBlockNode.put("block_id", SlackViewStateValues.BLOCK_ID_BETTER_ANSWER);

        ObjectNode elementBetterAnswerNode = mapper.createObjectNode();
        inputBetterAnswerBlockNode.put("element", elementBetterAnswerNode);
        elementBetterAnswerNode.put("type", "plain_text_input");
        elementBetterAnswerNode.put("action_id", "single_line_input");
        elementBetterAnswerNode.put("multiline", true);
        //elementNode.put("response_url_enabled", true);
        ObjectNode placeholderNode = mapper.createObjectNode();
        elementBetterAnswerNode.put("placeholder", placeholderNode);
        placeholderNode.put("type", FORMAT_PLAIN_TEXT);
        placeholderNode.put("text", messageSource.getMessage("what.would.be.helpful.answer", null, locale));

        ObjectNode labelNode = mapper.createObjectNode();
        inputBetterAnswerBlockNode.put("label", labelNode);
        labelNode.put("type", FORMAT_PLAIN_TEXT);
        labelNode.put("text", messageSource.getMessage("better.answer", null, locale));

        // INFO: Relevant URL block
        ObjectNode relevantUrlBlockNode = mapper.createObjectNode();
        blocksNode.add(relevantUrlBlockNode);
        relevantUrlBlockNode.put("type", "input");
        relevantUrlBlockNode.put("block_id", SlackViewStateValues.BLOCK_ID_RELEVANT_URL);
        relevantUrlBlockNode.put("optional", true);

        ObjectNode urlElementNode = mapper.createObjectNode();
        relevantUrlBlockNode.put("element", urlElementNode);
        urlElementNode.put("type", "url_text_input");
        // TODO: "action_id":"url_text_input-action"
        urlElementNode.put("action_id", "single_line_input"); // See com.wyona.katie.models.slack.SlackNodeRelevanturl#getSingle_line_input()
        //urlElementNode.put("response_url_enabled", true);
        ObjectNode urlPlaceholderNode = mapper.createObjectNode();
        urlElementNode.put("placeholder", urlPlaceholderNode);
        urlPlaceholderNode.put("type", FORMAT_PLAIN_TEXT);
        urlPlaceholderNode.put("text", "Relevant URL"); // TODO

        ObjectNode urlLabelNode = mapper.createObjectNode();
        relevantUrlBlockNode.put("label", urlLabelNode);
        urlLabelNode.put("type", FORMAT_PLAIN_TEXT);
        urlLabelNode.put("text", "Relevant URL"); // TODO

        ObjectNode urlHintNode = mapper.createObjectNode();
        relevantUrlBlockNode.put("hint", urlHintNode);
        urlHintNode.put("type", FORMAT_PLAIN_TEXT);
        urlHintNode.put("text", "Relevant URL, e.g. https://www.fedlex.admin.ch/eli/cc/27/317_321_377/de"); // TODO

        /*
        // INFO: Dropdown block to select a channel
        ObjectNode dropdownBlockNode = mapper.createObjectNode();
        blocksNode.add(dropdownBlockNode);
        dropdownBlockNode.put("type", "section");
        //dropdownBlockNode.put("type", "input"); // TODO: According to Slack support, one has to set input instead section, but does not seem to work
        dropdownBlockNode.put("block_id", SlackViewStateValues.BLOCK_ID_CHANNEL_ID);
        ObjectNode textNode = mapper.createObjectNode();
        dropdownBlockNode.put("text", textNode);
        textNode.put("type", "plain_text");
        textNode.put("text", "Pick a channel from the dropdown list");
        ObjectNode accessoryNode = mapper.createObjectNode();
        dropdownBlockNode.put("accessory", accessoryNode);
        accessoryNode.put("action_id", "select_id");
        accessoryNode.put("type", "channels_select");
        accessoryNode.put("response_url_enabled", true); // https://api.slack.com/surfaces/modals/using#modal_response_url
        accessoryNode.put("initial_channel", channelId);
        ObjectNode dropdownPlaceholderNode = mapper.createObjectNode();
        accessoryNode.put("placeholder", dropdownPlaceholderNode);
        dropdownPlaceholderNode.put("type", "plain_text");
        dropdownPlaceholderNode.put("text", "Select a channel");
         */

        //log.info("ObjectMapper: " + rootNode.toString());
        return rootNode.toString();
    }

    /**
     * https://api.slack.com/surfaces/modals
     * @param title Title of Modal, e.g. "Provide better answer"
     * @param submitLabel Label of submit button, e.g. "Submit better answer"
     * @param inputLabel Label of input field, e.g. "Bessere Antwort"
     * @param placeholder Placeholder of input field, e.g. "What would be a better answer?"
     * @param hint Hint of input field, e.g. "Please make sure ..."
     */
    private String getCreateConnectDomainModal(String callbackId, String title, String submitLabel, String blockId, String inputLabel, String placeholder, String hint, String channelId) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode rootNode = mapper.createObjectNode();
        rootNode.put("type", "modal");
        rootNode.put("callback_id", callbackId);
        rootNode.put("private_metadata", CHANNEL_ID + "::" + channelId);

        ObjectNode titleNode = mapper.createObjectNode();
        rootNode.put("title", titleNode);
        titleNode.put("type", FORMAT_PLAIN_TEXT);
        titleNode.put("text", title);

        ObjectNode submitNode = mapper.createObjectNode();
        rootNode.put("submit", submitNode);
        submitNode.put("type", FORMAT_PLAIN_TEXT);
        submitNode.put("text", submitLabel);

        ArrayNode blocksNode = mapper.createArrayNode();
        rootNode.put("blocks", blocksNode);

        // INFO: Input block
        ObjectNode inputBlockNode = mapper.createObjectNode();
        blocksNode.add(inputBlockNode);
        inputBlockNode.put("type", "input");
        inputBlockNode.put("block_id", blockId);

        ObjectNode elementNode = mapper.createObjectNode();
        inputBlockNode.put("element", elementNode);
        elementNode.put("type", "plain_text_input");
        elementNode.put("action_id", "single_line_input");
        //elementNode.put("response_url_enabled", true);
        ObjectNode placeholderNode = mapper.createObjectNode();
        elementNode.put("placeholder", placeholderNode);
        placeholderNode.put("type", FORMAT_PLAIN_TEXT);
        placeholderNode.put("text", placeholder);

        ObjectNode labelNode = mapper.createObjectNode();
        inputBlockNode.put("label", labelNode);
        labelNode.put("type", FORMAT_PLAIN_TEXT);
        labelNode.put("text", inputLabel);

        ObjectNode hintNode = mapper.createObjectNode();
        inputBlockNode.put("hint", hintNode);
        hintNode.put("type", FORMAT_PLAIN_TEXT);
        hintNode.put("text", hint);

        /*
        // INFO: Dropdown block to select channel
        ObjectNode dropdownBlockNode = mapper.createObjectNode();
        blocksNode.add(dropdownBlockNode);
        dropdownBlockNode.put("type", "section");
        //dropdownBlockNode.put("type", "input"); // TODO: According to Slack support, one has to set input instead section, but does not seem to work
        dropdownBlockNode.put("block_id", SlackViewStateValues.BLOCK_ID_CHANNEL_ID);
        ObjectNode textNode = mapper.createObjectNode();
        dropdownBlockNode.put("text", textNode);
        textNode.put("type", "plain_text");
        textNode.put("text", "Pick a channel from the dropdown list");
        ObjectNode accessoryNode = mapper.createObjectNode();
        dropdownBlockNode.put("accessory", accessoryNode);
        accessoryNode.put("action_id", "select_id");
        accessoryNode.put("type", "channels_select");
        accessoryNode.put("response_url_enabled", true); // https://api.slack.com/surfaces/modals/using#modal_response_url
        accessoryNode.put("initial_channel", channelId);
        ObjectNode dropdownPlaceholderNode = mapper.createObjectNode();
        accessoryNode.put("placeholder", dropdownPlaceholderNode);
        dropdownPlaceholderNode.put("type", "plain_text");
        dropdownPlaceholderNode.put("text", "Select a channel");
         */

        //log.info("ObjectMapper: " + rootNode.toString());
        return rootNode.toString();
    }

    /**
     * @return JSON of blocks
     */
    private String getBlocks(ObjectMapper mapper, ObjectNode bodyX, SlackAnswer answer) {

        ArrayNode blocksNode = mapper.createArrayNode();
        StringBuilder body = new StringBuilder();

        bodyX.put("blocks", blocksNode);
        body.append("\"blocks\":["); // INFO: Start of Blocks

        if (answer.getAnswer() != null) {
            body.append(getTextSectionBlock(mapper, blocksNode, answer.getFormat(), answer.getAnswer()));
        }

        SlackActionElement[] elements = answer.getElements();
        if (answer.getAnswer() != null && elements.length > 0) {
            body.append(",");
        }

        if (elements.length > 0) {
            body.append("{"); // INFO: Start of Actions
            body.append("\"type\":\"actions\",");
            ArrayNode elementsNode = mapper.createArrayNode();
            body.append("\"elements\":["); // INFO: Start of Elements
            for (int i = 0; i < elements.length; i++) {
                body.append(getActionElement(mapper, elementsNode, elements[i]));
                if (i < elements.length - 1) {
                    body.append(",");
                }
            }
            body.append("]"); // INFO: End of Elements
            body.append("}"); // INFO: End of Actions
        }
        body.append("]"); // INFO: End of Blocks

        return body.toString();
    }

    /**
     * @param answerFormat Answer format, e.g. "mrkdwn" or "plain_text"
     * @param text Text of text block
     * @return JSON of text section block
     */
    private String getTextSectionBlock(ObjectMapper mapper, ArrayNode blocksNode, String answerFormat, String text) {
        ObjectNode blockNode = mapper.createObjectNode();
        blocksNode.add(blockNode);

        blockNode.put("type", "section");

        ObjectNode textNode = mapper.createObjectNode();
        textNode.put("type", answerFormat);
        textNode.put("text", text);
        blockNode.put("text", textNode);

        return "{\"type\":\"section\",\"text\":{\"type\":\"" + answerFormat + "\",\"text\":\"" + text + "\"}}";

    }

    /**
     * Send question to expert
     * @param question Question which was asked by Slack user
     * @param slackChannel Slack channel where question was asked
     * @param slackUser Slack user which asked question
     */
    private String sendQuestionToExpertAction(String question, SlackChannel slackChannel, SlackUser slackUser) {
        log.info("Slack user '" + slackUser.getName() + "' would like to send question to expert ...");

        // TODO: Get questioner language from Slack or from question
        Language questionerLanguage = Language.en;
        Locale locale = new Locale(questionerLanguage.toString());

        String remoteAddress = null;
        String domainId = dataRepoService.getDomainIdForSlackTeamChannel(slackUser.getTeam_id(), slackChannel.getId());
        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
            User user = getUserById(slackUser.getId());
            String channelRequestId = java.util.UUID.randomUUID().toString();
            // TODO: Add slackUser
            dataRepoService.addSlackConversationValues(channelRequestId, domainId, null, slackChannel.getId(), null);
            contextService.answerQuestionByNaturalIntelligence(question, user, ChannelType.SLACK, channelRequestId, null, null, questionerLanguage, null, remoteAddress, domain);
            String[] emailsTo = contextService.getMailNotificationAddresses(domain.getId());
            if (emailsTo.length > 0) {
                String[] args = new String[2];
                args[0] = "*" + Utils.escapeDoubleQuotes(question) + "*";
                args[1] = getKatieDomainLink(messageSource.getMessage("send.question.to.expert.expert", null, locale), domain, null);
                return messageSource.getMessage("send.question.to.expert.forwarded", args, locale);
            } else {
                String[] args = new String[3];
                args[0] = "*'" + Utils.escapeDoubleQuotes(question) + "'*";
                args[1] = getKatieResubmittedQuestionsLink(messageSource.getMessage("knowledge.base", null, locale), domain, null);
                args[2] = getKatieDomainMembersLink(messageSource.getMessage("configure", null, locale), domain, null);
                return messageSource.getMessage("send.question.to.expert.forwarded.but.no.experts.configured", args, locale);
            }
        } catch(Exception e) {
           log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

    /**
     * Get protected answer if user is authorized to read answer
     * @return protected answer as Slack mrkdwn if user is authorized to read answer, otherwise exception message
     */
     private String getProtectedAnswerAction(SlackInteraction interaction) {
        String slackUserId = interaction.getUser().getId();
        String slackTeamId = interaction.getUser().getTeam_id();
        String domainId = dataRepoService.getDomainIdForSlackTeamChannel(slackTeamId, interaction.getChannel().getId());
        log.info("Slack user with Id '" + slackUserId + "' would like to get protected answer from Katie domain '" + domainId + "' ...");

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return "No Katie domain for Slack team '" + slackTeamId + "'!";
        }

        try {
            String jwtToken = interaction.getActions().get(0).getValue();
            if (jwtService.isJWTValid(jwtToken, null)) {
                String uuidQuestionAnswer = jwtService.getJWTClaimValue(jwtToken, JWT_CLAIM_UUID);
                log.info("Check whether user with Id '" + slackUserId + "' is authorized to read protected answer with uuid  '" + uuidQuestionAnswer + "' of domain '" + domainId + "'");

                User user = getUserById(slackUserId);
                if (user != null && contextService.isUserMemberOfDomain(user.getId(), domainId)) {
                    // TODO: Move authorization check to API getAnswer(...)

                    String askedQuestion = jwtService.getJWTClaimValue(jwtToken, JWT_CLAIM_QUESTION_ASKED);
                    log.info("Asked question: " + askedQuestion);
                    ResponseAnswer answer = qaService.getAnswer(uuidQuestionAnswer, domain, user.getUsername(), true, askedQuestion);

                    if (answer.getPermissionStatus() == PermissionStatus.PERMISSION_DENIED) {
                        return "'" + user.getUsername() + "' is not authorized to read answer.";
                    } else if (answer.getPermissionStatus() == PermissionStatus.NOT_SUFFICIENT_PERMISSIONS_TO_READ_ANSWER) {
                        // TODO: Add hint about who could be contacted to get permission
                        return "I am sorry, but you do not have sufficient permissions to read answer. You might want to contact the administrator of the Katie domain connected with this Slack workspace, whereas your Slack user Id is '" + user.getUsername() + "'.";
                    } else {
                        log.info("User '" + slackUserId+ "' with permission status '" + answer.getPermissionStatus()+ "' is authorized to get amswer '" + answer.getUuid() + "'.");
                        return Utils.convertToSlackMrkdwn(getAnswerInclMetaInformation(answer.getAnswer(), answer.getOriginalQuestion(), answer.getDateOriginalQuestion(), new Locale("en"), answer.getUrl(), "<br/>","<hr/>"));
                    }
                } else {
                    return "User with Id '" + slackUserId + "' is not member of Katie domain '" + domainId + "'!";
                }
            } else {
                log.error("JWT token is not valid!");
                return "Get protected answer failed!";
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

    /**
     * Create new Katie domain and connect with provided Slack team / channel
     * @param email Email provided by user adding Katie to Slack channel, which can be different from Slack email
     */
    private SlackAnswer createDomainAction(String _teamId, String _channelId, String _inviterUserId, String email) {
        try {
            SlackDomainMapping mapping = domainService.getDomainMappingForSlackTeamChannel(_teamId, _channelId);
            if (mapping != null) {
                return new SlackAnswer("Team / channel '" + _teamId + " / " + _channelId + "' already connected with domain '" + mapping.getDomainId() + "', whereas connecting status is '" + mapping.getStatus() + "'.", FORMAT_MARKDOWN);
            } else {
                String message = createNewKnowledgeBaseAndSayHi(_teamId, _channelId, _inviterUserId, email);
                return new SlackAnswer(message, FORMAT_MARKDOWN);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new SlackAnswer(e.getMessage(), FORMAT_MARKDOWN);
        }
    }

    /**
     * Request invitation for a particular user
     */
    private SlackAnswer requestInvitation(SlackInteraction interaction) {
        String teamId = interaction.getUser().getTeam_id();
        String channelId = interaction.getChannel().getId();
        String userId = interaction.getUser().getId();
        //String userName = interaction.getUser().getName();
        //String userUserame = interaction.getUser().getUsername();

        try {
            Context domain = domainService.getDomain(teamId, channelId);
            if (domain == null) {
                return new SlackAnswer("No domain connected with Slack team / channel '" + teamId + " / " + channelId + "'!", FORMAT_MARKDOWN);
            }
            User[] members = contextService.getMembers(domain.getId(), false);
            int numberOfNotificationsSent = 0;
            for (User user: members) {
                if (user.getEmail() != null && user.getIsExpert()) {
                    mailerService.send(user.getEmail(), domain.getMailSenderEmail(), "[" + domain.getMailSubjectTag() + "] " + "Invite Slack user to your Katie domain", getMailBodyForInvitingSlackUser(userId, teamId, channelId, domain, "TODO"), true);
                    numberOfNotificationsSent++;
                }
            }
            return new SlackAnswer("Invitation request has been sent to " + numberOfNotificationsSent + " domain members.", FORMAT_MARKDOWN);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new SlackAnswer(e.getMessage(), FORMAT_MARKDOWN);
        }
    }

    /**
     * Save user feedback (thumb up or thumb down)
     */
    private SlackAnswer thumbUpDown(ChannelAction actionId, SlackInteraction interaction) {
        String teamId = interaction.getUser().getTeam_id();
        String channelId = interaction.getChannel().getId();

        try {
            Context domain = domainService.getDomain(teamId, channelId);
            String questionUuid = interaction.getActions().get(0).getValue();
            AskedQuestion askedQuestion = contextService.getAskedQuestionByUUID(questionUuid);
            Locale locale = Locale.ENGLISH; // TODO
            if (actionId.equals(ChannelAction.THUMB_UP)) {
                contextService.thumbUpDown(askedQuestion, true, domain);
                return new SlackAnswer(messageSource.getMessage("thanks.for.positive.feedback", null, locale), FORMAT_MARKDOWN);
            } else {
                contextService.thumbUpDown(askedQuestion, false, domain);
                return new SlackAnswer(messageSource.getMessage("thanks.for.negative.feedback", null, locale), FORMAT_MARKDOWN);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new SlackAnswer(e.getMessage(), FORMAT_MARKDOWN);
        }
    }

    /**
     * Get login action response
     * @return answer containing login link if user is authorized to access Katie domain, otherwise return message why user is not authorized
     */
     private SlackAnswer loginAction(SlackInteraction interaction) {
        String slackUserId = interaction.getUser().getId();
        String slackTeamId = interaction.getUser().getTeam_id();
        String domainId = dataRepoService.getDomainIdForSlackTeamChannel(slackTeamId, interaction.getChannel().getId());
        log.info("Slack user with Id '" + slackUserId + "' would like to log into Katie domain '" + domainId + "' ...");

        try {
            String jwtToken = interaction.getActions().get(0).getValue();
            if (jwtService.isJWTValid(jwtToken, null)) {
                User user = getUserById(slackUserId);

                if (user != null && contextService.isUserMemberOfDomain(user.getId(), domainId)) {
                    String token = jwtService.generateJWT(user.getUsername(), domainId, 600, null); // TODO: Make token validity configurable
                    iamService.addJWT(new Username(user.getUsername()), token); // INFO: See AuthenticationController, where iamService.hasValidJWT(...) is being checked

                    String[] args = new String[2];
                    args[0] = interaction.getUser().getName();
                    String hereTranslation = messageSource.getMessage("here", args, new Locale(user.getLanguage()));
                    Context domain = null;
                    try {
                        domain = contextService.getContext(domainId);
                    } catch(Exception e) {
                        log.error(e.getMessage(), e);
                        return new SlackAnswer("No Katie domain for Slack team '" + slackTeamId + "'!",FORMAT_MARKDOWN);
                    }
                    args[1] = getKatieLoginLink(hereTranslation, domain, token);

                    return new SlackAnswer(messageSource.getMessage("click.here.to.access.katie", args, new Locale(user.getLanguage())), FORMAT_MARKDOWN);
                } else {
                    log.warn("User '" + slackUserId + "' is not member of domain '" + domainId + "'.");
                    SlackAnswer answer = new SlackAnswer("Access to Katie denied, because you (" + slackUserId + ") are not a member of the Katie domain '" + domainId + "'.", FORMAT_MARKDOWN);
                    answer.addElement(new SlackActionElement("Request invitation ...", slackUserId + "," + domainId, ChannelAction.REQUEST_INVITATION));
                    return answer;
               }
            } else {
                log.error("JWT token is not valid!");
                return new SlackAnswer("Authentication failed!", FORMAT_MARKDOWN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new SlackAnswer(e.getMessage(), FORMAT_MARKDOWN);
        }
    }

    /**
     * @return answer containing link to improve/correct trained question-answer
     */
     private SlackAnswer editTrainedQuestionAnswerAction(SlackInteraction interaction) {
        String slackUserId = interaction.getUser().getId();
        String slackTeamId = interaction.getUser().getTeam_id();
        String domainId = dataRepoService.getDomainIdForSlackTeamChannel(slackTeamId, interaction.getChannel().getId());
        String[] uuidQuestion = interaction.getActions().get(0).getValue().split(ACTION_IMPROVE_CORRECT_ANSWER_SEPARATOR);
        String uuid = uuidQuestion[0];
        String question = uuidQuestion[1];
        log.info("Slack user with Id '" + slackUserId + "' would like to edit trained question answer '" + uuid + "' inside Katie domain '" + domainId + "' ...");

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new SlackAnswer("No Katie domain for Slack team '" + slackTeamId + "'!", FORMAT_MARKDOWN);
        }

        try {
            User user = getUserById(slackUserId);

            if (user != null && contextService.isUserMemberOfDomain(user.getId(), domain.getId())) {
                String token = jwtService.generateJWT(user.getUsername(), domainId, 600, null); // TODO: Make token validity configurable
                iamService.addJWT(new Username(user.getUsername()), token); // INFO: See AuthenticationController, where iamService.hasValidJWT(...) is being checked
                String[] args = new String[3];
                args[0] = interaction.getUser().getName();
                args[1] = getImproveAnswerLink(messageSource.getMessage("here", args, new Locale("en")), domain, uuid, question, token);
                args[2] = uuid;
                return new SlackAnswer(messageSource.getMessage("improve.answer.link", args, new Locale("en")), FORMAT_MARKDOWN); // TODO: Language
            } else {
                String[] args = new String[2];
                args[0] = slackUserId;
                args[1] = domain.getId();
                SlackAnswer answer = new SlackAnswer(messageSource.getMessage("improve.answer.not.member", args, new Locale("en")), FORMAT_MARKDOWN);
                answer.addElement(new SlackActionElement("Request invitation ...", slackUserId + "," + domainId, ChannelAction.REQUEST_INVITATION));
                return answer;
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new SlackAnswer(e.getMessage(), FORMAT_MARKDOWN);
        }
    }

    /**
     * @return answer containing link to add new QnA
     */
    private String addQnAAction(SlackInteraction interaction) {
        String slackUserId = interaction.getUser().getId();
        String slackTeamId = interaction.getUser().getTeam_id();
        String domainId = dataRepoService.getDomainIdForSlackTeamChannel(slackTeamId, interaction.getChannel().getId());
        String question = interaction.getActions().get(0).getValue();
        log.info("Slack user with Id '" + slackUserId + "' would like to answer question '" + question + "' inside Katie domain '" + domainId + "' ...");

        Context domain = null;
        try {
            domain = contextService.getContext(domainId);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return "No Katie domain for Slack team '" + slackTeamId + "'!";
        }

        try {
            User user = getUserById(slackUserId);

            if (user != null && contextService.isUserMemberOfDomain(user.getId(), domain.getId())) {
                String token = jwtService.generateJWT(user.getUsername(), domainId, 600, null); // TODO: Make token validity configurable
                iamService.addJWT(new Username(user.getUsername()), token); // INFO: See AuthenticationController, where iamService.hasValidJWT(...) is being checked
                return "Hey " + interaction.getUser().getName() + ", click " + getAddQnALink("here", domain, question, token) + " in order to answer question '" + question + "'.";
            } else {
                return "User '" + user.getUsername() + "' is not member of domain '" + domain.getId() + "'. Verification necessary whether user is a Slack user!";
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return e.getMessage();
        }
    }

    /**
     * @param message Received message, e.g. "<@U018UN3S54G> how is the weather today?" or "How is the weather today?"
     * @param userIdKatie Slack user Id of Katie
     * @return analyzed message (without user Id) when message contains question (e.g. "How is the weather today?") or null otherwise
     */
    private AnalyzedMessage containsQuestion(String message, String userIdKatie, Context domain) {
        // INFO: The Slack event actually contains the user Id below inside the blocks elements (rich_text_section), but we parse this information ourselves below
        String userIdAtWhichMessageIsDirected = null;
        String messageWithoutUserId = message;

        if (message.startsWith("<@")) {
            int endOfUserId = message.indexOf(">") + 1;

            userIdAtWhichMessageIsDirected = message.substring(0, endOfUserId); // INFO: For example <@U018UN3S54G>
            userIdAtWhichMessageIsDirected = userIdAtWhichMessageIsDirected.substring(2, userIdAtWhichMessageIsDirected.length() - 1); // INFO: For example U018UN3S54G
            log.info("User Id to which message is directed: " + userIdAtWhichMessageIsDirected);

            messageWithoutUserId = message.substring(endOfUserId).trim();
            log.info("Message without user Id: '" + messageWithoutUserId + "'");

            if (!userIdAtWhichMessageIsDirected.equals(userIdKatie)) {
                log.info("Don't answer message '" + message + "', because it is directed at a particular person other than Katie or another bot.");
                return null;
            } else {
                log.info("Message directed at Katie, which has Slack user Id '" + userIdKatie + "'.");
            }
        }

        AnalyzedMessage analyzedMessage = questionAnalyzerService.analyze(messageWithoutUserId, domain);
        if (analyzedMessage.getContainsQuestions()) {
            log.info("Question(s) detected ...");
            return analyzedMessage;
        } else {
            return null;
        }
    }

    /**
     * Send event response back to Slack, e.g. when a user is asking a question
     */
    @Async
    public void sendEventResponse(SlackEventWrapper eventWrapper) {
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


        SlackEvent event = eventWrapper.getEvent();

        String teamId = eventWrapper.getTeam_id();
        String channelId = event.getChannel();

        if(event.getChannel_type().equals("im")) {
            // INFO: Direct message of user to Katie

            SlackAuthorizations[] authorizations = eventWrapper.getAuthorizations();
            if (authorizations != null && authorizations.length > 0) {
                log.info("Number of authorizations: " + authorizations.length);
                log.info("Team Id / User Id: " + authorizations[0].getTeam_id() + " / " + authorizations[0].getUser_id());
                log.info("Bot Id: " + event.getBot_id());
                if (authorizations[0].getIs_bot()) {
                    log.warn("Direct message by bot '" + event.getUser() + "' (" + teamId + "/" + channelId + ") not supported yet! Ignore and do not reply ...");
                    return;
                } else {
                    log.info("User '" + event.getUser() + "' is not a bot.");
                }
            }

            handleDirectMessage(teamId, channelId, event.getUser());
            
            return;
        }

        String domainId = dataRepoService.getDomainIdForSlackTeamChannel(teamId, channelId);
        String userIdKatie = dataRepoService.getKatieUserId(teamId);

        // INFO: The subtype channel_join is sent everytime something is added to a channel, not just when Katie is being added!
        if (event.getSubtype() != null && event.getSubtype().equals("channel_join")) {
            String inviterUserId = event.getInviter();
            log.info("User '" + event.getUser() + "' was invited to team '" + teamId + "' / channel '" + channelId + "' by user '" + inviterUserId + "' (Event text: " + event.getText() + ").");

            // INFO: Check whether Katie was invited to join channel
            if (!event.getUser().equals(userIdKatie)) {
                log.info("Invited user '" + event.getUser() + "' does not match with Katie user id '" + userIdKatie + "', therefore don't do anything.");
            } else {
                log.info("Invited user Id matches with Katie user Id '" + userIdKatie + "', therefore try to connect team/channel '" + teamId + " / " + channelId + "' with a Katie domain ...");
                try {
                    if (domainId == null) {
                        log.info("No Katie domain linked yet with Slack team / channel '" + teamId + " / " + channelId + "', therefore create Katie domain ...");
                        sayHiAndAskWhetherToCreateOrConnectDomain(teamId, channelId, inviterUserId);
                    } else {
                        sayHi(domainId, channelId);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

            return;
        }

        analyticsService.logMessageReceived(domainId, ChannelType.SLACK, channelId);

        Context domain = null;
        try {
            // TODO: Use domainId
            //domain = contextService.getContext(domainId);
            domain = domainService.getDomain(teamId, channelId);
        } catch (Exception e) {
            log.error(e.getMessage());
            return;
        }

        // INFO: Ignore questions within threads (if the message is a parent message and not a thread response, then thread_ts is null)
        if (isThreadResponse(event)) {
            log.info("Thread response detected, therefore do not answer.");

            String channelRequestId = dataRepoService.getSlackChannelRequestId(event.getThread_ts());
            if (channelRequestId == null) {
                log.warn("No channel request Id available for Slack thread timestamp '" + event.getThread_ts() + "'!");
                return;
            }

            String threadId = event.getChannel() + "-" + event.getThread_ts();
            contextService.saveThreadMessage(domain, event.getChannel(), channelRequestId, threadId, event.getText());

            if (domain != null && domain.getAnswersMustBeApprovedByModerator()) {
                log.info("Answers of domain '" + domain.getId() + "' must be approved by moderator, therefore check whether thread response contains useful answer ...");
                // INFO: Maybe thread message contains answer to question for which Katie's suggested answer has not been approved yet
                if (answerNotApprovedYet(channelRequestId)) {
                    log.info("TODO: Update Katie's suggested answer which is not approved yet ...");
                }
            } else {
                log.debug("Answers of domain '" + domain.getId() + "' do not need to be approved.");
            }

            return;
        } else {
            log.info("No thread response, therefore let's check whether message might contain a question ...");
        }

        // INFO: New message (TODO: Check whether previous message was from same user and if so concatenate)
        String message = event.getText();
        AnalyzedMessage analyzedMessage = containsQuestion(message, userIdKatie, domain);
        if (analyzedMessage == null) {
            log.info("No question detected ('" + message + "'), therefore no response will be sent.");
            return;
        }

        String slackUserId = event.getUser();
        try {
            authService.login(usernameTechnicalUser, null);
            log.info("Signed in as technical Katie user '" + authService.getUserId() + "', whereas Slack user Id is '" + slackUserId + "'.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        if (domain == null) {
            log.warn("No Katie domain linked yet with Slack team '" + teamId + "' and channel '" + channelId + "'! Maybe because channel '" + event.getChannel() + "' is shared, but owned by the team '" + teamId + "', to which Katie has not been added yet. Or maybe because it is a direct message to Katie.");
            if (event.getChannel_type().equals("im")) {
                log.info("Channel type is 'im', which means that the Slack user '" + event.getUser() + "' sent a direct message to Katie.");
            }
            // TODO: Send message explaining that this is a channel shared by two (or more) workspaces
            return;
        } else {
            if (!isAuthorized(domain.getId(), eventWrapper.getSecurityToken())) {
                log.warn("Slack event message is not authorized to access Katie domain '" + domain.getId() + "'!");
                // TODO: Katie should not send message to Slack or should it send a warning?!
            }
        }

        Date dateSubmitted = new Date();

        String answer = null;
        String uuid = null;
        ResponseAnswer topAnswer = null;
        boolean answerAvailable = false;
        PermissionStatus permissionStatus = PermissionStatus.UNKNOWN;

        String msgTs = event.getTs();
        try {
            // TODO: Get count during the last minute and limit rate by the minute
            int count = dataRepoService.getQuestionCountDuringLast24Hours(domain.getId(), ChannelType.SLACK);
            log.info(count + " questions have been asked for the domain '" + domain.getId() + "' during the last 24 hours by members of the Slack team '" + teamId + "'. The Slack user '" + slackUserId + "' is asking another question ...");

            if (count < numberOfQuestionsLimit) { // TODO: Get limit from domain configuration
                String channelRequestId = java.util.UUID.randomUUID().toString();
                dataRepoService.addSlackConversationValues(channelRequestId, domainId, event.getTeam(), event.getChannel(), msgTs);

                String remoteAddress = "TODO:Slack";
                // TODO: Set classifications, whereas force to lower case
                List<String> classifications = new ArrayList<String>();
                if (analyzedMessage.getQuestionsAndContexts().size() > 1) {
                    log.info("TODO: Handle multiple questions ...");
                }
                boolean checkAuthorization = false; // TODO
                String messageId = event.getClient_msg_id();
                List<ResponseAnswer> answers = qaService.getAnswers(analyzedMessage.getMessage(), classifications, messageId, domain, dateSubmitted, remoteAddress, ChannelType.SLACK, channelRequestId, 2, 0, checkAuthorization, null,false, false);

                if (answers != null && answers.size() > 0) {
                    topAnswer = answers.get(0);
                    uuid = topAnswer.getUuid();
                    answerAvailable = true;
                    permissionStatus = topAnswer.getPermissionStatus();
                    if (answerIsProtected(permissionStatus)) {
                        log.info("Answer '" + answers.get(0).getUuid() + "' is protected: " + permissionStatus);
                        answer = "I have an answer to your question '" + analyzedMessage.getMessage() + "', but the answer is protected and is only visible to users which are authorized accordingly.";
                    } else {
                        log.info("Return first answer of result set ...");
                        // TODO: If there is more than one answer, then tell user that there is more than one answer
                        answer = Utils.convertToSlackMrkdwn(getAnswerInclMetaInformation(topAnswer.getAnswer(), topAnswer.getOriginalQuestion(), topAnswer.getDateOriginalQuestion(), new Locale("en"), topAnswer.getUrl(),"<br/>", "<hr/>"));
                        // INFO: Not necessary, because we use ObjectMapper instead StringBuilder
                        //answer = Utils.escapeForJSON(answer);
                    }
                } else {
                    answerAvailable = false;
                    // TODO: Detect language of question, whereas see https://pm.wyona.com/issues/2739
                    answer = messageSource.getMessage("sorry.do.not.know.an.answer", null, new Locale("en"));
                }
            } else {
                answerAvailable = false;
                answer = "WARNING: The limit of " + numberOfQuestionsLimit + " questions during the last 24 hours has been reached.";

                String msgToAdmin = "WARNING: The limit of " + numberOfQuestionsLimit + " questions during the last 24 hours for the domain '" + domain.getId() + "' and channel type '" + ChannelType.SLACK + "' has been reached.";
                log.warn(msgToAdmin);
                mailerService.notifyAdministrator(msgToAdmin, null, domain.getMailSenderEmail(), false);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            answerAvailable = false;
            answer = "Sorry, an error occured '" + e.getMessage() + "'.";
        }

        // INFO: See https://api.slack.com/messaging/interactivity and https://api.slack.com/reference/block-kit/blocks

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();

        body.put("channel", channelId);

        // INFO: Reply to parent message (https://api.slack.com/messaging/sending#threading)
        log.info("Reply to parent message as thread response: " + msgTs);
        body.put("thread_ts", msgTs);

        // DEPRECATED: body.append("\"as_user\":\"false\",");
        body.put("username", USERNAME_KATIE);

        // TODO: Set format MARKDOWN
        body.put("text", answer);

        // TODO: Use getBlocks(SlackAnswer)
        SlackAnswer sAnswer = new SlackAnswer(answer, FORMAT_MARKDOWN);

        //getBlocks(sAnswer);

        // INFO: Start Blocks
        ArrayNode blocksNode = mapper.createArrayNode();
        body.put("blocks", blocksNode);
        getTextSectionBlock(mapper, blocksNode, FORMAT_MARKDOWN, answer);

        if (answerIsProtected(permissionStatus)) {
            try {
                getProtectedAnswerButton(mapper, blocksNode, uuid, analyzedMessage.getMessage(), domain);
            } catch(Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        if (domain.getSlackConfiguration().getButtonSendToExpertEnabled() || domain.getSlackConfiguration().getButtonImproveAnswerEnabled()) {
            // TODO: Replace plain text by markdown?!
            getTextSectionBlock(mapper, blocksNode, FORMAT_PLAIN_TEXT, messageSource.getMessage("answer.helpful", null, new Locale("en")));
        }

        // INFO: Start Actions re "Answer not helpful"
        ObjectNode actionsNode = mapper.createObjectNode();
        blocksNode.add(actionsNode);
        actionsNode.put("type", "actions");

        ArrayNode elementsNode = mapper.createArrayNode();
        actionsNode.put("elements", elementsNode);

        if (domain.getSlackConfiguration().getButtonSendToExpertEnabled()) {
            // TODO: Check whether experts are configured for this domain
            if (topAnswer != null) { // TODO
                String questionUuid = topAnswer.getQuestionUUID();
                String correct = messageSource.getMessage("correct.answer", null, Locale.ENGLISH);
                SlackActionElement elementThumbUp = new SlackActionElement(Emoji.THUMB_UP + " " + correct, questionUuid, ChannelAction.THUMB_UP);
                sAnswer.addElement(elementThumbUp);
                getActionElement(mapper, elementsNode, elementThumbUp);
            }
            if (topAnswer != null) { // TODO
                String questionUuid = topAnswer.getQuestionUUID();
                String wrong = messageSource.getMessage("wrong.answer", null, Locale.ENGLISH);
                SlackActionElement elementThumbDown = new SlackActionElement(Emoji.THUMB_DOWN + " " + wrong, questionUuid, ChannelAction.THUMB_DOWN);
                sAnswer.addElement(elementThumbDown);
                getActionElement(mapper, elementsNode, elementThumbDown);
            }

            SlackActionElement elementSendToExpert = new SlackActionElement(messageSource.getMessage("send.question.to.expert", null, Locale.ENGLISH), Utils.escapeDoubleQuotes(analyzedMessage.getMessage()), ChannelAction.SEND_QUESTION_TO_EXPERT);
            sAnswer.addElement(elementSendToExpert);
            getActionElement(mapper, elementsNode, elementSendToExpert);

            SlackActionElement elementProvideBetterAnswer = new SlackActionElement(messageSource.getMessage("provide.better.answer", null, Locale.ENGLISH), Utils.escapeDoubleQuotes(analyzedMessage.getMessage()), ChannelAction.ENTER_BETTER_ANSWER);
            sAnswer.addElement(elementProvideBetterAnswer);
            getActionElement(mapper, elementsNode, elementProvideBetterAnswer);
        }

        if (answerAvailable) {
            if (permissionStatus == PermissionStatus.IS_PUBLIC) {
                log.info("Available answer '" + uuid + "' is public.");
            } else {
                log.warn("Answer '" + uuid + "' is not public : " + permissionStatus);
            }
            if (permissionStatus == PermissionStatus.PERMISSION_DENIED || permissionStatus == PermissionStatus.NOT_SUFFICIENT_PERMISSIONS_TO_READ_ANSWER) {
                log.info("User '" + slackUserId + "' does not have sufficient permissions to improve or correct answer (" + permissionStatus + ") and therefore the 'improve or correct answer' button does not make sense.");
            } else {
                if (domain.getSlackConfiguration().getButtonImproveAnswerEnabled()) {
                    log.info("User '" + slackUserId + "' has sufficient permissions to improve or correct answer: " + permissionStatus);
                    SlackActionElement elementImproveOrCorrectAnswer = new SlackActionElement(messageSource.getMessage("improve.answer", null, Locale.ENGLISH), uuid + ACTION_IMPROVE_CORRECT_ANSWER_SEPARATOR + Utils.escapeDoubleQuotes(analyzedMessage.getMessage()), ChannelAction.IMPROVE_CORRECT_ANSWER);
                    sAnswer.addElement(elementImproveOrCorrectAnswer);
                    getActionElement(mapper, elementsNode, elementImproveOrCorrectAnswer);
                }
            }
        } else {
            log.info("No answer available.");
            if (domain.getInformUserNoAnswerAvailable()) {
                // TODO
            }
            if (domain.getSlackConfiguration().getButtonAnswerQuestionEnabled()) {
                SlackActionElement elementAnswerQuestion = new SlackActionElement("Answer question ...", Utils.escapeDoubleQuotes(analyzedMessage.getMessage()), ChannelAction.ANSWER_QUESTION);
                sAnswer.addElement(elementAnswerQuestion);
                getActionElement(mapper, elementsNode, elementAnswerQuestion);
            }
        }

        if (domain.getSlackConfiguration().getButtonLoginKatieEnabled()) {
            try {
                getLogIntoKatieActionElement(mapper, elementsNode, domain);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }

        // INFO: When moderation is enabled, then Katie does not send any messages back to the channel
        if (domain.getAnswersMustBeApprovedByModerator()) {
            log.info("Moderation approval enabled for domain '" + domain.getName() + "' (Id: " + domain.getId() + "), therefore do not return an answer.");
            // INFO: QuestionAnsweringService sends notification to moderators
            if (domain.getInformUserReModeration()) {
                // TODO: If finding an answer takes long, then maybe this message should be sent before
                log.info("Inform user re moderation ...");
                slackClientService.send(responsesNeedApproval(channelId, domain.getName()), postMessageURL, dataRepoService.getSlackBearerTokenOfDomain(domain.getId(), channelId));
            } else {
                log.info("Do not inform user re moderation.");
            }
            return;
        }

        slackClientService.send(body.toString(), postMessageURL, dataRepoService.getSlackBearerTokenOfDomain(domain.getId(), channelId));
    }

    /**
     * Get message in order to tell user, that responses from Katie need to be approved by human moderator
     * @param domainName Katie domain name, e.g. "Katie Demo"
     */
    private String responsesNeedApproval(String channelId, String domainName) {
        String[] args = new String[1];
        args[0] = domainName;
        String message =  messageSource.getMessage("responses.need.approval", args, new Locale("en"));

        SlackAnswer answer = new SlackAnswer(message, FORMAT_MARKDOWN);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyX = mapper.createObjectNode();

        StringBuilder body = new StringBuilder("{");
        body.append("\"channel\":\"" + channelId + "\",");
        // DEPRECATED: body.append("\"as_user\":\"false\",");
        body.append("\"username\":\"" + USERNAME_KATIE + "\",");
        body.append("\"text\":\"" + answer.getAnswer() + "\"");
        body.append(",");
        body.append(getBlocks(mapper, bodyX, answer));
        body.append("}");

        return body.toString();
    }

    /**
     * Check whether Slack message is a thread response, e.g. containing clarifying questions or response to original question
     * @param event Slack message
     * @return true when message is thread response and false otherwise
     */
    private boolean isThreadResponse(SlackEvent event) {
        String msgTs = event.getTs();
        log.info("Check whether this is a thread response maybe containing clarifying questions ...");
        if (event.getThread_ts() != null && !event.getThread_ts().equals(msgTs)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check whether Katie's answer has already been approved
     * @param channelRequestId Channel request Id, e.g. "3139c14f-ae63-4fc4-abe2-adceb67988af"
     * @return true when answer is not approved yet and false otherwise
     */
    private boolean answerNotApprovedYet(String channelRequestId) {
        try {
            AskedQuestion question = dataRepoService.getQuestionByChannelRequestId(channelRequestId);
            if (question.getModerationStatus() != null && question.getModerationStatus().equals(ModerationStatus.NEEDS_APPROVAL)) {
                log.info("Moderation status: " + question.getModerationStatus());
                return true;
            } else {
                log.info("No moderation status set.");
                return false;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    /**
     * Handle direct message of user to Katie
     * @param userId Slack user Id
     */
    private void handleDirectMessage(String teamId, String channelId, String userId) {
        SlackDomainMapping[] mappings = domainService.getDomainMappingsForSlackTeam(teamId);

        String message = "Hi '" + userId + "'! Direct messages to Katie are not supported yet,";
        if (mappings != null && mappings.length > 0) {
            message = message + " but the following channels of the Slack workspace '" + teamId + "' are already connected with Katie:";
            for (SlackDomainMapping mapping: mappings) {
                message = message + " " + mapping.getChannelId();
            }
        } else {
            message = message + " but you can invite Katie to any channel of your workspace '" + teamId + "'.";
        }
        log.info("Response to direct message: " + message);

        //String message =  messageSource.getMessage("hi.my.name.is.katie.ask.create.connect.domain", null, new Locale("en"));

        SlackAnswer answer = new SlackAnswer(message, FORMAT_MARKDOWN);
        //answer.addElement(new SlackActionElement("Create new Katie domain ...", teamId + ACTION_CREATE_DOMAIN_SEPARATOR + channelId + ACTION_CREATE_DOMAIN_SEPARATOR + userId, ChannelAction.CREATE_DOMAIN));
        //answer.addElement(new SlackActionElement("Connect with existing Katie domain ...", teamId + ACTION_CONNECT_DOMAIN_SEPARATOR + channelId + ACTION_CONNECT_DOMAIN_SEPARATOR + userId, ChannelAction.CONNECT_DOMAIN));

        StringBuilder body = new StringBuilder("{");
        body.append("\"channel\":\"" + channelId + "\",");
        // DEPRECATED: body.append("\"as_user\":\"false\",");
        body.append("\"username\":\"" + USERNAME_KATIE + "\",");
        body.append("\"text\":\"" + answer.getAnswer() + "\"");
        //body.append(",");
        //body.append(getBlocks(answer));
        body.append("}");

        slackClientService.send(body.toString(), postMessageURL, dataRepoService.getSlackBearerTokenOfTeam(teamId));
    }

    /**
     * Say Hi and ask user whether to create new domain or connect with existing domain
     * @param inviterUserId Slack user Id of person inviting Katie to join this channel
     */
    private void sayHiAndAskWhetherToCreateOrConnectDomain(String teamId, String channelId, String inviterUserId) throws Exception {
        String message =  messageSource.getMessage("hi.my.name.is.katie.ask.create.connect.domain", null, new Locale("en"));

        SlackAnswer answer = new SlackAnswer(message, FORMAT_MARKDOWN);
        answer.addElement(new SlackActionElement(messageSource.getMessage("create.new.domain", null, new Locale("en")), teamId + ACTION_CREATE_DOMAIN_SEPARATOR + channelId + ACTION_CREATE_DOMAIN_SEPARATOR + inviterUserId, ChannelAction.CREATE_DOMAIN));
        answer.addElement(new SlackActionElement(messageSource.getMessage("connect.with.existing.domain", null, new Locale("en")), teamId + ACTION_CONNECT_DOMAIN_SEPARATOR + channelId + ACTION_CONNECT_DOMAIN_SEPARATOR + inviterUserId, ChannelAction.CONNECT_DOMAIN));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyX = mapper.createObjectNode();

        StringBuilder body = new StringBuilder("{");
        body.append("\"channel\":\"" + channelId + "\",");
        // DEPRECATED: body.append("\"as_user\":\"false\",");
        body.append("\"username\":\"" + USERNAME_KATIE + "\",");
        body.append("\"text\":\"" + answer.getAnswer() + "\"");
        body.append(",");
        body.append(getBlocks(mapper, bodyX, answer));
        body.append("}");

        slackClientService.send(body.toString(), postMessageURL, dataRepoService.getSlackBearerTokenOfTeam(teamId));
    }

    /**
     * Create Katie domain and say hi :-)
     * @param inviterUserId Slack user Id of person inviting Katie to join this channel, e.g. "U0LP4BRLG"
     * @param email Email provided by user adding Katie to Slack channel, which can be different from Slack email
     */
    private String createNewKnowledgeBaseAndSayHi(String teamId, String channelId, String inviterUserId, String email) throws Exception {
        log.info("Create new knowledge base / Katie domain for Slack team / channel '" + teamId + " / " + channelId + "' ...");
        // TODO: Event wrapper does not contain team name nor channel name
        Context newDomain = domainService.createDomainForSlackTeam(teamId, null, channelId, null);

        User technicalUser = addTechnicalUserAsMember(newDomain.getId());

        User user = null;
        if (iamService.usernameExists(new Username(inviterUserId))) {
            log.info("Katie user exists with Slack user Id '" + inviterUserId + "'.");
            user = iamService.getUserByIdWithoutAuthCheck(inviterUserId);
        } else if (iamService.usernameExists(new Username(email))) {
            user = iamService.getUserByUsername(new Username(email), false, false);
        } else {
            log.info("Create Katie user with username '" + inviterUserId + "' and email '" + email + "' and add as member to new Katie domain '" + newDomain.getId() + "'.");
            // TODO: WARNING: Check whether user account creation requires approval!!!
            user = iamService.createUser(new Username(inviterUserId), email, Role.USER, authService.generatePassword(8), true,"TODO", "TODO", "en", false);
        }
        contextService.addMember(user.getId(), true, false, RoleDomain.OWNER, newDomain.getId());
        // TODO: Send confirmation instead invitation
        contextService.sendInvitation(technicalUser, user.getEmail(), user.getUsername(), newDomain.getId());
        
        String domainURL = newDomain.getHost() + "/#/domain/" + newDomain.getId();
        mailerService.notifyAdministrator("Slack user created new domain", "Slack user with email '" + email + "' created new Katie domain " + domainURL, null, false);

        String[] args = new String[3];
        args[0] = getKatieDomainLink("*" + newDomain.getId() + "*", newDomain, null); // TODO: Consider adding JWT token
        args[1] = user.getEmail();
        args[2] = katieHost;
        return messageSource.getMessage("hi.my.name.is.katie", args, new Locale("en"));
    }

    /**
     * Tell user that Katie Slack App is already connected with existing Katie domain
     * @param domainId Domain Id with which Katie Slack App is already connected with
     * @param channelId Channel Id where Katie Slack App has been invited to
     */
    private void sayHi(String domainId, String channelId) throws Exception {
        Context domain = contextService.getContext(domainId);

        String[] args = new String[1];
        args[0] = getKatieLoginLink("*" + domainId + "*", domain, null); // TBD: Add token
        //args[0] = "*" + domainId + "*";
        String answer = messageSource.getMessage("hi.my.name.is.katie.linked.with.existing.domain", args, new Locale("en"));

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyX = mapper.createObjectNode();

        StringBuilder body = new StringBuilder("{");
        body.append("\"channel\":\"" + channelId + "\",");
        // DEPRECATED: body.append("\"as_user\":\"false\",");
        body.append("\"username\":\"" + USERNAME_KATIE + "\",");
        body.append("\"text\":\"" + answer + "\"");
        body.append(",");
        body.append(getBlocks(mapper, bodyX, new SlackAnswer(answer, FORMAT_MARKDOWN)));
        body.append("}");

        slackClientService.send(body.toString(), postMessageURL, dataRepoService.getSlackBearerTokenOfDomain(domainId, channelId));
    }

    /**
     * Notify channel that Katie has been connected with Slack channel
     * @param mapping Slack/Katie mapping
     */
    public void sendNotificationThatConnectionGotApproved(SlackDomainMapping mapping) throws Exception {

        String answer = "Katie has been successfully connected with this Slack channel.";

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyX = mapper.createObjectNode();

        StringBuilder body = new StringBuilder("{");
        body.append("\"channel\":\"" + mapping.getChannelId() + "\",");
        // DEPRECATED: body.append("\"as_user\":\"false\",");
        body.append("\"username\":\"" + USERNAME_KATIE + "\",");
        body.append("\"text\":\"" + answer + "\"");
        body.append(",");
        body.append(getBlocks(mapper, bodyX, new SlackAnswer(answer, FORMAT_MARKDOWN)));
        body.append("}");

        slackClientService.send(body.toString(), postMessageURL, dataRepoService.getSlackBearerTokenOfDomain(mapping.getDomainId(), mapping.getChannelId()));
    }

    /**
     * Get Katie user with a particular Slack user Id
     * @param userId Slack user Id, e.g. "U018F80DU1C"
     */
    private User getUserById(String userId) {
        // TODO: Add Prefix "slack:"
        return iamService.getUserByUsername(new Username(userId), false, false);
    }

    /**
     * @param element Action element object
     * @return JSON of action element
     */
    private ObjectNode getActionElement(ObjectMapper mapper, ArrayNode elementsNode, SlackActionElement element) {

        ObjectNode elementNode = mapper.createObjectNode();
        elementsNode.add(elementNode);

        elementNode.put("type", "button");

        ObjectNode textNode = mapper.createObjectNode();
        elementNode.put("text", textNode);
        textNode.put("type", FORMAT_PLAIN_TEXT);
        textNode.put("text", element.getLabel());
        textNode.put("emoji", false);

        elementNode.put("value", element.getValue());
        elementNode.put("action_id", "" + element.getAction());

        return elementNode;
    }

    /**
     *
     */
    private ObjectNode getLogIntoKatieActionElement(ObjectMapper mapper, ArrayNode elementsNode, Context domain) throws Exception {

        ObjectNode elementNode = mapper.createObjectNode();
        elementsNode.add(elementNode);
        elementNode.put("type","button");

        ObjectNode textNode = mapper.createObjectNode();
        elementNode.put("text", textNode);
        textNode.put("type",FORMAT_PLAIN_TEXT);
        textNode.put("text","Log into Katie ...");
        textNode.put("emoji", false);

        elementNode.put("value", jwtService.generateJWT(null, domain.getId(), 600, null)); // TODO: Make token validity configurable
        elementNode.put("action_id", "" + ChannelAction.LOGIN);

        return elementNode;
    }

    /**
     * Create action button to get protected answer
     * @param uuid UUID of question/answer
     */
    private ObjectNode getProtectedAnswerButton(ObjectMapper mapper, ArrayNode blocksNode, String uuid, String question, Context domain) throws Exception {

        ObjectNode actionsNode = mapper.createObjectNode();
        blocksNode.add(actionsNode);

        actionsNode.put("type", "actions");

        ArrayNode elementsNode = mapper.createArrayNode();
        actionsNode.put("elements", elementsNode);

        ObjectNode elementNode = mapper.createObjectNode();
        elementsNode.add(elementNode);

        elementNode.put("type", "button");

        ObjectNode textNode = mapper.createObjectNode();
        elementNode.put("text", textNode);
        textNode.put("type", FORMAT_PLAIN_TEXT);
        textNode.put("text", "Get protected answer ...");
        textNode.put("emoji", false);

        HashMap<String, String> claims = new HashMap<String, String>();
        claims.put(JWT_CLAIM_UUID, uuid);
        claims.put(JWT_CLAIM_QUESTION_ASKED, question);
        elementNode.put("value", jwtService.generateJWT(null, domain.getId(), 600, claims)); // TODO: Make token validity configurable
        elementNode.put("action_id", "" + ChannelAction.GET_PROTECTED_ANSWER);

        return actionsNode;
    }

    /**
     * Get link in mrkdwn format (https://api.slack.com/reference/surfaces/formatting#linking-urls)
     */
    private String getLink(String url, String text) {
        try {
            url = url.replaceAll(" ", "+");
            log.debug("Spaces replaced: " + url);
            // INFO: URLEncoder.encode(url, "UTF-8") would also replace ":" by %3A and "/" by %2F etc.
            //log.info("Encoded URL: " + URLEncoder.encode(url, "UTF-8"));
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        //return "[" + text + "](" + url + ")"; // INFO: MS Teams is using this markdown format, which does not work with Slack
        return "<" + url + "|" + text + ">";
    }

    /**
     *
     */
    private String getKatieDomainMembersLink(String linkText, Context domain, String jwtToken) {
        if (jwtToken != null) {
            return getLink(domain.getHost() + "/#/domain/" + domain.getId() + "/members?token=" + jwtToken, linkText);
        } else {
            return getLink(domain.getHost() + "/#/domain/" + domain.getId() + "/members", linkText);
        }
    }

    /**
     *
     */
    private String getKatieResubmittedQuestionsLink(String linkText, Context domain, String jwtToken) {
        if (jwtToken != null) {
            return getLink(domain.getHost() + "/#/resubmitted-questions/" + domain.getId() + "?token=" + jwtToken, linkText);
        } else {
            return getLink(domain.getHost() + "/#/resubmitted-questions/" + domain.getId(), linkText);
        }
    }

    /**
     * Get link to a particular Katie domain
     */
    private String getKatieDomainLink(String linkText, Context domain, String jwtToken) {
        if (jwtToken != null) {
            return getLink(domain.getHost() + "/#/domain/" + domain.getId() + "?token=" + jwtToken, linkText);
        } else {
            return getLink(domain.getHost() + "/#/domain/" + domain.getId(), linkText);
        }
    }

    /**
     * Generate login link
     */
    private String getKatieLoginLink(String linkText, Context domain, String jwtToken) {
        if (jwtToken != null) {
            return getLink(domain.getHost() + "/#/login?token=" + jwtToken, linkText);
        } else {
            return getLink(domain.getHost() + "/#/login", linkText);
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
     * @param submittedQuestion Question which was asked by user and for which answer should be improved or corrected
     */
    private String getAddQnALink(String linkText, Context domain, String submittedQuestion, String jwtToken) {
        return getLink(domain.getHost() + "/#/trained-questions-answers/" + domain.getId() + "/add?token=" + jwtToken + "&q=" + submittedQuestion, linkText);
    }

    /**
     * @param token Security token, e.g. "T01848J69AP-C018TT68E72-U018A7XUWSY"
     * @return true when token is correct, false otherwise
     */
    private boolean isAuthorized(String domainId, String token) {
        log.warn("TODO: Check whether Slack token '" + token + "' is authorized to send message to Katie domain '" + domainId + "'!");
        return true;
    }

    /**
     * @return true when answer is protected and false otherwise
     */
    private boolean answerIsProtected(PermissionStatus ps) {
        if (ps == PermissionStatus.PERMISSION_DENIED || ps == PermissionStatus.NOT_SUFFICIENT_PERMISSIONS_TO_READ_ANSWER || ps == PermissionStatus.USER_AUTHORIZED_TO_READ_ANSWER) { // INFO: Even when a user has sufficient permissions (USER_AUTHORIZED_TO_READ_ANSWER), the answer should not be replied to the channel directly, because other channel members, which do not have sufficient permissions could also read the answer. The answer should only be sent as "ephemeral" message.
            return true;
        } else {
            return false;
        }
    }
}
