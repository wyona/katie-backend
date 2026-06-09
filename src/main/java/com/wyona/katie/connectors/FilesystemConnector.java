package com.wyona.katie.connectors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.wyona.katie.models.*;
import com.wyona.katie.services.SegmentationService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

/**
 * Connector for ingesting data from Filesystem
 */
@Slf4j
@Component
public class FilesystemConnector implements Connector {

    @Autowired
    SegmentationService segmentationService;

    /**
     * @see Connector#getAnswers(Sentence, int, KnowledgeSourceMeta)
     */
    public Hit[] getAnswers(Sentence question, int limit, KnowledgeSourceMeta ksMeta) {
        log.info("TODO: Implement: " + ksMeta.getName() + "' ...");
        List<Hit> hits = new ArrayList<Hit>();

        return hits.toArray(new Hit[0]);
    }

    /**
     * @see Connector#update(Context, KnowledgeSourceMeta, WebhookPayload, String)
     */
    public List<Answer> update(Context domain, KnowledgeSourceMeta ksMeta, WebhookPayload payload, String processId) {
        List<Answer> qnas = new ArrayList<Answer>();

        File baseDir = new File(ksMeta.getFilesystemBasePath());
        if (baseDir.isDirectory()) {
            log.info("Ingest data from filesystem: " + baseDir.getAbsolutePath());
            String[] files = baseDir.list();
            for (String filename : files) {
                try {
                    List<Answer> _qnas = getChunks(new File(baseDir, filename));
                    for (Answer answer : _qnas) {
                        qnas.add(answer);
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        } else {
            log.warn("No such directory: " + baseDir.getAbsolutePath());
        }

        return qnas;
    }

    /**
     * Ingest file into knowledge base
     * @param file PDF file
     */
    private List<Answer> getChunks(File file) throws Exception {
        log.info("Ingest file: " + file.getAbsolutePath());

        PDDocument pdDoc = PDDocument.load(file);
        String body = new PDFTextStripper().getText(pdDoc);
        pdDoc.close();

        // TODO: Make text splitter configurable
        //List<String> chunks = segmentationService.splitBySentences(body, "en", 700, true);
        List<String> chunks = segmentationService.getSegments(body, '\n', 2000, 100);
        List<Answer> qnas = new ArrayList<Answer>();
        String url = "http://filesystem/" + file.getName();
        for (String chunk : chunks) {
            qnas.add(new Answer(null, chunk, ContentType.TEXT_PLAIN, url, null, null, null, null, null, null, null, null, file.getName(), null, false, null, false, null));
        }
        log.info("Number of chunks extracted from PDF document '" + file.getName() + "': " + chunks.size());
        return qnas;
    }
}
