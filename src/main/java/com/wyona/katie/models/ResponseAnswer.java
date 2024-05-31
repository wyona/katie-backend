package com.wyona.katie.models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Answer which is used as response when user is asking a question, but does not contain all the details (permissions, etc.)
 */
@Slf4j
public class ResponseAnswer {

    private String questionUUID;
    private double score;
    private int rating;

    private String qnaUUID;
    private String submittedQuestion;
    private Date dateSubmittedQuestion;

    private String answer;
    private ContentType answerContentType;

    private List<String> classifications;

    private Date dateAnswerModified;
    private String answerClientSideEncryptionAlgorithm;
    private String email;
    private String originalQuestion;
    private Date dateOriginalQuestion;
    private PermissionStatus permissionStatus;
    private QnAType type;

    private String url;
    private List<AnswerContext> relevantContexts = new ArrayList<>();

    private JsonNode dataObjectAsJson;

    /**
     * @param qnaUUID UUID of QnA / answer
     * @param submittedQuestion Question submitted by user
     * @param answer Answer to previously asked original question
     * @param contentType Content type of answer, e.g. HTML or JSON
     * @param email Email of user who submitted question
     * @param originalQuestion Original question which answer is based on
     * @param type QnA type, e.g. Credentials or Bookmark
     * @param url URL referencing content containing answer
     */
    public ResponseAnswer(String qnaUUID, String submittedQuestion, Date dateSubmittedQuestion, String answer, ContentType contentType, List<String> classifications, Date dateAnswerModified, String answerClientSideEncryptionAlgorithm, String email, String originalQuestion, Date dateOriginalQuestion, PermissionStatus permissionStatus, QnAType type, String url) {
        this.questionUUID = null; // questionUUID;
        this.score = -1;
        this.rating = -1;

        this.qnaUUID = qnaUUID;
        this.submittedQuestion = submittedQuestion;
        this.dateSubmittedQuestion = dateSubmittedQuestion;

        this.answer = answer;
        this.answerContentType = contentType;

        this.classifications = classifications;

        this.dateAnswerModified = dateAnswerModified;
        this.answerClientSideEncryptionAlgorithm = answerClientSideEncryptionAlgorithm;
        this.email = email;
        this.originalQuestion = originalQuestion;
        this.dateOriginalQuestion = dateOriginalQuestion;
        this.permissionStatus = permissionStatus;
        this.type = type;

        this.url = url;

        this.dataObjectAsJson = null;
    }

    /**
     * Get UUID of submitted question
     */
    public String getQuestionUUID() {
        return questionUUID;
    }

    /**
     *
     */
    public void setQuestionUUID(String questionUUID) {
        this.questionUUID = questionUUID;
    }

    /**
     *
     */
    public void setScore(double score) {
        this.score = score;
    }

    /**
     * Get confidence score of answer
     */
    public double getScore() {
        return score;
    }

    /**
     * Set human rating
     * @param rating -1: no rating available, 0: completely wrong, 10: completely correct
     */
    public void setRating(int rating) {
        this.rating = rating;
    }

    /**
     * Get human rating
     * @return  -1: no rating available, 0: completely wrong, 10: completely correct
     */
    public int getRating() {
        return this.rating;
    }

    /**
     * Get UUID of QnA
     */
    public String getUuid() {
        return qnaUUID;
    }

    /**
     * Set answer
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    /**
     * Get answer
     * @return complete answer
     */
    public String getAnswer() {
        return answer;
        //return getAnswer(300);
    }

    /**
     * Get answer, whereas truncate answer if it is longer than a particular max length
     * @param max_length Maximum length of answer, e.g. 300 characters
     * @return truncated answer if it is longer than specified max length
     */
    public String getAnswer(int max_length, String i18nSeeMore) {
        if (answer.length() > max_length && url != null) {
            log.info("Truncate answer (" + getUuid() + "), because answer is longer than max length '" + max_length + "'.");

            String truncatedAnswer = answer.substring(0, max_length);
            String answerTail = answer.substring(max_length);
            log.info("Rest of answer: " + answerTail);

            char separator = ' ';
            //char separator = '\n';
            int indexNextSeparator = answerTail.indexOf(separator);
            if (indexNextSeparator >= 0 && indexNextSeparator < answerTail.length()) {
                log.info("Next character: " + answer.charAt(max_length));
                truncatedAnswer = truncatedAnswer + answerTail.substring(0, indexNextSeparator);
            }

            // TODO: Add method such that client knows whether answer got truncated
            if (getUrl() != null) {
                return truncatedAnswer + " ... <a href=\"" + getUrl() + "\">" + i18nSeeMore + "</a>";
            } else {
                return truncatedAnswer + " ...";
            }
        } else {
            log.info("Do not truncate answer (" + getUuid() + ").");
            return answer;
        }
    }

    /**
     * Get answer as JSON when answer is available as JSON
     */
    public JsonNode getAnswerAsJson() {
        if (answerContentType != null && answerContentType.equals(ContentType.APPLICATION_JSON)) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                return mapper.readTree(answer);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     *
     */
    public void setData(JsonNode dataObjectAsJson) {
        this.dataObjectAsJson = dataObjectAsJson;
    }

    /**
     *
     */
    public JsonNode getData() {
        return dataObjectAsJson;
    }

    /**
     *
     */
    public void setAnswerContentType(ContentType answerContentType) {
        this.answerContentType = answerContentType;
    }

    /**
     * @return content type of answer, e.g. "text/html" or "application/json"
     */
    public String getAnswerContentType() {
        if (answerContentType != null) {
            return answerContentType.toString();
        } else {
            return null;
        }
    }

    /**
     *
     */
    public String[] getClassifications() {
        if (classifications != null) {
            return classifications.toArray(new String[0]);
        } else {
            return null;
        }
    }

    /**
     * Get date as epoch when answer was last modified / updated
     */
    public long getDateAnswerModified() {
        if (dateAnswerModified != null) {
            return dateAnswerModified.getTime();
        } else {
            return -1;
        }
    }

    /**
     * Get cipher algorithm in case answer was client side encrypted
     * @return cipher algorithm in case answer was client side encrypted, e.g. "aes-256" and null otherwise
     */
    public String getAnswerClientSideEncryptionAlgorithm() {
        return answerClientSideEncryptionAlgorithm;
    }

    /**
     *
     */
    public String getOriginalQuestion() {
        return originalQuestion;
    }

    /**
     * Get date as epoch when original question was submitted
     */
    public long getDateOriginalQuestion() {
        if (dateOriginalQuestion != null) {
            return dateOriginalQuestion.getTime();
        } else {
            return -1;
        }
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
    public Date getDateSubmittedQuestion() {
        return dateSubmittedQuestion;
    }

    /**
     * Get email of user who submitted question originally
     */
    public String getEmail() {
        return email;
    }

    /**
     *
     */
    public PermissionStatus getPermissionStatus() {
        return permissionStatus;
    }

    /**
     * Get type, e.g. Credentials or Bookmark
     */
    public QnAType getType() {
        return type;
    }

    /**
     *
     */
    public String getUrl() {
        return url;
    }

    /**
     *
     */
    public List<AnswerContext> getRelevantContexts() {
        return relevantContexts;
    }

    /**
     *
     */
    public void addRelevantContext(AnswerContext context) {
        relevantContexts.add(context);
    }
}
