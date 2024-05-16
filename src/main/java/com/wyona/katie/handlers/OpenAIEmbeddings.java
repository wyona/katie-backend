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
public class OpenAIEmbeddings implements EmbeddingsProvider {

    @Value("${openai.host}")
    private String openAIHost;

    /**
     * @see EmbeddingsProvider#getEmbedding(String, String, EmbeddingType, EmbeddingValueType, String)
     */
    public Vector getEmbedding(String sentence, String openAIModel, EmbeddingType embeddingType, EmbeddingValueType valueType, String openAIKey) throws Exception {
        // INFO: https://platform.openai.com/docs/guides/embeddings/what-are-embeddings
        String requestUrl = openAIHost + "/v1/embeddings";

        return getEmbeddingFromOpenAICompatibleInterface(requestUrl, openAIModel, sentence, openAIKey);
    }

    /**
     * @param requestUrl URL of OpenAI compatible embedding endpoint, e.g. "http://localhost:3000/v1/embeddings" or "https://api.mistral.ai/v1/embeddings"
     * @param modelName Model name, e.g. "mistral-embed"
     */
    public Vector getEmbeddingFromOpenAICompatibleInterface(String requestUrl, String modelName, String sentence, String token) throws Exception {
        log.info("Get embedding from OpenAI compatible endpoint for sentence '" + sentence + "' ...");

        FloatVector vector = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootNode = mapper.createObjectNode();
            rootNode.put("input", sentence); // TODO: Array of strings for batch processing
            rootNode.put("model", modelName);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders(token);
            HttpEntity<String> request = new HttpEntity<String>(rootNode.toString(), headers);

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

        headers.set("Authorization", "Bearer " + openAIKey);

        return headers;
    }
}
