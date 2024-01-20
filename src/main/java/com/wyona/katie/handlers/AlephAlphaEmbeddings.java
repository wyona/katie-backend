package com.wyona.katie.handlers;

import com.wyona.katie.models.EmbeddingType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class AlephAlphaEmbeddings implements EmbeddingsProvider {

    @Value("${aleph-alpha.host}")
    private String alephAlphaHost;

    @Value("${aleph-alpha.hosting}")
    private String hosting;

    /**
     * @see EmbeddingsProvider#getEmbedding(String, String, EmbeddingType, String)
     */
    public float[] getEmbedding(String sentence, String alephAlphaModel, EmbeddingType embeddingType, String alephAlphaToken) {
        log.info("Get embedding from Aleph Alpha for sentence '" + sentence + "' ...");

        float[] vector = null;
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders(alephAlphaToken);
            String requestBody = getRequestBody(sentence, alephAlphaModel);
            log.info("Request body: " + requestBody);
            HttpEntity<String> request = new HttpEntity<String>(requestBody, headers);

            String requestUrl = alephAlphaHost + "/semantic_embed";
            log.info("Get embedding: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("Response JSON: " + bodyNode);

            JsonNode embeddingNode = bodyNode.get("embedding");
            if (embeddingNode.isArray()) {
                vector = new float[embeddingNode.size()];
                log.info("Vector size: " + vector.length);

                for (int i = 0; i < vector.length; i++) {
                    vector[i] = Float.parseFloat(embeddingNode.get(i).asText());
                }
            } else {
                log.error("No embedding received!");
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
    private String getRequestBody(String sentence, String alephAlphaModel) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();
        body.put("model", alephAlphaModel);
        body.put("prompt", sentence);

        // INFO: https://docs.aleph-alpha.com/api/semantic-embed/
        body.put("representation", "symmetric");
        body.put("compress_to_size", 128); // INFO: Otherwise it will be 5120
        body.put("normalize", true);
        if (hosting.length() > 0) {
            body.put("hosting", hosting);
        }

        // INFO: https://docs.aleph-alpha.com/api/embed/
        /*
        ArrayNode layersNode = mapper.createArrayNode();
        layersNode.add(0);
        layersNode.add(1);
        body.put("layers", layersNode);

        ArrayNode poolingNode = mapper.createArrayNode();
        poolingNode.add("max");
        body.put("pooling", poolingNode);

         */

        return body.toString();
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders(String alephAlphaToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Bearer " + alephAlphaToken);
        return headers;
    }
}
