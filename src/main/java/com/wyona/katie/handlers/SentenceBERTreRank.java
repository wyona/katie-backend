package com.wyona.katie.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.ArrayList;

import org.apache.http.HttpHost;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.codec.binary.Base64;

/**
 *
 */
@Slf4j
@Component
public class SentenceBERTreRank implements ReRankProvider {

    @Value("${sbert.hostname}")
    private String sbertHostname;

    @Value("${sbert.scheme}")
    private String sbertScheme;

    @Value("${sbert.port}")
    private String sbertPort;

    @Value("${sbert.basic.auth.username}")
    private String sbertBasicAuthUsername;

    @Value("${sbert.basic.auth.password}")
    private String sbertBasicAuthPassword;

    /**
     * @see ReRankProvider#getReRankedAnswers(String, String[], int)
     */
    public Integer[] getReRankedAnswers(String question, String[] answers, int limit) {
        log.info("Re-rank answers using SentenceBERT's re-rank service");

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

        ArrayNode textsNode = mapper.createArrayNode();
        body.put("texts", textsNode);
        for (String answer : answers) {
            textsNode.add(answer);
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = getHttpHeaders();
        HttpEntity<String> request = new HttpEntity<String>(body.toString(), headers);

        String requestUrl = getHttpHost() + "/api/v1/cross-encoder-similarity-score";
        log.info("Get re-ranked answers: " + requestUrl + " (Body: " + body + ")");
        ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
        JsonNode bodyNode = response.getBody();
        log.info("SentenceBERT Response JSON: " + bodyNode);

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
     * Get SentenceBERT host, e.g. 'https://similar.ukatie.com:443'
     */
    private HttpHost getHttpHost() {
        return new HttpHost(sbertHostname, Integer.parseInt(sbertPort), sbertScheme);
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        if (sbertBasicAuthUsername != null && sbertBasicAuthUsername.length() > 0) {
            String auth = sbertBasicAuthUsername + ":" + sbertBasicAuthPassword;
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            headers.set("Authorization", authHeader);
        }
        return headers;
    }
}
