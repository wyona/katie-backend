package com.wyona.katie.services;

import com.wyona.katie.handlers.*;
import com.wyona.katie.models.Context;
import com.wyona.katie.models.EmbeddingType;
import com.wyona.katie.models.EmbeddingsImpl;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;

@Slf4j
@Component
public class EmbeddingsService {

    @Autowired
    private SentenceBERTQuestionAnswerImpl sbertImpl;

    @Autowired
    private OpenAIEmbeddings openAIImpl;

    @Autowired
    private OpenAIAzureEmbeddings openAIAzureImpl;

    @Autowired
    private CohereEmbeddings cohereEmbeddingsImpl;

    @Autowired
    private AlephAlphaEmbeddings alephAlphaEmbeddingsImpl;

    @Autowired
    //private NumentaEmbeddingsWithPreTokenization numentaEmbeddings; // INFO: With pre tokenization
    private NumentaEmbeddingsWithoutPreTokenization numentaEmbeddings;

    @Autowired
    private GoogleEmbeddings googleEmbeddings;

    /**
     * Get embedding for a text
     * @param domain Katie Domain associated with text
     * @return embedding vector
     */
    public float[] getEmbedding(String text, Context domain, EmbeddingType embeddingType) throws Exception {
        try {
            return getEmbedding(text, domain.getEmbeddingsImpl(), domain.getEmbeddingsModel(), embeddingType, domain.getEmbeddingsApiToken());
        } catch (Exception e) {
            throw e;
        } finally {
            // INFO: Increase count only when embedding generation was successful
            increaseEmbeddingCount(domain);
        }
    }

    /**
     * Count how many embeddings have been generated for this domain, which might be used for billing
     */
    private void increaseEmbeddingCount(Context domain) throws Exception {
        if (true) {
            log.info("TODO: Concurrency, embedding counter not thread safe, therefore do not count!");
            return;
        }

        domain.getEmbeddingsImpl(); // TODO: Count per implementation

        File embeddingCounterFile = new File(domain.getContextDirectory(), "embedding-count.txt");

        int counter = 0;
        if (embeddingCounterFile.exists()) {
            counter = Integer.parseInt(Utils.convertInputStreamToString(new FileInputStream(embeddingCounterFile)));
        }
        counter++;
        log.info("Embedding count: " + counter);

        Utils.saveText("" + counter, embeddingCounterFile, false);
    }

    /**
     * Get embedding for a text
     * @param sentence Text
     * @param impl Embedding provider
     * @param model Model of embeddings implementation
     * @param embeddingType Embedding type
     * @param apiToken API token of embedding provider
     * @return embedding vector
     */
    public float[] getEmbedding(String sentence, EmbeddingsImpl impl, String model, EmbeddingType embeddingType, String apiToken) throws Exception {
        float[] vector = null;
        if (impl.equals(EmbeddingsImpl.SBERT) || impl.equals(EmbeddingsImpl.UNSET)) {
            vector = sbertImpl.getEmbedding(sentence, null, embeddingType, apiToken);
        } else if (impl.equals(EmbeddingsImpl.OPENAI)) {
            vector = openAIImpl.getEmbedding(sentence, model, embeddingType, apiToken);
        } else if (impl.equals(EmbeddingsImpl.OPENAI_AZURE)) {
            vector = openAIAzureImpl.getEmbedding(sentence, model, embeddingType, apiToken);
        } else if (impl.equals(EmbeddingsImpl.COHERE)) {
            log.info("Get embedding from Cohere (" + model + ") ...");
            vector = cohereEmbeddingsImpl.getEmbedding(sentence, model, embeddingType, apiToken);
        } else if (impl.equals(EmbeddingsImpl.ALEPH_ALPHA)) {
            vector = alephAlphaEmbeddingsImpl.getEmbedding(sentence, model, embeddingType, apiToken);
        } else if (impl.equals(EmbeddingsImpl.NUMENTA)) {
            vector = numentaEmbeddings.getEmbedding(sentence, model, embeddingType, apiToken);
        } else if (impl.equals(EmbeddingsImpl.GOOGLE)) {
            vector = googleEmbeddings.getEmbedding(sentence, model, embeddingType, apiToken);
        } else {
            log.error("No such embedding implementation '" + impl + "' supported!");
        }

        // TODO: Catch rate limit (but only for background processes), throttle and retry, whereas only a limited amount of time, because there are different rate limits, e.g. "10 requests per minute" or "500 requests per month"

        if (vector == null) {
            String msg = "Embedding implementation '" + impl + "' did not generate an embedding for sentence '" + sentence + "'!";
            log.error(msg);
            throw new Exception(msg);
        }

        return vector;
    }
}
