package com.wyona.katie.models;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * Answer 2 asked / submitted question
 */
@Slf4j
public class Answer {

    public static final String AK_UUID_COLON = "ak-uuid:";

    private String domainId;
    private String uuid;
    private String submittedQuestion;
    private String answer;
    private ContentType answerContentType;
    private String answerClientSideEncryptionAlgorithm;

    private String url;

    private QnAType type;
    private List<String> classifications;

    // TODO: Date dateAnswered;
    private Date dateAnswerModified;
    private String email;
    private String originalQuestion;
    private Date dateOriginalQuestionSubmitted;

    private List<String> alternativeQuestions;

    private boolean isPublic;
    private Ownership ownership;
    private Permissions permissions;

    private boolean isTrained;
    private String respondentId;
    private java.util.List<Rating> ratings;

    private boolean isReference;
    private QnAReference reference;

    private Language faqLanguage;
    private String faqTopicId; // TBD: Can QnA be connected with multiple FAQ topics?

    private String knowledgeSourceUuid;
    private String knowledgeSourceItemForeignKey;

    /**
     * @param submittedQuestion Question submitted by user
     * @param answer Answer to previously asked original question
     * @param answerContentType Content type of answer, e.g. JSON or HTML
     * @param url URL referencing content containing answer, e.g. "https://en.wikipedia.org/wiki/Brazil"
     * @param classifications Classifications, e.g. "num", "date", "count", "hum", "instruction", "code"
     * @param type QnA content type, e.g. URL/Bookmark, credentials, shopping list
     * @param answerClientSideEncryptionAlgorithm Client side encryption algorithm, e.g. "aes-256"
     * @param dateAnswerModified Date when answer was last modified / updated
     * @param email Email of user who submitted original question
     * @param domainId Id of domain the answer is associated with
     * @param uuid UUID of answer
     * @param originalQuestion Original question which answer is based on
     * @param isPublic true when answer is public and false otherwise
     * @param isTrained true when QnA is trained/indexed and false otherwise
     * @param respondentId Id of user who provided answer
     */
    public Answer(String submittedQuestion, String answer, ContentType answerContentType, String url, List<String> classifications, QnAType type, String answerClientSideEncryptionAlgorithm, Date dateAnswered, Date dateAnswerModified, String email, String domainId, String uuid, String originalQuestion, Date dateOriginalQuestionSubmitted, boolean isPublic, Permissions permissions, boolean isTrained, String respondentId) {
        this.submittedQuestion = submittedQuestion;

        this.answer = answer;
        this.answerContentType = answerContentType;

        this.url = url;
        this.classifications = classifications;
        this.type = type;
        this.answerClientSideEncryptionAlgorithm = answerClientSideEncryptionAlgorithm;
        this.dateAnswerModified = dateAnswerModified;
        // TODO: this.dateAnswered = dateAnswered;
        this.email = email;
        this.domainId = domainId;
        this.uuid = uuid;
        this.originalQuestion = originalQuestion;
        this.dateOriginalQuestionSubmitted = dateOriginalQuestionSubmitted;

        this.alternativeQuestions = new ArrayList<String>();

        this.isPublic = isPublic;
        this.permissions = permissions;
        this.ownership = null;

        this.isTrained = isTrained;
        this.respondentId = respondentId;

        this.isReference = false;
        this.reference = null;

        this.ratings = new ArrayList<Rating>();

        this.knowledgeSourceUuid = null;
        this.knowledgeSourceItemForeignKey = null;
    }

    /**
     *
     */
    public static Comparator<Answer> DateComparator = new Comparator<Answer>() {

        @Override
        public int compare(Answer a1, Answer a2) {
            long date1 = getDate(a1);
            long date2 = getDate(a2);
            if (date1 > date2) {
                return -1;
            } else if (date1 == date2) {
                return 0;
            } else {
                return 1;
            }
        }

        /**
         * Get date of answer to be compared
         */
        private long getDate(Answer answer) {
            if (answer.getDateAnswerModified() >= 0) {
                return answer.getDateAnswerModified();
            } else {
                return answer.getDateOriginalQuestion();
            }
        }
    };

