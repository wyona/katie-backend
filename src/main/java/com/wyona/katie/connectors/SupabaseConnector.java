package com.wyona.katie.connectors;

import com.fasterxml.jackson.databind.node.TextNode;
import com.wyona.katie.services.BackgroundProcessService;
import com.wyona.katie.services.ContextService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wyona.katie.models.*;
import com.wyona.katie.services.SegmentationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.*;

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

            String contentUrl = ksMeta.getSupabaseBaseUrl() + "/" + id;
            String webUrl = contentUrl; // TODO: Is content URL and web URL really the same?!

            try {
                domainService.savePayloadData(domain, new URI(webUrl), record);
                // INFO: Compare with
                // File dumpFile = utilsService.dumpContent(domain, new URI(contentUrl), apiToken);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            List<Answer> _qnas = splitIntoChunks(domain, webUrl, record, ksMeta, processId);
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
     * @param webUrl TODO
     * @param record Supabase record content
     * @return list of answers based on chunks
     */
    private List<Answer> splitIntoChunks(Context domain, String webUrl, JsonNode record, KnowledgeSourceMeta ksMeta, String processId) {
        List<Answer> qnas = new ArrayList<>();

        domainService.deletePreviouslyImportedChunks(webUrl, domain);

        try {
            String msg = "Extract text from Supabase record '" + webUrl + "' and split into chunks by sentences (Chunk size: " + ksMeta.getChunkSize() + ") ...";
            log.info(msg);
            backgroundProcessService.updateProcessStatus(processId, msg);

            String question = getConcatenatedValue(record, ksMeta.getSupabaseQuestionNames());
            List<String> labels = getClassificationValues(record, ksMeta.getSupabaseClassificationsNames());
            
            String concatenatedText = getConcatenatedValue(record, ksMeta.getSupabaseAnswerNames());
            // TODO: Detect language
            List<String> chunks = segmentationService.splitBySentences(concatenatedText, "de", ksMeta.getChunkSize(), true);
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
                log.info("Get text from field '" + fieldName + "' ...");
                String extractedText = getText(record.get(fieldName), "");
                log.info("Extracted text: " + extractedText);
                value.append(extractedText);
                value.append(" "); // TODO: Do not add for last fieldName
            } else {
                log.warn("No such field '" + fieldName + "'!");
            }
        }
        return value.toString();
    }

    /**
     * Extract all text values recursively from a JsonNode
     * @param node JsonNode, e.g. "data": {"en":"Some text", "de":"Ein Text"}
     * @param text Concatenated text
     * @return text
     */
    private String getText(JsonNode node, String text) {
        if (node instanceof TextNode) {
            return text + " " + node.asText();
        }

        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                text = getText(fields.next().getValue(), text);
            }
            return text;
        } else if (node.isArray()) {
            // TODO: Extract all text values recursively from array
            return "TODO_ARRAY";
        } else {
            log.error("JsonNode is neither object nor array!");
            return "";
        }
    }
}
