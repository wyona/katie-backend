package com.wyona.katie.connectors;

import java.util.ArrayList;
import java.util.List;

import com.wyona.katie.models.*;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 *
 */
@Slf4j
@Component
public class TOPdeskConnector implements Connector {

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Get answers from TOPdesk connector ...");
        List<Hit> hits = new ArrayList<Hit>();

        //String mockAnswer = "Mock answer";
        String mockAnswer = "A Moderator is a person which is moderating the answers of Katie before answers are being replied to users. Turning on moderation can make sense when you are testing Katie in a productive environment and you want to make sure, that the answers of Katie are really correct and helpful.";
        String url = "https://katie.qa/#/read-answer?domain-id=ROOT&uuid=45595eb6-1f95-4123-aa17-959ea2e24673";
        Answer answer = new Answer(question.getSentence(), mockAnswer, null, url, null, null, null, null, null, null, null, null, null, null, true, null, false, null);
        Hit hit = new Hit(answer, -3.14);
        hits.add(hit);

        return hits.toArray(new Hit[0]);
    }

    /**
     * @see Connector#update(Context, KnowledgeSourceMeta, WebhookPayload, String)
     */
    public List<Answer> update(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload, String processId) {
        log.info("TODO: Implement");
        return null;
    }
}
