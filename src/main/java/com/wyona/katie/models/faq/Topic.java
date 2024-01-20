package com.wyona.katie.models.faq;
  
import java.io.Serializable;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class Topic implements Serializable {

    private String id;
    private String title;
    private Icon icon;
    private ArrayList<Question> questions;
    private TopicVisibility visibility;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public Topic() {
        questions = new ArrayList<Question>();
    }

    /**
     * @param id Topic Id
     * @param visibility Visibility, e.g. "public" or "private"
     */
    public Topic(String id, String title, Icon icon, TopicVisibility visibility) {
        this.id = id;
        this.title = title;
        this.icon = icon;
        questions = new ArrayList<Question>();
        this.visibility = visibility;
    }

    /**
     * Get topic Id
     */
    public String getId() {
        return id;
    }

    /**
     * Set topic Id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get topic title, e.g. "General"
     */
    public String getTitle() {
        return title;
    }

    /**
     *
     */
    public Icon getIcon() {
        return icon;
    }

    /**
     * @return visibility, e.g. "public" or "private"
     */
    public TopicVisibility getVisibility() {
        return visibility;
    }

    /**
     * @param visibility Visibility, e.g. "public" or "private"
     */
    public void setVisibility(TopicVisibility visibility) {
        this.visibility = visibility;
    }

    /**
     *
     */
    public Question[] getQuestions() {
        return questions.toArray(new Question[0]);
    }

    /**
     *
     */
    public void addQuestion(String uuid, String question, String answer) {
        if (uuid != null) {
            log.debug("Add QnA with Id '" + uuid + "' in memory.");
        } else {
            log.debug("Add QnA in memory: Q: " + question + " | A: " + answer);
        }
        questions.add(new Question(uuid, question, answer));
    }

    /**
     * @return true when QnA was removed and false otherwise
     */
    public boolean removeQuestion(String uuid) {
        for (Question qna: questions) {
            if (qna.getUuid().equals(uuid)) {
                log.info("Topic '" + title + "' (Number of questions: " + questions.size() + ") does contain QnA with UUID '" + uuid + "', therefore remove from topic ...");
                questions.remove(qna);
                log.info("QnA removed. Number of questions: " + questions.size());
                return true;
            }
        }
        log.info("Topic '" + title + "' does not contain QnA with UUID '" + uuid + "'.");
        return false;
    }
}
