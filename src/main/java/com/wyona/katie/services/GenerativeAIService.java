package com.wyona.katie.services;

import com.wyona.katie.handlers.*;
import com.wyona.katie.models.CompletionImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Generative AI service
 */
@Slf4j
@Component
public class GenerativeAIService {

    @Value("${mistral.ai.completion.model}")
    private String mistralAIModel;
    @Value("${mistral.api.key}")
    private String mistralAIKey;

    @Value("${openai.generate.model}")
    private String openAIModel;
    @Value("${openai.key}")
    private String openAIKey;

    @Value("${ollama.completion.model}")
    private String ollamaModel;

    @Value("${aleph-alpha.token}")
    private String alephAlphaToken;

    @Autowired
    private OpenAIGenerate openAIGenerate;
    @Autowired
    private AlephAlphaGenerate alephAlphaGenerate;
    @Autowired
    private MistralAIGenerate mistralAIGenerate;
    @Autowired
    private OllamaGenerate ollamaGenerate;

    /**
     * Get GenAI implementation
     */
    public GenerateProvider getGenAIImplementation(CompletionImpl impl) {
        if (impl.equals(CompletionImpl.ALEPH_ALPHA)) {
            return alephAlphaGenerate;
        } else if (impl.equals(CompletionImpl.OPENAI)) {
            return openAIGenerate;
        } else if (impl.equals(CompletionImpl.MISTRAL_AI)) {
            return mistralAIGenerate;
        } else if (impl.equals(CompletionImpl.OLLAMA)) {
            return ollamaGenerate;
        } else {
            log.error("No such completion implemention supported yet: " + impl);
            return null;
        }
    }

    /**
     * Get GenAI model
     */
    public String getCompletionModel(CompletionImpl generateImpl) {
        String model = null;
        if (generateImpl.equals(CompletionImpl.ALEPH_ALPHA)) {
            model = "luminous-base"; // TODO: Make configurable
        } else if (generateImpl.equals(CompletionImpl.OPENAI)) {
            model = openAIModel;
        } else if (generateImpl.equals(CompletionImpl.MISTRAL_AI)) {
            model = mistralAIModel;
        } else if (generateImpl.equals(CompletionImpl.OLLAMA)) {
            model = ollamaModel;
        } else {
            log.error("No such completion implemention supported yet: " + generateImpl);
            model = null;
        }
        return model;
    }

    /**
     * Get API token of completion implementation
     */
    public String getApiToken(CompletionImpl generateImpl) {
        if (generateImpl.equals(CompletionImpl.ALEPH_ALPHA)) {
            return alephAlphaToken;
        } else if (generateImpl.equals(CompletionImpl.OPENAI)) {
            return openAIKey;
            //} else if (generateImpl.equals(EmbeddingsImpl.OPENAI_AZURE)) {
            //    return openAIAzureKey;
        } else if (generateImpl.equals(CompletionImpl.MISTRAL_AI)) {
            return mistralAIKey;
        } else {
            // INFO: For example Ollama
            return null;
        }
    }
}
