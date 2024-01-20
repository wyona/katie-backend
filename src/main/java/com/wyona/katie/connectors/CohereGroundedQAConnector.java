package com.wyona.katie.connectors;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

/**
 *
 */
@Slf4j
@Component
public class CohereGroundedQAConnector implements Connector {

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Get answers from CohereGroundedQA connector ...");

        String siteUrl = ksMeta.getGroundedQASiteUrl();
        String topic = ksMeta.getGroundedQATopic();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyRequest  = mapper.createObjectNode();
        if (siteUrl != null) {
            log.info("Site URL configured: " + siteUrl);
            bodyRequest.put("site-url", siteUrl);
        }
        String q = question.getSentence();
        if (topic != null) {
            log.info("Expand question with topic '" + topic + "' ...");
            q = topic + " " + q;
        }
        bodyRequest.put("question", q);

        log.info("Request body: " + bodyRequest.toString());

        List<Hit> hits = new ArrayList<Hit>();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        //HttpEntity<String> request = new HttpEntity<String>(headers);
        HttpEntity<String> request = new HttpEntity<String>(bodyRequest.toString(), headers);

        String requestUrl = ksMeta.getGroundedQAbaseUrl() + "/api/v1/ask";
        log.info("Try to get answer: " + requestUrl);
        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            String generatedAnswer = bodyNode.get("answer").asText();
            JsonNode source_urls = bodyNode.get("source_urls");
            String[] sourceURLs = removeDuplicates(source_urls);
            JsonNode source_texts = bodyNode.get("source_texts");

            StringBuilder answerStr = new StringBuilder();
            answerStr.append("<p>" + generatedAnswer + "</p>");

            answerStr.append("<p>Source(s):<br/>");
            answerStr.append("<ul>");
            for (String sourceURL: sourceURLs) {
                answerStr.append("<li><a href=\"" + sourceURL + "\">" + sourceURL + "</a></li>");
            }
            answerStr.append("</ul>");
            answerStr.append("</p>");

            answerStr.append("<p>Relevant texts:<br/>");
            answerStr.append("<ul>");
            for (JsonNode source_text: source_texts) {
                answerStr.append("<li>" + source_text.asText() + "</li>");
            }
            answerStr.append("</ul>");
            answerStr.append("</p>");

            Answer answer = new Answer(question.getSentence(), answerStr.toString(), null, null, null, null, null, null, null, null, null, null, null, null, true, null, false, null);
            Hit hit = new Hit(answer, -3.14);
            hits.add(hit);
            if (sourceURLs != null && sourceURLs.length > 0) {
                answer.setUrl(sourceURLs[0]);
            }
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            } else {
                log.info("Status Code: " + e.getRawStatusCode());
                log.error(e.getMessage(), e);
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        return hits.toArray(new Hit[0]);
    }

    /**
     * @see Connector#update(Context, KnowledgeSourceMeta, WebhookPayload, String)
     */
    public List<Answer> update(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload, String processId) {
        log.info("TODO: Implement");
        return null;
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");

        return headers;
    }

    /**
     * Remove URLs which appear more than once
     */
    private String[] removeDuplicates(JsonNode source_urls) {
        List<String> urls = new ArrayList<String>();

        for (JsonNode source_url: source_urls) {
            String url = source_url.asText();
            if (!alreadyExists(url, urls)) {
                urls.add(url);
            }
        }

        return urls.toArray(new String[0]);
    }

    /**
     * Check whether string already exists within list of strings
     * @return true when string already exists within list and false otherwise
     */
    private boolean alreadyExists(String text, List<String> texts) {
        for (String t : texts) {
            if (t.equals(text)) {
                log.info("String '" + text + "' already exists within list.");
                return true;
            }
        }
        return false;
    }
}
