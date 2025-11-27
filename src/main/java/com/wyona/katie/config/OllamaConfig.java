package com.wyona.katie.config;

import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import io.micrometer.observation.ObservationRegistry;

@Configuration
public class OllamaConfig {

    @Bean
    public OllamaApi ollamaApi() {
        return OllamaApi.builder()
                .baseUrl("http://localhost:11434")  // your Ollama instance
                .build();
    }

    @Bean
    public OllamaEmbeddingModel ollamaEmbeddingModel(OllamaApi api) {
        return new OllamaEmbeddingModel(
                api,
                OllamaEmbeddingOptions.builder().model("mistral").build(),
                ObservationRegistry.NOOP,
                ModelManagementOptions.builder().build()
        );
    }
}
