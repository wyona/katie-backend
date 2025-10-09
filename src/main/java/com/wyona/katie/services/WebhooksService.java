package com.wyona.katie.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
public class WebhooksService {

    @Autowired
    private XMLService xmlService;
    @Autowired
    private ContextService domainService;
    @Autowired
    private DataRepositoryService dataRepoService;

    /**
     *
     */
    public void deliver(WebhookTriggerEvent event, String domainId, String uuid, String question, String answer, ContentType contentType, String email, String channelRequestId) {
        try {
            Webhook[] webhooks = domainService.getWebhooks(domainId);
            if (webhooks != null && webhooks.length > 0) {
                String echoData = null;
                if (channelRequestId != null) {
                    echoData = dataRepoService.getWebhookEchoData(channelRequestId);
                }
                for (Webhook webhook: webhooks) {
                    if (webhook.getEnabled() && isTriggeredByEvent(event, webhook)) {
                        // TODO: msgType should depend on WebhookTriggerEvent value
                        deliver(webhook, "answer-to-question", domainId, uuid, question, answer, contentType, email, echoData);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Check whether webhook is triggered only by a certain event
     * @return true when webhook is triggered by event and false otherwise
     */
    private boolean isTriggeredByEvent(WebhookTriggerEvent event, Webhook webhook) {
        List<WebhookTriggerEvent> events = webhook.getEvents();
        boolean isTriggeredByEvent = true; // INFO: By default trigger webhook by every event
        if (events.size() > 0) {
            isTriggeredByEvent = false;
            for (WebhookTriggerEvent e : events) {
                if (event.equals(e)) {
                    return true;
                }
            }
        }
        return isTriggeredByEvent;
    }

    /**
     * Deliver answer to client referenced by webhook
     * @param msgType For example "answer-to-question" or "better-answer"
     * @param echoData Optional data to be sent to webhook, e.g. token containing custom information
     */
    @Async
    public void deliver(Webhook webhook, String msgType, String domainId, String uuid, String question, String answer, ContentType contentType, String email, String echoData) {

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

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();

        if (webhook.getPayloadURL().startsWith("https://discord")) {
            log.info("Create payload for Discord webhook ...");
            // TODO: Should answer also be delivered, when it was asked on Slack originally?
            // qna.getChannelRequestId()
            try {
                Context domain = domainService.getContext(domainId);
                body.put("content", "The question '" + question + "' has been answered by a human expert: " + domainService.getAnswerLink(uuid, email, domain));
            } catch(Exception e) {
                log.error(e.getMessage(), e);
                body.put("content", e.getMessage());
            }
        } else {
            log.info("Create payload for generic webhook ...");
            body.put("msgtype", msgType);
            body.put("question", question);
            body.put("answer", answer);
            body.put("contenttype", contentType.toString());
            body.put("formatted_answer", answer);
            if (echoData != null) {
                body.put("echodata", echoData);
            }
        }

        log.info("Body: " + body.toString().trim());

        HttpStatus status = null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders(); // getHttpHeaders();
            headers.set("Content-Type", "application/json; charset=UTF-8");
            HttpEntity<String> request = new HttpEntity<String>(body.toString(), headers);

            log.info("Send request to webhook: " + webhook.getPayloadURL());
            ResponseEntity<JsonNode> response = restTemplate.exchange(webhook.getPayloadURL(), HttpMethod.POST, request, JsonNode.class);
            status = HttpStatus.valueOf(response.getStatusCode().toString());
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);
        } catch(RestClientException e) {
            log.error(e.getMessage(), e);
            status = getStatus(e.getMessage());
        }

        log.info("Status code: " + status);
        try {
            xmlService.logWebhookDelivery(domainId, webhook, status.value(), new java.util.Date().getTime());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * 404 Not Found: [no body]
     * 404 : [{"timestamp":"2021-07-23T13:14:05.045+00:00","status":404,"error":"Not Found","message":"","path":"/api/ask"}]
     */
    private HttpStatus getStatus(String message) {
        int status = 500;
        try {
            String[] parts = message.split(" ");
            status = Integer.parseInt(parts[0].trim());
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }
        return HttpStatus.valueOf(status);
    }
}
