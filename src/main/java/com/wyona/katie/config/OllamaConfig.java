package com.wyona.katie.config;

import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.management.ModelManagementOptions;
import io.micrometer.observation.ObservationRegistry;

@Configuration
public class OllamaConfig {

    @Value("${spring.ai.ollama.base-url}")
    private String ollamaHost;

    @Value("${ollama.embedding.model}")
    private String ollamaEmbeddingModel;

    @Bean
    public OllamaApi ollamaApi() {
        return OllamaApi.builder()
                .baseUrl(ollamaHost)
                .build();
    }

    @Bean
    public OllamaEmbeddingModel ollamaEmbeddingModel(OllamaApi api) {
        return new OllamaEmbeddingModel(
                api,
                OllamaEmbeddingOptions.builder().model(ollamaEmbeddingModel).build(),
                ObservationRegistry.NOOP,
                ModelManagementOptions.builder().build()
        );
    }
}
