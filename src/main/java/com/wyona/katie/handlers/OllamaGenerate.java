package com.wyona.katie.handlers;

import com.wyona.katie.models.PromptMessage;
import com.wyona.katie.models.PromptMessageRole;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.chat.OllamaChatMessage;
import io.github.ollama4j.models.chat.OllamaChatMessageRole;
import io.github.ollama4j.models.chat.OllamaChatRequest;
import io.github.ollama4j.models.chat.OllamaChatRequestBuilder;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.types.OllamaModelType;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class OllamaGenerate implements GenerateProvider {

    @Value("${ollama.host}")
    private String ollamaHost;

    @Value("${ollama.timeout.in.seconds}")
    private Integer ollamaTimeout;

    @Value("${ollama.basic.auth.username}")
    private String ollamaBasicAuthUsername;

    @Value("${ollama.basic.auth.password}")
    private String ollamaBasicAuthPassword;

    /**
     * @see GenerateProvider#getCompletion(List, String, Double, String)
     */
    public String getCompletion(List<PromptMessage> promptMessages, String model, Double temperature, String apiKey) throws Exception {
        log.info("Complete prompt using Ollama completion API (" + ollamaHost + ") ...");

        String completedText = null;

        // INFO: https://github.com/ollama4j/ollama4j and https://ollama4j.github.io/ollama4j/intro and https://ollama4j.github.io/ollama4j/apidocs/io/github/ollama4j/OllamaAPI.html
        OllamaAPI ollamaAPI = new OllamaAPI(ollamaHost);
        //ollamaAPI.setVerbose(false); // INFO: Default is true

        // TODO: When not set inside secret-keys.properties, then do not set here
        ollamaAPI.setBasicAuth(ollamaBasicAuthUsername, ollamaBasicAuthPassword);

        ollamaAPI.setRequestTimeoutSeconds(ollamaTimeout);
        OptionsBuilder optionsBuilder = new OptionsBuilder();
        if (temperature != null) {
            optionsBuilder = optionsBuilder.setTemperature(temperature.floatValue());
        }
        Options options = optionsBuilder.build();

        // https://ollama4j.github.io/ollama4j/apis-generate/chat
        OllamaChatRequestBuilder builder = OllamaChatRequestBuilder.getInstance(model);
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
            OllamaResult result = ollamaAPI.chat(chatRequest);
            // https://ollama4j.github.io/ollama4j/apis-generate/generate
            //OllamaResult result = ollamaAPI.generate(model, promptMessages.get(promptMessages.size() - 1).getContent(), false, options);
            completedText = result.getResponse();

            return completedText;
        } catch (HttpTimeoutException e) {
            log.warn(e.getMessage(), e);
            throw new HttpTimeoutException("Timeout is set to " + ollamaTimeout + " seconds. The timeout should be increased if necessary, see parameter 'ollama.timeout.in.seconds'. Original exception message: " + e.getMessage());
        }
    }

    /**
     * @return true when Ollama server is alive and false otherwise
     */
    public boolean isAlive() {
        log.info("Check whether Ollama is alive: " + ollamaHost);
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
