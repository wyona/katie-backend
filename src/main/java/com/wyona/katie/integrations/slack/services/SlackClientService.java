package com.wyona.katie.integrations.slack.services;

import com.wyona.katie.services.MailerService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.slack.api.methods.response.auth.AuthTestResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import org.springframework.web.client.RestTemplate;

import com.slack.api.Slack;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;
import com.slack.api.methods.response.chat.ChatPostMessageResponse;

/**
 * Slack client as a service
 */
@Slf4j
@Component
public class SlackClientService {

    @Value("${slack.post.message.url}")
    private String postMessageURL;

    @Autowired
    private MailerService mailerService;

    /**
     * @param token Security token associated with Slack team Id / workspace (https://api.slack.com/authentication/token-types#bot)
     * @param channelId Slack channel Id, e.g. "C02AGB0BLQ4"
     */
    public void sendHiFromKatieMessage(String token, String channelId) {
        // INFO: https://slack.dev/java-slack-sdk/guides/web-api-basics
        Slack slackClient = Slack.getInstance();
        try {
            //ApiTestResponse response = slackClient.methods().apiTest(r -> r.foo("bar"));

            ChatPostMessageResponse response = slackClient.methods(token).chatPostMessage(ChatPostMessageRequest.builder()
                    .channel(channelId)
                    .text(":wave: Hi from Katie!")
                    .build());

            log.info("Response: " + response);

            if (response.isOk()) {
                log.info("Slack response message: " + response.getMessage());
            } else {
                log.error("Slack response error: " + response.getError()); // e.g., "invalid_auth", "channel_not_found"
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * @param token Security token associated with Slack team Id / workspace (https://api.slack.com/authentication/token-types#bot)
     */
    public void testAuth(String token) {
        // INFO: https://api.slack.com/methods/auth.test
        Slack slackClient = Slack.getInstance();
        try {
            AuthTestResponse response = slackClient.methods(token).authTest(req -> req.token(token));

            log.info("Response: " + response);

            if (response.isOk()) {
                log.info("Slack Bot Id: " + response.getBotId());
            } else {
                log.error("Slack response error: " + response.getError()); // e.g., "invalid_auth", "channel_not_found"
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Send request to Slack
     * @param body Body containing message as JSON
     * @param requestUrl Slack request URL, e.g. https://slack.com/api/chat.postMessage or https://hooks.slack.com/actions/T00000000/B00000000/XXXXXXXXXXXXXXXXXXXXXXXX
     * @param token Slack access/bearer token
     */
    public void send(String body, String requestUrl, String token) {
        if (requestUrl == null) {
            log.error("No request URL available, therefore do not send any request!");
            return;
        }

        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Accept-Charset", "UTF-8");
        headers.set("Authorization", "Bearer " + token);

        HttpEntity<String> request = new HttpEntity<String>(body, headers);

        try {
            log.info("Try to send response to Slack: " + requestUrl + ", " + body);
            ResponseEntity<JsonNode> response = restTemplate.postForEntity(requestUrl, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("Response JSON from Slack: " + bodyNode);

            boolean successfull = bodyNode.get("ok").asBoolean();

            if (!successfull) {
                String errMsg = bodyNode.get("error").asText();
                mailerService.notifyAdministrator("Send request to Slack failed: " + errMsg, body, null, false);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            try {
                mailerService.notifyAdministrator("Send request to Slack failed: " + e.getMessage(), body, null, false);
            } catch(Exception mailException) {
                log.error(mailException.getMessage(), mailException);
            }
        }
    }

    /**
     *
     * @param text For example answer to resubmitted question or answer which was approved by a moderator
     * @return JSON containing answer to question
     */
    public String getResponseJSON(String channelId, String msgTs, String text) {
        String USERNAME_KATIE = "Katie"; // TODO

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode responseBody = mapper.createObjectNode();
        responseBody.put("channel", channelId);
        if (msgTs != null) {
            log.info("Reply to parent message as thread response: " + msgTs);
            responseBody.put("thread_ts", msgTs);
        } else {
            log.info("No parent message timestamp available.");
        }
        responseBody.put("username", USERNAME_KATIE);
        responseBody.put("text", text);

        // TODO: Consider asking "Answer not helpful?"

        return responseBody.toString();
    }
}
