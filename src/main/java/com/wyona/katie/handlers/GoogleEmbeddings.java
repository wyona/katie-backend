package com.wyona.katie.handlers;

import com.wyona.katie.ai.models.FloatVector;
import com.wyona.katie.models.EmbeddingType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.EmbeddingValueType;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

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
public class GoogleEmbeddings implements EmbeddingsProvider {

    // https://cloud.google.com/vertex-ai/docs/generative-ai/embeddings/get-text-embeddings
    private String googleHost = "https://us-central1-aiplatform.googleapis.com";

    @Value("${google.key}")
    private String googleKey;

    /**
     * @see EmbeddingsProvider#getEmbedding(String, String, EmbeddingType, EmbeddingValueType, String)
     */
    public FloatVector getEmbedding(String sentence, String model, EmbeddingType embeddingType, EmbeddingValueType valueType, String apiToken) {
        log.info("Get embedding from Google for sentence '" + sentence + "' ...");

        FloatVector vector = null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders(apiToken);
            HttpEntity<String> request = new HttpEntity<String>(createRequestBody(sentence, model), headers);

            // https://console.cloud.google.com/apis/dashboard?authuser=0&hl=de&project=katie-388014
            String requestUrl = googleHost + "/v1/projects/katie-388014/locations/us-central1/publishers/google/models/textembedding-gecko:predict";
            log.info("Get embeddings: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("Response JSON: " + bodyNode);

            JsonNode embeddingsNode = bodyNode.get("predictions").get(0).get("embeddings").get("values");
            if (embeddingsNode.isArray()) {
                vector = new FloatVector(embeddingsNode.size());
                log.info("Vector size: " + vector.getDimension());
                for (int i = 0; i < vector.getDimension(); i++) {
                    vector.set(i, Float.parseFloat(embeddingsNode.get(i).asText()));
                }
            } else {
                log.error("No embeddings received!");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
        }

        /*
        if (vector != null) {
            for (int i = 0; i < vector.length; i++) {
                log.info(i + ": " + vector[i]);
            }
        }
         */

        return vector;
    }

    /**
     *
     */
    private String createRequestBody(String sentence, String model) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();

        // INFO: One could provide multiple texts with one request
        ArrayNode instancesNode = mapper.createArrayNode();
        body.put("instances", instancesNode);

        ObjectNode contentNode = mapper.createObjectNode();
        contentNode.put("content", sentence);
        instancesNode.add(contentNode);

        return body.toString();
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders(String apiToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");

        // TODO: Use apiToken instead googleKey
        headers.set("Authorization", "Bearer " + googleKey);

        return headers;
    }
}
