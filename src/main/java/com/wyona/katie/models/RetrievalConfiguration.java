package com.wyona.katie.models;

public class RetrievalConfiguration {

    private DetectDuplicatedQuestionImpl retrievalImpl;

    private EmbeddingsImpl embeddingImpl;
    private String embeddingEndpoint;
    private String embeddingAPIToken;
    private String embeddingModel;
    private EmbeddingValueType embeddingValueType;

    /**
     *
     */
    public void setRetrievalImpl(DetectDuplicatedQuestionImpl retrievalImpl) {
        this.retrievalImpl = retrievalImpl;
    }

    /**
     *
     */
    public DetectDuplicatedQuestionImpl getRetrievalImpl() {
        return retrievalImpl;
    }

    /**
     *
     */
    public void setEmbeddingImpl(EmbeddingsImpl embeddingImpl) {
        this.embeddingImpl = embeddingImpl;
    }

    /**
     *
     */
    public EmbeddingsImpl getEmbeddingImpl() {
        return embeddingImpl;
    }

    /**
     * @param embeddingEndpoint Embedding endpoint, e.g. "https://api.mistral.ai/v1/embeddings"
     */
    public void setEmbeddingEndpoint(String embeddingEndpoint) {
        this.embeddingEndpoint = embeddingEndpoint;
    }

    /**
     * @return embedding endpoint, e.g. "https://api.mistral.ai/v1/embeddings"
     */
    public String getEmbeddingEndpoint() {
        return embeddingEndpoint;
    }

    /**
     *
     */
    public void setEmbeddingAPIToken(String embeddingAPIToken) {
        this.embeddingAPIToken = embeddingAPIToken;
    }

    /**
     *
     */
    public String getEmbeddingAPIToken() {
        return embeddingAPIToken;
    }

    /**
     * @param embeddingModel Embedding model, e.g. "TODO"
     */
    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    /**
     * @return embedding model, e.g. "TODO"
     */
    public String getEmbeddingModel() {
        return embeddingModel;
    }

    /**
     *
     */
    public void setEmbeddingValueType(EmbeddingValueType embeddingValueType) {
        this.embeddingValueType = embeddingValueType;
    }

    /**
     *
     */
    public EmbeddingValueType getEmbeddingValueType() {
        return embeddingValueType;
    }
}
