package com.wyona.katie.controllers.v1;

import com.wyona.katie.integrations.discord.listeners.MessageSender;
import com.wyona.katie.integrations.matrix.MatrixMessageSender;
import com.wyona.katie.models.Error;
import com.wyona.katie.models.discord.DiscordDomainMapping;
import com.wyona.katie.models.discord.DiscordEvent;
import com.wyona.katie.models.slack.SlackDomainMapping;
import com.wyona.katie.models.slack.SlackEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.integrations.slack.SlackMessageSender;
import com.wyona.katie.models.*;
import com.wyona.katie.services.*;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

//import org.hibernate.validator.constraints.NotEmpty;
//import javax.validation.constraints.NotEmpty;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import lombok.extern.slf4j.Slf4j;

import javax.mail.Message;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//import java.security.Principal;
import java.nio.file.AccessDeniedException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Controller to get questions (all and resubmitted) (Version 1)
 */
@Slf4j
@RestController
@RequestMapping(value = "/api/v1/questions") 
public class QuestionsController {

    @Autowired
    private SlackMessageSender slackMessageSender;

    @Autowired
    private MatrixMessageSender matrixMessageSender;

    @Autowired
    private MailerService mailerService;

    @Autowired
    private DataRepositoryService dataRepoService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private QuestionAnsweringService questionAnsweringService;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private RememberMeService rememberMeService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private DynamicExpressionEvaluationService dynamicExprEvalService;

    @Autowired
    private MessageSender messageSender;

    @Autowired
    private AuthenticationService authenticationService;

    private final static String NO_CHANNEL_ID = "NO_CHANNEL_ID";

