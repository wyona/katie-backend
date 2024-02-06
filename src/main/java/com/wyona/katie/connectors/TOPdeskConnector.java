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
 * TOPdesk Connector
 */
@Slf4j
@Component
public class TOPdeskConnector implements Connector {

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Get answers from TOPdesk connector ...");
        List<Hit> hits = new ArrayList<Hit>();

        // INFO: https://developers.topdesk.com/explorer/?page=general#/search/get_search
        String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/search?index=incidents&query=" + question.getSentence();
        log.info("Request URL: " + requestUrl);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders(ksMeta);
        HttpEntity<String> request = new HttpEntity<String>(headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.GET, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            /*
            JsonNode dataNode = bodyNode.get("data");
            JsonNode getNode = dataNode.get("Get");
            JsonNode articlesNode = getNode.get("Articles");
            if (articlesNode.isArray()) {
                for (int i = 0; i < articlesNode.size(); i++) {
                    JsonNode resultNode = articlesNode.get(i);
                    String _answer = resultNode.get("title").asText() + " --- " + resultNode.get("text").asText();
                    String url = resultNode.get("url").asText();
                    JsonNode additionalNode = resultNode.get("_additional");
                    double score = additionalNode.get("distance").asDouble();
                    Answer answer = new Answer(question.getSentence(), _answer, null, url, null, null, null, null, null, null, null, null, null, null, true, null, false, null);
                    Hit hit = new Hit(answer, score);
                    hits.add(hit);
                }
            }
            */
        } catch(HttpClientErrorException e) {
            log.error(e.getMessage(), e);
            if (e.getRawStatusCode() == 403) {
                Hit hit = new Hit(getAnswerContainingErrorMsg(question.getSentence(), "Katie is not authorized to access TOPdesk service '" + ksMeta.getName() + "' configured within Katie domain '" + ksMeta.getDomainId() + "'!"), 0.0);
                hits.add(hit);
            }
            // INFO: Do not return null
        } catch(HttpServerErrorException e) {
            log.error(e.getMessage(), e);
            if (e.getRawStatusCode() == 500) {
                Hit hit = new Hit(getAnswerContainingErrorMsg(question.getSentence(), "Katie received an Internal Server Error from TOPdesk service '" + ksMeta.getName() + "' configured within Katie domain '" + ksMeta.getDomainId() + "'!"), 0.0);
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
        //String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents";
        //String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents/number/I-240131-0203";
        //String requestUrl = ksMeta.getTopDeskBaseUrl() + "/tas/api/incidents/number/I-240205-0956/progresstrail";
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
        headers.set("Accept", "application/json");

        // INFO: https://developers.topdesk.com/tutorial.html see "Create an application password"
        //headers.set("Authorization", "Bearer TOKEN");

        headers.setBasicAuth(ksMeta.getTopDeskUsername(), ksMeta.getTopDeskAPIPassword());

        return headers;
    }
}
