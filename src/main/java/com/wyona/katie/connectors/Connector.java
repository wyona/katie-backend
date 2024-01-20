package com.wyona.katie.connectors;

import com.wyona.katie.models.*;

import java.util.List;

/**
 * Connector interfaces to search for answers within third party knowledge sources
 */
public interface Connector {

    /**
     * Get answer(s) (including confidence score) for a particular question associated with a particular domain
     * @param question Question including entities, e.g. "How old is Michael?" whereas "Michael" is a PERSON entity, and including classifications, e.g. "credentials" or "performance"
     * @param limit Limit of returned hits
     * @param ksMeta Knowledge source meta information, e.g. base URL or access token
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta);

    /**
     * Get chunks from a particular third-party knowledge source
     * @param domain Domain connected with third-party knowledge source
     * @param processId Background process Id
     * @return chunks of a particular third-party knowledge source
     */
    public List<Answer> update(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload, String processId);
}
