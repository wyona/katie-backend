package com.wyona.katie.models;

import java.util.Date;
import java.io.Serializable;

import lombok.extern.slf4j.Slf4j;

import com.wyona.katie.models.Sentence;

/**
 *
 */
@Slf4j
public class ResubmittedQuestion implements Serializable {

    public final static String DEEP_LINK = "deeplink";

    private String uuid;

    private String question;
    private Sentence analyzedQuestion;

    private String questionerUserId;
    private Language questionerLanguage;

    private ChannelType channelType;
    private String channelRequestId;

    private String email;
    private String fcmToken;

    private String answerLinkType;
    private String status;
    private String remoteAddress;
    private Date timestampResubmitted;

    private String answer;
    private String answerClientSideEncryptedAlgorithm;
    private Ownership ownership;
    private String respondentUserId;
    private User respondent;

    private String contextId;

    private boolean trained;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public ResubmittedQuestion() {
    }

    /**
     * @param questionerLanguage Language of user asking question
     * @param email Email of user who submitted question
     * @param answerClientSideEncryptedAlgorithm Client side encryption algorithm, e.g. "aes-256"
     * @param respondentUserId User Id of user answering resubmitted question
     */
    public ResubmittedQuestion(String uuid, String question, String questionerUserId, Language questionerLanguage, ChannelType channelType, String channelRequestId, String email, String fcmToken, String answerLinkType, String status, String remoteAddress, Date timestampResubmitted, String answer, String answerClientSideEncryptedAlgorithm, Ownership ownership, String respondentUserId, String contextId) {
        this.uuid = uuid;

        this.question = question;

        this.questionerUserId = questionerUserId;
        this.questionerLanguage = questionerLanguage; // INFO: Can also be set when user is anonymous

        this.channelType = channelType;
        this.channelRequestId = channelRequestId;

        this.email = email;
        this.fcmToken = fcmToken;

        this.answerLinkType = answerLinkType;
        this.status = status;
        this.remoteAddress = remoteAddress;
        this.timestampResubmitted = timestampResubmitted;
      
        this.answer = answer;
        this.answerClientSideEncryptedAlgorithm = answerClientSideEncryptedAlgorithm;
        this.ownership = ownership;
        this.respondentUserId = respondentUserId;
        this.respondent = null;

        this.contextId = contextId;

        this.trained = false;
    }

    /**
     * Get context Id
     * @return context Id and ROOT when context Id is null
     */
    public String getContextId() {
        if (contextId == null) {
            return Context.ROOT_NAME;
        }
        return contextId;
    }

    /**
     *
     */
    public Date getTimestampResubmitted() {
        return timestampResubmitted;
    }

    /**
     *
     */
    public void setTimestampResubmitted(Date timestampResubmitted) {
        log.warn("TODO: Implement timestamp setter!");
    }

    /**
     *
     */
    public String getUuid() {
        return uuid;
    }

    /**
     *
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     *
     */
    public String getStatus() {
        return status;
    }

    /**
     * Get client side encryption algorithm, e.g. "aes-256"
     * @return algorithm (e.g. "aes-256") when answer was client side encrypted and null otherwise
     */
    public String getAnswerClientSideEncryptedAlgorithm() {
        return answerClientSideEncryptedAlgorithm;
    }

    /**
     *
     */
    public void setAnswerClientSideEncryptedAlgorithm(String answerClientSideEncryptedAlgorithm) {
        this.answerClientSideEncryptedAlgorithm = answerClientSideEncryptedAlgorithm;
    }

    /**
     * Get ownership in order to know who can view answer
     * @return ownership
     */
    public Ownership getOwnership() {
        return ownership;
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
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     *
     */
    public String getRemoteAddress() {
        return remoteAddress;
    }

    /**
     *
     */
    public void setRemoteAddress(String remoteAddress) {
        log.warn("TODO: Implement remot address setter!");
    }

    /**
     *
     */
    public void setAnalyzedQuestion(Sentence analyzedQuestion) {
        this.analyzedQuestion = analyzedQuestion;
    }

    /**
     *
     */
    public Sentence getAnalyzedQuestion() {
        return analyzedQuestion;
    }

    /**
     *
     */
    public void setQuestionerLanguage(Language language) {
        this.questionerLanguage = language;
    }

    /**
     *
     */
    public Language getQuestionerLanguage() {
        return questionerLanguage;
    }

    /**
     * Get question which was resubmitted, because either there was no answer available or provided answer was not satisfactory
     */
    public String getQuestion() {
        return question;
    }

    /**
     *
     */
    public void setQuestion(String question) {
        this.question = question;
    }

    /**
     * Get user Id of person which asked question
     */
    public String getQuestionerUserId() {
        return questionerUserId;
    }

    /**
     *
     */
    public ChannelType getChannelType() {
        return channelType;
    }

    /**
     *
     */
    public String getChannelRequestId() {
        return channelRequestId;
    }

    /**
     * Get email of person which asked question
     */
    public String getEmail() {
        return email;
    }

    /**
     *
     */
    public void setEmail(String email) {
        log.warn("TODO: Implement email setter!");
    }

    /**
     * Get FCM token of mobile device of person which asked question
     */
    public String getFCMToken() {
        return fcmToken;
    }

    /**
     *
     */
    public void setFCMToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    /**
     * Get answer link type
     */
    public String getAnswerLinkType() {
        return answerLinkType;
    }

    /**
     *
     */
    public void setAnswerLinkType(String answerLinkType) {
        log.warn("TODO: Implement answer link type setter!");
    }

    /**
     * Get answer which was provided by respondent
     */
    public String getAnswer() {
        return answer;
    }

    /**
     *
     */
    public void setAnswer(String answer) {
        this.answer = answer;
    }

    /**
     * Get user Id of user providing an answer to the resubmitted question
     */
    public String getRespondentUserId() {
        return respondentUserId;
    }

    /**
     * Set user providing an answer to the resubmitted question
     */
    public void setRespondent(User user) {
        this.respondent = user;
        this.respondentUserId = user.getId();
    }

    /**
     * Get user providing an answer to the resubmitted question
     * (WARNING: Used by REST interface)
     */
    public User getRespondent() {
        return respondent;
    }

    /**
     *
     */
    public void setTrained(boolean trained) {
        this.trained = trained;
    }

    /**
     * Get flag whether AI was trained with this resubmitted question and associated answer
     * (WARNING: Used by REST interface)
     * @return true when AI was trained with this resubmitted question and associated answer, and return false otherwise
     */
    public boolean getTrained() {
        return trained;
    }
}
