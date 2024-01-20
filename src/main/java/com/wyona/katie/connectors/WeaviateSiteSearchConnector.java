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
 * The indexing is done with https://github.com/weaviate/weaviate-io-site-search
 */
@Slf4j
@Component
public class WeaviateSiteSearchConnector implements Connector {

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Get answers from Weaviate Site Search connector ...");
        List<Hit> hits = new ArrayList<Hit>();

        String query = "{ Get { PageChunkOpenAI( hybrid: { query: \"" + question.getSentence() + "\" alpha: 0.4 } limit: 8 ) { title content url anchor order pageTitle typeOfItem _additional { score } } } }";

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyRequest = mapper.createObjectNode();
        bodyRequest.put("query", query);

        log.info("Request body: " + bodyRequest.toString());

        String requestUrl = "https://search.weaviate.io/v1/graphql"; // TODO: Get from knowledge source meta

        log.info("Post query to " + requestUrl);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(bodyRequest.toString(), headers);

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON response: " + bodyNode);

            JsonNode dataNode = bodyNode.get("data");
            JsonNode getNode = dataNode.get("Get");
            JsonNode pageChunkOpenAINode = getNode.get("PageChunkOpenAI");
            if (pageChunkOpenAINode.isArray()) {
                for (int i = 0; i < pageChunkOpenAINode.size(); i++) {
                    JsonNode resultNode = pageChunkOpenAINode.get(i);
                    String mockAnswer = resultNode.get("content").asText();
                    String url = "https://weaviate.io";
                    url = url + resultNode.get("url").asText();
                    JsonNode additionalNode = resultNode.get("_additional");
                    double score = additionalNode.get("score").asDouble();
                    Answer answer = new Answer(question.getSentence(), mockAnswer, null, url, null, null, null, null, null, null, null, null, null, null, true, null, false, null);
                    Hit hit = new Hit(answer, score);
                    hits.add(hit);
                }
            }
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
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json;odata=verbose");

        if (false) { // TODO: Check for token
            try {
                headers.setBearerAuth(getToken());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }


        return headers;
    }

    /**
     *
     */
    private String getToken() {
        return "TODO";
    }
}
