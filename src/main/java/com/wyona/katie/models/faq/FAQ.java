package com.wyona.katie.models.faq;
  
import java.io.Serializable;
import java.util.ArrayList;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
public class FAQ implements Serializable {

    private ArrayList<Topic> topics;

    // INFO: Default constructor is necessary, because otherwise a 400 is generated when using @RequestBody (see https://stackoverflow.com/questions/27006158/error-400-spring-json-requestbody-when-doing-post)
    /**
     *
     */
    public FAQ() {
        topics = new ArrayList<Topic>();
    }

    /**
     *
     */
    public Topic[] getTopics() {
        return topics.toArray(new Topic[0]);
    }

    /**
     * Get a particular topic
     * @param id Topic id
     */
    private Topic getTopic(String id) {
        for (Topic topic: topics) {
            if (topic.getId().equals(id)) {
                return topic;
            }
        }
        return null;
    }

    /**
     *
     */
    public void addTopic(Topic topic) {
        topics.add(topic);
    }

    /**
     *
     */
    public void addQnA(String topicId, String uuid) {
        log.info("Add QnA with UUID '" + uuid + "' to topic '" + topicId + "' ...");
        Topic topic = getTopic(topicId);
        if (topic != null) {
            // TODO: Check whether QnA with this UUID is already added
            topic.addQuestion(uuid, null, null);
        } else {
            log.error("No such topic with Id '" + topicId + "'!");
        }
    }

    /**
     * Remove a particular QnA from FAQ
     * @param uuid UUID of QnA
     * @return true when QnA has been removed and false otherwise
     */
    public boolean removeQnA(String uuid) {
        boolean removedFromFAQ = false;
        for (Topic topic: topics) {
            boolean removedFromTopic = topic.removeQuestion(uuid);
            // INFO: The QnA can be contained by multiple topics
            if (removedFromTopic) {
                removedFromFAQ = true;
            }
        }
        return removedFromFAQ;
    }

    /**
     *
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Topic topic: topics) {
            sb.append("Topic: " + topic.getTitle() + "\n");
            for (Question qna: topic.getQuestions()) {
                sb.append("  QnA: " + qna.getQuestion() + " | " + qna.getAnswer() + "\n");
            }
        }
        return sb.toString();
    }
}
