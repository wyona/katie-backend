package com.wyona.katie.connectors;

import com.wyona.katie.services.BackgroundProcessService;
import com.wyona.katie.services.ContextService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.*;
import com.wyona.katie.services.SegmentationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Search inside Supabase and sync Supabase
 */
@Slf4j
@Component
public class SupabaseConnector implements Connector {

    @Autowired
    private ContextService domainService;

    @Autowired
    BackgroundProcessService backgroundProcessService;

    @Autowired
    SegmentationService segmentationService;

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("TODO: Implement getting answers from Supabase");
        return null;
    }

    /**
     * @see Connector#update(Context, KnowledgeSourceMeta, WebhookPayload, String)
     */
    public List<Answer> update(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload, String processId) {
        return updateKnowledgeSourceSupabase(domain, ksMeta, (WebhookPayloadSupabase) payload, processId);
    }

    /**
     * TODO
     */
    private List<Answer> updateKnowledgeSourceSupabase(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayloadSupabase payload, String processId) {
        log.info("Update knowledge source connected with Supabase ...");

        List<Answer> qnas = new ArrayList<Answer>();

        if (payload.getType().equals("INSERT") || payload.getType().equals("UPDATE")) {
            ObjectNode record = payload.getRecord();
            String id = record.get(ksMeta.getSupabaseIdName()).asText();
            if (payload.getType().equals("INSERT")) {
                log.info("Insert Supabase item Id: " + id);
                backgroundProcessService.updateProcessStatus(processId, "Add QnA(s) based on Supabase Item '" + id + "' ...");
            } else {
                log.info("Update Supabase item Id: " + id);
                backgroundProcessService.updateProcessStatus(processId, "Update QnA(s) based on Supabase Item '" + id + "' ...");
            }
            List<Answer> _qnas = splitIntoChunks(domain, id, record, ksMeta, processId);
            for (Answer qna : _qnas) {
                qnas.add(qna);
            }
        } else if (payload.getType().equals("DELETE")) {
            ObjectNode record = payload.getOld_record();
            String id = record.get(ksMeta.getSupabaseIdName()).asText();
            log.info("Try to delete Supabase item '" + id + "' ...");

            String webUrl = ksMeta.getSupabaseBaseUrl() + "/" + id;
            domainService.deletePreviouslyImportedChunks(webUrl, domain);
        } else {
            log.warn("Supabase payload type '" + payload.getType() + "' not implemented yet!");
        }

        return qnas;
    }

    /**
     * Split Supabase record into text chunks
     * @param id Supabase record Id
     * @param record Supabase record content
     */
    private List<Answer> splitIntoChunks(Context domain, String id, JsonNode record, KnowledgeSourceMeta ksMeta, String processId) {
        List<Answer> qnas = new ArrayList<>();

        String contentUrl = ksMeta.getSupabaseBaseUrl() + "/" + id; // TODO: Is content URL and web URL really the same?!
        String webUrl = ksMeta.getSupabaseBaseUrl() + "/" + id;

        domainService.deletePreviouslyImportedChunks(webUrl, domain);
        // TODO
        //File dumpFile = utilsService.dumpContent(domain, new URI(contentUrl), apiToken);
        domainService.saveMetaInformation(contentUrl, webUrl, new Date(), domain);

        try {
            String msg = "Extract text from Supabase record '" + id + "' and split into chunks ...";
            log.info(msg);
            backgroundProcessService.updateProcessStatus(processId, msg);

            String question = getConcatenatedValue(record, ksMeta.getSupabaseQuestionNames());
            List<String> labels = getClassificationValues(record, ksMeta.getSupabaseClassificationsNames());

            String concatenatedText = getConcatenatedValue(record, ksMeta.getSupabaseAnswerNames());
            // TODO: Detect language
            List<String> chunks = segmentationService.splitBySentences(concatenatedText, "de", 500, true);
            for (String chunk : chunks) {
                qnas.add(new Answer(null, chunk, ContentType.TEXT_PLAIN, webUrl, labels, null, null, null, null, null, null, null, question, null, false, null, false, null));
            }
            msg = "Number of chunks extracted from Supabase record: " + chunks.size();
            log.info(msg);
            backgroundProcessService.updateProcessStatus(processId, msg);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return qnas;
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
