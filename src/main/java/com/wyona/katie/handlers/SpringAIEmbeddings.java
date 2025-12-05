package com.wyona.katie.handlers;

import com.wyona.katie.models.Vector;
import com.wyona.katie.models.EmbeddingType;
import com.wyona.katie.models.EmbeddingValueType;
import com.wyona.katie.models.FloatVector;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaEmbeddingOptions;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * https://docs.spring.io/spring-ai/reference/api/embeddings.html
 */
@Slf4j
@Component
public class SpringAIEmbeddings implements EmbeddingsProvider {

    @Autowired
    private OllamaEmbeddingModel embeddingModel;

    /**
     * @see EmbeddingsProvider#getEmbedding(String, String, EmbeddingType, EmbeddingValueType, String)
     */
    public Vector getEmbedding(String sentence, String model, EmbeddingType embeddingType, EmbeddingValueType valueType, String apiToken) {
        log.info("Get embedding for text '" + sentence + "' using SpringAI ...");

        EmbeddingResponse embeddingResponse = embeddingModel.call(
                new EmbeddingRequest(
                        List.of(sentence),
                        OllamaEmbeddingOptions.builder()
                                .model("chroma/all-minilm-l6-v2-f32")
                                .truncate(false)
                                .build()
                )
        );

        Embedding embedding = embeddingResponse.getResult();

        FloatVector floatVector = new FloatVector(embedding.getOutput());
        log.info("Embedding: " + floatVector);

        /*
        FloatVector floatVector = new FloatVector(3);
        floatVector.set(0, Float.valueOf("1.0"));
        floatVector.set(0, Float.valueOf("0.3"));
        floatVector.set(0, Float.valueOf("0.7"));

         */

        return floatVector;
    }
}