    /**
     *
     */
    public String getAnswer() { // INFO: No CamelCase for method name, because freemarker does not work properly with CamelCase method names
        //log.debug("Anser: " + answer);
        return answer;
    }

    /**
     * @return content type of answer, e.g. HTML or JSON
     */
    public ContentType getAnswerContentType() {
        return answerContentType;
    }

    /**
     * @param contentType Content type of answer, e.g. HTML or JSON
     */
    public void setAnswerContentType(ContentType contentType) {
        this.answerContentType = contentType;
    }

    /**
     * @return URL referencing content containing answer, e.g. "https://en.wikipedia.org/wiki/Brazil"
     */
    public String getUrl() {
        return url;
    }

    /**
     *
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Get QnA type, e.g. URL/bookmark, credentials
     */
    public QnAType getType() {
        return type;
    }

    /**
     * Get classification class, e.g. "announcement" or "[0,0,1]" (https://en.wikipedia.org/wiki/Multi-label_classification)
     */
    public List<String> getClassifications() {
        return classifications;
    }

    /**
     * Get cipher algorithm in case answer was client side encrypted
     * @return cipher algorithm in case answer was client side encrypted, e.g. "aes-256" and null otherwise
     */
    public String getAnswerClientSideEncryptionAlgorithm() {
        return answerClientSideEncryptionAlgorithm;
    }

    /**
     * @return epoch time when answer was last modified / updated, whereas -1 returned when date not set
     */
    public long getDateAnswerModified() {
        if (dateAnswerModified != null) {
            return dateAnswerModified.getTime();
        } else {
            return -1;
        }
    }

    /**
     * Set date when answer was last modified / updated
     */
    public void setDateAnswerModified(Date dateAnswerModified) {
        this.dateAnswerModified = dateAnswerModified;
    }

    /**
     *
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    /**
     * @return true when answer is public and false otherwise
     */
    public boolean isPublic() {
        return isPublic;
    }

    /**
     * Add user rating
     */
    public void addRating(Rating rating) {
        this.ratings.add(rating);
    }

    /**
     * Get all user ratings
     */
    public Rating[] getRatings() {
        return ratings.toArray(new Rating[0]);
    }

    /**
     *
     */
    public void setOwnership(Ownership ownership) {
        this.ownership = ownership;
    }

    /**
     *
     */
    public Ownership getOwnership() {
        return ownership;
    }

    /**
     *
     */
    public String getRespondentId() {
        return respondentId;
    }

    /**
     *
     */
    public void setTrained(boolean isTrained) {
        this.isTrained = isTrained;
    }

    /**
     * @return true when QnA is trained and false otherwise
     */
    public boolean isTrained() {
        return isTrained;
    }

    /**
     * @return permissions
     */
    public Permissions getPermissions() {
        return permissions;
    }

    /**
     *
     */
    public String getDomainid() {
        return domainId;
    }

    /**
     *
     */
    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    /**
     *
     */
    public String getUuid() { // INFO: No CamelCase for method name, because freemarker does not work properly with CamelCase method names
        //log.debug("UUID: " + uuid);
        return uuid;
    }

    /**
     *
     */
    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    /**
     * Get originally asked question to this answer, e.g. "Wo melde ich mich, wenn mich jemand bedroht?"
     */
    public String getOriginalquestion() { // INFO: No CamelCase for method name, because freemarker does not work properly with CamelCase method names
        //log.debug("Original question: " + originalQuestion);
        return originalQuestion;
    }

    /**
     *
     */
    public void setOriginalQuestion(String originalQuestion) {
        this.originalQuestion = originalQuestion;
    }

    /**
     * Get date as epoch when original question was submitted
     */
    public long getDateOriginalQuestion() {
        if (dateOriginalQuestionSubmitted != null) {
            return dateOriginalQuestionSubmitted.getTime();
        } else {
            return -1;
        }
    }

    /**
     *
     */
    public void setDateOriginalQuestion(Date dateOriginalQuestionSubmitted) {
        this.dateOriginalQuestionSubmitted = dateOriginalQuestionSubmitted;
    }

