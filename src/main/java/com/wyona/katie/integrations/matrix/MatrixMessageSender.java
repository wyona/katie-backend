package com.wyona.katie.integrations.matrix;

import com.wyona.katie.models.*;
import com.wyona.katie.models.matrix.MatrixConversationValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.services.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class MatrixMessageSender {

    @Value("${matrix.homeserver}")
    private String host;

    @Value("${matrix.access.token}")
    private String accessToken;

    @Autowired
    private DataRepositoryService dataRepoService;

    @Autowired
    private AuthenticationService authService;

    @Autowired
    private QuestionAnalyzerService questionAnalyzerService;

    @Autowired
    private ContextService contextService;

    @Autowired
    private DataRepositoryService dataRepositoryService;

    @Autowired
    private AnalyticsService analyticsService;

    @Autowired
    private QuestionAnsweringService qaService;

    private static final String ROOM_MESSAGE = "m.room.message";

    private List<String> processedEventsList = java.util.Collections.synchronizedList(new ArrayList<String>());

    /**
     *
     */
    public Question[] syncWithHomeserver() throws Exception {
        if (!isAdmin()) {
            throw new java.nio.file.AccessDeniedException("User has not role " + Role.ADMIN + "!");
        }

        List<Question> questions = new ArrayList<Question>();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(headers);

        String requestUrl = host + "/_matrix/client/r0/sync?access_token=" + accessToken;
        log.info("Sync with Matrix homeserver: " + requestUrl);
        ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
        JsonNode bodyNode = response.getBody();

        log.info("Response JSON: " + bodyNode);

        JsonNode roomsNode = bodyNode.get("rooms");
        JsonNode joinNode = roomsNode.get("join");
        //log.info("Join node: " + joinNode);
        java.util.Iterator<java.util.Map.Entry<String, JsonNode>> joinedRooms = joinNode.fields();
        while (joinedRooms.hasNext()) {
            java.util.Map.Entry<String, JsonNode> room = joinedRooms.next();
            String roomId = room.getKey();
            Context domain = getDomain(roomId);
            if (domain == null) {
                throw new Exception("No Katie domain associated with Matrix room '" + roomId + "'!");
            }
            log.info("Check messages of Matrix room '" + roomId + "', which is associated with Katie domain '" + domain.getId() + "' ...");

            JsonNode roomNode = room.getValue();
            JsonNode timeline = roomNode.get("timeline");
            log.info("Timeline node of room '" + roomId + "': " + timeline);

            JsonNode eventsNode = timeline.get("events");
            if (eventsNode.isArray()) {
                for (JsonNode event: eventsNode) {
                    String eventType = event.get("type").asText();

                    String eventId = event.get("event_id").asText();
                    if (eventMarkedAsBeingProcessed(eventId)) {
                        log.info("Matrix event '" + eventId + "' already processed, therefore skip it.");
                        continue;
                    }

                    //java.util.Date eventOriginServerTimestamp = new java.util.Date(event.get("origin_server_ts").asText());
                    String eventOriginServerTimestamp = event.get("origin_server_ts").asText();
                    String eventSender = event.get("sender").asText();

                    JsonNode contentNode = event.get("content");
                    if (eventType.equals(ROOM_MESSAGE) && contentNode.has("body")) {
                        String message = contentNode.get("body").asText();
                        //String messageFormat = contentNode.get("format").asText(); // For example "org.matrix.custom.html"

                        // INFO: A message can be edited, whereas the edited message will show up as a separate event, but linking to the original event using the Id of the original event
                        if (contentNode.has("m.new_content")) {
                            JsonNode relatesTo = contentNode.get("m.relates_to");
                            String relatesToId = relatesTo.get("event_id").asText();
                            String relatesType = relatesTo.get("rel_type").asText();
                            if (relatesType.equals("m.replace")) {
                                log.info("TODO: Update question '" + relatesToId + "' with updated content '" + contentNode.get("m.new_content").get("body").asText() + "'");
                                continue;
                            } else {
                                log.warn("No such relates type '" + relatesType + "' implemented!");
                            }
                        }

                        java.util.Date dateSubmitted = new java.util.Date(Long.parseLong(eventOriginServerTimestamp));
                        log.info("Matrix message '" + message + "' sent by '" + eventSender + "' at '" + dateSubmitted + "'.");
                        analyticsService.logMessageReceived(domain.getId(), ChannelType.MATRIX, roomId);
                        AnalyzedMessage analyzedMessage = questionAnalyzerService.analyze(message, domain);
                        if (analyzedMessage.getContainsQuestions()) {
                            log.info("Question detected in Matrix room '" + roomId + "'.");
                            if (analyzedMessage.getQuestionsAndContexts().size() > 1) {
                                log.info("TODO: Handle multiple questions ...");
                            }
                            Question q = new Question();
                            // TODO: Get question(s) from analyzed message
                            q.setQuestion(message);
                            questions.add(q);

                            String channelRequestId = java.util.UUID.randomUUID().toString();
                            MatrixConversationValues conversationValues = new MatrixConversationValues(null, roomId, eventId);
                            conversationValues.setUuid(channelRequestId);
                            conversationValues.setDomainId(domain.getId());
                            dataRepositoryService.addMatrixConversationValues(conversationValues);

                            // INFO: Check whether Katie knows an answer
                            String remoteAddress = "TODO:Matrix";
                            // TODO
                            List<String> classifications = new ArrayList<String>();
                            boolean checkAuthorization = false; // TODO
                            String messageId = null; // TODO
                            boolean includeFeedbackLinks = false;
                            ContentType answerContentType = null;
                            List<ResponseAnswer> answers = qaService.getAnswers(q.getQuestion(), classifications, messageId, domain, dateSubmitted, remoteAddress, ChannelType.MATRIX, channelRequestId, -1, -1, checkAuthorization, answerContentType, includeFeedbackLinks);

                            if (answers != null && answers.size() > 0) {
                                // INFO: When moderation is enabled, then Katie does not send any messages back to the channel
                                if (domain.getAnswersMustBeApprovedByModerator()) {
                                    log.info("Moderation approval enabled for domain '" + domain.getName() + "' (Id: " + domain.getId() + "), therefore do not return an answer.");
                                    continue;
                                }

                                log.warn("Return answer to Matrix room '" + roomId + "' ...");
                                sendRoomMessage(q.getQuestion(), answers.get(0).getAnswer(), channelRequestId);
                            } else {
                                // TODO: Get language from matrix or question
                                Language language = Language.en;
                                contextService.answerQuestionByNaturalIntelligence(q.getQuestion(), null, ChannelType.MATRIX, channelRequestId, null, null, language, null, remoteAddress, domain);
                            }
                        } else {
                            log.info("No question detected.");
                        }
                    } else {
                        log.info("Ignore event '" + eventId + "', because event content has either no body or type '" + eventType + "' is not equal '" + ROOM_MESSAGE + "'.");
                    }
                }
            }
        }

        return questions.toArray(new Question[0]);
    }

    /**
     * Check whether Matrix event is being or was already processed
     * @param id Unique event id, e.g. "$JJgq8x98e2pSALmQeP1sOKan-bpmwnZ_TGowGtxWGKo"
     * @return true when event was already processed and false otherwise
     */
    private boolean eventMarkedAsBeingProcessed(String id) {
        // TODO: Persist processed events list, such that it also works after restarting Katie
        if (processedEventsList.contains(id)) {
            return true;
        } else {
            processedEventsList.add(id);
            return false;
        }
    }

    /**
     * Get Katie domain associated with Matrix room
     * @param roomId Matrix room Id, e.g. "!ZuEJFYVSrFbPWVLQjv:matrix.org" or "!kwQFREBhkbipmDDUio:matrix.org"
     * @return Katie domain associated with Matrix room
     */
    private Context getDomain(String roomId) throws Exception {
        removeObsoleteMappings(roomId);

        String domainID = dataRepoService.getDomainIdForMatrix(roomId);
        if (domainID != null) {
            return contextService.getContext(domainID);
        } else {
            String name = "Matrix " + roomId;
            User signedInUser = authService.getUser(false, false);
            Context domain = contextService.createDomain(true,name, "Katie / Matrix", false, signedInUser);
            dataRepoService.addDomainIdMatrixMapping(domain.getId(), roomId);
            return domain;
        }
    }

    /**
     * Remove mappings where Katie domains do not exist anymore
     * @param roomId Matrix room Id
     */
    public void removeObsoleteMappings(String roomId) {
        log.info("Do some 'house cleaning', because some domains might have been deleted by Katie administrator");
        // TODO: Consider to check all mappings and not just the one linked with a particular Matrix room id
        String domainId = dataRepoService.getDomainIdForMatrix(roomId);
        if (domainId != null && !contextService.existsContext(domainId)) {
            try {
                log.info("Remove mapping associated with Matrix room '" + roomId + "' and domain '" + domainId + "' ...");
                dataRepoService.removeDomainIdMatrixMapping(domainId);
            } catch(Exception e) {
                log.error(e.getMessage(), e);

            }
        } else {
            if (domainId != null) {
                log.info("Katie domain '" + domainId + "' exists, no cleaning necessary :-)");
            } else {
                log.info("Matrix room '" + roomId + "' is not connected with any domain yet.");
            }
        }
    }

    /**
     * @return true when user is administrator and false otherwise
     */
    private boolean isAdmin() {
        log.info("Check whether user is signed in and has role ADMIN ...");
        User signedInUser = authService.getUser(false, false);
        if (signedInUser != null && (signedInUser.getRole() == Role.ADMIN)) {
            return true;
        } else {
            if (signedInUser != null) {
                log.warn("User '" + signedInUser.getId() + "' has not role \" + Role.ADMIN + \"!\"");
            } else {
                log.warn("User is not signed in!");
            }
            return false;
        }
    }

    /**
     * See https://matrix.org/docs/spec/client_server/latest#m-text
     */
    public void sendRoomMessage(String question, String answer, String channelRequestId) {
        MatrixConversationValues values = dataRepoService.getMatrixConversationValues(channelRequestId);
        String questionEventId = values.getEventId();

        String roomId = values.getRoomId();
        //String userId = values.getUserId();

        log.info("Send message to room '" + roomId + "' ...");
        String txnId = "" + new java.util.Date().getTime();
        String url = host + "/_matrix/client/r0/rooms/" + roomId + "/send/m.room.message/" + txnId + "?access_token=" + accessToken;

        // TODO: Use Jackson
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"msgtype\":\"m.text\"");

        sb.append(",");
        sb.append("\"body\":\"" + Utils.replaceNewLines(answer, " ") + "\"");
        //sb.append("\"body\":\" > <@michaelhanneswechner:matrix.org> " + prepare(question) + "TODO_TWO_LINE_BREAKS" + prepare(answer) + "\"");

        sb.append(",");
        sb.append("\"format\":\"org.matrix.custom.html\"");

        sb.append(",");
        sb.append("\"formatted_body\":\"" + Utils.replaceNewLines(answer, " ") + "\"");
        //sb.append("\"formatted_body\":\"<mx-reply><blockquote><a href=\\\"https://matrix.to/#/!ZuEJFYVSrFbPWVLQjv:matrix.org/$72wbRvFnXb6QYQYUdwjGnDUxVjBG_eOgHlzbT1fcVZ0?via=matrix.org\\\">In reply to</a> <a href=\\\"https://matrix.to/#/@michaelhanneswechner:matrix.org\\\">@michaelhanneswechner:matrix.org</a><br>" + prepare(question) + "</blockquote></mx-reply>" + prepare(answer) + "\"");

        sb.append(",");
        sb.append("\"m.relates_to\":{");
        sb.append("\"m.in_reply_to\":{");
        sb.append("\"event_id\":\"" + questionEventId + "\"");
        sb.append("}");
        sb.append("}");

        sb.append("}");

        log.info("Body: " + sb.toString().trim());

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders(); // getHttpHeaders();
            headers.set("Content-Type", "application/json; charset=UTF-8");
            HttpEntity<String> request = new HttpEntity<String>(sb.toString(), headers);

            log.info("Send request: " + url);
            ResponseEntity<JsonNode> response = restTemplate.exchange(url, HttpMethod.PUT, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }
}
