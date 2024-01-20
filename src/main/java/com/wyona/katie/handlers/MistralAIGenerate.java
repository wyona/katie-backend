package com.wyona.katie.handlers;

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
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

/**
 *
 */
@Slf4j
@Component
public class MistralAIGenerate implements GenerateProvider {

    @Value("${mistral.ai.host}")
    private String mistralAIHost;

    /**
     * @see GenerateProvider#getCompletion(String, String, Double, String)
     */
    public String getCompletion(String prompt, String model, Double temperature, String mistralAIKey) throws Exception {
        log.info("Complete prompt using Mistral AI chat completion ...");

        String completedText = null;

        try {
            // INFO: See https://docs.mistral.ai/api/ and https://platform.openai.com/docs/api-reference/chat/create
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBodyNode = mapper.createObjectNode();
            requestBodyNode.put("model", model);
            if (temperature != null) {
                requestBodyNode.put("temperature", temperature);
            }
            ArrayNode messages = mapper.createArrayNode();
            requestBodyNode.put("messages", messages);
            ObjectNode messageNode = mapper.createObjectNode();
            messageNode.put("role", "user");
            messageNode.put("content", prompt);
            messages.add(messageNode);

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders(mistralAIKey);
            HttpEntity<String> request = new HttpEntity<String>(requestBodyNode.toString(), headers);

            String requestUrl = mistralAIHost + "/v1/chat/completions";
            log.info("Get chat completion: " + requestUrl + " (Body: " + requestBodyNode + ")");
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBodyNode = response.getBody();
            log.info("Response JSON: " + responseBodyNode);

            JsonNode choicesNode = responseBodyNode.get("choices");
            if (choicesNode.isArray()) {
                completedText = choicesNode.get(0).get("message").get("content").asText();
            } else {
                log.warn("No choices!");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }

        return completedText;
    }

    /**
     *
     */
    private HttpHeaders getHttpHeaders(String mistralAIKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Bearer " + mistralAIKey);
        return headers;
    }
}
