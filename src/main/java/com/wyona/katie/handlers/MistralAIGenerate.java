package com.wyona.katie.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.*;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class MistralAIGenerate implements GenerateProvider {

    @Value("${mistral.ai.host}")
    private String mistralAIHost;

    /**
     * @see GenerateProvider#getAssistant(String, String, String, List, String, String)
     */
    public CompletionAssistant getAssistant(String id, String name, String instructions, List<CompletionTool> tools, String model, String apiToken) {
        log.error("Not implemented yet!");
        return null;
    }

    /**
     * @see GenerateProvider#getCompletion(List, CompletionAssistant, CompletionConfig, Double)
     */
    public CompletionResponse getCompletion(List<PromptMessage> promptMessages, CompletionAssistant assistant, CompletionConfig completionConfig, Double temperature) throws Exception {
        log.info("Complete prompt using Mistral AI chat completion ...");

        String completedText = null;

        try {
            // INFO: See https://docs.mistral.ai/api/ and https://platform.openai.com/docs/api-reference/chat/create
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBodyNode = mapper.createObjectNode();
            requestBodyNode.put("model", completionConfig.getModel());
            if (temperature != null) {
                requestBodyNode.put("temperature", temperature);
            }
            ArrayNode messages = mapper.createArrayNode();
            requestBodyNode.put("messages", messages);

            for (PromptMessage msg : promptMessages) {
                ObjectNode messageNode = mapper.createObjectNode();
                messageNode.put("role", msg.getRole().toString());
                messageNode.put("content", msg.getContent());
                messages.add(messageNode);
            }

            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders(completionConfig.getApiKey());
            HttpEntity<String> request = new HttpEntity<String>(requestBodyNode.toString(), headers);

            String requestUrl = mistralAIHost + "/v1/chat/completions";
            log.info("Get chat completion: " + requestUrl + " (Request Body: " + requestBodyNode + ")");
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

        return new CompletionResponse(completedText);
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
