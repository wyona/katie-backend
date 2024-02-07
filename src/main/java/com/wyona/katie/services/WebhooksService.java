package com.wyona.katie.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.ResubmittedQuestion;
import com.wyona.katie.models.Webhook;
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

@Slf4j
@Component
public class WebhooksService {

    @Autowired
    private XMLService xmlService;

    @Autowired
    private ContextService domainService;

    /**
     * Deliver answer to client referenced by webhook
     * @param echoData Optional data to be sent to webhook, e.g. token containing custom information
     */
    @Async
    public void deliver(Webhook webhook, ResubmittedQuestion qna, String echoData) {

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
                Context domain = domainService.getContext(qna.getContextId());
                body.put("content", "The question '" + qna.getQuestion() + "' has been answered by a human expert: " + domainService.getAnswerLink(qna, domain));
            } catch(Exception e) {
                log.error(e.getMessage(), e);
                body.put("content", e.getMessage());
            }
        } else {
            log.info("Create payload for generic webhook ...");
            body.put("msgtype", "answer-to-question");
            body.put("question", qna.getQuestion());
            body.put("answer",qna.getAnswer());
            body.put("contenttype","text/html"); // TODO: Get content type from QnA
            body.put("formatted_answer", qna.getAnswer());
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
            status = response.getStatusCode();
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);
        } catch(RestClientException e) {
            log.error(e.getMessage(), e);
            status = getStatus(e.getMessage());
        }

        log.info("Status code: " + status);
        try {
            xmlService.logWebhookDelivery(qna.getContextId(), webhook, status.value(), new java.util.Date().getTime());
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
