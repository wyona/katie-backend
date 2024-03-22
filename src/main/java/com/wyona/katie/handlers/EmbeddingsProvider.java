package com.wyona.katie.handlers;

import com.wyona.katie.models.EmbeddingType;
import com.wyona.katie.models.EmbeddingValueType;
import com.wyona.katie.models.FloatVector;

/**
 * Embeddings provider, e.g. SentenceBERT, Cohere, OpenAI, Aleph Alpha, ...
 */
public interface EmbeddingsProvider {

    /**
     * Get embedding for a sentence
     * @param sentence Text, e.g. a question or short answer
     * @param model Model name, e.g. OpenAI's "text-embedding-ada-002" or Cohere's "embed-multilingual-v2.0"
     * @param embeddingType Embedding type (see https://txt.cohere.com/introducing-embed-v3/)
     * @param valueType Embedding value type, either float32 or int8 (see for example https://www.elastic.co/search-labs/blog/articles/scalar-quantization-101)
     * @param apiToken API token
     * @return embedding, which is a vector with for example 1024 dimensions
     */
    public FloatVector getEmbedding(String sentence, String model, EmbeddingType embeddingType, EmbeddingValueType valueType, String apiToken) throws Exception;
}
