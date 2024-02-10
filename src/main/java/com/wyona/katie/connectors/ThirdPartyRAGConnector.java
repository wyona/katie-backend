package com.wyona.katie.connectors;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.models.*;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Connector for third-party RAG implementation, e.g. https://github.com/access2justice/law-bot/tree/master/backend
 */
@Slf4j
@Component
public class ThirdPartyRAGConnector implements Connector {

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Get answers using third-party RAG connector '" + ksMeta.getName() + "' ...");
        List<Hit> hits = new ArrayList<Hit>();

        // INFO: Use double curly braces for variables, e.g. {{QUESTION}}
        String body = ksMeta.getGetThirdPartyRAGBody();
        body = body.replaceAll("\\{\\{QUESTION\\}\\}", question.getSentence());
        log.info("Request body: " + body.toString());

        String requestUrl = ksMeta.getThirdPartyRAGUrl();
        log.info("Post query to " + requestUrl);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders(ksMeta);
        HttpEntity<String> request = new HttpEntity<String>(body, headers);

        try {
            /*
            // INFO: Plain text response
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, String.class);
            log.info("Response: " + response);
            String _answer = response.getBody();

             */

            // INFO: JSON response
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);
            JsonNode dataNode = bodyNode.get("data");
            String _answer = dataNode.get("content").asText();

            double score = 0.0;
            String url = null;
            Answer answer = new Answer(question.getSentence(), _answer, null, url, null, null, null, null, null, null, null, null, null, null, true, null, false, null);
            Hit hit = new Hit(answer, score);
            hits.add(hit);
        } catch(HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            if (e.getRawStatusCode() == 403) {
                Hit hit = new Hit(getAnswerContainingErrorMsg(question.getSentence(), "Katie is not authorized to access third-party RAG service '" + ksMeta.getName() + "' configured within Katie domain '" + ksMeta.getDomainId() + "'!"), 0.0);
                hits.add(hit);
            }
            // INFO: Do not return null
        } catch(HttpServerErrorException e) {
            log.error(e.getMessage(), e);
            if (e.getRawStatusCode() == 500) {
                Hit hit = new Hit(getAnswerContainingErrorMsg(question.getSentence(), "Katie received an Internal Server Error from third-party RAG service '" + ksMeta.getName() + "' configured within Katie domain '" + ksMeta.getDomainId() + "'!"), 0.0);
                hits.add(hit);
            }
            // INFO: Do not return null
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            // INFO: Do not return null
            Hit hit = new Hit(getAnswerContainingErrorMsg(question.getSentence(), e.getMessage()), 0.0);
            hits.add(hit);
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
    private Answer getAnswerContainingErrorMsg(String question, String errorMsg) {
        String url = null;
        Answer answer = new Answer(question, errorMsg, null, url, null, null, null, null, null, null, null, null, null, null, true, null, false, null);
        return answer;
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders(KnowledgeSourceMeta ksMeta) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json;odata=verbose");
        headers.set("Content-Type", "application/json");
        //headers.set("Content-Type", "application/json; charset=UTF-8");

        return headers;
    }
}
