package com.wyona.katie.connectors;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.*;
import org.springframework.beans.factory.annotation.Value;
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
public class WeaviateWikipediaSearchConnector implements Connector {

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Get answers from Weaviate Wikipedia Search connector ...");
        List<Hit> hits = new ArrayList<Hit>();

        String properties = "{ text title url views lang _additional {distance} }";
        String languages = "en";
        String where = "where:{operator:Equal valueString:\"" + languages + "\" path:[\"lang\"]}";
        String nearText = "nearText:{concepts:[\"" + question.getSentence() + "\"]}";
        String _limit = "limit:" + limit;

        String query = "{ Get { Articles( " + where + " " + nearText + " " + _limit + " ) " + properties + " } }";

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode bodyRequest = mapper.createObjectNode();
        bodyRequest.put("query", query);

        log.info("Request body: " + bodyRequest.toString());

        String baseUrl = ksMeta.getWeaviateWikipediaSearchUrl();
        String requestUrl = baseUrl + "/v1/graphql";

        log.info("Post query to " + requestUrl);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders(ksMeta);
        headers.set("Content-Type", "application/json; charset=UTF-8");
        HttpEntity<String> request = new HttpEntity<String>(bodyRequest.toString(), headers);

        try {
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

        String token = ksMeta.getWeaviateWikipediaSearchKey();
        try {
            headers.setBearerAuth(token);
            headers.set("X-Cohere-Api-Key", ksMeta.getWeaviateWikipediaSearchCohereKey()); // https://github.com/cohere-ai/notebooks/blob/main/notebooks/Wikipedia_search_demo_cohere_weaviate.ipynb
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return headers;
    }
}
