package com.wyona.katie.connectors;

import com.wyona.katie.services.BackgroundProcessService;
import com.wyona.katie.services.ContextService;
import com.wyona.katie.services.ForeignKeyIndexService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 *
 */
@Slf4j
@Component
public class SupabaseConnector implements Connector {

    @Autowired
    private ForeignKeyIndexService foreignKeyIndexService;

    @Autowired
    private ContextService domainService;

    @Autowired
    BackgroundProcessService backgroundProcessService;

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("TODO: Implement");
        return null;
    }

    /**
     * @see Connector#update(Context, KnowledgeSourceMeta, WebhookPayload, String)
     */
    public List<Answer> update(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload, String processId) {
        List<Answer> qnas = new ArrayList<Answer>();
        Answer qna = updateKnowledgeSourceSupabase(domain, ksMeta, (WebhookPayloadSupabase) payload, processId);
        if (qna != null) {
            qnas.add(qna);
        }
        return qnas;
    }

    /**
     * TODO
     */
    private Answer updateKnowledgeSourceSupabase(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayloadSupabase payload, String processId) {
        log.info("Update knowledge source connected with Supabase ...");

        if (payload.getType().equals("UPDATE")) {
            log.info("Update Supabase item ...");
            ObjectNode record = payload.getRecord();
            String id = record.get(ksMeta.getSupabaseIdName()).asText();
            log.info("Supabase Id: " + id);
            backgroundProcessService.updateProcessStatus(processId, "Update QnA based on Supabase Item '" + id + "' ...");
            String katieUUID = foreignKeyIndexService.getUUID(domain, ksMeta, id);

            if (katieUUID != null) {
                // TODO: Move this check to ForeignKeyIndexService#getUUID()
                if (!domainService.existsQnA(katieUUID, domain)) {
                    String msg = "QnA '" + katieUUID + "' was probably deleted, but the foreign key index was not cleaned up properly!";
                    log.warn(msg);
                    backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.WARN);
                    foreignKeyIndexService.deleteForeignKey(domain, ksMeta, id);
                    katieUUID = null;
                }
            }

            if (katieUUID == null) {
                log.info("Add QnA ...");
                return createQnA(domain, id, record, ksMeta, processId);
            } else {
                log.info("Override QnA '" + katieUUID + "' ...");
                try {
                    Answer qna = domainService.getQnA(null, katieUUID, domain);
                    qna.setOriginalQuestion(getConcatenatedValue(record, ksMeta.getSupabaseQuestionNames()));
                    qna.setAnswer(getConcatenatedValue(record, ksMeta.getSupabaseAnswerNames()));
                    qna.setUrl(ksMeta.getSupabaseBaseUrl() + "/" + id);

                    qna.deleteAllClassifications();
                    List<String> classifications = getClassificationValues(record, ksMeta.getSupabaseClassificationsNames());
                    for (String classification : classifications) {
                        qna.addClassification(classification);
                    }

                    qna.setDateAnswerModified(new Date());

                    // TODO: Let ConnectorService update and retrain QnA, whereas make sure, that ConnectorService is also handling Foreign Key
                    domainService.saveQuestionAnswer(domain, katieUUID, qna);
                    domainService.retrain(new QnA(qna), domain, false);
                    try {
                        domainService.saveDataObject(domain, qna.getUuid(), record);
                    } catch (Exception e) {
                        log.error(e.getMessage(), e);
                    }
                    backgroundProcessService.updateProcessStatus(processId, SupabaseConnector.class.getSimpleName() + ": QnA '" + katieUUID + "' updated.");
                    return null;
                    //return qna;
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } else if (payload.getType().equals("INSERT")) {
            log.info("Create new Supabase item ...");
            ObjectNode record = payload.getRecord();
            String id = record.get(ksMeta.getSupabaseIdName()).asText();
            log.info("Supabase Id: " + id);
            backgroundProcessService.updateProcessStatus(processId, "Create QnA based on Supabase Item '" + id + "' ...");

            if (foreignKeyIndexService.existsUUID(domain, ksMeta, id)) {
                String katieUUID = foreignKeyIndexService.getUUID(domain, ksMeta, id);
                // TODO: Move this check to ForeignKeyIndexService#getUUID()
                if (!domainService.existsQnA(katieUUID, domain)) {
                    String msg = "QnA '" + katieUUID + "' was probably deleted, but the foreign key index was not cleaned up properly!";
                    log.warn(msg);
                    backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.WARN);
                    foreignKeyIndexService.deleteForeignKey(domain, ksMeta, id);
                }
            }

            if (foreignKeyIndexService.existsUUID(domain, ksMeta, id)) {
                String katieUUID = foreignKeyIndexService.getUUID(domain, ksMeta, id);
                String msg = "QnA '" + katieUUID + "' already exists for Supabase item '" + id + "'!";
                log.error(msg);
                backgroundProcessService.updateProcessStatus(processId, msg, BackgroundProcessStatusType.ERROR);
            } else {
                return createQnA(domain, id, record, ksMeta, processId);
            }
        } else if (payload.getType().equals("DELETE")) {
            ObjectNode record = payload.getOld_record();
            String id = record.get(ksMeta.getSupabaseIdName()).asText();
            log.info("Try to delete Supabase item '" + id + "' ...");
            if (foreignKeyIndexService.existsUUID(domain, ksMeta, id)) {
                String uuid = foreignKeyIndexService.getUUID(domain, ksMeta, id);
                try {
                    domainService.deleteTrainedQnA(domain, uuid);
                    foreignKeyIndexService.deleteForeignKey(domain, ksMeta, id);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else {
                log.error("No such Supabase item '" + id + "'!");
            }
        } else {
            log.warn("Supabase payload type '" + payload.getType() + "' not implemented yet!");
        }

        return null;
    }

    /**
     *
     */
    private Answer createQnA(Context domain, String id, JsonNode record, KnowledgeSourceMeta ksMeta, String processId) {
        String question = getConcatenatedValue(record, ksMeta.getSupabaseQuestionNames());
        String answer = getConcatenatedValue(record, ksMeta.getSupabaseAnswerNames());
        String url = ksMeta.getSupabaseBaseUrl() + "/" + id;
        ContentType contentType = null;

        List<String> classifications  = getClassificationValues(record, ksMeta.getSupabaseClassificationsNames());

        Date dateAnswered = new Date();
        Date dateAnswerModified = dateAnswered;
        Date dateOriginalQuestionSubmitted = dateAnswered;
        boolean isPublic = false;
        Answer newQnA = new Answer(null, answer, contentType, url, classifications, QnAType.DEFAULT, null, dateAnswered, dateAnswerModified, null, domain.getId(), null, question, dateOriginalQuestionSubmitted, isPublic, new Permissions(isPublic), false, null);

        newQnA = domainService.addQuestionAnswer(newQnA, domain);
        try {
            domainService.saveDataObject(domain, newQnA.getUuid(), record);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        // TODO: Let ConnectorService import and train QnA, whereas make sure, that ConnectorService is also handling Foreign Key
        try {
            foreignKeyIndexService.addForeignKey(domain, ksMeta, id, newQnA.getUuid());
            domainService.train(new QnA(newQnA), domain, false);
            backgroundProcessService.updateProcessStatus(processId, SupabaseConnector.class.getSimpleName() + ": New QnA '" + newQnA.getUuid() + "' added.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
        //return newQnA;
    }

    /**
     * @param fieldNames Array of classification field names, e.g. "texts, custom_tags, coauthors"
     */
    private List<String> getClassificationValues(JsonNode record, String[] fieldNames) {
        List<String> values = new ArrayList<String>();

        for (String fieldName : fieldNames) {
            JsonNode nodes = record.get(fieldName);
            if (nodes != null && nodes.isArray()) {
                log.info(nodes.size() + " values provided by field '" + fieldName + "'.");
                for (int i = 0; i < nodes.size(); i++) {
                    values.add(nodes.get(i).asText());
                }
            } else {
                log.warn("Field '" + fieldName + "' contains no array!");
            }
        }

        return values;
    }

    /**
     * @param fieldNames Array of text field names, e.g. "abstract, text, titel"
     */
    private String getConcatenatedValue(JsonNode record, String[] fieldNames) {
        StringBuilder value = new StringBuilder();
        for (String fieldName : fieldNames) {
            if (record.has(fieldName)) {
                value.append(record.get(fieldName).asText());
                value.append(" "); // TODO: Do not add for last fieldName
            } else {
                log.warn("No such field '" + fieldName + "'!");
            }
        }
        return value.toString();
    }
}
