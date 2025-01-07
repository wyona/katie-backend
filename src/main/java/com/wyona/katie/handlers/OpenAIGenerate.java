package com.wyona.katie.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.PromptMessage;
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
public class OpenAIGenerate implements GenerateProvider {

    @Value("${openai.host}")
    private String openAIHost;

    /**
     * @see GenerateProvider#getCompletion(List, String, Double, String)
     */
    public String getCompletion(List<PromptMessage> promptMessages, String openAIModel, Double temperature, String openAIKey) throws Exception {
        if (true) {
            return chatCompletion(promptMessages, openAIModel, temperature, openAIKey);
        } else {
            return assistantThread(promptMessages, openAIModel, temperature, openAIKey);
        }
    }

    /**
     * Generate answer using OpenAI Chat Completion API https://platform.openai.com/docs/api-reference/chat
     */
    private String chatCompletion(List<PromptMessage> promptMessages, String openAIModel, Double temperature, String openAIKey) throws Exception {
        log.info("Complete prompt using OpenAI chat completion (API key: " + openAIKey.substring(0, 7) + "******) ...");

        String completedText = null;

        try {
            // INFO: See https://platform.openai.com/docs/api-reference/chat/create
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBodyNode = mapper.createObjectNode();
            requestBodyNode.put("model", openAIModel);
            //requestBodyNode.put("model", "gpt-4o-2024-08-06");
            ArrayNode messages = mapper.createArrayNode();
            requestBodyNode.put("messages", messages);

            for (PromptMessage msg : promptMessages) {
                ObjectNode messageNode = mapper.createObjectNode();
                messageNode.put("role", msg.getRole().toString());
                messageNode.put("content", msg.getContent());
                messages.add(messageNode);
            }

            String responseFormat = null;
            if (responseFormat != null && responseFormat.equals("json")) {
                // See https://platform.openai.com/docs/guides/structured-outputs
                ObjectNode responseFormatNode = mapper.createObjectNode();
                requestBodyNode.put("response_format", responseFormatNode);

                responseFormatNode.put("type", "json_schema");
                ObjectNode jsonSchemaNode = mapper.createObjectNode();
                responseFormatNode.put("json_schema", jsonSchemaNode);
                jsonSchemaNode.put("name", "TODO");

                ObjectNode schemaNode = mapper.createObjectNode();
                jsonSchemaNode.put("schema", schemaNode);
                schemaNode.put("type", "object");
                ObjectNode propertiesNode = mapper.createObjectNode();
                schemaNode.put("properties", propertiesNode);

                ObjectNode propertyNode = mapper.createObjectNode();
                propertiesNode.put("selected-answer", propertyNode);
                propertyNode.put("type", "string");
            }

            HttpHeaders headers = getHttpHeaders(openAIKey);
            HttpEntity<String> request = new HttpEntity<String>(requestBodyNode.toString(), headers);

            String requestUrl = openAIHost + "/v1/chat/completions";
            log.info("Get chat completion: " + requestUrl + " (Body: " + requestBodyNode + ")");
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBodyNode = response.getBody();
            log.info("JSON Response: " + responseBodyNode);

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

    /***
     * Generate answer using OpenAI Assistant Thread API https://platform.openai.com/docs/api-reference/threads
     */
    private String assistantThread(List<PromptMessage> promptMessages, String openAIModel, Double temperature, String openAIKey) throws Exception {
        log.info("Complete prompt using OpenAI assistant thread (API key: " + openAIKey.substring(0, 7) + "******) ...");

        String completedText = null;

        try {
            // INFO: See https://platform.openai.com/docs/api-reference/threads
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBodyNode = mapper.createObjectNode();
            //requestBodyNode.put("model", openAIModel);
            //requestBodyNode.put("model", "gpt-4o-2024-08-06");
            ArrayNode messages = mapper.createArrayNode();
            requestBodyNode.put("messages", messages);

            for (PromptMessage msg : promptMessages) {
                ObjectNode messageNode = mapper.createObjectNode();
                messageNode.put("role", msg.getRole().toString());
                messageNode.put("content", msg.getContent());

                String[] attachments = msg.getAttachments();
                if (attachments != null && attachments.length > 0) {
                    ArrayNode attachmentsNode = mapper.createArrayNode();
                    messageNode.put("attachments", attachmentsNode);
                    for (String attachment : attachments) {
                        // TODO: Add attachment, see for example https://platform.openai.com/docs/assistants/tools/file-search
                    }
                }

                messages.add(messageNode);
            }

            HttpHeaders headers = getHttpHeaders(openAIKey);
            headers.set("OpenAI-Beta", "assistants=v2");
            HttpEntity<String> request = new HttpEntity<String>(requestBodyNode.toString(), headers);

            String requestUrl = openAIHost + "/v1/threads";
            log.info("Get chat completion: " + requestUrl + " (Body: " + requestBodyNode + ")");
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBodyNode = response.getBody();
            log.info("JSON Response: " + responseBodyNode);

            String threadId = responseBodyNode.get("id").asText();
            if (threadId != null) {
                completedText = "Thread ID: " + threadId;
            } else {
                log.warn("No thread Id!");
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }

        return completedText;
    }

    /**
     * Get default headers
     */
    private HttpHeaders getHttpHeaders(String openAIKey) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/json");
        headers.set("Content-Type", "application/json; charset=UTF-8");
        headers.set("Authorization", "Bearer " + openAIKey);
        return headers;
    }
}
