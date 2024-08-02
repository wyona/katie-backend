package com.wyona.katie.models;

/**
 * Ask question body used by ask controller
 */
public class AskQuestionBody {

    private String messageId;
    private String question;
    private Language questionerLanguage;
    private AskQuestionContactInfo contactInfo;
    private AskQuestionPrivacyOptions privacyOptions;
    private String classification;
    private String[] classifications;

    private Boolean predictClassifications;
    private Boolean includeClassifications;

    private String answerContentType;

    private Boolean includePayloadData;

    /**
     *
     */
    public AskQuestionBody() {
        this.predictClassifications = false;
        this.includeClassifications = false;
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
     * Provide classification to narrow down search space
     * @param classification Classification, e.g. "birthdate"
     */
    public void setClassification(String classification) {
        this.classification = classification;
    }

    /**
     *
     */
    public void setPrivacyOptions(AskQuestionPrivacyOptions privacyOptions) {
        this.privacyOptions = privacyOptions;
    }

    /**
     *
     */
    public AskQuestionPrivacyOptions getPrivacyOptions() {
        return privacyOptions;
    }

    /**
     * @return classification, e.g. "birthdate"
     */
    public String getClassification() {
        return classification;
    }

    /**
     * Provide classifications to narrow down search space
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

    /**
     * @param predictClassifications When set to true, then Katie tries to predict Classifications / Labels based on question / input message
     */
    public void setPredictClassifications(Boolean predictClassifications) {
        this.predictClassifications = predictClassifications;
    }

    /**
     * @return true when Katie tries to predict classifications / labels and false when Katie does not predict classifications / labels
     */
    public Boolean getPredictClassifications() {
        return predictClassifications;
    }

    /**
     * @param includeClassifications When set to true, then classifications of answers and predicted classifications will be included into answers
     */
    public void setIncludeClassifications(Boolean includeClassifications) {
        this.includeClassifications = includeClassifications;
    }

    /**
     * @return true when classifications of answers and predicted classifications are being included into answers
     */
    public Boolean getIncludeClassifications() {
        return includeClassifications;
    }

    /**
     * @param includePayloadData When true, then payload of originally imported data should be included into answers
     */
    public void setIncludePayloadData(Boolean includePayloadData) {
        this.includePayloadData = includePayloadData;
    }

    /**
     * @return true when payload of originally imported data should be included into answers
     */
    public Boolean getIncludePayloadData() {
        return includePayloadData;
    }
}