    /**
     * Get array of alternative questions
     */
    public String[] getAlternativequestions() { // INFO: No CamelCase for method name, because freemarker does not work properly with CamelCase method names
        return alternativeQuestions.toArray(new String[0]);
    }

    /**
     * @param question Alternative question, e.g. "Was mache ich, wenn mir jemand droht?"
     */
    public void addAlternativeQuestion(String question) {
        alternativeQuestions.add(question);
    }

    /**
     * @param index Index of alternative question, e.g. "2"
     */
    public void deleteAlternativeQuestion(int index) {
        log.info("Delete alternative question at position '" + index + "' ...");
        if (0 <= index && index < alternativeQuestions.size()) {
            alternativeQuestions.remove(index);
        } else {
            log.warn("Index '" + index + "' out of range!");
        }
    }

    /**
     * @param classification Classification, e.g. "gravel bike"
     */
    public void addClassification(String classification) {
        if (classifications == null) {
            classifications = new ArrayList<String>();
        }

        classifications.add(classification);
    }

    /**
     * @param index Index of classification, e.g. "2"
     */
    public void deleteClassification(int index) {
        log.info("Delete classification at position '" + index + "' ...");
        if (0 <= index && index < classifications.size()) {
            classifications.remove(index);
        } else {
            log.warn("Index '" + index + "' out of range!");
        }
    }

    /**
     * Delete all classifications
     */
    public void deleteAllClassifications() {
        classifications.clear();
    }

    /**
     *
     */
    public String getSubmittedQuestion() {
        return submittedQuestion;
    }

    /**
     *
     */
    public void setSubmittedQuestion(String submittedQuestion) {
        this.submittedQuestion = submittedQuestion;
    }

    /**
     * Get email of user who submitted question originally
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return true when this QnA is referencing another QnA, false otherwise
     */
    public boolean getIsReference() {
        return isReference;
    }

    /**
     * Set reference when this QnA is pointing to another QnA
     */
    public void setReference(QnAReference reference) {
        this.reference = reference;
        this.isReference = true;
    }

    /**
     * Get reference when this QnA is pointing to another QnA
     * @return reference when pointing to another QnA, null otherwise
     */
    public QnAReference getReference() {
        return reference;
    }

    /**
    * Remove prefix
    * @param prefixAnswerUUID Prefix and UUID, e.g. 'ak-uuid:ffb2bfd2-9618-4e8b-bb4d-e0f2424513a5'
    * @return UUID, e.g. 'ffb2bfd2-9618-4e8b-bb4d-e0f2424513a5'
    */
   public static String removePrefix(String prefixAnswerUUID) {
       if (prefixAnswerUUID.startsWith(AK_UUID_COLON)) {
           return prefixAnswerUUID.substring(AK_UUID_COLON.length());
       } else {
           log.warn("String '" + prefixAnswerUUID + "' does not start with '" + AK_UUID_COLON + "'!");
           return null;
       }
   }

    /**
     * Get language of FAQ associated with QnA
     */
    public Language getFaqLanguage() {
        return faqLanguage;
    }

    /**
     *
     */
    public void setFaqLanguage(Language faqLanguage) {
        this.faqLanguage = faqLanguage;
    }

    /**
     * Get topic Id of FAQ associated with QnA
     */
    public String getFaqTopicId() {
        return faqTopicId;
    }

    /**
     *
     */
    public void setFaqTopicId(String faqTopicId) {
        this.faqTopicId = faqTopicId;
    }

    /**
     * Set third party knowledge source references
     * @param knowledgeSourceUuid UUID of third party knowledge source, whereas see CONTEXT/knowledge-sources.xml
     * @param knowledgeSourceItemForeignKey Foreign key of third party knowledge source item
     */
    public void setKnowledgeSource(String knowledgeSourceUuid, String knowledgeSourceItemForeignKey) {
        this.knowledgeSourceUuid = knowledgeSourceUuid;
        this.knowledgeSourceItemForeignKey = knowledgeSourceItemForeignKey;
    }

    /**
     *
     */
    public String getKnowledgeSourceUuid() {
        return knowledgeSourceUuid;
    }

    /**
     *
     */
    public String getKnowledgeSourceItemForeignKey() {
        return knowledgeSourceItemForeignKey;
    }
}
