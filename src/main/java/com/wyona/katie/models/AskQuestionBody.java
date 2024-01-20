package com.wyona.katie.models;

/**
 * Ask question body used by ask controller
 */
public class AskQuestionBody {

    private String messageId;
    private String question;
    private Language questionerLanguage;
    private AskQuestionContactInfo contactInfo;
    private String classification;
    private String[] classifications;

    private String answerContentType;

    /**
     *
     */
    public AskQuestionBody() {
    }

    /**
     * @param messageId Message Id of client which sent question / message to Katie
     */
    public void setMessageId(String messageId) {
        this.messageId = messageId;
    }

    /**
     * @return message Id of client which sent question / message to Katie
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * @param contentType Content type of answer accepted by client, e.g. "text/plain"
     */
    public void setAcceptContentType(String contentType) {
        this.answerContentType = contentType;
    }

    /**
     * @return content type of answer accepted by client, e.g. "text/plain"
     */
    public String getAcceptContentType() {
        return answerContentType;
    }

    /**
     * @param question Question asked by user
     */
    public void setQuestion(String question) {
        this.question = question;
    }

    /**
     * @return question asked by user
     */
    public String getQuestion() {
        return question;
    }

    /**
     * @param questionerLanguage Language of user asking question, e.g. "de" or "en"
     */
    public void setQuestionerLanguage(Language questionerLanguage) {
        this.questionerLanguage = questionerLanguage;
    }

    /**
     * @return language of user asking question, e.g. "de" or "en"
     */
    public Language getQuestionerLanguage() {
        return this.questionerLanguage;
    }

    /**
     * @param contactInfo Contact information of user, such that Katie can send expert's answer back to user
     */
    public void setOptionalContactInfo(AskQuestionContactInfo contactInfo) {
        this.contactInfo = contactInfo;
    }

    /**
     * @return contact information of user, such that Katie can send expert's answer back to user
     */
    public AskQuestionContactInfo getOptionalContactInfo() {
        return contactInfo;
    }

    /**
     * @param classification Classifications, e.g. "birthdate"
     */
    public void setClassification(String classification) {
        this.classification = classification;
    }

    /**
     * @return classification, e.g. "birthdate"
     */
    public String getClassification() {
        return classification;
    }

    /**
     * @param classifications Multiple classifications
     */
    public void setClassifications(String[] classifications) {
        this.classifications = classifications;
    }

    /**
     * @return multiple classifications
     */
    public String[] getClassifications() {
        return classifications;
    }
}
