package com.wyona.katie.services;

import com.wyona.katie.connectors.*;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class ConnectorService {

    @Autowired
    private ContextService domainService;

    @Autowired
    private KnowledgeSourceXMLFileService knowledgeSourceXMLFileService;

    @Autowired
    private ForeignKeyIndexService foreignKeyIndexService;

    @Autowired
    private BackgroundProcessService backgroundProcessService;

    @Autowired
    private DirectusConnector directusConnector;
    @Autowired
    private SupabaseConnector supabaseConnector;
    @Autowired
    private DiscourseConnector discourseConnector;
    @Autowired
    private ConfluenceConnector confluenceConnector;
    @Autowired
    private OneNoteConnector oneNoteConnector;
    @Autowired
    private OutlookConnector outlookConnector;
    @Autowired
    private SharepointConnector sharepointConnector;
    @Autowired
    private WebsiteConnector websiteConnector;
    @Autowired
    private TOPdeskConnector topDeskConnector;

    /**
     * Trigger third-party knowledge source synchronization in background
     */
    @Async
    public void triggerKnowledgeSourceConnectorInBackground(KnowledgeSourceConnector ksc, String domainId, String ksId, WebhookPayload payload, String processId, String userId) {

        Connector connector = null;
        if (ksc.equals(KnowledgeSourceConnector.OUTLOOK)) {
            connector = outlookConnector;
        } else if (ksc.equals(KnowledgeSourceConnector.ONENOTE)) {
            connector = oneNoteConnector;
        } else if (ksc.equals(KnowledgeSourceConnector.SHAREPOINT)) {
            connector = sharepointConnector;
        } else if (ksc.equals(KnowledgeSourceConnector.WEBSITE)) {
            connector = websiteConnector;
        } else if (ksc.equals(KnowledgeSourceConnector.CONFLUENCE)) {
            // TODO
        } else if (ksc.equals(KnowledgeSourceConnector.DIRECTUS)) {
            // TODO
        } else if (ksc.equals(KnowledgeSourceConnector.SUPABASE)) {
            connector = supabaseConnector;
        } else if (ksc.equals(KnowledgeSourceConnector.TOP_DESK)) {
            connector = topDeskConnector;
        } else if (ksc.equals(KnowledgeSourceConnector.DISCOURSE)) {
            connector = discourseConnector;
        } else {
            log.error("No such connector '" + ksc + "'!");
        }

        log.info("Trigger " + connector.getClass().getName() + " based knowledge source '" + ksId + "' of domain '" + domainId + "' ...");
        log.info("Payload received from " + connector.getClass().getName() + ": " + payload);

        backgroundProcessService.startProcess(processId, "Synchronize Knowledge Source '" + ksId + "' connected with domain '" + domainId + "'.", userId);

        try {
            KnowledgeSourceMeta ksMeta = domainService.getKnowledgeSource(domainId, ksId, ksc);

            if (ksMeta != null) {
                if (!ksMeta.getIsEnabled()) {
                    String msg = "Knowledge source '" + domainId + " / " + ksId + "' is disabled.";
                    log.warn(msg);
                    backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.WARN);
                    backgroundProcessService.stopProcess(processId);
                    return;
                }
                Context domain = domainService.getContext(domainId);
                List<Answer> qnas = connector.update(domain, ksMeta, payload, processId);
                if (qnas != null && qnas.size() > 0) {
                    backgroundProcessService.updateProcessStatus(processId, "Import and index " + qnas.size() + " QnAs ...");
                    int counterSuccessful = 0;
                    for (Answer qna : qnas) {
                        try {
                            String uuid = domainService.addQuestionAnswer(qna, domain).getUuid();
                            domainService.addToUuidUrlIndex(uuid, qna.getUrl(), domain);

                            if (domain.getDetectDuplicatedQuestionImpl().equals(DetectDuplicatedQuestionImpl.LUCENE_VECTOR_SEARCH) && domain.getEmbeddingsImpl().equals(EmbeddingsImpl.COHERE)) {
                                int throttleTimeInMillis = 10000; // TODO: Make configurable
                                if (throttleTimeInMillis > 0) {
                                    try {
                                        String msg = "Sleep for " + throttleTimeInMillis + " milliseconds ...";
                                        log.info(msg);
                                        backgroundProcessService.updateProcessStatus(processId, msg);
                                        Thread.sleep(throttleTimeInMillis);
                                    } catch (Exception e) {
                                        log.error(e.getMessage(), e);
                                    }
                                }
                            }
                            domainService.train(new QnA(qna), domain, true);

                            counterSuccessful++;
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                            backgroundProcessService.updateProcessStatus(processId, "Import / indexing of '" + qna.getUrl() + "' failed: " + e.getMessage(), BackgroundProcessStatusType.ERROR);
                        }
                    }
                    backgroundProcessService.updateProcessStatus(processId, counterSuccessful + " QnAs successfully imported and indexed.");

                    updateSyncInfo(counterSuccessful, domainId, ksId);
                    backgroundProcessService.updateProcessStatus(processId, "Sync info updated.");
                } else {
                    log.warn("No QnAs imported!");
                    backgroundProcessService.updateProcessStatus(processId, ConnectorService.class.getSimpleName() + ": No QnAs imported! Maybe Connector itself already imported / updated QnAs.", BackgroundProcessStatusType.WARN);
                }
            } else {
                backgroundProcessService.updateProcessStatus(processId, "No such knowledge source '" + ksId + " / " + ksc + "'!", BackgroundProcessStatusType.ERROR);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            backgroundProcessService.updateProcessStatus(processId, e.getMessage(), BackgroundProcessStatusType.ERROR);
        }

        backgroundProcessService.stopProcess(processId);
    }

    /**
     * Trigger Directus based knowledge source
     */
    public void triggerKnowledgeSourceDirectus(String domainId, String ksId, WebhookPayloadDirectus payload) throws Exception {
        log.info("Trigger Directus based knowledge source '" + ksId + "' of domain '" + domainId + "' ...");
        log.info("Payload received from Directus: " + payload);

        KnowledgeSourceMeta ksMeta = domainService.getKnowledgeSource(domainId, ksId, KnowledgeSourceConnector.DIRECTUS);

        if (ksMeta != null) {
            if (!ksMeta.getIsEnabled()) {
                log.warn("Knowledge source '" + domainId + " / " + ksId + "' is disabled.");
                return;
            }
            Context domain = domainService.getContext(domainId);
            updateKnowledgeSourceDirectus(domain, ksMeta, payload);
        }
    }

    /**
     * Trigger Confluence based knowledge source
     */
    public void triggerKnowledgeSourceConfluence(String domainId, String ksId, WebhookPayloadConfluence payload) throws Exception {
        log.info("Trigger Confluence based knowledge source '" + ksId + "' of domain '" + domainId + "' ...");
        log.info("Payload received from Confluence: " + payload);

        KnowledgeSourceMeta ksMeta = domainService.getKnowledgeSource(domainId, ksId, KnowledgeSourceConnector.CONFLUENCE);

        if (ksMeta != null) {
            if (!ksMeta.getIsEnabled()) {
                log.warn("Knowledge source '" + domainId + " / " + ksId + "' is disabled.");
                return;
            }
            Context domain = domainService.getContext(domainId);
            updateKnowledgeSourceConfluence(domain, ksMeta, payload);
        }
    }

    /**
     * Add date when Knowledge Base was synced successfully
     */
    private void updateSyncInfo(int numberOfChunks, String domainId, String ksId) throws Exception {
        knowledgeSourceXMLFileService.updateSyncInfo(domainId, ksId, numberOfChunks);
    }

    /**
     * TODO: Move this method to Directus implementation
     */
    private void updateKnowledgeSourceDirectus(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayloadDirectus payload) {
        log.info("Update knowledge source connected with Directus ...");

        if (payload.getEvent().equals("items.update")) {
            for (String key : payload.getKeys()) {
                log.info("Directus item updated: " + key);
                Answer answer = directusConnector.contextualize(key, ksMeta);
                if (answer != null) {
                    answer.setKnowledgeSource(ksMeta.getId(), key);
                    String katieUUID = foreignKeyIndexService.getUUID(domain, ksMeta, key);
                    if (katieUUID != null) {
                        answer.setUUID(katieUUID);
                        try {
                            domainService.saveQuestionAnswer(domain, katieUUID, answer);
                            domainService.retrain(new QnA(answer), domain, false);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    } else {
                        try {
                            answer = domainService.addQuestionAnswer(answer, domain);
                            foreignKeyIndexService.addForeignKey(domain, ksMeta, key, answer.getUuid());
                            domainService.train(new QnA(answer), domain, false);
                        } catch (Exception e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                } else {
                    log.info("Directus item '" + key + "' probably got unpublished.");
                    domainService.deleteKnowledgeSourceItem(domain, ksMeta, key);
                }
            }
        } else if (payload.getEvent().equals("items.create")) {
            log.info("New Directus item created: " + payload.getKey());
            Answer answer = directusConnector.contextualize(payload.getKey(), ksMeta);
            if (answer != null) {
                answer.setKnowledgeSource(ksMeta.getId(), payload.getKey());
                try {
                    answer = domainService.addQuestionAnswer(answer, domain);
                    foreignKeyIndexService.addForeignKey(domain, ksMeta, payload.getKey(), answer.getUuid());
                    domainService.train(new QnA(answer), domain, false);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } else if (payload.getEvent().equals("items.delete")) {
            for (String key : payload.getKeys()) {
                domainService.deleteKnowledgeSourceItem(domain, ksMeta, key);
            }
        } else {
            log.warn("Directus payload event type '" + payload.getEvent() + "' not implemented yet!");
        }
    }

    /**
     * TODO: Move this method to Confluence implementation
     * TODO: Re-use updateKnowledgeSourceDirectus(...)
     */
    private void updateKnowledgeSourceConfluence(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayloadConfluence payload) {
        log.info("Update knowledge source connected with Confluence ...");

        if (payload.getEventType().equals("page_updated")) {
            String key = payload.getPage().getId();
            Answer answer = confluenceConnector.contextualize(key, ksMeta);
            answer.setUrl(payload.getPage().getSelf());

            if (answer != null) {
                answer.setKnowledgeSource(ksMeta.getId(), key);
                String katieUUID = foreignKeyIndexService.getUUID(domain, ksMeta, key);
                if (katieUUID != null) {
                    answer.setUUID(katieUUID);
                    try {
                        domainService.saveQuestionAnswer(domain, katieUUID, answer);
                        domainService.retrain(new QnA(answer), domain, false);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                } else {
                    try {
                        answer = domainService.addQuestionAnswer(answer, domain);
                        foreignKeyIndexService.addForeignKey(domain, ksMeta, key, answer.getUuid());
                        domainService.train(new QnA(answer), domain, false);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                }
            } else {
                log.info("Confluence item '" + key + "' probably got unpublished.");
                domainService.deleteKnowledgeSourceItem(domain, ksMeta, key);
            }
        } else if (payload.getEventType().equals("page_created")) {
            /*
            log.info("New Directus item created: " + payload.getKey());
            Answer answer = confluenceConnector.contextualize(payload.getKey(), ksMeta);
            if (answer != null) {
                answer.setKnowledgeSource(ksMeta.getId(), payload.getKey());
                try {
                    answer = addQuestionAnswer(answer, domain);
                    foreignKeyIndexService.addForeignKey(domain, ksMeta, payload.getKey(), answer.getUuid());
                    train(new QnA(answer), domain, false);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }

             */
        } else if (payload.getEventType().equals("page_removed")) {
            /*
            for (String key : payload.getKeys()) {
                deleteKnowledgeSourceItem(domain, ksMeta, key);
            }

             */
        } else {
            log.warn("Event type '" + payload.getEventType() + "' not implemented yet!");
        }
    }
}
