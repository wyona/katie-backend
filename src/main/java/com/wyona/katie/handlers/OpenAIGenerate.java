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
        if (false) {
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

        //String assistantId = createAssistant("Legal Insurance Assistant", "You are a legal insurance expert. Use your knowledge base to select the relevant documents to answer questions about legal topics.", openAIModel, temperature, openAIKey);

        String assistantId = "asst_SVESqIZjrikjrE99hPXTJWgJ";
        String threadId = createThread(promptMessages, openAIKey);
        String completedText = runThread(assistantId, threadId, openAIKey);

        return completedText;
    }

    /**
     * @param name Name of assistant, e.g. "Legal Insurance Assistant"
     * @param instructions Instructions, e.g. "You are a personal math tutor. When asked a question, write and run Python code to answer the question."
     */
    private String createAssistant(String name, String instructions, String openAIModel, Double temperature, String openAIKey) throws Exception {
        log.info("Create assistant (API key: " + openAIKey.substring(0, 7) + "******) ...");

        try {
            // INFO: See https://platform.openai.com/docs/api-reference/threads
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBodyNode = mapper.createObjectNode();
            requestBodyNode.put("model", openAIModel);
            //requestBodyNode.put("model", "gpt-4o-2024-08-06");
            requestBodyNode.put("instructions", instructions);
            requestBodyNode.put("name", name);

            if (false) {
                ArrayNode toolsNode = mapper.createArrayNode();
                requestBodyNode.put("tools", toolsNode);
                ObjectNode fileSearchTool = mapper.createObjectNode();
                fileSearchTool.put("type", "file_search");
                toolsNode.add(fileSearchTool);
            }

            HttpHeaders headers = getAssistantHttpHeaders(openAIKey);
            HttpEntity<String> request = new HttpEntity<String>(requestBodyNode.toString(), headers);

            String requestUrl = openAIHost + "/v1/assistants";
            log.info("Create assistant: " + requestUrl + " (Body: " + requestBodyNode + ")");
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(requestUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBodyNode = response.getBody();
            log.info("JSON Response: " + responseBodyNode);

            String assistantId = responseBodyNode.get("id").asText();
            if (assistantId != null) {
                return assistantId;
            } else {
                log.error("No assistant Id!");
                return null;
            }
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * @return thread Id
     */
    private String createThread(List<PromptMessage> promptMessages, String openAIKey) throws Exception {
        log.info("Create thread (API key: " + openAIKey.substring(0, 7) + "******) ...");

        String completedText = null;

        try {
            // INFO: See https://platform.openai.com/docs/api-reference/threads
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode requestBodyNode = mapper.createObjectNode();
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

            HttpHeaders headers = getAssistantHttpHeaders(openAIKey);
            HttpEntity<String> request = new HttpEntity<String>(requestBodyNode.toString(), headers);

            String createThreadUrl = openAIHost + "/v1/threads";
            log.info("Create thread: " + createThreadUrl + " (Body: " + requestBodyNode + ")");
            RestTemplate restTemplate = new RestTemplate();
            ResponseEntity<JsonNode> response = restTemplate.exchange(createThreadUrl, HttpMethod.POST, request, JsonNode.class);
            JsonNode responseBodyNode = response.getBody();
            log.info("JSON Response: " + responseBodyNode);

            String threadId = responseBodyNode.get("id").asText();
            if (threadId != null) {
                completedText = "Thread ID: " + threadId;
            } else {
                log.warn("No thread Id!");
                return null;
            }

            /*
            ObjectNode requestBodyNode2 = mapper.createObjectNode();
            requestBodyNode2.put("role", "user");
            requestBodyNode2.put("content", "How can I upload a file");

            HttpEntity<String> request2 = new HttpEntity<String>(requestBodyNode2.toString(), headers);

            String createMessageUrl = openAIHost + "/v1/threads/" +threadId + "/messages";
            log.info("Create message " + createMessageUrl + " (Body: " + requestBodyNode2 + ")");
            RestTemplate restTemplate2 = new RestTemplate();
            ResponseEntity<JsonNode> response2 = restTemplate2.exchange(createMessageUrl, HttpMethod.POST, request2, JsonNode.class);
            JsonNode responseBodyNode2 = response2.getBody();
            log.info("JSON Response: " + responseBodyNode2);
             */

            return threadId;
        } catch(Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     *
     */
    private String runThread(String assistantId, String threadId, String openAIKey) throws Exception {
        log.info("Run thread (API key: " + openAIKey.substring(0, 7) + "******) ...");

        String completedText = null;

        try {
            // INFO: See https://platform.openai.com/docs/api-reference/threads
            ObjectMapper mapper = new ObjectMapper();

            ObjectNode requestBodyNode2 = mapper.createObjectNode();
            requestBodyNode2.put("assistant_id", assistantId);

            HttpHeaders headers = getAssistantHttpHeaders(openAIKey);
            HttpEntity<String> request2 = new HttpEntity<String>(requestBodyNode2.toString(), headers);

            String runThreadUrl = openAIHost + "/v1/threads/" +threadId + "/runs";
            log.info("Run thread " + runThreadUrl + " (Body: " + requestBodyNode2 + ")");
            RestTemplate restTemplate2 = new RestTemplate();
            ResponseEntity<JsonNode> response2 = restTemplate2.exchange(runThreadUrl, HttpMethod.POST, request2, JsonNode.class);
            JsonNode responseBodyNode2 = response2.getBody();
            log.info("JSON Response: " + responseBodyNode2);
            String runId = responseBodyNode2.get("id").asText();

            String status = responseBodyNode2.get("status").asText();
            int timeoutCounter = 0;
            while (!status.equals("completed")) {
                timeoutCounter++;
                if (timeoutCounter > 10) {
                    return "Timeout!";
                }
                try {
                    log.info("Sleep for 1 second ...");
                    Thread.sleep(1000);
                } catch(Exception e) {
                    log.error(e.getMessage(), e);
                }
                status = getRunStatus(threadId, runId, openAIKey);
                log.info("Thread status: " + status);
            }

            HttpEntity<String> request3 = new HttpEntity<String>(headers);
            String getMessagesUrl = openAIHost + "/v1/threads/" +threadId + "/messages";
            log.info("Get messages " + getMessagesUrl);
            RestTemplate restTemplate3 = new RestTemplate();
            ResponseEntity<JsonNode> response3 = restTemplate3.exchange(getMessagesUrl, HttpMethod.GET, request3, JsonNode.class);
            JsonNode responseBodyNode3 = response3.getBody();
            log.info("JSON Response: " + responseBodyNode3);

            JsonNode dataNode = responseBodyNode3.get("data");
            if (dataNode.isArray()) {
                completedText = dataNode.get(0).get("content").get(0).get("text").get("value").asText();
            } else {
                log.warn("No data!");
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
    private String getRunStatus(String threadId, String runId, String openAIKey) throws Exception {
        HttpHeaders headers = getAssistantHttpHeaders(openAIKey);
        HttpEntity<String> request = new HttpEntity<String>(headers);
        String getRunUrl = openAIHost + "/v1/threads/" + threadId + "/runs/" + runId;
        log.info("Get run " + getRunUrl);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<JsonNode> response = restTemplate.exchange(getRunUrl, HttpMethod.GET, request, JsonNode.class);
        JsonNode responseBodyNode = response.getBody();
        log.info("JSON Response: " + responseBodyNode);

        return responseBodyNode.get("status").asText();
    }

    /**
     *
     */
    private HttpHeaders getAssistantHttpHeaders(String openAIKey) {
        HttpHeaders headers = getHttpHeaders(openAIKey);
        headers.set("OpenAI-Beta", "assistants=v2");
        return headers;
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
