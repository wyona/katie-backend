package com.wyona.katie.models;
  
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Analyzed message
 */
@Slf4j
public class AnalyzedMessage {

    private String message;
    private boolean containsQuestions;
    private List<QuestionContext> qcs;
    private List<String> classifications;
    private List<Entity> entities;
    private String introduction;

    private QuestionClassificationImpl messageAnalyzerImpl;

    /**
     * @param message Original, complete message, for example "Hey all, I'm really interested in using a vector search DB for a project I'm working on, but I'm not sure Weaviate is the right tool. Could someone please tell me about the data visualization and analytics capabilities of this DB? I'm hoping to feed it text from around 100 different speakers and determine things like: who sounds the most similar to who and what topics does each person talk about the most. Can Weaviate do these sorts of things? Or does anyone have a recommendation for where I should look?"
     */
    public AnalyzedMessage(String message, QuestionClassificationImpl messageAnalyzerImpl) {
        this.message = message;
        this.containsQuestions = false;
        this.qcs = new ArrayList<QuestionContext>();
        this.classifications = new ArrayList<String>();
        this.entities = new ArrayList<Entity>();

        this.messageAnalyzerImpl = messageAnalyzerImpl;
    }

    /**
     *
     */
    public QuestionClassificationImpl getMessageAnalyzerImpl() {
        return messageAnalyzerImpl;
    }

    /**
     * Get original, complete message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get all entities, e.g. "Weaviate"
     */
    public List<Entity> getEntities() {
        return entities;
    }

    /**
     * @param containsQuestions True when message contains questions and false otherwise
     */
    public void setContainsQuestions(boolean containsQuestions) {
        this.containsQuestions = containsQuestions;
    }

    /**
     * @return true when message contains questions and false otherwise
     */
    public boolean getContainsQuestions() {
        return containsQuestions;
    }

    /**
     * @param classification Classification, e.g. "num", "tutorial"
     */
    public void addClassification(String classification) {
        classifications.add(classification);
    }

    /**
     * Get all classifications, e.g. "tutorial"
     */
    public List<String> getClassifications() {
        return classifications;
    }

    /**
     * Add a question and its context
     * @param question "Does somebody know a good vegetarian restaurant?"
     * @param context "We will be on holidays in Rio de Janeiro next week."
     */
    public void addQuestionAndContext(Sentence question, Sentence context) {
        qcs.add(new QuestionContext(question, context));
    }

    /**
     * Get all questions and its contexts contained by message, e.g. "Could someone please tell me about the data visualization and analytics capabilities of this DB?" / "visualization and analytics capabilities", "Can Weaviate do these sorts of things?", "Or does anyone have a recommendation for where I should look?" / "I'm hoping to feed it text from around 100 different speakers and determine things like: who sounds the most similar to who and what topics does each person talk about the most."
     */
    public List<QuestionContext> getQuestionsAndContexts() {
        return qcs;
    }

    /**
     *
     */
    public void setIntroduction(String introduction) {
        this.introduction= introduction;
    }

    /**
     * For example "Hey all, I'm really interested in using a vector search DB for a project I'm working on, but I'm not sure Weaviate is the right tool"
     */
    public String getIntroduction() {
        return introduction;
    }

    /**
     *  Get knowledge graph nodes
     */
    public String getKnowledgeGraphNodes() {
        return null;
    }

    @Override
    public String toString() {
    	return "Message: "+ this.message + ",\ncontains Question(s): " + Boolean.toString(this.containsQuestions) + ", \nQuestions and Contexts: " + this.qcs.toString() + ", \nClassifications: " + this.classifications.toString() + ", \nEntities: " + this.entities.toString();
    }
}
