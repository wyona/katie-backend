package com.wyona.katie.handlers;

import com.wyona.katie.models.EmbeddingType;
import com.wyona.katie.models.EmbeddingValueType;
import com.wyona.katie.models.FloatVector;
import com.wyona.katie.models.Vector;

/**
 * Embeddings provider, e.g. SentenceBERT, Cohere, OpenAI, ...
 */
public interface EmbeddingsProvider {

    /**
     * Get embedding for a sentence
     * @param sentence Text, e.g. a question or short answer
     * @param model Model name, e.g., Qwen3-Embedding-8B or OpenAI's "text-embedding-3-large" or Cohere's "embed-multilingual-v2.0"
     * @param embeddingType Embedding type (see https://txt.cohere.com/introducing-embed-v3/)
     * @param valueType Embedding value type, either float32 or int8 (see for example https://www.elastic.co/search-labs/blog/articles/scalar-quantization-101)
     * @param dimensions Optional dimensions parameter to use truncated embeddings. When an embedding model (e.g., Qwen3-Embedding-8B with 4096 dimensions) supports the Matryoshka approach, then we might want to use truncated embeddings (1014 instead 4096). See for example https://huggingface.co/blog/matryoshka
     * @param endpoint Embeddings base endpoint, e.g., "https://openai-katie.openai.azure.com"
     * @param apiToken API token
     * @return embedding, which is a vector with for example 1024 dimensions
     */
    public Vector getEmbedding(String sentence, String model, EmbeddingType embeddingType, EmbeddingValueType valueType, Integer dimensions, String endpoint, String apiToken) throws Exception;
}
