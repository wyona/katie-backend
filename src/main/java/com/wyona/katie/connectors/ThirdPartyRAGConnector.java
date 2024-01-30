package com.wyona.katie.connectors;

import java.util.ArrayList;
import java.util.List;

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
            // INFO: Plain text response
            ResponseEntity<String> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, String.class);
            log.info("Response: " + response);
            double score = 0.0;
            String url = null;
            String _answer = response.toString();
            Answer answer = new Answer(question.getSentence(), _answer, null, url, null, null, null, null, null, null, null, null, null, null, true, null, false, null);
            Hit hit = new Hit(answer, score);
            hits.add(hit);


            /*
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

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
            if (e.getRawStatusCode() == 403) {
                log.error("Not authorized to access '" + requestUrl + "'!");
            } else {
                log.error(e.getMessage(), e);
            }
            // INFO: Do not return null
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
    private HttpHeaders getHttpHeaders(KnowledgeSourceMeta ksMeta) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json;odata=verbose");
        headers.set("Content-Type", "application/json");
        //headers.set("Content-Type", "application/json; charset=UTF-8");

        return headers;
    }
}
