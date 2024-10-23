package com.wyona.katie.handlers;

import com.wyona.katie.models.PromptMessage;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.models.response.OllamaResult;
import io.github.ollama4j.utils.Options;
import io.github.ollama4j.utils.OptionsBuilder;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class OllamaGenerate implements GenerateProvider {

    @Value("${ollama.host}")
    private String ollamaHost;

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

        // INFO: https://github.com/amithkoujalgi/ollama4j
        OllamaAPI ollamaAPI = new OllamaAPI(ollamaHost);
        //ollamaAPI.setVerbose(false); // INFO: Default is true
        // TODO: When not set inside application.properties, then do not set here
        ollamaAPI.setBasicAuth(ollamaBasicAuthUsername, ollamaBasicAuthPassword);
        ollamaAPI.setRequestTimeoutSeconds(30);
        OptionsBuilder optionsBuilder = new OptionsBuilder();
        if (temperature != null) {
            optionsBuilder = optionsBuilder.setTemperature(temperature.floatValue());
        }
        Options options = optionsBuilder.build();
        // TODO: Use all messages and not just last message, see for example OpenAIGenerate
        OllamaResult result = ollamaAPI.generate(model, promptMessages.get(promptMessages.size() - 1).getContent(), false, options);
        completedText = result.getResponse();

        return completedText;
    }
}
