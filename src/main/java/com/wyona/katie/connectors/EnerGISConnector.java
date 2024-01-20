package com.wyona.katie.connectors;

import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class EnerGISConnector implements Connector {

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("Get answers from EnerGIS connector ...");
        // TODO: https://www.stadt-zuerich.ch/energis/frontend/#/
        List<Hit> hits = new ArrayList<Hit>();
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
