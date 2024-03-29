package com.wyona.katie.handlers;

import com.wyona.katie.models.EmbeddingType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.EmbeddingValueType;
import com.wyona.katie.models.FloatVector;
import com.wyona.katie.models.Vector;
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
public class OpenAIAzureEmbeddings implements EmbeddingsProvider {

    @Value("${openai.azure.host}")
    private String openAIAzureHost;
    @Value("${openai.azure.deployment.id}")
    private String openAIAzureDeplyomentId;
    @Value("${openai.azure.api.version}")
    private String openAIAzureApiVersion;

    /**
     * @see EmbeddingsProvider#getEmbedding(String, String, EmbeddingType, EmbeddingValueType, String)
     */
    public Vector getEmbedding(String sentence, String openAIModel, EmbeddingType embeddingType, EmbeddingValueType valueType, String openAIKey) throws Exception {
        log.info("Get embedding from OpenAI for sentence '" + sentence + "' ...");

        FloatVector vector = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode inputNode = mapper.createObjectNode();
            inputNode.put("input", sentence);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders(openAIKey);
            HttpEntity<String> request = new HttpEntity<String>(inputNode.toString(), headers);

            String requestUrl = openAIAzureHost + "/openai/deployments/" + openAIAzureDeplyomentId + "/embeddings?api-version=" + openAIAzureApiVersion;

            log.info("Get embedding: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("JSON: " + bodyNode);

            JsonNode embeddingNode = bodyNode.get("data").get(0).get("embedding");
            if (embeddingNode.isArray()) {
                vector = new FloatVector(embeddingNode.size());

                for (int i = 0; i < vector.getDimension(); i++) {
                    vector.set(i, Float.parseFloat(embeddingNode.get(i).asText()));
                }
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw e;
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
    private HttpHeaders getHttpHeaders(String openAIKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");

        // OpenAI Azure, https://learn.microsoft.com/en-us/azure/ai-services/openai/reference#authentication
        headers.set("api-key", openAIKey);

        return headers;
    }
}
