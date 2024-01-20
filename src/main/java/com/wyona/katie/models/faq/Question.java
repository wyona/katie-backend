package com.wyona.katie.models.faq;
  
import java.io.Serializable;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Question implements Serializable {

    private String uuid;
    private String question;
    private String answer;

/* TODO: Consider expanding question for better search results
    private String questionExpansion;
    private String answerExpansion;
    private String alternativeQuestion;
    private String knowledgeGraph;
    private String classification;
*/

    private String language;
    private String topicId;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Question() {
    }

    /**
     *
     */
    public Question(String uuid, String question, String answer) {
        this.uuid = uuid;
        this.question = question;
        this.answer = answer;
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
     * Get answer to question
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
     *
     */
    public String getLanguage() {
        return language;
    }

    /**
     *
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    /**
     *
     */
    public String getTopicId() {
        return topicId;
    }

    /**
     *
     */
    public void setTopicId(String topicId) {
        this.topicId = topicId;
    }
}
