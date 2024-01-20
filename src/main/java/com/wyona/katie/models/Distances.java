package com.wyona.katie.models;

/**
 * Distances between two vectors (embeddings) of various metrics, e.g. cosine similarity or euclidean
 * https://docs.scipy.org/doc/scipy/reference/generated/scipy.spatial.distance.cdist.html
 */
public class Distances {

    private float cosineSimilarity;
    private float cosineDistance;
    private float dotProduct;

    // INFO: Meta information
    private EmbeddingsImpl embeddingsImpl;
    private String embeddingsModel;
    private int vectorDimension;

    private String sentenceOne;
    private String sentenceTwo;

    private float[] embeddingOne;
    private float[] embeddingTwo;

    /**
     * @param vectorDimension Vector dimension, e.g. 128 or 768 or 1024
     */
    public Distances(float cosineSimilarity, float cosineDistance, EmbeddingsImpl embeddingsImpl, String embeddingsModel, int vectorDimension, String sentenceOne, String sentenceTwo, float dotProduct) {
        this.cosineSimilarity = cosineSimilarity;
        this.cosineDistance = cosineDistance;
        this.dotProduct = dotProduct;

        this.embeddingsImpl = embeddingsImpl;
        this.embeddingsModel = embeddingsModel;
        this.vectorDimension = vectorDimension;
        this.sentenceOne = sentenceOne;
        this.sentenceTwo = sentenceTwo;
    }

    /**
     *
     */
    public float getCosineSimilarity() {
        return cosineSimilarity;
    }

    /**
     *
     */
    public float getCosineDistance() {
        return cosineDistance;
    }

    /**
     *
     */
    public float getDotProduct() { return  dotProduct; }

    /**
     *
     */
    public EmbeddingsImpl getEmbeddingsImpl() {
        return embeddingsImpl;
    }

    /**
     * @return embeddings model, e.g. "all-mpnet-base-v2"
     */
    public String getEmbeddingsModel() {
        return embeddingsModel;
    }

    /**
     * @return vector dimension, e.g. 128 or 768 or 1024
     */
    public int getVectorDimension() {
        return vectorDimension;
    }

    /**
     *
     */
    public String getSentenceOne() {
        return sentenceOne;
    }

    /**
     *
     */
    public String getSentenceTwo() {
        return sentenceTwo;
    }

    /**
     *
     */
    public float[] getEmbeddingOne() {
        return embeddingOne;
    }

    /**
     *
     */
    public void setEmbeddingOne(float[] embedding) {
        this.embeddingOne = embedding;
    }

    /**
     *
     */
    public float[] getEmbeddingTwo() {
        return embeddingTwo;
    }

    /**
     *
     */
    public void setEmbeddingTwo(float[] embedding) {
        this.embeddingTwo = embedding;
    }
}
