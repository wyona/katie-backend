package com.wyona.katie.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayList;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 */
@Slf4j
@Component
public class CohereReRank implements ReRankProvider {

    @Value("${cohere.host}")
    private String cohereHost;

    @Value("${cohere.key}")
    private String cohereKey;

    @Value("${cohere.version}")
    private String cohereVersion;

    @Value("${cohere.re_rank.model}")
    private String cohereReRankModel;

    /**
     * @see ReRankProvider#getReRankedAnswers(String, String[], int)
     */
    public Integer[] getReRankedAnswers(String question, String[] answers, int limit) {
        log.info("Re-rank answers using Cohere's re-rank service");

        List<Integer> reRankedIndex = new ArrayList<Integer>();

        // INFO: Mockup implementation: Turn around order of answers
        /*
        for (int i = answers.length - 1; i >= 0; i--) {
            reRankedIndex.add(i);
        }
         */

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();

        body.put("query", question);
        if (limit < 0) {
            int defaultTopN = 4;
            log.info("No limit set, therefore use default top " + defaultTopN + " ...");
            body.put("top_n", defaultTopN); // TODO: Make default configurable
        } else {
            body.put("top_n", limit);
        }
        body.put("return_documents", true);
        body.put("model", cohereReRankModel);

        ArrayNode docsNode = mapper.createArrayNode();
        body.put("documents", docsNode);
        for (String answer : answers) {
            ObjectNode textNode = mapper.createObjectNode();
            textNode.put("text", answer);
            docsNode.add(textNode);
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(body.toString(), headers);

        String requestUrl = cohereHost + "/rerank";
        log.info("Get re-ranked answers: " + requestUrl + " (Body: " + body + ")");
        ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
        JsonNode bodyNode = response.getBody();
        log.info("Cohere Response JSON: " + bodyNode);

        JsonNode resultsNode = bodyNode.get("results");
        if (resultsNode.isArray()) {
            for (int i = 0; i < resultsNode.size(); i++) {
                JsonNode node = resultsNode.get(i);
                int index = node.get("index").asInt();
                reRankedIndex.add(index);
            }
        }

        return reRankedIndex.toArray(new Integer[0]);
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Bearer " + cohereKey);
        // INFO: See https://docs.cohere.ai/reference/versioning
        headers.set("Cohere-Version", cohereVersion);
        return headers;
    }
}
