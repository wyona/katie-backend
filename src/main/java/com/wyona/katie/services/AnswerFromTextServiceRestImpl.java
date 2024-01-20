package com.wyona.katie.services;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * REST implementation to return answer from text
 */
@Slf4j
@Component
public class AnswerFromTextServiceRestImpl implements AnswerFromTextService {

    @Autowired
    private MailerService mailerService;

    @Value("${aft.rest.impl.host}")
    private String host;

    private final int connectTimeout = 3000; // INFO: in milliseconds
    private final int readTimeout = 240000; // INFO: in milliseconds
    //private final int readTimeout = 5000; // INFO: in milliseconds

    /**
     * @see AnswerFromTextService#getAnswerFromText(String, String)
     */
    public String getAnswerFromText(String question, String text) {
        log.info("Get answer from text using REST implementation '" + host + "' ...");

        log.info("Text (not sanitized): " + text);
        String sanitizedText = Utils.escapeForJSON(text);

        log.info("Question (not sanitized): " + question);
        String sanitizedQuestion = Utils.escapeForJSON(question);

        StringBuilder body = new StringBuilder("{\"question\": "+ "\""+ sanitizedQuestion + "\",\"context\":\"" + sanitizedText + "\"}");

        log.info("Request body: " + body);

        RestTemplate restTemplate = new RestTemplate(getClientHttpRequestFactory());

        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(body.toString(), headers);

        String domainId = "ROOT"; // TODO
        String requestUrl = host + "/api/v1/ask/" + domainId + "/answers";
        log.info("Try to get answer from text: " + requestUrl);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            /*
            {
  "answers": [
    "Michael was born on February 16, 1969 and is now 53 years old. He is the son of a woman and a man who has been battling Alzheimer's disease for decades.",
    "Michael was born on February 16, 1969 and is now 53 years old. He is the father of three children and the oldest son of a man in the world."
  ],
  "context": "Michael was born February 16, 1969 and is now 53 years old",
  "question": "How old is Michael",
  "version": "1.0"
}
             */

            List<String> answers = new ArrayList<String>();
            for (JsonNode answer: bodyNode.get("answers")) {
                answers.add(answer.asText());
                log.info("Answer: " + answer.asText());
            }

            return answers.get(0);
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            } else if (e.getRawStatusCode() == 400) {
                log.error(e.getMessage(), e);
            } else {
                log.error(e.getMessage(), e);
            }
            mailerService.notifyAdministrator("ERROR: Get answer from text failed", e.getMessage() + "\n\nJSON:\n\n" + body, null, false);
            //return Optional.empty();
            return "ERROR";
        } catch(ResourceAccessException e) {
            // INFO: Both timeout and not reachable exceptions are handled as ResourceAccessException by restTemplate
            log.error(e.getMessage(), e);
            if (e.getMessage().contains("Read timed out")) {
                log.info("Configured connect timeout in milliseconds: " + connectTimeout);
                log.info("Configured read timeout in milliseconds: " + readTimeout);
                mailerService.notifyAdministrator("ERROR: Get answer from text failed", e.getMessage() + "\n\nJSON:\n\n" + body + "\n\nConfigured read timeout in milliseconds:\n\n" + readTimeout, null, false);
            } else {
                mailerService.notifyAdministrator("ERROR: Get answer from text failed", e.getMessage() + "\n\nJSON:\n\n" + body, null, false);
            }
            // TODO: The method isAlive(String) should also detect when the Question Classifier is down
            //return Optional.empty();
            return "ERROR";
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            mailerService.notifyAdministrator("ERROR: Analyzing message by QuestionClassifierRestImpl failed ", e.getMessage() + "\n\nJSON:\n\n" + body, null, false);
            //return Optional.empty();
            return "ERROR";
        }
    }

    /**
     * To configure a request timeout when querying a web service
     * @return clientHttpRequestFactory with timeout of 3000ms
     */
    private ClientHttpRequestFactory getClientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory
                = new HttpComponentsClientHttpRequestFactory();

        clientHttpRequestFactory.setConnectTimeout(connectTimeout);
        clientHttpRequestFactory.setReadTimeout(readTimeout);

        return clientHttpRequestFactory;
    }

    /**
     * Check whether Answer from Text service is alive
     * @param endpoint Health endpoint, e.g. "/api/v1/health"
     * @return true when alive and false otherwise
     */
    public boolean isAlive(String endpoint) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            String requestUrl = host + endpoint;
            log.info("Check whether Answer from Text service is alive: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);

            if (bodyNode.get("status").asText().equals("UP")) {
                return true;
            }
        } catch(Exception e) {
            log.error(e.getMessage());
        }

        log.warn("Answer from Text service '" + host + "' seems to be down!");
        mailerService.notifyAdministrator("WARNING: The Answer from Text Service at '" + host + "' seems to be DOWN", null, null, false);
        return false;
    }

    /**
     * Get http headers
     * @return HttpHeaders
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        return headers;
    }
}
