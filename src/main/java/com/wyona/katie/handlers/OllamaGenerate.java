package com.wyona.katie.handlers;

import com.wyona.katie.models.*;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import io.github.ollama4j.models.chat.OllamaChatResult;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpTimeoutException;
import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class OllamaGenerate implements GenerateProvider {

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaHost;

    @Value("${ollama.timeout.in.seconds}")
    private Integer ollamaTimeout;

    @Value("${ollama.basic.auth.username:#{null}}")
    private String ollamaBasicAuthUsername;

    @Value("${ollama.basic.auth.password:#{null}}")
    private String ollamaBasicAuthPassword;

    @Value("${ollama.bearer.token:#{null}}")
    private String ollamaBearerToken;

    @Autowired
    OllamaChatModel ollamaChatModel;

    /**
     *
     */
    private String getHost(CompletionConfig completionConfig) {
        if (completionConfig.getHost() != null) {
            return completionConfig.getHost();
        } else {
            log.info("Use Ollama default host ...");
            return ollamaHost;
        }
    }

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
        if (true) {
            return getCompletionUsingOllama4j(promptMessages, assistant, completionConfig, temperature);
        } else {
            return getCompletionUsingSpringAI(promptMessages, assistant, completionConfig, temperature);
        }
    }

    /**
     * Get completion using Spring AI implementation https://docs.spring.io/spring-ai/reference/api/chat/ollama-chat.html
     */
    private CompletionResponse getCompletionUsingSpringAI(List<PromptMessage> promptMessages, CompletionAssistant assistant, CompletionConfig completionConfig, Double temperature) throws Exception {
        log.info("Spring AI Ollama implementation ...");
        PromptMessage firstMessage = promptMessages.get(0);

        OllamaModel model = null;
        if (completionConfig.getModel().equals("mistral")) {
            model = OllamaModel.MISTRAL;
        } else if (completionConfig.getModel().equals("llama2")) {
            model = OllamaModel.LLAMA2;
        } else {
            throw new Exception("Spring AI Ollama implementation does not support model '" + completionConfig.getModel() + "'");
        }

        ChatResponse response = ollamaChatModel.call(
                new Prompt(
                        firstMessage.getContent(),
                        OllamaChatOptions.builder()
                                .model(model)
                                .temperature(temperature)
                                .build()
                )
        );
        return new CompletionResponse(response.getResult().toString());
    }

    /**
     * Get completion using Ollama4J implementation https://github.com/ollama4j/ollama4j
     */
    private CompletionResponse getCompletionUsingOllama4j(List<PromptMessage> promptMessages, CompletionAssistant assistant, CompletionConfig completionConfig, Double temperature) throws Exception {
        log.info("Ollama4j implementation ...");
        log.info("Complete prompt using Ollama completion API (Ollama host: " + getHost(completionConfig) + ") ...");

        String completedText = null;

        // INFO: https://github.com/ollama4j/ollama4j and https://ollama4j.github.io/ollama4j/intro and https://ollama4j.github.io/ollama4j/apidocs/io/github/ollama4j/OllamaAPI.html
        OllamaAPI ollamaAPI = new OllamaAPI(getHost(completionConfig));
        //ollamaAPI.setVerbose(false); // INFO: Default is true

        if (ollamaBasicAuthUsername != null && ollamaBasicAuthPassword != null) {
            ollamaAPI.setBasicAuth(ollamaBasicAuthUsername, ollamaBasicAuthPassword);
        } else {
            log.info("No Ollama Basic Auth credentials configured.");
        }

        if (ollamaBearerToken != null) {
            ollamaAPI.setBearerAuth(ollamaBearerToken);
        } else {
            log.info("No Ollama Access Bearer Token configured.");
        }

        ollamaAPI.setRequestTimeoutSeconds(ollamaTimeout);
        OptionsBuilder optionsBuilder = new OptionsBuilder();
        if (temperature != null) {
            optionsBuilder = optionsBuilder.setTemperature(temperature.floatValue());
        }
        Options options = optionsBuilder.build();

        // https://ollama4j.github.io/ollama4j/apis-generate/chat
        OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(completionConfig.getModel());
        //OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(OllamaModelType.MISTRAL);
        PromptMessage firstMessage = promptMessages.get(0);
        OllamaChatRequest chatRequest = builder.withMessage(getOllamaChatMessageRole(firstMessage.getRole()), firstMessage.getContent()).build();
        for (int i = 1; i < promptMessages.size(); i++) {
            PromptMessage msg = promptMessages.get(i);
            chatRequest = builder.withMessages(chatRequest.getMessages()).withMessage(getOllamaChatMessageRole(msg.getRole()), msg.getContent()).build();
        }
        chatRequest = builder.withMessages(chatRequest.getMessages()).withOptions(options).build();

        // TODO: Set options, see https://github.com/ollama4j/ollama4j/issues/70
        try {
            OllamaChatResult result = ollamaAPI.chat(chatRequest);
            // https://ollama4j.github.io/ollama4j/apis-generate/generate
            //OllamaResult result = ollamaAPI.generate(model, promptMessages.get(promptMessages.size() - 1).getContent(), false, options);
            completedText = result.getResponse();

            return new CompletionResponse(completedText);
        } catch (HttpTimeoutException e) {
            log.warn(e.getMessage(), e);
            throw new HttpTimeoutException("Timeout is set to " + ollamaTimeout + " seconds. The timeout should be increased if necessary, see parameter 'ollama.timeout.in.seconds'. Original exception message: " + e.getMessage());
        } catch (OllamaBaseException e) {
            log.error(e.getMessage(), e);
            throw new Exception("Exception when trying to connect to Ollama. Original exception message: " + e.getMessage());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * @return true when Ollama server is alive and false otherwise
     */
    public boolean isAlive() {
        log.info("Check whether Ollama default host is alive: " + ollamaHost);
        OllamaAPI ollamaAPI = new OllamaAPI(ollamaHost);
        ollamaAPI.setBasicAuth(ollamaBasicAuthUsername, ollamaBasicAuthPassword);
        return ollamaAPI.ping();
    }

    /**
     *
     */
    private OllamaChatMessageRole getOllamaChatMessageRole(PromptMessageRole role) {
        if (role == PromptMessageRole.USER) {
            return OllamaChatMessageRole.USER;
        } else if (role == PromptMessageRole.SYSTEM) {
            return OllamaChatMessageRole.SYSTEM;
        } else if (role == PromptMessageRole.ASSISTANT) {
            return OllamaChatMessageRole.ASSISTANT;
        } else if (role == PromptMessageRole.TOOL) {
            return OllamaChatMessageRole.TOOL;
        }

        log.warn("Ollama4J does not support role '" + role + "'!");
        return OllamaChatMessageRole.USER;
    }
}