    /**
     * REST interface to approve answer of asked question
     */
    @RequestMapping(value = "/asked/{qid}/approve", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Approve answer of asked question")
    public ResponseEntity<?> approveAnswerOfAskedQuestion(
        @ApiParam(name = "qid", value = "UUID of question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
        @PathVariable("qid") String qid,
        HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        log.info("Approve answer of asked question '" + qid + "' ...");

        try {
            AskedQuestion question = contextService.getAskedQuestionByUUID(qid);
            String domainId = question.getDomainId();
            if (contextService.isMemberOrAdmin(domainId)) {
                if (question.getModerationStatus().equals(ModerationStatus.NEEDS_APPROVAL)) {
                    dataRepoService.updateModerationStatus(qid, ModerationStatus.APPROVED);
                    analyticsService.logAnswerApproved(domainId, question.getChannelType());

                    Context domain = contextService.getContext(domainId);
                    Answer answer = contextService.getQnA(question.getQuestion(), question.getQnaUuid(), domain);

                    // TODO: Analyze question?!
                    Sentence analyzedQuestion = new Sentence("", null, null);
                    answer.setAnswer(dynamicExprEvalService.postProcess(answer.getAnswer(), analyzedQuestion));

                    if (question.getChannelType().equals(ChannelType.SLACK)) {
                        sendApprovedAnswerToSlack(question.getChannelRequestId(), answer);
                    } else if (question.getChannelType().equals(ChannelType.EMAIL)) {
                        sendAnswerByEmail(qid, domain, question, answer);
                    } else if (question.getChannelType().equals(ChannelType.MS_TEAMS)) {
                        log.warn("TODO: Send answer to MS Teams");
                    } else if (question.getChannelType().equals(ChannelType.DISCORD)) {
                        sendApprovedAnswerToDiscord(question.getChannelRequestId(), answer, qid);
                    } else if (question.getChannelType().equals(ChannelType.MATRIX)) {
                        sendAnswerToMatrix(question, answer);
                    } else {
                        String errorMessage = "No such channel type '" + question.getChannelType() + "' implemented!";
                        log.error(errorMessage);
                        return new ResponseEntity<>(new com.wyona.katie.models.Error(errorMessage, "NO_SUCH_CHANNEL_TYPE_IMPLEMENTED"), HttpStatus.BAD_REQUEST);
                    }

                    Rating rating = new Rating();
                    rating.setQuestionuuid(question.getUUID());
                    rating.setQnauuid(question.getQnaUuid());
                    rating.setUserquestion(question.getQuestion());
                    rating.setEmail(question.getUsername()); // TODO
                    rating.setDate(new Date());
                    rating.setRating(10);
                    contextService.rateAnswer(domain, rating);

                    return new ResponseEntity<>(question, HttpStatus.OK);
                } else {
                    log.warn("Moderation status is not '" + ModerationStatus.NEEDS_APPROVAL + "', but '" + question.getModerationStatus() + "', therefore cannot be approved!");
                    String errorMessage = "Answer of question '" + qid + "' does not need approval!";
                    log.error(errorMessage);
                    return new ResponseEntity<>(new com.wyona.katie.models.Error(errorMessage, "NO_APPROVAL_NECESSARY"), HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     *
     */
    private void sendAnswerToMatrix(AskedQuestion question, Answer answer) {
        ResubmittedQuestion qna = new ResubmittedQuestion(null, question.getQuestion(), null, null, ChannelType.MATRIX, question.getChannelRequestId(),null, null, null, null, null, question.getTimestamp(), answer.getAnswer(), null, null, null, question.getDomainId());
        matrixMessageSender.sendRoomMessage(qna.getQuestion(), qna.getAnswer(), qna.getChannelRequestId());
    }

    /**
     * Send approved answer back to Slack
     * @param channelRequestId Channel request Id
     * @param answer Answer suggested by Katie
     */
    private void sendApprovedAnswerToSlack(String channelRequestId, Answer answer) {
        SlackEvent event = dataRepoService.getSlackConversationValues(channelRequestId);
        ResubmittedQuestion qna = new ResubmittedQuestion(null, answer.getOriginalquestion(), null, null, ChannelType.SLACK, channelRequestId, null, null,null, null, null, new java.util.Date(answer.getDateOriginalQuestion()), answer.getAnswer(), null, null, null, answer.getDomainid());
        slackMessageSender.sendApprovedAnswer(qna, event.getChannel(), event.getTs());
    }

    /**
     * Send approved answer back to Discord
     * @param channelRequestId Channel request Id
     * @param answer Answer suggested by Katie
     * @param questionUuid UUID of question asked
     */
    private void sendApprovedAnswerToDiscord(String channelRequestId, Answer answer, String questionUuid) {
        DiscordEvent event = dataRepoService.getDiscordConversationValuesForChannelRequestId(channelRequestId);
        log.debug("Send answer to Discord channel '" + event.getChannelId() + "' ...");
        messageSender.sendAnswer(questionUuid, answer.getAnswer(), event.getChannelId(), event.getMsgId());
    }

    /**
     * Send answer by email
     */
    private void sendAnswerByEmail(String qid, Context domain, AskedQuestion question, Answer answer) throws Exception {
        log.info("Get message for question ID '" + qid + "'!");

        String uuidOfEmailFrom = dataRepoService.getEmailConversationValues(question.getChannelRequestId());
        Message message = contextService.readEmail(domain, uuidOfEmailFrom);

        List<ResponseAnswer> answers = new ArrayList<ResponseAnswer>();
        answers.add(questionAnsweringService.mapAnswer(answer, null,null));
        Message replyMessage = mailerService.generateReply(message, answers, domain);
        log.info("Send answer to '" + replyMessage.getAllRecipients()[0] + "' ...");
        // TODO: Finish implementation using message as argument to send email
        if (false) {
            mailerService.send(replyMessage);
        }
        mailerService.send(replyMessage.getAllRecipients()[0].toString(), domain.getMailSenderEmail(), replyMessage.getSubject(), mailerService.getBody(replyMessage), false);
    }

    /**
     * REST interface to discard answer of asked question (because maybe there is no meaningful answer)
     */
    @RequestMapping(value = "/asked/{qid}/discard", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Discard answer of asked question")
    public ResponseEntity<?> discardAnswerOfAskedQuestion(
            @ApiParam(name = "qid", value = "UUID of question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("qid") String qid,
            HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        log.info("Discard answer of asked question '" + qid + "' ...");

        try {
            AskedQuestion question = contextService.getAskedQuestionByUUID(qid);
            String domainId = question.getDomainId();
            if (contextService.isMemberOrAdmin(domainId)) {
                dataRepoService.updateModerationStatus(qid, ModerationStatus.DISCARDED);
                analyticsService.logAnswerDiscarded(domainId, question.getChannelType());

                Rating rating = new Rating();
                rating.setQuestionuuid(question.getUUID());
                rating.setQnauuid(question.getQnaUuid());
                rating.setUserquestion(question.getQuestion());
                rating.setEmail(question.getUsername()); // TODO
                rating.setDate(new Date());
                rating.setRating(0);
                contextService.rateAnswer(contextService.getDomain(domainId), rating);

                return new ResponseEntity<>(question, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to ignore answer of asked question (because maybe somebody else already answered)
     */
    @RequestMapping(value = "/asked/{qid}/ignore", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Ignore answer of asked question (because maybe somebody else already answered)")
    public ResponseEntity<?> ignoreAnswerOfAskedQuestion(
            @ApiParam(name = "qid", value = "UUID of question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("qid") String qid,
            HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        log.info("Ignore answer of asked question '" + qid + "' ...");

        try {
            AskedQuestion question = contextService.getAskedQuestionByUUID(qid);
            String domainId = question.getDomainId();
            if (contextService.isMemberOrAdmin(domainId)) {
                dataRepoService.updateModerationStatus(qid, ModerationStatus.IGNORED);
                analyticsService.logAnswerIgnored(domainId, question.getChannelType());
                return new ResponseEntity<>(question, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to replace suggested answer to asked question
     */
    @RequestMapping(value = "/asked/{qid}/replace", method = RequestMethod.PUT, produces = "application/json")
    @ApiOperation(value="Replace suggested answer to asked question")
    public ResponseEntity<?> replaceSuggestedAnswerToAskedQuestion(
            @ApiParam(name = "qid", value = "UUID of question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')", required = true)
            @PathVariable("qid") String qid,
            @ApiParam(name = "qna-uuid", value = "UUID of new answer / QnA (e.g. 'a629fdd5-fd6f-41a0-a516-b723aba954e7')", required = true)
            @RequestParam("qna-uuid") String uuid,
            HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        try {
            AskedQuestion question = contextService.getAskedQuestionByUUID(qid);
            String domainId = question.getDomainId();
            if (contextService.isMemberOrAdmin(domainId)) {
                if (contextService.existsQnA(uuid, contextService.getContext(domainId))) {
                    log.info("Replace suggested answer '" + question.getQnaUuid() + "' by new answer / QnA '" + uuid + "' ...");
                    dataRepoService.updateSuggestedAnswerUUID(qid, uuid);
                    analyticsService.logAnswerCorrected(domainId, question.getChannelType());
                    return new ResponseEntity<>(question, HttpStatus.OK);
                } else {
                    String errorMsg = "No such QnA " + uuid;
                    log.warn(errorMsg);
                    return new ResponseEntity<>(new com.wyona.katie.models.Error(errorMsg, "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get a particular asked question
     */
    @RequestMapping(value = "/asked/{qid}", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get a particular asked question")
    public ResponseEntity<?> getAskedQuestion(
            @ApiParam(name = "qid", value = "UUID of question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("qid") String qid,
            HttpServletRequest request, HttpServletResponse response) {

        User signedInUser = rememberMeService.tryAutoLogin(request, response);
        if (signedInUser != null) {
            log.info("User '" + signedInUser.getUsername() + "' is signed in.");
        } else {
            log.info("User is not signed in.");
        }

        log.info("Get asked question '" + qid + "' ...");

        try {
            AskedQuestion question = contextService.getAskedQuestionByUUID(qid);
            String domainId = question.getDomainId();
            if (contextService.isMemberOrAdmin(domainId)) {

                Context domain = contextService.getContext(domainId);
                question = completeAskedQuestionWithChannelInfo(domain, question);

                return new ResponseEntity<>(question, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "SQL_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get email containing asked question
     */
    //@RequestMapping(value = "/asked/{qid}/email", method = RequestMethod.GET, produces = "application/octet-stream")
    @RequestMapping(value = "/asked/{qid}/email", method = RequestMethod.GET, produces = "text/plain")
    @ApiOperation(value="Get email containing asked question")
    public ResponseEntity<?> getEmailContainingAskedQuestion(
            @ApiParam(name = "qid", value = "UUID of question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("qid") String qid,
            HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        log.info("Get email containing asked question '" + qid + "' ...");

        try {
            AskedQuestion question = contextService.getAskedQuestionByUUID(qid);
            String domainId = question.getDomainId();
            if (contextService.isMemberOrAdmin(domainId)) {
                String emailId = dataRepoService.getEmailConversationValues(question.getChannelRequestId());
                //Message message = contextService.readEmail(contextService.getContext(domainId), emailId);
                //log.info("Get message with subject '" + message.getSubject() + "'.");
                //return new ResponseEntity<>(message, HttpStatus.OK);

                java.io.File file = contextService.getEmailFile(contextService.getContext(domainId), emailId);
                log.info("Get original email containing question: " + file.getAbsolutePath());
                try {
                    org.springframework.core.io.InputStreamResource resource = new org.springframework.core.io.InputStreamResource(new java.io.FileInputStream(file));
                    return ResponseEntity.ok()
                            //.headers(headers)
                            .contentLength(file.length())
                            //.contentType(org.springframework.http.MediaType.parseMediaType("application/json"))
                            .body(resource);
                } catch(java.io.FileNotFoundException e) {
                    log.error(e.getMessage(), e);
                    return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "FILE_NOT_FOUND"), HttpStatus.NOT_FOUND);
                }
            } else {
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "SQL_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get thread messages of asked question
     */
    @RequestMapping(value = "/asked/{qid}/thread", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get thread messages of asked question")
    public ResponseEntity<?> getThreadOfAskedQuestion(
            @ApiParam(name = "qid", value = "UUID of asked question (e.g. '194b6cf3-bad2-48e6-a8d2-8c55eb33f027')",required = true)
            @PathVariable("qid") String qid,
            HttpServletRequest request, HttpServletResponse response) {

        User signedInUser = rememberMeService.tryAutoLogin(request, response);

        log.info("Get thread messages of asked question '" + qid + "' ...");

        try {
            AskedQuestion question = contextService.getAskedQuestionByUUID(qid);
            String domainId = question.getDomainId();
            if (signedInUser != null && contextService.isMemberOrAdmin(domainId, signedInUser)) {
                log.info("Get all thread messages of asked question '" + qid + "' linked with domain '" + domainId + "' ...");
                Context domain = contextService.getContext(domainId);

                question = completeAskedQuestionWithChannelInfo(domain, question);

                String channelId = null;
                log.info("Channel type: " + question.getChannelType());
                if (question.getChannelType().equals(ChannelType.SLACK)) {
                    channelId = question.getSlackChannelId();
                } else if (question.getChannelType().equals(ChannelType.DISCORD)) {
                    channelId = question.getDiscordChannelId();
                } else if (question.getChannelType().equals(ChannelType.EMAIL)) {
                    log.error("Get thread not implemented yet for channel type 'EMAIL'!");
                    return new ResponseEntity<>(new com.wyona.katie.models.Error("No thread", "NOT_FOUND"), HttpStatus.NOT_FOUND);
                } else {
                    channelId = NO_CHANNEL_ID;
                }

                if (channelId != null) {
                    String[] messages = contextService.getThreadMessages(domain, channelId, question.getChannelRequestId());
                    if (messages != null) {
                        ObjectMapper mapper = new ObjectMapper();
                        ObjectNode body = mapper.createObjectNode();
                        body.put("domainId", domainId);
                        body.put("channelType", "" + question.getChannelType());
                        body.put("slackTeamId", question.getSlackTeamId());
                        body.put("discordGuildId", question.getDiscordGuildId());
                        body.put("channelId", channelId);
                        body.put("channelRequestId", question.getChannelRequestId());
                        body.put("clientThreadId", "TODO"); // TODO: Get client thread Id (Discord: 1177606391125508156, Slack: C02RRQP23K3-1693701392.348199), whereas it is currently saved inside messages XML
                        ArrayNode messagesNode = mapper.createArrayNode();
                        for (int i = 0; i < messages.length; i++) {
                            ObjectNode msgNode = mapper.createObjectNode();
                            msgNode.put("text", messages[i]);
                            messagesNode.add(msgNode);
                        }
                        body.put("messages", messagesNode);

                        return new ResponseEntity<>(body.toString(), HttpStatus.OK);
                    } else {
                        return new ResponseEntity<>(new com.wyona.katie.models.Error("No thread", "NOT_FOUND"), HttpStatus.NOT_FOUND);
                    }
                } else {
                    String errMsg = "Channel type '" + question.getChannelType() + "' not implemented yet!";
                    log.error(errMsg);
                    return new ResponseEntity<>(new com.wyona.katie.models.Error(errMsg, "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to add a thread message / response to an originally asked question
     */
    @RequestMapping(value = "/asked/thread-message", method = RequestMethod.POST, produces = "application/json")
    @ApiOperation(value="Add a thread message / response to an originally asked question")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataType = "string", paramType = "header") })
    public ResponseEntity<?> addThreadMessage(
            @ApiParam(name = "client-message-id", value = "Client message / thread Id", required = true)
            @RequestParam(value = "client-message-id", required = true) String messageId,
            @ApiParam(name = "domain-id", value = "Katie domain Id", required = true)
            @RequestParam(value = "domain-id", required = true) String domainId,
            @ApiParam(name = "message", value = "The 'message' field is required, all other fields are optional", required = true)
            @RequestBody ThreadMessage message,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            authenticationService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        User signedInUser = rememberMeService.tryAutoLogin(request, response);

        // TODO: Check security / access token

        log.info("Add a thread message / response to an originally asked question ...");

        try {
            Context domain = contextService.getDomain(domainId);

            AskedQuestion askedQuestion = dataRepoService.getQuestionByMessageId(messageId);

            String channelRequestId = askedQuestion.getChannelRequestId();
            log.info("Channel request Id: " + channelRequestId);
            ChannelType channelType = askedQuestion.getChannelType();
            log.info("Channel type: " + channelType);

            String channelId = null;
            if (channelType.equals(ChannelType.SLACK)) {
                SlackEvent slackEvent = dataRepoService.getSlackConversationValues(channelRequestId);
                channelId = slackEvent.getChannel();
            } else if (channelType.equals(ChannelType.DISCORD)) {
                DiscordEvent discordEvent = dataRepoService.getDiscordConversationValuesForChannelRequestId(channelRequestId);
                channelId = discordEvent.getChannelId();
            } else {
                channelId = NO_CHANNEL_ID;
                log.error("No channel id available!");
                //return new ResponseEntity<>(new Error("No channel Id available!", "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
            }
            log.info("Channel Id: " + channelId);

            if (message.getAuthor() != null) {
                log.info("Also save author of thread message: " + message.getAuthor());
            }
            contextService.saveThreadMessage(domain, channelId, channelRequestId, messageId, message.getMessage());

            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "BAD_REQUEST"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Add channel information to asked question
     * @param question Question received from a particular channel
     * @return question containing additional channel information
     */
    private AskedQuestion completeAskedQuestionWithChannelInfo(Context domain, AskedQuestion question) {
        if (question.getChannelType() == ChannelType.SLACK) {
            log.info("Get Slack channel info ...");
            SlackEvent event = dataRepoService.getSlackConversationValues(question.getChannelRequestId());

            question.setSlackChannelId(event.getChannel());
            question.setSlackMsgTimestamp(event.getTs());
            log.info("Slack thread / message timestamps: " + event.getThread_ts() + " / " + event.getTs());

            SlackDomainMapping mapping = dataRepoService.getSlackDomainMappingForDomain(question.getDomainId(), event.getChannel());

            question.setSlackTeamId(mapping.getTeamId());

            String subdomain = domain.getSlackConfiguration().getSubdomain();
            question.setSlackSubdomain(subdomain);
        } else if (question.getChannelType() == ChannelType.DISCORD) {
            log.info("Get Discord channel info ...");
            DiscordEvent event = dataRepoService.getDiscordConversationValuesForChannelRequestId(question.getChannelRequestId());

            DiscordDomainMapping[] mappings = dataRepoService.getDiscordDomainMappingForDomain(question.getDomainId());

            String discordChannelId = event.getChannelId();

            boolean mappingExists = false;
            for (DiscordDomainMapping mapping: mappings) {
                if (mapping.getChannelId().equals(discordChannelId)) {
                    question.setDiscordGuildId(mapping.getGuildId());
                    question.setDiscordChannelId(mapping.getChannelId());
                    mappingExists = true;
                    break;
                }
            }

            if (!mappingExists) {
                log.error("No Discord Katie domain mapping for Discord channel '" + discordChannelId + "'!");
            }
        } else if (question.getChannelType() == ChannelType.MS_TEAMS) {
            log.warn("No such channel type '" + question.getChannelType() + "' supported yet!");
            question.setMsTeamsId("TODO");
            question.setMsTeamsChannelId("TODO");
        } else {
            log.info("No such channel type '" + question.getChannelType() + "' supported yet!");
        }

        return question;
    }

    /**
     * REST interface to get resubmitted questions
     */
    @RequestMapping(value = "/resubmitted", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get all resubmitted questions")
    public ResponseEntity<?> getResubmittedQuestions(
        @ApiParam(name = "status", value = "Status of resubmitted questions (e.g. 'answer-pending', 'answered-and-ready-to-send', 'answer-sent', 'answer-rated', 'trained-with-answer')",required = false)
        @RequestParam(value = "status", required = false) String status,
        @ApiParam(name = "contextId", value = "Context Id of resubmitted questions (e.g. 'wyona' or 'ROOT')",required = false)
        @RequestParam(value = "contextId", required = false) String contextId,
        // https://www.moesif.com/blog/technical/api-design/REST-API-Design-Filtering-Sorting-and-Pagination/#pagination
        @ApiParam(name = "limit", value = "Pagination: Limit the number of returned resubmitted questions",required = true)
        @RequestParam(value = "limit", required = true) int limit,
        @ApiParam(name = "offset", value = "Pagination: Offset indicates the start of the returned resubmitted questions",required = true)
        @RequestParam(value = "offset", required = true) int offset,
        HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        log.info("Get resubmitted questions for status '" + status + "' and domain Id '" + contextId + "' ...");

        try {
            if (contextService.isMemberOrAdmin(contextId)) {
                // TODO: Check whether user is authorized to each question/answer?!
                ResubmittedQuestion[] questions = dataRepoService.getResubmittedQuestions(status, contextId, limit, offset);

                Context domain = contextService.getContext(contextId);
                for (ResubmittedQuestion question: questions) {
                    if (contextService.existsQnA(question.getUuid(), domain)) {
                        Answer qna = contextService.getQnA(null, question.getUuid(), domain);
                        question.setTrained(qna.isTrained());
                    } else  {
                        question.setTrained(false);
                    }
                }

                return new ResponseEntity<>(questions, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "SQL_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get all questions asked so far
     * TODO: Filtering, Sorting, Pagination: https://www.moesif.com/blog/technical/api-design/REST-API-Design-Filtering-Sorting-and-Pagination/
     */
    @RequestMapping(value = "/asked", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get all asked questions")
    public ResponseEntity<?> getAskedQuestions(
        @ApiParam(name = "contextId", value = "Domain Id of asked questions (e.g. 'wyona' or 'ROOT')",required = false)
        @RequestParam(value = "contextId", required = false) String contextId,
        @ApiParam(name = "limit", value = "Pagination: Limit the number of returned questions",required = true, defaultValue = "10")
        @RequestParam(value = "limit", required = true) int limit,
        @ApiParam(name = "offset", value = "Pagination: Offset indicates the start of the returned questions",required = true, defaultValue = "0")
        @RequestParam(value = "offset", required = true) int offset,
        @ApiParam(name = "unanswered", value = "When set to true, then only return unanswered questions",required = false)
        @RequestParam(value = "unanswered", required = false) Boolean unanswered,
        HttpServletRequest request, HttpServletResponse response) {
        log.info("Get all asked questions");

        rememberMeService.tryAutoLogin(request, response);

        try {
            if (contextService.isMemberOrAdmin(contextId)) {

                // TEST: Uncomment lines below to test frotend spinner
                /*
                try {
                    for (int i = 0; i < 1; i++) {
                        log.info("Sleep for 2 seconds ...");
                        Thread.sleep(2000);
                    }
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
                 */

                AskedQuestion[] questions = dataRepoService.getQuestions(contextId, limit, offset, unanswered);
                if (questions != null & questions.length > 0) {
                    return new ResponseEntity<>(questions, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new com.wyona.katie.models.Error("No questions asked yet.", "NO_QUESTIONS"), HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "SQL_ERROR"), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * REST interface to get index of all trained questions/answers of a particular domain
     */
    @RequestMapping(value = "/trained/index", method = RequestMethod.GET, produces = "text/plain")
    @ApiOperation(value="Get index (UUID and question) of all trained QnAs of a particular domain")
    public ResponseEntity<?> getTrainedQuestionsIndex(
        @ApiParam(name = "domainId", value = "Domain Id of resubmitted questions (e.g. 'wyona' or 'ROOT')",required = true)
        @RequestParam(value = "domainId", required = true) String domainId,
        HttpServletRequest request, HttpServletResponse response) {

        rememberMeService.tryAutoLogin(request, response);

        log.info("Get index of all trained questions/answers");

        try {
            if (contextService.isMemberOrAdmin(domainId)) {
                int limit = -1; // TODO: Implement as request parameter
                int offset = -1; // TODO: Implement as request parameter
                Answer[] answers = contextService.getTrainedQnAs(contextService.getContext(domainId), limit, offset);
                StringBuilder entries = new StringBuilder();
                for (int i = 0; i < answers.length; i++) {
                    entries.append(Answer.AK_UUID_COLON + answers[i].getUuid() + "::" + answers[i].getOriginalquestion() + "\n");
                }
                if (answers != null && answers.length > 0) {
                    // TODO: Implement pagination, see for example https://www.moesif.com/blog/technical/api-design/REST-API-Design-Filtering-Sorting-and-Pagination/
                    return new ResponseEntity<>(entries.toString(), HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new com.wyona.katie.models.Error("TODO", "NO_TRAINED_QUESTIONS_ANSWERS"), HttpStatus.BAD_REQUEST);
                }
            } else {
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "DATA_REPOSITORY_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to get all trained questions/answers of a particular domain
     */
    @RequestMapping(value = "/trained", method = RequestMethod.GET, produces = "application/json")
    @ApiOperation(value="Get all trained QnAs of a particular domain")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataType = "string", paramType = "header") })
    public ResponseEntity<?> getTrainedQuestions(
            @ApiParam(name = "domainId", value = "Domain Id of knowledge base of trained questions (e.g. 'wyona' or 'ROOT')",required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            // https://www.moesif.com/blog/technical/api-design/REST-API-Design-Filtering-Sorting-and-Pagination/#pagination@ApiParam(name = "limit", value = "Pagination: Limit the number of returned trained QnAs", required = false)
            @ApiParam(name = "limit", value = "Pagination: Limit the number of returned trained QnAs", required = false)
            @RequestParam(value = "limit", required = false) Integer limit,
            @ApiParam(name = "offset", value = "Pagination: Offset indicates the start of the returned trained QnAs", required = false)
            @RequestParam(value = "offset", required = false) Integer offset,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        rememberMeService.tryAutoLogin(request, response);

        log.info("Get all trained questions/answers for domain '" + domainId + "' ...");

        // TEST: Uncomment lines below to test frotend spinner
        /*
        try {
            for (int i = 0; i < 1; i++) {
                log.info("Sleep for 2 seconds ...");
                Thread.sleep(2000);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
         */

        try {
            if (contextService.isMemberOrAdmin(domainId)) {
                int _limit = -1;
                if (limit != null) {
                    _limit = limit.intValue();
                }
                int _offset = -1;
                if (offset !=  null) {
                    _offset = offset.intValue();
                }
                log.info("Limit: " + _limit);
                log.info("Offset: " + _offset);
                Answer[] answers = contextService.getTrainedQnAs(contextService.getContext(domainId), _limit, _offset);
                if (answers != null & answers.length > 0) {
                    // TODO: Return pagination, such that client knows whether there are more entries
                    return new ResponseEntity<>(answers, HttpStatus.OK);
                } else {
                    return new ResponseEntity<>(new Answer[0], HttpStatus.OK);
                }
            } else {
                log.warn("Access denied!");
                return new ResponseEntity<>(new com.wyona.katie.models.Error("Access denied", "ACCESS_DENIED"), HttpStatus.FORBIDDEN);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "DATA_REPOSITORY_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to delete all trained questions/answers of a particular domain
     */
    @RequestMapping(value = "/trained", method = RequestMethod.DELETE, produces = "application/json")
    @ApiOperation(value="Delete all trained QnAs of a particular domain")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataType = "string", paramType = "header") })
    public ResponseEntity<?> deleteTrainedQnAs(
            @ApiParam(name = "domainId", value = "Domain Id of knowledge base of trained questions (e.g. 'wyona' or 'ROOT')",required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        rememberMeService.tryAutoLogin(request, response);

        log.info("Delete all trained questions/answers of domain '" + domainId + "' ...");

        // TEST: Uncomment lines below to test frotend spinner
        /*
        try {
            for (int i = 0; i < 1; i++) {
                log.info("Sleep for 2 seconds ...");
                Thread.sleep(2000);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
         */

        try {
            contextService.deleteAllTrainedQnAs(domainId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (AccessDeniedException e) {
            log.warn(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "UNAUTHORIZED"), HttpStatus.UNAUTHORIZED);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * REST interface to delete all questions/answers (trained and not trained) of a particular domain
     */
    @RequestMapping(value = "/all", method = RequestMethod.DELETE, produces = "application/json")
    @ApiOperation(value="Delete all QnAs (trained and not trained) of a particular domain")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "Authorization", value = "Bearer JWT",
                    required = false, dataType = "string", paramType = "header") })
    public ResponseEntity<?> deleteAllQnAs(
            @ApiParam(name = "domainId", value = "Domain Id of knowledge base of trained questions (e.g. 'wyona' or 'ROOT')",required = true)
            @RequestParam(value = "domainId", required = true) String domainId,
            HttpServletRequest request, HttpServletResponse response) {

        try {
            authService.tryJWTLogin(request);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        rememberMeService.tryAutoLogin(request, response);

        log.info("Delete all questions/answers (trained and not trained) of domain '" + domainId + "' ...");

        // TEST: Uncomment lines below to test frotend spinner
        /*
        try {
            for (int i = 0; i < 1; i++) {
                log.info("Sleep for 2 seconds ...");
                Thread.sleep(2000);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
         */

        try {
            contextService.deleteAllQnAs(domainId);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (AccessDeniedException e) {
            log.warn(e.getMessage(), e);
            return new ResponseEntity<>(new com.wyona.katie.models.Error(e.getMessage(), "UNAUTHORIZED"), HttpStatus.UNAUTHORIZED);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(new Error(e.getMessage(), "INTERNAL_SERVER_ERROR"), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
