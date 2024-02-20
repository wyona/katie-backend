package com.wyona.katie.handlers;

import com.wyona.katie.models.EmbeddingType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 */
@Slf4j
@Component
public class CohereEmbeddings implements EmbeddingsProvider {

    @Value("${cohere.host}")
    private String cohereHost;

    @Value("${cohere.version}")
    private String cohereVersion;

    /**
     * @see EmbeddingsProvider#getEmbedding(String, String, EmbeddingType, String)
     */
    public float[] getEmbedding(String sentence, String cohereModel, EmbeddingType embeddingType, String cohereKey) {
        // INFO: https://txt.cohere.com/introducing-embed-v3/
        String inputType = "search_document";
        if (embeddingType.equals(EmbeddingType.SEARCH_QUERY)) {
            inputType = "search_query";
        }
        //String inputType = "classification";
        //String inputType = "clustering";

        log.info("Get embedding from Cohere (Model: " + cohereModel + ", Input type: " + inputType + ") for sentence '" + sentence + "' ...");

        float[] vector = null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders(cohereKey);
            HttpEntity<String> request = new HttpEntity<String>(createRequestBody(sentence, cohereModel, inputType), headers);

            String requestUrl = cohereHost + "/embed";
            log.info("Get embeddings: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("Response JSON: " + bodyNode);

            JsonNode embeddingsNode = bodyNode.get("embeddings");
            if (embeddingsNode.isArray()) {
                JsonNode embeddingNode = embeddingsNode.get(0); // INFO: Embedding for sentence resp. first text node (see createRequestBody(...))

                if (embeddingNode.isArray()) {
                    vector = new float[embeddingNode.size()];
                    log.info("Vector size: " + vector.length);

                    for (int i = 0; i < vector.length; i++) {
                        vector[i] = Float.parseFloat(embeddingNode.get(i).asText());
                    }
                } else {
                    log.error("No embedding received for sentence '" + sentence + "'");
                }
            } else {
                log.error("No embeddings received!");
            }
        } catch(HttpClientErrorException e) {
            if (e.getRawStatusCode() == 429) {
                log.warn("Cohere Rate limit reached!");
                // TODO: Throw exception, such that EmbeddingsService can throttle and retry
            }
            log.error(e.getMessage(), e);
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
     * @param inputType Cohere input type, e.g. "search_document", "search_query", "classification", "clustering"
     */
    private String createRequestBody(String sentence, String cohereModel, String inputType) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();
        body.put("model", cohereModel);

        body.put("input_type", inputType);

        // INFO: One could provide multiple texts with one request
        ArrayNode textsNode = mapper.createArrayNode();
        textsNode.add(sentence);
        body.put("texts", textsNode);

        return body.toString();
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders(String cohereKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Bearer " + cohereKey);

        // INFO: See https://docs.cohere.ai/reference/versioning
        headers.set("Cohere-Version", cohereVersion);
        return headers;
    }
}
