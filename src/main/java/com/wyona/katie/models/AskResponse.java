package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.index.VectorSimilarityFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Response object to an ask request
 */
@Slf4j
public class AskResponse {

    private String askedQuestion;
    private String questionUUID;
    private List<String> submittedclassifications = new ArrayList<String>();
    private List<ResponseAnswer> answers;
    private Boolean knowledgeBaseEmpty;
    private int offset;
    private int limit;

    private DetectDuplicatedQuestionImpl retrievalImpl;
    private EmbeddingsImpl embeddingsImpl;
    private VectorSimilarityFunction similarityMetric;

    private HitLabel[] predictedLabels = null;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     * @param askedQuestion Question asked by user
     * @param submittedclassifications Classifications to narrow the search / response range
     */
    public AskResponse(String askedQuestion, String questionUUID, List<String> submittedclassifications, DetectDuplicatedQuestionImpl retrievalImpl, EmbeddingsImpl embeddingsImpl, VectorSimilarityFunction similarityMetric) {
        this.askedQuestion = askedQuestion;
        this.questionUUID = questionUUID;
        this.submittedclassifications = submittedclassifications;

        this.retrievalImpl = retrievalImpl;
        this.embeddingsImpl = embeddingsImpl;
        this.similarityMetric = similarityMetric;

        this.answers = new ArrayList<ResponseAnswer>();
        this.knowledgeBaseEmpty = null;
        this.offset = -1;
        this.limit = -1;
    }

    /**
     *
     */
    public String getAskedQuestion() {
        return askedQuestion;
    }

    /**
     *
     */
    public String getQuestionUUID() {
        return questionUUID;
    }

    /**
     *
     */
    public String[] getSubmittedClassifications() {
        return submittedclassifications.toArray(new String[0]);
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
    public EmbeddingsImpl getEmbeddingsImpl() {
        return embeddingsImpl;
    }

    /**
     *
     */
    public VectorSimilarityFunction getSimilarityMetric() {
        return similarityMetric;
    }

    /**
     *
     */
    public ResponseAnswer[] getAnswers() {
        return answers.toArray(new ResponseAnswer[0]);
    }

    /**
     *
     */
    public void setAnswers(List<ResponseAnswer> answers) {
        this.answers = answers;
    }

    /**
     *
     */
    public Boolean getKnowledgeBaseEmpty() {
        return knowledgeBaseEmpty;
    }

    /**
     *
     */
    public void setKnowledgeBaseEmpty(Boolean knowledgeBaseEmpty) {
        this.knowledgeBaseEmpty = knowledgeBaseEmpty;
    }

    /**
     *
     */
    public int getOffset() {
        return offset;
    }

    /**
     *
     */
    public void setOffset(int offset) {
        this.offset = offset;
    }

    /**
     *
     */
    public int getLimit() {
        return limit;
    }

    /**
     *
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }

    /**
     *
     */
    public void setPredictedLabels(HitLabel[] predictedLabels) {
        this.predictedLabels = predictedLabels;
    }

    /**
     * Get predicted labels / classifications, based on submitted question / message
     */
    public HitLabel[] getPredictedLabels() {
        return predictedLabels;
    }
}
