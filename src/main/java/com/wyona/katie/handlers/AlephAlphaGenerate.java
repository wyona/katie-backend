package com.wyona.katie.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
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
public class AlephAlphaGenerate implements GenerateProvider {

    @Value("${aleph-alpha.host}")
    private String alephAlphaHost;

    /**
     * @see GenerateProvider#getAssistant(String, String, String, List, CompletionConfig)
     */
    public CompletionAssistant getAssistant(String id, String name, String instructions, List<CompletionTool> tools, CompletionConfig completionConfig) {
        log.error("Not implemented yet!");
        return null;
    }

    /**
     * @see GenerateProvider#getCompletion(List, CompletionAssistant, CompletionConfig, Double)
     */
    public CompletionResponse getCompletion(List<PromptMessage> promptMessages, CompletionAssistant assistant, CompletionConfig completionConfig, Double temperature) throws Exception {
        log.info("Complete prompt using Aleph Alpha completion ...");

        String completedText = null;

        try {
            // INFO: See https://docs.aleph-alpha.com/api/complete/
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = getHttpHeaders(completionConfig.getApiKey());
            HttpEntity<String> request = new HttpEntity<String>(createRequestBody(promptMessages.get(0).getContent(), completionConfig.getModel()), headers);

            String requestUrl = alephAlphaHost + "/complete";
            log.info("Get completion: " + requestUrl);
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode bodyNode = response.getBody();
            log.info("Response JSON: " + bodyNode);

            completedText = "TODO";

            JsonNode completionsNode = bodyNode.get("completions");
            if (completionsNode.isArray()) {
                completedText = completionsNode.get(0).get("completion").asText();
            } else {
                log.warn("No completions!");
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

        return new CompletionResponse(completedText);
    }

    /**
     *
     */
    private String createRequestBody(String prompt, String alephAlphaModel) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode body = mapper.createObjectNode();
        body.put("model", alephAlphaModel);
        body.put("prompt", prompt);

        // INFO: See https://docs.aleph-alpha.com/api/complete/
        body.put("maximum_tokens", 128); // INFO: May not exceed 2048

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
