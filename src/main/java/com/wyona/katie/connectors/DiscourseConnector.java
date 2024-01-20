package com.wyona.katie.connectors;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.wyona.katie.services.BackgroundProcessService;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.models.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * Get messages from Discourse (https://discourse.org), e.g. Weaviate Forum https://forum.weaviate.io/
 */
@Slf4j
@Component
public class DiscourseConnector implements Connector {

    @Autowired
    BackgroundProcessService backgroundProcessService;

    @Autowired
    private ContextService domainService;

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Get answers from Discourse connector ...");
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
        backgroundProcessService.updateProcessStatus(processId, "Update Katie with Discourse payload data ...");
        WebhookPayloadDiscourse _payload = (WebhookPayloadDiscourse) payload;
        if (_payload.getPost() != null) {
            String postId = _payload.getPost().get("id").asText();
            log.info("Add Discourse post '" + postId + "' raw message: " + _payload.getPost().get("raw").asText());
            log.info("Add Discourse post '" + postId + "' cooked message: " + _payload.getPost().get("cooked").asText());
            try {
                // TODO: Consider using foreign key?!
                // TODO: Consider saving as thread
                String uuid = UUID.randomUUID().toString();
                domainService.saveDataObject(domain, uuid, _payload.getPost());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        return null;
    }
}
